package io.github.gigioderico.tribuna.etapa0.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.gigioderico.tribuna.etapa0.R
import io.github.gigioderico.tribuna.etapa0.speech.RecognizerStrategy
import io.github.gigioderico.tribuna.etapa0.viewmodel.AudioChoice
import io.github.gigioderico.tribuna.etapa0.viewmodel.DebugSettings
import io.github.gigioderico.tribuna.etapa0.viewmodel.PresetChoice
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderController
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderPhase
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderUiState

/**
 * Painel de depuração (TRD Etapa 0, item 4; spec 0003, D18): hipóteses brutas, cauda, score, movimento,
 * highWater, latências e tempos, só em memória (TRD §Onde o texto vive). Controles de D3–D5, D8, D13 e a
 * exportação (D7, D9). Ajustes do motor só com a leitura parada.
 */
@Composable
fun DebugPanel(
    state: ReaderUiState,
    controller: ReaderController,
    modifier: Modifier = Modifier,
) {
    val idle = state.phase == ReaderPhase.Idle
    val debug = state.debug
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val small = MaterialTheme.typography.bodySmall
        Text(stringResource(R.string.debug_partial, debug.lastPartial.joinToString(" ")), style = small)
        Text(stringResource(R.string.debug_final, debug.lastFinal.joinToString(" ")), style = small)
        Text(stringResource(R.string.debug_tail, debug.tail.joinToString(" ")), style = small)
        Text(
            stringResource(
                R.string.debug_align,
                state.cursor,
                debug.highWater,
                debug.missStreak,
                debug.lastMove?.name.orEmpty(),
                debug.score,
            ),
            style = small,
        )
        Text(
            stringResource(R.string.debug_engine, debug.activeSession?.toString() ?: "-", debug.engineState.name, debug.level),
            style = small,
        )
        Text(stringResource(R.string.debug_latency, debug.latency2Ms.orDash(), debug.latency3Ms.orDash()), style = small)
        Text(
            stringResource(
                R.string.debug_times,
                debug.installMs.orDash(),
                debug.prepareMs.orDash(),
                debug.beginSessionMs.orDash(),
                debug.firstPartialMs.orDash(),
            ),
            style = small,
        )
        Text(stringResource(R.string.debug_vocabulary, debug.vocabularySize?.toString() ?: "-"), style = small)

        Text(stringResource(R.string.preset), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PresetChoice.entries.forEach { preset ->
                FilterChip(
                    selected = state.settings.preset == preset,
                    onClick = { controller.setPreset(preset) },
                    enabled = idle,
                    label = { Text(stringResource(presetLabel(preset))) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.vocabulary))
            Switch(checked = state.settings.vocabulary, onCheckedChange = controller::setVocabulary, enabled = idle)
        }
        Text(stringResource(R.string.strategy), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RecognizerStrategy.entries.forEach { strategy ->
                FilterChip(
                    selected = state.settings.strategy == strategy,
                    onClick = { controller.setStrategy(strategy) },
                    enabled = idle,
                    label = { Text(stringResource(strategyLabel(strategy))) },
                )
            }
        }
        Text(stringResource(R.string.source), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = state.settings.source == AudioChoice.Microphone,
                onClick = { controller.setSource(AudioChoice.Microphone) },
                enabled = idle,
                label = { Text(stringResource(R.string.source_mic)) },
            )
            OutlinedButton(onClick = controller::refreshWavFiles, enabled = idle) { Text(stringResource(R.string.refresh_wav)) }
        }
        state.wavFiles.forEach { name ->
            FilterChip(
                selected = state.settings.source == AudioChoice.Wav(name),
                onClick = { controller.setSource(AudioChoice.Wav(name)) },
                enabled = idle,
                label = { Text(name) },
            )
        }
        Text(stringResource(R.string.level_threshold, state.settings.levelThreshold))
        Slider(value = state.settings.levelThreshold, onValueChange = controller::setLevelThreshold, valueRange = 0f..1f)
        Text(stringResource(R.string.animation, state.settings.animationMs))
        Slider(
            value = state.settings.animationMs.toFloat(),
            onValueChange = { controller.setAnimationMs(it.toInt()) },
            valueRange = DebugSettings.ANIMATION_MIN_MS.toFloat()..DebugSettings.ANIMATION_MAX_MS.toFloat(),
        )

        if (state.testTextId == null) {
            Text(stringResource(R.string.export_disabled), style = small)
        } else {
            OutlinedTextField(
                value = state.exportName,
                onValueChange = controller::setExportName,
                label = { Text(stringResource(R.string.export_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = controller::onExport, enabled = state.canExport) { Text(stringResource(R.string.export)) }
            OutlinedButton(onClick = controller::onStopReading, enabled = !idle) { Text(stringResource(R.string.stop)) }
        }
        if (state.exportFailed) Text(stringResource(R.string.export_failed), color = MaterialTheme.colorScheme.error)
        if (state.lastExport.isNotEmpty()) Text(stringResource(R.string.export_done, state.lastExport.joinToString()), style = small)
    }
}

private fun Long?.orDash(): String = this?.toString() ?: "-"

private fun presetLabel(preset: PresetChoice): Int =
    when (preset) {
        PresetChoice.Conservador -> R.string.preset_conservador
        PresetChoice.Normal -> R.string.preset_normal
        PresetChoice.Agil -> R.string.preset_agil
    }

private fun strategyLabel(strategy: RecognizerStrategy): Int =
    when (strategy) {
        RecognizerStrategy.NewPerSession -> R.string.strategy_new
        RecognizerStrategy.Reset -> R.string.strategy_reset
    }
