package io.github.gigioderico.tribuna.etapa0.speech

/** Decisão em aberto do TRD (Motor de voz §Captura e sessões), medida na 0005 (spec 0003, D4). */
enum class RecognizerStrategy {
    /** Um reconhecedor novo por sessão (texto principal do TRD; padrão). */
    NewPerSession,

    /** O mesmo reconhecedor com `reset()` e `SessionId` novo; reconhecedor novo se o vocabulário mudar. */
    Reset,
}
