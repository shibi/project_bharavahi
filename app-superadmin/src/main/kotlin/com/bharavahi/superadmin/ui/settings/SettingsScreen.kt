package com.bharavahi.superadmin.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.model.ThemeMode
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.superadmin.R
import com.bharavahi.superadmin.ui.common.BackTopBar

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val mail by viewModel.mail.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar(stringResource(R.string.settings_title), onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.settings_mail_section), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_mail_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FormTextField(
                value = mail.senderEmail,
                onValueChange = viewModel::onSenderEmailChange,
                label = stringResource(R.string.settings_sender_email),
                error = mail.fieldErrors[Field.SENDER_EMAIL],
                keyboardType = KeyboardType.Email,
                enabled = !mail.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            FormTextField(
                value = mail.appPassword,
                onValueChange = viewModel::onAppPasswordChange,
                label = stringResource(R.string.settings_app_password),
                error = mail.fieldErrors[Field.APP_PASSWORD],
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                isPassword = true,
                enabled = !mail.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            mail.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
            if (mail.saved) Text(stringResource(R.string.settings_saved), color = MaterialTheme.colorScheme.primary)
            Button(
                onClick = viewModel::saveMail,
                enabled = !mail.loading && !mail.saving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text(stringResource(R.string.settings_save_mail)) }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text(stringResource(R.string.settings_theme_section), style = MaterialTheme.typography.titleMedium)
            Column(Modifier.selectableGroup()) {
                ThemeOption(ThemeMode.SYSTEM, R.string.settings_theme_system, themeMode, viewModel::onThemeModeChange)
                ThemeOption(ThemeMode.LIGHT, R.string.settings_theme_light, themeMode, viewModel::onThemeModeChange)
                ThemeOption(ThemeMode.DARK, R.string.settings_theme_dark, themeMode, viewModel::onThemeModeChange)
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(stringResource(R.string.settings_sign_out))
            }
        }
    }
}

@Composable
private fun ThemeOption(mode: ThemeMode, labelRes: Int, selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .selectable(selected = mode == selected, onClick = { onSelect(mode) }, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = mode == selected, onClick = null)
        Text(stringResource(labelRes), modifier = Modifier.padding(start = 12.dp))
    }
}
