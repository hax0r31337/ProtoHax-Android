package dev.sora.protohax.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.isInvisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.sora.protohax.R
import dev.sora.protohax.ui.components.screen.settings.Settings
import dev.sora.protohax.ui.theme.MyApplicationTheme
import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.utils.logInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class ConfigureMenu(private val overlayManager: OverlayManager) {

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
			WindowManager.LayoutParams.MATCH_PARENT,
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
				val displayState = displayState(wm, params)

				// states to remember
				val selectedItem = remember { mutableStateOf(0) }

				AnimatedVisibility(
					visible = displayState.value,
					enter = fadeIn() + scaleIn(initialScale = 0.5f),
					exit = fadeOut() + scaleOut(targetScale = 0.5f)
				) {
					val configuration = LocalConfiguration.current

					// rotate the menu due to the menu doesn't look well on portrait orientation
					Box(
						modifier = Modifier
							.height((configuration.screenHeightDp * 0.8).toInt().dp)
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null
							) { visibility = false },
						contentAlignment = Alignment.Center
					) {
						Content(selectedItem)
					}
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
		composeView.setOnTouchListener { _, event ->
			if (event.action == MotionEvent.ACTION_OUTSIDE) {
				visibility = false
			}
			false
		}

		wm.addView(composeView, params)
		menuLayout = composeView
	}

	fun destroy(wm: WindowManager) {
		if (menuLayout == null) return
		wm.removeView(menuLayout)
		menuLayout = null
	}

	@Composable
	private fun Content(selectedItem: MutableState<Int>) {
		Card(
			colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
			modifier = Modifier
				.fillMaxHeight()
				.fillMaxWidth(0.8f)
				.clickable(false) {}
		) {
			Row {
				val categories = CheatCategory.values()
				val icons = mapOf(CheatCategory.COMBAT to painterResource(id = R.drawable.mdi_swords), CheatCategory.MOVEMENT to painterResource(id = R.drawable.mdi_sprint),
					CheatCategory.VISUAL to painterResource(id = R.drawable.mdi_view_in_ar), CheatCategory.MISC to painterResource(id = R.drawable.mdi_list_alt))
				NavigationRail(containerColor = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxHeight()) {
					Spacer(Modifier.weight(1f))
					categories.forEachIndexed { index, item ->
						NavigationRailItem(
							icon = { Icon(painter = icons[item]!!, contentDescription = item.choiceName) },
							label = { Text(item.choiceName) },
							selected = selectedItem.value == index,
							onClick = { selectedItem.value = index },
							colors = NavigationRailItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary, selectedIconColor = MaterialTheme.colorScheme.onPrimary)
						)
					}
					NavigationRailItem(
						icon = { Icon(Icons.Filled.Note, contentDescription = stringResource(id = R.string.clickgui_configs)) },
						label = { Text(stringResource(id = R.string.clickgui_configs)) },
						selected = selectedItem.value == categories.size,
						onClick = { selectedItem.value = categories.size },
						colors = NavigationRailItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary, selectedIconColor = MaterialTheme.colorScheme.onPrimary)
					)
					Spacer(Modifier.weight(1f))
				}

				Card(modifier = Modifier.fillMaxSize()) {
					Text(text = "pad2")
				}
			}
		}
	}

	@ExperimentalCoroutinesApi
	@Composable
	private fun displayState(wm: WindowManager, params: WindowManager.LayoutParams): State<Boolean> {
		// Creates a State<ConnectionState> with current connectivity state as initial value
		return produceState(initialValue = visibility) {
			// In a coroutine, can make suspend calls
			observeStateAsFlow().collect {
				if (value != it) {
					value = it
					menuLayout?.let { l ->
						if (Settings.trustClicks.getValue(overlayManager.ctx)) {
							if (it) {
								params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
							} else {
								params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
							}
							wm.updateViewLayout(l, params)
						} else {
							l.isInvisible = !it
						}
					}
					overlayManager.toggleRenderLayerViewVisibility(!it)
				}
			}
		}
	}

	@ExperimentalCoroutinesApi
	private fun observeStateAsFlow() = callbackFlow {
		val listener: (Boolean) -> Unit = {
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