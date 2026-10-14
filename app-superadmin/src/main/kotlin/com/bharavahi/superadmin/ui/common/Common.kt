package com.bharavahi.superadmin.ui.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.bharavahi.core.domain.model.ActivityAction
import com.bharavahi.core.domain.model.OrganizationStatus
import com.bharavahi.core.ui.theme.LocalStatusColors
import com.bharavahi.superadmin.R
import java.text.DateFormat
import java.util.Date

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

@StringRes
fun OrganizationStatus.labelRes(): Int = when (this) {
    OrganizationStatus.ACTIVE -> R.string.status_active
    OrganizationStatus.SUSPENDED -> R.string.status_suspended
    OrganizationStatus.DELETED -> R.string.status_deleted
}

@Composable
fun OrganizationStatus.color(): Color = when (this) {
    OrganizationStatus.ACTIVE -> LocalStatusColors.current.paid
    OrganizationStatus.SUSPENDED -> LocalStatusColors.current.partlyPaid
    OrganizationStatus.DELETED -> MaterialTheme.colorScheme.error
}

@StringRes
fun ActivityAction.labelRes(): Int = when (this) {
    ActivityAction.ORG_CREATED -> R.string.activity_org_created
    ActivityAction.ORG_UPDATED -> R.string.activity_org_updated
    ActivityAction.ORG_SUSPENDED -> R.string.activity_org_suspended
    ActivityAction.ORG_REACTIVATED -> R.string.activity_org_reactivated
    ActivityAction.ORG_DELETED -> R.string.activity_org_deleted
    ActivityAction.ADMIN_ACTIVATED -> R.string.activity_admin_activated
    ActivityAction.ADMIN_DEACTIVATED -> R.string.activity_admin_deactivated
    ActivityAction.ADMIN_ADDED -> R.string.activity_admin_added
    ActivityAction.UNKNOWN -> R.string.activity_unknown
}

@Composable
fun formatDateTime(millis: Long?): String =
    if (millis == null) {
        stringResource(R.string.activity_pending_time)
    } else {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
    }
