package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NoorUiState
import com.example.util.BanglaUtils
import com.example.util.GregorianDate
import com.example.util.HijriCalendarEngine
import com.example.util.HijriDate
import com.example.util.HijriEventOccurrence
import com.example.util.HijriMonthDayCell
import com.example.util.LocalAppLanguage
import com.example.util.isEnglishLang
import com.example.util.tr
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarScreen(
    uiState: NoorUiState,
    onBack: () -> Unit,
    onHijriOffsetChanged: (Int) -> Unit
) {
    val isEn = isEnglishLang(LocalAppLanguage.current)
    val dayOffset = uiState.settings.hijriDayOffset
    val targetTz = remember(uiState.activeTimeZoneId) { TimeZone.getTimeZone(uiState.activeTimeZoneId) }
    val todayCal = remember(targetTz, dayOffset) { Calendar.getInstance(targetTz) }
    val todayGregorian = remember(todayCal) {
        GregorianDate(
            year = todayCal.get(Calendar.YEAR),
            month = todayCal.get(Calendar.MONTH) + 1,
            day = todayCal.get(Calendar.DAY_OF_MONTH)
        )
    }
    val todayHijri = remember(todayCal, dayOffset) {
        HijriCalendarEngine.fromGregorian(todayCal, dayOffset)
    }

    var displayedYear by remember { mutableIntStateOf(todayHijri.year) }
    var displayedMonth by remember { mutableIntStateOf(todayHijri.month) }
    var selectedHijriDay by remember { mutableIntStateOf(todayHijri.day) }
    var showAllYearEvents by remember { mutableStateOf(false) }
    var activeEventDetail by remember { mutableStateOf<HijriEventOccurrence?>(null) }

    LaunchedEffect(dayOffset) {
        // Keep selection valid if moon-sighting adjustment changes
        val maxDay = HijriCalendarEngine.daysInHijriMonth(displayedYear, displayedMonth)
        if (selectedHijriDay > maxDay) selectedHijriDay = maxDay
    }

    val monthCells = remember(displayedYear, displayedMonth, dayOffset, todayCal) {
        HijriCalendarEngine.buildMonthCells(displayedYear, displayedMonth, dayOffset, todayCal)
    }
    val selectedCell = monthCells.find { it.hijriDate.day == selectedHijriDay } ?: monthCells.firstOrNull()
    val yearEvents = remember(displayedYear, dayOffset) {
        HijriCalendarEngine.eventsForHijriYear(displayedYear, dayOffset)
    }
    val visibleEvents = remember(yearEvents, displayedMonth, showAllYearEvents) {
        if (showAllYearEvents) yearEvents
        else yearEvents.filter { it.hijriDate.month == displayedMonth }
    }

    val shiftMonth: (Int) -> Unit = { delta ->
        val (ny, nm) = HijriCalendarEngine.shiftHijriMonth(displayedYear, displayedMonth, delta)
        displayedYear = ny
        displayedMonth = nm
        val maxDay = HijriCalendarEngine.daysInHijriMonth(ny, nm)
        selectedHijriDay = if (ny == todayHijri.year && nm == todayHijri.month) {
            todayHijri.day.coerceAtMost(maxDay)
        } else {
            1
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header Bar
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
                        .testTag("hijri_calendar_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr(isEn, "পিছনে যান", "Back")
                    )
                }
                Column {
                    Text(
                        text = tr(isEn, "হিজরি ক্যালেন্ডার", "Hijri Calendar"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(isEn, "ইসলামিক তারিখ ও বিশেষ দিবস", "Islamic Dates & Events"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(
                onClick = {
                    displayedYear = todayHijri.year
                    displayedMonth = todayHijri.month
                    selectedHijriDay = todayHijri.day
                },
                modifier = Modifier.testTag("hijri_today_button")
            ) {
                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(tr(isEn, "আজ", "Today"))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Today Side-by-Side Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp)
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tr(isEn, "আজকের তারিখ", "Today's Date"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (dayOffset != 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    val sign = if (dayOffset > 0) "+" else ""
                                    Text(
                                        text = "${tr(isEn, "চাঁদ সমন্বয়", "Hijri Adj.")}: ${BanglaUtils.formatDigits("$sign$dayOffset", isEn)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = tr(isEn, "হিজরি তারিখ", "Hijri Date"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = HijriCalendarEngine.formatHijriDate(todayHijri, isEn),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = tr(isEn, "ইংরেজি তারিখ", "Gregorian Date"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = HijriCalendarEngine.formatGregorianFull(todayGregorian, isEn),
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

            // Month Grid Card (Swipe left/right or tap arrows to change Hijri month)
            item {
                var accumulatedDrag by remember { mutableFloatStateOf(0f) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(displayedYear, displayedMonth) {
                            detectHorizontalDragGestures(
                                onDragStart = { accumulatedDrag = 0f },
                                onHorizontalDrag = { _, dragAmount -> accumulatedDrag += dragAmount },
                                onDragEnd = {
                                    if (accumulatedDrag < -80f) {
                                        shiftMonth(1)
                                    } else if (accumulatedDrag > 80f) {
                                        shiftMonth(-1)
                                    }
                                    accumulatedDrag = 0f
                                }
                            )
                        }
                        .testTag("hijri_month_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Month Header with Prev / Next buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { shiftMonth(-1) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("hijri_prev_month")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = tr(isEn, "আগের মাস", "Previous month")
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${HijriCalendarEngine.monthName(displayedMonth, isEn)} ${BanglaUtils.formatNumber(displayedYear, isEn)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = HijriCalendarEngine.formatGregorianMonthSpan(monthCells, isEn),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { shiftMonth(1) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("hijri_next_month")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = tr(isEn, "পরের মাস", "Next month")
                                )
                            }
                        }

                        // Weekday Header Row (Sun..Sat)
                        val weekdays = if (isEn) HijriCalendarEngine.shortWeekdaysEn else HijriCalendarEngine.shortWeekdaysBn
                        Row(modifier = Modifier.fillMaxWidth()) {
                            weekdays.forEachIndexed { idx, dayName ->
                                Text(
                                    text = dayName,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (idx == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        // Calendar Days Matrix (7 columns)
                        val leadingEmpty = monthCells.firstOrNull()?.dayOfWeek ?: 0
                        val totalSlots = leadingEmpty + monthCells.size
                        val rowCount = (totalSlots + 6) / 7

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (row in 0 until rowCount) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (col in 0 until 7) {
                                        val slotIndex = row * 7 + col
                                        val cellIndex = slotIndex - leadingEmpty
                                        if (cellIndex in monthCells.indices) {
                                            val cell = monthCells[cellIndex]
                                            HijriDayTile(
                                                cell = cell,
                                                isSelected = cell.hijriDate.day == selectedHijriDay,
                                                isEnglish = isEn,
                                                onClick = {
                                                    selectedHijriDay = cell.hijriDate.day
                                                    val firstEvent = cell.events.firstOrNull()
                                                    if (firstEvent != null) {
                                                        activeEventDetail = HijriEventOccurrence(
                                                            event = firstEvent,
                                                            hijriDate = cell.hijriDate,
                                                            gregorianDate = cell.gregorianDate
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // Subtle swipe hint + quick moon-sighting +/-1 selector (stacked to prevent narrow-column squeeze)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tr(isEn, "মাস বদলাতে সোয়াইপ করুন", "Swipe left/right to change month"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(-1, 0, 1).forEach { offset ->
                                    val label = when (offset) {
                                        -1 -> tr(isEn, "-১ দিন", "-1d")
                                        1 -> tr(isEn, "+১ দিন", "+1d")
                                        else -> tr(isEn, "০ (অটো)", "0 (Auto)")
                                    }
                                    FilterChip(
                                        selected = dayOffset == offset,
                                        onClick = { onHijriOffsetChanged(offset) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hijri_offset_chip_$offset")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Selected Day Side-by-Side Detail Card
            if (selectedCell != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = tr(isEn, "নির্বাচিত দিনের বিবরণ", "Selected Day"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${HijriCalendarEngine.formatGregorianFull(selectedCell.gregorianDate, isEn)}  /  ${HijriCalendarEngine.formatHijriDate(selectedCell.hijriDate, isEn)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            selectedCell.events.forEach { ev ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            activeEventDetail = HijriEventOccurrence(
                                                event = ev,
                                                hijriDate = selectedCell.hijriDate,
                                                gregorianDate = selectedCell.gregorianDate
                                            )
                                        },
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Celebration,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ev.title(isEn),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = tr(isEn, "বিস্তারিত দেখতে ট্যাপ করুন", "Tap for event details"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Islamic Events Section Header + Filter Chips (stacked to prevent heading squeeze)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tr(isEn, "ইসলামিক দিবসসমূহ", "Islamic Events"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !showAllYearEvents,
                            onClick = { showAllYearEvents = false },
                            label = { Text(tr(isEn, "এই মাস", "This Month")) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("events_filter_month")
                        )
                        FilterChip(
                            selected = showAllYearEvents,
                            onClick = { showAllYearEvents = true },
                            label = { Text(tr(isEn, "সারা বছর", "All Year")) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("events_filter_year")
                        )
                    }
                }
            }

            if (visibleEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tr(
                                    isEn,
                                    "এই হিজরি মাসে কোনো নির্ধারিত প্রধান দিবস নেই।",
                                    "No major Islamic events in this Hijri month."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            TextButton(onClick = { showAllYearEvents = true }) {
                                Text(tr(isEn, "সারা বছরের দিবস দেখুন", "View All Year Events"))
                            }
                        }
                    }
                }
            } else {
                items(visibleEvents, key = { "${it.hijriDate.year}_${it.event.id}" }) { occ ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeEventDetail = occ }
                            .testTag("islamic_event_${occ.event.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (occ.event.isMajorEid) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (occ.event.isMajorEid) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (occ.event.isMajorEid) Icons.Default.Celebration else Icons.Default.Event,
                                    contentDescription = null,
                                    tint = if (occ.event.isMajorEid) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = occ.event.title(isEn),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${HijriCalendarEngine.formatHijriDate(occ.hijriDate, isEn)} • ${HijriCalendarEngine.formatGregorianFull(occ.gregorianDate, isEn)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Event Detail Bottom Sheet
    activeEventDetail?.let { occ ->
        ModalBottomSheet(onDismissRequest = { activeEventDetail = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = occ.event.title(isEn),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = tr(isEn, "হিজরি তারিখ", "Hijri Date"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = HijriCalendarEngine.formatHijriDate(occ.hijriDate, isEn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = tr(isEn, "ইংরেজি তারিখ", "Gregorian Date"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = HijriCalendarEngine.formatGregorianFull(occ.gregorianDate, isEn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = occ.event.description(isEn),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = tr(
                        isEn,
                        "* চাঁদ দেখার ওপর নির্ভর করে হিজরি তারিখ ১ দিন আগে বা পরে হতে পারে। সেটিংস থেকে ±১ দিন সমন্বয় করা যাবে।",
                        "* Actual Islamic dates may vary by ±1 day depending on local moon sighting. You can adjust ±1 day in Settings."
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (occ.hijriDate.month != displayedMonth || occ.hijriDate.year != displayedYear) {
                    OutlinedButton(
                        onClick = {
                            displayedYear = occ.hijriDate.year
                            displayedMonth = occ.hijriDate.month
                            selectedHijriDay = occ.hijriDate.day
                            activeEventDetail = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr(isEn, "ক্যালেন্ডারে এই মাসে যান", "Jump to this month on calendar"))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HijriDayTile(
    cell: HijriMonthDayCell,
    isSelected: Boolean,
    isEnglish: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasEvent = cell.events.isNotEmpty()
    val hasMajorEid = cell.events.any { it.isMajorEid }
    val bgColor = when {
        cell.isToday -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        hasMajorEid -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val mainTextColor = when {
        cell.isToday -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val subTextColor = when {
        cell.isToday -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (isSelected && !cell.isToday) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = BanglaUtils.formatNumber(cell.hijriDate.day, isEnglish),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = mainTextColor
        )
        Text(
            text = HijriCalendarEngine.formatGregorianShort(cell.gregorianDate, isEnglish),
            fontSize = 9.sp,
            lineHeight = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            color = subTextColor
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !hasEvent -> androidx.compose.ui.graphics.Color.Transparent
                        cell.isToday -> MaterialTheme.colorScheme.onPrimary
                        hasMajorEid -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    }
                )
        )
    }
}
