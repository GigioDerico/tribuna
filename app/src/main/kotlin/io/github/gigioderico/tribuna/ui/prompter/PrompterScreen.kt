package io.github.gigioderico.tribuna.ui.prompter

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import io.github.gigioderico.tribuna.viewmodel.TimeWarning
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
    val context = LocalContext.current

    // Manter a tela sempre ligada durante o teleprompter (PRD D4 - FLAG_KEEP_SCREEN_ON sem WAKE_LOCK)
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(settings.theme.backgroundColor),
    ) {
        val totalSeconds = uiState.elapsedMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val formattedTime = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)

        // Tempo-alvo e alertas silenciosos (PRD D3)
        val targetMinutes = settings.targetTimeMinutes
        val timerText =
            if (targetMinutes > 0) {
                val targetMs = targetMinutes * 60 * 1000L
                val remainingMs = max(0L, targetMs - uiState.elapsedMs)
                val remSecTotal = remainingMs / 1000
                val remMin = remSecTotal / 60
                val remSec = remSecTotal % 60
                val formattedRem = String.format(Locale.ROOT, "%02d:%02d", remMin, remSec)
                "$formattedTime (-$formattedRem)"
            } else {
                formattedTime
            }

        val timerColor =
            when (uiState.warning) {
                TimeWarning.OneMin -> Color(0xFFE5483A)
                TimeWarning.FiveMin -> Color(0xFFF2B705)
                TimeWarning.None -> settings.theme.textColor.copy(alpha = 0.75f)
            }

        val warningBorderColor =
            when (uiState.warning) {
                TimeWarning.OneMin -> Color(0xFFE5483A)
                TimeWarning.FiveMin -> Color(0xFFF2B705)
                TimeWarning.None -> null
            }

        // Progresso do discurso para a barra superior (PRD D5)
        val progress =
            if (uiState.tokens.isNotEmpty()) {
                (uiState.cursor.toFloat() / (uiState.tokens.size - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
            } else {
                0f
            }
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(durationMillis = 200),
            label = "PrompterProgress",
        )

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
                val targetScroll = max(0f, lineTop).toInt()

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

            // Espaçador inferior para permitir rolagem até a linha guia e espaço para a barra fixa
            Spacer(modifier = Modifier.height(with(density) { (containerHeightPx - readingLineY + 120).toDp() }))
        }

        // Barra fina de progressão no topo da tela (PRD D5)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .background(settings.theme.textColor.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(3.dp)
                        .background(settings.theme.highlightColor),
            )
        }

        // Barra superior discreta: cronômetro, indicador de microfone e botão de saída rápido
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

            // Cronômetro decorrido e tempo restante (PRD D3)
            Text(
                text = timerText,
                color = timerColor,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Botão de saída rápido na barra superior
            Text(
                text = "✕ Sair",
                color = settings.theme.textColor.copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onFinish() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        // Linha guia visual sutil indicando a altura configurada de leitura (PRD A4)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = with(density) { readingLineY.toDp() }),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(settings.theme.highlightColor.copy(alpha = 0.22f)),
            )
            Text(
                text = "▶",
                color = settings.theme.highlightColor.copy(alpha = 0.50f),
                fontSize = 11.sp,
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp),
            )
            Text(
                text = "◀",
                color = settings.theme.highlightColor.copy(alpha = 0.50f),
                fontSize = 11.sp,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp),
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

        // Barra inferior fixa no final da leitura (presa na parte de baixo da tela)
        val isAtEnd =
            uiState.phase is ReadingPhase.Finished ||
                (uiState.tokens.isNotEmpty() && uiState.cursor >= uiState.tokens.size - 1)

        if (isAtEnd && uiState.phase !is ReadingPhase.Paused) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(settings.theme.backgroundColor.copy(alpha = 0.96f))
                        .border(
                            width = 1.dp,
                            color = settings.theme.textColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        ).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Fim do Discurso",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = settings.theme.highlightColor,
                        )
                        Text(
                            text = formattedTime,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = settings.theme.textColor.copy(alpha = 0.75f),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onRestart,
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = settings.theme.textColor,
                                ),
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(48.dp),
                        ) {
                            Text(
                                text = "Reiniciar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }

                        Button(
                            onClick = onFinish,
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = settings.theme.highlightColor,
                                    contentColor = settings.theme.backgroundColor,
                                ),
                            modifier =
                                Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                        ) {
                            Text(
                                text = "Voltar ao Editor",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }

        // Moldura de alerta silencioso nas bordas da tela (PRD D3)
        if (warningBorderColor != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .border(width = 6.dp, color = warningBorderColor),
            )
        }
    }
}
