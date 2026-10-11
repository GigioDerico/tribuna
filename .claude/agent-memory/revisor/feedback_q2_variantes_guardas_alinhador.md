---
name: q2-variantes-guardas-alinhador
description: 0002 e 0003 — quebrar a guarda inteira não basta; cada operando de cada if (inclusive cada fase numa guarda de fases do ciclo de vida) precisa de quebra própria já na rodada 1
metadata:
  type: feedback
---

No Aligner, as quebras planejadas (Q2-1..Q2-7) ficaram todas vermelhas, mas variantes vizinhas na **recuperação** passaram com tudo verde (0002, 2026-10-11): contar a ambiguidade depois do filtro A2, tirar o filtro A2 do candidato único, disparar no 1º miss. Também ficaram verdes D6 (Stay no cursor zera missStreak) e D9 (desempate por distância). Reprovei como BLOQUEANTE, porque a recuperação é o salto longo para qualquer ponto à frente e é o que faz o texto pular no meio da fala.

Na rodada 2, os testes novos pegaram tudo o que eu tinha listado. Mas a cláusula irmã no **mesmo `if`** (`heard.size >= RECOVERY_WORDS`, ou seja, 4 palavras) ficou verde, e eu não a tinha listado na rodada 1. Isso custou uma terceira rodada. No Ágil (k = 2) ela é alcançável: rodando no jshell, saltou 3.087 tokens com 3 palavras.

Na rodada 3, o construtor fez uma varredura de 109 quebras com operador mínimo (`>=`→`>`, ±1, `&&`→`||`) e travou todas. As que eu inventei fora da tabela e ficaram verdes eram **troca de variável de estado**: base da janela `cursor + W` → `highWater + W` (só testada com cursor == highWater) e `heard.size` → `words.size` (palavras normalizadas contra itens brutos). A varredura por operador não pega esse tipo de troca. Aprovei e mandei os dois casos para o OPR (015 e 016), porque não tocam a regra 6 nem a recuperação.

De novo na 0003 (2026-10-11), rodada 2: na rodada 1, a R21 tirava a guarda **inteira** do `onActivityStop` (`phase != Reading && phase != Paused`). O teste novo pegou essa quebra. Mas tirar só o operando `Paused` ficou verde: o `onStop` real com a leitura pausada não fecha a captura, e o microfone fica aberto em segundo plano. Isso custou outra rodada. Em guardas de fase, quebre **cada fase** separadamente e confira se existe teste partindo de cada estado da tabela da Máquina de estados (Reading, Paused, Preparing, Idle), não só do caminho feliz.

**Why:** o construtor escreve o teste sob medida para a quebra que eu nomeio. Se a minha lista de quebras está incompleta, o retrabalho também fica incompleto.

**How to apply:** na **primeira** rodada, abra cada `if`/`takeIf` que impede movimento e quebre cada operando separadamente: constante de misses, de palavras, de limite, ordem filtro/contagem e escolha entre candidatos. Inclua também a troca de variável por uma vizinha do mesmo tipo (cursor/highWater, heard/words, similarity/lastWord) e peça testes em estados onde essas variáveis divergem (cursor < highWater). Confira a alcançabilidade com cada preset (k varia: 4/3/2). Use um script em scratchpad que aplica a troca, roda `:core:test --rerun-tasks`, lê os XMLs e faz `git checkout`. Para demonstrar um caso sem criar arquivo de teste (isso foi negado pela permissão), use `jshell --class-path core/build/classes/kotlin/main:<kotlin-stdlib.jar>` com um `.jsh` em scratchpad. Funciona. Ver também [[q2-cobrir-todos-eventos-sessao]].
