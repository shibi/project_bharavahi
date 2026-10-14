package com.bharavahi.org.ui.members

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.Member
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.StatusBadge
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.InitialsAvatar
import com.bharavahi.org.ui.common.TabTopBar
import com.bharavahi.org.ui.common.color
import com.bharavahi.org.ui.common.labelRes

@Composable
fun MembersScreen(
    onAddMember: () -> Unit,
    onOpenMember: (String) -> Unit,
    viewModel: MembersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TabTopBar(stringResource(R.string.tab_members)) {
                IconButton(onClick = onAddMember) {
                    Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.members_add))
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(stringResource(R.string.members_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (state.query.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            ) {
                StatusChip(
                    label = "${stringResource(R.string.filter_all)} (${state.totalCount})",
                    selected = state.statusFilter == null,
                    onClick = { viewModel.onStatusFilter(null) },
                )
                MemberStatus.entries.forEach { status ->
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
                state.totalCount == 0 -> MessageContent(stringResource(R.string.members_empty))
                state.visible.isEmpty() -> MessageContent(stringResource(R.string.members_no_match))
                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.visible, key = Member::id) { member ->
                        MemberRow(member, onClick = { onOpenMember(member.id) })
                    }
                }
            }
        }
    }
}

/** Design: pill filter chip, solid teal when selected, outlined white otherwise. */
@Composable
private fun StatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/** Design: white 20dp card with teal initials avatar, nickname line, phone, status pill and chevron. */
@Composable
private fun MemberRow(member: Member, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            InitialsAvatar(
                member.name,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                size = 44.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(member.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (member.nickname.isNotBlank()) {
                    Text(
                        member.nickname,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (member.phone.isNotBlank()) {
                        Text(
                            member.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusBadge(stringResource(member.status.labelRes()), member.status.color())
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
