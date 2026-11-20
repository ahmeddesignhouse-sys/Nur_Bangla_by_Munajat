package com.example.ui.screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.model.AccentTheme
import com.example.data.model.AdhanAlertMode
import com.example.data.model.AsrMethod
import com.example.data.model.BdDistrict
import com.example.data.model.CalculationMethod
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.NextWaqtInfo
import com.example.data.model.ThemeMode
import com.example.data.model.WaqtType
import com.example.ui.NoorUiState
import com.example.ui.components.DailyShareSheet
import com.example.ui.components.DistrictPickerSheet
import com.example.util.BanglaUtils
import com.example.util.DailyInspirationEntry
import com.example.util.LocalAppLanguage
import com.example.util.LocationHelper
import com.example.util.isEnglishLang
import com.example.util.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    uiState: NoorUiState,
    onSelectDistrict: (BdDistrict) -> Unit,
    onGpsResolved: (Double, Double, String?, Boolean) -> Unit,
    onTimezoneChanged: () -> Unit,
    onAsrMethodChanged: (AsrMethod) -> Unit,
    onCalculationMethodChanged: (CalculationMethod) -> Unit,
    onWaqtAlertModeChanged: (WaqtType, AdhanAlertMode) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAccentThemeChanged: (AccentTheme) -> Unit,
    onOpenHijriCalendar: () -> Unit,
    onOpenRamadanCalendar: () -> Unit,
    onOpenDailyInspiration: () -> Unit,
    onOpenQuranTab: () -> Unit,
    onRamadanModeChanged: (Boolean) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isEn = isEnglishLang(LocalAppLanguage.current)

    var showDistrictSheet by remember { mutableStateOf(false) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var alertDialogWaqt by remember { mutableStateOf<WaqtType?>(null) }
    var shareEntry by remember { mutableStateOf<DailyInspirationEntry?>(null) }
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val fetchGps: (Boolean) -> Unit = { showToast ->
        LocationHelper.fetchCurrentGpsLocation(
            context = context,
            scope = scope,
            isEnglish = isEn,
            showToast = showToast,
            onResolved = onGpsResolved,
            onError = onStatusMessage
        )
    }

    // Auto-detect GPS on launch if permission is already granted
    LaunchedEffect(Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm && (uiState.settings.useGps || uiState.settings.gpsLatitude == null)) {
            fetchGps(false)
        }
    }

    // Refresh GPS on resume (when GPS is ON) and react immediately to device timezone changes
    DisposableEffect(lifecycleOwner, uiState.settings.useGps, isEn) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.settings.useGps) {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    fetchGps(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val tzReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm && uiState.settings.useGps) {
                    fetchGps(false)
                }
                onTimezoneChanged()
            }
        }
        val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
        ContextCompat.registerReceiver(context, tzReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                context.unregisterReceiver(tzReceiver)
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchGps(true)
        } else {
            onStatusMessage(
                tr(
                    isEn,
                    "লোকেশন অনুমতি দেওয়া হয়নি; ৬৪ জেলার তালিকা থেকে জেলা নির্বাচন করুন।",
                    "Location permission denied; choose a district from the 64-district list."
                )
            )
            showDistrictSheet = true
        }
    }

    val schedule = uiState.todaySchedule

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Location & Timezone Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showDistrictSheet = true }
                        .testTag("district_selector_button"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = tr(isEn, "লোকেশন নির্বাচন", "Select location"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = uiState.locationDisplayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.locationSubtitle} • ${tr(isEn, "পরিবর্তন", "Change")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showThemeSheet = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        .testTag("quick_theme_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = tr(isEn, "থিম পরিবর্তন", "Change theme"),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            fetchGps(true)
                        } else {
                            locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .testTag("gps_detect_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = tr(isEn, "অটো জিপিএস লোকেশন", "Auto GPS location"),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Clean Mosque Hero Banner — ONLY App Name (zero prayer text over photo)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .testTag("home_hero_banner")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_islamic_hero),
                    contentDescription = tr(isEn, "নূর বাংলা", "Nur Bangla"),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "নূর বাংলা",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    if (isEn) {
                        Text(
                            text = "Nur Bangla",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFE28A)
                        )
                    }
                }
            }
        }

        // Separate Solid Current Prayer Card (solid WHITE in light mode, solid DARK in dark mode)
        item {
            val cardBg = if (isDarkSurface) Color(0xFF1A2421) else Color.White
            val cardBorder = if (isDarkSurface) Color(0xFF2D4039) else Color(0xFFDCE5E1)
            val titleInk = if (isDarkSurface) Color.White else Color(0xFF0E2920)
            val subInk = if (isDarkSurface) Color(0xFFC4D4CE) else Color(0xFF3E524B)
            val accentInk = if (isDarkSurface) Color(0xFFFFD875) else MaterialTheme.colorScheme.primary
            val progressTrack = if (isDarkSurface) Color(0xFF2B3C36) else Color(0xFFE3ECE8)

            val currentWaqt = uiState.currentWaqt
            val next = uiState.nextWaqtInfo
            val elapsedProgress = calculateWaqtProgress(
                schedule = schedule,
                currentWaqt = currentWaqt,
                nextWaqtInfo = next
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(22.dp))
                    .testTag("home_current_prayer_card"),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (currentWaqt != null) {
                                    tr(isEn, "চলমান ওয়াক্ত", "Current Prayer")
                                } else {
                                    tr(isEn, "নিষিদ্ধ / বিরতি সময়", "Forbidden / Interval")
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showMethodSheet = true },
                            color = if (isDarkSurface) Color(0xFF263631) else Color(0xFFF0F5F3)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = accentInk,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = schedule?.appliedMethodTitle(isEn)
                                        ?: CalculationMethod.BANGLADESH_KARACHI.title(isEn),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = titleInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Large bold current prayer name + start–end time window
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentWaqt?.title(isEn)
                                    ?: tr(isEn, "বিরতি সময়", "Interval Time"),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = titleInk
                            )
                            if (currentWaqt != null && schedule != null) {
                                val startStr = BanglaUtils.formatTime12h(schedule.minuteFor(currentWaqt), isEn)
                                val endStr = BanglaUtils.formatTime12h(schedule.endMinuteFor(currentWaqt), isEn)
                                Text(
                                    text = "$startStr — $endStr",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = subInk
                                )
                            }
                        }

                        // Live Countdown block on right
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (next != null) {
                                    "${tr(isEn, "পরবর্তী", "Next")}: ${next.waqt.title(isEn)}"
                                } else {
                                    tr(isEn, "পরবর্তী ওয়াক্ত", "Next Prayer")
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = subInk
                            )
                            Text(
                                text = if (next != null) {
                                    BanglaUtils.formatCountdown(next.remainingSeconds, isEn)
                                } else {
                                    if (isEn) "00:00:00" else "০০:০০:০০"
                                },
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentInk
                            )
                            if (next != null) {
                                Text(
                                    text = BanglaUtils.formatTime12h(next.targetMinuteOfDay, isEn),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = subInk
                                )
                            }
                        }
                    }

                    // Elapsed Waqt Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { elapsedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .testTag("home_current_prayer_progress"),
                            color = accentInk,
                            trackColor = progressTrack
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr(isEn, "ওয়াক্তের অগ্রগতি", "Waqt Progress"),
                                style = MaterialTheme.typography.labelSmall,
                                color = subInk
                            )
                            val pct = (elapsedProgress * 100f).toInt()
                            Text(
                                text = "${BanglaUtils.formatNumber(pct, isEn)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentInk
                            )
                        }
                    }
                }
            }
        }

        // Ramadan Mode / Live Sehri & Iftar Countdown Banner (shown during Ramadan or when Ramadan Mode is ON)
        if (schedule != null && (uiState.settings.ramadanMode || uiState.isCurrentlyRamadan)) {
            item {
                val cd = uiState.sehriIftarCountdown
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenRamadanCalendar)
                        .testTag("open_ramadan_from_home"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = tr(isEn, "রমজান সাহরি ও ইফতার", "Ramadan Sehri & Iftar"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr(isEn, "৩০ দিন", "30 Days"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        if (cd != null) {
                            Text(
                                text = cd.label(isEn),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.testTag("home_ramadan_countdown")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = tr(isEn, "সাহরি শেষ", "Sehri Ends"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = BanglaUtils.formatTime12h(schedule.sehriEndMinute, isEn),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = tr(isEn, "ইফতার", "Iftar"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = BanglaUtils.formatTime12h(schedule.iftarMinute, isEn),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Triple Date Strip (Hijri, Bangla, English) — Tap to open Hijri Calendar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenHijriCalendar)
                    .testTag("open_hijri_calendar_home"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = uiState.hijriDate,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr(isEn, "ক্যালেন্ডার", "Calendar"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = tr(isEn, "হিজরি ক্যালেন্ডার খুলুন", "Open Hijri Calendar"),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${tr(isEn, "বাংলা", "Bangla")}: ${uiState.banglaDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${tr(isEn, "ইংরেজি", "Date")}: ${uiState.englishDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Daily Quran Goal Progress Card (Resets Daily)
        item {
            val goal = uiState.settings.dailyQuranGoal.coerceAtLeast(1)
            val readCount = uiState.dailyQuranReadCount
            val progress = (readCount.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
            val completed = readCount >= goal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenQuranTab)
                    .testTag("home_daily_quran_goal_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = tr(isEn, "দৈনিক কুরআন তিলাওয়াত লক্ষ্য", "Daily Quran Goal"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = tr(
                                    isEn,
                                    "আজ: ${BanglaUtils.toBanglaNumber(readCount)} / ${BanglaUtils.toBanglaNumber(goal)} আয়াত",
                                    "Today: $readCount / $goal Ayahs"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // Daily Hadith + Daily Ayah Card (Rotates daily; tap for full view & 7-day archive)
        val dailyInspiration = uiState.todayInspiration
        if (dailyInspiration != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenDailyInspiration)
                        .testTag("home_daily_inspiration_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = tr(isEn, "আজকের হাদিস ও আয়াত", "Hadith of the Day & Daily Ayah"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { shareEntry = dailyInspiration },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_daily_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = tr(isEn, "হাদিস ও আয়াত শেয়ার করুন", "Share Daily Hadith & Ayah"),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Hadith preview
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = tr(isEn, "হাদিস", "HADITH"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = dailyInspiration.hadith.meaning(isEn),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = dailyInspiration.hadith.source(isEn),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Ayah preview
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = tr(isEn, "আয়াত", "DAILY AYAH"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = dailyInspiration.ayah.meaning(isEn),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dailyInspiration.ayah.source(isEn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tr(isEn, "পূর্ণ বিবরণ ও বিগত ৭ দিন", "Full View & Last 7 Days"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Asr Method & Calculation Method Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text(
                        text = tr(isEn, "আসর:", "Asr:"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(AsrMethod.entries) { method ->
                    FilterChip(
                        selected = uiState.settings.asrMethod == method,
                        onClick = { onAsrMethodChanged(method) },
                        label = { Text(method.shortTitle(isEn)) }
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { showMethodSheet = true },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = {
                            Text(
                                "${tr(isEn, "পদ্ধতি", "Method")}: ${uiState.settings.calculationMethod.title(isEn)}"
                            )
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.settings.ramadanMode,
                        onClick = { onRamadanModeChanged(!uiState.settings.ramadanMode) },
                        leadingIcon = { Icon(Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(tr(isEn, "রমজান মোড", "Ramadan Mode")) },
                        modifier = Modifier.testTag("home_ramadan_mode_chip")
                    )
                }
            }
        }

        // Sehri, Sunrise, Sunset, Iftar Quick Grid
        if (schedule != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniSolarCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Restaurant,
                        label = tr(isEn, "সাহরি শেষ", "Sahri Ends"),
                        timeText = BanglaUtils.formatTime12h(schedule.sehriEndMinute, isEn)
                    )
                    MiniSolarCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Restaurant,
                        label = tr(isEn, "ইফতার", "Iftar"),
                        timeText = BanglaUtils.formatTime12h(schedule.iftarMinute, isEn)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniSolarCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WbSunny,
                        label = tr(isEn, "সূর্যোদয়", "Sunrise"),
                        timeText = BanglaUtils.formatTime12h(schedule.sunriseMinute, isEn)
                    )
                    MiniSolarCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WbTwilight,
                        label = tr(isEn, "সূর্যাস্ত", "Sunset"),
                        timeText = BanglaUtils.formatTime12h(schedule.sunsetMinute, isEn)
                    )
                }
            }

            // 5 Daily Prayers List (Start & End Time per Waqt)
            item {
                Text(
                    text = tr(
                        isEn,
                        "পাঁচ ওয়াক্ত নামাজের শুরু ও শেষ সময়",
                        "5 Daily Prayers — Start & End Times"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(WaqtType.entries) { waqt ->
                val isCurrent = uiState.currentWaqt == waqt
                val isNext = uiState.nextWaqtInfo?.waqt == waqt && !uiState.nextWaqtInfo.isTomorrow
                val alertMode = uiState.settings.alertConfig.modeFor(waqt)
                val startTimeStr = BanglaUtils.formatTime12h(schedule.minuteFor(waqt), isEn)
                val endTimeStr = BanglaUtils.formatTime12h(schedule.endMinuteFor(waqt), isEn)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { alertDialogWaqt = waqt }
                        .testTag("waqt_card_${waqt.key}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 3.dp else 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = waqt.title(isEn),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (isCurrent) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = tr(isEn, "চলমান", "Now"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isNext) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = tr(isEn, "পরবর্তী", "Next"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = waqt.rakatInfo(isEn),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            IconButton(
                                onClick = { alertDialogWaqt = waqt },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = alertIconFor(alertMode),
                                    contentDescription = tr(
                                        isEn,
                                        "${waqt.titleBn} আযান সেটিংস",
                                        "${waqt.titleEn} Adhan settings"
                                    ),
                                    tint = if (alertMode == AdhanAlertMode.SILENT) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = tr(isEn, "ওয়াক্ত শুরু", "Starts"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = startTimeStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = tr(isEn, "ওয়াক্ত শেষ", "Ends"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = endTimeStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Forbidden (Makruh) Prayer Times Card — High-contrast cream/amber palette
            item {
                val makruhBg = if (isDarkSurface) Color(0xFF2B2118) else Color(0xFFFFF8E1)
                val makruhBorder = if (isDarkSurface) Color(0xFF8C6239) else Color(0xFFE0B973)
                val makruhHeading = if (isDarkSurface) Color(0xFFFFE0B2) else Color(0xFF3E2723)
                val makruhBody = if (isDarkSurface) Color(0xFFFFF8E1) else Color(0xFF212121)
                val makruhIconTint = if (isDarkSurface) Color(0xFFFFCA28) else Color(0xFFB45309)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.2.dp, makruhBorder, RoundedCornerShape(18.dp))
                        .testTag("home_makruh_times_card"),
                    colors = CardDefaults.cardColors(containerColor = makruhBg),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = tr(isEn, "সতর্কতা", "Warning"),
                                tint = makruhIconTint,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = tr(
                                    isEn,
                                    "নামাজের নিষিদ্ধ (মাকরুহ) সময়সমূহ",
                                    "Forbidden (Makruh) Prayer Times"
                                ),
                                fontSize = 19.sp,
                                lineHeight = 25.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = makruhHeading
                            )
                        }
                        HorizontalDivider(color = makruhBorder.copy(alpha = 0.55f))
                        schedule.forbiddenTimes.forEach { window ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = window.label(isEn),
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = makruhBody,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 10.dp)
                                )
                                Text(
                                    text = "${BanglaUtils.formatTime12h(window.startMinute, isEn)} — ${BanglaUtils.formatTime12h(window.endMinute, isEn)}",
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = makruhHeading
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDistrictSheet) {
        DistrictPickerSheet(
            selectedDistrictId = if (uiState.settings.useGps) "" else uiState.activeDistrict.id,
            onSelect = { district ->
                onSelectDistrict(district)
                showDistrictSheet = false
            },
            onDismiss = { showDistrictSheet = false }
        )
    }

    if (showMethodSheet) {
        ModalBottomSheet(onDismissRequest = { showMethodSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tr(
                        isEn,
                        "নামাজ গণনা পদ্ধতি নির্বাচন করুন",
                        "Select Prayer Calculation Method"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                CalculationMethod.entries.forEach { method ->
                    val selected = uiState.settings.calculationMethod == method
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCalculationMethodChanged(method)
                                showMethodSheet = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(method.title(isEn), style = MaterialTheme.typography.titleMedium)
                            if (selected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = tr(isEn, "নির্বাচিত", "Selected"),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    alertDialogWaqt?.let { waqt ->
        val currentMode = uiState.settings.alertConfig.modeFor(waqt)
        ModalBottomSheet(onDismissRequest = { alertDialogWaqt = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tr(
                        isEn,
                        "${waqt.titleBn} ওয়াক্তের অ্যালার্ট মোড",
                        "${waqt.titleEn} Alert Mode"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                AdhanAlertMode.entries.forEach { mode ->
                    val selected = mode == currentMode
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onWaqtAlertModeChanged(waqt, mode)
                                alertDialogWaqt = null
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(alertIconFor(mode), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(mode.title(isEn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(mode.description(isEn), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (selected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = tr(isEn, "নির্বাচিত", "Selected"),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = tr(isEn, "অ্যাপের থিম নির্বাচন করুন", "Choose App Theme"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.settings.themeMode == mode,
                            onClick = { onThemeModeChanged(mode) },
                            label = { Text(mode.title(isEn), style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = tr(isEn, "রঙের থিম (৬টি):", "Color Themes (6):"),
                    style = MaterialTheme.typography.labelLarge
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccentTheme.entries.chunked(3).forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowThemes.forEach { accent ->
                                val primaryColor = Color(accent.primaryColorHex)
                                val secondaryColor = Color(accent.secondaryColorHex)
                                val selected = uiState.settings.accentTheme == accent
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onAccentThemeChanged(accent) }
                                        .then(
                                            if (selected) Modifier.border(2.dp, primaryColor, RoundedCornerShape(14.dp))
                                            else Modifier
                                        ),
                                    color = primaryColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(primaryColor)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(secondaryColor)
                                            )
                                        }
                                        Text(
                                            text = accent.title(isEn),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    shareEntry?.let { entry ->
        DailyShareSheet(
            entry = entry,
            isEnglish = isEn,
            onStatusMessage = onStatusMessage,
            onDismiss = { shareEntry = null }
        )
    }
}

/**
 * Computes the elapsed progress [0f..1f] inside the currently active Waqt window
 * (handling overnight Isha wrap cleanly).
 */
fun calculateWaqtProgress(
    schedule: DailyPrayerSchedule?,
    currentWaqt: WaqtType?,
    nextWaqtInfo: NextWaqtInfo?
): Float {
    if (schedule == null || currentWaqt == null || nextWaqtInfo == null) return 0f
    val startMin = schedule.minuteFor(currentWaqt)
    val endMin = schedule.endMinuteFor(currentWaqt)
    val durationMinutes = if (endMin >= startMin) {
        endMin - startMin
    } else {
        (24 * 60 - startMin) + endMin
    }
    val totalSeconds = (durationMinutes * 60L).coerceAtLeast(60L)
    val remaining = nextWaqtInfo.remainingSeconds.coerceIn(0L, totalSeconds)
    return ((totalSeconds - remaining).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
}

@Composable
private fun MiniSolarCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    timeText: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(timeText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun alertIconFor(mode: AdhanAlertMode): ImageVector = when (mode) {
    AdhanAlertMode.FULL_ADHAN -> Icons.AutoMirrored.Filled.VolumeUp
    AdhanAlertMode.BEEP -> Icons.Default.NotificationsActive
    AdhanAlertMode.VIBRATE -> Icons.Default.Vibration
    AdhanAlertMode.SILENT -> Icons.Default.NotificationsOff
}
