"""Roteiro -> tokens, em Python, repetindo o núcleo Kotlin da etapa0 (spec 0004, D3 e D4).

Espelha, regra por regra:
- Normalizer.kt (normalizar, palavras);
- NumberWords.kt (masculina, formas_cardinais, formas_masculinas);
- Tokenizer.kt (tokenizar), só índice, intervalo exibido e formas faladas (peso e início de frase
  ficam de fora: as ferramentas não usam).

A paridade é provada pelas referências geradas do Kotlin em
etapa0/core/src/test/resources/referencia/ (teste JVM ReferenciaFormasTest e testes/test_roteiro.py).
As regex usam [0-9] e o espaço ASCII de propósito: no Java, \\d e \\s são só ASCII.

Também guarda o catálogo dos textos de teste (CLAUDE.md "Textos de teste"; spec 0004, D8).
"""

import itertools
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path

FERRAMENTAS = Path(__file__).resolve().parent
ETAPA0 = FERRAMENTAS.parent
TEXTOS = ETAPA0 / "textos"
REFERENCIA = ETAPA0 / "core" / "src" / "test" / "resources" / "referencia"
IDS = ("T01", "T02", "T03", "T04", "T05", "T06", "T07", "T08", "T09", "T10", "L30")

# ---------------------------------------------------------------- Normalizer.kt

_ESPACO_JAVA = " \t\n\x0b\f\r"  # \s do java.util.regex sem UNICODE_CHARACTER_CLASS
_JUNCAO = re.compile(r"(?<=[^\W_])[-'’](?=[^\W_])")
_NAO_PALAVRA = re.compile(r"(?:[^\w" + _ESPACO_JAVA + r"]|_)+")
_ESPACOS = re.compile("[" + _ESPACO_JAVA + "]+")


def normalizar(texto):
    """Minúsculas, sem acento, sem pontuação; hífen e apóstrofo entre letras unem as partes."""
    minusculo = texto.lower()
    nfd = unicodedata.normalize("NFD", minusculo)
    sem_acento = "".join(c for c in nfd if unicodedata.category(c) != "Mn")
    sem_juncao = _JUNCAO.sub("", sem_acento)
    return _ESPACOS.sub(" ", _NAO_PALAVRA.sub(" ", sem_juncao)).strip()


def palavras(texto):
    return [p for p in normalizar(texto).split(" ") if p]


# ---------------------------------------------------------------- NumberWords.kt

MAX = 999_999
_INT_MAX = 2_147_483_647  # Int.MAX_VALUE: o toIntOrNull do Kotlin devolve null acima disso

_UNIDADES = (
    "zero", "um", "dois", "tres", "quatro", "cinco", "seis", "sete", "oito", "nove",
    "dez", "onze", "doze", "treze", "catorze", "quinze", "dezesseis", "dezessete", "dezoito", "dezenove",
)
_DEZENAS = ("", "", "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa")
_CENTENAS = (
    "", "cento", "duzentos", "trezentos", "quatrocentos",
    "quinhentos", "seiscentos", "setecentos", "oitocentos", "novecentos",
)


def _abaixo_de_mil(n):
    if n < 20:
        return [_UNIDADES[n]]
    if n < 100:
        return [_DEZENAS[n // 10]] + ([] if n % 10 == 0 else ["e", _UNIDADES[n % 10]])
    if n == 100:
        return ["cem"]
    return [_CENTENAS[n // 100]] + ([] if n % 100 == 0 else ["e"] + _abaixo_de_mil(n % 100))


def masculina(n):
    if not 0 <= n <= MAX:
        raise ValueError("número fora da faixa de D14")  # sem o valor (Q6), como no Kotlin
    if n < 1000:
        return _abaixo_de_mil(n)
    milhares, resto = divmod(n, 1000)
    cabeca = ["mil"] if milhares == 1 else _abaixo_de_mil(milhares) + ["mil"]
    if resto == 0:
        return cabeca
    if resto < 100 or resto % 100 == 0:
        return cabeca + ["e"] + _abaixo_de_mil(resto)
    return cabeca + _abaixo_de_mil(resto)


def _variantes(forma):
    if "catorze" in forma:
        return [forma, ["quatorze" if p == "catorze" else p for p in forma]]
    return [forma]


def _feminina(forma):
    saida = []
    for p in forma:
        if p == "um":
            saida.append("uma")
        elif p == "dois":
            saida.append("duas")
        elif p.endswith("entos"):
            saida.append(p[: -len("entos")] + "entas")
        else:
            saida.append(p)
    return saida


def _distintas(formas):
    vistas, saida = set(), []
    for f in formas:
        chave = tuple(f)
        if chave not in vistas:
            vistas.add(chave)
            saida.append(list(f))
    return saida


def formas_cardinais(n):
    m = masculina(n)
    f = _feminina(m)
    bases = [m] if f == m else [m, f]
    return _distintas([v for b in bases for v in _variantes(b)])


def formas_masculinas(n):
    return _variantes(masculina(n))


# ---------------------------------------------------------------- Tokenizer.kt


@dataclass(frozen=True)
class Token:
    indice: int
    inicio: int  # display.first: primeiro caractere exibido (UTF-16 = código Python no BMP)
    fim: int  # display.last: último caractere exibido, inclusivo
    formas: tuple  # tupla de tuplas de palavras normalizadas


_PALAVRA_CRUA = re.compile("[^" + _ESPACO_JAVA + "]+")
_REFERENCIA = re.compile(r"([0-9]+):([0-9]+)(?:[-–]([0-9]+))?")
_INICIO_REFERENCIA = re.compile(r"[0-9]+:[0-9]+")
_INTEIRO = re.compile(r"[0-9]+")
_NUMERO = re.compile(r"([0-9]{1,3}(?:\.[0-9]{3})+|[0-9]+)(?:,([0-9]+))?(%)?")
_NUMERO_LIVRO = ("1", "2", "3")
_PALAVRAS_INTERVALO = ("a", "-", "–")
_ORDINAIS = {
    1: ("primeira", "primeiro", "um"),
    2: ("segunda", "segundo", "dois"),
    3: ("terceira", "terceiro", "tres"),
}


def _letra_ou_digito(c):
    return c.isalpha() or c.isdecimal()  # Character.isLetterOrDigit: L* ou Nd


def _int_ou_none(s):
    v = int(s)
    return v if v <= _INT_MAX else None


class _Cru:
    def __init__(self, texto, inicio):
        self.texto = texto
        self.inicio = inicio
        idx = [i for i, c in enumerate(texto) if _letra_ou_digito(c)]
        fim = [i for i, c in enumerate(texto) if _letra_ou_digito(c) or c == "%"]
        self.tem_nucleo = bool(idx)
        self.ini_nucleo = idx[0] if idx else 0
        self.fim_nucleo = (fim[-1] + 1) if fim else 0
        self.nucleo = texto[self.ini_nucleo:self.fim_nucleo] if self.tem_nucleo else ""

    @property
    def abs_ini(self):
        return self.inicio + self.ini_nucleo

    @property
    def abs_ult(self):
        return self.inicio + self.fim_nucleo - 1


def tokenizar(texto):
    tokens = []
    inicio_linha = 0
    for linha in texto.split("\n"):
        aparada = linha.strip()
        if aparada and not aparada.startswith("//"):
            crus = [_Cru(m.group(0), inicio_linha + m.start()) for m in _PALAVRA_CRUA.finditer(linha)]
            i = 0
            while i < len(crus):
                feito = _montar(crus, i)
                if feito is None:
                    i += 1
                    continue
                ini, ult, formas, ultimo_cru = feito
                tokens.append(Token(len(tokens), ini, ult, tuple(tuple(f) for f in formas)))
                i = ultimo_cru + 1
        inicio_linha += len(linha) + 1
    return tokens


def _montar(crus, i):
    if not crus[i].tem_nucleo:
        return None  # pontuação solta não gera token (D19 da 0002)
    for regra in (_livro, _referencia, _numero, _palavra):
        feito = regra(crus, i)
        if feito is not None:
            return feito
    return None


def _livro(crus, i):
    cru = crus[i]
    if cru.texto not in _NUMERO_LIVRO or i + 2 >= len(crus):
        return None
    nome, ref = crus[i + 1], crus[i + 2]
    if not nome.tem_nucleo or not nome.nucleo[0].isupper() or not _INICIO_REFERENCIA.match(ref.nucleo):
        return None
    palavras_nome = palavras(nome.nucleo)
    formas = [[o] + palavras_nome for o in _ORDINAIS[int(cru.texto)]]
    return cru.abs_ini, nome.abs_ult, formas, i + 1


def _referencia(crus, i):
    cru = crus[i]
    m = _REFERENCIA.fullmatch(cru.nucleo)
    if m is None:
        return None
    capitulo = _int_ou_none(m.group(1))
    v0 = _int_ou_none(m.group(2))
    if capitulo is None or v0 is None:
        return None
    versiculos = [v0]
    ultimo = i
    intervalo = False
    prox = crus[i + 1] if i + 1 < len(crus) else None
    depois = crus[i + 2] if i + 2 < len(crus) else None
    if m.group(3):
        v = _int_ou_none(m.group(3))
        if v is None:
            return None
        versiculos.append(v)
        intervalo = True
    elif prox is not None and prox.texto in _PALAVRAS_INTERVALO and depois is not None and _INTEIRO.fullmatch(depois.nucleo):
        v = _int_ou_none(depois.nucleo)
        if v is None:
            return None
        versiculos.append(v)
        intervalo = True
        ultimo = i + 2
    else:
        while crus[ultimo].texto.endswith(","):
            if ultimo + 1 >= len(crus):
                break
            item = crus[ultimo + 1]
            if not _INTEIRO.fullmatch(item.nucleo):
                break
            v = _int_ou_none(item.nucleo)
            if v is None:
                return None
            versiculos.append(v)
            ultimo += 1
    if capitulo > MAX or any(v > MAX for v in versiculos):
        return None
    return cru.abs_ini, crus[ultimo].abs_ult, _formas_referencia(capitulo, versiculos, intervalo), ultimo


def _formas_referencia(capitulo, versiculos, intervalo):
    opcoes = [formas_masculinas(capitulo)] + [formas_masculinas(v) for v in versiculos]
    formas = []
    for comb in itertools.product(*opcoes):
        c, v = list(comb[0]), [list(x) for x in comb[1:]]
        if intervalo:
            formas += [
                c + v[0] + ["a"] + v[1],
                c + ["de"] + v[0] + ["a"] + v[1],
                ["capitulo"] + c + ["versiculos"] + v[0] + ["a"] + v[1],
            ]
        elif len(v) == 1:
            formas += [c + v[0], c + ["e"] + v[0], ["capitulo"] + c + ["versiculo"] + v[0]]
        else:
            lista = [p for x in v[:-1] for p in x] + ["e"] + v[-1]
            formas += [c + lista, c + ["versiculos"] + lista, ["capitulo"] + c + ["versiculos"] + lista]
    return _distintas(formas)


def _numero(crus, i):
    cru = crus[i]
    m = _NUMERO.fullmatch(cru.nucleo)
    if m is None:
        return None
    valor = _int_ou_none(m.group(1).replace(".", ""))
    if valor is None or valor > MAX:
        return None
    decimais = m.group(2) or ""
    porcento = bool(m.group(3))
    if not decimais and not porcento:
        formas = formas_cardinais(valor)
    else:
        inteiro = formas_masculinas(valor)
        if decimais:
            fracao = _formas_decimais(decimais)
            com_decimais = [w + ["virgula"] + f for w in inteiro for f in fracao]
        else:
            com_decimais = inteiro
        formas = [f + ["por", "cento"] for f in com_decimais] if porcento else com_decimais
    return cru.abs_ini, cru.abs_ult, formas, i


def _formas_decimais(digitos):
    zeros = ["zero"] * (len(digitos) - len(digitos.lstrip("0")))
    resto = digitos[len(zeros):]
    if not resto:
        return [zeros]
    valor = _int_ou_none(resto)
    if valor is None or valor > MAX:
        return [list(digitos)]
    return [zeros + f for f in formas_masculinas(valor)]


def _palavra(crus, i):
    cru = crus[i]
    nucleo = cru.nucleo.rstrip("%")
    unida = palavras(nucleo)
    if not unida:
        return None
    partes = [p for pedaco in nucleo.split("-") for p in palavras(pedaco)]
    if "-" in nucleo and len(partes) > 1 and partes != unida:
        formas = [partes, unida]
    else:
        formas = [unida]
    return cru.abs_ini, cru.abs_ini + len(nucleo) - 1, formas, i


# ---------------------------------------------------------------- catálogo dos textos de teste


def ler_texto(texto_id):
    """Sem tradução de fim de linha, como o readText do Kotlin."""
    return (TEXTOS / f"{texto_id}.txt").read_bytes().decode("utf-8")


def identificar_texto(conteudo):
    """ID do texto de teste com bytes idênticos a `conteudo` (bytes), ou None (spec 0004, D8)."""
    for texto_id in IDS:
        if (TEXTOS / f"{texto_id}.txt").read_bytes() == conteudo:
            return texto_id
    return None


def tokens_por_palavras(palavras_exibidas):
    """Tokens do primeiro texto de teste cujos tokens, normalizados, dão exatamente essa coluna palavra
    do expected.csv (impl_plan 0004, A9); None se nenhum. T01, T04 a T08 e T10 têm os mesmos tokens."""
    for texto_id in IDS:
        texto = ler_texto(texto_id)
        tokens = tokenizar(texto)
        if [normalizar(texto[t.inicio:t.fim + 1]) for t in tokens] == list(palavras_exibidas):
            return tokens
    return None


def formatar_formas(formas):
    """Formato das referências: formas separadas por |, palavras por espaço."""
    return "|".join(" ".join(f) for f in formas)
