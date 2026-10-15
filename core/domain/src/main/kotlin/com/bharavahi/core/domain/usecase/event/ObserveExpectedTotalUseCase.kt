package com.bharavahi.core.domain.usecase.event

import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.repository.EventRepository
import com.bharavahi.core.domain.repository.OrgContext
import com.bharavahi.core.domain.repository.ScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** The expected total of every event of the community, summed, for the events summary card. Paise. */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveExpectedTotalUseCase @Inject constructor(
    private val org: OrgContext,
    private val events: EventRepository,
    private val schedules: ScheduleRepository,
) {
    operator fun invoke(): Flow<Long> = flow {
        val orgId = org.orgId()
        emitAll(
            events.observeAll(orgId)
                .map { list -> list.map { it.id } }
                .distinctUntilChanged()
                .flatMapLatest { ids ->
                    if (ids.isEmpty()) {
                        flowOf(0L)
                    } else {
                        combine(
                            ids.map { id ->
                                combine(events.observeMembers(orgId, id), schedules.observeAll(orgId, id), ::expectedTotal)
                            },
                        ) { it.sum() }
                    }
                },
        )
    }

    companion object {
        /** Every active member owes the full amount of every schedule (BR-4). */
        fun expectedTotal(members: List<EventMember>, schedules: List<Schedule>): Long =
            members.count { it.active } * schedules.sumOf { it.amountPerMember }
    }
}
