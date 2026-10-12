package com.bharavahi.org.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.model.Member
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.usecase.member.ObserveMembersUseCase
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

data class MembersUiState(
    val query: String = "",
    val statusFilter: MemberStatus? = MemberStatus.ACTIVE,
    val loading: Boolean = true,
    val totalCount: Int = 0,
    val visible: List<Member> = emptyList(),
    val error: Throwable? = null,
)

/** ORG-6: search and filter the community's members. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MembersViewModel @Inject constructor(
    observeMembers: ObserveMembersUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<MemberStatus?>(MemberStatus.ACTIVE)
    private val retry = MutableStateFlow(0)

    private val members = retry.flatMapLatest {
        observeMembers()
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }

    val state: StateFlow<MembersUiState> =
        combine(members, query, statusFilter) { result, q, status ->
            val all = result.getOrDefault(emptyList())
            MembersUiState(
                query = q,
                statusFilter = status,
                loading = false,
                totalCount = all.size,
                visible = ObserveMembersUseCase.filter(all, q, status),
                error = result.exceptionOrNull(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MembersUiState())

    fun onQueryChange(value: String) { query.value = value }
    fun onStatusFilter(status: MemberStatus?) { statusFilter.value = status }
    fun retry() { retry.value++ }
}
