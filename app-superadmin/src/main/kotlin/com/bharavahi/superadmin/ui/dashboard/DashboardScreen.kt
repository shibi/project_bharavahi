package com.bharavahi.superadmin.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.StatCard
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.superadmin.R
import com.bharavahi.superadmin.ui.common.formatDateTime
import com.bharavahi.superadmin.ui.common.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenCommunities: () -> Unit,
    onOpenCommunity: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            DashboardUiState.Loading -> LoadingContent(Modifier.padding(padding))
            is DashboardUiState.Error -> MessageContent(
                message = stringResource(s.error.messageRes()),
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding),
            )
            is DashboardUiState.Ready -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val stats = s.dashboard.stats
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            stringResource(R.string.dashboard_active_communities),
                            stats.activeOrganizations.toString(),
                            Modifier.weight(1f),
                        )
                        StatCard(
                            stringResource(R.string.dashboard_suspended_communities),
                            stats.suspendedOrganizations.toString(),
                            Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            stringResource(R.string.dashboard_total_members),
                            stats.totalMembers.toString(),
                            Modifier.weight(1f),
                        )
                        StatCard(
                            stringResource(R.string.dashboard_total_events),
                            stats.totalEvents.toString(),
                            Modifier.weight(1f),
                        )
                    }
                }
                item {
                    Button(onClick = onOpenCommunities, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text(stringResource(R.string.dashboard_manage_communities))
                    }
                }
                item {
                    Text(
                        stringResource(R.string.dashboard_recent_activity),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (s.dashboard.recentActivity.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.dashboard_no_activity),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(s.dashboard.recentActivity, key = { it.orgId + it.id }) { log ->
                    ListItem(
                        headlineContent = { Text(stringResource(log.action.labelRes())) },
                        supportingContent = { Text(s.orgNames[log.orgId] ?: log.target) },
                        trailingContent = { Text(formatDateTime(log.atMillis), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.clickable { onOpenCommunity(log.orgId) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
