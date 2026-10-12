---
name: sensibilidade-com-entrada-derivada-no-relogio-errado
description: 0005 — corridas de sensibilidade (d±200) que reaproveitam um arquivo derivado no relógio do outro lado ficam cegas por construção; e critério "sem movimento" sem guarda de cursor parado (Q3)
metadata:
  type: feedback
---

Na 0005 (2026-10-12), as 145 quebras do plano davam o resultado declarado e mesmo assim reprovei com 2 BLOQUEANTES. Nenhuma quebra por operador acharia os dois:

1. **Sensibilidade cega.** O `metricas.py` soma `--deslocamento-ms` à trajetória **e** aos eventos. A folha (improviso, silêncio, pulo) vem do `words.csv` (relógio do WAV) e é gravada no relógio do app com `d`. Com a mesma folha em d±200, a folha anda junto com a trajetória, e "movimento dentro do intervalo" (CS2/CS4) e a recuperação (CS3) dão o mesmo resultado nas três corridas. O `veredito.py` ainda exigia os mesmos arquivos (sha) nas três, o que travava o erro. Provei com o `metricas.py` real: folha igual → PASSA/PASSA/PASSA; folha fixa no WAV → PASSA/PASSA/REPROVA.
2. **CS4 sem guarda.** Um T06/T07 com o cursor no token 0 saía PASSOU (exemplo literal da Q3). O CS2 tinha a guarda; o CS4, não.

**Why:** a varredura por operador só testa o que o desenho já tem. Uma premissa errada sobre em que relógio vive cada entrada não aparece em mutação. Também não aparece em teste de cadeia escrito pelo mesmo desenho: o `test_cli` travava a folha única.

**How to apply:** em qualquer ferramenta que marca "sensível" variando um parâmetro, liste cada entrada e em que referencial ela foi medida (app, WAV, token). Para cada uma, pergunte se ela deveria ficar fixa ou andar com o parâmetro, e monte o caso de borda com a ferramenta real a jusante. Para cada critério do tipo "zero X", procure a guarda de "nada para checar" (cursor parado, zero leitura) e exija o teste. Na mesma revisão, metade da proveniência (`[0]` vs `[1]`) e a causa em `t_app` vs `t_ms` ficaram sem teste: troque índice vizinho e relógio vizinho. Ver [[q2-constante-compartilhada-entre-criterios]] e [[equivalencia-por-diferencial-precisa-de-estrutura]].
