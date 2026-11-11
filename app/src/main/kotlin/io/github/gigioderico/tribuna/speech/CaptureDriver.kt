package io.github.gigioderico.tribuna.speech

/**
 * Quem roda o laÃƒÂ§o de captura (spec 0003, D1). Em produÃƒÂ§ÃƒÂ£o, uma thread prÃƒÂ³pria; nos testes, o teste chama
 * o passo ÃƒÂ  mÃƒÂ£o, o que torna determinÃƒÂ­sticas as corridas entre captura e `endSession`.
 */
interface CaptureDriver {
    /** ComeÃƒÂ§a a chamar `step` atÃƒÂ© ele devolver `false`. */
    fun start(step: () -> Boolean)

    /** Pede para parar; volta depois que o passo em curso termina (exceto se chamado de dentro do passo). */
    fun stop()
}

/** Uma thread por captura, com o laÃƒÂ§o de `AudioRecord` do TRD (sem o serviÃƒÂ§o pronto do vosk-android). */
class ThreadCaptureDriver : CaptureDriver {
    @Volatile
    private var thread: Thread? = null

    override fun start(step: () -> Boolean) {
        val t =
            Thread({
                while (!Thread.currentThread().isInterrupted && step()) {
                    // o passo lÃƒÂª um bloco e o encaminha
                }
            }, "tribuna-captura")
        thread = t
        t.start()
    }

    override fun stop() {
        val t = thread ?: return
        thread = null
        if (t !== Thread.currentThread()) t.join(STOP_TIMEOUT_MS)
    }

    private companion object {
        /** Uma leitura bloqueia no mÃƒÂ¡ximo um bloco (0,2 s); o limite sÃƒÂ³ evita travar se o aparelho falhar. */
        const val STOP_TIMEOUT_MS = 1_000L
    }
}
