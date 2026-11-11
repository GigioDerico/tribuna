---
name: scripts-qa-cadeia-ate-veredito
description: Revisão de scripts shell do QA (medir.sh): sem teste próprio; quebrar numa cópia descartável e rodar até o veredito.py; conferir origem de artefato reaproveitado
metadata:
  type: feedback
---

O `medir.sh` e os outros scripts da 0005 não têm teste próprio. Na revisão dos scripts do QA (0005, 2026-10-14), quem segurava as quebras do script era o `veredito.py` lá na frente. Com os sufixos ±200 trocados, o `.md` mostrou o resultado "esperado" e só o veredito pegou. Então a Q2 de script tem que rodar a cadeia inteira (folha → vizinhas → metricas → veredito), não só olhar o `.md`.

**Why:** se você olha só a saída do script, a quebra parece verde. A defesa real fica num consumidor a jusante.

**How to apply:**
- Copie `etapa0/ferramentas`, `etapa0/textos`, `comum.sh` e o script para um `git init` no scratchpad, com espaço no caminho. Gere dados com os helpers dos testes (ex.: `testes/test_cli_0005._caso_t04`, `_words`). Rode o script a partir de `cwd=/`.
- Complete wav/level/meta/revisao/oficiais.tsv e chame `veredito.py rodada` SEM `-I` (com `-I` o import de `metricas` falha).
- O hook nega comandos compostos grandes com `rm -rf`/heredoc. Escreva os geradores com Write em `scratchpad/ger/` e rode cada um num comando separado.
- Evidência do QA que diz "sem aparelho" pode usar artefato de uma corrida anterior no aparelho, até de outro build. Confira mtime e sha256 em `$TMPDIR/tribuna-0005/` e cruze com o `diario.txt` (Q8).

Relacionado: [[q2-cobrir-todos-eventos-sessao]], [[opr-ids-entre-branches]]
