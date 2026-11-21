package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalAppSettings
import com.example.data.local.QuranBookmarkEntity
import com.example.data.model.AccentTheme
import com.example.data.model.AdhanAlertMode
import com.example.data.model.AdhanVoice
import com.example.data.model.AsrMethod
import com.example.data.model.BanglaDhikrCatalog
import com.example.data.model.BangladeshiDistricts
import com.example.data.model.BdDistrict
import com.example.data.model.CalculationMethod
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.DhikrItem
import com.example.data.model.NextWaqtInfo
import com.example.data.model.QuranSearchHit
import com.example.data.model.Surah
import com.example.data.model.ThemeMode
import com.example.data.model.WaqtType
import com.example.data.repository.NoorRepository
import com.example.util.BanglaUtils
import com.example.util.DailyInspirationCatalog
import com.example.util.DailyInspirationEntry
import com.example.util.PrayerTimeCalculator
import com.example.util.QuranAudioController
import com.example.util.QuranAudioState
import com.example.util.QuranReciter
import com.example.util.RamadanEngine
import com.example.util.SehriIftarCountdown
import com.example.util.ZakatCalculator
import com.example.util.ZakatHistoryRecord
import com.example.util.isEnglishLang
import com.example.util.tr
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class NoorUiState(
    val isLoading: Boolean = true,
    val isGuest: Boolean = false,
    val settings: LocalAppSettings = LocalAppSettings(),
    val activeDistrict: BdDistrict = BangladeshiDistricts.DEFAULT,
    val locationDisplayTitle: String = "ঢাকা জেলা",
    val locationSubtitle: String = "ঢাকা বিভাগ • GMT+৬",
    val activeLatitude: Double = BangladeshiDistricts.DEFAULT.latitude,
    val activeLongitude: Double = BangladeshiDistricts.DEFAULT.longitude,
    val activeTimeZoneId: String = "Asia/Dhaka",
    val todaySchedule: DailyPrayerSchedule? = null,
    val currentWaqt: WaqtType? = null,
    val nextWaqtInfo: NextWaqtInfo? = null,
    val isCurrentlyRamadan: Boolean = false,
    val sehriIftarCountdown: SehriIftarCountdown? = null,
    val zakatHistory: List<ZakatHistoryRecord> = emptyList(),
    val todayDateKey: String = "",
    val dailyQuranReadCount: Int = 0,
    val dailyQuranReadKeys: Set<String> = emptySet(),
    val todayInspiration: DailyInspirationEntry? = null,
    val recentInspirations: List<DailyInspirationEntry> = emptyList(),
    val activeInspirationSlotKey: String = "",
    val nextInspirationCountdown: String = "",
    val quranAudioState: QuranAudioState = QuranAudioState(),
    val englishDate: String = "",
    val banglaDate: String = "",
    val hijriDate: String = "",
    val qiblaBearing: Float = 277.5f,
    val distanceToKaabaKm: Int = 5176,
    val surahs: List<Surah> = emptyList(),
    val bookmarks: List<QuranBookmarkEntity> = emptyList(),
    val globalSearchQuery: String = "",
    val quranSearchResults: List<QuranSearchHit> = emptyList(),
    val cloudSyncError: String? = null,
    val statusMessage: String? = null
)

class NoorViewModel(
    private val appContext: Context,
    private val repository: NoorRepository,
    private val currentUserId: String,
    private val isGuest: Boolean = LocalAppSettings.isGuestUserId(currentUserId)
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoorUiState(isGuest = isGuest))
    val uiState: StateFlow<NoorUiState> = _uiState.asStateFlow()

    private val tzRefreshTick = MutableStateFlow(0L)
    private var hasAppliedInitialRemotePrefs = false

    private val quranAudioController = QuranAudioController(appContext) { surahNumber, ayahNumber ->
        recordQuranAyahHeard(surahNumber, ayahNumber)
    }

    init {
        DailyInspirationCatalog.initialize(appContext)
        loadQuranData()
        observeAudioStream()
        observeLocalStreams()
        if (!isGuest) {
            observeCloudSyncStreams()
        }
        startCountdownTicker()
    }

    private fun observeAudioStream() {
        viewModelScope.launch {
            quranAudioController.audioState.collect { audio ->
                _uiState.update { it.copy(quranAudioState = audio) }
            }
        }
    }

    private fun loadQuranData() {
        viewModelScope.launch {
            val surahs = repository.getSurahs()
            _uiState.update { it.copy(surahs = surahs) }
        }
    }

    private fun observeLocalStreams() {
        viewModelScope.launch {
            combine(
                repository.settingsFlow,
                repository.observeLocalBookmarks(currentUserId),
                tzRefreshTick
            ) { settings, bookmarks, _ ->
                val isEn = isEnglishLang(settings.languageCode)
                val district = BangladeshiDistricts.findById(settings.districtId)
                val gpsLat = settings.gpsLatitude
                val gpsLon = settings.gpsLongitude
                val isManual = !settings.useGps || gpsLat == null || gpsLon == null
                val lat = if (!isManual) gpsLat else district.latitude
                val lon = if (!isManual) gpsLon else district.longitude

                val nowMillis = System.currentTimeMillis()
                val targetTz = PrayerTimeCalculator.resolveTimeZone(
                    latitude = lat,
                    longitude = lon,
                    isManualDistrict = isManual,
                    atMillis = nowMillis
                )
                val tzHours = targetTz.getOffset(nowMillis) / 3_600_000.0
                val tzLabel = PrayerTimeCalculator.formatTimeZoneLabel(targetTz, nowMillis, isEn)

                val now = Calendar.getInstance(targetTz)
                val todayBase = PrayerTimeCalculator.calculateForDate(
                    calendar = now,
                    latitude = lat,
                    longitude = lon,
                    asrMethod = settings.asrMethod,
                    timeZoneHours = tzHours,
                    calculationMethod = settings.calculationMethod,
                    timeZoneLabel = tzLabel
                )
                val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomorrowSchedule = PrayerTimeCalculator.calculateForDate(
                    calendar = tomorrow,
                    latitude = lat,
                    longitude = lon,
                    asrMethod = settings.asrMethod,
                    timeZoneHours = tzHours,
                    calculationMethod = settings.calculationMethod,
                    timeZoneLabel = tzLabel
                )
                val todaySchedule = todayBase.copy(ishaEndMinute = tomorrowSchedule.fajrMinute)
                val nextWaqt = PrayerTimeCalculator.computeNextWaqt(now, todaySchedule, tomorrowSchedule.fajrMinute)
                val currentWaqt = PrayerTimeCalculator.getCurrentWaqt(now, todaySchedule)
                val isRamadanNow = RamadanEngine.isCurrentlyRamadan(now, settings.hijriDayOffset)
                val sehriIftarCd = RamadanEngine.computeSehriIftarCountdown(
                    now = now,
                    todaySchedule = todaySchedule,
                    tomorrowSehriEndMinute = tomorrowSchedule.sehriEndMinute
                )
                val zakatHistory = ZakatCalculator.parseHistoryJson(settings.zakatHistoryJson)
                val todayDateKey = DailyInspirationCatalog.dateKeyFor(now)
                val todayReadKeys = settings.todayReadAyahKeys(todayDateKey)
                val activeSlotKey = DailyInspirationCatalog.slotKeyFor(now)
                val nextSlotCountdown = DailyInspirationCatalog.formatNextSlotCountdown(now, isEn)
                val recentInspirations = DailyInspirationCatalog.todayAndPreviousSevenDays(
                    todayCalendar = now,
                    hijriDayOffset = settings.hijriDayOffset
                )
                quranAudioController.setReciter(QuranReciter.fromId(settings.quranReciterId))

                val (title, subtitle) = if (isManual) {
                    if (isEn) {
                        "${district.nameEn} District" to "${district.divisionEn} Division • $tzLabel"
                    } else {
                        "${district.nameBn} জেলা" to "${district.divisionBn} বিভাগ • $tzLabel"
                    }
                } else if (PrayerTimeCalculator.isInsideBangladesh(lat, lon)) {
                    val label = settings.gpsLocationLabel?.takeIf { it.isNotBlank() } ?: district.displayName(isEn)
                    if (isEn) {
                        "$label (GPS)" to "${district.divisionEn} Division • $tzLabel"
                    } else {
                        "$label (জিপিএস)" to "${district.divisionBn} বিভাগ • $tzLabel"
                    }
                } else {
                    val coords = String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
                    val label = settings.gpsLocationLabel?.takeIf { it.isNotBlank() }
                        ?: BanglaUtils.formatDigits(coords, isEn)
                    if (isEn) {
                        "$label (GPS)" to "Auto Worldwide Schedule • $tzLabel"
                    } else {
                        "$label (জিপিএস)" to "স্বয়ংক্রিয় বিশ্বব্যাপী সময়সূচী • $tzLabel"
                    }
                }

                _uiState.value.copy(
                    isLoading = false,
                    isGuest = isGuest,
                    settings = settings,
                    activeDistrict = district,
                    locationDisplayTitle = title,
                    locationSubtitle = subtitle,
                    activeLatitude = lat,
                    activeLongitude = lon,
                    activeTimeZoneId = targetTz.id,
                    todaySchedule = todaySchedule,
                    currentWaqt = currentWaqt,
                    nextWaqtInfo = nextWaqt,
                    isCurrentlyRamadan = isRamadanNow,
                    sehriIftarCountdown = sehriIftarCd,
                    zakatHistory = zakatHistory,
                    todayDateKey = todayDateKey,
                    dailyQuranReadCount = todayReadKeys.size,
                    dailyQuranReadKeys = todayReadKeys,
                    todayInspiration = recentInspirations.firstOrNull(),
                    recentInspirations = recentInspirations,
                    activeInspirationSlotKey = activeSlotKey,
                    nextInspirationCountdown = nextSlotCountdown,
                    englishDate = BanglaUtils.getGregorianDate(now, isEn),
                    banglaDate = BanglaUtils.getBanglaCalendarDate(now, isEn),
                    hijriDate = BanglaUtils.getHijriDate(now, isEn, settings.hijriDayOffset),
                    qiblaBearing = PrayerTimeCalculator.calculateQiblaBearing(lat, lon),
                    distanceToKaabaKm = PrayerTimeCalculator.distanceToKaabaKm(lat, lon),
                    bookmarks = bookmarks
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { computed ->
                    _uiState.update { current ->
                        computed.copy(
                            surahs = current.surahs,
                            quranAudioState = current.quranAudioState,
                            globalSearchQuery = current.globalSearchQuery,
                            quranSearchResults = current.quranSearchResults,
                            cloudSyncError = current.cloudSyncError,
                            statusMessage = current.statusMessage
                        )
                    }
                }
        }
    }

    private fun observeCloudSyncStreams() {
        viewModelScope.launch {
            repository.remoteSync.observeBookmarks(currentUserId)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Cloud bookmark sync paused", fsErr ?: error)
                    val isEn = isEnglishLang(_uiState.value.settings.languageCode)
                    _uiState.update {
                        it.copy(
                            cloudSyncError = tr(
                                isEn,
                                "ক্লাউড সিঙ্ক অফলাইনে রয়েছে (লোকাল ডেটা সুরক্ষিত)",
                                "Cloud sync is offline (local data is safe)"
                            )
                        )
                    }
                }
                .collect { remoteBookmarks ->
                    repository.mergeRemoteBookmarks(currentUserId, remoteBookmarks)
                    _uiState.update { it.copy(cloudSyncError = null) }
                }
        }

        viewModelScope.launch {
            repository.remoteSync.observePreferences(currentUserId)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Cloud preferences sync paused", fsErr ?: error)
                }
                .collect { remotePrefs ->
                    if (remotePrefs != null && !hasAppliedInitialRemotePrefs) {
                        hasAppliedInitialRemotePrefs = true
                        repository.applyRemotePreferences(remotePrefs)
                    }
                }
        }
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val state = _uiState.value
                val schedule = state.todaySchedule ?: continue
                val isEn = isEnglishLang(state.settings.languageCode)
                val targetTz = TimeZone.getTimeZone(state.activeTimeZoneId)
                val now = Calendar.getInstance(targetTz)
                val nowMillis = now.timeInMillis
                val tzHours = targetTz.getOffset(nowMillis) / 3_600_000.0
                val tzLabel = PrayerTimeCalculator.formatTimeZoneLabel(targetTz, nowMillis, isEn)
                val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomorrowSchedule = PrayerTimeCalculator.calculateForDate(
                    calendar = tomorrow,
                    latitude = state.activeLatitude,
                    longitude = state.activeLongitude,
                    asrMethod = state.settings.asrMethod,
                    timeZoneHours = tzHours,
                    calculationMethod = state.settings.calculationMethod,
                    timeZoneLabel = tzLabel
                )
                val nextWaqt = PrayerTimeCalculator.computeNextWaqt(now, schedule, tomorrowSchedule.fajrMinute)
                val currentWaqt = PrayerTimeCalculator.getCurrentWaqt(now, schedule)
                val sehriIftarCd = RamadanEngine.computeSehriIftarCountdown(
                    now = now,
                    todaySchedule = schedule,
                    tomorrowSehriEndMinute = tomorrowSchedule.sehriEndMinute
                )
                val currentDayKey = DailyInspirationCatalog.dateKeyFor(now)
                val currentSlotKey = DailyInspirationCatalog.slotKeyFor(now)
                val nextSlotCountdown = DailyInspirationCatalog.formatNextSlotCountdown(now, isEn)
                val slotChanged = state.activeInspirationSlotKey.isNotEmpty() &&
                    currentSlotKey != state.activeInspirationSlotKey
                if ((state.todayDateKey.isNotEmpty() && currentDayKey != state.todayDateKey) || slotChanged) {
                    tzRefreshTick.value = nowMillis
                }
                val refreshedInspirations = if (slotChanged) {
                    DailyInspirationCatalog.todayAndPreviousSevenDays(
                        todayCalendar = now,
                        hijriDayOffset = state.settings.hijriDayOffset
                    )
                } else {
                    null
                }
                _uiState.update {
                    it.copy(
                        nextWaqtInfo = nextWaqt,
                        currentWaqt = currentWaqt,
                        sehriIftarCountdown = sehriIftarCd,
                        activeInspirationSlotKey = currentSlotKey,
                        nextInspirationCountdown = nextSlotCountdown,
                        todayInspiration = refreshedInspirations?.firstOrNull() ?: it.todayInspiration,
                        recentInspirations = refreshedInspirations ?: it.recentInspirations
                    )
                }
            }
        }
    }

    fun setLanguageCode(code: String, onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        viewModelScope.launch {
            val normalized = if (code == "en") "en" else "bn"
            repository.preferences.setLanguageCode(normalized)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            repository.syncPreferencesToCloud(currentUserId, latest)
            showMessage(
                if (normalized == "en") "Language switched to English"
                else "ভাষা বাংলায় পরিবর্তন করা হয়েছে"
            )
        }
    }

    fun setUseGps(enabled: Boolean, onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        viewModelScope.launch {
            repository.preferences.setUseGps(enabled)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            if (!enabled) {
                val isEn = isEnglishLang(latest.languageCode)
                val district = BangladeshiDistricts.findById(latest.districtId)
                showMessage(
                    tr(
                        isEn,
                        "ম্যানুয়াল জেলা চালু হয়েছে: ${district.nameBn}",
                        "Manual district enabled: ${district.nameEn}"
                    )
                )
            }
        }
    }

    fun refreshForTimezoneChange(onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        tzRefreshTick.value = System.currentTimeMillis()
        viewModelScope.launch {
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
        }
    }

    fun selectDistrict(district: BdDistrict, onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        viewModelScope.launch {
            repository.preferences.setDistrict(district.id)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            repository.syncPreferencesToCloud(currentUserId, latest)
            val isEn = isEnglishLang(latest.languageCode)
            showMessage(
                tr(
                    isEn,
                    "${district.nameBn} জেলার সময়সূচী নির্বাচিত হয়েছে",
                    "Prayer times updated for ${district.nameEn} District"
                )
            )
        }
    }

    fun updateGpsCoordinates(
        lat: Double,
        lon: Double,
        resolvedPlaceLabel: String?,
        showToast: Boolean = true,
        onAlarmsNeedUpdate: (LocalAppSettings) -> Unit
    ) {
        viewModelScope.launch {
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            val nearest = BangladeshiDistricts.findNearest(lat, lon)
            val displayLabel = when {
                !resolvedPlaceLabel.isNullOrBlank() -> resolvedPlaceLabel
                PrayerTimeCalculator.isInsideBangladesh(lat, lon) -> nearest.displayName(isEn)
                else -> String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
            }
            repository.preferences.setGpsLocation(lat, lon, nearest.id, displayLabel)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            repository.syncPreferencesToCloud(currentUserId, latest)
            if (showToast) {
                showMessage(
                    tr(
                        isEn,
                        "জিপিএস লোকেশন আপডেট হয়েছে: $displayLabel",
                        "GPS location updated: $displayLabel"
                    )
                )
            }
        }
    }

    fun setAsrMethod(method: AsrMethod, onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        viewModelScope.launch {
            repository.preferences.setAsrMethod(method)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            repository.syncPreferencesToCloud(currentUserId, latest)
        }
    }

    fun setCalculationMethod(method: CalculationMethod, onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        viewModelScope.launch {
            repository.preferences.setCalculationMethod(method)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            val isEn = isEnglishLang(latest.languageCode)
            showMessage(
                tr(
                    isEn,
                    "হিসাব পদ্ধতি: ${method.titleBn}",
                    "Calculation method: ${method.titleEn}"
                )
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.preferences.setThemeMode(mode)
            val latest = repository.settingsFlow.first()
            repository.syncPreferencesToCloud(currentUserId, latest)
        }
    }

    fun setAccentTheme(accent: AccentTheme) {
        viewModelScope.launch {
            repository.preferences.setAccentTheme(accent)
            val latest = repository.settingsFlow.first()
            repository.syncPreferencesToCloud(currentUserId, latest)
        }
    }

    fun setQuranDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setQuranDarkMode(enabled)
            val latest = repository.settingsFlow.first()
            repository.syncPreferencesToCloud(currentUserId, latest)
        }
    }

    fun setQuranFontSize(size: Int) {
        viewModelScope.launch {
            repository.preferences.setQuranFontSize(size)
            val latest = repository.settingsFlow.first()
            repository.syncPreferencesToCloud(currentUserId, latest)
        }
    }

    fun setWaqtAlertMode(waqt: WaqtType, mode: AdhanAlertMode, onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        viewModelScope.launch {
            repository.setWaqtAlertMode(waqt, mode)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
        }
    }

    fun setWaqtEnabled(waqt: WaqtType, enabled: Boolean, onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        viewModelScope.launch {
            repository.preferences.setWaqtEnabled(waqt, enabled)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            val isEn = isEnglishLang(latest.languageCode)
            showMessage(
                if (enabled) tr(isEn, "${waqt.titleBn} নোটিফিকেশন চালু হয়েছে", "${waqt.titleEn} notification enabled")
                else tr(isEn, "${waqt.titleBn} নোটিফিকেশন বন্ধ করা হয়েছে", "${waqt.titleEn} notification turned off")
            )
        }
    }

    fun setWaqtAdhanVoice(waqt: WaqtType, voice: AdhanVoice, onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        viewModelScope.launch {
            repository.preferences.setWaqtAdhanVoice(waqt, voice)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            val isEn = isEnglishLang(latest.languageCode)
            showMessage(
                tr(
                    isEn,
                    "${waqt.titleBn}: ${voice.titleBn}",
                    "${waqt.titleEn}: ${voice.titleEn}"
                )
            )
        }
    }

    fun setWaqtReminderMinutes(waqt: WaqtType, minutes: Int, onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        viewModelScope.launch {
            repository.preferences.setWaqtReminderMinutes(waqt, minutes)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            val isEn = isEnglishLang(latest.languageCode)
            showMessage(
                if (minutes == 0) {
                    tr(isEn, "${waqt.titleBn} পূর্ব-সতর্কতা বন্ধ করা হয়েছে", "${waqt.titleEn} pre-reminder turned off")
                } else {
                    tr(
                        isEn,
                        "${waqt.titleBn}: ${BanglaUtils.toBanglaNumber(minutes)} মিনিট আগে রিমাইন্ডার",
                        "${waqt.titleEn}: Remind $minutes min before"
                    )
                }
            )
        }
    }

    fun setPrePrayerMinutes(minutes: Int, onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        viewModelScope.launch {
            repository.preferences.setPrePrayerMinutes(minutes)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            repository.syncPreferencesToCloud(currentUserId, latest)
        }
    }

    fun toggleBookmark(surah: Surah, ayahNumber: Int, arabicText: String, banglaText: String) {
        viewModelScope.launch {
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            val bId = "${surah.number}_$ayahNumber"
            val exists = _uiState.value.bookmarks.any { it.bookmarkId == bId }
            repository.toggleBookmark(currentUserId, surah, ayahNumber, arabicText, banglaText, exists)
            showMessage(
                if (exists) {
                    tr(isEn, "বুকমার্ক সরানো হয়েছে", "Bookmark removed")
                } else {
                    tr(isEn, "আয়াত বুকমার্ক করা হয়েছে", "Ayah bookmarked")
                }
            )
        }
    }

    fun updateGlobalSearch(query: String) {
        _uiState.update { it.copy(globalSearchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.update { it.copy(quranSearchResults = emptyList()) }
            } else {
                val quranHits = repository.searchQuran(query)
                _uiState.update { it.copy(quranSearchResults = quranHits) }
            }
        }
    }

    fun incrementTasbih(): Boolean {
        val current = _uiState.value.settings
        val nextRaw = current.tasbihCount + 1
        val completedCycle = nextRaw >= current.tasbihTarget
        val nextCount = if (completedCycle) 0 else nextRaw
        val nextCycles = if (completedCycle) current.tasbihCycles + 1 else current.tasbihCycles
        viewModelScope.launch {
            repository.preferences.updateTasbih(
                dhikrId = current.tasbihDhikrId,
                target = current.tasbihTarget,
                count = nextCount,
                cycles = nextCycles
            )
        }
        return completedCycle
    }

    fun resetTasbih() {
        val current = _uiState.value.settings
        viewModelScope.launch {
            repository.preferences.updateTasbih(
                dhikrId = current.tasbihDhikrId,
                target = current.tasbihTarget,
                count = 0,
                cycles = 0
            )
        }
    }

    fun selectTasbihDhikr(dhikr: DhikrItem) {
        viewModelScope.launch {
            repository.preferences.updateTasbih(
                dhikrId = dhikr.id,
                target = dhikr.recommendedCount,
                count = 0,
                cycles = _uiState.value.settings.tasbihCycles
            )
        }
    }

    fun setTasbihTarget(target: Int) {
        val current = _uiState.value.settings
        val safeTarget = target.coerceIn(1, 10000)
        viewModelScope.launch {
            repository.preferences.updateTasbih(
                dhikrId = current.tasbihDhikrId,
                target = safeTarget,
                count = current.tasbihCount.coerceAtMost(safeTarget - 1),
                cycles = current.tasbihCycles
            )
        }
    }

    fun currentDhikr(): DhikrItem {
        val id = _uiState.value.settings.tasbihDhikrId
        return BanglaDhikrCatalog.ALL.find { it.id == id } ?: BanglaDhikrCatalog.ALL.first()
    }

    fun setHijriDayOffset(offset: Int) {
        viewModelScope.launch {
            val clamped = offset.coerceIn(-1, 1)
            repository.preferences.setHijriDayOffset(clamped)
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            val label = when (clamped) {
                -1 -> tr(isEn, "-১ দিন", "-1 day")
                1 -> tr(isEn, "+১ দিন", "+1 day")
                else -> tr(isEn, "০ দিন (স্বয়ংক্রিয়)", "0 days (Auto)")
            }
            showMessage(
                tr(
                    isEn,
                    "হিজরি তারিখ সমন্বয়: $label",
                    "Hijri date adjustment: $label"
                )
            )
        }
    }

    fun toggleFavoriteDua(duaId: String) {
        viewModelScope.launch {
            val added = repository.preferences.toggleFavoriteDua(duaId)
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            showMessage(
                if (added) tr(isEn, "দোয়া বুকমার্ক করা হয়েছে", "Dua saved to favorites")
                else tr(isEn, "দোয়া বুকমার্ক থেকে সরানো হয়েছে", "Dua removed from favorites")
            )
        }
    }

    fun setRamadanMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setRamadanMode(enabled)
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            showMessage(
                if (enabled) tr(isEn, "রমজান মোড চালু হয়েছে — হোমে সাহরি ও ইফতার দেখাবে", "Ramadan mode enabled — Home highlights Sehri & Iftar")
                else tr(isEn, "রমজান মোড বন্ধ করা হয়েছে", "Ramadan mode turned off")
            )
        }
    }

    fun setMetalPrices(goldPerGramBdt: Double, silverPerGramBdt: Double) {
        viewModelScope.launch {
            repository.preferences.setMetalPrices(goldPerGramBdt, silverPerGramBdt)
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            showMessage(
                tr(isEn, "স্বর্ণ ও রূপার বাজারদর সংরক্ষিত হয়েছে", "Gold & silver rates saved")
            )
        }
    }

    fun saveZakatCalculation(record: ZakatHistoryRecord) {
        viewModelScope.launch {
            repository.preferences.addZakatHistoryRecord(record)
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            showMessage(
                tr(isEn, "যাকাত হিসাব ইতিহাসে সংরক্ষিত হয়েছে", "Zakat calculation saved to history")
            )
        }
    }

    fun deleteZakatCalculation(recordId: String) {
        viewModelScope.launch {
            repository.preferences.deleteZakatHistoryRecord(recordId)
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            showMessage(
                tr(isEn, "যাকাত হিসাব মুছে ফেলা হয়েছে", "Zakat history item deleted")
            )
        }
    }

    fun setQuranReciter(reciter: QuranReciter) {
        viewModelScope.launch {
            repository.preferences.setQuranReciter(reciter.id)
            quranAudioController.setReciter(reciter)
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            showMessage(
                tr(
                    isEn,
                    "ক্বারী নির্বাচিত: ${reciter.nameBn}",
                    "Reciter selected: ${reciter.nameEn}"
                )
            )
        }
    }

    fun setDailyQuranGoal(goalAyahs: Int) {
        viewModelScope.launch {
            repository.preferences.setDailyQuranGoal(goalAyahs)
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            showMessage(
                tr(
                    isEn,
                    "দৈনিক কুরআন লক্ষ্য: ${BanglaUtils.toBanglaNumber(goalAyahs)} আয়াত",
                    "Daily Quran goal set to $goalAyahs Ayahs"
                )
            )
        }
    }

    fun playSurahAudio(surah: Surah) {
        val reciter = QuranReciter.fromId(_uiState.value.settings.quranReciterId)
        quranAudioController.playSurah(surah, reciter)
    }

    fun playAyahAudio(surah: Surah, ayahNumber: Int) {
        val reciter = QuranReciter.fromId(_uiState.value.settings.quranReciterId)
        quranAudioController.playAyah(surah, ayahNumber, reciter)
    }

    fun toggleQuranAudioPlayPause() {
        quranAudioController.togglePlayPause()
    }

    fun nextQuranAudioAyah() {
        quranAudioController.nextAyah()
    }

    fun previousQuranAudioAyah() {
        quranAudioController.previousAyah()
    }

    fun stopQuranAudio() {
        quranAudioController.stop()
    }

    private fun recordQuranAyahHeard(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            val tz = TimeZone.getTimeZone(_uiState.value.activeTimeZoneId)
            val todayKey = DailyInspirationCatalog.dateKeyFor(Calendar.getInstance(tz))
            repository.preferences.recordQuranAyahProgress(todayKey, surahNumber, ayahNumber)
        }
    }

    fun toggleAyahReadToday(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            val tz = TimeZone.getTimeZone(_uiState.value.activeTimeZoneId)
            val todayKey = DailyInspirationCatalog.dateKeyFor(Calendar.getInstance(tz))
            val marked = repository.preferences.toggleQuranAyahRead(todayKey, surahNumber, ayahNumber)
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            if (marked) {
                showMessage(tr(isEn, "আজকের পাঠে যুক্ত হয়েছে", "Added to today's Quran goal"))
            }
        }
    }

    fun toggleHajjStepCompleted(stepId: String) {
        viewModelScope.launch {
            repository.preferences.toggleHajjStepCompleted(stepId)
        }
    }

    fun resetHajjChecklist() {
        viewModelScope.launch {
            repository.preferences.resetHajjChecklist()
            val isEn = isEnglishLang(_uiState.value.settings.languageCode)
            showMessage(tr(isEn, "হজ্জ ও উমরাহ চেকলিস্ট রিসেট করা হয়েছে", "Hajj & Umrah checklist reset"))
        }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    override fun onCleared() {
        quranAudioController.release()
        super.onCleared()
    }

    companion object {
        private const val TAG = "NoorViewModel"
    }
}
