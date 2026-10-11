package io.github.gigioderico.tribuna.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ElfAlignmentTest {
    @Test
    fun elf64_segmentosLoadDe16k_saoLidos() {
        assertEquals(listOf(16_384L, 16_384L), ElfAlignment.loadSegmentAlignments(ElfFixtures.elf64(16_384L, 16_384L)))
    }

    @Test
    fun elf64_segmentoLoadDe4k_eLido() {
        assertEquals(listOf(4_096L), ElfAlignment.loadSegmentAlignments(ElfFixtures.elf64(4_096L)))
    }

    @Test
    fun elf32_segmentosLoadDe16k_saoLidos() {
        assertEquals(listOf(16_384L), ElfAlignment.loadSegmentAlignments(ElfFixtures.elf32(16_384L)))
    }

    @Test
    fun elf32_segmentoLoadDe4k_eLido() {
        assertEquals(listOf(4_096L, 16_384L), ElfAlignment.loadSegmentAlignments(ElfFixtures.elf32(4_096L, 16_384L)))
    }

    @Test
    fun segmentosQueNaoSaoLoad_saoIgnorados() {
        assertEquals(listOf(16_384L), ElfAlignment.loadSegmentAlignments(ElfFixtures.elf64(16_384L, nonLoadAlign = 8L)))
    }

    @Test
    fun arquivoQueNaoEElf_lancaErro() {
        val zip = ByteArray(64).also { it[0] = 'P'.code.toByte(); it[1] = 'K'.code.toByte() }
        assertThrows(IllegalArgumentException::class.java) { ElfAlignment.loadSegmentAlignments(zip) }
    }

    @Test
    fun tabelaDeSegmentosForaDoArquivo_lancaErro() {
        val truncated = ElfFixtures.elf64(16_384L).copyOf(80)
        assertThrows(IllegalArgumentException::class.java) { ElfAlignment.loadSegmentAlignments(truncated) }
    }
}
