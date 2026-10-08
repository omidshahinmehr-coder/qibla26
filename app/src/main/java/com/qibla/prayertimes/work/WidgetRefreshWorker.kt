package com.qibla.prayertimes.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.qibla.prayertimes.alarm.AlarmScheduler
import com.qibla.prayertimes.data.CityStore
import com.qibla.prayertimes.data.PrayerMethodPrefs
import com.qibla.prayertimes.data.PrayerTimesRepository
import com.qibla.prayertimes.data.PrayerTimesState
import com.qibla.prayertimes.data.WidgetDataStore
import com.qibla.prayertimes.model.defaultCities
import com.qibla.prayertimes.util.HijriCorrectionPrefs
import com.qibla.prayertimes.widget.QiblaWidgetUpdater
import java.util.concurrent.TimeUnit

/**
 * Redraws the home screen widgets every 15 minutes (the minimum interval WorkManager allows
 * for periodic work) so the "next prayer" countdown stays reasonably current.
 *
 * Also self-heals the day rollover: [PrayerTimesWorker] only truly re-fetches once a day, at
 * whatever time it first happened to be scheduled (not necessarily midnight) — so relying on
 * it alone could leave the widget showing yesterday's date/timings for hours after midnight,
 * until the app was opened. Since this worker already runs every 15 minutes anyway, it checks
 * [WidgetDataStore.isFreshToday] on every tick and does a full re-fetch (same as
 * [PrayerTimesWorker]) whenever the cached data has gone stale — so the widget corrects itself
 * within at most 15 minutes of midnight, with no need to open the app.
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = WidgetDataStore(applicationContext)

        if (!store.isFreshToday()) {
            val city = CityStore(applicationContext).loadSelectedCity() ?: defaultCities(applicationContext).first()
            val method = PrayerMethodPrefs.get(applicationContext)
            val correctionDays = HijriCorrectionPrefs.get(applicationContext)
            val state = PrayerTimesRepository().fetchToday(city.lat, city.lon, method, correctionDays)
            if (state is PrayerTimesState.Success) {
                store.save(city.name, state.result.timings, state.result.hijri, state.result.isOffline)
                AlarmScheduler.scheduleToday(applicationContext, state.result.timings)
            }
            // A failed fetch here (state is Error) leaves yesterday's cache in place rather than
            // clearing it — still-stale-but-present data is more useful on the widget than blank,
            // and the next 15-minute tick will simply try again.
        }

        QiblaWidgetUpdater.requestUpdate(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "qibla_widget_refresh"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
