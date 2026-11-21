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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.BundledPlacesCatalog
import com.example.data.repository.MosqueRepository
import com.example.data.repository.MosqueSearchResult
import com.example.data.repository.NearbyMosque
import com.example.data.repository.SourceAttemptDiagnostic
import com.example.ui.components.FallbackSavedListBanner
import com.example.ui.components.MapPlacePin
import com.example.ui.components.NearbyPlacesMapView
import com.example.ui.components.formatRadiusExpansionBanner
import com.example.ui.components.openExternalGoogleMapsNearMe
import com.example.util.BanglaUtils
import com.example.util.LocalAppLanguage
import com.example.util.LocationHelper
import com.example.util.isEnglishLang
import com.example.util.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

private enum class MosqueViewMode { MAP, LIST }

private sealed class MosqueScreenState {
    data object NeedsPermission : MosqueScreenState()
    data object LocationOff : MosqueScreenState()
    data object Loading : MosqueScreenState()
    data class Loaded(
        val mosques: List<NearbyMosque>,
        val requestedRadiusMeters: Int,
        val effectiveRadiusMeters: Int,
        val answeredMirror: String = "",
        val isBundledFallback: Boolean = false,
        val isOfflineFallback: Boolean = false,
        val diagnostics: List<SourceAttemptDiagnostic> = emptyList()
    ) : MosqueScreenState() {
        val wasExpanded: Boolean get() = !isBundledFallback && effectiveRadiusMeters > requestedRadiusMeters
    }
    data class Error(
        val message: String,
        val diagnostics: List<SourceAttemptDiagnostic> = emptyList()
    ) : MosqueScreenState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosqueFinderScreen(
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
    val repository = remember(context) { MosqueRepository(context) }

    var screenState by remember { mutableStateOf<MosqueScreenState>(MosqueScreenState.Loading) }
    var viewMode by remember { mutableStateOf(MosqueViewMode.MAP) }
    var searchQuery by remember { mutableStateOf("") }
    var radiusMeters by remember { mutableIntStateOf(5000) }
    var userCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var userPlaceLabel by remember { mutableStateOf(fallbackLocationLabel) }
    var selectedMosque by remember { mutableStateOf<NearbyMosque?>(null) }
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

    fun showOfflineBundledFallback() {
        val refLat = userCoords?.first ?: fallbackLat ?: 23.6238
        val refLon = userCoords?.second ?: fallbackLon ?: 90.5000
        viewMode = MosqueViewMode.LIST
        screenState = MosqueScreenState.Loaded(
            mosques = BundledPlacesCatalog.mosquesSortedByDistance(refLat, refLon),
            requestedRadiusMeters = radiusMeters,
            effectiveRadiusMeters = radiusMeters,
            answeredMirror = "",
            isBundledFallback = true,
            isOfflineFallback = true,
            diagnostics = listOf(
                SourceAttemptDiagnostic(
                    sourceName = "network",
                    httpStatus = null,
                    elapsedMs = 20L,
                    errorKind = "no internet"
                )
            )
        )
    }

    fun applyMosqueSearchResult(res: MosqueSearchResult) {
        when (res) {
            is MosqueSearchResult.Offline -> showOfflineBundledFallback()
            is MosqueSearchResult.Success -> screenState = MosqueScreenState.Loaded(
                mosques = res.mosques,
                requestedRadiusMeters = res.requestedRadiusMeters,
                effectiveRadiusMeters = res.effectiveRadiusMeters,
                answeredMirror = res.answeredMirror
            )
            is MosqueSearchResult.FallbackBundled -> {
                viewMode = MosqueViewMode.LIST
                screenState = MosqueScreenState.Loaded(
                    mosques = res.mosques,
                    requestedRadiusMeters = radiusMeters,
                    effectiveRadiusMeters = radiusMeters,
                    answeredMirror = "",
                    isBundledFallback = true,
                    isOfflineFallback = false,
                    diagnostics = res.diagnostics
                )
            }
            is MosqueSearchResult.Error -> screenState = MosqueScreenState.Error(
                message = if (isEn) res.messageEn else res.messageBn,
                diagnostics = res.diagnostics
            )
        }
    }

    fun loadMosquesFor(lat: Double, lon: Double, radius: Int, forceRefresh: Boolean = false) {
        activeSearchJob?.cancel()
        activeSearchJob = scope.launch {
            if (!repository.isOnline()) {
                showOfflineBundledFallback()
                return@launch
            }
            screenState = MosqueScreenState.Loading
            selectedMapPinId = null
            try {
                val res = repository.findNearbyMosques(lat, lon, radius, forceRefresh = forceRefresh)
                applyMosqueSearchResult(res)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                val diag = listOf(
                    SourceAttemptDiagnostic(
                        sourceName = "search",
                        httpStatus = null,
                        elapsedMs = 0L,
                        errorKind = e.javaClass.simpleName,
                        detail = e.message.orEmpty()
                    )
                )
                viewMode = MosqueViewMode.LIST
                screenState = MosqueScreenState.Loaded(
                    mosques = BundledPlacesCatalog.mosquesSortedByDistance(lat, lon),
                    requestedRadiusMeters = radius,
                    effectiveRadiusMeters = radius,
                    isBundledFallback = true,
                    isOfflineFallback = !repository.isOnline(),
                    diagnostics = diag
                )
            }
        }
    }

    fun detectGpsAndLoad(forceRefresh: Boolean = false) {
        // Check offline immediately (< 50 ms) before even touching GPS
        if (!repository.isOnline()) {
            activeSearchJob?.cancel()
            showOfflineBundledFallback()
            return
        }
        if (!hasLocationPermission()) {
            activeSearchJob?.cancel()
            screenState = MosqueScreenState.NeedsPermission
            return
        }
        if (!isLocationEnabled() && (fallbackLat == null || fallbackLon == null)) {
            activeSearchJob?.cancel()
            screenState = MosqueScreenState.LocationOff
            return
        }
        activeSearchJob?.cancel()
        activeSearchJob = scope.launch {
            screenState = MosqueScreenState.Loading
            selectedMapPinId = null
            val resolved = LocationHelper.resolveLocationFast(
                context = context,
                fallbackLat = fallbackLat,
                fallbackLon = fallbackLon
            )
            if (resolved == null) {
                screenState = if (!isLocationEnabled()) {
                    MosqueScreenState.LocationOff
                } else {
                    MosqueScreenState.Error(
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
                val res = repository.findNearbyMosques(
                    userLat = resolved.latitude,
                    userLon = resolved.longitude,
                    radiusMeters = radiusMeters,
                    forceRefresh = forceRefresh
                )
                applyMosqueSearchResult(res)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                viewMode = MosqueViewMode.LIST
                screenState = MosqueScreenState.Loaded(
                    mosques = BundledPlacesCatalog.mosquesSortedByDistance(resolved.latitude, resolved.longitude),
                    requestedRadiusMeters = radiusMeters,
                    effectiveRadiusMeters = radiusMeters,
                    isBundledFallback = true,
                    isOfflineFallback = !repository.isOnline(),
                    diagnostics = listOf(
                        SourceAttemptDiagnostic(
                            sourceName = "search",
                            httpStatus = null,
                            elapsedMs = 0L,
                            errorKind = e.javaClass.simpleName,
                            detail = e.message.orEmpty()
                        )
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
            screenState = MosqueScreenState.NeedsPermission
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
                (screenState is MosqueScreenState.LocationOff || screenState is MosqueScreenState.NeedsPermission)
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
                        .testTag("mosque_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr(isEn, "পিছনে যান", "Back")
                    )
                }
                Column {
                    Text(
                        text = tr(isEn, "নিকটবর্তী মসজিদ", "Mosque Finder"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userPlaceLabel?.let {
                            "${tr(isEn, "বর্তমান অবস্থান", "Near")}: $it"
                        } ?: tr(isEn, "জিপিএস দিয়ে কাছের মসজিদ খুঁজুন", "Find nearby mosques via GPS"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = { detectGpsAndLoad(forceRefresh = true) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("mosque_refresh_button")
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
                selected = viewMode == MosqueViewMode.MAP,
                onClick = { viewMode = MosqueViewMode.MAP },
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
                    .testTag("mosque_view_map")
            )
            FilterChip(
                selected = viewMode == MosqueViewMode.LIST,
                onClick = { viewMode = MosqueViewMode.LIST },
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
                    .testTag("mosque_view_list")
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
                        "মসজিদের নাম বা এলাকা খুঁজুন...",
                        "Search mosque by name or area..."
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
                .testTag("mosque_search_input")
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
                            loadMosquesFor(coords.first, coords.second, r)
                        } else {
                            detectGpsAndLoad()
                        }
                    },
                    label = {
                        Text("${BanglaUtils.formatNumber(km, isEn)} ${tr(isEn, "কি.মি.", "km")}")
                    },
                    modifier = Modifier.testTag("mosque_radius_$r")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (val state = screenState) {
            is MosqueScreenState.NeedsPermission -> {
                MosqueStatusCard(
                    icon = Icons.Default.MyLocation,
                    title = tr(isEn, "লোকেশন অনুমতি প্রয়োজন", "Location Permission Needed"),
                    body = tr(
                        isEn,
                        "আপনার কাছাকাছি মসজিদগুলো দূরত্ব অনুযায়ী দেখাতে জিপিএস লোকেশন অনুমতি দিন।",
                        "Grant location permission so we can find nearby mosques sorted by distance."
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
                    buttonTag = "mosque_allow_permission_button"
                )
            }

            is MosqueScreenState.LocationOff -> {
                MosqueStatusCard(
                    icon = Icons.Default.LocationOff,
                    title = tr(isEn, "জিপিএস / লোকেশন বন্ধ আছে", "GPS / Location Is Turned Off"),
                    body = tr(
                        isEn,
                        "কাছাকাছি মসজিদ খুঁজতে অনুগ্রহ করে ফোনের লোকেশন (GPS) চালু করুন।",
                        "Please turn on your device Location (GPS) to find nearby mosques."
                    ),
                    primaryButtonLabel = tr(isEn, "লোকেশন সেটিংস খুলুন", "Open Location Settings"),
                    onPrimaryClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    buttonTag = "mosque_open_location_settings"
                )
            }

            is MosqueScreenState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = tr(isEn, "কাছাকাছি মসজিদ খোঁজা হচ্ছে...", "Searching for nearby mosques..."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = tr(
                                isEn,
                                "সর্বোচ্চ ১৫ সেকেন্ডের মধ্যে ফলাফল দেখানো হবে",
                                "Results or saved list will load within 15 seconds"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is MosqueScreenState.Error -> {
                val diagSummary = if (state.diagnostics.isNotEmpty()) {
                    state.diagnostics.joinToString("\n") { it.formatLine(isEn) }
                } else {
                    tr(
                        isEn,
                        "অবস্থান যাচাই করে আবার চেষ্টা করুন অথবা সরাসরি Google Maps-এ খুঁজুন।",
                        "Please verify location and retry, or open directly in Google Maps."
                    )
                }
                MosqueStatusCard(
                    icon = Icons.Default.Mosque,
                    title = state.message,
                    body = diagSummary,
                    primaryButtonLabel = tr(isEn, "আবার চেষ্টা করুন", "Try Again"),
                    onPrimaryClick = { detectGpsAndLoad(forceRefresh = true) },
                    buttonTag = "mosque_error_retry"
                )
            }

            is MosqueScreenState.Loaded -> {
                val filtered = remember(state.mosques, searchQuery) {
                    MosqueRepository.filterMosques(state.mosques, searchQuery)
                }

                // Fallback banner with real server diagnostics + Retry + Open in Google Maps
                if (state.isBundledFallback) {
                    FallbackSavedListBanner(
                        isOffline = state.isOfflineFallback,
                        diagnostics = state.diagnostics,
                        isEnglish = isEn,
                        onRetry = { detectGpsAndLoad(forceRefresh = true) },
                        onOpenGoogleMaps = {
                            openExternalGoogleMapsNearMe(
                                context = context,
                                latitude = userCoords?.first ?: fallbackLat,
                                longitude = userCoords?.second ?: fallbackLon,
                                queryTerm = "mosque near me",
                                isEnglish = isEn,
                                onStatusMessage = onStatusMessage
                            )
                        },
                        testTagPrefix = "mosque"
                    )
                }

                // Auto-expanded radius notice banner
                if (state.wasExpanded && state.mosques.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("mosque_expanded_radius_banner")
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
                                    isMosque = true,
                                    isEnglish = isEn
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    MosqueStatusCard(
                        icon = Icons.Default.Mosque,
                        title = tr(isEn, "এই এলাকায় মসজিদ পাওয়া যায়নি", "No Mosques Found in This Area"),
                        body = if (searchQuery.isNotBlank()) {
                            tr(isEn, "আপনার অনুসন্ধানের সাথে মিলে এমন কোনো মসজিদ পাওয়া যায়নি।", "No mosques matched your search query.")
                        } else {
                            tr(
                                isEn,
                                "২০ কিমি দূরত্বের মধ্যে কোনো মসজিদ পাওয়া যায়নি। আবার চেষ্টা করুন।",
                                "No mosques were found even after expanding to 20 km. Please try again."
                            )
                        },
                        primaryButtonLabel = tr(isEn, "পুনরায় খুঁজুন", "Search Again"),
                        onPrimaryClick = { detectGpsAndLoad(forceRefresh = true) },
                        buttonTag = "mosque_empty_retry"
                    )
                } else if (viewMode == MosqueViewMode.MAP) {
                    val coords = userCoords
                    val mapLat = coords?.first ?: filtered.first().latitude
                    val mapLon = coords?.second ?: filtered.first().longitude
                    val pins = remember(filtered, isEn) {
                        filtered.map { m ->
                            val dist = formatDistanceKm(m.distanceKm, isEn)
                            val dir = MosqueRepository.cardinalLabel(m.bearingFromUserDeg, isEn)
                            val qiblaDeg = BanglaUtils.formatNumber(m.qiblaBearingDeg.toInt(), isEn)
                            MapPlacePin(
                                id = m.id,
                                title = m.displayName(isEn),
                                subtitle = m.address,
                                distanceLabel = "$dist ($dir)",
                                badge = "${tr(isEn, "কিবলা", "Qibla")} $qiblaDeg°",
                                latitude = m.latitude,
                                longitude = m.longitude
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
                            "${BanglaUtils.formatNumber(filtered.size, false)}টি মসজিদ",
                            "${filtered.size} Mosques"
                        ),
                        pinColor = MaterialTheme.colorScheme.primary,
                        mirrorSourceLabel = state.answeredMirror,
                        onSelectPin = { pinId -> selectedMapPinId = pinId },
                        onClearSelection = { selectedMapPinId = null },
                        onOpenDirections = { pin ->
                            openMosqueDirectionsInGoogleMaps(context, pin.latitude, pin.longitude, pin.title, isEn, onStatusMessage)
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
                                    "${BanglaUtils.formatNumber(filtered.size, false)}টি মসজিদ পাওয়া গেছে (দূরত্ব অনুযায়ী)$mirrorTag",
                                    "${filtered.size} mosques found (sorted by distance)$mirrorTag"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(filtered, key = { it.id }) { mosque ->
                            MosqueListCard(
                                mosque = mosque,
                                isEnglish = isEn,
                                onClick = { selectedMosque = mosque },
                                onOpenMaps = {
                                    openMosqueDirectionsInGoogleMaps(
                                        context,
                                        mosque.latitude,
                                        mosque.longitude,
                                        mosque.displayName(isEn),
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

    // Mosque Detail Sheet with Qibla direction + Directions
    selectedMosque?.let { mosque ->
        val distStr = formatDistanceKm(mosque.distanceKm, isEn)
        val dirStr = MosqueRepository.cardinalLabel(mosque.bearingFromUserDeg, isEn)
        val qiblaDirStr = MosqueRepository.cardinalLabel(mosque.qiblaBearingDeg, isEn)
        val qiblaDegStr = BanglaUtils.formatNumber(mosque.qiblaBearingDeg.toInt(), isEn)

        ModalBottomSheet(onDismissRequest = { selectedMosque = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mosque.displayName(isEn),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (mosque.address.isNotBlank()) {
                            Text(
                                text = mosque.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = tr(isEn, "দূরত্ব ও দিক", "Distance & Direction"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$distStr • $dirStr",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = tr(isEn, "এখান থেকে কিবলা", "Qibla from here"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$qiblaDegStr° ($qiblaDirStr)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedMapPinId = mosque.id
                            selectedMosque = null
                            viewMode = MosqueViewMode.MAP
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr(isEn, "ম্যাপে দেখুন", "Show on Map"))
                    }
                    Button(
                        onClick = {
                            openMosqueDirectionsInGoogleMaps(
                                context,
                                mosque.latitude,
                                mosque.longitude,
                                mosque.displayName(isEn),
                                isEn,
                                onStatusMessage
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mosque_open_maps_button")
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr(isEn, "দিকনির্দেশনা", "Directions"))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun MosqueListCard(
    mosque: NearbyMosque,
    isEnglish: Boolean,
    onClick: () -> Unit,
    onOpenMaps: () -> Unit
) {
    val distStr = formatDistanceKm(mosque.distanceKm, isEnglish)
    val dirStr = MosqueRepository.cardinalLabel(mosque.bearingFromUserDeg, isEnglish)
    val qiblaDeg = BanglaUtils.formatNumber(mosque.qiblaBearingDeg.toInt(), isEnglish)
    val qiblaDir = MosqueRepository.cardinalLabel(mosque.qiblaBearingDeg, isEnglish)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("mosque_card_${mosque.id}"),
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
                    imageVector = Icons.Default.Mosque,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = mosque.displayName(isEnglish),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (mosque.address.isNotBlank()) {
                    Text(
                        text = mosque.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$distStr ($dirStr) • ${tr(isEnglish, "কিবলা", "Qibla")} $qiblaDeg° $qiblaDir",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            OutlinedButton(
                onClick = onOpenMaps,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Directions, contentDescription = tr(isEnglish, "দিকনির্দেশনা", "Directions"), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MosqueStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

private fun formatDistanceKm(distanceKm: Double, isEnglish: Boolean): String {
    val formatted = String.format(Locale.US, "%.2f", distanceKm)
    return if (isEnglish) "$formatted km" else "${BanglaUtils.toBanglaDigits(formatted)} কি.মি."
}

private fun openMosqueDirectionsInGoogleMaps(
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
