---
name: correcao-revisao-0005-r1
description: Lições da rodada 1 da 0005 (ferramentas reprovadas) — relógio de cada entrada nas corridas ±200, guarda "motor andou depois", patches git no plano
metadata:
  type: project
---

Revisão das ferramentas da 0005 reprovou dois desenhos meus (2026-10-12). Correção no plano, seção "Correções da revisão rodada 1".

1. **Sensibilidade ±200 só vale se cada entrada ficar no relógio onde foi medida.** O `metricas.py` soma `--deslocamento-ms` à trajetória, às hipóteses e a TODOS os eventos. A folha (medida no WAV, gravada no relógio do app com d) andava junto com a trajetória: corridas ±200 cegas para improviso/silêncio/pulo. Solução: `vizinhas.py` escreve a folha de d com t+200 (corrida d−200) e t−200 (d+200); o veredito confere o invariante `ler_eventos(folha_k, desl_k) == ler_eventos(folha_d, d)`, não a constante.
   **Why:** eu tinha pensado em "deslocar a trajetória" sem listar o relógio de cada arquivo.
   **How to apply:** em qualquer medida com dois relógios, fazer a tabela arquivo → relógio antes de desenhar corridas de sensibilidade.

2. **"Zero movimentos dentro do intervalo" passa vazio se o motor morreu.** Guarda cursor≠0/leitura≠0 não pega "captura não reabriu" (cursor fica no token da pausa; amostras depois contam como leitura). Guarda certa: movimento automático com t ≥ fim de cada intervalo exigido; só leva PASSOU → NÃO MEDIDO.
   **How to apply:** todo critério "ausência de evento" precisa de prova de que o sistema estava vivo depois.

3. **Patch git dentro do plano funciona para executor Sonnet:** `git apply --check` aceita até com as linhas de contexto vazias sem o espaço (testado). Mais curto e exato que arquivo inteiro quando o arquivo já existe.

Ver [[prova-do-plano-em-copia]], [[sincronizacao-e-veredito-0005]].
