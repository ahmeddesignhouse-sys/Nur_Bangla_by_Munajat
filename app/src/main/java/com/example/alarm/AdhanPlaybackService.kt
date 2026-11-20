package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RawRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.AdhanVoice
import com.example.data.model.WaqtType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AdhanPlaybackState(
    val isPlaying: Boolean = false,
    val waqt: WaqtType = WaqtType.FAJR,
    val voice: AdhanVoice = AdhanVoice.MAKKAH,
    val isTestMode: Boolean = false,
    val isEnglish: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0
)

/**
 * Dedicated mediaPlayback foreground service that plays the FULL offline Makkah, Madinah, or
 * Al-Aqsa Azan MP3 on the Android ALARM audio stream (USAGE_ALARM) with WakeLock, AudioFocus,
 * an ongoing notification with a Stop button, and a full-screen lockscreen alarm screen.
 */
class AdhanPlaybackService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var progressJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlaybackAndSelf()
                return START_NOT_STICKY
            }
            ACTION_PLAY -> {
                val waqt = WaqtType.fromKey(intent.getStringExtra(EXTRA_WAQT_KEY) ?: WaqtType.FAJR.key)
                val voice = AdhanVoice.safeValueOf(intent.getStringExtra(EXTRA_VOICE_NAME))
                val isEnglish = intent.getBooleanExtra(EXTRA_IS_ENGLISH, false)
                val isTestMode = intent.getBooleanExtra(EXTRA_IS_TEST_MODE, false)
                beginFullAdhanPlayback(waqt, voice, isEnglish, isTestMode)
            }
        }
        return START_NOT_STICKY
    }

    private fun beginFullAdhanPlayback(
        waqt: WaqtType,
        voice: AdhanVoice,
        isEnglish: Boolean,
        isTestMode: Boolean
    ) {
        releasePlaybackResources()
        ensurePlaybackChannel(this)

        val notification = buildOngoingNotification(waqt, voice, isEnglish, isTestMode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        acquireWakeLock()
        requestAlarmAudioFocus()

        val rawResId = rawResIdForVoice(voice)
        try {
            val afd = resources.openRawResourceFd(rawResId)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = false
                prepare()
                setOnCompletionListener {
                    stopPlaybackAndSelf()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlaybackAndSelf()
                    true
                }
                start()
            }
            mediaPlayer = player
            val duration = player.duration.coerceAtLeast(0)
            _playbackState.value = AdhanPlaybackState(
                isPlaying = true,
                waqt = waqt,
                voice = voice,
                isTestMode = isTestMode,
                isEnglish = isEnglish,
                positionMs = 0,
                durationMs = duration
            )

            progressJob = serviceScope.launch {
                while (isActive) {
                    val p = mediaPlayer
                    if (p != null && p.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
                            positionMs = p.currentPosition.coerceAtLeast(0),
                            durationMs = p.duration.coerceAtLeast(duration)
                        )
                    }
                    delay(500L)
                }
            }
        } catch (_: Exception) {
            stopPlaybackAndSelf()
        }
    }

    private fun buildOngoingNotification(
        waqt: WaqtType,
        voice: AdhanVoice,
        isEnglish: Boolean,
        isTestMode: Boolean
    ): Notification {
        val prefix = if (isTestMode) {
            if (isEnglish) "[Test] " else "[পরীক্ষামূলক] "
        } else {
            ""
        }
        val title = prefix + if (isEnglish) {
            "${waqt.titleEn} Adhan — ${voice.titleEn}"
        } else {
            "${waqt.titleBn} আযান — ${voice.titleBn}"
        }
        val body = if (isEnglish) {
            "Hayya 'alas-Salah • Tap Stop to silence Adhan"
        } else {
            "হাইয়্যা আলাস সালাহ • আযান বন্ধ করতে 'বন্ধ করুন' ট্যাপ করুন"
        }

        val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            9901,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmScreenIntent = Intent(this, AdhanAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_WAQT_KEY, waqt.key)
            putExtra(EXTRA_VOICE_NAME, voice.name)
            putExtra(EXTRA_IS_ENGLISH, isEnglish)
            putExtra(EXTRA_IS_TEST_MODE, isTestMode)
        }
        val alarmScreenPendingIntent = PendingIntent.getActivity(
            this,
            9902,
            alarmScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, PLAYBACK_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(alarmScreenPendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                if (isEnglish) "Stop Adhan" else "আযান বন্ধ করুন",
                stopPendingIntent
            )

        if (!isTestMode) {
            builder.setFullScreenIntent(alarmScreenPendingIntent, true)
        }

        return builder.build()
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NurBangla:AdhanPlaybackWakeLock")
            wl.setReferenceCounted(false)
            wl.acquire(5 * 60 * 1000L) // Max 5 minutes safety bound
            wakeLock = wl
        } catch (_: Exception) {
        }
    }

    private fun requestAlarmAudioFocus() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(false)
                    .setWillPauseWhenDucked(false)
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
        } catch (_: Exception) {
        }
    }

    private fun releasePlaybackResources() {
        progressJob?.cancel()
        progressJob = null

        try {
            mediaPlayer?.let { p ->
                if (p.isPlaying) p.stop()
                p.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }

        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            }
        } catch (_: Exception) {
        } finally {
            audioFocusRequest = null
        }

        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {
        } finally {
            wakeLock = null
        }

        _playbackState.value = _playbackState.value.copy(isPlaying = false, positionMs = 0)
    }

    private fun stopPlaybackAndSelf() {
        releasePlaybackResources()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        releasePlaybackResources()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.noorbangla.islam.ACTION_PLAY_FULL_ADHAN"
        const val ACTION_STOP = "com.noorbangla.islam.ACTION_STOP_FULL_ADHAN"
        const val EXTRA_WAQT_KEY = "extra_waqt_key"
        const val EXTRA_VOICE_NAME = "extra_voice_name"
        const val EXTRA_IS_ENGLISH = "extra_is_english"
        const val EXTRA_IS_TEST_MODE = "extra_is_test_mode"

        const val PLAYBACK_CHANNEL_ID = "noor_bangla_full_adhan_alarm_v17"
        private const val NOTIFICATION_ID = 4401

        private val _playbackState = MutableStateFlow(AdhanPlaybackState())
        val playbackState: StateFlow<AdhanPlaybackState> = _playbackState.asStateFlow()

        @RawRes
        fun rawResIdForVoice(voice: AdhanVoice): Int = when (voice) {
            AdhanVoice.MAKKAH -> R.raw.adhan_makkah
            AdhanVoice.MADINAH -> R.raw.adhan_madinah
            AdhanVoice.AL_AQSA -> R.raw.adhan_alaqsa
            AdhanVoice.SHORT_BEEP, AdhanVoice.SILENT_VIBRATE -> R.raw.adhan_makkah
        }

        fun ensurePlaybackChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(PLAYBACK_CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        PLAYBACK_CHANNEL_ID,
                        "আযান অ্যালার্ম (Full Adhan Alarm)",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Plays full offline Makkah, Madinah, and Al-Aqsa Adhan at prayer times."
                        setSound(null, null) // Audio is played directly via MediaPlayer on USAGE_ALARM
                        enableVibration(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                    nm.createNotificationChannel(channel)
                }
            }
        }

        fun startPlayback(
            context: Context,
            waqt: WaqtType,
            voice: AdhanVoice,
            isEnglish: Boolean,
            isTestMode: Boolean
        ) {
            val intent = Intent(context.applicationContext, AdhanPlaybackService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_WAQT_KEY, waqt.key)
                putExtra(EXTRA_VOICE_NAME, voice.name)
                putExtra(EXTRA_IS_ENGLISH, isEnglish)
                putExtra(EXTRA_IS_TEST_MODE, isTestMode)
            }
            ContextCompat.startForegroundService(context.applicationContext, intent)
        }

        fun stopPlayback(context: Context) {
            try {
                val intent = Intent(context.applicationContext, AdhanPlaybackService::class.java).apply {
                    action = ACTION_STOP
                }
                context.applicationContext.startService(intent)
            } catch (_: Exception) {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, AdhanPlaybackService::class.java)
                )
            }
            _playbackState.value = _playbackState.value.copy(isPlaying = false, positionMs = 0)
        }
    }
}
