package com.bharavahi.org.ui.events

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.Event
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.TabTopBar
import com.bharavahi.org.ui.common.labelRes
import com.bharavahi.org.ui.dashboard.EventSummaryCard

@Composable
fun EventsScreen(
    onOpenEvent: (String) -> Unit,
    onCreateEvent: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TabTopBar(stringResource(R.string.tab_events)) {
                IconButton(onClick = onCreateEvent) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.events_create))
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                FilterChip(
                    selected = state.statusFilter == null,
                    onClick = { viewModel.onStatusFilter(null) },
                    label = { Text(stringResource(R.string.filter_all)) },
                )
                EventStatus.entries.forEach { status ->
                    FilterChip(
                        selected = state.statusFilter == status,
                        onClick = { viewModel.onStatusFilter(status) },
                        label = { Text(stringResource(status.labelRes())) },
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
                state.visible.isEmpty() -> MessageContent(stringResource(R.string.events_no_match))
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.visible, key = Event::id) { event ->
                        EventSummaryCard(event, onClick = { onOpenEvent(event.id) })
                    }
                }
            }
        }
    }
}
