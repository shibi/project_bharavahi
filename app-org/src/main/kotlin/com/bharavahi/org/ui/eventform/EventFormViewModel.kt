package com.bharavahi.org.ui.eventform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.EventDetails
import com.bharavahi.core.domain.model.EventMemberAssignment
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.Member
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.model.NewEvent
import com.bharavahi.core.domain.usecase.event.CreateEventUseCase
import com.bharavahi.core.domain.usecase.event.EventRoles
import com.bharavahi.core.domain.usecase.event.ObserveEventUseCase
import com.bharavahi.core.domain.usecase.event.UpdateEventDetailsUseCase
import com.bharavahi.core.domain.usecase.member.ObserveMembersUseCase
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

enum class EventStep { DETAILS, BALANCE, ROLES, MEMBERS }

data class EventFormUiState(
    val isEdit: Boolean,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val step: EventStep = EventStep.DETAILS,
    val name: String = "",
    val description: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val openingBalance: String = "",
    val roles: List<EventRole> = emptyList(),
    val newRoleName: String = "",
    val members: List<Member> = emptyList(),
    /** memberId → roleId for the members ticked in step 4. */
    val selected: Map<String, String?> = emptyMap(),
    val fieldErrors: Map<Field, ValidationError> = emptyMap(),
    val error: Throwable? = null,
)

/**
 * ORG-11: the step-by-step create flow (details, opening balance, roles, members).
 * Schedules are added from the event screen afterwards. With an eventId (ORG-12) only the
 * details and opening balance are edited here; roles and members are managed on the event screen.
 */
@HiltViewModel
class EventFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeEvent: ObserveEventUseCase,
    private val observeMembers: ObserveMembersUseCase,
    private val createEvent: CreateEventUseCase,
    private val updateEventDetails: UpdateEventDetailsUseCase,
) : ViewModel() {

    private val eventId: String? = savedStateHandle["eventId"]

    private val _state = MutableStateFlow(
        EventFormUiState(
            isEdit = eventId != null,
            loading = true,
            roles = if (eventId == null) DEFAULT_ROLES.map(EventRoles::newRole) else emptyList(),
        ),
    )
    val state: StateFlow<EventFormUiState> = _state.asStateFlow()

    private val _saved = Channel<String>(Channel.BUFFERED)
    /** Emits the event id once saved. */
    val saved: Flow<String> = _saved.receiveAsFlow()

    init {
        viewModelScope.launch { if (eventId != null) loadExisting(eventId) else loadMembers() }
    }

    private suspend fun loadExisting(id: String) {
        val event = runCatching { observeEvent(id).firstOrNull() }.getOrNull()
        _state.update {
            if (event == null) {
                it.copy(loading = false, error = DomainException.NotFound())
            } else {
                it.copy(
                    loading = false,
                    name = event.name,
                    description = event.description,
                    startDate = event.startDate,
                    endDate = event.endDate,
                    openingBalance = if (event.openingBalance == 0L) "" else Money.toInput(event.openingBalance),
                )
            }
        }
    }

    private suspend fun loadMembers() {
        val members = runCatching { observeMembers().firstOrNull() }
        _state.update {
            it.copy(
                loading = false,
                members = members.getOrNull().orEmpty().filter { m -> m.status == MemberStatus.ACTIVE },
                error = members.exceptionOrNull(),
            )
        }
    }

    fun onChange(field: Field, value: String) = _state.update {
        val updated = when (field) {
            Field.NAME -> it.copy(name = value)
            Field.DESCRIPTION -> it.copy(description = value)
            Field.OPENING_BALANCE -> it.copy(openingBalance = value)
            else -> it
        }
        updated.copy(fieldErrors = updated.fieldErrors - field)
    }

    fun onStartDate(date: LocalDate) = _state.update { it.copy(startDate = date, fieldErrors = it.fieldErrors - Field.START_DATE) }
    fun onEndDate(date: LocalDate) = _state.update { it.copy(endDate = date, fieldErrors = it.fieldErrors - Field.END_DATE) }

    fun onNewRoleNameChange(value: String) = _state.update { it.copy(newRoleName = value) }

    fun addRole() = _state.update {
        if (it.newRoleName.isBlank()) it
        else it.copy(roles = it.roles + EventRoles.newRole(it.newRoleName.trim()), newRoleName = "", fieldErrors = it.fieldErrors - Field.ROLE_NAME)
    }

    fun renameRole(id: String, name: String) = _state.update { s ->
        s.copy(roles = s.roles.map { if (it.id == id) it.copy(name = name) else it }, fieldErrors = s.fieldErrors - Field.ROLE_NAME)
    }

    fun removeRole(id: String) = _state.update { s ->
        s.copy(
            roles = s.roles.filterNot { it.id == id },
            selected = s.selected.mapValues { (_, roleId) -> roleId.takeUnless { it == id } },
            fieldErrors = s.fieldErrors - Field.ROLE_NAME,
        )
    }

    fun toggleMember(member: Member) = _state.update {
        it.copy(selected = if (member.id in it.selected) it.selected - member.id else it.selected + (member.id to null))
    }

    fun toggleAll() = _state.update { s ->
        s.copy(selected = if (s.selected.size == s.members.size) emptyMap() else s.members.associate { it.id to s.selected[it.id] })
    }

    fun setRole(memberId: String, roleId: String?) = _state.update { it.copy(selected = it.selected + (memberId to roleId)) }

    fun next() = _state.update { s -> EventStep.entries.getOrNull(s.step.ordinal + 1)?.let { s.copy(step = it) } ?: s }

    /** Returns false when already on the first step, so the screen can leave. */
    fun back(): Boolean {
        val s = _state.value
        if (s.isEdit || s.step == EventStep.DETAILS) return false
        _state.update { it.copy(step = EventStep.entries[s.step.ordinal - 1]) }
        return true
    }

    fun save() {
        val s = _state.value
        if (s.saving || s.loading) return
        val balance = if (s.openingBalance.isBlank()) 0L else Money.parseRupees(s.openingBalance) ?: -1L
        val details = EventDetails(s.name, s.description, s.startDate, s.endDate, balance)
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = if (eventId != null) {
                updateEventDetails(eventId, details).map { eventId }
            } else {
                val byId = s.members.associateBy { it.id }
                val members = s.selected.mapNotNull { (id, roleId) ->
                    byId[id]?.let { EventMemberAssignment(id, it.name, roleId) }
                }
                createEvent(NewEvent(details, s.roles, members))
            }
            result
                .onSuccess { id ->
                    _state.update { it.copy(saving = false) }
                    _saved.send(id)
                }
                .onFailure { e ->
                    val errors = (e as? DomainException.Validation)?.errors.orEmpty()
                    _state.update {
                        it.copy(
                            saving = false,
                            fieldErrors = errors,
                            error = e.takeUnless { e is DomainException.Validation },
                            step = stepFor(errors.keys) ?: it.step,
                        )
                    }
                }
        }
    }

    private fun stepFor(fields: Set<Field>): EventStep? = when {
        fields.any { it in setOf(Field.NAME, Field.START_DATE, Field.END_DATE) } -> EventStep.DETAILS
        Field.OPENING_BALANCE in fields -> EventStep.BALANCE
        Field.ROLE_NAME in fields -> EventStep.ROLES
        else -> null
    }

    private companion object {
        val DEFAULT_ROLES = listOf("President", "Secretary", "Treasurer")
    }
}
