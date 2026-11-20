package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PrayerStatus
import com.example.data.model.Surah
import com.example.data.model.WaqtType
import com.example.ui.NoorUiState
import com.example.ui.theme.AmiriFontFamily
import com.example.util.BanglaUtils

@Composable
fun QuranSearchScreen(
    uiState: NoorUiState,
    onSearchChanged: (String) -> Unit,
    onToggleBookmark: (Surah, Int, String, String) -> Unit,
    onQuranDarkModeChanged: (Boolean) -> Unit,
    onQuranFontSizeChanged: (Int) -> Unit,
    onNavigateToTrackerDate: (String) -> Unit
) {
    var selectedSurah by remember { mutableStateOf<Surah?>(null) }
    var showOnlyBookmarks by remember { mutableStateOf(false) }
    var showReaderSettings by remember { mutableStateOf(false) }

    val quranDark = uiState.settings.quranDarkMode
    val readerBg = if (quranDark) Color(0xFF101513) else MaterialTheme.colorScheme.background
    val readerCard = if (quranDark) Color(0xFF1A2320) else MaterialTheme.colorScheme.surface
    val readerText = if (quranDark) Color(0xFFE7EDE9) else MaterialTheme.colorScheme.onSurface
    val readerSubText = if (quranDark) Color(0xFFAEC0B9) else MaterialTheme.colorScheme.onSurfaceVariant

    if (selectedSurah != null) {
        val surah = selectedSurah!!
        BackHandler { selectedSurah = null }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(readerBg)
        ) {
            Surface(
                color = readerCard,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { selectedSurah = null },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "ফিরে যান",
                                    tint = readerText
                                )
                            }
                            Column {
                                Text(
                                    text = "${BanglaUtils.toBanglaNumber(surah.number)}. ${surah.nameBangla}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = readerText
                                )
                                Text(
                                    text = "${surah.revelationTypeBn} • ${BanglaUtils.toBanglaNumber(surah.ayahs.size)} আয়াত",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = readerSubText
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { showReaderSettings = !showReaderSettings }) {
                                Icon(Icons.Default.FormatSize, contentDescription = "ফন্ট ও ডার্ক মোড", tint = readerText)
                            }
                        }
                    }

                    if (showReaderSettings) {
                        HorizontalDivider(color = readerSubText.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, tint = readerText)
                                Text("চোখের আরামের ডার্ক মোড", color = readerText, style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(
                                checked = quranDark,
                                onCheckedChange = onQuranDarkModeChanged
                            )
                        }
                        Text(
                            text = "ফন্ট সাইজ: ${BanglaUtils.toBanglaNumber(uiState.settings.quranFontSize)}",
                            color = readerSubText,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = uiState.settings.quranFontSize.toFloat(),
                            onValueChange = { onQuranFontSizeChanged(it.toInt()) },
                            valueRange = 14f..32f
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(surah.ayahs, key = { it.number }) { ayah ->
                    val bId = "${surah.number}_${ayah.number}"
                    val isBookmarked = uiState.bookmarks.any { it.bookmarkId == bId }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = readerCard),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = BanglaUtils.toBanglaNumber(ayah.number),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onToggleBookmark(surah, ayah.number, ayah.arabic, ayah.bangla) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "বুকমার্ক",
                                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else readerSubText
                                    )
                                }
                            }

                            Text(
                                text = ayah.arabic,
                                fontFamily = AmiriFontFamily,
                                fontSize = (uiState.settings.quranFontSize + 6).sp,
                                lineHeight = (uiState.settings.quranFontSize + 18).sp,
                                color = readerText,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = ayah.bangla,
                                fontSize = uiState.settings.quranFontSize.sp,
                                lineHeight = (uiState.settings.quranFontSize + 8).sp,
                                color = readerSubText
                            )
                        }
                    }
                }
            }
        }
        return
    }

    // Main Quran Index + Global Search
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "আল-কুরআন ও অনুসন্ধান",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = readerText
                )
                IconButton(onClick = { onQuranDarkModeChanged(!quranDark) }) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = "কুরআন ডার্ক মোড",
                        tint = if (quranDark) MaterialTheme.colorScheme.primary else readerSubText
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = uiState.globalSearchQuery,
                onValueChange = onSearchChanged,
                placeholder = { Text("কুরআনের আয়াত (বাংলা) বা নামাজের লগ খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.globalSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "মুছুন")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_input"),
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (uiState.globalSearchQuery.isNotBlank()) {
            item {
                Text(
                    text = "কুরআনের ফলাফল (${BanglaUtils.toBanglaNumber(uiState.quranSearchResults.size)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (uiState.quranSearchResults.isEmpty()) {
                item {
                    Text(
                        text = "এই শব্দে কোনো কুরআনের আয়াত পাওয়া যায়নি।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = readerSubText
                    )
                }
            } else {
                items(uiState.quranSearchResults, key = { "${it.surah.number}_${it.ayah.number}" }) { hit ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSurah = hit.surah },
                        colors = CardDefaults.cardColors(containerColor = readerCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${hit.surah.nameBangla} • আয়াত ${BanglaUtils.toBanglaNumber(hit.ayah.number)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = hit.ayah.arabic,
                                fontFamily = AmiriFontFamily,
                                fontSize = 20.sp,
                                color = readerText,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = hit.ayah.bangla,
                                style = MaterialTheme.typography.bodyMedium,
                                color = readerSubText
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "সংরক্ষিত নামাজের লগ ফলাফল (${BanglaUtils.toBanglaNumber(uiState.logSearchResults.size)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (uiState.logSearchResults.isEmpty()) {
                item {
                    Text(
                        text = "কোনো নামাজের লগ বা নোট মেলেনি।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = readerSubText
                    )
                }
            } else {
                items(uiState.logSearchResults, key = { it.dateKey }) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToTrackerDate(log.dateKey) },
                        colors = CardDefaults.cardColors(containerColor = readerCard),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Mosque, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = BanglaUtils.formatDateKeyBangla(log.dateKey),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = readerText
                                )
                                val summary = WaqtType.entries.joinToString(" • ") {
                                    "${it.titleBn}: ${log.statusFor(it).titleBn}"
                                }
                                Text(text = summary, style = MaterialTheme.typography.labelSmall, color = readerSubText)
                                if (log.note.isNotBlank()) {
                                    Text(text = "নোট: ${log.note}", style = MaterialTheme.typography.bodyMedium, color = readerText)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showOnlyBookmarks,
                        onClick = { showOnlyBookmarks = false },
                        label = { Text("সকল সূরা (${BanglaUtils.toBanglaNumber(uiState.surahs.size)})") },
                        leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = showOnlyBookmarks,
                        onClick = { showOnlyBookmarks = true },
                        label = { Text("বুকমার্ক (${BanglaUtils.toBanglaNumber(uiState.bookmarks.size)})") },
                        leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            if (showOnlyBookmarks) {
                if (uiState.bookmarks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = readerCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(36.dp), tint = readerSubText)
                                Text("এখনো কোনো আয়াত বুকমার্ক করা হয়নি।", color = readerSubText)
                            }
                        }
                    }
                } else {
                    items(uiState.bookmarks, key = { it.bookmarkId }) { bm ->
                        val matchingSurah = uiState.surahs.find { it.number == bm.surahNumber }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (matchingSurah != null) selectedSurah = matchingSurah },
                            colors = CardDefaults.cardColors(containerColor = readerCard),
                            shape = RoundedCornerShape(16.dp)
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
                                        text = "${bm.surahNameBn} • আয়াত ${BanglaUtils.toBanglaNumber(bm.ayahNumber)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (matchingSurah != null) {
                                        IconButton(onClick = {
                                            onToggleBookmark(matchingSurah, bm.ayahNumber, bm.arabicText, bm.banglaText)
                                        }) {
                                            Icon(Icons.Default.Bookmark, contentDescription = "বুকমার্ক সরান", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Text(
                                    text = bm.arabicText,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 20.sp,
                                    color = readerText,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = bm.banglaText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = readerSubText
                                )
                            }
                        }
                    }
                }
            } else {
                items(uiState.surahs, key = { it.number }) { surah ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSurah = surah }
                            .testTag("surah_card_${surah.number}"),
                        colors = CardDefaults.cardColors(containerColor = readerCard),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = BanglaUtils.toBanglaNumber(surah.number),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Column {
                                    Text(
                                        text = surah.nameBangla,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = readerText
                                    )
                                    Text(
                                        text = "${surah.revelationTypeBn} • ${BanglaUtils.toBanglaNumber(surah.ayahs.size)} আয়াত",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = readerSubText
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = surah.nameArabic,
                                fontFamily = AmiriFontFamily,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
