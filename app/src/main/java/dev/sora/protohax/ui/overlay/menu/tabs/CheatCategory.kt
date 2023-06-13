package dev.sora.protohax.ui.overlay.menu.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheatCategoryTab(category: CheatCategory, modules: SnapshotStateMap<CheatModule, Boolean>, expandModules: SnapshotStateList<CheatModule>) {
	LazyColumn {
		item {
			Spacer(modifier = Modifier.height(10.dp))
		}
		items(modules.keys.filter { it.category == category }.sortedBy { it.name }) { module ->
			val expand = expandModules.contains(module)
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
				}
			) {
				Column(modifier = Modifier.padding(PaddingValues(13.dp, 3.dp, 13.dp, if (expand) 13.dp else 3.dp))) {
					Box(modifier = Modifier.fillMaxWidth()) {
						Text(
							text = module.name,
							fontWeight = if (expand) FontWeight.Bold else null,
							modifier = Modifier.align(Alignment.CenterStart)
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
							colors = SwitchDefaults.colors(checkedBorderColor = MaterialTheme.colorScheme.outline)
						)
					}
					AnimatedVisibility(visible = expand) {
						Text(text = "test")
					}
				}
			}
		}
		item {
			Spacer(modifier = Modifier.height(10.dp))
		}
	}
}
