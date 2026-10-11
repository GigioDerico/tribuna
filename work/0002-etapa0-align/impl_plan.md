# Plano — 0002-etapa0-align

**Autor:** arquiteto · **Spec:** `spec.md` (conferido pelo orquestrador em 2026-10-10) · **Aprovação:** conferida pelo orquestrador em 2026-10-11 (checklist do PROCESS.md: critérios mapeados a teste e comando, testes antes do código, Q6 preenchida, contratos intactos, sem permissão nova; A2 é refino conservador do algoritmo, sem mudar assinatura, campo, invariante ou parâmetro, então não é portão humano; texto do TRD fica em OPR-009)

> **Para o agente de construção (`motor-voz`, Sonnet):** execute tarefa por tarefa, na ordem, marcando os checkboxes (`- [ ]`). Só comece com a conferência do orquestrador registrada no cabeçalho acima. Não chame subagentes nem outros agentes. O código deste plano foi compilado, formatado pelo ktlint 1.8.0 e testado pelo arquiteto numa cópia descartável de `etapa0/` (ver "Prova do plano"): copie-o como está. Desvio do plano vai para o "Registro da construção" no fim, com o motivo. Se o código não couber num contrato do TRD, pare e escale ao `arquiteto` (CLAUDE.md, regra 5). Três falhas seguidas no mesmo problema: pare e escale (CLAUDE.md, processo 4).

**Objetivo:** implementar em Kotlin puro, no pacote `align` do `:core` de `etapa0/`, o `Normalizer`, o `Tokenizer` (com formas faladas), o `PartialReconciler` e o `Aligner` (`advance` e `reposition`), com as suítes "Reconciliação de hipóteses" e "Alinhamento" do TRD.

**Arquitetura:** funções e objetos puros, sem I/O e sem Android. `Normalizer` e `NumberWords` alimentam o `Tokenizer`, que produz os `Token` do contrato. O `PartialReconciler` é um objeto com estado de uma frase. O `Aligner` lê a cauda de palavras confirmadas e devolve um novo `AlignState`; a similaridade é Levenshtein próprio (`Similarity`). Os testes leem os textos de `etapa0/textos/` por uma propriedade de sistema que o Gradle passa ao `test`, e simulam trajetórias com um executor de teste (`TrajectoryRunner`).

**Stack:** Kotlin 2.4.21 (JVM 21) · JUnit 4.13.2 · ktlint 1.8.0 (`ktlint_official`) · Android Lint · Gradle 9.7.1. Nenhuma dependência nova.

---

## Resumo

Primeiro a ligação dos testes com `etapa0/textos/`. Depois, em TDD e nesta ordem, `Normalizer`, `Similarity`, `NumberWords`, `Tokenizer` (com `AlignConstants` e `AlignPresets`), `PartialReconciler` e, por fim, o `Aligner`, cujas suítes (pontuais, trajetórias T01–T09 e L30 sintético) são escritas inteiras antes da implementação. Fecha com as quebras propositais (Q2) conferidas localmente e a verificação completa com evidências na pasta da tarefa.

## Decisões do arquiteto

### A1. OPR-007 (a): `words` como cauda é interpretação do contrato, não mudança de semântica

Decisão: **interpretação**. Não há CIA nem portão humano.

Motivos, lidos no TRD real:

1. O contrato (TRD Contratos §Aligner) diz só "recebe o estado e as palavras confirmadas e devolve o novo estado"; a assinatura é `advance(state: AlignState, words: List<String>)`. O texto não diz "palavras novas". A semântica de `words` nunca foi fixada; o spec a fixa sem mexer em tipo, nome, campo ou invariante.
2. O algoritmo do mesmo TRD (§Alinhamento 2 e 4) compara "as últimas *k* palavras confirmadas" e "as últimas 4 ou mais". Como o `AlignState` não guarda palavras e o `Aligner` é função pura, a leitura "só as palavras novas" tornaria o TRD inexequível (o reconciliador entrega 1 ou 2 palavras por parcial). Entre as leituras possíveis do contrato, a cauda é a única coerente com o resto do documento. Escolher entre leituras de um texto ambíguo é interpretação; mudança seria alterar algo que o texto fixa.
3. Busca real: não há nenhum chamador de `advance` ou `reposition` no repositório (`grep -rn -E "advance\(|reposition\(|Aligner\(" etapa0 docs`): só a declaração em `Aligner.kt`, a construção em `AlignContractShapeTest.kt` e o bloco do TRD. Nada quebra.
4. A responsabilidade nova de quem chama (manter a cauda e zerá-la em `reposition` e ao abrir sessão) cabe no papel que o TRD já dá ao `PrompterViewModel` ("passa as palavras ao alinhador"; único dono do estado da leitura). Quem chama pode guardar só as últimas `max(k, 4)` palavras: o `Aligner` não lê mais que isso.

Proposta de esclarecimento do TRD (não aplicada; o TRD só muda com aprovação humana, e esta proposta não bloqueia nada). Em Contratos §Aligner, depois do bloco de código, antes de "Invariante":

> `words` é a cauda das palavras confirmadas desde o último `reposition` ou o início da sessão, em ordem, terminando na mais nova. Quem chama mantém a cauda (basta guardar as últimas max(k, 4)), chama `advance` uma vez por lote de palavras novas e zera a cauda em `reposition` e ao abrir sessão. A leitura começa com `reposition(0)`; o cursor é o índice do último token falado.

### A2. A palavra mais nova precisa casar com o token vencedor (refino do §Alinhamento 3)

Achado do arquiteto ao provar o plano (protótipo, ver "Prova do plano"): com o algoritmo ao pé da letra, a primeira palavra fora do roteiro avança o cursor um token. As *k* − 1 palavras anteriores já casam com a posição seguinte, então a média fica em (*k* − 1)/*k*: 0,667 ≥ 0,60 no Normal e 0,75 ≥ 0,70 no Conservador. No T04 isso move o cursor na primeira palavra do improviso e reprova o critério "zero mudanças de cursor durante o improviso" (e a métrica de salto errado da TESTES-ETAPA0).

Decisão: a posição vencedora da janela só conta se a palavra mais nova casar com a última palavra da forma falada usada naquele token com similaridade ≥ limite curto. Se não casar, o resultado é um miss (conta para a recuperação). Na recuperação, conta-se a ambiguidade antes desse filtro (todas as posições ≥ limite longo) e o único candidato também precisa passar no filtro.

Por que não é mudança de contrato: assinatura, campos e invariante ficam iguais; nenhum parâmetro novo (usa o limite curto que já existe); só torna o alinhador mais conservador, como pedem o princípio 3 do PRD e a regra 6 do CLAUDE.md. Custo medido no protótipo: uma palavra de espera quando a mais nova vem errada; o critério "palavras erradas" continua com atraso ≤ 3 em 100% das amostras.

Proposta de texto do TRD (Motor de voz §Alinhamento 3, acrescentar ao fim): "A posição só conta se a palavra mais nova casar com o seu token com similaridade ≥ limite curto; senão, o resultado é um resultado sem avanço." Registrado como OPR-009 para o humano aplicar ao TRD quando quiser (não bloqueia).

### A3. OPR-007 (b): recuperação ambígua

Fica como D8: salta só com exatamente uma posição ≥ limite longo à frente de `highWater`; com duas ou mais, o cursor fica parado. A margem "próximos do limite" não existe no PRD/TRD e vai para a calibração da 0005. Proposta de texto (Motor de voz §Alinhamento 4, segunda frase): "Salta apenas se existir exatamente uma posição com similaridade ≥ limite longo; com duas ou mais, o cursor fica parado. Uma margem abaixo do limite, se necessária, sai da calibração da Etapa 0."

### A4. Desenho interno (não é contrato; consumidor declarado em cada item, Q1)

- **Pontuação de uma posição** (TRD §Alinhamento 1 e 2): a palavra mais nova casa com a última palavra de uma forma falada do token candidato; as anteriores andam para trás pelo roteiro, token a token; cada token usa a forma que dá a maior média ponderada (todas as combinações são testadas; são no máximo 4 palavras). Peso de cada palavra = `Token.weight` do token de onde ela vem. Antes do início do roteiro, cada palavra que sobra conta com similaridade 0 e peso 1. Consumidor: `searchWindow` e `recover`.
- **Desempate na janela** (D9): maior média; empate (diferença ≤ 1e-9) → menor `|posição − cursor|`; empate de novo → menor índice (a busca é crescente).
- **`score`** = melhor média avaliada na chamada (inclusive na recuperação, quando ela roda), mesmo em `Stay` (D4). Consumidor: painel de depuração da 0003.
- **`PartialReconciler`**: `onPartial(words)`, `onFinal(words)`, `reset()`, todos devolvendo as palavras novas. Igualdade de palavras pela forma normalizada. Ponto alinhado (passo 3) = tamanho do prefixo de menor distância de edição com `confirmadas`; no empate, o maior (prefere não repetir palavra). Consumidor: tela da 0003, depois o ViewModel.
- **`AlignPresets`** (`conservador`, `normal`, `agil`) e **`AlignConstants`** (distância curta 5, recuo 3, 2 misses, 4 palavras, lista e peso das palavras comuns): objetos públicos do `align`, num lugar só (D10–D12). Consumidor: testes agora; 0003 (seletor de sensibilidade) e 0005 (calibração).
- **Formas faladas**: números soltos têm masculino, feminino quando difere (D15) e catorze/quatorze; referências e decimais só masculino (capítulo e versículo são masculinos) com catorze/quatorze em todas as combinações. A ordem das formas de uma referência começa pelos três exemplos do TRD.
- **`NumberWords.require`** não põe o número na mensagem (Q6: nada do roteiro em exceção que possa ir a crash report). O `Tokenizer` só o chama com valor na faixa.

### A5. OPR-008: golden de transcrições

Decisão: nova tarefa **`0006-etapa0-golden`** (`motor-voz`), aberta pelo orquestrador logo depois que a 0005 produzir as transcrições exportadas, antes da aprovação da arquitetura. Ela cria a suíte "Golden de transcrições" (JUnit em `:core`, lendo as transcrições brutas exportadas e comparando a trajetória com o `esperado`). Requisito para o PLAN da 0003: o arquivo de exportação de transcrições precisa guardar a sequência bruta de eventos `Partial`/`Final` com o tempo de cada um, num formato que um teste JVM leia sem Android. Se a 0005 levar à troca de motor, a 0006 é refeita com o motor novo (TRD Riscos: "Outro motor exige recalibrar o alinhador").

### A6. Achado para a 0005 (fora dos critérios do spec): Ágil com *k* = 2

No protótipo, o preset Ágil (só exigido no critério de leitura normal) dá salto errado quando duas palavras comuns seguidas casam com o roteiro ("e a" no T04 com improviso do T03; troca de palavra no T01) e no take C do T02 ("capítulo vinte e quatro versículo" casa com "4 versículos" do próprio texto). Com duas palavras comuns, a média ponderada é 1,0 qualquer que seja o peso. Não é critério desta tarefa (o spec usa o Normal fora da leitura normal) e não se muda parâmetro sem calibração. Registrado como OPR-010 (dono: arquiteto; resolver na calibração da 0005).

## Prova do plano (feita pelo arquiteto em 2026-10-11)

Cópia descartável de `etapa0/` no scratchpad da sessão (não versionada), com exatamente o código deste plano:

- `./gradlew :core:test`: 85 testes, 0 falhas (60 novos no pacote `align`, mais os 25 da 0001).
- `./gradlew :core:ktlintCheck :core:lint`: verde; o lint só tem o aviso preexistente `GradleDependency` (compileSdk 37) do `libs.versions.toml`.
- `./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest`: `BUILD SUCCESSFUL`, manifestos debug e release com só `RECORD_AUDIO`.
- L30 sintético: 3.399 chamadas de `advance`, total ≈ 127 ms, máximo ≈ 3,2 ms (JVM do Mac; evidência para a 0005, sem limiar, D23).
- Quebras propositais (Tarefa 8): as sete ficaram vermelhas no teste previsto.

## Arquivos que mudam

`<align-main>` = `etapa0/core/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/align/`; `<align-test>` = `etapa0/core/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/align/`.

| Arquivo | Mudança | Requisito / seção do TRD | Agente |
|---|---|---|---|
| `etapa0/core/build.gradle.kts` | Passa `etapa0/textos/` ao `test` (propriedade `tribuna.textos`, declarada como entrada) | CLAUDE.md "Textos de teste"; spec, critério de textos | motor-voz |
| `<align-main>Normalizer.kt` | Novo | TRD Motor de voz §Normalização e tokens; D13, D18 | motor-voz |
| `<align-main>Similarity.kt` | Novo (`internal`): Levenshtein e similaridade; `[unk]` = 0 | TRD §Stack (Levenshtein próprio), §Alinhamento 2; D13 | motor-voz |
| `<align-main>NumberWords.kt` | Novo (`internal`): números por extenso 0–999.999 | TRD §Normalização e tokens; D14, D15 | motor-voz |
| `<align-main>AlignConstants.kt` | Novo: constantes fixas e peso PROVISÓRIO das comuns | PRD Algoritmo; TRD §Alinhamento; D10, D12 | motor-voz |
| `<align-main>AlignPresets.kt` | Novo: três `AlignParams` do PRD | PRD R4 e tabela; D11 | motor-voz |
| `<align-main>Tokenizer.kt` | Novo | TRD Contratos §Token; §Normalização e tokens; PRD T5; D14–D20 | motor-voz |
| `<align-main>PartialReconciler.kt` | Novo | TRD §Reconciliação de hipóteses 1–6 | motor-voz |
| `<align-main>Aligner.kt` | Corpo de `advance` e `reposition` (assinaturas idênticas às da 0001) | TRD Contratos §Aligner; §Alinhamento 1–6; A1–A4 | motor-voz |
| `<align-test>TestTexts.kt` | Novo: leitura de `etapa0/textos/` | spec, critério de textos | motor-voz |
| `<align-test>Trajectory.kt` | Novo: `Step`, `Sample`, `Speech`, `TrajectoryRunner` e métricas (D21) | spec, Aligner; TESTES-ETAPA0 Métricas | motor-voz |
| `<align-test>NormalizerTest.kt`, `SimilarityTest.kt`, `NumberWordsTest.kt`, `TokenizerTest.kt`, `TokenizerTextsTest.kt`, `PartialReconcilerTest.kt`, `AlignerTest.kt`, `AlignerTrajectoryTest.kt`, `AlignerL30Test.kt` | Novos | TRD Testes, suítes "Reconciliação de hipóteses" e "Alinhamento" | motor-voz |
| `work/0002-etapa0-align/evidencias/*` | Saídas dos comandos de verificação | CLAUDE.md processo 8 | motor-voz |
| `work/0002-etapa0-align/impl_plan.md` | Só o "Registro da construção" no fim | PROCESS.md §3 | motor-voz |

Não mudam: `Token.kt`, `AlignState.kt`, `AlignParams.kt`, `Move.kt`, `AlignContractShapeTest.kt`, todo o pacote `speech`, `:app`, `build-logic`, manifesto, catalog.

## Ordem e paralelismo

1. Contratos: nenhum muda. `align/` inteiro nesta tarefa, por `motor-voz`, na ordem das tarefas abaixo: Normalizer → Similarity → NumberWords → Tokenizer → PartialReconciler → Aligner → trajetórias e L30.
2. `speech/`: nada nesta tarefa (0003).
3. `viewmodel/`: nada (app, depois da Etapa 0).
4. `ui/`: nada (0003 tem a tela mínima).

Não há trabalho de `interface` nesta tarefa. A 0003 pode ser planejada em paralelo, porque depende só dos contratos (já fixos) e das APIs públicas deste plano: `Tokenizer.tokenize`, `PartialReconciler.onPartial/onFinal/reset`, `Aligner`, `AlignPresets`.

## Testes primeiro

Todos são escritos e vistos falhando (erro de compilação ou `NotImplementedError`) antes do código que os satisfaz:

- [ ] `NormalizerTest` (6 testes) antes de `Normalizer.kt`
- [ ] `SimilarityTest` (3) antes de `Similarity.kt`
- [ ] `NumberWordsTest` (4) antes de `NumberWords.kt`
- [ ] `TokenizerTest` (14) e `TokenizerTextsTest` (2) antes de `Tokenizer.kt`, `AlignConstants.kt`, `AlignPresets.kt`
- [ ] `PartialReconcilerTest` (10) antes de `PartialReconciler.kt`
- [ ] `AlignerTest` (10), `AlignerTrajectoryTest` (10) e `AlignerL30Test` (1) antes do corpo do `Aligner`, incluindo `AlignerTest.recuosSucessivosNaoPassamDeHighWaterMenos3` e `AlignerTest.recuperacaoAmbigua_naoMoveOCursor_L30`

### Critérios do spec → teste → comando

Todos os comandos rodam em `etapa0/`. "T" = teste JUnit em `:core`, verificado por `./gradlew :core:test`.

| # | Critério do spec | Verificação |
|---|---|---|
| B1 | `:core:test` verde, ≥ 1 teste novo por critério | `./gradlew :core:test` + contagem por classe (Tarefa 9, passo 2); 60 testes novos |
| B2 | `testDebugUnitTest lint ktlintCheck verifyMergedManifest` verde; nenhum `@Ignore` | Tarefa 9, passos 1 e 4 |
| B3 | Sem `import android.`/`androidx.` no `align` | Tarefa 9, passo 3 (grep vazio) |
| B4 | Contratos sem mudança; assinaturas do `Aligner` idênticas | Tarefa 9, passo 5 (`git diff main` vazio; `diff` das linhas de assinatura vazio) |
| B5 | Nenhum `NotImplementedError` no `align` | Tarefa 9, passo 3 (grep vazio) |
| B6 | Textos de teste só de `etapa0/textos/` ou cópia literal; falas conforme D21 | Revisor, por leitura do diff. Variações escritas no teste (permitidas pelo spec): `12:1, 2`, `12:1, 2, 5`, `13:4–7`, `13:4 a 7` |
| N1 | Normalizer: minúsculas, acentos, `ç`, pontuação, idempotente, roteiro = fala | T `NormalizerTest.*` (6) |
| N2 | `index` = posição; `display` bate com uma forma; nenhum token das linhas `//` | T `TokenizerTextsTest.indexIgualAPosicaoEDisplayBateComUmaForma_todosOsTextos`, `TokenizerTextsTest.linhasDeNotaNaoGeramToken_T04eL30` |
| N3 | Exemplos do TRD Contratos | T `TokenizerTest.exemplosDoTrd_24`, `exemplosDoTrd_3_16_umTokenSeparadoDoLivro`, `exemplosDoTrd_13_4a7`, `exemplosDoTrd_12_1e2` |
| N4 | Números e referências do T02 e L30 | T `TokenizerTest.numerosDoT02`, `livroComNumero_1Corintios_D17`, `com1Capitulo_numeroProprioComMasculinoEFeminino`, `numerosDoL30`; `NumberWordsTest.*` |
| N5 | Intervalo com `–` e "a"; lista `12:1, 2, 5` | T `TokenizerTest.intervaloComTravessaoEComA_igualAoHifen`, `listaDeTresItens` |
| N6 | Hifenizadas (`bom-dia`, `escrevê-los`) | T `TokenizerTest.palavraHifenizada_umTokenComDuasFormas_D18` |
| N7 | Peso das comuns < 1 e igual ao provisório (T09) | T `TokenizerTest.pesoDasPalavrasComuns_T09` |
| N8 | `sentenceStart` (T09: 15; T01: parágrafos) | T `TokenizerTest.inicioDeFrase_T09temQuinzeFrases_D20`, `inicioDeFrase_T01primeiroTokenDeCadaParagrafo` |
| R1 | Parciais que só crescem | T `PartialReconcilerTest.parciaisQueSoCrescem_entregamSoOPrefixoEstavelETotalIgualAoFinal` |
| R2 | Inserção, remoção e correção no começo | T `PartialReconcilerTest.correcaoNoComecoDaFrase_naoRepeteNemRetira`, `insercaoNoComecoDaFrase_entregaSoODepoisDoPontoAlinhado`, `remocaoNoComecoDaFrase_entregaSoODepoisDoPontoAlinhado` |
| R3 | Final diferente do último parcial | T `PartialReconcilerTest.finalComMaisPalavras_entregaOQueFaltaEZera`, `finalComMenosPalavras_naoRetiraNada`, `finalComPalavraTrocada_entregaSoODepois` |
| R4 | Frase repetida | T `PartialReconcilerTest.fraseRepetida_mesmoFinalDuasVezesEntregaNasDuas_T08` |
| R5 | Parcial vazio e primeiro parcial | T `PartialReconcilerTest.parcialVazioEPrimeiroParcial_naoEntregamNada` |
| R6 | Sessão nova zera | T `PartialReconcilerTest.reset_sessaoNovaNaoComparaComHipotesesAnteriores` |
| A1 | Leitura normal T01, três presets | T `AlignerTrajectoryTest.leituraNormalT01_tresPresets_cursorIgualAoEsperadoDesdeAKesimaPalavra` |
| A2 | Palavras erradas T01 | T `AlignerTrajectoryTest.palavrasErradasT01_umaEmCincoTrocada` |
| A3 | Pulo de parágrafo T05 | T `AlignerTrajectoryTest.puloDeParagrafoT05_recuperaAte6Palavras` |
| A4 | Salto longo dentro da janela | T `AlignerTrajectoryTest.saltoLongoDentroDaJanelaT01_pularUmaFrase` |
| A5 | Improviso T04 | T `AlignerTrajectoryTest.improvisoT04_cursorParadoERetomaAte6Palavras` |
| A6 | Repetição T08 | T `AlignerTrajectoryTest.repeticaoPeloOradorT08_semSaltoErradoNemRecuoAlemDoLimite` |
| A7 | Trechos repetidos T03 | T `AlignerTrajectoryTest.trechosRepetidosT03_nuncaPulaParaOutraCopia` |
| A8 | Palavras comuns T09 | T `AlignerTrajectoryTest.palavrasComunsT09` |
| A9 | Números e variantes T02, três takes | T `AlignerTrajectoryTest.numerosEVariantesT02_tresTakes` |
| A10 | Invariante `highWater − 3` em recuos sucessivos | T `AlignerTest.recuosSucessivosNaoPassamDeHighWaterMenos3` (Q2: quebras Q2-1, Q2-2, Q2-7) |
| A11 | Recuperação ambígua | T `AlignerTest.recuperacaoAmbigua_naoMoveOCursor_L30` (Q2: quebra Q2-3). O refrão do T03 não serve aqui: as cópias estão a 33 tokens umas das outras, então a janela (W = 40) sempre alcança uma e o caso nunca chega à recuperação; o L30 tem "no fim do ano a" duas vezes, a mais de 1.900 tokens uma da outra |
| A12 | Acentos e maiúsculas; `[unk]` nunca casa | T `AlignerTest.falaComAcentosEMaiusculas_mesmaTrajetoriaQueANormalizada_T03`, `falaComAcentos_referenciasDoT02_mesmaTrajetoria`, `unkNuncaCasa_D13`; `SimilarityTest.of_unkNuncaCasa_D13` |
| A13 | `reposition` (D4) | T `AlignerTest.reposition_segueD4`, `reposition_primeiroAvancoPartedaNovaPosicao`, `reposition_voltaLivrementeAbaixoDeHighWaterMenos3`, `roteiroVazio_advanceDevolveOEstadoERepositionVaiAZero`, `menosDeKPalavras_ficaParadoSemMudarMissStreak_D5` |
| A14 | Cada `Move` aparece | T `AlignerTrajectoryTest.cadaMoveApareceComoLastMove` |
| A15 | L30 sintético, com tempo impresso | T `AlignerL30Test.l30Sintetico_semSaltoErrado_atrasoAte3Em95PorCento_chegaAoFim`; o tempo sai no `system-out` de `core/build/test-results/test/TEST-...AlignerL30Test.xml` |

### Quebras propositais previstas para o QA (Q2)

O `qa-publicacao` aplica cada uma isoladamente em `Aligner.kt`, `PartialReconciler.kt` ou `Similarity.kt`, roda `./gradlew :core:test`, registra o teste que ficou vermelho e desfaz a quebra. O arquiteto já viu as sete falharem na cópia de prova:

| ID | Quebra (troca exata de texto) | Fica vermelho |
|---|---|---|
| Q2-1 | `Aligner.searchWindow`: `maxOf(state.cursor - MAX_BACK_FROM_HIGH_WATER, state.highWater - MAX_BACK_FROM_HIGH_WATER, 0)` → `maxOf(state.cursor - MAX_BACK_FROM_HIGH_WATER, 0)` (limite medido contra o cursor) | `AlignerTest.recuosSucessivosNaoPassamDeHighWaterMenos3` |
| Q2-2 | A mesma expressão → `0` (limite removido) | `recuosSucessivosNaoPassamDeHighWaterMenos3`, `repeticaoPeloOradorT08_semSaltoErradoNemRecuoAlemDoLimite` |
| Q2-3 | `Aligner.recover`: `candidates.singleOrNull()` → `candidates.firstOrNull()` (salta no ambíguo) | `AlignerTest.recuperacaoAmbigua_naoMoveOCursor_L30` |
| Q2-4 | `Aligner.advance`: `if (best.lastWord >= params.shortThreshold) {` → `if (true) {` (sem A2) | `improvisoT04_cursorParadoERetomaAte6Palavras`, `l30Sintetico_...`, `unkNuncaCasa_D13` |
| Q2-5 | `PartialReconciler.reset`: apagar a linha `confirmed.clear()` | `reset_sessaoNovaNaoComparaComHipotesesAnteriores`, `finalComMaisPalavras_entregaOQueFaltaEZera`, `fraseRepetida_...` |
| Q2-6 | `Similarity.of`: apagar a linha `if (a == UNKNOWN \|\| b == UNKNOWN) return 0.0` | `SimilarityTest.of_unkNuncaCasa_D13`, `AlignerTest.unkNuncaCasa_D13` |
| Q2-7 | `Aligner.advance`: `highWater = max(state.highWater, best.position),` → `highWater = best.position,` (highWater segue o cursor) | `recuosSucessivosNaoPassamDeHighWaterMenos3` |

Q3 (contagem mínima) está dentro dos próprios testes: leitura normal exige ≥ (tokens − *k*) movimentos; recuos exigem que um recuo permitido aconteça; ambiguidade exige score ≥ limite longo (as 4 palavras casaram); L30 exige > 3.000 amostras de leitura normal; T09 exige ≥ 50 amostras; textos exigem > 50 tokens cada.

## Privacidade (primeira camada)

- **Caminho do texto.** O roteiro entra em `Tokenizer.tokenize(text)` e sai como `List<Token>` (intervalos e formas faladas) para o `Aligner`, que o guarda em memória (`forms`, `weights`). Nenhuma classe de `align` grava arquivo, abre stream, usa log, `println`, `SavedStateHandle` ou DataStore; nenhuma exceção carrega trecho do texto (A4, `NumberWords.require`). Os testes leem só `etapa0/textos/` (textos escritos para teste) e imprimem só tempos no L30.
- **Caminho da fala.** As palavras reconhecidas passam pelo `PartialReconciler` (lista `confirmed` da frase em curso, em memória, zerada no Final e no `reset`) e pela cauda mantida por quem chama, que o `Aligner` lê e descarta. A cauda é conteúdo do discurso: na 0003 e no app ela vive só em memória, no mesmo lugar do texto (CLAUDE.md regra 2); fica registrado aqui para o PLAN da 0003.
- **Caminho do áudio.** Nenhum nesta tarefa: `align` não recebe áudio.
- **Dependências novas e permissões que trazem:** nenhuma. `java.text.Normalizer`, `java.util.Locale` e `kotlin.collections.ArrayDeque` são da plataforma (e existem no Android desde a API 1). O manifesto não muda; `verifyMergedManifest` continua na verificação.
- **Q6 aplicada:** texto e fala seguidos por todos os caminhos novos (tokens, cauda, `confirmed`, `score`, mensagens de exceção, saída de teste). Nenhum escapa da memória. Atenção para a 0003: `Token.toString()` e `AlignState.toString()` são de `data class`; o painel de depuração pode mostrá-los em memória, mas nada disso pode ir a `Log` no release.

---

## Tarefa 0: ligar os testes aos textos de `etapa0/textos/`

**Arquivos:**
- Modificar: `etapa0/core/build.gradle.kts` (depois do bloco `dependencies`, antes de `ktlint`)
- Criar: `<align-test>TestTexts.kt`

- [ ] **Passo 1: conferir o ponto de partida**

```bash
cd /Users/giorgioderico/Projetos/Tribuna && git status --short && git branch --show-current
cd etapa0 && ./gradlew :core:test
```

Esperado: branch `work/0002-etapa0-align`; `BUILD SUCCESSFUL` (os 25 testes da 0001).

- [ ] **Passo 2: passar a pasta dos textos ao `test`**

Em `etapa0/core/build.gradle.kts`, inserir entre o fecho do bloco `dependencies { ... }` e a linha `ktlint {`:

```kotlin
// Os testes do align leem os textos de teste de etapa0/textos/ (spec 0002; CLAUDE.md "Textos de teste").
val textosDeTeste = rootProject.layout.projectDirectory.dir("textos")

tasks.named<Test>("test") {
    inputs
        .dir(textosDeTeste)
        .withPropertyName("textosDeTeste")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("tribuna.textos", textosDeTeste.asFile.absolutePath)
}
```

- [ ] **Passo 3: criar o leitor dos textos**

`<align-test>TestTexts.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import java.io.File

/** Textos de teste de etapa0/textos/ (CLAUDE.md: só eles; nunca discurso pessoal). */
object TestTexts {
    val names = listOf("T01", "T02", "T03", "T04", "T05", "T06", "T07", "T08", "T09", "T10", "L30")

    private val dir: File by lazy {
        File(System.getProperty("tribuna.textos") ?: error("propriedade tribuna.textos ausente: rode pelo Gradle"))
    }

    fun read(name: String): String = File(dir, "$name.txt").readText(Charsets.UTF_8)
}
```

A função `tokens(name)` entra no Passo 3 da Tarefa 4, quando o `Tokenizer` existir.

- [ ] **Passo 4: rodar**

Run: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:test`
Esperado: `BUILD SUCCESSFUL` (nada novo roda ainda; a tarefa `test` passa a ter os textos como entrada).

- [ ] **Passo 5: commit**

```bash
cd /Users/giorgioderico/Projetos/Tribuna
git add etapa0/core/build.gradle.kts etapa0/core/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/align/TestTexts.kt
git commit -m "test(align): testes leem etapa0/textos/ (spec 0002, textos de teste)"
```

## Tarefa 1: `Normalizer`

**Arquivos:**
- Teste: `<align-test>NormalizerTest.kt`
- Criar: `<align-main>Normalizer.kt`

- [ ] **Passo 1: escrever o teste**

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizerTest {
    @Test
    fun normalize_minusculasSemAcentosSemPontuacao_palavrasDoT01() {
        assertEquals("as", Normalizer.normalize("Às"))
        assertEquals("e", Normalizer.normalize("É"))
        assertEquals("conversa", Normalizer.normalize("conversa."))
        assertEquals("comeca", Normalizer.normalize("começa"))
        assertEquals("dificil", Normalizer.normalize("difícil,"))
        assertEquals("ouvir bem e uma habilidade", Normalizer.normalize("Ouvir bem é uma habilidade"))
    }

    @Test
    fun normalize_palavrasDoT09ESemPontuacaoSolta() {
        assertEquals("e entao", Normalizer.normalize("E então?"))
        assertEquals("so esperei", Normalizer.normalize("Só esperei."))
        assertEquals("nao e facil", Normalizer.normalize("Não é fácil,"))
        assertEquals("", Normalizer.normalize(" — … "))
    }

    @Test
    fun normalize_roteiroEFalaDoMesmoJeito() {
        assertEquals(Normalizer.normalize("três"), Normalizer.normalize("Três"))
        assertEquals("tres", Normalizer.normalize("TRÊS"))
        assertEquals("escolha", Normalizer.normalize("ESCOLHA"))
    }

    @Test
    fun normalize_hifenEApostrofoUnemAsPartes_D18() {
        assertEquals("bomdia", Normalizer.normalize("bom-dia"))
        assertEquals("escrevelos", Normalizer.normalize("escrevê-los"))
        assertEquals("dagua", Normalizer.normalize("d'água"))
    }

    @Test
    fun normalize_eIdempotenteEmTodosOsTextos() {
        for (name in TestTexts.names) {
            val once = Normalizer.normalize(TestTexts.read(name))
            assertEquals(name, once, Normalizer.normalize(once))
        }
    }

    @Test
    fun words_separaPorEspacoEDescartaVazios() {
        assertEquals(listOf("isso", "tem", "um", "custo"), Normalizer.words("  Isso tem um custo.  "))
        assertEquals(emptyList<String>(), Normalizer.words("—"))
    }
}
```

- [ ] **Passo 2: ver falhar**

Run: `cd /Users/giorgioderico/Projetos/Tribuna/etapa0 && ./gradlew :core:test --tests '*NormalizerTest'`
Esperado: FAIL na compilação, `Unresolved reference 'Normalizer'`.

- [ ] **Passo 3: implementar**

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import java.util.Locale
import java.text.Normalizer as JdkNormalizer

/**
 * Minúsculas, sem acentos e sem pontuação, igual para roteiro e fala (TRD Motor de voz §Normalização e tokens).
 * Hífen e apóstrofo entre letras unem as partes ("bom-dia" → "bomdia", "d'água" → "dagua"; D18).
 * Idempotente: normalize(normalize(x)) == normalize(x).
 */
object Normalizer {
    private val combiningMarks = Regex("\\p{Mn}+")
    private val joiners = Regex("(?<=[\\p{L}\\p{N}])[-'’](?=[\\p{L}\\p{N}])")
    private val nonWord = Regex("[^\\p{L}\\p{N}\\s]+")
    private val spaces = Regex("\\s+")

    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val withoutAccents = JdkNormalizer.normalize(lower, JdkNormalizer.Form.NFD).replace(combiningMarks, "")
        return withoutAccents
            .replace(joiners, "")
            .replace(nonWord, " ")
            .replace(spaces, " ")
            .trim()
    }

    fun words(text: String): List<String> = normalize(text).split(' ').filter { it.isNotEmpty() }
}
```

- [ ] **Passo 4: ver passar**

Run: `./gradlew :core:test --tests '*NormalizerTest'`
Esperado: PASS (6 testes).

- [ ] **Passo 5: commit**

```bash
cd /Users/giorgioderico/Projetos/Tribuna
git add etapa0/core/src
git commit -m "feat(align): Normalizer (TRD Motor de voz §Normalização e tokens)"
```

## Tarefa 2: `Similarity`

**Arquivos:**
- Teste: `<align-test>SimilarityTest.kt`
- Criar: `<align-main>Similarity.kt`

- [ ] **Passo 1: escrever o teste**

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarityTest {
    @Test
    fun levenshtein_casosBasicos() {
        assertEquals(0, Similarity.levenshtein("escolha", "escolha"))
        assertEquals(1, Similarity.levenshtein("pratica", "praticas"))
        assertEquals(5, Similarity.levenshtein("", "ouvir"))
        assertEquals(3, Similarity.levenshtein("custo", "susto2x"))
    }

    @Test
    fun of_eUmMenosLevenshteinSobreOMaiorComprimento() {
        assertEquals(1.0, Similarity.of("escolha", "escolha"), 1e-12)
        assertEquals(0.8, Similarity.of("ouvir", "ouvi"), 1e-12)
        assertEquals(0.0, Similarity.of("de", "um"), 1e-12)
    }

    @Test
    fun of_unkNuncaCasa_D13() {
        assertEquals(0.0, Similarity.of(Similarity.UNKNOWN, "unk"), 0.0)
        assertEquals(0.0, Similarity.of("unk", Similarity.UNKNOWN), 0.0)
        assertEquals(0.0, Similarity.of(Similarity.UNKNOWN, Similarity.UNKNOWN), 0.0)
    }
}
```

- [ ] **Passo 2: ver falhar**

Run: `./gradlew :core:test --tests '*SimilarityTest'`
Esperado: FAIL na compilação, `Unresolved reference 'Similarity'`.

- [ ] **Passo 3: implementar**

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

/** Similaridade de palavras: 1 − Levenshtein / maior comprimento (TRD §Stack "Levenshtein próprio"; §Alinhamento 2). */
internal object Similarity {
    /** Marca do vocabulário restrito do Vosk: nunca casa com palavra do roteiro (D13). */
    const val UNKNOWN = "[unk]"

    fun levenshtein(
        a: String,
        b: String,
    ): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    fun of(
        a: String,
        b: String,
    ): Double {
        if (a == UNKNOWN || b == UNKNOWN) return 0.0
        val longest = maxOf(a.length, b.length)
        if (longest == 0) return 1.0
        return 1.0 - levenshtein(a, b).toDouble() / longest
    }
}
```

- [ ] **Passo 4: ver passar**

Run: `./gradlew :core:test --tests '*SimilarityTest'`
Esperado: PASS (3 testes).

- [ ] **Passo 5: commit**

```bash
cd /Users/giorgioderico/Projetos/Tribuna
git add etapa0/core/src
git commit -m "feat(align): similaridade por Levenshtein, [unk] nunca casa (TRD §Alinhamento 2; D13)"
```

## Tarefa 3: `NumberWords`

**Arquivos:**
- Teste: `<align-test>NumberWordsTest.kt`
- Criar: `<align-main>NumberWords.kt`

- [ ] **Passo 1: escrever o teste**

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberWordsTest {
    private fun w(text: String) = text.split(' ')

    @Test
    fun masculine_formaUsual() {
        assertEquals(w("zero"), NumberWords.masculine(0))
        assertEquals(w("vinte e quatro"), NumberWords.masculine(24))
        assertEquals(w("cem"), NumberWords.masculine(100))
        assertEquals(w("cento e um"), NumberWords.masculine(101))
        assertEquals(w("trezentos e quarenta"), NumberWords.masculine(340))
        assertEquals(w("mil"), NumberWords.masculine(1000))
        assertEquals(w("mil e cinco"), NumberWords.masculine(1005))
        assertEquals(w("mil e novecentos"), NumberWords.masculine(1900))
        assertEquals(w("mil novecentos e catorze"), NumberWords.masculine(1914))
        assertEquals(w("tres mil e seiscentos"), NumberWords.masculine(3600))
        assertEquals(
            w("novecentos e noventa e nove mil novecentos e noventa e nove"),
            NumberWords.masculine(999_999),
        )
    }

    @Test
    fun cardinalForms_grafiasAlternativasEFeminino_D15() {
        val f1914 = NumberWords.cardinalForms(1914)
        assertEquals(w("mil novecentos e catorze"), f1914.first())
        assertTrue(w("mil novecentos e quatorze") in f1914)
        assertEquals(listOf(w("trezentos e quarenta"), w("trezentas e quarenta")), NumberWords.cardinalForms(340))
        assertEquals(listOf(w("um"), w("uma")), NumberWords.cardinalForms(1))
        assertEquals(listOf(w("vinte e quatro")), NumberWords.cardinalForms(24))
    }

    @Test
    fun masculineForms_semFeminino() {
        assertEquals(listOf(w("um")), NumberWords.masculineForms(1))
        assertEquals(listOf(w("catorze"), w("quatorze")), NumberWords.masculineForms(14))
    }

    @Test(expected = IllegalArgumentException::class)
    fun masculine_foraDaFaixaDeD14_falha() {
        NumberWords.masculine(1_000_000)
    }
}
```

- [ ] **Passo 2: ver falhar**

Run: `./gradlew :core:test --tests '*NumberWordsTest'`
Esperado: FAIL na compilação, `Unresolved reference 'NumberWords'`.

- [ ] **Passo 3: implementar**

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

/** Números por extenso em português, de 0 a 999.999 (TRD §Normalização e tokens; spec 0002, D14 e D15). */
internal object NumberWords {
    const val MAX = 999_999

    private val units =
        listOf(
            "zero",
            "um",
            "dois",
            "tres",
            "quatro",
            "cinco",
            "seis",
            "sete",
            "oito",
            "nove",
            "dez",
            "onze",
            "doze",
            "treze",
            "catorze",
            "quinze",
            "dezesseis",
            "dezessete",
            "dezoito",
            "dezenove",
        )
    private val tens = listOf("", "", "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa")
    private val hundreds =
        listOf(
            "",
            "cento",
            "duzentos",
            "trezentos",
            "quatrocentos",
            "quinhentos",
            "seiscentos",
            "setecentos",
            "oitocentos",
            "novecentos",
        )

    /** Forma masculina usual, sem alternativas: 1914 → [mil, novecentos, e, catorze]. */
    fun masculine(n: Int): List<String> {
        require(n in 0..MAX) { "número fora da faixa de D14" } // sem o valor: nada do roteiro em mensagem (Q6)
        if (n < 1000) return below1000(n)
        val thousands = n / 1000
        val rest = n % 1000
        val head = if (thousands == 1) listOf("mil") else below1000(thousands) + "mil"
        return when {
            rest == 0 -> head
            rest < 100 || rest % 100 == 0 -> head + "e" + below1000(rest)
            else -> head + below1000(rest)
        }
    }

    /** Grafias alternativas viram formas alternativas: catorze/quatorze (TRD §Normalização). */
    fun spellingVariants(form: List<String>): List<List<String>> =
        if ("catorze" in form) listOf(form, form.map { if (it == "catorze") "quatorze" else it }) else listOf(form)

    /** Forma feminina (D15): um → uma, dois → duas, -entos → -entas. */
    fun feminine(form: List<String>): List<String> =
        form.map {
            when {
                it == "um" -> "uma"
                it == "dois" -> "duas"
                it.endsWith("entos") -> it.removeSuffix("entos") + "entas"
                else -> it
            }
        }

    /** Número solto: masculina, feminina quando difere, cada uma com as grafias alternativas (D14, D15). */
    fun cardinalForms(n: Int): List<List<String>> {
        val masculine = masculine(n)
        val feminine = feminine(masculine)
        val bases = if (feminine == masculine) listOf(masculine) else listOf(masculine, feminine)
        return bases.flatMap(::spellingVariants).distinct()
    }

    /** Referências, decimais e porcentagens: só masculina, com as grafias alternativas. */
    fun masculineForms(n: Int): List<List<String>> = spellingVariants(masculine(n))

    private fun below1000(n: Int): List<String> =
        when {
            n < 20 -> listOf(units[n])
            n < 100 -> listOf(tens[n / 10]) + if (n % 10 == 0) emptyList() else listOf("e", units[n % 10])
            n == 100 -> listOf("cem")
            else -> listOf(hundreds[n / 100]) + if (n % 100 == 0) emptyList() else listOf("e") + below1000(n % 100)
        }
}
```

- [ ] **Passo 4: ver passar**

Run: `./gradlew :core:test --tests '*NumberWordsTest'`
Esperado: PASS (4 testes).

- [ ] **Passo 5: commit**

```bash
cd /Users/giorgioderico/Projetos/Tribuna
git add etapa0/core/src
git commit -m "feat(align): números por extenso até 999.999 com feminino e catorze/quatorze (TRD §Normalização; D14, D15)"
```

## Tarefa 4: `Tokenizer`, `AlignConstants` e `AlignPresets`

**Arquivos:**
- Testes: `<align-test>TokenizerTest.kt`, `<align-test>TokenizerTextsTest.kt`
- Modificar: `<align-test>TestTexts.kt` (acrescentar `tokens`)
- Criar: `<align-main>AlignConstants.kt`, `<align-main>AlignPresets.kt`, `<align-main>Tokenizer.kt`

Regras que o código implementa (D14–D20), para o revisor conferir:

- Linha em branco ou cuja primeira coisa depois de espaços é `//`: não gera token e marca o próximo token como início de frase.
- Palavra crua = sequência sem espaço. O trecho exibido vai do primeiro caractere letra/dígito ao último letra/dígito (ou `%`); pontuação solta não gera token.
- Na ordem: livro com número (`1`/`2`/`3` sozinho + palavra com maiúscula + referência `N:M` logo depois) → referência (`N:M`, `N:M-M`, `N:M–M`, `N:M a M`, lista `N:M, M, M` enquanto o item depois da vírgula for número) → número (`1914`, `3.600`, `3,5`, `3,5%`) → palavra (hifenizada = duas formas).
- Início de frase: primeiro token, token depois de palavra crua que termina em `.`, `!`, `?` ou `…` (ignorando aspas e parênteses de fechamento), e token depois de linha em branco ou nota.

- [ ] **Passo 1: escrever os testes**

`<align-test>TokenizerTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Formas faladas (TRD Contratos §Token e §Normalização e tokens; spec 0002, D14–D18). */
class TokenizerTest {
    private fun w(text: String) = text.split(' ')

    private fun display(
        text: String,
        token: Token,
    ) = text.substring(token.display.first, token.display.last + 1)

    /** Token cujo trecho exibido é exatamente `literal` (a primeira ocorrência). */
    private fun tokenOf(
        text: String,
        literal: String,
    ): Token = Tokenizer.tokenize(text).first { display(text, it) == literal }

    @Test
    fun exemplosDoTrd_24() {
        // T02: "Mais de 24 países"
        assertEquals(listOf(w("vinte e quatro")), tokenOf("Mais de 24 países", "24").spoken)
    }

    @Test
    fun exemplosDoTrd_3_16_umTokenSeparadoDoLivro() {
        val text = "O primeiro está em João 3:16."
        val tokens = Tokenizer.tokenize(text)
        assertEquals(listOf("O", "primeiro", "está", "em", "João", "3:16"), tokens.map { display(text, it) })
        val ref = tokens.last().spoken
        assertTrue(w("tres dezesseis") in ref)
        assertTrue(w("tres e dezesseis") in ref)
        assertTrue(w("capitulo tres versiculo dezesseis") in ref)
    }

    @Test
    fun exemplosDoTrd_13_4a7() {
        val ref = tokenOf("Também vale ler 1 Coríntios 13:4-7, que fala de amor", "13:4-7").spoken
        assertTrue(w("treze quatro a sete") in ref)
        assertTrue(w("treze de quatro a sete") in ref)
        assertTrue(w("capitulo treze versiculos quatro a sete") in ref)
    }

    @Test
    fun exemplosDoTrd_12_1e2() {
        // Variação do "Romanos 12:2" do T02, escrita no teste (spec, critério de intervalos e listas).
        val text = "E o último, em Romanos 12:1, 2. Também"
        val ref = tokenOf(text, "12:1, 2").spoken
        assertTrue(w("doze um e dois") in ref)
        assertTrue(w("doze versiculos um e dois") in ref)
        assertTrue(w("capitulo doze versiculos um e dois") in ref)
        assertEquals("Romanos", display(text, Tokenizer.tokenize(text)[4]))
    }

    @Test
    fun numerosDoT02() {
        val t02 = TestTexts.read("T02")
        val f1914 = tokenOf(t02, "1914").spoken
        assertTrue(w("mil novecentos e catorze") in f1914)
        assertTrue(w("mil novecentos e quatorze") in f1914)
        assertTrue(w("tres virgula cinco por cento") in tokenOf(t02, "3,5%").spoken)
        val f2414 = tokenOf(t02, "24:14").spoken
        assertTrue(w("vinte e quatro catorze") in f2414)
        assertTrue(w("vinte e quatro quatorze") in f2414)
    }

    @Test
    fun livroComNumero_1Corintios_D17() {
        val t02 = TestTexts.read("T02")
        val tokens = Tokenizer.tokenize(t02)
        val book = tokens.first { display(t02, it) == "1 Coríntios" }
        assertTrue(w("primeira corintios") in book.spoken)
        assertTrue(w("primeiro corintios") in book.spoken)
        assertTrue(w("um corintios") in book.spoken)
        assertEquals("13:4-7", display(t02, tokens[book.index + 1]))
    }

    @Test
    fun com1Capitulo_numeroProprioComMasculinoEFeminino() {
        val t02 = TestTexts.read("T02")
        val tokens = Tokenizer.tokenize(t02)
        val com = tokens.indices.first { display(t02, tokens[it]) == "Com" }
        val one = tokens[com + 1]
        assertEquals("1", display(t02, one))
        assertEquals(listOf(w("um"), w("uma")), one.spoken)
        assertEquals("capítulo", display(t02, tokens[com + 2]))
    }

    @Test
    fun numerosDoL30() {
        val l30 = TestTexts.read("L30")
        val f340 = tokenOf(l30, "340").spoken
        assertTrue(w("trezentos e quarenta") in f340)
        assertTrue(w("trezentas e quarenta") in f340)
        val tokens = Tokenizer.tokenize(l30)
        val list = tokens.first { display(l30, it) == "5:3, 4" }
        assertTrue(w("cinco tres e quatro") in list.spoken)
        assertEquals(listOf(listOf("o")), tokens[list.index + 1].spoken)
    }

    @Test
    fun intervaloComTravessaoEComA_igualAoHifen() {
        // Variações do "13:4-7" do T02, escritas no teste.
        val hyphen = tokenOf("ler 1 Coríntios 13:4-7, que", "13:4-7").spoken
        assertEquals(hyphen, tokenOf("ler 1 Coríntios 13:4–7, que", "13:4–7").spoken)
        assertEquals(hyphen, tokenOf("ler 1 Coríntios 13:4 a 7, que", "13:4 a 7").spoken)
    }

    @Test
    fun listaDeTresItens() {
        val ref = tokenOf("E o último, em Romanos 12:1, 2, 5. Também", "12:1, 2, 5").spoken
        assertTrue(w("doze um dois e cinco") in ref)
    }

    @Test
    fun palavraHifenizada_umTokenComDuasFormas_D18() {
        val t03 = TestTexts.read("T03")
        assertEquals(listOf(w("bom dia"), w("bomdia")), tokenOf(t03, "bom-dia").spoken)
        val l30 = TestTexts.read("L30")
        assertEquals(listOf(w("escreve los"), w("escrevelos")), tokenOf(l30, "escrevê-los").spoken)
    }

    @Test
    fun pesoDasPalavrasComuns_T09() {
        val tokens = TestTexts.tokens("T09")
        var common = 0
        for (token in tokens) {
            val single = token.spoken.singleOrNull()?.singleOrNull()
            if (single != null && single in AlignConstants.COMMON_WORDS) {
                common++
                assertEquals(AlignConstants.COMMON_WORD_WEIGHT, token.weight, 0f)
                assertTrue(token.weight < 1f)
            } else {
                assertEquals(1f, token.weight, 0f)
            }
        }
        assertTrue("T09 tem palavras comuns (Q3)", common >= 10)
        val t09 = TestTexts.read("T09")
        val e = tokens.first { display(t09, it) == "É" }
        assertEquals(AlignConstants.COMMON_WORD_WEIGHT, e.weight, 0f)
    }

    @Test
    fun inicioDeFrase_T09temQuinzeFrases_D20() {
        val t09 = TestTexts.read("T09")
        val starts = TestTexts.tokens("T09").filter { it.sentenceStart }.map { display(t09, it) }
        assertEquals(
            listOf("Sim", "E", "É", "De", "Que", "Foi", "E", "Nada", "Só", "E", "Deu", "Que", "Eu", "Não", "E"),
            starts,
        )
    }

    @Test
    fun inicioDeFrase_T01primeiroTokenDeCadaParagrafo() {
        val t01 = TestTexts.read("T01")
        val tokens = TestTexts.tokens("T01")
        val paragraphStarts = Regex("(?m)^(?=\\S)").findAll(t01).map { it.range.first }.toList()
        assertEquals(5, paragraphStarts.size)
        for (offset in paragraphStarts) {
            assertTrue("parágrafo em $offset", tokens.first { it.display.first == offset }.sentenceStart)
        }
        assertFalse(tokens[1].sentenceStart)
    }
}
```

`<align-test>TokenizerTextsTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Propriedades do Tokenizer em todos os textos de etapa0/textos/ (spec 0002, critério de tokens). */
class TokenizerTextsTest {
    private val numeric = Regex("^[\\d.,:%\\-– a]+$")
    private val book = Regex("^[123] \\p{Lu}\\p{L}*$")

    @Test
    fun indexIgualAPosicaoEDisplayBateComUmaForma_todosOsTextos() {
        for (name in TestTexts.names) {
            val text = TestTexts.read(name)
            val tokens = Tokenizer.tokenize(text)
            assertTrue("$name sem tokens (Q3)", tokens.size > 50)
            tokens.forEachIndexed { i, token ->
                assertEquals("$name índice", i, token.index)
                val shown = text.substring(token.display.first, token.display.last + 1)
                assertTrue("$name '$shown' começa sem pontuação", shown.first().isLetterOrDigit())
                assertTrue("$name '$shown' termina sem pontuação", shown.last().isLetterOrDigit() || shown.last() == '%')
                if (shown.any { it.isDigit() }) {
                    assertTrue("$name '$shown' é número ou referência", numeric.matches(shown) || book.matches(shown))
                    assertTrue("$name '$shown' vira palavras", token.spoken.all { form -> form.none { w -> w.any { it.isDigit() } } })
                } else {
                    assertTrue("$name '$shown' bate com ${token.spoken}", Normalizer.words(shown) in token.spoken)
                }
            }
        }
    }

    @Test
    fun linhasDeNotaNaoGeramToken_T04eL30() {
        for (name in listOf("T04", "L30")) {
            val text = TestTexts.read(name)
            val noteRanges =
                Regex("(?m)^[ \\t]*//.*$").findAll(text).map { it.range }.toList()
            assertTrue("$name tem notas (Q3)", noteRanges.isNotEmpty())
            for (token in Tokenizer.tokenize(text)) {
                for (note in noteRanges) {
                    assertTrue(
                        "$name token em ${token.display} dentro da nota $note",
                        token.display.last < note.first || token.display.first > note.last,
                    )
                }
            }
        }
    }
}
```

- [ ] **Passo 2: ver falhar**

Run: `./gradlew :core:test --tests '*Tokenizer*'`
Esperado: FAIL na compilação, `Unresolved reference 'Tokenizer'` e `'AlignConstants'`.

- [ ] **Passo 3: implementar**

Acrescentar em `<align-test>TestTexts.kt`, depois de `fun read(...)` e antes do `}` final, com uma linha em branco antes:

```kotlin
    fun tokens(name: String): List<Token> = Tokenizer.tokenize(read(name))
```

`<align-main>AlignConstants.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

/**
 * Constantes fixas do alinhamento (PRD, Algoritmo de alinhamento; TRD Motor de voz §Alinhamento).
 * Não variam por sensibilidade (spec 0002, D12). O peso das palavras comuns é PROVISÓRIO (D10).
 */
object AlignConstants {
    /** Avanço curto: distância até 5 tokens do cursor (TRD §Alinhamento 3). */
    const val SHORT_DISTANCE_TOKENS = 5

    /** Nenhum movimento automático abaixo de highWater − 3 (TRD §Alinhamento 2 e 5; CLAUDE.md regra 6). */
    const val MAX_BACK_FROM_HIGH_WATER = 3

    /** A recuperação dispara quando o missStreak chega a 2 (TRD §Alinhamento 4; D7). */
    const val MISSES_FOR_RECOVERY = 2

    /** Palavras comparadas na recuperação (TRD §Alinhamento 4: "4 ou mais"; D7). */
    const val RECOVERY_WORDS = 4

    /** Palavras comuns do PRD, na forma normalizada; "é" também conta, porque normaliza para "e" (D10). */
    val COMMON_WORDS: Set<String> = setOf("de", "que", "e", "o", "a")

    /** PROVISÓRIO: decisão do analista para a Etapa 0 (D10), a calibrar na 0005 com o T09 e o L30. */
    const val COMMON_WORD_WEIGHT = 0.5f
}
```

`<align-main>AlignPresets.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

/**
 * Presets de sensibilidade (PRD R4 e tabela do Algoritmo de alinhamento; spec 0002, D11).
 * Valores do PRD; a calibração final é da 0005. O contrato AlignParams continua sem valor padrão.
 */
object AlignPresets {
    val conservador = AlignParams(k = 4, window = 25, shortThreshold = 0.70, longThreshold = 0.90)
    val normal = AlignParams(k = 3, window = 40, shortThreshold = 0.60, longThreshold = 0.85)
    val agil = AlignParams(k = 2, window = 60, shortThreshold = 0.55, longThreshold = 0.80)
}
```

`<align-main>Tokenizer.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

/**
 * Roteiro → tokens (TRD Motor de voz §Normalização e tokens; Contratos §Token).
 * Recorte e formas: spec 0002, D14–D20. As formas faladas saem já normalizadas.
 */
object Tokenizer {
    private val rawWord = Regex("\\S+")
    private val reference = Regex("^(\\d+):(\\d+)(?:[-–](\\d+))?$")
    private val referenceStart = Regex("^\\d+:\\d+")
    private val integer = Regex("^\\d+$")
    private val number = Regex("^(\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,(\\d+))?(%)?$")
    private val bookNumber = Regex("^[123]$")
    private val intervalWords = setOf("a", "-", "–")
    private const val CLOSING = "\"'”’)»]"
    private const val SENTENCE_END = ".!?…"
    private val ordinals =
        mapOf(
            1 to listOf("primeira", "primeiro", "um"),
            2 to listOf("segunda", "segundo", "dois"),
            3 to listOf("terceira", "terceiro", "tres"),
        )

    fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var pendingStart = true
        var lineStart = 0
        for (line in text.split('\n')) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                pendingStart = true // D20: linha em branco ou nota abre frase; nota não gera token (T5)
            } else {
                val raws = rawWord.findAll(line).map { Raw(it.value, lineStart + it.range.first) }.toList()
                var i = 0
                while (i < raws.size) {
                    val built = build(raws, i)
                    if (built == null) {
                        if (endsSentence(raws[i].text)) pendingStart = true
                        i++
                        continue
                    }
                    tokens +=
                        Token(
                            index = tokens.size,
                            display = built.display,
                            spoken = built.spoken,
                            weight = built.weight,
                            sentenceStart = pendingStart,
                        )
                    pendingStart = endsSentence(raws[built.lastRaw].text)
                    i = built.lastRaw + 1
                }
            }
            lineStart += line.length + 1
        }
        return tokens
    }

    private class Raw(
        val text: String,
        val start: Int,
    ) {
        val coreStart: Int = text.indexOfFirst { it.isLetterOrDigit() }.coerceAtLeast(0)
        val coreEnd: Int = text.indexOfLast { it.isLetterOrDigit() || it == '%' } + 1
        val hasCore: Boolean get() = text.any { it.isLetterOrDigit() }
        val core: String get() = if (hasCore) text.substring(coreStart, coreEnd) else ""
        val absCoreStart: Int get() = start + coreStart
        val absCoreLast: Int get() = start + coreEnd - 1
    }

    private class Built(
        val display: IntRange,
        val spoken: List<List<String>>,
        val weight: Float,
        val lastRaw: Int,
    )

    private fun build(
        raws: List<Raw>,
        i: Int,
    ): Built? {
        if (!raws[i].hasCore) return null // pontuação solta não gera token (D19)
        return book(raws, i) ?: referenceToken(raws, i) ?: numberToken(raws[i], i) ?: word(raws[i], i)
    }

    /** D17: "1 Coríntios 13:4-7" → o livro com número é um token só. */
    private fun book(
        raws: List<Raw>,
        i: Int,
    ): Built? {
        val raw = raws[i]
        if (!bookNumber.matches(raw.text)) return null
        val name = raws.getOrNull(i + 1) ?: return null
        val ref = raws.getOrNull(i + 2) ?: return null
        if (!name.hasCore || !name.core.first().isUpperCase() || !referenceStart.containsMatchIn(ref.core)) return null
        val nameWords = Normalizer.words(name.core)
        val spoken = ordinals.getValue(raw.text.toInt()).map { listOf(it) + nameWords }
        return Built(raw.absCoreStart..name.absCoreLast, spoken, 1f, i + 1)
    }

    /** D16: referência simples, intervalo (-, – ou "a") ou lista, sempre um token só com o capítulo. */
    private fun referenceToken(
        raws: List<Raw>,
        i: Int,
    ): Built? {
        val raw = raws[i]
        val match = reference.matchEntire(raw.core) ?: return null
        val chapter = match.groupValues[1].toIntOrNull() ?: return null
        val verses = mutableListOf(match.groupValues[2].toIntOrNull() ?: return null)
        var last = i
        var interval = false
        val next = raws.getOrNull(i + 1)
        val afterNext = raws.getOrNull(i + 2)
        if (match.groupValues[3].isNotEmpty()) {
            verses += match.groupValues[3].toIntOrNull() ?: return null
            interval = true
        } else if (next != null && next.text in intervalWords && afterNext != null && integer.matches(afterNext.core)) {
            verses += afterNext.core.toIntOrNull() ?: return null
            interval = true
            last = i + 2
        } else {
            while (raws[last].text.endsWith(",")) {
                val item = raws.getOrNull(last + 1) ?: break
                if (!integer.matches(item.core)) break
                verses += item.core.toIntOrNull() ?: return null
                last++
            }
        }
        if (chapter > NumberWords.MAX || verses.any { it > NumberWords.MAX }) return null
        return Built(raw.absCoreStart..raws[last].absCoreLast, referenceForms(chapter, verses, interval), 1f, last)
    }

    private fun referenceForms(
        chapter: Int,
        verses: List<Int>,
        interval: Boolean,
    ): List<List<String>> {
        val options = listOf(NumberWords.masculineForms(chapter)) + verses.map { NumberWords.masculineForms(it) }
        val combinations =
            options.fold(listOf(emptyList<List<String>>())) { acc, alternatives ->
                acc.flatMap { prefix -> alternatives.map { prefix + listOf(it) } }
            }
        return combinations
            .flatMap { combination ->
                val c = combination.first()
                val v = combination.drop(1)
                when {
                    interval -> {
                        listOf(
                            c + v[0] + "a" + v[1],
                            c + "de" + v[0] + "a" + v[1],
                            listOf("capitulo") + c + "versiculos" + v[0] + "a" + v[1],
                        )
                    }

                    v.size == 1 -> {
                        listOf(
                            c + v[0],
                            c + "e" + v[0],
                            listOf("capitulo") + c + "versiculo" + v[0],
                        )
                    }

                    else -> {
                        val list = v.dropLast(1).flatten() + "e" + v.last()
                        listOf(
                            c + list,
                            c + "versiculos" + list,
                            listOf("capitulo") + c + "versiculos" + list,
                        )
                    }
                }
            }.distinct()
    }

    /** D14: inteiros até 999.999 (com ou sem ponto de milhar), decimais com vírgula e porcentagem. */
    private fun numberToken(
        raw: Raw,
        i: Int,
    ): Built? {
        val match = number.matchEntire(raw.core) ?: return null
        val value =
            match.groupValues[1]
                .replace(".", "")
                .toIntOrNull()
                ?.takeIf { it <= NumberWords.MAX } ?: return null
        val decimals = match.groupValues[2]
        val percent = match.groupValues[3].isNotEmpty()
        val spoken =
            if (decimals.isEmpty() && !percent) {
                NumberWords.cardinalForms(value)
            } else {
                val whole = NumberWords.masculineForms(value)
                val withDecimals =
                    if (decimals.isEmpty()) {
                        whole
                    } else {
                        val fraction = decimalForms(decimals)
                        whole.flatMap { w -> fraction.map { f -> w + "virgula" + f } }
                    }
                if (percent) withDecimals.map { it + listOf("por", "cento") } else withDecimals
            }
        return Built(raw.absCoreStart..raw.absCoreLast, spoken, 1f, i)
    }

    private fun decimalForms(digits: String): List<List<String>> {
        val zeros = List(digits.takeWhile { it == '0' }.length) { "zero" }
        val rest = digits.drop(zeros.size)
        if (rest.isEmpty()) return listOf(zeros)
        val value = rest.toIntOrNull()?.takeIf { it <= NumberWords.MAX } ?: return listOf(digits.map { it.toString() })
        return NumberWords.masculineForms(value).map { zeros + it }
    }

    /** Palavra comum ou hifenizada (D18, D19); peso menor para as palavras comuns (D10). */
    private fun word(
        raw: Raw,
        i: Int,
    ): Built? {
        val core = raw.core.trimEnd('%')
        val joined = Normalizer.words(core)
        if (joined.isEmpty()) return null
        val parts = core.split('-').flatMap { Normalizer.words(it) }
        val spoken = if (core.contains('-') && parts.size > 1 && parts != joined) listOf(parts, joined) else listOf(joined)
        val common = spoken.size == 1 && joined.size == 1 && joined[0] in AlignConstants.COMMON_WORDS
        val weight = if (common) AlignConstants.COMMON_WORD_WEIGHT else 1f
        return Built(raw.absCoreStart..(raw.absCoreStart + core.length - 1), spoken, weight, i)
    }

    /** D20: termina frase o texto cru que acaba em . ! ? ou …, ignorando aspas e parênteses de fechamento. */
    private fun endsSentence(raw: String): Boolean {
        val trimmed = raw.trimEnd { it in CLOSING }
        return trimmed.isNotEmpty() && trimmed.last() in SENTENCE_END
    }
}
```

- [ ] **Passo 4: ver passar**

Run: `./gradlew :core:test --tests '*Tokenizer*' && ./gradlew :core:ktlintCheck`
Esperado: PASS (16 testes) e ktlint verde.

- [ ] **Passo 5: commit**

```bash
cd /Users/giorgioderico/Projetos/Tribuna
git add etapa0/core/src
git commit -m "feat(align): Tokenizer com formas faladas, notas //, peso e início de frase (TRD Contratos §Token; §Normalização; T5)"
```

## Tarefa 5: `PartialReconciler`

**Arquivos:**
- Teste: `<align-test>PartialReconcilerTest.kt`
- Criar: `<align-main>PartialReconciler.kt`

- [ ] **Passo 1: escrever o teste**

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Test

/** Suíte "Reconciliação de hipóteses" (TRD Testes; Motor de voz §Reconciliação de hipóteses). Frases de etapa0/textos/. */
class PartialReconcilerTest {
    private val r = PartialReconciler()

    private fun w(text: String) = if (text.isEmpty()) emptyList() else text.split(' ')

    private fun p(text: String) = r.onPartial(w(text))

    private fun f(text: String) = r.onFinal(w(text))

    @Test
    fun parciaisQueSoCrescem_entregamSoOPrefixoEstavelETotalIgualAoFinal() {
        val out = mutableListOf<String>()
        out += p("ouvir").also { assertEquals(emptyList<String>(), it) }
        out += p("ouvir bem").also { assertEquals(w("ouvir"), it) }
        out += p("ouvir bem é uma").also { assertEquals(w("bem"), it) }
        out += f("ouvir bem é uma habilidade").also { assertEquals(w("é uma habilidade"), it) }
        assertEquals(w("ouvir bem é uma habilidade"), out)
    }

    @Test
    fun correcaoNoComecoDaFrase_naoRepeteNemRetira() {
        val out = mutableListOf<String>()
        out += p("a")
        out += p("a maioria")
        out += p("a maioria de")
        out += p("a maioria de nós")
        assertEquals(w("a maioria de"), out)
        out += p("a maior parte de nós").also { assertEquals(emptyList<String>(), it) }
        out += p("a maior parte de nós escuta").also { assertEquals(w("nós"), it) }
        out += f("a maior parte de nós escuta apenas").also { assertEquals(w("escuta apenas"), it) }
        assertEquals(w("a maioria de nós escuta apenas"), out)
    }

    @Test
    fun insercaoNoComecoDaFrase_entregaSoODepoisDoPontoAlinhado() {
        p("ouvir bem")
        assertEquals(w("ouvir bem"), p("ouvir bem é"))
        assertEquals(w("é"), p("ouvir bem é uma"))
        assertEquals(emptyList<String>(), p("então ouvir bem é uma"))
        assertEquals(w("uma"), p("então ouvir bem é uma habilidade"))
    }

    @Test
    fun remocaoNoComecoDaFrase_entregaSoODepoisDoPontoAlinhado() {
        p("a maioria de")
        assertEquals(w("a maioria de"), p("a maioria de nós"))
        assertEquals(emptyList<String>(), p("maioria de nós escuta"))
        assertEquals(w("nós escuta"), p("maioria de nós escuta apenas"))
    }

    @Test
    fun finalComMaisPalavras_entregaOQueFaltaEZera() {
        p("ouvir bem")
        assertEquals(w("ouvir bem"), p("ouvir bem é"))
        assertEquals(w("é uma habilidade"), f("ouvir bem é uma habilidade"))
        assertEquals(emptyList<String>(), p("que"))
        assertEquals(w("que"), p("que quase"))
    }

    @Test
    fun finalComMenosPalavras_naoRetiraNada() {
        p("ouvir bem é uma")
        assertEquals(w("ouvir bem é uma"), p("ouvir bem é uma habilidade"))
        assertEquals(emptyList<String>(), f("ouvir bem é"))
        assertEquals(emptyList<String>(), p("que quase"))
    }

    @Test
    fun finalComPalavraTrocada_entregaSoODepois() {
        p("ouvir vem é")
        assertEquals(w("ouvir vem é"), p("ouvir vem é uma"))
        assertEquals(w("uma habilidade"), f("ouvir bem é uma habilidade"))
    }

    @Test
    fun fraseRepetida_mesmoFinalDuasVezesEntregaNasDuas_T08() {
        assertEquals(w("isso tem um custo"), f("isso tem um custo"))
        assertEquals(w("isso tem um custo"), f("isso tem um custo"))
        p("isso tem")
        assertEquals(w("isso tem"), p("isso tem um"))
        assertEquals(w("um custo"), f("isso tem um custo"))
    }

    @Test
    fun parcialVazioEPrimeiroParcial_naoEntregamNada() {
        assertEquals(emptyList<String>(), p(""))
        assertEquals(emptyList<String>(), p("ouvir bem"))
        assertEquals(emptyList<String>(), p(""))
        assertEquals(emptyList<String>(), p("ouvir"))
    }

    @Test
    fun reset_sessaoNovaNaoComparaComHipotesesAnteriores() {
        p("ouvir bem")
        assertEquals(w("ouvir bem"), p("ouvir bem é"))
        r.reset()
        assertEquals(emptyList<String>(), p("ouvir bem é"))
        assertEquals(w("ouvir bem é"), p("ouvir bem é uma"))
    }
}
```

- [ ] **Passo 2: ver falhar**

Run: `./gradlew :core:test --tests '*PartialReconcilerTest'`
Esperado: FAIL na compilação, `Unresolved reference 'PartialReconciler'`.

- [ ] **Passo 3: implementar**

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

/**
 * Transforma parciais e finais do reconhecedor em palavras confirmadas, sem repetir nem perder palavra
 * (TRD Motor de voz §Reconciliação de hipóteses, passos 1–6). O passo 7 (descartar evento de sessão
 * antiga) é de quem chama, antes deste objeto; quem chama também chama reset() ao abrir cada sessão.
 * Não é thread-safe: um objeto por leitura, usado de um só lugar.
 */
class PartialReconciler {
    private val confirmed = mutableListOf<String>()
    private var previous: List<String>? = null

    /** Passos 2–4: só o prefixo comum com o parcial anterior é candidato; devolve as palavras novas. */
    fun onPartial(words: List<String>): List<String> {
        val last = previous
        previous = words
        if (last == null || words.isEmpty()) return emptyList()
        return deliver(commonPrefix(last, words))
    }

    /** Passo 5: entrega o que faltar do resultado final e zera o estado da frase. */
    fun onFinal(words: List<String>): List<String> {
        val delivered = deliver(words)
        reset()
        return delivered
    }

    /** Zera a frase em curso; chamado no Final e no começo de cada sessão. */
    fun reset() {
        confirmed.clear()
        previous = null
    }

    private fun deliver(sequence: List<String>): List<String> {
        val from = endOfConfirmed(sequence)
        val fresh = sequence.subList(from, sequence.size).toList()
        confirmed += fresh // passo 6: nada já entregue é retirado
        return fresh
    }

    private fun commonPrefix(
        a: List<String>,
        b: List<String>,
    ): List<String> {
        var n = 0
        while (n < a.size && n < b.size && same(a[n], b[n])) n++
        return b.subList(0, n).toList()
    }

    /**
     * Passo 3: distância de edição por palavra entre `confirmed` e cada prefixo de `sequence`.
     * Devolve o tamanho do prefixo mais próximo; no empate, o maior (prefere não repetir palavra).
     */
    private fun endOfConfirmed(sequence: List<String>): Int {
        if (confirmed.isEmpty()) return 0
        var row = IntArray(sequence.size + 1) { it }
        for (i in 1..confirmed.size) {
            val next = IntArray(sequence.size + 1)
            next[0] = i
            for (j in 1..sequence.size) {
                val cost = if (same(confirmed[i - 1], sequence[j - 1])) 0 else 1
                next[j] = minOf(row[j] + 1, next[j - 1] + 1, row[j - 1] + cost)
            }
            row = next
        }
        var best = 0
        for (j in row.indices) if (row[j] <= row[best]) best = j
        return best
    }

    private fun same(
        a: String,
        b: String,
    ): Boolean = Normalizer.normalize(a) == Normalizer.normalize(b)
}
```

- [ ] **Passo 4: ver passar**

Run: `./gradlew :core:test --tests '*PartialReconcilerTest'`
Esperado: PASS (10 testes).

- [ ] **Passo 5: commit**

```bash
cd /Users/giorgioderico/Projetos/Tribuna
git add etapa0/core/src
git commit -m "feat(align): PartialReconciler por prefixo estável e distância de edição (TRD §Reconciliação de hipóteses 1-6)"
```

## Tarefa 6: suítes do `Aligner` (antes do código)

**Arquivos:**
- Criar: `<align-test>Trajectory.kt`, `<align-test>AlignerTest.kt`, `<align-test>AlignerTrajectoryTest.kt`, `<align-test>AlignerL30Test.kt`

Como as trajetórias são montadas (D21): uma palavra por chamada de `advance`; a cauda é a lista de todas as palavras desde o último `reposition` ou `NewSession`; a leitura começa em `reposition(0)`; o esperado é o maior token do roteiro lido até ali (congela no improviso e na repetição) e vira a nova posição num `reposition`. Salto errado = cursor mais de 2 tokens à frente do esperado, ou qualquer mudança de cursor numa amostra de improviso. Palavras trocadas no T01: na palavra de índice *i* com *i* mod 5 = 2, alterna variação ortográfica (tira o "s" final, ou põe um) e a próxima palavra do T03. Improviso do T04: o T03 inteiro; improviso do L30: o 1º parágrafo do T01.

- [ ] **Passo 1: escrever o executor de trajetórias**

`<align-test>Trajectory.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

/**
 * Trajetória sintética (spec 0002, D21): uma palavra por chamada de advance, cauda mantida pelo teste (D1),
 * amostra depois de cada chamada. O esperado é o maior token lido do roteiro até ali (congela no improviso
 * e na repetição); reposition redefine o esperado para a nova posição.
 */
sealed interface Step {
    /** Palavra do roteiro; `token` é o token que termina nesta palavra, ou null no meio de um token composto. */
    data class Read(
        val word: String,
        val token: Int?,
    ) : Step

    /** Palavra fora do roteiro (improviso ou erro sem correspondente). */
    data class Improv(
        val word: String,
    ) : Step

    /** Nova sessão (pausa e retomada): quem chama zera a cauda; nenhuma chamada. */
    data object NewSession : Step

    /** Toque numa palavra: reposition e cauda zerada. */
    data class Reposition(
        val index: Int,
    ) : Step
}

enum class Kind { Read, Improv, Reposition }

data class Sample(
    val step: Int,
    val word: String?,
    val kind: Kind,
    val previousCursor: Int,
    val state: AlignState,
    val expected: Int,
) {
    /** Atraso em tokens (TESTES-ETAPA0, Métricas). */
    val delay: Int get() = expected - state.cursor

    /** Mais de 2 tokens à frente do esperado, ou qualquer movimento durante improviso. */
    val wrongJump: Boolean get() = state.cursor > expected + 2 || (kind == Kind.Improv && state.cursor != previousCursor)
}

/** Qual forma falada usar nas referências do T02 (protocolo: três takes). */
enum class Take { First, A, B, C }

object Speech {
    fun of(
        tokens: List<Token>,
        from: Int,
        toExclusive: Int,
        take: Take = Take.First,
    ): List<Step.Read> =
        (from until toExclusive).flatMap { i ->
            val form = pick(tokens[i], take)
            form.mapIndexed { n, word -> Step.Read(word, if (n == form.lastIndex) i else null) }
        }

    fun improv(words: List<String>): List<Step.Improv> = words.map { Step.Improv(it) }

    /** Todas as palavras faladas (primeira forma) de um texto, para usar como improviso. */
    fun wordsOf(tokens: List<Token>): List<String> = tokens.flatMap { it.spoken.first() }

    /** Índice do primeiro token de `phrase` (palavras simples), a partir de `from`. */
    fun indexOf(
        tokens: List<Token>,
        phrase: String,
        from: Int = 0,
    ): Int {
        val words = Normalizer.words(phrase)
        for (i in from..tokens.size - words.size) {
            if (words.indices.all { tokens[i + it].spoken.first() == listOf(words[it]) }) return i
        }
        error("frase não encontrada: $phrase")
    }

    /** Token de referência bíblica: tem uma forma "capítulo ... versículo(s) ...". */
    fun isReference(token: Token): Boolean = token.spoken.any { it.size > 1 && it.first() == "capitulo" }

    private fun pick(
        token: Token,
        take: Take,
    ): List<String> {
        if (take == Take.First || !isReference(token)) return token.spoken.first()
        val plain = token.spoken.filter { it.first() != "capitulo" }
        val shortest = plain.minOf { it.size }
        return when (take) {
            Take.A -> plain.first { it.size == shortest }

            // "três dezesseis", "treze quatro a sete"
            Take.B -> plain.first { it.size == shortest + 1 }

            // "três e dezesseis", "treze de quatro a sete"
            else -> token.spoken.first { it.first() == "capitulo" } // "capítulo três versículo dezesseis"
        }
    }
}

class TrajectoryRunner(
    tokens: List<Token>,
    params: AlignParams,
) {
    val aligner = Aligner(tokens, params)
    val advanceNanos = mutableListOf<Long>()

    fun run(steps: List<Step>): List<Sample> {
        var state = aligner.reposition(0) // D3
        val tail = mutableListOf<String>()
        var expected = 0
        val samples = mutableListOf<Sample>()
        steps.forEachIndexed { i, step ->
            val previous = state.cursor
            when (step) {
                is Step.NewSession -> {
                    tail.clear()
                }

                is Step.Reposition -> {
                    state = aligner.reposition(step.index)
                    tail.clear()
                    expected = state.cursor
                    samples += Sample(i, null, Kind.Reposition, previous, state, expected)
                }

                is Step.Read -> {
                    tail += step.word
                    state = timed(state, tail)
                    step.token?.let { expected = maxOf(expected, it) }
                    samples += Sample(i, step.word, Kind.Read, previous, state, expected)
                }

                is Step.Improv -> {
                    tail += step.word
                    state = timed(state, tail)
                    samples += Sample(i, step.word, Kind.Improv, previous, state, expected)
                }
            }
        }
        return samples
    }

    private fun timed(
        state: AlignState,
        tail: List<String>,
    ): AlignState {
        val start = System.nanoTime()
        val next = aligner.advance(state, tail)
        advanceNanos += System.nanoTime() - start
        return next
    }
}

fun List<Sample>.reads(): List<Sample> = filter { it.kind == Kind.Read }

fun List<Sample>.wrongJumps(): List<Sample> = filter { it.wrongJump }

fun List<Sample>.fractionWithinDelay(maxDelay: Int = 3): Double {
    require(isNotEmpty()) { "nenhuma amostra (Q3)" }
    return count { it.delay <= maxDelay }.toDouble() / size
}

fun List<Sample>.moveChanges(): Int = count { it.state.cursor != it.previousCursor }
```

- [ ] **Passo 2: escrever os testes pontuais**

`<align-test>AlignerTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** Casos pontuais do Aligner (TRD Contratos §Aligner; Motor de voz §Alinhamento; spec 0002, D3–D13). */
class AlignerTest {
    private val t01 = TestTexts.tokens("T01")
    private val normal = Aligner(t01, AlignPresets.normal)

    @Test
    fun reposition_segueD4() {
        assertEquals(AlignState(5, 5, 0, Move.Reposition, 0.0), normal.reposition(5))
        assertEquals(0, normal.reposition(-3).cursor)
        assertEquals(0, normal.reposition(-3).highWater)
        assertEquals(t01.lastIndex, normal.reposition(10_000).cursor)
        assertEquals(t01.lastIndex, normal.reposition(10_000).highWater)
    }

    @Test
    fun reposition_primeiroAvancoPartedaNovaPosicao() {
        val start = Speech.indexOf(t01, "Ouvir com atenção começa")
        var state = normal.reposition(start)
        val tail = mutableListOf<String>()
        for (word in listOf("ouvir", "com", "atencao")) {
            tail += word
            state = normal.advance(state, tail)
        }
        assertEquals(start + 2, state.cursor)
        assertEquals(start + 2, state.highWater)
        assertEquals(Move.Short, state.lastMove)
    }

    @Test
    fun reposition_voltaLivrementeAbaixoDeHighWaterMenos3() {
        val steps = Speech.of(t01, 0, 151) + Step.Reposition(10) + Speech.of(t01, 11, 14)
        val samples = TrajectoryRunner(t01, AlignPresets.normal).run(steps)
        val reposition = samples.first { it.kind == Kind.Reposition }
        assertEquals(150, reposition.previousCursor)
        assertEquals(AlignState(10, 10, 0, Move.Reposition, 0.0), reposition.state)
        assertEquals(13, samples.last().state.cursor)
        assertEquals(13, samples.last().state.highWater)
    }

    @Test
    fun roteiroVazio_advanceDevolveOEstadoERepositionVaiAZero() {
        val empty = Aligner(emptyList(), AlignPresets.normal)
        val state = empty.reposition(3)
        assertEquals(AlignState(0, 0, 0, Move.Reposition, 0.0), state)
        assertEquals(state, empty.advance(state, listOf("ouvir", "bem", "e")))
    }

    @Test
    fun menosDeKPalavras_ficaParadoSemMudarMissStreak_D5() {
        val start = normal.reposition(0).copy(missStreak = 1)
        val next = normal.advance(start, listOf("ouvir", "bem"))
        assertEquals(AlignState(0, 0, 1, Move.Stay, 0.0), next)
    }

    @Test
    fun unkNuncaCasa_D13() {
        val next = normal.advance(normal.reposition(0), listOf("[unk]", "[unk]", "[unk]"))
        assertEquals(0, next.cursor)
        assertEquals(Move.Stay, next.lastMove)
        assertEquals(0.0, next.score, 0.0)
        // [unk] no lugar da palavra mais nova: a posição não avança nessa chamada.
        val tail = mutableListOf("ouvir", "bem", "e")
        val atE = normal.advance(normal.reposition(0), tail)
        assertEquals(2, atE.cursor)
        tail += "[unk]"
        assertEquals(2, normal.advance(atE, tail).cursor)
    }

    @Test
    fun falaComAcentosEMaiusculas_mesmaTrajetoriaQueANormalizada_T03() {
        val text = TestTexts.read("T03")
        val tokens = Tokenizer.tokenize(text)
        val normalized = Speech.of(tokens, 0, tokens.size)
        val shouted =
            tokens.flatMap { token ->
                // "bom-dia" vira "BOM", "DIA": mesma divisão em palavras da forma normalizada.
                val parts = text.substring(token.display.first, token.display.last + 1).uppercase(Locale.ROOT).split('-')
                parts.mapIndexed { n, part -> Step.Read(part, if (n == parts.lastIndex) token.index else null) }
            }
        val a = TrajectoryRunner(tokens, AlignPresets.normal).run(normalized).map { it.state }
        val b = TrajectoryRunner(tokens, AlignPresets.normal).run(shouted).map { it.state }
        assertEquals(a, b)
        assertTrue(shouted.any { it.word == "ESCOLHA" })
    }

    @Test
    fun falaComAcentos_referenciasDoT02_mesmaTrajetoria() {
        val tokens = TestTexts.tokens("T02")
        val accents = mapOf("tres" to "Três", "capitulo" to "Capítulo", "versiculo" to "Versículo", "versiculos" to "versículos")
        val plain = Speech.of(tokens, 0, tokens.size, Take.C)
        val accented = plain.map { Step.Read(accents[it.word] ?: it.word, it.token) }
        val a = TrajectoryRunner(tokens, AlignPresets.normal).run(plain).map { it.state }
        val b = TrajectoryRunner(tokens, AlignPresets.normal).run(accented).map { it.state }
        assertEquals(a, b)
        assertTrue(accented.any { it.word == "Três" })
    }

    /**
     * Q2: este teste fica vermelho se a janela for medida contra o cursor (max(cursor − 3, 0)) em vez de
     * max(cursor − 3, highWater − 3), ou se o limite for removido.
     */
    @Test
    fun recuosSucessivosNaoPassamDeHighWaterMenos3() {
        val h = Speech.indexOf(t01, "Damos conselhos para um problema que não existe") + 7 // "existe"
        val read = Speech.of(t01, 0, h + 1)
        val back =
            "um problema que damos conselhos para um problema que damos conselhos para"
                .split(' ')
                .map { Step.Improv(it) }
        val samples = TrajectoryRunner(t01, AlignPresets.normal).run(read + back)
        val after = samples.drop(read.size)
        assertEquals(h, samples[read.size - 1].state.cursor)
        for (s in samples) {
            assertTrue("cursor ${s.state.cursor} abaixo de highWater ${s.state.highWater} − 3", s.state.cursor >= s.state.highWater - 3)
        }
        assertTrue("houve recuo permitido (Q3)", after.any { it.state.cursor < h })
        assertTrue("nunca abaixo de h − 3", after.all { it.state.cursor >= h - 3 })
    }

    /** Q2: fica vermelho se a recuperação saltar com dois ou mais candidatos (ex.: para o primeiro). */
    @Test
    fun recuperacaoAmbigua_naoMoveOCursor_L30() {
        val l30 = TestTexts.tokens("L30")
        val aligner = Aligner(l30, AlignPresets.normal)
        val first = Speech.indexOf(l30, "no fim do ano a")
        val second = Speech.indexOf(l30, "no fim do ano a", first + 1)
        assertTrue("duas cópias longe uma da outra", second - first > 1000)
        val start = first - 100 // a janela [start − 3, start + 40] não alcança nenhuma cópia
        var state = aligner.reposition(start)
        val tail = mutableListOf<String>()
        val states = mutableListOf<AlignState>()
        for (word in "no fim do ano a".split(' ')) {
            tail += word
            state = aligner.advance(state, tail)
            states += state
        }
        for (s in states) {
            assertEquals(start, s.cursor)
            assertEquals(start, s.highWater)
            assertEquals(Move.Stay, s.lastMove)
        }
        assertTrue("as 4 palavras casaram (Q3)", states.last().score >= AlignPresets.normal.longThreshold)
        assertTrue(states.last().missStreak >= AlignConstants.MISSES_FOR_RECOVERY)
    }
}
```

- [ ] **Passo 3: escrever os testes de trajetória**

`<align-test>AlignerTrajectoryTest.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suíte "Alinhamento" com trajetórias sintéticas (TRD Testes; spec 0002, critérios do Aligner e D21).
 * Salvo indicação, preset Normal. "6 palavras" ≈ 3 s a 130 palavras/min.
 */
class AlignerTrajectoryTest {
    private val t01 = TestTexts.tokens("T01")

    private fun assertNoWrongJumps(samples: List<Sample>) {
        val wrong = samples.wrongJumps()
        assertTrue("saltos errados: ${wrong.take(5)}", wrong.isEmpty())
    }

    /** Atraso ≤ 3 em todas as amostras de leitura a partir da 6ª palavra depois de `firstStep`. */
    private fun assertCaughtUpBySixthWord(
        samples: List<Sample>,
        firstStep: Int,
    ) {
        val after = samples.filter { it.kind == Kind.Read && it.step >= firstStep }
        assertTrue("amostras depois do evento (Q3)", after.size > 6)
        val late = after.drop(5).filter { it.delay > 3 }
        assertTrue("atraso > 3 depois da 6ª palavra: ${late.take(5)}", late.isEmpty())
    }

    @Test
    fun leituraNormalT01_tresPresets_cursorIgualAoEsperadoDesdeAKesimaPalavra() {
        for (params in listOf(AlignPresets.conservador, AlignPresets.normal, AlignPresets.agil)) {
            val samples = TrajectoryRunner(t01, params).run(Speech.of(t01, 0, t01.size))
            assertEquals(t01.size, samples.size)
            samples.drop(params.k - 1).forEach { assertEquals("$params passo ${it.step}", it.expected, it.state.cursor) }
            assertTrue("$params moveu ${samples.moveChanges()} vezes (Q3)", samples.moveChanges() >= t01.size - params.k)
            assertNoWrongJumps(samples)
        }
    }

    @Test
    fun palavrasErradasT01_umaEmCincoTrocada() {
        // D21: na palavra i com i % 5 == 2, alterna variação ortográfica (tira ou põe "s") e palavra do T03.
        val others = Speech.wordsOf(TestTexts.tokens("T03"))
        var next = 0
        val steps =
            Speech.of(t01, 0, t01.size).mapIndexed { i, step ->
                if (i % 5 != 2) {
                    step
                } else if ((i / 5) % 2 == 0) {
                    Step.Read(if (step.word.endsWith("s")) step.word.dropLast(1) else step.word + "s", step.token)
                } else {
                    while (others[next % others.size] == step.word) next++
                    Step.Read(others[next++ % others.size], step.token)
                }
            }
        assertEquals(43, steps.indices.count { it % 5 == 2 })
        val samples = TrajectoryRunner(t01, AlignPresets.normal).run(steps)
        assertNoWrongJumps(samples)
        assertTrue(samples.fractionWithinDelay() >= 0.95)
        assertEquals(t01.lastIndex, samples.last().state.cursor)
    }

    @Test
    fun puloDeParagrafoT05_recuperaAte6Palavras() {
        val t05 = TestTexts.tokens("T05")
        val skipped = Speech.indexOf(t05, "Isso tem um custo")
        val resume = Speech.indexOf(t05, "Ouvir com atenção começa")
        val before = Speech.of(t05, 0, skipped)
        val samples = TrajectoryRunner(t05, AlignPresets.normal).run(before + Speech.of(t05, resume, t05.size))
        assertNoWrongJumps(samples)
        assertCaughtUpBySixthWord(samples, firstStep = before.size)
        assertTrue(samples.any { it.state.lastMove == Move.Recovery })
        assertEquals(t05.lastIndex, samples.last().state.cursor)
    }

    @Test
    fun saltoLongoDentroDaJanelaT01_pularUmaFrase() {
        val skipped = Speech.indexOf(t01, "Damos conselhos para um problema")
        val resume = Speech.indexOf(t01, "E a pessoa sai")
        assertTrue(resume - skipped < AlignPresets.normal.window)
        val samples =
            TrajectoryRunner(t01, AlignPresets.normal).run(Speech.of(t01, 0, skipped) + Speech.of(t01, resume, t01.size))
        assertNoWrongJumps(samples)
        assertTrue(samples.any { it.state.lastMove == Move.LongJump })
    }

    @Test
    fun improvisoT04_cursorParadoERetomaAte6Palavras() {
        val t04 = TestTexts.tokens("T04")
        val back = Speech.indexOf(t04, "Isso tem um custo")
        val improv = Speech.improv(Speech.wordsOf(TestTexts.tokens("T03")))
        val before = Speech.of(t04, 0, back)
        val samples =
            TrajectoryRunner(t04, AlignPresets.normal).run(before + improv + Speech.of(t04, back, t04.size))
        val during = samples.filter { it.kind == Kind.Improv }
        assertEquals(improv.size, during.size)
        assertTrue("cursor mudou no improviso", during.all { it.state.cursor == it.previousCursor })
        assertNoWrongJumps(samples)
        assertCaughtUpBySixthWord(samples, firstStep = before.size + improv.size)
    }

    @Test
    fun repeticaoPeloOradorT08_semSaltoErradoNemRecuoAlemDoLimite() {
        val t08 = TestTexts.tokens("T08")
        val first = Speech.indexOf(t08, "Isso tem um custo")
        val afterFirst = Speech.indexOf(t08, "Quando não ouvimos")
        val second = Speech.indexOf(t08, "Um momento de pausa")
        val afterSecond = Speech.indexOf(t08, "Muitas vezes, é nesse")
        val part1 = Speech.of(t08, 0, afterFirst) + Speech.of(t08, first, afterFirst)
        val part2 = Speech.of(t08, afterFirst, afterSecond) + Speech.of(t08, second, afterSecond)
        val samples = TrajectoryRunner(t08, AlignPresets.normal).run(part1 + part2 + Speech.of(t08, afterSecond, t08.size))
        assertNoWrongJumps(samples)
        assertTrue(samples.all { it.state.cursor >= it.state.highWater - 3 })
        assertCaughtUpBySixthWord(samples.filter { it.step < part1.size + part2.size }, firstStep = part1.size)
        assertCaughtUpBySixthWord(samples, firstStep = part1.size + part2.size)
    }

    @Test
    fun trechosRepetidosT03_nuncaPulaParaOutraCopia() {
        val t03 = TestTexts.tokens("T03")
        val samples = TrajectoryRunner(t03, AlignPresets.normal).run(Speech.of(t03, 0, t03.size))
        assertNoWrongJumps(samples)
        assertTrue(samples.moveChanges() >= t03.size - AlignPresets.normal.k)
    }

    @Test
    fun palavrasComunsT09() {
        val t09 = TestTexts.tokens("T09")
        val samples = TrajectoryRunner(t09, AlignPresets.normal).run(Speech.of(t09, 0, t09.size))
        assertNoWrongJumps(samples)
        assertTrue(samples.size >= 50)
        assertTrue(samples.fractionWithinDelay() >= 0.95)
    }

    @Test
    fun numerosEVariantesT02_tresTakes() {
        val t02 = TestTexts.tokens("T02")
        val references = t02.filter { Speech.isReference(it) }.map { it.index }
        assertEquals(5, references.size)
        for (take in listOf(Take.A, Take.B, Take.C)) {
            val samples = TrajectoryRunner(t02, AlignPresets.normal).run(Speech.of(t02, 0, t02.size, take))
            assertNoWrongJumps(samples)
            assertEquals("$take", t02.lastIndex, samples.last().state.cursor)
            for (ref in references) {
                val reached = samples.any { it.state.cursor == ref }
                val passedClose = samples.firstOrNull { it.state.cursor > ref }?.let { it.delay <= 3 } ?: false
                assertTrue("$take referência $ref", reached || passedClose)
            }
        }
    }

    @Test
    fun cadaMoveApareceComoLastMove() {
        val seen = mutableSetOf<Move>()
        seen += TrajectoryRunner(t01, AlignPresets.normal).run(Speech.of(t01, 0, t01.size)).map { it.state.lastMove }
        val skipped = Speech.indexOf(t01, "Damos conselhos para um problema")
        val resume = Speech.indexOf(t01, "E a pessoa sai")
        seen +=
            TrajectoryRunner(t01, AlignPresets.normal)
                .run(Speech.of(t01, 0, skipped) + Speech.of(t01, resume, t01.size))
                .map { it.state.lastMove }
        val t05 = TestTexts.tokens("T05")
        seen +=
            TrajectoryRunner(t05, AlignPresets.normal)
                .run(
                    Speech.of(t05, 0, Speech.indexOf(t05, "Isso tem um custo")) +
                        Speech.of(t05, Speech.indexOf(t05, "Ouvir com atenção começa"), t05.size),
                ).map { it.state.lastMove }
        seen += TrajectoryRunner(t01, AlignPresets.normal).run(listOf(Step.Reposition(20))).map { it.state.lastMove }
        assertEquals(Move.entries.toSet(), seen)
    }
}
```

`<align-test>AlignerL30Test.kt`:

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** L30 sintético com os eventos das linhas // (spec 0002, último critério do Aligner; D21 e D23). */
class AlignerL30Test {
    @Test
    fun l30Sintetico_semSaltoErrado_atrasoAte3Em95PorCento_chegaAoFim() {
        val l30 = TestTexts.tokens("L30")
        val improvBefore = Speech.indexOf(l30, "Agora, é importante falar") // nota do minuto 8
        val pauseBefore = Speech.indexOf(l30, "Há momentos, porém") // nota do minuto 12
        val skipped = Speech.indexOf(l30, "Um detalhe prático ajuda") // nota do minuto 15
        val resume = Speech.indexOf(l30, "Outra ferramenta preciosa")
        val bridgeEnd = Speech.indexOf(l30, "como uma ponte velha") + 4 // nota do minuto 22
        val imagine = Speech.indexOf(l30, "Imagine duas pessoas")
        val repeat = Speech.indexOf(l30, "Ninguém chega longe de uma vez só") // nota do minuto 25
        val repeatEnd = repeat + 7

        val t01 = TestTexts.tokens("T01")
        val improv = Speech.improv(Speech.wordsOf(t01.subList(0, Speech.indexOf(t01, "Isso tem um custo"))))

        val steps = mutableListOf<Step>()
        steps += Speech.of(l30, 0, improvBefore)
        steps += improv
        steps += Speech.of(l30, improvBefore, pauseBefore)
        steps += Step.NewSession // pausa e retomada: nova sessão, cauda zerada
        steps += Speech.of(l30, pauseBefore, skipped)
        val skipStep = steps.size
        // Silêncio de 30 s depois do parágrafo do minuto 18: nenhuma chamada (nada a acrescentar).
        steps += Speech.of(l30, resume, bridgeEnd)
        val repositionStep = steps.size
        steps += Step.Reposition(imagine)
        steps += Speech.of(l30, imagine, repeatEnd)
        steps += Speech.of(l30, repeat, repeatEnd) // repetição como ênfase
        steps += Speech.of(l30, repeatEnd, l30.size)

        val runner = TrajectoryRunner(l30, AlignPresets.normal)
        val samples = runner.run(steps)

        val wrong = samples.wrongJumps()
        assertTrue("saltos errados: ${wrong.take(5)}", wrong.isEmpty())
        assertEquals(l30.lastIndex, samples.last().state.cursor)

        val reads = samples.reads()
        val excluded =
            reads
                .filter { it.step >= skipStep }
                .take(6)
                .map { it.step }
                .toSet() +
                reads
                    .filter { it.step > repositionStep }
                    .take(6)
                    .map { it.step }
                    .toSet()
        val normalReading = reads.filter { it.step !in excluded }
        assertTrue("amostras de leitura normal (Q3)", normalReading.size > 3000)
        assertTrue(normalReading.fractionWithinDelay() >= 0.95)
        assertTrue(samples.any { it.state.lastMove == Move.Recovery })

        val totalMs = runner.advanceNanos.sum() / 1_000_000.0
        val maxMs = runner.advanceNanos.max() / 1_000_000.0
        println("L30 sintético: ${runner.advanceNanos.size} chamadas de advance, total %.1f ms, máximo %.3f ms".format(totalMs, maxMs))
    }
}
```

- [ ] **Passo 4: ver falhar**

Run: `./gradlew :core:test --tests '*Aligner*Test'`
Esperado: compila; os 21 testes novos falham com `kotlin.NotImplementedError: Aligner.reposition é implementado na tarefa 0002` (ou `advance`). `AlignContractShapeTest` continua verde.

- [ ] **Passo 5: commit dos testes vermelhos**

```bash
cd /Users/giorgioderico/Projetos/Tribuna
git add etapa0/core/src/test
git commit -m "test(align): suítes do Aligner antes do código (TRD Testes, Alinhamento)"
```

## Tarefa 7: implementar o `Aligner`

**Arquivos:**
- Modificar: `<align-main>Aligner.kt` (substituir o arquivo inteiro; construtor, `advance` e `reposition` mantêm exatamente as assinaturas da 0001)

- [ ] **Passo 1: implementar**

```kotlin
package io.github.gigioderico.tribuna.etapa0.align

import io.github.gigioderico.tribuna.etapa0.align.AlignConstants.MAX_BACK_FROM_HIGH_WATER
import io.github.gigioderico.tribuna.etapa0.align.AlignConstants.MISSES_FOR_RECOVERY
import io.github.gigioderico.tribuna.etapa0.align.AlignConstants.RECOVERY_WORDS
import io.github.gigioderico.tribuna.etapa0.align.AlignConstants.SHORT_DISTANCE_TOKENS
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Função pura: recebe o estado e as palavras confirmadas e devolve o novo estado (TRD §Contratos, Aligner).
 * Não conhece Android nem o motor.
 *
 * Invariante: um movimento automático nunca leva o cursor abaixo de highWater − 3.
 * Só reposition move o cursor livremente, e ele redefine cursor e highWater para a nova posição.
 *
 * `words` é a cauda das palavras confirmadas desde o último reposition (ou início da sessão), em ordem,
 * terminando na mais nova; quem chama mantém a cauda e chama advance uma vez por lote de palavras novas
 * (impl_plan 0002, decisão A1). Só as últimas max(k, 4) palavras são lidas.
 * O cursor é o índice do último token falado (D2); a leitura começa com reposition(0) (D3).
 */
class Aligner(
    private val tokens: List<Token>,
    private val params: AlignParams,
) {
    private val forms: List<List<List<String>>> =
        tokens.map { token -> token.spoken.map { form -> form.flatMap(Normalizer::words) }.filter { it.isNotEmpty() } }
    private val weights: DoubleArray = DoubleArray(tokens.size) { tokens[it].weight.toDouble() }

    fun advance(
        state: AlignState,
        words: List<String>,
    ): AlignState {
        if (tokens.isEmpty()) return state
        val heard = recentWords(words)
        if (heard.size < params.k) return state.copy(lastMove = Move.Stay, score = 0.0) // D5
        val best = searchWindow(state, heard.takeLast(params.k))
        if (best.lastWord >= params.shortThreshold) { // decisão A2: a palavra mais nova precisa casar
            val short = abs(best.position - state.cursor) <= SHORT_DISTANCE_TOKENS && best.similarity >= params.shortThreshold
            if (short || best.similarity >= params.longThreshold) {
                if (best.position == state.cursor) {
                    return state.copy(missStreak = 0, lastMove = Move.Stay, score = best.similarity) // D6
                }
                return AlignState(
                    cursor = best.position,
                    highWater = max(state.highWater, best.position),
                    missStreak = 0,
                    lastMove = if (short) Move.Short else Move.LongJump,
                    score = best.similarity,
                )
            }
        }
        val misses = state.missStreak + 1 // D6: nenhum candidato passou
        if (misses >= MISSES_FOR_RECOVERY && heard.size >= RECOVERY_WORDS) {
            val recovery = recover(state.highWater, heard.takeLast(RECOVERY_WORDS))
            val jump = recovery.jump
            if (jump != null) {
                return AlignState(jump.position, jump.position, 0, Move.Recovery, jump.similarity) // D7
            }
            return state.copy(missStreak = misses, lastMove = Move.Stay, score = max(best.similarity, recovery.bestSimilarity))
        }
        return state.copy(missStreak = misses, lastMove = Move.Stay, score = best.similarity)
    }

    fun reposition(tokenIndex: Int): AlignState {
        // toque, Volume −, arrasto, troca de modo (D4: índice fora da faixa é limitado)
        val index = if (tokens.isEmpty()) 0 else tokenIndex.coerceIn(0, tokens.lastIndex)
        return AlignState(cursor = index, highWater = index, missStreak = 0, lastMove = Move.Reposition, score = 0.0)
    }

    private class Match(
        val position: Int,
        val similarity: Double,
        val lastWord: Double,
    )

    private class Recovery(
        val jump: Match?,
        val bestSimilarity: Double,
    )

    /** D13: normaliza a fala; [unk] vira a marca que nunca casa. Lê só o fim da cauda. */
    private fun recentWords(words: List<String>): List<String> {
        val needed = maxOf(params.k, RECOVERY_WORDS)
        val out = ArrayDeque<String>()
        var i = words.lastIndex
        while (i >= 0 && out.size < needed) {
            val raw = words[i]
            val normalized =
                if (raw.trim().lowercase(Locale.ROOT) == Similarity.UNKNOWN) listOf(Similarity.UNKNOWN) else Normalizer.words(raw)
            for (x in normalized.indices.reversed()) out.addFirst(normalized[x])
            i--
        }
        return out.toList().takeLast(needed)
    }

    /** TRD §Alinhamento 2 e D9: janela [max(cursor − 3, highWater − 3), cursor + W]; empate → mais perto do cursor. */
    private fun searchWindow(
        state: AlignState,
        heard: List<String>,
    ): Match {
        val from = maxOf(state.cursor - MAX_BACK_FROM_HIGH_WATER, state.highWater - MAX_BACK_FROM_HIGH_WATER, 0)
        val to = minOf(state.cursor + params.window, tokens.lastIndex)
        var best: Match? = null
        for (position in from..to) {
            val match = score(position, heard)
            val current = best
            if (current == null ||
                match.similarity > current.similarity + EPSILON ||
                (
                    abs(match.similarity - current.similarity) <= EPSILON &&
                        abs(position - state.cursor) < abs(current.position - state.cursor)
                )
            ) {
                best = match
            }
        }
        return best ?: Match(state.cursor, 0.0, 0.0)
    }

    /** TRD §Alinhamento 4, D7 e D8: só à frente de highWater; salta só com exatamente uma posição ≥ limite longo. */
    private fun recover(
        highWater: Int,
        heard: List<String>,
    ): Recovery {
        val candidates = mutableListOf<Match>()
        var bestSimilarity = 0.0
        for (position in highWater + 1..tokens.lastIndex) {
            val match = score(position, heard)
            bestSimilarity = max(bestSimilarity, match.similarity)
            if (match.similarity >= params.longThreshold) candidates += match
        }
        val only = candidates.singleOrNull()?.takeIf { it.lastWord >= params.shortThreshold }
        return Recovery(only, bestSimilarity)
    }

    /**
     * Similaridade média ponderada das palavras ouvidas terminando no token `position` (TRD §Alinhamento 1 e 2).
     * A palavra mais nova casa com a última palavra de uma forma falada do token; as anteriores andam para trás
     * pelo roteiro, e cada token usa a forma que der a maior média. Antes do início do roteiro, cada palavra
     * que sobra conta com similaridade 0 e peso 1. `lastWord` é a similaridade da palavra mais nova (decisão A2).
     */
    private fun score(
        position: Int,
        heard: List<String>,
    ): Match {
        var bestSimilarity = -1.0
        var bestLast = 0.0

        fun walk(
            token: Int,
            newest: Int,
            sum: Double,
            weight: Double,
            last: Double,
        ) {
            if (newest < 0 || token < 0) {
                val total = weight + (newest + 1)
                val similarity = if (total == 0.0) 0.0 else sum / total
                if (similarity > bestSimilarity + EPSILON) {
                    bestSimilarity = similarity
                    bestLast = last
                }
                return
            }
            val w = weights[token]
            for (form in forms[token]) {
                val used = minOf(form.size, newest + 1)
                var s = sum
                var total = weight
                for (x in 0 until used) {
                    s += w * Similarity.of(heard[newest - used + 1 + x], form[form.size - used + x])
                    total += w
                }
                val lastWord = if (token == position) Similarity.of(heard[newest], form.last()) else last
                walk(token - 1, newest - used, s, total, lastWord)
            }
        }

        walk(position, heard.lastIndex, 0.0, 0.0, 0.0)
        return Match(position, bestSimilarity.coerceAtLeast(0.0), bestLast)
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}
```

- [ ] **Passo 2: ver passar**

Run: `./gradlew :core:test`
Esperado: `BUILD SUCCESSFUL`, 85 testes, 0 falhas.

- [ ] **Passo 3: estilo**

Run: `./gradlew :core:ktlintCheck :core:lint`
Esperado: verde. Se o ktlint reclamar de formatação (não deveria; o código foi formatado com o ktlint 1.8.0), rode `./gradlew :core:ktlintFormat`, confira com `git diff` que só mudou espaço e quebra de linha, e registre no "Registro da construção".

- [ ] **Passo 4: commit**

```bash
cd /Users/giorgioderico/Projetos/Tribuna
git add etapa0/core/src
git commit -m "feat(align): Aligner advance/reposition com janela, recuperação e highWater-3 (TRD §Alinhamento 1-6)"
```

## Tarefa 8: conferir as quebras propositais (Q2), sem commit

Não substitui o QA (quem constrói não testa a própria entrega); serve para o QA não voltar por teste que não morde. Para cada linha da tabela "Quebras propositais previstas para o QA": aplique a troca exata de texto, rode `./gradlew :core:test`, anote os testes que falharam e desfaça com `git checkout -- <arquivo>`.

- [ ] **Passo 1: Q2-1 a Q2-7**, uma por vez, como descrito.
- [ ] **Passo 2: confirmar que nada ficou quebrado**

Run: `cd /Users/giorgioderico/Projetos/Tribuna && git status --short etapa0/ && cd etapa0 && ./gradlew :core:test`
Esperado: nenhum arquivo modificado em `etapa0/`; `BUILD SUCCESSFUL`.

- [ ] **Passo 3: registrar** no "Registro da construção" a tabela quebra → testes vermelhos. Se alguma quebra não deixar teste vermelho, pare e escale ao `arquiteto` (o teste não testa).

## Tarefa 9: verificação completa e evidências

- [ ] **Passo 1: build e regras**

```bash
cd /Users/giorgioderico/Projetos/Tribuna/etapa0
mkdir -p ../work/0002-etapa0-align/evidencias
./gradlew :core:test --rerun-tasks 2>&1 | tee ../work/0002-etapa0-align/evidencias/core-test.txt
./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest 2>&1 | tee ../work/0002-etapa0-align/evidencias/verificacao.txt
```

Esperado: `BUILD SUCCESSFUL` nos dois; em `verificacao.txt`, `verifyDebugMergedManifest: OK ... -> android.permission.RECORD_AUDIO` e o mesmo para release.

- [ ] **Passo 2: contagem de testes por classe**

```bash
cd /Users/giorgioderico/Projetos/Tribuna/etapa0
for f in core/build/test-results/test/TEST-*.xml; do grep -o 'testsuite name="[^"]*" tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done | tee ../work/0002-etapa0-align/evidencias/contagem-testes.txt
grep -h "L30 sintético" core/build/test-results/test/TEST-*AlignerL30Test.xml | tee -a ../work/0002-etapa0-align/evidencias/contagem-testes.txt
```

Esperado: `NormalizerTest` 6, `SimilarityTest` 3, `NumberWordsTest` 4, `TokenizerTest` 14, `TokenizerTextsTest` 2, `PartialReconcilerTest` 10, `AlignerTest` 10, `AlignerTrajectoryTest` 10, `AlignerL30Test` 1 (60 novos), todos com `skipped="0" failures="0" errors="0"`; e a linha do tempo do L30.

- [ ] **Passo 3: regras do pacote `align`**

```bash
cd /Users/giorgioderico/Projetos/Tribuna
{ echo '$ grep android/androidx'; grep -rE "import (android|androidx)\." etapa0/core/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/align; echo "exit=$?";
  echo '$ grep NotImplementedError'; grep -rn NotImplementedError etapa0/core/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/align; echo "exit=$?"; } | tee work/0002-etapa0-align/evidencias/regras-align.txt
```

Esperado: os dois `grep` sem saída e `exit=1`.

- [ ] **Passo 4: nenhum teste desativado**

```bash
cd /Users/giorgioderico/Projetos/Tribuna
{ echo '$ grep @Ignore/@Disabled'; grep -rnE "@Ignore|@Disabled" etapa0/core/src/test; echo "exit=$?"; } | tee -a work/0002-etapa0-align/evidencias/regras-align.txt
```

Esperado: sem saída, `exit=1`.

- [ ] **Passo 5: contratos intocados**

```bash
cd /Users/giorgioderico/Projetos/Tribuna
A=etapa0/core/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/align
{ echo '$ git diff main -- contratos'; git diff main -- $A/Token.kt $A/AlignState.kt $A/AlignParams.kt $A/Move.kt; echo "linhas=$(git diff main -- $A/Token.kt $A/AlignState.kt $A/AlignParams.kt $A/Move.kt | wc -l)";
  echo '$ assinaturas do Aligner (main vs atual)';
  R='class Aligner\( private val tokens: List<Token>, private val params: AlignParams, \)|fun advance\( state: AlignState, words: List<String>, \): AlignState|fun reposition\(tokenIndex: Int\): AlignState';
  git show main:$A/Aligner.kt | tr -s ' \n' ' ' | grep -oE "$R";
  diff <(git show main:$A/Aligner.kt | tr -s ' \n' ' ' | grep -oE "$R") <(tr -s ' \n' ' ' < $A/Aligner.kt | grep -oE "$R"); echo "diff_exit=$?"; } | tee work/0002-etapa0-align/evidencias/contratos.txt
```

Esperado: `linhas=0`; as três assinaturas de `main` impressas (uma por linha: `class Aligner( ... )`, `fun advance( ... ): AlignState`, `fun reposition(tokenIndex: Int): AlignState`), o que prova que a busca casou; o `diff` sem saída e `diff_exit=0`. Rode com `bash` se o shell não aceitar `<( )`.

- [ ] **Passo 6: preencher o "Registro da construção"** no fim deste arquivo e commitar evidências e registro

```bash
cd /Users/giorgioderico/Projetos/Tribuna
git add work/0002-etapa0-align/evidencias work/0002-etapa0-align/impl_plan.md
git commit -m "docs(0002): evidências da verificação e registro da construção"
```

## Verificação

```bash
cd etapa0
./gradlew :core:test
./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest
grep -rE "import (android|androidx)\." core/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/align   # vazio
grep -rn NotImplementedError core/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/align             # vazio
grep -rnE "@Ignore|@Disabled" core/src/test                                                               # vazio
git diff main -- core/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/align/{Token,AlignState,AlignParams,Move}.kt   # vazio
```

Sucesso significa: os dois Gradle em `BUILD SUCCESSFUL`; 85 testes em `:core` com 0 falhas e 0 ignorados, dos quais 60 novos distribuídos como na Tarefa 9, passo 2; manifesto debug e release só com `RECORD_AUDIO`; os quatro comandos de busca sem saída; as três assinaturas do `Aligner` idênticas às de `main`; as sete quebras da tabela Q2 deixando vermelho o teste previsto. `verify16kAlignment` não entra (não há release nesta tarefa e nada nativo muda).

## Riscos

- **O Vosk pode não devolver as palavras como o `Tokenizer` supõe** (Q4; só a 0003 mostra o JSON real). Casos: hífen junto ou separado ("escrevê-los"), número em dígitos em vez de extenso, "dia primeiro". Mitigação já no plano: as duas formas de hifenizada; o `Normalizer` une partes e o `Aligner` normaliza a fala. Se a 0003 vir dígitos no resultado do Vosk, a correção é no `Aligner.recentWords` (converter dígitos com `NumberWords`) e cai no PLAN da 0003, com teste; não mexer no contrato.
- **Tempo do `advance` no aparelho.** A recuperação percorre todo o roteiro à frente de `highWater` (no L30, ~3.700 posições × formas). Na JVM do Mac: máximo ≈ 3,2 ms. Num aparelho de entrada pode ser 10× mais. Não há meta (D23); a 0005 mede. Se passar do que a latência total permite (< 500 ms no total, TRD Metas), a otimização é um índice invertido da primeira palavra das formas, sem mudar comportamento: decide-se na 0005.
- **A2 atrasa uma palavra quando a mais nova vem errada.** Medido no protótipo: o critério de palavras erradas fica em 100% das amostras com atraso ≤ 3. Se a 0005 mostrar atraso maior com fala real, a calibração ajusta o limite curto; não se remove A2 sem um teste de improviso que continue verde.
- **Ágil com *k* = 2** salta em pares de palavras comuns (A6, OPR-010). Fora dos critérios; vai para a calibração da 0005.
- **ktlint ou lint diferentes do ambiente de prova.** O código foi formatado e checado com as mesmas versões do catalog. Se aparecer diferença, `ktlintFormat` resolve espaço; qualquer outra mudança vai para o registro.
- **Cópia errada do plano.** Se um teste falhar com o código copiado, compare o arquivo com o bloco do plano antes de mexer em lógica; o conjunto foi visto verde na prova. Depois de 3 falhas no mesmo teste, escale ao `arquiteto` com a saída.
- **Android.** Nada nesta tarefa toca Android; o pacote `align` usa só JDK e Kotlin padrão, disponíveis no Android (minSdk 26), então o código passa ao app sem adaptação.

## Portão automático do plano (checklist do PROCESS.md, conferida pelo arquiteto antes de entregar)

- [x] Cada critério do spec tem passo e comando de verificação: tabela "Critérios do spec → teste → comando" (B1–B6, N1–N8, R1–R6, A1–A15; B6 é leitura do revisor, como o próprio spec define).
- [x] Testes vêm antes do código: Tarefas 1–5 (teste no Passo 1, falha no Passo 2) e Tarefa 6 inteira antes da Tarefa 7.
- [x] Seção de privacidade preenchida, com Q6.
- [x] Nenhuma assinatura de contrato muda: `Token`, `AlignState`, `AlignParams`, `Move` intocados; `Aligner` com as mesmas assinaturas (Tarefa 9, passo 5). A1 é interpretação (justificada acima); A2 é refino do algoritmo sem mudar contrato.
- [x] Nenhuma permissão nova no manifesto: nenhuma dependência nova; `verifyMergedManifest` na verificação.
- Portões humanos: nenhum. As propostas de texto do TRD (A1, A2, A3) ficam registradas em OPR-007 e OPR-009 para o humano aplicar quando quiser; nenhuma delas bloqueia o BUILD.

---

## Registro da construção (preenchido pelo agente de construção)

**Agente:** motor-voz · **Commit(s):** 34e4fb8 (T0), Normalizer, Similarity, NumberWords, Tokenizer, PartialReconciler, testes do Aligner (vermelhos), Aligner (ver `git log main..work/0002-etapa0-align`); este registro e as evidências no commit final.

- Feito: Tarefas 0 a 9 na ordem do plano, código copiado do plano sem alteração de lógica (ktlint passou sem `ktlintFormat`). `align/` sem import `android.*`/`androidx.*`; contratos da 0001 e assinaturas do `Aligner` intocados; TRD e PRD intocados.
- Testes criados: 60 novos, vistos falhar antes do código (erro de compilação nas Tarefas 1 a 5; 21 `NotImplementedError` na Tarefa 6): NormalizerTest 6, SimilarityTest 3, NumberWordsTest 4, TokenizerTest 14, TokenizerTextsTest 2, PartialReconcilerTest 10, AlignerTest 10, AlignerTrajectoryTest 10, AlignerL30Test 1. Total em `:core`: 85, 0 falhas, 0 ignorados.
- Quebras Q2 conferidas (Tarefa 8, sem commit, tudo restaurado; saída em `evidencias/quebras-q2.txt`):
  - Q2-1 -> `AlignerTest.recuosSucessivosNaoPassamDeHighWaterMenos3`
  - Q2-2 -> `recuosSucessivosNaoPassamDeHighWaterMenos3`, `AlignerTrajectoryTest.repeticaoPeloOradorT08_...`
  - Q2-3 -> `AlignerTest.recuperacaoAmbigua_naoMoveOCursor_L30`
  - Q2-4 -> `AlignerTest.unkNuncaCasa_D13`, `AlignerTrajectoryTest.improvisoT04_...`, `AlignerL30Test.l30Sintetico_...`
  - Q2-5 -> `PartialReconcilerTest.reset_...`, `finalComMaisPalavras_...`, `fraseRepetida_...`
  - Q2-6 -> `SimilarityTest.of_unkNuncaCasa_D13`, `AlignerTest.unkNuncaCasa_D13`
  - Q2-7 -> `recuosSucessivosNaoPassamDeHighWaterMenos3`
- Desvios do plano e motivo: nenhum. Observação: a Tarefa 7, passo 3, rodou `:core:ktlintCheck :core:lint` verde sem formatar. Tempo do L30 nesta máquina: 3.399 chamadas de `advance`, total 151,1 ms, máximo 2,401 ms (evidência para a 0005, sem limiar).
- Comandos rodados e resultado (saídas em `evidencias/`): `./gradlew :core:test --rerun-tasks` BUILD SUCCESSFUL (`core-test.txt`); `./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest` BUILD SUCCESSFUL, manifestos debug e release só com `RECORD_AUDIO` (`verificacao.txt`); contagem por classe (`contagem-testes.txt`); greps de `android`/`androidx`, `NotImplementedError` e `@Ignore`/`@Disabled` vazios, exit=1 (`regras-align.txt`); `git diff main` dos quatro contratos com 0 linhas e assinaturas do `Aligner` idênticas (`contratos.txt`). Não revisado nem aprovado por mim: revisão é do `revisor`, teste do `qa-publicacao`.
