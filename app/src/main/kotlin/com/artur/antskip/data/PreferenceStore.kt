package com.artur.antskip.data

import android.content.Context
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider
import java.time.LocalTime

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

    fun isActionEnabledForProvider(provider: StreamingProvider, action: SkipAction): Boolean =
        preferences.getBoolean(providerActionKey(provider, action), provider.isActionEnabledByDefault(action))

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

    private fun migrateCriticalDefaults() {
        if (preferences.getInt(KEY_MIGRATION_VERSION, 0) >= CURRENT_MIGRATION_VERSION) return

        preferences.edit()
            .putBoolean(KEY_AUTOMATION_ENABLED, true)
            .putBoolean(StreamingProvider.CRUNCHYROLL.preferenceKey, true)
            .putBoolean(StreamingProvider.NETFLIX.preferenceKey, true)
            .putBoolean(SkipAction.INTRO.preferenceKey, true)
            .putBoolean(providerActionKey(StreamingProvider.CRUNCHYROLL, SkipAction.INTRO), true)
            .putBoolean(providerActionKey(StreamingProvider.NETFLIX, SkipAction.INTRO), true)
            .putInt(KEY_MIGRATION_VERSION, CURRENT_MIGRATION_VERSION)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "antskip"
        const val KEY_AUTOMATION_ENABLED = "automation_enabled"
        const val KEY_BLOCKED_PHRASES = "blocked_phrases"
        const val KEY_MIGRATION_VERSION = "migration_version"
        const val CURRENT_MIGRATION_VERSION = 3
        const val MINUTES_PER_DAY = 24 * 60
        const val DEFAULT_SLEEP_START_MINUTES = 23 * 60
        const val DEFAULT_SLEEP_END_MINUTES = 7 * 60

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
