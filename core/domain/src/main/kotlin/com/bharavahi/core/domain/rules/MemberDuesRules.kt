package com.bharavahi.core.domain.rules

import com.bharavahi.core.domain.model.DueStatus
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.MemberDueLine
import com.bharavahi.core.domain.model.MemberDues
import com.bharavahi.core.domain.model.MemberSort
import com.bharavahi.core.domain.model.Schedule
import java.time.LocalDate

/** BR-5 and ORG-17 to ORG-20: where each event member stands. */
object MemberDuesRules {

    /** ORG-20: every due of every schedule for one member, in schedule then due order. */
    fun lines(member: EventMember, schedules: List<Schedule>, today: LocalDate): List<MemberDueLine> =
        schedules.flatMap { schedule ->
            schedule.dues.map { due ->
                val paid = member.paidOn(schedule.id, due.index)
                MemberDueLine(
                    schedule = schedule,
                    due = due,
                    paid = paid,
                    status = DueStatusRules.statusOf(paid, due.amount),
                    isPast = due.dueDate.isBefore(today),
                )
            }
        }

    fun compute(member: EventMember, schedules: List<Schedule>, today: LocalDate): MemberDues {
        val lines = lines(member, schedules, today)
        val due = lines.filter { !it.due.dueDate.isAfter(today) }
        val current = schedules.mapNotNull { schedule ->
            DueStatusRules.currentDue(schedule.dues, today)?.let { d ->
                DueStatusRules.statusOf(member.paidOn(schedule.id, d.index), d.amount)
            }
        }
        return MemberDues(
            member = member,
            totalPaid = member.totalPaid,
            dueSoFar = due.sumOf { it.due.amount },
            pending = due.sumOf { it.owed },
            currentStatus = combine(current),
            overdueCount = lines.count { it.isPast && it.status != DueStatus.PAID },
            eventTotal = schedules.sumOf { it.amountPerMember },
        )
    }

    fun computeAll(members: List<EventMember>, schedules: List<Schedule>, today: LocalDate): List<MemberDues> =
        members.map { compute(it, schedules, today) }

    /**
     * ORG-18 and ORG-19. A status filter is a pending list, so disabled members drop out of it (BR-10);
     * with no status filter everyone is listed.
     */
    fun filterAndSort(
        dues: List<MemberDues>,
        status: DueStatus?,
        roleId: String?,
        sort: MemberSort,
    ): List<MemberDues> {
        val filtered = dues.filter { d ->
            (status == null || (d.member.active && d.currentStatus == status)) &&
                (roleId == null || d.member.roleId == roleId)
        }
        val byName = compareBy<MemberDues> { it.member.name.lowercase() }
        return when (sort) {
            MemberSort.MOST_BEHIND -> filtered.sortedWith(
                compareByDescending<MemberDues> { it.overdueCount }.thenByDescending { it.pending }.then(byName),
            )
            MemberSort.AMOUNT_PAID -> filtered.sortedWith(compareByDescending<MemberDues> { it.totalPaid }.then(byName))
            MemberSort.NAME -> filtered.sortedWith(byName)
        }
    }

    /** Paid only when every current due is paid; Pending when nothing is paid on any of them. */
    private fun combine(statuses: List<DueStatus>): DueStatus = when {
        statuses.isEmpty() || statuses.all { it == DueStatus.PAID } -> DueStatus.PAID
        statuses.all { it == DueStatus.PENDING } -> DueStatus.PENDING
        else -> DueStatus.PARTLY_PAID
    }
}
