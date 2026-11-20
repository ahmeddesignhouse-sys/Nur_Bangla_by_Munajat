package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccentTheme
import com.example.data.model.AdhanAlertMode
import com.example.data.model.AsrMethod
import com.example.data.model.CalculationMethod
import com.example.data.model.ThemeMode
import com.example.data.model.WaqtAlertConfig
import com.example.data.model.WaqtType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.noorDataStore: DataStore<Preferences> by preferencesDataStore(name = "noor_bangla_prefs")

data class LocalAppSettings(
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
    val tasbihCycles: Int = 0
)

class PreferencesDataStore(context: Context) {
    private val dataStore = context.applicationContext.noorDataStore

    private object Keys {
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
    }

    val settingsFlow: Flow<LocalAppSettings> = dataStore.data.map { prefs ->
        LocalAppSettings(
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
                fajr = AdhanAlertMode.safeValueOf(prefs[Keys.FAJR_ALERT] ?: AdhanAlertMode.FULL_ADHAN.name),
                dhuhr = AdhanAlertMode.safeValueOf(prefs[Keys.DHUHR_ALERT] ?: AdhanAlertMode.BEEP.name),
                asr = AdhanAlertMode.safeValueOf(prefs[Keys.ASR_ALERT] ?: AdhanAlertMode.BEEP.name),
                maghrib = AdhanAlertMode.safeValueOf(prefs[Keys.MAGHRIB_ALERT] ?: AdhanAlertMode.FULL_ADHAN.name),
                isha = AdhanAlertMode.safeValueOf(prefs[Keys.ISHA_ALERT] ?: AdhanAlertMode.BEEP.name),
                prePrayerMinutes = (prefs[Keys.PRE_PRAYER_MIN] ?: 15).coerceIn(0, 45)
            ),
            tasbihDhikrId = prefs[Keys.TASBIH_DHIKR] ?: "subhanallah",
            tasbihTarget = (prefs[Keys.TASBIH_TARGET] ?: 33).coerceAtLeast(1),
            tasbihCount = (prefs[Keys.TASBIH_COUNT] ?: 0).coerceAtLeast(0),
            tasbihCycles = (prefs[Keys.TASBIH_CYCLES] ?: 0).coerceAtLeast(0)
        )
    }

    suspend fun setDistrict(districtId: String) {
        dataStore.edit {
            it[Keys.DISTRICT_ID] = districtId
            it[Keys.USE_GPS] = false
        }
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
        }
    }

    suspend fun setPrePrayerMinutes(minutes: Int) {
        dataStore.edit { it[Keys.PRE_PRAYER_MIN] = minutes.coerceIn(0, 45) }
    }

    suspend fun updateTasbih(dhikrId: String, target: Int, count: Int, cycles: Int) {
        dataStore.edit {
            it[Keys.TASBIH_DHIKR] = dhikrId
            it[Keys.TASBIH_TARGET] = target.coerceAtLeast(1)
            it[Keys.TASBIH_COUNT] = count.coerceAtLeast(0)
            it[Keys.TASBIH_CYCLES] = cycles.coerceAtLeast(0)
        }
    }

    suspend fun applyRemotePreferences(
        districtId: String,
        asrMethod: AsrMethod,
        themeMode: ThemeMode,
        accentTheme: AccentTheme,
        quranDarkMode: Boolean,
        quranFontSize: Int,
        prePrayerMinutes: Int
    ) {
        dataStore.edit {
            it[Keys.DISTRICT_ID] = districtId
            it[Keys.ASR_METHOD] = asrMethod.name
            it[Keys.THEME_MODE] = themeMode.name
            it[Keys.ACCENT_THEME] = accentTheme.name
            it[Keys.QURAN_DARK_MODE] = quranDarkMode
            it[Keys.QURAN_FONT_SIZE] = quranFontSize.coerceIn(14, 32)
            it[Keys.PRE_PRAYER_MIN] = prePrayerMinutes.coerceIn(0, 45)
        }
    }
}
