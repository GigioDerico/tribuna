package io.github.gigioderico.tribuna.etapa0.speech

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/** Modelo pronto no armazenamento interno. */
data class InstalledModel(
    val dir: File,
    /** Primeira linha do `uuid` (D12): nome do modelo e SHA-256 do zip baixado. */
    val id: String,
    val copied: Boolean,
)

/** "Mesma versão → não copia; diferente ou ausente → copia" (TRD §Instalação do modelo 2; spec 0003, D12, C10). */
object ModelVersion {
    fun needsCopy(
        sourceUuid: String,
        targetUuid: String?,
    ): Boolean = targetUuid == null || targetUuid.trim() != sourceUuid.trim()
}

/**
 * Copia `assets/model-pt` para o armazenamento interno (`filesDir/model-pt`) na primeira execução
 * (TRD Motor de voz §Instalação do modelo 1–3; spec 0003, C10). Usa o mesmo arquivo `uuid` do
 * `StorageService` do Vosk, mas com cópia própria: o `StorageService.sync` do vosk-android 0.3.75 copia para
 * `getExternalFilesDir` e só avisa no fim (OPR-021).
 *
 * A cópia vai para `model-pt.tmp`, com o `uuid` por último, e só então troca de nome; uma cópia interrompida
 * nunca parece completa. Falha: [ModelLoadException] com `ModelMissing` (asset ausente) ou `ModelCorrupt`
 * (cópia falhou: sem espaço, arquivo corrompido), e a tela oferece "Tentar de novo".
 */
class ModelInstaller(
    private val assets: AssetTree,
    private val filesDir: File,
    private val assetDir: String = MODEL_DIR,
) {
    fun install(): InstalledModel {
        val sourceUuid =
            try {
                assets.open("$assetDir/$UUID").use(::firstLine)
            } catch (e: IOException) {
                throw ModelLoadException(EngineError.ModelMissing, "modelo ausente nos assets", e)
            } ?: throw ModelLoadException(EngineError.ModelMissing, "uuid do modelo vazio")
        val target = File(filesDir, assetDir)
        val targetUuidFile = File(target, UUID)
        val targetUuid = if (targetUuidFile.isFile) targetUuidFile.inputStream().use(::firstLine) else null
        if (!ModelVersion.needsCopy(sourceUuid, targetUuid)) return InstalledModel(target, sourceUuid.trim(), copied = false)
        val temp = File(filesDir, "$assetDir.tmp")
        try {
            temp.deleteRecursively()
            copyTree(assetDir, temp)
            copyFile("$assetDir/$UUID", File(temp, UUID))
            target.deleteRecursively()
            if (!temp.renameTo(target)) throw IOException("não foi possível mover ${temp.name} para ${target.name}")
        } catch (e: IOException) {
            temp.deleteRecursively()
            throw ModelLoadException(EngineError.ModelCorrupt, "falha na cópia do modelo", e)
        }
        return InstalledModel(target, sourceUuid.trim(), copied = true)
    }

    private fun copyTree(
        path: String,
        destination: File,
    ) {
        val children = assets.list(path)
        if (children.isEmpty()) {
            if (path != "$assetDir/$UUID") copyFile(path, File(destination, path.removePrefix("$assetDir/")))
            return
        }
        for (child in children) copyTree("$path/$child", destination)
    }

    private fun copyFile(
        path: String,
        destination: File,
    ) {
        val parent = destination.parentFile
        if (parent != null && !parent.isDirectory && !parent.mkdirs()) throw IOException("não foi possível criar ${parent.name}")
        assets.open(path).use { input -> FileOutputStream(destination).use { output -> input.copyTo(output) } }
    }

    private fun firstLine(input: InputStream): String? =
        input
            .bufferedReader(Charsets.UTF_8)
            .readLine()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    companion object {
        const val MODEL_DIR = "model-pt"
        const val UUID = "uuid"
    }
}
