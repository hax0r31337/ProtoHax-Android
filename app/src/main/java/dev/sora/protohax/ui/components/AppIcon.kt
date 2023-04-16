package dev.sora.protohax.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import dev.sora.protohax.R

@Composable
fun AppIcon(
	modifier: Modifier = Modifier
) {
	Image(
		painter = rememberDrawablePainter(drawable = ResourcesCompat.getDrawable(
			LocalContext.current.resources,
			R.mipmap.ic_launcher, LocalContext.current.theme
		)),
		"AppIcon",
		modifier = modifier
	)
}
