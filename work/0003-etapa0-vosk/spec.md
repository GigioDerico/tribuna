# Spec — 0003-etapa0-vosk

**Autor:** analista · **Data:** 2026-10-11 · **Aprovação:** conferida pelo orquestrador em 2026-10-11. Portão humano 1 (manifesto: OPR-001, OPR-002, Activity) aprovado por Giorgio Derico em 2026-10-11, opção A da CIA (`change_impact_analysis.md`): remover `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` e seu `<permission>` com `tools:node="remove"`; regras de backup excluindo os 9 domínios + `fullBackupContent`; `.MainActivity` lançadora `exported`; verificação passa a reprovar `<permission>` e `registerReceiver`. Regra 1 intacta.

Brainstorm: `work/0003-etapa0-vosk/brainstorm.md`. As decisões D1–D28 citadas abaixo estão lá, com alternativas e motivo.

## O que e por quê

Ligar o Vosk ao núcleo da 0002 no app experimental `etapa0/:app`. São quatro partes: `VoskEngine` (laço próprio de `AudioRecord` alimentando o `Recognizer`, sem `SpeechService`), `ModelInstaller` (`vosk-model-small-pt-0.3` copiado de assets), uma tela mínima em Compose (texto, Ouvir, Pausar, palavra atual destacada) e o painel de depuração, com exportação de transcrições brutas, trajetória e nível. É o item 3 da Etapa 0 (PROCESS.md). Sem esta tarefa, a 0005 não tem o que medir no aparelho, a 0006 não tem golden e o alinhamento de 16 KB não é verificado contra as bibliotecas reais. Para o orador ainda não muda nada: este é o protótipo que decide se a arquitetura segue.

## Requisitos atendidos

- PRD: R1 (base), R7 (nível de áudio), Motor de acompanhamento (Pipeline 1–2, Vocabulário restrito, Robustez: limiar de nível e reinício do reconhecedor, Teste obrigatório: modo de depuração); Requisitos não funcionais (Offline, Privacidade, Permissões, Tamanho).
- TRD: Contratos §SpeechEngine e regras; Motor de voz §Instalação do modelo, §Captura e sessões, §Reconciliação 7, §Vocabulário restrito; Máquina de estados (linhas Reading/Paused de Toque em palavra, Pausa e Segundo plano, só o que a tela mínima usa); Ciclo de vida, privacidade e permissões (Manifesto, Onde o texto vive, Segundo plano, Microfone); Metas de desempenho (só os pontos de medição, sem fixar meta); Testes §Privacidade nos testes, §Qualidade de código, §Distribuição (AAB, 16 KB, R8); Etapa 0 §O que construir, itens 1, 2, 4 e 5.
- Contratos: só implementação. `SpeechEngine`, `SpeechEvent`, `SessionId`, `EngineState`, `EngineError`, `Token` e `Aligner`/`AlignState` ficam como estão (CLAUDE.md, regra 5). Configurações próprias do `VoskEngine` (estratégia, fonte de áudio) entram pelo construtor, não pela interface. O tempo das exportações é carimbado por quem consome os eventos, não entra no evento (D6).
- Registro: resolve OPR-001 (com o portão humano), OPR-002, OPR-004, OPR-005 e OPR-018; atende o requisito de exportação de OPR-008.

### Interpretações e valores que este spec fixa (do brainstorm)

- **Costuras para a JVM** (D1): o laço de captura e sessões é testável com fonte de áudio e reconhecedor falsos, sem carregar `org.vosk.*`. As costuras são internas ao `:app`.
- **`Level.rms`** = `sqrt(média(s²)) / 32768`, em [0, 1] (D2).
- **Limiar de nível**: ajustável, padrão 0 (desligado), aplicado por quem consome; nenhum valor inventado (D3).
- **Reconhecedor novo ou `reset()`**: as duas estratégias, padrão reconhecedor novo, `SessionId` novo nas duas (D4). **Vocabulário restrito**: liga e desliga, padrão desligado, lista = palavras de `Token.spoken` + `[unk]` (D5).
- **`endSession`** descarta a hipótese pendente sem chamar `finalResult` (D14). Os erros seguem o mapeamento de D15.
- **Fonte de áudio**: microfone (padrão) ou WAV de teste, 16 kHz, mono, PCM 16 bits, lido no ritmo real, nunca gravado (D8).
- **Exportação** (D7, D9–D11), só quando o texto atual é idêntico a um texto de `etapa0/textos/` (empacotados como assets). Arquivos `<nome>.hypotheses.csv` (`t_ms,session,kind,words,accepted`), `.level.csv` (`t_ms,rms`), `.trajectory.csv` (`t_ms,cursor,move,score`), `.events.csv` (`pausa_toque`, `retoma_toque`, `toque_palavra`) e `.meta.txt`. `t_ms` = ms desde o `startCapture` da gravação, por relógio monotônico. Ficam no diretório externo do app (sem permissão), para `adb pull`.
- **Animação** da tela mínima: 150–600 ms, inicial **375 ms (PROVISÓRIO, ponto médio da faixa do R5, só na etapa0)** (D13).
- **Release da etapa0**: R8 ligado com as regras do TRD e assinado com a chave de debug (D19).

## Critérios de aceite

Os comandos rodam a partir de `etapa0/`, e a saída de cada um vai anexada em `work/0003-etapa0-vosk/evidencias/`. "Teste" quer dizer JUnit na JVM (`testDebugUnitTest` ou `:core:test`), sem Vosk nativo, sem aparelho e com o `FakeSpeechEngine` onde couber.

### A. Build, dependências e regras (por comando)

- [ ] A1. `./gradlew assembleDebug assembleRelease bundleRelease` termina verde. O APK de release e o AAB contêm `assets/model-pt/` com o arquivo `uuid` (D12) e `lib/<abi>/libvosk.so` e `libjnidispatch.so` (`unzip -l` anexado). O tamanho em bytes do AAB fica registrado na evidência (meta de 80 MB do TRD; se passar, abre OPR, mas não reprova).
- [ ] A2. `./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest` termina verde. Nenhum teste desativado (`@Ignore`/`@Disabled`) em `etapa0/`. A contagem de testes novos no relatório é ≥ 1 por critério das seções B a D (Q3).
- [ ] A3. Dependências novas só pelo `gradle/libs.versions.toml`: `vosk-android`, JNA (`@aar`) e o Compose (BOM, `activity-compose`, `material3`, `lifecycle-*-compose`). `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` fica anexado. As versões e a origem de cada uma (Maven Central, Google Maven) estão no `impl_plan.md`.
- [ ] A4. O modelo em `etapa0/app/src/main/assets/model-pt/` é o `vosk-model-small-pt-0.3` e está no LFS: `git lfs ls-files` lista os arquivos do modelo, e o SHA-256 do zip baixado de `alphacephei.com` fica registrado em `uuid` e na evidência.
- [ ] A5. `git diff main --stat -- etapa0/core/src/main` não mostra mudança em `speech/` nem em `align/` (contratos e núcleo intocados). Se o PLAN pôr código novo no `:core`, só arquivos novos, e as assinaturas existentes iguais.
- [ ] A6. `grep -rn "SpeechService" etapa0/app/src` não devolve nada (TRD Captura e sessões; CLAUDE.md "Erros comuns"). `grep -rn "res/layout"` e `find etapa0/app/src -path "*res/layout*"` também ficam vazios (regra 8).
- [ ] A7. Privacidade (regras 2 e 3): `grep -rnE "rememberSaveable|SavedStateHandle|DataStore|MediaRecorder" etapa0/app/src/main` não devolve nada. `grep -rnE "FileOutputStream|writeText|writeBytes|bufferedWriter|openFileOutput" etapa0/app/src/main` lista as escritas em arquivo, e o revisor confirma que todas são da exportação de D7 e que nenhuma escreve áudio. `grep -rn "Log\." etapa0/app/src/main` lista as chamadas, e o revisor confirma que nenhuma recebe texto, palavras, hipóteses ou JSON do Vosk (D28).
- [ ] A8. `./gradlew :app:ktlintCheck --rerun-tasks --console=plain` mostra as tarefas de fonte `main` e `test` do `:app` executadas (nenhuma com `NO-SOURCE`). Com uma violação proposital num `.kt` do `:app`, a tarefa falha, e a saída da falha fica anexada (OPR-018, Q2).

### B. Manifesto e verificações do build (por comando)

- [ ] B1. `./gradlew verifyMergedManifest` passa, e os manifestos mesclados reais de debug e release (lidos em `build/intermediates`, Q4) têm exatamente uma `uses-permission`, `android.permission.RECORD_AUDIO`. Nenhuma `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK` ou `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (OPR-001, conforme a decisão do portão humano).
- [ ] B2. O script negativo da 0001 (`scripts/verificar-manifesto-negativo.sh`) continua reprovando `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK` e `CAMERA`, agora com as dependências novas no grafo (`exit=0` no script).
- [ ] B3. O manifesto mesclado tem `android:allowBackup="false"` e `android:dataExtractionRules` apontando para um recurso que exclui tudo de `cloud-backup` e de `device-transfer`. O lint não mostra mais `DataExtractionRules` (OPR-002).
- [ ] B4. `./gradlew check --dry-run`, `assembleRelease --dry-run` e `bundleRelease --dry-run` listam a verificação de manifesto da variante correspondente (OPR-004; TRD Manifesto: "o build verifica").
- [ ] B5. `./gradlew verify16kAlignment` roda contra o APK de release com as `.so` reais e verifica `libvosk.so` e `libjnidispatch.so` em todas as ABIs presentes (a lista das ABIs fica na evidência). A verificação também cobre o AAB: segmentos ELF das duas bibliotecas em `base/lib/<abi>/` e a presença das duas em toda ABI. Se faltar uma das duas numa ABI, a tarefa falha (OPR-005, Q5). A saída real fica anexada.
  - **Passa:** o critério fecha.
  - **Reprova por alinhamento:** o critério fecha do mesmo jeito se a saída mostrar, por biblioteca e ABI, o alinhamento encontrado e um OPR estiver aberto com os caminhos (versão mais nova pelo catalog; recompilação, que é portão humano 6). A verificação não é afrouxada, desligada nem contornada (D20). É resultado esperado da Etapa 0 e vai para a decisão humana ao fim dela.
- [ ] B6. R8: `assembleRelease` gera `mapping.txt`, onde `org.vosk.Recognizer -> org.vosk.Recognizer` e `com.sun.jna.Native -> com.sun.jna.Native` aparecem com o nome mantido (TRD Distribuição; D19).

### C. Motor e instalação, lógica na JVM

Cada item tem pelo menos um teste. Os testes usam fonte de áudio e reconhecedor falsos (D1), e nenhum carrega `org.vosk.*`.

- [ ] C1. **Sessão presa ao reconhecedor:** `beginSession` cria o `SessionId` e o reconhecedor juntos. Todo `Partial`/`Final`/`Error` daquele reconhecedor leva o id dele. Um resultado produzido depois de `endSession` e de um novo `beginSession` sai com o id antigo. O teste fica vermelho se o código ler a sessão atual do motor na hora de emitir (Q2; o QA registra a quebra).
- [ ] C2. **Fora de sessão:** com a captura aberta e sem sessão, nenhum quadro chega a reconhecedor nenhum, e `Level` continua saindo, um por bloco lido.
- [ ] C3. **`endSession`:** blocos lidos e ainda não entregues ao reconhecedor no momento do `endSession` não chegam a ele. O reconhecedor é liberado (estratégia "novo") ou fica sem hipótese (estratégia `reset()`). Nenhum `Final` é emitido por causa do `endSession` (D14).
- [ ] C4. **Estratégias (D4):** com "novo", cada `beginSession` cria um reconhecedor. Com `reset()`, o mesmo reconhecedor é reaproveitado com `SessionId` novo, e um reconhecedor novo é criado quando o vocabulário muda. Nas duas, ids de sessões seguidas são distintos e crescentes.
- [ ] C5. **Vocabulário (D5):** `beginSession(vocabulary)` passa a lista ao reconhecedor criado. A lista derivada dos tokens de T02 contém cada palavra de `Token.spoken` uma vez, mais `[unk]`. Com o vocabulário desligado, o reconhecedor é criado sem lista.
- [ ] C6. **JSON → evento:** `{"partial" : "ouvir bem"}` vira `Partial(id, [ouvir, bem])`, `{"text" : "ouvir bem e uma"}` vira `Final(id, [ouvir, bem, e, uma])`, e `""` vira lista vazia. Os formatos de entrada vêm da documentação do Vosk; a confirmação contra o JSON real é da 0005 (Q4).
- [ ] C7. **`Level` (D2):** um bloco de zeros dá 0,0, e onda quadrada de ±32767 dá 1,0 (± 1e-3).
- [ ] C8. **Estados e erros (D15):** a sequência `prepare` → `startCapture` → `beginSession` → `endSession` → `stopCapture` passa por `Preparing`, `Ready`, `Capturing`, `Recognizing`, `Capturing` e `Ready`. Falha ao abrir a fonte gera `Error(null, MicUnavailable)`. Sem permissão gera `Error(null, PermissionRevoked)`. Exceção do reconhecedor gera `Error(sessão, Internal)`. Modelo ausente faz `prepare()` devolver falha, com estado `Failed` e `ModelMissing`.
- [ ] C9. **Modelo carregado uma vez:** duas chamadas de `prepare()` no mesmo processo carregam o modelo uma vez só (fábrica de modelo falsa conta as cargas).
- [ ] C10. **`ModelInstaller` (D12):** com `uuid` igual no destino, não copia. Com `uuid` diferente ou ausente, copia. Com falha de cópia, devolve erro que a tela mostra com "Tentar de novo" (TRD Instalação 2 e 3).
- [ ] C11. **Fonte WAV (D8):** o cabeçalho de um WAV de 16 kHz, mono, PCM 16 bits é aceito. Um WAV de 44,1 kHz, estéreo ou de 8 bits é recusado com erro. Os blocos são entregues em ordem e sem perda: a soma dos tamanhos dá o tamanho dos dados do arquivo. O WAV do teste é sintético (gerado no próprio teste), nunca uma gravação de discurso.

### D. Tela, ligação com o `align` e exportação, lógica na JVM

Os testes usam o ViewModel da tela com o `FakeSpeechEngine` e o relógio controlado do `kotlinx-coroutines-test`. As falas simuladas são trechos de `etapa0/textos/`.

- [ ] D1. **Descarte por sessão (regra 7):** depois de Pausar e Ouvir, um `Partial` e um `Final` atrasados da sessão anterior (`FakeRecognizer` antigo) não mudam o cursor nem chegam ao reconciliador. O teste fica vermelho se o filtro for removido (Q2).
- [ ] D2. **Fala na pausa:** com a leitura pausada (captura aberta, sem sessão), falas simuladas não movem o cursor. Ao retomar, a sessão é nova e a cauda de palavras começa vazia.
- [ ] D3. **Leitura de ponta a ponta:** lendo o T01 por parciais crescentes e finais (preset Normal), o cursor chega ao último token, e a `trajectory.csv` gerada tem uma linha por mudança de cursor, com `move` ∈ nomes de `Move`, começando por `0,0,Reposition,0.0`.
- [ ] D4. **Toque em palavra (D10):** tocar no token *i* durante a leitura faz `endSession`, `reposition(i)` e `beginSession` nessa ordem (ordem conferida no fake). O cursor vai a *i*, a cauda zera e `events.csv` ganha `toque_palavra` com detalhe *i*. Pausar e Ouvir geram `pausa_toque` e `retoma_toque`.
- [ ] D5. **Limiar de nível (D3):** com limiar 0, todo parcial é processado. Com limiar acima do último `Level` recebido, o parcial é ignorado e o cursor não se move.
- [ ] D6. **Exportação só com texto de teste (D9):** para cada arquivo de `etapa0/textos/`, a função de identificação devolve o ID dele. Com um caractere alterado, devolve nada, e a exportação fica desabilitada.
- [ ] D7. **Formato e ida e volta (D7, OPR-008):** os CSVs gerados têm exatamente os cabeçalhos de D7 e de TESTES-ETAPA0. Ler `hypotheses.csv` de volta e reprocessar só as linhas com `accepted = 1` pelo `PartialReconciler` entrega a mesma lista de palavras que a sessão ao vivo entregou. As linhas de sessões descartadas aparecem com `accepted = 0` e o id original.
- [ ] D8. **Proveniência (D11):** `meta.txt` traz todas as chaves de D11, e o ID do modelo é igual ao conteúdo de `uuid`.
- [ ] D9. **Ciclo de vida (D16):** `onStop` com `isChangingConfigurations = true` não chama `endSession` nem `stopCapture`. Com `false`, chama os dois e a tela fica pausada. O texto não sai do ViewModel (A7).

### E. Só no aparelho: fica para a 0005, não bloqueia esta tarefa

O QA da 0003 não executa estes itens. A 0005 executa, com o protocolo do TRD:

- Permissão de microfone em tempo de execução, cópia do modelo na primeira execução e carga, com as durações no painel e no `meta.txt`.
- `AudioRecord` a 16 kHz, mono, PCM 16 bits; JSON real do Vosk igual ao de C6 (Q4); `Level` respondendo à voz.
- Leitura de T01 a T10 e do L30 com trajetórias exportadas e puxadas por `adb pull`, inclusive com a fonte WAV.
- Latências 1 a 3, início da escuta, inicialização a frio, memória e bateria.
- Reconhecedor novo contra `reset()` (tempo de `beginSession`), vocabulário ligado contra desligado.
- Release com R8 rodando no aparelho (modelo carrega e reconhece).
- Toque em palavra, rolagem e destaque na tela real; rotação sem pausar; ida ao segundo plano.

## Fora do escopo

- O app `app/` (Tribuna). Nada desta tarefa entra lá, e a exportação nunca entra (TRD Privacidade nos testes).
- Medição, calibração e relatório: presets, limiar de nível, peso comum, margem de ambiguidade, vocabulário e estratégia de reconhecedor (0005). Ferramentas Python (0004). Golden de transcrições como suíte de regressão (0006, OPR-008).
- O teste instrumentado "Integração de voz" do TRD: precisa dos WAV da 0005 e de aparelho (OPR-019). `connectedDebugAndroidTest` não é critério desta tarefa.
- Recompilar o Vosk ou a JNA para 16 KB (portão humano 6, se for o caminho).
- Telas do app (Onboarding, Editor, Prompter, Settings), `ReadingStateMachine`, `PrompterViewModel`, contagem regressiva, cronômetro, modo Constante, Volume ±, arrasto, toque longo, imersivo, `FLAG_KEEP_SCREEN_ON`, DataStore, layout de tablet.
- Barra de progresso da cópia do modelo (tela 1b do app). Na etapa0 basta o estado "Preparando modelo…" com a duração (OPR-021).
- Marcação manual de improviso, silêncio e pulo no app; a folha de eventos continua à mão (TESTES-ETAPA0).
- Qualquer mudança em contrato do TRD. Se o motor não couber no contrato, parar (regra 5).
- OPR-003 (ktlint do `build-logic`) e OPR-006 (robustez do `build-logic`), a não ser que o PLAN precise mexer nesses arquivos.

## Mudança proposta no PRD (se houver)

Nenhuma nesta tarefa. Achado registrado para decisão futura: o R5 não tem padrão no PRD (OPR-022).

## Perguntas em aberto

1. **Portão humano 1, manifesto (OPR-001, OPR-002, D23–D25).** Precisa de CIA do arquiteto e aprovação humana antes do BUILD. O pedido é aprovar três mudanças no `etapa0/app/src/main/AndroidManifest.xml`:
   - (a) remover `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, injetada pelo `androidx.core` que vem com o Compose, com `tools:node="remove"`, sem usar `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)`;
   - (b) adicionar `android:dataExtractionRules` com regras vazias;
   - (c) declarar a Activity principal.

   Nenhuma permissão nova entra. As alternativas a (a) são mudar a regra 1 ou afrouxar a verificação: a primeira muda regra inegociável e a segunda é proibida. Se o humano decidir diferente de (a), o critério B1 muda junto.
2. **Portão humano 6, condicional (D26).** Só se `git lfs version` falhar na máquina (instalar o Git LFS) ou se o envio do modelo (por volta de 31 MB) for passar da cota gratuita de LFS do GitHub.
