"""metricas.py <trajetoria> <esperado> <eventos> [<eventos>...]: veredito dos critérios de voz da Etapa 0.

TESTES-ETAPA0, "Métricas: como calcular"; spec 0004, D10 a D19; impl_plan 0004, A5 a A9.

Critérios (cada um PASSA ou REPROVA; a recuperação pode ser NAO_SE_APLICA):
- atraso: >= 95% das amostras de leitura normal com esperado - cursor <= 3;
- salto errado: zero (cursor > esperado + 2 fora das janelas de toque; movimento automático em improviso,
  silêncio ou pausa);
- recuperação: <= 3000 ms do pulo_fim até a primeira amostra com o esperado já depois do pulo e atraso <= 3;
- recuo: nenhum movimento automático com cursor < hw - 3.
As latências são informativas e não mudam o código de saída.

Códigos de saída: 0 todos passam; 1 algum reprova; 2 entrada inválida.
"""

import argparse
import bisect
import csv
import hashlib
import io
import json
import math
import statistics
import sys
from dataclasses import dataclass
from pathlib import Path

import roteiro

PASSO_MS = 100  # TESTES-ETAPA0, Métricas: "amostrando a cada 100 ms"
ATRASO_MAX = 3  # TESTES-ETAPA0, Atraso: "≤ 3"
FRACAO_MIN_PCT = 95  # TESTES-ETAPA0, Atraso: "em 95% das amostras"
AVANCO_MAX = 2  # TESTES-ETAPA0, Salto errado: "mais de 2 tokens à frente"
POS_PULO_MS = 3000  # TESTES-ETAPA0, Atraso: "dos 3 s após um pulo"
RECUPERACAO_MAX_MS = 3000  # TESTES-ETAPA0, Recuperação de pulo: "≤ 3 s"
RECUO_MAX = 3  # TESTES-ETAPA0, Recuo: "highWater − 3"
META_LATENCIA_MS = 500  # TRD, Metas de desempenho: total < 500 ms (PROVISÓRIA), só informativa

CAB_TRAJETORIA = ["t_ms", "cursor", "move", "score"]
CAB_ESPERADO = ["token", "palavra", "t_inicio_ms", "t_fim_ms"]
CAB_EVENTOS = ["t_ms", "evento", "detalhe"]
CAB_HIPOTESES = ["t_ms", "session", "kind", "words", "accepted"]
MOVES = ("Stay", "Short", "LongJump", "Recovery", "Reposition")
AUTOMATICOS = ("Short", "LongJump", "Recovery")
PARES = {  # início -> fim dos intervalos (TESTES-ETAPA0, Folha de eventos)
    "improviso_inicio": "improviso_fim",
    "silencio_inicio": "silencio_fim",
    "pausa_toque": "retoma_toque",
    "pulo_inicio": "pulo_fim",
}
EVENTOS = tuple(PARES) + tuple(PARES.values()) + ("toque_palavra",)
SEM_MOVIMENTO = ("improviso", "silencio", "pausa")  # TESTES-ETAPA0, Salto errado (b)


class ErroEntrada(Exception):
    """Código de saída 2."""


@dataclass(frozen=True)
class Linha:
    t: int
    cursor: int
    move: str


@dataclass(frozen=True)
class Esperado:
    token: int
    inicio: int  # None se sem tempo
    fim: int


@dataclass(frozen=True)
class Evento:
    t: int
    nome: str
    detalhe: str


@dataclass(frozen=True)
class Hipotese:
    t: int
    kind: str
    palavras: tuple
    aceita: bool


# ------------------------------------------------------------------ leitura e validação


def _linhas_csv(conteudo, cabecalho, nome):
    linhas = list(csv.reader(io.StringIO(conteudo, newline="")))
    if not linhas or linhas[0] != cabecalho:
        raise ErroEntrada(f"{nome}: cabeçalho diferente de {','.join(cabecalho)}")
    for n, campos in enumerate(linhas[1:], start=1):
        if len(campos) != len(cabecalho):
            raise ErroEntrada(f"{nome}: linha {n} com {len(campos)} campos")
    return linhas[1:]


def _inteiro(texto, nome, n):
    try:
        return int(texto)
    except ValueError:
        raise ErroEntrada(f"{nome}: linha {n} com inteiro inválido") from None


def ler_trajetoria(conteudo, deslocamento):
    saida = []
    for n, (t, cursor, move, score) in enumerate(_linhas_csv(conteudo, CAB_TRAJETORIA, "trajetória"), start=1):
        if move not in MOVES:
            raise ErroEntrada(f"trajetória: linha {n} com move fora de {'/'.join(MOVES)}")
        try:
            float(score)
        except ValueError:
            raise ErroEntrada(f"trajetória: linha {n} com score inválido") from None
        linha = Linha(_inteiro(t, "trajetória", n) + deslocamento, _inteiro(cursor, "trajetória", n), move)
        if linha.cursor < 0:
            raise ErroEntrada(f"trajetória: linha {n} com cursor negativo")
        if saida and linha.t < saida[-1].t:
            raise ErroEntrada(f"trajetória: linha {n} com t_ms decrescente")
        saida.append(linha)
    if not saida or saida[0].move != "Reposition":
        raise ErroEntrada("trajetória: precisa começar por uma linha Reposition (formato da 0003)")
    return saida


def ler_esperado(conteudo):
    """Devolve (lista de Esperado, coluna palavra)."""
    saida, palavras = [], []
    for n, (token, palavra, ini, fim) in enumerate(_linhas_csv(conteudo, CAB_ESPERADO, "esperado"), start=1):
        if _inteiro(token, "esperado", n) != n - 1:
            raise ErroEntrada(f"esperado: linha {n} fora da sequência de tokens")
        palavras.append(palavra)
        if ini == "" and fim == "":
            saida.append(Esperado(n - 1, None, None))
            continue
        a, b = _inteiro(ini, "esperado", n), _inteiro(fim, "esperado", n)
        if b < a:
            raise ErroEntrada(f"esperado: linha {n} com fim antes do início")
        saida.append(Esperado(n - 1, a, b))
    if not any(e.fim is not None for e in saida):
        raise ErroEntrada("esperado: nenhum token com tempo")
    return saida, palavras


def ler_eventos(conteudos, deslocamento, n_tokens):
    eventos = []
    for arquivo, conteudo in enumerate(conteudos):
        for n, (t, nome, detalhe) in enumerate(_linhas_csv(conteudo, CAB_EVENTOS, "eventos"), start=1):
            if nome not in EVENTOS:
                raise ErroEntrada(f"eventos: linha {n} com evento fora da lista do TESTES-ETAPA0")
            if nome in ("toque_palavra", "pulo_inicio"):
                indice = _inteiro(detalhe, "eventos", n) if detalhe.strip() else None
                if indice is None or not 0 <= indice < n_tokens:
                    raise ErroEntrada(f"eventos: linha {n} ({nome}) sem índice de token válido")
            eventos.append((_inteiro(t, "eventos", n) + deslocamento, arquivo, n, nome, detalhe))
    eventos.sort(key=lambda e: (e[0], e[1], e[2]))  # D11: juntados e ordenados por t_ms (empate: arquivo, linha)
    return [Evento(t, nome, detalhe) for t, _, _, nome, detalhe in eventos]


def ler_hipoteses(conteudo, deslocamento):
    saida = []
    for n, (t, _sessao, kind, palavras, aceita) in enumerate(_linhas_csv(conteudo, CAB_HIPOTESES, "hipóteses"), start=1):
        if kind not in ("partial", "final", "error") or aceita not in ("0", "1"):
            raise ErroEntrada(f"hipóteses: linha {n} com kind ou accepted inválido")
        saida.append(Hipotese(_inteiro(t, "hipóteses", n) + deslocamento, kind, tuple(roteiro.palavras(palavras)), aceita == "1"))
    return saida


def intervalos(eventos):
    """Pares início/fim por tipo (D11, D12). Par desfeito ou sobreposto do mesmo tipo: ErroEntrada."""
    abertos = {}
    saida = {"improviso": [], "silencio": [], "pausa": [], "pulo": []}
    tipo = {
        "improviso_inicio": "improviso", "improviso_fim": "improviso",
        "silencio_inicio": "silencio", "silencio_fim": "silencio",
        "pausa_toque": "pausa", "retoma_toque": "pausa",
        "pulo_inicio": "pulo", "pulo_fim": "pulo",
    }
    for ev in eventos:
        if ev.nome not in tipo:
            continue
        k = tipo[ev.nome]
        if ev.nome in PARES:
            if k in abertos:
                raise ErroEntrada(f"eventos: {ev.nome} antes de fechar o anterior")
            abertos[k] = ev
        else:
            if k not in abertos:
                raise ErroEntrada(f"eventos: {ev.nome} sem início")
            inicio = abertos.pop(k)
            saida[k].append((inicio.t, ev.t, inicio.detalhe))
    if abertos:
        raise ErroEntrada(f"eventos: {', '.join(sorted(a.nome for a in abertos.values()))} sem fim")
    return saida


# ------------------------------------------------------------------ cálculo


def _dentro(t, faixas):
    return any(a <= t < b for a, b in faixas)


def _percentil95(valores):
    ordenados = sorted(valores)
    return ordenados[math.ceil(0.95 * len(ordenados)) - 1]  # posto mais próximo


def _estatisticas(valores):
    if not valores:
        return {"n": 0, "mediana": None, "p95": None, "maximo": None}
    return {"n": len(valores), "mediana": statistics.median(valores), "p95": _percentil95(valores), "maximo": max(valores)}


class Medicao:
    def __init__(self, trajetoria, esperado, eventos, hipoteses, formas):
        self.trajetoria = trajetoria
        self.esperado = esperado
        self.formas = formas  # tokens do roteiro (formas faladas), para as latências
        self.intervalos = intervalos(eventos)
        self.toques = [ev for ev in eventos if ev.nome == "toque_palavra"]
        self.hipoteses = hipoteses
        com_tempo = sorted((e.fim, e.token) for e in esperado if e.fim is not None)
        self._fins = [f for f, _ in com_tempo]
        self._max_ate = []  # maior índice com t_fim <= fins[k]
        maior = -1
        for _, token in com_tempo:
            maior = max(maior, token)
            self._max_ate.append(maior)
        self._t_traj = [linha.t for linha in trajetoria if linha.move != "Stay"]
        self._c_traj = [linha.cursor for linha in trajetoria if linha.move != "Stay"]

    # esperado(t): maior token com t_fim_ms <= t; -1 antes do primeiro (D12)
    def esperado_em(self, t):
        k = bisect.bisect_right(self._fins, t)
        return self._max_ate[k - 1] if k else -1

    # cursor(t): cursor da última linha com t_ms <= t; Stay ignorada (D12)
    def cursor_em(self, t):
        k = bisect.bisect_right(self._t_traj, t)
        return self._c_traj[k - 1] if k else None

    def janelas_toque(self):
        """D15 e A7: do toque (ou da linha Reposition que o precede) até o t_fim do primeiro token com tempo >= i."""
        janelas = []
        for ev in self.toques:
            i = int(ev.detalhe)
            inicio = ev.t
            anteriores = [linha for linha in self.trajetoria if linha.t <= ev.t]
            if anteriores and anteriores[-1].move == "Reposition" and anteriores[-1].cursor == i:
                inicio = anteriores[-1].t
            fins = [e.fim for e in self.esperado if e.fim is not None and e.token >= i]
            fim = fins[0] if fins else math.inf
            janelas.append((inicio, fim, i))
        return janelas

    def calcular(self):
        janelas = self.janelas_toque()
        faixas_toque = [(a, b) for a, b, _ in janelas]
        sem_mov = [(a, b) for k in SEM_MOVIMENTO for a, b, _ in self.intervalos[k]]
        pos_pulo = [(b, b + POS_PULO_MS) for _, b, _ in self.intervalos["pulo"]]
        excecoes = sem_mov + pos_pulo

        t0 = max(0, self.trajetoria[0].t)
        primeira = -(-t0 // PASSO_MS) * PASSO_MS  # A6: primeiro múltiplo de 100 >= max(0, início da trajetória)
        ultima = max(max(self._fins), self.trajetoria[-1].t)
        amostras = list(range(primeira, ultima + 1, PASSO_MS))

        normais, excl_toque, excl_intervalo = [], 0, 0
        for t in amostras:
            if _dentro(t, faixas_toque):
                excl_toque += 1
            elif _dentro(t, excecoes):
                excl_intervalo += 1
            else:
                normais.append(t)

        atraso = self._atraso(normais)
        salto = self._salto(amostras, faixas_toque)
        recuperacao = self._recuperacao(amostras)
        recuo = self._recuo()
        criterios = {"atraso": atraso, "salto_errado": salto, "recuperacao": recuperacao, "recuo": recuo}
        reprova = any(c["veredito"] == "REPROVA" for c in criterios.values())
        ultima_linha = self.trajetoria[-1]
        return {
            "veredito": "REPROVA" if reprova else "PASSA",
            "criterios": criterios,
            "amostras": {
                "passo_ms": PASSO_MS,
                "primeira_ms": primeira,
                "ultima_ms": amostras[-1] if amostras else None,
                "total": len(amostras),
                "leitura_normal": len(normais),
                "excluidas_toque": excl_toque,
                "excluidas_intervalo": excl_intervalo,
            },
            "janelas_toque": [
                {"indice": i, "inicio_ms": a, "fim_ms": None if b == math.inf else b,
                 "amostras_excluidas": sum(1 for t in amostras if a <= t < b)}
                for a, b, i in janelas
            ],
            "latencias": self._latencias(faixas_toque, excecoes),
            "cursor_final": {"cursor": ultima_linha.cursor, "ultimo_token": len(self.esperado) - 1},
        }

    def _atraso(self, normais):
        atrasos = [self.esperado_em(t) - self.cursor_em(t) for t in normais]
        ate3 = sum(1 for a in atrasos if a <= ATRASO_MAX)
        passa = bool(atrasos) and ate3 * 100 >= FRACAO_MIN_PCT * len(atrasos)  # inteiros: sem erro de float
        return {
            "veredito": "PASSA" if passa else "REPROVA",
            "amostras": len(atrasos),
            "ate_3": ate3,
            "fracao": (ate3 / len(atrasos)) if atrasos else None,
            "maior_atraso": max(atrasos) if atrasos else None,
            "motivo": None if atrasos else "sem amostras de leitura normal (Q3)",
        }

    def _salto(self, amostras, faixas_toque):
        episodios, atual = [], None
        for t in amostras:
            avanco = None
            if not _dentro(t, faixas_toque):
                avanco = self.cursor_em(t) - self.esperado_em(t)
            if avanco is not None and avanco > AVANCO_MAX:
                if atual is None:
                    atual = {"inicio_ms": t, "fim_ms": t, "amostras": 0, "maior_avanco": avanco}
                    episodios.append(atual)
                atual["fim_ms"] = t
                atual["amostras"] += 1
                atual["maior_avanco"] = max(atual["maior_avanco"], avanco)
            else:
                atual = None
        movimentos = []
        for linha in self.trajetoria:
            if linha.move not in AUTOMATICOS:
                continue
            for k in SEM_MOVIMENTO:
                for a, b, _ in self.intervalos[k]:
                    if a <= linha.t < b:
                        congelado = self.esperado_em(a)
                        movimentos.append({
                            "t_ms": linha.t, "cursor": linha.cursor, "move": linha.move, "intervalo": k,
                            "esperado_no_inicio": congelado,
                            "classe": "alcance" if linha.cursor <= congelado else "além do esperado",
                        })
        total = len(episodios) + len(movimentos)
        return {
            "veredito": "PASSA" if total == 0 else "REPROVA",
            "total": total,
            "por_posicao": episodios,
            "por_movimento": movimentos,
        }

    def _recuperacao(self, amostras):
        pulos = []
        for inicio, fim, detalhe in self.intervalos["pulo"]:
            depois = [e for e in self.esperado if e.fim is not None and e.token >= int(detalhe)]
            alvo = depois[0].token if depois else None
            recuperou = None
            if alvo is not None:
                for t in amostras:
                    if t < fim:
                        continue
                    esp = self.esperado_em(t)
                    if esp >= alvo and esp - self.cursor_em(t) <= ATRASO_MAX:
                        recuperou = t - fim
                        break
            pulos.append({"pulo_inicio_ms": inicio, "primeiro_pulado": int(detalhe), "pulo_fim_ms": fim,
                          "alvo": alvo, "recuperacao_ms": recuperou})
        if not pulos:
            veredito = "NAO_SE_APLICA"
        elif all(p["recuperacao_ms"] is not None and p["recuperacao_ms"] <= RECUPERACAO_MAX_MS for p in pulos):
            veredito = "PASSA"
        else:
            veredito = "REPROVA"
        return {"veredito": veredito, "pulos": pulos}

    def _recuo(self):
        violacoes, hw = [], None
        for linha in self.trajetoria:
            if linha.move == "Reposition":
                hw = linha.cursor
            elif linha.move in AUTOMATICOS:
                if linha.cursor < hw - RECUO_MAX:
                    violacoes.append({"t_ms": linha.t, "cursor": linha.cursor, "hw": hw, "move": linha.move})
                hw = max(hw, linha.cursor)
        return {"veredito": "PASSA" if not violacoes else "REPROVA", "violacoes": violacoes}

    def _primeiro_alcance(self, i):
        """Instante da primeira linha com cursor >= i, se essa linha for movimento automático (D18)."""
        for linha in self.trajetoria:
            if linha.move != "Stay" and linha.cursor >= i:
                return linha.t if linha.move in AUTOMATICOS else None
        return None

    def _latencias(self, faixas_toque, excecoes):
        donos = {}
        for t in self.formas or []:
            for forma in t.formas:
                for p in forma:
                    donos.setdefault(p, set()).add(t.indice)
        fala_cursor, lat1, lat2 = [], [], []
        aceitas = sorted((h for h in self.hipoteses or [] if h.aceita and h.kind in ("partial", "final")),
                         key=lambda h: h.t)
        tempos_aceitas = [h.t for h in aceitas]
        for e in self.esperado:
            if e.fim is None or _dentro(e.fim, faixas_toque) or _dentro(e.fim, excecoes):
                continue
            alcance = self._primeiro_alcance(e.token)
            if alcance is None:
                continue
            fala_cursor.append(alcance - e.fim)
            if self.formas is None:
                continue
            token = self.formas[e.token]
            elegivel = all(donos[p] == {e.token} for forma in token.formas for p in forma)
            if not elegivel or not aceitas:
                continue
            for h in aceitas[bisect.bisect_left(tempos_aceitas, e.inicio):]:
                if any(_contem(h.palavras, forma) for forma in token.formas):
                    lat1.append(h.t - e.fim)
                    lat2.append(alcance - h.t)
                    break
        com_hipoteses = self.hipoteses is not None
        return {
            "meta_provisoria_ms": META_LATENCIA_MS,
            "fala_cursor": _estatisticas(fala_cursor),
            "latencia_1": _estatisticas(lat1) if com_hipoteses else None,
            "latencia_2": _estatisticas(lat2) if com_hipoteses else None,
        }


def _contem(palavras, forma):
    n = len(forma)
    return any(tuple(palavras[k:k + n]) == tuple(forma) for k in range(len(palavras) - n + 1))


# ------------------------------------------------------------------ relatório


def relatorio(resultado, entradas, deslocamento):
    c = resultado["criterios"]
    a, s, r, q = c["atraso"], c["salto_errado"], c["recuperacao"], c["recuo"]
    lat = resultado["latencias"]
    linhas = ["# Métricas da Etapa 0", "", f"Veredito: **{resultado['veredito']}**", "", "## Critérios", ""]
    fracao = "-" if a["fracao"] is None else f"{a['fracao'] * 100:.2f}%"
    linhas.append(f"- Atraso: **{a['veredito']}**, {a['ate_3']}/{a['amostras']} amostras de leitura normal "
                  f"com atraso <= {ATRASO_MAX} ({fracao}; mínimo {FRACAO_MIN_PCT}%); maior atraso {a['maior_atraso']}")
    if a["motivo"]:
        linhas.append(f"  - {a['motivo']}")
    linhas.append(f"- Salto errado: **{s['veredito']}**, {s['total']} ocorrência(s)")
    for ep in s["por_posicao"]:
        linhas.append(f"  - posição: {ep['inicio_ms']}-{ep['fim_ms']} ms, {ep['amostras']} amostra(s), "
                      f"até {ep['maior_avanco']} à frente")
    for mv in s["por_movimento"]:
        linhas.append(f"  - movimento: {mv['move']} em {mv['t_ms']} ms para {mv['cursor']} durante {mv['intervalo']} "
                      f"(esperado no início {mv['esperado_no_inicio']}; {mv['classe']})")
    linhas.append(f"- Recuperação de pulo: **{r['veredito']}**")
    for p in r["pulos"]:
        rec = "não recuperou" if p["recuperacao_ms"] is None else f"{p['recuperacao_ms']} ms"
        linhas.append(f"  - pulo_fim em {p['pulo_fim_ms']} ms (alvo {p['alvo']}): {rec}; máximo {RECUPERACAO_MAX_MS} ms")
    linhas.append(f"- Recuo: **{q['veredito']}**, {len(q['violacoes'])} violação(ões)")
    for v in q["violacoes"]:
        linhas.append(f"  - {v['move']} em {v['t_ms']} ms para {v['cursor']} com hw {v['hw']}")
    am = resultado["amostras"]
    linhas += ["", "## Amostras", "",
               f"- {am['total']} amostras a cada {am['passo_ms']} ms, de {am['primeira_ms']} a {am['ultima_ms']} ms",
               f"- leitura normal: {am['leitura_normal']}; excluídas por intervalo: {am['excluidas_intervalo']}; "
               f"excluídas por toque: {am['excluidas_toque']}"]
    for j in resultado["janelas_toque"]:
        linhas.append(f"- janela de toque (token {j['indice']}): {j['inicio_ms']}-{j['fim_ms']} ms, "
                      f"{j['amostras_excluidas']} amostra(s) excluída(s)")
    linhas += ["", "## Latências (informativas, sem veredito)", ""]
    for nome, chave in (("Fala -> cursor", "fala_cursor"), ("Latência 1", "latencia_1"), ("Latência 2", "latencia_2")):
        e = lat[chave]
        if e is None:
            linhas.append(f"- {nome}: sem --hipoteses")
        else:
            linhas.append(f"- {nome}: n={e['n']}, mediana={e['mediana']}, p95={e['p95']}, máximo={e['maximo']} ms")
    linhas.append(f"- Meta provisória do TRD (total): {lat['meta_provisoria_ms']} ms")
    cf = resultado["cursor_final"]
    linhas += ["", "## Fim", "", f"- cursor final {cf['cursor']}; último token {cf['ultimo_token']}",
               "", "## Proveniência", "", f"- deslocamento: {deslocamento} ms"]
    for e in entradas:
        linhas.append(f"- {e['papel']}: {e['arquivo']} sha256={e['sha256']}")
    return "\n".join(linhas) + "\n"


# ------------------------------------------------------------------ linha de comando


def _ler(caminho):
    with open(caminho, "rb") as f:
        dados = f.read()
    return dados.decode("utf-8"), {"arquivo": Path(caminho).name, "sha256": hashlib.sha256(dados).hexdigest()}


def executar(args):
    entradas = []
    textos = {}
    for papel, caminho in [("trajetoria", args.trajetoria), ("esperado", args.esperado)] + \
            [("eventos", e) for e in args.eventos] + ([("hipoteses", args.hipoteses)] if args.hipoteses else []):
        texto, info = _ler(caminho)
        textos.setdefault(papel, []).append(texto)
        entradas.append({"papel": papel, **info})
    esperado, palavras = ler_esperado(textos["esperado"][0])
    trajetoria = ler_trajetoria(textos["trajetoria"][0], args.deslocamento_ms)
    if any(linha.cursor >= len(esperado) for linha in trajetoria):
        raise ErroEntrada("trajetória: cursor além do último token do esperado")
    eventos = ler_eventos(textos["eventos"], args.deslocamento_ms, len(esperado))
    hipoteses = ler_hipoteses(textos["hipoteses"][0], args.deslocamento_ms) if args.hipoteses else None
    formas = roteiro.tokens_por_palavras(palavras)  # A9: o roteiro sai da coluna palavra do esperado
    if args.hipoteses and formas is None:
        raise ErroEntrada("esperado: a coluna palavra não corresponde a nenhum texto de etapa0/textos/")
    resultado = Medicao(trajetoria, esperado, eventos, hipoteses, formas).calcular()
    resultado["deslocamento_ms"] = args.deslocamento_ms
    resultado["entradas"] = entradas
    return resultado, relatorio(resultado, entradas, args.deslocamento_ms)


def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="metricas.py",
        description="Compara a trajetória do app com a esperada e dá o veredito dos critérios de voz.",
    )
    parser.add_argument("trajetoria", help="<nome>.trajectory.csv (t_ms,cursor,move,score)")
    parser.add_argument("esperado", help="<nome>.expected.csv (token,palavra,t_inicio_ms,t_fim_ms)")
    parser.add_argument("eventos", nargs="+", help="um ou mais <nome>.events.csv (t_ms,evento,detalhe)")
    parser.add_argument("--deslocamento-ms", type=int, default=0,
                        help="somado aos t_ms da trajetória, dos eventos e das hipóteses (padrão 0)")
    parser.add_argument("--hipoteses", help="<nome>.hypotheses.csv, para as latências 1 e 2")
    parser.add_argument("--json", help="grava os mesmos números em JSON neste arquivo")
    args = parser.parse_args(argv)
    try:
        resultado, texto = executar(args)
    except (OSError, UnicodeDecodeError, ErroEntrada) as erro:
        print(f"metricas.py: {erro}", file=sys.stderr)
        return 2
    sys.stdout.write(texto)
    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(resultado, f, ensure_ascii=False, indent=2, sort_keys=True)
            f.write("\n")
    return 0 if resultado["veredito"] == "PASSA" else 1


if __name__ == "__main__":
    sys.exit(main())
