package dev.sora.protohax.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import dev.sora.protohax.relay.service.AppService
import dev.sora.protohax.relay.service.ServiceListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow


@ExperimentalCoroutinesApi
@Composable
fun connectionState(): State<Boolean> {
    // Creates a State<ConnectionState> with current connectivity state as initial value
    return produceState(initialValue = AppService.isActive) {
        // In a coroutine, can make suspend calls
        observeConnectionAsFlow().collect { value = it }
    }
}

@ExperimentalCoroutinesApi
fun observeConnectionAsFlow() = callbackFlow {
    val listener = object : ServiceListener {
        override fun onServiceStarted() {
            trySend(true)
        }

        override fun onServiceStopped() {
            trySend(false)
        }
    }
    AppService.addListener(listener)

    // Set current state
    trySend(AppService.isActive)

    // Remove callback when not used
    awaitClose {
        AppService.removeListener(listener)
    }
}