package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccentTheme
import com.example.data.model.AdhanAlertMode
import com.example.data.model.AdhanVoice
import com.example.data.model.AsrMethod
import com.example.data.model.CalculationMethod
import com.example.data.model.PerPrayerAlertSetting
import com.example.data.model.ThemeMode
import com.example.data.model.WaqtAlertConfig
import com.example.data.model.WaqtType
import com.example.util.QuranReciter
import com.example.util.ZakatCalculator
import com.example.util.ZakatHistoryRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.noorDataStore: DataStore<Preferences> by preferencesDataStore(name = "noor_bangla_prefs")

data class LocalAppSettings(
    val languageCode: String = "bn",
    val districtId: String = "dhaka",
    val useGps: Boolean = false,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsLocationLabel: String? = null,
    val asrMethod: AsrMethod = AsrMethod.HANAFI,
    val calculationMethod: CalculationMethod = CalculationMethod.AUTO,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentTheme: AccentTheme = AccentTheme.ISLAMIC_GREEN,
    val quranDarkMode: Boolean = false,
    val quranFontSize: Int = 20,
    val alertConfig: WaqtAlertConfig = WaqtAlertConfig(),
    val tasbihDhikrId: String = "subhanallah",
    val tasbihTarget: Int = 33,
    val tasbihCount: Int = 0,
    val tasbihCycles: Int = 0,
    val hijriDayOffset: Int = 0,
    val favoriteDuaIds: Set<String> = emptySet(),
    val ramadanMode: Boolean = false,
    val goldPricePerGramBdt: Double = ZakatCalculator.DEFAULT_GOLD_PRICE_PER_GRAM_BDT,
    val silverPricePerGramBdt: Double = ZakatCalculator.DEFAULT_SILVER_PRICE_PER_GRAM_BDT,
    val zakatHistoryJson: String = "[]",
    val quranReciterId: String = QuranReciter.ALAFASY.id,
    val dailyQuranGoal: Int = 10,
    val dailyQuranDateKey: String = "",
    val dailyQuranReadAyahKeys: Set<String> = emptySet(),
    val hajjCompletedStepIds: Set<String> = emptySet(),
    val isGuestMode: Boolean = false,
    val guestUserId: String = ""
) {
    fun todayReadAyahKeys(todayDateKey: String): Set<String> =
        if (dailyQuranDateKey == todayDateKey) dailyQuranReadAyahKeys else emptySet()

    fun todayReadAyahCount(todayDateKey: String): Int =
        todayReadAyahKeys(todayDateKey).size

    companion object {
        const val GUEST_ID_PREFIX = "guest-"

        fun isGuestUserId(userId: String): Boolean =
            userId.startsWith(GUEST_ID_PREFIX)
    }
}

class PreferencesDataStore(context: Context) {
    private val dataStore = context.applicationContext.noorDataStore

    private object Keys {
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val DISTRICT_ID = stringPreferencesKey("district_id")
        val USE_GPS = booleanPreferencesKey("use_gps")
        val GPS_LAT = doublePreferencesKey("gps_lat")
        val GPS_LON = doublePreferencesKey("gps_lon")
        val GPS_NAME = stringPreferencesKey("gps_name")
        val ASR_METHOD = stringPreferencesKey("asr_method")
        val CALC_METHOD = stringPreferencesKey("calc_method")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val QURAN_DARK_MODE = booleanPreferencesKey("quran_dark_mode")
        val QURAN_FONT_SIZE = intPreferencesKey("quran_font_size")
        val FAJR_ALERT = stringPreferencesKey("fajr_alert")
        val DHUHR_ALERT = stringPreferencesKey("dhuhr_alert")
        val ASR_ALERT = stringPreferencesKey("asr_alert")
        val MAGHRIB_ALERT = stringPreferencesKey("maghrib_alert")
        val ISHA_ALERT = stringPreferencesKey("isha_alert")
        val PRE_PRAYER_MIN = intPreferencesKey("pre_prayer_min")
        val TASBIH_DHIKR = stringPreferencesKey("tasbih_dhikr")
        val TASBIH_TARGET = intPreferencesKey("tasbih_target")
        val TASBIH_COUNT = intPreferencesKey("tasbih_count")
        val TASBIH_CYCLES = intPreferencesKey("tasbih_cycles")
        val HIJRI_DAY_OFFSET = intPreferencesKey("hijri_day_offset")
        val FAVORITE_DUA_IDS = stringSetPreferencesKey("favorite_dua_ids")
        val RAMADAN_MODE = booleanPreferencesKey("ramadan_mode")
        val GOLD_PRICE_GRAM_BDT = doublePreferencesKey("gold_price_gram_bdt")
        val SILVER_PRICE_GRAM_BDT = doublePreferencesKey("silver_price_gram_bdt")
        val ZAKAT_HISTORY_JSON = stringPreferencesKey("zakat_history_json")
        val QURAN_RECITER_ID = stringPreferencesKey("quran_reciter_id")
        val DAILY_QURAN_GOAL = intPreferencesKey("daily_quran_goal")
        val DAILY_QURAN_DATE_KEY = stringPreferencesKey("daily_quran_date_key")
        val DAILY_QURAN_READ_KEYS = stringSetPreferencesKey("daily_quran_read_keys")
        val HAJJ_COMPLETED_STEPS = stringSetPreferencesKey("hajj_completed_steps")
        val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
        val GUEST_USER_ID = stringPreferencesKey("guest_user_id")

        fun waqtEnabledKey(waqt: WaqtType) = booleanPreferencesKey("waqt_${waqt.key}_enabled")
        fun waqtVoiceKey(waqt: WaqtType) = stringPreferencesKey("waqt_${waqt.key}_voice")
        fun waqtReminderKey(waqt: WaqtType) = intPreferencesKey("waqt_${waqt.key}_reminder_min")
    }

    val settingsFlow: Flow<LocalAppSettings> = dataStore.data.map { prefs ->
        val rawLang = prefs[Keys.LANGUAGE_CODE] ?: "bn"
        val normalizedLang = if (rawLang == "en") "en" else "bn"
        val legacyPreMin = (prefs[Keys.PRE_PRAYER_MIN] ?: 15).coerceIn(0, 45)
        fun readWaqtSetting(waqt: WaqtType, legacyKey: Preferences.Key<String>, defaultVoice: AdhanVoice, defaultLegacy: AdhanAlertMode): PerPrayerAlertSetting {
            val legacyMode = AdhanAlertMode.safeValueOf(prefs[legacyKey] ?: defaultLegacy.name)
            val enabled = prefs[Keys.waqtEnabledKey(waqt)] ?: (legacyMode != AdhanAlertMode.SILENT)
            val voice = prefs[Keys.waqtVoiceKey(waqt)]?.let { AdhanVoice.safeValueOf(it) }
                ?: if (prefs[legacyKey] != null) AdhanVoice.fromLegacyMode(legacyMode) else defaultVoice
            val reminder = (prefs[Keys.waqtReminderKey(waqt)] ?: legacyPreMin).coerceIn(0, 45)
            return PerPrayerAlertSetting(enabled = enabled, voice = voice, reminderMinutesBefore = reminder)
        }
        val fajrSetting = readWaqtSetting(WaqtType.FAJR, Keys.FAJR_ALERT, AdhanVoice.MAKKAH, AdhanAlertMode.FULL_ADHAN)
        val dhuhrSetting = readWaqtSetting(WaqtType.DHUHR, Keys.DHUHR_ALERT, AdhanVoice.MADINAH, AdhanAlertMode.FULL_ADHAN)
        val asrSetting = readWaqtSetting(WaqtType.ASR, Keys.ASR_ALERT, AdhanVoice.AL_AQSA, AdhanAlertMode.FULL_ADHAN)
        val maghribSetting = readWaqtSetting(WaqtType.MAGHRIB, Keys.MAGHRIB_ALERT, AdhanVoice.MAKKAH, AdhanAlertMode.FULL_ADHAN)
        val ishaSetting = readWaqtSetting(WaqtType.ISHA, Keys.ISHA_ALERT, AdhanVoice.MADINAH, AdhanAlertMode.FULL_ADHAN)

        LocalAppSettings(
            languageCode = normalizedLang,
            districtId = prefs[Keys.DISTRICT_ID] ?: "dhaka",
            useGps = prefs[Keys.USE_GPS] ?: false,
            gpsLatitude = prefs[Keys.GPS_LAT],
            gpsLongitude = prefs[Keys.GPS_LON],
            gpsLocationLabel = prefs[Keys.GPS_NAME],
            asrMethod = AsrMethod.safeValueOf(prefs[Keys.ASR_METHOD] ?: AsrMethod.HANAFI.name),
            calculationMethod = CalculationMethod.safeValueOf(prefs[Keys.CALC_METHOD] ?: CalculationMethod.AUTO.name),
            themeMode = ThemeMode.safeValueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name),
            accentTheme = AccentTheme.safeValueOf(prefs[Keys.ACCENT_THEME] ?: AccentTheme.ISLAMIC_GREEN.name),
            quranDarkMode = prefs[Keys.QURAN_DARK_MODE] ?: false,
            quranFontSize = (prefs[Keys.QURAN_FONT_SIZE] ?: 20).coerceIn(14, 32),
            alertConfig = WaqtAlertConfig(
                fajr = fajrSetting.legacyAlertMode,
                dhuhr = dhuhrSetting.legacyAlertMode,
                asr = asrSetting.legacyAlertMode,
                maghrib = maghribSetting.legacyAlertMode,
                isha = ishaSetting.legacyAlertMode,
                prePrayerMinutes = legacyPreMin,
                fajrSetting = fajrSetting,
                dhuhrSetting = dhuhrSetting,
                asrSetting = asrSetting,
                maghribSetting = maghribSetting,
                ishaSetting = ishaSetting
            ),
            tasbihDhikrId = prefs[Keys.TASBIH_DHIKR] ?: "subhanallah",
            tasbihTarget = (prefs[Keys.TASBIH_TARGET] ?: 33).coerceAtLeast(1),
            tasbihCount = (prefs[Keys.TASBIH_COUNT] ?: 0).coerceAtLeast(0),
            tasbihCycles = (prefs[Keys.TASBIH_CYCLES] ?: 0).coerceAtLeast(0),
            hijriDayOffset = (prefs[Keys.HIJRI_DAY_OFFSET] ?: 0).coerceIn(-1, 1),
            favoriteDuaIds = prefs[Keys.FAVORITE_DUA_IDS] ?: emptySet(),
            ramadanMode = prefs[Keys.RAMADAN_MODE] ?: false,
            goldPricePerGramBdt = (prefs[Keys.GOLD_PRICE_GRAM_BDT] ?: ZakatCalculator.DEFAULT_GOLD_PRICE_PER_GRAM_BDT).coerceAtLeast(0.0),
            silverPricePerGramBdt = (prefs[Keys.SILVER_PRICE_GRAM_BDT] ?: ZakatCalculator.DEFAULT_SILVER_PRICE_PER_GRAM_BDT).coerceAtLeast(0.0),
            zakatHistoryJson = prefs[Keys.ZAKAT_HISTORY_JSON] ?: "[]",
            quranReciterId = QuranReciter.fromId(prefs[Keys.QURAN_RECITER_ID]).id,
            dailyQuranGoal = (prefs[Keys.DAILY_QURAN_GOAL] ?: 10).coerceIn(1, 200),
            dailyQuranDateKey = prefs[Keys.DAILY_QURAN_DATE_KEY] ?: "",
            dailyQuranReadAyahKeys = prefs[Keys.DAILY_QURAN_READ_KEYS] ?: emptySet(),
            hajjCompletedStepIds = prefs[Keys.HAJJ_COMPLETED_STEPS] ?: emptySet(),
            isGuestMode = prefs[Keys.IS_GUEST_MODE] ?: false,
            guestUserId = prefs[Keys.GUEST_USER_ID] ?: ""
        )
    }

    /**
     * Activates local Guest Mode and ensures a persistent local guest UUID ("guest-<uuid>").
     * Does not call Firebase Auth or require internet connectivity.
     */
    suspend fun enableGuestMode(): String {
        var resolvedId = ""
        dataStore.edit { prefs ->
            val existing = prefs[Keys.GUEST_USER_ID].orEmpty()
            resolvedId = if (existing.startsWith(LocalAppSettings.GUEST_ID_PREFIX) && existing.length > LocalAppSettings.GUEST_ID_PREFIX.length) {
                existing
            } else {
                "${LocalAppSettings.GUEST_ID_PREFIX}${UUID.randomUUID()}"
            }
            prefs[Keys.GUEST_USER_ID] = resolvedId
            prefs[Keys.IS_GUEST_MODE] = true
        }
        return resolvedId
    }

    suspend fun setGuestModeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_GUEST_MODE] = enabled
        }
    }

    suspend fun setLanguageCode(code: String) {
        val normalized = if (code == "en") "en" else "bn"
        dataStore.edit { it[Keys.LANGUAGE_CODE] = normalized }
    }

    suspend fun setDistrict(districtId: String) {
        dataStore.edit {
            it[Keys.DISTRICT_ID] = districtId
            it[Keys.USE_GPS] = false
        }
    }

    suspend fun setUseGps(enabled: Boolean) {
        dataStore.edit { it[Keys.USE_GPS] = enabled }
    }

    suspend fun setGpsLocation(lat: Double, lon: Double, nearestDistrictId: String, label: String) {
        dataStore.edit {
            it[Keys.USE_GPS] = true
            it[Keys.GPS_LAT] = lat
            it[Keys.GPS_LON] = lon
            it[Keys.DISTRICT_ID] = nearestDistrictId
            it[Keys.GPS_NAME] = label
        }
    }

    suspend fun setAsrMethod(method: AsrMethod) {
        dataStore.edit { it[Keys.ASR_METHOD] = method.name }
    }

    suspend fun setCalculationMethod(method: CalculationMethod) {
        dataStore.edit { it[Keys.CALC_METHOD] = method.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentTheme(accent: AccentTheme) {
        dataStore.edit { it[Keys.ACCENT_THEME] = accent.name }
    }

    suspend fun setQuranDarkMode(enabled: Boolean) {
        dataStore.edit { it[Keys.QURAN_DARK_MODE] = enabled }
    }

    suspend fun setQuranFontSize(size: Int) {
        dataStore.edit { it[Keys.QURAN_FONT_SIZE] = size.coerceIn(14, 32) }
    }

    suspend fun setWaqtAlertMode(waqt: WaqtType, mode: AdhanAlertMode) {
        dataStore.edit {
            when (waqt) {
                WaqtType.FAJR -> it[Keys.FAJR_ALERT] = mode.name
                WaqtType.DHUHR -> it[Keys.DHUHR_ALERT] = mode.name
                WaqtType.ASR -> it[Keys.ASR_ALERT] = mode.name
                WaqtType.MAGHRIB -> it[Keys.MAGHRIB_ALERT] = mode.name
                WaqtType.ISHA -> it[Keys.ISHA_ALERT] = mode.name
            }
            it[Keys.waqtEnabledKey(waqt)] = (mode != AdhanAlertMode.SILENT)
            if (mode != AdhanAlertMode.SILENT) {
                it[Keys.waqtVoiceKey(waqt)] = AdhanVoice.fromLegacyMode(mode).name
            }
        }
    }

    suspend fun setWaqtEnabled(waqt: WaqtType, enabled: Boolean) {
        dataStore.edit {
            it[Keys.waqtEnabledKey(waqt)] = enabled
        }
    }

    suspend fun setWaqtAdhanVoice(waqt: WaqtType, voice: AdhanVoice) {
        dataStore.edit {
            it[Keys.waqtVoiceKey(waqt)] = voice.name
            it[Keys.waqtEnabledKey(waqt)] = true
        }
    }

    suspend fun setWaqtReminderMinutes(waqt: WaqtType, minutes: Int) {
        dataStore.edit {
            it[Keys.waqtReminderKey(waqt)] = minutes.coerceIn(0, 45)
        }
    }

    suspend fun setPrePrayerMinutes(minutes: Int) {
        val safe = minutes.coerceIn(0, 45)
        dataStore.edit { prefs ->
            prefs[Keys.PRE_PRAYER_MIN] = safe
            WaqtType.entries.forEach { w ->
                prefs[Keys.waqtReminderKey(w)] = safe
            }
        }
    }

    suspend fun updateTasbih(dhikrId: String, target: Int, count: Int, cycles: Int) {
        dataStore.edit {
            it[Keys.TASBIH_DHIKR] = dhikrId
            it[Keys.TASBIH_TARGET] = target.coerceAtLeast(1)
            it[Keys.TASBIH_COUNT] = count.coerceAtLeast(0)
            it[Keys.TASBIH_CYCLES] = cycles.coerceAtLeast(0)
        }
    }

    suspend fun setHijriDayOffset(offset: Int) {
        dataStore.edit { it[Keys.HIJRI_DAY_OFFSET] = offset.coerceIn(-1, 1) }
    }

    suspend fun toggleFavoriteDua(duaId: String): Boolean {
        var added = false
        dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_DUA_IDS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(duaId)) {
                current.remove(duaId)
                added = false
            } else {
                current.add(duaId)
                added = true
            }
            prefs[Keys.FAVORITE_DUA_IDS] = current
        }
        return added
    }

    suspend fun setRamadanMode(enabled: Boolean) {
        dataStore.edit { it[Keys.RAMADAN_MODE] = enabled }
    }

    suspend fun setMetalPrices(goldPerGramBdt: Double, silverPerGramBdt: Double) {
        dataStore.edit {
            it[Keys.GOLD_PRICE_GRAM_BDT] = goldPerGramBdt.coerceAtLeast(0.0)
            it[Keys.SILVER_PRICE_GRAM_BDT] = silverPerGramBdt.coerceAtLeast(0.0)
        }
    }

    suspend fun addZakatHistoryRecord(record: ZakatHistoryRecord) {
        dataStore.edit { prefs ->
            val current = ZakatCalculator.parseHistoryJson(prefs[Keys.ZAKAT_HISTORY_JSON] ?: "[]")
            val updated = listOf(record) + current
            prefs[Keys.ZAKAT_HISTORY_JSON] = ZakatCalculator.serializeHistoryJson(updated)
        }
    }

    suspend fun deleteZakatHistoryRecord(recordId: String) {
        dataStore.edit { prefs ->
            val current = ZakatCalculator.parseHistoryJson(prefs[Keys.ZAKAT_HISTORY_JSON] ?: "[]")
            val updated = current.filterNot { it.id == recordId }
            prefs[Keys.ZAKAT_HISTORY_JSON] = ZakatCalculator.serializeHistoryJson(updated)
        }
    }

    suspend fun setQuranReciter(reciterId: String) {
        val valid = QuranReciter.fromId(reciterId).id
        dataStore.edit { it[Keys.QURAN_RECITER_ID] = valid }
    }

    suspend fun setDailyQuranGoal(goalAyahs: Int) {
        dataStore.edit { it[Keys.DAILY_QURAN_GOAL] = goalAyahs.coerceIn(1, 200) }
    }

    suspend fun recordQuranAyahProgress(todayDateKey: String, surahNumber: Int, ayahNumber: Int) {
        val ayahKey = "${surahNumber}_$ayahNumber"
        dataStore.edit { prefs ->
            val savedDate = prefs[Keys.DAILY_QURAN_DATE_KEY] ?: ""
            val currentSet = if (savedDate == todayDateKey) {
                prefs[Keys.DAILY_QURAN_READ_KEYS]?.toMutableSet() ?: mutableSetOf()
            } else {
                mutableSetOf()
            }
            currentSet.add(ayahKey)
            prefs[Keys.DAILY_QURAN_DATE_KEY] = todayDateKey
            prefs[Keys.DAILY_QURAN_READ_KEYS] = currentSet
        }
    }

    suspend fun toggleQuranAyahRead(todayDateKey: String, surahNumber: Int, ayahNumber: Int): Boolean {
        val ayahKey = "${surahNumber}_$ayahNumber"
        var marked = false
        dataStore.edit { prefs ->
            val savedDate = prefs[Keys.DAILY_QURAN_DATE_KEY] ?: ""
            val currentSet = if (savedDate == todayDateKey) {
                prefs[Keys.DAILY_QURAN_READ_KEYS]?.toMutableSet() ?: mutableSetOf()
            } else {
                mutableSetOf()
            }
            if (currentSet.contains(ayahKey)) {
                currentSet.remove(ayahKey)
                marked = false
            } else {
                currentSet.add(ayahKey)
                marked = true
            }
            prefs[Keys.DAILY_QURAN_DATE_KEY] = todayDateKey
            prefs[Keys.DAILY_QURAN_READ_KEYS] = currentSet
        }
        return marked
    }

    suspend fun toggleHajjStepCompleted(stepId: String): Boolean {
        var completed = false
        dataStore.edit { prefs ->
            val current = prefs[Keys.HAJJ_COMPLETED_STEPS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(stepId)) {
                current.remove(stepId)
                completed = false
            } else {
                current.add(stepId)
                completed = true
            }
            prefs[Keys.HAJJ_COMPLETED_STEPS] = current
        }
        return completed
    }

    suspend fun resetHajjChecklist() {
        dataStore.edit { prefs ->
            prefs[Keys.HAJJ_COMPLETED_STEPS] = emptySet()
        }
    }

    suspend fun applyRemotePreferences(
        districtId: String,
        asrMethod: AsrMethod,
        themeMode: ThemeMode,
        accentTheme: AccentTheme,
        quranDarkMode: Boolean,
        quranFontSize: Int,
        prePrayerMinutes: Int,
        languageCode: String? = null
    ) {
        dataStore.edit {
            it[Keys.DISTRICT_ID] = districtId
            it[Keys.ASR_METHOD] = asrMethod.name
            it[Keys.THEME_MODE] = themeMode.name
            it[Keys.ACCENT_THEME] = accentTheme.name
            it[Keys.QURAN_DARK_MODE] = quranDarkMode
            it[Keys.QURAN_FONT_SIZE] = quranFontSize.coerceIn(14, 32)
            it[Keys.PRE_PRAYER_MIN] = prePrayerMinutes.coerceIn(0, 45)
            if (languageCode == "bn" || languageCode == "en") {
                it[Keys.LANGUAGE_CODE] = languageCode
            }
        }
    }
}
