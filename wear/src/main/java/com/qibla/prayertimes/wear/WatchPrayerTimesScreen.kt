package com.qibla.prayertimes.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.qibla.prayertimes.data.PrayerCalculationMethod

private data class PrayerRow(val labelRes: Int, val timingsKey: String)

private val ROWS = listOf(
    PrayerRow(R.string.wear_prayer_fajr, "Fajr"),
    PrayerRow(R.string.wear_prayer_sunrise, "Sunrise"),
    PrayerRow(R.string.wear_prayer_dhuhr, "Dhuhr"),
    PrayerRow(R.string.wear_prayer_asr, "Asr"),
    PrayerRow(R.string.wear_prayer_sunset, "Sunset"),
    PrayerRow(R.string.wear_prayer_maghrib, "Maghrib"),
    PrayerRow(R.string.wear_prayer_isha, "Isha"),
    PrayerRow(R.string.wear_prayer_midnight, "Midnight")
)

@Composable
fun WatchPrayerTimesScreen(timings: Map<String, String>, method: PrayerCalculationMethod, onBack: () -> Unit) {
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
        backRow(onBack)
        item {
            ListHeader {
                Text(stringResource(R.string.wear_prayer_times_title), color = WatchAmberText, fontSize = 13.sp)
            }
        }
        items(ROWS) { row ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(row.labelRes), color = WatchAmberMuted, fontSize = 13.sp)
                Text(timings[row.timingsKey] ?: "--:--", color = WatchAmberText, fontSize = 13.sp, textAlign = TextAlign.End)
            }
        }
        item {
            val methodLabel = stringResource(
                if (method == PrayerCalculationMethod.JAFARI) R.string.wear_method_jafari else R.string.wear_method_tehran
            )
            Text(methodLabel, color = WatchBrassLight, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
