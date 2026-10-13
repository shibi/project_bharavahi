package com.bharavahi.org.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.EmailStatus
import com.bharavahi.core.domain.model.Payment
import com.bharavahi.core.ui.components.ConfirmDialog
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.BackTopBar
import com.bharavahi.org.ui.common.PickerField

@Composable
fun PaymentsScreen(
    onBack: () -> Unit,
    viewModel: PaymentsViewModel = hiltViewModel(),
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

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.payments_title), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val allMembers = stringResource(R.string.payments_all_members)
            val allSchedules = stringResource(R.string.payments_all_schedules)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                PickerField(
                    label = stringResource(R.string.payment_member),
                    options = listOf<String?>(null) + state.members.map { it.memberId },
                    selected = state.memberFilter,
                    optionLabel = { id -> state.members.firstOrNull { it.memberId == id }?.name ?: allMembers },
                    onSelect = viewModel::onMemberFilter,
                    modifier = Modifier.weight(1f),
                )
                PickerField(
                    label = stringResource(R.string.payment_schedule),
                    options = listOf<String?>(null) + state.schedules.map { it.id },
                    selected = state.scheduleFilter,
                    optionLabel = { id -> state.schedules.firstOrNull { it.id == id }?.name ?: allSchedules },
                    onSelect = viewModel::onScheduleFilter,
                    modifier = Modifier.weight(1f),
                )
            }
            when {
                state.loading -> LoadingContent()
                state.error != null -> MessageContent(stringResource(state.error!!.messageRes()), onRetry = viewModel::retry)
                state.totalCount == 0 -> MessageContent(stringResource(R.string.payments_empty))
                state.visible.isEmpty() -> MessageContent(stringResource(R.string.payments_no_match))
                else -> LazyColumn {
                    items(state.visible, key = Payment::id) { payment ->
                        val schedule = state.schedules.firstOrNull { it.id == payment.scheduleId }
                        val reversed = payment.id in state.reversedIds
                        val memberName = state.members.firstOrNull { it.memberId == payment.memberId }?.name.orEmpty()
                        PaymentRow(
                            payment = payment,
                            title = memberName,
                            dueLabel = schedule?.dues?.firstOrNull { it.index == payment.dueIndex }?.label ?: schedule?.name.orEmpty(),
                            reversed = reversed,
                            onReverse = if (payment.isReversal || reversed) null else ({ confirmReverse = payment }),
                            onResend = if (payment.emailStatus == EmailStatus.FAILED) ({ viewModel.resend(payment.id) }) else null,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            avatarName = memberName,
                        )
                    }
                }
            }
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
