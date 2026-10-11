package io.github.gigioderico.tribuna.etapa0.speech

/**
 * Costura do reconhecedor (spec 0003, D1): o laço de captura e sessões é testado na JVM com um
 * reconhecedor falso, sem carregar `org.vosk.*`. A implementação real fica em `VoskDecoders.kt`.
 */
interface Decoder {
    /** Entrega um bloco de áudio; `true` = fim de frase, e o resultado sai em [resultJson]. */
    fun accept(
        samples: ShortArray,
        count: Int,
    ): Boolean

    /** JSON do parcial, ex.: `{"partial" : "ouvir bem"}`. */
    fun partialJson(): String

    /** JSON do resultado da frase, ex.: `{"text" : "ouvir bem"}`. */
    fun resultJson(): String

    /** Zera a hipótese para reaproveitar o reconhecedor (estratégia [RecognizerStrategy.Reset]). */
    fun reset()

    /** Libera o reconhecedor. */
    fun close()
}

/** Cria reconhecedores a partir do modelo carregado. */
interface DecoderFactory {
    val info: ModelInfo

    /** `vocabulary` nulo = sem lista; senão, a lista inteira (já com `[unk]`). Lança exceção se não criar. */
    fun create(vocabulary: List<String>?): Decoder
}

/** Dados do modelo carregado, para o painel e o `meta.txt` (spec 0003, D11, D18). */
class ModelInfo(
    /** Conteúdo da primeira linha do arquivo `uuid` do modelo instalado (D12). */
    val id: String,
    val installMs: Long,
    val loadMs: Long,
    /** Léxico do modelo, lido só quando o vocabulário restrito é ligado (OPR-020). */
    val lexicon: Lazy<Lexicon?>,
)

/** Carrega o modelo. Lança [ModelLoadException] com `ModelMissing` ou `ModelCorrupt` (D15). */
fun interface ModelLoader {
    fun load(): DecoderFactory
}

class ModelLoadException(
    val error: EngineError,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * O modelo é carregado uma vez por processo (TRD Motor de voz §Instalação do modelo 4; spec 0003, C9).
 * Uma instância por processo é compartilhada por todos os [VoskEngine]. Falha não fica em cache:
 * a próxima chamada tenta de novo ("Tentar de novo", TRD §Instalação 3).
 */
class CachingModelLoader(
    private val delegate: ModelLoader,
) : ModelLoader {
    @Volatile
    var current: DecoderFactory? = null
        private set

    @Synchronized
    override fun load(): DecoderFactory = current ?: delegate.load().also { current = it }
}
