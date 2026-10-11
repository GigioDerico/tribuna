plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
        // Proveniência das exportações (spec 0003, D11).
        buildConfigField("String", "VOSK_VERSION", "\"${libs.versions.vosk.get()}\"")
        ndk {
            // As ABIs em que o vosk-android 0.3.75 traz libvosk.so; a JNA 5.19.1 traz também armeabi, mips e
            // mips64, onde o app não funcionaria sem o Vosk (impl_plan 0003, A7).
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            // TRD §Distribuição: R8 ligado com as regras do Vosk e da JNA; spec 0003 D19: assinado com a chave de debug.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    }
}

/** Copia etapa0/textos/ para assets/textos/ (spec 0003, D9: só textos de teste habilitam a exportação). */
abstract class TestTextsAssetsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val textos: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val dest = outputDir.get().dir("textos").asFile
        dest.deleteRecursively()
        dest.mkdirs()
        textos
            .get()
            .asFile
            .listFiles { file -> file.name.endsWith(".txt") }
            .orEmpty()
            .sortedBy { it.name }
            .forEach { it.copyTo(File(dest, it.name)) }
    }
}

val textosDeTeste = rootProject.layout.projectDirectory.dir("textos")

val testTextsAssets =
    tasks.register<TestTextsAssetsTask>("testTextsAssets") {
        textos.set(textosDeTeste)
    }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(testTextsAssets, TestTextsAssetsTask::outputDir)
    }
}

// Os testes JVM leem os textos de teste e os assets reais do modelo pelo caminho (spec 0003, D6, D8; OPR-020).
tasks.withType<Test>().configureEach {
    inputs
        .dir(textosDeTeste)
        .withPropertyName("textosDeTeste")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("tribuna.textos", textosDeTeste.asFile.absolutePath)
    systemProperty(
        "tribuna.modelo",
        layout.projectDirectory
            .dir("src/main/assets/model-pt")
            .asFile.absolutePath,
    )
}

ktlint {
    version.set(libs.versions.ktlint.get())
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.vosk.android)
    // TRD §Stack: JNA como AAR (o JAR traria as bibliotecas nativas de desktop).
    implementation(libs.jna) {
        artifact {
            type = "aar"
        }
    }
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Lista os AAR/JAR do classpath de release para scripts/varrer-registerreceiver.sh (CIA 0003, mitigação 3).
tasks.register("releaseClasspathFiles") {
    group = "verification"
    description = "Lista os arquivos do classpath de release (varredura de registerReceiver, CIA 0003)."
    val artifacts =
        configurations
            .named("releaseRuntimeClasspath")
            .map { it.incoming.artifacts }
    doLast {
        artifacts.get().artifacts.forEach {
            println("ART ${it.id.componentIdentifier.displayName.replace(' ', '_')} ${it.file}")
        }
    }
}
