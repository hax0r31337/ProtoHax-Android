package dev.sora.protohax.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.view.WindowCompat
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dev.sora.protohax.relay.service.AppService
import dev.sora.protohax.ui.components.PHaxApp
import dev.sora.protohax.ui.theme.MyApplicationTheme


class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialize vpn service
        val intent = Intent(AppService.ACTION_INITIALIZE)
        intent.setPackage(packageName)
        startService(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MyApplicationTheme {
                val windowSize = calculateWindowSizeClass(this)
                val displayFeatures = calculateDisplayFeatures(this)

                PHaxApp(
                    windowSize = windowSize,
                    displayFeatures = displayFeatures,
                )
            }
        }
    }

    companion object {
        const val KEY_TARGET_PACKAGE_CACHE = "TARGET_PACKAGE"
        const val KEY_MICROSOFT_REFRESH_TOKEN = "MICROSOFT_REFRESH_TOKEN"
        const val RESPONSE_CODE_MICROSOFT_LOGIN_OK = 1
    }
}