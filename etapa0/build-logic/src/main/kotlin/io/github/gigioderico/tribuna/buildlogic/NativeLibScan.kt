package io.github.gigioderico.tribuna.buildlogic

import java.io.File
import java.util.zip.ZipFile

/**
 * Varre as .so em <libRoot><abi>/ de um APK (`lib/`) ou de um AAB (`base/lib/`): exige as bibliotecas pedidas
 * em toda ABI e confere o alinhamento dos segmentos LOAD de toda .so (TRD §Qualidade de código: "em todas as
 * ABIs"; OPR-005).
 */
object NativeLibScan {
    const val APK_LIB_ROOT = "lib/"
    const val BUNDLE_LIB_ROOT = "base/lib/"

    private fun entryPattern(libRoot: String) = Regex("^" + Regex.escape(libRoot) + "([^/]+)/([^/]+\\.so)$")

    /** Uma linha por .so com os alinhamentos dos segmentos LOAD, para a evidência (spec 0003, B5). */
    fun report(
        archive: File,
        libRoot: String = APK_LIB_ROOT,
    ): List<String> =
        ZipFile(archive).use { zip ->
            val pattern = entryPattern(libRoot)
            zip
                .entries()
                .asSequence()
                .filter { pattern.matches(it.name) }
                .sortedBy { it.name }
                .map { entry ->
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    val aligns = runCatching { ElfAlignment.loadSegmentAlignments(bytes) }.getOrNull()
                    "${archive.name}: ${entry.name} LOAD ${aligns?.distinct()?.joinToString("/") ?: "ELF inválido"}"
                }.toList()
        }

    fun check(
        apk: File,
        requiredLibs: Set<String>,
        minAlign: Long,
        libRoot: String = APK_LIB_ROOT,
    ): List<String> {
        val entryPattern = entryPattern(libRoot)
        val problems = mutableListOf<String>()
        ZipFile(apk).use { zip ->
            val byAbi =
                zip
                    .entries()
                    .asSequence()
                    .mapNotNull { entry -> entryPattern.matchEntire(entry.name)?.let { it.groupValues[1] to entry } }
                    .groupBy({ it.first }, { it.second })
            if (byAbi.isEmpty()) {
                problems += "${apk.name}: nenhuma biblioteca nativa em $libRoot<abi>/; exigidas: ${requiredLibs.sorted().joinToString()}"
                return problems
            }
            for ((abi, entries) in byAbi.toSortedMap()) {
                val names = entries.map { it.name.substringAfterLast('/') }.toSet()
                (requiredLibs - names).sorted().forEach { problems += "${apk.name}: $abi sem $it" }
                for (entry in entries.sortedBy { it.name }) {
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    val aligns =
                        try {
                            ElfAlignment.loadSegmentAlignments(bytes)
                        } catch (e: IllegalArgumentException) {
                            problems += "${apk.name}: ${entry.name} não é um ELF válido (${e.message})"
                            continue
                        }
                    if (aligns.isEmpty()) problems += "${apk.name}: ${entry.name} não tem segmento LOAD"
                    aligns.filter { it < minAlign }.distinct().forEach {
                        problems += "${apk.name}: ${entry.name} tem segmento LOAD alinhado a $it bytes (mínimo $minAlign)"
                    }
                }
            }
        }
        return problems
    }
}
