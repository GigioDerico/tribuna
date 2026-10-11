package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.speech.SessionId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** D1 descarte por sessão (CLAUDE.md, regra 7; TRD Reconciliação 7) e D2 fala na pausa. */
class ReaderSessionTest {
    @Test
    fun parcialEFinalAtrasadosDaSessaoAnterior_naoMexemNoCursorNemNaCauda() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.partial("ouvir", "bem", "é", "uma")
            val cursor = h.state.cursor
            assertTrue("a leitura precisa ter andado (Q3)", cursor > 0)
            h.pause()
            h.listen()
            assertEquals(2L, h.state.debug.activeSession)
            val old = h.fake.recognizers.first()
            old.partial(listOf("ouvir", "bem", "é", "uma", "habilidade", "que", "quase"))
            old.final(listOf("ouvir", "bem", "é", "uma", "habilidade", "que", "quase", "ninguém", "pratica"))
            h.settle()
            assertEquals(cursor, h.state.cursor)
            assertEquals(emptyList<String>(), h.state.debug.tail)
        }

    @Test
    fun eventosAtrasadosEntramNaExportacaoComOIdOriginalEAccepted0() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            h.listen()
            val old = h.fake.recognizers.first()
            old.partial(listOf("ouvir", "bem"))
            old.final(listOf("ouvir", "bem"))
            h.settle()
            h.export()
            assertEquals(
                listOf("0,1,partial,\"ouvir bem\",0", "0,1,final,\"ouvir bem\",0"),
                h.rows("T01.hypotheses.csv"),
            )
        }

    @Test
    fun parcialAtrasadoDaSessaoAnterior_naoChegaAoReconciliador() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            h.listen()
            // Se o parcial antigo entrasse no reconciliador, ele viraria o "anterior" da sessão nova e o
            // primeiro parcial igual entregaria as palavras: o prefixo comum já existiria.
            h.fake.recognizers
                .first()
                .partial(listOf("ouvir", "bem"))
            h.settle()
            h.partial("ouvir", "bem")
            assertEquals(emptyList<String>(), h.state.debug.tail)
            assertEquals(0, h.state.cursor)
            h.partial("ouvir", "bem", "é")
            assertEquals(listOf("ouvir", "bem"), h.state.debug.tail)
        }

    @Test
    fun falaNaPausa_naoMoveOCursor_eAoRetomarASessaoENovaComCaudaVazia() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.pause()
            val cursor = h.state.cursor
            val paused = h.fake.recognizers.single()
            // Na pausa não há sessão: a fala chega ao motor e é descartada; o reconhecedor antigo pode atrasar.
            h.fake.hearPartial(listOf("uma", "habilidade", "que"))
            paused.partial(listOf("ouvir", "bem", "é", "uma", "habilidade"))
            h.settle()
            assertEquals(cursor, h.state.cursor)
            h.listen()
            assertEquals(
                SessionId(2),
                h.fake.recognizers
                    .last()
                    .session,
            )
            assertEquals(2L, h.state.debug.activeSession)
            assertEquals(emptyList<String>(), h.state.debug.tail)
            h.partial("uma")
            h.partial("uma", "habilidade")
            assertEquals(listOf("uma"), h.state.debug.tail)
        }
}
