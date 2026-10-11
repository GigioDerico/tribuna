---
name: q2-variantes-guardas-alinhador
description: 0002 — quebras do construtor só cobriam o caso para o qual o teste foi escrito; variantes nas guardas da recuperação (ambiguidade, filtro A2, nº de misses) e em D6/D9 passaram verdes
metadata:
  type: feedback
---

No Aligner, as quebras planejadas (Q2-1..Q2-7) ficaram todas vermelhas, mas variantes vizinhas na **recuperação** passaram com tudo verde (0002, 2026-10-11): contar a ambiguidade depois do filtro A2, tirar o filtro A2 do candidato único, disparar no 1º miss. Também ficaram verdes D6 (Stay no cursor zera missStreak) e D9 (desempate por distância). O teste de ambiguidade usava duas cópias idênticas, então só pegava `firstOrNull`. Reprovei como BLOQUEANTE, porque a recuperação é o salto longo para qualquer ponto à frente e é o que faz o texto pular no meio da fala.

**Why:** o construtor e o arquiteto escrevem a quebra junto com o teste que a pega; o teste fica sob medida. Cada cláusula de uma guarda conservadora ("só se única", "só se a palavra nova casa", "só depois de 2") precisa da própria quebra.

**How to apply:** em revisão de `Aligner` ou de qualquer guarda do tipo "na dúvida, parado", liste cada condição que impede movimento e quebre uma por uma (relaxar, mudar a ordem, trocar a constante). Use um script que aplica a troca, roda `:core:test --rerun-tasks`, lê os XMLs e faz `git checkout` do arquivo. Criar teste demonstrativo numa cópia em scratchpad foi negado pela permissão: argumente o caso por cálculo. Ver também [[q2-cobrir-todos-eventos-sessao]].
