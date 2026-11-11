# Retomada da 0005-etapa0-medicao

Escrito em 2026-10-12, no fim de uma sessão, para continuar depois sem perder contexto. Quem retomar (orquestrador): leia este arquivo, depois `spec.md`, `impl_plan.md` (seções "Runbook de execução" e "Correções da revisão rodada 1"), `code_review.md` (no worktree) e `test_result.md`.

## Onde o projeto está

Etapa 0 do Tribuna (decidir se Vosk + `align` servem antes de construir o app). Tarefas:

| Tarefa | Estado |
|---|---|
| 0001 contratos | mergeada na `main` (PR #2) |
| 0002 align | mergeada (PR #3) |
| 0003 vosk (app mínimo, exportação, manifesto, 16 KB) | mergeada (PR #4) |
| 0004 ferramentas Python (timestamps, alinhar, metricas) | mergeada (PR #5) |
| 0005 medição | **em andamento** (abaixo) |
| 0006 golden de transcrições | não aberta; depende das transcrições reais da 0005 (OPR-008) |

## 0005: o que está feito

- Spec e plano conferidos pelo orquestrador. Portão humano 6 decidido por Giorgio: **opção A** (pacote `vosk` em versão fixa num venv FORA do repositório e modelo grande de português, GPLv3, fora do repositório, SHA-256 registrado). **Nada foi instalado ainda**: só no passo D1, depois da S1.
- Branch `work/0005-etapa0-medicao` (árvore principal): spec, plano, scripts do QA (`scripts/`), evidências e `test_result.md` parcial. Commits até `4b1ed14`.
- Fase C no S23 (Galaxy S23, SM-S911B, Android 16, arm64, serial RQCW80BPXKN): passou. Entrada de áudio 16 kHz mono PCM16; mediana de bloco do `level.csv` 200,0 ms; privacidade sem vazamento; rotação e segundo plano; release com R8 (cursor 214 de 215 no T01 sintético); partida a frio (mediana 730 ms, n=10); primeira cópia do modelo (mediana 724 ms, n=3). Celular restaurado e APK de debug reinstalado (sha256 `cfd79ae2…502b`).
- As quedas do `adb` tinham como causa provável o **Auto Blocker** da Samsung (diagnóstico do humano). Giorgio o desativou. Confirmar estabilidade na S1 e fechar o OPR-044.

## 0005: o que está em andamento

Ferramentas novas de medição, no worktree `/Users/giorgioderico/Projetos/Tribuna-0005-ferramentas` (branch `work/0005-ferramentas`, criado de `work/0005-etapa0-medicao`):
`sincronizar.py`, `folha.py`, `veredito.py`, `nivel.py` (270 testes verdes, 145 quebras Q2, 144 vermelhas).

**Revisão rodada 1: REPROVADO** (`code_review.md` no worktree, commit `a728aae`):
1. Bloqueante: a marca SENSÍVEL AO DESLOCAMENTO não enxerga improviso, silêncio nem pulo, porque a folha de eventos é deslocada junto com a trajetória. Correção de desenho: **uma folha por corrida** (d−200, d, d+200), fixa no tempo do WAV. Afeta `medir.sh` do QA.
2. Bloqueante: o CS4 passa sem nada para checar. Precisa da mesma guarda do CS2 (nada para checar ⇒ NÃO MEDIDO).
3. Importantes: marca de sensibilidade some no Markdown; proveniência (sha256) sem teste; dois pontos do `nivel.py` sem teste.

**Correção de desenho: PRONTA** (arquiteto, commit `8ef6911` no worktree, seção "Correções da revisão rodada 1" do `impl_plan.md` do worktree, decisões C1 a C7, Tarefas 7 a 10 para o `motor-voz`, seção "Mudanças para o QA" Q-C1 a Q-C7). Nenhum portão humano. Resumo:
- **C1 (bloqueante 1):** folha de d fica como está; ferramenta nova `vizinhas.py` gera `<nome>.m200.folha.events.csv` e `<nome>.p200.folha.events.csv` a partir dela; `metricas.py` (0004) não muda; `veredito.py` confere proveniência, igualdade dos arquivos comuns, e o invariante `ler_eventos(folha_k, desl_k) == ler_eventos(folha_d, d)`. O procedimento antigo agora sai com código 2.
- **C2 (bloqueante 2):** CS4 ganha a guarda do CS2 (nada para checar ⇒ NÃO MEDIDO) e uma segunda guarda (cada intervalo de "sem movimento" exige movimento automático com t ≥ fim do intervalo). Só transforma PASSOU em NÃO MEDIDO.
- **OPR-047:** recomendação do arquiteto: lacuna declarada no relatório, não critério novo (decisão do analista/humano se quiserem critério).
- **Prova na cópia:** os dois casos do revisor agora dão SENSÍVEL AO DESLOCAMENTO; testes 270 → 325, 75 de 75 quebras novas vermelhas, 144 de 145 antigas vermelhas (S35 é o equivalente aceito).
- **OPR-048 (analista):** registrar no spec o texto de esclarecimento do E1.

## Próximos passos, em ordem

1. ~~Conferir a correção de desenho do arquiteto~~ (feito: sem portão humano; a correção não muda critério nem número do protocolo).
2. `motor-voz` executa as Tarefas 7 a 10 no worktree `/Users/giorgioderico/Projetos/Tribuna-0005-ferramentas` (teste antes, Q2 por operando e por constante; lição de todas as tarefas anteriores). **É aqui que a retomada começa.**
3. `revisor` rodada 2 (ferramentas). APROVADO → `git merge --no-ff work/0005-ferramentas` em `work/0005-etapa0-medicao`. O OPR-043..047 do revisor está no registro do worktree (conflito trivial de numeração com o OPR-042/043/044/045 da árvore principal: renumerar no merge).
4. `qa-publicacao`: aplicar as "Mudanças para o QA" nos scripts e fazer C11 (B8) e C13, que esperavam o merge.
5. **Humano, S1** (Giorgio, sala silenciosa, S23 no cabo, MacBook gravando o WAV com QuickTime, palma no começo de cada take; cerca de 2 h). O QA conduz.
6. D1: instalar `vosk` 0.3.44 (sha256 no plano) num venv fora do repo e baixar o modelo `vosk-model-pt-fb-v0.1.1-20220516_2113` (1,7 GB; ~10 GB livres no Mac) com SHA-256 registrado. Só depois da S1.
7. Folha, métricas e revisão de escuta da S1 (humano, 30 a 45 min), calibração por reprodução, S2 (segunda voz, consentimento por escrito pedido antes), S3 (salão com eco), bateria (45 min), relatório final em `etapa0/relatorios/etapa0-relatorio-final.md` e portão humano 3 (aprovar a arquitetura ou trocar o motor).

Tempo do humano estimado: 7 a 8 h em 4 ou 5 encontros, sem o tablet.

## Decisões humanas em aberto (não bloqueiam a medição)

- **OPR-024:** `libvosk.so` de 32 bits (armeabi-v7a, x86) vem a 4 KB; o `verify16kAlignment` reprova só por isso. Decidir antes da Play Store (versão mais nova do Vosk, recompilar ou só 64 bits).
- Textos propostos para o TRD (OPR-009; A5 vocabulário restrito e A6 instalação do modelo, da 0003) e para o TESTES-ETAPA0 (OPR-032, OPR-034).
- **OPR-047:** a retomada do T07 não é medida por nenhum critério (critério novo ou lacuna declarada).
- Portão condicional antes do push dos WAV: cota do Git LFS (~300 MB) e consentimento por escrito da segunda voz (`voz2`).
- Tablet: ainda não há; entra como lacuna declarada.

## Regras de operação que valem na retomada

- Orquestrador não implementa nem revisa. Spec e plano têm portão automático; portões humanos só os do `PROCESS.md`.
- Cada tarefa reprovou na revisão por guarda ou limiar sem teste: exigir ao construtor a varredura "quebrar cada operando e cada ocorrência de constante" antes de entregar.
- macOS: `timeout` não existe (usar `perl -e 'alarm N; exec @ARGV'`); `sed -i` com caminho relativo falhou (usar Edit); nunca encadear `&&` depois de `cp` que pode falhar.
- Telefone é pessoal: aplicar ajustes (avião, Não perturbe, brilho, tela ligada) só no começo de cada bloco e restaurar no fim de cada bloco (`scripts/aparelho.sh restaurar`; tolerar só o brilho em modo automático).
- Nunca abrir `~/.gradle/gradle.properties`. Não editar `.claude/settings.json` (o modo auto bloqueia; o humano aplica as regras de permissão).
- Limites de uso: agentes já caíram por limite de sessão e semanal; se um agente falhar por limite, ver o estado em disco e relançar só o que falta.
- Respostas ao humano em pt-br, caveman ultra; commits normais com `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

## Atualização (2026-10-14, fim da sessão)

- Ferramentas (Tarefas 7 a 10) mergeadas: merge `e3247f5` em `work/0005-etapa0-medicao`. Revisão rodada 2 APROVADA (`code_review.md`). OPR-043 a 046 do branch viraram 050 a 053 (`af9661d`).
- Scripts do QA (Q-C1 a Q-C7, C11 parcial sem aparelho): commits `7d5f6dc`, `4ba60fb`. Revisão dos scripts APROVADA (`code_review_scripts_qa.md`, `618122d`). OPR-054 e 055 abertos (`136caa7`).
- Roteiro da S1: `s1-roteiro.md` (`1f7fd2e`).
- **Próximo passo (outra sessão, com o S23 no cabo):** item 0 do roteiro: confirmar APK debug; C11 refeito com debug (OPR-054); C13 (F3); D23 (toque fora da tela); B7 com release. Depois, a S1 com o humano.
- Decisões humanas abertas: Pergunta 2 (cota LFS e consentimento da `voz2`), antes do push dos WAV. Menores do OPR-049 e OPR-054/055 não bloqueiam a S1.
