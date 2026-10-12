package com.bharavahi.org.ui.scheduleform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.bharavahi.core.domain.model.Due
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.formatDate
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.BackTopBar
import com.bharavahi.org.ui.common.DateField
import com.bharavahi.org.ui.common.SectionTitle

@Composable
fun ScheduleFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ScheduleFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { currentOnSaved() }
    }

    val title = stringResource(if (state.isEdit) R.string.schedule_form_edit_title else R.string.schedule_form_add_title)
    Scaffold(
        topBar = { BackTopBar(title, onBack) },
        bottomBar = {
            if (!state.loading) {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saving && !state.locked,
                    modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp).height(48.dp),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(if (state.isEdit) R.string.save else R.string.schedule_form_create))
                    }
                }
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
            item {
                ScheduleField(state.name, Field.NAME, R.string.schedule_name, state, viewModel)
                ScheduleField(state.description, Field.DESCRIPTION, R.string.schedule_description, state, viewModel)
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
                ScheduleField(
                    state.amount, Field.AMOUNT, R.string.schedule_amount, state, viewModel,
                    KeyboardType.Decimal, supporting = stringResource(R.string.schedule_amount_help),
                )
                ScheduleField(state.numberOfDues, Field.NUMBER_OF_DUES, R.string.schedule_dues, state, viewModel, KeyboardType.Number, ImeAction.Done)
                state.error?.let {
                    Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error)
                }
            }
            if (state.preview.isNotEmpty()) {
                item {
                    SectionTitle(stringResource(R.string.schedule_preview))
                    Text(
                        stringResource(R.string.schedule_preview_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.preview, key = Due::index) { due -> PreviewRow(due) }
            }
        }
    }
}

@Composable
private fun PreviewRow(due: Due) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(due.label, modifier = Modifier.weight(1f))
        Text(formatDate(due.dueDate), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
        Text(formatRupees(due.amount), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ScheduleField(
    value: String,
    field: Field,
    labelRes: Int,
    state: ScheduleFormUiState,
    viewModel: ScheduleFormViewModel,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    supporting: String? = null,
) {
    FormTextField(
        value = value,
        onValueChange = { viewModel.onChange(field, it) },
        label = stringResource(labelRes),
        error = state.fieldErrors[field],
        keyboardType = keyboardType,
        imeAction = imeAction,
        enabled = !state.locked,
        supportingText = supporting,
        modifier = Modifier.fillMaxWidth(),
    )
}
