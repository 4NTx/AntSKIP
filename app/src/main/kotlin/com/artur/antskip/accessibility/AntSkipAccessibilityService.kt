package com.artur.antskip.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.artur.antskip.R
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider
import com.artur.antskip.matcher.SkipMatcher

class AntSkipAccessibilityService : AccessibilityService() {
    private val preferences by lazy { PreferenceStore(this) }
    private val matcher by lazy { SkipMatcher(preferences) }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var lastClickAtMillis = 0L
    private var lastNoMatchLogAtMillis = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val provider = event?.packageName?.toString()?.let(StreamingProvider::fromPackageName) ?: return
        if (!preferences.isAutomationEnabled) {
            logThrottled("event=${event.eventType} provider=${provider.label} ignored=automation_disabled")
            return
        }
        if (!preferences.isProviderEnabled(provider)) {
            logThrottled("event=${event.eventType} provider=${provider.label} ignored=provider_disabled")
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastClickAtMillis < CLICK_COOLDOWN_MS) return

        val root = rootInActiveWindow ?: run {
            logThrottled("event=${event.eventType} provider=${provider.label} result=no_active_window")
            return
        }
        try {
            val match = matcher.findTarget(root, provider)
            if (match == null) {
                logThrottled("event=${event.eventType} provider=${provider.label} result=no_match")
                return
            }

            val clicked = match.targets.any { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
            preferences.appendDiagnosticLog(
                "event=${event.eventType} provider=${provider.label} action=${match.action.name} " +
                    "targets=${match.targets.size} clicked=$clicked visited=${match.visitedNodes} text='${match.matchedText}'",
            )
            if (clicked) {
                lastClickAtMillis = now
                showSkipToast(match.action)
            }
            match.targets.forEach { it.recycle() }
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() = Unit

    private fun showSkipToast(action: SkipAction) {
        val messageRes = when (action) {
            SkipAction.INTRO -> R.string.toast_skipping_intro
            SkipAction.RECAP -> R.string.toast_skipping_recap
            SkipAction.CREDITS -> R.string.toast_skipping_credits
            SkipAction.PREVIEW -> R.string.toast_skipping_preview
            SkipAction.NEXT_EPISODE -> R.string.toast_skipping_next_episode
        }
        mainHandler.post {
            Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()
        }
    }

    private fun logThrottled(message: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNoMatchLogAtMillis < NO_MATCH_LOG_COOLDOWN_MS) return
        lastNoMatchLogAtMillis = now
        preferences.appendDiagnosticLog(message)
    }

    private companion object {
        const val CLICK_COOLDOWN_MS = 3_000L
        const val NO_MATCH_LOG_COOLDOWN_MS = 5_000L
    }
}
