package com.example.forgeplan.core.language

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppLanguage {
    var current by mutableStateOf("EN")

    fun set(language: String) {
        current = language
    }

    fun isPortuguese(): Boolean {
        return current == "PT"
    }
}

fun appText(
    en: String,
    pt: String
): String {
    return if (AppLanguage.isPortuguese()) pt else en
}