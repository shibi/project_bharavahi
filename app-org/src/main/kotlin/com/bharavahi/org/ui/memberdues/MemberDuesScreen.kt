package com.bharavahi.org.ui.memberdues

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.MemberDueLine
import com.bharavahi.core.domain.model.Payment
import com.bharavahi.core.domain.usecase.payment.MemberDuesDetail
import com.bharavahi.core.domain.usecase.payment.ObservePaymentsUseCase
import com.bharavahi.core.ui.components.ConfirmDialog
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.StatusBadge
import com.bharavahi.core.ui.components.formatDate
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.BackTopBar
import com.bharavahi.org.ui.common.HeroStatTile
import com.bharavahi.org.ui.common.InitialsAvatar
import com.bharavahi.org.ui.common.SectionTitle
import com.bharavahi.org.ui.common.color
import com.bharavahi.org.ui.common.labelRes
import com.bharavahi.org.ui.payments.PaymentRow

@Composable
fun MemberDuesScreen(
    onBack: () -> Unit,
    onMarkPayment: (eventId: String, memberId: String) -> Unit,
    viewModel: MemberDuesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmReverse by remember { mutableStateOf<Payment?>(null) }

    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            snackbar.showSnackbar(context.getString(it.messageRes()))
            viewModel.dismissError()
        }
    }

    val detail = state.detail
    Scaffold(
        topBar = { BackTopBar(detail?.dues?.member?.name.orEmpty(), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (detail != null && detail.dues.member.active) {
                ExtendedFloatingActionButton(
                    onClick = { onMarkPayment(viewModel.eventId, viewModel.memberId) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.mark_payment)) },
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingContent(Modifier.padding(padding))
            detail == null -> MessageContent(
                stringResource(state.error?.messageRes() ?: R.string.event_not_found),
                Modifier.padding(padding),
                onRetry = viewModel::retry,
            )
            else -> Content(detail, Modifier.padding(padding), onReverse = { confirmReverse = it })
        }
    }

    confirmReverse?.let { payment ->
        ConfirmDialog(
            title = stringResource(R.string.payment_reverse),
            message = stringResource(R.string.payment_reverse_confirm, formatRupees(payment.amount)),
            confirmLabel = stringResource(R.string.payment_reverse),
            onConfirm = {
                viewModel.reverse(payment.id)
                confirmReverse = null
            },
            onDismiss = { confirmReverse = null },
        )
    }
}

@Composable
private fun Content(detail: MemberDuesDetail, modifier: Modifier, onReverse: (Payment) -> Unit) {
    val reversed = ObservePaymentsUseCase.reversedIds(detail.payments)
    val dueLabels = detail.lines.associate { (it.schedule.id to it.due.index) to it.due.label }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Summary(detail) }
        item { SectionTitle(stringResource(R.string.member_dues_breakdown)) }
        if (detail.lines.isEmpty()) {
            item { Text(stringResource(R.string.event_no_schedules)) }
        }
        items(detail.lines, key = { "l-${it.schedule.id}-${it.due.index}" }) { line -> DueLine(line, showSchedule = detail.lines.map { it.schedule.id }.toSet().size > 1) }
        item { SectionTitle(stringResource(R.string.member_dues_history)) }
        if (detail.payments.isEmpty()) {
            item { Text(stringResource(R.string.payments_empty)) }
        }
        items(detail.payments, key = { "p-${it.id}" }) { payment ->
            PaymentRow(
                payment = payment,
                title = formatDate(payment.paidOn),
                dueLabel = dueLabels[payment.scheduleId to payment.dueIndex].orEmpty(),
                reversed = payment.id in reversed,
                onReverse = if (payment.isReversal || payment.id in reversed) null else ({ onReverse(payment) }),
                onResend = null,
            )
        }
    }
}

/** Teal hero card (design): who, where they stand and the pending amount, with figures as tiles. */
@Composable
private fun Summary(detail: MemberDuesDetail) {
    val d = detail.dues
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InitialsAvatar(d.member.name)
                Column(Modifier.weight(1f)) {
                    Text(d.member.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        listOfNotNull(detail.event.roleName(d.member.roleId), detail.event.name).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                StatusBadge(stringResource(d.currentStatus.labelRes()), d.currentStatus.color())
            }
            Text(stringResource(R.string.member_dues_pending_label), style = MaterialTheme.typography.labelMedium)
            Text(formatRupees(d.pending), style = MaterialTheme.typography.headlineMedium)
            if (d.overdueCount > 0) {
                Text(stringResource(R.string.dues_row_overdue, d.overdueCount), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroStatTile(stringResource(R.string.member_dues_paid_label), formatRupees(d.totalPaid), Modifier.weight(1f))
                HeroStatTile(stringResource(R.string.member_dues_due_so_far_label), formatRupees(d.dueSoFar), Modifier.weight(1f))
                HeroStatTile(stringResource(R.string.member_dues_total_label), formatRupees(d.eventTotal), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DueLine(line: MemberDueLine, showSchedule: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text((if (showSchedule) line.schedule.name + " · " else "") + line.due.label)
            Text(
                stringResource(R.string.member_dues_line, formatRupees(line.paid), formatRupees(line.due.amount)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (line.overpaid) {
                Text(stringResource(R.string.member_dues_overpaid), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        StatusBadge(stringResource(line.status.labelRes()), line.status.color())
    }
}
