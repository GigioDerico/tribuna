**Veredito: APROVADO**

# Revisão — 0002-etapa0-align (rodada 3)

**Revisor:** revisor · **Data:** 2026-10-11 · **Base do diff:** 691e03c (rodada 2) → 3d77a0d (`work/0002-etapa0-align`)

Os dois achados da rodada 2 que voltaram para retrabalho estão resolvidos. Reapliquei o W4 (`heard.size >= RECOVERY_WORDS` → `params.k`) e ele agora fica vermelho em `AlignerTest.recuperacao_exigeQuatroPalavrasOuvidas_D12`. O KDoc de `Copies` está corrigido (R2-3). O diff tem só testes, evidências, o plano e a memória do construtor. `src/main`, `*.kts` e `gradle/` não mudaram. Reapliquei 12 quebras da tabela do construtor, e as 12 ficaram vermelhas. Também fiz 6 variantes minhas. Quatro ficaram vermelhas e duas verdes (Y1 e Y4). Nenhuma das duas verdes toca a regra 6 nem as guardas da recuperação, então vão para o registro como IMPORTANTE e MENOR, sem bloquear. Os 13 mutantes declarados equivalentes são equivalentes de fato. Gradle verde.

## Verificações pedidas pelo orquestrador

| Item | Comando | Resultado |
|---|---|---|
| (a) produção intocada | `git diff --stat 691e03c..HEAD -- etapa0/core/src/main app/src/main '*.kts' gradle/` → vazio. `git diff --stat 691e03c..HEAD`: `AlignerBoundaryTest.kt` (novo, 378), `AlignerTest.kt` (+25/−2), `evidencias/quebras-q2-rodada3.txt`, `evidencias/verificacao-rodada3.txt`, `impl_plan.md`, memória do motor-voz | OK |
| (b) W4, amostra da tabela e variantes minhas | Script em scratchpad: troca exata de texto (exige 1 ocorrência), `./gradlew :core:test --rerun-tasks -q`, leitura dos XMLs, `git checkout --` depois de cada quebra. No fim, `git status --short` vazio. Tabela abaixo | 12/12 da tabela vermelhas; 4/6 minhas vermelhas, 2 verdes (achados R3-1 e R3-2) |
| (b) 13 equivalentes | Leitura de cada um contra `Aligner.kt` (abaixo) | Todas as justificativas são válidas |
| (c) `AlignerBoundaryTest` | Leitura completa (378 linhas) | Não é tautológico e não tem texto de discurso. Desvio literal de B6 (achado R3-3, MENOR) |
| (d) Gradle | `./gradlew :core:test testDebugUnitTest lint ktlintCheck verifyMergedManifest --rerun-tasks` → BUILD SUCCESSFUL. XMLs de `:core`: **118 testes, 0 skipped, 0 falhas, 0 erros** (AlignerBoundaryTest 27, AlignerTest 16). `verifyDebugMergedManifest: OK … -> android.permission.RECORD_AUDIO` e o mesmo no release. Os dois manifestos mesclados têm um único `uses-permission`, `RECORD_AUDIO`. `@Ignore\|@Disabled` em `core/src/test` → 0. Imports `android`/`androidx` em `align/` → 0. `core:runKtlintCheckOverTestSourceSet` executou, então os testes novos passam pelo ktlint | OK. Observação à parte: o ktlint de `:app` dá `NO-SOURCE` em todos os source sets, inclusive para `app/src/test/kotlin/…/CoreFromAppTest.kt`. Já era assim antes deste diff (OPR-018) |
| (e) empates pinados | spec 0002 l. 24 (D9): "no empate, a posição mais próxima do cursor". Não diz nada sobre empate de distância nem de forma falada. TRD: sem regra de desempate. OPR-011 já registra o empate de distância | Nenhum dos dois contradiz o spec ou o TRD. É decisão do arquiteto. Aceito como OPR-011, ampliado para incluir o empate de formas |

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `verifyMergedManifest --rerun-tasks` e leitura dos manifestos mesclados (item d) | OK |
| Texto não persiste nem vai para log | O diff só tem testes, sem I/O, `println` ou log novo. A produção é a mesma da rodada 1 | OK |
| Áudio não gravado fora de `etapa0/` | Nenhum arquivo fora de `etapa0/`, `work/` e `.claude/agent-memory/` no diff | OK |
| `align/` sem Android | grep vazio. `AlignerBoundaryTest` importa só JUnit e `java.util.Random` | OK |
| Contratos do TRD inalterados | `src/main` com 0 linhas de diff | OK |
| `SessionId` preso ao reconhecedor | `align/` não lida com sessão | N.A. |
| Cursor nunca abaixo de `highWater − 3` | L103a, K2 e L129 vermelhas. A variante Y2 (recuperação olhando a partir de `highWater − 3`) também ficou vermelha | OK |
| Só Compose | Nada de UI no diff | N.A. |

## Testes (Q2 e Q3)

Cada quebra foi aplicada sozinha, com troca exata de texto, e restaurada com `git checkout` (`git status --short` vazio no fim).

| Quebra | Troca | Resultado |
|---|---|---|
| **W4 / L55b** | `heard.size >= RECOVERY_WORDS)` → `heard.size >= params.k)` | VERMELHO 1: `AlignerTest.recuperacao_exigeQuatroPalavrasOuvidas_D12` (**R2-1 resolvido**) |
| L55a | `misses >= MISSES_FOR_RECOVERY &&` → `misses >` | VERMELHO 8 |
| L55d | `misses >= MISSES_FOR_RECOVERY && heard` → `\|\| heard` | VERMELHO 2: `…exigeQuatroPalavrasOuvidas_D12`, `…soNoSegundoMiss_D7` |
| L56a | `heard.takeLast(RECOVERY_WORDS)` → `takeLast(params.k)` | VERMELHO 7 |
| L129 | recuperação a partir de `highWater` (inclusive) | VERMELHO 1: `recuperacao_naoOlhaOTokenEmHighWater` |
| L103a | janela sem o termo `highWater − 3` | VERMELHO 1: `recuosSucessivosNaoPassamDeHighWaterMenos3` |
| L104b | janela até `cursor + W + 1` | VERMELHO 1: `janela_terminaExatamenteEmCursorMaisW` |
| L132b | candidato da recuperação pelo limiar **curto** | VERMELHO 4 (inclusive o L30 e o improviso do T04) |
| L134a | `singleOrNull` → `firstOrNull` (ambígua salta) | VERMELHO 2 |
| L40c | `<= SHORT_DISTANCE_TOKENS && best` → `\|\| best` | VERMELHO 19 |
| L59a | recuperação sem subir `highWater` | VERMELHO 2 |
| K2 | `MAX_BACK_FROM_HIGH_WATER = 4` | VERMELHO 4 |
| Y1 (minha) | `heard.size >= RECOVERY_WORDS)` → `words.size >= RECOVERY_WORDS)` (conta itens brutos da cauda, não palavras normalizadas) | **VERDE** (achado R3-2) |
| Y2 (minha) | recuperação a partir de `maxOf(0, highWater − 3)` | VERMELHO 2 |
| Y3 (minha) | `heard.size >= RECOVERY_WORDS − 1` | VERMELHO 1: `…exigeQuatroPalavrasOuvidas_D12` |
| Y4 (minha) | fim da janela `minOf(state.highWater + params.window, …)` em vez de `state.cursor + params.window` | **VERDE** (achado R3-1) |
| Y5 (minha) | salto longo decidido por `best.lastWord >= longThreshold` em vez da média | VERMELHO 15 |
| Y6 (minha) | candidato da recuperação por `match.lastWord >= longThreshold` | VERMELHO 4 |

**Os 13 equivalentes declarados.** Conferi cada justificativa contra o código:
- L87b e L95: as palavras a mais são cortadas pelo `takeLast(needed)`, ou pelos `takeLast(k)` e `takeLast(RECOVERY_WORDS)` de quem consome. Quando sobra palavra, `heard.size` já passou dos dois limites. Válido.
- L103b: com `highWater ≥ cursor` (vale para todo estado que `advance` ou `reposition` produz), o termo `cursor − 3` nunca decide. L103c: posição negativa dá similaridade 0, e no desempate qualquer posição válida está mais perto do cursor. Válido.
- L109: `false ||` não muda nada. Não chega a ser um mutante, é ruído na tabela. L110a e L161a: só diferem com diferença exata de 1e-9. Válido.
- L119: a janela nunca fica vazia, porque `from ≤ cursor ≤ to` vale sob o mesmo invariante. Válido.
- L148: com média 0 e pesos > 0, a parcela da palavra mais nova (`last`) também é 0. Token sem forma nunca chega ao ramo terminal. Válido.
- L160a e L160b: com k ≥ 1 e peso > 0, `total > 0` no ramo terminal. Válido.
- L176a: `walk` só desce a partir de `position`. L181: o `last` inicial é sobrescrito na primeira forma de `position`, que é o único jeito de chegar ao terminal com `position ≥ 0`. Válido.

**Leitura dos testes novos (item c, Q3).**
- `recuperacao_exigeQuatroPalavrasOuvidas_D12` testa Ágil e Normal, com cauda de 3 palavras e `missStreak = 1`. Confere o `AlignState` inteiro em dois passos: o contador sobe e cursor e highWater ficam parados. W4 e Y3 vermelhos mostram que o caso exercita a guarda. Ele não passa por falta do que checar.
- `AlignerBoundaryTest`: cada teste confere o `AlignState` inteiro ou o par cursor/missStreak/lastMove com valores fixos. Os limiares vêm de `Similarity.of` sobre o próprio par e são testados dos dois lados com `Math.nextUp`, então o teste prende `>=` contra `>`, e não o valor da similaridade. Nos casos da recuperação, `recoveryScore` usa limite longo 2,0 para ler a similaridade sem saltar. `recuperacao_saltaParaOTokenLogoDepoisDeHighWater` exige `score ≥ limite longo` antes de afirmar o salto, o que prova que o caso exercita a guarda. Nenhum teste compara o código consigo mesmo. As poucas comparações com `result.score` aparecem só onde o score não é o que se afirma, e os outros campos são fixos.
- Texto: palavras de 10 letras sorteadas com semente fixa e algumas palavras soltas ("vinte", "e", "um", "dois", "duas", "repetida", "qualquer", "unk"). Não há discurso nem frase de pessoa, e não há risco de privacidade. Não vêm de `etapa0/textos/` (achado R3-3).

**Empates (item e).** `empateAMesmaDistancia_ficaComOMenorIndice` prende o recuo no empate de distância (cursor − 2 contra cursor + 2). Fica dentro da janela, então respeita `highWater − 3`. É exatamente o comportamento descrito no OPR-011. `formasEmpatadas_valeAPrimeira` prende a primeira forma falada quando as médias empatam. O spec (D9) e o TRD só definem o desempate entre posições pela distância. Nenhum dos dois testes contradiz o que está escrito. Se o arquiteto decidir outra regra, os dois testes mudam junto com o código. Ampliei o OPR-011 para cobrir o empate de formas.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| R3-1 | IMPORTANTE (não bloqueia) | `Aligner.kt:104` (`minOf(state.cursor + params.window, …)`); testes em `AlignerBoundaryTest.janela_terminaExatamenteEmCursorMaisW` | O fim da janela (TRD §Alinhamento 2: `cursor + W`) só é testado com `cursor == highWater`. Trocar a base por `highWater` passa verde. Depois de um recuo curto (`cursor < highWater`), essa troca deixa o salto longo alcançar até 3 tokens além de `cursor + W`. Não bloqueia: o salto continua exigindo o limite longo e a palavra mais nova (A2), não fere `highWater − 3` e não mexe nas guardas da recuperação. Mas é um operando de uma guarda do TRD sem trava | Y4: `:core:test --rerun-tasks` exit=0, 118/118. Exemplo não equivalente por leitura: `cursor = 27`, `highWater = 30`, `W = 10`, fala `word(40)` com k = 1. O código atual não salta (janela 27..37); com Y4, LongJump para 40 | Um teste em `AlignerBoundaryTest` com `cursor < highWater` e palavra exata em `cursor + W + 1..highWater + W` → `Stay`, miss. **OPR-015**, motor-voz |
| R3-2 | MENOR | `Aligner.kt:55` | A guarda "4 palavras" conta palavras normalizadas (`heard`). Contar itens brutos da cauda (`words.size`) passa verde. Os dois só divergem quando um item normaliza para nenhuma palavra (então há 4 itens e 3 palavras, e a recuperação dispara com 3) ou quando um item tem várias palavras (caso em que a troca é mais conservadora). Com o Vosk, item vazio é improvável | Y1 exit=0 | Um teste com cauda de 4 itens em que um é só pontuação (ex.: `"—"`) e 3 palavras que casam com posição única fora da janela → `Stay`. **OPR-016**, motor-voz |
| R3-3 | MENOR | `AlignerBoundaryTest.kt:8-30` e testes de formas (l. 170, 200, 219, 227, 235) | O critério B6 do plano diz "textos de teste só de `etapa0/textos/` ou cópia literal". O roteiro é sintético (letras sorteadas) e há palavras soltas fora dos textos. Não há risco de privacidade, e é o tipo de montagem que torna os limites exatos possíveis. Mesmo assim, é um desvio do texto do critério e não está registrado como exceção no spec | Leitura | Registrar no spec 0002 (B6) a exceção para roteiros sintéticos de palavras sem sentido em testes de limite. **OPR-017**, analista |

Achados da rodada 2: R2-1 resolvido (W4 vermelho) e R2-3 resolvido (KDoc). R2-2 continua no OPR-014 (aberto, motor-voz), como previsto. OPR-011 foi ampliado com o empate de formas. OPR-013 continua aberto.

## Não verificado

- Não rodei as 109 quebras da tabela do construtor. Rodei uma amostra de 12, escolhida pelo maior risco (guardas da recuperação, `highWater − 3`, janela e limiares), e as 12 batem com a tabela.
- `connectedDebugAndroidTest` e `verify16kAlignment` não se aplicam, porque só mudaram testes JVM.

---

# Histórico — rodada 2

**Veredito (rodada 2): REPROVADO**

## Revisão — 0002-etapa0-align (rodada 2)

**Revisor:** revisor · **Data:** 2026-10-11 · **Base do diff:** 599759a (rodada 1) → 5b51e0f (`work/0002-etapa0-align`)

Os cinco testes novos fazem o que prometem: V4, V6, V7, V8 e V10 ficaram vermelhos na minha execução, e as variantes W1, W2, W3 e W5 também. Os achados 1 e 2 da rodada 1 estão resolvidos. Mesmo assim, a tarefa volta: a cláusula que fica ao lado da V10, no mesmo `if`, "recuperação só com as últimas 4 palavras" (`Aligner.kt:55`, `heard.size >= RECOVERY_WORDS`), continua sem teste. Quebrei essa cláusula (W4): as 90 suítes passaram verdes. Com o preset Ágil, rodei o caso e ele faz uma recuperação de **3.087 tokens** com 3 palavras. Esta cláusula escapou na rodada 1 por falha minha, mas ela é da mesma classe que a V10 (constante fixa da recuperação, D12), que reprovei como BLOQUEANTE. A correção é um teste, sem mudar código.

## Verificações pedidas pelo orquestrador

| Item | Comando | Resultado |
|---|---|---|
| (a) `src/main` intocado desde a rodada 1 | `git diff 599759a..HEAD -- etapa0/core/src/main \| wc -l` → `0`; e também `etapa0/app/src/main`, `*.kts`, `gradle/` → `0`. `git diff --stat`: só `AlignerTest.kt`, `NormalizerTest.kt`, `opr_register.md`, `impl_plan.md` e evidências | OK |
| (b) V4, V6, V7, V8, V10 e variantes | Script em scratchpad: troca exata de texto, `./gradlew :core:test --rerun-tasks`, leitura dos XMLs, `git checkout --`. Tabela abaixo. No fim, `git status --short` limpo | Os 5 vermelhos; W4 verde (achado R2-1) |
| (c) testes novos não tautológicos e com trechos de `etapa0/textos/` | Leitura do diff (abaixo) | OK, com observações |
| (d) Gradle verde | `./gradlew :core:test --rerun-tasks` → BUILD SUCCESSFUL, 90 testes, 0 skipped, 0 falhas (XMLs). `./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest --rerun-tasks` → BUILD SUCCESSFUL; `verifyDebugMergedManifest: OK … -> android.permission.RECORD_AUDIO` e o mesmo no release; os dois `AndroidManifest.xml` mesclados têm um único `uses-permission`, `RECORD_AUDIO`. `@Ignore\|@Disabled` em `core/src/test` → 0; imports `android`/`androidx` em `align/` → 0 | OK |
| (e) OPR-012 | `git diff`: linha `"d'água"` removida e teste renomeado para `normalize_hifenUneAsPartes_D18`. `grep "'" etapa0/textos/*.txt`: a única ocorrência é aspa numa nota `//` do T05, não um apóstrofo dentro de palavra. A afirmação "os textos não têm apóstrofo" confere | Resolvido no que pedia. Efeito colateral: a junção por apóstrofo ficou sem teste (W6 verde; achado R2-2) |

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `verifyMergedManifest --rerun-tasks` e leitura dos manifestos mesclados (item d) | OK |
| Texto não persiste nem vai para log | Diff só de testes, sem I/O nem log novo; produção igual à da rodada 1 | OK |
| Áudio não gravado fora de `etapa0/` | Nenhum arquivo fora de `etapa0/`, `work/` e `registers/` no diff | OK |
| `align/` sem Android | grep vazio (item d) | OK |
| Contratos do TRD inalterados | `src/main` com 0 linhas de diff | OK |
| `SessionId` preso ao reconhecedor | `align/` não lida com sessão | N.A. |
| Cursor nunca abaixo de `highWater − 3` | Produção igual à da rodada 1. R-Q2-1/V1/V2/V3 continuam cobertos pelos mesmos testes | OK |
| Só Compose | Nada de UI no diff | N.A. |

## Testes (Q2 e Q3)

| Quebra | Troca em `Aligner.kt` (ou `Normalizer.kt`) | Vermelhos |
|---|---|---|
| V4 | `candidates.singleOrNull()?.takeIf { … }` → `candidates.singleOrNull()` | `recuperacao_candidatoUnicoComPalavraNovaSemCasarNaoSalta_A2` |
| V6 | ambiguidade contada depois do filtro A2 (`&& match.lastWord >= params.shortThreshold` na linha 132) | `recuperacao_ambiguidadeContaAntesDoFiltroA2` |
| V7 | `abs(match.similarity - current.similarity) <= EPSILON &&` → `false &&` | `empate_vaiParaAPosicaoMaisProximaDoCursor_D9` |
| V8 | `Stay` no cursor sem `missStreak = 0` | `vencedorNoCursor_zeraMissStreak_D6` |
| V10 | `misses >= MISSES_FOR_RECOVERY` → `misses >= 1` | `recuperacao_soNoSegundoMiss_D7` |
| W1 (minha) | ambíguo → maior similaridade: `candidates.maxByOrNull { it.similarity }?.takeIf` | `recuperacao_ambiguidadeContaAntesDoFiltroA2`, `recuperacaoAmbigua_naoMoveOCursor_L30` |
| W2 (minha) | A2 da recuperação testando `it.similarity` em vez de `it.lastWord` | `recuperacao_candidatoUnicoComPalavraNovaSemCasarNaoSalta_A2` |
| W3 (minha) | `misses >= MISSES_FOR_RECOVERY - 1` | `recuperacao_soNoSegundoMiss_D7` |
| **W4 (minha)** | `heard.size >= RECOVERY_WORDS)` → `heard.size >= params.k)` | **nenhum (exit=0, verde)** |
| W5 (minha) | `if (best.position == state.cursor)` → `if (false)` (Stay no cursor vira Short) | `vencedorNoCursor_zeraMissStreak_D6` |
| **W6 (minha)** | `Normalizer.kt:13`, joiner `[-'’]` → `[-]` | **nenhum (exit=0, verde)** |

**W4 executado fora da suíte.** Usei `jshell` com as classes compiladas de `:core` e o `L30.txt`, preset Ágil (k = 2). Fiz `reposition(0)`, depois `advance` com `["livro","inteiro"]` (miss 1) e depois com `["livro","inteiro","de"]` (miss 2, 3 palavras). Código atual: `AlignState(cursor=0, highWater=0, missStreak=2, lastMove=Stay, …)`. Com W4: `AlignState(cursor=3087, highWater=3087, missStreak=0, lastMove=Recovery, score=1.0)`. Restaurei com `git checkout` e recompilei, e o `git status` ficou limpo. O caminho é alcançável pelo contrato A1 (cauda cresce uma palavra por chamada). No Ágil, o 2º miss chega com 3 palavras na cauda. No Normal, chega se quem chama repetir a mesma cauda de 3, como o próprio `recuperacao_soNoSegundoMiss_D7` faz.

**Testes novos (item c).**
- Nenhum é tautológico. Cada um confere o `AlignState` inteiro com valores fixos (cursor, highWater, missStreak, lastMove). Só o `score` vem do próprio resultado, e onde isso importa há um `assertTrue(score ≥ limite longo)` que garante que o caso exercita mesmo a guarda (Q3). O D7 confere `Recovery`, cursor, highWater e `missStreak = 0` no 2º passo.
- Trechos: "no fim do ano a" sai do `L30.txt` via `TestTexts` (l. 27 e 71, separado por pontuação), e a frase do D6 sai do T01 (l. 3). Nos testes da recuperação, a forma falada de 1 ou 2 tokens da cópia é trocada ("o", "xx"). No D9, "no fim do ano" (trecho literal do L30) aparece repetido 4 vezes num roteiro montado no teste. São as montagens a partir de trechos que sugeri na rodada 1 (achado 1, "o que resolve"). Não há texto de fora do conjunto e não há risco de privacidade. Aceito.
- O cálculo do caso (a) da rodada 1 foi confirmado por execução: a 2ª cópia "fim do ano o" dá 0,857 e conta como 2º candidato.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| R2-1 | BLOQUEANTE | `Aligner.kt:55` (`heard.size >= RECOVERY_WORDS`); testes em `AlignerTest.kt` | A guarda "recuperação só com as últimas 4 palavras" (spec 0002, "Recuperação" e "Constantes fixas … 4 palavras na recuperação (D12)"; TRD §Alinhamento 4, "últimas 4 ou mais palavras") não tem teste. Relaxar a guarda para `params.k` deixa o salto longo disparar com 3 palavras no Ágil, ou com 3 no Normal em chamada repetida. É a mesma classe da V10, reprovada na rodada 1. Escapou na rodada 1 por falha minha de cobertura das quebras, não do construtor. | W4: `:core:test --rerun-tasks` exit=0, 90/90 verdes. `jshell`: recuperação para o token 3.087 com 3 palavras (acima) | Um teste que fique vermelho com W4. Exemplo: estado com `missStreak = 1` e cauda de **3** palavras que casam ≥ limite longo com uma única posição fora da janela → `Stay`, cursor e highWater parados, `missStreak = 2`. Pode ser no Ágil, com o caso "livro inteiro de" do L30, ou no Normal, com as cópias do `l30Copies` e a fala `["do","ano","a"]` (conferir o cálculo). Registrar W4 na tabela de quebras do `impl_plan.md` para o QA. |
| R2-2 | MENOR | `NormalizerTest.kt:33-36`; `Normalizer.kt:8,13` | Ao resolver o OPR-012, a junção por apóstrofo (D18, documentada no KDoc do `Normalizer` e no regex) perdeu o único teste: tirar `'’` do joiner deixa tudo verde (W6). O spec só exige o hífen (critério N6), então isso não bloqueia. Mas um roteiro colado pelo usuário com "d'água" ou "pra'" passa a ter comportamento sem trava. O KDoc continua citando "d'água". | W6 exit=0 | Registrar a exceção de B6 no spec e testar o apóstrofo com uma palavra montada no teste, ou decidir que o apóstrofo não une e tirar do regex e do KDoc. Registrado como OPR-014. |
| R2-3 | MENOR | `AlignerTest.kt` (KDoc de `Copies`) | Frase truncada no KDoc: "com edições aplicadas a tokens / indicado". Só legibilidade. | Leitura | Corrigir junto com o R2-1. Sem OPR, porque vai no mesmo retrabalho. |

Achados da rodada 1: o 1 (V4, V6, V10) e o 2 (D6, D9) estão resolvidos, e o 4 (OPR-012) também, com o efeito colateral do R2-2. O 3 (OPR-011) e o 5 (OPR-013) continuam abertos no registro, como previsto.

## Não verificado

- O W4 com o preset Normal não foi executado por `jshell`, só pela suíte (verde). O caso executado foi no Ágil.
- `connectedDebugAndroidTest` e `verify16kAlignment` não se aplicam: só testes JVM mudaram.

---

# Histórico — rodada 1

**Veredito (rodada 1): REPROVADO**

## Revisão — 0002-etapa0-align (rodada 1)

**Revisor:** revisor · **Data:** 2026-10-11 · **Base do diff:** 641a73c (merge-base com `main`) → 436aa37 (`work/0002-etapa0-align`)

Motivo único da reprovação: as guardas da **recuperação** (o movimento mais arriscado do alinhador, que salta para qualquer ponto à frente, fora da janela) não estão travadas por teste. Três quebras que deixam a recuperação mais agressiva, contra o TRD §Alinhamento 4, o spec e a decisão A2, passam com as 85 suítes verdes (achado 1). O código em si está correto e as regras inegociáveis estão de pé. A correção é só de teste.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `./gradlew :app:verifyMergedManifest --rerun-tasks` → `verifyDebugMergedManifest: OK ... -> android.permission.RECORD_AUDIO` e o mesmo para release. Abri os dois `AndroidManifest.xml` em `app/build/intermediates/merged_manifest/{debug,release}/` (regerados às 00:11 de 2026-10-11): um único `uses-permission`, `RECORD_AUDIO`. Nenhuma dependência nova no diff. | OK |
| Texto não persiste nem vai para log | `grep -rnE "println\|print\(\|Log\.\|File\|Stream\|Writer\|System\.(out\|err)\|error\(\|throw\|require\|check\("` no `align/` de produção: só `NumberWords.kt:47`, `require` com mensagem fixa, sem o número. Leitura dos 9 arquivos de `align/`: nenhum I/O, nenhum `SavedStateHandle`/DataStore. Nos testes, o único `println` (`AlignerL30Test.kt:65`) imprime tempos. | OK |
| Áudio não gravado fora de `etapa0/` | `align/` não recebe áudio; nenhum arquivo fora de `etapa0/`, `work/`, `registers/` e memórias de agentes no diff (`git diff main...HEAD --stat`). | OK |
| `align/` sem Android | `grep -rnE "import (android\|androidx)\." .../etapa0/align` → vazio, exit=1. Imports de produção: só `java.util.Locale`, `java.text.Normalizer`, `kotlin.math`. | OK |
| Contratos do TRD inalterados (ou CIA aprovada) | `git diff main -- Token.kt AlignState.kt AlignParams.kt Move.kt \| wc -l` → 0. `git diff main...HEAD -- Aligner.kt`: só corpo, imports e KDoc; construtor, `advance(state: AlignState, words: List<String>): AlignState` e `reposition(tokenIndex: Int): AlignState` idênticos. A1 (cauda em `words`) é interpretação documentada e registrada em OPR-007; concordo que não fixa nada que o TRD fixava. | OK |
| `SessionId` preso ao reconhecedor | `align/` não lida com sessão; o passo 7 fica com quem chama, documentado em `PartialReconciler.kt:5-6`. | N.A. |
| Cursor nunca abaixo de `highWater − 3` | Leitura: janela `from = maxOf(cursor − 3, highWater − 3, 0)` (`Aligner.kt:103`); recuperação só em `highWater + 1..lastIndex` (`:129`); `highWater` só cresce em movimento automático (`:47`, `:59`); `highWater ≥ cursor` sempre (todo caminho que muda o cursor iguala ou sobe `highWater`). Quebras R-Q2-1, V1, V2 e V3 abaixo ficaram vermelhas. | OK |
| Só Compose | Nenhum arquivo de UI ou layout no diff. | N.A. |

**Guarda A2 (pergunta específica do orquestrador).** A2 só acrescenta condições para mover: na janela (`Aligner.kt:39`) e no candidato único da recuperação (`:134`); a ambiguidade é contada antes do filtro (`:132`). Não há caminho em que A2 crie movimento que o algoritmo do TRD não criaria, então não viola `highWater − 3` nem "na dúvida, o cursor fica parado". Ela também transforma um `Stay` no cursor sem casar a palavra nova em miss (antes zerava `missStreak`), o que aproxima a recuperação. Isso é coerente com o TRD ("resultados seguidos sem avanço"), e a recuperação continua com as próprias guardas. A metade da janela está travada (V5 vermelho). As duas cláusulas da metade da recuperação **não** estão (achado 1).

## Spec e plano

| Critério de aceite | Implementação | Teste |
|---|---|---|
| B1 `:core:test` verde, ≥ 1 teste por critério | — | `./gradlew :core:test --rerun-tasks`: BUILD SUCCESSFUL; 85 testes, 0 falhas, 0 skipped (60 novos: Normalizer 6, Similarity 3, NumberWords 4, Tokenizer 14, TokenizerTexts 2, PartialReconciler 10, Aligner 10, AlignerTrajectory 10, AlignerL30 1) |
| B2 `testDebugUnitTest lint ktlintCheck verifyMergedManifest`; sem `@Ignore` | — | `--rerun-tasks`: BUILD SUCCESSFUL; `grep -rnE "@Ignore\|@Disabled" etapa0/core/src/test` → vazio |
| B3 sem Android em `align/` | — | grep vazio |
| B4 contratos intocados | — | `git diff` 0 linhas; assinaturas iguais |
| B5 sem `NotImplementedError` | — | grep vazio |
| B6 textos só de `etapa0/textos/` | `TestTexts` lê por propriedade do Gradle (`core/build.gradle.kts`, declarada como entrada) | Conferi com `grep -F` cada frase literal dos testes contra `etapa0/textos/`: todas existem (T01/T02/T03/T08/T09/L30), inclusive "no fim do ano a" (L30 l. 27 e 71, separado por pontuação). As variações de referência são as permitidas pelo spec. Exceção menor: `"d'água"` (achado 4) |
| N1–N8 Normalizer e Tokenizer | `Normalizer.kt`, `NumberWords.kt`, `Tokenizer.kt`, `AlignConstants.kt` | `NormalizerTest`, `NumberWordsTest`, `TokenizerTest`, `TokenizerTextsTest`. V15, V16 e V17 vermelhos |
| R1–R6 PartialReconciler (inclui frase repetida e sessão nova) | `PartialReconciler.kt` | `PartialReconcilerTest`. V11–V14 vermelhos; frase repetida (V12) e reset/sessão nova (V13, e Q2-5 do construtor) travados |
| A1–A9 trajetórias | `Aligner.kt` | `AlignerTrajectoryTest`: leitura normal exige cursor = esperado desde a k-ésima palavra e ≥ tokens − k movimentos; improviso exige cursor imóvel em cada amostra |
| A10 `highWater − 3` em recuos sucessivos | `Aligner.kt:103` | `AlignerTest.recuosSucessivosNaoPassamDeHighWaterMenos3`: vermelho com R-Q2-1, V1, V2, V3 |
| A11 recuperação ambígua | `Aligner.kt:127-135` | `AlignerTest.recuperacaoAmbigua_naoMoveOCursor_L30`: pega `firstOrNull`, não pega V6 (achado 1) |
| A12–A15 acentos/`[unk]`, `reposition`, cada `Move`, L30 | `Aligner.kt` | `AlignerTest`, `AlignerTrajectoryTest.cadaMoveApareceComoLastMove`, `AlignerL30Test` (3.399 chamadas, > 3.000 amostras de leitura normal exigidas) |

Desvios do plano: nenhum declarado, e não encontrei nenhum. O código bate com os blocos do `impl_plan.md` (Tarefas 1–7).

## Testes (Q2 e Q3)

Fiz cada quebra isoladamente com troca exata de texto, rodei `./gradlew :core:test --rerun-tasks`, li os XMLs de resultado e desfiz com `git checkout -- <arquivo>`. Ao fim, `git status --short etapa0/` saiu vazio e `:core:test` voltou verde. Script em scratchpad, fora do repositório.

| Quebra | Arquivo | Vermelhos |
|---|---|---|
| R-Q2-1 (do construtor) limite contra o cursor | Aligner.kt:103 | `recuosSucessivosNaoPassamDeHighWaterMenos3` |
| V1 limite `highWater − 4` | Aligner.kt:103 | `recuosSucessivos…` |
| V2 `highWater` não sobe em Short/LongJump | Aligner.kt:47 | `recuosSucessivos…`, `reposition_primeiroAvanco…`, `reposition_voltaLivremente…`, `repeticaoPeloOradorT08…` |
| V3 recuperação varre o roteiro inteiro (também para trás) | Aligner.kt:129 | `recuosSucessivos…`, `repeticaoPeloOradorT08…` |
| **V4** candidato único da recuperação sem o filtro A2 | Aligner.kt:134 | **nenhum (verde)** |
| V5 A2 removido na janela | Aligner.kt:39 | `unkNuncaCasa_D13`, `improvisoT04…` |
| **V6** ambiguidade contada **depois** do filtro A2 | Aligner.kt:132 | **nenhum (verde)** |
| **V7** sem desempate por distância (D9) | Aligner.kt:113 | **nenhum (verde)** |
| **V8** `Stay` no cursor não zera `missStreak` (D6) | Aligner.kt:43 | **nenhum (verde)** |
| V9 avanço curto sem limite de 5 tokens | Aligner.kt:40 | `cadaMove…`, `numerosEVariantesT02…`, `improvisoT04…`, `saltoLongo…` |
| **V10** recuperação já no 1º miss | Aligner.kt:55 | **nenhum (verde)** |
| V11 empate do ponto alinhado escolhe o menor prefixo | PartialReconciler.kt:67 | `correcaoNoComecoDaFrase…` |
| V12 Final sem `reset()` | PartialReconciler.kt:24 | `finalComMaisPalavras…`, `fraseRepetida…T08` |
| V13 `reset()` sem `previous = null` | PartialReconciler.kt:31 | `reset_sessaoNova…` |
| V14 parcial entrega tudo, sem prefixo estável | PartialReconciler.kt:18 | 10 de 10 do `PartialReconcilerTest` |
| V15 peso comum = 1,0 | AlignConstants.kt:24 | `pesoDasPalavrasComuns_T09` |
| V16 Normalizer sem tirar acento | Normalizer.kt:19 | 9 testes (Normalizer, Tokenizer, Aligner) |
| V17 linha `//` gera token | Tokenizer.kt:30 | `TokenizerTextsTest` (2), `repeticaoPeloOradorT08…`, `puloDeParagrafoT05…` |

Contagem mínima (Q3): os testes exigem as próprias contagens. Leitura normal: ≥ tokens − k movimentos. Recuos: pelo menos um recuo permitido. Ambiguidade: `score ≥ limite longo`. L30: > 3.000 amostras. T09: ≥ 50. Textos: > 50 tokens e notas presentes. Também "amostras depois do evento > 6" e 43 trocas no T01. Nenhuma aprovação vazia encontrada.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | BLOQUEANTE | `Aligner.kt:132`, `:134`, `:55`; teste em `AlignerTest.kt:128-151` | As guardas da recuperação não estão travadas. (a) **V6**: contar a ambiguidade depois do filtro A2 faz a recuperação saltar quando há **duas** posições ≥ limite longo. Isso viola o critério "Recuperação ambígua" do spec, o TRD §Alinhamento 4 ("salta apenas se existir uma única posição acima do limite longo") e a própria A2 ("conta-se a ambiguidade antes desse filtro"). (b) **V4**: o candidato único salta mesmo com a palavra mais nova sem casar. (c) **V10**: a recuperação dispara no 1º miss em vez do 2º (constante fixa do TRD, D7/D12). As três deixam o salto longo mais agressivo, contra "na dúvida, o cursor fica parado", e passam com as 85 suítes verdes. O único teste da recuperação ambígua usa duas cópias idênticas ("no fim do ano a"), em que as duas passam no filtro, então ele só pega `firstOrNull`. | Execução registrada na tabela Q2: V4, V6 e V10 com exit=0 e nenhum vermelho. Caso concreto de (a), por cálculo: cópias "fim do ano a" e "fim do ano o" à frente de `highWater`, fala "… fim do ano a". A segunda dá (1+1+1+0,5·0)/3,5 = 0,857 ≥ 0,85 com `lastWord` = 0, então o código atual vê 2 candidatos e fica parado, e o V6 vê 1 e salta. (Não executei esse caso: a criação de arquivo de teste numa cópia descartável foi negada pela permissão.) | Testes que fiquem vermelhos com V4, V6 e V10. Por exemplo: (a) duas posições ≥ limite longo em que só uma casa a palavra mais nova (com `Token`s montados no teste a partir de palavras do L30/T01, ou trecho dos textos) → `cursor`/`highWater` parados e `Stay`; (b) candidato único ≥ limite longo cuja palavra mais nova não casa → não salta; (c) depois do 1º miss com 4 palavras que casam com uma posição única fora da janela, ainda `Stay` e `missStreak = 1`, e só no 2º miss `Recovery`. Registrar V4/V6/V10 na tabela de quebras do plano para o QA. |
| 2 | IMPORTANTE | `Aligner.kt:43` (D6) e `Aligner.kt:109-114` (D9) | Duas decisões fixadas no spec sem teste que as trave. **D6**: vencedor no cursor zera `missStreak` (V8 verde; sem isso a recuperação chega mais cedo). **D9**: empate → posição mais próxima do cursor (V7 verde; o brainstorm diz que é o que "faz o refrão do T03 seguir a cópia certa", mas o teste do T03 continua verde sem ele). | Tabela Q2: V7 e V8 sem vermelho. | Um teste pontual para cada um: `advance` cujo vencedor é o próprio cursor partindo de `missStreak = 1` → `missStreak = 0`; empate exato de similaridade em duas posições a distâncias diferentes → vence a mais próxima. Pode entrar no mesmo retrabalho do achado 1. |
| 3 | MENOR | `Aligner.kt:109-114` | Empate de similaridade **e** de distância (cursor − d e cursor + d) escolhe o índice menor, ou seja, o recuo. Duas posições igualmente boas são dúvida, e o TRD não define o desempate. Hoje não fere `highWater − 3`. | Leitura do código (busca crescente com `<` estrito). | Decidir na calibração da 0005 (ficar no cursor ou preferir à frente). Registrado como OPR-011. |
| 4 | MENOR | `NormalizerTest.kt:36` | `"d'água"` não está em `etapa0/textos/` (spec B6; CLAUDE.md "Textos de teste"). É palavra isolada, não roteiro, e não traz risco de privacidade. | `grep -lF "d'água" etapa0/textos/*.txt` → vazio. | Trocar por palavra com apóstrofo dos textos, ou registrar a exceção no spec. Registrado como OPR-012. |
| 5 | MENOR | `PartialReconciler.kt:71-74`; `Aligner.kt:123-136` | Custo no aparelho: `same()` roda `Normalizer.normalize` (4 regex) duas vezes por célula da matriz de edição, a cada parcial. A recuperação varre o roteiro inteiro à frente de `highWater`. Na minha execução, o L30 deu total 267,0 ms e **máximo 25,453 ms** por `advance`; a evidência do construtor tem 151,1 ms e 2,401 ms. O máximo varia 10× entre execuções na mesma máquina (JIT), então o número da evidência não é medida estável (Q7). | `TEST-…AlignerL30Test.xml`, `system-out`. | Sem meta hoje (D23). Na 0005, medir no aparelho com aquecimento e várias repetições; se preciso, normalizar uma vez por palavra no reconciliador. Registrado como OPR-013. |

## Não verificado

- O caso concreto do achado 1(a) não foi executado, só calculado: a permissão negou criar o teste demonstrativo numa cópia descartável de `etapa0/`. A ausência de teste que trave V4, V6 e V10 foi verificada por execução.
- Comportamento com saída real do Vosk (dígitos, hífen, "dia primeiro"): fora do escopo, depende da 0003 (Q4).
- `connectedDebugAndroidTest` e `verify16kAlignment`: não se aplicam (nada nativo ou de UI muda nesta tarefa).
