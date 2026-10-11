package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Fronteiras da tokenização que nenhum texto de teste exercita (spec 0004, rodada 2; varredura anti-recorrência):
 * o limite de 999.999 (D14), os livros 1, 2 e 3, as referências no fim da linha, pontuação solta, porcentagem e
 * decimais. Cada linha de referencia/fronteiras.tsv é `texto<TAB>token ; token`, com `inicio-fim=formas`.
 *
 * O Kotlin é a autoridade: este teste só compara. O etapa0/ferramentas/roteiro.py lê o mesmo arquivo (test_roteiro).
 * Regenerar é ação explícita: copie build/referencia-atual/fronteiras.tsv para src/test/resources/referencia/.
 */
class FronteirasDaTokenizacaoTest {
    @Test
    fun fronteiras_batemComAReferencia() {
        val stream = javaClass.getResourceAsStream("/referencia/fronteiras.tsv")
        assertNotNull("referência fronteiras.tsv ausente: regenere (impl_plan 0004)", stream)
        val linhas = stream!!.use { it.readBytes().toString(Charsets.UTF_8) }.split('\n')
        assertEquals("", linhas.last())
        val casos = linhas.drop(1).dropLast(1).map { it.substringBefore('\t') }
        assertEquals("#fronteiras=${casos.size}", linhas.first())
        assertTrue("Q3: casos suficientes", casos.size >= 60)
        val gerado = StringBuilder("#fronteiras=${casos.size}\n")
        for (texto in casos) gerado.append("$texto\t${render(texto)}\n")
        File("build/referencia-atual").apply { mkdirs() }
        File("build/referencia-atual/fronteiras.tsv").writeText(gerado.toString(), Charsets.UTF_8)
        val obtido = gerado.toString().split('\n')
        for (i in linhas.indices) {
            assertEquals("fronteiras.tsv linha ${i + 1} (referência × Kotlin)", linhas[i], obtido[i])
        }
        assertEquals(linhas.size, obtido.size)
    }

    private fun render(texto: String): String =
        Tokenizer.tokenize(texto).joinToString(" ; ") { t ->
            "${t.display.first}-${t.display.last}=" + t.spoken.joinToString("|") { it.joinToString(" ") }
        }
}
