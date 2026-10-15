package com.bharavahi.org.ui.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.formatDate
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.core.ui.theme.LocalStatusColors
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.TabTopBar
import com.bharavahi.org.ui.common.color
import com.bharavahi.org.ui.common.labelRes

@Composable
fun EventsScreen(
    onOpenEvent: (String) -> Unit,
    onCreateEvent: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activeCount = state.statusCounts[EventStatus.ACTIVE] ?: 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TabTopBar(stringResource(R.string.tab_events)) {
                if (activeCount > 0) {
                    Text(
                        stringResource(R.string.events_active_badge, activeCount),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateEvent,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                text = { Text(stringResource(R.string.events_create), fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                StatusChip(
                    label = "${stringResource(R.string.filter_all)} (${state.totalCount})",
                    selected = state.statusFilter == null,
                    onClick = { viewModel.onStatusFilter(null) },
                )
                EventStatus.entries.forEach { status ->
                    StatusChip(
                        label = "${stringResource(status.labelRes())} (${state.statusCounts[status] ?: 0})",
                        selected = state.statusFilter == status,
                        onClick = { viewModel.onStatusFilter(status) },
                    )
                }
            }
            when {
                state.loading -> LoadingContent()
                state.error != null -> MessageContent(
                    message = stringResource(state.error!!.messageRes()),
                    onRetry = viewModel::retry,
                )
                state.totalCount == 0 -> MessageContent(stringResource(R.string.events_empty))
                else -> LazyColumn(
                    // Bottom padding keeps the last card clear of the floating button.
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { FundsSummaryCard(state.totalCollected, state.totalExpected) }
                    if (state.visible.isEmpty()) {
                        item { MessageContent(stringResource(R.string.events_no_match)) }
                    }
                    items(state.visible, key = Event::id) { event ->
                        if (event.status == EventStatus.CLOSED || event.status == EventStatus.ARCHIVED) {
                            FinishedEventCard(event, onClick = { onOpenEvent(event.id) })
                        } else {
                            EventCard(event, onClick = { onOpenEvent(event.id) })
                        }
                    }
                }
            }
        }
    }
}

/** Design: pill filter chip with a count; the selected one is solid teal with a check mark. */
@Composable
private fun StatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else {
            null
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/** Design: tinted summary strip with a wallet icon, everything collected across events and the sum of their totals. */
@Composable
private fun FundsSummaryCard(totalCollected: Long, totalExpected: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Column {
            Text(
                stringResource(R.string.events_total_funds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    formatRupees(totalCollected),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.events_collected_suffix),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Text(
                stringResource(R.string.events_total_expected, formatRupees(totalExpected)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Design: white 20dp card with bold name, tinted status pill, date range and a chevron, then
 * Collected / Spent / Balance tiles.
 */
@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
    EventCardContainer(onClick = onClick) {
        EventHeader(event, Icons.Default.DateRange, finished = false)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AmountTile(
                label = stringResource(R.string.events_tile_collected),
                amount = formatRupees(event.totalCollected),
                amountColor = MaterialTheme.colorScheme.primary,
                note = stringResource(R.string.events_tile_opening, formatRupees(event.openingBalance)),
                noteColor = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f),
            )
            AmountTile(
                label = stringResource(R.string.events_tile_spent),
                amount = formatRupees(event.totalExpenses),
                amountColor = MaterialTheme.colorScheme.error,
                note = null,
                noteColor = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f),
            )
            AmountTile(
                label = stringResource(R.string.events_tile_balance),
                amount = formatRupees(event.balance),
                amountColor = MaterialTheme.colorScheme.primary,
                note = stringResource(R.string.events_tile_in_hand),
                noteColor = LocalStatusColors.current.paid,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Design: closed and archived events as a dimmed card with one inline money summary. */
@Composable
private fun FinishedEventCard(event: Event, onClick: () -> Unit) {
    EventCardContainer(onClick = onClick, modifier = Modifier.alpha(0.9f)) {
        EventHeader(event, Icons.Default.EventAvailable, finished = true)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            InlineAmount(stringResource(R.string.events_tile_collected), formatRupees(event.totalCollected), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            InlineAmount(stringResource(R.string.events_tile_spent), formatRupees(event.totalExpenses), MaterialTheme.colorScheme.error, Modifier.weight(1f))
            InlineAmount(stringResource(R.string.events_tile_balance), formatRupees(event.balance), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun EventCardContainer(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun EventHeader(event: Event, dateIcon: androidx.compose.ui.graphics.vector.ImageVector, finished: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    event.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (finished) FontWeight.SemiBold else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                StatusPill(event.status)
            }
            if (event.startDate != null || event.endDate != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(dateIcon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.outline)
                    Text(
                        stringResource(R.string.event_dates, formatDate(event.startDate), formatDate(event.endDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (finished) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

/** Soft tinted pill; active and suspended get a dot in their status colour, finished ones stay neutral. */
@Composable
private fun StatusPill(status: EventStatus) {
    val color = status.color()
    val neutral = status == EventStatus.CLOSED || status == EventStatus.ARCHIVED
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(
                if (neutral) MaterialTheme.colorScheme.surfaceVariant else color.copy(alpha = 0.12f),
                CircleShape,
            )
            .then(if (neutral) Modifier else Modifier.border(1.dp, color.copy(alpha = 0.2f), CircleShape))
            .padding(horizontal = 10.dp, vertical = 2.dp),
    ) {
        if (!neutral) Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(
            stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (neutral) FontWeight.Medium else FontWeight.SemiBold,
            color = if (neutral) MaterialTheme.colorScheme.onSurfaceVariant else color,
        )
    }
}

@Composable
private fun AmountTile(
    label: String,
    amount: String,
    amountColor: Color,
    note: String?,
    noteColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            amount,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = amountColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (note != null) {
            Text(
                note,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = noteColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun InlineAmount(label: String, amount: String, amountColor: Color, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier) {
        Text(
            stringResource(R.string.events_inline_amount, label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(amount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = amountColor)
    }
}
