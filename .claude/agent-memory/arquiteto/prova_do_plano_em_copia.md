---
name: prova-do-plano-em-copia
description: Como o arquiteto prova um plano com código completo antes de entregar (cópia descartável de etapa0 no scratchpad, testes, ktlint, quebras Q2)
metadata:
  type: feedback
---

Para planos que trazem o código inteiro (a skill writing-plans exige; o construtor é Sonnet), o arquiteto valida tudo numa cópia descartável: `rsync -a --exclude build --exclude .gradle --exclude .kotlin etapa0/ <scratchpad>/e0/` (não usar `rm -rf`: o sandbox nega), escreve o código lá, roda `:core:test`, `ktlintFormat`/`ktlintCheck`, `lint` e as quebras Q2 por troca de texto, e gera o `impl_plan.md` por script que injeta os arquivos validados no esqueleto (placeholders `{{F:caminho}}`). Nada da cópia é versionado.

Na 0003 acrescentei: (1) simulação tarefa a tarefa numa segunda cópia limpa (`scripts/sim.sh` no scratchpad: aplica os arquivos na ordem do plano e roda build/teste no fim de cada tarefa e no "ver falhar"), que pega dependência de compilação entre tarefas; (2) runner de quebras Q2 por troca de texto (`q2.py`: aplica, roda `testDebugUnitTest --rerun-tasks`, lê os XML, restaura), com a lista reaproveitada para gerar a tabela do plano. Quebra que sobrevive vira teste novo ou guarda removida.

**Why:** na 0002 a prova achou erro de algoritmo do TRD (A2), testes mal desenhados (T03 refrão, hífen em maiúsculas, "capítulo" solto) e formatação do ktlint antes de chegar ao construtor; cada um seria uma falha na escada.

**How to apply:** repetir em PLAN com lógica não trivial; declarar no plano a seção "Prova do plano" com contagens reais; um protótipo em Python no scratchpad é bom para explorar o algoritmo antes do Kotlin. Ver [[align-decisoes-0002]].
