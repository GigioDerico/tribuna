**Resultado: PASSOU** (com 3 ressalvas MENORES, nenhuma muda o veredito; ver "Achados do QA")

# Testes — 0004-etapa0-ferramentas

**Responsável:** qa-publicacao · **Data:** 2026-10-11 · **Commit testado:** `5f6809f` (branch `work/0004-etapa0-ferramentas`, árvore limpa antes e depois)

## Ambiente

| Item | Valor |
|---|---|
| Aparelho(s) | Nenhum. A tarefa é de ferramentas Python e teste JVM; não toca o app (`connectedDebugAndroidTest` não se aplica). Tudo rodou no MacBook (Darwin 27, Apple Silicon), por comando |
| Ambiente acústico / voz / microfone | Não se aplica. Nenhum áudio real: só WAV de silêncio gerado em diretório temporário |
| Python | `Python 3.13.12` (`evidencias/qa-python-versao.txt`) |
| JDK / Gradle | JDK 21, Gradle 9.7.1 (Kotlin 2.4.21, AGP 9.4.1) |
| Pacote `vosk` | **Não instalado** (`import vosk` → `ModuleNotFoundError`), de propósito: portão humano 6 da 0005 |
| Rede / instalação | Nada instalado, nada baixado |

## Execução

| Suíte / comando | Resultado | Evidência |
|---|---|---|
| `python3 etapa0/ferramentas/testar.py` | rc 0; `149 testes, 0 falhas, 0 erros, 0 pulados`; 5,8 s | `evidencias/qa-testar.txt` |
| `cd etapa0 && ./gradlew :core:test lint ktlintCheck verifyMergedManifest testDebugUnitTest --rerun-tasks` | `BUILD SUCCESSFUL`, rc 0, 76 de 76 tarefas executadas. XMLs: `:core` **122** testes e `:app` **228** testes, 0 falhas, 0 erros, 0 pulados. `verifyMergedManifest` executou; único `uses-permission` no manifesto mesclado: `RECORD_AUDIO` | `evidencias/qa-gradle.txt` |
| `connectedDebugAndroidTest` | Não rodado: sem aparelho, e a tarefa não toca o app | — |
| Ponta a ponta pela CLI, dados sintéticos de `etapa0/textos/` | 21 verificações do `alinhar.py`, 8 do `timestamps.py`, 26 do `metricas.py` (E10 incluído; nenhuma FALHA), todos com o resultado esperado (rc 0/1/2/3) | `qa-alinhar-cli.txt`, `qa-timestamps-cli.txt`, `qa-metricas-cli.txt` |
| Quebras Q2 do Python (20) | 20 vermelhas, árvore restaurada | `qa-q2-python.txt` |
| Quebras de paridade Kotlin ↔ Python (7 no Kotlin, 2 no Python) | 9 vermelhas, restauradas | `qa-q2-paridade.txt`, `qa-q2-python-paridade.txt` |

Scripts (no scratchpad da sessão, não versionados): `qa_e2e_alinhar.py`, `qa_ts.py`, `qa_e2e_metricas.py`, `q2py.py`, `q2py2.py`, `q2kt.py`. Geram só dados a partir de `etapa0/textos/` e escrevem em diretório temporário.

## Critérios de aceite

Contagem por critério em `qa-testar.txt` (testes do `testar.py` cujo nome começa pelo ID do critério, incluindo `test_plano_*`): A5 2, B1 1, B3 3, B4 2, C1 3, C2 1, C3 2, C4 2, C5 2, D1 2, D2 1, D3 1, D4 1, D5 1, D6 1, D7 2, D8 1, D9 2, E1 2, E2 4, E3 3, E4 5, E5 2, E6 5, E7 5, E8 1, E9 2, E10 3, E11 3, E12 1, E13 1. B2 é o JVM (`ReferenciaFormasTest` 2 casos e `FronteirasDaTokenizacaoTest`). Todos os 149 `ok`.

| Critério | Como foi executado | Resultado | Evidência |
|---|---|---|---|
| A1 | `python3 --version` = 3.13.12; `testar.py` rc 0, 149 testes, 0 pulados, ≥ 1 teste por critério de B a E | PASSOU | `qa-python-versao.txt`, `qa-testar.txt` |
| A2 | grep de imports: só stdlib (`argparse, bisect, contextlib, csv, hashlib, io, itertools, json, math, re, statistics, subprocess, sys, tempfile, time, types, unicodedata, unittest, wave, array, dataclasses, pathlib`), módulos próprios (`alinhar, metricas, roteiro, timestamps, testes`) e `vosk` 1×, indentado em `timestamps.py:89` dentro de `_fabrica_vosk` | PASSOU | `qa-greps.txt` |
| A3 | `grep -rnE "urllib\|http\.\|socket\|requests\|model_name\|lang="` devolve **1 linha**: `testes/test_timestamps.py:249`, uma docstring que diz "nunca model_name ou lang". Nenhum uso em código. Grep extra (`ftplib\|smtplib\|urlopen\|download`): só comentário de `timestamps.py:96` ("sem download"). O modelo abre por `vosk.Model(model_path=...)` | PASSOU com ressalva: o spec diz "não devolve nada" e devolve a docstring (achado Q1) | `qa-greps.txt` |
| A4 | grep de escrita de áudio: só `testes/test_timestamps.py:87-91` (WAV de silêncio em `Pasta()` temporária). `open(...'wb')`: só aquela linha. Nenhuma ferramenta (fora de `testes/`) escreve áudio | PASSOU | `qa-greps.txt` |
| A5 | Os três `--help` com rc 0; listam `wav --modelo -o`, `texto transcricao -o` e `trajetoria esperado eventos --deslocamento-ms --hipoteses --json` | PASSOU | `qa-help.txt` |
| A6 | `git diff main --stat -- etapa0/core/src/main etapa0/app docs/ app/ gradle etapa0/gradle` e todos os `*.kts`: vazio. `git diff main --name-only` fora de `work/0004`, `.claude/`, `registers/`, `etapa0/ferramentas/`, `etapa0/core/src/test/`: vazio. `align/`: só 3 testes em `core/src/test` (`FronteirasDaTokenizacaoTest`, `NormalizerTest`, `ReferenciaFormasTest`), nada em `main` | PASSOU | `qa-greps.txt` |
| B1 | Existem `T01..T10.tsv`, `L30.tsv`, `numeros.tsv` e `fronteiras.tsv` em `etapa0/core/src/test/resources/referencia/`; `test_B1_ha_uma_referencia_por_texto_e_uma_de_numeros` ok; a regeneração é manual (plano, "Regenerar as referências") | PASSOU | `qa-testar.txt` |
| B2 | `:core:test` verde (122); `ReferenciaFormasTest` (`formasDeCadaTexto_batemComAReferencia`, `formasDosNumeros_batemComAReferencia`) e `FronteirasDaTokenizacaoTest`. 7 quebras no Kotlin (`NumberWords`, `Tokenizer`, `Normalizer`) deixam o JVM vermelho (2 a 5 falhas cada) | PASSOU | `qa-q2-paridade.txt` |
| B3 | `test_B3_*` ok. Quebras no Python: tirar `quatorze` (64 falhas) e `duas`→`dois` (292) vermelhas. Com referência regenerada do Kotlin quebrado, em cópia isolada (`git archive`), o `test_roteiro*` fica vermelho nas 7 quebras (1 a 292 falhas) | PASSOU | `qa-q2-python-paridade.txt`, `qa-q2-paridade.txt` |
| B4 | `test_B4_idempotente_em_todos_os_textos` e `test_B4_exemplos_iguais_ao_Normalizer_kt` ok (`Três, Versículo!`, `bom-dia`, `escrevê-los`) | PASSOU | `qa-testar.txt` |
| C1 | `test_C1_csv_com_cabecalho_e_palavras_em_ordem` (reconhecedor falso, cabeçalho e linhas `ouvir,1210,1530,1.0`, `bem,1530,1790,0.98`, final em ordem), `test_C1_milissegundos_arredondados` e `test_rodada2_varios_result_acumulam_todas_as_palavras_na_ordem`. Reconhecedor falso só é injetável por teste | PASSOU | `qa-testar.txt` |
| C2 | `test_C2_*` ok (`três` sai como `três`) | PASSOU | `qa-testar.txt` |
| C3 | Teste: todos os quadros chegam ao falso. CLI: WAV 44,1 kHz, estéreo e 8 bits → rc 2, stdout vazio | PASSOU | `qa-timestamps-cli.txt` |
| C4 | CLI: `discurso.wav` e `X99_giorgio_sala_t1.wav` → rc 2; `T02_giorgio_salao_t2.wav` aceito (chegou até `--modelo`/vosk, rc 3) | PASSOU | `qa-timestamps-cli.txt` |
| C5 | CLI real, sem `vosk` instalado: rc 3 com mensagem "o pacote vosk não está instalado"; sem `--modelo`: rc 2; WAV inexistente: rc 2. Nada de traceback | PASSOU | `qa-timestamps-cli.txt` |
| D1 | T01, primeira forma de cada token: cabeçalho certo, 215 linhas = 215 tokens, todos com tempo, 1ª linha `0,ouvir,0,250`; cada token com o tempo das palavras que o geraram (0 divergências) | PASSOU | `qa-alinhar-cli.txt` |
| D2 | `3:16` com "tres dezesseis", "tres e dezesseis" e "capitulo tres versiculo dezesseis": início da 1ª palavra e fim da última nas três. `13:4-7` e `1 Coríntios` idem | PASSOU | `qa-alinhar-cli.txt` |
| D3 | T05 sem o 2º parágrafo: os 44 tokens do parágrafo sem tempo, os demais com tempo | PASSOU | `qa-alinhar-cli.txt` |
| D4 | T04 com 12 palavras do T03 inseridas depois do 1º parágrafo: nenhum token com tempo dentro da faixa 14100–17650 ms; stderr traz `inseridas: palavras 47-58 [14100-17650 ms] ...` | PASSOU | `qa-alinhar-cli.txt` |
| D5 | T08 com "isso tem um custo" repetido: tokens ficam com o tempo da 1ª vez (14100); repetição em `inseridas: palavras 51-54 [15300-16450 ms]` | PASSOU | `qa-alinhar-cli.txt` |
| D6 | T03: refrão nos tokens 15, 48 e 78 com inícios 4500, 14700, 23700 (distintos e crescentes) | PASSOU | `qa-alinhar-cli.txt` |
| D7 | `três capítulo três Versículo dezesseis` casa o token `3:16`; `bom dia` separado casa `bom-dia` (T03, token 41) | PASSOU | `qa-alinhar-cli.txt` |
| D8 | T01 e L30 com +1 caractere, −1 caractere e CRLF: rc 2, stdout vazio, `-o` não criado (6 casos) | PASSOU | `qa-alinhar-cli.txt` |
| D9 | L30 duas vezes: stdout e stderr idênticos byte a byte (sha256 `c6d1fe5488eb763e…`), 3.668 tokens / 3.684 palavras, todos com tempo. **Tempo (sem meta):** 4,04 s e 3,96 s pela CLI (inclui partida do interpretador); 3,8 s medidos pelo teste do construtor; MacBook, Python 3.13.12 | PASSOU | `qa-alinhar-cli.txt` |
| E1 | CLI: atraso 1, 2 e 3 → 645/645 amostras ≤ 3 (100%), PASSA, rc 0 | PASSOU | `qa-metricas-cli.txt` |
| E2 | CLI: atraso 4 o tempo todo → 15/645 (2,33%), REPROVA, rc 1. Limites 5%/6% e exclusão dos intervalos pelos testes `test_E2_*` (4 testes ok) e pela quebra M5 vermelha | PASSOU | `qa-metricas-cli.txt`, `qa-testar.txt`, `qa-q2-python.txt` |
| E3 | CLI: cursor ≥ esperado+3 por ~3 tokens → 1 salto errado, REPROVA, rc 1; cursor em esperado+2 → 0 saltos, rc 0 | PASSOU | `qa-metricas-cli.txt` |
| E4 | CLI: `Short` dentro de `silencio` → 9 ocorrências, REPROVA, rc 1; silêncio sem movimento e alcance depois do fim → PASSA. Improviso e pausa: `test_E4_*` (5 ok) e quebra M16b vermelha | PASSOU | `qa-metricas-cli.txt`, `qa-q2-python.txt` |
| E5 | `test_E5_*` (2 ok) e `test_plano_A7_*`, `test_rodada2_toque_*`, janela excluída no relatório; quebra X51 vermelha (erro de execução) | PASSOU (por teste; sem caso novo pela CLI) | `qa-testar.txt`, `qa-q2-python.txt` |
| E6 | CLI com pulo (tokens 50–99 sem tempo): recuperação em 2900 ms → PASSA rc 0 ("2900 ms; máximo 3000 ms"); em 3100 ms → REPROVA rc 1; nunca → REPROVA rc 1. Sem pulo: `NAO_SE_APLICA` com rc 0 (E13) | PASSOU | `qa-metricas-cli.txt` |
| E7 | CLI: depois do cursor 40, `Short` para 36 → 1 violação, REPROVA rc 1; para 37 → PASSA rc 0. `Reposition` coberto por `test_E7_reposition_redefine_hw` (ok); M12, M13 vermelhas | PASSOU | `qa-metricas-cli.txt`, `qa-q2-python.txt` |
| E8 | CLI: mesma trajetória e eventos com `t_ms` −1500 e `--deslocamento-ms 1500`: `criterios` do JSON iguais, `deslocamento_ms` 1500 vs 0 | PASSOU | `qa-metricas-cli.txt` |
| E9 | CLI: dois arquivos de eventos == um arquivo com as 4 linhas (mesmo `criterios` e mesmo veredito). Nesse caso o veredito era REPROVA nos dois (a trajetória tem movimento na pausa e no silêncio); a equivalência com veredito PASSA é do `test_E9_*` (2 ok) | PASSOU | `qa-metricas-cli.txt`, `qa-testar.txt` |
| E10 | CLI, 9 entradas inválidas (cabeçalho de trajetória e de eventos, `move` fora da lista, `t_ms` decrescente, sem `Reposition` inicial, evento fora da lista, `toque_palavra` sem índice e com índice não inteiro, `improviso_inicio` sem fim) mais arquivo inexistente: todas rc 2, stdout vazio, mensagem clara, sem traceback | PASSOU | `qa-metricas-cli.txt` |
| E11 | CLI: cursor 400 ms depois do `t_fim_ms`, parciais `accepted=1` 150 ms depois e `accepted=0` espúrios: fala → cursor mediana = p95 = máx = 400 ms (n 214); latência 1 = 150 ms e latência 2 = 250 ms (n 98 elegíveis); rc 0. Inelegíveis (comuns e repetidos) pelos `test_E11_*` | PASSOU | `qa-metricas-cli.txt` |
| E12 | Relatório com os 4 critérios, amostras, latências, cursor final contra último token, deslocamento e nome + SHA-256 por arquivo; `--json` com `amostras, criterios, cursor_final, deslocamento_ms, entradas, janelas_toque, latencias, veredito` | PASSOU | `qa-metricas-cli.txt` |
| E13 | CLI: transcrição sintética do T01 → `alinhar.py` → `expected.csv` → trajetória com atraso 1 → `metricas.py`: rc 0; atraso 645/645, salto 0, recuo 0, recuperação `NAO_SE_APLICA` | PASSOU | `qa-metricas-cli.txt` |

## Métricas da Etapa 0 (quando aplicável)

Nenhuma gravação real foi medida: as gravações e o Vosk real são da 0005. Contagens (Q3/Q7) dos casos sintéticos da CLI, com a regra do spec (amostra a cada 100 ms, atraso em tokens, excluídos improviso, silêncio, pausa e 3 s após pulo):

| Caso sintético | Amostras de leitura normal | Atraso ≤ 3 | Saltos errados | Recuperação de pulo | Latência total |
|---|---|---|---|---|---|
| T01, atraso 1 | 645 de 645 | 645/645 (100%) | 0 | não se aplica | fala → cursor 300 ms (n 213) |
| T01, atraso 3 | 645 | 645/645 | 0 | não se aplica | — |
| T01, atraso 4 | 645 | 15/645 (2,33%) → REPROVA | 0 | não se aplica | — |
| T01, `Short` em silêncio | 615 (30 excluídas) | 615/615 | 9 → REPROVA | não se aplica | — |
| T01, pulo recuperado em 2900 ms | 615 (30 excluídas por intervalo) | 615/615 | 0 | 2900 ms → PASSA | — |
| T01, pulo recuperado em 3100 ms | 615 | 614/615 | 0 | 3100 ms → REPROVA | — |
| T01, cursor +400 ms com `--hipoteses` | 649 | 649/649 | 0 | não se aplica | fala → cursor 400 ms; lat. 1 = 150 ms (n 98); lat. 2 = 250 ms (n 98) |

Nenhuma métrica calculada sobre zero amostras. O relatório do `metricas.py` reprova atraso sem amostras (teste do construtor), e eu não gerei caso desse tipo pela CLI.

## Teste falso (Q2)

Todas as quebras foram feitas em cópia local, uma por vez, e desfeitas com `git checkout --`; `git status` limpo fora de `work/` depois de cada bateria e no fim.

**Python (`qa-q2-python.txt`, 20/20 vermelhas):**
- Bloqueante 1 (`metricas.py:368`): `<= ATRASO_MAX` → `<` (2 falhas), `<= 2` (2), `<= 4` (2). Primeiro vermelho: `test_rodada2_atraso_4_em_2900_e_3_em_3100_reprova_com_3100`.
- Bloqueante 2 (`:281`): `_salto(amostras` → `_salto(normais` (4 falhas; `test_rodada2_salto_errado_por_posicao_vale_dentro_dos_3_s_depois_do_pulo`).
- X06 (`:375` `all`→`any`, 3), X13 (`:338` `not in AUTOMATICOS`→`== "Stay"`, 6), X51 (`:254` sem `e.fim is not None`, 1 erro), Z10 (`timestamps.py:63` `[:1]`, 4), Z11 (`:71` `+=`→`=`, 1).
- Das tabelas do plano, 9: M5 (`:310`, 1), M12 (`:387`, 1), M13 (`:389`, 1), M16b (`:52`, 7+1 erro), M8 (`:34`, 4), L5a (`alinhar.py:185`, 7), L9 (`:110`, 2), R3 (`roteiro.py:41`, 12), T10 (`timestamps.py:70`, 6+1 erro).
- Duas minhas, fora das tabelas: `ATRASO_MAX = 4` (11) e `cursor − esp` invertido em `:368` (18).

**Paridade (`qa-q2-paridade.txt`, `qa-q2-python-paridade.txt`):**

| Quebra | JVM | Python, referência regenerada do Kotlin quebrado (cópia isolada) |
|---|---|---|
| `NumberWords.kt:68` `duas`→`dois` | vermelho (3) | vermelho (292) |
| `NumberWords.kt:54` sem `\|\| rest % 100 == 0` | vermelho (2) | vermelho (38) |
| `Tokenizer.kt:159` `e`→`de` | vermelho (3) | vermelho (16) |
| `NumberWords.kt:89` `cem`→`cento` | vermelho (2) | vermelho (25) |
| `NumberWords.kt:61` sem `quatorze` | vermelho (5) | vermelho (64) |
| `Tokenizer.kt:19` `primeira`/`primeiro` trocados (K3) | vermelho (2) | vermelho (3) |
| `Normalizer.kt:13` `[-'’]`→`[-']` (KK5, verde na rodada 1) | vermelho (2) | vermelho (1) |

No lado Python, sem regenerar a referência: `roteiro.py:91` sem `quatorze` (64 falhas) e `roteiro.py:101` `duas`→`dois` (292 falhas) ficam vermelhas.

Depois de tudo: `testar.py` 149/149 verde e `./gradlew :core:test --rerun-tasks` rc 0 de novo (restauração confirmada).

## Achados do QA

Nenhum bloqueia a 0005. Todos MENORES.

| # | Severidade | Onde | Achado | Evidência |
|---|---|---|---|---|
| Q1 | MENOR | spec A3; `testes/test_timestamps.py:249` | O grep de rede do spec devolve uma linha (docstring de teste que *nega* o uso de `model_name`/`lang`). O critério diz "não devolve nada". O código está certo; o revisor já viu a mesma linha. Proposta: reescrever a docstring sem essas palavras, ou ajustar o critério para "só em comentário/docstring". Decisão do orquestrador | `qa-greps.txt` |
| Q2 | MENOR | `timestamps.py` modo real | O modo real (Vosk de verdade, `SetWords`, JSON real) não foi exercitado: o pacote `vosk` não está instalado e a instalação é portão humano 6 da 0005. Só o falso por teste (`model_path`, `SetWords(True)`, `SetLogLevel(-1)`) | `qa-timestamps-cli.txt` (rc 3) |
| Q3 | MENOR (informação) | E9 pela CLI | No meu caso pela CLI o veredito comum era REPROVA (movimento na pausa e no silêncio). A igualdade de `criterios` vale, mas o caso PASSA de E9 só está no teste do construtor | `qa-metricas-cli.txt` |

Itens abertos do revisor (OPR-039 e OPR-040: ramo `ld[j-2]` de `alinhar.py:95` sem teste de trecho repetido no roteiro e lido uma vez, e quatro quebras verdes de latências/validação de nome) não foram reexaminados por mim além do que a bateria Q2 cobre; continuam com o dono do registro.

## Não testado

- Aparelho, ambiente acústico, vozes e microfone: não se aplica a esta tarefa.
- `timestamps.py` com o pacote `vosk` e modelo reais, e a latência dessas chamadas: 0005 (portão humano 6).
- Tempo do L30 em outra máquina: registrado só o deste MacBook; sem meta.
- `connectedDebugAndroidTest`: não toca o app.
- Todas as 643 trocas da varredura anti-recorrência: reexecutei 29 quebras (20 Python + 9 de paridade) e não a varredura inteira; ela é do construtor e foi amostrada pelo revisor.
- Caso E5 (toque em palavra) pela CLI: só pelos testes `test_E5_*`, `test_plano_A7_*` e `test_rodada2_toque_*`.
