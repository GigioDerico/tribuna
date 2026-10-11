package io.github.gigioderico.tribuna.buildlogic

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Alinhamento de 16 KB no release (TRD §Qualidade de código):
 * - APK: segmentos ELF de toda .so, presença das bibliotecas exigidas em toda ABI e `zipalign -c -P 16 -v 4`;
 * - AAB (OPR-005): segmentos ELF e presença das bibliotecas em `base/lib/<abi>/`. O `zipalign` dos APKs que
 *   o Play gera a partir do AAB é responsabilidade do Play.
 * Sem saídas: roda sempre.
 */
abstract class Verify16kAlignmentTask
    @Inject
    constructor(
        private val execOperations: ExecOperations,
    ) : DefaultTask() {
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val apkDirectory: DirectoryProperty

        @get:Internal
        abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

        @get:InputFile
        @get:PathSensitive(PathSensitivity.NONE)
        abstract val bundleFile: RegularFileProperty

        @get:InputFile
        @get:PathSensitive(PathSensitivity.NONE)
        abstract val zipalign: RegularFileProperty

        @get:Input
        abstract val requiredLibraries: SetProperty<String>

        @get:Input
        abstract val pageSizeKb: Property<Int>

        @TaskAction
        fun verify() {
            val dir = apkDirectory.get()
            val apks =
                builtArtifactsLoader.get().load(dir)?.elements?.map { File(it.outputFile) }.orEmpty()
            if (apks.isEmpty()) throw GradleException("$name: nenhum APK de release em ${dir.asFile}")
            val kb = pageSizeKb.get()
            val problems = mutableListOf<String>()
            for (apk in apks) {
                NativeLibScan.report(apk).forEach { logger.lifecycle("$name: $it") }
                problems += NativeLibScan.check(apk, requiredLibraries.get(), minAlign = kb * 1024L)
                val output = ByteArrayOutputStream()
                val result =
                    execOperations.exec {
                        commandLine(zipalign.get().asFile.path, "-c", "-P", kb.toString(), "-v", "4", apk.path)
                        standardOutput = output
                        errorOutput = output
                        isIgnoreExitValue = true
                    }
                val lastLine = output.toString().lineSequence().lastOrNull { it.isNotBlank() }.orEmpty()
                logger.lifecycle("$name: zipalign -c -P $kb -v 4 ${apk.name} -> $lastLine")
                if (result.exitValue != 0) problems += "${apk.name}: zipalign -c -P $kb falhou (código ${result.exitValue})"
            }
            val bundle = bundleFile.get().asFile
            NativeLibScan.report(bundle, NativeLibScan.BUNDLE_LIB_ROOT).forEach { logger.lifecycle("$name: $it") }
            val bundleProblems =
                NativeLibScan.check(bundle, requiredLibraries.get(), minAlign = kb * 1024L, libRoot = NativeLibScan.BUNDLE_LIB_ROOT)
            logger.lifecycle("$name: AAB ${bundle.name} -> ${if (bundleProblems.isEmpty()) "OK" else "${bundleProblems.size} problema(s)"}")
            problems += bundleProblems
            if (problems.isNotEmpty()) {
                throw GradleException(
                    buildString {
                        appendLine("$name: alinhamento de $kb KB reprovado (TRD §Qualidade de código):")
                        problems.forEach { appendLine("  - $it") }
                    },
                )
            }
            logger.lifecycle("$name: OK ${apks.joinToString { it.name }}, ${bundle.name}")
        }
    }
