package io.github.gigioderico.tribuna.align

data class AlignParams(
    val k: Int,
    val window: Int,
    val shortThreshold: Double,
    val longThreshold: Double,
)
