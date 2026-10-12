package com.bharavahi.superadmin.ui.communityform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.NewAdmin
import com.bharavahi.core.domain.model.OrganizationDetails
import com.bharavahi.core.domain.usecase.organization.CreateOrganizationUseCase
import com.bharavahi.core.domain.usecase.organization.ObserveOrganizationUseCase
import com.bharavahi.core.domain.usecase.organization.UpdateOrganizationUseCase
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
import javax.inject.Inject

data class CommunityFormUiState(
    val isEdit: Boolean,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val name: String = "",
    val type: String = "",
    val address: String = "",
    val contactPhone: String = "",
    val adminName: String = "",
    val adminEmail: String = "",
    val adminPhone: String = "",
    val adminPassword: String = "",
    val fieldErrors: Map<Field, ValidationError> = emptyMap(),
    val error: Throwable? = null,
)

/** SA-4 (add) and SA-5 (edit). Edit mode when the route carries an orgId. */
@HiltViewModel
class CommunityFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeOrganization: ObserveOrganizationUseCase,
    private val createOrganization: CreateOrganizationUseCase,
    private val updateOrganization: UpdateOrganizationUseCase,
) : ViewModel() {

    private val orgId: String? = savedStateHandle["orgId"]

    private val _state = MutableStateFlow(CommunityFormUiState(isEdit = orgId != null, loading = orgId != null))
    val state: StateFlow<CommunityFormUiState> = _state.asStateFlow()

    private val _saved = Channel<String>(Channel.BUFFERED)
    /** Emits the orgId once the community is saved. */
    val saved: Flow<String> = _saved.receiveAsFlow()

    init {
        if (orgId != null) loadExisting(orgId)
    }

    private fun loadExisting(id: String) = viewModelScope.launch {
        val org = runCatching { observeOrganization(id).firstOrNull() }.getOrNull()
        _state.update {
            if (org == null) {
                it.copy(loading = false, error = DomainException.NotFound())
            } else {
                it.copy(
                    loading = false,
                    name = org.name,
                    type = org.type,
                    address = org.address,
                    contactPhone = org.contactPhone,
                )
            }
        }
    }

    fun onChange(field: Field, value: String) = _state.update {
        val updated = when (field) {
            Field.NAME -> it.copy(name = value)
            Field.TYPE -> it.copy(type = value)
            Field.ADDRESS -> it.copy(address = value)
            Field.PHONE -> it.copy(contactPhone = value)
            Field.ADMIN_NAME -> it.copy(adminName = value)
            Field.ADMIN_EMAIL -> it.copy(adminEmail = value)
            Field.ADMIN_PHONE -> it.copy(adminPhone = value)
            Field.PASSWORD -> it.copy(adminPassword = value)
            else -> it
        }
        updated.copy(fieldErrors = updated.fieldErrors - field)
    }

    fun save() {
        val s = _state.value
        if (s.saving || s.loading) return
        _state.update { it.copy(saving = true, error = null) }
        val details = OrganizationDetails(s.name, s.type, s.address, s.contactPhone)

        viewModelScope.launch {
            val result = if (orgId != null) {
                updateOrganization(orgId, details).map { orgId }
            } else {
                createOrganization(details, NewAdmin(s.adminName, s.adminEmail, s.adminPhone, s.adminPassword))
            }
            result
                .onSuccess { id ->
                    _state.update { it.copy(saving = false) }
                    _saved.send(id)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            saving = false,
                            fieldErrors = (e as? DomainException.Validation)?.errors.orEmpty(),
                            error = e,
                        )
                    }
                }
        }
    }
}
