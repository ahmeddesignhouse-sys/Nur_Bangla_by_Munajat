package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.BanglaUtils
import com.example.util.tr
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

private const val MAP_MAX_LOADING_MS = 10_000L

data class MapPlacePin(
    val id: String,
    val title: String,
    val subtitle: String,
    val distanceLabel: String,
    val badge: String?,
    val latitude: Double,
    val longitude: Double
)

/**
 * 100% FREE embedded OpenStreetMap (osmdroid) view — no API key, no billing, no Google Maps SDK.
 * - Displays user's blue GPS dot and a marker for every discovered Mosque or Halal Restaurant.
 * - Tapping any marker opens a bottom detail card with name, distance in km, address, and a
 *   Directions button that opens the phone's map app via geo: / Google Maps navigation intent.
 * - Caps the tile-loading pill at max 10 seconds so slow connections never block interaction.
 */
@Composable
fun NearbyPlacesMapView(
    userLat: Double,
    userLon: Double,
    userLocationLabel: String,
    pins: List<MapPlacePin>,
    selectedPinId: String?,
    isEnglish: Boolean,
    countBadgeText: String,
    pinColor: Color,
    mirrorSourceLabel: String = "",
    onSelectPin: (String) -> Unit,
    onClearSelection: () -> Unit,
    onOpenDirections: (MapPlacePin) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pinArgb = pinColor.toArgb()

    remember(context) {
        configureOsmdroid(context)
        true
    }

    var isMapLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(MAP_MAX_LOADING_MS)
        isMapLoading = false
    }

    val userDrawable = remember(context) { createUserLocationDrawable(context) }
    val normalPinDrawable = remember(context, pinArgb) { createPlacePinDrawable(context, pinArgb, selected = false) }
    val selectedPinDrawable = remember(context, pinArgb) { createPlacePinDrawable(context, pinArgb, selected = true) }

    val mapViewHolder = remember { mutableStateOf<MapView?>(null) }
    val lastCameraSignature = remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewHolder.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewHolder.value?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewHolder.value?.onDetach()
        }
    }

    val selectedPin = remember(pins, selectedPinId) {
        pins.firstOrNull { it.id == selectedPinId }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("embedded_places_map"),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    controller.setZoom(14.5)
                    controller.setCenter(GeoPoint(userLat, userLon))
                    mapViewHolder.value = this
                    post { isMapLoading = false }
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                val userPoint = GeoPoint(userLat, userLon)
                val userMarker = Marker(mapView).apply {
                    position = userPoint
                    title = userLocationLabel
                    icon = userDrawable
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ ->
                        onClearSelection()
                        mapView.controller.animateTo(userPoint)
                        true
                    }
                }
                mapView.overlays.add(userMarker)

                for (pin in pins) {
                    val isSelected = pin.id == selectedPinId
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(pin.latitude, pin.longitude)
                        title = pin.title
                        snippet = pin.distanceLabel
                        icon = if (isSelected) selectedPinDrawable else normalPinDrawable
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ ->
                            onSelectPin(pin.id)
                            mapView.controller.animateTo(GeoPoint(pin.latitude, pin.longitude))
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }

                val signature = "${userLat}_${userLon}_${pins.size}_${pins.firstOrNull()?.id}"
                if (lastCameraSignature.value != signature) {
                    lastCameraSignature.value = signature
                    fitCameraToPoints(mapView, userLat, userLon, pins)
                }

                mapView.invalidate()
            }
        )

        // Top-left count badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 4.dp
        ) {
            Text(
                text = countBadgeText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Top-right recenter button
        FloatingActionButton(
            onClick = {
                mapViewHolder.value?.let { mv ->
                    mv.controller.animateTo(GeoPoint(userLat, userLon), 15.0, 450L)
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(48.dp)
                .testTag("map_recenter_button"),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = tr(isEnglish, "আমার অবস্থানে ফিরে যান", "Recenter on my location")
            )
        }

        // Non-blocking tile loading indicator (max 10 seconds)
        if (isMapLoading) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .testTag("map_loading_indicator"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = tr(isEnglish, "ম্যাপ লোড হচ্ছে...", "Loading map..."),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Bottom attribution + answering Overpass mirror badge
        if (selectedPin == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            ) {
                val mirrorSuffix = if (mirrorSourceLabel.isNotBlank()) " • $mirrorSourceLabel" else ""
                Text(
                    text = tr(
                        isEnglish,
                        "পিনে ট্যাপ করে বিস্তারিত ও দিকনির্দেশনা দেখুন • © OpenStreetMap$mirrorSuffix",
                        "Tap any pin for details & directions • © OpenStreetMap$mirrorSuffix"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Selected pin bottom detail card
        selectedPin?.let { pin ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("map_selected_pin_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = pin.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pin.distanceLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                pin.badge?.takeIf { it.isNotBlank() }?.let { badgeText ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            if (pin.subtitle.isNotBlank()) {
                                Text(
                                    text = pin.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("map_close_pin_card")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr(isEnglish, "বন্ধ করুন", "Close")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Button(
                        onClick = { onOpenDirections(pin) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("map_pin_directions_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(
                                isEnglish,
                                "দিকনির্দেশনা (Directions)",
                                "Directions"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun configureOsmdroid(context: Context) {
    val config = Configuration.getInstance()
    config.userAgentValue = "NurBanglaAndroid/1.8 (${context.packageName})"
    val basePath = File(context.cacheDir, "osmdroid")
    val tileCache = File(basePath, "tiles")
    if (!tileCache.exists()) {
        tileCache.mkdirs()
    }
    config.osmdroidBasePath = basePath
    config.osmdroidTileCache = tileCache
}

private fun fitCameraToPoints(
    mapView: MapView,
    userLat: Double,
    userLon: Double,
    pins: List<MapPlacePin>
) {
    val userPoint = GeoPoint(userLat, userLon)
    if (pins.isEmpty()) {
        mapView.controller.setZoom(14.5)
        mapView.controller.setCenter(userPoint)
        return
    }

    val focusPins = pins.take(10)
    var minLat = userLat
    var maxLat = userLat
    var minLon = userLon
    var maxLon = userLon
    for (p in focusPins) {
        if (p.latitude < minLat) minLat = p.latitude
        if (p.latitude > maxLat) maxLat = p.latitude
        if (p.longitude < minLon) minLon = p.longitude
        if (p.longitude > maxLon) maxLon = p.longitude
    }

    val latPad = ((maxLat - minLat) * 0.2).coerceAtLeast(0.004)
    val lonPad = ((maxLon - minLon) * 0.2).coerceAtLeast(0.004)
    val box = BoundingBox(
        (maxLat + latPad).coerceAtMost(85.0),
        (maxLon + lonPad).coerceAtMost(179.9),
        (minLat - latPad).coerceAtLeast(-85.0),
        (minLon - lonPad).coerceAtLeast(-179.9)
    )
    mapView.post {
        try {
            mapView.zoomToBoundingBox(box, false, 72)
            if (mapView.zoomLevelDouble > 16.5) {
                mapView.controller.setZoom(16.0)
            }
        } catch (_: Exception) {
            mapView.controller.setZoom(14.0)
            mapView.controller.setCenter(userPoint)
        }
    }
}

private fun createUserLocationDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (28 * density).toInt().coerceAtLeast(28)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(65, 25, 118, 210)
    }
    canvas.drawCircle(cx, cy, sizePx * 0.48f, haloPaint)

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
    }
    canvas.drawCircle(cx, cy, sizePx * 0.30f, ringPaint)

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(21, 101, 192)
    }
    canvas.drawCircle(cx, cy, sizePx * 0.21f, dotPaint)

    return BitmapDrawable(context.resources, bmp)
}

private fun createPlacePinDrawable(context: Context, fillColorArgb: Int, selected: Boolean): Drawable {
    val density = context.resources.displayMetrics.density
    val baseDp = if (selected) 38f else 30f
    val widthPx = (baseDp * density).toInt().coerceAtLeast(30)
    val heightPx = (baseDp * 1.25f * density).toInt().coerceAtLeast(38)
    val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val cx = widthPx / 2f
    val headRadius = widthPx * 0.40f
    val headCy = headRadius + (2f * density)

    val pointerPath = Path().apply {
        moveTo(cx - headRadius * 0.65f, headCy + headRadius * 0.55f)
        lineTo(cx, heightPx - (2f * density))
        lineTo(cx + headRadius * 0.65f, headCy + headRadius * 0.55f)
        close()
    }

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColorArgb
        style = Paint.Style.FILL
    }

    canvas.drawCircle(cx, headCy, headRadius + (2f * density), borderPaint)
    canvas.drawPath(pointerPath, fillPaint)
    canvas.drawCircle(cx, headCy, headRadius, fillPaint)
    canvas.drawCircle(cx, headCy, headRadius * (if (selected) 0.42f else 0.34f), borderPaint)

    return BitmapDrawable(context.resources, bmp)
}

fun formatRadiusExpansionBanner(
    requestedRadiusMeters: Int,
    effectiveRadiusMeters: Int,
    isMosque: Boolean,
    isEnglish: Boolean
): String {
    val reqKm = BanglaUtils.formatNumber(requestedRadiusMeters / 1000, isEnglish)
    val effKm = BanglaUtils.formatNumber(effectiveRadiusMeters / 1000, isEnglish)
    return if (isMosque) {
        tr(
            isEnglish,
            "$reqKm কিমি-এর মধ্যে মসজিদ পাওয়া যায়নি, $effKm কিমি-এর মধ্যে দেখানো হচ্ছে",
            "No mosque within $reqKm km, showing within $effKm km"
        )
    } else {
        tr(
            isEnglish,
            "$reqKm কিমি-এর মধ্যে রেস্টুরেন্ট পাওয়া যায়নি, $effKm কিমি-এর মধ্যে দেখানো হচ্ছে",
            "No restaurant within $reqKm km, showing within $effKm km"
        )
    }
}
