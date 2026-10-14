"""nivel.py <nome>... [--relatorios DIR] [--trajetorias DIR] [--eventos DIR] [--json SAIDA]: limiar de nível.

spec 0005, D12 (limiar) e critério F4; impl_plan 0005, A6.

Para cada gravação (relatório <nome>.json do metricas.py com limiar 0, <nome>.level.csv, <nome>.hypotheses.csv e
os eventos que o relatório usou): o nível (rms do level.csv) dentro do improviso, do silêncio e da pausa e na
leitura (o resto), com n, mediana, p95 e máximo; e cada movimento automático dentro desses intervalos, com a
hipótese aceita que o causou (a última com t <= t do movimento, no relógio do app) e o nível em vigor nela
(o da última linha do level.csv com t <= t da hipótese: é o `lastLevel` que o ReaderController compara).

Regra do D12: sem movimento em silêncio ou pausa, a recomendação é 0. Com movimento, o candidato é o menor nível
do level.csv (de todas as gravações) acima do maior nível das hipóteses parciais que causaram esses movimentos.
O limiar não atua em Final (ReaderController: `isFinal || lastLevel >= threshold`); movimento causado por Final
fica listado e não entra no candidato. O candidato ainda tem de ser verificado por reprodução (F4).
Códigos de saída: 0 sem movimento em silêncio ou pausa; 1 há movimento (candidato a verificar ou nenhum);
2 entrada inválida.
"""

import argparse
import json
import math
import statistics
import sys
from pathlib import Path

import metricas
import roteiro
import sincronizar
import veredito

INTERVALOS = ("improviso", "silencio", "pausa")
CORRIGIVEIS = ("silencio", "pausa")  # D12: o limiar é para silêncio e pausa


class ErroEntrada(Exception):
    """Código de saída 2."""


def estatisticas(valores):
    """n, mediana, p95 (posto mais próximo, como o metricas.py) e máximo."""
    if not valores:
        return {"n": 0, "mediana": None, "p95": None, "maximo": None}
    ordenados = sorted(valores)
    return {"n": len(ordenados), "mediana": statistics.median(ordenados),
            "p95": ordenados[math.ceil(0.95 * len(ordenados)) - 1], "maximo": ordenados[-1]}


def _ultimo_ate(itens, t, chave):
    melhor = None
    for item in itens:
        if chave(item) <= t:
            melhor = item
        else:
            break
    return melhor


def analisar(nome, relatorio, level, hipoteses, eventos):
    """relatorio: JSON do metricas.py; level: [(t, rms)]; hipoteses: [metricas.Hipotese] (relógio do app)."""
    d = relatorio["deslocamento_ms"]
    faixas = metricas.intervalos(eventos)
    stats = {}
    for k in INTERVALOS:
        stats[k] = estatisticas([rms for t, rms in level if any(a <= t < b for a, b, _ in faixas[k])])
    fora = [(t, rms) for t, rms in level if not any(a <= t < b for k in INTERVALOS for a, b, _ in faixas[k])]
    stats["leitura"] = estatisticas([rms for _, rms in fora])
    aceitas = sorted((h for h in hipoteses if h.aceita and h.kind in ("partial", "final")), key=lambda h: h.t)
    movimentos = []
    for mv in relatorio["criterios"]["salto_errado"]["por_movimento"]:
        t_app = mv["t_ms"] - d
        causa = _ultimo_ate(aceitas, t_app, lambda h: h.t)
        linha = _ultimo_ate(level, causa.t, lambda linha: linha[0]) if causa else None
        movimentos.append({"t_app_ms": t_app, "intervalo": mv["intervalo"], "move": mv["move"], "classe": mv["classe"],
                           "causa_t_ms": causa.t if causa else None, "causa_kind": causa.kind if causa else None,
                           "causa_nivel": linha[1] if linha else None})
    return {"nome": nome, "deslocamento_ms": d, "niveis": stats, "movimentos": movimentos,
            "leitura_rms": [rms for _, rms in fora]}


def recomendar(gravacoes, todos_os_niveis):
    corrigiveis = [mv for g in gravacoes for mv in g["movimentos"] if mv["intervalo"] in CORRIGIVEIS]
    if not corrigiveis:
        return {"recomendacao": 0.0, "candidato": None, "limite": None,
                "motivo": "nenhum movimento automático em silêncio ou pausa com limiar 0 (D12)"}
    por_parcial = [mv["causa_nivel"] for mv in corrigiveis if mv["causa_kind"] == "partial" and mv["causa_nivel"] is not None]
    if not por_parcial:
        return {"recomendacao": None, "candidato": None, "limite": None,
                "motivo": "movimentos causados por Final ou sem nível conhecido: o limiar não atua neles"}
    limite = max(por_parcial)
    acima = [rms for rms in todos_os_niveis if rms > limite]
    if not acima:
        return {"recomendacao": None, "candidato": None, "limite": limite,
                "motivo": "nenhum nível do level.csv acima do nível das parciais que causaram os movimentos"}
    candidato = min(acima)
    sem_efeito = len(corrigiveis) - len(por_parcial)
    motivo = "menor nível do level.csv acima do maior nível das parciais que causaram movimento (D12); verificar por reprodução"
    if sem_efeito:
        motivo += f"; {sem_efeito} movimento(s) por Final ou sem nível não são resolvidos pelo limiar"
    return {"recomendacao": None, "candidato": candidato, "limite": limite, "motivo": motivo}


def carregar(nome, pastas):
    rel = pastas["relatorios"] / f"{nome}.json"
    if not rel.is_file():
        raise ErroEntrada(f"{nome}: falta {rel.name}")
    try:
        relatorio = json.loads(rel.read_text(encoding="utf-8"))
    except json.JSONDecodeError as erro:
        raise ErroEntrada(f"{rel.name}: JSON inválido ({erro})") from None
    try:
        caminhos = veredito.conferir_entradas(relatorio["entradas"], pastas, nome)
    except veredito.ErroEntrada as erro:
        raise ErroEntrada(str(erro)) from None
    try:
        conteudos = [c.read_text(encoding="utf-8") for c in caminhos.get("eventos", [])]
        eventos = metricas.ler_eventos(conteudos, 0, 10**9)
        level = sincronizar.ler_level((pastas["trajetoria"] / f"{nome}.level.csv").read_text(encoding="utf-8"))
        hipoteses = metricas.ler_hipoteses((pastas["trajetoria"] / f"{nome}.hypotheses.csv").read_text(encoding="utf-8"), 0)
    except (metricas.ErroEntrada, sincronizar.ErroEntrada) as erro:
        raise ErroEntrada(f"{nome}: {erro}") from None
    return relatorio, level, hipoteses, eventos


def relatorio_md(res):
    linhas = ["# Limiar de nível (F4)", "", "| Gravação | Intervalo | n | mediana | p95 | máximo |", "|---|---|---|---|---|---|"]
    for g in res["gravacoes"]:
        for k in INTERVALOS + ("leitura",):
            e = g["niveis"][k]
            linhas.append(f"| {g['nome']} | {k} | {e['n']} | {e['mediana']} | {e['p95']} | {e['maximo']} |")
    linhas += ["", "## Movimentos automáticos dentro dos intervalos (limiar 0)", ""]
    movs = [(g["nome"], mv) for g in res["gravacoes"] for mv in g["movimentos"]]
    if not movs:
        linhas.append("- nenhum")
    for nome, mv in movs:
        linhas.append(f"- {nome}: {mv['move']} em {mv['t_app_ms']} ms do app, {mv['intervalo']} ({mv['classe']}); "
                      f"causa {mv['causa_kind']} em {mv['causa_t_ms']} ms, nível {mv['causa_nivel']}")
    r = res["recomendacao"]
    linhas += ["", "## Recomendação", "", f"- recomendação: {r['recomendacao']}", f"- candidato: {r['candidato']}",
               f"- maior nível das parciais causadoras: {r['limite']}", f"- {r['motivo']}"]
    for g in res["gravacoes"]:
        if g.get("leitura_abaixo_do_candidato") is not None:
            linhas.append(f"- {g['nome']}: {g['leitura_abaixo_do_candidato']} de {g['niveis']['leitura']['n']} "
                          "linhas de leitura abaixo do candidato")
    return "\n".join(linhas) + "\n"


def main(argv=None):
    parser = argparse.ArgumentParser(prog="nivel.py", description="Nível de áudio nos intervalos e candidato a limiar (D12).")
    parser.add_argument("nomes", nargs="+", help="gravações (<ID>_<voz>_<ambiente>_t<n>), com relatório de limiar 0")
    parser.add_argument("--relatorios", required=True, help="pasta dos <nome>.json do metricas.py")
    parser.add_argument("--trajetorias", default=str(roteiro.ETAPA0 / "trajetorias"))
    parser.add_argument("--eventos", default=str(roteiro.ETAPA0 / "eventos"))
    parser.add_argument("--esperado", default=str(roteiro.ETAPA0 / "esperado"))
    parser.add_argument("--json", help="grava o resultado em JSON")
    args = parser.parse_args(argv)
    pastas = {"relatorios": Path(args.relatorios), "trajetoria": Path(args.trajetorias),
              "hipoteses": Path(args.trajetorias), "eventos": Path(args.eventos), "esperado": Path(args.esperado)}
    try:
        gravacoes, todos = [], []
        for nome in args.nomes:
            relatorio, level, hipoteses, eventos = carregar(nome, pastas)
            gravacoes.append(analisar(nome, relatorio, level, hipoteses, eventos))
            todos += [rms for _, rms in level]
        recomendacao = recomendar(gravacoes, todos)
    except (OSError, UnicodeDecodeError, KeyError, TypeError, ErroEntrada) as erro:
        print(f"nivel.py: {erro!r}" if isinstance(erro, (KeyError, TypeError)) else f"nivel.py: {erro}", file=sys.stderr)
        return 2
    for g in gravacoes:
        leitura = g.pop("leitura_rms")
        c = recomendacao["candidato"]
        g["leitura_abaixo_do_candidato"] = None if c is None else sum(1 for rms in leitura if rms < c)
    res = {"gravacoes": gravacoes, "recomendacao": recomendacao}
    sys.stdout.write(relatorio_md(res))
    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(res, f, ensure_ascii=False, indent=2, sort_keys=True)
            f.write("\n")
    tem_movimento = any(mv["intervalo"] in CORRIGIVEIS for g in gravacoes for mv in g["movimentos"])
    return 1 if tem_movimento else 0


if __name__ == "__main__":
    sys.exit(main())
