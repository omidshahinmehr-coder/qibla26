package com.qibla.prayertimes.wear.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class WatchAdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(WatchAlarmScheduler.EXTRA_PRAYER) ?: return

        val serviceIntent = Intent(context, WatchAdhanPlaybackService::class.java).apply {
            putExtra(WatchAlarmScheduler.EXTRA_PRAYER, prayerName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
