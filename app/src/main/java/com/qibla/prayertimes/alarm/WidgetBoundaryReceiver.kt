package com.qibla.prayertimes.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qibla.prayertimes.widget.QiblaWidgetUpdater

/**
 * Fires at each of the five prayer boundaries (Fajr/Dhuhr/Asr/Maghrib/Isha) — always, regardless
 * of whether the user has that prayer's adhan sound enabled — and asks the widget to redraw.
 *
 * Why this needs to be separate from the adhan alarm: the widget's countdown timer digits are a
 * native Android `Chronometer` (via RemoteViews), which ticks on the system clock by itself with
 * no help from the app — but the *label* naming which prayer it's counting down to, and the
 * Chronometer's target, are plain values baked in the last time the widget was redrawn. Without
 * this receiver, that redraw only happens every ~15 minutes (WidgetRefreshWorker) or when the
 * app is opened — so for up to 15 minutes after a prayer time passes, the widget would show a
 * stale prayer name while the timer (having hit its old target) starts counting up instead of
 * down. Scheduling this exactly at each prayer's clock time keeps the transition instant. It's
 * intentionally independent of [AlarmPrefs.isEnabled] — the widget must stay correct even for a
 * prayer the user never wanted a sound for.
 */
class WidgetBoundaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        QiblaWidgetUpdater.requestUpdate(context)
    }
}
