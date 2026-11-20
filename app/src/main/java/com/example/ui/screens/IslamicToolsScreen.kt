package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BanglaDhikrCatalog
import com.example.data.model.DhikrItem
import com.example.ui.NoorUiState
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.StatusOnTimeColor
import com.example.util.BanglaUtils
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IslamicToolsScreen(
    uiState: NoorUiState,
    currentDhikr: DhikrItem,
    onIncrementTasbih: () -> Boolean,
    onResetTasbih: () -> Unit,
    onSelectDhikr: (DhikrItem) -> Unit,
    onSetTasbihTarget: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Qibla, 1 = Tasbih

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "ইসলামিক টুলস (অফলাইন)",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Default.Explore, contentDescription = null) }
            ) {
                Text("কিবলা কম্পাস")
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Default.TouchApp, contentDescription = null) }
            ) {
                Text("ডিজিটাল তাসবিহ")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            QiblaCompassPane(uiState = uiState)
        } else {
            DigitalTasbihPane(
                uiState = uiState,
                currentDhikr = currentDhikr,
                onIncrement = onIncrementTasbih,
                onReset = onResetTasbih,
                onSelectDhikr = onSelectDhikr,
                onSetTarget = onSetTasbihTarget
            )
        }
    }
}

@Composable
private fun QiblaCompassPane(uiState: NoorUiState) {
    val context = LocalContext.current
    var azimuthDeg by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationSensor == null && (accelSensor == null || magSensor == null)) {
            sensorAvailable = false
        }

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeo = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotMatrix, orientation)
                        val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                        azimuthDeg = smoothAngle(azimuthDeg, deg)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasGeo = true
                    }
                }
                if (rotationSensor == null && hasGravity && hasGeo) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                        azimuthDeg = smoothAngle(azimuthDeg, deg)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (accelSensor != null && magSensor != null) {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val qiblaBearing = uiState.qiblaBearing
    val relativeQibla = ((qiblaBearing - azimuthDeg) % 360f + 360f) % 360f
    val isAligned = relativeQibla <= 6f || relativeQibla >= 354f
    val animatedDialRotation by animateFloatAsState(
        targetValue = -azimuthDeg,
        animationSpec = spring(),
        label = "dialRotation"
    )
    val animatedQiblaNeedle by animateFloatAsState(
        targetValue = qiblaBearing - azimuthDeg,
        animationSpec = spring(),
        label = "qiblaNeedle"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAligned) StatusOnTimeColor.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isAligned) "আলহামদুলিল্লাহ! আপনি সঠিক কিবলামুখী হয়েছেন"
                        else "ফোনটি সমতলে রেখে কিবলার দিকে ঘোরান",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAligned) StatusOnTimeColor else primaryColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${uiState.locationDisplayTitle} থেকে কাবার দিক: ${BanglaUtils.toBanglaNumber(qiblaBearing.toInt())}° (${cardinalDirectionBn(qiblaBearing)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "মক্কা শরীফের দূরত্ব: প্রায় ${BanglaUtils.toBanglaNumber(uiState.distanceToKaabaKm)} কি.মি.",
                        style = MaterialTheme.typography.labelLarge,
                        color = secondaryColor
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f - 10f

                    // Outer bezel
                    drawCircle(
                        color = if (isAligned) StatusOnTimeColor else primaryColor,
                        radius = radius,
                        style = Stroke(width = 6f)
                    )
                    drawCircle(
                        color = surfaceVariantColor.copy(alpha = 0.35f),
                        radius = radius - 10f
                    )

                    // Rotating compass ticks
                    rotate(degrees = animatedDialRotation, pivot = center) {
                        for (deg in 0 until 360 step 15) {
                            val rad = Math.toRadians(deg.toDouble() - 90.0)
                            val inner = if (deg % 90 == 0) radius - 28f else radius - 16f
                            val outer = radius - 8f
                            drawLine(
                                color = if (deg == 0) Color(0xFFD93829) else onSurfaceColor.copy(alpha = 0.45f),
                                start = Offset(
                                    center.x + (inner * cos(rad)).toFloat(),
                                    center.y + (inner * sin(rad)).toFloat()
                                ),
                                end = Offset(
                                    center.x + (outer * cos(rad)).toFloat(),
                                    center.y + (outer * sin(rad)).toFloat()
                                ),
                                strokeWidth = if (deg % 90 == 0) 4f else 2f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Qibla pointer needle
                    rotate(degrees = animatedQiblaNeedle, pivot = center) {
                        val arrowPath = Path().apply {
                            moveTo(center.x, center.y - radius + 34f)
                            lineTo(center.x - 16f, center.y + 18f)
                            lineTo(center.x, center.y + 6f)
                            lineTo(center.x + 16f, center.y + 18f)
                            close()
                        }
                        drawPath(
                            path = arrowPath,
                            color = if (isAligned) StatusOnTimeColor else primaryColor
                        )
                        // Kaaba gold tip
                        drawCircle(
                            color = secondaryColor,
                            radius = 9f,
                            center = Offset(center.x, center.y - radius + 34f)
                        )
                    }

                    drawCircle(color = primaryColor, radius = 10f, center = center)
                }
            }
        }

        if (!sensorAvailable) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "এই ডিভাইসে ম্যাগনেটোমিটার সেন্সর পাওয়া যায়নি। তবুও ভৌগোলিক হিসেব অনুযায়ী কাবার দিক ${cardinalDirectionBn(qiblaBearing)} (${BanglaUtils.toBanglaNumber(qiblaBearing.toInt())}°)।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

private fun cardinalDirectionBn(bearingDeg: Float): String {
    val norm = ((bearingDeg % 360f) + 360f) % 360f
    return when {
        norm < 22.5f || norm >= 337.5f -> "উত্তর"
        norm < 67.5f -> "উত্তর-পূর্ব"
        norm < 112.5f -> "পূর্ব"
        norm < 157.5f -> "দক্ষিণ-পূর্ব"
        norm < 202.5f -> "দক্ষিণ"
        norm < 247.5f -> "দক্ষিণ-পশ্চিম"
        norm < 292.5f -> "পশ্চিম"
        else -> "উত্তর-পশ্চিম"
    }
}

private fun smoothAngle(current: Float, target: Float): Float {
    var diff = target - current
    while (diff < -180f) diff += 360f
    while (diff > 180f) diff -= 360f
    return if (abs(diff) < 0.8f) current else (current + diff * 0.25f + 360f) % 360f
}

@Composable
private fun DigitalTasbihPane(
    uiState: NoorUiState,
    currentDhikr: DhikrItem,
    onIncrement: () -> Boolean,
    onReset: () -> Unit,
    onSelectDhikr: (DhikrItem) -> Unit,
    onSetTarget: (Int) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showCustomTargetDialog by remember { mutableStateOf(false) }

    val count = uiState.settings.tasbihCount
    val target = uiState.settings.tasbihTarget
    val cycles = uiState.settings.tasbihCycles
    val progress = (count.toFloat() / target.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Bangla Dhikr Selector
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BanglaDhikrCatalog.ALL, key = { it.id }) { item ->
                    FilterChip(
                        selected = item.id == currentDhikr.id,
                        onClick = { onSelectDhikr(item) },
                        label = { Text(item.banglaTitle) }
                    )
                }
            }
        }

        // Active Dhikr Display Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = currentDhikr.arabic,
                        fontFamily = AmiriFontFamily,
                        fontSize = 30.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = currentDhikr.banglaTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentDhikr.banglaMeaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Tactile Counter Circle
        item {
            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .clickable {
                        val completed = onIncrement()
                        haptic.performHapticFeedback(
                            if (completed) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                        )
                        if (completed) {
                            vibrateShort(context, 180L)
                        } else {
                            vibrateShort(context, 30L)
                        }
                    }
                    .testTag("tasbih_tap_button"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 18f
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Surface(
                    modifier = Modifier.size(180.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = BanglaUtils.toBanglaNumber(count),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "লক্ষ্য: ${BanglaUtils.toBanglaNumber(target)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ট্যাপ করুন",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Target & Cycles Controls
        item {
            Text(
                text = "সম্পন্ন চক্র: ${BanglaUtils.toBanglaNumber(cycles)} বার",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(33, 99, 100).forEach { preset ->
                    FilterChip(
                        selected = target == preset,
                        onClick = { onSetTarget(preset) },
                        label = { Text(BanglaUtils.toBanglaNumber(preset)) }
                    )
                }
                OutlinedButton(onClick = { showCustomTargetDialog = true }) {
                    Text("কাস্টম")
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("tasbih_reset_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "রিসেট", modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showCustomTargetDialog) {
        var input by remember { mutableStateOf(target.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomTargetDialog = false },
            title = { Text("কাস্টম তাসবিহ লক্ষ্য") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("সংখ্যা লিখুন (যেমন: ১০০০)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = input.toIntOrNull()
                    if (parsed != null && parsed > 0) {
                        onSetTarget(parsed)
                    }
                    showCustomTargetDialog = false
                }) {
                    Text("সেট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTargetDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

private fun vibrateShort(context: Context, durationMs: Long) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    } catch (_: Exception) {
    }
}
