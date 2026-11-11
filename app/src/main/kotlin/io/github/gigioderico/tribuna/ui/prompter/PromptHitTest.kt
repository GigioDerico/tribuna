package io.github.gigioderico.tribuna.ui.prompter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.gigioderico.tribuna.align.Token

object PromptHitTest {
    /**
     * Valida se um toque na tela corresponde a uma palavra válida do roteiro (TRD §PromptText).
     * Usa getOffsetForPosition para encontrar o caractere candidato e confirma que as coordenadas
     * estão dentro do bounding box da palavra acrescido de uma margem segura de +8 dp (slopPx).
     */
    fun findTouchedToken(
        tapPosition: Offset,
        tokens: List<Token>,
        getCharOffset: (Offset) -> Int,
        getBoundingBox: (Int) -> Rect,
        slopPx: Float,
    ): Int? {
        if (tokens.isEmpty()) return null

        val charOffset = getCharOffset(tapPosition)
        if (charOffset < 0) return null

        val tokenIndex =
            tokens.indexOfFirst { token ->
                charOffset in token.display
            }

        if (tokenIndex < 0) return null

        val box = runCatching { getBoundingBox(charOffset) }.getOrNull() ?: return null
        val expandedBox =
            Rect(
                left = box.left - slopPx,
                top = box.top - slopPx,
                right = box.right + slopPx,
                bottom = box.bottom + slopPx,
            )

        return if (expandedBox.contains(tapPosition)) {
            tokenIndex
        } else {
            null
        }
    }
}
