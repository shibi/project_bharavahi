package com.bharavahi.org.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bharavahi.core.domain.error.DomainException
import com.bharavahi.core.domain.error.Field
import com.bharavahi.core.ui.components.FormTextField
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.BuildConfig
import com.bharavahi.org.R
import com.bharavahi.org.ui.common.AccentButton

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Design: a teal gradient band carries the brand; the form floats over its lower edge in an
    // elevated white card with roomy padding, on a lightly tinted page.
    val primary = MaterialTheme.colorScheme.primary
    val pageColor = primary.copy(alpha = 0.06f).compositeOver(MaterialTheme.colorScheme.background)
    Surface(Modifier.fillMaxSize(), color = pageColor) {
        Column(
            modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The band scrolls with the content, so the card always overlaps its lower edge.
            Box(Modifier.fillMaxWidth()) {
                HeaderBand()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Brand()
                    Spacer(Modifier.height(36.dp))
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    stringResource(R.string.login_welcome),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = primary,
                                )
                                Text(
                                    stringResource(R.string.login_welcome_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
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
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(54.dp),
                            ) {
                                if (state.loading) {
                                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        stringResource(R.string.login_button),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.profile_version, stringResource(R.string.login_title), BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.navigationBarsPadding().padding(top = 28.dp, bottom = 32.dp),
            )
        }
    }
}

/** Teal gradient across the top of the page with soft decorative circles, rounded at the bottom. */
@Composable
private fun HeaderBand() {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Box(
        Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.85f).compositeOver(Color.Black)))),
    ) {
        Box(
            Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-60).dp)
                .background(onPrimary.copy(alpha = 0.07f), CircleShape),
        )
        Box(
            Modifier
                .size(140.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 30.dp)
                .background(onPrimary.copy(alpha = 0.05f), CircleShape),
        )
    }
}

/** Emblem, app name and tagline, light on the teal band. */
@Composable
private fun Brand() {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.secondary,
            shadowElevation = 8.dp,
            modifier = Modifier.size(76.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.login_title).take(1),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary,
                )
            }
        }
        Text(
            stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = onPrimary,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = onPrimary.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
