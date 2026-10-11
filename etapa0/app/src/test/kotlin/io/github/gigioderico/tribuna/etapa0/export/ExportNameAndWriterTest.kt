package io.github.gigioderico.tribuna.etapa0.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/** Nome da exportação sem texto livre (Q6) e gravação dos arquivos (D7). */
class ExportNameAndWriterTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun nomeValido_eOIdDoTextoComSufixoOpcional() {
        assertTrue(ExportName.isValid("T01", "T01"))
        assertTrue(ExportName.isValid("T02_giorgio_salao_t2", "T02"))
        assertTrue(ExportName.isValid("L30_voz2_sala_t1", "L30"))
    }

    @Test
    fun nomeInvalido_outroTexto_textoLivre_ouCaminho() {
        assertFalse(ExportName.isValid("T02", "T01"))
        assertFalse(ExportName.isValid("T01 ouvir bem", "T01"))
        assertFalse(ExportName.isValid("T01_Sala", "T01"))
        assertFalse(ExportName.isValid("T01_../x", "T01"))
        assertFalse(ExportName.isValid("", "T01"))
        assertFalse(ExportName.isValid("T01_", "T01"))
    }

    @Test
    fun writer_gravaUmArquivoPorSufixo_emUtf8() {
        val dir = File(tmp.root, "exportacoes")
        val written = ExportWriter { dir }.write("T01_t1", linkedMapOf("trajectory.csv" to "t_ms\n", "meta.txt" to "model_id=três\n"))
        assertEquals(listOf("T01_t1.trajectory.csv", "T01_t1.meta.txt"), written)
        assertEquals("model_id=três\n", File(dir, "T01_t1.meta.txt").readText(Charsets.UTF_8))
    }

    @Test
    fun writer_semDiretorio_falhaComIOException() {
        val error = runCatching { ExportWriter { null }.write("T01", mapOf("meta.txt" to "")) }.exceptionOrNull()
        assertTrue(error is IOException)
    }

    @Test
    fun nome_comQuebraDeLinhaHifenOuSufixoVazio_ePontoNoIdDoTexto_saoInvalidos() {
        assertFalse(ExportName.isValid("T01\n", "T01"))
        assertFalse(ExportName.isValid("T01_a\n", "T01"))
        assertFalse(ExportName.isValid("T01-sala", "T01"))
        assertFalse(ExportName.isValid("T01__sala", "T01"))
        assertFalse(ExportName.isValid("T01_sala_", "T01"))
        assertFalse(ExportName.isValid("TX1", "T.1"))
        assertTrue(ExportName.isValid("T01_a_b2", "T01"))
    }
}
