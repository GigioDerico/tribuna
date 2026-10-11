#!/usr/bin/env bash
# Baixa o vosk-model-small-pt-0.3 (Apache 2.0, alphacephei.com) para app/src/main/assets/model-pt/ e grava o
# arquivo uuid com o SHA-256 do zip (spec 0003, A4, D12; TRD Motor de voz §Instalação do modelo 2).
# O SHA-256 esperado foi medido pelo arquiteto em 2026-10-11 (impl_plan 0003); outro zip aborta (Q8).
set -euo pipefail
cd "$(dirname "$0")/.."
URL="https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip"
SHA256="6e1ce909032e1afa7a88e68a3d628ecafff302bdf195befab308826c395e93b7"
DEST="app/src/main/assets/model-pt"
if [ -e "$DEST" ]; then
  echo "abortado: $DEST já existe"
  exit 2
fi
TMP="$(mktemp -d)"
trap 'rm -r "$TMP"' EXIT
curl -fsSL -o "$TMP/modelo.zip" "$URL"
got="$(shasum -a 256 "$TMP/modelo.zip" | cut -d' ' -f1)"
if [ "$got" != "$SHA256" ]; then
  echo "ERRO: SHA-256 do zip é $got; esperado $SHA256"
  exit 1
fi
mkdir "$TMP/x"
unzip -q "$TMP/modelo.zip" -d "$TMP/x"
mkdir -p "$DEST"
cp -R "$TMP/x/vosk-model-small-pt-0.3/." "$DEST/"
printf 'vosk-model-small-pt-0.3 sha256=%s\n' "$SHA256" > "$DEST/uuid"
echo "OK: zip sha256=$got"
echo "OK: uuid=$(cat "$DEST/uuid")"
find "$DEST" -type f | sort | while read -r f; do printf '%10d %s\n' "$(wc -c < "$f")" "$f"; done
