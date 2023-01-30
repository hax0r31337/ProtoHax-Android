package dev.sora.protohax.ui.activities

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import dev.sora.protohax.ui.theme.MyApplicationTheme
import dev.sora.protohax.util.ContextUtils.hasInternetPermission

class AppPickerActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        setContent {
            MyApplicationTheme {
                Content()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Content() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(id = dev.sora.protohax.R.string.dashboard_select_application))
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Filled.ArrowBack, null)
                        }
                    }
                )
            },
            content = { innerPadding ->
                val text = remember { mutableStateOf("") }

                Column(modifier = Modifier.padding(innerPadding)) {
                    TextField(
                        value = text.value,
                        singleLine = true,
                        onValueChange = { text.value = it },
                        placeholder = { Text(stringResource(dev.sora.protohax.R.string.dashboard_select_application_placeholder)) },
                        leadingIcon = { Icon(Icons.Filled.Search, null)},
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp, 0.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )

                    LazyColumn {
                        val listItems = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                            .filter { it.hasInternetPermission && it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.packageName != "dev.sora.protohax" }
                            .map { packageManager.getApplicationLabel(it.applicationInfo).toString() to it }.filter { it.first.startsWith(text.value, true) }.sortedBy { it.first }

                        items(listItems) {
                            val packageInfo = it.second
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp, 10.dp)
                                    .clickable {
                                        MainActivity.targetPackage = packageInfo.packageName
                                        finish()
                                    }
                            ) {
                                Image(
                                    painter = rememberDrawablePainter(packageManager.getApplicationIcon(packageInfo.packageName)),
                                    contentDescription = packageInfo.packageName,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.padding(6.dp, 0.dp))
                                Column {
                                    Text(packageManager.getApplicationLabel(packageInfo.applicationInfo).toString(), fontWeight = FontWeight.Bold)
                                    Text(packageInfo.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        )

    }

}