package com.qibla.prayertimes.wear

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.qibla.prayertimes.model.DEFAULT_CITY_CATALOG
import java.util.Locale

/**
 * Which of the catalog's languages to show, based on the watch's system language (the watch
 * has no in-app language setting of its own, unlike the phone).
 */
private fun systemCatalogLanguage(): String = when (Locale.getDefault().language) {
    "fa" -> "fa"
    "ar" -> "ar"
    else -> "en"
}

@Composable
fun WatchCityPicker(onBack: () -> Unit, onPickLive: () -> Unit, onPickCity: (name: String, lat: Double, lon: Double) -> Unit) {
    val language = remember { systemCatalogLanguage() }
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        backRow(onBack)
        item {
            Chip(
                onClick = onPickLive,
                label = { Text(stringResource(R.string.wear_my_location)) },
                icon = { Icon(Icons.Filled.MyLocation, contentDescription = null) },
                colors = ChipDefaults.chipColors(backgroundColor = WatchBrass)
            )
        }
        items(DEFAULT_CITY_CATALOG) { entry ->
            val name = entry.nameFor(language)
            Chip(
                onClick = { onPickCity(name, entry.lat, entry.lon) },
                label = { Text(name) },
                colors = ChipDefaults.chipColors(backgroundColor = WatchNightMid, contentColor = WatchAmberText)
            )
        }
    }
}
