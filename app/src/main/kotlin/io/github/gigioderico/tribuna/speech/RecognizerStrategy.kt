package io.github.gigioderico.tribuna.speech

/** DecisÃƒÂ£o em aberto do TRD (Motor de voz Ã‚Â§Captura e sessÃƒÂµes), medida na 0005 (spec 0003, D4). */
enum class RecognizerStrategy {
    /** Um reconhecedor novo por sessÃƒÂ£o (texto principal do TRD; padrÃƒÂ£o). */
    NewPerSession,

    /** O mesmo reconhecedor com `reset()` e `SessionId` novo; reconhecedor novo se o vocabulÃƒÂ¡rio mudar. */
    Reset,
}
