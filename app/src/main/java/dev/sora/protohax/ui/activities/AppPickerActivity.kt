package dev.sora.protohax.ui.activities

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.drawablepainter.DrawablePainter
import dev.sora.protohax.R
import dev.sora.protohax.ui.theme.MyApplicationTheme
import dev.sora.protohax.util.ContextUtils.hasInternetPermission
import kotlinx.coroutines.launch

class AppPickerActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MyApplicationTheme {
                Content()
            }
        }
    }

    @SuppressLint("CoroutineCreationDuringComposition")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Content() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(stringResource(id = R.string.dashboard_select_application))
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
                        placeholder = { Text(stringResource(R.string.dashboard_select_application_placeholder)) },
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

                    val scope = rememberCoroutineScope()
                    val listIcons = remember { mutableStateMapOf<String, Painter?>() }
                    val listItems = remember {
                        mutableStateListOf<Pair<String, PackageInfo>>().also {
                            scope.launch {
                                it.addAll(packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                                    .filter { it.hasInternetPermission && it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.packageName != "dev.sora.protohax" }
                                    .map { packageManager.getApplicationLabel(it.applicationInfo).toString() to it }.sortedBy { it.first })
                            }
                        }
                    }

                    if (listItems.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(stringResource(R.string.dialog_loading))
                        }
                    }

                    LazyColumn {
                        items(listItems.filter { it.first.startsWith(text.value, true) }) {
                            val packageName = it.second.packageName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp, 10.dp)
                                    .clickable {
                                        MainActivity.targetPackage = packageName
                                        finish()
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = listIcons[packageName]
                                if (icon == null) {
                                    Spacer(modifier = Modifier.size(42.dp))
                                    if (!listIcons.containsKey(packageName)) {
                                        listIcons[packageName] = null
                                        scope.launch {
                                            listIcons[packageName] = drawablePainter(packageManager.getApplicationIcon(packageName))
                                        }
                                    }
                                } else {
                                    Image(
                                        painter = icon,
                                        contentDescription = packageName,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.size(12.dp, 0.dp))
                                Column {
                                    Text(it.first, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(packageName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    private fun drawablePainter(drawable: Drawable): Painter {
        return when (drawable) {
            is BitmapDrawable -> BitmapPainter(drawable.bitmap.asImageBitmap())
            is ColorDrawable -> ColorPainter(Color(drawable.color))
            // Since the DrawablePainter will be remembered and it implements RememberObserver, it
            // will receive the necessary events
            else -> DrawablePainter(drawable.mutate())
        }
    }
}