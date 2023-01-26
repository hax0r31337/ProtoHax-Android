package dev.sora.protohax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import dev.sora.protohax.ui.MyApplicationTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text(text = "Hello, World!")
                }
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