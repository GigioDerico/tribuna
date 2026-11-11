#!/usr/bin/env python3
"""ui.py: dirige a tela da etapa0 por adb + uiautomator (impl_plan 0005, A8). Só stdlib; não muda número nenhum.

Uso (ANDROID_SERIAL no ambiente):
  ui.py tocar <texto>            toca no nó com esse texto exato (procura rolando o painel até 6 vezes)
  ui.py chip <ID>                toca no chip do texto de teste (rola a fileira de chips se preciso)
  ui.py campo <atual> <novo>     troca o texto do campo editável que hoje mostra <atual>
  ui.py tem <texto>              sai com 0 se o texto exato está na tela, 1 se não
  ui.py linhas <regex>           imprime os textos da tela que casam com a regex (números do painel)
  ui.py esperar <texto> <seg>    espera o texto aparecer (um dump a cada volta)
  ui.py marcado <texto>          imprime true/false: checked ou selected do nó (ou do pai clicável) com esse texto
  ui.py interruptor <rótulo> on|off   liga ou desliga o Switch da mesma linha do rótulo e confere
  ui.py esperar-habilitado <texto> <seg>   espera o nó com esse texto ficar habilitado (botão)
  ui.py limiar <valor>           põe o slider do limiar em [valor, valor + 0,002] e imprime o valor do rótulo
  ui.py tem-sistema <regex>      sai com 0 se algum nó de outro pacote (diálogo do sistema) casa com a regex
  ui.py tocar-sistema <texto>    toca no nó de outro pacote com esse texto exato (botão do diálogo de permissão)
  ui.py autoteste                testa o leitor de XML com um dump de exemplo (sem aparelho)

Os dumps ficam em $TMPDIR/tribuna-0005/ (fora do repositório) e são apagados no fim de cada chamada: podem ter
texto de notificação do sistema (Q6). Na evidência entram só as linhas pedidas.
Códigos: 0 ok; 1 não achou; 2 uiautomator falhou (por exemplo, "could not get idle state").
"""

import os
import re
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACOTE = "io.github.gigioderico.tribuna.etapa0"
TMP = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "tribuna-0005"
_BOUNDS = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")


class FalhaDump(Exception):
    pass


def adb(*args, capturar=True):
    r = subprocess.run(["adb", *args], capture_output=capturar, text=True, timeout=60)
    return r.stdout if capturar else ""


def nos_sistema(xml):
    """(texto, pacote, bounds) dos nós de outros pacotes: só para o diálogo de permissão (B1)."""
    saida = []
    for n in ET.fromstring(xml).iter("node"):
        m = _BOUNDS.fullmatch(n.get("bounds", ""))
        if n.get("package") != PACOTE and m and (n.get("text") or n.get("content-desc")):
            saida.append((n.get("text") or n.get("content-desc"), n.get("package"), tuple(int(v) for v in m.groups())))
    return saida


def nos(xml):
    """Lista de (texto, classe, (x1, y1, x2, y2), habilitado, marcado, checável) dos nós do pacote da etapa0.
    `marcado` é checked ou selected do próprio nó ou de um ancestral (o FilterChip marca o contêiner do rótulo)."""
    raiz = ET.fromstring(xml)
    saida = []

    def andar(n, marcado_pai):
        marcado = marcado_pai or n.get("checked") == "true" or n.get("selected") == "true"
        m = _BOUNDS.fullmatch(n.get("bounds", ""))
        if n.get("package") == PACOTE and m:
            texto = n.get("text") or n.get("content-desc") or ""
            saida.append((texto, n.get("class", ""), tuple(int(v) for v in m.groups()), n.get("enabled") == "true",
                          marcado, n.get("checkable") == "true"))
        for filho in n.findall("node"):
            andar(filho, marcado)

    for n in raiz.findall("node"):
        andar(n, False)
    return saida


def dump(sistema=False):
    TMP.mkdir(parents=True, exist_ok=True)
    destino = "/sdcard/tribuna-0005-ui.xml"
    saida = adb("shell", "uiautomator", "dump", destino)
    if "dumped to" not in saida:
        raise FalhaDump(saida.strip())
    local = TMP / "ui.xml"
    adb("pull", destino, str(local))
    adb("shell", "rm", destino)
    try:
        conteudo = local.read_text(encoding="utf-8")
        return nos_sistema(conteudo) if sistema else nos(conteudo)
    finally:
        local.unlink(missing_ok=True)


def tem_sistema(regex):
    achados = [(t, pac) for t, pac, _ in dump(sistema=True) if re.search(regex, t) and "systemui" not in pac]
    for t, pac in achados:
        print(f"{pac}\t{t}")
    return 0 if achados else 1


def tocar_sistema(texto):
    achados = [b for t, pac, b in dump(sistema=True) if t == texto and "systemui" not in pac]
    if len(achados) != 1:
        print(f"ui.py: '{texto}' achado {len(achados)} vez(es) fora do app", file=sys.stderr)
        return 1
    x, y = centro(achados[0])
    adb("shell", "input", "tap", str(x), str(y))
    return 0


def centro(b):
    return (b[0] + b[2]) // 2, (b[1] + b[3]) // 2


def achar(lista, texto):
    achados = [n for n in lista if n[0] == texto]
    return achados[0] if len(achados) == 1 else None


def tocar(texto, rolar=6):
    for volta in range(rolar + 1):
        n = achar(dump(), texto)
        if n is not None:
            if not n[3]:
                print(f"ui.py: '{texto}' está desabilitado", file=sys.stderr)
                return 1
            x, y = centro(n[2])
            adb("shell", "input", "tap", str(x), str(y))
            return 0
        if volta < rolar:
            adb("shell", "input", "swipe", "540", "2000", "540", "1400", "300")
            time.sleep(0.5)
    # o painel pode ter ficado rolado além do item: volta para cima (B7, release; H-cabo 2026-10-12)
    for _ in range(rolar + 2):
        adb("shell", "input", "swipe", "700", "1690", "700", "2250", "300")
        time.sleep(0.5)
        n = achar(dump(), texto)
        if n is not None and n[3]:
            x, y = centro(n[2])
            adb("shell", "input", "tap", str(x), str(y))
            return 0
    print(f"ui.py: '{texto}' não está na tela (ou aparece mais de uma vez)", file=sys.stderr)
    return 1


def chip(texto_id):
    for _ in range(4):
        lista = dump()
        n = achar(lista, texto_id)
        if n is not None:
            x, y = centro(n[2])
            adb("shell", "input", "tap", str(x), str(y))
            return 0
        chips = [c for c in lista if re.fullmatch(r"T\d\d|L30", c[0])]
        if not chips:
            break
        y = centro(chips[0][2])[1]
        adb("shell", "input", "swipe", "900", str(y), "200", str(y), "300")
        time.sleep(0.5)
    print(f"ui.py: chip {texto_id} não achado", file=sys.stderr)
    return 1


def campo(atual, novo):
    n = next((c for c in dump() if c[0] == atual and c[1].endswith("EditText")), None)
    if n is None:
        print(f"ui.py: campo com '{atual}' não achado", file=sys.stderr)
        return 1
    x, y = centro(n[2])
    adb("shell", "input", "tap", str(x), str(y))
    adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
    adb("shell", "input", "keyevent", *(["KEYCODE_DEL"] * (len(atual) + 2)))
    adb("shell", "input", "text", novo)
    adb("shell", "input", "keyevent", "KEYCODE_BACK")  # fecha o teclado
    time.sleep(0.5)
    if not any(c[0] == novo and c[1].endswith("EditText") for c in dump()):
        print(f"ui.py: o campo não ficou com '{novo}'", file=sys.stderr)
        return 1
    return 0


def topo():
    """Rola o painel de depuração até o topo (as linhas de estado ficam lá; tocar/rolar as deixa fora da tela).
    Achado da Fase C (B1): sem isto, `linhas` depois de `tocar Parar` volta vazio. Gestos dentro do painel (y>=1700)."""
    for _ in range(4):
        adb("shell", "input", "swipe", "540", "1700", "540", "2250", "200")
    time.sleep(0.5)


def linhas(regex):
    topo()
    padrao = re.compile(regex)
    achadas = [n[0] for n in dump() if padrao.search(n[0])]
    for t in achadas:
        print(t)
    return 0 if achadas else 1


def marcado(texto):
    n = achar(dump(), texto)
    if n is None:
        print(f"ui.py: '{texto}' não achado", file=sys.stderr)
        return 1
    print("true" if n[4] else "false")
    return 0


_ROTULO_LIMIAR = re.compile(r"Limiar de nível: (\d+\.\d{3})")


def limiar(valor, voltas=6):
    """Toca no slider do limiar até o rótulo ficar em [valor, valor + 0,002] (o rótulo tem 3 casas)."""
    alvo = float(valor)
    for _ in range(voltas):
        lista = dump()
        rotulo = next((n for n in lista if _ROTULO_LIMIAR.fullmatch(n[0])), None)
        if rotulo is None:
            print("ui.py: rótulo do limiar não está na tela (abra o painel)", file=sys.stderr)
            return 1
        atual = float(_ROTULO_LIMIAR.fullmatch(rotulo[0]).group(1))
        if alvo <= atual <= alvo + 0.002:
            print(f"{atual:.3f}")
            return 0
        abaixo = [n for n in lista if n[2][1] >= rotulo[2][3]
                  and (n[1].endswith("SeekBar") or (n[0] == "" and n[2][2] - n[2][0] > 500))]
        if not abaixo:
            print("ui.py: slider do limiar não achado", file=sys.stderr)
            return 1
        x1, y1, x2, y2 = min(abaixo, key=lambda n: n[2][1])[2]
        x = round(x1 + (x2 - x1) * (alvo + 0.001))
        adb("shell", "input", "tap", str(min(max(x, x1), x2)), str((y1 + y2) // 2))
        time.sleep(0.5)
    print("ui.py: o limiar não chegou ao alvo; ajuste à mão e confira o meta.txt", file=sys.stderr)
    return 1


def interruptor(rotulo, valor):
    """O Switch do Compose não tem texto e é irmão do rótulo: acha o nó checável à direita, na mesma linha."""
    for _ in range(2):
        lista = dump()
        r = achar(lista, rotulo)
        for gesto in (("700", "1690", "700", "2250"),) * 6 + (("700", "2200", "700", "1600"),) * 12:
            if r is not None:
                break
            adb("shell", "input", "swipe", *gesto, "300")
            time.sleep(0.5)
            lista = dump()
            r = achar(lista, rotulo)
        if r is None:
            print(f"ui.py: '{rotulo}' não achado", file=sys.stderr)
            return 1
        y = centro(r[2])[1]
        switches = [n for n in lista if n[5] and n[2][1] <= y <= n[2][3] and n[2][0] >= r[2][2]]
        if len(switches) != 1:
            print(f"ui.py: {len(switches)} interruptor(es) na linha de '{rotulo}'", file=sys.stderr)
            return 1
        if switches[0][4] == (valor == "on"):
            print(valor)
            return 0
        x, yy = centro(switches[0][2])
        adb("shell", "input", "tap", str(x), str(yy))
        time.sleep(0.5)
    print(f"ui.py: o interruptor de '{rotulo}' não ficou {valor}", file=sys.stderr)
    return 1


def esperar_habilitado(texto, segundos):
    fim = time.time() + float(segundos)
    while time.time() < fim:
        n = achar(dump(), texto)
        if n is not None and n[3]:
            return 0
        time.sleep(1)
    return 1


def esperar(texto, segundos):
    fim = time.time() + float(segundos)
    while time.time() < fim:
        if achar(dump(), texto) is not None:
            return 0
        time.sleep(1)
    return 1


EXEMPLO = """<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><hierarchy rotation="0">
<node text="" package="com.android.systemui" class="android.widget.FrameLayout" bounds="[0,0][1080,80]" enabled="true">
<node text="Mensagem de alguém" package="com.android.systemui" class="android.widget.TextView" bounds="[0,0][500,80]" enabled="true" /></node>
<node text="" package="io.github.gigioderico.tribuna.etapa0" class="android.view.View" bounds="[0,0][1080,2340]" enabled="true">
<node text="" package="io.github.gigioderico.tribuna.etapa0" class="android.view.View" bounds="[30,190][140,270]" enabled="true" selected="true">
<node text="T01" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.TextView" bounds="[36,200][136,260]" enabled="true" /></node>
<node text="Ouvir" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.TextView" bounds="[36,2100][236,2200]" enabled="true" />
<node text="Pausar" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.TextView" bounds="[260,2100][460,2200]" enabled="false" />
<node text="T01" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.EditText" bounds="[36,1800][1044,1900]" enabled="true" />
<node text="Vocabulário restrito" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.TextView" bounds="[36,1600][400,1660]" enabled="true" />
<node text="" package="io.github.gigioderico.tribuna.etapa0" class="android.view.View" bounds="[430,1590][560,1670]" enabled="true" checkable="true" checked="false" />
<node text="instalação 812 ms · prepare 1234 ms · beginSession 45 ms · 1º parcial - ms" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.TextView" bounds="[36,1500][1044,1540]" enabled="true" />
</node></hierarchy>"""


def autoteste():
    lista = nos(EXEMPLO)
    assert all("Mensagem" not in n[0] for n in lista), "nó de outro pacote entrou"
    assert ("Mensagem de alguém", "com.android.systemui", (0, 0, 500, 80)) in nos_sistema(EXEMPLO)
    assert achar(lista, "Ouvir")[2] == (36, 2100, 236, 2200)
    assert centro(achar(lista, "Ouvir")[2]) == (136, 2150)
    assert achar(lista, "T01") is None, "T01 aparece duas vezes (chip e campo): ambíguo"
    assert achar(lista, "Pausar")[3] is False
    assert [n[4] for n in lista if n[0] == "T01"] == [True, False], "marcado vem do contêiner do chip"
    r = achar(lista, "Vocabulário restrito")
    y = centro(r[2])[1]
    sw = [n for n in lista if n[5] and n[2][1] <= y <= n[2][3] and n[2][0] >= r[2][2]]
    assert len(sw) == 1 and sw[0][4] is False, "interruptor da linha do rótulo"
    numeros = [n[0] for n in lista if re.search(r"beginSession \d+ ms", n[0])]
    assert numeros == ["instalação 812 ms · prepare 1234 ms · beginSession 45 ms · 1º parcial - ms"]
    print("ui.py autoteste: ok")
    return 0


def main(argv):
    if not argv:
        print(__doc__)
        return 2
    comando, args = argv[0], argv[1:]
    try:
        if comando == "autoteste":
            return autoteste()
        funcoes = {"tocar": tocar, "chip": chip, "campo": campo, "tem": lambda t: 0 if achar(dump(), t) else 1,
                   "linhas": linhas, "esperar": esperar, "marcado": marcado, "limiar": limiar,
                   "tem-sistema": tem_sistema, "tocar-sistema": tocar_sistema, "interruptor": interruptor,
                   "esperar-habilitado": esperar_habilitado}
        return funcoes[comando](*args)
    except FalhaDump as erro:
        print(f"ui.py: uiautomator falhou: {erro}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
