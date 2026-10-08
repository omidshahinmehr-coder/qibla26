package com.qibla.prayertimes.wear

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.qibla.prayertimes.data.PrayerCalculationMethod

@Composable
fun WatchMethodPicker(current: PrayerCalculationMethod, onBack: () -> Unit, onPick: (PrayerCalculationMethod) -> Unit) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
        backRow(onBack)
        item {
            ListHeader {
                Text(stringResource(R.string.wear_method_title), color = WatchAmberText)
            }
        }
        item {
            Chip(
                onClick = { onPick(PrayerCalculationMethod.TEHRAN) },
                label = { Text(stringResource(R.string.wear_method_tehran)) },
                icon = if (current == PrayerCalculationMethod.TEHRAN) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null,
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (current == PrayerCalculationMethod.TEHRAN) WatchBrass else WatchNightMid,
                    contentColor = WatchAmberText
                )
            )
        }
        item {
            Chip(
                onClick = { onPick(PrayerCalculationMethod.JAFARI) },
                label = { Text(stringResource(R.string.wear_method_jafari)) },
                icon = if (current == PrayerCalculationMethod.JAFARI) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null,
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (current == PrayerCalculationMethod.JAFARI) WatchBrass else WatchNightMid,
                    contentColor = WatchAmberText
                )
            )
        }
    }
}
