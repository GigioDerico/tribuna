package io.github.gigioderico.tribuna.etapa0.align

import java.io.File

/** Textos de teste de etapa0/textos/ (CLAUDE.md: só eles; nunca discurso pessoal). */
object TestTexts {
    val names = listOf("T01", "T02", "T03", "T04", "T05", "T06", "T07", "T08", "T09", "T10", "L30")

    private val dir: File by lazy {
        File(System.getProperty("tribuna.textos") ?: error("propriedade tribuna.textos ausente: rode pelo Gradle"))
    }

    fun read(name: String): String = File(dir, "$name.txt").readText(Charsets.UTF_8)

    fun tokens(name: String): List<Token> = Tokenizer.tokenize(read(name))
}
