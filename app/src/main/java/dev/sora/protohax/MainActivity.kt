package dev.sora.protohax

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sora.libmitm.MITM
import dev.sora.libmitm.MITMConfig
import dev.sora.protohax.ui.theme.ProtoHaxTheme

class MainActivity : ComponentActivity() {

    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//        if (it.resultCode == RESULT_OK) {
//            startV2Ray()
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProtoHaxTheme {
                View()
            }
        }
    }

    @Composable
    private fun View() {
        val connectState = remember { mutableStateOf(false) }
        val targetPkgName = rememberSaveable { mutableStateOf("com.mojang.minecraftpe") }
        val ctx = LocalContext.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(R.string.app_name))
                    },
//                    backgroundColor = MaterialTheme.colors.primary,
//                    contentColor = Color.White,
                    elevation = 10.dp
                )
            }, floatingActionButton = {
                FloatingActionButton(onClick = {
                    connectState.value = !connectState.value
                    try {
                        if (connectState.value) {
                            val intent = MITM.prepare()
                            if (intent == null) {
                                MITM.start(MITMConfig(allowedApps = arrayOf(targetPkgName.value)))
                                Toast.makeText(ctx, "Launching MITM proxy for ${targetPkgName.value}", Toast.LENGTH_LONG).show()
                            } else {
                                requestVpnPermission.launch(intent)
                                connectState.value = false
                            }
                        } else {
                            MITM.stop()
                            Toast.makeText(ctx, "MITM proxy stopped", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Throwable) {
                        Log.e("ProtoHax", "mitm", e)
                    }
                },
                    backgroundColor = if (connectState.value) Color(0xFF4CAF50) else Color.Gray) {
                    Icon(Icons.Default.Send, "connIcon", tint = Color(0xFFDDDDDD))
                }
            },
            isFloatingActionButtonDocked = true,
            bottomBar = {
                BottomAppBar {
                    Text(text = stringResource(if(connectState.value) R.string.connected else R.string.not_connected),
                        modifier = Modifier.padding(start = 10.dp))
                }
            }, content = { _ ->
                OutlinedTextField(
                    value = targetPkgName.value,
                    onValueChange = { targetPkgName.value = it },
                    label = { Text(stringResource(R.string.package_name)) }
                )
            }
        )
    }
}