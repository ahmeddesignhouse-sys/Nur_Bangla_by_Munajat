package com.example.alarm

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.PreferencesDataStore
import com.example.data.model.AdhanAlertMode
import com.example.data.model.WaqtType
import com.example.util.BanglaUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdhanAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val waqtKey = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_WAQT_KEY) ?: WaqtType.FAJR.key
        val modeName = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_ALERT_MODE) ?: AdhanAlertMode.BEEP.name
        val isPreReminder = intent.getBooleanExtra(AdhanAlarmScheduler.EXTRA_IS_PRE_REMINDER, false)
        val preMinutes = intent.getIntExtra(AdhanAlarmScheduler.EXTRA_PRE_MINUTES, 15)

        val waqt = WaqtType.fromKey(waqtKey)
        val mode = AdhanAlertMode.safeValueOf(modeName)

        triggerAlert(context, waqt, mode, isPreReminder, preMinutes)

        // Reschedule next day's alarms cleanly
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = PreferencesDataStore(context).settingsFlow.first()
                AdhanAlarmScheduler.scheduleUpcomingAlarms(context, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private var activePlayer: MediaPlayer? = null

        fun stopActiveAudio() {
            try {
                activePlayer?.stop()
                activePlayer?.release()
            } catch (_: Exception) {
            } finally {
                activePlayer = null
            }
        }

        fun triggerAlert(
            context: Context,
            waqt: WaqtType,
            mode: AdhanAlertMode,
            isPreReminder: Boolean = false,
            preMinutes: Int = 15
        ) {
            AdhanAlarmScheduler.ensureNotificationChannel(context)

            val title = if (isPreReminder) {
                "${waqt.titleBn} নামাজের ${BanglaUtils.toBanglaNumber(preMinutes)} মিনিট বাকি"
            } else {
                "${waqt.titleBn} নামাজের ওয়াক্ত শুরু হয়েছে"
            }
            val body = if (isPreReminder) {
                "প্রস্তুতি নিন ও ওযু সম্পন্ন করুন।"
            } else {
                "হাইয়্যা আলাস সালাহ — সময়মতো নামাজ আদায় করুন (${waqt.rakatInfoBn})।"
            }

            val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (canNotify) {
                try {
                    val notification = NotificationCompat.Builder(context, AdhanAlarmScheduler.CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()
                    NotificationManagerCompat.from(context).notify(
                        (waqt.ordinal * 10) + if (isPreReminder) 1 else 0,
                        notification
                    )
                } catch (_: SecurityException) {
                }
            }

            when (mode) {
                AdhanAlertMode.FULL_ADHAN -> playAdhanSound(context)
                AdhanAlertMode.BEEP -> playBeepSound(context)
                AdhanAlertMode.VIBRATE -> triggerVibration(context)
                AdhanAlertMode.SILENT -> Unit
            }
        }

        private fun playAdhanSound(context: Context) {
            stopActiveAudio()
            try {
                val player = MediaPlayer.create(context.applicationContext, R.raw.adhan_tone)
                if (player != null) {
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    player.setOnCompletionListener {
                        it.release()
                        if (activePlayer === it) activePlayer = null
                    }
                    activePlayer = player
                    player.start()
                }
            } catch (_: Exception) {
            }
        }

        private fun playBeepSound(context: Context) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                ringtone?.play()
            } catch (_: Exception) {
            }
        }

        private fun triggerVibration(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 350), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 350), -1)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(400L)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
