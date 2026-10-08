package com.qibla.prayertimes.wear.alarm

import android.content.Context
import com.qibla.prayertimes.wear.R

/** Prayers that can have an adhan alarm on the watch. Sunrise/Sunset/Midnight are informational only. */
enum class WatchAdhanPrayer(val timingsKey: String, val labelRes: Int, val requestCode: Int) {
    FAJR("Fajr", R.string.wear_prayer_fajr, 201),
    DHUHR("Dhuhr", R.string.wear_prayer_dhuhr, 202),
    ASR("Asr", R.string.wear_prayer_asr, 203),
    MAGHRIB("Maghrib", R.string.wear_prayer_maghrib, 204),
    ISHA("Isha", R.string.wear_prayer_isha, 205);

    fun label(context: Context): String = context.getString(labelRes)
}
