"""timestamps.py <wav> --modelo DIR [-o SAIDA]: tempo de cada palavra de uma gravação de teste.

TESTES-ETAPA0, "Trajetória esperada", passo 1; spec 0004, D5.

Saída: CSV palavra,t_inicio_ms,t_fim_ms,conf, na ordem do tempo, com a palavra exatamente como o Vosk
devolveu (golden bruto). Só lê o WAV: nenhum áudio é escrito (CLAUDE.md regra 3).

Guardas: WAV 16 kHz, mono, PCM 16 bits; nome <ID>_<voz>_<ambiente>_t<n>.wav com <ID> de etapa0/textos/;
modelo só por caminho (--modelo), nunca por nome ou idioma, que baixariam da internet (D5, D21).
O pacote vosk é importado só no modo real, dentro de _fabrica_vosk (A2 do spec).
Códigos de saída: 0 ok; 2 entrada inválida; 3 dependência ausente.
"""

import argparse
import csv
import io
import json
import re
import sys
import wave
from pathlib import Path

import roteiro

CABECALHO = ["palavra", "t_inicio_ms", "t_fim_ms", "conf"]
TAXA = 16000
QUADROS_POR_BLOCO = 4000
_NOME = re.compile(r"(?P<id>[A-Z0-9]+)_[A-Za-z0-9]+_[A-Za-z0-9]+_t[0-9]+\.wav")


class ErroEntrada(Exception):
    """Código de saída 2."""


class DependenciaAusente(Exception):
    """Código de saída 3."""


def validar_nome(caminho):
    m = _NOME.fullmatch(Path(caminho).name)
    if m is None or m.group("id") not in roteiro.IDS:
        raise ErroEntrada("nome fora do padrão <ID>_<voz>_<ambiente>_t<n>.wav com ID de etapa0/textos/")


def blocos_do_wav(caminho):
    """Valida o formato e devolve a lista de blocos de bytes PCM, na ordem."""
    try:
        with wave.open(str(caminho), "rb") as w:
            if w.getcomptype() != "NONE" or w.getnchannels() != 1 or w.getsampwidth() != 2 or w.getframerate() != TAXA:
                raise ErroEntrada("o WAV precisa ser 16 kHz, mono, PCM 16 bits")
            blocos = []
            while True:
                dados = w.readframes(QUADROS_POR_BLOCO)
                if not dados:
                    break
                blocos.append(dados)
            return blocos
    except (wave.Error, EOFError, OSError) as erro:
        raise ErroEntrada(f"WAV ilegível: {erro}") from None


def _palavras_do_json(texto_json):
    return json.loads(texto_json).get("result", [])


def transcrever(blocos, reconhecedor):
    """Laço WAV -> reconhecedor -> palavras (cada uma um dict word/start/end/conf), em ordem de tempo."""
    palavras = []
    for dados in blocos:
        if reconhecedor.AcceptWaveform(dados):
            palavras += _palavras_do_json(reconhecedor.Result())
    palavras += _palavras_do_json(reconhecedor.FinalResult())
    return sorted(palavras, key=lambda p: (p["start"], p["end"]))  # sorted é estável


def para_csv(palavras):
    saida = io.StringIO()
    w = csv.writer(saida, lineterminator="\n")
    w.writerow(CABECALHO)
    for p in palavras:
        conf = p.get("conf")
        w.writerow([p["word"], round(p["start"] * 1000), round(p["end"] * 1000), "" if conf is None else conf])
    return saida.getvalue()


def _fabrica_vosk(modelo):
    """Modo real: abre o modelo pelo caminho e devolve uma função taxa -> reconhecedor com SetWords(True)."""
    try:
        import vosk  # só aqui (spec 0004, A2): os testes não dependem do pacote
    except ImportError:
        raise DependenciaAusente(
            "o pacote vosk não está instalado; instalar é portão humano 6 (0005), fora desta ferramenta"
        ) from None
    vosk.SetLogLevel(-1)  # sem o log do Kaldi no stderr
    try:
        model = vosk.Model(model_path=str(modelo))  # só por caminho: sem download (D5, D21)
    except Exception as erro:  # o pacote lança Exception("Failed to create a model")
        raise ErroEntrada(f"o modelo não abriu: {erro}") from None

    def criar(taxa):
        reconhecedor = vosk.KaldiRecognizer(model, taxa)
        reconhecedor.SetWords(True)
        return reconhecedor

    return criar


def main(argv=None, fabrica=None):
    """`fabrica` (taxa -> reconhecedor) só é passada pelos testes; sem ela, modo real com o Vosk."""
    parser = argparse.ArgumentParser(
        prog="timestamps.py",
        description="Transcreve uma gravação de texto de teste com tempos por palavra (Vosk, SetWords).",
    )
    parser.add_argument("wav", help="WAV 16 kHz mono PCM 16 bits, nome <ID>_<voz>_<ambiente>_t<n>.wav")
    parser.add_argument("--modelo", help="diretório do modelo Vosk (obrigatório; nunca baixado)")
    parser.add_argument("-o", "--saida", help="arquivo do CSV (padrão: stdout)")
    args = parser.parse_args(argv)
    try:
        validar_nome(args.wav)
        blocos = blocos_do_wav(args.wav)
        if fabrica is None:
            if not args.modelo:
                raise ErroEntrada("faltou --modelo <diretório do modelo>")
            if not Path(args.modelo).is_dir():
                raise ErroEntrada("--modelo não é um diretório")
            fabrica = _fabrica_vosk(args.modelo)
        texto = para_csv(transcrever(blocos, fabrica(TAXA)))
    except ErroEntrada as erro:
        print(f"timestamps.py: {erro}", file=sys.stderr)
        return 2
    except DependenciaAusente as erro:
        print(f"timestamps.py: {erro}", file=sys.stderr)
        return 3
    if args.saida:
        with open(args.saida, "w", encoding="utf-8", newline="") as f:
            f.write(texto)
    else:
        sys.stdout.write(texto)
    return 0


if __name__ == "__main__":
    sys.exit(main())
