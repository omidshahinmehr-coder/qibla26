package com.qibla.prayertimes.wear

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Fetches a single current location fix — same best-available-provider, cached-then-fresh-fix
 * strategy as the phone app's `LocationHelper`, trimmed down since the watch only needs raw
 * coordinates for the qibla bearing calculation, not a reverse-geocoded city name.
 */
class WatchLocationHelper(private val context: Context) {

    /**
     * Bounded by a timeout — without one, a fresh GPS fix request that never completes (weak
     * signal, indoors, no clear sky view) would leave the caller waiting forever with no way to
     * reach the "couldn't get a location" screen and its retry/pick-a-city-instead options.
     */
    suspend fun getCurrentLocation(): Location? = withTimeoutOrNull(TIMEOUT_MS) { awaitLocationFix() }

    @SuppressLint("MissingPermission")
    private suspend fun awaitLocationFix(): Location? = suspendCancellableCoroutine { cont ->
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val loc = try { locationManager.getLastKnownLocation(provider) } catch (e: SecurityException) { null }
            if (loc != null && (bestLocation == null || loc.accuracy < bestLocation!!.accuracy)) {
                bestLocation = loc
            }
        }

        if (bestLocation != null) {
            cont.resume(bestLocation)
            return@suspendCancellableCoroutine
        }

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                if (cont.isActive) cont.resume(location)
            }
        }
        try {
            locationManager.requestSingleUpdate(provider, listener, null)
        } catch (e: SecurityException) {
            cont.resume(null)
        }
        cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
    }

    companion object {
        private const val TIMEOUT_MS = 25_000L
    }
}
