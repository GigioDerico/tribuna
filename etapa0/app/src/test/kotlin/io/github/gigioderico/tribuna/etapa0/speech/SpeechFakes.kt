package io.github.gigioderico.tribuna.etapa0.speech

/** Reconhecedor falso: guarda o que recebeu e devolve o JSON combinado. Nunca carrega `org.vosk.*` (D1). */
class FakeDecoder(
    val vocabulary: List<String>?,
) : Decoder {
    val fed = mutableListOf<ShortArray>()
    var partial = "{\"partial\" : \"\"}"
    var result = "{\"text\" : \"\"}"
    var endOfUtterance = false
    var failOnAccept = false
    var onAccept: (() -> Unit)? = null
    var resets = 0
    var resultCalls = 0
    var closed = false

    override fun accept(
        samples: ShortArray,
        count: Int,
    ): Boolean {
        check(!closed) { "reconhecedor liberado recebeu áudio" }
        fed += samples.copyOf(count)
        onAccept?.invoke()
        if (failOnAccept) throw IllegalStateException("falha simulada do reconhecedor")
        return endOfUtterance
    }

    override fun partialJson(): String = partial

    override fun resultJson(): String {
        resultCalls++
        return result
    }

    override fun reset() {
        resets++
    }

    override fun close() {
        closed = true
    }
}

class FakeDecoderFactory(
    override val info: ModelInfo = ModelInfo(id = "modelo-falso", installMs = 1, loadMs = 2, lexicon = lazyOf(null)),
) : DecoderFactory {
    val created = mutableListOf<FakeDecoder>()

    override fun create(vocabulary: List<String>?): Decoder = FakeDecoder(vocabulary).also { created += it }
}

/** Fonte de áudio roteirizada: cada `push` é um bloco; sem bloco, `read` devolve 0. */
class FakeAudioSource(
    private val openError: EngineError? = null,
) : AudioSource {
    private val blocks = ArrayDeque<ShortArray>()
    var opened = 0
    var closed = 0
    var readError: EngineError? = null
    var onRead: (() -> Unit)? = null

    fun push(block: ShortArray) {
        blocks.addLast(block)
    }

    override fun open() {
        openError?.let { throw AudioSourceException(it, "falha simulada ao abrir") }
        opened++
    }

    override fun read(buffer: ShortArray): Int {
        readError?.let { throw AudioSourceException(it, "falha simulada na leitura") }
        val block = blocks.removeFirstOrNull() ?: return 0
        block.copyInto(buffer)
        onRead?.invoke()
        return block.size
    }

    override fun close() {
        closed++
    }
}

/** O teste chama o passo do laço à mão: corridas entre captura e sessão ficam determinísticas. */
class ManualCaptureDriver : CaptureDriver {
    private var step: (() -> Boolean)? = null
    var stops = 0

    override fun start(step: () -> Boolean) {
        this.step = step
    }

    override fun stop() {
        stops++
    }

    /** Roda `times` passos; devolve `false` se o laço terminou. */
    fun pump(times: Int = 1): Boolean {
        repeat(times) {
            val current = step ?: return false
            if (!current()) {
                step = null
                return false
            }
        }
        return true
    }
}

fun block(
    value: Int,
    size: Int = 160,
): ShortArray = ShortArray(size) { value.toShort() }
