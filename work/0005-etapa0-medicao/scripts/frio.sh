#!/usr/bin/env bash
# frio.sh quente <n> | primeira <n>: inicialização a frio e primeira cópia do modelo (spec G2, D17; impl_plan 0005, A12).
# Cada corrida: force-stop (ou pm clear + pm grant), am start -W, chip T01, Ouvir até o modelo carregar, Pausar,
# Parar, e lê o painel ("instalação X ms · prepare Y ms · beginSession Z ms · ..."). O microfone abre por ~3 s.
source "$(dirname "$0")/comum.sh"
exige_aparelho
modo="$1"; n="$2"; saida="$EV/frio-$modo.tsv"
echo -e "corrida\tTotalTime_ms\tpainel" > "$saida"
for i in $(seq 1 "$n"); do
  a am force-stop "$PKG"
  if [ "$modo" = primeira ]; then
    a pm clear "$PKG" > /dev/null
    a pm grant "$PKG" android.permission.RECORD_AUDIO
  fi
  total=$(a am start -W -n "$PKG/.MainActivity" | sed -n 's/^TotalTime: //p')
  sleep 1
  ui chip T01
  ui tocar "Ouvir"
  ui esperar-habilitado "Pausar" 180 || { echo "PARAR: o modelo não carregou em 180 s" >&2; exit 1; }
  sleep 3
  ui tocar "Pausar"
  ui tocar "Depuração"
  ui tocar "Parar"
  painel=$(ui linhas "beginSession" | head -1)
  echo -e "$i\t$total\t$painel" | tee -a "$saida"
done
python3 - "$saida" <<'EOF'
import re, statistics, sys
linhas = open(sys.argv[1], encoding="utf-8").read().splitlines()[1:]
def col(regex):
    v = [int(m.group(1)) for l in linhas for m in [re.search(regex, l)] if m]
    return f"n={len(v)}, mediana={statistics.median(v) if v else None}, máximo={max(v) if v else None}"
print("TotalTime ms:", col(r"^\d+\t(\d+)\t"))
print("prepare ms:", col(r"prepare (\d+) ms"))
print("instalação ms:", col(r"instalação (\d+) ms"))
EOF
