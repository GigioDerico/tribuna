#!/usr/bin/env bash
# privacidade.sh arquivos <rotulo> | comparar <antes> <depois> | logcat-limpar | logcat <rotulo>
# Regras 2 e 3 no aparelho (spec B1, B5; impl_plan 0005, A11). O logcat inteiro do aparelho do humano nunca vai
# para o repositório (tem dados de outros apps): fica em $TMPD, é lido por grep e apagado; na evidência só as
# contagens.
source "$(dirname "$0")/comum.sh"
exige_aparelho
PRIV="$EV/privacidade"; mkdir -p "$PRIV"
# 10 palavras distintivas dos textos lidos (conferidas em etapa0/textos/ no PLAN), com e sem acento
PALAVRAS=(rodeios conselhos estiagem agricultor perfeccionismo escondida "paciência" paciencia agradecer
          "coríntios" corintios subestimado)
AUDIO='\.(wav|pcm|raw|m4a|3gp|amr|ogg)$'
case "${1:-}" in
  arquivos)
    r="$2"
    { a run-as "$PKG" find . -type f 2>&1 | sed 's|^|interno: |'
      a find "/sdcard/Android/data/$PKG" -type f 2>&1 | sed 's|^|externo: |'; } | sort > "$PRIV/arquivos-$r.txt"
    wc -l < "$PRIV/arquivos-$r.txt" ;;
  comparar)
    novos="$PRIV/arquivos-novos-$2-$3.txt"
    comm -13 "$PRIV/arquivos-$2.txt" "$PRIV/arquivos-$3.txt" > "$novos"
    echo "novos: $(wc -l < "$novos")"; cat "$novos"
    ruins=$(grep -E -i "$AUDIO" "$novos" || true)
    fora=$(grep '^externo: ' "$novos" | grep -v "/files/exports/" || true)
    internos=$(grep '^interno: ' "$novos" || true)
    if [ -n "$ruins$fora$internos" ]; then
      echo "REPROVA: arquivo novo de áudio, fora de exports/ ou no armazenamento interno:"; echo "$ruins$fora$internos"; exit 1
    fi
    echo "PASSA: nenhum arquivo novo de áudio; novos só em exports/" ;;
  logcat-limpar) adb logcat -c; log "logcat limpo" ;;
  logcat)
    r="$2"; bruto="$TMPD/logcat-$r.txt"
    adb logcat -d > "$bruto"
    {
      echo "linhas do logcat: $(wc -l < "$bruto")"
      echo "linhas do pacote $PKG: $(grep -c "$PKG" "$bruto" || true)"
      echo "FATAL EXCEPTION: $(grep -c 'FATAL EXCEPTION' "$bruto" || true)"
      for p in "${PALAVRAS[@]}"; do echo "palavra $p: $(grep -c -i -w "$p" "$bruto" || true)"; done
    } > "$PRIV/logcat-$r.txt"
    rm -f "$bruto"
    cat "$PRIV/logcat-$r.txt"
    if grep -E -q '^(FATAL EXCEPTION|palavra [^:]+): [1-9]' "$PRIV/logcat-$r.txt"; then echo "REPROVA"; exit 1; fi
    echo "PASSA" ;;
  *) echo "uso: privacidade.sh arquivos <r> | comparar <antes> <depois> | logcat-limpar | logcat <r>" >&2; exit 2 ;;
esac
