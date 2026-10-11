---
name: equivalencia-por-diferencial-precisa-de-estrutura
description: 0004 r2 — "0 diferenças em 5000 entradas aleatórias" não prova equivalência; o gerador do construtor não tinha a estrutura (roteiro repetido) que distingue o mutante
metadata:
  type: feedback
---

Na rodada 2 da 0004 (2026-10-11), o construtor declarou 43 mutantes equivalentes, cada um com um diferencial aleatório de 5.000 entradas e 0 diferenças. Um deles não era: `alinhar.py:95`, ramo `else ld[j - 2]`. Em j = 1 o índice vira `ld[-1]` e lê a última coluna. Com tokens `x y z x y z` e transcrição `x y z`, o mutante dá `StopIteration`. Com trecho repetido no roteiro, foram 583 diferenças em 6.000 casos. O gerador do construtor fazia ruído na transcrição (troca, omissão, inserção), mas nunca repetia trecho do roteiro.

**Why:** o diferencial só enxerga o que o gerador produz. Índice negativo em Python (`[-1]`, `[j - 2]` com j = 1) não lança erro: dá a volta e lê o fim. Um mutante desses parece inofensivo em dados "normais".

**How to apply:** para cada mutante dito equivalente, procure um argumento de por que é equivalente. Se o argumento não fecha, monte à mão o caso que separaria (índice que dá a volta, estrutura repetida, borda de coluna 0/1) e rode um diferencial próprio com outra semente e estrutura adversária: roteiro repetido, improviso no meio, refrão, pulo longo. Leia o fonte do HEAD por `git show` para não colidir com o lote de quebras que está mexendo na árvore. Ver [[q2-variantes-guardas-alinhador]] e [[q2-constante-compartilhada-entre-criterios]].

Na mesma rodada, 65 variantes minhas (troca de variável vizinha e cada operando) deram 60 vermelhas. As 5 verdes eram só informativas, de validação ou equivalentes. A varredura do construtor fechou bem o que é guarda de critério. O que ainda escapa são as latências informativas e os detalhes de validação de entrada.
