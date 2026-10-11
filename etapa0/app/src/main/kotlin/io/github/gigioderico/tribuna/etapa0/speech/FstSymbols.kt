package io.github.gigioderico.tribuna.etapa0.speech

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Lê a tabela de símbolos de entrada de um FST do OpenFst (formato binário). No `vosk-model-small-pt-0.3`, o
 * léxico do modelo é a tabela de `Gr.fst` (o modelo não traz `words.txt`; OPR-020). Só lê o cabeçalho.
 */
object FstSymbols {
    private const val FST_MAGIC = 0x7EB2FDD6
    private const val SYMBOLS_MAGIC = 0x7EB2FB74
    private const val HAS_INPUT_SYMBOLS = 1
    private const val MAX_STRING = 1 shl 16
    private const val MAX_SYMBOLS = 10_000_000L

    fun inputSymbols(stream: InputStream): List<String> {
        val input = DataInputStream(BufferedInputStream(stream))
        if (int32(input) != FST_MAGIC) throw IOException("não é um FST do OpenFst")
        string(input) // tipo do FST
        string(input) // tipo do arco
        int32(input) // versão
        val flags = int32(input)
        int64(input) // propriedades
        int64(input) // estado inicial
        int64(input) // número de estados
        int64(input) // número de arcos
        if (flags and HAS_INPUT_SYMBOLS == 0) throw IOException("FST sem tabela de símbolos de entrada")
        if (int32(input) != SYMBOLS_MAGIC) throw IOException("tabela de símbolos inválida")
        string(input) // nome da tabela
        int64(input) // próxima chave livre
        val size = int64(input)
        if (size < 0 || size > MAX_SYMBOLS) throw IOException("tamanho de tabela inválido: $size")
        return List(size.toInt()) {
            val symbol = string(input)
            int64(input) // chave
            symbol
        }
    }

    private fun int32(input: DataInputStream): Int {
        val b0 = input.readUnsignedByte()
        val b1 = input.readUnsignedByte()
        val b2 = input.readUnsignedByte()
        val b3 = input.readUnsignedByte()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun int64(input: DataInputStream): Long {
        val low = int32(input).toLong() and 0xFFFFFFFFL
        val high = int32(input).toLong()
        return low or (high shl 32)
    }

    private fun string(input: DataInputStream): String {
        val length = int32(input)
        if (length < 0 || length > MAX_STRING) throw IOException("texto de tamanho inválido: $length")
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}
