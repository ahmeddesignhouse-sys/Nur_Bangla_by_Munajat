package com.example.ui.screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.model.AccentTheme
import com.example.data.model.AdhanAlertMode
import com.example.data.model.AsrMethod
import com.example.data.model.BangladeshiDistricts
import com.example.data.model.BdDistrict
import com.example.data.model.CalculationMethod
import com.example.data.model.ThemeMode
import com.example.data.model.WaqtType
import com.example.ui.NoorUiState
import com.example.ui.theme.StatusForbiddenColor
import com.example.util.BanglaUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    uiState: NoorUiState,
    onSelectDistrict: (BdDistrict) -> Unit,
    onGpsResolved: (Double, Double, String?, Boolean) -> Unit,
    onAsrMethodChanged: (AsrMethod) -> Unit,
    onCalculationMethodChanged: (CalculationMethod) -> Unit,
    onWaqtAlertModeChanged: (WaqtType, AdhanAlertMode) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAccentThemeChanged: (AccentTheme) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showDistrictSheet by remember { mutableStateOf(false) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var alertDialogWaqt by remember { mutableStateOf<WaqtType?>(null) }

    val fetchGps: (Boolean) -> Unit = { showToast ->
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        scope.launch {
                            val place = reverseGeocodeCityCountry(context, location.latitude, location.longitude)
                            onGpsResolved(location.latitude, location.longitude, place, showToast)
                        }
                    } else {
                        client.lastLocation.addOnSuccessListener { last ->
                            if (last != null) {
                                scope.launch {
                                    val place = reverseGeocodeCityCountry(context, last.latitude, last.longitude)
                                    onGpsResolved(last.latitude, last.longitude, place, showToast)
                                }
                            } else if (showToast) {
                                onStatusMessage("জিপিএস লোকেশন পাওয়া যায়নি; জেলা তালিকা থেকে বেছে নিন।")
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    if (showToast) {
                        onStatusMessage("জিপিএস লোকেশন নিতে সমস্যা হয়েছে।")
                    }
                }
        } catch (_: SecurityException) {
            if (showToast) {
                onStatusMessage("লোকেশন অনুমতি প্রয়োজন।")
            }
        }
    }

    // Automatically detect GPS on initial launch if location permission is already granted
    LaunchedEffect(Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm && (uiState.settings.useGps || uiState.settings.gpsLatitude == null)) {
            fetchGps(false)
        }
    }

    // Automatically refresh GPS whenever user returns to app or system timezone changes while traveling
    DisposableEffect(lifecycleOwner, uiState.settings.useGps) {
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
                } else {
                    onGpsResolved(
                        uiState.activeLatitude,
                        uiState.activeLongitude,
                        uiState.settings.gpsLocationLabel,
                        false
                    )
                }
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
            onStatusMessage("লোকেশন অনুমতি দেওয়া হয়নি; ৬৪ জেলার তালিকা থেকে জেলা নির্বাচন করুন।")
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
                            contentDescription = "লোকেশন নির্বাচন",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = uiState.locationDisplayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.locationSubtitle} • পরিবর্তন",
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
                        contentDescription = "থিম পরিবর্তন",
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
                        contentDescription = "অটো জিপিএস লোকেশন",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Hero Countdown Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_islamic_hero),
                    contentDescription = "পরবর্তী নামাজের সময়",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.80f),
                                    Color.Black.copy(alpha = 0.45f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            val activeLabel = if (uiState.currentWaqt != null && schedule != null) {
                                val startStr = BanglaUtils.formatTime12hBangla(schedule.minuteFor(uiState.currentWaqt))
                                val endStr = BanglaUtils.formatTime12hBangla(schedule.endMinuteFor(uiState.currentWaqt))
                                "চলমান: ${uiState.currentWaqt.titleBn} • $startStr - $endStr"
                            } else {
                                "নিষিদ্ধ/বিরতি সময়"
                            }
                            Text(
                                text = activeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMethodSheet = true },
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Text(
                                    text = schedule?.appliedMethodTitleBn ?: "ইসলামিক ফাউন্ডেশন",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    val next = uiState.nextWaqtInfo
                    Column {
                        Text(
                            text = if (next != null) {
                                "পরবর্তী ওয়াক্ত: ${next.waqt.titleBn} (${BanglaUtils.formatTime12hBangla(next.targetMinuteOfDay)})"
                            } else {
                                "সময়সূচী লোড হচ্ছে..."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (next != null) {
                                BanglaUtils.formatCountdownBangla(next.remainingSeconds)
                            } else {
                                "০০:০০:০০"
                            },
                            style = MaterialTheme.typography.displayLarge,
                            color = Color(0xFFFFD875),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "বাকি সময় (ঘণ্টা : মিনিট : সেকেন্ড)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }

        // Triple Date Strip (Hijri, Bangla, English)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.hijriDateBn,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "বাংলা: ${uiState.banglaDateBn}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ইংরেজি: ${uiState.englishDateBn}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        text = "আসর:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(AsrMethod.entries) { method ->
                    FilterChip(
                        selected = uiState.settings.asrMethod == method,
                        onClick = { onAsrMethodChanged(method) },
                        label = { Text(if (method == AsrMethod.HANAFI) "হানাফি" else "শাফেয়ী") }
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { showMethodSheet = true },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("পদ্ধতি: ${uiState.settings.calculationMethod.titleBn}") }
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
                        label = "সাহরি শেষ",
                        timeText = BanglaUtils.formatTime12hBangla(schedule.sehriEndMinute)
                    )
                    MiniSolarCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Restaurant,
                        label = "ইফতার",
                        timeText = BanglaUtils.formatTime12hBangla(schedule.iftarMinute)
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
                        label = "সূর্যোদয়",
                        timeText = BanglaUtils.formatTime12hBangla(schedule.sunriseMinute)
                    )
                    MiniSolarCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WbTwilight,
                        label = "সূর্যাস্ত",
                        timeText = BanglaUtils.formatTime12hBangla(schedule.sunsetMinute)
                    )
                }
            }

            // 5 Daily Prayers List (Start & End Time per Waqt)
            item {
                Text(
                    text = "পাঁচ ওয়াক্ত নামাজের শুরু ও শেষ সময়",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(WaqtType.entries) { waqt ->
                val isCurrent = uiState.currentWaqt == waqt
                val isNext = uiState.nextWaqtInfo?.waqt == waqt && !uiState.nextWaqtInfo.isTomorrow
                val alertMode = uiState.settings.alertConfig.modeFor(waqt)
                val startTimeStr = BanglaUtils.formatTime12hBangla(schedule.minuteFor(waqt))
                val endTimeStr = BanglaUtils.formatTime12hBangla(schedule.endMinuteFor(waqt))

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
                                        text = waqt.titleBn,
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
                                                text = "চলমান",
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
                                                text = "পরবর্তী",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = waqt.rakatInfoBn,
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
                                    contentDescription = "${waqt.titleBn} আযান সেটিংস",
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
                                        text = "ওয়াক্ত শুরু",
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
                                        text = "ওয়াক্ত শেষ",
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

            // Forbidden Times Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = StatusForbiddenColor.copy(alpha = 0.10f)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = StatusForbiddenColor
                            )
                            Text(
                                text = "নামাজের নিষিদ্ধ (মাকরুহ) সময়সমূহ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusForbiddenColor
                            )
                        }
                        schedule.forbiddenTimes.forEach { window ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = window.labelBn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${BanglaUtils.formatTime12hBangla(window.startMinute)} - ${BanglaUtils.formatTime12hBangla(window.endMinute)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusForbiddenColor
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
                    text = "নামাজ গণনা পদ্ধতি নির্বাচন করুন",
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
                            Text(method.titleBn, style = MaterialTheme.typography.titleMedium)
                            if (selected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "নির্বাচিত", tint = MaterialTheme.colorScheme.primary)
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
                    text = "${waqt.titleBn} ওয়াক্তের অ্যালার্ট মোড",
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(alertIconFor(mode), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(mode.titleBn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(mode.descriptionBn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (selected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "নির্বাচিত", tint = MaterialTheme.colorScheme.primary)
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
                    text = "অ্যাপের থিম নির্বাচন করুন",
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
                            label = { Text(mode.titleBn, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text("রঙের থিম (৬টি):", style = MaterialTheme.typography.labelLarge)
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
                                            text = accent.titleBn,
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
}

private suspend fun reverseGeocodeCityCountry(
    context: Context,
    lat: Double,
    lon: Double
): String? = withContext(Dispatchers.IO) {
    try {
        if (!Geocoder.isPresent()) return@withContext null
        val locales = listOf(Locale("bn", "BD"), Locale.ENGLISH)
        for (locale in locales) {
            val geocoder = Geocoder(context, locale)
            val first: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(4000L) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (cont.isActive) cont.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            }
            if (first != null) {
                val city = first.locality ?: first.subAdminArea ?: first.adminArea
                val country = first.countryName
                val joined = listOfNotNull(city, country)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(", ")
                if (joined.isNotBlank()) return@withContext joined
            }
        }
        null
    } catch (_: Exception) {
        null
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DistrictPickerSheet(
    selectedDistrictId: String,
    onSelect: (BdDistrict) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) BangladeshiDistricts.ALL_64
        else BangladeshiDistricts.ALL_64.filter {
            it.nameBn.contains(searchQuery.trim(), ignoreCase = true) ||
                it.divisionBn.contains(searchQuery.trim(), ignoreCase = true) ||
                it.id.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "বাংলাদেশের ৬৪ জেলা নির্বাচন করুন",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("জেলার নাম বা বিভাগ লিখুন (যেমন: ঢাকা, সিলেট)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.id }) { district ->
                    val isSelected = district.id == selectedDistrictId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onSelect(district) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = district.nameBn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${district.divisionBn} বিভাগ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "নির্বাচিত",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("বন্ধ করুন")
            }
        }
    }
}

fun alertIconFor(mode: AdhanAlertMode): ImageVector = when (mode) {
    AdhanAlertMode.FULL_ADHAN -> Icons.Default.VolumeUp
    AdhanAlertMode.BEEP -> Icons.Default.NotificationsActive
    AdhanAlertMode.VIBRATE -> Icons.Default.Vibration
    AdhanAlertMode.SILENT -> Icons.Default.NotificationsOff
}
