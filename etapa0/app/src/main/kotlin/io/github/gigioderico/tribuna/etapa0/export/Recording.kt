package io.github.gigioderico.tribuna.etapa0.export

import io.github.gigioderico.tribuna.etapa0.align.Move
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEvent

/** Uma linha de `hypotheses.csv` (spec 0003, D7). `session` nulo só em erro da captura. */
data class HypothesisRow(
    val tMs: Long,
    val session: Long?,
    val kind: String,
    val words: List<String>,
    val accepted: Boolean,
)

data class LevelRow(
    val tMs: Long,
    val rms: Float,
)

data class TrajectoryRow(
    val tMs: Long,
    val cursor: Int,
    val move: Move,
    val score: Double,
)

data class EventRow(
    val tMs: Long,
    val event: String,
    val detail: String,
)

/**
 * Uma gravação em memória: só existe quando o texto atual é um texto de teste (D9), do `startCapture` até a
 * próxima leitura. `t_ms` = ms desde o início da gravação, por relógio monotônico (D6). Nada sai daqui sem
 * [ExportFiles] e o botão Exportar.
 */
class Recording(
    val textId: String,
    /** Ajustes e ambiente no início da gravação, para o `meta.txt` (D11, Q8). */
    val meta: Map<String, String>,
    private val originMs: Long,
) {
    val hypotheses = mutableListOf<HypothesisRow>()
    val levels = mutableListOf<LevelRow>()
    val trajectory = mutableListOf<TrajectoryRow>()
    val events = mutableListOf<EventRow>()

    fun elapsed(nowMs: Long): Long = nowMs - originMs

    fun hypothesis(
        nowMs: Long,
        event: SpeechEvent,
        accepted: Boolean,
    ) {
        val row =
            when (event) {
                is SpeechEvent.Partial -> HypothesisRow(elapsed(nowMs), event.session.value, KIND_PARTIAL, event.words, accepted)
                is SpeechEvent.Final -> HypothesisRow(elapsed(nowMs), event.session.value, KIND_FINAL, event.words, accepted)
                is SpeechEvent.Error -> HypothesisRow(elapsed(nowMs), event.session?.value, KIND_ERROR, listOf(event.cause.name), accepted)
                is SpeechEvent.Level -> return
            }
        hypotheses += row
    }

    fun level(
        nowMs: Long,
        rms: Float,
    ) {
        levels += LevelRow(elapsed(nowMs), rms)
    }

    fun trajectory(
        nowMs: Long,
        cursor: Int,
        move: Move,
        score: Double,
    ) {
        trajectory += TrajectoryRow(elapsed(nowMs), cursor, move, score)
    }

    fun event(
        nowMs: Long,
        event: String,
        detail: String = "",
    ) {
        events += EventRow(elapsed(nowMs), event, detail)
    }

    companion object {
        const val KIND_PARTIAL = "partial"
        const val KIND_FINAL = "final"
        const val KIND_ERROR = "error"
        const val PAUSE_TAP = "pausa_toque"
        const val RESUME_TAP = "retoma_toque"
        const val WORD_TAP = "toque_palavra"
    }
}
