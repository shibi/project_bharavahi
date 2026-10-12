package com.bharavahi.org.ui.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.DueSummary
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.rules.DueStatusRules
import com.bharavahi.core.domain.usecase.schedule.DeleteScheduleUseCase
import com.bharavahi.core.domain.usecase.schedule.ObserveScheduleSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ScheduleUiState(
    val loading: Boolean = true,
    val schedule: Schedule? = null,
    val dues: List<DueSummary> = emptyList(),
    /** Index of the current due (BR-5), highlighted in the list. */
    val currentDueIndex: Int? = null,
    val error: Throwable? = null,
    val actionError: Throwable? = null,
)

/** ORG-15: each due with its paid / partly paid / pending counts. ORG-16: edit or delete while unlocked. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSummary: ObserveScheduleSummaryUseCase,
    private val deleteSchedule: DeleteScheduleUseCase,
) : ViewModel() {

    val eventId: String = checkNotNull(savedStateHandle["eventId"])
    val scheduleId: String = checkNotNull(savedStateHandle["scheduleId"])

    private val retry = MutableStateFlow(0)
    private val actionError = MutableStateFlow<Throwable?>(null)
    private var deleting = false

    val state: StateFlow<ScheduleUiState> = retry.flatMapLatest {
        observeSummary(eventId, scheduleId)
            .map { summary ->
                if (summary == null) {
                    ScheduleUiState(loading = false, error = DomainException.NotFound())
                } else {
                    val (schedule, dues) = summary
                    ScheduleUiState(
                        loading = false,
                        schedule = schedule,
                        dues = dues,
                        currentDueIndex = DueStatusRules.currentDue(schedule.dues, LocalDate.now())?.index,
                    )
                }
            }
            .catch { emit(ScheduleUiState(loading = false, error = it)) }
    }.combine(actionError) { s, e -> s.copy(actionError = e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    fun retry() { retry.value++ }
    fun dismissError() { actionError.value = null }

    fun delete() {
        if (deleting) return
        deleting = true
        viewModelScope.launch {
            deleteSchedule(eventId, scheduleId)
                .onSuccess { _deleted.send(Unit) }
                .onFailure { actionError.value = it }
            deleting = false
        }
    }
}
