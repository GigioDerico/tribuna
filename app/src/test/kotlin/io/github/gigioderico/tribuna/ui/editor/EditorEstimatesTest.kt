package io.github.gigioderico.tribuna.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorEstimatesTest {
    @Test
    fun countWords_emptyOrWhitespace_returnsZero() {
        assertEquals(0, EditorEstimates.countWords(""))
        assertEquals(0, EditorEstimates.countWords("   \n\t  "))
    }

    @Test
    fun countWords_countsSeparatedWordsCorrectly() {
        val text = "Tribuna é um teleprompter inteligente de código aberto."
        assertEquals(8, EditorEstimates.countWords(text))

        val textWithSpaces = "  palavra1   palavra2 \n\n palavra3   "
        assertEquals(3, EditorEstimates.countWords(textWithSpaces))
    }

    @Test
    fun estimateDuration_calculatesCorrectTimeBasedOn130Wpm() {
        // 130 palavras a 130 palavras por minuto = 60 segundos
        assertEquals(60, EditorEstimates.estimateDurationSeconds(130))

        // 65 palavras = 30 segundos
        assertEquals(30, EditorEstimates.estimateDurationSeconds(65))

        // 0 palavras = 0 segundos
        assertEquals(0, EditorEstimates.estimateDurationSeconds(0))
    }

    @Test
    fun formatDuration_formatsMinutesAndSeconds() {
        assertEquals("00:00", EditorEstimates.formatDuration(0))
        assertEquals("01:00", EditorEstimates.formatDuration(60))
        assertEquals("02:15", EditorEstimates.formatDuration(135))
        assertEquals("10:05", EditorEstimates.formatDuration(605))
    }
}
