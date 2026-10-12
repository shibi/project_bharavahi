package com.bharavahi.org.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.model.ThemeMode
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.TabTopBar

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Scaffold(topBar = { TabTopBar(stringResource(R.string.tab_profile)) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.profile_signed_in_as), style = MaterialTheme.typography.labelLarge)
            Text(email, style = MaterialTheme.typography.bodyLarge)

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text(stringResource(R.string.profile_theme), style = MaterialTheme.typography.titleMedium)
            Column(Modifier.selectableGroup()) {
                ThemeOption(ThemeMode.SYSTEM, R.string.theme_system, themeMode, viewModel::onThemeModeChange)
                ThemeOption(ThemeMode.LIGHT, R.string.theme_light, themeMode, viewModel::onThemeModeChange)
                ThemeOption(ThemeMode.DARK, R.string.theme_dark, themeMode, viewModel::onThemeModeChange)
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(stringResource(R.string.profile_sign_out))
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
