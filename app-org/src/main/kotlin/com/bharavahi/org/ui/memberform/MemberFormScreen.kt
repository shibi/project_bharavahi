package com.bharavahi.org.ui.memberform

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.ui.components.ConfirmDialog
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.StatusBadge
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.AccentButton
import com.bharavahi.org.ui.common.BackTopBar
import com.bharavahi.org.ui.common.SectionTitle
import com.bharavahi.org.ui.common.color
import com.bharavahi.org.ui.common.labelRes

@Composable
fun MemberFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: MemberFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)
    var confirmStatus by remember { mutableStateOf<MemberStatus?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { currentOnSaved() }
    }

    val title = stringResource(if (state.isEdit) R.string.member_form_edit_title else R.string.member_form_add_title)
    Scaffold(topBar = { BackTopBar(title, onBack) }) { padding ->
        if (state.loading) {
            LoadingContent(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FormField(state.name, Field.NAME, R.string.member_name, state, viewModel)
            FormField(state.nickname, Field.NICKNAME, R.string.member_nickname, state, viewModel)
            FormField(state.phone, Field.PHONE, R.string.member_phone, state, viewModel, KeyboardType.Phone)
            FormField(state.email, Field.EMAIL, R.string.member_email, state, viewModel, KeyboardType.Email, ImeAction.Done)

            state.error?.let {
                Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error)
            }
            AccentButton(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(if (state.isEdit) R.string.save else R.string.member_form_add))
                }
            }

            if (state.isEdit) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(stringResource(R.string.member_status_title))
                    StatusBadge(stringResource(state.status.labelRes()), state.status.color())
                }
                Text(
                    stringResource(R.string.member_status_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    MemberStatus.entries.filter { it != state.status }.forEach { target ->
                        OutlinedButton(onClick = { confirmStatus = target }) { Text(stringResource(target.actionRes())) }
                    }
                }
            }
        }
    }

    confirmStatus?.let { target ->
        ConfirmDialog(
            title = stringResource(target.actionRes()),
            message = stringResource(target.confirmRes(), state.name),
            confirmLabel = stringResource(target.actionRes()),
            onConfirm = {
                viewModel.setStatus(target)
                confirmStatus = null
            },
            onDismiss = { confirmStatus = null },
        )
    }
}

private fun MemberStatus.actionRes(): Int = when (this) {
    MemberStatus.ACTIVE -> R.string.member_action_enable
    MemberStatus.DISABLED -> R.string.member_action_disable
    MemberStatus.ARCHIVED -> R.string.member_action_archive
}

private fun MemberStatus.confirmRes(): Int = when (this) {
    MemberStatus.ACTIVE -> R.string.member_confirm_enable
    MemberStatus.DISABLED -> R.string.member_confirm_disable
    MemberStatus.ARCHIVED -> R.string.member_confirm_archive
}

@Composable
private fun FormField(
    value: String,
    field: Field,
    labelRes: Int,
    state: MemberFormUiState,
    viewModel: MemberFormViewModel,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    FormTextField(
        value = value,
        onValueChange = { viewModel.onChange(field, it) },
        label = stringResource(labelRes),
        error = state.fieldErrors[field],
        keyboardType = keyboardType,
        imeAction = imeAction,
        modifier = Modifier.fillMaxWidth(),
    )
}
