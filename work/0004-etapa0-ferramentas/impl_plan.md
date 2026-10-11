# Plano — 0004-etapa0-ferramentas

**Autor:** arquiteto · **Spec:** `spec.md` (conferido pelo orquestrador em 2026-10-11) · **Aprovação:** conferida pelo orquestrador em 2026-10-11 (critérios mapeados a teste e comando, testes antes do código, Q6, só stdlib, nenhum portão humano; A3, A5, A6, A7 e A9 do OPR-034 são refinos de medição sem mudar número nem PRD/TRD; align/ de main intocado)

> **Para o construtor (`motor-voz`):** siga as tarefas na ordem, passo a passo (caixas `- [ ]`). Cada passo de código traz o arquivo inteiro: copie exatamente, rode o comando e compare com a saída esperada. Não execute nada que não esteja aqui (nada de `pip install`, download de modelo ou rede: é portão humano 6). Ao terminar, preencha o "Registro da construção" no fim deste arquivo. Quem revisa é o `revisor`; quem testa é o `qa-publicacao`.

**Goal:** as três ferramentas Python da Etapa 0 (`timestamps.py`, `alinhar.py`, `metricas.py`), só com a biblioteca padrão, testadas com dados sintéticos de `etapa0/textos/`, e a paridade das formas faladas com o Kotlin provada por arquivos de referência.

**Architecture:** um módulo comum (`roteiro.py`) repete o `Normalizer`/`NumberWords`/`Tokenizer` do Kotlin; um teste JVM novo gera o que o Kotlin produz e o compara com referências versionadas que o teste Python também lê. O `alinhar.py` é um Needleman–Wunsch completo (sem faixa) numa treliça de formas faladas, com desempate por número de trechos. O `metricas.py` amostra a cada 100 ms e dá o veredito dos quatro critérios de voz. O Vosk só é importado no modo real do `timestamps.py`.

**Tech Stack:** Python 3 (stdlib: `unittest`, `csv`, `wave`, `json`, `argparse`, `array`, `unicodedata`, `hashlib`, `statistics`, `bisect`), Kotlin/JUnit 4 no `:core` da `etapa0` (Gradle 9.7.1).

---

## Resumo

Três ferramentas encadeadas por arquivo (`words.csv` → `expected.csv` → relatório), com toda a lógica testável só com a stdlib e dados sintéticos. A paridade com o `Tokenizer` fica em 12 arquivos `.tsv` gerados pelo Kotlin e conferidos dos dois lados. A única mudança fora de `etapa0/ferramentas/` é um teste JVM e as referências em `etapa0/core/src/test/`. Só o `motor-voz` executa; não há trabalho da `interface`.

## Decisões do arquiteto

O spec deixou ao plano a otimização do alinhamento, a lista fixa de números, a forma de gerar as referências e os detalhes que só aparecem lendo o código real. Cada decisão abaixo diz o que estava em aberto, a escolha e o porquê. Nenhuma toca contrato do TRD, manifesto, `align/` de `main` ou o app.

### A1. Ambiente conferido por comando (spec A1, D1, D2)

```
$ python3 --version
Python 3.13.12
$ python3 -c "import pytest"   -> ModuleNotFoundError: No module named 'pytest'
$ python3 -c "import vosk"     -> ModuleNotFoundError: No module named 'vosk'
$ python3 -c "import numpy"    -> ModuleNotFoundError: No module named 'numpy'
```

Nada foi instalado (instalar é portão humano 6). O código usa só a biblioteca padrão e nada mais novo que Python 3.9 (dataclasses, f-strings, `math.ceil`, `statistics.median`); foi provado no 3.13.12. Os testes são `unittest`, rodados por `python3 etapa0/ferramentas/testar.py`, que sai com 1 se houver falha, erro ou teste pulado.

### A2. Paridade com o Kotlin: referências geradas, versionadas e conferidas dos dois lados (spec B1–B3, D3)

- **Onde:** `etapa0/core/src/test/resources/referencia/`, 12 arquivos: `T01.tsv` … `T10.tsv`, `L30.tsv` e `numeros.tsv`. Ficam em `core/src/test` porque o critério A6 só permite mudança fora de `ferramentas/` em `etapa0/core/src/test`, e porque assim o Gradle já trata as referências como entrada do teste (`processTestResources`): mudar uma referência faz o `:core:test` rodar de novo, sem `--rerun`. O Python lê o mesmo diretório pelo caminho (`roteiro.REFERENCIA`).
- **Formato** (UTF-8, fim de linha `\n`, termina com `\n`):
  - texto: 1ª linha `#texto=<ID> sha256=<sha256 dos bytes do .txt> tokens=<n>`; depois uma linha por token, `<index>\t<display.first>\t<display.last>\t<formas>`, com as formas separadas por `|` e as palavras de cada forma por espaço, na ordem do `Token.spoken`. O peso e o `sentenceStart` ficam de fora (D3). Os índices são de UTF-16 (Kotlin); o teste Python confere que os textos estão todos no BMP, onde UTF-16 e índice Python coincidem.
  - números: 1ª linha `#numeros=<n>`; depois `<n>\t<cardinalForms>\t<masculineForms>`. Lista fixa (B1): todos os inteiros de 0 a 2.100 e mais `m × 1000 + r` para `m` ∈ {2, 3, 10, 11, 14, 16, 20, 21, 100, 101, 114, 200, 201, 999} e `r` ∈ {0, 1, 14, 99, 100, 101, 114, 200, 999} (2.227 números; 999.999 é o máximo de D14 e entra como 999 × 1000 + 999). Cobre "catorze/quatorze", feminino de milhar ("duas mil"), "cem/cento", "e" entre milhar e resto, e o maior número.
- **Lado Kotlin** (`ReferenciaFormasTest`, em `etapa0/core/src/test/kotlin/.../align/`, mesmo pacote do `NumberWords`, que é `internal`): gera o conteúdo, grava em `etapa0/core/build/referencia-atual/` (fora do git) e compara linha a linha com o recurso versionado. Nunca escreve no diretório versionado.
- **Lado Python** (`testes/test_roteiro.py`): compara o `roteiro.tokenizar` e as formas de números com as mesmas referências.
- **Defasagem:** se um texto de `etapa0/textos/` mudar, o `sha256` do cabeçalho deixa de bater e os dois testes falham (o Python com a mensagem "referência defasada: <ID>.txt mudou; regenere"). Se o Kotlin mudar, o teste JVM falha. Se a referência for regenerada depois de mudar o Kotlin, o teste Python falha até o `roteiro.py` acompanhar. Um texto novo em `etapa0/textos/` sem referência também falha (`test_B1_...`).
- **Regenerar (ação explícita, nunca dentro de um teste):** ver a seção "Regenerar as referências". Na prova, as quebras K1–K5 (abaixo) deixaram os testes vermelhos.

### A3. Needleman–Wunsch: DP completo, sem faixa, com desempate por trechos (spec D1–D9; brainstorm D6)

- **Treliça:** uma linha por palavra de cada forma de cada token; as formas de um token partem da mesma fronteira e a fronteira seguinte é o mínimo entre as formas (cada token usa exatamente uma forma). Colunas = palavras normalizadas da transcrição.
- **Custo:** uma edição (substituição, inserção, omissão) vale 1; igual vale 0 (D6). Para o desempate, o valor de cada célula é `edições × peso + trechos`, com `peso = linhas + palavras + 2` (maior que qualquer número de trechos), e três estados por célula (casamento, inserção, omissão; Gotoh). **Um trecho é uma sequência contígua de inserções ou de omissões**; abrir um trecho custa `peso + 1`, estender custa `peso`.
- **Desempate (refina o D6 sem mudar o custo):** (1) menos edições; (2) menos trechos; (3) na volta do fim para o começo, inserção antes de casamento antes de omissão, que dá a primeira ocorrência na transcrição, e a forma de menor índice. **Por que o critério (2):** a regra "primeira ocorrência" sozinha dá o tempo errado no fim de um improviso. Se o improviso tem a palavra que abre o parágrafo seguinte (o T09 tem "por isso que estamos aqui"; o T01 retoma com "Isso tem um custo"), casar o token com a palavra do improviso e inserir a real custa as mesmas edições, e a primeira ocorrência escolheria a do improviso. Contando trechos, a versão errada tem dois trechos de inserção e a certa um. No T08 (frase repetida) os dois alinhamentos têm um trecho cada, e vale a primeira vez, como o D6 pede. Os casos estão em `DesempateTest` e `DesempateMinimoTest`. Registrado para o texto do spec no OPR-034 (c).
- **Desempenho (pedido do orquestrador):** o DP completo do L30 sintético (3.668 tokens, 3.721 linhas de treliça × 3.684 palavras, 13,7 milhões de células × 3 estados) levou **3,65 s** e ~190 MB no Apple M4 com Python 3.13.12 (`test_D9_l30_sintetico_completo`; com 130 palavras de improviso e 300 omitidas, 3,4 s). Por isso **não há faixa**: uma faixa só economizaria tempo arriscando mudar o resultado em pulos e improvisos longos (o L30 tem pulo de 2 parágrafos e 1 min de improviso), e o critério manda não mudar o resultado. Cada linha guarda três `array("i")` (4 bytes por célula) para a volta. Guarda: entrada com `(linhas + palavras + 1) × (peso + 1) ≥ 2^29` sai com código 2 (o L30 usa ~1/10 do limite).
- **Tempo do token:** só se ao menos uma palavra da forma escolhida casar exatamente (D6); `t_inicio_ms` da primeira palavra alinhada ao token (casamento ou substituição) e `t_fim_ms` da última.

### A4. Transcrição de entrada (spec D5, D7)

Cada linha de `words.csv` passa pelo normalizador: "Versículo" vira "versiculo", "bom-dia" vira "bomdia" (uma palavra), e uma linha que normaliza para nada não entra. Uma palavra que normalizasse para várias herdaria o mesmo tempo (não acontece com o Vosk, mas é a regra). Validação (código 2): cabeçalho exato, 4 campos, tempos inteiros ≥ 0, fim ≥ início (duração zero é aceita), início em ordem não decrescente, `conf` vazio ou número.

### A5. Recuperação de pulo: o esperado tem de ter saltado (refina D16; Q3)

Pela leitura literal ("do `pulo_fim` até a primeira amostra com atraso ≤ 3"), a recuperação daria **0 ms em toda gravação**: no instante do `pulo_fim` (início da primeira palavra depois do pulo) o esperado ainda está no último token antes do pulo, porque o primeiro token novo só termina ~250 ms depois, e o cursor também está lá; o atraso é ≤ 3 e o critério passaria sem medir nada. Escolha: `alvo` = primeiro token com tempo e índice ≥ detalhe do `pulo_inicio` (o primeiro token pulado); a recuperação é a primeira amostra `t ≥ pulo_fim` com `esperado(t) ≥ alvo` **e** atraso ≤ 3. Isso usa o `pulo_inicio`, que o D16 deixava só no relatório. O teste `test_E6_nunca_recupera_reprova_mesmo_com_atraso_baixo_no_pulo_fim` prova o problema da leitura literal. Texto proposto ao TESTES-ETAPA0 no OPR-034 (a) (dono: analista; não bloqueia).

### A6. Primeira amostra com deslocamento positivo (refina D12)

O D12 manda amostrar de 0 e recusar (código 2) a trajetória cuja primeira linha venha depois da primeira amostra. Com o microfone ao vivo e `--deslocamento-ms N > 0` (gravador ligado antes do app), a primeira linha cai em `N` e toda gravação seria recusada na 0005. Escolha: a primeira amostra é o primeiro múltiplo de 100 ≥ max(0, `t` da primeira linha depois do deslocamento). Com deslocamento 0 e a trajetória do app (começa em 0), é exatamente o "de 0" do spec. A trajetória que não começa por `Reposition` continua sendo código 2. Registrado no OPR-034 (b).

### A7. Janela do toque começa na `Reposition` que o precede (D15; Q4)

No `ReaderController` real (`etapa0/app/.../viewmodel/ReaderController.kt:144-152`), o toque faz `reposition(index)` (que grava a linha `Reposition` na trajetória) **antes** de gravar o evento `toque_palavra`. Se uma amostra caísse entre os dois carimbos, contaria salto errado. Escolha: se a última linha da trajetória com `t ≤ t_toque` for `Reposition` para o mesmo índice, a janela começa no `t` dessa linha. O fim é o do D15: `t_fim` do primeiro token com tempo e índice ≥ *i* (aberto), ou o fim da gravação.

### A8. Contagem do salto errado por posição

Uma ocorrência por posição é um **episódio**: amostras consecutivas com cursor > esperado + 2 (uma amostra fora da condição ou dentro de janela de toque encerra o episódio). O relatório traz início, fim, número de amostras e o maior avanço. Para o veredito (zero) dá no mesmo que contar amostras; a contagem por episódio só deixa o número legível ("1 salto que durou 200 ms", E3 e E5). Movimento automático em improviso, silêncio ou pausa conta um por linha da trajetória, com a classe "alcance" (cursor ≤ esperado no início do intervalo) ou "além do esperado" (D14).

### A9. Latências 1 e 2 sem opção nova (D18)

As latências 1 e 2 precisam das formas faladas (elegibilidade e casamento com as palavras do parcial). Em vez de uma opção `--texto`, o `metricas.py` acha o roteiro pela coluna `palavra` do `expected.csv`: o primeiro texto de `etapa0/textos/` cujos tokens normalizados dão exatamente essa coluna (T01, T04–T08 e T10 têm os mesmos tokens, então qualquer um serve). Com `--hipoteses` e nenhum texto correspondente, código 2. Sem `--hipoteses`, a fala → cursor sai do mesmo jeito. As linhas de hipótese entram ordenadas por `t_ms`; só `accepted = 1` e `kind` ∈ {`partial`, `final`}, com `t_ms ≥ t_inicio` do token.

### A10. Casos degenerados (Q3)

- Atraso sem nenhuma amostra de leitura normal: **REPROVA**, com o motivo no relatório (95% de zero amostras não é aprovação).
- `expected.csv` sem nenhum token com tempo: código 2.
- `p95` por posto mais próximo: `sorted(v)[ceil(0,95·n) − 1]`; mediana do `statistics.median`.
- A posição esperada "congelada" do TESTES-ETAPA0 (linha 132) coincide com a fórmula do spec (maior token com `t_fim ≤ t`) sempre que nenhum token termina dentro de improviso, silêncio ou pausa; não há regra extra.

### A11. Os `grep` do spec (A2–A4) precisam de aspas no zsh

`grep ... --include=*.py` sem aspas falha no zsh (o shell do usuário) com `no matches found` antes de rodar o grep, e uma saída vazia seria lida como "passou" (Q3). Os comandos da seção Verificação usam `--include='*.py'`. Registrado no OPR-035 (dono: analista).

## Prova do plano (feita pelo arquiteto em 2026-10-11)

Numa cópia descartável de `etapa0` no scratchpad, com o código exato deste plano:

- `cd etapa0 && ./gradlew :core:ktlintCheck :core:lint :core:test`: verde; `:core` com 120 testes (118 de antes + 2 do `ReferenciaFormasTest`), 0 pulados.
- `python3 etapa0/ferramentas/testar.py`: **87 testes, 0 falhas, 0 erros, 0 pulados**; L30 sintético alinhado em 3,65 s.
- Simulação tarefa a tarefa numa segunda cópia limpa (arquivos aplicados na ordem das Tarefas 1 a 6): cada "ver falhar" falhou com a mensagem prevista e cada "passar" passou (contagens nos passos); as referências geradas na simulação são idênticas às da prova.
- Quebras Q2: 106 trocas de texto no Python (tabela abaixo), 106 vermelhas, 0 sobreviventes; mais 5 quebras de dados e do Kotlin (K1–K5), todas vermelhas. As sobreviventes da primeira rodada (M2b, M5, L3, L8, L9, L12, T3) viraram testes novos (`test_E1_atraso_3_ainda_conta_como_dentro`, `DesempateMinimoTest`, `test_D7_palavras_com_acento_e_maiuscula_como_o_vosk_grande`, `test_C1_milissegundos_arredondados`).
- Greps A2–A4 (com aspas): só stdlib, módulos próprios e `vosk` (indentado, dentro de `_fabrica_vosk`); nenhuma linha de rede; `wave.open(..., "wb")`, `setnchannels` e `writeframes` só em `testes/test_timestamps.py`.

## Arquivos que mudam

| Arquivo | Mudança | Requisito / seção | Agente |
|---|---|---|---|
| `etapa0/core/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/align/ReferenciaFormasTest.kt` | novo: gera as formas do Kotlin e compara com as referências | spec B1, B2; D3; TRD Contratos §Token | motor-voz |
| `etapa0/core/src/test/resources/referencia/*.tsv` (12) | novos, gerados pelo comando de A2 | spec B1; D3 | motor-voz |
| `etapa0/ferramentas/roteiro.py` | novo: normalizador, números, tokenizador e catálogo dos textos | spec B3, B4, D8; TRD Motor de voz §Normalização e tokens | motor-voz |
| `etapa0/ferramentas/timestamps.py` | novo | spec C1–C5; TESTES-ETAPA0 Trajetória esperada passo 1 | motor-voz |
| `etapa0/ferramentas/alinhar.py` | novo | spec D1–D9; TESTES-ETAPA0 passos 2 e 4 | motor-voz |
| `etapa0/ferramentas/metricas.py` | novo | spec E1–E13; TESTES-ETAPA0 Métricas; TRD Metas de desempenho | motor-voz |
| `etapa0/ferramentas/testar.py` | novo: executor `unittest` | spec A1; D2 | motor-voz |
| `etapa0/ferramentas/testes/__init__.py`, `apoio.py`, `sintetico.py` | novos: apoio e dados sintéticos | spec D20 | motor-voz |
| `etapa0/ferramentas/testes/test_roteiro.py`, `test_timestamps.py`, `test_alinhar.py`, `test_metricas.py`, `test_cli.py` | novos: testes | spec A1, A5, B–E | motor-voz |
| `work/0004-etapa0-ferramentas/evidencias/*.txt` | saídas dos comandos | spec, "Critérios de aceite" | motor-voz |
| `work/0004-etapa0-ferramentas/impl_plan.md` | só o "Registro da construção" | PROCESS.md, Build | motor-voz |

Não mudam: `etapa0/core/src/main`, `etapa0/app`, `etapa0/core/build.gradle.kts`, `gradle/libs.versions.toml`, manifesto, `docs/` (o TESTES-ETAPA0 e o TRD ficam como estão; propostas vão para o registro). O `.gitignore` já ignora `__pycache__/`; o `etapa0/core/build/referencia-atual/` está dentro de `build/`, também ignorado.

## Ordem e paralelismo

Ordem do PROCESS.md adaptada à tarefa (não há `speech/`, `viewmodel/` nem `ui/`): referências do Kotlin (núcleo `align/`, só teste) → módulo comum → ferramentas.

1. Tarefa 1: `ReferenciaFormasTest` e as referências (Kotlin).
2. Tarefa 2: `roteiro.py`, `testar.py`, apoio dos testes e paridade Python.
3. Tarefa 3: `timestamps.py`.
4. Tarefa 4: `alinhar.py` e os dados sintéticos.
5. Tarefa 5: `metricas.py`.
6. Tarefa 6: linha de comando por subprocesso e ponta a ponta.
7. Tarefa 7: verificação completa e evidências.

Tudo é do `motor-voz`, em sequência (cada tarefa usa a anterior: 2 lê as referências de 1; 4 e 5 usam o `roteiro.py`; os testes de 5 usam o escritor do `alinhar.py`). Não há paralelismo útil nem trabalho da `interface`, e nenhum contrato do TRD é tocado.

## Testes primeiro

Cada tarefa escreve os testes, roda e vê falhar (módulo ausente ou referência ausente), e só então escreve o código. A Tarefa 6 é de integração (subprocesso e cadeia completa) e passa de primeira, porque junta peças já testadas; o vermelho dela é provado pelas quebras M49, L13 e T6.

### Critérios do spec → teste → comando

Todos os testes Python rodam por `python3 etapa0/ferramentas/testar.py` (filtro opcional por arquivo: `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'`).

| Critério | Teste(s) | Comando |
|---|---|---|
| A1 | registrar `python3 --version`; `testar.py` com código 0, sem pulados; ≥ 1 teste por critério de B a E (esta tabela) | `python3 --version; python3 etapa0/ferramentas/testar.py` |
| A2 | só stdlib, módulos próprios e `vosk` (dentro de `_fabrica_vosk`) | grep de imports da seção Verificação |
| A3 | sem rede | grep de rede da seção Verificação (`rc=1`) |
| A4 | sem áudio escrito fora dos testes | grep de áudio da seção Verificação |
| A5 | `--help` das três ferramentas<br>`test_cli.test_A5_ajuda_das_tres_ferramentas` | três `--help` + `test_cli.AjudaTest` |
| A6 | núcleo, app e TRD intocados | `git diff main --stat -- etapa0/core/src/main etapa0/app docs/TRD.md` |
| B1 | `test_roteiro.test_B1_ha_uma_referencia_por_texto_e_uma_de_numeros` | `testar.py 'test_roteiro*.py'` |
| B2 | teste JVM novo verde; quebra K1–K3 vermelha | `cd etapa0 && ./gradlew :core:test` (`ReferenciaFormasTest.formasDeCadaTexto_batemComAReferencia`, `formasDosNumeros_batemComAReferencia`) |
| B3 | `test_roteiro.test_B3_tokens_de_cada_texto_batem_com_a_referencia`<br>`test_roteiro.test_B3_formas_dos_numeros_batem_com_a_referencia`<br>`test_roteiro.test_B3_numero_fora_da_faixa_lanca_erro` | `python3 etapa0/ferramentas/testar.py 'test_roteiro*.py'` |
| B4 | `test_roteiro.test_B4_idempotente_em_todos_os_textos`<br>`test_roteiro.test_B4_exemplos_iguais_ao_Normalizer_kt` | `python3 etapa0/ferramentas/testar.py 'test_roteiro*.py'` |
| C1 | `test_timestamps.test_C1_csv_com_cabecalho_e_palavras_em_ordem`<br>`test_timestamps.test_C1_ordena_por_tempo`<br>`test_timestamps.test_C1_milissegundos_arredondados` | `python3 etapa0/ferramentas/testar.py 'test_timestamps*.py'` |
| C2 | `test_timestamps.test_C2_palavra_sai_como_o_reconhecedor_devolveu` | `python3 etapa0/ferramentas/testar.py 'test_timestamps*.py'` |
| C3 | `test_timestamps.test_C3_wav_valido_entrega_todos_os_quadros`<br>`test_timestamps.test_C3_formato_errado_sai_com_2_sem_stdout` | `python3 etapa0/ferramentas/testar.py 'test_timestamps*.py'` |
| C4 | `test_timestamps.test_C4_nome_fora_do_padrao_sai_com_2`<br>`test_timestamps.test_C4_nome_do_protocolo_e_aceito` | `python3 etapa0/ferramentas/testar.py 'test_timestamps*.py'` |
| C5 | `test_timestamps.test_C5_sem_pacote_vosk_sai_com_3`<br>`test_timestamps.test_C5_sem_modelo_sai_com_2` | `python3 etapa0/ferramentas/testar.py 'test_timestamps*.py'` |
| D1 | `test_alinhar.test_D1_t01_todos_os_tokens_com_o_tempo_das_suas_palavras`<br>`test_alinhar.test_D1_coluna_palavra_e_o_exibido_normalizado` | `python3 etapa0/ferramentas/testar.py 'test_alinhar*.py'` |
| D2 | `test_alinhar.test_D2_tres_formas_das_referencias` | `python3 etapa0/ferramentas/testar.py 'test_alinhar*.py'` |
| D3 | `test_alinhar.test_D3_pulo_de_paragrafo_t05` | `python3 etapa0/ferramentas/testar.py 'test_alinhar*.py'` |
| D4 | `test_alinhar.test_D4_improviso_t04_nao_da_tempo_a_token` | `python3 etapa0/ferramentas/testar.py 'test_alinhar*.py'` |
| D5 | `test_alinhar.test_D5_repeticao_t08_fica_com_a_primeira_vez` | `python3 etapa0/ferramentas/testar.py 'test_alinhar*.py'` |
| D6 | `test_alinhar.test_D6_refrao_t03_cada_ocorrencia_com_o_seu_tempo` | `python3 etapa0/ferramentas/testar.py 'test_alinhar*.py'` |
| D7 | `test_alinhar.test_D7_palavras_com_acento_e_maiuscula_como_o_vosk_grande`<br>`test_alinhar.test_D7_acento_e_hifen_da_transcricao` | `python3 etapa0/ferramentas/testar.py 'test_alinhar*.py'` |
| D8 | `test_alinhar.test_D8_texto_diferente_de_etapa0_textos_sai_com_2_sem_escrever` | `python3 etapa0/ferramentas/testar.py 'test_alinhar*.py'` |
| D9 | `test_alinhar.test_D9_mesma_entrada_mesma_saida`<br>`test_alinhar.test_D9_l30_sintetico_completo` | `python3 etapa0/ferramentas/testar.py 'test_alinhar*.py'` |
| E1 | `test_metricas.test_E1_dois_atras_o_tempo_todo_passa`<br>`test_metricas.test_E1_atraso_3_ainda_conta_como_dentro` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E2 | `test_metricas.test_E2_atraso_4_em_5_por_cento_passa`<br>`test_metricas.test_E2_atraso_4_em_6_por_cento_reprova`<br>`test_metricas.test_E2_intervalos_de_excecao_nao_entram_na_conta`<br>`test_metricas.test_E2_tres_segundos_depois_do_pulo_nao_entram_na_conta` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E3 | `test_metricas.test_E3_tres_a_frente_por_200_ms_conta`<br>`test_metricas.test_E3_dois_saltos_separados_contam_dois`<br>`test_metricas.test_E3_dois_a_frente_nao_conta` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E4 | `test_metricas.test_E4_movimento_automatico_no_intervalo_conta_como_alcance`<br>`test_metricas.test_E4_um_ms_depois_do_fim_nao_conta`<br>`test_metricas.test_E4_bordas_do_intervalo`<br>`test_metricas.test_E4_classe_usa_o_esperado_do_inicio_do_intervalo`<br>`test_metricas.test_E4_alem_do_esperado` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E5 | `test_metricas.test_E5_toque_tres_paragrafos_a_frente_nao_conta_salto`<br>`test_metricas.test_E5_short_na_janela_conta_na_primeira_amostra_depois` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E6 | `test_metricas.test_E6_recupera_em_2900_passa`<br>`test_metricas.test_E6_recupera_em_3000_passa`<br>`test_metricas.test_E6_recupera_em_3100_reprova`<br>`test_metricas.test_E6_nunca_recupera_reprova_mesmo_com_atraso_baixo_no_pulo_fim`<br>`test_metricas.test_E6_sem_pulo_nao_se_aplica` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E7 | `test_metricas.test_E7_recuo_para_36_depois_de_40_viola`<br>`test_metricas.test_E7_hw_e_o_maior_nao_o_ultimo`<br>`test_metricas.test_E7_recuo_para_37_nao_viola`<br>`test_metricas.test_E7_reposition_redefine_hw`<br>`test_metricas.test_E7_longjump_e_recovery_tambem_contam` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E8 | `test_metricas.test_E8_deslocamento_leva_ao_relogio_do_esperado` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E9 | `test_metricas.test_E9_varios_arquivos_de_eventos`<br>`test_metricas.test_E9_eventos_fora_de_ordem_sao_ordenados` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E10 | `test_metricas.test_E10_t_ms_igual_e_aceito`<br>`test_metricas.test_E10_entrada_invalida_sai_com_2`<br>`test_metricas.test_E10_hipoteses_com_cabecalho_errado` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E11 | `test_metricas.test_E11_fala_cursor_e_latencias_1_e_2`<br>`test_metricas.test_E11_tokens_com_fim_em_intervalo_ficam_fora`<br>`test_metricas.test_E11_latencia_acima_da_meta_nao_muda_o_codigo` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E12 | `test_metricas.test_E12_stdout_traz_criterios_latencias_fim_e_proveniencia` | `python3 etapa0/ferramentas/testar.py 'test_metricas*.py'` |
| E13 | `test_cli.test_E13_transcricao_alinhar_metricas` | `python3 etapa0/ferramentas/testar.py 'test_cli*.py'` |
| Decisões do plano (A3, A5–A10) e guardas | `test_alinhar.test_plano_A3_improviso_que_termina_com_palavra_do_roteiro_nao_rouba_o_tempo`<br>`test_alinhar.test_plano_A3_palavra_comum_antes_do_improviso_fica_antes`<br>`test_alinhar.test_plano_A3_substituicao_nao_da_tempo`<br>`test_alinhar.test_plano_A3_uma_edicao_vale_mais_que_os_trechos`<br>`test_alinhar.test_plano_A3_insercao_no_comeco_tambem_e_trecho`<br>`test_alinhar.test_plano_A3_empate_de_formas_fica_com_a_primeira`<br>`test_alinhar.test_transcricao_invalida_sai_com_2`<br>`test_alinhar.test_palavra_de_duracao_zero_e_aceita`<br>`test_alinhar.test_saida_em_arquivo_com_o`<br>`test_cli.test_codigos_de_saida_pelo_processo`<br>`test_metricas.test_atraso_sem_amostras_de_leitura_normal_reprova`<br>`test_metricas.test_plano_A7_reposition_um_ms_antes_do_evento_fica_na_janela`<br>`test_metricas.test_plano_A7_so_a_reposition_para_i_antecipa_a_janela`<br>`test_metricas.test_plano_A5_alvo_e_o_primeiro_token_com_tempo_a_partir_do_pulo_inicio`<br>`test_metricas.test_plano_A6_deslocamento_positivo_comeca_na_trajetoria`<br>`test_metricas.test_esperado_inclui_o_fim_exato_e_usa_o_maior_indice`<br>`test_metricas.test_cursor_e_a_ultima_linha_ate_t_e_stay_e_ignorada`<br>`test_metricas.test_amostras_vao_ate_o_maior_fim_do_esperado`<br>`test_metricas.test_percentil95_posto_mais_proximo`<br>`test_roteiro.test_identifica_so_bytes_identicos`<br>`test_timestamps.test_saida_em_arquivo_com_o` | `python3 etapa0/ferramentas/testar.py` |

### Quebras propositais previstas para o QA (Q2)

Cada quebra troca um operando de uma comparação, um limiar, uma variável parecida ou remove uma guarda; o QA aplica uma por vez, roda `python3 etapa0/ferramentas/testar.py` (ou o comando JVM), confirma vermelho e restaura. Coluna "prova": o que aconteceu na cópia do arquiteto.

| ID | Arquivo | De | Para | Prova |
|---|---|---|---|---|
| M1 | `metricas.py` | `PASSO_MS = 100` | `PASSO_MS = 200` | vermelha |
| M2a | `metricas.py` | `ATRASO_MAX = 3` | `ATRASO_MAX = 4` | vermelha |
| M2b | `metricas.py` | `ATRASO_MAX = 3` | `ATRASO_MAX = 2` | vermelha |
| M3a | `metricas.py` | `FRACAO_MIN_PCT = 95` | `FRACAO_MIN_PCT = 94` | vermelha |
| M3b | `metricas.py` | `FRACAO_MIN_PCT = 95` | `FRACAO_MIN_PCT = 96` | vermelha |
| M4 | `metricas.py` | `ate3 * 100 >= FRACAO_MIN_PCT` | `ate3 * 100 > FRACAO_MIN_PCT` | vermelha |
| M5 | `metricas.py` | `if a <= ATRASO_MAX)` | `if a < ATRASO_MAX)` | vermelha |
| M6 | `metricas.py` | `passa = bool(atrasos) and` | `passa =` | vermelha |
| M7a | `metricas.py` | `AVANCO_MAX = 2` | `AVANCO_MAX = 3` | vermelha |
| M7b | `metricas.py` | `avanco > AVANCO_MAX` | `avanco >= AVANCO_MAX` | vermelha |
| M8 | `metricas.py` | `POS_PULO_MS = 3000` | `POS_PULO_MS = 2900` | vermelha |
| M9a | `metricas.py` | `RECUPERACAO_MAX_MS = 3000` | `RECUPERACAO_MAX_MS = 3100` | vermelha |
| M9b | `metricas.py` | `RECUPERACAO_MAX_MS = 3000` | `RECUPERACAO_MAX_MS = 2800` | vermelha |
| M10 | `metricas.py` | `p["recuperacao_ms"] <= RECUPERACAO_MAX_MS` | `p["recuperacao_ms"] < RECUPERACAO_MAX_MS` | vermelha |
| M11a | `metricas.py` | `RECUO_MAX = 3` | `RECUO_MAX = 2` | vermelha |
| M11b | `metricas.py` | `RECUO_MAX = 3` | `RECUO_MAX = 4` | vermelha |
| M12 | `metricas.py` | `linha.cursor < hw - RECUO_MAX` | `linha.cursor <= hw - RECUO_MAX` | vermelha |
| M13 | `metricas.py` | `hw = max(hw, linha.cursor)` | `hw = linha.cursor` | vermelha |
| M14 | `metricas.py` | `if linha.move == "Reposition": ⏎                 hw = linha.cursor ⏎             elif` | `if linha.move == "Reposition": ⏎                 hw = hw if hw is not None else linha.cursor ⏎             elif` | vermelha |
| M15a | `metricas.py` | `AUTOMATICOS = ("Short", "LongJump", "Recovery")` | `AUTOMATICOS = ("Short", "Recovery")` | vermelha |
| M15b | `metricas.py` | `AUTOMATICOS = ("Short", "LongJump", "Recovery")` | `AUTOMATICOS = ("Short", "LongJump")` | vermelha |
| M16a | `metricas.py` | `SEM_MOVIMENTO = ("improviso", "silencio", "pausa")` | `SEM_MOVIMENTO = ("silencio", "pausa")` | vermelha |
| M16b | `metricas.py` | `SEM_MOVIMENTO = ("improviso", "silencio", "pausa")` | `SEM_MOVIMENTO = ("improviso", "pausa")` | vermelha |
| M16c | `metricas.py` | `SEM_MOVIMENTO = ("improviso", "silencio", "pausa")` | `SEM_MOVIMENTO = ("improviso", "silencio")` | vermelha |
| M17a | `metricas.py` | `if a <= linha.t < b:` | `if a < linha.t < b:` | vermelha |
| M17b | `metricas.py` | `if a <= linha.t < b:` | `if a <= linha.t <= b:` | vermelha |
| M18a | `metricas.py` | `return any(a <= t < b for a, b in faixas)` | `return any(a < t < b for a, b in faixas)` | vermelha |
| M18b | `metricas.py` | `return any(a <= t < b for a, b in faixas)` | `return any(a <= t <= b for a, b in faixas)` | vermelha |
| M19 | `metricas.py` | `"alcance" if linha.cursor <= congelado` | `"alcance" if linha.cursor < congelado` | vermelha |
| M20 | `metricas.py` | `congelado = self.esperado_em(a)` | `congelado = self.esperado_em(linha.t)` | vermelha |
| M21 | `metricas.py` | `e.fim is not None and e.token >= i]` | `e.fim is not None and e.token > i]` | vermelha |
| M22a | `metricas.py` | `inicio = anteriores[-1].t ⏎` | `pass ⏎` | vermelha |
| M22b | `metricas.py` | `anteriores[-1].move == "Reposition" and anteriores[-1].cursor == i` | `anteriores[-1].move == "Reposition"` | vermelha |
| M22c | `metricas.py` | `anteriores[-1].move == "Reposition" and anteriores[-1].cursor == i` | `anteriores[-1].cursor == i` | vermelha |
| M23a | `metricas.py` | `if h.aceita and h.kind in` | `if h.kind in` | vermelha |
| M23b | `metricas.py` | `h.aceita and h.kind in ("partial", "final")` | `h.aceita` | vermelha |
| M24 | `metricas.py` | `aceitas[bisect.bisect_left(tempos_aceitas, e.inicio):]` | `aceitas` | vermelha |
| M25 | `metricas.py` | `elegivel = all(donos[p] == {e.token}` | `elegivel = all(e.token in donos[p]` | vermelha |
| M26a | `metricas.py` | `return linha.t if linha.move in AUTOMATICOS else None` | `return linha.t` | vermelha |
| M26b | `metricas.py` | `linha.move != "Stay" and linha.cursor >= i` | `linha.move != "Stay" and linha.cursor > i` | vermelha |
| M26c | `metricas.py` | `fala_cursor.append(alcance - e.fim)` | `fala_cursor.append(alcance - e.inicio)` | vermelha |
| M27a | `metricas.py` | `or _dentro(e.fim, excecoes):` | `:` | vermelha |
| M28 | `metricas.py` | `ordenados[math.ceil(0.95 * len(ordenados)) - 1]` | `ordenados[int(0.95 * len(ordenados))]` | vermelha |
| M29a | `metricas.py` | `Hipotese(_inteiro(t, "hipóteses", n) + deslocamento,` | `Hipotese(_inteiro(t, "hipóteses", n),` | vermelha |
| M29b | `metricas.py` | `eventos.append((_inteiro(t, "eventos", n) + deslocamento,` | `eventos.append((_inteiro(t, "eventos", n),` | vermelha |
| M29c | `metricas.py` | `Linha(_inteiro(t, "trajetória", n) + deslocamento,` | `Linha(_inteiro(t, "trajetória", n),` | vermelha |
| M30 | `metricas.py` | `eventos.sort(key=lambda e: (e[0], e[1], e[2]))` | `pass` | vermelha |
| M32 | `metricas.py` | `if saida and linha.t < saida[-1].t:` | `if saida and linha.t <= saida[-1].t:` | vermelha |
| M33 | `metricas.py` | `if not saida or saida[0].move != "Reposition":` | `if not saida:` | vermelha |
| M35 | `metricas.py` | `if any(linha.cursor >= len(esperado) for linha in trajetoria):` | `if False:` | vermelha |
| M36 | `metricas.py` | `if indice is None or not 0 <= indice < n_tokens:` | `if indice is None or not 0 <= indice <= n_tokens:` | vermelha |
| M37a | `metricas.py` | `if _inteiro(token, "esperado", n) != n - 1:` | `if False:` | vermelha |
| M37b | `metricas.py` | `if not any(e.fim is not None for e in saida): ⏎         raise` | `if False: ⏎         raise` | vermelha |
| M38a | `metricas.py` | `t0 = max(0, self.trajetoria[0].t)` | `t0 = 0` | vermelha |
| M38b | `metricas.py` | `primeira = -(-t0 // PASSO_MS) * PASSO_MS` | `primeira = t0` | vermelha |
| M39a | `metricas.py` | `ultima = max(max(self._fins), self.trajetoria[-1].t)` | `ultima = max(self._fins)` | vermelha |
| M39b | `metricas.py` | `ultima = max(max(self._fins), self.trajetoria[-1].t)` | `ultima = self.trajetoria[-1].t` | vermelha |
| M40 | `metricas.py` | `k = bisect.bisect_right(self._fins, t)` | `k = bisect.bisect_left(self._fins, t)` | vermelha |
| M41 | `metricas.py` | `maior = max(maior, token)` | `maior = token` | vermelha |
| M41b | `metricas.py` | `k = bisect.bisect_right(self._t_traj, t)` | `k = bisect.bisect_left(self._t_traj, t)` | vermelha |
| M41c | `metricas.py` | `self._t_traj = [linha.t for linha in trajetoria if linha.move != "Stay"] ⏎         self._c_traj = [linha.cursor for linha in trajetoria if linha.move != "Stay"]` | `self._t_traj = [linha.t for linha in trajetoria] ⏎         self._c_traj = [linha.cursor for linha in trajetoria]` | vermelha |
| M42 | `metricas.py` | `else: ⏎                 atual = None ⏎` | `else: ⏎                 pass ⏎` | vermelha |
| M43 | `metricas.py` | `atrasos = [self.esperado_em(t) - self.cursor_em(t) for t in normais]` | `atrasos = [self.cursor_em(t) - self.esperado_em(t) for t in normais]` | vermelha |
| M44a | `metricas.py` | `e.fim is not None and e.token >= int(detalhe)]` | `e.fim is not None and e.token > int(detalhe)]` | vermelha |
| M44b | `metricas.py` | `if t < fim: ⏎` | `if t <= fim: ⏎` | vermelha |
| M44c | `metricas.py` | `if esp >= alvo and esp - self.cursor_em(t) <= ATRASO_MAX:` | `if esp - self.cursor_em(t) <= ATRASO_MAX:` | vermelha |
| M44d | `metricas.py` | `recuperou = t - fim` | `recuperou = t - inicio` | vermelha |
| M45 | `metricas.py` | `if _dentro(t, faixas_toque): ⏎                 excl_toque += 1` | `if False: ⏎                 excl_toque += 1` | vermelha |
| M46 | `metricas.py` | `if not _dentro(t, faixas_toque): ⏎                 avanco` | `if True: ⏎                 avanco` | vermelha |
| M47 | `metricas.py` | `pos_pulo = [(b, b + POS_PULO_MS)` | `pos_pulo = [(a, a + POS_PULO_MS)` | vermelha |
| M48 | `metricas.py` | `fim = fins[0] if fins else math.inf` | `fim = fins[-1] if fins else math.inf` | vermelha |
| M49 | `metricas.py` | `return 0 if resultado["veredito"] == "PASSA" else 1` | `return 0` | vermelha |
| M50 | `metricas.py` | `reprova = any(c["veredito"] == "REPROVA" for c in criterios.values())` | `reprova = any(c["veredito"] != "PASSA" for c in criterios.values())` | vermelha |
| L1 | `alinhar.py` | `return peso + 1, peso ⏎` | `return peso, peso ⏎` | vermelha |
| L2 | `alinhar.py` | `_PRIORIDADE = (_I, _M, _D)` | `_PRIORIDADE = (_M, _I, _D)` | vermelha |
| L2b | `alinhar.py` | `_PRIORIDADE = (_I, _M, _D)` | `_PRIORIDADE = (_D, _M, _I)` | vermelha |
| L3 | `alinhar.py` | `linha = next(u for u in ultimas[b - 1] if` | `linha = next(u for u in reversed(ultimas[b - 1]) if` | vermelha |
| L4 | `alinhar.py` | `if any(exato for _, exato in p):` | `if p:` | vermelha |
| L5a | `alinhar.py` | `transcricao[min(js)].inicio` | `transcricao[max(js)].inicio` | vermelha |
| L5b | `alinhar.py` | `transcricao[max(js)].fim` | `transcricao[min(js)].fim` | vermelha |
| L6 | `alinhar.py` | `if anterior is not None and inicio < anterior:` | `if False:` | vermelha |
| L7 | `alinhar.py` | `if inicio < 0 or fim < inicio:` | `if inicio < 0 or fim <= inicio:` | vermelha |
| L8 | `alinhar.py` | `for p in roteiro.palavras(campos[0]):` | `for p in [campos[0]]:` | vermelha |
| L9 | `alinhar.py` | `peso = len(linhas) + m + 2` | `peso = 1` | vermelha |
| L10 | `alinhar.py` | `(0 if a == palavra else peso)` | `(0 if a == palavra else 2 * peso)` | vermelha |
| L11 | `alinhar.py` | `if len(campos) != 4:` | `if len(campos) < 3:` | vermelha |
| L12 | `alinhar.py` | `[_custos(peso)[0] + (j - 1) * _custos(peso)[1] for j in range(1, m + 1)]` | `[j * _custos(peso)[1] for j in range(1, m + 1)]` | vermelha |
| L13 | `alinhar.py` | `if roteiro.identificar_texto(conteudo_texto) is None:` | `if False:` | vermelha |
| R1 | `roteiro.py` | `return [forma, ["quatorze" if p == "catorze" else p for p in forma]]` | `return [forma]` | vermelha |
| R2 | `roteiro.py` | `saida.append("duas")` | `saida.append("dois")` | vermelha |
| R3 | `roteiro.py` | `sem_juncao = _JUNCAO.sub("", sem_acento)` | `sem_juncao = sem_acento` | vermelha |
| R4 | `roteiro.py` | `if unicodedata.category(c) != "Mn"` | `if True` | vermelha |
| R5 | `roteiro.py` | `1: ("primeira", "primeiro", "um"),` | `1: ("primeiro", "primeira", "um"),` | vermelha |
| R6 | `roteiro.py` | `if resto < 100 or resto % 100 == 0:` | `if resto < 100:` | vermelha |
| R7 | `roteiro.py` | `if (TEXTOS / f"{texto_id}.txt").read_bytes() == conteudo:` | `if (TEXTOS / f"{texto_id}.txt").read_bytes().startswith(conteudo):` | vermelha |
| T1 | `timestamps.py` | `TAXA = 16000` | `TAXA = 44100` | vermelha |
| T2a | `timestamps.py` | `w.getnchannels() != 1 or` | (remove) | vermelha |
| T2b | `timestamps.py` | `w.getsampwidth() != 2 or` | (remove) | vermelha |
| T3 | `timestamps.py` | `round(p["start"] * 1000)` | `int(p["start"] * 1000)` | vermelha |
| T4 | `timestamps.py` | `palavras += _palavras_do_json(reconhecedor.FinalResult()) ⏎` | (remove) | vermelha |
| T5 | `timestamps.py` | `return sorted(palavras, key=lambda p: (p["start"], p["end"]))` | `return palavras` | vermelha |
| T6 | `timestamps.py` | `or m.group("id") not in roteiro.IDS` | (remove) | vermelha |
| T7 | `timestamps.py` | `if not Path(args.modelo).is_dir(): ⏎                 raise` | `if False: ⏎                 raise` | vermelha |
| T8 | `timestamps.py` | `if not args.modelo: ⏎                 raise` | `if False: ⏎                 raise` | vermelha |
| T9 | `timestamps.py` | `except ImportError: ⏎         raise DependenciaAusente(` | `except ImportError: ⏎         raise ErroEntrada(` | vermelha |
| T10 | `timestamps.py` | `if reconhecedor.AcceptWaveform(dados):` | `if False and reconhecedor.AcceptWaveform(dados):` | vermelha |

Quebras de dados e do lado Kotlin (comando: `cd etapa0 && ./gradlew :core:test --tests '*ReferenciaFormasTest*'`, e o Python por `testar.py 'test_roteiro*.py'`):

| ID | Quebra | Prova |
|---|---|---|
| K1 | `NumberWords.kt`: `spellingVariants` troca "catorze" por "catorze" (some "quatorze") | JVM vermelho |
| K2 | `NumberWords.kt`: feminino de "dois" fica "dois" | JVM vermelho |
| K3 | `Tokenizer.kt`: ordinais de 1 na ordem "primeiro", "primeira", "um" | JVM vermelho |
| K4 | `referencia/T02.tsv` linha 4: "muita" → "muito" | JVM vermelho e Python vermelho |
| K5 | `textos/T09.txt` com um "x" no fim (texto mudou, referência defasada) | JVM vermelho e Python vermelho |

Quebras não listadas, por serem equivalentes ou não testáveis: `w.getcomptype() != "NONE"` (o módulo `wave` só abre PCM; outro formato já lança `wave.Error`, que sai com 2); a guarda "início antes de fechar o anterior" em `intervalos()` (sem ela, o segundo fim fica sem início e a entrada continua saindo com 2).


#### Quebras da rodada 2 (achadas pelo revisor; todas vermelhas, exceto 2 equivalentes)

Detalhe e primeiro teste vermelho de cada uma em `evidencias/quebras-q2-rodada2.txt` (refazer: `python3 work/0004-etapa0-ferramentas/evidencias/quebras_rodada2.py`).

| ID | Arquivo:linha | De | Para | Prova |
|---|---|---|---|---|
| X01 | `metricas.py:368` | `<= ATRASO_MAX` | `< ATRASO_MAX` | vermelha (2) |
| X02 | `metricas.py:368` | `<= ATRASO_MAX` | `<= ATRASO_MAX - 1` | vermelha (2) |
| X03 | `metricas.py:368` | `<= ATRASO_MAX` | `<= ATRASO_MAX + 1` | vermelha (2) |
| X10 | `metricas.py:281` | `_salto(amostras` | `_salto(normais` | vermelha (4) |
| X06 | `metricas.py:375` | `all(` | `any(` | vermelha (3) |
| X13 | `metricas.py:338` | `not in AUTOMATICOS` | `== "Stay"` | vermelha (6) |
| X51 | `metricas.py:254` | `e.fim is not None and ` | (removido) | vermelha (1) |
| Z10, Z11 | `timestamps.py:63,71` | `.get("result", [])`, `+=` | `[:2]`, `=` | vermelhas (1 e 1) |
| X12, X80, X70, X84, X60 a X65, X69, X71, Y07 a Y10, Y12, Y15, Z01, Z05, X21, X30, X31 | vários | ver o arquivo de evidência | | vermelhas |
| X20, X08, X40, X50, Z09, W03 | vários | ver o arquivo de evidência | | vermelhas (eram "equivalentes" no julgamento do revisor; agora têm teste) |
| Y02, Y03 | `alinhar.py:91,95` | `y + abre` / `lm[j-1] if ... else ld[j-1]` | `y + estende` / `lm[j-1]` | **verdes, equivalentes** (diferencial de 5000 entradas, 0 diferenças) |
| KK5 | `Normalizer.kt:13` | `[-'’]` | `[-']` | vermelha (JVM: 2 falhas) |

Mais a varredura anti-recorrência (`evidencias/varredura-anti-recorrencia.tsv`): 644 trocas de constante, comparação e conjunto de amostras em `metricas.py`, `alinhar.py` e `roteiro.py`; 589 vermelhas, 6 em laço infinito (detectadas por tempo-limite), 48 verdes, todas equivalentes (`evidencias/equivalencias-rodada2.txt`).
## Privacidade (primeira camada)

- **Caminho do texto:** só textos de `etapa0/textos/`. O `alinhar.py` recusa (código 2, sem stdout e sem criar o `-o`) qualquer texto cujos bytes não sejam idênticos a um deles (D8; `CatalogoTest` e `test_D8_...`, inclusive com `\r\n`). O `metricas.py` não lê texto; acha o roteiro pela coluna `palavra` só entre os textos de teste (A9). O stderr do `alinhar.py` lista palavras inseridas, que são de gravação de texto de teste. Os testes montam tudo a partir de `etapa0/textos/` (D20): as transcrições sintéticas usam formas faladas dos tokens, palavras de outro texto de teste (improviso do T03 e do T09) ou palavras que existem nos textos ("mal", no lugar de "bem", está no L30; "livro", "comecar" e "fala" estão no T02). Os nomes de WAV dos testes seguem o exemplo do protocolo (`T02_giorgio_salao_t2.wav`) ou usam `voz2`; não há discurso pessoal nem palavra inventada.
- **Caminho do áudio:** só o `timestamps.py` toca áudio, e só lê o WAV (`wave.open(..., "rb")`); ele exige o nome `<ID>_<voz>_<ambiente>_t<n>.wav` com ID de `etapa0/textos/`, de modo que só gravação de texto de teste é transcrita. Nenhuma ferramenta escreve áudio (A4 do spec); o WAV dos testes é silêncio gerado em diretório temporário e apagado no fim. Nenhuma gravação real entra nesta tarefa.
- **Rede:** nenhuma. O modelo abre só por `vosk.Model(model_path=...)`, que nunca baixa (a fonte do pacote `vosk` mostra que só `model_name`/`lang` disparam `download_model`: https://github.com/alphacep/vosk-api/blob/master/python/vosk/__init__.py); o grep A3 garante que nem `model_name` nem `lang=` aparecem. Sem `--modelo`, código 2.
- **Dependências novas e permissões:** nenhuma dependência Gradle, nenhuma permissão, manifesto intocado. O pacote `vosk` não é instalado nesta tarefa (portão humano 6 na 0005).
- **Persistência:** as ferramentas escrevem só no stdout, no stderr e nos arquivos de `-o`/`--json`. O teste JVM escreve só em `etapa0/core/build/referencia-atual/`.
- **Q6 aplicada:** nenhum dado de discurso entra ou sai; o único conteúdo textual versionado novo são as referências, derivadas dos textos de teste e de números.

## Regenerar as referências

Quando um texto de `etapa0/textos/` ou o `Tokenizer`/`NumberWords` mudar de propósito (nunca para fazer um teste passar sem entender por quê):

```bash
cd etapa0
./gradlew :core:test --tests '*ReferenciaFormasTest*' --rerun   # pode falhar: grava o atual em core/build/referencia-atual/
mkdir -p core/src/test/resources/referencia
cp core/build/referencia-atual/*.tsv core/src/test/resources/referencia/
./gradlew :core:test --tests '*ReferenciaFormasTest*'           # verde
cd .. && git diff --stat -- etapa0/core/src/test/resources/referencia/
python3 etapa0/ferramentas/testar.py 'test_roteiro*.py'         # vermelho até o roteiro.py acompanhar, se o Kotlin mudou
```

O diff das referências vai no mesmo commit da mudança que o causou, com o motivo.

## Tarefas

Antes de começar: confira no cabeçalho que o plano passou no portão do orquestrador, que o branch é `work/0004-etapa0-ferramentas` e que `git status --short -- etapa0` não mostra nada. Todos os comandos são da raiz do repositório, salvo quando começam com `cd etapa0`.

### Tarefa 1 (motor-voz): referências das formas faladas geradas do Kotlin (spec B1, B2; A2)

**Arquivos:**
- Criar: `etapa0/core/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/align/ReferenciaFormasTest.kt`
- Gerar (por comando): `etapa0/core/src/test/resources/referencia/{T01..T10,L30,numeros}.tsv`

- [ ] **Passo 1: escrever o teste JVM**

`etapa0/core/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/align/ReferenciaFormasTest.kt`:

````kotlin
package io.github.gigioderico.tribuna.etapa0.align

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Paridade das formas faladas com as ferramentas Python da Etapa 0 (spec 0004, B1 e B2; brainstorm D3).
 *
 * As referências versionadas ficam em src/test/resources/referencia/ e são lidas também pelo
 * etapa0/ferramentas/roteiro.py. Este teste só compara: escreve o que o Kotlin gera hoje em
 * build/referencia-atual/ (fora do git) e falha se a versão versionada divergir. Regenerar é
 * ação explícita (impl_plan 0004, "Regenerar as referências"), nunca automática.
 */
class ReferenciaFormasTest {
    private val atual = File("build/referencia-atual")

    @Test
    fun formasDeCadaTexto_batemComAReferencia() {
        val gerados =
            TestTexts.names.associate { name ->
                val tokens = TestTexts.tokens(name)
                assertTrue("$name sem tokens (Q3)", tokens.size > 50)
                "$name.tsv" to renderTexto(name, tokens)
            }
        gerados.forEach { (arquivo, gerado) -> escreve(arquivo, gerado) } // todos antes de comparar
        gerados.forEach { (arquivo, gerado) -> confere(arquivo, gerado) }
    }

    @Test
    fun formasDosNumeros_batemComAReferencia() {
        val numeros = numerosDaReferencia()
        assertEquals("quantidade de números (Q3)", 2101 + MILHARES.size * RESTOS.size, numeros.size)
        val gerado = renderNumeros(numeros)
        escreve("numeros.tsv", gerado)
        confere("numeros.tsv", gerado)
    }

    private fun escreve(
        arquivo: String,
        gerado: String,
    ) {
        atual.mkdirs()
        File(atual, arquivo).writeText(gerado, Charsets.UTF_8)
    }

    private fun confere(
        arquivo: String,
        gerado: String,
    ) {
        val stream = javaClass.getResourceAsStream("/referencia/$arquivo")
        assertNotNull("referência $arquivo ausente: regenere (impl_plan 0004)", stream)
        val versionado = stream!!.use { it.readBytes().toString(Charsets.UTF_8) }
        val esperado = versionado.split('\n')
        val obtido = gerado.split('\n')
        for (i in 0 until minOf(esperado.size, obtido.size)) {
            assertEquals("$arquivo linha ${i + 1} (referência × Kotlin)", esperado[i], obtido[i])
        }
        assertEquals("$arquivo número de linhas (referência × Kotlin)", esperado.size, obtido.size)
    }

    private fun renderTexto(
        name: String,
        tokens: List<Token>,
    ): String {
        val bytes = File(System.getProperty("tribuna.textos"), "$name.txt").readBytes()
        val sb = StringBuilder("#texto=$name sha256=${sha256(bytes)} tokens=${tokens.size}\n")
        for (t in tokens) {
            sb.append("${t.index}\t${t.display.first}\t${t.display.last}\t${formas(t.spoken)}\n")
        }
        return sb.toString()
    }

    private fun renderNumeros(numeros: List<Int>): String {
        val sb = StringBuilder("#numeros=${numeros.size}\n")
        for (n in numeros) {
            sb.append("$n\t${formas(NumberWords.cardinalForms(n))}\t${formas(NumberWords.masculineForms(n))}\n")
        }
        return sb.toString()
    }

    private fun formas(spoken: List<List<String>>): String = spoken.joinToString("|") { it.joinToString(" ") }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        /** Lista fixa do plano 0004 (B1): milhares × restos; 999 × 1000 + 999 é o máximo de D14. */
        val MILHARES = listOf(2, 3, 10, 11, 14, 16, 20, 21, 100, 101, 114, 200, 201, 999)
        val RESTOS = listOf(0, 1, 14, 99, 100, 101, 114, 200, 999)

        fun numerosDaReferencia(): List<Int> = (0..2100).toList() + MILHARES.flatMap { m -> RESTOS.map { r -> m * 1000 + r } }
    }
}
````

- [ ] **Passo 2: ver falhar**

Rodar: `cd etapa0 && ./gradlew :core:test --tests '*ReferenciaFormasTest*' --console=plain`
Esperado: `BUILD FAILED`, `2 tests completed, 2 failed`; no relatório (`etapa0/core/build/test-results/test/TEST-io.github.gigioderico.tribuna.etapa0.align.ReferenciaFormasTest.xml`) aparece `referência T01.tsv ausente` e `referência numeros.tsv ausente`. Os 12 arquivos atuais foram gravados em `etapa0/core/build/referencia-atual/`.

- [ ] **Passo 3: gerar as referências (a "implementação" desta tarefa)**

```bash
cd etapa0
mkdir -p core/src/test/resources/referencia
cp core/build/referencia-atual/*.tsv core/src/test/resources/referencia/
ls core/src/test/resources/referencia | wc -l          # 12
head -3 core/src/test/resources/referencia/T01.tsv
```

Esperado: 12 arquivos; `T01.tsv` começa por `#texto=T01 sha256=` e a linha seguinte é `0	0	4	ouvir` (separada por tabulação). Conferir à mão, uma vez, duas linhas do `T02.tsv` contra o texto: o token `1` é `1914` com as formas `mil novecentos e catorze|mil novecentos e quatorze|mil novecentas e catorze|mil novecentas e quatorze`, e o `3:16` tem `tres dezesseis|tres e dezesseis|capitulo tres versiculo dezesseis` (Q4: o arquivo, não a lembrança).

- [ ] **Passo 4: ver passar, com estilo e lint**

Rodar: `cd etapa0 && ./gradlew :core:ktlintCheck :core:lint :core:test --console=plain`
Esperado: `BUILD SUCCESSFUL`. Contagem: `grep -ho 'tests="[0-9]*"' etapa0/core/build/test-results/test/*.xml | awk -F'"' '{s+=$2} END {print s}'` dá 120.

- [ ] **Passo 5: commit**

```bash
git add etapa0/core/src/test/kotlin/io/github/gigioderico/tribuna/etapa0/align/ReferenciaFormasTest.kt etapa0/core/src/test/resources/referencia/
git commit -m "test(0004): referências das formas faladas geradas do Tokenizer (spec 0004 B1, B2)"
```

### Tarefa 2 (motor-voz): `roteiro.py`, executor e paridade Python (spec A1, B1, B3, B4; A2)

**Arquivos:**
- Criar: `etapa0/ferramentas/testar.py`, `etapa0/ferramentas/testes/__init__.py` (vazio), `etapa0/ferramentas/testes/apoio.py`, `etapa0/ferramentas/testes/test_roteiro.py`
- Criar depois do vermelho: `etapa0/ferramentas/roteiro.py`

- [ ] **Passo 1: executor dos testes**

`etapa0/ferramentas/testar.py`:

````python
"""python3 etapa0/ferramentas/testar.py: roda todos os testes das ferramentas (spec 0004, D2 e A1).

Só unittest da biblioteca padrão, sem o pacote vosk e sem rede. Sai com 0 só se todos passarem e nenhum
for pulado (A1: "sem nenhum pulado nem falho"). Argumento opcional: padrão de nome (ex.: test_metricas*.py).
"""

import sys
import unittest
from pathlib import Path

AQUI = Path(__file__).resolve().parent


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    padrao = argv[0] if argv else "test_*.py"
    sys.path.insert(0, str(AQUI))
    suite = unittest.defaultTestLoader.discover(start_dir=str(AQUI / "testes"), pattern=padrao, top_level_dir=str(AQUI))
    resultado = unittest.TextTestRunner(verbosity=2).run(suite)
    print(
        f"testar.py: {resultado.testsRun} testes, {len(resultado.failures)} falhas, "
        f"{len(resultado.errors)} erros, {len(resultado.skipped)} pulados"
    )
    ok = resultado.wasSuccessful() and not resultado.skipped and resultado.testsRun > 0
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
````

`etapa0/ferramentas/testes/__init__.py`: arquivo vazio (`touch etapa0/ferramentas/testes/__init__.py`).

`etapa0/ferramentas/testes/apoio.py`:

````python
"""Apoio dos testes sem dependência das ferramentas: rodar main() no processo e pasta temporária."""

import contextlib
import io
import tempfile
from pathlib import Path


def rodar(main, argv):
    """Roda main(argv) no processo; devolve (código, stdout, stderr)."""
    out, err = io.StringIO(), io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
        try:
            codigo = main(argv)
        except SystemExit as e:  # argparse
            codigo = e.code
    return codigo, out.getvalue(), err.getvalue()


class Pasta:
    """Diretório temporário com escrita de arquivos de texto."""

    def __enter__(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.dir = Path(self._tmp.name)
        return self

    def __exit__(self, *exc):
        self._tmp.cleanup()

    def arquivo(self, nome, conteudo):
        caminho = self.dir / nome
        if isinstance(conteudo, bytes):
            caminho.write_bytes(conteudo)
        else:
            caminho.write_text(conteudo, encoding="utf-8")
        return str(caminho)
````

- [ ] **Passo 2: escrever os testes de paridade**

`etapa0/ferramentas/testes/test_roteiro.py`:

````python
"""Paridade com o Kotlin (spec 0004, B1, B3, B4; brainstorm D3 e D4)."""

import hashlib
import unittest

import roteiro


def _linhas_referencia(nome):
    return (roteiro.REFERENCIA / nome).read_text(encoding="utf-8").split("\n")


class ReferenciaTest(unittest.TestCase):
    def test_B1_ha_uma_referencia_por_texto_e_uma_de_numeros(self):
        textos = sorted(p.stem for p in roteiro.TEXTOS.glob("*.txt"))
        self.assertEqual(sorted(roteiro.IDS), textos, "IDS do roteiro.py x etapa0/textos/")
        referencias = sorted(p.name for p in roteiro.REFERENCIA.glob("*.tsv"))
        self.assertEqual(sorted([f"{i}.tsv" for i in roteiro.IDS] + ["numeros.tsv"]), referencias)

    def test_B3_tokens_de_cada_texto_batem_com_a_referencia(self):
        for texto_id in roteiro.IDS:
            with self.subTest(texto=texto_id):
                bruto = (roteiro.TEXTOS / f"{texto_id}.txt").read_bytes()
                texto = bruto.decode("utf-8")
                self.assertTrue(all(ord(c) <= 0xFFFF for c in texto), "fora do BMP: UTF-16 != índice Python")
                ref = _linhas_referencia(f"{texto_id}.tsv")
                cabecalho = dict(campo.split("=", 1) for campo in ref[0].lstrip("#").split(" "))
                self.assertEqual(
                    hashlib.sha256(bruto).hexdigest(),
                    cabecalho["sha256"],
                    f"referência defasada: {texto_id}.txt mudou; regenere (impl_plan 0004, Regenerar as referências)",
                )
                tokens = roteiro.tokenizar(texto)
                self.assertGreater(len(tokens), 50, "Q3")
                self.assertEqual(int(cabecalho["tokens"]), len(tokens))
                obtido = [f"{t.indice}\t{t.inicio}\t{t.fim}\t{roteiro.formatar_formas(t.formas)}" for t in tokens]
                self.assertEqual(ref[1:-1], obtido)
                self.assertEqual("", ref[-1], "a referência termina em \\n")

    def test_B3_formas_dos_numeros_batem_com_a_referencia(self):
        ref = _linhas_referencia("numeros.tsv")
        self.assertEqual(f"#numeros={len(ref) - 2}", ref[0])
        self.assertGreaterEqual(len(ref) - 2, 2101, "Q3: 0 a 2.100 e a lista de milhares")
        for linha in ref[1:-1]:
            n, cardinal, masculina = linha.split("\t")
            with self.subTest(n=n):
                self.assertEqual(cardinal, roteiro.formatar_formas(roteiro.formas_cardinais(int(n))))
                self.assertEqual(masculina, roteiro.formatar_formas(roteiro.formas_masculinas(int(n))))

    def test_B3_numero_fora_da_faixa_lanca_erro(self):
        with self.assertRaises(ValueError):
            roteiro.masculina(roteiro.MAX + 1)
        with self.assertRaises(ValueError):
            roteiro.masculina(-1)


class NormalizadorTest(unittest.TestCase):
    def test_B4_idempotente_em_todos_os_textos(self):
        for texto_id in roteiro.IDS:
            with self.subTest(texto=texto_id):
                uma = roteiro.normalizar(roteiro.ler_texto(texto_id))
                self.assertGreater(len(uma), 100, "Q3")
                self.assertEqual(uma, roteiro.normalizar(uma))

    def test_B4_exemplos_iguais_ao_Normalizer_kt(self):
        self.assertEqual("tres versiculo", roteiro.normalizar("Três, Versículo!"))
        self.assertEqual("bomdia", roteiro.normalizar("bom-dia"))
        self.assertEqual("escrevelos", roteiro.normalizar("escrevê-los"))


class CatalogoTest(unittest.TestCase):
    def test_identifica_so_bytes_identicos(self):
        for texto_id in roteiro.IDS:
            bruto = (roteiro.TEXTOS / f"{texto_id}.txt").read_bytes()
            with self.subTest(texto=texto_id):
                self.assertEqual(texto_id, roteiro.identificar_texto(bruto))
                self.assertIsNone(roteiro.identificar_texto(bruto + b"x"))
                self.assertIsNone(roteiro.identificar_texto(bruto[:-1]))
                self.assertIn(b"\n", bruto, "Q3: o caso CRLF abaixo precisa de fim de linha")
                self.assertIsNone(roteiro.identificar_texto(bruto.replace(b"\n", b"\r\n")))


if __name__ == "__main__":
    unittest.main()
````

- [ ] **Passo 3: ver falhar**

Rodar: `python3 etapa0/ferramentas/testar.py`
Esperado: código 1; `ImportError: Failed to import test module: testes.test_roteiro` (`No module named 'roteiro'`); última linha `testar.py: 1 testes, 0 falhas, 1 erros, 0 pulados`.

- [ ] **Passo 4: escrever o `roteiro.py`**

`etapa0/ferramentas/roteiro.py` (repete `Normalizer.kt`, `NumberWords.kt` e `Tokenizer.kt` regra por regra; as regex usam `[0-9]` e os espaços ASCII porque no `java.util.regex` `\d` e `\s` são só ASCII):

````python
"""Roteiro -> tokens, em Python, repetindo o núcleo Kotlin da etapa0 (spec 0004, D3 e D4).

Espelha, regra por regra:
- Normalizer.kt (normalizar, palavras);
- NumberWords.kt (masculina, formas_cardinais, formas_masculinas);
- Tokenizer.kt (tokenizar), só índice, intervalo exibido e formas faladas (peso e início de frase
  ficam de fora: as ferramentas não usam).

A paridade é provada pelas referências geradas do Kotlin em
etapa0/core/src/test/resources/referencia/ (teste JVM ReferenciaFormasTest e testes/test_roteiro.py).
As regex usam [0-9] e o espaço ASCII de propósito: no Java, \\d e \\s são só ASCII.

Também guarda o catálogo dos textos de teste (CLAUDE.md "Textos de teste"; spec 0004, D8).
"""

import itertools
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path

FERRAMENTAS = Path(__file__).resolve().parent
ETAPA0 = FERRAMENTAS.parent
TEXTOS = ETAPA0 / "textos"
REFERENCIA = ETAPA0 / "core" / "src" / "test" / "resources" / "referencia"
IDS = ("T01", "T02", "T03", "T04", "T05", "T06", "T07", "T08", "T09", "T10", "L30")

# ---------------------------------------------------------------- Normalizer.kt

_ESPACO_JAVA = " \t\n\x0b\f\r"  # \s do java.util.regex sem UNICODE_CHARACTER_CLASS
_JUNCAO = re.compile(r"(?<=[^\W_])[-'’](?=[^\W_])")
_NAO_PALAVRA = re.compile(r"(?:[^\w" + _ESPACO_JAVA + r"]|_)+")
_ESPACOS = re.compile("[" + _ESPACO_JAVA + "]+")


def normalizar(texto):
    """Minúsculas, sem acento, sem pontuação; hífen e apóstrofo entre letras unem as partes."""
    minusculo = texto.lower()
    nfd = unicodedata.normalize("NFD", minusculo)
    sem_acento = "".join(c for c in nfd if unicodedata.category(c) != "Mn")
    sem_juncao = _JUNCAO.sub("", sem_acento)
    return _ESPACOS.sub(" ", _NAO_PALAVRA.sub(" ", sem_juncao)).strip()


def palavras(texto):
    return [p for p in normalizar(texto).split(" ") if p]


# ---------------------------------------------------------------- NumberWords.kt

MAX = 999_999
_INT_MAX = 2_147_483_647  # Int.MAX_VALUE: o toIntOrNull do Kotlin devolve null acima disso

_UNIDADES = (
    "zero", "um", "dois", "tres", "quatro", "cinco", "seis", "sete", "oito", "nove",
    "dez", "onze", "doze", "treze", "catorze", "quinze", "dezesseis", "dezessete", "dezoito", "dezenove",
)
_DEZENAS = ("", "", "vinte", "trinta", "quarenta", "cinquenta", "sessenta", "setenta", "oitenta", "noventa")
_CENTENAS = (
    "", "cento", "duzentos", "trezentos", "quatrocentos",
    "quinhentos", "seiscentos", "setecentos", "oitocentos", "novecentos",
)


def _abaixo_de_mil(n):
    if n < 20:
        return [_UNIDADES[n]]
    if n < 100:
        return [_DEZENAS[n // 10]] + ([] if n % 10 == 0 else ["e", _UNIDADES[n % 10]])
    if n == 100:
        return ["cem"]
    return [_CENTENAS[n // 100]] + ([] if n % 100 == 0 else ["e"] + _abaixo_de_mil(n % 100))


def masculina(n):
    if not 0 <= n <= MAX:
        raise ValueError("número fora da faixa de D14")  # sem o valor (Q6), como no Kotlin
    if n < 1000:
        return _abaixo_de_mil(n)
    milhares, resto = divmod(n, 1000)
    cabeca = ["mil"] if milhares == 1 else _abaixo_de_mil(milhares) + ["mil"]
    if resto == 0:
        return cabeca
    if resto < 100 or resto % 100 == 0:
        return cabeca + ["e"] + _abaixo_de_mil(resto)
    return cabeca + _abaixo_de_mil(resto)


def _variantes(forma):
    if "catorze" in forma:
        return [forma, ["quatorze" if p == "catorze" else p for p in forma]]
    return [forma]


def _feminina(forma):
    saida = []
    for p in forma:
        if p == "um":
            saida.append("uma")
        elif p == "dois":
            saida.append("duas")
        elif p.endswith("entos"):
            saida.append(p[: -len("entos")] + "entas")
        else:
            saida.append(p)
    return saida


def _distintas(formas):
    vistas, saida = set(), []
    for f in formas:
        chave = tuple(f)
        if chave not in vistas:
            vistas.add(chave)
            saida.append(list(f))
    return saida


def formas_cardinais(n):
    m = masculina(n)
    f = _feminina(m)
    bases = [m] if f == m else [m, f]
    return _distintas([v for b in bases for v in _variantes(b)])


def formas_masculinas(n):
    return _variantes(masculina(n))


# ---------------------------------------------------------------- Tokenizer.kt


@dataclass(frozen=True)
class Token:
    indice: int
    inicio: int  # display.first: primeiro caractere exibido (UTF-16 = código Python no BMP)
    fim: int  # display.last: último caractere exibido, inclusivo
    formas: tuple  # tupla de tuplas de palavras normalizadas


_PALAVRA_CRUA = re.compile("[^" + _ESPACO_JAVA + "]+")
_REFERENCIA = re.compile(r"([0-9]+):([0-9]+)(?:[-–]([0-9]+))?")
_INICIO_REFERENCIA = re.compile(r"[0-9]+:[0-9]+")
_INTEIRO = re.compile(r"[0-9]+")
_NUMERO = re.compile(r"([0-9]{1,3}(?:\.[0-9]{3})+|[0-9]+)(?:,([0-9]+))?(%)?")
_NUMERO_LIVRO = ("1", "2", "3")
_PALAVRAS_INTERVALO = ("a", "-", "–")
_ORDINAIS = {
    1: ("primeira", "primeiro", "um"),
    2: ("segunda", "segundo", "dois"),
    3: ("terceira", "terceiro", "tres"),
}


def _letra_ou_digito(c):
    return c.isalpha() or c.isdecimal()  # Character.isLetterOrDigit: L* ou Nd


def _int_ou_none(s):
    v = int(s)
    return v if v <= _INT_MAX else None


class _Cru:
    def __init__(self, texto, inicio):
        self.texto = texto
        self.inicio = inicio
        idx = [i for i, c in enumerate(texto) if _letra_ou_digito(c)]
        fim = [i for i, c in enumerate(texto) if _letra_ou_digito(c) or c == "%"]
        self.tem_nucleo = bool(idx)
        self.ini_nucleo = idx[0] if idx else 0
        self.fim_nucleo = (fim[-1] + 1) if fim else 0
        self.nucleo = texto[self.ini_nucleo:self.fim_nucleo] if self.tem_nucleo else ""

    @property
    def abs_ini(self):
        return self.inicio + self.ini_nucleo

    @property
    def abs_ult(self):
        return self.inicio + self.fim_nucleo - 1


def tokenizar(texto):
    tokens = []
    inicio_linha = 0
    for linha in texto.split("\n"):
        aparada = linha.strip()
        if aparada and not aparada.startswith("//"):
            crus = [_Cru(m.group(0), inicio_linha + m.start()) for m in _PALAVRA_CRUA.finditer(linha)]
            i = 0
            while i < len(crus):
                feito = _montar(crus, i)
                if feito is None:
                    i += 1
                    continue
                ini, ult, formas, ultimo_cru = feito
                tokens.append(Token(len(tokens), ini, ult, tuple(tuple(f) for f in formas)))
                i = ultimo_cru + 1
        inicio_linha += len(linha) + 1
    return tokens


def _montar(crus, i):
    if not crus[i].tem_nucleo:
        return None  # pontuação solta não gera token (D19 da 0002)
    for regra in (_livro, _referencia, _numero, _palavra):
        feito = regra(crus, i)
        if feito is not None:
            return feito
    return None


def _livro(crus, i):
    cru = crus[i]
    if cru.texto not in _NUMERO_LIVRO or i + 2 >= len(crus):
        return None
    nome, ref = crus[i + 1], crus[i + 2]
    if not nome.tem_nucleo or not nome.nucleo[0].isupper() or not _INICIO_REFERENCIA.match(ref.nucleo):
        return None
    palavras_nome = palavras(nome.nucleo)
    formas = [[o] + palavras_nome for o in _ORDINAIS[int(cru.texto)]]
    return cru.abs_ini, nome.abs_ult, formas, i + 1


def _referencia(crus, i):
    cru = crus[i]
    m = _REFERENCIA.fullmatch(cru.nucleo)
    if m is None:
        return None
    capitulo = _int_ou_none(m.group(1))
    v0 = _int_ou_none(m.group(2))
    if capitulo is None or v0 is None:
        return None
    versiculos = [v0]
    ultimo = i
    intervalo = False
    prox = crus[i + 1] if i + 1 < len(crus) else None
    depois = crus[i + 2] if i + 2 < len(crus) else None
    if m.group(3):
        v = _int_ou_none(m.group(3))
        if v is None:
            return None
        versiculos.append(v)
        intervalo = True
    elif prox is not None and prox.texto in _PALAVRAS_INTERVALO and depois is not None and _INTEIRO.fullmatch(depois.nucleo):
        v = _int_ou_none(depois.nucleo)
        if v is None:
            return None
        versiculos.append(v)
        intervalo = True
        ultimo = i + 2
    else:
        while crus[ultimo].texto.endswith(","):
            if ultimo + 1 >= len(crus):
                break
            item = crus[ultimo + 1]
            if not _INTEIRO.fullmatch(item.nucleo):
                break
            v = _int_ou_none(item.nucleo)
            if v is None:
                return None
            versiculos.append(v)
            ultimo += 1
    if capitulo > MAX or any(v > MAX for v in versiculos):
        return None
    return cru.abs_ini, crus[ultimo].abs_ult, _formas_referencia(capitulo, versiculos, intervalo), ultimo


def _formas_referencia(capitulo, versiculos, intervalo):
    opcoes = [formas_masculinas(capitulo)] + [formas_masculinas(v) for v in versiculos]
    formas = []
    for comb in itertools.product(*opcoes):
        c, v = list(comb[0]), [list(x) for x in comb[1:]]
        if intervalo:
            formas += [
                c + v[0] + ["a"] + v[1],
                c + ["de"] + v[0] + ["a"] + v[1],
                ["capitulo"] + c + ["versiculos"] + v[0] + ["a"] + v[1],
            ]
        elif len(v) == 1:
            formas += [c + v[0], c + ["e"] + v[0], ["capitulo"] + c + ["versiculo"] + v[0]]
        else:
            lista = [p for x in v[:-1] for p in x] + ["e"] + v[-1]
            formas += [c + lista, c + ["versiculos"] + lista, ["capitulo"] + c + ["versiculos"] + lista]
    return _distintas(formas)


def _numero(crus, i):
    cru = crus[i]
    m = _NUMERO.fullmatch(cru.nucleo)
    if m is None:
        return None
    valor = _int_ou_none(m.group(1).replace(".", ""))
    if valor is None or valor > MAX:
        return None
    decimais = m.group(2) or ""
    porcento = bool(m.group(3))
    if not decimais and not porcento:
        formas = formas_cardinais(valor)
    else:
        inteiro = formas_masculinas(valor)
        if decimais:
            fracao = _formas_decimais(decimais)
            com_decimais = [w + ["virgula"] + f for w in inteiro for f in fracao]
        else:
            com_decimais = inteiro
        formas = [f + ["por", "cento"] for f in com_decimais] if porcento else com_decimais
    return cru.abs_ini, cru.abs_ult, formas, i


def _formas_decimais(digitos):
    zeros = ["zero"] * (len(digitos) - len(digitos.lstrip("0")))
    resto = digitos[len(zeros):]
    if not resto:
        return [zeros]
    valor = _int_ou_none(resto)
    if valor is None or valor > MAX:
        return [list(digitos)]
    return [zeros + f for f in formas_masculinas(valor)]


def _palavra(crus, i):
    cru = crus[i]
    nucleo = cru.nucleo.rstrip("%")
    unida = palavras(nucleo)
    if not unida:
        return None
    partes = [p for pedaco in nucleo.split("-") for p in palavras(pedaco)]
    if "-" in nucleo and len(partes) > 1 and partes != unida:
        formas = [partes, unida]
    else:
        formas = [unida]
    return cru.abs_ini, cru.abs_ini + len(nucleo) - 1, formas, i


# ---------------------------------------------------------------- catálogo dos textos de teste


def ler_texto(texto_id):
    """Sem tradução de fim de linha, como o readText do Kotlin."""
    return (TEXTOS / f"{texto_id}.txt").read_bytes().decode("utf-8")


def identificar_texto(conteudo):
    """ID do texto de teste com bytes idênticos a `conteudo` (bytes), ou None (spec 0004, D8)."""
    for texto_id in IDS:
        if (TEXTOS / f"{texto_id}.txt").read_bytes() == conteudo:
            return texto_id
    return None


def tokens_por_palavras(palavras_exibidas):
    """Tokens do primeiro texto de teste cujos tokens, normalizados, dão exatamente essa coluna palavra
    do expected.csv (impl_plan 0004, A9); None se nenhum. T01, T04 a T08 e T10 têm os mesmos tokens."""
    for texto_id in IDS:
        texto = ler_texto(texto_id)
        tokens = tokenizar(texto)
        if [normalizar(texto[t.inicio:t.fim + 1]) for t in tokens] == list(palavras_exibidas):
            return tokens
    return None


def formatar_formas(formas):
    """Formato das referências: formas separadas por |, palavras por espaço."""
    return "|".join(" ".join(f) for f in formas)
````

- [ ] **Passo 5: ver passar**

Rodar: `python3 etapa0/ferramentas/testar.py`
Esperado: código 0; `testar.py: 7 testes, 0 falhas, 0 erros, 0 pulados`.

- [ ] **Passo 6: commit**

```bash
git add etapa0/ferramentas/testar.py etapa0/ferramentas/roteiro.py etapa0/ferramentas/testes/__init__.py etapa0/ferramentas/testes/apoio.py etapa0/ferramentas/testes/test_roteiro.py
git commit -m "feat(0004): roteiro.py com normalizador e formas faladas iguais ao Kotlin (spec 0004 B3, B4; TRD Motor de voz §Normalização e tokens)"
```

### Tarefa 3 (motor-voz): `timestamps.py` (spec C1–C5; D5)

**Arquivos:**
- Criar: `etapa0/ferramentas/testes/test_timestamps.py`
- Criar depois do vermelho: `etapa0/ferramentas/timestamps.py`

- [ ] **Passo 1: escrever os testes (reconhecedor falso injetado; WAV de silêncio gerado em diretório temporário)**

`etapa0/ferramentas/testes/test_timestamps.py`:

````python
"""timestamps.py com reconhecedor falso injetado (spec 0004, C1 a C5; D5). Sem o pacote vosk."""

import json
import sys
import unittest
import wave
from unittest import mock

import timestamps
from testes.apoio import Pasta, rodar

RESULTADO = {
    "result": [
        {"word": "ouvir", "start": 1.21, "end": 1.53, "conf": 1.0},
        {"word": "bem", "start": 1.53, "end": 1.79, "conf": 0.98},
    ],
    "text": "ouvir bem",
}
FINAL = {"result": [{"word": "três", "start": 2.0, "end": 2.4, "conf": 0.9}], "text": "três"}


class ReconhecedorFalso:
    """Imita o KaldiRecognizer: o 1º bloco fecha um Result(); o resto só acumula até o FinalResult()."""

    def __init__(self):
        self.bytes_recebidos = 0
        self.blocos = 0

    def AcceptWaveform(self, dados):  # noqa: N802 (nome da API do Vosk)
        self.bytes_recebidos += len(dados)
        self.blocos += 1
        return self.blocos == 1

    def Result(self):  # noqa: N802
        return json.dumps(RESULTADO)

    def FinalResult(self):  # noqa: N802
        return json.dumps(FINAL)


class Fabrica:
    def __init__(self):
        self.taxas = []
        self.reconhecedor = ReconhecedorFalso()

    def __call__(self, taxa):
        self.taxas.append(taxa)
        return self.reconhecedor


def escrever_wav(caminho, taxa=16000, canais=1, largura=2, quadros=10000):
    """WAV sintético de silêncio, só em diretório temporário de teste."""
    with wave.open(caminho, "wb") as w:
        w.setnchannels(canais)
        w.setsampwidth(largura)
        w.setframerate(taxa)
        w.writeframes(b"\x00" * (quadros * canais * largura))
    return caminho


class TimestampsTest(unittest.TestCase):
    def _rodar(self, nome="T02_giorgio_salao_t2.wav", **wav):
        with Pasta() as p:
            caminho = escrever_wav(str(p.dir / nome), **wav)
            fabrica = Fabrica()
            codigo, out, err = rodar(lambda argv: timestamps.main(argv, fabrica=fabrica), [caminho])
        return codigo, out, err, fabrica

    def test_C1_csv_com_cabecalho_e_palavras_em_ordem(self):
        codigo, out, _err, fabrica = self._rodar()
        self.assertEqual(0, codigo)
        self.assertEqual([16000], fabrica.taxas)
        self.assertEqual(
            ["palavra,t_inicio_ms,t_fim_ms,conf", "ouvir,1210,1530,1.0", "bem,1530,1790,0.98", "três,2000,2400,0.9"],
            out.splitlines(),
        )

    def test_C1_ordena_por_tempo(self):
        invertido = {"result": list(reversed(RESULTADO["result"]))}
        with mock.patch.object(ReconhecedorFalso, "Result", lambda self: json.dumps(invertido)):
            codigo, out, _err, _f = self._rodar()
        self.assertEqual(0, codigo)
        self.assertEqual(["ouvir", "bem", "três"], [linha.split(",")[0] for linha in out.splitlines()[1:]])

    def test_C1_milissegundos_arredondados(self):
        """4.02 × 1000 = 4019.9999999999995 em ponto flutuante: round dá 4020 (D5), int daria 4019."""
        quebrado = {"result": [{"word": "bem", "start": 4.02, "end": 4.06, "conf": 1.0}]}
        with mock.patch.object(ReconhecedorFalso, "Result", lambda self: json.dumps(quebrado)):
            _codigo, out, _err, _f = self._rodar()
        self.assertEqual("bem,4020,4060,1.0", out.splitlines()[2])

    def test_C2_palavra_sai_como_o_reconhecedor_devolveu(self):
        _codigo, out, _err, _f = self._rodar()
        self.assertIn("três,2000,2400,0.9", out.splitlines())
        self.assertNotIn("tres", out)

    def test_C3_wav_valido_entrega_todos_os_quadros(self):
        codigo, _out, _err, fabrica = self._rodar(quadros=10001)
        self.assertEqual(0, codigo)
        self.assertEqual(10001 * 2, fabrica.reconhecedor.bytes_recebidos)
        self.assertGreater(fabrica.reconhecedor.blocos, 1, "Q3: mais de um bloco")

    def test_C3_formato_errado_sai_com_2_sem_stdout(self):
        for caso in ({"taxa": 44100}, {"canais": 2}, {"largura": 1}):
            with self.subTest(caso=caso):
                codigo, out, err, fabrica = self._rodar(**caso)
                self.assertEqual(2, codigo)
                self.assertEqual("", out)
                self.assertIn("16 kHz", err)
                self.assertEqual([], fabrica.taxas)

    def test_C4_nome_fora_do_padrao_sai_com_2(self):
        for nome in ("discurso.wav", "X99_giorgio_sala_t1.wav", "T02_giorgio_salao.wav", "T02_giorgio_salao_t2.WAV"):
            with self.subTest(nome=nome):
                codigo, out, _err, _f = self._rodar(nome=nome)
                self.assertEqual(2, codigo)
                self.assertEqual("", out)

    def test_C4_nome_do_protocolo_e_aceito(self):
        for nome in ("T02_giorgio_salao_t2.wav", "L30_voz2_sala_t1.wav"):
            with self.subTest(nome=nome):
                self.assertEqual(0, self._rodar(nome=nome)[0])

    def test_C5_sem_pacote_vosk_sai_com_3(self):
        with Pasta() as p, mock.patch.dict(sys.modules, {"vosk": None}):
            caminho = escrever_wav(str(p.dir / "T01_giorgio_sala_t1.wav"))
            codigo, out, err = rodar(timestamps.main, [caminho, "--modelo", str(p.dir)])
        self.assertEqual(3, codigo)
        self.assertEqual("", out)
        self.assertIn("vosk", err)

    def test_C5_sem_modelo_sai_com_2(self):
        with Pasta() as p:
            caminho = escrever_wav(str(p.dir / "T01_giorgio_sala_t1.wav"))
            codigo, out, err = rodar(timestamps.main, [caminho])
            self.assertEqual(2, codigo)
            self.assertIn("--modelo", err)
            codigo, out, err = rodar(timestamps.main, [caminho, "--modelo", str(p.dir / "nao_existe")])
            self.assertEqual(2, codigo)
        self.assertEqual("", out)

    def test_saida_em_arquivo_com_o(self):
        with Pasta() as p:
            caminho = escrever_wav(str(p.dir / "T01_giorgio_sala_t1.wav"))
            destino = p.dir / "T01_giorgio_sala_t1.words.csv"
            codigo, out, _err = rodar(lambda a: timestamps.main(a, fabrica=Fabrica()), [caminho, "-o", str(destino)])
            self.assertEqual(0, codigo)
            self.assertEqual("", out)
            self.assertTrue(destino.read_text(encoding="utf-8").startswith("palavra,t_inicio_ms,t_fim_ms,conf\n"))


if __name__ == "__main__":
    unittest.main()
````

- [ ] **Passo 2: ver falhar**

Rodar: `python3 etapa0/ferramentas/testar.py`
Esperado: código 1; `ImportError: Failed to import test module: testes.test_timestamps`; `testar.py: 8 testes, 0 falhas, 1 erros, 0 pulados`.

- [ ] **Passo 3: escrever o `timestamps.py`**

`etapa0/ferramentas/timestamps.py` (o `import vosk` fica só dentro de `_fabrica_vosk`; o modelo abre só por `model_path`):

````python
"""timestamps.py <wav> --modelo DIR [-o SAIDA]: tempo de cada palavra de uma gravação de teste.

TESTES-ETAPA0, "Trajetória esperada", passo 1; spec 0004, D5.

Saída: CSV palavra,t_inicio_ms,t_fim_ms,conf, na ordem do tempo, com a palavra exatamente como o Vosk
devolveu (golden bruto). Só lê o WAV: nenhum áudio é escrito (CLAUDE.md regra 3).

Guardas: WAV 16 kHz, mono, PCM 16 bits; nome <ID>_<voz>_<ambiente>_t<n>.wav com <ID> de etapa0/textos/;
modelo só por caminho (--modelo), nunca por nome ou idioma, que baixariam da internet (D5, D21).
O pacote vosk é importado só no modo real, dentro de _fabrica_vosk (A2 do spec).
Códigos de saída: 0 ok; 2 entrada inválida; 3 dependência ausente.
"""

import argparse
import csv
import io
import json
import re
import sys
import wave
from pathlib import Path

import roteiro

CABECALHO = ["palavra", "t_inicio_ms", "t_fim_ms", "conf"]
TAXA = 16000
QUADROS_POR_BLOCO = 4000
_NOME = re.compile(r"(?P<id>[A-Z0-9]+)_[A-Za-z0-9]+_[A-Za-z0-9]+_t[0-9]+\.wav")


class ErroEntrada(Exception):
    """Código de saída 2."""


class DependenciaAusente(Exception):
    """Código de saída 3."""


def validar_nome(caminho):
    m = _NOME.fullmatch(Path(caminho).name)
    if m is None or m.group("id") not in roteiro.IDS:
        raise ErroEntrada("nome fora do padrão <ID>_<voz>_<ambiente>_t<n>.wav com ID de etapa0/textos/")


def blocos_do_wav(caminho):
    """Valida o formato e devolve a lista de blocos de bytes PCM, na ordem."""
    try:
        with wave.open(str(caminho), "rb") as w:
            if w.getcomptype() != "NONE" or w.getnchannels() != 1 or w.getsampwidth() != 2 or w.getframerate() != TAXA:
                raise ErroEntrada("o WAV precisa ser 16 kHz, mono, PCM 16 bits")
            blocos = []
            while True:
                dados = w.readframes(QUADROS_POR_BLOCO)
                if not dados:
                    break
                blocos.append(dados)
            return blocos
    except (wave.Error, EOFError, OSError) as erro:
        raise ErroEntrada(f"WAV ilegível: {erro}") from None


def _palavras_do_json(texto_json):
    return json.loads(texto_json).get("result", [])


def transcrever(blocos, reconhecedor):
    """Laço WAV -> reconhecedor -> palavras (cada uma um dict word/start/end/conf), em ordem de tempo."""
    palavras = []
    for dados in blocos:
        if reconhecedor.AcceptWaveform(dados):
            palavras += _palavras_do_json(reconhecedor.Result())
    palavras += _palavras_do_json(reconhecedor.FinalResult())
    return sorted(palavras, key=lambda p: (p["start"], p["end"]))  # sorted é estável


def para_csv(palavras):
    saida = io.StringIO()
    w = csv.writer(saida, lineterminator="\n")
    w.writerow(CABECALHO)
    for p in palavras:
        conf = p.get("conf")
        w.writerow([p["word"], round(p["start"] * 1000), round(p["end"] * 1000), "" if conf is None else conf])
    return saida.getvalue()


def _fabrica_vosk(modelo):
    """Modo real: abre o modelo pelo caminho e devolve uma função taxa -> reconhecedor com SetWords(True)."""
    try:
        import vosk  # só aqui (spec 0004, A2): os testes não dependem do pacote
    except ImportError:
        raise DependenciaAusente(
            "o pacote vosk não está instalado; instalar é portão humano 6 (0005), fora desta ferramenta"
        ) from None
    vosk.SetLogLevel(-1)  # sem o log do Kaldi no stderr
    try:
        model = vosk.Model(model_path=str(modelo))  # só por caminho: sem download (D5, D21)
    except Exception as erro:  # o pacote lança Exception("Failed to create a model")
        raise ErroEntrada(f"o modelo não abriu: {erro}") from None

    def criar(taxa):
        reconhecedor = vosk.KaldiRecognizer(model, taxa)
        reconhecedor.SetWords(True)
        return reconhecedor

    return criar


def main(argv=None, fabrica=None):
    """`fabrica` (taxa -> reconhecedor) só é passada pelos testes; sem ela, modo real com o Vosk."""
    parser = argparse.ArgumentParser(
        prog="timestamps.py",
        description="Transcreve uma gravação de texto de teste com tempos por palavra (Vosk, SetWords).",
    )
    parser.add_argument("wav", help="WAV 16 kHz mono PCM 16 bits, nome <ID>_<voz>_<ambiente>_t<n>.wav")
    parser.add_argument("--modelo", help="diretório do modelo Vosk (obrigatório; nunca baixado)")
    parser.add_argument("-o", "--saida", help="arquivo do CSV (padrão: stdout)")
    args = parser.parse_args(argv)
    try:
        validar_nome(args.wav)
        blocos = blocos_do_wav(args.wav)
        if fabrica is None:
            if not args.modelo:
                raise ErroEntrada("faltou --modelo <diretório do modelo>")
            if not Path(args.modelo).is_dir():
                raise ErroEntrada("--modelo não é um diretório")
            fabrica = _fabrica_vosk(args.modelo)
        texto = para_csv(transcrever(blocos, fabrica(TAXA)))
    except ErroEntrada as erro:
        print(f"timestamps.py: {erro}", file=sys.stderr)
        return 2
    except DependenciaAusente as erro:
        print(f"timestamps.py: {erro}", file=sys.stderr)
        return 3
    if args.saida:
        with open(args.saida, "w", encoding="utf-8", newline="") as f:
            f.write(texto)
    else:
        sys.stdout.write(texto)
    return 0


if __name__ == "__main__":
    sys.exit(main())
````

- [ ] **Passo 4: ver passar**

Rodar: `python3 etapa0/ferramentas/testar.py`
Esperado: código 0; `testar.py: 18 testes, 0 falhas, 0 erros, 0 pulados`.

- [ ] **Passo 5: commit**

```bash
git add etapa0/ferramentas/timestamps.py etapa0/ferramentas/testes/test_timestamps.py
git commit -m "feat(0004): timestamps.py com reconhecedor injetado e guardas de WAV e nome (spec 0004 C1-C5)"
```

### Tarefa 4 (motor-voz): `alinhar.py` e dados sintéticos (spec D1–D9; A3, A4)

**Arquivos:**
- Criar: `etapa0/ferramentas/testes/sintetico.py`, `etapa0/ferramentas/testes/test_alinhar.py`
- Criar depois do vermelho: `etapa0/ferramentas/alinhar.py`

- [ ] **Passo 1: dados sintéticos (usados também pela Tarefa 5)**

`etapa0/ferramentas/testes/sintetico.py`:

````python
"""Dados sintéticos dos testes (spec 0004, D20): tudo sai de etapa0/textos/, nada de gravação real.

Convenções:
- palavra n da transcrição sintética fica em [300n, 300n + 250] ms (critério D1 do spec);
- no esperado sintético, o token i com tempo fica em [300i, 300i + 250] ms, salvo quando o teste diz outra coisa.
"""

import csv
import io
import json

import alinhar
import roteiro
from testes.apoio import Pasta, rodar

PASSO = 300
DURACAO = 250


def tokens(texto_id):
    return roteiro.tokenizar(roteiro.ler_texto(texto_id))


def exibido(texto_id, token):
    return roteiro.ler_texto(texto_id)[token.inicio:token.fim + 1]


def paragrafos(texto_id):
    """Índices de tokens por parágrafo, sem os blocos de nota (//)."""
    texto = roteiro.ler_texto(texto_id)
    blocos, atual, pos, nota = [], None, 0, False
    for linha in texto.split("\n"):
        vazia = not linha.strip()
        if vazia:
            atual = None
        elif atual is None:
            nota = linha.strip().startswith("//")
            atual = [pos, pos + len(linha)]
            if not nota:
                blocos.append(atual)
        else:
            atual[1] = pos + len(linha)
        pos += len(linha) + 1
    return [[t.indice for t in tokens(texto_id) if a <= t.inicio <= b] for a, b in blocos]


def palavras_dos_tokens(lista_tokens, formas=None):
    """Lista de (índice do token, palavra), com a primeira forma (ou formas[i], índice ou lista de palavras)."""
    saida = []
    for t in lista_tokens:
        escolha = (formas or {}).get(t.indice, 0)
        forma = t.formas[escolha] if isinstance(escolha, int) else escolha
        saida += [(t.indice, p) for p in forma]
    return saida


def transcricao_csv(palavras):
    """palavras: lista de palavras cruas; a n-ésima fica em [300n, 300n + 250]."""
    saida = io.StringIO()
    w = csv.writer(saida, lineterminator="\n")
    w.writerow(alinhar.CABECALHO_TRANSCRICAO)
    for n, p in enumerate(palavras):
        w.writerow([p, PASSO * n, PASSO * n + DURACAO, "1.0"])
    return saida.getvalue()


def tempos_esperados(pares_token_palavra):
    """Tempo que cada token deve receber: do início da 1ª palavra ao fim da última (posições na lista)."""
    tempos = {}
    for n, (i, _) in enumerate(pares_token_palavra):
        if i is None:
            continue
        ini, _fim = tempos.get(i, (PASSO * n, None))
        tempos[i] = (ini, PASSO * n + DURACAO)
    return tempos


def linhas_csv(texto):
    return list(csv.reader(io.StringIO(texto, newline="")))


def esperado_csv(texto_id, tempos):
    """tempos: lista por token de (ini, fim) ou None. Usa o próprio escritor do alinhar.py."""
    texto = roteiro.ler_texto(texto_id)
    return alinhar.esperado_csv(texto, roteiro.tokenizar(texto), tempos)


def tempos_regulares(n, sem_tempo=(), deslocamento=None):
    """Token i em [300i, 300i + 250]; `sem_tempo` fica None; `deslocamento(i)` soma ms ao token i."""
    saida = []
    for i in range(n):
        if i in sem_tempo:
            saida.append(None)
        else:
            extra = deslocamento(i) if deslocamento else 0
            saida.append((PASSO * i + extra, PASSO * i + DURACAO + extra))
    return saida


def fim_em(tempos):
    """Função esperado(t) do esperado sintético: maior índice com fim <= t (−1 antes)."""
    fins = sorted((f[1], i) for i, f in enumerate(tempos) if f is not None)

    def esperado(t):
        melhor = -1
        for fim, i in fins:
            if fim <= t:
                melhor = max(melhor, i)
        return melhor

    return esperado


def trajetoria_csv(linhas):
    """linhas: (t_ms, cursor, move). A primeira deve ser Reposition, como no app (0003)."""
    saida = ["t_ms,cursor,move,score"]
    saida += [f"{t},{c},{m},0.0" for t, c, m in linhas]
    return "\n".join(saida) + "\n"


def trajetoria_seguindo(tempos, cursor_de, extras=(), ate=None, move="Short"):
    """Linha Reposition 0 em t=0 e uma linha a cada mudança do cursor desejado, avaliado nos instantes em que
    o esperado muda e nos `extras`. `cursor_de(t, esperado)` dá o cursor desejado."""
    esperado = fim_em(tempos)
    instantes = sorted({f[1] for f in tempos if f is not None} | set(extras))
    if ate is not None:
        instantes = [t for t in instantes if t <= ate]
    linhas = [(0, 0, "Reposition")]
    atual = 0
    for t in instantes:
        if t <= 0:
            continue
        c = max(0, cursor_de(t, esperado(t)))
        if c != atual:
            linhas.append((t, c, move))
            atual = c
    return linhas


def eventos_csv(eventos):
    saida = ["t_ms,evento,detalhe"]
    saida += [f"{t},{e},{d}" for t, e, d in eventos]
    return "\n".join(saida) + "\n"


def rodar_metricas(main, trajetoria, esperado, eventos, hipoteses=None, deslocamento=None):
    """Escreve os arquivos, roda metricas.main com --json e devolve (código, json ou None, stdout, stderr).
    `eventos` é uma lista de conteúdos (um arquivo por item)."""
    with Pasta() as p:
        argv = [p.arquivo("x.trajectory.csv", trajetoria), p.arquivo("x.expected.csv", esperado)]
        argv += [p.arquivo(f"x{k}.events.csv", e) for k, e in enumerate(eventos)]
        if hipoteses is not None:
            argv += ["--hipoteses", p.arquivo("x.hypotheses.csv", hipoteses)]
        if deslocamento is not None:
            argv += ["--deslocamento-ms", str(deslocamento)]
        saida_json = p.dir / "r.json"
        argv += ["--json", str(saida_json)]
        codigo, out, err = rodar(main, argv)
        dados = json.loads(saida_json.read_text(encoding="utf-8")) if saida_json.exists() else None
    return codigo, dados, out, err
````

- [ ] **Passo 2: escrever os testes do alinhamento**

`etapa0/ferramentas/testes/test_alinhar.py`:

````python
"""alinhar.py com transcrições sintéticas de etapa0/textos/ (spec 0004, D1 a D9; impl_plan A3)."""

import sys
import time
import unittest
from pathlib import Path

import alinhar
import roteiro
from testes import sintetico as s


def _alinhar(texto_id, palavras, saida=None):
    """Roda alinhar.main com o texto de etapa0/textos/ e a transcrição sintética de `palavras` (cruas)."""
    with s.Pasta() as p:
        transcricao = p.arquivo("t.words.csv", s.transcricao_csv(palavras))
        argv = [str(roteiro.TEXTOS / f"{texto_id}.txt"), transcricao]
        if saida:
            argv += ["-o", saida]
        return s.rodar(alinhar.main, argv)


def _tempos(out):
    """expected.csv -> {token: (ini, fim) ou None}."""
    linhas = s.linhas_csv(out)
    return {int(t): (None if a == "" else (int(a), int(b))) for t, _p, a, b in linhas[1:]}


def _indice(texto_id, exibicao):
    return next(t.indice for t in s.tokens(texto_id) if s.exibido(texto_id, t) == exibicao)


class LeituraLimpaTest(unittest.TestCase):
    def test_D1_t01_todos_os_tokens_com_o_tempo_das_suas_palavras(self):
        tokens = s.tokens("T01")
        pares = s.palavras_dos_tokens(tokens)
        codigo, out, _err = _alinhar("T01", [p for _, p in pares])
        self.assertEqual(0, codigo)
        linhas = s.linhas_csv(out)
        self.assertEqual(["token", "palavra", "t_inicio_ms", "t_fim_ms"], linhas[0])
        self.assertEqual(["0", "ouvir", "0", "250"], linhas[1])
        self.assertEqual(len(tokens), len(linhas) - 1)
        self.assertEqual([str(t.indice) for t in tokens], [linha[0] for linha in linhas[1:]])
        esperado = s.tempos_esperados(pares)
        self.assertEqual({i: esperado[i] for i in range(len(tokens))}, _tempos(out))

    def test_D1_coluna_palavra_e_o_exibido_normalizado(self):
        tokens = s.tokens("T02")
        _codigo, out, _err = _alinhar("T02", [p for _, p in s.palavras_dos_tokens(tokens)])
        palavras = [linha[1] for linha in s.linhas_csv(out)[1:]]
        self.assertEqual([roteiro.normalizar(s.exibido("T02", t)) for t in tokens], palavras)
        self.assertIn("3 16", palavras)


class VariantesTest(unittest.TestCase):
    def test_D2_tres_formas_das_referencias(self):
        tokens = s.tokens("T02")
        alvos = {_indice("T02", "3:16"), _indice("T02", "13:4-7"), _indice("T02", "1 Coríntios")}
        referencias = [t.indice for t in tokens if len(t.formas) in (3, 6) and s.exibido("T02", t)[0].isdigit()]
        self.assertGreaterEqual(len(referencias), 5, "Q3: 3:16, 23:1, 24:14, 12:2 e 13:4-7")
        for take in (0, 1, 2):  # "tres dezesseis", "tres e dezesseis", "capitulo tres versiculo dezesseis"
            with self.subTest(take=take):
                formas = {i: take for i in referencias}
                formas[_indice("T02", "13:4-7")] = ["treze", "de", "quatro", "a", "sete"]
                formas[_indice("T02", "1 Coríntios")] = ["primeira", "corintios"]
                pares = s.palavras_dos_tokens(tokens, formas)
                codigo, out, _err = _alinhar("T02", [p for _, p in pares])
                self.assertEqual(0, codigo)
                tempos = _tempos(out)
                esperado = s.tempos_esperados(pares)
                for i in alvos | set(referencias):
                    self.assertEqual(esperado[i], tempos[i], f"token {i} ({s.exibido('T02', tokens[i])})")
                ref = _indice("T02", "3:16")
                self.assertEqual(len(tokens[ref].formas[take]) - 1, (tempos[ref][1] - tempos[ref][0] - 250) // 300)


class EventosTest(unittest.TestCase):
    def test_D3_pulo_de_paragrafo_t05(self):
        par = s.paragrafos("T05")
        self.assertEqual(5, len(par))
        tokens = s.tokens("T05")
        pares = [x for x in s.palavras_dos_tokens(tokens) if x[0] not in par[1]]
        codigo, out, _err = _alinhar("T05", [p for _, p in pares])
        self.assertEqual(0, codigo)
        tempos = _tempos(out)
        esperado = s.tempos_esperados(pares)
        for i in range(len(tokens)):
            if i in par[1]:
                self.assertIsNone(tempos[i], f"token {i} do 2º parágrafo")
            else:
                self.assertEqual(esperado[i], tempos[i], f"token {i}")

    def test_D4_improviso_t04_nao_da_tempo_a_token(self):
        par = s.paragrafos("T04")
        tokens = s.tokens("T04")
        improviso = [p for _, p in s.palavras_dos_tokens(s.tokens("T03"))]
        base = s.palavras_dos_tokens(tokens)
        corte = max(n for n, (i, _) in enumerate(base) if i in par[0]) + 1
        pares = base[:corte] + [(None, p) for p in improviso] + base[corte:]
        codigo, out, err = _alinhar("T04", [p for _, p in pares])
        self.assertEqual(0, codigo)
        tempos = _tempos(out)
        esperado = s.tempos_esperados(pares)
        self.assertEqual({i: esperado[i] for i in range(len(tokens))}, tempos)
        a, z = corte, corte + len(improviso) - 1
        self.assertIn(f"inseridas: palavras {a}-{z} [{300 * a}-{300 * z + 250} ms]", err)
        self.assertIn(f"palavras inseridas: {len(improviso)}", err)

    def test_D5_repeticao_t08_fica_com_a_primeira_vez(self):
        tokens = s.tokens("T08")
        frase = [_indice("T08", w) for w in ("Isso",)]
        inicio = frase[0]
        indices = list(range(inicio, inicio + 4))  # Isso tem um custo
        base = s.palavras_dos_tokens(tokens)
        corte = max(n for n, (i, _) in enumerate(base) if i == indices[-1]) + 1
        repeticao = [p for i, p in base if i in indices]
        self.assertEqual(["isso", "tem", "um", "custo"], repeticao)
        pares = base[:corte] + [(None, p) for p in repeticao] + base[corte:]
        codigo, out, err = _alinhar("T08", [p for _, p in pares])
        self.assertEqual(0, codigo)
        tempos = _tempos(out)
        esperado = s.tempos_esperados(pares)
        for i in indices:
            self.assertEqual(esperado[i], tempos[i])
        self.assertIn(f"inseridas: palavras {corte}-{corte + 3} [{300 * corte}-{300 * (corte + 3) + 250} ms]", err)

    def test_D6_refrao_t03_cada_ocorrencia_com_o_seu_tempo(self):
        tokens = s.tokens("T03")
        texto = roteiro.ler_texto("T03")
        inicios = [t.indice for t in tokens if texto.startswith("E a escolha é sempre nossa", t.inicio)]
        self.assertEqual(3, len(inicios))
        pares = s.palavras_dos_tokens(tokens)
        codigo, out, _err = _alinhar("T03", [p for _, p in pares])
        self.assertEqual(0, codigo)
        tempos = _tempos(out)
        esperado = s.tempos_esperados(pares)
        for k in inicios:
            for i in range(k, k + 6):
                self.assertEqual(esperado[i], tempos[i])
        self.assertLess(tempos[inicios[0]][0], tempos[inicios[1]][0])
        self.assertLess(tempos[inicios[1]][0], tempos[inicios[2]][0])

    def test_D7_palavras_com_acento_e_maiuscula_como_o_vosk_grande(self):
        """Cada palavra só de letras sai como está no texto (Em, João, Repare...): todas casam depois do normalizador."""
        tokens = s.tokens("T02")
        formas = {t.indice: [s.exibido("T02", t)] for t in tokens if s.exibido("T02", t).isalpha()}
        self.assertIn("João", [f[0] for f in formas.values()])
        pares = s.palavras_dos_tokens(tokens, formas)
        _codigo, out, err = _alinhar("T02", [p for _, p in pares])
        self.assertEqual(s.tempos_esperados(pares), _tempos(out))
        self.assertIn("palavras inseridas: 0", err)

    def test_D7_acento_e_hifen_da_transcricao(self):
        tokens = s.tokens("T02")
        ref = _indice("T02", "3:16")
        pares = s.palavras_dos_tokens(tokens, {ref: ["capítulo", "três", "Versículo", "dezesseis"]})
        _codigo, out, _err = _alinhar("T02", [p for _, p in pares])
        self.assertEqual(s.tempos_esperados(pares)[ref], _tempos(out)[ref])
        tokens = s.tokens("T03")
        bom = _indice("T03", "bom-dia")
        pares = s.palavras_dos_tokens(tokens, {bom: ["bom", "dia"]})
        _codigo, out, _err = _alinhar("T03", [p for _, p in pares])
        self.assertEqual(s.tempos_esperados(pares)[bom], _tempos(out)[bom])
        self.assertIsNotNone(_tempos(out)[bom])


class DesempateTest(unittest.TestCase):
    def test_plano_A3_improviso_que_termina_com_palavra_do_roteiro_nao_rouba_o_tempo(self):
        """T09 tem "por isso que estamos aqui": o "Isso" do T01 fica com a sua ocorrência real."""
        par = s.paragrafos("T04")
        tokens = s.tokens("T04")
        improviso = [p for _, p in s.palavras_dos_tokens(s.tokens("T09"))]
        self.assertIn("isso", improviso)
        base = s.palavras_dos_tokens(tokens)
        corte = max(n for n, (i, _) in enumerate(base) if i in par[0]) + 1
        pares = base[:corte] + [(None, p) for p in improviso] + base[corte:]
        codigo, out, _err = _alinhar("T04", [p for _, p in pares])
        self.assertEqual(0, codigo)
        esperado = s.tempos_esperados(pares)
        self.assertEqual({i: esperado[i] for i in range(len(tokens))}, _tempos(out))

    def test_plano_A3_palavra_comum_antes_do_improviso_fica_antes(self):
        """O improviso do T03 tem "conversa", último token do 1º parágrafo: fica a ocorrência de antes."""
        par = s.paragrafos("T04")
        tokens = s.tokens("T04")
        improviso = [p for _, p in s.palavras_dos_tokens(s.tokens("T03"))]
        self.assertIn("conversa", improviso)
        ultimo = par[0][-1]
        self.assertEqual("conversa", tokens[ultimo].formas[0][0])
        base = s.palavras_dos_tokens(tokens)
        corte = max(n for n, (i, _) in enumerate(base) if i in par[0]) + 1
        pares = base[:corte] + [(None, p) for p in improviso] + base[corte:]
        _codigo, out, _err = _alinhar("T04", [p for _, p in pares])
        self.assertEqual(s.tempos_esperados(pares)[ultimo], _tempos(out)[ultimo])

    def test_plano_A3_substituicao_nao_da_tempo(self):
        tokens = s.tokens("T01")
        pares = s.palavras_dos_tokens(tokens, {1: ["mal"]})  # "bem" -> "mal"
        _codigo, out, _err = _alinhar("T01", [p for _, p in pares])
        tempos = _tempos(out)
        self.assertIsNone(tempos[1])
        self.assertEqual((0, 250), tempos[0])
        self.assertEqual((600, 850), tempos[2])


class DesempateMinimoTest(unittest.TestCase):
    """Casos mínimos com tokens do T02 (achados por busca de mutações na prova do plano) que travam o D6 e o A3:
    menos edições primeiro; depois menos trechos (inclusive o de inserção no começo); depois a primeira forma."""

    def _tempos(self, inicio, fim, palavras):
        tokens = [roteiro.Token(k, t.inicio, t.fim, t.formas) for k, t in enumerate(s.tokens("T02")[inicio:fim])]
        transcricao = [alinhar.Palavra(p, 300 * n, 300 * n + 250) for n, p in enumerate(palavras)]
        pares, inseridas = alinhar.alinhar(tokens, transcricao)
        return alinhar.tempos_dos_tokens(tokens, transcricao, pares), inseridas

    def test_plano_A3_uma_edicao_vale_mais_que_os_trechos(self):
        tempos, inseridas = self._tempos(7, 9, ["mais", "livro", "de", "comecar", "fala"])
        self.assertEqual([(0, 250), (600, 850)], tempos)
        self.assertEqual([1, 3, 4], inseridas)

    def test_plano_A3_insercao_no_comeco_tambem_e_trecho(self):
        tempos, inseridas = self._tempos(53, 58, ["muita", "versiculo", "dois", "tambem", "vale", "ler", "corintios"])
        self.assertEqual([(0, 850), (900, 1150), (1200, 1450), (1500, 1750), (1800, 2050)], tempos)
        self.assertEqual([], inseridas)

    def test_plano_A3_empate_de_formas_fica_com_a_primeira(self):
        tempos, inseridas = self._tempos(56, 60, ["ler", "treze", "em", "quatro", "a", "sete", "do"])
        self.assertEqual([(0, 250), None, (600, 1750), None], tempos)
        self.assertEqual([], inseridas)


class GuardaTest(unittest.TestCase):
    def test_D8_texto_diferente_de_etapa0_textos_sai_com_2_sem_escrever(self):
        original = (roteiro.TEXTOS / "T01.txt").read_bytes()
        transcricao = s.transcricao_csv(["ouvir", "bem"])
        for nome, conteudo in (("mais", original + b"x"), ("menos", original[:-1])):
            with self.subTest(caso=nome), s.Pasta() as p:
                destino = p.dir / "saida.expected.csv"
                argv = [p.arquivo("T01.txt", conteudo), p.arquivo("t.csv", transcricao), "-o", str(destino)]
                codigo, out, err = s.rodar(alinhar.main, argv)
                self.assertEqual(2, codigo)
                self.assertEqual("", out)
                self.assertIn("etapa0/textos", err)
                self.assertFalse(destino.exists())

    def test_transcricao_invalida_sai_com_2(self):
        casos = {
            "cabeçalho": "word,start,end,conf\nouvir,0,250,1.0\n",
            "tempo não inteiro": "palavra,t_inicio_ms,t_fim_ms,conf\nouvir,0.5,250,1.0\n",
            "fim antes do início": "palavra,t_inicio_ms,t_fim_ms,conf\nouvir,300,250,1.0\n",
            "fora de ordem": "palavra,t_inicio_ms,t_fim_ms,conf\nouvir,300,550,1.0\nbem,0,250,1.0\n",
            "campos": "palavra,t_inicio_ms,t_fim_ms,conf\nouvir,0,250\n",
        }
        for nome, conteudo in casos.items():
            with self.subTest(caso=nome), s.Pasta() as p:
                argv = [str(roteiro.TEXTOS / "T01.txt"), p.arquivo("t.csv", conteudo)]
                codigo, out, _err = s.rodar(alinhar.main, argv)
                self.assertEqual(2, codigo)
                self.assertEqual("", out)

    def test_palavra_de_duracao_zero_e_aceita(self):
        with s.Pasta() as p:
            conteudo = "palavra,t_inicio_ms,t_fim_ms,conf\nouvir,0,0,1.0\nbem,0,250,\n"
            codigo, out, err = s.rodar(alinhar.main, [str(roteiro.TEXTOS / "T01.txt"), p.arquivo("t.csv", conteudo)])
        self.assertEqual(0, codigo, err)
        self.assertEqual(["0", "ouvir", "0", "0"], s.linhas_csv(out)[1])

    def test_saida_em_arquivo_com_o(self):
        with s.Pasta() as p:
            destino = str(p.dir / "x.expected.csv")
            codigo, out, _err = _alinhar("T09", [w for _, w in s.palavras_dos_tokens(s.tokens("T09"))], destino)
            self.assertEqual(0, codigo)
            self.assertEqual("", out)
            self.assertTrue(Path(destino).read_text(encoding="utf-8").startswith("token,palavra,t_inicio_ms,t_fim_ms\n"))


class DeterminismoTest(unittest.TestCase):
    def test_D9_mesma_entrada_mesma_saida(self):
        par = s.paragrafos("T04")
        base = s.palavras_dos_tokens(s.tokens("T04"))
        corte = max(n for n, (i, _) in enumerate(base) if i in par[0]) + 1
        palavras = [p for _, p in base[:corte]] + [p for _, p in s.palavras_dos_tokens(s.tokens("T03"))]
        palavras += [p for _, p in base[corte:]]
        primeira = _alinhar("T04", palavras)
        segunda = _alinhar("T04", palavras)
        self.assertEqual(0, primeira[0])
        self.assertEqual(primeira, segunda)

    def test_D9_l30_sintetico_completo(self):
        tokens = s.tokens("L30")
        pares = s.palavras_dos_tokens(tokens)
        inicio = time.perf_counter()
        codigo, out, _err = _alinhar("L30", [p for _, p in pares])
        segundos = time.perf_counter() - inicio
        sys.stderr.write(f"\nD9: L30 sintético ({len(tokens)} tokens, {len(pares)} palavras) em {segundos:.2f} s\n")
        self.assertEqual(0, codigo)
        tempos = _tempos(out)
        self.assertEqual(len(tokens), len(tempos))
        self.assertTrue(all(v is not None for v in tempos.values()))


if __name__ == "__main__":
    unittest.main()
````

- [ ] **Passo 3: ver falhar**

Rodar: `python3 etapa0/ferramentas/testar.py`
Esperado: código 1; `ImportError: Failed to import test module: testes.test_alinhar` (`No module named 'alinhar'`); `testar.py: 19 testes, 0 falhas, 1 erros, 0 pulados`.

- [ ] **Passo 4: escrever o `alinhar.py`**

`etapa0/ferramentas/alinhar.py` (Needleman–Wunsch completo, três estados por célula, desempate de A3):

````python
"""alinhar.py <texto> <transcricao> [-o SAIDA]: roteiro x transcrição com tempos -> expected.csv.

TESTES-ETAPA0, "Trajetória esperada", passos 2 e 4; spec 0004, D6 a D9.

Needleman-Wunsch em nível de palavra sobre a treliça das formas faladas do Tokenizer:
- custo unitário (igual 0; substituição, inserção e omissão 1), igualdade exata depois do normalizador;
- cada token entra com exatamente uma forma falada, a de menor custo;
- desempate (impl_plan 0004, A3): entre alinhamentos de mesmo custo, o de menos trechos de inserção ou
  omissão; persistindo o empate, vale a primeira forma do token e a primeira ocorrência na transcrição;
- o token recebe tempo só se ao menos uma palavra da forma escolhida casar exatamente.

Só aceita texto idêntico a um de etapa0/textos/ (D8). Escreve só no stdout, no stderr e no -o.
"""

import argparse
import csv
import io
import sys
from array import array
from dataclasses import dataclass

import roteiro

CABECALHO_TRANSCRICAO = ["palavra", "t_inicio_ms", "t_fim_ms", "conf"]
CABECALHO_ESPERADO = ["token", "palavra", "t_inicio_ms", "t_fim_ms"]

_M, _I, _D = 0, 1, 2  # estados: casamento/substituição, inserção (palavra só da transcrição), omissão
_PRIORIDADE = (_I, _M, _D)  # empate na volta: inserção antes de casamento antes de omissão (A3)
_INF = 1 << 30  # "infinito" que cabe em array("i") (4 bytes por célula) mesmo somado a um custo


class ErroEntrada(Exception):
    """Entrada inválida: código de saída 2."""


@dataclass(frozen=True)
class Palavra:
    texto: str  # já normalizada
    inicio: int
    fim: int


def ler_transcricao(conteudo):
    """CSV palavra,t_inicio_ms,t_fim_ms,conf (D5) -> lista de Palavra normalizadas."""
    linhas = list(csv.reader(io.StringIO(conteudo, newline="")))
    if not linhas or linhas[0] != CABECALHO_TRANSCRICAO:
        raise ErroEntrada("transcrição: cabeçalho diferente de palavra,t_inicio_ms,t_fim_ms,conf")
    saida = []
    anterior = None
    for n, campos in enumerate(linhas[1:], start=1):
        if len(campos) != 4:
            raise ErroEntrada(f"transcrição: linha {n} sem 4 campos")
        try:
            inicio, fim = int(campos[1]), int(campos[2])
            if campos[3] != "":
                float(campos[3])
        except ValueError:
            raise ErroEntrada(f"transcrição: linha {n} com número inválido") from None
        if inicio < 0 or fim < inicio:
            raise ErroEntrada(f"transcrição: linha {n} com tempo inválido")
        if anterior is not None and inicio < anterior:
            raise ErroEntrada(f"transcrição: linha {n} fora da ordem do tempo")
        anterior = inicio
        for p in roteiro.palavras(campos[0]):  # "bom-dia" vira uma palavra; vazio não entra (D4)
            saida.append(Palavra(p, inicio, fim))
    return saida


def _trelica(tokens):
    """Linhas da treliça, na ordem: (token, posição na forma, palavra); e a última linha de cada forma."""
    linhas, ultimas = [], []
    for t in tokens:
        ultimas.append([])
        for forma in t.formas:
            for k, palavra in enumerate(forma):
                linhas.append((t.indice, k, palavra))
            ultimas[-1].append(len(linhas) - 1)
    return linhas, ultimas


def _custos(peso):
    """(abrir, estender) um trecho de inserção ou omissão: abrir soma uma edição e um trecho (A3)."""
    return peso + 1, peso


def _linha(pm, pi, pd, palavra, alvo, peso):
    """Uma linha da treliça a partir da anterior (pm, pi, pd): devolve (M, I, D) em array("i")."""
    abre, estende = _custos(peso)
    melhor = list(map(min, pm, pi, pd))
    lm = [_INF] + [min(b + (0 if a == palavra else peso), _INF) for b, a in zip(melhor, alvo)]
    ld = [min(x + abre, y + abre, z + estende, _INF) for x, y, z in zip(pm, pi, pd)]
    li = [_INF]
    corrente = _INF
    for j in range(1, len(alvo) + 1):
        anterior = lm[j - 1] if lm[j - 1] < ld[j - 1] else ld[j - 1]
        corrente = min(anterior + abre, corrente + estende, _INF)
        li.append(corrente)
    return array("i", lm), array("i", li), array("i", ld)


def alinhar(tokens, transcricao):
    """Alinhamento global (Needleman-Wunsch com contagem de trechos, A3).

    Devolve (pares, inseridas): pares[i] = [(índice da palavra, casou exatamente)] alinhadas ao token i;
    inseridas = índices das palavras da transcrição que ficaram como inserção, em ordem.
    """
    alvo = [p.texto for p in transcricao]
    m = len(alvo)
    linhas, ultimas = _trelica(tokens)
    peso = len(linhas) + m + 2  # uma edição vale mais que todos os trechos possíveis juntos
    if (len(linhas) + m + 1) * (peso + 1) >= _INF // 2:
        raise ErroEntrada("roteiro e transcrição grandes demais para o alinhamento")  # o L30 usa ~1/10 do limite
    inicio = (
        array("i", [0] + [_INF] * m),
        array("i", [_INF] + [_custos(peso)[0] + (j - 1) * _custos(peso)[1] for j in range(1, m + 1)]),
        array("i", [_INF] * (m + 1)),
    )
    fronteiras = [inicio]  # fronteiras[b] = estado antes do token b
    guardadas = []  # por linha da treliça: (M, I, D)
    for t in tokens:
        fins = []
        for forma in t.formas:
            pm, pi, pd = fronteiras[-1]
            for palavra in forma:
                pm, pi, pd = _linha(pm, pi, pd, palavra, alvo, peso)
                guardadas.append((pm, pi, pd))
            fins.append((pm, pi, pd))
        if len(fins) == 1:
            fronteiras.append(fins[0])
        else:
            fronteiras.append(tuple(array("i", map(min, *[fim[s] for fim in fins])) for s in (_M, _I, _D)))
    return _voltar(tokens, linhas, ultimas, guardadas, fronteiras, alvo, peso)


def _voltar(tokens, linhas, ultimas, guardadas, fronteiras, alvo, peso):
    """Volta do fim ao início. Empate: inserção, depois casamento, depois omissão; forma de menor índice (A3)."""
    abre, estende = _custos(peso)
    pares = [[] for _ in tokens]
    inseridas = []
    j = len(alvo)
    b = len(tokens)
    estado = min(_PRIORIDADE, key=lambda s: fronteiras[b][s][j])  # min() fica com o primeiro empatado
    linha = None  # None = na fronteira b
    while True:
        if linha is None:
            if b == 0:
                if estado == _I:
                    inseridas.extend(range(j - 1, -1, -1))
                break
            valor = fronteiras[b][estado][j]
            linha = next(u for u in ultimas[b - 1] if guardadas[u][estado][j] == valor)
            continue
        indice, k, palavra = linhas[linha]
        atual = guardadas[linha][estado][j]
        pai = fronteiras[indice] if k == 0 else guardadas[linha - 1]
        if estado == _I:
            inseridas.append(j - 1)
            mesma = guardadas[linha]
            estado = next(s for s in _PRIORIDADE if mesma[s][j - 1] + (estende if s == _I else abre) == atual)
            j -= 1
            continue
        if estado == _M:
            exato = alvo[j - 1] == palavra
            pares[indice].append((j - 1, exato))
            custo = 0 if exato else peso
            estado = next(s for s in _PRIORIDADE if pai[s][j - 1] + custo == atual)
            j -= 1
        else:
            estado = next(s for s in _PRIORIDADE if min(pai[s][j] + (estende if s == _D else abre), _INF) == atual)
        if k == 0:
            linha = None
            b = indice
        else:
            linha -= 1
    inseridas.reverse()
    return pares, inseridas


def tempos_dos_tokens(tokens, transcricao, pares):
    tempos = []
    for t in tokens:
        p = pares[t.indice]
        if any(exato for _, exato in p):
            js = [j for j, _ in p]
            tempos.append((transcricao[min(js)].inicio, transcricao[max(js)].fim))
        else:
            tempos.append(None)
    return tempos


def esperado_csv(texto, tokens, tempos):
    saida = io.StringIO()
    w = csv.writer(saida, lineterminator="\n")
    w.writerow(CABECALHO_ESPERADO)
    for t, tempo in zip(tokens, tempos):
        palavra = roteiro.normalizar(texto[t.inicio:t.fim + 1])
        if tempo is None:
            w.writerow([t.indice, palavra, "", ""])
        else:
            w.writerow([t.indice, palavra, tempo[0], tempo[1]])
    return saida.getvalue()


def _faixas(indices):
    """[3,4,5,9] -> [(3,5),(9,9)]."""
    faixas = []
    for i in indices:
        if faixas and i == faixas[-1][1] + 1:
            faixas[-1][1] = i
        else:
            faixas.append([i, i])
    return [tuple(f) for f in faixas]


def resumo(tokens, transcricao, tempos, inseridas):
    """Roteiro da revisão manual (D9): contagens e faixas, no stderr."""
    sem_tempo = [t.indice for t, tempo in zip(tokens, tempos) if tempo is None]
    linhas = [
        f"tokens com tempo: {len(tokens) - len(sem_tempo)}",
        f"tokens sem tempo: {len(sem_tempo)}",
        f"palavras inseridas: {len(inseridas)}",
    ]
    for a, z in _faixas(sem_tempo):
        linhas.append(f"sem tempo: tokens {a}-{z}")
    for a, z in _faixas(inseridas):
        trecho = " ".join(p.texto for p in transcricao[a:z + 1])
        linhas.append(f"inseridas: palavras {a}-{z} [{transcricao[a].inicio}-{transcricao[z].fim} ms] {trecho}")
    return "\n".join(linhas) + "\n"


def executar(conteudo_texto, conteudo_transcricao):
    """Bytes do texto + texto da transcrição -> (expected.csv, resumo). Lança ErroEntrada."""
    if roteiro.identificar_texto(conteudo_texto) is None:
        raise ErroEntrada("o texto não é idêntico a nenhum de etapa0/textos/ (spec 0004, D8)")
    texto = conteudo_texto.decode("utf-8")
    tokens = roteiro.tokenizar(texto)
    transcricao = ler_transcricao(conteudo_transcricao)
    pares, inseridas = alinhar(tokens, transcricao)
    tempos = tempos_dos_tokens(tokens, transcricao, pares)
    return esperado_csv(texto, tokens, tempos), resumo(tokens, transcricao, tempos, inseridas)


def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="alinhar.py",
        description="Alinha a transcrição com tempos ao roteiro (Needleman-Wunsch) e gera o expected.csv.",
    )
    parser.add_argument("texto", help="texto de teste, idêntico a um de etapa0/textos/")
    parser.add_argument("transcricao", help="CSV palavra,t_inicio_ms,t_fim_ms,conf (saída do timestamps.py)")
    parser.add_argument("-o", "--saida", help="arquivo do expected.csv (padrão: stdout)")
    args = parser.parse_args(argv)
    try:
        with open(args.texto, "rb") as f:
            conteudo_texto = f.read()
        with open(args.transcricao, encoding="utf-8", newline="") as f:
            conteudo_transcricao = f.read()
        esperado, texto_resumo = executar(conteudo_texto, conteudo_transcricao)
    except (OSError, UnicodeDecodeError, ErroEntrada) as erro:
        print(f"alinhar.py: {erro}", file=sys.stderr)
        return 2
    sys.stderr.write(texto_resumo)
    if args.saida:
        with open(args.saida, "w", encoding="utf-8", newline="") as f:
            f.write(esperado)
    else:
        sys.stdout.write(esperado)
    return 0


if __name__ == "__main__":
    sys.exit(main())
````

- [ ] **Passo 5: ver passar**

Rodar: `python3 etapa0/ferramentas/testar.py`
Esperado: código 0; `testar.py: 39 testes, 0 falhas, 0 erros, 0 pulados`; no meio da saída, `D9: L30 sintético (3668 tokens, 3684 palavras) em X s` (na prova, X = 3,65 no Apple M4; se passar de 60 s, parar e escalar ao arquiteto com a saída: é premissa de desempenho do plano).

- [ ] **Passo 6: commit**

```bash
git add etapa0/ferramentas/alinhar.py etapa0/ferramentas/testes/sintetico.py etapa0/ferramentas/testes/test_alinhar.py
git commit -m "feat(0004): alinhar.py, Needleman-Wunsch com formas faladas e desempate por trechos (spec 0004 D1-D9)"
```

### Tarefa 5 (motor-voz): `metricas.py` (spec E1–E12; A5–A10)

**Arquivos:**
- Criar: `etapa0/ferramentas/testes/test_metricas.py`
- Criar depois do vermelho: `etapa0/ferramentas/metricas.py`

Algoritmo de cada métrica, com a linha do `docs/TESTES-ETAPA0.md` de onde vem (o código abaixo implementa exatamente isto):

| Métrica | Fonte | Regra no `metricas.py` |
|---|---|---|
| Amostragem | linha 130 ("amostrando a cada 100 ms") | `t` = primeiro múltiplo de 100 ≥ max(0, `t` da 1ª linha da trajetória) até max(último `t_fim_ms`, último `t_ms` da trajetória), de 100 em 100 (A6) |
| Esperado | linha 132 | `esperado(t)` = maior índice de token com `t_fim_ms ≤ t`; −1 antes do primeiro; token sem tempo não conta |
| Cursor | linha 130 (`trajectory.csv`) | `cursor(t)` = cursor da última linha com `t_ms ≤ t`; linha `Stay` ignorada |
| Intervalos | linhas 110 e 133 | `[início, fim)` de improviso, silêncio, pausa (`pausa_toque`→`retoma_toque`) e `[pulo_fim, pulo_fim + 3000)` |
| Janela de toque | spec D15; A7 | `[t_toque` (ou `t` da `Reposition` para *i* logo antes)`, t_fim` do 1º token com tempo e índice ≥ *i*`)` |
| Atraso | linha 133 | nas amostras fora dos intervalos e das janelas: `esperado − cursor`; PASSA se `n > 0` e `100 × (nº com atraso ≤ 3) ≥ 95 × n` |
| Salto errado | linha 134 | (a) episódios de amostras com `cursor − esperado > 2` fora das janelas de toque (A8); (b) cada linha `Short`/`LongJump`/`Recovery` com `t_ms` em improviso, silêncio ou pausa, classe "alcance" se `cursor ≤ esperado(início do intervalo)`; PASSA se (a) + (b) = 0 |
| Recuperação | linha 135; A5 | para cada `pulo_fim`: 1ª amostra `t ≥ pulo_fim` com `esperado(t) ≥ alvo` e atraso ≤ 3, `alvo` = 1º token com tempo e índice ≥ detalhe do `pulo_inicio`; PASSA se todas ≤ 3000 ms; sem amostra, "não recuperou"; sem pulo, NAO_SE_APLICA |
| Recuo | linha 136 | `hw` = cursor de cada `Reposition`; cada movimento automático com `cursor < hw − 3` é violação; depois `hw = max(hw, cursor)`; PASSA se zero |
| Latências | linha 137; TRD Metas; D18 | fala → cursor = 1ª linha com cursor ≥ *i* (se automática) − `t_fim(i)`, nos tokens com `t_fim` em leitura normal; latência 1 e 2 com `--hipoteses`, só tokens elegíveis (A9); mediana, p95 (posto mais próximo) e máximo; meta provisória 500 ms ao lado; não mudam o código |

- [ ] **Passo 1: escrever os testes das métricas**

`etapa0/ferramentas/testes/test_metricas.py`:

````python
"""metricas.py com esperados, trajetórias e eventos sintéticos (spec 0004, E1 a E12; impl_plan A5 a A9).

Esperado base: tokens do T01, token i em [300i, 300i + 250] (sintetico.tempos_regulares). Os números do
veredito vêm do JSON de --json.
"""

import hashlib
import unittest

import metricas
from testes import sintetico as s

N = len(s.tokens("T01"))


def _base(sem_tempo=(), deslocamento=None):
    tempos = s.tempos_regulares(N, sem_tempo, deslocamento)
    return tempos, s.esperado_csv("T01", tempos)


def _medir(linhas, esperado, eventos=(), arquivos_eventos=None, **kw):
    arquivos = arquivos_eventos if arquivos_eventos is not None else [s.eventos_csv(eventos)]
    return s.rodar_metricas(metricas.main, s.trajetoria_csv(linhas), esperado, arquivos, **kw)


def _sem_proveniencia(dados):
    return {k: v for k, v in dados.items() if k not in ("entradas", "deslocamento_ms")}


class AtrasoTest(unittest.TestCase):
    def test_E1_dois_atras_o_tempo_todo_passa(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 2)
        codigo, dados, _out, _err = _medir(linhas, esperado)
        a = dados["criterios"]["atraso"]
        self.assertEqual(0, codigo)
        self.assertEqual("PASSA", a["veredito"])
        self.assertEqual(1.0, a["fracao"])
        self.assertEqual(2, a["maior_atraso"])
        self.assertGreaterEqual(a["amostras"], 600, "Q3")
        self.assertEqual("PASSA", dados["veredito"])

    def test_E1_atraso_3_ainda_conta_como_dentro(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 3)
        codigo, dados, _out, _err = _medir(linhas, esperado)
        a = dados["criterios"]["atraso"]
        self.assertEqual((1.0, 3, "PASSA"), (a["fracao"], a["maior_atraso"], a["veredito"]))
        self.assertEqual(0, codigo)

    def _com_janela(self, fim_janela, eventos=(), lag_no_intervalo=None):
        """600 amostras (tokens >= 200 sem tempo); atraso 4 em [30000, fim_janela); atraso 2 no resto."""
        tempos, esperado = _base(sem_tempo=range(200, N))
        intervalos = [(a, b) for (a, _), (b, _) in zip(eventos[::2], eventos[1::2])]

        def cursor(t, e):
            if lag_no_intervalo is not None and any(a <= t < b for a, b in intervalos):
                return e - lag_no_intervalo
            return e - (4 if 30000 <= t < fim_janela else 2)

        extras = [30000, fim_janela] + [t for par in intervalos for t in par]
        linhas = s.trajetoria_seguindo(tempos, cursor, extras=extras, move="Reposition")
        linhas[0] = (0, 0, "Reposition")
        evs = [(t, nome, "") for t, nome in eventos]
        return _medir(linhas, esperado, evs)

    def test_E2_atraso_4_em_5_por_cento_passa(self):
        codigo, dados, _out, _err = self._com_janela(33000)
        a = dados["criterios"]["atraso"]
        self.assertEqual(600, a["amostras"])
        self.assertEqual(570, a["ate_3"])
        self.assertEqual("PASSA", a["veredito"])
        self.assertEqual(0, codigo)

    def test_E2_atraso_4_em_6_por_cento_reprova(self):
        codigo, dados, _out, _err = self._com_janela(33600)
        a = dados["criterios"]["atraso"]
        self.assertEqual(564, a["ate_3"])
        self.assertEqual("REPROVA", a["veredito"])
        self.assertEqual(1, codigo)

    def test_E2_intervalos_de_excecao_nao_entram_na_conta(self):
        casos = {
            "improviso": [(40000, "improviso_inicio"), (43000, "improviso_fim")],
            "silencio": [(40000, "silencio_inicio"), (43000, "silencio_fim")],
            "pausa": [(40000, "pausa_toque"), (43000, "retoma_toque")],
        }
        for nome, eventos in casos.items():
            with self.subTest(intervalo=nome):
                _c, normal, _o, _e = self._com_janela(33000, eventos, lag_no_intervalo=2)
                _c, atrasado, _o, _e = self._com_janela(33000, eventos, lag_no_intervalo=50)
                self.assertEqual(570, normal["criterios"]["atraso"]["amostras"])
                self.assertEqual(normal["criterios"]["atraso"], atrasado["criterios"]["atraso"])

    def test_E2_tres_segundos_depois_do_pulo_nao_entram_na_conta(self):
        tempos, esperado = _base(sem_tempo=range(200, N))

        def medir(lag):
            def cursor(t, e):
                return e - (lag if 40000 <= t < 43000 else 2)

            linhas = s.trajetoria_seguindo(tempos, cursor, extras=[40000, 43000], move="Reposition")
            evs = [(39000, "pulo_inicio", "130"), (40000, "pulo_fim", "")]
            return _medir(linhas, esperado, evs)[1]["criterios"]["atraso"]

        self.assertEqual(570, medir(2)["amostras"])
        self.assertEqual(medir(2), medir(50))

    def test_atraso_sem_amostras_de_leitura_normal_reprova(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e)
        fim = 300 * (N - 1) + 250
        codigo, dados, _o, _e = _medir(linhas, esperado, [(0, "silencio_inicio", ""), (fim + 1, "silencio_fim", "")])
        a = dados["criterios"]["atraso"]
        self.assertEqual(0, a["amostras"])
        self.assertEqual("REPROVA", a["veredito"])
        self.assertEqual(1, codigo)


class SaltoErradoTest(unittest.TestCase):
    def _salto_em_30100(self, a_frente):
        tempos, esperado = _base()
        esperado_em = s.fim_em(tempos)
        alvo = esperado_em(30100) + a_frente  # esperado(30100) = 99

        def cursor(t, e):
            return max(e - 1, alvo) if t >= 30100 else e - 1

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[30100])
        return _medir(linhas, esperado)

    def test_E3_tres_a_frente_por_200_ms_conta(self):
        codigo, dados, _o, _e = self._salto_em_30100(3)
        sal = dados["criterios"]["salto_errado"]
        self.assertGreaterEqual(sal["total"], 1)
        self.assertEqual([30100, 30200, 2], [sal["por_posicao"][0][k] for k in ("inicio_ms", "fim_ms", "amostras")])
        self.assertEqual("REPROVA", sal["veredito"])
        self.assertEqual(1, codigo)

    def test_E3_dois_saltos_separados_contam_dois(self):
        tempos, esperado = _base()

        def cursor(t, e):
            if 30100 <= t < 30300 or 50100 <= t < 50300:
                return e + 3
            return e - 1

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[30100, 30300, 50100, 50300])
        _c, dados, _o, _e = _medir(linhas, esperado)
        sal = dados["criterios"]["salto_errado"]
        self.assertEqual(2, sal["total"])
        self.assertEqual([30100, 50100], [ep["inicio_ms"] for ep in sal["por_posicao"]])

    def test_E3_dois_a_frente_nao_conta(self):
        codigo, dados, _o, _e = self._salto_em_30100(2)
        self.assertEqual(0, dados["criterios"]["salto_errado"]["total"])
        self.assertEqual(0, codigo)

    def _movimento(self, inicio, fim, t_short):
        """Tokens >= 100 depois de um intervalo [30000, 40000) sem fala; Short para o esperado em t_short."""
        tempos, esperado = _base(deslocamento=lambda i: 10000 if i >= 100 else 0)

        def cursor(t, e):
            return e if t >= t_short and e == 99 else e - 1

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[t_short])
        self.assertIn((t_short, 99, "Short"), linhas)
        return _medir(linhas, esperado, [(30000, inicio, ""), (40000, fim, "")])

    def test_E4_movimento_automatico_no_intervalo_conta_como_alcance(self):
        for inicio, fim in (("silencio_inicio", "silencio_fim"), ("improviso_inicio", "improviso_fim"),
                            ("pausa_toque", "retoma_toque")):
            with self.subTest(intervalo=inicio):
                codigo, dados, _o, _e = self._movimento(inicio, fim, 35000)
                sal = dados["criterios"]["salto_errado"]
                self.assertEqual(1, sal["total"])
                self.assertEqual([], sal["por_posicao"])
                mov = sal["por_movimento"][0]
                self.assertEqual((35000, 99, "Short", "alcance", 99),
                                 (mov["t_ms"], mov["cursor"], mov["move"], mov["classe"], mov["esperado_no_inicio"]))
                self.assertEqual("REPROVA", sal["veredito"])
                self.assertEqual(1, codigo)

    def test_E4_um_ms_depois_do_fim_nao_conta(self):
        for inicio, fim in (("silencio_inicio", "silencio_fim"), ("improviso_inicio", "improviso_fim"),
                            ("pausa_toque", "retoma_toque")):
            with self.subTest(intervalo=inicio):
                codigo, dados, _o, _e = self._movimento(inicio, fim, 40001)
                self.assertEqual(0, dados["criterios"]["salto_errado"]["total"])
                self.assertEqual(0, codigo)

    def test_E4_bordas_do_intervalo(self):
        """Fechado no início e aberto no fim (D12): Short em 30000 conta; em 40000 não conta."""
        codigo, dados, _o, _e = self._movimento("silencio_inicio", "silencio_fim", 30000)
        self.assertEqual(1, dados["criterios"]["salto_errado"]["total"])
        self.assertEqual(1, codigo)
        codigo, dados, _o, _e = self._movimento("silencio_inicio", "silencio_fim", 40000)
        self.assertEqual(0, dados["criterios"]["salto_errado"]["total"])
        self.assertEqual(0, codigo)

    def test_E4_classe_usa_o_esperado_do_inicio_do_intervalo(self):
        """Tokens com fim dentro do improviso: a classe compara com esperado(início), não com esperado(t)."""
        tempos, esperado = _base()

        def cursor(t, e):
            return e - 1 if t < 30000 else (98 if t < 35000 else 101)

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[35000], ate=35000)
        _c, dados, _o, _e = _medir(linhas, esperado, [(30000, "improviso_inicio", ""), (40000, "improviso_fim", "")])
        mov = dados["criterios"]["salto_errado"]["por_movimento"]
        self.assertEqual([(35000, 101, 99, "além do esperado")],
                         [(m["t_ms"], m["cursor"], m["esperado_no_inicio"], m["classe"]) for m in mov])

    def test_E4_alem_do_esperado(self):
        tempos, esperado = _base(deslocamento=lambda i: 10000 if i >= 100 else 0)

        def cursor(t, e):
            return 101 if 35000 <= t < 40000 else (max(e - 1, 101) if t >= 40000 else e - 1)

        linhas = s.trajetoria_seguindo(tempos, cursor, extras=[35000, 40000])
        _c, dados, _o, _e = _medir(linhas, esperado, [(30000, "silencio_inicio", ""), (40000, "silencio_fim", "")])
        mov = dados["criterios"]["salto_errado"]["por_movimento"]
        self.assertEqual("além do esperado", mov[0]["classe"])


class ToqueTest(unittest.TestCase):
    """L30 min 22 e PRD: toque numa palavra 3 parágrafos à frente (spec D15; impl_plan A7)."""

    def _cenario(self, t_reposition=40000, t_toque=40000, short_na_janela=False):
        par = s.paragrafos("T01")
        i = par[3][0]  # 1º token do 4º parágrafo: 3 parágrafos à frente do 1º
        ultimo_lido = par[0][-1]
        retomada = 42000  # o orador começa a ler i 2 s depois do toque

        def deslocamento(k):
            return 0 if k <= ultimo_lido else retomada - 300 * i

        sem_tempo = [k for k in range(N) if ultimo_lido < k < i]
        tempos, esperado = _base(sem_tempo=sem_tempo, deslocamento=deslocamento)
        fim_i = tempos[i][1]
        linhas = [ln for ln in s.trajetoria_seguindo(tempos, lambda t, e: e - 1) if ln[0] < t_reposition]
        linhas.append((t_reposition, i, "Reposition"))
        if short_na_janela:
            linhas.append((t_toque + 500, i + 3, "Short"))
        seguintes = s.trajetoria_seguindo(tempos, lambda t, e: max(e - 1, i + 3 if short_na_janela else i))
        linhas += [ln for ln in seguintes if ln[0] > t_toque + 500 and ln[1] > linhas[-1][1]]
        eventos = [(t_toque, "toque_palavra", str(i))]
        return i, fim_i, _medir(linhas, esperado, eventos)

    def test_E5_toque_tres_paragrafos_a_frente_nao_conta_salto(self):
        i, fim_i, (codigo, dados, out, _e) = self._cenario()
        self.assertEqual(0, dados["criterios"]["salto_errado"]["total"])
        janela = dados["janelas_toque"][0]
        self.assertEqual((i, 40000, fim_i), (janela["indice"], janela["inicio_ms"], janela["fim_ms"]))
        self.assertEqual(len(range(40000, fim_i, 100)), janela["amostras_excluidas"])
        self.assertGreaterEqual(janela["amostras_excluidas"], 20, "Q3")
        self.assertEqual(janela["amostras_excluidas"], dados["amostras"]["excluidas_toque"])
        self.assertIn("janela de toque", out)
        self.assertEqual(0, codigo)

    def test_E5_short_na_janela_conta_na_primeira_amostra_depois(self):
        _i, fim_i, (codigo, dados, _o, _e) = self._cenario(short_na_janela=True)
        sal = dados["criterios"]["salto_errado"]
        self.assertEqual(1, sal["total"])
        primeira = -(-fim_i // 100) * 100
        self.assertEqual(primeira, sal["por_posicao"][0]["inicio_ms"])
        self.assertEqual(3, sal["por_posicao"][0]["maior_avanco"])
        self.assertEqual("REPROVA", sal["veredito"])
        self.assertEqual(1, codigo)

    def test_plano_A7_reposition_um_ms_antes_do_evento_fica_na_janela(self):
        _i, _f, (codigo, dados, _o, _e) = self._cenario(t_reposition=39999, t_toque=40001)
        self.assertEqual(39999, dados["janelas_toque"][0]["inicio_ms"])
        self.assertEqual(0, dados["criterios"]["salto_errado"]["total"])
        self.assertEqual(0, codigo)


    def test_plano_A7_so_a_reposition_para_i_antecipa_a_janela(self):
        _tempos, esperado = _base()
        for linhas in ([(0, 0, "Reposition")], [(0, 0, "Reposition"), (4000, 150, "Short")]):
            with self.subTest(ultima_linha=linhas[-1]):
                _c, dados, _o, _e = _medir(linhas, esperado, [(5000, "toque_palavra", "150")])
                self.assertEqual(5000, dados["janelas_toque"][0]["inicio_ms"])


class RecuperacaoTest(unittest.TestCase):
    """T05: o 2º parágrafo não é lido; pulo_fim no início da 1ª palavra do 3º (spec D16; impl_plan A5)."""

    def _cenario(self, recupera_em):
        par = s.paragrafos("T05")
        pulados = par[1]
        tempos, esperado = _base(sem_tempo=pulados, deslocamento=lambda k: -300 * len(pulados) if k > pulados[-1] else 0)
        primeiro_depois = pulados[-1] + 1
        pulo_fim = tempos[primeiro_depois][0]
        self.assertEqual(0, pulo_fim % 100, "pulo_fim cai numa amostra")
        ultimo_antes = pulados[0] - 1

        def cursor(t, e):
            if t < pulo_fim:
                return e - 1
            if recupera_em is None or t < pulo_fim + recupera_em:
                return ultimo_antes - 1
            return e - 1

        extras = [pulo_fim] + ([pulo_fim + recupera_em] if recupera_em is not None else [])
        linhas = s.trajetoria_seguindo(tempos, cursor, extras=extras, move="Recovery")
        eventos = [(pulo_fim - 100, "pulo_inicio", str(pulados[0])), (pulo_fim, "pulo_fim", "")]
        return _medir(linhas, esperado, eventos)

    def test_E6_recupera_em_2900_passa(self):
        _c, dados, _o, _e = self._cenario(2900)
        r = dados["criterios"]["recuperacao"]
        self.assertEqual(2900, r["pulos"][0]["recuperacao_ms"])
        self.assertEqual("PASSA", r["veredito"])

    def test_E6_recupera_em_3000_passa(self):
        _c, dados, _o, _e = self._cenario(3000)
        r = dados["criterios"]["recuperacao"]
        self.assertEqual(3000, r["pulos"][0]["recuperacao_ms"])
        self.assertEqual("PASSA", r["veredito"])

    def test_plano_A5_alvo_e_o_primeiro_token_com_tempo_a_partir_do_pulo_inicio(self):
        """Recuperação rápida: no 1º token depois do pulo. Com o alvo errado (índice seguinte), daria 600."""
        par = s.paragrafos("T05")
        pulados = par[1]
        tempos, esperado = _base(sem_tempo=pulados, deslocamento=lambda k: -300 * len(pulados) if k > pulados[-1] else 0)
        alvo = pulados[-1] + 1
        p = tempos[alvo][0]
        for detalhe, marca, recuperacao in ((pulados[0], p, 300), (alvo, p, 300), (pulados[0], p + 300, 0)):
            with self.subTest(detalhe=detalhe, pulo_fim=marca):
                linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1 if t < p or t >= p + 250 else pulados[0] - 2,
                                               extras=[p, p + 250])
                eventos = [(p - 100, "pulo_inicio", str(detalhe)), (marca, "pulo_fim", "")]
                _c, dados, _o, _e = _medir(linhas, esperado, eventos)
                self.assertEqual(alvo, dados["criterios"]["recuperacao"]["pulos"][0]["alvo"])
                self.assertEqual(recuperacao, dados["criterios"]["recuperacao"]["pulos"][0]["recuperacao_ms"])

    def test_E6_recupera_em_3100_reprova(self):
        codigo, dados, _o, _e = self._cenario(3100)
        r = dados["criterios"]["recuperacao"]
        self.assertEqual(3100, r["pulos"][0]["recuperacao_ms"])
        self.assertEqual("REPROVA", r["veredito"])
        self.assertEqual(1, codigo)

    def test_E6_nunca_recupera_reprova_mesmo_com_atraso_baixo_no_pulo_fim(self):
        """A5: no pulo_fim o esperado ainda não saltou; a leitura literal daria 0 ms e passaria (Q3)."""
        codigo, dados, _o, _e = self._cenario(None)
        r = dados["criterios"]["recuperacao"]
        self.assertIsNone(r["pulos"][0]["recuperacao_ms"])
        self.assertEqual("REPROVA", r["veredito"])
        self.assertEqual(1, codigo)

    def test_E6_sem_pulo_nao_se_aplica(self):
        tempos, esperado = _base()
        codigo, dados, out, _e = _medir(s.trajetoria_seguindo(tempos, lambda t, e: e - 1), esperado)
        self.assertEqual("NAO_SE_APLICA", dados["criterios"]["recuperacao"]["veredito"])
        self.assertEqual(0, codigo)
        self.assertIn("NAO_SE_APLICA", out)


class RecuoTest(unittest.TestCase):
    def _recuo(self, sequencia):
        _tempos, esperado = _base()
        linhas = [(0, 0, "Reposition")] + [(100 * (k + 1), c, m) for k, (c, m) in enumerate(sequencia)]
        return _medir(linhas, esperado)[1]["criterios"]["recuo"]

    def test_E7_recuo_para_36_depois_de_40_viola(self):
        subida = [(c, "Short") for c in range(5, 41, 5)]
        r = self._recuo(subida + [(36, "Short")])
        self.assertEqual("REPROVA", r["veredito"])
        self.assertEqual([{"t_ms": 900, "cursor": 36, "hw": 40, "move": "Short"}], r["violacoes"])

    def test_E7_hw_e_o_maior_nao_o_ultimo(self):
        subida = [(c, "Short") for c in range(5, 41, 5)]
        r = self._recuo(subida + [(38, "Short"), (36, "Short")])
        self.assertEqual([36], [v["cursor"] for v in r["violacoes"]])
        self.assertEqual([40], [v["hw"] for v in r["violacoes"]])

    def test_E7_recuo_para_37_nao_viola(self):
        subida = [(c, "Short") for c in range(5, 41, 5)]
        self.assertEqual("PASSA", self._recuo(subida + [(37, "Short")])["veredito"])

    def test_E7_reposition_redefine_hw(self):
        subida = [(c, "Short") for c in range(5, 41, 5)]
        self.assertEqual("PASSA", self._recuo(subida + [(10, "Reposition"), (12, "Short")])["veredito"])

    def test_E7_longjump_e_recovery_tambem_contam(self):
        for move in ("LongJump", "Recovery"):
            with self.subTest(move=move):
                r = self._recuo([(40, move), (36, move)])
                self.assertEqual(1, len(r["violacoes"]))


class EntradaTest(unittest.TestCase):
    def test_E8_deslocamento_leva_ao_relogio_do_esperado(self):
        par = s.paragrafos("T05")
        pulados = par[1]
        tempos, esperado = _base(sem_tempo=pulados, deslocamento=lambda k: -300 * len(pulados) if k > pulados[-1] else 0)
        pulo_fim = tempos[pulados[-1] + 1][0]
        linhas = s.trajetoria_seguindo(
            tempos, lambda t, e: pulados[0] - 2 if pulo_fim <= t < pulo_fim + 2000 else e - 1, extras=[pulo_fim + 2000]
        )
        eventos = [(5000, "improviso_inicio", ""), (6000, "improviso_fim", ""),
                   (pulo_fim - 100, "pulo_inicio", str(pulados[0])), (pulo_fim, "pulo_fim", "")]
        hip = ["t_ms,session,kind,words,accepted"] + [
            f'{f[1] + 100},1,partial,"{" ".join(t.formas[0])}",1' for t, f in zip(s.tokens("T05"), tempos) if f
        ]
        hip_menos = [hip[0]] + [f"{int(h.split(',')[0]) - 1500}," + h.split(",", 1)[1] for h in hip[1:]]
        _c, original, _o, _e = _medir(linhas, esperado, eventos, hipoteses="\n".join(hip) + "\n")
        menos = [(t - 1500, c, m) for t, c, m in linhas]
        eventos_menos = [(t - 1500, e, d) for t, e, d in eventos]
        _c, deslocado, _o, _e = _medir(menos, esperado, eventos_menos, deslocamento=1500,
                                       hipoteses="\n".join(hip_menos) + "\n")
        self.assertGreater(original["latencias"]["latencia_1"]["n"], 0, "Q3: as hipóteses entram")
        self.assertEqual(1500, deslocado["deslocamento_ms"])
        self.assertEqual(_sem_proveniencia(original), _sem_proveniencia(deslocado))
        self.assertEqual(2000, original["criterios"]["recuperacao"]["pulos"][0]["recuperacao_ms"], "Q3")

    def test_plano_A6_deslocamento_positivo_comeca_na_trajetoria(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1)
        codigo, dados, _o, _e = _medir(linhas, esperado, deslocamento=1550)
        self.assertNotEqual(2, codigo)
        self.assertEqual(1600, dados["amostras"]["primeira_ms"])

    def test_E9_varios_arquivos_de_eventos(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1)
        manual = [(10000, "improviso_inicio", ""), (12000, "improviso_fim", "")]
        app = [(20000, "pausa_toque", ""), (21000, "retoma_toque", "")]
        _c, um, _o, _e = _medir(linhas, esperado, manual + app)
        _c, dois, _o, _e = _medir(linhas, esperado, arquivos_eventos=[s.eventos_csv(manual), s.eventos_csv(app)])
        self.assertEqual(_sem_proveniencia(um), _sem_proveniencia(dois))
        self.assertEqual(30, um["amostras"]["excluidas_intervalo"], "Q3: os dois intervalos contam (20 + 10)")

    def test_E9_eventos_fora_de_ordem_sao_ordenados(self):
        tempos, esperado = _base()
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1)
        ordenado = [(10000, "improviso_inicio", ""), (12000, "improviso_fim", "")]
        _c, a, _o, _e = _medir(linhas, esperado, ordenado)
        codigo, b, _o, _e = _medir(linhas, esperado, list(reversed(ordenado)))
        self.assertNotEqual(2, codigo)
        self.assertEqual(_sem_proveniencia(a), _sem_proveniencia(b))

    def test_E10_t_ms_igual_e_aceito(self):
        tempos, esperado = _base()
        linhas = [(0, 0, "Reposition"), (5000, 20, "Reposition"), (5000, 21, "Short")]
        codigo, _d, _o, err = _medir(linhas, esperado)
        self.assertNotEqual(2, codigo, err)

    def test_E10_entrada_invalida_sai_com_2(self):
        tempos, esperado = _base()
        traj = s.trajetoria_csv(s.trajetoria_seguindo(tempos, lambda t, e: e - 1))
        ev = s.eventos_csv([])
        casos = {
            "cabeçalho da trajetória": (traj.replace("t_ms,cursor,move,score", "t,cursor,move,score"), esperado, [ev]),
            "cabeçalho do esperado": (traj, esperado.replace("token,palavra", "indice,palavra"), [ev]),
            "cabeçalho dos eventos": (traj, esperado, ["t_ms,event,detalhe\n"]),
            "move desconhecido": (traj.replace(",Short,", ",Jump,", 1), esperado, [ev]),
            "t_ms decrescente": (traj + "10,5,Short,0.0\n", esperado, [ev]),
            "não começa por Reposition": (traj.replace("0,0,Reposition", "0,0,Short", 1), esperado, [ev]),
            "evento fora da lista": (traj, esperado, [s.eventos_csv([(100, "aplauso", "")])]),
            "toque sem índice": (traj, esperado, [s.eventos_csv([(100, "toque_palavra", "")])]),
            "toque com índice não inteiro": (traj, esperado, [s.eventos_csv([(100, "toque_palavra", "x")])]),
            "improviso sem fim": (traj, esperado, [s.eventos_csv([(100, "improviso_inicio", "")])]),
            "fim sem início": (traj, esperado, [s.eventos_csv([(100, "silencio_fim", "")])]),
            "pulo_inicio sem índice": (traj, esperado, [s.eventos_csv([(100, "pulo_inicio", ""), (200, "pulo_fim", "")])]),
            "cursor além do roteiro": (traj + f"999999,{N},Short,0.0\n", esperado, [ev]),
            "toque além do roteiro": (traj, esperado, [s.eventos_csv([(100, "toque_palavra", str(N))])]),
            "esperado sem tempo": (traj, s.esperado_csv("T01", [None] * N), [ev]),
            "esperado fora de sequência": (traj, esperado.replace("\n1,", "\n2,", 1), [ev]),
        }
        for nome, (t, e, evs) in casos.items():
            with self.subTest(caso=nome):
                codigo, dados, out, err = s.rodar_metricas(metricas.main, t, e, evs)
                self.assertEqual(2, codigo)
                self.assertIsNone(dados)
                self.assertEqual("", out)
                self.assertIn("metricas.py:", err)

    def test_E10_hipoteses_com_cabecalho_errado(self):
        tempos, esperado = _base()
        traj = s.trajetoria_csv(s.trajetoria_seguindo(tempos, lambda t, e: e - 1))
        codigo, _d, out, _e = s.rodar_metricas(metricas.main, traj, esperado, [s.eventos_csv([])],
                                               hipoteses="t_ms,kind,words\n")
        self.assertEqual(2, codigo)
        self.assertEqual("", out)


class LatenciasTest(unittest.TestCase):
    def _hipoteses(self, tempos, atraso_aceita, elegiveis):
        """Uma linha accepted=1 por token: 150 ms depois do fim nos elegíveis, 10 ms nos outros. Devem ser
        ignoradas: accepted=0 (fim + 50), kind=error (fim + 20) e a linha antes do t_inicio (início − 10)."""
        tokens = s.tokens("T01")
        linhas = ["t_ms,session,kind,words,accepted"]
        for t, tempo in zip(tokens, tempos):
            palavras = " ".join(t.formas[0])
            linhas.append(f'{tempo[1] + 50},1,partial,"{palavras}",0')
            linhas.append(f'{tempo[1] + 20},,error,"{palavras}",1')
            if t.indice in elegiveis:
                linhas.append(f'{tempo[0] - 10},1,partial,"{palavras}",1')  # antes do início: não vale
            atraso = atraso_aceita if t.indice in elegiveis else 10
            linhas.append(f'{tempo[1] + atraso},1,partial,"{palavras}",1')
        linhas.sort(key=lambda linha: int(linha.split(",")[0]) if linha[0].isdigit() else -1)
        return "\n".join(linhas) + "\n"

    def _elegiveis(self):
        donos = {}
        for t in s.tokens("T01"):
            for forma in t.formas:
                for p in forma:
                    donos.setdefault(p, set()).add(t.indice)
        return {t.indice for t in s.tokens("T01") if all(donos[p] == {t.indice} for f in t.formas for p in f)}

    def test_E11_fala_cursor_e_latencias_1_e_2(self):
        tempos, esperado = _base()
        fins = {tempo[1] + 400: i for i, tempo in enumerate(tempos)}
        linhas = [(0, 0, "Reposition")] + [(t, i, "Short") for t, i in sorted(fins.items()) if i > 0]
        elegiveis = self._elegiveis()
        self.assertTrue(10 <= len(elegiveis) < N - 50, "Q3: há elegíveis e há excluídos")
        for comum in ("de", "que", "e", "o", "a"):
            self.assertTrue(all(comum not in f for i in elegiveis for f in s.tokens("T01")[i].formas))
        codigo, dados, _o, _e = _medir(linhas, esperado, hipoteses=self._hipoteses(tempos, 150, elegiveis))
        lat = dados["latencias"]
        self.assertEqual({"n": N - 1, "mediana": 400, "p95": 400, "maximo": 400}, lat["fala_cursor"])
        self.assertEqual({"n": len(elegiveis - {0}), "mediana": 150, "p95": 150, "maximo": 150}, lat["latencia_1"])
        self.assertEqual({"n": len(elegiveis - {0}), "mediana": 250, "p95": 250, "maximo": 250}, lat["latencia_2"])
        self.assertEqual(500, lat["meta_provisoria_ms"])
        self.assertEqual(0, codigo)

    def test_E11_tokens_com_fim_em_intervalo_ficam_fora(self):
        tempos, esperado = _base()
        linhas = [(0, 0, "Reposition")] + [(tempo[1] + 400, i, "Short") for i, tempo in enumerate(tempos) if i > 0]
        eventos = [(30000, "improviso_inicio", ""), (33000, "improviso_fim", "")]
        _c, dados, _o, _e = _medir(linhas, esperado, eventos)
        self.assertEqual(N - 1 - 10, dados["latencias"]["fala_cursor"]["n"])  # fins 30050..32950: tokens 100..109

    def test_E11_latencia_acima_da_meta_nao_muda_o_codigo(self):
        tempos, esperado = _base()
        linhas = [(0, 0, "Reposition")] + [(tempo[1] + 600, i, "Short") for i, tempo in enumerate(tempos) if i > 0]
        codigo, dados, _o, _e = _medir(linhas, esperado)
        self.assertEqual(600, dados["latencias"]["fala_cursor"]["p95"])
        self.assertEqual(300 * (N - 1) + 250 + 600 - 50, dados["amostras"]["ultima_ms"], "vai até a última linha")
        self.assertIsNone(dados["latencias"]["latencia_1"])
        self.assertEqual(0, codigo)


class RelatorioTest(unittest.TestCase):
    def test_E12_stdout_traz_criterios_latencias_fim_e_proveniencia(self):
        tempos, esperado = _base()
        parado = s.fim_em(tempos)(10000) - 1  # o cursor fica parado no improviso

        def cursor(t, e):
            return parado if 10000 <= t < 12000 else e - 1

        traj = s.trajetoria_csv(s.trajetoria_seguindo(tempos, cursor, extras=[12000]))
        eventos = s.eventos_csv([(10000, "improviso_inicio", ""), (12000, "improviso_fim", "")])
        codigo, dados, out, _e = s.rodar_metricas(metricas.main, traj, esperado, [eventos], deslocamento=0)
        self.assertEqual(0, codigo)
        a = dados["criterios"]["atraso"]
        for trecho in ("Atraso: **PASSA**", f"{a['ate_3']}/{a['amostras']}", "Salto errado: **PASSA**",
                       "Recuperação de pulo: **NAO_SE_APLICA**", "Recuo: **PASSA**", "Fala -> cursor",
                       "Latência 1: sem --hipoteses", f"cursor final {N - 2}; último token {N - 1}",
                       "deslocamento: 0 ms", "Meta provisória"):
            self.assertIn(trecho, out)
        for nome, conteudo in (("x.trajectory.csv", traj), ("x.expected.csv", esperado), ("x0.events.csv", eventos)):
            self.assertIn(f"{nome} sha256={hashlib.sha256(conteudo.encode('utf-8')).hexdigest()}", out)
        self.assertEqual(["trajetoria", "esperado", "eventos"], [e["papel"] for e in dados["entradas"]])


class DefinicoesTest(unittest.TestCase):
    """esperado(t), cursor(t), amostras e p95 (D12, D18), direto na classe Medicao."""

    def _medicao(self, esperados, linhas):
        trajetoria = [metricas.Linha(t, c, m) for t, c, m in linhas]
        esp = [metricas.Esperado(i, a, b) for i, (a, b) in enumerate(esperados)]
        return metricas.Medicao(trajetoria, esp, [], None, None)

    def test_esperado_inclui_o_fim_exato_e_usa_o_maior_indice(self):
        m = self._medicao([(0, 2000), (500, 1000), (2000, 3000)], [(0, 0, "Reposition")])
        self.assertEqual(-1, m.esperado_em(999))
        self.assertEqual(1, m.esperado_em(1000))
        self.assertEqual(1, m.esperado_em(1999))
        self.assertEqual(1, m.esperado_em(2000))
        self.assertEqual(2, m.esperado_em(3000))

    def test_cursor_e_a_ultima_linha_ate_t_e_stay_e_ignorada(self):
        m = self._medicao([(0, 1000)], [(0, 0, "Reposition"), (500, 3, "Short"), (600, 7, "Stay")])
        self.assertEqual(0, m.cursor_em(499))
        self.assertEqual(3, m.cursor_em(500))
        self.assertEqual(3, m.cursor_em(700))

    def test_amostras_vao_ate_o_maior_fim_do_esperado(self):
        tempos, esperado = _base()
        linhas = [ln for ln in s.trajetoria_seguindo(tempos, lambda t, e: e - 1) if ln[0] <= 30000]
        _c, dados, _o, _e = _medir(linhas, esperado)
        self.assertEqual(64400, dados["amostras"]["ultima_ms"])
        self.assertEqual(0, dados["amostras"]["primeira_ms"])
        self.assertEqual(645, dados["amostras"]["total"])

    def test_percentil95_posto_mais_proximo(self):
        self.assertEqual(19, metricas._percentil95(list(range(1, 21))))
        self.assertEqual(10, metricas._percentil95(list(range(1, 11))))
        self.assertEqual(7, metricas._percentil95([7]))


if __name__ == "__main__":
    unittest.main()
````

- [ ] **Passo 2: ver falhar**

Rodar: `python3 etapa0/ferramentas/testar.py`
Esperado: código 1; `ImportError: Failed to import test module: testes.test_metricas` (`No module named 'metricas'`); `testar.py: 40 testes, 0 falhas, 1 erros, 0 pulados`.

- [ ] **Passo 3: escrever o `metricas.py`**

`etapa0/ferramentas/metricas.py`:

````python
"""metricas.py <trajetoria> <esperado> <eventos> [<eventos>...]: veredito dos critérios de voz da Etapa 0.

TESTES-ETAPA0, "Métricas: como calcular"; spec 0004, D10 a D19; impl_plan 0004, A5 a A9.

Critérios (cada um PASSA ou REPROVA; a recuperação pode ser NAO_SE_APLICA):
- atraso: >= 95% das amostras de leitura normal com esperado - cursor <= 3;
- salto errado: zero (cursor > esperado + 2 fora das janelas de toque; movimento automático em improviso,
  silêncio ou pausa);
- recuperação: <= 3000 ms do pulo_fim até a primeira amostra com o esperado já depois do pulo e atraso <= 3;
- recuo: nenhum movimento automático com cursor < hw - 3.
As latências são informativas e não mudam o código de saída.

Códigos de saída: 0 todos passam; 1 algum reprova; 2 entrada inválida.
"""

import argparse
import bisect
import csv
import hashlib
import io
import json
import math
import statistics
import sys
from dataclasses import dataclass
from pathlib import Path

import roteiro

PASSO_MS = 100  # TESTES-ETAPA0, Métricas: "amostrando a cada 100 ms"
ATRASO_MAX = 3  # TESTES-ETAPA0, Atraso: "≤ 3"
FRACAO_MIN_PCT = 95  # TESTES-ETAPA0, Atraso: "em 95% das amostras"
AVANCO_MAX = 2  # TESTES-ETAPA0, Salto errado: "mais de 2 tokens à frente"
POS_PULO_MS = 3000  # TESTES-ETAPA0, Atraso: "dos 3 s após um pulo"
RECUPERACAO_MAX_MS = 3000  # TESTES-ETAPA0, Recuperação de pulo: "≤ 3 s"
RECUO_MAX = 3  # TESTES-ETAPA0, Recuo: "highWater − 3"
META_LATENCIA_MS = 500  # TRD, Metas de desempenho: total < 500 ms (PROVISÓRIA), só informativa

CAB_TRAJETORIA = ["t_ms", "cursor", "move", "score"]
CAB_ESPERADO = ["token", "palavra", "t_inicio_ms", "t_fim_ms"]
CAB_EVENTOS = ["t_ms", "evento", "detalhe"]
CAB_HIPOTESES = ["t_ms", "session", "kind", "words", "accepted"]
MOVES = ("Stay", "Short", "LongJump", "Recovery", "Reposition")
AUTOMATICOS = ("Short", "LongJump", "Recovery")
PARES = {  # início -> fim dos intervalos (TESTES-ETAPA0, Folha de eventos)
    "improviso_inicio": "improviso_fim",
    "silencio_inicio": "silencio_fim",
    "pausa_toque": "retoma_toque",
    "pulo_inicio": "pulo_fim",
}
EVENTOS = tuple(PARES) + tuple(PARES.values()) + ("toque_palavra",)
SEM_MOVIMENTO = ("improviso", "silencio", "pausa")  # TESTES-ETAPA0, Salto errado (b)


class ErroEntrada(Exception):
    """Código de saída 2."""


@dataclass(frozen=True)
class Linha:
    t: int
    cursor: int
    move: str


@dataclass(frozen=True)
class Esperado:
    token: int
    inicio: int  # None se sem tempo
    fim: int


@dataclass(frozen=True)
class Evento:
    t: int
    nome: str
    detalhe: str


@dataclass(frozen=True)
class Hipotese:
    t: int
    kind: str
    palavras: tuple
    aceita: bool


# ------------------------------------------------------------------ leitura e validação


def _linhas_csv(conteudo, cabecalho, nome):
    linhas = list(csv.reader(io.StringIO(conteudo, newline="")))
    if not linhas or linhas[0] != cabecalho:
        raise ErroEntrada(f"{nome}: cabeçalho diferente de {','.join(cabecalho)}")
    for n, campos in enumerate(linhas[1:], start=1):
        if len(campos) != len(cabecalho):
            raise ErroEntrada(f"{nome}: linha {n} com {len(campos)} campos")
    return linhas[1:]


def _inteiro(texto, nome, n):
    try:
        return int(texto)
    except ValueError:
        raise ErroEntrada(f"{nome}: linha {n} com inteiro inválido") from None


def ler_trajetoria(conteudo, deslocamento):
    saida = []
    for n, (t, cursor, move, score) in enumerate(_linhas_csv(conteudo, CAB_TRAJETORIA, "trajetória"), start=1):
        if move not in MOVES:
            raise ErroEntrada(f"trajetória: linha {n} com move fora de {'/'.join(MOVES)}")
        try:
            float(score)
        except ValueError:
            raise ErroEntrada(f"trajetória: linha {n} com score inválido") from None
        linha = Linha(_inteiro(t, "trajetória", n) + deslocamento, _inteiro(cursor, "trajetória", n), move)
        if linha.cursor < 0:
            raise ErroEntrada(f"trajetória: linha {n} com cursor negativo")
        if saida and linha.t < saida[-1].t:
            raise ErroEntrada(f"trajetória: linha {n} com t_ms decrescente")
        saida.append(linha)
    if not saida or saida[0].move != "Reposition":
        raise ErroEntrada("trajetória: precisa começar por uma linha Reposition (formato da 0003)")
    return saida


def ler_esperado(conteudo):
    """Devolve (lista de Esperado, coluna palavra)."""
    saida, palavras = [], []
    for n, (token, palavra, ini, fim) in enumerate(_linhas_csv(conteudo, CAB_ESPERADO, "esperado"), start=1):
        if _inteiro(token, "esperado", n) != n - 1:
            raise ErroEntrada(f"esperado: linha {n} fora da sequência de tokens")
        palavras.append(palavra)
        if ini == "" and fim == "":
            saida.append(Esperado(n - 1, None, None))
            continue
        a, b = _inteiro(ini, "esperado", n), _inteiro(fim, "esperado", n)
        if b < a:
            raise ErroEntrada(f"esperado: linha {n} com fim antes do início")
        saida.append(Esperado(n - 1, a, b))
    if not any(e.fim is not None for e in saida):
        raise ErroEntrada("esperado: nenhum token com tempo")
    return saida, palavras


def ler_eventos(conteudos, deslocamento, n_tokens):
    eventos = []
    for arquivo, conteudo in enumerate(conteudos):
        for n, (t, nome, detalhe) in enumerate(_linhas_csv(conteudo, CAB_EVENTOS, "eventos"), start=1):
            if nome not in EVENTOS:
                raise ErroEntrada(f"eventos: linha {n} com evento fora da lista do TESTES-ETAPA0")
            if nome in ("toque_palavra", "pulo_inicio"):
                indice = _inteiro(detalhe, "eventos", n) if detalhe.strip() else None
                if indice is None or not 0 <= indice < n_tokens:
                    raise ErroEntrada(f"eventos: linha {n} ({nome}) sem índice de token válido")
            eventos.append((_inteiro(t, "eventos", n) + deslocamento, arquivo, n, nome, detalhe))
    eventos.sort(key=lambda e: (e[0], e[1], e[2]))  # D11: juntados e ordenados por t_ms (empate: arquivo, linha)
    return [Evento(t, nome, detalhe) for t, _, _, nome, detalhe in eventos]


def ler_hipoteses(conteudo, deslocamento):
    saida = []
    for n, (t, _sessao, kind, palavras, aceita) in enumerate(_linhas_csv(conteudo, CAB_HIPOTESES, "hipóteses"), start=1):
        if kind not in ("partial", "final", "error") or aceita not in ("0", "1"):
            raise ErroEntrada(f"hipóteses: linha {n} com kind ou accepted inválido")
        saida.append(Hipotese(_inteiro(t, "hipóteses", n) + deslocamento, kind, tuple(roteiro.palavras(palavras)), aceita == "1"))
    return saida


def intervalos(eventos):
    """Pares início/fim por tipo (D11, D12). Par desfeito ou sobreposto do mesmo tipo: ErroEntrada."""
    abertos = {}
    saida = {"improviso": [], "silencio": [], "pausa": [], "pulo": []}
    tipo = {
        "improviso_inicio": "improviso", "improviso_fim": "improviso",
        "silencio_inicio": "silencio", "silencio_fim": "silencio",
        "pausa_toque": "pausa", "retoma_toque": "pausa",
        "pulo_inicio": "pulo", "pulo_fim": "pulo",
    }
    for ev in eventos:
        if ev.nome not in tipo:
            continue
        k = tipo[ev.nome]
        if ev.nome in PARES:
            if k in abertos:
                raise ErroEntrada(f"eventos: {ev.nome} antes de fechar o anterior")
            abertos[k] = ev
        else:
            if k not in abertos:
                raise ErroEntrada(f"eventos: {ev.nome} sem início")
            inicio = abertos.pop(k)
            saida[k].append((inicio.t, ev.t, inicio.detalhe))
    if abertos:
        raise ErroEntrada(f"eventos: {', '.join(sorted(a.nome for a in abertos.values()))} sem fim")
    return saida


# ------------------------------------------------------------------ cálculo


def _dentro(t, faixas):
    return any(a <= t < b for a, b in faixas)


def _percentil95(valores):
    ordenados = sorted(valores)
    return ordenados[math.ceil(0.95 * len(ordenados)) - 1]  # posto mais próximo


def _estatisticas(valores):
    if not valores:
        return {"n": 0, "mediana": None, "p95": None, "maximo": None}
    return {"n": len(valores), "mediana": statistics.median(valores), "p95": _percentil95(valores), "maximo": max(valores)}


class Medicao:
    def __init__(self, trajetoria, esperado, eventos, hipoteses, formas):
        self.trajetoria = trajetoria
        self.esperado = esperado
        self.formas = formas  # tokens do roteiro (formas faladas), para as latências
        self.intervalos = intervalos(eventos)
        self.toques = [ev for ev in eventos if ev.nome == "toque_palavra"]
        self.hipoteses = hipoteses
        com_tempo = sorted((e.fim, e.token) for e in esperado if e.fim is not None)
        self._fins = [f for f, _ in com_tempo]
        self._max_ate = []  # maior índice com t_fim <= fins[k]
        maior = -1
        for _, token in com_tempo:
            maior = max(maior, token)
            self._max_ate.append(maior)
        self._t_traj = [linha.t for linha in trajetoria if linha.move != "Stay"]
        self._c_traj = [linha.cursor for linha in trajetoria if linha.move != "Stay"]

    # esperado(t): maior token com t_fim_ms <= t; -1 antes do primeiro (D12)
    def esperado_em(self, t):
        k = bisect.bisect_right(self._fins, t)
        return self._max_ate[k - 1] if k else -1

    # cursor(t): cursor da última linha com t_ms <= t; Stay ignorada (D12)
    def cursor_em(self, t):
        k = bisect.bisect_right(self._t_traj, t)
        return self._c_traj[k - 1] if k else None

    def janelas_toque(self):
        """D15 e A7: do toque (ou da linha Reposition que o precede) até o t_fim do primeiro token com tempo >= i."""
        janelas = []
        for ev in self.toques:
            i = int(ev.detalhe)
            inicio = ev.t
            anteriores = [linha for linha in self.trajetoria if linha.t <= ev.t]
            if anteriores and anteriores[-1].move == "Reposition" and anteriores[-1].cursor == i:
                inicio = anteriores[-1].t
            fins = [e.fim for e in self.esperado if e.fim is not None and e.token >= i]
            fim = fins[0] if fins else math.inf
            janelas.append((inicio, fim, i))
        return janelas

    def calcular(self):
        janelas = self.janelas_toque()
        faixas_toque = [(a, b) for a, b, _ in janelas]
        sem_mov = [(a, b) for k in SEM_MOVIMENTO for a, b, _ in self.intervalos[k]]
        pos_pulo = [(b, b + POS_PULO_MS) for _, b, _ in self.intervalos["pulo"]]
        excecoes = sem_mov + pos_pulo

        t0 = max(0, self.trajetoria[0].t)
        primeira = -(-t0 // PASSO_MS) * PASSO_MS  # A6: primeiro múltiplo de 100 >= max(0, início da trajetória)
        ultima = max(max(self._fins), self.trajetoria[-1].t)
        amostras = list(range(primeira, ultima + 1, PASSO_MS))

        normais, excl_toque, excl_intervalo = [], 0, 0
        for t in amostras:
            if _dentro(t, faixas_toque):
                excl_toque += 1
            elif _dentro(t, excecoes):
                excl_intervalo += 1
            else:
                normais.append(t)

        atraso = self._atraso(normais)
        salto = self._salto(amostras, faixas_toque)
        recuperacao = self._recuperacao(amostras)
        recuo = self._recuo()
        criterios = {"atraso": atraso, "salto_errado": salto, "recuperacao": recuperacao, "recuo": recuo}
        reprova = any(c["veredito"] == "REPROVA" for c in criterios.values())
        ultima_linha = self.trajetoria[-1]
        return {
            "veredito": "REPROVA" if reprova else "PASSA",
            "criterios": criterios,
            "amostras": {
                "passo_ms": PASSO_MS,
                "primeira_ms": primeira,
                "ultima_ms": amostras[-1] if amostras else None,
                "total": len(amostras),
                "leitura_normal": len(normais),
                "excluidas_toque": excl_toque,
                "excluidas_intervalo": excl_intervalo,
            },
            "janelas_toque": [
                {"indice": i, "inicio_ms": a, "fim_ms": None if b == math.inf else b,
                 "amostras_excluidas": sum(1 for t in amostras if a <= t < b)}
                for a, b, i in janelas
            ],
            "latencias": self._latencias(faixas_toque, excecoes),
            "cursor_final": {"cursor": ultima_linha.cursor, "ultimo_token": len(self.esperado) - 1},
        }

    def _atraso(self, normais):
        atrasos = [self.esperado_em(t) - self.cursor_em(t) for t in normais]
        ate3 = sum(1 for a in atrasos if a <= ATRASO_MAX)
        passa = bool(atrasos) and ate3 * 100 >= FRACAO_MIN_PCT * len(atrasos)  # inteiros: sem erro de float
        return {
            "veredito": "PASSA" if passa else "REPROVA",
            "amostras": len(atrasos),
            "ate_3": ate3,
            "fracao": (ate3 / len(atrasos)) if atrasos else None,
            "maior_atraso": max(atrasos) if atrasos else None,
            "motivo": None if atrasos else "sem amostras de leitura normal (Q3)",
        }

    def _salto(self, amostras, faixas_toque):
        episodios, atual = [], None
        for t in amostras:
            avanco = None
            if not _dentro(t, faixas_toque):
                avanco = self.cursor_em(t) - self.esperado_em(t)
            if avanco is not None and avanco > AVANCO_MAX:
                if atual is None:
                    atual = {"inicio_ms": t, "fim_ms": t, "amostras": 0, "maior_avanco": avanco}
                    episodios.append(atual)
                atual["fim_ms"] = t
                atual["amostras"] += 1
                atual["maior_avanco"] = max(atual["maior_avanco"], avanco)
            else:
                atual = None
        movimentos = []
        for linha in self.trajetoria:
            if linha.move not in AUTOMATICOS:
                continue
            for k in SEM_MOVIMENTO:
                for a, b, _ in self.intervalos[k]:
                    if a <= linha.t < b:
                        congelado = self.esperado_em(a)
                        movimentos.append({
                            "t_ms": linha.t, "cursor": linha.cursor, "move": linha.move, "intervalo": k,
                            "esperado_no_inicio": congelado,
                            "classe": "alcance" if linha.cursor <= congelado else "além do esperado",
                        })
        total = len(episodios) + len(movimentos)
        return {
            "veredito": "PASSA" if total == 0 else "REPROVA",
            "total": total,
            "por_posicao": episodios,
            "por_movimento": movimentos,
        }

    def _recuperacao(self, amostras):
        pulos = []
        for inicio, fim, detalhe in self.intervalos["pulo"]:
            depois = [e for e in self.esperado if e.fim is not None and e.token >= int(detalhe)]
            alvo = depois[0].token if depois else None
            recuperou = None
            if alvo is not None:
                for t in amostras:
                    if t < fim:
                        continue
                    esp = self.esperado_em(t)
                    if esp >= alvo and esp - self.cursor_em(t) <= ATRASO_MAX:
                        recuperou = t - fim
                        break
            pulos.append({"pulo_inicio_ms": inicio, "primeiro_pulado": int(detalhe), "pulo_fim_ms": fim,
                          "alvo": alvo, "recuperacao_ms": recuperou})
        if not pulos:
            veredito = "NAO_SE_APLICA"
        elif all(p["recuperacao_ms"] is not None and p["recuperacao_ms"] <= RECUPERACAO_MAX_MS for p in pulos):
            veredito = "PASSA"
        else:
            veredito = "REPROVA"
        return {"veredito": veredito, "pulos": pulos}

    def _recuo(self):
        violacoes, hw = [], None
        for linha in self.trajetoria:
            if linha.move == "Reposition":
                hw = linha.cursor
            elif linha.move in AUTOMATICOS:
                if linha.cursor < hw - RECUO_MAX:
                    violacoes.append({"t_ms": linha.t, "cursor": linha.cursor, "hw": hw, "move": linha.move})
                hw = max(hw, linha.cursor)
        return {"veredito": "PASSA" if not violacoes else "REPROVA", "violacoes": violacoes}

    def _primeiro_alcance(self, i):
        """Instante da primeira linha com cursor >= i, se essa linha for movimento automático (D18)."""
        for linha in self.trajetoria:
            if linha.move != "Stay" and linha.cursor >= i:
                return linha.t if linha.move in AUTOMATICOS else None
        return None

    def _latencias(self, faixas_toque, excecoes):
        donos = {}
        for t in self.formas or []:
            for forma in t.formas:
                for p in forma:
                    donos.setdefault(p, set()).add(t.indice)
        fala_cursor, lat1, lat2 = [], [], []
        aceitas = sorted((h for h in self.hipoteses or [] if h.aceita and h.kind in ("partial", "final")),
                         key=lambda h: h.t)
        tempos_aceitas = [h.t for h in aceitas]
        for e in self.esperado:
            if e.fim is None or _dentro(e.fim, faixas_toque) or _dentro(e.fim, excecoes):
                continue
            alcance = self._primeiro_alcance(e.token)
            if alcance is None:
                continue
            fala_cursor.append(alcance - e.fim)
            if self.formas is None:
                continue
            token = self.formas[e.token]
            elegivel = all(donos[p] == {e.token} for forma in token.formas for p in forma)
            if not elegivel or not aceitas:
                continue
            for h in aceitas[bisect.bisect_left(tempos_aceitas, e.inicio):]:
                if any(_contem(h.palavras, forma) for forma in token.formas):
                    lat1.append(h.t - e.fim)
                    lat2.append(alcance - h.t)
                    break
        com_hipoteses = self.hipoteses is not None
        return {
            "meta_provisoria_ms": META_LATENCIA_MS,
            "fala_cursor": _estatisticas(fala_cursor),
            "latencia_1": _estatisticas(lat1) if com_hipoteses else None,
            "latencia_2": _estatisticas(lat2) if com_hipoteses else None,
        }


def _contem(palavras, forma):
    n = len(forma)
    return any(tuple(palavras[k:k + n]) == tuple(forma) for k in range(len(palavras) - n + 1))


# ------------------------------------------------------------------ relatório


def relatorio(resultado, entradas, deslocamento):
    c = resultado["criterios"]
    a, s, r, q = c["atraso"], c["salto_errado"], c["recuperacao"], c["recuo"]
    lat = resultado["latencias"]
    linhas = ["# Métricas da Etapa 0", "", f"Veredito: **{resultado['veredito']}**", "", "## Critérios", ""]
    fracao = "-" if a["fracao"] is None else f"{a['fracao'] * 100:.2f}%"
    linhas.append(f"- Atraso: **{a['veredito']}**, {a['ate_3']}/{a['amostras']} amostras de leitura normal "
                  f"com atraso <= {ATRASO_MAX} ({fracao}; mínimo {FRACAO_MIN_PCT}%); maior atraso {a['maior_atraso']}")
    if a["motivo"]:
        linhas.append(f"  - {a['motivo']}")
    linhas.append(f"- Salto errado: **{s['veredito']}**, {s['total']} ocorrência(s)")
    for ep in s["por_posicao"]:
        linhas.append(f"  - posição: {ep['inicio_ms']}-{ep['fim_ms']} ms, {ep['amostras']} amostra(s), "
                      f"até {ep['maior_avanco']} à frente")
    for mv in s["por_movimento"]:
        linhas.append(f"  - movimento: {mv['move']} em {mv['t_ms']} ms para {mv['cursor']} durante {mv['intervalo']} "
                      f"(esperado no início {mv['esperado_no_inicio']}; {mv['classe']})")
    linhas.append(f"- Recuperação de pulo: **{r['veredito']}**")
    for p in r["pulos"]:
        rec = "não recuperou" if p["recuperacao_ms"] is None else f"{p['recuperacao_ms']} ms"
        linhas.append(f"  - pulo_fim em {p['pulo_fim_ms']} ms (alvo {p['alvo']}): {rec}; máximo {RECUPERACAO_MAX_MS} ms")
    linhas.append(f"- Recuo: **{q['veredito']}**, {len(q['violacoes'])} violação(ões)")
    for v in q["violacoes"]:
        linhas.append(f"  - {v['move']} em {v['t_ms']} ms para {v['cursor']} com hw {v['hw']}")
    am = resultado["amostras"]
    linhas += ["", "## Amostras", "",
               f"- {am['total']} amostras a cada {am['passo_ms']} ms, de {am['primeira_ms']} a {am['ultima_ms']} ms",
               f"- leitura normal: {am['leitura_normal']}; excluídas por intervalo: {am['excluidas_intervalo']}; "
               f"excluídas por toque: {am['excluidas_toque']}"]
    for j in resultado["janelas_toque"]:
        linhas.append(f"- janela de toque (token {j['indice']}): {j['inicio_ms']}-{j['fim_ms']} ms, "
                      f"{j['amostras_excluidas']} amostra(s) excluída(s)")
    linhas += ["", "## Latências (informativas, sem veredito)", ""]
    for nome, chave in (("Fala -> cursor", "fala_cursor"), ("Latência 1", "latencia_1"), ("Latência 2", "latencia_2")):
        e = lat[chave]
        if e is None:
            linhas.append(f"- {nome}: sem --hipoteses")
        else:
            linhas.append(f"- {nome}: n={e['n']}, mediana={e['mediana']}, p95={e['p95']}, máximo={e['maximo']} ms")
    linhas.append(f"- Meta provisória do TRD (total): {lat['meta_provisoria_ms']} ms")
    cf = resultado["cursor_final"]
    linhas += ["", "## Fim", "", f"- cursor final {cf['cursor']}; último token {cf['ultimo_token']}",
               "", "## Proveniência", "", f"- deslocamento: {deslocamento} ms"]
    for e in entradas:
        linhas.append(f"- {e['papel']}: {e['arquivo']} sha256={e['sha256']}")
    return "\n".join(linhas) + "\n"


# ------------------------------------------------------------------ linha de comando


def _ler(caminho):
    with open(caminho, "rb") as f:
        dados = f.read()
    return dados.decode("utf-8"), {"arquivo": Path(caminho).name, "sha256": hashlib.sha256(dados).hexdigest()}


def executar(args):
    entradas = []
    textos = {}
    for papel, caminho in [("trajetoria", args.trajetoria), ("esperado", args.esperado)] + \
            [("eventos", e) for e in args.eventos] + ([("hipoteses", args.hipoteses)] if args.hipoteses else []):
        texto, info = _ler(caminho)
        textos.setdefault(papel, []).append(texto)
        entradas.append({"papel": papel, **info})
    esperado, palavras = ler_esperado(textos["esperado"][0])
    trajetoria = ler_trajetoria(textos["trajetoria"][0], args.deslocamento_ms)
    if any(linha.cursor >= len(esperado) for linha in trajetoria):
        raise ErroEntrada("trajetória: cursor além do último token do esperado")
    eventos = ler_eventos(textos["eventos"], args.deslocamento_ms, len(esperado))
    hipoteses = ler_hipoteses(textos["hipoteses"][0], args.deslocamento_ms) if args.hipoteses else None
    formas = roteiro.tokens_por_palavras(palavras)  # A9: o roteiro sai da coluna palavra do esperado
    if args.hipoteses and formas is None:
        raise ErroEntrada("esperado: a coluna palavra não corresponde a nenhum texto de etapa0/textos/")
    resultado = Medicao(trajetoria, esperado, eventos, hipoteses, formas).calcular()
    resultado["deslocamento_ms"] = args.deslocamento_ms
    resultado["entradas"] = entradas
    return resultado, relatorio(resultado, entradas, args.deslocamento_ms)


def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="metricas.py",
        description="Compara a trajetória do app com a esperada e dá o veredito dos critérios de voz.",
    )
    parser.add_argument("trajetoria", help="<nome>.trajectory.csv (t_ms,cursor,move,score)")
    parser.add_argument("esperado", help="<nome>.expected.csv (token,palavra,t_inicio_ms,t_fim_ms)")
    parser.add_argument("eventos", nargs="+", help="um ou mais <nome>.events.csv (t_ms,evento,detalhe)")
    parser.add_argument("--deslocamento-ms", type=int, default=0,
                        help="somado aos t_ms da trajetória, dos eventos e das hipóteses (padrão 0)")
    parser.add_argument("--hipoteses", help="<nome>.hypotheses.csv, para as latências 1 e 2")
    parser.add_argument("--json", help="grava os mesmos números em JSON neste arquivo")
    args = parser.parse_args(argv)
    try:
        resultado, texto = executar(args)
    except (OSError, UnicodeDecodeError, ErroEntrada) as erro:
        print(f"metricas.py: {erro}", file=sys.stderr)
        return 2
    sys.stdout.write(texto)
    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(resultado, f, ensure_ascii=False, indent=2, sort_keys=True)
            f.write("\n")
    return 0 if resultado["veredito"] == "PASSA" else 1


if __name__ == "__main__":
    sys.exit(main())
````

- [ ] **Passo 4: ver passar**

Rodar: `python3 etapa0/ferramentas/testar.py`
Esperado: código 0; `testar.py: 84 testes, 0 falhas, 0 erros, 0 pulados`.

- [ ] **Passo 5: commit**

```bash
git add etapa0/ferramentas/metricas.py etapa0/ferramentas/testes/test_metricas.py
git commit -m "feat(0004): metricas.py com os quatro critérios de voz e latências informativas (spec 0004 E1-E12; TESTES-ETAPA0 Métricas)"
```

### Tarefa 6 (motor-voz): linha de comando e ponta a ponta (spec A5, E13)

**Arquivos:**
- Criar: `etapa0/ferramentas/testes/test_cli.py`

- [ ] **Passo 1: escrever os testes por subprocesso**

`etapa0/ferramentas/testes/test_cli.py`:

````python
"""Linha de comando de verdade (subprocesso) e a cadeia ponta a ponta (spec 0004, A5 e E13)."""

import json
import subprocess
import sys
import unittest

import roteiro
from testes import sintetico as s

FERRAMENTAS = roteiro.FERRAMENTAS


def _cli(ferramenta, *argv):
    return subprocess.run(
        [sys.executable, str(FERRAMENTAS / ferramenta), *argv],
        capture_output=True, text=True, encoding="utf-8", timeout=120, check=False,
    )


class AjudaTest(unittest.TestCase):
    def test_A5_ajuda_das_tres_ferramentas(self):
        esperado = {
            "timestamps.py": ["wav", "--modelo", "-o"],
            "alinhar.py": ["texto", "transcricao", "-o"],
            "metricas.py": ["trajetoria", "esperado", "eventos", "--deslocamento-ms", "--hipoteses", "--json"],
        }
        for ferramenta, termos in esperado.items():
            with self.subTest(ferramenta=ferramenta):
                r = _cli(ferramenta, "--help")
                self.assertEqual(0, r.returncode)
                for termo in termos:
                    self.assertIn(termo, r.stdout)

    def test_codigos_de_saida_pelo_processo(self):
        r = _cli("timestamps.py", "discurso.wav")
        self.assertEqual((2, ""), (r.returncode, r.stdout))
        with s.Pasta() as p:
            quase_t01 = (roteiro.TEXTOS / "T01.txt").read_bytes() + b"x"
            r = _cli("alinhar.py", p.arquivo("texto.txt", quase_t01), p.arquivo("t.csv", "x\n"))
            self.assertEqual((2, ""), (r.returncode, r.stdout))
            r = _cli("metricas.py", p.arquivo("a.csv", "x\n"), p.arquivo("b.csv", "y\n"), p.arquivo("c.csv", "z\n"))
            self.assertEqual((2, ""), (r.returncode, r.stdout))


class PontaAPontaTest(unittest.TestCase):
    def test_E13_transcricao_alinhar_metricas(self):
        tokens = s.tokens("T01")
        pares = s.palavras_dos_tokens(tokens)
        with s.Pasta() as p:
            transcricao = p.arquivo("T01_sintetico_sala_t1.words.csv", s.transcricao_csv([w for _, w in pares]))
            esperado = str(p.dir / "T01_sintetico_sala_t1.expected.csv")
            r = _cli("alinhar.py", str(roteiro.TEXTOS / "T01.txt"), transcricao, "-o", esperado)
            self.assertEqual(0, r.returncode, r.stderr)
            linhas = s.linhas_csv(open(esperado, encoding="utf-8").read())
            tempos = [(int(a), int(b)) for _t, _p, a, b in linhas[1:]]
            self.assertEqual(len(tokens), len(tempos))
            trajetoria = s.trajetoria_csv(s.trajetoria_seguindo(tempos, lambda t, e: e - 1))
            argv = [p.arquivo("x.trajectory.csv", trajetoria), esperado, p.arquivo("x.events.csv", s.eventos_csv([]))]
            saida_json = str(p.dir / "r.json")
            r = _cli("metricas.py", *argv, "--json", saida_json)
            self.assertEqual(0, r.returncode, r.stdout + r.stderr)
            dados = json.load(open(saida_json, encoding="utf-8"))
        c = dados["criterios"]
        self.assertEqual(["PASSA", "PASSA", "NAO_SE_APLICA", "PASSA"],
                         [c[k]["veredito"] for k in ("atraso", "salto_errado", "recuperacao", "recuo")])
        self.assertGreater(c["atraso"]["amostras"], 600, "Q3")


if __name__ == "__main__":
    unittest.main()
````

- [ ] **Passo 2: rodar**

Rodar: `python3 etapa0/ferramentas/testar.py`
Esperado: código 0; `testar.py: 87 testes, 0 falhas, 0 erros, 0 pulados`. (Passa de primeira por ser integração; se falhar, o problema está na fiação `sys.exit(main())` ou no `-o`/`--json`, não na lógica.)

- [ ] **Passo 3: commit**

```bash
git add etapa0/ferramentas/testes/test_cli.py
git commit -m "test(0004): ajuda, códigos de saída por processo e cadeia ponta a ponta (spec 0004 A5, E13)"
```

### Tarefa 7 (motor-voz): verificação completa e evidências

- [ ] **Passo 1: gravar as evidências**

```bash
E=work/0004-etapa0-ferramentas/evidencias
mkdir -p $E
python3 --version > $E/python-versao.txt 2>&1
python3 etapa0/ferramentas/testar.py > $E/testar.txt 2>&1; echo "rc=$?" >> $E/testar.txt
{ grep -rhoE "^\s*(import|from) [a-zA-Z_]+" etapa0/ferramentas --include='*.py' | sort | uniq -c; grep -rn "import vosk" etapa0/ferramentas --include='*.py'; } > $E/a2-imports.txt
{ grep -rnE "urllib|http\.|socket|requests|model_name|lang=" etapa0/ferramentas --include='*.py'; echo "rc=$?"; } > $E/a3-rede.txt
grep -rnE "wave\.open\([^)]*['\"]w|\.writeframes|setnchannels" etapa0/ferramentas --include='*.py' > $E/a4-audio.txt
for f in timestamps alinhar metricas; do python3 etapa0/ferramentas/$f.py --help; echo "rc=$?"; done > $E/a5-ajuda.txt 2>&1
{ git diff main --stat -- etapa0/core/src/main etapa0/app docs/TRD.md; echo "---"; git diff main --stat; } > $E/a6-diff.txt
(cd etapa0 && ./gradlew :core:ktlintCheck :core:lint :core:test testDebugUnitTest --console=plain) > $E/gradle.txt 2>&1; echo "rc=$?" >> $E/gradle.txt
```

- [ ] **Passo 2: conferir contra "Sucesso significa" da seção Verificação**

Abrir cada arquivo (Q4) e conferir: `testar.txt` termina com `87 testes, 0 falhas, 0 erros, 0 pulados` e `rc=0`, e traz a linha `D9: L30 sintético`; `a3-rede.txt` só tem `rc=1`; `a4-audio.txt` só cita `testes/test_timestamps.py`; `a5-ajuda.txt` tem três `rc=0`; a primeira parte de `a6-diff.txt` (antes de `---`) está vazia; `gradle.txt` tem `BUILD SUCCESSFUL` e `rc=0`. Qualquer divergência: corrigir e repetir; 3 falhas seguidas no mesmo ponto, escalar ao arquiteto.

- [ ] **Passo 3: preencher o "Registro da construção" e commit**

```bash
git add work/0004-etapa0-ferramentas/evidencias/ work/0004-etapa0-ferramentas/impl_plan.md
git commit -m "docs(0004): evidências da construção das ferramentas da Etapa 0"
```


## Verificação

Da raiz do repositório, com a saída de cada comando em `work/0004-etapa0-ferramentas/evidencias/` (Tarefa 7):

```bash
python3 --version
python3 etapa0/ferramentas/testar.py
grep -rhoE "^\s*(import|from) [a-zA-Z_]+" etapa0/ferramentas --include='*.py' | sort | uniq -c
grep -rn "import vosk" etapa0/ferramentas --include='*.py'
grep -rnE "urllib|http\.|socket|requests|model_name|lang=" etapa0/ferramentas --include='*.py'; echo "rc=$?"
grep -rnE "wave\.open\([^)]*['\"]w|\.writeframes|setnchannels" etapa0/ferramentas --include='*.py'
python3 etapa0/ferramentas/timestamps.py --help; python3 etapa0/ferramentas/alinhar.py --help; python3 etapa0/ferramentas/metricas.py --help
git diff main --stat -- etapa0/core/src/main etapa0/app docs/TRD.md
git diff main --stat
(cd etapa0 && ./gradlew :core:ktlintCheck :core:lint :core:test testDebugUnitTest --console=plain)
```

Sucesso significa:

- `testar.py` termina com código 0 e a última linha diz `87 testes, 0 falhas, 0 erros, 0 pulados` (ou mais testes, nunca menos, e nenhum pulado); a linha `D9: L30 sintético ... em X s` aparece na saída (tempo registrado, sem meta).
- O primeiro grep lista só módulos da stdlib (`argparse`, `array`, `bisect`, `contextlib`, `csv`, `dataclasses`, `hashlib`, `io`, `itertools`, `json`, `math`, `pathlib`, `re`, `statistics`, `subprocess`, `sys`, `tempfile`, `time`, `unicodedata`, `unittest`, `wave`), os módulos próprios (`roteiro`, `alinhar`, `metricas`, `timestamps`, `testes`) e `vosk`; o `import vosk` aparece uma vez, indentado, em `timestamps.py` dentro de `_fabrica_vosk`.
- O grep de rede não devolve linha (`rc=1`).
- O grep de áudio devolve só linhas de `etapa0/ferramentas/testes/test_timestamps.py`.
- As três ajudas saem com código 0 e listam os argumentos e opções do spec.
- `git diff main --stat -- etapa0/core/src/main etapa0/app docs/TRD.md` não mostra nada; o `git diff main --stat` geral só mostra `etapa0/ferramentas/`, `etapa0/core/src/test/` e `work/0004-etapa0-ferramentas/`.
- O Gradle termina com `BUILD SUCCESSFUL`.

## Riscos

- **R1. O JSON real do Vosk Python difere do falso** (campos, `conf` ausente, palavras fora de ordem). O `timestamps.py` lê só `result[].word/start/end/conf`, ordena por tempo e aceita `conf` ausente (coluna vazia). Se a 0005 achar outro formato, é divergência documento × realidade: parar, descrever e ajustar o spec/plano antes do código (Q4). A conferência contra o pacote real é da 0005, com o portão 6.
- **R2. Memória do alinhamento em gravações muito maiores que o L30.** ~190 MB no L30; cresce com linhas × palavras. A guarda de A3 recusa (código 2) antes de estourar o `array("i")`. Se a 0005 precisar de mais, alinhar por trechos entre eventos da folha (decisão de plano, com teste de igualdade contra o DP completo).
- **R3. Desempate de A3 em dados reais.** Com o WER do modelo grande, empates de custo ficam mais raros, mas o resultado continua sendo o ótimo do D6; a revisão manual (passo 3 do TESTES-ETAPA0) corrige o que o alinhamento errar. O stderr lista as faixas para isso.
- **R4. A5/A6 mudam a leitura do TESTES-ETAPA0.** São refinamentos sem número novo; se o humano preferir outra regra, muda uma função (`_recuperacao` ou o cálculo de `primeira`) e os testes correspondentes. Registrados no OPR-034.
- **R5. Tempo do L30 varia entre máquinas** (Q7). O número da evidência vale para a máquina e a execução registradas; não há meta.

## Portão automático do plano (checklist do PROCESS.md, conferida pelo arquiteto antes de entregar)

- [x] Cada critério do spec (A1–A6, B1–B4, C1–C5, D1–D9, E1–E13) tem passo e comando de verificação (tabela "Critérios do spec → teste → comando" e seção Verificação).
- [x] Testes vêm antes do código em todas as tarefas de código (Tarefas 1 a 5: "ver falhar" antes de "escrever"); a Tarefa 6 é integração, com o vermelho provado por quebras.
- [x] Seção de privacidade preenchida (texto, áudio, rede, dependências, persistência, Q6).
- [x] Nenhuma assinatura de contrato do TRD muda (`align/` e `speech/` de `main` intocados; só um teste novo em `core/src/test`).
- [x] Nenhuma permissão nova no manifesto (nenhuma dependência Gradle nova; o manifesto não é tocado).
- [x] Nada cai em portão humano: nenhuma instalação (o `vosk` e o modelo ficam para a 0005, portão 6 lá), nenhuma mudança de contrato, PRD ou regra inegociável. Os textos propostos ao TESTES-ETAPA0 e ao spec (A3, A5, A6) estão no OPR-034 e não bloqueiam.

---

## Registro da construção (preenchido pelo agente de construção)

**Agente:** motor-voz · **Commit(s):** 9e6bdbe (T1), 6db3597 (T2), 30e6f14 (T3), f332959 (T4), 4cd1e5a (T5), 352c405 (T6) e o de evidências (T7)

- Feito: Tarefas 1 a 7 na ordem, código copiado do plano por extrator (sem edição). `ReferenciaFormasTest` e 12 referências `.tsv`; `roteiro.py`, `timestamps.py`, `alinhar.py`, `metricas.py`, `testar.py`, apoio e sintético dos testes. Cada "ver falhar" falhou como previsto (Kotlin: `referência T01.tsv ausente` e `numeros.tsv ausente`; Python: `No module named 'roteiro'`, e depois os módulos de teste com 1 erro nas contagens 1/8/19/40 testes).
- Testes criados: Kotlin 2 (`:core` 118 -> 120, 0 pulados); Python 87 (7 roteiro, 11 timestamps, 21 alinhar, 45 metricas, 3 cli), 0 falhas, 0 erros, 0 pulados. `:app` `testDebugUnitTest`: 228, 0 falhas.
- Desvios do plano e motivo: nenhum de código. Notas: (1) as contagens por tarefa (7, 18, 39, 84, 87) e o 120 bateram com o plano; (2) o `git diff main --stat` geral inclui, além de `etapa0/ferramentas/`, `etapa0/core/src/test/` e `work/0004-...`, os commits anteriores da própria branch (`.claude/agent-memory/` e `registers/opr_register.md`, da fase de plano); `core/src/main`, `app` e `docs/TRD.md` sem diferença.
- Comandos rodados e resultado: `python3 etapa0/ferramentas/testar.py` rc=0, 87 testes, `D9: L30 sintético (3668 tokens, 3684 palavras) em 3.77 s` (Apple M4, Python 3.13.12; 3.73 a 3.94 s nas execuções); `cd etapa0 && ./gradlew :core:ktlintCheck :core:lint :core:test testDebugUnitTest` BUILD SUCCESSFUL; `./gradlew :core:test lint ktlintCheck verifyMergedManifest testDebugUnitTest --rerun-tasks` BUILD SUCCESSFUL (rc=0). Greps A2 a A4 como esperado (só stdlib e `vosk` indentado em `timestamps.py:89`; rede rc=1; áudio só em `test_timestamps.py`); três `--help` rc=0. Evidências em `work/0004-etapa0-ferramentas/evidencias/`. Nada instalado, nenhum OPR novo.

### Rodada 2 (motor-voz, 2026-10-11; code_review.md rodada 1, REPROVADO)

**Commits:** a4d5004 (testes) e o de evidências e registros desta retomada. Nenhum arquivo de código das ferramentas mudou (`git diff 5f33efd..HEAD` nos `.py` fora de `testes/` vazio): só testes, a referência `fronteiras.tsv` e evidências.

- Feito: bloqueantes 1 e 2 e importantes 3 a 6 do code_review cobertos por teste, e os achados menores 7 a 9 (OPR-036/037) também. Testes novos: Python 87 -> 149 (roteiro 12, timestamps 18, alinhar 33, metricas 83, cli 3); Kotlin `:core` 120 -> 122 casos de teste de paridade (`FronteirasDaTokenizacaoTest`, caso novo no `NormalizerTest`), a partir de `referencia/fronteiras.tsv` lida pelos dois lados.
- Quebras: as 40 verdes do revisor refeitas, 38 vermelhas e 2 equivalentes (Y02, Y03); X01, X02, X03 e X10 vermelhas, com variantes vizinhas (`<= 2`, `<= 4`, `ATRASO_MAX +/- 1`, `normais`). KK5 vermelha no JVM. Varredura anti-recorrência de constantes e conjuntos de amostras: 644 trocas, 48 verdes, todas equivalentes por diferencial (`evidencias/equivalencias_rodada2.py`; 43 equivalências, 4 controles que o diferencial enxerga).
- Equivalentes: alinhar.py `_INF`, guarda `:111`, bordas de `ld`/`lm` e `peso` (guarda só muda perto de 2^29 células); roteiro.py `_INT_MAX`, `resto < 100`, `ini_nucleo`/`fim_nucleo` vazios, `v[-1]` com um só `v`; metricas.py `maior = -1/-0/-2`, chave de ordenação `(t, arquivo, arquivo)`, `[-1]` dos arquivos únicos, `indent` do JSON (só formatação).
- Desvios: nenhum.
- Comandos: `python3 etapa0/ferramentas/testar.py`: 149 testes, 0 falhas, 0 erros, 0 pulados. `cd etapa0 && ./gradlew :core:test lint ktlintCheck verifyMergedManifest testDebugUnitTest --rerun-tasks`: BUILD SUCCESSFUL (350 casos, 0 falhas; saída em `evidencias/gradle-rodada2.txt`). Não declaro o código revisado nem aprovado: a rodada 2 de revisão é do `revisor`.
