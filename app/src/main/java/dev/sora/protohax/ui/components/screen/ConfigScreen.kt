package dev.sora.protohax.ui.components.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.ui.components.ListItem
import dev.sora.protohax.ui.components.PHaxAppBar
import dev.sora.protohax.util.NavigationType
import dev.sora.protohax.util.isValidRemark
import dev.sora.protohax.util.suggestRemark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConfigScreen(navigationType: NavigationType) {
    val mContext = LocalContext.current
	val scope = rememberCoroutineScope()
    val list = remember { MinecraftRelay.configManager.listConfig().sorted().toMutableStateList() }
    val refreshList = {
        list.clear()
        list.addAll(MinecraftRelay.configManager.listConfig())
        list.sort()
    }

    val dialogCreate = remember { mutableStateOf(false) }
    DialogCreate(dialogCreate, refreshList)

    val fileSelectorLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it ?: return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                val fos = File(mContext.getExternalFilesDir("configs")!!, "imported-${System.currentTimeMillis()}.json")
                val fis = mContext.contentResolver.openInputStream(it) ?: return@withContext
                fis.copyTo(fos.outputStream())
                fis.close()
                refreshList()
            }
        }
    }

    PHaxAppBar(
        title = stringResource(id = R.string.tab_configs),
        navigationType = navigationType,
        actions = {
            IconButton(onClick = { fileSelectorLauncher.launch(arrayOf("application/json")) }) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = stringResource(id = R.string.config_import)
                )
            }
            IconButton(onClick = { dialogCreate.value = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.config_create)
                )
            }
        }
    ) { innerPadding ->
        val dialogCopy: MutableState<String?> = remember { mutableStateOf(null) }
        val dialogRename: MutableState<String?> = remember { mutableStateOf(null) }
        val dialogDelete: MutableState<String?> = remember { mutableStateOf(null) }

        DialogRenameCopy(dialogCopy, true, refreshList)
        DialogRenameCopy(dialogRename, false, refreshList)
        DialogDelete(dialogDelete, refreshList)

        LazyColumn(
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(list, key = { it }) {
                Box(modifier = Modifier.animateItemPlacement()) {
					val expanded = remember { mutableStateOf(false) }
					ListItem(
						title = it,
						expanded = expanded
					) {
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
}

@Composable
private fun DialogCreate(target: MutableState<Boolean>, callback: () -> Unit) {
	AnimatedVisibility(
		visible = target.value,
	) {
		val name = remember { mutableStateOf("") }
		if (name.value.isEmpty()) {
			name.value = suggestRemark()
		}

		AlertDialog(
			onDismissRequest = { target.value = false },
			title = { Text(stringResource(R.string.config_create)) },
			text = {
				TextField(
					value = name.value,
					onValueChange = { name.value = it },
					trailingIcon = {
						IconButton(onClick = { name.value = suggestRemark() }) {
							Icon(Icons.Filled.Refresh, null)
						}
					}
				)
			},
			confirmButton = {
				TextButton(
					enabled = isValidRemark(MinecraftRelay.configManager.listConfig(), name.value),
					onClick = {
						target.value = false
						MinecraftRelay.configManager.getConfigFile(name.value)
							.writeText("{}")
						name.value = ""
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

@Composable
private fun DialogRenameCopy(target: MutableState<String?>, copy: Boolean, callback: () -> Unit) {
    val value = target.value

	AnimatedVisibility(
		visible = value != null,
	) {
		val name: MutableState<String?> = remember { mutableStateOf(null) }
		if (name.value == null) {
			name.value = value
		}

        AlertDialog(
            onDismissRequest = { target.value = null },
            title = { Text(stringResource(if (copy) R.string.config_copy_dialog_message else R.string.config_rename_dialog_message)) },
			text = {
				TextField(
					value = name.value ?: "",
					onValueChange = { name.value = it },
					trailingIcon = {
						IconButton(onClick = { name.value = suggestRemark() }) {
							Icon(Icons.Filled.Refresh, null)
						}
					}
				)
			},
            confirmButton = {
                TextButton(
					enabled = isValidRemark(MinecraftRelay.configManager.listConfig(), name.value ?: ""),
                    onClick = {
						if (value != null) {
							if (copy) {
								MinecraftRelay.configManager.copyConfig(value, name.value ?: "")
							} else {
								MinecraftRelay.configManager.renameConfig(value, name.value ?: "")
							}
							target.value = null
							name.value = null
							callback()
						}
                    }
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { target.value = null }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun DialogDelete(target: MutableState<String?>, callback: () -> Unit) {
	val value = target.value

	AnimatedVisibility(
		visible = value != null,
	) {
        AlertDialog(
            onDismissRequest = { target.value = null },
            title = { Text(stringResource(R.string.dialog_title)) },
            text = { Text(if (value != null) {
				stringResource(R.string.config_delete_dialog_message, value)
			} else {
				stringResource(R.string.config_delete_dialog_message_completed)
			}) },
            confirmButton = {
                TextButton(
                    onClick = {
						if (value != null) {
							MinecraftRelay.configManager.deleteConfig(value)
							target.value = null
							callback()
						}
                    }
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { target.value = null }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}
