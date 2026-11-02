package com.artur.antskip.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.service.SleepTimerService
import java.util.Calendar

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val preferences = PreferenceStore(context)
        val action = intent?.action

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            ScheduleManager.syncSleepRoutineAlarm(context, preferences)
            return
        }

        if (action == ScheduleManager.ACTION_TRIGGER_SLEEP_ROUTINE) {
            ScheduleManager.syncSleepRoutineAlarm(context, preferences)

            if (!preferences.isSleepRoutineEnabled) return

            val now = Calendar.getInstance()
            val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek !in preferences.sleepRoutineDays) return

            if (!SleepTimerService.isRunning) {
                val durationSec = preferences.sleepTimerDurationMinutes * 60L
                val startIntent = Intent(context, SleepTimerService::class.java).apply {
                    this.action = SleepTimerService.ACTION_START
                    putExtra(SleepTimerService.EXTRA_DURATION_SECONDS, durationSec)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(startIntent)
                } else {
                    context.startService(startIntent)
                }
            }
        }
    }
}
