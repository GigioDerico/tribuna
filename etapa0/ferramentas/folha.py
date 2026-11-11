"""folha.py <texto> <expected.csv> <words.csv> --deslocamento-ms D [-o SAIDA]: folha de eventos derivada dos dados.

spec 0005, D6, D7 e critério D5; impl_plan 0005, A4.

Para os textos com evento pela voz (T04, T05, T06 e L30), acha no expected.csv e na transcrição (words.csv,
relógio do WAV) as fronteiras do improviso, do silêncio e do pulo marcados pelas notas // do texto, e escreve
<nome>.folha.events.csv (t_ms,evento,detalhe) no relógio do app: t_app = t_wav − deslocamento. Pausa e toque em
palavra vêm do events.csv do app e não entram aqui.

Regras (impl_plan A4):
- improviso: da primeira à última palavra da transcrição entre o fim do último token com tempo antes da
  fronteira e o início do primeiro token com tempo depois dela;
- silêncio: do fim do último token com tempo antes da fronteira ao início do primeiro com tempo depois;
- pulo: pulo_inicio no fim do último token com tempo antes do parágrafo pulado, com o índice do primeiro token
  pulado; pulo_fim no início do primeiro token com tempo depois dele.
No stderr: um ponto de escuta por fronteira (D7) e os avisos.
Códigos de saída: 0 folha escrita; 1 folha escrita com aviso (fronteira em token sem tempo, palavra dentro do
silêncio, token do parágrafo pulado com tempo): os avisos vão para a revisão; 2 entrada inválida.
"""

import argparse
import csv
import io
import sys
from dataclasses import dataclass

import alinhar
import metricas
import roteiro

CABECALHO = ["t_ms", "evento", "detalhe"]

# (evento, começo da linha da nota //, onde fica a fronteira)
REGRAS = {
    "T04": (("improviso", "// IMPROVISAR", "na_nota"),),
    "T05": (("pulo", "// PULAR", "paragrafo_seguinte"),),
    "T06": (("silencio", "// SILÊNCIO", "na_nota"),),
    "L30": (
        ("improviso", "// L30 — Minuto 8 (", "fim_do_paragrafo_seguinte"),
        ("pulo", "// L30 — Minuto 15 (", "paragrafo_seguinte"),
        ("silencio", "// L30 — Minuto 18 (", "fim_do_paragrafo_seguinte"),
    ),
}


class ErroEntrada(Exception):
    """Código de saída 2."""


@dataclass(frozen=True)
class Bloco:
    ini: int  # índice do primeiro caractere
    fim: int  # índice depois do último caractere
    nota: bool
    texto: str


def blocos(texto):
    """Linhas não vazias seguidas formam um bloco; bloco que começa com // é nota."""
    saida, atual, pos = [], None, 0
    for linha in texto.split("\n"):
        if linha.strip():
            if atual is None:
                atual = [pos, pos + len(linha), linha.strip()]
            else:
                atual[1] = pos + len(linha)
                atual[2] += "\n" + linha.strip()
        elif atual is not None:
            saida.append(Bloco(atual[0], atual[1], atual[2].startswith("//"), atual[2]))
            atual = None
        pos += len(linha) + 1
    if atual is not None:
        saida.append(Bloco(atual[0], atual[1], atual[2].startswith("//"), atual[2]))
    return saida


def fronteiras(texto_id, texto, tokens):
    """Lista de (evento, último token antes, primeiro token depois, primeiro token pulado ou None)."""
    lista = blocos(texto)

    def antes(pos):
        anteriores = [t.indice for t in tokens if t.inicio < pos]
        return anteriores[-1] if anteriores else None

    def depois(pos):
        seguintes = [t.indice for t in tokens if t.inicio >= pos]
        return seguintes[0] if seguintes else None

    saida = []
    for evento, prefixo, onde in REGRAS[texto_id]:
        notas = [k for k, b in enumerate(lista) if b.nota and b.texto.startswith(prefixo)]
        if len(notas) != 1:
            raise ErroEntrada(f"{texto_id}: a nota '{prefixo}' aparece {len(notas)} vez(es); esperado 1")
        k = notas[0]
        seguinte = next((b for b in lista[k + 1:] if not b.nota), None)
        pulado = None
        if onde == "na_nota":
            a, b = antes(lista[k].ini), depois(lista[k].fim)
        elif seguinte is None:
            raise ErroEntrada(f"{texto_id}: sem parágrafo depois da nota '{prefixo}'")
        elif onde == "fim_do_paragrafo_seguinte":
            a, b = antes(seguinte.fim), depois(seguinte.fim)
        else:  # paragrafo_seguinte: o parágrafo inteiro é pulado
            a, b, pulado = antes(seguinte.ini), depois(seguinte.fim), depois(seguinte.ini)
        if a is None or b is None:
            raise ErroEntrada(f"{texto_id}: a nota '{prefixo}' não fica entre dois tokens")
        saida.append((evento, a, b, pulado))
    return saida


def _com_tempo_antes(esperado, i):
    for k in range(i, -1, -1):
        if esperado[k].fim is not None:
            return k
    return None


def _com_tempo_depois(esperado, i):
    for k in range(i, len(esperado)):
        if esperado[k].fim is not None:
            return k
    return None


def derivar(texto_id, texto, tokens, esperado, transcricao, deslocamento):
    """Devolve (eventos [(t_app, nome, detalhe)], pontos de escuta, avisos). Lança ErroEntrada."""
    eventos, pontos, avisos = [], [], []

    def exibido(i):
        t = tokens[i]
        return texto[t.inicio:t.fim + 1]

    def marcar(t_wav, nome, detalhe, contexto):
        t_app = t_wav - deslocamento
        if t_app < 0:
            raise ErroEntrada(f"{nome} cairia antes do início da gravação do app ({t_app} ms): deslocamento errado?")
        eventos.append((t_app, nome, detalhe))
        pontos.append(f"revisar: {nome} em {t_wav} ms do WAV ({t_app} ms do app), {contexto}")

    for evento, a, b, pulado in fronteiras(texto_id, texto, tokens):
        ka, kb = _com_tempo_antes(esperado, a), _com_tempo_depois(esperado, b)
        if ka is None or kb is None:
            raise ErroEntrada(f"{evento}: nenhum token com tempo antes do token {a} ou depois do token {b}")
        for pedido, achado in ((a, ka), (b, kb)):
            if pedido != achado:
                avisos.append(f"aviso: {evento}: o token {pedido} ('{exibido(pedido)}') não tem tempo; "
                              f"a fronteira usa o token {achado} ('{exibido(achado)}')")
        fim_antes, inicio_depois = esperado[ka].fim, esperado[kb].inicio
        if inicio_depois < fim_antes:
            raise ErroEntrada(f"{evento}: o token {kb} começa antes do fim do token {ka}")
        perto = f"entre '{exibido(ka)}' (token {ka}) e '{exibido(kb)}' (token {kb})"
        if evento == "improviso":
            dentro = [p for p in transcricao if p.inicio >= fim_antes and p.fim <= inicio_depois]
            if not dentro:
                raise ErroEntrada(f"improviso: nenhuma palavra da transcrição {perto}")
            marcar(dentro[0].inicio, "improviso_inicio", "", f"primeira palavra do improviso, {perto}")
            marcar(dentro[-1].fim, "improviso_fim", "", f"última palavra do improviso, {perto}")
        elif evento == "silencio":
            dentro = [p for p in transcricao if p.inicio > fim_antes and p.fim < inicio_depois]
            if dentro:
                avisos.append(f"aviso: silencio: {len(dentro)} palavra(s) da transcrição dentro do silêncio, {perto}")
            marcar(fim_antes, "silencio_inicio", "", f"fim de '{exibido(ka)}' (token {ka})")
            marcar(inicio_depois, "silencio_fim", "", f"início de '{exibido(kb)}' (token {kb})")
        else:
            falados = [i for i in range(pulado, b) if esperado[i].fim is not None]
            if falados:
                avisos.append(f"aviso: pulo: {len(falados)} token(s) do parágrafo pulado com tempo "
                              f"(primeiro: token {falados[0]})")
            marcar(fim_antes, "pulo_inicio", str(pulado), f"fim de '{exibido(ka)}' (token {ka}); "
                                                         f"primeiro pulado: token {pulado}")
            marcar(inicio_depois, "pulo_fim", "", f"início de '{exibido(kb)}' (token {kb})")
    return eventos, pontos, avisos  # em ordem: as regras de cada texto seguem a ordem do texto


def folha_csv(eventos):
    saida = io.StringIO()
    w = csv.writer(saida, lineterminator="\n")
    w.writerow(CABECALHO)
    for t, nome, detalhe in eventos:
        w.writerow([t, nome, detalhe])
    return saida.getvalue()


def executar(conteudo_texto, conteudo_esperado, conteudo_transcricao, deslocamento):
    texto_id = roteiro.identificar_texto(conteudo_texto)
    if texto_id is None:
        raise ErroEntrada("o texto não é idêntico a nenhum de etapa0/textos/")
    if texto_id not in REGRAS:
        raise ErroEntrada(f"{texto_id} não tem evento pela voz; folha só para {', '.join(REGRAS)}")
    texto = conteudo_texto.decode("utf-8")
    tokens = roteiro.tokenizar(texto)
    try:
        esperado, palavras = metricas.ler_esperado(conteudo_esperado)
        transcricao = alinhar.ler_transcricao(conteudo_transcricao)
    except (metricas.ErroEntrada, alinhar.ErroEntrada) as erro:
        raise ErroEntrada(str(erro)) from None
    if palavras != [roteiro.normalizar(texto[t.inicio:t.fim + 1]) for t in tokens]:
        raise ErroEntrada(f"o expected.csv não é do texto {texto_id}")
    eventos, pontos, avisos = derivar(texto_id, texto, tokens, esperado, transcricao, deslocamento)
    return folha_csv(eventos), pontos, avisos


def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="folha.py",
        description="Deriva a folha de eventos (improviso, silêncio, pulo) do expected.csv e da transcrição.",
    )
    parser.add_argument("texto", help="texto de teste com eventos pela voz (T04, T05, T06 ou L30), idêntico ao de etapa0/textos/")
    parser.add_argument("esperado", help="<nome>.expected.csv (relógio do WAV)")
    parser.add_argument("transcricao", help="<nome>.words.csv, saída do timestamps.py (relógio do WAV)")
    parser.add_argument("--deslocamento-ms", type=int, required=True,
                        help="o do sincronizar.py (t_wav = t_app + d); a folha sai no relógio do app")
    parser.add_argument("-o", "--saida", help="arquivo da folha (padrão: stdout)")
    args = parser.parse_args(argv)
    try:
        with open(args.texto, "rb") as f:
            conteudo_texto = f.read()
        with open(args.esperado, encoding="utf-8", newline="") as f:
            conteudo_esperado = f.read()
        with open(args.transcricao, encoding="utf-8", newline="") as f:
            conteudo_transcricao = f.read()
        texto_folha, pontos, avisos = executar(conteudo_texto, conteudo_esperado, conteudo_transcricao,
                                               args.deslocamento_ms)
    except (OSError, UnicodeDecodeError, ErroEntrada) as erro:
        print(f"folha.py: {erro}", file=sys.stderr)
        return 2
    sys.stderr.write("".join(f"{linha}\n" for linha in pontos + avisos))
    if args.saida:
        with open(args.saida, "w", encoding="utf-8", newline="") as f:
            f.write(texto_folha)
    else:
        sys.stdout.write(texto_folha)
    return 1 if avisos else 0


if __name__ == "__main__":
    sys.exit(main())
