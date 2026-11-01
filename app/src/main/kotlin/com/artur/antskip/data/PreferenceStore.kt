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

    val sleepTimerDurationMinutes: Int
        get() = try {
            preferences.getInt(KEY_SLEEP_TIMER_DURATION_MINUTES, 15)
        } catch (_: Exception) {
            15
        }

    fun setSleepTimerDurationMinutes(minutes: Int) {
        preferences.edit().putInt(KEY_SLEEP_TIMER_DURATION_MINUTES, minutes.coerceAtLeast(1)).apply()
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
        const val KEY_SLEEP_TIMER_DURATION_MINUTES = "sleep_timer_duration_minutes"
        const val MAX_DIAGNOSTIC_LOG_LINES = 120

        val LOG_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
