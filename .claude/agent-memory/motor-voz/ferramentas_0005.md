---
name: ferramentas-0005
description: Construção 0005 (sincronizar, folha, veredito, nivel): 270 testes, runner das 145 quebras Q2 por tabela do plano, tempo
metadata:
  type: project
---

Construída em 2026-10-12 no worktree Tribuna-0005-ferramentas, sem desvio de lógica: 121 testes novos (32/24/51/11/3), suíte 270, tudo verde de primeira.
- Blocos do plano extraídos por faixa de linhas (sed); para "ver falhar" de cada tarefa, guardar ferramentas e testes das tarefas seguintes fora do repo e devolver um a um.
- Runner Q2 lê a tabela do plano (⏎ = quebra de linha, "(remove)" = vazio) e roda as 145 quebras: ~10 s cada, ~25 min total; 144 vermelhas, S35 verde (equivalente declarado). Cuidado: `pgrep -f` num laço `until` casa com o próprio laço.
- Gradle no worktree funciona com ANDROID_HOME=$HOME/Library/Android/sdk, sem local.properties.
**Why:** repetir rápido em rodadas de revisão. **How to apply:** reaproveitar o runner (ajustando a faixa de linhas da tabela).
