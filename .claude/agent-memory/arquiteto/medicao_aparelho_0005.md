---
name: medicao-aparelho-0005
description: Fatos medidos no PLAN da 0005 sobre o S23, o adb/uiautomator, o macOS (say, afconvert, zsh) e o vosk do PyPI que moldam as medições da Etapa 0
metadata:
  type: project
---

Medido em 2026-10-12 (`work/0005-etapa0-medicao/impl_plan.md`, A1, A7, A12, A16):

- **S23 (SM-S911B, Android 16, SDK 36):** `getconf PAGE_SIZE` = 4096 → 16 KB não é exercitado em execução, só estaticamente. Tela costuma estar bloqueada e o aparelho saiu do adb no meio do PLAN: todo passo de aparelho começa por checar `adb get-state` e pedir desbloqueio.
- **`uiautomator dump` leva ~2,3 s** (4 medidas) → "≥ 1 amostra/s do painel" é impossível sem instalar nada; com o nível mudando a cada 200 ms pode dar "could not get idle state" (plano B: `screencap` lido pelo agente, fora do repo).
- `cmd connectivity airplane-mode`, `cmd notification set_dnd`, `settings put`, `svc power stayon usb` existem por adb; `pm clear` apaga também `/sdcard/Android/data/<pkg>` (WAVs e exportações).
- **macOS:** `say` tem vozes pt_BR (Luciana etc.); `afconvert -f WAVE -d LEI16@16000 -c 1 --mix` gera WAV com bloco `FLLR` que o `wave` e o `WavHeader` da 0003 aceitam. No **zsh** `$S` com espaços não vira palavras → scripts em bash.
- **vosk PyPI:** 0.3.45 (a mais nova) **não tem wheel macOS**; usar 0.3.44 universal2 (sha256 029d0b3d…882801). `import vosk` puxa requests/srt/tqdm. Modelo grande pt: `vosk-model-pt-fb-v0.1.1-20220516_2113` (1.693.530.883 bytes, GPLv3).
- **App da 0003:** `level.csv` carimba o bloco na chegada ao controlador; origem do relógio = fim do `startCapture`; `ExportName` só aceita `^ID(_[a-z0-9]+)*$` (sem hífen); limiar não filtra Final (`isFinal || lastLevel >= threshold`).
- **Prova Q2 em Python:** `.pyc` velho (mtime em segundos + tamanho) mantém o mutante depois de restaurado → rodar quebras com `PYTHONDONTWRITEBYTECODE=1 python3 -B` e apagar `__pycache__`. Arquivo não versionado não volta com `git checkout`: commitar a cópia antes das quebras.

**Why:** cada item derrubou uma suposição do spec ou do PLAN (G1, D20, versão do vosk, restauração do aparelho).

**How to apply:** em planos com aparelho ou ferramentas Python, partir destes fatos e remedir ao trocar aparelho/versão. Ver [[vosk-android-fatos-0003]], [[prova-do-plano-em-copia]], [[ferramentas-etapa0-0004]].
