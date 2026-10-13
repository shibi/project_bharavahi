package com.bharavahi.core.domain.model

import java.time.LocalDate

/** Money spent from an event's funds (ORG-26). Amount is in paise. */
data class Expense(
    val id: String,
    val title: String,
    val amount: Long,
    val date: LocalDate?,
    val description: String,
    val paidByMemberId: String?,
    val recordedBy: String,
    val createdAtMillis: Long?,
    val waitingToSync: Boolean,
)

data class NewExpense(
    val title: String,
    val amount: Long,
    val date: LocalDate?,
    val description: String,
    val paidByMemberId: String?,
)
