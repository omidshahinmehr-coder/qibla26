package com.qibla.prayertimes.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qibla.prayertimes.R
import com.qibla.prayertimes.ui.theme.AmberMuted
import com.qibla.prayertimes.ui.theme.AmberText
import com.qibla.prayertimes.ui.theme.Brass
import com.qibla.prayertimes.ui.theme.NightDeep
import com.qibla.prayertimes.ui.theme.QiblaAppTheme
import com.qibla.prayertimes.ui.theme.ThemeState
import com.qibla.prayertimes.util.LocalePrefs

/**
 * Shown full-screen, over the lock screen, the instant the adhan starts playing — not just a
 * notification, per the request that a screen actually come up announcing which prayer it is,
 * with its own stop control. Launched via the playback notification's `fullScreenIntent` (see
 * [AdhanPlaybackService]), which is the Android-sanctioned way to bring an activity to the
 * front from a background alarm even while the device is locked — directly calling
 * `startActivity` from the service would be blocked by background-activity-start restrictions
 * on Android 10+.
 */
class AdhanAlertActivity : ComponentActivity() {

    private var screenOffReceiver: BroadcastReceiver? = null

    // The alert screen follows the in-app language, not the system language.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocalePrefs.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This can be the very first activity to run in the process (e.g. right after a
        // reboot, before MainActivity ever opened this session), so the saved light/dark
        // preference needs loading here too — not just relying on MainActivity having already
        // done it.
        ThemeState.initFrom(this)

        showOverLockScreen()

        // The window closes only via the stop button: swallow the back gesture/button.
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* ignored on purpose */ }
        })

        // Hardware volume keys, while this activity is in front, adjust the alarm stream —
        // the same stream the adhan itself plays on (see AudioAttributes.USAGE_ALARM in
        // AdhanPlaybackService) — so raising/lowering volume here raises/lowers the adhan.
        volumeControlStream = AudioManager.STREAM_ALARM

        val prayerName = intent?.getStringExtra(AlarmScheduler.EXTRA_PRAYER)
        val prayer = AdhanPrayer.entries.firstOrNull { it.name == prayerName } ?: AdhanPrayer.FAJR

        setContent {
            AdhanAlertScreen(prayer = prayer, onStop = ::stopAndFinish)
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Power button: a SCREEN_OFF broadcast while this alert is in front is, in practice,
        // the user pressing power — stop the adhan and close, like alarm-clock apps do.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                stopAndFinish()
            }
        }
        screenOffReceiver = receiver
        registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onPause() {
        screenOffReceiver?.let { unregisterReceiver(it) }
        screenOffReceiver = null
        super.onPause()
    }

    /**
     * Volume keys (also on the lock screen) raise/lower the adhan itself by adjusting the alarm
     * stream directly, instead of relying on the system to route them.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.adjustStreamVolume(
                AudioManager.STREAM_ALARM,
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun stopAndFinish() {
        AdhanPlaybackService.stopNow(this)
        finish()
    }
}

/** Entezar — used only for the prayer-name line; everything else on this screen uses the
 *  app's default Estedad typography (see QiblaAppTheme). */
private val entezarFontFamily = FontFamily(Font(R.font.entezar))

@Composable
private fun AdhanAlertScreen(prayer: AdhanPrayer, onStop: () -> Unit) {
    val context = LocalContext.current

    // Auto-dismiss the instant playback stops for any reason (safety timeout, a playback
    // error) — not just when the user taps stop here.
    val playing by AdhanPlaybackState.currentlyPlaying.collectAsState()
    DisposableEffect(playing) {
        if (playing == null) onStop()
        onDispose { }
    }

    QiblaAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NightDeep)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hadith block, pinned near the top — uses the screen's default font
                // (Estedad, from QiblaAppTheme's typography), same as the hint text below.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.adhan_hadith_narrator),
                        color = AmberText,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.adhan_hadith_arabic),
                        color = AmberText,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.adhan_hadith_translation),
                        color = AmberMuted,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.adhan_hadith_source),
                        color = AmberMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.weight(1f))

                // Prayer name + stop control, vertically centered — the only text on this
                // screen in Entezar rather than the screen's default Estedad.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.adhan_time_title, prayer.label(context)),
                        color = AmberText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = entezarFontFamily,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.adhan_alert_hint),
                        color = AmberMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = Brass)) {
                        Text(stringResource(R.string.stop_sound_action), fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }
}
