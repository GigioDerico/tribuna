---
name: vosk-etapa0-decisoes
description: Decisões de escopo da 0003 (VoskEngine/etapa0) que valem para 0005/0006 e para o app: base de tempo, formatos de exportação, padrões provisórios, portões
metadata:
  type: project
---

Decisões da 0003-etapa0-vosk (work/0003-etapa0-vosk/brainstorm.md D1–D28):

- Tempo nunca entra em SpeechEvent (contrato). Quem consome carimba ms desde startCapture com relógio monotônico; latência 1 é calculada offline.
- Exportações: `hypotheses.csv` (t_ms,session,kind,words,accepted), `level.csv`, `trajectory.csv` (t_ms,cursor,move,score), `events.csv` automático (pausa/retoma/toque_palavra), `meta.txt` (proveniência). Só com texto idêntico a etapa0/textos.
- Fonte WAV de teste (só leitura) além do microfone; base da suíte "Integração de voz" (OPR-019).
- Padrões sem número inventado: limiar de nível 0 (desligado), vocabulário desligado, reconhecedor novo por sessão, preset Normal; animação 375 ms PROVISÓRIO (R5 sem padrão no PRD, OPR-022).
- Level.rms = RMS/32768 em [0,1].
- Qualquer mudança no manifesto (até declarar Activity, dataExtractionRules, tools:node remove) foi tratada como portão humano 1 com CIA. 16 KB reprovado na etapa0 não reprova a tarefa se diagnosticado e com OPR; recompilar = portão 6.

**Why:** PRD/TRD não fixam esses pontos; o orquestrador pediu critérios por comando e linha clara entre comando e aparelho.
**How to apply:** specs da 0004/0005/0006 e do app partem destes formatos e padrões; ver [[align-interpretacoes]] e [[parametros-provisorios]].
