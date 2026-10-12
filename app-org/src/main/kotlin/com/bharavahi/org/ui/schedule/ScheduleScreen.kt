package com.bharavahi.org.ui.schedule

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.DueStatus
import com.bharavahi.core.domain.model.DueSummary
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
import com.bharavahi.org.ui.common.color
import com.bharavahi.org.ui.common.labelRes

@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    onEdit: (eventId: String, scheduleId: String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnDeleted by rememberUpdatedState(onDeleted)
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.deleted.collect { currentOnDeleted() }
    }
    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            snackbar.showSnackbar(context.getString(it.messageRes()))
            viewModel.dismissError()
        }
    }

    val schedule = state.schedule
    Scaffold(
        topBar = {
            BackTopBar(schedule?.name.orEmpty(), onBack) {
                if (schedule != null && !schedule.locked) {
                    IconButton(onClick = { onEdit(viewModel.eventId, viewModel.scheduleId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.schedule_edit))
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.schedule_delete))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading -> LoadingContent(Modifier.padding(padding))
            schedule == null -> MessageContent(
                message = stringResource(state.error?.messageRes() ?: R.string.event_not_found),
                modifier = Modifier.padding(padding),
                onRetry = viewModel::retry,
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Header(schedule) }
                items(state.dues, key = { it.due.index }) { summary ->
                    DueCard(summary, current = summary.due.index == state.currentDueIndex)
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.schedule_delete),
            message = stringResource(R.string.schedule_delete_confirm, schedule?.name.orEmpty()),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Header(schedule: Schedule) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        if (schedule.description.isNotBlank()) Text(schedule.description)
        Text(
            stringResource(
                R.string.schedule_summary,
                formatRupees(schedule.amountPerMember),
                schedule.numberOfDues,
                formatDate(schedule.startDate),
                formatDate(schedule.endDate),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(if (schedule.locked) R.string.schedule_locked else R.string.schedule_unlocked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DueCard(summary: DueSummary, current: Boolean) {
    Card(
        colors = if (current) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(summary.due.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(formatRupees(summary.due.amount), style = MaterialTheme.typography.titleSmall)
            }
            Text(
                if (current) {
                    stringResource(R.string.due_current, formatDate(summary.due.dueDate))
                } else {
                    stringResource(R.string.due_date, formatDate(summary.due.dueDate))
                },
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CountBadge(DueStatus.PAID, summary.paid)
                CountBadge(DueStatus.PARTLY_PAID, summary.partlyPaid)
                CountBadge(DueStatus.PENDING, summary.pending)
            }
        }
    }
}

@Composable
private fun CountBadge(status: DueStatus, count: Int) {
    StatusBadge(stringResource(R.string.due_count, count, stringResource(status.labelRes())), status.color())
}
