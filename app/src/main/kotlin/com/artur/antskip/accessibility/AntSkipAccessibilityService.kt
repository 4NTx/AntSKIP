package com.artur.antskip.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
            if (match != null && (tapFirstValidBound(match.tapBounds) || clickFirstValidTarget(match.targets))) {
                lastClickAtMillis = now
            }
            match?.targets?.forEach { it.recycle() }
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() = Unit

    private fun tapFirstValidBound(bounds: List<Rect>): Boolean =
        bounds.firstOrNull { it.centerX() > 0 && it.centerY() > 0 }?.let { rect ->
            val path = Path().apply {
                moveTo(rect.centerX().toFloat(), rect.centerY().toFloat())
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS))
                .build()
            dispatchGesture(gesture, null, null)
        } == true

    private fun clickFirstValidTarget(targets: List<AccessibilityNodeInfo>): Boolean =
        targets.any { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }

    private companion object {
        const val CLICK_COOLDOWN_MS = 3_000L
        const val TAP_DURATION_MS = 80L
    }
}
