package io.github.gigioderico.tribuna.buildlogic

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lê o p_align dos segmentos PT_LOAD de um ELF (32 ou 64 bits), como `llvm-objdump -p | grep LOAD`
 * (https://developer.android.com/guide/practices/page-sizes). TRD §Qualidade de código.
 */
object ElfAlignment {
    private const val PT_LOAD = 1

    fun loadSegmentAlignments(bytes: ByteArray): List<Long> {
        require(
            bytes.size >= 52 &&
                bytes[0] == 0x7F.toByte() &&
                bytes[1] == 'E'.code.toByte() &&
                bytes[2] == 'L'.code.toByte() &&
                bytes[3] == 'F'.code.toByte(),
        ) { "não é um arquivo ELF" }
        val is64 =
            when (bytes[4].toInt()) {
                1 -> false
                2 -> true
                else -> throw IllegalArgumentException("classe ELF desconhecida: ${bytes[4]}")
            }
        val order =
            when (bytes[5].toInt()) {
                1 -> ByteOrder.LITTLE_ENDIAN
                2 -> ByteOrder.BIG_ENDIAN
                else -> throw IllegalArgumentException("ordem de bytes ELF desconhecida: ${bytes[5]}")
            }
        require(!is64 || bytes.size >= 64) { "cabeçalho ELF64 truncado" }
        val buf = ByteBuffer.wrap(bytes).order(order)
        val phoff = if (is64) buf.getLong(0x20) else buf.getInt(0x1C).toLong() and 0xFFFFFFFFL
        val phentsize = buf.getShort(if (is64) 0x36 else 0x2A).toInt() and 0xFFFF
        val phnum = buf.getShort(if (is64) 0x38 else 0x2C).toInt() and 0xFFFF
        require(phoff >= 0 && phoff + phentsize.toLong() * phnum <= bytes.size) { "tabela de segmentos fora do arquivo" }
        return (0 until phnum).mapNotNull { i ->
            val base = (phoff + i.toLong() * phentsize).toInt()
            when {
                buf.getInt(base) != PT_LOAD -> null
                is64 -> buf.getLong(base + 0x30)
                else -> buf.getInt(base + 0x1C).toLong() and 0xFFFFFFFFL
            }
        }
    }
}
