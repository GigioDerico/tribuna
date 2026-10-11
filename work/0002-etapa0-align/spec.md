# Spec — 0002-etapa0-align

**Autor:** analista · **Data:** 2026-10-10 · **Aprovação:** conferida pelo orquestrador em 2026-10-10 (critérios verificáveis por comando, parâmetros só do PRD/TRD, peso 0,5 marcado PROVISÓRIO, nenhum portão humano; D1 segue ao arquiteto, OPR-007)

Brainstorm: `work/0002-etapa0-align/brainstorm.md`. As decisões D1–D23 citadas abaixo estão lá, com alternativas e motivo.

## O que e por quê

Implementar o núcleo do acompanhamento por voz em Kotlin puro, no pacote `align` do módulo `:core` de `etapa0/`: `Normalizer`, `Tokenizer` (com formas faladas), `PartialReconciler` e `Aligner` (`advance` e `reposition`, hoje esqueleto da 0001), com as suítes JUnit do TRD. É o item 2 da Etapa 0 (PROCESS.md). Ele tem de ficar pronto e verde antes que a 0003 ligue o Vosk e a 0005 meça no aparelho. Para o orador não muda nada ainda: este é o código que vai decidir se o texto anda, espera ou salta.

## Requisitos atendidos

- PRD: Motor de acompanhamento por voz (Pipeline 3–4, Algoritmo de alinhamento, tabela de parâmetros por sensibilidade), base de R1 e R4; princípio 3 ("na dúvida, o texto espera"); Testes (unitários do Aligner e do PartialReconciler); T5, só a parte "linha com // não é seguida pela voz".
- TRD: Contratos §Token e §Aligner (`Aligner`, `AlignState`, `Move`, `AlignParams`, invariante `highWater − 3`); Motor de voz §Reconciliação de hipóteses (passos 1–6; o passo 7 é de quem chama), §Normalização e tokens, §Alinhamento (passos 1–6); Módulos e responsabilidades (`align` sem Android); Testes, suítes "Reconciliação de hipóteses" e "Alinhamento"; Etapa 0, O que construir, item 3.
- Contratos: só a implementação muda, não a assinatura. `Token`, `AlignState`, `AlignParams`, `Move` e a assinatura de `Aligner` ficam como a 0001 entregou (CLAUDE.md, regra 5).

### Interpretações e valores que este spec fixa (do brainstorm)

- **`words` em `advance`** é a cauda das palavras confirmadas desde o último `reposition` ou início de sessão, terminando na mais nova; quem chama mantém a cauda (D1). Uma chamada de `advance` é um "resultado".
- **Cursor** = índice do último token falado. Uma posição candidata é o token onde termina a sequência de *k* palavras (D2). A leitura começa com `reposition(0)` (D3).
- **`reposition(i)`**: `cursor = highWater = i` limitado a `[0, tokens.size − 1]`, `missStreak = 0`, `lastMove = Reposition`, `score = 0.0`. Em `advance`, `score` = melhor similaridade avaliada na chamada, ou `0.0` (D4).
- **Menos de *k* palavras** na cauda: `Stay`, sem mudar `missStreak` (D5). **Miss** = nenhum candidato passa no limite. Se o vencedor passa e coincide com o cursor, o resultado é `Stay` com `missStreak = 0` (D6).
- **Recuperação**: na chamada em que `missStreak` chega a 2 ou mais, com as últimas 4 palavras, só à frente de `highWater` e até o fim do roteiro. Salta só se exatamente uma posição for ≥ limite longo (D7, D8).
- **Janela normal**: vence a maior média ponderada; no empate, a posição mais próxima do cursor. A distância é contada em tokens (D9).
- **Peso das palavras comuns**: as palavras normalizadas `de`, `que`, `e`, `o`, `a` (PRD) pesam **0,5 (PROVISÓRIO, decisão do analista, a calibrar na 0005)**; as demais, 1,0. O valor e a lista ficam num só lugar nomeado (D10).
- **Presets**: Conservador (4, 25, 0,70, 0,90), Normal (3, 40, 0,60, 0,85), Ágil (2, 60, 0,55, 0,80), da tabela do PRD, num objeto do `align` sem tocar no contrato (D11).
- **Constantes fixas** do PRD/TRD: distância curta ≤ 5 tokens; recuo ≥ `highWater − 3`; 2 misses; 4 palavras na recuperação (D12).
- **Normalização da fala**: o `Aligner` normaliza as palavras recebidas; `[unk]` tem similaridade 0 (D13).
- **Números**: inteiros de 0 a 999.999 (com ou sem ponto de milhar), decimais com vírgula, `%`; forma feminina como alternativa (D14, D15). Referências, livros com número, hífen, recorte e `sentenceStart`: D16–D20.

## Critérios de aceite

Todos são verificados por comando, rodado a partir de `etapa0/`, com a saída anexada à pasta. "Teste" quer dizer JUnit na JVM, em `:core`.

### Build e regras

- [ ] `./gradlew :core:test` termina verde, e a contagem de testes novos no relatório é ≥ 1 por critério desta seção (Q3).
- [ ] `./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest` termina verde. Nenhum teste fica desativado (`@Ignore`/`@Disabled`) no pacote `align`.
- [ ] `grep -rE "import (android|androidx)\." etapa0/core/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/align` não devolve nada (regra inegociável 4).
- [ ] `git diff main -- Token.kt AlignState.kt AlignParams.kt Move.kt` (caminhos completos em `etapa0/core/src/main/kotlin/io/github/gigioderico/tribuna/etapa0/align/`) não mostra mudança. Em `Aligner.kt`, a assinatura do construtor, de `advance` e de `reposition` é idêntica à da 0001.
- [ ] Nenhum `NotImplementedError` sobra no pacote `align` (`grep -r NotImplementedError` vazio).
- [ ] Todo texto de roteiro usado nos testes é lido de `etapa0/textos/` ou é cópia literal de trecho desses arquivos. As falas simuladas seguem D21. A conferência é do revisor, por leitura do diff.

### Normalizer e Tokenizer (suíte Alinhamento)

- [ ] `Normalizer` deixa tudo minúsculo, tira os acentos (incluindo `ç → c`) e a pontuação. É idempotente (normalizar duas vezes dá o mesmo resultado) e trata o roteiro e a fala do mesmo jeito. Testes com palavras de T01 e T09 (ex.: "Às" → "as", "É" → "e", "conversa." → "conversa").
- [ ] Tokenizar cada texto de `etapa0/textos/` produz tokens com `index` igual à posição na lista. Cada `display` aponta para um trecho do texto original que, normalizado, bate com uma das formas faladas do token. Nenhum token sai das linhas `//` (teste com T04 e L30: nenhum `display` cai dentro de linha de nota).
- [ ] As formas faladas contêm exatamente estes exemplos do TRD Contratos: `"24"` → `[[vinte, e, quatro]]`; `"3:16"` contém `[tres, dezesseis]`, `[tres, e, dezesseis]`, `[capitulo, tres, versiculo, dezesseis]`; `"13:4-7"` contém `[treze, quatro, a, sete]`, `[treze, de, quatro, a, sete]`, `[capitulo, treze, versiculos, quatro, a, sete]`; `"12:1, 2"` contém `[doze, um, e, dois]`, `[doze, versiculos, um, e, dois]`, `[capitulo, doze, versiculos, um, e, dois]`. Cada referência é um único token, separado do token do livro.
- [ ] Nos números e referências do T02 e do L30, as formas incluem: `1914` → `mil novecentos e catorze` e `… quatorze`; `3,5%` → um token com `[tres, virgula, cinco, por, cento]`; `340` → `trezentos e quarenta` e `trezentas e quarenta`; `24:14` → com `catorze` e com `quatorze`; `5:3, 4` → um token com `[cinco, tres, e, quatro]` e o token seguinte é `o`; `1 Coríntios` → um token com `[primeira, corintios]`, `[primeiro, corintios]`, `[um, corintios]`; "Com 1 capítulo" → `1` é token próprio, com `[um]` e `[uma]`.
- [ ] Intervalo com `–` e com "a" (ex.: `13:4–7`, `13:4 a 7`) gera as mesmas formas que `13:4-7`, e a lista `12:1, 2, 5` gera `[doze, um, dois, e, cinco]`. Esses casos são variações dos números do T02 escritas no próprio teste.
- [ ] Palavra hifenizada (`bom-dia` do T03, `escrevê-los` do L30) vira um token com a forma de partes separadas e a de partes unidas (D18).
- [ ] Os tokens de `de`, `que`, `e`, `o`, `a` e `é` têm peso < 1,0 e igual ao valor provisório nomeado; os demais têm peso 1,0 (teste no T09).
- [ ] `sentenceStart` é `true` exatamente nos tokens previstos em D20. Teste no T09: 15 frases, então 15 tokens com `sentenceStart = true`. Teste no T01: o primeiro token de cada parágrafo é início de frase.

### PartialReconciler (suíte Reconciliação de hipóteses)

As sequências de parciais são escritas no teste com frases de `etapa0/textos/`. Cada teste confere a lista completa de palavras entregues, em ordem.

- [ ] Parciais que só crescem (ex.: "ouvir" → "ouvir bem" → "ouvir bem e uma" → Final "ouvir bem e uma habilidade"): só sai palavra que aparece no prefixo comum de dois parciais seguidos, e o total entregue até o Final é igual ao Final, sem repetir nem perder palavra.
- [ ] Inserção, remoção e correção no começo da frase depois de palavras já entregues (ex.: "a maioria de nós" revisado para "a maior parte de nós"): nenhuma palavra já entregue sai de novo nem é retirada, e as palavras depois do ponto alinhado são entregues uma vez.
- [ ] Final diferente do último parcial (mais palavras, menos palavras, palavra trocada): o que falta é entregue no Final, sem duplicar o que já saiu, e o estado da frase zera.
- [ ] Frase repetida: o mesmo Final duas vezes seguidas (orador repete "isso tem um custo", T08) entrega as palavras nas duas vezes.
- [ ] Parcial vazio e primeiro parcial de uma frase não entregam nada.
- [ ] Começar sessão nova zera o estado: um parcial depois do reinício não é comparado com hipóteses da sessão anterior.

### Aligner (suíte Alinhamento)

Trajetórias sintéticas conforme D21: uma palavra por chamada, cauda mantida pelo teste, amostra depois de cada chamada. **Salto errado** = cursor mais de 2 tokens à frente do esperado, ou qualquer mudança de cursor durante improviso. **Atraso** = esperado − cursor, em tokens. Salvo indicação, o preset é Normal.

- [ ] **Leitura normal (T01)**, com cada um dos três presets e leitura exata: a partir da *k*-ésima palavra, o cursor é igual ao esperado em 100% das amostras, e o cursor se move pelo menos (nº de tokens − *k*) vezes (Q3).
- [ ] **Palavras erradas (T01)**: com 1 em cada 5 palavras trocada por variação ortográfica ou por palavra de outro texto do conjunto, temos zero saltos errados, atraso ≤ 3 em ≥ 95% das amostras, e o cursor chega ao último token.
- [ ] **Pulo de parágrafo (T05: T01 sem o 2º parágrafo)**: zero saltos errados; atraso ≤ 3 até a 6ª palavra depois do pulo (≈ 3 s a 130 palavras/min); `lastMove = Recovery` em pelo menos uma amostra.
- [ ] **Salto longo dentro da janela**: no T01, pular uma frase (menos de *W* tokens) gera `lastMove = LongJump` em pelo menos uma amostra, com zero saltos errados.
- [ ] **Improviso (T04: T01 com trecho do T03 inserido depois do 1º parágrafo)**: zero mudanças de cursor durante o improviso; depois da volta ao roteiro, atraso ≤ 3 até a 6ª palavra.
- [ ] **Repetição pelo orador (T08)**: depois de dizer "Isso tem um custo." e "Um momento de pausa não precisa ser preenchido.", o orador repete cada frase. Resultado: zero saltos errados, cursor nunca abaixo de `highWater − 3`, e atraso ≤ 3 até a 6ª palavra depois da repetição.
- [ ] **Trechos repetidos no roteiro (T03)**: na leitura normal, zero saltos errados em todas as amostras; o cursor nunca pula para outra cópia do refrão.
- [ ] **Palavras comuns (T09)**: zero saltos errados; atraso ≤ 3 em ≥ 95% das amostras.
- [ ] **Números e variantes (T02)**: em três trajetórias, uma por take do protocolo ("três dezesseis"; "três e dezesseis"; "capítulo três, versículo dezesseis", com intervalo e lista no formato correspondente), temos zero saltos errados, o cursor chega ao último token e cada token de referência é cursor em pelo menos uma amostra ou é ultrapassado com atraso ≤ 3.
- [ ] **Invariante `highWater − 3` em recuos sucessivos**: uma sequência de vários recuos curtos seguidos, cada um permitido isoladamente, nunca leva o cursor abaixo de `highWater − 3`. O teste fica vermelho se o limite for medido contra o cursor em vez de `highWater`, ou se for removido (Q2; o QA registra a quebra).
- [ ] **Recuperação ambígua**: quando as últimas 4 palavras casam ≥ limite longo com duas ou mais posições à frente de `highWater` (ex.: refrão do T03), `cursor` e `highWater` não mudam e `lastMove = Stay`.
- [ ] **Fala com acentos e maiúsculas** ("Três", "ESCOLHA") produz a mesma trajetória que a forma normalizada; `[unk]` na cauda nunca casa.
- [ ] **`reposition`** segue D4: cursor, `highWater` e `missStreak` como definidos, `lastMove = Reposition`, índice negativo ou maior que o último é limitado, e o primeiro avanço depois do reposicionamento parte da nova posição.
- [ ] **Cobertura de movimentos**: cada valor de `Move` (`Stay`, `Short`, `LongJump`, `Recovery`, `Reposition`) aparece como `lastMove` em pelo menos um teste.
- [ ] **L30 sintético** com os eventos das linhas `//`: improviso (trecho do T01), pulo do parágrafo "Um detalhe prático", silêncio (nenhuma chamada), `reposition` em "Imagine" três parágrafos à frente, e repetição de "Ninguém chega longe de uma vez só.". Resultado: zero saltos errados; atraso ≤ 3 em ≥ 95% das amostras de leitura normal (fora do improviso e das 6 palavras depois do pulo e do reposicionamento); cursor no último token no fim. O teste imprime o tempo total e o tempo máximo por chamada de `advance` (evidência para a 0005, sem limiar; D23).

## Fora do escopo

- Golden de transcrições reais: depende das gravações da 0005 (OPR-008).
- Descarte de evento de sessão antiga (passo 7 da Reconciliação), cauda de palavras e limiar de nível de áudio: ficam com quem chama (tela da 0003; ViewModel do app).
- `VoskEngine`, `ModelInstaller`, tela mínima, painel de depuração, exportação de trajetórias (0003). Lista de vocabulário restrito: a 0003 a deriva de `Token.spoken`.
- Ferramentas Python e `alinhar.py` com formas faladas (0004). Medição no aparelho e calibração de presets, peso comum e margem de ambiguidade (0005).
- `ReadingStateMachine`, `PrompterViewModel`, fim do texto ("Fim" após 2 s), Volume − (que vai usar `sentenceStart`): são do app.
- Números fora de D14: ordinais (`1º`), horas, moeda, números romanos, milhões; "dia 1" falado "dia primeiro"; abreviações com ponto. Nesses casos o token fica com a forma crua e o cursor espera.
- Qualquer mudança em `Token`, `AlignState`, `AlignParams`, `Move` ou na assinatura de `Aligner`. Se o código não couber no contrato, parar (CLAUDE.md, regra 5).
- Orçamento de tempo do `advance`: ainda não existe meta; a 0005 fixa.

## Mudança proposta no PRD (se houver)

Nenhuma.

## Perguntas em aberto

- Nenhuma num portão humano.
- Para o arquiteto, no PLAN (não bloqueia este spec): confirmar que D1 (cauda de palavras em `words`) é interpretação do contrato e não mudança de semântica. Se ele entender que é mudança, vira CIA e portão humano 1 antes do BUILD. Ver OPR-007.
