package com.bharavahi.org.ui.memberdues

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.usecase.payment.MemberDuesDetail
import com.bharavahi.core.domain.usecase.payment.ObserveMemberDuesUseCase
import com.bharavahi.core.domain.usecase.payment.ReversePaymentUseCase
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

data class MemberDuesUiState(
    val loading: Boolean = true,
    val detail: MemberDuesDetail? = null,
    val error: Throwable? = null,
    val actionError: Throwable? = null,
)

/** ORG-20: one member's dues line by line and their payment history, with reversal (ORG-25). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MemberDuesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeMemberDues: ObserveMemberDuesUseCase,
    private val reversePayment: ReversePaymentUseCase,
) : ViewModel() {

    val eventId: String = checkNotNull(savedStateHandle["eventId"])
    val memberId: String = checkNotNull(savedStateHandle["memberId"])

    private val retry = MutableStateFlow(0)
    private val actionError = MutableStateFlow<Throwable?>(null)

    val state: StateFlow<MemberDuesUiState> = retry.flatMapLatest {
        observeMemberDues(eventId, memberId, LocalDate.now())
            .map { MemberDuesUiState(loading = false, detail = it, error = if (it == null) DomainException.NotFound() else null) }
            .catch { emit(MemberDuesUiState(loading = false, error = it)) }
    }.combine(actionError) { s, e -> s.copy(actionError = e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemberDuesUiState())

    fun retry() { retry.value++ }
    fun dismissError() { actionError.value = null }

    fun reverse(paymentId: String) {
        viewModelScope.launch {
            reversePayment(eventId, paymentId, LocalDate.now()).onFailure { actionError.value = it }
        }
    }
}
