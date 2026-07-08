package com.artur.antskip.matcher

import android.view.accessibility.AccessibilityNodeInfo
import com.artur.antskip.data.PreferenceStore

class SkipMatcher(
    private val preferences: PreferenceStore,
    private val phraseBank: SkipPhraseBank = SkipPhraseBank,
) {
    fun findTarget(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var visited = 0
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(AccessibilityNodeInfo.obtain(root))

        while (pending.isNotEmpty() && visited < MAX_VISITED_NODES) {
            val node = pending.removeFirst()
            visited++

            val action = phraseBank.match(NodeText.from(node))
            if (action != null && preferences.isActionEnabled(action)) {
                val clickable = node.nearestClickable()
                node.recycle()
                pending.recycleAll()
                return clickable
            }

            repeat(node.childCount) { index ->
                node.getChild(index)?.let { pending.add(it) }
            }
            node.recycle()
        }

        pending.recycleAll()
        return null
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

    private fun ArrayDeque<AccessibilityNodeInfo>.recycleAll() {
        while (isNotEmpty()) {
            removeFirst().recycle()
        }
    }

    private companion object {
        const val MAX_VISITED_NODES = 300
    }
}
