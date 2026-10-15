package com.bharavahi.core.domain.usecase.payment

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.EmailStatus
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.MemberDueLine
import com.bharavahi.core.domain.model.MemberDues
import com.bharavahi.core.domain.model.NewPayment
import com.bharavahi.core.domain.model.Payment
import com.bharavahi.core.domain.model.PaymentMethod
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.repository.EventRepository
import com.bharavahi.core.domain.repository.OrgContext
import com.bharavahi.core.domain.repository.PaymentBackupScheduler
import com.bharavahi.core.domain.repository.PaymentRepository
import com.bharavahi.core.domain.repository.ScheduleRepository
import com.bharavahi.core.domain.repository.SheetSyncScheduler
import com.bharavahi.core.domain.rules.MemberDuesRules
import com.bharavahi.core.domain.rules.WhatsAppMessage
import com.bharavahi.core.domain.usecase.domainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import javax.inject.Inject

/** An event with every member's dues worked out (ORG-17). */
data class EventDues(val event: Event, val schedules: List<Schedule>, val members: List<MemberDues>)

class ObserveEventDuesUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
    private val schedules: ScheduleRepository,
) {
    /** Emits null when the event no longer exists. */
    operator fun invoke(eventId: String, today: LocalDate): Flow<EventDues?> = flow {
        val orgId = org.orgId()
        emitAll(
            combine(
                events.observe(orgId, eventId),
                schedules.observeAll(orgId, eventId),
                events.observeMembers(orgId, eventId),
            ) { event, scheduleList, members ->
                event?.let { EventDues(it, scheduleList, MemberDuesRules.computeAll(members, scheduleList, today)) }
            },
        )
    }
}

/** ORG-20: one member's dues line by line, and their payments in this event. */
data class MemberDuesDetail(
    val event: Event,
    val dues: MemberDues,
    val lines: List<MemberDueLine>,
    val payments: List<Payment>,
)

class ObserveMemberDuesUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
    private val schedules: ScheduleRepository,
    private val payments: PaymentRepository,
) {
    operator fun invoke(eventId: String, memberId: String, today: LocalDate): Flow<MemberDuesDetail?> = flow {
        val orgId = org.orgId()
        emitAll(
            combine(
                events.observe(orgId, eventId),
                schedules.observeAll(orgId, eventId),
                events.observeMembers(orgId, eventId),
                payments.observeAll(orgId, eventId),
            ) { event, scheduleList, members, paymentList ->
                val member = members.firstOrNull { it.memberId == memberId }
                if (event == null || member == null) {
                    null
                } else {
                    MemberDuesDetail(
                        event = event,
                        dues = MemberDuesRules.compute(member, scheduleList, today),
                        lines = MemberDuesRules.lines(member, scheduleList, today),
                        payments = paymentList.filter { it.memberId == memberId },
                    )
                }
            },
        )
    }
}

/** ORG-21 to ORG-23: validates against what is owed, saves in one batch, then queues the backup email. */
class RecordPaymentUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
    private val schedules: ScheduleRepository,
    private val payments: PaymentRepository,
    private val backup: PaymentBackupScheduler,
    private val sheets: SheetSyncScheduler,
) {
    suspend operator fun invoke(eventId: String, payment: NewPayment): Result<String> = domainResult {
        val orgId = org.orgId()
        val schedule = schedules.observe(orgId, eventId, payment.scheduleId).first()
        val member = events.observeMembers(orgId, eventId).first().firstOrNull { it.memberId == payment.memberId }
        validate(payment, schedule, member?.paidOn(payment.scheduleId, payment.dueIndex))
        val id = payments.record(orgId, eventId, payment.copy(note = payment.note.trim()))
        backup.enqueue(orgId, eventId, id)
        sheets.enqueue(orgId, eventId)
        id
    }

    private fun validate(payment: NewPayment, schedule: Schedule?, alreadyPaid: Long?) {
        val due = schedule?.dues?.firstOrNull { it.index == payment.dueIndex }
        val errors = buildMap<Field, ValidationError> {
            if (alreadyPaid == null) put(Field.MEMBER, ValidationError.REQUIRED)
            if (schedule == null) put(Field.SCHEDULE, ValidationError.REQUIRED)
            if (schedule != null && due == null) put(Field.DUE, ValidationError.REQUIRED)
            if (due != null && alreadyPaid != null) {
                val owed = due.amount - alreadyPaid
                when {
                    owed <= 0 -> put(Field.DUE, ValidationError.DUE_ALREADY_PAID)
                    payment.amount <= 0 -> put(Field.AMOUNT, ValidationError.INVALID_AMOUNT)
                    payment.amount > owed -> put(Field.AMOUNT, ValidationError.AMOUNT_TOO_LARGE)
                }
            } else if (payment.amount <= 0) {
                put(Field.AMOUNT, ValidationError.INVALID_AMOUNT)
            }
        }
        if (errors.isNotEmpty()) throw DomainException.Validation(errors)
    }
}

/** ORG-25: a mistake is corrected with a negative entry linked to the original; nothing is edited or deleted. */
class ReversePaymentUseCase @Inject constructor(
    private val org: OrgContext,
    private val payments: PaymentRepository,
    private val backup: PaymentBackupScheduler,
    private val sheets: SheetSyncScheduler,
) {
    suspend operator fun invoke(eventId: String, paymentId: String, today: LocalDate): Result<String> = domainResult {
        val orgId = org.orgId()
        val all = payments.observeAll(orgId, eventId).first()
        val original = all.firstOrNull { it.id == paymentId } ?: throw DomainException.NotFound()
        if (original.isReversal || all.any { it.reversesPaymentId == paymentId }) throw DomainException.AlreadyReversed()
        val reversal = NewPayment(
            memberId = original.memberId,
            scheduleId = original.scheduleId,
            dueIndex = original.dueIndex,
            amount = -original.amount,
            method = original.method,
            note = "Reversal",
            paidOn = today,
        )
        val id = payments.record(orgId, eventId, reversal, reversesPaymentId = original.id)
        backup.enqueue(orgId, eventId, id)
        sheets.enqueue(orgId, eventId)
        id
    }
}

/** ORG-24. */
class ObservePaymentsUseCase @Inject constructor(
    private val org: OrgContext,
    private val payments: PaymentRepository,
) {
    operator fun invoke(eventId: String): Flow<List<Payment>> = flow { emitAll(payments.observeAll(org.orgId(), eventId)) }

    companion object {
        fun filter(payments: List<Payment>, memberId: String?, scheduleId: String?): List<Payment> = payments.filter {
            (memberId == null || it.memberId == memberId) && (scheduleId == null || it.scheduleId == scheduleId)
        }

        /** Ids of payments that a later reversing entry cancels. */
        fun reversedIds(payments: List<Payment>): Set<String> = payments.mapNotNull { it.reversesPaymentId }.toSet()
    }
}

/** ORG-29: Resend for a failed backup email. */
class ResendPaymentEmailUseCase @Inject constructor(
    private val org: OrgContext,
    private val payments: PaymentRepository,
    private val backup: PaymentBackupScheduler,
) {
    suspend operator fun invoke(eventId: String, paymentId: String): Result<Unit> = domainResult {
        val orgId = org.orgId()
        payments.setEmailStatus(orgId, eventId, paymentId, EmailStatus.PENDING)
        backup.enqueue(orgId, eventId, paymentId)
    }
}

/** ORG-31: the WhatsApp text for an event as it stands now. */
class BuildWhatsAppMessageUseCase @Inject constructor(
    private val observeEventDues: ObserveEventDuesUseCase,
) {
    suspend operator fun invoke(eventId: String, today: LocalDate): Result<String> = domainResult {
        val dues = observeEventDues(eventId, today).first() ?: throw DomainException.NotFound()
        WhatsAppMessage.build(dues.event.name, today, dues.members)
    }
}

/** Payment methods in the order the form shows them. */
val PAYMENT_METHODS = listOf(PaymentMethod.OFFLINE, PaymentMethod.ONLINE)
