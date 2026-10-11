package io.github.gigioderico.tribuna.etapa0.speech

/**
 * Quem roda o laço de captura (spec 0003, D1). Em produção, uma thread própria; nos testes, o teste chama
 * o passo à mão, o que torna determinísticas as corridas entre captura e `endSession`.
 */
interface CaptureDriver {
    /** Começa a chamar `step` até ele devolver `false`. */
    fun start(step: () -> Boolean)

    /** Pede para parar; volta depois que o passo em curso termina (exceto se chamado de dentro do passo). */
    fun stop()
}

/** Uma thread por captura, com o laço de `AudioRecord` do TRD (sem o serviço pronto do vosk-android). */
class ThreadCaptureDriver : CaptureDriver {
    @Volatile
    private var thread: Thread? = null

    override fun start(step: () -> Boolean) {
        val t =
            Thread({
                while (!Thread.currentThread().isInterrupted && step()) {
                    // o passo lê um bloco e o encaminha
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
        /** Uma leitura bloqueia no máximo um bloco (0,2 s); o limite só evita travar se o aparelho falhar. */
        const val STOP_TIMEOUT_MS = 1_000L
    }
}
