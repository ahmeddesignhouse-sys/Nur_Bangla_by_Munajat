package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.repository.HalalRestaurantRepository
import com.example.data.repository.HalalSearchResult
import com.example.data.repository.MosqueRepository
import com.example.data.repository.NearbyHalalRestaurant
import com.example.ui.theme.StatusOnTimeColor
import com.example.util.BanglaUtils
import com.example.util.LocalAppLanguage
import com.example.util.LocationHelper
import com.example.util.isEnglishLang
import com.example.util.tr
import kotlinx.coroutines.launch
import java.util.Locale

private sealed class HalalScreenState {
    data object NeedsPermission : HalalScreenState()
    data object LocationOff : HalalScreenState()
    data object Loading : HalalScreenState()
    data object Offline : HalalScreenState()
    data class Loaded(val restaurants: List<NearbyHalalRestaurant>) : HalalScreenState()
    data class Error(val message: String) : HalalScreenState()
}

@Composable
fun HalalFinderScreen(
    onStatusMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isEn = isEnglishLang(LocalAppLanguage.current)
    val repository = remember(context) { HalalRestaurantRepository(context) }

    var screenState by remember { mutableStateOf<HalalScreenState>(HalalScreenState.Loading) }
    var searchQuery by remember { mutableStateOf("") }
    var radiusMeters by remember { mutableIntStateOf(5000) }
    var userCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var userPlaceLabel by remember { mutableStateOf<String?>(null) }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    fun loadHalalPlacesFor(lat: Double, lon: Double, radius: Int) {
        scope.launch {
            screenState = HalalScreenState.Loading
            when (val res = repository.findNearbyHalalRestaurants(lat, lon, radius)) {
                is HalalSearchResult.Offline -> screenState = HalalScreenState.Offline
                is HalalSearchResult.Success -> screenState = HalalScreenState.Loaded(res.restaurants)
                is HalalSearchResult.Error -> screenState = HalalScreenState.Error(
                    if (isEn) res.messageEn else res.messageBn
                )
            }
        }
    }

    fun detectGpsAndLoad() {
        if (!hasLocationPermission()) {
            screenState = HalalScreenState.NeedsPermission
            return
        }
        if (!isLocationEnabled()) {
            screenState = HalalScreenState.LocationOff
            return
        }
        if (!repository.isOnline()) {
            screenState = HalalScreenState.Offline
            return
        }
        screenState = HalalScreenState.Loading
        LocationHelper.fetchCurrentGpsLocation(
            context = context,
            scope = scope,
            isEnglish = isEn,
            showToast = false,
            onResolved = { lat, lon, label, _ ->
                userCoords = lat to lon
                userPlaceLabel = label
                loadHalalPlacesFor(lat, lon, radiusMeters)
            },
            onError = { errMsg ->
                screenState = HalalScreenState.Error(errMsg)
            }
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            detectGpsAndLoad()
        } else {
            screenState = HalalScreenState.NeedsPermission
        }
    }

    LaunchedEffect(Unit) {
        detectGpsAndLoad()
    }

    // Auto-refresh when returning from system Location settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                (screenState is HalalScreenState.LocationOff || screenState is HalalScreenState.NeedsPermission)
            ) {
                detectGpsAndLoad()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("halal_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr(isEn, "পিছনে যান", "Back")
                    )
                }
                Column {
                    Text(
                        text = tr(isEn, "হালাল রেস্টুরেন্ট", "Halal Food Finder"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userPlaceLabel?.let {
                            "${tr(isEn, "বর্তমান অবস্থান", "Near")}: $it"
                        } ?: tr(isEn, "জিপিএস দিয়ে কাছের হালাল খাবার খুঁজুন", "Find nearby halal restaurants via GPS"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = { detectGpsAndLoad() },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("halal_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = tr(isEn, "রিফ্রেশ", "Refresh")
                )
            }
        }

        // Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    tr(
                        isEn,
                        "নাম, এলাকা বা খাবারের ধরন খুঁজুন...",
                        "Search by name, area, or cuisine..."
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
                .testTag("halal_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Radius Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr(isEn, "দূরত্ব:", "Radius:"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            listOf(2000, 5000, 10000).forEach { r ->
                val km = r / 1000
                FilterChip(
                    selected = radiusMeters == r,
                    onClick = {
                        radiusMeters = r
                        val coords = userCoords
                        if (coords != null) {
                            loadHalalPlacesFor(coords.first, coords.second, r)
                        } else {
                            detectGpsAndLoad()
                        }
                    },
                    label = {
                        Text("${BanglaUtils.formatNumber(km, isEn)} ${tr(isEn, "কি.মি.", "km")}")
                    },
                    modifier = Modifier.testTag("halal_radius_$r")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (val state = screenState) {
            is HalalScreenState.NeedsPermission -> {
                HalalStatusCard(
                    icon = Icons.Default.MyLocation,
                    title = tr(isEn, "লোকেশন অনুমতি প্রয়োজন", "Location Permission Needed"),
                    body = tr(
                        isEn,
                        "আপনার কাছাকাছি হালাল রেস্টুরেন্টগুলো দূরত্ব অনুযায়ী দেখাতে জিপিএস লোকেশন অনুমতি দিন।",
                        "Grant location permission to discover nearby halal restaurants sorted by distance."
                    ),
                    primaryButtonLabel = tr(isEn, "লোকেশন অনুমতি দিন", "Allow Location"),
                    onPrimaryClick = {
                        permLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    buttonTag = "halal_allow_permission_button"
                )
            }

            is HalalScreenState.LocationOff -> {
                HalalStatusCard(
                    icon = Icons.Default.LocationOff,
                    title = tr(isEn, "জিপিএস / লোকেশন বন্ধ আছে", "GPS / Location Is Turned Off"),
                    body = tr(
                        isEn,
                        "কাছাকাছি হালাল রেস্টুরেন্ট খুঁজতে অনুগ্রহ করে ফোনের লোকেশন (GPS) চালু করুন।",
                        "Please turn on your device Location (GPS) to find nearby halal restaurants."
                    ),
                    primaryButtonLabel = tr(isEn, "লোকেশন সেটিংস খুলুন", "Open Location Settings"),
                    onPrimaryClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    buttonTag = "halal_open_location_settings"
                )
            }

            is HalalScreenState.Offline -> {
                HalalStatusCard(
                    icon = Icons.Default.WifiOff,
                    title = tr(isEn, "ইন্টারনেট সংযোগ প্রয়োজন", "Internet Required"),
                    body = tr(
                        isEn,
                        "হালাল রেস্টুরেন্ট খোঁজার জন্য ইন্টারনেট সংযোগ প্রয়োজন (OpenStreetMap প্লেস ডেটা)। ইন্টারনেট চালু করে পুনরায় চেষ্টা করুন।",
                        "Internet connection is required to search nearby halal restaurants (OpenStreetMap Places data). Connect and retry."
                    ),
                    primaryButtonLabel = tr(isEn, "পুনরায় চেষ্টা করুন", "Retry"),
                    onPrimaryClick = { detectGpsAndLoad() },
                    buttonTag = "halal_offline_retry"
                )
            }

            is HalalScreenState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = tr(
                                isEn,
                                "কাছাকাছি হালাল রেস্টুরেন্ট খোঁজা হচ্ছে...",
                                "Searching for nearby halal restaurants..."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is HalalScreenState.Error -> {
                HalalStatusCard(
                    icon = Icons.Default.Restaurant,
                    title = tr(isEn, "তথ্য লোড করা যায়নি", "Unable to Load Places"),
                    body = state.message,
                    primaryButtonLabel = tr(isEn, "আবার চেষ্টা করুন", "Try Again"),
                    onPrimaryClick = { detectGpsAndLoad() },
                    buttonTag = "halal_error_retry"
                )
            }

            is HalalScreenState.Loaded -> {
                val filtered = remember(state.restaurants, searchQuery) {
                    HalalRestaurantRepository.filterRestaurants(state.restaurants, searchQuery)
                }
                if (filtered.isEmpty()) {
                    HalalStatusCard(
                        icon = Icons.Default.Restaurant,
                        title = tr(isEn, "কোনো হালাল রেস্টুরেন্ট পাওয়া যায়নি", "No Halal Restaurants Found"),
                        body = if (searchQuery.isNotBlank()) {
                            tr(
                                isEn,
                                "আপনার অনুসন্ধানের সাথে মিলে এমন কোনো রেস্টুরেন্ট পাওয়া যায়নি।",
                                "No halal restaurants matched your search query."
                            )
                        } else {
                            tr(
                                isEn,
                                "এই দূরত্বের মধ্যে কোনো হালাল রেস্টুরেন্ট পাওয়া যায়নি। দূরত্ব বাড়িয়ে (যেমন ১০ কি.মি.) চেষ্টা করুন।",
                                "No halal restaurants found within this radius. Try expanding the search radius to 10 km."
                            )
                        },
                        primaryButtonLabel = tr(isEn, "পুনরায় খুঁজুন", "Search Again"),
                        onPrimaryClick = { detectGpsAndLoad() },
                        buttonTag = "halal_empty_retry"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = tr(
                                    isEn,
                                    "${BanglaUtils.formatNumber(filtered.size, false)}টি হালাল রেস্টুরেন্ট (দূরত্ব অনুযায়ী • ট্যাপ করে গুগল ম্যাপে খুলুন)",
                                    "${filtered.size} halal restaurants (sorted by distance • tap to open Google Maps)"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(filtered, key = { it.id }) { place ->
                            HalalRestaurantCard(
                                place = place,
                                isEnglish = isEn,
                                onOpenMaps = {
                                    openHalalPlaceInGoogleMaps(context, place, isEn, onStatusMessage)
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HalalRestaurantCard(
    place: NearbyHalalRestaurant,
    isEnglish: Boolean,
    onOpenMaps: () -> Unit
) {
    val distStr = formatHalalDistanceKm(place.distanceKm, isEnglish)
    val dirStr = MosqueRepository.cardinalLabel(place.bearingFromUserDeg, isEnglish)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenMaps)
            .testTag("halal_card_${place.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = place.displayName(isEnglish),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Halal badge + optional rating pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = StatusOnTimeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = StatusOnTimeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = place.halalBadge(isEnglish),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusOnTimeColor
                            )
                        }
                    }

                    place.rating?.let { ratingVal ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isEnglish) ratingVal else BanglaUtils.toBanglaDigits(ratingVal),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                if (place.cuisine.isNotBlank()) {
                    Text(
                        text = place.cuisine,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (place.address.isNotBlank()) {
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$distStr ($dirStr)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    place.openingHours?.let { hours ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = hours,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenMaps,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("halal_maps_${place.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = tr(isEnglish, "গুগল ম্যাপে খুলুন", "Open in Google Maps"),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HalalStatusCard(
    icon: ImageVector,
    title: String,
    body: String,
    primaryButtonLabel: String,
    onPrimaryClick: () -> Unit,
    buttonTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onPrimaryClick,
                    modifier = Modifier.testTag(buttonTag)
                ) {
                    Text(primaryButtonLabel)
                }
            }
        }
    }
}

private fun formatHalalDistanceKm(distanceKm: Double, isEnglish: Boolean): String {
    val formatted = String.format(Locale.US, "%.2f", distanceKm)
    return if (isEnglish) "$formatted km" else "${BanglaUtils.toBanglaDigits(formatted)} কি.মি."
}

private fun openHalalPlaceInGoogleMaps(
    context: Context,
    place: NearbyHalalRestaurant,
    isEnglish: Boolean,
    onStatusMessage: (String) -> Unit
) {
    val label = Uri.encode(place.displayName(isEnglish))
    val geoUri = Uri.parse("geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}($label)")
    val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        if (mapsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapsIntent)
        } else {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${place.latitude},${place.longitude}")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    } catch (_: Exception) {
        onStatusMessage(
            tr(isEnglish, "গুগল ম্যাপ খোলা যায়নি", "Could not open Google Maps")
        )
    }
}
