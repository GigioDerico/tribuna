---
name: ferramentas-etapa0-decisoes
description: Decisões da 0004 (timestamps/alinhar/metricas.py) que valem para 0005/0006: interpretação das métricas, janela de toque, alcance literal, paridade por referência Kotlin, vosk = portão 6 na 0005
metadata:
  type: project
---

Decisões da 0004-etapa0-ferramentas (work/0004-etapa0-ferramentas/brainstorm.md D1–D22):

- Python só stdlib + unittest, executor `python3 etapa0/ferramentas/testar.py` (cabe na permissão das settings). O `vosk` é importado só no modo real do timestamps.py.
- Paridade das formas faladas: arquivos de referência gerados do Tokenizer/NumberWords Kotlin, comparados por um teste JVM e por um teste Python.
- NW de custo unitário com igualdade exata depois do normalizador. O token só ganha tempo com ≥ 1 palavra igual. No empate vale a primeira forma e a primeira ocorrência.
- Métricas: intervalos [início, fim); janela de toque (toque_palavra i até o t_fim de i) fora do atraso e do "à frente" (interpretação, texto do TESTES pendente de aprovação humana). Movimento automático no improviso, silêncio ou pausa conta literal mesmo quando é "alcance" (classificado; a tolerância seria decisão humana).
- Latências 2 e 3 não são exportadas pela 0003. O metricas calcula fala→cursor e, com --hipoteses, as latências 1 e 2 só nos tokens de palavras exclusivas. São informativas.
- Instalar o pacote vosk e baixar o modelo grande (GPLv3) é portão humano 6 na 0005.

**Why:** o TESTES-ETAPA0 tem lacunas que mudam o veredito dos critérios de saída, e o usuário proíbe inventar parâmetros e afrouxar verificação.
**How to apply:** specs da 0005/0006 partem destas definições. Se a 0005 achar "alcance" reprovando, abrir decisão humana em vez de mexer no metricas. Ver [[vosk-etapa0-decisoes]], [[align-interpretacoes]], [[parametros-provisorios]].
