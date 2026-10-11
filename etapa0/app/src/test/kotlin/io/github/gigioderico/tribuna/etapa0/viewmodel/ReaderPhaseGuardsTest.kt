package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.RecognizerStrategy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rodada 3: uma guarda de fase por teste, para cada fase que a guarda recusa ou aceita (varredura Q2 por fase). */
class ReaderPhaseGuardsTest {
    @Test
    fun onStopComALeituraParada_naoMarcaSegundoPlano_eOuvirSemOnStartComecaNormalmente() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.controller.onActivityStop(changingConfigurations = false)
            h.settle()
            h.listen()
            assertEquals(ReaderPhase.Reading, h.state.phase)
            assertTrue(h.state.capturing)
        }

    @Test
    fun setTextEmPausaOuEmCarga_eIgnorado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            h.controller.setText(TestFiles.text("T09"))
            h.settle()
            assertEquals("T01", h.state.testTextId)
            h.controller.onStopReading()
            h.settle()
            val gate = CompletableDeferred<Unit>()
            h.engine.prepareGate = gate
            h.listen()
            assertEquals(ReaderPhase.Preparing, h.state.phase)
            h.controller.setText(TestFiles.text("T09"))
            h.settle()
            assertEquals("T01", h.state.testTextId)
            gate.complete(Unit)
            h.settle()
        }

    @Test
    fun ajustesEmPausaOuEmCarga_saoIgnorados() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            h.controller.setPreset(PresetChoice.Agil)
            h.controller.setStrategy(RecognizerStrategy.Reset)
            h.settle()
            assertEquals(PresetChoice.Normal, h.state.settings.preset)
            assertEquals(1, h.engines.size)
            h.controller.onStopReading()
            h.settle()
            val gate = CompletableDeferred<Unit>()
            h.engine.prepareGate = gate
            h.listen()
            h.controller.setPreset(PresetChoice.Agil)
            h.controller.setVocabulary(true)
            h.settle()
            assertEquals(PresetChoice.Normal, h.state.settings.preset)
            assertFalse(h.state.settings.vocabulary)
            gate.complete(Unit)
            h.settle()
        }

    @Test
    fun ouvirComALeituraEmCurso_naoPreparaNemAbreCapturaDeNovo() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            val before = h.engine.calls.toList()
            h.listen()
            assertEquals(before, h.engine.calls)
            assertEquals(ReaderPhase.Reading, h.state.phase)
        }

    @Test
    fun ouvirEnquantoOModeloCarrega_naoPreparaDeNovo() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            val gate = CompletableDeferred<Unit>()
            h.engine.prepareGate = gate
            h.listen()
            h.listen()
            gate.complete(Unit)
            h.settle()
            assertEquals(1, h.engine.calls.count { it == "prepare" })
            assertEquals(ReaderPhase.Reading, h.state.phase)
        }

    @Test
    fun erroDeCapturaEnquantoOModeloCarrega_naoMudaAFase() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            val gate = CompletableDeferred<Unit>()
            h.engine.prepareGate = gate
            h.listen()
            h.fake.emitCaptureError(EngineError.MicUnavailable)
            h.settle()
            assertEquals(ReaderPhase.Preparing, h.state.phase)
            gate.complete(Unit)
            h.settle()
        }

    @Test
    fun rotacaoEnquantoOModeloCarrega_comACargaTerminandoAntesDoOnStart_naoMarcaSegundoPlano() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            val gate = CompletableDeferred<Unit>()
            h.engine.prepareGate = gate
            h.listen()
            h.controller.onActivityStop(changingConfigurations = true)
            h.settle()
            gate.complete(Unit)
            h.settle()
            assertEquals(ReaderPhase.Reading, h.state.phase)
            assertTrue(h.state.capturing)
        }
}
