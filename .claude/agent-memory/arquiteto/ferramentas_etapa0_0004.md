---
name: ferramentas-etapa0-0004
description: Decisões e surpresas do PLAN da 0004 (ferramentas Python da Etapa 0): ambiente, paridade por .tsv, NW com desempate por trechos, recuperação literal trivial, glob do zsh
metadata:
  type: project
---

PLAN da 0004 (2026-10-11), `work/0004-etapa0-ferramentas/impl_plan.md` (A1–A11), OPR-034/035:

- **Ambiente:** Python 3.13.12 no Mac do usuário, sem pytest, numpy nem vosk. Ferramentas só stdlib; `unittest` via `etapa0/ferramentas/testar.py`. Instalar `vosk`/modelo grande é portão 6 na 0005.
- **Paridade Kotlin↔Python:** referências `.tsv` em `etapa0/core/src/test/resources/referencia/` (Gradle já trata como entrada do teste; A6 do spec só deixava mexer em `core/src/test`). O teste JVM grava o atual em `core/build/referencia-atual/`; regenerar = `cp` explícito. Cabeçalho com sha256 do texto detecta defasagem. O port Python bateu de primeira usando `[0-9]` e espaço ASCII (no Java `\d`/`\s` são ASCII) e `isalpha() or isdecimal()` para `isLetterOrDigit`.
- **Needleman–Wunsch:** DP completo cabe (L30: 3,65 s, ~190 MB, `array("i")`); faixa rejeitada porque poderia mudar o resultado. Surpresa: "primeira ocorrência" sozinha dá tempo errado no FIM de improviso (palavra do improviso igual à que abre o parágrafo seguinte); resolvido com custo lexicográfico edições×peso+trechos (Gotoh, 3 estados). Busca aleatória de mutações achou casos mínimos para travar cada operando.
- **Métricas:** a recuperação literal do TESTES-ETAPA0 dá 0 ms sempre (no `pulo_fim` o esperado ainda não saltou) → exige `esperado ≥ alvo` do `pulo_inicio`. Deslocamento positivo recusaria toda gravação ao vivo → amostras começam na trajetória. Toque: o app grava `Reposition` antes do evento `toque_palavra` (ReaderController), janela começa na Reposition.
- **zsh:** `grep --include=*.py` sem aspas falha com "no matches found" (saída vazia parece "passou").

**Why:** spec 0004 deixou ao plano desempenho, lista de números e regeneração; as surpresas reprovariam medições reais na 0005.

**How to apply:** na 0005/0006, não remover o desempate por trechos nem a regra do alvo sem decisão (OPR-034); comandos com glob sempre entre aspas. Ver [[prova-do-plano-em-copia]], [[vosk-android-fatos-0003]].
