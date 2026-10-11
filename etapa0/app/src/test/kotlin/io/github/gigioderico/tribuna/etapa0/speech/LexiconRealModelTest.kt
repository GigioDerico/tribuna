package io.github.gigioderico.tribuna.etapa0.speech

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.align.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Evidência por comando contra o modelo real em assets (Q4; OPR-020; spec 0003, A4, D12): o léxico do
 * `vosk-model-small-pt-0.3` tem a grafia com acento, e `Token.spoken` não; o vocabulário expandido cobre isso.
 */
class LexiconRealModelTest {
    private val modelDir = TestFiles.modelDir

    private val lexiconWords: List<String> by lazy {
        val fst = File(modelDir, "Gr.fst")
        assertTrue("modelo ausente ou só ponteiro do LFS: rode scripts/baixar-modelo.sh e git lfs pull", fst.length() > 1_000_000)
        fst.inputStream().use(FstSymbols::inputSymbols)
    }

    @Test
    fun uuidDoModelo_temNomeESha256DoZip() {
        val uuid = File(modelDir, "uuid").readLines().first()
        assertTrue(uuid, Regex("^vosk-model-small-pt-0\\.3 sha256=[0-9a-f]{64}$").matches(uuid))
    }

    @Test
    fun lexicoDoModelo_temAGrafiaComAcento() {
        val words = lexiconWords.toSet()
        assertEquals(99_105, lexiconWords.size)
        assertTrue(listOf("versículo", "três", "não", "coração", "[unk]").all { it in words })
        assertFalse("versiculo" in words)
        assertFalse("nao" in words)
    }

    @Test
    fun t02_formasFaladasForaDoLexico_eAsQueContinuamSemGrafiaDepoisDaExpansao() {
        val words = lexiconWords.toSet()
        val lexicon = Lexicon(lexiconWords)
        val spoken = Tokenizer.tokenize(TestFiles.text("T02")).flatMap { t -> t.spoken.flatten() }.distinct()
        val outside = spoken.filter { it !in words }
        assertEquals(12, outside.size)
        assertEquals(listOf("corintios"), outside.filter { lexicon.spellings(it).isEmpty() })
    }

    @Test
    fun t02_vocabularioExpandido_trazAGrafiaDoLexico() {
        val text = TestFiles.text("T02")
        val list = Vocabulary.build(text, Tokenizer.tokenize(text), Lexicon(lexiconWords))
        assertTrue(listOf("versículo", "três", "você", "capítulo").all { it in list })
        assertEquals(Vocabulary.UNKNOWN, list.last())
    }
}
