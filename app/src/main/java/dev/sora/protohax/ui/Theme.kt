package dev.sora.protohax.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ColorSakuraPrimary,
    secondary = ColorSakuraSecondary,
    tertiary = ColorSakuraTertiary,
)

private val LightColorScheme = lightColorScheme(
    primary = ColorSakuraPrimary,
    secondary = ColorSakuraSecondary,
    tertiary = ColorSakuraTertiary,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
//    val view = LocalView.current
//    if (!view.isInEditMode) {
//        rememberSystemUiController().setSystemBarsColor(Color.Transparent)
//    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}