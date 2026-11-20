package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PrayerLogEntity
import com.example.data.model.PrayerStatus
import com.example.data.model.WaqtType
import com.example.ui.NoorUiState
import com.example.ui.theme.StatusLateColor
import com.example.ui.theme.StatusOnTimeColor
import com.example.ui.theme.StatusQazaColor
import com.example.util.BanglaUtils
import java.util.Calendar

@Composable
fun PrayerTrackerScreen(
    uiState: NoorUiState,
    onSelectDate: (String) -> Unit,
    onUpdatePrayerStatus: (String, WaqtType, PrayerStatus) -> Unit,
    onUpdatePrayerNote: (String, String) -> Unit
) {
    val recentDates = remember {
        val today = Calendar.getInstance()
        (0..13).map { offset ->
            val cal = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = BanglaUtils.getDateKey(cal)
            val label = if (offset == 0) "আজ" else "${BanglaUtils.toBanglaNumber(cal.get(Calendar.DAY_OF_MONTH))}"
            key to label
        }
    }

    val selectedDateKey = uiState.selectedTrackerDateKey
    val currentLog = uiState.prayerLogs.find { it.dateKey == selectedDateKey }
    var noteDraft by remember(selectedDateKey) { mutableStateOf(currentLog?.note ?: "") }
    LaunchedEffect(currentLog?.note) {
        if (currentLog != null && noteDraft.isEmpty()) {
            noteDraft = currentLog.note
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "দৈনিক সালাত ট্র্যাকার",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = BanglaUtils.formatDateKeyBangla(selectedDateKey),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = if (uiState.cloudSyncError == null) {
                        StatusOnTimeColor.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.cloudSyncError == null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (uiState.cloudSyncError == null) StatusOnTimeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (uiState.cloudSyncError == null) "সিঙ্ক সক্রিয়" else "অফলাইন",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Date Strip
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentDates, key = { it.first }) { (dateKey, shortLabel) ->
                    val isSelected = dateKey == selectedDateKey
                    val log = uiState.prayerLogs.find { it.dateKey == dateKey }
                    val completedCount = if (log == null) 0 else {
                        WaqtType.entries.count {
                            log.statusFor(it) == PrayerStatus.ON_TIME || log.statusFor(it) == PrayerStatus.LATE
                        }
                    }

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .clickable { onSelectDate(dateKey) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = shortLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${BanglaUtils.toBanglaNumber(completedCount)}/৫",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 5 Waqt Status Selector Cards
        items(WaqtType.entries) { waqt ->
            val currentStatus = currentLog?.statusFor(waqt) ?: PrayerStatus.NONE

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
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
                        Text(
                            text = waqt.titleBn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentStatus.titleBn,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColorFor(currentStatus),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            PrayerStatus.ON_TIME,
                            PrayerStatus.LATE,
                            PrayerStatus.QAZA,
                            PrayerStatus.NONE
                        ).forEach { status ->
                            val selected = currentStatus == status
                            val chipColor = statusColorFor(status)
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdatePrayerStatus(selectedDateKey, waqt, status) },
                                label = {
                                    Text(
                                        text = if (status == PrayerStatus.NONE) "খালি" else status.titleBn,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor.copy(alpha = 0.18f),
                                    selectedLabelColor = chipColor
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tracker_${waqt.key}_${status.name}")
                            )
                        }
                    }
                }
            }
        }

        // Daily Reflection Note
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("দৈনিক আমল ও নোট", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it.take(500) },
                        placeholder = { Text("আজকের নামাজ বা তিলাওয়াত সম্পর্কে নোট লিখুন...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                    Button(
                        onClick = { onUpdatePrayerNote(selectedDateKey, noteDraft) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("নোট সংরক্ষণ করুন")
                    }
                }
            }
        }

        // Archive History Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ইতিহাস আর্কাইভ (${BanglaUtils.toBanglaNumber(uiState.prayerLogs.size)} দিন)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.prayerLogs.isEmpty()) {
            item {
                Text(
                    text = "এখনো কোনো দিনের নামাজ সংরক্ষিত হয়নি। ওপরের তালিকা থেকে আজকের নামাজ চিহ্নিত করুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.prayerLogs, key = { it.dateKey }) { log ->
                HistoryRowCard(log = log, onClick = { onSelectDate(log.dateKey) })
            }
        }
    }
}

@Composable
private fun HistoryRowCard(log: PrayerLogEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = BanglaUtils.formatDateKeyBangla(log.dateKey),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (log.note.isNotBlank()) {
                    Text(
                        text = log.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                WaqtType.entries.forEach { waqt ->
                    val status = log.statusFor(waqt)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColorFor(status))
                    )
                }
            }
        }
    }
}

fun statusColorFor(status: PrayerStatus): Color = when (status) {
    PrayerStatus.ON_TIME -> StatusOnTimeColor
    PrayerStatus.LATE -> StatusLateColor
    PrayerStatus.QAZA -> StatusQazaColor
    PrayerStatus.NONE -> Color(0xFF9AA3A0)
}
