package com.bharavahi.superadmin.ui.communities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.domain.usecase.organization.ObserveOrganizationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CommunitiesUiState(
    val query: String = "",
    val statusFilter: OrganizationStatus? = null,
    val loading: Boolean = true,
    val totalCount: Int = 0,
    val visible: List<Organization> = emptyList(),
    val error: Throwable? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommunitiesViewModel @Inject constructor(
    observeOrganizations: ObserveOrganizationsUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<OrganizationStatus?>(null)
    private val retry = MutableStateFlow(0)

    private val organizations = retry.flatMapLatest {
        observeOrganizations()
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }

    val state: StateFlow<CommunitiesUiState> =
        combine(organizations, query, statusFilter) { result, q, status ->
            val all = result.getOrDefault(emptyList())
            CommunitiesUiState(
                query = q,
                statusFilter = status,
                loading = false,
                totalCount = all.size,
                visible = ObserveOrganizationsUseCase.filter(all, q, status),
                error = result.exceptionOrNull(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommunitiesUiState())

    fun onQueryChange(value: String) { query.value = value }
    fun onStatusFilter(status: OrganizationStatus?) { statusFilter.value = status }
    fun retry() { retry.value++ }
}
