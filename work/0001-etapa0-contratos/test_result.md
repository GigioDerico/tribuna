**Resultado: PASSOU**

# Testes — 0001-etapa0-contratos

**Responsável:** qa-publicacao · **Data:** 2026-10-10 · **Commit testado:** 13fc389 (branch `work/0001-etapa0-contratos`, árvore limpa antes e depois)

Todos os comandos foram rodados por mim, de `etapa0/`, entre 21:36 e 21:42 BRT. Evidências em `work/0001-etapa0-contratos/evidencias/qa-*`.

## Ambiente

| Item | Valor |
|---|---|
| Aparelho(s) | Nenhum. Sem aparelho nem emulador; a tarefa não tem tela nem instrumentados (spec, fora do escopo). Testes na JVM do MacBook Air (macOS), JDK 21, SDK 36, Gradle 9.7.1 |
| Ambiente acústico | N.A. (sem áudio nesta tarefa) |
| Voz(es) | N.A. |
| Distância do microfone | N.A. |
| Brilho / modo avião | N.A. |

## Execução

| Suíte / comando | Resultado | Evidência |
|---|---|---|
| `./gradlew assembleDebug testDebugUnitTest lint ktlintCheck verifyMergedManifest --rerun-tasks` | BUILD SUCCESSFUL, exit=0, 85 tarefas executadas | `evidencias/qa-tudo-verde.txt` |
| Contagem nos XML de resultado | 46 testes, 0 falhas, 0 erros, 0 ignorados | `evidencias/qa-contagem-testes.txt` |
| `bash scripts/verificar-manifesto-negativo.sh` | script-exit=0; INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK e CAMERA reprovados; só RECORD_AUDIO passa | `evidencias/qa-manifesto-negativo.txt` |
| `./gradlew verify16kAlignment` | exit=1 por desenho: `nenhuma biblioteca nativa em lib/<abi>/; exigidas: libjnidispatch.so, libvosk.so` | `evidencias/qa-verify16k.txt` |
| Greps estáticos e manifestos mesclados | ver critérios 5, 9, 10 | `evidencias/qa-checagens-estaticas.txt` |
| `:core` runtimeClasspath e `:app` debugRuntimeClasspath | só kotlin-stdlib 2.4.21 e coroutines 1.11.0, ambos do catalog | `evidencias/qa-dependencies.txt` |
| `connectedDebugAndroidTest` | Não executado (sem aparelho; sem instrumentados na tarefa) | N.A. |

## Critérios de aceite

| # | Critério | Como foi executado | Resultado | Evidência |
|---|---|---|---|---|
| 1 | `assembleDebug` BUILD SUCCESSFUL, JDK 21, SDK 36 | tudo-verde com `--rerun-tasks`; `compileSdk 36` e `jvm 21` vêm do catalog | PASSOU | `qa-tudo-verde.txt`, `qa-dependencies.txt` (catalog) |
| 2 | `testDebugUnitTest` verde, um teste por item (ciclo de vida, cada `SpeechEvent`, `SessionId` crescente) | 46 testes: SpeechContractShape 3, AlignContractShape 4, FakeSpeechEngine 18, CoreFromApp 1, ManifestPermissionCheck 8, ElfAlignment 7, NativeLibScan 5; testes por nome presentes (`partial_`, `final_`, `level_`, `error_`, `sessionId_cresce...`, `cicloDeVida_...`) | PASSOU | `qa-contagem-testes.txt` |
| 3 | Callback atrasado chega com o id antigo; vermelho se o fake ler "sessão atual" | Os três caminhos (Partial/Final, Error) têm teste (`callbackAtrasado_*`). Quebras de outros autores A, D, E' não repeti. Minha quebra F (Q2) mostra o conjunto pegando fala sem sessão | PASSOU | `qa-q2-quebra-f-fala-sem-sessao.txt`; evidências `q2-quebra-a`, `q2-quebra-d` do construtor |
| 4 | `endSession`: sem Partial/Final; Level continua | `endSession_naoEmiteMaisPartialNemFinal_eLevelContinua` verde; fica vermelho com a quebra F | PASSOU | `qa-contagem-testes.txt`, `qa-q2-quebra-f-fala-sem-sessao.txt` |
| 5 | Nenhum import android/androidx em `align`; compila e testa na JVM sem plugin Android | `grep -rnE "import (android\|androidx)\." core/src` exit=1; 5 arquivos `.kt` em `align/` main; `:core` usa só `kotlin.jvm` (+ `android.lint`, que é verificador, não plugin de app) | PASSOU | `qa-checagens-estaticas.txt` |
| 6 | `verifyMergedManifest` passa, falha com INTERNET/ACCESS_NETWORK_STATE/WAKE_LOCK, e com qualquer outra permissão | Passa no projeto (debug e release, `-> android.permission.RECORD_AUDIO`); script negativo: 4 reprovações (3 proibidas + CAMERA) e 1 passagem; manifesto de teste removido ao fim | PASSOU | `qa-tudo-verde.txt`, `qa-manifesto-negativo.txt` |
| 7 | `verify16kAlignment` existe, roda, falha explícito sem as duas bibliotecas | exit=1 com mensagem explícita; `zipalign -c -P 16` do APK passou. Execução contra Vosk real é da 0003 | PASSOU (escopo da tarefa) | `qa-verify16k.txt` |
| 8 | `lint ktlintCheck` sem erros | dentro do tudo-verde, exit=0 | PASSOU | `qa-tudo-verde.txt` |
| 9 | Versões só do catalog; sem literal nos `build.gradle.kts` | `grep -rnE '[0-9]+\.[0-9]+' --include='*.gradle.kts'` em 6 arquivos: exit=1 (nenhum); árvores de dependência mostram stdlib 2.4.21 e coroutines 1.11.0, iguais às do catalog | PASSOU | `qa-checagens-estaticas.txt`, `qa-dependencies.txt` |
| 10 | Sem layout XML; `uses-permission` só `RECORD_AUDIO` | Sem `res/layout*`; XMLs versionados: só `AndroidManifest.xml` e `values/strings.xml`. Manifestos mesclados lidos em `build/intermediates` (Q4): debug e release com uma única `uses-permission`, RECORD_AUDIO | PASSOU | `qa-checagens-estaticas.txt` |

## Métricas da Etapa 0

Não aplicável a esta tarefa (sem gravações nem trajetórias).

Regra de contagem (Q3 e Q7), declarada para cada número desta tarefa:

| Número | Regra de contagem | Amostras |
|---|---|---|
| 46 testes | soma dos atributos `tests` de cada `<testsuite>` nos XML de resultado gerados nesta execução (mtime 21:36), conferida contra a contagem de elementos `<testcase>` (46); `skipped`, `failures` e `errors` somados = 0 | 7 suítes |
| 85 tarefas | linha `actionable tasks` do Gradle com `--rerun-tasks`; todas executadas, nenhuma de cache | 85 |
| Permissões | `uses-permission` nos dois manifestos mesclados reais (debug, release): 1 em cada. Teste não passa sobre manifesto vazio: `semRecordAudio_falha` exige RECORD_AUDIO | 2 manifestos |
| 16 KB | 0 bibliotecas nativas no APK; a tarefa falha por exigir 2 e achar 0. Nenhum alinhamento foi medido (não há `.so`); isso fica para a 0003 | 0 `.so` |
| Literais de versão | grep em 6 arquivos `.gradle.kts` versionados (excluídos `build/`) | 6 arquivos |

## Teste falso (Q2)

Quebras minhas, novas, diferentes de A–E do construtor e do revisor. Ambas locais e desfeitas (`git checkout`, `git status --porcelain` vazio depois, `app/src/debug` removido):

- **F** (`FakeSpeechEngine.hearPartial/hearFinal` reaproveitam o último reconhecedor quando não há sessão ativa, ou seja, deixam de descartar fala sem sessão). Vermelho: `FakeSpeechEngineTest > endSession_naoEmiteMaisPartialNemFinal_eLevelContinua FAILED` (`AssertionError at FakeSpeechEngineTest.kt:185`), 25 testes no `:core`, 1 falha. Evidência: `evidencias/qa-q2-quebra-f-fala-sem-sessao.txt`.
- **G** (`ManifestPermissionCheck.problems` passa a ignorar `ACCESS_NETWORK_STATE`). Vermelho em dois níveis: unitário `ManifestPermissionCheckTest > accessNetworkState_falha FAILED` (20 testes do build-logic, 1 falha) e o script negativo contra o projeto real passou a mostrar `verifyDebugMergedManifest: OK ... ACCESS_NETWORK_STATE, RECORD_AUDIO`, com exit do script 1. Evidência: `evidencias/qa-q2-quebra-g-manifesto.txt`.
- Após restaurar: `testDebugUnitTest verifyMergedManifest --rerun-tasks` BUILD SUCCESSFUL (`qa-pos-restauracao.txt`) e árvore limpa.

## Não testado

- Aparelho/emulador, `connectedDebugAndroidTest`, qualquer áudio, ambiente acústico e vozes: não há aparelho e a tarefa não tem tela.
- `verify16kAlignment` com `libvosk.so` e `libjnidispatch.so` reais, em todas as ABIs: é da 0003. Aqui só a falha explícita e os unitários com ELF sintéticos (7 + 5 casos).
- Verificação do AAB (a tarefa lê o APK de release; OPR-005) e ligação do manifesto ao ciclo `check`/`bundle` (OPR-004): abertos no `opr_register.md`, não exigidos nesta tarefa.
- Publicação (`bundleRelease`, `release_check.md`): não solicitada e sem autorização humana na pasta.
- Compatibilidade formal AGP 9.4.1 com KGP 2.4.21: sem erro nem aviso nas minhas execuções, mas sem garantia além disso.
