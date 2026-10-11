package io.github.gigioderico.tribuna.etapa0

import java.io.File

/** Textos de etapa0/textos/ e assets reais do modelo, pelo caminho que o Gradle passa (CLAUDE.md: só textos de teste). */
object TestFiles {
    private val textos: File by lazy {
        File(System.getProperty("tribuna.textos") ?: error("propriedade tribuna.textos ausente: rode pelo Gradle"))
    }

    /** Diretório `app/src/main/assets/model-pt` (LFS; baixado por scripts/baixar-modelo.sh). */
    val modelDir: File by lazy {
        File(System.getProperty("tribuna.modelo") ?: error("propriedade tribuna.modelo ausente: rode pelo Gradle"))
    }

    val textIds = listOf("L30", "T01", "T02", "T03", "T04", "T05", "T06", "T07", "T08", "T09", "T10")

    fun text(id: String): String = File(textos, "$id.txt").readText(Charsets.UTF_8)

    /** Palavras como o Vosk as devolve: minúsculas, com acento, sem pontuação (só o formato; o JSON real é da 0005). */
    fun spokenLike(id: String): List<String> =
        text(id)
            .lines()
            .filterNot { it.trim().startsWith("//") }
            .flatMap { line -> line.lowercase().split(Regex("[^\\p{L}\\p{N}-]+")) }
            .filter { it.isNotEmpty() }
}
