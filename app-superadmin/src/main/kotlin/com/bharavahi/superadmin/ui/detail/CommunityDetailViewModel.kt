package com.bharavahi.superadmin.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.usecase.organization.DeleteOrganizationUseCase
import com.bharavahi.core.domain.usecase.organization.ObserveOrganizationDetailUseCase
import com.bharavahi.core.domain.usecase.organization.OrganizationDetail
import com.bharavahi.core.domain.usecase.organization.SetAdminActiveUseCase
import com.bharavahi.core.domain.usecase.organization.SetOrganizationStatusUseCase
import com.bharavahi.superadmin.navigation.CommunityDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailContent {
    data object Loading : DetailContent
    data class Ready(val detail: OrganizationDetail) : DetailContent
    data class Error(val error: Throwable) : DetailContent
}

enum class DetailDialog { SUSPEND, DELETE }

data class DetailActionState(
    val busy: Boolean = false,
    val dialog: DetailDialog? = null,
    val deleteConfirmText: String = "",
    val error: Throwable? = null,
)

/** SA-5, SA-6, SA-7. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommunityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeDetail: ObserveOrganizationDetailUseCase,
    private val setStatus: SetOrganizationStatusUseCase,
    private val deleteOrganization: DeleteOrganizationUseCase,
    private val setAdminActiveUseCase: SetAdminActiveUseCase,
) : ViewModel() {

    val orgId: String = savedStateHandle.toRoute<CommunityDetailRoute>().orgId
    private val retry = MutableStateFlow(0)

    val content: StateFlow<DetailContent> = retry
        .flatMapLatest {
            observeDetail(orgId)
                .map<OrganizationDetail, DetailContent> { DetailContent.Ready(it) }
                .catch { emit(DetailContent.Error(it)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailContent.Loading)

    private val _actions = MutableStateFlow(DetailActionState())
    val actions: StateFlow<DetailActionState> = _actions.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    fun retry() { retry.value++ }

    fun showDialog(dialog: DetailDialog?) = _actions.update {
        it.copy(dialog = dialog, deleteConfirmText = "", error = null)
    }

    fun onDeleteConfirmTextChange(value: String) = _actions.update { it.copy(deleteConfirmText = value) }

    /** Suspends an active community or reactivates a suspended one. */
    fun toggleSuspended(suspend: Boolean) = runAction {
        val org = currentOrganization() ?: return@runAction Result.success(Unit)
        setStatus(org, suspend)
    }

    fun confirmDelete() = runAction {
        val org = currentOrganization() ?: return@runAction Result.success(Unit)
        deleteOrganization(org, _actions.value.deleteConfirmText).onSuccess { _deleted.trySend(Unit) }
    }

    fun setAdminActive(admin: OrgAdmin, active: Boolean) = runAction { setAdminActiveUseCase(admin, active) }

    private fun currentOrganization() = (content.value as? DetailContent.Ready)?.detail?.organization

    private fun runAction(block: suspend () -> Result<Unit>) {
        if (_actions.value.busy) return
        _actions.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = block()
            _actions.update {
                if (result.isSuccess) DetailActionState() else it.copy(busy = false, error = result.exceptionOrNull())
            }
        }
    }
}
