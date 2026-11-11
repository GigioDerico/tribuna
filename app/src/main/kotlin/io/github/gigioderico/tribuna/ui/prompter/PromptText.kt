package io.github.gigioderico.tribuna.ui.prompter

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gigioderico.tribuna.align.Token
import io.github.gigioderico.tribuna.data.AppSettings

/**
 * Componente central de exibição do discurso (TRD §PromptText; PRD R1, R6, C2).
 *
 * - Renderiza com BasicText e AnnotatedString estilizada por token.
 * - Cache de TextLayoutResult para cálculo de rolagem e de toques.
 * - Espelhamento horizontal e vertical via graphicsLayer.
 * - Detecção segura de toques em palavras (getOffsetForPosition + getBoundingBox + 8 dp).
 */
@Composable
fun PromptText(
    text: String,
    tokens: List<Token>,
    cursor: Int,
    settings: AppSettings,
    onWordClick: (tokenIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val slopPx = with(density) { 8.dp.toPx() }

    val annotatedText =
        remember(text, tokens, cursor, settings.theme, settings.readTextOpacity) {
            PromptStyling.buildPromptText(
                text = text,
                tokens = tokens,
                cursorIndex = cursor,
                theme = settings.theme,
                readOpacity = settings.readTextOpacity,
            )
        }

    val textStyle =
        TextStyle(
            fontSize = settings.fontSizeSp.sp,
            lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp,
            color = settings.theme.textColor,
        )

    val tapModifier =
        Modifier.pointerInput(tokens, layoutResult) {
            detectTapGestures { offset ->
                val layout = layoutResult ?: return@detectTapGestures
                val touchedToken =
                    PromptHitTest.findTouchedToken(
                        tapPosition = offset,
                        tokens = tokens,
                        getCharOffset = { pos -> layout.getOffsetForPosition(pos) },
                        getBoundingBox = { charIndex -> layout.getBoundingBox(charIndex) },
                        slopPx = slopPx,
                    )
                if (touchedToken != null) {
                    onWordClick(touchedToken)
                }
            }
        }

    val mirrorModifier =
        Modifier.graphicsLayer {
            if (settings.mirrorHorizontal) scaleX = -1f
            if (settings.mirrorVertical) scaleY = -1f
        }

    BasicText(
        text = annotatedText,
        modifier =
            modifier
                .then(mirrorModifier)
                .then(tapModifier),
        style = textStyle,
        onTextLayout = { result ->
            layoutResult = result
            onTextLayout(result)
        },
    )
}
