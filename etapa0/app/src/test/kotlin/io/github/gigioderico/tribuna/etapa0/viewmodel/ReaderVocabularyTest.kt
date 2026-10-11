package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.align.Tokenizer
import io.github.gigioderico.tribuna.etapa0.speech.Vocabulary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** C5 e D5 no controlador: o vocabulário restrito é do texto carregado (OPR-028; TRD Motor de voz §Vocabulário restrito). */
class ReaderVocabularyTest {
    @Test
    fun trocarDeTexto_naoReaproveitaOVocabularioDoTextoAnterior() =
        runTest {
            val h = ReaderHarness(this)
            h.controller.setVocabulary(true)
            h.load("T01")
            h.listen()
            h.controller.onStopReading()
            h.settle()
            h.load("T09")
            h.listen()
            val text = TestFiles.text("T09")
            val expected = Vocabulary.build(text, Tokenizer.tokenize(text), null)
            assertEquals(2, h.fake.recognizers.size)
            val first =
                h.fake.recognizers
                    .first()
                    .vocabulary
            val last =
                h.fake.recognizers
                    .last()
                    .vocabulary
            assertEquals(expected, last)
            assertNotEquals(first, last)
            assertEquals(expected.size, h.state.debug.vocabularySize)
        }
}
