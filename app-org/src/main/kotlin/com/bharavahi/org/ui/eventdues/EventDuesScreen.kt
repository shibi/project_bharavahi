package com.bharavahi.org.ui.eventdues

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.ui.Alignment
import com.bharavahi.org.ui.common.InitialsAvatar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.DueStatus
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.MemberDues
import com.bharavahi.core.domain.model.MemberSort
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.StatusBadge
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.BackTopBar
import com.bharavahi.org.ui.common.color
import com.bharavahi.org.ui.common.labelRes
import com.bharavahi.org.ui.common.shareToWhatsApp
import kotlinx.coroutines.launch

@Composable
fun EventDuesScreen(
    onBack: () -> Unit,
    onOpenMember: (eventId: String, memberId: String) -> Unit,
    onMarkPayment: (eventId: String, memberId: String) -> Unit,
    viewModel: EventDuesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shareTitle = stringResource(R.string.share_whatsapp)

    Scaffold(
        topBar = {
            BackTopBar(stringResource(R.string.dues_title, state.eventName), onBack) {
                SortMenu(state.sort, viewModel::onSort)
                IconButton(onClick = {
                    scope.launch { viewModel.whatsAppText().onSuccess { shareToWhatsApp(context, it, shareTitle) } }
                }) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_whatsapp))
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Filters(state, viewModel)
            when {
                state.loading -> LoadingContent()
                state.error != null -> MessageContent(stringResource(state.error!!.messageRes()), onRetry = viewModel::retry)
                state.totalCount == 0 -> MessageContent(stringResource(R.string.event_no_members))
                state.visible.isEmpty() -> MessageContent(stringResource(R.string.members_no_match))
                else -> LazyColumn {
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Text(
                                stringResource(R.string.dues_showing, state.visible.size),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                stringResource(R.string.dues_total_pending, formatRupees(state.visible.sumOf { it.pending })),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    items(state.visible, key = { it.member.memberId }) { dues ->
                        DuesRow(
                            dues,
                            state.roles,
                            onClick = { onOpenMember(viewModel.eventId, dues.member.memberId) },
                            onMarkPayment = { onMarkPayment(viewModel.eventId, dues.member.memberId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Filters(state: EventDuesUiState, viewModel: EventDuesViewModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        FilterChip(
            selected = state.statusFilter == null,
            onClick = { viewModel.onStatusFilter(null) },
            label = { Text(stringResource(R.string.filter_all)) },
        )
        DueStatus.entries.forEach { status ->
            FilterChip(
                selected = state.statusFilter == status,
                onClick = { viewModel.onStatusFilter(status) },
                label = { Text(stringResource(status.labelRes())) },
            )
        }
        state.roles.forEach { role ->
            FilterChip(
                selected = state.roleFilter == role.id,
                onClick = { viewModel.onRoleFilter(if (state.roleFilter == role.id) null else role.id) },
                label = { Text(role.name) },
            )
        }
    }
}

/** Design: member card with avatar, status pill and a Mark payment action while something is pending. */
@Composable
private fun DuesRow(dues: MemberDues, roles: List<EventRole>, onClick: () -> Unit, onMarkPayment: () -> Unit) {
    val role = roles.firstOrNull { it.id == dues.member.roleId }?.name
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InitialsAvatar(dues.member.name)
                Column(Modifier.weight(1f)) {
                    Text(dues.member.name, style = MaterialTheme.typography.titleMedium)
                    if (role != null) {
                        Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                StatusBadge(stringResource(dues.currentStatus.labelRes()), dues.currentStatus.color())
            }
            Text(
                stringResource(R.string.dues_row_amounts, formatRupees(dues.totalPaid), formatRupees(dues.dueSoFar), formatRupees(dues.pending)),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (dues.overdueCount > 0) {
                Text(
                    stringResource(R.string.dues_row_overdue, dues.overdueCount),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!dues.member.active) Text(stringResource(R.string.event_member_disabled), style = MaterialTheme.typography.bodySmall)
            if (dues.pending > 0 && dues.member.active) {
                Button(
                    onClick = onMarkPayment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    modifier = Modifier.align(Alignment.End),
                ) { Text(stringResource(R.string.mark_payment)) }
            }
        }
    }
}

@Composable
private fun SortMenu(current: MemberSort, onSelect: (MemberSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MemberSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text((if (sort == current) "✓ " else "") + stringResource(sort.labelRes())) },
                    onClick = {
                        open = false
                        onSelect(sort)
                    },
                )
            }
        }
    }
}

private fun MemberSort.labelRes(): Int = when (this) {
    MemberSort.MOST_BEHIND -> R.string.sort_most_behind
    MemberSort.AMOUNT_PAID -> R.string.sort_amount_paid
    MemberSort.NAME -> R.string.sort_name
}
