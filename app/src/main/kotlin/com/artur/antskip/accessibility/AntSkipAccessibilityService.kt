package com.artur.antskip.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
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
            val rootProvider = root.packageName?.toString()?.let(StreamingProvider::fromPackageName)
            if (rootProvider != provider) {
                logThrottled(
                    "event=${event.eventType} provider=${provider.label} ignored=active_window_package " +
                        "rootPackage=${root.packageName}",
                )
                return
            }

            val match = matcher.findTarget(root, provider)
            if (match == null) {
                logThrottled("event=${event.eventType} provider=${provider.label} result=no_match")
                return
            }

            val clicked = match.targets.any { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
            val gestureClicked = !clicked && canUseGestureFallback(provider, match) && tapBounds(match.gestureBounds)
            val logLine = "event=${event.eventType} provider=${provider.label} action=${match.action.name} " +
                "targets=${match.targets.size} clicked=${clicked || gestureClicked} gesture=$gestureClicked " +
                "visited=${match.visitedNodes} bounds=${match.gestureBounds.flattenToString()} text='${match.matchedText}'"
            if (match.targets.isEmpty()) {
                logThrottled(logLine)
            } else {
                preferences.appendDiagnosticLog(logLine)
            }
            if (clicked || gestureClicked) {
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

    private fun canUseGestureFallback(provider: StreamingProvider, match: SkipMatcher.MatchResult): Boolean =
        provider == StreamingProvider.CRUNCHYROLL &&
            match.action == SkipAction.NEXT_EPISODE &&
            match.targets.isEmpty() &&
            !match.gestureBounds.isEmpty

    private fun tapBounds(bounds: android.graphics.Rect): Boolean {
        val tapYAboveLabel = bounds.top - maxOf(bounds.height() * 4f, NEXT_EPISODE_ABOVE_LABEL_OFFSET_PX)
        val gestureBuilder = GestureDescription.Builder()
        if (tapYAboveLabel > 0f) {
            gestureBuilder.addStroke(
                GestureDescription.StrokeDescription(
                    tapPath(bounds.exactCenterX(), tapYAboveLabel),
                    0,
                    GESTURE_TAP_DURATION_MS,
                ),
            )
        }
        gestureBuilder.addStroke(
            GestureDescription.StrokeDescription(
                tapPath(bounds.exactCenterX(), bounds.exactCenterY()),
                GESTURE_SECOND_TAP_DELAY_MS,
                GESTURE_TAP_DURATION_MS,
            ),
        )
        val gesture = gestureBuilder.build()
        return dispatchGesture(gesture, null, null)
    }

    private fun tapPath(x: Float, y: Float): Path =
        Path().apply { moveTo(x, y) }

    private companion object {
        const val CLICK_COOLDOWN_MS = 3_000L
        const val NO_MATCH_LOG_COOLDOWN_MS = 5_000L
        const val GESTURE_TAP_DURATION_MS = 80L
        const val GESTURE_SECOND_TAP_DELAY_MS = 140L
        const val NEXT_EPISODE_ABOVE_LABEL_OFFSET_PX = 160f
    }
}
