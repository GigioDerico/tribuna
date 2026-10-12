---
name: aparelho-s23-licoes
description: S23 RQCW80BPXKN no adb: cabo cai, DND lento, pasta exportacoes, rotação, painel; calibração do ensaio
metadata:
  type: project
---

- S23 SM-S911B (RQCW80BPXKN), Android 16, PAGE_SIZE 4096. O cabo/adb caiu 3 vezes na 0005 (2026-10-12); na retomada (blocos pequenos) não caiu. Aplique ajustes só no começo do bloco e restaure no fim; se cair, pare e escreva H-cabo.
- `aparelho.sh aplicar` sempre diz "nao-perturbe" falhou: reaplicar `cmd notification set_dnd none` e conferir `zen_mode`=2. O `restaurar` sai com 1 só por brilho (modo automático: antes 104, depois 76-90): aceitar se só o brilho difere.
- O app grava em `files/exportacoes/` (não `exports/`).
- Painel de Depuração tem rolagem própria (região y 1500-2260); `ui.py` agora rola nos dois sentidos (OPR-045). `tocar Depuração` é alternância: não chame se o painel já está aberto.
- Rotação para paisagem fecha o painel. Voltar do segundo plano reabre a captura.
- Ensaio T01 sintético (afplay): level.csv mediana 200 ms; cursor 214. Reprodução WAV no release: cursor 214. Sem `timeout` no macOS: `perl -e 'alarm N; exec @ARGV'`.
- G2 no S23: partida a frio mediana 730 ms (n=10); primeira cópia instalação ~273 ms, prepare ~520 ms (n=3). `pm clear` do app de teste NÃO foi bloqueado.
- `dumpsys audio` completo lista apps pessoais: filtrar pelo pacote etapa0.
- Pendente na 0005: B8/C13 e números das ferramentas novas, depois do merge APROVADO.
