package com.bharavahi.superadmin.ui.communities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.StatusBadge
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.superadmin.R
import com.bharavahi.superadmin.ui.common.BackTopBar
import com.bharavahi.superadmin.ui.common.color
import com.bharavahi.superadmin.ui.common.labelRes

@Composable
fun CommunitiesScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpenCommunity: (String) -> Unit,
    viewModel: CommunitiesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.communities_title), onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.communities_add)) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(stringResource(R.string.communities_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                FilterChip(
                    selected = state.statusFilter == null,
                    onClick = { viewModel.onStatusFilter(null) },
                    label = { Text(stringResource(R.string.communities_filter_all)) },
                )
                listOf(OrganizationStatus.ACTIVE, OrganizationStatus.SUSPENDED).forEach { status ->
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
                state.totalCount == 0 -> MessageContent(stringResource(R.string.communities_empty))
                state.visible.isEmpty() -> MessageContent(stringResource(R.string.communities_no_match))
                else -> LazyColumn {
                    items(state.visible, key = Organization::id) { org ->
                        CommunityRow(org, onClick = { onOpenCommunity(org.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityRow(org: Organization, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(org.name) },
        supportingContent = {
            Text(
                org.type + " · " +
                    stringResource(R.string.communities_counts, org.memberCount.toInt(), org.eventCount.toInt()),
            )
        },
        trailingContent = { StatusBadge(stringResource(org.status.labelRes()), org.status.color()) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
