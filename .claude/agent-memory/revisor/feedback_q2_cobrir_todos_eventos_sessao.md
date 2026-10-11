---
name: q2-cobrir-todos-eventos-sessao
description: Padrão recorrente — testes de callback atrasado (regra 7) cobrem Partial/Final mas esquecem Error; quebrar cada caminho de emissão separadamente
metadata:
  type: feedback
---

Na Q2 da regra 7, quebre **cada** caminho de emissão com sessão (Partial, Final e Error) separadamente, não só o que o construtor já quebrou. Na 0001 (2026-10-10), o plano e o construtor só testaram callback atrasado de Partial/Final; trocar `FakeRecognizer.error` para `Error(active?.session, ...)` deixou tudo verde — achado BLOQUEANTE (critério do spec dizia "vermelho se o fake ler sessão atual no callback").

**Why:** o TRD manda o ViewModel filtrar Partial, Final **e** Error pelo id; o fake é o instrumento das suítes futuras de sessão, então um caminho sem teste vira falso verde depois.

**How to apply:** em qualquer revisão que toque `SpeechEngine`, `FakeSpeechEngine`, `VoskEngine` ou o filtro de sessão no ViewModel, liste os métodos que emitem evento com `SessionId` e faça uma quebra por método. Também conferir: verificações de build (manifesto, 16 KB) estão ligadas ao ciclo `check`/`bundle`, não só como tarefa avulsa (ver OPR-004).
