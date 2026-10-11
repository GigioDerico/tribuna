package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Leitura da tabela de símbolos de um FST do OpenFst (léxico do modelo; OPR-020). */
class FstSymbolsTest {
    private class Bytes {
        val out = ByteArrayOutputStream()

        fun int32(v: Int) = apply { repeat(4) { out.write((v shr (8 * it)) and 0xFF) } }

        fun int64(v: Long) = apply { repeat(8) { out.write(((v shr (8 * it)) and 0xFF).toInt()) } }

        fun string(s: String) =
            apply {
                val b = s.toByteArray(Charsets.UTF_8)
                int32(b.size)
                out.write(b)
            }
    }

    private fun fst(
        symbols: List<String>,
        flags: Int = 3,
        magic: Int = 0x7EB2FDD6,
    ): ByteArray {
        val b =
            Bytes()
                .int32(magic)
                .string("ngram")
                .string("standard")
                .int32(4)
                .int32(flags)
        b
            .int64(0)
            .int64(1)
            .int64(10)
            .int64(0)
        b
            .int32(0x7EB2FB74)
            .string("words.txt")
            .int64(symbols.size.toLong())
            .int64(symbols.size.toLong())
        symbols.forEachIndexed { i, s -> b.string(s).int64(i.toLong()) }
        b.int32(0) // resto do FST, não lido
        return b.out.toByteArray()
    }

    @Test
    fun leATabelaDeSimbolosDeEntrada() {
        val symbols = listOf("<eps>", "!SIL", "[unk]", "ouvir", "três", "versículo")
        assertEquals(symbols, FstSymbols.inputSymbols(ByteArrayInputStream(fst(symbols))))
    }

    @Test
    fun naoEFst_falha() {
        assertTrue(
            runCatching { FstSymbols.inputSymbols(ByteArrayInputStream(fst(listOf("a"), magic = 1))) }.exceptionOrNull() is IOException,
        )
    }

    @Test
    fun fstSemTabelaDeEntrada_falha() {
        assertTrue(
            runCatching { FstSymbols.inputSymbols(ByteArrayInputStream(fst(listOf("a"), flags = 2))) }.exceptionOrNull() is IOException,
        )
    }
}
