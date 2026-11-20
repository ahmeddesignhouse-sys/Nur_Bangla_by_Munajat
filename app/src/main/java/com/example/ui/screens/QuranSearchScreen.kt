package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Surah
import com.example.ui.NoorUiState
import com.example.ui.theme.AmiriFontFamily
import com.example.util.BanglaUtils
import com.example.util.LocalAppLanguage
import com.example.util.QuranAudioState
import com.example.util.isEnglishLang
import com.example.util.tr

@Composable
fun QuranSearchScreen(
    uiState: NoorUiState,
    onSearchChanged: (String) -> Unit,
    onToggleBookmark: (Surah, Int, String, String) -> Unit,
    onQuranDarkModeChanged: (Boolean) -> Unit,
    onQuranFontSizeChanged: (Int) -> Unit,
    onPlaySurahAudio: (Surah) -> Unit,
    onPlayAyahAudio: (Surah, Int) -> Unit,
    onToggleAudioPlayPause: () -> Unit,
    onNextAudioAyah: () -> Unit,
    onPreviousAudioAyah: () -> Unit,
    onStopAudio: () -> Unit,
    onToggleAyahReadToday: (Int, Int) -> Unit
) {
    val isEn = isEnglishLang(LocalAppLanguage.current)
    var selectedSurah by remember { mutableStateOf<Surah?>(null) }
    var showOnlyBookmarks by remember { mutableStateOf(false) }
    var showReaderSettings by remember { mutableStateOf(false) }
    val audioState = uiState.quranAudioState

    val quranDark = uiState.settings.quranDarkMode
    val readerBg = if (quranDark) Color(0xFF101513) else MaterialTheme.colorScheme.background
    val readerCard = if (quranDark) Color(0xFF1A2320) else MaterialTheme.colorScheme.surface
    val readerText = if (quranDark) Color(0xFFE7EDE9) else MaterialTheme.colorScheme.onSurface
    val readerSubText = if (quranDark) Color(0xFFAEC0B9) else MaterialTheme.colorScheme.onSurfaceVariant

    if (selectedSurah != null) {
        val surah = selectedSurah!!
        val listState = rememberLazyListState()
        val isThisSurahPlaying = audioState.surahNumber == surah.number && audioState.isPlaying
        val isThisSurahLoading = audioState.surahNumber == surah.number && audioState.isLoading

        LaunchedEffect(audioState.surahNumber, audioState.ayahIndex) {
            if (audioState.surahNumber == surah.number && audioState.ayahIndex in surah.ayahs.indices) {
                listState.animateScrollToItem(audioState.ayahIndex)
            }
        }

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { selectedSurah = null },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = tr(isEn, "ফিরে যান", "Back"),
                                    tint = readerText
                                )
                            }
                            Column {
                                Text(
                                    text = "${BanglaUtils.formatNumber(surah.number, isEn)}. ${surah.displayName(isEn)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = readerText
                                )
                                Text(
                                    text = "${surah.revelationType(isEn)} • ${BanglaUtils.formatNumber(surah.ayahs.size, isEn)} ${tr(isEn, "আয়াত", "Ayahs")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = readerSubText
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onPlaySurahAudio(surah) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reader_play_surah_button")
                            ) {
                                if (isThisSurahLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isThisSurahPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isThisSurahPlaying) {
                                            tr(isEn, "তিলাওয়াত থামান", "Pause recitation")
                                        } else {
                                            tr(isEn, "সূরা তিলাওয়াত শুনুন", "Play Surah recitation")
                                        },
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { showReaderSettings = !showReaderSettings }) {
                                Icon(
                                    Icons.Default.FormatSize,
                                    contentDescription = tr(isEn, "ফন্ট ও ডার্ক মোড", "Font & Dark Mode"),
                                    tint = readerText
                                )
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
                                Text(
                                    text = tr(isEn, "চোখের আরামের ডার্ক মোড", "Eye-Comfort Dark Mode"),
                                    color = readerText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Switch(
                                checked = quranDark,
                                onCheckedChange = onQuranDarkModeChanged
                            )
                        }
                        Text(
                            text = "${tr(isEn, "ফন্ট সাইজ", "Font Size")}: ${BanglaUtils.formatNumber(uiState.settings.quranFontSize, isEn)}",
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
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(surah.ayahs, key = { it.number }) { ayah ->
                    val bId = "${surah.number}_${ayah.number}"
                    val isBookmarked = uiState.bookmarks.any { it.bookmarkId == bId }
                    val isReadToday = uiState.dailyQuranReadKeys.contains(bId)
                    val isAyahActive = audioState.surahNumber == surah.number && audioState.ayahNumber == ayah.number
                    val isAyahPlaying = isAyahActive && audioState.isPlaying
                    val isAyahLoading = isAyahActive && audioState.isLoading

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ayah_card_${surah.number}_${ayah.number}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAyahActive) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (quranDark) 0.28f else 0.45f)
                            } else {
                                readerCard
                            }
                        ),
                        border = if (isAyahActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = if (isAyahActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = BanglaUtils.formatNumber(ayah.number, isEn),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isAyahActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (isAyahActive && (audioState.isPlaying || audioState.isLoading)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.GraphicEq,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = if (isAyahLoading) tr(isEn, "লোড হচ্ছে", "Loading")
                                                    else tr(isEn, "চলছে", "Playing"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onPlayAyahAudio(surah, ayah.number) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("ayah_play_${surah.number}_${ayah.number}")
                                    ) {
                                        if (isAyahLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isAyahPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isAyahPlaying) {
                                                    tr(isEn, "আয়াত থামান", "Pause Ayah")
                                                } else {
                                                    tr(isEn, "আয়াত শুনুন", "Play Ayah")
                                                },
                                                tint = if (isAyahActive) MaterialTheme.colorScheme.primary else readerSubText
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onToggleAyahReadToday(surah.number, ayah.number) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("ayah_read_${surah.number}_${ayah.number}")
                                    ) {
                                        Icon(
                                            imageVector = if (isReadToday) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                            contentDescription = tr(isEn, "আজকের পাঠে চিহ্নিত করুন", "Mark read for daily goal"),
                                            tint = if (isReadToday) MaterialTheme.colorScheme.primary else readerSubText
                                        )
                                    }
                                    IconButton(
                                        onClick = { onToggleBookmark(surah, ayah.number, ayah.arabic, ayah.bangla) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = tr(isEn, "বুকমার্ক", "Bookmark"),
                                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else readerSubText
                                        )
                                    }
                                }
                            }

                            // Arabic text is always preserved regardless of app language
                            Text(
                                text = ayah.arabic,
                                fontFamily = AmiriFontFamily,
                                fontSize = (uiState.settings.quranFontSize + 6).sp,
                                lineHeight = (uiState.settings.quranFontSize + 18).sp,
                                color = readerText,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Translation follows app language: Bangla in bn mode, English in en mode
                            Text(
                                text = ayah.translation(isEn),
                                fontSize = uiState.settings.quranFontSize.sp,
                                lineHeight = (uiState.settings.quranFontSize + 8).sp,
                                color = readerSubText
                            )
                        }
                    }
                }
            }

            if (audioState.isActive) {
                QuranAudioPlayerBar(
                    audioState = audioState,
                    isEnglish = isEn,
                    onTogglePlayPause = onToggleAudioPlayPause,
                    onPrevious = onPreviousAudioAyah,
                    onNext = onNextAudioAyah,
                    onClose = onStopAudio
                )
            }
        }
        return
    }

    // Main Quran Index + Search + Sticky Audio Bar
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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
                        text = tr(isEn, "আল-কুরআন ও অনুসন্ধান", "Al-Quran & Search"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = readerText
                    )
                    IconButton(onClick = { onQuranDarkModeChanged(!quranDark) }) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = tr(isEn, "কুরআন ডার্ক মোড", "Quran Dark Mode"),
                            tint = if (quranDark) MaterialTheme.colorScheme.primary else readerSubText
                        )
                    }
                }
            }

            // Daily Quran Goal Progress Strip
            item {
                val goal = uiState.settings.dailyQuranGoal.coerceAtLeast(1)
                val readCount = uiState.dailyQuranReadCount
                val progress = (readCount.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
                Surface(
                    color = readerCard,
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(
                                    isEn,
                                    "আজকের লক্ষ্য: ${BanglaUtils.toBanglaNumber(readCount)} / ${BanglaUtils.toBanglaNumber(goal)} আয়াত",
                                    "Today: $readCount / $goal Ayahs"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = audioState.reciter.displayName(isEn),
                                style = MaterialTheme.typography.labelSmall,
                                color = readerSubText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

        item {
            OutlinedTextField(
                value = uiState.globalSearchQuery,
                onValueChange = onSearchChanged,
                placeholder = {
                    Text(
                        tr(
                            isEn,
                            "কুরআনের সূরা বা আয়াত খুঁজুন...",
                            "Search Quran Surahs or verses..."
                        )
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.globalSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = tr(isEn, "মুছুন", "Clear"))
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
                    text = "${tr(isEn, "কুরআনের ফলাফল", "Quran Results")} (${BanglaUtils.formatNumber(uiState.quranSearchResults.size, isEn)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (uiState.quranSearchResults.isEmpty()) {
                item {
                    Text(
                        text = tr(
                            isEn,
                            "এই শব্দে কোনো কুরআনের আয়াত পাওয়া যায়নি।",
                            "No Quran verses matched your search."
                        ),
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
                                text = "${hit.surah.displayName(isEn)} • ${tr(isEn, "আয়াত", "Ayah")} ${BanglaUtils.formatNumber(hit.ayah.number, isEn)}",
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
                                text = hit.ayah.translation(isEn),
                                style = MaterialTheme.typography.bodyMedium,
                                color = readerSubText
                            )
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
                        label = {
                            Text("${tr(isEn, "সকল সূরা", "All Surahs")} (${BanglaUtils.formatNumber(uiState.surahs.size, isEn)})")
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = showOnlyBookmarks,
                        onClick = { showOnlyBookmarks = true },
                        label = {
                            Text("${tr(isEn, "বুকমার্ক", "Bookmarks")} (${BanglaUtils.formatNumber(uiState.bookmarks.size, isEn)})")
                        },
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
                                Text(
                                    text = tr(
                                        isEn,
                                        "এখনো কোনো আয়াত বুকমার্ক করা হয়নি।",
                                        "No bookmarked verses yet."
                                    ),
                                    color = readerSubText
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.bookmarks, key = { it.bookmarkId }) { bm ->
                        val matchingSurah = uiState.surahs.find { it.number == bm.surahNumber }
                        val matchingAyah = matchingSurah?.ayahs?.find { it.number == bm.ayahNumber }
                        val displaySurahName = matchingSurah?.displayName(isEn) ?: bm.surahNameBn
                        val displayTranslation = matchingAyah?.translation(isEn) ?: bm.banglaText

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
                                        text = "$displaySurahName • ${tr(isEn, "আয়াত", "Ayah")} ${BanglaUtils.formatNumber(bm.ayahNumber, isEn)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (matchingSurah != null) {
                                        IconButton(onClick = {
                                            onToggleBookmark(matchingSurah, bm.ayahNumber, bm.arabicText, bm.banglaText)
                                        }) {
                                            Icon(
                                                Icons.Default.Bookmark,
                                                contentDescription = tr(isEn, "বুকমার্ক সরান", "Remove bookmark"),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
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
                                    text = displayTranslation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = readerSubText
                                )
                            }
                        }
                    }
                }
            } else {
                items(uiState.surahs, key = { it.number }) { surah ->
                    val isSurahPlaying = audioState.surahNumber == surah.number && audioState.isPlaying
                    val isSurahLoading = audioState.surahNumber == surah.number && audioState.isLoading
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
                                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                        text = BanglaUtils.formatNumber(surah.number, isEn),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Column {
                                    Text(
                                        text = surah.displayName(isEn),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = readerText
                                    )
                                    Text(
                                        text = "${surah.revelationType(isEn)} • ${BanglaUtils.formatNumber(surah.ayahs.size, isEn)} ${tr(isEn, "আয়াত", "Ayahs")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = readerSubText
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = surah.nameArabic,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 19.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        selectedSurah = surah
                                        onPlaySurahAudio(surah)
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("surah_play_${surah.number}")
                                ) {
                                    if (isSurahLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isSurahPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isSurahPlaying) {
                                                tr(isEn, "থামান", "Pause")
                                            } else {
                                                tr(isEn, "তিলাওয়াত শুনুন", "Play recitation")
                                            },
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        if (audioState.isActive) {
            QuranAudioPlayerBar(
                audioState = audioState,
                isEnglish = isEn,
                onTogglePlayPause = onToggleAudioPlayPause,
                onPrevious = onPreviousAudioAyah,
                onNext = onNextAudioAyah,
                onClose = onStopAudio
            )
        }
    }
}

@Composable
private fun QuranAudioPlayerBar(
    audioState: QuranAudioState,
    isEnglish: Boolean,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val errorMsg = audioState.errorMessage(isEnglish)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quran_audio_player_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (errorMsg != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${audioState.surahTitle(isEnglish)} • ${tr(isEnglish, "আয়াত", "Ayah")} ${BanglaUtils.formatNumber(audioState.ayahNumber ?: 0, isEnglish)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = audioState.reciter.displayName(isEnglish),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = audioState.canGoPrevious,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("audio_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = tr(isEnglish, "আগের আয়াত", "Previous Ayah")
                        )
                    }
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .testTag("audio_play_pause_button")
                    ) {
                        if (audioState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (audioState.isPlaying) {
                                    tr(isEnglish, "বিরতি", "Pause")
                                } else {
                                    tr(isEnglish, "চালু করুন", "Play")
                                },
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    IconButton(
                        onClick = onNext,
                        enabled = audioState.canGoNext,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("audio_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = tr(isEnglish, "পরের আয়াত", "Next Ayah")
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("audio_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = tr(isEnglish, "বন্ধ করুন", "Close player")
                        )
                    }
                }
            }
        }
    }
}
