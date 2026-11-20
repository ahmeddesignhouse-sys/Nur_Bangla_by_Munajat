package com.example.alarm

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import com.example.data.model.AdhanVoice
import com.example.data.model.WaqtType
import com.example.util.BanglaUtils
import com.example.util.isEnglishLang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AdhanAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val waqtKey = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_WAQT_KEY) ?: WaqtType.FAJR.key
        val modeName = intent.getStringExtra(AdhanAlarmScheduler.EXTRA_ALERT_MODE) ?: AdhanAlertMode.BEEP.name
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
        private var activePlayer: MediaPlayer? = null
        @Volatile
        private var activeAudioTrack: AudioTrack? = null

        fun stopActiveAudio() {
            try {
                activePlayer?.stop()
                activePlayer?.release()
            } catch (_: Exception) {
            } finally {
                activePlayer = null
            }
            try {
                activeAudioTrack?.stop()
                activeAudioTrack?.release()
            } catch (_: Exception) {
            } finally {
                activeAudioTrack = null
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

            when (voice) {
                AdhanVoice.MAKKAH -> playMakkahAdhan(context)
                AdhanVoice.MADINAH -> playSynthesizedMaqamAdhan(
                    notesHz = doubleArrayOf(293.66, 329.63, 392.00, 369.99, 329.63, 293.66),
                    durationsSec = doubleArrayOf(0.55, 0.45, 0.70, 0.45, 0.55, 0.80)
                )
                AdhanVoice.AL_AQSA -> playSynthesizedMaqamAdhan(
                    notesHz = doubleArrayOf(349.23, 392.00, 440.00, 392.00, 349.23, 329.63),
                    durationsSec = doubleArrayOf(0.50, 0.55, 0.75, 0.45, 0.55, 0.75)
                )
                AdhanVoice.SHORT_BEEP -> playBeepSound(context)
                AdhanVoice.SILENT_VIBRATE -> triggerVibration(context)
            }
        }

        private fun playMakkahAdhan(context: Context) {
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

        /**
         * Plays a smooth offline harmonic maqam chime cadence for Madinah & Al-Aqsa Adhan voices.
         */
        private fun playSynthesizedMaqamAdhan(notesHz: DoubleArray, durationsSec: DoubleArray) {
            stopActiveAudio()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val sampleRate = 22050
                    val totalSamples = durationsSec.sumOf { (it * sampleRate).toInt() }
                    val pcm = ShortArray(totalSamples)
                    var cursor = 0
                    for (idx in notesHz.indices) {
                        val freq = notesHz[idx]
                        val len = (durationsSec[idx] * sampleRate).toInt()
                        for (i in 0 until len) {
                            if (cursor >= totalSamples) break
                            val t = i.toDouble() / sampleRate
                            val attack = (i.toDouble() / (sampleRate * 0.04)).coerceAtMost(1.0)
                            val decay = exp(-2.2 * t / durationsSec[idx])
                            val env = attack * decay
                            val wave = (0.65 * sin(2.0 * PI * freq * t) +
                                0.25 * sin(2.0 * PI * freq * 2.0 * t) +
                                0.10 * sin(2.0 * PI * freq * 3.0 * t))
                            pcm[cursor++] = (wave * env * 18000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        }
                    }

                    val bufferBytes = pcm.size * 2
                    val track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(bufferBytes)
                        .build()

                    track.write(pcm, 0, pcm.size)
                    activeAudioTrack = track
                    track.play()
                } catch (_: Exception) {
                }
            }
        }

        private fun playBeepSound(context: Context) {
            stopActiveAudio()
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                ringtone?.play()
            } catch (_: Exception) {
            }
        }

        private fun triggerVibration(context: Context) {
            stopActiveAudio()
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
