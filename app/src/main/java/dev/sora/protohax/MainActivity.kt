package dev.sora.protohax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dev.sora.protohax.ui.components.PHaxApp
import dev.sora.protohax.ui.theme.MyApplicationTheme


class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        private const val REQUEST_CODE_WITH_MITM_RECALL = 0
        private const val REQUEST_CODE_WITH_MITM_RECALL_ONLY_OK = 1
        private const val REQUEST_CODE_MICROSOFT_LOGIN_OK = 2
        const val RESPONSE_CODE_MICROSOFT_LOGIN_OK = 1
    }
}