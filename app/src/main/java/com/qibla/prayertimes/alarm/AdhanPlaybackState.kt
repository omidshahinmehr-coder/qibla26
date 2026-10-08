package com.qibla.prayertimes.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the adhan is currently playing, and which prayer — read by [AdhanAlertActivity] so it
 * can auto-dismiss itself the moment playback stops for any reason (tapped stop, the 4-minute
 * safety timeout, a playback error), without the activity needing to poll the service. The
 * service and the activity run in the same process, so a plain in-memory StateFlow is enough.
 */
object AdhanPlaybackState {
    private val _currentlyPlaying = MutableStateFlow<AdhanPrayer?>(null)
    val currentlyPlaying: StateFlow<AdhanPrayer?> = _currentlyPlaying

    fun setPlaying(prayer: AdhanPrayer) {
        _currentlyPlaying.value = prayer
    }

    fun clear() {
        _currentlyPlaying.value = null
    }
}
