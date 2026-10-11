# Brainstorm — 0004-etapa0-ferramentas

**Autor:** analista · **Data:** 2026-10-11 · Skill: `superpowers:brainstorming`, com os ajustes do projeto. O analista respondeu às perguntas da skill. Não houve companheiro visual, porque as ferramentas rodam na linha de comando e não têm decisão visual. O trabalho para aqui e no `spec.md`, sem `writing-plans`.

## Contexto lido

- `CLAUDE.md`: comandos das ferramentas, regras 1 a 3, "Não invente parâmetros", "Textos de teste". `PROCESS.md`: Etapa 0, item 4 (motor-voz), portões humanos, Q2 no QA.
- `docs/TESTES-ETAPA0.md`: nomes dos WAV, folha de eventos (`t_ms,evento,detalhe` e eventos válidos), trajetória esperada (passos 1 a 4, `token,palavra,t_inicio_ms,t_fim_ms`) e "Métricas: como calcular" (amostra a cada 100 ms, atraso, salto errado, recuperação, recuo, latências). `docs/TESTES-ETAPA0-L30.md`: eventos do L30, entre eles o toque numa palavra 3 parágrafos à frente (min 22).
- `docs/TRD.md`: Contratos §Token (formas faladas, exemplos de referência, intervalo e lista), Motor de voz §Normalização e tokens, Metas de desempenho (latências 1 a 3, total abaixo de 500 ms provisória, atraso, saltos), Testes (Golden, Integração de voz, Privacidade nos testes), Etapa 0 (critérios de saída).
- `docs/PRD.md`: Motor de acompanhamento (normalização, palavras comuns), Métricas de sucesso, Critérios de aceite do MVP (toque 3 parágrafos à frente, pulo em até 3 s, 30 s de silêncio).
- Exportações da 0003 (`etapa0/app/.../export/ExportFiles.kt`, `Recording.kt`): `hypotheses.csv` (`t_ms,session,kind,words,accepted`, palavras entre aspas), `level.csv`, `trajectory.csv` (`t_ms,cursor,move,score`, `move` = nome do `Move`, primeira linha `0,0,Reposition,0.0`), `events.csv` (`pausa_toque`, `retoma_toque`, `toque_palavra`), `meta.txt` (chaves de D11, entre elas `animation_ms`). As latências 2 e 3 aparecem só no painel e **não são exportadas**. O `t_ms` conta desde o `startCapture`.
- Núcleo da 0002: `Normalizer.kt`, `Tokenizer.kt`, `NumberWords.kt`, `AlignConstants.kt`; os testes Kotlin leem `etapa0/textos/` pela propriedade `tribuna.textos` (`TestTexts.kt`).
- `registers/opr_register.md`: OPR-008 e OPR-019 (golden e integração na 0006, fora daqui), OPR-011 (empate: vale a primeira forma), OPR-017 (exceção para dados sintéticos), OPR-020 (léxico com acento), OPR-025 (hífen).
- Memória do analista: base de tempo e formatos da 0003, interpretações do align (cursor = índice do último token falado, leitura começa com `reposition(0)`), regra dos parâmetros provisórios.
- Estado do repositório: `etapa0/ferramentas/`, `eventos/`, `esperado/` e `trajetorias/` só têm `.gitkeep`. Não há código Python nem testes Python. O `.gitignore` já ignora `__pycache__/` e `.venv/`. `.claude/settings.json` permite `Bash(python3 etapa0/ferramentas/*)`. Os agentes já rodaram `python3` em scripts de scratchpad (memórias do QA e do motor-voz), então há um Python 3 na máquina. A versão e a presença do pytest não foram conferidas aqui, porque o analista não tem shell: o PLAN confere.

## Escopo

São três ferramentas encadeadas, com uma fronteira por arquivo:

```
WAV --timestamps.py--> words.csv --alinhar.py (+ texto)--> expected.csv
trajectory.csv + expected.csv + events.csv [+ hypotheses.csv] --metricas.py--> relatório
```

Cabem num spec só. O risco está em três pontos: (1) a paridade das formas faladas entre Python e Kotlin; (2) as lacunas das definições de métrica do TESTES-ETAPA0, que mudam o veredito dos critérios de saída; (3) o `timestamps.py`, que depende do Vosk em Python e do modelo grande, nenhum dos dois instalado no projeto. Decisão de forma: toda a lógica é testável só com a biblioteca padrão e dados sintéticos. O Vosk entra só no modo real do `timestamps.py`, que a 0005 executa.

## Abordagens consideradas

1. **Spec curto, "conforme o TESTES-ETAPA0".** Rejeitada. O documento não diz o que fazer com o toque em palavra (o cursor salta à frente por decisão do orador), não diz como a ferramenta escolhe a forma falada nem como desempata o alinhamento, não diz onde exportar as latências e não diz como verificar a paridade com o Kotlin. O portão também ficaria sem critério por comando.
2. **Spec que fixa as lacunas, com critérios em dados sintéticos derivados de `etapa0/textos/`, paridade verificada por arquivo de referência gerado pelo Kotlin e o modo real do Vosk deixado para a 0005.** **Escolhida.**
3. **Chamar o Kotlin a partir do Python (JAR do `:core` como fonte das formas faladas).** Rejeitada. Dá paridade por construção, mas exige JDK e Gradle para rodar uma ferramenta de computador, acopla o Python ao build do Android e complica o teste. A paridade por arquivo de referência é verificável dos dois lados com um comando cada.

## Decisões

Cada decisão traz a pergunta, as alternativas, a escolha e o motivo. Nenhuma toca contrato do TRD, manifesto ou app.

**D1. Dependências do Python.**
- (a) numpy/pandas para o DP e as métricas.
- (b) Só a biblioteca padrão (`csv`, `wave`, `json`, `unicodedata`, `re`, `hashlib`, `argparse`, `unittest`). O pacote `vosk` é importado só dentro do modo real do `timestamps.py`.
- **Escolha: (b).** O pedido prefere a stdlib. Instalar pacote é portão humano 6, e nada aqui precisa de vetorização. Os testes não dependem do pacote `vosk`, que é falsificado por injeção.

**D2. Framework de teste e como rodar.**
- (a) pytest. (b) `unittest` da stdlib, com um executor em `etapa0/ferramentas/`.
- **Escolha: (b)**, com o executor `python3 etapa0/ferramentas/testar.py`, que descobre e roda todos os testes e sai com código diferente de 0 se algum falhar. Funciona sem pytest e cabe na permissão `python3 etapa0/ferramentas/*` das settings. Se o pytest existir, ele também roda os mesmos testes, mas não é exigido. O PLAN registra `python3 --version` na evidência e o código não usa recurso de versão mais nova que essa.

**D3. Paridade das formas faladas com o Tokenizer/NumberWords (pedido do orquestrador).**
- (a) Confiar na revisão. (b) Python chama o Kotlin (abordagem 3). (c) Arquivos de referência versionados, gerados a partir do `Tokenizer` Kotlin: um por texto de `etapa0/textos/` (índice, intervalo exibido e formas faladas, na ordem, de cada token) e um de números (`NumberWords`). Um teste JVM no `:core` falha se o Kotlin divergir da referência, e um teste Python falha se o Python divergir. Os dois verdes provam a paridade.
- **Escolha: (c).** O Kotlin é a referência (TRD: "formas faladas do Tokenizer"), e regenerar o arquivo é ação explícita, documentada no plano, nunca automática dentro do teste. O peso e o `sentenceStart` ficam fora da referência, porque as ferramentas não os usam. O arquivo de números cobre de 0 a 2.100, todos os inteiros, mais uma lista fixa de milhares com resto até 999.999, definida no plano. Para cada número entram as formas de `cardinalForms` e de `masculineForms`. Como são só números, não é texto de discurso nem fere a regra dos textos de teste.
- **Consequência:** a tarefa mexe também em `etapa0/core/src/test` (teste novo de referência), sem tocar `align/` de `main`. A 0002 é do motor-voz, que é o mesmo dono.

**D4. O `Normalizer` em Python.**
- **Escolha:** minúsculas, NFD sem marcas combinantes (`Mn`), hífen e apóstrofo entre letras ou dígitos unem as partes, o resto que não é letra, dígito ou espaço vira espaço, e espaços são colapsados. É a mesma regra de `Normalizer.kt`. A paridade fica provada pelos arquivos de D3, que passam pelo normalizador em todos os textos, e por um teste de idempotência. Aplica-se ao roteiro e à transcrição do Vosk grande, que vem com acento (OPR-020).

**D5. Formato da transcrição com tempos (`timestamps.py <wav>`).**
- **Escolha:** CSV `palavra,t_inicio_ms,t_fim_ms,conf`, uma linha por palavra reconhecida, na ordem do tempo. `palavra` sai exatamente como o Vosk devolveu (com acento; golden bruto). Os tempos são `round(segundos × 1000)`, e `conf` vem do Vosk. A saída vai para stdout, ou para um arquivo com `-o`. As palavras vêm de todo `Result()` e do `FinalResult()` com `SetWords(True)`.
- **Entrada:** WAV 16 kHz, mono, PCM 16 bits (protocolo do TESTES-ETAPA0, o mesmo da fonte WAV da 0003). Outro formato é recusado com código 2. O nome do arquivo tem de seguir `<texto>_<voz>_<ambiente>_<take>.wav`, com `<texto>` sendo um ID de `etapa0/textos/` (T01 a T10, L30). Isso torna explícito que só se transcreve gravação de texto de teste (TRD, Privacidade nos testes).
- **Modelo:** `--modelo <diretório>`, obrigatório no modo real e fora do repositório. O modelo grande é GPLv3 e grande demais, e fica só na ferramenta de teste (TESTES-ETAPA0 passo 1). O código abre o modelo só por caminho, nunca por nome ou idioma, porque esses modos do pacote `vosk` baixam o modelo da internet.
- **Teste sem Vosk:** o laço WAV → reconhecedor → palavras recebe o reconhecedor por injeção. Os testes usam um falso que devolve o JSON documentado (`{"result":[{"word","start","end","conf"}],"text"}`). Sem o pacote `vosk`, o modo real sai com código 3 e uma mensagem dizendo o que falta. A confirmação contra o JSON real é da 0005 (Q4).

**D6. Como o `alinhar.py` escolhe a forma falada e alinha (Needleman–Wunsch em nível de palavra, TESTES-ETAPA0 passo 2).** O documento não dá pontuação.
- (a) Pontuação ponderada por similaridade de Levenshtein com limiar. Exige limiar, que não existe nos documentos.
- (b) Custo unitário de edição em nível de palavra (igual 0; substituição, inserção e omissão 1), que é o Needleman–Wunsch clássico sem parâmetro livre. Cada token entra com exatamente uma de suas formas faladas, a que minimiza o custo total. A igualdade de palavras é exata depois do normalizador.
- **Escolha: (b).** Não inventa número, e a revisão manual (passo 3) corrige o que errar.
- **Tempo do token:** um token recebe tempo só se pelo menos uma palavra da forma escolhida casar exatamente com uma palavra da transcrição. Assim, palavras de improviso ou de terceiros não dão tempo a token por substituição. `t_inicio_ms` é o início da primeira palavra da transcrição alinhada ao token (igualdade ou substituição), e `t_fim_ms` é o fim da última. Token sem casamento fica sem tempo, com as duas colunas vazias ("tokens pulados ou não falados ficam sem tempo").
- **Desempate determinístico:** entre alinhamentos de mesmo custo, vale a primeira forma falada do token (mesma regra do Kotlin, OPR-011), e as palavras do roteiro casam com a primeira ocorrência na transcrição. Por exemplo, numa frase repetida para ênfase (T08), o token fica com o tempo da primeira vez em que foi dito, e a repetição conta como inserção.
- **Desempenho:** o L30 tem cerca de 3.600 tokens contra cerca de 4.000 palavras. O DP completo cabe, mas pode levar dezenas de segundos em Python puro. Faixa (banda) ou outra otimização ficam a critério do arquiteto, desde que os critérios sintéticos deem o mesmo resultado. O tempo do L30 sintético fica registrado na evidência, sem meta, porque nenhum documento dá uma.

**D7. Coluna `palavra` do `expected.csv`.**
- **Escolha:** o texto exibido do token passado pelo normalizador ("Ouvir" → "ouvir", como no exemplo do TESTES-ETAPA0; "3:16" → "3 16"). O normalizador não deixa vírgula, então o CSV não precisa de aspas. A coluna serve para a revisão manual e não entra nas métricas. A saída tem todos os tokens do roteiro, na ordem, inclusive os sem tempo. Vai para stdout ou para um arquivo com `-o`.

**D8. O `alinhar.py` aceita qualquer texto?**
- (a) Sim. (b) Só um arquivo de conteúdo idêntico a um de `etapa0/textos/`. Outro texto sai com código 2 sem escrever nada.
- **Escolha: (b).** É a mesma guarda da exportação da 0003 (D9 de lá) e impede que um discurso pessoal vire `expected.csv` no repositório (TRD, Privacidade nos testes; CLAUDE.md "Textos de teste"). Custa uma comparação.

**D9. Resumo para a revisão manual.**
- **Escolha:** o `alinhar.py` escreve no stderr as contagens (tokens com tempo, tokens sem tempo, palavras inseridas) e as faixas de tokens consecutivos sem tempo e de palavras inseridas consecutivas, com os tempos. Sem limiar de tamanho, todas as faixas saem. É o roteiro da revisão do passo 3. Só aparecem palavras de textos de teste (D8).

**D10. Relógio comum entre trajetória e esperado.** O `expected.csv` está no relógio do WAV. A trajetória e os eventos do app estão em ms desde o `startCapture`. Com a fonte WAV da 0003, os dois coincidem por construção (0003 D6/D8). Com o microfone ao vivo, não coincidem.
- **Escolha:** o `metricas.py` aceita `--deslocamento-ms N` (padrão 0), somado ao `t_ms` da trajetória, dos eventos e das hipóteses para levar tudo ao relógio do esperado. Não é limiar, é dado da gravação. Como medir N (palma, marca no áudio) é da 0005. A ferramenta supõe que trajetória, eventos e hipóteses estão no mesmo relógio (o do app).

**D11. Folha de eventos manual e eventos automáticos do app.** O app exporta `<nome>.events.csv` com pausa, retomada e toque. A folha manual (improviso, silêncio, pulo) tem o mesmo nome e formato.
- **Escolha:** o `metricas.py` aceita um ou mais arquivos de eventos (`metricas.py <trajetoria> <esperado> <eventos> [<eventos>...]`) e junta todos, ordenados por `t_ms`. A forma documentada no `CLAUDE.md` continua valendo. Evento fora da lista do TESTES-ETAPA0, `detalhe` inválido em `toque_palavra` ou `pulo_inicio`, ou par desfeito (início sem fim, fim sem início, intervalos do mesmo tipo sobrepostos) é erro de entrada: código 2.

**D12. Amostragem e definições básicas (TESTES-ETAPA0, Métricas).**
- Amostras em `t = 0, 100, 200, …` ms até o maior entre o último `t_fim_ms` do esperado e o último `t_ms` da trajetória.
- `esperado(t)` = maior índice de token com `t_fim_ms ≤ t`. Antes do primeiro, vale −1 (literal: nenhum token falado). Tokens sem tempo não contam.
- `cursor(t)` = cursor da última linha da trajetória com `t_ms ≤ t`. A trajetória tem de começar por uma linha `Reposition` com `t_ms` ≤ a primeira amostra (formato da 0003). Senão, código 2.
- Intervalos de exceção, fechados no início e abertos no fim: improviso `[improviso_inicio, improviso_fim)`, silêncio `[silencio_inicio, silencio_fim)`, pausa `[pausa_toque, retoma_toque)` e pós-pulo `[pulo_fim, pulo_fim + 3000)`. Os 3 s vêm do TESTES-ETAPA0.
- Movimento automático = linha da trajetória com `move` ∈ {`Short`, `LongJump`, `Recovery`}. `Reposition` é manual. `Stay` não muda o cursor, e uma linha `Stay` é aceita e ignorada.

**D13. Atraso.** `atraso(t) = esperado(t) − cursor(t)`, contado só nas amostras de leitura normal (fora dos intervalos de exceção e das janelas de toque de D15). Passa se a fração de amostras com atraso ≤ 3 for ≥ 95%. Atraso negativo (cursor à frente) conta como ≤ 3, e o "à frente" é cobrado em D14. O relatório traz a fração, o número de amostras e o maior atraso. Se a gravação é de ambiente silencioso, o que o critério de saída exige, é decisão da 0005 ao montar o relatório. A ferramenta só calcula.

**D14. Salto errado.** Conta (a) toda amostra com `cursor(t) − esperado(t) > 2` em toda a trajetória, menos as janelas de toque (D15), e (b) todo movimento automático com `t_ms` dentro de improviso, silêncio ou pausa. Critério: zero. O relatório lista cada ocorrência (tempo, cursor, esperado, `move`).
- **Atraso do reconhecedor no começo de um intervalo:** o Vosk entrega as últimas palavras lidas antes do improviso ou do silêncio algumas centenas de ms depois da marca de início. O movimento que só alcança essas palavras cai dentro do intervalo. A leitura literal do TESTES-ETAPA0 ("qualquer movimento automático durante improviso, silêncio ou pausa") conta esse movimento como salto errado. **Escolha:** manter a leitura literal no veredito, sem afrouxar o critério (regra de processo 6). Cada movimento no intervalo sai classificado como "alcance" (cursor final ≤ esperado congelado no início do intervalo) ou "além do esperado", para a 0005 e o humano julgarem. Se a 0005 encontrar alcances que reprovam, a tolerância vira proposta de texto do TESTES-ETAPA0 e dos critérios de saída, com decisão humana. Registrado como OPR, dono analista.

**D15. Toque em palavra (L30 min 22; PRD "Tocar numa palavra 3 parágrafos à frente").** Depois de `toque_palavra` com detalhe *i*, o cursor vai a *i* por `reposition`, mas `esperado(t)` continua no último token falado antes do toque até o orador falar *i*. Pela definição literal, isso seria salto errado (cursor bem à frente) e atraso negativo grande. Nenhum dos dois é movimento automático, e o comportamento é o que o PRD pede.
- (a) Literal: o L30 reprova sempre. (b) Corrigir `esperado` para `i − 1` depois do toque, o que inventa posição. (c) Excluir das verificações de atraso e de "à frente" a janela `[t do toque, t_fim do primeiro token com tempo e índice ≥ i)` (até o fim, se não houver). Movimentos automáticos na janela continuam contados na verificação de recuo (D17), e as amostras excluídas aparecem no relatório.
- **Escolha: (c).** É interpretação, não mudança de critério: o salto do toque é do orador, e o TESTES-ETAPA0 foi escrito pensando em movimento automático. Não precisa de número novo. O texto proposto para o TESTES-ETAPA0 vai como OPR (dono analista), pendente de aprovação humana e sem bloquear, no mesmo padrão de OPR-007 e OPR-009.

**D16. Recuperação de pulo.** Para cada `pulo_fim`, o tempo de recuperação é `t − pulo_fim` da primeira amostra com `t ≥ pulo_fim` e atraso ≤ 3. Passa se for ≤ 3000 ms. Sem amostra assim até o fim, "não recuperou" (reprova). A resolução é de 100 ms, a da amostragem. O `pulo_inicio` é validado (D11) e sai no relatório, mas não entra na conta.

**D17. Recuo.** Pelo histórico da trajetória: `hw` começa no cursor de cada `Reposition` (que redefine cursor e highWater, TRD Contratos), e cada movimento automático é conferido contra o `hw` de antes dele: cursor novo < `hw − 3` é violação. Depois da conferência, `hw = max(hw, cursor novo)`. Critério: zero. É a invariante do TRD vista de fora, sem depender do `AlignState`.

**D18. Latências (TRD Metas: trechos 1 a 3; 0003 D6 deixou a latência 1 para cálculo offline).** As exportações não trazem as latências 2 e 3 (estão só no painel).
- **Fala → cursor (sempre):** para cada token *i* com tempo cujo `t_fim_ms` cai em leitura normal e que o cursor alcança por movimento automático, `L(i)` = primeiro instante em que o cursor fica ≥ *i* (por linha da trajetória) − `t_fim_ms(i)`. Pode ser negativo. Sai como contagem, mediana, p95 e máximo. Corresponde à "latência total fala → rolagem, sem a animação" do TRD.
- **Latência 1 e 2 (com `--hipoteses <hypotheses.csv>`):** `p(i)` = `t_ms` da primeira linha com `accepted = 1` e `kind` ∈ {`partial`, `final`}, com `t_ms ≥ t_inicio_ms(i)`, cujas palavras normalizadas contêm uma forma falada de *i* como subsequência contígua. Latência 1 = `p(i) − t_fim_ms(i)`, e latência 2 = (primeiro instante com cursor ≥ *i*) − `p(i)`. Para não confundir ocorrências, só entram os tokens elegíveis: nenhuma palavra de nenhuma forma do token aparece em forma falada de outro token do roteiro. A regra não tem parâmetro e exclui as palavras comuns e as repetidas. A precisão é limitada pelo carimbo na recepção (0003 D6).
- **Sem veredito:** as metas das latências são "a fixar" ou provisórias no TRD. O relatório mostra a meta provisória de 500 ms ao lado do p95 da fala → cursor, como informação, e as latências não mudam o código de saída. A latência 3 (animação) é igual à duração configurada (`animation_ms` do `meta.txt`) e é lida no painel pela 0005. O início da escuta também fica no painel. As duas ficam fora da ferramenta.

**D19. Relatório e código de saída do `metricas.py`.**
- **Escolha:** relatório em texto (Markdown) no stdout com os quatro critérios (atraso, salto errado, recuperação, recuo), cada um com PASSA ou REPROVA e os números. Também traz as latências (D18), o cursor final contra o último token do roteiro (informativo, ligado ao "Fim" do L30), as amostras excluídas por toque e a proveniência: nome e SHA-256 de cada arquivo de entrada e o deslocamento usado (Q8). Com `--json <arquivo>`, os mesmos números vão em JSON, que é o que os testes conferem.
- **Códigos de saída:** 0 = todos os critérios passam; 1 = algum reprova; 2 = entrada inválida (cabeçalho, `move` desconhecido, `t_ms` decrescente, evento inválido); 3 = dependência ausente (só no `timestamps.py`, modo real). Um critério sem dado (sem `pulo_fim`, a recuperação) sai como "não se aplica" e não reprova.

**D20. Dados de teste.** Só sintéticos, gerados dentro dos testes a partir de `etapa0/textos/`: transcrição feita da primeira forma falada de cada token (ou de outra forma, nos casos do T02), com tempos fixos por palavra, e perturbações (parágrafo omitido como no T05, palavras de outro texto de teste inseridas como no T04, frase repetida como no T08). As trajetórias e as folhas de eventos são montadas a partir do esperado. Nenhum WAV real nem transcrição real (são da 0005). O WAV dos testes do `timestamps.py` é gerado no próprio teste (silêncio ou tom), e o conteúdo vem do reconhecedor falso. Os arquivos de referência de D3 são os únicos dados novos versionados.

**D21. Rede e privacidade nas ferramentas.**
- **Escolha:** nenhuma ferramenta acessa rede (`urllib`, `http`, `socket`, `requests`, nem os modos do pacote `vosk` que baixam modelo). Nenhuma escreve áudio: o `timestamps.py` só lê o WAV. As ferramentas só escrevem no stdout, no stderr e no arquivo de `-o`/`--json`. O que for verificável vai por `grep` no spec.

**D22. Onde ficam as saídas reais.** Seguem o layout do TESTES-ETAPA0: `words.csv` ao lado do WAV, em `gravacoes/`, porque é a "transcrição bruta" que vira golden. O `expected.csv` vai para `esperado/` e o relatório para `relatorios/`. Quem grava lá é a 0005, com `-o`. As ferramentas não escolhem diretório sozinhas.

## Linha entre comando e 0005

- **Por comando (bloqueia a 0004):** testes `unittest` com dados sintéticos das três ferramentas, paridade Kotlin ↔ Python pelos arquivos de referência (teste JVM e teste Python), cadeia ponta a ponta sintética (`words.csv` → `alinhar.py` → `metricas.py`), códigos de saída, guardas de entrada e greps de rede e de áudio.
- **Na 0005 (não bloqueia):** instalar o pacote `vosk` e baixar o modelo grande (portão humano 6 lá), rodar o `timestamps.py` nos WAV reais, conferir o JSON real (Q4), revisar à mão os `expected.csv`, medir o deslocamento ao vivo e produzir os relatórios.

## Perguntas que caem em portão humano

Nenhuma nesta tarefa. Aviso para o orquestrador sobre a 0005: rodar o `timestamps.py` de verdade exige instalar o pacote `vosk` do PyPI (num `.venv` do projeto ou no sistema) e baixar o modelo grande de português (GPLv3, fora do repositório). As duas coisas são portão humano 6 quando a 0005 começar.
