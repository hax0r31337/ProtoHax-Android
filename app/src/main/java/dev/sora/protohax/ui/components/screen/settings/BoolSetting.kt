package dev.sora.protohax.ui.components.screen.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.sora.protohax.util.ContextUtils.readBoolean
import dev.sora.protohax.util.ContextUtils.writeBoolean

class BoolSetting(override val name: Int, override val description: Int, val key: String, val default: Boolean,
				  override val restartRequired: Boolean = false, val callback: (Boolean) -> Unit = {}) : ISetting<Boolean> {

	@Composable
	override fun Draw(restartRequiredCallback: () -> Unit) {
		val mContext = LocalContext.current
		var value by remember { mutableStateOf(getValue(mContext)) }

		SettingsTab(
			name = name, description = description,
			modifier = Modifier
				.clickable {
					value = !value
					setValue(mContext, value)
					restartRequiredCallback()
				}
		) {
			Switch(value, onCheckedChange = {
				value = !value
				setValue(mContext, value)
				restartRequiredCallback()
			})
		}
	}

	override fun getValue(context: Context): Boolean {
		return context.readBoolean(key, default)
	}

	override fun setValue(context: Context, value: Boolean) {
		callback(value)
		context.writeBoolean(key, value)
	}
}
