package com.bharavahi.org.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.DueStatus
import com.bharavahi.core.domain.model.EmailStatus
import com.bharavahi.core.domain.model.EventStatus
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.model.PaymentMethod
import com.bharavahi.core.ui.components.formatDate
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.core.ui.theme.LocalStatusColors
import com.bharavahi.org.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
            }
        },
        actions = { actions() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabTopBar(title: String, actions: @Composable () -> Unit = {}) {
    TopAppBar(title = { Text(title) }, actions = { actions() })
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 12.dp, bottom = 4.dp))
}

/** Round avatar with the member's initials (design: member and payment rows). */
@Composable
fun InitialsAvatar(name: String, modifier: Modifier = Modifier) {
    val initials = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        .take(2).joinToString("") { it.first().uppercase() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Text(initials, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@StringRes
fun MemberStatus.labelRes(): Int = when (this) {
    MemberStatus.ACTIVE -> R.string.member_status_active
    MemberStatus.DISABLED -> R.string.member_status_disabled
    MemberStatus.ARCHIVED -> R.string.member_status_archived
}

@Composable
fun MemberStatus.color(): Color = when (this) {
    MemberStatus.ACTIVE -> LocalStatusColors.current.paid
    MemberStatus.DISABLED -> LocalStatusColors.current.partlyPaid
    MemberStatus.ARCHIVED -> MaterialTheme.colorScheme.outline
}

@StringRes
fun EventStatus.labelRes(): Int = when (this) {
    EventStatus.ACTIVE -> R.string.event_status_active
    EventStatus.SUSPENDED -> R.string.event_status_suspended
    EventStatus.ARCHIVED -> R.string.event_status_archived
    EventStatus.CLOSED -> R.string.event_status_closed
}

@Composable
fun EventStatus.color(): Color = when (this) {
    EventStatus.ACTIVE -> LocalStatusColors.current.paid
    EventStatus.SUSPENDED -> LocalStatusColors.current.partlyPaid
    EventStatus.ARCHIVED, EventStatus.CLOSED -> MaterialTheme.colorScheme.outline
}

@StringRes
fun DueStatus.labelRes(): Int = when (this) {
    DueStatus.PAID -> R.string.due_status_paid
    DueStatus.PARTLY_PAID -> R.string.due_status_partly_paid
    DueStatus.PENDING -> R.string.due_status_pending
}

@Composable
fun DueStatus.color(): Color = when (this) {
    DueStatus.PAID -> LocalStatusColors.current.paid
    DueStatus.PARTLY_PAID -> LocalStatusColors.current.partlyPaid
    DueStatus.PENDING -> LocalStatusColors.current.pending
}

/** A read-only field that opens a date picker. Dates are whole days, so the picker works in UTC. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: ValidationError? = null,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = formatDate(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            isError = error != null,
            supportingText = if (error != null) {
                { Text(stringResource(error.messageRes())) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: LocalDate.now()).toEpochDay() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onValueChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    open = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

@StringRes
fun PaymentMethod.labelRes(): Int = when (this) {
    PaymentMethod.ONLINE -> R.string.payment_method_online
    PaymentMethod.OFFLINE -> R.string.payment_method_offline
}

@StringRes
fun EmailStatus.labelRes(): Int = when (this) {
    EmailStatus.PENDING -> R.string.email_status_pending
    EmailStatus.SENT -> R.string.email_status_sent
    EmailStatus.FAILED -> R.string.email_status_failed
}
