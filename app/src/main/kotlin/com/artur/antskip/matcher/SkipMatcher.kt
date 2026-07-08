package com.artur.antskip.matcher

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
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(AccessibilityNodeInfo.obtain(root))

        while (pending.isNotEmpty() && visited < MAX_VISITED_NODES) {
            val node = pending.removeFirst()
            visited++

            val nodeText = NodeText.from(node)
            val action = phraseBank.match(nodeText) ?: matchCustomPhrase(nodeText)
            if (action != null && preferences.isActionEnabledForProvider(provider, action)) {
                val clickable = node.nearestClickable()
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

    private fun matchCustomPhrase(text: String): SkipAction? =
        SkipAction.entries.firstOrNull { action ->
            preferences.customPhrases(action)
                .map { it.normalizeForMatch() }
                .filter { it.isNotBlank() }
                .any { phrase -> text == phrase || text.contains(phrase) }
        }

    private fun AccessibilityNodeInfo.nearestClickable(): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(this)
        while (current != null) {
            if (current.isEnabled && current.isVisibleToUser && current.isClickable) {
                return current
            }
            val parent = current.parent
            current.recycle()
            current = parent
        }
        return null
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
    }
}
