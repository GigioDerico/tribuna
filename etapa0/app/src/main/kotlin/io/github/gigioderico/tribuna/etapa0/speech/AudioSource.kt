package io.github.gigioderico.tribuna.etapa0.speech

/**
 * Costura da captura (spec 0003, D1 e D8): o microfone ou um WAV de teste, 16 kHz, mono, PCM 16 bits.
 * Interna ao app da etapa0; não é contrato do TRD. Nada aqui grava áudio (CLAUDE.md, regra 3).
 */
interface AudioSource {
    /** Abre a fonte. Lança [AudioSourceException] se não abrir. */
    fun open()

    /**
     * Lê até `buffer.size` amostras e devolve quantas leu (0 = nada agora).
     * Lança [AudioSourceException] se a fonte se perder (ex.: permissão revogada).
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
