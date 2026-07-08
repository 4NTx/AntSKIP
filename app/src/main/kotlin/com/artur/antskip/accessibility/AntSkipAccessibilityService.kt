package com.artur.antskip.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider
import com.artur.antskip.matcher.SkipMatcher

class AntSkipAccessibilityService : AccessibilityService() {
    private val preferences by lazy { PreferenceStore(this) }
    private val matcher by lazy { SkipMatcher(preferences) }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var lastClickAtMillis = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val provider = event?.packageName?.toString()?.let(StreamingProvider::fromPackageName) ?: return
        if (!preferences.isAutomationEnabled || !preferences.isProviderEnabled(provider)) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastClickAtMillis < CLICK_COOLDOWN_MS) return

        val root = rootInActiveWindow ?: return
        try {
            val match = matcher.findTarget(root, provider)
            if (match != null && performSkip(match, provider)) {
                lastClickAtMillis = now
                showSkipToast(match.action)
            }
            match?.targets?.forEach { it.recycle() }
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() = Unit

    private fun performSkip(match: SkipMatcher.MatchResult, provider: StreamingProvider): Boolean =
        if (provider == StreamingProvider.CRUNCHYROLL && match.action == SkipAction.INTRO) {
            tapFirstValidBound(match.tapBounds) || clickFirstValidTarget(match.targets)
        } else {
            clickFirstValidTarget(match.targets) || tapFirstValidBound(match.tapBounds)
        }

    private fun tapFirstValidBound(bounds: List<Rect>): Boolean {
        val metrics = resources.displayMetrics
        val maxTapWidth = (metrics.widthPixels * MAX_TAP_WIDTH_RATIO).toInt()
        val maxTapHeight = (metrics.heightPixels * MAX_TAP_HEIGHT_RATIO).toInt()
        val tapTarget = bounds
            .filter { rect ->
                rect.centerX() > 0 &&
                    rect.centerY() > 0 &&
                    rect.width() <= maxTapWidth &&
                    rect.height() <= maxTapHeight
            }
            .minByOrNull { rect -> rect.width().toLong() * rect.height().toLong() }

        return tapTarget?.let { rect ->
            val path = Path().apply {
                moveTo(rect.centerX().toFloat(), rect.centerY().toFloat())
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS))
                .build()
            dispatchGesture(gesture, null, null)
        } == true
    }

    private fun clickFirstValidTarget(targets: List<AccessibilityNodeInfo>): Boolean =
        targets.any { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }

    private fun showSkipToast(action: SkipAction) {
        val message = when (action) {
            SkipAction.INTRO -> "Pulando abertura"
            SkipAction.RECAP -> "Pulando resumo"
            SkipAction.CREDITS -> "Pulando creditos"
            SkipAction.PREVIEW -> "Pulando previa"
            SkipAction.NEXT_EPISODE -> "Pulando para o proximo episodio"
        }
        mainHandler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val CLICK_COOLDOWN_MS = 3_000L
        const val TAP_DURATION_MS = 80L
        const val MAX_TAP_WIDTH_RATIO = 0.75f
        const val MAX_TAP_HEIGHT_RATIO = 0.35f
    }
}
