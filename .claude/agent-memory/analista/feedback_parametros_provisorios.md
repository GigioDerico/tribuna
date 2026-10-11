---
name: parametros-provisorios
description: Como tratar parâmetros que faltam no PRD/TRD em specs do Tribuna (orquestrador pediu explicitamente)
metadata:
  type: feedback
---

Não inventar parâmetros. Se um valor necessário não está no PRD/TRD, registrar como decisão no brainstorm com valor marcado PROVISÓRIO e a fonte (ex.: "decisão do analista, a calibrar na 0005"), ou declarar como limite de escopo. Critérios de aceite não devem depender do número provisório em si.

**Why:** pedido do orquestrador na 0002 e CLAUDE.md ("Não invente parâmetros").
**How to apply:** em todo spec do align, sensibilidade, limiares de áudio, tabela do modo Constante.
