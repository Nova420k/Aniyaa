package com.nyaa.aniyaa.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nyaa.aniyaa.data.model.Category
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder

@Composable
fun TinyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val color = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier
            .semantics {
                this.selected = selected
                role = Role.Button
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1
        )
    }
}

@Composable
fun MenuChip(
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    menu: @Composable () -> Unit
) {
    Box {
        TinyChip(text = label, selected = selected, onClick = onExpand)
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, content = { menu() })
    }
}

fun Category.compactLabel(): String = when {
    value == "0_0" -> "All"
    !isPrimary -> displayName.substringAfter(" - ").ifBlank { displayName }
    else -> shortLabel
}

fun sortChipLabel(field: SortField, order: SortOrder): String {
    val name = when (field) {
        SortField.DATE -> "Date"
        SortField.SEEDERS -> "Seeds"
        SortField.LEECHERS -> "Leech"
        SortField.SIZE -> "Size"
        SortField.DOWNLOADS -> "Dls"
        SortField.COMMENTS -> "Comments"
    }
    val arrow = if (order == SortOrder.DESC) "\u2193" else "\u2191"
    return "$name $arrow"
}
