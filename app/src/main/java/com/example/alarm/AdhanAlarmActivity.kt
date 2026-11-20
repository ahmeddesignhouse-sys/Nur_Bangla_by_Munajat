package com.example.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdhanVoice
import com.example.data.model.WaqtType
import com.example.ui.theme.NoorBanglaTheme
import com.example.util.BanglaUtils
import com.example.util.tr
import java.util.Locale

/**
 * Full-screen lockscreen Adhan Alarm activity shown when a prayer-time full Adhan fires.
 * Displays the active prayer name, selected Mosque Adhan voice, elapsed playback progress,
 * and a large accessible Stop Adhan button.
 */
class AdhanAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureShowOverLockScreen()

        val initialWaqt = WaqtType.fromKey(
            intent.getStringExtra(AdhanPlaybackService.EXTRA_WAQT_KEY) ?: WaqtType.FAJR.key
        )
        val initialVoice = AdhanVoice.safeValueOf(
            intent.getStringExtra(AdhanPlaybackService.EXTRA_VOICE_NAME)
        )
        val initialIsEnglish = intent.getBooleanExtra(AdhanPlaybackService.EXTRA_IS_ENGLISH, false)

        setContent {
            NoorBanglaTheme {
                val state by AdhanPlaybackService.playbackState.collectAsStateWithLifecycle()
                val waqt = if (state.isPlaying) state.waqt else initialWaqt
                val voice = if (state.isPlaying) state.voice else initialVoice
                val isEn = if (state.isPlaying) state.isEnglish else initialIsEnglish

                LaunchedEffect(state.isPlaying, state.positionMs) {
                    if (!state.isPlaying && state.durationMs > 0) {
                        finish()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF07251C),
                                    Color(0xFF0D3B2C),
                                    Color(0xFF081D16)
                                )
                            )
                        )
                        .padding(horizontal = 28.dp, vertical = 48.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top app identity
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "নূর বাংলা",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF5D57A)
                            )
                            Text(
                                text = tr(isEn, "নামাজের ওয়াক্ত শুরু হয়েছে", "Prayer Time Has Begun"),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        // Center Mosque emblem + Waqt + Voice + Live Progress
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF5D57A).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mosque,
                                    contentDescription = null,
                                    tint = Color(0xFFF5D57A),
                                    modifier = Modifier.size(54.dp)
                                )
                            }

                            Text(
                                text = waqt.title(isEn),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = voice.title(isEn),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF5D57A),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Text(
                                text = waqt.rakatInfo(isEn),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.80f),
                                textAlign = TextAlign.Center
                            )

                            val progress = if (state.durationMs > 0) {
                                (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFFF5D57A),
                                    trackColor = Color.White.copy(alpha = 0.20f)
                                )
                                Text(
                                    text = "${formatMmSs(state.positionMs, isEn)} / ${formatMmSs(state.durationMs, isEn)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }

                        // Large Stop Adhan CTA
                        Button(
                            onClick = {
                                AdhanPlaybackService.stopPlayback(this@AdhanAlarmActivity)
                                finish()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .testTag("adhan_alarm_stop_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = tr(isEn, "আযান বন্ধ করুন", "Stop Adhan"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    private fun configureShowOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun formatMmSs(ms: Int, isEnglish: Boolean): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val min = totalSec / 60
        val sec = totalSec % 60
        val formatted = String.format(Locale.US, "%02d:%02d", min, sec)
        return if (isEnglish) formatted else BanglaUtils.toBanglaDigits(formatted)
    }
}
