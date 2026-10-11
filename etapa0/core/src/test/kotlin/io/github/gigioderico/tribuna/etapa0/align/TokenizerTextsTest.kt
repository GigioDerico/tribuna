package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Propriedades do Tokenizer em todos os textos de etapa0/textos/ (spec 0002, critério de tokens). */
class TokenizerTextsTest {
    private val numeric = Regex("^[\\d.,:%\\-– a]+$")
    private val book = Regex("^[123] \\p{Lu}\\p{L}*$")

    @Test
    fun indexIgualAPosicaoEDisplayBateComUmaForma_todosOsTextos() {
        for (name in TestTexts.names) {
            val text = TestTexts.read(name)
            val tokens = Tokenizer.tokenize(text)
            assertTrue("$name sem tokens (Q3)", tokens.size > 50)
            tokens.forEachIndexed { i, token ->
                assertEquals("$name índice", i, token.index)
                val shown = text.substring(token.display.first, token.display.last + 1)
                assertTrue("$name '$shown' começa sem pontuação", shown.first().isLetterOrDigit())
                assertTrue("$name '$shown' termina sem pontuação", shown.last().isLetterOrDigit() || shown.last() == '%')
                if (shown.any { it.isDigit() }) {
                    assertTrue("$name '$shown' é número ou referência", numeric.matches(shown) || book.matches(shown))
                    assertTrue("$name '$shown' vira palavras", token.spoken.all { form -> form.none { w -> w.any { it.isDigit() } } })
                } else {
                    assertTrue("$name '$shown' bate com ${token.spoken}", Normalizer.words(shown) in token.spoken)
                }
            }
        }
    }

    @Test
    fun linhasDeNotaNaoGeramToken_T04eL30() {
        for (name in listOf("T04", "L30")) {
            val text = TestTexts.read(name)
            val noteRanges =
                Regex("(?m)^[ \\t]*//.*$").findAll(text).map { it.range }.toList()
            assertTrue("$name tem notas (Q3)", noteRanges.isNotEmpty())
            for (token in Tokenizer.tokenize(text)) {
                for (note in noteRanges) {
                    assertTrue(
                        "$name token em ${token.display} dentro da nota $note",
                        token.display.last < note.first || token.display.first > note.last,
                    )
                }
            }
        }
    }
}
