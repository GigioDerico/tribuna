package io.github.gigioderico.tribuna.speech

/**
 * Costura do reconhecedor (spec 0003, D1): o laÃƒÂ§o de captura e sessÃƒÂµes ÃƒÂ© testado na JVM com um
 * reconhecedor falso, sem carregar `org.vosk.*`. A implementaÃƒÂ§ÃƒÂ£o real fica em `VoskDecoders.kt`.
 */
interface Decoder {
    /** Entrega um bloco de ÃƒÂ¡udio; `true` = fim de frase, e o resultado sai em [resultJson]. */
    fun accept(
        samples: ShortArray,
        count: Int,
    ): Boolean

    /** JSON do parcial, ex.: `{"partial" : "ouvir bem"}`. */
    fun partialJson(): String

    /** JSON do resultado da frase, ex.: `{"text" : "ouvir bem"}`. */
    fun resultJson(): String

    /** Zera a hipÃƒÂ³tese para reaproveitar o reconhecedor (estratÃƒÂ©gia [RecognizerStrategy.Reset]). */
    fun reset()

    /** Libera o reconhecedor. */
    fun close()
}

/** Cria reconhecedores a partir do modelo carregado. */
interface DecoderFactory {
    val info: ModelInfo

    /** `vocabulary` nulo = sem lista; senÃƒÂ£o, a lista inteira (jÃƒÂ¡ com `[unk]`). LanÃƒÂ§a exceÃƒÂ§ÃƒÂ£o se nÃƒÂ£o criar. */
    fun create(vocabulary: List<String>?): Decoder
}

/** Dados do modelo carregado, para o painel e o `meta.txt` (spec 0003, D11, D18). */
class ModelInfo(
    /** ConteÃƒÂºdo da primeira linha do arquivo `uuid` do modelo instalado (D12). */
    val id: String,
    val installMs: Long,
    val loadMs: Long,
    /** LÃƒÂ©xico do modelo, lido sÃƒÂ³ quando o vocabulÃƒÂ¡rio restrito ÃƒÂ© ligado (OPR-020). */
    val lexicon: Lazy<Lexicon?>,
)

/** Carrega o modelo. LanÃƒÂ§a [ModelLoadException] com `ModelMissing` ou `ModelCorrupt` (D15). */
fun interface ModelLoader {
    fun load(): DecoderFactory
}

class ModelLoadException(
    val error: EngineError,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * O modelo ÃƒÂ© carregado uma vez por processo (TRD Motor de voz Ã‚Â§InstalaÃƒÂ§ÃƒÂ£o do modelo 4; spec 0003, C9).
 * Uma instÃƒÂ¢ncia por processo ÃƒÂ© compartilhada por todos os [VoskEngine]. Falha nÃƒÂ£o fica em cache:
 * a prÃƒÂ³xima chamada tenta de novo ("Tentar de novo", TRD Ã‚Â§InstalaÃƒÂ§ÃƒÂ£o 3).
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
