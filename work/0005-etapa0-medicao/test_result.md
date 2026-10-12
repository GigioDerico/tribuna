# test_result 0005 (PARCIAL: Fase C retomada em 2026-10-12; B7, G2 e C14 feitos; faltam B8/C13 após o merge APROVADO)

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
| B7 (versão anterior, substituída pela linha B7 abaixo) | PARCIAL: release instalado, sha256 do instalado = do arquivo (fb8326b9...ce40), não depurável; `reproduzir.py` falhou em "Normal não está na tela" (painel fora de posição) e em seguida o adb perdeu o aparelho; resultado do cursor NÃO medido | b7-*.txt |
| B8 (C11), C13 e números das ferramentas novas | NÃO EXECUTADOS: aguardam o merge APROVADO do revisor | |
| G2 | NÃO EXECUTADO (aparelho saiu do adb) | |
| C9 rotação/segundo plano | ver B6 | |
| C14 | NÃO EXECUTADO. A cada bloco o estado foi restaurado (bloco 1: ok; brilho 90 vs 104 em modo automático, tolerado). O bloco 2 terminou sem restauração | aparelho-ajustes.txt |

## Retomada 2026-10-12 (blocos pequenos; ajustes aplicados no começo e restaurados no fim de cada bloco)

| Critério | Resultado | Evidência |
|---|---|---|
| B7 | PASSOU (informativo quanto à voz: sintética Luciana, n=1 reprodução): com o release instalado (pm sem DEBUGGABLE, sha256 fb8326b9...ce40), `reproduzir.py --ensaio` na fonte WAV (meta: audio_source=wav, preset=Normal, vocabulary=off, NewPerSession) carregou o modelo (prepare 426 ms) e terminou com cursor 214 = último token do T01 (215 tokens; 148 linhas de trajectory.csv). Debug reinstalado e sha256 conferido: cfd79ae2...502b igual ao arquivo | qa-b7-release.txt |
| G2 partida a frio, modelo já copiado (n=10) | registrado, sem meta numérica no spec: TotalTime mediana 730 ms, máximo 852 ms; prepare mediana 301,5 ms, máximo 526 ms; instalação mediana 1 ms, máximo 30 ms | qa-g2-quente.log/.tsv |
| G2 primeira cópia (n=3, `pm clear` não foi bloqueado) | TotalTime mediana 724 ms, máximo 730; prepare mediana 520, máximo 643; instalação (install_ms) mediana 273, máximo 334 | qa-g2-primeira.log/.tsv |
| C14 | PASSOU com tolerância: estado final = estado de antes, exceto brilho 77 contra 104 (modo automático, tolerado). Debug instalado (cfd79ae2...), permissão de microfone concedida | qa-c14-final.txt, aparelho-ajustes.txt |
| B8 (C11), C13, números de sincronizar/folha/veredito/nivel | NÃO EXECUTADOS por instrução: aguardam o merge APROVADO e "Mudanças para o QA" | |

Observações: o Não perturbe só pegou com um segundo `cmd notification set_dnd none` (o `aparelho.sh aplicar` acusou falha e foi ignorado, pois o item não afeta B7/G2). No G2 o "1º parcial" apareceu em 2 de 13 corridas (ruído de ambiente, sem fala): não é métrica. Mudei `scripts/ui.py` (OPR-045: rolagem nos dois sentidos). Resultado parcial geral da 0005 continua pendente de B8/C13 e do merge.
