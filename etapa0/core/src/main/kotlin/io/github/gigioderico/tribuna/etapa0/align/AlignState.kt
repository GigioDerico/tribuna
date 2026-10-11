package io.github.gigioderico.tribuna.etapa0.align

data class AlignState(
    val cursor: Int,
    val highWater: Int, // maior posição confirmada desde o último reposicionamento
    val missStreak: Int, // resultados seguidos sem avanço, para a recuperação
    val lastMove: Move,
    val score: Double,
)
