package dev.sora.protohax.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListItem(
    title: String,
    description: @Composable () -> Unit = {},
    dropdownTitle: Boolean = true,
    expanded: MutableState<Boolean> = remember { mutableStateOf(false) },
    onClick: () -> Unit = {},
    menuItems: @Composable (ColumnScope.() -> Unit)
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .combinedClickable(onLongClick = {
                expanded.value = true
            }, onClick = onClick)
            .fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp, 12.dp)) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface)
            description()
//            if (description.isNotEmpty()) {
//                Text(text = description, color = MaterialTheme.colorScheme.onSurfaceVariant)
//            }
        }

        Box(
            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.CenterEnd).padding(12.dp, 0.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(onClick = { expanded.value = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                if (dropdownTitle) {
                    DropdownMenuItem(
                        text = { Text(text = title, color = MaterialTheme.colorScheme.primary) },
                        onClick = {},
                        enabled = false
                    )
                    Divider()
                }
                menuItems()
            }
        }
    }
}