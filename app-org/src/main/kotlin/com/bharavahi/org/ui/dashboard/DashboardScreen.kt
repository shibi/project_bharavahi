package com.bharavahi.org.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.usecase.orgdashboard.EventProgress
import com.bharavahi.core.domain.usecase.orgdashboard.OrgDashboard
import com.bharavahi.core.domain.usecase.orgdashboard.RecentPayment
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.StatusBadge
import com.bharavahi.core.ui.components.formatDate
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.core.ui.theme.LocalStatusColors
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.SectionTitle
import com.bharavahi.org.ui.common.TabTopBar
import com.bharavahi.org.ui.common.color
import com.bharavahi.org.ui.common.labelRes
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onOpenEvent: (String) -> Unit,
    onCreateEvent: () -> Unit,
    onAddMember: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { TabTopBar(stringResource(R.string.tab_dashboard)) }) { padding ->
        val dashboard = state.dashboard
        when {
            state.loading -> LoadingContent(Modifier.padding(padding))
            dashboard == null -> MessageContent(
                message = stringResource(state.error?.messageRes() ?: R.string.dashboard_unavailable),
                modifier = Modifier.padding(padding),
                onRetry = viewModel::retry,
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { HeroCard(dashboard) }
                item { SectionTitle(stringResource(R.string.dashboard_active_events)) }
                if (dashboard.activeEvents.isEmpty()) {
                    item { GettingStarted(onCreateEvent, onAddMember) }
                }
                items(dashboard.events, key = { it.event.id }) { progress ->
                    EventProgressCard(progress, onClick = { onOpenEvent(progress.event.id) })
                }
                if (dashboard.recentPayments.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.dashboard_recent_payments)) }
                    items(dashboard.recentPayments, key = { it.payment.id }) { recent -> RecentPaymentRow(recent) }
                }
            }
        }
    }
}

/** UI-11: the teal hero card with a progress ring for the first active event. */
@Composable
private fun HeroCard(dashboard: OrgDashboard) {
    val first = dashboard.events.firstOrNull()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(dashboard.communityName, style = MaterialTheme.typography.headlineSmall)
                Text(
                    pluralStringResource(R.plurals.dashboard_members, dashboard.activeMembers, dashboard.activeMembers) +
                        " · " +
                        pluralStringResource(R.plurals.dashboard_events, dashboard.events.size, dashboard.events.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.dashboard_collected, formatRupees(dashboard.activeEvents.sumOf { it.totalCollected })),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (first != null) {
                    Text(
                        pluralStringResource(R.plurals.dashboard_pending_members, first.pendingMembers, first.pendingMembers, first.event.name),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (first != null) ProgressRing(first.progress, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, color: Color, textColor: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(start = 16.dp)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(72.dp),
            color = color,
            trackColor = textColor.copy(alpha = 0.2f),
            strokeWidth = 8.dp,
        )
        Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall, color = textColor)
    }
}

/** Active event card with the share collected so far and this period's numbers. */
@Composable
private fun EventProgressCard(progress: EventProgress, onClick: () -> Unit) {
    val event = progress.event
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(event.name, style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { progress.progress }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.dashboard_event_collected, formatRupees(event.totalCollected), formatRupees(progress.expected)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.dashboard_this_period, formatRupees(progress.collectedThisPeriod)),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    pluralStringResource(R.plurals.dashboard_pending_short, progress.pendingMembers, progress.pendingMembers),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (progress.pendingMembers > 0) LocalStatusColors.current.pending else LocalStatusColors.current.paid,
                )
            }
        }
    }
}

@Composable
private fun RecentPaymentRow(recent: RecentPayment) {
    ListItem(
        headlineContent = { Text(recent.memberName) },
        supportingContent = { Text(recent.eventName + " · " + formatDate(recent.payment.paidOn)) },
        trailingContent = {
            Text(
                formatRupees(recent.payment.amount),
                color = if (recent.payment.amount < 0) LocalStatusColors.current.pending else LocalStatusColors.current.paid,
                style = MaterialTheme.typography.titleSmall,
            )
        },
    )
}

@Composable
private fun GettingStarted(onCreateEvent: () -> Unit, onAddMember: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.dashboard_getting_started),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddMember) { Text(stringResource(R.string.members_add)) }
                OutlinedButton(onClick = onCreateEvent) { Text(stringResource(R.string.events_create)) }
            }
        }
    }
}

@Composable
fun EventSummaryCard(event: Event, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(event.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (event.status != EventStatus.ACTIVE) {
                    StatusBadge(stringResource(event.status.labelRes()), event.status.color())
                }
            }
            Text(
                stringResource(R.string.event_dates, formatDate(event.startDate), formatDate(event.endDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.event_collected_short, formatRupees(event.totalCollected)))
                Text(stringResource(R.string.event_balance_short, formatRupees(event.balance)))
            }
        }
    }
}
