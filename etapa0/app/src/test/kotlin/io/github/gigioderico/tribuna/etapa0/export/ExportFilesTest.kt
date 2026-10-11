package io.github.gigioderico.tribuna.etapa0.export

import io.github.gigioderico.tribuna.etapa0.align.Move
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.SessionId
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEvent
import org.junit.Assert.assertEquals
import org.junit.Test

/** D7 e D11: formato dos arquivos (cabeçalhos de D7 e de TESTES-ETAPA0) e leitura de volta das hipóteses. */
class ExportFilesTest {
    private fun recording(): Recording = Recording("T01", meta = mapOf("text_id" to "T01"), originMs = 1_000L)

    @Test
    fun cabecalhos_saoOsDeD7EDeTestesEtapa0() {
        val files = ExportFiles.build(recording())
        assertEquals(listOf("hypotheses.csv", "level.csv", "trajectory.csv", "events.csv", "meta.txt"), files.keys.toList())
        assertEquals("t_ms,session,kind,words,accepted", files.getValue("hypotheses.csv").lines().first())
        assertEquals("t_ms,rms", files.getValue("level.csv").lines().first())
        assertEquals("t_ms,cursor,move,score", files.getValue("trajectory.csv").lines().first())
        assertEquals("t_ms,evento,detalhe", files.getValue("events.csv").lines().first())
    }

    @Test
    fun hipoteses_umaLinhaPorEvento_comPalavrasEntreAspas() {
        val rec = recording()
        rec.hypothesis(1_120L, SpeechEvent.Partial(SessionId(1), listOf("ouvir", "bem")), accepted = true)
        rec.hypothesis(1_300L, SpeechEvent.Partial(SessionId(1), emptyList()), accepted = true)
        rec.hypothesis(1_450L, SpeechEvent.Final(SessionId(1), listOf("ouvir", "bem", "é")), accepted = false)
        rec.hypothesis(1_500L, SpeechEvent.Error(null, EngineError.MicUnavailable), accepted = true)
        rec.hypothesis(1_600L, SpeechEvent.Level(0.5f), accepted = true)
        assertEquals(
            listOf(
                "t_ms,session,kind,words,accepted",
                "120,1,partial,\"ouvir bem\",1",
                "300,1,partial,\"\",1",
                "450,1,final,\"ouvir bem é\",0",
                "500,,error,\"MicUnavailable\",1",
                "",
            ),
            ExportFiles.hypotheses(rec.hypotheses).lines(),
        )
    }

    @Test
    fun trajetoria_nivelEEventos() {
        val rec = recording()
        rec.trajectory(1_000L, 0, Move.Reposition, 0.0)
        rec.trajectory(2_250L, 3, Move.Short, 0.75)
        rec.level(1_200L, 0.25f)
        rec.event(3_000L, Recording.PAUSE_TAP)
        rec.event(3_500L, Recording.WORD_TAP, "12")
        assertEquals("t_ms,cursor,move,score\n0,0,Reposition,0.0\n1250,3,Short,0.75\n", ExportFiles.trajectory(rec.trajectory))
        assertEquals("t_ms,rms\n200,0.250000\n", ExportFiles.level(rec.levels))
        assertEquals("t_ms,evento,detalhe\n2000,pausa_toque,\n2500,toque_palavra,12\n", ExportFiles.events(rec.events))
    }

    @Test
    fun meta_trazTodasAsChavesDeD11_naOrdem() {
        val meta = ExportFiles.meta(mapOf("model_id" to "vosk-model-small-pt-0.3 sha256=ab", "preset" to "Normal"))
        val keys = meta.lines().filter { it.isNotEmpty() }.map { it.substringBefore('=') }
        assertEquals(ExportFiles.META_KEYS, keys)
        assertEquals(15, keys.size)
        assertEquals(true, meta.contains("model_id=vosk-model-small-pt-0.3 sha256=ab\n"))
    }

    @Test
    fun hipoteses_lidasDeVolta_saoIguaisAsGravadas() {
        val rec = recording()
        rec.hypothesis(1_100L, SpeechEvent.Partial(SessionId(1), listOf("ouvir")), accepted = true)
        rec.hypothesis(1_200L, SpeechEvent.Final(SessionId(2), listOf("diz", "\"isso\"")), accepted = false)
        rec.hypothesis(1_300L, SpeechEvent.Error(null, EngineError.PermissionRevoked), accepted = true)
        assertEquals(rec.hypotheses, ExportFiles.readHypotheses(ExportFiles.hypotheses(rec.hypotheses)))
    }

    @Test
    fun reprocessar_soAsAceitas_zerandoAFraseACadaSessao() {
        val rows =
            listOf(
                HypothesisRow(0, 1, "partial", listOf("ouvir"), true),
                HypothesisRow(1, 1, "partial", listOf("ouvir", "bem"), true),
                HypothesisRow(2, 1, "partial", listOf("ouvir", "bem", "é"), false),
                HypothesisRow(3, 2, "partial", listOf("uma"), true),
                HypothesisRow(4, 1, "final", listOf("ouvir", "bem", "é", "uma"), false),
                HypothesisRow(5, 2, "final", listOf("uma", "habilidade"), true),
                HypothesisRow(6, null, "error", listOf("MicUnavailable"), true),
            )
        assertEquals(listOf("ouvir", "uma", "habilidade"), ExportFiles.replayAccepted(rows))
    }
}
