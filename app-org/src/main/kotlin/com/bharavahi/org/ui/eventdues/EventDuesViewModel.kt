package com.bharavahi.org.ui.eventdues

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.model.DueStatus
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.MemberDues
import com.bharavahi.core.domain.model.MemberSort
import com.bharavahi.core.domain.rules.MemberDuesRules
import com.bharavahi.core.domain.usecase.payment.BuildWhatsAppMessageUseCase
import com.bharavahi.core.domain.usecase.payment.ObserveEventDuesUseCase
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
import java.time.LocalDate
import javax.inject.Inject

data class EventDuesUiState(
    val loading: Boolean = true,
    val eventName: String = "",
    val roles: List<EventRole> = emptyList(),
    val statusFilter: DueStatus? = null,
    val roleFilter: String? = null,
    val sort: MemberSort = MemberSort.MOST_BEHIND,
    val totalCount: Int = 0,
    val query: String = "",
    /** Active members per current status, for the filter chip counts. */
    val statusCounts: Map<DueStatus, Int> = emptyMap(),
    val visible: List<MemberDues> = emptyList(),
    val error: Throwable? = null,
)

private data class Filters(val status: DueStatus?, val roleId: String?, val sort: MemberSort, val query: String = "")

/** ORG-17 to ORG-19 and ORG-31: who has paid what, filtered and sorted, and the WhatsApp list. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventDuesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeEventDues: ObserveEventDuesUseCase,
    private val buildWhatsApp: BuildWhatsAppMessageUseCase,
) : ViewModel() {

    val eventId: String = checkNotNull(savedStateHandle["eventId"])

    private val filters = MutableStateFlow(Filters(null, null, MemberSort.MOST_BEHIND))
    private val retry = MutableStateFlow(0)

    private val dues = retry.flatMapLatest {
        observeEventDues(eventId, LocalDate.now())
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }

    val state: StateFlow<EventDuesUiState> = combine(dues, filters) { result, f ->
        val d = result.getOrNull()
        EventDuesUiState(
            loading = false,
            eventName = d?.event?.name.orEmpty(),
            roles = d?.event?.roles.orEmpty(),
            statusFilter = f.status,
            roleFilter = f.roleId,
            sort = f.sort,
            totalCount = d?.members?.size ?: 0,
            query = f.query,
            statusCounts = d?.members.orEmpty().filter { it.member.active }.groupingBy { it.currentStatus }.eachCount(),
            visible = d?.let { MemberDuesRules.filterAndSort(it.members, f.status, f.roleId, f.sort) }.orEmpty()
                .filter { f.query.isBlank() || it.member.name.contains(f.query.trim(), ignoreCase = true) },
            error = result.exceptionOrNull() ?: if (d == null) DomainException.NotFound() else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventDuesUiState())

    fun onStatusFilter(status: DueStatus?) = filters.value.let { filters.value = it.copy(status = status) }
    fun onRoleFilter(roleId: String?) = filters.value.let { filters.value = it.copy(roleId = roleId) }
    fun onSort(sort: MemberSort) = filters.value.let { filters.value = it.copy(sort = sort) }
    fun onQuery(query: String) = filters.value.let { filters.value = it.copy(query = query) }
    fun retry() { retry.value++ }

    suspend fun whatsAppText(): Result<String> = buildWhatsApp(eventId, LocalDate.now())
}
