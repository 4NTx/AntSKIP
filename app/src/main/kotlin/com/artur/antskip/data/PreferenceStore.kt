package com.artur.antskip.data

import android.content.Context
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PreferenceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val isAutomationEnabled: Boolean
        get() = try {
            preferences.getBoolean(KEY_AUTOMATION_ENABLED, true)
        } catch (_: Exception) {
            true
        }

    fun setAutomationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTOMATION_ENABLED, enabled).apply()
    }

    fun isActionEnabled(action: SkipAction): Boolean =
        try {
            preferences.getBoolean(action.preferenceKey, action.enabledByDefault)
        } catch (_: Exception) {
            action.enabledByDefault
        }

    fun setActionEnabled(action: SkipAction, enabled: Boolean) {
        preferences.edit().putBoolean(action.preferenceKey, enabled).apply()
    }

    fun isProviderEnabled(provider: StreamingProvider): Boolean =
        try {
            preferences.getBoolean(provider.preferenceKey, provider.enabledByDefault)
        } catch (_: Exception) {
            provider.enabledByDefault
        }

    fun setProviderEnabled(provider: StreamingProvider, enabled: Boolean) {
        preferences.edit().putBoolean(provider.preferenceKey, enabled).apply()
    }

    // Sleep Timer (Xitee1/sleep-timer style) settings
    val isSleepTimerPauseMediaEnabled: Boolean
        get() = try {
            preferences.getBoolean(KEY_SLEEP_TIMER_PAUSE_MEDIA, true)
        } catch (_: Exception) {
            true
        }

    fun setSleepTimerPauseMediaEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SLEEP_TIMER_PAUSE_MEDIA, enabled).apply()
    }

    val isSleepTimerTurnOffScreenEnabled: Boolean
        get() = try {
            preferences.getBoolean(KEY_SLEEP_TIMER_TURN_OFF_SCREEN, true)
        } catch (_: Exception) {
            true
        }

    fun setSleepTimerTurnOffScreenEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SLEEP_TIMER_TURN_OFF_SCREEN, enabled).apply()
    }

    val isSleepTimerAudioFadeOutEnabled: Boolean
        get() = try {
            preferences.getBoolean(KEY_SLEEP_TIMER_AUDIO_FADE_OUT, true)
        } catch (_: Exception) {
            true
        }

    fun setSleepTimerAudioFadeOutEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SLEEP_TIMER_AUDIO_FADE_OUT, enabled).apply()
    }

    val isSleepTimerShakeToExtendEnabled: Boolean
        get() = try {
            preferences.getBoolean(KEY_SLEEP_TIMER_SHAKE_TO_EXTEND, false)
        } catch (_: Exception) {
            false
        }

    fun setSleepTimerShakeToExtendEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SLEEP_TIMER_SHAKE_TO_EXTEND, enabled).apply()
    }

    val sleepTimerDurationMinutes: Int
        get() = try {
            preferences.getInt(KEY_SLEEP_TIMER_DURATION_MINUTES, 15)
        } catch (_: Exception) {
            15
        }

    fun setSleepTimerDurationMinutes(minutes: Int) {
        preferences.edit().putInt(KEY_SLEEP_TIMER_DURATION_MINUTES, minutes.coerceAtLeast(1)).apply()
    }

    // Stealth Mode: Toast Notifications
    val isToastNotificationEnabled: Boolean
        get() = try {
            preferences.getBoolean(KEY_TOAST_NOTIFICATION_ENABLED, true)
        } catch (_: Exception) {
            true
        }

    fun setToastNotificationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_TOAST_NOTIFICATION_ENABLED, enabled).apply()
    }

    // Schedule System (Agendamento Automático por Horário / Dias)
    val isScheduleEnabled: Boolean
        get() = try {
            preferences.getBoolean(KEY_SCHEDULE_ENABLED, false)
        } catch (_: Exception) {
            false
        }

    fun setScheduleEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply()
    }

    val schedulePreset: String
        get() = try {
            preferences.getString(KEY_SCHEDULE_PRESET, "ALWAYS") ?: "ALWAYS"
        } catch (_: Exception) {
            "ALWAYS"
        }

    fun setSchedulePreset(preset: String) {
        preferences.edit().putString(KEY_SCHEDULE_PRESET, preset).apply()
    }

    val scheduleStartHour: Int
        get() = try {
            preferences.getInt(KEY_SCHEDULE_START_HOUR, 22)
        } catch (_: Exception) {
            22
        }

    fun setScheduleStartHour(hour: Int) {
        preferences.edit().putInt(KEY_SCHEDULE_START_HOUR, hour.coerceIn(0, 23)).apply()
    }

    val scheduleStartMinute: Int
        get() = try {
            preferences.getInt(KEY_SCHEDULE_START_MINUTE, 0)
        } catch (_: Exception) {
            0
        }

    fun setScheduleStartMinute(minute: Int) {
        preferences.edit().putInt(KEY_SCHEDULE_START_MINUTE, minute.coerceIn(0, 59)).apply()
    }

    val scheduleEndHour: Int
        get() = try {
            preferences.getInt(KEY_SCHEDULE_END_HOUR, 6)
        } catch (_: Exception) {
            6
        }

    fun setScheduleEndHour(hour: Int) {
        preferences.edit().putInt(KEY_SCHEDULE_END_HOUR, hour.coerceIn(0, 23)).apply()
    }

    val scheduleEndMinute: Int
        get() = try {
            preferences.getInt(KEY_SCHEDULE_END_MINUTE, 0)
        } catch (_: Exception) {
            0
        }

    fun setScheduleEndMinute(minute: Int) {
        preferences.edit().putInt(KEY_SCHEDULE_END_MINUTE, minute.coerceIn(0, 59)).apply()
    }

    val scheduleDays: Set<Int>
        get() = try {
            val raw = preferences.getStringSet(KEY_SCHEDULE_DAYS, null)
            val parsed = raw?.mapNotNull { it.toIntOrNull() }?.toSet()
            if (parsed.isNullOrEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else parsed
        } catch (_: Exception) {
            setOf(1, 2, 3, 4, 5, 6, 7)
        }

    fun setScheduleDays(days: Set<Int>) {
        val safeDays = if (days.isEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else days
        val raw = safeDays.map { it.toString() }.toSet()
        preferences.edit().putStringSet(KEY_SCHEDULE_DAYS, raw).apply()
    }

    fun isScheduleActiveNow(): Boolean {
        if (!isScheduleEnabled) return true
        val now = java.util.Calendar.getInstance()
        val currentDay = now.get(java.util.Calendar.DAY_OF_WEEK)
        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(java.util.Calendar.MINUTE)
        val currentMinutes = currentHour * 60 + currentMinute

        val startMinutes = scheduleStartHour * 60 + scheduleStartMinute
        val endMinutes = scheduleEndHour * 60 + scheduleEndMinute
        val days = scheduleDays

        return if (startMinutes <= endMinutes) {
            currentDay in days && currentMinutes in startMinutes..endMinutes
        } else {
            // Overnight span (e.g. 22:00 to 06:00)
            if (currentMinutes >= startMinutes) {
                currentDay in days
            } else if (currentMinutes <= endMinutes) {
                val previousDay = if (currentDay == java.util.Calendar.SUNDAY) java.util.Calendar.SATURDAY else currentDay - 1
                previousDay in days
            } else {
                false
            }
        }
    }

    // Sleep Routine (Agendamento Automático do Temporizador de Sono)
    val isSleepRoutineEnabled: Boolean
        get() = try {
            preferences.getBoolean(KEY_SLEEP_ROUTINE_ENABLED, false)
        } catch (_: Exception) {
            false
        }

    fun setSleepRoutineEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SLEEP_ROUTINE_ENABLED, enabled).apply()
    }

    val sleepRoutinePreset: String
        get() = try {
            preferences.getString(KEY_SLEEP_ROUTINE_PRESET, "EVERYDAY") ?: "EVERYDAY"
        } catch (_: Exception) {
            "EVERYDAY"
        }

    fun setSleepRoutinePreset(preset: String) {
        preferences.edit().putString(KEY_SLEEP_ROUTINE_PRESET, preset).apply()
    }

    val sleepRoutineHour: Int
        get() = try {
            preferences.getInt(KEY_SLEEP_ROUTINE_HOUR, 23)
        } catch (_: Exception) {
            23
        }

    fun setSleepRoutineHour(hour: Int) {
        preferences.edit().putInt(KEY_SLEEP_ROUTINE_HOUR, hour.coerceIn(0, 23)).apply()
    }

    val sleepRoutineMinute: Int
        get() = try {
            preferences.getInt(KEY_SLEEP_ROUTINE_MINUTE, 30)
        } catch (_: Exception) {
            30
        }

    fun setSleepRoutineMinute(minute: Int) {
        preferences.edit().putInt(KEY_SLEEP_ROUTINE_MINUTE, minute.coerceIn(0, 59)).apply()
    }

    val sleepRoutineDays: Set<Int>
        get() = try {
            val raw = preferences.getStringSet(KEY_SLEEP_ROUTINE_DAYS, null)
            val parsed = raw?.mapNotNull { it.toIntOrNull() }?.toSet()
            if (parsed.isNullOrEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else parsed
        } catch (_: Exception) {
            setOf(1, 2, 3, 4, 5, 6, 7)
        }

    fun setSleepRoutineDays(days: Set<Int>) {
        val safeDays = if (days.isEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else days
        val raw = safeDays.map { it.toString() }.toSet()
        preferences.edit().putStringSet(KEY_SLEEP_ROUTINE_DAYS, raw).apply()
    }

    fun diagnosticLogs(): String = DiagnosticLogger.getLogs()

    fun appendDiagnosticLog(message: String) {
        DiagnosticLogger.log(message)
    }

    fun clearDiagnosticLogs() {
        DiagnosticLogger.clear()
    }

    private companion object {
        const val PREFERENCES_NAME = "antskip"
        const val KEY_AUTOMATION_ENABLED = "automation_enabled"
        const val KEY_DIAGNOSTIC_LOGS = "diagnostic_logs"
        const val KEY_SLEEP_TIMER_PAUSE_MEDIA = "sleep_timer_pause_media"
        const val KEY_SLEEP_TIMER_TURN_OFF_SCREEN = "sleep_timer_turn_off_screen"
        const val KEY_SLEEP_TIMER_AUDIO_FADE_OUT = "sleep_timer_audio_fade_out"
        const val KEY_SLEEP_TIMER_SHAKE_TO_EXTEND = "sleep_timer_shake_to_extend"
        const val KEY_SLEEP_TIMER_DURATION_MINUTES = "sleep_timer_duration_minutes"
        const val KEY_TOAST_NOTIFICATION_ENABLED = "toast_notification_enabled"
        const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
        const val KEY_SCHEDULE_PRESET = "schedule_preset"
        const val KEY_SCHEDULE_START_HOUR = "schedule_start_hour"
        const val KEY_SCHEDULE_START_MINUTE = "schedule_start_minute"
        const val KEY_SCHEDULE_END_HOUR = "schedule_end_hour"
        const val KEY_SCHEDULE_END_MINUTE = "schedule_end_minute"
        const val KEY_SCHEDULE_DAYS = "schedule_days"
        const val KEY_SLEEP_ROUTINE_ENABLED = "sleep_routine_enabled"
        const val KEY_SLEEP_ROUTINE_PRESET = "sleep_routine_preset"
        const val KEY_SLEEP_ROUTINE_HOUR = "sleep_routine_hour"
        const val KEY_SLEEP_ROUTINE_MINUTE = "sleep_routine_minute"
        const val KEY_SLEEP_ROUTINE_DAYS = "sleep_routine_days"
        const val MAX_DIAGNOSTIC_LOG_LINES = 120

        val LOG_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
