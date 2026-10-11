---
name: q2-cobrir-todos-eventos-sessao
description: Padrão recorrente (0001 e 0003) — testes de evento atrasado (regra 7) cobrem Partial/Final mas esquecem Error, e no consumidor o Partial atrasado passa porque o 1º parcial não entrega palavra; quebrar cada caminho no produtor E no consumidor
metadata:
  type: feedback
---

Na Q2 da regra 7, quebre **cada** caminho de emissão com sessão (Partial, Final e Error) separadamente, não só o que o construtor já quebrou. Faça isso **no produtor** (motor/fake) **e no consumidor** (o `when` que despacha o evento e o filtro do ViewModel/controlador).

- 0001 (2026-10-10): o plano e o construtor só testaram callback atrasado de Partial/Final. Trocar `FakeRecognizer.error` para `Error(active?.session, ...)` deixou tudo verde. BLOQUEANTE.
- 0003 (2026-10-11), de novo: no `VoskEngine`, o `Error` do `accept` que lança, carimbado com `synchronized(lock){current}?.id`, ficou verde. Os testes de exceção só testavam a sessão em que ela nasceu. No controlador, despachar só o `Partial` com `active ?: event.session` também ficou verde. Motivo: o primeiro parcial só grava o `previous` do `PartialReconciler` e não muda cauda nem cursor, que é o que o teste D1 olhava. O D1 dizia "nem chegam ao reconciliador". Os dois viraram BLOQUEANTE. As 54 quebras do plano estavam todas vermelhas e não pegaram nenhum dos dois.

**Why:** o TRD manda filtrar Partial, Final **e** Error pelo id. O construtor escreve o teste sob medida para a quebra que o plano nomeia, e o plano repete o mesmo ponto cego (Error, parcial isolado) de tarefa em tarefa. Asserção só sobre cursor/cauda não detecta evento que entrou no reconciliador sem efeito visível.

Na rodada 2, refaça a quebra do construtor **e** pelo menos uma variante (ex.: `SessionId(nextSession - 1)`), para não validar só o caso que o teste foi escrito para pegar.

**How to apply:** em revisão que toque `SpeechEngine`, `FakeSpeechEngine`, `VoskEngine` ou o filtro de sessão, liste cada ponto que cria evento com `SessionId` (inclusive o `catch` que vira `Error`) e cada ramo do despacho no consumidor, e faça uma quebra por ponto. Para o parcial atrasado, exija asserção que veja o efeito escondido (`accepted=0` na exportação ou o próximo parcial não herdando o prefixo). Confira também que as verificações de build estão ligadas a `check`/`assemble`/`bundle` (OPR-004, resolvido na 0003). Ver [[q2-variantes-guardas-alinhador]].
