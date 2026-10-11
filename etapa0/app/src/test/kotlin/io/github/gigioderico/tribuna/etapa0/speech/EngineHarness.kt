package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** Monta um VoskEngine com fonte, reconhecedor e laço falsos, e coleta os eventos (spec 0003, D1). */
class EngineHarness(
    scope: TestScope,
    strategy: RecognizerStrategy = RecognizerStrategy.NewPerSession,
    val source: FakeAudioSource = FakeAudioSource(),
    val factory: FakeDecoderFactory = FakeDecoderFactory(),
    loader: ModelLoader = ModelLoader { factory },
) {
    val driver = ManualCaptureDriver()
    val engine =
        VoskEngine(
            modelLoader = loader,
            audioSource = { source },
            strategy = strategy,
            captureDriver = driver,
            loadDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
        )
    val events = mutableListOf<SpeechEvent>()

    init {
        scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { engine.events.collect { events += it } }
    }

    /** prepare + startCapture. */
    suspend fun open() {
        engine.prepare().getOrThrow()
        engine.startCapture()
    }

    /** Entrega um bloco à fonte e roda um passo do laço. */
    fun feed(block: ShortArray = block(1000)) {
        source.push(block)
        driver.pump()
    }

    fun recognitionEvents(): List<SpeechEvent> = events.filterNot { it is SpeechEvent.Level }
}
