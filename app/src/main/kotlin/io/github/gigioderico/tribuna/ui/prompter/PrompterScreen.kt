package io.github.gigioderico.tribuna.ui.prompter

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gigioderico.tribuna.data.AppSettings
import io.github.gigioderico.tribuna.data.ScrollMode
import io.github.gigioderico.tribuna.viewmodel.PrompterUiState
import io.github.gigioderico.tribuna.viewmodel.ReadingPhase
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.max

/**
 * Tela principal do Teleprompter (TRD §PrompterScreen; PRD R1, R2, R5, R6, R7, D2).
 */
@Composable
fun PrompterScreen(
    uiState: PrompterUiState,
    settings: AppSettings,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onFinish: () -> Unit,
    onWordClick: (Int) -> Unit,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(settings.theme.backgroundColor),
    ) {
        val containerHeightPx = constraints.maxHeight.toFloat()
        val readingLineRatio = (settings.readingLinePercent / 100f).coerceIn(0.1f, 0.9f)
        val readingLineY = containerHeightPx * readingLineRatio

        // Rolagem suave no modo Voz baseada no cursor ativo
        LaunchedEffect(uiState.cursor, layoutResult, uiState.mode) {
            val layout = layoutResult ?: return@LaunchedEffect
            if (uiState.mode != ScrollMode.Voice) return@LaunchedEffect
            val tokens = uiState.tokens
            if (tokens.isEmpty() || uiState.cursor !in tokens.indices) return@LaunchedEffect

            val token = tokens[uiState.cursor]
            val charIndex = token.display.first
            if (charIndex in 0 until layout.layoutInput.text.length) {
                val lineIndex = layout.getLineForOffset(charIndex)
                val lineTop = layout.getLineTop(lineIndex)
                val targetScroll = max(0f, lineTop - readingLineY).toInt()

                scrollState.animateScrollTo(
                    value = targetScroll,
                    animationSpec =
                        tween(
                            durationMillis = settings.scrollDurationMs.toInt(),
                            easing = FastOutSlowInEasing,
                        ),
                )
            }
        }

        // Rolagem contínua no modo Constante
        LaunchedEffect(uiState.phase, uiState.mode, settings.constantSpeedLevel) {
            if (uiState.phase == ReadingPhase.Reading && uiState.mode == ScrollMode.Constant) {
                val pxPerSecond = with(density) { (20 + settings.constantSpeedLevel * 15).dp.toPx() }
                var lastTime = System.nanoTime()

                while (isActive && uiState.phase == ReadingPhase.Reading) {
                    val now = System.nanoTime()
                    val dt = (now - lastTime) / 1_000_000_000f
                    lastTime = now
                    val step = (pxPerSecond * dt).toInt()
                    if (step > 0) {
                        scrollState.dispatchRawDelta(step.toFloat())
                    }
                    kotlinx.coroutines.delay(16L) // ~60 fps
                }
            }
        }

        // Container do texto com margem lateral configurada
        val marginHorizontalPercent = settings.horizontalMarginPercent / 100f
        val horizontalPadding = (maxWidth * marginHorizontalPercent)

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (uiState.phase == ReadingPhase.Reading) {
                            onPause()
                        } else if (uiState.phase == ReadingPhase.Paused) {
                            onResume()
                        }
                    }.padding(horizontal = horizontalPadding),
        ) {
            // Espaçador para que o primeiro parágrafo comece na linha guia de leitura
            Spacer(modifier = Modifier.height(with(density) { readingLineY.toDp() }))

            PromptText(
                text = uiState.text,
                tokens = uiState.tokens,
                cursor = uiState.cursor,
                settings = settings,
                onWordClick = onWordClick,
                onTextLayout = { layoutResult = it },
                modifier = Modifier.fillMaxWidth(),
            )

            // Espaçador inferior para permitir rolagem até o final do texto
            Spacer(modifier = Modifier.height(with(density) { (containerHeightPx - readingLineY).toDp() }))
        }

        // Barra superior discreta: cronômetro e indicador de microfone
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Indicador de microfone
            val micColor =
                when {
                    !uiState.micAvailable -> {
                        Color.Red
                    }

                    uiState.phase == ReadingPhase.Reading -> {
                        if (uiState.micLevel > 0.05f) settings.theme.highlightColor else Color(0xFF4CAF50)
                    }

                    else -> {
                        Color.Gray
                    }
                }

            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .background(color = micColor, shape = CircleShape),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (uiState.mode == ScrollMode.Voice) "VOZ" else "CONST",
                color = settings.theme.textColor.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Cronômetro decorrido
            val totalSeconds = uiState.elapsedMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val formattedTime = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)

            Text(
                text = formattedTime,
                color = settings.theme.textColor.copy(alpha = 0.75f),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            )
        }

        // Overlay de pausa
        if (uiState.phase == ReadingPhase.Paused) {
            PauseOverlay(
                mode = uiState.mode,
                theme = settings.theme,
                onResume = onResume,
                onRestart = onRestart,
                onFinish = onFinish,
                onToggleMode = onToggleMode,
            )
        }
    }
}
