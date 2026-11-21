package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DailyShareSheet
import com.example.ui.theme.AmiriFontFamily
import com.example.util.AppLinks
import com.example.util.BanglaUtils
import com.example.util.DailyInspirationEntry
import com.example.util.LocalAppLanguage
import com.example.util.isEnglishLang
import com.example.util.tr

@Composable
fun DailyInspirationScreen(
    entries: List<DailyInspirationEntry>,
    nextSlotCountdown: String = "",
    onStatusMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isEn = isEnglishLang(LocalAppLanguage.current)
    BackHandler(onBack = onBack)

    var selectedDateKey by remember(entries) {
        mutableStateOf(entries.firstOrNull()?.dateKey)
    }
    val activeEntry = entries.find { it.dateKey == selectedDateKey } ?: entries.firstOrNull()
    val previousDays = remember(entries) { entries.drop(1) }
    var shareSheetEntry by remember { mutableStateOf<DailyInspirationEntry?>(null) }

    val copyText: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Nur Bangla Daily", text))
        onStatusMessage(tr(isEn, "ক্লিপবোর্ডে কপি করা হয়েছে", "Copied to clipboard"))
    }

    val shareText: (String) -> Unit = { text ->
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(
            sendIntent,
            tr(isEn, "দৈনিক হাদিস ও আয়াত শেয়ার করুন", "Share Daily Hadith & Ayah")
        )
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            onStatusMessage(tr(isEn, "শেয়ার অ্যাপ পাওয়া যায়নি", "No share app available"))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("daily_inspiration_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr(isEn, "ফিরে যান", "Back")
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(isEn, "দৈনিক হাদিস ও আয়াত", "Hadith of the Day & Daily Ayah"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (activeEntry != null) {
                        Text(
                            text = "${activeEntry.gregorianDate(isEn)} • ${activeEntry.hijriDate(isEn)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (activeEntry == null) {
            BoxEmptyDaily(isEn)
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Day indicator + Full Copy & Share bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = dayLabel(activeEntry.daysAgo, isEn),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { copyText(activeEntry.formatShareText(isEn)) },
                            modifier = Modifier.testTag("daily_copy_all_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(tr(isEn, "কপি", "Copy"))
                        }
                        FilledTonalButton(
                            onClick = { shareSheetEntry = activeEntry },
                            modifier = Modifier.testTag("daily_share_all_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(tr(isEn, "কার্ড শেয়ার", "Share Card"))
                        }
                    }
                }
            }

            // Slot info + Special-day badge + Live next-slot countdown
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeEntry.specialDayLabel(isEn)?.let { specialLabel ->
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("daily_screen_special_day_badge")
                        ) {
                            Text(
                                text = "★ $specialLabel",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_screen_slot_countdown_bar")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${tr(isEn, "স্লট", "Slot")}: ${activeEntry.slotLabel(isEn)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (activeEntry.daysAgo == 0 && nextSlotCountdown.isNotBlank()) {
                                Text(
                                    text = nextSlotCountdown,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Full Hadith Card (Arabic + Bangla Meaning + English Meaning)
            item {
                val hadith = activeEntry.hadith
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_hadith_full_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = tr(isEn, "আজকের হাদিস", "Hadith of the Day"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = hadith.narrator(isEn),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        copyText(
                                            "${hadith.arabic}\n\nবাংলা: ${hadith.bangla}\nEnglish: ${hadith.english}\n— ${hadith.source(isEn)}\n\n${AppLinks.shareCaptionFooter(isEn)}"
                                        )
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = tr(isEn, "হাদিস কপি করুন", "Copy Hadith"),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        shareText(
                                            "${hadith.arabic}\n\nবাংলা: ${hadith.bangla}\nEnglish: ${hadith.english}\n— ${hadith.source(isEn)}\n\n${AppLinks.shareCaptionFooter(isEn)}"
                                        )
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = tr(isEn, "হাদিস শেয়ার করুন", "Share Hadith"),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Arabic always shown
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = hadith.arabic,
                                fontFamily = AmiriFontFamily,
                                fontSize = 24.sp,
                                lineHeight = 38.sp,
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            )
                        }

                        // Both Bangla & English meanings shown clearly in full view
                        MeaningBlock(
                            label = tr(isEn, "বাংলা অর্থ", "Bangla Meaning"),
                            text = hadith.bangla,
                            emphasized = !isEn
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MeaningBlock(
                            label = tr(isEn, "ইংরেজি অর্থ", "English Meaning"),
                            text = hadith.english,
                            emphasized = isEn
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${tr(isEn, "সূত্র", "Source")}: ${hadith.source(isEn)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Full Daily Ayah Card (Arabic + Bangla Meaning + English Meaning)
            item {
                val ayah = activeEntry.ayah
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_ayah_full_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = tr(isEn, "আজকের আয়াত", "Daily Ayah"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = {
                                        copyText(
                                            "${ayah.arabic}\n\nবাংলা: ${ayah.bangla}\nEnglish: ${ayah.english}\n— ${ayah.source(isEn)}\n\n${AppLinks.shareCaptionFooter(isEn)}"
                                        )
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = tr(isEn, "আয়াত কপি করুন", "Copy Ayah"),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        shareText(
                                            "${ayah.arabic}\n\nবাংলা: ${ayah.bangla}\nEnglish: ${ayah.english}\n— ${ayah.source(isEn)}\n\n${AppLinks.shareCaptionFooter(isEn)}"
                                        )
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = tr(isEn, "আয়াত শেয়ার করুন", "Share Ayah"),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = ayah.arabic,
                                fontFamily = AmiriFontFamily,
                                fontSize = 24.sp,
                                lineHeight = 38.sp,
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            )
                        }

                        MeaningBlock(
                            label = tr(isEn, "বাংলা অনুবাদ", "Bangla Translation"),
                            text = ayah.bangla,
                            emphasized = !isEn
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        MeaningBlock(
                            label = tr(isEn, "ইংরেজি অনুবাদ", "English Translation"),
                            text = ayah.english,
                            emphasized = isEn
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${tr(isEn, "সূত্র", "Source")}: ${ayah.source(isEn)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Previous 7 Days Archive
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = tr(isEn, "বিগত ৭ দিনের হাদিস ও আয়াত", "Previous Days (Last 7 Days)"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Allow jumping back to Today if currently viewing a past day
            if (activeEntry.daysAgo > 0 && entries.isNotEmpty()) {
                val todayItem = entries.first()
                item {
                    PreviousDayRowCard(
                        entry = todayItem,
                        selected = false,
                        isEnglish = isEn,
                        onClick = { selectedDateKey = todayItem.dateKey }
                    )
                }
            }

            items(previousDays, key = { it.dateKey }) { pastEntry ->
                PreviousDayRowCard(
                    entry = pastEntry,
                    selected = pastEntry.dateKey == activeEntry.dateKey,
                    isEnglish = isEn,
                    onClick = { selectedDateKey = pastEntry.dateKey }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    shareSheetEntry?.let { entry ->
        DailyShareSheet(
            entry = entry,
            isEnglish = isEn,
            onStatusMessage = onStatusMessage,
            onDismiss = { shareSheetEntry = null }
        )
    }
}

@Composable
private fun MeaningBlock(
    label: String,
    text: String,
    emphasized: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PreviousDayRowCard(
    entry: DailyInspirationEntry,
    selected: Boolean,
    isEnglish: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("previous_day_item_${entry.daysAgo}"),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(16.dp)
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
                Text(
                    text = dayLabel(entry.daysAgo, isEnglish),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = entry.gregorianDate(isEnglish),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "• ${entry.hadith.meaning(isEnglish)}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${entry.hadith.source(isEnglish)}  •  ${entry.ayah.source(isEnglish)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BoxEmptyDaily(isEn: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = tr(isEn, "দৈনিক হাদিস ও আয়াত লোড হচ্ছে...", "Loading Daily Hadith & Ayah..."),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun dayLabel(daysAgo: Int, isEnglish: Boolean): String {
    return when (daysAgo) {
        0 -> if (isEnglish) "Today" else "আজ"
        1 -> if (isEnglish) "Yesterday" else "গতকাল"
        else -> if (isEnglish) "$daysAgo days ago" else "${BanglaUtils.toBanglaNumber(daysAgo)} দিন আগে"
    }
}
