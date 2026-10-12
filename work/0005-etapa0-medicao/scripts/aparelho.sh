#!/usr/bin/env bash
# aparelho.sh salvar|aplicar [bateria]|restaurar|ler: ajustes do aparelho com valor de antes e de depois
# (spec A4, D18; impl_plan 0005, A9). Nunca sobrescreve o "antes": se ele já existe, salvar sai com erro.
source "$(dirname "$0")/comum.sh"
ESTADO="$EV/aparelho-estado-antes.env"
AJ="$EV/aparelho-ajustes.txt"

ler() {
  echo "airplane_mode_on=$(a settings get global airplane_mode_on)"
  echo "zen_mode=$(a settings get global zen_mode)"
  echo "screen_brightness=$(a settings get system screen_brightness)"
  echo "screen_brightness_mode=$(a settings get system screen_brightness_mode)"
  echo "screen_off_timeout=$(a settings get system screen_off_timeout)"
  echo "stay_on_while_plugged_in=$(a settings get global stay_on_while_plugged_in)"
  echo "accelerometer_rotation=$(a settings get system accelerometer_rotation)"
  echo "user_rotation=$(a settings get system user_rotation)"
}

exige_aparelho
case "${1:-}" in
  ler) ler ;;
  salvar)
    if [ -f "$ESTADO" ]; then echo "já existe $ESTADO: restaure antes de salvar de novo" >&2; exit 1; fi
    ler > "$ESTADO"
    { echo "== antes $(date '+%Y-%m-%dT%H:%M:%S')"; cat "$ESTADO"; } >> "$AJ" ;;
  aplicar)
    [ -f "$ESTADO" ] || { echo "rode 'aparelho.sh salvar' antes" >&2; exit 1; }
    a cmd connectivity airplane-mode enable
    a cmd notification set_dnd on
    a settings put system screen_brightness_mode 0
    a settings put system screen_brightness 255
    a svc power stayon usb
    if [ "${2:-}" = "bateria" ]; then a settings put system screen_off_timeout 3600000; fi
    { echo "== aplicado ${2:-sessao} $(date '+%Y-%m-%dT%H:%M:%S')"; ler; } >> "$AJ"
    falta=""
    [ "$(a settings get global airplane_mode_on)" = "1" ] || falta="$falta modo-aviao"
    [ "$(a settings get global zen_mode)" != "0" ] || falta="$falta nao-perturbe"
    [ "$(a settings get system screen_brightness_mode)" = "0" ] || falta="$falta brilho-fixo"
    if [ "${2:-}" = "bateria" ]; then
      [ "$(a settings get system screen_off_timeout)" = "3600000" ] || falta="$falta tempo-de-tela"
    fi
    if [ -n "$falta" ]; then echo "PARAR: o adb não conseguiu:$falta. Pedir ao humano que ajuste na tela." >&2; exit 1; fi
    echo "ajustes aplicados" ;;
  restaurar)
    [ -f "$ESTADO" ] || { echo "nada a restaurar" >&2; exit 1; }
    # shellcheck disable=SC1090
    source "$ESTADO"
    if [ "$airplane_mode_on" = "0" ]; then a cmd connectivity airplane-mode disable; fi
    case "$zen_mode" in 0) a cmd notification set_dnd off ;; 1) a cmd notification set_dnd priority ;;
      2) a cmd notification set_dnd none ;; 3) a cmd notification set_dnd alarms ;; esac
    a settings put system screen_brightness "$screen_brightness"
    a settings put system screen_brightness_mode "$screen_brightness_mode"
    a settings put system screen_off_timeout "$screen_off_timeout"
    a settings put global stay_on_while_plugged_in "$stay_on_while_plugged_in"
    a settings put system accelerometer_rotation "$accelerometer_rotation"
    a settings put system user_rotation "$user_rotation"
    ler > "$TMPD/depois.env"
    { echo "== depois $(date '+%Y-%m-%dT%H:%M:%S')"; cat "$TMPD/depois.env"; } >> "$AJ"
    if diff -u "$ESTADO" "$TMPD/depois.env" >> "$AJ"; then
      mv "$ESTADO" "$EV/aparelho-estado-antes-$(date '+%Y%m%dT%H%M%S').env"
      echo "restaurado: igual ao antes"
    else
      echo "PARAR: o estado depois difere do antes (diff em $AJ). Pedir ao humano." >&2; exit 1
    fi ;;
  *) echo "uso: aparelho.sh salvar|aplicar [bateria]|restaurar|ler" >&2; exit 2 ;;
esac
