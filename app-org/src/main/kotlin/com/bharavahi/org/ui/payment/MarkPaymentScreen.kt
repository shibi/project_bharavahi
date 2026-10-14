package com.bharavahi.org.ui.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.model.PaymentMethod
import com.bharavahi.core.domain.usecase.payment.PAYMENT_METHODS
import com.bharavahi.core.domain.validation.Money
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.formatDate
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
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
        topBar = { PaymentTopBar(onBack) },
        bottomBar = {
            if (!state.loading && state.events.isNotEmpty()) {
                val entered = Money.parseRupees(state.amount)
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Button(
                        onClick = viewModel::save,
                        enabled = !state.saving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp).height(56.dp),
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(
                                if (entered != null && entered > 0) {
                                    stringResource(R.string.payment_save_amount, formatRupees(entered))
                                } else {
                                    stringResource(R.string.payment_save)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
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
        SavedDialog(onShare = share, onMarkAnother = viewModel::markAnother, onDone = onBack)
    }
}

@Composable
private fun PaymentTopBar(onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_back),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column {
                    Text(
                        stringResource(R.string.payment_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.payment_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun OfflineBanner() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
        Text(
            stringResource(R.string.payment_offline_banner),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Default.CloudSync,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp),
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OfflineBanner()
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
        DuesGroup(state, viewModel)
        AmountField(state, viewModel)
        MethodChips(state.method, viewModel::onMethodChange)
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

/** Schedule and due pickers grouped in a tinted panel, with the "owed of" helper under them. */
@Composable
private fun DuesGroup(state: MarkPaymentUiState, viewModel: MarkPaymentViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    stringResource(R.string.payment_owed, formatRupees(line.owed), formatRupees(line.due.amount)) +
                        " (" + stringResource(R.string.due_date, formatDate(line.due.dueDate)) + ")",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

/** Large rupee amount entry with an "Exact" marker when it equals what is owed. */
@Composable
private fun AmountField(state: MarkPaymentUiState, viewModel: MarkPaymentViewModel) {
    val owed = state.selectedLine?.owed
    val entered = Money.parseRupees(state.amount)
    val error = state.fieldErrors[Field.AMOUNT]
    val primary = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.payment_amount_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            singleLine = true,
            isError = error != null,
            prefix = { Text("₹", style = MaterialTheme.typography.headlineSmall, color = primary, fontWeight = FontWeight.Bold) },
            trailingIcon = {
                if (owed != null && owed > 0 && entered == owed) {
                    Text(
                        stringResource(R.string.payment_exact),
                        style = MaterialTheme.typography.labelSmall,
                        color = primary,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            },
            textStyle = MaterialTheme.typography.headlineSmall.copy(color = primary, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = primary,
                unfocusedIndicatorColor = primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Text(stringResource(error.messageRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        } else {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.payment_part_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (owed != null && owed > 0) {
                    Text(
                        stringResource(R.string.payment_max, formatRupees(owed)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun MethodChips(selected: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.payment_method_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PAYMENT_METHODS.forEach { method ->
                val icon = if (method == PaymentMethod.OFFLINE) Icons.Default.Payments else Icons.Default.QrCode2
                val label = @Composable {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(stringResource(method.labelRes()), modifier = Modifier.padding(start = 8.dp))
                }
                val chip = Modifier.weight(1f).height(48.dp)
                if (selected == method) {
                    Button(onClick = { onSelect(method) }, modifier = chip) { label() }
                } else {
                    OutlinedButton(
                        onClick = { onSelect(method) },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = chip,
                    ) { label() }
                }
            }
        }
    }
}

@Composable
private fun SavedDialog(onShare: () -> Unit, onMarkAnother: () -> Unit, onDone: () -> Unit) {
    Dialog(onDismissRequest = onDone) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        stringResource(R.string.payment_saved_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    stringResource(R.string.payment_saved_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.share_whatsapp), modifier = Modifier.padding(start = 8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onMarkAnother, modifier = Modifier.weight(1f).height(40.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.payment_mark_another), modifier = Modifier.padding(start = 4.dp))
                    }
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.weight(1f).height(40.dp),
                    ) { Text(stringResource(R.string.done)) }
                }
            }
        }
    }
}
