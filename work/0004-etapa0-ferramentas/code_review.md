**Veredito: REPROVADO**

# Revisão — 0004-etapa0-ferramentas

**Revisor:** revisor · **Data:** 2026-10-11 · **Base do diff:** `0a6c8b1` (merge-base com `main`) → `d9c67e5` (`work/0004-etapa0-ferramentas`)

Resumo: as regras inegociáveis, os comandos e a paridade com o Kotlin estão em ordem. As 106 quebras do plano ficaram todas vermelhas. Mas duas quebras minhas no `metricas.py`, fora da tabela do plano, ficaram verdes e mudam o veredito da 0005: o "atraso ≤ 3" da recuperação de pulo e o salto errado por posição dentro dos 3 s depois do pulo. As duas são guardas de critério de saída sem teste. Mostrei a troca de veredito em memória (abaixo). Por isso, REPROVADO.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `cd etapa0 && ./gradlew :core:test lint ktlintCheck verifyMergedManifest testDebugUnitTest --rerun-tasks`: `BUILD SUCCESSFUL`, rc=0, `:app:verifyMergedManifest` executado. Abri o manifesto mesclado em `app/build/intermediates/merged_manifest/debug`: único `uses-permission` é `RECORD_AUDIO`. O `android.permission.DUMP` que aparece é atributo `android:permission` do receiver do profileinstaller, não pedido de permissão, e já existia antes desta tarefa. Nenhum arquivo Gradle nem manifesto no diff | OK |
| Texto não persiste nem vai para log | O diff não toca `etapa0/app` nem `core/src/main`. As ferramentas só leem `etapa0/textos/`: o `alinhar.py` recusa bytes diferentes (D8, `roteiro.py:345-350`, testado com +1, −1 e CRLF). O `metricas.py` não lê texto: acha o roteiro só entre os textos de teste (`roteiro.py:353-361`). As fixtures derivam de `etapa0/textos/`: conferi por script que todas as palavras sintéticas avulsas (`mal`, `livro`, `comecar`, `fala`, `muita`, `corintios`…) existem nas formas faladas dos textos. Não há discurso pessoal | OK |
| Áudio não gravado fora de `etapa0/` | `grep -rnE "wave\.open\([^)]*['\"]w\|\.writeframes\|setnchannels\|open\([^)]*['\"]wb"`: só aparece em `testes/test_timestamps.py:53-57` (silêncio em `TemporaryDirectory`). O `timestamps.py:48` abre com `"rb"` | OK |
| Rede | `grep -rnE "urllib\|http\.\|socket\|requests\|model_name\|lang=\|ftplib\|smtplib\|urlopen"` rc=1. O `vosk` só é importado dentro de `_fabrica_vosk` (`timestamps.py:89`, indentado). O modelo abre só por `vosk.Model(model_path=...)` (`:96`) | OK |
| Imports só da stdlib | O grep de imports só lista stdlib, módulos próprios e `vosk` (1×) | OK |
| `align/` sem Android | `git diff main --stat -- etapa0/core/src/main etapa0/app docs/ app/ gradle/ etapa0/*.gradle.kts etapa0/core/build.gradle.kts etapa0/gradle` vazio. O único Kotlin novo é `core/src/test/.../ReferenciaFormasTest.kt`, que importa só `org.junit` e `java.*` | OK |
| Contratos do TRD inalterados | Mesmo diff vazio. `docs/` intocado | OK |
| `SessionId` preso ao reconhecedor | Nenhum código de sessão tocado | N.A. |
| Cursor nunca abaixo de `highWater − 3` | Nenhum código de cursor tocado. O critério de recuo do `metricas.py` está travado (M11a/b, M12–M14 vermelhas) | N.A. |
| Só Compose | Nenhuma UI tocada | N.A. |

Comandos que eu mesmo rodei, com a saída real:

- `python3 --version`: 3.13.12. `python3 etapa0/ferramentas/testar.py`: `87 testes, 0 falhas, 0 erros, 0 pulados`, rc=0.
- Gradle acima: 375 casos nos XMLs reescritos (0 pulados, 0 falhas), entre eles os 2 do `ReferenciaFormasTest`.
- Os três `--help`: rc=0. Listam `wav --modelo -o`, `texto transcricao -o` e `trajetoria esperado eventos --deslocamento-ms --hipoteses --json`.
- `git diff main` fora de `etapa0/ferramentas`, `etapa0/core/src/test` e `work/0004-…`: só aparecem `.claude/agent-memory/*` e `registers/opr_register.md`, que são da fase de plano.

## Spec e plano

| Critério de aceite | Implementação | Teste |
|---|---|---|
| A1–A6 | `testar.py`, greps, `--help`, diff | comandos acima (verdes) |
| B1–B4 | `roteiro.py`, `ReferenciaFormasTest.kt`, 12 `.tsv` | `test_roteiro.py` (7), JVM (2). Quebras K abaixo |
| C1–C5 | `timestamps.py` | `test_timestamps.py` (11). Ver achado 6 (laço de `Result()`) |
| D1–D9 | `alinhar.py` | `test_alinhar.py` (21) |
| E1–E13 | `metricas.py` | `test_metricas.py` (45), `test_cli.py` (3). **E6 e a regra de salto errado do TESTES-ETAPA0 têm guarda sem teste (achados 1 e 2)** |

Desvios do plano: nenhum no código. O construtor copiou o plano, e as decisões A3, A5, A6, A7 e A9 estão no OPR-034 e no plano, justificadas.

Os testes de falha esperada são reais. Trajetória ruim reprova com código 1: E2 6%, E3, E4, E5 variante, E6 3100/nunca e E7. M49 (`return 0`) e M50 ficam vermelhas. E13 ponta a ponta passa com código 0.

## Testes (Q2 e Q3)

**Python.** Script em scratchpad: aplica uma troca por vez, roda `testar.py` e restaura com `git checkout`. A árvore ficou limpa no fim (`git status` vazio). Foram 182 quebras:

- **As 106 da tabela do plano (M, L, R, T): 106 vermelhas.**
- **76 minhas (X, Y, Z, W): 36 vermelhas e 40 verdes.** Todas as verdes estão na tabela abaixo.

Q3: os testes exigem contagem mínima (≥ 600 amostras de leitura normal, ≥ 20 amostras na janela de toque, > 50 tokens por texto, ≥ 2.101 números). Atraso sem nenhuma amostra REPROVA.

**Kotlin (paridade).** Quebrei um ponto por vez em `core/src/main/.../align/` e rodei `./gradlew :core:test --tests '*ReferenciaFormasTest*'`. Depois copiei o `build/referencia-atual/` (o "Regenerar") para uma cópia isolada da árvore (`git archive`) e rodei o `test_roteiro` do Python contra ela. Restaurei com `git checkout`, e o JVM voltou a rc=0.

| ID | Quebra | JVM | Python com a referência regenerada |
|---|---|---|---|
| KK1 | `NumberWords.kt:68` `"duas"`→`"dois"` | vermelho (2 falhas) | vermelho (291 subtestes) |
| KK2 | `NumberWords.kt:54` tirar `\|\| rest % 100 == 0` | vermelho | vermelho (38) |
| KK3 | `Tokenizer.kt:159` `c + "e" + v[0]`→`c + "de" + v[0]` | vermelho | vermelho (2) |
| KK4 | `NumberWords.kt:89` `"cem"`→`"cento"` | vermelho | vermelho (25) |
| KK6 | `NumberWords.kt:61` sem `quatorze` | vermelho | vermelho (64) |
| KK5 | `Normalizer.kt:13` `[-'’]`→`[-']` | **verde** | verde |

A paridade funciona como o plano promete. O teste JVM fica vermelho quando o Kotlin muda. O teste Python fica vermelho quando a referência é regenerada e o `roteiro.py` não acompanha (ele não lê o Kotlin diretamente: é o desenho do A2). O KK5 e a W03, a mesma troca no Python, ficam verdes porque nenhum texto de teste tem `’` nem `'` entre letras. A paridade fica provada no domínio dos textos e dos números, que é o que as ferramentas medem. Mandei para o OPR.

### Quebras que ficaram verdes (87/87)

| ID | Arquivo:linha | Troca | Julgamento |
|---|---|---|---|
| **X01** | `metricas.py:368` | `esp - cursor <= ATRASO_MAX` → `< ATRASO_MAX` | **BLOQUEANTE (achado 1)** |
| **X02** | `metricas.py:368` | `<= ATRASO_MAX` → `<= ATRASO_MAX - 1` | **BLOQUEANTE (achado 1)** |
| **X03** | `metricas.py:368` | `<= ATRASO_MAX` → `<= ATRASO_MAX + 1` | **BLOQUEANTE (achado 1)** |
| **X10** | `metricas.py:281` | `self._salto(amostras, …)` → `self._salto(normais, …)` | **BLOQUEANTE (achado 2)** |
| X06 | `metricas.py:375` | `all(...)` → `any(...)` entre pulos | IMPORTANTE (achado 3) |
| X13 | `metricas.py:338` | movimento no intervalo: `not in AUTOMATICOS` → `== "Stay"` (Reposition conta) | IMPORTANTE (achado 4) |
| X51 | `metricas.py:254` | janela do toque sem `e.fim is not None` | IMPORTANTE (achado 5) |
| Z10 | `timestamps.py:63` | `.get("result", [])` → `[:2]` | IMPORTANTE (achado 6) |
| Z11 | `timestamps.py:71` | `palavras += …Result()` → `palavras = …` | IMPORTANTE (achado 6), rodada à parte |
| X12 | `metricas.py:347` | classe `<= congelado` → `<= congelado + 1` | MENOR: só classificação, não muda veredito |
| X80 | `metricas.py:410` | latências sem excluir janela de toque | MENOR: informativo |
| X70 | `metricas.py:516` | `--hipoteses` sem texto correspondente não sai com 2 (A9) | MENOR: latências 1/2 viram n=0 sem aviso |
| X84 | `roteiro.py:359` | `tokens_por_palavras` devolve sempre o 1º texto | MENOR: só T01/T05 nos testes de latência |
| X60 | `metricas.py:155` | aceita índice negativo em `toque_palavra`/`pulo_inicio` | MENOR: validação |
| X61 | `metricas.py:118` | sem guarda de cursor negativo | MENOR: validação |
| X62 | `metricas.py:114` | sem validar `score` | MENOR: validação |
| X63 | `metricas.py:139` | sem guarda `fim < início` no esperado | MENOR: validação |
| X64 | `metricas.py:135` | `ini == "" and fim == ""` → `ini == ""` (aceita meio vazio) | MENOR: validação |
| X65 | `metricas.py:165` | sem validar `kind`/`accepted` | MENOR: validação |
| X69 | `metricas.py:96` | `!=` → `<` no número de campos (vira exceção, rc 1) | MENOR: validação |
| X71 | `metricas.py:539` | sem `OSError`/`UnicodeDecodeError` (arquivo ausente vira traceback, rc 1) | MENOR: rc 1 confunde com REPROVA |
| Y09 | `alinhar.py:59` | sem `inicio < 0` | MENOR: validação |
| Y10 | `alinhar.py:55-56` | sem validar `conf` | MENOR: validação |
| Y12 | `alinhar.py:111` | sem a guarda de tamanho (estoura com `OverflowError`, rc 1) | MENOR: não testável barato |
| Y15 | `alinhar.py:61` | ordem por `fim < anterior` | MENOR: validação |
| Y08 | `alinhar.py:147-148` | inserção no começo some do stderr | MENOR: só o roteiro da revisão manual |
| Y07 | `alinhar.py:142` | estado final `(_M,_I,_D)` em vez de `_PRIORIDADE` | MENOR: desempate só no fim da transcrição (repetição da última palavra) |
| Z01 | `timestamps.py:40` | `fullmatch` → `match` (sufixo depois de `.wav`) | MENOR: o ID continua exigido |
| Z05 | `timestamps.py:58` | sem capturar `wave.Error`/`EOFError`/`OSError` (WAV corrompido vira traceback) | MENOR: C3 não pede arquivo corrompido |
| X21 | `metricas.py:311` | `* len(atrasos)` → `* (len - 1)` | MENOR: o limite de 95% só é testado com n = 600; não há inteiro entre 569,05 e 570 |
| X31 | `metricas.py:269` | `ultima + 1` → `ultima` | MENOR: perde 1 amostra quando o fim é múltiplo de 100 |
| X30 | `metricas.py:266` | `max(0, t0)` → `t0` | MENOR: só com deslocamento negativo; acrescenta amostras com esperado −1 |
| X20 | `metricas.py:310` | `abs(a) <= 3` | Equivalente no veredito: cursor ≥ 4 à frente já é salto errado |
| X08 | `metricas.py:365` | `t < fim` → `t < inicio` | Equivalente com folha correta: `esperado ≥ alvo` só acontece depois do `pulo_fim` |
| X40 | `metricas.py:386` | `elif AUTOMATICOS` → `else` (Stay entra no recuo) | Equivalente: Stay não muda o cursor |
| X50 | `metricas.py:251` | `linha.t <= ev.t` → `<` | Equivalente: no empate, `inicio = ev.t` dos dois jeitos |
| Y02 | `alinhar.py:91` | I→D estende o trecho | Equivalente: D colado com I nunca é ótimo (uma substituição custa 1 edição, D+I custam 2) |
| Y03 | `alinhar.py:95` | inserção só abre a partir de M | Equivalente pelo mesmo motivo |
| Z09 | `timestamps.py:73` | ordena por `(end, start)` | Equivalente com palavras que não se sobrepõem |
| W03 | `roteiro.py:31` | junção sem `’` | Fora do domínio (KK5) |

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | BLOQUEANTE | `etapa0/ferramentas/metricas.py:368`; testes em `testes/test_metricas.py:289-358` | O limiar "atraso ≤ 3" da **recuperação de pulo** (TESTES-ETAPA0 Métricas; spec E6, D16) não tem teste em nenhum dos dois lados. Todos os cenários de `RecuperacaoTest` e do `test_plano_A5` recuperam com o cursor a 1 do esperado, e antes disso o atraso é de dezenas de tokens. As três trocas X01 (`<`), X02 (`<= 2`) e X03 (`<= 4`) ficam 87/87. As quebras M2a/M2b do plano ficam vermelhas só pelo critério de atraso, não pela recuperação. Lição das 0002/0003 de novo: a constante está travada, mas o uso dela em outro critério não está | Em memória, sobre a cópia limpa (`scratchpad/demo_x01.py`): cursor a exatamente 3 do esperado em `pulo_fim + 2900` e a 2 em `+3100`. Original `('PASSA', 2900)`. Com X01 `('REPROVA', 3100)`. Na 0005, uma gravação boa reprovaria (ou o contrário, com X03) | Teste com o cursor chegando a **exatamente** `esperado − 3` em `pulo_fim + 2900` (PASSA, 2900), e teste com `esperado − 4` em `+2900` e `− 3` em `+3100` (REPROVA, 3100). Os dois juntos travam X01, X02 e X03 |
| 2 | BLOQUEANTE | `etapa0/ferramentas/metricas.py:281` (`_salto(amostras, …)`); testes em `test_metricas.py:120-223` | O salto errado por posição deve valer em "qualquer amostra… em toda a trajetória" (TESTES-ETAPA0; spec "Salto errado"). Isso inclui os 3 s depois do pulo, que é exatamente onde o `Recovery`/`LongJump` do app salta. Nenhum teste tem cursor > esperado + 2 dentro de um intervalo de exceção. Trocar `amostras` por `normais` (X10, variável vizinha) fica 87/87. Seria o salto errado de um pulo que erra o destino (refrão, trecho repetido) e é corrigido em menos de 3 s, o caso que a métrica "zero saltos errados" existe para pegar | `scratchpad/demo_x10.py`: `Recovery` 1 s depois do `pulo_fim` cai 6 tokens à frente, e o orador alcança antes dos 3 s. Original `('REPROVA', 1)`. Com X10 `('PASSA', 0)` | Teste com salto por posição que começa e termina dentro de `[pulo_fim, pulo_fim + 3000)`, esperando 1 salto e REPROVA. Recomendo também um caso dentro de improviso ou pausa em que o cursor fica > esperado + 2 por amostra (não só por movimento) |
| 3 | IMPORTANTE | `metricas.py:375` | O veredito da recuperação com **mais de um pulo** não tem teste: `all` → `any` (X06) fica verde. Hoje o protocolo tem um pulo por gravação (T05, L30 min 15), por isso não bloqueia | 87/87 com X06 | Teste com dois pulos, um recuperado em 2900 e outro nunca: REPROVA |
| 4 | IMPORTANTE | `metricas.py:337-338` | "Movimento automático = Short, LongJump, Recovery" (spec) no salto por movimento: nenhum teste tem `Reposition` dentro de improviso, silêncio ou pausa. Tratar `Reposition` como automático (X13) fica verde. O app permite toque em palavra com a leitura pausada (`ReaderController.kt:144-152`, fase `Paused`) | `demo_x10.py`: toque em palavra durante a pausa. Original `('PASSA', 0)`. Com X13 `('REPROVA', 1)` | Teste com `pausa_toque`, `Reposition` + `toque_palavra` dentro da pausa e `retoma_toque`: zero saltos |
| 5 | IMPORTANTE | `metricas.py:254` | A janela do toque usa o primeiro token **com tempo** de índice ≥ *i* (D15). Nenhum teste deixa o token *i* sem tempo (a primeira palavra depois do toque não casou no alinhamento, caso plausível com o WER do Vosk). Sem `e.fim is not None` (X51), o código levanta `TypeError` e sai com rc 1, sem relatório | `demo_x10.py`: original calcula a janela até o token seguinte. X51 levanta `TypeError` | Teste com `toque_palavra` *i* e o token *i* sem tempo no esperado: a janela vai até o `t_fim` do próximo token com tempo |
| 6 | IMPORTANTE | `timestamps.py:63,69-72`; `testes/test_timestamps.py:22-38` | O reconhecedor falso devolve `Result()` só no 1º bloco, com 2 palavras. Por isso, acumular todas as palavras de cada `Result()` e de todos os `Result()` não tem teste. `palavras = …` no lugar de `+=` (Z11) e `[:2]` (Z10) ficam verdes. Com o Vosk real, que fecha um `Result()` a cada pausa, o `words.csv` ficaria só com o último trecho. O C1 do spec está cumprido ao pé da letra; o que falta são os dados do teste | 87/87 com Z10 e com Z11 | O falso fecha `Result()` em mais de um bloco, um deles com ≥ 3 palavras, e a saída tem todas as palavras na ordem |
| 7 | MENOR | ver tabela acima (X12, X20–X21, X30–X31, X60–X65, X69–X71, X80, X84, Y07–Y10, Y12, Y15, Z01, Z05) | Validações de entrada e partes informativas sem teste. Exceção não tratada sai com rc 1, igual a REPROVA, sem relatório | tabela de verdes | OPR-036 e OPR-037 (dono motor-voz) |
| 8 | MENOR | `etapa0/app/.../ReaderController.kt:144-152,269-277` × `metricas.py:194-195` | Toque em palavra com a leitura pausada retoma com `resume(fromTap = false)`, sem `retoma_toque`. O `events.csv` do app fica com `pausa_toque` sem fim, e o `metricas.py` sai com 2. Fora do roteiro do protocolo, mas dá para contornar com a folha manual (vários arquivos, D11) | leitura do código (Q4) | OPR-038 (dono arquiteto) |
| 9 | MENOR | `Normalizer.kt:13` × `roteiro.py:31` | A paridade do `’` na junção não tem prova (KK5 e W03 verdes): nenhum texto de teste tem o caractere | tabela K | OPR-037 (registro: paridade provada no domínio dos textos e números) |

Os achados 3 a 6 não bloqueiam sozinhos. Como já há rodada 2, recomendo fechá-los junto: são quatro testes curtos. Se ficarem de fora, estão no OPR-037.

**Para a rodada 2:** além dos testes acima, refaça as quebras X01, X02, X03 e X10 (e X06, X13, X51, Z10 e Z11, se entrarem) e pelo menos uma variante vizinha de cada. Exemplos: o `ATRASO_MAX` da recuperação trocado por literal 2 e 4; `_salto(amostras` com as amostras de `excecoes` filtradas. Registre na evidência.

## Não verificado

- O `timestamps.py` contra o pacote `vosk` real (JSON real, `SetWords`, `Model(model_path=…)` sem download): o pacote não está instalado, e instalar é o portão humano 6 da 0005. A afirmação de que `model_path` nunca baixa vem da fonte do pacote citada no plano. Eu não a abri.
- O tempo do L30 em outra máquina (sem meta, Q7).
- Não rodei `connectedDebugAndroidTest`: a tarefa não toca o app.
