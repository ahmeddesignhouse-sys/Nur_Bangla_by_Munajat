package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOff
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
import com.example.data.repository.MosqueRepository
import com.example.data.repository.MosqueSearchResult
import com.example.data.repository.NearbyMosque
import com.example.util.BanglaUtils
import com.example.util.LocalAppLanguage
import com.example.util.LocationHelper
import com.example.util.isEnglishLang
import com.example.util.tr
import kotlinx.coroutines.launch
import java.util.Locale

private sealed class MosqueScreenState {
    data object NeedsPermission : MosqueScreenState()
    data object LocationOff : MosqueScreenState()
    data object Loading : MosqueScreenState()
    data object Offline : MosqueScreenState()
    data class Loaded(val mosques: List<NearbyMosque>) : MosqueScreenState()
    data class Error(val message: String) : MosqueScreenState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosqueFinderScreen(
    onStatusMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isEn = isEnglishLang(LocalAppLanguage.current)
    val repository = remember(context) { MosqueRepository(context) }

    var screenState by remember { mutableStateOf<MosqueScreenState>(MosqueScreenState.Loading) }
    var searchQuery by remember { mutableStateOf("") }
    var radiusMeters by remember { mutableIntStateOf(5000) }
    var userCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var userPlaceLabel by remember { mutableStateOf<String?>(null) }
    var selectedMosque by remember { mutableStateOf<NearbyMosque?>(null) }

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

    fun loadMosquesFor(lat: Double, lon: Double, radius: Int) {
        scope.launch {
            screenState = MosqueScreenState.Loading
            when (val res = repository.findNearbyMosques(lat, lon, radius)) {
                is MosqueSearchResult.Offline -> screenState = MosqueScreenState.Offline
                is MosqueSearchResult.Success -> screenState = MosqueScreenState.Loaded(res.mosques)
                is MosqueSearchResult.Error -> screenState = MosqueScreenState.Error(
                    if (isEn) res.messageEn else res.messageBn
                )
            }
        }
    }

    fun detectGpsAndLoad() {
        if (!hasLocationPermission()) {
            screenState = MosqueScreenState.NeedsPermission
            return
        }
        if (!isLocationEnabled()) {
            screenState = MosqueScreenState.LocationOff
            return
        }
        if (!repository.isOnline()) {
            screenState = MosqueScreenState.Offline
            return
        }
        screenState = MosqueScreenState.Loading
        LocationHelper.fetchCurrentGpsLocation(
            context = context,
            scope = scope,
            isEnglish = isEn,
            showToast = false,
            onResolved = { lat, lon, label, _ ->
                userCoords = lat to lon
                userPlaceLabel = label
                loadMosquesFor(lat, lon, radiusMeters)
            },
            onError = { errMsg ->
                screenState = MosqueScreenState.Error(errMsg)
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
            screenState = MosqueScreenState.NeedsPermission
        }
    }

    LaunchedEffect(Unit) {
        detectGpsAndLoad()
    }

    // Auto-refresh when returning from system Location settings
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
                onClick = { detectGpsAndLoad() },
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

        // Search & Radius Filter Row
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

        Spacer(modifier = Modifier.height(8.dp))

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
                            loadMosquesFor(coords.first, coords.second, r)
                        } else {
                            detectGpsAndLoad()
                        }
                    },
                    label = {
                        Text("${BanglaUtils.formatNumber(km, isEn)} ${tr(isEn, "কি.মি.", "km")}")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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

            is MosqueScreenState.Offline -> {
                MosqueStatusCard(
                    icon = Icons.Default.WifiOff,
                    title = tr(isEn, "ইন্টারনেট সংযোগ প্রয়োজন", "Internet Required"),
                    body = tr(
                        isEn,
                        "নিকটবর্তী মসজিদের তালিকা অনলাইন প্লেস সার্চ (OpenStreetMap) থেকে লোড হয়। ইন্টারনেট চালু করে পুনরায় চেষ্টা করুন।",
                        "Nearby mosque discovery requires an internet connection (OpenStreetMap Places search). Connect to the internet and retry."
                    ),
                    primaryButtonLabel = tr(isEn, "পুনরায় চেষ্টা করুন", "Retry"),
                    onPrimaryClick = { detectGpsAndLoad() },
                    buttonTag = "mosque_offline_retry"
                )
            }

            is MosqueScreenState.Loading -> {
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
                            text = tr(isEn, "কাছাকাছি মসজিদ খোঁজা হচ্ছে...", "Searching for nearby mosques..."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is MosqueScreenState.Error -> {
                MosqueStatusCard(
                    icon = Icons.Default.Mosque,
                    title = tr(isEn, "মসজিদ লোড করা যায়নি", "Unable to Load Mosques"),
                    body = state.message,
                    primaryButtonLabel = tr(isEn, "আবার চেষ্টা করুন", "Try Again"),
                    onPrimaryClick = { detectGpsAndLoad() },
                    buttonTag = "mosque_error_retry"
                )
            }

            is MosqueScreenState.Loaded -> {
                val filtered = remember(state.mosques, searchQuery) {
                    MosqueRepository.filterMosques(state.mosques, searchQuery)
                }
                if (filtered.isEmpty()) {
                    MosqueStatusCard(
                        icon = Icons.Default.Mosque,
                        title = tr(isEn, "কোনো মসজিদ পাওয়া যায়নি", "No Mosques Found"),
                        body = if (searchQuery.isNotBlank()) {
                            tr(isEn, "আপনার অনুসন্ধানের সাথে মিলে এমন কোনো মসজিদ পাওয়া যায়নি।", "No mosques matched your search query.")
                        } else {
                            tr(
                                isEn,
                                "এই দূরত্বের মধ্যে কোনো মসজিদ পাওয়া যায়নি। দূরত্ব বাড়িয়ে (যেমন ১০ কি.মি.) চেষ্টা করুন।",
                                "No mosques found within this radius. Try expanding the search radius."
                            )
                        },
                        primaryButtonLabel = tr(isEn, "পুনরায় খুঁজুন", "Search Again"),
                        onPrimaryClick = { detectGpsAndLoad() },
                        buttonTag = "mosque_empty_retry"
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
                                    "${BanglaUtils.formatNumber(filtered.size, false)}টি মসজিদ পাওয়া গেছে (দূরত্ব অনুযায়ী সাজানো)",
                                    "${filtered.size} mosques found (sorted by distance)"
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
                                onOpenMaps = { openMosqueInGoogleMaps(context, mosque, isEn, onStatusMessage) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    // Mosque Detail Sheet with Qibla direction from that mosque + Open in Google Maps
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

                Button(
                    onClick = {
                        openMosqueInGoogleMaps(context, mosque, isEn, onStatusMessage)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mosque_open_maps_button")
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr(isEn, "গুগল ম্যাপে দিকনির্দেশনা দেখুন", "Open in Google Maps"))
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
                Icon(Icons.Default.Directions, contentDescription = tr(isEnglish, "ম্যাপ", "Maps"), modifier = Modifier.size(16.dp))
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

private fun openMosqueInGoogleMaps(
    context: Context,
    mosque: NearbyMosque,
    isEnglish: Boolean,
    onStatusMessage: (String) -> Unit
) {
    val label = Uri.encode(mosque.displayName(isEnglish))
    val geoUri = Uri.parse("geo:${mosque.latitude},${mosque.longitude}?q=${mosque.latitude},${mosque.longitude}($label)")
    val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        if (mapsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapsIntent)
        } else {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${mosque.latitude},${mosque.longitude}")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    } catch (_: Exception) {
        onStatusMessage(
            tr(isEnglish, "গুগল ম্যাপ খোলা যায়নি", "Could not open Google Maps")
        )
    }
}
