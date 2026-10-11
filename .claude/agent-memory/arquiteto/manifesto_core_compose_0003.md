---
name: manifesto-core-compose-0003
description: Fatos medidos na CIA da 0003 sobre DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION do androidx.core, regras de backup e o teto de compileSdk 36 para Compose/lifecycle
metadata:
  type: project
---

Medido em 2026-10-11 numa cópia de etapa0 (CIA 0003; portão 1 aprovado pelo humano em 2026-10-11, opção A):

- androidx.core 1.18.0 injeta `<permission signature>` E `<uses-permission>` de `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. `tools:node="remove"` nos dois funciona (debug, release e bundle; aapt2 mostra só RECORD_AUDIO).
- `ContextCompat.registerReceiver` com RECEIVER_NOT_EXPORTED (4) no API 26–32 lança RuntimeException se a permissão não estiver no app. No grafo Compose+Vosk o único chamador é o `MediaQuery` do compose-ui (DOCK_EVENT, flag 2 = EXPORTED), que não depende dela.
- `verifyMergedManifest` só lê `uses-permission*`; `<permission>` passava despercebido. A CIA propõe endurecer.
- Seção de backup vazia/ausente = tudo liberado; `allowBackup=false` não desliga device-transfer no 12+. Lint exige `fullBackupContent` também (minSdk 26).
- Compose 1.12.x (BOM 2026.08.00+) e lifecycle 2.11.0 exigem compileSdk 37. Teto com 36: BOM 2026.06.01, lifecycle 2.10.0, activity-compose 1.13.0.

**Why:** isso derruba a suposição do D26 ("versão estável mais recente") e da redação do TRD ("regras vazias").

**How to apply:** no PLAN da 0003 e do app, fixar essas versões; refazer a varredura de registerReceiver a cada troca de versão. Ver [[prova-do-plano-em-copia]], [[build-toolchain-etapa0]].
