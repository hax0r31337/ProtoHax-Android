package dev.sora.protohax.ui.overlay.menu.tabs

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sora.protohax.R

@Composable
fun BoxScope.ConfigTab() {
	Card(
		modifier = Modifier.align(Alignment.BottomEnd),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
		shape = RoundedCornerShape(15.dp, 0.dp, 0.dp, 0.dp)
	) {
		IconButton(onClick = {  }) {
			Icon(
				imageVector = Icons.Default.Add,
				contentDescription = stringResource(id = R.string.config_create)
			)
		}
	}
}
