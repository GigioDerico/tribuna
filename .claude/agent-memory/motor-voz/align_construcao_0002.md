---
name: align-construcao-0002
description: Como foi construir o align da 0002 (Normalizer a Aligner) a partir do plano pronto; o que passou de primeira e onde olhar
metadata:
  type: project
---

Construído em 2026-10-11 copiando o código do impl_plan 0002 sem mudança de lógica: 85 testes em `:core` verdes de primeira, ktlint sem `ktlintFormat`, 0 desvios.

- Plano com código compilado pelo arquiteto: copiar literalmente e rodar; o trabalho real é a disciplina de commit por tarefa e ver os testes falharem antes.
- Q2 (sete quebras) automatizável com um script Python que troca o texto, roda `:core:test` e lê `<failure message>` nos XMLs; todas deixaram vermelho o teste previsto.
- Tempo do L30 sintético no Mac: ~3.400 chamadas de advance, total 125-150 ms, máximo 2-3 ms (varia entre execuções). Sem limiar (D23); a 0005 mede no aparelho.
- Vosk ainda não exercitado nesta tarefa (nada sobre JSON/custo/vocabulário restrito).

**Why:** evitar rediagnóstico na 0003/0005. **How to apply:** na 0003 usar `Tokenizer.tokenize`, `PartialReconciler.onPartial/onFinal/reset`, `Aligner`, `AlignPresets`; quem chama mantém a cauda de palavras (decisão A1).
