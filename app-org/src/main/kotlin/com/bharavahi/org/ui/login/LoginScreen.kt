package com.bharavahi.org.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.AccentButton

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Design (Stitch): centered brand emblem and title, with the form in a bordered white card below.
    // Stitch tints the page light blue so the white card stands out.
    val pageColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        .compositeOver(MaterialTheme.colorScheme.background)
    Surface(Modifier.fillMaxSize(), color = pageColor) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    stringResource(R.string.login_title).take(1),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text(
                stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.error?.let {
                    // A refused login here means a missing or inactive admin link, or a suspended community.
                    val message = if (it is DomainException.NotAuthorized) R.string.login_not_allowed else it.messageRes()
                    Text(
                        stringResource(message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
                FormTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = stringResource(R.string.login_email),
                    error = state.fieldErrors[Field.EMAIL],
                    keyboardType = KeyboardType.Email,
                    leadingIcon = Icons.Filled.Email,
                    labelAbove = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FormTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = stringResource(R.string.login_password),
                    error = state.fieldErrors[Field.PASSWORD],
                    keyboardType = KeyboardType.Password,
                    leadingIcon = Icons.Filled.Lock,
                    labelAbove = true,
                    imeAction = ImeAction.Done,
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AccentButton(
                    onClick = viewModel::submit,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.login_button))
                    }
                }
            }
        }
    }
}
