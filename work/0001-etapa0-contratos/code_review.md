**Veredito: REPROVADO**

# Revisão — 0001-etapa0-contratos

**Revisor:** revisor · **Data:** 2026-10-10 · **Base do diff:** `ce3c8a2` → `6a05f4e` (branch `work/0001-etapa0-contratos`)

Motivo único da reprovação: o achado 1 (BLOQUEANTE). O código de produção e o fake estão corretos hoje; o que falta é o teste que prende a regra 7 no caminho `Error` do reconhecedor. Uma quebra proposital minha (fake lendo a sessão ativa no callback de erro) passou com tudo verde, o que contraria o critério 3 do spec ("esse teste fica vermelho se o fake passar a ler uma variável de 'sessão atual' no callback"). A correção é um teste; o resto da entrega pode ser reaproveitado sem mudança.

Todos os comandos abaixo foram rodados por mim, de `etapa0/`, em 2026-10-10 ~21:29 BRT, com a árvore limpa antes e depois (`git status --porcelain` vazio). Não usei as evidências do construtor como prova; comparei com elas depois.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `./gradlew verifyMergedManifest` (debug e release `OK ... -> android.permission.RECORD_AUDIO`); li os dois manifestos mesclados em `app/build/intermediates/merged_manifest/{debug,release}/.../AndroidManifest.xml` (Q4): uma só `uses-permission`, `RECORD_AUDIO`. `bash scripts/verificar-manifesto-negativo.sh`: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK e CAMERA reprovados com a mensagem, só RECORD_AUDIO passa, `script-exit=0`, `app/src/debug` removido | OK |
| Texto não persiste nem vai para log | `grep -rnE "Log\.\|println\|print\(\|Timber\|FileOutputStream\|writeText\|SavedStateHandle\|rememberSaveable\|DataStore\|AudioRecord\|Intent"` em `etapa0/**/*.kt(s)` fora de `build/`: nenhum resultado. Não há texto do discurso nesta tarefa; as palavras de teste vêm de `etapa0/textos/T01.txt` e só passam pelo `Channel` em memória. O `logger.lifecycle` do `build-logic` só imprime caminho de arquivo, nome de permissão e nome de APK. Evidências e memória do `motor-voz` sem segredos (grep por password/secret/token/signing/`org.gradle.java.home`) | OK |
| Áudio não gravado fora de `etapa0/` | Não há `AudioRecord`, Vosk nem bytes de áudio no diff; o fake simula áudio com chamadas de método | OK |
| `align/` sem Android | `grep -rnE "import (android\|androidx)\." etapa0/core/src` → `exit=1`; `:core` usa `kotlin.jvm` (só `com.android.lint` de Android). Quebra proposital: criei `align/QuebraR4.kt` com `import android.util.Log`; `:core:compileKotlin` falhou com `Unresolved reference 'android'` — o build impõe a regra, não só o grep. Arquivo apagado | OK |
| Contratos do TRD inalterados (ou CIA aprovada) | Comparei linha a linha `speech/{SpeechEngine,SessionId,SpeechEvent,EngineState,EngineError}.kt` e `align/{Token,Aligner,AlignState,Move,AlignParams}.kt` com TRD §Contratos (linhas 61–128): nomes, tipos, ordem, nulabilidade, padrão `vocabulary = null`, `@JvmInline`, `sealed interface`, ordem dos enums e comentários iguais. Diferença só de formatação do ktlint (desvio 2). `AlignParams` sem padrões inventados. `Aligner.advance/reposition` lançam `NotImplementedError` (spec, decisão 3) | OK |
| `SessionId` preso ao reconhecedor | `FakeSpeechEngine.kt:62` cria `FakeRecognizer(SessionId(nextSession++), vocabulary)`; `partial/final/error` (linhas 112–122) usam o `session` capturado, nunca `active`. Código correto. **Mas o caminho `error` não tem teste que o prenda** (achado 1) | OK no código; teste insuficiente |
| Cursor nunca abaixo de `highWater − 3` | `Aligner` é só esqueleto; nenhum movimento implementado | N.A. |
| Só Compose | `find etapa0 -path '*/res/layout*'` vazio; únicos XML versionados: `AndroidManifest.xml` e `res/values/strings.xml`; nenhuma View | OK |

## Spec e plano

| Critério de aceite | Implementação | Teste / verificação (minha saída) |
|---|---|---|
| 1. `assembleDebug` com JDK 21 e SDK 36 | `gradle/gradle-daemon-jvm.properties`, `local.properties` | `./gradlew --version`: `Daemon JVM: Compatible with Java 21 ... (from gradle/gradle-daemon-jvm.properties)`, Launcher 21.0.10; `assembleDebug ... --rerun-tasks` → BUILD SUCCESSFUL, 85 tarefas executadas |
| 2. `testDebugUnitTest` verde; ciclo de vida, cada `SpeechEvent`, `SessionId` crescente | `FakeSpeechEngine`, contratos | 45 testes, 0 falhas, 0 ignorados (XMLs gerados às 21:29 por mim): `SpeechContractShapeTest` 3, `AlignContractShapeTest` 4, `FakeSpeechEngineTest` 17, `CoreFromAppTest` 1, `ManifestPermissionCheckTest` 8, `ElfAlignmentTest` 7, `NativeLibScanTest` 5. Partial/Final/Level/Error e `sessionId_cresceACadaBeginSession_eNuncaSeRepete` presentes |
| 3. Evento de sessão encerrada chega com id antigo; teste vermelho se o fake ler "sessão atual" no callback | `FakeRecognizer` com id capturado | **Parcial.** Partial e Final: cobertos (`callbackAtrasado_comOutraSessaoAtiva_chegaComOIdAntigo` ficou vermelho na minha quebra). Error: nenhum teste emite `Error` de reconhecedor antigo; minha quebra no `error` ficou verde (achado 1) |
| 4. Depois de `endSession`, sem Partial/Final; Level continua | `endSession` zera `active`; `hearPartial/hearFinal` usam `active?` | `endSession_naoEmiteMaisPartialNemFinal_eLevelContinua` verde; quebra B do construtor conferida no diff da evidência |
| 5. `align` sem Android, JVM pura | `:core` com `kotlin.jvm` | grep `exit=1`, `plugin-exit=1`; quebra R4 não compila |
| 6. `verifyMergedManifest` passa e falha com proibidas/qualquer outra | `build-logic` `ManifestPermissionCheck` + `VerifyMergedManifestTask` por variante | Passa no projeto; script negativo com 4 reprovações e 1 passagem; minha quebra (deixar `WAKE_LOCK` passar) deixou `wakeLock_falha` e `listaTodasAsProibidas_emOrdemAlfabetica` vermelhos |
| 7. `verify16kAlignment` existe, roda e falha explícito sem as libs | `Verify16kAlignmentTask`, `NativeLibScan`, `ElfAlignment` | `./gradlew verify16kAlignment` → `exit=1`, `zipalign -c -P 16 -v 4 app-release-unsigned.apk -> Verification successful` e `- app-release-unsigned.apk: nenhuma biblioteca nativa em lib/<abi>/; exigidas: libjnidispatch.so, libvosk.so`. Falha explícita, como pede o spec |
| 8. `lint ktlintCheck` sem erros | — | BUILD SUCCESSFUL; SARIF: 0 erros, 3 avisos (`DataExtractionRules`, `MissingApplicationIcon` no `:app`; `GradleDependency` no `:core`), os mesmos do Registro |
| 9. Versões só do catalog | `gradle/libs.versions.toml` | `grep -rnE '[0-9]+\.[0-9]+' etapa0 --include='*.gradle.kts'` → `exit=1`; `:core` runtime: `kotlin-stdlib:2.4.21`, `kotlinx-coroutines-core:1.11.0`; `:app` releaseRuntimeClasspath só `:core` e transitivas destas |
| 10. Sem layout XML; `uses-permission` só `RECORD_AUDIO` | `app/src/main/AndroidManifest.xml` | `find` vazio; grep com uma linha, `RECORD_AUDIO` |

Desvios do plano: justificados.
- Desvio 1 (KDoc com `/*`): necessário, só comentário. Aceito.
- Desvio 2 (`ktlintFormat`): conferi `Aligner.kt` e os `build.gradle.kts`; só formatação, o comentário do TRD em `reposition` desceu uma linha. Aceito.
- Desvio 3 (evidências da Q2 com diff e `<failure message>`): melhora a evidência. Aceito.
- Desvio 4 (trailer `Claude Opus 5.5` em vez de `Claude Sonnet 5.5`): aceito. O CLAUDE.md não fixa trailer; o trailer usado é a atribuição configurada no ambiente e descreve o modelo que de fato escreveu os commits, o que é o propósito da linha. Uma instrução de agente pedindo para atribuir a outro modelo não deveria prevalecer sobre a atribuição verdadeira. Fica a inconsistência com o commit-base `ce3c8a2` (Sonnet), sem ação.
- R1–R10 conferidos no Registro; R10 (verify16k vermelho por desenho) confirmado com a minha saída.

OPR-002 (`dataExtractionRules`) e OPR-003 (ktlint fora do `build-logic`) estão registrados corretamente, com origem e severidade. A transferência de dono ("motor-voz (transferir ao arquiteto)") ainda não foi confirmada no registro; até lá o dono é o `motor-voz`.

## Testes (Q2 e Q3)

Quebras feitas por mim (locais, desfeitas com `git checkout`/`rm`, árvore limpa conferida depois de cada uma):

| # | Invariante | Quebra | Resultado |
|---|---|---|---|
| A' | Regra 7, `Final` | `FakeRecognizer.final` passa a emitir `SessionId(nextSession - 1)` (contador de "sessão atual" do motor) | `:core:test` vermelho: `callbackAtrasado_comOutraSessaoAtiva_chegaComOIdAntigo` (`expected ...Final(session=SessionId(value=1)...` ). 24 testes, 1 falha |
| E | Regra 7, `Error` | `FakeRecognizer.error` passa a emitir `Error(active?.session, cause)` | **`:core:test` verde, BUILD SUCCESSFUL.** Nenhum teste pegou (achado 1) |
| M | Regra 1 | `ManifestPermissionCheck.problems` deixa passar `*WAKE_LOCK` | `testDebugUnitTest` vermelho: `wakeLock_falha`, `listaTodasAsProibidas_emOrdemAlfabetica` |
| R4 | Regra 4 | `import android.util.Log` em `align/` | `:core:compileKotlin` falha (`Unresolved reference 'android'`) |

Quebras A, B e C do construtor: conferi os diffs e as mensagens nas evidências; a minha A' reproduz a A por outro caminho.

Q3: as suítes têm contagem não vazia (45 casos, conferidos nos XML); `verifyMergedManifest` exige `RECORD_AUDIO` presente (não passa sobre manifesto vazio, teste `semRecordAudio_falha`); `verify16kAlignment` falha explícito com zero `.so` em vez de passar.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | BLOQUEANTE | `etapa0/core/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/FakeSpeechEngineTest.kt` (falta o caso); código sob teste em `etapa0/core/src/testFixtures/.../speech/FakeSpeechEngine.kt:120-122` | O caminho `Error` do reconhecedor não tem teste de callback atrasado. Se o fake passar a ler a sessão ativa ao emitir `Error`, nada fica vermelho. O TRD §Contratos (regras, linha 90) faz o ViewModel filtrar `Partial`, `Final` **e** `Error` pelo id, e a suíte "Sessões e leitura" do TRD vai depender deste fake para testar o descarte de callback atrasado. O critério 3 do spec exige que o teste fique vermelho com o fake lendo "sessão atual" no callback; para `error` isso não acontece. `error_doReconhecedorTrazASessao...` não pega porque emite com a sessão ainda ativa | Quebra E acima: `Error(active?.session, cause)` → `BUILD SUCCESSFUL` | Acrescentar um teste em `FakeSpeechEngineTest`, p.ex. `callbackAtrasado_errorDaSessaoAntiga_chegaComOIdAntigo`: `antiga = beginSession()`, guarda o reconhecedor, `endSession()`, `nova = beginSession()`, `reconhecedorAntigo.error(EngineError.Internal)` e espera `Error(antiga, Internal)`. Mostrar a quebra E vermelha e anexar como evidência. Dono: `motor-voz` |
| 2 | MENOR | `etapa0/build-logic/.../AndroidVerificationsPlugin.kt:15-29` | `verifyMergedManifest` não está ligado ao ciclo do build (`check`, `assemble*`, `bundle*`): `./gradlew assembleRelease`/`bundleRelease` com uma lib que traga `INTERNET` passaria. TRD §Manifesto (linha 290) diz "o build verifica o manifesto final mesclado". O spec só pede a tarefa e o CLAUDE.md a roda explicitamente, por isso não bloqueia | Leitura do plugin; nenhum `dependsOn`/`finalizedBy` em tarefa de build | Decidir no PLAN da 0003: `check.dependsOn(verifyMergedManifest)` e fazer `assemble`/`bundle` de release depender da verificação da variante. Registrado como OPR-004, dono `arquiteto` |
| 3 | MENOR | `etapa0/build-logic/.../Verify16kAlignmentTask.kt:50-52`; `AndroidVerificationsPlugin.kt:34` | `verify16kAlignment` verifica o APK de release, não o AAB, que é o que vai ao Play (TRD §Distribuição; Q4 cita "o `.so` dentro do AAB"). A leitura ELF das `.so` seria igual, mas o `zipalign -c -P 16` do APK não diz nada sobre o pacote que o Play gera. Fora do escopo da 0001 (sem Vosk) | Leitura da tarefa: entrada `SingleArtifact.APK` | Na 0003/release: verificar também as `.so` em `base/lib/<abi>/` do AAB (`SingleArtifact.BUNDLE`) e documentar que o `zipalign` do pacote final é do Play. Registrado como OPR-005, dono `arquiteto` |
| 4 | MENOR | `etapa0/build-logic/.../ElfAlignment.kt:38-45`; `NativeLibScan.kt:36-41`; `AndroidVerificationsPlugin.kt:30-31` | Robustez: (a) com `e_phentsize` menor que a estrutura (ELF malformado), `buf.getInt/getLong` lança `IndexOutOfBoundsException`, que escapa do `catch (IllegalArgumentException)` do `NativeLibScan`; a tarefa ainda falha (não passa em silêncio), mas com stack trace em vez do problema listado. (b) Com flavors, duas variantes `release` registrariam `verify16kAlignment` duas vezes e a configuração quebraria | Leitura | (a) validar `phentsize >= 56/32` no `require` ou capturar `IndexOutOfBoundsException`; (b) registrar por variante e agregar, como o manifesto. Registrado como OPR-006, dono `motor-voz` |

## Não verificado

- Execução em aparelho ou emulador: nenhum conectado, e a tarefa não tem tela nem instrumentados (spec, fora do escopo).
- `verify16kAlignment` contra `libvosk.so`/`libjnidispatch.so` reais: não há Vosk nesta tarefa (é da 0003). Verifiquei só a falha explícita e os unitários com ELF sintéticos.
- Compatibilidade formal AGP 9.4.1 × KGP 2.4.21 fora do intervalo testado pela JetBrains (R1): não houve erro nem aviso na minha rodada, mas não há garantia além disso.
- Não reli o `impl_plan.md` inteiro linha a linha contra o código das Tarefas 5 e 6 (o plano tem 2.256 linhas); revisei o código real do diff e os critérios do spec com a minha saída.
