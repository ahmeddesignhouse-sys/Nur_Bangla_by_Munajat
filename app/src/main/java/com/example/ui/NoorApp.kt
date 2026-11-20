package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.alarm.AdhanAlarmScheduler
import com.example.data.local.LocalAppSettings
import com.example.data.local.PreferencesDataStore
import com.example.data.repository.NoorRepository
import com.example.ui.screens.AsmaulHusnaScreen
import com.example.ui.screens.DailyInspirationScreen
import com.example.ui.screens.DuaAzkarScreen
import com.example.ui.screens.HajjUmrahGuideScreen
import com.example.ui.screens.HalalFinderScreen
import com.example.ui.screens.HijriCalendarScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.IslamicToolsScreen
import com.example.ui.screens.MosqueFinderScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.QuranSearchScreen
import com.example.ui.screens.RamadanCalendarScreen
import com.example.ui.screens.ZakatCalculatorScreen
import com.example.ui.theme.NoorBanglaTheme
import com.example.util.AppUpdateInfo
import com.example.util.LocalAppLanguage
import com.example.util.UpdateCheckResult
import com.example.util.UpdateChecker
import com.example.util.UpdateDialog
import com.example.util.UpdateDialogState
import com.example.util.isEnglishLang
import com.example.util.tr
import com.example.widget.PrayerTimesWidgetProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.Locale

private const val ROUTE_HIJRI_CALENDAR = "hijri_calendar"
private const val ROUTE_DUA_AZKAR = "dua_azkar"
private const val ROUTE_ASMAUL_HUSNA = "asmaul_husna"
private const val ROUTE_RAMADAN_CALENDAR = "ramadan_calendar"
private const val ROUTE_ZAKAT_CALCULATOR = "zakat_calculator"
private const val ROUTE_MOSQUE_FINDER = "mosque_finder"
private const val ROUTE_DAILY_INSPIRATION = "daily_inspiration"
private const val ROUTE_HAJJ_UMRAH_GUIDE = "hajj_umrah_guide"
private const val ROUTE_HALAL_FINDER = "halal_finder"

private enum class NoorRoute(
    val route: String,
    val labelBn: String,
    val labelEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "সময়সূচী", "Prayer", Icons.Filled.AccessTimeFilled, Icons.Outlined.AccessTime),
    QURAN("quran", "কুরআন", "Quran", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    TOOLS("tools", "টুলস", "Tools", Icons.Filled.Explore, Icons.Outlined.Explore),
    PROFILE("profile", "সেটিংস", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings);

    fun label(isEnglish: Boolean): String = if (isEnglish) labelEn else labelBn
}

sealed class AppSessionState {
    data object Unauthenticated : AppSessionState()
    data class Guest(val guestUserId: String) : AppSessionState()
    data class GuestPendingMigration(val guestUserId: String, val signedInUserId: String) : AppSessionState()
    data class SignedIn(val userId: String) : AppSessionState()
}

/**
 * Pure session resolver determining whether the app shows AuthScreen, Guest Mode,
 * Guest -> Cloud migration prompt, or Signed-In Mode.
 */
fun resolveAppSession(
    firebaseUid: String?,
    isGuestMode: Boolean,
    guestUserId: String,
    isSigningOut: Boolean = false
): AppSessionState {
    if (isSigningOut && guestUserId.isNotBlank()) {
        return AppSessionState.Guest(guestUserId)
    }
    if (!firebaseUid.isNullOrBlank()) {
        return if (isGuestMode && guestUserId.isNotBlank()) {
            AppSessionState.GuestPendingMigration(
                guestUserId = guestUserId,
                signedInUserId = firebaseUid
            )
        } else {
            AppSessionState.SignedIn(firebaseUid)
        }
    }
    if (isGuestMode && guestUserId.isNotBlank()) {
        return AppSessionState.Guest(guestUserId)
    }
    return AppSessionState.Unauthenticated
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun NoorApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsStore = remember(context) { PreferencesDataStore(context) }
    val loadedSettings by prefsStore.settingsFlow.collectAsStateWithLifecycle(initialValue = null)
    val appSettings = loadedSettings ?: LocalAppSettings()

    LaunchedEffect(appSettings.languageCode) {
        val locale = if (appSettings.languageCode == "en") Locale.ENGLISH else Locale.forLanguageTag("bn-BD")
        Locale.setDefault(locale)
    }

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var isSigningOut by remember { mutableStateOf(false) }
    var isUploadingGuestData by remember { mutableStateOf(false) }
    var transitionMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            isSigningOut = false
        }
    }

    CompositionLocalProvider(LocalAppLanguage provides appSettings.languageCode) {
        NoorBanglaTheme(
            themeMode = appSettings.themeMode,
            accentTheme = appSettings.accentTheme
        ) {
            if (loadedSettings == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            } else {
                val isEn = isEnglishLang(appSettings.languageCode)
                val session = resolveAppSession(
                    firebaseUid = currentUser?.uid,
                    isGuestMode = appSettings.isGuestMode,
                    guestUserId = appSettings.guestUserId,
                    isSigningOut = isSigningOut
                )

                when (session) {
                    is AppSessionState.Unauthenticated -> {
                        AuthScreen(
                            onAuthSuccess = {
                                // AuthStateListener automatically transitions to SignedIn or GuestPendingMigration
                            },
                            onContinueAsGuest = {
                                scope.launch {
                                    prefsStore.enableGuestMode()
                                }
                            },
                            onLanguageSelected = { code ->
                                scope.launch { prefsStore.setLanguageCode(code) }
                            }
                        )
                    }

                    is AppSessionState.Guest -> {
                        key(session.guestUserId) {
                            MainNoorContent(
                                currentUserId = session.guestUserId,
                                isGuest = true,
                                enableAutoUpdateCheck = true,
                                initialBannerMessage = transitionMessage,
                                onConsumeInitialBannerMessage = { transitionMessage = null },
                                onGuestGoogleSignInSuccess = {
                                    // AuthStateListener detects firebaseUid while isGuestMode == true
                                    // and transitions deterministically to GuestPendingMigration.
                                },
                                onSignOutToGuest = {}
                            )
                        }
                    }

                    is AppSessionState.GuestPendingMigration -> {
                        key(session.guestUserId) {
                            MainNoorContent(
                                currentUserId = session.guestUserId,
                                isGuest = true,
                                enableAutoUpdateCheck = false,
                                initialBannerMessage = transitionMessage,
                                onConsumeInitialBannerMessage = { transitionMessage = null },
                                onGuestGoogleSignInSuccess = {},
                                onSignOutToGuest = {}
                            )
                        }

                        AlertDialog(
                            onDismissRequest = {
                                if (!isUploadingGuestData) {
                                    scope.launch {
                                        val repo = NoorRepository(context)
                                        repo.migrateLocalBookmarksToUser(session.guestUserId, session.signedInUserId)
                                        prefsStore.setGuestModeEnabled(false)
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            title = {
                                Text(
                                    text = tr(
                                        isEn,
                                        "লোকাল ডেটা ক্লাউডে ব্যাকআপ করবেন?",
                                        "Upload Local Data to Cloud?"
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = tr(
                                        isEn,
                                        "অতিথি মোডে সংরক্ষিত আপনার কুরআন বুকমার্ক ও সেটিংস গুগল অ্যাকাউন্টে যুক্ত (মার্জ) করে ক্লাউডে আপলোড করতে চান? আপনার লোকাল কোনো ডেটা মুছে যাবে না।",
                                        "Would you like to merge and upload your local Guest Mode bookmarks and settings to your Google account? Your local data will not be deleted."
                                    )
                                )
                            },
                            confirmButton = {
                                Button(
                                    enabled = !isUploadingGuestData,
                                    onClick = {
                                        isUploadingGuestData = true
                                        scope.launch {
                                            val repo = NoorRepository(context)
                                            val res = repo.uploadLocalGuestDataToCloud(
                                                guestUserId = session.guestUserId,
                                                signedInUserId = session.signedInUserId,
                                                settings = appSettings
                                            )
                                            prefsStore.setGuestModeEnabled(false)
                                            transitionMessage = if (res.isSuccess) {
                                                tr(
                                                    isEn,
                                                    "গুগল অ্যাকাউন্ট যুক্ত হয়েছে এবং লোকাল ডেটা ক্লাউডে ব্যাকআপ করা হয়েছে",
                                                    "Signed in with Google & local data backed up to cloud"
                                                )
                                            } else {
                                                tr(
                                                    isEn,
                                                    "গুগল অ্যাকাউন্ট যুক্ত হয়েছে (লোকাল ডেটা সুরক্ষিত আছে)",
                                                    "Signed in with Google (local data preserved)"
                                                )
                                            }
                                            isUploadingGuestData = false
                                        }
                                    },
                                    modifier = Modifier.testTag("guest_migrate_upload_button")
                                ) {
                                    if (isUploadingGuestData) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                    }
                                    Text(tr(isEn, "আপলোড ও মার্জ করুন", "Upload & Merge"))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    enabled = !isUploadingGuestData,
                                    onClick = {
                                        scope.launch {
                                            val repo = NoorRepository(context)
                                            repo.migrateLocalBookmarksToUser(session.guestUserId, session.signedInUserId)
                                            prefsStore.setGuestModeEnabled(false)
                                            transitionMessage = tr(
                                                isEn,
                                                "গুগল অ্যাকাউন্টে সাইন ইন সম্পন্ন হয়েছে (লোকাল ডেটা সংরক্ষিত)",
                                                "Signed in with Google (local data kept)"
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("guest_migrate_skip_button")
                                ) {
                                    Text(tr(isEn, "আপলোড ছাড়াই চালু করুন", "Keep Local & Switch"))
                                }
                            },
                            modifier = Modifier.testTag("guest_migration_dialog")
                        )
                    }

                    is AppSessionState.SignedIn -> {
                        key(session.userId) {
                            MainNoorContent(
                                currentUserId = session.userId,
                                isGuest = false,
                                enableAutoUpdateCheck = true,
                                initialBannerMessage = transitionMessage,
                                onConsumeInitialBannerMessage = { transitionMessage = null },
                                onGuestGoogleSignInSuccess = {},
                                onSignOutToGuest = { credentialManager ->
                                    isSigningOut = true
                                    scope.launch {
                                        val repo = NoorRepository(context)
                                        val guestId = prefsStore.enableGuestMode()
                                        repo.migrateLocalBookmarksToUser(session.userId, guestId)
                                        transitionMessage = tr(
                                            isEn,
                                            "সাইন আউট সম্পন্ন — অতিথি মোডে চালু আছে (লোকাল ডেটা সুরক্ষিত)",
                                            "Signed out — continuing in Guest Mode (local data kept)"
                                        )
                                        performSignOut(credentialManager, scope)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainNoorContent(
    currentUserId: String,
    isGuest: Boolean,
    enableAutoUpdateCheck: Boolean,
    initialBannerMessage: String?,
    onConsumeInitialBannerMessage: () -> Unit,
    onGuestGoogleSignInSuccess: (String) -> Unit,
    onSignOutToGuest: (CredentialManager) -> Unit
) {
    val context = LocalContext.current
    val isEn = isEnglishLang(LocalAppLanguage.current)
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var updateDialogState by remember { mutableStateOf<UpdateDialogState>(UpdateDialogState.Hidden) }
    var isCheckingForUpdate by remember { mutableStateOf(false) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var hasAutoCheckedUpdate by rememberSaveable(currentUserId) { mutableStateOf(false) }

    val viewModel: NoorViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val repository = NoorRepository(app)
                NoorViewModel(
                    appContext = app,
                    repository = repository,
                    currentUserId = currentUserId,
                    isGuest = isGuest
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    fun runUpdateCheck(manual: Boolean) {
        if (isCheckingForUpdate) return
        isCheckingForUpdate = true
        coroutineScope.launch {
            val result = UpdateChecker.checkForUpdate(context)
            isCheckingForUpdate = false
            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    updateDialogState = UpdateDialogState.Available(result.info)
                }
                is UpdateCheckResult.UpToDate -> {
                    if (manual) {
                        viewModel.showMessage(
                            tr(
                                isEn,
                                "আপনি সর্বশেষ ভার্সন ব্যবহার করছেন",
                                "You are on the latest version"
                            )
                        )
                    }
                }
                is UpdateCheckResult.NoInternet -> {
                    Toast.makeText(
                        context,
                        tr(
                            isEn,
                            "ইন্টারনেট সংযোগ নেই, আপডেট যাচাই করা যায়নি",
                            "No internet, could not check for updates"
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is UpdateCheckResult.Error -> {
                    if (manual) {
                        viewModel.showMessage(
                            tr(
                                isEn,
                                "আপডেট যাচাই করা যায়নি (${result.message})",
                                "Could not check for updates (${result.message})"
                            )
                        )
                    }
                }
            }
        }
    }

    fun startApkDownload(info: AppUpdateInfo) {
        downloadJob?.cancel()
        updateDialogState = UpdateDialogState.Downloading(
            info = info,
            progressPercent = 0,
            downloadedBytes = 0L,
            totalBytes = -1L
        )
        downloadJob = coroutineScope.launch {
            val res = UpdateChecker.downloadApk(
                context = context,
                apkUrl = info.apkUrl,
                versionName = info.latestVersionName
            ) { pct, downloaded, total ->
                updateDialogState = UpdateDialogState.Downloading(
                    info = info,
                    progressPercent = pct,
                    downloadedBytes = downloaded,
                    totalBytes = total
                )
            }
            res.onSuccess { apkFile ->
                if (UpdateChecker.canInstallPackages(context)) {
                    val launched = UpdateChecker.launchApkInstaller(context, apkFile)
                    if (launched) {
                        updateDialogState = if (info.forceUpdate) {
                            UpdateDialogState.Available(info)
                        } else {
                            UpdateDialogState.Hidden
                        }
                    } else {
                        updateDialogState = UpdateDialogState.DownloadFailed(
                            info = info,
                            errorMessage = tr(isEn, "ইনস্টলার খোলা যায়নি", "Could not open installer")
                        )
                    }
                } else {
                    updateDialogState = UpdateDialogState.NeedsInstallPermission(info, apkFile)
                }
            }.onFailure { err ->
                updateDialogState = UpdateDialogState.DownloadFailed(
                    info = info,
                    errorMessage = err.localizedMessage ?: "Network error"
                )
            }
        }
    }

    LaunchedEffect(initialBannerMessage) {
        if (initialBannerMessage != null) {
            snackbarHostState.showSnackbar(initialBannerMessage)
            onConsumeInitialBannerMessage()
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(currentUserId, uiState.settings) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            AdhanAlarmScheduler.ensureNotificationChannel(context)
            AdhanAlarmScheduler.scheduleUpcomingAlarms(context, uiState.settings)
            PrayerTimesWidgetProvider.requestWidgetUpdate(context, uiState.settings)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NoorRoute.HOME.route

    // Auto-check for updates on app launch ONLY when on the Home screen (never on AuthScreen)
    LaunchedEffect(enableAutoUpdateCheck, currentRoute) {
        if (enableAutoUpdateCheck && !hasAutoCheckedUpdate && currentRoute == NoorRoute.HOME.route) {
            hasAutoCheckedUpdate = true
            runUpdateCheck(manual = false)
        }
    }

    val activeTabRoute = when (currentRoute) {
        ROUTE_DAILY_INSPIRATION -> NoorRoute.HOME.route
        ROUTE_HIJRI_CALENDAR,
        ROUTE_DUA_AZKAR,
        ROUTE_ASMAUL_HUSNA,
        ROUTE_RAMADAN_CALENDAR,
        ROUTE_ZAKAT_CALCULATOR,
        ROUTE_MOSQUE_FINDER,
        ROUTE_HAJJ_UMRAH_GUIDE,
        ROUTE_HALAL_FINDER -> NoorRoute.TOOLS.route
        else -> currentRoute
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        NoorRoute.entries.forEach { tab ->
                            val selected = activeTabRoute == tab.route
                            val label = tab.label(isEn)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail {
                        NoorRoute.entries.forEach { tab ->
                            val selected = activeTabRoute == tab.route
                            val label = tab.label(isEn)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = NoorRoute.HOME.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                    ) {
                        composable(NoorRoute.HOME.route) {
                            HomeDashboardScreen(
                                uiState = uiState,
                                onSelectDistrict = { district ->
                                    viewModel.selectDistrict(district) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onGpsResolved = { lat, lon, placeLabel, showToast ->
                                    viewModel.updateGpsCoordinates(lat, lon, placeLabel, showToast) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onTimezoneChanged = {
                                    viewModel.refreshForTimezoneChange { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onAsrMethodChanged = { method ->
                                    viewModel.setAsrMethod(method) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onCalculationMethodChanged = { method ->
                                    viewModel.setCalculationMethod(method) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onWaqtAlertModeChanged = { waqt, mode ->
                                    viewModel.setWaqtAlertMode(waqt, mode) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onThemeModeChanged = viewModel::setThemeMode,
                                onAccentThemeChanged = viewModel::setAccentTheme,
                                onOpenHijriCalendar = {
                                    navController.navigate(ROUTE_HIJRI_CALENDAR) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenRamadanCalendar = {
                                    navController.navigate(ROUTE_RAMADAN_CALENDAR) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenDailyInspiration = {
                                    navController.navigate(ROUTE_DAILY_INSPIRATION) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenQuranTab = {
                                    navigateToTab(NoorRoute.QURAN.route)
                                },
                                onRamadanModeChanged = viewModel::setRamadanMode,
                                onStatusMessage = viewModel::showMessage
                            )
                        }

                        composable(ROUTE_DAILY_INSPIRATION) {
                            DailyInspirationScreen(
                                entries = uiState.recentInspirations,
                                onStatusMessage = viewModel::showMessage,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NoorRoute.QURAN.route) {
                            QuranSearchScreen(
                                uiState = uiState,
                                onSearchChanged = viewModel::updateGlobalSearch,
                                onToggleBookmark = viewModel::toggleBookmark,
                                onQuranDarkModeChanged = viewModel::setQuranDarkMode,
                                onQuranFontSizeChanged = viewModel::setQuranFontSize,
                                onPlaySurahAudio = viewModel::playSurahAudio,
                                onPlayAyahAudio = viewModel::playAyahAudio,
                                onToggleAudioPlayPause = viewModel::toggleQuranAudioPlayPause,
                                onNextAudioAyah = viewModel::nextQuranAudioAyah,
                                onPreviousAudioAyah = viewModel::previousQuranAudioAyah,
                                onStopAudio = viewModel::stopQuranAudio,
                                onToggleAyahReadToday = viewModel::toggleAyahReadToday
                            )
                        }

                        composable(NoorRoute.TOOLS.route) {
                            IslamicToolsScreen(
                                uiState = uiState,
                                currentDhikr = viewModel.currentDhikr(),
                                onIncrementTasbih = viewModel::incrementTasbih,
                                onResetTasbih = viewModel::resetTasbih,
                                onSelectDhikr = viewModel::selectTasbihDhikr,
                                onSetTasbihTarget = viewModel::setTasbihTarget,
                                onOpenHijriCalendar = {
                                    navController.navigate(ROUTE_HIJRI_CALENDAR) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenDuaAzkar = {
                                    navController.navigate(ROUTE_DUA_AZKAR) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenAsmaulHusna = {
                                    navController.navigate(ROUTE_ASMAUL_HUSNA) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenRamadanCalendar = {
                                    navController.navigate(ROUTE_RAMADAN_CALENDAR) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenZakatCalculator = {
                                    navController.navigate(ROUTE_ZAKAT_CALCULATOR) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenMosqueFinder = {
                                    navController.navigate(ROUTE_MOSQUE_FINDER) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenHajjUmrahGuide = {
                                    navController.navigate(ROUTE_HAJJ_UMRAH_GUIDE) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenHalalFinder = {
                                    navController.navigate(ROUTE_HALAL_FINDER) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(ROUTE_HIJRI_CALENDAR) {
                            HijriCalendarScreen(
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onHijriOffsetChanged = viewModel::setHijriDayOffset
                            )
                        }

                        composable(ROUTE_DUA_AZKAR) {
                            DuaAzkarScreen(
                                favoriteDuaIds = uiState.settings.favoriteDuaIds,
                                onToggleFavorite = viewModel::toggleFavoriteDua,
                                onStatusMessage = viewModel::showMessage,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_ASMAUL_HUSNA) {
                            AsmaulHusnaScreen(
                                onStatusMessage = viewModel::showMessage,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_RAMADAN_CALENDAR) {
                            RamadanCalendarScreen(
                                uiState = uiState,
                                onRamadanModeChanged = viewModel::setRamadanMode,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_ZAKAT_CALCULATOR) {
                            ZakatCalculatorScreen(
                                uiState = uiState,
                                onSaveMetalPrices = viewModel::setMetalPrices,
                                onSaveHistory = viewModel::saveZakatCalculation,
                                onDeleteHistory = viewModel::deleteZakatCalculation,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_MOSQUE_FINDER) {
                            MosqueFinderScreen(
                                onStatusMessage = viewModel::showMessage,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_HAJJ_UMRAH_GUIDE) {
                            HajjUmrahGuideScreen(
                                completedStepIds = uiState.settings.hajjCompletedStepIds,
                                onToggleStepCompleted = viewModel::toggleHajjStepCompleted,
                                onResetChecklist = viewModel::resetHajjChecklist,
                                onStatusMessage = viewModel::showMessage,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_HALAL_FINDER) {
                            HalalFinderScreen(
                                onStatusMessage = viewModel::showMessage,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NoorRoute.PROFILE.route) {
                            ProfileSettingsScreen(
                                uiState = uiState,
                                onLanguageChanged = { code ->
                                    viewModel.setLanguageCode(code) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onHijriDayOffsetChanged = viewModel::setHijriDayOffset,
                                onQuranReciterChanged = viewModel::setQuranReciter,
                                onDailyQuranGoalChanged = viewModel::setDailyQuranGoal,
                                onUseGpsChanged = { enabled ->
                                    viewModel.setUseGps(enabled) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onGpsResolved = { lat, lon, placeLabel, showToast ->
                                    viewModel.updateGpsCoordinates(lat, lon, placeLabel, showToast) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onSelectDistrict = { district ->
                                    viewModel.selectDistrict(district) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onCalculationMethodChanged = { method ->
                                    viewModel.setCalculationMethod(method) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onAsrMethodChanged = { method ->
                                    viewModel.setAsrMethod(method) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onThemeModeChanged = viewModel::setThemeMode,
                                onAccentThemeChanged = viewModel::setAccentTheme,
                                onWaqtEnabledChanged = { waqt, enabled ->
                                    viewModel.setWaqtEnabled(waqt, enabled) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onWaqtAdhanVoiceChanged = { waqt, voice ->
                                    viewModel.setWaqtAdhanVoice(waqt, voice) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onWaqtReminderMinutesChanged = { waqt, mins ->
                                    viewModel.setWaqtReminderMinutes(waqt, mins) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onGuestGoogleSignInSuccess = onGuestGoogleSignInSuccess,
                                onSignOutClick = {
                                    onSignOutToGuest(credentialManager)
                                },
                                onStatusMessage = viewModel::showMessage,
                                isCheckingForUpdate = isCheckingForUpdate,
                                onCheckForUpdates = { runUpdateCheck(manual = true) }
                            )
                        }
                    }
                }
            }
        }

        UpdateDialog(
            state = updateDialogState,
            onStartDownload = { info -> startApkDownload(info) },
            onCancelDownload = { info ->
                downloadJob?.cancel()
                downloadJob = null
                updateDialogState = if (info.forceUpdate) {
                    UpdateDialogState.Available(info)
                } else {
                    UpdateDialogState.Hidden
                }
            },
            onDismiss = { updateDialogState = UpdateDialogState.Hidden },
            onOpenInstallPermissionSettings = {
                UpdateChecker.openUnknownSourcesSettings(context)
            },
            onRetryInstall = { info, apkFile ->
                if (UpdateChecker.canInstallPackages(context)) {
                    val launched = UpdateChecker.launchApkInstaller(context, apkFile)
                    if (launched) {
                        updateDialogState = if (info.forceUpdate) {
                            UpdateDialogState.Available(info)
                        } else {
                            UpdateDialogState.Hidden
                        }
                    }
                }
            }
        )
    }
}
