package com.artur.antskip.matcher

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider

class SkipMatcher(
    private val preferences: PreferenceStore,
    private val phraseBank: SkipPhraseBank = SkipPhraseBank,
) {
    data class MatchResult(
        val target: AccessibilityNodeInfo,
        val action: SkipAction,
    )

    fun findTarget(root: AccessibilityNodeInfo, provider: StreamingProvider): MatchResult? {
        var visited = 0
        val rootBounds = Rect().also(root::getBoundsInScreen)
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(AccessibilityNodeInfo.obtain(root))

        while (pending.isNotEmpty() && visited < MAX_VISITED_NODES) {
            val node = pending.removeFirst()
            visited++

            val nodeText = if (provider == StreamingProvider.CRUNCHYROLL) {
                NodeText.visibleFrom(node)
            } else {
                NodeText.from(node)
            }
            val action = matchAction(nodeText, provider)
            if (action != null && isActionAllowed(action, provider)) {
                val clickable = node.nearestClickable(provider, rootBounds)
                node.recycle()
                pending.recycleAll()
                return clickable?.let { MatchResult(it, action) }
            }

            addChildren(node, pending)
            node.recycle()
        }

        pending.recycleAll()
        return null
    }

    private fun matchAction(text: String, provider: StreamingProvider): SkipAction? =
        if (provider == StreamingProvider.CRUNCHYROLL) {
            matchCrunchyrollIntro(text)
        } else {
            phraseBank.match(text) ?: matchCustomPhrase(text)
        }

    private fun isActionAllowed(action: SkipAction, provider: StreamingProvider): Boolean =
        if (provider == StreamingProvider.CRUNCHYROLL) {
            preferences.isActionEnabled(action)
        } else {
            preferences.isActionEnabledForProvider(provider, action)
        }

    private fun matchCrunchyrollIntro(text: String): SkipAction? =
        SkipAction.INTRO.takeIf {
            CRUNCHYROLL_INTRO_PHRASES.any { phrase -> text == phrase || text.contains(phrase) }
        }

    private fun matchCustomPhrase(text: String): SkipAction? =
        SkipAction.entries.firstOrNull { action ->
            preferences.customPhrases(action)
                .map { it.normalizeForMatch() }
                .filter { it.isNotBlank() }
                .any { phrase -> text == phrase || text.contains(phrase) }
        }

    private fun AccessibilityNodeInfo.nearestClickable(
        provider: StreamingProvider,
        rootBounds: Rect,
    ): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(this)
        while (current != null) {
            if (current.isEnabled &&
                current.isVisibleToUser &&
                current.isClickable &&
                current.isValidClickTarget(provider, rootBounds)
            ) {
                return current
            }
            val parent = current.parent
            current.recycle()
            current = parent
        }
        return null
    }

    private fun AccessibilityNodeInfo.isValidClickTarget(provider: StreamingProvider, rootBounds: Rect): Boolean {
        if (provider != StreamingProvider.CRUNCHYROLL) return true
        val bounds = Rect()
        getBoundsInScreen(bounds)
        if (bounds.isEmpty || rootBounds.isEmpty) return false
        return bounds.width() <= rootBounds.width() * MAX_CRUNCHYROLL_BUTTON_WIDTH_RATIO &&
            bounds.height() <= rootBounds.height() * MAX_CRUNCHYROLL_BUTTON_HEIGHT_RATIO
    }

    private fun addChildren(node: AccessibilityNodeInfo, pending: ArrayDeque<AccessibilityNodeInfo>) {
        repeat(node.childCount) { index ->
            node.getChild(index)?.let { pending.add(it) }
        }
    }

    private fun ArrayDeque<AccessibilityNodeInfo>.recycleAll() {
        while (isNotEmpty()) {
            removeFirst().recycle()
        }
    }

    private companion object {
        const val MAX_VISITED_NODES = 300
        const val MAX_CRUNCHYROLL_BUTTON_WIDTH_RATIO = 0.9f
        const val MAX_CRUNCHYROLL_BUTTON_HEIGHT_RATIO = 0.25f

        val CRUNCHYROLL_INTRO_PHRASES = setOf(
            "pular abertura",
            "pular introducao",
            "pular a introducao",
            "skip intro",
            "skip opening",
        )
    }
}
