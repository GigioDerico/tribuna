---
name: medir-saida-completa-0005-r4
description: Rodada 4 da 0005 (OPR-056) — código 1 do metricas.py é ambíguo (reprova ou exceção); medir.sh confere a saída pelo contrato; bash 3.2 do Mac
metadata:
  type: project
---

Código 1 de ferramenta Python da Etapa 0 é ambíguo: reprovação legítima ou exceção não tratada (o Python sai com 1). Decisão da 0005, rodada 4 (R4-1, commit 4e2b391): não mudar a ferramenta da 0004; o script confere a saída pelo contrato (.md não vazio, .json legível com `veredito`, 0 se e só se PASSA) antes de qualquer `mv`.

**Why:** o R3-2 (troca dos seis relatórios juntos) aceitava rc ≤ 1 e o revisor achou o conjunto misto com `raise` na corrida d+200. Distinguir por `Traceback` no stderr foi descartado (reordena a saída e depende do interpretador).

**How to apply:** ao desenhar script que consome ferramenta com "0/1 = veredito, 2 = entrada inválida", sempre validar os artefatos além do código. O `/bin/bash` do Mac é 3.2 (sem `declare -A`): conferir por iteração, não guardar códigos em vetor associativo. Ensaio com veredito VERDE/VERMELHO por sonda deixou as quebras legíveis numa linha. Ver [[correcao-revisao-0005-r1]] e [[prova-do-plano-em-copia]].
