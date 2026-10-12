#!/usr/bin/env bash
# medir.sh converter <m4a> <nome> | sinc <rodada> <nome> [reproducao <rep>] | verdade <nome>
#          | folha <rodada> <nome> [reproducao <rep>] | metricas <rodada> <nome> [reproducao <rep>]
# Do áudio ao relatório de uma gravação (spec C2, D1–D5, E1; impl_plan 0005, A3–A5). Saídas e códigos vão para
# relatorios/<rodada>/ e para o diário. Nenhum passo sobrescreve WAV, words.csv ou expected.csv (correções da
# revisão ficam no expected.csv e no revisao.md, D7).
source "$(dirname "$0")/comum.sh"
cmd="${1:-}"
rc_aceito() { [ "$1" -le "$2" ] || { echo "PARAR: código $1 (entrada inválida); ver stderr acima" >&2; exit 1; }; }

case "$cmd" in
  converter)
    m4a="$2"; nome="$3"; wav="$E0/gravacoes/$nome.wav"
    [ -e "$wav" ] && { echo "PARAR: $wav já existe" >&2; exit 1; }
    mkdir -p "$EV/afinfo"
    afinfo "$m4a" > "$EV/afinfo/$nome.original.txt"
    afconvert -f WAVE -d LEI16@16000 -c 1 --mix "$m4a" "$wav"
    afinfo "$wav" > "$EV/afinfo/$nome.wav.txt"
    python3 -c "import wave,sys; w=wave.open(sys.argv[1]); assert (w.getnchannels(),w.getsampwidth(),w.getframerate(),w.getcomptype())==(1,2,16000,'NONE'), 'formato'; print('wav ok', w.getnframes()/16000, 's')" "$wav" | tee -a "$EV/diario.txt" ;;
  sinc)
    rodada="$2"; nome="$3"; rel="$E0/relatorios/$rodada"; mkdir -p "$rel"
    if [ "${4:-}" = reproducao ]; then
      rep="$5"; rel="$rel/reproducoes"; mkdir -p "$rel"
      set +e; python3 "$FERR/sincronizar.py" "$E0/gravacoes/$nome.wav" "$E0/trajetorias/reproducoes/$rep.level.csv" \
        --faixa-ms -1000 1000 --json "$rel/$rep.sinc.json" > "$rel/$rep.sinc.md"; rc=$?; set -e
      log "sinc $rep: rc=$rc d=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['deslocamento_ms'])" "$rel/$rep.sinc.json" 2>/dev/null)"
    else
      set +e; python3 "$FERR/sincronizar.py" "$E0/gravacoes/$nome.wav" "$E0/trajetorias/$nome.level.csv" \
        --json "$rel/$nome.sinc.json" > "$rel/$nome.sinc.md"; rc=$?; set -e
      log "sinc $nome: rc=$rc d=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['deslocamento_ms'])" "$rel/$nome.sinc.json" 2>/dev/null)"
    fi
    rc_aceito "$rc" 1 ;;
  verdade)
    nome="$2"; id="${nome%%_*}"; esp="$E0/esperado"
    words="$esp/$nome.words.csv"; expected="$esp/$nome.expected.csv"
    [ -x "$VENV/bin/python" ] || { echo "PARAR: venv do D1 ausente ($VENV); o passo D1 vem antes" >&2; exit 1; }
    if [ ! -e "$words" ]; then
      "$VENV/bin/python" "$FERR/timestamps.py" "$E0/gravacoes/$nome.wav" --modelo "$MODELO" -o "$words"
      log "timestamps $nome: rc=0 $(wc -l < "$words") linhas"
    fi
    if [ ! -e "$expected" ]; then
      mkdir -p "$EV/alinhar"
      python3 "$FERR/alinhar.py" "$E0/textos/$id.txt" "$words" -o "$expected" 2> "$EV/alinhar/$nome.stderr.txt"
      log "alinhar $nome: rc=0; $(head -3 "$EV/alinhar/$nome.stderr.txt" | tr '\n' ' ')"
    fi ;;
  folha)
    rodada="$2"; nome="$3"; id="${nome%%_*}"
    case "$id" in T04|T05|T06|L30) ;; *) echo "$id não tem folha (só T04, T05, T06, L30)"; exit 0 ;; esac
    sincjson="$E0/relatorios/$rodada/$nome.sinc.json"; folha="$E0/eventos/$nome.folha.events.csv"; rotulo="$nome"
    if [ "${4:-}" = reproducao ]; then  # a folha da reprodução sai no relógio da reprodução (A10)
      sincjson="$E0/relatorios/$rodada/reproducoes/$5.sinc.json"; folha="$E0/eventos/reproducoes/$5.folha.events.csv"
      rotulo="$5"
    fi
    d=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['deslocamento_ms'])" "$sincjson")
    mkdir -p "$EV/folha"
    set +e; python3 "$FERR/folha.py" "$E0/textos/$id.txt" "$E0/esperado/$nome.expected.csv" "$E0/esperado/$nome.words.csv" \
      --deslocamento-ms "$d" -o "$folha" 2> "$EV/folha/$rotulo.stderr.txt"; rc=$?; set -e
    log "folha $rotulo: rc=$rc (pontos de escuta em evidencias/folha/$rotulo.stderr.txt)"
    rc_aceito "$rc" 1 ;;
  metricas)
    rodada="$2"; nome="$3"; rel="$E0/relatorios/$rodada"
    traj="$E0/trajetorias/$nome"; ev="$E0/eventos/$nome.events.csv"; saida="$nome"; sincjson="$rel/$nome.sinc.json"
    if [ "${4:-}" = reproducao ]; then
      rep="$5"; traj="$E0/trajetorias/reproducoes/$rep"; ev="$E0/eventos/reproducoes/$rep.events.csv"
      rel="$rel/reproducoes"; saida="$rep"; sincjson="$rel/$rep.sinc.json"
    fi
    d=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['deslocamento_ms'])" "$sincjson")
    folha="$E0/eventos/$nome.folha.events.csv"
    [ "${4:-}" = reproducao ] && folha="$E0/eventos/reproducoes/$5.folha.events.csv"
    eventos=("$ev"); [ -e "$folha" ] && eventos+=("$folha")
    for par in ":$d" ".m200:$((d - 200))" ".p200:$((d + 200))"; do
      suf="${par%%:*}"; desl="${par##*:}"
      set +e; python3 "$FERR/metricas.py" "$traj.trajectory.csv" "$E0/esperado/$nome.expected.csv" "${eventos[@]}" \
        --deslocamento-ms "$desl" --hipoteses "$traj.hypotheses.csv" --json "$rel/$saida$suf.json" > "$rel/$saida$suf.md"
      rc=$?; set -e
      log "metricas $saida$suf (d=$desl): rc=$rc"
      rc_aceito "$rc" 1
    done ;;
  *) echo "uso: ver o cabeçalho" >&2; exit 2 ;;
esac
