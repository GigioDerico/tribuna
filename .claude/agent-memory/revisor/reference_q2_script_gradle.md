---
name: q2-script-gradle-etapa0
description: Como rodar lotes de quebras Q2 na etapa0 sem falso verde — script python em scratchpad, apagar test-results antes, distinguir "não compilou", git checkout depois
metadata:
  type: reference
---

Na 0003 (2026-10-11), o lote de quebras funcionou bem assim:

- O script fica em scratchpad. Ele recebe uma lista `(id, arquivo, texto_exato, troca)`, confere que o texto aparece uma vez só, grava, apaga `app/build/test-results/testDebugUnitTest` e `build-logic/build/test-results/test` e roda `./gradlew testDebugUnitTest --continue` (o raiz cobre `:app`, `:core` e o build incluído `build-logic`, 145 casos no app + build-logic). Depois lê os XMLs e restaura com `git checkout -- <arquivo>` num `finally`.
- Se não apagar os XMLs, um erro de compilação deixa os resultados antigos no lugar e a quebra parece "verde". O script marca "NÃO COMPILOU" quando o log tem `e: file://`.
- Cada execução leva uns 20 s. 50 quebras dão uns 17 min em segundo plano.
- Quebra por comando (manifesto, `dependsOn`): trocar com `python3 -I -c` e caminho por argumento. Heredoc com aspas triplas aninhadas quebrou uma vez. Depois de mexer no `build-logic`, rode o `--dry-run` de novo após o checkout para confirmar que voltou.
- No zsh, `--include=*.kt` sem aspas falha com "no matches found". Use `--include='*.kt'`.

Relacionado: [[q2-cobrir-todos-eventos-sessao]], [[q2-variantes-guardas-alinhador]].
