package io.github.gigioderico.tribuna.speech

/**
 * Costura da captura (spec 0003, D1 e D8): o microfone ou um WAV de teste, 16 kHz, mono, PCM 16 bits.
 * Interna ao app da etapa0; nÃƒÂ£o ÃƒÂ© contrato do TRD. Nada aqui grava ÃƒÂ¡udio (CLAUDE.md, regra 3).
 */
interface AudioSource {
    /** Abre a fonte. LanÃƒÂ§a [AudioSourceException] se nÃƒÂ£o abrir. */
    fun open()

    /**
     * LÃƒÂª atÃƒÂ© `buffer.size` amostras e devolve quantas leu (0 = nada agora).
     * LanÃƒÂ§a [AudioSourceException] se a fonte se perder (ex.: permissÃƒÂ£o revogada).
     */
    fun read(buffer: ShortArray): Int

    /** Fecha a fonte; pode ser chamada mais de uma vez. */
    fun close()
}

class AudioSourceException(
    val error: EngineError,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
