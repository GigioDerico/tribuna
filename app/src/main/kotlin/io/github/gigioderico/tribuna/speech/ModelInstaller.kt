package io.github.gigioderico.tribuna.speech

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

/** "Mesma versÃƒÂ£o Ã¢â€ â€™ nÃƒÂ£o copia; diferente ou ausente Ã¢â€ â€™ copia" (TRD Ã‚Â§InstalaÃƒÂ§ÃƒÂ£o do modelo 2; spec 0003, D12, C10). */
object ModelVersion {
    fun needsCopy(
        sourceUuid: String,
        targetUuid: String?,
    ): Boolean = targetUuid == null || targetUuid.trim() != sourceUuid.trim()
}

/**
 * Copia `assets/model-pt` para o armazenamento interno (`filesDir/model-pt`) na primeira execuÃƒÂ§ÃƒÂ£o
 * (TRD Motor de voz Ã‚Â§InstalaÃƒÂ§ÃƒÂ£o do modelo 1Ã¢â‚¬â€œ3; spec 0003, C10). Usa o mesmo arquivo `uuid` do
 * `StorageService` do Vosk, mas com cÃƒÂ³pia prÃƒÂ³pria: o `StorageService.sync` do vosk-android 0.3.75 copia para
 * `getExternalFilesDir` e sÃƒÂ³ avisa no fim (OPR-021).
 *
 * A cÃƒÂ³pia vai para `model-pt.tmp`, com o `uuid` por ÃƒÂºltimo, e sÃƒÂ³ entÃƒÂ£o troca de nome; uma cÃƒÂ³pia interrompida
 * nunca parece completa. Falha: [ModelLoadException] com `ModelMissing` (asset ausente) ou `ModelCorrupt`
 * (cÃƒÂ³pia falhou: sem espaÃƒÂ§o, arquivo corrompido), e a tela oferece "Tentar de novo".
 */
class ModelInstaller(
    private val assets: AssetTree,
    private val filesDir: File,
    private val assetDir: String = MODEL_DIR,
) {
    fun install(): InstalledModel =
        synchronized(installLock) {
            val sourceUuid =
                try {
                    assets.open("$assetDir/$UUID").use(::firstLine)
                } catch (e: IOException) {
                    throw ModelLoadException(EngineError.ModelMissing, "modelo ausente nos assets", e)
                } ?: throw ModelLoadException(EngineError.ModelMissing, "uuid do modelo vazio")
            val target = File(filesDir, assetDir)
            val targetUuidFile = File(target, UUID)
            val targetUuid = if (targetUuidFile.isFile) targetUuidFile.inputStream().use(::firstLine) else null
            if (!ModelVersion.needsCopy(sourceUuid, targetUuid) && isTreeComplete(assetDir, target)) {
                return InstalledModel(target, sourceUuid.trim(), copied = false)
            }
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

    private fun isTreeComplete(
        path: String,
        destination: File,
    ): Boolean {
        val children = assets.list(path)
        if (children.isEmpty()) {
            val targetFile = File(destination, path.removePrefix("$assetDir/"))
            return targetFile.isFile && targetFile.length() > 0L
        }
        return children.all { child -> isTreeComplete("$path/$child", destination) }
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
        if (parent != null && !parent.isDirectory && !parent.mkdirs()) throw IOException("nÃƒÂ£o foi possÃƒÂ­vel criar ${parent.name}")
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
        private val installLock = Any()

        fun isInstalled(context: android.content.Context): Boolean {
            val target = File(context.filesDir, MODEL_DIR)
            val uuidFile = File(target, UUID)
            return uuidFile.isFile
        }

        fun installIfNeeded(context: android.content.Context): Result<InstalledModel> =
            runCatching {
                ModelInstaller(AndroidAssetTree(context.assets), context.filesDir).install()
            }
    }
}
