package io.github.gigioderico.tribuna.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gigioderico.tribuna.align.AlignPresets
import io.github.gigioderico.tribuna.data.AppSettings
import io.github.gigioderico.tribuna.data.AppTheme
import java.util.Locale

/**
 * Tela de configurações visuais e do motor de leitura (PRD A1–A14; TRD §ui.settings).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = settings.theme
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes",
                        color = theme.textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, color = theme.textColor)
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = theme.backgroundColor,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Seção: Prévia da Leitura Real
            Text(
                text = "Prévia da Leitura",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = theme.highlightColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Veja em tempo real como o texto aparecerá no teleprompter:",
                fontSize = 12.sp,
                color = theme.textColor.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(10.dp))

            ReadingPreviewCard(settings = settings)

            Spacer(modifier = Modifier.height(24.dp))

            // Seção: Tema Visual
            Text(
                text = "Tema Visual",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = theme.highlightColor,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AppTheme.entries.forEach { itemTheme ->
                    val isSelected = theme == itemTheme
                    Box(
                        modifier =
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(itemTheme.backgroundColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) itemTheme.highlightColor else theme.textColor.copy(alpha = 0.35f),
                                    shape = CircleShape,
                                ).clickable { onUpdateSettings { it.copy(theme = itemTheme) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Aa",
                            color = itemTheme.textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Seção: Tamanho da Fonte
            Text(
                text = "Tamanho da Fonte: ${settings.fontSizeSp.toInt()} sp",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textColor,
            )
            Slider(
                value = settings.fontSizeSp,
                onValueChange = { newVal -> onUpdateSettings { it.copy(fontSizeSp = newVal) } },
                valueRange = 24f..72f,
                steps = 23,
                colors =
                    SliderDefaults.colors(
                        thumbColor = theme.highlightColor,
                        activeTrackColor = theme.highlightColor,
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Seção: Espaçamento entre Linhas
            Text(
                text = String.format(Locale.ROOT, "Espaçamento entre Linhas: %.1fx", settings.lineSpacingMultiplier),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textColor,
            )
            Slider(
                value = settings.lineSpacingMultiplier,
                onValueChange = { newVal -> onUpdateSettings { it.copy(lineSpacingMultiplier = newVal) } },
                valueRange = 1.1f..2.0f,
                steps = 8,
                colors =
                    SliderDefaults.colors(
                        thumbColor = theme.highlightColor,
                        activeTrackColor = theme.highlightColor,
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Seção: Margem Lateral
            Text(
                text = "Margem Lateral: ${settings.horizontalMarginPercent.toInt()}%",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textColor,
            )
            Slider(
                value = settings.horizontalMarginPercent,
                onValueChange = { newVal -> onUpdateSettings { it.copy(horizontalMarginPercent = newVal) } },
                valueRange = 2f..25f,
                steps = 22,
                colors =
                    SliderDefaults.colors(
                        thumbColor = theme.highlightColor,
                        activeTrackColor = theme.highlightColor,
                    ),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Espelhamento Horizontal
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Espelhar Texto (Horizontal)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Para uso com vidros de reflexão / teleprompter",
                        fontSize = 12.sp,
                        color = theme.textColor.copy(alpha = 0.6f),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = settings.mirrorHorizontal,
                    onCheckedChange = { checked -> onUpdateSettings { it.copy(mirrorHorizontal = checked) } },
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = theme.backgroundColor,
                            checkedTrackColor = theme.highlightColor,
                        ),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sensibilidade do Motor de Voz
            Text(
                text = "Sensibilidade do Acompanhamento",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textColor,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(AlignPresets.CONSERVADOR_NAME, AlignPresets.NORMAL_NAME, AlignPresets.AGIL_NAME).forEach { presetName ->
                    val isSelected = settings.sensitivity.equals(presetName, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onUpdateSettings { it.copy(sensitivity = presetName) } },
                        label = {
                            Text(
                                text = presetName,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = theme.highlightColor,
                                selectedLabelColor = theme.backgroundColor,
                                labelColor = theme.textColor,
                            ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Card de prévia interativa que simula o teleprompter com as configurações atuais.
 */
@Composable
private fun ReadingPreviewCard(
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val theme = settings.theme
    val previewFontSize = (settings.fontSizeSp * 0.55f).coerceIn(16f, 36f)
    val previewLineHeight = (previewFontSize * settings.lineSpacingMultiplier)
    val previewMarginPercent = (settings.horizontalMarginPercent / 100f).coerceIn(0.02f, 0.25f)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(theme.backgroundColor)
                .border(
                    width = 1.5.dp,
                    color = theme.textColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(14.dp),
                ).padding(12.dp),
    ) {
        Column {
            // Barra superior simulada da prévia
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .background(color = Color(0xFF4CAF50), shape = CircleShape),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "VOZ",
                    color = theme.textColor.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "00:42",
                    color = theme.textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Texto com estilos simulando palavras lidas, palavra ativa e palavras seguintes
            val previewText =
                buildAnnotatedString {
                    // Trecho já lido com opacidade reduzida
                    withStyle(
                        style =
                            SpanStyle(
                                color = theme.textColor.copy(alpha = settings.readTextOpacity),
                            ),
                    ) {
                        append("Ouvir com atenção é o ")
                    }

                    // Palavra ativa destacada com a cor de destaque do tema
                    withStyle(
                        style =
                            SpanStyle(
                                color = theme.highlightColor,
                                fontWeight = FontWeight.Bold,
                            ),
                    ) {
                        append("primeiro passo")
                    }

                    // Próximas palavras a serem lidas
                    withStyle(
                        style =
                            SpanStyle(
                                color = theme.textColor,
                            ),
                    ) {
                        append(" para uma fala clara e confiante diante do público.")
                    }
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = (100 * previewMarginPercent).dp)
                        .then(
                            if (settings.mirrorHorizontal) {
                                Modifier.graphicsLayer(scaleX = -1f)
                            } else {
                                Modifier
                            },
                        ),
            ) {
                Text(
                    text = previewText,
                    fontSize = previewFontSize.sp,
                    lineHeight = previewLineHeight.sp,
                )
            }
        }
    }
}
