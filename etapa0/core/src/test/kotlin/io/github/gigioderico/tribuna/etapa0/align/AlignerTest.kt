package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** Casos pontuais do Aligner (TRD Contratos §Aligner; Motor de voz §Alinhamento; spec 0002, D3–D13). */
class AlignerTest {
    private val t01 = TestTexts.tokens("T01")
    private val normal = Aligner(t01, AlignPresets.normal)

    @Test
    fun reposition_segueD4() {
        assertEquals(AlignState(5, 5, 0, Move.Reposition, 0.0), normal.reposition(5))
        assertEquals(0, normal.reposition(-3).cursor)
        assertEquals(0, normal.reposition(-3).highWater)
        assertEquals(t01.lastIndex, normal.reposition(10_000).cursor)
        assertEquals(t01.lastIndex, normal.reposition(10_000).highWater)
    }

    @Test
    fun reposition_primeiroAvancoPartedaNovaPosicao() {
        val start = Speech.indexOf(t01, "Ouvir com atenção começa")
        var state = normal.reposition(start)
        val tail = mutableListOf<String>()
        for (word in listOf("ouvir", "com", "atencao")) {
            tail += word
            state = normal.advance(state, tail)
        }
        assertEquals(start + 2, state.cursor)
        assertEquals(start + 2, state.highWater)
        assertEquals(Move.Short, state.lastMove)
    }

    @Test
    fun reposition_voltaLivrementeAbaixoDeHighWaterMenos3() {
        val steps = Speech.of(t01, 0, 151) + Step.Reposition(10) + Speech.of(t01, 11, 14)
        val samples = TrajectoryRunner(t01, AlignPresets.normal).run(steps)
        val reposition = samples.first { it.kind == Kind.Reposition }
        assertEquals(150, reposition.previousCursor)
        assertEquals(AlignState(10, 10, 0, Move.Reposition, 0.0), reposition.state)
        assertEquals(13, samples.last().state.cursor)
        assertEquals(13, samples.last().state.highWater)
    }

    @Test
    fun roteiroVazio_advanceDevolveOEstadoERepositionVaiAZero() {
        val empty = Aligner(emptyList(), AlignPresets.normal)
        val state = empty.reposition(3)
        assertEquals(AlignState(0, 0, 0, Move.Reposition, 0.0), state)
        assertEquals(state, empty.advance(state, listOf("ouvir", "bem", "e")))
    }

    @Test
    fun menosDeKPalavras_ficaParadoSemMudarMissStreak_D5() {
        val start = normal.reposition(0).copy(missStreak = 1)
        val next = normal.advance(start, listOf("ouvir", "bem"))
        assertEquals(AlignState(0, 0, 1, Move.Stay, 0.0), next)
    }

    @Test
    fun unkNuncaCasa_D13() {
        val next = normal.advance(normal.reposition(0), listOf("[unk]", "[unk]", "[unk]"))
        assertEquals(0, next.cursor)
        assertEquals(Move.Stay, next.lastMove)
        assertEquals(0.0, next.score, 0.0)
        // [unk] no lugar da palavra mais nova: a posição não avança nessa chamada.
        val tail = mutableListOf("ouvir", "bem", "e")
        val atE = normal.advance(normal.reposition(0), tail)
        assertEquals(2, atE.cursor)
        tail += "[unk]"
        assertEquals(2, normal.advance(atE, tail).cursor)
    }

    @Test
    fun falaComAcentosEMaiusculas_mesmaTrajetoriaQueANormalizada_T03() {
        val text = TestTexts.read("T03")
        val tokens = Tokenizer.tokenize(text)
        val normalized = Speech.of(tokens, 0, tokens.size)
        val shouted =
            tokens.flatMap { token ->
                // "bom-dia" vira "BOM", "DIA": mesma divisão em palavras da forma normalizada.
                val parts = text.substring(token.display.first, token.display.last + 1).uppercase(Locale.ROOT).split('-')
                parts.mapIndexed { n, part -> Step.Read(part, if (n == parts.lastIndex) token.index else null) }
            }
        val a = TrajectoryRunner(tokens, AlignPresets.normal).run(normalized).map { it.state }
        val b = TrajectoryRunner(tokens, AlignPresets.normal).run(shouted).map { it.state }
        assertEquals(a, b)
        assertTrue(shouted.any { it.word == "ESCOLHA" })
    }

    @Test
    fun falaComAcentos_referenciasDoT02_mesmaTrajetoria() {
        val tokens = TestTexts.tokens("T02")
        val accents = mapOf("tres" to "Três", "capitulo" to "Capítulo", "versiculo" to "Versículo", "versiculos" to "versículos")
        val plain = Speech.of(tokens, 0, tokens.size, Take.C)
        val accented = plain.map { Step.Read(accents[it.word] ?: it.word, it.token) }
        val a = TrajectoryRunner(tokens, AlignPresets.normal).run(plain).map { it.state }
        val b = TrajectoryRunner(tokens, AlignPresets.normal).run(accented).map { it.state }
        assertEquals(a, b)
        assertTrue(accented.any { it.word == "Três" })
    }

    /**
     * Q2: este teste fica vermelho se a janela for medida contra o cursor (max(cursor − 3, 0)) em vez de
     * max(cursor − 3, highWater − 3), ou se o limite for removido.
     */
    @Test
    fun recuosSucessivosNaoPassamDeHighWaterMenos3() {
        val h = Speech.indexOf(t01, "Damos conselhos para um problema que não existe") + 7 // "existe"
        val read = Speech.of(t01, 0, h + 1)
        val back =
            "um problema que damos conselhos para um problema que damos conselhos para"
                .split(' ')
                .map { Step.Improv(it) }
        val samples = TrajectoryRunner(t01, AlignPresets.normal).run(read + back)
        val after = samples.drop(read.size)
        assertEquals(h, samples[read.size - 1].state.cursor)
        for (s in samples) {
            assertTrue("cursor ${s.state.cursor} abaixo de highWater ${s.state.highWater} − 3", s.state.cursor >= s.state.highWater - 3)
        }
        assertTrue("houve recuo permitido (Q3)", after.any { it.state.cursor < h })
        assertTrue("nunca abaixo de h − 3", after.all { it.state.cursor >= h - 3 })
    }

    /** Q2: fica vermelho se a recuperação saltar com dois ou mais candidatos (ex.: para o primeiro). */
    @Test
    fun recuperacaoAmbigua_naoMoveOCursor_L30() {
        val l30 = TestTexts.tokens("L30")
        val aligner = Aligner(l30, AlignPresets.normal)
        val first = Speech.indexOf(l30, "no fim do ano a")
        val second = Speech.indexOf(l30, "no fim do ano a", first + 1)
        assertTrue("duas cópias longe uma da outra", second - first > 1000)
        val start = first - 100 // a janela [start − 3, start + 40] não alcança nenhuma cópia
        var state = aligner.reposition(start)
        val tail = mutableListOf<String>()
        val states = mutableListOf<AlignState>()
        for (word in "no fim do ano a".split(' ')) {
            tail += word
            state = aligner.advance(state, tail)
            states += state
        }
        for (s in states) {
            assertEquals(start, s.cursor)
            assertEquals(start, s.highWater)
            assertEquals(Move.Stay, s.lastMove)
        }
        assertTrue("as 4 palavras casaram (Q3)", states.last().score >= AlignPresets.normal.longThreshold)
        assertTrue(states.last().missStreak >= AlignConstants.MISSES_FOR_RECOVERY)
    }
}
