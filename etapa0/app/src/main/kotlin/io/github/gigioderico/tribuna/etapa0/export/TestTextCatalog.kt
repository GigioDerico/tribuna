package io.github.gigioderico.tribuna.etapa0.export

import io.github.gigioderico.tribuna.etapa0.speech.AssetTree

/**
 * Textos de teste de `etapa0/textos/`, empacotados em `assets/textos/` (spec 0003, D9). A exportação só fica
 * habilitada quando o texto atual é idêntico a um deles: um discurso digitado nunca vira arquivo
 * (TRD §Privacidade nos testes; CLAUDE.md, regras 2 e 3).
 */
class TestTextCatalog(
    private val texts: Map<String, String>,
) {
    /** IDs em ordem (L30, T01…T10). */
    val ids: List<String> = texts.keys.sorted()

    fun text(id: String): String? = texts[id]

    /** ID do texto de teste idêntico a `text`, ou `null`. */
    fun identify(text: String): String? = texts.entries.firstOrNull { it.value == text }?.key

    companion object {
        const val ASSET_DIR = "textos"

        fun fromAssets(assets: AssetTree): TestTextCatalog =
            TestTextCatalog(
                assets
                    .list(ASSET_DIR)
                    .filter { it.endsWith(".txt") }
                    .associate { name ->
                        name.removeSuffix(".txt") to assets.open("$ASSET_DIR/$name").use { it.readBytes().toString(Charsets.UTF_8) }
                    },
            )
    }
}
