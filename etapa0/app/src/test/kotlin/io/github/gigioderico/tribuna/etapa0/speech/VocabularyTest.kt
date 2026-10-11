package io.github.gigioderico.tribuna.etapa0.speech

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.align.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** C5 e OPR-020: lista do vocabulário restrito (TRD Motor de voz §Vocabulário restrito; spec 0003, D5). */
class VocabularyTest {
    private val t02 = TestFiles.text("T02")
    private val t02Tokens = Tokenizer.tokenize(t02)
    private val spoken = t02Tokens.flatMap { t -> t.spoken.flatten() }.distinct()

    @Test
    fun semLexico_cadaPalavraDeTokenSpokenUmaVez_maisUnk() {
        val list = Vocabulary.build(t02, t02Tokens, lexicon = null)
        assertEquals(spoken + "[unk]", list)
        assertEquals(list.size, list.distinct().size)
    }

    @Test
    fun comLexico_comecaPelaFormaDoTrd_eAcrescentaAGrafiaDoLexico() {
        val lexicon = Lexicon(listOf("<eps>", "!SIL", "#0", "[unk]", "três", "tres", "versículo", "você", "coríntio", "ouvir"))
        val list = Vocabulary.build(t02, t02Tokens, lexicon)
        assertEquals(spoken, list.take(spoken.size))
        assertEquals(listOf("três", "versículo", "você", "[unk]"), list.drop(spoken.size))
    }

    @Test
    fun comLexico_partesDePalavraComHifen_entramComoNoLexico() {
        val t03 = TestFiles.text("T03")
        val list = Vocabulary.build(t03, Tokenizer.tokenize(t03), Lexicon(listOf("bom", "dia", "escolha")))
        assertTrue("bom" in list && "dia" in list)
        assertTrue("bomdia" in list)
    }

    @Test
    fun simbolosInternosDoModelo_naoEntram() {
        val lexicon = Lexicon(listOf("<eps>", "!SIL", "#0", "[unk]"))
        assertEquals(0, lexicon.size)
        assertEquals(emptyList<String>(), lexicon.spellings("0"))
    }
}
