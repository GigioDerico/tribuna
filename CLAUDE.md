# Tribuna

App Android de teleprompter que acompanha a voz do orador, 100% offline, sem salvar o texto do discurso. Kotlin + Jetpack Compose, reconhecimento de voz com Vosk.

## Documentos (leia antes de mudar comportamento)

- `docs/PRD.md` — o que o app faz: requisitos (T, A, R, D, C), telas, gestos, critérios de aceite.
- `docs/TRD.md` — como construir: contratos Kotlin, máquina de estados, motor de voz, metas, testes. **Em divergência técnica, o TRD prevalece sobre o PRD.**
- `docs/TESTES-ETAPA0.md` e `docs/TESTES-ETAPA0-L30.md` — textos de teste, protocolo de gravação e cálculo das métricas da Etapa 0.
- `docs/CONFORMIDADE.md` — Play Store, privacidade, licenças.

Cite o requisito ou a seção do TRD no commit quando a mudança vier de um deles (ex.: `feat(align): recuperação de pulo (TRD Motor de voz §Alinhamento 4)`).

## Regras inegociáveis

Quebrar qualquer uma destas é bug, mesmo que o resto funcione.

1. **Sem rede.** O manifesto final só pode ter `RECORD_AUDIO`. Nada de `INTERNET`, `ACCESS_NETWORK_STATE` ou `WAKE_LOCK`. Antes de adicionar uma dependência, confira se ela traz permissões; a tarefa de verificação do manifesto mesclado precisa continuar passando.
2. **O texto do discurso nunca é persistido.** Ele vive só no `PrompterViewModel`. Proibido em `SavedStateHandle`, `rememberSaveable`, DataStore, arquivo, banco, log ou crash report. DataStore guarda só ajustes.
3. **Nenhum áudio é gravado.** O áudio passa pelo reconhecedor e é descartado. Exportação de transcrições existe só no projeto `etapa0/` e só com textos de teste.
4. **`align/` não conhece Android.** `Normalizer`, `Tokenizer`, `PartialReconciler` e `Aligner` são Kotlin puro, testáveis na JVM. Nenhum import de `android.*` ou `androidx.*` nesse pacote.
5. **Os contratos do TRD são fixos.** `SpeechEngine`, `SpeechEvent`, `SessionId`, `Token`, `Aligner`/`AlignState`, `PrompterUiState`, `ReadingPhase`. Mudar assinatura ou semântica exige parar e pedir aprovação; não adapte o contrato para o código caber.
6. **Na dúvida, o cursor fica parado.** Nenhum movimento automático leva o cursor abaixo de `highWater − 3`. Só `reposition` move o cursor livremente.
7. **Sessão de voz é sagrada.** Todo evento de reconhecimento carrega o `SessionId` do reconhecedor que o gerou; o ViewModel descarta eventos de sessão que não é a ativa. Nunca leia uma variável de "sessão atual" dentro de um callback.
8. **Só Compose.** Nenhum layout XML, nenhuma View antiga.

## Regras de processo

O ciclo completo, os papéis e a escada de escalonamento estão em `PROCESS.md`. As perguntas de revisão estão em `REVIEW-QUESTIONS.md`.

1. **Quem implementa não revisa nem testa.** Código escrito por `motor-voz` ou `interface` é revisado pelo `revisor` e testado pelo `qa-publicacao`, cada um em seu próprio contexto. Nenhum agente declara o próprio código "revisado" ou "aprovado".
2. **Nenhum código antes do plano aprovado.** Fora mudanças triviais (texto de interface, cor, correção de uma linha com teste), primeiro `spec.md`, depois `impl_plan.md`, portão automático do orquestrador (humano só nos portões do `PROCESS.md`, Autonomia e portões humanos), e só então código.
3. **Análise de impacto antes de mexer em contrato.** Contratos do TRD, chaves do DataStore, manifesto ou regra inegociável: `change_impact_analysis.md` e aprovação humana primeiro.
4. **Pare depois de 3 falhas seguidas** no mesmo problema e escale ao `arquiteto`, com o que foi tentado e a saída real.
5. **Nenhuma sessão termina em silêncio.** Sempre diga o que foi feito, onde está (arquivo, commit), o que ficou pendente e se há decisão humana esperando.
6. **Sem atalho por pressa.** Pular revisão, desativar teste ou afrouxar verificação "só desta vez" é proibido.
7. **Nenhum achado se perde.** O que não cabe na tarefa vai para `registers/opr_register.md` com dono.
8. **Comando antes de opinião.** O que um comando pode responder (build, testes, manifesto, 16 KB, métricas) é respondido pelo comando, com a saída anexada.

A sessão principal é o orquestrador: abre a tarefa em `work/<NNNN>-<nome>/` a partir de `templates/`, chama os agentes na ordem do `PROCESS.md` e leva ao humano só o que cai num portão humano. Ela não implementa nem revisa.

## Stack

- Kotlin 2.x, coroutines, `StateFlow`; Jetpack Compose + Material 3 (Compose BOM); Navigation Compose; uma Activity.
- `material3-window-size-class` para celular e tablet.
- `com.alphacephei:vosk-android` + JNA (AAR); modelo `vosk-model-small-pt-0.3` em `app/src/main/assets/model-pt/`.
- Jetpack DataStore (Preferences).
- minSdk 26, targetSdk 36, compileSdk 36. Gradle Kotlin DSL com version catalog (`gradle/libs.versions.toml`); versões novas entram só pelo catalog.

## Estrutura do repositório

```
CLAUDE.md             este arquivo
PROCESS.md            ciclo, papéis, escalonamento
REVIEW-QUESTIONS.md   perguntas de revisão (Q1–Q8)
.claude/agents/       analista, arquiteto, motor-voz, interface, revisor, qa-publicacao
templates/            spec, impl_plan, change_impact_analysis, code_review, test_result, release_check, opr_register
work/                 uma pasta por tarefa com seus artefatos
registers/            opr_register.md (problemas abertos)
docs/                 PRD, TRD, testes, conformidade
etapa0/               projeto experimental da Etapa 0 (app mínimo de voz + ferramentas de medição)
  textos/ gravacoes/ eventos/ esperado/ trajetorias/ ferramentas/ relatorios/
app/                  o app Tribuna (só começa depois da Etapa 0 aprovada)
```

Pacotes do app (`app/src/main/java/.../tribuna/`): `ui.onboarding`, `ui.editor`, `ui.prompter`, `ui.settings`, `viewmodel`, `speech`, `align`, `data`, `input`. Quem pode alterar o quê está na tabela "Módulos e responsabilidades" do TRD: o `PrompterViewModel` é o único dono do cursor e do estado da leitura; motor de voz, toques e teclas só enviam eventos.

## Comandos

```bash
./gradlew assembleDebug                 # build
./gradlew testDebugUnitTest             # unitários (align, máquina de estados, ViewModel)
./gradlew connectedDebugAndroidTest     # instrumentados e Compose UI (precisa de aparelho/emulador)
./gradlew lint ktlintCheck              # lint e estilo
./gradlew verifyMergedManifest          # falha se o manifesto final tiver permissão além de RECORD_AUDIO
./gradlew verify16kAlignment            # alinhamento de 16 KB de libvosk.so e libjnidispatch.so no release
```

`verifyMergedManifest` e `verify16kAlignment` são tarefas do projeto: se ainda não existirem, criá-las é parte da primeira entrega (TRD, Qualidade de código).

Ferramentas da Etapa 0 (Python, rodam no computador):

```bash
python etapa0/ferramentas/timestamps.py <wav>
python etapa0/ferramentas/alinhar.py <texto> <transcricao>
python etapa0/ferramentas/metricas.py <trajetoria> <esperado> <eventos>
```

## Como trabalhar

- **O plano é o `impl_plan.md` aprovado da tarefa.** Antes de codar, confira que ele existe e passou no portão do orquestrador; em mudança trivial, descreva em três linhas que arquivos mudam e por quê.
- **Teste primeiro no núcleo.** `align/` e `ReadingStateMachine` são desenvolvidos com teste antes do código. Os casos obrigatórios estão na tabela de suítes do TRD.
- **Use `FakeSpeechEngine`** para testar ViewModel e fluxo de leitura; testes de unidade nunca dependem do Vosk nem de microfone.
- **Uma entrega = tudo verde.** Unitários, lint, `verifyMergedManifest` e, a partir do release, `verify16kAlignment`. Não marque tarefa como concluída com teste falhando ou desativado.
- **Não invente parâmetros.** Limiares, janelas e sensibilidades vêm da tabela do PRD e da calibração da Etapa 0. Se um valor não existe nos documentos, pergunte.
- **Divergência entre documento e realidade** (uma API do Vosk que não se comporta como o TRD supõe, um limite do Android): pare, descreva o problema e proponha a mudança no documento antes de contornar no código.
- **Textos de teste**: só os de `etapa0/textos/`. Nunca use um discurso pessoal em teste, golden ou exemplo.
- **Strings de interface em português do Brasil**, em `strings.xml` (recurso, não layout). Código, identificadores e commits em inglês ou português, mas consistentes dentro do arquivo.

## Erros comuns a evitar

- Usar `SpeechService` do Vosk: o TRD usa laço próprio de `AudioRecord` alimentando o `Recognizer`, para separar captura de sessão.
- Contar "palavras já consumidas" de parciais: use o `PartialReconciler` (prefixo estável + distância de edição).
- Pausar a leitura no `onStop` durante rotação: confira `isChangingConfigurations`.
- Animar a rolagem no modo Constante a partir do cursor: ali a rolagem conduz o cursor, não o contrário.
- Bloquear orientação no tablet: com targetSdk 36 o Android 16 ignora; o A14 só existe no celular.
- `getOffsetForPosition` sozinho para decidir toque em palavra: sempre confira com `getBoundingBox` (+8 dp).
