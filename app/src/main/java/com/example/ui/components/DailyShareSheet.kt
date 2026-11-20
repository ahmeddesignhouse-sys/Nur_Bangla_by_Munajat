package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.DailyInspirationEntry
import com.example.util.DailyShareCardGenerator
import com.example.util.ShareCardFormat
import com.example.util.ShareCardTheme
import com.example.util.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Interactive share sheet for Daily Hadith & Ayah:
 * renders a live preview of the full-content PNG card (Story 9:16 or Post 1:1, Dark or Light)
 * and provides one-tap Share as Image (WhatsApp/Facebook/Telegram), Share as Text, and Copy.
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
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSharing by remember { mutableStateOf(false) }

    LaunchedEffect(entry.dateKey, isEnglish, selectedFormat, selectedTheme) {
        previewBitmap = withContext(Dispatchers.Default) {
            DailyShareCardGenerator.renderDailyCard(
                context = context,
                entry = entry,
                isEnglish = isEnglish,
                format = selectedFormat,
                theme = selectedTheme
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
                text = tr(isEnglish, "হাদিস ও আয়াত শেয়ার করুন", "Share Daily Hadith & Ayah"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tr(
                    isEnglish,
                    "ফেসবুক স্টোরি/পোস্ট বা হোয়াটসঅ্যাপ স্ট্যাটাসে সম্পূর্ণ লেখা সহ সুন্দর কার্ড ছবি শেয়ার করুন",
                    "Share a clean full-text card image to Facebook Story/Post, WhatsApp Status, or Telegram"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Format selector (Story 9:16 vs Feed 1:1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShareCardFormat.entries.forEach { fmt ->
                    FilterChip(
                        selected = selectedFormat == fmt,
                        onClick = { selectedFormat = fmt },
                        label = { Text(fmt.label(isEnglish), style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_format_${fmt.name.lowercase()}")
                    )
                }
            }

            // Card theme selector (Dark Emerald vs Light Ivory)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShareCardTheme.entries.forEach { cardTheme ->
                    FilterChip(
                        selected = selectedTheme == cardTheme,
                        onClick = { selectedTheme = cardTheme },
                        label = { Text(cardTheme.label(isEnglish), style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_theme_${cardTheme.name.lowercase()}")
                    )
                }
            }

            // Live Card Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 380.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                            .heightIn(max = 356.dp)
                            .aspectRatio(ratio)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    CircularProgressIndicator()
                }
            }

            // Primary CTA: Share as Image (PNG via FileProvider)
            Button(
                onClick = {
                    if (isSharing) return@Button
                    isSharing = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            DailyShareCardGenerator.shareCardImage(
                                context = context,
                                entry = entry,
                                isEnglish = isEnglish,
                                format = selectedFormat,
                                theme = selectedTheme
                            )
                        }
                        isSharing = false
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
                enabled = !isSharing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("share_as_image_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSharing) {
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
                    text = tr(isEnglish, "ছবি হিসেবে শেয়ার করুন (PNG কার্ড)", "Share as Image (PNG Card)"),
                    fontWeight = FontWeight.Bold
                )
            }

            // Secondary CTA: Share as Plain Text + Copy Text
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
