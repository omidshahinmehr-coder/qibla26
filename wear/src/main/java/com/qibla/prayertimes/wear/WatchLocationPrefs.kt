package com.qibla.prayertimes.wear

import android.content.Context

/**
 * Remembers whether the watch should use its own live GPS fix, or a fixed city the user picked
 * from the default catalog — so the choice survives closing and reopening the app.
 */
object WatchLocationPrefs {
    private const val PREFS_NAME = "qibla_watch_location_prefs"
    private const val KEY_MODE = "mode" // "live" or "city"
    private const val KEY_CITY_NAME = "city_name"
    private const val KEY_CITY_LAT = "city_lat"
    private const val KEY_CITY_LON = "city_lon"
    private const val KEY_LAST_LAT = "last_known_lat"
    private const val KEY_LAST_LON = "last_known_lon"

    sealed class Choice {
        object Live : Choice()
        data class City(val name: String, val lat: Double, val lon: Double) : Choice()
    }

    fun get(context: Context): Choice {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_MODE, "live") == "city") {
            val name = prefs.getString(KEY_CITY_NAME, null)
            val lat = prefs.getFloat(KEY_CITY_LAT, Float.NaN).toDouble()
            val lon = prefs.getFloat(KEY_CITY_LON, Float.NaN).toDouble()
            if (name != null && !lat.isNaN() && !lon.isNaN()) {
                return Choice.City(name, lat, lon)
            }
        }
        return Choice.Live
    }

    fun setLive(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, "live").apply()
    }

    fun setCity(context: Context, name: String, lat: Double, lon: Double) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, "city")
            .putString(KEY_CITY_NAME, name)
            .putFloat(KEY_CITY_LAT, lat.toFloat())
            .putFloat(KEY_CITY_LON, lon.toFloat())
            .apply()
    }

    /**
     * Caches the most recent coordinates actually used to compute prayer times, regardless of
     * mode (a live GPS fix, or a picked city) — so [com.qibla.prayertimes.wear.alarm.WatchBootReceiver]
     * has *something* to reschedule today's alarms from right after a reboot, before the app
     * has been reopened to get a fresh GPS fix.
     */
    fun setLastKnownCoordinates(context: Context, lat: Double, lon: Double) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_LAST_LAT, lat.toFloat())
            .putFloat(KEY_LAST_LON, lon.toFloat())
            .apply()
    }

    /** The last coordinates cached by [setLastKnownCoordinates], if any. */
    fun getLastKnownCoordinates(context: Context): Pair<Double, Double>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lat = prefs.getFloat(KEY_LAST_LAT, Float.NaN).toDouble()
        val lon = prefs.getFloat(KEY_LAST_LON, Float.NaN).toDouble()
        return if (!lat.isNaN() && !lon.isNaN()) lat to lon else null
    }
}
