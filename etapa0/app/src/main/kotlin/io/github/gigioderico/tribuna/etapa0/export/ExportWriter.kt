package io.github.gigioderico.tribuna.etapa0.export

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Onde a exportação é gravada (costura para a JVM). */
fun interface ExportSink {
    /** Grava `<nome>.<sufixo>` para cada arquivo e devolve os nomes gravados. */
    fun write(
        name: String,
        files: Map<String, String>,
    ): List<String>
}

/** Nome da exportação: o ID do texto de teste e um sufixo opcional `[a-z0-9_]` (nada do discurso no nome; Q6). */
object ExportName {
    fun isValid(
        name: String,
        textId: String,
    ): Boolean = Regex("^" + Regex.escape(textId) + "(_[a-z0-9]+)*$").matches(name)
}

/**
 * Grava a exportação no diretório externo do app (sem permissão; `adb pull`), só arquivos de texto da gravação
 * de um texto de teste (spec 0003, D7, D9). Única escrita de arquivo do app além da cópia do modelo; nunca
 * escreve áudio (CLAUDE.md, regra 3).
 */
class ExportWriter(
    private val directory: () -> File?,
) : ExportSink {
    override fun write(
        name: String,
        files: Map<String, String>,
    ): List<String> {
        val dir = directory() ?: throw IOException("armazenamento externo do app indisponível")
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("não foi possível criar ${dir.name}")
        return files.map { (suffix, content) ->
            val file = File(dir, "$name.$suffix")
            FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            file.name
        }
    }
}
