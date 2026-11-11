package io.github.gigioderico.tribuna.ui.prompter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import io.github.gigioderico.tribuna.align.Token
import io.github.gigioderico.tribuna.data.AppTheme

object PromptStyling {
    /** Cor âmbar/dourada para notas de palco e avisos de roteiro (PRD T5). */
    val STAGE_NOTE_COLOR = Color(0xFFFFB74D)

    /**
     * Converte o texto bruto e a lista de tokens num AnnotatedString estilizado:
     * - Tokens anteriores ao cursor recebem opacidade atenuada (readOpacity).
     * - O token sob o cursor recebe a cor de destaque (highlightColor) e negrito.
     * - Tokens posteriores recebem a cor normal (textColor) sem atenuação.
     * - Linhas com // (PRD T5) viram notas visuais de palco (itálico e cor âmbar).
     */
    fun buildPromptText(
        text: String,
        tokens: List<Token>,
        cursorIndex: Int,
        theme: AppTheme,
        readOpacity: Float,
    ): AnnotatedString =
        buildAnnotatedString {
            append(text)

            // Dicas de palco no texto (PRD T5: linhas com // viram notas visuais)
            var lineStart = 0
            for (line in text.split('\n')) {
                val trimmed = line.trimStart()
                if (trimmed.startsWith("//")) {
                    val start = lineStart
                    val end = (lineStart + line.length).coerceIn(start, text.length)
                    if (start < end) {
                        val isPast =
                            when {
                                cursorIndex in tokens.indices -> cursorIndex > 0 && end <= tokens[cursorIndex].display.first
                                tokens.isNotEmpty() && cursorIndex >= tokens.size -> true
                                else -> false
                            }
                        val alpha = if (isPast) readOpacity else 0.95f
                        addStyle(
                            SpanStyle(
                                color = STAGE_NOTE_COLOR.copy(alpha = alpha),
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            start,
                            end,
                        )
                    }
                }
                lineStart += line.length + 1
            }

            tokens.forEachIndexed { index, token ->
                val start = token.display.first.coerceIn(0, text.length)
                val end = (token.display.last + 1).coerceIn(start, text.length)

                if (start < end) {
                    val style =
                        when {
                            index < cursorIndex -> {
                                SpanStyle(
                                    color = theme.textColor.copy(alpha = readOpacity),
                                )
                            }

                            index == cursorIndex -> {
                                SpanStyle(
                                    color = theme.highlightColor,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            else -> {
                                SpanStyle(
                                    color = theme.textColor,
                                )
                            }
                        }
                    addStyle(style, start, end)
                }
            }
        }
}
