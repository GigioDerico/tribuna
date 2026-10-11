package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** L30 sintético com os eventos das linhas // (spec 0002, último critério do Aligner; D21 e D23). */
class AlignerL30Test {
    @Test
    fun l30Sintetico_semSaltoErrado_atrasoAte3Em95PorCento_chegaAoFim() {
        val l30 = TestTexts.tokens("L30")
        val improvBefore = Speech.indexOf(l30, "Agora, é importante falar") // nota do minuto 8
        val pauseBefore = Speech.indexOf(l30, "Há momentos, porém") // nota do minuto 12
        val skipped = Speech.indexOf(l30, "Um detalhe prático ajuda") // nota do minuto 15
        val resume = Speech.indexOf(l30, "Outra ferramenta preciosa")
        val bridgeEnd = Speech.indexOf(l30, "como uma ponte velha") + 4 // nota do minuto 22
        val imagine = Speech.indexOf(l30, "Imagine duas pessoas")
        val repeat = Speech.indexOf(l30, "Ninguém chega longe de uma vez só") // nota do minuto 25
        val repeatEnd = repeat + 7

        val t01 = TestTexts.tokens("T01")
        val improv = Speech.improv(Speech.wordsOf(t01.subList(0, Speech.indexOf(t01, "Isso tem um custo"))))

        val steps = mutableListOf<Step>()
        steps += Speech.of(l30, 0, improvBefore)
        steps += improv
        steps += Speech.of(l30, improvBefore, pauseBefore)
        steps += Step.NewSession // pausa e retomada: nova sessão, cauda zerada
        steps += Speech.of(l30, pauseBefore, skipped)
        val skipStep = steps.size
        // Silêncio de 30 s depois do parágrafo do minuto 18: nenhuma chamada (nada a acrescentar).
        steps += Speech.of(l30, resume, bridgeEnd)
        val repositionStep = steps.size
        steps += Step.Reposition(imagine)
        steps += Speech.of(l30, imagine, repeatEnd)
        steps += Speech.of(l30, repeat, repeatEnd) // repetição como ênfase
        steps += Speech.of(l30, repeatEnd, l30.size)

        val runner = TrajectoryRunner(l30, AlignPresets.normal)
        val samples = runner.run(steps)

        val wrong = samples.wrongJumps()
        assertTrue("saltos errados: ${wrong.take(5)}", wrong.isEmpty())
        assertEquals(l30.lastIndex, samples.last().state.cursor)

        val reads = samples.reads()
        val excluded =
            reads
                .filter { it.step >= skipStep }
                .take(6)
                .map { it.step }
                .toSet() +
                reads
                    .filter { it.step > repositionStep }
                    .take(6)
                    .map { it.step }
                    .toSet()
        val normalReading = reads.filter { it.step !in excluded }
        assertTrue("amostras de leitura normal (Q3)", normalReading.size > 3000)
        assertTrue(normalReading.fractionWithinDelay() >= 0.95)
        assertTrue(samples.any { it.state.lastMove == Move.Recovery })

        val totalMs = runner.advanceNanos.sum() / 1_000_000.0
        val maxMs = runner.advanceNanos.max() / 1_000_000.0
        println("L30 sintético: ${runner.advanceNanos.size} chamadas de advance, total %.1f ms, máximo %.3f ms".format(totalMs, maxMs))
    }
}
