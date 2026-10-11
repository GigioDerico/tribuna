package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.align.AlignConstants
import io.github.gigioderico.tribuna.etapa0.align.AlignState
import io.github.gigioderico.tribuna.etapa0.align.Aligner
import io.github.gigioderico.tribuna.etapa0.align.Move
import io.github.gigioderico.tribuna.etapa0.align.PartialReconciler
import io.github.gigioderico.tribuna.etapa0.align.Tokenizer
import io.github.gigioderico.tribuna.etapa0.export.ExportFiles
import io.github.gigioderico.tribuna.etapa0.export.ExportName
import io.github.gigioderico.tribuna.etapa0.export.ExportSink
import io.github.gigioderico.tribuna.etapa0.export.Recording
import io.github.gigioderico.tribuna.etapa0.export.TestTextCatalog
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.EngineState
import io.github.gigioderico.tribuna.etapa0.speech.ModelInfo
import io.github.gigioderico.tribuna.etapa0.speech.ModelLoadException
import io.github.gigioderico.tribuna.etapa0.speech.RecognizerStrategy
import io.github.gigioderico.tribuna.etapa0.speech.SessionId
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEngine
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEvent
import io.github.gigioderico.tribuna.etapa0.speech.Vocabulary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/** Configuração de construção do motor: estratégia e fonte vêm do construtor do VoskEngine (spec 0003, D4, D8). */
data class EngineConfig(
    val strategy: RecognizerStrategy,
    val source: AudioChoice,
)

/** Ambiente para o `meta.txt` (D11). */
data class Provenance(
    val appVersion: String,
    val voskVersion: String,
    val deviceModel: String,
    val androidVersion: String,
)

/**
 * Lógica da tela mínima da etapa0 (spec 0003, D1–D9; TRD Reconciliação 7, Máquina de estados nas linhas
 * Reading/Paused que a tela usa, Segundo plano). Kotlin puro, testado na JVM com o `FakeSpeechEngine`.
 *
 * Único dono do texto, do cursor e da leitura. O texto vive só aqui, em memória (CLAUDE.md, regra 2).
 * Todo estado muda numa só fila (`processing` com paralelismo 1), fora da thread principal (TRD §Captura e
 * sessões): comandos da tela e eventos do motor nunca correm juntos.
 */
class ReaderController(
    private val scope: CoroutineScope,
    private val engineFactory: (EngineConfig) -> SpeechEngine,
    private val catalog: TestTextCatalog,
    private val modelInfo: () -> ModelInfo?,
    private val exportSink: ExportSink,
    private val provenance: Provenance,
    private val wavFiles: () -> List<String>,
    private val clock: () -> Long,
    processing: CoroutineDispatcher,
    private val io: CoroutineDispatcher,
) {
    private val confined = processing.limitedParallelism(1)
    private val mutableState = MutableStateFlow(ReaderUiState(testTextIds = catalog.ids))
    val state: StateFlow<ReaderUiState> = mutableState.asStateFlow()

    private var engine: SpeechEngine = createEngine()
    private var collector: Job? = null
    private var aligner: Aligner? = null
    private var align: AlignState? = null
    private val reconciler = PartialReconciler()
    private val tail = ArrayDeque<String>()
    private var active: SessionId? = null
    private var lastLevel = 0f
    private var sessionStartedAt = 0L
    private var firstPartialSeen = false
    private var recording: Recording? = null
    private var backgrounded = false
    private var vocabulary: List<String>? = null

    init {
        collectEvents()
    }

    // ---- comandos da tela ----

    fun setText(text: String) =
        enqueue {
            if (mutableState.value.phase != ReaderPhase.Idle) return@enqueue
            val id = catalog.identify(text)
            recording = null
            vocabulary = null
            mutableState.update {
                it.copy(
                    text = text,
                    tokens = Tokenizer.tokenize(text),
                    cursor = 0,
                    testTextId = id,
                    exportName = id.orEmpty(),
                    canExport = false,
                    lastExport = emptyList(),
                )
            }
        }

    fun loadTestText(id: String) {
        val text = catalog.text(id) ?: return
        setText(text)
    }

    fun onListen() =
        enqueue {
            when (mutableState.value.phase) {
                ReaderPhase.Idle -> startReading()
                ReaderPhase.Paused -> resume(fromTap = true)
                else -> Unit
            }
        }

    fun onPause() =
        enqueue {
            if (mutableState.value.phase != ReaderPhase.Reading) return@enqueue
            endSession()
            recording?.event(clock(), Recording.PAUSE_TAP)
            mutableState.update { it.copy(phase = ReaderPhase.Paused) }
        }

    /** Encerra a gravação: fecha a sessão e a captura; a exportação fica disponível. */
    fun onStopReading() =
        enqueue {
            val phase = mutableState.value.phase
            if (phase != ReaderPhase.Reading && phase != ReaderPhase.Paused) return@enqueue
            endSession()
            stopCapture()
            mutableState.update { it.copy(phase = ReaderPhase.Idle) }
        }

    /** Toque em palavra (D10; TRD Máquina de estados, Reading e Paused): endSession, reposition, beginSession. */
    fun onWordTapped(index: Int) =
        enqueue {
            val phase = mutableState.value.phase
            if (phase != ReaderPhase.Reading && phase != ReaderPhase.Paused) return@enqueue
            endSession()
            reposition(index)
            recording?.event(clock(), Recording.WORD_TAP, index.toString())
            resume(fromTap = false)
        }

    /** `onStop` da Activity (TRD §Segundo plano; D16). */
    fun onActivityStop(changingConfigurations: Boolean) =
        enqueue {
            if (changingConfigurations) return@enqueue
            val phase = mutableState.value.phase
            if (phase != ReaderPhase.Reading && phase != ReaderPhase.Paused) return@enqueue
            endSession()
            stopCapture()
            backgrounded = true
            mutableState.update { it.copy(phase = ReaderPhase.Paused) }
        }

    /** `onStart` da Activity: na volta do segundo plano, a leitura continua pausada e a captura reabre. */
    fun onActivityStart() =
        enqueue {
            if (!backgrounded) return@enqueue
            backgrounded = false
            if (mutableState.value.phase == ReaderPhase.Paused) startCapture()
        }

    fun onPermissionDenied() = enqueue { mutableState.update { it.copy(error = EngineError.PermissionRevoked) } }

    fun setPreset(preset: PresetChoice) = updateIdleSettings { it.copy(preset = preset) }

    fun setVocabulary(enabled: Boolean) = updateIdleSettings { it.copy(vocabulary = enabled) }

    fun setStrategy(strategy: RecognizerStrategy) = updateIdleSettings(recreateEngine = true) { it.copy(strategy = strategy) }

    fun setSource(source: AudioChoice) = updateIdleSettings(recreateEngine = true) { it.copy(source = source) }

    fun setLevelThreshold(threshold: Float) =
        enqueue { mutableState.update { it.copy(settings = it.settings.copy(levelThreshold = threshold.coerceIn(0f, 1f))) } }

    fun setAnimationMs(ms: Int) =
        enqueue {
            val clamped = ms.coerceIn(DebugSettings.ANIMATION_MIN_MS, DebugSettings.ANIMATION_MAX_MS)
            mutableState.update { it.copy(settings = it.settings.copy(animationMs = clamped)) }
        }

    fun setExportName(name: String) = enqueue { mutableState.update { it.copy(exportName = name) } }

    fun refreshWavFiles() = enqueue { mutableState.update { it.copy(wavFiles = wavFiles()) } }

    /** Fim da animação da tela para o cursor `cursor`: latência 3 (D18). */
    fun onScrollSettled(cursor: Int) =
        enqueue {
            val current = mutableState.value
            if (current.cursor != cursor) return@enqueue
            mutableState.update { it.copy(debug = it.debug.copy(latency3Ms = clock() - current.cursorAtMs)) }
        }

    /** Grava os cinco arquivos (D7) se o texto é de teste e o nome é válido. */
    fun onExport() =
        enqueue {
            val rec = recording ?: return@enqueue
            val current = mutableState.value
            if (!ExportName.isValid(current.exportName, rec.textId)) {
                mutableState.update { it.copy(exportFailed = true, lastExport = emptyList()) }
                return@enqueue
            }
            val files = ExportFiles.build(rec)
            val written =
                try {
                    withContext(io) { exportSink.write(current.exportName, files) }
                } catch (e: IOException) {
                    null
                }
            mutableState.update { it.copy(lastExport = written.orEmpty(), exportFailed = written == null) }
        }

    /** Fim do ViewModel. */
    fun close() {
        collector?.cancel()
        engine.release()
    }

    // ---- leitura ----

    private suspend fun startReading() {
        val current = mutableState.value
        if (current.tokens.isEmpty()) return
        mutableState.update { it.copy(phase = ReaderPhase.Preparing, error = null, lastExport = emptyList()) }
        val started = clock()
        val prepared = engine.prepare()
        val prepareMs = clock() - started
        val info = modelInfo()
        mutableState.update { it.copy(debug = it.debug.copy(prepareMs = prepareMs, installMs = info?.installMs)) }
        if (prepared.isFailure) {
            val cause = (prepared.exceptionOrNull() as? ModelLoadException)?.error ?: EngineError.Internal
            mutableState.update { it.copy(phase = ReaderPhase.Idle, error = cause) }
            return
        }
        if (!startCapture()) {
            mutableState.update { it.copy(phase = ReaderPhase.Idle) }
            return
        }
        recording = current.testTextId?.let { Recording(it, metaFor(current, info, prepareMs), originMs = clock()) }
        aligner = Aligner(current.tokens, current.settings.preset.params)
        reposition(0)
        beginSession()
        mutableState.update { it.copy(phase = ReaderPhase.Reading, canExport = recording != null) }
    }

    private fun resume(fromTap: Boolean) {
        if (!mutableState.value.capturing && !startCapture()) {
            mutableState.update { it.copy(phase = ReaderPhase.Paused) }
            return
        }
        beginSession()
        if (fromTap) recording?.event(clock(), Recording.RESUME_TAP)
        mutableState.update { it.copy(phase = ReaderPhase.Reading, error = null) }
    }

    private fun startCapture(): Boolean {
        engine.startCapture()
        val ok = engine.state.value == EngineState.Capturing
        mutableState.update { it.copy(capturing = ok) }
        return ok
    }

    private fun stopCapture() {
        if (!mutableState.value.capturing) return
        val engineState = engine.state.value
        if (engineState == EngineState.Capturing || engineState == EngineState.Recognizing) engine.stopCapture()
        mutableState.update { it.copy(capturing = false) }
    }

    private fun beginSession() {
        reconciler.reset()
        tail.clear()
        val current = mutableState.value
        val words = if (current.settings.vocabulary) vocabularyFor(current) else null
        val started = clock()
        val id = engine.beginSession(words)
        val now = clock()
        active = id
        sessionStartedAt = now
        firstPartialSeen = false
        mutableState.update {
            it.copy(
                debug =
                    it.debug.copy(
                        activeSession = id.value,
                        beginSessionMs = now - started,
                        firstPartialMs = null,
                        tail = emptyList(),
                        vocabularySize = words?.size,
                    ),
            )
        }
    }

    private fun endSession() {
        if (active == null) return
        engine.endSession()
        active = null
        mutableState.update { it.copy(debug = it.debug.copy(activeSession = null)) }
    }

    private fun reposition(index: Int) {
        val currentAligner = aligner ?: return
        // A cauda e o reconciliador zeram no beginSession que sempre vem depois (0002 D1).
        publish(currentAligner.reposition(index), clock())
    }

    private fun vocabularyFor(current: ReaderUiState): List<String> =
        vocabulary ?: Vocabulary.build(current.text, current.tokens, modelInfo()?.lexicon?.value).also { vocabulary = it }

    // ---- eventos do motor ----

    private fun collectEvents() {
        val source = engine
        collector = scope.launch(confined) { source.events.collect(::handle) }
    }

    private fun handle(event: SpeechEvent) {
        val now = clock()
        when (event) {
            is SpeechEvent.Level -> onLevel(now, event)
            is SpeechEvent.Partial -> onHypothesis(now, event, event.session, event.words, isFinal = false)
            is SpeechEvent.Final -> onHypothesis(now, event, event.session, event.words, isFinal = true)
            is SpeechEvent.Error -> onError(now, event)
        }
    }

    private fun onLevel(
        now: Long,
        event: SpeechEvent.Level,
    ) {
        lastLevel = event.rms
        recording?.level(now, event.rms)
        mutableState.update { it.copy(debug = it.debug.copy(level = event.rms, engineState = engine.state.value)) }
    }

    private fun onHypothesis(
        now: Long,
        event: SpeechEvent,
        session: SessionId,
        words: List<String>,
        isFinal: Boolean,
    ) {
        val threshold = mutableState.value.settings.levelThreshold
        // Descarta evento de sessão que não é a ativa antes do reconciliador (TRD Reconciliação 7; regra 7).
        val fromActive = session == active
        // Limiar de nível só para parciais (TRD §Captura e sessões; D3, D5).
        val passesLevel = isFinal || lastLevel >= threshold
        val accepted = fromActive && passesLevel
        recording?.hypothesis(now, event, accepted)
        mutableState.update {
            it.copy(
                debug = if (isFinal) it.debug.copy(lastFinal = words) else it.debug.copy(lastPartial = words),
            )
        }
        if (!accepted) return
        if (!isFinal && !firstPartialSeen && words.isNotEmpty()) {
            firstPartialSeen = true
            mutableState.update { it.copy(debug = it.debug.copy(firstPartialMs = now - sessionStartedAt)) }
        }
        val fresh = if (isFinal) reconciler.onFinal(words) else reconciler.onPartial(words)
        if (fresh.isEmpty()) return
        fresh.forEach(tail::addLast)
        val keep = maxOf(mutableState.value.settings.preset.params.k, AlignConstants.RECOVERY_WORDS)
        while (tail.size > keep) tail.removeFirst()
        val currentAligner = aligner ?: return
        val currentAlign = align ?: return
        publish(currentAligner.advance(currentAlign, tail.toList()), now)
    }

    private fun onError(
        now: Long,
        event: SpeechEvent.Error,
    ) {
        // Erro da captura (sem sessão) ou da sessão ativa; erro de sessão antiga é descartado (regra 7).
        val fromCapture = event.session == null
        val accepted = fromCapture || event.session == active
        recording?.hypothesis(now, event, accepted)
        if (!accepted) return
        val phase = mutableState.value.phase
        endSession()
        if (fromCapture) stopCapture()
        // Na tela, um erro pausa, mostra o motivo e oferece "Tentar de novo" (D15).
        val next = if (phase == ReaderPhase.Reading || phase == ReaderPhase.Paused) ReaderPhase.Paused else phase
        mutableState.update { it.copy(phase = next, error = event.cause) }
    }

    /** Publica o novo estado do alinhador; grava a trajetória a cada reposition e a cada mudança de cursor (D7). */
    private fun publish(
        next: AlignState,
        receivedAt: Long,
    ) {
        val previous = align
        align = next
        if (next.lastMove == Move.Reposition || next.cursor != previous?.cursor) {
            recording?.trajectory(clock(), next.cursor, next.lastMove, next.score)
        }
        val publishedAt = clock()
        mutableState.update {
            it.copy(
                cursor = next.cursor,
                cursorAtMs = if (next.cursor != it.cursor || next.lastMove == Move.Reposition) publishedAt else it.cursorAtMs,
                debug =
                    it.debug.copy(
                        tail = tail.toList(),
                        score = next.score,
                        lastMove = next.lastMove,
                        highWater = next.highWater,
                        missStreak = next.missStreak,
                        latency2Ms = if (next.lastMove == Move.Reposition) it.debug.latency2Ms else publishedAt - receivedAt,
                    ),
            )
        }
    }

    // ---- ajustes ----

    private fun updateIdleSettings(
        recreateEngine: Boolean = false,
        change: (DebugSettings) -> DebugSettings,
    ) = enqueue {
        if (mutableState.value.phase != ReaderPhase.Idle) return@enqueue
        mutableState.update { it.copy(settings = change(it.settings)) }
        if (recreateEngine) replaceEngine()
    }

    private fun replaceEngine() {
        collector?.cancel()
        engine.release()
        engine = createEngine()
        collectEvents()
    }

    private fun createEngine(): SpeechEngine {
        val settings = mutableState.value.settings
        return engineFactory(EngineConfig(settings.strategy, settings.source))
    }

    private fun metaFor(
        current: ReaderUiState,
        info: ModelInfo?,
        prepareMs: Long,
    ): Map<String, String> {
        val settings = current.settings
        val source = settings.source
        return mapOf(
            "app_version" to provenance.appVersion,
            "vosk_android_version" to provenance.voskVersion,
            "model_id" to info?.id.orEmpty(),
            "text_id" to current.testTextId.orEmpty(),
            "preset" to settings.preset.name,
            "vocabulary" to if (settings.vocabulary) "on" else "off",
            "recognizer_strategy" to settings.strategy.name,
            "audio_source" to if (source is AudioChoice.Wav) "wav" else "mic",
            "wav_name" to (source as? AudioChoice.Wav)?.name.orEmpty(),
            "level_threshold" to settings.levelThreshold.toString(),
            "animation_ms" to settings.animationMs.toString(),
            "install_ms" to info?.installMs?.toString().orEmpty(),
            "prepare_ms" to prepareMs.toString(),
            "device_model" to provenance.deviceModel,
            "android_version" to provenance.androidVersion,
        )
    }

    /** Toda mudança de estado passa por aqui: uma fila só, fora da thread principal. */
    private fun enqueue(block: suspend () -> Unit) {
        scope.launch(confined) { block() }
    }
}
