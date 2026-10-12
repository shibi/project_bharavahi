package com.bharavahi.core.domain.model

import java.time.LocalDate

/** Something the community collects money for, e.g. an annual celebration. Amounts are in paise. */
data class Event(
    val id: String,
    val name: String,
    val description: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val openingBalance: Long,
    val status: EventStatus,
    val roles: List<EventRole>,
    val totalCollected: Long,
    val totalExpenses: Long,
    val createdAtMillis: Long?,
) {
    /** BR-9. */
    val balance: Long get() = openingBalance + totalCollected - totalExpenses

    fun roleName(roleId: String?): String? = roles.firstOrNull { it.id == roleId }?.name
}

enum class EventStatus { ACTIVE, SUSPENDED, ARCHIVED, CLOSED }

/** A position inside one event, e.g. President or Secretary. */
data class EventRole(val id: String, val name: String)

/** Input for creating or editing an event's details. */
data class EventDetails(
    val name: String,
    val description: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val openingBalance: Long,
)

/** A community member taking part in an event, with their running totals. */
data class EventMember(
    val memberId: String,
    val name: String,
    val roleId: String?,
    val active: Boolean,
    val totalPaid: Long,
    /** Key is [Due.key], value is paise paid on that due. */
    val paidByDue: Map<String, Long>,
) {
    fun paidOn(scheduleId: String, dueIndex: Int): Long = paidByDue[Due.key(scheduleId, dueIndex)] ?: 0L
}

/** Adds a member to an event with an optional role. */
data class EventMemberAssignment(val memberId: String, val name: String, val roleId: String?)

/** Everything collected by the create-event flow (ORG-11). */
data class NewEvent(
    val details: EventDetails,
    val roles: List<EventRole>,
    val members: List<EventMemberAssignment>,
)
