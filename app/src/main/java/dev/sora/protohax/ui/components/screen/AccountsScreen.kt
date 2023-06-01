package dev.sora.protohax.ui.components.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sora.protohax.R
import dev.sora.protohax.relay.Account
import dev.sora.protohax.relay.AccountManager
import dev.sora.protohax.ui.activities.MicrosoftLoginActivity
import dev.sora.protohax.ui.components.PHaxAppBar
import dev.sora.protohax.util.NavigationType
import dev.sora.protohax.util.isValidRemark
import dev.sora.protohax.util.suggestRemark
import dev.sora.relay.session.listener.xbox.XboxDeviceInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(navigationType: NavigationType) {
    val list = remember { AccountManager.accounts.toMutableStateList() }
    val refreshList = {
        list.clear()
        list.addAll(AccountManager.accounts)
        Unit
    }

    if (list.size != AccountManager.accounts.size) {
        refreshList()
    }

    val menuCreate = remember { mutableStateOf(false) }
    MenuCreate(menuCreate, refreshList)

    PHaxAppBar(
        title = stringResource(id = R.string.tab_accounts),
        navigationType = navigationType,
        actions = {
            IconButton(onClick = { menuCreate.value = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.accounts_login)
                )
            }
        }
    ) { innerPadding ->
        val dialogRename = remember { mutableStateOf<Account?>(null) }
        val dialogDelete = remember { mutableStateOf<Account?>(null) }

        DialogRename(dialogRename, refreshList)
        DialogDelete(dialogDelete, refreshList)

        LazyColumn(
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(list, key = { it.remark }) {
				Box(modifier = Modifier.animateItemPlacement()) {
					val expanded = remember { mutableStateOf(false) }
					dev.sora.protohax.ui.components.ListItem(
						title = it.remark,
						description = {
							Row {
								Text(text = it.platform.deviceType, color = MaterialTheme.colorScheme.onSurfaceVariant)
								if (it == AccountManager.currentAccount) {
									Text(
										stringResource(R.string.account_selected),
										modifier = Modifier.padding(6.dp, 0.dp),
										color = MaterialTheme.colorScheme.primary,
										fontWeight = FontWeight.Bold
									)
								}
							}
						},
						onClick = {
							if (it == AccountManager.currentAccount) {
								AccountManager.currentAccount = null
							} else {
								AccountManager.currentAccount = it
							}
							refreshList()
						},
						expanded = expanded
					) {
						if (it == AccountManager.currentAccount) {
							DropdownMenuItem(
								text = { Text(stringResource(R.string.config_deselect)) },
								onClick = {
									AccountManager.currentAccount = null
									refreshList()
									expanded.value = false
								},
								leadingIcon = { Icon(Icons.Outlined.CheckBoxOutlineBlank, contentDescription = null) }
							)
						} else {
							DropdownMenuItem(
								text = { Text(stringResource(R.string.account_select)) },
								onClick = {
									AccountManager.currentAccount = it
									refreshList()
									expanded.value = false
								},
								leadingIcon = { Icon(Icons.Outlined.CheckBox, contentDescription = null) }
							)
						}
						DropdownMenuItem(
							text = { Text(stringResource(R.string.config_rename)) },
							onClick = {
								dialogRename.value = it
								expanded.value = false
							},
							leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
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
private fun MenuCreate(state: MutableState<Boolean>, callback: () -> Unit) {
    Box(
        modifier = Modifier
			.fillMaxSize()
			.wrapContentSize(Alignment.TopEnd)
			.padding(12.dp, 0.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        val loginActivityLauncher = rememberLauncherForActivityResult(MicrosoftLoginActivity.LauncherContract()) {
            callback()
        }

        DropdownMenu(
            expanded = state.value,
            onDismissRequest = { state.value = false }
        ) {
            XboxDeviceInfo.devices.values.forEach { device ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.accounts_login_as, device.deviceType)) },
                    onClick = {
                        loginActivityLauncher.launch(device)
                    }
                )
            }
        }
    }
}

@Composable
private fun DialogRename(target: MutableState<Account?>, callback: () -> Unit) {
	val value = target.value
	if (value != null) {
		val name: MutableState<String?> = remember { mutableStateOf(null) }
		if (name.value == null) {
			name.value = value.remark
		}

		AlertDialog(
			onDismissRequest = { target.value = null },
			title = { Text(stringResource(R.string.config_rename_dialog_message)) },
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
					enabled = isValidRemark(AccountManager.accounts.map { it.remark }, name.value ?: ""),
					onClick = {
						value.remark = name.value ?: ""
						AccountManager.save()
						target.value = null
						name.value = null

						callback()
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
private fun DialogDelete(target: MutableState<Account?>, callback: () -> Unit) {
    val value = target.value

	if (value != null) {
        AlertDialog(
            onDismissRequest = { target.value = null },
            title = { Text(stringResource(R.string.dialog_title)) },
            text = { Text(stringResource(R.string.account_delete_dialog_message, value.remark)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        AccountManager.accounts.remove(value)
                        if (AccountManager.currentAccount == value) {
                            AccountManager.currentAccount = null
                        }
                        AccountManager.save()
                        target.value = null
                        callback()
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
