package com.qibla.prayertimes.wear

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.qibla.prayertimes.data.OfflinePrayerCalculator
import com.qibla.prayertimes.data.PrayerCalculationMethod
import com.qibla.prayertimes.data.PrayerMethodPrefs
import com.qibla.prayertimes.data.QiblaMath
import com.qibla.prayertimes.sensor.rememberDeviceHeading
import com.qibla.prayertimes.wear.alarm.WatchAdhanPlaybackService
import com.qibla.prayertimes.wear.alarm.WatchAdhanPlaybackState
import com.qibla.prayertimes.wear.alarm.WatchAlarmScheduler
import kotlinx.coroutines.launch

/** State machine for the "live GPS" path only — irrelevant while a fixed city is chosen. */
private sealed class LiveGpsState {
    object PermissionNeeded : LiveGpsState()
    object Locating : LiveGpsState()
    object NoLocation : LiveGpsState()
    data class Ready(val lat: Double, val lon: Double) : LiveGpsState()
}

private sealed class Screen {
    object Main : Screen()
    object LocationPicker : Screen()
    object Menu : Screen()
    object PrayerTimes : Screen()
    object MethodPicker : Screen()
    object AlarmSettings : Screen()
}

/** Where the back button/gesture should go from each screen. */
private fun parentOf(screen: Screen): Screen = when (screen) {
    Screen.Main -> Screen.Main
    Screen.Menu -> Screen.Main
    Screen.LocationPicker -> Screen.Main
    Screen.PrayerTimes, Screen.MethodPicker, Screen.AlarmSettings -> Screen.Menu
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchQiblaApp()
        }
    }
}

@Composable
private fun WatchQiblaApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var locationChoice by remember { mutableStateOf(WatchLocationPrefs.get(context)) }
    var method by remember { mutableStateOf(PrayerMethodPrefs.get(context)) }

    var liveState by remember {
        mutableStateOf<LiveGpsState>(
            if (hasLocationPermission(context)) LiveGpsState.Locating else LiveGpsState.PermissionNeeded
        )
    }

    val permissionLauncher = rememberLocationPermissionLauncher { granted ->
        liveState = if (granted) LiveGpsState.Locating else LiveGpsState.PermissionNeeded
    }

    fun fetchLocation() {
        scope.launch {
            val location: Location? = WatchLocationHelper(context).getCurrentLocation()
            liveState = if (location != null) {
                LiveGpsState.Ready(location.latitude, location.longitude)
            } else {
                LiveGpsState.NoLocation
            }
        }
    }

    // Only chase a live GPS fix while the user is actually in "my location" mode.
    LaunchedEffect(liveState is LiveGpsState.Locating, locationChoice) {
        if (locationChoice is WatchLocationPrefs.Choice.Live && liveState is LiveGpsState.Locating) {
            fetchLocation()
        }
    }

    // The coordinates currently in effect, whichever mode we're in — null while still locating.
    val activeCoordinates: Pair<Double, Double>? = when (val choice = locationChoice) {
        is WatchLocationPrefs.Choice.City -> choice.lat to choice.lon
        is WatchLocationPrefs.Choice.Live -> (liveState as? LiveGpsState.Ready)?.let { it.lat to it.lon }
    }
    val activeLabel: String = when (val choice = locationChoice) {
        is WatchLocationPrefs.Choice.City -> choice.name
        is WatchLocationPrefs.Choice.Live -> stringResource(R.string.wear_my_location)
    }

    // Recompute prayer times (offline — no network needed on the watch) and reschedule today's
    // adhan alarms whenever the effective location or the calculation method changes. Also
    // caches the coordinates so WatchBootReceiver has something to reschedule from after a
    // reboot, before the app is reopened.
    var timings by remember { mutableStateOf<Map<String, String>?>(null) }
    LaunchedEffect(activeCoordinates, method) {
        val (lat, lon) = activeCoordinates ?: return@LaunchedEffect
        val computed = OfflinePrayerCalculator.computeToday(lat, lon, method)
        timings = computed
        WatchLocationPrefs.setLastKnownCoordinates(context, lat, lon)
        WatchAlarmScheduler.scheduleToday(context, computed)
    }

    fun openMenu() { screen = Screen.Menu }

    BackHandler(enabled = screen != Screen.Main) {
        screen = parentOf(screen)
    }

    val playingAdhan by WatchAdhanPlaybackState.currentlyPlaying.collectAsState()

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WatchNightDeep),
            contentAlignment = Alignment.Center
        ) {
            when (screen) {
                is Screen.LocationPicker -> WatchCityPicker(
                    onBack = { screen = parentOf(Screen.LocationPicker) },
                    onPickLive = {
                        WatchLocationPrefs.setLive(context)
                        locationChoice = WatchLocationPrefs.Choice.Live
                        liveState = if (hasLocationPermission(context)) LiveGpsState.Locating else LiveGpsState.PermissionNeeded
                        screen = Screen.Main
                    },
                    onPickCity = { name, lat, lon ->
                        WatchLocationPrefs.setCity(context, name, lat, lon)
                        locationChoice = WatchLocationPrefs.Choice.City(name, lat, lon)
                        screen = Screen.Main
                    }
                )
                is Screen.Menu -> WatchMenuScreen(
                    onBack = { screen = parentOf(Screen.Menu) },
                    onOpenPrayerTimes = { screen = Screen.PrayerTimes },
                    onOpenMethod = { screen = Screen.MethodPicker },
                    onOpenAlarms = { screen = Screen.AlarmSettings },
                    onOpenLocation = { screen = Screen.LocationPicker }
                )
                is Screen.PrayerTimes -> {
                    val t = timings
                    if (t != null) {
                        WatchPrayerTimesScreen(timings = t, method = method, onBack = { screen = parentOf(Screen.PrayerTimes) })
                    } else {
                        LocatingScreen()
                    }
                }
                is Screen.MethodPicker -> WatchMethodPicker(
                    current = method,
                    onBack = { screen = parentOf(Screen.MethodPicker) },
                    onPick = { picked ->
                        PrayerMethodPrefs.set(context, picked)
                        method = picked
                        screen = Screen.Main
                    }
                )
                is Screen.AlarmSettings -> WatchAlarmSettingsScreen(
                    currentTimings = timings,
                    onBack = { screen = parentOf(Screen.AlarmSettings) }
                )
                is Screen.Main -> when (val choice = locationChoice) {
                    is WatchLocationPrefs.Choice.City -> QiblaDialScreen(
                        lat = choice.lat,
                        lon = choice.lon,
                        label = choice.name,
                        onChangeLocation = { screen = Screen.LocationPicker },
                        onOpenMenu = ::openMenu
                    )
                    is WatchLocationPrefs.Choice.Live -> when (val s = liveState) {
                        is LiveGpsState.PermissionNeeded -> PermissionScreen {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                        is LiveGpsState.Locating -> LocatingScreen()
                        is LiveGpsState.NoLocation -> NoLocationScreen(
                            onRetry = { liveState = LiveGpsState.Locating },
                            onPickCityInstead = { screen = Screen.LocationPicker }
                        )
                        is LiveGpsState.Ready -> QiblaDialScreen(
                            lat = s.lat,
                            lon = s.lon,
                            label = activeLabel,
                            onChangeLocation = { screen = Screen.LocationPicker },
                            onOpenMenu = ::openMenu
                        )
                    }
                }
            }

            // Shown on top of whatever screen is up, whenever the adhan is actually playing —
            // a direct, unmissable way to stop it, instead of relying on the user finding the
            // system notification's stop action (which is easy to miss on a small watch screen).
            val playing = playingAdhan
            if (playing != null) {
                AdhanPlayingOverlay(prayerLabel = playing.label(context), onStop = { WatchAdhanPlaybackService.stopNow(context) })
            }
        }
    }
}

private fun hasLocationPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@Composable
private fun rememberLocationPermissionLauncher(onResult: (Boolean) -> Unit) =
    androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        onResult(results.values.any { it })
    }

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.wear_permission_needed),
            color = WatchAmberText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Chip(
            onClick = onRequest,
            label = { Text(stringResource(R.string.wear_grant_permission)) },
            colors = ChipDefaults.chipColors(backgroundColor = WatchBrass)
        )
    }
}

@Composable
private fun LocatingScreen() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(indicatorColor = WatchBrassLight)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.wear_finding_location), color = WatchAmberMuted, fontSize = 12.sp)
    }
}

@Composable
private fun NoLocationScreen(onRetry: () -> Unit, onPickCityInstead: () -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.wear_no_location),
            color = WatchAmberText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Chip(
            onClick = onRetry,
            label = { Text(stringResource(R.string.wear_retry)) },
            colors = ChipDefaults.chipColors(backgroundColor = WatchBrass)
        )
        Spacer(Modifier.height(8.dp))
        Chip(
            onClick = onPickCityInstead,
            label = { Text(stringResource(R.string.wear_change_location)) },
            colors = ChipDefaults.chipColors(backgroundColor = WatchNightMid, contentColor = WatchAmberText)
        )
    }
}

/**
 * A full-screen, high-contrast overlay shown whenever the adhan is playing — tapping anywhere
 * on it (or the button) stops the sound immediately. Deliberately covers the whole screen so
 * it can't be missed or accidentally dismissed by a stray tap on whatever was underneath.
 */
@Composable
private fun AdhanPlayingOverlay(prayerLabel: String, onStop: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WatchNightDeep)
            .clickable(onClick = onStop),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.adhan_time_title, prayerLabel),
                color = WatchAmberText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(14.dp))
            Chip(
                onClick = onStop,
                label = { Text(stringResource(R.string.wear_stop_adhan)) },
                colors = ChipDefaults.chipColors(backgroundColor = WatchBrass)
            )
        }
    }
}

/**
 * Same bearing/alignment math as the phone app's QiblaScreen: the needle is drawn relative to
 * the live device heading (so it always points at the Kaaba regardless of which way the watch
 * is facing), the tick ring counter-rotates to stay true-north-referenced, and "aligned" is
 * within 6° either side — identical thresholds to the phone.
 *
 * [label] shows either "My Location" (live GPS mode) or the chosen city's name, and is
 * tappable to open the city picker — see [onChangeLocation]. A small settings icon opens the
 * menu for prayer times / calculation method / adhan alarm — see [onOpenMenu].
 */
@Composable
private fun QiblaDialScreen(lat: Double, lon: Double, label: String, onChangeLocation: () -> Unit, onOpenMenu: () -> Unit) {
    val bearing = remember(lat, lon) { QiblaMath.bearing(lat, lon).toFloat() }
    val distanceKm = remember(lat, lon) { QiblaMath.distanceKm(lat, lon) }
    val deviceHeading = rememberDeviceHeading()

    val needleAngle = if (deviceHeading != null) (bearing - deviceHeading + 360f) % 360f else bearing
    val dialRotation = if (deviceHeading != null) (360f - deviceHeading) % 360f else 0f
    val isAligned = deviceHeading != null && minOf(needleAngle, 360f - needleAngle) < 6f

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            text = label,
            color = WatchBrassLight,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onChangeLocation)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        if (deviceHeading == null) {
            Text(
                text = stringResource(R.string.wear_no_compass),
                color = WatchAmberMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        WatchCompassDial(
            bearingDegrees = needleAngle,
            dialRotationDegrees = dialRotation,
            isAligned = isAligned,
            dialSize = 150.dp
        )
        Spacer(Modifier.height(6.dp))
        Text("${"%.0f".format(bearing)}°", color = WatchAmberText, fontSize = 15.sp)
        Text(
            text = stringResource(R.string.wear_distance_km, distanceKm),
            color = WatchAmberMuted,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(4.dp))
        Icon(
            Icons.Filled.Settings,
            contentDescription = stringResource(R.string.wear_open_alarm_settings),
            tint = WatchAmberMuted,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenMenu)
                .padding(6.dp)
        )
    }
}
