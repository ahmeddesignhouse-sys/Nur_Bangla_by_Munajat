package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.SourceAttemptDiagnostic
import com.example.util.tr

/**
 * Reusable fallback banner shown on Mosque Finder & Halal Finder whenever offline or all
 * 5 free Overpass mirrors + Nominatim fail within the 15s hard cap.
 *
 * Displays:
 * - Clear banner ("অনলাইনে খুঁজে পাওয়া যায়নি — সংরক্ষিত তালিকা দেখানো হচ্ছে" or "ইন্টারনেট নেই")
 * - Real per-source diagnostics (mirrors tried, HTTP status code, elapsed seconds, exact error)
 * - Retry button + "Google Maps-এ খুলুন" external intent button (zero API key)
 */
@Composable
fun FallbackSavedListBanner(
    isOffline: Boolean,
    diagnostics: List<SourceAttemptDiagnostic>,
    isEnglish: Boolean,
    onRetry: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    var showFullLog by remember { mutableStateOf(true) }

    val headline = if (isOffline) {
        tr(
            isEnglish,
            "ইন্টারনেট নেই — সংরক্ষিত তালিকা দেখানো হচ্ছে",
            "No Internet — showing saved list"
        )
    } else {
        tr(
            isEnglish,
            "অনলাইনে খুঁজে পাওয়া যায়নি — সংরক্ষিত তালিকা দেখানো হচ্ছে",
            "Could not find online — showing saved list"
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("${testTagPrefix}_fallback_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            if (diagnostics.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { showFullLog = !showFullLog },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.testTag("${testTagPrefix}_diag_toggle")
                    ) {
                        Text(
                            text = tr(
                                isEnglish,
                                "সার্ভার ডায়াগনস্টিক (${diagnostics.size}টি উৎস পরীক্ষিত)",
                                "Server Diagnostics (${diagnostics.size} sources checked)"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showFullLog) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (showFullLog) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("${testTagPrefix}_diagnostics_log")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                diagnostics.forEach { diag ->
                                    Text(
                                        text = diag.formatLine(isEnglish),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_fallback_retry")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tr(isEnglish, "আবার চেষ্টা করুন", "Retry"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onOpenGoogleMaps,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_open_google_maps")
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tr(isEnglish, "Google Maps-এ খুলুন", "Open in Google Maps"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Opens an external Google Maps search near the user's coordinates — 100% FREE, ZERO API key!
 */
fun openExternalGoogleMapsNearMe(
    context: Context,
    latitude: Double?,
    longitude: Double?,
    queryTerm: String,
    isEnglish: Boolean,
    onStatusMessage: (String) -> Unit
) {
    val encodedQuery = Uri.encode(queryTerm)
    val geoUri = if (latitude != null && longitude != null) {
        Uri.parse("geo:$latitude,$longitude?q=$encodedQuery")
    } else {
        Uri.parse("geo:0,0?q=$encodedQuery")
    }
    val mapsAppIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    val genericGeoIntent = Intent(Intent.ACTION_VIEW, geoUri)
    val webUrl = if (latitude != null && longitude != null) {
        "https://maps.google.com/?q=$encodedQuery&ll=$latitude,$longitude"
    } else {
        "https://maps.google.com/?q=$encodedQuery"
    }

    try {
        when {
            mapsAppIntent.resolveActivity(context.packageManager) != null ->
                context.startActivity(mapsAppIntent)
            genericGeoIntent.resolveActivity(context.packageManager) != null ->
                context.startActivity(genericGeoIntent)
            else ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
        }
    } catch (_: Exception) {
        onStatusMessage(
            tr(isEnglish, "Google Maps খোলা যায়নি", "Could not open Google Maps")
        )
    }
}
