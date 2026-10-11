package io.github.gigioderico.tribuna.etapa0.ui

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.align.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
