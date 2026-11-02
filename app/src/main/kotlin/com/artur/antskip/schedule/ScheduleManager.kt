package com.artur.antskip.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.artur.antskip.data.PreferenceStore
import java.util.Calendar

object ScheduleManager {
    const val ACTION_TRIGGER_SLEEP_ROUTINE = "com.artur.antskip.ACTION_TRIGGER_SLEEP_ROUTINE"
    private const val REQUEST_CODE_SLEEP_ROUTINE = 5050

    fun syncSleepRoutineAlarm(context: Context, preferences: PreferenceStore) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_SLEEP_ROUTINE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SLEEP_ROUTINE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (!preferences.isSleepRoutineEnabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, preferences.sleepRoutineHour)
            set(Calendar.MINUTE, preferences.sleepRoutineMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 1000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetCalendar.timeInMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    targetCalendar.timeInMillis,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetCalendar.timeInMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    targetCalendar.timeInMillis,
                    pendingIntent,
                )
            }
        } catch (_: Exception) {
        }
    }
}
