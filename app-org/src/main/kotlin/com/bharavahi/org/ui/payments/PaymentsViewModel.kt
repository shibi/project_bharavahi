package com.bharavahi.org.ui.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.Payment
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.usecase.event.ObserveEventMembersUseCase
import com.bharavahi.core.domain.usecase.payment.ObservePaymentsUseCase
import com.bharavahi.core.domain.usecase.payment.ResendPaymentEmailUseCase
import com.bharavahi.core.domain.usecase.payment.ReversePaymentUseCase
import com.bharavahi.core.domain.usecase.schedule.ObserveSchedulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PaymentsUiState(
    val loading: Boolean = true,
    val members: List<EventMember> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    val memberFilter: String? = null,
    val scheduleFilter: String? = null,
    val totalCount: Int = 0,
    val visible: List<Payment> = emptyList(),
    val reversedIds: Set<String> = emptySet(),
    val error: Throwable? = null,
    val actionError: Throwable? = null,
)

private data class Inputs(val payments: List<Payment>, val members: List<EventMember>, val schedules: List<Schedule>)

/** ORG-24 and ORG-25: the event's payments, newest first, with filters, Resend and reversal. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PaymentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePayments: ObservePaymentsUseCase,
    observeMembers: ObserveEventMembersUseCase,
    observeSchedules: ObserveSchedulesUseCase,
    private val reversePayment: ReversePaymentUseCase,
    private val resendEmail: ResendPaymentEmailUseCase,
) : ViewModel() {

    val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val memberFilter = MutableStateFlow<String?>(null)
    private val scheduleFilter = MutableStateFlow<String?>(null)
    private val retry = MutableStateFlow(0)
    private val actionError = MutableStateFlow<Throwable?>(null)

    private val inputs = retry.flatMapLatest {
        combine(observePayments(eventId), observeMembers(eventId), observeSchedules(eventId)) { p, m, s -> Inputs(p, m, s) }
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }

    val state: StateFlow<PaymentsUiState> =
        combine(inputs, memberFilter, scheduleFilter, actionError) { result, member, schedule, actionErr ->
            val i = result.getOrNull()
            val all = i?.payments.orEmpty()
            PaymentsUiState(
                loading = false,
                members = i?.members.orEmpty(),
                schedules = i?.schedules.orEmpty(),
                memberFilter = member,
                scheduleFilter = schedule,
                totalCount = all.size,
                visible = ObservePaymentsUseCase.filter(all, member, schedule),
                reversedIds = ObservePaymentsUseCase.reversedIds(all),
                error = result.exceptionOrNull(),
                actionError = actionErr,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaymentsUiState())

    fun onMemberFilter(memberId: String?) { memberFilter.value = memberId }
    fun onScheduleFilter(scheduleId: String?) { scheduleFilter.value = scheduleId }
    fun retry() { retry.value++ }
    fun dismissError() { actionError.value = null }

    fun reverse(paymentId: String) = launchAction { reversePayment(eventId, paymentId, LocalDate.now()).map { } }
    fun resend(paymentId: String) = launchAction { resendEmail(eventId, paymentId) }

    private fun launchAction(block: suspend () -> Result<Unit>) {
        viewModelScope.launch { block().onFailure { actionError.value = it } }
    }
}
