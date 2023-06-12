package dev.sora.protohax.ui.overlay.menu.tabs

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.relay.cheat.module.CheatCategory

@Composable
fun CheatCategoryTab(category: CheatCategory) {
	LazyColumn {
		items(MinecraftRelay.moduleManager.modules.filter { it.category == category }.sortedBy { it.name }) {
			Text(text = it.name)
		}
	}
}
