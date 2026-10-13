package com.bharavahi.org.navigation

import kotlinx.serialization.Serializable

@Serializable data object LoginRoute

// Bottom navigation tabs (UI-10).
@Serializable data object DashboardRoute
@Serializable data object EventsRoute
@Serializable data object MembersRoute
@Serializable data object ProfileRoute

@Serializable data object AddMemberRoute
@Serializable data class EditMemberRoute(val memberId: String)

@Serializable data object CreateEventRoute
@Serializable data class EditEventRoute(val eventId: String)
@Serializable data class EventDetailRoute(val eventId: String)
@Serializable data class AddEventMembersRoute(val eventId: String)

@Serializable data class AddScheduleRoute(val eventId: String)
@Serializable data class EditScheduleRoute(val eventId: String, val scheduleId: String)
@Serializable data class ScheduleRoute(val eventId: String, val scheduleId: String)

// Stage 2: payments, dues and expenses.
@Serializable data class MarkPaymentRoute(val eventId: String? = null, val memberId: String? = null)
@Serializable data class EventDuesRoute(val eventId: String)
@Serializable data class MemberDuesRoute(val eventId: String, val memberId: String)
@Serializable data class PaymentsRoute(val eventId: String)
@Serializable data class ExpensesRoute(val eventId: String)
@Serializable data class AddExpenseRoute(val eventId: String)
