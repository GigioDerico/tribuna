package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guardas de estado, captura perdida e reuso do reconhecedor no VoskEngine (TRD §Contratos, SpeechEngine e regras;
 * Motor de voz §Captura e sessões; spec 0003, C1-C9). Cada teste nasceu de uma quebra mínima do revisor ou da
 * varredura da rodada 2 que ficava verde (evidencias/quebras-q2-rodada2-motor.txt).
 */
class VoskEngineRobustnessTest {
    private class Scripted(
        private val onRead: (Int) -> Int,
    ) : AudioSource {
        var reads = 0
        var closed = 0

        override fun open() = Unit

        override fun read(buffer: ShortArray): Int = onRead(++reads)

        override fun close() {
            closed++
        }
    }

    /** Guarda todos os passos de laço já iniciados, para o teste rodar o de uma captura antiga. */
    private class RecordingDriver : CaptureDriver {
        val steps = mutableListOf<() -> Boolean>()

        override fun start(step: () -> Boolean) {
            steps += step
        }

        override fun stop() = Unit
    }

    private fun TestScope.raw(
        source: AudioSource,
        driver: CaptureDriver,
        events: MutableList<SpeechEvent> = mutableListOf(),
    ): VoskEngine {
        val factory = FakeDecoderFactory()
        val engine =
            VoskEngine(
                modelLoader = { factory },
                audioSource = { source },
                captureDriver = driver,
                loadDispatcher = UnconfinedTestDispatcher(testScheduler),
            )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { engine.events.collect { events += it } }
        return engine
    }

    // ---- estados e guardas ----

    @Test
    fun prepareDepoisDoRelease_lanca() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.release()
            assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { h.engine.prepare() } }
        }

    @Test
    fun startCaptureForaDoEstadoReady_lanca() =
        runTest {
            val h = EngineHarness(this)
            assertThrows(IllegalStateException::class.java) { h.engine.startCapture() } // Idle
            h.engine.prepare()
            h.engine.startCapture()
            assertThrows(IllegalStateException::class.java) { h.engine.startCapture() } // Capturing
            h.engine.beginSession()
            assertThrows(IllegalStateException::class.java) { h.engine.startCapture() } // Recognizing
            val failed =
                EngineHarness(this, loader = { throw ModelLoadException(EngineError.ModelCorrupt, "falha simulada") })
            failed.engine.prepare()
            assertEquals(EngineState.Failed, failed.engine.state.value)
            assertThrows(IllegalStateException::class.java) { failed.engine.startCapture() } // Failed
        }

    @Test
    fun beginSessionComSessaoAtiva_lanca() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            assertThrows(IllegalStateException::class.java) { h.engine.beginSession() }
        }

    @Test
    fun stopCaptureDuasVezes_chamaODriverUmaVez() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.stopCapture()
            h.engine.stopCapture()
            assertEquals(1, h.driver.stops)
            assertEquals(EngineState.Ready, h.engine.state.value)
        }

    @Test
    fun stopCaptureComSessao_novaCapturaPermiteNovaSessao() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            h.engine.stopCapture()
            h.engine.startCapture()
            h.engine.beginSession() // não lança: a sessão antiga não ficou como atual
            assertEquals(EngineState.Recognizing, h.engine.state.value)
            assertEquals(2, h.factory.created.size)
        }

    @Test
    fun release_voltaAIdle_eFechaOFluxoDeEventos() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            val done = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { h.engine.events.collect { } }
            assertFalse(done.isCompleted)
            h.engine.release()
            assertEquals(EngineState.Idle, h.engine.state.value)
            assertTrue(done.isCompleted)
        }

    // ---- laço de captura ----

    @Test
    fun capturaParadaAntesDoPasso_naoLeNemUmBloco_efechaAFonteUmaVez() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            var reads = 0
            h.source.onRead = { reads++ }
            h.source.push(block(1000))
            h.engine.stopCapture()
            assertFalse(h.driver.pump())
            assertEquals(0, reads)
            assertEquals(1, h.source.closed)
        }

    @Test
    fun leituraSemAmostras_naoEmiteLevelNemEntregaAoReconhecedor_eOLacoContinua() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            assertTrue(h.driver.pump())
            assertTrue(h.events.isEmpty())
            assertTrue(
                h.factory.created
                    .single()
                    .fed
                    .isEmpty(),
            )
            assertEquals(EngineState.Recognizing, h.engine.state.value)
        }

    @Test
    fun leituraNegativa_eMicUnavailable_fechaAFonteEEncerraOLaco() =
        runTest {
            val events = mutableListOf<SpeechEvent>()
            val driver = ManualCaptureDriver()
            val source = Scripted { -1 }
            val engine = raw(source, driver, events)
            engine.prepare()
            engine.startCapture()
            assertFalse(driver.pump())
            assertEquals(listOf<SpeechEvent>(SpeechEvent.Error(null, EngineError.MicUnavailable)), events)
            assertEquals(EngineState.Ready, engine.state.value)
            assertEquals(1, source.closed)
        }

    @Test
    fun fonteQueLanca_encerraOLaco_eFechaAFonteUmaVez() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.source.readError = EngineError.MicUnavailable
            assertFalse(h.driver.pump())
            assertEquals(1, h.source.closed)
            assertEquals(listOf(SpeechEvent.Error(null, EngineError.MicUnavailable)), h.events)
        }

    @Test
    fun blocoMenorQueOBuffer_levelEReconhecedorUsamSoAsAmostrasLidas() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            val small = block(1000, size = 160)
            h.feed(small)
            assertEquals(SpeechEvent.Level(AudioLevel.rms(small, 160)), h.events.first())
            assertEquals(
                1,
                h.factory.created
                    .single()
                    .fed.size,
            )
            assertEquals(
                160,
                h.factory.created
                    .single()
                    .fed
                    .single()
                    .size,
            )
            h.feed(block(1, size = 40))
            assertEquals(
                listOf(160, 40),
                h.factory.created
                    .single()
                    .fed
                    .map { it.size },
            )
        }

    // ---- captura perdida ----

    @Test
    fun passoDeCapturaAntiga_queFalhaDepoisDaNovaCaptura_naoDerrubaANova() =
        runTest {
            // A leitura da captura A lança depois que stopCapture + startCapture abriram a B: o erro de A não pode
            // fechar B nem virar Error (regra 7: evento de captura antiga não age sobre a nova).
            val events = mutableListOf<SpeechEvent>()
            val driver = RecordingDriver()
            lateinit var engine: VoskEngine
            val source =
                Scripted { n ->
                    if (n == 1) {
                        engine.stopCapture()
                        engine.startCapture()
                        throw AudioSourceException(EngineError.MicUnavailable, "falha simulada")
                    }
                    0
                }
            engine = raw(source, driver, events)
            engine.prepare()
            engine.startCapture()
            assertFalse(driver.steps[0]())
            assertEquals(EngineState.Capturing, engine.state.value)
            assertTrue(events.isEmpty())
            assertTrue(driver.steps[1]())
        }

    @Test
    fun capturaPerdida_stopCaptureDepois_naoMexeNoDriver() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            h.source.readError = EngineError.PermissionRevoked
            h.driver.pump()
            h.engine.stopCapture()
            assertEquals(0, h.driver.stops)
            assertEquals(EngineState.Ready, h.engine.state.value)
        }

    @Test
    fun capturaPerdidaNoMeioDaSessao_novaCapturaPermiteNovaSessao() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            h.source.readError = EngineError.PermissionRevoked
            h.driver.pump()
            h.source.readError = null
            h.engine.startCapture()
            h.engine.beginSession() // não lança: a sessão da captura perdida foi encerrada
            assertEquals(EngineState.Recognizing, h.engine.state.value)
        }

    // ---- reconhecedor ----

    @Test
    fun estrategiaReset_tresSessoesComOMesmoVocabulario_reaproveitamOMesmoReconhecedor() =
        runTest {
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            repeat(3) {
                h.engine.beginSession(listOf("ouvir", "[unk]"))
                h.engine.endSession()
            }
            val decoder = h.factory.created.single()
            assertEquals(3, decoder.resets)
            assertFalse(decoder.closed)
        }

    @Test
    fun estrategiaReset_reconhecedorComExcecao_eFechadoENaoReaproveitado() =
        runTest {
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            h.engine.beginSession()
            val broken = h.factory.created.single()
            broken.failOnAccept = true
            h.feed()
            assertTrue(broken.closed)
            assertEquals(0, broken.resets)
            h.engine.endSession()
            h.engine.beginSession()
            assertEquals(2, h.factory.created.size)
            assertEquals(1, broken.closeCalls)
        }

    @Test
    fun reconhecedorComExcecao_endSessionDepois_naoFechaDeNovo() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            val decoder = h.factory.created.single()
            decoder.failOnAccept = true
            h.feed()
            h.engine.endSession()
            h.engine.stopCapture()
            assertEquals(1, decoder.closeCalls)
        }

    @Test
    fun constantes_16kHz_eBlocoDeDuzentosMilissegundos() {
        assertEquals(16_000, VoskEngine.SAMPLE_RATE)
        assertEquals(3_200, VoskEngine.BLOCK_SAMPLES)
        assertEquals(VoskEngine.SAMPLE_RATE / 5, VoskEngine.BLOCK_SAMPLES)
    }
}
