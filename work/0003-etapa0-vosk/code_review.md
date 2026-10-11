**Veredito: REPROVADO**

# Revisão — 0003-etapa0-vosk (rodada 2)

**Revisor:** revisor · **Data:** 2026-10-11 · **Base do diff:** 92f8518 (rodada 1, REPROVADO) → b40841d (`work/0003-etapa0-vosk`)

Resumo: os dois bloqueantes da rodada 1 estão fechados. Reapliquei as duas quebras de cada um e as quatro ficaram vermelhas. A mudança de produção do OPR-027 e do OPR-029 está correta: em `Preparing → onStop → fim da carga` o microfone não abre e a sessão não começa. Todos os comandos passaram, com as contagens esperadas. A reprovação vem de uma guarda da mesma área que continua sem teste. Com a leitura em `Paused`, a captura fica aberta (TRD Máquina de estados, "Microfone na pausa"). Um `onStop` real nesse estado tem de chamar `stopCapture` (TRD §Segundo plano, tabela "Ida real ao segundo plano | Paused: stopCapture"; spec D9). Tirei o `Paused` dessa guarda de duas formas diferentes e as 220 suítes continuaram verdes. Ou seja, nenhum teste impede que o microfone fique aberto com o app em segundo plano. Eu devia ter listado esse operando na rodada 1: a quebra R21 tirava a guarda inteira e não cada operando. O código de hoje está certo. O retrabalho é um teste.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest --rerun-tasks` (em `etapa0/`): `verifyDebugMergedManifest: OK … -> android.permission.RECORD_AUDIO` e `verifyReleaseMergedManifest: OK … -> android.permission.RECORD_AUDIO`, BUILD SUCCESSFUL, exit=0. A rodada 2 não muda manifesto nem dependência (`git diff --stat 92f8518..HEAD`) | OK |
| Texto não persiste nem vai para log | `grep -rnE "rememberSaveable\|SavedStateHandle\|DataStore\|MediaRecorder\|SharedPreferences\|onSaveInstanceState\|putExtra\|ClipboardManager\|println\|printStackTrace\|Timber\|Log\." app/src/main --include='*.kt'`: só dois comentários que dizem "nunca `rememberSaveable`" (`FieldSync.kt:7`, `ReaderScreen.kt:80`). O `FieldSync` é uma classe comum, guardada com `remember { FieldSync(state.text) }`, e o campo usa `remember { mutableStateOf(...) }`. Não há `Saver` nem estado salvo, e os dois vivem dentro do ramo `Idle`. Na rotação o estado é recriado a partir do `state.text` do controlador | OK |
| Nenhum áudio gravado | A rodada 2 não toca `speech/` nem `export/` em `src/main` | OK |
| `align/` sem Android | `git diff 92f8518..HEAD -- etapa0/core/src/main`: 0 linhas. `grep -rnE "^import (android\|androidx)" core/src/main`: vazio | OK |
| Contratos do TRD inalterados | `core/src/main` sem diff. Em `src/main` só mudaram `ReaderController.kt` (+17/−4), `ReaderScreen.kt` (+12/−2) e `FieldSync.kt` (novo, 37), todos do `:app` da etapa0 | OK |
| `SessionId` preso ao reconhecedor | Bloqueantes 1 e 2 da rodada 1 reaplicados: B1a, B1b, B2a e B2b **vermelhos** (abaixo). Variantes minhas no produtor (V7 Partial e V13 Final com `SessionId(nextSession - 1)`) e no consumidor (V5, V6 e V12 aceitando evento sem sessão ativa): todas vermelhas | OK |
| Cursor nunca abaixo de `highWater − 3` | O cursor continua mudando só por `Aligner.advance` e `reposition`. O ramo novo do OPR-027 só chama `reposition(0)`, igual ao início normal | OK |
| Só Compose | `find app/src -path '*res/layout*'` e `grep -rnE "import android\.(view\|widget)\.\|AndroidView\|setContentView" app/src/main`: vazios | OK |
| Laço próprio, sem `SpeechService` | `grep -rn SpeechService app/src`: vazio | OK |
| Modelo no LFS como ponteiro | `git lfs ls-files`: 15. `git cat-file -s HEAD:<arquivo>`: de 127 a 133 bytes nos 15. `git lfs fsck`: OK. Maior blob em `92f8518..HEAD`: `impl_plan.md`, com 336 KB | OK |

## Mudança de produção da rodada 2 (OPR-027 e OPR-029)

`ReaderController.kt:158-171,253-267`: o `onActivityStop` em `Preparing` só marca `backgrounded`. Quando o `prepare()` termina, `startReading` lê `backgrounded` uma vez (`inBackground`). Se estiver marcado, não chama `startCapture` nem `beginSession`. Cria a gravação, o alinhador e o `reposition(0)` e entra em `Paused` com `capturing = false`. Percorri os caminhos:

- `Preparing → onStop(false) → fim da carga`: `Paused`, chamadas do motor só `["prepare"]`, `activeSession = null`, sem captura. Na volta, o `onActivityStart` zera `backgrounded` e, como a fase é `Paused`, reabre a captura. O Ouvir chama `resume`, que abre uma sessão nova. Nenhuma sessão se perde, porque nenhuma foi aberta. Teste: `onStopDeVerdadeEnquantoOModeloCarrega_…`.
- `Preparing → onStop → onStart → fim da carga`: `backgrounded` volta a `false`, e a leitura começa com o app em primeiro plano. O microfone só abre com a Activity visível. Foi uma escolha declarada no registro da construção. Ver o achado 3. Teste: `voltaAntesDoFimDaCarga_…`.
- `Preparing → onStop(true)` (rotação): ignorado. Teste: `rotacaoEnquantoOModeloCarrega_…`.
- `Preparing → onStop → carga falha`: `Idle` com o erro, e o `backgrounded` velho é zerado no `onStart` sem efeito, porque a fase é `Idle`.
- A fila (`limitedParallelism(1)`) libera durante o `prepare` suspenso, e é isso que permite ao `onActivityStop` rodar no meio. A leitura de `backgrounded` vem depois do `prepare`, como precisa. Os comandos que poderiam competir (`setText`, ajustes, `onPause`, `onStopReading`, toque) já recusam `Preparing` pela guarda de fase.

`FieldSync` e `ReaderScreen`: o campo mostra o que a pessoa digitou na hora. Os ecos atrasados do controlador são consumidos em ordem pelo `indexOf` (um `StateFlow` conflado pula ecos intermediários), e só um texto que não veio do campo (um chip) substitui o valor. O `onValueChange` do overload com `String` só dispara quando o texto muda. A fiação `LaunchedEffect(state.text)` não tem teste na JVM. Fica para o aparelho na 0005, como foi declarado.

## Spec e plano

| Critério | Implementação | Teste / comando (rodado pelo revisor) |
|---|---|---|
| A1 | sem mudança | `./gradlew assembleDebug assembleRelease bundleRelease --rerun-tasks`: BUILD SUCCESSFUL, 110 tarefas executadas, exit=0. `app-release.aab` com 49.590.809 bytes |
| A2 | — | `:app` **220**, `build-logic` **27**, `:core` **118**. Contei nos XMLs: 0 falhas, 0 erros, 0 ignorados. `@Ignore\|@Disabled`: vazio. lint (`:app:lintDebug`, `:core:lintJvm`) e ktlint (`ktlintMainSourceSetCheck`, `ktlintTestSourceSetCheck` e scripts de `:app` e `:core`) executados e verdes |
| A5 | — | `core/src/main` sem diff |
| B1–B3 | sem mudança | `verifyMergedManifest` OK (acima) |
| B5 | sem mudança | `verify16kAlignment`: **FAILED, exit=1**, só pelas 4 `libvosk.so` de 32 bits com LOAD 4096 (armeabi-v7a e x86, no APK e no AAB). `zipalign -c -P 16 -v 4`: Verification successful. É o previsto, OPR-024 |
| C1 | `VoskEngine.Session` | Partial, Final **e** Error travados: `erroDoReconhecedorComSessaoTrocadaNoMeioDoBloco_saiComOIdAntigo` (B1a, B1b, V7, V13 vermelhos) |
| D1 | `ReaderController.onHypothesis` | `parcialAtrasadoDaSessaoAnterior_naoChegaAoReconciliador` e exportação com `0,1,partial,"ouvir bem",0` (B2a, B2b, V6 vermelhos) |
| D6 (privacidade da exportação) | `TestTextCatalog` | `textoDeTesteComAlgoAcrescentado_naoIdentifica` (T29 `startsWith` vermelho). OPR-026 fechado |
| D9 (ciclo de vida) | `ReaderController.onActivityStop/onActivityStart` | Coberto a partir de `Reading`, de `Preparing` e de `Idle`, e na rotação com a leitura pausada. **`onStop` real a partir de `Paused` sem teste (achado 1)** |

Desvios do plano: justificados. A única mudança de produção que não é teste é a do OPR-027 e do OPR-029, que o próprio `code_review` da rodada 1 pediu, feita com teste primeiro (o registro mostra `expected:<Paused> but was:<Reading>` antes da correção). O plano foi atualizado com as quebras Q2-R*, Q2-M* e Q2-I*.

## Testes (Q2 e Q3)

Script em scratchpad: aplica uma troca exata por vez, confere que o texto aparece uma vez só, apaga `app/build/test-results`, roda `:app:testDebugUnitTest --continue`, lê os XMLs (220 casos por execução), marca "não compilou" e restaura com `git checkout -- <arquivo>` num `finally`. `git status --short` no fim: limpo.

| ID | Troca (uma por vez) | Resultado |
|---|---|---|
| B1a | `VoskEngine.kt:269` `Error(id, …)` → `Error(synchronized(lock) { current }?.id ?: id, …)` | vermelho (`erroDoReconhecedorComSessaoTrocadaNoMeioDoBloco_saiComOIdAntigo`) |
| B1b | idem → `Error(SessionId(nextSession - 1), …)` | vermelho (o mesmo) |
| B2a | `ReaderController.kt:346` Partial com `active ?: event.session` | vermelho (`parcialAtrasado…_naoChegaAoReconciliador`, `eventosAtrasados…Accepted0`) |
| B2b | `:370` `fromActive = session == active \|\| (!isFinal && active != null)` | vermelho (os mesmos 2) |
| T26 | `SessionId(nextSession++)` → `SessionId(nextSession)` | vermelho (4) |
| T47 | `release()`: `kept?.decoder?.close()` → `Unit` | vermelho |
| T53 | `spare == null && !released` → `spare == null` | vermelho |
| TM1 | `if (capture !== lost) return` → `if (false) return` | vermelho |
| TM2 | `count == 0` → `count == 1` | vermelho |
| T15 | `stopCapture`: `capture = null` → `Unit` | vermelho |
| T08 | `check(state == Ready)` → `check(state != Idle)` | vermelho |
| T29 | `TestTextCatalog` `it.value == text` → `text.startsWith(it.value)` | vermelho |
| T27 | `setText` sem `vocabulary = null` | vermelho |
| TR23 | `setText` sem `recording = null` | vermelho |
| TR21 | `onActivityStop` sem a guarda de fase | vermelho |
| TI4 | `val inBackground = backgrounded` → `false` | vermelho (2) |
| TI4b | tirar o ramo `if (phase == Preparing)` do `onActivityStop` | vermelho (2) |
| TI6 | `firstPartialMs = now - sessionStartedAt` → `sessionStartedAt - now` | vermelho (2) |
| TI7 | `next.cursor != previous?.cursor` → `previous?.highWater` | vermelho |
| TFS | `FieldSync.received` sem `pending.clear()` | vermelho |
| V1 (meu) | `if (!inBackground && !startCapture())` → `if (!startCapture())`: abre o microfone em segundo plano | vermelho |
| V2 (meu) | `beginSession()` no ramo de segundo plano | vermelho (2) |
| V4 (meu) | `onActivityStart` só reabre se `capturing` | vermelho (2) |
| V5 (meu) | `fromCapture = event.session == null \|\| active == null` | vermelho |
| V6 (meu) | `fromActive = active == null \|\| session == active` | vermelho (2) |
| V7 (meu) | `Partial(SessionId(nextSession - 1), …)` no `Session.feed` | vermelho |
| V8 (meu) | coluna `accepted` da exportação com `fromActive` (ignora o limiar) | vermelho |
| V9 (meu) | `FieldSync` `indexOf` → `lastIndexOf` | vermelho |
| V10 (meu) | `FieldSync` sem `if (text == value) return false` | vermelho (2) |
| V11 (meu) | `onActivityStart` sem `backgrounded = false` | vermelho |
| V12 (meu) | `onError` aceita com `\|\| active == null` | vermelho |
| V13 (meu) | `Final(SessionId(nextSession - 1), …)` | vermelho |
| **V3 (meu)** | `onActivityStop`: `if (phase == Preparing)` → `if (phase == Preparing \|\| phase == Paused)`: em `Paused` só marca e **não fecha a captura** | **VERDE** (220/220, achado 1) |
| **V3b (meu)** | guarda `phase != Reading && phase != Paused` → `phase != Reading`: `onStop` em `Paused` não faz nada | **VERDE** (220/220, achado 1) |

Ao todo foram 36 quebras: as 4 dos bloqueantes, 16 das tabelas dos construtores e 16 minhas. 34 ficaram vermelhas. As 2 verdes são o mesmo ponto, o achado 1.

**Mutantes declarados equivalentes.** Li a justificativa de cada um contra o código. Motor (20): aceitos. Os três "pares redundantes" (`ended` em `end()`, `decoderReleased` em `release()`) são de fato redundantes entre si, e o construtor mostra os pares vermelhos (linhas 396–398 da tabela). `Error(null` → `current?.id` no `open()` que falha é inalcançável, porque não há captura e, portanto, não há sessão. As âncoras `^`/`$` com `matches()` e `name`/`toString()` de enum são equivalentes de verdade. Interface (12): aceitos, com uma ressalva. O `limitedParallelism(1) → 2` não é "equivalente". Ele não é testável com o despachante de uma thread só, e é ele que garante que comandos e eventos não correm juntos, o que sustenta a regra 7. Não bloqueia, porque é uma constante estrutural e o TRD a nomeia, mas o rótulo está errado (achado 4). Nos 11 "não compila", os textos de troca eram inválidos e não são mutantes vivos.

**Testes novos (Q3), não tautológicos.** `erroDoReconhecedor…` usa `onAccept` + `failOnAccept` e faz a troca de sessão real dentro do bloco. `parcialAtrasado…` olha o efeito escondido: a cauda continua vazia depois do parcial novo igual e só recebe palavras quando há prefixo novo. `onStopDeVerdadeEnquantoOModeloCarrega…` usa o `prepareGate` para segurar a carga e confere a lista exata de chamadas do motor. `FieldSyncTest` cobre o conflado, a fila de ecos repetidos e o texto externo. `TestTextCatalogTest` cobre sufixo, prefixo, linha a mais, recorte e caixa trocada. O falso de assets ficou mais estrito (`require(path.startsWith("textos/T"))`). Nenhum teste compara um valor com ele mesmo. O antigo `META_KEYS` contra `META_KEYS` foi trocado pelos literais de D11.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | **BLOQUEANTE** | `etapa0/app/src/main/kotlin/.../viewmodel/ReaderController.kt:162-170`; teste em `.../viewmodel/ReaderLifecycleTest.kt` | O `onStop` real com a leitura em `Paused` não tem teste. Em `Paused` a captura fica aberta (TRD Máquina de estados, "Microfone na pausa"). A ida ao segundo plano tem de fechá-la (TRD §Segundo plano: "Sem serviço em primeiro plano … não há captura em segundo plano"; tabela "Ida real ao segundo plano \| Paused: stopCapture"; spec D9: "Com `false`, chama os dois e a tela fica pausada"). Os testes de `onStop` real partem de `Reading` (`onStopDeVerdade_…`, `voltaDoSegundoPlano_…`), de `Preparing` e de `Idle`. O único teste a partir de `Paused` é o da rotação. Tirar o `Paused` da guarda, ou desviá-lo para o ramo novo de `Preparing`, deixa o microfone aberto com o app em segundo plano, e tudo continua verde. É exatamente o risco do OPR-027, por outro caminho, e é a área que a rodada 2 mexeu | V3 e V3b: 220/220 verdes | Em `ReaderLifecycleTest`, um teste `onStopDeVerdadeComALeituraPausada_fechaACaptura_eNaVoltaReabre`: `load`, `listen`, `pause`, `onActivityStop(false)`, conferir que `stopCapture` está nas chamadas, `capturing == false`, `Paused`; depois `onActivityStart` reabre a captura e continua `Paused`. Conferir V3 e V3b vermelhos |
| 2 | MENOR | `etapa0/app/src/main/kotlin/.../ui/FieldSync.kt:24-36` | Quando o `StateFlow` conflata para um valor que repete um eco anterior (digitar "a", "ab" e apagar para "a", e só "a" chegar), o `indexOf` consome só o primeiro "a" e deixa "ab" e "a" pendentes. Um texto externo igual a um eco velho seria engolido. Na etapa0 o único texto externo são os chips dos textos de teste, então não há efeito prático | Leitura do código | Consumir até o **último** igual quando o valor recebido é igual a `value`, com teste. Ou aceitar e documentar. OPR-030 |
| 3 | MENOR | `ReaderController.kt:174-179,254` | `Preparing → onStop → onStart → fim da carga` começa a leitura sem toque. O microfone abre em primeiro plano, então não há violação. Mas o TRD §Segundo plano ("na volta, a leitura continua Paused … o orador toca para continuar") não fala do caso da carga. Decidir isso para o `PrompterViewModel` do app, onde há contagem regressiva | Teste `voltaAntesDoFimDaCarga_aLeituraComecaNormalmente` fixa o comportamento atual | Registrar a decisão no TRD (Máquina de estados, Preparing/Countdown) antes do app. OPR-030 |
| 4 | MENOR | `work/0003-etapa0-vosk/evidencias/quebras-q2-rodada2-interface.txt`, linha 21 | `limitedParallelism(1) → 2` está rotulado como "equivalente na JVM". Na verdade é um mutante que esse despachante não tem como testar, e é ele que sustenta a regra 7 (comandos e eventos numa fila só) | Tabela do construtor | Corrigir o rótulo. Se quiser travar, um teste com despachante de várias threads e uma barreira. OPR-030 |
| 5 | MENOR | `etapa0/app/src/test/.../speech/EngineHarness.kt:22,27`; `.../speech/VoskEngineRobustnessTest.kt:56,58,128` | Avisos de `@OptIn` de coroutines-test continuam no lado do motor (o `ReaderHarness` foi resolvido). Só em teste, não bloqueia | Saída do `compileDebugUnitTestKotlin` (`w: …needs opt-in`) | `@OptIn(ExperimentalCoroutinesApi::class)`. Já está no OPR-029, motor-voz |

OPR-026 fechado (verificado). OPR-027 corrigido na etapa0 e verificado, mas o achado 1 é a lacuna irmã. Continua aberto para o `PrompterViewModel`. OPR-028 completo: R13, R17, R20, R21, R22, R23, R26 e R27 têm teste, e conferi as quebras T47, TR21, TR23 e T27 e as linhas 70, 73 e 74 da tabela do motor. OPR-029(b) resolvido, (a) parcial.

## Saída real do `verify16kAlignment` (rodado pelo revisor, rodada 2)

```
> Task :app:verify16kAlignment FAILED
verify16kAlignment: app-release.apk: lib/armeabi-v7a/libvosk.so LOAD 4096
verify16kAlignment: app-release.apk: lib/x86/libvosk.so LOAD 4096
verify16kAlignment: zipalign -c -P 16 -v 4 app-release.apk -> Verification successful
verify16kAlignment: app-release.aab: base/lib/armeabi-v7a/libvosk.so LOAD 4096
verify16kAlignment: app-release.aab: base/lib/x86/libvosk.so LOAD 4096
verify16kAlignment: AAB app-release.aab -> 2 problema(s)
    - app-release.apk: lib/armeabi-v7a/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)
    - app-release.apk: lib/x86/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)
    - app-release.aab: base/lib/armeabi-v7a/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)
    - app-release.aab: base/lib/x86/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)
BUILD FAILED in 2s
exit16k=1
```

Todas as outras `.so` (arm64-v8a, x86_64 e as `libjnidispatch.so` e `libandroidx.graphics.path.so` das 4 ABIs) estão em LOAD 16384.

## Não verificado (rodada 2)

- A fiação Compose do `FieldSync` (`LaunchedEffect(state.text)`, recomposição, digitação real no teclado). Não há teste de UI nem aparelho. Fica para a 0005, critério E.
- `Preparing → onStop` com o `prepare()` real do Vosk numa thread de IO e o ciclo de vida real da Activity. Os testes usam o `prepareGate` do falso.
- O mesmo que na rodada 1: critério E inteiro, cota de LFS no push real e `ThreadCaptureDriver` sob corrida.

---

# Histórico: rodada 1

Veredito da rodada 1: REPROVADO (bloqueantes 1 e 2, fechados na rodada 2).

## Revisão — 0003-etapa0-vosk (rodada 1)

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
