package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DuaCategory
import com.example.data.model.DuaItem
import com.example.data.repository.DuaRepository
import com.example.ui.theme.AmiriFontFamily
import com.example.util.BanglaUtils
import com.example.util.LocalAppLanguage
import com.example.util.isEnglishLang
import com.example.util.tr

@Composable
fun DuaAzkarScreen(
    favoriteDuaIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onStatusMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isEn = isEnglishLang(LocalAppLanguage.current)
    val repository = remember(context) { DuaRepository(context) }

    var allDuas by remember { mutableStateOf<List<DuaItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<DuaCategory?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var showTransliteration by remember { mutableStateOf(true) }

    LaunchedEffect(repository) {
        allDuas = repository.loadAllDuas()
    }

    val filteredDuas = remember(allDuas, searchQuery, selectedCategory, favoritesOnly, favoriteDuaIds) {
        repository.filterDuas(
            allDuas = allDuas,
            query = searchQuery,
            selectedCategory = selectedCategory,
            favoritesOnly = favoritesOnly,
            favoriteIds = favoriteDuaIds
        )
    }

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
                        .testTag("dua_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr(isEn, "পিছনে যান", "Back")
                    )
                }
                Column {
                    Text(
                        text = tr(isEn, "দোয়া ও আজকার", "Dua & Azkar"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            isEn,
                            "${BanglaUtils.formatNumber(allDuas.size, false)}টি মাসনুন দোয়া ও জিকির",
                            "${allDuas.size} Authentic Supplications & Adhkar"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilterChip(
                selected = showTransliteration,
                onClick = { showTransliteration = !showTransliteration },
                label = {
                    Text(
                        text = tr(isEn, "উচ্চারণ", "Translit."),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag("dua_translit_toggle")
            )
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    tr(
                        isEn,
                        "দোয়া খুঁজুন (যেমন: সকাল, সফর, ইফতার, বিপদ)...",
                        "Search duas (e.g., Morning, Travel, Iftar, Hardship)..."
                    )
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = tr(isEn, "মুছুন", "Clear"))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("dua_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Row (All + Favorites + 10 Categories)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null && !favoritesOnly,
                    onClick = {
                        selectedCategory = null
                        favoritesOnly = false
                    },
                    label = { Text(tr(isEn, "সবগুলো", "All")) },
                    modifier = Modifier.testTag("dua_cat_all")
                )
            }
            item {
                FilterChip(
                    selected = favoritesOnly,
                    onClick = {
                        favoritesOnly = !favoritesOnly
                        if (favoritesOnly) selectedCategory = null
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            "${tr(isEn, "প্রিয়", "Favorites")} (${BanglaUtils.formatNumber(favoriteDuaIds.size, isEn)})"
                        )
                    },
                    modifier = Modifier.testTag("dua_cat_favorites")
                )
            }
            items(DuaCategory.entries, key = { it.key }) { cat ->
                FilterChip(
                    selected = selectedCategory == cat && !favoritesOnly,
                    onClick = {
                        favoritesOnly = false
                        selectedCategory = if (selectedCategory == cat) null else cat
                    },
                    label = { Text(cat.title(isEn)) },
                    modifier = Modifier.testTag("dua_cat_${cat.key}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredDuas.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (favoritesOnly) {
                        tr(
                            isEn,
                            "এখনো কোনো দোয়া বুকমার্ক করা হয়নি। যেকোনো দোয়ার বুকমার্ক আইকনে ট্যাপ করে সংরক্ষণ করুন।",
                            "No bookmarked duas yet. Tap the bookmark icon on any dua to save it here."
                        )
                    } else {
                        tr(
                            isEn,
                            "আপনার অনুসন্ধানের সাথে মিলে এমন কোনো দোয়া পাওয়া যায়নি।",
                            "No duas matched your search."
                        )
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDuas, key = { it.id }) { dua ->
                    val isFav = favoriteDuaIds.contains(dua.id)
                    DuaCard(
                        dua = dua,
                        isEnglish = isEn,
                        isFavorite = isFav,
                        showTransliteration = showTransliteration,
                        onToggleFavorite = { onToggleFavorite(dua.id) },
                        onCopy = {
                            val textToCopy = buildShareableDuaText(dua, isEn, showTransliteration)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(dua.title(isEn), textToCopy))
                            onStatusMessage(
                                tr(isEn, "দোয়া কপি করা হয়েছে", "Dua copied to clipboard")
                            )
                        },
                        onShare = {
                            val shareText = buildShareableDuaText(dua, isEn, showTransliteration)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, dua.title(isEn))
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(
                                Intent.createChooser(intent, tr(isEn, "দোয়া শেয়ার করুন", "Share Dua"))
                            )
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun DuaCard(
    dua: DuaItem,
    isEnglish: Boolean,
    isFavorite: Boolean,
    showTransliteration: Boolean,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dua_card_${dua.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = dua.category.title(isEnglish),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = dua.title(isEnglish),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dua_fav_${dua.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isFavorite) {
                            tr(isEnglish, "বুকমার্ক সরান", "Remove bookmark")
                        } else {
                            tr(isEnglish, "বুকমার্ক করুন", "Bookmark dua")
                        },
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Arabic text (always shown)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = dua.arabic,
                    fontFamily = AmiriFontFamily,
                    fontSize = 24.sp,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                )
            }

            if (showTransliteration && dua.transliteration.isNotBlank()) {
                Text(
                    text = dua.transliteration,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                text = dua.meaning(isEnglish),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (dua.reference(isEnglish).isNotBlank()) {
                Text(
                    text = "${tr(isEnglish, "সূত্র", "Reference")}: ${dua.reference(isEnglish)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dua_copy_${dua.id}")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr(isEnglish, "কপি করুন", "Copy"))
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dua_share_${dua.id}")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr(isEnglish, "শেয়ার করুন", "Share"))
                }
            }
        }
    }
}

private fun buildShareableDuaText(
    dua: DuaItem,
    isEnglish: Boolean,
    includeTransliteration: Boolean
): String {
    return buildString {
        appendLine(dua.title(isEnglish))
        appendLine()
        appendLine(dua.arabic)
        if (includeTransliteration && dua.transliteration.isNotBlank()) {
            appendLine()
            appendLine(dua.transliteration)
        }
        appendLine()
        appendLine(dua.meaning(isEnglish))
        if (dua.reference(isEnglish).isNotBlank()) {
            appendLine()
            append("(${dua.reference(isEnglish)})")
        }
    }
}
