package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Formas faladas (TRD Contratos §Token e §Normalização e tokens; spec 0002, D14–D18). */
class TokenizerTest {
    private fun w(text: String) = text.split(' ')

    private fun display(
        text: String,
        token: Token,
    ) = text.substring(token.display.first, token.display.last + 1)

    /** Token cujo trecho exibido é exatamente `literal` (a primeira ocorrência). */
    private fun tokenOf(
        text: String,
        literal: String,
    ): Token = Tokenizer.tokenize(text).first { display(text, it) == literal }

    @Test
    fun exemplosDoTrd_24() {
        // T02: "Mais de 24 países"
        assertEquals(listOf(w("vinte e quatro")), tokenOf("Mais de 24 países", "24").spoken)
    }

    @Test
    fun exemplosDoTrd_3_16_umTokenSeparadoDoLivro() {
        val text = "O primeiro está em João 3:16."
        val tokens = Tokenizer.tokenize(text)
        assertEquals(listOf("O", "primeiro", "está", "em", "João", "3:16"), tokens.map { display(text, it) })
        val ref = tokens.last().spoken
        assertTrue(w("tres dezesseis") in ref)
        assertTrue(w("tres e dezesseis") in ref)
        assertTrue(w("capitulo tres versiculo dezesseis") in ref)
    }

    @Test
    fun exemplosDoTrd_13_4a7() {
        val ref = tokenOf("Também vale ler 1 Coríntios 13:4-7, que fala de amor", "13:4-7").spoken
        assertTrue(w("treze quatro a sete") in ref)
        assertTrue(w("treze de quatro a sete") in ref)
        assertTrue(w("capitulo treze versiculos quatro a sete") in ref)
    }

    @Test
    fun exemplosDoTrd_12_1e2() {
        // Variação do "Romanos 12:2" do T02, escrita no teste (spec, critério de intervalos e listas).
        val text = "E o último, em Romanos 12:1, 2. Também"
        val ref = tokenOf(text, "12:1, 2").spoken
        assertTrue(w("doze um e dois") in ref)
        assertTrue(w("doze versiculos um e dois") in ref)
        assertTrue(w("capitulo doze versiculos um e dois") in ref)
        assertEquals("Romanos", display(text, Tokenizer.tokenize(text)[4]))
    }

    @Test
    fun numerosDoT02() {
        val t02 = TestTexts.read("T02")
        val f1914 = tokenOf(t02, "1914").spoken
        assertTrue(w("mil novecentos e catorze") in f1914)
        assertTrue(w("mil novecentos e quatorze") in f1914)
        assertTrue(w("tres virgula cinco por cento") in tokenOf(t02, "3,5%").spoken)
        val f2414 = tokenOf(t02, "24:14").spoken
        assertTrue(w("vinte e quatro catorze") in f2414)
        assertTrue(w("vinte e quatro quatorze") in f2414)
    }

    @Test
    fun livroComNumero_1Corintios_D17() {
        val t02 = TestTexts.read("T02")
        val tokens = Tokenizer.tokenize(t02)
        val book = tokens.first { display(t02, it) == "1 Coríntios" }
        assertTrue(w("primeira corintios") in book.spoken)
        assertTrue(w("primeiro corintios") in book.spoken)
        assertTrue(w("um corintios") in book.spoken)
        assertEquals("13:4-7", display(t02, tokens[book.index + 1]))
    }

    @Test
    fun com1Capitulo_numeroProprioComMasculinoEFeminino() {
        val t02 = TestTexts.read("T02")
        val tokens = Tokenizer.tokenize(t02)
        val com = tokens.indices.first { display(t02, tokens[it]) == "Com" }
        val one = tokens[com + 1]
        assertEquals("1", display(t02, one))
        assertEquals(listOf(w("um"), w("uma")), one.spoken)
        assertEquals("capítulo", display(t02, tokens[com + 2]))
    }

    @Test
    fun numerosDoL30() {
        val l30 = TestTexts.read("L30")
        val f340 = tokenOf(l30, "340").spoken
        assertTrue(w("trezentos e quarenta") in f340)
        assertTrue(w("trezentas e quarenta") in f340)
        val tokens = Tokenizer.tokenize(l30)
        val list = tokens.first { display(l30, it) == "5:3, 4" }
        assertTrue(w("cinco tres e quatro") in list.spoken)
        assertEquals(listOf(listOf("o")), tokens[list.index + 1].spoken)
    }

    @Test
    fun intervaloComTravessaoEComA_igualAoHifen() {
        // Variações do "13:4-7" do T02, escritas no teste.
        val hyphen = tokenOf("ler 1 Coríntios 13:4-7, que", "13:4-7").spoken
        assertEquals(hyphen, tokenOf("ler 1 Coríntios 13:4–7, que", "13:4–7").spoken)
        assertEquals(hyphen, tokenOf("ler 1 Coríntios 13:4 a 7, que", "13:4 a 7").spoken)
    }

    @Test
    fun listaDeTresItens() {
        val ref = tokenOf("E o último, em Romanos 12:1, 2, 5. Também", "12:1, 2, 5").spoken
        assertTrue(w("doze um dois e cinco") in ref)
    }

    @Test
    fun palavraHifenizada_umTokenComDuasFormas_D18() {
        val t03 = TestTexts.read("T03")
        assertEquals(listOf(w("bom dia"), w("bomdia")), tokenOf(t03, "bom-dia").spoken)
        val l30 = TestTexts.read("L30")
        assertEquals(listOf(w("escreve los"), w("escrevelos")), tokenOf(l30, "escrevê-los").spoken)
    }

    @Test
    fun pesoDasPalavrasComuns_T09() {
        val tokens = TestTexts.tokens("T09")
        var common = 0
        for (token in tokens) {
            val single = token.spoken.singleOrNull()?.singleOrNull()
            if (single != null && single in AlignConstants.COMMON_WORDS) {
                common++
                assertEquals(AlignConstants.COMMON_WORD_WEIGHT, token.weight, 0f)
                assertTrue(token.weight < 1f)
            } else {
                assertEquals(1f, token.weight, 0f)
            }
        }
        assertTrue("T09 tem palavras comuns (Q3)", common >= 10)
        val t09 = TestTexts.read("T09")
        val e = tokens.first { display(t09, it) == "É" }
        assertEquals(AlignConstants.COMMON_WORD_WEIGHT, e.weight, 0f)
    }

    @Test
    fun inicioDeFrase_T09temQuinzeFrases_D20() {
        val t09 = TestTexts.read("T09")
        val starts = TestTexts.tokens("T09").filter { it.sentenceStart }.map { display(t09, it) }
        assertEquals(
            listOf("Sim", "E", "É", "De", "Que", "Foi", "E", "Nada", "Só", "E", "Deu", "Que", "Eu", "Não", "E"),
            starts,
        )
    }

    @Test
    fun inicioDeFrase_T01primeiroTokenDeCadaParagrafo() {
        val t01 = TestTexts.read("T01")
        val tokens = TestTexts.tokens("T01")
        val paragraphStarts = Regex("(?m)^(?=\\S)").findAll(t01).map { it.range.first }.toList()
        assertEquals(5, paragraphStarts.size)
        for (offset in paragraphStarts) {
            assertTrue("parágrafo em $offset", tokens.first { it.display.first == offset }.sentenceStart)
        }
        assertFalse(tokens[1].sentenceStart)
    }
}
