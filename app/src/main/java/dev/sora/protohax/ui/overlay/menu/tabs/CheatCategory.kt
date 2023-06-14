package dev.sora.protohax.ui.overlay.menu.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sora.protohax.ui.components.clickableNoRipple
import dev.sora.protohax.ui.overlay.OverlayManager
import dev.sora.protohax.ui.overlay.Shortcut
import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.BoolValue
import dev.sora.relay.cheat.value.FloatValue
import dev.sora.relay.cheat.value.IntRangeValue
import dev.sora.relay.cheat.value.IntValue
import dev.sora.relay.cheat.value.ListValue
import dev.sora.relay.cheat.value.NamedChoice
import dev.sora.relay.cheat.value.StringValue
import dev.sora.relay.cheat.value.Value
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CheatCategoryTab(
	category: CheatCategory,
	modules: SnapshotStateMap<CheatModule, Boolean>,
	expandModules: SnapshotStateList<CheatModule>,
	overlayManager: OverlayManager
) {
	LazyColumn {
		item {
			Spacer(modifier = Modifier.height(10.dp))
		}
		items(modules.keys.filter { it.category == category }.sortedBy { it.name }) { module ->
			val expand = expandModules.contains(module)
			Card(
				colors = CardDefaults.cardColors(containerColor = animateColorAsState(targetValue = if (expand) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inversePrimary).value),
				modifier = Modifier
					.fillMaxWidth()
					.padding(15.dp, 7.dp),
				onClick = {
					if (expand) {
						expandModules.remove(module)
					} else {
						expandModules.add(module)
					}
				}
			) {
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
						AnimatedVisibility(visible = expand, modifier = Modifier.clickableNoRipple {  }) {
							Box {
								val recomposeTrigger = remember { mutableStateOf(0) }
								Text(text = "${recomposeTrigger.value}", color = Color.Transparent)
								val recomposeTriggerFunc = {
									recomposeTrigger.value = if (recomposeTrigger.value == Int.MAX_VALUE) {
										Int.MIN_VALUE
									} else {
										recomposeTrigger.value + 1
									}
								}
								Column {
									module.values.forEach { value ->
										CheatValue(value, recomposeTriggerFunc)
									}
									CheatShortcut(module, recomposeTriggerFunc, overlayManager)
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CheatShortcut(
	module: CheatModule, recomposeTrigger: () -> Unit,
	overlayManager: OverlayManager
) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = Modifier
			.fillMaxWidth()
			.height(30.dp)
	) {
		Text(text = "Shortcut", modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE))
		Spacer(modifier = Modifier.weight(1f))
		Checkbox(
			checked = overlayManager.hasShortcut(module),
			onCheckedChange = {
				if (overlayManager.hasShortcut(module)) {
					overlayManager.removeShortcut(module)
				} else {
					overlayManager.addShortcut(Shortcut(module, overlayManager))
				}
				recomposeTrigger()
			},
			colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.onPrimary, uncheckedColor = MaterialTheme.colorScheme.onPrimary, checkmarkColor = MaterialTheme.colorScheme.secondary)
		)
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CheatValue(value: Value<*>, recomposeTrigger: () -> Unit) {
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
	} else if (value is IntValue || value is FloatValue) {
		Row(modifier = Modifier.padding(0.dp, 5.dp, 0.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
			Text(text = value.name, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE))
			Spacer(modifier = Modifier.weight(1f))
			Text(text = if (value.value is Float) {
				String.format("%.2f", value.value)
			} else value.value.toString())
		}
		val colors = SliderDefaults.colors(
			thumbColor = MaterialTheme.colorScheme.onPrimary,
			activeTrackColor = MaterialTheme.colorScheme.onPrimary,
			inactiveTrackColor = MaterialTheme.colorScheme.outline,
		)
		if (value is IntValue) {
			Slider(
				value = value.value.toFloat(),
				valueRange = value.range.toFloatRange(),
				onValueChange = {
					value.value = it.roundToInt()
					recomposeTrigger()
				},
				colors = colors,
				modifier = Modifier.height(25.dp)
			)
		} else if (value is FloatValue) { // use kotlin smart cast
			Slider(
				value = value.value,
				valueRange = value.range,
				onValueChange = {
					value.value = it
					recomposeTrigger()
				},
				colors = colors,
				modifier = Modifier.height(25.dp)
			)
		}
	} else if (value is IntRangeValue) {
		Row(modifier = Modifier.padding(0.dp, 5.dp, 0.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
			Text(text = value.name, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE))
			Spacer(modifier = Modifier.weight(1f))
			Text(text = "${value.value.first}..${value.value.last}")
		}
		RangeSlider(
			value = value.value.toFloatRange(),
			valueRange = value.range.toFloatRange(),
			onValueChange = {
				value.value = it.start.roundToInt()..it.endInclusive.roundToInt()
				recomposeTrigger()
			},
			colors = SliderDefaults.colors(
				thumbColor = MaterialTheme.colorScheme.onPrimary,
				activeTrackColor = MaterialTheme.colorScheme.onPrimary,
				inactiveTrackColor = MaterialTheme.colorScheme.outline,
			),
			modifier = Modifier.height(25.dp)
		)
	} else if (value is ListValue) {
		Text(
			text = value.name,
			modifier = Modifier
				.padding(0.dp, 5.dp)
				.basicMarquee(iterations = Int.MAX_VALUE)
		)
		Row(
			modifier = Modifier
				.horizontalScroll(rememberScrollState())
				.fillMaxWidth()
		) {
			value.values.forEach {
				val name = if (it is NamedChoice) it.choiceName else it.toString()
				val selected = value.value == it
				AssistChip(
					modifier = Modifier
						.padding(3.dp, 2.dp)
						.height(30.dp),
					onClick = {
						value.fromString(name)
						recomposeTrigger()
					},
					label = {
						Text(
							text = name,
							style = TextStyle(fontSize = 14.sp)
						)
					},
					colors = AssistChipDefaults.elevatedAssistChipColors(
						containerColor = animateColorAsState(targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline).value,
						labelColor = animateColorAsState(targetValue = if (selected) MaterialTheme.colorScheme.secondary else Color.LightGray).value,
					),
					border = null
				)
			}
		}
	} else if (value is StringValue) {
		Text(text = value.name)
		TextField(
			value = value.value,
			onValueChange = {
				value.value = it
				recomposeTrigger()
			},
			singleLine = true,
			modifier = Modifier
				.fillMaxWidth()
				.padding(0.dp, 0.dp, 0.dp, 10.dp),
			colors = OutlinedTextFieldDefaults.colors(
				focusedTextColor = MaterialTheme.colorScheme.onPrimary,
				unfocusedTextColor = MaterialTheme.colorScheme.primaryContainer,
				focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
				unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
				focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
				unfocusedLabelColor = MaterialTheme.colorScheme.primaryContainer,
			)
		)
	}
}


private fun IntRange.toFloatRange()
	= first.toFloat()..last.toFloat()
