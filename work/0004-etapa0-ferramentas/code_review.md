**Veredito: APROVADO**

# Revisão (rodada 2): 0004-etapa0-ferramentas

**Revisor:** revisor · **Data:** 2026-10-11 · **Base do diff:** `5f33efd` (rodada 1) → `89111e4` (`work/0004-etapa0-ferramentas`). A base da tarefa continua sendo `0a6c8b1`, o merge-base com `main`.

Resumo: os bloqueantes 1 e 2 e os importantes 3 a 6 da rodada 1 agora têm teste. Reapliquei cada um, uma quebra por vez, mais uma variante vizinha de cada, e todas ficaram vermelhas. Depois reapliquei 26 quebras tiradas das tabelas do construtor (todas vermelhas) e fiz 65 variantes novas minhas: troca de variável vizinha (início↔fim, esperado↔alvo, hipótese↔final, ms↔s), cada operando das guardas do `alinhar.py`, do `timestamps.py` e do `metricas.py`, e cada uso de constante repetida. Dessas 65, 60 ficaram vermelhas e 5 verdes. Nenhuma das verdes muda o veredito da 0005: são latências informativas, validação de nome ou CSV, e um caso equivalente. Das 48 equivalências declaradas pelo construtor, uma não vale (`alinhar.py:95`, ramo `else ld[j - 2]`; tenho o contraexemplo). Mas o código está certo, e o caso fica fora do protocolo. Todos os comandos passaram. O código das ferramentas não mudou. As regras inegociáveis estão intactas. Os achados restantes são MENOR e foram para o OPR-039 e o OPR-040.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `cd etapa0 && ./gradlew :core:test lint ktlintCheck verifyMergedManifest testDebugUnitTest --rerun-tasks --offline`: `BUILD SUCCESSFUL`, rc=0, 76 tarefas executadas. Único `uses-permission` em `app/build/intermediates/merged_manifest/debug`: `android.permission.RECORD_AUDIO`. Nenhum Gradle nem manifesto no diff | OK |
| Texto não persiste nem vai para log | O diff `5f33efd..HEAD` só toca testes, `fronteiras.tsv`, evidências, plano, registro e memória (`git diff --name-only`). Os textos novos dos testes (`d'água`, `bom-dia`, `livro`, `fala`, `mal`, `muita`, os fragmentos de `fronteiras.tsv`) são fronteiras sintéticas ou palavras de `etapa0/textos/`. Não há discurso pessoal | OK |
| Áudio não gravado fora de `etapa0/` | O grep de escrita de áudio só acha `testes/test_timestamps.py:87-91`, que escreve silêncio no diretório temporário de `Pasta()` | OK |
| Rede | O grep de rede só acha `test_timestamps.py:249`, uma docstring que *afirma* que não se usa `model_name`/`lang`. O teste novo `test_rodada2_modo_real_abre_o_modelo_so_por_caminho_e_liga_SetWords` prova, com módulo `vosk` falso, que a chamada é `Model(model_path=...)` e nada além disso. `import vosk` aparece uma única vez, indentado, em `timestamps.py:89` (`_fabrica_vosk`) | OK |
| Imports só da stdlib | Os imports listam só stdlib, módulos próprios e `vosk` (1×) | OK |
| `align/` sem Android; contratos do TRD | `git diff --stat 5f33efd..HEAD -- etapa0/core/src/main etapa0/app app/ docs/ gradle/ '*.kts' etapa0/gradle` vazio. Os `.py` fora de `testes/` também têm diff vazio. O Kotlin novo é só teste (`org.junit`, `java.io.File`) | OK |
| `SessionId`, cursor ≥ `hw − 3` no app, só Compose | Nenhum código do app tocado. O critério de recuo do `metricas.py` está travado (S-v387, N06, N07 vermelhas) | N.A. |

## Comandos que eu rodei

- `python3 --version`: 3.13.12. `python3 etapa0/ferramentas/testar.py`: `149 testes, 0 falhas, 0 erros, 0 pulados`, rc=0.
- Gradle acima: `:core` 122 casos, `:app` `testDebugUnitTest` 228 casos, 0 falhas, 0 erros, 0 pulados. Contei pelos XMLs reescritos, com uns 40 s de idade.
- KK5 (`Normalizer.kt:13`, `[-'’]` → `[-']`): `:core:test` deu `122 tests completed, 2 failed`, rc=1. Restaurei com `git checkout`.
- Árvore limpa no fim (`git status --short` vazio antes de eu escrever este relatório e o registro).

## Spec e plano

| Critério de aceite | Teste novo ou reforçado nesta rodada |
|---|---|
| E6 (recuperação ≤ 3 s com atraso ≤ 3) | `test_rodada2_atraso_exatamente_3_em_2900_passa`, `..._atraso_4_em_2900_e_3_em_3100_reprova_com_3100`, `..._atraso_4_em_2900_nunca_melhor_nunca_recupera`, `..._dois_pulos_valem_todos_nao_qualquer`, `..._limite_de_3000_ms_vale_para_o_ms_exato` |
| Salto errado "em toda a trajetória" | `test_rodada2_salto_errado_por_posicao_vale_dentro_dos_3_s_depois_do_pulo`, `..._salto_por_posicao_vale_dentro_de_intervalos_sem_movimento_automatico` (improviso, silêncio, pausa) |
| Movimento automático = Short/LongJump/Recovery | `test_rodada2_reposition_dentro_de_pausa_improviso_ou_silencio_nao_e_movimento_automatico` |
| D15 (janela do toque) | `test_rodada2_toque_em_token_sem_tempo_vai_ate_o_proximo_token_com_tempo` (fim 42550 calculado à mão) |
| C1 (vários `Result()`) | `test_rodada2_varios_result_acumulam_todas_as_palavras_na_ordem`: 2 `Result()` em 5 blocos, um com 3 palavras, CSV inteiro conferido |
| D5/D21 (modelo só por caminho) | `test_rodada2_modo_real_abre_o_modelo_so_por_caminho_e_liga_SetWords`, `..._modelo_que_nao_abre_sai_com_2` |
| B (paridade Kotlin × Python) | `fronteiras.tsv` lido pelos dois lados (`FronteirasDaTokenizacaoTest`, `FronteirasTest`); apóstrofo tipográfico nos dois |

Desvios do plano: nenhum no código. O plano ganhou a seção "Quebras da rodada 2", que é registro, não mudança de desenho. Uma ressalva de contagem (Q7): o plano diz "644 trocas", mas o `.tsv` tem 643 trocas e 644 linhas, porque conta o cabeçalho (595 vermelhas, entre elas as 6 de laço infinito, e 48 verdes). Está no OPR-039.

## Testes (Q2 e Q3)

Script em scratchpad (`q2r2.py`): aplica uma troca por vez na linha indicada, confere que o trecho aparece uma vez só, roda `testar.py` com limite de 180 s e restaura com `git checkout` no `finally`. Foram 105 quebras: **100 vermelhas e 5 verdes**. A árvore ficou limpa.

- **Bloqueantes e importantes da rodada 1, reaplicados (14):** X01 (`<`), X02 (`<= 2`), X03 (`<= 4`), X10 (`normais`) e as variantes X10b/c/d (amostras sem as de `excecoes`, sem as de `pos_pulo`, sem as de `sem_mov`). Também X06, X13, X13b (`Reposition` como automático), X51, Z10, Z10b (`[:1]`) e Z11. **Todas vermelhas.**
- **Amostra das tabelas do construtor (26):** 10 de `quebras-q2-rodada2.txt` e 16 da varredura, reescritas à mão a partir da forma `ast.unparse`. **Todas vermelhas.** Nas 10 da tabela de quebras, o primeiro teste vermelho é o mesmo que a evidência registra.
- **Variantes novas minhas (65):** **60 vermelhas e 5 verdes.**

Q3: os testes novos conferem valores calculados à mão (2900/3100, 42550, `(t0, pulo_fim + 2000, 6)`, o CSV inteiro do `timestamps`). Exigem contagem mínima, como "2 Result() em 5 blocos", `≥ 60` fronteiras e `≥ 3` tamanhos de texto. Também conferem que o cenário existe, como `pulo_fim % 100 == 0` e `ep["fim_ms"] < pulo_fim + 3000`. Não achei teste tautológico: cada `assert` compara com um número fixo ou com um valor que o próprio teste montou.

### Tabela das 105 quebras

| ID | Arquivo:linha | Troca | Resultado (testes vermelhos) | Primeiro teste vermelho |
|---|---|---|---|---|
| B-X01 | `metricas.py:368` | `<= ATRASO_MAX:` → `< ATRASO_MAX:` | VERMELHO (2) | test_rodada2_atraso_4_em_2900_e_3_em_3100_reprova_com_3100 |
| B-X02 | `metricas.py:368` | `<= ATRASO_MAX:` → `<= 2:` | VERMELHO (2) | test_rodada2_atraso_4_em_2900_e_3_em_3100_reprova_com_3100 |
| B-X03 | `metricas.py:368` | `<= ATRASO_MAX:` → `<= 4:` | VERMELHO (2) | test_rodada2_atraso_4_em_2900_e_3_em_3100_reprova_com_3100 |
| B-X10 | `metricas.py:281` | `_salto(amostras` → `_salto(normais` | VERMELHO (4) | test_rodada2_salto_errado_por_posicao_vale_dentro_dos_3_s_depois_do_pulo |
| B-X10b | `metricas.py:281` | `_salto(amostras` → `_salto([t for t in amostras if not _dentro(t, excecoes)]` | VERMELHO (4) | test_rodada2_salto_errado_por_posicao_vale_dentro_dos_3_s_depois_do_pulo |
| B-X10c | `metricas.py:281` | `_salto(amostras` → `_salto([t for t in amostras if not _dentro(t, pos_pulo)]` | VERMELHO (1) | test_rodada2_salto_errado_por_posicao_vale_dentro_dos_3_s_depois_do_pulo |
| B-X10d | `metricas.py:281` | `_salto(amostras` → `_salto([t for t in amostras if not _dentro(t, sem_mov)]` | VERMELHO (3) | test_rodada2_salto_por_posicao_vale_dentro_de_intervalos_sem_movimento_automatico |
| I-X06 | `metricas.py:375` | `elif all(` → `elif any(` | VERMELHO (3) | test_rodada2_dois_pulos_valem_todos_nao_qualquer |
| I-X13 | `metricas.py:338` | `not in AUTOMATICOS` → `== "Stay"` | VERMELHO (6) | test_rodada2_salto_por_posicao_vale_dentro_de_intervalos_sem_movimento_automatico |
| I-X13b | `metricas.py:338` | `not in AUTOMATICOS` → `not in AUTOMATICOS + ("Reposition",)` | VERMELHO (6) | test_rodada2_salto_por_posicao_vale_dentro_de_intervalos_sem_movimento_automatico |
| I-X51 | `metricas.py:254` | `e.fim is not None and ` → `(removido)` | VERMELHO (1) | test_rodada2_toque_em_token_sem_tempo_vai_ate_o_proximo_token_com_tempo |
| I-Z10 | `timestamps.py:63` | `.get("result", [])` → `.get("result", [])[:2]` | VERMELHO (1) | test_rodada2_varios_result_acumulam_todas_as_palavras_na_ordem |
| I-Z10b | `timestamps.py:63` | `.get("result", [])` → `.get("result", [])[:1]` | VERMELHO (4) | test_C1_csv_com_cabecalho_e_palavras_em_ordem |
| I-Z11 | `timestamps.py:71` | `palavras += ` → `palavras = ` | VERMELHO (1) | test_rodada2_varios_result_acumulam_todas_as_palavras_na_ordem |
| S-X60 | `metricas.py:155` | `not 0 <= indice < n_tokens` → `indice >= n_tokens` | VERMELHO (1) | test_rodada2_validacoes_dos_eventos |
| S-X70 | `metricas.py:516` | `if args.hipoteses and formas is None:` → `if False:` | VERMELHO (1) | test_rodada2_hipoteses_sem_texto_de_teste_correspondente_sai_com_2 |
| S-Y07 | `alinhar.py:142` | `min(_PRIORIDADE,` → `min((_M, _I, _D),` | VERMELHO (1) | test_rodada2_ultima_palavra_repetida_fica_com_a_primeira_vez |
| S-Y15 | `alinhar.py:61` | `inicio < anterior` → `fim < anterior` | VERMELHO (1) | test_rodada2_transcricao_invalida_mais_casos |
| S-X21 | `metricas.py:311` | `* len(atrasos)` → `* (len(atrasos) - 1)` | VERMELHO (1) | test_rodada2_limite_de_95_por_cento_com_n_21 |
| S-X31 | `metricas.py:269` | `ultima + 1` → `ultima` | VERMELHO (3) | test_rodada2_amostragem_nunca_comeca_antes_de_zero |
| S-Z01 | `timestamps.py:40` | `fullmatch` → `match` | VERMELHO (3) | test_rodada2_nome_com_sufixo_depois_do_wav_sai_com_2 |
| S-X08 | `metricas.py:365` | `if t < fim:` → `if t < inicio:` | VERMELHO (1) | test_rodada2_pulo_fim_marcado_depois_de_o_orador_retomar_da_zero_nao_negativo |
| S-X50 | `metricas.py:251` | `linha.t <= ev.t` → `linha.t < ev.t` | VERMELHO (1) | test_rodada2_ultima_linha_ate_o_evento_manda_mesmo_com_t_igual |
| S-X40 | `metricas.py:386` | `elif linha.move in AUTOMATICOS:` → `else:` | VERMELHO (1) | test_rodada2_linhas_stay_nao_repetem_a_violacao |
| S-v31 | `metricas.py:31` | `ATRASO_MAX = 3` → `ATRASO_MAX = 4` | VERMELHO (11) | test_E2_atraso_4_em_5_por_cento_passa |
| S-v263 | `metricas.py:263` | `b + POS_PULO_MS)` → `b + POS_PULO_MS - 1)` | VERMELHO (1) | test_rodada2_tres_segundos_depois_do_pulo_fecham_no_ms_exato |
| S-v310 | `metricas.py:310` | `a <= ATRASO_MAX` → `a < ATRASO_MAX` | VERMELHO (1) | test_E1_atraso_3_ainda_conta_como_dentro |
| S-v311 | `metricas.py:311` | `ate3 * 100 >=` → `ate3 * 100 >` | VERMELHO (3) | test_E2_atraso_4_em_5_por_cento_passa |
| S-v327 | `metricas.py:327` | `avanco > AVANCO_MAX` → `avanco >= AVANCO_MAX` | VERMELHO (3) | test_rodada2_salto_errado_por_posicao_vale_dentro_dos_3_s_depois_do_pulo |
| S-v342 | `metricas.py:342` | `a <= linha.t < b` → `a <= linha.t <= b` | VERMELHO (2) | test_E12_stdout_traz_criterios_latencias_fim_e_proveniencia |
| S-v360 | `metricas.py:360` | `e.token >= int(detalhe)` → `e.token > int(detalhe)` | VERMELHO (1) | test_plano_A5_alvo_e_o_primeiro_token_com_tempo_a_partir_do_pulo_inicio |
| S-v368 | `metricas.py:368` | `esp >= alvo` → `esp > alvo` | VERMELHO (4) | test_plano_A5_alvo_e_o_primeiro_token_com_tempo_a_partir_do_pulo_inicio |
| S-v375 | `metricas.py:375` | `<= RECUPERACAO_MAX_MS` → `< RECUPERACAO_MAX_MS` | VERMELHO (2) | test_E6_recupera_em_3000_passa |
| S-v387 | `metricas.py:387` | `linha.cursor < hw - RECUO_MAX` → `linha.cursor <= hw - RECUO_MAX` | VERMELHO (1) | test_E7_recuo_para_37_nao_viola |
| S-v395 | `metricas.py:395` | `linha.cursor >= i` → `linha.cursor > i` | VERMELHO (3) | test_E11_fala_cursor_e_latencias_1_e_2 |
| S-v203 | `metricas.py:203` | `a <= t < b` → `a < t < b` | VERMELHO (15) | test_E2_intervalos_de_excecao_nao_entram_na_conta |
| S-v282 | `metricas.py:282` | `_recuperacao(amostras)` → `_recuperacao(normais)` | VERMELHO (15) | test_E8_deslocamento_leva_ao_relogio_do_esperado |
| S-v511 | `metricas.py:511` | `linha.cursor >= len(esperado)` → `linha.cursor > len(esperado)` | VERMELHO (1) | test_E10_entrada_invalida_sai_com_2 |
| S-a151 | `alinhar.py:151` | `ultimas[b - 1]` → `ultimas[b - 2]` | VERMELHO (29) | test_rodada2_insercao_no_comeco_com_substituicao_adiante |
| S-r66 | `roteiro.py:66` | `if n < 20:` → `if n < 19:` | VERMELHO (21) | test_B3_formas_dos_numeros_batem_com_a_referencia |
| N01 | `metricas.py:225` | `(e.fim, e.token) for e` → `(e.inicio, e.token) for e` | VERMELHO (14) | test_E1_atraso_3_ainda_conta_como_dentro |
| N02 | `metricas.py:368` | `esp - self.cursor_em(t)` → `alvo - self.cursor_em(t)` | VERMELHO (2) | test_rodada2_atraso_4_em_2900_e_3_em_3100_reprova_com_3100 |
| N03 | `metricas.py:369` | `recuperou = t - fim` → `recuperou = t - inicio` | VERMELHO (20) | test_E8_deslocamento_leva_ao_relogio_do_esperado |
| N04 | `metricas.py:263` | `for _, b, _ in self.intervalos` → `for b, _, _ in self.intervalos` | VERMELHO (2) | test_E2_tres_segundos_depois_do_pulo_nao_entram_na_conta |
| N05 | `metricas.py:343` | `self.esperado_em(a)` → `self.esperado_em(b)` | VERMELHO (1) | test_E4_classe_usa_o_esperado_do_inicio_do_intervalo |
| N06 | `metricas.py:389` | `hw = max(hw, linha.cursor)` → `hw = linha.cursor` | VERMELHO (1) | test_E7_hw_e_o_maior_nao_o_ultimo |
| N07 | `metricas.py:385` | `hw = linha.cursor` → `hw = linha.cursor if hw is None else max(hw, linha.cursor)` | VERMELHO (1) | test_E7_reposition_redefine_hw |
| N08 | `metricas.py:252` | ` and anteriores[-1].cursor == i` → `(removido)` | VERMELHO (1) | test_plano_A7_so_a_reposition_para_i_antecipa_a_janela |
| N09 | `metricas.py:252` | ` and anteriores[-1].move == "Reposition"` → `(removido)` | VERMELHO (1) | test_plano_A7_so_a_reposition_para_i_antecipa_a_janela |
| N10 | `metricas.py:268` | `max(max(self._fins), self.trajetoria[-1].t)` → `max(self._fins)` | VERMELHO (1) | test_E11_latencia_acima_da_meta_nao_muda_o_codigo |
| N11 | `metricas.py:268` | `max(max(self._fins), self.trajetoria[-1].t)` → `self.trajetoria[-1].t` | VERMELHO (4) | test_amostras_vao_ate_o_maior_fim_do_esperado |
| N12 | `metricas.py:117` | `_inteiro(t, "trajetória", n) + deslocamento` → `_inteiro(t, "trajetória", n)` | VERMELHO (2) | test_E8_deslocamento_leva_ao_relogio_do_esperado |
| N13 | `metricas.py:157` | `_inteiro(t, "eventos", n) + deslocamento` → `_inteiro(t, "eventos", n)` | VERMELHO (1) | test_E8_deslocamento_leva_ao_relogio_do_esperado |
| N14 | `metricas.py:167` | `_inteiro(t, "hipóteses", n) + deslocamento` → `_inteiro(t, "hipóteses", n)` | VERMELHO (1) | test_E8_deslocamento_leva_ao_relogio_do_esperado |
| N15 | `metricas.py:406` | `h.aceita and ` → `(removido)` | VERMELHO (1) | test_E11_fala_cursor_e_latencias_1_e_2 |
| N16 | `metricas.py:406` | `h.kind in ("partial", "final")` → `h.kind in ("final",)` | VERMELHO (2) | test_E8_deslocamento_leva_ao_relogio_do_esperado |
| N17 | `metricas.py:415` | `alcance - e.fim` → `alcance - e.inicio` | VERMELHO (2) | test_E11_fala_cursor_e_latencias_1_e_2 |
| N18 | `metricas.py:424` | `h.t - e.fim` → `h.t - e.inicio` | VERMELHO (1) | test_E11_fala_cursor_e_latencias_1_e_2 |
| N19 | `metricas.py:422` | `tempos_aceitas, e.inicio)` → `tempos_aceitas, e.fim)` | VERDE (0) | — |
| N20 | `metricas.py:410` | `_dentro(e.fim, excecoes)` → `_dentro(e.inicio, excecoes)` | VERDE (0) | — |
| N21 | `metricas.py:396` | `return linha.t if linha.move in AUTOMATICOS else None` → `return linha.t` | VERMELHO (4) | test_E11_fala_cursor_e_latencias_1_e_2 |
| N22 | `metricas.py:419` | `donos[p] == {e.token}` → `e.token in donos[p]` | VERMELHO (1) | test_E11_fala_cursor_e_latencias_1_e_2 |
| N23 | `metricas.py:208` | `math.ceil(0.95 * len(ordenados)) - 1` → `int(0.95 * len(ordenados))` | VERMELHO (2) | test_percentil95_posto_mais_proximo |
| N24 | `metricas.py:375` | `p["recuperacao_ms"] is not None and ` → `(removido)` | VERMELHO (3) | test_E6_nunca_recupera_reprova_mesmo_com_atraso_baixo_no_pulo_fim |
| N25 | `metricas.py:326` | `self.cursor_em(t) - self.esperado_em(t)` → `self.cursor_em(t) - self.esperado_em(t - PASSO_MS)` | VERMELHO (9) | test_rodada2_salto_errado_por_posicao_vale_dentro_dos_3_s_depois_do_pulo |
| N26 | `metricas.py:309` | `self.esperado_em(t) - self.cursor_em(t)` → `self.esperado_em(t + PASSO_MS) - self.cursor_em(t)` | VERMELHO (8) | test_E1_atraso_3_ainda_conta_como_dentro |
| N27 | `metricas.py:360` | `e.fim is not None and ` → `(removido)` | VERMELHO (2) | test_plano_A5_alvo_e_o_primeiro_token_com_tempo_a_partir_do_pulo_inicio |
| N28 | `metricas.py:343` | `congelado = self.esperado_em(a)` → `congelado = self.esperado_em(linha.t)` | VERMELHO (1) | test_E4_classe_usa_o_esperado_do_inicio_do_intervalo |
| N29 | `metricas.py:254` | `e.token >= i]` → `e.token >= i - 1]` | VERMELHO (2) | test_rodada2_totais_do_relatorio_fecham_com_toque_intervalos_e_pulo |
| N30 | `metricas.py:266` | `self.trajetoria[0].t` → `self.trajetoria[1].t` | VERMELHO (25) | test_rodada2_amostragem_nunca_comeca_antes_de_zero |
| T01 | `timestamps.py:49` | `w.getnchannels() != 1 or ` → `(removido)` | VERMELHO (1) | test_C3_formato_errado_sai_com_2_sem_stdout |
| T02 | `timestamps.py:49` | `w.getsampwidth() != 2 or ` → `(removido)` | VERMELHO (1) | test_C3_formato_errado_sai_com_2_sem_stdout |
| T03 | `timestamps.py:49` | ` or w.getframerate() != TAXA` → `(removido)` | VERMELHO (1) | test_C3_formato_errado_sai_com_2_sem_stdout |
| T04 | `timestamps.py:82` | `round(p["start"] * 1000)` → `round(p["end"] * 1000)` | VERMELHO (5) | test_C1_csv_com_cabecalho_e_palavras_em_ordem |
| T05 | `timestamps.py:82` | `round(p["end"] * 1000)` → `round(p["start"] * 1000)` | VERMELHO (5) | test_C1_csv_com_cabecalho_e_palavras_em_ordem |
| T06 | `timestamps.py:82` | `round(p["start"] * 1000)` → `int(p["start"] * 1000)` | VERMELHO (1) | test_C1_milissegundos_arredondados |
| T07 | `timestamps.py:82` | `round(p["end"] * 1000)` → `round(p["end"] * 100)` | VERMELHO (5) | test_C1_csv_com_cabecalho_e_palavras_em_ordem |
| T08 | `timestamps.py:41` | `m is None or ` → `(removido)` | VERMELHO (7) | test_C4_nome_fora_do_padrao_sai_com_2 |
| T09 | `timestamps.py:41` | ` or m.group("id") not in roteiro.IDS` → `(removido)` | VERMELHO (1) | test_C4_nome_fora_do_padrao_sai_com_2 |
| T10 | `timestamps.py:72` | `palavras += _palavras_do_json(reconhecedor.FinalResult())` → `pass` | VERMELHO (6) | test_C1_milissegundos_arredondados |
| T11 | `timestamps.py:82` | ` if conf is None else conf` → `conf` | VERDE (0) | — |
| T12 | `timestamps.py:70` | `if reconhecedor.AcceptWaveform(dados):` → `if reconhecedor.AcceptWaveform(dados) or True:` | VERMELHO (4) | test_rodada2_varios_result_acumulam_todas_as_palavras_na_ordem |
| T13 | `timestamps.py:122` | `if not args.modelo:` → `if False:` | VERMELHO (1) | test_C5_sem_modelo_sai_com_2 |
| T14 | `timestamps.py:124` | `if not Path(args.modelo).is_dir():` → `if False:` | VERMELHO (1) | test_C5_sem_modelo_sai_com_2 |
| T15 | `timestamps.py:28` | `_t[0-9]+` → `_t[0-9]*` | VERDE (0) | — |
| T16 | `timestamps.py:94` | `vosk.SetLogLevel(-1)` → `pass` | VERMELHO (1) | test_rodada2_modo_real_abre_o_modelo_so_por_caminho_e_liga_SetWords |
| T17 | `timestamps.py:101` | `vosk.KaldiRecognizer(model, taxa)` → `vosk.KaldiRecognizer(model, TAXA)` | VERDE (0) | — |
| T18 | `timestamps.py:127` | `fabrica(TAXA)` → `fabrica(8000)` | VERMELHO (2) | test_C1_csv_com_cabecalho_e_palavras_em_ordem |
| A01 | `alinhar.py:185` | `transcricao[min(js)].inicio` → `transcricao[max(js)].inicio` | VERMELHO (7) | test_plano_A3_empate_de_formas_fica_com_a_primeira |
| A02 | `alinhar.py:185` | `transcricao[max(js)].fim` → `transcricao[min(js)].fim` | VERMELHO (7) | test_plano_A3_empate_de_formas_fica_com_a_primeira |
| A03 | `alinhar.py:185` | `transcricao[min(js)].inicio` → `transcricao[min(js)].fim` | VERMELHO (23) | test_rodada2_dois_trechos_de_insercao_no_comeco_valem_um_trecho |
| A04 | `alinhar.py:183` | `any(exato` → `all(exato` | VERMELHO (7) | test_rodada2_omissao_no_meio_e_insercao_no_fim |
| A05 | `alinhar.py:59` | ` or fim < inicio` → `(removido)` | VERMELHO (1) | test_transcricao_invalida_sai_com_2 |
| A06 | `alinhar.py:151` | `for u in ultimas[b - 1]` → `for u in reversed(ultimas[b - 1])` | VERMELHO (1) | test_plano_A3_empate_de_formas_fica_com_a_primeira |
| A07 | `alinhar.py:28` | `_PRIORIDADE = (_I, _M, _D)` → `_PRIORIDADE = (_M, _I, _D)` | VERMELHO (2) | test_D5_repeticao_t08_fica_com_a_primeira_vez |
| A08 | `alinhar.py:28` | `_PRIORIDADE = (_I, _M, _D)` → `_PRIORIDADE = (_I, _D, _M)` | VERMELHO (1) | test_plano_A3_empate_de_formas_fica_com_a_primeira |
| A09 | `alinhar.py:131` | `map(min, *` → `map(max, *` | VERMELHO (6) | test_plano_A3_empate_de_formas_fica_com_a_primeira |
| A10 | `alinhar.py:90` | `(0 if a == palavra else peso)` → `(0 if a == palavra else 1)` | VERMELHO (7) | test_rodada2_insercao_no_comeco_com_substituicao_adiante |
| A11 | `alinhar.py:164` | `pares[indice].append((j - 1, exato))` → `pares[indice].append((j - 1, True))` | VERMELHO (3) | test_rodada2_insercao_no_comeco_com_substituicao_adiante |
| A12 | `alinhar.py:63` | `anterior = inicio` → `anterior = fim` | VERMELHO (2) | test_rodada2_palavras_que_se_sobrepoem_sao_aceitas_se_o_inicio_nao_recua |
| A13 | `alinhar.py:91` | `x + abre` → `x + estende` | VERMELHO (7) | test_rodada2_omissao_no_meio_e_insercao_no_fim |
| A14 | `alinhar.py:96` | `anterior + abre` → `anterior + estende` | VERMELHO (13) | test_rodada2_dois_trechos_de_insercao_no_comeco_valem_um_trecho |
| A15 | `alinhar.py:166` | `pai[s][j - 1] + custo` → `pai[s][j - 1] + 0` | VERMELHO (4) | test_rodada2_insercao_no_comeco_com_substituicao_adiante |
| A16 | `alinhar.py:157` | `inseridas.append(j - 1)` → `inseridas.append(j)` | VERMELHO (6) | test_rodada2_ultima_palavra_repetida_fica_com_a_primeira_vez |
| A17 | `alinhar.py:110` | `peso = len(linhas) + m + 2` → `peso = 1` | VERMELHO (2) | test_plano_A3_uma_edicao_vale_mais_que_os_trechos |

### As 5 verdes

| ID | Julgamento |
|---|---|
| N19 `metricas.py:422` (busca da hipótese a partir de `e.fim`) | MENOR: só a latência 1, que é informativa → OPR-040 |
| N20 `metricas.py:410` (exclusão das latências por `e.inicio`) | MENOR: informativa → OPR-040 |
| T11 `timestamps.py:82` (`conf` ausente vira `None`) | MENOR: o Vosk com `SetWords` sempre manda `conf`; se não mandasse, o `alinhar.py` recusaria com rc 2, nada silencioso → OPR-040 |
| T15 `timestamps.py:28` (`_t[0-9]*`) | MENOR: validação de nome → OPR-040 |
| T17 `timestamps.py:101` (`taxa` → `TAXA`) | Equivalente: a fábrica só é chamada com `TAXA` (`:127`) |

## Equivalências declaradas pelo construtor (c)

- **Y02 e Y03** (`alinhar.py:91,95`). O argumento vale: como `peso` é maior que todos os trechos juntos, D colado a I custa 2 edições, e uma substituição custa 1. Por isso a ordem I/D nunca entra num ótimo. Confirmei com um diferencial meu, com outra semente, fatias de até 25 tokens, improviso no meio e refrão na transcrição: 20.000 entradas, 0 diferenças. O controle (`lm[j]`) deu 15.105 diferenças.
- **Os 5 justificados por argumento valem.** `metricas.py:509/510/514` (`[0]`→`[-1]`): o argparse dá um único caminho por papel, então cada lista tem um elemento. `:545` (`indent`): é só formatação, e o JSON lido é o mesmo. `alinhar.py:129` (`fins[-1]`): está dentro de `if len(fins) == 1`.
- **Das 43 do `equivalencias-rodada2.txt`, 42 valem.** Algumas valem "no domínio", não em geral: `_INT_MAX ±1` só muda acima de `MAX = 1.000.000`, que já é recusado antes (`roteiro.py:265,318`). A guarda `:111`, o `_INF = 1 << 29` e o `peso ± 1` só mudam perto do limite de células, longe do L30. `maior = -0/-2`, a chave `(t, arquivo, arquivo)` (o sort é estável e a ordem por linha dentro do arquivo já vem da leitura), `max(-1, t0)`, `range(len - n + 2)` (a fatia curta nunca é igual a uma forma de tamanho n) e as variantes de `roteiro` também conferem.
- **Uma não vale: "alinhar:95 ramo else com ld[j - 2]".** Em j = 1, `lm[0]` é sempre `_INF`, então o `else` é sempre tomado e `ld[-1]` lê a última coluna. A inserção começa da transcrição já consumida. Contraexemplo: tokens `x y z x y z` e transcrição `x y z`. O original casa os tokens 3 a 5. O mutante levanta `StopIteration` na volta (traceback, rc 1). Num diferencial meu com trecho repetido no roteiro e lido uma vez, foram 6.000 casos e 583 diferenças. Todas as outras 21 do alinhador deram 0 nesse mesmo diferencial. O diferencial do construtor deu 0 porque o gerador nunca repete trecho do roteiro. O código atual está certo, e um roteiro de teste com trecho repetido e lido uma vez não está no protocolo. O mutante quebraria com erro visível, não com um veredito errado em silêncio. Por isso é MENOR → OPR-039.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | MENOR | `work/0004-…/evidencias/equivalencias-rodada2.txt` (linha "ramo else com ld[j - 2]"); `alinhar.py:95` | Equivalência declarada que não vale. Falta teste de alinhamento com trecho repetido no roteiro e lido uma vez | Contraexemplo `x y z x y z` / `x y z` → `StopIteration`; 583/6.000 no diferencial com repetição (`scratchpad/dif_rep.py`) | Teste em `DesempateComTokensExplicitosTest`; corrigir a linha da evidência. OPR-039 (motor-voz) |
| 2 | MENOR | `impl_plan.md`, "Rodada 2" e "Quebras da rodada 2" | "644 trocas" quando são 643 (o cabeçalho entrou na conta) | `wc -l` = 644; 595 + 48 = 643 | Corrigir o número. OPR-039 |
| 3 | MENOR | `metricas.py:410,422`; `timestamps.py:28,82` | N19, N20, T11 e T15 verdes (informativo e validação) | Tabela acima, 149/149 | Um teste por item, ou registrar como fora de escopo. OPR-040 (motor-voz) |

Os OPR-036 e OPR-037 têm cada item travado por teste. Reapliquei a amostra, e ficou toda vermelha. Marquei os dois como fechados no registro, com a ressalva do OPR-039 no 037.

## Não verificado

- O `timestamps.py` contra o pacote `vosk` real: ele não está instalado, e instalar é o portão humano 6 da 0005. A chamada está provada só contra o módulo falso (`model_path`, `SetWords(True)`, `SetLogLevel(-1)`).
- `connectedDebugAndroidTest`: a tarefa não toca o app.
- Não reapliquei todas as 643 trocas da varredura, só uma amostra de 16, mais as 10 da tabela de quebras.

---

# Histórico: rodada 1 (REPROVADO)

# Revisão — 0004-etapa0-ferramentas

**Revisor:** revisor · **Data:** 2026-10-11 · **Base do diff:** `0a6c8b1` (merge-base com `main`) → `d9c67e5` (`work/0004-etapa0-ferramentas`)

Resumo: as regras inegociáveis, os comandos e a paridade com o Kotlin estão em ordem. As 106 quebras do plano ficaram todas vermelhas. Mas duas quebras minhas no `metricas.py`, fora da tabela do plano, ficaram verdes e mudam o veredito da 0005: o "atraso ≤ 3" da recuperação de pulo e o salto errado por posição dentro dos 3 s depois do pulo. As duas são guardas de critério de saída sem teste. Mostrei a troca de veredito em memória (abaixo). Por isso, REPROVADO.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `cd etapa0 && ./gradlew :core:test lint ktlintCheck verifyMergedManifest testDebugUnitTest --rerun-tasks`: `BUILD SUCCESSFUL`, rc=0, `:app:verifyMergedManifest` executado. Abri o manifesto mesclado em `app/build/intermediates/merged_manifest/debug`: único `uses-permission` é `RECORD_AUDIO`. O `android.permission.DUMP` que aparece é atributo `android:permission` do receiver do profileinstaller, não pedido de permissão, e já existia antes desta tarefa. Nenhum arquivo Gradle nem manifesto no diff | OK |
| Texto não persiste nem vai para log | O diff não toca `etapa0/app` nem `core/src/main`. As ferramentas só leem `etapa0/textos/`: o `alinhar.py` recusa bytes diferentes (D8, `roteiro.py:345-350`, testado com +1, −1 e CRLF). O `metricas.py` não lê texto: acha o roteiro só entre os textos de teste (`roteiro.py:353-361`). As fixtures derivam de `etapa0/textos/`: conferi por script que todas as palavras sintéticas avulsas (`mal`, `livro`, `comecar`, `fala`, `muita`, `corintios`…) existem nas formas faladas dos textos. Não há discurso pessoal | OK |
| Áudio não gravado fora de `etapa0/` | `grep -rnE "wave\.open\([^)]*['\"]w\|\.writeframes\|setnchannels\|open\([^)]*['\"]wb"`: só aparece em `testes/test_timestamps.py:53-57` (silêncio em `TemporaryDirectory`). O `timestamps.py:48` abre com `"rb"` | OK |
| Rede | `grep -rnE "urllib\|http\.\|socket\|requests\|model_name\|lang=\|ftplib\|smtplib\|urlopen"` rc=1. O `vosk` só é importado dentro de `_fabrica_vosk` (`timestamps.py:89`, indentado). O modelo abre só por `vosk.Model(model_path=...)` (`:96`) | OK |
| Imports só da stdlib | O grep de imports só lista stdlib, módulos próprios e `vosk` (1×) | OK |
| `align/` sem Android | `git diff main --stat -- etapa0/core/src/main etapa0/app docs/ app/ gradle/ etapa0/*.gradle.kts etapa0/core/build.gradle.kts etapa0/gradle` vazio. O único Kotlin novo é `core/src/test/.../ReferenciaFormasTest.kt`, que importa só `org.junit` e `java.*` | OK |
| Contratos do TRD inalterados | Mesmo diff vazio. `docs/` intocado | OK |
| `SessionId` preso ao reconhecedor | Nenhum código de sessão tocado | N.A. |
| Cursor nunca abaixo de `highWater − 3` | Nenhum código de cursor tocado. O critério de recuo do `metricas.py` está travado (M11a/b, M12–M14 vermelhas) | N.A. |
| Só Compose | Nenhuma UI tocada | N.A. |

Comandos que eu mesmo rodei, com a saída real:

- `python3 --version`: 3.13.12. `python3 etapa0/ferramentas/testar.py`: `87 testes, 0 falhas, 0 erros, 0 pulados`, rc=0.
- Gradle acima: 375 casos nos XMLs reescritos (0 pulados, 0 falhas), entre eles os 2 do `ReferenciaFormasTest`.
- Os três `--help`: rc=0. Listam `wav --modelo -o`, `texto transcricao -o` e `trajetoria esperado eventos --deslocamento-ms --hipoteses --json`.
- `git diff main` fora de `etapa0/ferramentas`, `etapa0/core/src/test` e `work/0004-…`: só aparecem `.claude/agent-memory/*` e `registers/opr_register.md`, que são da fase de plano.

## Spec e plano

| Critério de aceite | Implementação | Teste |
|---|---|---|
| A1–A6 | `testar.py`, greps, `--help`, diff | comandos acima (verdes) |
| B1–B4 | `roteiro.py`, `ReferenciaFormasTest.kt`, 12 `.tsv` | `test_roteiro.py` (7), JVM (2). Quebras K abaixo |
| C1–C5 | `timestamps.py` | `test_timestamps.py` (11). Ver achado 6 (laço de `Result()`) |
| D1–D9 | `alinhar.py` | `test_alinhar.py` (21) |
| E1–E13 | `metricas.py` | `test_metricas.py` (45), `test_cli.py` (3). **E6 e a regra de salto errado do TESTES-ETAPA0 têm guarda sem teste (achados 1 e 2)** |

Desvios do plano: nenhum no código. O construtor copiou o plano, e as decisões A3, A5, A6, A7 e A9 estão no OPR-034 e no plano, justificadas.

Os testes de falha esperada são reais. Trajetória ruim reprova com código 1: E2 6%, E3, E4, E5 variante, E6 3100/nunca e E7. M49 (`return 0`) e M50 ficam vermelhas. E13 ponta a ponta passa com código 0.

## Testes (Q2 e Q3)

**Python.** Script em scratchpad: aplica uma troca por vez, roda `testar.py` e restaura com `git checkout`. A árvore ficou limpa no fim (`git status` vazio). Foram 182 quebras:

- **As 106 da tabela do plano (M, L, R, T): 106 vermelhas.**
- **76 minhas (X, Y, Z, W): 36 vermelhas e 40 verdes.** Todas as verdes estão na tabela abaixo.

Q3: os testes exigem contagem mínima (≥ 600 amostras de leitura normal, ≥ 20 amostras na janela de toque, > 50 tokens por texto, ≥ 2.101 números). Atraso sem nenhuma amostra REPROVA.

**Kotlin (paridade).** Quebrei um ponto por vez em `core/src/main/.../align/` e rodei `./gradlew :core:test --tests '*ReferenciaFormasTest*'`. Depois copiei o `build/referencia-atual/` (o "Regenerar") para uma cópia isolada da árvore (`git archive`) e rodei o `test_roteiro` do Python contra ela. Restaurei com `git checkout`, e o JVM voltou a rc=0.

| ID | Quebra | JVM | Python com a referência regenerada |
|---|---|---|---|
| KK1 | `NumberWords.kt:68` `"duas"`→`"dois"` | vermelho (2 falhas) | vermelho (291 subtestes) |
| KK2 | `NumberWords.kt:54` tirar `\|\| rest % 100 == 0` | vermelho | vermelho (38) |
| KK3 | `Tokenizer.kt:159` `c + "e" + v[0]`→`c + "de" + v[0]` | vermelho | vermelho (2) |
| KK4 | `NumberWords.kt:89` `"cem"`→`"cento"` | vermelho | vermelho (25) |
| KK6 | `NumberWords.kt:61` sem `quatorze` | vermelho | vermelho (64) |
| KK5 | `Normalizer.kt:13` `[-'’]`→`[-']` | **verde** | verde |

A paridade funciona como o plano promete. O teste JVM fica vermelho quando o Kotlin muda. O teste Python fica vermelho quando a referência é regenerada e o `roteiro.py` não acompanha (ele não lê o Kotlin diretamente: é o desenho do A2). O KK5 e a W03, a mesma troca no Python, ficam verdes porque nenhum texto de teste tem `’` nem `'` entre letras. A paridade fica provada no domínio dos textos e dos números, que é o que as ferramentas medem. Mandei para o OPR.

### Quebras que ficaram verdes (87/87)

| ID | Arquivo:linha | Troca | Julgamento |
|---|---|---|---|
| **X01** | `metricas.py:368` | `esp - cursor <= ATRASO_MAX` → `< ATRASO_MAX` | **BLOQUEANTE (achado 1)** |
| **X02** | `metricas.py:368` | `<= ATRASO_MAX` → `<= ATRASO_MAX - 1` | **BLOQUEANTE (achado 1)** |
| **X03** | `metricas.py:368` | `<= ATRASO_MAX` → `<= ATRASO_MAX + 1` | **BLOQUEANTE (achado 1)** |
| **X10** | `metricas.py:281` | `self._salto(amostras, …)` → `self._salto(normais, …)` | **BLOQUEANTE (achado 2)** |
| X06 | `metricas.py:375` | `all(...)` → `any(...)` entre pulos | IMPORTANTE (achado 3) |
| X13 | `metricas.py:338` | movimento no intervalo: `not in AUTOMATICOS` → `== "Stay"` (Reposition conta) | IMPORTANTE (achado 4) |
| X51 | `metricas.py:254` | janela do toque sem `e.fim is not None` | IMPORTANTE (achado 5) |
| Z10 | `timestamps.py:63` | `.get("result", [])` → `[:2]` | IMPORTANTE (achado 6) |
| Z11 | `timestamps.py:71` | `palavras += …Result()` → `palavras = …` | IMPORTANTE (achado 6), rodada à parte |
| X12 | `metricas.py:347` | classe `<= congelado` → `<= congelado + 1` | MENOR: só classificação, não muda veredito |
| X80 | `metricas.py:410` | latências sem excluir janela de toque | MENOR: informativo |
| X70 | `metricas.py:516` | `--hipoteses` sem texto correspondente não sai com 2 (A9) | MENOR: latências 1/2 viram n=0 sem aviso |
| X84 | `roteiro.py:359` | `tokens_por_palavras` devolve sempre o 1º texto | MENOR: só T01/T05 nos testes de latência |
| X60 | `metricas.py:155` | aceita índice negativo em `toque_palavra`/`pulo_inicio` | MENOR: validação |
| X61 | `metricas.py:118` | sem guarda de cursor negativo | MENOR: validação |
| X62 | `metricas.py:114` | sem validar `score` | MENOR: validação |
| X63 | `metricas.py:139` | sem guarda `fim < início` no esperado | MENOR: validação |
| X64 | `metricas.py:135` | `ini == "" and fim == ""` → `ini == ""` (aceita meio vazio) | MENOR: validação |
| X65 | `metricas.py:165` | sem validar `kind`/`accepted` | MENOR: validação |
| X69 | `metricas.py:96` | `!=` → `<` no número de campos (vira exceção, rc 1) | MENOR: validação |
| X71 | `metricas.py:539` | sem `OSError`/`UnicodeDecodeError` (arquivo ausente vira traceback, rc 1) | MENOR: rc 1 confunde com REPROVA |
| Y09 | `alinhar.py:59` | sem `inicio < 0` | MENOR: validação |
| Y10 | `alinhar.py:55-56` | sem validar `conf` | MENOR: validação |
| Y12 | `alinhar.py:111` | sem a guarda de tamanho (estoura com `OverflowError`, rc 1) | MENOR: não testável barato |
| Y15 | `alinhar.py:61` | ordem por `fim < anterior` | MENOR: validação |
| Y08 | `alinhar.py:147-148` | inserção no começo some do stderr | MENOR: só o roteiro da revisão manual |
| Y07 | `alinhar.py:142` | estado final `(_M,_I,_D)` em vez de `_PRIORIDADE` | MENOR: desempate só no fim da transcrição (repetição da última palavra) |
| Z01 | `timestamps.py:40` | `fullmatch` → `match` (sufixo depois de `.wav`) | MENOR: o ID continua exigido |
| Z05 | `timestamps.py:58` | sem capturar `wave.Error`/`EOFError`/`OSError` (WAV corrompido vira traceback) | MENOR: C3 não pede arquivo corrompido |
| X21 | `metricas.py:311` | `* len(atrasos)` → `* (len - 1)` | MENOR: o limite de 95% só é testado com n = 600; não há inteiro entre 569,05 e 570 |
| X31 | `metricas.py:269` | `ultima + 1` → `ultima` | MENOR: perde 1 amostra quando o fim é múltiplo de 100 |
| X30 | `metricas.py:266` | `max(0, t0)` → `t0` | MENOR: só com deslocamento negativo; acrescenta amostras com esperado −1 |
| X20 | `metricas.py:310` | `abs(a) <= 3` | Equivalente no veredito: cursor ≥ 4 à frente já é salto errado |
| X08 | `metricas.py:365` | `t < fim` → `t < inicio` | Equivalente com folha correta: `esperado ≥ alvo` só acontece depois do `pulo_fim` |
| X40 | `metricas.py:386` | `elif AUTOMATICOS` → `else` (Stay entra no recuo) | Equivalente: Stay não muda o cursor |
| X50 | `metricas.py:251` | `linha.t <= ev.t` → `<` | Equivalente: no empate, `inicio = ev.t` dos dois jeitos |
| Y02 | `alinhar.py:91` | I→D estende o trecho | Equivalente: D colado com I nunca é ótimo (uma substituição custa 1 edição, D+I custam 2) |
| Y03 | `alinhar.py:95` | inserção só abre a partir de M | Equivalente pelo mesmo motivo |
| Z09 | `timestamps.py:73` | ordena por `(end, start)` | Equivalente com palavras que não se sobrepõem |
| W03 | `roteiro.py:31` | junção sem `’` | Fora do domínio (KK5) |

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | BLOQUEANTE | `etapa0/ferramentas/metricas.py:368`; testes em `testes/test_metricas.py:289-358` | O limiar "atraso ≤ 3" da **recuperação de pulo** (TESTES-ETAPA0 Métricas; spec E6, D16) não tem teste em nenhum dos dois lados. Todos os cenários de `RecuperacaoTest` e do `test_plano_A5` recuperam com o cursor a 1 do esperado, e antes disso o atraso é de dezenas de tokens. As três trocas X01 (`<`), X02 (`<= 2`) e X03 (`<= 4`) ficam 87/87. As quebras M2a/M2b do plano ficam vermelhas só pelo critério de atraso, não pela recuperação. Lição das 0002/0003 de novo: a constante está travada, mas o uso dela em outro critério não está | Em memória, sobre a cópia limpa (`scratchpad/demo_x01.py`): cursor a exatamente 3 do esperado em `pulo_fim + 2900` e a 2 em `+3100`. Original `('PASSA', 2900)`. Com X01 `('REPROVA', 3100)`. Na 0005, uma gravação boa reprovaria (ou o contrário, com X03) | Teste com o cursor chegando a **exatamente** `esperado − 3` em `pulo_fim + 2900` (PASSA, 2900), e teste com `esperado − 4` em `+2900` e `− 3` em `+3100` (REPROVA, 3100). Os dois juntos travam X01, X02 e X03 |
| 2 | BLOQUEANTE | `etapa0/ferramentas/metricas.py:281` (`_salto(amostras, …)`); testes em `test_metricas.py:120-223` | O salto errado por posição deve valer em "qualquer amostra… em toda a trajetória" (TESTES-ETAPA0; spec "Salto errado"). Isso inclui os 3 s depois do pulo, que é exatamente onde o `Recovery`/`LongJump` do app salta. Nenhum teste tem cursor > esperado + 2 dentro de um intervalo de exceção. Trocar `amostras` por `normais` (X10, variável vizinha) fica 87/87. Seria o salto errado de um pulo que erra o destino (refrão, trecho repetido) e é corrigido em menos de 3 s, o caso que a métrica "zero saltos errados" existe para pegar | `scratchpad/demo_x10.py`: `Recovery` 1 s depois do `pulo_fim` cai 6 tokens à frente, e o orador alcança antes dos 3 s. Original `('REPROVA', 1)`. Com X10 `('PASSA', 0)` | Teste com salto por posição que começa e termina dentro de `[pulo_fim, pulo_fim + 3000)`, esperando 1 salto e REPROVA. Recomendo também um caso dentro de improviso ou pausa em que o cursor fica > esperado + 2 por amostra (não só por movimento) |
| 3 | IMPORTANTE | `metricas.py:375` | O veredito da recuperação com **mais de um pulo** não tem teste: `all` → `any` (X06) fica verde. Hoje o protocolo tem um pulo por gravação (T05, L30 min 15), por isso não bloqueia | 87/87 com X06 | Teste com dois pulos, um recuperado em 2900 e outro nunca: REPROVA |
| 4 | IMPORTANTE | `metricas.py:337-338` | "Movimento automático = Short, LongJump, Recovery" (spec) no salto por movimento: nenhum teste tem `Reposition` dentro de improviso, silêncio ou pausa. Tratar `Reposition` como automático (X13) fica verde. O app permite toque em palavra com a leitura pausada (`ReaderController.kt:144-152`, fase `Paused`) | `demo_x10.py`: toque em palavra durante a pausa. Original `('PASSA', 0)`. Com X13 `('REPROVA', 1)` | Teste com `pausa_toque`, `Reposition` + `toque_palavra` dentro da pausa e `retoma_toque`: zero saltos |
| 5 | IMPORTANTE | `metricas.py:254` | A janela do toque usa o primeiro token **com tempo** de índice ≥ *i* (D15). Nenhum teste deixa o token *i* sem tempo (a primeira palavra depois do toque não casou no alinhamento, caso plausível com o WER do Vosk). Sem `e.fim is not None` (X51), o código levanta `TypeError` e sai com rc 1, sem relatório | `demo_x10.py`: original calcula a janela até o token seguinte. X51 levanta `TypeError` | Teste com `toque_palavra` *i* e o token *i* sem tempo no esperado: a janela vai até o `t_fim` do próximo token com tempo |
| 6 | IMPORTANTE | `timestamps.py:63,69-72`; `testes/test_timestamps.py:22-38` | O reconhecedor falso devolve `Result()` só no 1º bloco, com 2 palavras. Por isso, acumular todas as palavras de cada `Result()` e de todos os `Result()` não tem teste. `palavras = …` no lugar de `+=` (Z11) e `[:2]` (Z10) ficam verdes. Com o Vosk real, que fecha um `Result()` a cada pausa, o `words.csv` ficaria só com o último trecho. O C1 do spec está cumprido ao pé da letra; o que falta são os dados do teste | 87/87 com Z10 e com Z11 | O falso fecha `Result()` em mais de um bloco, um deles com ≥ 3 palavras, e a saída tem todas as palavras na ordem |
| 7 | MENOR | ver tabela acima (X12, X20–X21, X30–X31, X60–X65, X69–X71, X80, X84, Y07–Y10, Y12, Y15, Z01, Z05) | Validações de entrada e partes informativas sem teste. Exceção não tratada sai com rc 1, igual a REPROVA, sem relatório | tabela de verdes | OPR-036 e OPR-037 (dono motor-voz) |
| 8 | MENOR | `etapa0/app/.../ReaderController.kt:144-152,269-277` × `metricas.py:194-195` | Toque em palavra com a leitura pausada retoma com `resume(fromTap = false)`, sem `retoma_toque`. O `events.csv` do app fica com `pausa_toque` sem fim, e o `metricas.py` sai com 2. Fora do roteiro do protocolo, mas dá para contornar com a folha manual (vários arquivos, D11) | leitura do código (Q4) | OPR-038 (dono arquiteto) |
| 9 | MENOR | `Normalizer.kt:13` × `roteiro.py:31` | A paridade do `’` na junção não tem prova (KK5 e W03 verdes): nenhum texto de teste tem o caractere | tabela K | OPR-037 (registro: paridade provada no domínio dos textos e números) |

Os achados 3 a 6 não bloqueiam sozinhos. Como já há rodada 2, recomendo fechá-los junto: são quatro testes curtos. Se ficarem de fora, estão no OPR-037.

**Para a rodada 2:** além dos testes acima, refaça as quebras X01, X02, X03 e X10 (e X06, X13, X51, Z10 e Z11, se entrarem) e pelo menos uma variante vizinha de cada. Exemplos: o `ATRASO_MAX` da recuperação trocado por literal 2 e 4; `_salto(amostras` com as amostras de `excecoes` filtradas. Registre na evidência.

## Não verificado

- O `timestamps.py` contra o pacote `vosk` real (JSON real, `SetWords`, `Model(model_path=…)` sem download): o pacote não está instalado, e instalar é o portão humano 6 da 0005. A afirmação de que `model_path` nunca baixa vem da fonte do pacote citada no plano. Eu não a abri.
- O tempo do L30 em outra máquina (sem meta, Q7).
- Não rodei `connectedDebugAndroidTest`: a tarefa não toca o app.
