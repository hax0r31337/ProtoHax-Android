package dev.sora.protohax.ui.overlay.menu.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.BoolValue
import dev.sora.relay.cheat.value.ListValue
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.cheat.value.StringValue
import dev.sora.relay.cheat.value.Value

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CheatCategoryTab(category: CheatCategory, modules: SnapshotStateMap<CheatModule, Boolean>, expandModules: SnapshotStateList<CheatModule>) {
	LazyColumn {
		item {
			Spacer(modifier = Modifier.height(10.dp))
		}
		items(modules.keys.filter { it.category == category }.sortedBy { it.name }) { module ->
			val expand = expandModules.contains(module)
			val content: @Composable ColumnScope.() -> Unit = {
				Column(modifier = Modifier.padding(PaddingValues(13.dp, 3.dp, 13.dp, if (expand) 13.dp else 3.dp))) {
					Box(modifier = Modifier.fillMaxWidth()) {
						Text(
							text = module.name,
							fontWeight = if (expand) FontWeight.Bold else null,
							modifier = Modifier
								.fillMaxWidth()
								.padding(0.dp, 0.dp, 60.dp, 0.dp)
								.basicMarquee(iterations = Int.MAX_VALUE)
								.align(Alignment.CenterStart)
						)
						Switch(
							checked = module.state,
							modifier = Modifier.align(Alignment.CenterEnd)
								.let { if (module.canToggle) it else it
									.clickable { module.state = true }
									.minimumInteractiveComponentSize() },
							onCheckedChange = if (module.canToggle) { {
								module.state = it
							} } else null,
							enabled = module.canToggle,
							colors = if (expand) {
								SwitchDefaults.colors(checkedBorderColor = MaterialTheme.colorScheme.outlineVariant)
							} else {
								SwitchDefaults.colors()
							}
						)
					}
					if (module.values.isNotEmpty()) {
						AnimatedVisibility(visible = expand) {
							Box {
								val recomposeTrigger = remember { mutableStateOf(0) }
								Text(text = "${recomposeTrigger.value}", color = Color.Transparent)
								Column {
									module.values.forEach { value ->
										CheatValue(value) {
											recomposeTrigger.value = if (recomposeTrigger.value == Int.MAX_VALUE) {
												Int.MIN_VALUE
											} else {
												recomposeTrigger.value + 1
											}
										}
									}
								}
							}
						}
					}
				}
			}
			if (module.values.isNotEmpty()) {
				Card(
					colors = CardDefaults.cardColors(containerColor = if (expand) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inversePrimary),
					modifier = Modifier
						.fillMaxWidth()
						.padding(15.dp, 7.dp),
					onClick = {
						if (expand) {
							expandModules.remove(module)
						} else {
							expandModules.add(module)
						}
					},
					content = content
				)
			} else {
				Card(
					colors = CardDefaults.cardColors(containerColor = if (expand) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inversePrimary),
					modifier = Modifier
						.fillMaxWidth()
						.padding(15.dp, 7.dp),
					content = content
				)
			}
		}
		item {
			Spacer(modifier = Modifier.height(10.dp))
		}
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheatValue(value: Value<*>, recomposeTrigger: () -> Unit) {
	if (value is BoolValue) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.fillMaxWidth()
				.height(30.dp)
		) {
			Text(text = value.name, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE))
			Spacer(modifier = Modifier.weight(1f))
			Checkbox(
				checked = value.value,
				onCheckedChange = {
					value.value = it
					recomposeTrigger()
				},
				colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.onPrimary, uncheckedColor = MaterialTheme.colorScheme.onPrimary, checkmarkColor = MaterialTheme.colorScheme.secondary)
			)
		}
	} else if (value is StringValue) {
		Row {
			Text(text = value.name)
			Spacer(modifier = Modifier.weight(1f))
			Text(
				text = value.value,
				maxLines = 1,
				modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
				textDecoration = TextDecoration.Underline
			)
		}
	} else if (value is ListValue) {
		Row {
			Text(text = value.name)
			Spacer(modifier = Modifier.weight(1f))
			Row(modifier = Modifier.clickable {

			}) {
				Text(
					text = value.value.let { if (it is NamedChoice) it.choiceName else it.toString() },
					maxLines = 1,
					modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
					textDecoration = TextDecoration.Underline
				)
				Icon(Icons.Filled.ArrowDropDown, null)
			}
		}
	}
}
