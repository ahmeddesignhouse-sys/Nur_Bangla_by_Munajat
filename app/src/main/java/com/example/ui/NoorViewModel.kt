package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalAppSettings
import com.example.data.local.PrayerLogEntity
import com.example.data.local.QuranBookmarkEntity
import com.example.data.model.AccentTheme
import com.example.data.model.AdhanAlertMode
import com.example.data.model.AsrMethod
import com.example.data.model.BanglaDhikrCatalog
import com.example.data.model.BangladeshiDistricts
import com.example.data.model.BdDistrict
import com.example.data.model.CalculationMethod
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.DhikrItem
import com.example.data.model.NextWaqtInfo
import com.example.data.model.PrayerStatus
import com.example.data.model.QuranSearchHit
import com.example.data.model.Surah
import com.example.data.model.ThemeMode
import com.example.data.model.WaqtType
import com.example.data.repository.NoorRepository
import com.example.util.BanglaUtils
import com.example.util.PrayerTimeCalculator
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
import java.util.TimeZone

data class DailyPrayerSummary(
    val dateKey: String,
    val onTimeCount: Int,
    val lateCount: Int,
    val qazaCount: Int
)

data class NoorUiState(
    val isLoading: Boolean = true,
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
    val englishDateBn: String = "",
    val banglaDateBn: String = "",
    val hijriDateBn: String = "",
    val qiblaBearing: Float = 277.5f,
    val distanceToKaabaKm: Int = 5176,
    val surahs: List<Surah> = emptyList(),
    val bookmarks: List<QuranBookmarkEntity> = emptyList(),
    val prayerLogs: List<PrayerLogEntity> = emptyList(),
    val selectedTrackerDateKey: String = BanglaUtils.getDateKey(Calendar.getInstance()),
    val globalSearchQuery: String = "",
    val quranSearchResults: List<QuranSearchHit> = emptyList(),
    val logSearchResults: List<PrayerLogEntity> = emptyList(),
    val currentStreakDays: Int = 0,
    val totalOnTimePrayers: Int = 0,
    val last7DaysSummary: List<DailyPrayerSummary> = emptyList(),
    val cloudSyncError: String? = null,
    val statusMessage: String? = null
)

class NoorViewModel(
    private val repository: NoorRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoorUiState())
    val uiState: StateFlow<NoorUiState> = _uiState.asStateFlow()

    private var hasAppliedInitialRemotePrefs = false

    init {
        loadQuranData()
        observeLocalStreams()
        observeCloudSyncStreams()
        startCountdownTicker()
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
                repository.observeLocalPrayerLogs(currentUserId),
                repository.observeLocalBookmarks(currentUserId)
            ) { settings, logs, bookmarks ->
                val district = BangladeshiDistricts.findById(settings.districtId)
                val isManual = !settings.useGps || settings.gpsLatitude == null || settings.gpsLongitude == null
                val lat = if (!isManual) settings.gpsLatitude!! else district.latitude
                val lon = if (!isManual) settings.gpsLongitude!! else district.longitude

                val nowMillis = System.currentTimeMillis()
                val targetTz = PrayerTimeCalculator.resolveTimeZone(
                    latitude = lat,
                    longitude = lon,
                    isManualDistrict = isManual,
                    atMillis = nowMillis
                )
                val tzHours = targetTz.getOffset(nowMillis) / 3_600_000.0
                val tzLabel = PrayerTimeCalculator.formatTimeZoneLabel(targetTz, nowMillis)

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
                val (streak, totalOnTime, last7) = computeStats(logs)

                val (title, subtitle) = if (isManual) {
                    "${district.nameBn} জেলা" to "${district.divisionBn} বিভাগ • $tzLabel"
                } else if (PrayerTimeCalculator.isInsideBangladesh(lat, lon)) {
                    val label = settings.gpsLocationLabel?.takeIf { it.isNotBlank() } ?: district.nameBn
                    "$label (জিপিএস)" to "${district.divisionBn} বিভাগ • $tzLabel"
                } else {
                    val label = settings.gpsLocationLabel?.takeIf { it.isNotBlank() }
                        ?: "${BanglaUtils.toBanglaDigits(String.format("%.2f°, %.2f°", lat, lon))}"
                    "$label (জিপিএস)" to "স্বয়ংক্রিয় বিশ্বব্যাপী সময়সূচী • $tzLabel"
                }

                _uiState.value.copy(
                    isLoading = false,
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
                    englishDateBn = BanglaUtils.getEnglishDateBangla(now),
                    banglaDateBn = BanglaUtils.getBanglaCalendarDate(now),
                    hijriDateBn = BanglaUtils.getHijriDateBangla(now),
                    qiblaBearing = PrayerTimeCalculator.calculateQiblaBearing(lat, lon),
                    distanceToKaabaKm = PrayerTimeCalculator.distanceToKaabaKm(lat, lon),
                    bookmarks = bookmarks,
                    prayerLogs = logs,
                    currentStreakDays = streak,
                    totalOnTimePrayers = totalOnTime,
                    last7DaysSummary = last7
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { computed ->
                    _uiState.update { current ->
                        computed.copy(
                            surahs = current.surahs,
                            selectedTrackerDateKey = current.selectedTrackerDateKey,
                            globalSearchQuery = current.globalSearchQuery,
                            quranSearchResults = current.quranSearchResults,
                            logSearchResults = current.logSearchResults,
                            cloudSyncError = current.cloudSyncError,
                            statusMessage = current.statusMessage
                        )
                    }
                }
        }
    }

    private fun observeCloudSyncStreams() {
        viewModelScope.launch {
            repository.remoteSync.observePrayerLogs(currentUserId)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val root = fsErr ?: error
                    Log.w(TAG, "Cloud prayer log sync paused", root)
                    _uiState.update { it.copy(cloudSyncError = "ক্লাউড সিঙ্ক অফলাইনে রয়েছে (লোকাল ডেটা সুরক্ষিত)") }
                }
                .collect { remoteLogs ->
                    repository.mergeRemotePrayerLogs(currentUserId, remoteLogs)
                    _uiState.update { it.copy(cloudSyncError = null) }
                }
        }

        viewModelScope.launch {
            repository.remoteSync.observeBookmarks(currentUserId)
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    Log.w(TAG, "Cloud bookmark sync paused", fsErr ?: error)
                }
                .collect { remoteBookmarks ->
                    repository.mergeRemoteBookmarks(currentUserId, remoteBookmarks)
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
                val targetTz = TimeZone.getTimeZone(state.activeTimeZoneId)
                val now = Calendar.getInstance(targetTz)
                val nowMillis = now.timeInMillis
                val tzHours = targetTz.getOffset(nowMillis) / 3_600_000.0
                val tzLabel = PrayerTimeCalculator.formatTimeZoneLabel(targetTz, nowMillis)
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
                _uiState.update {
                    it.copy(
                        nextWaqtInfo = nextWaqt,
                        currentWaqt = currentWaqt
                    )
                }
            }
        }
    }

    fun selectDistrict(district: BdDistrict, onAlarmsNeedUpdate: (LocalAppSettings) -> Unit) {
        viewModelScope.launch {
            repository.preferences.setDistrict(district.id)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            repository.syncPreferencesToCloud(currentUserId, latest)
            showMessage("${district.nameBn} জেলার সময়সূচী নির্বাচিত হয়েছে")
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
            val nearest = BangladeshiDistricts.findNearest(lat, lon)
            val displayLabel = when {
                !resolvedPlaceLabel.isNullOrBlank() -> resolvedPlaceLabel
                PrayerTimeCalculator.isInsideBangladesh(lat, lon) -> nearest.nameBn
                else -> String.format("%.2f°, %.2f°", lat, lon)
            }
            repository.preferences.setGpsLocation(lat, lon, nearest.id, displayLabel)
            val latest = repository.settingsFlow.first()
            onAlarmsNeedUpdate(latest)
            repository.syncPreferencesToCloud(currentUserId, latest)
            if (showToast) {
                showMessage("জিপিএস লোকেশন আপডেট হয়েছে: $displayLabel")
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
            showMessage("হিসাব পদ্ধতি: ${method.titleBn}")
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
            val bId = "${surah.number}_$ayahNumber"
            val exists = _uiState.value.bookmarks.any { it.bookmarkId == bId }
            repository.toggleBookmark(currentUserId, surah, ayahNumber, arabicText, banglaText, exists)
            showMessage(if (exists) "বুকমার্ক সরানো হয়েছে" else "আয়াত বুকমার্ক করা হয়েছে")
        }
    }

    fun updateGlobalSearch(query: String) {
        _uiState.update { it.copy(globalSearchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                _uiState.update { it.copy(quranSearchResults = emptyList(), logSearchResults = emptyList()) }
            } else {
                val quranHits = repository.searchQuran(query)
                val logHits = repository.searchPrayerLogs(currentUserId, query)
                _uiState.update { it.copy(quranSearchResults = quranHits, logSearchResults = logHits) }
            }
        }
    }

    fun selectTrackerDate(dateKey: String) {
        _uiState.update { it.copy(selectedTrackerDateKey = dateKey) }
    }

    fun updatePrayerStatus(dateKey: String, waqt: WaqtType, status: PrayerStatus) {
        viewModelScope.launch {
            val result = repository.updatePrayerStatus(currentUserId, dateKey, waqt, status)
            if (result.isFailure) {
                _uiState.update { it.copy(cloudSyncError = "লোকাল স্টোরেজে সংরক্ষিত; ক্লাউডে সিঙ্ক বাকি রয়েছে") }
            }
        }
    }

    fun updatePrayerNote(dateKey: String, note: String) {
        viewModelScope.launch {
            repository.updatePrayerNote(currentUserId, dateKey, note)
            showMessage("নোট সংরক্ষিত হয়েছে")
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

    fun showMessage(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun computeStats(logs: List<PrayerLogEntity>): Triple<Int, Int, List<DailyPrayerSummary>> {
        val byDate = logs.associateBy { it.dateKey }
        var totalOnTime = 0
        for (log in logs) {
            WaqtType.entries.forEach { w ->
                if (log.statusFor(w) == PrayerStatus.ON_TIME) totalOnTime++
            }
        }

        var streak = 0
        val cal = Calendar.getInstance()
        for (offset in 0..365) {
            val checkCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = BanglaUtils.getDateKey(checkCal)
            val log = byDate[key]
            val completedCount = if (log == null) 0 else {
                WaqtType.entries.count { log.statusFor(it) == PrayerStatus.ON_TIME || log.statusFor(it) == PrayerStatus.LATE }
            }
            if (completedCount >= 5) {
                streak++
            } else if (offset > 0) {
                break
            }
        }

        val last7 = (6 downTo 0).map { offset ->
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = BanglaUtils.getDateKey(dayCal)
            val log = byDate[key]
            if (log == null) {
                DailyPrayerSummary(key, 0, 0, 0)
            } else {
                DailyPrayerSummary(
                    dateKey = key,
                    onTimeCount = WaqtType.entries.count { log.statusFor(it) == PrayerStatus.ON_TIME },
                    lateCount = WaqtType.entries.count { log.statusFor(it) == PrayerStatus.LATE },
                    qazaCount = WaqtType.entries.count { log.statusFor(it) == PrayerStatus.QAZA }
                )
            }
        }

        return Triple(streak, totalOnTime, last7)
    }

    companion object {
        private const val TAG = "NoorViewModel"
    }
}
