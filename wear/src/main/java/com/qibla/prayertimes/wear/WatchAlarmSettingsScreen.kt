package com.qibla.prayertimes.wear

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.qibla.prayertimes.wear.alarm.WatchAdhanPrayer
import com.qibla.prayertimes.wear.alarm.WatchAlarmPrefs
import com.qibla.prayertimes.wear.alarm.WatchAlarmScheduler

/**
 * [currentTimings] is used to (re)schedule alarms immediately when a toggle changes, so the
 * effect is immediate rather than waiting for the next time prayer times are recomputed.
 */
@Composable
fun WatchAlarmSettingsScreen(currentTimings: Map<String, String>?, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { WatchAlarmPrefs(context) }
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
        backRow(onBack)
        item {
            ListHeader {
                Text(stringResource(R.string.wear_alarm_settings_title), color = WatchAmberText)
            }
        }
        items(WatchAdhanPrayer.entries) { prayer ->
            var enabled by remember(prayer) { mutableStateOf(prefs.isEnabled(prayer)) }
            ToggleChip(
                checked = enabled,
                onCheckedChange = { checked ->
                    enabled = checked
                    prefs.setEnabled(prayer, checked)
                    if (currentTimings != null) {
                        WatchAlarmScheduler.scheduleToday(context, currentTimings)
                    }
                },
                label = { Text(prayer.label(context)) },
                toggleControl = {
                    Switch(checked = enabled)
                },
                colors = ToggleChipDefaults.toggleChipColors(
                    checkedStartBackgroundColor = WatchBrass,
                    uncheckedStartBackgroundColor = WatchNightMid
                )
            )
        }
    }
}
