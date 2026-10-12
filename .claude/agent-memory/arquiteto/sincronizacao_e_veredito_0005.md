---
name: sincronizacao-e-veredito-0005
description: Decisões de arquitetura do PLAN da 0005 — 4 ferramentas novas na própria tarefa, sincronização por Pearson em dB com palma no meio do patamar, veredito com sensibilidade/revisão/proveniência, reproduções com pausas reagendadas
metadata:
  type: project
---

Decidido em 2026-10-12 (`work/0005-etapa0-medicao/impl_plan.md`, A2–A6, A10):

- Ferramentas que mudam número (`sincronizar.py`, `folha.py`, `veredito.py`, `nivel.py`) ficam **dentro da 0005**, construídas pelo motor-voz num worktree e revisadas antes de qualquer número; não abrir 0005a. Scripts do QA (adb) também passam pelo revisor porque alguns fazem conta.
- **Sincronização:** Pearson em dB (piso 1/32768) entre `level.csv` e blocos de 200 ms do WAV; grade 100→10 ms; faixa padrão = WAV cobre a gravação. Surpresa: um estalo de palma cabe em vários blocos e dá patamar de ~180 ms; "primeiro máximo" ficava a −180 ms do limite → usar o **meio do patamar**. Deriva por metades, estendida à duração (conservador).
- **Veredito:** D9 em código testado; PASSOU em d e outro estado em ±200 ou motivo da sincronização → SENSÍVEL; sem "Revisão concluída: sim" → PENDENTE; sha256 de toda entrada conferido (Q8); `--sufixo/--config` reaproveita a lógica para as tabelas de calibração.
- **Reproduções:** nome `_r_` (ExportName), pausas oficiais reagendadas por adb no tempo do WAV, folha regenerada no relógio da reprodução, d medido (não suposto 0).
- Ajustes do aparelho restaurados a cada bloco (telefone pessoal do humano).

**Why:** o spec deixou ao plano ferramentas e divisão agente/humano; a prova (270 testes, 138/139 quebras vermelhas) validou.

**How to apply:** na 0006 (golden) e no app, reaproveitar `sinal.py`/`veredito.py`; ao mexer na sincronização, manter o meio do patamar e o teste da palma tardia. Ver [[medicao-aparelho-0005]], [[ferramentas-etapa0-0004]].
