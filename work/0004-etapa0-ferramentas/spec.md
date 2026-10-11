# Spec — 0004-etapa0-ferramentas

**Autor:** analista · **Data:** 2026-10-11 · **Aprovação:** conferida pelo orquestrador em 2026-10-11 (critérios por comando em dados sintéticos de etapa0/textos/, parâmetros só dos docs, só stdlib, sem portão humano; única mudança em Kotlin é teste JVM de referência, align/ de main intocado)

Brainstorm: `work/0004-etapa0-ferramentas/brainstorm.md`. As decisões D1–D22 citadas abaixo estão lá, com alternativas e motivo.

## O que e por quê

Três ferramentas Python que rodam no computador e fecham o ciclo de medição da Etapa 0 (PROCESS.md, Etapa 0, item 4):

- `timestamps.py <wav>`: tempo de cada palavra de uma gravação, pelo Vosk com `SetWords(True)`.
- `alinhar.py <texto> <transcricao>`: Needleman–Wunsch em nível de palavra, com as formas faladas do `Tokenizer`, gerando o `expected.csv`.
- `metricas.py <trajetoria> <esperado> <eventos>`: compara a trajetória do app com a esperada e dá o veredito dos critérios de voz.

Sem elas, a 0005 não consegue transformar gravações em números contra os critérios de saída. Para o orador nada muda: é instrumento de medição.

## Requisitos atendidos

- PRD: Métricas de sucesso (atraso ≤ 3 palavras em 95%, zero saltos errados); Motor de acompanhamento §Pipeline 3 (normalização e formas faladas); Critérios de aceite do MVP (pulo em até 3 s, 30 s de silêncio sem movimento, toque 3 parágrafos à frente, pausa com alguém falando).
- TRD: Contratos §Token (formas faladas); Motor de voz §Normalização e tokens; Metas de desempenho (latências 1 e 2, latência total, atraso, saltos errados); Testes §Privacidade nos testes; Etapa 0 §Critérios de saída (as quatro primeiras linhas, que a 0005 mede com estas ferramentas).
- `docs/TESTES-ETAPA0.md`: Folha de eventos, Trajetória esperada (passos 1, 2 e 4), Métricas: como calcular, layout de `etapa0/`.
- Exportações da 0003 (`work/0003-etapa0-vosk/spec.md`, D6–D11): `trajectory.csv`, `events.csv`, `hypotheses.csv`, `meta.txt`.
- Registro: atende a exigência de paridade com a 0002 (formas faladas). Segue OPR-011 no desempate (vale a primeira forma) e trata OPR-020 (transcrição com acento) e OPR-025 (hífen), porque o normalizador e o Tokenizer em Python repetem os do Kotlin. Abre dois OPR novos (ver "Achados para o registro").
- Contratos e app: nada muda. Nenhum contrato do TRD, nenhum manifesto, nenhuma dependência Gradle e nenhum arquivo de `align/` em `main`. A única mudança no Kotlin é um teste novo em `etapa0/core/src/test` (D3).

### Interpretações e valores que este spec fixa (do brainstorm)

- **Só biblioteca padrão** (D1). O pacote `vosk` é importado só no modo real do `timestamps.py`. Testes com `unittest`, rodados por `python3 etapa0/ferramentas/testar.py` (D2).
- **Paridade por arquivos de referência gerados do Kotlin** (D3): um por texto de `etapa0/textos/` (índice, intervalo exibido e formas faladas de cada token, na ordem) e um de números (`cardinalForms` e `masculineForms` de 0 a 2.100 e de uma lista fixa de milhares até 999.999, definida no plano). Um teste JVM e um teste Python comparam cada lado com a mesma referência.
- **Formatos.** Transcrição: `palavra,t_inicio_ms,t_fim_ms,conf`, com a palavra crua do Vosk (D5). Esperado: `token,palavra,t_inicio_ms,t_fim_ms`, com todos os tokens e `palavra` = texto exibido normalizado; token sem tempo fica com as colunas de tempo vazias (D7). Saída no stdout ou em `-o`.
- **Alinhamento** (D6): custo unitário de edição em nível de palavra, igualdade exata depois do normalizador e uma forma falada por token (a de menor custo). O token recebe tempo só se ao menos uma palavra casar exatamente. No empate, vale a primeira forma do token e a primeira ocorrência na transcrição.
- **Guardas de privacidade** (D5, D8, D21): o `alinhar.py` só aceita texto idêntico a um de `etapa0/textos/`. O `timestamps.py` só aceita WAV 16 kHz, mono, PCM 16 bits, com nome `<ID>_<voz>_<ambiente>_t<n>.wav` e `<ID>` de `etapa0/textos/`. O modelo abre só por caminho (`--modelo`). Nenhuma ferramenta acessa rede nem escreve áudio.
- **Métricas** (D10–D17), sobre o TESTES-ETAPA0:
  - Amostra a cada 100 ms, de 0 até o maior entre o último `t_fim_ms` e o último `t_ms` da trajetória.
  - `esperado(t)` = maior token com `t_fim_ms ≤ t` (−1 antes do primeiro). `cursor(t)` = última linha da trajetória com `t_ms ≤ t`.
  - Intervalos de exceção `[início, fim)`: improviso, silêncio, pausa (`pausa_toque`→`retoma_toque`) e 3 s depois de `pulo_fim`.
  - **Janela de toque:** de `toque_palavra` *i* até o `t_fim_ms` do primeiro token com tempo e índice ≥ *i*. Fica fora das verificações de atraso e de "à frente", e as amostras excluídas aparecem no relatório (D15).
  - Movimento automático = `Short`, `LongJump`, `Recovery`.
  - **Atraso:** passa com ≥ 95% das amostras de leitura normal com atraso ≤ 3.
  - **Salto errado:** amostra com cursor > esperado + 2 fora das janelas de toque, ou movimento automático dentro de improviso, silêncio ou pausa (leitura literal; cada um classificado como "alcance" ou "além do esperado"). Passa com zero.
  - **Recuperação:** da `pulo_fim` à primeira amostra com atraso ≤ 3. Passa com ≤ 3000 ms.
  - **Recuo:** movimento automático com cursor < `hw − 3`, sendo `hw` o maior cursor desde o último `Reposition`, tomado antes do movimento. Passa com zero.
- **`--deslocamento-ms N`** (padrão 0) leva trajetória, eventos e hipóteses ao relógio do esperado (D10). Um ou mais arquivos de eventos, juntados e ordenados (D11).
- **Latências** (D18), informativas, sem veredito:
  - Fala → cursor por token, sempre.
  - Latência 1 e latência 2 só com `--hipoteses`, e só nos tokens cujas palavras não aparecem em forma falada de outro token.
  - Mediana, p95 e máximo, com a meta provisória de 500 ms mostrada ao lado.
- **Saída do `metricas.py`** (D19): relatório Markdown no stdout, `--json <arquivo>` opcional, proveniência com SHA-256 das entradas. Códigos de saída: 0 passa, 1 reprova, 2 entrada inválida, 3 dependência ausente (só `timestamps.py`).

## Critérios de aceite

Os comandos rodam da raiz do repositório, e a saída de cada um vai anexada em `work/0004-etapa0-ferramentas/evidencias/`. "Teste" quer dizer `unittest`, rodado por `python3 etapa0/ferramentas/testar.py`, sem o pacote `vosk`, sem rede e só com dados sintéticos gerados no teste a partir de `etapa0/textos/` (D20).

### A. Execução, dependências e regras (por comando)

- [ ] A1. `python3 --version` fica registrado na evidência. `python3 etapa0/ferramentas/testar.py` termina com código 0 e mostra o total de testes executados, sem nenhum pulado (`skip`) nem falho. A contagem tem ≥ 1 teste por critério das seções B a E (Q3).
- [ ] A2. Só biblioteca padrão: `grep -rhoE "^\s*(import|from) [a-zA-Z_]+" etapa0/ferramentas --include=*.py` lista só módulos da stdlib, módulos das próprias ferramentas e `vosk`. O `vosk` aparece só dentro da função do modo real do `timestamps.py`, nunca no topo do módulo.
- [ ] A3. Sem rede (D21): `grep -rnE "urllib|http\.|socket|requests|model_name|lang=" etapa0/ferramentas --include=*.py` não devolve nada.
- [ ] A4. Sem áudio escrito (regra 3): `grep -rnE "wave\.open\([^)]*['\"]w|\.writeframes|setnchannels" etapa0/ferramentas --include=*.py` só devolve linhas de arquivos de teste que geram o WAV sintético em diretório temporário. O revisor confirma que nenhuma ferramenta (fora dos testes) escreve áudio.
- [ ] A5. As três ferramentas respondem a `--help` com código 0 (`python3 etapa0/ferramentas/timestamps.py --help`, `alinhar.py --help`, `metricas.py --help`), e a ajuda lista os argumentos posicionais e as opções deste spec.
- [ ] A6. `git diff main --stat -- etapa0/core/src/main etapa0/app docs/TRD.md` não mostra nada: o núcleo, o app e os contratos ficam intocados. Fora de `etapa0/ferramentas/`, só mudam `etapa0/core/src/test` (D3) e os artefatos em `work/0004-etapa0-ferramentas/`.

### B. Paridade com o Kotlin (D3, D4)

- [ ] B1. Existe um arquivo de referência por texto de `etapa0/textos/` (T01 a T10 e L30) e um de números, gerados pelo `Tokenizer`/`NumberWords` Kotlin. A regeneração está documentada no plano e não acontece sozinha dentro de um teste.
- [ ] B2. Lado Kotlin: `cd etapa0 && ./gradlew :core:test` termina verde, com um teste novo que compara o `Tokenizer.tokenize` de cada texto (índice, `display`, `spoken`) e as formas de `NumberWords` com a referência. Alterar uma forma no Kotlin deixa esse teste vermelho (Q2; quebra registrada pelo QA).
- [ ] B3. Lado Python: um teste compara o tokenizador Python de cada texto e as formas de números do Python com as mesmas referências e passa. Alterar uma forma no Python deixa esse teste vermelho (Q2; quebra registrada pelo QA, por exemplo tirar "quatorze" ou a forma feminina).
- [ ] B4. Normalizador: para cada texto de `etapa0/textos/`, `normalizar(normalizar(x)) == normalizar(x)`. Também `normalizar("Três, Versículo!") == "tres versiculo"`, `normalizar("bom-dia") == "bomdia"` e `normalizar("escrevê-los") == "escrevelos"`, igual ao `Normalizer.kt`.

### C. `timestamps.py` (D5)

- [ ] C1. Com um reconhecedor falso injetado, que devolve `{"result":[{"word":"ouvir","start":1.21,"end":1.53,"conf":1.0},{"word":"bem","start":1.53,"end":1.79,"conf":0.98}],"text":"ouvir bem"}` num `Result()` e uma palavra num `FinalResult()`, a saída tem o cabeçalho `palavra,t_inicio_ms,t_fim_ms,conf` e as linhas `ouvir,1210,1530,1.0`, `bem,1530,1790,0.98` e a do final, em ordem de tempo.
- [ ] C2. A palavra sai como o reconhecedor devolveu: `três` continua `três` (sem normalizar, golden bruto).
- [ ] C3. Um WAV sintético de 16 kHz, mono, PCM 16 bits, gerado no teste, é aceito, e todos os seus quadros chegam ao reconhecedor falso (a soma dos bytes entregues dá o tamanho dos dados). Um WAV de 44,1 kHz, um estéreo e um de 8 bits saem com código 2, sem nada no stdout.
- [ ] C4. Nome fora do padrão (`discurso.wav`, `X99_giorgio_sala_t1.wav`) sai com código 2. `T02_giorgio_salao_t2.wav` é aceito.
- [ ] C5. Sem o pacote `vosk` (o teste força a falha do import), o modo real sai com código 3 e a mensagem cita `vosk`. Sem `--modelo`, o modo real sai com código 2.

### D. `alinhar.py` (D6–D9)

As transcrições são sintéticas: palavras das formas faladas dos tokens de `etapa0/textos/`, com tempos fixos (por exemplo, palavra *n* em `[300n, 300n + 250]` ms) e no formato de C1.

- [ ] D1. **Leitura limpa (T01):** transcrição com a primeira forma de cada token. O `expected.csv` tem o cabeçalho `token,palavra,t_inicio_ms,t_fim_ms`, uma linha por token do T01 na ordem, todos com tempo, e cada tempo igual ao das palavras que o geraram. Com a palavra *n* em `[300n, 300n + 250]`, a primeira linha de dados é `0,ouvir,0,250`.
- [ ] D2. **Variantes de referência (T02):** com "tres dezesseis", depois "tres e dezesseis" e depois "capitulo tres versiculo dezesseis" no lugar de "3:16" (três transcrições), o token de "3:16" recebe `t_inicio_ms` da primeira palavra da variante e `t_fim_ms` da última, nas três. O mesmo vale para "13:4-7" com "treze de quatro a sete" e para "1 Coríntios" com "primeira corintios".
- [ ] D3. **Pulo de parágrafo (T05):** sem as palavras do 2º parágrafo do T01, os tokens desse parágrafo ficam sem tempo (colunas vazias), e os do 1º e do 3º ao 5º ficam com os tempos certos.
- [ ] D4. **Improviso (T04):** com palavras do T03 inseridas depois do fim do 1º parágrafo do T01, nenhum token recebe tempo de palavra inserida, e o stderr lista a faixa de palavras inseridas com seus tempos.
- [ ] D5. **Repetição (T08):** com "isso tem um custo" dito duas vezes, os tokens da frase ficam com os tempos da primeira vez, e a repetição sai no stderr como palavras inseridas.
- [ ] D6. **Refrão (T03):** cada uma das três ocorrências de "E a escolha é sempre nossa" recebe os tempos da sua ocorrência na transcrição (tokens distintos, tempos crescentes).
- [ ] D7. **Acento e hífen (OPR-020, OPR-025):** a transcrição com `três`, `Versículo` e `bom dia` (separado) casa com os tokens de "3:16" (variante com "capítulo") e "bom-dia", que recebem tempo.
- [ ] D8. **Guarda (D8):** um texto com um caractere a mais ou a menos que qualquer arquivo de `etapa0/textos/` sai com código 2, sem nada no stdout e sem criar o arquivo de `-o`.
- [ ] D9. **Determinismo:** rodar duas vezes a mesma entrada dá saídas idênticas byte a byte. O tempo de execução do L30 sintético completo fica registrado na evidência, sem meta.

### E. `metricas.py` (D10–D19)

As trajetórias e as folhas de eventos são montadas no teste a partir de um esperado sintético. Os números do veredito vêm do JSON de `--json`.

- [ ] E1. **Atraso, passa:** trajetória com o cursor 2 tokens atrás do esperado o tempo todo: fração com atraso ≤ 3 = 100%, critério PASSA, código 0.
- [ ] E2. **Atraso, limite:** com atraso 4 em exatamente 5% das amostras de leitura normal, passa (95% ≤ 3). Com atraso 4 em 6%, reprova, código 1. As amostras dentro de improviso, silêncio, pausa e dos 3 s depois de `pulo_fim` não entram na conta (um atraso de 50 só nesses intervalos não muda a fração).
- [ ] E3. **Salto errado por posição:** uma linha `Short` que deixa o cursor 3 à frente do esperado por 200 ms conta pelo menos 1 salto errado e reprova. Com 2 à frente, não conta.
- [ ] E4. **Salto errado por movimento no intervalo:** um `Short` dentro de `[silencio_inicio, silencio_fim)` conta 1 salto errado mesmo que o cursor não passe do esperado, sai classificado como "alcance" e reprova. O mesmo `Short` 1 ms depois de `silencio_fim` não conta. Vale também para improviso e pausa.
- [ ] E5. **Toque em palavra (D15):** `toque_palavra` com detalhe *i* 3 parágrafos à frente (T01), linha `Reposition` para *i* e leitura que continua de *i*: nenhum salto errado, e as amostras da janela até o `t_fim_ms` de *i* aparecem como excluídas no relatório. Variante: um `Short` para *i* + 3 dentro da janela, antes de *i* ser falado, não conta salto durante a janela. Na primeira amostra depois dela (cursor *i* + 3, esperado *i*), conta 1 salto errado por posição, e o critério reprova.
- [ ] E6. **Recuperação:** com `pulo_fim` em *t* e atraso ≤ 3 a partir de *t* + 2900 ms, recuperação = 2900 ms, PASSA. A partir de *t* + 3100 ms, 3100 ms, REPROVA. Sem nunca chegar, "não recuperou", REPROVA. Sem `pulo_fim`, "não se aplica" e o código não muda por isso.
- [ ] E7. **Recuo:** depois de chegar ao cursor 40 por movimentos automáticos, um `Short` para 36 é violação, e para 37 não é. Depois de um `Reposition` para 10, um `Short` para 12 não é violação.
- [ ] E8. **Deslocamento (D10):** a mesma trajetória e os mesmos eventos com todos os `t_ms` diminuídos de 1500 e rodados com `--deslocamento-ms 1500` dão o mesmo JSON de critérios que o original.
- [ ] E9. **Vários arquivos de eventos (D11):** a folha manual (improviso) e o `events.csv` do app (pausa e retomada) passados como dois argumentos dão o mesmo resultado que um arquivo só com as quatro linhas.
- [ ] E10. **Entrada inválida → código 2:** cabeçalho errado em qualquer arquivo, `move` fora dos nomes de `Move`, `t_ms` decrescente na trajetória, trajetória que não começa por `Reposition`, evento fora da lista do TESTES-ETAPA0, `toque_palavra` sem índice inteiro, `improviso_inicio` sem `improviso_fim`.
- [ ] E11. **Latências (D18):** com o cursor alcançando cada token 400 ms depois do `t_fim_ms`, a fala → cursor dá mediana = p95 = máximo = 400 ms. Com `--hipoteses` (cada palavra aparecendo num parcial `accepted=1` 150 ms depois do seu `t_fim_ms`), latência 1 = 150 ms e latência 2 = 250 ms nos tokens elegíveis. Linhas `accepted=0` são ignoradas, e as palavras comuns ("de", "que", "e", "o", "a") e os tokens repetidos no roteiro ficam de fora. As latências não mudam o código de saída.
- [ ] E12. **Relatório:** o stdout traz os quatro critérios com PASSA/REPROVA, as latências, o cursor final contra o último token, o deslocamento e o nome e o SHA-256 de cada arquivo de entrada. O JSON de `--json` tem os mesmos números.
- [ ] E13. **Ponta a ponta sintética:** a transcrição sintética do T01 → `alinhar.py` → `expected.csv`; uma trajetória com atraso 1 sobre esse esperado → `metricas.py` com código 0 e os quatro critérios em PASSA (ou "não se aplica" na recuperação).

## Fora do escopo

- Rodar o `timestamps.py` com o Vosk real, instalar o pacote `vosk` e baixar o modelo grande. É da 0005, e as duas coisas são portão humano 6 quando a 0005 começar.
- Gravações, WAV reais, revisão manual dos `expected.csv`, medição do deslocamento ao vivo, relatórios das rodadas e calibração (0005).
- Golden de transcrições como suíte de regressão do reconciliador e do alinhador (0006, OPR-008) e o teste instrumentado "Integração de voz" (0006, OPR-019).
- Latência 3 (animação, igual ao `animation_ms` do `meta.txt`), início da escuta, inicialização a frio, memória e bateria: ficam no painel e na 0005.
- Ferramenta que junte a folha manual de eventos com a do app num arquivo só. O `metricas.py` aceita vários arquivos (D11).
- Tolerância para o "alcance" no começo de improviso ou silêncio. O veredito fica literal (D14), e a mudança, se vier, é decisão humana (OPR novo).
- Qualquer mudança em `align/`, no app da etapa0, em contrato do TRD, no PRD ou no texto do TESTES-ETAPA0 nesta tarefa. Os textos propostos vão para o registro.
- Meta de tempo de execução das ferramentas.

## Mudança proposta no PRD (se houver)

Nenhuma.

## Achados para o registro (o orquestrador abre ao conferir; dono: analista)

1. **Janela do toque em palavra no TESTES-ETAPA0 (D15).** Texto proposto para "Métricas: como calcular", depois de "Salto errado": "Depois de `toque_palavra` (detalhe *i*), até o fim do token *i* na trajetória esperada, as amostras não entram no atraso nem na verificação de cursor à frente: o reposicionamento é do orador, não automático. Movimentos automáticos nessa janela continuam valendo para o recuo." O texto do documento fica pendente de aprovação humana e não bloqueia a 0004.
2. **"Alcance" no começo de improviso ou silêncio (D14).** O veredito literal conta como salto errado o movimento automático que só alcança as últimas palavras lidas antes do intervalo, entregues com o atraso do reconhecedor. A 0005 confere nos relatórios (classificação "alcance" ou "além do esperado"). Se isso reprovar gravações em que o texto não saltou, propor tolerância no TESTES-ETAPA0 e nos critérios de saída, com decisão humana.

## Perguntas em aberto

- Nenhuma nesta tarefa. Aviso: a 0005 vai precisar do portão humano 6 para instalar o pacote `vosk` (PyPI) e baixar o modelo grande de português (GPLv3, fora do repositório).
