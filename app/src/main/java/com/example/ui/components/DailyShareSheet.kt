package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.util.AppLinks
import com.example.util.BanglaUtils
import com.example.util.DailyInspirationEntry
import com.example.util.DailyShareCardGenerator
import com.example.util.DirectShareTarget
import com.example.util.ShareCardFontSize
import com.example.util.ShareCardFormat
import com.example.util.ShareCardTheme
import com.example.util.ShareCardVideoGenerator
import com.example.util.ShareVideoDuration
import com.example.util.ShareVideoMusic
import com.example.util.VideoShareDestination
import com.example.util.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Interactive Share Card Customizer & Direct Image/Video Share Sheet (v1.9):
 * - Live PNG card preview + live animated Ken Burns & background music Preview
 * - 12 Islamic themes (2-column scrollable grid) & 5 font sizes (Small..XXL)
 * - Story 9:16 & Post 1:1 formats with zero-clipping guarantee
 * - Image share (PNG), installed direct share targets, Save PNG, Share Text, Copy
 * - Video share (15 / 30 / 60s H.264 1080x1920 MP4) with 3 royalty-free music choices
 *   and 5 social video share destinations + Save Video (MP4)
 * - Honest Facebook Account Switch helper + Save Image & Copy Caption
 * - App download link row (tap to open, long-press to copy — no QR code)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var selectedVideoDuration by remember { mutableStateOf(ShareVideoDuration.SEC_30) }
    var selectedVideoMusic by remember { mutableStateOf(ShareVideoMusic.CALM_NASHEED) }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var isEncodingVideo by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableFloatStateOf(0f) }
    var isPreviewingVideo by remember { mutableStateOf(false) }

    val installedTargets = remember(context) {
        DailyShareCardGenerator.getInstalledShareTargets(context)
    }

    // Re-render card preview whenever entry, language, format, theme, or font size changes
    LaunchedEffect(entry.dateKey, entry.slot, isEnglish, selectedFormat, selectedTheme, selectedFontSize) {
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

    // Play/stop royalty-free background music during in-app video preview
    DisposableEffect(isPreviewingVideo, selectedVideoMusic) {
        var player: MediaPlayer? = null
        val resId = selectedVideoMusic.rawResId
        if (isPreviewingVideo && resId != null) {
            player = runCatching {
                MediaPlayer.create(context, resId)?.apply {
                    isLooping = true
                    setVolume(0.85f, 0.85f) // Track is already pre-mastered at ~22% background level
                    start()
                }
            }.getOrNull()
        }
        onDispose {
            runCatching {
                if (player?.isPlaying == true) player.stop()
                player?.release()
            }
        }
    }

    val previewZoom by if (isPreviewingVideo) {
        val transition = rememberInfiniteTransition(label = "ken_burns_preview")
        transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ken_burns_scale"
        )
    } else {
        remember { mutableFloatStateOf(1.0f) }
    }

    fun generateAndHandleVideo(
        destination: VideoShareDestination?,
        saveOnly: Boolean
    ) {
        if (isWorking || isEncodingVideo) return
        isPreviewingVideo = false
        isEncodingVideo = true
        videoProgress = 0.05f
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                val storyBitmap = DailyShareCardGenerator.renderDailyCard(
                    context = context,
                    entry = entry,
                    isEnglish = isEnglish,
                    format = ShareCardFormat.STORY,
                    theme = selectedTheme,
                    fontSize = selectedFontSize
                ).bitmap
                ShareCardVideoGenerator.generateVideoMp4(
                    context = context,
                    cardBitmap = storyBitmap,
                    dateKey = entry.dateKey,
                    duration = selectedVideoDuration,
                    music = selectedVideoMusic,
                    onProgress = { p -> videoProgress = p.coerceIn(0.05f, 1f) }
                )
            }
            if (result.isFailure) {
                isEncodingVideo = false
                videoProgress = 0f
                onStatusMessage(
                    tr(
                        isEnglish,
                        "ভিডিও তৈরি করা যায়নি। আবার চেষ্টা করুন।",
                        "Could not generate MP4 video. Please try again."
                    )
                )
                return@launch
            }
            val videoFile = result.getOrThrow()
            if (saveOnly) {
                val saved = withContext(Dispatchers.IO) {
                    ShareCardVideoGenerator.saveVideoMp4ToGallery(
                        context = context,
                        videoFile = videoFile,
                        dateKey = entry.dateKey,
                        durationSeconds = selectedVideoDuration.seconds
                    )
                }
                isEncodingVideo = false
                videoProgress = 0f
                if (saved.isSuccess) {
                    onStatusMessage(
                        tr(
                            isEnglish,
                            "ভিডিও গ্যালারিতে (Movies/NurBangla) সেভ হয়েছে",
                            "Video saved to Gallery (Movies/NurBangla)"
                        )
                    )
                } else {
                    onStatusMessage(
                        tr(
                            isEnglish,
                            "ভিডিও গ্যালারিতে সেভ করা যায়নি",
                            "Could not save video to Gallery"
                        )
                    )
                }
            } else {
                val shared = ShareCardVideoGenerator.shareVideoToDestination(
                    context = context,
                    destination = destination,
                    videoFile = videoFile,
                    entry = entry,
                    isEnglish = isEnglish
                )
                isEncodingVideo = false
                videoProgress = 0f
                if (shared.isFailure) {
                    onStatusMessage(
                        tr(
                            isEnglish,
                            "ভিডিও শেয়ার করা যায়নি। আবার চেষ্টা করুন।",
                            "Could not share video. Please try again."
                        )
                    )
                } else {
                    onDismiss()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            isPreviewingVideo = false
            onDismiss()
        },
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
                    "১২টি থিম, ৫টি ফন্ট সাইজ ও ছবি বা মিউজিকসহ ভিডিও বেছে নিন",
                    "Pick from 12 themes, 5 text sizes, and share as crisp PNG image or MP4 video with music"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Live Rendered PNG / Animated Video Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 380.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = previewBitmap
                if (bmp != null) {
                    val ratio = selectedFormat.widthPx.toFloat() / selectedFormat.heightPx.toFloat()
                    Box(
                        modifier = Modifier
                            .heightIn(max = 358.dp)
                            .aspectRatio(ratio)
                            .clip(RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = tr(isEnglish, "শেয়ার কার্ড প্রিভিউ", "Share card preview"),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = previewZoom
                                    scaleY = previewZoom
                                }
                        )
                        if (isPreviewingVideo) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = tr(
                                        isEnglish,
                                        "প্রিভিউ • ${selectedVideoDuration.labelBn}",
                                        "Preview • ${selectedVideoDuration.labelEn}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
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

            // 2. 12 Themes (2-column scrollable grid so every theme name stays clear)
            Text(
                text = tr(isEnglish, "থিম নির্বাচন করুন (১২টি থিম)", "Choose Theme (12 Themes)"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            val themeRows = ShareCardTheme.entries.chunked(2)
            Column(
                modifier = Modifier.testTag("share_theme_picker_grid"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 3. 5 Font Sizes (Small, Medium, Large, Extra Large, XXL — 2 clean rows)
            Text(
                text = tr(isEnglish, "লেখার আকার (৫টি সাইজ)", "Text Size (5 Sizes)"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val firstRowSizes = ShareCardFontSize.entries.take(3)
                val secondRowSizes = ShareCardFontSize.entries.drop(3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    firstRowSizes.forEach { sizeOption ->
                        FilterChip(
                            selected = selectedFontSize == sizeOption,
                            onClick = { selectedFontSize = sizeOption },
                            label = {
                                Text(
                                    text = sizeOption.label(isEnglish),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_font_${sizeOption.name.lowercase()}")
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    secondRowSizes.forEach { sizeOption ->
                        FilterChip(
                            selected = selectedFontSize == sizeOption,
                            onClick = { selectedFontSize = sizeOption },
                            label = {
                                Text(
                                    text = sizeOption.label(isEnglish),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_font_${sizeOption.name.lowercase()}")
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            // 4. Primary CTA: Share as Image (PNG via FileProvider)
            Button(
                onClick = {
                    if (isWorking || isEncodingVideo) return@Button
                    isPreviewingVideo = false
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
                enabled = !isWorking && !isEncodingVideo,
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

            // Helpful tip for Facebook Story Music Sticker + Profile Picture image-only rule
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tr(
                            isEnglish,
                            "টিপস: ছবি শেয়ার করার পর Facebook Story-তে Music sticker দিয়ে গান যোগ করতে পারবেন।",
                            "Tip: After sharing an image to Facebook Story, you can add music with Facebook's Music sticker."
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tr(
                            isEnglish,
                            "প্রোফাইল ছবির জন্য শুধু ছবি (PNG) শেয়ার করা যায়, ভিডিও নয়।",
                            "Note: Only PNG images can be used for profile pictures, not videos."
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 5. Direct App Image Share Targets (ONLY shown for installed apps on this device)
            if (installedTargets.isNotEmpty()) {
                Text(
                    text = tr(isEnglish, "সরাসরি অ্যাপে ছবি শেয়ার করুন", "Share Image Directly to App"),
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
                            enabled = !isWorking && !isEncodingVideo,
                            onClick = {
                                if (isWorking || isEncodingVideo) return@DirectShareTargetChip
                                isPreviewingVideo = false
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

            // 6. Save PNG to Gallery + Share Text + Copy
            OutlinedButton(
                onClick = {
                    if (isWorking || isEncodingVideo) return@OutlinedButton
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
                enabled = !isWorking && !isEncodingVideo,
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
                    enabled = !isWorking && !isEncodingVideo,
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            // 7. VIDEO SHARE WITH BACKGROUND MUSIC (15 / 30 / 60 sec MP4, Default 30s)
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_video_section")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = tr(
                                    isEnglish,
                                    "ভিডিও হিসেবে শেয়ার করুন (মিউজিক সহ)",
                                    "Share as Video (with Music)"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    isEnglish,
                                    "১০৮০×১৯২০ ভার্টিক্যাল MP4 • হালকা জুম অ্যানিমেশন • কম ভলিউমে (২০%)",
                                    "1080×1920 vertical MP4 • subtle zoom motion • low volume (20%)"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Duration Picker (15s / 30s / 60s)
                    Text(
                        text = tr(isEnglish, "ভিডিওর সময়কাল", "Video Duration"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShareVideoDuration.entries.forEach { dur ->
                            FilterChip(
                                selected = selectedVideoDuration == dur,
                                onClick = { selectedVideoDuration = dur },
                                enabled = !isEncodingVideo,
                                label = {
                                    Text(
                                        text = dur.label(isEnglish),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("video_duration_${dur.seconds}s")
                            )
                        }
                    }

                    // Music Picker (3 royalty-free options)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr(isEnglish, "ব্যাকগ্রাউন্ড মিউজিক", "Background Music"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = tr(isEnglish, "কম ভলিউমে (২০%)", "Low volume (20%)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShareVideoMusic.entries.forEach { music ->
                            val isSelected = selectedVideoMusic == music
                            Surface(
                                onClick = { selectedVideoMusic = music },
                                enabled = !isEncodingVideo,
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("video_music_${music.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = music.label(isEnglish),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // In-app Video + Music Preview Play/Stop button
                    FilledTonalButton(
                        onClick = { isPreviewingVideo = !isPreviewingVideo },
                        enabled = !isEncodingVideo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("video_preview_toggle_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewingVideo) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPreviewingVideo) {
                                tr(isEnglish, "প্রিভিউ বন্ধ করুন", "Stop Preview")
                            } else {
                                tr(isEnglish, "প্রিভিউ দেখুন (জুম + মিউজিক)", "Preview Video & Music")
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (isEncodingVideo) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { videoProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = tr(
                                    isEnglish,
                                    "MP4 ভিডিও তৈরি হচ্ছে... ${BanglaUtils.toBanglaNumber((videoProgress * 100).toInt())}%",
                                    "Encoding MP4 video... ${(videoProgress * 100).toInt()}%"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Video Share Destination Buttons (5 targets + Save Video)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VideoShareDestination.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { dest ->
                                    OutlinedButton(
                                        onClick = { generateAndHandleVideo(destination = dest, saveOnly = false) },
                                        enabled = !isWorking && !isEncodingVideo,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("share_video_${dest.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = dest.label(isEnglish),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    OutlinedButton(
                                        onClick = { generateAndHandleVideo(destination = null, saveOnly = true) },
                                        enabled = !isWorking && !isEncodingVideo,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("save_video_mp4_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = tr(isEnglish, "ভিডিও সেভ করুন", "Save Video"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. HONEST FACEBOOK ACCOUNT SWITCH HELPER (Android OS / Facebook app limitation)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("facebook_account_helper_section")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = tr(isEnglish, "ফেসবুক অ্যাকাউন্ট বদলান", "Switch Facebook Account"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = tr(
                            isEnglish,
                            "Android ও Facebook অ্যাপের নিয়ম অনুযায়ী Nur Bangla থেকে অন্য Facebook অ্যাকাউন্ট বেছে নেওয়া যায় না — শেয়ার সবসময় Facebook অ্যাপে বর্তমানে লগ-ইন থাকা অ্যাকাউন্ট থেকে যায়।\n\nঅন্য অ্যাকাউন্টে শেয়ার করতে: Facebook অ্যাপ > Menu > আপনার নাম > অন্য অ্যাকাউন্টে Switch করুন > Nur Bangla-তে ফিরে আবার Share চাপুন — এবার নতুন অ্যাকাউন্ট থেকে যাবে।",
                            "By Facebook & Android security rules, third-party apps cannot switch your Facebook account directly — shares always open in the currently active Facebook account.\n\nTo post from another account: Open Facebook app > Menu > Your Name > Switch account > return to Nur Bangla and tap Share again."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            val opened = DailyShareCardGenerator.openFacebookAppToSwitchAccount(context)
                            if (!opened) {
                                onStatusMessage(
                                    tr(isEnglish, "Facebook অ্যাপ বা ব্রাউজার খোলা যায়নি", "Could not open Facebook")
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_facebook_switch_account_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(
                                isEnglish,
                                "Facebook অ্যাপে অ্যাকাউন্ট বদলান",
                                "Open Facebook App to Switch Account"
                            ),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            if (isWorking || isEncodingVideo) return@FilledTonalButton
                            isWorking = true
                            scope.launch {
                                val res = withContext(Dispatchers.IO) {
                                    val bmp = previewBitmap ?: DailyShareCardGenerator.renderDailyCard(
                                        context = context,
                                        entry = entry,
                                        isEnglish = isEnglish,
                                        format = selectedFormat,
                                        theme = selectedTheme,
                                        fontSize = selectedFontSize
                                    ).bitmap
                                    DailyShareCardGenerator.saveCardAndCopyCaption(
                                        context = context,
                                        bitmap = bmp,
                                        entry = entry,
                                        isEnglish = isEnglish,
                                        format = selectedFormat
                                    )
                                }
                                isWorking = false
                                if (res.isSuccess) {
                                    onStatusMessage(
                                        tr(
                                            isEnglish,
                                            "ছবি গ্যালারিতে সেভ ও সম্পূর্ণ লেখা কপি হয়েছে — যেকোনো অ্যাকাউন্ট বা পেজ থেকে পোস্ট করুন",
                                            "Image saved to Gallery & full caption copied — paste in any Facebook account or page"
                                        )
                                    )
                                } else {
                                    onStatusMessage(
                                        tr(
                                            isEnglish,
                                            "ছবি সেভ করা যায়নি, তবে লেখা কপি হয়েছে",
                                            "Could not save image, but caption was copied"
                                        )
                                    )
                                }
                            }
                        },
                        enabled = !isWorking && !isEncodingVideo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_image_copy_caption_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(isEnglish, "ছবি সেভ + লেখা কপি", "Save Image + Copy Caption"),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 9. APP DOWNLOAD LINK ROW (Tap opens in browser, Long-Press copies — NO QR code)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .combinedClickable(
                        onClick = {
                            val opened = AppLinks.openDownloadPage(context)
                            if (!opened) {
                                onStatusMessage(
                                    tr(isEnglish, "ব্রাউজার খোলা যায়নি", "Could not open browser")
                                )
                            }
                        },
                        onLongClick = {
                            AppLinks.copyDownloadLink(context)
                        }
                    )
                    .testTag("share_app_download_link_row")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = tr(isEnglish, "অ্যাপ ডাউনলোড লিংক", "App download link"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = tr(
                                isEnglish,
                                "অ্যাপ লিংক — একবার চাপলে খুলবে, চেপে ধরে রাখলে কপি হবে",
                                "App link — tap to open, long-press to copy"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = AppLinks.APP_DOWNLOAD_URL,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
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
