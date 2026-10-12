package com.bharavahi.superadmin.ui.communityform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.superadmin.R
import com.bharavahi.superadmin.ui.common.BackTopBar

@Composable
fun CommunityFormScreen(
    onBack: () -> Unit,
    onSaved: (orgId: String) -> Unit,
    viewModel: CommunityFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSaved by rememberUpdatedState(onSaved)

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { currentOnSaved(it) }
    }

    val title = stringResource(if (state.isEdit) R.string.form_edit_title else R.string.form_add_title)
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
            SectionTitle(stringResource(R.string.form_section_details))
            FormField(state.name, Field.NAME, R.string.form_name, state, viewModel)
            FormField(state.type, Field.TYPE, R.string.form_type, state, viewModel)
            FormField(state.address, Field.ADDRESS, R.string.form_address, state, viewModel)
            FormField(state.contactPhone, Field.PHONE, R.string.form_phone, state, viewModel, KeyboardType.Phone)

            if (!state.isEdit) {
                SectionTitle(stringResource(R.string.form_section_admin))
                Text(
                    stringResource(R.string.form_admin_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FormField(state.adminName, Field.ADMIN_NAME, R.string.form_admin_name, state, viewModel)
                FormField(state.adminEmail, Field.ADMIN_EMAIL, R.string.form_admin_email, state, viewModel, KeyboardType.Email)
                FormField(state.adminPhone, Field.ADMIN_PHONE, R.string.form_admin_phone, state, viewModel, KeyboardType.Phone)
                FormField(
                    state.adminPassword, Field.PASSWORD, R.string.form_admin_password, state, viewModel,
                    KeyboardType.Password, ImeAction.Done,
                )
            }

            state.error?.let {
                Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(48.dp),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(if (state.isEdit) R.string.form_save else R.string.form_create))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
private fun FormField(
    value: String,
    field: Field,
    labelRes: Int,
    state: CommunityFormUiState,
    viewModel: CommunityFormViewModel,
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
        isPassword = field == Field.PASSWORD,
        modifier = Modifier.fillMaxWidth(),
    )
}
