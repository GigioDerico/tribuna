package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Paridade das formas faladas com as ferramentas Python da Etapa 0 (spec 0004, B1 e B2; brainstorm D3).
 *
 * As referências versionadas ficam em src/test/resources/referencia/ e são lidas também pelo
 * etapa0/ferramentas/roteiro.py. Este teste só compara: escreve o que o Kotlin gera hoje em
 * build/referencia-atual/ (fora do git) e falha se a versão versionada divergir. Regenerar é
 * ação explícita (impl_plan 0004, "Regenerar as referências"), nunca automática.
 */
class ReferenciaFormasTest {
    private val atual = File("build/referencia-atual")

    @Test
    fun formasDeCadaTexto_batemComAReferencia() {
        val gerados =
            TestTexts.names.associate { name ->
                val tokens = TestTexts.tokens(name)
                assertTrue("$name sem tokens (Q3)", tokens.size > 50)
                "$name.tsv" to renderTexto(name, tokens)
            }
        gerados.forEach { (arquivo, gerado) -> escreve(arquivo, gerado) } // todos antes de comparar
        gerados.forEach { (arquivo, gerado) -> confere(arquivo, gerado) }
    }

    @Test
    fun formasDosNumeros_batemComAReferencia() {
        val numeros = numerosDaReferencia()
        assertEquals("quantidade de números (Q3)", 2101 + MILHARES.size * RESTOS.size, numeros.size)
        val gerado = renderNumeros(numeros)
        escreve("numeros.tsv", gerado)
        confere("numeros.tsv", gerado)
    }

    private fun escreve(
        arquivo: String,
        gerado: String,
    ) {
        atual.mkdirs()
        File(atual, arquivo).writeText(gerado, Charsets.UTF_8)
    }

    private fun confere(
        arquivo: String,
        gerado: String,
    ) {
        val stream = javaClass.getResourceAsStream("/referencia/$arquivo")
        assertNotNull("referência $arquivo ausente: regenere (impl_plan 0004)", stream)
        val versionado = stream!!.use { it.readBytes().toString(Charsets.UTF_8) }
        val esperado = versionado.split('\n')
        val obtido = gerado.split('\n')
        for (i in 0 until minOf(esperado.size, obtido.size)) {
            assertEquals("$arquivo linha ${i + 1} (referência × Kotlin)", esperado[i], obtido[i])
        }
        assertEquals("$arquivo número de linhas (referência × Kotlin)", esperado.size, obtido.size)
    }

    private fun renderTexto(
        name: String,
        tokens: List<Token>,
    ): String {
        val bytes = File(System.getProperty("tribuna.textos"), "$name.txt").readBytes()
        val sb = StringBuilder("#texto=$name sha256=${sha256(bytes)} tokens=${tokens.size}\n")
        for (t in tokens) {
            sb.append("${t.index}\t${t.display.first}\t${t.display.last}\t${formas(t.spoken)}\n")
        }
        return sb.toString()
    }

    private fun renderNumeros(numeros: List<Int>): String {
        val sb = StringBuilder("#numeros=${numeros.size}\n")
        for (n in numeros) {
            sb.append("$n\t${formas(NumberWords.cardinalForms(n))}\t${formas(NumberWords.masculineForms(n))}\n")
        }
        return sb.toString()
    }

    private fun formas(spoken: List<List<String>>): String = spoken.joinToString("|") { it.joinToString(" ") }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        /** Lista fixa do plano 0004 (B1): milhares × restos; 999 × 1000 + 999 é o máximo de D14. */
        val MILHARES = listOf(2, 3, 10, 11, 14, 16, 20, 21, 100, 101, 114, 200, 201, 999)
        val RESTOS = listOf(0, 1, 14, 99, 100, 101, 114, 200, 999)

        fun numerosDaReferencia(): List<Int> = (0..2100).toList() + MILHARES.flatMap { m -> RESTOS.map { r -> m * 1000 + r } }
    }
}
