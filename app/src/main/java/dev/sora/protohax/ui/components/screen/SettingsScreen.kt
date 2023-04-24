package dev.sora.protohax.ui.components.screen

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sora.protohax.R
import dev.sora.protohax.relay.service.AppService
import dev.sora.protohax.ui.activities.LogActivity
import dev.sora.protohax.ui.components.PHaxAppBar
import dev.sora.protohax.util.ContextUtils.readString
import dev.sora.protohax.util.ContextUtils.writeString
import dev.sora.protohax.util.NavigationType

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
		Column {
			Text(text = stringResource(name))
			Text(text = stringResource(description), color = MaterialTheme.colorScheme.outline)
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
	modifier: Modifier = Modifier
) {
	val mContext = LocalContext.current
	val value = remember { mutableStateOf(mContext.readString(key) == "true") }

	SettingsTabToggle(
		name = name, description = description, modifier = modifier,
		value = value.value, onChanged = {
			value.value = it
			mContext.writeString(key, it.toString())
		}
	)
}

@Composable
fun SettingsScreen(navigationType: NavigationType) {
	PHaxAppBar(
		title = stringResource(id = R.string.tab_settings),
		navigationType = navigationType
	) { innerPadding ->
		Column(
			modifier = Modifier
				.padding(innerPadding)
				.verticalScroll(rememberScrollState()),
		) {
			val mContext = LocalContext.current
			SettingsTabTogglePerf(
				name = R.string.setting_encryption, description = R.string.setting_encryption_desc,
				key = AppService.KEY_OFFLINE_SESSION_ENCRYPTION
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
}
