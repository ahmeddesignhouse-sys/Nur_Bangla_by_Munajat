package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.example.R
import com.example.alarm.AdhanAlarmScheduler
import com.example.data.local.LocalAppSettings
import com.example.data.local.PreferencesDataStore
import com.example.data.repository.NoorRepository
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.IslamicToolsScreen
import com.example.ui.screens.PrayerTrackerScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.QuranSearchScreen
import com.example.ui.theme.NoorBanglaTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private enum class NoorRoute(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Filled.AccessTimeFilled, Icons.Outlined.AccessTime),
    QURAN("quran", R.string.nav_quran, Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    TOOLS("tools", R.string.nav_tools, Icons.Filled.Explore, Icons.Outlined.Explore),
    TRACKER("tracker", R.string.nav_tracker, Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    PROFILE("profile", R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
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
    val prefsStore = remember(context) { PreferencesDataStore(context) }
    val themeSettings by prefsStore.settingsFlow.collectAsStateWithLifecycle(initialValue = LocalAppSettings())

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    NoorBanglaTheme(
        themeMode = themeSettings.themeMode,
        accentTheme = themeSettings.accentTheme
    ) {
        val user = currentUser
        if (user == null) {
            AuthScreen(onAuthSuccess = {})
        } else {
            key(user.uid) {
                AuthenticatedNoorContent(currentUserId = user.uid)
            }
        }
    }
}

@Composable
private fun AuthenticatedNoorContent(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: NoorViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val repository = NoorRepository(app)
                NoorViewModel(repository = repository, currentUserId = currentUserId)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Schedule alarms off the main thread so first frame surface sync is never blocked
    LaunchedEffect(currentUserId, uiState.settings) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            AdhanAlarmScheduler.ensureNotificationChannel(context)
            AdhanAlarmScheduler.scheduleUpcomingAlarms(context, uiState.settings)
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NoorRoute.HOME.route

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
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
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
                            val selected = currentRoute == tab.route
                            val label = stringResource(tab.labelRes)
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
                                onStatusMessage = viewModel::showMessage
                            )
                        }

                        composable(NoorRoute.QURAN.route) {
                            QuranSearchScreen(
                                uiState = uiState,
                                onSearchChanged = viewModel::updateGlobalSearch,
                                onToggleBookmark = viewModel::toggleBookmark,
                                onQuranDarkModeChanged = viewModel::setQuranDarkMode,
                                onQuranFontSizeChanged = viewModel::setQuranFontSize,
                                onNavigateToTrackerDate = { dateKey ->
                                    viewModel.selectTrackerDate(dateKey)
                                    navigateToTab(NoorRoute.TRACKER.route)
                                }
                            )
                        }

                        composable(NoorRoute.TOOLS.route) {
                            IslamicToolsScreen(
                                uiState = uiState,
                                currentDhikr = viewModel.currentDhikr(),
                                onIncrementTasbih = viewModel::incrementTasbih,
                                onResetTasbih = viewModel::resetTasbih,
                                onSelectDhikr = viewModel::selectTasbihDhikr,
                                onSetTasbihTarget = viewModel::setTasbihTarget
                            )
                        }

                        composable(NoorRoute.TRACKER.route) {
                            PrayerTrackerScreen(
                                uiState = uiState,
                                onSelectDate = viewModel::selectTrackerDate,
                                onUpdatePrayerStatus = viewModel::updatePrayerStatus,
                                onUpdatePrayerNote = viewModel::updatePrayerNote
                            )
                        }

                        composable(NoorRoute.PROFILE.route) {
                            ProfileSettingsScreen(
                                uiState = uiState,
                                onThemeModeChanged = viewModel::setThemeMode,
                                onAccentThemeChanged = viewModel::setAccentTheme,
                                onWaqtAlertModeChanged = { waqt, mode ->
                                    viewModel.setWaqtAlertMode(waqt, mode) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onPrePrayerMinutesChanged = { mins ->
                                    viewModel.setPrePrayerMinutes(mins) { updated ->
                                        AdhanAlarmScheduler.scheduleUpcomingAlarms(context, updated)
                                    }
                                },
                                onSignOutClick = {
                                    performSignOut(credentialManager, scope)
                                },
                                onStatusMessage = viewModel::showMessage
                            )
                        }
                    }
                }
            }
        }
    }
}
