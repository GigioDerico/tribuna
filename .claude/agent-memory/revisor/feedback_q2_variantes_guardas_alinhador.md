---
name: q2-variantes-guardas-alinhador
description: 0002 — quebras do construtor só cobriam o caso sob medida; variantes da recuperação (ambiguidade, A2, nº misses, nº de palavras) passaram verdes em duas rodadas; enumerar TODA cláusula de cada if na rodada 1
metadata:
  type: feedback
---

No Aligner, as quebras planejadas (Q2-1..Q2-7) ficaram todas vermelhas, mas variantes vizinhas na **recuperação** passaram com tudo verde (0002, 2026-10-11): contar a ambiguidade depois do filtro A2, tirar o filtro A2 do candidato único, disparar no 1º miss. Também ficaram verdes D6 (Stay no cursor zera missStreak) e D9 (desempate por distância). Reprovei como BLOQUEANTE, porque a recuperação é o salto longo para qualquer ponto à frente e é o que faz o texto pular no meio da fala.

Na rodada 2, os testes novos pegaram tudo o que eu tinha listado. Mas a cláusula irmã no **mesmo `if`** (`heard.size >= RECOVERY_WORDS`, ou seja, 4 palavras) ficou verde, e eu não a tinha listado na rodada 1. Isso custou uma terceira rodada. No Ágil (k = 2) ela é alcançável: rodando no jshell, saltou 3.087 tokens com 3 palavras.

**Why:** o construtor escreve o teste sob medida para a quebra que eu nomeio. Se a minha lista de quebras está incompleta, o retrabalho também fica incompleto.

**How to apply:** na **primeira** rodada, abra cada `if`/`takeIf` que impede movimento e quebre cada operando separadamente: constante de misses, de palavras, de limite, ordem filtro/contagem e escolha entre candidatos. Confira a alcançabilidade com cada preset (k varia: 4/3/2). Use um script em scratchpad que aplica a troca, roda `:core:test --rerun-tasks`, lê os XMLs e faz `git checkout`. Para demonstrar um caso sem criar arquivo de teste (isso foi negado pela permissão), use `jshell --class-path core/build/classes/kotlin/main:<kotlin-stdlib.jar>` com um `.jsh` em scratchpad. Funciona. Ver também [[q2-cobrir-todos-eventos-sessao]].
