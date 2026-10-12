package com.bharavahi.org.ui.eventform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bharavahi.core.domain.error.ValidationError
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.ui.components.messageRes
import com.bharavahi.org.R

/** ORG-11 step 3 and ORG-12: create, rename and remove role names. */
@Composable
fun RolesEditor(
    roles: List<EventRole>,
    newRoleName: String,
    onNewRoleNameChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRename: (id: String, name: String) -> Unit,
    onRemove: (id: String) -> Unit,
    error: ValidationError?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        roles.forEach { role ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = role.name,
                    onValueChange = { onRename(role.id, it) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(role.id) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.roles_remove, role.name))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newRoleName,
                onValueChange = onNewRoleNameChange,
                label = { Text(stringResource(R.string.roles_new)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAdd, enabled = newRoleName.isNotBlank()) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.roles_add))
            }
        }
        if (error != null) {
            Text(stringResource(error.messageRes()), color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
        }
    }
}
