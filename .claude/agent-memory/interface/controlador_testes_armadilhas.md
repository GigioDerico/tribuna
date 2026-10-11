---
name: controlador-testes-armadilhas
description: 0003 rodada 2 — armadilhas ao testar o ReaderController na JVM (relógio virtual, eco assíncrono do campo, mutantes que só vivem com estado divergente)
metadata:
  type: project
---

- O relógio virtual do teste fica em 0: quase toda medida de tempo (prepare, beginSession, firstPartialMs, latência 2/3) passa verde com `-`→`+` ou operandos trocados. Solução no `ReaderHarness`: `clockSkew` (soma ao relógio), `tick` (cada leitura do relógio avança) e ganchos `onPrepare`/`onBeginSession`. Sempre comece o cenário com o relógio longe de 0.
- Mutantes cursor/highWater só vivem com cursor < highWater e um evento que não move (recuo + ruído `final("zzz")`). Ver `ReaderGuardsTest.recuoSeguidoDeRuido_…`.
- `prepare()` suspende: para agir durante `Preparing` use `RecordingEngine.prepareGate` (CompletableDeferred). A fila `limitedParallelism(1)` libera na suspensão (origem do OPR-027).
- Sequência mínima que move o cursor em T01: 3 parciais crescentes e o 4º com 4 palavras (`ouvir bem é uma`); 2 parciais só entregam `ouvir bem` e o cursor fica em 0.
- Varredura: gerar mutantes por script (operadores + trocas de variável) rende ~300 em 4 arquivos; ~2 s cada, mas laço infinito vira timeout (use 60 s). `NOCOMPILE` é descartado. Mutantes equivalentes típicos: valor inicial sobrescrito no `beginSession`, `collector?.cancel()` antes de `release()` que fecha o canal, guarda dupla (`capturing` + estado do motor).
- Campo de texto com valor assíncrono: `ui/FieldSync` (eco pendente consumido pelo primeiro igual; texto externo zera os pendentes). Fiação Compose (`LaunchedEffect(state.text)`) não é testável na JVM.
- O ViewModel é testável na JVM com `Dispatchers.setMain` + `ViewModelStore().put/clear()`.
