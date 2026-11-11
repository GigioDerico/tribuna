---
name: opr-ids-colidem-entre-worktree-e-principal
description: 0005 r2 — worktree e árvore principal numeraram OPR-043..045 com problemas diferentes; confira o registro dos dois lados antes de criar ou citar OPR
metadata:
  type: feedback
---

Na rodada 2 da 0005 (2026-10-14), o branch das ferramentas (worktree) e o `work/0005-etapa0-medicao` (árvore principal) criaram OPR-043, 044 e 045 com conteúdos diferentes. Commits dos dois lados citam esses IDs. Eu mesmo abri OPR-043..046 na rodada 1 sem olhar o outro branch.

**Why:** cada lado só vê o próprio `registers/opr_register.md`. No merge, o registro dá conflito e as citações em commit ficam ambíguas (Q8). Nenhum teste pega isso.

**How to apply:** quando a tarefa tem worktree, antes de criar um OPR, rode `grep -o "^| OPR-0[0-9]*"` no registro dos dois lados e use o próximo livre nos dois. Em toda revisão de branch paralelo, compare os IDs dos dois lados e aponte a colisão como achado de processo para o orquestrador renumerar no merge. Ver [[sensibilidade-com-entrada-derivada-no-relogio-errado]].

Também na r2: as guardas novas de "nada para checar" (motor depois do intervalo) tinham borda testada só por uma fonte de intervalo (pausa do app) e só com um intervalo por tipo. Quebre "primeiro intervalo só" (`[:1]`) e "folha da corrida vizinha" sempre que houver max/any sobre intervalos.
