package io.github.gigioderico.tribuna.ui.prompter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gigioderico.tribuna.data.AppTheme
import io.github.gigioderico.tribuna.data.ScrollMode
import java.util.Locale

/**
 * Painel sobreposto exibido durante a fase de pausa (PRD R6; TRD §PrompterScreen).
 * Permite retomar a leitura, alternar modo, reiniciar do início ou encerrar.
 */
@Composable
fun PauseOverlay(
    mode: ScrollMode,
    theme: AppTheme,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onFinish: () -> Unit,
    onToggleMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .padding(20.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(0.92f)
                    .background(
                        color = theme.backgroundColor,
                        shape = RoundedCornerShape(16.dp),
                    ).padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = "PAUSADO",
                color = theme.highlightColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Modo: ${if (mode == ScrollMode.Voice) "Acompanhamento por Voz" else "Rolagem Constante"}",
                color = theme.textColor.copy(alpha = 0.8f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Botão Principal: Continuar
            Button(
                onClick = onResume,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = theme.highlightColor,
                        contentColor = theme.backgroundColor,
                    ),
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
            ) {
                Text(
                    text = "Continuar Leitura",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Alternar Modo
            OutlinedButton(
                onClick = onToggleMode,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = theme.textColor,
                    ),
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp),
            ) {
                Text(
                    text = if (mode == ScrollMode.Voice) "Mudar para Modo Constante" else "Mudar para Modo Voz",
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reiniciar Leitura
            OutlinedButton(
                onClick = onRestart,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = theme.textColor,
                    ),
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp),
            ) {
                Text(
                    text = "Reiniciar do Início",
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Encerrar / Voltar ao Editor
            OutlinedButton(
                onClick = onFinish,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF5252),
                    ),
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp),
            ) {
                Text(
                    text = "Concluir e Voltar ao Editor",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/**
 * Painel sobreposto exibido ao concluir a leitura (PRD §Fluxo de encerramento; TRD §PrompterScreen).
 */
@Composable
fun FinishedOverlay(
    elapsedMs: Long,
    theme: AppTheme,
    onRestart: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val formattedTime = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .padding(20.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(0.92f)
                    .background(
                        color = theme.backgroundColor,
                        shape = RoundedCornerShape(16.dp),
                    ).padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Text(
                text = "Fim da Leitura",
                color = theme.highlightColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tempo total de apresentação:",
                color = theme.textColor.copy(alpha = 0.75f),
                fontSize = 13.sp,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formattedTime,
                color = theme.textColor,
                fontSize = 28.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onFinish,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = theme.highlightColor,
                        contentColor = theme.backgroundColor,
                    ),
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
            ) {
                Text(
                    text = "Voltar ao Editor",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onRestart,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = theme.textColor,
                    ),
                shape = RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp),
            ) {
                Text(
                    text = "Reiniciar Apresentação",
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
