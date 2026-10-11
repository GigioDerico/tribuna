plugins {
    `kotlin-dsl`
}

group = "io.github.gigioderico.tribuna.buildlogic"

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    compileOnly(libs.android.gradle.api)
    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidVerifications") {
            id = "tribuna.android.verifications"
            implementationClass = "io.github.gigioderico.tribuna.buildlogic.AndroidVerificationsPlugin"
        }
    }
}
