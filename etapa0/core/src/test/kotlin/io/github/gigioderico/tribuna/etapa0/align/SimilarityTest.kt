package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarityTest {
    @Test
    fun levenshtein_casosBasicos() {
        assertEquals(0, Similarity.levenshtein("escolha", "escolha"))
        assertEquals(1, Similarity.levenshtein("pratica", "praticas"))
        assertEquals(5, Similarity.levenshtein("", "ouvir"))
        assertEquals(3, Similarity.levenshtein("custo", "susto2x"))
    }

    @Test
    fun of_eUmMenosLevenshteinSobreOMaiorComprimento() {
        assertEquals(1.0, Similarity.of("escolha", "escolha"), 1e-12)
        assertEquals(0.8, Similarity.of("ouvir", "ouvi"), 1e-12)
        assertEquals(0.0, Similarity.of("de", "um"), 1e-12)
    }

    @Test
    fun of_unkNuncaCasa_D13() {
        assertEquals(0.0, Similarity.of(Similarity.UNKNOWN, "unk"), 0.0)
        assertEquals(0.0, Similarity.of("unk", Similarity.UNKNOWN), 0.0)
        assertEquals(0.0, Similarity.of(Similarity.UNKNOWN, Similarity.UNKNOWN), 0.0)
    }
}
