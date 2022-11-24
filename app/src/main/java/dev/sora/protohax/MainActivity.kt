package dev.sora.protohax

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.megatronking.netbare.NetBare
import com.github.megatronking.netbare.NetBareConfig
import com.github.megatronking.netbare.NetBareListener
import com.github.megatronking.netbare.ip.IpAddress
import dev.sora.protohax.ui.theme.ProtoHaxTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {

    private val configBuilder: NetBareConfig.Builder
        get() = NetBareConfig.Builder()
        .setMtu(4096)
        .setAddress(IpAddress("10.1.10.1", 32))
        .setSession("ProtoHax   ")
        .addRoute(IpAddress("0.0.0.0", 0))

    private val requestVpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//        if (it.resultCode == RESULT_OK) {
//            startV2Ray()
//        }
    }

    @ExperimentalCoroutinesApi
    fun observeConnectivityAsFlow() = callbackFlow {
        val nb = NetBare.get()
        val callback = NetworkCallback { state -> trySend(state) }
        nb.registerNetBareListener(callback)

        // Set current state
        trySend(nb.isActive)

        // Remove callback when not used
        awaitClose {
            nb.unregisterNetBareListener(callback)
        }
    }

    fun NetworkCallback(callback: (Boolean) -> Unit): NetBareListener {
        return object : NetBareListener {
            override fun onServiceStarted() {
                callback(true)
            }

            override fun onServiceStopped() {
                callback(false)
            }
        }
    }

    @ExperimentalCoroutinesApi
    @Composable
    fun connectivityState(): State<Boolean> {
        // Creates a State<ConnectionState> with current connectivity state as initial value
        return produceState(initialValue = NetBare.get().isActive) {
            // In a coroutine, can make suspend calls
            observeConnectivityAsFlow().collect { value = it }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProtoHaxTheme {
                View()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Composable
    private fun View() {
        val targetPkgName = rememberSaveable { mutableStateOf("com.mojang.minecraftpe") }
        val ctx = LocalContext.current
        val connection by connectivityState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(R.string.app_name))
                    },
                    elevation = 10.dp
                )
            }, floatingActionButton = {
                FloatingActionButton(onClick = {
                    try {
                        if (!NetBare.get().isActive) {
                            val intent = NetBare.get().prepare()
                            if (intent == null) {
                                NetBare.get().start(configBuilder
                                    .addAllowedApplication(targetPkgName.value)
                                    .build())
                                Toast.makeText(ctx, getString(R.string.start_proxy_toast, targetPkgName.value), Toast.LENGTH_LONG).show()
                            } else {
                                requestVpnPermission.launch(intent)
                            }
                        } else {
                            NetBare.get().stop()
                            Toast.makeText(ctx, getString(R.string.stop_proxy_toast), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Throwable) {
                        Log.e("ProtoHax", "mitm", e)
                    }
                },
                    backgroundColor = if (connection) Color(0xFF4CAF50) else Color.Gray) {
                    Icon(painter = painterResource(R.drawable.notification_icon), "connIcon", tint = Color(0xFFDDDDDD))
                }
            },
            isFloatingActionButtonDocked = true,
            bottomBar = {
                BottomAppBar {
                    Text(text = stringResource(if(connection) R.string.connected else R.string.not_connected),
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