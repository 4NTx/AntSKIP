package com.artur.antskip.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.notificationTimeout = SERVICE_NOTIFICATION_TIMEOUT_MS
        info.flags = info.flags or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun lockScreen(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val eventPackage = event?.packageName?.toString().orEmpty()
        var provider = StreamingProvider.fromPackageName(eventPackage)

        val root = rootInActiveWindow
        if (provider == null && root != null) {
            val rootPackage = root.packageName?.toString().orEmpty()
            provider = StreamingProvider.fromPackageName(rootPackage)
        }

        if (provider == null) return

        if (!preferences.isAutomationEnabled) {
            logThrottled("event=${event?.eventType} provider=${provider.label} ignored=automation_disabled")
            return
        }
        if (!preferences.isProviderEnabled(provider)) {
            logThrottled("event=${event?.eventType} provider=${provider.label} ignored=provider_disabled")
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastClickAtMillis < CLICK_COOLDOWN_MS) return

        if (root == null) {
            logThrottled("event=${event?.eventType} provider=${provider.label} result=no_active_window")
            return
        }

        val match = matcher.findTarget(root, provider)
        if (match == null) {
            logThrottled("event=${event?.eventType} provider=${provider.label} result=no_match")
            return
        }

        val standardClicked = match.targets.any { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
        val gestureClicked = !standardClicked && canUseGestureFallback(match) && tapBounds(provider, match.action, match.gestureBounds)
        val success = standardClicked || gestureClicked

        val logLine = "event=${event?.eventType} provider=${provider.label} action=${match.action.name} " +
            "targets=${match.targets.size} clicked=$success gesture=$gestureClicked " +
            "visited=${match.visitedNodes} bounds=${match.gestureBounds.flattenToString()} text='${match.matchedText}'"

        if (match.targets.isEmpty() && !gestureClicked) {
            logThrottled(logLine)
        } else {
            preferences.appendDiagnosticLog(logLine)
        }

        if (success) {
            lastClickAtMillis = now
            showSkipToast(match.action)
        }
    }

    override fun onInterrupt() = Unit

    private fun showSkipToast(action: SkipAction) {
        val messageRes = when (action) {
            SkipAction.INTRO -> R.string.toast_skipping_intro
            SkipAction.RECAP -> R.string.toast_skipping_recap
            SkipAction.CREDITS -> R.string.toast_skipping_credits
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

    private fun canUseGestureFallback(match: SkipMatcher.MatchResult): Boolean =
        !match.gestureBounds.isEmpty && match.gestureBounds.width() > 10 && match.gestureBounds.height() > 10

    private fun tapBounds(provider: StreamingProvider, action: SkipAction, bounds: Rect): Boolean {
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(
            GestureDescription.StrokeDescription(
                tapPath(bounds.exactCenterX(), bounds.exactCenterY()),
                0L,
                GESTURE_TAP_DURATION_MS,
            ),
        )
        return dispatchGesture(gestureBuilder.build(), null, null)
    }

    private fun tapPath(x: Float, y: Float): Path =
        Path().apply { moveTo(x, y) }

    companion object {
        var instance: AntSkipAccessibilityService? = null
            private set

        private const val SERVICE_NOTIFICATION_TIMEOUT_MS = 100L
        private const val CLICK_COOLDOWN_MS = 2_000L
        private const val NO_MATCH_LOG_COOLDOWN_MS = 5_000L
        private const val GESTURE_TAP_DURATION_MS = 60L
    }
}
