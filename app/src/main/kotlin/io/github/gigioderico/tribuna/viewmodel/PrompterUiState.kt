package io.github.gigioderico.tribuna.viewmodel

import io.github.gigioderico.tribuna.align.Token
import io.github.gigioderico.tribuna.data.ScrollMode

enum class TimeWarning {
    None,
    FiveMin,
    OneMin,
}

sealed interface ReadingPhase {
    data object Idle : ReadingPhase

    data class Countdown(
        val secondsLeft: Int,
    ) : ReadingPhase

    data object Reading : ReadingPhase

    data object Paused : ReadingPhase

    data class Finished(
        val totalMs: Long,
    ) : ReadingPhase
}

data class PrompterUiState(
    val text: String = "",
    val tokens: List<Token> = emptyList(),
    val cursor: Int = 0,
    val phase: ReadingPhase = ReadingPhase.Idle,
    val mode: ScrollMode = ScrollMode.Voice,
    val elapsedMs: Long = 0L,
    val targetMs: Long? = null,
    val warning: TimeWarning = TimeWarning.None,
    val micLevel: Float = 0f,
    val micAvailable: Boolean = false,
)
