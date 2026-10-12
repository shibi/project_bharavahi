package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.model.Due
import com.bharavahi.core.domain.model.DueStatus
import com.bharavahi.core.domain.model.DueSummary
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.Schedule
import java.time.LocalDate

/** BR-5: how far along each member is with their dues. */
object DueStatusRules {

    fun statusOf(paid: Long, amount: Long): DueStatus = when {
        paid >= amount -> DueStatus.PAID
        paid > 0 -> DueStatus.PARTLY_PAID
        else -> DueStatus.PENDING
    }

    /** The latest due on or before [today]; before the first due date, the first due. */
    fun currentDue(dues: List<Due>, today: LocalDate): Due? =
        dues.lastOrNull { !it.dueDate.isAfter(today) } ?: dues.firstOrNull()

    /** ORG-15: counts per due. Disabled members are left out (BR-10). */
    fun summarize(schedule: Schedule, members: List<EventMember>): List<DueSummary> {
        val counted = members.filter { it.active }
        return schedule.dues.map { due ->
            val statuses = counted.map { statusOf(it.paidOn(schedule.id, due.index), due.amount) }
            DueSummary(
                due = due,
                paid = statuses.count { it == DueStatus.PAID },
                partlyPaid = statuses.count { it == DueStatus.PARTLY_PAID },
                pending = statuses.count { it == DueStatus.PENDING },
            )
        }
    }
}
