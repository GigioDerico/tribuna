---
name: aparelho-s23-licoes
description: S23 RQCW80BPXKN no adb: cabo cai, DND lento, pasta exportacoes, rotação, painel; calibração do ensaio
metadata:
  type: project
---

- S23 SM-S911B (RQCW80BPXKN), Android 16, PAGE_SIZE 4096. O cabo/adb caiu 3 vezes na 0005 (a última durante B7, 2026-10-12 ~12:12). Aplique ajustes só no começo do bloco e restaure no fim; se cair, pare e escreva H-cabo.
- `aparelho.sh aplicar` às vezes diz "nao-perturbe" falhou: o zen_mode demora; reaplicar `cmd notification set_dnd none` e conferir `settings get global zen_mode` = 2. O `restaurar` sai com 1 por brilho (modo automático reajusta; antes 104, depois 90/114): aceitar se só o brilho difere. `restaura.sh` tolerante ficou no scratchpad.
- O app grava em `files/exportacoes/` (não `exports/`); scripts corrigidos (OPR-043).
- Rotação para paisagem fecha o painel de Depuração; ler `sessão` só reabrindo em retrato. `ui.py chip` falha com o painel rolado; o texto já selecionado vale.
- Ensaio T01 sintético (afplay, Mac vol 100): level.csv mediana 200 ms; leitura chegou a cursor 214. Sem `timeout` no macOS: usar `perl -e 'alarm N; exec @ARGV'`.
- `dumpsys audio` completo lista apps pessoais do humano: filtrar só o pacote etapa0 antes de anexar.
- Voltar do segundo plano reabre a captura (ReaderController onStart), por projeto.
- Pendente: B7 (release instalado; reproduzir.py parou em "Normal"), G2, C14, restaurar ajustes e reinstalar debug.
