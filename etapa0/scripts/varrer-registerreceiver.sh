#!/usr/bin/env bash
# CIA 0003 (portão 1, opção A), mitigação 3: varre as classes do classpath de release e mostra cada chamada de
# ContextCompat.registerReceiver com a flag passada, e cada chamada de receiveBroadcasts (core-ktx).
# Passa (exit 0) só se nenhuma chamada usar RECEIVER_NOT_EXPORTED (4) ou flag não constante, e se ninguém
# chamar receiveBroadcasts (o ContextKt do core-ktx só repassa a flag de quem chama). Rodar a cada troca de versão no catalog; a saída vai para work/<tarefa>/evidencias/.
set -euo pipefail
cd "$(dirname "$0")/.."
WORK="$(mktemp -d)"
trap 'rm -r "$WORK"' EXIT
./gradlew -q :app:releaseClasspathFiles --console=plain | sed -n 's/^ART //p' > "$WORK/arts.txt"
n=0
classes=0
: > "$WORK/dirs.txt"
while read -r id file; do
  [ -f "$file" ] || { echo "aviso: $id sem arquivo ($file); rode assembleRelease antes"; continue; }
  n=$((n + 1))
  dir="$WORK/c$n"
  mkdir -p "$dir/x"
  case "$file" in
    *.aar)
      if unzip -q -o "$file" classes.jar -d "$dir" 2>/dev/null; then unzip -q -o "$dir/classes.jar" -d "$dir/x"; fi
      ;;
    *.jar) unzip -q -o "$file" -d "$dir/x" ;;
  esac
  count=$(find "$dir/x" -name '*.class' | wc -l | tr -d ' ')
  classes=$((classes + count))
  echo "$id $dir/x" >> "$WORK/dirs.txt"
done < "$WORK/arts.txt"
echo "bibliotecas: $n; classes: $classes"
fail=0
found=0
while read -r id dir; do
  while IFS= read -r cls; do
    rel="${cls#"$dir"/}"
    out=$(javap -c -p "$cls" | awk -v lib="$id" -v cls="$rel" '
      /invokestatic.*androidx\/core\/content\/ContextCompat\.registerReceiver/ {
        flag = "nao_constante"
        if (prev ~ /iconst_2$/) flag = "2_EXPORTED"
        else if (prev ~ /iconst_4$/) flag = "4_NOT_EXPORTED"
        else if (prev ~ /(bipush|sipush|iconst_[0-9m]+)/) { n = split(prev, a, " "); flag = "constante_" a[n] }
        if (cls ~ /^androidx\/core\/content\/ContextKt/) flag = "repassada_por_receiveBroadcasts"
        print lib " " cls " ContextCompat.registerReceiver flag=" flag
      }
      /invoke[a-z]+ .*receiveBroadcasts/ && cls !~ /^androidx\/core\/content\/ContextKt/ { print lib " " cls " receiveBroadcasts chamada" }
      /^ *[0-9]+: / { prev = $0 }')
    if [ -n "$out" ]; then
      echo "$out"
      found=$((found + 1))
      if grep -qE "4_NOT_EXPORTED|nao_constante|receiveBroadcasts chamada" <<<"$out"; then fail=1; fi
    fi
  done < <(grep -rlaE "ContextCompat\.registerReceiver|androidx/core/content/ContextCompat|receiveBroadcasts" "$dir" --include='*.class' 2>/dev/null | grep -v "androidx/core/content/ContextCompat" || true)
done < "$WORK/dirs.txt"
echo "classes com chamada: $found"
if [ "$fail" -eq 0 ]; then echo "OK: nenhuma chamada com RECEIVER_NOT_EXPORTED ou flag não constante"; else echo "REPROVADO: ver as linhas acima"; fi
exit "$fail"
