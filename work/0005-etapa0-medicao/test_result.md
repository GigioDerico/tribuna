**Resultado: PARCIAL (nada reprovado; nenhuma gravação oficial existe). Não é PASSOU nem FALHOU: C a H dependem da S1, que ainda não aconteceu.**

# Testes — 0005-etapa0-medicao

**Responsável:** qa-publicacao · **Data:** 2026-10-14 (reescrito a partir de `progresso.md`; medições no aparelho de 2026-10-11 e 2026-10-12) · **Commit testado:** `1a0937e` (build e Gradle de A1/A2, medido); HEAD da auditoria `ad8c51b`. O código do app e do `core` não mudou desde o Gradle (`git diff main --stat -- etapa0/core/src/main etapa0/app docs/` vazio, conferido na auditoria).

Fonte do estado de cada critério: `work/0005-etapa0-medicao/progresso.md` (auditoria por evidência, commit `621ef53`). Evidências em `work/0005-etapa0-medicao/evidencias/` (abreviado `ev/`).

Relatório final: `etapa0/relatorios/etapa0-relatorio-final.md` existe só como esqueleto (rascunho, sem medição final). Esta tabela aponta para ele; os números dele virão da S1, S2, S3 e da bateria.

## Seções pendentes da S1

Tudo o que depende da S1 está PENDENTE: B4, C1 a C3, D1 a D2, D4 a D6, E1 a E3, F1, F2, F4 a F6, G1, G3, G5, H1, H2 e o fechamento de H4 e H5. Sem WAV, trajetória nem folha reais, nenhum número de CS1 a CS4 existe. Os ensaios sintéticos em `ev/qc/` e `ev/b8-*` não valem como medição oficial.

## Ambiente

| Item | Valor |
|---|---|
| Aparelho(s) | Samsung S23 SM-S911B (RQCW80BPXKN), Android 16 (SDK 36), arm64-v8a, PAGE_SIZE 4096, MemTotal 7242456 kB (`ev/a3-aparelho.txt`). Rótulo para metas: "aparelho topo de linha". Aparelho de entrada, intermediário e tablet: não existem |
| Ambiente acústico | Nenhum take oficial. Ensaios B7 e B8 por WAV sintético (reprodução pela fonte WAV) e G2 sem fala |
| Voz(es) | Só sintética (macOS `say`, voz pt_BR Luciana, `ev/b8-say-vozes.txt`). Voz do humano (S1), `voz2` (S2): não gravadas |
| Distância do microfone | Não aplicável até a S1 (~50 cm no protocolo) |
| Brilho / modo avião | Ajustes aplicados no começo e restaurados no fim de cada bloco (`ev/aparelho-ajustes.txt`, `ev/aparelho-estado-antes.env`); brilho em modo automático diverge (77 contra 104), tolerado |

## Execução

| Suíte / comando | Resultado | Evidência |
|---|---|---|
| Gradle (build, testes, lint, manifesto, assemble), `--rerun-tasks` | BUILD SUCCESSFUL in 59s, 144 tasks (2026-10-12, `1a0937e`) | `ev/a1-gradle.txt`, `ev/a1-commit.txt` |
| `python3 etapa0/ferramentas/testar.py` | 329 testes, 0 falhas, 0 erros, 0 pulados (reexecutado na auditoria de 2026-10-14; era 149/0 pulados em `ev/a1-testar-base.txt`) | `ev/a1-testar-base.txt`, `ev/b-testar.txt`, `progresso.md` §4 |
| `./gradlew verify16kAlignment` (release) | rc=1, esperado OPR-024: reprova só `libvosk.so` de armeabi-v7a e x86 (LOAD 4096), APK e AAB; `zipalign -P 16` ok | `ev/a2-16k.txt` |
| `./gradlew connectedDebugAndroidTest` | Não rodado nesta tarefa (a 0005 é de medição; não está no spec) | |

## Critérios de aceite

Estados copiados de `progresso.md`. FEITO = tem evidência; PARCIAL = falta parte; ABERTO = sem evidência.

| Critério | Como foi executado | Resultado | Evidência |
|---|---|---|---|
| A1 Build, testes, lint, manifesto | Gradle com `--rerun-tasks` e `testar.py` | FEITO (citar `1a0937e` e 329 testes no relatório) | `ev/a1-gradle.txt`, `ev/a1-commit.txt`, `ev/a1-testar-base.txt` |
| A2 `verify16kAlignment` no release | Gradle; saída real anexada | FEITO (resultado esperado do OPR-024, não reprova a 0005) | `ev/a2-16k.txt` |
| A3 Instalação, sha256, getprop | `adb install`, sha256 do APK instalado | FEITO (cfd79ae2…502b igual) | `ev/a3-sha256.txt`, `ev/a3-install.txt`, `ev/a3-aparelho.txt` |
| A4 Ajustes antes/depois restaurados | `aparelho.sh` aplicar/restaurar por bloco | PARCIAL: falta restauração final depois da S1 e do último bloco | `ev/aparelho-estado-antes.env`, `ev/aparelho-ajustes.txt`, `ev/qa-c14-final.txt` |
| B1 Diálogo de microfone, modelo pronto | Execução no S23 | FEITO (instalação 352 ms, prepare 659 ms, beginSession 30 ms, FATAL 0) | `ev/b1-dialogo.txt`, `ev/b1-painel.txt`, `ev/b1-logcat.txt` |
| B2 Manifesto instalado só com RECORD_AUDIO | `dumpsys package` | FEITO | `ev/b2-permissoes.txt` |
| B3 Trilha 16 kHz, mono, PCM16 | `dumpsys media.audio_flinger`; mediana do `level.csv` 200,0 ms (n=558 intervalos, min 143, max 445, 111,8 s) | FEITO (a mediana está só neste arquivo, sem saída própria) | `ev/b3-audioflinger.txt` |
| B4 JSON real sem `{ } :` nem aspas; `final` com palavras em cada `hypotheses.csv` | Checagem sobre os `hypotheses.csv` da S1 | ABERTO (só um ensaio de 2026-10-12, fora do repositório) | nenhuma |
| B5 Privacidade: sem áudio novo; logcat sem palavras distintivas | Antes/depois da listagem de arquivos; logcat com 12 palavras (n=12; 2424 linhas do pacote) | FEITO (ressalva: "novos: 0" é fraco porque o nome já existia; repetir numa leitura da S1) | `ev/b5-comparar.txt`, `ev/b5-arquivos.txt`, `ev/b5-logcat.txt`, `ev/privacidade/` |
| B6 HOME pausa e solta o microfone; rotação mantém leitura e SessionId | 1 rotação ida e volta, 1 HOME (n pequeno) | FEITO (`sessão 2` antes e depois; `rec stop` e 0 trilhas ativas) | `ev/b6-audio.txt`, `ev/b6-sessao-antes.txt`, `ev/b6-sessao-depois.txt` |
| B7 Release com R8: reprodução do T01 termina no último token | `reproduzir.py --ensaio`, fonte WAV, release instalado, depois debug reinstalado | FEITO (cursor 214 de 215 tokens, 148 linhas de trajectory.csv; sha256 release fb8326b9…ce40; ressalva: voz sintética, n=1) | `ev/qa-b7-release.txt`, `ev/b7-instalado-release.txt` |
| B8 Ensaio D21 e teste D23 | Reprodução sintética como WAV; ao vivo | PARCIAL: D21 reprodução SINCRONIZADO (d=0 ms, correlação 0,998, deriva 0); ao vivo SENSÍVEL (d=-970 ms, sem palma, informativo). Faltam C11 refeito com o debug (o `level.csv` veio do release, OPR-054) e D23 (não existe `ev/b8-toque.txt`) | `ev/b8-sinc-reproducao.txt`, `ev/b8-sinc-ao-vivo.txt`, `ev/b8-say-vozes.txt` |
| C1 Conjunto completo de arquivos por take (S1, S2, S3) | | ABERTO (`etapa0/gravacoes`, `trajetorias`, `eventos` vazias) | nenhuma |
| C2 WAV 16 kHz mono PCM16, nome padrão, `meta.txt` | | ABERTO (não há WAV) | nenhuma |
| C3 `git lfs ls-files` lista os WAV; push só após a Pergunta 2 | | ABERTO (`.gitattributes` já tem `*.wav` no LFS; hoje o LFS lista só o modelo pequeno) | nenhuma |
| D1 `vosk` em venv fora do repo, modelo grande com SHA-256, `words.csv` | | ABERTO (autorização opção A registrada no spec; só depois da S1) | nenhuma |
| D2 `expected.csv` por gravação via `alinhar.py` | | ABERTO (`etapa0/esperado` vazia) | nenhuma |
| D3 Ferramentas de sincronização e folha, com testes, revisão APROVADA e Q2 | `testar.py` (329/0/0); quebras Q2 locais, desfeitas | FEITO (`code_review.md` e `code_review_scripts_qa.md` com veredito APROVADO; OPR-049 e OPR-055 não bloqueiam) | `ev/b-quebras-q2.txt`, `ev/b2-quebras-q2.txt`, `ev/b-imports.txt`, `ev/b-testar.txt` |
| D4 Deslocamento, pico da palma, deriva por gravação | | ABERTO (só amostra do ensaio B8, não é gravação oficial) | `ev/b8-sinc-reproducao.txt` (ensaio) |
| D5 `folha.events.csv` para T04, T05, T06, L30 | | ABERTO (ferramenta pronta; só o ramo de folha inválida ensaiado) | `ev/qc/folha-invalida-rc2.txt` (ensaio) |
| D6 `revisao.md` por gravação | | ABERTO (escuta humana) | nenhuma |
| E1 `metricas.py` por gravação, relatório e JSON, corridas ±200 ms | | ABERTO (só ensaios sintéticos; `etapa0/relatorios` vazia) | `ev/qc/` (ensaios, não oficial) |
| E2 Resumo por gravação e critério (PASSOU/REPROVOU/NÃO MEDIDO/SENSÍVEL) | | ABERTO (`veredito.py` ensaiado em dados sintéticos) | nenhuma |
| E3 Salto errado com classe e resposta da revisão | | ABERTO | nenhuma |
| F1 Presets Conservador/Normal/Ágil | | ABERTO | nenhuma |
| F2 Vocabulário ligado × desligado | | ABERTO | nenhuma |
| F3 Estratégia novo × `reset()`, 10 ciclos cada | | ABERTO (C13 não executado; `ev/f3-estrategia.md` não existe; só `beginSession` de 24 a 36 ms do ensaio de G2) | `ev/qa-g2-quente.log` (só o ensaio) |
| F4 Limiar de áudio | | ABERTO (`nivel.py` pronto, sem dados) | nenhuma |
| F5 Provisórios fora do painel; OPR-025 | | ABERTO | nenhuma |
| F6 Configuração recomendada ≠ oficial | | ABERTO | nenhuma |
| G1 Latências 1, 2, fala→cursor, total, 3 e `beginSession` por gravação da S1 | | ABERTO (o `.tsv` do painel é do ensaio sintético) | `ev/painel/T01_ensaio_sala_t1_r_normal_off_novo_l0000.tsv` (ensaio) |
| G2 Partida a frio: 10 corridas e 3 de primeira cópia | `am force-stop` + `am start -W`; `pm clear` | FEITO. Quente (n=10): TotalTime mediana 730 ms, máx 852; prepare mediana 301,5, máx 526. Primeira cópia (n=3): TotalTime mediana 724, máx 730; install_ms mediana 273, máx 334. Sem meta numérica no spec; "1º parcial" em 2 de 13 corridas é ruído, não métrica | `ev/qa-g2-quente.log` e `.tsv`, `ev/qa-g2-primeira.log` e `.tsv` |
| G3 Memória a cada 60 s por ≥ 45 min | | ABERTO (`memoria.sh` ainda não rodou em sessão real) | nenhuma |
| G4 Bateria ≥ 45 min desconectado | | ABERTO (depende do humano desconectar o cabo) | nenhuma |
| G5 Custo do alinhamento no L30 | | ABERTO | nenhuma |
| H1 `etapa0-relatorio-final.md` com os 8 critérios de saída | | ABERTO (só esqueleto, sem números) | `etapa0/relatorios/etapa0-relatorio-final.md` |
| H2 Seções do relatório | | ABERTO (seções existem no esqueleto, sem conteúdo medido) | idem |
| H3 Este `test_result.md` | Reescrito a partir de `progresso.md` | PARCIAL: atualizado hoje, falta incluir B8/C13/D23 e as rodadas, e apontar para o relatório com números | este arquivo |
| H4 `git status` só com pastas permitidas; nada do modelo grande nem venv | `git status --short`, `git ls-files`, `git diff main --stat` das três pastas | PARCIAL: na auditoria só havia arquivos de memória de agente e o diff das três pastas era vazio; é critério de fim de tarefa, repetir ao fechar | `progresso.md` §2 e §4 |
| H5 Achados novos no `registers/opr_register.md`; OPR medidos atualizados | Leitura do registro | PARCIAL: achados novos (OPR-044, 045, 047, 049, 050 a 055) com dono; linhas dos OPR a medir (007b, 010, 011, 013, 015, 020, 024, 025, 033) ainda "aberto até a 0005" | `registers/opr_register.md` |

Critérios sem linha própria acima: C9 rotação/segundo plano (ver B6) e C14 (restauração do aparelho): PASSOU com tolerância, estado final igual ao de antes exceto brilho 77 contra 104 em modo automático (`ev/qa-c14-final.txt`, `ev/aparelho-ajustes.txt`).

## Métricas da Etapa 0 (quando aplicável)

Regra de contagem (Q7): amostras a cada 100 ms; atraso em tokens; excluídos improviso, silêncio, pausa e 3 s após pulo.

Nenhuma gravação oficial foi medida. Contagem de amostras de leitura normal: 0. Nenhum número de atraso, salto, recuperação ou latência por gravação existe; métrica sobre zero amostras não aprova nada.

| Gravação | Amostras de leitura normal | Atraso ≤ 3 | Saltos errados | Recuperação de pulo | Latência total |
|---|---|---|---|---|---|
| Nenhuma (aguarda S1) | 0 | NÃO MEDIDO | NÃO MEDIDO | NÃO MEDIDO | NÃO MEDIDO |

## Teste falso (Q2)

- Ferramentas da 0005 (D3): quebras locais propositais nos scripts (S01–S30, N09–N16 e outras, mais as de `b2`) deixaram testes vermelhos e foram desfeitas. Evidência em `ev/b-quebras-q2.txt` e `ev/b2-quebras-q2.txt`. Nenhum teste novo de app foi escrito na 0005.

## Não testado

- S1, S2 (segunda voz) e S3 (salão com eco): não aconteceram. Dependem do humano (e de achar a voz2 e o salão, sem data). Bloqueiam B4, C, D, E, F, G1, G3, G5 e o relatório final.
- Bateria (G4): precisa do humano para desconectar o cabo por ≥ 45 min.
- B8: C11 refeito com o debug (OPR-054) e D23 (toque em palavra fora da tela). C13/F3 (novo × `reset()`): podem rodar com o S23 no cabo, não foram executados.
- D1: `vosk` 0.3.44 e modelo grande em venv fora do repositório, só depois da S1.
- Aparelho de entrada, aparelho intermediário e tablet: não existem; vão como lacunas ao relatório.
- Escuta de revisão (D6, E3) e decisões humanas OPR-022, 024, 032, 033, 034 e 047 para o portão 3.
- Pergunta 2 (cota LFS e consentimento por escrito da `voz2`): antes do push dos WAV.
- Restauração final dos ajustes do aparelho (A4): fim da tarefa. OPR-044 (Auto Blocker) a fechar se a S1 correr sem queda de adb.
- Nada foi aprovado nesta atualização. Nenhum comando de aparelho foi rodado ao reescrever este arquivo.
