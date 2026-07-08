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
        val targets: List<AccessibilityNodeInfo>,
        val tapBounds: List<Rect>,
        val action: SkipAction,
    )

    fun findTarget(root: AccessibilityNodeInfo, provider: StreamingProvider): MatchResult? {
        var visited = 0
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(AccessibilityNodeInfo.obtain(root))

        while (pending.isNotEmpty() && visited < MAX_VISITED_NODES) {
            val node = pending.removeFirst()
            visited++

            val nodeText = NodeText.from(node)
            if (isBlocked(nodeText)) {
                addChildren(node, pending)
            } else {
                val action = phraseBank.match(nodeText) ?: matchCustomPhrase(nodeText)
                if (action != null && preferences.isActionEnabledForProvider(provider, action)) {
                    val targets = node.clickCandidates()
                    val bounds = node.tapBounds()
                    node.recycle()
                    pending.recycleAll()
                    return if (targets.isNotEmpty() || bounds.isNotEmpty()) {
                        MatchResult(targets, bounds, action)
                    } else {
                        null
                    }
                }

                addChildren(node, pending)
            }

            node.recycle()
        }

        pending.recycleAll()
        return null
    }

    private fun matchCustomPhrase(text: String): SkipAction? =
        SkipAction.entries.firstOrNull { action ->
            preferences.customPhrases(action)
                .map { it.normalizeForMatch() }
                .filter { it.isNotBlank() }
                .any { phrase -> text == phrase || text.contains(phrase) }
        }

    private fun isBlocked(text: String): Boolean =
        preferences.blockedPhrases()
            .map { it.normalizeForMatch() }
            .filter { it.isNotBlank() }
            .any { phrase -> text == phrase || text.contains(phrase) }

    private fun AccessibilityNodeInfo.clickCandidates(): List<AccessibilityNodeInfo> {
        val clickableTargets = mutableListOf<AccessibilityNodeInfo>()
        val actionTargets = mutableListOf<AccessibilityNodeInfo>()
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(this)
        while (current != null) {
            if (current.isEnabled && current.isVisibleToUser) {
                when {
                    current.isClickable -> clickableTargets.add(AccessibilityNodeInfo.obtain(current))
                    current.hasClickAction() -> actionTargets.add(AccessibilityNodeInfo.obtain(current))
                }
            }
            val parent = current.parent
            current.recycle()
            current = parent
        }
        return clickableTargets + actionTargets
    }

    private fun AccessibilityNodeInfo.tapBounds(): List<Rect> {
        val bounds = mutableListOf<Rect>()
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(this)
        while (current != null) {
            if (current.isEnabled && current.isVisibleToUser) {
                val rect = Rect()
                current.getBoundsInScreen(rect)
                if (!rect.isEmpty && rect.width() >= MIN_TAP_SIZE && rect.height() >= MIN_TAP_SIZE) {
                    bounds.add(rect)
                }
            }
            val parent = current.parent
            current.recycle()
            current = parent
        }
        return bounds.distinctBy { "${it.left},${it.top},${it.right},${it.bottom}" }
    }

    private fun AccessibilityNodeInfo.hasClickAction(): Boolean =
        isClickable || actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }

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
        const val MIN_TAP_SIZE = 12
    }
}
