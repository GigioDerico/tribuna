package io.github.gigioderico.tribuna.etapa0.speech

sealed interface SpeechEvent {
    data class Partial(
        val session: SessionId,
        val words: List<String>,
    ) : SpeechEvent

    data class Final(
        val session: SessionId,
        val words: List<String>,
    ) : SpeechEvent

    data class Level(
        val rms: Float,
    ) : SpeechEvent // da captura, sem sessão

    data class Error(
        val session: SessionId?,
        val cause: EngineError,
    ) : SpeechEvent
}
