package com.qibla.prayertimes.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AlarmScheduler {

    /**
     * Schedules today's adhan alarms (and any enabled pre-adhan reminders) from a
     * freshly-fetched timings map (HH:mm strings, as returned by
     * [com.qibla.prayertimes.data.PrayerTimesRepository]). Prayers that are disabled in
     * [AlarmPrefs], or whose time has already passed today, are skipped for the adhan sound
     * itself — but a separate widget-boundary alarm (see [WidgetBoundaryReceiver]) is always
     * scheduled for every prayer whose time hasn't passed yet, regardless of that toggle.
     */
    fun scheduleToday(context: Context, timings: Map<String, String>) {
        val prefs = AlarmPrefs(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = Calendar.getInstance()

        for (prayer in AdhanPrayer.entries) {
            cancel(context, prayer)
            val timeStr = timings[prayer.timingsKey]
            val triggerAt = timeStr?.let { parseToday(it) }

            if (prefs.isEnabled(prayer) && triggerAt != null && !triggerAt.before(now)) {
                schedule(context, alarmManager, AdhanAlarmReceiver::class.java, prayer.requestCode, triggerAt) {
                    putExtra(EXTRA_PRAYER, prayer.name)
                    putExtra(EXTRA_IS_REMINDER, false)
                }
            }

            val reminderMinutes = prefs.reminderMinutes(prayer)
            if (reminderMinutes > 0 && triggerAt != null) {
                val reminderAt = (triggerAt.clone() as Calendar).apply { add(Calendar.MINUTE, -reminderMinutes) }
                if (!reminderAt.before(now)) {
                    schedule(context, alarmManager, AdhanAlarmReceiver::class.java, prayer.reminderRequestCode, reminderAt) {
                        putExtra(EXTRA_PRAYER, prayer.name)
                        putExtra(EXTRA_IS_REMINDER, true)
                    }
                }
            }

            if (triggerAt != null && !triggerAt.before(now)) {
                schedule(context, alarmManager, WidgetBoundaryReceiver::class.java, prayer.widgetBoundaryRequestCode, triggerAt) {}
            }
        }

        // Sunrise/Sunset aren't adhan-eligible (no AdhanPrayer entry — they're informational,
        // not a prayer), so the loop above never touches them. But the widget's own countdown
        // still treats them as valid "next" targets (see QiblaWidget.kt's nextPrayerCountdown),
        // so without their own widget-boundary alarm here, the widget would stay stale for up
        // to 15 minutes after each one passes — instead of updating instantly like the five
        // prayers do.
        scheduleWidgetBoundaryOnly(context, alarmManager, "Sunrise", WIDGET_BOUNDARY_SUNRISE_CODE, timings, now)
        scheduleWidgetBoundaryOnly(context, alarmManager, "Sunset", WIDGET_BOUNDARY_SUNSET_CODE, timings, now)
    }

    private fun scheduleWidgetBoundaryOnly(
        context: Context,
        alarmManager: AlarmManager,
        timingsKey: String,
        requestCode: Int,
        timings: Map<String, String>,
        now: Calendar
    ) {
        cancelWidgetBoundary(context, requestCode)
        val triggerAt = timings[timingsKey]?.let { parseToday(it) }
        if (triggerAt != null && !triggerAt.before(now)) {
            schedule(context, alarmManager, WidgetBoundaryReceiver::class.java, requestCode, triggerAt) {}
        }
    }

    private fun cancelWidgetBoundary(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetBoundaryReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun schedule(
        context: Context,
        alarmManager: AlarmManager,
        receiver: Class<*>,
        requestCode: Int,
        triggerAt: Calendar,
        putExtras: Intent.() -> Unit
    ) {
        val intent = Intent(context, receiver).apply(putExtras)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAt.timeInMillis, pendingIntent
                    )
                } else {
                    // Fall back to an inexact alarm if the user hasn't granted the exact-alarm permission.
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt.timeInMillis, pendingIntent)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt.timeInMillis, pendingIntent)
            }
            else -> {
                // API 21-22 (Android 5.0/5.1): setExactAndAllowWhileIdle doesn't exist yet.
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt.timeInMillis, pendingIntent)
            }
        }
    }

    fun cancel(context: Context, prayer: AdhanPrayer) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (code in listOf(prayer.requestCode, prayer.reminderRequestCode)) {
            val intent = Intent(context, AdhanAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        run {
            val intent = Intent(context, WidgetBoundaryReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, prayer.widgetBoundaryRequestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun cancelAll(context: Context) {
        AdhanPrayer.entries.forEach { cancel(context, it) }
        cancelWidgetBoundary(context, WIDGET_BOUNDARY_SUNRISE_CODE)
        cancelWidgetBoundary(context, WIDGET_BOUNDARY_SUNSET_CODE)
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

    const val EXTRA_PRAYER = "extra_prayer"
    const val EXTRA_IS_REMINDER = "extra_is_reminder"

    // Distinct from AdhanPrayer's own request-code ranges (101-105 / 601-605 / 1001-1005).
    private const val WIDGET_BOUNDARY_SUNRISE_CODE = 1006
    private const val WIDGET_BOUNDARY_SUNSET_CODE = 1007
}
