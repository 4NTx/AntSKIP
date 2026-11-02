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
import com.artur.antskip.tile.AutoSkipTileService

class AntSkipAccessibilityService : AccessibilityService() {
    private val preferences by lazy { PreferenceStore(this) }
    private val matcher by lazy { SkipMatcher(preferences) }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var lastClickAtMillis = 0L

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
        preferences.appendDiagnosticLog("Serviço de acessibilidade ativo e pronto")
        AutoSkipTileService.requestUpdate(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        preferences.appendDiagnosticLog("Serviço de acessibilidade desativado")
        AutoSkipTileService.requestUpdate(this)
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

        // PERFORMANCE: If event is from another app, return immediately without touching IPC!
        if (provider == null) {
            if (eventPackage.isNotBlank()) return
            val root = rootInActiveWindow ?: return
            val rootPackage = root.packageName?.toString().orEmpty()
            provider = StreamingProvider.fromPackageName(rootPackage) ?: return
        }

        if (!preferences.isAutomationEnabled || !preferences.isProviderEnabled(provider)) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastClickAtMillis < CLICK_COOLDOWN_MS) return

        val root = rootInActiveWindow ?: return

        val match = matcher.findTarget(root, provider) ?: return

        val standardClicked = match.targets.any { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
        val gestureClicked = !standardClicked && canUseGestureFallback(match) && tapBounds(provider, match.action, match.gestureBounds)
        val success = standardClicked || gestureClicked

        if (success) {
            lastClickAtMillis = now
            preferences.appendDiagnosticLog("PULOU ${match.action.label} no ${provider.label} ('${match.matchedText}')")
            showSkipToast(match.action)
        } else if (match.targets.isNotEmpty() || gestureClicked) {
            preferences.appendDiagnosticLog("Falha ao tocar em: ${match.action.label} no ${provider.label} ('${match.matchedText}')")
        }
    }

    override fun onInterrupt() = Unit

    private fun showSkipToast(action: SkipAction) {
        if (!preferences.isToastNotificationEnabled) return
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
        private const val GESTURE_TAP_DURATION_MS = 60L
    }
}
