plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
    id("tribuna.android.verifications")
}

android {
    namespace = "io.github.gigioderico.tribuna.etapa0"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "io.github.gigioderico.tribuna.etapa0"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode =
            libs.versions.versionCode
                .get()
                .toInt()
        versionName = libs.versions.versionName.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    }
}

ktlint {
    version.set(libs.versions.ktlint.get())
}

dependencies {
    implementation(project(":core"))
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
