package com.artur.antskip.matcher

import android.view.accessibility.AccessibilityNodeInfo

object NodeText {
    fun from(node: AccessibilityNodeInfo): String = buildString {
        append(node.text?.toString().orEmpty())
        append(' ')
        append(node.contentDescription?.toString().orEmpty())
        append(' ')
        append(node.viewIdResourceName.orEmpty())
    }.normalizeForMatch()
}
