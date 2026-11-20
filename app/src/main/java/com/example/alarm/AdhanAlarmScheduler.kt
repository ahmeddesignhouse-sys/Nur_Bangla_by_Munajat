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
import com.example.data.model.BangladeshiDistricts
import com.example.data.model.WaqtType
import com.example.util.PrayerTimeCalculator
import java.util.Calendar

object AdhanAlarmScheduler {
    const val CHANNEL_ID = "adhan_alerts_channel"
    const val EXTRA_WAQT_KEY = "extra_waqt_key"
    const val EXTRA_ALERT_MODE = "extra_alert_mode"
    const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
    const val EXTRA_PRE_MINUTES = "extra_pre_minutes"

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
                    // Sound/vibration handled directly by AdhanAlarmReceiver based on per-Waqt preference
                    setSound(null, null)
                    enableVibration(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleUpcomingAlarms(context: Context, settings: LocalAppSettings) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val district = BangladeshiDistricts.findById(settings.districtId)
        val isManual = !settings.useGps || settings.gpsLatitude == null || settings.gpsLongitude == null
        val lat = if (!isManual) settings.gpsLatitude!! else district.latitude
        val lon = if (!isManual) settings.gpsLongitude!! else district.longitude

        val nowEpoch = System.currentTimeMillis()
        val targetTz = PrayerTimeCalculator.resolveTimeZone(lat, lon, isManualDistrict = isManual, atMillis = nowEpoch)
        val tzHours = targetTz.getOffset(nowEpoch) / 3_600_000.0
        val tzLabel = PrayerTimeCalculator.formatTimeZoneLabel(targetTz, nowEpoch)

        val now = Calendar.getInstance(targetTz)
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

        val preMinutes = settings.alertConfig.prePrayerMinutes

        WaqtType.entries.forEachIndexed { index, waqt ->
            val mode = settings.alertConfig.modeFor(waqt)
            val mainRequestCode = 100 + index
            val preRequestCode = 200 + index

            cancelAlarm(context, alarmManager, mainRequestCode)
            cancelAlarm(context, alarmManager, preRequestCode)

            if (mode == AdhanAlertMode.SILENT) return@forEachIndexed

            val todayTarget = triggerCalendar(now, todaySchedule.minuteFor(waqt))
            val mainTrigger = if (todayTarget.after(now)) {
                todayTarget
            } else {
                triggerCalendar(tomorrow, tomorrowSchedule.minuteFor(waqt))
            }

            scheduleSingleAlarm(
                context = context,
                alarmManager = alarmManager,
                requestCode = mainRequestCode,
                triggerAtMillis = mainTrigger.timeInMillis,
                waqt = waqt,
                mode = mode,
                isPreReminder = false,
                preMinutes = 0
            )

            if (preMinutes > 0) {
                val preTriggerMillis = mainTrigger.timeInMillis - (preMinutes * 60_000L)
                if (preTriggerMillis > System.currentTimeMillis()) {
                    scheduleSingleAlarm(
                        context = context,
                        alarmManager = alarmManager,
                        requestCode = preRequestCode,
                        triggerAtMillis = preTriggerMillis,
                        waqt = waqt,
                        mode = AdhanAlertMode.BEEP,
                        isPreReminder = true,
                        preMinutes = preMinutes
                    )
                }
            }
        }
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
        isPreReminder: Boolean,
        preMinutes: Int
    ) {
        val intent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra(EXTRA_WAQT_KEY, waqt.key)
            putExtra(EXTRA_ALERT_MODE, mode.name)
            putExtra(EXTRA_IS_PRE_REMINDER, isPreReminder)
            putExtra(EXTRA_PRE_MINUTES, preMinutes)
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
