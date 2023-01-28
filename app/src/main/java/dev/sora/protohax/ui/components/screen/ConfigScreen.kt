package dev.sora.protohax.ui.components.screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.ui.components.ListItem
import dev.sora.protohax.ui.components.PHaxAppBar
import dev.sora.protohax.util.NavigationType

@Composable
fun ConfigScreen(navigationType: NavigationType) {
    val mContext = LocalContext.current
    val list = remember { MinecraftRelay.configManager.listConfig().sorted().toMutableStateList() }
    val refreshList = {
        list.clear()
        list.addAll(MinecraftRelay.configManager.listConfig())
        list.sort()
    }

    val dialogCreate = remember { mutableStateOf(false) }
    DialogCreate(dialogCreate, refreshList)

    PHaxAppBar(
        title = stringResource(id = R.string.tab_configs),
        navigationType = navigationType,
        actions = {
            IconButton(onClick = { dialogCreate.value = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.config_create)
                )
            }
        }
    ) { innerPadding ->
        val dialogCopy = remember { mutableStateOf("") }
        val dialogRename = remember { mutableStateOf("") }
        val dialogDelete = remember { mutableStateOf("") }

        DialogRenameCopy(dialogCopy, true, refreshList)
        DialogRenameCopy(dialogRename, false, refreshList)
        DialogDelete(dialogDelete, refreshList)

        LazyColumn(
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(list) {
                val expanded = remember { mutableStateOf(false) }
                ListItem(title = it, expanded = expanded) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.config_copy)) },
                        onClick = {
                            dialogCopy.value = it
                            expanded.value = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.config_rename)) },
                        onClick = {
                            dialogRename.value = it
                            expanded.value = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.config_share)) },
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM,
                                    FileProvider.getUriForFile(mContext, mContext.packageName,
                                        MinecraftRelay.configManager.getConfigFile(it)))
                                type = "application/json"
                            }
                            mContext.startActivity(Intent.createChooser(shareIntent, null))
                            expanded.value = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.config_delete)) },
                        onClick = {
                            dialogDelete.value = it
                            expanded.value = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogCreate(target: MutableState<Boolean>, callback: () -> Unit) {
    if (target.value) {
        val name = remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { target.value = false },
            title = { Text(stringResource(R.string.config_create)) },
            text = { TextField(name.value, { name.value = it }) },
            confirmButton = {
                TextButton(
                    onClick = {
                        target.value = false
                        MinecraftRelay.configManager.getConfigFile(name.value)
                            .writeText("{}")
                        callback()
                    }
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { target.value = false }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogRenameCopy(target: MutableState<String>, copy: Boolean, callback: () -> Unit) {
    if (target.value.isNotEmpty()) {
        val name = remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { target.value = "" },
            title = { Text(stringResource(if (copy) R.string.config_copy_dialog_message else R.string.config_rename_dialog_message)) },
            text = { TextField(name.value, { name.value = it }) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (copy) {
                            MinecraftRelay.configManager.copyConfig(target.value, name.value)
                        } else {
                            MinecraftRelay.configManager.renameConfig(target.value, name.value)
                        }
                        target.value = ""
                        callback()
                    }
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { target.value = "" }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun DialogDelete(target: MutableState<String>, callback: () -> Unit) {
    if (target.value.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { target.value = "" },
            title = { Text(stringResource(R.string.dialog_title)) },
            text = { Text(stringResource(R.string.config_delete_dialog_message, target.value)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        MinecraftRelay.configManager.deleteConfig(target.value)
                        target.value = ""
                        callback()
                    }
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { target.value = "" }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}