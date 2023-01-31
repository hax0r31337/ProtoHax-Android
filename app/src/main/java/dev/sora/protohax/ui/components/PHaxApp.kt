package dev.sora.protohax.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dev.sora.protohax.ui.components.screen.AccountsScreen
import dev.sora.protohax.ui.components.screen.ConfigScreen
import dev.sora.protohax.ui.components.screen.DashboardScreen
import dev.sora.protohax.ui.components.screen.LogsScreen
import dev.sora.protohax.ui.navigation.*
import dev.sora.protohax.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@Composable
fun PHaxApp(
    windowSize: WindowSizeClass,
    displayFeatures: List<DisplayFeature>
) {
    /**
     * This will help us select type of navigation and content type depending on window size and
     * fold state of the device.
     */
    val navigationType: NavigationType

    /**
     * We are using display's folding features to map the device postures a fold is in.
     * In the state of folding device If it's half fold in BookPosture we want to avoid content
     * at the crease/hinge
     */
    val foldingFeature = displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()

    val foldingDevicePosture = when {
        isBookPosture(foldingFeature) ->
            DevicePosture.BookPosture(foldingFeature.bounds)

        isSeparating(foldingFeature) ->
            DevicePosture.Separating(foldingFeature.bounds, foldingFeature.orientation)

        else -> DevicePosture.NormalPosture
    }

    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            navigationType = NavigationType.BOTTOM_NAVIGATION
        }
        WindowWidthSizeClass.Medium -> {
            navigationType = NavigationType.NAVIGATION_RAIL
        }
        WindowWidthSizeClass.Expanded -> {
            navigationType = if (foldingDevicePosture is DevicePosture.BookPosture) {
                NavigationType.NAVIGATION_RAIL
            } else {
                NavigationType.PERMANENT_NAVIGATION_DRAWER
            }
        }
        else -> {
            navigationType = NavigationType.BOTTOM_NAVIGATION
        }
    }

    /**
     * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
     * ergonomics and reachability depending upon the height of the device.
     */
    val navigationContentPosition = when (windowSize.heightSizeClass) {
        WindowHeightSizeClass.Compact -> {
            NavigationContentPosition.TOP
        }
        WindowHeightSizeClass.Medium,
        WindowHeightSizeClass.Expanded -> {
            NavigationContentPosition.CENTER
        }
        else -> {
            NavigationContentPosition.TOP
        }
    }

    PHaxNavigationWrapper(
        navigationType = navigationType,
        navigationContentPosition = navigationContentPosition
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalCoroutinesApi::class)
@Composable
private fun PHaxNavigationWrapper(
    navigationType: NavigationType,
    navigationContentPosition: NavigationContentPosition
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navController = rememberAnimatedNavController()
    val navigationActions = remember(navController) {
        PHaxNavigationActions(navController)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination =
        navBackStackEntry?.destination?.route ?: PHaxRoute.DASHBOARD
    val connectionState = connectionState()

    if (navigationType == NavigationType.PERMANENT_NAVIGATION_DRAWER) {
        PermanentNavigationDrawer(drawerContent = {
            PermanentNavigationDrawerContent(
                selectedDestination = selectedDestination,
                navigationContentPosition = navigationContentPosition,
                navigateToTopLevelDestination = navigationActions::navigateTo
            )
        }) {
            PHaxAppContent(
                navigationType = navigationType,
                navigationContentPosition = navigationContentPosition,
                navController = navController,
                selectedDestination = selectedDestination,
                navigateToTopLevelDestination = navigationActions::navigateTo,
                connectionState = connectionState
            )
        }
    } else if (navigationType != NavigationType.BOTTOM_NAVIGATION) {
        ModalNavigationDrawer(
            drawerContent = {
                ModalNavigationDrawerContent(
                    selectedDestination = selectedDestination,
                    navigationContentPosition = navigationContentPosition,
                    navigateToTopLevelDestination = navigationActions::navigateTo,
                    onDrawerClicked = {
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
            },
            drawerState = drawerState
        ) {
            PHaxAppContent(
                navigationType = navigationType,
                navigationContentPosition = navigationContentPosition,
                navController = navController,
                selectedDestination = selectedDestination,
                navigateToTopLevelDestination = navigationActions::navigateTo,
                connectionState = connectionState
            ) {
                scope.launch {
                    drawerState.open()
                }
            }
        }
    } else {
        PHaxAppContent(
            navigationType = navigationType,
            navigationContentPosition = navigationContentPosition,
            navController = navController,
            selectedDestination = selectedDestination,
            navigateToTopLevelDestination = navigationActions::navigateTo,
            connectionState = connectionState
        ) {
            scope.launch {
                drawerState.open()
            }
        }
    }
}

@Composable
fun PHaxAppContent(
    modifier: Modifier = Modifier,
    navigationType: NavigationType,
    navigationContentPosition: NavigationContentPosition,
    navController: NavHostController,
    selectedDestination: String,
    navigateToTopLevelDestination: (PHaxTopLevelDestination) -> Unit,
    connectionState: State<Boolean>,
    onDrawerClicked: () -> Unit = {}
) {
    Row(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = navigationType == NavigationType.NAVIGATION_RAIL) {
            PHaxNavigationRail(
                selectedDestination = selectedDestination,
                navigationContentPosition = navigationContentPosition,
                navigateToTopLevelDestination = navigateToTopLevelDestination,
                onDrawerClicked = onDrawerClicked
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.inverseOnSurface)
        ) {
            PHaxNavHost(
                navController = navController,
                navigationType = navigationType,
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
                connectionState = connectionState,
                navigateToTopLevelDestination = navigateToTopLevelDestination
            )
            AnimatedVisibility(visible = navigationType == NavigationType.BOTTOM_NAVIGATION) {
                PHaxBottomNavigationBar(
                    selectedDestination = selectedDestination,
                    navigateToTopLevelDestination = navigateToTopLevelDestination
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PHaxNavHost(
    navController: NavHostController,
    navigationType: NavigationType,
    modifier: Modifier = Modifier,
    connectionState: State<Boolean>,
    navigateToTopLevelDestination: (PHaxTopLevelDestination) -> Unit
) {
    AnimatedNavHost(
        modifier = modifier,
        navController = navController,
        startDestination = PHaxRoute.DASHBOARD,
        enterTransition = {
            scaleIn(initialScale = 0.8f) + fadeIn()
        },
        exitTransition = {
            fadeOut()
        },
    ) {
        composable(PHaxRoute.DASHBOARD) {
            DashboardScreen(navigationType, connectionState, navigateToTopLevelDestination)
        }
        composable(PHaxRoute.CONFIG) {
            ConfigScreen(navigationType)
        }
        composable(PHaxRoute.ACCOUNTS) {
            AccountsScreen(navigationType)
        }
        composable(PHaxRoute.LOGS) {
            LogsScreen(navigationType)
        }
    }
}
