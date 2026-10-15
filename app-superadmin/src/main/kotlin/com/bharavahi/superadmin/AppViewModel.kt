package com.bharavahi.superadmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.model.SessionState
import com.bharavahi.core.domain.model.ThemeMode
import com.bharavahi.core.domain.usecase.auth.ObserveSessionUseCase
import com.bharavahi.core.domain.usecase.settings.ObserveThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** App-wide state: who is signed in, and which theme mode to draw. */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
) : ViewModel() {

    val session: StateFlow<SessionState> = observeSession()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    val themeMode: StateFlow<ThemeMode> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DEFAULT)
}
