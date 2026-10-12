#!/usr/bin/env python3
"""ensaio.py montar <ID> | concatenar <saida> <wav>...: áudio sintético do ensaio (D21) e o WAV longo da bateria (D14).

montar: `say` com a voz pt-BR Luciana lê etapa0/textos/<ID>.txt; o afconvert passa a 16 kHz, mono, PCM 16 bits; na
frente entram 2 s de silêncio, uma palma sintética de 20 ms e mais 2 s (o roteiro do humano). Sai em
$TMPDIR/tribuna-0005/ensaio/<ID>_ensaio_sala_t1.wav, FORA do repositório (o ensaio não conta para critério nenhum).
concatenar: junta WAVs de gravacoes/ num só (bateria, D14), também fora do repositório.
Nada aqui grava voz de ninguém: o `say` é voz sintética e a concatenação só copia WAVs de texto de teste.
"""

import os
import subprocess
import sys
import tempfile
import wave
from array import array
from pathlib import Path

TMP = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "tribuna-0005" / "ensaio"
RAIZ = Path(subprocess.run(["git", "-C", str(Path(__file__).parent), "rev-parse", "--show-toplevel"],
                           capture_output=True, text=True, check=True).stdout.strip())


def montar(texto_id):
    TMP.mkdir(parents=True, exist_ok=True)
    aiff, fala = TMP / f"{texto_id}.aiff", TMP / f"{texto_id}.fala.wav"
    subprocess.run(["say", "-v", "Luciana", "-f", str(RAIZ / "etapa0" / "textos" / f"{texto_id}.txt"), "-o", str(aiff)],
                   check=True)
    subprocess.run(["afconvert", "-f", "WAVE", "-d", "LEI16@16000", "-c", "1", "--mix", str(aiff), str(fala)], check=True)
    with wave.open(str(fala)) as w:
        amostras = array("h")
        amostras.frombytes(w.readframes(w.getnframes()))
    palma = array("h", [30000 if k % 2 == 0 else -30000 for k in range(320)])
    tudo = array("h", [0] * 32000) + palma + array("h", [0] * (32000 - 320)) + amostras
    saida = TMP / f"{texto_id}_ensaio_sala_t1.wav"
    with wave.open(str(saida), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(16000)
        w.writeframes(tudo.tobytes())
    aiff.unlink()
    fala.unlink()
    print(f"{saida}\t{len(tudo) / 16000:.1f} s; palma em 2000 ms")
    return 0


def concatenar(saida, wavs):
    with wave.open(str(saida), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(16000)
        total = 0
        for caminho in wavs:
            with wave.open(str(caminho)) as w:
                assert (w.getnchannels(), w.getsampwidth(), w.getframerate()) == (1, 2, 16000), caminho
                out.writeframes(w.readframes(w.getnframes()))
                total += w.getnframes()
    print(f"{saida}\t{total / 16000 / 60:.1f} min")
    return 0


if __name__ == "__main__":
    if sys.argv[1:2] == ["montar"]:
        sys.exit(montar(sys.argv[2]))
    if sys.argv[1:2] == ["concatenar"]:
        sys.exit(concatenar(sys.argv[2], sys.argv[3:]))
    print(__doc__)
    sys.exit(2)
