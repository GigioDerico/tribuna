**Veredito: APROVADO**

# Revisão — 0005-etapa0-medicao, lacunas de teste do OPR-049 (veredito.py)

**Revisor:** revisor · **Data:** 2026-10-14 · **Base do diff:** `ad8c51b^1` (`c3159f0`) → `ad8c51b` (merge de `a7b2f9e`)

Escopo: a classe `LacunasOpr049Test` (4 testes) em `etapa0/ferramentas/testes/test_veredito.py:895-945`, que deve cobrir os achados 2, 3 e 4 da rodada 2 (`code_review.md`, quebras R2-01, R2-20, R2-28 e R2-29). O achado 5 (contagem do plano) e a renumeração dos OPR (achado 1) não são teste e ficam fora desta entrega (ver achado 1 abaixo).

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | O merge não toca `app/`, manifesto nem dependência (`git diff ad8c51b^1 ad8c51b --name-only` = só o arquivo de teste) | N.A. (nada mudou) |
| Texto não persiste nem vai para log | Os testes escrevem só arquivos sintéticos em pasta temporária (`Pasta()`), com eventos e trajetórias gerados por `testes/sintetico.py`; nenhum texto de discurso | OK |
| Áudio não gravado fora de `etapa0/` | O único "WAV" é `b"RIFF" + nome` em pasta temporária (`test_veredito.py:91`, já existente) | OK |
| `align/` sem Android | Sem mudança em `align/` | N.A. |
| Contratos do TRD inalterados | Sem mudança de código de produção | OK |
| `SessionId`, `highWater − 3`, só Compose | Sem mudança no app | N.A. |

Mudança de produção no merge:

```
$ git diff ad8c51b^1 ad8c51b --stat
 etapa0/ferramentas/testes/test_veredito.py | 53 ++++++++++++++++++++++++++++++
 1 file changed, 53 insertions(+)
$ git diff ad8c51b^1 ad8c51b -- . ':!etapa0/ferramentas/testes' | wc -l
       0
```

Observação: `e3247f5..ad8c51b` tem 16 arquivos porque inclui commits já revisados (`7d5f6dc`, `4ba60fb`, revisão dos scripts do QA, OPR-054 etc.). O que este merge trouxe é só o arquivo de teste.

## Cada teste contra o achado

| Achado (rodada 2) | Teste | O que trava | Conferido |
|---|---|---|---|
| 2, parte 1: borda `b`/`b−1` com intervalo da folha (R2-01) | `test_Q3_borda_da_guarda_com_o_silencio_da_folha` (`:899`) | T06, silêncio da folha em [1.000, 31.000): movimento em 31.000 → PASSOU; em 30.999 → NÃO MEDIDO com motivo exato `fim em {31000 + D}` | R2-01a e R2-01b vermelhas |
| 2, parte 2: dois intervalos do mesmo tipo (R2-20) | `test_Q3_dois_silencios_o_ultimo_movimento_entre_eles_nao_mede` (`:911`) | L30 com silêncios [26.000, 27.000) e [30.000, 60.000): movimento em 28.000 → NÃO MEDIDO citando o fim do segundo (60.000 + D); em 60.000 → PASSOU | R2-20 vermelha |
| 3: vizinha sem o `events.csv` do app (R2-28) | `test_Q8_corrida_vizinha_sem_o_events_csv_do_app_sai_com_2` (`:925`) | T07 (sem folha) e T06 (com folha), m200 e p200: tirar a entrada `<nome>.events.csv` → código 2, sem JSON, erro "mesmos arquivos" | R2-28 vermelha nas 4 subtestes |
| 4: PENDENTE de REPROVOU sensível (R2-29) | `test_D7_reprovou_sensivel_sem_revisao_fica_pendente_com_o_motivo` (`:936`) | T01 REPROVA em d, PASSA em m200, sem revisão → PENDENTE com motivo exato e a marca no Markdown | R2-29 vermelha |

O texto pedido em cada "O que resolve" da rodada 2 está atendido. O teste de borda usa a folha (T06), não a pausa do app. O teste dos dois silêncios usa o L30 com o último movimento entre eles. O teste da vizinha confere o código 2. O teste do PENDENTE exige "sensível ao deslocamento" no motivo e na saída.

## Testes (Q2 e Q3)

Suíte inteira na árvore principal (Python 3.13.12):

```
$ PYTHONDONTWRITEBYTECODE=1 python3 -B etapa0/ferramentas/testar.py
Ran 329 tests in 13.007s
OK
testar.py: 329 testes, 0 falhas, 0 erros, 0 pulados
```

Quebras feitas numa cópia descartável (`git archive ad8c51b etapa0/ferramentas etapa0/textos etapa0/core/src/test/resources` no scratchpad, fora da árvore). O script troca um trecho que precisa aparecer exatamente uma vez no `veredito.py`, roda o `testar.py` inteiro e restaura o original num `finally`. Linha de base na cópia: `329 testes, 0 falhas`. O número de falhas conta subtestes.

| Id | `veredito.py` | De → Para | Resultado | Testes vermelhos |
|---|---|---|---|---|
| R2-01a | `:333` | `parados_depois(caminhos["d"], …)` → `caminhos["m200"]` | **vermelho** (4 falhas) | borda da folha, dois silêncios |
| R2-01b | `:333` | → `caminhos["p200"]` (a variante que a rodada 2 julgou inofensiva) | **vermelho** (2) | borda da folha, dois silêncios |
| R2-20 | `:280` | `faixas[k]` → `faixas[k][:1]` | **vermelho** (1) | dois silêncios |
| R2-20b (minha) | `:280` | `faixas[k]` → `faixas[k][:-1]` | vermelho (8) | os 2 novos de Q3 e 5 antigos |
| R2-21 | `:280` | `faixas[k]` → `faixas[k][-1:]` | verde | (equivalente, como na rodada 2) |
| X-borda (minha) | `:280` | `ultimo < b` → `ultimo <= b` | vermelho (3) | borda da folha, dois silêncios, `..._fim_exato_...` |
| R2-28 | `:308-309` | `comuns` também sem `f"{nome}.events.csv"` | **vermelho** (4) | vizinha sem `events.csv` |
| R2-29 | `:346` | `(f" ({motivo})" if motivo else "")` → `if motivo and estado != REPROVOU` | **vermelho** (1) | PENDENTE de REPROVOU |

Saída real do script:

```
R2-01a: testar.py: 329 testes, 4 falhas, 0 erros, 0 pulados
R2-01b: testar.py: 329 testes, 2 falhas, 0 erros, 0 pulados
R2-20: testar.py: 329 testes, 1 falhas, 0 erros, 0 pulados
R2-20b: testar.py: 329 testes, 8 falhas, 0 erros, 0 pulados
R2-21: testar.py: 329 testes, 0 falhas, 0 erros, 0 pulados
X-borda: testar.py: 329 testes, 3 falhas, 0 erros, 0 pulados
R2-28: testar.py: 329 testes, 4 falhas, 0 erros, 0 pulados
R2-29: testar.py: 329 testes, 1 falhas, 0 erros, 0 pulados
cmp restaurado: True
```

- Restauração: `filecmp.cmp(original, alvo, shallow=False)` → `True` depois da última quebra. A árvore do repositório não foi tocada (`git status --short` só mostra arquivos de `.claude/agent-memory/revisor/` que já estavam assim antes desta revisão).
- R2-21 continua verde e continua equivalente. `ultimo` é o maior movimento automático, então "parado depois de algum intervalo" é o mesmo que "parado depois do último". Não registro.
- **Q3:** cada teste novo tem um par positivo e um negativo (PASSOU/NÃO MEDIDO na mesma borda, código 2 contra o caminho feliz de `completa()`). Os NÃO MEDIDO conferem o motivo exato, então um NÃO MEDIDO por outra causa ("sem silencio nos eventos", configuração) não passaria.
- **Independência de dados reais:** os quatro testes usam só `Rodada(p.dir)` em pasta temporária. Não leem `etapa0/trajetorias/`, `etapa0/gravacoes/` nem textos pessoais, e não usam aparelho. "giorgio" é só o nome da voz no nome sintético do arquivo, como nos testes antigos.

## Q1, Q4, Q5, Q8

- Q1: o teste não acrescenta campo nem arquivo. Os helpers usados (`Rodada.completa`, `_trocar_entradas`, `_estado`, `metricas_json`) já existiam.
- Q4: conferi o `veredito.py` real em `ad8c51b` (linhas 280, 308, 333 e 346). As linhas citadas na rodada 2 continuam batendo.
- Q5: o docstring da classe diz "achados 2 a 4" e cobre os três. Não alega cobrir o achado 5.
- Q8: o commit `a7b2f9e` cita o OPR-049 e descreve os quatro itens que o teste cobre.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | MENOR (processo) | `registers/opr_register.md:58` | O OPR-049 tem cinco partes: (a), (b) e (c) são testes; (d) é o número 75→74 no `impl_plan.md`; e há a renumeração dos OPR-043 a 046 no merge. Este commit fecha só (a), (b) e (c). Se o OPR-049 for fechado inteiro citando `a7b2f9e`, (d) e a renumeração se perdem. | `git diff ad8c51b^1 ad8c51b --name-only` só traz o teste; o registro continua "Aberto" com (d) e a renumeração no texto. | Orquestrador: na Resolução do OPR-049, marcar (a) a (c) resolvidos por `a7b2f9e` e esta revisão. Deixar (d) e a renumeração abertos, ou movê-los para um OPR próprio com dono (arquiteto e orquestrador). Não editei o registro, como pedido. |

Nenhum achado no código do teste.

## Não verificado

- Não rodei as 74 quebras da tabela da rodada 2 nem as 145 antigas. Rodei só as 8 acima.
- Nada contra gravação real nem no aparelho: o escopo é teste de ferramenta da Etapa 0 com dados sintéticos.
- Não rodei `./gradlew` (lint, `verifyMergedManifest`): o merge não toca o projeto Android.
