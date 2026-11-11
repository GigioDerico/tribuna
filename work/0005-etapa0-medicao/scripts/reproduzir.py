#!/usr/bin/env python3
"""reproduzir.py <oficial> --preset P --vocab on|off --estrategia novo|reset [--limiar V] [--painel] [--ciclos N]
[--captura] [--rodada S1]

Reproduz no app o WAV de uma gravação oficial pela fonte WAV da 0003, com a configuração pedida, e exporta como
<oficial>_r_<preset>_<vocab>_<estrategia>_l<limiar×1000 em 4 dígitos> (spec D1, D11, F1–F4, G1; impl_plan 0005, A10).
- As pausas da gravação oficial (pausa_toque/retoma_toque do events.csv dela) são repetidas por adb no mesmo
  instante do WAV: t_wav = t_app_oficial + deslocamento oficial (sinc.json da rodada).
- --painel: lê as linhas de beginSession e de latência do painel durante a reprodução (uiautomator, um dump por
  volta) e grava em evidencias/painel/<rep>.tsv; --captura troca o dump por screencap (PNG fora do repositório)
  se o uiautomator não conseguir ler a tela em movimento.
- --ciclos N: em vez de seguir as pausas oficiais, faz N ciclos Pausar -> Ouvir (F3) e lê o beginSession de cada um.
Só dirige o aparelho; os números saem do metricas.py, do veredito.py e do painel.
"""

import argparse
import json
import re
import subprocess
import sys
import time
import wave
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ui  # noqa: E402

RAIZ = Path(subprocess.run(["git", "-C", str(Path(__file__).parent), "rev-parse", "--show-toplevel"],
                           capture_output=True, text=True, check=True).stdout.strip())
E0 = RAIZ / "etapa0"
EV = RAIZ / "work" / "0005-etapa0-medicao" / "evidencias"
WAVDIR = f"/sdcard/Android/data/{ui.PACOTE}/files/wav"
PRESETS = {"conservador": "Conservador", "normal": "Normal", "agil": "Ágil"}
ESTRATEGIAS = {"novo": "Novo por sessão", "reset": "reset()"}
_PAINEL = re.compile(r"beginSession|latência 2:")


def nome_reproducao(oficial, preset, vocab, estrategia, limiar):
    return f"{oficial}_r_{preset}_{vocab}_{estrategia}_l{round(limiar * 1000):04d}"


def agenda_pausas(eventos_csv, deslocamento_oficial):
    """[(t_wav_ms, botão)] das pausas da gravação oficial, no relógio do WAV."""
    saida = []
    for linha in eventos_csv.splitlines()[1:]:
        t, evento, _ = linha.split(",", 2)
        if evento == "pausa_toque":
            saida.append((int(t) + deslocamento_oficial, "Pausar"))
        elif evento == "retoma_toque":
            saida.append((int(t) + deslocamento_oficial, "Ouvir"))
    return saida


def agenda_ciclos(n, inicio_ms=8000, periodo_ms=8000, pausa_ms=2000):
    saida = []
    for k in range(n):
        t = inicio_ms + k * periodo_ms
        saida += [(t, "Pausar"), (t + pausa_ms, "Ouvir")]
    return saida


def duracao_ms(caminho):
    with wave.open(str(caminho)) as w:
        return w.getnframes() * 1000 // w.getframerate()


def coordenadas(*textos):
    lista = ui.dump()
    saida = {}
    for t in textos:
        n = ui.achar(lista, t)
        if n is None:
            raise SystemExit(f"reproduzir.py: '{t}' não está na tela")
        saida[t] = ui.centro(n[2])
    return saida


def tocar_xy(xy):
    ui.adb("shell", "input", "tap", str(xy[0]), str(xy[1]))


def amostrar(destino, captura, rotulo):
    agora = time.time()
    if captura:
        png = ui.TMP / f"painel-{rotulo}-{agora:.0f}.png"
        ui.TMP.mkdir(parents=True, exist_ok=True)
        with open(png, "wb") as f:
            subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=f, check=False)
        destino.write(f"{agora:.3f}\t{rotulo}\tcaptura\t{png}\n")
        return
    try:
        linhas = [n[0] for n in ui.dump() if _PAINEL.search(n[0])]
    except ui.FalhaDump as erro:
        destino.write(f"{agora:.3f}\t{rotulo}\tfalha\t{erro}\n")
        return
    for linha in linhas:
        destino.write(f"{agora:.3f}\t{rotulo}\tpainel\t{linha}\n")


def configurar(preset, vocab, estrategia, limiar, wav_nome):
    ui.tocar("Atualizar WAV")
    passos = [(wav_nome, None), (PRESETS[preset], None), (ESTRATEGIAS[estrategia], None)]
    for texto, _ in passos:
        if ui.tocar(texto) != 0:
            raise SystemExit(f"reproduzir.py: não consegui tocar em '{texto}'")
    if ui.interruptor("Vocabulário restrito", vocab) != 0:
        raise SystemExit("reproduzir.py: vocabulário não ajustado")
    if limiar > 0 and ui.limiar(str(limiar)) != 0:
        raise SystemExit("reproduzir.py: limiar não ajustado")


def main(argv=None):
    p = argparse.ArgumentParser(prog="reproduzir.py")
    p.add_argument("oficial")
    p.add_argument("--preset", required=True, choices=sorted(PRESETS))
    p.add_argument("--vocab", required=True, choices=("on", "off"))
    p.add_argument("--estrategia", required=True, choices=sorted(ESTRATEGIAS))
    p.add_argument("--limiar", type=float, default=0.0)
    p.add_argument("--rodada", default="S1")
    p.add_argument("--painel", action="store_true")
    p.add_argument("--captura", action="store_true")
    p.add_argument("--ciclos", type=int, default=0)
    p.add_argument("--ensaio", help="WAV do ensaio (D21), fora do repositório: sem pausas, exporta fora do repositório")
    p.add_argument("--autoteste", action="store_true")
    args = p.parse_args(argv)
    if args.autoteste:
        assert nome_reproducao("T01_giorgio_sala_t1", "agil", "off", "novo", 0.0) == "T01_giorgio_sala_t1_r_agil_off_novo_l0000"
        assert nome_reproducao("T01_giorgio_sala_t1", "normal", "on", "reset", 0.012) == "T01_giorgio_sala_t1_r_normal_on_reset_l0012"
        ev = "t_ms,evento,detalhe\n5000,pausa_toque,\n15500,retoma_toque,\n20000,toque_palavra,12\n"
        assert agenda_pausas(ev, 1370) == [(6370, "Pausar"), (16870, "Ouvir")]
        assert agenda_ciclos(2) == [(8000, "Pausar"), (10000, "Ouvir"), (16000, "Pausar"), (18000, "Ouvir")]
        print("reproduzir.py autoteste: ok")
        return 0
    oficial = args.oficial
    texto_id = oficial.split("_")[0]
    wav = Path(args.ensaio) if args.ensaio else E0 / "gravacoes" / f"{oficial}.wav"
    sub = "ensaio" if args.ensaio else "reproducoes"
    destino = (ui.TMP / "ensaio") if args.ensaio else (E0 / "trajetorias" / "reproducoes")
    rep = nome_reproducao(oficial, args.preset, args.vocab, args.estrategia, args.limiar)
    if (destino / f"{rep}.trajectory.csv").exists():
        raise SystemExit(f"reproduzir.py: {rep} já existe")
    if args.ensaio:
        agenda = []
    elif args.ciclos:
        agenda = agenda_ciclos(args.ciclos)
    else:
        sinc = json.loads((E0 / "relatorios" / args.rodada / f"{oficial}.sinc.json").read_text(encoding="utf-8"))
        agenda = agenda_pausas((E0 / "eventos" / f"{oficial}.events.csv").read_text(encoding="utf-8"),
                               sinc["deslocamento_ms"])
    if "wav" not in ui.adb("shell", "ls", f"{WAVDIR}/{oficial}.wav"):
        ui.adb("push", str(wav), f"{WAVDIR}/{oficial}.wav")
    ui.adb("shell", "am", "start", "-n", f"{ui.PACOTE}/.MainActivity")
    time.sleep(1)
    if ui.chip(texto_id) != 0:
        raise SystemExit(1)
    ui.tocar("Depuração")
    configurar(args.preset, args.vocab, args.estrategia, args.limiar, f"{oficial}.wav")
    if not args.painel:
        ui.tocar("Depuração")
    xy = coordenadas("Ouvir", "Pausar")
    EV.joinpath("painel").mkdir(parents=True, exist_ok=True)
    fim_ms = (args.ciclos * 8000 + 10000) if args.ciclos else duracao_ms(wav) + 3000
    with open(EV / "painel" / f"{rep}.tsv", "a", encoding="utf-8") as painel:
        tocar_xy(xy["Ouvir"])
        t0 = time.monotonic()
        pendentes = list(agenda)
        while (time.monotonic() - t0) * 1000 < fim_ms:
            agora_ms = (time.monotonic() - t0) * 1000
            if pendentes and agora_ms >= pendentes[0][0]:
                t, botao = pendentes.pop(0)
                tocar_xy(xy[botao])
                painel.write(f"{time.time():.3f}\ttoque\t{botao}\t{t}\n")
                if args.ciclos and botao == "Ouvir":
                    time.sleep(1.5)
                    amostrar(painel, args.captura, f"ciclo-{len(agenda) - len(pendentes)}")
                continue
            if args.painel and not args.ciclos:
                amostrar(painel, args.captura, "leitura")
            else:
                time.sleep(0.05)
    subprocess.run([str(Path(__file__).parent / "take.sh"), "fechar", rep, sub], check=True)
    meta = (destino / f"{rep}.meta.txt").read_text(encoding="utf-8")
    esperado = {"audio_source": "wav", "wav_name": f"{oficial}.wav", "vocabulary": args.vocab,
                "recognizer_strategy": "NewPerSession" if args.estrategia == "novo" else "Reset",
                "preset": {"conservador": "Conservador", "normal": "Normal", "agil": "Agil"}[args.preset]}
    erros = [f"{k}={v}" for k, v in esperado.items() if f"{k}={v}" not in meta.splitlines()]
    if erros:
        raise SystemExit(f"reproduzir.py: PARAR: meta.txt de {rep} não tem {', '.join(erros)}")
    print(f"reproduzido: {rep}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
