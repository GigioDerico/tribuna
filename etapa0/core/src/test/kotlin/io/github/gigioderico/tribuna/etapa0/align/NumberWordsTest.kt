package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberWordsTest {
    private fun w(text: String) = text.split(' ')

    @Test
    fun masculine_formaUsual() {
        assertEquals(w("zero"), NumberWords.masculine(0))
        assertEquals(w("vinte e quatro"), NumberWords.masculine(24))
        assertEquals(w("cem"), NumberWords.masculine(100))
        assertEquals(w("cento e um"), NumberWords.masculine(101))
        assertEquals(w("trezentos e quarenta"), NumberWords.masculine(340))
        assertEquals(w("mil"), NumberWords.masculine(1000))
        assertEquals(w("mil e cinco"), NumberWords.masculine(1005))
        assertEquals(w("mil e novecentos"), NumberWords.masculine(1900))
        assertEquals(w("mil novecentos e catorze"), NumberWords.masculine(1914))
        assertEquals(w("tres mil e seiscentos"), NumberWords.masculine(3600))
        assertEquals(
            w("novecentos e noventa e nove mil novecentos e noventa e nove"),
            NumberWords.masculine(999_999),
        )
    }

    @Test
    fun cardinalForms_grafiasAlternativasEFeminino_D15() {
        val f1914 = NumberWords.cardinalForms(1914)
        assertEquals(w("mil novecentos e catorze"), f1914.first())
        assertTrue(w("mil novecentos e quatorze") in f1914)
        assertEquals(listOf(w("trezentos e quarenta"), w("trezentas e quarenta")), NumberWords.cardinalForms(340))
        assertEquals(listOf(w("um"), w("uma")), NumberWords.cardinalForms(1))
        assertEquals(listOf(w("vinte e quatro")), NumberWords.cardinalForms(24))
    }

    @Test
    fun masculineForms_semFeminino() {
        assertEquals(listOf(w("um")), NumberWords.masculineForms(1))
        assertEquals(listOf(w("catorze"), w("quatorze")), NumberWords.masculineForms(14))
    }

    @Test(expected = IllegalArgumentException::class)
    fun masculine_foraDaFaixaDeD14_falha() {
        NumberWords.masculine(1_000_000)
    }
}
