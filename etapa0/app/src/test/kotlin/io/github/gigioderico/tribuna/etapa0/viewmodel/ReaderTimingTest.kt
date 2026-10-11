package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.align.Move
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Medidas de tempo do painel (spec 0003, D18): cada uma com o relógio do controlador em valores distintos. */
class ReaderTimingTest {
    private fun ReaderHarness.readThreeWords() {
        partial("ouvir", "bem")
        partial("ouvir", "bem", "é")
        partial("ouvir", "bem", "é", "uma")
    }

    @Test
    fun prepareEBeginSession_medemOTempoGastoEmCadaUm() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.advance(50)
            h.engine.onPrepare = { h.clockSkew += 300 }
            h.engine.onBeginSession = { h.clockSkew += 7 }
            h.listen()
            assertEquals(300L, h.state.debug.prepareMs)
            assertEquals(7L, h.state.debug.beginSessionMs)
            h.export()
            assertTrue(
                h.exported
                    .getValue("T01.meta.txt")
                    .lines()
                    .contains("prepare_ms=300"),
            )
            assertTrue(
                h.exported
                    .getValue("T01.meta.txt")
                    .lines()
                    .contains("install_ms=1234"),
            )
            assertEquals(1_234L, h.state.debug.installMs)
        }

    @Test
    fun inicioDaEscuta_contaDoBeginSessionDaSessaoAtual_eReiniciaACadaSessao() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.advance(1_000)
            h.listen()
            h.advance(300)
            h.partial("ouvir")
            assertEquals(300L, h.state.debug.firstPartialMs)
            h.advance(5_000)
            h.pause()
            h.listen()
            assertNull(h.state.debug.firstPartialMs)
            h.advance(40)
            h.partial("uma")
            assertEquals(40L, h.state.debug.firstPartialMs)
        }

    @Test
    fun latencia2_vaiDaRecepcaoDoParcialAoCursorPublicado_eNaoMudaNoToque() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            assertNull(h.state.debug.latency2Ms)
            h.clockSkew = 1_000
            h.tick = 1
            h.readThreeWords()
            h.tick = 0
            val latency = h.state.debug.latency2Ms
            assertNotNull(latency)
            assertTrue("latência 2 positiva e pequena: $latency", latency!! in 1L..20L)
            h.tick = 5
            h.controller.onWordTapped(h.state.cursor)
            h.settle()
            h.tick = 0
            assertEquals(Move.Reposition, h.state.debug.lastMove)
            assertEquals(latency, h.state.debug.latency2Ms)
        }

    @Test
    fun latencia3_contaDoUltimoMovimentoDoCursor_naoDoUltimoEvento() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.clockSkew = 100
            h.readThreeWords()
            h.final("ouvir", "bem", "é", "uma")
            val cursor = h.state.cursor
            assertTrue(cursor > 0)
            // Fala sem palavra do texto: o alinhador publica sem mover o cursor.
            h.clockSkew = 400
            h.partial("zzz")
            h.partial("zzz", "yyy")
            assertEquals(
                listOf("zzz"),
                h.state.debug.tail
                    .takeLast(1),
            )
            assertEquals(cursor, h.state.cursor)
            h.clockSkew = 700
            h.controller.onScrollSettled(cursor + 1)
            h.settle()
            assertNull(h.state.debug.latency3Ms)
            h.controller.onScrollSettled(cursor)
            h.settle()
            assertEquals(600L, h.state.debug.latency3Ms)
        }

    @Test
    fun latencia3_toqueNaPalavraDoCursor_reiniciaAContagem() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.clockSkew = 100
            h.readThreeWords()
            val cursor = h.state.cursor
            h.clockSkew = 900
            h.controller.onWordTapped(cursor)
            h.settle()
            assertEquals(cursor, h.state.cursor)
            h.clockSkew = 1_000
            h.controller.onScrollSettled(cursor)
            h.settle()
            assertEquals(100L, h.state.debug.latency3Ms)
        }
}
