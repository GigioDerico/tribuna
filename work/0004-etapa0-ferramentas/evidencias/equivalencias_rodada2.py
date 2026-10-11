"""Evidência de equivalência das quebras que seguem verdes depois da rodada 2 (0004).

Uso, da raiz do repositório:  python3 work/0004-etapa0-ferramentas/evidencias/equivalencias_rodada2.py [N]

Cada quebra vira um módulo em memória (os arquivos do repositório não são alterados). O script compara a saída do
original com a da quebra em N entradas pseudoaleatórias (semente fixa) e em todos os textos de etapa0/textos/.
Sai com 1 se alguma entrada separar original e quebra. Sem rede, sem arquivo escrito.
"""
import random
import sys
import types
from pathlib import Path

FERR = Path("etapa0/ferramentas").resolve()
sys.path.insert(0, str(FERR))
import roteiro  # noqa: E402

N = int(sys.argv[1]) if len(sys.argv) > 1 else 5000


def fonte(nome):
    return (FERR / nome).read_text(encoding="utf-8")


def modulo(nome, texto):
    mod = types.ModuleType(nome)
    mod.__dict__["__name__"] = nome
    mod.__dict__["__file__"] = str(FERR / "x.py")
    exec(compile(texto, nome, "exec"), mod.__dict__)
    return mod


def quebrar(arquivo, antigo, novo, ocorrencia=0):
    texto = fonte(arquivo)
    partes = texto.split(antigo)
    assert len(partes) - 1 > ocorrencia, (arquivo, antigo)
    return antigo.join(partes[:ocorrencia + 1]) + novo + antigo.join(partes[ocorrencia + 1:])


def plano(x):
    """Tokens e eventos de módulos diferentes são classes diferentes: compara só os campos."""
    if isinstance(x, list):
        return [plano(i) for i in x]
    if hasattr(x, "__dataclass_fields__"):
        return tuple(plano(getattr(x, k)) for k in x.__dataclass_fields__)
    return x


def tenta(f, *args):
    try:
        return plano(f(*args))
    except BaseException as e:  # noqa: BLE001 - uma exceção também é diferença de comportamento
        return ("EXC", type(e).__name__)


falhou = False


def controle(nome, comparadas, diferentes):
    """Quebra que NÃO é equivalente (a rodada 2 a trava com teste): o diferencial tem de enxergá-la."""
    global falhou
    ok = diferentes > 0
    falhou |= not ok
    print(f"{'CONTROLE ok' if ok else 'CONTROLE FALHOU':12s} {nome}: {comparadas} entradas, {diferentes} diferentes (deve ser > 0)")


def relatar(nome, comparadas, diferentes, exemplo=None):
    global falhou
    estado = "EQUIVALENTE" if diferentes == 0 else "DIFERE"
    falhou |= diferentes > 0
    print(f"{estado:12s} {nome}: {comparadas} entradas, {diferentes} diferentes" + (f" ({exemplo!r})" if exemplo else ""))


# ----------------------------------------------------------------------------------------------- alinhar.py
ALINHAR = [  # (nome, trecho original, trecho quebrado): as quebras que ficaram verdes na varredura
    ("alinhar:83 abre = peso + 2", "return peso + 1, peso", "return peso + 2, peso"),
    ("alinhar:94 range(1, m + 2) na linha da inserção", "for j in range(1, len(alvo) + 1):", "for j in range(1, len(alvo) + 2):"),
    ("alinhar:95 condição com ld[j - 2]", "lm[j - 1] < ld[j - 1] else", "lm[j - 1] < ld[j - 2] else"),
    ("alinhar:95 condição com ld[j - 0]", "lm[j - 1] < ld[j - 1] else", "lm[j - 1] < ld[j - 0] else"),
    ("alinhar:95 comparação <=", "lm[j - 1] < ld[j - 1] else", "lm[j - 1] <= ld[j - 1] else"),
    ("alinhar:95 ramo else com ld[j - 2]", "else ld[j - 1]\n", "else ld[j - 2]\n"),
    ("alinhar:95 ramo else com ld[j - 0]", "else ld[j - 1]\n", "else ld[j - 0]\n"),
    ("alinhar:110 peso = linhas + m + 3", "peso = len(linhas) + m + 2", "peso = len(linhas) + m + 3"),
    ("alinhar:110 peso = linhas + m + 1", "peso = len(linhas) + m + 2", "peso = len(linhas) + m + 1"),
    ("alinhar:115 inserção inicial até m + 1", "for j in range(1, m + 1)]),", "for j in range(1, m + 2)]),"),
    ("alinhar:116 linha D inicial com m + 2", 'array("i", [_INF] * (m + 1)),', 'array("i", [_INF] * (m + 2)),'),
    ("alinhar:129 fins[-1] com uma forma só", "fronteiras.append(fins[0])", "fronteiras.append(fins[-1])"),
    ("alinhar:196 fim + 2 no texto exibido", "texto[t.inicio:t.fim + 1])\n        if tempo is None", "texto[t.inicio:t.fim + 2])\n        if tempo is None"),
    ("alinhar:29 _INF = 1 << 29", "_INF = 1 << 30", "_INF = 1 << 29"),
    ("alinhar:91 Y02 inserção -> omissão estende", "y + abre, z + estende", "y + estende, z + estende"),
    ("alinhar:95 Y03 inserção só abre a partir de M", "anterior = lm[j - 1] if lm[j - 1] < ld[j - 1] else ld[j - 1]", "anterior = lm[j - 1]"),
]
GUARDA = [  # alinhar.py:111, a guarda de capacidade: só muda perto de 2^29 células
    ("alinhar:111 (linhas + m + 2)", "(len(linhas) + m + 1)", "(len(linhas) + m + 2)"),
    ("alinhar:111 (linhas + m + 0)", "(len(linhas) + m + 1)", "(len(linhas) + m + 0)"),
    ("alinhar:111 (peso + 2)", "(peso + 1) >=", "(peso + 2) >="),
    ("alinhar:111 (peso + 0)", "(peso + 1) >=", "(peso + 0) >="),
    ("alinhar:111 _INF // 3", ">= _INF // 2", ">= _INF // 3"),
    ("alinhar:111 comparação >", ">= _INF // 2", "> _INF // 2"),
]


def instancias_alinhar(n):
    rng = random.Random(20261011)
    por_texto = {t: roteiro.tokenizar(roteiro.ler_texto(t)) for t in ("T01", "T02", "T03", "T04")}
    vocab = sorted({p for ts in por_texto.values() for t in ts for f in t.formas for p in f})
    saida = []
    for _ in range(n):
        ts = por_texto[rng.choice(list(por_texto))]
        ini = rng.randrange(0, len(ts) - 12)
        fatia = [(t.formas) for t in ts[ini:ini + rng.randint(1, 10)]]
        palavras = []
        for formas in fatia:
            for p in rng.choice(formas):
                r = rng.random()
                if r < 0.12:
                    continue
                palavras.append(rng.choice(vocab) if r < 0.25 else p)
                while rng.random() < 0.2:
                    palavras.append(rng.choice(vocab) if rng.random() < 0.6 else p)
        if rng.random() < 0.5:
            palavras = [rng.choice(vocab) for _ in range(rng.randint(1, 4))] + palavras
        if rng.random() < 0.3:
            palavras += [rng.choice(vocab) for _ in range(rng.randint(1, 3))]
        saida.append((fatia, palavras))
    return saida


def roda_alinhar(mod, fatia, palavras):
    toks = [roteiro.Token(k, 0, 0, formas) for k, formas in enumerate(fatia)]
    tr = [mod.Palavra(p, 300 * n, 300 * n + 250) for n, p in enumerate(palavras)]
    return tenta(mod.alinhar, toks, tr)


def secao_alinhar():
    original = modulo("alinhar_original", fonte("alinhar.py"))
    entradas = instancias_alinhar(N)
    refs = [roda_alinhar(original, *e) for e in entradas]
    mod = modulo("alinhar_q", quebrar("alinhar.py", "anterior = lm[j - 1] if", "anterior = lm[j] if"))
    dif = [i for i, e in enumerate(entradas) if roda_alinhar(mod, *e) != refs[i]]
    controle("alinhar:95 lm[j] no ramo", len(entradas), len(dif))
    for nome, antigo, novo in ALINHAR + GUARDA:
        mod = modulo("alinhar_q", quebrar("alinhar.py", antigo, novo))
        dif = [i for i, e in enumerate(entradas) if roda_alinhar(mod, *e) != refs[i]]
        relatar(nome, len(entradas), len(dif), entradas[dif[0]] if dif else None)


# --------------------------------------------------------------------------------------------- roteiro.py
ROTEIRO = [
    ("roteiro:52 _INT_MAX + 1", "_INT_MAX = 2_147_483_647", "_INT_MAX = 2_147_483_648"),
    ("roteiro:52 _INT_MAX - 1", "_INT_MAX = 2_147_483_647", "_INT_MAX = 2_147_483_646"),
    ("roteiro:161 v < _INT_MAX", "return v if v <= _INT_MAX else None", "return v if v < _INT_MAX else None"),
    ("roteiro:84 resto < 101", "if resto < 100 or resto % 100 == 0:", "if resto < 101 or resto % 100 == 0:"),
    ("roteiro:84 resto <= 100", "if resto < 100 or resto % 100 == 0:", "if resto <= 100 or resto % 100 == 0:"),
    ("roteiro:171 ini_nucleo padrão 1", "self.ini_nucleo = idx[0] if idx else 0", "self.ini_nucleo = idx[0] if idx else 1"),
    ("roteiro:171 ini_nucleo padrão -1", "self.ini_nucleo = idx[0] if idx else 0", "self.ini_nucleo = idx[0] if idx else -1"),
    ("roteiro:172 fim_nucleo padrão 1", "self.fim_nucleo = (fim[-1] + 1) if fim else 0", "self.fim_nucleo = (fim[-1] + 1) if fim else 1"),
    ("roteiro:172 fim_nucleo padrão -1", "self.fim_nucleo = (fim[-1] + 1) if fim else 0", "self.fim_nucleo = (fim[-1] + 1) if fim else -1"),
    ("roteiro:238 prox com i + 2", "prox = crus[i + 1] if i + 1 < len(crus) else None", "prox = crus[i + 1] if i + 2 < len(crus) else None"),
    ("roteiro:282 v[-1] na 1ª forma", "formas += [c + v[0], c + [\"e\"]", "formas += [c + v[-1], c + [\"e\"]"),
    ("roteiro:282 v[-1] na 2ª forma", "c + [\"e\"] + v[0], [\"capitulo\"] + c + [\"versiculo\"] + v[0]]", "c + [\"e\"] + v[-1], [\"capitulo\"] + c + [\"versiculo\"] + v[0]]"),
    ("roteiro:282 v[-1] na 3ª forma", "[\"capitulo\"] + c + [\"versiculo\"] + v[0]]", "[\"capitulo\"] + c + [\"versiculo\"] + v[-1]]"),
    ("roteiro:330 len(partes) > 0", "len(partes) > 1", "len(partes) > 0"),
    ("roteiro:330 len(partes) >= 1", "len(partes) > 1", "len(partes) >= 1"),
    ("roteiro:359 fim + 2 em tokens_por_palavras", "texto[t.inicio:t.fim + 1]) for t in tokens] == list(palavras_exibidas)",
     "texto[t.inicio:t.fim + 2]) for t in tokens] == list(palavras_exibidas)"),
]
FRAGMENTOS = ["13:4", "a", "7", "1", "2", "3", "Coríntios", "João", "Paulo", "—", "-", "5%", "3,5", "999999", "1000000",
              "2147483647", "2147483648", "bom-dia", "d'água", ",", "5,", "13:4-7", "4–7", "1.000", "0,0", "ao", "e",
              "12:1,", "texto.", "(dois)", "x-", "-y", "99999999999", "1.000.000", "%", "10%,"]


def entradas_roteiro(n):
    rng = random.Random(7)
    textos = [roteiro.ler_texto(t) for t in roteiro.IDS]
    for linha in Path("etapa0/core/src/test/resources/referencia/fronteiras.tsv").read_text(encoding="utf-8").split("\n")[1:-1]:
        textos.append(linha.split("\t", 1)[0])
    for _ in range(n):
        textos.append(" ".join(rng.choice(FRAGMENTOS) for _ in range(rng.randint(1, 7))))
    for k in range(0, 2101, 7):
        textos.append(str(k))
    return textos


def secao_roteiro():
    original = modulo("roteiro_original", fonte("roteiro.py"))
    entradas = entradas_roteiro(N)
    refs = [tenta(original.tokenizar, t) for t in entradas]
    mod = modulo("roteiro_q", quebrar("roteiro.py", "MAX = 999_999", "MAX = 1_000_000"))
    controle("roteiro:51 MAX = 1.000.000", len(entradas), sum(tenta(mod.tokenizar, t) != refs[i] for i, t in enumerate(entradas)))
    for nome, antigo, novo in ROTEIRO:
        mod = modulo("roteiro_q", quebrar("roteiro.py", antigo, novo))
        if "359" in nome:
            alvo = [original.tokenizar(original.ler_texto(t)) for t in original.IDS]
            colunas = [[original.normalizar(original.ler_texto(i)[t.inicio:t.fim + 1]) for t in toks]
                       for i, toks in zip(original.IDS, alvo)]
            dif = sum(tenta(mod.tokens_por_palavras, c) != tenta(original.tokens_por_palavras, c) for c in colunas)
            relatar(nome, len(colunas), dif)
            continue
        dif = [i for i, t in enumerate(entradas) if tenta(mod.tokenizar, t) != refs[i]]
        relatar(nome, len(entradas), len(dif), entradas[dif[0]] if dif else None)


# -------------------------------------------------------------------------------------------- metricas.py
def secao_metricas():
    import metricas as m0

    # :158 ordenação dos eventos: o desempate (arquivo, linha) já é a ordem de inserção, e sort() é estável
    quebra = modulo("metricas_q", quebrar("metricas.py", "eventos.sort(key=lambda e: (e[0], e[1], e[2]))",
                                           "eventos.sort(key=lambda e: (e[0], e[1], e[1]))"))
    rng = random.Random(3)
    nomes = list(m0.EVENTOS)
    diferentes = 0
    for _ in range(N):
        arquivos = []
        for _a in range(rng.randint(1, 3)):
            linhas = ["t_ms,evento,detalhe"]
            for _l in range(rng.randint(0, 6)):
                linhas.append(f"{rng.choice([0, 100, 100, 200])},{rng.choice(nomes)},0")
            arquivos.append("\n".join(linhas) + "\n")
        if tenta(m0.ler_eventos, arquivos, 0, 5) != tenta(quebra.ler_eventos, arquivos, 0, 5):
            diferentes += 1
    relatar("metricas:158 chave (t, arquivo, arquivo)", N, diferentes)
    quebra = modulo("metricas_q", quebrar("metricas.py", "eventos.sort(key=lambda e: (e[0], e[1], e[2]))",
                                           "eventos.sort(key=lambda e: (e[0], e[2], e[2]))"))
    rng2 = random.Random(3)
    dif = 0
    for _ in range(N):
        arquivos = []
        for _a in range(rng2.randint(1, 3)):
            linhas = ["t_ms,evento,detalhe"]
            for _l in range(rng2.randint(0, 6)):
                linhas.append(f"{rng2.choice([0, 100, 100, 200])},{rng2.choice(nomes)},0")
            arquivos.append("\n".join(linhas) + "\n")
        dif += tenta(m0.ler_eventos, arquivos, 0, 5) != tenta(quebra.ler_eventos, arquivos, 0, 5)
    controle("metricas:158 chave (t, linha, linha)", N, dif)

    # :228 valor inicial do maior índice e :266 piso do primeiro instante
    for nome, antigo, novo in (("metricas:228 maior = -0", "maior = -1", "maior = -0"), ("metricas:228 maior = -2", "maior = -1", "maior = -2")):
        quebra = modulo("metricas_q", quebrar("metricas.py", antigo, novo))
        dif = 0
        for _ in range(N):
            n = rng.randint(1, 8)
            esp0 = [m0.Esperado(i, *(sorted(rng.sample(range(0, 3000, 50), 2)))) for i in range(n)]
            esp0 = [e if rng.random() > 0.2 else m0.Esperado(e.token, None, None) for e in esp0]
            if all(e.fim is None for e in esp0):
                continue
            tr = [m0.Linha(0, 0, "Reposition")]
            a = m0.Medicao(tr, esp0, [], None, None)
            b = quebra.Medicao(tr, [quebra.Esperado(e.token, e.inicio, e.fim) for e in esp0], [], None, None)
            dif += any(a.esperado_em(t) != b.esperado_em(t) for t in range(-100, 3300, 25))
        relatar(nome, N, dif)
    quebra = modulo("metricas_q", quebrar("metricas.py", "t0 = max(0, self.trajetoria[0].t)", "t0 = max(-1, self.trajetoria[0].t)"))
    dif = 0
    for t0 in range(-1000, 1001):
        tr = [m0.Linha(t0, 0, "Reposition"), m0.Linha(t0 + 500, 0, "Stay")]
        a = m0.Medicao(tr, [m0.Esperado(0, 0, 1000)], [], None, None).calcular()["amostras"]
        tr_q = [quebra.Linha(t0, 0, "Reposition"), quebra.Linha(t0 + 500, 0, "Stay")]
        b = quebra.Medicao(tr_q, [quebra.Esperado(0, 0, 1000)], [], None, None).calcular()["amostras"]
        dif += a != b
    relatar("metricas:266 max(-1, t0)", 2001, dif)

    # :438 janela a mais no fim: a fatia curta nunca é igual à forma
    quebra = modulo("metricas_q", quebrar("metricas.py", "range(len(palavras) - n + 1))", "range(len(palavras) - n + 2))"))
    dif = 0
    for _ in range(N):
        palavras = tuple(rng.choice("abc") for _ in range(rng.randint(0, 6)))
        forma = tuple(rng.choice("abc") for _ in range(rng.randint(1, 3)))
        dif += m0._contem(palavras, forma) != quebra._contem(palavras, forma)
    relatar("metricas:438 range(len - n + 2)", N, dif)


if __name__ == "__main__":
    secao_alinhar()
    secao_roteiro()
    secao_metricas()
    sys.exit(1 if falhou else 0)
