# Brainstorm — 0002-etapa0-align

**Autor:** analista · **Data:** 2026-10-10 · Skill: `superpowers:brainstorming`, com os ajustes do projeto: o analista responde às perguntas da skill, não há companheiro visual (não há tela nesta tarefa) e o trabalho termina neste arquivo e no `spec.md`, sem `writing-plans`.

## Contexto lido

- `CLAUDE.md`, `PROCESS.md` (Etapa 0, item 2), `docs/PRD.md` (Motor de acompanhamento por voz, tabela de sensibilidade, Testes), `docs/TRD.md` (Contratos §Token e §Aligner; Motor de voz §Reconciliação de hipóteses, §Normalização e tokens, §Alinhamento; Testes: tabela de suítes; Etapa 0), `docs/TESTES-ETAPA0.md` (textos T01–T10 e L30, regras das métricas), `REVIEW-QUESTIONS.md`, `registers/opr_register.md`.
- O que a 0001 entregou: `etapa0/core` (módulo Kotlin/JVM puro) com `Token`, `AlignParams`, `AlignState`, `Move` e `Aligner` (esqueleto que lança `NotImplementedError`), além dos contratos de `speech` e do `FakeSpeechEngine`.
- Textos de teste: `etapa0/textos/T01.txt` a `T10.txt` e `L30.txt`. Os números e referências que aparecem neles são `1914`, `24`, `3,5%`, `João 3:16`, `Salmo 23:1`, `Mateus 24:14`, `Romanos 12:2`, `1 Coríntios 13:4-7`, `4`, `7`, `2`, `15`, `1`, `30`, `10` (T02); `Gálatas 6:9`, `Romanos 5:3, 4`, `340 pessoas`, `Hebreus 12:1` (L30). Também há palavras hifenizadas (`bom-dia`, `escrevê-los`, `cumpri-la`, `lê-la`), linhas de nota `//` (T04–T08, T10, L30), travessão e aspas.

## Escopo

É um subsistema só, o pacote `align`, com quatro unidades e uma fronteira já fixada pelos contratos do TRD. Não precisa ser decomposto. O que o orador ganha é indireto: é o núcleo que a 0005 vai medir no aparelho.

## Abordagens consideradas (para o spec, não para o código)

1. **Spec curto que só aponta para o TRD.** Barato, mas deixa sem resposta pontos que mudam o resultado dos testes (a semântica de `words`, o que conta como "posição", quando um movimento conta como miss, a parte ambígua da recuperação). Assim o portão automático não teria como verificar os critérios.
2. **Spec que aponta para o TRD e decide só as lacunas**, com critérios verificáveis sobre trajetórias sintéticas tiradas dos textos de teste. **Escolhida.**
3. **Spec que também desenha as classes**, com APIs do `PartialReconciler` e estruturas internas. Rejeitada: o desenho é do `arquiteto` (PLAN).

## Decisões

Cada decisão traz a pergunta, as alternativas, a escolha e o motivo. Nenhuma muda a assinatura de um contrato.

**D1. O que é `words` em `Aligner.advance(state, words)`?**
- (a) Só as palavras novas que o reconciliador acabou de entregar.
- (b) A cauda das palavras confirmadas desde o último reposicionamento ou desde o início da sessão, em ordem, terminando na mais nova.
- (c) Acrescentar um buffer de palavras ao `AlignState`.
- **Escolha: (b).** O passo 2 do Alinhamento compara "as últimas *k* palavras confirmadas" e a recuperação usa "as últimas 4 ou mais". O `AlignState` do contrato não guarda histórico de palavras, e o reconciliador costuma entregar 1 ou 2 palavras por parcial. Com (a), uma função pura não consegue cumprir o TRD. (c) muda um contrato, o que pede CIA e portão humano sem necessidade. (b) cabe na assinatura e no texto do TRD ("recebe o estado e as palavras confirmadas"). Quem chama (a tela mínima da 0003 e, mais tarde, o ViewModel) mantém a cauda e a zera em `reposition` e no começo de cada sessão (PRD, Regras do toque 5). Nesta tarefa os testes simulam essa cauda. Fica para o arquiteto confirmar no PLAN que se trata de interpretação e não de mudança de semântica. Se ele entender que é mudança, vira CIA e portão humano. Registrado como OPR-007.

**D2. O que é a "posição" do cursor?**
- (a) O índice do último token falado (a palavra atual).
- (b) O índice do próximo token a falar.
- **Escolha: (a).** A métrica (TESTES-ETAPA0, "posição esperada em t: o maior token cujo `t_fim_ms ≤ t`") e a regra "cursor no último token por 2 s → Finished" (TRD, Máquina de estados) só fecham com (a). Na frase "avança o cursor para depois desse token inteiro" (TRD Contratos), leio que as palavras de uma variante pertencem a um único token: o cursor não para no meio dele. Uma posição candidata na janela é o token onde termina a sequência de *k* palavras.

**D3. Como a leitura começa?** Não existe fábrica de estado inicial no contrato. **Escolha:** o estado inicial é `reposition(0)`. Isso evita criar API nova e é o que um toque na primeira palavra faria.

**D4. Que valor `score` recebe em `reposition`?** **Escolha:** `0.0`, com `missStreak = 0`, `lastMove = Reposition` e `cursor = highWater = índice`. Um índice fora da faixa é limitado a `[0, tokens.size − 1]`, porque quem chama é a interface e lançar exceção derrubaria a leitura. Com o roteiro vazio, `advance` devolve o estado inalterado. Em `advance`, `score` é a melhor similaridade avaliada na chamada, mesmo quando o movimento é `Stay`, para alimentar o painel de depuração (TRD Etapa 0, item 4). Vale `0.0` quando nada foi avaliado.

**D5. O que acontece com menos de *k* palavras na cauda?**
- (a) Comparar só as que existem.
- (b) Não mover.
- **Escolha: (b).** O resultado é `Stay`, e `missStreak` e `highWater` ficam como estavam. Comparar 1 palavra quando *k* = 4 abre caminho para salto errado ("na dúvida, espera"). O custo é um atraso de até *k* − 1 ≤ 3 palavras no início, dentro da meta.

**D6. Quando uma chamada conta como miss (`missStreak`)?**
- (a) Quando nenhum candidato passa no limite (PRD: "nada passa na janela").
- (b) Quando o cursor não muda de lugar.
- **Escolha: (a).** É o texto do PRD e não pune a reconfirmação da mesma posição. Se o vencedor passa no limite e coincide com o cursor, o resultado é `lastMove = Stay` com `missStreak = 0`. Um recuo permitido (até `highWater − 3`) com similaridade ≥ limite curto é `Short`.

**D7. Quando a recuperação dispara?** **Escolha:** na mesma chamada em que o miss leva `missStreak` a 2 ou mais. Usa as últimas 4 palavras (PRD/TRD: "4 ou mais"; *k* ≤ 4 em todos os presets). Com menos de 4 palavras na cauda, não há recuperação. Se a recuperação salta, `missStreak` volta a 0 e `highWater` passa a ser o novo cursor.

**D8. O que conta como recuperação ambígua?** O TRD diz "com dois ou mais candidatos próximos do limite, o cursor fica parado".
- (a) Dois ou mais candidatos ≥ limite longo.
- (b) Mais uma margem abaixo do limite.
- **Escolha: (a).** A margem de (b) não existe no PRD nem no TRD, e inventá-la é proibido. Leitura adotada: a recuperação salta só se existir exatamente uma posição ≥ limite longo; com duas ou mais, mesmo vizinhas, o cursor fica parado. A margem, se for preciso, entra na calibração da 0005. Registrado como OPR-007 (redação do TRD).

**D9. Como desempatar na janela normal?** **Escolha:** fica a posição de maior similaridade média ponderada (TRD passo 2) e só ela é testada contra os limites. No empate, vence a posição mais próxima do cursor. É o critério conservador e o que faz o refrão do T03 seguir a cópia certa. "Distância" é `|posição − cursor|` em tokens (TRD; o PRD diz "palavras", e o TRD prevalece).

**D10. Peso das palavras comuns.**
- Lista: exatamente as cinco do PRD, comparadas na forma normalizada: `de`, `que`, `e`, `o`, `a`. Por isso "é" também conta como comum, porque normaliza para `e`.
- Valor: **0,5, PROVISÓRIO**. Nem o PRD nem o TRD trazem número. Fonte: decisão do analista para a Etapa 0, a calibrar na 0005 com o T09 e o L30. Os demais tokens pesam 1,0. O valor e a lista ficam num único lugar nomeado do `align` e podem mudar sem mexer em contrato. Os critérios do spec não dependem do 0,5 em si, só de o peso comum ser menor que 1.

**D11. Presets de sensibilidade.** **Escolha:** três valores de `AlignParams` com a tabela do PRD: Conservador (4, 25, 0,70, 0,90), Normal (3, 40, 0,60, 0,85), Ágil (2, 60, 0,55, 0,80). Ficam num objeto do `align`, sem acrescentar valor padrão ao contrato (decisão da 0001). São valores do PRD, com calibração final na 0005.

**D12. Constantes fixas do algoritmo.** Distância curta ≤ 5 tokens, recuo ≤ 3 a partir de `highWater`, 2 misses e 4 palavras na recuperação. Todas vêm do PRD/TRD e não variam por sensibilidade.

**D13. Quem normaliza as palavras faladas?** **Escolha:** o `Aligner` aplica o `Normalizer` às palavras recebidas, o que é idempotente. O Vosk em pt devolve acentos ("três"), e esquecer a normalização em quem chama zeraria o acompanhamento. `[unk]` (vocabulário restrito) tem similaridade 0 com qualquer palavra do roteiro.

**D14. Faixa dos números.** **Escolha:** inteiros de 0 a 999.999, com e sem separador de milhar por ponto (`3.600`), decimais com vírgula e porcentagem. Essa faixa cobre todos os números dos textos de teste. Fora dela, ou em formatos não previstos (ordinais `1º`, horas `10h`, moeda), o token fica com a forma crua: o alinhamento não casa e o cursor espera. É limite de escopo, não parâmetro. "Dia 1" falado como "dia primeiro" (T02) não é gerado, porque o TRD não prevê essa forma. Fica como observação para a 0005.

**D15. Formas femininas dos números.** O L30 tem "340 pessoas", que se lê "trezentas e quarenta".
- (a) Só a forma masculina.
- (b) Masculina e feminina como formas alternativas.
- **Escolha: (b)**, quando a forma por extenso contém `um`, `dois` ou centenas de 200 a 900. Aplica o TRD ("forma por extenso usual"; "grafias alternativas viram formas faladas alternativas") a um caso real dos textos de teste. Não cria parâmetro.

**D16. Referências bíblicas.** **Escolha:** as formas faladas listadas nos exemplos do TRD Contratos são o mínimo obrigatório, generalizado para qualquer capítulo e versículo:
- simples: `[c, v]`, `[c, e, v]`, `[capitulo, c, versiculo, v]`;
- intervalo: `[c, a…]`, `[c, de, …]`, `[capitulo, c, versiculos, …]`;
- lista: `[c, …, e, último]`, `[c, versiculos, …]`, `[capitulo, c, versiculos, …]`.

Outras combinações com e sem "de", "versículo(s)" e "capítulo" (TRD Normalização) são permitidas, mas não exigidas. Grafias alternativas (catorze/quatorze) entram também nas referências. Um intervalo aceita `-`, `–` e "a". Uma lista continua enquanto o item depois da vírgula for número. "Romanos 5:3, 4, o raciocínio" vira a lista [3, 4], seguida do token `o`. Um padrão `N:M` sem livro também vira referência. Horas no formato `10:30` seriam lidas como referência; não aparecem nos textos e ficam como limitação conhecida.

**D17. Livros com número.** **Escolha:** `1`, `2` ou `3` seguido de palavra com inicial maiúscula que, por sua vez, vem seguida de referência `N:M` forma um único token do livro. As formas são `primeira/primeiro/um`, e por analogia `segunda/segundo/dois` e `terceira/terceiro/tres`. Não se usa lista de livros. "Com 1 capítulo" (minúscula, sem referência) continua número.

**D18. Hífen e apóstrofo.** O Vosk pode devolver "escrevê-los" junto ou separado, e só a 0003/0005 vai mostrar qual (Q4). **Escolha:** a palavra hifenizada é um token com duas formas, `[partes separadas]` e `[partes unidas]`. Na fala, o `Normalizer` remove o hífen dentro da palavra, unindo as partes. Apóstrofo é removido sem separar (`d'água` → `dagua`).

**D19. Recorte de tokens e intervalos exibidos.** **Escolha:** um token por palavra separada por espaço, exceto os tokens compostos de D14–D17. O `display` cobre só os caracteres da palavra, sem a pontuação em volta (vírgula, ponto, aspas). Pontuação solta (`—`, `…`, aspas isoladas) não gera token. Linhas cujo primeiro texto depois dos espaços é `//` não geram token (TRD; T5).

**D20. `sentenceStart`.** **Escolha:** é `true` no primeiro token do texto, no primeiro token depois de um token que termina em `.`, `!`, `?` ou `…`, e no primeiro token depois de uma linha em branco ou de uma linha de nota. Quebra de linha simples não conta, porque texto colado de PDF costuma vir com quebras no meio da frase. Abreviações ("Sr.") geram falso início de frase; é limitação aceita nesta etapa.

**D21. Testes com trajetória sintética.** As falas simuladas são palavras dos textos de `etapa0/textos/`, uma por chamada de `advance`. A cauda é mantida pelo teste conforme D1. As alterações sintéticas permitidas são: troca de palavra por variação ortográfica ou por palavra de outro texto do conjunto; improviso feito com trecho de outro texto do conjunto que não aparece no roteiro; e as três leituras de referência do protocolo do T02. A amostra é o estado depois de cada chamada. O esperado é o token da última palavra lida do roteiro (congelado no improviso). Atraso e salto errado seguem a definição de TESTES-ETAPA0 aplicada por palavra: atraso = esperado − cursor; salto errado = cursor mais de 2 tokens à frente do esperado, ou qualquer movimento durante o improviso. "3 s após um pulo" equivale a 6 palavras (3 s × 130 palavras/min, base do T4 do PRD, ≈ 6,5).

**D22. Golden de transcrições.** Depende das gravações da 0005 e fica fora desta tarefa. Nenhuma tarefa da Etapa 0 o cobre hoje. Registrado como OPR-008 (dono: arquiteto).

**D23. Desempenho do `advance`.** O PRD/TRD não fixam orçamento só para o alinhador; a meta de latência sai da Etapa 0. **Escolha:** nenhum limiar numérico. O teste do L30 sintético registra o tempo total e o tempo máximo por chamada na saída, como evidência para a 0005.

## Perguntas que caem em portão humano

Nenhuma. D1 é interpretação, não mudança de contrato. Se o arquiteto discordar no PLAN, o caso vira CIA e portão humano 1.

## Próximo passo

`spec.md` desta pasta, conferido pelo portão automático do orquestrador. Depois, o `impl_plan.md` do arquiteto.
