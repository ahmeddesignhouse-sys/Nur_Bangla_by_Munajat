package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.alarm.AdhanAlarmReceiver
import com.example.data.model.AccentTheme
import com.example.data.model.AdhanAlertMode
import com.example.data.model.ThemeMode
import com.example.data.model.WaqtType
import com.example.ui.DailyPrayerSummary
import com.example.ui.NoorUiState
import com.example.ui.theme.StatusLateColor
import com.example.ui.theme.StatusOnTimeColor
import com.example.ui.theme.StatusQazaColor
import com.example.util.BanglaUtils
import com.example.util.ExportHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun ProfileSettingsScreen(
    uiState: NoorUiState,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAccentThemeChanged: (AccentTheme) -> Unit,
    onWaqtAlertModeChanged: (WaqtType, AdhanAlertMode) -> Unit,
    onPrePrayerMinutesChanged: (Int) -> Unit,
    onSignOutClick: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val userName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "মুসলিম ব্যবহারকারী"
    val userEmail = currentUser?.email ?: ""

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        onStatusMessage(
            if (granted) "আযান নোটিফিকেশন অনুমতি চালু হয়েছে"
            else "নোটিফিকেশন অনুমতি ছাড়া আযান অ্যালার্ট দেখানো যাবে না"
        )
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    ExportHelper.writePrayerLogsCsv(os, uiState.prayerLogs)
                }
                onStatusMessage("সালাত রিপোর্ট (CSV) সফলভাবে সেভ হয়েছে")
            } catch (e: Exception) {
                onStatusMessage("CSV সেভ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    ExportHelper.writePrayerLogsPdf(os, uiState.prayerLogs, userName)
                }
                onStatusMessage("সালাত রিপোর্ট (PDF) সফলভাবে সেভ হয়েছে")
            } catch (e: Exception) {
                onStatusMessage("PDF সেভ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User Profile & Sign Out Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (userEmail.isNotBlank()) {
                                Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "গুগল ক্লাউড সিঙ্ক যুক্ত",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusOnTimeColor
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOutClick,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(stringResource(R.string.sign_out))
                    }
                }
            }
        }

        // Streak & Total Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${BanglaUtils.toBanglaNumber(uiState.currentStreakDays)} দিন",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("টানা ৫ ওয়াক্ত স্ট্রিক", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusOnTimeColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${BanglaUtils.toBanglaNumber(uiState.totalOnTimePrayers)} ওয়াক্ত",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("মোট সময়মতো নামাজ", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 7-Day Progress Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "গত ৭ দিনের নামাজের অগ্রগতি",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    WeeklyPrayerBarChart(summaries = uiState.last7DaysSummary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendDot("সময়মতো", StatusOnTimeColor)
                        LegendDot("বিলম্বে", StatusLateColor)
                        LegendDot("কাজা", StatusQazaColor)
                    }
                }
            }
        }

        // CSV & PDF Data Export
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "সালাত লগ এক্সপোর্ট (CSV ও PDF)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "আপনার সব নামাজের রেকর্ড ফাইল আকারে ডিভাইসে ডাউনলোড করুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { csvExportLauncher.launch("noor_bangla_salat_logs.csv") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("CSV ডাউনলোড")
                        }
                        Button(
                            onClick = { pdfExportLauncher.launch("noor_bangla_salat_report.pdf") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("PDF ডাউনলোড")
                        }
                    }
                }
            }
        }

        // Theme Engine
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("থিম ও অ্যাকসেন্ট রঙ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

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

                    Text("থিমের রঙ বেছে নিন (৬টি থিম):", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentTheme.entries.chunked(3).forEach { rowThemes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowThemes.forEach { accent ->
                                    AccentSwatch(
                                        title = accent.titleBn,
                                        primaryColor = Color(accent.primaryColorHex),
                                        secondaryColor = Color(accent.secondaryColorHex),
                                        selected = uiState.settings.accentTheme == accent,
                                        onClick = { onAccentThemeChanged(accent) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("accent_swatch_${accent.name}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Adhan & Notification Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("আযান ও নামাজের সতর্কতা", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("নোটিফিকেশন অনুমতি দিন")
                        }
                    }

                    Text("নামাজের পূর্ব-সতর্কতা (মিনিট আগে):", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 5, 10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = uiState.settings.alertConfig.prePrayerMinutes == mins,
                                onClick = { onPrePrayerMinutesChanged(mins) },
                                label = {
                                    Text(if (mins == 0) "বন্ধ" else "${BanglaUtils.toBanglaNumber(mins)} মি.")
                                }
                            )
                        }
                    }

                    HorizontalDivider()

                    WaqtType.entries.forEach { waqt ->
                        val currentMode = uiState.settings.alertConfig.modeFor(waqt)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(waqt.titleBn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AdhanAlertMode.entries.forEach { mode ->
                                    val shortLabel = when (mode) {
                                        AdhanAlertMode.FULL_ADHAN -> "আযান"
                                        AdhanAlertMode.BEEP -> "বিপ"
                                        AdhanAlertMode.VIBRATE -> "ভাইব্রেট"
                                        AdhanAlertMode.SILENT -> "নীরব"
                                    }
                                    FilterChip(
                                        selected = currentMode == mode,
                                        onClick = { onWaqtAlertModeChanged(waqt, mode) },
                                        label = { Text(shortLabel, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                AdhanAlarmReceiver.triggerAlert(
                                    context = context,
                                    waqt = WaqtType.FAJR,
                                    mode = AdhanAlertMode.FULL_ADHAN
                                )
                                onStatusMessage("পরীক্ষামূলক আযান বাজানো হচ্ছে")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("আযান টেস্ট")
                        }
                        TextButton(
                            onClick = {
                                AdhanAlarmReceiver.stopActiveAudio()
                                onStatusMessage("আযান শব্দ বন্ধ করা হয়েছে")
                            }
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("বন্ধ করুন")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyPrayerBarChart(summaries: List<DailyPrayerSummary>) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            if (summaries.isEmpty()) return@Canvas
            val barWidth = (size.width / summaries.size) * 0.48f
            val spacing = size.width / summaries.size
            val maxUnits = 5f

            summaries.forEachIndexed { idx, day ->
                val x = idx * spacing + (spacing - barWidth) / 2f
                // Track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                var currentBottom = size.height
                val segments = listOf(
                    day.onTimeCount to StatusOnTimeColor,
                    day.lateCount to StatusLateColor,
                    day.qazaCount to StatusQazaColor
                )
                segments.forEach { (count, color) ->
                    if (count > 0) {
                        val h = (count / maxUnits) * size.height
                        currentBottom -= h
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, currentBottom),
                            size = Size(barWidth, h),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            summaries.forEach { day ->
                val dayPart = day.dateKey.takeLast(2)
                Text(
                    text = BanglaUtils.toBanglaDigits(dayPart),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun AccentSwatch(
    title: String,
    primaryColor: Color,
    secondaryColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
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
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
