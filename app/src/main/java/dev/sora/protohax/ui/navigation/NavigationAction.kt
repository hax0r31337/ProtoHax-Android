package dev.sora.protohax.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import dev.sora.protohax.R


object PHaxRoute {
    const val DASHBOARD = "Dashboard"
    const val CONFIG = "Config"
    const val ACCOUNTS = "Accounts"
    const val LOGS = "Logs"
}

data class PHaxTopLevelDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val iconTextId: Int
)

class PHaxNavigationActions(private val navController: NavHostController) {

    fun navigateTo(destination: PHaxTopLevelDestination) {
        navController.navigate(destination.route) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }
}

val TOP_LEVEL_DESTINATIONS = listOf(
    PHaxTopLevelDestination(
        route = PHaxRoute.DASHBOARD,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        iconTextId = R.string.tab_dashboard
    ),
    PHaxTopLevelDestination(
        route = PHaxRoute.CONFIG,
        selectedIcon = Icons.Filled.Feed,
        unselectedIcon = Icons.Outlined.Feed,
        iconTextId = R.string.tab_configs
    ),
    PHaxTopLevelDestination(
        route = PHaxRoute.ACCOUNTS,
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People,
        iconTextId = R.string.tab_accounts
    ),
    PHaxTopLevelDestination(
        route = PHaxRoute.LOGS,
        selectedIcon = Icons.Filled.BugReport,
        unselectedIcon = Icons.Outlined.BugReport,
        iconTextId = R.string.tab_logs
    )
)
