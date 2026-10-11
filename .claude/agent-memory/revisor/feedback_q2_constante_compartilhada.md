---
name: q2-constante-compartilhada-entre-criterios
description: 0004 — constante travada por um critério esconde o mesmo limiar sem teste em outro critério; e trocar o conjunto de amostras (amostras↔normais) passa; quebre no local de uso, não na constante
metadata:
  type: feedback
---

Na 0004 (2026-10-11), as 106 quebras da tabela do plano ficaram todas vermelhas. Ficaram verdes duas quebras minhas que mudam o veredito:

- **Limiar repetido em outro critério.** `ATRASO_MAX` aparece no atraso e na recuperação de pulo. Trocar a constante (M2a/M2b) ficava vermelho, mas só por causa do critério de atraso. No local de uso da recuperação, `<=`→`<`, `-1` e `+1` ficavam todos verdes, porque os testes recuperavam com o cursor a 1 do esperado.
- **Troca do conjunto de amostras.** `_salto(amostras)` → `_salto(normais)` ficou verde. Nenhum teste tinha salto por posição dentro de um intervalo de exceção (3 s depois do pulo).

**Why:** o plano quebra a constante e o operador uma vez. Fica parecendo cobertura total, mas o limiar usado num segundo lugar não tem teste de borda.

**How to apply:** para cada constante, faça `grep` de todos os usos e quebre **cada uso no local** (`<=`→`<`, ±1). Para cada função que recebe uma coleção filtrada, troque pela coleção vizinha (todas↔filtradas). Exija testes de borda (valor exatamente no limiar) em cada critério. Para provar a troca de veredito sem criar teste, carregue o módulo com `exec` de uma cópia da fonte trocada em memória, sobre um `git archive` em scratchpad. Ver [[q2-variantes-guardas-alinhador]] e [[q2-script-gradle-etapa0]].
