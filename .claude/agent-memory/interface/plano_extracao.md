---
name: plano-extracao
description: impl_plan.md da etapa0 traz o código literal por arquivo; extrair com script Python em vez de redigitar
metadata:
  type: project
---

Nas tarefas de interface da 0003 (etapa0), o plano contém cada arquivo inteiro em blocos ```kotlin precedidos de "Criar `caminho`". Extrair por script (no scratchpad) preserva fidelidade e poupa tokens. Teste-primeiro: extrair só os testes, ver "Unresolved reference", depois o código.

**Why:** planos aprovados passaram pelo portão; divergir exige registro.
**How to apply:** conferir contagens de testes esperadas pelo plano (ex.: :app 118 após T12-13) como sinal de que nada faltou. Lint/ktlint do etapa0 só dão aviso (não erro) para opt-in de coroutines-test.
