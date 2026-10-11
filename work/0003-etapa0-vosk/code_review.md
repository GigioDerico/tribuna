**Veredito: REPROVADO**

# Revisão — 0003-etapa0-vosk

**Revisor:** revisor · **Data:** 2026-10-11 · **Base do diff:** 4fc6325 (merge-base com `main`) → c774cd2 (`work/0003-etapa0-vosk`)

Resumo: build, manifesto, 16 KB, R8, LFS, privacidade e Compose estão corretos, e eu mesmo confirmei tudo por comando. A reprovação vem de dois caminhos da regra 7 que ficam verdes quando quebrados. O primeiro é o `Error` do reconhecedor carimbado com a sessão atual do motor (C1). O segundo é o `Partial` atrasado chegando ao reconciliador (D1). Os dois são critérios do spec que nomeiam a quebra, e é o mesmo padrão que reprovou a 0001. O código de produção nos dois pontos está certo hoje. Falta o teste que o trava. O retrabalho são dois testes, sem mudança de código.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest --rerun-tasks`: `verify{Debug,Release}MergedManifest: OK … -> android.permission.RECORD_AUDIO`. Li os 6 manifestos gerados em `app/build/intermediates` (merged_manifest, merged_manifests, packaged_manifests; debug e release): 1 permissão cada, nenhum `<permission>`. `aapt2 dump permissions app-release.apk`: só `RECORD_AUDIO`. `scripts/verificar-manifesto-negativo.sh`: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK e CAMERA reprovadas, só RECORD_AUDIO passa, `exit=0`. Quebra Q2-C2 (tirar o `tools:node="remove"` do `<permission>`): `verifyReleaseMergedManifest FAILED - declaração de permissão não permitida: …DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | OK |
| Texto não persiste nem vai para log | `grep -rnE "rememberSaveable\|SavedStateHandle\|DataStore\|MediaRecorder\|SharedPreferences\|onSaveInstanceState\|putExtra\|ClipboardManager\|println\|printStackTrace\|Timber" etapa0/app/src/main`: vazio. `grep -rn "Log\." etapa0/app/src/main`: vazio. As escritas em arquivo são só duas: `ModelInstaller.kt:82`, que copia o modelo, e `ExportWriter.kt:40`, que grava os CSVs da exportação. A exportação só existe quando `TestTextCatalog.identify` acha um texto idêntico (`==`). O nome do arquivo passa pela regex `^<id>(_[a-z0-9]+)*$`. O Kaldi fica em `vosk_set_log_level(-2)` antes de carregar o modelo, o R8 tem `-assumenosideeffects` para `Log`, o `ViewModel` não tem `SavedStateHandle` e o `OutlinedTextField` usa o valor do estado do controlador | OK (ver achado 3, IMPORTANTE) |
| Nenhum áudio gravado | `WavAudioSource` só abre `FileInputStream` e lê. `MicAudioSource` só lê o `AudioRecord`. Não há `MediaRecorder`. O `ExportFiles` gera só `hypotheses`, `level` (rms), `trajectory`, `events` e `meta` | OK |
| `align/` sem Android | `git diff main...HEAD --stat -- etapa0/core/src/main`: vazio. `grep -rnE "^import (android\|androidx)" etapa0/core/src/main`: vazio | OK |
| Contratos do TRD inalterados (ou CIA aprovada) | `core/src/main` sem diff. O TRD §Manifesto recebeu exatamente o texto da CIA aprovada no portão 1 (opção A), no mesmo commit do manifesto (de3ce85) | OK |
| `SessionId` preso ao reconhecedor | `VoskEngine.kt:263-269`: todo evento sai com o `id` do `Session`. O `current` só é lido em `:218` para rotear o áudio. `ReaderController.kt:357,388` filtra pelo `active`. As quebras E1, E2, R2, R3, R4, R7, R8, R9, R10 e R32 ficaram vermelhas. **R1/R1b (Error) e R6/R6b (Partial no controlador) ficaram verdes** | OK no código; **teste falta (achados 1 e 2)** |
| Cursor nunca abaixo de `highWater − 3` | O controlador só move o cursor por `Aligner.advance` (núcleo da 0002, intocado) e por `reposition` (início e toque em palavra, que a regra 6 permite) | OK |
| Só Compose | `find etapa0/app/src -path "*res/layout*"` e `grep -rn "res/layout"`: vazios. Os XMLs de `res/` são `values/strings.xml` e os dois `xml/` de backup. `grep -rnE "import android\.(view\|widget)\.\|AndroidView\|setContentView"`: vazio | OK |
| Laço próprio, sem `SpeechService` | `grep -rn "SpeechService" etapa0/app/src`: vazio. O `ThreadCaptureDriver` chama `Capture.step`, que lê o `AudioSource` e entrega ao `Decoder` | OK |
| Modelo no LFS como ponteiro | `git lfs ls-files`: 15 arquivos de `model-pt/`. `git cat-file -s HEAD:<arquivo>`: de 127 a 133 bytes em todos (ponteiros). `git rev-list --objects main..HEAD` não tem nenhum blob acima de 1 MB. `git lfs fsck`: OK. `uuid` no APK = `vosk-model-small-pt-0.3 sha256=6e1ce909…93b7` | OK |

## Spec e plano

| Critério | Implementação | Teste / comando (rodado pelo revisor) |
|---|---|---|
| A1 | `app/build.gradle.kts`, ABIs filtradas (A7) | `assembleDebug assembleRelease bundleRelease --rerun-tasks`: BUILD SUCCESSFUL, 110 tarefas executadas. APK e AAB com `model-pt/uuid` e `libvosk.so` + `libjnidispatch.so` nas 4 ABIs. AAB com 49.589.180 bytes |
| A2 | — | Verde. `:app` 118, `build-logic` 27, `:core` 118, 0 falhas, 0 ignorados (contei nos XMLs). `@Ignore\|@Disabled`: vazio. Lint do `:app`: só `MissingApplicationIcon` |
| A3 | `libs.versions.toml` | Dependências só pelo catalog, com as versões de A1 do plano |
| A4 | `scripts/baixar-modelo.sh`, `uuid` | LFS conferido (tabela acima) |
| A5 | — | `core/src/main` sem diff |
| A6, A7 | — | Greps vazios, a não ser as 2 escritas esperadas |
| A8 | `.editorconfig` | `runKtlintCheckOverMainSourceSet` e `…TestSourceSet` executados. Com uma violação minha em `app/src/test/.../QuebraRevisor.kt`, `ktlintTestSourceSetCheck FAILED`. Arquivo apagado |
| B1–B3 | manifesto e `res/xml` | Tabela acima. As regras de backup excluem os 9 domínios em `cloud-backup`, `device-transfer` e `full-backup-content` |
| B4 | `AndroidVerificationsPlugin.kt:24,35-38` | Os `--dry-run` de `check`, `assembleRelease`, `bundleRelease` e `assembleDebug` listam a verificação. Quebra Q2-C1 (tirar o `configureEach`): `assembleRelease` e `bundleRelease` deixam de listar. Restaurado e reconferido |
| B5 | `Verify16kAlignmentTask.kt`, `NativeLibScan.kt` | `verify16kAlignment` **FAILED (exit=1)**, como previsto. As 4 reprovações são só `libvosk.so` de armeabi-v7a e x86 com LOAD 4096, no APK e no AAB. Todo o resto está em 16384 (saída anexa abaixo). `zipalign -c -P 16 -v 4`: Verification successful. OPR-024 aberto. Fecha pelo caminho "reprova por alinhamento" |
| B6 | `proguard-rules.pro` | `mapping.txt`: `com.sun.jna.Native -> com.sun.jna.Native:` e `org.vosk.Recognizer -> org.vosk.Recognizer:` |
| C1 | `VoskEngine.Session` | Partial e Final travados (E1, E2). **Error sem teste de troca de sessão (achado 1)** |
| C2–C11 | `VoskEngine`, `ModelInstaller`, `WavAudioSource`, `VoskJson`, `AudioLevel`, `Vocabulary` | Testes da tabela do plano. Amostra de quebras vermelha (abaixo) |
| D1 | `ReaderController.onHypothesis` | Final atrasado travado. **Partial atrasado chegando ao reconciliador não fica vermelho (achado 2)** |
| D2–D9 | `ReaderController` | Testes da tabela do plano. Amostra de quebras vermelha |
| E | — | Aparelho, fica para a 0005 (spec) |

Desvios do plano: justificados. São três: o modelo tem 15 arquivos e não 16 (erro de contagem do plano, conteúdo e SHA conferem), o AAB tem +29/+30 bytes e o manifesto foi aplicado por engano e refeito na Tarefa 2. Nenhum muda o resultado. O código é o do plano, e as quebras do plano que refiz bateram com o que o plano previa.

Avisos do compilador (`@OptIn` de coroutines-test em `EngineHarness.kt:22,27` e `ReaderHarness.kt:98,112`, `lateinit` desnecessário em `ReaderHarness.kt:80`): **não são bloqueantes**. Ficam só no código de teste, o build não usa `allWarningsAsErrors`, e não escondem nem enfraquecem nenhuma verificação. Foram para o OPR (achado 9).

## Testes (Q2 e Q3)

Script em scratchpad: aplica cada troca exata, roda `./gradlew testDebugUnitTest --continue`, lê os XMLs do `:app` e do `build-logic` (145 testes por execução), distingue "vermelho" de "não compilou" e restaura com `git checkout -- <arquivo>`. `git status` ao fim de cada lote: só a memória do motor-voz, que já estava modificada antes da revisão.

Quebras do plano refeitas (19), todas **vermelhas** nos testes previstos: E1, E2, E5, E6, E9, E10, V1, V5, V6, V8, V12, V17, V18, V20, V21, X1, X2, S2 e B1. Por comando: Q2-C1 e Q2-C2 também vermelhas.

Quebras do revisor (36):

| ID | Troca | Resultado |
|---|---|---|
| R1 | `VoskEngine.kt:269` `Error(id, …)` → `Error(synchronized(lock) { current }?.id ?: id, …)` | **VERDE** (achado 1) |
| R1b | idem → `Error(SessionId(nextSession - 1), …)` | **VERDE** (achado 1) |
| R2 | `:127` `Error(id, Internal)` → `Error(null, …)` | vermelho (`reconhecedorQueNaoCria_…`) |
| R3 | `Session(id, …)` → `Session(SessionId(nextSession), …)` | vermelho (6) |
| R4 | `captureLost` `Error(null, …)` → `Error(ended?.id, …)` | vermelho (`permissaoPerdidaDuranteACaptura_…`) |
| R5 | `Final` lê `partialJson()` | vermelho (2) |
| R6 | `ReaderController.kt:333` Partial com `active ?: event.session` | **VERDE** (achado 2) |
| R6b | `:357` `fromActive = session == active \|\| (!isFinal && active != null)` | **VERDE** (achado 2) |
| R7 | `:334` Final com `active ?: event.session` | vermelho (3) |
| R8 | `fromActive = active != null` | vermelho (3) |
| R9 | `onError` `event.session == active` → `active != null` | vermelho |
| R10 | `endSession` não zera `active` | vermelho |
| R32 | `fromCapture = event.session == null \|\| event.session != active` | vermelho |
| R11, R12 | `stopCapture` / `captureLost` sem `ended?.end()` | vermelhos |
| R14 | `end()` libera durante o `feed` (`!feeding` → `true`) | vermelho (2) |
| R15, R16 | `reset()` removido; guardado com outro vocabulário não fecha | vermelhos |
| R18, R19 | `prepare` falho sem `Failed`; sem cache do modelo | vermelhos |
| R24 | cauda `maxOf` → `minOf` | vermelho |
| R25 | `onPause` sem `endSession()` | vermelho (6) |
| R28 | erro com a leitura parada vira `Paused` | vermelho |
| R30 | nome aceita qualquer ID (`^[A-Z0-9]+…`) | vermelho |
| R31 | `DECLARATION_TAGS` só `permission` | vermelho (`grupoEArvoreDePermissao_falham`) |
| R29 | `TestTextCatalog.kt:20` `it.value == text` → `text.startsWith(it.value)` | **VERDE** (achado 3) |
| R27 | `setText` não zera o `vocabulary` em cache | **VERDE** (achado 5) |
| R17 | `release()` não fecha o reconhecedor guardado | **VERDE** (achado 6) |
| R21 | `onActivityStop` sem a guarda de fase | **VERDE** (achado 7) |
| R13, R20, R22, R23, R26 | guardas defensivas (`if (!active)` inicial do `step`, `count < 0`, `onStart` sem checar a fase, `setText` não zera `recording`, toque em `Idle`) | **VERDES** (achado 8) |

Contagem mínima (Q3): 118 testes no `:app`, 27 no `build-logic` e 118 no `:core`, todos executados com `--rerun-tasks`. O D3 exige mais de 100 linhas de trajetória e o D1 exige `cursor > 0` antes da pausa. O C1 Partial/Final usa `onAccept` para trocar a sessão no meio do bloco, o que torna a corrida real e determinística.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | **BLOQUEANTE** | `etapa0/app/src/main/kotlin/.../speech/VoskEngine.kt:267-269`; teste em `.../speech/VoskEngineSessionTest.kt` | O `Error` que o reconhecedor emite quando o `accept` lança não tem teste de troca de sessão. O C1 diz "Todo `Partial`/`Final`/`Error` daquele reconhecedor leva o id dele … O teste fica vermelho se o código ler a sessão atual do motor na hora de emitir". O caso é alcançável: exceção no nativo enquanto um toque em palavra troca a sessão. Com a sessão atual, o `Error` sairia com o id novo, e o `ReaderController` pausaria a sessão nova por um erro da antiga (regra 7) | R1 e R1b: 145/145 verdes. `excecaoDoReconhecedor_…`, `reconhecedorComExcecao_…` e `todoEventoDoReconhecedor_…` só testam a exceção dentro da própria sessão. Mesmo padrão do achado bloqueante da 0001 | Teste no estilo de `resultadoProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo`: `old.onAccept = { endSession(); second = beginSession() }` com `old.failOnAccept = true` (o `FakeDecoder` chama `onAccept` antes de lançar, `SpeechFakes.kt:23-24`). Esperado: `[Error(first, Internal)]`. Conferir que R1 e R1b ficam vermelhos |
| 2 | **BLOQUEANTE** | `etapa0/app/src/main/kotlin/.../viewmodel/ReaderController.kt:333,357`; teste em `.../viewmodel/ReaderSessionTest.kt` | O D1 diz que "um `Partial` e um `Final` atrasados da sessão anterior não mudam o cursor **nem chegam ao reconciliador**. O teste fica vermelho se o filtro for removido". Removido o filtro só para parciais, tudo fica verde. O primeiro parcial só grava o `previous` no `PartialReconciler` (`onPartial`, `:14-18`) e não muda cauda nem cursor, que são as únicas coisas que o teste olha. E o teste de exportação só manda o `Final` atrasado. Um parcial antigo no reconciliador da sessão nova contamina o prefixo comum do próximo parcial (regra 7, TRD Reconciliação 7) | R6 e R6b: 145/145 verdes | Em `eventosAtrasadosEntramNaExportacaoComOIdOriginalEAccepted0`, mandar também `old.partial(...)` e exigir a linha `…,1,partial,"…",0`. Ou testar que, depois de um parcial antigo, o primeiro parcial da sessão nova com o mesmo prefixo não entrega palavra nenhuma. Conferir R6 e R6b vermelhos |
| 3 | IMPORTANTE | `etapa0/app/src/main/kotlin/.../export/TestTextCatalog.kt:20`; `.../export/TestTextCatalogTest.kt` | A guarda de privacidade da exportação (D6, TRD §Privacidade nos testes) não tem teste para "texto de teste + acréscimo". Trocar `==` por `startsWith` fica verde. Com essa regressão, um texto de teste seguido de um trecho pessoal habilitaria a exportação da transcrição falada. O código atual está certo | R29 verde | `assertNull(catalog.identify(TestFiles.text(id) + "x"))` para cada ID. Recomendo fazer no mesmo retrabalho (uma linha). OPR-026 |
| 4 | IMPORTANTE | `ReaderController.kt:232-255` (`startReading`) | O `prepare()` suspende (`withContext(IO)`), e o `limitedParallelism(1)` libera a fila na suspensão. Um `onActivityStop(false)` durante `Preparing` cai na guarda de fase (`:159`) e é descartado. Quando o modelo termina de carregar, o controlador abre o microfone e entra em `Reading` com o app em segundo plano. Na volta, `backgrounded` é `false`, então a leitura continua. Contraria o TRD §Segundo plano (ir ao segundo plano pausa). Alcançável na primeira carga do modelo (segundos). Sem teste | Leitura do código. `onActivityStop` só aceita `Reading`/`Paused` | Lembrar o `onStop` em `Preparing` (marcar `backgrounded` e, ao fim do `prepare`, ficar em `Paused` sem abrir a captura), com teste no `ReaderLifecycleTest`. OPR-027 |
| 5 | MENOR | `ReaderController.kt:97` | O teste não trava o `vocabulary = null` no `setText`. Sem ele, ao trocar de texto com o vocabulário ligado, o reconhecedor recebe a lista do texto anterior, e a medição da 0005 sai errada | R27 verde | Teste: ligar o vocabulário, ler o T01, parar, carregar o T02, Ouvir, e conferir que a lista do `beginSession` é a do T02. OPR-028 |
| 6 | MENOR | `VoskEngine.kt:150` | O `release()` fecha o reconhecedor guardado (estratégia `Reset`) sem teste. Sem isso, cada troca de estratégia ou fonte vaza um `Recognizer` nativo | R17 verde | Teste: `Reset`, sessão encerrada, `release()`, `assertTrue(decoder.closed)`. OPR-028 |
| 7 | MENOR | `ReaderController.kt:159` | A guarda de fase do `onActivityStop` não tem teste. Sem ela, `onStop` com a leitura parada leva a tela de `Idle` para `Paused` sem alinhador | R21 verde | Teste: `onActivityStop(false)` em `Idle` mantém `Idle` e não chama o motor. OPR-028 |
| 8 | MENOR | `VoskEngine.kt:207,215`; `ReaderController.kt:96,147,171` | Guardas defensivas sem teste (R13, R20, R22, R23, R26). Nenhuma toca regra inegociável: a segunda checagem de `active`, o `MicAudioSource`, que já lança com `count < 0`, e a UI, que não mostra Exportar nem o texto tocável em `Idle`, cobrem os casos hoje. O `onExport` não confere `canExport`: depende de `setText` zerar a `recording` (R23) | Quebras verdes | Decidir se ficam com teste ou se o KDoc as declara defensivas, como o plano fez com `captureLost`. OPR-028 |
| 9 | MENOR | `etapa0/app/src/test/.../speech/EngineHarness.kt:22,27`; `.../viewmodel/ReaderHarness.kt:80,98,112` | Avisos do compilador: API experimental de coroutines-test sem `@OptIn` e `lateinit` desnecessário. Só em teste, não bloqueia | Saída do `compileDebugUnitTestKotlin` | `@OptIn(ExperimentalCoroutinesApi::class)` nos dois harness e sem o `lateinit`. OPR-029 |
| 10 | MENOR | `etapa0/app/src/main/kotlin/.../ui/ReaderScreen.kt:79-80` | O `OutlinedTextField` usa `value = state.text`, atualizado de forma assíncrona (`setText` vai para a fila no `Dispatchers.Default`). Digitar rápido pode perder ou reordenar caracteres e fazer o cursor do campo saltar. Só na etapa0, onde os textos entram pelos chips. O app terá editor próprio | Leitura do código | Estado local do campo com `remember` (nunca `rememberSaveable`) e envio ao controlador, ou `setText` síncrono no estado. OPR-029 |

## Saída real do `verify16kAlignment` (rodado pelo revisor)

```
> Task :app:verify16kAlignment FAILED
verify16kAlignment: app-release.apk: lib/arm64-v8a/libandroidx.graphics.path.so LOAD 16384
verify16kAlignment: app-release.apk: lib/arm64-v8a/libjnidispatch.so LOAD 16384
verify16kAlignment: app-release.apk: lib/arm64-v8a/libvosk.so LOAD 16384
verify16kAlignment: app-release.apk: lib/armeabi-v7a/libandroidx.graphics.path.so LOAD 16384
verify16kAlignment: app-release.apk: lib/armeabi-v7a/libjnidispatch.so LOAD 16384
verify16kAlignment: app-release.apk: lib/armeabi-v7a/libvosk.so LOAD 4096
verify16kAlignment: app-release.apk: lib/x86/libandroidx.graphics.path.so LOAD 16384
verify16kAlignment: app-release.apk: lib/x86/libjnidispatch.so LOAD 16384
verify16kAlignment: app-release.apk: lib/x86/libvosk.so LOAD 4096
verify16kAlignment: app-release.apk: lib/x86_64/libandroidx.graphics.path.so LOAD 16384
verify16kAlignment: app-release.apk: lib/x86_64/libjnidispatch.so LOAD 16384
verify16kAlignment: app-release.apk: lib/x86_64/libvosk.so LOAD 16384
verify16kAlignment: zipalign -c -P 16 -v 4 app-release.apk -> Verification successful
verify16kAlignment: app-release.aab: base/lib/arm64-v8a/libandroidx.graphics.path.so LOAD 16384
verify16kAlignment: app-release.aab: base/lib/arm64-v8a/libjnidispatch.so LOAD 16384
verify16kAlignment: app-release.aab: base/lib/arm64-v8a/libvosk.so LOAD 16384
verify16kAlignment: app-release.aab: base/lib/armeabi-v7a/libandroidx.graphics.path.so LOAD 16384
verify16kAlignment: app-release.aab: base/lib/armeabi-v7a/libjnidispatch.so LOAD 16384
verify16kAlignment: app-release.aab: base/lib/armeabi-v7a/libvosk.so LOAD 4096
verify16kAlignment: app-release.aab: base/lib/x86/libandroidx.graphics.path.so LOAD 16384
verify16kAlignment: app-release.aab: base/lib/x86/libjnidispatch.so LOAD 16384
verify16kAlignment: app-release.aab: base/lib/x86/libvosk.so LOAD 4096
verify16kAlignment: app-release.aab: base/lib/x86_64/libandroidx.graphics.path.so LOAD 16384
verify16kAlignment: app-release.aab: base/lib/x86_64/libjnidispatch.so LOAD 16384
verify16kAlignment: app-release.aab: base/lib/x86_64/libvosk.so LOAD 16384
verify16kAlignment: AAB app-release.aab -> 2 problema(s)
> verify16kAlignment: alinhamento de 16 KB reprovado (TRD §Qualidade de código):
    - app-release.apk: lib/armeabi-v7a/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)
    - app-release.apk: lib/x86/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)
    - app-release.aab: base/lib/armeabi-v7a/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)
    - app-release.aab: base/lib/x86/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)
BUILD FAILED in 1s
exit=1
```

Outros comandos do revisor: `scripts/varrer-registerreceiver.sh` deu 77 bibliotecas e 13.076 classes. Só duas chamam o receptor: `ContextKt` (core-ktx, repassa a flag) e `MediaQuery` (compose-ui, flag 2 = EXPORTED). `exit=0`. `grep -rnE "registerReceiver|receiveBroadcasts|RECEIVER_NOT_EXPORTED" etapa0/app/src --include='*.kt'`: vazio.

## Não verificado

- Tudo do critério E (aparelho): permissão em tempo de execução, `AudioRecord` real, JSON real do Vosk (C6 é só documentação), carga do modelo, R8 rodando no aparelho, tela e rotação reais. Não há aparelho nesta revisão. É da 0005.
- Cota de LFS no `git push` real. Só existe o `--dry-run` do construtor. Não fiz push.
- O comportamento real do `ThreadCaptureDriver` sob corrida (join com limite de 1 s, `release` vindo da thread principal no `onCleared`). Os testes usam `ManualCaptureDriver`. Li o código e não achei defeito, mas não há prova por execução.
