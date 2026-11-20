package com.example.alarm

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
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
import com.example.data.model.AdhanVoice
import com.example.data.model.WaqtType
import com.example.util.BanglaUtils
import com.example.util.isEnglishLang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdhanAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val waqtKey = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_WAQT_KEY) ?: WaqtType.FAJR.key
        val modeName = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_ALERT_MODE) ?: AdhanAlertMode.FULL_ADHAN.name
        val voiceName = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_ADHAN_VOICE)
        val isPreReminder = intent.getBooleanExtra(AdhanAlarmScheduler.EXTRA_IS_PRE_REMINDER, false)
        val preMinutes = intent.getIntExtra(AdhanAlarmScheduler.EXTRA_PRE_MINUTES, 15)
        val languageCode = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_LANGUAGE_CODE) ?: "bn"

        val waqt = WaqtType.fromKey(waqtKey)
        val mode = AdhanAlertMode.safeValueOf(modeName)
        val voice = if (voiceName != null) AdhanVoice.safeValueOf(voiceName) else AdhanVoice.fromLegacyMode(mode)

        triggerAlert(
            context = context,
            waqt = waqt,
            voice = voice,
            isPreReminder = isPreReminder,
            preMinutes = preMinutes,
            isEnglish = isEnglishLang(languageCode)
        )

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
        @Volatile
        private var activeRingtone: Ringtone? = null

        fun stopActiveAudio(context: Context? = null) {
            try {
                activeRingtone?.stop()
            } catch (_: Exception) {
            } finally {
                activeRingtone = null
            }
            if (context != null) {
                AdhanPlaybackService.stopPlayback(context)
            }
        }

        fun triggerAlert(
            context: Context,
            waqt: WaqtType,
            mode: AdhanAlertMode,
            isPreReminder: Boolean = false,
            preMinutes: Int = 15,
            isEnglish: Boolean = false
        ) {
            triggerAlert(
                context = context,
                waqt = waqt,
                voice = AdhanVoice.fromLegacyMode(mode),
                isPreReminder = isPreReminder,
                preMinutes = preMinutes,
                isEnglish = isEnglish
            )
        }

        fun triggerAlert(
            context: Context,
            waqt: WaqtType,
            voice: AdhanVoice,
            isPreReminder: Boolean = false,
            preMinutes: Int = 15,
            isEnglish: Boolean = false,
            isTestMode: Boolean = false
        ) {
            // Full offline Azan MP3 via dedicated mediaPlayback Foreground Service on USAGE_ALARM
            if (!isPreReminder && voice.isFullAdhan) {
                stopActiveAudio(context)
                AdhanPlaybackService.startPlayback(
                    context = context,
                    waqt = waqt,
                    voice = voice,
                    isEnglish = isEnglish,
                    isTestMode = isTestMode
                )
                return
            }

            // Pre-prayer reminder (5/10/15m) or Short Beep / Silent Vibrate -> concise notification + short tone/vibrate
            AdhanAlarmScheduler.ensureNotificationChannel(context)

            val prefix = if (isTestMode) {
                if (isEnglish) "[Test] " else "[পরীক্ষামূলক] "
            } else {
                ""
            }

            val title = prefix + if (isEnglish) {
                if (isPreReminder) {
                    "$preMinutes minutes left for ${waqt.titleEn} prayer"
                } else {
                    "${waqt.titleEn} prayer time has begun"
                }
            } else {
                if (isPreReminder) {
                    "${waqt.titleBn} নামাজের ${BanglaUtils.toBanglaNumber(preMinutes)} মিনিট বাকি"
                } else {
                    "${waqt.titleBn} নামাজের ওয়াক্ত শুরু হয়েছে"
                }
            }

            val body = if (isEnglish) {
                if (isPreReminder) {
                    "Prepare for prayer and perform Wudu."
                } else {
                    "Hayya 'alas-Salah — ${voice.titleEn} (${waqt.rakatInfoEn})."
                }
            } else {
                if (isPreReminder) {
                    "প্রস্তুতি নিন ও ওযু সম্পন্ন করুন।"
                } else {
                    "হাইয়্যা আলাস সালাহ — ${voice.titleBn} (${waqt.rakatInfoBn})।"
                }
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

            when {
                voice == AdhanVoice.SILENT_VIBRATE -> triggerVibration(context)
                else -> playBeepSound(context)
            }
        }

        private fun playBeepSound(context: Context) {
            stopActiveAudio(context)
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                activeRingtone = ringtone
                ringtone?.play()
            } catch (_: Exception) {
            }
        }

        private fun triggerVibration(context: Context) {
            stopActiveAudio(context)
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
