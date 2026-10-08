package com.qibla.prayertimes.wear.alarm

import android.content.Context
import android.net.Uri
import com.qibla.prayertimes.wear.R

/**
 * Whether each prayer's adhan alarm is on, on the watch. Unlike the phone app, the watch
 * always uses the single bundled adhan sound and has no separate pre-adhan reminder — the
 * watch screen is small and this keeps the settings screen to one simple toggle list.
 */
class WatchAlarmPrefs(private val context: Context) {
    private val prefs = context.getSharedPreferences("qibla_watch_alarm_prefs", Context.MODE_PRIVATE)

    fun isEnabled(prayer: WatchAdhanPrayer): Boolean =
        prefs.getBoolean(enabledKey(prayer), false)

    fun setEnabled(prayer: WatchAdhanPrayer, enabled: Boolean) {
        prefs.edit().putBoolean(enabledKey(prayer), enabled).apply()
    }

    /** The one bundled adhan sound — `res/raw/adhan_default.mp3`. */
    fun soundUri(): Uri = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_default}")

    private fun enabledKey(prayer: WatchAdhanPrayer) = "enabled_${prayer.name}"
}
