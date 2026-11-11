package io.github.gigioderico.tribuna.ui.editor

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gigioderico.tribuna.data.AppTheme
import io.github.gigioderico.tribuna.data.ScrollMode

/**
 * Tela de edição do discurso (PRD T1, T2, T3; TRD §ui.editor).
 *
 * - O texto vive unicamente em memória no PrompterViewModel.
 * - Suporta Colar da área de transferência e Limpar com um toque.
 * - Exibe contagem de palavras e estimativa de tempo a 130 ppm.
 * - Alternância entre os modos Voz e Constante.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    text: String,
    onTextChanged: (String) -> Unit,
    mode: ScrollMode,
    onModeChanged: (ScrollMode) -> Unit,
    onStartReading: () -> Unit,
    onOpenSettings: () -> Unit,
    theme: AppTheme,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val wordCount = remember(text) { EditorEstimates.countWords(text) }
    val durationSeconds = remember(wordCount) { EditorEstimates.estimateDurationSeconds(wordCount) }
    val formattedDuration = remember(durationSeconds) { EditorEstimates.formatDuration(durationSeconds) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tribuna",
                        color = theme.textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                },
                actions = {
                    if (text.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onTextChanged("") },
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = theme.textColor.copy(alpha = 0.7f),
                                ),
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text("Limpar", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                onTextChanged(clipText)
                            }
                        },
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = theme.highlightColor,
                            ),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Colar", fontSize = 12.sp)
                    }

                    IconButton(onClick = onOpenSettings) {
                        Text("⚙", fontSize = 20.sp, color = theme.textColor)
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = theme.backgroundColor,
                    ),
            )
        },
        bottomBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(theme.backgroundColor)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                // Estatísticas e Seletor de Modo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "$wordCount palavras · ~$formattedDuration",
                        color = theme.textColor.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    Row {
                        FilterChip(
                            selected = mode == ScrollMode.Voice,
                            onClick = { onModeChanged(ScrollMode.Voice) },
                            label = { Text("Voz", fontSize = 12.sp) },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.highlightColor,
                                    selectedLabelColor = theme.backgroundColor,
                                    labelColor = theme.textColor,
                                ),
                            modifier = Modifier.padding(end = 4.dp),
                        )

                        FilterChip(
                            selected = mode == ScrollMode.Constant,
                            onClick = { onModeChanged(ScrollMode.Constant) },
                            label = { Text("Constante", fontSize = 12.sp) },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.highlightColor,
                                    selectedLabelColor = theme.backgroundColor,
                                    labelColor = theme.textColor,
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botão de Iniciar Leitura
                Button(
                    onClick = onStartReading,
                    enabled = text.isNotBlank(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = theme.highlightColor,
                            contentColor = theme.backgroundColor,
                            disabledContainerColor = theme.textColor.copy(alpha = 0.2f),
                            disabledContentColor = theme.textColor.copy(alpha = 0.4f),
                        ),
                    shape = RoundedCornerShape(12.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                ) {
                    Text(
                        text = "Iniciar Leitura",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            TextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = "Cole ou digite aqui o discurso que você irá apresentar...",
                        color = theme.textColor.copy(alpha = 0.4f),
                        fontSize = 16.sp,
                    )
                },
                textStyle =
                    TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = theme.textColor,
                    ),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
