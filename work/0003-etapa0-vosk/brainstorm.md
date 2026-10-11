# Brainstorm — 0003-etapa0-vosk

**Autor:** analista · **Data:** 2026-10-11 · Skill: `superpowers:brainstorming`, com os ajustes do projeto. O analista responde às perguntas da skill. Não houve companheiro visual: a tela é mínima e não tem decisão visual. O trabalho termina aqui e no `spec.md`, sem `writing-plans`.

## Contexto lido

- `CLAUDE.md`, `PROCESS.md` (Etapa 0, item 3; portões humanos), `REVIEW-QUESTIONS.md`.
- `docs/TRD.md`: Contratos (SpeechEngine e regras), Máquina de estados (só as linhas de Reading/Paused), Motor de voz (Instalação do modelo, Captura e sessões, Vocabulário restrito), Ciclo de vida e privacidade (Manifesto, Onde o texto vive, Segundo plano, Microfone), Metas de desempenho, Testes (suítes "Integração de voz" e "Golden"), Qualidade de código, Distribuição, Etapa 0.
- `docs/PRD.md`: R1, R5, R7, Motor de acompanhamento (Pipeline, Robustez, Teste obrigatório), Requisitos não funcionais.
- `docs/TESTES-ETAPA0.md`: nomes dos WAV, `events.csv`, `expected.csv`, `trajectory.csv` (`t_ms,cursor,move,score`), métricas e latências. `docs/CONFORMIDADE.md`: licenças (Vosk e modelo Apache 2.0; JNA sob Apache 2.0; modelo grande GPLv3 proibido no app).
- `registers/opr_register.md`: OPR-001, 002, 004, 005, 006, 007, 008, 013 e 018 tocam esta tarefa.
- Estado do repositório (`main`, com 0001 e 0002): `etapa0/:core` (JVM puro) tem os contratos `speech` e o `align` completo. O `FakeSpeechEngine` fica em `testFixtures` do `:core`. `etapa0/:app` só tem o manifesto (`RECORD_AUDIO`, `allowBackup="false"`), o `strings.xml` e um teste. O catalog não tem Compose nem Vosk. `.gitattributes` já põe `etapa0/**/model-pt/**` no LFS. As tarefas `verifyMergedManifest` e `verify16kAlignment` existem: a segunda lê o APK de release e exige `libvosk.so` e `libjnidispatch.so` em toda ABI.
- `SpeechEvent.Partial`/`Final` não têm tempo. Nada de tempo pode entrar no evento sem mudar o contrato.

## Escopo

O PROCESS.md define esta tarefa como uma só, e ela tem quatro partes ligadas pelo contrato: motor (`VoskEngine`), instalação (`ModelInstaller`), tela mínima com a ligação ao `align` e painel de depuração com exportação. Ela não precisa ser decomposta. O risco está em quanto dela se verifica sem aparelho, e o spec tem de deixar essa linha clara. Decisão de forma: a lógica fica separada do Android sempre que der, para ser testada na JVM, e o que depende do aparelho vai para a 0005.

## Abordagens consideradas (para o spec)

1. **Spec curto, só "conforme o TRD".** Rejeitada. O TRD deixa abertos pontos que mudam o que a 0005 consegue medir: base de tempo da exportação, formato das transcrições brutas, como alternar entre reconhecedor novo e `reset()`, o padrão do vocabulário e do limiar de nível. E o portão não teria critério por comando.
2. **Spec que fixa as lacunas e divide os critérios em dois grupos**: o que se verifica por comando (build, JVM, manifesto, 16 KB, R8) e o que só o aparelho valida (0005, sem bloquear). **Escolhida.**
3. **Spec com desenho de classes e threads.** Rejeitada: o desenho é do `arquiteto`.

## Decisões

Cada decisão traz a pergunta, as alternativas, a escolha e o motivo. Nenhuma muda a assinatura ou a semântica de um contrato do TRD.

**D1. Como testar o motor sem aparelho?**
- (a) Só teste instrumentado.
- (b) Separar o laço de captura e sessões das classes do Android e do Vosk por costuras internas (fonte de áudio, fábrica de reconhecedor), com fakes na JVM.
- **Escolha: (b).** As regras do contrato (id preso ao reconhecedor, callback atrasado com id antigo, quadros fora de sessão descartados, buffer esvaziado no `endSession`, `Level` com e sem sessão) são lógica, e o portão pede comando. Os testes JVM não podem carregar classes `org.vosk.*`, que puxam a biblioteca nativa pela JNA. As costuras são internas ao `:app` de `etapa0/` e não são contrato do TRD. Onde ficam e como se chamam é decisão do arquiteto.

**D2. O que é o valor de `Level(rms)`?**
- (a) RMS bruto em amostras de 16 bits.
- (b) RMS normalizado em [0, 1]: `sqrt(média(s²)) / 32768`.
- (c) dBFS.
- **Escolha: (b).** É uma definição, não um limiar. Cabe no `Float` do contrato, é estável entre aparelhos e serve direto de escala para o indicador (R7) e para o limiar da 0005. Um quadro de silêncio dá 0, e onda quadrada em fundo de escala dá 1,0 (± 1e-3). A 0005 pode converter para dBFS no relatório.

**D3. Limiar mínimo de nível para ignorar parciais (TRD Captura e sessões; PRD Robustez).** O valor não existe nos documentos ("calibrado na Etapa 0").
- (a) Inventar um padrão.
- (b) Limiar ajustável no painel, com padrão 0 (desligado), e o nível exportado junto com as hipóteses.
- **Escolha: (b).** Com isso não se inventa número, e a 0005 calibra offline a partir de `level.csv` e `hypotheses.csv`. O filtro fica em quem consome os eventos (o ViewModel da tela), não no motor, porque o motor só emite (TRD Módulos). O parcial é ignorado quando o último `Level` recebido está abaixo do limiar.

**D4. Reconhecedor novo por sessão ou `reset()` (decisão em aberto do TRD, a medir na Etapa 0).**
- **Escolha:** o `VoskEngine` da etapa0 tem as duas estratégias, escolhidas por parâmetro de construção (não pela interface `SpeechEngine`) e alternáveis no painel com a captura parada. O padrão é **reconhecedor novo**, que é o texto principal do TRD. Nas duas, `beginSession` cria um `SessionId` novo, preso ao objeto de sessão que embrulha o reconhecedor. Com `reset()`, se o vocabulário mudou desde a sessão anterior, cria-se um reconhecedor novo. O painel mostra quanto tempo `beginSession` levou, que é a medida que decide.

**D5. Vocabulário restrito (decisão em aberto do TRD).**
- **Escolha:** liga e desliga no painel; o padrão é **desligado**. A lista é derivada de `Token.spoken` (todas as palavras, sem repetição) mais `[unk]`, como diz o TRD.
- **Risco a conferir no PLAN (Q4):** `Token.spoken` sai normalizado, sem acento ("tres", "versiculo"). O léxico do modelo tem a grafia com acento, e o Vosk ignora com aviso as palavras que não conhece. Se o arquiteto confirmar isso no modelo real, a lista na forma do TRD fica quase vazia nas palavras acentuadas. É divergência entre documento e realidade: ele para, descreve e propõe a forma da lista (por exemplo, a grafia do texto original em minúsculas, mais as formas faladas). Não contornar em silêncio. Vai para o registro (OPR-020).

**D6. Base de tempo das exportações sem mudar o contrato.**
- (a) Acrescentar tempo a `SpeechEvent` (muda contrato: CIA e portão humano).
- (b) API paralela no `VoskEngine` com o tempo do áudio.
- (c) Quem consome carimba cada evento ao receber, com relógio monotônico, em ms desde o `startCapture` da gravação.
- **Escolha: (c).** Não toca o contrato, e com captura em tempo real o tempo de recepção acompanha o tempo do áudio, com atraso de um buffer. Com a fonte WAV (D8), o tempo do áudio e o tempo de recepção coincidem por construção. A latência 1 (fim da palavra no áudio → `Partial`) é calculada offline pela 0004/0005, com `expected.csv` e `hypotheses.csv`. O painel mostra ao vivo os trechos 2 e 3.

**D7. Formato da exportação de transcrições brutas (golden da 0006, OPR-008).**
- **Escolha:** `<nome>.hypotheses.csv` com cabeçalho `t_ms,session,kind,words`, `kind` ∈ {`partial`, `final`, `error`}, `words` com as palavras exatamente como o Vosk devolveu, separadas por um espaço e entre aspas (no `error`, o nome do `EngineError`). Uma linha por evento, inclusive parciais repetidos e vazios, porque o golden tem de reproduzir o que o motor emitiu, sem filtro. Inclui os eventos de sessões já encerradas, com o id original, marcados como descartados ou não (coluna extra `accepted` = `1`/`0`). Assim o golden reproduz também o descarte do passo 7 (Q1: consumidores são a 0006 e a latência da 0005).
- Ao lado vão `<nome>.level.csv` (`t_ms,rms`, um por bloco de áudio lido; consumidor: calibração do limiar na 0005), `<nome>.trajectory.csv` (`t_ms,cursor,move,score`, como em TESTES-ETAPA0: uma linha por mudança de cursor e por `reposition`, com `move` = nome do `Move`; consumidor: `metricas.py`), `<nome>.events.csv` com os eventos automáticos (D10) e `<nome>.meta.txt` (D11).
- Critério de leitura por teste JVM: uma função lê `hypotheses.csv` de volta para a sequência de `SpeechEvent`, e o reconciliador produz as mesmas palavras entregues que produziu ao vivo (ida e volta).

**D8. Fonte de áudio: só microfone ou também arquivo WAV?**
- (a) Só microfone.
- (b) Microfone (padrão) ou um WAV de teste lido do armazenamento do app, enviado por `adb push`, entregue ao reconhecedor no ritmo real.
- **Escolha: (b).** A suíte "Integração de voz" do TRD já pressupõe alimentar o `VoskEngine` com áudios de textos de teste. Com o WAV, a trajetória do app fica no mesmo relógio que o `expected.csv` gerado do mesmo arquivo, e a 0005 pode repetir a mesma gravação com presets, vocabulário e estratégia diferentes. É só leitura: o app não grava áudio (regra 3). Um WAV fora de 16 kHz, mono, PCM 16 bits é recusado com erro. O `Level` também é emitido nessa fonte. A troca de fonte fica atrás da costura de D1, não da interface `SpeechEngine`.

**D9. Exportação só com textos de teste: como garantir (TRD Privacidade nos testes; regra 3)?**
- (a) Confiar em quem usa.
- (b) Os textos de `etapa0/textos/` entram como assets do app de etapa0, e um seletor os carrega. A exportação só fica habilitada quando o texto atual é idêntico a um deles.
- **Escolha: (b).** É verificável na JVM (função pura texto → ID do texto de teste ou nada) e impede que um discurso digitado vire arquivo. Digitar livremente continua possível para experimentar, mas sem exportação.

**D10. A tela mínima precisa de toque em palavra?** O L30 tem "toque numa palavra 3 parágrafos à frente" (min 22) e o T07 tem pausa por toque.
- **Escolha:** sim, mínimo. O botão Pausar faz a pausa do T07. Tocar numa palavra (com `getOffsetForPosition` conferido com `getBoundingBox` + 8 dp, CLAUDE.md "Erros comuns") faz `endSession`, `reposition` e `beginSession` (TRD Máquina de estados, Reading). Pausar, Ouvir e o toque gravam sozinhos `pausa_toque`, `retoma_toque` e `toque_palavra` (detalhe = índice do token) no `events.csv`. Os outros eventos (improviso, silêncio, pulo) continuam preenchidos à mão na 0005, como diz TESTES-ETAPA0.

**D11. Proveniência das exportações (Q8).**
- **Escolha:** `<nome>.meta.txt` (chave=valor) com versão do app, versão do `vosk-android`, ID do modelo (conteúdo do arquivo de versão, D12), preset, vocabulário ligado ou não, estratégia de reconhecedor, fonte de áudio (e nome do WAV), limiar, duração da instalação e do `prepare`, modelo do aparelho e versão do Android. Consumidores: relatório da 0005 e conferência de que o golden da 0006 corresponde ao modelo em assets.

**D12. Arquivo de versão do modelo (TRD Instalação 2).**
- **Escolha:** um arquivo `uuid` dentro de `assets/model-pt/`, que é o mecanismo que o `StorageService.unpack` do Vosk usa para não copiar de novo. O conteúdo é `vosk-model-small-pt-0.3` mais o SHA-256 do zip baixado, e esse mesmo conteúdo vai para o `meta.txt`. A comparação "mesma versão → não copia; diferente ou ausente → copia" é função pura testada na JVM. O arquiteto confirma o comportamento real do `StorageService` lendo a fonte da versão escolhida (Q4).
- **Achado para o app (não para a 0003):** se o `StorageService.unpack` só avisa no fim, sem progresso, a barra da tela 1b (TRD Instalação 1) vai exigir cópia própria ou progresso indeterminado. Na etapa0 basta "Preparando modelo…" e a duração. Registrado como OPR-021.

**D13. Duração da animação de rolagem da tela mínima (latência 3).** O PRD dá a faixa de R5 (150–600 ms), mas não dá padrão.
- **Escolha:** ajustável no painel entre 150 e 600 ms, com valor inicial de **375 ms (PROVISÓRIO, ponto médio da faixa do R5, decisão do analista, só para a etapa0)**. Nenhum critério depende desse número. A falta de padrão do R5 no PRD vai para o registro, porque o app vai precisar dele (OPR-022, dono analista, decisão humana).

**D14. O que acontece com a hipótese pendente no `endSession`?**
- **Escolha:** é descartada. Não se chama `finalResult` e nenhum `Final` sai depois do `endSession` por causa dele ("descarta hipóteses e áudio pendente", contrato). Um evento que já estava sendo emitido no momento do `endSession` sai com o id antigo e é descartado pelo consumidor.

**D15. Erros do motor.**
- **Escolha:** falha ao abrir o `AudioRecord` gera `Error(null, MicUnavailable)`. Sem permissão de microfone no `startCapture`, ou permissão perdida durante a captura, gera `Error(null, PermissionRevoked)`. Modelo ausente gera `ModelMissing` e falha de carga gera `ModelCorrupt`; nos dois, `prepare()` devolve falha e o estado vai a `Failed`. Exceção do reconhecedor dentro da sessão gera `Error(session, Internal)`. Na tela, um erro pausa, mostra o motivo e oferece "Tentar de novo". As transições de `EngineState` seguem as do `FakeSpeechEngine`.

**D16. Ciclo de vida na tela mínima.**
- **Escolha:** vale o TRD Segundo plano. `onStop` com `isChangingConfigurations` não faz nada. Ida real ao segundo plano faz `endSession` e `stopCapture`, e a tela fica pausada. O modelo é carregado uma vez por processo. O texto vive só no ViewModel da tela: nada de `rememberSaveable`, `SavedStateHandle`, DataStore ou arquivo (regra 2, aplicada também à etapa0). Os ajustes do painel ficam em memória; a etapa0 não tem DataStore.

**D17. Ligação com o `align` na tela.**
- **Escolha:** descarta evento de sessão que não é a ativa antes do reconciliador (TRD Reconciliação 7; regra 7). Depois, `PartialReconciler` → cauda de palavras confirmadas, zerada em `reposition` e no começo de cada sessão (0002 D1) → `Aligner.advance` → cursor. A leitura começa com `reposition(0)` (0002 D3). O preset é escolhido no painel (Conservador, Normal, Ágil; padrão Normal, que é o meio da tabela do PRD e não é número novo). Essa ligação é testada na JVM com o `FakeSpeechEngine`. O processamento roda fora da thread principal (TRD Captura e sessões).

**D18. Painel de depuração: o que mostra (TRD Etapa 0, item 4).**
- **Escolha:** último parcial e último final brutos, cauda de palavras confirmadas, `score` de cada avanço, `lastMove`, `cursor`, `highWater`, `missStreak`, sessão ativa, `EngineState`, nível, latência 2 (recepção do `Partial` → cursor publicado) e latência 3 (cursor → fim da animação), duração da instalação, do `prepare` e do `beginSession`, e o tempo de `beginSession` até o primeiro parcial não vazio (início da escuta). Os controles são preset, vocabulário, estratégia, fonte de áudio, limiar, duração da animação e nome/exportação. A latência 1 não aparece ao vivo (D6).

**D19. Release da etapa0: R8 e assinatura.**
- **Escolha:** release com R8 ligado e as regras do TRD Distribuição (`org.vosk.**`, `com.sun.jna.**`), para as regras do app serem provadas cedo. A assinatura é a chave de debug, para a 0005 instalar o release no aparelho; a etapa0 não é distribuída. Por comando, o `mapping.txt` mostra `org.vosk.Recognizer` e `com.sun.jna.Native` mantidos com o mesmo nome. No aparelho, a 0005 confirma que o release carrega o modelo e reconhece.

**D20. 16 KB contra as bibliotecas reais (TRD Qualidade de código; OPR-005).**
- **Escolha:** `verify16kAlignment` roda contra o APK de release com `libvosk.so` e `libjnidispatch.so` reais, em todas as ABIs que as dependências trazem. A verificação também cobre o AAB (`bundleRelease`): segmentos ELF das duas `.so` em `base/lib/<abi>/` e a presença das duas em toda ABI. O `zipalign` dos APKs que o Play gera a partir do AAB fica documentado como responsabilidade do Play.
- **Se reprovar:** é um resultado esperado da Etapa 0 (critério de saída "alinhadas ou caminho definido") e não reprova a 0003, desde que a saída real esteja anexada, por biblioteca e ABI, com o alinhamento encontrado, e um OPR esteja aberto com os caminhos (versão mais nova do `vosk-android` ou da JNA pelo catalog; recompilação). A verificação não é afrouxada nem desligada. Recompilar o Vosk exige instalar o NDK e a toolchain, o que é portão humano 6, e por isso não entra nesta tarefa.

**D21. Ligação do `verifyMergedManifest` ao build (OPR-004).**
- **Escolha:** TRD Manifesto, "o build verifica o manifesto final mesclado". `check`, `assembleRelease` e `bundleRelease` passam a depender da verificação da variante. Isso é conferido por `--dry-run`, e a prova negativa da 0001 continua passando.

**D22. ktlint do `:app` (OPR-018).**
- **Escolha:** critério por comando: o `ktlintCheck` do `:app` analisa os fontes `main` e `test` (nenhuma tarefa de fonte do `:app` com `NO-SOURCE`) e falha com uma violação proposital num arquivo do `:app` (Q2).

**D23. `dataExtractionRules` (OPR-002).**
- **Escolha:** adotar já. Um recurso XML de regras vazias, que exclui tudo de nuvem e de transferência entre aparelhos, referenciado no manifesto junto com `allowBackup="false"`, como o TRD Manifesto pede. Não é layout, então não fere a regra 8. O aviso `DataExtractionRules` do lint some.

**D24. Permissão `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` do `androidx.core` (OPR-001).** Compose é obrigatório (regra 8) e traz `androidx.core` 1.9+, que injeta essa permissão no manifesto mesclado.
- (a) Removê-la com `tools:node="remove"` e não usar `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)`.
- (b) Mudar a regra 1 para aceitá-la.
- (c) Afrouxar o `verifyMergedManifest`.
- **Recomendação: (a).** (b) muda regra inegociável, e (c) é proibido (regra de processo 6). O risco de (a) é alguma biblioteca do grafo depender dessa permissão. O arquiteto confere no PLAN, buscando chamadas de `registerReceiver` com `RECEIVER_NOT_EXPORTED` nas dependências.
- **Portão humano 1:** o OPR-001 diz "decidir com o humano", e qualquer mudança de manifesto pede CIA e aprovação humana (CLAUDE.md, regra de processo 3). Esta decisão vai ao orquestrador.

**D25. O que mais muda no manifesto.** Além de D23 e D24: a declaração da Activity principal (lançador). Também é mudança de manifesto, e na dúvida cai no mesmo portão 1. Para o humano decidir uma vez só, D23, D24 e D25 vão juntos numa CIA do arquiteto, antes do BUILD. Nenhuma permissão nova entra: o manifesto mesclado tem de continuar com exatamente `RECORD_AUDIO`.

**D26. Dependências novas e de onde vêm.**
- **Escolha:** pelo version catalog, na versão estável mais recente do dia (TRD Stack), conferida pelo arquiteto no `maven-metadata.xml` do repositório:
  - Maven Central: `com.alphacephei:vosk-android` (AAR). A versão mais recente que o analista conhece é a 0.3.47; o arquiteto confirma se há outra mais nova. JNA `net.java.dev.jna:jna` com `@aar`, na versão que o POM do Vosk pede, a não ser que o 16 KB exija uma mais nova (D20). O `dependencies` do `:app` é anexado.
  - Google Maven: Compose BOM, `activity-compose`, `material3`, `lifecycle-viewmodel-compose` e `lifecycle-runtime-compose`.
  - alphacephei.com: `https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip` (modelo pequeno, Apache 2.0, por volta de 31 MB), descompactado em `etapa0/app/src/main/assets/model-pt/` (LFS) com o arquivo `uuid` de D12, e o SHA-256 do zip registrado. O modelo grande (GPLv3) não entra no app (CONFORMIDADE).
- **Portões:** baixar dependências pelo Gradle é o mesmo caminho que a 0001 já usou (AGP, Kotlin), e o modelo e o AAR já estão decididos no TRD e no PRD aprovados. Isso não é instalação nova e não cai em portão. Duas condições caem no portão 6 e voltam ao orquestrador se acontecerem: o `git lfs` não estar instalado na máquina (`git lfs version` falha) e o envio ao GitHub passar da cota gratuita de LFS. Uma dependência que traga permissão vai para o portão 1 (D25).

**D27. Teste instrumentado "Integração de voz" do TRD.**
- **Escolha:** fica fora da 0003, porque precisa dos WAV gravados na 0005 e de aparelho. A 0003 entrega a fonte WAV (D8), que esse teste vai usar. Registrado como OPR-019 para o arquiteto atribuir (0005 ou 0006).

**D28. Logs.**
- **Escolha:** nenhuma chamada de `android.util.Log` no `:app` recebe texto, palavras, hipóteses ou JSON do Vosk, nem na etapa0 (Q6; TRD "o texto colado nunca vai para log"). O painel mostra só em memória.

## Linha entre comando e aparelho

- **Por comando (bloqueia a 0003):** build debug e release, AAB, testes JVM da lógica (D1, D2, D7, D9, D12, D14, D15, D17), manifesto mesclado e prova negativa, ligação ao `check` e ao empacotamento, 16 KB executado contra as `.so` reais com a saída anexada (D20), R8 no `mapping.txt`, ktlint do `:app` com prova negativa, lint, greps de privacidade e contrato intocado.
- **Só no aparelho (0005, não bloqueia):** JSON real do Vosk (Q4), cópia e carga do modelo, `AudioRecord` a 16 kHz, permissão em tempo de execução, latências, início da escuta, memória, comparação entre reconhecedor novo e `reset()`, vocabulário com o modelo real, toque e rolagem, exportação puxada por `adb`, release com R8 funcionando.

## Perguntas que caem em portão humano

1. **Portão 1 (manifesto):** D23, D24 e D25 juntos. A recomendação é remover `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` com `tools:node="remove"`, adicionar `dataExtractionRules` vazias e declarar a Activity. Antes, a CIA do arquiteto.
2. **Portão 6, condicional (D26):** só se o `git lfs` não estiver instalado ou se a cota gratuita de LFS for passar.
