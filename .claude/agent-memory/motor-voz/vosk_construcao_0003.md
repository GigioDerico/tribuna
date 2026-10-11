---
name: vosk-construcao-0003
description: Como construir a 0003 (Vosk) a partir do plano gigante com extrator, pegadinhas do modelo/LFS/manifesto observadas nas Tarefas 1 a 11
metadata:
  type: project
---

Construído em 2026-10-11 (Tarefas 1 a 11): 82 testes `:app` + 27 `build-logic` verdes, 0 mudança de lógica no código do plano.

- O impl_plan tem 6.800 linhas (300 KB): não cabe no Read. Cada bloco de código é precedido por uma linha com o caminho em crase (`KT/`, `KTT/`, `BL/`, `BLT/` expandem para pacotes). Um script Python que pega o primeiro trecho em crase da linha anterior à cerca e grava o corpo funciona; cuidado: na linha do script da Tarefa 2 o primeiro trecho em crase é `registerReceiver`, não o caminho (gravou arquivo errado).
- Modelo `vosk-model-small-pt-0.3`: 14 arquivos no zip + `uuid` = 15 (o plano diz 16). Download pelo `baixar-modelo.sh` leva segundos; `git add` já gera ponteiros LFS (`git lfs ls-files` mostra `*`).
- Vosk real (JSON, custo de criar reconhecedor, vocabulário) ainda NÃO exercitado: testes JVM usam falsos; só o aparelho (0005) responde.
- `verifyMergedManifest` roda no `assembleRelease` etc. depois da Tarefa 1; com Compose o manifesto exige o `tools:node="remove"` da `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- aapt2 fica em `~/Library/Android/sdk/build-tools/36.0.0/` (`$ANDROID_HOME` não está definido no shell).
- Helper de resumo de testes: ler `app/build/test-results/testDebugUnitTest/*.xml` (atributos `tests`, `failures`).

**Why:** evita redescobrir o extrator e as contagens. **How to apply:** na Tarefa 16 e em qualquer reconstrução, reusar a mesma abordagem; confirmar versões do catalog antes.
