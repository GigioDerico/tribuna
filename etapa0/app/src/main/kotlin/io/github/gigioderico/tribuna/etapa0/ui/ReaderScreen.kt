package io.github.gigioderico.tribuna.etapa0.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gigioderico.tribuna.etapa0.R
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.viewmodel.AudioChoice
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderController
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderPhase
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderUiState

/** Liga a tela ao controlador e pede o microfone na hora do Ouvir (TRD §Microfone). */
@Composable
fun ReaderRoute(controller: ReaderController) {
    val state by controller.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) controller.onListen() else controller.onPermissionDenied()
        }
    ReaderScreen(
        state = state,
        controller = controller,
        onListen = {
            val needsMic = state.settings.source == AudioChoice.Microphone
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (needsMic && !granted) permission.launch(Manifest.permission.RECORD_AUDIO) else controller.onListen()
        },
    )
}

/** Tela mínima da Etapa 0 (TRD Etapa 0, item 1): texto, Ouvir, Pausar e a palavra atual destacada. */
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    controller: ReaderController,
    onListen: () -> Unit,
) {
    var showPanel by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.phase == ReaderPhase.Idle) {
                Text(stringResource(R.string.test_texts), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.testTextIds) { id ->
                        AssistChip(onClick = { controller.loadTestText(id) }, label = { Text(id) })
                    }
                }
                // Estado local só em `remember` (nunca `rememberSaveable`): o texto do discurso não é salvo (regra 2).
                val sync = remember { FieldSync(state.text) }
                var field by remember { mutableStateOf(sync.value) }
                LaunchedEffect(state.text) { if (sync.received(state.text)) field = sync.value }
                OutlinedTextField(
                    value = field,
                    onValueChange = {
                        field = sync.typed(it)
                        controller.setText(it)
                    },
                    label = { Text(stringResource(R.string.text_label)) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } else {
                PromptText(
                    state = state,
                    onWordTapped = controller::onWordTapped,
                    onScrollSettled = controller::onScrollSettled,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
            if (state.phase == ReaderPhase.Preparing) Text(stringResource(R.string.preparing_model))
            state.error?.let { error ->
                Text(stringResource(errorText(error)), color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val listenLabel = if (state.error != null) R.string.try_again else R.string.listen
                Button(onClick = onListen, enabled = state.phase == ReaderPhase.Idle || state.phase == ReaderPhase.Paused) {
                    Text(stringResource(listenLabel))
                }
                Button(onClick = controller::onPause, enabled = state.phase == ReaderPhase.Reading) {
                    Text(stringResource(R.string.pause))
                }
                OutlinedButton(onClick = { showPanel = !showPanel }) { Text(stringResource(R.string.debug_panel)) }
            }
            if (showPanel) DebugPanel(state = state, controller = controller, modifier = Modifier.weight(1f))
        }
    }
}

private fun errorText(error: EngineError): Int =
    when (error) {
        EngineError.ModelMissing -> R.string.error_model_missing
        EngineError.ModelCorrupt -> R.string.error_model_corrupt
        EngineError.MicUnavailable -> R.string.error_mic_unavailable
        EngineError.PermissionRevoked -> R.string.error_permission
        EngineError.Internal -> R.string.error_internal
    }
