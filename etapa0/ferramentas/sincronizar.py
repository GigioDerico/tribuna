"""sincronizar.py <wav> <level.csv> [--faixa-ms MIN MAX] [--palma-ate-ms N] [--json SAIDA]: põe o relógio do app
no relógio do WAV de referência.

spec 0005, D3 e D4; impl_plan 0005, A3.

Convenção: t_wav = t_app + deslocamento. É o valor de `metricas.py --deslocamento-ms`.

Método:
- cada linha do level.csv (t_ms, rms) é o RMS do bloco de 200 ms que terminou em t_ms (0003: 3.200 amostras);
- o WAV vira energia por fatia de 10 ms; o bloco que termina em t_ms + deslocamento sai da soma de 20 fatias
  (fora do WAV conta como silêncio);
- os dois lados viram dB, com piso de um degrau do PCM 16 bits, e o deslocamento é o de maior correlação de
  Pearson: grade de 100 ms na faixa inteira e depois de 10 ms em volta do melhor (empate: o menor);
- faixa padrão: todo deslocamento em que o WAV cobre a gravação inteira do app (o roteiro manda ligar o
  QuickTime antes do Ouvir e parar depois do Parar); --faixa-ms troca a faixa (reprodução pela fonte WAV);
- palma: a linha mais alta do level.csv até --palma-ate-ms; as linhas a até 1 s dela são correlacionadas de
  novo a até 1 s do deslocamento (empate: o meio do patamar, porque um estalo cabe inteiro em vários blocos de
  200 ms); confere se esse deslocamento fica a até 200 ms do global. O pico do WAV fica registrado;
- deriva: cada metade das linhas (por contagem) é correlacionada de novo a até 1 s do deslocamento; a
  diferença (começo − fim) é estendida à duração inteira. Melhor na borda da busca = indeterminada.

Só lê o WAV; nada de áudio é escrito (CLAUDE.md regra 3).
Códigos de saída: 0 sincronizado; 1 calculado, mas sensível ao deslocamento (palma não confere, deriva acima
de 200 ms ou indeterminada); 2 entrada inválida.
"""

import argparse
import csv
import hashlib
import io
import json
import math
import sys
import wave
from array import array
from pathlib import Path

TAXA = 16000
BLOCO_MS = 200  # 0003: BLOCK_SAMPLES = 3.200 amostras a 16 kHz
SUB_MS = 10  # grade da busca
AMOSTRAS_SUB = TAXA * SUB_MS // 1000  # 160
SUBS_BLOCO = BLOCO_MS // SUB_MS  # 20
AMOSTRAS_BLOCO = TAXA * BLOCO_MS // 1000  # 3.200
PASSO_GROSSO_MS = 100
TOLERANCIA_MS = 200  # spec D3: resolução de um bloco
PALMA_ATE_MS = 4000  # roteiro do spec: palma ~2 s depois do Ouvir; leitura ~2 s depois da palma
JANELA_LOCAL_MS = 1000  # busca da palma no WAV e de cada metade
PISO = 1 / 32768  # um degrau do PCM 16 bits
CAB_LEVEL = ["t_ms", "rms"]
MIN_LINHAS = 4  # duas por metade
EMPATE = 1e-9  # correlações iguais a menos do arredondamento do float


class ErroEntrada(Exception):
    """Código de saída 2."""


# ------------------------------------------------------------------ leitura


def ler_level(conteudo):
    """level.csv -> lista de (t_ms, rms). t não decrescente, rms em [0, 1]. Linhas no mesmo instante caem todas
    no mesmo bloco do WAV e saem como "sem variância"."""
    linhas = list(csv.reader(io.StringIO(conteudo, newline="")))
    if not linhas or linhas[0] != CAB_LEVEL:
        raise ErroEntrada("level.csv: cabeçalho diferente de t_ms,rms")
    saida = []
    for n, campos in enumerate(linhas[1:], start=1):
        if len(campos) != 2:
            raise ErroEntrada(f"level.csv: linha {n} sem 2 campos")
        try:
            t, rms = int(campos[0]), float(campos[1])
        except ValueError:
            raise ErroEntrada(f"level.csv: linha {n} com número inválido") from None
        if t < 0 or not 0 <= rms <= 1:  # nan e inf também caem aqui
            raise ErroEntrada(f"level.csv: linha {n} fora da faixa (t >= 0, 0 <= rms <= 1)")
        if saida and t < saida[-1][0]:
            raise ErroEntrada(f"level.csv: linha {n} com t_ms decrescente")
        saida.append((t, rms))
    if len(saida) < MIN_LINHAS:
        raise ErroEntrada(f"level.csv: precisa de ao menos {MIN_LINHAS} linhas")
    return saida


def energias_do_wav(caminho):
    """Soma dos quadrados por fatia de 10 ms (inteiros exatos). A última fatia incompleta fica de fora."""
    try:
        with wave.open(str(caminho), "rb") as w:
            if w.getcomptype() != "NONE" or w.getnchannels() != 1 or w.getsampwidth() != 2 or w.getframerate() != TAXA:
                raise ErroEntrada("o WAV precisa ser 16 kHz, mono, PCM 16 bits")
            energias = []
            while True:
                dados = w.readframes(AMOSTRAS_SUB * 1000)
                if not dados:
                    break
                a = array("h")
                a.frombytes(dados[: len(dados) - len(dados) % 2])
                if sys.byteorder == "big":
                    a.byteswap()
                for k in range(0, len(a) - AMOSTRAS_SUB + 1, AMOSTRAS_SUB):
                    fatia = a[k:k + AMOSTRAS_SUB]
                    energias.append(math.sumprod(fatia, fatia))
            return energias
    except (wave.Error, EOFError) as erro:
        raise ErroEntrada(f"WAV ilegível: {erro}") from None


# ------------------------------------------------------------------ cálculo


def _db(rms):
    return 20 * math.log10(max(rms, PISO))


def _teto10(v):
    return -((-v) // SUB_MS) * SUB_MS


def _piso10(v):
    return (v // SUB_MS) * SUB_MS


def faixa_padrao(t_min, t_max, duracao_ms):
    """Deslocamentos em que o WAV cobre a gravação inteira: [200 − t_min, duração − t_max], na grade de 10 ms."""
    lo, hi = _teto10(BLOCO_MS - t_min), _piso10(duracao_ms - t_max)
    if lo > hi:
        raise ErroEntrada("o WAV não cobre a gravação do app (QuickTime ligado depois do Ouvir ou parado antes "
                          "do Parar); numa reprodução pela fonte WAV, use --faixa-ms")
    return lo, hi


class Wav:
    def __init__(self, energias):
        self.energias = energias
        self.n = len(energias)
        self.duracao_ms = self.n * SUB_MS
        self.prefixo = [0]
        for e in energias:
            self.prefixo.append(self.prefixo[-1] + e)

    def energia(self, fim):
        """Energia do bloco de 200 ms que termina na fatia `fim` (exclusiva); fora do WAV é silêncio."""
        a = min(max(fim - SUBS_BLOCO, 0), self.n)
        b = min(max(fim, 0), self.n)
        return self.prefixo[b] - self.prefixo[a]

    def db(self, fim):
        return _db(math.sqrt(self.energia(fim) / AMOSTRAS_BLOCO) / 32768)

    def dentro(self, fim):
        return SUBS_BLOCO <= fim <= self.n


class Serie:
    """Linhas do level.csv prontas para a correlação: dB centrado e fim de cada bloco em fatias de 10 ms."""

    def __init__(self, linhas):
        self.linhas = linhas
        self.bases = [t // SUB_MS for t, _ in linhas]  # fatia em que o bloco termina (erro < 10 ms)
        x = [_db(rms) for _, rms in linhas]
        media = math.fsum(x) / len(x)
        self.xc = [v - media for v in x]
        self.sxx = math.fsum(v * v for v in self.xc)
        self.centro = math.fsum(t for t, _ in linhas) / len(linhas)

    def correlacao(self, wav, deslocamento):
        if self.sxx <= 0:
            return None
        s = deslocamento // SUB_MS
        y = [wav.db(b + s) for b in self.bases]
        n = len(y)
        my = math.fsum(y) / n
        syy = math.fsum((v - my) * (v - my) for v in y)
        if syy <= 0:
            return None
        return math.sumprod(self.xc, y) / math.sqrt(self.sxx * syy)


def _melhor(serie, wav, candidatos, meio_do_empate=False):
    """Maior correlação. Empate: o menor deslocamento, ou o do meio do patamar com `meio_do_empate`."""
    pares = [(d, serie.correlacao(wav, d)) for d in candidatos]
    pares = [(d, r) for d, r in pares if r is not None]
    if not pares:
        return None, None
    r_max = max(r for _, r in pares)
    empatados = [d for d, r in pares if r >= r_max - EMPATE]
    d = empatados[(len(empatados) - 1) // 2] if meio_do_empate else empatados[0]
    return d, dict(pares)[d]


def buscar(serie, wav, lo, hi, meio_do_empate=False):
    """Grade de 100 ms a partir de lo e depois de 10 ms a até 100 ms do melhor, sem sair de [lo, hi]: o raio
    da grade fina cobre o passo da grossa até hi. (None, None) se sem variância."""
    d, _ = _melhor(serie, wav, range(lo, hi + 1, PASSO_GROSSO_MS))
    if d is None:
        return None, None
    finos = range(max(lo, d - PASSO_GROSSO_MS), min(hi, d + PASSO_GROSSO_MS) + 1, SUB_MS)
    return _melhor(serie, wav, finos, meio_do_empate)


def palma(linhas, wav, deslocamento, ate_ms):
    """Conferência independente da correlação global: só as linhas a até 1 s da palma, buscadas a até 1 s."""
    saida = {"app_ms": None, "wav_pico_ms": None, "deslocamento_ms": None, "diferenca_ms": None,
             "proeminencia_db": None, "confere": False, "motivo": None}
    candidatas = [(t, rms) for t, rms in linhas if t <= ate_ms]
    if not candidatas:
        saida["motivo"] = f"nenhuma linha do level.csv até {ate_ms} ms"
        return saida
    t_app, rms_app = max(candidatas, key=lambda linha: linha[1])  # max devolve o primeiro em empate
    dbs = sorted(_db(rms) for _, rms in candidatas)
    meio = len(dbs) // 2
    mediana = dbs[meio] if len(dbs) % 2 else (dbs[meio - 1] + dbs[meio]) / 2
    saida["app_ms"], saida["proeminencia_db"] = t_app, round(_db(rms_app) - mediana, 2)
    base = t_app // SUB_MS + deslocamento // SUB_MS
    raio = JANELA_LOCAL_MS // SUB_MS
    fatias = [k for k in range(base - raio - SUBS_BLOCO, base + raio) if 0 <= k < wav.n]
    if fatias:
        saida["wav_pico_ms"] = max(fatias, key=lambda k: wav.energias[k]) * SUB_MS
    locais = [linha for linha in linhas if abs(linha[0] - t_app) <= JANELA_LOCAL_MS]
    lo, hi = deslocamento - JANELA_LOCAL_MS, deslocamento + JANELA_LOCAL_MS
    d_palma, _ = buscar(Serie(locais), wav, lo, hi, meio_do_empate=True)
    if d_palma is None:
        saida["motivo"] = "sem variância em volta da palma"
        return saida
    saida["deslocamento_ms"], saida["diferenca_ms"] = d_palma, d_palma - deslocamento
    saida["confere"] = abs(d_palma - deslocamento) <= TOLERANCIA_MS  # a borda da busca (1 s) já fica fora
    return saida


def deriva(linhas, wav, deslocamento):
    metade = len(linhas) // 2
    partes = []
    for pedaco in (linhas[:metade], linhas[metade:]):
        serie = Serie(pedaco)
        lo, hi = deslocamento - JANELA_LOCAL_MS, deslocamento + JANELA_LOCAL_MS
        d, _ = buscar(serie, wav, lo, hi)
        partes.append((d, serie.centro, d is None or d in (lo, hi)))
    (d_ini, c_ini, borda_ini), (d_fim, c_fim, borda_fim) = partes
    indeterminada = borda_ini or borda_fim
    saida = {"inicio_ms": d_ini, "fim_ms": d_fim, "centro_inicio_ms": round(c_ini), "centro_fim_ms": round(c_fim),
             "diferenca_metades_ms": None, "deriva_ms": None, "indeterminada": indeterminada}
    if not indeterminada:
        duracao = linhas[-1][0] - linhas[0][0]
        saida["diferenca_metades_ms"] = d_ini - d_fim
        saida["deriva_ms"] = round((d_ini - d_fim) * duracao / (c_fim - c_ini))
    return saida


def motivos_de(resultado_palma, resultado_deriva):
    motivos = []
    if not resultado_palma["confere"]:
        motivos.append("palma não confere" + (f" ({resultado_palma['motivo']})" if resultado_palma["motivo"] else ""))
    if resultado_deriva["indeterminada"]:
        motivos.append("deriva indeterminada")
    elif abs(resultado_deriva["deriva_ms"]) > TOLERANCIA_MS:
        motivos.append(f"deriva acima de {TOLERANCIA_MS} ms")
    return motivos


def sincronizar(linhas, energias, faixa=None, palma_ate_ms=PALMA_ATE_MS):
    wav = Wav(energias)
    if wav.n < SUBS_BLOCO:
        raise ErroEntrada("WAV mais curto que um bloco de 200 ms")
    if faixa is None:
        lo, hi = faixa_padrao(linhas[0][0], linhas[-1][0], wav.duracao_ms)
    else:
        lo, hi = _teto10(faixa[0]), _piso10(faixa[1])
        if lo > hi:
            raise ErroEntrada("--faixa-ms: MIN maior que MAX")
    serie = Serie(linhas)
    d, r = buscar(serie, wav, lo, hi)
    if d is None:
        raise ErroEntrada("sem variância no level.csv ou no WAV: nada para correlacionar")
    resultado_palma = palma(linhas, wav, d, palma_ate_ms)
    resultado_deriva = deriva(linhas, wav, d)
    motivos = motivos_de(resultado_palma, resultado_deriva)
    s = d // SUB_MS
    return {
        "deslocamento_ms": d,
        "correlacao": round(r, 4),
        "faixa_ms": [lo, hi],
        "linhas": len(linhas),
        "linhas_fora_do_wav": sum(1 for b in serie.bases if not wav.dentro(b + s)),
        "duracao_wav_ms": wav.duracao_ms,
        "palma_ate_ms": palma_ate_ms,
        "palma": resultado_palma,
        "deriva": resultado_deriva,
        "sensivel": bool(motivos),
        "motivos": motivos,
        "codigo": 1 if motivos else 0,
    }


# ------------------------------------------------------------------ relatório e linha de comando


def relatorio(res, entradas):
    p, dv = res["palma"], res["deriva"]
    veredito = "SENSÍVEL AO DESLOCAMENTO" if res["sensivel"] else "SINCRONIZADO"
    linhas = [
        "# Sincronização", "", f"Veredito: **{veredito}**", "",
        f"- deslocamento: {res['deslocamento_ms']} ms (t_wav = t_app + deslocamento); correlação {res['correlacao']}; "
        f"faixa {res['faixa_ms'][0]} a {res['faixa_ms'][1]} ms; {res['linhas']} linhas do level.csv, "
        f"{res['linhas_fora_do_wav']} fora do WAV; WAV de {res['duracao_wav_ms']} ms",
    ]
    if p["deslocamento_ms"] is None:
        linhas.append(f"- palma: não conferida ({p['motivo']})")
    else:
        linhas.append(f"- palma: app {p['app_ms']} ms, pico no WAV {p['wav_pico_ms']} ms, deslocamento pela "
                      f"palma {p['deslocamento_ms']} ms, diferença {p['diferenca_ms']} ms (limite {TOLERANCIA_MS}), "
                      f"proeminência {p['proeminencia_db']} dB: {'confere' if p['confere'] else 'NÃO CONFERE'}")
    if dv["indeterminada"]:
        linhas.append(f"- deriva: INDETERMINADA (começo {dv['inicio_ms']} ms, fim {dv['fim_ms']} ms)")
    else:
        linhas.append(f"- deriva: começo {dv['inicio_ms']} ms, fim {dv['fim_ms']} ms, diferença entre as metades "
                      f"{dv['diferenca_metades_ms']} ms, estendida à gravação {dv['deriva_ms']} ms "
                      f"(limite {TOLERANCIA_MS})")
    for m in res["motivos"]:
        linhas.append(f"- motivo: {m}")
    linhas += ["", "## Proveniência", ""]
    for e in entradas:
        linhas.append(f"- {e['papel']}: {e['arquivo']} sha256={e['sha256']}")
    return "\n".join(linhas) + "\n"


def _sha256(caminho):
    h = hashlib.sha256()
    with open(caminho, "rb") as f:
        for bloco in iter(lambda: f.read(1 << 20), b""):
            h.update(bloco)
    return h.hexdigest()


def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="sincronizar.py",
        description="Deslocamento entre o relógio do app (level.csv) e o do WAV de referência (t_wav = t_app + d).",
    )
    parser.add_argument("wav", help="WAV de referência, 16 kHz, mono, PCM 16 bits")
    parser.add_argument("level", help="<nome>.level.csv exportado pelo app (t_ms,rms)")
    parser.add_argument("--faixa-ms", nargs=2, type=int, metavar=("MIN", "MAX"),
                        help="faixa de busca do deslocamento (padrão: o WAV cobre a gravação inteira)")
    parser.add_argument("--palma-ate-ms", type=int, default=PALMA_ATE_MS,
                        help=f"procura a palma no level.csv até este t_ms (padrão {PALMA_ATE_MS})")
    parser.add_argument("--json", help="grava o resultado em JSON neste arquivo")
    args = parser.parse_args(argv)
    try:
        with open(args.level, encoding="utf-8", newline="") as f:
            linhas = ler_level(f.read())
        energias = energias_do_wav(args.wav)
        faixa = tuple(args.faixa_ms) if args.faixa_ms else None
        res = sincronizar(linhas, energias, faixa, args.palma_ate_ms)
        entradas = [{"papel": "wav", "arquivo": Path(args.wav).name, "sha256": _sha256(args.wav)},
                    {"papel": "level", "arquivo": Path(args.level).name, "sha256": _sha256(args.level)}]
    except (OSError, UnicodeDecodeError, ErroEntrada) as erro:
        print(f"sincronizar.py: {erro}", file=sys.stderr)
        return 2
    res["entradas"] = entradas
    sys.stdout.write(relatorio(res, entradas))
    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(res, f, ensure_ascii=False, indent=2, sort_keys=True)
            f.write("\n")
    return res["codigo"]


if __name__ == "__main__":
    sys.exit(main())
