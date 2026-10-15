package com.bharavahi.org.ui.eventdetail

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.bharavahi.core.ui.theme.LocalStatusColors
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.SheetsStatus
import com.bharavahi.core.domain.usecase.sheets.SheetsState
import com.bharavahi.core.domain.model.EventMember
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.Expense
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import java.time.format.DateTimeFormatter
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
import com.bharavahi.org.ui.common.sharePdf
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
    onAddExpense: (eventId: String) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rolesDialog by viewModel.rolesDialog.collectAsStateWithLifecycle()
    val sheets by viewModel.sheets.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmStatus by remember { mutableStateOf<EventStatus?>(null) }
    var confirmRemove by remember { mutableStateOf<EventMember?>(null) }
    val scope = rememberCoroutineScope()
    val shareTitle = stringResource(R.string.share_whatsapp)
    val pdfShareTitle = stringResource(R.string.pdf_share_title)
    var sharingPdf by remember { mutableStateOf(false) }

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
                item { Overview(event, state.pendingDues) }
                item {
                    EventActions(
                        memberCount = state.members.size,
                        onMarkPayment = { onMarkPayment(event.id) },
                        onAddExpense = { onAddExpense(event.id) },
                        onOpenPayments = { onOpenPayments(event.id) },
                        onOpenDues = { onOpenDues(event.id) },
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
                    SectionHeader(
                        title = stringResource(R.string.event_schedules),
                        actionLabel = stringResource(R.string.schedule_add),
                        count = state.schedules.size,
                    ) { onAddSchedule(event.id) }
                }
                if (state.schedules.isEmpty()) {
                    item { HelpText(stringResource(R.string.event_no_schedules)) }
                }
                items(state.schedules, key = { "s-${it.id}" }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        members = state.members,
                        onClick = { onOpenSchedule(event.id, schedule.id) },
                    )
                }
                if (state.schedules.isNotEmpty()) {
                    item {
                        SheetBackupCard(
                            event = event,
                            sheets = sheets,
                            sharingPdf = sharingPdf,
                            onSharePdf = {
                                scope.launch {
                                    sharingPdf = true
                                    viewModel.pdfReport()
                                        .mapCatching { sharePdf(context, it, pdfShareTitle) }
                                        .onFailure { snackbar.showSnackbar(context.getString(it.messageRes())) }
                                    sharingPdf = false
                                }
                            },
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = stringResource(R.string.event_recent_expenses),
                        actionLabel = stringResource(R.string.expense_add),
                        count = state.expenses.size,
                    ) { onAddExpense(event.id) }
                    if (state.expenses.isEmpty()) {
                        HelpText(stringResource(R.string.expenses_empty))
                    } else {
                        RecentExpensesCard(
                            expenses = state.expenses,
                            members = state.members,
                            onClick = { onOpenExpenses(event.id) },
                        )
                    }
                }

                item {
                    SectionHeader(stringResource(R.string.event_roles), stringResource(R.string.roles_edit)) {
                        viewModel.openRolesDialog()
                    }
                    RoleChips(event, state.members)
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

/** ORG-13: current balance with the opening/collected/expenses/pending breakdown and an income-vs-outflow bar (BR-9). */
@Composable
private fun Overview(event: Event, pendingDues: Long) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val inflow = Color(0xFFBCE9FD)
    val outflow = MaterialTheme.colorScheme.secondary
    val total = (event.totalCollected + event.totalExpenses).coerceAtLeast(0L)
    val inShare = if (total > 0) event.totalCollected.toFloat() / total else 0f
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = onPrimary,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.event_current_balance).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(stringResource(event.status.labelRes()), onPrimary)
            }
            Text(
                formatRupees(event.balance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.event_dates, formatDate(event.startDate), formatDate(event.endDate)),
                style = MaterialTheme.typography.bodySmall,
                color = onPrimary.copy(alpha = 0.7f),
            )
            if (event.description.isNotBlank()) {
                Text(event.description, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(R.string.event_opening_balance_short, event.openingBalance, null, Modifier.weight(1f))
                StatTile(R.string.event_total_collected, event.totalCollected, inflow, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(R.string.event_total_expenses, event.totalExpenses, outflow, Modifier.weight(1f))
                StatTile(R.string.event_pending_dues, pendingDues, null, Modifier.weight(1f))
            }
            if (total > 0) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).height(10.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.2f)),
                ) {
                    if (inShare > 0f) Box(Modifier.weight(inShare).fillMaxHeight().background(inflow))
                    if (inShare < 1f) Box(Modifier.weight(1f - inShare).fillMaxHeight().background(outflow))
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.event_share_collections, (inShare * 100).roundToInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = onPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.event_share_outflow, ((1f - inShare) * 100).roundToInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = onPrimary.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

/** Translucent tile on the teal overview card, with an accent dot for the coloured figures. */
@Composable
private fun StatTile(labelRes: Int, paise: Long, accent: Color?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (accent != null) Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
            Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
        }
        Text(
            formatRupees(paise),
            style = MaterialTheme.typography.titleMedium,
            color = accent ?: MaterialTheme.colorScheme.onPrimary,
        )
    }
}


/** Design: bold teal title with an optional muted count, and an amber "+ action" link on the right. */
@Composable
private fun SectionHeader(title: String, actionLabel: String, count: Int? = null, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (count != null) {
                Text(
                    "($count)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
            Text(
                actionLabel,
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun HelpText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * ORG-36 and ORG-34: the event's Google Sheet backup in its own card: where it stands, a link to open the
 * sheet once it exists, and a PDF of the same tables that works without a Google account.
 */
@Composable
private fun SheetBackupCard(event: Event, sheets: SheetsState?, sharingPdf: Boolean, onSharePdf: () -> Unit) {
    val context = LocalContext.current
    val connection = sheets?.connection
    val sheetId = event.sheetId
    val status = when {
        sheets == null -> null
        connection == null -> stringResource(R.string.sheets_event_off)
        connection.status == SheetsStatus.NEEDS_RECONNECT -> stringResource(R.string.sheets_event_paused)
        sheetId == null -> stringResource(R.string.sheets_event_pending)
        else -> event.sheetSyncedAtMillis?.let {
            stringResource(R.string.sheets_event_synced, DateUtils.getRelativeTimeSpanString(it).toString())
        } ?: stringResource(R.string.sheets_event_pending)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.sheets_card_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            if (status != null) {
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (connection?.status == SheetsStatus.NEEDS_RECONNECT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (sheetId != null) {
                    OutlinedButton(onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/spreadsheets/d/$sheetId")))
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.sheets_open), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                FilledTonalButton(onClick = onSharePdf, enabled = !sharingPdf) {
                    if (sharingPdf) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(stringResource(R.string.pdf_share), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/**
 * Design: white 20dp card with name and per-member dues, a lock pill, the date range with a calendar
 * icon and a collection-progress bar (collected so far against amount per member times members).
 */
@Composable
private fun ScheduleCard(schedule: Schedule, members: List<EventMember>, onClick: () -> Unit) {
    val collected = members.sumOf { member ->
        (1..schedule.numberOfDues).sumOf { member.paidOn(schedule.id, it) }
    }
    val target = schedule.amountPerMember * members.size
    val progress = if (target > 0) (collected.toFloat() / target).coerceIn(0f, 1f) else 0f
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        schedule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(
                            R.string.schedule_per_member_dues,
                            formatRupees(schedule.amountPerMember),
                            schedule.numberOfDues,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (schedule.locked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            stringResource(R.string.schedule_locked_short),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Text(
                    formatDate(schedule.startDate) + " – " + formatDate(schedule.endDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.schedule_collection_progress),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.schedule_percent_collected, (progress * 100).roundToInt()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                ) {
                    if (progress > 0f) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.schedule_amount_collected, formatRupees(collected)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.schedule_target, formatRupees(target)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

private const val RECENT_EXPENSE_LIMIT = 3
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

/**
 * Design: the latest expenses in one white 20dp card, rows split by hairline dividers. Each row has a
 * round icon, the title, "Paid by <name> · <date>", the amount in red and a sync note when pending.
 */
@Composable
private fun RecentExpensesCard(expenses: List<Expense>, members: List<EventMember>, onClick: () -> Unit) {
    val recent = expenses
        .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAtMillis })
        .take(RECENT_EXPENSE_LIMIT)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        recent.forEachIndexed { index, expense ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ExpenseRow(expense, payer = members.firstOrNull { it.memberId == expense.paidByMemberId }?.name)
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, payer: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                expense.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val date = expense.date?.format(SHORT_DATE).orEmpty()
            val subtitle = if (payer != null) {
                val text = stringResource(R.string.expense_paid_by_on, payer, date)
                val start = text.indexOf(payer)
                buildAnnotatedString {
                    append(text)
                    if (start >= 0) {
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            start,
                            start + payer.length,
                        )
                    }
                }
            } else {
                AnnotatedString(date)
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "-" + formatRupees(expense.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
            if (expense.waitingToSync) {
                Text(
                    stringResource(R.string.payment_waiting_to_sync),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalStatusColors.current.partlyPaid,
                )
            }
        }
    }
}

/**
 * Design: committee roles as chips pairing the role with the member who holds it.
 * A role held by several members collapses into one chip showing how many hold it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoleChips(event: Event, members: List<EventMember>) {
    if (event.roles.isEmpty()) {
        HelpText(stringResource(R.string.event_no_roles))
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        event.roles.forEach { role ->
            val holders = members.filter { it.roleId == role.id }
            when (holders.size) {
                0 -> RoleChip(role.name, avatar = null, holder = null)
                1 -> {
                    val name = holders.single().name
                    RoleChip(role.name, avatar = initials(name), holder = name)
                }
                else -> RoleChip(
                    role.name,
                    avatar = holders.size.toString(),
                    holder = pluralStringResource(R.plurals.event_role_member_count, holders.size, holders.size),
                )
            }
        }
    }
}

private fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }

@Composable
private fun RoleChip(role: String, avatar: String?, holder: String?) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (avatar != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    avatar,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        Column {
            Text(
                role.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            if (holder != null) Text(holder, style = MaterialTheme.typography.labelMedium)
        }
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

/**
 * Design: Mark Payment (amber) and + Expense side by side, then Payments, Members (dues) and WhatsApp as
 * icon-over-label tiles. The expense list itself opens from the recent expenses card.
 */
@Composable
private fun EventActions(
    memberCount: Int,
    onMarkPayment: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenDues: () -> Unit,
    onShare: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onMarkPayment,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.mark_payment), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(
                onClick = onAddExpense,
                shape = shape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(R.string.event_add_expense_short),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionTile(
                label = stringResource(R.string.payments_title),
                onClick = onOpenPayments,
                modifier = Modifier.weight(1f),
            ) { Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ActionTile(
                label = stringResource(R.string.event_members_short, memberCount),
                onClick = onOpenDues,
                modifier = Modifier.weight(1f),
            ) { Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ActionTile(
                label = stringResource(R.string.event_whatsapp_short),
                onClick = onShare,
                accent = LocalStatusColors.current.paid,
                modifier = Modifier.weight(1f),
            ) {
                // The real logo keeps its own brand colours.
                Icon(painterResource(R.drawable.ic_whatsapp), contentDescription = null, tint = Color.Unspecified)
            }
        }
    }
}

/** Outlined tile with an icon above its label; an [accent] tints the tile and its label. */
@Composable
private fun ActionTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    icon: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = accent?.copy(alpha = 0.12f)?.compositeOver(MaterialTheme.colorScheme.surface)
                ?: MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, accent?.copy(alpha = 0.3f) ?: MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.height(72.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        ) {
            icon()
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent ?: MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
