package com.qibla.prayertimes.wear.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object WatchAlarmScheduler {

    const val EXTRA_PRAYER = "extra_prayer"

    /**
     * Schedules today's adhan alarms from a freshly-computed timings map (HH:mm strings).
     * Prayers disabled in [WatchAlarmPrefs], or whose time has already passed today, are
     * skipped. Always cancels first so re-computing (new location, new method, or a new day)
     * never leaves a stale alarm behind.
     */
    fun scheduleToday(context: Context, timings: Map<String, String>) {
        val prefs = WatchAlarmPrefs(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = Calendar.getInstance()

        for (prayer in WatchAdhanPrayer.entries) {
            cancel(context, prayer)
            val timeStr = timings[prayer.timingsKey]
            val triggerAt = timeStr?.let { parseToday(it) }
            if (prefs.isEnabled(prayer) && triggerAt != null && !triggerAt.before(now)) {
                schedule(context, alarmManager, prayer, triggerAt)
            }
        }
    }

    private fun schedule(context: Context, alarmManager: AlarmManager, prayer: WatchAdhanPrayer, triggerAt: Calendar) {
        val intent = Intent(context, WatchAdhanAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER, prayer.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, prayer.requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt.timeInMillis, pendingIntent)
                }
            }
            else -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt.timeInMillis, pendingIntent)
            }
        }
    }

    fun cancel(context: Context, prayer: WatchAdhanPrayer) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WatchAdhanAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, prayer.requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAll(context: Context) {
        WatchAdhanPrayer.entries.forEach { cancel(context, it) }
    }

    private fun parseToday(hhmm: String): Calendar? {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            val parsed = sdf.parse(hhmm) ?: return null
            val parsedCal = Calendar.getInstance().apply { time = parsed }
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            null
        }
    }
}
