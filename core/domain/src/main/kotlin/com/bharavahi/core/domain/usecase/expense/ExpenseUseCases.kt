package com.bharavahi.core.domain.usecase.expense

import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.Expense
import com.bharavahi.core.domain.model.NewExpense
import com.bharavahi.core.domain.repository.ExpenseRepository
import com.bharavahi.core.domain.repository.OrgContext
import com.bharavahi.core.domain.usecase.domainResult
import com.bharavahi.core.domain.validation.Validators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** ORG-27. */
class ObserveExpensesUseCase @Inject constructor(
    private val org: OrgContext,
    private val expenses: ExpenseRepository,
) {
    operator fun invoke(eventId: String): Flow<List<Expense>> = flow { emitAll(expenses.observeAll(org.orgId(), eventId)) }
}

/** ORG-26. Returns the expense id. */
class AddExpenseUseCase @Inject constructor(
    private val org: OrgContext,
    private val expenses: ExpenseRepository,
) {
    suspend operator fun invoke(eventId: String, expense: NewExpense): Result<String> = domainResult {
        val errors = buildMap<Field, ValidationError> {
            Validators.required(expense.title)?.let { put(Field.TITLE, it) }
            if (expense.amount <= 0) put(Field.AMOUNT, ValidationError.INVALID_AMOUNT)
            if (expense.date == null) put(Field.DATE, ValidationError.REQUIRED)
        }
        if (errors.isNotEmpty()) throw DomainException.Validation(errors)
        expenses.add(
            org.orgId(),
            eventId,
            expense.copy(title = expense.title.trim(), description = expense.description.trim()),
        )
    }
}
