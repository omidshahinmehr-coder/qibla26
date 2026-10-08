package com.qibla.prayertimes.wear.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qibla.prayertimes.data.OfflinePrayerCalculator
import com.qibla.prayertimes.data.PrayerMethodPrefs
import com.qibla.prayertimes.wear.WatchLocationPrefs

/**
 * Reschedules today's adhan alarms right after the watch reboots (AlarmManager entries don't
 * survive a reboot). Uses whatever coordinates were last cached — see
 * [WatchLocationPrefs.setLastKnownCoordinates] — since there's no UI open yet to get a fresh
 * GPS fix. Always computed offline (no network dependency at boot time).
 */
class WatchBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val (lat, lon) = WatchLocationPrefs.getLastKnownCoordinates(context) ?: return
        val method = PrayerMethodPrefs.get(context)
        val timings = OfflinePrayerCalculator.computeToday(lat, lon, method)
        WatchAlarmScheduler.scheduleToday(context, timings)
    }
}
