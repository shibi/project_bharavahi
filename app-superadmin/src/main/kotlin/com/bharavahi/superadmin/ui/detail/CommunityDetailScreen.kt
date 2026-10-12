package com.bharavahi.superadmin.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.model.Organization
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.ui.components.ConfirmDialog
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.StatusBadge
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.superadmin.R
import com.bharavahi.superadmin.ui.common.BackTopBar
import com.bharavahi.superadmin.ui.common.color
import com.bharavahi.superadmin.ui.common.formatDateTime
import com.bharavahi.superadmin.ui.common.labelRes

@Composable
fun CommunityDetailScreen(
    onBack: () -> Unit,
    onEdit: (orgId: String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: CommunityDetailViewModel = hiltViewModel(),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val actions by viewModel.actions.collectAsStateWithLifecycle()
    val currentOnDeleted by rememberUpdatedState(onDeleted)

    LaunchedEffect(viewModel) { viewModel.deleted.collect { currentOnDeleted() } }

    Scaffold(
        topBar = {
            BackTopBar(stringResource(R.string.detail_title), onBack) {
                if (content is DetailContent.Ready) {
                    IconButton(onClick = { onEdit(viewModel.orgId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.detail_edit))
                    }
                }
            }
        },
    ) { padding ->
        when (val c = content) {
            DetailContent.Loading -> LoadingContent(Modifier.padding(padding))
            is DetailContent.Error -> MessageContent(
                stringResource(c.error.messageRes()),
                Modifier.padding(padding),
                onRetry = viewModel::retry,
            )
            is DetailContent.Ready -> {
                val org = c.detail.organization
                if (org == null) {
                    MessageContent(stringResource(com.bharavahi.core.ui.R.string.core_error_not_found), Modifier.padding(padding))
                } else {
                    DetailList(org, c, actions, viewModel, Modifier.padding(padding))
                    Dialogs(org, actions, viewModel)
                }
            }
        }
    }
}

@Composable
private fun DetailList(
    org: Organization,
    content: DetailContent.Ready,
    actions: DetailActionState,
    viewModel: CommunityDetailViewModel,
    modifier: Modifier,
) {
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SummaryCard(org) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val suspended = org.status == OrganizationStatus.SUSPENDED
                OutlinedButton(
                    onClick = {
                        if (suspended) viewModel.toggleSuspended(suspend = false) else viewModel.showDialog(DetailDialog.SUSPEND)
                    },
                    enabled = !actions.busy,
                ) {
                    Text(stringResource(if (suspended) R.string.detail_reactivate else R.string.detail_suspend))
                }
                OutlinedButton(onClick = { viewModel.showDialog(DetailDialog.DELETE) }, enabled = !actions.busy) {
                    Text(stringResource(R.string.detail_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        actions.error?.takeIf { actions.dialog == null }?.let {
            item { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        }

        item { SectionTitle(stringResource(R.string.detail_admins)) }
        if (content.detail.admins.isEmpty()) {
            item { QuietText(stringResource(R.string.detail_no_admins)) }
        }
        items(content.detail.admins, key = OrgAdmin::uid) { admin ->
            AdminRow(admin, enabled = !actions.busy) { viewModel.setAdminActive(admin, it) }
            HorizontalDivider()
        }

        item { SectionTitle(stringResource(R.string.detail_activity)) }
        if (content.detail.activity.isEmpty()) {
            item { QuietText(stringResource(R.string.detail_no_activity)) }
        }
        items(content.detail.activity, key = { it.id }) { log ->
            ListItem(
                headlineContent = { Text(stringResource(log.action.labelRes())) },
                supportingContent = { Text(log.target) },
                trailingContent = { Text(formatDateTime(log.atMillis), style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

@Composable
private fun SummaryCard(org: Organization) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(org.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                StatusBadge(stringResource(org.status.labelRes()), org.status.color())
            }
            Text(
                if (org.contactPhone.isBlank()) org.type else stringResource(R.string.detail_type_phone, org.type, org.contactPhone),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (org.address.isNotBlank()) QuietText(org.address)
            Text(stringResource(R.string.detail_members_events, org.memberCount.toInt(), org.eventCount.toInt()))
        }
    }
}

@Composable
private fun AdminRow(admin: OrgAdmin, enabled: Boolean, onActiveChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(admin.name) },
        supportingContent = { Text(listOf(admin.email, admin.phone).filter(String::isNotBlank).joinToString(" · ")) },
        trailingContent = {
            Switch(checked = admin.active, onCheckedChange = onActiveChange, enabled = enabled)
        },
    )
}

@Composable
private fun Dialogs(org: Organization, actions: DetailActionState, viewModel: CommunityDetailViewModel) {
    when (actions.dialog) {
        DetailDialog.SUSPEND -> ConfirmDialog(
            title = stringResource(R.string.detail_suspend_title, org.name),
            message = stringResource(R.string.detail_suspend_message),
            confirmLabel = stringResource(R.string.detail_suspend),
            onConfirm = { viewModel.toggleSuspended(suspend = true) },
            onDismiss = { viewModel.showDialog(null) },
            confirmEnabled = !actions.busy,
        )
        DetailDialog.DELETE -> ConfirmDialog(
            title = stringResource(R.string.detail_delete_title, org.name),
            message = "",
            confirmLabel = stringResource(R.string.detail_delete),
            onConfirm = viewModel::confirmDelete,
            onDismiss = { viewModel.showDialog(null) },
            confirmEnabled = !actions.busy && actions.deleteConfirmText.trim() == org.name.trim(),
            body = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.detail_delete_message))
                    OutlinedTextField(
                        value = actions.deleteConfirmText,
                        onValueChange = viewModel::onDeleteConfirmTextChange,
                        label = { Text(stringResource(R.string.detail_delete_field)) },
                        singleLine = true,
                    )
                    actions.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
                }
            },
        )
        null -> Unit
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun QuietText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
