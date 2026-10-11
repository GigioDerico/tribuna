package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.export.ExportSink
import io.github.gigioderico.tribuna.etapa0.export.TestTextCatalog
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.EngineState
import io.github.gigioderico.tribuna.etapa0.speech.FakeSpeechEngine
import io.github.gigioderico.tribuna.etapa0.speech.ModelInfo
import io.github.gigioderico.tribuna.etapa0.speech.ModelLoadException
import io.github.gigioderico.tribuna.etapa0.speech.SessionId
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEngine
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import java.io.File
import java.io.IOException

/**
 * FakeSpeechEngine com registro das chamadas e do cursor no momento de cada uma (D4: ordem conferida no fake).
 * `failPrepare` faz o próximo prepare falhar com o erro dado, sem tocar no fake.
 */
class RecordingEngine(
    val fake: FakeSpeechEngine,
    val config: EngineConfig,
    private val cursor: () -> Int,
) : SpeechEngine {
    val calls = mutableListOf<String>()
    var failPrepare: EngineError? = null
    var failStartCapture: EngineError? = null

    /** Se definido, o prepare espera aqui: deixa o teste agir enquanto o modelo ainda carrega (OPR-027). */
    var prepareGate: CompletableDeferred<Unit>? = null

    /** Chamados dentro do prepare e do beginSession: deixam o teste gastar tempo do relógio do controlador. */
    var onPrepare: (() -> Unit)? = null
    var onBeginSession: (() -> Unit)? = null

    override val events: Flow<SpeechEvent> = fake.events
    override val state: StateFlow<EngineState> = fake.state

    override suspend fun prepare(): Result<Unit> {
        calls += "prepare"
        prepareGate?.await()
        onPrepare?.invoke()
        val error = failPrepare ?: return fake.prepare()
        failPrepare = null
        return Result.failure(ModelLoadException(error, "falha simulada"))
    }

    override fun startCapture() {
        calls += "startCapture"
        val error = failStartCapture ?: return fake.startCapture()
        // Como o VoskEngine: a fonte não abre, o estado fica Ready e sai um Error sem sessão.
        failStartCapture = null
        fake.emitCaptureError(error)
    }

    override fun stopCapture() {
        calls += "stopCapture"
        fake.stopCapture()
    }

    override fun beginSession(vocabulary: List<String>?): SessionId {
        calls += "beginSession@${cursor()}"
        onBeginSession?.invoke()
        return fake.beginSession(vocabulary)
    }

    override fun endSession() {
        calls += "endSession@${cursor()}"
        fake.endSession()
    }

    /** Como o VoskEngine quando a captura cai: o estado volta a Ready e sai um Error sem sessão (só sem sessão ativa). */
    fun loseCapture(error: EngineError) {
        fake.stopCapture()
        fake.emitCaptureError(error)
    }

    override fun release() {
        calls += "release"
        fake.release()
    }
}

/** ReaderController com FakeSpeechEngine, relógio virtual do kotlinx-coroutines-test e exportação em memória. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderHarness(
    private val scope: TestScope,
) {
    val engines = mutableListOf<RecordingEngine>()
    val exported = linkedMapOf<String, String>()

    /** Relógio do controlador = relógio virtual + `clockSkew`; com `tick > 0`, cada leitura do relógio avança `tick` ms. */
    var clockSkew = 0L
    var tick = 0L

    /** Faz a gravação da exportação falhar com IOException. */
    var failWrite = false
    val modelId: String = File(TestFiles.modelDir, "uuid").readLines().first().trim()
    private val dispatcher = StandardTestDispatcher(scope.testScheduler)

    val controller: ReaderController =
        ReaderController(
            scope = scope.backgroundScope,
            engineFactory = { config ->
                RecordingEngine(FakeSpeechEngine(), config) { controller.state.value.cursor }.also { engines += it }
            },
            catalog = TestTextCatalog(TestFiles.textIds.associateWith(TestFiles::text)),
            modelInfo = { ModelInfo(id = modelId, installMs = 1_234L, loadMs = 456L, lexicon = lazyOf(null)) },
            exportSink =
                ExportSink { name, files ->
                    if (failWrite) throw IOException("falha simulada na gravação")
                    files.map { (suffix, content) ->
                        "$name.$suffix".also { exported[it] = content }
                    }
                },
            provenance = Provenance("0.0.1", "0.3.75", "Fabricante Modelo", "16 (API 36)"),
            wavFiles = { listOf("T01_giorgio_sala_t1.wav") },
            clock = {
                clockSkew += tick
                scope.testScheduler.currentTime + clockSkew
            },
            processing = dispatcher,
            io = dispatcher,
        )

    val engine: RecordingEngine get() = engines.last()
    val fake: FakeSpeechEngine get() = engine.fake
    val state: ReaderUiState get() = controller.state.value

    /** Processa a fila sem avançar o relógio. */
    fun settle() = scope.testScheduler.runCurrent()

    /** Avança o relógio virtual e processa. */
    fun advance(ms: Long) {
        scope.testScheduler.advanceTimeBy(ms)
        scope.testScheduler.runCurrent()
    }

    fun load(id: String) {
        controller.loadTestText(id)
        settle()
    }

    fun listen() {
        controller.onListen()
        settle()
    }

    fun pause() {
        controller.onPause()
        settle()
    }

    fun partial(vararg words: String) {
        fake.hearPartial(words.toList())
        settle()
    }

    fun final(vararg words: String) {
        fake.hearFinal(words.toList())
        settle()
    }

    fun export(): Map<String, String> {
        controller.onExport()
        settle()
        return exported
    }

    /** Linhas de um arquivo exportado, sem o cabeçalho. */
    fun rows(file: String): List<String> =
        exported
            .getValue(file)
            .lines()
            .drop(1)
            .filter { it.isNotEmpty() }
}
