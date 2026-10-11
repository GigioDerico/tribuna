package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.RecognizerStrategy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D9 ciclo de vida (TRD §Segundo plano; D16), erros na tela (D15) e troca de estratégia com a captura parada (D4). */
class ReaderLifecycleTest {
    @Test
    fun onStopComMudancaDeConfiguracao_naoFechaSessaoNemCaptura() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            val before = h.engine.calls.toList()
            h.controller.onActivityStop(changingConfigurations = true)
            h.controller.onActivityStart()
            h.settle()
            assertEquals(before, h.engine.calls)
            assertEquals(ReaderPhase.Reading, h.state.phase)
        }

    @Test
    fun onStopDeVerdade_fechaSessaoECaptura_eATelaFicaPausada() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.onActivityStop(changingConfigurations = false)
            h.settle()
            assertEquals(listOf("endSession@0", "stopCapture"), h.engine.calls.takeLast(2))
            assertEquals(ReaderPhase.Paused, h.state.phase)
            assertFalse(h.state.capturing)
        }

    @Test
    fun voltaDoSegundoPlano_reabreACaptura_eContinuaPausada() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.onActivityStop(changingConfigurations = false)
            h.controller.onActivityStart()
            h.settle()
            assertEquals("startCapture", h.engine.calls.last())
            assertEquals(ReaderPhase.Paused, h.state.phase)
            assertTrue(h.state.capturing)
            h.listen()
            assertTrue(
                h.engine.calls
                    .last()
                    .startsWith("beginSession"),
            )
        }

    @Test
    fun falhaNoPrepare_mostraOErro_eTentarDeNovoPreparaOutraVez() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.engine.failPrepare = EngineError.ModelCorrupt
            h.listen()
            assertEquals(EngineError.ModelCorrupt, h.state.error)
            assertEquals(ReaderPhase.Idle, h.state.phase)
            h.listen()
            assertEquals(null, h.state.error)
            assertEquals(ReaderPhase.Reading, h.state.phase)
            assertEquals(2, h.engine.calls.count { it == "prepare" })
        }

    @Test
    fun erroDaSessaoAtiva_pausaEMostraOMotivo_erroDeSessaoAntigaEIgnorado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            h.listen()
            h.fake.recognizers
                .first()
                .error(EngineError.Internal)
            h.settle()
            assertEquals(ReaderPhase.Reading, h.state.phase)
            h.fake.recognizers
                .last()
                .error(EngineError.Internal)
            h.settle()
            assertEquals(ReaderPhase.Paused, h.state.phase)
            assertEquals(EngineError.Internal, h.state.error)
        }

    @Test
    fun erroDaCaptura_pausaEFechaACaptura_eOuvirReabre() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.fake.emitCaptureError(EngineError.MicUnavailable)
            h.settle()
            assertEquals(ReaderPhase.Paused, h.state.phase)
            assertFalse(h.state.capturing)
            assertEquals(EngineError.MicUnavailable, h.state.error)
            h.listen()
            assertEquals(ReaderPhase.Reading, h.state.phase)
            assertEquals(listOf("startCapture", "beginSession@0"), h.engine.calls.takeLast(2))
        }

    @Test
    fun trocarEstrategiaComALeituraParada_recriaOMotor() =
        runTest {
            val h = ReaderHarness(this)
            h.controller.setStrategy(RecognizerStrategy.Reset)
            h.settle()
            assertEquals(2, h.engines.size)
            assertEquals(
                "release",
                h.engines
                    .first()
                    .calls
                    .last(),
            )
            assertEquals(RecognizerStrategy.Reset, h.engine.config.strategy)
            h.load("T01")
            h.listen()
            h.controller.setStrategy(RecognizerStrategy.NewPerSession)
            h.settle()
            assertEquals(2, h.engines.size)
        }

    @Test
    fun vocabularioLigado_vaiParaOBeginSession_comUnkNoFim() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T09")
            h.controller.setVocabulary(true)
            h.listen()
            val vocabulary =
                h.fake.recognizers
                    .single()
                    .vocabulary
            assertEquals("[unk]", vocabulary?.last())
            assertEquals(vocabulary?.size, h.state.debug.vocabularySize)
        }

    @Test
    fun rotacaoComALeituraPausada_naoReabreACaptura() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            val before = h.engine.calls.toList()
            h.controller.onActivityStop(changingConfigurations = true)
            h.controller.onActivityStart()
            h.settle()
            assertEquals(before, h.engine.calls)
            assertEquals(ReaderPhase.Paused, h.state.phase)
            assertTrue(h.state.capturing)
        }
}
