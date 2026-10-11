---
name: align-interpretacoes
description: Interpretações de escopo do align fixadas na 0002 (words = cauda, cursor = último token falado, miss, recuperação ambígua, peso comum provisório 0,5, formas faladas)
metadata:
  type: project
---

Decisões da 0002-etapa0-align (work/0002-etapa0-align/brainstorm.md D1–D23), valem para app e tarefas futuras até o TRD dizer outra coisa:

- `Aligner.advance(state, words)`: `words` = cauda das palavras confirmadas desde reposition/início de sessão (quem chama mantém e zera). Pendente confirmação do arquiteto (OPR-007); se virar mudança de semântica, CIA + portão humano.
- Cursor = índice do último token falado; leitura começa com `reposition(0)`; reposition dá score 0.0 e limita índice.
- Miss = nenhum candidato passa no limite; recuperação na chamada em que missStreak chega a 2, últimas 4 palavras, salta só com exatamente 1 posição ≥ limite longo (sem margem inventada).
- Peso das palavras comuns: lista do PRD (de, que, e, o, a, normalizadas) com 0,5 PROVISÓRIO, calibração na 0005.
- Números 0–999.999, formas femininas como alternativas (caso "340 pessoas" do L30); "dia primeiro" não é gerado.

**Why:** PRD/TRD não fixam esses pontos e o usuário proíbe inventar parâmetros; valores sem fonte entram como PROVISÓRIO com fonte declarada.
**How to apply:** em specs futuras do align/ViewModel (ex.: cauda de palavras no ViewModel, Volume − com sentenceStart), partir destas leituras; ao calibrar na 0005, atualizar o peso e a margem aqui.
