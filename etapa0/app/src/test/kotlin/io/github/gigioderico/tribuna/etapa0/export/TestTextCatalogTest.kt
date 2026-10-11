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
    fun textoDeTesteComAlgoAcrescentado_naoIdentifica() {
        // Guarda de privacidade (OPR-026): um texto de teste seguido de um trecho pessoal não habilita a exportação.
        for (id in TestFiles.textIds) {
            val text = TestFiles.text(id)
            assertNull("$id com sufixo", catalog.identify(text + "x"))
            assertNull("$id com linha a mais", catalog.identify(text + "Trecho pessoal.\n"))
            assertNull("$id com prefixo", catalog.identify("x$text"))
            assertNull("$id sem o início", catalog.identify(text.drop(1)))
        }
    }

    @Test
    fun letraMaiusculaOuMinusculaTrocada_naoIdentifica_eIdNaoTemCaixaAlta() {
        for (id in TestFiles.textIds) {
            val text = TestFiles.text(id)
            assertNull("$id em maiúsculas", catalog.identify(text.uppercase()))
            assertNull("$id em minúsculas", catalog.identify(text.lowercase()))
        }
        assertNull(catalog.text("t01"))
        assertEquals(TestFiles.text("T01"), catalog.text("T01"))
    }

    @Test
    fun idsVemEmOrdemAlfabetica_independenteDaOrdemDeEntrada() {
        val shuffled = TestTextCatalog(TestFiles.textIds.reversed().associateWith(TestFiles::text))
        assertEquals(TestFiles.textIds, shuffled.ids)
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

                override fun open(path: String): InputStream {
                    require(path.startsWith("textos/T")) { "caminho fora de assets/textos: $path" }
                    return ByteArrayInputStream(
                        TestFiles.text(path.removePrefix("textos/").removeSuffix(".txt")).toByteArray(Charsets.UTF_8),
                    )
                }
            }
        val fromAssets = TestTextCatalog.fromAssets(assets)
        assertEquals(listOf("T01", "T09"), fromAssets.ids)
        assertEquals("T09", fromAssets.identify(TestFiles.text("T09")))
    }
}
