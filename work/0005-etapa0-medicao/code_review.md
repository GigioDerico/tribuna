**Veredito: REPROVADO**

# Revisão — 0005-etapa0-medicao (ferramentas: sincronizar, folha, veredito, nivel)

**Revisor:** revisor · **Data:** 2026-10-12 · **Base do diff:** a4419fb (merge-base com `work/0005-etapa0-medicao`) → ff211d6 (`work/0005-ferramentas`, worktree `Tribuna-0005-ferramentas`)

Escopo: as quatro ferramentas novas, `testes/sinal.py`, os cinco arquivos de teste, as evidências e o Registro da construção. Os scripts bash do QA (`work/0005-etapa0-medicao/scripts/`, na árvore principal) ficam para a revisão seguinte. Eles entram aqui só onde decidem um número junto com estas ferramentas (`medir.sh metricas`, achado 1).

Resumo: o código segue o plano e as 145 quebras da tabela dão o resultado declarado (144 vermelhas, S35 equivalente). Mesmo assim, há dois caminhos para PASSOU indevido no veredito. (1) A marca SENSÍVEL AO DESLOCAMENTO (spec D3) é cega para os intervalos da folha, e o `veredito.py` impede o procedimento certo. (2) O CS4 passa quando o cursor nunca se moveu (Q3).

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `cd etapa0 && ./gradlew --offline verifyMergedManifest`: `verifyDebugMergedManifest: OK ... -> android.permission.RECORD_AUDIO`, idem release, `BUILD SUCCESSFUL`, rc=0. O diff não toca Gradle nem manifesto. | OK |
| Texto não persiste nem vai para log | O diff só tem ferramentas de PC. O `folha.py` só aceita texto idêntico a um de `etapa0/textos/` (`roteiro.identificar_texto`, folha.py:185). O stderr dele só cita palavras desses textos. `veredito`/`nivel`/`sincronizar` não leem texto. | OK |
| Áudio não gravado fora de `etapa0/` | `grep` de `writeframes`/`setnchannels`/`wave.open(..."w...)`: só `testes/sinal.py:35-43`, ruído pseudoaleatório escrito em `TemporaryDirectory` (`test_sincronizar.py:26`, `apoio.Pasta`). Nenhuma ferramenta escreve WAV, só lê (`sincronizar.py:88`, `"rb"`). As escritas são `open(..., "w")` de `--json`/`-o` (texto). | OK |
| Sem rede / só stdlib | `grep` de imports nos 10 arquivos: só stdlib (`argparse csv hashlib io json math re statistics sys wave array pathlib dataclasses`) e módulos locais. `subprocess`/`tempfile` só em `test_cli_0005.py` e `test_sincronizar.py`. Nenhum `socket`/`urllib`/`http`/`requests`. | OK |
| `align/` sem Android | O diff não toca `etapa0/core`. | OK |
| Contratos do TRD inalterados | `git diff --stat work/0005-etapa0-medicao...work/0005-ferramentas -- etapa0/core etapa0/app docs etapa0/ferramentas/{metricas,alinhar,timestamps,roteiro}.py` vazio, e o mesmo contra `main`. Os testes da 0004 também não mudaram (só arquivos novos em `testes/`). | OK |
| `SessionId` preso ao reconhecedor | Não se aplica (ferramentas de PC) | N.A. |
| Cursor nunca abaixo de `highWater − 3` | Não se aplica | N.A. |
| Só Compose | Não se aplica | N.A. |

## Spec e plano

| Critério | Implementação | Teste |
|---|---|---|
| D3: sincronização (correlação 200 ms, palma, deriva por metades, limite 200 ms) e folha, só stdlib, com teste e ≥ 1 quebra de cada | `sincronizar.py`, `folha.py` | `test_sincronizar.py` (32), `test_folha.py` (24). S01–S53 e F01–F29 vermelhas. Lacunas menores no OPR-043 e no OPR-044 |
| D3: "veredito muda dentro de ±200 ms → sensível, não conta como PASSOU" | `veredito.py:247-277` | **Achado 1:** nos intervalos da folha (improviso, silêncio, pulo), a corrida ±200 é idêntica à corrida em `d` por construção |
| D4: deslocamento, palma (app e WAV), deriva | `sincronizar.py:278-291` | `test_json_e_proveniencia`, `PalmaTest`, `DerivaTest`. A janela do pico no WAV não tem teste (RS03, OPR-043) |
| D6: folha por regras do texto, fim de parágrafo no L30, relógio do app | `folha.py:34-43, 77-172` | `test_folha.py`. Regras conferidas contra `etapa0/textos/{T04,T05,T06,L30}.txt` (notas `//` das linhas 3, 3, 7 e 23/41/53) |
| D7: pontos de escuta; nenhum REPROVA sem revisão | `folha.py:138`; `veredito.py:261-280` | `RevisaoEConfiguracaoTest`; V22, V23, V24 e RV10, RV11 vermelhas |
| D9: ligações CS1–CS4, só sala no CS1, agregação por gravação e rodada, NÃO MEDIDO sem amostra | `veredito.py:53-64, 154-175, 289-348` | `RodadaTest`, `FinalTest`. **Achado 2:** CS4 sem mínimo de leitura |
| D10: configuração oficial | `veredito.py:44-51, 260` | `test_D10_configuracao_diferente_nao_mede` (chave a chave) |
| E1: corridas em d, d−200, d+200 | `veredito.py:236-253`; `medir.sh metricas` (plano, linhas 4083-4106) | Achado 1 |
| E2: número, regra, n e estado por gravação e critério | `veredito.py:178-202, 354-371` | No CS4, o n de leitura e o cursor não aparecem (achado 2). Um REPROVOU sensível sai sem marca no relatório em Markdown (achado 3) |
| E3: classes e janela de toque à parte | `veredito.py:184-193, 199-202` | `test_E3_classes_e_janela_de_toque_contadas`. A contagem da regra de recuperação (OPR-034) não está no veredito; fica para o relatório do QA |
| F1, F2: `--sufixo`/`--config` | `veredito.py:229-237, 402-424` | `ReproducaoTest` |
| F4/D12: nível só de parciais, nunca Final | `nivel.py:67-99` | `test_nivel.py`. Duas lacunas na atribuição da causa (achado 5) |
| H1: final CS1–CS4 (S1+S2; S1+S3) | `veredito.py:318-348` | `FinalTest` |
| Q8: sha256 das entradas, mesmos arquivos nas três corridas | `veredito.py:209-226, 251-255` | Metade da conferência não tem teste (achado 4) |

Desvios do plano: o Registro diz "nenhum de lógica". Conferi o código contra os blocos do plano e não achei desvio. Os achados 1 e 2 são do desenho do plano (A5, `medir.sh`), não da cópia.

## Testes (Q2 e Q3)

- `python3 etapa0/ferramentas/testar.py` (Python 3.13.12), rodado por mim: `Ran 270 tests ... OK` / `testar.py: 270 testes, 0 falhas, 0 erros, 0 pulados`.
- **Tabela do plano (145 quebras):** rodei todas, uma a uma, em 6 cópias por `git archive HEAD etapa0/ferramentas etapa0/textos etapa0/core/src/test/resources`, com `PYTHONDONTWRITEBYTECODE=1 python3 -B` e `__pycache__` apagado antes de cada execução. O worktree não foi tocado. Resultado: **144 vermelhas, 1 verde (S35)**, igual a `evidencias/b-quebras-q2.txt`. Sobre a S35 (RMS ÷ 160 em vez de ÷ 3.200): o ganho constante vira soma em dB, e Pearson não vê. A única diferença é o piso de 1/32768, que corta valores diferentes abaixo de 1 LSB de RMS. Isso não ocorre com o ruído de fundo de uma gravação real. Aceito como equivalente.
- **Uma quebra no próprio worktree**, como pede o processo: V17 (`or bool(sinc["motivos"])` removido) → `FAIL: test_D3_deriva_ou_palma_da_sincronizacao_vira_sensivel`, `51 testes, 1 falhas`. Desfeita com `git checkout -- etapa0/ferramentas/veredito.py`; `git status --short` vazio.
- **95 quebras minhas fora da tabela:** cada operando, cada uso local de constante repetida (`TOLERANCIA_MS` na palma e na deriva, `JANELA_LOCAL_MS` em cada uso, `CORRIGIVEIS`/`INTERVALOS` em cada uso) e trocas de variável parecida (início↔fim, `t_app`↔`t_ms`, `m200`↔`p200`, `[0]`↔`[1]`↔`[2]`, `nome`↔`oficial`, voz↔ambiente, `+ s`↔`- s`). Resultado: **69 vermelhas, 26 verdes**. Tabela completa no fim. Julgamento das verdes:
  - **Equivalentes por argumento (9):** RS17 (WAV < 200 ms já cai na faixa padrão ou dá "sem variância"); RS18 e RS19 (a grade fina de ±100 ms cobre o ponto grosso excluído, e o vizinho grosso tem correlação menor que o melhor); RS24 (o `wave` só abre `NONE`); RF02 (nota `//` não tem token, então `antes(ini) = antes(fim)`); RF13 (os textos são fixos e conferidos por identidade, e nenhuma nota fica no fim do texto); RV33 (o ambiente só pode ser `sala`/`salao`, validado em `ler_oficiais`); RV36 (arquivo ausente vira `OSError` → código 2 do mesmo jeito); RN12 (`level` e `aceitas` estão ordenados: `ler_level` recusa t decrescente e `aceitas` é `sorted`).
  - **Inalcançáveis pela ferramenta (3):** RV26 (rodada com `--config` sem `--sufixo` lê o `meta.txt` oficial e sai toda NÃO MEDIDO, então a final não tem como passar); RV27 (todas as rodadas exigidas presentes e o critério NÃO SE APLICA em todas não ocorre, porque o T01 e os textos do CS2–CS4 são obrigatórios em toda rodada); RV19 (o mutante só põe NÃO MEDIDO a mais, ou seja, erra para o lado seguro). Vão para o OPR-045.
  - **Lacunas reais de teste (14):** RV15, RV16 (achado 4); RN02, RN11 (achado 5); RS03, RS05, RS06, RS13, RS15, RS21, RS23 (OPR-043); RF08, RF12 (OPR-044); RN04 (OPR-046).
- **Q3:** o achado 2 é o exemplo literal da Q3 ("nenhum salto errado num teste em que o cursor nunca se moveu").
- **Diferencial com estrutura:** em vez de diferencial aleatório, montei à mão os dois casos que separam o comportamento das corridas ±200 (achado 1): `metricas.py` real, folha igual nas três corridas contra folha fixa no relógio do WAV. Os scripts ficam no scratchpad da sessão, fora do repositório.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | **BLOQUEANTE** | `veredito.py:251-253` (exige os mesmos arquivos, folha incluída, em d, d−200 e d+200); `veredito.py:273-274`; plano `medir.sh metricas` (impl_plan l. 4083-4106: uma folha só para as três corridas); `test_cli_0005.py:87-90` trava esse desenho | **A sensibilidade ao deslocamento (D3) não enxerga improviso, silêncio nem pulo.** A folha é derivada do `words.csv` (relógio do WAV) e gravada no relógio do app com `d` (`t_app = t_wav − d`). O `metricas.py` soma o deslocamento à trajetória **e** aos eventos (`metricas.py:157`, `ler_eventos(..., deslocamento, ...)`). Com a mesma folha em d±200, a folha anda junto com a trajetória, e só o `expected.csv` se desloca. O certo seria a folha ficar parada no relógio do WAV, onde foi medida. Consequências: (a) "movimento automático dentro de improviso/silêncio" (CS2 do T04 e do L30, CS4 do T06 e do L30) dá o mesmo resultado em d, d−200 e d+200, sempre; (b) a recuperação do CS3 fica igual quando o cursor é o gargalo; (c) as exclusões do CS1 (silêncio, improviso, 3 s pós-pulo) andam com a trajetória. Uma gravação cujo veredito muda dentro de ±200 ms sai PASSOU em vez de SENSÍVEL, ao contrário do spec D3. O caso típico é o "alcance" no começo do improviso (OPR-033): a última palavra antes do improviso é reconhecida com atraso parecido com o respiro. A pausa (`events.csv` do app) está certa, porque é medida no relógio do app. | `metricas.py` real, T01 com improviso depois do token 46, respiro de 600 ms, cursor 500 ms depois do fim de cada palavra (relógio do WAV): **mesma folha → `PASSA` em d, d−200 e d+200; folha fixa no WAV → `PASSA`, `PASSA`, `REPROVA` (`('improviso', 46)`)**. CS3, T05 com recuperação de 2.900 ms: **mesma folha → 2.900 / 2.900 / 2.900 ms, PASSA; folha fixa no WAV → 2.900 / 2.700 / 3.100 ms, REPROVA em d+200**. Pelo `veredito.py`, os dois saem PASSOU. | Folha por corrida. O `folha.py` roda com `--deslocamento-ms d−200` e `d+200` e grava `<nome>.m200.folha.events.csv` e `<nome>.p200.folha.events.csv`, que entram nas corridas ±200. O `events.csv` do app é o mesmo nas três. O `veredito.py` exige o mesmo `trajetoria`/`esperado`/`hipoteses`/`events.csv` do app nas três e, na folha, exige a de cada corrida: nome `<nome>[.m200\|.p200].folha.events.csv` e conteúdo igual ao da folha de `d` com os tempos somados de +200/−200 (assim é a mesma folha, só no outro relógio). Testes novos com `metricas.py` real: os dois casos acima viram SENSÍVEL; folha da corrida trocada → código 2. O `test_cli_0005` passa a gerar as três folhas. Isso muda o plano (A5 e `medir.sh`) e esclarece o E1 do spec ("a folha da corrida ±200 é a derivada com o mesmo deslocamento"). Volta ao arquiteto. |
| 2 | **BLOQUEANTE** | `veredito.py:174-175` (`estado_base`, CS4) e `veredito.py:199-202` (`numeros`, CS4) | **CS4 PASSOU sem nada para checar (Q3).** O CS4 só exige o evento (silêncio ou pausa) na folha e "zero movimentos dentro". Não olha leitura normal nem cursor. Um T06 ou T07 em que o motor nunca saiu do token 0 (modelo que não carregou, captura que não reabriu) sai PASSOU, e o CS4 da rodada também. O T06 e o T07 só estão ligados ao CS4, então nenhum outro critério pega o problema. Os números do CS4 também não mostram cursor nem leitura, e o humano não tem como ver isso no relatório. O CS2 tem a guarda (`veredito.py:163-166`); o CS4 não. O spec E2 diz "zero amostras → NÃO MEDIDO". | Com o `Rodada` dos testes, T06 e T07 com `metricas_json(cursor=0, leitura=0, amostras=0)`: `T06_giorgio_sala_t1 PASSOU {'intervalos': {'pausa': 0, 'silencio': 1}, 'movimentos': 0, ...}`, `T07_giorgio_sala_t1 PASSOU`, `CS4 da rodada: PASSOU`. | No CS4, a mesma guarda do CS2: `leitura_normal == 0` ou `cursor_final.cursor == 0` → NÃO MEDIDO, com o motivo. `leitura_normal` e `fracao_lida` entram nos números do CS4. Testes: T06 e T07 com cursor parado → NÃO MEDIDO; borda `cursor == 1` → mede. O T07 diz "retoma no ponto certo", e isso não é medido por critério nenhum (D9). Vai ao analista no OPR-047. |
| 3 | IMPORTANTE | `veredito.py:274-280, 362-367` | Um **REPROVOU sensível não aparece como sensível no relatório em Markdown.** O plano mantém REPROVOU (A5), mas o motivo fica `None` e a tabela mostra "-". O `sensivel: true` só existe no JSON. No PENDENTE, o motivo é sobrescrito e perde os motivos da sincronização e os estados em ±200. Com isso, o humano pode reprovar a arquitetura no portão 3 por um REPROVOU que está dentro da incerteza de sincronização, sem saber. | Leitura do código. `test_D3_reprova_em_d_continua_reprovado_e_marca_sensivel` só confere o JSON. | Pôr no motivo do REPROVOU sensível "sensível ao deslocamento: d−200 X, d+200 Y; <motivos do sinc>" e preservar isso no motivo do PENDENTE. Teste sobre o `out` (Markdown). |
| 4 | IMPORTANTE | `veredito.py:252` e `veredito.py:220` | **Metade da proveniência (Q8) não tem teste.** RV15: sem a comparação `[0] != [1]`, um `m200.json` gerado com outra folha ou trajetória passa, e o teste só altera o `p200`. RV16: sem a checagem do nome de eventos, um `events.csv` de outra gravação passa, desde que o sha256 bata com o disco. O código está certo hoje, mas nada impede que essas conferências quebrem depois. | RV15 e RV16 verdes (270/270). | Um teste que altera o `m200.json` (mesma mensagem "não usaram os mesmos arquivos") e um teste com entrada `eventos` de outra gravação ("eventos de outra gravação"). |
| 5 | IMPORTANTE | `nivel.py:71` e `nivel.py:67` | **A atribuição da causa no `nivel.py` tem dois buracos de teste que mudam o candidato a limiar (F4).** RN02: buscar a hipótese em `mv["t_ms"]` (relógio do WAV) em vez de `t_app` fica verde, porque os testes não têm hipótese entre `t_app` e `t_app + d`. Ao vivo, `d` passa de 2 s, e o erro atribuiria o movimento a uma parcial posterior. RN11: tirar `final` das aceitas fica verde, porque o único teste de Final não tem parcial antes dela. Com o mutante, um movimento causado por Final no silêncio (o caso comum, a Final chega no fim da frase) seria atribuído à parcial anterior e geraria candidato. É exatamente o que o A6 diz que não pode acontecer. | RN02 e RN11 verdes. | Teste com `d = 3000` e uma parcial aceita entre `t_app` e `t_app + d`: a causa é a anterior a `t_app`. Teste com parcial aceita antes de uma Final que causa o movimento: `causa_kind == "final"` e nenhum candidato. |
| 6 | MENOR | `sincronizar.py:89, 164, 216, 219-221, 267` | Lacunas de teste na sincronização: a janela do pico da palma no WAV (RS03, número registrado no D4); o tamanho da janela local da palma (RS05, RS06, RS13); `--faixa-ms` fora da grade de 10 ms (RS15); `centro` com linhas não uniformes (RS21); largura de amostra do WAV (RS23). Nenhuma muda o `d` global. As da palma podem mudar o "confere". | Verdes listadas acima. | OPR-043 (motor-voz). |
| 7 | MENOR | `folha.py:105, 142, 165, 224` | `ka`/`kb` `None` e `a`/`b` `None` sem teste (RF12, RF13). Sem a guarda, sai `TypeError` não capturado, com código 1, que no `folha.py` quer dizer "folha escrita com aviso" (nenhuma folha foi escrita). O `main` não captura `TypeError`. A borda do último token pulado com tempo (RF08) também não tem teste. Hoje é seguro: sem folha, o veredito dá NÃO MEDIDO pelos eventos exigidos. | Verdes listadas acima. | OPR-044 (motor-voz). |
| 8 | MENOR | `veredito.py:308-309, 318-348` | A final não confere de novo o sha256 das entradas das rodadas (o JSON da rodada não guarda hashes, Q8). Três mutantes inalcançáveis ficam sem teste (RV19, RV26, RV27). | Leitura; verdes listadas acima. | OPR-045 (motor-voz). |
| 9 | MENOR | `nivel.py:65` | A estatística de "leitura" sem o improviso não tem teste (RN04). Ela muda a mediana e o p95 da leitura e o `leitura_abaixo_do_candidato`, que vão à tabela do F4. | RN04 verde. | OPR-046 (motor-voz). |

## Não verificado

- Os scripts do QA (`work/0005-etapa0-medicao/scripts/*`). Revisão própria depois, como combinado. Só li o bloco `medir.sh metricas` do plano, porque ele decide o achado 1.
- Nada foi rodado contra gravação real nem no aparelho (adb fora do escopo desta revisão). A prova com fala real do A3 (voz Luciana) é do arquiteto e não foi refeita.
- Não refiz a `:core:test` (o diff não toca Kotlin; `git diff` vazio em `etapa0/core` e `etapa0/app`).

## Tabela das 95 quebras do revisor

Fonte: `PYTHONDONTWRITEBYTECODE=1 python3 -B etapa0/ferramentas/testar.py` numa cópia por quebra, restaurada depois. "vermelho" quer dizer que algum teste falhou.

| Id | Arquivo | De | Para | Resultado |
|---|---|---|---|---|
| RS01 | `sincronizar.py` | `y = [wav.db(b + s) for b in self.bases]` | `y = [wav.db(b - s) for b in self.bases]` | vermelho |
| RS02 | `sincronizar.py` | `base = t_app // SUB_MS + deslocamento // SUB_MS` | `base = t_app // SUB_MS - deslocamento // SUB_MS` | vermelho |
| RS03 | `sincronizar.py` | `fatias = [k for k in range(base - raio - SUBS_BLOCO, base + raio) if 0 <= k < wav.n]` | `fatias = [k for k in range(base - raio, base + raio) if 0 <= k < wav.n]` | verde |
| RS04 | `sincronizar.py` | `t_app, rms_app = max(candidatas, key=lambda linha: linha[1])` | `t_app, rms_app = max(candidatas, key=lambda linha: linha[0])` | vermelho |
| RS05 | `sincronizar.py` | `locais = [... if abs(linha[0] - t_app) <= JANELA_LOCAL_MS]` | `... < JANELA_LOCAL_MS]` | verde |
| RS06 | `sincronizar.py` | palma: `lo, hi = deslocamento - JANELA_LOCAL_MS, deslocamento + JANELA_LOCAL_MS` | `... - 2 * JANELA_LOCAL_MS, ... + 2 * JANELA_LOCAL_MS` | verde |
| RS07 | `sincronizar.py` | `duracao = linhas[-1][0] - linhas[0][0]` | `duracao = linhas[-1][0]` | vermelho |
| RS08 | `sincronizar.py` | `for pedaco in (linhas[:metade], linhas[metade:]):` | `for pedaco in (linhas[:metade], linhas):` | vermelho |
| RS09 | `sincronizar.py` | palma `<= TOLERANCIA_MS` | `<= TOLERANCIA_MS - 10` | vermelho |
| RS10 | `sincronizar.py` | palma `<= TOLERANCIA_MS` | `<= TOLERANCIA_MS + 10` | vermelho |
| RS11 | `sincronizar.py` | deriva `> TOLERANCIA_MS` | `> TOLERANCIA_MS + 10` | vermelho |
| RS12 | `sincronizar.py` | deriva `> TOLERANCIA_MS` | `> TOLERANCIA_MS - 10` | vermelho |
| RS13 | `sincronizar.py` | `locais = [... <= JANELA_LOCAL_MS]` | `... <= JANELA_LOCAL_MS // 2]` | verde |
| RS14 | `sincronizar.py` | faixa padrão `if lo > hi:` | `if lo >= hi:` | vermelho |
| RS15 | `sincronizar.py` | `lo, hi = _teto10(faixa[0]), _piso10(faixa[1])` | `lo, hi = faixa[0], faixa[1]` | verde |
| RS16 | `sincronizar.py` | `--faixa-ms` `if lo > hi:` | `if False:` | vermelho |
| RS17 | `sincronizar.py` | `if wav.n < SUBS_BLOCO:` | `if wav.n < 1:` | verde (equivalente) |
| RS18 | `sincronizar.py` | `range(lo, hi + 1, PASSO_GROSSO_MS)` | `range(lo, hi, PASSO_GROSSO_MS)` | verde (equivalente) |
| RS19 | `sincronizar.py` | `min(hi, d + PASSO_GROSSO_MS) + 1, SUB_MS)` | `min(hi, d + PASSO_GROSSO_MS), SUB_MS)` | verde (equivalente) |
| RS20 | `sincronizar.py` | `"sensivel": bool(motivos),` | `"sensivel": False,` | vermelho |
| RS21 | `sincronizar.py` | `self.centro = math.fsum(t for t, _ in linhas) / len(linhas)` | `self.centro = linhas[0][0]` | verde |
| RS22 | `sincronizar.py` | `linhas_fora_do_wav ... wav.dentro(b + s)` | `wav.dentro(b - s)` | vermelho |
| RS23 | `sincronizar.py` | ` or w.getsampwidth() != 2` | (remove) | verde |
| RS24 | `sincronizar.py` | `w.getcomptype() != "NONE" or ` | (remove) | verde (equivalente) |
| RS25 | `sincronizar.py` | `if resultado_deriva["indeterminada"]:` | `if False:` (com guarda de None no elif) | vermelho |
| RS26 | `sincronizar.py` | `_teto10(BLOCO_MS - t_min), _piso10(duracao_ms - t_max)` | `_teto10(BLOCO_MS - t_max), _piso10(duracao_ms - t_min)` | vermelho |
| RS27 | `sincronizar.py` | `palma(linhas, wav, d, palma_ate_ms)` | `palma(linhas, wav, d, PALMA_ATE_MS)` | vermelho |
| RS28 | `sincronizar.py` | `dbs = sorted(_db(rms) for _, rms in candidatas)` | `... in linhas)` | vermelho |
| RF01 | `folha.py` | `if t.inicio < pos]` (antes) | `if t.inicio <= pos]` | vermelho |
| RF02 | `folha.py` | `a, b = antes(lista[k].ini), depois(lista[k].fim)` | `a, b = antes(lista[k].fim), depois(lista[k].ini)` | verde (equivalente) |
| RF03 | `folha.py` | `lista[k + 1:]` | `lista[k + 2:]` | vermelho |
| RF04 | `folha.py` | `pulado = depois(seguinte.ini)` | `antes(seguinte.ini)` | vermelho |
| RF05 | `folha.py` | `ka = _com_tempo_antes(esperado, a)` | `_com_tempo_depois(esperado, a)` | vermelho |
| RF06 | `folha.py` | improviso `p.fim <= inicio_depois` | `p.inicio <= inicio_depois` | vermelho |
| RF07 | `folha.py` | silêncio `p.inicio > fim_antes and p.fim < inicio_depois` | `p.fim > fim_antes and p.inicio < inicio_depois` | vermelho |
| RF08 | `folha.py` | `range(pulado, b)` | `range(pulado, b - 1)` | verde |
| RF09 | `folha.py` | `range(pulado, b)` | `range(pulado, b + 1)` | vermelho |
| RF10 | `folha.py` | T05 `paragrafo_seguinte` | `na_nota` | vermelho |
| RF11 | `folha.py` | T04 `na_nota` | `fim_do_paragrafo_seguinte` | vermelho |
| RF12 | `folha.py` | `if ka is None or kb is None:` | `if ka is None:` | verde |
| RF13 | `folha.py` | `if a is None or b is None:` | `if a is None:` | verde (equivalente nos textos fixos) |
| RF14 | `folha.py` | `antes(seguinte.fim), depois(seguinte.fim)` | `antes(seguinte.ini), depois(seguinte.ini)` | vermelho |
| RF15 | `folha.py` | `marcar(inicio_depois, "silencio_fim"` | `marcar(esperado[kb].fim, "silencio_fim"` | vermelho |
| RF16 | `folha.py` | `marcar(fim_antes, "pulo_inicio"` | `marcar(esperado[ka].inicio, "pulo_inicio"` | vermelho |
| RF17 | `folha.py` | `inicio_depois = ... esperado[kb].inicio` | `esperado[kb].fim` | vermelho |
| RF18 | `folha.py` | `if texto_id not in REGRAS:` | `if False:` | vermelho |
| RV01 | `veredito.py` | CS2 `salto ... or c["recuo"]` | `and` | vermelho |
| RV02 | `veredito.py` | `numeros` CS4 | movimentos zerados | vermelho |
| RV03 | `veredito.py` | `any(v != estado for v in vizinhos)` | `all(...)` | vermelho |
| RV04 | `veredito.py` | `if estado == PASSOU and sensivel:` | `if sensivel:` | vermelho |
| RV05 | `veredito.py` | `estado_base(c, m[k])` (vizinhos) | `estado_base(c, m["d"])` | vermelho |
| RV06 | `veredito.py` | `if eventos[k] == 0]` | `< 0]` | vermelho |
| RV07 | `veredito.py` | `EXIGIDOS.get((c, texto_id), ())` | `EXIGIDOS.get((texto_id, c), ())` | vermelho |
| RV08 | `veredito.py` | `if diferentes:` | `if False:` | vermelho |
| RV09 | `veredito.py` | `config.items()` | `CONFIG_OFICIAL.items()` | vermelho |
| RV10 | `veredito.py` | `revisada = revisao.is_file() and ...` | `revisada = True or ...` | vermelho |
| RV11 | `veredito.py` | `(PASSOU, REPROVOU, SENSIVEL) and not revisada` | `(REPROVOU, SENSIVEL) ...` | vermelho |
| RV12 | `veredito.py` | `("m200", d - TOLERANCIA_MS)` | `d - TOLERANCIA_MS + 10` | vermelho |
| RV13 | `veredito.py` | `("p200", d + TOLERANCIA_MS)` | `d + TOLERANCIA_MS - 10` | vermelho |
| RV14 | `veredito.py` | `if m[chave]["deslocamento_ms"] != esperado:` | `if False:` | vermelho |
| RV15 | `veredito.py` | `proveniencia[0] != proveniencia[1] or proveniencia[0] != proveniencia[2]` | `proveniencia[0] != proveniencia[2]` | **verde** |
| RV16 | `veredito.py` | `if papel == "eventos" and not arquivo.startswith(nome + "."):` | `if False:` | **verde** |
| RV17 | `veredito.py` | `"wav": oficial or nome` | `"wav": nome` | vermelho |
| RV18 | `veredito.py` | `if not estados: return NAO_SE_APLICA` | `return PASSOU` | vermelho |
| RV19 | `veredito.py` | ausentes `if ligado(c, t, amb)` | (remove a condição) | verde (lado seguro) |
| RV20 | `veredito.py` | final `if e != NAO_SE_APLICA:` | `if True:` | vermelho |
| RV21 | `veredito.py` | final `any(e == REPROVOU ...)` | `all(...)` | vermelho |
| RV22 | `veredito.py` | final `all(e == PASSOU ...)` | `any(...)` | vermelho |
| RV23 | `veredito.py` | final rodada repetida | `if False:` | vermelho |
| RV24 | `veredito.py` | `range(3, 11)` (obrigatórios) | `range(3, 10)` | vermelho |
| RV25 | `veredito.py` | `m.group(1) not in roteiro.IDS` | (remove) | vermelho |
| RV26 | `veredito.py` | `r.get("sufixo") or r.get("config") != CONFIG_OFICIAL` | `r.get("sufixo")` | verde (inalcançável) |
| RV27 | `veredito.py` | final `not motivos and estados and all(` | `not motivos and all(` | verde (inalcançável) |
| RV28 | `veredito.py` | `cursor == 0` (CS2) | `cursor < 0` | vermelho |
| RV29 | `veredito.py` | agregar `all(e == PASSOU)` | `all(e in (PASSOU, SENSIVEL))` | vermelho |
| RV30 | `veredito.py` | agregar `all(e == PASSOU)` | `all(e != NAO_MEDIDO)` | vermelho |
| RV31 | `veredito.py` | `if r == "NAO_SE_APLICA":` | `... and False:` | vermelho |
| RV33 | `veredito.py` | `ambiente == "sala"` | `ambiente != "salao"` | verde (equivalente) |
| RV34 | `veredito.py` | `_NOME.fullmatch(oficial).group(3)` | `.group(2)` | vermelho |
| RV35 | `veredito.py` | `(m.group(2), m.group(3)) != (voz, amb)` | `(m.group(3), m.group(2))` | vermelho |
| RV36 | `veredito.py` | `not caminho.is_file() or _sha256(...)` | `_sha256(...)` | verde (equivalente) |
| RV37 | `veredito.py` | `caminhos.get("eventos", [])` | `[:1]` (só o 1º arquivo de eventos) | vermelho |
| RV38 | `veredito.py` | `for g in gravacoes` (rodada) | `for g in gravacoes[1:]` | vermelho |
| RN01 | `nivel.py` | `t_app = mv["t_ms"] - d` | `+ d` | vermelho |
| RN02 | `nivel.py` | `_ultimo_ate(aceitas, t_app, ...)` | `_ultimo_ate(aceitas, mv["t_ms"], ...)` | **verde** |
| RN03 | `nivel.py` | `_ultimo_ate(level, causa.t, ...)` | `_ultimo_ate(level, t_app, ...)` | vermelho |
| RN04 | `nivel.py` | `fora`: `for k in INTERVALOS` | `for k in CORRIGIVEIS` | verde |
| RN05 | `nivel.py` | `tem_movimento ... in CORRIGIVEIS` | `in INTERVALOS` | vermelho |
| RN06 | `nivel.py` | `corrigiveis ... in CORRIGIVEIS` | `in INTERVALOS` | vermelho |
| RN07 | `nivel.py` | `fora`: `a <= t < b` | `a < t < b` | vermelho |
| RN08 | `nivel.py` | `fora`: `a <= t < b` | `a <= t <= b` | vermelho |
| RN09 | `nivel.py` | `veredito.conferir_entradas(...)` | lista sem conferir sha | vermelho |
| RN10 | `nivel.py` | `ler_eventos(conteudos, 0, ...)` | `ler_eventos(conteudos, relatorio["deslocamento_ms"], ...)` | vermelho |
| RN11 | `nivel.py` | `h.kind in ("partial", "final")` | `h.kind in ("partial",)` | **verde** |
| RN12 | `nivel.py` | `_ultimo_ate` com `break` | sem `break` | verde (equivalente) |
