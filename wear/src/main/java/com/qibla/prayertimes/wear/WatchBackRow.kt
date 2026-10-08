package com.qibla.prayertimes.wear

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text

/**
 * A tappable "back" row, meant as the first item of a sub-screen's ScalingLazyColumn. Wear
 * devices don't reliably offer a hardware/gesture back on every model, so every sub-screen
 * needs an explicit, always-visible way back — this plus [androidx.activity.compose.BackHandler]
 * in MainActivity (which handles the system back button/gesture where it does exist) together
 * cover both cases.
 */
fun ScalingLazyListScope.backRow(onBack: () -> Unit) {
    item {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = WatchAmberMuted)
            Text(stringResource(R.string.wear_back), color = WatchAmberMuted, modifier = Modifier.padding(start = 6.dp))
        }
    }
}
