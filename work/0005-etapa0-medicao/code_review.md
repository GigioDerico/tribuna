**Veredito: APROVADO**

# Revisão — 0005-etapa0-medicao (ferramentas), rodada 2

**Revisor:** revisor · **Data:** 2026-10-14 · **Base do diff:** a728aae → c23a9f0 (`work/0005-ferramentas`, worktree `Tribuna-0005-ferramentas`)

A rodada 1 (REPROVADO, 2 bloqueantes e 3 importantes) está em `git show a728aae:work/0005-etapa0-medicao/code_review.md`. Este arquivo a substitui.

Escopo: `vizinhas.py` (novo), `veredito.py`, os seis arquivos de teste, as evidências `b2-*`, o Registro da construção (rodada 2) e a linha nova do `opr_register.md`. O bloco `medir.sh metricas` (Q-C1) é do QA e só entra depois do merge. Ele fica para a revisão dos scripts.

Resumo: os dois bloqueantes e os três importantes da rodada 1 foram tratados. Conferi pelo código, pelos testes e por quebras que eu mesmo rodei. As 18 quebras da tabela nova que amostrei ficaram vermelhas. Das 30 quebras minhas, 24 ficaram vermelhas. As 6 verdes são lacunas menores de teste ou casos equivalentes, e nenhuma muda um número com o código de hoje. A diferença entre 74 e 75 é erro de contagem no plano: não falta teste. Há um problema de processo no registro (IDs de OPR repetidos entre os branches, achado 1). Ele não toca o código e se resolve no merge.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `cd etapa0 && ./gradlew --offline verifyMergedManifest`: `verifyDebugMergedManifest: OK .../AndroidManifest.xml -> android.permission.RECORD_AUDIO`, idem `verifyReleaseMergedManifest`, `BUILD SUCCESSFUL in 7s`. O diff não toca Gradle nem manifesto. | OK |
| Texto não persiste nem vai para log | O diff só tem ferramentas de PC. O `vizinhas.py` lê e escreve folhas de eventos (tempo, nome do evento, índice de token), sem texto. O `veredito.py` não lê texto. O stderr cita só nome de arquivo e de evento. | OK |
| Áudio não gravado fora de `etapa0/` | O diff não tem escrita de WAV. As escritas são as duas folhas (`vizinhas.py:70-72`) e o `--json`/stdout do veredito. | OK |
| Sem rede / só stdlib | `grep -n -E "^(import\|from) "`: `vizinhas.py` importa `argparse sys pathlib folha metricas`; `veredito.py` importa `argparse hashlib json os re sys pathlib metricas roteiro`. `grep -n -E "urllib\|socket\|http\|requests\|subprocess\|os\.system\|Popen"` nos dois → `rc=1`. `subprocess` só em `testes/test_cli.py` e `testes/test_cli_0005.py` (rodam as ferramentas pela linha de comando). | OK |
| `align/` sem Android | O diff não toca `etapa0/core`. | OK |
| Contratos do TRD inalterados | `git diff a728aae..c23a9f0 --stat -- etapa0/ferramentas/{metricas,alinhar,timestamps,roteiro,folha,sincronizar,nivel}.py etapa0/core etapa0/app docs` vazio. O `--stat` inteiro: só `vizinhas.py`, `veredito.py`, seis testes, `opr_register.md`, `impl_plan.md` e as duas evidências. | OK |
| `SessionId` / `highWater − 3` / só Compose | Não se aplica (ferramentas de PC) | N.A. |

## Achados da rodada 1: como ficaram

| Rodada 1 | Tratamento | Conferido como |
|---|---|---|
| **B1** Sensibilidade cega para a folha | C1: `vizinhas.py` escreve a folha de d nos relógios das corridas ±200. `veredito.conferir_entradas(..., corrida)` aceita só `<nome>.events.csv` e a folha da corrida (`veredito.py:239`). `conferir_folhas` (`veredito.py:249-267`) exige uma folha em cada corrida e confere que `ler_eventos(folha_k, desl_k) == ler_eventos(folha_d, d)`. Ou seja, confere o invariante (mesmo instante no WAV), não a constante. A comparação entre corridas exclui só a folha (`veredito.py:308-311`). | Os meus dois casos da rodada 1 viraram teste pela cadeia real `folha → vizinhas → metricas → veredito` (`test_cli_0005.py:211-224`): `["PASSA","PASSA","REPROVA"]` com `('improviso', 46)` e `[2900, 2700, 3100]`, os dois `SENSÍVEL AO DESLOCAMENTO`. São os mesmos números que medi na rodada 1. A mesma folha nas três corridas agora sai com código 2 (`test_D3_a_mesma_folha_nas_tres_corridas_sai_com_2`). Z01, Z08 e minhas R2-24, R2-25 e R2-27 (sinal, ±10 ms, folha lida com deslocamento) derrubam esses testes. **Resolvido.** |
| **B2** CS4 PASSOU sem nada para checar | C2: `_sem_leitura` vale para CS2 e CS4 (`veredito.py:179`). Além disso, `parados_depois` (`veredito.py:270-280`) só deixa PASSOU se houver movimento automático depois do fim de cada intervalo exigido "sem movimento". Isso pega a "captura que não reabriu", que a guarda pedida por mim sozinha não pegava. REPROVOU continua REPROVOU. | `Cs4ComCursorTest`: cursor 0/1, leitura 0, T07 parado depois da pausa, movimento em `b` e em `b−1`, Reposition e Stay não contam. Quebra ★ no worktree (abaixo) com 6 vermelhas. **Resolvido.** |
| **I3** Sensível sem marca no Markdown | C3: `veredito.py:338-346` (motivo "sensível ao deslocamento: ...", preservado no PENDENTE) | `test_D3_reprovou_sensivel_aparece_no_markdown` confere a linha da tabela no `out`. **Resolvido**, com uma lacuna menor no PENDENTE de um REPROVOU (achado 4). |
| **I4** Metade da proveniência sem teste | C4: RV15 e RV16 com teste. A rodada guarda `lidos` (caminho relativo e sha256), e a `final` recalcula cada um (`veredito.py:389-400`). Rodada sem `lidos` → `KeyError` → código 2 (`main`, `except (KeyError, TypeError)`). | RV15 e RV16 vermelhas (amostra minha). R2-11, R2-17, R2-18 e R2-30 (minhas) vermelhas. **Resolvido.** OPR-045 do worktree fica coberto. |
| **I5** Dois pontos do `nivel.py` | C6: testes para RN02, RN11 e RN04 | RN11 vermelha (amostra minha). RN02 e RN04 vermelhas na evidência. **Resolvido**, junto com OPR-046. |

## Spec e plano

| Critério | Implementação | Teste |
|---|---|---|
| D3/E1 (com o esclarecimento C1, OPR-048 para o analista) | `vizinhas.py`; `veredito.py:239, 249-267, 308-316` | `test_vizinhas.py` (7); `SensibilidadeDeVerdadeTest`; `test_D3_*` de folha |
| E2/Q3: CS4 com leitura, cursor e motor depois do intervalo | `veredito.py:157-169, 179, 216, 270-280, 331-336` | `Cs4ComCursorTest`, `test_E2_cs4_mostra_leitura_normal_e_fracao_lida` |
| Achado 3: marca no Markdown | `veredito.py:338-346` | `MotivoNoRelatorioTest` |
| Q8: `lidos` e `final` | `veredito.py:291-292, 315-316, 375-377, 389-400` | `FinalProvenienciaTest` |
| C6: lacunas de `nivel`, `folha`, `sincronizar` | só testes | RN02/04/11, RF08/12/12b/13/13b, RS03/03b/05/06/13/15/21/23 na tabela |
| C7: o que não muda | — | `git diff --stat` vazio nos arquivos listados |

Desvios do plano: o Registro diz "nenhum de lógica" e aponta só a contagem 74 contra 75. Não achei outro desvio.

**Contagem 74 contra 75 (Q7): erro de contagem no plano, sem teste faltando.** Evidência: a tabela do passo 1 da Tarefa 10 tem 74 linhas (extraídas por regex: 9 Z, 44 V, 21 R). Os números de V40 a V71 e de Z01 a Z09 não têm buraco, e não há id repetido. Todos os mutantes que C4 e C6 mandam testar (RV15, 16, 19, 26 e 27; RN02, 04 e 11; RF08, 12, 12b, 13 e 13b; RS03, 03b, 05, 06, 13, 15, 21 e 23) estão na tabela. A contagem de testes fecha: 270 + 7 + 37 + 11 = 325. O "75" aparece só no texto do plano (`impl_plan.md`, Tarefa 10 passo 1 e checklist do portão) e na `retomada.md` da árvore principal. Ele vem da prova do arquiteto, que não está no repositório. Não dá para provar que a prova não tinha uma 75ª linha. O que dá para afirmar é que toda decisão de C1 a C6 tem ao menos uma quebra na tabela e que as minhas 30 cobrem os operandos que sobravam (abaixo).

## Testes (Q2 e Q3)

- `PYTHONDONTWRITEBYTECODE=1 python3 -B etapa0/ferramentas/testar.py` (Python 3.13.12), rodado por mim: `Ran 325 tests in 13.411s` / `OK` / `testar.py: 325 testes, 0 falhas, 0 erros, 0 pulados`.
- **Quebra ★ no próprio worktree:** `veredito.py`, `            if parados:` → `            if False:` (a guarda do motor depois do intervalo, C2). Resultado: `testar.py: 325 testes, 6 falhas, 0 erros` (`test_Q3_cada_intervalo_exigido_conta_no_seu_criterio`, `..._movimento_no_fim_exato_... (t=10999)`, `..._reposition_ou_stay_... (Reposition)` e `(Stay)`, `..._sem_nenhum_movimento_automatico_nao_mede`, `..._t07_cursor_parado_depois_da_pausa_nao_mede`). Desfeita com `git checkout -- etapa0/ferramentas/veredito.py`; `git status --short` vazio.
- **Amostra da tabela nova (18 de 74),** rodada por mim em cópias por `git archive HEAD etapa0/ferramentas etapa0/textos etapa0/core/src/test/resources`, com a troca lida da própria tabela: Z01, Z02, Z08, V42b, V50, V53, V57, V58, V59b, V61, V64c, V65b, RV15, RV16, RV19, RN11, RF12b e RS03b. **18 vermelhas**, iguais à `b2-quebras-q2.txt`.
- **30 quebras minhas fora da tabela** (outro operando, relógio vizinho, corrida vizinha, primeiro ou último intervalo, outro papel). **24 vermelhas, 6 verdes:**

| Id | Arquivo | De → Para | Resultado |
|---|---|---|---|
| R2-01 | `veredito.py:333` | `parados_depois(caminhos["d"], ...)` → `caminhos["m200"]` | **verde** (achado 2) |
| R2-02 | `veredito.py:274` | trajetória com `d` → `0` (relógio só da trajetória) | vermelho |
| R2-03 | `veredito.py:276` | eventos com `d` → `0` (relógio só dos eventos) | vermelho |
| R2-04 | `veredito.py:275` | só a folha (sem o `events.csv` do app) na guarda | vermelho |
| R2-05 | `veredito.py:275` | só o `events.csv` do app (sem a folha) na guarda | vermelho |
| R2-06 | `veredito.py:279` | `max` → `min` | vermelho |
| R2-07 | `veredito.py:333` | guarda também em REPROVOU | vermelho |
| R2-08 | `veredito.py:332` | `EXIGIDOS[(c, …)]` → `[("CS4", …)]` | vermelho |
| R2-09 | `veredito.py:332` | → `[("CS2", …)]` | vermelho |
| R2-10 | `veredito.py:253` | `all(not folhas…)` → `any(...)` | vermelho |
| R2-11 | `veredito.py:292` | `lidos` sem os JSON e o `meta.txt` | vermelho |
| R2-12 | `veredito.py:265` | compara com `na_wav["m200"]` em vez de `["d"]` | vermelho |
| R2-13 | `veredito.py:316` | contagem de eventos só da folha | vermelho |
| R2-14 | `veredito.py:340` | marca sem os motivos do `sinc` | vermelho |
| R2-15 | `veredito.py:341` | `{vizinhos[1]}` → `{vizinhos[0]}` | vermelho |
| R2-16 | `veredito.py:179` | guarda de leitura também no CS3 | **verde** (lado seguro, ver abaixo) |
| R2-17 | `veredito.py:398` | `final`: só `is_file()`, sem sha256 | vermelho |
| R2-18 | `veredito.py:377` | `lidos` com sha fixo | vermelho |
| R2-19 | `veredito.py:280` | só o primeiro tipo (`tipos[:1]`) | vermelho |
| R2-20 | `veredito.py:280` | só o primeiro intervalo de cada tipo (`faixas[k][:1]`) | **verde** (achado 2) |
| R2-21 | `veredito.py:280` | só o último intervalo de cada tipo (`faixas[k][-1:]`) | verde (equivalente) |
| R2-22 | `veredito.py:216` | `leitura_normal` do CS4 ← `amostras.total` | vermelho |
| R2-23 | `veredito.py:162` | `cursor == 0` → `cursor <= 1` | vermelho |
| R2-24 | `vizinhas.py:25` | m200 `+200` → `+190` | vermelho |
| R2-25 | `vizinhas.py:25` | p200 `−200` → `−190` | vermelho |
| R2-26 | `vizinhas.py:35` | perde o primeiro evento | vermelho |
| R2-27 | `vizinhas.py:35` | lê a folha com deslocamento 50 | vermelho |
| R2-28 | `veredito.py:308-309` | `comuns` também sem o `events.csv` do app | **verde** (achado 3) |
| R2-29 | `veredito.py:346` | PENDENTE de um REPROVOU perde o motivo | **verde** (achado 4) |
| R2-30 | `veredito.py:396` | `final` confere só a primeira gravação | vermelho |

  Julgamento das verdes:
  - **R2-21 é equivalente.** Como `ultimo` é o maior movimento automático, "parado depois de algum intervalo" é o mesmo que "parado depois do intervalo que termina por último". Os intervalos de cada tipo saem ordenados do `metricas.intervalos`.
  - **R2-16 fica do lado seguro.** O mutante só troca REPROVOU por NÃO MEDIDO no CS3 com o cursor parado, e os dois estados impedem a final de passar. O CS3 não mudou nesta rodada. Não registro.
  - **R2-01, R2-20, R2-28 e R2-29 são lacunas menores de teste.** O código de hoje está certo (achados 2 a 4). No R2-01, a variante perigosa seria usar a folha da p200 (`b − 200`). Ela também é inofensiva: um movimento em `[b−200, b)` cai dentro do intervalo, que tem segundos, e já dá REPROVA no `metricas.py`.
- **Q3:** o desenho da guarda nova é o certo. Ela reprova o "nada para checar" de dois jeitos: sem leitura ou cursor no 0, e motor parado depois do intervalo. Ela não troca REPROVOU por NÃO MEDIDO (R2-07 vermelha). Nas corridas ±200, os vizinhos usam `estado_base` sem a guarda do motor. Conferi que isso não esconde nada: um movimento que passe de `[b, b+200)` para dentro do intervalo na corrida d−200 vira REPROVA no `metricas.py`, e a marca de sensível aparece.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | IMPORTANTE (processo, não bloqueia o código) | `registers/opr_register.md:48-51` (worktree) contra o mesmo arquivo na árvore principal | **IDs de OPR repetidos entre os branches (Q8).** No worktree, OPR-043 a 046 são as lacunas da minha rodada 1 (sincronizar, folha, final, nivel). Em `work/0005-etapa0-medicao` (árvore principal), OPR-043, 044 e 045 são outros problemas: exportações em `files/exportacoes/`, queda do adb no S23 e o `ui.py` rolando nos dois sentidos. Os commits deste diff (`fff44ae` "OPR-043, OPR-044, OPR-046") e o plano citam os IDs do worktree. Os commits da árvore principal (`43dfede`, `4b1ed14`) citam os outros. No merge, o registro dá conflito, e as citações passam a apontar para dois problemas diferentes. | `grep "^\| OPR-04[1-9]"` nos dois arquivos: textos diferentes para 043, 044 e 045. | No merge (orquestrador): renumerar os OPR de um dos lados para IDs livres (o próximo livre nos dois é 049, que eu uso no achado 5), anotar na coluna Resolução das linhas renumeradas o ID antigo e os commits que o citam, e fechar OPR-043 a 046 do worktree citando `fff44ae`/`8e3b8fd`. Os commits não se reescrevem. |
| 2 | MENOR | `veredito.py:280, 333` | A borda exata da guarda C2 só é testada com a pausa do `events.csv` do app (T07), não com intervalo da folha. Também não há teste com dois intervalos do mesmo tipo. Por isso R2-01 (guarda com a folha de outra corrida) e R2-20 (só o primeiro intervalo de cada tipo) ficam verdes. Hoje o código está certo. Com o R2-20, um L30 em que o motor parasse entre o primeiro e o último silêncio teria o CS4 PASSOU no salão. | R2-01 e R2-20 verdes. | Teste de borda `b`/`b−1` com o silêncio da folha (T06), e teste do L30 com dois silêncios e o último movimento entre eles → NÃO MEDIDO. OPR-049 (motor-voz). |
| 3 | MENOR | `veredito.py:308-311` | Nenhum teste tem uma corrida vizinha sem o `events.csv` do app. Com o R2-28 (comuns sem o papel `eventos`), um `m200.json` sem pausa e sem toque passaria. A janela de toque e a pausa sumiriam só na vizinha. | R2-28 verde. | Teste com o `m200.json` sem a entrada `<nome>.events.csv` → código 2. OPR-049. |
| 4 | MENOR | `veredito.py:346` | O motivo do PENDENTE só é testado quando o estado anterior era SENSÍVEL (`test_D7_pendente_guarda_o_motivo`). Um REPROVOU sensível sem revisão pode perder a marca sem nenhum teste falhar. | R2-29 verde. | Teste de REPROVOU sensível com `revisada=False`: o motivo contém "sensível ao deslocamento". OPR-049. |
| 5 | MENOR | `impl_plan.md` (Tarefa 10, passo 1; checklist do portão) | O plano diz 75 quebras novas e a tabela tem 74. O Registro da construção já aponta a diferença sem ajustar o número. É erro de contagem, não teste faltando (ver Spec e plano). | Contagem acima. | O arquiteto corrige o número no plano quando mexer nele de novo. Vai no OPR-049. |

## Não verificado

- O bloco `medir.sh metricas` (Q-C1) e os demais scripts do QA: entram na revisão dos scripts depois do merge, como diz o próprio plano (Q-C7).
- Não reexecutei as 74 quebras novas inteiras nem as 145 antigas. Rodei 18 da tabela nova, 30 minhas e a ★ no worktree. Para o resto, a fonte é a `b2-quebras-q2.txt` do motor-voz.
- Não rodei nada contra gravação real nem no aparelho.
- A prova do arquiteto (de onde vem o "75") não está no repositório.
- O esclarecimento do E1 no spec (OPR-048) é do analista e ainda está aberto. Este APROVADO cobre o código, não esse texto do spec.
