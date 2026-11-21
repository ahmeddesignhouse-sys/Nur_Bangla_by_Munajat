package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.util.DailyInspirationEntry
import com.example.util.DailyShareCardGenerator
import com.example.util.DirectShareTarget
import com.example.util.ShareCardFontSize
import com.example.util.ShareCardFormat
import com.example.util.ShareCardTheme
import com.example.util.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Interactive Share Card Customizer & Direct Share Sheet (v1.8):
 * - Live PNG card preview updating immediately for 4 Themes, 3 Font Sizes, and Story 9:16 vs Post 1:1
 * - Primary "Share as Image (PNG)" CTA
 * - Installed-only direct app targets (Facebook Story/Post, WhatsApp Status/Chat, Telegram,
 *   Instagram Story/Post, Messenger, X) — safely hidden if uninstalled
 * - Zero-permission "Save PNG to Gallery", "Share as Text", and "Copy"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyShareSheet(
    entry: DailyInspirationEntry,
    isEnglish: Boolean,
    onStatusMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedFormat by remember { mutableStateOf(ShareCardFormat.STORY) }
    var selectedTheme by remember { mutableStateOf(ShareCardTheme.DARK_MOSQUE) }
    var selectedFontSize by remember { mutableStateOf(ShareCardFontSize.MEDIUM) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isWorking by remember { mutableStateOf(false) }

    val installedTargets = remember(context) {
        DailyShareCardGenerator.getInstalledShareTargets(context)
    }

    LaunchedEffect(entry.dateKey, isEnglish, selectedFormat, selectedTheme, selectedFontSize) {
        previewBitmap = withContext(Dispatchers.Default) {
            DailyShareCardGenerator.renderDailyCard(
                context = context,
                entry = entry,
                isEnglish = isEnglish,
                format = selectedFormat,
                theme = selectedTheme,
                fontSize = selectedFontSize
            ).bitmap
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = tr(isEnglish, "হাদিস ও আয়াত কার্ড শেয়ার করুন", "Customize & Share Daily Card"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tr(
                    isEnglish,
                    "থিম, ফন্ট সাইজ ও ফরম্যাট বেছে নিন — প্রিভিউ দেখে সরাসরি ছবি শেয়ার বা গ্যালারিতে সেভ করুন",
                    "Pick theme, font size, and format — live preview updates instantly before sharing or saving"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Live Rendered PNG Card Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 390.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = previewBitmap
                if (bmp != null) {
                    val ratio = selectedFormat.widthPx.toFloat() / selectedFormat.heightPx.toFloat()
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = tr(isEnglish, "শেয়ার কার্ড প্রিভিউ", "Share card preview"),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .heightIn(max = 368.dp)
                            .aspectRatio(ratio)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    CircularProgressIndicator()
                }
            }

            // 1. Format selector (Story 9:16 vs Post 1:1)
            Text(
                text = tr(isEnglish, "কার্ড ফরম্যাট", "Card Format"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShareCardFormat.entries.forEach { fmt ->
                    FilterChip(
                        selected = selectedFormat == fmt,
                        onClick = { selectedFormat = fmt },
                        label = {
                            Text(
                                text = fmt.label(isEnglish),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_format_${fmt.name.lowercase()}")
                    )
                }
            }

            // 2. 4 Themes (2x2 grid so every theme name stays crisp and full-width)
            Text(
                text = tr(isEnglish, "থিম নির্বাচন করুন", "Choose Theme"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            val themeRows = ShareCardTheme.entries.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themeRows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { cardTheme ->
                            val isSelected = selectedTheme == cardTheme
                            Surface(
                                onClick = { selectedTheme = cardTheme },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_theme_${cardTheme.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(cardTheme.previewBgArgb))
                                            .border(2.dp, Color(cardTheme.previewAccentArgb), CircleShape)
                                    )
                                    Text(
                                        text = cardTheme.label(isEnglish),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Font Size selector (Small / Medium / Large)
            Text(
                text = tr(isEnglish, "লেখার আকার", "Text Size"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShareCardFontSize.entries.forEach { sizeOption ->
                    FilterChip(
                        selected = selectedFontSize == sizeOption,
                        onClick = { selectedFontSize = sizeOption },
                        label = {
                            Text(
                                text = sizeOption.label(isEnglish),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_font_${sizeOption.name.lowercase()}")
                    )
                }
            }

            // 4. Primary CTA: Share as Image (PNG via FileProvider)
            Button(
                onClick = {
                    if (isWorking) return@Button
                    isWorking = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            DailyShareCardGenerator.shareCardImage(
                                context = context,
                                entry = entry,
                                isEnglish = isEnglish,
                                format = selectedFormat,
                                theme = selectedTheme,
                                fontSize = selectedFontSize
                            )
                        }
                        isWorking = false
                        if (result.isFailure) {
                            onStatusMessage(
                                tr(
                                    isEnglish,
                                    "ছবি শেয়ার করা যায়নি। আবার চেষ্টা করুন।",
                                    "Could not share image. Please try again."
                                )
                            )
                        } else {
                            onDismiss()
                        }
                    }
                },
                enabled = !isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("share_as_image_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(isEnglish, "ছবি হিসেবে শেয়ার করুন (PNG)", "Share as Image (PNG)"),
                    fontWeight = FontWeight.Bold
                )
            }

            // 5. Direct App Share Targets (ONLY shown for installed apps on this device)
            if (installedTargets.isNotEmpty()) {
                Text(
                    text = tr(isEnglish, "সরাসরি অ্যাপে শেয়ার করুন", "Share Directly to App"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct_share_targets_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(installedTargets, key = { it.name }) { target ->
                        DirectShareTargetChip(
                            target = target,
                            isEnglish = isEnglish,
                            enabled = !isWorking,
                            onClick = {
                                if (isWorking) return@DirectShareTargetChip
                                isWorking = true
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        DailyShareCardGenerator.shareCardToDirectTarget(
                                            context = context,
                                            target = target,
                                            entry = entry,
                                            isEnglish = isEnglish,
                                            format = selectedFormat,
                                            theme = selectedTheme,
                                            fontSize = selectedFontSize
                                        )
                                    }
                                    isWorking = false
                                    if (result.isFailure) {
                                        onStatusMessage(
                                            tr(
                                                isEnglish,
                                                "${target.label(false)}-এ শেয়ার করা যায়নি",
                                                "Could not open ${target.label(true)}"
                                            )
                                        )
                                    } else {
                                        onDismiss()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 6. Save PNG to Gallery (zero storage permissions on Android 10+)
            OutlinedButton(
                onClick = {
                    if (isWorking) return@OutlinedButton
                    isWorking = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            val bmp = previewBitmap ?: DailyShareCardGenerator.renderDailyCard(
                                context = context,
                                entry = entry,
                                isEnglish = isEnglish,
                                format = selectedFormat,
                                theme = selectedTheme,
                                fontSize = selectedFontSize
                            ).bitmap
                            DailyShareCardGenerator.saveCardPngToGallery(
                                context = context,
                                bitmap = bmp,
                                dateKey = entry.dateKey,
                                format = selectedFormat
                            )
                        }
                        isWorking = false
                        if (result.isSuccess) {
                            onStatusMessage(
                                tr(
                                    isEnglish,
                                    "কার্ডটি গ্যালারিতে (Pictures/NurBangla) সেভ হয়েছে",
                                    "Card saved to Gallery (Pictures/NurBangla)"
                                )
                            )
                        } else {
                            onStatusMessage(
                                tr(
                                    isEnglish,
                                    "গ্যালারিতে সেভ করা যায়নি। আবার চেষ্টা করুন।",
                                    "Could not save PNG to Gallery. Please try again."
                                )
                            )
                        }
                    }
                },
                enabled = !isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_png_gallery_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(isEnglish, "গ্যালারিতে সেভ করুন (PNG)", "Save PNG to Gallery"),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 7. Secondary CTA: Share as Plain Text + Copy Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val result = DailyShareCardGenerator.sharePlainText(context, entry, isEnglish)
                        if (result.isFailure) {
                            onStatusMessage(
                                tr(isEnglish, "শেয়ার অ্যাপ পাওয়া যায়নি", "No share app available")
                            )
                        } else {
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_as_text_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr(isEnglish, "টেক্সট শেয়ার", "Share as Text"))
                }

                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("Nur Bangla Daily", entry.formatShareText(isEnglish))
                        )
                        onStatusMessage(tr(isEnglish, "সম্পূর্ণ লেখা কপি করা হয়েছে", "Full text copied"))
                    },
                    modifier = Modifier.testTag("share_copy_full_text_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr(isEnglish, "কপি", "Copy"))
                }
            }
        }
    }
}

@Composable
private fun DirectShareTargetChip(
    target: DirectShareTarget,
    isEnglish: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = Color(target.badgeColorArgb).copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color(target.badgeColorArgb).copy(alpha = 0.45f)),
        modifier = Modifier.testTag("direct_share_${target.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(target.badgeColorArgb))
            )
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = Color(target.badgeColorArgb),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = target.label(isEnglish),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
