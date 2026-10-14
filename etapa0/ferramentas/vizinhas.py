"""vizinhas.py <nome>.folha.events.csv: a mesma folha nos relógios das corridas d − 200 e d + 200.

spec 0005, D3 e E1 (esclarecimento da revisão, rodada 1); impl_plan 0005, Correções da revisão rodada 1, C1.

A folha (folha.py) é medida no relógio do WAV e gravada no relógio do app com o deslocamento d da
sincronização: t_app = t_wav − d. O metricas.py soma o deslocamento da corrida a todos os eventos. Para a folha
ficar parada no relógio do WAV, onde foi medida, enquanto a trajetória, as hipóteses e o events.csv do app andam
200 ms, cada corrida vizinha precisa da folha com o próprio deslocamento:
  corrida d − 200: t = t_wav − (d − 200) = t_d + 200  ->  <nome>.m200.folha.events.csv
  corrida d + 200: t = t_wav − (d + 200) = t_d − 200  ->  <nome>.p200.folha.events.csv
As duas saem da folha de d como ela está (com as correções da revisão, D7), na mesma pasta, e são sobrescritas
a cada execução: são derivadas, não se editam.
Códigos de saída: 0 as duas escritas; 2 entrada inválida (nome fora do padrão, folha inválida, tempo negativo).
"""

import argparse
import sys
from pathlib import Path

import folha
import metricas

TOLERANCIA_MS = 200  # spec D3
SUFIXO = ".folha.events.csv"
CORRIDAS = {"m200": TOLERANCIA_MS, "p200": -TOLERANCIA_MS}  # soma ao t da folha de d


class ErroEntrada(Exception):
    """Código de saída 2."""


def deslocar(conteudo, ms):
    """Folha de d (texto CSV) -> mesma folha com ms somado a cada t_ms, no formato do folha.py."""
    try:
        eventos = metricas.ler_eventos([conteudo], 0, 10**9)
        metricas.intervalos(eventos)
    except metricas.ErroEntrada as erro:
        raise ErroEntrada(str(erro)) from None
    saida = [(ev.t + ms, ev.nome, ev.detalhe) for ev in eventos]
    negativos = [t for t, _, _ in saida if t < 0]
    if negativos:
        raise ErroEntrada(f"t_ms {negativos[0]} negativo com {ms:+d} ms: a folha de d começa antes de {-ms} ms")
    return folha.folha_csv(saida)


def caminhos(entrada):
    """<pasta>/<nome>.folha.events.csv -> {corrida: <pasta>/<nome>.<corrida>.folha.events.csv}."""
    entrada = Path(entrada)
    if not entrada.name.endswith(SUFIXO):
        raise ErroEntrada(f"{entrada.name}: o nome tem de terminar em {SUFIXO}")
    nome = entrada.name[: -len(SUFIXO)]
    if not nome or nome.endswith(tuple(f".{c}" for c in CORRIDAS)):
        raise ErroEntrada(f"{entrada.name}: é a folha de uma corrida vizinha; passe a folha de d")
    return {c: entrada.with_name(f"{nome}.{c}{SUFIXO}") for c in CORRIDAS}


def main(argv=None):
    parser = argparse.ArgumentParser(prog="vizinhas.py", description="Escreve as folhas das corridas d − 200 e "
                                     "d + 200 a partir da folha de d (spec 0005, D3, E1).")
    parser.add_argument("folha", help="<nome>.folha.events.csv, a folha de d (relógio do app com o d do sincronizar)")
    args = parser.parse_args(argv)
    try:
        saidas = caminhos(args.folha)
        with open(args.folha, encoding="utf-8", newline="") as f:
            conteudo = f.read()
        textos = {c: deslocar(conteudo, ms) for c, ms in CORRIDAS.items()}
    except (OSError, UnicodeDecodeError, ErroEntrada) as erro:
        print(f"vizinhas.py: {erro}", file=sys.stderr)
        return 2
    for c, caminho in saidas.items():
        with open(caminho, "w", encoding="utf-8", newline="") as f:
            f.write(textos[c])
        print(f"{caminho.name}: {CORRIDAS[c]:+d} ms")
    return 0


if __name__ == "__main__":
    sys.exit(main())
