package io.github.gigioderico.tribuna.etapa0.export

import io.github.gigioderico.tribuna.etapa0.align.PartialReconciler
import java.util.Locale

/**
 * Conteúdo dos cinco arquivos da exportação (spec 0003, D7, D11; TESTES-ETAPA0). Consumidores: `metricas.py`
 * (trajetória e eventos), calibração do limiar na 0005 (nível), golden da 0006 (hipóteses) e relatório (meta).
 */
object ExportFiles {
    const val HYPOTHESES_HEADER = "t_ms,session,kind,words,accepted"
    const val LEVEL_HEADER = "t_ms,rms"
    const val TRAJECTORY_HEADER = "t_ms,cursor,move,score"
    const val EVENTS_HEADER = "t_ms,evento,detalhe"

    /** Chaves do `meta.txt`, na ordem (D11). */
    val META_KEYS =
        listOf(
            "app_version",
            "vosk_android_version",
            "model_id",
            "text_id",
            "preset",
            "vocabulary",
            "recognizer_strategy",
            "audio_source",
            "wav_name",
            "level_threshold",
            "animation_ms",
            "install_ms",
            "prepare_ms",
            "device_model",
            "android_version",
        )

    /** Sufixo do arquivo → conteúdo. O nome final é `<nome>.<sufixo>`. */
    fun build(recording: Recording): Map<String, String> =
        linkedMapOf(
            "hypotheses.csv" to hypotheses(recording.hypotheses),
            "level.csv" to level(recording.levels),
            "trajectory.csv" to trajectory(recording.trajectory),
            "events.csv" to events(recording.events),
            "meta.txt" to meta(recording.meta),
        )

    fun hypotheses(rows: List<HypothesisRow>): String =
        lines(HYPOTHESES_HEADER, rows) { row ->
            "${row.tMs},${row.session ?: ""},${row.kind},${quote(row.words.joinToString(" "))},${if (row.accepted) 1 else 0}"
        }

    fun level(rows: List<LevelRow>): String = lines(LEVEL_HEADER, rows) { "${it.tMs},${String.format(Locale.ROOT, "%.6f", it.rms)}" }

    fun trajectory(rows: List<TrajectoryRow>): String =
        lines(TRAJECTORY_HEADER, rows) { "${it.tMs},${it.cursor},${it.move.name},${it.score}" }

    fun events(rows: List<EventRow>): String = lines(EVENTS_HEADER, rows) { "${it.tMs},${it.event},${it.detail}" }

    fun meta(values: Map<String, String>): String =
        META_KEYS.joinToString(separator = "\n", postfix = "\n") { "$it=${values[it].orEmpty()}" }

    /** Lê `hypotheses.csv` de volta (golden da 0006; ida e volta de D7). */
    fun readHypotheses(csv: String): List<HypothesisRow> {
        val lines = csv.lines().filter { it.isNotEmpty() }
        require(lines.firstOrNull() == HYPOTHESES_HEADER) { "cabeçalho inesperado: ${lines.firstOrNull()}" }
        return lines.drop(1).map(::parseHypothesis)
    }

    /**
     * Reprocessa as linhas com `accepted = 1` pelo [PartialReconciler], zerando a frase a cada sessão nova,
     * como a tela faz ao vivo; devolve as palavras entregues, na ordem.
     */
    fun replayAccepted(rows: List<HypothesisRow>): List<String> {
        val reconciler = PartialReconciler()
        val out = mutableListOf<String>()
        var session: Long? = null
        for (row in rows) {
            if (!row.accepted || row.kind == Recording.KIND_ERROR) continue
            if (row.session != session) {
                reconciler.reset()
                session = row.session
            }
            out +=
                when (row.kind) {
                    Recording.KIND_PARTIAL -> reconciler.onPartial(row.words)
                    else -> reconciler.onFinal(row.words)
                }
        }
        return out
    }

    private fun <T> lines(
        header: String,
        rows: List<T>,
        format: (T) -> String,
    ): String = (listOf(header) + rows.map(format)).joinToString(separator = "\n", postfix = "\n")

    private fun quote(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    private fun parseHypothesis(line: String): HypothesisRow {
        val firstQuote = line.indexOf('"')
        val lastQuote = line.lastIndexOf('"')
        require(firstQuote in 0 until lastQuote) { "linha sem palavras entre aspas: $line" }
        val head = line.substring(0, firstQuote).split(',')
        val tail = line.substring(lastQuote + 1).removePrefix(",")
        val words = line.substring(firstQuote + 1, lastQuote).replace("\"\"", "\"")
        return HypothesisRow(
            tMs = head[0].toLong(),
            session = head[1].takeIf { it.isNotEmpty() }?.toLong(),
            kind = head[2],
            words = words.split(' ').filter { it.isNotEmpty() },
            accepted = tail == "1",
        )
    }
}
