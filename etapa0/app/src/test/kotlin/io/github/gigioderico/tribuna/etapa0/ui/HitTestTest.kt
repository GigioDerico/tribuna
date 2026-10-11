package io.github.gigioderico.tribuna.etapa0.ui

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.align.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Toque em palavra ou vazio (TRD §Hit-test 1–4; spec 0003, D10): parte pura. */
class HitTestTest {
    private val text = TestFiles.text("T01")
    private val tokens = Tokenizer.tokenize(text)

    @Test
    fun caractereDeCadaToken_achaOToken() {
        for (token in tokens) {
            assertEquals(token.index, HitTest.tokenAt(token.display.first, tokens))
            assertEquals(token.index, HitTest.tokenAt(token.display.last, tokens))
        }
    }

    @Test
    fun espacoEntrePalavras_naoEToken() {
        val gap = tokens[0].display.last + 1
        assertEquals(' ', text[gap])
        assertNull(HitTest.tokenAt(gap, tokens))
        assertNull(HitTest.tokenAt(text.length + 10, tokens))
    }

    private val box = WordBox(left = 100f, top = 50f, right = 180f, bottom = 90f)

    @Test
    fun dentroDoRetanguloAmpliado_contaComoPalavra() {
        assertEquals(true, HitTest.inside(95f, 45f, box, slop = 8f))
        assertEquals(true, HitTest.inside(188f, 98f, box, slop = 8f))
    }

    @Test
    fun foraDoRetanguloAmpliado_eVazio() {
        assertEquals(false, HitTest.inside(91f, 70f, box, slop = 8f))
        assertEquals(false, HitTest.inside(140f, 99f, box, slop = 8f))
        assertEquals(false, HitTest.inside(189f, 70f, box, slop = 8f))
        assertEquals(false, HitTest.inside(140f, 41f, box, slop = 8f))
    }

    @Test
    fun uniaoDosCaracteres_eORetanguloDaPalavra() {
        val union = HitTest.union(listOf(WordBox(0f, 10f, 5f, 20f), WordBox(5f, 12f, 11f, 22f)))
        assertEquals(WordBox(0f, 10f, 11f, 22f), union)
        assertNull(HitTest.union(emptyList()))
    }

    @Test
    fun toqueLongeDoTexto_comOCaractereMaisProximo_eVazio() {
        // getOffsetForPosition devolve o caractere mais próximo mesmo longe do texto (TRD §Hit-test 1).
        val charBox = { _: Int -> WordBox(0f, 0f, 10f, 20f) }
        assertEquals(0, HitTest.tappedToken(5f, 10f, offset = 0, tokens = tokens, boxOf = charBox, slop = 8f))
        assertNull(HitTest.tappedToken(5f, 300f, offset = 0, tokens = tokens, boxOf = charBox, slop = 8f))
    }

    @Test
    fun noLimiteExatoDoRetanguloAmpliado_contaComoPalavra() {
        assertEquals(true, HitTest.inside(92f, 70f, box, slop = 8f))
        assertEquals(true, HitTest.inside(188f, 70f, box, slop = 8f))
        assertEquals(true, HitTest.inside(140f, 42f, box, slop = 8f))
        assertEquals(true, HitTest.inside(140f, 98f, box, slop = 8f))
    }

    @Test
    fun uniaoNaoDependeDaOrdemNemDeQualCaractereEOMaior() {
        val wide = WordBox(0f, 0f, 50f, 50f)
        val small = WordBox(10f, 10f, 20f, 20f)
        assertEquals(wide, HitTest.union(listOf(wide, small)))
        assertEquals(wide, HitTest.union(listOf(small, wide)))
        val a = WordBox(10f, 0f, 20f, 10f)
        val b = WordBox(0f, 10f, 5f, 20f)
        assertEquals(WordBox(0f, 0f, 20f, 20f), HitTest.union(listOf(a, b)))
        assertEquals(WordBox(0f, 0f, 20f, 20f), HitTest.union(listOf(b, a)))
        assertEquals(a, HitTest.union(listOf(a)))
    }

    @Test
    fun toqueNaSegundaPalavra_usaOsCaracteresDaSegundaPalavra() {
        val boxOf = { offset: Int -> WordBox(offset * 10f, 0f, offset * 10f + 10f, 20f) }
        val second = tokens[1]
        val x = second.display.first * 10f + 5f
        assertEquals(1, HitTest.tappedToken(x, 10f, offset = second.display.first, tokens = tokens, boxOf = boxOf, slop = 8f))
        // Fora do retângulo da segunda palavra, mesmo com o caractere mais próximo dentro dela.
        assertNull(HitTest.tappedToken(x, 100f, offset = second.display.first, tokens = tokens, boxOf = boxOf, slop = 8f))
        // O retângulo é a união de todos os caracteres: o último caractere da palavra também conta.
        val lastX = second.display.last * 10f + 5f
        assertEquals(1, HitTest.tappedToken(lastX, 10f, offset = second.display.first, tokens = tokens, boxOf = boxOf, slop = 8f))
    }

    @Test
    fun buscaBinaria_achaOTokenEmTextoLongo_eNaoTrataOffsetNegativoComoToken() {
        val big = Tokenizer.tokenize(TestFiles.text("T09"))
        for (token in big) assertEquals(token.index, HitTest.tokenAt(token.display.first, big))
        assertNull(HitTest.tokenAt(-1, big))
        assertNull(HitTest.tokenAt(0, emptyList()))
    }

    @Test
    fun buscaBinaria_olhaPoucosTokens() {
        val big = Tokenizer.tokenize(TestFiles.text("T09"))
        var reads = 0
        val counting =
            object : AbstractList<io.github.gigioderico.tribuna.etapa0.align.Token>() {
                override val size get() = big.size

                override fun get(index: Int): io.github.gigioderico.tribuna.etapa0.align.Token {
                    reads++
                    return big[index]
                }
            }
        assertEquals(0, HitTest.tokenAt(big[0].display.first, counting))
        assertTrue("leituras: $reads de ${big.size} tokens", big.size > 50 && reads <= 8)
    }
}
