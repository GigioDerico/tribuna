package io.github.gigioderico.tribuna.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Falha se o manifesto mesclado tiver permissão fora da lista (CLAUDE.md, regra 1). Sem saídas: roda sempre. */
abstract class VerifyMergedManifestTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val allowedPermissions: SetProperty<String>

    @get:Input
    abstract val requiredPermissions: SetProperty<String>

    @TaskAction
    fun verify() {
        val file = mergedManifest.get().asFile
        val xml = file.readText()
        val problems = ManifestPermissionCheck.problems(xml, allowedPermissions.get(), requiredPermissions.get())
        if (problems.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("$name: o manifesto mesclado ${file.path} viola a regra 1 do CLAUDE.md (TRD §Manifesto):")
                    problems.forEach { appendLine("  - $it") }
                    append("Permitidas: ${allowedPermissions.get().sorted().joinToString()}")
                },
            )
        }
        logger.lifecycle("$name: OK ${file.path} -> ${ManifestPermissionCheck.declaredPermissions(xml).joinToString()}")
    }
}
