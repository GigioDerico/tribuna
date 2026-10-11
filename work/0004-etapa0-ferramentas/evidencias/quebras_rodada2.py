"""Refaz as 40 quebras que ficaram verdes na revisão (code_review.md, rodada 1) contra os testes da rodada 2.

Uso, da raiz do repositório:  python3 work/0004-etapa0-ferramentas/evidencias/quebras_rodada2.py
Aplica uma troca por vez no arquivo, roda testar.py, restaura com git checkout e imprime uma linha TSV por quebra:
id, arquivo:linha, troca, resultado, testes vermelhos, primeiro teste vermelho. Termina com a árvore limpa.
"""
import re
import subprocess
import sys
from pathlib import Path

FERR = Path("etapa0/ferramentas")
# (id, arquivo, linha, trecho original na linha, trecho quebrado)
Q = [
    ("X01", "metricas.py", 368, "<= ATRASO_MAX:", "< ATRASO_MAX:"),
    ("X02", "metricas.py", 368, "<= ATRASO_MAX:", "<= ATRASO_MAX - 1:"),
    ("X03", "metricas.py", 368, "<= ATRASO_MAX:", "<= ATRASO_MAX + 1:"),
    ("X10", "metricas.py", 281, "_salto(amostras", "_salto(normais"),
    ("X06", "metricas.py", 375, "elif all(", "elif any("),
    ("X13", "metricas.py", 338, "not in AUTOMATICOS", '== "Stay"'),
    ("X51", "metricas.py", 254, "e.fim is not None and ", ""),
    ("Z10", "timestamps.py", 63, '.get("result", [])', '.get("result", [])[:2]'),
    ("Z11", "timestamps.py", 71, "palavras += ", "palavras = "),
    ("X12", "metricas.py", 347, "<= congelado", "<= congelado + 1"),
    ("X80", "metricas.py", 410, " or _dentro(e.fim, faixas_toque)", ""),
    ("X70", "metricas.py", 516, "if args.hipoteses and formas is None:", "if False:"),
    ("X84", "roteiro.py", 359, "if [normalizar", "if True or [normalizar"),
    ("X60", "metricas.py", 155, "not 0 <= indice < n_tokens", "indice >= n_tokens"),
    ("X61", "metricas.py", 118, "if linha.cursor < 0:", "if False:"),
    ("X62", "metricas.py", 114, "float(score)", "pass"),
    ("X63", "metricas.py", 139, "if b < a:", "if False:"),
    ("X64", "metricas.py", 135, 'if ini == "" and fim == "":', 'if ini == "":'),
    ("X65", "metricas.py", 165, 'if kind not in ("partial", "final", "error") or aceita not in ("0", "1"):', "if False:"),
    ("X69", "metricas.py", 96, "!= len(cabecalho)", "< len(cabecalho)"),
    ("X71", "metricas.py", 539, "except (OSError, UnicodeDecodeError, ErroEntrada)", "except ErroEntrada"),
    ("Y09", "alinhar.py", 59, "inicio < 0 or ", ""),
    ("Y10", "alinhar.py", 56, "float(campos[3])", "pass"),
    ("Y12", "alinhar.py", 111, "if (len(linhas) + m + 1) * (peso + 1) >= _INF // 2:", "if False:"),
    ("Y15", "alinhar.py", 61, "inicio < anterior", "fim < anterior"),
    ("Y08", "alinhar.py", 147, "if estado == _I:", "if False:"),
    ("Y07", "alinhar.py", 142, "min(_PRIORIDADE,", "min((_M, _I, _D),"),
    ("Z01", "timestamps.py", 40, "fullmatch", "match"),
    ("Z05", "timestamps.py", 58, "except (wave.Error, EOFError, OSError)", "except ZeroDivisionError"),
    ("X21", "metricas.py", 311, "* len(atrasos)", "* (len(atrasos) - 1)"),
    ("X31", "metricas.py", 269, "ultima + 1", "ultima"),
    ("X30", "metricas.py", 266, "max(0, self.trajetoria[0].t)", "self.trajetoria[0].t"),
    ("X20", "metricas.py", 310, "a <= ATRASO_MAX", "abs(a) <= ATRASO_MAX"),
    ("X08", "metricas.py", 365, "if t < fim:", "if t < inicio:"),
    ("X40", "metricas.py", 386, "elif linha.move in AUTOMATICOS:", "else:"),
    ("X50", "metricas.py", 251, "linha.t <= ev.t", "linha.t < ev.t"),
    ("Y02", "alinhar.py", 91, "y + abre", "y + estende"),
    ("Y03", "alinhar.py", 95, "lm[j - 1] if lm[j - 1] < ld[j - 1] else ld[j - 1]", "lm[j - 1]"),
    ("Z09", "timestamps.py", 73, '(p["start"], p["end"])', '(p["end"], p["start"])'),
    ("W03", "roteiro.py", 31, "[-'’]", "[-']"),
]


def roda():
    r = subprocess.run([sys.executable, str(FERR / "testar.py")], capture_output=True, text=True)
    saida = r.stdout + r.stderr
    falhas = re.findall(r"^(?:FAIL|ERROR): (\S+)", saida, re.M)
    return r.returncode, falhas


print("id\tarquivo:linha\ttroca\tresultado\ttestes vermelhos\tprimeiro teste vermelho")
for qid, arq, linha, antes, depois in Q:
    caminho = FERR / arq
    linhas = caminho.read_text(encoding="utf-8").split("\n")
    assert antes in linhas[linha - 1], (qid, linhas[linha - 1])
    original = linhas[linha - 1]
    linhas[linha - 1] = original.replace(antes, depois, 1)
    caminho.write_text("\n".join(linhas), encoding="utf-8")
    try:
        rc, falhas = roda()
    finally:
        subprocess.run(["git", "checkout", "--", str(caminho)], check=True)
    estado = "VERDE" if rc == 0 else "VERMELHO"
    print(f"{qid}\t{arq}:{linha}\t{antes} => {depois}\t{estado}\t{len(falhas)}\t{falhas[0] if falhas else ''}", flush=True)
