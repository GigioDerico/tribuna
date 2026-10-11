package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.export.ExportFiles
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D6 exportação só com texto de teste, D7 ida e volta (OPR-008), D8 proveniência (D11). */
class ReaderExportTest {
    @Test
    fun textoAlterado_desabilitaAExportacao() =
        runTest {
            val h = ReaderHarness(this)
            h.controller.setText(TestFiles.text("T01").replaceFirst("Ouvir", "Ouvi"))
            h.settle()
            assertNull(h.state.testTextId)
            h.listen()
            h.partial("ouvir", "bem")
            assertFalse(h.state.canExport)
            assertTrue(h.export().isEmpty())
        }

    @Test
    fun textoDeTeste_habilitaAExportacao_comONomeDoTexto() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T09")
            assertEquals("T09", h.state.testTextId)
            assertEquals("T09", h.state.exportName)
            h.listen()
            assertTrue(h.state.canExport)
            assertEquals(
                listOf("T09.hypotheses.csv", "T09.level.csv", "T09.trajectory.csv", "T09.events.csv", "T09.meta.txt"),
                h.export().keys.toList(),
            )
        }

    @Test
    fun textoNovoDepoisDeGravar_apagaAGravacaoAnterior_eNaoExportaNada() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.partial("ouvir", "bem")
            h.controller.onStopReading()
            h.settle()
            h.controller.setText("um texto qualquer que não é de teste")
            h.controller.setExportName("T01")
            h.settle()
            assertFalse(h.state.canExport)
            assertTrue(h.export().isEmpty())
            assertFalse(h.state.exportFailed)
        }

    @Test
    fun nomeComTextoLivre_eRecusado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.setExportName("T01 ouvir bem")
            h.settle()
            assertTrue(h.export().isEmpty())
            assertTrue(h.state.exportFailed)
        }

    @Test
    fun hipotesesLidasDeVolta_entregamAsMesmasPalavrasDaSessaoAoVivo() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.partial("ouvir")
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.final("ouvir", "bem", "é", "uma")
            assertEquals(listOf("ouvir", "bem", "é", "uma"), h.state.debug.tail)
            h.pause()
            h.listen()
            h.fake.recognizers
                .first()
                .final(listOf("habilidade"))
            h.settle()
            h.partial("habilidade", "que")
            h.partial("habilidade", "que", "quase")
            h.final("habilidade", "que", "quase", "ninguém")
            assertEquals(listOf("habilidade", "que", "quase", "ninguém"), h.state.debug.tail)
            h.export()
            val rows = ExportFiles.readHypotheses(h.exported.getValue("T01.hypotheses.csv"))
            assertEquals(
                listOf("ouvir", "bem", "é", "uma", "habilidade", "que", "quase", "ninguém"),
                ExportFiles.replayAccepted(rows),
            )
            assertEquals(listOf(1L, 2L), rows.filter { it.accepted }.map { it.session }.distinct())
            assertEquals(1, rows.count { !it.accepted && it.session == 1L })
        }

    @Test
    fun meta_trazTodasAsChaves_eOIdDoModeloIgualAoUuid() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T02")
            h.listen()
            h.export()
            val meta =
                h.exported
                    .getValue("T02.meta.txt")
                    .lines()
                    .filter { it.isNotEmpty() }
                    .associate { it.substringBefore('=') to it.substringAfter('=') }
            assertEquals(ExportFiles.META_KEYS, meta.keys.toList())
            assertEquals(h.modelId, meta["model_id"])
            assertTrue(meta.getValue("model_id").startsWith("vosk-model-small-pt-0.3 sha256="))
            assertEquals("T02", meta["text_id"])
            assertEquals("Normal", meta["preset"])
            assertEquals("off", meta["vocabulary"])
            assertEquals("NewPerSession", meta["recognizer_strategy"])
            assertEquals("mic", meta["audio_source"])
            assertEquals("0.0", meta["level_threshold"])
            assertEquals("375", meta["animation_ms"])
            assertEquals("1234", meta["install_ms"])
            assertEquals("0.3.75", meta["vosk_android_version"])
        }
}
