package io.github.gigioderico.tribuna.etapa0.export

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.speech.AssetTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/** D6: a exportação só existe com um texto de teste idêntico (spec 0003, D9; TRD §Privacidade nos testes). */
class TestTextCatalogTest {
    private val catalog = TestTextCatalog(TestFiles.textIds.associateWith(TestFiles::text))

    @Test
    fun cadaTextoDeTeste_eIdentificadoPeloSeuId() {
        assertEquals(11, TestFiles.textIds.size)
        for (id in TestFiles.textIds) assertEquals(id, catalog.identify(TestFiles.text(id)))
    }

    @Test
    fun umCaractereAlterado_naoIdentifica() {
        for (id in TestFiles.textIds) {
            val text = TestFiles.text(id)
            val altered = text.replaceFirst("a", "o")
            assertNull("$id alterado", catalog.identify(altered))
        }
    }

    @Test
    fun semAQuebraDeLinhaFinal_naoIdentifica() {
        assertNull(catalog.identify(TestFiles.text("T01").trimEnd()))
    }

    @Test
    fun textoQualquer_naoIdentifica() {
        assertNull(catalog.identify(""))
        assertNull(catalog.identify("Ouvir bem é uma habilidade"))
    }

    @Test
    fun catalogoDosAssets_leOsTxtDeTextos() {
        val assets =
            object : AssetTree {
                override fun list(path: String): List<String> =
                    if (path ==
                        "textos"
                    ) {
                        listOf("T01.txt", "T09.txt", "leia.md")
                    } else {
                        emptyList()
                    }

                override fun open(path: String): InputStream =
                    ByteArrayInputStream(TestFiles.text(path.removePrefix("textos/").removeSuffix(".txt")).toByteArray(Charsets.UTF_8))
            }
        val fromAssets = TestTextCatalog.fromAssets(assets)
        assertEquals(listOf("T01", "T09"), fromAssets.ids)
        assertEquals("T09", fromAssets.identify(TestFiles.text("T09")))
    }
}
