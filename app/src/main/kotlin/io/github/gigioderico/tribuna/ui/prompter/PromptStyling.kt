package io.github.gigioderico.tribuna.ui.prompter

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import io.github.gigioderico.tribuna.align.Token
import io.github.gigioderico.tribuna.data.AppTheme

object PromptStyling {
    /**
     * Converte o texto bruto e a lista de tokens num AnnotatedString estilizado:
     * - Tokens anteriores ao cursor recebem opacidade atenuada (readOpacity).
     * - O token sob o cursor recebe a cor de destaque (highlightColor) e negrito.
     * - Tokens posteriores recebem a cor normal (textColor) sem atenuação.
     */
    fun buildPromptText(
        text: String,
        tokens: List<Token>,
        cursorIndex: Int,
        theme: AppTheme,
        readOpacity: Float,
    ): AnnotatedString {
        if (tokens.isEmpty()) {
            return AnnotatedString(text)
        }

        return buildAnnotatedString {
            append(text)

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
}
