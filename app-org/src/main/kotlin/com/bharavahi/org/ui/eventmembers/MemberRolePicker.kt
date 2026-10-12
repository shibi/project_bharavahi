package com.bharavahi.org.ui.eventmembers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.bharavahi.core.domain.model.EventRole
import com.bharavahi.core.domain.model.Member
import com.bharavahi.org.R

/**
 * ORG-11 step 4: tick members to add and give each an optional role.
 * [selected] maps a ticked memberId to its roleId (null = no role).
 */
fun LazyListScope.memberRoleItems(
    members: List<Member>,
    selected: Map<String, String?>,
    roles: List<EventRole>,
    onToggle: (Member) -> Unit,
    onRoleChange: (memberId: String, roleId: String?) -> Unit,
) {
    items(members, key = { it.id }) { member ->
        val checked = member.id in selected
        ListItem(
            leadingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle(member) }) },
            headlineContent = { Text(member.name) },
            supportingContent = if (member.nickname.isNotBlank()) {
                { Text(member.nickname) }
            } else {
                null
            },
            trailingContent = if (checked && roles.isNotEmpty()) {
                { RolePicker(roles, selected[member.id], onChange = { onRoleChange(member.id, it) }) }
            } else {
                null
            },
            modifier = Modifier.clickable { onToggle(member) },
        )
    }
}

/** A small dropdown for choosing an event role, or none. */
@Composable
fun RolePicker(roles: List<EventRole>, roleId: String?, onChange: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(roles.firstOrNull { it.id == roleId }?.name ?: stringResource(R.string.role_none))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.role_none)) },
                onClick = {
                    onChange(null)
                    open = false
                },
            )
            roles.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role.name) },
                    onClick = {
                        onChange(role.id)
                        open = false
                    },
                )
            }
        }
    }
}
