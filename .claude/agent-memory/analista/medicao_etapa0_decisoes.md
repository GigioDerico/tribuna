---
name: medicao-etapa0-decisoes
description: Decisões de escopo da 0005 (medição da Etapa 0) que valem para 0006, tablet e rodadas futuras: trajetória oficial ao vivo, WAV pelo Mac, sincronização, agregação dos critérios, verdade-terreno = portão 6
metadata:
  type: project
---

Decisões da 0005-etapa0-medicao (work/0005-etapa0-medicao/brainstorm.md D1–D28), 2026-10-12:

- Trajetória oficial = microfone ao vivo no aparelho. O WAV de referência é gravado ao mesmo tempo pelo MacBook (QuickTime em qualidade Máxima + `afconvert`, nada instalado), porque o app não grava áudio. As reproduções do WAV pela fonte WAV servem para calibrar.
- Deslocamento: correlação do level.csv (bloco de 200 ms) com o envelope do WAV, mais palma. Veredito que muda dentro de ±200 ms = "sensível", não PASSOU. Ferramenta nova com teste e revisão.
- Agregação: por gravação. CS1 só na sala (T01, T10, L30). CS2: L30 nos dois ambientes + T02, T03, T04, T08, T09. CS3: T05 + L30. CS4: T06, T07 + L30. Zero amostras = NÃO MEDIDO.
- Peso comum, margem, desempate e hífen não estão no painel: na 0005 só se confirmam. Recalibrar é tarefa nova com o golden. A tabela do PRD não muda (portão 2).
- Verdade-terreno: não há caminho sem decisão humana que mantenha a validade. A transcrição do app usa tempo de chegada, e com o modelo pequeno a verdade fica correlacionada com o que se mede. Recomendado: portão 6 (vosk em venv + modelo grande, tudo fora do repo).
- Só existe o S23 (topo de linha). Metas de entrada e intermediário não podem ser fixadas; é lacuna declarada.
- A etapa0 não tem KEEP_SCREEN_ON, latência 2/3 e beginSession só aparecem no painel (lê-se por uiautomator), e não há tela "Fim".

**Why:** o orquestrador pediu uma linha clara entre agente, humano e portões, e nenhum limiar inventado.
**How to apply:** rodadas futuras (tablet, aparelho de entrada, troca de motor) e a 0006 partem destas convenções (nomes `<ID>_<voz>_<amb>_t<n>`, voz `giorgio`/`voz2`, `.folha.events.csv`). Ver [[ferramentas-etapa0-decisoes]], [[vosk-etapa0-decisoes]], [[parametros-provisorios]].
