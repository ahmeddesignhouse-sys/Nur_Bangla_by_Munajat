package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.NoorUiState
import com.example.util.BanglaUtils
import com.example.util.HijriCalendarEngine
import com.example.util.LocalAppLanguage
import com.example.util.RamadanDayEntry
import com.example.util.RamadanEngine
import com.example.util.isEnglishLang
import com.example.util.tr
import java.util.Calendar
import java.util.TimeZone

@Composable
fun RamadanCalendarScreen(
    uiState: NoorUiState,
    onRamadanModeChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val isEn = isEnglishLang(LocalAppLanguage.current)
    val targetTz = remember(uiState.activeTimeZoneId) { TimeZone.getTimeZone(uiState.activeTimeZoneId) }
    val todayCal = remember(targetTz) { Calendar.getInstance(targetTz) }
    val defaultYear = remember(todayCal, uiState.settings.hijriDayOffset) {
        RamadanEngine.defaultRamadanHijriYear(todayCal, uiState.settings.hijriDayOffset)
    }

    var ramadanYear by remember(defaultYear) { mutableIntStateOf(defaultYear) }
    var ashraFilter by remember { mutableIntStateOf(0) } // 0 = All 30, 1 = 1..10, 2 = 11..20, 3 = 21..30

    val fullTimetable = remember(
        ramadanYear,
        uiState.activeLatitude,
        uiState.activeLongitude,
        uiState.activeTimeZoneId,
        uiState.settings.asrMethod,
        uiState.settings.calculationMethod,
        uiState.settings.hijriDayOffset
    ) {
        RamadanEngine.buildRamadanTimetable(
            ramadanHijriYear = ramadanYear,
            latitude = uiState.activeLatitude,
            longitude = uiState.activeLongitude,
            timeZone = targetTz,
            asrMethod = uiState.settings.asrMethod,
            calculationMethod = uiState.settings.calculationMethod,
            hijriDayOffset = uiState.settings.hijriDayOffset,
            todayCalendar = todayCal
        )
    }

    val displayedDays = remember(fullTimetable, ashraFilter) {
        when (ashraFilter) {
            1 -> fullTimetable.filter { it.ramadanDay in 1..10 }
            2 -> fullTimetable.filter { it.ramadanDay in 11..20 }
            3 -> fullTimetable.filter { it.ramadanDay in 21..30 }
            else -> fullTimetable
        }
    }

    val schedule = uiState.todaySchedule
    val countdown = uiState.sehriIftarCountdown

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("ramadan_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr(isEn, "পিছনে যান", "Back")
                    )
                }
                Column {
                    Text(
                        text = tr(isEn, "রমজান ৩০ দিন ক্যালেন্ডার", "Ramadan 30-Day Calendar"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.locationDisplayTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Sehri / Iftar Countdown Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ramadan_countdown_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = countdown?.shortEventTitle(isEn)
                                        ?: tr(isEn, "সাহরি ও ইফতার", "Sehri & Iftar"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = tr(isEn, "অফলাইন সময়সূচী", "Offline Timetable"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = countdown?.label(isEn)
                                ?: tr(isEn, "সময় গণনা হচ্ছে...", "Calculating countdown..."),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("ramadan_live_countdown_text")
                        )

                        if (schedule != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = tr(isEn, "আজকের সাহরি শেষ", "Today's Sehri Ends"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = BanglaUtils.formatTime12h(schedule.sehriEndMinute, isEn),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${tr(isEn, "ফজর", "Fajr")}: ${BanglaUtils.formatTime12h(schedule.fajrMinute, isEn)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = tr(isEn, "আজকের ইফতার", "Today's Iftar"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = BanglaUtils.formatTime12h(schedule.iftarMinute, isEn),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${tr(isEn, "মাগরিব", "Maghrib")}: ${BanglaUtils.formatTime12h(schedule.maghribMinute, isEn)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${uiState.locationDisplayTitle} • ${uiState.locationSubtitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Ramadan Mode Toggle Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(isEn, "রমজান মোড", "Ramadan Mode"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    isEn,
                                    "চালু থাকলে হোম স্ক্রিনে সাহরি ও ইফতারের লাইভ কাউন্টডাউন প্রধানভাবে দেখাবে",
                                    "When ON, Home displays Sehri & Iftar countdown prominently"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.settings.ramadanMode,
                            onCheckedChange = onRamadanModeChanged,
                            modifier = Modifier.testTag("ramadan_mode_switch")
                        )
                    }
                }
            }

            // Hijri Year Selector + Ashra Filter
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { ramadanYear -= 1 },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = tr(isEn, "আগের বছর", "Previous Ramadan year")
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = tr(
                                        isEn,
                                        "রমজান ${BanglaUtils.toBanglaNumber(ramadanYear)} হিজরি",
                                        "Ramadan $ramadanYear AH"
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val firstG = fullTimetable.firstOrNull()?.gregorianDate
                                val lastG = fullTimetable.lastOrNull()?.gregorianDate
                                if (firstG != null && lastG != null) {
                                    Text(
                                        text = "${HijriCalendarEngine.formatGregorianFull(firstG, isEn)} – ${HijriCalendarEngine.formatGregorianFull(lastG, isEn)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { ramadanYear += 1 },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = tr(isEn, "পরের বছর", "Next Ramadan year")
                                )
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val tabs = listOf(
                                0 to tr(isEn, "সব ৩০ দিন", "All 30 Days"),
                                1 to tr(isEn, "রহমত (১-১০)", "Rahmah (1–10)"),
                                2 to tr(isEn, "মাগফিরাত (১১-২০)", "Maghfirah (11–20)"),
                                3 to tr(isEn, "নাজাত (২১-৩০)", "Najah (21–30)")
                            )
                            items(tabs) { (idx, label) ->
                                FilterChip(
                                    selected = ashraFilter == idx,
                                    onClick = { ashraFilter = idx },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // 30-Day Ramadan Timetable Cards
            items(displayedDays, key = { it.ramadanDay }) { entry ->
                RamadanDayRowCard(entry = entry, isEnglish = isEn)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RamadanDayRowCard(
    entry: RamadanDayEntry,
    isEnglish: Boolean
) {
    val bgColor = if (entry.isToday) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (entry.isToday) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                } else Modifier
            )
            .testTag("ramadan_day_${entry.ramadanDay}"),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (entry.isToday) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (entry.isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = BanglaUtils.formatNumber(entry.ramadanDay, isEnglish),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.isToday) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(
                            text = HijriCalendarEngine.formatHijriDate(entry.hijriDate, isEnglish),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = HijriCalendarEngine.formatGregorianFull(entry.gregorianDate, isEnglish),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (entry.isToday) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tr(isEnglish, "আজ", "Today"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Spacious 2x2 grid so Bangla Sehri/Fajr/Maghrib/Iftar labels & times never squeeze
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RamadanTimeColumn(
                        label = tr(isEnglish, "সাহরি শেষ", "Sehri End"),
                        value = BanglaUtils.formatTime12h(entry.sehriEndMinute, isEnglish),
                        highlight = true,
                        modifier = Modifier.weight(1f)
                    )
                    RamadanTimeColumn(
                        label = tr(isEnglish, "ফজর", "Fajr"),
                        value = BanglaUtils.formatTime12h(entry.fajrMinute, isEnglish),
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RamadanTimeColumn(
                        label = tr(isEnglish, "মাগরিব", "Maghrib"),
                        value = BanglaUtils.formatTime12h(entry.maghribMinute, isEnglish),
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                    RamadanTimeColumn(
                        label = tr(isEnglish, "ইফতার", "Iftar"),
                        value = BanglaUtils.formatTime12h(entry.iftarMinute, isEnglish),
                        highlight = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RamadanTimeColumn(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
