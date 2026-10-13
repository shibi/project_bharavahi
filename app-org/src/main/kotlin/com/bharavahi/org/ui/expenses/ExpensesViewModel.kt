package com.bharavahi.org.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.Expense
import com.bharavahi.core.domain.model.NewExpense
import com.bharavahi.core.domain.usecase.event.ObserveEventMembersUseCase
import com.bharavahi.core.domain.usecase.expense.AddExpenseUseCase
import com.bharavahi.core.domain.usecase.expense.ObserveExpensesUseCase
import com.bharavahi.core.domain.validation.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ExpensesUiState(
    val loading: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val members: List<EventMember> = emptyList(),
    val error: Throwable? = null,
) {
    val total: Long get() = expenses.sumOf { it.amount }
}

/** ORG-27: the event's expenses, newest first, with the total. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeExpenses: ObserveExpensesUseCase,
    observeMembers: ObserveEventMembersUseCase,
) : ViewModel() {

    val eventId: String = checkNotNull(savedStateHandle["eventId"])
    private val retry = MutableStateFlow(0)

    val state: StateFlow<ExpensesUiState> = retry.flatMapLatest {
        combine(observeExpenses(eventId), observeMembers(eventId)) { e, m -> ExpensesUiState(loading = false, expenses = e, members = m) }
            .catch { emit(ExpensesUiState(loading = false, error = it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesUiState())

    fun retry() { retry.value++ }
}

data class AddExpenseUiState(
    val saving: Boolean = false,
    val members: List<EventMember> = emptyList(),
    val title: String = "",
    val amount: String = "",
    val date: LocalDate? = LocalDate.now(),
    val description: String = "",
    val paidByMemberId: String? = null,
    val fieldErrors: Map<Field, ValidationError> = emptyMap(),
    val error: Throwable? = null,
)

/** ORG-26. */
@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeMembers: ObserveEventMembersUseCase,
    private val addExpense: AddExpenseUseCase,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _state = MutableStateFlow(AddExpenseUiState())
    val state: StateFlow<AddExpenseUiState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeMembers(eventId).map { list -> list.filter { it.active } }.catch { }.collect { members ->
                _state.update { it.copy(members = members) }
            }
        }
    }

    fun onChange(field: Field, value: String) = _state.update {
        val updated = when (field) {
            Field.TITLE -> it.copy(title = value)
            Field.AMOUNT -> it.copy(amount = value)
            Field.DESCRIPTION -> it.copy(description = value)
            else -> it
        }
        updated.copy(fieldErrors = updated.fieldErrors - field)
    }

    fun onDate(date: LocalDate) = _state.update { it.copy(date = date, fieldErrors = it.fieldErrors - Field.DATE) }
    fun onPaidBy(memberId: String?) = _state.update { it.copy(paidByMemberId = memberId) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true, error = null) }
        val expense = NewExpense(s.title, Money.parseRupees(s.amount) ?: -1L, s.date, s.description, s.paidByMemberId)
        viewModelScope.launch {
            addExpense(eventId, expense)
                .onSuccess {
                    _state.update { it.copy(saving = false) }
                    _saved.send(Unit)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            saving = false,
                            fieldErrors = (e as? DomainException.Validation)?.errors.orEmpty(),
                            error = e.takeUnless { e is DomainException.Validation },
                        )
                    }
                }
        }
    }
}
