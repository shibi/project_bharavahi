package com.bharavahi.org.ui.eventdues

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.core.ui.theme.LocalStatusColors
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.BackTopBar
import com.bharavahi.org.ui.common.InitialsAvatar
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
    val share: () -> Unit = {
        scope.launch { viewModel.whatsAppText().onSuccess { shareToWhatsApp(context, it, shareTitle) } }
    }

    Scaffold(
        topBar = {
            BackTopBar(stringResource(R.string.dues_title, state.eventName), onBack) {
                SortMenu(state.sort, viewModel::onSort)
                IconButton(onClick = share) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_whatsapp))
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingContent(Modifier.padding(padding))
            state.error != null -> MessageContent(
                stringResource(state.error!!.messageRes()),
                modifier = Modifier.padding(padding),
                onRetry = viewModel::retry,
            )
            state.totalCount == 0 -> MessageContent(stringResource(R.string.event_no_members), Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ShareBanner(share) }
                item { Filters(state, viewModel) }
                item { SearchField(state.query, viewModel::onQuery) }
                if (state.visible.isEmpty()) {
                    item { MessageContent(stringResource(R.string.members_no_match)) }
                } else {
                    item { Summary(state.visible) }
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

/** Design: tappable banner that shares the "who has paid" list to the WhatsApp group. */
@Composable
private fun ShareBanner(onShare: () -> Unit) {
    val paid = LocalStatusColors.current.paid
    Card(
        onClick = onShare,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(paid.copy(alpha = 0.12f)),
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = paid, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.dues_share_banner_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.dues_share_banner_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun Filters(state: EventDuesUiState, viewModel: EventDuesViewModel) {
    val colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        FilterChip(
            selected = state.statusFilter == null,
            onClick = { viewModel.onStatusFilter(null) },
            label = { Text(stringResource(R.string.filter_all) + " (${state.statusCounts.values.sum()})") },
            shape = CircleShape,
            colors = colors,
        )
        DueStatus.entries.forEach { status ->
            FilterChip(
                selected = state.statusFilter == status,
                onClick = { viewModel.onStatusFilter(status) },
                label = { Text(stringResource(status.labelRes()) + " (${state.statusCounts[status] ?: 0})") },
                shape = CircleShape,
                colors = colors,
            )
        }
        state.roles.forEach { role ->
            FilterChip(
                selected = state.roleFilter == role.id,
                onClick = { viewModel.onRoleFilter(if (state.roleFilter == role.id) null else role.id) },
                label = { Text(role.name) },
                shape = CircleShape,
                colors = colors,
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.members_search)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

@Composable
private fun Summary(visible: List<MemberDues>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(LocalStatusColors.current.pending))
        Text(
            stringResource(R.string.dues_showing, visible.size),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
        )
        Text(
            stringResource(R.string.dues_total_pending, formatRupees(visible.sumOf { it.pending })),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

/** Design: member card with avatar, role tag, status pill, a ledger strip and a Mark payment action. */
@Composable
private fun DuesRow(dues: MemberDues, roles: List<EventRole>, onClick: () -> Unit, onMarkPayment: () -> Unit) {
    val role = roles.firstOrNull { it.id == dues.member.roleId }?.name
    val statusColor = dues.currentStatus.color()
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InitialsAvatar(dues.member.name)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(dues.member.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                        if (role != null) Tag(role, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                    }
                    if (dues.overdueCount > 0) {
                        Tag(
                            stringResource(R.string.dues_row_overdue, dues.overdueCount),
                            LocalStatusColors.current.pending,
                            LocalStatusColors.current.pending.copy(alpha = 0.12f),
                        )
                    }
                    if (!dues.member.active) {
                        Text(
                            stringResource(R.string.event_member_disabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    stringResource(dues.currentStatus.labelRes()),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.dues_row_paid_so_far, formatRupees(dues.totalPaid), formatRupees(dues.dueSoFar)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.dues_row_pending, formatRupees(dues.pending)),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (dues.pending > 0) statusColor else LocalStatusColors.current.paid,
                    )
                }
                if (dues.pending > 0 && dues.member.active) {
                    Button(
                        onClick = onMarkPayment,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.mark_payment), modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, color: Color, container: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.background(container, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
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
