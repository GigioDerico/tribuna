---
name: ferramentas-0004-rodada2
description: Rodada 2 da 0004: 149 testes Py, 40 quebras do revisor travadas, scripts de quebra e equivalência reaproveitáveis
metadata:
  type: project
---

Rodada 2 fechada em a4d5004 (testes) + commit de evidências. Só testes mudam; ferramentas .py intactas.
- `work/0004-.../evidencias/quebras_rodada2.py` (uma troca por linha, testar.py, git checkout) e `equivalencias_rodada2.py` (diferencial em memória, controles que precisam diferir) servem de molde para varreduras futuras.
- Pegadinha: quebra com sed que não casa o texto fica "verde" por não ter mudado nada; confira `git diff --stat` antes de concluir (o runner faz assert no trecho).
- Mutantes de laço (`i += 0`) viram laço infinito: usar timeout, contam como detectados.
**Why:** o revisor da rodada 1 achou 2 bloqueantes por constante travada mas usada em outro critério. **How to apply:** ao travar constante, varrer todos os usos, não só a definição.
