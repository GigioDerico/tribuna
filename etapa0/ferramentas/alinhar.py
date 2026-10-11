"""alinhar.py <texto> <transcricao> [-o SAIDA]: roteiro x transcrição com tempos -> expected.csv.

TESTES-ETAPA0, "Trajetória esperada", passos 2 e 4; spec 0004, D6 a D9.

Needleman-Wunsch em nível de palavra sobre a treliça das formas faladas do Tokenizer:
- custo unitário (igual 0; substituição, inserção e omissão 1), igualdade exata depois do normalizador;
- cada token entra com exatamente uma forma falada, a de menor custo;
- desempate (impl_plan 0004, A3): entre alinhamentos de mesmo custo, o de menos trechos de inserção ou
  omissão; persistindo o empate, vale a primeira forma do token e a primeira ocorrência na transcrição;
- o token recebe tempo só se ao menos uma palavra da forma escolhida casar exatamente.

Só aceita texto idêntico a um de etapa0/textos/ (D8). Escreve só no stdout, no stderr e no -o.
"""

import argparse
import csv
import io
import sys
from array import array
from dataclasses import dataclass

import roteiro

CABECALHO_TRANSCRICAO = ["palavra", "t_inicio_ms", "t_fim_ms", "conf"]
CABECALHO_ESPERADO = ["token", "palavra", "t_inicio_ms", "t_fim_ms"]

_M, _I, _D = 0, 1, 2  # estados: casamento/substituição, inserção (palavra só da transcrição), omissão
_PRIORIDADE = (_I, _M, _D)  # empate na volta: inserção antes de casamento antes de omissão (A3)
_INF = 1 << 30  # "infinito" que cabe em array("i") (4 bytes por célula) mesmo somado a um custo


class ErroEntrada(Exception):
    """Entrada inválida: código de saída 2."""


@dataclass(frozen=True)
class Palavra:
    texto: str  # já normalizada
    inicio: int
    fim: int


def ler_transcricao(conteudo):
    """CSV palavra,t_inicio_ms,t_fim_ms,conf (D5) -> lista de Palavra normalizadas."""
    linhas = list(csv.reader(io.StringIO(conteudo, newline="")))
    if not linhas or linhas[0] != CABECALHO_TRANSCRICAO:
        raise ErroEntrada("transcrição: cabeçalho diferente de palavra,t_inicio_ms,t_fim_ms,conf")
    saida = []
    anterior = None
    for n, campos in enumerate(linhas[1:], start=1):
        if len(campos) != 4:
            raise ErroEntrada(f"transcrição: linha {n} sem 4 campos")
        try:
            inicio, fim = int(campos[1]), int(campos[2])
            if campos[3] != "":
                float(campos[3])
        except ValueError:
            raise ErroEntrada(f"transcrição: linha {n} com número inválido") from None
        if inicio < 0 or fim < inicio:
            raise ErroEntrada(f"transcrição: linha {n} com tempo inválido")
        if anterior is not None and inicio < anterior:
            raise ErroEntrada(f"transcrição: linha {n} fora da ordem do tempo")
        anterior = inicio
        for p in roteiro.palavras(campos[0]):  # "bom-dia" vira uma palavra; vazio não entra (D4)
            saida.append(Palavra(p, inicio, fim))
    return saida


def _trelica(tokens):
    """Linhas da treliça, na ordem: (token, posição na forma, palavra); e a última linha de cada forma."""
    linhas, ultimas = [], []
    for t in tokens:
        ultimas.append([])
        for forma in t.formas:
            for k, palavra in enumerate(forma):
                linhas.append((t.indice, k, palavra))
            ultimas[-1].append(len(linhas) - 1)
    return linhas, ultimas


def _custos(peso):
    """(abrir, estender) um trecho de inserção ou omissão: abrir soma uma edição e um trecho (A3)."""
    return peso + 1, peso


def _linha(pm, pi, pd, palavra, alvo, peso):
    """Uma linha da treliça a partir da anterior (pm, pi, pd): devolve (M, I, D) em array("i")."""
    abre, estende = _custos(peso)
    melhor = list(map(min, pm, pi, pd))
    lm = [_INF] + [min(b + (0 if a == palavra else peso), _INF) for b, a in zip(melhor, alvo)]
    ld = [min(x + abre, y + abre, z + estende, _INF) for x, y, z in zip(pm, pi, pd)]
    li = [_INF]
    corrente = _INF
    for j in range(1, len(alvo) + 1):
        anterior = lm[j - 1] if lm[j - 1] < ld[j - 1] else ld[j - 1]
        corrente = min(anterior + abre, corrente + estende, _INF)
        li.append(corrente)
    return array("i", lm), array("i", li), array("i", ld)


def alinhar(tokens, transcricao):
    """Alinhamento global (Needleman-Wunsch com contagem de trechos, A3).

    Devolve (pares, inseridas): pares[i] = [(índice da palavra, casou exatamente)] alinhadas ao token i;
    inseridas = índices das palavras da transcrição que ficaram como inserção, em ordem.
    """
    alvo = [p.texto for p in transcricao]
    m = len(alvo)
    linhas, ultimas = _trelica(tokens)
    peso = len(linhas) + m + 2  # uma edição vale mais que todos os trechos possíveis juntos
    if (len(linhas) + m + 1) * (peso + 1) >= _INF // 2:
        raise ErroEntrada("roteiro e transcrição grandes demais para o alinhamento")  # o L30 usa ~1/10 do limite
    inicio = (
        array("i", [0] + [_INF] * m),
        array("i", [_INF] + [_custos(peso)[0] + (j - 1) * _custos(peso)[1] for j in range(1, m + 1)]),
        array("i", [_INF] * (m + 1)),
    )
    fronteiras = [inicio]  # fronteiras[b] = estado antes do token b
    guardadas = []  # por linha da treliça: (M, I, D)
    for t in tokens:
        fins = []
        for forma in t.formas:
            pm, pi, pd = fronteiras[-1]
            for palavra in forma:
                pm, pi, pd = _linha(pm, pi, pd, palavra, alvo, peso)
                guardadas.append((pm, pi, pd))
            fins.append((pm, pi, pd))
        if len(fins) == 1:
            fronteiras.append(fins[0])
        else:
            fronteiras.append(tuple(array("i", map(min, *[fim[s] for fim in fins])) for s in (_M, _I, _D)))
    return _voltar(tokens, linhas, ultimas, guardadas, fronteiras, alvo, peso)


def _voltar(tokens, linhas, ultimas, guardadas, fronteiras, alvo, peso):
    """Volta do fim ao início. Empate: inserção, depois casamento, depois omissão; forma de menor índice (A3)."""
    abre, estende = _custos(peso)
    pares = [[] for _ in tokens]
    inseridas = []
    j = len(alvo)
    b = len(tokens)
    estado = min(_PRIORIDADE, key=lambda s: fronteiras[b][s][j])  # min() fica com o primeiro empatado
    linha = None  # None = na fronteira b
    while True:
        if linha is None:
            if b == 0:
                if estado == _I:
                    inseridas.extend(range(j - 1, -1, -1))
                break
            valor = fronteiras[b][estado][j]
            linha = next(u for u in ultimas[b - 1] if guardadas[u][estado][j] == valor)
            continue
        indice, k, palavra = linhas[linha]
        atual = guardadas[linha][estado][j]
        pai = fronteiras[indice] if k == 0 else guardadas[linha - 1]
        if estado == _I:
            inseridas.append(j - 1)
            mesma = guardadas[linha]
            estado = next(s for s in _PRIORIDADE if mesma[s][j - 1] + (estende if s == _I else abre) == atual)
            j -= 1
            continue
        if estado == _M:
            exato = alvo[j - 1] == palavra
            pares[indice].append((j - 1, exato))
            custo = 0 if exato else peso
            estado = next(s for s in _PRIORIDADE if pai[s][j - 1] + custo == atual)
            j -= 1
        else:
            estado = next(s for s in _PRIORIDADE if min(pai[s][j] + (estende if s == _D else abre), _INF) == atual)
        if k == 0:
            linha = None
            b = indice
        else:
            linha -= 1
    inseridas.reverse()
    return pares, inseridas


def tempos_dos_tokens(tokens, transcricao, pares):
    tempos = []
    for t in tokens:
        p = pares[t.indice]
        if any(exato for _, exato in p):
            js = [j for j, _ in p]
            tempos.append((transcricao[min(js)].inicio, transcricao[max(js)].fim))
        else:
            tempos.append(None)
    return tempos


def esperado_csv(texto, tokens, tempos):
    saida = io.StringIO()
    w = csv.writer(saida, lineterminator="\n")
    w.writerow(CABECALHO_ESPERADO)
    for t, tempo in zip(tokens, tempos):
        palavra = roteiro.normalizar(texto[t.inicio:t.fim + 1])
        if tempo is None:
            w.writerow([t.indice, palavra, "", ""])
        else:
            w.writerow([t.indice, palavra, tempo[0], tempo[1]])
    return saida.getvalue()


def _faixas(indices):
    """[3,4,5,9] -> [(3,5),(9,9)]."""
    faixas = []
    for i in indices:
        if faixas and i == faixas[-1][1] + 1:
            faixas[-1][1] = i
        else:
            faixas.append([i, i])
    return [tuple(f) for f in faixas]


def resumo(tokens, transcricao, tempos, inseridas):
    """Roteiro da revisão manual (D9): contagens e faixas, no stderr."""
    sem_tempo = [t.indice for t, tempo in zip(tokens, tempos) if tempo is None]
    linhas = [
        f"tokens com tempo: {len(tokens) - len(sem_tempo)}",
        f"tokens sem tempo: {len(sem_tempo)}",
        f"palavras inseridas: {len(inseridas)}",
    ]
    for a, z in _faixas(sem_tempo):
        linhas.append(f"sem tempo: tokens {a}-{z}")
    for a, z in _faixas(inseridas):
        trecho = " ".join(p.texto for p in transcricao[a:z + 1])
        linhas.append(f"inseridas: palavras {a}-{z} [{transcricao[a].inicio}-{transcricao[z].fim} ms] {trecho}")
    return "\n".join(linhas) + "\n"


def executar(conteudo_texto, conteudo_transcricao):
    """Bytes do texto + texto da transcrição -> (expected.csv, resumo). Lança ErroEntrada."""
    if roteiro.identificar_texto(conteudo_texto) is None:
        raise ErroEntrada("o texto não é idêntico a nenhum de etapa0/textos/ (spec 0004, D8)")
    texto = conteudo_texto.decode("utf-8")
    tokens = roteiro.tokenizar(texto)
    transcricao = ler_transcricao(conteudo_transcricao)
    pares, inseridas = alinhar(tokens, transcricao)
    tempos = tempos_dos_tokens(tokens, transcricao, pares)
    return esperado_csv(texto, tokens, tempos), resumo(tokens, transcricao, tempos, inseridas)


def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="alinhar.py",
        description="Alinha a transcrição com tempos ao roteiro (Needleman-Wunsch) e gera o expected.csv.",
    )
    parser.add_argument("texto", help="texto de teste, idêntico a um de etapa0/textos/")
    parser.add_argument("transcricao", help="CSV palavra,t_inicio_ms,t_fim_ms,conf (saída do timestamps.py)")
    parser.add_argument("-o", "--saida", help="arquivo do expected.csv (padrão: stdout)")
    args = parser.parse_args(argv)
    try:
        with open(args.texto, "rb") as f:
            conteudo_texto = f.read()
        with open(args.transcricao, encoding="utf-8", newline="") as f:
            conteudo_transcricao = f.read()
        esperado, texto_resumo = executar(conteudo_texto, conteudo_transcricao)
    except (OSError, UnicodeDecodeError, ErroEntrada) as erro:
        print(f"alinhar.py: {erro}", file=sys.stderr)
        return 2
    sys.stderr.write(texto_resumo)
    if args.saida:
        with open(args.saida, "w", encoding="utf-8", newline="") as f:
            f.write(esperado)
    else:
        sys.stdout.write(esperado)
    return 0


if __name__ == "__main__":
    sys.exit(main())
