package io.github.gigioderico.tribuna.etapa0.speech

/**
 * JSON do Vosk ↔ palavras (TRD Motor de voz §Captura e sessões; spec 0003, C6).
 * Formatos da documentação do `org.vosk.Recognizer` 0.3.75: `{"partial" : "..."}` e `{"text" : "..."}`.
 * A confirmação contra o JSON real é da 0005 (Q4). Sem dependência de JSON: os testes JVM não têm `org.json`.
 */
object VoskJson {
    private val partialKey = Regex("\"partial\"\\s*:\\s*\"")
    private val textKey = Regex("\"text\"\\s*:\\s*\"")

    /** Palavras do parcial ou do resultado; JSON vazio, sem a chave ou com texto vazio = lista vazia. */
    fun words(json: String): List<String> {
        val text = stringAfter(json, partialKey) ?: stringAfter(json, textKey) ?: return emptyList()
        return text.split(' ').filter { it.isNotEmpty() }
    }

    /** Lista de palavras → gramática do Vosk: um array JSON com uma frase por palavra. */
    fun grammar(words: List<String>): String = words.joinToString(separator = ",", prefix = "[", postfix = "]") { quote(it) }

    private fun stringAfter(
        json: String,
        key: Regex,
    ): String? {
        val match = key.find(json) ?: return null
        val out = StringBuilder()
        var i = match.range.last + 1
        while (i < json.length) {
            val c = json[i]
            when {
                c == '"' -> {
                    return out.toString()
                }

                c == '\\' && i + 1 < json.length -> {
                    val next = json[i + 1]
                    when (next) {
                        'n' -> {
                            out.append('\n')
                        }

                        't' -> {
                            out.append('\t')
                        }

                        'r' -> {
                            out.append('\r')
                        }

                        'b' -> {
                            out.append('\b')
                        }

                        'f' -> {
                            out.append('\u000C')
                        }

                        'u' -> {
                            if (i + 5 >= json.length) return null
                            out.append(json.substring(i + 2, i + 6).toInt(16).toChar())
                            i += 4
                        }

                        else -> {
                            out.append(next)
                        }
                    }
                    i += 2
                    continue
                }

                else -> {
                    out.append(c)
                }
            }
            i++
        }
        return null
    }

    private fun quote(word: String): String =
        buildString {
            append('"')
            for (c in word) {
                when {
                    c == '"' -> append("\\\"")
                    c == '\\' -> append("\\\\")
                    c < ' ' -> append("\\u%04x".format(c.code))
                    else -> append(c)
                }
            }
            append('"')
        }
}
