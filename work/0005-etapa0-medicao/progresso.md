# Progresso da 0005-etapa0-medicao (auditoria por evidência)

Data da auditoria: 2026-10-14. Branch `work/0005-etapa0-medicao`, HEAD `ad8c51b`. Auditor: qa-publicacao. Isto não é veredito de aprovação: é o estado dos critérios A1 a H5 do `spec.md`. Evidências em `work/0005-etapa0-medicao/evidencias/` (abreviado `ev/` abaixo). Nada de aparelho, adb, áudio, Vosk ou modelo foi rodado nesta auditoria.

## 1. Resumo em contagens (40 critérios)

| Seção | FEITO | PARCIAL | ABERTO | NÃO SE APLICA | Total |
|---|---|---|---|---|---|
| A. Base e instalação | 3 | 1 | 0 | 0 | 4 |
| B. Verificações no aparelho | 6 | 1 | 1 | 0 | 8 |
| C. Gravações | 0 | 0 | 3 | 0 | 3 |
| D. Verdade-terreno e sincronização | 1 | 0 | 5 | 0 | 6 |
| E. Métricas e veredito | 0 | 0 | 3 | 0 | 3 |
| F. Calibração e decisões | 0 | 0 | 6 | 0 | 6 |
| G. Medições para as metas | 1 | 0 | 4 | 0 | 5 |
| H. Relatório final e entrega | 0 | 3 | 2 | 0 | 5 |
| **Total** | **11** | **5** | **24** | **0** | **40** |

Leitura: todo o trabalho sem humano e sem gravação real está feito (base, aparelho, ferramentas). Nenhuma gravação oficial existe: `etapa0/{gravacoes,trajetorias,eventos,esperado,relatorios}/` estão vazias. Tudo de C a H (exceto D3 e G2) depende da S1.

## 2. Tabela

Siglas de bloqueio: H = humano (S1, S2, S3, bateria, escuta); Ag = agente (qa-publicacao) com aparelho no cabo; dep = depende de critério anterior.

| ID | Descrição curta | Estado | Evidência | O que falta | Quem bloqueia |
|---|---|---|---|---|---|
| A1 | Build, testes, lint, manifesto, assemble verdes; `testar.py` sem skip; commit medido | FEITO | `ev/a1-gradle.txt` (BUILD SUCCESSFUL in 59s, 144 tasks), `ev/a1-commit.txt` (1a0937e), `ev/a1-testar-base.txt` (149/0 pulados). Reexecutei hoje `python3 etapa0/ferramentas/testar.py`: 329 testes, 0 falhas, 0 pulados. `git diff main --stat -- etapa0/core/src/main etapa0/app docs/` vazio (app não mudou desde o Gradle) | Nada. O relatório final deve citar o commit medido (Q8) e a contagem atual (329), não a de `a1-testar-base.txt` | nenhum |
| A2 | `verify16kAlignment` no release, saída anexada, esperado OPR-024 | FEITO | `ev/a2-16k.txt` (rc=1; reprova só `base/lib/x86/libvosk.so` e armeabi-v7a a 4096) | Levar ao relatório final (H2, seção 16 KB) com caminhos a–d do OPR-024 | decisão humana OPR-024 (não bloqueia a 0005) |
| A3 | Instalação do debug, sha256 igual, getprop/PAGE_SIZE/MemTotal | FEITO | `ev/a3-sha256.txt` (cfd79ae2…502b igual), `ev/a3-install.txt`, `ev/a3-aparelho.txt` (SM-S911B, Android 16, arm64-v8a, 4096, 7242456 kB) | Nada. Reconferir o sha256 do debug no item 0 do `s1-roteiro.md` (o release foi o último instalado, depois reinstalado o debug em `ev/qa-b7-release.txt`) | nenhum |
| A4 | Ajustes do aparelho antes/depois, todos restaurados ao fim da tarefa | PARCIAL | `ev/aparelho-estado-antes.env`, `ev/aparelho-ajustes.txt`, `ev/qa-c14-final.txt` (restaurado, exceto brilho 77 contra 104 em modo automático, tolerado) | Restauração final depois da S1 e do último bloco (S2, S3, bateria). O critério diz "ao fim da tarefa", que não chegou. Brilho em modo automático diverge do valor numérico de antes | Ag (a cada bloco); fim da tarefa |
| B1 | Diálogo de microfone, modelo pronto, `install_ms`/`prepare_ms`, sem FATAL | FEITO | `ev/b1-dialogo.txt` (diálogo permissioncontroller), `ev/b1-painel.txt` (instalação 352 ms, prepare 659 ms, beginSession 30 ms), `ev/b1-logcat.txt` (FATAL 0) | Nada | nenhum |
| B2 | Manifesto instalado só com `RECORD_AUDIO` | FEITO | `ev/b2-permissoes.txt` (só `android.permission.RECORD_AUDIO`, INTERNET/WAKE_LOCK/NETWORK_STATE: 0 linhas, granted=true) | Nada. Observação: `ev/b2-verificacao.txt` e `ev/b2-quebras-q2.txt` são da revisão das ferramentas, não deste critério | nenhum |
| B3 | Trilha de entrada 16 kHz, 1 canal, PCM16; mediana do `level.csv` | FEITO | `ev/b3-audioflinger.txt` (16000 Hz, mono, PCM_16_BIT, VOICE_RECOGNITION); `test_result.md` registra mediana 200,0 ms (n=558 intervalos) | Nada. A mediana está só no `test_result.md`, não num arquivo de saída próprio (ressalva menor) | nenhum |
| B4 | JSON real: nenhuma palavra com `{ } :` ou aspas, nem `partial/text/result`, e `final` com palavras em cada `hypotheses.csv` da S1 | ABERTO | Nenhuma. Só o ensaio de 2026-10-12 existiu e está fora do repositório | Rodar a checagem em todo `hypotheses.csv` da S1 (passo de fechamento do `s1-roteiro.md`) | H (S1) |
| B5 | Privacidade: sem arquivo de áudio novo; logcat sem 10 palavras distintivas | FEITO | `ev/b5-comparar.txt` (novos: 0), `ev/b5-arquivos.txt`, `ev/b5-logcat.txt` (12 palavras, 0 ocorrências, 2424 linhas do pacote, FATAL 0), `ev/b1-logcat.txt`, `ev/privacidade/` | Nada para o critério. Ressalva do próprio `test_result.md`: o nome da exportação já existia, então "novos: 0" é fraco; vale repetir antes/depois numa leitura da S1 (barato) | nenhum |
| B6 | HOME pausa e solta o microfone; rotação mantém leitura e `SessionId` | FEITO | `ev/b6-audio.txt` (`rec stop` do pacote, 0 trilhas ativas), `ev/b6-sessao-antes.txt` e `ev/b6-sessao-depois.txt` (`sessão 2` nos dois) | Nada. Amostra: 1 rotação, 1 HOME (n pequeno; o spec não pede mais) | nenhum |
| B7 | Release com R8: reprodução do T01 pela fonte WAV termina no último token; debug reinstalado | FEITO | `ev/qa-b7-release.txt` (cursor 214 de 215 tokens, 148 linhas de `trajectory.csv`, meta `audio_source=wav`, `pm` sem DEBUGGABLE, debug reinstalado com sha256 igual); `ev/b7-instalado-release.txt` (sha256 do release fb8326b9…ce40) | Nada pelo texto do critério. Ressalva: voz sintética (Luciana), n=1; o `s1-roteiro.md` item 5 prevê refazer com a gravação real se necessário | nenhum |
| B8 | Ensaio D21 (sintético como WAV dá deslocamento 0 ± 200 ms) e teste D23 (toque em palavra fora da tela) | PARCIAL | `ev/b8-say-vozes.txt` (há voz pt_BR), `ev/b8-sinc-reproducao.txt` (SINCRONIZADO, d = 0 ms, correlação 0,998, palma confere, deriva 0), `ev/b8-sinc-ao-vivo.txt` (SENSÍVEL, d = -970 ms, sem palma: era o ensaio sem palma, informativo) | (1) C11 refeito com o APK debug: o `level.csv` do `b8-sinc-reproducao.txt` veio do release (OPR-054, achado 1). (2) D23: não existe `ev/b8-toque.txt` | Ag com aparelho no cabo (item 0 do `s1-roteiro.md`); OPR-054 |
| C1 | Conjunto completo de arquivos por take das S1, S2, S3 | ABERTO | Nenhuma. `etapa0/gravacoes`, `trajetorias`, `eventos` vazias | Todas as gravações; script de conjuntos faltantes | H (S1, S2, S3) |
| C2 | WAV 16 kHz mono PCM16, nome padrão, `meta.txt` com D10 | ABERTO | Nenhuma (não há WAV) | Gravar e converter com `afconvert`; conferir cabeçalhos e metas | H (S1, S2, S3); dep C1 |
| C3 | `git lfs ls-files` lista os WAV; tamanho registrado; push só após a Pergunta 2 | ABERTO | Nenhuma. `.gitattributes` já tem `*.wav` no LFS (o LFS lista hoje só o modelo em `app/src/main/assets/model-pt/`) | WAV; resposta à Pergunta 2 (cota LFS ~300 MB e consentimento por escrito da `voz2`) | H (Pergunta 2); dep C1 |
| D1 | `vosk` em venv fora do repo e modelo grande com SHA-256; `words.csv` por gravação | ABERTO | Nenhuma. Autorização (opção A) registrada no `spec.md`. `git ls-files` não mostra venv nem `vosk-model-pt-fb` (conferido hoje) | Instalar `vosk` 0.3.44 e baixar o modelo (~1,7 GB, ~10 GB livres) só depois da S1; gerar os `words.csv` | Ag, depois da S1 (portão 6 já decidido, opção A); dep C2 |
| D2 | `expected.csv` por gravação via `alinhar.py`, stderr anexado | ABERTO | Nenhuma (`etapa0/esperado` vazia) | Gerar com os `words.csv` | Ag; dep D1 |
| D3 | Ferramentas de sincronização e folha na stdlib, com testes, revisão APROVADA e quebra Q2 | FEITO | `etapa0/ferramentas/{sincronizar,folha,vizinhas,veredito,nivel}.py`; `code_review.md` (Veredito: APROVADO, rodada 2); `code_review_scripts_qa.md` (APROVADO); `ev/b-quebras-q2.txt` (quebras S01–S30, N09–N16 e outras, todas vermelhas, restauradas), `ev/b2-quebras-q2.txt`; `ev/b-imports.txt` (só stdlib e módulos do projeto). Reexecutei hoje `testar.py`: 329 testes, 0 falhas | Nada. Pendências do OPR-049 (lacunas de teste menores) e OPR-055 não bloqueiam | nenhum |
| D4 | Por gravação: deslocamento, pico da palma no WAV e no `level.csv`, deriva | ABERTO | Só a amostra do ensaio (`ev/b8-sinc-reproducao.txt`), que não é gravação oficial | `medir.sh sinc S1` com os WAV reais | H (S1); dep C2 |
| D5 | `folha.events.csv` para T04, T05, T06, L30, aceita pelo `metricas.py` | ABERTO | Ferramenta pronta e testada (D3). `ev/qc/folha-invalida-rc2.txt` mostra só o ramo de folha inválida. Nenhuma folha real (`etapa0/eventos` vazia) | Gerar as folhas das gravações reais | H (S1); dep C1 e D4 |
| D6 | `revisao.md` por gravação com pontos de escuta e correções | ABERTO | Nenhuma | Escuta do humano (30–45 min por rodada) | H (escuta); dep E1 |
| E1 | `metricas.py` por gravação, relatório e JSON em `etapa0/relatorios/<rodada>/`, corridas ±200 ms | ABERTO | Só ensaios sintéticos: `ev/qc/medir-metricas-t04-sintetico.txt`, `ev/qc/t04-tres-corridas.txt` (PASSA, PASSA, REPROVA), `ev/qc/medir-metricas-reproducao-sintetico.txt`, `ev/qc/medir-reproducao-salto.txt`. `etapa0/relatorios` vazia | Rodar nas gravações reais | H (S1); dep D4, D5 |
| E2 | Resumo por gravação e critério com número, regra, amostras, PASSOU/REPROVOU/NÃO MEDIDO/SENSÍVEL | ABERTO | O `veredito.py` existe e foi ensaiado em dados sintéticos; sem rodada real | Rodada real | H (S1); dep E1 |
| E3 | Salto errado com classe (alcance/além do esperado) e resposta da revisão; amostras OPR-032 e OPR-034 à parte | ABERTO | Nenhuma | Rodada real e escuta (D6) | H; dep E1, D6 |
| F1 | Presets Conservador/Normal/Ágil por reprodução, cobrindo CS1–CS4 da S1; tabela por preset | ABERTO | Nenhuma | Reproduções por adb sobre os WAV da S1 | Ag com aparelho; dep C2 (WAV da S1) |
| F2 | Vocabulário ligado × desligado em T01, T02 (3 takes), T03, T04, T09 da S1 e S2 | ABERTO | Nenhuma | Reproduções, tabela; S2 inclusa | Ag com aparelho; dep S1 e S2 |
| F3 | Estratégia novo × `reset()`, 10 ciclos cada, `beginSession` e início da escuta | ABERTO | Nenhuma (`ev/f3-estrategia.md` não existe). Há só `beginSession` do ensaio (24–36 ms em `ev/qa-g2-quente.log`) | C13 do `s1-roteiro.md` (item 0): `reproduzir.py --ciclos 10 --painel` | Ag com aparelho no cabo; não depende do humano |
| F4 | Limiar: tabela de movimentos em silêncio/pausa/improviso e estatísticas do `level.csv` | ABERTO | Ferramenta `nivel.py` pronta e testada; sem dados | Gravações S1 e reproduções | H (S1); dep D5 |
| F5 | Provisórios fora do painel: confirmados ou apontados, com OPR; OPR-025 com atraso nos tokens com hífen | ABERTO | Nenhuma | Rodada real do T03 e do L30 | H (S1, L30); dep E1 |
| F6 | Configuração recomendada ≠ oficial: sessão curta ao vivo ou declaração explícita | ABERTO | Nenhuma | Só se a recomendação diferir da oficial; senão declarar que só houve reprodução | H (confirmação, ~25 min) ou declaração no relatório; dep F1–F4 |
| G1 | Latências 1, 2, fala→cursor, total, latência 3 e `beginSession`, por gravação da S1 | ABERTO | `ev/painel/T01_ensaio_sala_t1_r_normal_off_novo_l0000.tsv` é do ensaio sintético, não da S1 | Amostragem do painel (≥ 1/s) em T01 e L30 da S1, com medianas, p95, máximo, n | H (S1); Ag |
| G2 | Partida a frio: 10 corridas, e 3 de primeira cópia | FEITO | `ev/qa-g2-quente.log` e `.tsv` (n=10; TotalTime mediana 730 ms, máx 852; prepare mediana 301,5, máx 526), `ev/qa-g2-primeira.log` e `.tsv` (n=3; TotalTime mediana 724, máx 730; install_ms mediana 273, máx 334) | Nada. O "1º parcial" apareceu em 2 de 13 corridas por ruído de ambiente: não é métrica | nenhum |
| G3 | `dumpsys meminfo` a cada 60 s por ≥ 45 min na S1, com pico e medianas de PSS/RSS | ABERTO | Nenhuma. `memoria.sh` ainda não rodou numa sessão real | `memoria.sh S1 120` em segundo plano durante a S1 | H (S1); Ag |
| G4 | Bateria: corrida ≥ 45 min desconectado, com `dumpsys battery` no início e no fim | ABERTO | Nenhuma | Corrida de bateria (humano desconecta e reconecta o cabo) | H (bateria) |
| G5 | Custo do alinhamento: latência 2 do L30 separada do T01 | ABERTO | Nenhuma | L30 real e `metricas.py` | H (L30); dep E1 |
| H1 | `etapa0-relatorio-final.md` com os 8 critérios de saída | ABERTO | Nenhuma (`etapa0/relatorios` vazia) | Todo o resto | dep E2, F, G |
| H2 | Seções: decisões, calibração, metas, lacunas, interpretações pendentes, 16 KB, recomendação (e "Alternativas de motor" se CS1–CS4 reprovarem) | ABERTO | Nenhuma | Depois do H1. Lacunas já conhecidas: tablet, aparelho de entrada e intermediário, OPR-047 (retomada do T07 sem critério) | dep H1; decisões humanas: OPR-022, 024, 032, 033, 034 e 047 |
| H3 | `test_result.md` no template, aponta para o relatório e as evidências, diz o que não foi medido | PARCIAL | `test_result.md` existe e é parcial (cabeçalho e algumas linhas dizem "faltam B7, G2, C14", mas a tabela de retomada abaixo mostra esses itens feitos, então o texto está desatualizado) | Reescrever com os resultados de B8/C13/D23 e das rodadas; apontar para o relatório final; declarar o que não foi medido | qa-publicacao, ao fim |
| H4 | `git status` só com pastas permitidas; nada do modelo grande nem do venv; `git diff main --stat -- etapa0/core/src/main etapa0/app docs/` vazio | PARCIAL | Hoje: `git status --short` mostra só 3 arquivos de memória de agente (`.claude/agent-memory/revisor/`); o `git diff main --stat` dessas três pastas dá 0 linhas; `git ls-files` sem venv nem modelo grande. Obs.: o `git diff main --stat` geral lista 95 arquivos (esperado, é a branch inteira, não é o que o critério pede) | O critério é de fim de tarefa: repetir ao fechar, com gravações e relatórios já versionados | qa-publicacao, ao fim |
| H5 | Achados novos em `registers/opr_register.md` com dono; OPR medidos com a linha atualizada com o resultado | PARCIAL | Achados novos já têm linha e dono: OPR-044, 045, 047, 049, 050 a 055 (lidos no registro; abertos). As linhas dos OPR a medir (007b, 010, 011, 013, 015, 020, 024, 025, 033) ainda dizem "aberto até a 0005" | Atualizar as linhas dos OPR medidos depois da rodada real | dono de cada OPR (arquiteto, motor-voz, analista); dep E/F |

## 3. O que bloqueia o fechamento, na ordem de impacto

1. **S1 (humano, sala silenciosa, ~2 h, S23 no cabo, QuickTime, palma por take).** Sem ela não existem WAV, trajetórias nem folhas; bloqueia B4, C1, C2, D1 a D6, E1 a E3, F1, F2, F4, F5, G1, G3, G5 e, por consequência, H1 e H2. É o maior bloqueio.
2. **Item 0 do `s1-roteiro.md` (agente, com o S23 no cabo, não precisa do humano).** Fecha B8 (C11 refeito com o debug, OPR-054; D23, toque fora da tela) e F3 (C13). Pode andar já.
3. **D1 (agente, depois da S1):** instalar `vosk` 0.3.44 em venv fora do repo e baixar o modelo grande com SHA-256 (portão 6, opção A, já decidido). Bloqueia D2, E1 e a cadeia de métricas.
4. **Escuta de revisão (humano, 30–45 min por rodada):** D6 e E3. Sem ela nenhum REPROVA de salto errado ou de recuperação pode sair.
5. **S2 (segunda voz) e S3 (salão com eco):** exigidos por D9 (CS2 a CS4 valem nos dois ambientes; F2 inclui S2). Dependem de achar a voz2 e o salão; ainda sem data. Se a S1 reprovar de forma clara, avisar o orquestrador antes de gastá-las.
6. **Pergunta 2 (humano):** cota do LFS (~300 MB) e consentimento por escrito da `voz2`, antes do push dos WAV (C3). Não bloqueia gravar, só publicar.
7. **Bateria (humano, ~50 min):** G4.
8. **Decisões humanas que o relatório precisa levar ao portão 3:** OPR-022, 024, 032, 033, 034 e OPR-047 (a retomada do T07 não é medida por critério). Não impedem medir, impedem a recomendação final fechar sem ressalva.
9. **Lacunas declaradas:** tablet (ainda não há), aparelho de entrada e intermediário. Entram no relatório; não são bloqueio.
10. **Fechamento (qa-publicacao):** restauração final dos ajustes do aparelho (A4), `test_result.md` atualizado (H3), conferência de `git status` (H4), linhas dos OPR medidos (H5). OPR-044 (Auto Blocker) a fechar se a S1 correr sem queda de adb.

Nota sobre OPR abertos que não bloqueiam a S1: OPR-049 (lacunas de teste), OPR-054 (achados 1 e 2 da evidência do QA, tratados no item 0) e OPR-055 (folhas das reproduções fora do `git add`).

## 4. Comandos que rodei nesta auditoria

Nenhum usa aparelho, adb, áudio, Vosk ou modelo.

- `ls -la work/0005-etapa0-medicao/` e `find evidencias` (inventário das evidências)
- `cat`/`head`/`tail` em `spec.md`, `retomada.md`, `test_result.md`, `s1-roteiro.md`, `evidencias/qc/LEIA-ME.md`, `evidencias/diario.txt` e nos arquivos `ev/a*.txt`, `ev/b*.txt`, `ev/qa-*.txt`, `ev/aparelho-*`
- `grep` em `code_review.md` e `code_review_scripts_qa.md` (veredito) e em `registers/opr_register.md` (situação dos OPR citados)
- `git log --oneline | head -5`; `git status --short`; `git status --short --ignored etapa0`
- `ls etapa0/ferramentas etapa0/{gravacoes,trajetorias,eventos,esperado,relatorios}`
- `python3 etapa0/ferramentas/testar.py`: 329 testes, 0 falhas, 0 erros, 0 pulados
- `git lfs ls-files` (só o modelo pequeno em `app/src/main/assets/model-pt/`)
- `git diff main --stat -- etapa0/core/src/main etapa0/app docs/`: vazio
- `git ls-files | grep -i -E "venv|vosk-model-pt-fb"`: vazio

Limites da auditoria: não reexecutei Gradle (as saídas de A1 e A2 são de 2026-10-12, commit `1a0937e`; o diff de `etapa0/core/src/main`, `etapa0/app` e `docs` contra `main` está vazio, então o código medido não mudou). Não confirmei por comando que o APK instalado hoje no S23 é o debug (`ev/qa-b7-release.txt` mostra o debug reinstalado em 2026-10-12).
