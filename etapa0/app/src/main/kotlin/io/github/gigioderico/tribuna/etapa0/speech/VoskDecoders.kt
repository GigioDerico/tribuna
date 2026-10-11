package io.github.gigioderico.tribuna.etapa0.speech

import android.content.Context
import android.content.res.AssetManager
import org.vosk.LibVosk
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/** `AssetManager` como [AssetTree]. */
class AndroidAssetTree(
    private val assets: AssetManager,
) : AssetTree {
    override fun list(path: String): List<String> = assets.list(path)?.toList().orEmpty()

    override fun open(path: String): InputStream = assets.open(path)
}

/** Reconhecedores do Vosk sobre um modelo carregado uma vez (TRD §Instalação do modelo 4). */
class VoskDecoderFactory(
    private val model: Model,
    override val info: ModelInfo,
) : DecoderFactory {
    override fun create(vocabulary: List<String>?): Decoder {
        val rate = VoskEngine.SAMPLE_RATE.toFloat()
        val recognizer = if (vocabulary == null) Recognizer(model, rate) else Recognizer(model, rate, VoskJson.grammar(vocabulary))
        return VoskDecoder(recognizer)
    }
}

private class VoskDecoder(
    private val recognizer: Recognizer,
) : Decoder {
    override fun accept(
        samples: ShortArray,
        count: Int,
    ): Boolean = recognizer.acceptWaveForm(samples, count)

    override fun partialJson(): String = recognizer.partialResult

    override fun resultJson(): String = recognizer.result

    override fun reset() = recognizer.reset()

    override fun close() = recognizer.close()
}

/** Instala (se preciso) e carrega o `vosk-model-small-pt-0.3`; mede as duas etapas (spec 0003, D11, D18). */
class VoskModelLoader(
    private val context: Context,
) : ModelLoader {
    override fun load(): DecoderFactory {
        val installStart = System.nanoTime()
        val installed = ModelInstaller(AndroidAssetTree(context.assets), context.filesDir).install()
        val loadStart = System.nanoTime()
        val model =
            try {
                // -2: só erros. Os avisos do Kaldi citam palavras do vocabulário, que vêm do texto (Q6).
                LibVosk.vosk_set_log_level(KALDI_ERRORS_ONLY)
                Model(installed.dir.absolutePath)
            } catch (e: IOException) {
                throw ModelLoadException(EngineError.ModelCorrupt, "o Vosk não carregou o modelo", e)
            } catch (e: UnsatisfiedLinkError) {
                throw ModelLoadException(EngineError.ModelCorrupt, "biblioteca nativa do Vosk não carregou", e)
            }
        val done = System.nanoTime()
        val grammarFst = File(installed.dir, "Gr.fst")
        val info =
            ModelInfo(
                id = installed.id,
                installMs = (loadStart - installStart) / 1_000_000L,
                loadMs = (done - loadStart) / 1_000_000L,
                lexicon =
                    lazy {
                        try {
                            FileInputStream(grammarFst).use { Lexicon(FstSymbols.inputSymbols(it)) }
                        } catch (e: IOException) {
                            null
                        }
                    },
            )
        return VoskDecoderFactory(model, info)
    }

    private companion object {
        const val KALDI_ERRORS_ONLY = -2
    }
}

/** Um carregador por processo: o modelo é carregado uma vez e mantido enquanto o app vive. */
object VoskModels {
    @Volatile
    private var loader: CachingModelLoader? = null

    fun loader(context: Context): CachingModelLoader =
        loader ?: synchronized(this) {
            loader ?: CachingModelLoader(VoskModelLoader(context.applicationContext)).also { loader = it }
        }
}
