package com.bharavahi.org.ui.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bharavahi.core.domain.model.EmailStatus
import com.bharavahi.core.domain.model.Payment
import com.bharavahi.core.ui.components.formatDate
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.theme.LocalStatusColors
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.labelRes

/**
 * ORG-24: one payment with its due, method, sync state and backup email status.
 * [onReverse] is null when the payment can't be reversed; [onResend] only when its email failed.
 */
@Composable
fun PaymentRow(
    payment: Payment,
    title: String,
    dueLabel: String,
    reversed: Boolean,
    onReverse: (() -> Unit)?,
    onResend: (() -> Unit)?,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(dueLabel + " · " + formatDate(payment.paidOn) + " · " + stringResource(payment.method.labelRes()))
                if (payment.note.isNotBlank()) Text(payment.note, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (payment.waitingToSync) {
                        Text(
                            stringResource(R.string.payment_waiting_to_sync),
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalStatusColors.current.partlyPaid,
                        )
                    }
                    Text(
                        stringResource(R.string.payment_email, stringResource(payment.emailStatus.labelRes())),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (payment.emailStatus == EmailStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (reversed) {
                        Text(stringResource(R.string.payment_reversed), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row {
                    if (onResend != null) TextButton(onClick = onResend) { Text(stringResource(R.string.payment_resend)) }
                    if (onReverse != null) TextButton(onClick = onReverse) { Text(stringResource(R.string.payment_reverse)) }
                }
            }
        },
        trailingContent = {
            Text(
                formatRupees(payment.amount),
                style = MaterialTheme.typography.titleMedium,
                color = if (payment.amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}
