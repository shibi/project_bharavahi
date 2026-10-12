package com.bharavahi.org

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.model.AdminSessionState
import com.bharavahi.core.domain.model.ThemeMode
import com.bharavahi.core.domain.usecase.orgauth.ObserveAdminSessionUseCase
import com.bharavahi.core.domain.usecase.settings.ObserveThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-wide state: which admin is signed in, and which theme mode to draw. */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeSession: ObserveAdminSessionUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
) : ViewModel() {

    val session: StateFlow<AdminSessionState> = observeSession()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AdminSessionState.Loading)

    val themeMode: StateFlow<ThemeMode> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
}
