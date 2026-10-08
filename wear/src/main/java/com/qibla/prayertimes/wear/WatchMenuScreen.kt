package com.qibla.prayertimes.wear

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text

@Composable
fun WatchMenuScreen(
    onBack: () -> Unit,
    onOpenPrayerTimes: () -> Unit,
    onOpenMethod: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenLocation: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
        backRow(onBack)
        item {
            Chip(
                onClick = onOpenPrayerTimes,
                label = { Text(stringResource(R.string.wear_open_prayer_times)) },
                colors = ChipDefaults.chipColors(backgroundColor = WatchNightMid, contentColor = WatchAmberText)
            )
        }
        item {
            Chip(
                onClick = onOpenMethod,
                label = { Text(stringResource(R.string.wear_method_title)) },
                colors = ChipDefaults.chipColors(backgroundColor = WatchNightMid, contentColor = WatchAmberText)
            )
        }
        item {
            Chip(
                onClick = onOpenAlarms,
                label = { Text(stringResource(R.string.wear_open_alarm_settings)) },
                colors = ChipDefaults.chipColors(backgroundColor = WatchNightMid, contentColor = WatchAmberText)
            )
        }
        item {
            Chip(
                onClick = onOpenLocation,
                label = { Text(stringResource(R.string.wear_change_location)) },
                colors = ChipDefaults.chipColors(backgroundColor = WatchNightMid, contentColor = WatchAmberText)
            )
        }
    }
}
