package io.github.gigioderico.tribuna.etapa0.align

/**
 * Roteiro → tokens (TRD Motor de voz §Normalização e tokens; Contratos §Token).
 * Recorte e formas: spec 0002, D14–D20. As formas faladas saem já normalizadas.
 */
object Tokenizer {
    private val rawWord = Regex("\\S+")
    private val reference = Regex("^(\\d+):(\\d+)(?:[-–](\\d+))?$")
    private val referenceStart = Regex("^\\d+:\\d+")
    private val integer = Regex("^\\d+$")
    private val number = Regex("^(\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,(\\d+))?(%)?$")
    private val bookNumber = Regex("^[123]$")
    private val intervalWords = setOf("a", "-", "–")
    private const val CLOSING = "\"'”’)»]"
    private const val SENTENCE_END = ".!?…"
    private val ordinals =
        mapOf(
            1 to listOf("primeira", "primeiro", "um"),
            2 to listOf("segunda", "segundo", "dois"),
            3 to listOf("terceira", "terceiro", "tres"),
        )

    fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var pendingStart = true
        var lineStart = 0
        for (line in text.split('\n')) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                pendingStart = true // D20: linha em branco ou nota abre frase; nota não gera token (T5)
            } else {
                val raws = rawWord.findAll(line).map { Raw(it.value, lineStart + it.range.first) }.toList()
                var i = 0
                while (i < raws.size) {
                    val built = build(raws, i)
                    if (built == null) {
                        if (endsSentence(raws[i].text)) pendingStart = true
                        i++
                        continue
                    }
                    tokens +=
                        Token(
                            index = tokens.size,
                            display = built.display,
                            spoken = built.spoken,
                            weight = built.weight,
                            sentenceStart = pendingStart,
                        )
                    pendingStart = endsSentence(raws[built.lastRaw].text)
                    i = built.lastRaw + 1
                }
            }
            lineStart += line.length + 1
        }
        return tokens
    }

    private class Raw(
        val text: String,
        val start: Int,
    ) {
        val coreStart: Int = text.indexOfFirst { it.isLetterOrDigit() }.coerceAtLeast(0)
        val coreEnd: Int = text.indexOfLast { it.isLetterOrDigit() || it == '%' } + 1
        val hasCore: Boolean get() = text.any { it.isLetterOrDigit() }
        val core: String get() = if (hasCore) text.substring(coreStart, coreEnd) else ""
        val absCoreStart: Int get() = start + coreStart
        val absCoreLast: Int get() = start + coreEnd - 1
    }

    private class Built(
        val display: IntRange,
        val spoken: List<List<String>>,
        val weight: Float,
        val lastRaw: Int,
    )

    private fun build(
        raws: List<Raw>,
        i: Int,
    ): Built? {
        if (!raws[i].hasCore) return null // pontuação solta não gera token (D19)
        return book(raws, i) ?: referenceToken(raws, i) ?: numberToken(raws[i], i) ?: word(raws[i], i)
    }

    /** D17: "1 Coríntios 13:4-7" → o livro com número é um token só. */
    private fun book(
        raws: List<Raw>,
        i: Int,
    ): Built? {
        val raw = raws[i]
        if (!bookNumber.matches(raw.text)) return null
        val name = raws.getOrNull(i + 1) ?: return null
        val ref = raws.getOrNull(i + 2) ?: return null
        if (!name.hasCore || !name.core.first().isUpperCase() || !referenceStart.containsMatchIn(ref.core)) return null
        val nameWords = Normalizer.words(name.core)
        val spoken = ordinals.getValue(raw.text.toInt()).map { listOf(it) + nameWords }
        return Built(raw.absCoreStart..name.absCoreLast, spoken, 1f, i + 1)
    }

    /** D16: referência simples, intervalo (-, – ou "a") ou lista, sempre um token só com o capítulo. */
    private fun referenceToken(
        raws: List<Raw>,
        i: Int,
    ): Built? {
        val raw = raws[i]
        val match = reference.matchEntire(raw.core) ?: return null
        val chapter = match.groupValues[1].toIntOrNull() ?: return null
        val verses = mutableListOf(match.groupValues[2].toIntOrNull() ?: return null)
        var last = i
        var interval = false
        val next = raws.getOrNull(i + 1)
        val afterNext = raws.getOrNull(i + 2)
        if (match.groupValues[3].isNotEmpty()) {
            verses += match.groupValues[3].toIntOrNull() ?: return null
            interval = true
        } else if (next != null && next.text in intervalWords && afterNext != null && integer.matches(afterNext.core)) {
            verses += afterNext.core.toIntOrNull() ?: return null
            interval = true
            last = i + 2
        } else {
            while (raws[last].text.endsWith(",")) {
                val item = raws.getOrNull(last + 1) ?: break
                if (!integer.matches(item.core)) break
                verses += item.core.toIntOrNull() ?: return null
                last++
            }
        }
        if (chapter > NumberWords.MAX || verses.any { it > NumberWords.MAX }) return null
        return Built(raw.absCoreStart..raws[last].absCoreLast, referenceForms(chapter, verses, interval), 1f, last)
    }

    private fun referenceForms(
        chapter: Int,
        verses: List<Int>,
        interval: Boolean,
    ): List<List<String>> {
        val options = listOf(NumberWords.masculineForms(chapter)) + verses.map { NumberWords.masculineForms(it) }
        val combinations =
            options.fold(listOf(emptyList<List<String>>())) { acc, alternatives ->
                acc.flatMap { prefix -> alternatives.map { prefix + listOf(it) } }
            }
        return combinations
            .flatMap { combination ->
                val c = combination.first()
                val v = combination.drop(1)
                when {
                    interval -> {
                        listOf(
                            c + v[0] + "a" + v[1],
                            c + "de" + v[0] + "a" + v[1],
                            listOf("capitulo") + c + "versiculos" + v[0] + "a" + v[1],
                        )
                    }

                    v.size == 1 -> {
                        listOf(
                            c + v[0],
                            c + "e" + v[0],
                            listOf("capitulo") + c + "versiculo" + v[0],
                        )
                    }

                    else -> {
                        val list = v.dropLast(1).flatten() + "e" + v.last()
                        listOf(
                            c + list,
                            c + "versiculos" + list,
                            listOf("capitulo") + c + "versiculos" + list,
                        )
                    }
                }
            }.distinct()
    }

    /** D14: inteiros até 999.999 (com ou sem ponto de milhar), decimais com vírgula e porcentagem. */
    private fun numberToken(
        raw: Raw,
        i: Int,
    ): Built? {
        val match = number.matchEntire(raw.core) ?: return null
        val value =
            match.groupValues[1]
                .replace(".", "")
                .toIntOrNull()
                ?.takeIf { it <= NumberWords.MAX } ?: return null
        val decimals = match.groupValues[2]
        val percent = match.groupValues[3].isNotEmpty()
        val spoken =
            if (decimals.isEmpty() && !percent) {
                NumberWords.cardinalForms(value)
            } else {
                val whole = NumberWords.masculineForms(value)
                val withDecimals =
                    if (decimals.isEmpty()) {
                        whole
                    } else {
                        val fraction = decimalForms(decimals)
                        whole.flatMap { w -> fraction.map { f -> w + "virgula" + f } }
                    }
                if (percent) withDecimals.map { it + listOf("por", "cento") } else withDecimals
            }
        return Built(raw.absCoreStart..raw.absCoreLast, spoken, 1f, i)
    }

    private fun decimalForms(digits: String): List<List<String>> {
        val zeros = List(digits.takeWhile { it == '0' }.length) { "zero" }
        val rest = digits.drop(zeros.size)
        if (rest.isEmpty()) return listOf(zeros)
        val value = rest.toIntOrNull()?.takeIf { it <= NumberWords.MAX } ?: return listOf(digits.map { it.toString() })
        return NumberWords.masculineForms(value).map { zeros + it }
    }

    /** Palavra comum ou hifenizada (D18, D19); peso menor para as palavras comuns (D10). */
    private fun word(
        raw: Raw,
        i: Int,
    ): Built? {
        val core = raw.core.trimEnd('%')
        val joined = Normalizer.words(core)
        if (joined.isEmpty()) return null
        val parts = core.split('-').flatMap { Normalizer.words(it) }
        val spoken = if (core.contains('-') && parts.size > 1 && parts != joined) listOf(parts, joined) else listOf(joined)
        val common = spoken.size == 1 && joined.size == 1 && joined[0] in AlignConstants.COMMON_WORDS
        val weight = if (common) AlignConstants.COMMON_WORD_WEIGHT else 1f
        return Built(raw.absCoreStart..(raw.absCoreStart + core.length - 1), spoken, weight, i)
    }

    /** D20: termina frase o texto cru que acaba em . ! ? ou …, ignorando aspas e parênteses de fechamento. */
    private fun endsSentence(raw: String): Boolean {
        val trimmed = raw.trimEnd { it in CLOSING }
        return trimmed.isNotEmpty() && trimmed.last() in SENTENCE_END
    }
}
