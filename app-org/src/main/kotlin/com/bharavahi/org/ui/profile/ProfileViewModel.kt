package com.bharavahi.org.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.model.AdminSessionState
import com.bharavahi.core.domain.model.ThemeMode
import com.bharavahi.core.domain.usecase.auth.SignOutUseCase
import com.bharavahi.core.domain.usecase.orgauth.ObserveAdminSessionUseCase
import com.bharavahi.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.bharavahi.core.domain.usecase.settings.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ORG-3 (first part) and UI-5: who is signed in, theme mode, sign out. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeSession: ObserveAdminSessionUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    val email: StateFlow<String> = observeSession()
        .map { (it as? AdminSessionState.SignedIn)?.user?.email.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val themeMode: StateFlow<ThemeMode> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun signOut() {
        viewModelScope.launch { signOutUseCase() }
    }
}
