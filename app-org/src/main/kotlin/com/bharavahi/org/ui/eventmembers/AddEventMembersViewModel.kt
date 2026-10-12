package com.bharavahi.org.ui.eventmembers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.model.EventMemberAssignment
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.Member
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.usecase.event.AddEventMembersUseCase
import com.bharavahi.core.domain.usecase.event.ObserveEventMembersUseCase
import com.bharavahi.core.domain.usecase.event.ObserveEventUseCase
import com.bharavahi.core.domain.usecase.member.ObserveMembersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEventMembersUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    /** Active community members not yet in the event. */
    val candidates: List<Member> = emptyList(),
    val roles: List<EventRole> = emptyList(),
    val selected: Map<String, String?> = emptyMap(),
    val error: Throwable? = null,
)

/** ORG-12: add more members to an event. BR-6: they owe every due, past ones included. */
@HiltViewModel
class AddEventMembersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeEvent: ObserveEventUseCase,
    private val observeMembers: ObserveMembersUseCase,
    private val observeEventMembers: ObserveEventMembersUseCase,
    private val addEventMembers: AddEventMembersUseCase,
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val _state = MutableStateFlow(AddEventMembersUiState())
    val state: StateFlow<AddEventMembersUiState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val inEvent = observeEventMembers(eventId).first().map { it.memberId }.toSet()
                val candidates = observeMembers().first().filter { it.status == MemberStatus.ACTIVE && it.id !in inEvent }
                val roles = observeEvent(eventId).first()?.roles.orEmpty()
                _state.update { it.copy(loading = false, candidates = candidates, roles = roles) }
            }.onFailure { e -> _state.update { it.copy(loading = false, error = e) } }
        }
    }

    fun toggle(member: Member) = _state.update {
        it.copy(selected = if (member.id in it.selected) it.selected - member.id else it.selected + (member.id to null))
    }

    fun toggleAll() = _state.update { s ->
        s.copy(selected = if (s.selected.size == s.candidates.size) emptyMap() else s.candidates.associate { it.id to s.selected[it.id] })
    }

    fun setRole(memberId: String, roleId: String?) = _state.update { it.copy(selected = it.selected + (memberId to roleId)) }

    fun save() {
        val s = _state.value
        if (s.saving || s.selected.isEmpty()) return
        val byId = s.candidates.associateBy { it.id }
        val members = s.selected.mapNotNull { (id, roleId) -> byId[id]?.let { EventMemberAssignment(id, it.name, roleId) } }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            addEventMembers(eventId, members)
                .onSuccess {
                    _state.update { it.copy(saving = false) }
                    _saved.send(Unit)
                }
                .onFailure { e -> _state.update { it.copy(saving = false, error = e) } }
        }
    }
}
