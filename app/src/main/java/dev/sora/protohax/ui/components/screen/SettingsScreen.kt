package dev.sora.protohax.ui.components.screen

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.ui.activities.LogActivity
import dev.sora.protohax.ui.components.PHaxAppBar
import dev.sora.protohax.util.ContextUtils.readBoolean
import dev.sora.protohax.util.ContextUtils.writeBoolean
import dev.sora.protohax.util.NavigationType
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@Composable
private fun SettingsTab(
	name: Int,
	description: Int,
	modifier: Modifier = Modifier,
	extra: @Composable () -> Unit = {},
) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = modifier
	) {
		Column(modifier = Modifier.fillMaxWidth(0.8f)) {
			Text(text = stringResource(name))
			Text(text = stringResource(description), color = MaterialTheme.colorScheme.outline, fontSize = 14.sp, lineHeight = 18.sp)
		}
		Spacer(Modifier.weight(1f))
		extra()
	}
}

@Composable
private fun SettingsTabToggle(
	name: Int,
	description: Int,
	modifier: Modifier = Modifier,
	value: Boolean,
	onChanged: (Boolean) -> Unit,
) {
	SettingsTab(
		name = name, description = description,
		modifier = modifier
			.clickable { onChanged(!value) }
			.padding(18.dp, 10.dp)
	) {
		Switch(value, onCheckedChange = { onChanged(!value) })
	}
}

@Composable
private fun SettingsTabTogglePerf(
	name: Int,
	description: Int,
	key: String,
	default: Boolean,
	modifier: Modifier = Modifier,
	onToggle: (Boolean) -> Unit = {}
) {
	val mContext = LocalContext.current
	val value = remember { mutableStateOf(mContext.readBoolean(key, default)) }

	SettingsTabToggle(
		name = name, description = description, modifier = modifier,
		value = value.value, onChanged = {
			value.value = it
			mContext.writeBoolean(key, it)
			onToggle(it)
		}
	)
}

@Composable
fun SettingsScreen(navigationType: NavigationType) {
	val snackbarHostState = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	val mContext = LocalContext.current

	val restartRequired = { _: Boolean ->
		scope.launch {
			val result = snackbarHostState.showSnackbar(
				message = mContext.getString(R.string.setting_restart_required),
				actionLabel = mContext.getString(R.string.setting_restart_required_action),
				duration = SnackbarDuration.Long
			)
			if (result == SnackbarResult.ActionPerformed) {
				exitProcess(0)
			}
		}
		Unit
	}

	PHaxAppBar(
		title = stringResource(id = R.string.tab_settings),
		navigationType = navigationType
	) { innerPadding ->
		Column(
			modifier = Modifier
				.padding(innerPadding)
				.verticalScroll(rememberScrollState()),
		) {
			SettingsTabTogglePerf(
				name = R.string.setting_encryption, description = R.string.setting_encryption_desc,
				key = MinecraftRelay.Constants.KEY_OFFLINE_SESSION_ENCRYPTION,  default = MinecraftRelay.Constants.KEY_OFFLINE_SESSION_ENCRYPTION_DEFAULT
			)
			SettingsTabTogglePerf(
				name = R.string.setting_commands, description = R.string.setting_commands_desc,
				key = MinecraftRelay.Constants.KEY_ENABLE_COMMAND_MANAGER,  default = MinecraftRelay.Constants.KEY_ENABLE_COMMAND_MANAGER_DEFAULT,
				onToggle = restartRequired
			)
			SettingsTabTogglePerf(
				name = R.string.setting_rak_reliability, description = R.string.setting_rak_reliability_desc,
				key = MinecraftRelay.Constants.KEY_ENABLE_RAK_RELIABILITY,  default = MinecraftRelay.Constants.KEY_ENABLE_RAK_RELIABILITY_DEFAULT,
				onToggle = restartRequired
			)
			SettingsTab(
				name = R.string.setting_logs, description = R.string.setting_logs_desc,
				modifier = Modifier
					.clickable {
						mContext.startActivity(Intent(mContext, LogActivity::class.java))
					}
					.padding(18.dp, 10.dp)
			)
		}
	}

	Box(modifier = Modifier.fillMaxSize()) {
		SnackbarHost(
			snackbarHostState,
			modifier = Modifier
				.align(Alignment.BottomEnd)
		)
	}
}
