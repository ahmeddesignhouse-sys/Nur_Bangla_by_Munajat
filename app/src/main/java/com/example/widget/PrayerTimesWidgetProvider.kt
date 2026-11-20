package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.LocalAppSettings
import com.example.data.local.PreferencesDataStore
import com.example.data.model.BangladeshiDistricts
import com.example.util.BanglaUtils
import com.example.util.PrayerTimeCalculator
import com.example.util.isEnglishLang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

data class PrayerWidgetSnapshot(
    val locationHeader: String,
    val hijriDate: String,
    val nextLabel: String,
    val nextPrayerTitle: String,
    val countdownLabel: String,
    val remainingSeconds: Long,
    val sehriLabel: String,
    val sehriTime: String,
    val iftarLabel: String,
    val iftarTime: String,
    val nextRefreshDelayMillis: Long
)

class PrayerTimesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = PreferencesDataStore(context).settingsFlow.first()
                updateWidgetsNow(context, appWidgetManager, appWidgetIds, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == ACTION_WIDGET_REFRESH ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_LOCALE_CHANGED
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerTimesWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = PreferencesDataStore(context).settingsFlow.first()
                    updateWidgetsNow(context, manager, ids, settings)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelScheduledRefresh(context)
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.noorbangla.islam.ACTION_PRAYER_WIDGET_REFRESH"
        private const val WIDGET_ALARM_REQUEST_CODE = 505
        private const val WIDGET_CLICK_REQUEST_CODE = 506

        fun computeSnapshot(
            settings: LocalAppSettings,
            nowMillis: Long = System.currentTimeMillis()
        ): PrayerWidgetSnapshot {
            val isEn = isEnglishLang(settings.languageCode)
            val district = BangladeshiDistricts.findById(settings.districtId)
            val gpsLat = settings.gpsLatitude
            val gpsLon = settings.gpsLongitude
            val isManual = !settings.useGps || gpsLat == null || gpsLon == null
            val lat = if (!isManual) gpsLat!! else district.latitude
            val lon = if (!isManual) gpsLon!! else district.longitude

            val targetTz = PrayerTimeCalculator.resolveTimeZone(
                latitude = lat,
                longitude = lon,
                isManualDistrict = isManual,
                atMillis = nowMillis
            )
            val tzHours = targetTz.getOffset(nowMillis) / 3_600_000.0
            val tzLabel = PrayerTimeCalculator.formatTimeZoneLabel(targetTz, nowMillis, isEn)

            val now = Calendar.getInstance(targetTz).apply { timeInMillis = nowMillis }
            val todaySchedule = PrayerTimeCalculator.calculateForDate(
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

            val nextWaqt = PrayerTimeCalculator.computeNextWaqt(
                nowInTargetTz = now,
                todaySchedule = todaySchedule,
                tomorrowFajrMinute = tomorrowSchedule.fajrMinute
            )

            val appTitle = if (isEn) "Nur Bangla" else "নূর বাংলা"
            val placeName = when {
                isManual -> district.displayName(isEn)
                !settings.gpsLocationLabel.isNullOrBlank() -> settings.gpsLocationLabel
                PrayerTimeCalculator.isInsideBangladesh(lat, lon) -> district.displayName(isEn)
                else -> String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
            }

            val nextTimeFormatted = BanglaUtils.formatTime12h(nextWaqt.targetMinuteOfDay, isEn)
            val sehriFormatted = BanglaUtils.formatTime12h(todaySchedule.sehriEndMinute, isEn)
            val iftarFormatted = BanglaUtils.formatTime12h(todaySchedule.iftarMinute, isEn)
            val hijriFormatted = BanglaUtils.getHijriDate(now, isEn, settings.hijriDayOffset)

            // Refresh right after the next prayer boundary (plus 2s safety) or midnight, whichever comes first
            val nextPrayerDelayMs = (nextWaqt.remainingSeconds.coerceAtLeast(15L) + 2L) * 1000L
            val nextMidnight = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }
            val midnightDelayMs = (nextMidnight.timeInMillis - nowMillis).coerceAtLeast(60_000L)
            val refreshDelayMs = minOf(nextPrayerDelayMs, midnightDelayMs)

            return PrayerWidgetSnapshot(
                locationHeader = "$appTitle • $placeName",
                hijriDate = hijriFormatted,
                nextLabel = if (isEn) "Next Prayer" else "পরবর্তী ওয়াক্ত",
                nextPrayerTitle = "${nextWaqt.waqt.title(isEn)} • $nextTimeFormatted",
                countdownLabel = if (isEn) "In" else "বাকি",
                remainingSeconds = nextWaqt.remainingSeconds.coerceAtLeast(0L),
                sehriLabel = if (isEn) "Sehri" else "সাহরি",
                sehriTime = sehriFormatted,
                iftarLabel = if (isEn) "Iftar" else "ইফতার",
                iftarTime = iftarFormatted,
                nextRefreshDelayMillis = refreshDelayMs
            )
        }

        fun buildRemoteViews(
            context: Context,
            snapshot: PrayerWidgetSnapshot,
            elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime()
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)
            views.setTextViewText(R.id.widget_location_text, snapshot.locationHeader)
            views.setTextViewText(R.id.widget_hijri_date, snapshot.hijriDate)
            views.setTextViewText(R.id.widget_next_label, snapshot.nextLabel)
            views.setTextViewText(R.id.widget_next_prayer_title, snapshot.nextPrayerTitle)
            views.setTextViewText(R.id.widget_countdown_label, snapshot.countdownLabel)
            views.setTextViewText(R.id.widget_sehri_label, snapshot.sehriLabel)
            views.setTextViewText(R.id.widget_sehri_time, snapshot.sehriTime)
            views.setTextViewText(R.id.widget_iftar_label, snapshot.iftarLabel)
            views.setTextViewText(R.id.widget_iftar_time, snapshot.iftarTime)

            val targetElapsed = elapsedRealtimeMillis + (snapshot.remainingSeconds * 1000L)
            views.setChronometer(R.id.widget_countdown_chronometer, targetElapsed, "%s", true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                views.setChronometerCountDown(R.id.widget_countdown_chronometer, true)
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                WIDGET_CLICK_REQUEST_CODE,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, clickPendingIntent)
            return views
        }

        fun updateWidgetsNow(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            settings: LocalAppSettings
        ) {
            if (appWidgetIds.isEmpty()) return
            val nowMillis = System.currentTimeMillis()
            val snapshot = computeSnapshot(settings, nowMillis)
            val views = buildRemoteViews(context, snapshot)
            appWidgetIds.forEach { widgetId ->
                appWidgetManager.updateAppWidget(widgetId, views)
            }
            scheduleNextRefresh(context, nowMillis + snapshot.nextRefreshDelayMillis)
        }

        fun requestWidgetUpdate(context: Context, settings: LocalAppSettings? = null) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, PrayerTimesWidgetProvider::class.java))
            if (ids.isEmpty()) return
            if (settings != null) {
                updateWidgetsNow(appContext, manager, ids, settings)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val latest = PreferencesDataStore(appContext).settingsFlow.first()
                    updateWidgetsNow(appContext, manager, ids, latest)
                }
            }
        }

        fun isPinWidgetSupported(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            return try {
                AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
            } catch (_: Exception) {
                false
            }
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, PrayerTimesWidgetProvider::class.java)
            return try {
                manager.requestPinAppWidget(provider, null, null)
            } catch (_: Exception) {
                false
            }
        }

        private fun scheduleNextRefresh(context: Context, triggerAtMillis: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PrayerTimesWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            val pi = PendingIntent.getBroadcast(
                context,
                WIDGET_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
                if (canExact) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, triggerAtMillis, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, triggerAtMillis, pi)
                }
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, triggerAtMillis, pi)
            }
        }

        private fun cancelScheduledRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PrayerTimesWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            val pi = PendingIntent.getBroadcast(
                context,
                WIDGET_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                alarmManager.cancel(pi)
            }
        }
    }
}
