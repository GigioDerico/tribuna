plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
}

tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Roda os testes do build-logic (manifesto e 16 KB) junto com os unitários dos módulos."
    dependsOn(gradle.includedBuild("build-logic").task(":test"))
}
