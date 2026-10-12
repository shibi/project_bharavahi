package com.bharavahi.org.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.usecase.orgdashboard.ObserveOrgDashboardUseCase
import com.bharavahi.core.domain.usecase.orgdashboard.OrgDashboard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val loading: Boolean = true,
    val dashboard: OrgDashboard? = null,
    val error: Throwable? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDashboard: ObserveOrgDashboardUseCase,
) : ViewModel() {

    private val retry = MutableStateFlow(0)

    val state: StateFlow<DashboardUiState> = retry.flatMapLatest {
        observeDashboard()
            .map { DashboardUiState(loading = false, dashboard = it) }
            .catch { emit(DashboardUiState(loading = false, error = it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun retry() { retry.value++ }
}
