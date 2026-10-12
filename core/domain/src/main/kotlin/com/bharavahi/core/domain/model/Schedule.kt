package com.bharavahi.core.domain.model

import java.time.LocalDate

/** A collection plan inside an event. Every event member owes [amountPerMember] (BR-4). */
data class Schedule(
    val id: String,
    val name: String,
    val description: String,
    val amountPerMember: Long,
    val numberOfDues: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dues: List<Due>,
    val locked: Boolean,
)

/** One instalment of a schedule. */
data class Due(
    val index: Int,
    val label: String,
    val dueDate: LocalDate,
    val amount: Long,
) {
    companion object {
        /** Key used in eventMembers.paidByDue. */
        fun key(scheduleId: String, dueIndex: Int): String = "${scheduleId}_$dueIndex"
    }
}

/** Input for creating or editing a schedule. Amount is in paise. */
data class ScheduleDetails(
    val name: String,
    val description: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val amountPerMember: Long,
    val numberOfDues: Int,
)

/** BR-5. */
enum class DueStatus { PAID, PARTLY_PAID, PENDING }

/** How many event members are in each status for one due (ORG-15). */
data class DueSummary(val due: Due, val paid: Int, val partlyPaid: Int, val pending: Int)
