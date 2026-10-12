package com.bharavahi.org.ui.eventform

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.BackTopBar
import com.bharavahi.org.ui.common.DateField
import com.bharavahi.org.ui.eventmembers.memberRoleItems

@Composable
fun EventFormScreen(
    onBack: () -> Unit,
    onSaved: (eventId: String) -> Unit,
    viewModel: EventFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { currentOnSaved(it) }
    }
    val goBack = { if (!viewModel.back()) onBack() }
    BackHandler(enabled = !state.isEdit && state.step != EventStep.DETAILS) { goBack() }

    val title = stringResource(if (state.isEdit) R.string.event_form_edit_title else R.string.event_form_create_title)
    Scaffold(
        topBar = { BackTopBar(title, goBack) },
        bottomBar = {
            if (!state.loading) {
                FormButtons(state, onBack = goBack, onNext = viewModel::next, onSave = viewModel::save)
            }
        },
    ) { padding ->
        if (state.loading) {
            LoadingContent(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!state.isEdit) {
                item {
                    val step = state.step.ordinal + 1
                    Text(
                        stringResource(R.string.event_form_step, step, EventStep.entries.size, stringResource(state.step.titleRes())),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(
                        progress = { step.toFloat() / EventStep.entries.size },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
            }
            if (state.isEdit || state.step == EventStep.DETAILS) {
                item { DetailsFields(state, viewModel) }
            }
            if (state.isEdit || state.step == EventStep.BALANCE) {
                item { BalanceField(state, viewModel) }
            }
            if (!state.isEdit && state.step == EventStep.ROLES) {
                item {
                    Text(
                        stringResource(R.string.event_form_roles_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    RolesEditor(
                        roles = state.roles,
                        newRoleName = state.newRoleName,
                        onNewRoleNameChange = viewModel::onNewRoleNameChange,
                        onAdd = viewModel::addRole,
                        onRename = viewModel::renameRole,
                        onRemove = viewModel::removeRole,
                        error = state.fieldErrors[Field.ROLE_NAME],
                    )
                }
            }
            if (!state.isEdit && state.step == EventStep.MEMBERS) {
                item {
                    Text(
                        stringResource(R.string.event_form_members_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.members.isEmpty()) {
                        Text(stringResource(R.string.event_form_no_members), modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        TextButton(onClick = viewModel::toggleAll) {
                            Text(
                                stringResource(
                                    if (state.selected.size == state.members.size) R.string.select_none else R.string.select_all,
                                ),
                            )
                        }
                    }
                }
                memberRoleItems(
                    members = state.members,
                    selected = state.selected,
                    roles = state.roles,
                    onToggle = viewModel::toggleMember,
                    onRoleChange = viewModel::setRole,
                )
            }
            state.error?.let {
                item { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun DetailsFields(state: EventFormUiState, viewModel: EventFormViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FormTextField(
            value = state.name,
            onValueChange = { viewModel.onChange(Field.NAME, it) },
            label = stringResource(R.string.event_name),
            error = state.fieldErrors[Field.NAME],
            modifier = Modifier.fillMaxWidth(),
        )
        FormTextField(
            value = state.description,
            onValueChange = { viewModel.onChange(Field.DESCRIPTION, it) },
            label = stringResource(R.string.event_description),
            modifier = Modifier.fillMaxWidth(),
        )
        DateField(
            value = state.startDate,
            onValueChange = viewModel::onStartDate,
            label = stringResource(R.string.start_date),
            error = state.fieldErrors[Field.START_DATE],
            modifier = Modifier.fillMaxWidth(),
        )
        DateField(
            value = state.endDate,
            onValueChange = viewModel::onEndDate,
            label = stringResource(R.string.end_date),
            error = state.fieldErrors[Field.END_DATE],
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BalanceField(state: EventFormUiState, viewModel: EventFormViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.event_form_balance_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormTextField(
            value = state.openingBalance,
            onValueChange = { viewModel.onChange(Field.OPENING_BALANCE, it) },
            label = stringResource(R.string.event_opening_balance),
            error = state.fieldErrors[Field.OPENING_BALANCE],
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FormButtons(state: EventFormUiState, onBack: () -> Unit, onNext: () -> Unit, onSave: () -> Unit) {
    val last = state.isEdit || state.step == EventStep.entries.last()
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp),
    ) {
        if (!state.isEdit && state.step != EventStep.DETAILS) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(48.dp)) {
                Text(stringResource(R.string.back))
            }
        }
        Button(
            onClick = if (last) onSave else onNext,
            enabled = !state.saving,
            modifier = Modifier.weight(1f).height(48.dp),
        ) {
            when {
                state.saving -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                state.isEdit -> Text(stringResource(R.string.save))
                last -> Text(stringResource(R.string.event_form_create))
                else -> Text(stringResource(R.string.next))
            }
        }
    }
}

private fun EventStep.titleRes(): Int = when (this) {
    EventStep.DETAILS -> R.string.event_step_details
    EventStep.BALANCE -> R.string.event_step_balance
    EventStep.ROLES -> R.string.event_step_roles
    EventStep.MEMBERS -> R.string.event_step_members
}
