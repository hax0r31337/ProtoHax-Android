package dev.sora.protohax.ui.components

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.sora.protohax.util.ContextUtils.shareText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LogsScreen() {
    val scope = rememberCoroutineScope()
    fun refresh(text: MutableState<String>, flush: Boolean = false) {
        scope.launch {
            withContext(Dispatchers.IO) {
                if (flush) {
                    val process = Runtime.getRuntime().exec(arrayOf("logcat", "-c"))
                    process.waitFor()
                }
                val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "-s", "ProtoHax"))
                text.value = process.inputStream.bufferedReader().use { it.readText() }
            }
        }
    }
    val text = remember { mutableStateOf("") }
    val mContext = LocalContext.current

    PHaxAppBar(
        title = stringResource(id = dev.sora.protohax.R.string.tab_logs),
        actions = {
            IconButton(onClick = {
                mContext.shareText(text.value, mContext.getString(dev.sora.protohax.R.string.tab_logs))
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(id = dev.sora.protohax.R.string.logcat_clear)
                )
            }
            IconButton(onClick = { refresh(text, true) }) {
                Icon(
                    imageVector = Icons.Default.ClearAll,
                    contentDescription = stringResource(id = dev.sora.protohax.R.string.logcat_clear)
                )
            }
        }) {
        // this method was triggered every time the view updated
        refresh(text)

        SelectionContainer {
            Text(text = text.value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}