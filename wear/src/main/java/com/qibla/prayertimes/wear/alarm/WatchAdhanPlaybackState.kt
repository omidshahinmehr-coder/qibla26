package com.qibla.prayertimes.wear.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the adhan is currently playing, and which prayer — read by the UI (MainActivity) so
 * it can show a big, impossible-to-miss "stop" button directly on screen, instead of the user
 * having to find and tap the system notification's stop action. The service and the activity
 * run in the same process (no android:process on the service), so a plain in-memory StateFlow
 * is enough — no cross-process IPC needed.
 */
object WatchAdhanPlaybackState {
    private val _currentlyPlaying = MutableStateFlow<WatchAdhanPrayer?>(null)
    val currentlyPlaying: StateFlow<WatchAdhanPrayer?> = _currentlyPlaying

    fun setPlaying(prayer: WatchAdhanPrayer) {
        _currentlyPlaying.value = prayer
    }

    fun clear() {
        _currentlyPlaying.value = null
    }
}
