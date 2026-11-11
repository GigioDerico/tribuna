#!/usr/bin/env bash
# memoria.sh <rotulo> <minutos>: dumpsys meminfo do pacote a cada 60 s (spec G3, D15; impl_plan 0005, A12).
# Rodar em segundo plano durante a S1; para sozinho no fim ou quando o processo morre (registrado).
source "$(dirname "$0")/comum.sh"
r="$1"; minutos="$2"; saida="$EV/memoria-$r.tsv"
echo -e "hora\tpid\tlinha_total" > "$saida"
for _ in $(seq 1 "$minutos"); do
  pid=$(a pidof "$PKG" || true)
  if [ -z "$pid" ]; then echo -e "$(date '+%H:%M:%S')\t-\tprocesso morto" >> "$saida"
  else echo -e "$(date '+%H:%M:%S')\t$pid\t$(a dumpsys meminfo "$PKG" | grep -E 'TOTAL PSS:|TOTAL RSS:' | tr -s ' ' | head -1)" >> "$saida"
  fi
  sleep 60
done
python3 - "$saida" <<'EOF'
import re, statistics, sys
pss, rss = [], []
for linha in open(sys.argv[1], encoding="utf-8").read().splitlines()[1:]:
    m = re.search(r"TOTAL PSS: (\d+).*TOTAL RSS: (\d+)", linha)
    if m:
        pss.append(int(m.group(1))); rss.append(int(m.group(2)))
for nome, v in (("PSS", pss), ("RSS", rss)):
    print(f"{nome} kB: n={len(v)}, mediana={statistics.median(v) if v else None}, pico={max(v) if v else None}")
EOF
