package com.artur.antskip.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
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
            val match = matcher.findTarget(root)
            if (match?.target?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true) {
                lastClickAtMillis = now
                showSkipToast(match.action)
            }
            match?.target?.recycle()
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() = Unit

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
    }
}
