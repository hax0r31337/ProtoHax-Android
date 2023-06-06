package dev.sora.protohax.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.sora.protohax.ui.theme.MyApplicationTheme
import dev.sora.relay.utils.logInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class ConfigureMenu(private val layoutWindow: LayoutWindow) {

	val hasDisplay: Boolean
		get() = menuLayout != null

	private var menuLayout: View? = null
	private val lifecycleOwner = MyLifecycleOwner()
	private val viewModelStore = ViewModelStore()
	private val recomposer: Recomposer
	private val composeScope: CoroutineScope

	private var firstRun = true

	var visibility = false
		set(value) {
			if (field != value) {
				listeners.forEach {
					it(value)
				}
				field = value
			}
		}

	private val listeners = mutableListOf<(Boolean) -> Unit>()

	init {
		lifecycleOwner.performRestore(null)

		val coroutineContext = AndroidUiDispatcher.CurrentThread
		composeScope = CoroutineScope(coroutineContext)
		recomposer = Recomposer(coroutineContext)
	}

	@OptIn(ExperimentalCoroutinesApi::class, ExperimentalAnimationApi::class)
	@SuppressLint("ClickableViewAccessibility")
	fun display(wm: WindowManager, ctx: Context) {
		val params = WindowManager.LayoutParams(
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
			PixelFormat.TRANSLUCENT
		)
		params.gravity = Gravity.CENTER or Gravity.CENTER
		params.x = 0
		params.y = 0

		val composeView = ComposeView(ctx)
		composeView.setContent {
			MyApplicationTheme(
				isActivity = false
			) {
				val displayState = displayState()
				AnimatedVisibility(
					visible = displayState.value,
					enter = fadeIn() + scaleIn(initialScale = 0.5f),
					exit = fadeOut() + scaleOut(targetScale = 0.5f)
				) {
					Content()
				}
			}
		}

		// Trick The ComposeView into thinking we are tracking lifecycle
		lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
		composeView.setViewTreeLifecycleOwner(lifecycleOwner)
		composeView.setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
			override val viewModelStore: ViewModelStore
				get() = this@ConfigureMenu.viewModelStore
		})
		composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
		composeView.compositionContext = recomposer
		if (firstRun) {
			composeScope.launch {
				recomposer.runRecomposeAndApplyChanges()
			}
			firstRun = false
		}
//		composeView.setOnTouchListener { _, event ->
//			if (event.action == MotionEvent.ACTION_OUTSIDE) {
//				visibility = false
//			}
//			false
//		}

		wm.addView(composeView, params)
		menuLayout = composeView
	}

	fun destroy(wm: WindowManager) {
		if (menuLayout == null) return
		wm.removeView(menuLayout)
		menuLayout = null
	}

	@Composable
	private fun Content() {
		Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)) {
			Text(text = "pad1")
			Card {
				Text(text = "pad2")
				val state = remember { mutableStateOf(false) }
				Button(onClick = { state.value = !state.value }) {
					Text(text = "${state.value}")
				}
			}
		}
	}

	@ExperimentalCoroutinesApi
	@Composable
	private fun displayState(): State<Boolean> {
		// Creates a State<ConnectionState> with current connectivity state as initial value
		return produceState(initialValue = visibility) {
			// In a coroutine, can make suspend calls
			observeConnectionAsFlow().collect {
				value = it
				logInfo("collect $value")
			}
		}
	}

	@ExperimentalCoroutinesApi
	private fun observeConnectionAsFlow() = callbackFlow {
		val listener: (Boolean) -> Unit = {
			logInfo("$it")
			trySend(it)
		}
		listeners.add(listener)

		// Set current state
		trySend(visibility)

		// Remove callback when not used
		awaitClose {
			listeners.remove(listener)
		}
	}
}
