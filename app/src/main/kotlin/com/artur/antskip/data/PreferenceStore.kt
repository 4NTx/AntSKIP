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

    fun customPhrases(action: SkipAction): Set<String> =
        preferences.getStringSet(customPhraseKey(action), emptySet()).orEmpty()

    fun setCustomPhrases(action: SkipAction, phrases: Set<String>) {
        preferences.edit().putStringSet(customPhraseKey(action), phrases).apply()
    }

    fun isProviderEnabled(provider: StreamingProvider): Boolean =
        preferences.getBoolean(provider.preferenceKey, provider.enabledByDefault)

    fun setProviderEnabled(provider: StreamingProvider, enabled: Boolean) {
        preferences.edit().putBoolean(provider.preferenceKey, enabled).apply()
    }

    private fun customPhraseKey(action: SkipAction): String =
        "custom_phrases_${action.name.lowercase()}"

    private companion object {
        const val PREFERENCES_NAME = "antskip"
        const val KEY_AUTOMATION_ENABLED = "automation_enabled"
    }
}
