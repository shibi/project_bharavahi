package com.bharavahi.org.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import com.bharavahi.core.domain.model.Expense
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.MessageContent
import com.bharavahi.core.ui.components.formatDate
import com.bharavahi.core.ui.components.formatRupees
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.core.ui.theme.LocalStatusColors
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.BackTopBar
import com.bharavahi.org.ui.common.DateField
import com.bharavahi.org.ui.common.PickerField

@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    onAdd: (eventId: String) -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.expenses_title), onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAdd(viewModel.eventId) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.expense_add)) },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingContent(Modifier.padding(padding))
            state.error != null -> MessageContent(stringResource(state.error!!.messageRes()), Modifier.padding(padding), viewModel::retry)
            state.expenses.isEmpty() -> MessageContent(stringResource(R.string.expenses_empty), Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
                item {
                    Text(
                        stringResource(R.string.expenses_total, formatRupees(state.total)),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                items(state.expenses, key = Expense::id) { expense ->
                    val paidBy = state.members.firstOrNull { it.memberId == expense.paidByMemberId }?.name
                    ListItem(
                        headlineContent = { Text(expense.title) },
                        supportingContent = {
                            Column {
                                Text(formatDate(expense.date) + if (paidBy != null) " · " + stringResource(R.string.expense_paid_by, paidBy) else "")
                                if (expense.description.isNotBlank()) Text(expense.description, style = MaterialTheme.typography.bodySmall)
                                if (expense.waitingToSync) {
                                    Text(
                                        stringResource(R.string.payment_waiting_to_sync),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LocalStatusColors.current.partlyPaid,
                                    )
                                }
                            }
                        },
                        trailingContent = { Text(formatRupees(expense.amount), style = MaterialTheme.typography.titleMedium) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)
    LaunchedEffect(viewModel) { viewModel.saved.collect { currentOnSaved() } }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.expense_add), onBack) },
        bottomBar = {
            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp).height(48.dp),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FormTextField(
                value = state.title,
                onValueChange = { viewModel.onChange(Field.TITLE, it) },
                label = stringResource(R.string.expense_title),
                error = state.fieldErrors[Field.TITLE],
                modifier = Modifier.fillMaxWidth(),
            )
            FormTextField(
                value = state.amount,
                onValueChange = { viewModel.onChange(Field.AMOUNT, it) },
                label = stringResource(R.string.expense_amount),
                error = state.fieldErrors[Field.AMOUNT],
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.fillMaxWidth(),
            )
            DateField(
                value = state.date,
                onValueChange = viewModel::onDate,
                label = stringResource(R.string.expense_date),
                error = state.fieldErrors[Field.DATE],
                modifier = Modifier.fillMaxWidth(),
            )
            val nobody = stringResource(R.string.expense_paid_by_none)
            PickerField(
                label = stringResource(R.string.expense_paid_by_label),
                options = listOf<String?>(null) + state.members.map { it.memberId },
                selected = state.paidByMemberId,
                optionLabel = { id -> state.members.firstOrNull { it.memberId == id }?.name ?: nobody },
                onSelect = viewModel::onPaidBy,
                modifier = Modifier.fillMaxWidth(),
            )
            FormTextField(
                value = state.description,
                onValueChange = { viewModel.onChange(Field.DESCRIPTION, it) },
                label = stringResource(R.string.expense_description),
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        }
    }
}
