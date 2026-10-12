package com.bharavahi.org.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.usecase.event.ObserveEventsUseCase
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

data class EventsUiState(
    val statusFilter: EventStatus? = null,
    val loading: Boolean = true,
    val totalCount: Int = 0,
    val visible: List<Event> = emptyList(),
    val error: Throwable? = null,
)

/** ORG-10: event list with a status filter. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventsViewModel @Inject constructor(
    observeEvents: ObserveEventsUseCase,
) : ViewModel() {

    private val statusFilter = MutableStateFlow<EventStatus?>(null)
    private val retry = MutableStateFlow(0)

    private val events = retry.flatMapLatest {
        observeEvents()
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }

    val state: StateFlow<EventsUiState> =
        combine(events, statusFilter) { result, status ->
            val all = result.getOrDefault(emptyList())
            EventsUiState(
                statusFilter = status,
                loading = false,
                totalCount = all.size,
                visible = ObserveEventsUseCase.filter(all, status),
                error = result.exceptionOrNull(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventsUiState())

    fun onStatusFilter(status: EventStatus?) { statusFilter.value = status }
    fun retry() { retry.value++ }
}
