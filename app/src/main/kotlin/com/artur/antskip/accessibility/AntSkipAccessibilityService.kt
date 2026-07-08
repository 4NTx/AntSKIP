package com.artur.antskip.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.StreamingProvider
import com.artur.antskip.matcher.SkipMatcher

class AntSkipAccessibilityService : AccessibilityService() {
    private val preferences by lazy { PreferenceStore(this) }
    private val matcher by lazy { SkipMatcher(preferences) }
    private var lastClickAtMillis = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val provider = event?.packageName?.toString()?.let(StreamingProvider::fromPackageName) ?: return
        if (!preferences.isAutomationEnabled || !preferences.isProviderEnabled(provider)) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastClickAtMillis < CLICK_COOLDOWN_MS) return

        val root = rootInActiveWindow ?: return
        try {
            val match = matcher.findTarget(root, provider)
            if (match?.targets?.any { it.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) } == true) {
                lastClickAtMillis = now
            }
            match?.targets?.forEach { it.recycle() }
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() = Unit

    private companion object {
        const val CLICK_COOLDOWN_MS = 3_000L
    }
}
