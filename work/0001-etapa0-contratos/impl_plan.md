# Plano — 0001-etapa0-contratos

**Autor:** arquiteto · **Spec:** `spec.md` (aprovado em 2026-10-10) · **Aprovação:** conferida pelo orquestrador em 2026-10-10 (portão automático: 10 de 10 critérios do spec com comando e resultado esperado; testes antes do código; seção de privacidade presente; nenhum contrato nem permissão alterados)

> **Para o agente de construção (`motor-voz`):** execute tarefa por tarefa, na ordem, marcando os checkboxes (`- [ ]`). Só comece com a conferência do orquestrador registrada no cabeçalho acima. Não chame subagentes nem outros agentes. Desvio do plano vai para o "Registro da construção" no fim, com o motivo. Se o código não couber num contrato do TRD, pare e escale ao `arquiteto` (CLAUDE.md, regra 5).

**Objetivo:** projeto Gradle `etapa0/` compilável com os contratos do TRD em Kotlin, o `FakeSpeechEngine` e as tarefas `verifyMergedManifest` e `verify16kAlignment`, tudo coberto por testes.

**Arquitetura:** três builds Gradle em `etapa0/`. `:core` é Kotlin/JVM puro (sem plugin Android) com os pacotes `speech` (contratos) e `align` (contratos e esqueleto do `Aligner`), e o `FakeSpeechEngine` no source set `testFixtures`. `:app` é um módulo Android mínimo, sem tela, que consome o `:core` e recebe as tarefas de verificação. `build-logic` é um build incluído com o plugin de convenção `tribuna.android.verifications`, cuja lógica (manifesto, ELF, varredura do APK) é Kotlin puro testado com JUnit.

**Stack:** Gradle 9.7.1 · AGP 9.4.1 (Kotlin embutido) · Kotlin Gradle Plugin 2.4.21 · kotlinx-coroutines 1.11.0 · JUnit 4.13.2 · ktlint-gradle 14.2.0 com ktlint 1.8.0 · JDK 21 (Temurin) · SDK 36, build-tools 36.0.0.

---

## Resumo

Primeiro o esqueleto Gradle com versões fixadas no catalog e o daemon preso ao JDK 21. Depois os contratos de `speech` e `align` exatamente como no TRD, com testes de forma que quebram a compilação se o contrato derivar. Em seguida o `FakeSpeechEngine` com testes primeiro (incluindo o callback atrasado da Q2) e, por fim, o `build-logic` com testes, o módulo `:app` e as duas tarefas de verificação, cada uma provada falhando.

## Versões escolhidas e fontes (consultadas em 2026-10-10)

| Item | Versão | Fonte | Observação |
|---|---|---|---|
| Gradle | 9.7.1 (19/08/2026) | https://gradle.org/releases/ e `https://services.gradle.org/versions/all` | 9.8.1 (07/10/2026) é a mais nova, mas o KGP 2.4.21 tem suporte completo até 9.7.x. SHA-256 da distribuição `bin`: `acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a` (https://services.gradle.org/distributions/gradle-9.7.1-bin.zip.sha256) |
| AGP | 9.4.1 (POM publicado em 18/09/2026) | https://developer.android.com/build/releases/gradle-plugin (9.4: Gradle mínimo 9.6.0, build-tools 36.0.0, JDK 17+, API máxima 37); `maven-metadata.xml` do Google Maven (9.4.1 é a estável mais nova; 9.5.0 está em alpha) | O AGP 9 traz Kotlin embutido e dependência de execução no KGP 2.2.10; versão maior é subida pelo classpath (https://developer.android.com/build/releases/agp-9-0-0-release-notes). Não aplicar `org.jetbrains.kotlin.android` (https://developer.android.com/build/migrate-to-built-in-kotlin) |
| Kotlin (KGP) | 2.4.21 (08/10/2026) | https://kotlinlang.org/docs/releases.html | Tabela de compatibilidade (https://kotlinlang.org/docs/gradle-configure-project.html): KGP 2.4.20–2.4.21 → Gradle 7.6.3–9.7.0, AGP 8.5.2–9.3.1. **AGP 9.4.1 está acima do intervalo testado pela JetBrains** (ver Riscos, R1) |
| kotlinx-coroutines (core e test) | 1.11.0 | `maven-metadata.xml` do Maven Central (`<release>1.11.0`) | |
| JUnit | 4.13.2 | `maven-metadata.xml` do Maven Central | Runner padrão do `testDebugUnitTest` |
| ktlint-gradle | 14.2.0 (12/03/2026) | https://github.com/JLLeitschuh/ktlint-gradle/releases | 14.1.0 adicionou suporte ao Kotlin embutido do AGP 9 (nova DSL); 13.1.0 adicionou Gradle 9 |
| ktlint | 1.8.0 | `maven-metadata.xml` de `com.pinterest.ktlint:ktlint-cli` | O padrão do plugin é 1.5; fixamos a mais nova |
| Compose BOM | não entra | — | Esta tarefa não tem tela; Compose entra na 0003 com a tela mínima |
| JDK | 21 (Temurin 21.0.10, instalado) | README (pré-requisitos) | AGP 9.4 exige JDK 17+; compatível |
| Build-tools | 36.0.0 (instalado) | Tabela do AGP 9.4 (padrão = 36.0.0) | Fixado no catalog para o AGP não tentar baixar outro e para achar o `zipalign` |

Compatibilidade AGP × Gradle × Kotlin × JDK 21: AGP 9.4.1 pede Gradle ≥ 9.6.0 (9.7.1 atende) e JDK ≥ 17 (21 atende). KGP 2.4.21 suporta Gradle até 9.7.x (atende) e AGP testado até 9.3.1 (não atende formalmente; ver R1 e o plano B com AGP 9.3.3).

## Ambiente (verificado pelo arquiteto em 2026-10-10)

- JDK 21 Temurin em `/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home` (também há 17 e 25).
- **`~/.gradle/gradle.properties` do usuário define `org.gradle.java.home` para o JDK 17.** O `gradle --version` mostrou `Daemon JVM: .../temurin-17.jdk (from org.gradle.java.home)`. Sem ação, o build rodaria no 17 e quebraria o critério "usando JDK 21". Solução no plano: `etapa0/gradle/gradle-daemon-jvm.properties` com `toolchainVersion=21`, que tem precedência sobre `JAVA_HOME` e `org.gradle.java.home` (https://docs.gradle.org/current/userguide/gradle_daemon.html: "If the Daemon JVM criteria configuration is provided, it takes precedence over JAVA_HOME and org.gradle.java.home"). **Não editar o `~/.gradle/gradle.properties`** (é do usuário e tem segredos de outros projetos; não abrir nem copiar o conteúdo).
- SDK em `~/Library/Android/sdk`: `platforms/android-36`, `build-tools/36.0.0` (com `zipalign`, que aceita `-c -P 16`; conferido rodando o binário), `platform-tools`. Sem `cmdline-tools`, sem NDK; nenhum dos dois é necessário.
- `ANDROID_HOME` e `ANDROID_SDK_ROOT` não estão definidos: o plano cria `etapa0/local.properties` (já no `.gitignore`).
- Não há `gradle` no PATH. Há distribuições em cache do wrapper: `~/.gradle/wrapper/dists/gradle-9.3.1-bin/23ovyewtku6u96viwx3xl3oks/gradle-9.3.1/bin/gradle`, usada só uma vez para gerar o wrapper 9.7.1.
- Não há `readelf`: a leitura dos segmentos ELF é feita em Kotlin no `build-logic` (sem ferramenta externa).
- Rede: necessária só na máquina de desenvolvimento, no primeiro build (distribuição do Gradle e dependências). Nada a instalar à mão.

## Arquivos que mudam

Todos os caminhos são relativos à raiz do repositório. Pacote base do `:core`: `io.github.gigioderico.tribuna.etapa0` (spec, decisão 1). Diretório de código: `etapa0/core/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/` (abreviado como `<core-main>`), testes em `etapa0/core/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/` (`<core-test>`) e fake em `etapa0/core/src/testFixtures/kotlin/io/github/gigioderico/tribuna/etapa0/` (`<core-fixtures>`).

| Arquivo | Mudança | Requisito / seção do TRD | Agente |
|---|---|---|---|
| `.gitignore` | Acrescenta `.kotlin/` (diretório de sessão do Kotlin 2.x) | — (higiene) | motor-voz |
| `etapa0/settings.gradle.kts` | Novo: repositórios, `includeBuild("build-logic")`, `:core`, `:app` | TRD §Stack (Gradle Kotlin DSL) | motor-voz |
| `etapa0/build.gradle.kts` | Novo: plugins `apply false` (põe AGP e KGP 2.4.21 no classpath raiz) e alias `testDebugUnitTest` → testes do `build-logic` | TRD §Stack; CLAUDE.md §Comandos | motor-voz |
| `etapa0/gradle.properties` | Novo: memória do daemon, AndroidX, estilo Kotlin | — | motor-voz |
| `etapa0/gradle/libs.versions.toml` | Novo: todas as versões, SDKs, build-tools, JVM, versionCode/versionName | TRD §Stack; §Distribuição ("versão e código de versão no version catalog") | motor-voz |
| `etapa0/gradle/gradle-daemon-jvm.properties` | Novo: `toolchainVersion=21` | Spec, critério 1 (JDK 21) | motor-voz |
| `etapa0/gradlew`, `etapa0/gradlew.bat`, `etapa0/gradle/wrapper/gradle-wrapper.{jar,properties}` | Gerados pelo `gradle wrapper` (9.7.1, com SHA-256) | TRD §Stack | motor-voz |
| `etapa0/.editorconfig` | Novo: `ktlint_code_style = ktlint_official` | TRD §Qualidade de código (ktlint) | motor-voz |
| `etapa0/local.properties` | Novo, **não versionado**: `sdk.dir` | — | motor-voz |
| `etapa0/core/build.gradle.kts` | Novo: `kotlin("jvm")`, `java-test-fixtures`, `com.android.lint`, ktlint, alias `testDebugUnitTest` → `test` | CLAUDE.md regra 4; spec, decisão 2 | motor-voz |
| `<core-main>/speech/SpeechEngine.kt`, `SessionId.kt`, `SpeechEvent.kt`, `EngineState.kt`, `EngineError.kt` | Novos: contratos copiados do TRD | TRD §Contratos, SpeechEngine | motor-voz |
| `<core-main>/align/Token.kt` | Novo: contrato | TRD §Contratos, Token | motor-voz |
| `<core-main>/align/Aligner.kt`, `AlignState.kt`, `Move.kt`, `AlignParams.kt` | Novos: contrato; `advance`/`reposition` lançam `NotImplementedError` | TRD §Contratos, Aligner; spec, decisão 3 | motor-voz |
| `<core-fixtures>/speech/FakeSpeechEngine.kt` | Novo: fake com `FakeRecognizer` interno que captura o `SessionId` na criação | TRD §Módulos e responsabilidades; §Contratos, regras do SpeechEngine | motor-voz |
| `<core-test>/speech/SpeechContractShapeTest.kt` | Novo | TRD §Contratos, SpeechEngine | motor-voz |
| `<core-test>/speech/FakeSpeechEngineTest.kt` | Novo | TRD §Testes (Sessões e leitura: callback atrasado); spec, critérios 2–4 | motor-voz |
| `<core-test>/align/AlignContractShapeTest.kt` | Novo | TRD §Contratos, Token e Aligner | motor-voz |
| `etapa0/build-logic/settings.gradle.kts`, `etapa0/build-logic/build.gradle.kts` | Novos: build incluído, `kotlin-dsl`, catalog compartilhado, plugin `tribuna.android.verifications` | TRD §Qualidade de código | motor-voz |
| `etapa0/build-logic/src/main/kotlin/io/github/gigioderico/tribuna/buildlogic/ManifestPermissionCheck.kt` | Novo: lê `uses-permission*` do manifesto mesclado | TRD §Ciclo de vida, Manifesto; CLAUDE.md regra 1 | motor-voz |
| `.../buildlogic/ElfAlignment.kt` | Novo: lê `p_align` dos segmentos `PT_LOAD` (ELF32/ELF64) | TRD §Qualidade de código (segmentos ELF) | motor-voz |
| `.../buildlogic/NativeLibScan.kt` | Novo: varre `lib/<abi>/*.so` do APK; exige `libvosk.so` e `libjnidispatch.so` em toda ABI | TRD §Qualidade de código ("em todas as ABIs") | motor-voz |
| `.../buildlogic/VerifyMergedManifestTask.kt`, `Verify16kAlignmentTask.kt`, `AndroidVerificationsPlugin.kt` | Novos: tarefas Gradle e registro por variante (Variant API) | TRD §Qualidade de código; CLAUDE.md §Comandos | motor-voz |
| `etapa0/build-logic/src/test/kotlin/io/github/gigioderico/tribuna/buildlogic/{ManifestPermissionCheckTest,ElfAlignmentTest,NativeLibScanTest,ElfFixtures}.kt` | Novos | Q2, Q3 | motor-voz |
| `etapa0/app/build.gradle.kts` | Novo: `com.android.application`, ktlint, `tribuna.android.verifications`; consome `:core` | Spec, decisão 2; TRD §Stack (SDKs) | motor-voz |
| `etapa0/app/src/main/AndroidManifest.xml` | Novo: só `RECORD_AUDIO`, `allowBackup="false"` | TRD §Manifesto; CLAUDE.md regra 1 | motor-voz |
| `etapa0/app/src/main/res/values/strings.xml` | Novo: `app_name` | CLAUDE.md (strings em `strings.xml`) | motor-voz |
| `etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/CoreFromAppTest.kt` | Novo: prova que `:core` e o fake chegam ao classpath Android | Q1 (consumidor do `:core`) | motor-voz |
| `etapa0/scripts/verificar-manifesto-negativo.sh` | Novo: injeta permissões proibidas num manifesto de debug temporário e exige falha | Spec, critério 6; Q2 | motor-voz |
| `work/0001-etapa0-contratos/evidencias/*.txt` | Saídas dos comandos de verificação | CLAUDE.md, regra de processo 8 | motor-voz |

Não muda: `docs/TRD.md`, `docs/PRD.md`, `CLAUDE.md`, nenhum contrato. CIA não é necessária: a tarefa cria os contratos como escritos, sem mudar assinatura nem semântica.

## Ordem e paralelismo

Só o `motor-voz` constrói; a `interface` não participa desta tarefa. Ordem obrigatória (contratos e `align/` → `speech/`; não há ViewModel nem telas aqui):

1. Tarefa 1 — esqueleto Gradle, wrapper, catalog, JDK 21.
2. Tarefa 2 — `:core` e contratos de `speech`.
3. Tarefa 3 — contratos de `align` e esqueleto do `Aligner`.
4. Tarefa 4 — `FakeSpeechEngine` (testes primeiro) e as quebras propositais da Q2.
5. Tarefa 5 — `build-logic` (testes primeiro).
6. Tarefa 6 — módulo `:app` e ligação das verificações.
7. Tarefa 7 — verificação final e evidências.

Paralelismo: a Tarefa 5 não depende das Tarefas 2–4 (só da Tarefa 1) e poderia correr em paralelo com elas, mas como há um único agente, a execução é sequencial na ordem acima. A Tarefa 6 depende de 2, 4 e 5. As tarefas 0002 (align) e 0003 (Vosk) só começam depois desta aprovada pelo revisor, e dependem apenas dos contratos daqui.

## Testes primeiro

Casos que devem existir e falhar (por compilação ou asserção) antes do código. Palavras usadas nos testes vêm de `etapa0/textos/T01.txt` ("ouvir", "bem", "habilidade").

`:core` — `SpeechContractShapeTest` (3):
- [x] `engineState_temOsEstadosDoTrdNaOrdem`
- [x] `engineError_temOsErrosDoTrdNaOrdem`
- [x] `speechEvent_temQuatroVariantes_eSoPartialFinalEErrorCarregamSessao` (`when` exaustivo sem `else`: um subtipo novo quebra a compilação)

`:core` — `AlignContractShapeTest` (4):
- [x] `token_representaOExemploDoTrd316`
- [x] `alignState_temOsCamposDoContrato`
- [x] `move_temOsMovimentosDoTrdNaOrdem`
- [x] `aligner_eAlignParams_seConstroemComOsArgumentosDoContrato` (não chama `advance` nem `reposition`)

`:core` — `FakeSpeechEngineTest` (17):
- [x] `prepare_sucesso_vaiDeIdleParaReady`
- [x] `prepare_falha_vaiParaFailed_eDevolveAFalha`
- [x] `cicloDeVida_capturaESessao_atualizamOEstado`
- [x] `release_vaiParaIdle_eEncerraOFluxoDeEventos`
- [x] `sessionId_cresceACadaBeginSession_eNuncaSeRepete` (critério 2)
- [x] `beginSession_guardaOVocabularioNoReconhecedorDaSessao`
- [x] `partial_chegaComOIdDaSessaoAtiva`
- [x] `final_chegaComOIdDaSessaoAtiva`
- [x] `level_chegaSemSessao_comACapturaAberta`
- [x] `error_doReconhecedorTrazASessao_eODaCapturaVemSemSessao`
- [x] `callbackAtrasado_comOutraSessaoAtiva_chegaComOIdAntigo` (critério 3; alvo da quebra A)
- [x] `callbackAtrasado_semSessaoAtiva_chegaComOIdAntigo`
- [x] `endSession_naoEmiteMaisPartialNemFinal_eLevelContinua` (critério 4; alvo da quebra B)
- [x] `level_comMicrofoneFechado_naoChega`
- [x] `beginSession_semCaptura_falha`
- [x] `beginSession_comSessaoAtiva_falha`
- [x] `stopCapture_comSessaoAtiva_falha`

`build-logic` — `ManifestPermissionCheckTest` (8), `ElfAlignmentTest` (7), `NativeLibScanTest` (5): listados na Tarefa 5.

`:app` — `CoreFromAppTest` (1): `contratosEFakeDoCore_chegamAoClasspathDoApp`.

Total esperado: 46 testes, 0 falhas, 0 ignorados.

## Privacidade (primeira camada)

- **Caminho do texto:** não existe nesta tarefa. Não há editor, ViewModel, tela nem campo de texto. As únicas palavras no código são de teste, tiradas de `etapa0/textos/T01.txt`, e só passam pelo `Channel` em memória do `FakeSpeechEngine`. Nenhum `SavedStateHandle`, `rememberSaveable`, DataStore, arquivo ou log recebe texto.
- **Caminho do áudio:** não existe. Não há `AudioRecord`, Vosk nem microfone em uso; o `RECORD_AUDIO` é só declarado para a 0003. O fake simula áudio com chamadas de método, sem bytes de áudio.
- **Logs:** nenhum `android.util.Log`, `println` ou logger no `:core` e no `:app`. As tarefas do `build-logic` registram no console do Gradle só nomes de permissão, caminhos de arquivo e nomes de `.so`, nunca conteúdo do usuário.
- **Dependências novas e permissões que trazem:**
  - `kotlinx-coroutines-core` 1.11.0 (JAR JVM, sem manifesto Android): nenhuma permissão.
  - Kotlin stdlib 2.4.21 (JAR): nenhuma.
  - `junit` e `kotlinx-coroutines-test`: só em teste, fora do APK.
  - AGP, KGP, ktlint-gradle: só no build, fora do APK.
  - Nenhuma AAR entra no `:app` nesta tarefa, então nada contribui para o manifesto mesclado além do próprio `AndroidManifest.xml`. Isso é provado pelo `verifyMergedManifest` (lê o manifesto mesclado, não o do código; Q4).
- **Backup:** `android:allowBackup="false"` no manifesto do `:app` (TRD §Manifesto).
- **Q6 aplicada:** nenhum dado do discurso existe nesta tarefa, e nenhum caminho novo pode persistir, logar ou enviar texto ou áudio. A tarefa cria a barreira executável da regra 1 (`verifyMergedManifest`), provada falhando com `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK` e uma permissão qualquer (`CAMERA`).
- **Achado para a 0003 (registrado no `opr_register.md`, OPR-001):** com `androidx.core` 1.9+ (que vem com Compose), o manifesto mesclado ganha `<uses-permission android:name="<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"/>`. O `verifyMergedManifest` vai reprovar isso, corretamente pela regra 1 como está escrita. A 0003 precisa decidir (com o humano) entre removê-la com `tools:node="remove"` ou mudar a regra; a verificação não deve ser afrouxada.

## Como as tarefas de verificação funcionam

**`verifyMergedManifest`** (Gradle Kotlin DSL, plugin `tribuna.android.verifications` do `build-logic`):

- O plugin, ao ver `com.android.application`, chama `androidComponents.onVariants { }` e, para cada variante (`debug`, `release`), registra `verify<Variante>MergedManifest` com entrada `variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)`. Isso faz o Gradle gerar o manifesto mesclado antes e garante que a verificação lê o artefato real em `build/intermediates`, não o `AndroidManifest.xml` do código (Q4).
- A agregadora `verifyMergedManifest` depende de todas as tarefas por variante.
- A lógica (`ManifestPermissionCheck`) analisa o XML com namespace, coleta `android:name` de `uses-permission`, `uses-permission-sdk-23` e `uses-permission-sdk-m`, e falha se houver qualquer permissão fora de `{android.permission.RECORD_AUDIO}` ou se `RECORD_AUDIO` estiver ausente. A exigência de `RECORD_AUDIO` evita que a tarefa passe sobre um manifesto vazio ou errado (Q3).
- A tarefa não declara saídas, então nunca fica "up-to-date" e roda sempre.
- **Como é testada falhando:** (1) unitários no `build-logic` com manifestos sintéticos para `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `CAMERA`, `uses-permission-sdk-23` e ausência de `RECORD_AUDIO`; (2) ponta a ponta com `etapa0/scripts/verificar-manifesto-negativo.sh`, que cria um manifesto temporário em `etapa0/app/src/debug/AndroidManifest.xml` com uma permissão proibida (o mesclador o funde só na variante debug), roda `:app:verifyDebugMergedManifest`, exige código diferente de zero e o nome da permissão na saída, apaga o arquivo e confirma que, sem ele, a tarefa passa. O manifesto principal nunca é editado.

**`verify16kAlignment`** (mesmo plugin):

- Registrada só para a variante `release`. Entradas: o diretório `SingleArtifact.APK` do release com o `BuiltArtifactsLoader` (lê o `output-metadata.json` e lista os APKs), o binário `zipalign` em `<sdk>/build-tools/<buildTools do catalog>/zipalign` (via `androidComponents.sdkComponents.sdkDirectory`) e a lista exigida `{libvosk.so, libjnidispatch.so}`. Depender do artefato faz o Gradle montar o APK de release antes.
- Para cada APK: (1) `NativeLibScan` abre o ZIP, agrupa as entradas `lib/<abi>/<nome>.so` por ABI e reporta: nenhuma `.so` em lugar nenhum (falha explícita, que é o caso desta tarefa, sem Vosk), ABI sem uma das bibliotecas exigidas, e qualquer `.so` (não só as duas, porque o Play exige todas; Q5) com segmento `PT_LOAD` de `p_align` < 16384 ou sem segmento `PT_LOAD`; (2) `ElfAlignment` lê o cabeçalho ELF (classe 32/64, ordem dos bytes, `e_phoff`, `e_phentsize`, `e_phnum`) e devolve o `p_align` de cada `PT_LOAD`, como o `llvm-objdump -p | grep LOAD` da documentação do Android (https://developer.android.com/guide/practices/page-sizes, "align 2**14"); (3) roda `zipalign -c -P 16 -v 4 <apk>` (comando da mesma página; build-tools ≥ 35) e reprova se o código de saída não for zero.
- Junta todos os problemas e falha com a lista; só passa sem nenhum problema.
- **Nesta tarefa ela deve falhar**, com a mensagem `nenhuma biblioteca nativa em lib/<abi>/; exigidas: libjnidispatch.so, libvosk.so`. A execução real contra o Vosk é da 0003 (spec, critério 7). Por isso o conjunto "tudo verde" desta entrega é `testDebugUnitTest lint ktlintCheck verifyMergedManifest`; o `verify16kAlignment` entra no "tudo verde" a partir do release (CLAUDE.md, "Uma entrega = tudo verde").

---

## Tarefa 1: esqueleto Gradle, wrapper, catalog e JDK 21

**Arquivos:**
- Modificar: `.gitignore`
- Criar: `etapa0/settings.gradle.kts`, `etapa0/gradle.properties`, `etapa0/gradle/libs.versions.toml`, `etapa0/gradle/gradle-daemon-jvm.properties`, `etapa0/.editorconfig`, `etapa0/local.properties` (não versionado)
- Gerar: `etapa0/gradlew`, `etapa0/gradlew.bat`, `etapa0/gradle/wrapper/gradle-wrapper.jar`, `etapa0/gradle/wrapper/gradle-wrapper.properties`

- [x] **Passo 1: confirmar o branch**

Rodar: `git -C /Users/giorgioderico/Projetos/Tribuna branch --show-current`
Esperado: `work/0001-etapa0-contratos`. Se for outro, `git checkout work/0001-etapa0-contratos`.

- [x] **Passo 2: acrescentar `.kotlin/` ao `.gitignore`**

No bloco `# Gradle / build` de `/Users/giorgioderico/Projetos/Tribuna/.gitignore`, depois da linha `captures/`, acrescentar:

```gitignore
.kotlin/
```

- [x] **Passo 3: criar `etapa0/settings.gradle.kts` mínimo** (só para gerar o wrapper; é completado nas Tarefas 2, 5 e 6)

```kotlin
rootProject.name = "tribuna-etapa0"
```

- [x] **Passo 4: gerar o wrapper 9.7.1 com checksum**

Rodar:

```bash
cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ~/.gradle/wrapper/dists/gradle-9.3.1-bin/23ovyewtku6u96viwx3xl3oks/gradle-9.3.1/bin/gradle wrapper --gradle-version 9.7.1 --distribution-type bin --gradle-distribution-sha256-sum acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a
```

Esperado: `BUILD SUCCESSFUL`; `etapa0/gradle/wrapper/gradle-wrapper.properties` contém `gradle-9.7.1-bin.zip` e `distributionSha256Sum=acd53f1e...`.

- [x] **Passo 5: criar `etapa0/gradle/gradle-daemon-jvm.properties`**

```properties
toolchainVersion=21
```

- [x] **Passo 6: criar `etapa0/gradle/libs.versions.toml`**

```toml
[versions]
agp = "9.4.1"
kotlin = "2.4.21"
coroutines = "1.11.0"
junit = "4.13.2"
ktlintGradle = "14.2.0"
ktlint = "1.8.0"
jvm = "21"
compileSdk = "36"
targetSdk = "36"
minSdk = "26"
buildTools = "36.0.0"
versionCode = "1"
versionName = "0.0.1"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }
android-gradle-api = { module = "com.android.tools.build:gradle-api", version.ref = "agp" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-lint = { id = "com.android.lint", version.ref = "agp" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlintGradle" }
```

- [x] **Passo 7: criar `etapa0/gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
```

- [x] **Passo 8: criar `etapa0/.editorconfig`**

```editorconfig
root = true

[*.{kt,kts}]
ktlint_code_style = ktlint_official
```

- [x] **Passo 9: criar `etapa0/local.properties`** (não versionado; já coberto por `local.properties` no `.gitignore`)

```bash
printf 'sdk.dir=%s\n' "$HOME/Library/Android/sdk" > /Users/giorgioderico/Projetos/Tribuna/etapa0/local.properties
```

- [x] **Passo 10: confirmar que o daemon usa o JDK 21**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew --version`
Esperado: `Gradle 9.7.1` e uma linha `Daemon JVM:` apontando para o JDK 21 Temurin com a origem `gradle/gradle-daemon-jvm.properties` (não `org.gradle.java.home`). Se aparecer o 17, parar e seguir o risco R4.

- [x] **Passo 11: confirmar que nada indevido será versionado**

Rodar: `git -C /Users/giorgioderico/Projetos/Tribuna status --porcelain etapa0 .gitignore`
Esperado: aparecem `.gitignore`, `etapa0/settings.gradle.kts`, `etapa0/gradle.properties`, `etapa0/.editorconfig`, `etapa0/gradle/`, `etapa0/gradlew`, `etapa0/gradlew.bat`. Não aparecem `etapa0/local.properties`, `etapa0/.gradle/` nem `etapa0/.kotlin/`.

- [x] **Passo 12: commit**

```bash
cd /Users/giorgioderico/Projetos/Tribuna && git add .gitignore etapa0/settings.gradle.kts etapa0/gradle.properties etapa0/.editorconfig etapa0/gradle etapa0/gradlew etapa0/gradlew.bat && git commit -m "chore(etapa0): wrapper Gradle 9.7.1, catalog e daemon no JDK 21 (TRD §Stack e versões)"
```

---

## Tarefa 2: módulo `:core` e contratos de `speech`

**Arquivos:**
- Modificar: `etapa0/settings.gradle.kts`
- Criar: `etapa0/build.gradle.kts`, `etapa0/core/build.gradle.kts`
- Criar: `<core-main>/speech/SpeechEngine.kt`, `SessionId.kt`, `SpeechEvent.kt`, `EngineState.kt`, `EngineError.kt`
- Teste: `<core-test>/speech/SpeechContractShapeTest.kt`

- [x] **Passo 1: substituir `etapa0/settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tribuna-etapa0"
include(":core")
```

- [x] **Passo 2: criar `etapa0/build.gradle.kts`**

A declaração `kotlin.jvm apply false` põe o KGP 2.4.21 no classpath raiz; o Kotlin embutido do AGP 9 passa a usá-lo no lugar do 2.2.10 (notas do AGP 9.0).

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
}
```

- [x] **Passo 3: criar `etapa0/core/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-test-fixtures`
    alias(libs.plugins.android.lint)
    alias(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

ktlint {
    version.set(libs.versions.ktlint.get())
}

// O comando único do CLAUDE.md é `./gradlew testDebugUnitTest`; num módulo JVM a tarefa se chama `test`.
tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Alias de test, para o comando testDebugUnitTest do CLAUDE.md cobrir o :core."
    dependsOn(tasks.named("test"))
}
```

- [x] **Passo 4: escrever o teste que falha** — `<core-test>/speech/SpeechContractShapeTest.kt`

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechContractShapeTest {
    @Test
    fun engineState_temOsEstadosDoTrdNaOrdem() {
        assertEquals(
            listOf(
                EngineState.Idle,
                EngineState.Preparing,
                EngineState.Ready,
                EngineState.Capturing,
                EngineState.Recognizing,
                EngineState.Failed,
            ),
            EngineState.entries,
        )
    }

    @Test
    fun engineError_temOsErrosDoTrdNaOrdem() {
        assertEquals(
            listOf(
                EngineError.ModelMissing,
                EngineError.ModelCorrupt,
                EngineError.MicUnavailable,
                EngineError.PermissionRevoked,
                EngineError.Internal,
            ),
            EngineError.entries,
        )
    }

    @Test
    fun speechEvent_temQuatroVariantes_eSoPartialFinalEErrorCarregamSessao() {
        val id = SessionId(42L)
        val events: List<SpeechEvent> =
            listOf(
                SpeechEvent.Partial(id, listOf("ouvir")),
                SpeechEvent.Final(id, listOf("ouvir", "bem")),
                SpeechEvent.Level(0.5f),
                SpeechEvent.Error(null, EngineError.ModelMissing),
            )
        assertEquals(listOf(id, id, null, null), events.map(::sessionOf))
        assertEquals(42L, id.value)
    }

    // `when` exaustivo sem `else`: um subtipo novo de SpeechEvent quebra a compilação deste teste.
    private fun sessionOf(event: SpeechEvent): SessionId? =
        when (event) {
            is SpeechEvent.Partial -> event.session
            is SpeechEvent.Final -> event.session
            is SpeechEvent.Level -> null
            is SpeechEvent.Error -> event.session
        }
}
```

- [x] **Passo 5: rodar e ver falhar**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:test`
Esperado: FAIL na compilação (`:core:compileTestKotlin`) com `Unresolved reference 'EngineState'` (e semelhantes para `SessionId`, `SpeechEvent`, `EngineError`).

- [x] **Passo 6: escrever os contratos, copiados do TRD §Contratos (SpeechEngine)**

Nomes, tipos, ordem dos parâmetros, valores padrão e comentários iguais ao TRD. `ktlintFormat` pode mudar só a formatação.

`<core-main>/speech/SpeechEngine.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Separa a captura de áudio do reconhecimento (TRD §Contratos, SpeechEngine).
 * A captura fica aberta durante toda a leitura; o reconhecimento acontece em sessões,
 * e cada sessão tem um reconhecedor limpo.
 */
interface SpeechEngine {
    val events: Flow<SpeechEvent>
    val state: StateFlow<EngineState>

    suspend fun prepare(): Result<Unit> // carrega o modelo

    fun startCapture() // abre o microfone

    fun stopCapture() // fecha o microfone

    fun beginSession(vocabulary: List<String>? = null): SessionId

    fun endSession() // descarta hipóteses e áudio pendente

    fun release()
}
```

`<core-main>/speech/SessionId.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

@JvmInline
value class SessionId(
    val value: Long,
)
```

`<core-main>/speech/SpeechEvent.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

sealed interface SpeechEvent {
    data class Partial(
        val session: SessionId,
        val words: List<String>,
    ) : SpeechEvent

    data class Final(
        val session: SessionId,
        val words: List<String>,
    ) : SpeechEvent

    data class Level(
        val rms: Float,
    ) : SpeechEvent // da captura, sem sessão

    data class Error(
        val session: SessionId?,
        val cause: EngineError,
    ) : SpeechEvent
}
```

`<core-main>/speech/EngineState.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

enum class EngineState { Idle, Preparing, Ready, Capturing, Recognizing, Failed }
```

`<core-main>/speech/EngineError.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

enum class EngineError { ModelMissing, ModelCorrupt, MicUnavailable, PermissionRevoked, Internal }
```

- [x] **Passo 7: rodar e ver passar**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:test`
Esperado: `BUILD SUCCESSFUL`; `core/build/test-results/test/TEST-io.github.gigioderico.tribuna.etapa0.speech.SpeechContractShapeTest.xml` com `tests="3" skipped="0" failures="0" errors="0"`.

- [x] **Passo 8: estilo**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:ktlintCheck`
Esperado: `BUILD SUCCESSFUL`. Se falhar só por formatação, rodar `./gradlew :core:ktlintFormat`, conferir com `git diff` que só mudou formatação (nenhum nome, tipo, ordem ou padrão do contrato) e rodar `:core:ktlintCheck` de novo. Não desativar regras.

- [x] **Passo 9: commit**

```bash
cd /Users/giorgioderico/Projetos/Tribuna && git add etapa0/settings.gradle.kts etapa0/build.gradle.kts etapa0/core && git commit -m "feat(core): contratos SpeechEngine, SessionId, SpeechEvent, EngineState, EngineError (TRD §Contratos SpeechEngine)"
```

---

## Tarefa 3: contratos de `align` e esqueleto do `Aligner`

**Arquivos:**
- Criar: `<core-main>/align/Token.kt`, `Aligner.kt`, `AlignState.kt`, `Move.kt`, `AlignParams.kt`
- Teste: `<core-test>/align/AlignContractShapeTest.kt`

- [x] **Passo 1: escrever o teste que falha** — `<core-test>/align/AlignContractShapeTest.kt`

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AlignContractShapeTest {
    @Test
    fun token_representaOExemploDoTrd316() {
        // "João 3:16": a referência ocupa os caracteres 5..8 e é um único token (TRD §Contratos, Token).
        val token =
            Token(
                index = 1,
                display = 5..8,
                spoken =
                    listOf(
                        listOf("tres", "dezesseis"),
                        listOf("tres", "e", "dezesseis"),
                        listOf("capitulo", "tres", "versiculo", "dezesseis"),
                    ),
                weight = 1.0f,
                sentenceStart = false,
            )
        assertEquals(5..8, token.display)
        assertEquals(3, token.spoken.size)
        assertEquals(listOf("tres", "e", "dezesseis"), token.spoken[1])
    }

    @Test
    fun alignState_temOsCamposDoContrato() {
        val state = AlignState(cursor = 4, highWater = 7, missStreak = 0, lastMove = Move.Short, score = 0.5)
        assertEquals(AlignState(5, 7, 0, Move.Short, 0.5), state.copy(cursor = 5))
    }

    @Test
    fun move_temOsMovimentosDoTrdNaOrdem() {
        assertEquals(
            listOf(Move.Stay, Move.Short, Move.LongJump, Move.Recovery, Move.Reposition),
            Move.entries,
        )
    }

    @Test
    fun aligner_eAlignParams_seConstroemComOsArgumentosDoContrato() {
        // Valores sintéticos, só para construir: não são parâmetros calibrados (vêm da Etapa 0).
        // advance e reposition não são chamados nesta tarefa (spec, decisão 3).
        val params = AlignParams(k = 1, window = 1, shortThreshold = 0.0, longThreshold = 0.0)
        val aligner = Aligner(tokens = emptyList(), params = params)
        assertEquals(1, params.k)
        assertNotNull(aligner)
    }
}
```

- [x] **Passo 2: rodar e ver falhar**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:test`
Esperado: FAIL na compilação com `Unresolved reference 'Token'` (e `AlignState`, `Move`, `AlignParams`, `Aligner`).

- [x] **Passo 3: escrever os contratos, copiados do TRD §Contratos (Token e Aligner)**

`<core-main>/align/Token.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

/**
 * Separa o que aparece na tela das formas faladas aceitas (TRD §Contratos, Token).
 * Destaque, toque e progresso usam o token; o alinhamento usa as formas faladas.
 */
data class Token(
    val index: Int,
    val display: IntRange, // caracteres no texto original
    val spoken: List<List<String>>, // formas faladas aceitas, cada uma com 1+ palavras
    val weight: Float, // menor para palavras comuns
    val sentenceStart: Boolean, // para Volume − e frase anterior
)
```

`<core-main>/align/Aligner.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

/**
 * Função pura: recebe o estado e as palavras confirmadas e devolve o novo estado (TRD §Contratos, Aligner).
 * Não conhece Android nem o motor.
 *
 * Invariante: um movimento automático nunca leva o cursor abaixo de highWater − 3.
 * Só reposition move o cursor livremente, e ele redefine cursor e highWater para a nova posição.
 *
 * Esqueleto da tarefa 0001: a implementação é da tarefa 0002 (TRD §Motor de voz, Alinhamento).
 */
class Aligner(
    private val tokens: List<Token>,
    private val params: AlignParams,
) {
    fun advance(
        state: AlignState,
        words: List<String>,
    ): AlignState = throw NotImplementedError("Aligner.advance é implementado na tarefa 0002")

    fun reposition(tokenIndex: Int): AlignState = // toque, Volume −, arrasto, troca de modo
        throw NotImplementedError("Aligner.reposition é implementado na tarefa 0002")
}
```

`<core-main>/align/AlignState.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

data class AlignState(
    val cursor: Int,
    val highWater: Int, // maior posição confirmada desde o último reposicionamento
    val missStreak: Int, // resultados seguidos sem avanço, para a recuperação
    val lastMove: Move,
    val score: Double,
)
```

`<core-main>/align/Move.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

enum class Move { Stay, Short, LongJump, Recovery, Reposition }
```

`<core-main>/align/AlignParams.kt` (sem valores padrão: vêm da calibração; spec, fora do escopo):

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

data class AlignParams(
    val k: Int,
    val window: Int,
    val shortThreshold: Double,
    val longThreshold: Double,
)
```

- [x] **Passo 4: rodar e ver passar**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:test`
Esperado: `BUILD SUCCESSFUL`; `TEST-io.github.gigioderico.tribuna.etapa0.align.AlignContractShapeTest.xml` com `tests="4" ... failures="0" errors="0"`.

- [x] **Passo 5: conferir a regra 4 e o estilo**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna && grep -rnE "import (android|androidx)\." etapa0/core/src ; echo "exit=$?"`
Esperado: nenhuma linha e `exit=1` (grep sem achados).
Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:ktlintCheck`
Esperado: `BUILD SUCCESSFUL` (mesma regra de `ktlintFormat` da Tarefa 2, Passo 8).

- [x] **Passo 6: commit**

```bash
cd /Users/giorgioderico/Projetos/Tribuna && git add etapa0/core && git commit -m "feat(core): contratos Token, Aligner (esqueleto), AlignState, Move, AlignParams (TRD §Contratos Token e Aligner)"
```

---

## Tarefa 4: `FakeSpeechEngine`, testes primeiro, e quebras propositais (Q2)

Comportamento do fake (decisões do arquiteto; são do fake, não do contrato):

- `beginSession` cria um `FakeRecognizer` (classe interna) com o `SessionId` capturado no construtor. O reconhecedor sempre emite com esse id, mesmo depois de `endSession`: é assim que o teste simula o callback atrasado do TRD (§Contratos, regras do SpeechEngine).
- `hearPartial`/`hearFinal` simulam áudio chegando ao motor: vão ao reconhecedor ativo; sem sessão ativa, são descartados (áudio entre sessões vai para o lixo). Esse é o caminho do critério 4.
- `emitLevel` só chega com a captura aberta (`Capturing` ou `Recognizing`); `emitCaptureError` emite `Error(null, causa)`.
- `SessionId` vem de um contador que começa em 1 e só cresce.
- `events` é um `Channel` ilimitado exposto com `receiveAsFlow()`: um coletor só (o ViewModel), nada se perde se o teste coletar depois. `release()` fecha o canal e o fluxo termina.
- Uso fora de ordem (`beginSession` sem captura ou com sessão ativa, `stopCapture` com sessão ativa, `startCapture` fora de `Ready`, qualquer chamada depois de `release`) lança `IllegalStateException`, para expor erro de quem chama. A máquina de estados do TRD sempre chama `endSession` antes de `beginSession` e de `stopCapture`, então essas checagens não contradizem o TRD. `endSession` sem sessão ativa não faz nada.
- Erros não mudam o `EngineState` do fake.

**Arquivos:**
- Criar: `<core-fixtures>/speech/FakeSpeechEngine.kt`
- Teste: `<core-test>/speech/FakeSpeechEngineTest.kt`

- [x] **Passo 1: escrever os testes que falham** — `<core-test>/speech/FakeSpeechEngineTest.kt`

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSpeechEngineTest {
    // Palavras de etapa0/textos/T01.txt (CLAUDE.md: textos de teste só de etapa0/textos/).
    private val ouvirBem = listOf("ouvir", "bem")
    private val habilidade = listOf("habilidade")

    private suspend fun capturing(): FakeSpeechEngine =
        FakeSpeechEngine().also {
            it.prepare()
            it.startCapture()
        }

    @Test
    fun prepare_sucesso_vaiDeIdleParaReady() =
        runTest {
            val fake = FakeSpeechEngine()
            assertEquals(EngineState.Idle, fake.state.value)
            assertTrue(fake.prepare().isSuccess)
            assertEquals(EngineState.Ready, fake.state.value)
        }

    @Test
    fun prepare_falha_vaiParaFailed_eDevolveAFalha() =
        runTest {
            val cause = IllegalStateException("modelo ausente")
            val fake = FakeSpeechEngine(prepareResult = Result.failure(cause))
            assertEquals(cause, fake.prepare().exceptionOrNull())
            assertEquals(EngineState.Failed, fake.state.value)
        }

    @Test
    fun cicloDeVida_capturaESessao_atualizamOEstado() =
        runTest {
            val fake = FakeSpeechEngine()
            fake.prepare()
            fake.startCapture()
            assertEquals(EngineState.Capturing, fake.state.value)
            fake.beginSession()
            assertEquals(EngineState.Recognizing, fake.state.value)
            fake.endSession()
            assertEquals(EngineState.Capturing, fake.state.value)
            fake.stopCapture()
            assertEquals(EngineState.Ready, fake.state.value)
        }

    @Test
    fun release_vaiParaIdle_eEncerraOFluxoDeEventos() =
        runTest {
            val fake = capturing()
            fake.emitLevel(0.2f)
            fake.release()
            assertEquals(EngineState.Idle, fake.state.value)
            assertEquals(listOf<SpeechEvent>(SpeechEvent.Level(0.2f)), fake.events.toList())
        }

    @Test
    fun sessionId_cresceACadaBeginSession_eNuncaSeRepete() =
        runTest {
            val fake = capturing()
            val ids =
                List(3) {
                    fake.beginSession().also { fake.endSession() }
                }
            assertTrue(ids.zipWithNext().all { (antes, depois) -> depois.value > antes.value })
            assertEquals(3, ids.toSet().size)
        }

    @Test
    fun beginSession_guardaOVocabularioNoReconhecedorDaSessao() =
        runTest {
            val fake = capturing()
            val id = fake.beginSession(vocabulary = listOf("ouvir", "bem", "[unk]"))
            val recognizer = fake.recognizers.last()
            assertEquals(id, recognizer.session)
            assertEquals(listOf("ouvir", "bem", "[unk]"), recognizer.vocabulary)
        }

    @Test
    fun partial_chegaComOIdDaSessaoAtiva() =
        runTest {
            val fake = capturing()
            val id = fake.beginSession()
            fake.hearPartial(ouvirBem)
            assertEquals(SpeechEvent.Partial(id, ouvirBem), fake.events.first())
        }

    @Test
    fun final_chegaComOIdDaSessaoAtiva() =
        runTest {
            val fake = capturing()
            val id = fake.beginSession()
            fake.hearFinal(ouvirBem)
            assertEquals(SpeechEvent.Final(id, ouvirBem), fake.events.first())
        }

    @Test
    fun level_chegaSemSessao_comACapturaAberta() =
        runTest {
            val fake = capturing()
            fake.emitLevel(0.7f)
            assertEquals(SpeechEvent.Level(0.7f), fake.events.first())
        }

    @Test
    fun error_doReconhecedorTrazASessao_eODaCapturaVemSemSessao() =
        runTest {
            val fake = capturing()
            val id = fake.beginSession()
            fake.recognizers.last().error(EngineError.Internal)
            fake.emitCaptureError(EngineError.MicUnavailable)
            assertEquals(
                listOf(
                    SpeechEvent.Error(id, EngineError.Internal),
                    SpeechEvent.Error(null, EngineError.MicUnavailable),
                ),
                fake.events.take(2).toList(),
            )
        }

    // Spec, critério 3; alvo da quebra A (Q2).
    @Test
    fun callbackAtrasado_comOutraSessaoAtiva_chegaComOIdAntigo() =
        runTest {
            val fake = capturing()
            val antiga = fake.beginSession()
            val reconhecedorAntigo = fake.recognizers.last()
            fake.endSession()
            val nova = fake.beginSession()
            assertNotEquals(antiga, nova)

            reconhecedorAntigo.partial(habilidade)
            reconhecedorAntigo.final(habilidade)

            assertEquals(
                listOf(
                    SpeechEvent.Partial(antiga, habilidade),
                    SpeechEvent.Final(antiga, habilidade),
                ),
                fake.events.take(2).toList(),
            )
        }

    @Test
    fun callbackAtrasado_semSessaoAtiva_chegaComOIdAntigo() =
        runTest {
            val fake = capturing()
            val antiga = fake.beginSession()
            val reconhecedorAntigo = fake.recognizers.last()
            fake.endSession()
            reconhecedorAntigo.final(habilidade)
            assertEquals(SpeechEvent.Final(antiga, habilidade), fake.events.first())
        }

    // Spec, critério 4; alvo da quebra B (Q2). O Level serve de sentinela: se algo
    // tivesse sido emitido antes dele, `first()` devolveria esse algo.
    @Test
    fun endSession_naoEmiteMaisPartialNemFinal_eLevelContinua() =
        runTest {
            val fake = capturing()
            fake.beginSession()
            fake.endSession()
            fake.hearPartial(ouvirBem)
            fake.hearFinal(ouvirBem)
            fake.emitLevel(0.4f)
            assertEquals(SpeechEvent.Level(0.4f), fake.events.first())
            assertEquals(EngineState.Capturing, fake.state.value)
        }

    @Test
    fun level_comMicrofoneFechado_naoChega() =
        runTest {
            val fake = capturing()
            fake.stopCapture()
            fake.emitLevel(0.9f)
            fake.emitCaptureError(EngineError.PermissionRevoked)
            assertEquals(SpeechEvent.Error(null, EngineError.PermissionRevoked), fake.events.first())
        }

    @Test
    fun beginSession_semCaptura_falha() =
        runTest {
            val fake = FakeSpeechEngine()
            fake.prepare()
            assertThrows(IllegalStateException::class.java) { fake.beginSession() }
        }

    @Test
    fun beginSession_comSessaoAtiva_falha() =
        runTest {
            val fake = capturing()
            fake.beginSession()
            assertThrows(IllegalStateException::class.java) { fake.beginSession() }
        }

    @Test
    fun stopCapture_comSessaoAtiva_falha() =
        runTest {
            val fake = capturing()
            fake.beginSession()
            assertThrows(IllegalStateException::class.java) { fake.stopCapture() }
        }
}
```

- [x] **Passo 2: rodar e ver falhar**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:test`
Esperado: FAIL na compilação com `Unresolved reference 'FakeSpeechEngine'`.

- [x] **Passo 3: implementar** — `<core-fixtures>/speech/FakeSpeechEngine.kt`

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * SpeechEngine falso para testes (TRD §Módulos e responsabilidades; §Contratos, regras do SpeechEngine).
 *
 * - Cada beginSession cria um [FakeRecognizer] com o SessionId capturado na criação. O reconhecedor
 *   emite sempre com esse id, mesmo depois de endSession: é o callback atrasado do TRD.
 * - [hearPartial] e [hearFinal] simulam áudio chegando: vão ao reconhecedor ativo; sem sessão, são descartados.
 * - [emitLevel] só chega com a captura aberta.
 * - [events] tem um coletor só e buffer ilimitado; [release] encerra o fluxo.
 * - Uso fora de ordem lança IllegalStateException, para expor erro de quem chama. Isso é do fake, não do contrato.
 */
class FakeSpeechEngine(
    private val prepareResult: Result<Unit> = Result.success(Unit),
) : SpeechEngine {
    private val channel = Channel<SpeechEvent>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow(EngineState.Idle)
    private val created = mutableListOf<FakeRecognizer>()
    private var nextSession = 1L
    private var active: FakeRecognizer? = null
    private var released = false

    override val events: Flow<SpeechEvent> = channel.receiveAsFlow()
    override val state: StateFlow<EngineState> = mutableState.asStateFlow()

    /** Reconhecedores criados, em ordem; o último é o da sessão mais recente. */
    val recognizers: List<FakeRecognizer>
        get() = created.toList()

    override suspend fun prepare(): Result<Unit> {
        checkNotReleased()
        mutableState.value = EngineState.Preparing
        mutableState.value = if (prepareResult.isSuccess) EngineState.Ready else EngineState.Failed
        return prepareResult
    }

    override fun startCapture() {
        checkNotReleased()
        check(mutableState.value == EngineState.Ready) { "startCapture exige Ready; estado: ${mutableState.value}" }
        mutableState.value = EngineState.Capturing
    }

    override fun stopCapture() {
        checkNotReleased()
        check(active == null) { "endSession antes de stopCapture" }
        check(mutableState.value == EngineState.Capturing) { "stopCapture exige Capturing; estado: ${mutableState.value}" }
        mutableState.value = EngineState.Ready
    }

    override fun beginSession(vocabulary: List<String>?): SessionId {
        checkNotReleased()
        check(mutableState.value == EngineState.Capturing && active == null) {
            "beginSession exige captura aberta e nenhuma sessão ativa; estado: ${mutableState.value}"
        }
        val recognizer = FakeRecognizer(SessionId(nextSession++), vocabulary)
        created += recognizer
        active = recognizer
        mutableState.value = EngineState.Recognizing
        return recognizer.session
    }

    override fun endSession() {
        if (active == null) return
        active = null
        mutableState.value = EngineState.Capturing
    }

    override fun release() {
        released = true
        active = null
        mutableState.value = EngineState.Idle
        channel.close()
    }

    /** Fala chegando ao motor: vira Partial da sessão ativa; sem sessão, é descartada. */
    fun hearPartial(words: List<String>) {
        active?.partial(words)
    }

    /** Fala chegando ao motor: vira Final da sessão ativa; sem sessão, é descartada. */
    fun hearFinal(words: List<String>) {
        active?.final(words)
    }

    /** Nível de áudio da captura; só chega com o microfone aberto. */
    fun emitLevel(rms: Float) {
        val current = mutableState.value
        if (current == EngineState.Capturing || current == EngineState.Recognizing) {
            channel.trySend(SpeechEvent.Level(rms))
        }
    }

    /** Erro da captura, sem sessão (ex.: MicUnavailable, PermissionRevoked). */
    fun emitCaptureError(cause: EngineError) {
        channel.trySend(SpeechEvent.Error(null, cause))
    }

    private fun checkNotReleased() = check(!released) { "FakeSpeechEngine já liberado" }

    /** Reconhecedor de uma sessão. O id é capturado na criação; nenhum método lê a sessão ativa do motor. */
    inner class FakeRecognizer internal constructor(
        val session: SessionId,
        val vocabulary: List<String>?,
    ) {
        fun partial(words: List<String>) {
            channel.trySend(SpeechEvent.Partial(session, words))
        }

        fun final(words: List<String>) {
            channel.trySend(SpeechEvent.Final(session, words))
        }

        fun error(cause: EngineError) {
            channel.trySend(SpeechEvent.Error(session, cause))
        }
    }
}
```

- [x] **Passo 4: rodar e ver passar**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:test :core:ktlintCheck`
Esperado: `BUILD SUCCESSFUL`; `TEST-...speech.FakeSpeechEngineTest.xml` com `tests="17" skipped="0" failures="0" errors="0"`.

- [x] **Passo 5: commit** (antes das quebras, para poder restaurar com `git checkout`)

```bash
cd /Users/giorgioderico/Projetos/Tribuna && git add etapa0/core && git commit -m "feat(core): FakeSpeechEngine com SessionId preso ao reconhecedor (TRD §Contratos, regras do SpeechEngine)"
```

- [x] **Passo 6: quebra A (Q2, critério 3) — o fake passa a ler a "sessão atual" no callback**

Em `<core-fixtures>/speech/FakeSpeechEngine.kt`, dentro de `FakeRecognizer.partial`, trocar
`channel.trySend(SpeechEvent.Partial(session, words))`
por
`channel.trySend(SpeechEvent.Partial(active?.session ?: session, words))`.

Rodar:

```bash
mkdir -p /Users/giorgioderico/Projetos/Tribuna/work/0001-etapa0-contratos/evidencias && cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:test --tests '*FakeSpeechEngineTest.callbackAtrasado_comOutraSessaoAtiva*' 2>&1 | tee ../work/0001-etapa0-contratos/evidencias/q2-quebra-a-sessao-atual.txt
```

Esperado: `FAILED` em `callbackAtrasado_comOutraSessaoAtiva_chegaComOIdAntigo`, com `expected:<[Partial(session=SessionId(value=1)...` e o valor real contendo `SessionId(value=2)`, e `BUILD FAILED`.

Restaurar: `git -C /Users/giorgioderico/Projetos/Tribuna checkout -- etapa0/core/src/testFixtures`

- [x] **Passo 7: quebra B (Q2, critério 4) — `endSession` deixa o reconhecedor ativo**

Em `endSession()`, apagar a linha `active = null`.

Rodar:

```bash
cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:test --tests '*FakeSpeechEngineTest.endSession_naoEmiteMaisPartialNemFinal*' 2>&1 | tee ../work/0001-etapa0-contratos/evidencias/q2-quebra-b-endsession.txt
```

Esperado: `FAILED` em `endSession_naoEmiteMaisPartialNemFinal_eLevelContinua`, com `expected:<Level(rms=0.4)> but was:<Partial(session=SessionId(value=1), words=[ouvir, bem])>`, e `BUILD FAILED`.

Restaurar: `git -C /Users/giorgioderico/Projetos/Tribuna checkout -- etapa0/core/src/testFixtures`

- [x] **Passo 8: confirmar a restauração**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:test && git -C /Users/giorgioderico/Projetos/Tribuna status --porcelain etapa0/core`
Esperado: `BUILD SUCCESSFUL` e nenhuma linha do `git status` (árvore limpa no `:core`).

---

## Tarefa 5: `build-logic` com as verificações (testes primeiro)

**Arquivos:**
- Modificar: `etapa0/settings.gradle.kts`, `etapa0/build.gradle.kts`
- Criar: `etapa0/build-logic/settings.gradle.kts`, `etapa0/build-logic/build.gradle.kts`
- Criar em `etapa0/build-logic/src/main/kotlin/io/github/gigioderico/tribuna/buildlogic/`: `ManifestPermissionCheck.kt`, `ElfAlignment.kt`, `NativeLibScan.kt`, `VerifyMergedManifestTask.kt`, `Verify16kAlignmentTask.kt`, `AndroidVerificationsPlugin.kt`
- Teste em `etapa0/build-logic/src/test/kotlin/io/github/gigioderico/tribuna/buildlogic/`: `ElfFixtures.kt`, `ManifestPermissionCheckTest.kt`, `ElfAlignmentTest.kt`, `NativeLibScanTest.kt`

- [x] **Passo 1: criar `etapa0/build-logic/settings.gradle.kts`**

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
```

- [x] **Passo 2: criar `etapa0/build-logic/build.gradle.kts`**

```kotlin
plugins {
    `kotlin-dsl`
}

group = "io.github.gigioderico.tribuna.buildlogic"

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

dependencies {
    compileOnly(libs.android.gradle.api)
    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidVerifications") {
            id = "tribuna.android.verifications"
            implementationClass = "io.github.gigioderico.tribuna.buildlogic.AndroidVerificationsPlugin"
        }
    }
}
```

- [x] **Passo 3: ligar o build incluído** — substituir `etapa0/settings.gradle.kts`

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tribuna-etapa0"
include(":core")
```

E substituir `etapa0/build.gradle.kts` (acrescenta o alias que faz `./gradlew testDebugUnitTest` rodar os testes do `build-logic`):

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
}

tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Roda os testes do build-logic (manifesto e 16 KB) junto com os unitários dos módulos."
    dependsOn(gradle.includedBuild("build-logic").task(":test"))
}
```

- [x] **Passo 4: escrever os testes que falham**

`ElfFixtures.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** ELF sintéticos (little-endian, como no Android) só com cabeçalho e tabela de segmentos. */
object ElfFixtures {
    private const val PT_LOAD = 1
    private const val PT_DYNAMIC = 2

    fun elf64(
        vararg loadAligns: Long,
        nonLoadAlign: Long? = null,
    ): ByteArray {
        val headers = loadAligns.map { PT_LOAD to it } + listOfNotNull(nonLoadAlign?.let { PT_DYNAMIC to it })
        val phoff = 64
        val phentsize = 56
        val buf = ByteBuffer.allocate(phoff + phentsize * headers.size).order(ByteOrder.LITTLE_ENDIAN)
        magic(buf, elfClass = 2)
        buf.putLong(0x20, phoff.toLong())
        buf.putShort(0x36, phentsize.toShort())
        buf.putShort(0x38, headers.size.toShort())
        headers.forEachIndexed { i, (type, align) ->
            val base = phoff + i * phentsize
            buf.putInt(base, type)
            buf.putLong(base + 0x30, align)
        }
        return buf.array()
    }

    fun elf32(vararg loadAligns: Long): ByteArray {
        val phoff = 52
        val phentsize = 32
        val buf = ByteBuffer.allocate(phoff + phentsize * loadAligns.size).order(ByteOrder.LITTLE_ENDIAN)
        magic(buf, elfClass = 1)
        buf.putInt(0x1C, phoff)
        buf.putShort(0x2A, phentsize.toShort())
        buf.putShort(0x2C, loadAligns.size.toShort())
        loadAligns.forEachIndexed { i, align ->
            val base = phoff + i * phentsize
            buf.putInt(base, PT_LOAD)
            buf.putInt(base + 0x1C, align.toInt())
        }
        return buf.array()
    }

    private fun magic(
        buf: ByteBuffer,
        elfClass: Int,
    ) {
        buf.put(0, 0x7F.toByte())
        buf.put(1, 'E'.code.toByte())
        buf.put(2, 'L'.code.toByte())
        buf.put(3, 'F'.code.toByte())
        buf.put(4, elfClass.toByte())
        buf.put(5, 1.toByte()) // little-endian
    }
}
```

`ManifestPermissionCheckTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Test

class ManifestPermissionCheckTest {
    private val recordAudio = ManifestPermissionCheck.RECORD_AUDIO
    private val allowed = setOf(recordAudio)

    private fun perm(
        name: String,
        tag: String = "uses-permission",
    ) = "<$tag android:name=\"$name\" />"

    private fun manifest(vararg elements: String) =
        elements.joinToString(
            separator = "\n",
            prefix = "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n",
            postfix = "\n<application android:allowBackup=\"false\" />\n</manifest>",
        )

    private fun problems(xml: String) = ManifestPermissionCheck.problems(xml, allowed = allowed, required = allowed)

    @Test
    fun soRecordAudio_passa() {
        assertEquals(emptyList<String>(), problems(manifest(perm(recordAudio))))
    }

    @Test
    fun internet_falha() {
        assertEquals(
            listOf("permissão não permitida: android.permission.INTERNET"),
            problems(manifest(perm(recordAudio), perm("android.permission.INTERNET"))),
        )
    }

    @Test
    fun accessNetworkState_falha() {
        assertEquals(
            listOf("permissão não permitida: android.permission.ACCESS_NETWORK_STATE"),
            problems(manifest(perm(recordAudio), perm("android.permission.ACCESS_NETWORK_STATE"))),
        )
    }

    @Test
    fun wakeLock_falha() {
        assertEquals(
            listOf("permissão não permitida: android.permission.WAKE_LOCK"),
            problems(manifest(perm(recordAudio), perm("android.permission.WAKE_LOCK"))),
        )
    }

    @Test
    fun qualquerOutraPermissao_falha() {
        assertEquals(
            listOf("permissão não permitida: android.permission.CAMERA"),
            problems(manifest(perm(recordAudio), perm("android.permission.CAMERA"))),
        )
    }

    @Test
    fun usesPermissionSdk23_tambemConta() {
        assertEquals(
            listOf("permissão não permitida: android.permission.INTERNET"),
            problems(manifest(perm(recordAudio), perm("android.permission.INTERNET", tag = "uses-permission-sdk-23"))),
        )
    }

    @Test
    fun semRecordAudio_falha() {
        assertEquals(
            listOf("permissão exigida ausente: android.permission.RECORD_AUDIO"),
            problems(manifest()),
        )
    }

    @Test
    fun listaTodasAsProibidas_emOrdemAlfabetica() {
        assertEquals(
            listOf(
                "permissão não permitida: android.permission.INTERNET",
                "permissão não permitida: android.permission.WAKE_LOCK",
            ),
            problems(manifest(perm("android.permission.WAKE_LOCK"), perm(recordAudio), perm("android.permission.INTERNET"))),
        )
    }
}
```

`ElfAlignmentTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ElfAlignmentTest {
    @Test
    fun elf64_segmentosLoadDe16k_saoLidos() {
        assertEquals(listOf(16_384L, 16_384L), ElfAlignment.loadSegmentAlignments(ElfFixtures.elf64(16_384L, 16_384L)))
    }

    @Test
    fun elf64_segmentoLoadDe4k_eLido() {
        assertEquals(listOf(4_096L), ElfAlignment.loadSegmentAlignments(ElfFixtures.elf64(4_096L)))
    }

    @Test
    fun elf32_segmentosLoadDe16k_saoLidos() {
        assertEquals(listOf(16_384L), ElfAlignment.loadSegmentAlignments(ElfFixtures.elf32(16_384L)))
    }

    @Test
    fun elf32_segmentoLoadDe4k_eLido() {
        assertEquals(listOf(4_096L, 16_384L), ElfAlignment.loadSegmentAlignments(ElfFixtures.elf32(4_096L, 16_384L)))
    }

    @Test
    fun segmentosQueNaoSaoLoad_saoIgnorados() {
        assertEquals(listOf(16_384L), ElfAlignment.loadSegmentAlignments(ElfFixtures.elf64(16_384L, nonLoadAlign = 8L)))
    }

    @Test
    fun arquivoQueNaoEElf_lancaErro() {
        val zip = ByteArray(64).also { it[0] = 'P'.code.toByte(); it[1] = 'K'.code.toByte() }
        assertThrows(IllegalArgumentException::class.java) { ElfAlignment.loadSegmentAlignments(zip) }
    }

    @Test
    fun tabelaDeSegmentosForaDoArquivo_lancaErro() {
        val truncated = ElfFixtures.elf64(16_384L).copyOf(80)
        assertThrows(IllegalArgumentException::class.java) { ElfAlignment.loadSegmentAlignments(truncated) }
    }
}
```

`NativeLibScanTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class NativeLibScanTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val required = setOf("libvosk.so", "libjnidispatch.so")
    private val aligned = ElfFixtures.elf64(16_384L, 16_384L)
    private val misaligned = ElfFixtures.elf64(4_096L)

    private fun apk(vararg entries: Pair<String, ByteArray>): File {
        val file = tmp.newFile("app.apk")
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    @Test
    fun duasAbisComAsDuasLibsAlinhadas_semProblemas() {
        val file =
            apk(
                "lib/arm64-v8a/libvosk.so" to aligned,
                "lib/arm64-v8a/libjnidispatch.so" to aligned,
                "lib/x86_64/libvosk.so" to aligned,
                "lib/x86_64/libjnidispatch.so" to aligned,
                "classes.dex" to ByteArray(8),
            )
        assertEquals(emptyList<String>(), NativeLibScan.check(file, required, minAlign = 16_384L))
    }

    @Test
    fun abiSemLibjnidispatch_eReportada() {
        val file =
            apk(
                "lib/arm64-v8a/libvosk.so" to aligned,
                "lib/arm64-v8a/libjnidispatch.so" to aligned,
                "lib/x86_64/libvosk.so" to aligned,
            )
        assertEquals(listOf("app.apk: x86_64 sem libjnidispatch.so"), NativeLibScan.check(file, required, minAlign = 16_384L))
    }

    @Test
    fun semNenhumaLibNativa_falhaExplicitamente() {
        val file = apk("classes.dex" to ByteArray(8))
        assertEquals(
            listOf("app.apk: nenhuma biblioteca nativa em lib/<abi>/; exigidas: libjnidispatch.so, libvosk.so"),
            NativeLibScan.check(file, required, minAlign = 16_384L),
        )
    }

    @Test
    fun libExigidaDesalinhada_eReportada() {
        val file =
            apk(
                "lib/arm64-v8a/libvosk.so" to misaligned,
                "lib/arm64-v8a/libjnidispatch.so" to aligned,
            )
        assertEquals(
            listOf("app.apk: lib/arm64-v8a/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)"),
            NativeLibScan.check(file, required, minAlign = 16_384L),
        )
    }

    @Test
    fun outraLibDesalinhada_tambemEReportada() {
        val file =
            apk(
                "lib/arm64-v8a/libvosk.so" to aligned,
                "lib/arm64-v8a/libjnidispatch.so" to aligned,
                "lib/arm64-v8a/libc++_shared.so" to misaligned,
            )
        assertEquals(
            listOf("app.apk: lib/arm64-v8a/libc++_shared.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)"),
            NativeLibScan.check(file, required, minAlign = 16_384L),
        )
    }
}
```

- [x] **Passo 5: rodar e ver falhar**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew testDebugUnitTest`
Esperado: FAIL em `:build-logic:compileTestKotlin` com `Unresolved reference 'ManifestPermissionCheck'` (e `ElfAlignment`, `NativeLibScan`).

- [x] **Passo 6: implementar a lógica pura**

`ManifestPermissionCheck.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/** Permissões declaradas no manifesto mesclado (CLAUDE.md, regra 1; TRD §Ciclo de vida, Manifesto). */
object ManifestPermissionCheck {
    const val RECORD_AUDIO = "android.permission.RECORD_AUDIO"
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    private val PERMISSION_TAGS = listOf("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")

    fun declaredPermissions(manifestXml: String): List<String> {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(manifestXml)))
        return PERMISSION_TAGS.flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).map { (nodes.item(it) as Element).getAttributeNS(ANDROID_NS, "name") }
        }
    }

    fun problems(
        manifestXml: String,
        allowed: Set<String>,
        required: Set<String>,
    ): List<String> {
        val declared = declaredPermissions(manifestXml)
        val extra = declared.filterNot { it in allowed }.distinct().sorted().map { "permissão não permitida: $it" }
        val missing = (required - declared.toSet()).sorted().map { "permissão exigida ausente: $it" }
        return extra + missing
    }
}
```

`ElfAlignment.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lê o p_align dos segmentos PT_LOAD de um ELF (32 ou 64 bits), como `llvm-objdump -p | grep LOAD`
 * (https://developer.android.com/guide/practices/page-sizes). TRD §Qualidade de código.
 */
object ElfAlignment {
    private const val PT_LOAD = 1

    fun loadSegmentAlignments(bytes: ByteArray): List<Long> {
        require(
            bytes.size >= 52 &&
                bytes[0] == 0x7F.toByte() &&
                bytes[1] == 'E'.code.toByte() &&
                bytes[2] == 'L'.code.toByte() &&
                bytes[3] == 'F'.code.toByte(),
        ) { "não é um arquivo ELF" }
        val is64 =
            when (bytes[4].toInt()) {
                1 -> false
                2 -> true
                else -> throw IllegalArgumentException("classe ELF desconhecida: ${bytes[4]}")
            }
        val order =
            when (bytes[5].toInt()) {
                1 -> ByteOrder.LITTLE_ENDIAN
                2 -> ByteOrder.BIG_ENDIAN
                else -> throw IllegalArgumentException("ordem de bytes ELF desconhecida: ${bytes[5]}")
            }
        require(!is64 || bytes.size >= 64) { "cabeçalho ELF64 truncado" }
        val buf = ByteBuffer.wrap(bytes).order(order)
        val phoff = if (is64) buf.getLong(0x20) else buf.getInt(0x1C).toLong() and 0xFFFFFFFFL
        val phentsize = buf.getShort(if (is64) 0x36 else 0x2A).toInt() and 0xFFFF
        val phnum = buf.getShort(if (is64) 0x38 else 0x2C).toInt() and 0xFFFF
        require(phoff >= 0 && phoff + phentsize.toLong() * phnum <= bytes.size) { "tabela de segmentos fora do arquivo" }
        return (0 until phnum).mapNotNull { i ->
            val base = (phoff + i.toLong() * phentsize).toInt()
            when {
                buf.getInt(base) != PT_LOAD -> null
                is64 -> buf.getLong(base + 0x30)
                else -> buf.getInt(base + 0x1C).toLong() and 0xFFFFFFFFL
            }
        }
    }
}
```

`NativeLibScan.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import java.io.File
import java.util.zip.ZipFile

/**
 * Varre lib/<abi>/*.so de um APK: exige as bibliotecas pedidas em toda ABI e confere o alinhamento
 * dos segmentos LOAD de toda .so (TRD §Qualidade de código: "em todas as ABIs").
 */
object NativeLibScan {
    private val LIB_ENTRY = Regex("^lib/([^/]+)/([^/]+\\.so)$")

    fun check(
        apk: File,
        requiredLibs: Set<String>,
        minAlign: Long,
    ): List<String> {
        val problems = mutableListOf<String>()
        ZipFile(apk).use { zip ->
            val byAbi =
                zip
                    .entries()
                    .asSequence()
                    .mapNotNull { entry -> LIB_ENTRY.matchEntire(entry.name)?.let { it.groupValues[1] to entry } }
                    .groupBy({ it.first }, { it.second })
            if (byAbi.isEmpty()) {
                problems += "${apk.name}: nenhuma biblioteca nativa em lib/<abi>/; exigidas: ${requiredLibs.sorted().joinToString()}"
                return problems
            }
            for ((abi, entries) in byAbi.toSortedMap()) {
                val names = entries.map { it.name.substringAfterLast('/') }.toSet()
                (requiredLibs - names).sorted().forEach { problems += "${apk.name}: $abi sem $it" }
                for (entry in entries.sortedBy { it.name }) {
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    val aligns =
                        try {
                            ElfAlignment.loadSegmentAlignments(bytes)
                        } catch (e: IllegalArgumentException) {
                            problems += "${apk.name}: ${entry.name} não é um ELF válido (${e.message})"
                            continue
                        }
                    if (aligns.isEmpty()) problems += "${apk.name}: ${entry.name} não tem segmento LOAD"
                    aligns.filter { it < minAlign }.distinct().forEach {
                        problems += "${apk.name}: ${entry.name} tem segmento LOAD alinhado a $it bytes (mínimo $minAlign)"
                    }
                }
            }
        }
        return problems
    }
}
```

- [x] **Passo 7: implementar as tarefas e o plugin**

`VerifyMergedManifestTask.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Falha se o manifesto mesclado tiver permissão fora da lista (CLAUDE.md, regra 1). Sem saídas: roda sempre. */
abstract class VerifyMergedManifestTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val allowedPermissions: SetProperty<String>

    @get:Input
    abstract val requiredPermissions: SetProperty<String>

    @TaskAction
    fun verify() {
        val file = mergedManifest.get().asFile
        val xml = file.readText()
        val problems = ManifestPermissionCheck.problems(xml, allowedPermissions.get(), requiredPermissions.get())
        if (problems.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("$name: o manifesto mesclado ${file.path} viola a regra 1 do CLAUDE.md (TRD §Manifesto):")
                    problems.forEach { appendLine("  - $it") }
                    append("Permitidas: ${allowedPermissions.get().sorted().joinToString()}")
                },
            )
        }
        logger.lifecycle("$name: OK ${file.path} -> ${ManifestPermissionCheck.declaredPermissions(xml).joinToString()}")
    }
}
```

`Verify16kAlignmentTask.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Alinhamento de 16 KB no APK de release: segmentos ELF de toda .so, presença das bibliotecas exigidas
 * em toda ABI e `zipalign -c -P 16 -v 4` (TRD §Qualidade de código). Sem saídas: roda sempre.
 */
abstract class Verify16kAlignmentTask
    @Inject
    constructor(
        private val execOperations: ExecOperations,
    ) : DefaultTask() {
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        abstract val apkDirectory: DirectoryProperty

        @get:Internal
        abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

        @get:InputFile
        @get:PathSensitive(PathSensitivity.NONE)
        abstract val zipalign: RegularFileProperty

        @get:Input
        abstract val requiredLibraries: SetProperty<String>

        @get:Input
        abstract val pageSizeKb: Property<Int>

        @TaskAction
        fun verify() {
            val dir = apkDirectory.get()
            val apks =
                builtArtifactsLoader.get().load(dir)?.elements?.map { File(it.outputFile) }.orEmpty()
            if (apks.isEmpty()) throw GradleException("$name: nenhum APK de release em ${dir.asFile}")
            val kb = pageSizeKb.get()
            val problems = mutableListOf<String>()
            for (apk in apks) {
                problems += NativeLibScan.check(apk, requiredLibraries.get(), minAlign = kb * 1024L)
                val output = ByteArrayOutputStream()
                val result =
                    execOperations.exec {
                        commandLine(zipalign.get().asFile.path, "-c", "-P", kb.toString(), "-v", "4", apk.path)
                        standardOutput = output
                        errorOutput = output
                        isIgnoreExitValue = true
                    }
                val lastLine = output.toString().lineSequence().lastOrNull { it.isNotBlank() }.orEmpty()
                logger.lifecycle("$name: zipalign -c -P $kb -v 4 ${apk.name} -> $lastLine")
                if (result.exitValue != 0) problems += "${apk.name}: zipalign -c -P $kb falhou (código ${result.exitValue})"
            }
            if (problems.isNotEmpty()) {
                throw GradleException(
                    buildString {
                        appendLine("$name: alinhamento de $kb KB reprovado (TRD §Qualidade de código):")
                        problems.forEach { appendLine("  - $it") }
                    },
                )
            }
            logger.lifecycle("$name: OK ${apks.joinToString { it.name }}")
        }
    }
```

`AndroidVerificationsPlugin.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Registra verifyMergedManifest (todas as variantes) e verify16kAlignment (release) no módulo Android. */
class AndroidVerificationsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.application") {
            val android = project.extensions.getByType(ApplicationExtension::class.java)
            val components = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            val aggregate =
                project.tasks.register("verifyMergedManifest") {
                    group = "verification"
                    description = "Falha se o manifesto mesclado de qualquer variante tiver permissão além de RECORD_AUDIO."
                }
            components.onVariants { variant ->
                val suffix = variant.name.replaceFirstChar { it.uppercase() }
                val perVariant =
                    project.tasks.register("verify${suffix}MergedManifest", VerifyMergedManifestTask::class.java) {
                        group = "verification"
                        mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                        allowedPermissions.set(setOf(ManifestPermissionCheck.RECORD_AUDIO))
                        requiredPermissions.set(setOf(ManifestPermissionCheck.RECORD_AUDIO))
                    }
                aggregate.configure { dependsOn(perVariant) }
                if (variant.buildType == "release") {
                    project.tasks.register("verify16kAlignment", Verify16kAlignmentTask::class.java) {
                        group = "verification"
                        description = "Alinhamento de 16 KB de libvosk.so e libjnidispatch.so no APK de release."
                        apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
                        builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
                        zipalign.set(
                            components.sdkComponents.sdkDirectory.map {
                                it.file("build-tools/${android.buildToolsVersion}/zipalign")
                            },
                        )
                        requiredLibraries.set(setOf("libvosk.so", "libjnidispatch.so"))
                        pageSizeKb.set(16)
                    }
                }
            }
        }
    }
}
```

- [x] **Passo 8: rodar e ver passar**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew testDebugUnitTest`
Esperado: `BUILD SUCCESSFUL`. Em `build-logic/build/test-results/test/`: `ManifestPermissionCheckTest` `tests="8"`, `ElfAlignmentTest` `tests="7"`, `NativeLibScanTest` `tests="5"`, todos com `failures="0" errors="0"`. O `:core` também roda (24 testes).

- [x] **Passo 9: commit** (antes da quebra, para poder restaurar com `git checkout`)

```bash
cd /Users/giorgioderico/Projetos/Tribuna && git add etapa0/settings.gradle.kts etapa0/build.gradle.kts etapa0/build-logic && git commit -m "build(etapa0): verifyMergedManifest e verify16kAlignment no build-logic, com testes (TRD §Qualidade de código)"
```

- [x] **Passo 10: quebra C (Q2) — a varredura deixa de reprovar desalinhamento**

Em `NativeLibScan.kt`, trocar `aligns.filter { it < minAlign }` por `aligns.filter { it < 0 }`.

Rodar:

```bash
cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew testDebugUnitTest 2>&1 | tee ../work/0001-etapa0-contratos/evidencias/q2-quebra-c-elf.txt
```

Esperado: `FAILED` em `NativeLibScanTest.libExigidaDesalinhada_eReportada` e `NativeLibScanTest.outraLibDesalinhada_tambemEReportada`; `BUILD FAILED`.

Restaurar: `git -C /Users/giorgioderico/Projetos/Tribuna checkout -- etapa0/build-logic/src/main/kotlin/io/github/gigioderico/tribuna/buildlogic/NativeLibScan.kt`

- [x] **Passo 11: confirmar a restauração**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew testDebugUnitTest && git -C /Users/giorgioderico/Projetos/Tribuna status --porcelain etapa0/build-logic`
Esperado: `BUILD SUCCESSFUL` e nenhuma linha do `git status`.

---

## Tarefa 6: módulo `:app` e ligação das verificações

**Arquivos:**
- Modificar: `etapa0/settings.gradle.kts`
- Criar: `etapa0/app/build.gradle.kts`, `etapa0/app/src/main/AndroidManifest.xml`, `etapa0/app/src/main/res/values/strings.xml`
- Teste: `etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/CoreFromAppTest.kt`
- Criar: `etapa0/scripts/verificar-manifesto-negativo.sh`

- [x] **Passo 1: incluir o `:app`** — em `etapa0/settings.gradle.kts`, depois de `include(":core")`, acrescentar:

```kotlin
include(":app")
```

- [x] **Passo 2: criar `etapa0/app/build.gradle.kts`**

Sem `org.jetbrains.kotlin.android` (o AGP 9 já traz Kotlin). Nenhum literal de versão: tudo vem do catalog.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
    id("tribuna.android.verifications")
}

android {
    namespace = "io.github.gigioderico.tribuna.etapa0"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "io.github.gigioderico.tribuna.etapa0"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    }
}

ktlint {
    version.set(libs.versions.ktlint.get())
}

dependencies {
    implementation(project(":core"))
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [x] **Passo 3: criar `etapa0/app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <application
        android:allowBackup="false"
        android:label="@string/app_name" />

</manifest>
```

- [x] **Passo 4: criar `etapa0/app/src/main/res/values/strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Tribuna Etapa 0</string>
</resources>
```

- [x] **Passo 5: escrever o teste que falha** — `etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/CoreFromAppTest.kt`

```kotlin
package io.github.gigioderico.tribuna.etapa0

import io.github.gigioderico.tribuna.etapa0.speech.FakeSpeechEngine
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Prova que os contratos do :core e o fake (testFixtures) chegam ao classpath do módulo Android. */
class CoreFromAppTest {
    @Test
    fun contratosEFakeDoCore_chegamAoClasspathDoApp() =
        runTest {
            val fake = FakeSpeechEngine()
            fake.prepare()
            fake.startCapture()
            val id = fake.beginSession()
            fake.hearPartial(listOf("ouvir"))
            assertEquals(SpeechEvent.Partial(id, listOf("ouvir")), fake.events.first())
        }
}
```

Este teste não depende de código de produção novo (o `:core` já existe): ele falha na compilação se a ligação `:app` → `:core`/`testFixtures` estiver errada. A prova de que ele roda é a contagem `tests="1"` no Passo 7.

- [x] **Passo 6: build**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew assembleDebug 2>&1 | tee ../work/0001-etapa0-contratos/evidencias/assembleDebug.txt`
Esperado: `BUILD SUCCESSFUL`; `app/build/outputs/apk/debug/app-debug.apk` existe.

- [x] **Passo 7: unitários de todos os builds**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew testDebugUnitTest 2>&1 | tee ../work/0001-etapa0-contratos/evidencias/testDebugUnitTest.txt`
Esperado: `BUILD SUCCESSFUL`; executadas `:testDebugUnitTest` (raiz → `build-logic:test`), `:core:test` e `:app:testDebugUnitTest`; `app/build/test-results/testDebugUnitTest/TEST-io.github.gigioderico.tribuna.etapa0.CoreFromAppTest.xml` com `tests="1" failures="0" errors="0"`.

- [x] **Passo 8: manifesto mesclado (caminho positivo)**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew verifyMergedManifest 2>&1 | tee ../work/0001-etapa0-contratos/evidencias/verifyMergedManifest.txt`
Esperado: `BUILD SUCCESSFUL`, com duas linhas `verifyDebugMergedManifest: OK .../merged_manifest/debug/.../AndroidManifest.xml -> android.permission.RECORD_AUDIO` e o equivalente para `verifyReleaseMergedManifest`.

Conferir o artefato real (Q4): abrir o arquivo citado na linha `OK` e confirmar que ele só tem `uses-permission` de `RECORD_AUDIO`.

- [x] **Passo 9: criar `etapa0/scripts/verificar-manifesto-negativo.sh`** e torná-lo executável (`chmod +x`)

```bash
#!/usr/bin/env bash
# Prova (Q2) que verifyMergedManifest falha com permissões proibidas e passa sem elas.
# Cria um manifesto temporário só na variante debug; o manifesto principal não é tocado.
set -u
cd "$(dirname "$0")/.."
OVERLAY_DIR=app/src/debug
OVERLAY="$OVERLAY_DIR/AndroidManifest.xml"
if [ -e "$OVERLAY" ]; then
  echo "abortado: $OVERLAY já existe"
  exit 2
fi
cleanup() {
  rm -f "$OVERLAY"
  rmdir "$OVERLAY_DIR" 2>/dev/null || true
}
trap cleanup EXIT
mkdir -p "$OVERLAY_DIR"
fail=0
for perm in INTERNET ACCESS_NETWORK_STATE WAKE_LOCK CAMERA; do
  cat > "$OVERLAY" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.$perm" />
</manifest>
EOF
  out=$(./gradlew :app:verifyDebugMergedManifest --console=plain 2>&1)
  code=$?
  if [ "$code" -ne 0 ] && grep -q "permissão não permitida: android.permission.$perm" <<<"$out"; then
    echo "OK: $perm -> verifyDebugMergedManifest falhou como esperado"
    grep "permissão não permitida" <<<"$out"
  else
    echo "ERRO: $perm -> código $code, sem a mensagem esperada"
    tail -20 <<<"$out"
    fail=1
  fi
done
cleanup
out=$(./gradlew :app:verifyDebugMergedManifest --console=plain 2>&1)
code=$?
if [ "$code" -eq 0 ]; then
  echo "OK: só RECORD_AUDIO -> verifyDebugMergedManifest passou"
else
  echo "ERRO: manifesto limpo falhou (código $code)"
  tail -20 <<<"$out"
  fail=1
fi
exit "$fail"
```

- [x] **Passo 10: rodar a prova negativa**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && bash scripts/verificar-manifesto-negativo.sh 2>&1 | tee ../work/0001-etapa0-contratos/evidencias/verifyMergedManifest-negativo.txt; echo "exit=${PIPESTATUS[0]}"`
Esperado: quatro linhas `OK: <PERM> -> verifyDebugMergedManifest falhou como esperado` (INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, CAMERA), cada uma seguida da linha `  - permissão não permitida: android.permission.<PERM>`, depois `OK: só RECORD_AUDIO -> verifyDebugMergedManifest passou` e `exit=0`. Depois: `ls etapa0/app/src/debug` não existe.

- [x] **Passo 11: lint e estilo**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew lint ktlintCheck 2>&1 | tee ../work/0001-etapa0-contratos/evidencias/lint-ktlint.txt`
Esperado: `BUILD SUCCESSFUL`, com `:app:lint`, `:core:lint` e as tarefas `ktlint*Check` de `:app` e `:core` executadas. Avisos do lint são aceitos e listados no registro; erros não.

- [x] **Passo 12: commit**

```bash
cd /Users/giorgioderico/Projetos/Tribuna && git add etapa0/settings.gradle.kts etapa0/app etapa0/scripts && git commit -m "feat(etapa0): módulo :app consumindo :core, manifesto só com RECORD_AUDIO e prova negativa do verifyMergedManifest (TRD §Manifesto, §Qualidade de código)"
```

---

## Tarefa 7: verificação final e evidências

- [x] **Passo 1: `verify16kAlignment` falha de forma explícita (esperado nesta tarefa)**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew verify16kAlignment 2>&1 | tee ../work/0001-etapa0-contratos/evidencias/verify16kAlignment.txt; echo "exit=${PIPESTATUS[0]}"`
Esperado: `BUILD FAILED`, `exit=1`, com:
- uma linha `verify16kAlignment: zipalign -c -P 16 -v 4 app-release-unsigned.apk -> Verification successful` (o APK em si está alinhado), e
- `  - app-release-unsigned.apk: nenhuma biblioteca nativa em lib/<abi>/; exigidas: libjnidispatch.so, libvosk.so`.

Se passar (`BUILD SUCCESSFUL`), é defeito: a tarefa estaria passando em silêncio sem as bibliotecas.

- [x] **Passo 2: regra 4 (`align` sem Android) e JVM pura**

Rodar:

```bash
cd /Users/giorgioderico/Projetos/Tribuna && grep -rnE "import (android|androidx)\." etapa0/core/src ; echo "grep-exit=$?" ; grep -nE "com.android.application|com.android.library|kotlin.android" etapa0/core/build.gradle.kts ; echo "plugin-exit=$?"
```

Esperado: `grep-exit=1` e `plugin-exit=1` (nenhum import Android no `:core`; o `:core` não aplica plugin Android de app/biblioteca; só o `com.android.lint`, que não compila para Android).

- [x] **Passo 3: versões só do catalog**

Rodar:

```bash
cd /Users/giorgioderico/Projetos/Tribuna && grep -rnE '[0-9]+\.[0-9]+' etapa0 --include='*.gradle.kts' ; echo "exit=$?"
```

Esperado: `exit=1` (nenhum literal de versão nos `build.gradle.kts`/`settings.gradle.kts`).

Rodar:

```bash
cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:dependencies --configuration runtimeClasspath :app:dependencies --configuration debugRuntimeClasspath buildEnvironment 2>&1 | tee ../work/0001-etapa0-contratos/evidencias/dependencies.txt | grep -E "kotlinx-coroutines-core|kotlin-stdlib|kotlin-gradle-plugin|com.android.tools.build:gradle:"
```

Esperado: `kotlinx-coroutines-core:1.11.0`, `kotlin-stdlib:2.4.21`, `kotlin-gradle-plugin:2.2.10 -> 2.4.21` (ou `2.4.21` direto) e `com.android.tools.build:gradle:9.4.1`; nenhuma versão fora das do catalog.

- [x] **Passo 4: sem layout XML e só `RECORD_AUDIO`**

Rodar:

```bash
cd /Users/giorgioderico/Projetos/Tribuna && find etapa0 -path '*/res/layout*' -not -path '*/build/*' ; grep -rn "uses-permission" etapa0 --include=AndroidManifest.xml --exclude-dir=build
```

Esperado: `find` não imprime nada; o `grep` imprime uma única linha, `etapa0/app/src/main/AndroidManifest.xml:...<uses-permission android:name="android.permission.RECORD_AUDIO" />`.

- [x] **Passo 5: JDK 21 e contagem de testes (Q3)**

Rodar:

```bash
cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew --version | tee ../work/0001-etapa0-contratos/evidencias/gradle-version.txt && grep -ho 'testsuite name="[^"]*" tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' core/build/test-results/test/*.xml app/build/test-results/testDebugUnitTest/*.xml build-logic/build/test-results/test/*.xml | tee ../work/0001-etapa0-contratos/evidencias/contagem-testes.txt
```

Esperado: `Daemon JVM` no JDK 21 (origem `gradle/gradle-daemon-jvm.properties`); sete suítes: `SpeechContractShapeTest` 3, `AlignContractShapeTest` 4, `FakeSpeechEngineTest` 18, `CoreFromAppTest` 1, `ManifestPermissionCheckTest` 8, `ElfAlignmentTest` 7, `NativeLibScanTest` 5 — total 46, todas com `skipped="0" failures="0" errors="0"`.

- [x] **Passo 6: rodada final do conjunto "tudo verde"**

Rodar: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew assembleDebug testDebugUnitTest lint ktlintCheck verifyMergedManifest 2>&1 | tee ../work/0001-etapa0-contratos/evidencias/final.txt`
Esperado: `BUILD SUCCESSFUL`.

- [x] **Passo 7: commit das evidências e registro da construção**

Preencher a seção "Registro da construção" abaixo (o que foi feito, testes, desvios, comandos e resultado; avisos de lint, se houver) e:

```bash
cd /Users/giorgioderico/Projetos/Tribuna && git add work/0001-etapa0-contratos/evidencias work/0001-etapa0-contratos/impl_plan.md && git commit -m "test(etapa0): evidências da 0001 (verificações, Q2, contagem de testes)"
```

---

## Verificação

Todos a partir de `/Users/giorgioderico/Projetos/Tribuna/etapa0`:

```bash
./gradlew --version
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lint ktlintCheck
./gradlew verifyMergedManifest
bash scripts/verificar-manifesto-negativo.sh
./gradlew verify16kAlignment                       # deve FALHAR nesta tarefa, de forma explícita
./gradlew :core:dependencies --configuration runtimeClasspath
./gradlew :app:dependencies --configuration debugRuntimeClasspath
```

Sucesso significa, critério a critério do spec:

| Critério do spec | Comando | Resultado esperado |
|---|---|---|
| 1. `assembleDebug` com JDK 21 e SDK 36 | `./gradlew --version`; `./gradlew assembleDebug` | `Daemon JVM` 21 (de `gradle-daemon-jvm.properties`); `BUILD SUCCESSFUL` |
| 2. `testDebugUnitTest` verde, ciclo de vida, cada `SpeechEvent`, `SessionId` crescente | `./gradlew testDebugUnitTest` + contagem (Tarefa 7, Passo 5) | `BUILD SUCCESSFUL`; 46 testes, 0 falhas; testes nomeados na seção "Testes primeiro" |
| 3. Callback atrasado com id antigo; vermelho se ler "sessão atual" | `FakeSpeechEngineTest.callbackAtrasado_comOutraSessaoAtiva_chegaComOIdAntigo` + quebra A | Verde no código; `FAILED` com a quebra A (`evidencias/q2-quebra-a-sessao-atual.txt`) |
| 4. Depois de `endSession`, sem `Partial`/`Final`; `Level` continua | `FakeSpeechEngineTest.endSession_naoEmiteMaisPartialNemFinal_eLevelContinua` + quebra B | Verde no código; `FAILED` com a quebra B (`evidencias/q2-quebra-b-endsession.txt`) |
| 5. `align` sem Android, JVM pura | grep da Tarefa 7, Passo 2; `:core` usa `kotlin("jvm")` | `grep-exit=1`, `plugin-exit=1`; `:core:test` roda na JVM |
| 6. `verifyMergedManifest` passa; falha com `INTERNET`/`ACCESS_NETWORK_STATE`/`WAKE_LOCK`/outra | `./gradlew verifyMergedManifest`; `scripts/verificar-manifesto-negativo.sh`; `ManifestPermissionCheckTest` | Passa no projeto; 4 falhas esperadas + 1 passagem no script, `exit=0`; 8 unitários verdes |
| 7. `verify16kAlignment` existe, roda e falha explicitamente sem as bibliotecas | `./gradlew verify16kAlignment`; `ElfAlignmentTest`, `NativeLibScanTest`; quebra C | `BUILD FAILED` com "nenhuma biblioteca nativa em lib/<abi>/; exigidas: libjnidispatch.so, libvosk.so"; 12 unitários verdes; quebra C vermelha |
| 8. `lint ktlintCheck` sem erros | `./gradlew lint ktlintCheck` | `BUILD SUCCESSFUL` |
| 9. Versões só do catalog | grep da Tarefa 7, Passo 3; `dependencies` | `exit=1` no grep; versões iguais às do catalog |
| 10. Sem layout XML; `uses-permission` só `RECORD_AUDIO` | `find`/`grep` da Tarefa 7, Passo 4 | `find` vazio; uma linha com `RECORD_AUDIO` |

## Riscos

- **R1 — AGP 9.4.1 acima do intervalo testado do KGP 2.4.21 (AGP 8.5.2–9.3.1).** Sintoma: aviso de compatibilidade ou erro ao configurar o Kotlin no `:app`. Se for só aviso, seguir e registrar. Se falhar o build: trocar no catalog `agp = "9.4.1"` por `agp = "9.3.3"` (última 9.3.x no Google Maven em 10/10/2026, dentro do intervalo), manter Gradle 9.7.1, registrar o desvio e a saída. Essa troca já fica aprovada com este plano. Se também falhar, parar e escalar ao `arquiteto`.
- **R2 — Gradle 9.7.1 versus "máximo 9.7.0" do KGP.** É patch da mesma linha 9.7; não se espera problema. Se o KGP reclamar, usar 9.7.0 (regerar o wrapper com o SHA-256 de `https://services.gradle.org/distributions/gradle-9.7.0-bin.zip.sha256`) e registrar.
- **R3 — Lint com Kotlin 2.4.** O lint do AGP 9.4 pode avisar que a versão do Kotlin é mais nova que a que ele conhece. Aviso: registrar. Erro: parar e escalar; não desligar o lint nem baixar `abortOnError`.
- **R4 — Daemon fora do JDK 21.** Se `./gradlew --version` mostrar o 17 (do `org.gradle.java.home` do usuário) mesmo com `gradle-daemon-jvm.properties`, parar e escalar ao humano, com a saída. Não editar `~/.gradle/gradle.properties` (é do usuário e tem segredos de outros projetos) e não contornar com flag em cada comando sem aprovação.
- **R5 — Plugin do `build-logic` não enxerga as classes do AGP** (`NoClassDefFoundError`/`ClassCastException` com `ApplicationAndroidComponentsExtension`). O padrão (AGP `apply false` na raiz e `compileOnly` no `build-logic`) é o usado em projetos de referência; se falhar, escalar ao `arquiteto` com a saída. Não mover a lógica para o `build.gradle.kts` do `:app` (perderia os testes da Q2).
- **R6 — `testFixtures(project(":core"))` não resolve no módulo Android.** Sintoma: `CoreFromAppTest` não compila. Escalar ao `arquiteto`; não mover o fake para o `main` do `:core` (iria para o APK).
- **R7 — ktlint-gradle não cria `ktlintCheck` no `:app` com o Kotlin embutido.** Conferir com `./gradlew :app:tasks --all | grep -i ktlint`. Se faltar, escalar; não remover o ktlint do `:app`.
- **R8 — Nova DSL do AGP 9 recusar `compileSdk = Int`.** Se der erro (não aviso), usar `compileSdk { version = release(libs.versions.compileSdk.get().toInt()) }` e registrar.
- **R9 — O AGP injetar alguma permissão no manifesto de debug ou release.** O `verifyMergedManifest` vai reprovar. Não afrouxar a lista; parar e escalar com o manifesto mesclado anexado.
- **R10 — `verify16kAlignment` vermelho por desenho nesta tarefa.** Não entra no conjunto "tudo verde" da 0001; o revisor deve conferir que a falha é a explícita esperada. A execução real contra `libvosk.so`/`libjnidispatch.so` é da 0003.
- **Se o Android não se comportar como o TRD supõe:** nesta tarefa não há Vosk nem aparelho. O ponto de contato com o Android é o AGP (R1, R3, R8, R9). Qualquer divergência entre o que o TRD pede (manifesto mesclado, `zipalign -c -P 16`, segmentos ELF) e o que a ferramenta permite vai escrita no registro, com a saída real, e escalada ao `arquiteto`; não contornar em silêncio.
- **Pendência para depois da Etapa 0:** os pacotes do `:core` estão em `io.github.gigioderico.tribuna.etapa0.*`. Quando o app consumir ou copiar o código, haverá troca de pacote; a decisão é posterior (spec, decisão 2).

---

## Registro da construção (preenchido pelo agente de construção)

**Agente:** motor-voz · **Data:** 2026-10-10 · **Branch:** `work/0001-etapa0-contratos` (sem push) · **Commit(s):** `4abb406` (T1), `3366484` (T2), `6908074` (T3), `d4b0301` e `407ccba` (T4), `81fd72f` e `d6fa9b0` (T5), `5cf852f` (T6), e o commit de evidências da T7 que traz este registro.

Este registro descreve a construção. Não é revisão: o veredito é do `revisor` em `code_review.md`.

- **Feito:** Tarefas 1 a 7 na ordem, todos os passos marcados. Projeto `etapa0/` com Gradle 9.7.1 (wrapper com SHA-256), AGP 9.4.1, KGP 2.4.21, daemon no JDK 21 por `gradle/gradle-daemon-jvm.properties`; `:core` JVM puro com os contratos de `speech` e `align` copiados do TRD §Contratos e o `FakeSpeechEngine` em `testFixtures`; `build-logic` com o plugin `tribuna.android.verifications` (`verifyMergedManifest` por variante e agregada, `verify16kAlignment` no release); `:app` Android mínimo, sem tela, só com `RECORD_AUDIO` e `allowBackup="false"`, consumindo `:core` e o fake; script `etapa0/scripts/verificar-manifesto-negativo.sh`.
- **Testes criados (46, 0 falhas, 0 ignorados):** `SpeechContractShapeTest` 3, `AlignContractShapeTest` 4, `FakeSpeechEngineTest` 18, `CoreFromAppTest` 1, `ManifestPermissionCheckTest` 8, `ElfAlignmentTest` 7, `NativeLibScanTest` 5 (`evidencias/contagem-testes.txt`). Cada suíte foi vista falhando na compilação antes do código de produção. Quebras propositais da Q2, todas vermelhas e restauradas com `git checkout` (árvore limpa conferida depois):
  - A (callback lê a sessão ativa): `callbackAtrasado_comOutraSessaoAtiva_chegaComOIdAntigo` falha com `expected:<[Partial(session=SessionId(value=1)...` `but was:<[Partial(session=SessionId(value=2)...` (`evidencias/q2-quebra-a-sessao-atual.txt`).
  - B (`endSession` não solta o reconhecedor): `endSession_naoEmiteMaisPartialNemFinal_eLevelContinua` falha com `expected:<Level(rms=0.4)> but was:<Partial(session=SessionId(value=1), words=[ouvir, bem])>` (`evidencias/q2-quebra-b-endsession.txt`).
  - C (varredura ignora desalinhamento): `libExigidaDesalinhada_eReportada` e `outraLibDesalinhada_tambemEReportada` falham com `but was:<[]>` (`evidencias/q2-quebra-c-elf.txt`).
- **Desvios do plano e motivo:**
  1. `NativeLibScan.kt`, só o KDoc: o plano trazia `Varre lib/<abi>/*.so de um APK`; em Kotlin, `/*` dentro de um comentário abre um comentário aninhado e a compilação falhou com `NativeLibScan.kt:52:1 Syntax error: Unclosed comment`. Trocado por `Varre as .so em lib/<abi>/ de um APK`. Nenhum código mudou.
  2. Formatação pelo `ktlintFormat` (permitida pelo plano, T2 P8): `core/build.gradle.kts` e `app/build.gradle.kts` tiveram as cadeias `libs.versions.x.get().toInt()` quebradas em linhas (`standard:chain-method-continuation`); em `Aligner.kt`, o comentário `// toque, Volume −, arrasto, troca de modo` passou da linha da assinatura de `reposition` para a linha seguinte (`standard:function-signature`). Nome, tipo, ordem e padrão dos contratos não mudaram (diff conferido).
  3. Evidências da Q2: além da saída do Gradle (que no console só mostra `AssertionError at <linha>`), cada arquivo traz o diff da quebra e a mensagem `<failure message=...>` do XML de resultado, para a comparação esperado/real pedida no plano ficar anexada.
  4. Trailer dos commits: a instrução do orquestrador pedia `Co-Authored-By: Claude Sonnet 5.5`; os commits usam `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`, que é o modelo que de fato executou esta sessão e a atribuição configurada no ambiente.
  - R1 (AGP 9.4.1 acima do intervalo testado do KGP) não se manifestou: nenhum erro nem aviso; o plano B (AGP 9.3.3) não foi usado. R2, R4–R9 também não ocorreram. R3: nenhum aviso de versão do Kotlin no lint.
- **Avisos do lint (0 erros, 3 avisos, aceitos pelo plano T6 P11; `evidencias/lint-ktlint.txt`):**
  - `:app` `DataExtractionRules` em `android:allowBackup`: o TRD §Ciclo de vida (Manifesto) pede também regras de extração vazias, que o plano não incluiu. Registrado como OPR-002 no `registers/opr_register.md`.
  - `:app` `MissingApplicationIcon`: app de teste sem tela; ícone fica para a 0003.
  - `:core` `GradleDependency`: `compileSdk 37` disponível; o TRD fixa 36.
- **Observações para o revisor:**
  - `verify16kAlignment` falha de propósito nesta tarefa (R10), com a mensagem explícita esperada; `zipalign -c -P 16 -v 4` do APK em si passa (`Verification successful`).
  - O ktlint não cobre `build-logic`, `etapa0/build.gradle.kts` nem `etapa0/settings.gradle.kts` (o plano não aplica o plugin lá). Registrado como OPR-003.
  - `buildEnvironment` mostra `kotlin-stdlib {strictly 2.4.0}` e `kotlinx-coroutines-core-jvm 1.9.0` no classpath de build: vêm do Kotlin embutido do Gradle 9.7.1 e de dependências transitivas do AGP, não de declaração nossa nem do APK. Os classpaths de execução de `:core` e `:app` só têm `kotlinx-coroutines-core:1.11.0` e `kotlin-stdlib:2.4.21`.
  - O `./gradlew clean` da raiz não limpa o build incluído `build-logic`; a rodada final foi feita com `--rerun-tasks` (85 tarefas executadas, nenhuma do cache).
- **Comandos rodados e resultado** (a partir de `etapa0/`; saídas em `work/0001-etapa0-contratos/evidencias/`):

| Comando | Resultado | Evidência |
|---|---|---|
| `./gradlew --version` | Gradle 9.7.1; `Daemon JVM: Compatible with Java 21 ... (from gradle/gradle-daemon-jvm.properties)`; buildEnvironment: `Daemon JVM: Eclipse Temurin JDK 21 (21.0.10+7-LTS)` | `gradle-version.txt`, `dependencies.txt` |
| `./gradlew assembleDebug` | BUILD SUCCESSFUL; `app/build/outputs/apk/debug/app-debug.apk` | `assembleDebug.txt` |
| `./gradlew testDebugUnitTest` | BUILD SUCCESSFUL; `:build-logic:test`, `:core:test`, `:app:testDebugUnitTest`; 46 testes | `testDebugUnitTest.txt`, `contagem-testes.txt` |
| `./gradlew verifyMergedManifest` | BUILD SUCCESSFUL; debug e release `OK ... -> android.permission.RECORD_AUDIO`; manifestos mesclados reais anexados (Q4) | `verifyMergedManifest.txt` |
| `bash scripts/verificar-manifesto-negativo.sh` | INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK e CAMERA reprovados com a mensagem; só RECORD_AUDIO passa; `exit=0`; `app/src/debug` removido | `verifyMergedManifest-negativo.txt` |
| `./gradlew lint ktlintCheck` | BUILD SUCCESSFUL; 0 erros, 3 avisos (acima) | `lint-ktlint.txt` |
| `./gradlew verify16kAlignment` | BUILD FAILED, `exit=1`, como esperado: `app-release-unsigned.apk: nenhuma biblioteca nativa em lib/<abi>/; exigidas: libjnidispatch.so, libvosk.so` | `verify16kAlignment.txt` |
| greps da T7 P2–P4 | `grep-exit=1`, `plugin-exit=1`, literal de versão `exit=1`, nenhum layout, uma só `uses-permission` (`RECORD_AUDIO`) | `checagens-estaticas.txt` |
| `./gradlew :core:dependencies ... :app:dependencies ... buildEnvironment` | coroutines 1.11.0, stdlib 2.4.21, AGP 9.4.1, KGP 2.2.10 -> 2.4.21, ktlint-gradle 14.2.0 | `dependencies.txt` |
| `./gradlew assembleDebug testDebugUnitTest lint ktlintCheck verifyMergedManifest --rerun-tasks` | BUILD SUCCESSFUL, `exit=0`, 85 tarefas executadas | `final.txt` |

### Rodada 2 (volta ao BUILD após reprovação do revisor; achado 1)

- **Achado 1 (BLOQUEANTE):** nenhum teste cobria `Error` atrasado de sessão antiga. Criado `FakeSpeechEngineTest.callbackAtrasado_errorDaSessaoAntiga_chegaComOIdAntigo` (critério 3). Verde no código atual.
- **Quebra D (Q2):** `FakeRecognizer.error` passou a emitir `Error(active?.session, cause)`. Teste VERMELHO: `expected:<Error(session=SessionId(value=1), cause=Internal)> but was:<Error(session=SessionId(value=2), cause=Internal)>`. Fake restaurado com `git checkout`; `git diff` do fake vazio. Evidência: `evidencias/q2-quebra-d-error-sessao-atual.txt`.
- **Contagem:** 45 para 46 testes (`FakeSpeechEngineTest` 17 para 18), 0 falhas, 0 ignorados; citações de 45 neste plano ajustadas (as seções anteriores a esta rodada foram atualizadas só nas linhas de total). Evidência: `evidencias/contagem-testes-rodada2.txt`.
- **Tudo-verde:** `./gradlew assembleDebug testDebugUnitTest lint ktlintCheck verifyMergedManifest --rerun-tasks` = BUILD SUCCESSFUL, 85 tarefas executadas (`evidencias/final-rodada2.txt`).
- **Desvios:** nenhum. Achados 2 a 4 (OPR-004, OPR-005, OPR-006) não tocados; código do fake e contratos inalterados.
