package dev.sora.protohax.ui.overlay.menu.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.util.suggestRemark
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BoxScope.ConfigTab() {
	val list = remember { MinecraftRelay.configManager.listConfig().sorted().toMutableStateList() }
	val refreshList = {
		list.clear()
		list.addAll(MinecraftRelay.configManager.listConfig())
		list.sort()
	}
	val snackbarHostState = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	val mContext = LocalContext.current

	LazyColumn {
		item {
			Spacer(modifier = Modifier.height(10.dp))
		}
		items(list, key = { it }) {
			Card(
				colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inversePrimary),
				modifier = Modifier
					.fillMaxWidth()
					.padding(15.dp, 7.dp)
					.animateItemPlacement()
			) {
				Box(
					modifier = Modifier.padding(PaddingValues(13.dp))
				) {
					Text(
						text = it,
						modifier = Modifier
							.fillMaxWidth()
							.padding(0.dp, 0.dp, 60.dp, 0.dp)
							.align(Alignment.CenterStart)
							.basicMarquee(iterations = Int.MAX_VALUE)
					)
					Row(modifier = Modifier.align(Alignment.CenterEnd)) {
						IconButton(
							onClick = {
								MinecraftRelay.configManager.loadConfig(it)
								scope.launch {
									snackbarHostState.showSnackbar(mContext.getString(R.string.config_loaded, it))
								}
							},
							modifier = Modifier
								.height(25.dp)
								.width(30.dp)
						) {
							Icon(Icons.Filled.Publish, null)
						}
						IconButton(
							onClick = {
								MinecraftRelay.configManager.saveConfig(it)
								scope.launch {
									snackbarHostState.showSnackbar(mContext.getString(R.string.config_saved, it))
								}
							},
							modifier = Modifier
								.height(25.dp)
								.width(30.dp)
						) {
							Icon(Icons.Filled.Save, null)
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
	Card(
		modifier = Modifier.align(Alignment.BottomEnd),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
		shape = RoundedCornerShape(15.dp, 0.dp, 0.dp, 0.dp)
	) {
		IconButton(onClick = {
			MinecraftRelay.configManager.getConfigFile(suggestRemark())
				.writeText("{}")
			refreshList()
		}) {
			Icon(
				imageVector = Icons.Default.Add,
				contentDescription = stringResource(id = R.string.config_create)
			)
		}
	}
}
