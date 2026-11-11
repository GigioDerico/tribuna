package io.github.gigioderico.tribuna.ui.editor

import java.util.Locale

object EditorEstimates {
    const val DEFAULT_WORDS_PER_MINUTE = 130

    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
    }

    fun estimateDurationSeconds(
        wordCount: Int,
        wordsPerMinute: Int = DEFAULT_WORDS_PER_MINUTE,
    ): Int {
        if (wordCount <= 0 || wordsPerMinute <= 0) return 0
        return ((wordCount.toDouble() / wordsPerMinute) * 60).toInt()
    }

    fun formatDuration(totalSeconds: Int): String {
        val seconds = totalSeconds.coerceAtLeast(0)
        val minutes = seconds / 60
        val remSeconds = seconds % 60
        return String.format(Locale.ROOT, "%02d:%02d", minutes, remSeconds)
    }
}
