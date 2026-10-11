package io.github.gigioderico.tribuna.etapa0.viewmodel

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.gigioderico.tribuna.etapa0.BuildConfig
import io.github.gigioderico.tribuna.etapa0.export.ExportWriter
import io.github.gigioderico.tribuna.etapa0.export.TestTextCatalog
import io.github.gigioderico.tribuna.etapa0.speech.AndroidAssetTree
import io.github.gigioderico.tribuna.etapa0.speech.MicAudioSource
import io.github.gigioderico.tribuna.etapa0.speech.VoskEngine
import io.github.gigioderico.tribuna.etapa0.speech.VoskModels
import io.github.gigioderico.tribuna.etapa0.speech.WavAudioSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * ViewModel da tela mínima: sobrevive à rotação e guarda o [ReaderController], onde o texto vive em memória
 * (TRD §Onde o texto vive; spec 0003, D16). Nada vai para o estado salvo da Activity.
 */
class ReaderViewModel(
    createController: (CoroutineScope) -> ReaderController,
) : ViewModel() {
    val controller: ReaderController = createController(viewModelScope)

    override fun onCleared() {
        controller.close()
    }

    companion object {
        private const val EXPORT_DIR = "exportacoes"
        private const val WAV_DIR = "wav"

        /** Monta as dependências reais (Vosk, assets, diretórios externos do app). */
        fun factory(context: Context) =
            viewModelFactory {
                initializer {
                    val app = context.applicationContext
                    val wavDir = app.getExternalFilesDir(WAV_DIR)
                    ReaderViewModel { scope ->
                        ReaderController(
                            scope = scope,
                            engineFactory = { config ->
                                VoskEngine(
                                    modelLoader = VoskModels.loader(app),
                                    audioSource = {
                                        when (val source = config.source) {
                                            AudioChoice.Microphone -> MicAudioSource(app)
                                            is AudioChoice.Wav -> WavAudioSource(File(wavDir ?: app.filesDir, source.name))
                                        }
                                    },
                                    strategy = config.strategy,
                                )
                            },
                            catalog = TestTextCatalog.fromAssets(AndroidAssetTree(app.assets)),
                            modelInfo = { VoskModels.loader(app).current?.info },
                            exportSink = ExportWriter { app.getExternalFilesDir(EXPORT_DIR) },
                            provenance =
                                Provenance(
                                    appVersion = BuildConfig.VERSION_NAME,
                                    voskVersion = BuildConfig.VOSK_VERSION,
                                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                                    androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                                ),
                            wavFiles = {
                                wavDir
                                    ?.listFiles { file -> file.name.endsWith(".wav") }
                                    ?.map { it.name }
                                    ?.sorted()
                                    .orEmpty()
                            },
                            clock = SystemClock::elapsedRealtime,
                            processing = Dispatchers.Default,
                            io = Dispatchers.IO,
                        )
                    }
                }
            }
    }
}
