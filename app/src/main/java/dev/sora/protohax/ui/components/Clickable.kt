package dev.sora.protohax.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
	return clickable(
		interactionSource = remember { MutableInteractionSource() },
		indication = null,
		onClick = onClick
	)
}
