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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.util.CompassAccuracyLevel
import com.example.util.LocalAppLanguage
import com.example.util.QiblaCompassMath
import com.example.util.isEnglishLang
import com.example.util.tr
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vertically scrollable Islamic Tools screen:
 * - Top 8 tool shortcuts grid
 * - Dedicated full-width Qibla Compass card with fixed 320dp compass container (no clipping/overlap)
 * - Dedicated full-width Digital Tasbih card directly below with full counter dial & controls
 */
@Composable
fun IslamicToolsScreen(
    uiState: NoorUiState,
    currentDhikr: DhikrItem,
    onIncrementTasbih: () -> Boolean,
    onResetTasbih: () -> Unit,
    onSelectDhikr: (DhikrItem) -> Unit,
    onSetTasbihTarget: (Int) -> Unit,
    onOpenHijriCalendar: () -> Unit,
    onOpenDuaAzkar: () -> Unit,
    onOpenAsmaulHusna: () -> Unit,
    onOpenRamadanCalendar: () -> Unit,
    onOpenZakatCalculator: () -> Unit,
    onOpenMosqueFinder: () -> Unit,
    onOpenHajjUmrahGuide: () -> Unit,
    onOpenHalalFinder: () -> Unit
) {
    val isEn = isEnglishLang(LocalAppLanguage.current)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tools_scroll_container"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = tr(isEn, "ইসলামিক টুলস", "Islamic Tools"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Shortcut Grid (3 clean rows, all 8 tools preserved)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Hijri Calendar, Dua & Azkar, 99 Names of Allah
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToolFeatureShortcutCard(
                        icon = Icons.Default.CalendarMonth,
                        title = tr(isEn, "হিজরি\nক্যালেন্ডার", "Hijri\nCalendar"),
                        onClick = onOpenHijriCalendar,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_open_hijri_calendar")
                    )
                    ToolFeatureShortcutCard(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = tr(isEn, "দোয়া ও\nআজকার", "Dua &\nAzkar"),
                        onClick = onOpenDuaAzkar,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_open_dua_azkar")
                    )
                    ToolFeatureShortcutCard(
                        icon = Icons.Default.AutoAwesome,
                        title = tr(isEn, "আল্লাহর\n৯৯ নাম", "99 Names\nof Allah"),
                        onClick = onOpenAsmaulHusna,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_open_asmaul_husna")
                    )
                }

                // Row 2: Ramadan Calendar, Zakat Calculator, Mosque Finder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToolFeatureShortcutCard(
                        icon = Icons.Default.NightsStay,
                        title = tr(isEn, "রমজান\nক্যালেন্ডার", "Ramadan\nCalendar"),
                        onClick = onOpenRamadanCalendar,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_open_ramadan_calendar")
                    )
                    ToolFeatureShortcutCard(
                        icon = Icons.Default.Calculate,
                        title = tr(isEn, "যাকাত\nহিসাব", "Zakat\nCalculator"),
                        onClick = onOpenZakatCalculator,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_open_zakat_calculator")
                    )
                    ToolFeatureShortcutCard(
                        icon = Icons.Default.Mosque,
                        title = tr(isEn, "নিকটবর্তী\nমসজিদ", "Mosque\nFinder"),
                        onClick = onOpenMosqueFinder,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_open_mosque_finder")
                    )
                }

                // Row 3: Hajj & Umrah Guide, Halal Food Finder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToolFeatureShortcutCard(
                        icon = Icons.Default.TravelExplore,
                        title = tr(isEn, "হজ্জ ও উমরাহ\nগাইড", "Hajj & Umrah\nGuide"),
                        onClick = onOpenHajjUmrahGuide,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_open_hajj_umrah")
                    )
                    ToolFeatureShortcutCard(
                        icon = Icons.Default.Restaurant,
                        title = tr(isEn, "হালাল\nরেস্টুরেন্ট", "Halal Food\nFinder"),
                        onClick = onOpenHalalFinder,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_open_halal_finder")
                    )
                }
            }
        }

        // Dedicated Full-Width Qibla Compass Card (fixed 320dp compass area — never clipped or overlapped)
        item {
            QiblaCompassSectionCard(uiState = uiState, isEn = isEn)
        }

        // Dedicated Full-Width Digital Tasbih Card directly below Qibla Compass
        item {
            DigitalTasbihSectionCard(
                uiState = uiState,
                isEn = isEn,
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
private fun ToolFeatureShortcutCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title.replace("\n", " "),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun QiblaCompassSectionCard(uiState: NoorUiState, isEn: Boolean) {
    val context = LocalContext.current
    var continuousAzimuthDeg by remember { mutableFloatStateOf(0f) }
    var normalizedAzimuthDeg by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(true) }
    var accuracyLevel by remember { mutableStateOf(CompassAccuracyLevel.MEDIUM) }

    val activeLat = uiState.activeLatitude
    val activeLon = uiState.activeLongitude
    val declinationDeg = remember(activeLat, activeLon) {
        QiblaCompassMath.computeMagneticDeclinationDegrees(activeLat, activeLon)
    }

    DisposableEffect(context, declinationDeg) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val useFusion = accelSensor != null && magSensor != null
        if (!useFusion && rotationSensor == null) {
            sensorAvailable = false
        }

        var filteredGravity = FloatArray(3)
        var filteredGeo = FloatArray(3)
        var hasGravity = false
        var hasGeo = false
        var initializedAzimuth = false
        var runningContinuous = continuousAzimuthDeg

        val rotMatrix = FloatArray(9)
        val inclMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        fun applyTrueNorthAzimuth(rawMagneticDeg: Float) {
            val trueNorthDeg = QiblaCompassMath.toTrueNorthAzimuth(rawMagneticDeg, declinationDeg)
            if (!initializedAzimuth) {
                initializedAzimuth = true
                runningContinuous = trueNorthDeg
            } else {
                // Continuous unwrapped smoothing (alpha=0.28 at SENSOR_DELAY_GAME -> reacts in <1s, settles in <2s, ZERO 360° wrap spin!)
                runningContinuous = QiblaCompassMath.stepUnwrappedAngle(
                    currentContinuousDeg = runningContinuous,
                    targetMod360Deg = trueNorthDeg,
                    smoothingAlpha = 0.28f
                )
            }
            continuousAzimuthDeg = runningContinuous
            normalizedAzimuthDeg = QiblaCompassMath.normalizeDegrees(runningContinuous)
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        filteredGravity = if (!hasGravity) {
                            event.values.copyOf(3)
                        } else {
                            QiblaCompassMath.lowPassVector(event.values, filteredGravity, alpha = 0.22f)
                        }
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        filteredGeo = if (!hasGeo) {
                            event.values.copyOf(3)
                        } else {
                            QiblaCompassMath.lowPassVector(event.values, filteredGeo, alpha = 0.22f)
                        }
                        hasGeo = true
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        if (!useFusion) {
                            SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                            SensorManager.getOrientation(rotMatrix, orientation)
                            val deg = QiblaCompassMath.normalizeDegrees(Math.toDegrees(orientation[0].toDouble()).toFloat())
                            applyTrueNorthAzimuth(deg)
                        }
                    }
                }
                if (useFusion && hasGravity && hasGeo) {
                    if (SensorManager.getRotationMatrix(rotMatrix, inclMatrix, filteredGravity, filteredGeo)) {
                        SensorManager.getOrientation(rotMatrix, orientation)
                        val deg = QiblaCompassMath.normalizeDegrees(Math.toDegrees(orientation[0].toDouble()).toFloat())
                        applyTrueNorthAzimuth(deg)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    accuracyLevel = CompassAccuracyLevel.fromSensorAccuracy(accuracy)
                }
            }
        }

        if (useFusion) {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_GAME)
        } else if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val qiblaBearing = uiState.qiblaBearing
    val relativeQibla = QiblaCompassMath.normalizeDegrees(qiblaBearing - normalizedAzimuthDeg)
    val isAligned = sensorAvailable && (relativeQibla <= 6f || relativeQibla >= 354f)

    // Critically damped spring on continuous unwrapped angle -> crisp response, zero oscillation, zero 360° spin!
    val animatedDialRotation by animateFloatAsState(
        targetValue = -continuousAzimuthDeg,
        animationSpec = spring(dampingRatio = 1f, stiffness = 320f),
        label = "dialRotation"
    )
    val animatedQiblaNeedle by animateFloatAsState(
        targetValue = qiblaBearing - continuousAzimuthDeg,
        animationSpec = spring(dampingRatio = 1f, stiffness = 320f),
        label = "qiblaNeedle"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val accuracyBadgeColor = when (accuracyLevel) {
        CompassAccuracyLevel.HIGH -> StatusOnTimeColor
        CompassAccuracyLevel.MEDIUM -> MaterialTheme.colorScheme.primary
        CompassAccuracyLevel.LOW, CompassAccuracyLevel.UNRELIABLE -> MaterialTheme.colorScheme.error
    }

    val textDirectionSummary = QiblaCompassMath.formatTextDirectionSummary(
        qiblaBearingDeg = qiblaBearing,
        distanceKm = uiState.distanceToKaabaKm.toDouble(),
        isEnglish = isEn
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tools_qibla_section_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (isAligned) StatusOnTimeColor.copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = if (isAligned) StatusOnTimeColor else primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tr(isEn, "কিবলা কম্পাস", "Qibla Compass"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (sensorAvailable) {
                    Surface(
                        color = accuracyBadgeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("qibla_accuracy_badge")
                    ) {
                        Text(
                            text = accuracyLevel.title(isEn),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accuracyBadgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = if (isAligned) {
                    tr(
                        isEn,
                        "আলহামদুলিল্লাহ! আপনি সঠিক কিবলামুখী হয়েছেন",
                        "Alhamdulillah! You are facing the Qibla"
                    )
                } else {
                    tr(
                        isEn,
                        "ফোন সমতল করে ধরুন এবং কিবলার দিকে ঘোরান",
                        "Hold your phone flat and rotate toward Qibla"
                    )
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAligned) StatusOnTimeColor else primaryColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = textDirectionSummary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("qibla_text_direction_summary")
            )

            Text(
                text = tr(
                    isEn,
                    "সত্য উত্তর সমন্বয় (True North): ${uiState.locationDisplayTitle}",
                    "True North calibrated for ${uiState.locationDisplayTitle}"
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Fixed-height 320dp container so the complete compass dial & needle are ALWAYS visible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .testTag("tools_qibla_compass_box"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(280.dp)
                        .padding(8.dp)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f - 10f

                    drawCircle(
                        color = if (isAligned) StatusOnTimeColor else primaryColor,
                        radius = radius,
                        style = Stroke(width = 6f)
                    )
                    drawCircle(
                        color = surfaceVariantColor.copy(alpha = 0.35f),
                        radius = radius - 10f
                    )

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
                        drawCircle(
                            color = secondaryColor,
                            radius = 9f,
                            center = Offset(center.x, center.y - radius + 34f)
                        )
                    }

                    drawCircle(color = primaryColor, radius = 10f, center = center)
                }
            }

            // Figure-8 calibration hint whenever accuracy is Low/Unreliable
            if (sensorAvailable && accuracyLevel.needsCalibration) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qibla_calibration_hint"),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tr(
                                isEn,
                                "কম্পাস ক্যালিব্রেট করতে ফোনটি ৮ (figure-8) আকারে ঘোরান",
                                "Move your phone in a figure-8 (8) motion to calibrate the compass"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${tr(isEn, "ফোন সমতল করে ধরুন", "Hold phone flat")} • $textDirectionSummary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (!sensorAvailable) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qibla_no_sensor_banner"),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tr(
                                isEn,
                                "এই ডিভাইসে ম্যাগনেটোমিটার সেন্সর পাওয়া যায়নি। ফোন সমতল করে ধরুন।",
                                "Magnetometer sensor not found on this device. Hold phone flat."
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = textDirectionSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitalTasbihSectionCard(
    uiState: NoorUiState,
    isEn: Boolean,
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tools_tasbih_section_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = tr(isEn, "ডিজিটাল তাসবিহ", "Digital Tasbih"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BanglaDhikrCatalog.ALL, key = { it.id }) { item ->
                    FilterChip(
                        selected = item.id == currentDhikr.id,
                        onClick = { onSelectDhikr(item) },
                        label = { Text(item.title(isEn)) }
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        text = currentDhikr.title(isEn),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = currentDhikr.meaning(isEn),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier = Modifier
                    .size(224.dp)
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
                    modifier = Modifier.size(176.dp),
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
                            text = BanglaUtils.formatNumber(count, isEn),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tr(isEn, "লক্ষ্য", "Target")}: ${BanglaUtils.formatNumber(target, isEn)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = tr(isEn, "ট্যাপ করুন", "Tap to count"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Text(
                text = tr(
                    isEn,
                    "সম্পন্ন চক্র: ${BanglaUtils.formatNumber(cycles, false)} বার",
                    "Completed Cycles: ${BanglaUtils.formatNumber(cycles, true)}"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Target preset chips on Row 1 (never cramped on narrow phones)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(33, 99, 100).forEach { preset ->
                    FilterChip(
                        selected = target == preset,
                        onClick = { onSetTarget(preset) },
                        label = { Text(BanglaUtils.formatNumber(preset, isEn)) },
                        modifier = Modifier.testTag("tasbih_target_$preset")
                    )
                }
            }

            // Custom target + Reset buttons on Row 2 (full width, generous 48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showCustomTargetDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tasbih_custom_target_button")
                ) {
                    Text(tr(isEn, "কাস্টম লক্ষ্য", "Custom Target"))
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tasbih_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr(isEn, "রিসেট", "Reset"))
                }
            }
        }
    }

    if (showCustomTargetDialog) {
        var input by remember { mutableStateOf(target.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomTargetDialog = false },
            title = { Text(tr(isEn, "কাস্টম তাসবিহ লক্ষ্য", "Custom Tasbih Target")) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = {
                        Text(
                            tr(isEn, "সংখ্যা লিখুন (যেমন: ১০০০)", "Enter target count (e.g., 1000)")
                        )
                    },
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
                    Text(tr(isEn, "সেট করুন", "Set"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTargetDialog = false }) {
                    Text(tr(isEn, "বাতিল", "Cancel"))
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
