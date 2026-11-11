package io.github.gigioderico.tribuna.align

/**
 * Presets de sensibilidade (PRD R4 e tabela do Algoritmo de alinhamento; spec 0002, D11).
 * Valores do PRD; a calibracao final e da 0005. O contrato AlignParams continua sem valor padrao.
 */
object AlignPresets {
    const val CONSERVADOR_NAME = "Conservador"
    const val NORMAL_NAME = "Normal"
    const val AGIL_NAME = "Ágil"

    val conservador = AlignParams(k = 4, window = 25, shortThreshold = 0.70, longThreshold = 0.90)
    val normal = AlignParams(k = 3, window = 40, shortThreshold = 0.60, longThreshold = 0.85)
    val agil = AlignParams(k = 2, window = 60, shortThreshold = 0.55, longThreshold = 0.80)

    fun getOrDefault(name: String): AlignParams =
        when (name.lowercase()) {
            "conservador" -> conservador
            "ágil", "agil" -> agil
            else -> normal
        }
}
