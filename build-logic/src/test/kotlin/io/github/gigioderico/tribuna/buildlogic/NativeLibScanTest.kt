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

    private fun apk(vararg entries: Pair<String, ByteArray>): File = archive("app.apk", *entries)

    private fun archive(
        name: String,
        vararg entries: Pair<String, ByteArray>,
    ): File {
        val file = tmp.newFile(name)
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

    @Test
    fun aabComAsDuasLibsAlinhadasEmBaseLib_semProblemas() {
        val file =
            archive(
                "app.aab",
                "base/lib/arm64-v8a/libvosk.so" to aligned,
                "base/lib/arm64-v8a/libjnidispatch.so" to aligned,
                "base/lib/x86_64/libvosk.so" to aligned,
                "base/lib/x86_64/libjnidispatch.so" to aligned,
            )
        assertEquals(
            emptyList<String>(),
            NativeLibScan.check(file, required, minAlign = 16_384L, libRoot = NativeLibScan.BUNDLE_LIB_ROOT),
        )
    }

    @Test
    fun aabComLibDesalinhadaEAbiIncompleta_eReportado() {
        val file =
            archive(
                "app.aab",
                "base/lib/arm64-v8a/libvosk.so" to misaligned,
                "base/lib/arm64-v8a/libjnidispatch.so" to aligned,
                "base/lib/x86/libvosk.so" to aligned,
            )
        assertEquals(
            listOf(
                "app.aab: base/lib/arm64-v8a/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)",
                "app.aab: x86 sem libjnidispatch.so",
            ),
            NativeLibScan.check(file, required, minAlign = 16_384L, libRoot = NativeLibScan.BUNDLE_LIB_ROOT),
        )
    }

    @Test
    fun aabSemBaseLib_falhaExplicitamente() {
        // Um AAB com as libs fora de base/lib/ (ou um APK lido como AAB) não pode passar em silêncio (Q3).
        val file = archive("app.aab", "lib/arm64-v8a/libvosk.so" to aligned, "lib/arm64-v8a/libjnidispatch.so" to aligned)
        assertEquals(
            listOf("app.aab: nenhuma biblioteca nativa em base/lib/<abi>/; exigidas: libjnidispatch.so, libvosk.so"),
            NativeLibScan.check(file, required, minAlign = 16_384L, libRoot = NativeLibScan.BUNDLE_LIB_ROOT),
        )
    }

    @Test
    fun relatorio_listaCadaLibComOAlinhamento() {
        val file =
            archive(
                "app.aab",
                "base/lib/x86/libvosk.so" to misaligned,
                "base/lib/arm64-v8a/libvosk.so" to aligned,
                "base/assets/model-pt/uuid" to ByteArray(4),
            )
        assertEquals(
            listOf("app.aab: base/lib/arm64-v8a/libvosk.so LOAD 16384", "app.aab: base/lib/x86/libvosk.so LOAD 4096"),
            NativeLibScan.report(file, NativeLibScan.BUNDLE_LIB_ROOT),
        )
    }
}
