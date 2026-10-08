package com.qibla.prayertimes.wear.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WatchStopAdhanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WatchAdhanPlaybackService.stopNow(context)
    }
}
