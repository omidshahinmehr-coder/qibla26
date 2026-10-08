package com.qibla.prayertimes.alarm

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
import com.qibla.prayertimes.R
import com.qibla.prayertimes.util.LocalePrefs

class AdhanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val stopHandler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stopSelfCleanly() }

    override fun onBind(intent: Intent?): IBinder? = null

    // Use the in-app language (not the system one) for every string this service produces:
    // notification title/text/action and the channel name/description.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocalePrefs.wrap(newBase))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerName = intent?.getStringExtra(AlarmScheduler.EXTRA_PRAYER)
        val prayer = AdhanPrayer.entries.firstOrNull { it.name == prayerName } ?: AdhanPrayer.FAJR

        AdhanPlaybackState.setPlaying(prayer)
        startForeground(NOTIFICATION_ID, buildNotification(prayer))
        launchAlertScreen(prayer)
        playSound(prayer)

        // Safety net: never let the adhan ring longer than 4 minutes even if playback loops or hangs.
        stopHandler.postDelayed(autoStopRunnable, 4 * 60 * 1000L)

        return START_NOT_STICKY
    }

    /**
     * Besides the notification's full-screen intent (which Android only honours when the
     * "full-screen notifications" permission is granted), also try to open the alert screen
     * directly. Background activity starts can be blocked by the OS, so failure is ignored.
     */
    private fun launchAlertScreen(prayer: AdhanPrayer) {
        try {
            startActivity(
                Intent(this, AdhanAlertActivity::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_PRAYER, prayer.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                }
            )
        } catch (e: Exception) {
            // blocked by the system — the full-screen intent on the notification is the fallback
        }
    }

    private fun playSound(prayer: AdhanPrayer) {
        val uri = AlarmPrefs(this).soundUri(prayer)
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(this@AdhanPlaybackService, uri)
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

    private fun buildNotification(prayer: AdhanPrayer): Notification {
        createChannelIfNeeded()

        val alertIntent = Intent(this, AdhanAlertActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_PRAYER, prayer.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        val alertPendingIntent = PendingIntent.getActivity(
            this, 0, alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, StopAdhanReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_adhan)
            .setContentTitle(getString(R.string.adhan_time_title, prayer.label(this)))
            .setContentText(getString(R.string.adhan_tap_to_stop))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(alertPendingIntent)
            // Brings AdhanAlertActivity to the front immediately — even over the lock screen —
            // rather than leaving the alert as just a notification the user has to go find.
            .setFullScreenIntent(alertPendingIntent, true)
            .addAction(0, getString(R.string.stop_sound_action), stopPendingIntent)
            .build()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Always (re)create: for an existing channel Android only updates the name and
            // description, which keeps them in sync with the in-app language.
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.adhan_channel_name), NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.adhan_channel_desc)
                setSound(null, null) // the service plays the chosen sound itself
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
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
        AdhanPlaybackState.clear()
        @Suppress("DEPRECATION")
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopHandler.removeCallbacks(autoStopRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
        AdhanPlaybackState.clear()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "adhan_channel"
        private const val NOTIFICATION_ID = 5001

        fun stopNow(context: Context) {
            context.stopService(Intent(context, AdhanPlaybackService::class.java))
        }
    }
}
