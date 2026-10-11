package io.github.gigioderico.tribuna.etapa0.align

/**
 * Separa o que aparece na tela das formas faladas aceitas (TRD §Contratos, Token).
 * Destaque, toque e progresso usam o token; o alinhamento usa as formas faladas.
 */
data class Token(
    val index: Int,
    val display: IntRange, // caracteres no texto original
    val spoken: List<List<String>>, // formas faladas aceitas, cada uma com 1+ palavras
    val weight: Float, // menor para palavras comuns
    val sentenceStart: Boolean, // para Volume − e frase anterior
)
