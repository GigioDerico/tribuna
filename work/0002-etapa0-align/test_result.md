**Resultado: PASSOU**

# Testes — 0002-etapa0-align

**Responsável:** qa-publicacao · **Data:** 2026-10-11 · **Commit testado:** d0452e5 (branch work/0002-etapa0-align)

## Ambiente

| Item | Valor |
|---|---|
| Aparelho(s) | Nenhum. Tarefa só de Kotlin puro na JVM (MacBook, JDK 21, Gradle 9.7.1). Não há tela nem áudio |
| Ambiente acústico / voz / microfone / brilho | Não se aplica |

## Execução

| Suíte / comando | Resultado | Evidência (em `evidencias/`) |
|---|---|---|
| `./gradlew :core:test --rerun-tasks` | BUILD SUCCESSFUL | `qa-core-test.txt` |
| `./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest --rerun-tasks` | BUILD SUCCESSFUL. `core:runKtlintCheckOverTestSourceSet`, `core:lint`, `core:testDebugUnitTest`, `app:testDebugUnitTest` executaram. O ktlint de `:app` dá NO-SOURCE (OPR-018, já registrado) | `qa-verificacao.txt` |
| Contagem dos XMLs de `:core` | **118 testes, 0 skipped, 0 falhas, 0 erros** (Aligner 16, AlignerBoundary 27, AlignerTrajectory 10, AlignerL30 1, PartialReconciler 10, Tokenizer 14, TokenizerTexts 2, Normalizer 6, NumberWords 4, Similarity 3, mais contratos/Fake) | `qa-contagem.txt`, `qa-testes-nomeados.txt` |
| Manifesto mesclado | debug e release: um único `uses-permission`, `android.permission.RECORD_AUDIO` | `qa-verificacao.txt`, `qa-contagem.txt` |
| `connectedDebugAndroidTest` | Não se aplica (sem aparelho, nada de UI ou nativo mudou) | n/a |

## Critérios de aceite

Todos executados por comando. Os nomes são testes JUnit que rodaram verdes na execução acima.

| Critério | Como foi executado | Resultado | Evidência |
|---|---|---|---|
| B1 `:core:test` verde, ≥ 1 teste por critério | Execução acima, 118 testes; cada critério abaixo tem teste nomeado | APROVADO | `qa-core-test.txt`, `qa-contagem.txt` |
| B2 `testDebugUnitTest lint ktlintCheck verifyMergedManifest` verde; sem `@Ignore`/`@Disabled` | Execução acima; `grep -rnE "@Ignore\|@Disabled" core/src app/src` exit 1 (vazio); 0 skipped nos XMLs | APROVADO | `qa-verificacao.txt`, `qa-greps.txt` |
| B3 sem `android`/`androidx` em `align/` | `grep -rE "import (android\|androidx)\."` exit 1 (vazio) | APROVADO | `qa-greps.txt` |
| B4 contratos intocados | `git diff main -- Token.kt AlignState.kt AlignParams.kt Move.kt \| wc -l` = 0. Em `Aligner.kt` o construtor `(tokens: List<Token>, params: AlignParams)`, `advance(state: AlignState, words: List<String>): AlignState` e `reposition(tokenIndex: Int): AlignState` são iguais aos de `main` (só o corpo mudou) | APROVADO | `qa-greps.txt` |
| B5 sem `NotImplementedError` | `grep -rn NotImplementedError core/src app/src` exit 1 (vazio) | APROVADO | `qa-greps.txt` |
| B6 textos só de `etapa0/textos/` | Já é do revisor, que a aprovou com a exceção R3-3 (roteiro sintético em `AlignerBoundaryTest`, OPR-017). Não reconferi por leitura, só confirmei que não há discurso pessoal nos nomes de teste | N.A. (do revisor) | `code_review.md` |
| N1 Normalizer (minúsculas, sem acento/pontuação, idempotente, roteiro = fala) | `NormalizerTest.normalize_minusculasSemAcentosSemPontuacao_palavrasDoT01`, `…eIdempotenteEmTodosOsTextos`, `…roteiroEFalaDoMesmoJeito`, `…palavrasDoT09ESemPontuacaoSolta` | APROVADO | `qa-testes-nomeados.txt` |
| N2 tokens: index = posição, display bate, nada vem de linha `//` | `TokenizerTextsTest.indexIgualAPosicaoEDisplayBateComUmaForma_todosOsTextos`, `…linhasDeNotaNaoGeramToken_T04eL30` | APROVADO | idem |
| N3 exemplos do TRD (24, 3:16, 13:4-7, 12:1, 2; referência separada do livro) | `TokenizerTest.exemplosDoTrd_24`, `_3_16_umTokenSeparadoDoLivro`, `_13_4a7`, `_12_1e2` | APROVADO | idem |
| N4 números do T02 e L30 (1914, 3,5%, 340, 24:14, 5:3, 4, 1 Coríntios, "Com 1 capítulo") | `TokenizerTest.numerosDoT02`, `numerosDoL30`, `livroComNumero_1Corintios_D17`, `com1Capitulo_numeroProprioComMasculinoEFeminino`; `NumberWordsTest.cardinalForms_grafiasAlternativasEFeminino_D15` | APROVADO | idem |
| N5 intervalo com `–` e "a"; lista de 3 itens | `TokenizerTest.intervaloComTravessaoEComA_igualAoHifen`, `listaDeTresItens` | APROVADO | idem |
| N6 hifenizadas (`bom-dia`, `escrevê-los`) | `TokenizerTest.palavraHifenizada_umTokenComDuasFormas_D18`, `NormalizerTest.normalize_hifenUneAsPartes_D18` | APROVADO | idem |
| N7 peso de `de/que/e/o/a/é` | `TokenizerTest.pesoDasPalavrasComuns_T09` | APROVADO | idem |
| N8 `sentenceStart` (T09: 15; T01: primeiro de cada parágrafo) | `TokenizerTest.inicioDeFrase_T09temQuinzeFrases_D20`, `inicioDeFrase_T01primeiroTokenDeCadaParagrafo` | APROVADO | idem |
| R1 parciais que só crescem | `PartialReconcilerTest.parciaisQueSoCrescem_entregamSoOPrefixoEstavelETotalIgualAoFinal` | APROVADO | idem |
| R2 inserção/remoção/correção no começo | `insercaoNoComecoDaFrase_…`, `remocaoNoComecoDaFrase_…`, `correcaoNoComecoDaFrase_naoRepeteNemRetira` | APROVADO | idem |
| R3 Final diferente do último parcial | `finalComMaisPalavras_entregaOQueFaltaEZera`, `finalComMenosPalavras_naoRetiraNada`, `finalComPalavraTrocada_entregaSoODepois` | APROVADO | idem |
| R4 frase repetida (T08) | `fraseRepetida_mesmoFinalDuasVezesEntregaNasDuas_T08` | APROVADO | idem |
| R5 parcial vazio / primeiro parcial | `parcialVazioEPrimeiroParcial_naoEntregamNada` | APROVADO | idem |
| R6 sessão nova zera estado | `reset_sessaoNovaNaoComparaComHipotesesAnteriores` | APROVADO | idem |
| A1 leitura normal T01, 3 presets | `AlignerTrajectoryTest.leituraNormalT01_tresPresets_cursorIgualAoEsperadoDesdeAKesimaPalavra` | APROVADO | idem |
| A2 palavras erradas (1 em 5) | `palavrasErradasT01_umaEmCincoTrocada` | APROVADO | idem |
| A3 pulo de parágrafo (T05) | `puloDeParagrafoT05_recuperaAte6Palavras` | APROVADO | idem |
| A4 salto longo na janela | `saltoLongoDentroDaJanelaT01_pularUmaFrase` | APROVADO | idem |
| A5 improviso (T04) | `improvisoT04_cursorParadoERetomaAte6Palavras` | APROVADO | idem |
| A6 repetição pelo orador (T08) | `repeticaoPeloOradorT08_semSaltoErradoNemRecuoAlemDoLimite` | APROVADO | idem |
| A7 trechos repetidos (T03) | `trechosRepetidosT03_nuncaPulaParaOutraCopia` | APROVADO | idem |
| A8 palavras comuns (T09) | `palavrasComunsT09` | APROVADO | idem |
| A9 números e variantes (T02, 3 takes) | `numerosEVariantesT02_tresTakes` | APROVADO | idem |
| A10 invariante `highWater − 3` | `AlignerTest.recuosSucessivosNaoPassamDeHighWaterMenos3` (vermelho com Q2-1 e Q2-7, ver abaixo) | APROVADO | `qa-quebras-q2.txt` |
| A11 recuperação ambígua | `recuperacaoAmbigua_naoMoveOCursor_L30`, `recuperacao_ambiguidadeContaAntesDoFiltroA2` (vermelhos com Q2-3 e V6) | APROVADO | idem |
| A12 acentos/maiúsculas, `[unk]` | `falaComAcentosEMaiusculas_mesmaTrajetoriaQueANormalizada_T03`, `falaComAcentos_referenciasDoT02_…`, `unkNuncaCasa_D13`, `SimilarityTest.of_unkNuncaCasa_D13` | APROVADO | `qa-testes-nomeados.txt` |
| A13 `reposition` (D4) | `reposition_segueD4`, `reposition_primeiroAvancoPartedaNovaPosicao`, `reposition_voltaLivrementeAbaixoDeHighWaterMenos3` | APROVADO | idem |
| A14 cobertura de `Move` | `AlignerTrajectoryTest.cadaMoveApareceComoLastMove` | APROVADO | idem |
| A15 L30 sintético | `AlignerL30Test.l30Sintetico_semSaltoErrado_atrasoAte3Em95PorCento_chegaAoFim`: 3.399 chamadas de `advance` (regra de contagem do próprio teste: > 3.000 amostras de leitura normal exigidas) | APROVADO | `qa-l30-tempo.txt` |

Q3 e Q7: a contagem de amostras vem do próprio teste (ele falha se ficar abaixo do mínimo: leitura normal ≥ tokens − k movimentos; L30 > 3.000 amostras; T09 ≥ 50; T01 com 43 trocas). Não conferi o valor de cada mínimo por leitura do código; conferi só que os testes passam e que nas quebras eles ficam vermelhos, o que mostra que não aprovam no vazio.

## Tempo do L30 (evidência para a 0005, sem limiar, D23)

`L30 sintético: 3399 chamadas de advance, total 380,7 ms, máximo 20,367 ms` (esta execução, JVM do computador, com JIT frio). As execuções do revisor deram 267,0 ms / 25,5 ms e a do construtor 151,1 ms / 2,4 ms. O máximo varia cerca de 10x entre execuções. Não é medida de aparelho nem estável (OPR-013): a 0005 precisa medir no aparelho com aquecimento e repetições.

## Teste falso (Q2)

Feitas por mim, uma por vez, com troca exata de texto em `Aligner.kt` e `git checkout --` depois de cada uma; script em scratchpad, fora do repositório. Saída: `qa-quebras-q2.txt`. Depois da última: `:core:test --rerun-tasks` verde de novo e `git status --short etapa0/` vazio.

| Quebra | Vermelhos |
|---|---|
| Q2-1 limite contra o cursor, sem `highWater − 3` | 1: `recuosSucessivosNaoPassamDeHighWaterMenos3` |
| Q2-3 ambíguo salta (`firstOrNull`) | 2: `recuperacao_ambiguidadeContaAntesDoFiltroA2`, `recuperacaoAmbigua_naoMoveOCursor_L30` |
| Q2-7 `highWater = best.position` | 3 |
| V4 candidato único sem filtro A2 | 2 |
| V6 ambiguidade depois do A2 | 1 |
| V10 recuperação no 1º miss | 1: `recuperacao_soNoSegundoMiss_D7` |
| W4 recuperação com `k` palavras | 1: `recuperacao_exigeQuatroPalavrasOuvidas_D12` |
| L129 recuperação inclui `highWater` | 1: `recuperacao_naoOlhaOTokenEmHighWater` |
| L104b janela até `cursor + W + 1` | 1: `janela_terminaExatamenteEmCursorMaisW` |
| L132b recuperação pelo limiar curto | 4 |
| L40c distância curta ignorada | 19 |
| L59a recuperação sem subir `highWater` | 2 |
| L56b recuperação parte do cursor | 1 |
| Y4 fim da janela em `highWater + W` (do revisor, achado R3-1) | **VERDE, 118/118** (confirmado, não bloqueia; OPR-015) |

13 de 13 quebras esperadas ficaram vermelhas; Y4 ficou verde como o revisor declarou. Isso conta como lacuna de teste já registrada, não como falha da tarefa.

## Não testado

- Aparelho, microfone e Vosk: fora do escopo (0003 e 0005). Nenhum número daqui vale como desempenho de campo.
- Golden de transcrições reais (OPR-008) e orçamento de tempo do `advance` (sem meta).
- Não rodei as 109 quebras do construtor nem as 12 do revisor além das que coincidem com a lista acima. As 13 acima são amostra minha.
- Achados abertos que seguem do revisor, não bloqueantes: R3-1 (Y4, OPR-015), R3-2 (guarda de 4 palavras conta palavras normalizadas, OPR-016), R3-3 (B6, OPR-017), OPR-011, 013, 014, 018.
