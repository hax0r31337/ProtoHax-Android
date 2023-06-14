package dev.sora.protohax.ui.components.screen.settings

import android.content.Context
import androidx.compose.runtime.Composable

interface ISetting<T> {

	val name: Int
	val description: Int
	val restartRequired: Boolean

	@Composable
	fun Draw(restartRequiredCallback: () -> Unit)

	fun getValue(context: Context): T
	fun setValue(context: Context, value: T)
}
