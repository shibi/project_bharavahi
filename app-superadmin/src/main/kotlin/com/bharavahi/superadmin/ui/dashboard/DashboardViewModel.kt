package com.bharavahi.superadmin.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.model.Dashboard
import com.bharavahi.core.domain.usecase.dashboard.ObserveDashboardUseCase
import com.bharavahi.core.domain.usecase.organization.ObserveOrganizationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(val dashboard: Dashboard, val orgNames: Map<String, String>) : DashboardUiState
    data class Error(val error: Throwable) : DashboardUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeDashboard: ObserveDashboardUseCase,
    private val observeOrganizations: ObserveOrganizationsUseCase,
) : ViewModel() {

    private val retry = MutableStateFlow(0)

    val state: StateFlow<DashboardUiState> = retry
        .flatMapLatest {
            combine(observeDashboard(), observeOrganizations()) { dashboard, orgs ->
                DashboardUiState.Ready(dashboard, orgs.associate { it.id to it.name }) as DashboardUiState
            }.catch { emit(DashboardUiState.Error(it)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState.Loading)

    fun retry() { retry.value++ }
}
