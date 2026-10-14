package com.bharavahi.org.ui.memberform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.bharavahi.core.domain.model.BloodGroup
import com.bharavahi.core.domain.model.Gender
import com.bharavahi.core.domain.model.MemberStatus
import com.bharavahi.core.domain.model.ShirtSize
import com.bharavahi.core.ui.components.ConfirmDialog
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.LoadingContent
import com.bharavahi.core.ui.components.StatusBadge
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.AccentButton
import com.bharavahi.org.ui.common.BackTopBar
import com.bharavahi.org.ui.common.DateField
import com.bharavahi.org.ui.common.PickerField
import com.bharavahi.org.ui.common.SectionTitle
import com.bharavahi.org.ui.common.color
import com.bharavahi.org.ui.common.labelRes
import java.time.LocalDate

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
            FormField(
                state.email, Field.EMAIL, R.string.member_email, state, viewModel, KeyboardType.Email,
                if (state.extrasExpanded) ImeAction.Next else ImeAction.Done,
            )
            AdditionalDetails(state, viewModel)

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

/** ORG-7 "Additional details": optional fields in a collapsible section. */
@Composable
private fun AdditionalDetails(state: MemberFormUiState, viewModel: MemberFormViewModel) {
    val extras = state.extras
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = viewModel::toggleExtras),
    ) {
        SectionTitle(stringResource(R.string.member_extras_title))
        Text(
            stringResource(R.string.member_extras_optional),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp).weight(1f),
        )
        Icon(
            if (state.extrasExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = stringResource(
                if (state.extrasExpanded) R.string.member_extras_collapse else R.string.member_extras_expand,
            ),
        )
    }
    AnimatedVisibility(state.extrasExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DateField(
                value = extras.dateOfBirth,
                onValueChange = { viewModel.onExtrasChange(extras.copy(dateOfBirth = it)) },
                label = stringResource(R.string.member_dob),
                error = state.fieldErrors[Field.DATE_OF_BIRTH],
                onClear = { viewModel.onExtrasChange(extras.copy(dateOfBirth = null)) },
                maxDate = LocalDate.now(),
                modifier = Modifier.fillMaxWidth(),
            )
            OptionalPicker(R.string.member_gender, Gender.entries, extras.gender, { stringResource(it.labelRes()) }) {
                viewModel.onExtrasChange(extras.copy(gender = it))
            }
            OptionalPicker(R.string.member_blood_group, BloodGroup.entries, extras.bloodGroup, { it.label }) {
                viewModel.onExtrasChange(extras.copy(bloodGroup = it))
            }
            OptionalPicker(R.string.member_shirt_size, ShirtSize.entries, extras.shirtSize, { it.name }) {
                viewModel.onExtrasChange(extras.copy(shirtSize = it))
            }
            FormField(extras.job, Field.JOB, R.string.member_job, state, viewModel)
            FormField(extras.houseName, Field.HOUSE_NAME, R.string.member_house_name, state, viewModel, imeAction = ImeAction.Done)
        }
    }
}

/** A dropdown whose first option, "Not set", clears the value. */
@Composable
private fun <T : Any> OptionalPicker(
    labelRes: Int,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
) {
    val notSet = stringResource(R.string.not_set)
    val labels = options.associateWith { optionLabel(it) }
    PickerField(
        label = stringResource(labelRes),
        options = listOf(null) + options,
        selected = selected,
        optionLabel = { it?.let(labels::getValue) ?: notSet },
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth(),
    )
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
