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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
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
import com.example.ui.components.MapPlacePin
import com.example.ui.components.NearbyPlacesMapView
import com.example.ui.components.formatRadiusExpansionBanner
import com.example.ui.theme.StatusOnTimeColor
import com.example.util.BanglaUtils
import com.example.util.LocalAppLanguage
import com.example.util.LocationHelper
import com.example.util.isEnglishLang
import com.example.util.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

private enum class HalalViewMode { MAP, LIST }

private sealed class HalalScreenState {
    data object NeedsPermission : HalalScreenState()
    data object LocationOff : HalalScreenState()
    data object Loading : HalalScreenState()
    data object Offline : HalalScreenState()
    data class Loaded(
        val restaurants: List<NearbyHalalRestaurant>,
        val requestedRadiusMeters: Int,
        val effectiveRadiusMeters: Int,
        val answeredMirror: String = ""
    ) : HalalScreenState() {
        val wasExpanded: Boolean get() = effectiveRadiusMeters > requestedRadiusMeters
    }
    data class Error(val message: String) : HalalScreenState()
}

@Composable
fun HalalFinderScreen(
    fallbackLat: Double? = null,
    fallbackLon: Double? = null,
    fallbackLocationLabel: String? = null,
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
    var viewMode by remember { mutableStateOf(HalalViewMode.MAP) }
    var searchQuery by remember { mutableStateOf("") }
    var radiusMeters by remember { mutableIntStateOf(5000) }
    var userCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var userPlaceLabel by remember { mutableStateOf(fallbackLocationLabel) }
    var selectedMapPinId by remember { mutableStateOf<String?>(null) }
    var activeSearchJob by remember { mutableStateOf<Job?>(null) }
    var geocodeJob by remember { mutableStateOf<Job?>(null) }

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

    fun loadHalalPlacesFor(lat: Double, lon: Double, radius: Int, forceRefresh: Boolean = false) {
        activeSearchJob?.cancel()
        activeSearchJob = scope.launch {
            if (!repository.isOnline()) {
                screenState = HalalScreenState.Offline
                return@launch
            }
            screenState = HalalScreenState.Loading
            selectedMapPinId = null
            try {
                when (val res = repository.findNearbyHalalRestaurants(lat, lon, radius, forceRefresh = forceRefresh)) {
                    is HalalSearchResult.Offline -> screenState = HalalScreenState.Offline
                    is HalalSearchResult.Success -> screenState = HalalScreenState.Loaded(
                        restaurants = res.restaurants,
                        requestedRadiusMeters = res.requestedRadiusMeters,
                        effectiveRadiusMeters = res.effectiveRadiusMeters,
                        answeredMirror = res.answeredMirror
                    )
                    is HalalSearchResult.Error -> screenState = HalalScreenState.Error(
                        if (isEn) res.messageEn else res.messageBn
                    )
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                screenState = HalalScreenState.Error(
                    tr(
                        isEn,
                        "তিনটি সার্ভারই ব্যস্ত, ১ মিনিট পর আবার চেষ্টা করুন",
                        "All 3 servers are busy, please try again in 1 minute"
                    )
                )
            }
        }
    }

    fun detectGpsAndLoad(forceRefresh: Boolean = false) {
        // Check offline immediately (< 50 ms) before even touching GPS
        if (!repository.isOnline()) {
            activeSearchJob?.cancel()
            screenState = HalalScreenState.Offline
            return
        }
        if (!hasLocationPermission()) {
            activeSearchJob?.cancel()
            screenState = HalalScreenState.NeedsPermission
            return
        }
        if (!isLocationEnabled() && (fallbackLat == null || fallbackLon == null)) {
            activeSearchJob?.cancel()
            screenState = HalalScreenState.LocationOff
            return
        }
        activeSearchJob?.cancel()
        activeSearchJob = scope.launch {
            screenState = HalalScreenState.Loading
            selectedMapPinId = null
            val resolved = LocationHelper.resolveLocationFast(
                context = context,
                fallbackLat = fallbackLat,
                fallbackLon = fallbackLon
            )
            if (resolved == null) {
                screenState = if (!isLocationEnabled()) {
                    HalalScreenState.LocationOff
                } else {
                    HalalScreenState.Error(
                        tr(
                            isEn,
                            "জিপিএস লোকেশন পাওয়া যায়নি। লোকেশন চালু করে আবার চেষ্টা করুন।",
                            "GPS location unavailable. Please enable Location and try again."
                        )
                    )
                }
                return@launch
            }

            userCoords = resolved.latitude to resolved.longitude

            // Non-blocking reverse geocode so Overpass starts immediately
            geocodeJob?.cancel()
            geocodeJob = scope.launch {
                val place = LocationHelper.reverseGeocodeCityCountry(
                    context,
                    resolved.latitude,
                    resolved.longitude,
                    isEn
                )
                if (!place.isNullOrBlank()) {
                    userPlaceLabel = place
                }
            }

            try {
                when (
                    val res = repository.findNearbyHalalRestaurants(
                        userLat = resolved.latitude,
                        userLon = resolved.longitude,
                        radiusMeters = radiusMeters,
                        forceRefresh = forceRefresh
                    )
                ) {
                    is HalalSearchResult.Offline -> screenState = HalalScreenState.Offline
                    is HalalSearchResult.Success -> screenState = HalalScreenState.Loaded(
                        restaurants = res.restaurants,
                        requestedRadiusMeters = res.requestedRadiusMeters,
                        effectiveRadiusMeters = res.effectiveRadiusMeters,
                        answeredMirror = res.answeredMirror
                    )
                    is HalalSearchResult.Error -> screenState = HalalScreenState.Error(
                        if (isEn) res.messageEn else res.messageBn
                    )
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                screenState = HalalScreenState.Error(
                    tr(
                        isEn,
                        "তিনটি সার্ভারই ব্যস্ত, ১ মিনিট পর আবার চেষ্টা করুন",
                        "All 3 servers are busy, please try again in 1 minute"
                    )
                )
            }
        }
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

    // Cancel any in-flight Overpass / GPS requests when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            activeSearchJob?.cancel()
            geocodeJob?.cancel()
        }
    }

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
                onClick = { detectGpsAndLoad(forceRefresh = true) },
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

        // Map / List Toggle Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = viewMode == HalalViewMode.MAP,
                onClick = { viewMode = HalalViewMode.MAP },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = { Text(tr(isEn, "ম্যাপ (Map)", "Map View")) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("halal_view_map")
            )
            FilterChip(
                selected = viewMode == HalalViewMode.LIST,
                onClick = { viewMode = HalalViewMode.LIST },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = { Text(tr(isEn, "তালিকা (List)", "List View")) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("halal_view_list")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

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

        Spacer(modifier = Modifier.height(6.dp))

        // Radius Filter Row (2 / 5 / 10 / 20 km)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Text(
                    text = tr(isEn, "দূরত্ব:", "Radius:"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(MosqueRepository.RADIUS_STEPS_METERS) { r ->
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

        Spacer(modifier = Modifier.height(6.dp))

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
                    title = tr(isEn, "ইন্টারনেট নেই", "No Internet Connection"),
                    body = tr(
                        isEn,
                        "হালাল রেস্টুরেন্ট খুঁজতে ও ম্যাপ দেখতে ইন্টারনেট চালু করে আবার চেষ্টা করুন।",
                        "Turn on internet connection to search nearby halal restaurants and view the map, then retry."
                    ),
                    primaryButtonLabel = tr(isEn, "আবার চেষ্টা করুন", "Try Again"),
                    onPrimaryClick = { detectGpsAndLoad(forceRefresh = true) },
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
                    title = state.message,
                    body = tr(
                        isEn,
                        "ওপেনস্ট্রিটম্যাপ সার্ভার থেকে তথ্য আনতে সময় লাগছে। কিছুক্ষণ পর আবার চেষ্টা করুন।",
                        "Could not get a timely response from the map server. Please try again."
                    ),
                    primaryButtonLabel = tr(isEn, "আবার চেষ্টা করুন", "Try Again"),
                    onPrimaryClick = { detectGpsAndLoad(forceRefresh = true) },
                    buttonTag = "halal_error_retry"
                )
            }

            is HalalScreenState.Loaded -> {
                val filtered = remember(state.restaurants, searchQuery) {
                    HalalRestaurantRepository.filterRestaurants(state.restaurants, searchQuery)
                }

                // Auto-expanded radius notice banner
                if (state.wasExpanded && state.restaurants.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("halal_expanded_radius_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = formatRadiusExpansionBanner(
                                    requestedRadiusMeters = state.requestedRadiusMeters,
                                    effectiveRadiusMeters = state.effectiveRadiusMeters,
                                    isMosque = false,
                                    isEnglish = isEn
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    HalalStatusCard(
                        icon = Icons.Default.Restaurant,
                        title = tr(isEn, "এই এলাকায় রেস্টুরেন্ট পাওয়া যায়নি", "No Restaurants Found in This Area"),
                        body = if (searchQuery.isNotBlank()) {
                            tr(
                                isEn,
                                "আপনার অনুসন্ধানের সাথে মিলে এমন কোনো রেস্টুরেন্ট পাওয়া যায়নি।",
                                "No restaurants matched your search query."
                            )
                        } else {
                            tr(
                                isEn,
                                "২০ কিমি দূরত্বের মধ্যে কোনো রেস্টুরেন্ট পাওয়া যায়নি। আবার চেষ্টা করুন।",
                                "No restaurants were found even after expanding to 20 km. Please try again."
                            )
                        },
                        primaryButtonLabel = tr(isEn, "পুনরায় খুঁজুন", "Search Again"),
                        onPrimaryClick = { detectGpsAndLoad(forceRefresh = true) },
                        buttonTag = "halal_empty_retry"
                    )
                } else if (viewMode == HalalViewMode.MAP) {
                    val coords = userCoords
                    val mapLat = coords?.first ?: filtered.first().latitude
                    val mapLon = coords?.second ?: filtered.first().longitude
                    val pins = remember(filtered, isEn) {
                        filtered.map { r ->
                            val dist = formatHalalDistanceKm(r.distanceKm, isEn)
                            val dir = MosqueRepository.cardinalLabel(r.bearingFromUserDeg, isEn)
                            val sub = listOf(r.cuisine, r.address)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            MapPlacePin(
                                id = r.id,
                                title = r.displayName(isEn),
                                subtitle = sub,
                                distanceLabel = "$dist ($dir)",
                                badge = r.halalBadge(isEn),
                                latitude = r.latitude,
                                longitude = r.longitude
                            )
                        }
                    }
                    NearbyPlacesMapView(
                        userLat = mapLat,
                        userLon = mapLon,
                        userLocationLabel = userPlaceLabel ?: tr(isEn, "আপনার অবস্থান", "Your Location"),
                        pins = pins,
                        selectedPinId = selectedMapPinId,
                        isEnglish = isEn,
                        countBadgeText = tr(
                            isEn,
                            "${BanglaUtils.formatNumber(filtered.size, false)}টি রেস্টুরেন্ট",
                            "${filtered.size} Places"
                        ),
                        pinColor = StatusOnTimeColor,
                        mirrorSourceLabel = state.answeredMirror,
                        onSelectPin = { pinId -> selectedMapPinId = pinId },
                        onClearSelection = { selectedMapPinId = null },
                        onOpenDirections = { pin ->
                            openHalalDirectionsInGoogleMaps(
                                context,
                                pin.latitude,
                                pin.longitude,
                                pin.title,
                                isEn,
                                onStatusMessage
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            val mirrorTag = if (state.answeredMirror.isNotBlank()) " • ${state.answeredMirror}" else ""
                            Text(
                                text = tr(
                                    isEn,
                                    "${BanglaUtils.formatNumber(filtered.size, false)}টি রেস্টুরেন্ট (হালাল আগে ও দূরত্ব অনুযায়ী)$mirrorTag",
                                    "${filtered.size} restaurants (Halal first, then nearest)$mirrorTag"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(filtered, key = { it.id }) { place ->
                            HalalRestaurantCard(
                                place = place,
                                isEnglish = isEn,
                                onCardTap = {
                                    selectedMapPinId = place.id
                                    viewMode = HalalViewMode.MAP
                                },
                                onOpenMaps = {
                                    openHalalDirectionsInGoogleMaps(
                                        context,
                                        place.latitude,
                                        place.longitude,
                                        place.displayName(isEn),
                                        isEn,
                                        onStatusMessage
                                    )
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
    onCardTap: () -> Unit,
    onOpenMaps: () -> Unit
) {
    val distStr = formatHalalDistanceKm(place.distanceKm, isEnglish)
    val dirStr = MosqueRepository.cardinalLabel(place.bearingFromUserDeg, isEnglish)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardTap)
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
                    contentDescription = tr(isEnglish, "দিকনির্দেশনা", "Directions"),
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

private fun openHalalDirectionsInGoogleMaps(
    context: Context,
    latitude: Double,
    longitude: Double,
    placeName: String,
    isEnglish: Boolean,
    onStatusMessage: (String) -> Unit
) {
    val navUri = Uri.parse("google.navigation:q=$latitude,$longitude")
    val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    val label = Uri.encode(placeName)
    val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
    val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
    try {
        when {
            navIntent.resolveActivity(context.packageManager) != null -> context.startActivity(navIntent)
            geoIntent.resolveActivity(context.packageManager) != null -> context.startActivity(geoIntent)
            else -> {
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        }
    } catch (_: Exception) {
        onStatusMessage(
            tr(isEnglish, "দিকনির্দেশনা খোলা যায়নি", "Could not open directions")
        )
    }
}
