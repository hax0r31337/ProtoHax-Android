package dev.sora.protohax.ui.components.screen.settings

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sora.protohax.R
import dev.sora.protohax.ui.activities.LogActivity
import dev.sora.protohax.ui.components.PHaxAppBar
import dev.sora.protohax.util.NavigationType
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@Composable
fun SettingsTab(
	name: Int,
	description: Int,
	modifier: Modifier = Modifier,
	extraPadding: Boolean = true,
	extra: (@Composable () -> Unit)? = null,
) {
	Row(
		verticalAlignment = Alignment.CenterVertically,
		modifier = modifier
			.padding(18.dp, 10.dp)
	) {
		Column(modifier = Modifier.fillMaxWidth(if (extraPadding) 0.8f else 1f)) {
			Text(text = stringResource(name))
			Text(text = stringResource(description), color = MaterialTheme.colorScheme.outline, fontSize = 14.sp, lineHeight = 18.sp)
		}
		if (extra != null) {
			Spacer(Modifier.weight(1f))
			extra()
		}
	}
}

@Composable
fun SettingsScreen(navigationType: NavigationType) {
	val snackbarHostState = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	val mContext = LocalContext.current

	val restartRequired = {
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
			Settings.settings.forEach {
				it.Draw(restartRequired)
			}
			SettingsTab(
				name = R.string.setting_logs, description = R.string.setting_logs_desc,
				modifier = Modifier
					.clickable {
						mContext.startActivity(Intent(mContext, LogActivity::class.java))
					},
				extraPadding = false
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
