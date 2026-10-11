---
name: ferramentas-0004
description: Construção da 0004 (ferramentas Python da Etapa 0): extrator de blocos do plano, contagens, tempo do L30, referências Kotlin
metadata:
  type: project
---

Construída em 2026-10-11 sem desvio: 87 testes Python, `:core` 120, `:app` 228, tudo verde de primeira em cada tarefa.

- Plano de 3.539 linhas: extrator no scratchpad achou as 13 cercas ```` ```` ```` e o caminho no último trecho em crase da linha anterior (índices 0 a 12 na ordem das tarefas). Mais rápido que copiar à mão.
- L30 sintético alinha em ~3,7 s (M4, Python 3.13.12), sem faixa no DP.
- Referências `.tsv` geradas pelo teste JVM em `core/build/referencia-atual/` e copiadas para `core/src/test/resources/referencia/`; o Python lê as mesmas.
- zsh: `$?` depois de pipe `| tail` dá o do tail; usar `${pipestatus[1]}`.

**Why:** reconstruir/regenerar sem redescobrir. **How to apply:** 0005 usa estas ferramentas com Vosk real (portão humano 6 para instalar).
