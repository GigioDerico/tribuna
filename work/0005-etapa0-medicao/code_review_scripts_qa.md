**Veredito: APROVADO**

# Revisão — 0005-etapa0-medicao, scripts do QA (Q-C1 a Q-C5, C11 parcial)

**Revisor:** revisor · **Data:** 2026-10-14 · **Base do diff:** e3247f5 → 4ba60fb (`work/0005-etapa0-medicao`, árvore principal)

Escopo: `7d5f6dc` (`scripts/medir.sh` bloco `metricas)`, `impl_plan.md` Fase F passo 4, `evidencias/qc/*`) e `4ba60fb` (`evidencias/b8-*.txt`). É a revisão que o Q-C7 pedia para o `medir.sh` mudado. Os outros scripts (`take.sh`, `aparelho.sh`, `privacidade.sh`, `frio.sh`, `memoria.sh`, `ui.py`, `reproduzir.py`, `ensaio.py`) não mudaram neste diff. Neles fiz só `bash -n`/`py_compile` e a varredura de risco pedida (abaixo); não fiz uma revisão linha a linha.

Resumo: o bloco `metricas)` é **idêntico, byte a byte,** ao texto do Q-C1 no plano aprovado. A ordem das folhas está certa: d usa a folha de d, a corrida d−200 usa `.m200` (+200 ms) e a corrida d+200 usa `.p200` (−200 ms). Conferi isso no instante do WAV. Qualquer código diferente de 0 do `vizinhas.py` para o script antes de ele tocar qualquer relatório. Reexecutei o ensaio num repositório descartável (com espaço no caminho) e obtive o mesmo resultado que o QA anexou. Reexecutei também os dois `sincronizar.py` do B8 sobre os mesmos arquivos (mesmos sha256) e as saídas ficaram idênticas. Não há bloqueante nem importante. Os quatro achados menores vão para OPR-054 e OPR-055.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `cd etapa0 && ./gradlew --offline verifyMergedManifest`: `verifyDebugMergedManifest: OK …/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml -> android.permission.RECORD_AUDIO`, idem `verifyReleaseMergedManifest`, `BUILD SUCCESSFUL in 1s`. `git diff --stat e3247f5..4ba60fb -- etapa0 docs '*.gradle.kts' '*AndroidManifest.xml'` vazio. | OK |
| Texto não persiste nem vai para log | O diff só tem script de PC e evidências. O log novo (`medir.sh:84`) escreve no `diario.txt` só os nomes de arquivo (`${eventos[*]##*/}`), d e rc. As folhas têm tempo, evento e índice, sem texto. As evidências `qc/*` e `b8-*` têm só nomes de arquivo, sha256 e números, sem caminho absoluto. `b8-say-vozes.txt` é a lista de vozes do `say -v '?'` (frases de exemplo da Apple), pedida em C11. | OK |
| Áudio não gravado fora de `etapa0/` | `git diff --name-only e3247f5..4ba60fb \| grep -E '^\.claude/\|\.wav$\|\.m4a$\|vosk\|model\|\.apk$\|\.so$'` → `rc=1`. O WAV do ensaio continua em `$TMPDIR/tribuna-0005/ensaio/` (A13), fora do repositório e fora do git. `find etapa0/relatorios etapa0/eventos -type f` → só `.gitkeep`. | OK |
| Sem rede / sem instalação | `git diff e3247f5..4ba60fb \| grep -E 'adb \|pip install\|vosk-model\|curl\|wget'` → `rc=1` | OK |
| `align/` sem Android, contratos do TRD, `SessionId`, `highWater − 3`, só Compose | O diff não toca `etapa0/core`, `etapa0/app`, `docs` nem `app/` | N.A. |
| Nada de `.claude/` no commit | `git show --stat 7d5f6dc 4ba60fb`: só `work/0005-etapa0-medicao/…` (9 arquivos) | OK |

## Spec e plano

| Item do plano | Implementação | Conferido |
|---|---|---|
| Q-C1 bloco `metricas)` | `medir.sh:60-86` | `awk` do bloco do plano × `sed -n 60,86p medir.sh` → `diff` vazio (**IGUAL**). `bash -n` sem saída em todos os `.sh`; `py_compile` ok nos `.py` de `scripts/`. |
| Q-C1, conferência | três folhas por gravação e `.m200.json` citando a `.m200.folha` | No ensaio: `ls eventos` lista `.folha`, `.m200.folha` e `.p200.folha`. As entradas `eventos` dos JSON são `…folha.events.csv`, `…m200.folha.events.csv` e `…p200.folha.events.csv`, cada uma na sua corrida. Valor no WAV: 22800+2500 = 23000+2300 = 22600+2700 = 25300 = fim do token 46 + 600. |
| Q-C2 Fase F passo 4 | `impl_plan.md:4611` | O texto do Q-C2 está lá, mais duas frases que vêm do Q-C3 (`git add` das três folhas) e do Q-C4 (refazer a rodada). Não há decisão nova. O plano só mudou onde o próprio plano aprovado mandava. |
| Q-C3, Q-C4 | instruções de processo | Entraram no passo 4. Lacuna para reproduções: achado 3. |
| Q-C5 | sem relatórios antigos | A conferência do plano (`ls etapa0/relatorios/*/*.m200.json`) é mais estreita que a alegação (`relatorios/**`, também p200 e `reproducoes/`). Rodei `find etapa0/relatorios etapa0/eventos -type f` → só `.gitkeep`. A alegação vale. |
| C11 / B8 (parcial) | `b8-*.txt` | `say -v '?'` anexado (10 vozes pt_BR, entre elas Luciana). Reprodução: d = 0, palma confere (−90 ms, limite 200), `\|d\| ≤ 200` → atende. Ao vivo: d = −970, informativo. O toque fora da tela (D23) fica pendente e está declarado no commit. A origem do `level.csv` da reprodução não foi declarada: achado 1. |

Desvios do plano: um, não declarado. C11 manda reproduzir no debug, e o `level.csv` usado saiu da corrida de release do C10 (achado 1). O número não muda por isso, mas a origem precisa ficar escrita.

## Comandos (saída real)

```
$ PYTHONDONTWRITEBYTECODE=1 python3 -B etapa0/ferramentas/testar.py | tail -1
testar.py: 325 testes, 0 falhas, 0 erros, 0 pulados

$ for f in work/0005-etapa0-medicao/scripts/*.sh; do bash -n "$f"; echo "$f rc=$?"; done
aparelho.sh rc=0 / comum.sh rc=0 / frio.sh rc=0 / medir.sh rc=0 / memoria.sh rc=0 / privacidade.sh rc=0 / take.sh rc=0

$ grep -n -E 'sed -i|timeout ' work/0005-etapa0-medicao/scripts/*.sh
aparelho.sh:33 … screen_off_timeout 3600000   (configuração do Android, não o comando timeout)
aparelho.sh:53 … screen_off_timeout "$screen_off_timeout"
```

Os dois `sincronizar.py` do B8, rodados de novo com o código de HEAD sobre `$TMPDIR/tribuna-0005/ensaio/` (sha256 do WAV `896912b0…`, `level` ao vivo `d5b26dea…` e `level` da reprodução `ec63fc79…`, os mesmos citados na evidência):

```
reprodução (--faixa-ms -1000 1000): rc=0; diff contra b8-sinc-reproducao.txt → IGUAL
ao vivo    (--faixa-ms -5000 0):    rc=1; diff contra b8-sinc-ao-vivo.txt    → IGUAL
```

### Ensaio sintético, reexecutado

Montei um repositório descartável (`git init`) em `scratchpad/repo com espaço/`, com espaço no caminho de propósito. Copiei para ele `etapa0/ferramentas`, `etapa0/textos`, `comum.sh` e `medir.sh`. Gerei o caso T04 de `testes/test_cli_0005._caso_t04` (texto `etapa0/textos/T04.txt`, d = 2500) e rodei pelo próprio script, a partir de `cwd=/`: `medir.sh folha`, depois `medir.sh metricas`, nos dois ramos. Para fechar a cadeia, rodei `veredito.py rodada` sobre o resultado.

```
vizinhas T04_giorgio_sala_t1.folha.events.csv: rc=0
metricas T04_giorgio_sala_t1 (d=2500, eventos: …events.csv …folha.events.csv): rc=0
metricas T04_giorgio_sala_t1.m200 (d=2300, eventos: …events.csv …m200.folha.events.csv): rc=0
metricas T04_giorgio_sala_t1.p200 (d=2700, eventos: …events.csv …p200.folha.events.csv): rc=1
d / .m200 / .p200: Salto errado PASSA, PASSA, REPROVA (1)        ← igual a qc/t04-tres-corridas.txt
reproducao T04_giorgio_sala_t1_r_normal_off_novo_l0000: mesmas linhas e PASSA, PASSA, REPROVA   ← igual a qc/medir-metricas-reproducao-sintetico.txt
veredito rc 1: SENSÍVEL AO DESLOCAMENTO | sensível ao deslocamento: com d−200 e d+200: PASSOU, REPROVOU
folha inválida (improviso sem fim): "vizinhas.py: eventos: improviso_inicio sem fim" / rc=2 / "PARAR: código 2 (entrada inválida)" / medir rc=1
d negativo (sinc.json d=-970): corridas d=-970, -1170, -770 (a aritmética e o corte de ":" aceitam o sinal)
sem folha (folhas tiradas): três corridas só com events.csv, PASSA ×3; veredito: NÃO MEDIDO | sem improviso nos eventos
```

O caso "sem folha" mostra que pular o `medir.sh folha` num T04 não passa em silêncio: o `veredito.py` dá NÃO MEDIDO (Q3).

## Testes (Q2 e Q3)

As quebras foram feitas só na cópia do `medir.sh` dentro do repositório descartável, nunca na árvore principal. Depois de cada uma restaurei a cópia e conferi com `cmp` contra o original ("medir.sh igual ao original"). O `git status --short` da árvore principal mostrou só os dois arquivos da minha memória, antes e depois.

| Quebra | O que mudei | Resultado | Quem pegou |
|---|---|---|---|
| B1 | `medir.sh:80` folha de d nas três corridas (comportamento antigo) | PASSA ×3 (a sensibilidade some) | `veredito.py` rc 2: "a corrida m200 usou …folha.events.csv como eventos" → **vermelho** |
| B2 | `medir.sh:78` sufixos trocados (`.m200` com d+200) | PASSA, PASSA, REPROVA (parece certo!) | `veredito.py` rc 2: "….m200.json com deslocamento 2700, esperado 2300" → **vermelho** |
| B3 | `medir.sh:74` sem `vizinhas.py`, depois de corrigir a folha de d (−300 ms) | vizinhas velhas usadas: REPROVA, PASSA, REPROVA | `veredito.py` rc 2: "…m200.folha.events.csv não é …folha.events.csv no relógio do WAV" → **vermelho**. Controle com o script certo: as três refeitas, veredito REPROVOU sem erro. |
| B4 | `medir.sh:76` `rc_aceito "$rc" 2` com folha sem fim | `metricas.py` também recusa (rc 2) → PARAR | redundante para esse caso (o metricas pega) |
| B4b | igual a B4, com folha válida em d e `t` negativo na p200 (`t_d = 100`) | o script segue com as vizinhas velhas: rc 1, 0, 1 | `veredito.py` rc 2 (folha da m200 não confere). Com a guarda: PARAR antes de qualquer relatório. A guarda tem efeito próprio. |

Leitura: o `medir.sh` não tem teste próprio. Quem segura cada quebra do script é o `veredito.py` (`conferir_entradas` e `conferir_folhas`, `veredito.py:227-267`), que sai com 2 e diz o que rodar. É a defesa que o Q-C5 descreve. O B2 merece nota: o `.md` da corrida trocada mostra o resultado "esperado", e só o veredito percebe a troca. Quem ler os `.md` à mão antes do veredito não percebe nada. Mesmo assim, nenhum número chega ao relatório sem passar pelo veredito (Fase F, passo 5).

Q3: o ensaio tem conteúdo de verdade, não é vazio. A corrida p200 reprova com 1 ocorrência e o veredito vê "PASSOU, REPROVOU".

## Q1, Q4 a Q8 sobre o diff

- **Q1.** As vizinhas são consumidas pelo `metricas.py` (`medir.sh:80-82`) e conferidas pelo `veredito.py` (sha256 e relógio do WAV). O log novo é para leitura humana no `diario.txt`.
- **Q4.** Abri os artefatos do ensaio: as folhas (`22800/23000/22600`) e as `entradas` dos JSON. Para o B8, li os arquivos reais em `$TMPDIR` e conferi os sha256.
- **Q5.** A alegação do Q-C5 cobre mais do que o comando do plano confere. Cobri com `find` (ver Spec e plano). Para o achado 2, reproduzi eu mesmo o que o `LEIA-ME` alega sem anexar.
- **Q6.** Nenhum dado de discurso. Os nomes de arquivo seguem o padrão do protocolo, e os textos vêm só de `etapa0/textos/`.
- **Q7.** Não há número de critério nesta entrega. O d do B8 vem rotulado "t_wav = t_app + deslocamento".
- **Q8.** O `level.csv` da reprodução no B8 vem de uma corrida anterior (2026-10-12 12:28, com o release instalado, `diario.txt` linhas "release instalado" e "puxado T01_ensaio_sala_t1_r_normal_off_novo_l0000"). Isso não está escrito: achado 1.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | MENOR | `evidencias/b8-sinc-reproducao.txt:12`; `impl_plan.md:4530` | O C11 manda "reproduzir o ensaio no debug (como em C10)". A evidência usa o `level.csv` da reprodução de 2026-10-12 12:28, feita com o **release** instalado (B7/C10: `diario.txt` "12:14:54 … release instalado" e "12:28:39 puxado …_l0000"; `b7-instalado-release.txt`). Nem a evidência nem o commit dizem isso: "sem aparelho" sugere que nada veio do aparelho. O d = 0 vale para o caminho WAV → app, mas a versão do APK fica sem registro. | `ls -la $TMPDIR/tribuna-0005/ensaio/` (mtime 12:28); sha256 `ec63fc79…` igual ao da evidência | Uma linha na evidência ou no `LEIA-ME` com a origem (corrida do C10, release, 2026-10-12 12:28, sha do APK). Ou refazer no debug quando o aparelho voltar para o resto do C11 (toque fora da tela). OPR-054. |
| 2 | MENOR | `evidencias/qc/LEIA-ME.md:11`; `qc/medir-metricas-reproducao-sintetico.txt` | Duas alegações sem saída anexada (regra de processo 8). (a) "Folha inválida: vizinhas.py sai com 2 e o script para com PARAR: código 2" não tem arquivo de saída. (b) O ramo `reproducao` só tem o log de rc, sem as linhas de Salto errado das três corridas. | Reproduzi as duas (seção Ensaio): a alegação vale | Anexar as saídas ao `qc/`. OPR-054. |
| 3 | MENOR | `impl_plan.md:4611` (Q-C3, `impl_plan.md:6129`) | O `git add etapa0/eventos/<nome>*.folha.events.csv` cobre só a gravação oficial. As reproduções também ganham três folhas (`etapa0/eventos/reproducoes/<rep>*.folha.events.csv`, `medir.sh:67` e `:73-80`), e o `veredito.py rodada --sufixo` confere o sha256 delas. O plano não diz se elas vão para o git nem quando. | `grep -n "git add" impl_plan.md`: nenhuma linha para `reproducoes/` | Uma frase no passo 4 ou na Fase G dizendo onde as folhas das reproduções vão para o git (ou que não vão, e por quê). OPR-055. |
| 4 | MENOR (fora do diff; linhas 81-82 iguais à rodada 1) | `medir.sh:81-82` | Se o `metricas.py` sai com 2 numa corrida, o `> "$rel/$saida$suf.md"` já truncou o `.md` dessa corrida, e o `.json` e as corridas seguintes ficam com o conteúdo antigo. O PARAR avisa e o sha256 do veredito pega mudança de entrada, mas uma mudança no código do `metricas.py` não entra no sha. | Quebra B4: "d: (sem md)" com os `.m200`/`.p200` antigos ainda no disco | Escrever em arquivo temporário e mover só com rc ≤ 1, ou apagar os seis arquivos da gravação antes do laço. OPR-055. |

## Não verificado

- O ramo `metricas` contra gravação real: ainda não existe nenhuma (Q-C5, lista vazia). A primeira gravação da S1 com folha deve repetir a conferência do Q-C1 (`ls …folha.events.csv` com três arquivos, `grep folha …m200.json`).
- A parte do C11 que precisa do aparelho (toque fora da tela, D23) e o C13: pendentes, sem aparelho nesta sessão.
- Os scripts que este diff não muda: só `bash -n`, `py_compile` e a varredura de `sed -i`/`timeout`. Não fiz uma revisão linha a linha deles.
