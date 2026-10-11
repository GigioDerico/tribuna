package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.align.Move
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** D4 toque em palavra (spec 0003, D10; TRD Máquina de estados, Reading e Paused). */
class ReaderTapTest {
    @Test
    fun toqueNaPalavraDuranteALeitura_endSessionRepositionBeginSession_nessaOrdem() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.controller.onWordTapped(20)
            h.settle()
            // endSession com o cursor antigo; beginSession já com o cursor no token tocado.
            assertEquals(listOf("endSession@0", "beginSession@20"), h.engine.calls.takeLast(2))
            assertEquals(20, h.state.cursor)
            assertEquals(Move.Reposition, h.state.debug.lastMove)
            assertEquals(emptyList<String>(), h.state.debug.tail)
            assertEquals(ReaderPhase.Reading, h.state.phase)
            h.export()
            assertEquals(listOf("0,toque_palavra,20"), h.rows("T01.events.csv"))
            assertEquals("0,20,Reposition,0.0", h.rows("T01.trajectory.csv").last())
        }

    @Test
    fun toqueNaPalavraNaPausa_retomaNaPalavra() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            h.controller.onWordTapped(7)
            h.settle()
            assertEquals("beginSession@7", h.engine.calls.last())
            assertEquals(ReaderPhase.Reading, h.state.phase)
            assertEquals(7, h.state.cursor)
        }

    @Test
    fun pausarEOuvir_gravamPausaToqueERetomaToque() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.advance(1_000)
            h.pause()
            h.advance(2_500)
            h.listen()
            h.export()
            assertEquals(listOf("1000,pausa_toque,", "3500,retoma_toque,"), h.rows("T01.events.csv"))
        }

    @Test
    fun toqueNaPalavraAtual_tambemGravaOReposition() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.onWordTapped(0)
            h.settle()
            h.export()
            assertEquals(listOf("0,0,Reposition,0.0", "0,0,Reposition,0.0"), h.rows("T01.trajectory.csv"))
        }

    @Test
    fun toqueNoTextoComALeituraParada_eIgnorado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.controller.onWordTapped(20)
            h.settle()
            assertEquals(ReaderPhase.Idle, h.state.phase)
            assertEquals(0, h.state.cursor)
            assertFalse(h.state.capturing)
            assertEquals(emptyList<String>(), h.engine.calls)
        }

    @Test
    fun toqueEnquantoOModeloCarrega_eIgnorado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            val gate = CompletableDeferred<Unit>()
            h.engine.prepareGate = gate
            h.listen()
            h.controller.onWordTapped(20)
            h.settle()
            assertEquals(listOf("prepare"), h.engine.calls)
            gate.complete(Unit)
            h.settle()
            assertEquals(0, h.state.cursor)
            assertEquals(ReaderPhase.Reading, h.state.phase)
        }
}
