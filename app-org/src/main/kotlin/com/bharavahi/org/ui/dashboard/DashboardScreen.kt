package com.bharavahi.org.ui.dashboard

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import com.bharavahi.org.ui.common.InitialsAvatar
import com.bharavahi.org.ui.common.color
import com.bharavahi.org.ui.common.labelRes
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onOpenEvent: (String) -> Unit,
    onCreateEvent: () -> Unit,
    onAddMember: () -> Unit,
    onCollectDues: () -> Unit,
    onViewAllEvents: () -> Unit = {},
    onSeeAllPayments: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dashboard = state.dashboard
    Scaffold(topBar = { DashboardTopBar(dashboard?.communityName.orEmpty()) }) { padding ->
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
                item { HeroCard(dashboard, onCollectDues) }
                val pending = dashboard.events.firstOrNull { it.pendingMembers > 0 }
                if (pending != null) item { ReminderCard(pending) }
                item {
                    SectionHeader(
                        stringResource(R.string.dashboard_active_events),
                        stringResource(R.string.dashboard_view_all),
                        onViewAllEvents,
                    )
                }
                if (dashboard.activeEvents.isEmpty()) {
                    item { GettingStarted(onCreateEvent, onAddMember) }
                }
                items(dashboard.events, key = { it.event.id }) { progress ->
                    EventProgressCard(progress, onClick = { onOpenEvent(progress.event.id) })
                }
                if (dashboard.recentPayments.isNotEmpty()) {
                    val firstEventId = dashboard.events.firstOrNull()?.event?.id
                    item {
                        SectionHeader(
                            stringResource(R.string.dashboard_recent_payments),
                            stringResource(R.string.dashboard_see_all).takeIf { firstEventId != null },
                        ) { firstEventId?.let(onSeeAllPayments) }
                    }
                    item { RecentPaymentsCard(dashboard.recentPayments) }
                }
            }
        }
    }
}

@Composable
private fun DashboardTopBar(communityName: String) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        Icons.Default.Apartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        communityName.ifBlank { stringResource(R.string.tab_dashboard) },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    Text(
                        stringResource(R.string.app_name).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.clickable(onClick = onAction).padding(4.dp),
            )
        }
    }
}

/** UI-11: the teal hero card with a progress ring for the first active event. */
@Composable
private fun HeroCard(dashboard: OrgDashboard, onCollectDues: () -> Unit) {
    val first = dashboard.events.firstOrNull()
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                dashboard.communityName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            )
            Text(
                pluralStringResource(R.plurals.dashboard_members, dashboard.activeMembers, dashboard.activeMembers) +
                    " · " +
                    pluralStringResource(R.plurals.dashboard_events, dashboard.events.size, dashboard.events.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.dashboard_total_collection),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    )
                    Text(
                        formatRupees(dashboard.activeEvents.sumOf { it.totalCollected }),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (first != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                            Text(
                                pluralStringResource(
                                    R.plurals.dashboard_pending_members,
                                    first.pendingMembers,
                                    first.pendingMembers,
                                    first.event.name,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                if (first != null) {
                    ProgressRing(first.progress, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onPrimary)
                }
            }
            Button(
                onClick = onCollectDues,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.dashboard_collect_dues), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, color: Color, textColor: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(start = 12.dp)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(72.dp),
            color = color,
            trackColor = textColor.copy(alpha = 0.25f),
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = textColor)
            Text(
                stringResource(R.string.dashboard_ring_collected),
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f),
            )
        }
    }
}

/** Peach callout: who still owes this period, with a one-tap share of the reminder text. */
@Composable
private fun ReminderCard(progress: EventProgress) {
    val context = LocalContext.current
    val text = pluralStringResource(
        R.plurals.dashboard_reminder_text,
        progress.pendingMembers,
        progress.pendingMembers,
        progress.event.name,
    )
    val share = stringResource(R.string.dashboard_share_reminder)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
            ) {
                Icon(
                    Icons.Default.Campaign,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    share,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.clickable {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                        context.startActivity(Intent.createChooser(send, share))
                    },
                )
            }
        }
    }
}

/** Active event card with the share collected so far and this period's numbers. */
@Composable
private fun EventProgressCard(progress: EventProgress, onClick: () -> Unit) {
    val event = progress.event
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(event.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.event_dates, formatDate(event.startDate), formatDate(event.endDate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                val statusColor =
                    if (progress.pendingMembers > 0) LocalStatusColors.current.pending else LocalStatusColors.current.paid
                Text(
                    pluralStringResource(R.plurals.dashboard_pending_short, progress.pendingMembers, progress.pendingMembers),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
            LinearProgressIndicator(
                progress = { progress.progress },
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.tertiaryContainer,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        R.string.dashboard_event_collected,
                        formatRupees(event.totalCollected),
                        formatRupees(progress.expected),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.dashboard_this_period, formatRupees(progress.collectedThisPeriod)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun RecentPaymentsCard(payments: List<RecentPayment>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(8.dp)) {
            payments.forEachIndexed { index, recent ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                RecentPaymentRow(recent)
            }
        }
    }
}

@Composable
private fun RecentPaymentRow(recent: RecentPayment) {
    val reversal = recent.payment.amount < 0
    val amountColor = if (reversal) LocalStatusColors.current.pending else LocalStatusColors.current.paid
    Row(
        Modifier.fillMaxWidth().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InitialsAvatar(recent.memberName)
        Column(Modifier.weight(1f)) {
            Text(recent.memberName, style = MaterialTheme.typography.labelLarge)
            Text(
                recent.eventName + " · " + formatDate(recent.payment.paidOn),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(recent.payment.method.labelRes()).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp),
                )
                Text(
                    stringResource(if (reversal) R.string.dashboard_reversal else R.string.due_status_paid),
                    style = MaterialTheme.typography.labelSmall,
                    color = amountColor,
                    modifier = Modifier
                        .background(amountColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp),
                )
            }
        }
        Text(
            (if (reversal) "" else "+") + formatRupees(recent.payment.amount),
            style = MaterialTheme.typography.titleSmall,
            color = amountColor,
        )
    }
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
