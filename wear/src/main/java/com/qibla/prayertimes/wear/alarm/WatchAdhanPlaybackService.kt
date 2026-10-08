package com.qibla.prayertimes.wear.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.qibla.prayertimes.wear.MainActivity
import com.qibla.prayertimes.wear.R

class WatchAdhanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val stopHandler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stopSelfCleanly() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerName = intent?.getStringExtra(WatchAlarmScheduler.EXTRA_PRAYER)
        val prayer = WatchAdhanPrayer.entries.firstOrNull { it.name == prayerName } ?: WatchAdhanPrayer.FAJR

        WatchAdhanPlaybackState.setPlaying(prayer)
        startForeground(NOTIFICATION_ID, buildNotification(prayer))
        playSound()

        // Safety net: never let the adhan ring longer than 4 minutes even if playback hangs.
        stopHandler.postDelayed(autoStopRunnable, 4 * 60 * 1000L)

        return START_NOT_STICKY
    }

    private fun playSound() {
        val uri = WatchAlarmPrefs(this).soundUri()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(this@WatchAdhanPlaybackService, uri)
                isLooping = false
                setOnCompletionListener { stopSelfCleanly() }
                setOnErrorListener { _, _, _ -> stopSelfCleanly(); true }
                prepare()
                start()
            }
        } catch (e: Exception) {
            stopSelfCleanly()
        }
    }

    private fun buildNotification(prayer: WatchAdhanPrayer): Notification {
        createChannelIfNeeded()

        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, WatchStopAdhanReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_adhan)
            .setContentTitle(getString(R.string.adhan_time_title, prayer.label(this)))
            .setContentText(getString(R.string.adhan_tap_to_stop))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.stop_sound_action), stopPendingIntent)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, getString(R.string.adhan_channel_name), NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(R.string.adhan_channel_desc)
                    setSound(null, null) // the service plays the chosen sound itself
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun stopSelfCleanly() {
        stopHandler.removeCallbacks(autoStopRunnable)
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            // already released or never fully prepared
        }
        mediaPlayer = null
        WatchAdhanPlaybackState.clear()
        @Suppress("DEPRECATION")
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopHandler.removeCallbacks(autoStopRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
        WatchAdhanPlaybackState.clear()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "watch_adhan_channel"
        private const val NOTIFICATION_ID = 6001

        fun stopNow(context: Context) {
            context.stopService(Intent(context, WatchAdhanPlaybackService::class.java))
        }
    }
}
