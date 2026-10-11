package io.github.gigioderico.tribuna.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registra verifyMergedManifest (todas as variantes) e verify16kAlignment (release) no módulo Android.
 * O build verifica o manifesto mesclado (TRD §Manifesto; OPR-004): `check` depende do agregado, e
 * `assemble<Variante>` e `bundle<Variante>` dependem da verificação da variante.
 */
class AndroidVerificationsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.application") {
            val android = project.extensions.getByType(ApplicationExtension::class.java)
            val components = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            val aggregate =
                project.tasks.register("verifyMergedManifest") {
                    group = "verification"
                    description = "Falha se o manifesto mesclado de qualquer variante tiver permissão além de RECORD_AUDIO."
                }
            project.tasks.named("check") { dependsOn(aggregate) }
            components.onVariants { variant ->
                val suffix = variant.name.replaceFirstChar { it.uppercase() }
                val perVariant =
                    project.tasks.register("verify${suffix}MergedManifest", VerifyMergedManifestTask::class.java) {
                        group = "verification"
                        mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                        allowedPermissions.set(setOf(ManifestPermissionCheck.RECORD_AUDIO))
                        requiredPermissions.set(setOf(ManifestPermissionCheck.RECORD_AUDIO))
                    }
                aggregate.configure { dependsOn(perVariant) }
                val packaging = setOf("assemble$suffix", "bundle$suffix")
                project.tasks.configureEach {
                    if (name in packaging) dependsOn(perVariant)
                }
                if (variant.buildType == "release") {
                    project.tasks.register("verify16kAlignment", Verify16kAlignmentTask::class.java) {
                        group = "verification"
                        description = "Alinhamento de 16 KB de libvosk.so e libjnidispatch.so no APK e no AAB de release."
                        apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
                        bundleFile.set(variant.artifacts.get(SingleArtifact.BUNDLE))
                        builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
                        zipalign.set(
                            components.sdkComponents.sdkDirectory.map {
                                it.file("build-tools/${android.buildToolsVersion}/zipalign")
                            },
                        )
                        requiredLibraries.set(setOf("libvosk.so", "libjnidispatch.so"))
                        pageSizeKb.set(16)
                    }
                }
            }
        }
    }
}
