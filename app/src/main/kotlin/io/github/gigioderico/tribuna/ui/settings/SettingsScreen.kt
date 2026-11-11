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
import androidx.compose.ui.text.font.FontWeight
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
            // Seção Temas
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
                AppTheme.entries.take(4).forEach { itemTheme ->
                    val isSelected = theme == itemTheme
                    Box(
                        modifier =
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(itemTheme.backgroundColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) itemTheme.highlightColor else theme.textColor.copy(alpha = 0.3f),
                                    shape = CircleShape,
                                ).clickable { onUpdateSettings { it.copy(theme = itemTheme) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Aa",
                            color = itemTheme.textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Seção Tamanho da Fonte
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

            Spacer(modifier = Modifier.height(20.dp))

            // Seção Espaçamento entre Linhas
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

            Spacer(modifier = Modifier.height(20.dp))

            // Seção Margem Lateral
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

            Spacer(modifier = Modifier.height(24.dp))

            // Espelhamento Horizontal
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Espelhar Texto (Horizontal)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.textColor,
                    )
                    Text(
                        text = "Para uso com vidros de reflexão / teleprompter",
                        fontSize = 12.sp,
                        color = theme.textColor.copy(alpha = 0.6f),
                    )
                }

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

            Spacer(modifier = Modifier.height(28.dp))

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
                        label = { Text(presetName) },
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
        }
    }
}
