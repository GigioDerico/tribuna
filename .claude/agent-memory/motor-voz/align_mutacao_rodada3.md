---
name: align-mutacao-rodada3
description: Lições da varredura de quebras mínimas do Aligner (0002 rodada 3): onde os testes tinham furo e como travar limites exatos
metadata:
  type: project
---

Antes de entregar código de limiar, rodar quebra mínima em CADA operando (>=/>, constantes, índices): na 0002, mais de 40 de 109 ficaram verdes mesmo com o revisor tendo pedido só 1 teste.

- Limite exato sem depender de float: probar com params de limiar impossível (long=2.0) e ler `score`; depois usar esse valor e `Math.nextUp` como limiar. `Similarity` é internal e acessível dos testes.
- Roteiro sintético: `Token(i, i..i, spoken, 1f, false)` direto, palavras de 10 letras sem 'z' e `noisy(w,n)` troca n letras por 'z' (2 → 0,8; 4 → 0,6).
- Ruído de ponto flutuante: somas (1,0; 0,6; 0,8) dão 2,4000000000000004 e (1,0; 0,8; 0,6) dão 2,4; serve para travar o EPSILON.
- 13 mutantes equivalentes (invariante highWater>=cursor, takeLast redundante, ramos 0/0 inalcançáveis); lista em work/0002/evidencias/quebras-q2-rodada3.txt.

**How to apply:** na 0003 e nas próximas, fazer a varredura antes de entregar o `Aligner`/`PartialReconciler` mexidos.
