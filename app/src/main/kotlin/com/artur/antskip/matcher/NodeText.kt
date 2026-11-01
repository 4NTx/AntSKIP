package com.artur.antskip.matcher

import android.view.accessibility.AccessibilityNodeInfo

object NodeText {
    fun from(node: AccessibilityNodeInfo): String = buildString {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!text.isNullOrBlank()) append(text)
        if (!desc.isNullOrBlank()) {
            if (isNotEmpty()) append(' ')
            append(desc)
        }
    }.normalizeForMatch()

    fun visibleFrom(node: AccessibilityNodeInfo): String = from(node)
}
