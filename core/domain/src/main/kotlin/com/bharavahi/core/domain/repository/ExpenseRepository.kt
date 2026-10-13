package com.bharavahi.core.domain.repository

import com.bharavahi.core.domain.model.Expense
import com.bharavahi.core.domain.model.NewExpense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    /** Expenses of an event, newest first. */
    fun observeAll(orgId: String, eventId: String): Flow<List<Expense>>

    /** ORG-26: creates the expense and increments event.totalExpenses in one batch. Returns the id. */
    suspend fun add(orgId: String, eventId: String, expense: NewExpense): String
}
