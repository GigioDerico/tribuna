package io.github.gigioderico.tribuna.etapa0.ui

/**
 * Sincroniza o campo de texto da tela com o estado do controlador, que atualiza o texto de forma assíncrona
 * (OPR-029): o campo mostra sempre o que a pessoa digitou, e só um texto que não veio do campo (um chip de
 * texto de teste, por exemplo) o substitui. Ecos atrasados do que foi digitado são ignorados.
 * Vive só em `remember`, nunca em `rememberSaveable` (CLAUDE.md, regra 2).
 */
class FieldSync(
    initial: String,
) {
    var value: String = initial
        private set

    private val pending = ArrayDeque<String>()

    /** A pessoa digitou `text`: o campo mostra o texto na hora e o eco dele passa a ser esperado. */
    fun typed(text: String): String {
        value = text
        pending.addLast(text)
        return text
    }

    /** O estado do controlador passou a ter `text`. Devolve `true` se o campo deve passar a mostrá-lo. */
    fun received(text: String): Boolean {
        // O primeiro igual na fila é o eco mais antigo; o `StateFlow` pode pular ecos intermediários.
        val echo = pending.indexOf(text)
        if (echo >= 0) {
            repeat(echo + 1) { pending.removeFirst() }
            return false
        }
        if (text == value) return false
        pending.clear()
        value = text
        return true
    }
}
