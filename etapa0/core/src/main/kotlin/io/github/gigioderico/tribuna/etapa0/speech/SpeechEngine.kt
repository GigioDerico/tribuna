package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Separa a captura de áudio do reconhecimento (TRD §Contratos, SpeechEngine).
 * A captura fica aberta durante toda a leitura; o reconhecimento acontece em sessões,
 * e cada sessão tem um reconhecedor limpo.
 */
interface SpeechEngine {
    val events: Flow<SpeechEvent>
    val state: StateFlow<EngineState>

    suspend fun prepare(): Result<Unit> // carrega o modelo

    fun startCapture() // abre o microfone

    fun stopCapture() // fecha o microfone

    fun beginSession(vocabulary: List<String>? = null): SessionId

    fun endSession() // descarta hipóteses e áudio pendente

    fun release()
}
