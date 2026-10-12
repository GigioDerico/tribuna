# test_result 0005 (PARCIAL, Fase C interrompida: aparelho saiu do adb em 2026-10-12 ~11:27)

Aparelho: S23 SM-S911B RQCW80BPXKN, Android 16 (SDK 36), arm64-v8a, PAGE_SIZE 4096, MemTotal 7242456 kB.
Resultado parcial: nada reprovado; faltam B3 (fechamento do take), B5, B6, B7, B8, G2 e C14 (restaurar ajustes).

| Critério | Resultado | Evidência |
|---|---|---|
| Tarefa Q (10 scripts) | autotestes ok, bash -n ok, commit 1a0937e | scripts/ |
| A1 | PASSOU: BUILD SUCCESSFUL (--rerun-tasks), :app 228, :core 122, build-logic 27, testar.py 149/0 pulados | a1-*.txt |
| A2 | esperado OPR-024: reprova só libvosk armeabi-v7a e x86 (LOAD 4096), APK e AAB; zipalign -P 16 ok; rc=1 | a2-16k.txt |
| A3 | PASSOU: sha256 igual (cfd79ae2...502b) | a3-*.txt |
| A4 | aplicado (antes em aparelho-estado-antes.env); NÃO restaurado | aparelho-ajustes.txt |
| B1 | PASSOU: diálogo permissioncontroller, concedido por ui.py, modelo carregou, instalação 352 ms, prepare 659 ms, beginSession 30 ms, FATAL 0, 12 palavras 0 | b1-*.txt |
| B2 | PASSOU: só RECORD_AUDIO | b2-permissoes.txt |
| B3 | parcial: thread de entrada 16000 Hz, mono, PCM_16_BIT, fonte VOICE_RECOGNITION, trilha do pid do app ativa; mediana do level.csv NÃO medida (take não fechado) | b3-audioflinger.txt |
| B5-B8, G2, C11, C13 | NÃO EXECUTADOS | |
