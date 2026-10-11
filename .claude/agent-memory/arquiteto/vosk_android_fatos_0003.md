---
name: vosk-android-fatos-0003
description: Fatos medidos sobre vosk-android 0.3.75, JNA 5.19.1 e vosk-model-small-pt-0.3 no PLAN da 0003 (léxico com acento no Gr.fst, StorageService externo, 16 KB só nas 64 bits, ABIs da JNA, R8/AWT, log do Kaldi)
metadata:
  type: project
---

Medido em 2026-10-11 (fontes do `vosk-android-0.3.75-sources.jar`, zip do modelo, build real numa cópia):

- **Léxico**: o `vosk-model-small-pt-0.3` não tem `words.txt`; o léxico é a tabela de símbolos de entrada do `Gr.fst` (cabeçalho OpenFst, `flags=3`, 99.105 símbolos, offset 65). Grafia com acento (`versículo`, `não`, `joão`); `Token.spoken` não tem acento → ~10% das formas fora do léxico (inclui `nao`). Nenhuma palavra com hífen no léxico. Solução da 0003: lista = forma do TRD + grafias do léxico + `[unk]` (OPR-020, OPR-025).
- **StorageService.sync** copia para `getExternalFilesDir` (externo), compara só a 1ª linha do `uuid`, sem progresso. A 0003 usa cópia própria para `filesDir` (OPR-021).
- **16 KB**: `libvosk.so` arm64-v8a e x86_64 a 16384; armeabi-v7a e x86 a 4096. `libjnidispatch.so` 16384 em todas. Play exige 16 KB "on 64-bit devices" (prazo 2027-02-01). OPR-024 para decisão humana.
- **JNA 5.19.1 AAR** traz `libjnidispatch.so` também para armeabi, mips, mips64 → precisa `abiFilters` com as 4 ABIs do Vosk, senão o APK reprova a presença de `libvosk.so`.
- **R8**: a JNA referencia `java.awt.*`; sem `-dontwarn java.awt.**` o `minifyReleaseWithR8` falha.
- **Kaldi** imprime avisos com as palavras do vocabulário (texto do orador); `LogLevel.WARNINGS` (-1) ainda imprime; usar `LibVosk.vosk_set_log_level(-2)`.
- Referência do laço: bloco de 0,2 s (3.200 amostras), `AudioSource.VOICE_RECOGNITION` (6). `acceptWaveForm` true = fim de frase → `getResult()`; senão `getPartialResult()`.
- Zip do modelo: 32.453.112 bytes, SHA-256 `6e1ce909…95e93b7`; `vosk-android` 0.3.75 é o mais recente (pede jna 5.18.1).

**Why:** derrubam suposições do TRD (lista de vocabulário, StorageService, "16 KB em todas as ABIs") e do brainstorm (versão 0.3.47).

**How to apply:** no app (`app/`) e nas próximas tarefas da Etapa 0, partir destes fatos; refazer as medições ao trocar versão. Ver [[manifesto-core-compose-0003]], [[arquitetura-etapa0-app-0003]].
