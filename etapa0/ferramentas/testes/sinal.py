"""Sinais sintéticos para o sincronizar.py (spec 0005, D3): ruído em degraus, como sílabas e pausas, mais uma palma.

Nada aqui é voz nem gravação: amostras pseudoaleatórias com semente fixa, escritas só em pasta temporária.
"""

import math
import random
import wave

TAXA = 16000


def fala_sintetica(duracao_ms, semente, palma_ms=None, silencio_ate_ms=0):
    """Amostras int16. Trechos de 50 a 400 ms com amplitude de 0 (pausa) a 8.000; palma de 20 ms a 30.000.

    Até `silencio_ate_ms` só há silêncio (o tempo antes da leitura), salvo a palma.
    """
    rnd = random.Random(semente)
    amostras = []
    total = duracao_ms * TAXA // 1000
    while len(amostras) < total:
        trecho = rnd.randint(5, 40) * 160
        ms = len(amostras) * 1000 // TAXA
        amp = 0 if ms < silencio_ate_ms or rnd.random() < 0.3 else rnd.randint(500, 8000)
        amostras += [rnd.randint(-amp, amp) for _ in range(trecho)] if amp else [0] * trecho
    amostras = amostras[:total]
    if palma_ms is not None:
        i = palma_ms * TAXA // 1000
        for k in range(i, min(i + 320, total)):
            amostras[k] = 30000 if (k - i) % 2 == 0 else -30000
    return amostras


def escrever_wav(caminho, amostras, canais=1, taxa=TAXA):
    with wave.open(str(caminho), "wb") as w:
        w.setnchannels(canais)
        w.setsampwidth(2)
        w.setframerate(taxa)
        dados = bytearray()
        for a in amostras:
            for _ in range(canais):
                dados += int(a).to_bytes(2, "little", signed=True)
        w.writeframes(bytes(dados))
    return str(caminho)


def _rms(amostras, fim_amostra):
    """RMS normalizado do bloco de 3.200 amostras que termina em `fim_amostra` (fora do sinal = zero)."""
    ini = fim_amostra - 3200
    soma = 0
    for k in range(max(ini, 0), min(fim_amostra, len(amostras))):
        soma += amostras[k] * amostras[k]
    return math.sqrt(soma / 3200) / 32768


def level_csv(amostras, deslocamento_ms, ate_app_ms, ganho=1.0, piso=0.0, deriva_ppm=0, semente=7,
              extras=None, constante_depois_de_ms=None, salto=None):
    """level.csv como o app gravaria: linha a cada ~200 ms (jitter 0 a 30 ms) de 200 ms até `ate_app_ms`.

    Bloco da linha t_app = áudio do WAV em [t_wav − 200, t_wav), t_wav = t_app × (1 + ppm) + deslocamento.
    `extras`: {t_app: rms} trocados à mão (um pico só do aparelho). `constante_depois_de_ms`: microfone mudo.
    `salto`: (t_app, ms): depois de t_app o relógio do WAV pula ms (um corte na gravação de referência).
    """
    rnd = random.Random(semente)
    linhas = ["t_ms,rms"]
    t = 200
    while t <= ate_app_ms:
        t_app = t + rnd.randint(0, 30)
        t_wav = t_app * (1 + deriva_ppm / 1_000_000) + deslocamento_ms
        if salto and t_app > salto[0]:
            t_wav += salto[1]
        rms = min(1.0, ganho * _rms(amostras, round(t_wav * TAXA / 1000)) + piso)
        if constante_depois_de_ms is not None and t_app > constante_depois_de_ms:
            rms = 0.01
        if extras and t in extras:
            rms = extras[t]
        linhas.append(f"{t_app},{rms:.6f}")
        t += 200
    return "\n".join(linhas) + "\n"
