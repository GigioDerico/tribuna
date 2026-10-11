package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

/**
 * SpeechEngine com laço próprio de captura alimentando o reconhecedor, sem o serviço pronto do vosk-android
 * (TRD §Contratos, SpeechEngine e regras; Motor de voz §Captura e sessões; spec 0003, C1–C9).
 *
 * - A captura fica aberta na leitura e na pausa; fora de sessão, os blocos são lidos e descartados, e só o
 *   [SpeechEvent.Level] segue.
 * - `beginSession` cria o [SessionId] e o reconhecedor juntos, num [Session]; todo evento daquele reconhecedor
 *   sai com o id guardado no [Session], nunca com uma variável de "sessão atual" (CLAUDE.md, regra 7).
 * - `endSession` para de alimentar o reconhecedor, descarta o bloco ainda não entregue e libera o reconhecedor
 *   (estratégia nova) ou o zera com `reset()` (estratégia [RecognizerStrategy.Reset]). Não chama o resultado
 *   final: a hipótese pendente é descartada (D14).
 *
 * Estratégia e fonte de áudio vêm do construtor, não da interface (D4, D8). Thread-safe: os comandos vêm de
 * uma thread e o laço roda em outra.
 */
class VoskEngine(
    private val modelLoader: ModelLoader,
    private val audioSource: () -> AudioSource,
    private val strategy: RecognizerStrategy = RecognizerStrategy.NewPerSession,
    private val captureDriver: CaptureDriver = ThreadCaptureDriver(),
    private val loadDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val blockSamples: Int = BLOCK_SAMPLES,
) : SpeechEngine {
    private val channel = Channel<SpeechEvent>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow(EngineState.Idle)
    private val lock = Any()
    private var factory: DecoderFactory? = null
    private var capture: Capture? = null
    private var current: Session? = null
    private var spare: Spare? = null
    private var nextSession = 1L
    private var released = false

    override val events: Flow<SpeechEvent> = channel.receiveAsFlow()
    override val state: StateFlow<EngineState> = mutableState.asStateFlow()

    override suspend fun prepare(): Result<Unit> {
        synchronized(lock) {
            check(!released) { "VoskEngine já liberado" }
            if (factory != null) return Result.success(Unit)
            mutableState.value = EngineState.Preparing
        }
        return try {
            val loaded = withContext(loadDispatcher) { modelLoader.load() }
            synchronized(lock) {
                factory = loaded
                mutableState.value = EngineState.Ready
            }
            Result.success(Unit)
        } catch (e: ModelLoadException) {
            mutableState.value = EngineState.Failed
            Result.failure(e)
        }
    }

    override fun startCapture() {
        val started: Capture
        synchronized(lock) {
            check(!released) { "VoskEngine já liberado" }
            check(mutableState.value == EngineState.Ready) { "startCapture exige Ready; estado: ${mutableState.value}" }
            val source = audioSource()
            try {
                source.open()
            } catch (e: AudioSourceException) {
                channel.trySend(SpeechEvent.Error(null, e.error))
                return
            }
            started = Capture(source)
            capture = started
            mutableState.value = EngineState.Capturing
        }
        captureDriver.start(started::step)
    }

    override fun stopCapture() {
        val stopped: Capture
        val ended: Session?
        synchronized(lock) {
            stopped = capture ?: return
            capture = null
            ended = current
            current = null
            mutableState.value = EngineState.Ready
        }
        ended?.end()
        stopped.active = false
        captureDriver.stop()
    }

    override fun beginSession(vocabulary: List<String>?): SessionId {
        val id: SessionId
        val reused: Decoder?
        val loaded: DecoderFactory
        synchronized(lock) {
            check(mutableState.value == EngineState.Capturing && current == null) {
                "beginSession exige captura aberta e nenhuma sessão ativa; estado: ${mutableState.value}"
            }
            loaded = checkNotNull(factory) { "beginSession antes de prepare" }
            id = SessionId(nextSession++)
            val kept = spare
            spare = null
            reused = if (kept != null && kept.vocabulary == vocabulary) kept.decoder else null
            if (kept != null && reused == null) kept.decoder.close()
        }
        val decoder =
            reused ?: try {
                loaded.create(vocabulary)
            } catch (e: Exception) {
                null
            }
        synchronized(lock) {
            current = Session(id, decoder, vocabulary)
            mutableState.value = EngineState.Recognizing
        }
        if (decoder == null) channel.trySend(SpeechEvent.Error(id, EngineError.Internal))
        return id
    }

    override fun endSession() {
        val ended: Session
        synchronized(lock) {
            ended = current ?: return
            current = null
            mutableState.value = EngineState.Capturing
        }
        ended.end()
    }

    override fun release() {
        stopCapture()
        val kept: Spare?
        synchronized(lock) {
            released = true
            kept = spare
            spare = null
            mutableState.value = EngineState.Idle
        }
        kept?.decoder?.close()
        channel.close()
    }

    /** O reconhecedor de uma sessão terminou de ser usado: libera ou guarda para `reset()` (D4). */
    private fun recycle(
        decoder: Decoder,
        vocabulary: List<String>?,
    ) {
        if (strategy == RecognizerStrategy.NewPerSession) {
            decoder.close()
            return
        }
        decoder.reset()
        val keep =
            synchronized(lock) {
                if (spare == null && !released) {
                    spare = Spare(decoder, vocabulary)
                    true
                } else {
                    false
                }
            }
        if (!keep) decoder.close()
    }

    private fun captureLost(
        lost: Capture,
        error: EngineError,
    ) {
        val ended: Session?
        synchronized(lock) {
            if (capture !== lost) return
            capture = null
            ended = current
            current = null
            mutableState.value = EngineState.Ready
        }
        ended?.end()
        channel.trySend(SpeechEvent.Error(null, error))
    }

    private class Spare(
        val decoder: Decoder,
        val vocabulary: List<String>?,
    )

    /** Uma captura aberta. O laço chama [step] até ela devolver `false`; quem fecha a fonte é o próprio laço. */
    private inner class Capture(
        private val source: AudioSource,
    ) {
        @Volatile
        var active = true

        private val buffer = ShortArray(blockSamples)

        fun step(): Boolean {
            if (!active) return close()
            val count =
                try {
                    source.read(buffer)
                } catch (e: AudioSourceException) {
                    return fail(e.error)
                }
            if (!active) return close() // parou durante a leitura: o bloco é descartado
            if (count < 0) return fail(EngineError.MicUnavailable)
            if (count == 0) return true
            channel.trySend(SpeechEvent.Level(AudioLevel.rms(buffer, count)))
            val session = synchronized(lock) { current } // para onde vai o áudio, não o id dos eventos
            session?.feed(buffer, count)
            return true
        }

        private fun close(): Boolean {
            source.close()
            return false
        }

        private fun fail(error: EngineError): Boolean {
            source.close()
            active = false
            captureLost(this, error)
            return false
        }
    }

    /**
     * Um reconhecedor e o seu id, criados juntos. Todo evento sai com [id]; um evento produzido depois do
     * `endSession` (o bloco já estava no reconhecedor) sai com o id antigo e é descartado por quem consome.
     */
    private inner class Session(
        val id: SessionId,
        private val decoder: Decoder?,
        private val vocabulary: List<String>?,
    ) {
        private var feeding = false
        private var ended = false
        private var failed = false
        private var decoderReleased = false

        fun feed(
            samples: ShortArray,
            count: Int,
        ) {
            val active = decoder ?: return
            synchronized(this) {
                if (ended) return // bloco lido antes do endSession e ainda não entregue: descartado (C3)
                feeding = true
            }
            try {
                val event =
                    try {
                        if (active.accept(samples, count)) {
                            SpeechEvent.Final(id, VoskJson.words(active.resultJson()))
                        } else {
                            SpeechEvent.Partial(id, VoskJson.words(active.partialJson()))
                        }
                    } catch (e: Exception) {
                        synchronized(this) { failed = true }
                        SpeechEvent.Error(id, EngineError.Internal)
                    }
                channel.trySend(event)
            } finally {
                val releaseNow =
                    synchronized(this) {
                        feeding = false
                        if (failed) ended = true // reconhecedor com exceção não recebe mais áudio
                        ended
                    }
                if (releaseNow) release(active)
            }
        }

        fun end() {
            val releaseNow =
                synchronized(this) {
                    if (ended) return
                    ended = true
                    !feeding
                }
            if (releaseNow) decoder?.let(::release)
        }

        private fun release(active: Decoder) {
            val broken =
                synchronized(this) {
                    if (decoderReleased) return
                    decoderReleased = true
                    failed
                }
            if (broken) active.close() else recycle(active, vocabulary)
        }
    }

    companion object {
        const val SAMPLE_RATE = 16_000

        /** 0,2 s de áudio por bloco, o mesmo do laço de referência do vosk-android 0.3.75 (`BUFFER_SIZE_SECONDS`). */
        const val BLOCK_SAMPLES = 3_200
    }
}
