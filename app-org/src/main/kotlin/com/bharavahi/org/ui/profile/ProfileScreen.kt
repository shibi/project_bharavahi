package com.bharavahi.org.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.domain.model.OrgAdmin
import com.bharavahi.core.domain.model.ThemeMode
import com.bharavahi.core.ui.components.ConfirmDialog
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.BuildConfig
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.InitialsAvatar
import com.bharavahi.org.ui.common.TabTopBar

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val addForm by viewModel.addForm.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmDeactivate by remember { mutableStateOf<OrgAdmin?>(null) }

    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            snackbar.showSnackbar(context.getString(it.messageRes()))
            viewModel.dismissError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TabTopBar(stringResource(R.string.tab_profile)) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IdentityCard(state.email, state.me)
            CoAdminsSection(
                state = state,
                onActivate = { viewModel.setActive(it, true) },
                onDeactivate = { confirmDeactivate = it },
                onAdd = viewModel::openAddForm,
            )
            PreferencesCard(themeMode, viewModel::onThemeModeChange)
            OutlinedButton(
                onClick = viewModel::signOut,
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.profile_sign_out),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                stringResource(R.string.profile_version, stringResource(R.string.login_title), BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }

    confirmDeactivate?.let { admin ->
        ConfirmDialog(
            title = stringResource(R.string.coadmins_deactivate_title),
            message = stringResource(R.string.coadmins_deactivate_confirm, admin.name.ifBlank { admin.email }),
            confirmLabel = stringResource(R.string.coadmins_deactivate),
            onConfirm = {
                viewModel.setActive(admin, false)
                confirmDeactivate = null
            },
            onDismiss = { confirmDeactivate = null },
        )
    }

    addForm?.let { form ->
        ConfirmDialog(
            title = stringResource(R.string.coadmins_add),
            message = "",
            confirmLabel = stringResource(R.string.coadmins_create),
            confirmEnabled = !form.saving,
            onConfirm = viewModel::submitAddForm,
            onDismiss = viewModel::closeAddForm,
            body = { AddCoAdminFields(form, viewModel) },
        )
    }
}

/** Design: white 20dp card with a teal top strip, large avatar, name, admin badge and contact rows. */
@Composable
private fun IdentityCard(email: String, me: OrgAdmin?) {
    val name = me?.name?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')
    ProfileCard {
        Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.primary))
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                InitialsAvatar(
                    name,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    size = 56.dp,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            stringResource(R.string.profile_admin_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 16.dp, bottom = 14.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ContactRow(Icons.Default.Mail, stringResource(R.string.profile_signed_in_as), email)
                me?.phone?.takeIf { it.isNotBlank() }?.let {
                    ContactRow(Icons.Default.Call, stringResource(R.string.profile_phone), it)
                }
            }
        }
    }
}

@Composable
private fun ContactRow(icon: ImageVector, description: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Design: "Co-admins (2 of 3 active admins)" with open slots on the right, one card per other admin
 * with a deactivate (or activate) action, and a peach "Add co-admin" button while a slot is free (ORG-4).
 */
@Composable
private fun CoAdminsSection(
    state: ProfileUiState,
    onActivate: (OrgAdmin) -> Unit,
    onDeactivate: (OrgAdmin) -> Unit,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.coadmins_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.coadmins_active_count, state.activeCount, state.maxAdmins),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (state.openSlots > 0) {
                Text(
                    pluralStringResource(R.plurals.coadmins_slots_open, state.openSlots, state.openSlots),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        if (state.coAdmins.isEmpty()) {
            Text(
                stringResource(R.string.coadmins_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        state.coAdmins.forEach { admin ->
            CoAdminCard(
                admin = admin,
                canActivate = state.openSlots > 0,
                onActivate = { onActivate(admin) },
                onDeactivate = { onDeactivate(admin) },
            )
        }
        if (state.openSlots > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button, onClick = onAdd)
                    .padding(14.dp),
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    stringResource(R.string.coadmins_add),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    stringResource(R.string.coadmins_max, state.maxAdmins),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun CoAdminCard(admin: OrgAdmin, canActivate: Boolean, onActivate: () -> Unit, onDeactivate: () -> Unit) {
    val name = admin.name.ifBlank { admin.email.substringBefore('@') }
    ProfileCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        ) {
            InitialsAvatar(
                name,
                containerColor = if (admin.active) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (admin.active) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.outline,
                size = 44.dp,
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (admin.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!admin.active) {
                        Text(
                            stringResource(R.string.coadmins_inactive),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(
                    listOf(admin.phone, admin.email).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (admin.active) {
                TextButton(onClick = onDeactivate) {
                    Text(
                        stringResource(R.string.coadmins_deactivate),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                TextButton(onClick = onActivate, enabled = canActivate) {
                    Text(
                        stringResource(R.string.coadmins_activate),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCoAdminFields(form: AddCoAdminForm, viewModel: ProfileViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.coadmins_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormTextField(
            value = form.name,
            onValueChange = viewModel::onNameChange,
            label = stringResource(R.string.coadmins_name),
            error = form.fieldErrors[Field.ADMIN_NAME],
            enabled = !form.saving,
            modifier = Modifier.fillMaxWidth(),
        )
        FormTextField(
            value = form.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.coadmins_email),
            error = form.fieldErrors[Field.ADMIN_EMAIL],
            keyboardType = KeyboardType.Email,
            enabled = !form.saving,
            modifier = Modifier.fillMaxWidth(),
        )
        FormTextField(
            value = form.phone,
            onValueChange = viewModel::onPhoneChange,
            label = stringResource(R.string.coadmins_phone),
            error = form.fieldErrors[Field.ADMIN_PHONE],
            keyboardType = KeyboardType.Phone,
            enabled = !form.saving,
            modifier = Modifier.fillMaxWidth(),
        )
        FormTextField(
            value = form.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.coadmins_password),
            error = form.fieldErrors[Field.PASSWORD],
            supportingText = stringResource(R.string.coadmins_password_help),
            isPassword = true,
            imeAction = ImeAction.Done,
            enabled = !form.saving,
            modifier = Modifier.fillMaxWidth(),
        )
        form.error?.let {
            Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Design: "App Preferences" card with the theme as a three-way segmented control. */
@Composable
private fun PreferencesCard(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    ProfileCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.profile_preferences),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.profile_appearance),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                        .selectableGroup(),
                ) {
                    ThemeSegment(ThemeMode.SYSTEM, R.string.theme_system, selected, onSelect, Modifier.weight(1f))
                    ThemeSegment(ThemeMode.LIGHT, R.string.theme_light, selected, onSelect, Modifier.weight(1f))
                    ThemeSegment(ThemeMode.DARK, R.string.theme_dark, selected, onSelect, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeSegment(
    mode: ThemeMode,
    labelRes: Int,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = mode == selected
    val shape = RoundedCornerShape(8.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(MaterialTheme.colorScheme.surface, shape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                } else {
                    Modifier
                },
            )
            .selectable(selected = isSelected, onClick = { onSelect(mode) }, role = Role.RadioButton)
            .padding(vertical = 10.dp),
    ) {
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            maxLines = 1,
        )
    }
}

/** White 20dp card with a hairline border, the design's standard container. */
@Composable
private fun ProfileCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}
