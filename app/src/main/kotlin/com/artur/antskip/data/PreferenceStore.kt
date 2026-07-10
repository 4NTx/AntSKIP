package com.artur.antskip.data

import android.content.Context
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PreferenceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        migrateCriticalDefaults()
    }

    val isAutomationEnabled: Boolean
        get() = preferences.getBoolean(KEY_AUTOMATION_ENABLED, true)

    fun setAutomationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTOMATION_ENABLED, enabled).apply()
    }

    fun isActionEnabled(action: SkipAction): Boolean =
        preferences.getBoolean(action.preferenceKey, action.enabledByDefault)

    fun setActionEnabled(action: SkipAction, enabled: Boolean) {
        preferences.edit().putBoolean(action.preferenceKey, enabled).apply()
    }

    fun isActionEnabledForProvider(provider: StreamingProvider, action: SkipAction): Boolean {
        val key = providerActionKey(provider, action)
        if (preferences.contains(key)) return preferences.getBoolean(key, provider.isActionEnabledByDefault(action))
        return isActionEnabled(action) && provider.isActionEnabledByDefault(action)
    }

    fun setActionEnabledForProvider(provider: StreamingProvider, action: SkipAction, enabled: Boolean) {
        preferences.edit().putBoolean(providerActionKey(provider, action), enabled).apply()
    }

    fun isNextEpisodeScheduleEnabled(provider: StreamingProvider): Boolean =
        preferences.getBoolean(nextEpisodeScheduleEnabledKey(provider), false)

    fun setNextEpisodeScheduleEnabled(provider: StreamingProvider, enabled: Boolean) {
        preferences.edit().putBoolean(nextEpisodeScheduleEnabledKey(provider), enabled).apply()
    }

    fun nextEpisodeScheduleStartMinutes(provider: StreamingProvider): Int =
        preferences.getInt(nextEpisodeScheduleStartKey(provider), DEFAULT_SLEEP_START_MINUTES)

    fun setNextEpisodeScheduleStartMinutes(provider: StreamingProvider, minutes: Int) {
        preferences.edit().putInt(nextEpisodeScheduleStartKey(provider), minutes.coerceIn(0, MINUTES_PER_DAY - 1)).apply()
    }

    fun nextEpisodeScheduleEndMinutes(provider: StreamingProvider): Int =
        preferences.getInt(nextEpisodeScheduleEndKey(provider), DEFAULT_SLEEP_END_MINUTES)

    fun setNextEpisodeScheduleEndMinutes(provider: StreamingProvider, minutes: Int) {
        preferences.edit().putInt(nextEpisodeScheduleEndKey(provider), minutes.coerceIn(0, MINUTES_PER_DAY - 1)).apply()
    }

    fun isNextEpisodeBlockedBySchedule(provider: StreamingProvider, now: LocalTime = LocalTime.now()): Boolean {
        if (!isNextEpisodeScheduleEnabled(provider)) return false
        val start = nextEpisodeScheduleStartMinutes(provider)
        val end = nextEpisodeScheduleEndMinutes(provider)
        val current = now.hour * 60 + now.minute
        return when {
            start == end -> true
            start < end -> current in start until end
            else -> current >= start || current < end
        }
    }

    fun nextEpisodePauseUntilMillis(provider: StreamingProvider): Long =
        preferences.getLong(nextEpisodePauseUntilKey(provider), 0L)

    fun setNextEpisodePauseUntilMillis(provider: StreamingProvider, untilMillis: Long) {
        preferences.edit().putLong(nextEpisodePauseUntilKey(provider), untilMillis).apply()
    }

    fun clearNextEpisodePause(provider: StreamingProvider) {
        preferences.edit().remove(nextEpisodePauseUntilKey(provider)).apply()
    }

    fun isNextEpisodeBlocked(provider: StreamingProvider, nowMillis: Long = System.currentTimeMillis()): Boolean =
        nextEpisodePauseUntilMillis(provider) > nowMillis || isNextEpisodeBlockedBySchedule(provider)

    fun isSleepProtectionActive(provider: StreamingProvider): Boolean =
        isNextEpisodeBlocked(provider)

    fun blocksCreditsDuringSleep(provider: StreamingProvider): Boolean =
        preferences.getBoolean(blockCreditsDuringSleepKey(provider), true)

    fun setBlocksCreditsDuringSleep(provider: StreamingProvider, enabled: Boolean) {
        preferences.edit().putBoolean(blockCreditsDuringSleepKey(provider), enabled).apply()
    }

    fun customPhrases(action: SkipAction): Set<String> =
        preferences.getStringSet(customPhraseKey(action), emptySet()).orEmpty()

    fun setCustomPhrases(action: SkipAction, phrases: Set<String>) {
        preferences.edit().putStringSet(customPhraseKey(action), phrases).apply()
    }

    fun blockedPhrases(): Set<String> =
        preferences.getStringSet(KEY_BLOCKED_PHRASES, DEFAULT_BLOCKED_PHRASES).orEmpty()

    fun setBlockedPhrases(phrases: Set<String>) {
        preferences.edit().putStringSet(KEY_BLOCKED_PHRASES, phrases).apply()
    }

    fun isProviderEnabled(provider: StreamingProvider): Boolean =
        preferences.getBoolean(provider.preferenceKey, provider.enabledByDefault)

    fun setProviderEnabled(provider: StreamingProvider, enabled: Boolean) {
        preferences.edit().putBoolean(provider.preferenceKey, enabled).apply()
    }

    fun diagnosticLogs(): String =
        preferences.getString(KEY_DIAGNOSTIC_LOGS, "").orEmpty()

    fun appendDiagnosticLog(message: String) {
        val line = "${LocalDateTime.now().format(LOG_TIME_FORMATTER)}  $message"
        val cutoff = LocalDateTime.now().minusDays(MAX_DIAGNOSTIC_LOG_AGE_DAYS)
        val retainedLines = (diagnosticLogs().lineSequence() + line)
            .filter { it.isNotBlank() }
            .filter { it.logTimestamp()?.isBefore(cutoff) != true }
            .toList()
            .takeLast(MAX_DIAGNOSTIC_LOG_LINES)
        val nextLogs = retainedLines.trimToMaxLogSize().joinToString("\n")
        preferences.edit().putString(KEY_DIAGNOSTIC_LOGS, nextLogs).apply()
    }

    fun clearDiagnosticLogs() {
        preferences.edit().remove(KEY_DIAGNOSTIC_LOGS).apply()
    }

    private fun customPhraseKey(action: SkipAction): String =
        "custom_phrases_${action.name.lowercase()}"

    private fun providerActionKey(provider: StreamingProvider, action: SkipAction): String =
        "provider_action_${provider.name.lowercase()}_${action.name.lowercase()}"

    private fun nextEpisodeScheduleEnabledKey(provider: StreamingProvider): String =
        "next_episode_schedule_enabled_${provider.name.lowercase()}"

    private fun nextEpisodeScheduleStartKey(provider: StreamingProvider): String =
        "next_episode_schedule_start_${provider.name.lowercase()}"

    private fun nextEpisodeScheduleEndKey(provider: StreamingProvider): String =
        "next_episode_schedule_end_${provider.name.lowercase()}"

    private fun nextEpisodePauseUntilKey(provider: StreamingProvider): String =
        "next_episode_pause_until_${provider.name.lowercase()}"

    private fun blockCreditsDuringSleepKey(provider: StreamingProvider): String =
        "block_credits_during_sleep_${provider.name.lowercase()}"

    private fun String.logTimestamp(): LocalDateTime? =
        take(LOG_TIMESTAMP_LENGTH).runCatching { LocalDateTime.parse(this, LOG_TIME_FORMATTER) }.getOrNull()

    private fun List<String>.trimToMaxLogSize(): List<String> {
        var retained = this
        while (
            retained.size > MIN_DIAGNOSTIC_LOG_LINES &&
            retained.joinToString("\n").length > MAX_DIAGNOSTIC_LOG_CHARS
        ) {
            retained = retained.drop(1)
        }
        return retained
    }

    private fun migrateCriticalDefaults() {
        val currentVersion = preferences.getInt(KEY_MIGRATION_VERSION, 0)
        if (currentVersion >= CURRENT_MIGRATION_VERSION) return

        val editor = preferences.edit()

        if (currentVersion < 3) {
            editor
                .putBoolean(KEY_AUTOMATION_ENABLED, true)
                .putBoolean(StreamingProvider.CRUNCHYROLL.preferenceKey, true)
                .putBoolean(StreamingProvider.NETFLIX.preferenceKey, true)
                .putBoolean(SkipAction.INTRO.preferenceKey, true)
                .putBoolean(providerActionKey(StreamingProvider.CRUNCHYROLL, SkipAction.INTRO), true)
                .putBoolean(providerActionKey(StreamingProvider.NETFLIX, SkipAction.INTRO), true)
        }

        val netflixNextEpisodeKey = providerActionKey(StreamingProvider.NETFLIX, SkipAction.NEXT_EPISODE)
        if (currentVersion < 4 && !preferences.contains(netflixNextEpisodeKey)) {
            editor.putBoolean(netflixNextEpisodeKey, false)
        }

        val netflixCreditsKey = providerActionKey(StreamingProvider.NETFLIX, SkipAction.CREDITS)
        if (currentVersion < 5 && !preferences.contains(netflixCreditsKey)) {
            editor.putBoolean(netflixCreditsKey, false)
        }

        if (currentVersion < 6) {
            editor
                .putBoolean(providerActionKey(StreamingProvider.NETFLIX, SkipAction.CREDITS), true)
                .putBoolean(providerActionKey(StreamingProvider.NETFLIX, SkipAction.NEXT_EPISODE), true)
        }

        editor
            .putInt(KEY_MIGRATION_VERSION, CURRENT_MIGRATION_VERSION)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "antskip"
        const val KEY_AUTOMATION_ENABLED = "automation_enabled"
        const val KEY_BLOCKED_PHRASES = "blocked_phrases"
        const val KEY_DIAGNOSTIC_LOGS = "diagnostic_logs"
        const val KEY_MIGRATION_VERSION = "migration_version"
        const val CURRENT_MIGRATION_VERSION = 6
        const val MINUTES_PER_DAY = 24 * 60
        const val DEFAULT_SLEEP_START_MINUTES = 23 * 60
        const val DEFAULT_SLEEP_END_MINUTES = 7 * 60
        const val MAX_DIAGNOSTIC_LOG_LINES = 180
        const val MIN_DIAGNOSTIC_LOG_LINES = 40
        const val MAX_DIAGNOSTIC_LOG_CHARS = 24_000
        const val MAX_DIAGNOSTIC_LOG_AGE_DAYS = 7L
        const val LOG_TIMESTAMP_LENGTH = 19

        val LOG_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val DEFAULT_BLOCKED_PHRASES = setOf(
            "assistir do inicio",
            "watch from beginning",
            "restart",
            "reiniciar",
            "trailer",
            "mais informacoes",
            "more info",
            "detalhes",
            "details",
        )
    }
}
