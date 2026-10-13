package com.bharavahi.org.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.usecase.payment.PAYMENT_METHODS
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.BackTopBar
import com.bharavahi.org.ui.common.DateField
import com.bharavahi.org.ui.common.PickerField
import com.bharavahi.org.ui.common.labelRes
import com.bharavahi.org.ui.common.shareToWhatsApp
import kotlinx.coroutines.launch

@Composable
fun MarkPaymentScreen(
    onBack: () -> Unit,
    viewModel: MarkPaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shareTitle = stringResource(R.string.share_whatsapp)
    val share: () -> Unit = {
        scope.launch {
            viewModel.whatsAppText().onSuccess { shareToWhatsApp(context, it, shareTitle) }
        }
    }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.mark_payment), onBack) },
        bottomBar = {
            if (!state.loading && state.events.isNotEmpty()) {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp).height(48.dp),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.payment_save))
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingContent(Modifier.padding(padding))
            state.events.isEmpty() -> MessageContent(
                message = stringResource(state.error?.messageRes() ?: R.string.payment_no_events),
                modifier = Modifier.padding(padding),
            )
            else -> Form(state, viewModel, Modifier.padding(padding))
        }
    }

    if (state.saved) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text(stringResource(R.string.payment_saved_title)) },
            text = { Text(stringResource(R.string.payment_saved_body)) },
            confirmButton = { TextButton(onClick = share) { Text(stringResource(R.string.share_whatsapp)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::markAnother) { Text(stringResource(R.string.payment_mark_another)) }
                    TextButton(onClick = onBack) { Text(stringResource(R.string.done)) }
                }
            },
        )
    }
}

@Composable
private fun Form(state: MarkPaymentUiState, viewModel: MarkPaymentViewModel, modifier: Modifier) {
    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PickerField(
            label = stringResource(R.string.payment_event),
            options = state.events,
            selected = state.events.firstOrNull { it.id == state.eventId },
            optionLabel = { it.name },
            onSelect = { viewModel.selectEvent(it.id) },
            error = state.fieldErrors[Field.EVENT],
            modifier = Modifier.fillMaxWidth(),
        )
        PickerField(
            label = stringResource(R.string.payment_member),
            options = state.members,
            selected = state.members.firstOrNull { it.memberId == state.memberId },
            optionLabel = { it.name },
            onSelect = { viewModel.selectMember(it.memberId) },
            error = state.fieldErrors[Field.MEMBER],
            enabled = state.dues != null,
            modifier = Modifier.fillMaxWidth(),
        )
        PickerField(
            label = stringResource(R.string.payment_schedule),
            options = state.schedules,
            selected = state.schedules.firstOrNull { it.id == state.scheduleId },
            optionLabel = { it.name },
            onSelect = { viewModel.selectSchedule(it.id) },
            error = state.fieldErrors[Field.SCHEDULE],
            enabled = state.dues != null,
            modifier = Modifier.fillMaxWidth(),
        )
        PickerField(
            label = stringResource(R.string.payment_due),
            options = state.lines,
            selected = state.selectedLine,
            optionLabel = { line ->
                if (line.owed > 0) {
                    line.due.label + " · " + formatRupees(line.owed)
                } else {
                    line.due.label + " · ✓"
                }
            },
            onSelect = { viewModel.selectDue(it.due.index) },
            error = state.fieldErrors[Field.DUE],
            enabled = state.memberId != null && state.scheduleId != null,
            modifier = Modifier.fillMaxWidth(),
        )
        state.selectedLine?.let { line ->
            Text(
                stringResource(R.string.payment_owed, formatRupees(line.owed), formatRupees(line.due.amount)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FormTextField(
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            label = stringResource(R.string.payment_amount),
            error = state.fieldErrors[Field.AMOUNT],
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PAYMENT_METHODS.forEach { method ->
                FilterChip(
                    selected = state.method == method,
                    onClick = { viewModel.onMethodChange(method) },
                    label = { Text(stringResource(method.labelRes())) },
                )
            }
        }
        DateField(
            value = state.paidOn,
            onValueChange = viewModel::onPaidOnChange,
            label = stringResource(R.string.payment_paid_on),
            modifier = Modifier.fillMaxWidth(),
        )
        FormTextField(
            value = state.note,
            onValueChange = viewModel::onNoteChange,
            label = stringResource(R.string.payment_note),
            imeAction = ImeAction.Done,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error)
        }
    }
}

