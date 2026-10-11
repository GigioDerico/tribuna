---
name: verificacoes-gradle-desenho
description: Decisões de arquitetura da 0001 — build-logic testado para verifyMergedManifest/verify16kAlignment, fake em testFixtures, aliases testDebugUnitTest, semântica do FakeSpeechEngine
metadata:
  type: project
---

- Verificações num build incluído `etapa0/build-logic` (plugin `tribuna.android.verifications`), com a lógica pura (manifesto, ELF, varredura do APK) testada em JUnit; a prova negativa ponta a ponta do manifesto usa um `app/src/debug/AndroidManifest.xml` temporário (script `etapa0/scripts/verificar-manifesto-negativo.sh`).
- `verifyMergedManifest` exige RECORD_AUDIO presente (evita passar em manifesto vazio, Q3); `verify16kAlignment` checa toda .so, não só as duas do Vosk (Q5).
- `FakeSpeechEngine` em `testFixtures` do `:core` (não vai ao APK); `FakeRecognizer` interno captura o SessionId; `hear*` é o caminho normal (descartado sem sessão), `recognizer.partial/final` é o callback atrasado. Uso fora de ordem lança IllegalStateException (decisão do fake, não do contrato).
- `./gradlew testDebugUnitTest` não alcança módulo JVM nem build incluído: há aliases no `:core` e na raiz.
- Achado aberto: OPR-001 (DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION com androidx.core na 0003).

**Why:** spec 0001 aprovado em 2026-10-10 (módulo JVM `:core` + `:app`); Q2/Q3/Q5 exigem verificação provada falhando.

**How to apply:** nas tarefas 0002/0003 e no app, reutilizar esse plugin e esse fake; não mover a lógica para scripts sem teste. Ver [[build-toolchain-etapa0]].
