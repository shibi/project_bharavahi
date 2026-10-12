package com.bharavahi.org.ui.scheduleform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.Due
import com.bharavahi.core.domain.model.ScheduleDetails
import com.bharavahi.core.domain.usecase.event.ObserveEventUseCase
import com.bharavahi.core.domain.usecase.schedule.CreateScheduleUseCase
import com.bharavahi.core.domain.usecase.schedule.ObserveScheduleUseCase
import com.bharavahi.core.domain.usecase.schedule.PreviewDuesUseCase
import com.bharavahi.core.domain.usecase.schedule.UpdateScheduleUseCase
import com.bharavahi.core.domain.validation.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ScheduleFormUiState(
    val isEdit: Boolean,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val locked: Boolean = false,
    val name: String = "",
    val description: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val amount: String = "",
    val numberOfDues: String = "",
    /** ORG-14: the dues these inputs would create, or empty while the inputs are incomplete. */
    val preview: List<Due> = emptyList(),
    val fieldErrors: Map<Field, ValidationError> = emptyMap(),
    val error: Throwable? = null,
)

/** ORG-14 (create with preview) and ORG-16 (edit only while unlocked). */
@HiltViewModel
class ScheduleFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeEvent: ObserveEventUseCase,
    private val observeSchedule: ObserveScheduleUseCase,
    private val previewDues: PreviewDuesUseCase,
    private val createSchedule: CreateScheduleUseCase,
    private val updateSchedule: UpdateScheduleUseCase,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])
    private val scheduleId: String? = savedStateHandle["scheduleId"]

    private val _state = MutableStateFlow(ScheduleFormUiState(isEdit = scheduleId != null))
    val state: StateFlow<ScheduleFormUiState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        viewModelScope.launch { if (scheduleId != null) loadSchedule(scheduleId) else prefillFromEvent() }
    }

    /** New schedules start with the event's dates, the most common case. */
    private suspend fun prefillFromEvent() {
        val event = runCatching { observeEvent(eventId).firstOrNull() }.getOrNull()
        update { it.copy(loading = false, startDate = event?.startDate, endDate = event?.endDate) }
    }

    private suspend fun loadSchedule(id: String) {
        val schedule = runCatching { observeSchedule(eventId, id).firstOrNull() }.getOrNull()
        if (schedule == null) {
            _state.update { it.copy(loading = false, error = DomainException.NotFound()) }
            return
        }
        update {
            it.copy(
                loading = false,
                locked = schedule.locked,
                name = schedule.name,
                description = schedule.description,
                startDate = schedule.startDate,
                endDate = schedule.endDate,
                amount = Money.toInput(schedule.amountPerMember),
                numberOfDues = schedule.numberOfDues.toString(),
                error = if (schedule.locked) DomainException.ScheduleLocked() else null,
            )
        }
    }

    fun onChange(field: Field, value: String) = update {
        val updated = when (field) {
            Field.NAME -> it.copy(name = value)
            Field.DESCRIPTION -> it.copy(description = value)
            Field.AMOUNT -> it.copy(amount = value)
            Field.NUMBER_OF_DUES -> it.copy(numberOfDues = value.filter(Char::isDigit))
            else -> it
        }
        updated.copy(fieldErrors = updated.fieldErrors - field)
    }

    fun onStartDate(date: LocalDate) = update { it.copy(startDate = date, fieldErrors = it.fieldErrors - Field.START_DATE) }
    fun onEndDate(date: LocalDate) = update { it.copy(endDate = date, fieldErrors = it.fieldErrors - Field.END_DATE) }

    fun save() {
        val s = _state.value
        if (s.saving || s.loading || s.locked) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val details = s.toDetails()
            val result = if (scheduleId != null) {
                updateSchedule(eventId, scheduleId, details)
            } else {
                createSchedule(eventId, details).map { }
            }
            result
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

    /** Applies [change] and refreshes the preview. */
    private fun update(change: (ScheduleFormUiState) -> ScheduleFormUiState) = _state.update {
        val next = change(it)
        // The name doesn't affect the dues, so preview as soon as the numbers and dates are in.
        val details = next.toDetails().let { d -> d.copy(name = d.name.ifBlank { "-" }) }
        next.copy(preview = previewDues(details).getOrDefault(emptyList()))
    }

    private fun ScheduleFormUiState.toDetails() = ScheduleDetails(
        name = name,
        description = description,
        startDate = startDate,
        endDate = endDate,
        amountPerMember = Money.parseRupees(amount) ?: -1L,
        numberOfDues = numberOfDues.toIntOrNull() ?: 0,
    )
}
