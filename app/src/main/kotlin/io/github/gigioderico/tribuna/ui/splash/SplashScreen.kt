package io.github.gigioderico.tribuna.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gigioderico.tribuna.data.AppTheme
import kotlinx.coroutines.delay

/**
 * Splash Screen animada em Jetpack Compose (Regra 8: 100% Compose).
 */
@Composable
fun SplashScreen(
    theme: AppTheme,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "SplashScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "SplashAlpha",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "SplashWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "WavePhase",
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(1_250L)
        onFinished()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .scale(scale)
                    .alpha(alpha),
        ) {
            TribunaEmblem(
                highlightColor = theme.highlightColor,
                textColor = theme.textColor,
                backgroundColor = theme.backgroundColor,
                wavePhase = wavePhase,
                modifier = Modifier.size(132.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Tribuna",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = theme.highlightColor,
                letterSpacing = 1.sp,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Teleprompter Guiado por Voz",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = theme.textColor.copy(alpha = 0.8f),
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Barras animadas de onda de voz
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(24.dp),
            ) {
                val multipliers = listOf(0.45f, 0.85f, 1.0f, 0.7f, 0.45f)
                multipliers.forEachIndexed { index, factor ->
                    val dynamicFraction =
                        if (index % 2 == 0) {
                            (factor * wavePhase).coerceIn(0.25f, 1f)
                        } else {
                            (factor * (1.35f - wavePhase)).coerceIn(0.25f, 1f)
                        }
                    Box(
                        modifier =
                            Modifier
                                .width(4.dp)
                                .height((22f * dynamicFraction).dp)
                                .background(
                                    color = theme.highlightColor.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(50),
                                ),
                    )
                }
            }
        }

        // Selo inferior de privacidade offline
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .alpha(alpha)
                    .background(
                        color = theme.textColor.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(50),
                    ).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "🔒 100% Offline · Privacidade Total",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = theme.textColor.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
private fun TribunaEmblem(
    highlightColor: Color,
    textColor: Color,
    backgroundColor: Color,
    wavePhase: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // Halo externo sutil pulsante
        drawCircle(
            color = highlightColor.copy(alpha = 0.08f * wavePhase),
            radius = w * 0.48f,
            center = center,
        )
        drawCircle(
            color = textColor.copy(alpha = 0.06f),
            radius = w * 0.42f,
            center = center,
        )

        // Moldura da tela do teleprompter
        val cardLeft = w * 0.20f
        val cardTop = h * 0.18f
        val cardWidth = w * 0.60f
        val cardHeight = h * 0.40f
        drawRoundRect(
            color = textColor.copy(alpha = 0.09f),
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
        )
        drawRoundRect(
            color = textColor.copy(alpha = 0.22f),
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = w * 0.018f),
        )

        // Linha 1 (texto já lido)
        drawRoundRect(
            color = textColor.copy(alpha = 0.35f),
            topLeft = Offset(w * 0.28f, h * 0.26f),
            size = Size(w * 0.34f, h * 0.035f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f),
        )

        // Linha 2 (linha guia ativa em destaque com marcador lateral)
        val trianglePath =
            Path().apply {
                moveTo(w * 0.23f, h * 0.345f)
                lineTo(w * 0.265f, h * 0.368f)
                lineTo(w * 0.23f, h * 0.391f)
                close()
            }
        drawPath(path = trianglePath, color = highlightColor)

        drawRoundRect(
            color = highlightColor,
            topLeft = Offset(w * 0.28f, h * 0.35f),
            size = Size(w * 0.44f, h * 0.038f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f),
        )

        // Linha 3 (texto a ler)
        drawRoundRect(
            color = textColor.copy(alpha = 0.50f),
            topLeft = Offset(w * 0.28f, h * 0.44f),
            size = Size(w * 0.26f, h * 0.035f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f),
        )

        // Cápsula do microfone em destaque
        val micWidth = w * 0.14f
        val micHeight = h * 0.23f
        val micLeft = (w - micWidth) / 2f
        val micTop = h * 0.46f
        drawRoundRect(
            color = highlightColor,
            topLeft = Offset(micLeft, micTop),
            size = Size(micWidth, micHeight),
            cornerRadius = CornerRadius(micWidth / 2f, micWidth / 2f),
        )

        // Detalhes na cápsula do microfone
        for (i in 0..2) {
            val lineY = micTop + micHeight * (0.32f + i * 0.18f)
            drawLine(
                color = backgroundColor,
                start = Offset(micLeft + micWidth * 0.25f, lineY),
                end = Offset(micLeft + micWidth * 0.75f, lineY),
                strokeWidth = w * 0.015f,
                cap = StrokeCap.Round,
            )
        }

        // Arco e pedestal do microfone
        val arcWidth = w * 0.24f
        val arcHeight = h * 0.20f
        val arcRect =
            Rect(
                offset = Offset((w - arcWidth) / 2f, h * 0.53f),
                size = Size(arcWidth, arcHeight),
            )
        drawArc(
            color = highlightColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = w * 0.028f, cap = StrokeCap.Round),
        )

        // Haste vertical e base
        drawLine(
            color = highlightColor,
            start = Offset(w * 0.5f, h * 0.73f),
            end = Offset(w * 0.5f, h * 0.81f),
            strokeWidth = w * 0.028f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = highlightColor,
            start = Offset(w * 0.40f, h * 0.81f),
            end = Offset(w * 0.60f, h * 0.81f),
            strokeWidth = w * 0.028f,
            cap = StrokeCap.Round,
        )
    }
}
