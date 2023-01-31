package dev.sora.protohax.ui.components

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import dev.sora.protohax.R
import dev.sora.protohax.relay.AccountManager
import dev.sora.protohax.ui.activities.AppPickerActivity
import dev.sora.protohax.ui.navigation.PHaxTopLevelDestination
import dev.sora.protohax.ui.navigation.TOP_LEVEL_DESTINATIONS
import dev.sora.protohax.util.ContextUtils.getApplicationName
import dev.sora.protohax.util.ContextUtils.getPackageInfo
import dev.sora.relay.game.GameSession

@Composable
fun CardLoginAlert(
    navigateToTopLevelDestination: (PHaxTopLevelDestination) -> Unit
) {
    if (AccountManager.currentAccount == null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp, 10.dp).clickable {
                    navigateToTopLevelDestination(TOP_LEVEL_DESTINATIONS.find { it.iconTextId == R.string.tab_accounts } ?: return@clickable)
                },
        ) {
            Row(
                modifier = Modifier.padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(12.dp))
                Text(
                    stringResource(R.string.dashboard_no_account_selected),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun CardCurrentApplication(
    applicationSelected: MutableState<String>,
    pickAppActivityLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val ctx = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp, 10.dp)
            .clickable {
                pickAppActivityLauncher.launch(Intent(ctx, AppPickerActivity::class.java))
            },
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Text(
                stringResource(if (applicationSelected.value.isEmpty()) R.string.dashboard_select_application else R.string.dashboard_selected_application),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.size(0.dp, 8.dp))
            if (applicationSelected.value.isEmpty()) {
                Text(
                    stringResource(R.string.dashboard_no_application),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                val lineHeight = 14.sp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberDrawablePainter(ctx.packageManager.getApplicationIcon(applicationSelected.value)),
                        contentDescription = applicationSelected.value,
                        modifier = Modifier
                            .size(with(LocalDensity.current) { lineHeight.toDp() } + 6.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.size(6.dp, 0.dp))
                    Text(
                        "${ctx.packageManager.getApplicationName(applicationSelected.value)} (${applicationSelected.value})",
                        fontSize = lineHeight,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.size(0.dp, 6.dp))
                Text(
                    stringResource(R.string.dashboard_current_version, ctx.packageManager.getPackageInfo(applicationSelected.value).versionName),
                    fontSize = lineHeight,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    stringResource(R.string.dashboard_recommended_version, GameSession.RECOMMENDED_VERSION),
                    fontSize = lineHeight,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}