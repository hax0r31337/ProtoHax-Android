package dev.sora.protohax.ui.components.screen.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.sora.protohax.util.ContextUtils.readString
import dev.sora.protohax.util.ContextUtils.writeString

class TabSetting<T : TabChoice>(override val name: Int, val key: String,
				 val default: T, val choices: Array<T>,
				 override val restartRequired: Boolean = false) : ISetting<T> {

	override val description: Int
		get() = name

	@Composable
	override fun Draw(restartRequiredCallback: () -> Unit) {
		val mContext = LocalContext.current
		var value by remember { mutableStateOf(getValue(mContext)) }
		var expanded by remember { mutableStateOf(false) }

		SettingsTab(
			name = name, description = value.displayName,
			modifier = Modifier
				.clickable {
					expanded = true
				},
			extraPadding = false
		) {
			DropdownMenu(
				expanded = expanded,
				onDismissRequest = { expanded = false }
			) {
				choices.forEach { choice ->
					DropdownMenuItem(
						text = { Text(stringResource(id = choice.displayName)) },
						onClick = {
							expanded = false
							value = choice
							setValue(mContext, choice)
						}
					)
				}
			}
		}
	}

	override fun getValue(context: Context): T {
		val record = context.readString(key) ?: return default
		return choices.find { it.internalName == record } ?: default
	}

	override fun setValue(context: Context, value: T) {
		context.writeString(key, value.internalName)
	}
}

interface TabChoice {

	val displayName: Int
	val internalName: String
}
