package com.bharavahi.org.ui.memberform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.MemberDetails
import com.bharavahi.core.domain.model.MemberExtras
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.usecase.member.AddMemberUseCase
import com.bharavahi.core.domain.usecase.member.ObserveMemberUseCase
import com.bharavahi.core.domain.usecase.member.SetMemberStatusUseCase
import com.bharavahi.core.domain.usecase.member.UpdateMemberUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberFormUiState(
    val isEdit: Boolean,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val name: String = "",
    val nickname: String = "",
    val phone: String = "",
    val email: String = "",
    val extras: MemberExtras = MemberExtras(),
    val extrasExpanded: Boolean = false,
    val status: MemberStatus = MemberStatus.ACTIVE,
    val fieldErrors: Map<Field, ValidationError> = emptyMap(),
    val error: Throwable? = null,
)

/** ORG-7 (add), ORG-8 (edit) and ORG-9 (status). Edit mode when the route carries a memberId. */
@HiltViewModel
class MemberFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeMember: ObserveMemberUseCase,
    private val addMember: AddMemberUseCase,
    private val updateMember: UpdateMemberUseCase,
    private val setMemberStatus: SetMemberStatusUseCase,
) : ViewModel() {

    private val memberId: String? = savedStateHandle["memberId"]

    private val _state = MutableStateFlow(MemberFormUiState(isEdit = memberId != null, loading = memberId != null))
    val state: StateFlow<MemberFormUiState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        if (memberId != null) watch(memberId)
    }

    /** Fills the form once, then keeps only the status live so status buttons reflect changes. */
    private fun watch(id: String) = viewModelScope.launch {
        var filled = false
        observeMember(id)
            .catch { e -> _state.update { it.copy(loading = false, error = e) } }
            .collect { member ->
                _state.update {
                    when {
                        member == null -> it.copy(loading = false, error = DomainException.NotFound())
                        filled -> it.copy(status = member.status)
                        else -> it.copy(
                            loading = false,
                            name = member.name,
                            nickname = member.nickname,
                            phone = member.phone,
                            email = member.email,
                            extras = member.extras,
                            extrasExpanded = !member.extras.isEmpty,
                            status = member.status,
                        )
                    }
                }
                filled = member != null
            }
    }

    fun onChange(field: Field, value: String) = _state.update {
        val updated = when (field) {
            Field.NAME -> it.copy(name = value)
            Field.NICKNAME -> it.copy(nickname = value)
            Field.PHONE -> it.copy(phone = value)
            Field.EMAIL -> it.copy(email = value)
            Field.JOB -> it.copy(extras = it.extras.copy(job = value))
            Field.HOUSE_NAME -> it.copy(extras = it.extras.copy(houseName = value))
            else -> it
        }
        updated.copy(fieldErrors = updated.fieldErrors - field)
    }

    /** Dropdown and date fields of the Additional details section. */
    fun onExtrasChange(extras: MemberExtras) = _state.update {
        it.copy(extras = extras, fieldErrors = it.fieldErrors - Field.DATE_OF_BIRTH)
    }

    fun toggleExtras() = _state.update { it.copy(extrasExpanded = !it.extrasExpanded) }

    fun save() {
        val s = _state.value
        if (s.saving || s.loading) return
        _state.update { it.copy(saving = true, error = null) }
        val details = MemberDetails(s.name, s.nickname, s.phone, s.email, s.extras)
        viewModelScope.launch {
            val result = if (memberId != null) updateMember(memberId, details) else addMember(details).map { }
            result
                .onSuccess {
                    _state.update { it.copy(saving = false) }
                    _saved.send(Unit)
                }
                .onFailure { e ->
                    val errors = (e as? DomainException.Validation)?.errors.orEmpty()
                    _state.update {
                        it.copy(
                            saving = false,
                            fieldErrors = errors,
                            error = e,
                            // Keep a date-of-birth error visible even if the section was collapsed.
                            extrasExpanded = it.extrasExpanded || Field.DATE_OF_BIRTH in errors,
                        )
                    }
                }
        }
    }

    fun setStatus(status: MemberStatus) {
        val id = memberId ?: return
        viewModelScope.launch {
            setMemberStatus(id, status).onFailure { e -> _state.update { it.copy(error = e) } }
        }
    }
}
