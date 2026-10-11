package io.github.gigioderico.tribuna.etapa0.align

data class AlignParams(
    val k: Int,
    val window: Int,
    val shortThreshold: Double,
    val longThreshold: Double,
)
