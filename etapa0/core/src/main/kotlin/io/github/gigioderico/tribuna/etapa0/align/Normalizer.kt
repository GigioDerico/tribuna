package io.github.gigioderico.tribuna.etapa0.align

import java.util.Locale
import java.text.Normalizer as JdkNormalizer

/**
 * Minúsculas, sem acentos e sem pontuação, igual para roteiro e fala (TRD Motor de voz §Normalização e tokens).
 * Hífen e apóstrofo entre letras unem as partes ("bom-dia" → "bomdia", "d'água" → "dagua"; D18).
 * Idempotente: normalize(normalize(x)) == normalize(x).
 */
object Normalizer {
    private val combiningMarks = Regex("\\p{Mn}+")
    private val joiners = Regex("(?<=[\\p{L}\\p{N}])[-'’](?=[\\p{L}\\p{N}])")
    private val nonWord = Regex("[^\\p{L}\\p{N}\\s]+")
    private val spaces = Regex("\\s+")

    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val withoutAccents = JdkNormalizer.normalize(lower, JdkNormalizer.Form.NFD).replace(combiningMarks, "")
        return withoutAccents
            .replace(joiners, "")
            .replace(nonWord, " ")
            .replace(spaces, " ")
            .trim()
    }

    fun words(text: String): List<String> = normalize(text).split(' ').filter { it.isNotEmpty() }
}
