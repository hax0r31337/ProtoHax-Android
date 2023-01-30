package dev.sora.protohax.ui.components.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sora.protohax.R
import dev.sora.protohax.ui.components.PHaxAppBar
import dev.sora.protohax.util.ContextUtils.shareTextAsFile
import dev.sora.protohax.util.NavigationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LogsScreen(navigationType: NavigationType) {
    val scope = rememberCoroutineScope()
    val text = remember { mutableStateOf("") }
    val mContext = LocalContext.current

    PHaxAppBar(
        title = stringResource(id = R.string.tab_logs),
        navigationType = navigationType,
        actions = {
            IconButton(onClick = {
                mContext.shareTextAsFile(text.value, mContext.getString(R.string.tab_logs))
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(id = R.string.logcat_share)
                )
            }
            IconButton(onClick = { scope.refreshLogs(text, true) }) {
                Icon(
                    imageVector = Icons.Default.ClearAll,
                    contentDescription = stringResource(id = R.string.logcat_clear)
                )
            }
        }
    ) { innerPadding ->
        Spacer(modifier = Modifier.size(8.dp))
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // this method was triggered every time the view updated
            scope.refreshLogs(text)

            SelectionContainer {
                Text(text = text.value, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun CoroutineScope.refreshLogs(text: MutableState<String>, flush: Boolean = false) {
    launch {
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