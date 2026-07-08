package com.artur.antskip.data

import android.content.Context
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider

class PreferenceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

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
        preferences.getBoolean(providerActionKey(provider, action), isActionEnabled(action))

    fun setActionEnabledForProvider(provider: StreamingProvider, action: SkipAction, enabled: Boolean) {
        preferences.edit().putBoolean(providerActionKey(provider, action), enabled).apply()
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

    private companion object {
        const val PREFERENCES_NAME = "antskip"
        const val KEY_AUTOMATION_ENABLED = "automation_enabled"
        const val KEY_BLOCKED_PHRASES = "blocked_phrases"

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
