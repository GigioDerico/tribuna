"""veredito.py rodada|final: junta os relatórios do metricas.py nos critérios de saída CS1 a CS4 da Etapa 0.

spec 0005, D3, D7, D8, D9, D10 e critérios C1, C2, E2, E3, H1; impl_plan 0005, A5.

veredito.py rodada --rodada S1 --oficiais <tsv> [--relatorios DIR] ... [--json SAIDA]
  Para cada gravação oficial da rodada lê <nome>.json, <nome>.m200.json e <nome>.p200.json (metricas.py com o
  deslocamento d, d − 200 e d + 200), <nome>.sinc.json (sincronizar.py), <nome>.meta.txt e
  <nome>.revisao.md, e dá o estado de cada critério ligado:
  PASSOU, REPROVOU, NÃO MEDIDO, SENSÍVEL AO DESLOCAMENTO ou PENDENTE DE REVISÃO.
  Com --sufixo S e --config chave=valor (reprodução para calibrar, D11): os arquivos da reprodução são
  <nome>S.* (relatórios, trajetória, eventos, level, hipóteses, meta), e o esperado, o WAV e a revisão continuam
  os da gravação oficial <nome>; a configuração exigida é a oficial com as chaves trocadas.
veredito.py final <rodada.json>... [--json SAIDA]
  Junta as rodadas: CS1 exige S1 e S2 (cada voz, sala); CS2 a CS4 exigem S1 e S3 (os dois ambientes); a S2 entra
  em CS2 a CS4 quando existe.

Regras (impl_plan A5): toda gravação ligada tem de passar (D9); um critério só PASSOU com todas as gravações
exigidas presentes e PASSOU; REPROVOU basta uma; o resto é NÃO MEDIDO, com o motivo. Arquivo que mudou depois
do relatório (sha256 diferente) é entrada inválida (Q8).
Códigos de saída: 0 todos os critérios aplicáveis PASSOU e nada pendente; 1 algum não passou ou há pendência;
2 entrada inválida.
"""

import argparse
import hashlib
import json
import os
import re
import sys
from pathlib import Path

import metricas
import roteiro

PASSOU, REPROVOU, NAO_MEDIDO = "PASSOU", "REPROVOU", "NÃO MEDIDO"
SENSIVEL, PENDENTE, NAO_SE_APLICA = "SENSÍVEL AO DESLOCAMENTO", "PENDENTE DE REVISÃO", "NÃO SE APLICA"
TOLERANCIA_MS = 200  # spec D3
CORRIDAS = ("d", "m200", "p200")  # metricas.py com d, d − 200 e d + 200 (spec E1)
FOLHA = ".folha.events.csv"  # folha de eventos (folha.py); as das corridas vizinhas saem do vizinhas.py
REVISAO_CONCLUIDA = "Revisão concluída: sim"

RODADAS = {  # spec D8: voz, ambiente, L30 obrigatório
    "S1": ("giorgio", "sala", True),
    "S2": ("voz2", "sala", False),
    "S3": ("giorgio", "salao", True),
}
CONFIG_OFICIAL = {  # spec D10 (chaves do meta.txt da 0003)
    "preset": "Normal",
    "vocabulary": "off",
    "recognizer_strategy": "NewPerSession",
    "level_threshold": "0.0",
    "animation_ms": "375",
    "audio_source": "mic",
}
CRITERIOS = ("CS1", "CS2", "CS3", "CS4")
LIGADOS = {  # spec D9
    "CS1": ("T01", "T10", "L30"),
    "CS2": ("L30", "T02", "T03", "T04", "T08", "T09"),
    "CS3": ("T05", "L30"),
    "CS4": ("T06", "T07", "L30"),
}
SO_SALA = ("CS1",)  # CS1: "em ambiente silencioso"
EXIGIDOS = {  # eventos que a gravação precisa ter para o critério medir algo (Q3)
    ("CS2", "T04"): ("improviso",), ("CS2", "L30"): ("improviso",),
    ("CS3", "T05"): ("pulo",), ("CS3", "L30"): ("pulo",),
    ("CS4", "T06"): ("silencio",), ("CS4", "T07"): ("pausa",), ("CS4", "L30"): ("pausa", "silencio"),
}
REGRAS = {  # regra de contagem, ao lado do número (Q7)
    "CS1": "esperado − cursor <= 3 em >= 95% das amostras de 100 ms de leitura normal (fora de improviso, "
           "silêncio, pausa, 3 s depois do pulo e janela de toque)",
    "CS2": "zero amostras com cursor > esperado + 2 fora da janela de toque, zero movimentos automáticos em "
           "improviso, silêncio ou pausa, zero recuos abaixo de highWater − 3",
    "CS3": "do pulo_fim até a primeira amostra com esperado no alvo e atraso <= 3, <= 3.000 ms, em todo pulo",
    "CS4": "zero movimentos automáticos (Short, LongJump, Recovery) dentro de silêncio ou pausa",
}
EXIGIDAS_FINAL = {"CS1": ("S1", "S2"), "CS2": ("S1", "S3"), "CS3": ("S1", "S3"), "CS4": ("S1", "S3")}
_NOME = re.compile(r"([A-Z0-9]+)_([a-z0-9]+)_([a-z]+)_t([0-9]+)")


class ErroEntrada(Exception):
    """Código de saída 2."""


# ------------------------------------------------------------------ leitura


def _sha256(caminho):
    h = hashlib.sha256()
    with open(caminho, "rb") as f:
        for bloco in iter(lambda: f.read(1 << 20), b""):
            h.update(bloco)
    return h.hexdigest()


def _json(caminho):
    try:
        with open(caminho, encoding="utf-8") as f:
            return json.load(f)
    except json.JSONDecodeError as erro:
        raise ErroEntrada(f"{Path(caminho).name}: JSON inválido ({erro})") from None


def obrigatorios(rodada):
    _voz, _amb, com_l30 = RODADAS[rodada]
    lista = [("T01", "")] + [("T02", v) for v in ("1", "2", "3")]
    lista += [(f"T{n:02d}", "") for n in range(3, 11)]
    return lista + ([("L30", "")] if com_l30 else [])


def ler_oficiais(conteudo, rodada):
    """oficiais.tsv (nome<TAB>variante) -> lista de (nome, ID, variante). Valida voz, ambiente e repetição."""
    voz, amb, _ = RODADAS[rodada]
    linhas = [linha.split("\t") for linha in conteudo.splitlines() if linha.strip()]
    if not linhas or linhas[0] != ["nome", "variante"]:
        raise ErroEntrada("oficiais: cabeçalho diferente de nome<TAB>variante")
    saida, vistos = [], set()
    for n, campos in enumerate(linhas[1:], start=1):
        if len(campos) != 2:
            raise ErroEntrada(f"oficiais: linha {n} sem 2 campos")
        nome, variante = campos
        m = _NOME.fullmatch(nome)
        if m is None or m.group(1) not in roteiro.IDS:
            raise ErroEntrada(f"oficiais: linha {n}: nome fora do padrão <ID>_<voz>_<ambiente>_t<n>")
        texto_id = m.group(1)
        if (m.group(2), m.group(3)) != (voz, amb):
            raise ErroEntrada(f"oficiais: linha {n}: {nome} não é da rodada {rodada} ({voz}, {amb})")
        if (texto_id == "T02") != (variante in ("1", "2", "3")) or (texto_id != "T02" and variante != ""):
            raise ErroEntrada(f"oficiais: linha {n}: variante só no T02, e lá é 1, 2 ou 3")
        if (texto_id, variante) in vistos:
            raise ErroEntrada(f"oficiais: linha {n}: {texto_id} {variante} repetido")
        vistos.add((texto_id, variante))
        saida.append((nome, texto_id, variante))
    return saida


def ler_meta(caminho):
    meta = {}
    for linha in Path(caminho).read_text(encoding="utf-8").splitlines():
        if "=" in linha:
            chave, valor = linha.split("=", 1)
            meta[chave] = valor
    return meta


def contar_eventos(conteudos):
    """Intervalos por tipo nos arquivos de eventos que o relatório usou, com a validação do metricas.py."""
    try:
        faixas = metricas.intervalos(metricas.ler_eventos(conteudos, 0, 10**9))
    except metricas.ErroEntrada as erro:
        raise ErroEntrada(str(erro)) from None
    return {k: len(faixas[k]) for k in ("improviso", "silencio", "pausa", "pulo")}


# ------------------------------------------------------------------ estado por gravação


def _sem_leitura(m):
    """Motivo para NÃO MEDIDO quando o motor não acompanhou nada (Q3), ou None."""
    if m["amostras"]["leitura_normal"] == 0:
        return "sem amostras de leitura normal"
    if m["cursor_final"]["cursor"] == 0:
        return "o cursor não saiu do token 0"
    return None


def _fracao_lida(m):
    cf = m["cursor_final"]
    return round(cf["cursor"] / cf["ultimo_token"], 4) if cf["ultimo_token"] else None


def estado_base(criterio, m):
    """(estado, motivo) de um critério num relatório do metricas.py, sem sensibilidade nem revisão."""
    c = m["criterios"]
    if criterio == "CS1":
        a = c["atraso"]
        if a["amostras"] == 0:
            return NAO_MEDIDO, "sem amostras de leitura normal"
        return (PASSOU, None) if a["veredito"] == "PASSA" else (REPROVOU, None)
    if criterio in ("CS2", "CS4") and _sem_leitura(m):
        return NAO_MEDIDO, _sem_leitura(m)
    if criterio == "CS2":
        ruim = c["salto_errado"]["veredito"] == "REPROVA" or c["recuo"]["veredito"] == "REPROVA"
        return (REPROVOU, None) if ruim else (PASSOU, None)
    if criterio == "CS3":
        r = c["recuperacao"]["veredito"]
        if r == "NAO_SE_APLICA":
            return NAO_MEDIDO, "sem pulo na folha de eventos"
        return (PASSOU, None) if r == "PASSA" else (REPROVOU, None)
    movimentos = [mv for mv in c["salto_errado"]["por_movimento"] if mv["intervalo"] in ("silencio", "pausa")]
    return (REPROVOU, None) if movimentos else (PASSOU, None)


def numeros(criterio, m, eventos):
    c = m["criterios"]
    if criterio == "CS1":
        a = c["atraso"]
        return {"amostras": a["amostras"], "ate_3": a["ate_3"], "fracao": a["fracao"], "maior_atraso": a["maior_atraso"]}
    if criterio == "CS2":
        s = c["salto_errado"]
        classes = {}
        for mv in s["por_movimento"]:
            classes[mv["classe"]] = classes.get(mv["classe"], 0) + 1
        return {"amostras": m["amostras"]["total"], "leitura_normal": m["amostras"]["leitura_normal"],
                "episodios_posicao": len(s["por_posicao"]), "movimentos": len(s["por_movimento"]),
                "movimentos_por_classe": classes, "recuos": len(c["recuo"]["violacoes"]),
                "amostras_janela_toque": sum(j["amostras_excluidas"] for j in m["janelas_toque"]),
                "fracao_lida": _fracao_lida(m), "eventos": eventos}
    if criterio == "CS3":
        return {"pulos": [{"recuperacao_ms": p["recuperacao_ms"], "alvo": p["alvo"],
                           "primeiro_pulado": p["primeiro_pulado"]} for p in c["recuperacao"]["pulos"]],
                "eventos": eventos}
    movimentos = [mv for mv in c["salto_errado"]["por_movimento"] if mv["intervalo"] in ("silencio", "pausa")]
    return {"intervalos": {"silencio": eventos["silencio"], "pausa": eventos["pausa"]}, "movimentos": len(movimentos),
            "movimentos_por_classe": {k: sum(1 for mv in movimentos if mv["classe"] == k)
                                      for k in ("alcance", "além do esperado")},
            "leitura_normal": m["amostras"]["leitura_normal"], "fracao_lida": _fracao_lida(m)}


def ligado(criterio, texto_id, ambiente):
    return texto_id in LIGADOS[criterio] and (criterio not in SO_SALA or ambiente == "sala")


def folha_da_corrida(nome, corrida):
    return nome + ("" if corrida == "d" else "." + corrida) + FOLHA


def conferir_entradas(entradas, pastas, nome, oficial=None, corrida="d"):
    """sha256 de cada arquivo citado pelo relatório igual ao do disco (Q8). Devolve {papel: [caminhos]}.
    `oficial`: nome da gravação oficial, dono do esperado e do WAV quando `nome` é uma reprodução.
    `corrida`: d, m200 ou p200; os eventos só podem ser o events.csv do app e a folha dessa corrida."""
    caminhos = {}
    sufixos = {"trajetoria": ".trajectory.csv", "esperado": ".expected.csv", "hipoteses": ".hypotheses.csv",
               "wav": ".wav", "level": ".level.csv"}
    dono = {"esperado": oficial or nome, "wav": oficial or nome}
    for e in entradas:
        papel, arquivo = e["papel"], e["arquivo"]
        if papel in sufixos and arquivo != dono.get(papel, nome) + sufixos[papel]:
            raise ErroEntrada(f"{nome}: o relatório usou {arquivo} como {papel}")
        if papel == "eventos" and arquivo not in (nome + ".events.csv", folha_da_corrida(nome, corrida)):
            raise ErroEntrada(f"{nome}: a corrida {corrida} usou {arquivo} como eventos; só valem {nome}.events.csv "
                              f"e {folha_da_corrida(nome, corrida)}")
        caminho = pastas[papel] / arquivo
        if not caminho.is_file() or _sha256(caminho) != e["sha256"]:
            raise ErroEntrada(f"{nome}: {arquivo} mudou ou sumiu depois do relatório (Q8); rode de novo")
        caminhos.setdefault(papel, []).append(caminho)
    return caminhos


def conferir_folhas(caminhos, m, nome):
    """A folha de cada corrida vizinha é a folha de d no mesmo relógio do WAV (spec D3, E1): mesmos eventos, com
    t + deslocamento da corrida igual a t + d. A folha fica parada onde foi medida; o resto anda ±200 ms."""
    folhas = {k: [c for c in caminhos[k].get("eventos", []) if c.name.endswith(FOLHA)] for k in CORRIDAS}
    if all(not folhas[k] for k in CORRIDAS):
        return
    na_wav = {}
    for k in CORRIDAS:
        if len(folhas[k]) != 1:
            raise ErroEntrada(f"{nome}: a corrida {k} não usou a folha dela ({folha_da_corrida(nome, k)}); "
                              "rode vizinhas.py e o metricas.py das três corridas")
        try:
            na_wav[k] = metricas.ler_eventos([folhas[k][0].read_text(encoding="utf-8")], m[k]["deslocamento_ms"], 10**9)
        except metricas.ErroEntrada as erro:
            raise ErroEntrada(f"{folhas[k][0].name}: {erro}") from None
    for k in ("m200", "p200"):
        if na_wav[k] != na_wav["d"]:
            raise ErroEntrada(f"{nome}: {folhas[k][0].name} não é {folhas['d'][0].name} no relógio do WAV; "
                              "rode vizinhas.py e o metricas.py das três corridas")


def parados_depois(caminhos, d, tipos):
    """Intervalos [(tipo, fim no relógio do WAV)] dos `tipos` sem nenhum movimento automático depois do fim (Q3):
    sem o motor andando depois do intervalo, "zero movimentos dentro" não mostra nada."""
    try:
        trajetoria = metricas.ler_trajetoria(caminhos["trajetoria"][0].read_text(encoding="utf-8"), d)
        conteudos = [c.read_text(encoding="utf-8") for c in caminhos.get("eventos", [])]
        faixas = metricas.intervalos(metricas.ler_eventos(conteudos, d, 10**9))
    except metricas.ErroEntrada as erro:
        raise ErroEntrada(str(erro)) from None
    ultimo = max((linha.t for linha in trajetoria if linha.move in metricas.AUTOMATICOS), default=None)
    return [(k, b) for k in tipos for _, b, _ in faixas[k] if ultimo is None or ultimo < b]


def avaliar_gravacao(oficial, texto_id, pastas, sufixo="", config=None):
    """Estado de cada critério ligado de uma gravação da lista (ou da reprodução dela, com `sufixo`)."""
    config = config or CONFIG_OFICIAL
    nome = oficial + sufixo
    ambiente = _NOME.fullmatch(oficial).group(3)
    criterios = [c for c in CRITERIOS if ligado(c, texto_id, ambiente)]
    rel = pastas["relatorios"]
    arquivos = {"d": rel / f"{nome}.json", "m200": rel / f"{nome}.m200.json", "p200": rel / f"{nome}.p200.json",
                "sinc": rel / f"{nome}.sinc.json", "meta": pastas["trajetoria"] / f"{nome}.meta.txt"}
    faltam = sorted(p.name for p in arquivos.values() if not p.is_file())
    revisao = pastas["esperado"] / f"{oficial}.revisao.md"
    lidos = {p for p in list(arquivos.values()) + [revisao] if p.is_file()}
    saida = {"nome": nome, "oficial": oficial, "texto": texto_id, "criterios": {}, "faltam": faltam, "motivos": [],
             "_lidos": lidos}
    if faltam:
        for c in criterios:
            saida["criterios"][c] = {"estado": NAO_MEDIDO, "motivo": "faltam " + ", ".join(faltam)}
        return saida
    m = {k: _json(arquivos[k]) for k in ("d", "m200", "p200")}
    sinc = _json(arquivos["sinc"])
    d = sinc["deslocamento_ms"]
    for chave, esperado in (("d", d), ("m200", d - TOLERANCIA_MS), ("p200", d + TOLERANCIA_MS)):
        if m[chave]["deslocamento_ms"] != esperado:
            raise ErroEntrada(f"{nome}: {arquivos[chave].name} com deslocamento {m[chave]['deslocamento_ms']}, "
                              f"esperado {esperado} (sincronizar deu {d})")
    comuns = [sorted((e["papel"], e["arquivo"], e["sha256"]) for e in m[k]["entradas"]
                     if not e["arquivo"].endswith(FOLHA)) for k in CORRIDAS]
    if comuns[0] != comuns[1] or comuns[0] != comuns[2]:
        raise ErroEntrada(f"{nome}: os três relatórios do metricas.py não usaram os mesmos arquivos")
    caminhos = {k: conferir_entradas(m[k]["entradas"], pastas, nome, oficial, k) for k in CORRIDAS}
    conferir_folhas(caminhos, m, nome)
    lidos.update(c for k in CORRIDAS for lista in caminhos[k].values() for c in lista)
    lidos.update(c for lista in conferir_entradas(sinc["entradas"], pastas, nome, oficial).values() for c in lista)
    eventos = contar_eventos([c.read_text(encoding="utf-8") for c in caminhos["d"].get("eventos", [])])
    meta = ler_meta(arquivos["meta"])
    if meta.get("text_id") != texto_id:
        raise ErroEntrada(f"{nome}: meta.txt com text_id={meta.get('text_id')}")
    diferentes = [f"{k}={meta.get(k)}" for k, v in config.items() if meta.get(k) != v]
    revisada = revisao.is_file() and REVISAO_CONCLUIDA in revisao.read_text(encoding="utf-8").splitlines()
    saida.update({"deslocamento_ms": d, "sinc_motivos": sinc["motivos"], "revisada": revisada, "eventos": eventos,
                  "configuracao_diferente": diferentes})
    for c in criterios:
        faltando = [k for k in EXIGIDOS.get((c, texto_id), ()) if eventos[k] == 0]
        if diferentes:
            estado, motivo = NAO_MEDIDO, "configuração diferente da pedida (D10 ou --config): " + ", ".join(diferentes)
        elif faltando:
            estado, motivo = NAO_MEDIDO, "sem " + " e ".join(faltando) + " nos eventos"
        else:
            estado, motivo = estado_base(c, m["d"])
            tipos = [k for k in EXIGIDOS.get((c, texto_id), ()) if k in metricas.SEM_MOVIMENTO]
            parados = parados_depois(caminhos["d"], d, tipos) if estado == PASSOU and tipos else []
            if parados:
                estado = NAO_MEDIDO
                motivo = "o cursor não andou depois de " + ", ".join(f"{k} (fim em {b} ms do WAV)" for k, b in parados)
        vizinhos = [estado_base(c, m[k])[0] for k in ("m200", "p200")]
        sensivel = estado != NAO_MEDIDO and (any(v != estado for v in vizinhos) or bool(sinc["motivos"]))
        if sensivel:
            marca = "sensível ao deslocamento: " + "; ".join(
                sinc["motivos"] + [f"com d−200 e d+200: {vizinhos[0]}, {vizinhos[1]}"])
            motivo = marca  # estado_base não dá motivo a PASSOU nem a REPROVOU
            if estado == PASSOU:
                estado = SENSIVEL
        if estado in (PASSOU, REPROVOU, SENSIVEL) and not revisada:
            motivo = f"sem revisão concluída (D7); sem ela seria {estado}" + (f" ({motivo})" if motivo else "")
            estado = PENDENTE
        saida["criterios"][c] = {"estado": estado, "motivo": motivo, "sensivel": sensivel,
                                 "regra": REGRAS[c], "numeros": numeros(c, m["d"], eventos)}
    return saida


# ------------------------------------------------------------------ agregação


def agregar(estados):
    """estados: lista de (nome, estado). Devolve (estado do critério, motivos)."""
    if not estados:
        return NAO_SE_APLICA, []
    if any(e == REPROVOU for _, e in estados):
        return REPROVOU, [f"{n}: {e}" for n, e in estados if e == REPROVOU]
    if all(e == PASSOU for _, e in estados):
        return PASSOU, []
    return NAO_MEDIDO, [f"{n}: {e}" for n, e in estados if e != PASSOU]


def rodada(nome_rodada, oficiais, pastas, sufixo="", config=None, base=None):
    """`base`: pasta do JSON da rodada; os arquivos lidos ficam com caminho relativo a ela e sha256 (Q8)."""
    voz, amb, _ = RODADAS[nome_rodada]
    base = Path(base or Path.cwd()).resolve()
    presentes = {(texto_id, variante): nome for nome, texto_id, variante in oficiais}
    gravacoes = [avaliar_gravacao(nome, texto_id, pastas, sufixo, config) for nome, texto_id, _ in oficiais]
    for g in gravacoes:
        g["lidos"] = {os.path.relpath(p.resolve(), base): _sha256(p) for p in sorted(g.pop("_lidos"))}
    ausentes = [(t, v) for t, v in obrigatorios(nome_rodada) if (t, v) not in presentes]
    criterios = {}
    for c in CRITERIOS:
        estados = [(g["nome"], g["criterios"][c]["estado"]) for g in gravacoes if c in g["criterios"]]
        estados += [(f"{t}{' variante ' + v if v else ''} (ausente)", NAO_MEDIDO) for t, v in ausentes
                    if ligado(c, t, amb)]
        estado, motivos = agregar(estados)
        criterios[c] = {"estado": estado, "motivos": motivos, "gravacoes": len(estados), "regra": REGRAS[c]}
    pendencias = [f"{g['nome']} {c}" for g in gravacoes for c, v in g["criterios"].items() if v["estado"] == PENDENTE]
    return {"rodada": nome_rodada, "voz": voz, "ambiente": amb, "sufixo": sufixo, "config": config or CONFIG_OFICIAL,
            "criterios": criterios, "gravacoes": gravacoes,
            "ausentes": [f"{t}{' variante ' + v if v else ''}" for t, v in ausentes], "pendencias": pendencias}


def final(rodadas, bases):
    """`bases`: pasta de cada JSON de rodada; cada arquivo que a rodada leu tem de estar igual (Q8)."""
    por_nome = {}
    for r, base in zip(rodadas, bases):
        if r.get("rodada") not in RODADAS:
            raise ErroEntrada("final: JSON que não é de veredito.py rodada")
        for g in r["gravacoes"]:
            for relativo, sha in g["lidos"].items():
                caminho = Path(base) / relativo
                if not caminho.is_file() or _sha256(caminho) != sha:
                    raise ErroEntrada(f"final: {relativo} mudou ou sumiu depois da rodada {r['rodada']} (Q8); "
                                      "rode a rodada de novo")
        if r.get("sufixo") or r.get("config") != CONFIG_OFICIAL:
            raise ErroEntrada(f"final: a rodada {r['rodada']} é de reprodução ou de outra configuração; "
                              "os critérios de saída usam só as gravações oficiais (D1, D10)")
        if r["rodada"] in por_nome:
            raise ErroEntrada(f"final: rodada {r['rodada']} repetida")
        por_nome[r["rodada"]] = r
    criterios = {}
    for c in CRITERIOS:
        exigidas = EXIGIDAS_FINAL[c]
        estados = []
        motivos = [f"rodada {s} ausente" for s in exigidas if s not in por_nome]
        for s, r in sorted(por_nome.items()):
            e = r["criterios"][c]["estado"]
            if e != NAO_SE_APLICA:
                estados.append((s, e))
        if any(e == REPROVOU for _, e in estados):
            estado = REPROVOU
        elif not motivos and estados and all(e == PASSOU for _, e in estados):
            estado = PASSOU
        else:
            estado = NAO_MEDIDO
        motivos += [f"{s}: {e}" for s, e in estados if e != PASSOU]
        gravacoes = sum(por_nome[s]["criterios"][c]["gravacoes"] for s, _ in estados)
        criterios[c] = {"estado": estado, "motivos": motivos, "rodadas": [s for s, _ in estados],
                        "gravacoes": gravacoes, "regra": REGRAS[c]}
    return {"criterios": criterios, "pendencias": [p for r in por_nome.values() for p in r["pendencias"]]}


# ------------------------------------------------------------------ relatório e linha de comando


def relatorio_rodada(res):
    titulo = f"# Veredito da rodada {res['rodada']} ({res['voz']}, {res['ambiente']})"
    if res["sufixo"]:
        titulo += f", reprodução {res['sufixo']}"
    linhas = [titulo, "", "Configuração exigida: " + ", ".join(f"{k}={v}" for k, v in res["config"].items()), "",
              "## Critérios", ""]
    linhas += ["| Critério | Estado | Gravações | Motivos |", "|---|---|---|---|"]
    for c, v in res["criterios"].items():
        linhas.append(f"| {c} | {v['estado']} | {v['gravacoes']} | {'; '.join(v['motivos']) or '-'} |")
    linhas += ["", "## Gravações", "", "| Gravação | Critério | Estado | Números | Motivo |", "|---|---|---|---|---|"]
    for g in res["gravacoes"]:
        for c, v in g["criterios"].items():
            nums = json.dumps(v.get("numeros", {}), ensure_ascii=False, sort_keys=True)
            linhas.append(f"| {g['nome']} | {c} | {v['estado']} | {nums} | {v['motivo'] or '-'} |")
    linhas += ["", "## Regras de contagem", ""] + [f"- {c}: {r}" for c, r in REGRAS.items()]
    linhas += ["", f"Ausentes: {', '.join(res['ausentes']) or 'nenhuma'}",
               f"Pendências de revisão: {', '.join(res['pendencias']) or 'nenhuma'}"]
    return "\n".join(linhas) + "\n"


def relatorio_final(res):
    linhas = ["# Critérios de saída CS1 a CS4 (voz)", "", "| Critério | Estado | Rodadas | Gravações | Motivos |",
              "|---|---|---|---|---|"]
    for c, v in res["criterios"].items():
        linhas.append(f"| {c} | {v['estado']} | {', '.join(v['rodadas']) or '-'} | {v['gravacoes']} | "
                      f"{'; '.join(v['motivos']) or '-'} |")
    linhas += ["", "## Regras de contagem", ""] + [f"- {c}: {r}" for c, r in REGRAS.items()]
    linhas += ["", f"Pendências de revisão: {', '.join(res['pendencias']) or 'nenhuma'}"]
    return "\n".join(linhas) + "\n"


def _codigo(res):
    """Pendência já deixa o critério em NÃO MEDIDO (agregar), então basta olhar os estados."""
    return 0 if all(v["estado"] in (PASSOU, NAO_SE_APLICA) for v in res["criterios"].values()) else 1


def main(argv=None):
    parser = argparse.ArgumentParser(prog="veredito.py", description="Critérios de saída CS1 a CS4 por gravação, "
                                     "por rodada e no fim (spec 0005, D9).")
    sub = parser.add_subparsers(dest="comando", required=True)
    p_rodada = sub.add_parser("rodada", help="veredito de uma rodada")
    p_rodada.add_argument("--rodada", required=True, choices=sorted(RODADAS))
    p_rodada.add_argument("--oficiais", required=True, help="TSV nome<TAB>variante das gravações oficiais")
    p_rodada.add_argument("--relatorios", help="pasta dos JSON (padrão: etapa0/relatorios/<rodada>)")
    p_rodada.add_argument("--trajetorias", default=str(roteiro.ETAPA0 / "trajetorias"))
    p_rodada.add_argument("--eventos", default=str(roteiro.ETAPA0 / "eventos"))
    p_rodada.add_argument("--esperado", default=str(roteiro.ETAPA0 / "esperado"))
    p_rodada.add_argument("--gravacoes", default=str(roteiro.ETAPA0 / "gravacoes"))
    p_rodada.add_argument("--sufixo", default="", help="reprodução: <nome><sufixo> (ex.: _r_agil_off_novo_l0000)")
    p_rodada.add_argument("--config", action="append", default=[], metavar="CHAVE=VALOR",
                          help="troca uma chave da configuração oficial (D10) exigida no meta.txt")
    p_rodada.add_argument("--json", help="grava o resultado em JSON")
    p_final = sub.add_parser("final", help="junta as rodadas nos critérios de saída")
    p_final.add_argument("rodadas", nargs="+", help="JSON de veredito.py rodada")
    p_final.add_argument("--json", help="grava o resultado em JSON")
    args = parser.parse_args(argv)
    try:
        if args.comando == "rodada":
            pastas = {"relatorios": Path(args.relatorios or roteiro.ETAPA0 / "relatorios" / args.rodada),
                      "trajetoria": Path(args.trajetorias), "hipoteses": Path(args.trajetorias),
                      "level": Path(args.trajetorias), "eventos": Path(args.eventos),
                      "esperado": Path(args.esperado), "wav": Path(args.gravacoes)}
            oficiais = ler_oficiais(Path(args.oficiais).read_text(encoding="utf-8"), args.rodada)
            config = dict(CONFIG_OFICIAL)
            for par in args.config:
                chave, sep, valor = par.partition("=")
                if not sep or chave not in CONFIG_OFICIAL:
                    raise ErroEntrada(f"--config {par}: use chave=valor com chave em {', '.join(CONFIG_OFICIAL)}")
                config[chave] = valor
            if args.sufixo and not re.fullmatch(r"(_[a-z0-9]+)+", args.sufixo):
                raise ErroEntrada("--sufixo: só _[a-z0-9]+ (o nome da exportação do app não aceita outro caractere)")
            base = Path(args.json).resolve().parent if args.json else Path.cwd()
            res = rodada(args.rodada, oficiais, pastas, args.sufixo, config, base)
            texto = relatorio_rodada(res)
        else:
            res = final([_json(c) for c in args.rodadas], [Path(c).resolve().parent for c in args.rodadas])
            texto = relatorio_final(res)
    except (OSError, UnicodeDecodeError, ErroEntrada) as erro:
        print(f"veredito.py: {erro}", file=sys.stderr)
        return 2
    except (KeyError, TypeError) as erro:
        print(f"veredito.py: JSON sem o campo esperado ({erro!r}); é saída do metricas.py/sincronizar.py?",
              file=sys.stderr)
        return 2
    sys.stdout.write(texto)
    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(res, f, ensure_ascii=False, indent=2, sort_keys=True)
            f.write("\n")
    return _codigo(res)


if __name__ == "__main__":
    sys.exit(main())
