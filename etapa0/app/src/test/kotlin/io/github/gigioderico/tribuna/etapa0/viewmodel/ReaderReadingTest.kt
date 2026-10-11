package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.align.Move
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** D3 leitura de ponta a ponta e D5 limiar de nível (spec 0003, D3, D17). */
class ReaderReadingTest {
    /** Lê o texto como o Vosk entregaria: parciais crescentes por frase de até 8 palavras e o Final da frase. */
    private fun ReaderHarness.readAll(id: String) {
        TestFiles.spokenLike(id).chunked(8).forEach { phrase ->
            for (n in 1..phrase.size) {
                fake.hearPartial(phrase.take(n))
                advance(150)
            }
            fake.hearFinal(phrase)
            advance(300)
        }
    }

    @Test
    fun t01PorParciaisEFinais_cursorChegaAoUltimoToken_eTrajetoriaTemUmaLinhaPorMudanca() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.readAll("T01")
            assertEquals(h.state.tokens.lastIndex, h.state.cursor)
            h.export()
            val rows = h.rows("T01.trajectory.csv")
            assertEquals("0,0,Reposition,0.0", rows.first())
            val cursors = rows.map { it.split(',')[1].toInt() }
            val moves = rows.map { it.split(',')[2] }
            assertTrue(moves.all { move -> Move.entries.any { it.name == move } })
            assertTrue("uma linha por mudança de cursor", cursors.zipWithNext().all { (a, b) -> a != b })
            assertTrue("pelo menos 100 avanços (Q3)", rows.size > 100)
            assertEquals(h.state.tokens.lastIndex, cursors.last())
            val times = rows.map { it.split(',')[0].toLong() }
            assertEquals(times.sorted(), times)
        }

    @Test
    fun limiarZero_todoParcialEProcessado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.fake.emitLevel(0f)
            h.settle()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.partial("ouvir", "bem", "é", "uma")
            assertTrue(h.state.cursor > 0)
        }

    @Test
    fun limiarAcimaDoUltimoNivel_parcialIgnorado_cursorParado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.controller.setLevelThreshold(0.5f)
            h.listen()
            h.fake.emitLevel(0.2f)
            h.settle()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.partial("ouvir", "bem", "é", "uma")
            assertEquals(0, h.state.cursor)
            assertEquals(emptyList<String>(), h.state.debug.tail)
            h.export()
            assertTrue(h.rows("T01.hypotheses.csv").all { it.endsWith(",0") })
        }

    @Test
    fun limiarNaoFiltraFinal() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.controller.setLevelThreshold(0.5f)
            h.listen()
            h.fake.emitLevel(0.2f)
            h.settle()
            h.final("ouvir", "bem", "é", "uma")
            assertTrue(h.state.cursor > 0)
        }

    @Test
    fun nivelAcimaDoLimiar_parcialProcessado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.controller.setLevelThreshold(0.5f)
            h.listen()
            h.fake.emitLevel(0.5f)
            h.settle()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.partial("ouvir", "bem", "é", "uma")
            assertTrue(h.state.cursor > 0)
        }

    @Test
    fun recuoCurto_trajetoriaGravaOCursor_eOHighWaterFicaNoMaior() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.final("ouvir", "bem", "é", "uma", "habilidade", "que")
            assertEquals(5, h.state.cursor)
            h.final("bem", "é", "uma")
            assertEquals(3, h.state.cursor)
            assertEquals(5, h.state.debug.highWater)
            h.export()
            assertEquals("3", h.rows("T01.trajectory.csv").last().split(',')[1])
        }

    @Test
    fun inicioDaEscuta_doBeginSessionAoPrimeiroParcialNaoVazio() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.advance(300)
            h.partial()
            h.advance(400)
            h.partial("ouvir")
            h.advance(100)
            h.partial("ouvir", "bem")
            assertEquals(700L, h.state.debug.firstPartialMs)
        }

    @Test
    fun textoNaoMudaDuranteALeitura() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.setText(TestFiles.text("T09"))
            h.settle()
            assertEquals("T01", h.state.testTextId)
        }

    @Test
    fun microfoneNaoAbre_aLeituraNaoComeca_eMostraOErro() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.engine.failStartCapture = EngineError.MicUnavailable
            h.listen()
            assertEquals(ReaderPhase.Idle, h.state.phase)
            assertEquals(false, h.state.capturing)
            assertEquals(EngineError.MicUnavailable, h.state.error)
        }
}
