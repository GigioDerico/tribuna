---
name: ambiente-etapa0
description: Ambiente de QA da Etapa 0 do Tribuna (sem aparelho, comandos, armadilhas de shell)
metadata:
  type: project
---

Tarefa 0001 (2026-10-10): QA feito só por comando, sem aparelho/emulador, MacBook Air, JDK 21, Gradle 9.7.1, AGP 9.4.1, Kotlin 2.4.21. Resultado PASSOU, 46 testes (core 26 + build-logic 20).
Tarefa 0002 (2026-10-11): PASSOU, 118 testes em `:core`, também sem aparelho. L30 sintético: 3.399 chamadas, total 380,7 ms, máx 20,4 ms (varia ~10x entre execuções, JIT frio; não é medida de aparelho).

- Testes do build-logic (ManifestPermissionCheck etc.) rodam dentro de `./gradlew testDebugUnitTest` da raiz de `etapa0/`; não precisa de subcomando.
- `verify16kAlignment` falhar com "nenhuma biblioteca nativa" é o esperado até a 0003 (Vosk).
- `scripts/verificar-manifesto-negativo.sh` cria e remove `app/src/debug`; conferir que sumiu depois.
- O sandbox negou comando composto com `rm -rf` e gradle aninhado; usar passos simples e `git checkout --` para restaurar quebras.
- Em pipes `${PIPESTATUS[0]}` dentro de subshell com redirecionamento sai vazio; ler "BUILD FAILED"/"FAILED" na saída.
- Quebras Q2 já usadas na 0001: A–D (construtor), E' (revisor), F (hearPartial/hearFinal sem sessão), G (ManifestPermissionCheck ignora ACCESS_NETWORK_STATE). Variar nas próximas.
- Quebras Q2 da 0002 (em `Aligner.kt`, 13 vermelhas + Y4 verde conhecida): Q2-1, Q2-3, Q2-7, V4, V6, V10, W4, L129, L104b, L132b, L40c, L59a, L56b. Script de troca exata + `:core:test --rerun-tasks -q` + leitura dos XMLs + `git checkout --` roda a lista inteira em ~1 min e funcionou direto em Python no scratchpad (sem negação do sandbox).
- Em `etapa0/`, os XMLs de teste ficam em `core/build/test-results/test/`; contar tests/skipped/failures por regex neles.

Tarefa 0003 (2026-10-11): PASSOU (A–D), sem aparelho (`adb devices` vazio; critério E vai à 0005). Contagens: :app 228, :core 118, build-logic 27. AAB 49,6 MB, APK 74,2 MB; build limpo ~52 s. `verify16kAlignment` reprova só `libvosk.so` armeabi-v7a e x86 (LOAD 4096) no APK e AAB (OPR-024), `zipalign -P 16` do APK passa.
- `aapt2`/`zipalign` não estão no PATH: usar `~/Library/Android/sdk/build-tools/36.0.0/`.
- XMLs do `:app` em `app/build/test-results/testDebugUnitTest/`. Script de quebras em Python (scratchpad) rodou 22 quebras em ~4 min com `./gradlew testDebugUnitTest --continue -q` (cobre app e build-logic); texto de troca precisa ser único: `SpeechEvent.Error(id, EngineError.Internal)` aparece 2x em VoskEngine.kt, usar contexto `failed = true }\n ...`.
- ktlint negativo: o primeiro erro de `main` interrompe; para provar a fonte `test` rodar com `--continue` (ou separado).
- Quebras 0003 já usadas: Q2-C1, C2, R1, R1b, I1, I2, V3, V3b, I4a/b, E1, E2, E7, V1, V17, V9, V18, S1, S5, S6, S8, X3, R29, B1. Não refeitas: C2/C5/C8/C9/D5/D8 (testes). Variar na próxima.

Tarefa 0004 (2026-10-11, ferramentas Python da Etapa 0): PASSOU, sem aparelho, commit 5f6809f. Python 3.13.12, `testar.py` 149 testes em ~6 s, `:core` 122, `:app` 228. `vosk` NÃO instalado (rc 3 esperado no modo real; portão humano 6 na 0005). L30 `alinhar.py` pela CLI ~4,0 s (MacBook).
- No zsh, `grep ... --include=*.py` precisa de aspas (`'--include=*.py'`), senão "no matches found" (A11 do plano).
- Scripts do QA em scratchpad: CLI ponta a ponta (alinhar/timestamps/metricas) gera dados sintéticos só de `etapa0/textos/` via `roteiro.tokenizar`; quebras Python por troca exata de linha + `git checkout --` (~20 quebras em ~2 min); paridade Kotlin: quebra + `:core:test --rerun-tasks -q`, copiar `core/build/referencia-atual/*.tsv` para cópia `git archive` e rodar `python3 -I copia/etapa0/ferramentas/testar.py 'test_roteiro*.py'`.
- Armadilha minha: ao montar trajetória sintética, não reutilizar lista de tempos já mutada (tokens pulados com fim=None) em casos seguintes; e o `metricas.py` rejeita t_ms decrescente (ordenar antes).
- Quebras 0004 usadas: metricas.py :368 (`<`, `<= 2`, `<= 4`, invertido), :281 normais, :375 any, :338, :254, :310, :387, :389, :52, :34, :31; timestamps.py :63, :70, :71; alinhar.py :185, :110; roteiro.py :41, :91, :101; Kotlin NumberWords :54/:61/:68/:89, Tokenizer :19/:159, Normalizer :13. Variar na próxima.
