package io.github.gigioderico.tribuna.ui.prompter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.gigioderico.tribuna.align.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PromptHitTestTest {
    @Test
    fun findTouchedToken_tapInsideBoundingBox_returnsTokenIndex() {
        val text = "Tribuna teleprompter inteligente"
        val tokens = Tokenizer.tokenize(text)

        // Simula layout onde cada caractere ocupa 10px de largura e 20px de altura
        // "teleprompter" começa no offset 8 e termina no offset 20
        val tokenTeleprompter = tokens[1]
        val rect = Rect(left = 80f, top = 0f, right = 200f, bottom = 20f)

        val hitToken =
            PromptHitTest.findTouchedToken(
                tapPosition = Offset(x = 100f, y = 10f),
                tokens = tokens,
                getCharOffset = { 10 }, // Dentro de "teleprompter"
                getBoundingBox = { rect },
                slopPx = 8f,
            )

        assertEquals(1, hitToken)
    }

    @Test
    fun findTouchedToken_tapWithinSlopMargin_returnsTokenIndex() {
        val text = "Tribuna teleprompter"
        val tokens = Tokenizer.tokenize(text)
        val rect = Rect(left = 80f, top = 10f, right = 200f, bottom = 30f)

        // Toque a 5px abaixo da base (dentro da margem de segurança de 8px)
        val hitToken =
            PromptHitTest.findTouchedToken(
                tapPosition = Offset(x = 100f, y = 35f),
                tokens = tokens,
                getCharOffset = { 10 },
                getBoundingBox = { rect },
                slopPx = 8f,
            )

        assertEquals(1, hitToken)
    }

    @Test
    fun findTouchedToken_tapOutsideSlopMargin_returnsNull() {
        val text = "Tribuna teleprompter"
        val tokens = Tokenizer.tokenize(text)
        val rect = Rect(left = 80f, top = 10f, right = 200f, bottom = 30f)

        // Toque a 30px de distância (fora dos 8px de tolerância)
        val hitToken =
            PromptHitTest.findTouchedToken(
                tapPosition = Offset(x = 100f, y = 60f),
                tokens = tokens,
                getCharOffset = { 10 },
                getBoundingBox = { rect },
                slopPx = 8f,
            )

        assertNull(hitToken)
    }
}
