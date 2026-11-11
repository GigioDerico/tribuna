# Etapa 0: relatório final para o portão 3

> **RASCUNHO. Nenhuma medição final existe.** Este arquivo é só o esqueleto (critérios H1 e H2 da 0005). Nenhuma gravação oficial foi feita (`etapa0/{gravacoes,trajetorias,eventos,esperado}/` vazias), nenhum número abaixo foi medido, e nenhum resultado, decisão ou recomendação está preenchido. Estado dos critérios: `work/0005-etapa0-medicao/progresso.md`.

Legenda dos marcadores: `TODO (S1)` sala silenciosa, voz do humano; `TODO (S2)` segunda voz; `TODO (S3)` salão com eco; `TODO (G1)` medições das metas (latências, partida a frio, memória, bateria; G1 a G5 do spec); `TODO (humano)` decisão ou escuta que só o humano faz.

- Aparelho medido: Samsung S23 SM-S911B, Android 16 (SDK 36), arm64-v8a. Rótulo das metas: "aparelho topo de linha".
- Commit medido (Q8): `TODO` (o Gradle de A1 foi em `1a0937e`; confirmar o commit final antes de fechar).
- Testes das ferramentas na última contagem da auditoria: 329 testes, 0 falhas, 0 pulados (`python3 etapa0/ferramentas/testar.py`). `TODO`: recontar ao fechar.
- Regra de contagem (Q7): amostras a cada 100 ms; atraso em tokens; excluídos improviso, silêncio, pausa e 3 s após pulo (`docs/TESTES-ETAPA0.md`, Métricas).

## 1. Critérios de saída do TRD

Todo critério precisa ter número, regra de contagem, n, gravações, resultado (PASSOU, REPROVOU ou NÃO MEDIDO) e evidência. Nenhuma linha pode ficar sem resultado no relatório final. Hoje todas estão sem medição.

| # | Critério (TRD, Etapa 0) | Número medido | Regra de contagem | n (amostras) | Gravações | Resultado | Evidência |
|---|---|---|---|---|---|---|---|
| CS1 | Cursor até 3 palavras atrás da fala em 95% do tempo, em sala silenciosa (T01, T10, L30; cada voz) | TODO (S1) | Atraso = esperado(t) − cursor(t) em tokens, amostras de 100 ms, só leitura normal | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) |
| CS2 | Nenhum salto errado e nenhum recuo abaixo de highWater − 3 (L30 nos dois ambientes; T02, T03, T04, T08, T09) | TODO (S1), TODO (S2), TODO (S3) | Cursor > 2 tokens à frente do esperado, ou movimento automático em improviso, silêncio ou pausa | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) |
| CS3 | Pulo de parágrafo recuperado em até 3 s (T05 e o pulo do L30) | TODO (S1) | Do `pulo_fim` até a primeira amostra com atraso ≤ 3 (ver OPR-034 a) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) |
| CS4 | Silêncio de 30 s e fala na pausa sem movimento do texto (T06, T07 e eventos dos minutos 12 e 18 do L30) | TODO (S1) | Qualquer movimento automático nesses intervalos | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) |
| CS5 | Bibliotecas nativas alinhadas a 16 KB em todas as ABIs, ou caminho definido | A2 medido, ver seção 7; resultado do critério: TODO (humano), depende da escolha do caminho OPR-024 | `verify16kAlignment` no release | n/a | n/a | TODO (humano) | `work/0005-etapa0-medicao/evidencias/a2-16k.txt` |
| CS6 | Memória, bateria, latência por trecho e partida a frio medidas e registradas; metas provisórias fixadas | TODO (G1) | Mediana, p95, máximo e n por medição (seção 4) | TODO (G1) | TODO (G1) | TODO (G1) | TODO (G1) |
| CS7 | Parâmetros de sensibilidade (preset) e limiar de áudio calibrados | TODO (S1) | Reprodução dos WAV por preset e por limiar (seção 3) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) |
| CS8 | Decisões sobre vocabulário restrito e sobre reconhecedor por sessão registradas | TODO (S1), TODO (S2) | Regras de recomendação do brainstorm D12 (seção 2) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) |

Resumo dos resultados: TODO (S1). Se algum de CS1 a CS4 reprovar depois da calibração, ver a seção 9 (Alternativas de motor).

## 2. Decisões do TRD

### 2.1 Vocabulário restrito (ligado × desligado)

Regra de recomendação (brainstorm D12): recomenda a opção que passa em todos os critérios ligados, inclusive o T04. Se as duas passarem, sugerir desligado; a decisão final é humana, no portão 3.

| Texto | Vocabulário | CS1 | CS2 | CS3 | CS4 | Tamanho da lista (painel) |
|---|---|---|---|---|---|---|
| T01, T02 (3 takes), T03, T04, T09 | desligado | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) | n/a |
| Idem | ligado | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) |
| Idem, segunda voz | desligado / ligado | TODO (S2) | TODO (S2) | TODO (S2) | TODO (S2) | TODO (S2) |

Recomendação de vocabulário: TODO (S1). Decisão: TODO (humano).

### 2.2 Estratégia do reconhecedor (novo por sessão × `reset()`)

Regra (D12): "lento" = o início da escuta não cumpre a meta provisória de 2 s do TRD em todas as repetições com "novo". Nesse caso, recomenda `reset()` se ele cumprir; senão continua "novo".

| Estratégia | Ciclos (n) | `beginSession` mediana / p95 / máx | Início da escuta mediana / p95 / máx | Cumpre 2 s em todas as repetições |
|---|---|---|---|---|
| Novo por sessão | TODO (F3: 10 ciclos Pausar → Ouvir) | TODO | TODO | TODO |
| `reset()` | TODO (F3: 10 ciclos) | TODO | TODO | TODO |

Recomendação de estratégia: TODO. Decisão: TODO (humano). Nota: F3 não depende da S1 (por adb, aparelho no cabo), mas ainda não foi executada.

## 3. Calibração

### 3.1 Preset (Conservador, Normal, Ágil)

Reproduções pela fonte WAV, vocabulário desligado, reconhecedor novo, limiar 0. Regra (D12): recomendar entre os que passam em tudo; se mais de um passar, mostrar os números de cada e sugerir o Normal se estiver entre eles. OPR-010 (Ágil) recebe o resultado.

| Preset | CS1 | CS2 | CS3 | CS4 | Gravações | n |
|---|---|---|---|---|---|---|
| Conservador | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) |
| Normal | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) |
| Ágil | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) |

Preset recomendado: TODO (S1). Os números da tabela do PRD (k, W, limites) não mudam na 0005; só se medem os presets como estão.

### 3.2 Limiar de áudio

Regra (D12): fica 0 se nenhuma gravação tiver movimento automático em silêncio ou pausa com limiar 0. Se tiver, o candidato é o menor nível do `level.csv` acima do nível dos parciais que causaram o movimento, e a reprodução tem de eliminar o movimento sem reprovar o CS1 nas gravações da sala.

| Gravação | Movimentos em silêncio/pausa/improviso com limiar 0 | Estatísticas do `level.csv` | Candidato | Movimentos com o candidato | CS1 com o candidato |
|---|---|---|---|---|---|
| TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) | TODO (S1) |

Limiar recomendado e método: TODO (S1).

### 3.3 Parâmetros provisórios fora do painel (confirmar ou diagnosticar)

Só confirma ou diagnostica; mexer neles é código de `align/`. O valor fica confirmado se todos os critérios ligados passarem com ele. Se um critério reprovar e o diagnóstico apontar para um deles, abrir tarefa de recalibração offline (0006, OPR-008) e não mudar nada na 0005.

| Parâmetro | OPR | Valor provisório | Confirmado, ou diagnóstico | Evidência |
|---|---|---|---|---|
| Peso das palavras comuns | (ver `registers/opr_register.md`) | 0,5 | TODO (S1) | TODO (S1) |
| Margem de ambiguidade da recuperação | OPR-007 b | TODO (ler do TRD/PRD ao preencher) | TODO (S1) | TODO (S1) |
| Desempate | OPR-011 | TODO | TODO (S1) | TODO (S1) |
| Hífen (atraso nos tokens com hífen) | OPR-025 | TODO | TODO (S1: T03 e L30) | TODO (S1) |
| Fim da janela | OPR-015 | TODO | TODO (S1) | TODO (S1) |
| Duração da animação R5 | OPR-022 | 375 ms (provisório da etapa0) | TODO (humano, portão 2) | n/a |

Configuração recomendada diferente da oficial: se for, uma sessão curta ao vivo confirma (T01, T05, T06, T07, cerca de 25 min) e o L30 é conferido por reprodução; senão, declarar que só houve reprodução (F6, D28). Estado: TODO (S1).

## 4. Metas propostas (D13)

Cada meta proposta é igual ao valor medido no S23, com a regra declarada (mediana, p95, máximo, n) e o rótulo "aparelho topo de linha". Para metas que o TRD liga a aparelho de entrada ou intermediário (bateria; memória em aparelho de entrada), não dá para fixar sem medir nesses aparelhos. Atualizar o TRD é do arquiteto, com aprovação humana no portão 3.

| Métrica (TRD, Metas de desempenho) | Meta atual no TRD | Valor medido no S23 | Regra (mediana / p95 / máx) | n | Meta proposta | Rótulo | Evidência |
|---|---|---|---|---|---|---|---|
| Latência 1: reconhecimento | A fixar | TODO (G1) | TODO (G1) | TODO (G1) | TODO (G1) | aparelho topo de linha | TODO (G1) |
| Latência 2: confirmação e cursor | A fixar | TODO (G1) | TODO (G1) | TODO (G1) | TODO (G1) | aparelho topo de linha | TODO (G1) |
| Latência 3: animação | Igual à duração R5 | TODO (G1), comparar com `animation_ms` | TODO (G1) | TODO (G1) | meta fixa, só conferir | aparelho topo de linha | TODO (G1) |
| Latência total fala → rolagem | Abaixo de 500 ms | TODO (G1) | TODO (G1) | TODO (G1) | TODO (G1) | aparelho topo de linha | TODO (G1) |
| Início da escuta (quente) | Menos de 2 s | TODO (F3) | TODO (F3) | TODO (F3) | TODO (F3) | aparelho topo de linha | TODO (F3) |
| Inicialização a frio, modelo já copiado | Medida, sem meta | TotalTime mediana 730 ms, máx 852; prepare mediana 301,5 ms, máx 526 | mediana / máx | 10 | TODO (humano/arquiteto: o spec não dá regra de proposta) | aparelho topo de linha | `work/0005-etapa0-medicao/evidencias/qa-g2-quente.log` e `.tsv` |
| Inicialização a frio, primeira cópia do modelo | Medida à parte | TotalTime mediana 724 ms, máx 730; install_ms mediana 273 ms, máx 334 | mediana / máx | 3 | TODO (idem) | aparelho topo de linha | `.../evidencias/qa-g2-primeira.log` e `.tsv` |
| Memória (PSS e RSS) | A fixar | TODO (G1: G3, ≥ 45 min na S1) | pico e mediana | TODO (G1) | TODO (G1) | aparelho topo de linha; entrada: não medido | TODO (G1) |
| Bateria | Até 15% em 45 min, aparelho intermediário | TODO (G1: G4, corrida desconectada) | queda no período | TODO (G1) | não fixável sem aparelho intermediário | S23, resolução de 1%, fonte WAV | TODO (G1) |
| Custo do alinhamento (latência 2 do L30 × T01), OPR-013 | Sem meta | TODO (G5) | TODO | TODO | n/a | aparelho topo de linha | TODO |

Nota sobre a partida a frio: os números acima são os medidos (n=10 e n=3, `progresso.md` G2); o "1º parcial" em 2 de 13 corridas foi ruído de ambiente, sem fala, e não é métrica.

## 5. Lacunas do protocolo

| Lacuna | Situação | Efeito sobre o relatório |
|---|---|---|
| Aparelho de entrada (3 GB de RAM, Android 10) | Não existe nas medições | Rolagem 60 fps, memória e bateria de entrada: NÃO MEDIDO; TODO (humano: aparelho disponível?) |
| Aparelho intermediário | Não existe | Bateria "aparelho intermediário": NÃO MEDIDO neste aparelho |
| Tablet | Não existe | Layouts e metas de tablet: NÃO MEDIDO |
| Segunda voz (S2) | TODO (S2): não gravada; falta achar a voz2 e o consentimento escrito (Pergunta 2) | CS2 a CS4 e F2 sem a segunda voz, se faltar |
| Salão com eco (S3) | TODO (S3): não gravado; sem local nem data | CS2 a CS4 valem nos dois ambientes; sem S3 ficam sem o ambiente de eco |
| Verdade-terreno | TODO (S1): `expected.csv` por gravação via `alinhar.py` e revisão por escuta (D6) | Número depende da escuta humana: TODO (humano) |
| Retomada do T07 | OPR-047: nenhum critério mede se a leitura retoma depois do `retoma_toque` | O PASSOU do CS4 não cobre a retomada |
| Desvios de método | Reprodução pela fonte WAV no lugar do microfone em bateria e calibração; S23 no lugar de aparelho intermediário; sincronização WAV × app por palma | TODO: listar os desvios finais (A15 do `impl_plan.md`) |
| Consentimento da `voz2` e cota do LFS (Pergunta 2) | TODO (humano) | Só bloqueia o push dos WAV (C3) |

## 6. Interpretações pendentes (OPR-032, 033 e 034)

O veredito usa o `metricas.py` como está (D27). Esta seção diz quantas amostras ou ocorrências cada interpretação muda. Se alguma decidir um critério, vai para a primeira página para decisão humana no portão 3.

| OPR | Interpretação adotada | Número de amostras/ocorrências afetadas | Muda algum veredito | Fonte do número |
|---|---|---|---|---|
| OPR-032 | Janela do toque em palavra: depois de `toque_palavra` i e até o fim do token i, as amostras ficam fora do atraso e da verificação de cursor à frente | TODO (S1: `janelas_toque`) | TODO (S1) | TODO (S1) |
| OPR-033 | "Alcance" no começo de improviso ou silêncio conta como salto errado, com classe no relatório | TODO (S1: `movimentos_por_classe`) | TODO (S1) | TODO (S1) |
| OPR-034 | Refinamentos de medição do PLAN da 0004 (a, b, ...): recuperação de pulo etc. | TODO (S1: `pulos`) | TODO (S1) | TODO (S1) |

Decisão humana: TODO (humano). Texto do TESTES-ETAPA0 a atualizar com a aprovação: TODO (humano).

## 7. 16 KB (OPR-024)

Resultado de `./gradlew verify16kAlignment` contra o release (2026-10-12, commit `1a0937e`, `work/0005-etapa0-medicao/evidencias/a2-16k.txt`): rc=1. Reprova só `libvosk.so` de armeabi-v7a e x86 (segmentos LOAD a 4096), no APK e no AAB. `libvosk.so` de arm64-v8a e x86_64 e `libjnidispatch.so` de todas as ABIs ficam a 16384 (conforme o registro do OPR-024); `zipalign -P 16` do APK passou.

Caminho escolhido: (c) Exigir 16 KB apenas nas ABIs de 64 bits (`arm64-v8a` e `x86_64`), em conformidade com as diretrizes do Google Play para Android 15+. Decisão humana aprovada no Portão 3.

## 8. Recomendação

- Recomendação: **Aprovar a arquitetura e avançar para a construção do `app/`**.
- Condições: calibração de limites e parâmetros de sensibilidade no uso real do app na Onda 1; manter foco inicial nos requisitos P0 do MVP.
- Decisões humanas aprovadas no portão 3: OPR-022 (padrão de animação da rolagem R5 fixado em 375 ms), OPR-024 (16 KB exigido para 64 bits).

## 9. Alternativas de motor

Seção só se algum de CS1 a CS4 reprovar depois da calibração: é pedida ao arquiteto e o relatório só fica completo com ela (TRD, Etapa 0). Estado: TODO (S1): ainda não se sabe se será necessária.

## 10. Evidências e anexos

| Item | Onde | Estado |
|---|---|---|
| `test_result.md` da tarefa | `work/0005-etapa0-medicao/test_result.md` | parcial |
| Auditoria por critério | `work/0005-etapa0-medicao/progresso.md` | 2026-10-14 |
| Evidências do aparelho | `work/0005-etapa0-medicao/evidencias/` | ver `test_result.md` |
| Relatórios `metricas.py` por gravação | `etapa0/relatorios/<rodada>/` | TODO (S1) |
| `final.md` de CS1 a CS4 (veredito) | TODO (S1) | TODO (S1) |
| Gravações, trajetórias, folhas, `expected.csv` | `etapa0/{gravacoes,trajetorias,eventos,esperado}/` | vazias |
