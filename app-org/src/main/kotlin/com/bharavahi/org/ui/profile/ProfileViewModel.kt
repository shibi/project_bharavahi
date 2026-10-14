package com.bharavahi.org.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.AdminSessionState
import com.bharavahi.core.domain.model.NewAdmin
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.model.ThemeMode
import com.bharavahi.core.domain.usecase.auth.SignOutUseCase
import com.bharavahi.core.domain.usecase.orgadmin.AddCoAdminUseCase
import com.bharavahi.core.domain.usecase.orgadmin.MAX_ACTIVE_ADMINS
import com.bharavahi.core.domain.usecase.orgadmin.ObserveOrgAdminsUseCase
import com.bharavahi.core.domain.usecase.orgadmin.SetCoAdminActiveUseCase
import com.bharavahi.core.domain.usecase.orgauth.ObserveAdminSessionUseCase
import com.bharavahi.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.bharavahi.core.domain.usecase.settings.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val email: String = "",
    /** The signed-in admin's own record, for name and phone. Null until it loads. */
    val me: OrgAdmin? = null,
    val coAdmins: List<OrgAdmin> = emptyList(),
    val activeCount: Int = 0,
    /** A failed activate/deactivate; shown as a message. */
    val actionError: Throwable? = null,
) {
    val maxAdmins: Int get() = MAX_ACTIVE_ADMINS
    val openSlots: Int get() = (MAX_ACTIVE_ADMINS - activeCount).coerceAtLeast(0)
}

/** The "Add co-admin" form while it is open. */
data class AddCoAdminForm(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val fieldErrors: Map<Field, ValidationError> = emptyMap(),
    val error: Throwable? = null,
    val saving: Boolean = false,
)

/** ORG-3 and ORG-4 and UI-5: who is signed in, co-admins, theme mode, sign out. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeSession: ObserveAdminSessionUseCase,
    observeAdmins: ObserveOrgAdminsUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val addCoAdmin: AddCoAdminUseCase,
    private val setCoAdminActive: SetCoAdminActiveUseCase,
) : ViewModel() {

    private val actionError = MutableStateFlow<Throwable?>(null)

    val state: StateFlow<ProfileUiState> = combine(
        observeSession(),
        observeAdmins().onStart { emit(emptyList()) }.catch { emit(emptyList()) },
        actionError,
    ) { session, admins, error ->
        val user = (session as? AdminSessionState.SignedIn)?.user
        ProfileUiState(
            email = user?.email.orEmpty(),
            me = admins.firstOrNull { it.uid == user?.uid },
            coAdmins = admins.filter { it.uid != user?.uid }.sortedByDescending { it.active },
            activeCount = admins.count { it.active },
            actionError = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    val themeMode: StateFlow<ThemeMode> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    private val _addForm = MutableStateFlow<AddCoAdminForm?>(null)
    val addForm: StateFlow<AddCoAdminForm?> = _addForm.asStateFlow()

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun signOut() {
        viewModelScope.launch { signOutUseCase() }
    }

    fun setActive(admin: OrgAdmin, active: Boolean) {
        viewModelScope.launch {
            setCoAdminActive(admin, active).onFailure { actionError.value = it }
        }
    }

    fun dismissError() { actionError.value = null }

    fun openAddForm() { _addForm.value = AddCoAdminForm() }

    fun closeAddForm() {
        if (_addForm.value?.saving != true) _addForm.value = null
    }

    fun onNameChange(value: String) = _addForm.update { it?.copy(name = value, fieldErrors = it.fieldErrors - Field.ADMIN_NAME) }
    fun onEmailChange(value: String) = _addForm.update { it?.copy(email = value, fieldErrors = it.fieldErrors - Field.ADMIN_EMAIL) }
    fun onPhoneChange(value: String) = _addForm.update { it?.copy(phone = value, fieldErrors = it.fieldErrors - Field.ADMIN_PHONE) }
    fun onPasswordChange(value: String) = _addForm.update { it?.copy(password = value, fieldErrors = it.fieldErrors - Field.PASSWORD) }

    fun submitAddForm() {
        val form = _addForm.value ?: return
        if (form.saving) return
        _addForm.value = form.copy(saving = true, error = null)
        viewModelScope.launch {
            addCoAdmin(NewAdmin(form.name, form.email, form.phone, form.password))
                .onSuccess { _addForm.value = null }
                .onFailure { e ->
                    _addForm.update {
                        it?.copy(
                            saving = false,
                            fieldErrors = (e as? DomainException.Validation)?.errors.orEmpty(),
                            error = e.takeUnless { e is DomainException.Validation },
                        )
                    }
                }
        }
    }
}
