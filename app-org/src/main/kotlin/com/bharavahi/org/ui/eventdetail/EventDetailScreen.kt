package com.bharavahi.org.ui.eventdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.Schedule
import com.bharavahi.core.ui.components.ConfirmDialog
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.StatusBadge
import com.bharavahi.core.ui.components.formatDate
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.BackTopBar
import com.bharavahi.org.ui.common.SectionTitle
import com.bharavahi.org.ui.common.labelRes
import com.bharavahi.org.ui.eventform.RolesEditor
import com.bharavahi.org.ui.eventmembers.RolePicker
import com.bharavahi.org.ui.common.shareToWhatsApp
import kotlinx.coroutines.launch

@Composable
fun EventDetailScreen(
    onBack: () -> Unit,
    onEdit: (eventId: String) -> Unit,
    onAddMembers: (eventId: String) -> Unit,
    onAddSchedule: (eventId: String) -> Unit,
    onOpenSchedule: (eventId: String, scheduleId: String) -> Unit,
    onMarkPayment: (eventId: String) -> Unit,
    onOpenDues: (eventId: String) -> Unit,
    onOpenPayments: (eventId: String) -> Unit,
    onOpenExpenses: (eventId: String) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rolesDialog by viewModel.rolesDialog.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmStatus by remember { mutableStateOf<EventStatus?>(null) }
    var confirmRemove by remember { mutableStateOf<EventMember?>(null) }
    val scope = rememberCoroutineScope()
    val shareTitle = stringResource(R.string.share_whatsapp)

    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            snackbar.showSnackbar(context.getString(it.messageRes()))
            viewModel.dismissError()
        }
    }

    val event = state.event
    Scaffold(
        topBar = {
            BackTopBar(event?.name.orEmpty(), onBack) {
                if (event != null) {
                    IconButton(onClick = { onEdit(event.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.event_edit))
                    }
                    StatusMenu(event.status, onSelect = { confirmStatus = it })
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading -> LoadingContent(Modifier.padding(padding))
            event == null -> MessageContent(
                message = stringResource(state.error?.messageRes() ?: R.string.event_not_found),
                modifier = Modifier.padding(padding),
                onRetry = viewModel::retry,
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Overview(event) }
                item {
                    EventActions(
                        onMarkPayment = { onMarkPayment(event.id) },
                        onOpenDues = { onOpenDues(event.id) },
                        onOpenPayments = { onOpenPayments(event.id) },
                        onOpenExpenses = { onOpenExpenses(event.id) },
                        onShare = {
                            scope.launch {
                                viewModel.whatsAppText()
                                    .onSuccess { shareToWhatsApp(context, it, shareTitle) }
                                    .onFailure { snackbar.showSnackbar(context.getString(it.messageRes())) }
                            }
                        },
                    )
                }

                item {
                    SectionHeader(stringResource(R.string.event_schedules), stringResource(R.string.schedule_add)) {
                        onAddSchedule(event.id)
                    }
                }
                if (state.schedules.isEmpty()) {
                    item { HelpText(stringResource(R.string.event_no_schedules)) }
                }
                items(state.schedules, key = { "s-${it.id}" }) { schedule ->
                    ScheduleCard(schedule, onClick = { onOpenSchedule(event.id, schedule.id) })
                }

                item {
                    SectionHeader(stringResource(R.string.event_roles), stringResource(R.string.roles_edit), viewModel::openRolesDialog)
                    RoleChips(event)
                }

                item {
                    SectionHeader(
                        stringResource(R.string.event_members_count, state.members.size),
                        stringResource(R.string.event_add_members),
                    ) { onAddMembers(event.id) }
                }
                if (state.members.isEmpty()) {
                    item { HelpText(stringResource(R.string.event_no_members)) }
                }
                items(state.members, key = { "m-${it.memberId}" }) { member ->
                    MemberRow(
                        member = member,
                        event = event,
                        onRoleChange = { viewModel.setMemberRole(member.memberId, it) },
                        onRemove = { confirmRemove = member },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    confirmStatus?.let { target ->
        ConfirmDialog(
            title = stringResource(target.actionRes()),
            message = stringResource(target.confirmRes()),
            confirmLabel = stringResource(target.actionRes()),
            onConfirm = {
                viewModel.setStatus(target)
                confirmStatus = null
            },
            onDismiss = { confirmStatus = null },
        )
    }

    confirmRemove?.let { member ->
        ConfirmDialog(
            title = stringResource(R.string.event_remove_member),
            message = stringResource(R.string.event_remove_member_confirm, member.name),
            confirmLabel = stringResource(R.string.remove),
            onConfirm = {
                viewModel.removeMember(member.memberId)
                confirmRemove = null
            },
            onDismiss = { confirmRemove = null },
        )
    }

    rolesDialog?.let { dialog ->
        ConfirmDialog(
            title = stringResource(R.string.roles_edit),
            message = "",
            confirmLabel = stringResource(R.string.save),
            confirmEnabled = !dialog.saving,
            onConfirm = viewModel::saveRoles,
            onDismiss = viewModel::closeRolesDialog,
            body = {
                RolesEditor(
                    roles = dialog.roles,
                    newRoleName = dialog.newRoleName,
                    onNewRoleNameChange = viewModel::onNewRoleNameChange,
                    onAdd = viewModel::addRole,
                    onRename = viewModel::renameRole,
                    onRemove = viewModel::removeRole,
                    error = dialog.error,
                )
            },
        )
    }
}

/** ORG-13: opening balance, collected, expenses and balance (BR-9). The chart arrives with payments. */
@Composable
private fun Overview(event: Event) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.event_dates, formatDate(event.startDate), formatDate(event.endDate)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(stringResource(event.status.labelRes()), MaterialTheme.colorScheme.onPrimary)
            }
            if (event.description.isNotBlank()) {
                Text(event.description, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                stringResource(R.string.event_balance, formatRupees(event.balance)),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Amount(R.string.event_opening_balance, event.openingBalance)
            Amount(R.string.event_total_collected, event.totalCollected)
            Amount(R.string.event_total_expenses, event.totalExpenses)
        }
    }
}

@Composable
private fun Amount(labelRes: Int, paise: Long) {
    Row {
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(formatRupees(paise), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        SectionTitle(title, Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun HelpText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ScheduleCard(schedule: Schedule, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(schedule.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.schedule_summary,
                    formatRupees(schedule.amountPerMember),
                    schedule.numberOfDues,
                    formatDate(schedule.startDate),
                    formatDate(schedule.endDate),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoleChips(event: Event) {
    if (event.roles.isEmpty()) {
        HelpText(stringResource(R.string.event_no_roles))
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        event.roles.forEach { role -> AssistChip(onClick = {}, label = { Text(role.name) }) }
    }
}

@Composable
private fun MemberRow(member: EventMember, event: Event, onRoleChange: (String?) -> Unit, onRemove: () -> Unit) {
    ListItem(
        headlineContent = { Text(member.name) },
        supportingContent = {
            Text(
                if (member.active) {
                    stringResource(R.string.event_member_paid, formatRupees(member.totalPaid))
                } else {
                    stringResource(R.string.event_member_disabled)
                },
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (event.roles.isNotEmpty()) RolePicker(event.roles, member.roleId, onRoleChange)
                if (member.totalPaid == 0L) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.PersonRemove, contentDescription = stringResource(R.string.event_remove_member))
                    }
                }
            }
        },
    )
}

@Composable
private fun StatusMenu(current: EventStatus, onSelect: (EventStatus) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_actions))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            EventStatus.entries.filter { it != current }.forEach { status ->
                DropdownMenuItem(
                    text = { Text(stringResource(status.actionRes())) },
                    onClick = {
                        open = false
                        onSelect(status)
                    },
                )
            }
        }
    }
}

private fun EventStatus.actionRes(): Int = when (this) {
    EventStatus.ACTIVE -> R.string.event_action_reactivate
    EventStatus.SUSPENDED -> R.string.event_action_suspend
    EventStatus.ARCHIVED -> R.string.event_action_archive
    EventStatus.CLOSED -> R.string.event_action_close
}

private fun EventStatus.confirmRes(): Int = when (this) {
    EventStatus.ACTIVE -> R.string.event_confirm_reactivate
    EventStatus.SUSPENDED -> R.string.event_confirm_suspend
    EventStatus.ARCHIVED -> R.string.event_confirm_archive
    EventStatus.CLOSED -> R.string.event_confirm_close
}

/** Stage 2 entry points: payments, member dues, expenses and the WhatsApp list. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventActions(
    onMarkPayment: () -> Unit,
    onOpenDues: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenExpenses: () -> Unit,
    onShare: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = onMarkPayment,
            label = { Text(stringResource(R.string.mark_payment)) },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
        )
        AssistChip(
            onClick = onOpenDues,
            label = { Text(stringResource(R.string.event_members_dues)) },
            leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
        )
        AssistChip(
            onClick = onOpenPayments,
            label = { Text(stringResource(R.string.payments_title)) },
            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
        )
        AssistChip(
            onClick = onOpenExpenses,
            label = { Text(stringResource(R.string.expenses_title)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
        )
        AssistChip(
            onClick = onShare,
            label = { Text(stringResource(R.string.share_whatsapp)) },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
        )
    }
}
