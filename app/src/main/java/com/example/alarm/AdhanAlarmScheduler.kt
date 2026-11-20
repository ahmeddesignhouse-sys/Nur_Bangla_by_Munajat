package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.R
import com.example.data.local.LocalAppSettings
import com.example.data.model.AdhanAlertMode
import com.example.data.model.AdhanVoice
import com.example.data.model.BangladeshiDistricts
import com.example.data.model.WaqtType
import com.example.util.PrayerTimeCalculator
import com.example.util.isEnglishLang
import java.util.Calendar

data class ScheduledPrayerAlarm(
    val waqt: WaqtType,
    val triggerAtMillis: Long,
    val voice: AdhanVoice,
    val isPreReminder: Boolean,
    val preMinutes: Int
)

object AdhanAlarmScheduler {
    const val CHANNEL_ID = "adhan_alerts_channel"
    const val EXTRA_WAQT_KEY = "extra_waqt_key"
    const val EXTRA_ALERT_MODE = "extra_alert_mode"
    const val EXTRA_ADHAN_VOICE = "extra_adhan_voice"
    const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
    const val EXTRA_PRE_MINUTES = "extra_pre_minutes"
    const val EXTRA_LANGUAGE_CODE = "extra_language_code"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.adhan_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.adhan_channel_desc)
                    setSound(null, null)
                    enableVibration(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Pure timezone-aware alarm planner using the app's Auto Sync timezone & location logic.
     * Any prayer with enabled == false produces zero alarms (neither main nor pre-reminder).
     */
    fun planUpcomingAlarms(
        settings: LocalAppSettings,
        nowEpoch: Long = System.currentTimeMillis()
    ): List<ScheduledPrayerAlarm> {
        val district = BangladeshiDistricts.findById(settings.districtId)
        val gpsLat = settings.gpsLatitude
        val gpsLon = settings.gpsLongitude
        val isManual = !settings.useGps || gpsLat == null || gpsLon == null
        val lat = if (!isManual) gpsLat else district.latitude
        val lon = if (!isManual) gpsLon else district.longitude

        val isEnglish = isEnglishLang(settings.languageCode)
        val targetTz = PrayerTimeCalculator.resolveTimeZone(lat, lon, isManualDistrict = isManual, atMillis = nowEpoch)
        val tzHours = targetTz.getOffset(nowEpoch) / 3_600_000.0
        val tzLabel = PrayerTimeCalculator.formatTimeZoneLabel(targetTz, nowEpoch, isEnglish)

        val now = Calendar.getInstance(targetTz).apply { timeInMillis = nowEpoch }
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

        val planned = mutableListOf<ScheduledPrayerAlarm>()
        WaqtType.entries.forEach { waqt ->
            val setting = settings.alertConfig.settingFor(waqt)
            if (!setting.enabled) return@forEach

            val todayTarget = triggerCalendar(now, todaySchedule.minuteFor(waqt))
            val mainTrigger = if (todayTarget.after(now)) {
                todayTarget
            } else {
                triggerCalendar(tomorrow, tomorrowSchedule.minuteFor(waqt))
            }

            planned.add(
                ScheduledPrayerAlarm(
                    waqt = waqt,
                    triggerAtMillis = mainTrigger.timeInMillis,
                    voice = setting.voice,
                    isPreReminder = false,
                    preMinutes = 0
                )
            )

            val preMin = setting.reminderMinutesBefore
            if (preMin > 0) {
                val preTriggerMillis = mainTrigger.timeInMillis - (preMin * 60_000L)
                if (preTriggerMillis > nowEpoch) {
                    planned.add(
                        ScheduledPrayerAlarm(
                            waqt = waqt,
                            triggerAtMillis = preTriggerMillis,
                            voice = if (setting.voice == AdhanVoice.SILENT_VIBRATE) {
                                AdhanVoice.SILENT_VIBRATE
                            } else {
                                AdhanVoice.SHORT_BEEP
                            },
                            isPreReminder = true,
                            preMinutes = preMin
                        )
                    )
                }
            }
        }
        return planned
    }

    fun scheduleUpcomingAlarms(context: Context, settings: LocalAppSettings) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Always cancel all existing alarms first so turning a prayer OFF guarantees zero alerts
        WaqtType.entries.forEachIndexed { index, _ ->
            cancelAlarm(context, alarmManager, 100 + index)
            cancelAlarm(context, alarmManager, 200 + index)
        }

        val planned = planUpcomingAlarms(settings)
        planned.forEach { item ->
            val requestCode = (if (item.isPreReminder) 200 else 100) + item.waqt.ordinal
            scheduleSingleAlarm(
                context = context,
                alarmManager = alarmManager,
                requestCode = requestCode,
                triggerAtMillis = item.triggerAtMillis,
                waqt = item.waqt,
                mode = item.voice.toLegacyAlertMode(enabled = true),
                voice = item.voice,
                isPreReminder = item.isPreReminder,
                preMinutes = item.preMinutes,
                languageCode = settings.languageCode
            )
        }
        com.example.widget.PrayerTimesWidgetProvider.requestWidgetUpdate(context, settings)
    }

    private fun triggerCalendar(baseDate: Calendar, minuteOfDay: Int): Calendar {
        return (baseDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun scheduleSingleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        triggerAtMillis: Long,
        waqt: WaqtType,
        mode: AdhanAlertMode,
        voice: AdhanVoice,
        isPreReminder: Boolean,
        preMinutes: Int,
        languageCode: String
    ) {
        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra(EXTRA_WAQT_KEY, waqt.key)
            putExtra(EXTRA_ALERT_MODE, mode.name)
            putExtra(EXTRA_ADHAN_VOICE, voice.name)
            putExtra(EXTRA_IS_PRE_REMINDER, isPreReminder)
            putExtra(EXTRA_PRE_MINUTES, preMinutes)
            putExtra(EXTRA_LANGUAGE_CODE, languageCode)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, AdhanAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
        }
    }
}
