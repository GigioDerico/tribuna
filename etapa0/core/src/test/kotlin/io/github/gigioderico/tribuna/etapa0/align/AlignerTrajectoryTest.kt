package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suíte "Alinhamento" com trajetórias sintéticas (TRD Testes; spec 0002, critérios do Aligner e D21).
 * Salvo indicação, preset Normal. "6 palavras" ≈ 3 s a 130 palavras/min.
 */
class AlignerTrajectoryTest {
    private val t01 = TestTexts.tokens("T01")

    private fun assertNoWrongJumps(samples: List<Sample>) {
        val wrong = samples.wrongJumps()
        assertTrue("saltos errados: ${wrong.take(5)}", wrong.isEmpty())
    }

    /** Atraso ≤ 3 em todas as amostras de leitura a partir da 6ª palavra depois de `firstStep`. */
    private fun assertCaughtUpBySixthWord(
        samples: List<Sample>,
        firstStep: Int,
    ) {
        val after = samples.filter { it.kind == Kind.Read && it.step >= firstStep }
        assertTrue("amostras depois do evento (Q3)", after.size > 6)
        val late = after.drop(5).filter { it.delay > 3 }
        assertTrue("atraso > 3 depois da 6ª palavra: ${late.take(5)}", late.isEmpty())
    }

    @Test
    fun leituraNormalT01_tresPresets_cursorIgualAoEsperadoDesdeAKesimaPalavra() {
        for (params in listOf(AlignPresets.conservador, AlignPresets.normal, AlignPresets.agil)) {
            val samples = TrajectoryRunner(t01, params).run(Speech.of(t01, 0, t01.size))
            assertEquals(t01.size, samples.size)
            samples.drop(params.k - 1).forEach { assertEquals("$params passo ${it.step}", it.expected, it.state.cursor) }
            assertTrue("$params moveu ${samples.moveChanges()} vezes (Q3)", samples.moveChanges() >= t01.size - params.k)
            assertNoWrongJumps(samples)
        }
    }

    @Test
    fun palavrasErradasT01_umaEmCincoTrocada() {
        // D21: na palavra i com i % 5 == 2, alterna variação ortográfica (tira ou põe "s") e palavra do T03.
        val others = Speech.wordsOf(TestTexts.tokens("T03"))
        var next = 0
        val steps =
            Speech.of(t01, 0, t01.size).mapIndexed { i, step ->
                if (i % 5 != 2) {
                    step
                } else if ((i / 5) % 2 == 0) {
                    Step.Read(if (step.word.endsWith("s")) step.word.dropLast(1) else step.word + "s", step.token)
                } else {
                    while (others[next % others.size] == step.word) next++
                    Step.Read(others[next++ % others.size], step.token)
                }
            }
        assertEquals(43, steps.indices.count { it % 5 == 2 })
        val samples = TrajectoryRunner(t01, AlignPresets.normal).run(steps)
        assertNoWrongJumps(samples)
        assertTrue(samples.fractionWithinDelay() >= 0.95)
        assertEquals(t01.lastIndex, samples.last().state.cursor)
    }

    @Test
    fun puloDeParagrafoT05_recuperaAte6Palavras() {
        val t05 = TestTexts.tokens("T05")
        val skipped = Speech.indexOf(t05, "Isso tem um custo")
        val resume = Speech.indexOf(t05, "Ouvir com atenção começa")
        val before = Speech.of(t05, 0, skipped)
        val samples = TrajectoryRunner(t05, AlignPresets.normal).run(before + Speech.of(t05, resume, t05.size))
        assertNoWrongJumps(samples)
        assertCaughtUpBySixthWord(samples, firstStep = before.size)
        assertTrue(samples.any { it.state.lastMove == Move.Recovery })
        assertEquals(t05.lastIndex, samples.last().state.cursor)
    }

    @Test
    fun saltoLongoDentroDaJanelaT01_pularUmaFrase() {
        val skipped = Speech.indexOf(t01, "Damos conselhos para um problema")
        val resume = Speech.indexOf(t01, "E a pessoa sai")
        assertTrue(resume - skipped < AlignPresets.normal.window)
        val samples =
            TrajectoryRunner(t01, AlignPresets.normal).run(Speech.of(t01, 0, skipped) + Speech.of(t01, resume, t01.size))
        assertNoWrongJumps(samples)
        assertTrue(samples.any { it.state.lastMove == Move.LongJump })
    }

    @Test
    fun improvisoT04_cursorParadoERetomaAte6Palavras() {
        val t04 = TestTexts.tokens("T04")
        val back = Speech.indexOf(t04, "Isso tem um custo")
        val improv = Speech.improv(Speech.wordsOf(TestTexts.tokens("T03")))
        val before = Speech.of(t04, 0, back)
        val samples =
            TrajectoryRunner(t04, AlignPresets.normal).run(before + improv + Speech.of(t04, back, t04.size))
        val during = samples.filter { it.kind == Kind.Improv }
        assertEquals(improv.size, during.size)
        assertTrue("cursor mudou no improviso", during.all { it.state.cursor == it.previousCursor })
        assertNoWrongJumps(samples)
        assertCaughtUpBySixthWord(samples, firstStep = before.size + improv.size)
    }

    @Test
    fun repeticaoPeloOradorT08_semSaltoErradoNemRecuoAlemDoLimite() {
        val t08 = TestTexts.tokens("T08")
        val first = Speech.indexOf(t08, "Isso tem um custo")
        val afterFirst = Speech.indexOf(t08, "Quando não ouvimos")
        val second = Speech.indexOf(t08, "Um momento de pausa")
        val afterSecond = Speech.indexOf(t08, "Muitas vezes, é nesse")
        val part1 = Speech.of(t08, 0, afterFirst) + Speech.of(t08, first, afterFirst)
        val part2 = Speech.of(t08, afterFirst, afterSecond) + Speech.of(t08, second, afterSecond)
        val samples = TrajectoryRunner(t08, AlignPresets.normal).run(part1 + part2 + Speech.of(t08, afterSecond, t08.size))
        assertNoWrongJumps(samples)
        assertTrue(samples.all { it.state.cursor >= it.state.highWater - 3 })
        assertCaughtUpBySixthWord(samples.filter { it.step < part1.size + part2.size }, firstStep = part1.size)
        assertCaughtUpBySixthWord(samples, firstStep = part1.size + part2.size)
    }

    @Test
    fun trechosRepetidosT03_nuncaPulaParaOutraCopia() {
        val t03 = TestTexts.tokens("T03")
        val samples = TrajectoryRunner(t03, AlignPresets.normal).run(Speech.of(t03, 0, t03.size))
        assertNoWrongJumps(samples)
        assertTrue(samples.moveChanges() >= t03.size - AlignPresets.normal.k)
    }

    @Test
    fun palavrasComunsT09() {
        val t09 = TestTexts.tokens("T09")
        val samples = TrajectoryRunner(t09, AlignPresets.normal).run(Speech.of(t09, 0, t09.size))
        assertNoWrongJumps(samples)
        assertTrue(samples.size >= 50)
        assertTrue(samples.fractionWithinDelay() >= 0.95)
    }

    @Test
    fun numerosEVariantesT02_tresTakes() {
        val t02 = TestTexts.tokens("T02")
        val references = t02.filter { Speech.isReference(it) }.map { it.index }
        assertEquals(5, references.size)
        for (take in listOf(Take.A, Take.B, Take.C)) {
            val samples = TrajectoryRunner(t02, AlignPresets.normal).run(Speech.of(t02, 0, t02.size, take))
            assertNoWrongJumps(samples)
            assertEquals("$take", t02.lastIndex, samples.last().state.cursor)
            for (ref in references) {
                val reached = samples.any { it.state.cursor == ref }
                val passedClose = samples.firstOrNull { it.state.cursor > ref }?.let { it.delay <= 3 } ?: false
                assertTrue("$take referência $ref", reached || passedClose)
            }
        }
    }

    @Test
    fun cadaMoveApareceComoLastMove() {
        val seen = mutableSetOf<Move>()
        seen += TrajectoryRunner(t01, AlignPresets.normal).run(Speech.of(t01, 0, t01.size)).map { it.state.lastMove }
        val skipped = Speech.indexOf(t01, "Damos conselhos para um problema")
        val resume = Speech.indexOf(t01, "E a pessoa sai")
        seen +=
            TrajectoryRunner(t01, AlignPresets.normal)
                .run(Speech.of(t01, 0, skipped) + Speech.of(t01, resume, t01.size))
                .map { it.state.lastMove }
        val t05 = TestTexts.tokens("T05")
        seen +=
            TrajectoryRunner(t05, AlignPresets.normal)
                .run(
                    Speech.of(t05, 0, Speech.indexOf(t05, "Isso tem um custo")) +
                        Speech.of(t05, Speech.indexOf(t05, "Ouvir com atenção começa"), t05.size),
                ).map { it.state.lastMove }
        seen += TrajectoryRunner(t01, AlignPresets.normal).run(listOf(Step.Reposition(20))).map { it.state.lastMove }
        assertEquals(Move.entries.toSet(), seen)
    }
}
