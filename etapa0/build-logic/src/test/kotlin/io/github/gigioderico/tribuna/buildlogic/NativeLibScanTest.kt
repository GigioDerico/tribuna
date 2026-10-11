package io.github.gigioderico.tribuna.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class NativeLibScanTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val required = setOf("libvosk.so", "libjnidispatch.so")
    private val aligned = ElfFixtures.elf64(16_384L, 16_384L)
    private val misaligned = ElfFixtures.elf64(4_096L)

    private fun apk(vararg entries: Pair<String, ByteArray>): File {
        val file = tmp.newFile("app.apk")
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    @Test
    fun duasAbisComAsDuasLibsAlinhadas_semProblemas() {
        val file =
            apk(
                "lib/arm64-v8a/libvosk.so" to aligned,
                "lib/arm64-v8a/libjnidispatch.so" to aligned,
                "lib/x86_64/libvosk.so" to aligned,
                "lib/x86_64/libjnidispatch.so" to aligned,
                "classes.dex" to ByteArray(8),
            )
        assertEquals(emptyList<String>(), NativeLibScan.check(file, required, minAlign = 16_384L))
    }

    @Test
    fun abiSemLibjnidispatch_eReportada() {
        val file =
            apk(
                "lib/arm64-v8a/libvosk.so" to aligned,
                "lib/arm64-v8a/libjnidispatch.so" to aligned,
                "lib/x86_64/libvosk.so" to aligned,
            )
        assertEquals(listOf("app.apk: x86_64 sem libjnidispatch.so"), NativeLibScan.check(file, required, minAlign = 16_384L))
    }

    @Test
    fun semNenhumaLibNativa_falhaExplicitamente() {
        val file = apk("classes.dex" to ByteArray(8))
        assertEquals(
            listOf("app.apk: nenhuma biblioteca nativa em lib/<abi>/; exigidas: libjnidispatch.so, libvosk.so"),
            NativeLibScan.check(file, required, minAlign = 16_384L),
        )
    }

    @Test
    fun libExigidaDesalinhada_eReportada() {
        val file =
            apk(
                "lib/arm64-v8a/libvosk.so" to misaligned,
                "lib/arm64-v8a/libjnidispatch.so" to aligned,
            )
        assertEquals(
            listOf("app.apk: lib/arm64-v8a/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)"),
            NativeLibScan.check(file, required, minAlign = 16_384L),
        )
    }

    @Test
    fun outraLibDesalinhada_tambemEReportada() {
        val file =
            apk(
                "lib/arm64-v8a/libvosk.so" to aligned,
                "lib/arm64-v8a/libjnidispatch.so" to aligned,
                "lib/arm64-v8a/libc++_shared.so" to misaligned,
            )
        assertEquals(
            listOf("app.apk: lib/arm64-v8a/libc++_shared.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)"),
            NativeLibScan.check(file, required, minAlign = 16_384L),
        )
    }
}
