package com.artur.antskip.matcher

import java.text.Normalizer
import java.util.Locale

private val whitespace = Regex("\\s+")
private val marks = Regex("\\p{M}+")
private val unsupported = Regex("[^\\p{L}\\p{N}-]+")

fun CharSequence?.normalizeForMatch(): String {
    if (isNullOrBlank()) return ""
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(marks, "")
        .lowercase(Locale.ROOT)
        .replace(unsupported, " ")
        .trim()
        .replace(whitespace, " ")
}
