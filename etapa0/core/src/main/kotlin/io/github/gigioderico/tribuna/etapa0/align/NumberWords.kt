package io.github.gigioderico.tribuna.etapa0.align

/** Números por extenso em português, de 0 a 999.999 (TRD §Normalização e tokens; spec 0002, D14 e D15). */
internal object NumberWords {
    const val MAX = 999_999

    private val units =
        listOf(
            "zero",
            "um",
            "dois",
            "tres",
            "quatro",
            "cinco",
            "seis",
            "sete",
            "oito",
            "nove",
            "dez",
            "onze",
            "doze",
            "treze",
            "catorze",
            "quinze",
            "dezesseis",
            "dezessete",
            "dezoito",
            "dezenove",
        )
    private val tens = listOf("", "", "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa")
    private val hundreds =
        listOf(
            "",
            "cento",
            "duzentos",
            "trezentos",
            "quatrocentos",
            "quinhentos",
            "seiscentos",
            "setecentos",
            "oitocentos",
            "novecentos",
        )

    /** Forma masculina usual, sem alternativas: 1914 → [mil, novecentos, e, catorze]. */
    fun masculine(n: Int): List<String> {
        require(n in 0..MAX) { "número fora da faixa de D14" } // sem o valor: nada do roteiro em mensagem (Q6)
        if (n < 1000) return below1000(n)
        val thousands = n / 1000
        val rest = n % 1000
        val head = if (thousands == 1) listOf("mil") else below1000(thousands) + "mil"
        return when {
            rest == 0 -> head
            rest < 100 || rest % 100 == 0 -> head + "e" + below1000(rest)
            else -> head + below1000(rest)
        }
    }

    /** Grafias alternativas viram formas alternativas: catorze/quatorze (TRD §Normalização). */
    fun spellingVariants(form: List<String>): List<List<String>> =
        if ("catorze" in form) listOf(form, form.map { if (it == "catorze") "quatorze" else it }) else listOf(form)

    /** Forma feminina (D15): um → uma, dois → duas, -entos → -entas. */
    fun feminine(form: List<String>): List<String> =
        form.map {
            when {
                it == "um" -> "uma"
                it == "dois" -> "duas"
                it.endsWith("entos") -> it.removeSuffix("entos") + "entas"
                else -> it
            }
        }

    /** Número solto: masculina, feminina quando difere, cada uma com as grafias alternativas (D14, D15). */
    fun cardinalForms(n: Int): List<List<String>> {
        val masculine = masculine(n)
        val feminine = feminine(masculine)
        val bases = if (feminine == masculine) listOf(masculine) else listOf(masculine, feminine)
        return bases.flatMap(::spellingVariants).distinct()
    }

    /** Referências, decimais e porcentagens: só masculina, com as grafias alternativas. */
    fun masculineForms(n: Int): List<List<String>> = spellingVariants(masculine(n))

    private fun below1000(n: Int): List<String> =
        when {
            n < 20 -> listOf(units[n])
            n < 100 -> listOf(tens[n / 10]) + if (n % 10 == 0) emptyList() else listOf("e", units[n % 10])
            n == 100 -> listOf("cem")
            else -> listOf(hundreds[n / 100]) + if (n % 100 == 0) emptyList() else listOf("e") + below1000(n % 100)
        }
}
