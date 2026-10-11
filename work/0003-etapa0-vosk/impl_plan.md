# Plano — 0003-etapa0-vosk

**Autor:** arquiteto · **Spec:** `spec.md` (conferido pelo orquestrador em 2026-10-11; portão humano 1 aprovado por Giorgio Derico em 2026-10-11, opção A da `change_impact_analysis.md`) · **Aprovação:** conferida pelo orquestrador em 2026-10-11 (checklist do PROCESS.md: critérios com tarefa e comando, testes antes do código, Q6, contratos Kotlin intactos, nenhuma permissão nova; portão 1 já aprovado por humano; OPR-024 (libvosk 32 bits a 4 KB) segue o caminho "reprova por alinhamento" do B5 do spec, sem afrouxar a verificação)

> **Para os agentes de construção:** este plano foi escrito com `superpowers:writing-plans`, no formato de `templates/impl_plan.md`. Siga as tarefas na ordem da seção "Ordem e paralelismo", com teste antes do código, e marque os passos (`- [ ]`). A execução é dos agentes `motor-voz` e `interface`, sob o ciclo do `PROCESS.md`; não use as skills de execução de plano da superpowers. Cada bloco de código abaixo é o conteúdo **inteiro** do arquivo (criar ou substituir), já validado numa cópia do repositório (seção "Prova do plano").

## Resumo

Liga o Vosk ao núcleo da 0002 no `etapa0/:app`, sem tocar no `:core`. O `VoskEngine` é Kotlin puro sobre costuras internas (fonte de áudio, reconhecedor, carregador de modelo, laço de captura), testado na JVM com falsos; no motor, só `MicAudioSource.kt` e `VoskDecoders.kt` falam com Android ou com `org.vosk.*`. A tela mínima tem um controlador puro (`ReaderController`, testado com o `FakeSpeechEngine` e relógio virtual) dentro de um `ViewModel`, e a exportação só existe para textos de `etapa0/textos/`. O build ganha manifesto da CIA, R8, verificações ligadas ao empacotamento e 16 KB também no AAB.

## Decisões do arquiteto

Cada decisão traz o que estava em aberto, a escolha e o porquê. Nenhuma muda assinatura ou semântica de contrato do TRD (`SpeechEngine`, `SpeechEvent`, `SessionId`, `EngineState`, `EngineError`, `Token`, `Aligner`/`AlignState`); o `:core` não muda (critério A5).

### A1. Versões e origem das dependências (critério A3)

Conferidas em 2026-10-11 nos `maven-metadata.xml` e no POM/`.module`:

| Dependência | Versão | Origem | Observação |
|---|---|---|---|
| `com.alphacephei:vosk-android` | 0.3.75 | Maven Central | Mais recente (`<release>0.3.75</release>`, publicada em 2025-12-08). O analista citava 0.3.47; há 0.3.70 e 0.3.75 depois dela. O `.module` pede `net.java.dev.jna:jna:5.18.1` com `type=aar` e exclui tudo da JNA. |
| `net.java.dev.jna:jna` (`aar`) | 5.19.1 | Maven Central | Mais recente (`<release>5.19.1</release>`). Declarada com `artifact { type = "aar" }`; o Gradle resolve `5.18.1 -> 5.19.1` só como AAR (o JAR traria bibliotecas de desktop). |
| `androidx.compose:compose-bom` | 2026.06.01 | Google Maven | Compose UI 1.11.4, material3 1.4.0. A 2026.08.00 e a 2026.09.00 exigem compileSdk 37 (CIA E4; OPR-023). |
| `androidx.activity:activity-compose` | 1.13.0 | Google Maven | |
| `androidx.lifecycle:lifecycle-viewmodel-compose` e `lifecycle-runtime-compose` | 2.10.0 | Google Maven | A 2.11.0 exige compileSdk 37 (OPR-023). |
| plugin `org.jetbrains.kotlin.plugin.compose` | 2.4.21 (= Kotlin) | Gradle Plugin Portal | Obrigatório para Compose com Kotlin 2.x. |
| modelo `vosk-model-small-pt-0.3` | zip de 32.453.112 bytes, SHA-256 `6e1ce909032e1afa7a88e68a3d628ecafff302bdf195befab308826c395e93b7` | `https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip` | Apache 2.0 (CONFORMIDADE). 15 arquivos, 53.552.717 bytes descompactado. |

compileSdk continua 36 (CLAUDE.md, Stack). O lint não avisa `GradleDependency` nessas versões hoje; se avisar depois, o motivo é o OPR-023.

### A2. Onde fica cada coisa no `:app`

Pacotes em `etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/`:

- `speech/` (motor-voz): costuras e `VoskEngine` (Kotlin puro), `ModelInstaller`, `WavAudioSource`, `FstSymbols`, `Vocabulary`. Só `MicAudioSource.kt` e `VoskDecoders.kt` importam `android.*` ou `org.vosk.*`.
- `export/` (motor-voz): catálogo dos textos de teste, gravação em memória, formato dos arquivos e escrita.
- `viewmodel/` (interface): `ReaderController` (Kotlin puro, dono do texto, do cursor e da leitura) e `ReaderViewModel` (casca Android que monta as dependências reais).
- `ui/` (interface): `ReaderScreen`, `PromptText`, `DebugPanel`, `HitTest`.
- `MainActivity.kt` fica no pacote raiz, porque o manifesto aprovado no portão 1 declara `.MainActivity`.

O `FakeSpeechEngine` continua em `testFixtures` do `:core`. Nenhum arquivo do `:core/src/main` muda.

### A3. Costuras para a JVM (brainstorm D1)

| Costura | Produção | Teste | Consumidor (Q1) |
|---|---|---|---|
| `AudioSource` | `MicAudioSource` (`AudioRecord`), `WavAudioSource` | `FakeAudioSource` | laço do `VoskEngine` |
| `Decoder` / `DecoderFactory` | `VoskDecoder` / `VoskDecoderFactory` (`org.vosk.Recognizer`) | `FakeDecoder` / `FakeDecoderFactory` | `Session` do `VoskEngine` |
| `ModelLoader` | `CachingModelLoader(VoskModelLoader)`, um por processo (`VoskModels`) | lambdas e `CachingModelLoader` com contador | `VoskEngine.prepare` |
| `CaptureDriver` | `ThreadCaptureDriver` (uma thread por captura) | `ManualCaptureDriver` (o teste chama o passo) | `VoskEngine.startCapture/stopCapture` |
| `AssetTree` | `AndroidAssetTree` (`AssetManager`) | diretório temporário | `ModelInstaller`, `TestTextCatalog` |
| `ExportSink` | `ExportWriter` (diretório externo do app) | mapa em memória | `ReaderController.onExport` |
| relógio e dispatchers | `SystemClock::elapsedRealtime`, `Dispatchers.Default`/`IO` | `testScheduler.currentTime`, `StandardTestDispatcher` | `ReaderController` |

Nenhum teste JVM referencia `org.vosk.*` (as classes estão no classpath, mas nunca são carregadas).

### A4. Sessões, threads e blocos no `VoskEngine` (TRD §Contratos, regras; Motor de voz §Captura e sessões)

- `beginSession` cria o `SessionId` e o reconhecedor juntos num objeto `Session`. Todo evento sai com o `id` guardado no `Session`. O laço lê o `Session` atual só para decidir para onde vai o bloco (rotear áudio), nunca para carimbar o evento (CLAUDE.md, regra 7).
- `endSession` desliga o `Session` do motor e marca `ended`. Se um bloco estiver no reconhecedor naquele instante, o evento sai com o id antigo e a liberação do reconhecedor é adiada até o bloco terminar (nunca `close()` com `accept()` em curso, o que derrubaria o nativo). Não chama resultado final (D14).
- O laço lê blocos de 3.200 amostras (0,2 s), o mesmo `BUFFER_SIZE_SECONDS` do serviço de referência do vosk-android 0.3.75 (fonte lida em `vosk-android-0.3.75-sources.jar`). Fonte `AudioSource.VOICE_RECOGNITION` (constante 6), a mesma do Vosk. Não é parâmetro do PRD: é o valor da referência, medido na 0005 (latência 1).
- Estratégia `Reset` (D4): o reconhecedor de uma sessão encerrada passa por `reset()` e fica guardado; o próximo `beginSession` com o mesmo vocabulário o reaproveita, e com vocabulário diferente fecha o guardado e cria outro.
- `prepare()` carrega o modelo pelo `ModelLoader`; o `CachingModelLoader` por processo garante "uma vez por processo" mesmo quando o motor é recriado (troca de estratégia ou fonte, D4/D8). Falha não fica em cache ("Tentar de novo").
- Fonte WAV (D8): depois do fim dos dados, entrega zeros no mesmo ritmo, como microfone mudo; o reconhecedor fecha a frase sozinho, sem caso especial no motor.

### A5. OPR-020 confirmado: léxico do modelo × `Token.spoken` (divergência TRD × realidade)

Pesquisa por comando (Q4) no zip real:

- O `vosk-model-small-pt-0.3` não traz `words.txt`. O léxico é a tabela de símbolos de entrada do `Gr.fst` (cabeçalho OpenFst: tipo `ngram`, `flags=3`, tabela `exp/chain/tdnn/lgraph/words.txt` com 99.105 símbolos). O modelo é de grafo com lookahead (`HCLr.fst` + `Gr.fst`), então aceita lista de palavras (o KDoc de `Recognizer(Model, float, String)` diz que modelos HCLG pré-compilados não aceitam).
- O léxico tem a grafia com acento: `versículo`, `três`, `não`, `joão`, `coração` estão; `versiculo`, `nao`, `joao` não estão (`tres`, `voce`, `capitulo` aparecem como entradas soltas).
- Nas formas faladas distintas dos textos de teste: T01 tem 12 de 130 fora do léxico, T02 12 de 90, T09 3 de 33, L30 165 de 1.213. Entre as que faltam está `nao` (a palavra mais comum do português falado). Com a lista na forma literal do TRD, o Vosk ignoraria essas palavras e nunca as reconheceria.

É divergência documento × realidade. A solução mais conservadora dentro do spec, sem mudar contrato:

- **Lista = forma do TRD + grafias do léxico + `[unk]`.** Primeiro cada palavra de `Token.spoken`, uma vez, na ordem (C5 continua literal). Depois, as grafias do léxico cuja forma normalizada (`Normalizer`) é uma dessas palavras, ou uma parte de palavra com hífen do texto exibido (`bom-dia` → `bom`, `dia`). Por fim `[unk]`. Palavras fora do léxico seguem na lista e o Vosk as ignora, como hoje. Sem léxico, a lista é a do TRD.
- Depois da expansão, só 1 forma do T02 (`corintios`) e 13 do L30 ficam sem grafia no léxico (partes juntadas por hífen, como `bomdia`, `escrevelos`, e palavras raras).
- O léxico vem do próprio modelo instalado (`FstSymbols` lê só o cabeçalho do `Gr.fst`), então sempre corresponde ao modelo em assets (Q8). É lido só quando o vocabulário é ligado.
- Contratos intactos: `beginSession(vocabulary: List<String>?)` e `Token` não mudam; só o conteúdo da lista, que o TRD descreve em prosa.

Texto proposto para o TRD §Vocabulário restrito (aplicar com aprovação humana; não bloqueia o BUILD):

```markdown
**Vocabulário restrito.** O Vosk aceita uma lista de palavras na criação do Recognizer. A lista tem as formas faladas do roteiro (palavras de `Token.spoken`), mais as grafias do léxico do modelo que normalizam para elas ou para as partes de palavras com hífen do texto (o léxico do modelo pequeno tem acento: "três", "versículo", "não"; palavra fora do léxico é ignorada pelo Vosk), mais `[unk]`. No `vosk-model-small-pt-0.3`, o léxico é a tabela de símbolos do `Gr.fst`. Pode melhorar a precisão, mas piorar o improviso; a Etapa 0 decide se fica ligado, desligado ou configurável.
```

Achado novo, fora desta tarefa (OPR-025): o léxico não tem nenhuma palavra com hífen, então o Vosk diz "bom dia" e "segunda feira" em duas palavras, e o `Normalizer` junta `bom-dia` em `bomdia` no roteiro (0002 D18). O alinhador vai comparar `bomdia` com `bom`/`dia`. Medir na 0005 (T03, L30) e decidir no `align` se o hífen vira espaço.

### A6. OPR-021: `StorageService` do vosk-android 0.3.75 (fonte lida)

`StorageService.sync` copia `assets/<origem>` para `getExternalFilesDir(null)/<destino>` (armazenamento **externo** do app), compara só a primeira linha do `uuid` e não tem progresso: `unpack` roda num executor e avisa só no fim (`completeCallback`). O TRD §Instalação 1 pede armazenamento interno e progresso na tela 1b. Escolha: `ModelInstaller` com cópia própria para `filesDir/model-pt` (interno), com o mesmo mecanismo de `uuid` (D12), cópia em `model-pt.tmp` e troca de nome no fim. O app poderá contar bytes nessa mesma cópia para a barra da tela 1b. A etapa0 não mostra progresso (fora do escopo).

Texto proposto para o TRD §Instalação do modelo, item 1 (aprovação humana; não bloqueia):

```markdown
1. Na primeira execução, ModelInstaller copia assets/model-pt para o armazenamento interno (`filesDir/model-pt`), com cópia própria e o mesmo arquivo `uuid` do StorageService do Vosk, mostrando o progresso (bytes copiados) na tela 1b. O StorageService do vosk-android 0.3.75 copia para o armazenamento externo do app e só avisa no fim, por isso não é usado.
```

### A7. ABIs empacotadas

A JNA 5.19.1 traz `libjnidispatch.so` também para `armeabi`, `mips` e `mips64`, onde o vosk-android não tem `libvosk.so`. Sem filtro, o APK de release sai com 7 ABIs e o `verify16kAlignment` reprova com `armeabi sem libvosk.so`, `mips sem libvosk.so` e `mips64 sem libvosk.so` (o AAB já sai só com 4). O app nessas ABIs quebraria ao carregar o Vosk. Decisão: `ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64") }`, que são as ABIs do AAR do Vosk. A verificação continua exigindo as duas bibliotecas em toda ABI presente (não é afrouxamento).

### A8. OPR-005 e 16 KB contra as bibliotecas reais

Medido na prova (APK e AAB de release, `verify16kAlignment` com o relatório novo por biblioteca):

| ABI | `libvosk.so` | `libjnidispatch.so` |
|---|---|---|
| arm64-v8a | 16384 | 16384 |
| x86_64 | 16384 | 16384 |
| armeabi-v7a | **4096** | 16384 |
| x86 | **4096** | 16384 |

`zipalign -c -P 16 -v 4` no APK: "Verification successful". O AAB tem as quatro ABIs em `base/lib/` com os mesmos valores. Resultado: `verify16kAlignment` reprova pelas duas `libvosk.so` de 32 bits. Isso é o caminho "Reprova por alinhamento" do critério B5: fecha com a saída anexada e o OPR-024 aberto. A verificação não é afrouxada.

Fonte para a decisão humana do fim da Etapa 0: a página de 16 KB do Android (https://developer.android.com/guide/practices/page-sizes) diz que "all apps targeting Android 15 (API level 35) and higher must support 16 KB memory page sizes on 64-bit devices on Google Play", com prazo de 1º de fevereiro de 2027 para atualizações, e só cita bibliotecas `arm64-v8a` e `x86_64`. Não diz que as de 32 bits estão isentas. Caminhos no OPR-024: (a) versão mais nova do vosk-android (hoje não há); (b) recompilar as de 32 bits (portão humano 6); (c) mudar o TRD §Qualidade de código para exigir 16 KB só nas ABIs de 64 bits (aprovação humana); (d) tirar as ABIs de 32 bits do app (muda o público: aparelho de entrada com Android 10 de 32 bits; portão humano 2/3).

### A9. OPR-004, OPR-018 e `.editorconfig`

- OPR-004: o plugin `tribuna.android.verifications` liga `check` ao agregado `verifyMergedManifest` e `assemble<Variante>`/`bundle<Variante>` à verificação da variante (`tasks.configureEach` pelo nome). Prova: `--dry-run` (B4) e o script negativo (B2).
- OPR-018: a premissa não vale mais. Na cópia, com um `.kt` com violação em `app/src/test/kotlin` e outro em `app/src/main/kotlin`, `runKtlintCheckOverTestSourceSet` e `runKtlintCheckOverMainSourceSet` executam e falham; o `NO-SOURCE` do `main` era só falta de fonte. O critério A8 continua com a prova negativa por comando, agora com código real no `:app`.
- `.editorconfig`: `ktlint_function_naming_ignore_when_annotated_with = Composable`. Sem isso, o ktlint 1.8.0 reprova todo `@Composable` com nome em maiúscula (convenção do Compose).
- OPR-003 e OPR-006 ficam fora (o spec não pede); só mexo no `build-logic` no que o OPR-004 e o OPR-005 exigem.

### A10. Erros (brainstorm D15) e onde a decisão não estava escrita

- Falha na cópia do modelo (sem espaço, leitura corrompida) → `ModelCorrupt` ("modelo inutilizável"); asset ou `uuid` ausente → `ModelMissing`; `Model(path)` com `IOException` ou `UnsatisfiedLinkError` → `ModelCorrupt`.
- WAV que não abre ou fora de 16 kHz/mono/PCM 16 → `Error(null, MicUnavailable)` no `startCapture` (é a fonte de áudio indisponível).
- Reconhecedor que não é criado no `beginSession` → `Error(id, Internal)` com o id novo; o `beginSession` devolve o id assim mesmo (o contrato exige um `SessionId`).
- Reconhecedor que lança exceção num bloco → `Error(id, Internal)`, e aquele reconhecedor não recebe mais áudio.

### A11. Exportação (brainstorm D7–D11)

- Diretório: `getExternalFilesDir("exportacoes")` (sem permissão; `adb pull /sdcard/Android/data/io.github.gigioderico.tribuna.etapa0/files/exportacoes/`). WAVs de teste em `getExternalFilesDir("wav")` (`adb push`).
- Nome: o ID do texto de teste mais sufixo opcional `(_[a-z0-9]+)*` (`T02_giorgio_salao_t2`). Nada de texto livre no nome de arquivo (Q6). Nome inválido: a exportação é recusada e a tela avisa.
- A gravação existe só quando o texto atual é idêntico a um de `etapa0/textos/` (empacotados em `assets/textos/` por uma tarefa do Gradle a partir de `etapa0/textos/`, sem cópia no repositório). Vai do Ouvir com a leitura parada até o próximo Ouvir com a leitura parada; Parar encerra a captura e mantém a gravação para exportar.
- `accepted = 1` quando o evento foi entregue ao reconciliador: da sessão ativa e, se for parcial, com nível ≥ limiar. Assim, reprocessar só as linhas aceitas reproduz as palavras entregues ao vivo (D7). Erro tem `accepted = 1` quando é da captura ou da sessão ativa.
- `meta.txt`: as chaves de D11 mais `text_id` (o golden e o relatório precisam saber qual texto, Q8) e `animation_ms` (latência 3 = duração configurada, TRD Metas). Ajustes copiados no início da gravação, para não descrever um ajuste trocado depois (Q8).
- `level.csv`: `rms` com 6 casas (`Locale.ROOT`). `trajectory.csv`: `score` com `Double.toString` (o D3 pede `0,0,Reposition,0.0`).
- Leitura do critério A7: as escritas em arquivo do app são duas, a exportação (`ExportWriter`) e a cópia do modelo de assets (`ModelInstaller`, TRD §Instalação), que não é texto nem áudio. As duas usam `FileOutputStream` explícito para o grep listá-las.
- Leitura da mitigação 2 da CIA: o grep de `registerReceiver|receiveBroadcasts|RECEIVER_NOT_EXPORTED` é feito só nos `.kt`, porque o manifesto aprovado contém o nome `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (que casa com `RECEIVER_NOT_EXPORTED`) e um comentário que cita `registerReceiver`.

### A12. Logs (D28; Q6)

- Nenhuma chamada de `android.util.Log` no `:app` (o grep de A7 sai vazio).
- O Kaldi, dentro do `libvosk.so`, escreve avisos que citam as palavras da lista de vocabulário ("Ignoring word missing in vocabulary"), que vêm do texto. O `VoskModelLoader` chama `LibVosk.vosk_set_log_level(-2)` (só erros) antes de carregar o modelo. O `LogLevel.WARNINGS` (-1) do vosk-android ainda imprime avisos.
- R8 com `-assumenosideeffects class android.util.Log`, como o TRD pede para o app.

### A13. Tela mínima (TRD Etapa 0, item 1; D10, D13, D16, D18)

- Linha de leitura a 30% da altura (padrão do PRD A10); texto já lido com 40% de opacidade (padrão do A9); palavra atual com `colorScheme.primary`. Nenhum número novo.
- Toque em palavra: `getOffsetForPosition` conferido com a união dos `getBoundingBox` ampliada em 8 dp (`HitTest`, testado na JVM). Toque no vazio não faz nada: na etapa0 a pausa do T07 é o botão Pausar (D10).
- Ouvir pede `RECORD_AUDIO` na hora, só com a fonte microfone. Negada: erro "Sem permissão de microfone".
- Rotação: o `ViewModel` sobrevive; `onStop` com `isChangingConfigurations` não faz nada; `onStop` real faz `endSession` e `stopCapture` e deixa pausado; `onStart` depois disso reabre a captura e continua pausado (TRD §Segundo plano).
- Estratégia e fonte só mudam com a leitura parada; o controlador libera o motor e cria outro pela fábrica (D4, D8). O modelo não é recarregado (A4).

### A14. Modelo no repositório: quem baixa, como, LFS e cota

- `git lfs version` → `git-lfs/3.8.0`: instalado, sem portão 6. O `.gitattributes` já tem `etapa0/**/model-pt/** filter=lfs`.
- **Quem baixa: o `motor-voz`**, na Tarefa 3, com o script `etapa0/scripts/baixar-modelo.sh` (criado nesta tarefa). Ele baixa de `alphacephei.com` para um diretório temporário, confere o SHA-256 fixado acima (outro zip aborta, Q8), descompacta num diretório novo e copia para `etapa0/app/src/main/assets/model-pt/`, gravando `uuid` = `vosk-model-small-pt-0.3 sha256=<hex>`. Baixar o modelo já decidido no TRD/PRD não é instalação nova (brainstorm D26).
- Tamanho no LFS: 15 arquivos + `uuid`, 53,6 MB. Portão 6 só se o `git push` for recusado por cota de LFS (aí o `motor-voz` para e o orquestrador leva ao humano).
- AAB de release medido na prova: 49.589.150 bytes (abaixo da meta de 80 MB). APK de release: cerca de 74,6 MB.

## Prova do plano (feita pelo arquiteto em 2026-10-11)

Numa cópia descartável de `etapa0/` no scratchpad (`rsync` sem `build/`), com o código exato deste plano e o modelo real baixado pelo próprio `scripts/baixar-modelo.sh`. Nada da cópia é versionado. Resultados reais:

- `./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest` → BUILD SUCCESSFUL. Testes: 118 no `:app` (19 classes; 117 novos mais o `CoreFromAppTest` que já existia), 27 no `build-logic`, 118 no `:core` (inalterados); 0 falhas, 0 ignorados.
- Lint do `:app`: só `MissingApplicationIcon` (aviso que já existia). Sem `DataExtractionRules`, sem `WrongConstant`.
- `./gradlew assembleDebug assembleRelease bundleRelease` → BUILD SUCCESSFUL (com `-dontwarn java.awt.**`; sem ele o R8 para em "Missing class java.awt.Component", porque a JNA referencia AWT).
- `aapt2 dump permissions` do APK de release → só `android.permission.RECORD_AUDIO`. `verify{Debug,Release}MergedManifest: OK … -> android.permission.RECORD_AUDIO`.
- `mapping.txt`: `com.sun.jna.Native -> com.sun.jna.Native:` e `org.vosk.Recognizer -> org.vosk.Recognizer:`.
- `check`, `assembleRelease`, `bundleRelease` e `assembleDebug` com `--dry-run` listam a verificação de manifesto da variante.
- `scripts/verificar-manifesto-negativo.sh` → 4 reprovações esperadas e 1 aprovação, `exit=0`.
- `scripts/varrer-registerreceiver.sh` → 77 bibliotecas, 13.076 classes; só o `MediaQuery` do compose-ui (flag 2) e o `ContextKt` do core-ktx (repassa a flag de quem chama `receiveBroadcasts`; ninguém chama); `exit=0`. A primeira versão do script, sem a exceção do `ContextKt`, reprovou (prova de que o caminho de falha funciona).
- `verify16kAlignment` → reprova só em `libvosk.so` de armeabi-v7a e x86 (4096), no APK e no AAB (A8).
- ktlint do `:app` com violação proposital em `src/main/kotlin` e em `src/test/kotlin` → as duas tarefas executam e falham (A9).
- Greps de A6 e A7 e de `registerReceiver` (só `.kt`) → vazios, exceto a lista esperada de escritas (A11).
- Simulação tarefa a tarefa numa segunda cópia limpa: aplicando os arquivos na ordem das Tarefas 1 a 15, cada passo "ver falhar" falhou (compilação ou verificação) e cada fim de tarefa ficou verde (build, testes, ktlint e, onde pedido, lint e empacotamento); o resultado final é idêntico à primeira cópia. A simulação achou e corrigiu uma dependência fora de ordem (`AssetTree`, agora na Tarefa 5).
- Quebras propositais (Q2): 54 quebras de código, todas vermelhas (tabela em "Quebras propositais previstas para o QA"; saída em `evidencias/` pelo QA).

O que a prova não cobre (fica para a 0005, critério E do spec): JSON real do Vosk, `AudioRecord` no aparelho, carga do modelo, tela no aparelho e release com R8 rodando.

## Arquivos que mudam

Todos em `etapa0/`.

| Arquivo | Mudança | Requisito / seção do TRD | Agente |
|---|---|---|---|
| `.editorconfig` | Composables podem começar com maiúscula | TRD §Qualidade de código (ktlint); A9 | motor-voz |
| `gradle/libs.versions.toml` | Compose BOM, activity-compose, lifecycle, vosk-android, JNA, plugin Compose | TRD §Stack; spec A3; A1 | motor-voz |
| `build.gradle.kts` (raiz) | plugin Compose `apply false` | TRD §Stack | motor-voz |
| `app/build.gradle.kts` | Compose, BuildConfig, R8 + assinatura debug no release, ABIs, textos de teste em assets, propriedades dos testes, tarefa `releaseClasspathFiles` | TRD §Distribuição; D9, D19; A7; CIA mitigação 3 | motor-voz |
| `app/proguard-rules.pro` (novo) | `-keep` Vosk e JNA, `-dontwarn java.awt.**`, remove `Log` | TRD §Distribuição, §Onde o texto vive | motor-voz |
| `app/src/main/AndroidManifest.xml` | texto aprovado no portão 1 | CIA 0003 (a, b, c); OPR-001, OPR-002 | motor-voz |
| `app/src/main/res/xml/data_extraction_rules.xml`, `backup_rules.xml` (novos) | excluem os nove domínios | CIA 0003 (b); TRD §Manifesto | motor-voz |
| `docs/TRD.md` §Manifesto (fora de `etapa0/`) | texto aprovado no portão 1 (item 4) | CIA 0003 | motor-voz |
| `build-logic/.../ManifestPermissionCheck.kt` + teste | reprova `<permission>`, `<permission-group>`, `<permission-tree>` | CIA 0003, mitigação 1 | motor-voz |
| `build-logic/.../AndroidVerificationsPlugin.kt` | `check`/`assemble*`/`bundle*` dependem da verificação; 16 KB lê o AAB | OPR-004, OPR-005 | motor-voz |
| `build-logic/.../NativeLibScan.kt` + teste | raiz `lib/` ou `base/lib/`; relatório por biblioteca | OPR-005; B5 | motor-voz |
| `build-logic/.../Verify16kAlignmentTask.kt` | também o AAB; registra o alinhamento de cada `.so` | OPR-005; B5 | motor-voz |
| `scripts/baixar-modelo.sh` (novo) | baixa e confere o modelo, grava `uuid` | spec A4; D12; A14 | motor-voz |
| `scripts/varrer-registerreceiver.sh` (novo) | varredura de `registerReceiver` no classpath | CIA 0003, mitigação 3 | motor-voz |
| `app/src/main/assets/model-pt/**` (novo, LFS) | `vosk-model-small-pt-0.3` + `uuid` | TRD §Instalação 2; A4 | motor-voz |
| `app/src/main/kotlin/.../speech/AudioSource.kt`, `AssetTree.kt`, `Decoder.kt`, `RecognizerStrategy.kt`, `CaptureDriver.kt` | costuras | D1, D4; A3 | motor-voz |
| `.../speech/AudioLevel.kt`, `VoskJson.kt` | `Level.rms`, JSON ↔ palavras | D2; C6, C7 | motor-voz |
| `.../speech/WavAudioSource.kt` | fonte WAV | D8; C11 | motor-voz |
| `.../speech/ModelInstaller.kt` | cópia por `uuid` | TRD §Instalação 1–3; C10; A6 | motor-voz |
| `.../speech/FstSymbols.kt`, `Vocabulary.kt` | léxico do modelo e lista do vocabulário | TRD §Vocabulário restrito; C5; A5 | motor-voz |
| `.../speech/VoskEngine.kt` | o motor | TRD §Contratos (SpeechEngine e regras), §Captura e sessões; C1–C9 | motor-voz |
| `.../speech/MicAudioSource.kt`, `VoskDecoders.kt` | Android e `org.vosk.*` | TRD §Captura e sessões, §Instalação 4; D15 | motor-voz |
| `.../export/TestTextCatalog.kt`, `Recording.kt`, `ExportFiles.kt`, `ExportWriter.kt` | exportação | TRD Etapa 0 item 5, §Privacidade nos testes; D6–D11 | motor-voz |
| `.../viewmodel/ReaderUiState.kt`, `ReaderController.kt` | lógica da tela e ligação com o `align` | TRD Reconciliação 7, Máquina de estados (Reading/Paused), §Segundo plano; D1–D9, D17 | interface |
| `.../viewmodel/ReaderViewModel.kt` | casca Android e dependências reais | TRD §Onde o texto vive; D16 | interface |
| `.../ui/HitTest.kt`, `PromptText.kt`, `ReaderScreen.kt`, `DebugPanel.kt` | tela mínima e painel | TRD Etapa 0 itens 1 e 4, §Hit-test, §Rolagem; D10, D13, D18 | interface |
| `.../MainActivity.kt` (pacote raiz) | Activity única | CIA 0003 (c); TRD §Segundo plano; regra 8 | motor-voz (esqueleto, Tarefa 2) e interface (final, Tarefa 15) |
| `app/src/main/res/values/strings.xml` | textos da tela em pt-BR | CLAUDE.md, strings | interface |
| `app/src/test/kotlin/...` (novos) | testes de todas as seções C e D | spec C1–C11, D1–D9 | cada um com o seu código |

`core/src/main/**` não muda (A5).

## Ordem e paralelismo

Ordem do PROCESS.md (contratos e `align` → `speech` → `viewmodel` → `ui`). Os contratos e o `align` já existem e não mudam; a ordem aqui é:

1. **motor-voz, Tarefas 1 a 3** (sozinho): `build-logic`, dependências + manifesto + TRD §Manifesto (um commit só, para o build nunca ficar verde com a regra sem verificação), modelo no LFS.
2. **motor-voz, Tarefas 4 a 7**: costuras, `AudioLevel`/`VoskJson`, `Vocabulary`, exportação. Ao fim da Tarefa 7 (commit), o `interface` pode começar: o controlador depende só do contrato (`SpeechEngine` e o `FakeSpeechEngine`) e destes tipos (`ModelInfo`, `ModelLoadException`, `RecognizerStrategy`, `Vocabulary`, `Lexicon`, `export/*`).
3. **Podem correr em paralelo:**
   - motor-voz, Tarefas 8 a 11: `VoskEngine`, `ModelInstaller`, `WavAudioSource`, adaptadores Android.
   - interface, Tarefas 12 a 14: `ReaderController` (com `FakeSpeechEngine`), `HitTest` e as telas.

   Os dois não podem dividir a mesma árvore de trabalho: o passo "ver falhar" de um quebra o build do outro. Para correr em paralelo, o `interface` trabalha numa worktree própria, num branch a partir do commit da Tarefa 7:

   ```bash
   git worktree add -b work/0003-etapa0-vosk-ui ../Tribuna-0003-ui work/0003-etapa0-vosk
   cd ../Tribuna-0003-ui && git lfs pull   # os objetos do LFS são os mesmos do repositório
   ```

   Depois da Tarefa 11 do `motor-voz`, o `interface` traz o trabalho dele para o branch principal da tarefa (`git checkout work/0003-etapa0-vosk && git merge --no-ff work/0003-etapa0-vosk-ui`; os arquivos são disjuntos, sem conflito esperado) e faz a Tarefa 15 ali. Sem worktree, a ordem é sequencial: Tarefas 1 a 11 (motor-voz), 12 a 15 (interface), 16 (motor-voz). Em qualquer caso, cada agente faz `git add` só dos próprios arquivos, com caminho explícito.
4. **interface, Tarefa 15** (depois da Tarefa 11 do motor-voz): `ReaderViewModel` e `MainActivity` final. Precisa de `VoskEngine`, `MicAudioSource`, `WavAudioSource`, `VoskModels`, `AndroidAssetTree`, `ExportWriter`.
5. **motor-voz, Tarefa 16**: verificação completa e evidências em `work/0003-etapa0-vosk/evidencias/`.

Modelos (PROCESS.md, Autonomia): Sonnet para os dois; nada de Haiku (o motor mexe no ciclo de vida de sessão).

## Testes primeiro

Cada tarefa traz o teste antes do código, com o comando que deve falhar (compilação: "Unresolved reference") e depois passar. Contagem final medida na prova: 118 testes no `:app`, 27 no `build-logic`.

### Critérios do spec → teste → comando

Os comandos rodam em `etapa0/`. "Teste" = `./gradlew testDebugUnitTest` (que já roda `build-logic`, `:core` e `:app`).

| Critério | Teste(s) ou comando | Tarefa |
|---|---|---|
| A1 | `./gradlew assembleDebug assembleRelease bundleRelease`; `unzip -l` do APK e do AAB com `assets/model-pt/uuid`, `lib/<abi>/libvosk.so`, `libjnidispatch.so`; `wc -c` do AAB | 16 |
| A2 | `./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest`; `grep -rnE "@Ignore\|@Disabled" etapa0 --include=*.kt` vazio; contagem por classe (Q3) | 16 |
| A3 | `./gradlew :app:dependencies --configuration releaseRuntimeClasspath`; A1 deste plano | 2, 16 |
| A4 | `git lfs ls-files` lista os 16 arquivos; `LexiconRealModelTest.uuidDoModelo_temNomeESha256DoZip`; saída do `baixar-modelo.sh` | 3, 4 |
| A5 | `git diff main --stat -- etapa0/core/src/main` vazio | 16 |
| A6 | `grep -rn "SpeechService" etapa0/app/src`; `grep -rn "res/layout" etapa0/app/src`; `find etapa0/app/src -path "*res/layout*"`: todos vazios | 16 |
| A7 | greps de A7 do spec: o primeiro vazio; o segundo lista só `ModelInstaller.kt` e `ExportWriter.kt` (A11); o terceiro vazio | 16 |
| A8 | `./gradlew :app:ktlintCheck --rerun-tasks --console=plain` com `runKtlintCheckOverMainSourceSet` e `runKtlintCheckOverTestSourceSet` executados; prova negativa com violação proposital | 16 |
| B1 | `verifyMergedManifest`; manifestos mesclados de debug e release em `build/intermediates/merged_manifest/`; `aapt2 dump permissions` | 2, 16 |
| B2 | `scripts/verificar-manifesto-negativo.sh` → `exit=0` | 2, 16 |
| B3 | manifesto mesclado com `allowBackup="false"` e `dataExtractionRules`; lint sem `DataExtractionRules` | 2, 16 |
| B4 | `./gradlew check --dry-run`, `assembleRelease --dry-run`, `bundleRelease --dry-run` listam `verify…MergedManifest` | 1, 16 |
| B5 | `NativeLibScanTest` (AAB e relatório); `./gradlew verify16kAlignment` com a saída por biblioteca e ABI; OPR-024 | 1, 16 |
| B6 | `grep -E "^(org.vosk.Recognizer\|com.sun.jna.Native) ->" app/build/outputs/mapping/release/mapping.txt` | 2, 16 |
| C1 | `VoskEngineSessionTest.todoEventoDoReconhecedor_levaOIdDele`, `resultadoProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo`, `finalProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo`, `reconhecedorAntigoSoELiberadoDepoisDeTerminarOBloco` | 8 |
| C2 | `VoskEngineSessionTest.foraDeSessao_nenhumBlocoChegaAoReconhecedor_eLevelContinua` | 8 |
| C3 | `VoskEngineSessionTest.blocoLidoAntesDoEndSessionENaoEntregue_naoChegaAoReconhecedor`, `endSession_naoEmiteFinal_eNaoPedeOResultado`, `estrategiaReset_endSessionZeraEMantemOReconhecedor`, `stopCaptureDuranteALeitura_descartaOBloco` | 8 |
| C4 | `VoskEngineSessionTest.estrategiaNova_…`, `estrategiaReset_reaproveitaOReconhecedor_comIdNovo`, `estrategiaReset_vocabularioMudou_criaReconhecedorNovo`, `estrategiaReset_sessaoAntigaAtrasada_…`, `releaseDuranteUmBloco_…` | 8 |
| C5 | `VoskEngineSessionTest.vocabulario_vaiParaOReconhecedorCriado_esemListaSemVocabulario`; `VocabularyTest` (4); `LexiconRealModelTest` (OPR-020); `ReaderLifecycleTest.vocabularioLigado_…` | 4, 8, 12 |
| C6 | `VoskJsonTest` (8) | 6 |
| C7 | `AudioLevelTest` (5) | 6 |
| C8 | `VoskEngineStateTest.sequenciaDeEstados`, `falhaAoAbrirAFonte_…`, `semPermissao_…`, `permissaoPerdidaDuranteACaptura_…`, `excecaoDoReconhecedor_…`, `modeloAusente_…`, `stopCapture_…`; `VoskEngineSessionTest.reconhecedorQueNaoCria_…` | 8 |
| C9 | `VoskEngineStateTest.duasChamadasDePrepare_carregamOModeloUmaVez`, `prepareComOMotorAberto_naoRecarregaNemMudaOEstado`, `falhaDeCargaNaoFicaEmCache_…` | 8 |
| C10 | `ModelInstallerTest` (7); `ReaderLifecycleTest.falhaNoPrepare_mostraOErro_eTentarDeNovoPreparaOutraVez` | 9, 12 |
| C11 | `WavAudioSourceTest` (8) | 10 |
| D1 | `ReaderSessionTest.parcialEFinalAtrasadosDaSessaoAnterior_…`, `eventosAtrasadosEntramNaExportacao…` | 12 |
| D2 | `ReaderSessionTest.falaNaPausa_…` | 12 |
| D3 | `ReaderReadingTest.t01PorParciaisEFinais_…`, `recuoCurto_…` | 12 |
| D4 | `ReaderTapTest` (4) | 12 |
| D5 | `ReaderReadingTest.limiarZero_…`, `limiarAcimaDoUltimoNivel_…`, `limiarNaoFiltraFinal`, `nivelAcimaDoLimiar_…` | 12 |
| D6 | `TestTextCatalogTest` (5); `ReaderExportTest.textoAlterado_…`, `textoDeTeste_…`, `nomeComTextoLivre_…`; `ExportNameAndWriterTest` | 7, 12 |
| D7 | `ExportFilesTest` (6); `ReaderExportTest.hipotesesLidasDeVolta_…` | 7, 12 |
| D8 | `ExportFilesTest.meta_…`; `ReaderExportTest.meta_trazTodasAsChaves_eOIdDoModeloIgualAoUuid` | 7, 12 |
| D9 | `ReaderLifecycleTest.onStopComMudancaDeConfiguracao_…`, `onStopDeVerdade_…`, `voltaDoSegundoPlano_…`; A7 | 12, 16 |
| E (aparelho) | não executado nesta tarefa; 0005 | — |

Testes além dos critérios, cada um com consumidor: `FstSymbolsTest` (léxico, A5), `HitTestTest` (toque em palavra, D10), `ReaderLifecycleTest` de erros e de troca de estratégia (D4, D15), `ReaderReadingTest.inicioDaEscuta_…` (painel D18), `textoNaoMudaDuranteALeitura`, `microfoneNaoAbre_…`, `ManifestPermissionCheckTest` (3 novos, CIA) e `NativeLibScanTest` (4 novos, OPR-005).

### Quebras propositais previstas para o QA (Q2)

Cada linha: arquivo, troca de texto exata, teste que fica vermelho. Todas foram feitas na prova e ficaram vermelhas. O QA refaz e anexa a saída em `work/0003-etapa0-vosk/evidencias/quebras-q2.txt`, restaurando o arquivo depois de cada uma (`git checkout -- <arquivo>`).

`⏎` marca quebra de linha; a indentação é a do arquivo. A troca é por texto exato (uma ocorrência).

- **Q2-E1** (C1 Partial com a sessão atual do motor), `KT/speech/VoskEngine.kt`: trocar `SpeechEvent.Partial(id, VoskJson.words(active.partialJson()))` por `SpeechEvent.Partial(synchronized(lock) { current }?.id ?: id, VoskJson.words(active.partialJson()))`. Vermelho: `VoskEngineSessionTest.resultadoProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo`.
- **Q2-E2** (C1 Final com a sessão atual do motor), `KT/speech/VoskEngine.kt`: trocar `SpeechEvent.Final(id, VoskJson.words(active.resultJson()))` por `SpeechEvent.Final(synchronized(lock) { current }?.id ?: id, VoskJson.words(active.resultJson()))`. Vermelho: `VoskEngineSessionTest.finalProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo`.
- **Q2-E3** (feed ignora ended), `KT/speech/VoskEngine.kt`: trocar `if (ended) return // bloco lido` por `if (false) return // bloco lido`. Vermelho: `VoskEngineSessionTest.reconhecedorComExcecao_naoRecebeMaisAudio`.
- **Q2-E4** (sem descarte do bloco lido durante o stop), `KT/speech/VoskEngine.kt`: trocar `if (!active) return close() // parou durante a leitura` por `if (false) return close() // parou durante a leitura`. Vermelho: `VoskEngineSessionTest.stopCaptureDuranteALeitura_descartaOBloco`.
- **Q2-E5** (C3 endSession não libera), `KT/speech/VoskEngine.kt`: trocar `if (releaseNow) decoder?.let(::release)` por `if (false) decoder?.let(::release)`. Vermelho: `VoskEngineSessionTest.blocoLidoAntesDoEndSessionENaoEntregue_naoChegaAoReconhecedor`, `VoskEngineSessionTest.endSession_naoEmiteFinal_eNaoPedeOResultado`, `VoskEngineSessionTest.estrategiaNova_cadaSessaoCriaUmReconhecedor_comIdsCrescentes` e mais 3.
- **Q2-E6** (C2 Level só com sessão), `KT/speech/VoskEngine.kt`: trocar `channel.trySend(SpeechEvent.Level(AudioLevel.rms(buffer, count))) ⏎ val session = synchronized(lock) { current }` por `val session = synchronized(lock) { current } ⏎ if (session != null) channel.trySend(SpeechEvent.Level(AudioLevel.rms(buffer, count)))`. Vermelho: `VoskEngineSessionTest.foraDeSessao_nenhumBlocoChegaAoReconhecedor_eLevelContinua`.
- **Q2-E7** (C4 reset ignora vocabulário), `KT/speech/VoskEngine.kt`: trocar `if (kept != null && kept.vocabulary == vocabulary) kept.decoder else null` por `if (kept != null) kept.decoder else null`. Vermelho: `VoskEngineSessionTest.estrategiaReset_vocabularioMudou_criaReconhecedorNovo`.
- **Q2-E8** (C4 estratégia invertida), `KT/speech/VoskEngine.kt`: trocar `if (strategy == RecognizerStrategy.NewPerSession) {` por `if (strategy != RecognizerStrategy.NewPerSession) {`. Vermelho: `VoskEngineSessionTest.blocoLidoAntesDoEndSessionENaoEntregue_naoChegaAoReconhecedor`, `VoskEngineSessionTest.endSession_naoEmiteFinal_eNaoPedeOResultado`, `VoskEngineSessionTest.estrategiaNova_cadaSessaoCriaUmReconhecedor_comIdsCrescentes` e mais 3.
- **Q2-E9** (spare == null), `KT/speech/VoskEngine.kt`: trocar `if (spare == null && !released) {` por `if (!released) {`. Vermelho: `VoskEngineSessionTest.estrategiaReset_sessaoAntigaAtrasada_guardaSoOReconhecedorMaisNovo`.
- **Q2-E10** (!released), `KT/speech/VoskEngine.kt`: trocar `if (spare == null && !released) {` por `if (spare == null) {`. Vermelho: `VoskEngineSessionTest.releaseDuranteUmBloco_fechaOReconhecedorQuandoOBlocoTermina`.
- **Q2-E11** (C9 prepare recarrega), `KT/speech/VoskEngine.kt`: trocar `if (factory != null) return Result.success(Unit)` por `if (false) return Result.success(Unit)`. Vermelho: `VoskEngineStateTest.prepareComOMotorAberto_naoRecarregaNemMudaOEstado`.
- **Q2-E12** (C8 erro de abertura engolido), `KT/speech/VoskEngine.kt`: trocar `channel.trySend(SpeechEvent.Error(null, e.error)) ⏎ return` por `return`. Vermelho: `VoskEngineStateTest.falhaAoAbrirAFonte_geraMicUnavailableSemSessao`, `VoskEngineStateTest.semPermissao_geraPermissionRevokedSemSessao`.
- **Q2-E13** (C5 vocabulário não repassado), `KT/speech/VoskEngine.kt`: trocar `loaded.create(vocabulary)` por `loaded.create(null)`. Vermelho: `VoskEngineSessionTest.vocabulario_vaiParaOReconhecedorCriado_esemListaSemVocabulario`.
- **Q2-V1** (D1 sem filtro de sessão), `KT/viewmodel/ReaderController.kt`: trocar `val fromActive = session == active` por `val fromActive = true`. Vermelho: `ReaderExportTest.hipotesesLidasDeVolta_entregamAsMesmasPalavrasDaSessaoAoVivo`, `ReaderSessionTest.eventosAtrasadosEntramNaExportacaoComOIdOriginalEAccepted0`, `ReaderSessionTest.falaNaPausa_naoMoveOCursor_eAoRetomarASessaoENovaComCaudaVazia` e mais 1.
- **Q2-V2** (D5 limiar filtra Final), `KT/viewmodel/ReaderController.kt`: trocar `val passesLevel = isFinal || lastLevel >= threshold` por `val passesLevel = lastLevel >= threshold`. Vermelho: `ReaderReadingTest.limiarNaoFiltraFinal`.
- **Q2-V3** (D5 >= vira >), `KT/viewmodel/ReaderController.kt`: trocar `val passesLevel = isFinal || lastLevel >= threshold` por `val passesLevel = isFinal || lastLevel > threshold`. Vermelho: `ReaderExportTest.hipotesesLidasDeVolta_entregamAsMesmasPalavrasDaSessaoAoVivo`, `ReaderReadingTest.inicioDaEscuta_doBeginSessionAoPrimeiroParcialNaoVazio`, `ReaderReadingTest.limiarZero_todoParcialEProcessado` e mais 3.
- **Q2-V4** (D5 sem limiar), `KT/viewmodel/ReaderController.kt`: trocar `val passesLevel = isFinal || lastLevel >= threshold` por `val passesLevel = true`. Vermelho: `ReaderReadingTest.limiarAcimaDoUltimoNivel_parcialIgnorado_cursorParado`.
- **Q2-V5** (&& vira ||), `KT/viewmodel/ReaderController.kt`: trocar `val accepted = fromActive && passesLevel` por `val accepted = fromActive || passesLevel`. Vermelho: `ReaderExportTest.hipotesesLidasDeVolta_entregamAsMesmasPalavrasDaSessaoAoVivo`, `ReaderReadingTest.limiarAcimaDoUltimoNivel_parcialIgnorado_cursorParado`, `ReaderSessionTest.eventosAtrasadosEntramNaExportacaoComOIdOriginalEAccepted0` e mais 2.
- **Q2-V6** (erro de sessão ativa ignorado), `KT/viewmodel/ReaderController.kt`: trocar `val accepted = fromCapture || event.session == active` por `val accepted = fromCapture`. Vermelho: `ReaderLifecycleTest.erroDaSessaoAtiva_pausaEMostraOMotivo_erroDeSessaoAntigaEIgnorado`.
- **Q2-V7** (erro de captura ignorado), `KT/viewmodel/ReaderController.kt`: trocar `val accepted = fromCapture || event.session == active` por `val accepted = event.session != null && event.session == active`. Vermelho: `ReaderLifecycleTest.erroDaCaptura_pausaEFechaACaptura_eOuvirReabre`, `ReaderReadingTest.microfoneNaoAbre_aLeituraNaoComeca_eMostraOErro`.
- **Q2-V8** (erro de sessão antiga aceito), `KT/viewmodel/ReaderController.kt`: trocar `val accepted = fromCapture || event.session == active` por `val accepted = fromCapture || event.session != null`. Vermelho: `ReaderLifecycleTest.erroDaSessaoAtiva_pausaEMostraOMotivo_erroDeSessaoAntigaEIgnorado`.
- **Q2-V9** (D2 reconciliador não zera na sessão), `KT/viewmodel/ReaderController.kt`: trocar ` reconciler.reset() ⏎ tail.clear()` por ` tail.clear()`. Vermelho: `ReaderSessionTest.falaNaPausa_naoMoveOCursor_eAoRetomarASessaoENovaComCaudaVazia`.
- **Q2-V10** (D2 cauda não zera na sessão), `KT/viewmodel/ReaderController.kt`: trocar ` reconciler.reset() ⏎ tail.clear()` por ` reconciler.reset()`. Vermelho: `ReaderSessionTest.falaNaPausa_naoMoveOCursor_eAoRetomarASessaoENovaComCaudaVazia`.
- **Q2-V11** (cauda só k), `KT/viewmodel/ReaderController.kt`: trocar `val keep = maxOf(mutableState.value.settings.preset.params.k, AlignConstants.RECOVERY_WORDS)` por `val keep = mutableState.value.settings.preset.params.k`. Vermelho: `ReaderExportTest.hipotesesLidasDeVolta_entregamAsMesmasPalavrasDaSessaoAoVivo`.
- **Q2-V12** (D3 trajetória só em reposition), `KT/viewmodel/ReaderController.kt`: trocar `if (next.lastMove == Move.Reposition || next.cursor != previous?.cursor) {` por `if (next.lastMove == Move.Reposition) {`. Vermelho: `ReaderReadingTest.recuoCurto_trajetoriaGravaOCursor_eOHighWaterFicaNoMaior`, `ReaderReadingTest.t01PorParciaisEFinais_cursorChegaAoUltimoToken_eTrajetoriaTemUmaLinhaPorMudanca`.
- **Q2-V13** (reposition no mesmo cursor sem linha), `KT/viewmodel/ReaderController.kt`: trocar `if (next.lastMove == Move.Reposition || next.cursor != previous?.cursor) {` por `if (next.cursor != previous?.cursor) {`. Vermelho: `ReaderTapTest.toqueNaPalavraAtual_tambemGravaOReposition`.
- **Q2-V14** (trajetória grava highWater no lugar do cursor), `KT/viewmodel/ReaderController.kt`: trocar `recording?.trajectory(clock(), next.cursor, next.lastMove, next.score)` por `recording?.trajectory(clock(), next.highWater, next.lastMove, next.score)`. Vermelho: `ReaderReadingTest.recuoCurto_trajetoriaGravaOCursor_eOHighWaterFicaNoMaior`.
- **Q2-V15** (estado publica highWater como cursor), `KT/viewmodel/ReaderController.kt`: trocar ` cursor = next.cursor,` por ` cursor = next.highWater,`. Vermelho: `ReaderReadingTest.recuoCurto_trajetoriaGravaOCursor_eOHighWaterFicaNoMaior`.
- **Q2-V16** (painel mostra cursor como highWater), `KT/viewmodel/ReaderController.kt`: trocar ` highWater = next.highWater,` por ` highWater = next.cursor,`. Vermelho: `ReaderReadingTest.recuoCurto_trajetoriaGravaOCursor_eOHighWaterFicaNoMaior`.
- **Q2-V17** (D4 reposition antes do endSession), `KT/viewmodel/ReaderController.kt`: trocar ` endSession() ⏎ reposition(index)` por ` reposition(index) ⏎ endSession()`. Vermelho: `ReaderTapTest.toqueNaPalavraDuranteALeitura_endSessionRepositionBeginSession_nessaOrdem`.
- **Q2-V18** (D9 rotação fecha captura), `KT/viewmodel/ReaderController.kt`: trocar `if (changingConfigurations) return@enqueue` por `if (false) return@enqueue`. Vermelho: `ReaderLifecycleTest.onStopComMudancaDeConfiguracao_naoFechaSessaoNemCaptura`.
- **Q2-V19** (D9 onStart sem flag), `KT/viewmodel/ReaderController.kt`: trocar `if (!backgrounded) return@enqueue` por `if (false) return@enqueue`. Vermelho: `ReaderLifecycleTest.rotacaoComALeituraPausada_naoReabreACaptura`.
- **Q2-V20** (D6 exporta texto qualquer), `KT/viewmodel/ReaderController.kt`: trocar `recording = current.testTextId?.let { Recording(it, metaFor(current, info, prepareMs), originMs = clock()) }` por `recording = Recording(current.testTextId ?: "T01", metaFor(current, info, prepareMs), originMs = clock())`. Vermelho: `ReaderExportTest.textoAlterado_desabilitaAExportacao`.
- **Q2-V21** (nome não validado), `KT/viewmodel/ReaderController.kt`: trocar `if (!ExportName.isValid(current.exportName, rec.textId)) {` por `if (false) {`. Vermelho: `ReaderExportTest.nomeComTextoLivre_eRecusado`.
- **Q2-V22** (erro de captura não fecha a captura), `KT/viewmodel/ReaderController.kt`: trocar `if (fromCapture) stopCapture()` por `if (false) stopCapture()`. Vermelho: `ReaderLifecycleTest.erroDaCaptura_pausaEFechaACaptura_eOuvirReabre`.
- **Q2-V23** (retomar reabre captura sempre), `KT/viewmodel/ReaderController.kt`: trocar `if (!mutableState.value.capturing && !startCapture()) {` por `if (!startCapture()) {`. Vermelho: `ReaderExportTest.hipotesesLidasDeVolta_entregamAsMesmasPalavrasDaSessaoAoVivo`, `ReaderLifecycleTest.erroDaSessaoAtiva_pausaEMostraOMotivo_erroDeSessaoAntigaEIgnorado`, `ReaderLifecycleTest.voltaDoSegundoPlano_reabreACaptura_eContinuaPausada` e mais 3.
- **Q2-V24** (captura sempre ok), `KT/viewmodel/ReaderController.kt`: trocar `val ok = engine.state.value == EngineState.Capturing` por `val ok = true`. Vermelho: `ReaderReadingTest.microfoneNaoAbre_aLeituraNaoComeca_eMostraOErro`.
- **Q2-V25** (texto muda na leitura), `KT/viewmodel/ReaderController.kt`: trocar ` enqueue { ⏎ if (mutableState.value.phase != ReaderPhase.Idle) return@enqueue ⏎ val id = catalog.identify(text)` por ` enqueue { ⏎ val id = catalog.identify(text)`. Vermelho: `ReaderReadingTest.textoNaoMudaDuranteALeitura`.
- **Q2-X1** (D6 identifica com trim), `KT/export/TestTextCatalog.kt`: trocar `it.value == text` por `it.value.trim() == text.trim()`. Vermelho: `TestTextCatalogTest.semAQuebraDeLinhaFinal_naoIdentifica`.
- **Q2-X2** (nome livre), `KT/export/ExportWriter.kt`: trocar `"(_[a-z0-9]+)*$"` por `".*$"`. Vermelho: `ExportNameAndWriterTest.nomeInvalido_outroTexto_textoLivre_ouCaminho`, `ReaderExportTest.nomeComTextoLivre_eRecusado`.
- **Q2-X3** (D7 replay sem reset por sessão), `KT/export/ExportFiles.kt`: trocar ` reconciler.reset() ⏎ session = row.session` por ` session = row.session`. Vermelho: `ExportFilesTest.reprocessar_soAsAceitas_zerandoAFraseACadaSessao`.
- **Q2-X4** (D7 replay inclui recusadas), `KT/export/ExportFiles.kt`: trocar `if (!row.accepted || row.kind == Recording.KIND_ERROR) continue` por `if (row.kind == Recording.KIND_ERROR) continue`. Vermelho: `ExportFilesTest.reprocessar_soAsAceitas_zerandoAFraseACadaSessao`, `ReaderExportTest.hipotesesLidasDeVolta_entregamAsMesmasPalavrasDaSessaoAoVivo`.
- **Q2-S1** (C10 needsCopy invertido), `KT/speech/ModelInstaller.kt`: trocar `targetUuid == null || targetUuid.trim() != sourceUuid.trim()` por `targetUuid == null || targetUuid.trim() == sourceUuid.trim()`. Vermelho: `ModelInstallerTest.falhaNaCopia_devolveModelCorrupt_eNaoDeixaCopiaPelaMetade`, `ModelInstallerTest.mesmaVersao_naoCopia_diferenteOuAusente_copia`, `ModelInstallerTest.uuidDiferenteNoDestino_copiaDeNovo` e mais 1.
- **Q2-S2** (C10 cópia direta no destino), `KT/speech/ModelInstaller.kt`: trocar `val temp = File(filesDir, "$assetDir.tmp")` por `val temp = target`. Vermelho: `ModelInstallerTest.falhaNaCopia_devolveModelCorrupt_eNaoDeixaCopiaPelaMetade`, `ModelInstallerTest.semModeloNoDestino_copiaTudo_comUuidPorUltimo`, `ModelInstallerTest.uuidAusenteNoDestino_copia` e mais 2.
- **Q2-S3** (C5 vocabulário com repetidas), `KT/speech/Vocabulary.kt`: trocar `lexicon.spellings(key).filterTo(extra) { it !in spoken }` por `lexicon.spellings(key).filterTo(extra) { true }`. Vermelho: `VocabularyTest.comLexico_comecaPelaFormaDoTrd_eAcrescentaAGrafiaDoLexico`.
- **Q2-S4** (símbolos internos entram), `KT/speech/Vocabulary.kt`: trocar `it[0] !in INTERNAL_PREFIXES` por `true`. Vermelho: `VocabularyTest.simbolosInternosDoModelo_naoEntram`.
- **Q2-S5** (C7 escala 32767), `KT/speech/AudioLevel.kt`: trocar `/ 32768.0` por `/ 32767.0`. Vermelho: `AudioLevelTest.meioDaEscala_daMeio`, `AudioLevelTest.soContaAsAmostrasLidas`.
- **Q2-S6** (C11 aceita não PCM), `KT/speech/WavAudioSource.kt`: trocar `format.audioFormat == PCM && ` por (nada). Vermelho: `WavAudioSourceTest.wavNaoPcm_eRecusado`.
- **Q2-S7** (C11 aceita estéreo), `KT/speech/WavAudioSource.kt`: trocar `format.channels == 1 && ` por (nada). Vermelho: `WavAudioSourceTest.wavEstereo_eRecusado`.
- **Q2-S8** (C11 aceita 44,1 kHz), `KT/speech/WavAudioSource.kt`: trocar `format.sampleRate == 16_000 && ` por (nada). Vermelho: `WavAudioSourceTest.wav44kHz_eRecusado`.
- **Q2-S9** (C11 aceita 8 bits), `KT/speech/WavAudioSource.kt`: trocar ` && format.bitsPerSample == 16` por (nada). Vermelho: `WavAudioSourceTest.wav8Bits_eRecusado`.
- **Q2-S10** (C6 só lê partial), `KT/speech/VoskJson.kt`: trocar `stringAfter(json, partialKey) ?: stringAfter(json, textKey) ?: return emptyList()` por `stringAfter(json, partialKey) ?: return emptyList()`. Vermelho: `VoskEngineSessionTest.finalProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo`, `VoskEngineSessionTest.todoEventoDoReconhecedor_levaOIdDele`, `VoskJsonTest.escapesUnicode_saoDecodificados` e mais 2.
- **Q2-B1** (manifesto sem declarações), `BL/ManifestPermissionCheck.kt`: trocar `return extra + missing + declarations` por `return extra + missing`. Vermelho: `ManifestPermissionCheckTest.declaracaoDePermissaoPropria_falha`, `ManifestPermissionCheckTest.grupoEArvoreDePermissao_falham`, `ManifestPermissionCheckTest.soADeclaracao_semUsesPermission_tambemFalha`.
- **Q2-B2** (AAB lido como APK), `BL/NativeLibScan.kt`: trocar `val entryPattern = entryPattern(libRoot)` por `val entryPattern = entryPattern(APK_LIB_ROOT)`. Vermelho: `NativeLibScanTest.aabComAsDuasLibsAlinhadasEmBaseLib_semProblemas`, `NativeLibScanTest.aabComLibDesalinhadaEAbiIncompleta_eReportado`, `NativeLibScanTest.aabSemBaseLib_falhaExplicitamente`.

Quebras por comando (fora do JUnit), também para o QA:

| ID | Quebra | Esperado |
|---|---|---|
| Q2-C1 | apagar as 3 linhas de `project.tasks.configureEach { if (name in packaging) dependsOn(perVariant) }` no `AndroidVerificationsPlugin.kt` | `assembleRelease --dry-run` e `bundleRelease --dry-run` deixam de listar `verifyReleaseMergedManifest` |
| Q2-C2 | apagar `tools:node="remove"` do `<permission>` no manifesto | `verifyReleaseMergedManifest` falha com "declaração de permissão não permitida: …DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" |
| Q2-C3 | escrever `class X {   val a = 1 }` num `.kt` novo em `app/src/main/kotlin/.../ui/` e outro em `app/src/test/kotlin/.../ui/` | `./gradlew :app:ktlintCheck --rerun-tasks` falha nas duas tarefas (A8) |
| Q2-C4 | no `varrer-registerreceiver.sh`, apagar a linha `if (cls ~ /^androidx\/core\/content\/ContextKt/) flag = "repassada_por_receiveBroadcasts"` | `exit=1` com `flag=nao_constante` no `ContextKt` |
| Q2-C5 | trocar o SHA-256 esperado no `baixar-modelo.sh` por 64 zeros e rodar numa cópia sem `model-pt/` | `ERRO: SHA-256 do zip é 6e1c…; esperado 000…`, `exit=1` |
| Q2-C6 | remover `bundleFile.set(...)` e a varredura do AAB no `Verify16kAlignmentTask` | a saída do `verify16kAlignment` deixa de listar `app-release.aab` (B5 exige as linhas do AAB) |

Trocas de variável parecida incluídas (cursor/highWater e afins): Q2-V14, Q2-V15, Q2-V16 (cursor ↔ highWater na trajetória, no estado e no painel), Q2-E1/Q2-E2 (id do reconhecedor ↔ sessão atual do motor), Q2-V17 (ordem endSession ↔ reposition), Q2-B2 (raiz do AAB ↔ do APK). Operandos de cada guarda nova: Q2-V2/V3/V4/V5 (`isFinal || lastLevel >= threshold` e `fromActive && passesLevel`), Q2-V6/V7/V8 (`fromCapture || event.session == active`), Q2-V12/V13 (`lastMove == Reposition || cursor != previous?.cursor`), Q2-E7 (`kept != null && kept.vocabulary == vocabulary`), Q2-E9/E10 (`spare == null && !released`), Q2-S6 a S9 (cada operando do formato WAV), Q2-V23 (`!capturing && !startCapture()`).

Guardas defensivas sem teste, de propósito: `captureLost` ignora captura que já não é a atual (`if (capture !== lost) return`), para uma falha de leitura que chegue junto com um `stopCapture` (corrida entre threads, não reproduzível com o laço manual); o `uuid` copiado por último dentro de `model-pt.tmp` (a troca de nome no fim já protege, Q2-S2). O revisor confere que continuam simples.

## Privacidade (primeira camada)

- **Caminho do texto.** Digitado no `OutlinedTextField` (valor vindo do estado, sem `rememberSaveable`) ou carregado de `assets/textos/` → `ReaderController.setText` → `ReaderUiState.text`/`tokens` em memória → `Tokenizer` → `Aligner`. O `ViewModel` não usa `SavedStateHandle`; a Activity não grava estado. Nenhum caminho novo persiste o texto. A lista do vocabulário (derivada do texto) vai só ao `Recognizer` nativo, e o log do Kaldi fica em "só erros" para ela não aparecer no logcat (A12). O nome de arquivo da exportação não aceita texto livre (A11).
- **Caminho do áudio.** `AudioRecord` (ou WAV de teste, só leitura) → `ShortArray` reaproveitado → `AudioLevel.rms` e `Decoder.accept` → descartado. Nenhuma escrita de áudio: as duas únicas escritas em arquivo do app são a cópia do modelo (`ModelInstaller`) e a exportação (`ExportWriter`), conferidas pelo grep de A7. Sem `MediaRecorder`.
- **Transcrições.** Hipóteses, cauda e trajetória ficam em memória (painel). Só vão para arquivo quando o texto atual é idêntico a um texto de teste e a pessoa aperta Exportar (D9, TRD §Privacidade nos testes). Arquivos no diretório externo do app, excluído do backup (domínio `external` nas regras).
- **Dependências novas e permissões.** Compose/activity/lifecycle trazem `androidx.core` 1.18.0, que injeta `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (removida com `tools:node="remove"`, portão 1 aprovado). vosk-android e JNA não declaram permissão (CIA E1/E3). Manifesto final: só `RECORD_AUDIO` (`aapt2`).
- **Q6 aplicada** (todo caminho novo): log (nenhum `Log.` no `:app`; R8 remove no release; Kaldi em só erros); `SavedStateHandle`/`rememberSaveable` (nenhum); crash report (não há biblioteca de crash); arquivo temporário (`model-pt.tmp` só com o modelo); intent (nenhum intent leva texto); clipboard (a etapa0 não tem Colar); nome de arquivo (regex fechada); painel de depuração no release (existe na etapa0, que não é distribuída; o código não vai para o `app/`, TRD §Privacidade nos testes). Resultado: nenhuma saída de texto ou áudio fora das exportações de textos de teste.

## Tarefas

Convenções para todas as tarefas:

- Diretório de trabalho: `etapa0/` (os comandos `./gradlew` rodam lá). Caminhos de arquivo abaixo são relativos a `etapa0/`, exceto `docs/` e `work/`.
- Pacote base: `KT = app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0` e `KTT = app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0`; `BL = build-logic/src/main/kotlin/io/github/gigioderico/tribuna/buildlogic` e `BLT = build-logic/src/test/kotlin/io/github/gigioderico/tribuna/buildlogic`.
- "Substituir" = trocar o arquivo inteiro pelo bloco. Os blocos já passaram no `ktlintCheck`; se o `ktlintFormat` mudar algo, aceite a formatação e siga.
- Antes de cada commit: `./gradlew ktlintCheck` verde. `git add` só dos arquivos da tarefa, com caminho explícito.
- Ver os resultados dos testes: `./gradlew testDebugUnitTest --console=plain` e os XML em `app/build/test-results/testDebugUnitTest/` e `build-logic/build/test-results/test/`.

---

### Tarefa 1 (motor-voz): `build-logic` — manifesto endurecido, AAB no 16 KB, verificação ligada ao build

Resolve a mitigação 1 da CIA, OPR-004 e OPR-005 (B4, B5).

**Arquivos:** modificar `BLT/ManifestPermissionCheckTest.kt`, `BLT/NativeLibScanTest.kt`, `BL/ManifestPermissionCheck.kt`, `BL/NativeLibScan.kt`, `BL/Verify16kAlignmentTask.kt`, `BL/AndroidVerificationsPlugin.kt`.

- [ ] **Passo 1: testes primeiro.** Substituir `BLT/ManifestPermissionCheckTest.kt`:

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

    @Test
    fun declaracaoDePermissaoPropria_falha() {
        // XML de E1 da CIA 0003: o que o androidx.core injeta sem o tools:node="remove".
        val nome = "io.github.gigioderico.tribuna.etapa0.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        assertEquals(
            listOf(
                "permissão não permitida: $nome",
                "declaração de permissão não permitida: $nome",
            ),
            problems(
                manifest(
                    perm(recordAudio),
                    "<permission android:name=\"$nome\" android:protectionLevel=\"signature\" />",
                    perm(nome),
                ),
            ),
        )
    }

    @Test
    fun soADeclaracao_semUsesPermission_tambemFalha() {
        assertEquals(
            listOf("declaração de permissão não permitida: x.y.MINHA"),
            problems(manifest(perm(recordAudio), "<permission android:name=\"x.y.MINHA\" />")),
        )
    }

    @Test
    fun grupoEArvoreDePermissao_falham() {
        assertEquals(
            listOf(
                "declaração de permissão não permitida: x.y.ARVORE",
                "declaração de permissão não permitida: x.y.GRUPO",
            ),
            problems(
                manifest(
                    perm(recordAudio),
                    "<permission-tree android:name=\"x.y.ARVORE\" />",
                    "<permission-group android:name=\"x.y.GRUPO\" />",
                ),
            ),
        )
    }
}
```

Substituir `BLT/NativeLibScanTest.kt`:

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

    private fun apk(vararg entries: Pair<String, ByteArray>): File = archive("app.apk", *entries)

    private fun archive(
        name: String,
        vararg entries: Pair<String, ByteArray>,
    ): File {
        val file = tmp.newFile(name)
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

    @Test
    fun aabComAsDuasLibsAlinhadasEmBaseLib_semProblemas() {
        val file =
            archive(
                "app.aab",
                "base/lib/arm64-v8a/libvosk.so" to aligned,
                "base/lib/arm64-v8a/libjnidispatch.so" to aligned,
                "base/lib/x86_64/libvosk.so" to aligned,
                "base/lib/x86_64/libjnidispatch.so" to aligned,
            )
        assertEquals(
            emptyList<String>(),
            NativeLibScan.check(file, required, minAlign = 16_384L, libRoot = NativeLibScan.BUNDLE_LIB_ROOT),
        )
    }

    @Test
    fun aabComLibDesalinhadaEAbiIncompleta_eReportado() {
        val file =
            archive(
                "app.aab",
                "base/lib/arm64-v8a/libvosk.so" to misaligned,
                "base/lib/arm64-v8a/libjnidispatch.so" to aligned,
                "base/lib/x86/libvosk.so" to aligned,
            )
        assertEquals(
            listOf(
                "app.aab: base/lib/arm64-v8a/libvosk.so tem segmento LOAD alinhado a 4096 bytes (mínimo 16384)",
                "app.aab: x86 sem libjnidispatch.so",
            ),
            NativeLibScan.check(file, required, minAlign = 16_384L, libRoot = NativeLibScan.BUNDLE_LIB_ROOT),
        )
    }

    @Test
    fun aabSemBaseLib_falhaExplicitamente() {
        // Um AAB com as libs fora de base/lib/ (ou um APK lido como AAB) não pode passar em silêncio (Q3).
        val file = archive("app.aab", "lib/arm64-v8a/libvosk.so" to aligned, "lib/arm64-v8a/libjnidispatch.so" to aligned)
        assertEquals(
            listOf("app.aab: nenhuma biblioteca nativa em base/lib/<abi>/; exigidas: libjnidispatch.so, libvosk.so"),
            NativeLibScan.check(file, required, minAlign = 16_384L, libRoot = NativeLibScan.BUNDLE_LIB_ROOT),
        )
    }

    @Test
    fun relatorio_listaCadaLibComOAlinhamento() {
        val file =
            archive(
                "app.aab",
                "base/lib/x86/libvosk.so" to misaligned,
                "base/lib/arm64-v8a/libvosk.so" to aligned,
                "base/assets/model-pt/uuid" to ByteArray(4),
            )
        assertEquals(
            listOf("app.aab: base/lib/arm64-v8a/libvosk.so LOAD 16384", "app.aab: base/lib/x86/libvosk.so LOAD 4096"),
            NativeLibScan.report(file, NativeLibScan.BUNDLE_LIB_ROOT),
        )
    }
}
```

- [ ] **Passo 2: rodar e ver falhar.** `./gradlew testDebugUnitTest --console=plain`. Esperado: FAIL de compilação do `build-logic` ("Unresolved reference 'BUNDLE_LIB_ROOT'", "'report'", "No parameter with name 'libRoot'"). Se compilar, os testes `declaracaoDePermissaoPropria_falha`, `soADeclaracao_semUsesPermission_tambemFalha` e `grupoEArvoreDePermissao_falham` têm de falhar.

- [ ] **Passo 3: código.** Substituir `BL/ManifestPermissionCheck.kt`:

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

    /** Declarações de permissão própria; nenhuma é permitida (CIA 0003, portão 1, opção A). */
    private val DECLARATION_TAGS = listOf("permission", "permission-group", "permission-tree")

    fun declaredPermissions(manifestXml: String): List<String> = namesOf(manifestXml, PERMISSION_TAGS)

    fun permissionDeclarations(manifestXml: String): List<String> = namesOf(manifestXml, DECLARATION_TAGS)

    private fun namesOf(
        manifestXml: String,
        tags: List<String>,
    ): List<String> {
        val factory =
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(manifestXml)))
        return tags.flatMap { tag ->
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
        val declarations =
            permissionDeclarations(manifestXml).distinct().sorted().map { "declaração de permissão não permitida: $it" }
        return extra + missing + declarations
    }
}
```

Substituir `BL/NativeLibScan.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import java.io.File
import java.util.zip.ZipFile

/**
 * Varre as .so em <libRoot><abi>/ de um APK (`lib/`) ou de um AAB (`base/lib/`): exige as bibliotecas pedidas
 * em toda ABI e confere o alinhamento dos segmentos LOAD de toda .so (TRD §Qualidade de código: "em todas as
 * ABIs"; OPR-005).
 */
object NativeLibScan {
    const val APK_LIB_ROOT = "lib/"
    const val BUNDLE_LIB_ROOT = "base/lib/"

    private fun entryPattern(libRoot: String) = Regex("^" + Regex.escape(libRoot) + "([^/]+)/([^/]+\\.so)$")

    /** Uma linha por .so com os alinhamentos dos segmentos LOAD, para a evidência (spec 0003, B5). */
    fun report(
        archive: File,
        libRoot: String = APK_LIB_ROOT,
    ): List<String> =
        ZipFile(archive).use { zip ->
            val pattern = entryPattern(libRoot)
            zip
                .entries()
                .asSequence()
                .filter { pattern.matches(it.name) }
                .sortedBy { it.name }
                .map { entry ->
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    val aligns = runCatching { ElfAlignment.loadSegmentAlignments(bytes) }.getOrNull()
                    "${archive.name}: ${entry.name} LOAD ${aligns?.distinct()?.joinToString("/") ?: "ELF inválido"}"
                }.toList()
        }

    fun check(
        apk: File,
        requiredLibs: Set<String>,
        minAlign: Long,
        libRoot: String = APK_LIB_ROOT,
    ): List<String> {
        val entryPattern = entryPattern(libRoot)
        val problems = mutableListOf<String>()
        ZipFile(apk).use { zip ->
            val byAbi =
                zip
                    .entries()
                    .asSequence()
                    .mapNotNull { entry -> entryPattern.matchEntire(entry.name)?.let { it.groupValues[1] to entry } }
                    .groupBy({ it.first }, { it.second })
            if (byAbi.isEmpty()) {
                problems += "${apk.name}: nenhuma biblioteca nativa em $libRoot<abi>/; exigidas: ${requiredLibs.sorted().joinToString()}"
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

Substituir `BL/Verify16kAlignmentTask.kt`:

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
 * Alinhamento de 16 KB no release (TRD §Qualidade de código):
 * - APK: segmentos ELF de toda .so, presença das bibliotecas exigidas em toda ABI e `zipalign -c -P 16 -v 4`;
 * - AAB (OPR-005): segmentos ELF e presença das bibliotecas em `base/lib/<abi>/`. O `zipalign` dos APKs que
 *   o Play gera a partir do AAB é responsabilidade do Play.
 * Sem saídas: roda sempre.
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
        abstract val bundleFile: RegularFileProperty

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
                NativeLibScan.report(apk).forEach { logger.lifecycle("$name: $it") }
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
            val bundle = bundleFile.get().asFile
            NativeLibScan.report(bundle, NativeLibScan.BUNDLE_LIB_ROOT).forEach { logger.lifecycle("$name: $it") }
            val bundleProblems =
                NativeLibScan.check(bundle, requiredLibraries.get(), minAlign = kb * 1024L, libRoot = NativeLibScan.BUNDLE_LIB_ROOT)
            logger.lifecycle("$name: AAB ${bundle.name} -> ${if (bundleProblems.isEmpty()) "OK" else "${bundleProblems.size} problema(s)"}")
            problems += bundleProblems
            if (problems.isNotEmpty()) {
                throw GradleException(
                    buildString {
                        appendLine("$name: alinhamento de $kb KB reprovado (TRD §Qualidade de código):")
                        problems.forEach { appendLine("  - $it") }
                    },
                )
            }
            logger.lifecycle("$name: OK ${apks.joinToString { it.name }}, ${bundle.name}")
        }
    }
```

Substituir `BL/AndroidVerificationsPlugin.kt`:

```kotlin
package io.github.gigioderico.tribuna.buildlogic

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registra verifyMergedManifest (todas as variantes) e verify16kAlignment (release) no módulo Android.
 * O build verifica o manifesto mesclado (TRD §Manifesto; OPR-004): `check` depende do agregado, e
 * `assemble<Variante>` e `bundle<Variante>` dependem da verificação da variante.
 */
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
            project.tasks.named("check") { dependsOn(aggregate) }
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
                val packaging = setOf("assemble$suffix", "bundle$suffix")
                project.tasks.configureEach {
                    if (name in packaging) dependsOn(perVariant)
                }
                if (variant.buildType == "release") {
                    project.tasks.register("verify16kAlignment", Verify16kAlignmentTask::class.java) {
                        group = "verification"
                        description = "Alinhamento de 16 KB de libvosk.so e libjnidispatch.so no APK e no AAB de release."
                        apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
                        bundleFile.set(variant.artifacts.get(SingleArtifact.BUNDLE))
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

- [ ] **Passo 4: rodar e ver passar.** `./gradlew testDebugUnitTest --console=plain`. Esperado: BUILD SUCCESSFUL; `ManifestPermissionCheckTest` 11 testes, `NativeLibScanTest` 9, `ElfAlignmentTest` 7, 0 falhas.

- [ ] **Passo 5: ligação ao build (B4).** `./gradlew check --dry-run --console=plain | grep MergedManifest` lista `:app:verifyDebugMergedManifest`, `:app:verifyReleaseMergedManifest` e `:app:verifyMergedManifest`; `./gradlew assembleRelease --dry-run --console=plain | grep MergedManifest` e `./gradlew bundleRelease --dry-run --console=plain | grep MergedManifest` listam `:app:verifyReleaseMergedManifest`. Neste ponto ainda não há Compose: `./gradlew verifyMergedManifest` passa.

- [ ] **Passo 6: commit.**

```bash
git add etapa0/build-logic/src
git commit -m "feat(build): manifesto reprova <permission>, 16 KB lê o AAB, verificação ligada a check/assemble/bundle (CIA 0003; OPR-004; OPR-005)"
```

---

### Tarefa 2 (motor-voz): dependências, manifesto do portão 1, R8 e TRD §Manifesto (um commit)

Critérios A3, B1, B2, B3, B6. O manifesto e as dependências entram juntos: com o Compose e sem o `tools:node="remove"`, o `verifyMergedManifest` reprova (é o "teste que falha antes").

**Arquivos:** substituir `.editorconfig`, `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`; criar `app/proguard-rules.pro`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/MainActivity.kt` (esqueleto), `scripts/varrer-registerreceiver.sh`; modificar `docs/TRD.md`.

- [ ] **Passo 1: dependências e build.** Substituir `.editorconfig`:

```ini
root = true

[*.{kt,kts}]
ktlint_code_style = ktlint_official
# Composables começam com maiúscula (convenção do Compose; regra 8 do CLAUDE.md).
ktlint_function_naming_ignore_when_annotated_with = Composable
```

Substituir `gradle/libs.versions.toml`:

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
# Compose BOM 2026.06.01 (Compose 1.11.x) e lifecycle 2.10.0: as mais novas que compilam com compileSdk 36 (OPR-023).
composeBom = "2026.06.01"
activityCompose = "1.13.0"
lifecycle = "2.10.0"
vosk = "0.3.75"
# O vosk-android 0.3.75 pede jna 5.18.1; a 5.19.1 é a estável mais recente (Maven Central, 2026-06-12).
jna = "5.19.1"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }
android-gradle-api = { module = "com.android.tools.build:gradle-api", version.ref = "agp" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-foundation = { module = "androidx.compose.foundation:foundation" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
vosk-android = { module = "com.alphacephei:vosk-android", version.ref = "vosk" }
jna = { module = "net.java.dev.jna:jna", version.ref = "jna" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-lint = { id = "com.android.lint", version.ref = "agp" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlintGradle" }
```

Substituir `build.gradle.kts` (raiz):

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
}

tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Roda os testes do build-logic (manifesto e 16 KB) junto com os unitários dos módulos."
    dependsOn(gradle.includedBuild("build-logic").task(":test"))
}
```

Substituir `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    id("tribuna.android.verifications")
}

android {
    namespace = "io.github.gigioderico.tribuna.etapa0"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "io.github.gigioderico.tribuna.etapa0"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode =
            libs.versions.versionCode
                .get()
                .toInt()
        versionName = libs.versions.versionName.get()
        // Proveniência das exportações (spec 0003, D11).
        buildConfigField("String", "VOSK_VERSION", "\"${libs.versions.vosk.get()}\"")
        ndk {
            // As ABIs em que o vosk-android 0.3.75 traz libvosk.so; a JNA 5.19.1 traz também armeabi, mips e
            // mips64, onde o app não funcionaria sem o Vosk (impl_plan 0003, A7).
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            // TRD §Distribuição: R8 ligado com as regras do Vosk e da JNA; spec 0003 D19: assinado com a chave de debug.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    }
}

/** Copia etapa0/textos/ para assets/textos/ (spec 0003, D9: só textos de teste habilitam a exportação). */
abstract class TestTextsAssetsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val textos: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val dest = outputDir.get().dir("textos").asFile
        dest.deleteRecursively()
        dest.mkdirs()
        textos
            .get()
            .asFile
            .listFiles { file -> file.name.endsWith(".txt") }
            .orEmpty()
            .sortedBy { it.name }
            .forEach { it.copyTo(File(dest, it.name)) }
    }
}

val textosDeTeste = rootProject.layout.projectDirectory.dir("textos")

val testTextsAssets =
    tasks.register<TestTextsAssetsTask>("testTextsAssets") {
        textos.set(textosDeTeste)
    }

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(testTextsAssets, TestTextsAssetsTask::outputDir)
    }
}

// Os testes JVM leem os textos de teste e os assets reais do modelo pelo caminho (spec 0003, D6, D8; OPR-020).
tasks.withType<Test>().configureEach {
    inputs
        .dir(textosDeTeste)
        .withPropertyName("textosDeTeste")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("tribuna.textos", textosDeTeste.asFile.absolutePath)
    systemProperty(
        "tribuna.modelo",
        layout.projectDirectory
            .dir("src/main/assets/model-pt")
            .asFile.absolutePath,
    )
}

ktlint {
    version.set(libs.versions.ktlint.get())
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.vosk.android)
    // TRD §Stack: JNA como AAR (o JAR traria as bibliotecas nativas de desktop).
    implementation(libs.jna) {
        artifact {
            type = "aar"
        }
    }
    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Lista os AAR/JAR do classpath de release para scripts/varrer-registerreceiver.sh (CIA 0003, mitigação 3).
tasks.register("releaseClasspathFiles") {
    group = "verification"
    description = "Lista os arquivos do classpath de release (varredura de registerReceiver, CIA 0003)."
    val artifacts =
        configurations
            .named("releaseRuntimeClasspath")
            .map { it.incoming.artifacts }
    doLast {
        artifacts.get().artifacts.forEach {
            println("ART ${it.id.componentIdentifier.displayName.replace(' ', '_')} ${it.file}")
        }
    }
}
```

Criar `app/proguard-rules.pro`:

```proguard
# TRD §Distribuição: classes do Vosk e da JNA são acessadas por código nativo; os nomes ficam.
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
# A JNA referencia AWT (desktop), ausente no Android; o caminho não é usado.
-dontwarn java.awt.**

# TRD §Onde o texto vive: em release, o R8 remove as chamadas de Log.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static int println(...);
}
```

Criar o esqueleto `KT/MainActivity.kt` (a Tarefa 15 o substitui; ele existe agora porque o manifesto declara `.MainActivity` e o lint acusaria classe ausente):

```kotlin
package io.github.gigioderico.tribuna.etapa0

import androidx.activity.ComponentActivity

/** Esqueleto da Activity declarada no manifesto (CIA 0003, portão 1). A tela entra na Tarefa 15 do impl_plan. */
class MainActivity : ComponentActivity()
```

- [ ] **Passo 2: ver a verificação falhar com o manifesto antigo.** `./gradlew verifyMergedManifest --console=plain`. Esperado: FAIL nas duas variantes com `permissão não permitida: io.github.gigioderico.tribuna.etapa0.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` e `declaração de permissão não permitida: io.github.gigioderico.tribuna.etapa0.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Guarde a saída em `work/0003-etapa0-vosk/evidencias/B1-manifesto-antes.txt`.

- [ ] **Passo 3: manifesto e regras aprovados no portão 1.** Substituir `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <!-- OPR-001: injetadas pelo androidx.core; o app não usa ContextCompat.registerReceiver com RECEIVER_NOT_EXPORTED. -->
    <permission
        android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        tools:node="remove" />
    <uses-permission
        android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        tools:node="remove" />

    <application
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:label="@string/app_name">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

Criar `app/src/main/res/xml/data_extraction_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" path="." />
        <exclude domain="file" path="." />
        <exclude domain="database" path="." />
        <exclude domain="sharedpref" path="." />
        <exclude domain="external" path="." />
        <exclude domain="device_root" path="." />
        <exclude domain="device_file" path="." />
        <exclude domain="device_database" path="." />
        <exclude domain="device_sharedpref" path="." />
    </device-transfer>
</data-extraction-rules>
```

Criar `app/src/main/res/xml/backup_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <exclude domain="root" path="." />
    <exclude domain="file" path="." />
    <exclude domain="database" path="." />
    <exclude domain="sharedpref" path="." />
    <exclude domain="external" path="." />
    <exclude domain="device_root" path="." />
    <exclude domain="device_file" path="." />
    <exclude domain="device_database" path="." />
    <exclude domain="device_sharedpref" path="." />
</full-backup-content>
```

- [ ] **Passo 4: TRD §Manifesto (item 4 da aprovação humana).** Em `docs/TRD.md`, seção "Ciclo de vida, privacidade e permissões" → **Manifesto**, substituir as três linhas que começam com "- Única permissão: RECORD_AUDIO.", "- android:allowBackup=\"false\" e regras de extração de dados vazias" e "- O build verifica o manifesto final mesclado: falha se alguma biblioteca trouxer INTERNET." por exatamente:

```markdown
- Única permissão: RECORD_AUDIO. Sem INTERNET, sem ACCESS_NETWORK_STATE. A tela ligada usa a flag da janela, então WAKE_LOCK não é necessário.
- A permissão própria `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, que o androidx.core (1.9+) injeta, é removida no manifesto do app com `tools:node="remove"`, tanto o `<permission>` quanto o `<uses-permission>`. Por isso nenhum código do app chama `ContextCompat.registerReceiver` (nem `Context.receiveBroadcasts` do core-ktx) com `RECEIVER_NOT_EXPORTED`: do Android 8 ao 12L essa chamada lança exceção sem a permissão. Se um receptor não exportado for necessário, usar `Context.registerReceiver(…, RECEIVER_NOT_EXPORTED)` só a partir do API 33. A cada troca de versão de dependência, a varredura de `registerReceiver` no classpath de release confirma que nenhuma biblioteca usa esse caminho.
- android:allowBackup="false", e regras de extração e de backup que excluem todos os domínios (`root`, `file`, `database`, `sharedpref`, `external` e os `device_*`), em `cloud-backup` e em `device-transfer` (`android:dataExtractionRules`, Android 12+) e em `android:fullBackupContent` (Android 8–11). Seção ausente ou vazia não exclui nada, e no Android 12+ `allowBackup="false"` sozinho não desliga a transferência entre aparelhos.
- O build verifica o manifesto final mesclado: falha se houver `uses-permission` além de RECORD_AUDIO ou qualquer declaração `<permission>`, `<permission-group>` ou `<permission-tree>`.
```

- [ ] **Passo 5: script da varredura de `registerReceiver` (CIA, mitigação 3).** Criar `scripts/varrer-registerreceiver.sh` e dar permissão de execução (`chmod +x scripts/varrer-registerreceiver.sh`):

```bash
#!/usr/bin/env bash
# CIA 0003 (portão 1, opção A), mitigação 3: varre as classes do classpath de release e mostra cada chamada de
# ContextCompat.registerReceiver com a flag passada, e cada chamada de receiveBroadcasts (core-ktx).
# Passa (exit 0) só se nenhuma chamada usar RECEIVER_NOT_EXPORTED (4) ou flag não constante, e se ninguém
# chamar receiveBroadcasts (o ContextKt do core-ktx só repassa a flag de quem chama). Rodar a cada troca de versão no catalog; a saída vai para work/<tarefa>/evidencias/.
set -euo pipefail
cd "$(dirname "$0")/.."
WORK="$(mktemp -d)"
trap 'rm -r "$WORK"' EXIT
./gradlew -q :app:releaseClasspathFiles --console=plain | sed -n 's/^ART //p' > "$WORK/arts.txt"
n=0
classes=0
: > "$WORK/dirs.txt"
while read -r id file; do
  [ -f "$file" ] || { echo "aviso: $id sem arquivo ($file); rode assembleRelease antes"; continue; }
  n=$((n + 1))
  dir="$WORK/c$n"
  mkdir -p "$dir/x"
  case "$file" in
    *.aar)
      if unzip -q -o "$file" classes.jar -d "$dir" 2>/dev/null; then unzip -q -o "$dir/classes.jar" -d "$dir/x"; fi
      ;;
    *.jar) unzip -q -o "$file" -d "$dir/x" ;;
  esac
  count=$(find "$dir/x" -name '*.class' | wc -l | tr -d ' ')
  classes=$((classes + count))
  echo "$id $dir/x" >> "$WORK/dirs.txt"
done < "$WORK/arts.txt"
echo "bibliotecas: $n; classes: $classes"
fail=0
found=0
while read -r id dir; do
  while IFS= read -r cls; do
    rel="${cls#"$dir"/}"
    out=$(javap -c -p "$cls" | awk -v lib="$id" -v cls="$rel" '
      /invokestatic.*androidx\/core\/content\/ContextCompat\.registerReceiver/ {
        flag = "nao_constante"
        if (prev ~ /iconst_2$/) flag = "2_EXPORTED"
        else if (prev ~ /iconst_4$/) flag = "4_NOT_EXPORTED"
        else if (prev ~ /(bipush|sipush|iconst_[0-9m]+)/) { n = split(prev, a, " "); flag = "constante_" a[n] }
        if (cls ~ /^androidx\/core\/content\/ContextKt/) flag = "repassada_por_receiveBroadcasts"
        print lib " " cls " ContextCompat.registerReceiver flag=" flag
      }
      /invoke[a-z]+ .*receiveBroadcasts/ && cls !~ /^androidx\/core\/content\/ContextKt/ { print lib " " cls " receiveBroadcasts chamada" }
      /^ *[0-9]+: / { prev = $0 }')
    if [ -n "$out" ]; then
      echo "$out"
      found=$((found + 1))
      if grep -qE "4_NOT_EXPORTED|nao_constante|receiveBroadcasts chamada" <<<"$out"; then fail=1; fi
    fi
  done < <(grep -rlaE "ContextCompat\.registerReceiver|androidx/core/content/ContextCompat|receiveBroadcasts" "$dir" --include='*.class' 2>/dev/null | grep -v "androidx/core/content/ContextCompat" || true)
done < "$WORK/dirs.txt"
echo "classes com chamada: $found"
if [ "$fail" -eq 0 ]; then echo "OK: nenhuma chamada com RECEIVER_NOT_EXPORTED ou flag não constante"; else echo "REPROVADO: ver as linhas acima"; fi
exit "$fail"
```

- [ ] **Passo 6: rodar e ver passar.**

```bash
./gradlew testDebugUnitTest verifyMergedManifest lint ktlintCheck --console=plain
./gradlew assembleDebug assembleRelease bundleRelease --console=plain
"$ANDROID_HOME/build-tools/36.0.0/aapt2" dump permissions app/build/outputs/apk/release/app-release.apk
grep -E "^(org.vosk.Recognizer|com.sun.jna.Native) ->" app/build/outputs/mapping/release/mapping.txt
./scripts/verificar-manifesto-negativo.sh; echo "exit=$?"
./gradlew -q :core:jar && ./scripts/varrer-registerreceiver.sh; echo "exit=$?"
```

(Se `ANDROID_HOME` não estiver definido, use o `sdk.dir` de `local.properties`.) Esperado: `verify{Debug,Release}MergedManifest: OK … -> android.permission.RECORD_AUDIO`; lint sem erro e sem `DataExtractionRules` (só `MissingApplicationIcon`, que já existia); os três empacotamentos verdes; `aapt2` com só `uses-permission: name='android.permission.RECORD_AUDIO'`; o `mapping.txt` com as duas linhas `X -> X:`; o script negativo `exit=0`; a varredura `OK: nenhuma chamada …`, `exit=0`. Se o R8 parar em `Missing class java.awt.*`, confira que a linha `-dontwarn java.awt.**` está no `proguard-rules.pro`. Guarde as saídas em `work/0003-etapa0-vosk/evidencias/` (`B1-B3-manifesto.txt`, `B2-negativo.txt`, `B6-mapping.txt`, `varredura-registerreceiver.txt`).

- [ ] **Passo 7: commit (manifesto, endurecimento já commitado na Tarefa 1, dependências e TRD juntos).**

```bash
git add etapa0/.editorconfig etapa0/gradle/libs.versions.toml etapa0/build.gradle.kts etapa0/app/build.gradle.kts \
  etapa0/app/proguard-rules.pro etapa0/app/src/main/AndroidManifest.xml etapa0/app/src/main/res/xml \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/MainActivity.kt \
  etapa0/scripts/varrer-registerreceiver.sh docs/TRD.md work/0003-etapa0-vosk/evidencias
git commit -m "feat(etapa0): Compose, Vosk e JNA pelo catalog; manifesto e backup do portão 1; R8 (CIA 0003; TRD §Manifesto, §Distribuição)"
```

---

### Tarefa 3 (motor-voz): modelo no LFS e leitura do léxico (`FstSymbols`)

Critério A4; base do A5/OPR-020.

**Arquivos:** criar `scripts/baixar-modelo.sh`, `app/src/main/assets/model-pt/**` (gerado), `KTT/speech/FstSymbolsTest.kt`, `KT/speech/FstSymbols.kt`.

- [ ] **Passo 1: script.** Criar `scripts/baixar-modelo.sh` e `chmod +x scripts/baixar-modelo.sh`:

```bash
#!/usr/bin/env bash
# Baixa o vosk-model-small-pt-0.3 (Apache 2.0, alphacephei.com) para app/src/main/assets/model-pt/ e grava o
# arquivo uuid com o SHA-256 do zip (spec 0003, A4, D12; TRD Motor de voz §Instalação do modelo 2).
# O SHA-256 esperado foi medido pelo arquiteto em 2026-10-11 (impl_plan 0003); outro zip aborta (Q8).
set -euo pipefail
cd "$(dirname "$0")/.."
URL="https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip"
SHA256="6e1ce909032e1afa7a88e68a3d628ecafff302bdf195befab308826c395e93b7"
DEST="app/src/main/assets/model-pt"
if [ -e "$DEST" ]; then
  echo "abortado: $DEST já existe"
  exit 2
fi
TMP="$(mktemp -d)"
trap 'rm -r "$TMP"' EXIT
curl -fsSL -o "$TMP/modelo.zip" "$URL"
got="$(shasum -a 256 "$TMP/modelo.zip" | cut -d' ' -f1)"
if [ "$got" != "$SHA256" ]; then
  echo "ERRO: SHA-256 do zip é $got; esperado $SHA256"
  exit 1
fi
mkdir "$TMP/x"
unzip -q "$TMP/modelo.zip" -d "$TMP/x"
mkdir -p "$DEST"
cp -R "$TMP/x/vosk-model-small-pt-0.3/." "$DEST/"
printf 'vosk-model-small-pt-0.3 sha256=%s\n' "$SHA256" > "$DEST/uuid"
echo "OK: zip sha256=$got"
echo "OK: uuid=$(cat "$DEST/uuid")"
find "$DEST" -type f | sort | while read -r f; do printf '%10d %s\n' "$(wc -c < "$f")" "$f"; done
```

- [ ] **Passo 2: baixar.** `./scripts/baixar-modelo.sh | tee ../work/0003-etapa0-vosk/evidencias/A4-modelo.txt`. Esperado: `OK: zip sha256=6e1ce909032e1afa7a88e68a3d628ecafff302bdf195befab308826c395e93b7`, `OK: uuid=vosk-model-small-pt-0.3 sha256=6e1c…93b7` e a lista de 16 arquivos (os 15 do zip + `uuid`; `Gr.fst` com 16.007.119 bytes). Se o SHA-256 não bater, **pare**: o zip publicado mudou, e o modelo novo precisa de decisão (Q8); escale ao arquiteto.

- [ ] **Passo 3: conferir o LFS.** `git add etapa0/app/src/main/assets/model-pt` e depois `git lfs ls-files | grep model-pt` lista os 16 arquivos (todos com `*` ou `-`), e `git diff --cached --stat` mostra ponteiros pequenos, não 16 MB. Se `git lfs ls-files` não listar, **não commite**: confira o `.gitattributes` da raiz (linha `etapa0/**/model-pt/** filter=lfs …`).

- [ ] **Passo 4: teste primeiro.** Criar `KTT/speech/FstSymbolsTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Leitura da tabela de símbolos de um FST do OpenFst (léxico do modelo; OPR-020). */
class FstSymbolsTest {
    private class Bytes {
        val out = ByteArrayOutputStream()

        fun int32(v: Int) = apply { repeat(4) { out.write((v shr (8 * it)) and 0xFF) } }

        fun int64(v: Long) = apply { repeat(8) { out.write(((v shr (8 * it)) and 0xFF).toInt()) } }

        fun string(s: String) =
            apply {
                val b = s.toByteArray(Charsets.UTF_8)
                int32(b.size)
                out.write(b)
            }
    }

    private fun fst(
        symbols: List<String>,
        flags: Int = 3,
        magic: Int = 0x7EB2FDD6,
    ): ByteArray {
        val b =
            Bytes()
                .int32(magic)
                .string("ngram")
                .string("standard")
                .int32(4)
                .int32(flags)
        b
            .int64(0)
            .int64(1)
            .int64(10)
            .int64(0)
        b
            .int32(0x7EB2FB74)
            .string("words.txt")
            .int64(symbols.size.toLong())
            .int64(symbols.size.toLong())
        symbols.forEachIndexed { i, s -> b.string(s).int64(i.toLong()) }
        b.int32(0) // resto do FST, não lido
        return b.out.toByteArray()
    }

    @Test
    fun leATabelaDeSimbolosDeEntrada() {
        val symbols = listOf("<eps>", "!SIL", "[unk]", "ouvir", "três", "versículo")
        assertEquals(symbols, FstSymbols.inputSymbols(ByteArrayInputStream(fst(symbols))))
    }

    @Test
    fun naoEFst_falha() {
        assertTrue(
            runCatching { FstSymbols.inputSymbols(ByteArrayInputStream(fst(listOf("a"), magic = 1))) }.exceptionOrNull() is IOException,
        )
    }

    @Test
    fun fstSemTabelaDeEntrada_falha() {
        assertTrue(
            runCatching { FstSymbols.inputSymbols(ByteArrayInputStream(fst(listOf("a"), flags = 2))) }.exceptionOrNull() is IOException,
        )
    }
}
```

- [ ] **Passo 5: ver falhar.** `./gradlew :app:testDebugUnitTest --console=plain`. Esperado: FAIL de compilação, "Unresolved reference 'FstSymbols'".

- [ ] **Passo 6: código.** Criar `KT/speech/FstSymbols.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Lê a tabela de símbolos de entrada de um FST do OpenFst (formato binário). No `vosk-model-small-pt-0.3`, o
 * léxico do modelo é a tabela de `Gr.fst` (o modelo não traz `words.txt`; OPR-020). Só lê o cabeçalho.
 */
object FstSymbols {
    private const val FST_MAGIC = 0x7EB2FDD6
    private const val SYMBOLS_MAGIC = 0x7EB2FB74
    private const val HAS_INPUT_SYMBOLS = 1
    private const val MAX_STRING = 1 shl 16
    private const val MAX_SYMBOLS = 10_000_000L

    fun inputSymbols(stream: InputStream): List<String> {
        val input = DataInputStream(BufferedInputStream(stream))
        if (int32(input) != FST_MAGIC) throw IOException("não é um FST do OpenFst")
        string(input) // tipo do FST
        string(input) // tipo do arco
        int32(input) // versão
        val flags = int32(input)
        int64(input) // propriedades
        int64(input) // estado inicial
        int64(input) // número de estados
        int64(input) // número de arcos
        if (flags and HAS_INPUT_SYMBOLS == 0) throw IOException("FST sem tabela de símbolos de entrada")
        if (int32(input) != SYMBOLS_MAGIC) throw IOException("tabela de símbolos inválida")
        string(input) // nome da tabela
        int64(input) // próxima chave livre
        val size = int64(input)
        if (size < 0 || size > MAX_SYMBOLS) throw IOException("tamanho de tabela inválido: $size")
        return List(size.toInt()) {
            val symbol = string(input)
            int64(input) // chave
            symbol
        }
    }

    private fun int32(input: DataInputStream): Int {
        val b0 = input.readUnsignedByte()
        val b1 = input.readUnsignedByte()
        val b2 = input.readUnsignedByte()
        val b3 = input.readUnsignedByte()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun int64(input: DataInputStream): Long {
        val low = int32(input).toLong() and 0xFFFFFFFFL
        val high = int32(input).toLong()
        return low or (high shl 32)
    }

    private fun string(input: DataInputStream): String {
        val length = int32(input)
        if (length < 0 || length > MAX_STRING) throw IOException("texto de tamanho inválido: $length")
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }
}
```

- [ ] **Passo 7: ver passar.** `./gradlew :app:testDebugUnitTest --console=plain`: `FstSymbolsTest` 3 testes verdes.

- [ ] **Passo 8: commit.**

```bash
git add etapa0/scripts/baixar-modelo.sh etapa0/app/src/main/assets/model-pt \
  etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/FstSymbolsTest.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/FstSymbols.kt \
  work/0003-etapa0-vosk/evidencias/A4-modelo.txt
git commit -m "feat(etapa0): vosk-model-small-pt-0.3 no LFS com uuid e SHA-256; leitura do léxico do Gr.fst (TRD §Instalação do modelo 2; OPR-020)"
git push   # se o push for recusado por cota de LFS: pare e avise o orquestrador (portão humano 6)
```

---

### Tarefa 4 (motor-voz): `Lexicon` e `Vocabulary` (C5; OPR-020)

**Arquivos:** criar `KTT/TestFiles.kt`, `KTT/speech/VocabularyTest.kt`, `KTT/speech/LexiconRealModelTest.kt`, `KT/speech/Vocabulary.kt`.

- [ ] **Passo 1: testes primeiro.** Criar `KTT/TestFiles.kt` (acesso aos textos de teste e ao modelo pelo caminho que o Gradle passa; usado por todos os testes do `:app` que leem arquivos):

```kotlin
package io.github.gigioderico.tribuna.etapa0

import java.io.File

/** Textos de etapa0/textos/ e assets reais do modelo, pelo caminho que o Gradle passa (CLAUDE.md: só textos de teste). */
object TestFiles {
    private val textos: File by lazy {
        File(System.getProperty("tribuna.textos") ?: error("propriedade tribuna.textos ausente: rode pelo Gradle"))
    }

    /** Diretório `app/src/main/assets/model-pt` (LFS; baixado por scripts/baixar-modelo.sh). */
    val modelDir: File by lazy {
        File(System.getProperty("tribuna.modelo") ?: error("propriedade tribuna.modelo ausente: rode pelo Gradle"))
    }

    val textIds = listOf("L30", "T01", "T02", "T03", "T04", "T05", "T06", "T07", "T08", "T09", "T10")

    fun text(id: String): String = File(textos, "$id.txt").readText(Charsets.UTF_8)

    /** Palavras como o Vosk as devolve: minúsculas, com acento, sem pontuação (só o formato; o JSON real é da 0005). */
    fun spokenLike(id: String): List<String> =
        text(id)
            .lines()
            .filterNot { it.trim().startsWith("//") }
            .flatMap { line -> line.lowercase().split(Regex("[^\\p{L}\\p{N}-]+")) }
            .filter { it.isNotEmpty() }
}
```

Criar `KTT/speech/VocabularyTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.align.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** C5 e OPR-020: lista do vocabulário restrito (TRD Motor de voz §Vocabulário restrito; spec 0003, D5). */
class VocabularyTest {
    private val t02 = TestFiles.text("T02")
    private val t02Tokens = Tokenizer.tokenize(t02)
    private val spoken = t02Tokens.flatMap { t -> t.spoken.flatten() }.distinct()

    @Test
    fun semLexico_cadaPalavraDeTokenSpokenUmaVez_maisUnk() {
        val list = Vocabulary.build(t02, t02Tokens, lexicon = null)
        assertEquals(spoken + "[unk]", list)
        assertEquals(list.size, list.distinct().size)
    }

    @Test
    fun comLexico_comecaPelaFormaDoTrd_eAcrescentaAGrafiaDoLexico() {
        val lexicon = Lexicon(listOf("<eps>", "!SIL", "#0", "[unk]", "três", "tres", "versículo", "você", "coríntio", "ouvir"))
        val list = Vocabulary.build(t02, t02Tokens, lexicon)
        assertEquals(spoken, list.take(spoken.size))
        assertEquals(listOf("três", "versículo", "você", "[unk]"), list.drop(spoken.size))
    }

    @Test
    fun comLexico_partesDePalavraComHifen_entramComoNoLexico() {
        val t03 = TestFiles.text("T03")
        val list = Vocabulary.build(t03, Tokenizer.tokenize(t03), Lexicon(listOf("bom", "dia", "escolha")))
        assertTrue("bom" in list && "dia" in list)
        assertTrue("bomdia" in list)
    }

    @Test
    fun simbolosInternosDoModelo_naoEntram() {
        val lexicon = Lexicon(listOf("<eps>", "!SIL", "#0", "[unk]"))
        assertEquals(0, lexicon.size)
        assertEquals(emptyList<String>(), lexicon.spellings("0"))
    }
}
```

Criar `KTT/speech/LexiconRealModelTest.kt` (lê o modelo real em assets; é a evidência por comando do OPR-020 e do `uuid`):

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.align.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Evidência por comando contra o modelo real em assets (Q4; OPR-020; spec 0003, A4, D12): o léxico do
 * `vosk-model-small-pt-0.3` tem a grafia com acento, e `Token.spoken` não; o vocabulário expandido cobre isso.
 */
class LexiconRealModelTest {
    private val modelDir = TestFiles.modelDir

    private val lexiconWords: List<String> by lazy {
        val fst = File(modelDir, "Gr.fst")
        assertTrue("modelo ausente ou só ponteiro do LFS: rode scripts/baixar-modelo.sh e git lfs pull", fst.length() > 1_000_000)
        fst.inputStream().use(FstSymbols::inputSymbols)
    }

    @Test
    fun uuidDoModelo_temNomeESha256DoZip() {
        val uuid = File(modelDir, "uuid").readLines().first()
        assertTrue(uuid, Regex("^vosk-model-small-pt-0\\.3 sha256=[0-9a-f]{64}$").matches(uuid))
    }

    @Test
    fun lexicoDoModelo_temAGrafiaComAcento() {
        val words = lexiconWords.toSet()
        assertEquals(99_105, lexiconWords.size)
        assertTrue(listOf("versículo", "três", "não", "coração", "[unk]").all { it in words })
        assertFalse("versiculo" in words)
        assertFalse("nao" in words)
    }

    @Test
    fun t02_formasFaladasForaDoLexico_eAsQueContinuamSemGrafiaDepoisDaExpansao() {
        val words = lexiconWords.toSet()
        val lexicon = Lexicon(lexiconWords)
        val spoken = Tokenizer.tokenize(TestFiles.text("T02")).flatMap { t -> t.spoken.flatten() }.distinct()
        val outside = spoken.filter { it !in words }
        assertEquals(12, outside.size)
        assertEquals(listOf("corintios"), outside.filter { lexicon.spellings(it).isEmpty() })
    }

    @Test
    fun t02_vocabularioExpandido_trazAGrafiaDoLexico() {
        val text = TestFiles.text("T02")
        val list = Vocabulary.build(text, Tokenizer.tokenize(text), Lexicon(lexiconWords))
        assertTrue(listOf("versículo", "três", "você", "capítulo").all { it in list })
        assertEquals(Vocabulary.UNKNOWN, list.last())
    }
}
```

- [ ] **Passo 2: ver falhar.** `./gradlew :app:testDebugUnitTest --console=plain`: FAIL de compilação, "Unresolved reference 'Vocabulary'" e "'Lexicon'".

- [ ] **Passo 3: código.** Criar `KT/speech/Vocabulary.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import io.github.gigioderico.tribuna.etapa0.align.Normalizer
import io.github.gigioderico.tribuna.etapa0.align.Token

/** Léxico do modelo indexado pela forma normalizada (OPR-020). Sem símbolos internos (`<eps>`, `!SIL`, `#0`). */
class Lexicon(
    words: Collection<String>,
) {
    private val byNormalized: Map<String, List<String>> =
        words
            .filter { it.isNotEmpty() && it != Vocabulary.UNKNOWN && it[0] !in INTERNAL_PREFIXES }
            .groupBy(Normalizer::normalize)
            .mapValues { (_, spellings) -> spellings.sorted() }

    val size: Int = byNormalized.values.sumOf { it.size }

    /** Grafias do léxico cuja forma normalizada é `normalized`; vazia se nenhuma. */
    fun spellings(normalized: String): List<String> = byNormalized[normalized].orEmpty()

    private companion object {
        const val INTERNAL_PREFIXES = "<#!"
    }
}

/**
 * Lista do vocabulário restrito (TRD Motor de voz §Vocabulário restrito; spec 0003, D5, C5; OPR-020).
 *
 * Primeiro, cada palavra de `Token.spoken`, uma vez, na ordem do roteiro (a forma do TRD). Depois, as grafias
 * do léxico do modelo que normalizam para uma dessas palavras ou para uma parte de palavra com hífen do texto
 * ("três" para "tres", "bom" e "dia" para "bom-dia"): o léxico do `vosk-model-small-pt-0.3` tem a grafia com
 * acento e o Vosk ignora palavra fora do léxico. Por último, `[unk]`. Sem léxico, a lista é a do TRD.
 * Nada aqui sai da memória: a lista vai só ao reconhecedor.
 */
object Vocabulary {
    const val UNKNOWN = "[unk]"
    private val joiners = Regex("[-'’]")

    fun build(
        text: String,
        tokens: List<Token>,
        lexicon: Lexicon?,
    ): List<String> {
        val spoken = LinkedHashSet<String>()
        tokens.forEach { token -> token.spoken.forEach { form -> spoken.addAll(form) } }
        if (lexicon == null) return spoken.toList() + UNKNOWN
        val keys = LinkedHashSet<String>(spoken)
        tokens.forEach { token ->
            val shown = text.substring(token.display.first, token.display.last + 1)
            keys.addAll(Normalizer.words(shown.replace(joiners, " ")))
        }
        val extra = sortedSetOf<String>()
        keys.forEach { key -> lexicon.spellings(key).filterTo(extra) { it !in spoken } }
        return spoken.toList() + extra + UNKNOWN
    }
}
```

- [ ] **Passo 4: ver passar.** `VocabularyTest` 4 e `LexiconRealModelTest` 4 verdes. Se `lexicoDoModelo_temAGrafiaComAcento` falhar com "modelo ausente ou só ponteiro do LFS", rode `git lfs pull` (o arquivo no disco é o ponteiro).

- [ ] **Passo 5: commit.**

```bash
git add etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/TestFiles.kt \
  etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/VocabularyTest.kt \
  etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/LexiconRealModelTest.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/Vocabulary.kt
git commit -m "feat(speech): lista do vocabulário com a grafia do léxico do modelo (TRD §Vocabulário restrito; OPR-020)"
```

---

### Tarefa 5 (motor-voz): costuras do motor

Interfaces e tipos sem lógica; o teste delas é o uso nas Tarefas 8 a 10 (D1).

**Arquivos:** criar `KT/speech/AudioSource.kt`, `KT/speech/AssetTree.kt`, `KT/speech/Decoder.kt`, `KT/speech/RecognizerStrategy.kt`, `KT/speech/CaptureDriver.kt`.

- [ ] **Passo 1: código.** Criar `KT/speech/AudioSource.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

/**
 * Costura da captura (spec 0003, D1 e D8): o microfone ou um WAV de teste, 16 kHz, mono, PCM 16 bits.
 * Interna ao app da etapa0; não é contrato do TRD. Nada aqui grava áudio (CLAUDE.md, regra 3).
 */
interface AudioSource {
    /** Abre a fonte. Lança [AudioSourceException] se não abrir. */
    fun open()

    /**
     * Lê até `buffer.size` amostras e devolve quantas leu (0 = nada agora).
     * Lança [AudioSourceException] se a fonte se perder (ex.: permissão revogada).
     */
    fun read(buffer: ShortArray): Int

    /** Fecha a fonte; pode ser chamada mais de uma vez. */
    fun close()
}

class AudioSourceException(
    val error: EngineError,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
```

Criar `KT/speech/AssetTree.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import java.io.InputStream

/** Leitura dos assets empacotados (costura para a JVM; em Android, o `AssetManager`). */
interface AssetTree {
    /** Nomes dentro de `path`; lista vazia = arquivo ou caminho inexistente (como `AssetManager.list`). */
    fun list(path: String): List<String>

    /** Abre um arquivo; lança `IOException` se não existir. */
    fun open(path: String): InputStream
}
```

Criar `KT/speech/Decoder.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

/**
 * Costura do reconhecedor (spec 0003, D1): o laço de captura e sessões é testado na JVM com um
 * reconhecedor falso, sem carregar `org.vosk.*`. A implementação real fica em `VoskDecoders.kt`.
 */
interface Decoder {
    /** Entrega um bloco de áudio; `true` = fim de frase, e o resultado sai em [resultJson]. */
    fun accept(
        samples: ShortArray,
        count: Int,
    ): Boolean

    /** JSON do parcial, ex.: `{"partial" : "ouvir bem"}`. */
    fun partialJson(): String

    /** JSON do resultado da frase, ex.: `{"text" : "ouvir bem"}`. */
    fun resultJson(): String

    /** Zera a hipótese para reaproveitar o reconhecedor (estratégia [RecognizerStrategy.Reset]). */
    fun reset()

    /** Libera o reconhecedor. */
    fun close()
}

/** Cria reconhecedores a partir do modelo carregado. */
interface DecoderFactory {
    val info: ModelInfo

    /** `vocabulary` nulo = sem lista; senão, a lista inteira (já com `[unk]`). Lança exceção se não criar. */
    fun create(vocabulary: List<String>?): Decoder
}

/** Dados do modelo carregado, para o painel e o `meta.txt` (spec 0003, D11, D18). */
class ModelInfo(
    /** Conteúdo da primeira linha do arquivo `uuid` do modelo instalado (D12). */
    val id: String,
    val installMs: Long,
    val loadMs: Long,
    /** Léxico do modelo, lido só quando o vocabulário restrito é ligado (OPR-020). */
    val lexicon: Lazy<Lexicon?>,
)

/** Carrega o modelo. Lança [ModelLoadException] com `ModelMissing` ou `ModelCorrupt` (D15). */
fun interface ModelLoader {
    fun load(): DecoderFactory
}

class ModelLoadException(
    val error: EngineError,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * O modelo é carregado uma vez por processo (TRD Motor de voz §Instalação do modelo 4; spec 0003, C9).
 * Uma instância por processo é compartilhada por todos os [VoskEngine]. Falha não fica em cache:
 * a próxima chamada tenta de novo ("Tentar de novo", TRD §Instalação 3).
 */
class CachingModelLoader(
    private val delegate: ModelLoader,
) : ModelLoader {
    @Volatile
    var current: DecoderFactory? = null
        private set

    @Synchronized
    override fun load(): DecoderFactory = current ?: delegate.load().also { current = it }
}
```

Criar `KT/speech/RecognizerStrategy.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

/** Decisão em aberto do TRD (Motor de voz §Captura e sessões), medida na 0005 (spec 0003, D4). */
enum class RecognizerStrategy {
    /** Um reconhecedor novo por sessão (texto principal do TRD; padrão). */
    NewPerSession,

    /** O mesmo reconhecedor com `reset()` e `SessionId` novo; reconhecedor novo se o vocabulário mudar. */
    Reset,
}
```

Criar `KT/speech/CaptureDriver.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

/**
 * Quem roda o laço de captura (spec 0003, D1). Em produção, uma thread própria; nos testes, o teste chama
 * o passo à mão, o que torna determinísticas as corridas entre captura e `endSession`.
 */
interface CaptureDriver {
    /** Começa a chamar `step` até ele devolver `false`. */
    fun start(step: () -> Boolean)

    /** Pede para parar; volta depois que o passo em curso termina (exceto se chamado de dentro do passo). */
    fun stop()
}

/** Uma thread por captura, com o laço de `AudioRecord` do TRD (sem o serviço pronto do vosk-android). */
class ThreadCaptureDriver : CaptureDriver {
    @Volatile
    private var thread: Thread? = null

    override fun start(step: () -> Boolean) {
        val t =
            Thread({
                while (!Thread.currentThread().isInterrupted && step()) {
                    // o passo lê um bloco e o encaminha
                }
            }, "tribuna-captura")
        thread = t
        t.start()
    }

    override fun stop() {
        val t = thread ?: return
        thread = null
        if (t !== Thread.currentThread()) t.join(STOP_TIMEOUT_MS)
    }

    private companion object {
        /** Uma leitura bloqueia no máximo um bloco (0,2 s); o limite só evita travar se o aparelho falhar. */
        const val STOP_TIMEOUT_MS = 1_000L
    }
}
```

- [ ] **Passo 2: compilar.** `./gradlew :app:compileDebugKotlin ktlintCheck --console=plain` verde.

- [ ] **Passo 3: commit.**

```bash
git add etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/AudioSource.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/AssetTree.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/Decoder.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/RecognizerStrategy.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/CaptureDriver.kt
git commit -m "feat(speech): costuras de captura, reconhecedor e modelo para testar o motor na JVM (spec 0003 D1, D4)"
```

---

### Tarefa 6 (motor-voz): `AudioLevel` e `VoskJson` (C6, C7)

**Arquivos:** criar `KTT/speech/AudioLevelTest.kt`, `KTT/speech/VoskJsonTest.kt`, `KT/speech/AudioLevel.kt`, `KT/speech/VoskJson.kt`.

- [ ] **Passo 1: testes primeiro.** Criar `KTT/speech/AudioLevelTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertEquals
import org.junit.Test

/** C7: `Level.rms` = sqrt(média(s²)) / 32768 (spec 0003, D2). */
class AudioLevelTest {
    @Test
    fun blocoDeZeros_daZero() {
        assertEquals(0.0f, AudioLevel.rms(ShortArray(3200), 3200), 0.0f)
    }

    @Test
    fun ondaQuadradaEmFundoDeEscala_daUm() {
        val square = ShortArray(3200) { if (it % 2 == 0) 32767 else -32767 }
        assertEquals(1.0f, AudioLevel.rms(square, square.size), 1e-3f)
    }

    @Test
    fun meioDaEscala_daMeio() {
        assertEquals(0.5f, AudioLevel.rms(ShortArray(100) { 16384 }, 100), 1e-6f)
    }

    @Test
    fun soContaAsAmostrasLidas() {
        val samples = ShortArray(200) { if (it < 100) 16384 else 0 }
        assertEquals(0.5f, AudioLevel.rms(samples, 100), 1e-6f)
    }

    @Test
    fun contagemZero_daZero() {
        assertEquals(0.0f, AudioLevel.rms(ShortArray(10) { 1000 }, 0), 0.0f)
    }
}
```

Criar `KTT/speech/VoskJsonTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertEquals
import org.junit.Test

/** C6: JSON do Vosk → palavras, nos formatos da documentação do `org.vosk.Recognizer` 0.3.75. */
class VoskJsonTest {
    @Test
    fun parcial_viraPalavras() {
        assertEquals(listOf("ouvir", "bem"), VoskJson.words("{\"partial\" : \"ouvir bem\"}"))
    }

    @Test
    fun resultado_viraPalavras() {
        assertEquals(listOf("ouvir", "bem", "e", "uma"), VoskJson.words("{\"text\" : \"ouvir bem e uma\"}"))
    }

    @Test
    fun textoVazio_viraListaVazia() {
        assertEquals(emptyList<String>(), VoskJson.words("{\"partial\" : \"\"}"))
        assertEquals(emptyList<String>(), VoskJson.words("{\"text\" : \"\"}"))
        assertEquals(emptyList<String>(), VoskJson.words(""))
    }

    @Test
    fun jsonComQuebrasDeLinha_eAcentos() {
        assertEquals(listOf("ouvir", "é", "três"), VoskJson.words("{\n  \"partial\" : \"ouvir é três\"\n}"))
    }

    @Test
    fun escapesUnicode_saoDecodificados() {
        assertEquals(listOf("é", "três"), VoskJson.words("{\"text\" : \"\\u00e9 tr\\u00eas\"}"))
    }

    @Test
    fun resultadoComPalavrasComTempo_usaOTexto() {
        val json = "{\"result\" : [{\"conf\" : 1.0, \"word\" : \"ouvir\"}], \"text\" : \"ouvir\"}"
        assertEquals(listOf("ouvir"), VoskJson.words(json))
    }

    @Test
    fun gramatica_eUmArrayComUmaFrasePorPalavra() {
        assertEquals("[\"ouvir\",\"três\",\"[unk]\"]", VoskJson.grammar(listOf("ouvir", "três", "[unk]")))
    }

    @Test
    fun gramatica_escapaAspasEBarra() {
        assertEquals("[\"a\\\"b\",\"c\\\\d\"]", VoskJson.grammar(listOf("a\"b", "c\\d")))
    }
}
```

- [ ] **Passo 2: ver falhar.** FAIL de compilação ("Unresolved reference 'AudioLevel'", "'VoskJson'").

- [ ] **Passo 3: código.** Criar `KT/speech/AudioLevel.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import kotlin.math.sqrt

/** `Level.rms` = sqrt(média(s²)) / 32768, em [0, 1] (spec 0003, D2). */
object AudioLevel {
    fun rms(
        samples: ShortArray,
        count: Int,
    ): Float {
        if (count <= 0) return 0f
        var sum = 0.0
        for (i in 0 until count) {
            val s = samples[i].toDouble()
            sum += s * s
        }
        return (sqrt(sum / count) / 32768.0).toFloat()
    }
}
```

Criar `KT/speech/VoskJson.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

/**
 * JSON do Vosk ↔ palavras (TRD Motor de voz §Captura e sessões; spec 0003, C6).
 * Formatos da documentação do `org.vosk.Recognizer` 0.3.75: `{"partial" : "..."}` e `{"text" : "..."}`.
 * A confirmação contra o JSON real é da 0005 (Q4). Sem dependência de JSON: os testes JVM não têm `org.json`.
 */
object VoskJson {
    private val partialKey = Regex("\"partial\"\\s*:\\s*\"")
    private val textKey = Regex("\"text\"\\s*:\\s*\"")

    /** Palavras do parcial ou do resultado; JSON vazio, sem a chave ou com texto vazio = lista vazia. */
    fun words(json: String): List<String> {
        val text = stringAfter(json, partialKey) ?: stringAfter(json, textKey) ?: return emptyList()
        return text.split(' ').filter { it.isNotEmpty() }
    }

    /** Lista de palavras → gramática do Vosk: um array JSON com uma frase por palavra. */
    fun grammar(words: List<String>): String = words.joinToString(separator = ",", prefix = "[", postfix = "]") { quote(it) }

    private fun stringAfter(
        json: String,
        key: Regex,
    ): String? {
        val match = key.find(json) ?: return null
        val out = StringBuilder()
        var i = match.range.last + 1
        while (i < json.length) {
            val c = json[i]
            when {
                c == '"' -> {
                    return out.toString()
                }

                c == '\\' && i + 1 < json.length -> {
                    val next = json[i + 1]
                    when (next) {
                        'n' -> {
                            out.append('\n')
                        }

                        't' -> {
                            out.append('\t')
                        }

                        'r' -> {
                            out.append('\r')
                        }

                        'b' -> {
                            out.append('\b')
                        }

                        'f' -> {
                            out.append('\u000C')
                        }

                        'u' -> {
                            if (i + 5 >= json.length) return null
                            out.append(json.substring(i + 2, i + 6).toInt(16).toChar())
                            i += 4
                        }

                        else -> {
                            out.append(next)
                        }
                    }
                    i += 2
                    continue
                }

                else -> {
                    out.append(c)
                }
            }
            i++
        }
        return null
    }

    private fun quote(word: String): String =
        buildString {
            append('"')
            for (c in word) {
                when {
                    c == '"' -> append("\\\"")
                    c == '\\' -> append("\\\\")
                    c < ' ' -> append("\\u%04x".format(c.code))
                    else -> append(c)
                }
            }
            append('"')
        }
}
```

- [ ] **Passo 4: ver passar.** `AudioLevelTest` 5 e `VoskJsonTest` 8 verdes.

- [ ] **Passo 5: commit.**

```bash
git add etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/AudioLevelTest.kt \
  etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/VoskJsonTest.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/AudioLevel.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/VoskJson.kt
git commit -m "feat(speech): Level.rms normalizado e JSON do Vosk em palavras (spec 0003 D2; TRD §Captura e sessões)"
```

---

### Tarefa 7 (motor-voz): exportação (D6, D7, D8)

**Arquivos:** criar `KTT/export/TestTextCatalogTest.kt`, `KTT/export/ExportFilesTest.kt`, `KTT/export/ExportNameAndWriterTest.kt`, `KT/export/TestTextCatalog.kt`, `KT/export/Recording.kt`, `KT/export/ExportFiles.kt`, `KT/export/ExportWriter.kt`.

- [ ] **Passo 1: testes primeiro.** Criar `KTT/export/TestTextCatalogTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.export

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.speech.AssetTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/** D6: a exportação só existe com um texto de teste idêntico (spec 0003, D9; TRD §Privacidade nos testes). */
class TestTextCatalogTest {
    private val catalog = TestTextCatalog(TestFiles.textIds.associateWith(TestFiles::text))

    @Test
    fun cadaTextoDeTeste_eIdentificadoPeloSeuId() {
        assertEquals(11, TestFiles.textIds.size)
        for (id in TestFiles.textIds) assertEquals(id, catalog.identify(TestFiles.text(id)))
    }

    @Test
    fun umCaractereAlterado_naoIdentifica() {
        for (id in TestFiles.textIds) {
            val text = TestFiles.text(id)
            val altered = text.replaceFirst("a", "o")
            assertNull("$id alterado", catalog.identify(altered))
        }
    }

    @Test
    fun semAQuebraDeLinhaFinal_naoIdentifica() {
        assertNull(catalog.identify(TestFiles.text("T01").trimEnd()))
    }

    @Test
    fun textoQualquer_naoIdentifica() {
        assertNull(catalog.identify(""))
        assertNull(catalog.identify("Ouvir bem é uma habilidade"))
    }

    @Test
    fun catalogoDosAssets_leOsTxtDeTextos() {
        val assets =
            object : AssetTree {
                override fun list(path: String): List<String> =
                    if (path ==
                        "textos"
                    ) {
                        listOf("T01.txt", "T09.txt", "leia.md")
                    } else {
                        emptyList()
                    }

                override fun open(path: String): InputStream =
                    ByteArrayInputStream(TestFiles.text(path.removePrefix("textos/").removeSuffix(".txt")).toByteArray(Charsets.UTF_8))
            }
        val fromAssets = TestTextCatalog.fromAssets(assets)
        assertEquals(listOf("T01", "T09"), fromAssets.ids)
        assertEquals("T09", fromAssets.identify(TestFiles.text("T09")))
    }
}
```

Criar `KTT/export/ExportFilesTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.export

import io.github.gigioderico.tribuna.etapa0.align.Move
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.SessionId
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEvent
import org.junit.Assert.assertEquals
import org.junit.Test

/** D7 e D11: formato dos arquivos (cabeçalhos de D7 e de TESTES-ETAPA0) e leitura de volta das hipóteses. */
class ExportFilesTest {
    private fun recording(): Recording = Recording("T01", meta = mapOf("text_id" to "T01"), originMs = 1_000L)

    @Test
    fun cabecalhos_saoOsDeD7EDeTestesEtapa0() {
        val files = ExportFiles.build(recording())
        assertEquals(listOf("hypotheses.csv", "level.csv", "trajectory.csv", "events.csv", "meta.txt"), files.keys.toList())
        assertEquals("t_ms,session,kind,words,accepted", files.getValue("hypotheses.csv").lines().first())
        assertEquals("t_ms,rms", files.getValue("level.csv").lines().first())
        assertEquals("t_ms,cursor,move,score", files.getValue("trajectory.csv").lines().first())
        assertEquals("t_ms,evento,detalhe", files.getValue("events.csv").lines().first())
    }

    @Test
    fun hipoteses_umaLinhaPorEvento_comPalavrasEntreAspas() {
        val rec = recording()
        rec.hypothesis(1_120L, SpeechEvent.Partial(SessionId(1), listOf("ouvir", "bem")), accepted = true)
        rec.hypothesis(1_300L, SpeechEvent.Partial(SessionId(1), emptyList()), accepted = true)
        rec.hypothesis(1_450L, SpeechEvent.Final(SessionId(1), listOf("ouvir", "bem", "é")), accepted = false)
        rec.hypothesis(1_500L, SpeechEvent.Error(null, EngineError.MicUnavailable), accepted = true)
        rec.hypothesis(1_600L, SpeechEvent.Level(0.5f), accepted = true)
        assertEquals(
            listOf(
                "t_ms,session,kind,words,accepted",
                "120,1,partial,\"ouvir bem\",1",
                "300,1,partial,\"\",1",
                "450,1,final,\"ouvir bem é\",0",
                "500,,error,\"MicUnavailable\",1",
                "",
            ),
            ExportFiles.hypotheses(rec.hypotheses).lines(),
        )
    }

    @Test
    fun trajetoria_nivelEEventos() {
        val rec = recording()
        rec.trajectory(1_000L, 0, Move.Reposition, 0.0)
        rec.trajectory(2_250L, 3, Move.Short, 0.75)
        rec.level(1_200L, 0.25f)
        rec.event(3_000L, Recording.PAUSE_TAP)
        rec.event(3_500L, Recording.WORD_TAP, "12")
        assertEquals("t_ms,cursor,move,score\n0,0,Reposition,0.0\n1250,3,Short,0.75\n", ExportFiles.trajectory(rec.trajectory))
        assertEquals("t_ms,rms\n200,0.250000\n", ExportFiles.level(rec.levels))
        assertEquals("t_ms,evento,detalhe\n2000,pausa_toque,\n2500,toque_palavra,12\n", ExportFiles.events(rec.events))
    }

    @Test
    fun meta_trazTodasAsChavesDeD11_naOrdem() {
        val meta = ExportFiles.meta(mapOf("model_id" to "vosk-model-small-pt-0.3 sha256=ab", "preset" to "Normal"))
        val keys = meta.lines().filter { it.isNotEmpty() }.map { it.substringBefore('=') }
        assertEquals(ExportFiles.META_KEYS, keys)
        assertEquals(15, keys.size)
        assertEquals(true, meta.contains("model_id=vosk-model-small-pt-0.3 sha256=ab\n"))
    }

    @Test
    fun hipoteses_lidasDeVolta_saoIguaisAsGravadas() {
        val rec = recording()
        rec.hypothesis(1_100L, SpeechEvent.Partial(SessionId(1), listOf("ouvir")), accepted = true)
        rec.hypothesis(1_200L, SpeechEvent.Final(SessionId(2), listOf("diz", "\"isso\"")), accepted = false)
        rec.hypothesis(1_300L, SpeechEvent.Error(null, EngineError.PermissionRevoked), accepted = true)
        assertEquals(rec.hypotheses, ExportFiles.readHypotheses(ExportFiles.hypotheses(rec.hypotheses)))
    }

    @Test
    fun reprocessar_soAsAceitas_zerandoAFraseACadaSessao() {
        val rows =
            listOf(
                HypothesisRow(0, 1, "partial", listOf("ouvir"), true),
                HypothesisRow(1, 1, "partial", listOf("ouvir", "bem"), true),
                HypothesisRow(2, 1, "partial", listOf("ouvir", "bem", "é"), false),
                HypothesisRow(3, 2, "partial", listOf("uma"), true),
                HypothesisRow(4, 1, "final", listOf("ouvir", "bem", "é", "uma"), false),
                HypothesisRow(5, 2, "final", listOf("uma", "habilidade"), true),
                HypothesisRow(6, null, "error", listOf("MicUnavailable"), true),
            )
        assertEquals(listOf("ouvir", "uma", "habilidade"), ExportFiles.replayAccepted(rows))
    }
}
```

Criar `KTT/export/ExportNameAndWriterTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/** Nome da exportação sem texto livre (Q6) e gravação dos arquivos (D7). */
class ExportNameAndWriterTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun nomeValido_eOIdDoTextoComSufixoOpcional() {
        assertTrue(ExportName.isValid("T01", "T01"))
        assertTrue(ExportName.isValid("T02_giorgio_salao_t2", "T02"))
        assertTrue(ExportName.isValid("L30_voz2_sala_t1", "L30"))
    }

    @Test
    fun nomeInvalido_outroTexto_textoLivre_ouCaminho() {
        assertFalse(ExportName.isValid("T02", "T01"))
        assertFalse(ExportName.isValid("T01 ouvir bem", "T01"))
        assertFalse(ExportName.isValid("T01_Sala", "T01"))
        assertFalse(ExportName.isValid("T01_../x", "T01"))
        assertFalse(ExportName.isValid("", "T01"))
        assertFalse(ExportName.isValid("T01_", "T01"))
    }

    @Test
    fun writer_gravaUmArquivoPorSufixo_emUtf8() {
        val dir = File(tmp.root, "exportacoes")
        val written = ExportWriter { dir }.write("T01_t1", linkedMapOf("trajectory.csv" to "t_ms\n", "meta.txt" to "model_id=três\n"))
        assertEquals(listOf("T01_t1.trajectory.csv", "T01_t1.meta.txt"), written)
        assertEquals("model_id=três\n", File(dir, "T01_t1.meta.txt").readText(Charsets.UTF_8))
    }

    @Test
    fun writer_semDiretorio_falhaComIOException() {
        val error = runCatching { ExportWriter { null }.write("T01", mapOf("meta.txt" to "")) }.exceptionOrNull()
        assertTrue(error is IOException)
    }
}
```

- [ ] **Passo 2: ver falhar.** FAIL de compilação ("Unresolved reference 'TestTextCatalog'", "'Recording'", "'ExportFiles'", "'ExportName'").

- [ ] **Passo 3: código.** Criar `KT/export/TestTextCatalog.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.export

import io.github.gigioderico.tribuna.etapa0.speech.AssetTree

/**
 * Textos de teste de `etapa0/textos/`, empacotados em `assets/textos/` (spec 0003, D9). A exportação só fica
 * habilitada quando o texto atual é idêntico a um deles: um discurso digitado nunca vira arquivo
 * (TRD §Privacidade nos testes; CLAUDE.md, regras 2 e 3).
 */
class TestTextCatalog(
    private val texts: Map<String, String>,
) {
    /** IDs em ordem (L30, T01…T10). */
    val ids: List<String> = texts.keys.sorted()

    fun text(id: String): String? = texts[id]

    /** ID do texto de teste idêntico a `text`, ou `null`. */
    fun identify(text: String): String? = texts.entries.firstOrNull { it.value == text }?.key

    companion object {
        const val ASSET_DIR = "textos"

        fun fromAssets(assets: AssetTree): TestTextCatalog =
            TestTextCatalog(
                assets
                    .list(ASSET_DIR)
                    .filter { it.endsWith(".txt") }
                    .associate { name ->
                        name.removeSuffix(".txt") to assets.open("$ASSET_DIR/$name").use { it.readBytes().toString(Charsets.UTF_8) }
                    },
            )
    }
}
```

Criar `KT/export/Recording.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.export

import io.github.gigioderico.tribuna.etapa0.align.Move
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEvent

/** Uma linha de `hypotheses.csv` (spec 0003, D7). `session` nulo só em erro da captura. */
data class HypothesisRow(
    val tMs: Long,
    val session: Long?,
    val kind: String,
    val words: List<String>,
    val accepted: Boolean,
)

data class LevelRow(
    val tMs: Long,
    val rms: Float,
)

data class TrajectoryRow(
    val tMs: Long,
    val cursor: Int,
    val move: Move,
    val score: Double,
)

data class EventRow(
    val tMs: Long,
    val event: String,
    val detail: String,
)

/**
 * Uma gravação em memória: só existe quando o texto atual é um texto de teste (D9), do `startCapture` até a
 * próxima leitura. `t_ms` = ms desde o início da gravação, por relógio monotônico (D6). Nada sai daqui sem
 * [ExportFiles] e o botão Exportar.
 */
class Recording(
    val textId: String,
    /** Ajustes e ambiente no início da gravação, para o `meta.txt` (D11, Q8). */
    val meta: Map<String, String>,
    private val originMs: Long,
) {
    val hypotheses = mutableListOf<HypothesisRow>()
    val levels = mutableListOf<LevelRow>()
    val trajectory = mutableListOf<TrajectoryRow>()
    val events = mutableListOf<EventRow>()

    fun elapsed(nowMs: Long): Long = nowMs - originMs

    fun hypothesis(
        nowMs: Long,
        event: SpeechEvent,
        accepted: Boolean,
    ) {
        val row =
            when (event) {
                is SpeechEvent.Partial -> HypothesisRow(elapsed(nowMs), event.session.value, KIND_PARTIAL, event.words, accepted)
                is SpeechEvent.Final -> HypothesisRow(elapsed(nowMs), event.session.value, KIND_FINAL, event.words, accepted)
                is SpeechEvent.Error -> HypothesisRow(elapsed(nowMs), event.session?.value, KIND_ERROR, listOf(event.cause.name), accepted)
                is SpeechEvent.Level -> return
            }
        hypotheses += row
    }

    fun level(
        nowMs: Long,
        rms: Float,
    ) {
        levels += LevelRow(elapsed(nowMs), rms)
    }

    fun trajectory(
        nowMs: Long,
        cursor: Int,
        move: Move,
        score: Double,
    ) {
        trajectory += TrajectoryRow(elapsed(nowMs), cursor, move, score)
    }

    fun event(
        nowMs: Long,
        event: String,
        detail: String = "",
    ) {
        events += EventRow(elapsed(nowMs), event, detail)
    }

    companion object {
        const val KIND_PARTIAL = "partial"
        const val KIND_FINAL = "final"
        const val KIND_ERROR = "error"
        const val PAUSE_TAP = "pausa_toque"
        const val RESUME_TAP = "retoma_toque"
        const val WORD_TAP = "toque_palavra"
    }
}
```

Criar `KT/export/ExportFiles.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.export

import io.github.gigioderico.tribuna.etapa0.align.PartialReconciler
import java.util.Locale

/**
 * Conteúdo dos cinco arquivos da exportação (spec 0003, D7, D11; TESTES-ETAPA0). Consumidores: `metricas.py`
 * (trajetória e eventos), calibração do limiar na 0005 (nível), golden da 0006 (hipóteses) e relatório (meta).
 */
object ExportFiles {
    const val HYPOTHESES_HEADER = "t_ms,session,kind,words,accepted"
    const val LEVEL_HEADER = "t_ms,rms"
    const val TRAJECTORY_HEADER = "t_ms,cursor,move,score"
    const val EVENTS_HEADER = "t_ms,evento,detalhe"

    /** Chaves do `meta.txt`, na ordem (D11). */
    val META_KEYS =
        listOf(
            "app_version",
            "vosk_android_version",
            "model_id",
            "text_id",
            "preset",
            "vocabulary",
            "recognizer_strategy",
            "audio_source",
            "wav_name",
            "level_threshold",
            "animation_ms",
            "install_ms",
            "prepare_ms",
            "device_model",
            "android_version",
        )

    /** Sufixo do arquivo → conteúdo. O nome final é `<nome>.<sufixo>`. */
    fun build(recording: Recording): Map<String, String> =
        linkedMapOf(
            "hypotheses.csv" to hypotheses(recording.hypotheses),
            "level.csv" to level(recording.levels),
            "trajectory.csv" to trajectory(recording.trajectory),
            "events.csv" to events(recording.events),
            "meta.txt" to meta(recording.meta),
        )

    fun hypotheses(rows: List<HypothesisRow>): String =
        lines(HYPOTHESES_HEADER, rows) { row ->
            "${row.tMs},${row.session ?: ""},${row.kind},${quote(row.words.joinToString(" "))},${if (row.accepted) 1 else 0}"
        }

    fun level(rows: List<LevelRow>): String = lines(LEVEL_HEADER, rows) { "${it.tMs},${String.format(Locale.ROOT, "%.6f", it.rms)}" }

    fun trajectory(rows: List<TrajectoryRow>): String =
        lines(TRAJECTORY_HEADER, rows) { "${it.tMs},${it.cursor},${it.move.name},${it.score}" }

    fun events(rows: List<EventRow>): String = lines(EVENTS_HEADER, rows) { "${it.tMs},${it.event},${it.detail}" }

    fun meta(values: Map<String, String>): String =
        META_KEYS.joinToString(separator = "\n", postfix = "\n") { "$it=${values[it].orEmpty()}" }

    /** Lê `hypotheses.csv` de volta (golden da 0006; ida e volta de D7). */
    fun readHypotheses(csv: String): List<HypothesisRow> {
        val lines = csv.lines().filter { it.isNotEmpty() }
        require(lines.firstOrNull() == HYPOTHESES_HEADER) { "cabeçalho inesperado: ${lines.firstOrNull()}" }
        return lines.drop(1).map(::parseHypothesis)
    }

    /**
     * Reprocessa as linhas com `accepted = 1` pelo [PartialReconciler], zerando a frase a cada sessão nova,
     * como a tela faz ao vivo; devolve as palavras entregues, na ordem.
     */
    fun replayAccepted(rows: List<HypothesisRow>): List<String> {
        val reconciler = PartialReconciler()
        val out = mutableListOf<String>()
        var session: Long? = null
        for (row in rows) {
            if (!row.accepted || row.kind == Recording.KIND_ERROR) continue
            if (row.session != session) {
                reconciler.reset()
                session = row.session
            }
            out +=
                when (row.kind) {
                    Recording.KIND_PARTIAL -> reconciler.onPartial(row.words)
                    else -> reconciler.onFinal(row.words)
                }
        }
        return out
    }

    private fun <T> lines(
        header: String,
        rows: List<T>,
        format: (T) -> String,
    ): String = (listOf(header) + rows.map(format)).joinToString(separator = "\n", postfix = "\n")

    private fun quote(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    private fun parseHypothesis(line: String): HypothesisRow {
        val firstQuote = line.indexOf('"')
        val lastQuote = line.lastIndexOf('"')
        require(firstQuote in 0 until lastQuote) { "linha sem palavras entre aspas: $line" }
        val head = line.substring(0, firstQuote).split(',')
        val tail = line.substring(lastQuote + 1).removePrefix(",")
        val words = line.substring(firstQuote + 1, lastQuote).replace("\"\"", "\"")
        return HypothesisRow(
            tMs = head[0].toLong(),
            session = head[1].takeIf { it.isNotEmpty() }?.toLong(),
            kind = head[2],
            words = words.split(' ').filter { it.isNotEmpty() },
            accepted = tail == "1",
        )
    }
}
```

Criar `KT/export/ExportWriter.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.export

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Onde a exportação é gravada (costura para a JVM). */
fun interface ExportSink {
    /** Grava `<nome>.<sufixo>` para cada arquivo e devolve os nomes gravados. */
    fun write(
        name: String,
        files: Map<String, String>,
    ): List<String>
}

/** Nome da exportação: o ID do texto de teste e um sufixo opcional `[a-z0-9_]` (nada do discurso no nome; Q6). */
object ExportName {
    fun isValid(
        name: String,
        textId: String,
    ): Boolean = Regex("^" + Regex.escape(textId) + "(_[a-z0-9]+)*$").matches(name)
}

/**
 * Grava a exportação no diretório externo do app (sem permissão; `adb pull`), só arquivos de texto da gravação
 * de um texto de teste (spec 0003, D7, D9). Única escrita de arquivo do app além da cópia do modelo; nunca
 * escreve áudio (CLAUDE.md, regra 3).
 */
class ExportWriter(
    private val directory: () -> File?,
) : ExportSink {
    override fun write(
        name: String,
        files: Map<String, String>,
    ): List<String> {
        val dir = directory() ?: throw IOException("armazenamento externo do app indisponível")
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("não foi possível criar ${dir.name}")
        return files.map { (suffix, content) ->
            val file = File(dir, "$name.$suffix")
            FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            file.name
        }
    }
}
```

- [ ] **Passo 4: ver passar.** `TestTextCatalogTest` 5, `ExportFilesTest` 6, `ExportNameAndWriterTest` 4 verdes.

- [ ] **Passo 5: commit e aviso.**

```bash
git add etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/export \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/export
git commit -m "feat(export): exportação só com texto de teste, CSVs de D7 e meta.txt (TRD Etapa 0 item 5; §Privacidade nos testes)"
```

Ao terminar este commit, o `motor-voz` avisa o orquestrador: o `interface` pode começar a Tarefa 12.

---

### Tarefa 8 (motor-voz): `VoskEngine` (C1–C5, C8, C9)

**Arquivos:** criar `KTT/speech/SpeechFakes.kt`, `KTT/speech/EngineHarness.kt`, `KTT/speech/VoskEngineSessionTest.kt`, `KTT/speech/VoskEngineStateTest.kt`, `KT/speech/VoskEngine.kt`.

- [ ] **Passo 1: falsos e testes primeiro.** Criar `KTT/speech/SpeechFakes.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

/** Reconhecedor falso: guarda o que recebeu e devolve o JSON combinado. Nunca carrega `org.vosk.*` (D1). */
class FakeDecoder(
    val vocabulary: List<String>?,
) : Decoder {
    val fed = mutableListOf<ShortArray>()
    var partial = "{\"partial\" : \"\"}"
    var result = "{\"text\" : \"\"}"
    var endOfUtterance = false
    var failOnAccept = false
    var onAccept: (() -> Unit)? = null
    var resets = 0
    var resultCalls = 0
    var closed = false

    override fun accept(
        samples: ShortArray,
        count: Int,
    ): Boolean {
        check(!closed) { "reconhecedor liberado recebeu áudio" }
        fed += samples.copyOf(count)
        onAccept?.invoke()
        if (failOnAccept) throw IllegalStateException("falha simulada do reconhecedor")
        return endOfUtterance
    }

    override fun partialJson(): String = partial

    override fun resultJson(): String {
        resultCalls++
        return result
    }

    override fun reset() {
        resets++
    }

    override fun close() {
        closed = true
    }
}

class FakeDecoderFactory(
    override val info: ModelInfo = ModelInfo(id = "modelo-falso", installMs = 1, loadMs = 2, lexicon = lazyOf(null)),
) : DecoderFactory {
    val created = mutableListOf<FakeDecoder>()

    override fun create(vocabulary: List<String>?): Decoder = FakeDecoder(vocabulary).also { created += it }
}

/** Fonte de áudio roteirizada: cada `push` é um bloco; sem bloco, `read` devolve 0. */
class FakeAudioSource(
    private val openError: EngineError? = null,
) : AudioSource {
    private val blocks = ArrayDeque<ShortArray>()
    var opened = 0
    var closed = 0
    var readError: EngineError? = null
    var onRead: (() -> Unit)? = null

    fun push(block: ShortArray) {
        blocks.addLast(block)
    }

    override fun open() {
        openError?.let { throw AudioSourceException(it, "falha simulada ao abrir") }
        opened++
    }

    override fun read(buffer: ShortArray): Int {
        readError?.let { throw AudioSourceException(it, "falha simulada na leitura") }
        val block = blocks.removeFirstOrNull() ?: return 0
        block.copyInto(buffer)
        onRead?.invoke()
        return block.size
    }

    override fun close() {
        closed++
    }
}

/** O teste chama o passo do laço à mão: corridas entre captura e sessão ficam determinísticas. */
class ManualCaptureDriver : CaptureDriver {
    private var step: (() -> Boolean)? = null
    var stops = 0

    override fun start(step: () -> Boolean) {
        this.step = step
    }

    override fun stop() {
        stops++
    }

    /** Roda `times` passos; devolve `false` se o laço terminou. */
    fun pump(times: Int = 1): Boolean {
        repeat(times) {
            val current = step ?: return false
            if (!current()) {
                step = null
                return false
            }
        }
        return true
    }
}

fun block(
    value: Int,
    size: Int = 160,
): ShortArray = ShortArray(size) { value.toShort() }
```

Criar `KTT/speech/EngineHarness.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** Monta um VoskEngine com fonte, reconhecedor e laço falsos, e coleta os eventos (spec 0003, D1). */
class EngineHarness(
    scope: TestScope,
    strategy: RecognizerStrategy = RecognizerStrategy.NewPerSession,
    val source: FakeAudioSource = FakeAudioSource(),
    val factory: FakeDecoderFactory = FakeDecoderFactory(),
    loader: ModelLoader = ModelLoader { factory },
) {
    val driver = ManualCaptureDriver()
    val engine =
        VoskEngine(
            modelLoader = loader,
            audioSource = { source },
            strategy = strategy,
            captureDriver = driver,
            loadDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
        )
    val events = mutableListOf<SpeechEvent>()

    init {
        scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { engine.events.collect { events += it } }
    }

    /** prepare + startCapture. */
    suspend fun open() {
        engine.prepare().getOrThrow()
        engine.startCapture()
    }

    /** Entrega um bloco à fonte e roda um passo do laço. */
    fun feed(block: ShortArray = block(1000)) {
        source.push(block)
        driver.pump()
    }

    fun recognitionEvents(): List<SpeechEvent> = events.filterNot { it is SpeechEvent.Level }
}
```

Criar `KTT/speech/VoskEngineSessionTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras do contrato do SpeechEngine no VoskEngine (TRD §Contratos; Motor de voz §Captura e sessões):
 * C1 sessão presa ao reconhecedor, C2 fora de sessão, C3 endSession, C4 estratégias, C5 vocabulário.
 */
class VoskEngineSessionTest {
    @Test
    fun todoEventoDoReconhecedor_levaOIdDele() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            val id = h.engine.beginSession()
            val decoder = h.factory.created.single()
            decoder.partial = "{\"partial\" : \"ouvir bem\"}"
            h.feed()
            decoder.endOfUtterance = true
            decoder.result = "{\"text\" : \"ouvir bem e uma\"}"
            h.feed()
            decoder.failOnAccept = true
            h.feed()
            assertEquals(
                listOf(
                    SpeechEvent.Partial(id, listOf("ouvir", "bem")),
                    SpeechEvent.Final(id, listOf("ouvir", "bem", "e", "uma")),
                    SpeechEvent.Error(id, EngineError.Internal),
                ),
                h.recognitionEvents(),
            )
        }

    @Test
    fun resultadoProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo() =
        runTest {
            // C1 (Q2): o reconhecedor antigo ainda processa o bloco quando a sessão troca; o evento sai com o id
            // dele. Se o código ler a sessão atual do motor na hora de emitir, sai o id novo e o teste falha.
            val h = EngineHarness(this)
            h.open()
            val first = h.engine.beginSession()
            val old = h.factory.created.single()
            old.partial = "{\"partial\" : \"ouvir bem\"}"
            var second: SessionId? = null
            old.onAccept = {
                old.onAccept = null
                h.engine.endSession()
                second = h.engine.beginSession()
            }
            h.feed()
            assertEquals(listOf(SpeechEvent.Partial(first, listOf("ouvir", "bem"))), h.recognitionEvents())
            assertNotEquals(first, second)
        }

    @Test
    fun reconhecedorAntigoSoELiberadoDepoisDeTerminarOBloco() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            val old = h.factory.created.single()
            var closedDuringAccept = true
            old.onAccept = {
                old.onAccept = null
                h.engine.endSession()
                closedDuringAccept = old.closed
            }
            h.feed()
            assertFalse(closedDuringAccept)
            assertTrue(old.closed)
        }

    @Test
    fun foraDeSessao_nenhumBlocoChegaAoReconhecedor_eLevelContinua() =
        runTest {
            // C2: com a captura aberta e sem sessão, um Level por bloco lido e nenhum bloco a reconhecedor nenhum.
            val h = EngineHarness(this)
            h.open()
            repeat(3) { h.feed(block(1000 + it)) }
            assertEquals(3, h.events.filterIsInstance<SpeechEvent.Level>().size)
            assertTrue(h.factory.created.isEmpty())

            h.engine.beginSession()
            h.feed()
            h.engine.endSession()
            val decoder = h.factory.created.single()
            repeat(2) { h.feed(block(7)) }
            assertEquals(1, decoder.fed.size)
            assertEquals(6, h.events.filterIsInstance<SpeechEvent.Level>().size)
        }

    @Test
    fun blocoLidoAntesDoEndSessionENaoEntregue_naoChegaAoReconhecedor() =
        runTest {
            // C3: o endSession acontece entre a leitura do bloco e a entrega.
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            val decoder = h.factory.created.single()
            h.source.onRead = {
                h.source.onRead = null
                h.engine.endSession()
            }
            h.feed(block(42))
            assertTrue(decoder.fed.isEmpty())
            assertTrue(decoder.closed)
        }

    @Test
    fun endSession_naoEmiteFinal_eNaoPedeOResultado() =
        runTest {
            // C3 e D14: a hipótese pendente é descartada.
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            val decoder = h.factory.created.single()
            decoder.partial = "{\"partial\" : \"ouvir bem\"}"
            decoder.result = "{\"text\" : \"ouvir bem\"}"
            h.feed()
            h.engine.endSession()
            h.feed()
            assertEquals(0, decoder.resultCalls)
            assertTrue(h.recognitionEvents().none { it is SpeechEvent.Final })
            assertTrue(decoder.closed)
        }

    @Test
    fun estrategiaReset_endSessionZeraEMantemOReconhecedor() =
        runTest {
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            h.engine.beginSession()
            val decoder = h.factory.created.single()
            h.feed()
            h.engine.endSession()
            assertEquals(1, decoder.resets)
            assertFalse(decoder.closed)
        }

    @Test
    fun estrategiaNova_cadaSessaoCriaUmReconhecedor_comIdsCrescentes() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            val ids =
                List(3) {
                    h.engine.beginSession().also { h.engine.endSession() }
                }
            assertEquals(3, h.factory.created.size)
            assertTrue(h.factory.created.all { it.closed })
            assertEquals(ids.sortedBy { it.value }, ids)
            assertEquals(3, ids.map { it.value }.distinct().size)
        }

    @Test
    fun estrategiaReset_reaproveitaOReconhecedor_comIdNovo() =
        runTest {
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            val first = h.engine.beginSession(listOf("ouvir", "[unk]"))
            h.engine.endSession()
            val second = h.engine.beginSession(listOf("ouvir", "[unk]"))
            assertEquals(1, h.factory.created.size)
            assertTrue(second.value > first.value)
            val decoder = h.factory.created.single()
            decoder.partial = "{\"partial\" : \"ouvir\"}"
            h.feed()
            assertEquals(listOf(SpeechEvent.Partial(second, listOf("ouvir"))), h.recognitionEvents())
        }

    @Test
    fun estrategiaReset_vocabularioMudou_criaReconhecedorNovo() =
        runTest {
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            h.engine.beginSession(listOf("ouvir", "[unk]"))
            h.engine.endSession()
            h.engine.beginSession(null)
            assertEquals(2, h.factory.created.size)
            assertTrue(h.factory.created[0].closed)
            assertEquals(null, h.factory.created[1].vocabulary)
        }

    @Test
    fun vocabulario_vaiParaOReconhecedorCriado_esemListaSemVocabulario() =
        runTest {
            // C5: beginSession(vocabulary) passa a lista; desligado, o reconhecedor é criado sem lista.
            val h = EngineHarness(this)
            h.open()
            val words = listOf("ouvir", "bem", "[unk]")
            h.engine.beginSession(words)
            h.engine.endSession()
            h.engine.beginSession()
            assertEquals(words, h.factory.created[0].vocabulary)
            assertEquals(null, h.factory.created[1].vocabulary)
        }

    @Test
    fun reconhecedorQueNaoCria_emiteErroInternoDaSessao() =
        runTest {
            val failing =
                object : DecoderFactory {
                    override val info = FakeDecoderFactory().info

                    override fun create(vocabulary: List<String>?): Decoder = throw java.io.IOException("Failed to create a recognizer")
                }
            val h = EngineHarness(this, loader = ModelLoader { failing })
            h.open()
            val id = h.engine.beginSession()
            assertEquals(listOf(SpeechEvent.Error(id, EngineError.Internal)), h.recognitionEvents())
        }

    @Test
    fun finalProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            val first = h.engine.beginSession()
            val old = h.factory.created.single()
            old.endOfUtterance = true
            old.result = "{\"text\" : \"ouvir bem\"}"
            old.onAccept = {
                old.onAccept = null
                h.engine.endSession()
                h.engine.beginSession()
            }
            h.feed()
            assertEquals(listOf(SpeechEvent.Final(first, listOf("ouvir", "bem"))), h.recognitionEvents())
        }

    @Test
    fun reconhecedorComExcecao_naoRecebeMaisAudio() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            val id = h.engine.beginSession()
            val decoder = h.factory.created.single()
            decoder.failOnAccept = true
            h.feed()
            h.feed()
            assertEquals(1, decoder.fed.size)
            assertEquals(listOf(SpeechEvent.Error(id, EngineError.Internal)), h.recognitionEvents())
            assertTrue(decoder.closed)
        }

    @Test
    fun stopCaptureDuranteALeitura_descartaOBloco() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.source.onRead = {
                h.source.onRead = null
                h.engine.stopCapture()
            }
            h.feed()
            assertTrue(h.events.isEmpty())
            assertEquals(1, h.source.closed)
        }

    @Test
    fun estrategiaReset_sessaoAntigaAtrasada_guardaSoOReconhecedorMaisNovo() =
        runTest {
            // A sessão A termina durante um bloco (liberação adiada); B cria outro reconhecedor e termina antes.
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            h.engine.beginSession()
            val first = h.factory.created.single()
            first.onAccept = {
                first.onAccept = null
                h.engine.endSession()
                h.engine.beginSession()
                h.engine.endSession()
            }
            h.feed()
            val second = h.factory.created[1]
            assertTrue(first.closed)
            assertFalse(second.closed)
            h.engine.beginSession()
            h.feed()
            assertEquals(2, h.factory.created.size)
            assertEquals(1, second.fed.size)
        }

    @Test
    fun releaseDuranteUmBloco_fechaOReconhecedorQuandoOBlocoTermina() =
        runTest {
            val h = EngineHarness(this, strategy = RecognizerStrategy.Reset)
            h.open()
            h.engine.beginSession()
            val decoder = h.factory.created.single()
            decoder.onAccept = {
                decoder.onAccept = null
                h.engine.release()
            }
            h.feed()
            assertTrue(decoder.closed)
        }
}
```

Criar `KTT/speech/VoskEngineStateTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** C8 estados e erros (D15) e C9 modelo carregado uma vez (TRD §Instalação do modelo 4). */
class VoskEngineStateTest {
    @Test
    fun sequenciaDeEstados() =
        runTest {
            val seen = mutableListOf<EngineState>()
            lateinit var h: EngineHarness
            val factory = FakeDecoderFactory()
            h =
                EngineHarness(
                    this,
                    factory = factory,
                    loader =
                        ModelLoader {
                            seen += h.engine.state.value
                            factory
                        },
                )
            seen += h.engine.state.value
            h.engine.prepare()
            seen += h.engine.state.value
            h.engine.startCapture()
            seen += h.engine.state.value
            h.engine.beginSession()
            seen += h.engine.state.value
            h.engine.endSession()
            seen += h.engine.state.value
            h.engine.stopCapture()
            seen += h.engine.state.value
            assertEquals(
                listOf(
                    EngineState.Idle,
                    EngineState.Preparing,
                    EngineState.Ready,
                    EngineState.Capturing,
                    EngineState.Recognizing,
                    EngineState.Capturing,
                    EngineState.Ready,
                ),
                seen,
            )
        }

    @Test
    fun falhaAoAbrirAFonte_geraMicUnavailableSemSessao() =
        runTest {
            val h = EngineHarness(this, source = FakeAudioSource(openError = EngineError.MicUnavailable))
            h.engine.prepare()
            h.engine.startCapture()
            assertEquals(listOf(SpeechEvent.Error(null, EngineError.MicUnavailable)), h.events)
            assertEquals(EngineState.Ready, h.engine.state.value)
        }

    @Test
    fun semPermissao_geraPermissionRevokedSemSessao() =
        runTest {
            val h = EngineHarness(this, source = FakeAudioSource(openError = EngineError.PermissionRevoked))
            h.engine.prepare()
            h.engine.startCapture()
            assertEquals(listOf(SpeechEvent.Error(null, EngineError.PermissionRevoked)), h.events)
        }

    @Test
    fun permissaoPerdidaDuranteACaptura_geraPermissionRevoked_eFechaACaptura() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            h.source.readError = EngineError.PermissionRevoked
            h.driver.pump()
            assertEquals(listOf(SpeechEvent.Error(null, EngineError.PermissionRevoked)), h.recognitionEvents())
            assertEquals(EngineState.Ready, h.engine.state.value)
            assertTrue(
                h.factory.created
                    .single()
                    .closed,
            )
            assertEquals(1, h.source.closed)
        }

    @Test
    fun excecaoDoReconhecedor_geraErroInternoDaSessao() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            val id = h.engine.beginSession()
            h.factory.created
                .single()
                .failOnAccept = true
            h.feed()
            assertEquals(listOf(SpeechEvent.Error(id, EngineError.Internal)), h.recognitionEvents())
        }

    @Test
    fun modeloAusente_prepareFalha_comFailedEModelMissing() =
        runTest {
            val h = EngineHarness(this, loader = ModelLoader { throw ModelLoadException(EngineError.ModelMissing, "ausente") })
            val result = h.engine.prepare()
            assertTrue(result.isFailure)
            assertEquals(EngineError.ModelMissing, (result.exceptionOrNull() as ModelLoadException).error)
            assertEquals(EngineState.Failed, h.engine.state.value)
        }

    @Test
    fun stopCapture_paraOLaco_eFechaAFonte() =
        runTest {
            val h = EngineHarness(this)
            h.open()
            h.engine.beginSession()
            h.engine.stopCapture()
            assertEquals(false, h.driver.pump())
            assertEquals(1, h.source.closed)
            assertEquals(1, h.driver.stops)
            assertTrue(
                h.factory.created
                    .single()
                    .closed,
            )
        }

    @Test
    fun duasChamadasDePrepare_carregamOModeloUmaVez() =
        runTest {
            var loads = 0
            val factory = FakeDecoderFactory()
            val shared =
                CachingModelLoader {
                    loads++
                    factory
                }
            val a = EngineHarness(this, factory = factory, loader = shared)
            a.engine.prepare()
            a.engine.prepare()
            val b = EngineHarness(this, factory = factory, loader = shared)
            b.engine.prepare()
            assertEquals(1, loads)
            assertEquals(EngineState.Ready, b.engine.state.value)
        }

    @Test
    fun falhaDeCargaNaoFicaEmCache_tentarDeNovoCarrega() =
        runTest {
            var loads = 0
            val factory = FakeDecoderFactory()
            val shared =
                CachingModelLoader {
                    loads++
                    if (loads == 1) throw ModelLoadException(EngineError.ModelCorrupt, "falhou")
                    factory
                }
            val h = EngineHarness(this, factory = factory, loader = shared)
            assertTrue(h.engine.prepare().isFailure)
            assertTrue(h.engine.prepare().isSuccess)
            assertEquals(2, loads)
        }

    @Test
    fun prepareComOMotorAberto_naoRecarregaNemMudaOEstado() =
        runTest {
            var loads = 0
            val factory = FakeDecoderFactory()
            val h =
                EngineHarness(
                    this,
                    factory = factory,
                    loader =
                        ModelLoader {
                            loads++
                            factory
                        },
                )
            h.open()
            h.engine.beginSession()
            assertTrue(h.engine.prepare().isSuccess)
            assertEquals(1, loads)
            assertEquals(EngineState.Recognizing, h.engine.state.value)
        }
}
```

- [ ] **Passo 2: ver falhar.** FAIL de compilação ("Unresolved reference 'VoskEngine'").

- [ ] **Passo 3: código.** Criar `KT/speech/VoskEngine.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

/**
 * SpeechEngine com laço próprio de captura alimentando o reconhecedor, sem o serviço pronto do vosk-android
 * (TRD §Contratos, SpeechEngine e regras; Motor de voz §Captura e sessões; spec 0003, C1–C9).
 *
 * - A captura fica aberta na leitura e na pausa; fora de sessão, os blocos são lidos e descartados, e só o
 *   [SpeechEvent.Level] segue.
 * - `beginSession` cria o [SessionId] e o reconhecedor juntos, num [Session]; todo evento daquele reconhecedor
 *   sai com o id guardado no [Session], nunca com uma variável de "sessão atual" (CLAUDE.md, regra 7).
 * - `endSession` para de alimentar o reconhecedor, descarta o bloco ainda não entregue e libera o reconhecedor
 *   (estratégia nova) ou o zera com `reset()` (estratégia [RecognizerStrategy.Reset]). Não chama o resultado
 *   final: a hipótese pendente é descartada (D14).
 *
 * Estratégia e fonte de áudio vêm do construtor, não da interface (D4, D8). Thread-safe: os comandos vêm de
 * uma thread e o laço roda em outra.
 */
class VoskEngine(
    private val modelLoader: ModelLoader,
    private val audioSource: () -> AudioSource,
    private val strategy: RecognizerStrategy = RecognizerStrategy.NewPerSession,
    private val captureDriver: CaptureDriver = ThreadCaptureDriver(),
    private val loadDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val blockSamples: Int = BLOCK_SAMPLES,
) : SpeechEngine {
    private val channel = Channel<SpeechEvent>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow(EngineState.Idle)
    private val lock = Any()
    private var factory: DecoderFactory? = null
    private var capture: Capture? = null
    private var current: Session? = null
    private var spare: Spare? = null
    private var nextSession = 1L
    private var released = false

    override val events: Flow<SpeechEvent> = channel.receiveAsFlow()
    override val state: StateFlow<EngineState> = mutableState.asStateFlow()

    override suspend fun prepare(): Result<Unit> {
        synchronized(lock) {
            check(!released) { "VoskEngine já liberado" }
            if (factory != null) return Result.success(Unit)
            mutableState.value = EngineState.Preparing
        }
        return try {
            val loaded = withContext(loadDispatcher) { modelLoader.load() }
            synchronized(lock) {
                factory = loaded
                mutableState.value = EngineState.Ready
            }
            Result.success(Unit)
        } catch (e: ModelLoadException) {
            mutableState.value = EngineState.Failed
            Result.failure(e)
        }
    }

    override fun startCapture() {
        val started: Capture
        synchronized(lock) {
            check(!released) { "VoskEngine já liberado" }
            check(mutableState.value == EngineState.Ready) { "startCapture exige Ready; estado: ${mutableState.value}" }
            val source = audioSource()
            try {
                source.open()
            } catch (e: AudioSourceException) {
                channel.trySend(SpeechEvent.Error(null, e.error))
                return
            }
            started = Capture(source)
            capture = started
            mutableState.value = EngineState.Capturing
        }
        captureDriver.start(started::step)
    }

    override fun stopCapture() {
        val stopped: Capture
        val ended: Session?
        synchronized(lock) {
            stopped = capture ?: return
            capture = null
            ended = current
            current = null
            mutableState.value = EngineState.Ready
        }
        ended?.end()
        stopped.active = false
        captureDriver.stop()
    }

    override fun beginSession(vocabulary: List<String>?): SessionId {
        val id: SessionId
        val reused: Decoder?
        val loaded: DecoderFactory
        synchronized(lock) {
            check(mutableState.value == EngineState.Capturing && current == null) {
                "beginSession exige captura aberta e nenhuma sessão ativa; estado: ${mutableState.value}"
            }
            loaded = checkNotNull(factory) { "beginSession antes de prepare" }
            id = SessionId(nextSession++)
            val kept = spare
            spare = null
            reused = if (kept != null && kept.vocabulary == vocabulary) kept.decoder else null
            if (kept != null && reused == null) kept.decoder.close()
        }
        val decoder =
            reused ?: try {
                loaded.create(vocabulary)
            } catch (e: Exception) {
                null
            }
        synchronized(lock) {
            current = Session(id, decoder, vocabulary)
            mutableState.value = EngineState.Recognizing
        }
        if (decoder == null) channel.trySend(SpeechEvent.Error(id, EngineError.Internal))
        return id
    }

    override fun endSession() {
        val ended: Session
        synchronized(lock) {
            ended = current ?: return
            current = null
            mutableState.value = EngineState.Capturing
        }
        ended.end()
    }

    override fun release() {
        stopCapture()
        val kept: Spare?
        synchronized(lock) {
            released = true
            kept = spare
            spare = null
            mutableState.value = EngineState.Idle
        }
        kept?.decoder?.close()
        channel.close()
    }

    /** O reconhecedor de uma sessão terminou de ser usado: libera ou guarda para `reset()` (D4). */
    private fun recycle(
        decoder: Decoder,
        vocabulary: List<String>?,
    ) {
        if (strategy == RecognizerStrategy.NewPerSession) {
            decoder.close()
            return
        }
        decoder.reset()
        val keep =
            synchronized(lock) {
                if (spare == null && !released) {
                    spare = Spare(decoder, vocabulary)
                    true
                } else {
                    false
                }
            }
        if (!keep) decoder.close()
    }

    private fun captureLost(
        lost: Capture,
        error: EngineError,
    ) {
        val ended: Session?
        synchronized(lock) {
            if (capture !== lost) return
            capture = null
            ended = current
            current = null
            mutableState.value = EngineState.Ready
        }
        ended?.end()
        channel.trySend(SpeechEvent.Error(null, error))
    }

    private class Spare(
        val decoder: Decoder,
        val vocabulary: List<String>?,
    )

    /** Uma captura aberta. O laço chama [step] até ela devolver `false`; quem fecha a fonte é o próprio laço. */
    private inner class Capture(
        private val source: AudioSource,
    ) {
        @Volatile
        var active = true

        private val buffer = ShortArray(blockSamples)

        fun step(): Boolean {
            if (!active) return close()
            val count =
                try {
                    source.read(buffer)
                } catch (e: AudioSourceException) {
                    return fail(e.error)
                }
            if (!active) return close() // parou durante a leitura: o bloco é descartado
            if (count < 0) return fail(EngineError.MicUnavailable)
            if (count == 0) return true
            channel.trySend(SpeechEvent.Level(AudioLevel.rms(buffer, count)))
            val session = synchronized(lock) { current } // para onde vai o áudio, não o id dos eventos
            session?.feed(buffer, count)
            return true
        }

        private fun close(): Boolean {
            source.close()
            return false
        }

        private fun fail(error: EngineError): Boolean {
            source.close()
            active = false
            captureLost(this, error)
            return false
        }
    }

    /**
     * Um reconhecedor e o seu id, criados juntos. Todo evento sai com [id]; um evento produzido depois do
     * `endSession` (o bloco já estava no reconhecedor) sai com o id antigo e é descartado por quem consome.
     */
    private inner class Session(
        val id: SessionId,
        private val decoder: Decoder?,
        private val vocabulary: List<String>?,
    ) {
        private var feeding = false
        private var ended = false
        private var failed = false
        private var decoderReleased = false

        fun feed(
            samples: ShortArray,
            count: Int,
        ) {
            val active = decoder ?: return
            synchronized(this) {
                if (ended) return // bloco lido antes do endSession e ainda não entregue: descartado (C3)
                feeding = true
            }
            try {
                val event =
                    try {
                        if (active.accept(samples, count)) {
                            SpeechEvent.Final(id, VoskJson.words(active.resultJson()))
                        } else {
                            SpeechEvent.Partial(id, VoskJson.words(active.partialJson()))
                        }
                    } catch (e: Exception) {
                        synchronized(this) { failed = true }
                        SpeechEvent.Error(id, EngineError.Internal)
                    }
                channel.trySend(event)
            } finally {
                val releaseNow =
                    synchronized(this) {
                        feeding = false
                        if (failed) ended = true // reconhecedor com exceção não recebe mais áudio
                        ended
                    }
                if (releaseNow) release(active)
            }
        }

        fun end() {
            val releaseNow =
                synchronized(this) {
                    if (ended) return
                    ended = true
                    !feeding
                }
            if (releaseNow) decoder?.let(::release)
        }

        private fun release(active: Decoder) {
            val broken =
                synchronized(this) {
                    if (decoderReleased) return
                    decoderReleased = true
                    failed
                }
            if (broken) active.close() else recycle(active, vocabulary)
        }
    }

    companion object {
        const val SAMPLE_RATE = 16_000

        /** 0,2 s de áudio por bloco, o mesmo do laço de referência do vosk-android 0.3.75 (`BUFFER_SIZE_SECONDS`). */
        const val BLOCK_SAMPLES = 3_200
    }
}
```

- [ ] **Passo 4: ver passar.** `VoskEngineSessionTest` 17 e `VoskEngineStateTest` 10 verdes. Conferir no relatório que nenhum teste carregou `org.vosk` (nenhum `UnsatisfiedLinkError`/`jna` na saída).

- [ ] **Passo 5: commit.**

```bash
git add etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/SpeechFakes.kt \
  etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/EngineHarness.kt \
  etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/VoskEngineSessionTest.kt \
  etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/VoskEngineStateTest.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/VoskEngine.kt
git commit -m "feat(speech): VoskEngine com laço próprio de captura e sessões presas ao reconhecedor (TRD §Contratos, SpeechEngine; Motor de voz §Captura e sessões)"
```

---

### Tarefa 9 (motor-voz): `ModelInstaller` (C10)

**Arquivos:** criar `KTT/speech/ModelInstallerTest.kt`, `KT/speech/ModelInstaller.kt`.

- [ ] **Passo 1: teste primeiro.** Criar `KTT/speech/ModelInstallerTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/** C10: cópia do modelo de assets por `uuid` (TRD Motor de voz §Instalação do modelo 1–3; spec 0003, D12). */
class ModelInstallerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    /** Assets falsos sobre um diretório; `failOn` simula falha de leitura (sem espaço, arquivo corrompido). */
    private class DirAssetTree(
        private val root: File,
        private val failOn: String? = null,
    ) : AssetTree {
        override fun list(path: String): List<String> = File(root, path).list()?.sorted().orEmpty()

        override fun open(path: String): InputStream {
            if (path == failOn) throw IOException("falha simulada em $path")
            return FileInputStream(File(root, path))
        }
    }

    private fun assets(uuid: String?): File {
        val root = tmp.newFolder("assets")
        val model = File(root, "model-pt").apply { mkdirs() }
        File(model, "final.mdl").writeText("modelo")
        File(model, "ivector").mkdirs()
        File(model, "ivector/final.ie").writeText("ivector")
        if (uuid != null) File(model, "uuid").writeText("$uuid\n")
        return root
    }

    @Test
    fun semModeloNoDestino_copiaTudo_comUuidPorUltimo() {
        val files = tmp.newFolder("files")
        val installed = ModelInstaller(DirAssetTree(assets("v1 sha256=abc")), files).install()
        assertTrue(installed.copied)
        assertEquals("v1 sha256=abc", installed.id)
        assertEquals(File(files, "model-pt"), installed.dir)
        assertEquals("modelo", File(installed.dir, "final.mdl").readText())
        assertEquals("ivector", File(installed.dir, "ivector/final.ie").readText())
        assertEquals("v1 sha256=abc\n", File(installed.dir, "uuid").readText())
        assertFalse(File(files, "model-pt.tmp").exists())
    }

    @Test
    fun uuidIgualNoDestino_naoCopia() {
        val files = tmp.newFolder("files")
        val tree = DirAssetTree(assets("v1"))
        ModelInstaller(tree, files).install()
        val marker = File(files, "model-pt/marca").apply { writeText("fica") }
        val again = ModelInstaller(tree, files).install()
        assertFalse(again.copied)
        assertTrue(marker.exists())
    }

    @Test
    fun uuidDiferenteNoDestino_copiaDeNovo() {
        val files = tmp.newFolder("files")
        File(files, "model-pt").mkdirs()
        File(files, "model-pt/uuid").writeText("v0\n")
        val marker = File(files, "model-pt/marca").apply { writeText("velho") }
        val installed = ModelInstaller(DirAssetTree(assets("v1")), files).install()
        assertTrue(installed.copied)
        assertFalse(marker.exists())
        assertEquals("v1\n", File(files, "model-pt/uuid").readText())
    }

    @Test
    fun uuidAusenteNoDestino_copia() {
        val files = tmp.newFolder("files")
        File(files, "model-pt").mkdirs()
        File(files, "model-pt/final.mdl").writeText("pela metade")
        val installed = ModelInstaller(DirAssetTree(assets("v1")), files).install()
        assertTrue(installed.copied)
        assertEquals("modelo", File(files, "model-pt/final.mdl").readText())
    }

    @Test
    fun falhaNaCopia_devolveModelCorrupt_eNaoDeixaCopiaPelaMetade() {
        val files = tmp.newFolder("files")
        File(files, "model-pt").mkdirs()
        File(files, "model-pt/uuid").writeText("v0\n")
        val error =
            runCatching { ModelInstaller(DirAssetTree(assets("v1"), failOn = "model-pt/ivector/final.ie"), files).install() }
                .exceptionOrNull() as ModelLoadException
        assertEquals(EngineError.ModelCorrupt, error.error)
        assertFalse(File(files, "model-pt.tmp").exists())
        assertEquals("v0\n", File(files, "model-pt/uuid").readText())
    }

    @Test
    fun modeloAusenteNosAssets_devolveModelMissing() {
        val files = tmp.newFolder("files")
        val error =
            runCatching {
                ModelInstaller(
                    DirAssetTree(assets(uuid = null)),
                    files,
                ).install()
            }.exceptionOrNull() as ModelLoadException
        assertEquals(EngineError.ModelMissing, error.error)
    }

    @Test
    fun mesmaVersao_naoCopia_diferenteOuAusente_copia() {
        assertFalse(ModelVersion.needsCopy("v1", "v1"))
        assertFalse(ModelVersion.needsCopy("v1", " v1 "))
        assertTrue(ModelVersion.needsCopy("v1", "v0"))
        assertTrue(ModelVersion.needsCopy("v1", null))
    }
}
```

- [ ] **Passo 2: ver falhar.** FAIL de compilação ("Unresolved reference 'ModelInstaller'", "'ModelVersion'").

- [ ] **Passo 3: código.** Criar `KT/speech/ModelInstaller.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/** Modelo pronto no armazenamento interno. */
data class InstalledModel(
    val dir: File,
    /** Primeira linha do `uuid` (D12): nome do modelo e SHA-256 do zip baixado. */
    val id: String,
    val copied: Boolean,
)

/** "Mesma versão → não copia; diferente ou ausente → copia" (TRD §Instalação do modelo 2; spec 0003, D12, C10). */
object ModelVersion {
    fun needsCopy(
        sourceUuid: String,
        targetUuid: String?,
    ): Boolean = targetUuid == null || targetUuid.trim() != sourceUuid.trim()
}

/**
 * Copia `assets/model-pt` para o armazenamento interno (`filesDir/model-pt`) na primeira execução
 * (TRD Motor de voz §Instalação do modelo 1–3; spec 0003, C10). Usa o mesmo arquivo `uuid` do
 * `StorageService` do Vosk, mas com cópia própria: o `StorageService.sync` do vosk-android 0.3.75 copia para
 * `getExternalFilesDir` e só avisa no fim (OPR-021).
 *
 * A cópia vai para `model-pt.tmp`, com o `uuid` por último, e só então troca de nome; uma cópia interrompida
 * nunca parece completa. Falha: [ModelLoadException] com `ModelMissing` (asset ausente) ou `ModelCorrupt`
 * (cópia falhou: sem espaço, arquivo corrompido), e a tela oferece "Tentar de novo".
 */
class ModelInstaller(
    private val assets: AssetTree,
    private val filesDir: File,
    private val assetDir: String = MODEL_DIR,
) {
    fun install(): InstalledModel {
        val sourceUuid =
            try {
                assets.open("$assetDir/$UUID").use(::firstLine)
            } catch (e: IOException) {
                throw ModelLoadException(EngineError.ModelMissing, "modelo ausente nos assets", e)
            } ?: throw ModelLoadException(EngineError.ModelMissing, "uuid do modelo vazio")
        val target = File(filesDir, assetDir)
        val targetUuidFile = File(target, UUID)
        val targetUuid = if (targetUuidFile.isFile) targetUuidFile.inputStream().use(::firstLine) else null
        if (!ModelVersion.needsCopy(sourceUuid, targetUuid)) return InstalledModel(target, sourceUuid.trim(), copied = false)
        val temp = File(filesDir, "$assetDir.tmp")
        try {
            temp.deleteRecursively()
            copyTree(assetDir, temp)
            copyFile("$assetDir/$UUID", File(temp, UUID))
            target.deleteRecursively()
            if (!temp.renameTo(target)) throw IOException("não foi possível mover ${temp.name} para ${target.name}")
        } catch (e: IOException) {
            temp.deleteRecursively()
            throw ModelLoadException(EngineError.ModelCorrupt, "falha na cópia do modelo", e)
        }
        return InstalledModel(target, sourceUuid.trim(), copied = true)
    }

    private fun copyTree(
        path: String,
        destination: File,
    ) {
        val children = assets.list(path)
        if (children.isEmpty()) {
            if (path != "$assetDir/$UUID") copyFile(path, File(destination, path.removePrefix("$assetDir/")))
            return
        }
        for (child in children) copyTree("$path/$child", destination)
    }

    private fun copyFile(
        path: String,
        destination: File,
    ) {
        val parent = destination.parentFile
        if (parent != null && !parent.isDirectory && !parent.mkdirs()) throw IOException("não foi possível criar ${parent.name}")
        assets.open(path).use { input -> FileOutputStream(destination).use { output -> input.copyTo(output) } }
    }

    private fun firstLine(input: InputStream): String? =
        input
            .bufferedReader(Charsets.UTF_8)
            .readLine()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    companion object {
        const val MODEL_DIR = "model-pt"
        const val UUID = "uuid"
    }
}
```

- [ ] **Passo 4: ver passar.** `ModelInstallerTest` 7 verdes.

- [ ] **Passo 5: commit.**

```bash
git add etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/ModelInstallerTest.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/ModelInstaller.kt
git commit -m "feat(speech): ModelInstaller copia o modelo para o armazenamento interno por uuid (TRD §Instalação do modelo 1-3; OPR-021)"
```

---

### Tarefa 10 (motor-voz): `WavAudioSource` (C11)

**Arquivos:** criar `KTT/speech/WavAudioSourceTest.kt`, `KT/speech/WavAudioSource.kt`.

- [ ] **Passo 1: teste primeiro.** Criar `KTT/speech/WavAudioSourceTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** C11: fonte WAV de teste (spec 0003, D8). O WAV é sintético, gerado aqui; nunca uma gravação de discurso. */
class WavAudioSourceTest {
    private fun wav(
        samples: ShortArray,
        audioFormat: Int = 1,
        sampleRate: Int = 16_000,
        channels: Int = 1,
        bits: Int = 16,
        extraChunk: Boolean = false,
    ): ByteArray {
        val data = ByteArrayOutputStream()
        samples.forEach {
            data.write(it.toInt() and 0xFF)
            data.write((it.toInt() shr 8) and 0xFF)
        }
        val body = ByteArrayOutputStream()

        fun int16(v: Int) {
            body.write(v and 0xFF)
            body.write((v shr 8) and 0xFF)
        }

        fun int32(v: Int) {
            int16(v and 0xFFFF)
            int16((v shr 16) and 0xFFFF)
        }
        body.write("WAVE".toByteArray())
        body.write("fmt ".toByteArray())
        int32(16)
        int16(audioFormat)
        int16(channels)
        int32(sampleRate)
        int32(sampleRate * channels * bits / 8)
        int16(channels * bits / 8)
        int16(bits)
        if (extraChunk) {
            body.write("LIST".toByteArray())
            int32(3)
            body.write(byteArrayOf(1, 2, 3, 0))
        }
        body.write("data".toByteArray())
        int32(data.size())
        body.write(data.toByteArray())
        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray())
        val size = body.size()
        out.write(
            byteArrayOf((size and 0xFF).toByte(), (size shr 8 and 0xFF).toByte(), (size shr 16 and 0xFF).toByte(), (size shr 24).toByte()),
        )
        out.write(body.toByteArray())
        return out.toByteArray()
    }

    private fun source(bytes: ByteArray) = WavAudioSource({ ByteArrayInputStream(bytes) }, Pacer { })

    private val ramp = ShortArray(10_000) { (it * 7 - 30_000).toShort() }

    @Test
    fun wav16kMonoPcm16_eAceito_eOsBlocosSaemEmOrdemESemPerda() {
        val wav = source(wav(ramp, extraChunk = true))
        wav.open()
        val buffer = ShortArray(3_200)
        val read = mutableListOf<Short>()
        while (!wav.exhausted) {
            val n = wav.read(buffer)
            read += buffer.copyOf(n).toList()
        }
        assertEquals(ramp.size, read.size)
        assertArrayEquals(ramp, read.toShortArray())
    }

    @Test
    fun depoisDoFim_entregaSilencio() {
        val wav = source(wav(ShortArray(100) { 500 }))
        wav.open()
        val buffer = ShortArray(3_200)
        assertEquals(100, wav.read(buffer))
        assertEquals(3_200, wav.read(buffer))
        assertTrue(buffer.all { it == 0.toShort() })
    }

    @Test
    fun ritmoReal_recebeOTotalEntregue() {
        val delivered = mutableListOf<Long>()
        val wav = WavAudioSource({ ByteArrayInputStream(wav(ShortArray(5_000))) }, Pacer { delivered += it })
        wav.open()
        val buffer = ShortArray(3_200)
        wav.read(buffer)
        wav.read(buffer)
        assertEquals(listOf(3_200L, 5_000L), delivered)
    }

    private fun refused(bytes: ByteArray): EngineError? =
        (runCatching { source(bytes).open() }.exceptionOrNull() as? AudioSourceException)?.error

    @Test
    fun wav44kHz_eRecusado() {
        assertEquals(EngineError.MicUnavailable, refused(wav(ramp, sampleRate = 44_100)))
    }

    @Test
    fun wavEstereo_eRecusado() {
        assertEquals(EngineError.MicUnavailable, refused(wav(ramp, channels = 2)))
    }

    @Test
    fun wav8Bits_eRecusado() {
        assertEquals(EngineError.MicUnavailable, refused(wav(ramp, bits = 8)))
    }

    @Test
    fun arquivoQueNaoEWav_eRecusado() {
        assertEquals(EngineError.MicUnavailable, refused("não é wav".toByteArray()))
    }

    @Test
    fun wavNaoPcm_eRecusado() {
        assertEquals(EngineError.MicUnavailable, refused(wav(ramp, audioFormat = 3)))
    }
}
```

- [ ] **Passo 2: ver falhar.** FAIL de compilação ("Unresolved reference 'WavAudioSource'", "'Pacer'").

- [ ] **Passo 3: código.** Criar `KT/speech/WavAudioSource.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/** Formato lido do cabeçalho de um WAV. */
data class WavFormat(
    val audioFormat: Int,
    val channels: Int,
    val sampleRate: Int,
    val bitsPerSample: Int,
    /** Bytes do bloco `data`. */
    val dataSize: Long,
)

/** Cabeçalho RIFF/WAVE (spec 0003, D8, C11). Só lê; nunca escreve. */
object WavHeader {
    private const val PCM = 1

    /** Lê o cabeçalho até o início do bloco `data`; o stream fica posicionado na primeira amostra. */
    fun read(input: DataInputStream): WavFormat {
        if (tag(input) != "RIFF") throw IOException("não é RIFF")
        int32(input)
        if (tag(input) != "WAVE") throw IOException("não é WAVE")
        var format: IntArray? = null
        while (true) {
            val id = tag(input)
            val size = int32(input).toLong() and 0xFFFFFFFFL
            when (id) {
                "fmt " -> {
                    val audioFormat = int16(input)
                    val channels = int16(input)
                    val sampleRate = int32(input)
                    int32(input) // byteRate
                    int16(input) // blockAlign
                    val bits = int16(input)
                    skip(input, size - 16)
                    format = intArrayOf(audioFormat, channels, sampleRate, bits)
                }

                "data" -> {
                    val f = format ?: throw IOException("bloco data antes de fmt")
                    return WavFormat(f[0], f[1], f[2], f[3], size)
                }

                else -> {
                    skip(input, size + (size and 1L))
                }
            }
        }
    }

    /** 16 kHz, mono, PCM 16 bits, o formato da captura (TRD §Captura e sessões; TESTES-ETAPA0). */
    fun isSpeechFormat(format: WavFormat): Boolean =
        format.audioFormat == PCM && format.channels == 1 && format.sampleRate == 16_000 && format.bitsPerSample == 16

    private fun tag(input: DataInputStream): String {
        val bytes = ByteArray(4)
        input.readFully(bytes)
        return String(bytes, Charsets.US_ASCII)
    }

    private fun int16(input: DataInputStream): Int {
        val b0 = input.readUnsignedByte()
        val b1 = input.readUnsignedByte()
        return b0 or (b1 shl 8)
    }

    private fun int32(input: DataInputStream): Int = int16(input) or (int16(input) shl 16)

    private fun skip(
        input: DataInputStream,
        count: Long,
    ) {
        var left = count
        while (left > 0) {
            val skipped = input.skip(left)
            if (skipped <= 0) throw EOFException("WAV truncado")
            left -= skipped
        }
    }
}

/** Ritmo da fonte WAV: espera até o tempo real de `delivered` amostras (spec 0003, D8). */
fun interface Pacer {
    fun awaitSamples(delivered: Long)
}

/** Ritmo real, por relógio monotônico. */
class RealTimePacer(
    private val sampleRate: Int = VoskEngine.SAMPLE_RATE,
) : Pacer {
    private var startNanos = -1L

    override fun awaitSamples(delivered: Long) {
        if (startNanos < 0) startNanos = System.nanoTime()
        val due = startNanos + delivered * 1_000_000_000L / sampleRate
        val waitMs = (due - System.nanoTime()) / 1_000_000L
        if (waitMs > 0) Thread.sleep(waitMs)
    }
}

/**
 * WAV de teste como fonte de áudio (spec 0003, D8): lido no ritmo real, nunca gravado. Depois do fim dos dados,
 * entrega silêncio (zeros) no mesmo ritmo, como um microfone mudo: o reconhecedor fecha a frase sozinho.
 * Fora de 16 kHz, mono, PCM 16 bits, `open()` recusa com [EngineError.MicUnavailable].
 */
class WavAudioSource(
    private val open: () -> InputStream,
    private val pacer: Pacer = RealTimePacer(),
) : AudioSource {
    constructor(file: File) : this({ FileInputStream(file) })

    private var input: DataInputStream? = null
    private var remainingSamples = 0L
    private var delivered = 0L

    /** `true` depois que todas as amostras do arquivo foram entregues. */
    val exhausted: Boolean
        get() = remainingSamples == 0L

    override fun open() {
        val stream =
            try {
                DataInputStream(BufferedInputStream(open.invoke()))
            } catch (e: IOException) {
                throw AudioSourceException(EngineError.MicUnavailable, "WAV não abriu: ${e.message}", e)
            }
        val format =
            try {
                WavHeader.read(stream)
            } catch (e: IOException) {
                stream.close()
                throw AudioSourceException(EngineError.MicUnavailable, "WAV inválido: ${e.message}", e)
            }
        if (!WavHeader.isSpeechFormat(format)) {
            stream.close()
            throw AudioSourceException(EngineError.MicUnavailable, "WAV fora de 16 kHz, mono, PCM 16 bits: $format")
        }
        input = stream
        remainingSamples = format.dataSize / 2
        delivered = 0L
    }

    override fun read(buffer: ShortArray): Int {
        val stream = input ?: throw AudioSourceException(EngineError.MicUnavailable, "WAV fechado")
        val count: Int
        if (remainingSamples > 0) {
            count = minOf(buffer.size.toLong(), remainingSamples).toInt()
            try {
                for (i in 0 until count) {
                    val lo = stream.readUnsignedByte()
                    val hi = stream.readByte().toInt()
                    buffer[i] = ((hi shl 8) or lo).toShort()
                }
            } catch (e: IOException) {
                throw AudioSourceException(EngineError.MicUnavailable, "WAV truncado", e)
            }
            remainingSamples -= count
        } else {
            count = buffer.size
            buffer.fill(0)
        }
        delivered += count
        pacer.awaitSamples(delivered)
        return count
    }

    override fun close() {
        input?.close()
        input = null
    }
}
```

- [ ] **Passo 4: ver passar.** `WavAudioSourceTest` 8 verdes.

- [ ] **Passo 5: commit.**

```bash
git add etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/speech/WavAudioSourceTest.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/WavAudioSource.kt
git commit -m "feat(speech): fonte WAV de teste 16 kHz mono PCM 16 no ritmo real, sem gravar (spec 0003 D8)"
```

---

### Tarefa 11 (motor-voz): adaptadores Android e Vosk

Sem teste JVM (dependem de aparelho; a 0005 valida). Verificação: compilação, lint e os greps.

**Arquivos:** criar `KT/speech/MicAudioSource.kt`, `KT/speech/VoskDecoders.kt`.

- [ ] **Passo 1: código.** Criar `KT/speech/MicAudioSource.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.core.content.ContextCompat

/**
 * Microfone por `AudioRecord`, 16 kHz, mono, PCM 16 bits (TRD Motor de voz §Captura e sessões; spec 0003, D15).
 * Só lê para o reconhecedor; nada é gravado (CLAUDE.md, regra 3). Validado no aparelho pela 0005.
 */
class MicAudioSource(
    private val context: Context,
) : AudioSource {
    private var record: AudioRecord? = null

    @SuppressLint("MissingPermission") // a permissão é conferida logo antes
    override fun open() {
        if (!hasPermission()) throw AudioSourceException(EngineError.PermissionRevoked, "sem permissão de microfone")
        val minBytes = AudioRecord.getMinBufferSize(VoskEngine.SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBytes <= 0) throw AudioSourceException(EngineError.MicUnavailable, "AudioRecord.getMinBufferSize=$minBytes")
        val created =
            try {
                AudioRecord(VOICE_RECOGNITION, VoskEngine.SAMPLE_RATE, CHANNEL, ENCODING, maxOf(minBytes, BUFFER_BYTES))
            } catch (e: SecurityException) {
                throw AudioSourceException(EngineError.PermissionRevoked, "permissão negada ao abrir o microfone", e)
            } catch (e: IllegalArgumentException) {
                throw AudioSourceException(EngineError.MicUnavailable, "parâmetros do AudioRecord recusados", e)
            }
        if (created.state != AudioRecord.STATE_INITIALIZED) {
            created.release()
            throw AudioSourceException(EngineError.MicUnavailable, "AudioRecord não inicializou")
        }
        created.startRecording()
        if (created.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            created.release()
            throw AudioSourceException(EngineError.MicUnavailable, "microfone ocupado")
        }
        record = created
    }

    override fun read(buffer: ShortArray): Int {
        val active = record ?: throw AudioSourceException(EngineError.MicUnavailable, "microfone fechado")
        val count = active.read(buffer, 0, buffer.size)
        if (count < 0) {
            val cause = if (hasPermission()) EngineError.MicUnavailable else EngineError.PermissionRevoked
            throw AudioSourceException(cause, "AudioRecord.read=$count")
        }
        return count
    }

    override fun close() {
        val active = record ?: return
        record = null
        try {
            active.stop()
        } catch (e: IllegalStateException) {
            // já parado
        }
        active.release()
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private companion object {
        /** `AudioSource.VOICE_RECOGNITION` da API do Android (6), a mesma fonte do Vosk 0.3.75. */
        const val VOICE_RECOGNITION = 6
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** Dois blocos de 16 bits, como o laço de referência do vosk-android 0.3.75 (bufferSize * 2). */
        const val BUFFER_BYTES = VoskEngine.BLOCK_SAMPLES * 2 * 2
    }
}
```

Criar `KT/speech/VoskDecoders.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.speech

import android.content.Context
import android.content.res.AssetManager
import org.vosk.LibVosk
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/** `AssetManager` como [AssetTree]. */
class AndroidAssetTree(
    private val assets: AssetManager,
) : AssetTree {
    override fun list(path: String): List<String> = assets.list(path)?.toList().orEmpty()

    override fun open(path: String): InputStream = assets.open(path)
}

/** Reconhecedores do Vosk sobre um modelo carregado uma vez (TRD §Instalação do modelo 4). */
class VoskDecoderFactory(
    private val model: Model,
    override val info: ModelInfo,
) : DecoderFactory {
    override fun create(vocabulary: List<String>?): Decoder {
        val rate = VoskEngine.SAMPLE_RATE.toFloat()
        val recognizer = if (vocabulary == null) Recognizer(model, rate) else Recognizer(model, rate, VoskJson.grammar(vocabulary))
        return VoskDecoder(recognizer)
    }
}

private class VoskDecoder(
    private val recognizer: Recognizer,
) : Decoder {
    override fun accept(
        samples: ShortArray,
        count: Int,
    ): Boolean = recognizer.acceptWaveForm(samples, count)

    override fun partialJson(): String = recognizer.partialResult

    override fun resultJson(): String = recognizer.result

    override fun reset() = recognizer.reset()

    override fun close() = recognizer.close()
}

/** Instala (se preciso) e carrega o `vosk-model-small-pt-0.3`; mede as duas etapas (spec 0003, D11, D18). */
class VoskModelLoader(
    private val context: Context,
) : ModelLoader {
    override fun load(): DecoderFactory {
        val installStart = System.nanoTime()
        val installed = ModelInstaller(AndroidAssetTree(context.assets), context.filesDir).install()
        val loadStart = System.nanoTime()
        val model =
            try {
                // -2: só erros. Os avisos do Kaldi citam palavras do vocabulário, que vêm do texto (Q6).
                LibVosk.vosk_set_log_level(KALDI_ERRORS_ONLY)
                Model(installed.dir.absolutePath)
            } catch (e: IOException) {
                throw ModelLoadException(EngineError.ModelCorrupt, "o Vosk não carregou o modelo", e)
            } catch (e: UnsatisfiedLinkError) {
                throw ModelLoadException(EngineError.ModelCorrupt, "biblioteca nativa do Vosk não carregou", e)
            }
        val done = System.nanoTime()
        val grammarFst = File(installed.dir, "Gr.fst")
        val info =
            ModelInfo(
                id = installed.id,
                installMs = (loadStart - installStart) / 1_000_000L,
                loadMs = (done - loadStart) / 1_000_000L,
                lexicon =
                    lazy {
                        try {
                            FileInputStream(grammarFst).use { Lexicon(FstSymbols.inputSymbols(it)) }
                        } catch (e: IOException) {
                            null
                        }
                    },
            )
        return VoskDecoderFactory(model, info)
    }

    private companion object {
        const val KALDI_ERRORS_ONLY = -2
    }
}

/** Um carregador por processo: o modelo é carregado uma vez e mantido enquanto o app vive. */
object VoskModels {
    @Volatile
    private var loader: CachingModelLoader? = null

    fun loader(context: Context): CachingModelLoader =
        loader ?: synchronized(this) {
            loader ?: CachingModelLoader(VoskModelLoader(context.applicationContext)).also { loader = it }
        }
}
```

- [ ] **Passo 2: verificar.** `./gradlew :app:compileDebugKotlin lint ktlintCheck --console=plain` verde (lint sem `WrongConstant` nem `MissingPermission`). `grep -rnE "MediaRecorder|SpeechService|Log\." app/src/main` vazio.

- [ ] **Passo 3: commit e aviso.**

```bash
git add etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/MicAudioSource.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/speech/VoskDecoders.kt
git commit -m "feat(speech): AudioRecord, Recognizer do Vosk e modelo carregado uma vez por processo (TRD §Captura e sessões; §Instalação do modelo 4)"
```

Avise o orquestrador: o `interface` pode fazer a Tarefa 15.

---

### Tarefa 12 (interface): `ReaderController` (D1–D9; C5 e C10 na tela)

Começa depois do commit da Tarefa 7. Usa só o contrato (`FakeSpeechEngine`) e os tipos das Tarefas 4, 5 e 7.

**Arquivos:** criar `KTT/viewmodel/ReaderHarness.kt`, `KTT/viewmodel/ReaderSessionTest.kt`, `KTT/viewmodel/ReaderReadingTest.kt`, `KTT/viewmodel/ReaderTapTest.kt`, `KTT/viewmodel/ReaderExportTest.kt`, `KTT/viewmodel/ReaderLifecycleTest.kt`, `KT/viewmodel/ReaderUiState.kt`, `KT/viewmodel/ReaderController.kt`.

- [ ] **Passo 1: testes primeiro.** Criar `KTT/viewmodel/ReaderHarness.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.export.ExportSink
import io.github.gigioderico.tribuna.etapa0.export.TestTextCatalog
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.EngineState
import io.github.gigioderico.tribuna.etapa0.speech.FakeSpeechEngine
import io.github.gigioderico.tribuna.etapa0.speech.ModelInfo
import io.github.gigioderico.tribuna.etapa0.speech.ModelLoadException
import io.github.gigioderico.tribuna.etapa0.speech.SessionId
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEngine
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import java.io.File

/**
 * FakeSpeechEngine com registro das chamadas e do cursor no momento de cada uma (D4: ordem conferida no fake).
 * `failPrepare` faz o próximo prepare falhar com o erro dado, sem tocar no fake.
 */
class RecordingEngine(
    val fake: FakeSpeechEngine,
    val config: EngineConfig,
    private val cursor: () -> Int,
) : SpeechEngine {
    val calls = mutableListOf<String>()
    var failPrepare: EngineError? = null
    var failStartCapture: EngineError? = null

    override val events: Flow<SpeechEvent> = fake.events
    override val state: StateFlow<EngineState> = fake.state

    override suspend fun prepare(): Result<Unit> {
        calls += "prepare"
        val error = failPrepare ?: return fake.prepare()
        failPrepare = null
        return Result.failure(ModelLoadException(error, "falha simulada"))
    }

    override fun startCapture() {
        calls += "startCapture"
        val error = failStartCapture ?: return fake.startCapture()
        // Como o VoskEngine: a fonte não abre, o estado fica Ready e sai um Error sem sessão.
        failStartCapture = null
        fake.emitCaptureError(error)
    }

    override fun stopCapture() {
        calls += "stopCapture"
        fake.stopCapture()
    }

    override fun beginSession(vocabulary: List<String>?): SessionId {
        calls += "beginSession@${cursor()}"
        return fake.beginSession(vocabulary)
    }

    override fun endSession() {
        calls += "endSession@${cursor()}"
        fake.endSession()
    }

    override fun release() {
        calls += "release"
        fake.release()
    }
}

/** ReaderController com FakeSpeechEngine, relógio virtual do kotlinx-coroutines-test e exportação em memória. */
class ReaderHarness(
    private val scope: TestScope,
) {
    val engines = mutableListOf<RecordingEngine>()
    val exported = linkedMapOf<String, String>()
    val modelId: String = File(TestFiles.modelDir, "uuid").readLines().first().trim()
    private val dispatcher = StandardTestDispatcher(scope.testScheduler)
    private lateinit var controllerRef: ReaderController

    val controller: ReaderController =
        ReaderController(
            scope = scope.backgroundScope,
            engineFactory = { config ->
                RecordingEngine(FakeSpeechEngine(), config) { controllerRef.state.value.cursor }.also { engines += it }
            },
            catalog = TestTextCatalog(TestFiles.textIds.associateWith(TestFiles::text)),
            modelInfo = { ModelInfo(id = modelId, installMs = 1_234L, loadMs = 456L, lexicon = lazyOf(null)) },
            exportSink =
                ExportSink { name, files ->
                    files.map { (suffix, content) ->
                        "$name.$suffix".also { exported[it] = content }
                    }
                },
            provenance = Provenance("0.0.1", "0.3.75", "Fabricante Modelo", "16 (API 36)"),
            wavFiles = { listOf("T01_giorgio_sala_t1.wav") },
            clock = { scope.testScheduler.currentTime },
            processing = dispatcher,
            io = dispatcher,
        ).also { controllerRef = it }

    val engine: RecordingEngine get() = engines.last()
    val fake: FakeSpeechEngine get() = engine.fake
    val state: ReaderUiState get() = controller.state.value

    /** Processa a fila sem avançar o relógio. */
    fun settle() = scope.testScheduler.runCurrent()

    /** Avança o relógio virtual e processa. */
    fun advance(ms: Long) {
        scope.testScheduler.advanceTimeBy(ms)
        scope.testScheduler.runCurrent()
    }

    fun load(id: String) {
        controller.loadTestText(id)
        settle()
    }

    fun listen() {
        controller.onListen()
        settle()
    }

    fun pause() {
        controller.onPause()
        settle()
    }

    fun partial(vararg words: String) {
        fake.hearPartial(words.toList())
        settle()
    }

    fun final(vararg words: String) {
        fake.hearFinal(words.toList())
        settle()
    }

    fun export(): Map<String, String> {
        controller.onExport()
        settle()
        return exported
    }

    /** Linhas de um arquivo exportado, sem o cabeçalho. */
    fun rows(file: String): List<String> =
        exported
            .getValue(file)
            .lines()
            .drop(1)
            .filter { it.isNotEmpty() }
}
```

Criar `KTT/viewmodel/ReaderSessionTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.speech.SessionId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** D1 descarte por sessão (CLAUDE.md, regra 7; TRD Reconciliação 7) e D2 fala na pausa. */
class ReaderSessionTest {
    @Test
    fun parcialEFinalAtrasadosDaSessaoAnterior_naoMexemNoCursorNemNaCauda() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.partial("ouvir", "bem", "é", "uma")
            val cursor = h.state.cursor
            assertTrue("a leitura precisa ter andado (Q3)", cursor > 0)
            h.pause()
            h.listen()
            assertEquals(2L, h.state.debug.activeSession)
            val old = h.fake.recognizers.first()
            old.partial(listOf("ouvir", "bem", "é", "uma", "habilidade", "que", "quase"))
            old.final(listOf("ouvir", "bem", "é", "uma", "habilidade", "que", "quase", "ninguém", "pratica"))
            h.settle()
            assertEquals(cursor, h.state.cursor)
            assertEquals(emptyList<String>(), h.state.debug.tail)
        }

    @Test
    fun eventosAtrasadosEntramNaExportacaoComOIdOriginalEAccepted0() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            h.listen()
            h.fake.recognizers
                .first()
                .final(listOf("ouvir", "bem"))
            h.settle()
            h.export()
            assertEquals(listOf("0,1,final,\"ouvir bem\",0"), h.rows("T01.hypotheses.csv"))
        }

    @Test
    fun falaNaPausa_naoMoveOCursor_eAoRetomarASessaoENovaComCaudaVazia() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.pause()
            val cursor = h.state.cursor
            val paused = h.fake.recognizers.single()
            // Na pausa não há sessão: a fala chega ao motor e é descartada; o reconhecedor antigo pode atrasar.
            h.fake.hearPartial(listOf("uma", "habilidade", "que"))
            paused.partial(listOf("ouvir", "bem", "é", "uma", "habilidade"))
            h.settle()
            assertEquals(cursor, h.state.cursor)
            h.listen()
            assertEquals(
                SessionId(2),
                h.fake.recognizers
                    .last()
                    .session,
            )
            assertEquals(2L, h.state.debug.activeSession)
            assertEquals(emptyList<String>(), h.state.debug.tail)
            h.partial("uma")
            h.partial("uma", "habilidade")
            assertEquals(listOf("uma"), h.state.debug.tail)
        }
}
```

Criar `KTT/viewmodel/ReaderReadingTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.align.Move
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** D3 leitura de ponta a ponta e D5 limiar de nível (spec 0003, D3, D17). */
class ReaderReadingTest {
    /** Lê o texto como o Vosk entregaria: parciais crescentes por frase de até 8 palavras e o Final da frase. */
    private fun ReaderHarness.readAll(id: String) {
        TestFiles.spokenLike(id).chunked(8).forEach { phrase ->
            for (n in 1..phrase.size) {
                fake.hearPartial(phrase.take(n))
                advance(150)
            }
            fake.hearFinal(phrase)
            advance(300)
        }
    }

    @Test
    fun t01PorParciaisEFinais_cursorChegaAoUltimoToken_eTrajetoriaTemUmaLinhaPorMudanca() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.readAll("T01")
            assertEquals(h.state.tokens.lastIndex, h.state.cursor)
            h.export()
            val rows = h.rows("T01.trajectory.csv")
            assertEquals("0,0,Reposition,0.0", rows.first())
            val cursors = rows.map { it.split(',')[1].toInt() }
            val moves = rows.map { it.split(',')[2] }
            assertTrue(moves.all { move -> Move.entries.any { it.name == move } })
            assertTrue("uma linha por mudança de cursor", cursors.zipWithNext().all { (a, b) -> a != b })
            assertTrue("pelo menos 100 avanços (Q3)", rows.size > 100)
            assertEquals(h.state.tokens.lastIndex, cursors.last())
            val times = rows.map { it.split(',')[0].toLong() }
            assertEquals(times.sorted(), times)
        }

    @Test
    fun limiarZero_todoParcialEProcessado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.fake.emitLevel(0f)
            h.settle()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.partial("ouvir", "bem", "é", "uma")
            assertTrue(h.state.cursor > 0)
        }

    @Test
    fun limiarAcimaDoUltimoNivel_parcialIgnorado_cursorParado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.controller.setLevelThreshold(0.5f)
            h.listen()
            h.fake.emitLevel(0.2f)
            h.settle()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.partial("ouvir", "bem", "é", "uma")
            assertEquals(0, h.state.cursor)
            assertEquals(emptyList<String>(), h.state.debug.tail)
            h.export()
            assertTrue(h.rows("T01.hypotheses.csv").all { it.endsWith(",0") })
        }

    @Test
    fun limiarNaoFiltraFinal() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.controller.setLevelThreshold(0.5f)
            h.listen()
            h.fake.emitLevel(0.2f)
            h.settle()
            h.final("ouvir", "bem", "é", "uma")
            assertTrue(h.state.cursor > 0)
        }

    @Test
    fun nivelAcimaDoLimiar_parcialProcessado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.controller.setLevelThreshold(0.5f)
            h.listen()
            h.fake.emitLevel(0.5f)
            h.settle()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.partial("ouvir", "bem", "é", "uma")
            assertTrue(h.state.cursor > 0)
        }

    @Test
    fun recuoCurto_trajetoriaGravaOCursor_eOHighWaterFicaNoMaior() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.final("ouvir", "bem", "é", "uma", "habilidade", "que")
            assertEquals(5, h.state.cursor)
            h.final("bem", "é", "uma")
            assertEquals(3, h.state.cursor)
            assertEquals(5, h.state.debug.highWater)
            h.export()
            assertEquals("3", h.rows("T01.trajectory.csv").last().split(',')[1])
        }

    @Test
    fun inicioDaEscuta_doBeginSessionAoPrimeiroParcialNaoVazio() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.advance(300)
            h.partial()
            h.advance(400)
            h.partial("ouvir")
            h.advance(100)
            h.partial("ouvir", "bem")
            assertEquals(700L, h.state.debug.firstPartialMs)
        }

    @Test
    fun textoNaoMudaDuranteALeitura() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.setText(TestFiles.text("T09"))
            h.settle()
            assertEquals("T01", h.state.testTextId)
        }

    @Test
    fun microfoneNaoAbre_aLeituraNaoComeca_eMostraOErro() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.engine.failStartCapture = EngineError.MicUnavailable
            h.listen()
            assertEquals(ReaderPhase.Idle, h.state.phase)
            assertEquals(false, h.state.capturing)
            assertEquals(EngineError.MicUnavailable, h.state.error)
        }
}
```

Criar `KTT/viewmodel/ReaderTapTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.align.Move
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** D4 toque em palavra (spec 0003, D10; TRD Máquina de estados, Reading e Paused). */
class ReaderTapTest {
    @Test
    fun toqueNaPalavraDuranteALeitura_endSessionRepositionBeginSession_nessaOrdem() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.controller.onWordTapped(20)
            h.settle()
            // endSession com o cursor antigo; beginSession já com o cursor no token tocado.
            assertEquals(listOf("endSession@0", "beginSession@20"), h.engine.calls.takeLast(2))
            assertEquals(20, h.state.cursor)
            assertEquals(Move.Reposition, h.state.debug.lastMove)
            assertEquals(emptyList<String>(), h.state.debug.tail)
            assertEquals(ReaderPhase.Reading, h.state.phase)
            h.export()
            assertEquals(listOf("0,toque_palavra,20"), h.rows("T01.events.csv"))
            assertEquals("0,20,Reposition,0.0", h.rows("T01.trajectory.csv").last())
        }

    @Test
    fun toqueNaPalavraNaPausa_retomaNaPalavra() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            h.controller.onWordTapped(7)
            h.settle()
            assertEquals("beginSession@7", h.engine.calls.last())
            assertEquals(ReaderPhase.Reading, h.state.phase)
            assertEquals(7, h.state.cursor)
        }

    @Test
    fun pausarEOuvir_gravamPausaToqueERetomaToque() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.advance(1_000)
            h.pause()
            h.advance(2_500)
            h.listen()
            h.export()
            assertEquals(listOf("1000,pausa_toque,", "3500,retoma_toque,"), h.rows("T01.events.csv"))
        }

    @Test
    fun toqueNaPalavraAtual_tambemGravaOReposition() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.onWordTapped(0)
            h.settle()
            h.export()
            assertEquals(listOf("0,0,Reposition,0.0", "0,0,Reposition,0.0"), h.rows("T01.trajectory.csv"))
        }
}
```

Criar `KTT/viewmodel/ReaderExportTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.export.ExportFiles
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** D6 exportação só com texto de teste, D7 ida e volta (OPR-008), D8 proveniência (D11). */
class ReaderExportTest {
    @Test
    fun textoAlterado_desabilitaAExportacao() =
        runTest {
            val h = ReaderHarness(this)
            h.controller.setText(TestFiles.text("T01").replaceFirst("Ouvir", "Ouvi"))
            h.settle()
            assertNull(h.state.testTextId)
            h.listen()
            h.partial("ouvir", "bem")
            assertFalse(h.state.canExport)
            assertTrue(h.export().isEmpty())
        }

    @Test
    fun textoDeTeste_habilitaAExportacao_comONomeDoTexto() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T09")
            assertEquals("T09", h.state.testTextId)
            assertEquals("T09", h.state.exportName)
            h.listen()
            assertTrue(h.state.canExport)
            assertEquals(
                listOf("T09.hypotheses.csv", "T09.level.csv", "T09.trajectory.csv", "T09.events.csv", "T09.meta.txt"),
                h.export().keys.toList(),
            )
        }

    @Test
    fun nomeComTextoLivre_eRecusado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.setExportName("T01 ouvir bem")
            h.settle()
            assertTrue(h.export().isEmpty())
            assertTrue(h.state.exportFailed)
        }

    @Test
    fun hipotesesLidasDeVolta_entregamAsMesmasPalavrasDaSessaoAoVivo() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.partial("ouvir")
            h.partial("ouvir", "bem")
            h.partial("ouvir", "bem", "é")
            h.final("ouvir", "bem", "é", "uma")
            assertEquals(listOf("ouvir", "bem", "é", "uma"), h.state.debug.tail)
            h.pause()
            h.listen()
            h.fake.recognizers
                .first()
                .final(listOf("habilidade"))
            h.settle()
            h.partial("habilidade", "que")
            h.partial("habilidade", "que", "quase")
            h.final("habilidade", "que", "quase", "ninguém")
            assertEquals(listOf("habilidade", "que", "quase", "ninguém"), h.state.debug.tail)
            h.export()
            val rows = ExportFiles.readHypotheses(h.exported.getValue("T01.hypotheses.csv"))
            assertEquals(
                listOf("ouvir", "bem", "é", "uma", "habilidade", "que", "quase", "ninguém"),
                ExportFiles.replayAccepted(rows),
            )
            assertEquals(listOf(1L, 2L), rows.filter { it.accepted }.map { it.session }.distinct())
            assertEquals(1, rows.count { !it.accepted && it.session == 1L })
        }

    @Test
    fun meta_trazTodasAsChaves_eOIdDoModeloIgualAoUuid() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T02")
            h.listen()
            h.export()
            val meta =
                h.exported
                    .getValue("T02.meta.txt")
                    .lines()
                    .filter { it.isNotEmpty() }
                    .associate { it.substringBefore('=') to it.substringAfter('=') }
            assertEquals(ExportFiles.META_KEYS, meta.keys.toList())
            assertEquals(h.modelId, meta["model_id"])
            assertTrue(meta.getValue("model_id").startsWith("vosk-model-small-pt-0.3 sha256="))
            assertEquals("T02", meta["text_id"])
            assertEquals("Normal", meta["preset"])
            assertEquals("off", meta["vocabulary"])
            assertEquals("NewPerSession", meta["recognizer_strategy"])
            assertEquals("mic", meta["audio_source"])
            assertEquals("0.0", meta["level_threshold"])
            assertEquals("375", meta["animation_ms"])
            assertEquals("1234", meta["install_ms"])
            assertEquals("0.3.75", meta["vosk_android_version"])
        }
}
```

Criar `KTT/viewmodel/ReaderLifecycleTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.RecognizerStrategy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D9 ciclo de vida (TRD §Segundo plano; D16), erros na tela (D15) e troca de estratégia com a captura parada (D4). */
class ReaderLifecycleTest {
    @Test
    fun onStopComMudancaDeConfiguracao_naoFechaSessaoNemCaptura() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            val before = h.engine.calls.toList()
            h.controller.onActivityStop(changingConfigurations = true)
            h.controller.onActivityStart()
            h.settle()
            assertEquals(before, h.engine.calls)
            assertEquals(ReaderPhase.Reading, h.state.phase)
        }

    @Test
    fun onStopDeVerdade_fechaSessaoECaptura_eATelaFicaPausada() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.onActivityStop(changingConfigurations = false)
            h.settle()
            assertEquals(listOf("endSession@0", "stopCapture"), h.engine.calls.takeLast(2))
            assertEquals(ReaderPhase.Paused, h.state.phase)
            assertFalse(h.state.capturing)
        }

    @Test
    fun voltaDoSegundoPlano_reabreACaptura_eContinuaPausada() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.controller.onActivityStop(changingConfigurations = false)
            h.controller.onActivityStart()
            h.settle()
            assertEquals("startCapture", h.engine.calls.last())
            assertEquals(ReaderPhase.Paused, h.state.phase)
            assertTrue(h.state.capturing)
            h.listen()
            assertTrue(
                h.engine.calls
                    .last()
                    .startsWith("beginSession"),
            )
        }

    @Test
    fun falhaNoPrepare_mostraOErro_eTentarDeNovoPreparaOutraVez() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.engine.failPrepare = EngineError.ModelCorrupt
            h.listen()
            assertEquals(EngineError.ModelCorrupt, h.state.error)
            assertEquals(ReaderPhase.Idle, h.state.phase)
            h.listen()
            assertEquals(null, h.state.error)
            assertEquals(ReaderPhase.Reading, h.state.phase)
            assertEquals(2, h.engine.calls.count { it == "prepare" })
        }

    @Test
    fun erroDaSessaoAtiva_pausaEMostraOMotivo_erroDeSessaoAntigaEIgnorado() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            h.listen()
            h.fake.recognizers
                .first()
                .error(EngineError.Internal)
            h.settle()
            assertEquals(ReaderPhase.Reading, h.state.phase)
            h.fake.recognizers
                .last()
                .error(EngineError.Internal)
            h.settle()
            assertEquals(ReaderPhase.Paused, h.state.phase)
            assertEquals(EngineError.Internal, h.state.error)
        }

    @Test
    fun erroDaCaptura_pausaEFechaACaptura_eOuvirReabre() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.fake.emitCaptureError(EngineError.MicUnavailable)
            h.settle()
            assertEquals(ReaderPhase.Paused, h.state.phase)
            assertFalse(h.state.capturing)
            assertEquals(EngineError.MicUnavailable, h.state.error)
            h.listen()
            assertEquals(ReaderPhase.Reading, h.state.phase)
            assertEquals(listOf("startCapture", "beginSession@0"), h.engine.calls.takeLast(2))
        }

    @Test
    fun trocarEstrategiaComALeituraParada_recriaOMotor() =
        runTest {
            val h = ReaderHarness(this)
            h.controller.setStrategy(RecognizerStrategy.Reset)
            h.settle()
            assertEquals(2, h.engines.size)
            assertEquals(
                "release",
                h.engines
                    .first()
                    .calls
                    .last(),
            )
            assertEquals(RecognizerStrategy.Reset, h.engine.config.strategy)
            h.load("T01")
            h.listen()
            h.controller.setStrategy(RecognizerStrategy.NewPerSession)
            h.settle()
            assertEquals(2, h.engines.size)
        }

    @Test
    fun vocabularioLigado_vaiParaOBeginSession_comUnkNoFim() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T09")
            h.controller.setVocabulary(true)
            h.listen()
            val vocabulary =
                h.fake.recognizers
                    .single()
                    .vocabulary
            assertEquals("[unk]", vocabulary?.last())
            assertEquals(vocabulary?.size, h.state.debug.vocabularySize)
        }

    @Test
    fun rotacaoComALeituraPausada_naoReabreACaptura() =
        runTest {
            val h = ReaderHarness(this)
            h.load("T01")
            h.listen()
            h.pause()
            val before = h.engine.calls.toList()
            h.controller.onActivityStop(changingConfigurations = true)
            h.controller.onActivityStart()
            h.settle()
            assertEquals(before, h.engine.calls)
            assertEquals(ReaderPhase.Paused, h.state.phase)
            assertTrue(h.state.capturing)
        }
}
```

- [ ] **Passo 2: ver falhar.** FAIL de compilação ("Unresolved reference 'ReaderController'", "'ReaderPhase'", "'EngineConfig'", "'Provenance'").

- [ ] **Passo 3: código.** Criar `KT/viewmodel/ReaderUiState.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.align.AlignParams
import io.github.gigioderico.tribuna.etapa0.align.AlignPresets
import io.github.gigioderico.tribuna.etapa0.align.Move
import io.github.gigioderico.tribuna.etapa0.align.Token
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.EngineState
import io.github.gigioderico.tribuna.etapa0.speech.RecognizerStrategy

/** Fase da tela mínima da etapa0 (só o que ela usa; não é o `ReadingPhase` do app). */
enum class ReaderPhase { Idle, Preparing, Reading, Paused }

/** Presets do PRD (spec 0002, D11; spec 0003, D17: padrão Normal). */
enum class PresetChoice(
    val params: AlignParams,
) {
    Conservador(AlignPresets.conservador),
    Normal(AlignPresets.normal),
    Agil(AlignPresets.agil),
}

/** Fonte de áudio (spec 0003, D8). */
sealed interface AudioChoice {
    data object Microphone : AudioChoice

    data class Wav(
        val name: String,
    ) : AudioChoice
}

/** Ajustes do painel, só em memória (spec 0003, D16: a etapa0 não grava ajustes em disco). */
data class DebugSettings(
    val preset: PresetChoice = PresetChoice.Normal,
    val vocabulary: Boolean = false,
    val strategy: RecognizerStrategy = RecognizerStrategy.NewPerSession,
    val source: AudioChoice = AudioChoice.Microphone,
    /** Limiar de nível; 0 = desligado (D3). */
    val levelThreshold: Float = 0f,
    /** PROVISÓRIO: ponto médio da faixa do R5 (150–600 ms), só na etapa0 (D13; OPR-022). */
    val animationMs: Int = ANIMATION_DEFAULT_MS,
) {
    companion object {
        const val ANIMATION_MIN_MS = 150
        const val ANIMATION_MAX_MS = 600
        const val ANIMATION_DEFAULT_MS = 375
    }
}

/** O que o painel de depuração mostra (spec 0003, D18). Tudo em memória. */
data class DebugInfo(
    val lastPartial: List<String> = emptyList(),
    val lastFinal: List<String> = emptyList(),
    val tail: List<String> = emptyList(),
    val score: Double = 0.0,
    val lastMove: Move? = null,
    val highWater: Int = 0,
    val missStreak: Int = 0,
    val activeSession: Long? = null,
    val engineState: EngineState = EngineState.Idle,
    val level: Float = 0f,
    /** Latência 2: recepção do Partial → cursor publicado. */
    val latency2Ms: Long? = null,
    /** Latência 3: cursor publicado → fim da animação. */
    val latency3Ms: Long? = null,
    val installMs: Long? = null,
    val prepareMs: Long? = null,
    val beginSessionMs: Long? = null,
    /** Início da escuta: beginSession → primeiro parcial não vazio. */
    val firstPartialMs: Long? = null,
    val vocabularySize: Int? = null,
)

data class ReaderUiState(
    val text: String = "",
    val tokens: List<Token> = emptyList(),
    val cursor: Int = 0,
    /** Quando o cursor atual foi publicado (relógio do controlador), para a latência 3. */
    val cursorAtMs: Long = 0L,
    val phase: ReaderPhase = ReaderPhase.Idle,
    val capturing: Boolean = false,
    /** ID do texto de teste idêntico ao texto atual; só com ele a exportação existe (D9). */
    val testTextId: String? = null,
    val testTextIds: List<String> = emptyList(),
    val wavFiles: List<String> = emptyList(),
    val settings: DebugSettings = DebugSettings(),
    val exportName: String = "",
    val canExport: Boolean = false,
    val lastExport: List<String> = emptyList(),
    val exportFailed: Boolean = false,
    val error: EngineError? = null,
    val debug: DebugInfo = DebugInfo(),
)
```

Criar `KT/viewmodel/ReaderController.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.viewmodel

import io.github.gigioderico.tribuna.etapa0.align.AlignConstants
import io.github.gigioderico.tribuna.etapa0.align.AlignState
import io.github.gigioderico.tribuna.etapa0.align.Aligner
import io.github.gigioderico.tribuna.etapa0.align.Move
import io.github.gigioderico.tribuna.etapa0.align.PartialReconciler
import io.github.gigioderico.tribuna.etapa0.align.Tokenizer
import io.github.gigioderico.tribuna.etapa0.export.ExportFiles
import io.github.gigioderico.tribuna.etapa0.export.ExportName
import io.github.gigioderico.tribuna.etapa0.export.ExportSink
import io.github.gigioderico.tribuna.etapa0.export.Recording
import io.github.gigioderico.tribuna.etapa0.export.TestTextCatalog
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.speech.EngineState
import io.github.gigioderico.tribuna.etapa0.speech.ModelInfo
import io.github.gigioderico.tribuna.etapa0.speech.ModelLoadException
import io.github.gigioderico.tribuna.etapa0.speech.RecognizerStrategy
import io.github.gigioderico.tribuna.etapa0.speech.SessionId
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEngine
import io.github.gigioderico.tribuna.etapa0.speech.SpeechEvent
import io.github.gigioderico.tribuna.etapa0.speech.Vocabulary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/** Configuração de construção do motor: estratégia e fonte vêm do construtor do VoskEngine (spec 0003, D4, D8). */
data class EngineConfig(
    val strategy: RecognizerStrategy,
    val source: AudioChoice,
)

/** Ambiente para o `meta.txt` (D11). */
data class Provenance(
    val appVersion: String,
    val voskVersion: String,
    val deviceModel: String,
    val androidVersion: String,
)

/**
 * Lógica da tela mínima da etapa0 (spec 0003, D1–D9; TRD Reconciliação 7, Máquina de estados nas linhas
 * Reading/Paused que a tela usa, Segundo plano). Kotlin puro, testado na JVM com o `FakeSpeechEngine`.
 *
 * Único dono do texto, do cursor e da leitura. O texto vive só aqui, em memória (CLAUDE.md, regra 2).
 * Todo estado muda numa só fila (`processing` com paralelismo 1), fora da thread principal (TRD §Captura e
 * sessões): comandos da tela e eventos do motor nunca correm juntos.
 */
class ReaderController(
    private val scope: CoroutineScope,
    private val engineFactory: (EngineConfig) -> SpeechEngine,
    private val catalog: TestTextCatalog,
    private val modelInfo: () -> ModelInfo?,
    private val exportSink: ExportSink,
    private val provenance: Provenance,
    private val wavFiles: () -> List<String>,
    private val clock: () -> Long,
    processing: CoroutineDispatcher,
    private val io: CoroutineDispatcher,
) {
    private val confined = processing.limitedParallelism(1)
    private val mutableState = MutableStateFlow(ReaderUiState(testTextIds = catalog.ids))
    val state: StateFlow<ReaderUiState> = mutableState.asStateFlow()

    private var engine: SpeechEngine = createEngine()
    private var collector: Job? = null
    private var aligner: Aligner? = null
    private var align: AlignState? = null
    private val reconciler = PartialReconciler()
    private val tail = ArrayDeque<String>()
    private var active: SessionId? = null
    private var lastLevel = 0f
    private var sessionStartedAt = 0L
    private var firstPartialSeen = false
    private var recording: Recording? = null
    private var backgrounded = false
    private var vocabulary: List<String>? = null

    init {
        collectEvents()
    }

    // ---- comandos da tela ----

    fun setText(text: String) =
        enqueue {
            if (mutableState.value.phase != ReaderPhase.Idle) return@enqueue
            val id = catalog.identify(text)
            recording = null
            vocabulary = null
            mutableState.update {
                it.copy(
                    text = text,
                    tokens = Tokenizer.tokenize(text),
                    cursor = 0,
                    testTextId = id,
                    exportName = id.orEmpty(),
                    canExport = false,
                    lastExport = emptyList(),
                )
            }
        }

    fun loadTestText(id: String) {
        val text = catalog.text(id) ?: return
        setText(text)
    }

    fun onListen() =
        enqueue {
            when (mutableState.value.phase) {
                ReaderPhase.Idle -> startReading()
                ReaderPhase.Paused -> resume(fromTap = true)
                else -> Unit
            }
        }

    fun onPause() =
        enqueue {
            if (mutableState.value.phase != ReaderPhase.Reading) return@enqueue
            endSession()
            recording?.event(clock(), Recording.PAUSE_TAP)
            mutableState.update { it.copy(phase = ReaderPhase.Paused) }
        }

    /** Encerra a gravação: fecha a sessão e a captura; a exportação fica disponível. */
    fun onStopReading() =
        enqueue {
            val phase = mutableState.value.phase
            if (phase != ReaderPhase.Reading && phase != ReaderPhase.Paused) return@enqueue
            endSession()
            stopCapture()
            mutableState.update { it.copy(phase = ReaderPhase.Idle) }
        }

    /** Toque em palavra (D10; TRD Máquina de estados, Reading e Paused): endSession, reposition, beginSession. */
    fun onWordTapped(index: Int) =
        enqueue {
            val phase = mutableState.value.phase
            if (phase != ReaderPhase.Reading && phase != ReaderPhase.Paused) return@enqueue
            endSession()
            reposition(index)
            recording?.event(clock(), Recording.WORD_TAP, index.toString())
            resume(fromTap = false)
        }

    /** `onStop` da Activity (TRD §Segundo plano; D16). */
    fun onActivityStop(changingConfigurations: Boolean) =
        enqueue {
            if (changingConfigurations) return@enqueue
            val phase = mutableState.value.phase
            if (phase != ReaderPhase.Reading && phase != ReaderPhase.Paused) return@enqueue
            endSession()
            stopCapture()
            backgrounded = true
            mutableState.update { it.copy(phase = ReaderPhase.Paused) }
        }

    /** `onStart` da Activity: na volta do segundo plano, a leitura continua pausada e a captura reabre. */
    fun onActivityStart() =
        enqueue {
            if (!backgrounded) return@enqueue
            backgrounded = false
            if (mutableState.value.phase == ReaderPhase.Paused) startCapture()
        }

    fun onPermissionDenied() = enqueue { mutableState.update { it.copy(error = EngineError.PermissionRevoked) } }

    fun setPreset(preset: PresetChoice) = updateIdleSettings { it.copy(preset = preset) }

    fun setVocabulary(enabled: Boolean) = updateIdleSettings { it.copy(vocabulary = enabled) }

    fun setStrategy(strategy: RecognizerStrategy) = updateIdleSettings(recreateEngine = true) { it.copy(strategy = strategy) }

    fun setSource(source: AudioChoice) = updateIdleSettings(recreateEngine = true) { it.copy(source = source) }

    fun setLevelThreshold(threshold: Float) =
        enqueue { mutableState.update { it.copy(settings = it.settings.copy(levelThreshold = threshold.coerceIn(0f, 1f))) } }

    fun setAnimationMs(ms: Int) =
        enqueue {
            val clamped = ms.coerceIn(DebugSettings.ANIMATION_MIN_MS, DebugSettings.ANIMATION_MAX_MS)
            mutableState.update { it.copy(settings = it.settings.copy(animationMs = clamped)) }
        }

    fun setExportName(name: String) = enqueue { mutableState.update { it.copy(exportName = name) } }

    fun refreshWavFiles() = enqueue { mutableState.update { it.copy(wavFiles = wavFiles()) } }

    /** Fim da animação da tela para o cursor `cursor`: latência 3 (D18). */
    fun onScrollSettled(cursor: Int) =
        enqueue {
            val current = mutableState.value
            if (current.cursor != cursor) return@enqueue
            mutableState.update { it.copy(debug = it.debug.copy(latency3Ms = clock() - current.cursorAtMs)) }
        }

    /** Grava os cinco arquivos (D7) se o texto é de teste e o nome é válido. */
    fun onExport() =
        enqueue {
            val rec = recording ?: return@enqueue
            val current = mutableState.value
            if (!ExportName.isValid(current.exportName, rec.textId)) {
                mutableState.update { it.copy(exportFailed = true, lastExport = emptyList()) }
                return@enqueue
            }
            val files = ExportFiles.build(rec)
            val written =
                try {
                    withContext(io) { exportSink.write(current.exportName, files) }
                } catch (e: IOException) {
                    null
                }
            mutableState.update { it.copy(lastExport = written.orEmpty(), exportFailed = written == null) }
        }

    /** Fim do ViewModel. */
    fun close() {
        collector?.cancel()
        engine.release()
    }

    // ---- leitura ----

    private suspend fun startReading() {
        val current = mutableState.value
        if (current.tokens.isEmpty()) return
        mutableState.update { it.copy(phase = ReaderPhase.Preparing, error = null, lastExport = emptyList()) }
        val started = clock()
        val prepared = engine.prepare()
        val prepareMs = clock() - started
        val info = modelInfo()
        mutableState.update { it.copy(debug = it.debug.copy(prepareMs = prepareMs, installMs = info?.installMs)) }
        if (prepared.isFailure) {
            val cause = (prepared.exceptionOrNull() as? ModelLoadException)?.error ?: EngineError.Internal
            mutableState.update { it.copy(phase = ReaderPhase.Idle, error = cause) }
            return
        }
        if (!startCapture()) {
            mutableState.update { it.copy(phase = ReaderPhase.Idle) }
            return
        }
        recording = current.testTextId?.let { Recording(it, metaFor(current, info, prepareMs), originMs = clock()) }
        aligner = Aligner(current.tokens, current.settings.preset.params)
        reposition(0)
        beginSession()
        mutableState.update { it.copy(phase = ReaderPhase.Reading, canExport = recording != null) }
    }

    private fun resume(fromTap: Boolean) {
        if (!mutableState.value.capturing && !startCapture()) {
            mutableState.update { it.copy(phase = ReaderPhase.Paused) }
            return
        }
        beginSession()
        if (fromTap) recording?.event(clock(), Recording.RESUME_TAP)
        mutableState.update { it.copy(phase = ReaderPhase.Reading, error = null) }
    }

    private fun startCapture(): Boolean {
        engine.startCapture()
        val ok = engine.state.value == EngineState.Capturing
        mutableState.update { it.copy(capturing = ok) }
        return ok
    }

    private fun stopCapture() {
        if (!mutableState.value.capturing) return
        val engineState = engine.state.value
        if (engineState == EngineState.Capturing || engineState == EngineState.Recognizing) engine.stopCapture()
        mutableState.update { it.copy(capturing = false) }
    }

    private fun beginSession() {
        reconciler.reset()
        tail.clear()
        val current = mutableState.value
        val words = if (current.settings.vocabulary) vocabularyFor(current) else null
        val started = clock()
        val id = engine.beginSession(words)
        val now = clock()
        active = id
        sessionStartedAt = now
        firstPartialSeen = false
        mutableState.update {
            it.copy(
                debug =
                    it.debug.copy(
                        activeSession = id.value,
                        beginSessionMs = now - started,
                        firstPartialMs = null,
                        tail = emptyList(),
                        vocabularySize = words?.size,
                    ),
            )
        }
    }

    private fun endSession() {
        if (active == null) return
        engine.endSession()
        active = null
        mutableState.update { it.copy(debug = it.debug.copy(activeSession = null)) }
    }

    private fun reposition(index: Int) {
        val currentAligner = aligner ?: return
        // A cauda e o reconciliador zeram no beginSession que sempre vem depois (0002 D1).
        publish(currentAligner.reposition(index), clock())
    }

    private fun vocabularyFor(current: ReaderUiState): List<String> =
        vocabulary ?: Vocabulary.build(current.text, current.tokens, modelInfo()?.lexicon?.value).also { vocabulary = it }

    // ---- eventos do motor ----

    private fun collectEvents() {
        val source = engine
        collector = scope.launch(confined) { source.events.collect(::handle) }
    }

    private fun handle(event: SpeechEvent) {
        val now = clock()
        when (event) {
            is SpeechEvent.Level -> onLevel(now, event)
            is SpeechEvent.Partial -> onHypothesis(now, event, event.session, event.words, isFinal = false)
            is SpeechEvent.Final -> onHypothesis(now, event, event.session, event.words, isFinal = true)
            is SpeechEvent.Error -> onError(now, event)
        }
    }

    private fun onLevel(
        now: Long,
        event: SpeechEvent.Level,
    ) {
        lastLevel = event.rms
        recording?.level(now, event.rms)
        mutableState.update { it.copy(debug = it.debug.copy(level = event.rms, engineState = engine.state.value)) }
    }

    private fun onHypothesis(
        now: Long,
        event: SpeechEvent,
        session: SessionId,
        words: List<String>,
        isFinal: Boolean,
    ) {
        val threshold = mutableState.value.settings.levelThreshold
        // Descarta evento de sessão que não é a ativa antes do reconciliador (TRD Reconciliação 7; regra 7).
        val fromActive = session == active
        // Limiar de nível só para parciais (TRD §Captura e sessões; D3, D5).
        val passesLevel = isFinal || lastLevel >= threshold
        val accepted = fromActive && passesLevel
        recording?.hypothesis(now, event, accepted)
        mutableState.update {
            it.copy(
                debug = if (isFinal) it.debug.copy(lastFinal = words) else it.debug.copy(lastPartial = words),
            )
        }
        if (!accepted) return
        if (!isFinal && !firstPartialSeen && words.isNotEmpty()) {
            firstPartialSeen = true
            mutableState.update { it.copy(debug = it.debug.copy(firstPartialMs = now - sessionStartedAt)) }
        }
        val fresh = if (isFinal) reconciler.onFinal(words) else reconciler.onPartial(words)
        if (fresh.isEmpty()) return
        fresh.forEach(tail::addLast)
        val keep = maxOf(mutableState.value.settings.preset.params.k, AlignConstants.RECOVERY_WORDS)
        while (tail.size > keep) tail.removeFirst()
        val currentAligner = aligner ?: return
        val currentAlign = align ?: return
        publish(currentAligner.advance(currentAlign, tail.toList()), now)
    }

    private fun onError(
        now: Long,
        event: SpeechEvent.Error,
    ) {
        // Erro da captura (sem sessão) ou da sessão ativa; erro de sessão antiga é descartado (regra 7).
        val fromCapture = event.session == null
        val accepted = fromCapture || event.session == active
        recording?.hypothesis(now, event, accepted)
        if (!accepted) return
        val phase = mutableState.value.phase
        endSession()
        if (fromCapture) stopCapture()
        // Na tela, um erro pausa, mostra o motivo e oferece "Tentar de novo" (D15).
        val next = if (phase == ReaderPhase.Reading || phase == ReaderPhase.Paused) ReaderPhase.Paused else phase
        mutableState.update { it.copy(phase = next, error = event.cause) }
    }

    /** Publica o novo estado do alinhador; grava a trajetória a cada reposition e a cada mudança de cursor (D7). */
    private fun publish(
        next: AlignState,
        receivedAt: Long,
    ) {
        val previous = align
        align = next
        if (next.lastMove == Move.Reposition || next.cursor != previous?.cursor) {
            recording?.trajectory(clock(), next.cursor, next.lastMove, next.score)
        }
        val publishedAt = clock()
        mutableState.update {
            it.copy(
                cursor = next.cursor,
                cursorAtMs = if (next.cursor != it.cursor || next.lastMove == Move.Reposition) publishedAt else it.cursorAtMs,
                debug =
                    it.debug.copy(
                        tail = tail.toList(),
                        score = next.score,
                        lastMove = next.lastMove,
                        highWater = next.highWater,
                        missStreak = next.missStreak,
                        latency2Ms = if (next.lastMove == Move.Reposition) it.debug.latency2Ms else publishedAt - receivedAt,
                    ),
            )
        }
    }

    // ---- ajustes ----

    private fun updateIdleSettings(
        recreateEngine: Boolean = false,
        change: (DebugSettings) -> DebugSettings,
    ) = enqueue {
        if (mutableState.value.phase != ReaderPhase.Idle) return@enqueue
        mutableState.update { it.copy(settings = change(it.settings)) }
        if (recreateEngine) replaceEngine()
    }

    private fun replaceEngine() {
        collector?.cancel()
        engine.release()
        engine = createEngine()
        collectEvents()
    }

    private fun createEngine(): SpeechEngine {
        val settings = mutableState.value.settings
        return engineFactory(EngineConfig(settings.strategy, settings.source))
    }

    private fun metaFor(
        current: ReaderUiState,
        info: ModelInfo?,
        prepareMs: Long,
    ): Map<String, String> {
        val settings = current.settings
        val source = settings.source
        return mapOf(
            "app_version" to provenance.appVersion,
            "vosk_android_version" to provenance.voskVersion,
            "model_id" to info?.id.orEmpty(),
            "text_id" to current.testTextId.orEmpty(),
            "preset" to settings.preset.name,
            "vocabulary" to if (settings.vocabulary) "on" else "off",
            "recognizer_strategy" to settings.strategy.name,
            "audio_source" to if (source is AudioChoice.Wav) "wav" else "mic",
            "wav_name" to (source as? AudioChoice.Wav)?.name.orEmpty(),
            "level_threshold" to settings.levelThreshold.toString(),
            "animation_ms" to settings.animationMs.toString(),
            "install_ms" to info?.installMs?.toString().orEmpty(),
            "prepare_ms" to prepareMs.toString(),
            "device_model" to provenance.deviceModel,
            "android_version" to provenance.androidVersion,
        )
    }

    /** Toda mudança de estado passa por aqui: uma fila só, fora da thread principal. */
    private fun enqueue(block: suspend () -> Unit) {
        scope.launch(confined) { block() }
    }
}
```

- [ ] **Passo 4: ver passar.** `ReaderSessionTest` 3, `ReaderReadingTest` 9, `ReaderTapTest` 4, `ReaderExportTest` 5, `ReaderLifecycleTest` 9 verdes. Se o `t01PorParciaisEFinais_…` parar antes do último token, não ajuste o teste: confira se o `ReaderController` mantém a cauda com `max(k, 4)` palavras e zera reconciliador e cauda só no `beginSession`.

- [ ] **Passo 5: commit.**

```bash
git add etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/viewmodel \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/viewmodel/ReaderUiState.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/viewmodel/ReaderController.kt
git commit -m "feat(viewmodel): controlador da tela mínima com descarte por sessão, limiar, toque e exportação (TRD Reconciliação 7; §Segundo plano; regra 7)"
```

---

### Tarefa 13 (interface): `HitTest` (D10)

**Arquivos:** criar `KTT/ui/HitTestTest.kt`, `KT/ui/HitTest.kt`.

- [ ] **Passo 1: teste primeiro.** Criar `KTT/ui/HitTestTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.ui

import io.github.gigioderico.tribuna.etapa0.TestFiles
import io.github.gigioderico.tribuna.etapa0.align.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Toque em palavra ou vazio (TRD §Hit-test 1–4; spec 0003, D10): parte pura. */
class HitTestTest {
    private val text = TestFiles.text("T01")
    private val tokens = Tokenizer.tokenize(text)

    @Test
    fun caractereDeCadaToken_achaOToken() {
        for (token in tokens) {
            assertEquals(token.index, HitTest.tokenAt(token.display.first, tokens))
            assertEquals(token.index, HitTest.tokenAt(token.display.last, tokens))
        }
    }

    @Test
    fun espacoEntrePalavras_naoEToken() {
        val gap = tokens[0].display.last + 1
        assertEquals(' ', text[gap])
        assertNull(HitTest.tokenAt(gap, tokens))
        assertNull(HitTest.tokenAt(text.length + 10, tokens))
    }

    private val box = WordBox(left = 100f, top = 50f, right = 180f, bottom = 90f)

    @Test
    fun dentroDoRetanguloAmpliado_contaComoPalavra() {
        assertEquals(true, HitTest.inside(95f, 45f, box, slop = 8f))
        assertEquals(true, HitTest.inside(188f, 98f, box, slop = 8f))
    }

    @Test
    fun foraDoRetanguloAmpliado_eVazio() {
        assertEquals(false, HitTest.inside(91f, 70f, box, slop = 8f))
        assertEquals(false, HitTest.inside(140f, 99f, box, slop = 8f))
        assertEquals(false, HitTest.inside(189f, 70f, box, slop = 8f))
        assertEquals(false, HitTest.inside(140f, 41f, box, slop = 8f))
    }

    @Test
    fun uniaoDosCaracteres_eORetanguloDaPalavra() {
        val union = HitTest.union(listOf(WordBox(0f, 10f, 5f, 20f), WordBox(5f, 12f, 11f, 22f)))
        assertEquals(WordBox(0f, 10f, 11f, 22f), union)
        assertNull(HitTest.union(emptyList()))
    }

    @Test
    fun toqueLongeDoTexto_comOCaractereMaisProximo_eVazio() {
        // getOffsetForPosition devolve o caractere mais próximo mesmo longe do texto (TRD §Hit-test 1).
        val charBox = { _: Int -> WordBox(0f, 0f, 10f, 20f) }
        assertEquals(0, HitTest.tappedToken(5f, 10f, offset = 0, tokens = tokens, boxOf = charBox, slop = 8f))
        assertNull(HitTest.tappedToken(5f, 300f, offset = 0, tokens = tokens, boxOf = charBox, slop = 8f))
    }
}
```

- [ ] **Passo 2: ver falhar.** FAIL de compilação ("Unresolved reference 'HitTest'", "'WordBox'").

- [ ] **Passo 3: código.** Criar `KT/ui/HitTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.ui

import io.github.gigioderico.tribuna.etapa0.align.Token

/** Retângulo de uma palavra na tela, em px. */
data class WordBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/**
 * Toque em palavra ou vazio (TRD Renderização e toque §Hit-test 1–4; CLAUDE.md "Erros comuns"): o caractere
 * de `getOffsetForPosition` só conta se o toque cair no retângulo da palavra (união dos `getBoundingBox`)
 * ampliado em 8 dp. Parte pura, testada na JVM; a tela fornece o layout.
 */
object HitTest {
    /** Token cujo intervalo exibido contém `offset` (busca binária, TRD §Hit-test 4), ou `null`. */
    fun tokenAt(
        offset: Int,
        tokens: List<Token>,
    ): Int? {
        var low = 0
        var high = tokens.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val range = tokens[mid].display
            when {
                offset < range.first -> high = mid - 1
                offset > range.last -> low = mid + 1
                else -> return mid
            }
        }
        return null
    }

    /** União dos retângulos dos caracteres; `null` se não houver nenhum. */
    fun union(boxes: List<WordBox>): WordBox? =
        boxes.reduceOrNull { a, b ->
            WordBox(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
        }

    /** O ponto está no retângulo ampliado de `slop` px para todos os lados? */
    fun inside(
        x: Float,
        y: Float,
        box: WordBox,
        slop: Float,
    ): Boolean = x >= box.left - slop && x <= box.right + slop && y >= box.top - slop && y <= box.bottom + slop

    /** Índice do token tocado, ou `null` (vazio). */
    fun tappedToken(
        x: Float,
        y: Float,
        offset: Int,
        tokens: List<Token>,
        boxOf: (Int) -> WordBox,
        slop: Float,
    ): Int? {
        val index = tokenAt(offset, tokens) ?: return null
        val box = union(tokens[index].display.map(boxOf)) ?: return null
        return if (inside(x, y, box, slop)) index else null
    }
}
```

- [ ] **Passo 4: ver passar.** `HitTestTest` 6 verdes.

- [ ] **Passo 5: commit.**

```bash
git add etapa0/app/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/ui/HitTestTest.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/ui/HitTest.kt
git commit -m "feat(ui): toque em palavra com getBoundingBox + 8 dp (TRD §Hit-test)"
```

---

### Tarefa 14 (interface): tela mínima e painel

Sem teste JVM (Compose UI no aparelho é da 0005); a lógica está no controlador e no `HitTest`. Verificação: compilação, lint, ktlint e os greps.

**Arquivos:** substituir `app/src/main/res/values/strings.xml`; criar `KT/ui/PromptText.kt`, `KT/ui/DebugPanel.kt`, `KT/ui/ReaderScreen.kt`.

- [ ] **Passo 1: código.** Substituir `app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Tribuna Etapa 0</string>
    <string name="text_label">Texto</string>
    <string name="test_texts">Textos de teste</string>
    <string name="listen">Ouvir</string>
    <string name="pause">Pausar</string>
    <string name="stop">Parar</string>
    <string name="debug_panel">Depuração</string>
    <string name="try_again">Tentar de novo</string>
    <string name="preparing_model">Preparando modelo…</string>
    <string name="error_model_missing">Modelo de voz ausente.</string>
    <string name="error_model_corrupt">Falha ao preparar o modelo de voz.</string>
    <string name="error_mic_unavailable">Microfone ou arquivo de áudio indisponível.</string>
    <string name="error_permission">Sem permissão de microfone.</string>
    <string name="error_internal">Erro no reconhecimento de voz.</string>
    <string name="preset">Sensibilidade</string>
    <string name="preset_conservador">Conservador</string>
    <string name="preset_normal">Normal</string>
    <string name="preset_agil">Ágil</string>
    <string name="vocabulary">Vocabulário restrito</string>
    <string name="strategy">Reconhecedor</string>
    <string name="strategy_new">Novo por sessão</string>
    <string name="strategy_reset">reset()</string>
    <string name="source">Fonte de áudio</string>
    <string name="source_mic">Microfone</string>
    <string name="refresh_wav">Atualizar WAV</string>
    <string name="level_threshold">Limiar de nível: %1$.3f</string>
    <string name="animation">Animação: %1$d ms</string>
    <string name="export_name">Nome da exportação</string>
    <string name="export">Exportar</string>
    <string name="export_disabled">Exportação só com texto de teste idêntico.</string>
    <string name="export_failed">Exportação recusada: nome inválido ou falha ao gravar.</string>
    <string name="export_done">Exportado: %1$s</string>
    <string name="debug_partial">Parcial: %1$s</string>
    <string name="debug_final">Final: %1$s</string>
    <string name="debug_tail">Cauda: %1$s</string>
    <string name="debug_align">cursor %1$d · highWater %2$d · missStreak %3$d · %4$s · score %5$.3f</string>
    <string name="debug_engine">sessão %1$s · motor %2$s · nível %3$.3f</string>
    <string name="debug_latency">latência 2: %1$s ms · latência 3: %2$s ms</string>
    <string name="debug_times">instalação %1$s ms · prepare %2$s ms · beginSession %3$s ms · 1º parcial %4$s ms</string>
    <string name="debug_vocabulary">vocabulário: %1$s palavras</string>
</resources>
```

Criar `KT/ui/PromptText.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderUiState
import kotlin.math.roundToInt

/**
 * O texto inteiro num só `BasicText` com a palavra atual destacada (TRD Renderização e toque §PromptText,
 * mínimo da etapa0). O cursor conduz a rolagem: cada cursor novo anima até a linha da palavra ficar na linha de
 * leitura (30% da altura, padrão do A10), com a duração do painel; um cursor novo no meio cancela e redireciona a animação (TRD §Rolagem).
 */
@Composable
fun PromptText(
    state: ReaderUiState,
    onWordTapped: (Int) -> Unit,
    onScrollSettled: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var viewport by remember { mutableIntStateOf(0) }
    val slop = with(LocalDensity.current) { TOUCH_SLOP.toPx() }
    val colors = MaterialTheme.colorScheme
    val annotated =
        remember(state.text, state.tokens, state.cursor, colors) {
            buildAnnotatedString {
                append(state.text)
                val token = state.tokens.getOrNull(state.cursor) ?: return@buildAnnotatedString
                addStyle(SpanStyle(color = colors.onSurface.copy(alpha = READ_ALPHA)), 0, token.display.first)
                addStyle(SpanStyle(color = colors.onPrimary, background = colors.primary), token.display.first, token.display.last + 1)
            }
        }
    LaunchedEffect(state.cursor, state.cursorAtMs, layout, viewport) {
        val measured = layout ?: return@LaunchedEffect
        val token = state.tokens.getOrNull(state.cursor) ?: return@LaunchedEffect
        val line = measured.getLineForOffset(token.display.first)
        val target = (measured.getLineTop(line) - viewport * READING_LINE).roundToInt().coerceIn(0, scroll.maxValue)
        scroll.animateScrollTo(target, tween(durationMillis = state.settings.animationMs))
        onScrollSettled(state.cursor)
    }
    Box(modifier = modifier.onSizeChanged { viewport = it.height }.verticalScroll(scroll)) {
        BasicText(
            text = annotated,
            style = MaterialTheme.typography.headlineSmall.copy(color = colors.onSurface, lineHeight = 36.sp),
            onTextLayout = { layout = it },
            modifier =
                Modifier.fillMaxWidth().pointerInput(state.tokens) {
                    detectTapGestures { position ->
                        val measured = layout ?: return@detectTapGestures
                        val index =
                            HitTest.tappedToken(
                                x = position.x,
                                y = position.y,
                                offset = measured.getOffsetForPosition(position),
                                tokens = state.tokens,
                                boxOf = { char ->
                                    val rect = measured.getBoundingBox(char)
                                    WordBox(rect.left, rect.top, rect.right, rect.bottom)
                                },
                                slop = slop,
                            )
                        if (index != null) onWordTapped(index)
                    }
                },
        )
    }
}

/** Retângulo da palavra ampliado em 8 dp (TRD §Hit-test 2). */
private val TOUCH_SLOP = 8.dp

/** Opacidade do texto já lido: padrão do PRD A9 (40%). */
private const val READ_ALPHA = 0.4f

/** Linha de leitura: padrão do PRD A10 (30% da altura). */
private const val READING_LINE = 0.3f
```

Criar `KT/ui/DebugPanel.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.gigioderico.tribuna.etapa0.R
import io.github.gigioderico.tribuna.etapa0.speech.RecognizerStrategy
import io.github.gigioderico.tribuna.etapa0.viewmodel.AudioChoice
import io.github.gigioderico.tribuna.etapa0.viewmodel.DebugSettings
import io.github.gigioderico.tribuna.etapa0.viewmodel.PresetChoice
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderController
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderPhase
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderUiState

/**
 * Painel de depuração (TRD Etapa 0, item 4; spec 0003, D18): hipóteses brutas, cauda, score, movimento,
 * highWater, latências e tempos, só em memória (TRD §Onde o texto vive). Controles de D3–D5, D8, D13 e a
 * exportação (D7, D9). Ajustes do motor só com a leitura parada.
 */
@Composable
fun DebugPanel(
    state: ReaderUiState,
    controller: ReaderController,
    modifier: Modifier = Modifier,
) {
    val idle = state.phase == ReaderPhase.Idle
    val debug = state.debug
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val small = MaterialTheme.typography.bodySmall
        Text(stringResource(R.string.debug_partial, debug.lastPartial.joinToString(" ")), style = small)
        Text(stringResource(R.string.debug_final, debug.lastFinal.joinToString(" ")), style = small)
        Text(stringResource(R.string.debug_tail, debug.tail.joinToString(" ")), style = small)
        Text(
            stringResource(
                R.string.debug_align,
                state.cursor,
                debug.highWater,
                debug.missStreak,
                debug.lastMove?.name.orEmpty(),
                debug.score,
            ),
            style = small,
        )
        Text(
            stringResource(R.string.debug_engine, debug.activeSession?.toString() ?: "-", debug.engineState.name, debug.level),
            style = small,
        )
        Text(stringResource(R.string.debug_latency, debug.latency2Ms.orDash(), debug.latency3Ms.orDash()), style = small)
        Text(
            stringResource(
                R.string.debug_times,
                debug.installMs.orDash(),
                debug.prepareMs.orDash(),
                debug.beginSessionMs.orDash(),
                debug.firstPartialMs.orDash(),
            ),
            style = small,
        )
        Text(stringResource(R.string.debug_vocabulary, debug.vocabularySize?.toString() ?: "-"), style = small)

        Text(stringResource(R.string.preset), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PresetChoice.entries.forEach { preset ->
                FilterChip(
                    selected = state.settings.preset == preset,
                    onClick = { controller.setPreset(preset) },
                    enabled = idle,
                    label = { Text(stringResource(presetLabel(preset))) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.vocabulary))
            Switch(checked = state.settings.vocabulary, onCheckedChange = controller::setVocabulary, enabled = idle)
        }
        Text(stringResource(R.string.strategy), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RecognizerStrategy.entries.forEach { strategy ->
                FilterChip(
                    selected = state.settings.strategy == strategy,
                    onClick = { controller.setStrategy(strategy) },
                    enabled = idle,
                    label = { Text(stringResource(strategyLabel(strategy))) },
                )
            }
        }
        Text(stringResource(R.string.source), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = state.settings.source == AudioChoice.Microphone,
                onClick = { controller.setSource(AudioChoice.Microphone) },
                enabled = idle,
                label = { Text(stringResource(R.string.source_mic)) },
            )
            OutlinedButton(onClick = controller::refreshWavFiles, enabled = idle) { Text(stringResource(R.string.refresh_wav)) }
        }
        state.wavFiles.forEach { name ->
            FilterChip(
                selected = state.settings.source == AudioChoice.Wav(name),
                onClick = { controller.setSource(AudioChoice.Wav(name)) },
                enabled = idle,
                label = { Text(name) },
            )
        }
        Text(stringResource(R.string.level_threshold, state.settings.levelThreshold))
        Slider(value = state.settings.levelThreshold, onValueChange = controller::setLevelThreshold, valueRange = 0f..1f)
        Text(stringResource(R.string.animation, state.settings.animationMs))
        Slider(
            value = state.settings.animationMs.toFloat(),
            onValueChange = { controller.setAnimationMs(it.toInt()) },
            valueRange = DebugSettings.ANIMATION_MIN_MS.toFloat()..DebugSettings.ANIMATION_MAX_MS.toFloat(),
        )

        if (state.testTextId == null) {
            Text(stringResource(R.string.export_disabled), style = small)
        } else {
            OutlinedTextField(
                value = state.exportName,
                onValueChange = controller::setExportName,
                label = { Text(stringResource(R.string.export_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = controller::onExport, enabled = state.canExport) { Text(stringResource(R.string.export)) }
            OutlinedButton(onClick = controller::onStopReading, enabled = !idle) { Text(stringResource(R.string.stop)) }
        }
        if (state.exportFailed) Text(stringResource(R.string.export_failed), color = MaterialTheme.colorScheme.error)
        if (state.lastExport.isNotEmpty()) Text(stringResource(R.string.export_done, state.lastExport.joinToString()), style = small)
    }
}

private fun Long?.orDash(): String = this?.toString() ?: "-"

private fun presetLabel(preset: PresetChoice): Int =
    when (preset) {
        PresetChoice.Conservador -> R.string.preset_conservador
        PresetChoice.Normal -> R.string.preset_normal
        PresetChoice.Agil -> R.string.preset_agil
    }

private fun strategyLabel(strategy: RecognizerStrategy): Int =
    when (strategy) {
        RecognizerStrategy.NewPerSession -> R.string.strategy_new
        RecognizerStrategy.Reset -> R.string.strategy_reset
    }
```

Criar `KT/ui/ReaderScreen.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gigioderico.tribuna.etapa0.R
import io.github.gigioderico.tribuna.etapa0.speech.EngineError
import io.github.gigioderico.tribuna.etapa0.viewmodel.AudioChoice
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderController
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderPhase
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderUiState

/** Liga a tela ao controlador e pede o microfone na hora do Ouvir (TRD §Microfone). */
@Composable
fun ReaderRoute(controller: ReaderController) {
    val state by controller.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) controller.onListen() else controller.onPermissionDenied()
        }
    ReaderScreen(
        state = state,
        controller = controller,
        onListen = {
            val needsMic = state.settings.source == AudioChoice.Microphone
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (needsMic && !granted) permission.launch(Manifest.permission.RECORD_AUDIO) else controller.onListen()
        },
    )
}

/** Tela mínima da Etapa 0 (TRD Etapa 0, item 1): texto, Ouvir, Pausar e a palavra atual destacada. */
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    controller: ReaderController,
    onListen: () -> Unit,
) {
    var showPanel by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.phase == ReaderPhase.Idle) {
                Text(stringResource(R.string.test_texts), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.testTextIds) { id ->
                        AssistChip(onClick = { controller.loadTestText(id) }, label = { Text(id) })
                    }
                }
                OutlinedTextField(
                    value = state.text,
                    onValueChange = controller::setText,
                    label = { Text(stringResource(R.string.text_label)) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            } else {
                PromptText(
                    state = state,
                    onWordTapped = controller::onWordTapped,
                    onScrollSettled = controller::onScrollSettled,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
            if (state.phase == ReaderPhase.Preparing) Text(stringResource(R.string.preparing_model))
            state.error?.let { error ->
                Text(stringResource(errorText(error)), color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val listenLabel = if (state.error != null) R.string.try_again else R.string.listen
                Button(onClick = onListen, enabled = state.phase == ReaderPhase.Idle || state.phase == ReaderPhase.Paused) {
                    Text(stringResource(listenLabel))
                }
                Button(onClick = controller::onPause, enabled = state.phase == ReaderPhase.Reading) {
                    Text(stringResource(R.string.pause))
                }
                OutlinedButton(onClick = { showPanel = !showPanel }) { Text(stringResource(R.string.debug_panel)) }
            }
            if (showPanel) DebugPanel(state = state, controller = controller, modifier = Modifier.weight(1f))
        }
    }
}

private fun errorText(error: EngineError): Int =
    when (error) {
        EngineError.ModelMissing -> R.string.error_model_missing
        EngineError.ModelCorrupt -> R.string.error_model_corrupt
        EngineError.MicUnavailable -> R.string.error_mic_unavailable
        EngineError.PermissionRevoked -> R.string.error_permission
        EngineError.Internal -> R.string.error_internal
    }
```

- [ ] **Passo 2: verificar.** `./gradlew :app:compileDebugKotlin lint ktlintCheck --console=plain` verde. `grep -rnE "rememberSaveable|SavedStateHandle" app/src/main` vazio.

- [ ] **Passo 3: commit.**

```bash
git add etapa0/app/src/main/res/values/strings.xml etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/ui
git commit -m "feat(ui): tela mínima com texto, Ouvir, Pausar, palavra destacada e painel de depuração (TRD Etapa 0 itens 1 e 4)"
```

---

### Tarefa 15 (interface): `ReaderViewModel` e `MainActivity`

Depois do commit da Tarefa 11 (precisa de `VoskEngine`, `MicAudioSource`, `WavAudioSource`, `VoskModels`, `AndroidAssetTree`, `ExportWriter`).

**Arquivos:** criar `KT/viewmodel/ReaderViewModel.kt`; substituir `KT/MainActivity.kt`.

- [ ] **Passo 1: código.** Criar `KT/viewmodel/ReaderViewModel.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.viewmodel

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.gigioderico.tribuna.etapa0.BuildConfig
import io.github.gigioderico.tribuna.etapa0.export.ExportWriter
import io.github.gigioderico.tribuna.etapa0.export.TestTextCatalog
import io.github.gigioderico.tribuna.etapa0.speech.AndroidAssetTree
import io.github.gigioderico.tribuna.etapa0.speech.MicAudioSource
import io.github.gigioderico.tribuna.etapa0.speech.VoskEngine
import io.github.gigioderico.tribuna.etapa0.speech.VoskModels
import io.github.gigioderico.tribuna.etapa0.speech.WavAudioSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * ViewModel da tela mínima: sobrevive à rotação e guarda o [ReaderController], onde o texto vive em memória
 * (TRD §Onde o texto vive; spec 0003, D16). Nada vai para o estado salvo da Activity.
 */
class ReaderViewModel(
    createController: (CoroutineScope) -> ReaderController,
) : ViewModel() {
    val controller: ReaderController = createController(viewModelScope)

    override fun onCleared() {
        controller.close()
    }

    companion object {
        private const val EXPORT_DIR = "exportacoes"
        private const val WAV_DIR = "wav"

        /** Monta as dependências reais (Vosk, assets, diretórios externos do app). */
        fun factory(context: Context) =
            viewModelFactory {
                initializer {
                    val app = context.applicationContext
                    val wavDir = app.getExternalFilesDir(WAV_DIR)
                    ReaderViewModel { scope ->
                        ReaderController(
                            scope = scope,
                            engineFactory = { config ->
                                VoskEngine(
                                    modelLoader = VoskModels.loader(app),
                                    audioSource = {
                                        when (val source = config.source) {
                                            AudioChoice.Microphone -> MicAudioSource(app)
                                            is AudioChoice.Wav -> WavAudioSource(File(wavDir ?: app.filesDir, source.name))
                                        }
                                    },
                                    strategy = config.strategy,
                                )
                            },
                            catalog = TestTextCatalog.fromAssets(AndroidAssetTree(app.assets)),
                            modelInfo = { VoskModels.loader(app).current?.info },
                            exportSink = ExportWriter { app.getExternalFilesDir(EXPORT_DIR) },
                            provenance =
                                Provenance(
                                    appVersion = BuildConfig.VERSION_NAME,
                                    voskVersion = BuildConfig.VOSK_VERSION,
                                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                                    androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                                ),
                            wavFiles = {
                                wavDir
                                    ?.listFiles { file -> file.name.endsWith(".wav") }
                                    ?.map { it.name }
                                    ?.sorted()
                                    .orEmpty()
                            },
                            clock = SystemClock::elapsedRealtime,
                            processing = Dispatchers.Default,
                            io = Dispatchers.IO,
                        )
                    }
                }
            }
    }
}
```

Substituir `KT/MainActivity.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import io.github.gigioderico.tribuna.etapa0.ui.ReaderRoute
import io.github.gigioderico.tribuna.etapa0.viewmodel.ReaderViewModel

/**
 * Activity única da etapa0, só Compose (CLAUDE.md, regra 8). Declarada no manifesto conforme a CIA 0003
 * (portão 1). Sem `configChanges`: a rotação recria a Activity e o ViewModel segue vivo (TRD §Segundo plano).
 */
class MainActivity : ComponentActivity() {
    private val viewModel: ReaderViewModel by viewModels { ReaderViewModel.factory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ReaderRoute(viewModel.controller)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.controller.onActivityStart()
    }

    override fun onStop() {
        super.onStop()
        // Rotação não é ida ao segundo plano: com isChangingConfigurations, nada muda (TRD §Segundo plano).
        viewModel.controller.onActivityStop(isChangingConfigurations)
    }
}
```

- [ ] **Passo 2: verificar.** `./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest assembleDebug --console=plain` verde.

- [ ] **Passo 3: commit.**

```bash
git add etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/viewmodel/ReaderViewModel.kt \
  etapa0/app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/MainActivity.kt
git commit -m "feat(ui): ViewModel com dependências reais e Activity única com isChangingConfigurations (TRD §Segundo plano; CIA 0003 c)"
```

---

### Tarefa 16 (motor-voz): verificação completa e evidências

Tudo em `etapa0/`, com a saída em `work/0003-etapa0-vosk/evidencias/` (um arquivo por critério; `2>&1 | tee`).

- [ ] **Passo 1: A2.** `./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest --rerun-tasks --console=plain > ../work/0003-etapa0-vosk/evidencias/A2-gradle.txt 2>&1`; depois a contagem por classe dos XML (`app/build/test-results/testDebugUnitTest/*.xml`, `build-logic/build/test-results/test/*.xml`, `core/build/test-results/test/*.xml`) e `grep -rnE "@Ignore|@Disabled" . --include=*.kt` (vazio). Esperado: 118 no `:app`, 27 no `build-logic`, 118 no `:core`, 0 falhas.
- [ ] **Passo 2: A1, A3, B6.** `./gradlew assembleDebug assembleRelease bundleRelease`; `unzip -l app/build/outputs/apk/release/app-release.apk | grep -E "lib/|model-pt/uuid"`; `unzip -l app/build/outputs/bundle/release/app-release.aab | grep -E "base/lib/|model-pt/uuid"`; `wc -c app/build/outputs/bundle/release/app-release.aab` (prova: 49.589.150); `./gradlew :app:dependencies --configuration releaseRuntimeClasspath`; o grep do `mapping.txt`.
- [ ] **Passo 3: B1, B3.** `grep -E "uses-permission|<permission|allowBackup|dataExtractionRules|fullBackupContent" app/build/intermediates/merged_manifest/{debug/processDebugMainManifest,release/processReleaseMainManifest}/AndroidManifest.xml`; `aapt2 dump permissions` do APK de release; o relatório do lint sem `DataExtractionRules`.
- [ ] **Passo 4: B2, B4.** `./scripts/verificar-manifesto-negativo.sh; echo exit=$?`; os três `--dry-run` da Tarefa 1, Passo 5.
- [ ] **Passo 5: B5.** `./gradlew verify16kAlignment --console=plain`. Esperado (prova): reprova só com `lib/armeabi-v7a/libvosk.so` e `lib/x86/libvosk.so` alinhadas a 4096, no APK e no AAB; as linhas `LOAD` de todas as `.so` aparecem. Anexe a saída inteira e confira que o OPR-024 está no registro. Se reprovar com outra coisa (biblioteca ausente numa ABI, `zipalign` falhando, `libvosk.so` de 64 bits a 4096), pare e escale ao arquiteto.
- [ ] **Passo 6: A5, A6, A7, CIA.** `git diff main --stat -- etapa0/core/src/main` (vazio); os greps de A6 e A7 do spec; `grep -rnE "registerReceiver|receiveBroadcasts|RECEIVER_NOT_EXPORTED" app/src --include=*.kt` (vazio); `./scripts/varrer-registerreceiver.sh`.
- [ ] **Passo 7: A8.** `./gradlew :app:ktlintCheck --rerun-tasks --console=plain` (as tarefas `runKtlintCheckOverMainSourceSet` e `runKtlintCheckOverTestSourceSet` aparecem sem `NO-SOURCE`). Prova negativa: criar `app/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/ui/Quebra.kt` com `package io.github.gigioderico.tribuna.etapa0.ui` e `class Quebra {   val a = 1 }`, rodar de novo (FAIL com "Unnecessary long whitespace"), anexar e **apagar o arquivo**.
- [ ] **Passo 8: registro e commit.** Preencher "Registro da construção" no fim deste plano.

```bash
git add work/0003-etapa0-vosk/evidencias work/0003-etapa0-vosk/impl_plan.md
git commit -m "test(0003): evidências por comando dos critérios A, B, C e D"
```

## Verificação

Em `etapa0/`:

```bash
./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest
./gradlew assembleDebug assembleRelease bundleRelease
./gradlew verify16kAlignment                      # reprova só nas libvosk.so de 32 bits (A8; OPR-024)
./gradlew check --dry-run | grep MergedManifest   # e assembleRelease/bundleRelease --dry-run
./scripts/verificar-manifesto-negativo.sh
./scripts/varrer-registerreceiver.sh
"$ANDROID_HOME/build-tools/36.0.0/aapt2" dump permissions app/build/outputs/apk/release/app-release.apk
grep -E "^(org.vosk.Recognizer|com.sun.jna.Native) ->" app/build/outputs/mapping/release/mapping.txt
git lfs ls-files | grep model-pt
git diff main --stat -- etapa0/core/src/main
grep -rn "SpeechService" app/src; find app/src -path "*res/layout*"
grep -rnE "rememberSaveable|SavedStateHandle|DataStore|MediaRecorder" app/src/main
grep -rnE "FileOutputStream|writeText|writeBytes|bufferedWriter|openFileOutput" app/src/main
grep -rn "Log\." app/src/main
grep -rnE "registerReceiver|receiveBroadcasts|RECEIVER_NOT_EXPORTED" app/src --include=*.kt
```

Sucesso significa:

- Primeira linha: BUILD SUCCESSFUL; 118 testes no `:app`, 27 no `build-logic`, 118 no `:core`, 0 falhas, 0 ignorados; lint do `:app` só com `MissingApplicationIcon`.
- Empacotamento verde; APK e AAB com `assets/model-pt/uuid` e `libvosk.so` + `libjnidispatch.so` nas 4 ABIs; AAB por volta de 49,6 MB.
- `verify16kAlignment`: `zipalign` "Verification successful"; problemas só nas duas `libvosk.so` de 32 bits (caminho "Reprova por alinhamento" do B5, com OPR-024). Qualquer outro problema reprova a tarefa.
- `--dry-run` com a verificação da variante; script negativo `exit=0`; varredura `exit=0`; `aapt2` só com `RECORD_AUDIO`; `mapping.txt` com as duas linhas.
- `git lfs ls-files` com os 16 arquivos do modelo; `:core/src/main` sem diff.
- Greps: vazios, exceto o de escritas, que lista só `speech/ModelInstaller.kt` e `export/ExportWriter.kt` (import e uso de `FileOutputStream`).

## Riscos

- **JSON real do Vosk diferente do documentado** (C6; Q4). Se a 0005 vir outro formato (por exemplo, `partial` com `result` de palavras), a correção fica em `VoskJson` e no `VoskJsonTest`, sem tocar no contrato.
- **`libvosk.so` de 32 bits sem 16 KB** (A8). Já medido; decisão humana no fim da Etapa 0 entre os caminhos do OPR-024. Não afrouxar a verificação.
- **Criar reconhecedor por sessão lento** (TRD §Captura e sessões). O painel mostra `beginSession` em ms; a estratégia `Reset` já existe. Se a criação passar de algumas centenas de ms com o vocabulário ligado (gramática compilada a cada sessão), a 0005 compara as duas estratégias com os mesmos WAV.
- **`stopCapture` espera a leitura em curso** (até 0,2 s, `ThreadCaptureDriver.stop` com `join`). Se o aparelho travar o `AudioRecord.read`, o limite de 1 s evita congelar a tela; nesse caso o `release()` do `AudioRecord` é feito pela própria thread quando a leitura voltar.
- **Reabrir o microfone logo depois de fechar** (volta rápida do segundo plano). Alguns aparelhos recusam um segundo `AudioRecord` enquanto o primeiro não foi liberado; a tela mostra "Microfone… indisponível" e Ouvir tenta de novo. A 0005 registra se acontecer.
- **AudioRecord sem permissão no meio da leitura** (TRD §Microfone). `read` negativo vira `PermissionRevoked` se a permissão sumiu, senão `MicUnavailable`; validado só no aparelho.
- **Cota de LFS no push do modelo** (53,6 MB). Se o GitHub recusar, para e vai ao humano (portão 6).
- **Mudança do zip publicado** em `alphacephei.com`. O script aborta por SHA-256; trocar o modelo é decisão (Q8) e regera o `uuid`.
- **Hífen no roteiro × Vosk** (OPR-025). Pode atrasar o cursor em palavras com hífen no T03 e no L30; medir na 0005.
- **Se o Vosk ou o Android não se comportarem como o TRD supõe** no BUILD (por exemplo, `Recognizer` não aceitar a gramática com palavras ausentes, ou o `AudioRecord` exigir outra fonte): pare, registre a saída real na pasta da tarefa e escale ao arquiteto (nível 1). Não contorne no código nem mude contrato.

## Portão automático do plano (checklist do PROCESS.md, conferida pelo arquiteto antes de entregar)

| Item | Situação |
|---|---|
| Cada critério do spec tem passo e comando de verificação | Sim: tabela "Critérios do spec → teste → comando" (A1–A8, B1–B6, C1–C11, D1–D9); E é da 0005 pelo próprio spec |
| Testes vêm antes do código | Sim: Tarefas 1, 3, 4, 6–10, 12, 13 começam pelo teste e pelo comando que falha; Tarefas 5, 11, 14, 15 são costuras ou código de aparelho sem lógica nova, verificadas por compilação, lint e greps; a Tarefa 2 começa pela verificação do manifesto falhando |
| Seção de privacidade preenchida | Sim, com Q6 caminho a caminho |
| Nenhuma assinatura de contrato muda | Sim: `core/src/main` intocado (A5); `beginSession(vocabulary: List<String>?)` mantido; o OPR-020 foi resolvido no conteúdo da lista, sem mudar `Token` |
| Nenhuma permissão nova no manifesto | Sim: o manifesto é o aprovado no portão 1 (só `RECORD_AUDIO`, provado por `aapt2`) |

Portões humanos: o 1 (manifesto) já foi aprovado. Nenhum outro é necessário para o BUILD. Ficam para decisão humana, sem bloquear: textos do TRD propostos em A5 (§Vocabulário restrito) e A6 (§Instalação do modelo 1); o caminho do OPR-024 no fim da Etapa 0; portão 6 só se o push do LFS for recusado.

---

## Registro da construção (preenchido pelo agente de construção)

**Agente:** motor-voz (Tarefas 1 a 11; faltam as Tarefas 12 a 15, do `interface`, e a 16, do `motor-voz`) · **Commits:** 16b7a18 (T1), de3ce85 (T2), 7265da3 (T3), 28698cc (T4), d796367 (T5), 773fc6e (T6), d9d1c23 (T7), 4ab55a2 (T8), 8c01c72 (T9), ae971e2 (T10), 33a2656 (T11)

- Feito: os blocos do plano aplicados na ordem, um commit por tarefa (trailer de coautoria incluído), teste antes do código em cada uma (compilação falhou com "Unresolved reference" antes de cada implementação). Modelo baixado por `scripts/baixar-modelo.sh` (SHA-256 conferido) e commitado como ponteiros LFS (`git lfs ls-files` lista 15 arquivos com `*`; `git diff --cached --stat` mostrou 3 linhas por arquivo). TRD §Manifesto substituído pelo texto aprovado (3 linhas por 4). `core/src/main` intocado (`git diff main --stat -- etapa0/core/src/main` vazio). Sem push.
- Testes criados: `:app` 82 (FstSymbols 3, Vocabulary 4, LexiconRealModel 4, AudioLevel 5, VoskJson 8, TestTextCatalog 5, ExportFiles 6, ExportNameAndWriter 4, VoskEngineSession 17, VoskEngineState 10, ModelInstaller 7, WavAudioSource 8, mais o `CoreFromAppTest` que já existia); `build-logic` 27 (ManifestPermissionCheck 11, NativeLibScan 9, ElfAlignment 7). `:core` 118, inalterado.
- Desvios do plano e motivo:
  1. Contagem do modelo: o plano diz "15 arquivos + uuid = 16". O zip tem 14 arquivos; com o `uuid` são 15 (`find` = 15; soma 53.552.813 bytes = 53.552.717 do plano + 96 do `uuid`). Conteúdo e SHA-256 batem; só o número do plano estava errado. Os 15 arquivos estão no LFS.
  2. Na Tarefa 2, apliquei o novo manifesto por engano antes do Passo 2 e refiz: restaurei o manifesto antigo, rodei `verifyMergedManifest --continue` (as duas variantes reprovam com o nome `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, evidência `B1-manifesto-antes.txt`) e reapliquei o aprovado. Sem efeito no resultado.
  3. A Tarefa 7 e a 11 pedem "avisar o orquestrador" para o `interface` começar: o trabalho foi sequencial (sem worktree), então o aviso vai neste relatório.
  4. Aviso do compilador (não é erro, não afrouxei nada): `EngineHarness.kt` (teste, código literal do plano) usa API experimental de coroutines-test sem `@OptIn`. Fica para o revisor decidir.
- Comandos rodados e resultado (em `etapa0/`): `./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest --rerun-tasks` BUILD SUCCESSFUL (lint do `:app`: só `MissingApplicationIcon`, que já existia); `assembleDebug assembleRelease bundleRelease` BUILD SUCCESSFUL na Tarefa 2; `aapt2 dump permissions` do APK de release: só `RECORD_AUDIO`; `mapping.txt` mantém `com.sun.jna.Native` e `org.vosk.Recognizer`; `scripts/verificar-manifesto-negativo.sh` exit=0 (4 reprovações, 1 aprovação); `scripts/varrer-registerreceiver.sh` exit=0 (77 bibliotecas, 13.076 classes; só `MediaQuery` do compose-ui com flag 2 e `ContextKt` do core-ktx); `check`, `assembleRelease` e `bundleRelease` com `--dry-run` listam `verify…MergedManifest`; `grep -rnE "MediaRecorder|SpeechService|Log\." app/src/main` vazio; nenhum `@Ignore`/`@Disabled`. Evidências em `work/0003-etapa0-vosk/evidencias/` (B1, B2, B3, B4, B6, A4, CIA). O `verify16kAlignment` e a verificação completa ficam para a Tarefa 16.

### Registro da construção: interface (Tarefas 12 a 15)

**Agente:** interface · **Commits:** 2a48890 (T12), 9e6ba2c (T13), e0913ff (T14), b7091b8 (T15). Sem push.

- Feito: código dos blocos do plano aplicado sem alteração de conteúdo (extraído do próprio plano), um commit por tarefa, trailer de coautoria incluído. Teste antes do código: T12 falhou na compilação ("Unresolved reference 'setText'" etc.) e T13 também ("Unresolved reference 'HitTest'") antes das implementações. T14 e T15 sem teste JVM, como o plano manda (Compose UI no aparelho é da 0005).
- Testes criados (todos verdes, 0 falhas, 0 ignorados): ReaderSessionTest 3, ReaderReadingTest 9, ReaderTapTest 4, ReaderExportTest 5, ReaderLifecycleTest 9, HitTestTest 6 (= 36). Total `:app` 118 (batem com o esperado do plano), `:core` 118, `build-logic` 27.
- Desvios: nenhum no conteúdo. Pontos que o plano não pedia e não fiz: prévias do Compose (as do agente valem para o app, não para a tela mínima da etapa0; sem tela de verdade para as duas classes de largura). Avisos do compilador, sem erro, vindos do código literal do plano e não afrouxados: `ReaderHarness.kt` (lateinit desnecessário na linha 80; uso de API experimental de coroutines-test sem `@OptIn`, linhas 98 e 112), mesma família do aviso já registrado no `EngineHarness.kt`. Fica para o revisor decidir.
- Comandos (em `etapa0/`): `./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest --rerun-tasks` BUILD SUCCESSFUL (saída em `work/0003-etapa0-vosk/evidencias/T12-15-gradle.txt`); `./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest assembleDebug` BUILD SUCCESSFUL; `grep -rnE "rememberSaveable|SavedStateHandle|Log\.|@Ignore|@Disabled" app/src` vazio; `core/src/main` intocado.
