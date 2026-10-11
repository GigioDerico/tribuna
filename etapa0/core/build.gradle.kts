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

// Os testes do align leem os textos de teste de etapa0/textos/ (spec 0002; CLAUDE.md "Textos de teste").
val textosDeTeste = rootProject.layout.projectDirectory.dir("textos")

tasks.named<Test>("test") {
    inputs
        .dir(textosDeTeste)
        .withPropertyName("textosDeTeste")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("tribuna.textos", textosDeTeste.asFile.absolutePath)
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
