package dev.sora.protohax.ui.overlay.menu

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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.isInvisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.createGraph
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.ui.components.screen.settings.Settings
import dev.sora.protohax.ui.navigation.PHaxRoute
import dev.sora.protohax.ui.overlay.MyLifecycleOwner
import dev.sora.protohax.ui.overlay.OverlayManager
import dev.sora.protohax.ui.overlay.menu.tabs.CheatCategoryTab
import dev.sora.protohax.ui.overlay.menu.tabs.ConfigTab
import dev.sora.protohax.ui.theme.MyApplicationTheme
import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.EventModuleToggle
import dev.sora.relay.game.event.EventHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class ConfigureMenu(private val overlayManager: OverlayManager) {

//	val hasDisplay: Boolean
//		get() = menuLayout != null

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
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
			PixelFormat.TRANSLUCENT
		)
		params.dimAmount = 0.5f
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
				val modules = moduleState()
				val expandModules = remember { mutableStateListOf<CheatModule>() }
				val navController = rememberAnimatedNavController()
				val navGraph = remember {
					val categories = CheatCategory.values()
					navController.createGraph(categories[0].choiceName) {
						composable(PHaxRoute.CONFIG) {
							Box(modifier = Modifier.fillMaxSize()) {
								ConfigTab()
							}
						}
						categories.forEach { category ->
							composable(category.choiceName) {
								Box(modifier = Modifier.fillMaxSize()) {
									CheatCategoryTab(category, modules, expandModules, overlayManager)
								}
							}
						}
					}
				}
				val scrollState = rememberScrollState()

				AnimatedVisibility(
					visible = displayState.value,
					enter = fadeIn() + scaleIn(initialScale = 0.5f),
					exit = fadeOut() + scaleOut(targetScale = 0.5f)
				) {
					val configuration = LocalConfiguration.current

					// rotate the menu due to the menu doesn't look well on portrait orientation
					Box(
						modifier = Modifier
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null
							) { visibility = false },
						contentAlignment = Alignment.Center
					) {
						Content(navController, navGraph, scrollState)
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

	private fun NavHostController.safeNavigate(dst: String) {
		navigate(dst) {
			// Pop up to the start destination of the graph to
			// avoid building up a large stack of destinations
			// on the back stack as users select items
			popUpTo(graph.findStartDestination().id) {
				saveState = true
			}
			// Avoid multiple copies of the same destination when
			// reselecting the same item
			launchSingleTop = true
			// Restore state when reselecting a previously selected item
			restoreState = true
		}
	}

	@OptIn(ExperimentalAnimationApi::class)
	@Composable
	private fun Content(navController: NavHostController, navGraph: NavGraph, scrollState: ScrollState) {
		Card(
			colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
			modifier = Modifier
				.fillMaxHeight(0.9f)
				.fillMaxWidth(0.8f)
				.clickable(false) {}
		) {
			Row {
				val categories = CheatCategory.values()
				val icons = mapOf(CheatCategory.COMBAT to painterResource(id = R.drawable.mdi_swords), CheatCategory.MOVEMENT to painterResource(id = R.drawable.mdi_sprint),
					CheatCategory.VISUAL to painterResource(id = R.drawable.mdi_view_in_ar), CheatCategory.MISC to painterResource(id = R.drawable.mdi_list_alt))

				val navBackStackEntry by navController.currentBackStackEntryAsState()
				val selectedDestination = navBackStackEntry?.destination?.route ?: categories[0].choiceName

				NavigationRail(
					containerColor = MaterialTheme.colorScheme.surface,
					modifier = Modifier
						.fillMaxHeight()
						.verticalScroll(scrollState)
				) {
					Spacer(Modifier.weight(1f))
					categories.forEach { item ->
						NavigationRailItem(
							icon = { Icon(painter = icons[item]!!, contentDescription = item.choiceName) },
							label = { Text(item.choiceName) },
							selected = selectedDestination == item.choiceName,
							onClick = { navController.safeNavigate(item.choiceName) }
						)
					}
					NavigationRailItem(
						icon = { Icon(Icons.Outlined.Feed, contentDescription = stringResource(id = R.string.tab_configs)) },
						label = { Text(stringResource(id = R.string.tab_configs)) },
						selected = selectedDestination == PHaxRoute.CONFIG,
						onClick = { navController.safeNavigate(PHaxRoute.CONFIG) }
					)
					Spacer(Modifier.weight(1f))
				}

				Card(modifier = Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
					val stack = categories.map { it.choiceName }.toMutableSet().also {
						it.add(PHaxRoute.CONFIG)
					}
					AnimatedNavHost(
						navController = navController,
						graph = navGraph,
						enterTransition = {
							if (stack.indexOf(this.initialState.destination.route ?: "") > stack.indexOf(this.targetState.destination.route ?: "")) {
								slideInVertically { -it / 2 } + fadeIn()
							} else {
								slideInVertically { it / 2 } + fadeIn()
							}
						},
						exitTransition = {
							if (stack.indexOf(this.initialState.destination.route ?: "") > stack.indexOf(this.targetState.destination.route ?: "")) {
								slideOutVertically { it / 2 } + fadeOut()
							} else {
								slideOutVertically { -it / 2 } + fadeOut()
							}
						}
					)
				}
			}
		}
	}

	@ExperimentalCoroutinesApi
	@Composable
	private fun moduleState(): SnapshotStateMap<CheatModule, Boolean> {
		val stateMap = remember { mutableStateMapOf<CheatModule, Boolean>() }

		LaunchedEffect(Unit) {
			callbackFlow {
				val listener = EventHook(EventModuleToggle::class.java, handler = {
					trySend(it)
				})

				MinecraftRelay.session.eventManager.register(listener)

				MinecraftRelay.moduleManager.modules.forEach { module ->
					stateMap[module] = module.state
				}

				// Remove callback when not used
				awaitClose {
					MinecraftRelay.session.eventManager.removeHandler(listener)
				}
			}.collect {
				if (it.module.canToggle) {
					stateMap[it.module] = it.targetState
				}
			}
		}

		return stateMap
	}

	@ExperimentalCoroutinesApi
	@Composable
	private fun displayState(wm: WindowManager, params: WindowManager.LayoutParams): State<Boolean> {
		// Creates a State<ConnectionState> with current connectivity state as initial value
		return produceState(initialValue = visibility) {
			// In a coroutine, can make suspend calls
			observeStateAsFlow().collect {
				value = it
				menuLayout?.let { l ->
					params.flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_DIM_BEHIND
					if (Settings.trustClicks.getValue(overlayManager.ctx)) {
						if (!it) {
							params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
						}
						l.isInvisible = false
					} else {
						if (!it) {
							params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						}
						l.isInvisible = !it
					}
					wm.updateViewLayout(l, params)
				}
				overlayManager.toggleRenderLayerViewVisibility(!it)
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
