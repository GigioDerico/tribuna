package io.github.gigioderico.tribuna.etapa0.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderUiState
import kotlin.math.roundToInt

/**
 * O texto inteiro num só `BasicText` com a palavra atual destacada (TRD Renderização e toque §PromptText,
 * mínimo da etapa0). O cursor conduz a rolagem: cada cursor novo anima até a linha da palavra ficar na linha de
 * leitura (30% da altura, padrão do A10), com a duração do painel; um cursor novo no meio cancela e redireciona a animação (TRD §Rolagem).
 */
@Composable
fun PromptText(
    state: ReaderUiState,
    onWordTapped: (Int) -> Unit,
    onScrollSettled: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewport by remember { mutableIntStateOf(0) }
    val slop = with(LocalDensity.current) { TOUCH_SLOP.toPx() }
    val colors = MaterialTheme.colorScheme
    val annotated =
        remember(state.text, state.tokens, state.cursor, colors) {
            buildAnnotatedString {
                append(state.text)
                val token = state.tokens.getOrNull(state.cursor) ?: return@buildAnnotatedString
                addStyle(SpanStyle(color = colors.onSurface.copy(alpha = READ_ALPHA)), 0, token.display.first)
                addStyle(SpanStyle(color = colors.onPrimary, background = colors.primary), token.display.first, token.display.last + 1)
            }
        }
    LaunchedEffect(state.cursor, state.cursorAtMs, layout, viewport) {
        val measured = layout ?: return@LaunchedEffect
        val token = state.tokens.getOrNull(state.cursor) ?: return@LaunchedEffect
        val line = measured.getLineForOffset(token.display.first)
        val target = (measured.getLineTop(line) - viewport * READING_LINE).roundToInt().coerceIn(0, scroll.maxValue)
        scroll.animateScrollTo(target, tween(durationMillis = state.settings.animationMs))
        onScrollSettled(state.cursor)
    }
    Box(modifier = modifier.onSizeChanged { viewport = it.height }.verticalScroll(scroll)) {
        BasicText(
            text = annotated,
            style = MaterialTheme.typography.headlineSmall.copy(color = colors.onSurface, lineHeight = 36.sp),
            onTextLayout = { layout = it },
            modifier =
                Modifier.fillMaxWidth().pointerInput(state.tokens) {
                    detectTapGestures { position ->
                        val measured = layout ?: return@detectTapGestures
                        val index =
                            HitTest.tappedToken(
                                x = position.x,
                                y = position.y,
                                offset = measured.getOffsetForPosition(position),
                                tokens = state.tokens,
                                boxOf = { char ->
                                    val rect = measured.getBoundingBox(char)
                                    WordBox(rect.left, rect.top, rect.right, rect.bottom)
                                },
                                slop = slop,
                            )
                        if (index != null) onWordTapped(index)
                    }
                },
        )
    }
}

/** Retângulo da palavra ampliado em 8 dp (TRD §Hit-test 2). */
private val TOUCH_SLOP = 8.dp

/** Opacidade do texto já lido: padrão do PRD A9 (40%). */
private const val READ_ALPHA = 0.4f

/** Linha de leitura: padrão do PRD A10 (30% da altura). */
private const val READING_LINE = 0.3f
