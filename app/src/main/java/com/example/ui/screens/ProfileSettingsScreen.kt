package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.compose.material3.LinearProgressIndicator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.alarm.AdhanAlarmReceiver
import com.example.alarm.AdhanPlaybackService
import com.example.data.model.AccentTheme
import com.example.data.model.AdhanVoice
import com.example.data.model.AsrMethod
import com.example.data.model.BdDistrict
import com.example.data.model.CalculationMethod
import com.example.data.model.ThemeMode
import com.example.data.model.WaqtType
import com.example.ui.NoorUiState
import com.example.ui.onGoogleSignInClicked
import com.example.ui.components.DistrictPickerSheet
import com.example.ui.theme.StatusOnTimeColor
import com.example.util.BanglaUtils
import com.example.util.LocalAppLanguage
import com.example.util.LocationHelper
import com.example.util.QuranReciter
import com.example.util.isEnglishLang
import com.example.util.tr
import com.example.widget.PrayerTimesWidgetProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun ProfileSettingsScreen(
    uiState: NoorUiState,
    onLanguageChanged: (String) -> Unit,
    onHijriDayOffsetChanged: (Int) -> Unit,
    onQuranReciterChanged: (QuranReciter) -> Unit,
    onDailyQuranGoalChanged: (Int) -> Unit,
    onUseGpsChanged: (Boolean) -> Unit,
    onGpsResolved: (Double, Double, String?, Boolean) -> Unit,
    onSelectDistrict: (BdDistrict) -> Unit,
    onCalculationMethodChanged: (CalculationMethod) -> Unit,
    onAsrMethodChanged: (AsrMethod) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAccentThemeChanged: (AccentTheme) -> Unit,
    onWaqtEnabledChanged: (WaqtType, Boolean) -> Unit,
    onWaqtAdhanVoiceChanged: (WaqtType, AdhanVoice) -> Unit,
    onWaqtReminderMinutesChanged: (WaqtType, Int) -> Unit,
    onGuestGoogleSignInSuccess: (String) -> Unit,
    onSignOutClick: () -> Unit,
    onStatusMessage: (String) -> Unit,
    isCheckingForUpdate: Boolean = false,
    onCheckForUpdates: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val isEn = isEnglishLang(LocalAppLanguage.current)
    val currentUser = if (uiState.isGuest) null else Firebase.auth.currentUser
    val userName = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: tr(isEn, "মুসলিম ব্যবহারকারী", "Muslim User")
    val userEmail = currentUser?.email ?: ""

    var isSigningIn by remember { mutableStateOf(false) }
    var showDistrictSheet by remember { mutableStateOf(false) }
    var showWidgetGuideDialog by remember { mutableStateOf(false) }
    val adhanPlayback by AdhanPlaybackService.playbackState.collectAsStateWithLifecycle()

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        onStatusMessage(
            if (granted) tr(isEn, "আযান নোটিফিকেশন অনুমতি চালু হয়েছে", "Adhan notification permission enabled")
            else tr(isEn, "নোটিফিকেশন অনুমতি ছাড়া আযান অ্যালার্ট দেখানো যাবে না", "Notification permission is required for Adhan alerts")
        )
    }

    val fetchGpsNow: () -> Unit = {
        LocationHelper.fetchCurrentGpsLocation(
            context = context,
            scope = scope,
            isEnglish = isEn,
            showToast = true,
            onResolved = onGpsResolved,
            onError = onStatusMessage
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchGpsNow()
        } else {
            onUseGpsChanged(false)
            onStatusMessage(
                tr(
                    isEn,
                    "লোকেশন অনুমতি দেওয়া হয়নি; ম্যানুয়াল জেলা ব্যবহার করা হচ্ছে।",
                    "Location permission denied; using manual district."
                )
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User Profile / Guest Mode Banner Card
        item {
            if (uiState.isGuest) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guest_mode_banner_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(isEn, "অতিথি মোড (লোকাল সংরক্ষণ)", "Guest Mode (Local Storage)"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        isEn,
                                        "ক্লাউড সিঙ্ক বন্ধ • সকল তথ্য আপনার ফোনেই সংরক্ষিত",
                                        "Cloud Sync Off • All data saved locally on device"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = tr(
                                isEn,
                                "আপনি অতিথি মোডে ব্যবহার করছেন — আপনার বুকমার্ক ও সেটিংস ক্লাউডে ব্যাকআপ রাখতে গুগল দিয়ে সাইন ইন করুন।",
                                "You are using Guest Mode — sign in with Google to back up your bookmarks & settings to the cloud."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Button(
                            onClick = {
                                isSigningIn = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    isEnglish = isEn,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        val uid = Firebase.auth.currentUser?.uid
                                        if (!uid.isNullOrBlank()) {
                                            onGuestGoogleSignInSuccess(uid)
                                        }
                                    },
                                    onAuthError = { msg ->
                                        isSigningIn = false
                                        onStatusMessage(msg)
                                    },
                                    onAuthCancelled = {
                                        isSigningIn = false
                                    },
                                    scope = scope
                                )
                            },
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_google_sign_in_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(tr(isEn, "গুগল দিয়ে সাইন ইন করুন", "Sign in with Google"))
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column {
                                Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (userEmail.isNotBlank()) {
                                    Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = tr(isEn, "গুগল ক্লাউড সিঙ্ক যুক্ত", "Google Cloud Sync Connected"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusOnTimeColor
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOutClick,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(tr(isEn, "সাইন আউট", "Sign Out"))
                        }
                    }
                }
            }
        }

        // Language Setting Card (Immediate Bangla / English switch)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                text = tr(isEn, "ভাষা (Language)", "Language (ভাষা)"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    isEn,
                                    "রিস্টার্ট ছাড়াই সম্পূর্ণ অ্যাপ ও কুরআন অনুবাদের ভাষা পরিবর্তন করুন",
                                    "Switch the entire app & Quran translation immediately"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LanguageOptionCard(
                            title = "বাংলা",
                            subtitle = "Bangla (Default)",
                            selected = !isEn,
                            onClick = { onLanguageChanged("bn") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_option_bn")
                        )
                        LanguageOptionCard(
                            title = "English",
                            subtitle = "ইংরেজি",
                            selected = isEn,
                            onClick = { onLanguageChanged("en") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_option_en")
                        )
                    }
                }
            }
        }

        // App Version, What's New & In-App Self-Update Card (Right next to Language)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_update_settings_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = tr(isEn, "অ্যাপ ভার্সন ও আপডেট", "App Version & Updates"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        isEn,
                                        "নতুন ভার্সন এলে সরাসরি ডাউনলোড ও ইনস্টল করুন",
                                        "Check and install new APK releases directly"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("settings_app_version_badge")
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whats_new_section")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tr(
                                    isEn,
                                    "নতুন যা যুক্ত হয়েছে (v${BuildConfig.VERSION_NAME}):",
                                    "What's New (v${BuildConfig.VERSION_NAME}):"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = tr(
                                    isEn,
                                    "• পূর্ণ অফলাইন মক্কা, মদীনা ও আল-আকসা আযান অ্যালার্ম।\n• দৈনিক হাদিস ও আয়াত সুন্দর PNG কার্ড ছবি হিসেবে শেয়ার (স্টোরি/পোস্ট)।\n• হোম স্ক্রিনে পরিষ্কার নামাজ কার্ড ও স্পষ্ট মাকরুহ সময়ের ডিজাইন।\n• মসজিদ ও হালাল ফাইন্ডারে নির্ভরযোগ্য সার্চ ও অ্যাপের ভেতরেই লাইভ ম্যাপ।",
                                    "• Full offline Makkah, Madinah & Al-Aqsa Adhan alarm recordings.\n• Share Daily Hadith & Ayah as clean full-text PNG cards (Story/Post).\n• Clean separate Home current-prayer card & high-contrast Makruh times.\n• Reliable Mosque & Halal Finder with auto-expanding radius & in-app map."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingForUpdate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("check_for_updates_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isCheckingForUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(tr(isEn, "আপডেট যাচাই করা হচ্ছে...", "Checking for updates..."))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(tr(isEn, "আপডেট যাচাই করুন", "Check for Updates"))
                        }
                    }
                }
            }
        }

        // Hijri Date Moon-Sighting Adjustment (+/- 1 day)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = tr(isEn, "হিজরি তারিখ সমন্বয় (চাঁদ দেখা)", "Hijri Date Adjustment (Moon Sighting)"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    isEn,
                                    "স্থানীয় চাঁদ দেখার পার্থক্যের জন্য ±১ দিন সমন্বয় করুন",
                                    "Adjust ±1 day to match local moon sighting"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(-1, 0, 1).forEach { offset ->
                            val label = when (offset) {
                                -1 -> tr(isEn, "-১ দিন", "-1 Day")
                                1 -> tr(isEn, "+১ দিন", "+1 Day")
                                else -> tr(isEn, "০ (স্বয়ংক্রিয়)", "0 (Auto)")
                            }
                            FilterChip(
                                selected = uiState.settings.hijriDayOffset == offset,
                                onClick = { onHijriDayOffsetChanged(offset) },
                                label = { Text(label) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_hijri_offset_$offset")
                            )
                        }
                    }

                    Text(
                        text = "${tr(isEn, "বর্তমান হিজরি তারিখ", "Current Hijri Date")}: ${uiState.hijriDate}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Quran Audio Reciter & Daily Goal Settings
        item {
            val activeReciter = QuranReciter.fromId(uiState.settings.quranReciterId)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = tr(isEn, "কুরআন অডিও ও দৈনিক লক্ষ্য", "Quran Audio & Daily Goal"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    isEn,
                                    "আরবি তিলাওয়াতের ক্বারী ও প্রতিদিনের আয়াত লক্ষ্য নির্ধারণ করুন",
                                    "Choose Arabic reciter & daily Ayah reading goal"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = tr(isEn, "ক্বারী নির্বাচন করুন (অনলাইন স্ট্রিমিং):", "Select Reciter (Online Streaming):"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        QuranReciter.entries.forEach { reciter ->
                            val selected = activeReciter == reciter
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onQuranReciterChanged(reciter) }
                                    .testTag("settings_reciter_${reciter.id}"),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reciter.displayName(isEn),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = reciter.style(isEn),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = tr(isEn, "নির্বাচিত", "Selected"),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = tr(isEn, "দৈনিক কুরআন লক্ষ্য (প্রতিদিন আয়াত সংখ্যা):", "Daily Quran Goal (Ayahs per Day):"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 20, 30).forEach { goalCount ->
                            FilterChip(
                                selected = uiState.settings.dailyQuranGoal == goalCount,
                                onClick = { onDailyQuranGoalChanged(goalCount) },
                                label = {
                                    Text(
                                        tr(
                                            isEn,
                                            "${BanglaUtils.toBanglaNumber(goalCount)} আয়াত",
                                            "$goalCount"
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_quran_goal_$goalCount")
                            )
                        }
                    }
                    Text(
                        text = tr(
                            isEn,
                            "আজকের অগ্রগতি: ${BanglaUtils.toBanglaNumber(uiState.dailyQuranReadCount)} / ${BanglaUtils.toBanglaNumber(uiState.settings.dailyQuranGoal)} আয়াত (প্রতিদিন মধ্যরাতে রিসেট হয়)",
                            "Today's progress: ${uiState.dailyQuranReadCount} / ${uiState.settings.dailyQuranGoal} Ayahs (resets daily)"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Home Screen Widget Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = tr(isEn, "হোম স্ক্রিন উইজেট", "Home Screen Widget"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    isEn,
                                    "পরবর্তী নামাজ, লাইভ কাউন্টডাউন, সাহরি, ইফতার ও হিজরি তারিখ সরাসরি হোম স্ক্রিনে দেখুন",
                                    "See next prayer, live countdown, Sehri, Iftar & Hijri date right on your home screen"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val pinned = PrayerTimesWidgetProvider.requestPinWidget(context)
                                if (pinned) {
                                    onStatusMessage(
                                        tr(
                                            isEn,
                                            "হোম স্ক্রিনে উইজেট যোগ করার পপ-আপ খোলা হয়েছে",
                                            "System prompt opened to pin widget to home screen"
                                        )
                                    )
                                } else {
                                    showWidgetGuideDialog = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_widget_home_button")
                        ) {
                            Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(tr(isEn, "হোম স্ক্রিনে উইজেট যোগ করুন", "Add widget to home screen"))
                        }
                        OutlinedButton(
                            onClick = { showWidgetGuideDialog = true },
                            modifier = Modifier.testTag("widget_help_button")
                        ) {
                            Text(tr(isEn, "নিয়ম", "Guide"))
                        }
                    }
                }
            }
        }

        // Auto Location (GPS), Manual 64-District Selector & Calculation Method Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = tr(isEn, "লোকেশন ও নামাজ গণনা", "Location & Prayer Calculation"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // GPS Toggle at top of Location section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(isEn, "বর্তমান লোকেশন ব্যবহার করুন (জিপিএস)", "Use my current location (GPS)"),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = tr(
                                    isEn,
                                    "বিদেশে ভ্রমণ করলে স্বয়ংক্রিয়ভাবে দেশ, শহর ও টাইমজোন অনুযায়ী সময় দেখাবে",
                                    "Auto-detects country, city & timezone when traveling abroad"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        Switch(
                            checked = uiState.settings.useGps,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        onUseGpsChanged(true)
                                        fetchGpsNow()
                                    } else {
                                        locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    }
                                } else {
                                    onUseGpsChanged(false)
                                }
                            },
                            modifier = Modifier.testTag("gps_location_switch")
                        )
                    }

                    // Active location & timezone summary pill
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${tr(isEn, "সক্রিয় অবস্থান", "Active Location")}: ${uiState.locationDisplayTitle}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${tr(isEn, "টাইমজোন", "Timezone")}: ${uiState.activeTimeZoneId} (${uiState.todaySchedule?.timeZoneLabel ?: ""})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Manual 64-district selector below GPS toggle
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDistrictSheet = true }
                            .testTag("settings_district_selector"),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(
                                        text = tr(isEn, "ম্যানুয়াল জেলা নির্বাচন (বাংলাদেশ ৬৪ জেলা)", "Manual District (Bangladesh 64 Districts)"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${uiState.activeDistrict.displayName(isEn)} • ${uiState.activeDistrict.displayDivision(isEn)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = tr(isEn, "পরিবর্তন", "Select"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider()

                    // Asr Juristic Method
                    Text(
                        text = tr(isEn, "আসরের হিসাব (মাজহাব):", "Asr Calculation Method:"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsrMethod.entries.forEach { method ->
                            FilterChip(
                                selected = uiState.settings.asrMethod == method,
                                onClick = { onAsrMethodChanged(method) },
                                label = { Text(method.shortTitle(isEn)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Calculation Method Selector (Auto, IFB/Karachi, MWL, ISNA, Umm al-Qura, Egypt)
                    Text(
                        text = tr(isEn, "নামাজ গণনা পদ্ধতি:", "Prayer Calculation Method:"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CalculationMethod.entries.forEach { method ->
                            val selected = uiState.settings.calculationMethod == method
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onCalculationMethodChanged(method) },
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = method.title(isEn),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (selected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = tr(isEn, "নির্বাচিত", "Selected"),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Theme Engine
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = tr(isEn, "থিম ও অ্যাকসেন্ট রঙ", "Theme & Accent Colors"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.settings.themeMode == mode,
                                onClick = { onThemeModeChanged(mode) },
                                label = { Text(mode.title(isEn), style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = tr(isEn, "থিমের রঙ বেছে নিন (৬টি থিম):", "Choose Color Theme (6 Themes):"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentTheme.entries.chunked(3).forEach { rowThemes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowThemes.forEach { accent ->
                                    AccentSwatch(
                                        title = accent.title(isEn),
                                        primaryColor = Color(accent.primaryColorHex),
                                        secondaryColor = Color(accent.secondaryColorHex),
                                        selected = uiState.settings.accentTheme == accent,
                                        onClick = { onAccentThemeChanged(accent) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("accent_swatch_${accent.name}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Per-Prayer Adhan Sound & Notification Settings (merged with pre-prayer reminder)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = tr(isEn, "প্রতি ওয়াক্তের আযান ও নোটিফিকেশন", "Per-Prayer Adhan & Notifications"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr(
                                        isEn,
                                        "প্রতিটি নামাজের জন্য আলাদা চালু/বন্ধ, আযানের সুর ও পূর্ব-রিমাইন্ডার বেছে নিন",
                                        "Toggle notifications, choose Adhan voice & pre-prayer reminder per prayer"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                AdhanAlarmReceiver.stopActiveAudio(context)
                                onStatusMessage(
                                    tr(isEn, "আযান শব্দ বন্ধ করা হয়েছে", "Adhan sound stopped")
                                )
                            },
                            modifier = Modifier.testTag("stop_adhan_audio_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(tr(isEn, "বন্ধ", "Stop"))
                        }
                    }

                    if (adhanPlayback.isPlaying) {
                        val posSec = (adhanPlayback.positionMs / 1000).coerceAtLeast(0)
                        val durSec = (adhanPlayback.durationMs / 1000).coerceAtLeast(1)
                        val progress = (adhanPlayback.positionMs.toFloat() / adhanPlayback.durationMs.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        val timeText = String.format(
                            java.util.Locale.US,
                            "%02d:%02d / %02d:%02d",
                            posSec / 60, posSec % 60,
                            durSec / 60, durSec % 60
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("adhan_now_playing_banner")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tr(
                                                isEn,
                                                "চলছে: ${adhanPlayback.voice.titleBn}",
                                                "Playing: ${adhanPlayback.voice.titleEn}"
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = if (isEn) timeText else BanglaUtils.toBanglaDigits(timeText),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            AdhanAlarmReceiver.stopActiveAudio(context)
                                            onStatusMessage(tr(isEn, "আযান বন্ধ করা হয়েছে", "Adhan stopped"))
                                        },
                                        modifier = Modifier.testTag("adhan_banner_stop_button")
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text(tr(isEn, "থামান", "Stop"))
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }

                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("allow_notifications_button")
                        ) {
                            Text(tr(isEn, "নোটিফিকেশন অনুমতি দিন", "Allow Notifications"))
                        }
                    }

                    Text(
                        text = tr(
                            isEn,
                            "ভ্রমণের সময় অটো-সিঙ্ক টাইমজোন (${uiState.activeTimeZoneId}) অনুযায়ী স্থানীয় সময়ে অ্যালার্ট বাজবে।",
                            "Alerts automatically sync to your local travel timezone (${uiState.activeTimeZoneId})."
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    WaqtType.entries.forEach { waqt ->
                        val setting = uiState.settings.alertConfig.settingFor(waqt)
                        Surface(
                            color = if (setting.enabled) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prayer_alert_card_${waqt.key}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = waqt.title(isEn),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (setting.enabled) {
                                                val remStr = if (setting.reminderMinutesBefore > 0) {
                                                    tr(
                                                        isEn,
                                                        " • ${BanglaUtils.toBanglaNumber(setting.reminderMinutesBefore)} মি. আগে",
                                                        " • ${setting.reminderMinutesBefore}m before"
                                                    )
                                                } else {
                                                    ""
                                                }
                                                "${setting.voice.title(isEn)}$remStr"
                                            } else {
                                                tr(isEn, "নোটিফিকেশন বন্ধ আছে", "Notifications off")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = setting.enabled,
                                        onCheckedChange = { enabled -> onWaqtEnabledChanged(waqt, enabled) },
                                        modifier = Modifier.testTag("prayer_toggle_${waqt.key}")
                                    )
                                }

                                if (setting.enabled) {
                                    // Adhan Sound Selection (3 Adhan voices + Beep + Silent/Vibrate)
                                    Text(
                                        text = tr(isEn, "আযানের সুর:", "Adhan Sound:"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(AdhanVoice.MAKKAH, AdhanVoice.MADINAH, AdhanVoice.AL_AQSA).forEach { voice ->
                                            FilterChip(
                                                selected = setting.voice == voice,
                                                onClick = { onWaqtAdhanVoiceChanged(waqt, voice) },
                                                label = {
                                                    Text(
                                                        text = voice.shortTitle(isEn),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1
                                                    )
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("prayer_voice_${waqt.key}_${voice.name.lowercase()}")
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(AdhanVoice.SHORT_BEEP, AdhanVoice.SILENT_VIBRATE).forEach { voice ->
                                            FilterChip(
                                                selected = setting.voice == voice,
                                                onClick = { onWaqtAdhanVoiceChanged(waqt, voice) },
                                                label = {
                                                    Text(
                                                        text = if (voice == AdhanVoice.SILENT_VIBRATE) {
                                                            tr(isEn, "নীরব / ভাইব্রেট", "Silent / Vibrate")
                                                        } else {
                                                            tr(isEn, "সংক্ষিপ্ত বিপ", "Short Beep")
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1
                                                    )
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("prayer_voice_${waqt.key}_${voice.name.lowercase()}")
                                            )
                                        }
                                    }

                                    // Per-prayer "Remind X minutes before" (Off / 5 / 10 / 15 min)
                                    Text(
                                        text = tr(isEn, "কত মিনিট আগে রিমাইন্ডার:", "Remind before prayer:"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(0, 5, 10, 15).forEach { mins ->
                                            FilterChip(
                                                selected = setting.reminderMinutesBefore == mins,
                                                onClick = { onWaqtReminderMinutesChanged(waqt, mins) },
                                                label = {
                                                    Text(
                                                        text = if (mins == 0) {
                                                            tr(isEn, "বন্ধ", "Off")
                                                        } else {
                                                            "${BanglaUtils.formatNumber(mins, isEn)} ${tr(isEn, "মি.", "m")}"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("prayer_reminder_${waqt.key}_$mins")
                                            )
                                        }
                                    }

                                    // Test Notification Button for this prayer
                                    OutlinedButton(
                                        onClick = {
                                            AdhanAlarmReceiver.triggerAlert(
                                                context = context,
                                                waqt = waqt,
                                                voice = setting.voice,
                                                isPreReminder = false,
                                                preMinutes = setting.reminderMinutesBefore,
                                                isEnglish = isEn,
                                                isTestMode = true
                                            )
                                            onStatusMessage(
                                                tr(
                                                    isEn,
                                                    "${waqt.titleBn} টেস্ট নোটিফিকেশন পাঠানো হয়েছে (${setting.voice.shortTitleBn})",
                                                    "Test notification triggered for ${waqt.titleEn} (${setting.voice.shortTitleEn})"
                                                )
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("test_notif_${waqt.key}"),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text(
                                            tr(
                                                isEn,
                                                "${waqt.titleBn} টেস্ট নোটিফিকেশন",
                                                "Test ${waqt.titleEn} notification"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDistrictSheet) {
        DistrictPickerSheet(
            selectedDistrictId = if (uiState.settings.useGps) "" else uiState.activeDistrict.id,
            onSelect = { district ->
                onSelectDistrict(district)
                showDistrictSheet = false
            },
            onDismiss = { showDistrictSheet = false }
        )
    }

    if (showWidgetGuideDialog) {
        AlertDialog(
            onDismissRequest = { showWidgetGuideDialog = false },
            title = {
                Text(
                    text = tr(isEn, "হোম স্ক্রিনে উইজেট যোগ করার নিয়ম", "How to Add Home Screen Widget"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = tr(
                        isEn,
                        "১. ফোনের হোম স্ক্রিনের ফাঁকা জায়গায় কিছুক্ষণ চেপে ধরুন।\n২. নিচের মেনু থেকে \"উইজেট (Widgets)\" অপশনে ট্যাপ করুন।\n৩. তালিকা থেকে \"নূর বাংলা নামাজের সময়সূচী\" খুঁজে বের করে হোম স্ক্রিনে টেনে বসিয়ে দিন।\n\nউইজেটটি আপনার ভাষা (বাংলা/English), লাইট/ডার্ক থিম, লোকেশন ও টাইমজোন অনুযায়ী স্বয়ংক্রিয়ভাবে আপডেট হবে।",
                        "1. Touch and hold an empty space on your phone's Home screen.\n2. Tap \"Widgets\" from the menu.\n3. Find \"Nur Bangla Prayer Times\" and drag it onto your Home screen.\n\nThe widget automatically syncs with your app language (English/Bangla), Light/Dark theme, location, and timezone."
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { showWidgetGuideDialog = false }) {
                    Text(tr(isEn, "বুঝেছি", "Got it"))
                }
            }
        )
    }
}

@Composable
private fun LanguageOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(2.dp, primary, RoundedCornerShape(14.dp))
                else Modifier
            ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    title: String,
    primaryColor: Color,
    secondaryColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(2.dp, primaryColor, RoundedCornerShape(14.dp))
                else Modifier
            ),
        color = primaryColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(secondaryColor)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
