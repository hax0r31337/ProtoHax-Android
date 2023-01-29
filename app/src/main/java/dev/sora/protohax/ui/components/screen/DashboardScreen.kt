package dev.sora.protohax.ui.components.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sora.protohax.R
import dev.sora.protohax.ui.components.PHaxAppBar
import dev.sora.protohax.util.NavigationType

@Composable
fun DashboardScreen(navigationType: NavigationType) {
    PHaxAppBar(
        title = stringResource(id = R.string.app_name),
        navigationType = navigationType,
    ) {
        EmptyComingSoon()
    }
}