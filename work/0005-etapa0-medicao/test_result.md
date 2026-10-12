# test_result 0005 (PARCIAL, Fase C interrompida pela segunda vez: aparelho saiu do adb em 2026-10-12 ~12:12, durante B7)

Aparelho: S23 SM-S911B RQCW80BPXKN, Android 16 (SDK 36), arm64-v8a, PAGE_SIZE 4096, MemTotal 7242456 kB.
Resultado parcial: nada reprovado. Faltam B7 (release instalado, reprodução não concluída), B8, G2 e C14. ATENÇÃO: o S23 está com os ajustes da sessão aplicados e com o APK de release instalado (ver OPR-044).

| Critério | Resultado | Evidência |
|---|---|---|
| Tarefa Q (10 scripts) | autotestes ok, bash -n ok, commit 1a0937e | scripts/ |
| A1 | PASSOU: BUILD SUCCESSFUL (--rerun-tasks), :app 228, :core 122, build-logic 27, testar.py 149/0 pulados | a1-*.txt |
| A2 | esperado OPR-024: reprova só libvosk armeabi-v7a e x86 (LOAD 4096), APK e AAB; zipalign -P 16 ok; rc=1 | a2-16k.txt |
| A3 | PASSOU: sha256 igual (cfd79ae2...502b) | a3-*.txt |
| A4 | aplicado (antes em aparelho-estado-antes.env); NÃO restaurado | aparelho-ajustes.txt |
| B1 | PASSOU: diálogo permissioncontroller, concedido por ui.py, modelo carregou, instalação 352 ms, prepare 659 ms, beginSession 30 ms, FATAL 0, 12 palavras 0 | b1-*.txt |
| B2 | PASSOU: só RECORD_AUDIO | b2-permissoes.txt |
| B3 | PASSOU: trilha de entrada do pid do app ativa a 16000 Hz, mono, PCM_16_BIT, fonte VOICE_RECOGNITION; mediana do level.csv 200,0 ms (n=558 intervalos, min 143, max 445, 111,8 s) | b3-audioflinger.txt |
| B5 | PASSOU: antes 22 arquivos, depois 0 novos; só os 5 de `exportacoes/` (csv/txt), nenhum arquivo de áudio fora de wav/; logcat: FATAL 0 e 12 palavras distintivas com 0 ocorrências (n=12 palavras, 2424 linhas do pacote). Ressalva: o nome da exportação já existia da 1ª passada, então "novos: 0" sozinho é fraco; vale a listagem final | b5-comparar.txt, b5-logcat.txt, b5-arquivos.txt |
| B6 | PASSOU: rotação para paisagem e volta, `sessão 2` antes e depois, Pausar continuou habilitado (leitura sem pausa); HOME: `rec stop` do pacote 12:07:39, 0 trilhas de entrada ativas; ao voltar, Ouvir habilitado (pausado) e a captura reabre por projeto (ReaderController onStart). Sessão depois da rotação lida com o painel reaberto (a rotação fecha o painel). Amostra: 1 rotação ida e volta, 1 HOME | b6-*.txt |
| B7 | PARCIAL: release instalado, sha256 do instalado = do arquivo (fb8326b9...ce40), não depurável; `reproduzir.py` falhou em "Normal não está na tela" (painel fora de posição) e em seguida o adb perdeu o aparelho; resultado do cursor NÃO medido | b7-*.txt |
| B8 (C11), C13 e números das ferramentas novas | NÃO EXECUTADOS: aguardam o merge APROVADO do revisor | |
| G2 | NÃO EXECUTADO (aparelho saiu do adb) | |
| C9 rotação/segundo plano | ver B6 | |
| C14 | NÃO EXECUTADO. A cada bloco o estado foi restaurado (bloco 1: ok; brilho 90 vs 104 em modo automático, tolerado). O bloco 2 terminou sem restauração | aparelho-ajustes.txt |
