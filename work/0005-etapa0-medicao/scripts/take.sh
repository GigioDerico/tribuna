#!/usr/bin/env bash
# take.sh preparar <ID> | fechar <nome> [reproducoes|ensaio] | puxar <nome> [reproducoes|ensaio]
# Antes e depois de cada take (spec C1, C2; impl_plan 0005, A8). O humano toca Ouvir/Pausar; o agente fecha.
source "$(dirname "$0")/comum.sh"
exige_aparelho
cmd="${1:-}"; arg="${2:-}"; sub="${3:-}"
case "$cmd" in
  preparar)
    [[ "$arg" =~ ^(T0[1-9]|T10|L30)$ ]] || { echo "ID inválido: $arg" >&2; exit 2; }
    a am start -n "$PKG/.MainActivity" > /dev/null
    sleep 1
    ui chip "$arg"
    log "preparado $arg" ;;
  fechar)
    nome="$arg"; id="${nome%%_*}"
    [[ "$nome" =~ ^(T0[1-9]|T10|L30)(_[a-z0-9]+)+$ ]] || { echo "nome fora do padrão: $nome" >&2; exit 2; }
    ui tocar "Depuração"
    ui tocar "Parar"
    ui campo "$id" "$nome"
    ui tocar "Exportar"
    sleep 1
    ui linhas "^Exportado: " | tee -a "$EV/diario.txt"
    ui tocar "Depuração"
    "$0" puxar "$nome" "$sub" ;;
  puxar)
    nome="$arg"; id="${nome%%_*}"
    destino_t="$E0/trajetorias${sub:+/$sub}"; destino_e="$E0/eventos${sub:+/$sub}"
    if [ "$sub" = ensaio ]; then destino_t="$TMPD/ensaio"; destino_e="$TMPD/ensaio"; fi  # ensaio: fora do repositório
    mkdir -p "$destino_t" "$destino_e"
    for suf in hypotheses.csv level.csv trajectory.csv meta.txt events.csv; do
      alvo="$destino_t/$nome.$suf"; [ "$suf" = events.csv ] && alvo="$destino_e/$nome.$suf"
      if [ -e "$alvo" ]; then echo "PARAR: $alvo já existe (take refeito ganha t<n+1>)" >&2; exit 1; fi
      adb pull "$EXPORTS/$nome.$suf" "$TMPD/$nome.$suf" > /dev/null
      mv "$TMPD/$nome.$suf" "$alvo"
    done
    meta="$destino_t/$nome.meta.txt"
    grep -qx "text_id=$id" "$meta" || { echo "PARAR: text_id do meta.txt não é $id" >&2; exit 1; }
    log "puxado $nome: $(tr '\n' ' ' < "$meta")"
    wc -l "$destino_t/$nome."*.csv "$destino_e/$nome.events.csv" | tee -a "$EV/diario.txt" ;;
  *) echo "uso: take.sh preparar <ID> | fechar <nome> [reproducoes] | puxar <nome> [reproducoes]" >&2; exit 2 ;;
esac
