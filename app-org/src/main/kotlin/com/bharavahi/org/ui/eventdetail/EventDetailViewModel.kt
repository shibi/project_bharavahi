package com.bharavahi.org.ui.eventdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.Expense
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.domain.usecase.event.EventRoles
import com.bharavahi.core.domain.usecase.event.ObserveEventMembersUseCase
import com.bharavahi.core.domain.usecase.event.ObserveEventUseCase
import com.bharavahi.core.domain.usecase.event.RemoveEventMemberUseCase
import com.bharavahi.core.domain.usecase.event.SetEventMemberRoleUseCase
import com.bharavahi.core.domain.usecase.event.SetEventStatusUseCase
import com.bharavahi.core.domain.usecase.event.UpdateEventRolesUseCase
import com.bharavahi.core.domain.usecase.expense.ObserveExpensesUseCase
import com.bharavahi.core.domain.rules.EventPdfReport
import com.bharavahi.core.domain.rules.MemberDuesRules
import com.bharavahi.core.domain.usecase.payment.BuildEventPdfReportUseCase
import com.bharavahi.core.domain.usecase.payment.BuildWhatsAppMessageUseCase
import com.bharavahi.core.domain.usecase.schedule.ObserveSchedulesUseCase
import com.bharavahi.core.domain.usecase.sheets.ObserveSheetsStateUseCase
import com.bharavahi.core.domain.usecase.sheets.SheetsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class EventDetailUiState(
    val loading: Boolean = true,
    val event: Event? = null,
    val schedules: List<Schedule> = emptyList(),
    val members: List<EventMember> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    /** What active members still owe on dues already due (BR-5), for the overview card. */
    val pendingDues: Long = 0L,
    val error: Throwable? = null,
    /** A failed action (status change, role, remove); shown as a message, the screen stays. */
    val actionError: Throwable? = null,
)

/** Roles being edited in the dialog (ORG-12). */
data class RolesDialogState(
    val roles: List<EventRole>,
    val newRoleName: String = "",
    val error: ValidationError? = null,
    val saving: Boolean = false,
)

/** ORG-12 and ORG-13: event overview, its schedules, roles and members. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeEvent: ObserveEventUseCase,
    observeSchedules: ObserveSchedulesUseCase,
    observeEventMembers: ObserveEventMembersUseCase,
    observeExpenses: ObserveExpensesUseCase,
    private val setEventStatus: SetEventStatusUseCase,
    private val updateEventRoles: UpdateEventRolesUseCase,
    private val setEventMemberRole: SetEventMemberRoleUseCase,
    private val removeEventMember: RemoveEventMemberUseCase,
    private val buildWhatsApp: BuildWhatsAppMessageUseCase,
    private val buildPdfReport: BuildEventPdfReportUseCase,
    observeSheets: ObserveSheetsStateUseCase,
) : ViewModel() {

    /** ORG-36: whether sheet backup is on, for the Google Sheet row. Null until known. */
    val sheets: StateFlow<SheetsState?> = observeSheets()
        .map<SheetsState, SheetsState?> { it }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val retry = MutableStateFlow(0)
    private val actionError = MutableStateFlow<Throwable?>(null)

    val state: StateFlow<EventDetailUiState> = retry.flatMapLatest {
        combine(
            observeEvent(eventId),
            observeSchedules(eventId),
            observeEventMembers(eventId),
            observeExpenses(eventId),
        ) { event, schedules, members, expenses ->
            EventDetailUiState(
                loading = false,
                event = event,
                schedules = schedules,
                members = members,
                expenses = expenses,
                pendingDues = MemberDuesRules.computeAll(members.filter { it.active }, schedules, LocalDate.now()).sumOf { it.pending },
                error = if (event == null) DomainException.NotFound() else null,
            )
        }.catch { emit(EventDetailUiState(loading = false, error = it)) }
    }.combine(actionError) { s, e -> s.copy(actionError = e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventDetailUiState())

    private val _rolesDialog = MutableStateFlow<RolesDialogState?>(null)
    val rolesDialog: StateFlow<RolesDialogState?> = _rolesDialog.asStateFlow()

    fun retry() {
        actionError.value = null
        retry.value++
    }

    fun dismissError() { actionError.value = null }

    /** ORG-31. */
    suspend fun whatsAppText(): Result<String> = buildWhatsApp(eventId, LocalDate.now())

    /** ORG-34. */
    suspend fun pdfReport(): Result<EventPdfReport> = buildPdfReport(eventId, LocalDate.now())

    fun setStatus(status: EventStatus) = launchAction { setEventStatus(eventId, status) }

    fun setMemberRole(memberId: String, roleId: String?) = launchAction { setEventMemberRole(eventId, memberId, roleId) }

    fun removeMember(memberId: String) = launchAction { removeEventMember(eventId, memberId) }

    fun openRolesDialog() {
        _rolesDialog.value = RolesDialogState(roles = state.value.event?.roles.orEmpty())
    }

    fun closeRolesDialog() { _rolesDialog.value = null }

    fun onNewRoleNameChange(value: String) = _rolesDialog.update { it?.copy(newRoleName = value) }

    fun addRole() = _rolesDialog.update {
        if (it == null || it.newRoleName.isBlank()) it
        else it.copy(roles = it.roles + EventRoles.newRole(it.newRoleName.trim()), newRoleName = "", error = null)
    }

    fun renameRole(id: String, name: String) = _rolesDialog.update { d ->
        d?.copy(roles = d.roles.map { if (it.id == id) it.copy(name = name) else it }, error = null)
    }

    fun removeRole(id: String) = _rolesDialog.update { d -> d?.copy(roles = d.roles.filterNot { it.id == id }, error = null) }

    fun saveRoles() {
        val dialog = _rolesDialog.value ?: return
        if (dialog.saving) return
        _rolesDialog.value = dialog.copy(saving = true)
        viewModelScope.launch {
            updateEventRoles(eventId, dialog.roles)
                .onSuccess { _rolesDialog.value = null }
                .onFailure { e ->
                    _rolesDialog.update {
                        it?.copy(saving = false, error = (e as? DomainException.Validation)?.errors?.get(Field.ROLE_NAME))
                    }
                    if (e !is DomainException.Validation) actionError.value = e
                }
        }
    }

    private fun launchAction(block: suspend () -> Result<Unit>) {
        viewModelScope.launch { block().onFailure { actionError.value = it } }
    }
}
