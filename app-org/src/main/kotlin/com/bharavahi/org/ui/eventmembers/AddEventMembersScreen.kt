package com.bharavahi.org.ui.eventmembers

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.BackTopBar

@Composable
fun AddEventMembersScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddEventMembersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { currentOnSaved() }
    }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.event_add_members), onBack) },
        bottomBar = {
            if (state.candidates.isNotEmpty()) {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saving && state.selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(pluralStringResource(R.plurals.add_members_button, state.selected.size, state.selected.size))
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingContent(Modifier.padding(padding))
            state.candidates.isEmpty() -> MessageContent(
                message = stringResource(state.error?.messageRes() ?: R.string.add_members_none_left),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
                item {
                    Text(
                        stringResource(R.string.add_members_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    TextButton(onClick = viewModel::toggleAll, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            stringResource(
                                if (state.selected.size == state.candidates.size) R.string.select_none else R.string.select_all,
                            ),
                        )
                    }
                }
                memberRoleItems(
                    members = state.candidates,
                    selected = state.selected,
                    roles = state.roles,
                    onToggle = viewModel::toggle,
                    onRoleChange = viewModel::setRole,
                )
                state.error?.let {
                    item {
                        Text(
                            stringResource(it.messageRes()),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
