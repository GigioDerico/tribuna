# comum.sh: incluído pelos scripts da 0005 (impl_plan 0005, A8). Bash, não zsh: no zsh "$VAR com espaço" não
# se divide em palavras e "a()" com adb quebra (visto no PLAN).
set -euo pipefail
RAIZ="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
SCRIPTS="$RAIZ/work/0005-etapa0-medicao/scripts"
EV="$RAIZ/work/0005-etapa0-medicao/evidencias"
E0="$RAIZ/etapa0"
FERR="$E0/ferramentas"
PKG="io.github.gigioderico.tribuna.etapa0"
EXPORTS="/sdcard/Android/data/$PKG/files/exportacoes"
WAVDIR="/sdcard/Android/data/$PKG/files/wav"
export ANDROID_SERIAL="${ANDROID_SERIAL:-RQCW80BPXKN}"
TMPD="${TMPDIR:-/tmp}/tribuna-0005"
VENV="${TRIBUNA_VENV:-$HOME/.venvs/tribuna-etapa0}"
MODELO="${TRIBUNA_MODELO:-$HOME/vosk-modelos/vosk-model-pt-fb-v0.1.1-20220516_2113}"
mkdir -p "$EV" "$TMPD"

a() { adb shell "$@" | tr -d '\r'; }
ui() { python3 "$SCRIPTS/ui.py" "$@"; }
exige_aparelho() {
  if [ "$(adb get-state 2>/dev/null)" != "device" ]; then
    echo "PARAR: aparelho $ANDROID_SERIAL não está no adb (cabo, depuração USB ou autorização). Pedir ao humano." >&2
    exit 3
  fi
}
log() { echo "$(date '+%Y-%m-%dT%H:%M:%S') $*" | tee -a "$EV/diario.txt"; }
