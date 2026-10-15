package com.bharavahi.core.domain.usecase.orgdashboard

import com.bharavahi.core.domain.model.DueStatus
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.model.Payment
import com.bharavahi.core.domain.repository.EventRepository
import com.bharavahi.core.domain.repository.MemberRepository
import com.bharavahi.core.domain.repository.OrgContext
import com.bharavahi.core.domain.repository.OrganizationRepository
import com.bharavahi.core.domain.repository.PaymentRepository
import com.bharavahi.core.domain.repository.ScheduleRepository
import com.bharavahi.core.domain.rules.DueStatusRules
import com.bharavahi.core.domain.rules.MemberDuesRules
import com.bharavahi.core.domain.usecase.event.ObserveExpectedTotalUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** One active event's progress on the dashboard. Amounts in paise. */
data class EventProgress(
    val event: Event,
    /** Every active member's full total across all schedules. */
    val expected: Long,
    /** Paid on the current due of each schedule (BR-5). */
    val collectedThisPeriod: Long,
    /** Active members whose current due is not fully paid. */
    val pendingMembers: Int,
) {
    /** 0..1 share of [expected] collected so far, for the progress ring (UI-11). */
    val progress: Float get() = if (expected <= 0) 0f else (event.totalCollected.toFloat() / expected).coerceIn(0f, 1f)
}

data class RecentPayment(val eventId: String, val eventName: String, val memberName: String, val payment: Payment)

data class OrgDashboard(
    val communityName: String,
    val activeMembers: Int,
    val events: List<EventProgress>,
    val recentPayments: List<RecentPayment>,
) {
    val activeEvents: List<Event> get() = events.map { it.event }

    /** 0..1 share of every active event's expected total collected so far, for the hero ring (UI-11). */
    val progress: Float get() {
        val expected = events.sumOf { it.expected }
        return if (expected <= 0) 0f else (activeEvents.sumOf { it.totalCollected }.toFloat() / expected).coerceIn(0f, 1f)
    }
}

/** ORG-5: member count, active events with their progress, pending current dues and recent payments. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveOrgDashboardUseCase @Inject constructor(
    private val org: OrgContext,
    private val organizations: OrganizationRepository,
    private val members: MemberRepository,
    private val events: EventRepository,
    private val schedules: ScheduleRepository,
    private val payments: PaymentRepository,
) {
    operator fun invoke(today: LocalDate): Flow<OrgDashboard> = flow {
        val orgId = org.orgId()
        val activeEvents = events.observeAll(orgId)
            .map { list -> list.filter { it.status == EventStatus.ACTIVE } }
        val progress = activeEvents
            .map { list -> list.map { it.id } }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(ids.map { eventFlow(orgId, it, today) }) { it.filterNotNull() }
                }
            }
        emitAll(
            combine(organizations.observe(orgId), members.observeAll(orgId), progress) { organization, memberList, perEvent ->
                OrgDashboard(
                    communityName = organization?.name.orEmpty(),
                    activeMembers = memberList.count { it.status == MemberStatus.ACTIVE },
                    events = perEvent.map { it.first },
                    recentPayments = perEvent.flatMap { it.second }
                        .sortedByDescending { it.payment.createdAtMillis ?: Long.MAX_VALUE }
                        .take(RECENT_PAYMENTS),
                )
            },
        )
    }

    private fun eventFlow(orgId: String, eventId: String, today: LocalDate): Flow<Pair<EventProgress, List<RecentPayment>>?> =
        combine(
            events.observe(orgId, eventId),
            schedules.observeAll(orgId, eventId),
            events.observeMembers(orgId, eventId),
            payments.observeAll(orgId, eventId),
        ) { event, scheduleList, memberList, paymentList ->
            if (event == null) return@combine null
            val active = memberList.filter { it.active }
            val dues = MemberDuesRules.computeAll(active, scheduleList, today)
            val currentDues = scheduleList.mapNotNull { s -> DueStatusRules.currentDue(s.dues, today)?.let { s.id to it.index } }
            val names = memberList.associate { it.memberId to it.name }
            EventProgress(
                event = event,
                expected = ObserveExpectedTotalUseCase.expectedTotal(memberList, scheduleList),
                collectedThisPeriod = active.sumOf { m -> currentDues.sumOf { (sid, idx) -> m.paidOn(sid, idx) } },
                pendingMembers = dues.count { it.currentStatus != DueStatus.PAID },
            ) to paymentList.take(RECENT_PAYMENTS).map {
                RecentPayment(event.id, event.name, names[it.memberId] ?: "", it)
            }
        }

    private companion object {
        const val RECENT_PAYMENTS = 5
    }
}
