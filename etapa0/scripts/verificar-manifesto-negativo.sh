#!/usr/bin/env bash
# Prova (Q2) que verifyMergedManifest falha com permissões proibidas e passa sem elas.
# Cria um manifesto temporário só na variante debug; o manifesto principal não é tocado.
set -u
cd "$(dirname "$0")/.."
OVERLAY_DIR=app/src/debug
OVERLAY="$OVERLAY_DIR/AndroidManifest.xml"
if [ -e "$OVERLAY" ]; then
  echo "abortado: $OVERLAY já existe"
  exit 2
fi
cleanup() {
  rm -f "$OVERLAY"
  rmdir "$OVERLAY_DIR" 2>/dev/null || true
}
trap cleanup EXIT
mkdir -p "$OVERLAY_DIR"
fail=0
for perm in INTERNET ACCESS_NETWORK_STATE WAKE_LOCK CAMERA; do
  cat > "$OVERLAY" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.$perm" />
</manifest>
EOF
  out=$(./gradlew :app:verifyDebugMergedManifest --console=plain 2>&1)
  code=$?
  if [ "$code" -ne 0 ] && grep -q "permissão não permitida: android.permission.$perm" <<<"$out"; then
    echo "OK: $perm -> verifyDebugMergedManifest falhou como esperado"
    grep "permissão não permitida" <<<"$out"
  else
    echo "ERRO: $perm -> código $code, sem a mensagem esperada"
    tail -20 <<<"$out"
    fail=1
  fi
done
cleanup
out=$(./gradlew :app:verifyDebugMergedManifest --console=plain 2>&1)
code=$?
if [ "$code" -eq 0 ]; then
  echo "OK: só RECORD_AUDIO -> verifyDebugMergedManifest passou"
else
  echo "ERRO: manifesto limpo falhou (código $code)"
  tail -20 <<<"$out"
  fail=1
fi
exit "$fail"
