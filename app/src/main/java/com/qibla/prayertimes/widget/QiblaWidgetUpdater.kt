package com.qibla.prayertimes.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object QiblaWidgetUpdater {

    /** Fire-and-forget: asks every placed instance of both widgets (dark + light) to redraw from cached data. */
    fun requestUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                QiblaWidget().updateAll(context)
                LightQiblaWidget().updateAll(context)
            } catch (e: Exception) {
                // No widget currently placed on any home screen, or a transient Glance error — safe to ignore.
            }
        }
    }
}
