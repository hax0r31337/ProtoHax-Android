package dev.sora.protohax.ui.overlay.menu.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.ui.components.clickableNoRipple
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.HudTab() {
	val list = remember { MinecraftRelay.hudManager.elements.toMutableStateList() }
	val refreshList = {
		list.clear()
		list.addAll(MinecraftRelay.hudManager.elements)
	}
	val snackbarHostState = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	val mContext = LocalContext.current

	LazyColumn {
		item {
			Spacer(modifier = Modifier.height(10.dp))
		}
		items(list) {
			val expand = remember { mutableStateOf(false) }
			Card(
				colors = CardDefaults.cardColors(containerColor = animateColorAsState(targetValue = if (expand.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inversePrimary).value),
				modifier = Modifier
					.fillMaxWidth()
					.padding(15.dp, 7.dp)
					.animateItemPlacement(),
				onClick = {
					expand.value = !expand.value
				}
			) {
				Column(modifier = Modifier.padding(PaddingValues(13.dp, 3.dp, 13.dp, if (expand.value) 13.dp else 3.dp))) {
					Box(
						modifier = Modifier.padding(PaddingValues(0.dp, 10.dp, 0.dp, 10.dp))
					) {
						Text(
							text = it.name,
							fontWeight = if (expand.value) FontWeight.Bold else null,
							modifier = Modifier
								.fillMaxWidth()
								.padding(0.dp, 0.dp, 30.dp, 0.dp)
								.align(Alignment.CenterStart)
								.basicMarquee(iterations = Int.MAX_VALUE)
						)

						IconButton(
							onClick = {
								scope.launch {
									val result = snackbarHostState.showSnackbar(
										message = mContext.getString(R.string.hud_delete_prompt, it.name),
										actionLabel = mContext.getString(R.string.config_delete),
										duration = SnackbarDuration.Short
									)
									if (result == SnackbarResult.ActionPerformed) {
										MinecraftRelay.hudManager.removeElement(it)
										refreshList()
									}
								}
							},
							modifier = Modifier
								.align(Alignment.CenterEnd)
								.height(25.dp)
								.width(30.dp)
						) {
							Icon(Icons.Filled.Delete, null)
						}
					}
					AnimatedVisibility(
						visible = expand.value,
						modifier = Modifier.clickableNoRipple { }) {
						Box {
							val recomposeTrigger = remember { mutableStateOf(0) }
							Text(text = "${recomposeTrigger.value}", color = Color.Transparent)
							val recomposeTriggerFunc = {
								recomposeTrigger.value =
									if (recomposeTrigger.value == Int.MAX_VALUE) {
										Int.MIN_VALUE
									} else {
										recomposeTrigger.value + 1
									}
							}
							Column {
								it.values.forEach { value ->
									if (value.isVisibilityVariable) {
										AnimatedVisibility(visible = value.visible) {
											Column {
												CheatValue(value, recomposeTriggerFunc)
											}
										}
									} else {
										CheatValue(value, recomposeTriggerFunc)
									}
								}
							}
						}
					}
				}
			}
		}
		item {
			Spacer(modifier = Modifier.height(10.dp))
		}
	}

	SnackbarHost(
		hostState = snackbarHostState,
		modifier = Modifier.align(Alignment.BottomEnd).padding(0.dp, 0.dp, 0.dp, 50.dp),
	)

	val showMenu = remember { mutableStateOf(false) }

	Card(
		modifier = Modifier.align(Alignment.BottomEnd),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
		shape = RoundedCornerShape(15.dp, 0.dp, 0.dp, 0.dp)
	) {
		Row {
			IconButton(onClick = {
				scope.launch {
					snackbarHostState.showSnackbar("TODO")
				}
			}) {
				Icon(Icons.Default.Edit, null)
			}
			IconButton(onClick = {
				showMenu.value = true
			}) {
				Icon(Icons.Default.Add, null)
			}
		}
	}


	Box(
		modifier = Modifier
			.fillMaxSize()
			.align(Alignment.BottomEnd)
			.wrapContentSize(Alignment.BottomEnd)
			.padding(7.dp, 0.dp)
	) {
		DropdownMenu(
			expanded = showMenu.value,
			onDismissRequest = { showMenu.value = false }
		) {
			MinecraftRelay.hudManager.elementRegistry.keys.forEach {
				DropdownMenuItem(
					text = { Text(it) },
					onClick = {
						showMenu.value = false
						MinecraftRelay.hudManager.addElement(it)
						refreshList()
					}
				)
			}
		}
	}
}
