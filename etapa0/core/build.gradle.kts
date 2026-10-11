plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
    alias(libs.plugins.android.lint)
    alias(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(
        libs.versions.jvm
            .get()
            .toInt(),
    )
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

ktlint {
    version.set(libs.versions.ktlint.get())
}

// O comando único do CLAUDE.md é `./gradlew testDebugUnitTest`; num módulo JVM a tarefa se chama `test`.
tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Alias de test, para o comando testDebugUnitTest do CLAUDE.md cobrir o :core."
    dependsOn(tasks.named("test"))
}
