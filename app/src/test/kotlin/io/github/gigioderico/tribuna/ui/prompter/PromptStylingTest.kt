package io.github.gigioderico.tribuna.ui.prompter

import androidx.compose.ui.text.font.FontWeight
import io.github.gigioderico.tribuna.align.Tokenizer
import io.github.gigioderico.tribuna.data.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptStylingTest {
    @Test
    fun buildPromptText_highlightsCurrentTokenAndDimsPreviousTokens() {
        val text = "Tribuna teleprompter inteligente de voz"
        val tokens = Tokenizer.tokenize(text)
        assertEquals(5, tokens.size)

        val cursor = 2 // "inteligente"
        val theme = AppTheme.DarkTealMint
        val readOpacity = 0.5f

        val annotated =
            PromptStyling.buildPromptText(
                text = text,
                tokens = tokens,
                cursorIndex = cursor,
                theme = theme,
                readOpacity = readOpacity,
            )

        assertEquals(text, annotated.text)

        // Deve haver estilos aplicados
        val spans = annotated.spanStyles
        assertTrue(spans.isNotEmpty())

        // Token 0 ("Tribuna") deve ter cor atenuada
        val token0 = tokens[0]
        val span0 = spans.firstOrNull { it.start == token0.display.first && it.end == token0.display.last + 1 }
        assertEquals(theme.textColor.copy(alpha = readOpacity), span0?.item?.color)

        // Token 2 ("inteligente") deve ter a cor de highlight e negrito
        val token2 = tokens[2]
        val span2 = spans.firstOrNull { it.start == token2.display.first && it.end == token2.display.last + 1 }
        assertEquals(theme.highlightColor, span2?.item?.color)
        assertEquals(FontWeight.Bold, span2?.item?.fontWeight)

        // Token 4 ("voz") deve ter cor normal
        val token4 = tokens[4]
        val span4 = spans.firstOrNull { it.start == token4.display.first && it.end == token4.display.last + 1 }
        assertEquals(theme.textColor, span4?.item?.color)
    }

    @Test
    fun buildPromptText_emptyTokens_returnsRawText() {
        val text = "Texto sem tokens válidos"
        val annotated =
            PromptStyling.buildPromptText(
                text = text,
                tokens = emptyList(),
                cursorIndex = 0,
                theme = AppTheme.DarkTealMint,
                readOpacity = 0.4f,
            )
        assertEquals(text, annotated.text)
        assertTrue(annotated.spanStyles.isEmpty())
    }
}
