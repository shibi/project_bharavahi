package com.bharavahi.core.data.firebase

import com.bharavahi.core.domain.model.Due
import com.bharavahi.core.domain.model.EmailStatus
import com.bharavahi.core.domain.model.Expense
import com.bharavahi.core.domain.model.Payment
import com.bharavahi.core.domain.model.PaymentMethod
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.Member
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.model.Schedule
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate

// Dates without time are stored as "YYYY-MM-DD" strings (REQUIREMENTS.md section 6).
internal fun LocalDate?.toFirestore(): String? = this?.toString()

internal fun String?.toLocalDate(): LocalDate? = this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

internal fun MemberStatus.toFirestore(): String = name.lowercase()

internal fun String?.toMemberStatus(): MemberStatus =
    MemberStatus.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: MemberStatus.ACTIVE

internal fun EventStatus.toFirestore(): String = name.lowercase()

internal fun String?.toEventStatus(): EventStatus =
    EventStatus.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: EventStatus.ACTIVE

private fun Any?.asLong(): Long = (this as? Number)?.toLong() ?: 0L

private fun DocumentSnapshot.mapList(field: String): List<Map<*, *>> =
    (get(field) as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()

internal fun DocumentSnapshot.toMember(): Member = Member(
    id = id,
    name = getString(FirestorePaths.Member.NAME).orEmpty(),
    nickname = getString(FirestorePaths.Member.NICKNAME).orEmpty(),
    phone = getString(FirestorePaths.Member.PHONE).orEmpty(),
    email = getString(FirestorePaths.Member.EMAIL).orEmpty(),
    status = getString(FirestorePaths.Member.STATUS).toMemberStatus(),
    joinedAtMillis = getTimestamp(FirestorePaths.Member.JOINED_AT)?.toDate()?.time,
)

internal fun DocumentSnapshot.toEvent(): Event = Event(
    id = id,
    name = getString(FirestorePaths.Event.NAME).orEmpty(),
    description = getString(FirestorePaths.Event.DESCRIPTION).orEmpty(),
    startDate = getString(FirestorePaths.Event.START_DATE).toLocalDate(),
    endDate = getString(FirestorePaths.Event.END_DATE).toLocalDate(),
    openingBalance = getLong(FirestorePaths.Event.OPENING_BALANCE) ?: 0L,
    status = getString(FirestorePaths.Event.STATUS).toEventStatus(),
    roles = mapList(FirestorePaths.Event.ROLES).map {
        EventRole(id = it[FirestorePaths.Role.ID]?.toString().orEmpty(), name = it[FirestorePaths.Role.NAME]?.toString().orEmpty())
    },
    totalCollected = getLong(FirestorePaths.Event.TOTAL_COLLECTED) ?: 0L,
    totalExpenses = getLong(FirestorePaths.Event.TOTAL_EXPENSES) ?: 0L,
    createdAtMillis = getTimestamp(FirestorePaths.Event.CREATED_AT)?.toDate()?.time,
)

internal fun EventRole.toMap(): Map<String, Any> = mapOf(FirestorePaths.Role.ID to id, FirestorePaths.Role.NAME to name)

internal fun DocumentSnapshot.toEventMember(): EventMember = EventMember(
    memberId = id,
    name = getString(FirestorePaths.EventMember.NAME).orEmpty(),
    roleId = getString(FirestorePaths.EventMember.ROLE_ID),
    active = getBoolean(FirestorePaths.EventMember.ACTIVE) ?: true,
    totalPaid = getLong(FirestorePaths.EventMember.TOTAL_PAID) ?: 0L,
    paidByDue = (get(FirestorePaths.EventMember.PAID_BY_DUE) as? Map<*, *>).orEmpty()
        .entries.associate { (key, value) -> key.toString() to value.asLong() },
)

internal fun DocumentSnapshot.toSchedule(): Schedule = Schedule(
    id = id,
    name = getString(FirestorePaths.Schedule.NAME).orEmpty(),
    description = getString(FirestorePaths.Schedule.DESCRIPTION).orEmpty(),
    amountPerMember = getLong(FirestorePaths.Schedule.AMOUNT_PER_MEMBER) ?: 0L,
    numberOfDues = (getLong(FirestorePaths.Schedule.NUMBER_OF_DUES) ?: 0L).toInt(),
    startDate = getString(FirestorePaths.Schedule.START_DATE).toLocalDate() ?: LocalDate.MIN,
    endDate = getString(FirestorePaths.Schedule.END_DATE).toLocalDate() ?: LocalDate.MIN,
    dues = mapList(FirestorePaths.Schedule.DUES).map {
        Due(
            index = it[FirestorePaths.Due.INDEX].asLong().toInt(),
            label = it[FirestorePaths.Due.LABEL]?.toString().orEmpty(),
            dueDate = it[FirestorePaths.Due.DUE_DATE]?.toString().toLocalDate() ?: LocalDate.MIN,
            amount = it[FirestorePaths.Due.AMOUNT].asLong(),
        )
    }.sortedBy { it.index },
    locked = getBoolean(FirestorePaths.Schedule.LOCKED) ?: false,
)

internal fun Schedule.toMap(): Map<String, Any> = mapOf(
    FirestorePaths.Schedule.NAME to name,
    FirestorePaths.Schedule.DESCRIPTION to description,
    FirestorePaths.Schedule.AMOUNT_PER_MEMBER to amountPerMember,
    FirestorePaths.Schedule.NUMBER_OF_DUES to numberOfDues,
    FirestorePaths.Schedule.START_DATE to startDate.toString(),
    FirestorePaths.Schedule.END_DATE to endDate.toString(),
    FirestorePaths.Schedule.DUES to dues.map {
        mapOf(
            FirestorePaths.Due.INDEX to it.index,
            FirestorePaths.Due.LABEL to it.label,
            FirestorePaths.Due.DUE_DATE to it.dueDate.toString(),
            FirestorePaths.Due.AMOUNT to it.amount,
        )
    },
    FirestorePaths.Schedule.LOCKED to locked,
)

internal fun EmailStatus.toFirestore(): String = name.lowercase()

internal fun PaymentMethod.toFirestore(): String = name.lowercase()

internal fun DocumentSnapshot.toPayment(): Payment = Payment(
    id = id,
    memberId = getString(FirestorePaths.Payment.MEMBER_ID).orEmpty(),
    scheduleId = getString(FirestorePaths.Payment.SCHEDULE_ID).orEmpty(),
    dueIndex = (getLong(FirestorePaths.Payment.DUE_INDEX) ?: 0L).toInt(),
    amount = getLong(FirestorePaths.Payment.AMOUNT) ?: 0L,
    method = PaymentMethod.entries.firstOrNull { it.name.equals(getString(FirestorePaths.Payment.METHOD), ignoreCase = true) }
        ?: PaymentMethod.OFFLINE,
    note = getString(FirestorePaths.Payment.NOTE).orEmpty(),
    paidOn = getString(FirestorePaths.Payment.PAID_ON).toLocalDate(),
    recordedBy = getString(FirestorePaths.Payment.RECORDED_BY).orEmpty(),
    createdAtMillis = getTimestamp(FirestorePaths.Payment.CREATED_AT)?.toDate()?.time,
    reversesPaymentId = getString(FirestorePaths.Payment.REVERSES_PAYMENT_ID),
    emailStatus = EmailStatus.entries.firstOrNull { it.name.equals(getString(FirestorePaths.Payment.EMAIL_STATUS), ignoreCase = true) }
        ?: EmailStatus.PENDING,
    waitingToSync = metadata.hasPendingWrites(),
)

internal fun DocumentSnapshot.toExpense(): Expense = Expense(
    id = id,
    title = getString(FirestorePaths.Expense.TITLE).orEmpty(),
    amount = getLong(FirestorePaths.Expense.AMOUNT) ?: 0L,
    date = getString(FirestorePaths.Expense.DATE).toLocalDate(),
    description = getString(FirestorePaths.Expense.DESCRIPTION).orEmpty(),
    paidByMemberId = getString(FirestorePaths.Expense.PAID_BY_MEMBER_ID),
    recordedBy = getString(FirestorePaths.Expense.RECORDED_BY).orEmpty(),
    createdAtMillis = getTimestamp(FirestorePaths.Expense.CREATED_AT)?.toDate()?.time,
    waitingToSync = metadata.hasPendingWrites(),
)
