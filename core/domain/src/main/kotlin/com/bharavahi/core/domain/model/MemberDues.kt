package com.bharavahi.core.domain.model

/** ORG-17: one event member's position across every schedule of the event. Amounts in paise. */
data class MemberDues(
    val member: EventMember,
    val totalPaid: Long,
    /** BR-5: sum of every due already due. */
    val dueSoFar: Long,
    /** What the member still owes of [dueSoFar]. */
    val pending: Long,
    /** Status on the current due of each schedule, combined. */
    val currentStatus: DueStatus,
    /** BR-5: past dues not fully paid. */
    val overdueCount: Int,
    /** Sum of amountPerMember across all schedules (ORG-33). */
    val eventTotal: Long,
) {
    val paidInFull: Boolean get() = eventTotal > 0 && totalPaid >= eventTotal
}

/** ORG-20: one due of one schedule for one member. */
data class MemberDueLine(
    val schedule: Schedule,
    val due: Due,
    val paid: Long,
    val status: DueStatus,
    val isPast: Boolean,
) {
    val owed: Long get() = (due.amount - paid).coerceAtLeast(0)

    /** BR-8 note: a due paid beyond its amount (two admins offline at once) needs a reversing entry. */
    val overpaid: Boolean get() = paid > due.amount
}

enum class MemberSort { MOST_BEHIND, AMOUNT_PAID, NAME }
