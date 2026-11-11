package io.github.gigioderico.tribuna.ui.prompter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gigioderico.tribuna.data.AppTheme
import io.github.gigioderico.tribuna.data.ScrollMode

/**
 * Painel sobreposto exibido durante a fase de pausa (PRD R6; TRD §PrompterScreen).
 * Permite retomar a leitura, reiniciar a partir do início, alternar modo ou concluir.
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
                    .padding(24.dp)
                    .background(
                        color = theme.backgroundColor,
                        shape = RoundedCornerShape(16.dp),
                    ).padding(24.dp),
        ) {
            Text(
                text = "PAUSADO",
                color = theme.highlightColor,
                fontSize = 24.sp,
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Modo atual: ${if (mode == ScrollMode.Voice) "Voz" else "Constante"}",
                color = theme.textColor.copy(alpha = 0.8f),
                fontSize = 14.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onResume,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = theme.highlightColor,
                        contentColor = theme.backgroundColor,
                    ),
                modifier = Modifier.fillMaxWidth(0.8f),
            ) {
                Text("Retomar leitura", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(0.8f),
            ) {
                OutlinedButton(
                    onClick = onToggleMode,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = theme.textColor,
                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (mode == ScrollMode.Voice) "Mudar p/ Constante" else "Mudar p/ Voz",
                        fontSize = 12.sp,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onRestart,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = theme.textColor,
                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Do início", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onFinish,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF5252),
                    ),
                modifier = Modifier.fillMaxWidth(0.8f),
            ) {
                Text("Concluir e sair", fontSize = 14.sp)
            }
        }
    }
}
