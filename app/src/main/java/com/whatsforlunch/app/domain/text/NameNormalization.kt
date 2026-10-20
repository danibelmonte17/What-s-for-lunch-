package com.whatsforlunch.app.domain.text

import java.text.Normalizer
import java.util.Locale

/** Normalizes names consistently for search and duplicate detection. */
fun normalizeName(value: String): String = Normalizer
    .normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(ACCENT_MARKS, "")
    .replace(WHITESPACE, " ")

private val ACCENT_MARKS = Regex("\\p{Mn}+")
private val WHITESPACE = Regex("\\s+")
