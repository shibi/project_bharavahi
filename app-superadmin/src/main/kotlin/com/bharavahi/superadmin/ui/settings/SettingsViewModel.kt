package com.bharavahi.superadmin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.ThemeMode
import com.bharavahi.core.domain.usecase.auth.SignOutUseCase
import com.bharavahi.core.domain.usecase.settings.GetMailConfigUseCase
import com.bharavahi.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.bharavahi.core.domain.usecase.settings.SaveMailConfigUseCase
import com.bharavahi.core.domain.usecase.settings.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MailFormState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val senderEmail: String = "",
    val appPassword: String = "",
    val saved: Boolean = false,
    val fieldErrors: Map<Field, ValidationError> = emptyMap(),
    val error: Throwable? = null,
)

/** SA-9 mail sender, UI-5 theme mode, and sign out. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getMailConfig: GetMailConfigUseCase,
    private val saveMailConfig: SaveMailConfigUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    private val _mail = MutableStateFlow(MailFormState())
    val mail: StateFlow<MailFormState> = _mail.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.DEFAULT)

    init {
        viewModelScope.launch {
            getMailConfig()
                .onSuccess { config ->
                    _mail.update {
                        it.copy(
                            loading = false,
                            senderEmail = config?.senderEmail.orEmpty(),
                            appPassword = config?.appPassword.orEmpty(),
                        )
                    }
                }
                .onFailure { e -> _mail.update { it.copy(loading = false, error = e) } }
        }
    }

    fun onSenderEmailChange(value: String) = _mail.update {
        it.copy(senderEmail = value, saved = false, fieldErrors = it.fieldErrors - Field.SENDER_EMAIL)
    }

    fun onAppPasswordChange(value: String) = _mail.update {
        it.copy(appPassword = value, saved = false, fieldErrors = it.fieldErrors - Field.APP_PASSWORD)
    }

    fun saveMail() {
        val s = _mail.value
        if (s.saving) return
        _mail.update { it.copy(saving = true, error = null, saved = false) }
        viewModelScope.launch {
            saveMailConfig(s.senderEmail, s.appPassword)
                .onSuccess { _mail.update { it.copy(saving = false, saved = true) } }
                .onFailure { e ->
                    _mail.update {
                        it.copy(
                            saving = false,
                            fieldErrors = (e as? DomainException.Validation)?.errors.orEmpty(),
                            error = e.takeUnless { e is DomainException.Validation },
                        )
                    }
                }
        }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    /** Navigation back to Login follows from the session change. */
    fun signOut() {
        viewModelScope.launch { signOutUseCase() }
    }
}
