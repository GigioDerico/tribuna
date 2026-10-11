package io.github.gigioderico.tribuna.buildlogic

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** ELF sintéticos (little-endian, como no Android) só com cabeçalho e tabela de segmentos. */
object ElfFixtures {
    private const val PT_LOAD = 1
    private const val PT_DYNAMIC = 2

    fun elf64(
        vararg loadAligns: Long,
        nonLoadAlign: Long? = null,
    ): ByteArray {
        val headers = loadAligns.map { PT_LOAD to it } + listOfNotNull(nonLoadAlign?.let { PT_DYNAMIC to it })
        val phoff = 64
        val phentsize = 56
        val buf = ByteBuffer.allocate(phoff + phentsize * headers.size).order(ByteOrder.LITTLE_ENDIAN)
        magic(buf, elfClass = 2)
        buf.putLong(0x20, phoff.toLong())
        buf.putShort(0x36, phentsize.toShort())
        buf.putShort(0x38, headers.size.toShort())
        headers.forEachIndexed { i, (type, align) ->
            val base = phoff + i * phentsize
            buf.putInt(base, type)
            buf.putLong(base + 0x30, align)
        }
        return buf.array()
    }

    fun elf32(vararg loadAligns: Long): ByteArray {
        val phoff = 52
        val phentsize = 32
        val buf = ByteBuffer.allocate(phoff + phentsize * loadAligns.size).order(ByteOrder.LITTLE_ENDIAN)
        magic(buf, elfClass = 1)
        buf.putInt(0x1C, phoff)
        buf.putShort(0x2A, phentsize.toShort())
        buf.putShort(0x2C, loadAligns.size.toShort())
        loadAligns.forEachIndexed { i, align ->
            val base = phoff + i * phentsize
            buf.putInt(base, PT_LOAD)
            buf.putInt(base + 0x1C, align.toInt())
        }
        return buf.array()
    }

    private fun magic(
        buf: ByteBuffer,
        elfClass: Int,
    ) {
        buf.put(0, 0x7F.toByte())
        buf.put(1, 'E'.code.toByte())
        buf.put(2, 'L'.code.toByte())
        buf.put(3, 'F'.code.toByte())
        buf.put(4, elfClass.toByte())
        buf.put(5, 1.toByte()) // little-endian
    }
}
