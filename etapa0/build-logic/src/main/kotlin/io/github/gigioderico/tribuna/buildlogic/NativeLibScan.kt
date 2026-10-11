package io.github.gigioderico.tribuna.buildlogic

import java.io.File
import java.util.zip.ZipFile

/**
 * Varre as .so em lib/<abi>/ de um APK: exige as bibliotecas pedidas em toda ABI e confere o alinhamento
 * dos segmentos LOAD de toda .so (TRD §Qualidade de código: "em todas as ABIs").
 */
object NativeLibScan {
    private val LIB_ENTRY = Regex("^lib/([^/]+)/([^/]+\\.so)$")

    fun check(
        apk: File,
        requiredLibs: Set<String>,
        minAlign: Long,
    ): List<String> {
        val problems = mutableListOf<String>()
        ZipFile(apk).use { zip ->
            val byAbi =
                zip
                    .entries()
                    .asSequence()
                    .mapNotNull { entry -> LIB_ENTRY.matchEntire(entry.name)?.let { it.groupValues[1] to entry } }
                    .groupBy({ it.first }, { it.second })
            if (byAbi.isEmpty()) {
                problems += "${apk.name}: nenhuma biblioteca nativa em lib/<abi>/; exigidas: ${requiredLibs.sorted().joinToString()}"
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
