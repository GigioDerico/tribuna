"""Dados sintéticos dos testes (spec 0004, D20): tudo sai de etapa0/textos/, nada de gravação real.

Convenções:
- palavra n da transcrição sintética fica em [300n, 300n + 250] ms (critério D1 do spec);
- no esperado sintético, o token i com tempo fica em [300i, 300i + 250] ms, salvo quando o teste diz outra coisa.
"""

import csv
import io
import json

import alinhar
import roteiro
from testes.apoio import Pasta, rodar

PASSO = 300
DURACAO = 250


def tokens(texto_id):
    return roteiro.tokenizar(roteiro.ler_texto(texto_id))


def exibido(texto_id, token):
    return roteiro.ler_texto(texto_id)[token.inicio:token.fim + 1]


def paragrafos(texto_id):
    """Índices de tokens por parágrafo, sem os blocos de nota (//)."""
    texto = roteiro.ler_texto(texto_id)
    blocos, atual, pos, nota = [], None, 0, False
    for linha in texto.split("\n"):
        vazia = not linha.strip()
        if vazia:
            atual = None
        elif atual is None:
            nota = linha.strip().startswith("//")
            atual = [pos, pos + len(linha)]
            if not nota:
                blocos.append(atual)
        else:
            atual[1] = pos + len(linha)
        pos += len(linha) + 1
    return [[t.indice for t in tokens(texto_id) if a <= t.inicio <= b] for a, b in blocos]


def palavras_dos_tokens(lista_tokens, formas=None):
    """Lista de (índice do token, palavra), com a primeira forma (ou formas[i], índice ou lista de palavras)."""
    saida = []
    for t in lista_tokens:
        escolha = (formas or {}).get(t.indice, 0)
        forma = t.formas[escolha] if isinstance(escolha, int) else escolha
        saida += [(t.indice, p) for p in forma]
    return saida


def transcricao_csv(palavras):
    """palavras: lista de palavras cruas; a n-ésima fica em [300n, 300n + 250]."""
    saida = io.StringIO()
    w = csv.writer(saida, lineterminator="\n")
    w.writerow(alinhar.CABECALHO_TRANSCRICAO)
    for n, p in enumerate(palavras):
        w.writerow([p, PASSO * n, PASSO * n + DURACAO, "1.0"])
    return saida.getvalue()


def tempos_esperados(pares_token_palavra):
    """Tempo que cada token deve receber: do início da 1ª palavra ao fim da última (posições na lista)."""
    tempos = {}
    for n, (i, _) in enumerate(pares_token_palavra):
        if i is None:
            continue
        ini, _fim = tempos.get(i, (PASSO * n, None))
        tempos[i] = (ini, PASSO * n + DURACAO)
    return tempos


def linhas_csv(texto):
    return list(csv.reader(io.StringIO(texto, newline="")))


def esperado_csv(texto_id, tempos):
    """tempos: lista por token de (ini, fim) ou None. Usa o próprio escritor do alinhar.py."""
    texto = roteiro.ler_texto(texto_id)
    return alinhar.esperado_csv(texto, roteiro.tokenizar(texto), tempos)


def tempos_regulares(n, sem_tempo=(), deslocamento=None):
    """Token i em [300i, 300i + 250]; `sem_tempo` fica None; `deslocamento(i)` soma ms ao token i."""
    saida = []
    for i in range(n):
        if i in sem_tempo:
            saida.append(None)
        else:
            extra = deslocamento(i) if deslocamento else 0
            saida.append((PASSO * i + extra, PASSO * i + DURACAO + extra))
    return saida


def fim_em(tempos):
    """Função esperado(t) do esperado sintético: maior índice com fim <= t (−1 antes)."""
    fins = sorted((f[1], i) for i, f in enumerate(tempos) if f is not None)

    def esperado(t):
        melhor = -1
        for fim, i in fins:
            if fim <= t:
                melhor = max(melhor, i)
        return melhor

    return esperado


def trajetoria_csv(linhas):
    """linhas: (t_ms, cursor, move). A primeira deve ser Reposition, como no app (0003)."""
    saida = ["t_ms,cursor,move,score"]
    saida += [f"{t},{c},{m},0.0" for t, c, m in linhas]
    return "\n".join(saida) + "\n"


def trajetoria_seguindo(tempos, cursor_de, extras=(), ate=None, move="Short"):
    """Linha Reposition 0 em t=0 e uma linha a cada mudança do cursor desejado, avaliado nos instantes em que
    o esperado muda e nos `extras`. `cursor_de(t, esperado)` dá o cursor desejado."""
    esperado = fim_em(tempos)
    instantes = sorted({f[1] for f in tempos if f is not None} | set(extras))
    if ate is not None:
        instantes = [t for t in instantes if t <= ate]
    linhas = [(0, 0, "Reposition")]
    atual = 0
    for t in instantes:
        if t <= 0:
            continue
        c = max(0, cursor_de(t, esperado(t)))
        if c != atual:
            linhas.append((t, c, move))
            atual = c
    return linhas


def eventos_csv(eventos):
    saida = ["t_ms,evento,detalhe"]
    saida += [f"{t},{e},{d}" for t, e, d in eventos]
    return "\n".join(saida) + "\n"


def rodar_metricas(main, trajetoria, esperado, eventos, hipoteses=None, deslocamento=None):
    """Escreve os arquivos, roda metricas.main com --json e devolve (código, json ou None, stdout, stderr).
    `eventos` é uma lista de conteúdos (um arquivo por item)."""
    with Pasta() as p:
        argv = [p.arquivo("x.trajectory.csv", trajetoria), p.arquivo("x.expected.csv", esperado)]
        argv += [p.arquivo(f"x{k}.events.csv", e) for k, e in enumerate(eventos)]
        if hipoteses is not None:
            argv += ["--hipoteses", p.arquivo("x.hypotheses.csv", hipoteses)]
        if deslocamento is not None:
            argv += ["--deslocamento-ms", str(deslocamento)]
        saida_json = p.dir / "r.json"
        argv += ["--json", str(saida_json)]
        codigo, out, err = rodar(main, argv)
        dados = json.loads(saida_json.read_text(encoding="utf-8")) if saida_json.exists() else None
    return codigo, dados, out, err
