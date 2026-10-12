# Plano — 0005-etapa0-medicao

**Autor:** arquiteto · **Spec:** `spec.md` (conferido pelo orquestrador em 2026-10-12; portão 6 respondido pelo humano: opção A) · **Aprovação:** conferida pelo orquestrador em 2026-10-12 (checklist do PROCESS.md: critérios mapeados, testes antes do código, Q6, ferramentas novas passam por revisão antes de entrar em número de relatório; portão 6 opção A aprovado pelo humano; WAV/LFS e voz2 seguem como portão condicional; instalações só no passo D1)

> **Para os executores.** Há dois executores e uma revisão no meio:
> - **`motor-voz`** faz as Tarefas 1 a 6 (quatro ferramentas Python novas em `etapa0/ferramentas/`), num `git worktree` próprio, com teste antes do código. Cada passo traz o arquivo inteiro: copie exatamente, rode o comando e compare com a saída esperada. Nada de `pip install`, download ou rede.
> - **`revisor`** revisa o diff das ferramentas e dos scripts do QA (`code_review.md`) antes de qualquer número delas entrar num relatório (spec D3).
> - **`qa-publicacao`** executa as Fases C a L (aparelho, sessões com o humano, métricas, calibração, relatório), com os scripts da Tarefa Q.
> - Cada **PARAR E PEDIR AO HUMANO** é uma parada de verdade: o agente escreve a mensagem indicada, encerra a vez e espera. Fora delas, o agente não pede nada ao humano.
>
> Não execute nada que não esteja aqui. Ao terminar cada parte, preencha o "Registro da construção" (motor-voz) ou o `test_result.md` (QA).

**Goal:** medir no Galaxy S23 se o Vosk, o reconciliador e o alinhador da etapa0 cumprem os critérios de saída da Etapa 0, calibrar o que o painel permite e entregar o relatório final ao portão humano 3.

**Architecture:** quatro ferramentas Python só com a stdlib, testadas com sinais e roteiros sintéticos e revisadas (sincronização dos relógios, folha de eventos, veredito por critério e limiar de nível), em cima das três da 0004. Scripts de bash e Python do QA dirigem o aparelho por `adb` e `uiautomator`. As sessões ao vivo com o humano dão as trajetórias oficiais; as reproduções do mesmo WAV pela fonte WAV da 0003 dão a calibração.

**Tech Stack:** Python 3.13.12 (stdlib: `unittest`, `wave`, `array`, `math.sumprod`, `csv`, `json`, `argparse`, `statistics`, `xml.etree`); bash; `adb` 37.0.1 com `uiautomator`; macOS `say`, `afconvert`, `afinfo`, `afplay`, QuickTime Player; `vosk` 0.3.44 e o modelo grande `vosk-model-pt-fb-v0.1.1-20220516_2113` num venv e numa pasta fora do repositório (passo D1, portão 6 opção A).

---

## Resumo

Uma tarefa só, em fases. **Construção** (motor-voz, Tarefas 1–6): `sincronizar.py`, `folha.py`, `veredito.py`, `nivel.py`, com 121 testes novos (270 no total) e 144 de 145 quebras Q2 vermelhas na prova (a outra é equivalente e está explicada). **Tarefa Q** (qa-publicacao): dez scripts de condução do aparelho, com autoteste. **Execução** (qa-publicacao): uma parte sem humano (base, verificações no aparelho, ensaio com voz sintética, frio, estratégia), depois as sessões S1, S2 e S3 com o humano, a verdade-terreno com o `vosk` instalado no passo D1, a revisão de escuta, a calibração por reprodução, a bateria e o relatório. Nada muda em `align/`, no app de etapa0, em contrato, no manifesto, no `metricas.py`, no `alinhar.py` nem no `timestamps.py`. A `interface` não trabalha nesta tarefa.

## Decisões do arquiteto

O spec deixou ao plano as ferramentas novas, a divisão entre agente e humano, os comandos, a ordem e os detalhes que só aparecem lendo o código e o aparelho. Cada decisão diz o que estava em aberto, a escolha e o porquê.

### A1. Ambiente conferido por comando em 2026-10-12 (pedido do orquestrador, item 3)

```
$ adb devices -l
RQCW80BPXKN  device usb:1048576X product:dm1qxxx model:SM_S911B device:dm1q transport_id:1
$ adb shell getprop ro.product.model / ro.build.version.release / ro.build.version.sdk / ro.product.cpu.abilist
SM-S911B / 16 / 36 / arm64-v8a,armeabi-v7a,armeabi
$ adb shell getconf PAGE_SIZE        -> 4096
$ adb shell grep MemTotal /proc/meminfo  -> MemTotal: 7242456 kB
$ adb shell which uiautomator        -> /system/bin/uiautomator
$ adb shell dumpsys package io.github.gigioderico.tribuna.etapa0 | grep -E "requested|RECORD"
    versionName=0.0.1
    requested permissions:
      android.permission.RECORD_AUDIO
        android.permission.RECORD_AUDIO: granted=false
$ adb shell ls /sdcard/Android/data/io.github.gigioderico.tribuna.etapa0/files/   -> wav/ (vazio)
$ adb shell run-as io.github.gigioderico.tribuna.etapa0 ls files                    -> profileInstalled (modelo ainda não copiado)
$ adb shell settings get global airplane_mode_on / zen_mode; system screen_brightness / screen_brightness_mode /
  screen_off_timeout; global stay_on_while_plugged_in; system accelerometer_rotation / user_rotation
0 / 0 / 81 / 1 / 600000 / 0 / 1 / 0
$ adb shell cmd connectivity airplane-mode   -> disabled      (o comando existe)
$ adb shell cmd notification                 -> lista set_dnd [on|none|priority|alarms|all|off]
$ adb shell dumpsys battery | head           -> USB powered: true, level: 100, Charge counter: 3705000
$ adb shell dumpsys window | grep isKeyguardShowing   -> isKeyguardShowing=true (tela bloqueada)
$ 4 x (adb exec-out uiautomator dump /dev/tty)       -> 2,26 s, 2,29 s, 2,27 s, 2,26 s por dump
$ say -v '?' | grep pt_BR   -> Eddy, Flo, Grandma, Grandpa, Luciana, Reed, Rocko, Sandy, Shelley (pt_BR)
$ which afconvert afplay afinfo   -> /usr/bin/...;  osascript -e 'id of application "QuickTime Player"' -> com.apple.QuickTimePlayerX
$ sw_vers -> macOS 27.0.1;  python3 --version -> Python 3.13.12;  git lfs version -> git-lfs/3.8.0
```

Consequências:
- **O S23 roda com páginas de 4 KB.** O 16 KB não é exercitado em tempo de execução; o critério A2 e o CS5 ficam só com a verificação estática do `verify16kAlignment` (OPR-024).
- **A tela estava bloqueada** e o aparelho **saiu do adb no meio do PLAN** (`adb devices` vazio às 10:0x). Todo script começa por `exige_aparelho`, e cada fase começa por uma parada curta para o humano desbloquear e ligar o cabo. Desbloquear não dá para fazer por adb sem a senha, e a senha não é pedida.
- **`uiautomator dump` leva ~2,3 s.** O spec G1 pede "pelo menos 1 amostra por segundo" do painel; isso é impossível com o `uiautomator` sem instalar nada. Ver A12.
- **Há voz pt-BR no `say`** (Luciana): o ensaio de D21 é feito com ela (A13).
- **`afconvert` gera WAV legível pelo `wave` do Python e pelo `WavHeader` do app**, testado na prova: `say` → AIFF 22.050 Hz → `afconvert -f WAVE -d LEI16@16000 -c 1 --mix` → `RIFF/WAVE`, `fmt ` PCM 1 canal 16 kHz 16 bits, mais um bloco `FLLR` de preenchimento (o `wave` e o `WavHeader.read` pulam blocos desconhecidos). Com uma fonte estéreo ALAC 48 kHz, `-c 1` sem `--mix` e com `--mix` diferem em no máximo 1 degrau; o plano usa `--mix`.
- **No zsh (shell do humano e dos agentes), `$VAR` com espaços não vira várias palavras**: `S="adb -s X shell"; $S ls` falha com "command not found". Todos os scripts são **bash** (`#!/usr/bin/env bash`) e usam funções.

### A2. Ferramentas novas: quatro, nesta tarefa, pelo `motor-voz` (pedido do orquestrador, item 1)

O que muda número no relatório passa pelo ciclo normal (teste, revisão). São quatro, porque cada uma decide um número ou um veredito que, à mão, um agente erraria em ~40 gravações:

| Ferramenta | Decide | Spec |
|---|---|---|
| `sincronizar.py` | o deslocamento `t_wav = t_app + d`, a palma e a deriva; a marca "sensível" vinda da sincronização | D3, D4 |
| `folha.py` | os tempos de improviso, silêncio e pulo da folha de eventos, no relógio do app; os pontos de escuta das fronteiras | D5, D6, D7 |
| `veredito.py` | o estado de cada critério por gravação (PASSOU, REPROVOU, NÃO MEDIDO, SENSÍVEL AO DESLOCAMENTO, PENDENTE DE REVISÃO), a agregação por rodada e a final CS1–CS4, e as tabelas de calibração por configuração | C1, C2, D9, E2, E3, F1, F2, H1 |
| `nivel.py` | as estatísticas do nível em silêncio, pausa, improviso e leitura, e o candidato a limiar pela regra do D12 | F4, D12 |

**Por que dentro da 0005 e não numa 0005a:** o spec já tem os critérios delas (D3) e o PROCESS só exige que o código passe por construção, revisão e QA. Uma tarefa à parte repetiria spec e plano sem ganho. As ferramentas ficam no branch da 0005, construídas num `git worktree` (memória do arquiteto: dois agentes na mesma árvore quebram o "ver falhar" um do outro), e o QA só usa número delas depois do `code_review.md` APROVADO.

**Scripts do QA** (Tarefa Q, `work/0005-etapa0-medicao/scripts/`): só dirigem o aparelho, puxam arquivos e chamam as ferramentas. Dois deles fazem conta (mediana e pico da memória; mediana do frio), e o `ui.py` filtra as linhas do painel. Por isso o `revisor` também lê os scripts no mesmo REVIEW, e cada script tem autoteste ou `bash -n`.

### A3. `sincronizar.py` (D3, D4)

- **Convenção:** `t_wav = t_app + d`, o mesmo sinal do `metricas.py --deslocamento-ms`. Ao vivo, `d > 0` (o QuickTime grava antes do Ouvir).
- **Modelo do `level.csv`** (lido no código da 0003): cada linha é o RMS do bloco de 3.200 amostras (200 ms) que acabou de chegar, carimbado quando o `ReaderController` o recebe (`recording?.level(now, ...)`); a origem do relógio é o fim do `startCapture` (`Recording(originMs = clock())`). Então a linha `t` cobre o áudio `[t − 200, t)` do app.
- **Método:** WAV em energia por fatia de 10 ms (inteiros exatos, `math.sumprod`); bloco de 200 ms terminando em `t + d` = soma de 20 fatias; fora do WAV conta como silêncio. Os dois lados em dB, com piso de um degrau do PCM 16 bits (`1/32768`, que vem do formato, não é parâmetro). O `d` é o de maior correlação de Pearson, na grade de 100 ms e depois de 10 ms a até 100 ms do melhor. Pearson em dB é invariante a ganho constante: o microfone do Mac e o do S23 têm ganhos diferentes (quebra S35, equivalente por desenho).
- **Faixa padrão:** todo `d` em que o WAV cobre a gravação inteira do app (`[200 − t_min, duração − t_max]`). O roteiro garante (QuickTime antes do Ouvir, parado depois do Parar). Se não cobrir, sai com 2 e uma mensagem que diz o porquê. Nas reproduções pela fonte WAV o app continua depois do fim do arquivo (silêncio), então elas usam `--faixa-ms -1000 1000`.
- **Palma:** a linha mais alta do `level.csv` até 4.000 ms (o roteiro põe a palma ~2 s depois do Ouvir e a leitura ~2 s depois dela). As linhas a até 1 s dela são correlacionadas de novo a até 1 s do `d` global. **Um estalo cabe inteiro em vários blocos de 200 ms**, e a correlação faz um patamar de ~180 ms. Na prova, o "primeiro máximo" deu −180 ms, quase no limite de 200. Por isso a palma usa o **meio do patamar** (empate = diferença menor que `1e-9`, só arredondamento). Ela confere se fica a até 200 ms do global. O pico da palma no WAV fica registrado (D4).
- **Deriva:** cada metade das linhas (por contagem) é correlacionada de novo a até 1 s do `d`. A diferença começo − fim é estendida à duração inteira (conservador: marca "sensível" mais vezes, nunca menos). Melhor na borda da busca, ou metade sem variância, dá "indeterminada". Um corte na gravação de referência aparece como deriva acima de 200 ms (teste `test_D3_corte_na_gravacao_de_referencia_sai_sensivel`).
- **Códigos:** 0 sincronizado; 1 sensível (palma não confere, deriva > 200 ms ou indeterminada), com o `d` calculado; 2 entrada inválida.
- **Prova com fala real:** T01 (92 s) e L30 (27 min) da voz Luciana com a palma sintética, `level.csv` simulado como o app faria. Reprodução: `d = −40` exato (real −40), palma −80 ms, deriva 0 a 20. Ao vivo, com ganho 0,3, piso 0,003 e deriva de 50 ppm: T01 `d = 2.500` (real 2.500), L30 `d = 2.540` (deriva medida −80 ms; a real é 50 ppm × 27 min ≈ 81 ms). O L30 inteiro leva 0,3 s depois de ler o WAV (0,4 s).

### A4. `folha.py` (D5, D6, D7)

- **Onde está cada evento** (lido nos textos): T04 `// IMPROVISAR` (entre os tokens 46 "conversa" e 47 "Isso"); T05 `// PULAR` (pula os tokens 47 a 90; volta no 91 "Ouvir"); T06 `// SILÊNCIO` (entre 134 "rodeios" e 135 "Outra"); L30 `// L30 — Minuto 8 (` (improviso no **fim do parágrafo abaixo da nota**: entre 1120 e 1121), `// L30 — Minuto 15 (` (pula o parágrafo seguinte: 1804 a 1898; volta no 1899) e `// L30 — Minuto 18 (` (silêncio no fim do parágrafo abaixo: entre 2427 e 2428). As notas do L30 dizem "ao terminar o parágrafo abaixo"; as do T04 e T06 ficam no lugar do evento. Por isso a regra é por texto, e cada prefixo tem de aparecer exatamente uma vez (texto mudado = erro, não folha errada).
- **Improviso** (D6: "faixa de palavras inseridas"): da primeira à última palavra do `words.csv` entre o fim do último token com tempo antes da fronteira e o início do primeiro com tempo depois. O tempo da palavra vem da transcrição, não do `alinhar.py`, para não depender de o NW ter casado uma palavra do improviso com um token.
- **Silêncio:** do fim do token antes ao início do token depois. Palavra da transcrição dentro do silêncio gera aviso.
- **Pulo:** `pulo_inicio` no fim do token antes do parágrafo pulado, com o índice do primeiro token pulado; `pulo_fim` no início do primeiro token com tempo depois dele. Token pulado com tempo gera aviso.
- **Fronteira em token sem tempo:** usa o vizinho com tempo e avisa. No `pulo_fim` isso deixaria a recuperação otimista, então vira ponto de escuta obrigatório (D7).
- **Saída:** CSV no relógio do app (`t_wav − d`; `t_app < 0` é erro), um "revisar: ..." por fronteira no stderr e os avisos. Códigos 0, 1 (com aviso) e 2. Pausa e toque continuam vindo do `events.csv` do app.

### A5. `veredito.py` (D3, D7, D8, D9, D10, E2, E3, H1)

- **Ligação (D9):** CS1 = T01, T10, L30 só na sala; CS2 = L30, T02, T03, T04, T08, T09 (salto **e** recuo); CS3 = T05, L30; CS4 = T06, T07, L30 (só movimentos automáticos em silêncio ou pausa).
- **Mínimos (Q3):** CS1 sem amostras de leitura → NÃO MEDIDO; CS2 sem leitura normal ou com o cursor parado no token 0 → NÃO MEDIDO; CS3 sem pulo → NÃO MEDIDO. Eventos exigidos: T04 e L30 precisam de improviso (CS2), T05 e L30 de pulo (CS3), T06 de silêncio, T07 de pausa e L30 de pausa e silêncio (CS4). A contagem usa o `metricas.ler_eventos` e o `metricas.intervalos`, que já validam os pares.
- **Sensibilidade (D3):** o mesmo critério em `d`, `d − 200` e `d + 200`. PASSOU em `d` e outro estado em ±200, ou `motivos` no `sinc.json`, vira SENSÍVEL AO DESLOCAMENTO. REPROVOU em `d` continua REPROVOU (marcado `sensivel`), porque o melhor `d` medido reprova.
- **Revisão (D7):** sem a linha exata `Revisão concluída: sim` em `esperado/<nome>.revisao.md`, todo estado vira PENDENTE DE REVISÃO, com o estado que teria. Assim nenhum REPROVA sai sem o ponto ouvido.
- **Configuração (D10):** o `meta.txt` tem de ter `preset=Normal`, `vocabulary=off`, `recognizer_strategy=NewPerSession`, `level_threshold=0.0`, `animation_ms=375`, `audio_source=mic` e o `text_id` do nome. Configuração diferente → NÃO MEDIDO com o motivo; `text_id` errado → código 2.
- **Proveniência (Q8):** o sha256 de cada arquivo que o `metricas.py` e o `sincronizar.py` citam tem de bater com o disco, os três relatórios têm de citar os mesmos arquivos e os deslocamentos têm de ser `d`, `d − 200` e `d + 200`. Senão, código 2 ("rode de novo").
- **Agregação:** REPROVOU se alguma gravação ligada reprova; PASSOU só com todas as exigidas presentes e PASSOU; o resto é NÃO MEDIDO com os motivos. Na `final`, CS1 exige S1 e S2 (cada voz) e CS2–CS4 exigem S1 e S3 (os dois ambientes), com a S2 somada quando existe. A `final` recusa rodada de reprovação.
- **Reprodução (F1, F2):** `--sufixo _r_<preset>_<vocab>_<estrategia>_l<limiar>` e `--config chave=valor` aplicam a mesma lógica às reproduções. O esperado, o WAV e a revisão continuam os da gravação oficial.

### A6. `nivel.py` (F4, D12)

O limiar do app filtra parciais com `lastLevel >= threshold` e nunca filtra Final (`ReaderController.kt:372`: `isFinal || lastLevel >= threshold`). A ferramenta lista cada movimento automático dentro de improviso, silêncio ou pausa, com a hipótese aceita que o causou e o nível em vigor nela. O **candidato** é o menor nível do `level.csv` (de todas as gravações pedidas) acima do maior nível das **parciais** que causaram movimento em silêncio ou pausa. Improviso e Final ficam listados, mas não entram, porque o limiar não os resolve. Sem movimento em silêncio ou pausa, a recomendação é 0. O candidato ainda é verificado por reprodução (F4).

### A7. Passo D1: `vosk` 0.3.44, não 0.3.45 (portão 6 opção A, decidido pelo humano)

**Surpresa (PyPI JSON, 2026-10-12):** a versão mais nova, `vosk` 0.3.45 (2022-12-14), **não tem wheel para macOS** (só Linux e Windows). A mais nova com wheel para Mac é a **0.3.44**: `vosk-0.3.44-py3-none-macosx_10_6_universal2.whl`, 4.710.953 bytes, sha256 `029d0b3d6a5cff874c575b8a5814a4cccfb37608cff52e846c02a8c67a882801`. Baixei o wheel no scratchpad, conferi o sha256 e li o código sem instalar. `Model(model_path=...)` não usa a rede; `KaldiRecognizer(model, taxa)`, `SetWords` e `SetLogLevel` existem, e é o que o `timestamps.py` chama. O `import vosk` precisa de `requests`, `srt` e `tqdm` (dependências declaradas), que vêm do PyPI no `pip install`. O modelo: `vosk-model-pt-fb-v0.1.1-20220516_2113.zip` (FalaBrasil, GPLv3), `Content-Length: 1693530883`, `Last-Modified: Fri, 20 May 2022 13:18:48 GMT` (HEAD em 2026-10-12). O site não publica o sha256 do zip: ele é calculado no download e registrado. Fontes: https://pypi.org/pypi/vosk/json e https://alphacephei.com/vosk/models. A opção B do spec fica descartada (decisão humana), registrada só como nota.

### A8. Divisão entre agente e humano (pedido do orquestrador, item 2)

- **Sem humano:** Fases B, C (exceto a parada de desbloqueio), E, F (menos a escuta), G, L.
- **Com humano:** S1, S2, S3, a escuta da revisão, a bateria, a confirmação (só se D28 pedir) e as paradas de desbloqueio e de portão. Cada uma é um bloco **PARAR E PEDIR AO HUMANO**, com a mensagem pronta.
- **Quem toca no aparelho durante o take:** o humano toca Ouvir, Pausar, Ouvir e a palavra do L30. Ao ouvir "fim", o agente fecha o take por adb (Depuração, Parar, nome, Exportar, `adb pull`). Isso tira do humano o campo de texto e o botão Parar, que fica dentro do painel.

### A9. Ajustes do aparelho restaurados **ao fim de cada bloco**, não só no fim da tarefa (D18, A4)

O S23 é o telefone pessoal do humano. Deixar modo avião e Não perturbe ligados entre as sessões (dias) seria um problema real. Então `aparelho.sh salvar` + `aplicar` abrem cada bloco com o aparelho, e `aparelho.sh restaurar` o fecha, conferindo que o estado depois é igual ao antes (diff em `evidencias/aparelho-ajustes.txt`). Brilho fixo = 255 (máximo, o mesmo da bateria), `svc power stayon usb` no lugar de mexer no tempo de tela; a bateria (sem cabo) usa `screen_off_timeout` de 1 h. O `salvar` se recusa a sobrescrever um "antes" que ainda não foi restaurado.

### A10. Reproduções (D1, D11, F1–F4)

- **Nome:** o app só exporta nomes `^<ID>(_[a-z0-9]+)*$` (`ExportName`, 0003). O `-r-` do D20 é recusado. As reproduções se chamam `<nome>_r_<preset>_<vocab>_<estrategia>_l<limiar×1000 com 4 dígitos>` (ex.: `T01_giorgio_sala_t1_r_agil_off_novo_l0000`) e ficam em `trajetorias/reproducoes/`, `eventos/reproducoes/` e `relatorios/<rodada>/reproducoes/`.
- **Pausas:** a fonte WAV toca em tempo real e continua consumindo o arquivo na pausa (a captura fica aberta). Sem repetir as pausas oficiais, a fala de terceiros do T07 e do L30 cairia numa leitura ativa. O `reproduzir.py` toca Pausar e Ouvir por adb no instante do WAV de cada `pausa_toque`/`retoma_toque` oficial, ou seja, `t_app oficial + d oficial`. O toque na palavra do L30 não é repetido: a reprodução segue a fala pelo próprio alinhador (recuperação), o que só mede o motor e vale como dado de calibração.
- **Deslocamento:** a origem do `Recording` é o fim do `startCapture` (`ReaderController.kt:259`) e o `RealTimePacer` começa no primeiro bloco. O `d` de uma reprodução é perto de 0, mas não é 0. Ele é **medido** com `sincronizar.py --faixa-ms -1000 1000` (a prova deu −40 ms no sintético), e não suposto.
- **Folha:** a folha está no relógio do app da gravação oficial. Para a reprodução sai outra, `eventos/reproducoes/<rep>.folha.events.csv`, com o `d` da reprodução.
- **Configuração:** o `meta.txt` da reprodução é o oráculo (`reproduzir.py` para se não bater). O estado dos chips e do switch lido pelo `uiautomator` é só ajuda.

### A11. Privacidade do que sai do aparelho (Q6; pedido do orquestrador, item 5)

O `logcat`, os `uiautomator dump` e os `dumpsys` do aparelho do humano têm dados de outros apps (notificações, contas). Eles **nunca vão para o repositório**: ficam em `$TMPDIR/tribuna-0005/` e são lidos por `grep` ou pelo `ui.py`, que só guarda nós do pacote da etapa0, e então apagados. Na evidência entram só contagens, as linhas numéricas do painel e as linhas filtradas do pacote. As capturas de tela do recurso de A12 também ficam fora.

### A12. Latência 3 e `beginSession` com um dump de ~2,3 s (G1, F3; divergência com a letra do spec)

O spec G1 pede amostragem do painel "de pelo menos 1 por segundo". Medido: o `uiautomator dump` leva 2,26 a 2,29 s, então o máximo é ~1 a cada 2,3 s. A amostragem não muda o valor de cada amostra, só o n (a latência 3 aparece no painel depois de cada animação). **Decisão:** amostrar o mais rápido possível, registrar o n e a taxa obtida e declarar o desvio no `test_result.md`. Isso também vai ao `opr_register.md` (OPR-041, dono: analista), para ajustar o texto do G1. Se o `uiautomator` não conseguir o estado ocioso com o painel aberto e o nível mudando a cada 200 ms (`ERROR: could not get idle state`, o `ui.py` sai com 2), o `reproduzir.py --captura` troca o dump por `screencap`. O agente lê os números das imagens, que ficam fora do repositório, e anota "lido de captura" ao lado de cada valor.

### A13. Ensaio sem humano (D21, D23, B8)

Com a voz Luciana: `ensaio.py montar T01` gera `T01_ensaio_sala_t1.wav` (2 s de silêncio, palma sintética de 20 ms, 2 s, fala; 92 s) fora do repositório. Usos: (1) **reprodução** pela fonte WAV (B8: `|d| ≤ 200`; B7 com o release); (2) **ao vivo**, tocado pelo `afplay` do Mac ao lado do aparelho, para B3, B5 e o caminho exportar → puxar → sincronizar; (3) os 10 ciclos da estratégia (F3, D16). O ensaio não conta para critério nenhum e não entra no repositório.

### A14. Bateria (D14, G4)

Para o humano não precisar tocar no aparelho com o cabo solto, o agente junta `L30` + T01…T10 da S1 num WAV de ≥ 46 min (`ensaio.py concatenar`, fora do repositório) e o toca pela fonte WAV com o texto L30 carregado. O trabalho é o mesmo: reconhecimento contínuo, tela no máximo. O alinhamento perde o roteiro depois do L30, o que só reduz um custo pequeno, e isso fica declarado. O humano só desconecta e reconecta. A medida usa `level` e também `Charge counter` (µAh) do `dumpsys battery`, mais fino que 1%, antes de desconectar e logo ao reconectar, com os horários do relógio do Mac.

### A15. Desvios declarados em relação à letra do spec

1. Nome das reproduções com `_r_` (A10; o app não aceita `-`).
2. Amostragem do painel em G1 limitada a ~1/2,3 s (A12; OPR-041).
3. Ajustes do aparelho restaurados a cada bloco (A9; mais estrito que o spec).
4. Deslocamento das reproduções medido, não suposto 0 (A10).
5. Bateria com um WAV concatenado (A14; é o "L30 e, em seguida, outros WAV" do D14 sem toques).
Nenhum muda número de critério nem do PRD.

### A16. `.pyc` velho engana a prova Q2 (surpresa da prova)

O Python valida o `__pycache__` só pela data (em segundos inteiros) e pelo tamanho do fonte. Uma quebra que troca um trecho por outro do mesmo tamanho, restaurada no mesmo segundo, deixa o bytecode do mutante valendo. Na prova isso fez uma quebra (F29) ficar "presa" depois de restaurada e derrubou quatro testes do `folha.py` sem motivo aparente. **Toda rodada de quebras roda com `PYTHONDONTWRITEBYTECODE=1 python3 -B` e apaga o `__pycache__` antes de cada execução.**

## Prova do plano (feita pelo arquiteto em 2026-10-12)

Numa cópia descartável de `etapa0/ferramentas`, `etapa0/textos` e das referências da 0004 (scratchpad, fora do repositório):

- As quatro ferramentas e os cinco arquivos de teste deste plano: **270 testes, 0 falhas, 0 pulados** (`python3 etapa0/ferramentas/testar.py`: 149 da 0004 + 121 novos), em ~8,5 s.
- **Quebras Q2:** 145 trocas aplicadas uma a uma (o runner restaura o arquivo, apaga o `__pycache__` e roda o `testar.py`; o `git diff -- '*.py'` volta vazio no fim). **144 vermelhas, 1 verde equivalente (S35)**. As que sobreviveram nas rodadas anteriores viraram teste ou simplificação (A3: `isfinite` e "mesmo instante" eram redundantes; arredondamento do `t` trocado por divisão inteira; `_codigo` sem a pendência redundante; contagem de eventos pelo `metricas.intervalos`; o caso `-r-agil` do teste do `--sufixo` era lido pelo argparse como opção e virou `_r-agil`). A tabela está em "Quebras propositais".
- Sincronização com fala real da voz Luciana (A3), T01 e L30.
- Scripts do QA: `ui.py autoteste`, `reproduzir.py --autoteste`, `bash -n` em todos os `.sh`; `ensaio.py montar T01` gerou o WAV de 92 s, que o `sincronizar.energias_do_wav` lê e cujo nome o `timestamps.validar_nome` aceita. **Não provado no aparelho** (ele saiu do adb durante o PLAN): cada script de aparelho é provado no primeiro uso, na Fase C, com saída esperada e plano B escritos lá.
- `vosk` 0.3.44: wheel baixado e conferido (sha256), código lido, nada instalado (A7).

## Arquivos que mudam

| Arquivo | Mudança | Requisito / spec | Agente |
|---|---|---|---|
| `etapa0/ferramentas/sincronizar.py` | novo | spec D3, D4; TESTES-ETAPA0 Métricas (deslocamento) | motor-voz |
| `etapa0/ferramentas/folha.py` | novo | spec D5, D6, D7; TESTES-ETAPA0 Folha de eventos | motor-voz |
| `etapa0/ferramentas/veredito.py` | novo | spec C1, C2, D9, E2, E3, F1, F2, H1; TRD Etapa 0, Critérios de saída | motor-voz |
| `etapa0/ferramentas/nivel.py` | novo | spec F4, D12; TRD Motor de voz (limiar) | motor-voz |
| `etapa0/ferramentas/testes/sinal.py` | novo (sinais sintéticos) | spec D3 | motor-voz |
| `etapa0/ferramentas/testes/test_sincronizar.py`, `test_folha.py`, `test_veredito.py`, `test_nivel.py`, `test_cli_0005.py` | novos | REVIEW-QUESTIONS Q2, Q3, Q4, Q8 | motor-voz |
| `work/0005-etapa0-medicao/scripts/*` (10 arquivos) | novos | spec D25, A–G | qa-publicacao |
| `work/0005-etapa0-medicao/evidencias/**` | saídas dos comandos | spec, critérios | qa-publicacao |
| `etapa0/{gravacoes,trajetorias,eventos,esperado,relatorios}/**` | dados das rodadas | spec C, D, E, F, G, H | qa-publicacao |
| `work/0005-etapa0-medicao/{code_review,test_result}.md` | artefatos do ciclo | PROCESS | revisor, qa-publicacao |
| `registers/opr_register.md` | OPR-041 e as linhas medidas | spec H5 | arquiteto (041), qa-publicacao |

Nada em `etapa0/core`, `etapa0/app`, `docs/`, manifesto, contratos ou nos `.py` da 0004.

## Ordem e paralelismo

```
Fase A  orquestrador: commita o plano; cria o worktree das ferramentas
Fase B  motor-voz (worktree work/0005-ferramentas): Tarefas 1–6  ──► revisor: code_review.md  ──► merge
Fase C  qa-publicacao (árvore principal): Tarefa Q + base + aparelho + ensaio + frio + estratégia     (em paralelo com a B;
        C11 (B8) e qualquer número das ferramentas novas só depois do merge APROVADO)
Fase D  S1 com o humano
Fase E  D1 (venv + modelo grande) e verdade-terreno da S1
Fase F  folha, métricas e revisão de escuta da S1 (humano) ──► veredito S1 ──► decisão de seguir
Fase G  calibração por reprodução (S1), latências, nível
Fase H  S2 (+ verdade, folha, revisão, veredito, F2 da S2)
Fase I  S3 (idem)
Fase J  bateria (humano)
Fase K  confirmação ao vivo (só se D28) ; tablet = lacuna
Fase L  relatório final, test_result, OPR, H4, portão do push (Pergunta 2) e portão 3
```

O `motor-voz` e o `qa-publicacao` trabalham em paralelo em árvores diferentes; a interface não participa. As Fases D a J dependem do humano e podem ter dias entre elas; a G roda enquanto o humano não está.

## Testes primeiro

Todos em `etapa0/ferramentas/testes/`, escritos e vistos falhando (`ModuleNotFoundError` da ferramenta) antes do código de cada tarefa:

- `test_sincronizar.py` (32): `DeslocamentoTest` (10 ms com ganho e piso de outro microfone, reprodução com faixa, WAV que não cobre, bordas da faixa), `PalmaTest` (confere e registra, palma tardia até 4.000 ms, limite de 200 ms por mock, palma só com as linhas em volta, faixa errada), `BuscaTest` (empate menor e meio do patamar, tolerância do empate, WAV em silêncio), `DerivaTest` (1% passa de 200, deriva pequena passa, corte na referência, borda da busca, metade sem variância), `EntradaTest` (10 casos de `level.csv` inválido, limites aceitos, mesmo instante, WAV estéreo/44,1 kHz/corrompido/ausente, fatias de 10 ms, bloco fora do WAV), `SaidaTest`.
- `test_folha.py` (24): improviso do T04 (inclusive bordas exatas e palavra que atravessa), pulo do T05 (índice 47, `pulo_fim` sem tempo, token pulado com tempo), silêncio do T06, L30 com os três eventos e tempos exatos aceito pelo `metricas.py`, entradas inválidas, nota repetida, sobreposição.
- `test_veredito.py` (51): ligações D9, salão, reprovação, recuo, ausentes, variante do T02, L30 opcional na S2, sensibilidade em ±200 e pela sincronização, eventos exigidos (Q3), classes e janela de toque (E3), revisão pendente, configuração D10 chave a chave, proveniência (Q8), lista de oficiais, final (S2 e S3 ausentes, reprovação vence), reprodução (`--sufixo`, `--config`), e os campos lidos num JSON real do `metricas.py` (Q4).
- `test_nivel.py` (11): níveis por intervalo com bordas, candidato por parcial, causa e nível no limite, Final, improviso, várias gravações, nenhum nível acima, p95, entradas inválidas.
- `test_cli_0005.py` (3): `--help` e código 2 pelo processo; cadeia sincronizar → folha → metricas com o mesmo `d`; veredito rodada e final pelo processo.

### Critérios do spec → passo → comando

| Critério | Onde | Comando de verificação (da raiz) | Sucesso |
|---|---|---|---|
| A1 | Fase C1, e de novo depois do merge | `cd etapa0 && ./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest assembleDebug assembleRelease` e `python3 etapa0/ferramentas/testar.py` | BUILD SUCCESSFUL; `testar.py: 149` (antes do merge) e `270 testes, 0 falhas, 0 erros, 0 pulados` (depois); contagens e `git rev-parse HEAD` em `evidencias/a1-*.txt` |
| A2 | C2 | `cd etapa0 && ./gradlew verify16kAlignment` | falha só pelas `.so` de 32 bits (OPR-024); saída inteira em `evidencias/a2-16k.txt` |
| A3 | C3 | `adb install -r`; `shasum -a 256` do APK construído e do puxado; `getprop`, `getconf PAGE_SIZE`, `MemTotal` | dois sha256 iguais; `evidencias/a3-*.txt` |
| A4 | início e fim de cada bloco | `scripts/aparelho.sh salvar`, `aplicar`, `restaurar` | `restaurado: igual ao antes` em todos os blocos; `evidencias/aparelho-ajustes.txt` |
| B1 | C5 | `pm clear`, `ui.py tem-sistema`, `ui.py tocar-sistema`, `ui.py linhas beginSession`, `privacidade.sh logcat` | diálogo achado; `instalação`/`prepare` registrados; `FATAL EXCEPTION: 0` |
| B2 | C6 | `adb shell dumpsys package <pacote>` (bloco `requested permissions`) | só `android.permission.RECORD_AUDIO` |
| B3 | C7 | `adb shell dumpsys media.audio_flinger` durante a leitura ao vivo + mediana dos intervalos do `level.csv` | trilha de entrada do pacote com 16000 Hz, 1 canal, PCM 16 bits; mediana ~200 ms |
| B4 | D (fim da S1) | script Python de C8 do runbook (B4) sobre `trajetorias/*_giorgio_sala_*.hypotheses.csv` | 0 palavras com `{`, `}`, `:`, `"` ou iguais a `partial`/`text`/`result`; ≥ 1 `final` com palavras por gravação |
| B5 | C8 | `privacidade.sh arquivos/comparar/logcat` em volta do ensaio ao vivo | `PASSA` nos dois |
| B6 | C9 | `input keyevent KEYCODE_HOME` + `dumpsys audio`; rotação por `settings put system user_rotation` + `ui.py` | pausado e sem captura; girado continua lendo, mesma sessão |
| B7 | C10 | APK de release + `reproduzir.py --ensaio` (ou T01 da S1) | cursor final = último token do T01 no `trajectory.csv` |
| B8 | C11 (após o merge) | `reproduzir.py --ensaio` + `sincronizar.py --faixa-ms -1000 1000` | `|d| <= 200`; `say -v '?'` anexado; teste do toque fora da tela registrado |
| C1, C2 | D, H, I | `take.sh puxar` (recusa sobrescrever; confere `text_id`) e `veredito.py rodada` (lista ausentes, confere meta D10) | nenhum ausente sem motivo; código ≠ 2 |
| C3 | L | `git lfs ls-files`, `du -ch etapa0/gravacoes/*.wav` | todos os WAV listados; total registrado; push só depois da Pergunta 2 |
| D1 | E | comandos do runbook E (venv, `pip download` + sha256, `curl`, `shasum`, `git status`) | sha256 do wheel = `029d0b3d…882801`; zip com 1.693.530.883 bytes; nada do modelo/venv no git; `timestamps.py` código 0 |
| D2 | E | `medir.sh verdade <nome>` | `alinhar.py` código 0; stderr em `evidencias/alinhar/` |
| D3 | B, revisão | `python3 etapa0/ferramentas/testar.py` + quebras S/F da tabela Q2 | 270 verdes; quebras vermelhas; `code_review.md` APROVADO |
| D4 | F | `medir.sh sinc` | `sinc.json` com `deslocamento_ms`, `palma.app_ms`, `palma.wav_pico_ms`, `deriva.deriva_ms` |
| D5 | F | `medir.sh folha` + `medir.sh metricas` | folha para T04, T05, T06, L30; `metricas.py` com código 0 ou 1 |
| D6 | F | `esperado/<nome>.revisao.md` (modelo no runbook F) | cada ponto com resposta e registro; `Revisão concluída: sim` |
| E1 | F, G | `medir.sh metricas` (d, d−200, d+200) | três JSON por gravação; códigos no `diario.txt` |
| E2, E3 | F, H, I | `veredito.py rodada --json` | tabela por gravação e critério com número, regra, n, estado; classes e janela de toque |
| F1 | G | `reproduzir.py` × 3 presets + `veredito.py rodada --sufixo … --config preset=…` | uma tabela por preset; `meta.txt` confere |
| F2 | G, H | idem com `--vocab on` | tabela lado a lado; tamanho da lista do painel |
| F3 | C13 | `reproduzir.py --ensaio … --ciclos 10 --estrategia novo|reset` | 10 `beginSession` por estratégia; mediana, p95, máximo, n |
| F4 | G | `nivel.py` sobre as oficiais; reprodução com o candidato | tabela; movimentos eliminados e CS1 da sala intacto, ou recomendação 0 |
| F5 | L | leitura dos relatórios do `metricas.py` (episódios) | um parágrafo por provisório |
| F6 | K | sessão curta ou declaração | um dos dois no relatório |
| G1 | G | `reproduzir.py --painel` (T01, L30) + `metricas.py --hipoteses` | mediana, p95, máximo, n por gravação; taxa obtida declarada (A12) |
| G2 | C12 | `frio.sh quente 10`, `frio.sh primeira 3` | mediana, máximo, n |
| G3 | D | `memoria.sh S1 120` em segundo plano | n ≥ 45 amostras; pico e mediana de PSS e RSS |
| G4 | J | runbook J | duração ≥ 45 min; nível e charge counter antes e depois |
| G5 | G | latência 2 do L30 em quartos do texto | tabela para o OPR-013 |
| H1–H5 | L | `veredito.py final`; `git status`; `git diff main --stat -- etapa0/core/src/main etapa0/app docs/` | relatório completo; diff vazio; OPR atualizados |

### Quebras propositais (Q2)

Rodadas pelo runner da prova (cada troca aplicada sozinha; `PYTHONDONTWRITEBYTECODE=1 python3 -B etapa0/ferramentas/testar.py`; vermelho = algum teste falha). O revisor e o QA repetem pelo menos as marcadas com ★, uma por ferramenta e por limiar.

| Id | Arquivo | De | Para | O que quebra | Resultado |
|---|---|---|---|---|---|
| S01 ★ | `sincronizar.py` | `TOLERANCIA_MS = 200` | `TOLERANCIA_MS = 190` | limite de 200 ms (palma e deriva) | vermelho |
| S02 | `sincronizar.py` | `abs(resultado_deriva["deriva_ms"]) > TOLERANCIA_MS` | `abs(resultado_deriva["deriva_ms"]) >= TOLERANCIA_MS` | deriva: > vira >= | vermelho |
| S03 | `sincronizar.py` | `abs(resultado_deriva["deriva_ms"]) > TOLERANCIA_MS` | `resultado_deriva["deriva_ms"] > TOLERANCIA_MS` | deriva sem abs | vermelho |
| S04 ★ | `sincronizar.py` | `abs(d_palma - deslocamento) <= TOLERANCIA_MS` | `abs(d_palma - deslocamento) < TOLERANCIA_MS` | palma: <= vira < | vermelho |
| S05 | `sincronizar.py` | `abs(d_palma - deslocamento) <= TOLERANCIA_MS` | `d_palma - deslocamento <= TOLERANCIA_MS` | palma sem abs | vermelho |
| S06 | `sincronizar.py` | `PALMA_ATE_MS = 4000` | `PALMA_ATE_MS = 3000` | janela da palma | vermelho |
| S07 | `sincronizar.py` | `if t <= ate_ms]` | `if t < ate_ms]` | palma: linha no limite | vermelho |
| S08 ★ | `sincronizar.py` | `_teto10(BLOCO_MS - t_min)` | `_teto10(-t_min)` | faixa padrão sem o bloco | vermelho |
| S09 | `sincronizar.py` | `_piso10(duracao_ms - t_max)` | `_piso10(duracao_ms - t_min)` | faixa padrão: t_max -> t_min | vermelho |
| S10 | `sincronizar.py` | `lo, hi = _teto10(BLOCO_MS - t_min), _piso10(duracao_ms - t_max)` | `lo, hi = _piso10(BLOCO_MS - t_min), _teto10(duracao_ms - t_max)` | arredondamento da faixa trocado | vermelho |
| S11 | `sincronizar.py` | `    if lo > hi:⏎        raise ErroEntrada("o WAV` | `    if lo > hi + 10:⏎        raise ErroEntrada("o WAV` | faixa vazia aceita | vermelho |
| S12 | `sincronizar.py` | `return 20 * math.log10(max(rms, PISO))` | `return rms` | correlação linear, sem dB | vermelho |
| S13 | `sincronizar.py` | `PISO = 1 / 32768` | `PISO = 1e-12` | piso do dB | vermelho |
| S14 ★ | `sincronizar.py` | `finos = range(max(lo, d - PASSO_GROSSO_MS), min(hi, d + PASSO_GROSSO_MS) + 1, SUB_MS)` | `finos = [d]` | sem grade fina | vermelho |
| S15 | `sincronizar.py` | `finos = range(max(lo, d - PASSO_GROSSO_MS)` | `finos = range(max(lo, d)` | grade fina só para cima | vermelho |
| S16 | `sincronizar.py` | `empatados[(len(empatados) - 1) // 2] if meio_do_empate else empatados[0]` | `empatados[0]` | palma sem o meio do patamar | vermelho |
| S17 | `sincronizar.py` | `empatados[(len(empatados) - 1) // 2] if meio_do_empate else empatados[0]` | `empatados[(len(empatados) - 1) // 2] if meio_do_empate else empatados[-1]` | empate global: maior | vermelho |
| S18 | `sincronizar.py` | `empatados[(len(empatados) - 1) // 2]` | `empatados[len(empatados) // 2]` | meio do patamar par | vermelho |
| S19 | `sincronizar.py` | `EMPATE = 1e-9` | `EMPATE = 1e-3` | tolerância do empate | vermelho |
| S20 | `sincronizar.py` | `EMPATE = 1e-9` | `EMPATE = 0` | empate exato | vermelho |
| S21 | `sincronizar.py` | `        if syy <= 0:⏎            return None⏎` | (remove) | y constante aceito | vermelho |
| S22 | `sincronizar.py` | `        if self.sxx <= 0:⏎            return None⏎` | `        if self.sxx < 0:⏎            return None⏎` | x constante aceito | vermelho |
| S23 | `sincronizar.py` | `metade = len(linhas) // 2` | `metade = len(linhas) // 4` | metades desiguais | vermelho |
| S24 ★ | `sincronizar.py` | `saida["deriva_ms"] = round((d_ini - d_fim) * duracao / (c_fim - c_ini))` | `saida["deriva_ms"] = d_ini - d_fim` | deriva sem estender | vermelho |
| S25 | `sincronizar.py` | `saida["deriva_ms"] = round((d_ini - d_fim) * duracao` | `saida["deriva_ms"] = round((d_fim - d_ini) * duracao` | sinal da deriva | vermelho |
| S26 ★ | `sincronizar.py` | `partes.append((d, serie.centro, d is None or d in (lo, hi)))` | `partes.append((d, serie.centro, d is None))` | borda da metade aceita | vermelho |
| S27 | `sincronizar.py` | `partes.append((d, serie.centro, d is None or d in (lo, hi)))` | `partes.append((d, serie.centro, d is None or d == lo))` | só a borda de baixo | vermelho |
| S28 | `sincronizar.py` | `lo, hi = deslocamento - JANELA_LOCAL_MS, deslocamento + JANELA_LOCAL_MS⏎        d, _ = buscar(serie, wav, lo, hi)` | `lo, hi = deslocamento - 2 * JANELA_LOCAL_MS, deslocamento + 2 * JANELA_LOCAL_MS⏎        d, _ = buscar(serie, wav, lo, hi)` | janela da metade | vermelho |
| S29 | `sincronizar.py` | `indeterminada = borda_ini or borda_fim` | `indeterminada = borda_ini and borda_fim` | indeterminada: or -> and | vermelho |
| S30 | `sincronizar.py` | `a = min(max(fim - SUBS_BLOCO, 0), self.n)` | `a = min(max(fim - SUBS_BLOCO + 1, 0), self.n)` | bloco de 190 ms | vermelho |
| S31 | `sincronizar.py` | `b = min(max(fim, 0), self.n)` | `b = max(fim, 0)` | fim fora do WAV | vermelho |
| S32 | `sincronizar.py` | `return SUBS_BLOCO <= fim <= self.n` | `return SUBS_BLOCO < fim <= self.n` | dentro: borda de baixo | vermelho |
| S33 | `sincronizar.py` | `return SUBS_BLOCO <= fim <= self.n` | `return SUBS_BLOCO <= fim < self.n` | dentro: borda de cima | vermelho |
| S34 | `sincronizar.py` | `for k in range(0, len(a) - AMOSTRAS_SUB + 1, AMOSTRAS_SUB):` | `for k in range(0, len(a), AMOSTRAS_SUB):` | fatia incompleta entra | vermelho |
| S35 | `sincronizar.py` | `return _db(math.sqrt(self.energia(fim) / AMOSTRAS_BLOCO) / 32768)` | `return _db(math.sqrt(self.energia(fim) / AMOSTRAS_SUB) / 32768)` | RMS do WAV por fatia | verde (equivalente) |
| S38 | `sincronizar.py` | `if t < 0 or not 0 <= rms <= 1:` | `if not 0 <= rms <= 1:` | t negativo aceito | vermelho |
| S39 | `sincronizar.py` | `if t < 0 or not 0 <= rms <= 1:` | `if t < 0 or not 0 <= rms:` | rms > 1 e inf aceitos | vermelho |
| S52 | `sincronizar.py` | `if t < 0 or not 0 <= rms <= 1:` | `if t < 0 or not rms <= 1:` | rms negativo aceito | vermelho |
| S53 | `sincronizar.py` | `self.bases = [t // SUB_MS for t, _ in linhas]` | `self.bases = [t // SUB_MS + 20 for t, _ in linhas]` | bloco deslocado 200 ms | vermelho |
| S40 | `sincronizar.py` | `if saida and t < saida[-1][0]:` | `if saida and t <= saida[-1][0]:` | t repetido recusado | vermelho |
| S41 | `sincronizar.py` | `if saida and t < saida[-1][0]:` | `if False:` | t decrescente aceito | vermelho |
| S42 | `sincronizar.py` | `MIN_LINHAS = 4` | `MIN_LINHAS = 3` | mínimo de linhas | vermelho |
| S44 | `sincronizar.py` | `"linhas_fora_do_wav": sum(1 for b in serie.bases if not wav.dentro(b + s)),` | `"linhas_fora_do_wav": 0,` | linhas fora do WAV | vermelho |
| S45 | `sincronizar.py` | `"codigo": 1 if motivos else 0,` | `"codigo": 0,` | código 1 | vermelho |
| S46 | `sincronizar.py` | `w.getframerate() != TAXA` | `w.getframerate() < TAXA` | taxa maior aceita | vermelho |
| S47 | `sincronizar.py` | `w.getnchannels() != 1` | `w.getnchannels() > 2` | estéreo aceito | vermelho |
| S48 | `sincronizar.py` | `    if not resultado_palma["confere"]:⏎` | `    if False:⏎` | palma ignorada | vermelho |
| S49 | `sincronizar.py` | `candidatas = [(t, rms) for t, rms in linhas if t <= ate_ms]` | `candidatas = list(linhas)` | palma no texto todo | vermelho |
| S50 | `sincronizar.py` | `locais = [linha for linha in linhas if abs(linha[0] - t_app) <= JANELA_LOCAL_MS]` | `locais = linhas` | palma com todas as linhas | vermelho |
| S51 | `sincronizar.py` | `    d_palma, _ = buscar(Serie(locais), wav, lo, hi, meio_do_empate=True)` | `    d_palma = deslocamento` | palma sempre confere | vermelho |
| F01 ★ | `folha.py` | `if p.inicio >= fim_antes and p.fim <= inicio_depois]` | `if p.inicio > fim_antes and p.fim <= inicio_depois]` | improviso: >= vira > | vermelho |
| F02 | `folha.py` | `if p.inicio >= fim_antes and p.fim <= inicio_depois]` | `if p.inicio >= fim_antes and p.fim < inicio_depois]` | improviso: <= vira < | vermelho |
| F03 | `folha.py` | `if p.inicio >= fim_antes and p.fim <= inicio_depois]` | `if p.inicio >= fim_antes]` | improviso sem o fim | vermelho |
| F04 ★ | `folha.py` | `if p.inicio > fim_antes and p.fim < inicio_depois]` | `if p.inicio >= fim_antes and p.fim < inicio_depois]` | silêncio: > vira >= | vermelho |
| F05 | `folha.py` | `if p.inicio > fim_antes and p.fim < inicio_depois]` | `if p.inicio > fim_antes and p.fim <= inicio_depois]` | silêncio: < vira <= | vermelho |
| F06 | `folha.py` | `if t_app < 0:` | `if t_app <= 0:` | t_app zero recusado | vermelho |
| F07 | `folha.py` | `if t_app < 0:` | `if False:` | t_app negativo aceito | vermelho |
| F08 ★ | `folha.py` | `t_app = t_wav - deslocamento` | `t_app = t_wav + deslocamento` | sinal do deslocamento | vermelho |
| F09 | `folha.py` | `for k in range(i, -1, -1):` | `for k in range(i - 1, -1, -1):` | token antes pula o próprio | vermelho |
| F10 | `folha.py` | `for k in range(i, len(esperado)):` | `for k in range(i + 1, len(esperado)):` | token depois pula o próprio | vermelho |
| F11 | `folha.py` | `        if inicio_depois < fim_antes:⏎` | `        if False:⏎` | sobreposição aceita | vermelho |
| F12 | `folha.py` | `marcar(dentro[0].inicio, "improviso_inicio"` | `marcar(dentro[0].fim, "improviso_inicio"` | início do improviso | vermelho |
| F13 | `folha.py` | `marcar(dentro[-1].fim, "improviso_fim"` | `marcar(dentro[-1].inicio, "improviso_fim"` | fim do improviso | vermelho |
| F14 ★ | `folha.py` | `marcar(fim_antes, "pulo_inicio", str(pulado)` | `marcar(fim_antes, "pulo_inicio", str(b)` | índice do pulo | vermelho |
| F15 | `folha.py` | `marcar(inicio_depois, "pulo_fim"` | `marcar(esperado[kb].fim, "pulo_fim"` | pulo_fim no fim do token | vermelho |
| F16 | `folha.py` | `marcar(fim_antes, "silencio_inicio"` | `marcar(esperado[ka].inicio, "silencio_inicio"` | silêncio começa no início do token | vermelho |
| F17 | `folha.py` | `falados = [i for i in range(pulado, b) if esperado[i].fim is not None]` | `falados = [i for i in range(pulado + 1, b) if esperado[i].fim is not None]` | primeiro pulado com tempo | vermelho |
| F18 | `folha.py` | `            if falados:⏎` | `            if False:⏎` | pulados com tempo ignorados | vermelho |
| F19 | `folha.py` | `            if dentro:⏎                avisos.append(f"aviso: silencio` | `            if False:⏎                avisos.append(f"aviso: silencio` | palavra no silêncio ignorada | vermelho |
| F20 | `folha.py` | `            if pedido != achado:⏎` | `            if False:⏎` | fronteira aproximada sem aviso | vermelho |
| F21 | `folha.py` | `    return 1 if avisos else 0` | `    return 0` | código 1 | vermelho |
| F22 ★ | `folha.py` | `seguintes = [t.indice for t in tokens if t.inicio >= pos]` | `seguintes = [t.indice for t in tokens if t.inicio > pos]` | depois: >= vira > | vermelho |
| F23 | `folha.py` | `"T06": (("silencio", "// SILÊNCIO", "na_nota"),),` | `"T06": (("silencio", "// SILÊNCIO", "fim_do_paragrafo_seguinte"),),` | regra do T06 | vermelho |
| F24 | `folha.py` | `("improviso", "// L30 — Minuto 8 (", "fim_do_paragrafo_seguinte"),` | `("improviso", "// L30 — Minuto 8 (", "na_nota"),` | regra do improviso do L30 | vermelho |
| F25 | `folha.py` | `("silencio", "// L30 — Minuto 18 (", "fim_do_paragrafo_seguinte"),` | `("silencio", "// L30 — Minuto 18 (", "na_nota"),` | regra do silêncio do L30 | vermelho |
| F26 | `folha.py` | `("pulo", "// L30 — Minuto 15 (", "paragrafo_seguinte"),` | `("pulo", "// L30 — Minuto 12 (", "paragrafo_seguinte"),` | nota do pulo do L30 | vermelho |
| F27 | `folha.py` | `        if len(notas) != 1:⏎` | `        if not notas:⏎` | nota repetida aceita | vermelho |
| F28 | `folha.py` | `    if palavras != [roteiro.normalizar(texto[t.inicio:t.fim + 1]) for t in tokens]:⏎` | `    if False:⏎` | esperado de outro texto | vermelho |
| F29 | `folha.py` | `            a, b, pulado = antes(seguinte.ini), depois(seguinte.fim), depois(seguinte.ini)` | `            a, b, pulado = antes(seguinte.ini), depois(seguinte.ini), depois(seguinte.ini)` | pulo sem pular | vermelho |
| V01 ★ | `veredito.py` | `("m200", d - TOLERANCIA_MS), ("p200", d + TOLERANCIA_MS)` | `("m200", d + TOLERANCIA_MS), ("p200", d - TOLERANCIA_MS)` | m200/p200 trocados | vermelho |
| V02 ★ | `veredito.py` | `"CS1": ("T01", "T10", "L30"),` | `"CS1": ("T01", "L30"),` | T10 fora do CS1 | vermelho |
| V03 | `veredito.py` | `"CS2": ("L30", "T02", "T03", "T04", "T08", "T09"),` | `"CS2": ("L30", "T02", "T03", "T04", "T08"),` | T09 fora do CS2 | vermelho |
| V04 | `veredito.py` | `"CS3": ("T05", "L30"),` | `"CS3": ("T05",),` | L30 fora do CS3 | vermelho |
| V05 | `veredito.py` | `"CS4": ("T06", "T07", "L30"),` | `"CS4": ("T06", "L30"),` | T07 fora do CS4 | vermelho |
| V06 | `veredito.py` | `SO_SALA = ("CS1",)` | `SO_SALA = ()` | CS1 no salão | vermelho |
| V07 | `veredito.py` | `("CS4", "L30"): ("pausa", "silencio"),` | `("CS4", "L30"): ("pausa",),` | L30 sem silêncio exigido | vermelho |
| V08 | `veredito.py` | `("CS2", "T04"): ("improviso",), ("CS2", "L30"): ("improviso",),` | `("CS2", "T04"): ("improviso",),` | L30 sem improviso exigido | vermelho |
| V09 | `veredito.py` | `("CS4", "T06"): ("silencio",), ("CS4", "T07"): ("pausa",),` | `("CS4", "T06"): ("silencio",),` | T07 sem pausa exigida | vermelho |
| V10 | `veredito.py` | `if a["amostras"] == 0:` | `if a["amostras"] < 0:` | CS1 com zero amostras | vermelho |
| V11 | `veredito.py` | `if m["amostras"]["leitura_normal"] == 0:` | `if False:` | CS2 sem leitura | vermelho |
| V12 | `veredito.py` | `if m["cursor_final"]["cursor"] == 0:` | `if False:` | CS2 com cursor parado | vermelho |
| V13 ★ | `veredito.py` | `ruim = c["salto_errado"]["veredito"] == "REPROVA" or c["recuo"]["veredito"] == "REPROVA"` | `ruim = c["salto_errado"]["veredito"] == "REPROVA"` | recuo fora do CS2 | vermelho |
| V14 | `veredito.py` | `if r == "NAO_SE_APLICA":⏎            return NAO_MEDIDO` | `if False:⏎            return NAO_MEDIDO` | CS3 sem pulo passa | vermelho |
| V15 | `veredito.py` | `movimentos = [mv for mv in c["salto_errado"]["por_movimento"] if mv["intervalo"] in ("silencio", "pausa")]⏎    return (REPROVOU` | `movimentos = [mv for mv in c["salto_errado"]["por_movimento"] if mv["intervalo"] in ("silencio",)]⏎    return (REPROVOU` | pausa fora do CS4 | vermelho |
| V16 | `veredito.py` | `movimentos = [mv for mv in c["salto_errado"]["por_movimento"] if mv["intervalo"] in ("silencio", "pausa")]⏎    return (REPROVOU` | `movimentos = list(c["salto_errado"]["por_movimento"])⏎    return (REPROVOU` | improviso no CS4 | vermelho |
| V17 ★ | `veredito.py` | `sensivel = estado != NAO_MEDIDO and (any(v != estado for v in vizinhos) or bool(sinc["motivos"]))` | `sensivel = estado != NAO_MEDIDO and any(v != estado for v in vizinhos)` | sincronização ignorada | vermelho |
| V18 | `veredito.py` | `sensivel = estado != NAO_MEDIDO and (any(v != estado for v in vizinhos) or bool(sinc["motivos"]))` | `sensivel = estado != NAO_MEDIDO and (vizinhos[1] != estado or bool(sinc["motivos"]))` | d - 200 ignorado | vermelho |
| V19 | `veredito.py` | `sensivel = estado != NAO_MEDIDO and (any(v != estado for v in vizinhos) or bool(sinc["motivos"]))` | `sensivel = estado != NAO_MEDIDO and (vizinhos[0] != estado or bool(sinc["motivos"]))` | d + 200 ignorado | vermelho |
| V20 | `veredito.py` | `sensivel = estado != NAO_MEDIDO and (` | `sensivel = (` | sensível em não medido | vermelho |
| V21 | `veredito.py` | `if estado == PASSOU and sensivel:` | `if False:` | sensível conta como passou | vermelho |
| V22 ★ | `veredito.py` | `if estado in (PASSOU, REPROVOU, SENSIVEL) and not revisada:` | `if estado in (PASSOU, REPROVOU) and not revisada:` | sensível sem revisão | vermelho |
| V23 | `veredito.py` | `if estado in (PASSOU, REPROVOU, SENSIVEL) and not revisada:` | `if estado in (PASSOU, SENSIVEL) and not revisada:` | reprovado sem revisão | vermelho |
| V24 | `veredito.py` | `REVISAO_CONCLUIDA in revisao.read_text(encoding="utf-8").splitlines()` | `"Revisão concluída" in revisao.read_text(encoding="utf-8")` | linha da revisão frouxa | vermelho |
| V25 ★ | `veredito.py` | `    if any(e == REPROVOU for _, e in estados):⏎        return REPROVOU` | `    if all(e == REPROVOU for _, e in estados):⏎        return REPROVOU` | agregar: any -> all | vermelho |
| V26 | `veredito.py` | `    if all(e == PASSOU for _, e in estados):⏎        return PASSOU, []` | `    if any(e == PASSOU for _, e in estados):⏎        return PASSOU, []` | agregar: all -> any | vermelho |
| V27 | `veredito.py` | `estados += [(f"{t}{' variante ' + v if v else ''} (ausente)", NAO_MEDIDO) for t, v in ausentes⏎                    if ligado(c, t, amb)]` | `estados += []` | ausentes ignorados | vermelho |
| V28 | `veredito.py` | `EXIGIDAS_FINAL = {"CS1": ("S1", "S2"),` | `EXIGIDAS_FINAL = {"CS1": ("S1",),` | segunda voz não exigida | vermelho |
| V29 | `veredito.py` | `"CS2": ("S1", "S3"), "CS3": ("S1", "S3"), "CS4": ("S1", "S3")}` | `"CS2": ("S1",), "CS3": ("S1", "S3"), "CS4": ("S1", "S3")}` | salão não exigido no CS2 | vermelho |
| V30 ★ | `veredito.py` | `        elif not motivos and estados and all(e == PASSOU for _, e in estados):` | `        elif estados and all(e == PASSOU for _, e in estados):` | rodada ausente passa | vermelho |
| V31 | `veredito.py` | `if papel in sufixos and arquivo != dono.get(papel, nome) + sufixos[papel]:` | `if False:` | arquivo de outra gravação | vermelho |
| V32 ★ | `veredito.py` | `if not caminho.is_file() or _sha256(caminho) != e["sha256"]:` | `if not caminho.is_file():` | sha256 ignorado | vermelho |
| V33 | `veredito.py` | `    if proveniencia[0] != proveniencia[1] or proveniencia[0] != proveniencia[2]:` | `    if proveniencia[0] != proveniencia[1]:` | p200 com outros arquivos | vermelho |
| V34 | `veredito.py` | `    conferir_entradas(sinc["entradas"], pastas, nome, oficial)⏎` | (remove) | WAV e level não conferidos | vermelho |
| V35 | `veredito.py` | `if meta.get("text_id") != texto_id:` | `if False:` | text_id ignorado | vermelho |
| V36 | `veredito.py` | `"level_threshold": "0.0",` | (remove) | limiar fora da configuração oficial | vermelho |
| V37 | `veredito.py` | `"audio_source": "mic",` | (remove) | fonte fora da configuração oficial | vermelho |
| V38 | `veredito.py` | `lista = [("T01", "")] + [("T02", v) for v in ("1", "2", "3")]` | `lista = [("T01", "")] + [("T02", v) for v in ("1", "3")]` | variante 2 não exigida | vermelho |
| V39 | `veredito.py` | `"S2": ("voz2", "sala", False),` | `"S2": ("voz2", "sala", True),` | L30 exigido na S2 | vermelho |
| V40 | `veredito.py` | `if (m.group(2), m.group(3)) != (voz, amb):` | `if m.group(2) != voz:` | ambiente não conferido | vermelho |
| V41 | `veredito.py` | `if (texto_id, variante) in vistos:` | `if False:` | repetido aceito | vermelho |
| V43 | `veredito.py` | `"fracao_lida": round(cf["cursor"] / cf["ultimo_token"], 4)` | `"fracao_lida": round(cf["cursor"] / (cf["ultimo_token"] + 1), 4)` | fração lida | vermelho |
| V45 | `veredito.py` | `return 0 if all(v["estado"] in (PASSOU, NAO_SE_APLICA) for v in res["criterios"].values()) else 1` | `return 0 if all(v["estado"] in (PASSOU, NAO_SE_APLICA, NAO_MEDIDO) for v in res["criterios"].values()) else 1` | não medido com código 0 | vermelho |
| V46 | `veredito.py` | `return {k: len(faixas[k]) for k in ("improviso", "silencio", "pausa", "pulo")}` | `return {k: 1 for k in ("improviso", "silencio", "pausa", "pulo")}` | eventos não contados | vermelho |
| V47 ★ | `veredito.py` | `dono = {"esperado": oficial or nome, "wav": oficial or nome}` | `dono = {"esperado": nome, "wav": oficial or nome}` | reprodução com esperado próprio | vermelho |
| V48 | `veredito.py` | `    config = config or CONFIG_OFICIAL⏎` | `    config = CONFIG_OFICIAL⏎` | --config ignorado | vermelho |
| V49 | `veredito.py` | `revisao = pastas["esperado"] / f"{oficial}.revisao.md"` | `revisao = pastas["esperado"] / f"{nome}.revisao.md"` | revisão da reprodução | vermelho |
| V50 | `veredito.py` | `        if r.get("sufixo") or r.get("config") != CONFIG_OFICIAL:⏎` | `        if False:⏎` | final aceita reprodução | vermelho |
| V51 | `veredito.py` | `            if args.sufixo and not re.fullmatch(r"(_[a-z0-9]+)+", args.sufixo):` | `            if False:` | sufixo com hífen | vermelho |
| V52 | `veredito.py` | `                if not sep or chave not in CONFIG_OFICIAL:` | `                if not sep:` | chave de configuração desconhecida | vermelho |
| N01 ★ | `nivel.py` | `if any(a <= t < b for a, b, _ in faixas[k])])` | `if any(a < t < b for a, b, _ in faixas[k])])` | intervalo: começo aberto | vermelho |
| N02 | `nivel.py` | `if any(a <= t < b for a, b, _ in faixas[k])])` | `if any(a <= t <= b for a, b, _ in faixas[k])])` | intervalo: fim fechado | vermelho |
| N03 | `nivel.py` | `CORRIGIVEIS = ("silencio", "pausa")` | `CORRIGIVEIS = ("silencio", "pausa", "improviso")` | improviso pede limiar | vermelho |
| N04 | `nivel.py` | `CORRIGIVEIS = ("silencio", "pausa")` | `CORRIGIVEIS = ("silencio",)` | pausa não pede limiar | vermelho |
| N05 | `nivel.py` | `aceitas = sorted((h for h in hipoteses if h.aceita and h.kind in ("partial", "final")), key=lambda h: h.t)` | `aceitas = sorted((h for h in hipoteses if h.kind in ("partial", "final")), key=lambda h: h.t)` | recusadas como causa | vermelho |
| N06 | `nivel.py` | `aceitas = sorted((h for h in hipoteses if h.aceita and h.kind in ("partial", "final")), key=lambda h: h.t)` | `aceitas = sorted((h for h in hipoteses if h.aceita), key=lambda h: h.t)` | erro como causa | vermelho |
| N07 | `nivel.py` | `        if chave(item) <= t:⏎` | `        if chave(item) < t:⏎` | causa: <= vira < | vermelho |
| N08 ★ | `nivel.py` | `    limite = max(por_parcial)` | `    limite = min(por_parcial)` | limite: max -> min | vermelho |
| N09 ★ | `nivel.py` | `    acima = [rms for rms in todos_os_niveis if rms > limite]` | `    acima = [rms for rms in todos_os_niveis if rms >= limite]` | candidato: > vira >= | vermelho |
| N10 | `nivel.py` | `    candidato = min(acima)` | `    candidato = max(acima)` | candidato: min -> max | vermelho |
| N11 ★ | `nivel.py` | `if mv["causa_kind"] == "partial" and mv["causa_nivel"] is not None]` | `if mv["causa_nivel"] is not None]` | Final no candidato | vermelho |
| N12 | `nivel.py` | `t_app = mv["t_ms"] - d` | `t_app = mv["t_ms"]` | deslocamento ignorado | vermelho |
| N13 | `nivel.py` | `g["leitura_abaixo_do_candidato"] = None if c is None else sum(1 for rms in leitura if rms < c)` | `g["leitura_abaixo_do_candidato"] = None if c is None else sum(1 for rms in leitura if rms <= c)` | leitura abaixo: < vira <= | vermelho |
| N14 | `nivel.py` | `"p95": ordenados[math.ceil(0.95 * len(ordenados)) - 1]` | `"p95": ordenados[math.floor(0.95 * len(ordenados)) - 1]` | p95 com floor | vermelho |
| N15 | `nivel.py` | `    return 1 if tem_movimento else 0` | `    return 0` | código 1 | vermelho |
| N16 | `nivel.py` | `todos += [rms for _, rms in level]` | `todos += []` | candidato sem os níveis | vermelho |

Total: 145 quebras; 144 vermelhas; 1 equivalente.

**Equivalente:** S35 (RMS do WAV dividido por 160 em vez de 3.200): é uma constante multiplicando o RMS, que vira soma constante em dB, e a correlação de Pearson ignora isso. É a invariância a ganho que o método quer (A3).

**Lição das 0001–0004 aplicada:** cada limiar tem a troca de operador (`<`/`<=`, `>`/`>=`) e a troca do operando ou da variável parecida (`t_min`/`t_max`, `m200`/`p200`, `nome`/`oficial`, `inicio`/`fim`, `fim_antes`/`inicio_depois`, `d_ini`/`d_fim`). Cada guarda de entrada tem um teste que falha sem ela, e as bordas exatas (200 ms, 4.000 ms, `t_app = 0`, palavra encostada na fronteira) estão nos testes.

## Privacidade (primeira camada)

- **Caminho do texto:** só textos de `etapa0/textos/` são carregados no app (chips), lidos em voz alta, alinhados e exportados (`ExportFiles`, que já recusa texto que não é de teste, OPR-026). As ferramentas novas recebem só o `expected.csv`, o `words.csv`, os eventos e os relatórios, todos de texto de teste. O `folha.py` aceita só texto idêntico a um de `etapa0/textos/` (`roteiro.identificar_texto`). O humano lê do Mac (`open -a TextEdit etapa0/textos/<ID>.txt`), nunca de um discurso pessoal. O improviso e a fala de terceiros são inventados e neutros (roteiro).
- **Caminho do áudio:** o app não grava áudio (regra 3), e B5 confere isso no aparelho. O WAV de referência é gravado pelo QuickTime no Mac. O original `.m4a` fica na pasta do humano, fora do repositório; o agente não o apaga. Só entra no repositório `etapa0/gravacoes/<nome>.wav` (LFS, regra `*.wav` do `.gitattributes`), e só de texto de teste. O push espera a Pergunta 2: cota do LFS e consentimento escrito da `voz2`, guardado pelo humano fora do repositório, com uma linha "consentimento recebido em <data>" no `test_result.md`. O ensaio (voz sintética), o WAV concatenado da bateria e as capturas ficam em `$TMPDIR/tribuna-0005/`, fora do repositório, e são apagados na Fase L. Os WAV copiados para `/sdcard/Android/data/<pacote>/files/wav/` e as exportações no aparelho são apagados na Fase L.
- **Logs e dumps do aparelho:** A11. Só contagens e linhas do pacote vão para a evidência.
- **Dependências novas:** nenhuma no app nem nas ferramentas (só stdlib). O `vosk` 0.3.44 e o modelo GPLv3 ficam num venv e numa pasta do usuário (`~/.venvs/tribuna-etapa0`, `~/vosk-modelos/`), nunca no repositório nem no app. Nenhuma permissão nova; o manifesto não muda (B2 confere o instalado).
- **Rede:** o Mac usa a rede só no passo D1 (download) e no push. O aparelho fica em modo avião em todas as sessões (A9).
- **Q6 aplicada:** nenhum caminho novo persiste texto ou áudio do orador. Os nomes de arquivo levam só ID, pseudônimo (`giorgio`, `voz2`), ambiente e take; o nome real da segunda voz não entra no repositório. O painel e o `meta.txt` não têm texto do discurso.

## Verificação

```bash
# ferramentas (motor-voz, revisor, QA)
python3 etapa0/ferramentas/testar.py                       # 270 testes, 0 falhas, 0 erros, 0 pulados
PYTHONDONTWRITEBYTECODE=1 python3 -B etapa0/ferramentas/testar.py   # nas quebras Q2
python3 work/0005-etapa0-medicao/scripts/ui.py autoteste
python3 work/0005-etapa0-medicao/scripts/reproduzir.py X --preset normal --vocab off --estrategia novo --autoteste
for f in work/0005-etapa0-medicao/scripts/*.sh; do bash -n "$f"; done
# app (sem mudança; base de A1)
cd etapa0 && ./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest assembleDebug assembleRelease
# fim da tarefa (H4)
git status --short
git diff main --stat -- etapa0/core/src/main etapa0/app docs/      # vazio
git ls-files | grep -i -E "vosk-model-pt-fb|\.venvs|tribuna-0005"  # vazio
```

Sucesso significa: todos os critérios do spec com a evidência indicada na tabela, o relatório final com os oito critérios de saída preenchidos (PASSOU, REPROVOU ou NÃO MEDIDO) e o `test_result.md` dizendo o que não pôde ser medido.

## Riscos (e o que fazer se o Vosk ou o Android não se comportarem como o TRD supõe)

| Risco | Sinal | O que fazer |
|---|---|---|
| `uiautomator` não chega ao estado ocioso com o painel aberto (nível muda a cada 200 ms) | `ui.py` sai com 2: `could not get idle state` | Ler o painel só em Idle (depois do Parar), quando ele para de mudar. Para F3/G1, `--captura` (A12). Nada muda no app. |
| O diálogo de permissão do Android 16 tem outro texto | `ui.py tem-sistema` não acha `Durante o uso do app` | Usar o texto que o `tem-sistema "Permitir\|Allow\|uso do app\|esta vez"` listar; se nada aparecer, PARAR E PEDIR AO HUMANO que toque em "Permitir". |
| `cmd connectivity airplane-mode` ou `set_dnd` recusados sem root | `aparelho.sh aplicar` termina em `PARAR: o adb não conseguiu: …` | O humano liga à mão (mensagem pronta no runbook); o `restaurar` só repõe o que o adb pôs. |
| `svc power stayon usb` sem efeito e a tela apaga no L30 | `onStop` pausa a leitura (`events.csv` sem retomada; tela preta) | Antes do L30: `aparelho.sh` com `settings put system screen_off_timeout 3600000` (o mesmo da bateria) e restaurar no fim. O take perdido é refeito como `t<n+1>`. |
| O slider do limiar não para no valor | `ui.py limiar` sai com 1 | O humano arrasta o slider até o rótulo mostrar o valor; o `meta.txt` registra o valor exato usado. |
| Correlação fraca ao vivo (sala com eco, microfones muito diferentes) | `sincronizar.py` código 1 com "palma não confere" ou correlação < 0,5 | Ponto de escuta da palma (D7). Se a palma confirmar outro `d`, a gravação sai SENSÍVEL e não PASSOU. Não se força `d` à mão. |
| O Vosk grande (FalaBrasil) erra muito numa voz | muitos tokens sem tempo no `alinhar.py` (stderr) | Revisão de escuta nos trechos sem tempo vizinhos de veredito (D7). Se mais de ~10% dos tokens ficarem sem tempo, o relatório mostra a cobertura, e o critério daquela gravação pode sair NÃO MEDIDO por falta de verdade. Não se troca para o modelo pequeno (portão 6). |
| `vosk` 0.3.44 não carrega no macOS 27 (`libvosk.dyld` universal2 de 2022) | `timestamps.py` código 2 "o modelo não abriu" ou `OSError` no `import` | PARAR. Escalar ao arquiteto com a saída. Caminhos: outra versão com wheel para Mac (0.3.42), ou a opção B (que exige nova decisão humana). |
| O Android encerra o processo no L30 | `memoria.sh` com "processo morto"; app volta sem texto | Take refeito. O relatório registra (o PRD aceita o encerramento), e a memória do momento entra no G3. |
| Reprodução: os toques de pausa chegam fora de hora | `events.csv` da reprodução com pausa > 500 ms do instante oficial | Vale a pausa real da reprodução (o `metricas.py` usa os eventos dela). Se a fala de terceiros cair fora da pausa, a reprodução do T07/L30 sai "não comparável" na tabela de calibração. |
| Bateria: o S23 está a 100% e o carregamento interfere | `status: 5` (cheia) no início | Desconectar já faz o nível começar a cair; o charge counter dá a resolução fina. Se a corrida for interrompida (tela apagou, app pausou), refazer com o humano. |

---

## Tarefas de construção (motor-voz)

Trabalhe no worktree criado pelo orquestrador na Fase A:

```bash
git worktree add ../Tribuna-0005-ferramentas -b work/0005-ferramentas work/0005-etapa0-medicao
cd ../Tribuna-0005-ferramentas
python3 etapa0/ferramentas/testar.py        # esperado: testar.py: 149 testes, 0 falhas, 0 erros, 0 pulados
```

Todos os comandos abaixo rodam da raiz desse worktree.

### Tarefa 1 (motor-voz): `sincronizar.py` (spec D3, D4; A3)

**Arquivos:**
- Criar: `etapa0/ferramentas/testes/sinal.py`
- Criar: `etapa0/ferramentas/testes/test_sincronizar.py`
- Criar: `etapa0/ferramentas/sincronizar.py`

- [ ] **Passo 1: escrever os sinais sintéticos**

`etapa0/ferramentas/testes/sinal.py`:

```python
"""Sinais sintéticos para o sincronizar.py (spec 0005, D3): ruído em degraus, como sílabas e pausas, mais uma palma.

Nada aqui é voz nem gravação: amostras pseudoaleatórias com semente fixa, escritas só em pasta temporária.
"""

import math
import random
import wave

TAXA = 16000


def fala_sintetica(duracao_ms, semente, palma_ms=None, silencio_ate_ms=0):
    """Amostras int16. Trechos de 50 a 400 ms com amplitude de 0 (pausa) a 8.000; palma de 20 ms a 30.000.

    Até `silencio_ate_ms` só há silêncio (o tempo antes da leitura), salvo a palma.
    """
    rnd = random.Random(semente)
    amostras = []
    total = duracao_ms * TAXA // 1000
    while len(amostras) < total:
        trecho = rnd.randint(5, 40) * 160
        ms = len(amostras) * 1000 // TAXA
        amp = 0 if ms < silencio_ate_ms or rnd.random() < 0.3 else rnd.randint(500, 8000)
        amostras += [rnd.randint(-amp, amp) for _ in range(trecho)] if amp else [0] * trecho
    amostras = amostras[:total]
    if palma_ms is not None:
        i = palma_ms * TAXA // 1000
        for k in range(i, min(i + 320, total)):
            amostras[k] = 30000 if (k - i) % 2 == 0 else -30000
    return amostras


def escrever_wav(caminho, amostras, canais=1, taxa=TAXA):
    with wave.open(str(caminho), "wb") as w:
        w.setnchannels(canais)
        w.setsampwidth(2)
        w.setframerate(taxa)
        dados = bytearray()
        for a in amostras:
            for _ in range(canais):
                dados += int(a).to_bytes(2, "little", signed=True)
        w.writeframes(bytes(dados))
    return str(caminho)


def _rms(amostras, fim_amostra):
    """RMS normalizado do bloco de 3.200 amostras que termina em `fim_amostra` (fora do sinal = zero)."""
    ini = fim_amostra - 3200
    soma = 0
    for k in range(max(ini, 0), min(fim_amostra, len(amostras))):
        soma += amostras[k] * amostras[k]
    return math.sqrt(soma / 3200) / 32768


def level_csv(amostras, deslocamento_ms, ate_app_ms, ganho=1.0, piso=0.0, deriva_ppm=0, semente=7,
              extras=None, constante_depois_de_ms=None, salto=None):
    """level.csv como o app gravaria: linha a cada ~200 ms (jitter 0 a 30 ms) de 200 ms até `ate_app_ms`.

    Bloco da linha t_app = áudio do WAV em [t_wav − 200, t_wav), t_wav = t_app × (1 + ppm) + deslocamento.
    `extras`: {t_app: rms} trocados à mão (um pico só do aparelho). `constante_depois_de_ms`: microfone mudo.
    `salto`: (t_app, ms): depois de t_app o relógio do WAV pula ms (um corte na gravação de referência).
    """
    rnd = random.Random(semente)
    linhas = ["t_ms,rms"]
    t = 200
    while t <= ate_app_ms:
        t_app = t + rnd.randint(0, 30)
        t_wav = t_app * (1 + deriva_ppm / 1_000_000) + deslocamento_ms
        if salto and t_app > salto[0]:
            t_wav += salto[1]
        rms = min(1.0, ganho * _rms(amostras, round(t_wav * TAXA / 1000)) + piso)
        if constante_depois_de_ms is not None and t_app > constante_depois_de_ms:
            rms = 0.01
        if extras and t in extras:
            rms = extras[t]
        linhas.append(f"{t_app},{rms:.6f}")
        t += 200
    return "\n".join(linhas) + "\n"
```

- [ ] **Passo 2: escrever o teste**

`etapa0/ferramentas/testes/test_sincronizar.py`:

```python
"""sincronizar.py com sinais sintéticos (spec 0005, D3, D4; impl_plan 0005, A3).

Cenário base, como o roteiro do spec: QuickTime liga, 1.370 ms depois o orador toca Ouvir (t_app = 0), palma
2 s depois (WAV 3.370 ms), silêncio até a leitura (WAV 5.370 ms), WAV de 40 s. O level.csv sai do mesmo sinal,
com ganho e piso de outro microfone e jitter de 0 a 30 ms por linha.
"""

import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import sincronizar
from testes import sinal
from testes.apoio import Pasta, rodar

D = 1370
_TMP = None
AMOSTRAS = None
WAV = None


def setUpModule():
    global _TMP, AMOSTRAS, WAV
    _TMP = tempfile.TemporaryDirectory()
    AMOSTRAS = sinal.fala_sintetica(40000, 1, palma_ms=D + 2000, silencio_ate_ms=D + 4000)
    WAV = sinal.escrever_wav(Path(_TMP.name) / "T01_giorgio_sala_t1.wav", AMOSTRAS)


def tearDownModule():
    _TMP.cleanup()


def _rodar(level, *extra, wav=None):
    with Pasta() as p:
        caminho_json = str(p.dir / "s.json")
        codigo, out, err = rodar(sincronizar.main, [wav or WAV, p.arquivo("x.level.csv", level), *extra,
                                                    "--json", caminho_json])
        dados = json.loads(Path(caminho_json).read_text(encoding="utf-8")) if Path(caminho_json).exists() else None
    return codigo, dados, out, err


class DeslocamentoTest(unittest.TestCase):
    def test_D3_acha_o_deslocamento_ao_vivo_a_10_ms(self):
        codigo, dados, out, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25))
        self.assertEqual(0, codigo)
        self.assertLessEqual(abs(dados["deslocamento_ms"] - D), 10)
        self.assertNotEqual(0, dados["deslocamento_ms"] % 100, "a grade fina de 10 ms foi usada")
        self.assertFalse(dados["sensivel"])
        self.assertEqual([], dados["motivos"])
        self.assertEqual(0, dados["linhas_fora_do_wav"])
        self.assertIn("SINCRONIZADO", out)

    def test_D3_ganho_e_piso_de_outro_microfone_nao_mudam_o_deslocamento(self):
        for ganho, piso in ((0.05, 0.0), (1.0, 0.0), (0.05, 0.002)):
            with self.subTest(ganho=ganho, piso=piso):
                codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, ganho=ganho, piso=piso))
                self.assertEqual(0, codigo)
                self.assertLessEqual(abs(dados["deslocamento_ms"] - D), 10)

    def test_D3_reproducao_pela_fonte_wav_com_faixa(self):
        level = sinal.level_csv(AMOSTRAS, -40, 41000, ganho=0.5)
        codigo, dados, _, _ = _rodar(level, "--faixa-ms", "-1000", "1000")
        self.assertEqual(0, codigo)
        self.assertLessEqual(abs(dados["deslocamento_ms"] + 40), 10)
        self.assertEqual([-1000, 1000], dados["faixa_ms"])
        self.assertGreater(dados["linhas_fora_do_wav"], 0, "o app seguiu depois do fim do WAV")

    def test_D3_wav_que_nao_cobre_a_gravacao_sai_com_2(self):
        level = sinal.level_csv(AMOSTRAS, -40, 41000, ganho=0.5)
        codigo, dados, out, err = _rodar(level)
        self.assertEqual((2, None, ""), (codigo, dados, out))
        self.assertIn("não cobre", err)

    def test_faixa_padrao_nas_bordas(self):
        self.assertEqual((0, 1000), sincronizar.faixa_padrao(200, 39000, 40000))
        self.assertEqual((10, 10), sincronizar.faixa_padrao(195, 39985, 40000))
        self.assertEqual((0, 0), sincronizar.faixa_padrao(200, 39000, 39000))
        self.assertEqual((-1000, -1000), sincronizar.faixa_padrao(1200, 41000, 40000))
        with self.assertRaises(sincronizar.ErroEntrada):
            sincronizar.faixa_padrao(200, 39001, 39000)

    def test_faixa_invertida_sai_com_2(self):
        codigo, _, _, err = _rodar(sinal.level_csv(AMOSTRAS, D, 38000), "--faixa-ms", "500", "400")
        self.assertEqual(2, codigo)
        self.assertIn("MIN maior que MAX", err)


class PalmaTest(unittest.TestCase):
    def test_D4_palma_confere_e_fica_registrada(self):
        _, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25))
        p = dados["palma"]
        self.assertTrue(p["confere"])
        self.assertTrue(1800 <= p["app_ms"] <= 2230, p)
        self.assertTrue(D + 1800 <= p["wav_pico_ms"] <= D + 2030, p)
        self.assertLessEqual(abs(p["diferenca_ms"]), sincronizar.TOLERANCIA_MS)
        self.assertGreater(p["proeminencia_db"], 20)

    def test_D3_faixa_errada_palma_nao_confere_sai_com_1(self):
        codigo, dados, out, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25), "--faixa-ms", "3000", "3500")
        self.assertEqual(1, codigo)
        self.assertFalse(dados["palma"]["confere"])
        self.assertTrue(dados["sensivel"])
        self.assertTrue(any(m.startswith("palma não confere") for m in dados["motivos"]))
        self.assertIn("SENSÍVEL AO DESLOCAMENTO", out)

    def test_D3_palma_usa_so_as_linhas_em_volta_dela(self):
        # corte de 600 ms no WAV aos 6 s: o resto da gravação puxa o deslocamento global; a palma (2 s) não
        codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25, salto=(6000, 600)))
        self.assertLessEqual(abs(dados["deslocamento_ms"] - (D + 600)), 10)
        self.assertFalse(dados["palma"]["confere"])
        self.assertLessEqual(abs(dados["palma"]["deslocamento_ms"] - D), 200)
        self.assertEqual(1, codigo)

    def test_sem_linha_ate_o_limite_da_palma_nao_confere(self):
        codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000), "--palma-ate-ms", "100")
        self.assertEqual(1, codigo)
        self.assertIn("nenhuma linha do level.csv até 100 ms", dados["palma"]["motivo"])

    def test_D4_palma_tardia_ate_4000_ms_entra_no_padrao(self):
        amostras = sinal.fala_sintetica(30000, 2, palma_ms=D + 3700, silencio_ate_ms=D + 5000)
        with Pasta() as p:
            wav = sinal.escrever_wav(p.dir / "T09_giorgio_sala_t1.wav", amostras)
            _, dados, _, _ = _rodar(sinal.level_csv(amostras, D, 28000, ganho=0.25), wav=wav)
        self.assertEqual(4000, dados["palma_ate_ms"])
        self.assertTrue(3700 < dados["palma"]["app_ms"] <= 4000, dados["palma"])
        self.assertTrue(dados["palma"]["confere"])

    def test_D3_palma_confere_ate_200_ms_do_global(self):
        linhas = sincronizar.ler_level("t_ms,rms\n200,0.001\n400,0.5\n600,0.001\n800,0.002\n")
        wav = sincronizar.Wav([1] * 300)
        for d_palma, confere in ((1200, True), (800, True), (1210, False), (790, False)):
            with self.subTest(d_palma=d_palma), mock.patch.object(sincronizar, "buscar", return_value=(d_palma, 0.9)):
                res = sincronizar.palma(linhas, wav, 1000, 4000)
                self.assertEqual((confere, d_palma - 1000), (res["confere"], res["diferenca_ms"]))

    def test_limite_da_palma_inclui_a_linha_no_limite(self):
        level = "t_ms,rms\n200,0.001\n400,0.5\n600,0.001\n800,0.002\n"
        res = sincronizar.palma(sincronizar.ler_level(level), sincronizar.Wav([1] * 200), 0, 400)
        self.assertEqual(400, res["app_ms"])

    def test_motivos_nos_limites(self):
        def p(confere):
            return {"confere": confere, "motivo": None}

        def dv(ms, indeterminada=False):
            return {"deriva_ms": ms, "indeterminada": indeterminada}

        self.assertEqual([], sincronizar.motivos_de(p(True), dv(200)))
        self.assertEqual([], sincronizar.motivos_de(p(True), dv(-200)))
        self.assertEqual(["deriva acima de 200 ms"], sincronizar.motivos_de(p(True), dv(201)))
        self.assertEqual(["deriva acima de 200 ms"], sincronizar.motivos_de(p(True), dv(-201)))
        self.assertEqual(["palma não confere"], sincronizar.motivos_de(p(False), dv(0)))
        self.assertEqual(["deriva indeterminada"], sincronizar.motivos_de(p(True), dv(None, True)))


class BuscaTest(unittest.TestCase):
    class _Fixa:
        def __init__(self, valores):
            self.valores = valores

        def correlacao(self, _wav, d):
            return self.valores.get(d)

    def test_empate_global_fica_com_o_menor_e_o_da_palma_com_o_meio(self):
        serie = self._Fixa({10: 0.5, 20: 0.9, 30: 0.9 - 1e-12, 40: 0.9, 50: 0.9, 60: 0.2})
        self.assertEqual(20, sincronizar._melhor(serie, None, range(0, 70, 10))[0])
        self.assertEqual(30, sincronizar._melhor(serie, None, range(0, 70, 10), meio_do_empate=True)[0])
        serie = self._Fixa({10: 0.9, 20: 0.9, 30: 0.9})
        self.assertEqual(20, sincronizar._melhor(serie, None, [10, 20, 30], meio_do_empate=True)[0])
        self.assertEqual((None, None), sincronizar._melhor(self._Fixa({}), None, [10, 20]))

    def test_empate_tolera_so_o_arredondamento(self):
        serie = self._Fixa({10: 0.9, 20: 0.9 - 1e-6, 30: 0.9 - 1e-6})
        self.assertEqual(10, sincronizar._melhor(serie, None, [10, 20, 30], meio_do_empate=True)[0])

    def test_wav_em_silencio_sai_com_2(self):
        with Pasta() as p:
            wav = sinal.escrever_wav(p.dir / "s.wav", [0] * 16000 * 40)
            codigo, _, _, err = _rodar(sinal.level_csv(AMOSTRAS, D, 38000), wav=wav)
        self.assertEqual(2, codigo)
        self.assertIn("sem variância", err)


class DerivaTest(unittest.TestCase):
    def test_D3_deriva_de_1_por_cento_passa_de_200_e_sai_com_1(self):
        codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, deriva_ppm=10000))
        dv = dados["deriva"]
        self.assertEqual(1, codigo)
        self.assertIn("deriva acima de 200 ms", dados["motivos"])
        self.assertLess(dv["deriva_ms"], -300, "1% de 38 s é ~380 ms, com a extensão à gravação inteira")
        self.assertGreater(dv["deriva_ms"], -450)
        self.assertLess(abs(dv["deriva_ms"]), 2.2 * abs(dv["diferenca_metades_ms"]))

    def test_D3_deriva_pequena_passa(self):
        codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, deriva_ppm=2000))
        self.assertEqual(0, codigo)
        self.assertLessEqual(abs(dados["deriva"]["deriva_ms"]), 200)
        self.assertGreater(abs(dados["deriva"]["deriva_ms"]), 0)

    def test_D3_corte_na_gravacao_de_referencia_sai_sensivel(self):
        for salto in ((19000, 3000), (19000, -3000)):
            with self.subTest(salto=salto):
                codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, salto=salto))
                self.assertEqual(1, codigo)
                self.assertIn("deriva acima de 200 ms", dados["motivos"])

    def test_D3_metade_na_borda_da_busca_deriva_indeterminada(self):
        linhas = sincronizar.ler_level("t_ms,rms\n200,0.1\n400,0.2\n600,0.1\n800,0.3\n")
        for fim, indeterminada in ((D + 1000, True), (D - 1000, True), (None, True), (D + 990, False),
                                   (D - 990, False)):
            with self.subTest(fim=fim), mock.patch.object(sincronizar, "buscar",
                                                          side_effect=[(D, 0.9), (fim, 0.9 if fim else None)]):
                dv = sincronizar.deriva(linhas, None, D)
                self.assertEqual(indeterminada, dv["indeterminada"])
                self.assertEqual(None if indeterminada else round((D - fim) * 600 / 400), dv["deriva_ms"])

    def test_D3_metade_sem_variancia_deriva_indeterminada(self):
        codigo, dados, _, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000, constante_depois_de_ms=17000))
        self.assertEqual(1, codigo)
        self.assertTrue(dados["deriva"]["indeterminada"])
        self.assertIsNone(dados["deriva"]["deriva_ms"])
        self.assertIn("deriva indeterminada", dados["motivos"])


class EntradaTest(unittest.TestCase):
    def test_level_invalido_sai_com_2(self):
        casos = {
            "cabeçalho": "t,rms\n200,0.1\n400,0.1\n600,0.2\n800,0.1\n",
            "campos": "t_ms,rms\n200,0.1,9\n400,0.1\n600,0.2\n800,0.1\n",
            "inteiro": "t_ms,rms\n2x0,0.1\n400,0.1\n600,0.2\n800,0.1\n",
            "negativo": "t_ms,rms\n-200,0.1\n400,0.1\n600,0.2\n800,0.1\n",
            "rms acima de 1": "t_ms,rms\n200,1.5\n400,0.1\n600,0.2\n800,0.1\n",
            "rms negativo": "t_ms,rms\n200,-0.1\n400,0.1\n600,0.2\n800,0.1\n",
            "nan": "t_ms,rms\n200,nan\n400,0.1\n600,0.2\n800,0.1\n",
            "decrescente": "t_ms,rms\n200,0.1\n400,0.1\n300,0.2\n800,0.1\n",
            "poucas linhas": "t_ms,rms\n200,0.1\n400,0.1\n600,0.2\n",
            "inf": "t_ms,rms\n200,inf\n400,0.1\n600,0.2\n800,0.1\n",
        }
        for nome, level in casos.items():
            with self.subTest(caso=nome):
                codigo, dados, out, err = _rodar(level)
                self.assertEqual((2, None, ""), (codigo, dados, out))
                self.assertTrue(err.startswith("sincronizar.py: "))

    def test_tudo_no_mesmo_instante_sai_com_2_sem_variancia(self):
        codigo, _, _, err = _rodar("t_ms,rms\n5000,0.1\n5000,0.2\n5000,0.3\n5000,0.1\n")
        self.assertEqual(2, codigo)
        self.assertIn("sem variância", err)

    def test_level_nos_limites_aceito(self):
        linhas = sincronizar.ler_level("t_ms,rms\n0,0\n0,1\n400,0.5\n600,0.2\n")
        self.assertEqual([(0, 0.0), (0, 1.0), (400, 0.5), (600, 0.2)], linhas)

    def test_wav_fora_do_formato_sai_com_2(self):
        with Pasta() as p:
            estereo = sinal.escrever_wav(p.dir / "e.wav", AMOSTRAS, canais=2)
            taxa = sinal.escrever_wav(p.dir / "t.wav", AMOSTRAS, taxa=44100)
            corrompido = p.arquivo("c.wav", b"RIFF0000WAVEfmt ")
            for caminho in (estereo, taxa, corrompido, str(p.dir / "nao-existe.wav")):
                with self.subTest(wav=Path(caminho).name):
                    codigo, dados, out, _ = _rodar(sinal.level_csv(AMOSTRAS, D, 38000), wav=caminho)
                    self.assertEqual((2, None, ""), (codigo, dados, out))

    def test_sem_variancia_sai_com_2(self):
        level = "t_ms,rms\n" + "".join(f"{200 * k},0.1\n" for k in range(1, 50))
        codigo, _, _, err = _rodar(level)
        self.assertEqual(2, codigo)
        self.assertIn("sem variância", err)

    def test_energias_do_wav_por_fatia_de_10_ms(self):
        with Pasta() as p:
            caminho = sinal.escrever_wav(p.dir / "u.wav", [1] * 160 + [2] * 160 + [3] * 100)
            self.assertEqual([160, 640], sincronizar.energias_do_wav(caminho))

    def test_bloco_fora_do_wav_e_silencio(self):
        wav = sincronizar.Wav([5] * 30)
        self.assertEqual(100, wav.energia(20))
        self.assertEqual(50, wav.energia(10))
        self.assertEqual(50, wav.energia(40))
        self.assertEqual(0, wav.energia(60))
        self.assertTrue(wav.dentro(20) and wav.dentro(30))
        self.assertFalse(wav.dentro(19) or wav.dentro(31))


class SaidaTest(unittest.TestCase):
    def test_json_e_proveniencia(self):
        level = sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25)
        codigo, dados, out, _ = _rodar(level)
        self.assertEqual(0, codigo)
        for chave in ("deslocamento_ms", "correlacao", "faixa_ms", "linhas", "linhas_fora_do_wav", "duracao_wav_ms",
                      "palma", "deriva", "sensivel", "motivos", "codigo", "entradas", "palma_ate_ms"):
            self.assertIn(chave, dados)
        self.assertEqual(40000, dados["duracao_wav_ms"])
        self.assertEqual(["wav", "level"], [e["papel"] for e in dados["entradas"]])
        self.assertEqual("T01_giorgio_sala_t1.wav", dados["entradas"][0]["arquivo"])
        self.assertEqual(64, len(dados["entradas"][0]["sha256"]))
        self.assertIn("t_wav = t_app + deslocamento", out)
        self.assertIn("sha256=", out)

    def test_deterministico(self):
        level = sinal.level_csv(AMOSTRAS, D, 38000, ganho=0.25)
        self.assertEqual(_rodar(level)[1], _rodar(level)[1])

    def test_ajuda(self):
        codigo, out, _ = rodar(sincronizar.main, ["--help"])
        self.assertEqual(0, codigo)
        for termo in ("wav", "level", "--faixa-ms", "--palma-ate-ms", "--json"):
            self.assertIn(termo, out)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Passo 3: ver falhar**

Rodar: `python3 etapa0/ferramentas/testar.py test_sincronizar.py`
Esperado: `ERROR: test_sincronizar` com `ModuleNotFoundError: No module named 'sincronizar'`; última linha `testar.py: 1 testes, 0 falhas, 1 erros, 0 pulados`.

- [ ] **Passo 4: escrever a ferramenta**

`etapa0/ferramentas/sincronizar.py`:

```python
"""sincronizar.py <wav> <level.csv> [--faixa-ms MIN MAX] [--palma-ate-ms N] [--json SAIDA]: põe o relógio do app
no relógio do WAV de referência.

spec 0005, D3 e D4; impl_plan 0005, A3.

Convenção: t_wav = t_app + deslocamento. É o valor de `metricas.py --deslocamento-ms`.

Método:
- cada linha do level.csv (t_ms, rms) é o RMS do bloco de 200 ms que terminou em t_ms (0003: 3.200 amostras);
- o WAV vira energia por fatia de 10 ms; o bloco que termina em t_ms + deslocamento sai da soma de 20 fatias
  (fora do WAV conta como silêncio);
- os dois lados viram dB, com piso de um degrau do PCM 16 bits, e o deslocamento é o de maior correlação de
  Pearson: grade de 100 ms na faixa inteira e depois de 10 ms em volta do melhor (empate: o menor);
- faixa padrão: todo deslocamento em que o WAV cobre a gravação inteira do app (o roteiro manda ligar o
  QuickTime antes do Ouvir e parar depois do Parar); --faixa-ms troca a faixa (reprodução pela fonte WAV);
- palma: a linha mais alta do level.csv até --palma-ate-ms; as linhas a até 1 s dela são correlacionadas de
  novo a até 1 s do deslocamento (empate: o meio do patamar, porque um estalo cabe inteiro em vários blocos de
  200 ms); confere se esse deslocamento fica a até 200 ms do global. O pico do WAV fica registrado;
- deriva: cada metade das linhas (por contagem) é correlacionada de novo a até 1 s do deslocamento; a
  diferença (começo − fim) é estendida à duração inteira. Melhor na borda da busca = indeterminada.

Só lê o WAV; nada de áudio é escrito (CLAUDE.md regra 3).
Códigos de saída: 0 sincronizado; 1 calculado, mas sensível ao deslocamento (palma não confere, deriva acima
de 200 ms ou indeterminada); 2 entrada inválida.
"""

import argparse
import csv
import hashlib
import io
import json
import math
import sys
import wave
from array import array
from pathlib import Path

TAXA = 16000
BLOCO_MS = 200  # 0003: BLOCK_SAMPLES = 3.200 amostras a 16 kHz
SUB_MS = 10  # grade da busca
AMOSTRAS_SUB = TAXA * SUB_MS // 1000  # 160
SUBS_BLOCO = BLOCO_MS // SUB_MS  # 20
AMOSTRAS_BLOCO = TAXA * BLOCO_MS // 1000  # 3.200
PASSO_GROSSO_MS = 100
TOLERANCIA_MS = 200  # spec D3: resolução de um bloco
PALMA_ATE_MS = 4000  # roteiro do spec: palma ~2 s depois do Ouvir; leitura ~2 s depois da palma
JANELA_LOCAL_MS = 1000  # busca da palma no WAV e de cada metade
PISO = 1 / 32768  # um degrau do PCM 16 bits
CAB_LEVEL = ["t_ms", "rms"]
MIN_LINHAS = 4  # duas por metade
EMPATE = 1e-9  # correlações iguais a menos do arredondamento do float


class ErroEntrada(Exception):
    """Código de saída 2."""


# ------------------------------------------------------------------ leitura


def ler_level(conteudo):
    """level.csv -> lista de (t_ms, rms). t não decrescente, rms em [0, 1]. Linhas no mesmo instante caem todas
    no mesmo bloco do WAV e saem como "sem variância"."""
    linhas = list(csv.reader(io.StringIO(conteudo, newline="")))
    if not linhas or linhas[0] != CAB_LEVEL:
        raise ErroEntrada("level.csv: cabeçalho diferente de t_ms,rms")
    saida = []
    for n, campos in enumerate(linhas[1:], start=1):
        if len(campos) != 2:
            raise ErroEntrada(f"level.csv: linha {n} sem 2 campos")
        try:
            t, rms = int(campos[0]), float(campos[1])
        except ValueError:
            raise ErroEntrada(f"level.csv: linha {n} com número inválido") from None
        if t < 0 or not 0 <= rms <= 1:  # nan e inf também caem aqui
            raise ErroEntrada(f"level.csv: linha {n} fora da faixa (t >= 0, 0 <= rms <= 1)")
        if saida and t < saida[-1][0]:
            raise ErroEntrada(f"level.csv: linha {n} com t_ms decrescente")
        saida.append((t, rms))
    if len(saida) < MIN_LINHAS:
        raise ErroEntrada(f"level.csv: precisa de ao menos {MIN_LINHAS} linhas")
    return saida


def energias_do_wav(caminho):
    """Soma dos quadrados por fatia de 10 ms (inteiros exatos). A última fatia incompleta fica de fora."""
    try:
        with wave.open(str(caminho), "rb") as w:
            if w.getcomptype() != "NONE" or w.getnchannels() != 1 or w.getsampwidth() != 2 or w.getframerate() != TAXA:
                raise ErroEntrada("o WAV precisa ser 16 kHz, mono, PCM 16 bits")
            energias = []
            while True:
                dados = w.readframes(AMOSTRAS_SUB * 1000)
                if not dados:
                    break
                a = array("h")
                a.frombytes(dados[: len(dados) - len(dados) % 2])
                if sys.byteorder == "big":
                    a.byteswap()
                for k in range(0, len(a) - AMOSTRAS_SUB + 1, AMOSTRAS_SUB):
                    fatia = a[k:k + AMOSTRAS_SUB]
                    energias.append(math.sumprod(fatia, fatia))
            return energias
    except (wave.Error, EOFError) as erro:
        raise ErroEntrada(f"WAV ilegível: {erro}") from None


# ------------------------------------------------------------------ cálculo


def _db(rms):
    return 20 * math.log10(max(rms, PISO))


def _teto10(v):
    return -((-v) // SUB_MS) * SUB_MS


def _piso10(v):
    return (v // SUB_MS) * SUB_MS


def faixa_padrao(t_min, t_max, duracao_ms):
    """Deslocamentos em que o WAV cobre a gravação inteira: [200 − t_min, duração − t_max], na grade de 10 ms."""
    lo, hi = _teto10(BLOCO_MS - t_min), _piso10(duracao_ms - t_max)
    if lo > hi:
        raise ErroEntrada("o WAV não cobre a gravação do app (QuickTime ligado depois do Ouvir ou parado antes "
                          "do Parar); numa reprodução pela fonte WAV, use --faixa-ms")
    return lo, hi


class Wav:
    def __init__(self, energias):
        self.energias = energias
        self.n = len(energias)
        self.duracao_ms = self.n * SUB_MS
        self.prefixo = [0]
        for e in energias:
            self.prefixo.append(self.prefixo[-1] + e)

    def energia(self, fim):
        """Energia do bloco de 200 ms que termina na fatia `fim` (exclusiva); fora do WAV é silêncio."""
        a = min(max(fim - SUBS_BLOCO, 0), self.n)
        b = min(max(fim, 0), self.n)
        return self.prefixo[b] - self.prefixo[a]

    def db(self, fim):
        return _db(math.sqrt(self.energia(fim) / AMOSTRAS_BLOCO) / 32768)

    def dentro(self, fim):
        return SUBS_BLOCO <= fim <= self.n


class Serie:
    """Linhas do level.csv prontas para a correlação: dB centrado e fim de cada bloco em fatias de 10 ms."""

    def __init__(self, linhas):
        self.linhas = linhas
        self.bases = [t // SUB_MS for t, _ in linhas]  # fatia em que o bloco termina (erro < 10 ms)
        x = [_db(rms) for _, rms in linhas]
        media = math.fsum(x) / len(x)
        self.xc = [v - media for v in x]
        self.sxx = math.fsum(v * v for v in self.xc)
        self.centro = math.fsum(t for t, _ in linhas) / len(linhas)

    def correlacao(self, wav, deslocamento):
        if self.sxx <= 0:
            return None
        s = deslocamento // SUB_MS
        y = [wav.db(b + s) for b in self.bases]
        n = len(y)
        my = math.fsum(y) / n
        syy = math.fsum((v - my) * (v - my) for v in y)
        if syy <= 0:
            return None
        return math.sumprod(self.xc, y) / math.sqrt(self.sxx * syy)


def _melhor(serie, wav, candidatos, meio_do_empate=False):
    """Maior correlação. Empate: o menor deslocamento, ou o do meio do patamar com `meio_do_empate`."""
    pares = [(d, serie.correlacao(wav, d)) for d in candidatos]
    pares = [(d, r) for d, r in pares if r is not None]
    if not pares:
        return None, None
    r_max = max(r for _, r in pares)
    empatados = [d for d, r in pares if r >= r_max - EMPATE]
    d = empatados[(len(empatados) - 1) // 2] if meio_do_empate else empatados[0]
    return d, dict(pares)[d]


def buscar(serie, wav, lo, hi, meio_do_empate=False):
    """Grade de 100 ms a partir de lo e depois de 10 ms a até 100 ms do melhor, sem sair de [lo, hi]: o raio
    da grade fina cobre o passo da grossa até hi. (None, None) se sem variância."""
    d, _ = _melhor(serie, wav, range(lo, hi + 1, PASSO_GROSSO_MS))
    if d is None:
        return None, None
    finos = range(max(lo, d - PASSO_GROSSO_MS), min(hi, d + PASSO_GROSSO_MS) + 1, SUB_MS)
    return _melhor(serie, wav, finos, meio_do_empate)


def palma(linhas, wav, deslocamento, ate_ms):
    """Conferência independente da correlação global: só as linhas a até 1 s da palma, buscadas a até 1 s."""
    saida = {"app_ms": None, "wav_pico_ms": None, "deslocamento_ms": None, "diferenca_ms": None,
             "proeminencia_db": None, "confere": False, "motivo": None}
    candidatas = [(t, rms) for t, rms in linhas if t <= ate_ms]
    if not candidatas:
        saida["motivo"] = f"nenhuma linha do level.csv até {ate_ms} ms"
        return saida
    t_app, rms_app = max(candidatas, key=lambda linha: linha[1])  # max devolve o primeiro em empate
    dbs = sorted(_db(rms) for _, rms in candidatas)
    meio = len(dbs) // 2
    mediana = dbs[meio] if len(dbs) % 2 else (dbs[meio - 1] + dbs[meio]) / 2
    saida["app_ms"], saida["proeminencia_db"] = t_app, round(_db(rms_app) - mediana, 2)
    base = t_app // SUB_MS + deslocamento // SUB_MS
    raio = JANELA_LOCAL_MS // SUB_MS
    fatias = [k for k in range(base - raio - SUBS_BLOCO, base + raio) if 0 <= k < wav.n]
    if fatias:
        saida["wav_pico_ms"] = max(fatias, key=lambda k: wav.energias[k]) * SUB_MS
    locais = [linha for linha in linhas if abs(linha[0] - t_app) <= JANELA_LOCAL_MS]
    lo, hi = deslocamento - JANELA_LOCAL_MS, deslocamento + JANELA_LOCAL_MS
    d_palma, _ = buscar(Serie(locais), wav, lo, hi, meio_do_empate=True)
    if d_palma is None:
        saida["motivo"] = "sem variância em volta da palma"
        return saida
    saida["deslocamento_ms"], saida["diferenca_ms"] = d_palma, d_palma - deslocamento
    saida["confere"] = abs(d_palma - deslocamento) <= TOLERANCIA_MS  # a borda da busca (1 s) já fica fora
    return saida


def deriva(linhas, wav, deslocamento):
    metade = len(linhas) // 2
    partes = []
    for pedaco in (linhas[:metade], linhas[metade:]):
        serie = Serie(pedaco)
        lo, hi = deslocamento - JANELA_LOCAL_MS, deslocamento + JANELA_LOCAL_MS
        d, _ = buscar(serie, wav, lo, hi)
        partes.append((d, serie.centro, d is None or d in (lo, hi)))
    (d_ini, c_ini, borda_ini), (d_fim, c_fim, borda_fim) = partes
    indeterminada = borda_ini or borda_fim
    saida = {"inicio_ms": d_ini, "fim_ms": d_fim, "centro_inicio_ms": round(c_ini), "centro_fim_ms": round(c_fim),
             "diferenca_metades_ms": None, "deriva_ms": None, "indeterminada": indeterminada}
    if not indeterminada:
        duracao = linhas[-1][0] - linhas[0][0]
        saida["diferenca_metades_ms"] = d_ini - d_fim
        saida["deriva_ms"] = round((d_ini - d_fim) * duracao / (c_fim - c_ini))
    return saida


def motivos_de(resultado_palma, resultado_deriva):
    motivos = []
    if not resultado_palma["confere"]:
        motivos.append("palma não confere" + (f" ({resultado_palma['motivo']})" if resultado_palma["motivo"] else ""))
    if resultado_deriva["indeterminada"]:
        motivos.append("deriva indeterminada")
    elif abs(resultado_deriva["deriva_ms"]) > TOLERANCIA_MS:
        motivos.append(f"deriva acima de {TOLERANCIA_MS} ms")
    return motivos


def sincronizar(linhas, energias, faixa=None, palma_ate_ms=PALMA_ATE_MS):
    wav = Wav(energias)
    if wav.n < SUBS_BLOCO:
        raise ErroEntrada("WAV mais curto que um bloco de 200 ms")
    if faixa is None:
        lo, hi = faixa_padrao(linhas[0][0], linhas[-1][0], wav.duracao_ms)
    else:
        lo, hi = _teto10(faixa[0]), _piso10(faixa[1])
        if lo > hi:
            raise ErroEntrada("--faixa-ms: MIN maior que MAX")
    serie = Serie(linhas)
    d, r = buscar(serie, wav, lo, hi)
    if d is None:
        raise ErroEntrada("sem variância no level.csv ou no WAV: nada para correlacionar")
    resultado_palma = palma(linhas, wav, d, palma_ate_ms)
    resultado_deriva = deriva(linhas, wav, d)
    motivos = motivos_de(resultado_palma, resultado_deriva)
    s = d // SUB_MS
    return {
        "deslocamento_ms": d,
        "correlacao": round(r, 4),
        "faixa_ms": [lo, hi],
        "linhas": len(linhas),
        "linhas_fora_do_wav": sum(1 for b in serie.bases if not wav.dentro(b + s)),
        "duracao_wav_ms": wav.duracao_ms,
        "palma_ate_ms": palma_ate_ms,
        "palma": resultado_palma,
        "deriva": resultado_deriva,
        "sensivel": bool(motivos),
        "motivos": motivos,
        "codigo": 1 if motivos else 0,
    }


# ------------------------------------------------------------------ relatório e linha de comando


def relatorio(res, entradas):
    p, dv = res["palma"], res["deriva"]
    veredito = "SENSÍVEL AO DESLOCAMENTO" if res["sensivel"] else "SINCRONIZADO"
    linhas = [
        "# Sincronização", "", f"Veredito: **{veredito}**", "",
        f"- deslocamento: {res['deslocamento_ms']} ms (t_wav = t_app + deslocamento); correlação {res['correlacao']}; "
        f"faixa {res['faixa_ms'][0]} a {res['faixa_ms'][1]} ms; {res['linhas']} linhas do level.csv, "
        f"{res['linhas_fora_do_wav']} fora do WAV; WAV de {res['duracao_wav_ms']} ms",
    ]
    if p["deslocamento_ms"] is None:
        linhas.append(f"- palma: não conferida ({p['motivo']})")
    else:
        linhas.append(f"- palma: app {p['app_ms']} ms, pico no WAV {p['wav_pico_ms']} ms, deslocamento pela "
                      f"palma {p['deslocamento_ms']} ms, diferença {p['diferenca_ms']} ms (limite {TOLERANCIA_MS}), "
                      f"proeminência {p['proeminencia_db']} dB: {'confere' if p['confere'] else 'NÃO CONFERE'}")
    if dv["indeterminada"]:
        linhas.append(f"- deriva: INDETERMINADA (começo {dv['inicio_ms']} ms, fim {dv['fim_ms']} ms)")
    else:
        linhas.append(f"- deriva: começo {dv['inicio_ms']} ms, fim {dv['fim_ms']} ms, diferença entre as metades "
                      f"{dv['diferenca_metades_ms']} ms, estendida à gravação {dv['deriva_ms']} ms "
                      f"(limite {TOLERANCIA_MS})")
    for m in res["motivos"]:
        linhas.append(f"- motivo: {m}")
    linhas += ["", "## Proveniência", ""]
    for e in entradas:
        linhas.append(f"- {e['papel']}: {e['arquivo']} sha256={e['sha256']}")
    return "\n".join(linhas) + "\n"


def _sha256(caminho):
    h = hashlib.sha256()
    with open(caminho, "rb") as f:
        for bloco in iter(lambda: f.read(1 << 20), b""):
            h.update(bloco)
    return h.hexdigest()


def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="sincronizar.py",
        description="Deslocamento entre o relógio do app (level.csv) e o do WAV de referência (t_wav = t_app + d).",
    )
    parser.add_argument("wav", help="WAV de referência, 16 kHz, mono, PCM 16 bits")
    parser.add_argument("level", help="<nome>.level.csv exportado pelo app (t_ms,rms)")
    parser.add_argument("--faixa-ms", nargs=2, type=int, metavar=("MIN", "MAX"),
                        help="faixa de busca do deslocamento (padrão: o WAV cobre a gravação inteira)")
    parser.add_argument("--palma-ate-ms", type=int, default=PALMA_ATE_MS,
                        help=f"procura a palma no level.csv até este t_ms (padrão {PALMA_ATE_MS})")
    parser.add_argument("--json", help="grava o resultado em JSON neste arquivo")
    args = parser.parse_args(argv)
    try:
        with open(args.level, encoding="utf-8", newline="") as f:
            linhas = ler_level(f.read())
        energias = energias_do_wav(args.wav)
        faixa = tuple(args.faixa_ms) if args.faixa_ms else None
        res = sincronizar(linhas, energias, faixa, args.palma_ate_ms)
        entradas = [{"papel": "wav", "arquivo": Path(args.wav).name, "sha256": _sha256(args.wav)},
                    {"papel": "level", "arquivo": Path(args.level).name, "sha256": _sha256(args.level)}]
    except (OSError, UnicodeDecodeError, ErroEntrada) as erro:
        print(f"sincronizar.py: {erro}", file=sys.stderr)
        return 2
    res["entradas"] = entradas
    sys.stdout.write(relatorio(res, entradas))
    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(res, f, ensure_ascii=False, indent=2, sort_keys=True)
            f.write("\n")
    return res["codigo"]


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Passo 5: ver passar**

Rodar: `python3 etapa0/ferramentas/testar.py test_sincronizar.py`
Esperado: `testar.py: 32 testes, 0 falhas, 0 erros, 0 pulados`.

- [ ] **Passo 6: commit**

```bash
git add etapa0/ferramentas/sincronizar.py etapa0/ferramentas/testes/sinal.py etapa0/ferramentas/testes/test_sincronizar.py
git commit -m "feat(etapa0): sincronizar.py, relógio do app no relógio do WAV (spec 0005 D3, D4)"
```

### Tarefa 2 (motor-voz): `folha.py` (spec D5, D6, D7; A4)

**Arquivos:**
- Criar: `etapa0/ferramentas/testes/test_folha.py`
- Criar: `etapa0/ferramentas/folha.py`

- [ ] **Passo 1: escrever o teste**

`etapa0/ferramentas/testes/test_folha.py`:

```python
"""folha.py com esperados e transcrições sintéticos (spec 0005, D5, D6, D7; impl_plan 0005, A4).

Relógio do WAV: token i em [300i, 300i + 250], mais GAP ms para os tokens depois da fronteira. A folha sai no
relógio do app com deslocamento D (t_app = t_wav − D).
"""

import csv
import io
import unittest

import alinhar
import folha
import metricas
import roteiro
from testes import sintetico as s
from testes.apoio import Pasta, rodar

D = 1370
GAP = 60000


def _tempos(n, depois_de, sem_tempo=(), gap=GAP):
    saida = []
    for i in range(n):
        if i in sem_tempo:
            saida.append(None)
            continue
        extra = gap if i > depois_de else 0
        saida.append((300 * i + extra, 300 * i + 250 + extra))
    return saida


def _palavras(texto_id, tempos, extras=()):
    """Uma palavra (1ª da 1ª forma) por token com tempo, mais `extras` [(palavra, ini, fim)], em ordem."""
    tokens = s.tokens(texto_id)
    lista = [(t.formas[0][0], *tempos[t.indice]) for t in tokens if tempos[t.indice] is not None]
    lista += list(extras)
    lista.sort(key=lambda p: (p[1], p[2]))
    saida = io.StringIO()
    w = csv.writer(saida, lineterminator="\n")
    w.writerow(alinhar.CABECALHO_TRANSCRICAO)
    for palavra, ini, fim in lista:
        w.writerow([palavra, ini, fim, "1.0"])
    return saida.getvalue()


def _rodar(texto_id, tempos, extras=(), deslocamento=D, texto=None, esperado_de=None):
    with Pasta() as p:
        conteudo = texto if texto is not None else (roteiro.TEXTOS / f"{texto_id}.txt").read_bytes()
        argv = [p.arquivo("texto.txt", conteudo),
                p.arquivo("x.expected.csv", s.esperado_csv(esperado_de or texto_id, tempos)),
                p.arquivo("x.words.csv", _palavras(esperado_de or texto_id, tempos, extras)),
                "--deslocamento-ms", str(deslocamento)]
        codigo, out, err = rodar(folha.main, argv)
    return codigo, [tuple(linha) for linha in csv.reader(io.StringIO(out))], err


def _improviso(ini, fim, n=10):
    passo = (fim - ini) // n
    return [("palavra", ini + k * passo, ini + k * passo + passo // 2) for k in range(n - 1)] + [("fim", fim - 100, fim)]


N = len(s.tokens("T01"))
NL = len(s.tokens("L30"))


class ImprovisoTest(unittest.TestCase):
    def test_D6_t04_improviso_da_primeira_a_ultima_palavra_inserida(self):
        tempos = _tempos(N, 46)
        fim46, ini47 = tempos[46][1], tempos[47][0]
        extras = _improviso(fim46 + 2000, ini47 - 1500)
        codigo, linhas, err = _rodar("T04", tempos, extras)
        self.assertEqual(0, codigo, err)
        self.assertEqual([("t_ms", "evento", "detalhe"),
                          (str(fim46 + 2000 - D), "improviso_inicio", ""),
                          (str(ini47 - 1500 - D), "improviso_fim", "")], linhas)
        self.assertEqual(2, err.count("revisar: "))
        self.assertIn(f"revisar: improviso_inicio em {fim46 + 2000} ms do WAV ({fim46 + 2000 - D} ms do app)", err)
        self.assertIn("'conversa' (token 46)", err)

    def test_D6_palavras_encostadas_nas_fronteiras_entram(self):
        tempos = _tempos(N, 46)
        fim46, ini47 = tempos[46][1], tempos[47][0]
        extras = [("primeira", fim46, fim46 + 300), ("meio", fim46 + 5000, fim46 + 5300), ("ultima", ini47 - 300, ini47)]
        codigo, linhas, _ = _rodar("T04", tempos, extras)
        self.assertEqual(0, codigo)
        self.assertEqual((str(fim46 - D), "improviso_inicio", ""), linhas[1])
        self.assertEqual((str(ini47 - D), "improviso_fim", ""), linhas[2])

    def test_D6_palavra_que_atravessa_a_fronteira_fica_fora(self):
        tempos = _tempos(N, 46)
        fim46, ini47 = tempos[46][1], tempos[47][0]
        extras = [("cedo", fim46 - 1, fim46 + 300), ("meio", fim46 + 5000, fim46 + 5300), ("tarde", ini47 - 300, ini47 + 1)]
        codigo, linhas, _ = _rodar("T04", tempos, extras)
        self.assertEqual(0, codigo)
        self.assertEqual([(str(fim46 + 5000 - D), "improviso_inicio", ""), (str(fim46 + 5300 - D), "improviso_fim", "")],
                         linhas[1:])

    def test_D6_sem_palavra_no_improviso_sai_com_2(self):
        codigo, linhas, err = _rodar("T04", _tempos(N, 46))
        self.assertEqual((2, []), (codigo, linhas))
        self.assertIn("improviso: nenhuma palavra", err)

    def test_D7_fronteira_em_token_sem_tempo_avisa_e_sai_com_1(self):
        tempos = _tempos(N, 46, sem_tempo=(46,))
        fim45, ini47 = tempos[45][1], tempos[47][0]
        extras = _improviso(fim45 + 2000, ini47 - 1500)
        codigo, linhas, err = _rodar("T04", tempos, extras)
        self.assertEqual(1, codigo)
        self.assertEqual(str(fim45 + 2000 - D), linhas[1][0])
        self.assertIn("aviso: improviso: o token 46 ('conversa') não tem tempo; a fronteira usa o token 45 ('na')", err)


class PuloTest(unittest.TestCase):
    def _tempos_t05(self, sem_tempo=()):
        pulados = set(range(47, 91))
        return _tempos(N, 46, sem_tempo=pulados | set(sem_tempo), gap=-300 * 44 + 400)

    def test_D6_t05_pulo_com_indice_do_primeiro_pulado(self):
        tempos = self._tempos_t05()
        codigo, linhas, err = _rodar("T05", tempos)
        self.assertEqual(0, codigo, err)
        self.assertEqual([("t_ms", "evento", "detalhe"),
                          (str(tempos[46][1] - D), "pulo_inicio", "47"),
                          (str(tempos[91][0] - D), "pulo_fim", "")], linhas)
        self.assertIn("primeiro pulado: token 47", err)

    def test_D7_primeiro_token_depois_do_pulo_sem_tempo_avisa(self):
        tempos = self._tempos_t05(sem_tempo=(91,))
        codigo, linhas, err = _rodar("T05", tempos)
        self.assertEqual(1, codigo)
        self.assertEqual((str(tempos[92][0] - D), "pulo_fim", ""), linhas[2])
        self.assertIn("o token 91 ('Ouvir') não tem tempo; a fronteira usa o token 92", err)

    def test_D7_token_do_paragrafo_pulado_com_tempo_avisa(self):
        tempos = self._tempos_t05()
        tempos[47] = (tempos[46][1] + 10, tempos[46][1] + 20)
        codigo, _, err = _rodar("T05", tempos)
        self.assertEqual(1, codigo)
        self.assertIn("aviso: pulo: 1 token(s) do parágrafo pulado com tempo (primeiro: token 47)", err)

    def test_D5_metricas_aceita_a_folha_do_pulo(self):
        tempos = self._tempos_t05()
        _codigo, linhas, _ = _rodar("T05", tempos)
        folha_csv = "\n".join(",".join(linha) for linha in linhas) + "\n"
        trajetoria = s.trajetoria_csv([(0, 0, "Reposition")])
        codigo, dados, _, err = s.rodar_metricas(metricas.main, trajetoria, s.esperado_csv("T05", tempos),
                                                 [s.eventos_csv([]), folha_csv], deslocamento=D)
        self.assertNotEqual(2, codigo, err)
        self.assertEqual(47, dados["criterios"]["recuperacao"]["pulos"][0]["primeiro_pulado"])
        self.assertEqual(tempos[91][0], dados["criterios"]["recuperacao"]["pulos"][0]["pulo_fim_ms"])


class SilencioTest(unittest.TestCase):
    def test_D6_t06_silencio_entre_os_tokens(self):
        tempos = _tempos(N, 134, gap=30000)
        codigo, linhas, err = _rodar("T06", tempos)
        self.assertEqual(0, codigo, err)
        self.assertEqual([("t_ms", "evento", "detalhe"),
                          (str(tempos[134][1] - D), "silencio_inicio", ""),
                          (str(tempos[135][0] - D), "silencio_fim", "")], linhas)
        self.assertIn("'rodeios' (token 134)", err)

    def test_D7_palavra_dentro_do_silencio_avisa(self):
        tempos = _tempos(N, 134, gap=30000)
        extras = [("tosse", tempos[134][1] + 1, tempos[134][1] + 400)]
        codigo, _, err = _rodar("T06", tempos, extras)
        self.assertEqual(1, codigo)
        self.assertIn("aviso: silencio: 1 palavra(s) da transcrição dentro do silêncio", err)

    def test_tokens_da_fronteira_sobrepostos_saem_com_2(self):
        tempos = _tempos(N, 134, gap=30000)
        tempos[135] = (tempos[134][1] - 1, tempos[134][1] + 100)
        codigo, _, err = _rodar("T06", tempos)
        self.assertEqual(2, codigo)
        self.assertIn("começa antes do fim do token 134", err)

    def test_D7_palavra_encostada_no_silencio_nao_avisa(self):
        tempos = _tempos(N, 134, gap=30000)
        extras = [("antes", tempos[134][1], tempos[134][1] + 400), ("depois", tempos[135][0] - 400, tempos[135][0])]
        codigo, _, err = _rodar("T06", tempos, extras)
        self.assertEqual(0, codigo, err)


class L30Test(unittest.TestCase):
    def test_D5_l30_tres_eventos_em_ordem_e_metricas_aceita(self):
        tempos = [(300 * i, 300 * i + 250) for i in range(NL)]
        for i in range(1121, NL):
            tempos[i] = (tempos[i][0] + 60000, tempos[i][1] + 60000)
        for i in range(1804, 1899):
            tempos[i] = None
        for i in range(2428, NL):
            tempos[i] = (tempos[i][0] + 30000, tempos[i][1] + 30000)
        extras = _improviso(tempos[1120][1] + 1000, tempos[1121][0] - 1000)
        codigo, linhas, err = _rodar("L30", tempos, extras)
        self.assertEqual(0, codigo, err)
        self.assertEqual(["improviso_inicio", "improviso_fim", "pulo_inicio", "pulo_fim", "silencio_inicio",
                          "silencio_fim"], [linha[1] for linha in linhas[1:]])
        self.assertEqual([tempos[1120][1] + 1000 - D, tempos[1121][0] - 1000 - D, tempos[1803][1] - D,
                          tempos[1899][0] - D, tempos[2427][1] - D, tempos[2428][0] - D],
                         [int(linha[0]) for linha in linhas[1:]])
        self.assertEqual("1804", linhas[3][2])
        self.assertEqual(6, err.count("revisar: "))
        app = s.eventos_csv([(tempos[1500][1] - D, "pausa_toque", ""), (tempos[1501][0] - D, "retoma_toque", "")])
        folha_csv = "\n".join(",".join(linha) for linha in linhas) + "\n"
        codigo_m, dados, _, err_m = s.rodar_metricas(metricas.main, s.trajetoria_csv([(0, 0, "Reposition")]),
                                                     s.esperado_csv("L30", tempos), [app, folha_csv], deslocamento=D)
        self.assertNotEqual(2, codigo_m, err_m)
        self.assertEqual(1, len(dados["criterios"]["recuperacao"]["pulos"]))


class EntradaTest(unittest.TestCase):
    def test_texto_sem_evento_pela_voz_sai_com_2(self):
        codigo, _, err = _rodar("T01", _tempos(N, 46))
        self.assertEqual(2, codigo)
        self.assertIn("T01 não tem evento pela voz", err)

    def test_texto_que_nao_e_de_teste_sai_com_2(self):
        quase = (roteiro.TEXTOS / "T04.txt").read_bytes() + b" "
        codigo, _, err = _rodar("T04", _tempos(N, 46), texto=quase)
        self.assertEqual(2, codigo)
        self.assertIn("não é idêntico", err)

    def test_esperado_de_outro_texto_sai_com_2(self):
        n9 = len(s.tokens("T09"))
        codigo, _, err = _rodar("T04", [(300 * i, 300 * i + 250) for i in range(n9)], esperado_de="T09")
        self.assertEqual(2, codigo)
        self.assertIn("não é do texto T04", err)

    def test_deslocamento_grande_demais_sai_com_2(self):
        tempos = _tempos(N, 134, gap=30000)
        codigo, _, err = _rodar("T06", tempos, deslocamento=tempos[134][1] + 1)
        self.assertEqual(2, codigo)
        self.assertIn("antes do início da gravação do app", err)

    def test_deslocamento_no_limite_aceito(self):
        tempos = _tempos(N, 134, gap=30000)
        codigo, linhas, _ = _rodar("T06", tempos, deslocamento=tempos[134][1])
        self.assertEqual(0, codigo)
        self.assertEqual(("0", "silencio_inicio", ""), linhas[1])

    def test_entradas_invalidas_saem_com_2(self):
        with Pasta() as p:
            texto = str(roteiro.TEXTOS / "T06.txt")
            bom_esp = p.arquivo("e.csv", s.esperado_csv("T06", _tempos(N, 134, gap=30000)))
            bom_pal = p.arquivo("w.csv", _palavras("T06", _tempos(N, 134, gap=30000)))
            casos = {
                "sem deslocamento": [texto, bom_esp, bom_pal],
                "esperado ruim": [texto, p.arquivo("e2.csv", "x\n"), bom_pal, "--deslocamento-ms", "0"],
                "transcrição ruim": [texto, bom_esp, p.arquivo("w2.csv", "x\n"), "--deslocamento-ms", "0"],
                "arquivo ausente": [texto, str(p.dir / "nada.csv"), bom_pal, "--deslocamento-ms", "0"],
            }
            for nome, argv in casos.items():
                with self.subTest(caso=nome):
                    codigo, out, _ = rodar(folha.main, argv)
                    self.assertEqual((2, ""), (codigo, out))

    def test_nota_repetida_sai_com_erro(self):
        texto = roteiro.ler_texto("T04") + "\n// IMPROVISAR de novo\n\nOutra frase.\n"
        with self.assertRaises(folha.ErroEntrada):
            folha.fronteiras("T04", texto, roteiro.tokenizar(texto))

    def test_saida_em_arquivo(self):
        tempos = _tempos(N, 134, gap=30000)
        with Pasta() as p:
            saida = p.dir / "T06_giorgio_sala_t1.folha.events.csv"
            argv = [str(roteiro.TEXTOS / "T06.txt"), p.arquivo("e.csv", s.esperado_csv("T06", tempos)),
                    p.arquivo("w.csv", _palavras("T06", tempos)), "--deslocamento-ms", str(D), "-o", str(saida)]
            codigo, out, _ = rodar(folha.main, argv)
            self.assertEqual((0, ""), (codigo, out))
            self.assertTrue(saida.read_text(encoding="utf-8").startswith("t_ms,evento,detalhe\n"))

    def test_blocos_e_notas(self):
        b = folha.blocos("a b\nc\n\n// nota\n\n\nfim")
        self.assertEqual([(0, 5, False), (7, 14, True), (17, 20, False)], [(x.ini, x.fim, x.nota) for x in b])

    def test_ajuda(self):
        codigo, out, _ = rodar(folha.main, ["--help"])
        self.assertEqual(0, codigo)
        for termo in ("texto", "esperado", "transcricao", "--deslocamento-ms", "-o"):
            self.assertIn(termo, out)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Passo 2: ver falhar**

Rodar: `python3 etapa0/ferramentas/testar.py test_folha.py`
Esperado: `ModuleNotFoundError: No module named 'folha'`; `testar.py: 1 testes, 0 falhas, 1 erros, 0 pulados`.

- [ ] **Passo 3: escrever a ferramenta**

`etapa0/ferramentas/folha.py`:

```python
"""folha.py <texto> <expected.csv> <words.csv> --deslocamento-ms D [-o SAIDA]: folha de eventos derivada dos dados.

spec 0005, D6, D7 e critério D5; impl_plan 0005, A4.

Para os textos com evento pela voz (T04, T05, T06 e L30), acha no expected.csv e na transcrição (words.csv,
relógio do WAV) as fronteiras do improviso, do silêncio e do pulo marcados pelas notas // do texto, e escreve
<nome>.folha.events.csv (t_ms,evento,detalhe) no relógio do app: t_app = t_wav − deslocamento. Pausa e toque em
palavra vêm do events.csv do app e não entram aqui.

Regras (impl_plan A4):
- improviso: da primeira à última palavra da transcrição entre o fim do último token com tempo antes da
  fronteira e o início do primeiro token com tempo depois dela;
- silêncio: do fim do último token com tempo antes da fronteira ao início do primeiro com tempo depois;
- pulo: pulo_inicio no fim do último token com tempo antes do parágrafo pulado, com o índice do primeiro token
  pulado; pulo_fim no início do primeiro token com tempo depois dele.
No stderr: um ponto de escuta por fronteira (D7) e os avisos.
Códigos de saída: 0 folha escrita; 1 folha escrita com aviso (fronteira em token sem tempo, palavra dentro do
silêncio, token do parágrafo pulado com tempo): os avisos vão para a revisão; 2 entrada inválida.
"""

import argparse
import csv
import io
import sys
from dataclasses import dataclass

import alinhar
import metricas
import roteiro

CABECALHO = ["t_ms", "evento", "detalhe"]

# (evento, começo da linha da nota //, onde fica a fronteira)
REGRAS = {
    "T04": (("improviso", "// IMPROVISAR", "na_nota"),),
    "T05": (("pulo", "// PULAR", "paragrafo_seguinte"),),
    "T06": (("silencio", "// SILÊNCIO", "na_nota"),),
    "L30": (
        ("improviso", "// L30 — Minuto 8 (", "fim_do_paragrafo_seguinte"),
        ("pulo", "// L30 — Minuto 15 (", "paragrafo_seguinte"),
        ("silencio", "// L30 — Minuto 18 (", "fim_do_paragrafo_seguinte"),
    ),
}


class ErroEntrada(Exception):
    """Código de saída 2."""


@dataclass(frozen=True)
class Bloco:
    ini: int  # índice do primeiro caractere
    fim: int  # índice depois do último caractere
    nota: bool
    texto: str


def blocos(texto):
    """Linhas não vazias seguidas formam um bloco; bloco que começa com // é nota."""
    saida, atual, pos = [], None, 0
    for linha in texto.split("\n"):
        if linha.strip():
            if atual is None:
                atual = [pos, pos + len(linha), linha.strip()]
            else:
                atual[1] = pos + len(linha)
                atual[2] += "\n" + linha.strip()
        elif atual is not None:
            saida.append(Bloco(atual[0], atual[1], atual[2].startswith("//"), atual[2]))
            atual = None
        pos += len(linha) + 1
    if atual is not None:
        saida.append(Bloco(atual[0], atual[1], atual[2].startswith("//"), atual[2]))
    return saida


def fronteiras(texto_id, texto, tokens):
    """Lista de (evento, último token antes, primeiro token depois, primeiro token pulado ou None)."""
    lista = blocos(texto)

    def antes(pos):
        anteriores = [t.indice for t in tokens if t.inicio < pos]
        return anteriores[-1] if anteriores else None

    def depois(pos):
        seguintes = [t.indice for t in tokens if t.inicio >= pos]
        return seguintes[0] if seguintes else None

    saida = []
    for evento, prefixo, onde in REGRAS[texto_id]:
        notas = [k for k, b in enumerate(lista) if b.nota and b.texto.startswith(prefixo)]
        if len(notas) != 1:
            raise ErroEntrada(f"{texto_id}: a nota '{prefixo}' aparece {len(notas)} vez(es); esperado 1")
        k = notas[0]
        seguinte = next((b for b in lista[k + 1:] if not b.nota), None)
        pulado = None
        if onde == "na_nota":
            a, b = antes(lista[k].ini), depois(lista[k].fim)
        elif seguinte is None:
            raise ErroEntrada(f"{texto_id}: sem parágrafo depois da nota '{prefixo}'")
        elif onde == "fim_do_paragrafo_seguinte":
            a, b = antes(seguinte.fim), depois(seguinte.fim)
        else:  # paragrafo_seguinte: o parágrafo inteiro é pulado
            a, b, pulado = antes(seguinte.ini), depois(seguinte.fim), depois(seguinte.ini)
        if a is None or b is None:
            raise ErroEntrada(f"{texto_id}: a nota '{prefixo}' não fica entre dois tokens")
        saida.append((evento, a, b, pulado))
    return saida


def _com_tempo_antes(esperado, i):
    for k in range(i, -1, -1):
        if esperado[k].fim is not None:
            return k
    return None


def _com_tempo_depois(esperado, i):
    for k in range(i, len(esperado)):
        if esperado[k].fim is not None:
            return k
    return None


def derivar(texto_id, texto, tokens, esperado, transcricao, deslocamento):
    """Devolve (eventos [(t_app, nome, detalhe)], pontos de escuta, avisos). Lança ErroEntrada."""
    eventos, pontos, avisos = [], [], []

    def exibido(i):
        t = tokens[i]
        return texto[t.inicio:t.fim + 1]

    def marcar(t_wav, nome, detalhe, contexto):
        t_app = t_wav - deslocamento
        if t_app < 0:
            raise ErroEntrada(f"{nome} cairia antes do início da gravação do app ({t_app} ms): deslocamento errado?")
        eventos.append((t_app, nome, detalhe))
        pontos.append(f"revisar: {nome} em {t_wav} ms do WAV ({t_app} ms do app), {contexto}")

    for evento, a, b, pulado in fronteiras(texto_id, texto, tokens):
        ka, kb = _com_tempo_antes(esperado, a), _com_tempo_depois(esperado, b)
        if ka is None or kb is None:
            raise ErroEntrada(f"{evento}: nenhum token com tempo antes do token {a} ou depois do token {b}")
        for pedido, achado in ((a, ka), (b, kb)):
            if pedido != achado:
                avisos.append(f"aviso: {evento}: o token {pedido} ('{exibido(pedido)}') não tem tempo; "
                              f"a fronteira usa o token {achado} ('{exibido(achado)}')")
        fim_antes, inicio_depois = esperado[ka].fim, esperado[kb].inicio
        if inicio_depois < fim_antes:
            raise ErroEntrada(f"{evento}: o token {kb} começa antes do fim do token {ka}")
        perto = f"entre '{exibido(ka)}' (token {ka}) e '{exibido(kb)}' (token {kb})"
        if evento == "improviso":
            dentro = [p for p in transcricao if p.inicio >= fim_antes and p.fim <= inicio_depois]
            if not dentro:
                raise ErroEntrada(f"improviso: nenhuma palavra da transcrição {perto}")
            marcar(dentro[0].inicio, "improviso_inicio", "", f"primeira palavra do improviso, {perto}")
            marcar(dentro[-1].fim, "improviso_fim", "", f"última palavra do improviso, {perto}")
        elif evento == "silencio":
            dentro = [p for p in transcricao if p.inicio > fim_antes and p.fim < inicio_depois]
            if dentro:
                avisos.append(f"aviso: silencio: {len(dentro)} palavra(s) da transcrição dentro do silêncio, {perto}")
            marcar(fim_antes, "silencio_inicio", "", f"fim de '{exibido(ka)}' (token {ka})")
            marcar(inicio_depois, "silencio_fim", "", f"início de '{exibido(kb)}' (token {kb})")
        else:
            falados = [i for i in range(pulado, b) if esperado[i].fim is not None]
            if falados:
                avisos.append(f"aviso: pulo: {len(falados)} token(s) do parágrafo pulado com tempo "
                              f"(primeiro: token {falados[0]})")
            marcar(fim_antes, "pulo_inicio", str(pulado), f"fim de '{exibido(ka)}' (token {ka}); "
                                                         f"primeiro pulado: token {pulado}")
            marcar(inicio_depois, "pulo_fim", "", f"início de '{exibido(kb)}' (token {kb})")
    return eventos, pontos, avisos  # em ordem: as regras de cada texto seguem a ordem do texto


def folha_csv(eventos):
    saida = io.StringIO()
    w = csv.writer(saida, lineterminator="\n")
    w.writerow(CABECALHO)
    for t, nome, detalhe in eventos:
        w.writerow([t, nome, detalhe])
    return saida.getvalue()


def executar(conteudo_texto, conteudo_esperado, conteudo_transcricao, deslocamento):
    texto_id = roteiro.identificar_texto(conteudo_texto)
    if texto_id is None:
        raise ErroEntrada("o texto não é idêntico a nenhum de etapa0/textos/")
    if texto_id not in REGRAS:
        raise ErroEntrada(f"{texto_id} não tem evento pela voz; folha só para {', '.join(REGRAS)}")
    texto = conteudo_texto.decode("utf-8")
    tokens = roteiro.tokenizar(texto)
    try:
        esperado, palavras = metricas.ler_esperado(conteudo_esperado)
        transcricao = alinhar.ler_transcricao(conteudo_transcricao)
    except (metricas.ErroEntrada, alinhar.ErroEntrada) as erro:
        raise ErroEntrada(str(erro)) from None
    if palavras != [roteiro.normalizar(texto[t.inicio:t.fim + 1]) for t in tokens]:
        raise ErroEntrada(f"o expected.csv não é do texto {texto_id}")
    eventos, pontos, avisos = derivar(texto_id, texto, tokens, esperado, transcricao, deslocamento)
    return folha_csv(eventos), pontos, avisos


def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="folha.py",
        description="Deriva a folha de eventos (improviso, silêncio, pulo) do expected.csv e da transcrição.",
    )
    parser.add_argument("texto", help="texto de teste com eventos pela voz (T04, T05, T06 ou L30), idêntico ao de etapa0/textos/")
    parser.add_argument("esperado", help="<nome>.expected.csv (relógio do WAV)")
    parser.add_argument("transcricao", help="<nome>.words.csv, saída do timestamps.py (relógio do WAV)")
    parser.add_argument("--deslocamento-ms", type=int, required=True,
                        help="o do sincronizar.py (t_wav = t_app + d); a folha sai no relógio do app")
    parser.add_argument("-o", "--saida", help="arquivo da folha (padrão: stdout)")
    args = parser.parse_args(argv)
    try:
        with open(args.texto, "rb") as f:
            conteudo_texto = f.read()
        with open(args.esperado, encoding="utf-8", newline="") as f:
            conteudo_esperado = f.read()
        with open(args.transcricao, encoding="utf-8", newline="") as f:
            conteudo_transcricao = f.read()
        texto_folha, pontos, avisos = executar(conteudo_texto, conteudo_esperado, conteudo_transcricao,
                                               args.deslocamento_ms)
    except (OSError, UnicodeDecodeError, ErroEntrada) as erro:
        print(f"folha.py: {erro}", file=sys.stderr)
        return 2
    sys.stderr.write("".join(f"{linha}\n" for linha in pontos + avisos))
    if args.saida:
        with open(args.saida, "w", encoding="utf-8", newline="") as f:
            f.write(texto_folha)
    else:
        sys.stdout.write(texto_folha)
    return 1 if avisos else 0


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Passo 4: ver passar**

Rodar: `python3 etapa0/ferramentas/testar.py test_folha.py`
Esperado: `testar.py: 24 testes, 0 falhas, 0 erros, 0 pulados`.

- [ ] **Passo 5: commit**

```bash
git add etapa0/ferramentas/folha.py etapa0/ferramentas/testes/test_folha.py
git commit -m "feat(etapa0): folha.py, folha de eventos derivada dos dados (spec 0005 D6, D7)"
```

### Tarefa 3 (motor-voz): `veredito.py` (spec C1, C2, D9, E2, E3, F1, F2, H1; A5)

**Arquivos:**
- Criar: `etapa0/ferramentas/testes/test_veredito.py`
- Criar: `etapa0/ferramentas/veredito.py`

- [ ] **Passo 1: escrever o teste**

`etapa0/ferramentas/testes/test_veredito.py`:

```python
"""veredito.py com rodadas sintéticas em pasta temporária (spec 0005, D3, D7, D8, D9, D10; impl_plan 0005, A5).

O `Rodada` escreve, para cada gravação, arquivos de entrada com conteúdo único, os três JSON do metricas.py
(d, d − 200, d + 200) com a proveniência certa, o JSON do sincronizar.py, o meta.txt e a revisão. Um teste roda
o metricas.py de verdade para travar os campos que o veredito lê (Q4).
"""

import copy
import hashlib
import json
import unittest
from pathlib import Path

import metricas
import veredito
from testes import sintetico as s
from testes.apoio import Pasta, rodar

D = 1370
META_OFICIAL = dict(veredito.CONFIG_OFICIAL)


def _sha(caminho):
    return hashlib.sha256(Path(caminho).read_bytes()).hexdigest()


def metricas_json(atraso="PASSA", amostras=600, salto="PASSA", movimentos=(), recuo="PASSA",
                  recuperacao="NAO_SE_APLICA", cursor=150, leitura=600):
    """Recorte do JSON do metricas.py com os campos que o veredito lê."""
    return {
        "criterios": {
            "atraso": {"veredito": atraso, "amostras": amostras, "ate_3": amostras, "fracao": 1.0 if amostras else None,
                       "maior_atraso": 2},
            "salto_errado": {"veredito": salto, "total": len(movimentos), "por_posicao": [],
                             "por_movimento": [{"t_ms": 1, "cursor": 1, "move": "Short", "intervalo": k,
                                                "esperado_no_inicio": 0, "classe": classe}
                                               for k, classe in movimentos]},
            "recuo": {"veredito": recuo, "violacoes": []},
            "recuperacao": {"veredito": recuperacao,
                            "pulos": [] if recuperacao == "NAO_SE_APLICA" else
                            [{"pulo_inicio_ms": 1, "primeiro_pulado": 47, "pulo_fim_ms": 2, "alvo": 91,
                              "recuperacao_ms": 900 if recuperacao == "PASSA" else 4000}]},
        },
        "amostras": {"total": 900, "leitura_normal": leitura, "excluidas_toque": 0, "excluidas_intervalo": 0,
                     "passo_ms": 100, "primeira_ms": 0, "ultima_ms": 90000},
        "janelas_toque": [],
        "cursor_final": {"cursor": cursor, "ultimo_token": 214},
    }


class Rodada:
    def __init__(self, raiz, nome_rodada="S1", sufixo="", config=()):
        self.nome_rodada = nome_rodada
        self.sufixo, self.config = sufixo, list(config)
        self.pastas = {k: Path(raiz) / k for k in ("relatorios", "trajetorias", "eventos", "esperado", "gravacoes")}
        for p in self.pastas.values():
            p.mkdir(parents=True, exist_ok=True)
        self.oficiais = []

    def gravacao(self, nome, variante="", eventos=(), folha=(), m=None, m200=None, p200=None, sinc_motivos=(),
                 meta=None, revisada=True, sem=(), deslocamentos=(D, D - 200, D + 200)):
        self.oficiais.append((nome, variante))
        oficial, nome = nome, nome + self.sufixo
        tp, ev = self.pastas["trajetorias"], self.pastas["eventos"]
        arquivos = {"trajetoria": tp / f"{nome}.trajectory.csv",
                    "esperado": self.pastas["esperado"] / f"{oficial}.expected.csv",
                    "hipoteses": tp / f"{nome}.hypotheses.csv"}
        for papel, caminho in arquivos.items():
            caminho.write_text(f"{papel} {nome}\n", encoding="utf-8")
        app = ev / f"{nome}.events.csv"
        app.write_text(s.eventos_csv(eventos), encoding="utf-8")
        entradas = [{"papel": p, "arquivo": c.name, "sha256": _sha(c)} for p, c in arquivos.items()]
        entradas.append({"papel": "eventos", "arquivo": app.name, "sha256": _sha(app)})
        if folha:
            f = ev / f"{nome}.folha.events.csv"
            f.write_text(s.eventos_csv(folha), encoding="utf-8")
            entradas.append({"papel": "eventos", "arquivo": f.name, "sha256": _sha(f)})
        wav, level = self.pastas["gravacoes"] / f"{oficial}.wav", tp / f"{nome}.level.csv"
        wav.write_bytes(b"RIFF" + oficial.encode())
        level.write_text(f"level {nome}\n", encoding="utf-8")
        rel = self.pastas["relatorios"]
        base = m or metricas_json()
        for sufixo, dados, desl in ((".json", base, deslocamentos[0]), (".m200.json", m200 or base, deslocamentos[1]),
                                    (".p200.json", p200 or base, deslocamentos[2])):
            j = copy.deepcopy(dados)
            j["deslocamento_ms"], j["entradas"] = desl, entradas
            (rel / f"{nome}{sufixo}").write_text(json.dumps(j), encoding="utf-8")
        sinc = {"deslocamento_ms": D, "motivos": list(sinc_motivos),
                "entradas": [{"papel": "wav", "arquivo": wav.name, "sha256": _sha(wav)},
                             {"papel": "level", "arquivo": level.name, "sha256": _sha(level)}]}
        (rel / f"{nome}.sinc.json").write_text(json.dumps(sinc), encoding="utf-8")
        texto_id = nome.split("_")[0]
        valores = {**META_OFICIAL, "text_id": texto_id, **dict(c.split("=", 1) for c in self.config), **(meta or {})}
        (tp / f"{nome}.meta.txt").write_text("".join(f"{k}={v}\n" for k, v in valores.items()), encoding="utf-8")
        if revisada:
            (self.pastas["esperado"] / f"{oficial}.revisao.md").write_text(
                f"# Revisão {oficial}\n\n{veredito.REVISAO_CONCLUIDA}\n", encoding="utf-8")
        for sufixo in sem:
            (rel / f"{nome}{sufixo}").unlink()
        return self

    def completa(self, voz="giorgio", amb="sala", com_l30=True, **por_texto):
        """Todas as obrigatórias da rodada, passando, com os eventos exigidos; por_texto[ID] troca os kwargs."""
        padrao = {
            "T04": {"folha": [(1000, "improviso_inicio", ""), (2000, "improviso_fim", "")]},
            "T05": {"folha": [(1000, "pulo_inicio", "47"), (2000, "pulo_fim", "")],
                    "m": metricas_json(recuperacao="PASSA")},
            "T06": {"folha": [(1000, "silencio_inicio", ""), (31000, "silencio_fim", "")]},
            "T07": {"eventos": [(1000, "pausa_toque", ""), (11000, "retoma_toque", "")]},
            "L30": {"eventos": [(5000, "pausa_toque", ""), (25000, "retoma_toque", "")],
                    "folha": [(1000, "improviso_inicio", ""), (2000, "improviso_fim", ""),
                              (3000, "pulo_inicio", "1804"), (4000, "pulo_fim", ""),
                              (30000, "silencio_inicio", ""), (60000, "silencio_fim", "")],
                    "m": metricas_json(recuperacao="PASSA")},
        }
        for texto_id, variante in veredito.obrigatorios(self.nome_rodada) + ([("L30", "")] if com_l30 and
                                                                             self.nome_rodada == "S2" else []):
            if texto_id == "L30" and not com_l30:
                continue
            kwargs = {**padrao.get(texto_id, {}), **por_texto.get(texto_id, {})}
            take = f"t{variante}" if variante else "t1"
            self.gravacao(f"{texto_id}_{voz}_{amb}_{take}", variante, **kwargs)
        return self

    def tsv(self):
        caminho = self.pastas["relatorios"] / "oficiais.tsv"
        caminho.write_text("nome\tvariante\n" + "".join(f"{n}\t{v}\n" for n, v in self.oficiais), encoding="utf-8")
        return caminho

    def rodar(self):
        saida = self.pastas["relatorios"] / "rodada.json"
        argv = ["rodada", "--rodada", self.nome_rodada, "--oficiais", str(self.tsv()),
                "--relatorios", str(self.pastas["relatorios"]), "--trajetorias", str(self.pastas["trajetorias"]),
                "--eventos", str(self.pastas["eventos"]), "--esperado", str(self.pastas["esperado"]),
                "--gravacoes", str(self.pastas["gravacoes"]), "--json", str(saida)]
        if self.sufixo:
            argv += ["--sufixo", self.sufixo]
        for c in self.config:
            argv += ["--config", c]
        codigo, out, err = rodar(veredito.main, argv)
        dados = json.loads(saida.read_text(encoding="utf-8")) if saida.exists() else None
        return codigo, dados, out, err


def _estado(dados, nome, criterio):
    return next(g for g in dados["gravacoes"] if g["nome"] == nome)["criterios"][criterio]


class RodadaTest(unittest.TestCase):
    def test_D9_rodada_completa_passando(self):
        with Pasta() as p:
            codigo, dados, out, err = Rodada(p.dir).completa().rodar()
        self.assertEqual(0, codigo, err)
        self.assertEqual({c: "PASSOU" for c in veredito.CRITERIOS}, {c: v["estado"] for c, v in dados["criterios"].items()})
        self.assertEqual({"CS1": 3, "CS2": 8, "CS3": 2, "CS4": 3},
                         {c: v["gravacoes"] for c, v in dados["criterios"].items()})
        self.assertEqual([], dados["ausentes"])
        self.assertIn("| CS1 | PASSOU | 3 |", out)
        self.assertIn("Regras de contagem", out)

    def test_D9_ligacoes_de_cada_texto(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa().rodar()
        ligados = {g["texto"]: sorted(g["criterios"]) for g in dados["gravacoes"]}
        self.assertEqual(["CS1"], ligados["T01"])
        self.assertEqual(["CS2"], ligados["T02"])
        self.assertEqual(["CS2"], ligados["T03"])
        self.assertEqual(["CS2"], ligados["T04"])
        self.assertEqual(["CS3"], ligados["T05"])
        self.assertEqual(["CS4"], ligados["T06"])
        self.assertEqual(["CS4"], ligados["T07"])
        self.assertEqual(["CS2"], ligados["T08"])
        self.assertEqual(["CS2"], ligados["T09"])
        self.assertEqual(["CS1"], ligados["T10"])
        self.assertEqual(["CS1", "CS2", "CS3", "CS4"], ligados["L30"])

    def test_D9_salao_nao_liga_cs1(self):
        with Pasta() as p:
            codigo, dados, _, _ = Rodada(p.dir, "S3").completa(amb="salao").rodar()
        self.assertEqual(0, codigo)
        self.assertEqual("NÃO SE APLICA", dados["criterios"]["CS1"]["estado"])
        self.assertNotIn("CS1", next(g for g in dados["gravacoes"] if g["texto"] == "T01")["criterios"])

    def test_D9_uma_gravacao_reprovada_reprova_o_criterio(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T09={"m": metricas_json(salto="REPROVA", movimentos=())})
            codigo, dados, _, _ = r.rodar()
        self.assertEqual(1, codigo)
        self.assertEqual("REPROVOU", dados["criterios"]["CS2"]["estado"])
        self.assertEqual(["T09_giorgio_sala_t1: REPROVOU"], dados["criterios"]["CS2"]["motivos"])
        self.assertEqual("PASSOU", dados["criterios"]["CS1"]["estado"])

    def test_D9_recuo_reprova_cs2(self):
        with Pasta() as p:
            codigo, dados, _, _ = Rodada(p.dir).completa(T08={"m": metricas_json(recuo="REPROVA")}).rodar()
        self.assertEqual("REPROVOU", _estado(dados, "T08_giorgio_sala_t1", "CS2")["estado"])

    def test_D8_gravacao_obrigatoria_ausente_nao_mede(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            r.oficiais = [o for o in r.oficiais if not o[0].startswith("T10")]
            codigo, dados, _, _ = r.rodar()
        self.assertEqual(1, codigo)
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS1"]["estado"])
        self.assertIn("T10 (ausente): NÃO MEDIDO", dados["criterios"]["CS1"]["motivos"])
        self.assertEqual(["T10"], dados["ausentes"])

    def test_D8_variante_do_t02_ausente_nao_mede(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            r.oficiais = [o for o in r.oficiais if o != ("T02_giorgio_sala_t2", "2")]
            _, dados, _, _ = r.rodar()
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS2"]["estado"])
        self.assertEqual(["T02 variante 2"], dados["ausentes"])

    def test_D8_l30_opcional_na_s2(self):
        with Pasta() as p:
            codigo, dados, _, _ = Rodada(p.dir, "S2").completa(voz="voz2", com_l30=False).rodar()
        self.assertEqual(0, codigo)
        self.assertEqual({"CS1": 2, "CS2": 7, "CS3": 1, "CS4": 2},
                         {c: v["gravacoes"] for c, v in dados["criterios"].items()})
        with Pasta() as p:
            r = Rodada(p.dir, "S1").completa(com_l30=False)
            _, dados, _, _ = r.rodar()
        self.assertEqual(["L30"], dados["ausentes"])

    def test_C1_arquivo_que_falta_nao_mede(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T06={"sem": (".p200.json",)})
            _, dados, _, _ = r.rodar()
        v = _estado(dados, "T06_giorgio_sala_t1", "CS4")
        self.assertEqual("NÃO MEDIDO", v["estado"])
        self.assertIn("T06_giorgio_sala_t1.p200.json", v["motivo"])


class SensibilidadeTest(unittest.TestCase):
    def test_D3_passa_em_d_e_reprova_em_d_mais_200_vira_sensivel(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T01={"p200": metricas_json(atraso="REPROVA")})
            codigo, dados, _, _ = r.rodar()
        v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
        self.assertEqual("SENSÍVEL AO DESLOCAMENTO", v["estado"])
        self.assertIn("PASSOU, REPROVOU", v["motivo"])
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS1"]["estado"])
        self.assertEqual(1, codigo)

    def test_D3_passa_em_d_e_reprova_em_d_menos_200_vira_sensivel(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T01={"m200": metricas_json(atraso="REPROVA")}).rodar()
        self.assertEqual("SENSÍVEL AO DESLOCAMENTO", _estado(dados, "T01_giorgio_sala_t1", "CS1")["estado"])

    def test_D3_reprova_em_d_continua_reprovado_e_marca_sensivel(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T01={"m": metricas_json(atraso="REPROVA"), "m200": metricas_json()})
            _, dados, _, _ = r.rodar()
        v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
        self.assertEqual(("REPROVOU", True), (v["estado"], v["sensivel"]))
        self.assertEqual("REPROVOU", dados["criterios"]["CS1"]["estado"])

    def test_D3_deriva_ou_palma_da_sincronizacao_vira_sensivel(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T10={"sinc_motivos": ["deriva acima de 200 ms"]})
            _, dados, _, _ = r.rodar()
        v = _estado(dados, "T10_giorgio_sala_t1", "CS1")
        self.assertEqual("SENSÍVEL AO DESLOCAMENTO", v["estado"])
        self.assertIn("deriva acima de 200 ms", v["motivo"])

    def test_D3_deslocamentos_inconsistentes_saem_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T01={"deslocamentos": (D, D - 100, D + 200)})
            codigo, dados, out, err = r.rodar()
        self.assertEqual((2, None, ""), (codigo, dados, out))
        self.assertIn("T01_giorgio_sala_t1.m200.json com deslocamento 1270, esperado 1170", err)


class EventosTest(unittest.TestCase):
    def test_Q3_t06_sem_silencio_na_folha_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T06={"folha": ()}).rodar()
        v = _estado(dados, "T06_giorgio_sala_t1", "CS4")
        self.assertEqual(("NÃO MEDIDO", "sem silencio nos eventos"), (v["estado"], v["motivo"]))

    def test_Q3_l30_sem_pausa_nao_mede_cs4_mas_mede_cs2(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(L30={"eventos": ()}).rodar()
        self.assertEqual("NÃO MEDIDO", _estado(dados, "L30_giorgio_sala_t1", "CS4")["estado"])
        self.assertEqual("sem pausa nos eventos", _estado(dados, "L30_giorgio_sala_t1", "CS4")["motivo"])
        self.assertEqual("PASSOU", _estado(dados, "L30_giorgio_sala_t1", "CS2")["estado"])

    def test_Q3_l30_sem_silencio_ou_sem_improviso_nao_mede(self):
        folha = [(3000, "pulo_inicio", "1804"), (4000, "pulo_fim", ""),
                 (30000, "silencio_inicio", ""), (60000, "silencio_fim", "")]
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(L30={"folha": folha}).rodar()
        self.assertEqual("sem improviso nos eventos", _estado(dados, "L30_giorgio_sala_t1", "CS2")["motivo"])
        self.assertEqual("PASSOU", _estado(dados, "L30_giorgio_sala_t1", "CS4")["estado"])
        folha = [(1000, "improviso_inicio", ""), (2000, "improviso_fim", ""), (3000, "pulo_inicio", "1804"),
                 (4000, "pulo_fim", "")]
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(L30={"folha": folha}).rodar()
        self.assertEqual("sem silencio nos eventos", _estado(dados, "L30_giorgio_sala_t1", "CS4")["motivo"])

    def test_Q3_t04_sem_improviso_nao_mede_cs2(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T04={"folha": ()}).rodar()
        self.assertEqual("sem improviso nos eventos", _estado(dados, "T04_giorgio_sala_t1", "CS2")["motivo"])

    def test_eventos_desemparelhados_saem_com_2(self):
        with Pasta() as p:
            codigo, _, _, err = Rodada(p.dir).completa(T06={"folha": [(1000, "silencio_inicio", "")]}).rodar()
        self.assertEqual(2, codigo)
        self.assertIn("sem fim", err)

    def test_Q3_t07_sem_pausa_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T07={"eventos": ()}).rodar()
        self.assertEqual("sem pausa nos eventos", _estado(dados, "T07_giorgio_sala_t1", "CS4")["motivo"])

    def test_Q3_t05_sem_pulo_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T05={"folha": ()}).rodar()
        self.assertEqual("sem pulo nos eventos", _estado(dados, "T05_giorgio_sala_t1", "CS3")["motivo"])

    def test_D9_movimento_no_silencio_ou_na_pausa_reprova_cs4_e_no_improviso_nao(self):
        for intervalo, esperado in (("silencio", "REPROVOU"), ("pausa", "REPROVOU"), ("improviso", "PASSOU")):
            with self.subTest(intervalo=intervalo), Pasta() as p:
                m = metricas_json(salto="REPROVA", movimentos=[(intervalo, "alcance")])
                _, dados, _, _ = Rodada(p.dir).completa(T06={"m": m}).rodar()
                v = _estado(dados, "T06_giorgio_sala_t1", "CS4")
                self.assertEqual(esperado, v["estado"])
                self.assertEqual(1 if esperado == "REPROVOU" else 0, v["numeros"]["movimentos_por_classe"]["alcance"])

    def test_E3_classes_e_janela_de_toque_contadas(self):
        m = metricas_json(salto="REPROVA", movimentos=[("improviso", "alcance"), ("silencio", "além do esperado")])
        m["janelas_toque"] = [{"indice": 3, "inicio_ms": 0, "fim_ms": 900, "amostras_excluidas": 9}]
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(L30={"m": m}).rodar()
        n = _estado(dados, "L30_giorgio_sala_t1", "CS2")["numeros"]
        self.assertEqual({"alcance": 1, "além do esperado": 1}, n["movimentos_por_classe"])
        self.assertEqual(9, n["amostras_janela_toque"])
        self.assertEqual(round(150 / 214, 4), n["fracao_lida"])


class AmostrasTest(unittest.TestCase):
    def test_Q3_cs1_sem_amostras_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T01={"m": metricas_json(atraso="REPROVA", amostras=0)}).rodar()
        v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
        self.assertEqual(("NÃO MEDIDO", "sem amostras de leitura normal"), (v["estado"], v["motivo"]))

    def test_Q3_cs2_cursor_parado_no_zero_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T09={"m": metricas_json(cursor=0)}).rodar()
        self.assertEqual("o cursor não saiu do token 0", _estado(dados, "T09_giorgio_sala_t1", "CS2")["motivo"])

    def test_Q3_cs2_sem_leitura_normal_nao_mede(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T09={"m": metricas_json(leitura=0)}).rodar()
        self.assertEqual("sem amostras de leitura normal", _estado(dados, "T09_giorgio_sala_t1", "CS2")["motivo"])

    def test_cs3_reprova_com_pulo_lento(self):
        with Pasta() as p:
            _, dados, _, _ = Rodada(p.dir).completa(T05={"m": metricas_json(recuperacao="REPROVA")}).rodar()
        v = _estado(dados, "T05_giorgio_sala_t1", "CS3")
        self.assertEqual("REPROVOU", v["estado"])
        self.assertEqual(4000, v["numeros"]["pulos"][0]["recuperacao_ms"])


class RevisaoEConfiguracaoTest(unittest.TestCase):
    def test_D7_sem_revisao_fica_pendente(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T09={"m": metricas_json(salto="REPROVA"), "revisada": False})
            codigo, dados, out, _ = r.rodar()
        v = _estado(dados, "T09_giorgio_sala_t1", "CS2")
        self.assertEqual("PENDENTE DE REVISÃO", v["estado"])
        self.assertIn("sem ela seria REPROVOU", v["motivo"])
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS2"]["estado"])
        self.assertEqual(["T09_giorgio_sala_t1 CS2"], dados["pendencias"])
        self.assertEqual(1, codigo)
        self.assertIn("Pendências de revisão: T09_giorgio_sala_t1 CS2", out)

    def test_D7_sensivel_sem_revisao_fica_pendente(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa(T01={"p200": metricas_json(atraso="REPROVA"), "revisada": False})
            _, dados, _, _ = r.rodar()
        v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
        self.assertEqual("PENDENTE DE REVISÃO", v["estado"])
        self.assertIn("sem ela seria SENSÍVEL AO DESLOCAMENTO", v["motivo"])

    def test_D7_revisao_sem_a_linha_exata_fica_pendente(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            (r.pastas["esperado"] / "T01_giorgio_sala_t1.revisao.md").write_text("Revisão concluída: não\n",
                                                                                   encoding="utf-8")
            _, dados, _, _ = r.rodar()
        self.assertEqual("PENDENTE DE REVISÃO", _estado(dados, "T01_giorgio_sala_t1", "CS1")["estado"])

    def test_D10_configuracao_diferente_nao_mede(self):
        for chave, valor in (("preset", "Agil"), ("vocabulary", "on"), ("recognizer_strategy", "Reset"),
                             ("level_threshold", "0.02"), ("animation_ms", "600"), ("audio_source", "wav")):
            with self.subTest(chave=chave), Pasta() as p:
                _, dados, _, _ = Rodada(p.dir).completa(T01={"meta": {chave: valor}}).rodar()
                v = _estado(dados, "T01_giorgio_sala_t1", "CS1")
                self.assertEqual(("NÃO MEDIDO", False), (v["estado"], v["sensivel"]))
                self.assertIn(f"{chave}={valor}", v["motivo"])

    def test_C2_text_id_diferente_sai_com_2(self):
        with Pasta() as p:
            codigo, _, _, err = Rodada(p.dir).completa(T01={"meta": {"text_id": "T10"}}).rodar()
        self.assertEqual(2, codigo)
        self.assertIn("text_id=T10", err)


class ProvenienciaTest(unittest.TestCase):
    def test_Q8_arquivo_mudado_depois_do_relatorio_sai_com_2(self):
        for pasta, arquivo in (("esperado", "T01_giorgio_sala_t1.expected.csv"),
                               ("eventos", "T06_giorgio_sala_t1.folha.events.csv"),
                               ("gravacoes", "T01_giorgio_sala_t1.wav"),
                               ("trajetorias", "T01_giorgio_sala_t1.level.csv")):
            with self.subTest(arquivo=arquivo), Pasta() as p:
                r = Rodada(p.dir).completa()
                with open(r.pastas[pasta] / arquivo, "a", encoding="utf-8") as f:
                    f.write("x")
                codigo, _, _, err = r.rodar()
                self.assertEqual(2, codigo)
                self.assertIn(f"{arquivo} mudou ou sumiu", err)

    def test_Q8_relatorios_com_arquivos_diferentes_saem_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            caminho = r.pastas["relatorios"] / "T01_giorgio_sala_t1.p200.json"
            j = json.loads(caminho.read_text(encoding="utf-8"))
            j["entradas"][0]["sha256"] = "0" * 64
            caminho.write_text(json.dumps(j), encoding="utf-8")
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn("não usaram os mesmos arquivos", err)

    def test_Q8_relatorio_de_outra_gravacao_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            rel = r.pastas["relatorios"]
            for suf in (".json", ".m200.json", ".p200.json"):
                (rel / f"T10_giorgio_sala_t1{suf}").write_text((rel / f"T01_giorgio_sala_t1{suf}").read_text(
                    encoding="utf-8"), encoding="utf-8")
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn("T10_giorgio_sala_t1: o relatório usou T01_giorgio_sala_t1.trajectory.csv", err)


class OficiaisTest(unittest.TestCase):
    def test_lista_invalida_sai_com_2(self):
        casos = {
            "cabeçalho": "nome,variante\n",
            "voz de outra rodada": "nome\tvariante\nT01_voz2_sala_t1\t\n",
            "ambiente de outra rodada": "nome\tvariante\nT01_giorgio_salao_t1\t\n",
            "T02 sem variante": "nome\tvariante\nT02_giorgio_sala_t1\t\n",
            "variante fora do T02": "nome\tvariante\nT01_giorgio_sala_t1\t1\n",
            "variante 4": "nome\tvariante\nT02_giorgio_sala_t4\t4\n",
            "repetido": "nome\tvariante\nT01_giorgio_sala_t1\t\nT01_giorgio_sala_t2\t\n",
            "texto que não é de teste": "nome\tvariante\nX99_giorgio_sala_t1\t\n",
            "campos": "nome\tvariante\nT01_giorgio_sala_t1\n",
        }
        for nome, conteudo in casos.items():
            with self.subTest(caso=nome):
                with self.assertRaises(veredito.ErroEntrada):
                    veredito.ler_oficiais(conteudo, "S1")

    def test_take_refeito_vale_pela_lista(self):
        lista = veredito.ler_oficiais("nome\tvariante\nT01_giorgio_sala_t2\t\nT02_giorgio_sala_t4\t2\n", "S1")
        self.assertEqual([("T01_giorgio_sala_t2", "T01", ""), ("T02_giorgio_sala_t4", "T02", "2")], lista)

    def test_json_invalido_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            (r.pastas["relatorios"] / "T01_giorgio_sala_t1.sinc.json").write_text("{", encoding="utf-8")
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn("JSON inválido", err)

    def test_json_sem_campo_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir).completa()
            (r.pastas["relatorios"] / "T01_giorgio_sala_t1.sinc.json").write_text("{}", encoding="utf-8")
            codigo, _, _, err = r.rodar()
        self.assertEqual(2, codigo)
        self.assertIn("JSON sem o campo esperado", err)


class ReproducaoTest(unittest.TestCase):
    CONFIG = ("preset=Agil", "audio_source=wav")

    def test_D11_reproducao_usa_os_arquivos_dela_e_o_esperado_da_oficial(self):
        with Pasta() as p:
            r = Rodada(p.dir, sufixo="_r_agil_off_novo_l0000", config=self.CONFIG).completa()
            codigo, dados, out, err = r.rodar()
        self.assertEqual(0, codigo, err)
        self.assertEqual(("T01_giorgio_sala_t1_r_agil_off_novo_l0000", "T01_giorgio_sala_t1"),
                         (dados["gravacoes"][0]["nome"], dados["gravacoes"][0]["oficial"]))
        self.assertEqual("Agil", dados["config"]["preset"])
        self.assertIn("reprodução _r_agil_off_novo_l0000", out)
        self.assertIn("preset=Agil", out)

    def test_D11_reproducao_com_meta_da_configuracao_oficial_nao_mede(self):
        with Pasta() as p:
            r = Rodada(p.dir, sufixo="_r_agil_off_novo_l0000", config=("audio_source=wav",))
            r.completa()
            r.config = list(self.CONFIG)
            _, dados, _, _ = r.rodar()
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS1"]["estado"])
        self.assertIn("preset=Normal", _estado(dados, "T01_giorgio_sala_t1_r_agil_off_novo_l0000", "CS1")["motivo"])

    def test_config_e_sufixo_invalidos_saem_com_2(self):
        for sufixo, config in (("_r-agil", ()), ("_R_AGIL", ()), ("_r_agil", ("cor=azul",)), ("_r_agil", ("preset",))):
            with self.subTest(sufixo=sufixo, config=config), Pasta() as p:
                r = Rodada(p.dir).completa()
                r.sufixo, r.config = sufixo, list(config)
                codigo, _, _, _ = r.rodar()
                self.assertEqual(2, codigo)

    def test_final_recusa_rodada_de_reproducao(self):
        with Pasta() as p:
            r = Rodada(p.dir, sufixo="_r_agil_off_novo_l0000", config=self.CONFIG).completa()
            r.rodar()
            codigo, _, err = rodar(veredito.main, ["final", str(r.pastas["relatorios"] / "rodada.json")])
        self.assertEqual(2, codigo)
        self.assertIn("reprodução", err)


class FinalTest(unittest.TestCase):
    def _rodadas(self, p, **kwargs):
        caminhos = []
        for nome, (voz, amb, com_l30) in (("S1", ("giorgio", "sala", True)), ("S2", ("voz2", "sala", False)),
                                          ("S3", ("giorgio", "salao", True))):
            if nome in kwargs.get("sem", ()):
                continue
            r = Rodada(p.dir / nome, nome).completa(voz=voz, amb=amb, com_l30=com_l30, **kwargs.get(nome, {}))
            r.rodar()
            caminhos.append(str(r.pastas["relatorios"] / "rodada.json"))
        saida = p.dir / "final.json"
        codigo, out, err = rodar(veredito.main, ["final", *caminhos, "--json", str(saida)])
        dados = json.loads(saida.read_text(encoding="utf-8")) if saida.exists() else None
        return codigo, dados, out, err

    def test_H1_tres_rodadas_passando(self):
        with Pasta() as p:
            codigo, dados, out, err = self._rodadas(p)
        self.assertEqual(0, codigo, err)
        self.assertEqual({c: "PASSOU" for c in veredito.CRITERIOS}, {c: v["estado"] for c, v in dados["criterios"].items()})
        self.assertEqual(["S1", "S2"], dados["criterios"]["CS1"]["rodadas"])
        self.assertEqual(["S1", "S2", "S3"], dados["criterios"]["CS4"]["rodadas"])
        self.assertEqual(3 + 2, dados["criterios"]["CS1"]["gravacoes"])
        self.assertIn("| CS1 | PASSOU | S1, S2 | 5 |", out)

    def test_D8_sem_segunda_voz_cs1_nao_mede_e_o_resto_passa(self):
        with Pasta() as p:
            codigo, dados, _, _ = self._rodadas(p, sem=("S2",))
        self.assertEqual(1, codigo)
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS1"]["estado"])
        self.assertIn("rodada S2 ausente", dados["criterios"]["CS1"]["motivos"])
        self.assertEqual("PASSOU", dados["criterios"]["CS2"]["estado"])

    def test_D8_sem_salao_cs2_a_cs4_nao_medem(self):
        with Pasta() as p:
            _, dados, _, _ = self._rodadas(p, sem=("S3",))
        self.assertEqual("PASSOU", dados["criterios"]["CS1"]["estado"])
        for c in ("CS2", "CS3", "CS4"):
            self.assertEqual("NÃO MEDIDO", dados["criterios"][c]["estado"])
            self.assertIn("rodada S3 ausente", dados["criterios"][c]["motivos"])

    def test_D9_reprovacao_numa_rodada_reprova_o_criterio(self):
        with Pasta() as p:
            _, dados, _, _ = self._rodadas(p, S3={"T03": {"m": metricas_json(salto="REPROVA")}})
        self.assertEqual("REPROVOU", dados["criterios"]["CS2"]["estado"])
        self.assertIn("S3: REPROVOU", dados["criterios"]["CS2"]["motivos"])

    def test_D9_reprovacao_vence_rodada_ausente(self):
        with Pasta() as p:
            _, dados, _, _ = self._rodadas(p, sem=("S3",), S1={"T03": {"m": metricas_json(salto="REPROVA")}})
        self.assertEqual("REPROVOU", dados["criterios"]["CS2"]["estado"])

    def test_D7_pendencia_numa_rodada_vira_nao_medido_no_final(self):
        with Pasta() as p:
            codigo, dados, _, _ = self._rodadas(p, S2={"T01": {"revisada": False}})
        self.assertEqual("NÃO MEDIDO", dados["criterios"]["CS1"]["estado"])
        self.assertEqual(["T01_voz2_sala_t1 CS1"], dados["pendencias"])
        self.assertEqual(1, codigo)

    def test_rodada_repetida_ou_json_estranho_sai_com_2(self):
        with Pasta() as p:
            r = Rodada(p.dir / "S1").completa()
            r.rodar()
            caminho = str(r.pastas["relatorios"] / "rodada.json")
            codigo, _, err = rodar(veredito.main, ["final", caminho, caminho])
            self.assertEqual(2, codigo)
            self.assertIn("repetida", err)
            codigo, _, _ = rodar(veredito.main, ["final", p.arquivo("x.json", "{}")])
            self.assertEqual(2, codigo)


class MetricasDeVerdadeTest(unittest.TestCase):
    def test_Q4_campos_lidos_existem_no_json_do_metricas(self):
        tempos = s.tempos_regulares(len(s.tokens("T01")))
        linhas = s.trajetoria_seguindo(tempos, lambda t, e: e - 1)
        jsons = {}
        for desl in (0, -200, 200):
            codigo, dados, _, err = s.rodar_metricas(metricas.main, s.trajetoria_csv(linhas),
                                                     s.esperado_csv("T01", tempos), [s.eventos_csv([])],
                                                     deslocamento=desl)
            self.assertIn(codigo, (0, 1), err)
            jsons[desl] = dados
        for c in veredito.CRITERIOS:
            with self.subTest(criterio=c):
                estado, _ = veredito.estado_base(c, jsons[0])
                self.assertIn(estado, ("PASSOU", "REPROVOU", "NÃO MEDIDO"))
                veredito.numeros(c, jsons[0], {"improviso": 0, "silencio": 0, "pausa": 0, "pulo": 0})
        self.assertEqual("PASSOU", veredito.estado_base("CS1", jsons[0])[0])
        self.assertEqual("PASSOU", veredito.estado_base("CS2", jsons[0])[0])
        self.assertEqual("NÃO MEDIDO", veredito.estado_base("CS3", jsons[0])[0])
        self.assertEqual(-200, jsons[-200]["deslocamento_ms"])
        self.assertEqual({"trajetoria", "esperado", "eventos"}, {e["papel"] for e in jsons[0]["entradas"]})


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Passo 2: ver falhar**

Rodar: `python3 etapa0/ferramentas/testar.py test_veredito.py`
Esperado: `ModuleNotFoundError: No module named 'veredito'`; `testar.py: 1 testes, 0 falhas, 1 erros, 0 pulados`.

- [ ] **Passo 3: escrever a ferramenta**

`etapa0/ferramentas/veredito.py`:

```python
"""veredito.py rodada|final: junta os relatórios do metricas.py nos critérios de saída CS1 a CS4 da Etapa 0.

spec 0005, D3, D7, D8, D9, D10 e critérios C1, C2, E2, E3, H1; impl_plan 0005, A5.

veredito.py rodada --rodada S1 --oficiais <tsv> [--relatorios DIR] ... [--json SAIDA]
  Para cada gravação oficial da rodada lê <nome>.json, <nome>.m200.json e <nome>.p200.json (metricas.py com o
  deslocamento d, d − 200 e d + 200), <nome>.sinc.json (sincronizar.py), <nome>.meta.txt e
  <nome>.revisao.md, e dá o estado de cada critério ligado:
  PASSOU, REPROVOU, NÃO MEDIDO, SENSÍVEL AO DESLOCAMENTO ou PENDENTE DE REVISÃO.
  Com --sufixo S e --config chave=valor (reprodução para calibrar, D11): os arquivos da reprodução são
  <nome>S.* (relatórios, trajetória, eventos, level, hipóteses, meta), e o esperado, o WAV e a revisão continuam
  os da gravação oficial <nome>; a configuração exigida é a oficial com as chaves trocadas.
veredito.py final <rodada.json>... [--json SAIDA]
  Junta as rodadas: CS1 exige S1 e S2 (cada voz, sala); CS2 a CS4 exigem S1 e S3 (os dois ambientes); a S2 entra
  em CS2 a CS4 quando existe.

Regras (impl_plan A5): toda gravação ligada tem de passar (D9); um critério só PASSOU com todas as gravações
exigidas presentes e PASSOU; REPROVOU basta uma; o resto é NÃO MEDIDO, com o motivo. Arquivo que mudou depois
do relatório (sha256 diferente) é entrada inválida (Q8).
Códigos de saída: 0 todos os critérios aplicáveis PASSOU e nada pendente; 1 algum não passou ou há pendência;
2 entrada inválida.
"""

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

import metricas
import roteiro

PASSOU, REPROVOU, NAO_MEDIDO = "PASSOU", "REPROVOU", "NÃO MEDIDO"
SENSIVEL, PENDENTE, NAO_SE_APLICA = "SENSÍVEL AO DESLOCAMENTO", "PENDENTE DE REVISÃO", "NÃO SE APLICA"
TOLERANCIA_MS = 200  # spec D3
REVISAO_CONCLUIDA = "Revisão concluída: sim"

RODADAS = {  # spec D8: voz, ambiente, L30 obrigatório
    "S1": ("giorgio", "sala", True),
    "S2": ("voz2", "sala", False),
    "S3": ("giorgio", "salao", True),
}
CONFIG_OFICIAL = {  # spec D10 (chaves do meta.txt da 0003)
    "preset": "Normal",
    "vocabulary": "off",
    "recognizer_strategy": "NewPerSession",
    "level_threshold": "0.0",
    "animation_ms": "375",
    "audio_source": "mic",
}
CRITERIOS = ("CS1", "CS2", "CS3", "CS4")
LIGADOS = {  # spec D9
    "CS1": ("T01", "T10", "L30"),
    "CS2": ("L30", "T02", "T03", "T04", "T08", "T09"),
    "CS3": ("T05", "L30"),
    "CS4": ("T06", "T07", "L30"),
}
SO_SALA = ("CS1",)  # CS1: "em ambiente silencioso"
EXIGIDOS = {  # eventos que a gravação precisa ter para o critério medir algo (Q3)
    ("CS2", "T04"): ("improviso",), ("CS2", "L30"): ("improviso",),
    ("CS3", "T05"): ("pulo",), ("CS3", "L30"): ("pulo",),
    ("CS4", "T06"): ("silencio",), ("CS4", "T07"): ("pausa",), ("CS4", "L30"): ("pausa", "silencio"),
}
REGRAS = {  # regra de contagem, ao lado do número (Q7)
    "CS1": "esperado − cursor <= 3 em >= 95% das amostras de 100 ms de leitura normal (fora de improviso, "
           "silêncio, pausa, 3 s depois do pulo e janela de toque)",
    "CS2": "zero amostras com cursor > esperado + 2 fora da janela de toque, zero movimentos automáticos em "
           "improviso, silêncio ou pausa, zero recuos abaixo de highWater − 3",
    "CS3": "do pulo_fim até a primeira amostra com esperado no alvo e atraso <= 3, <= 3.000 ms, em todo pulo",
    "CS4": "zero movimentos automáticos (Short, LongJump, Recovery) dentro de silêncio ou pausa",
}
EXIGIDAS_FINAL = {"CS1": ("S1", "S2"), "CS2": ("S1", "S3"), "CS3": ("S1", "S3"), "CS4": ("S1", "S3")}
_NOME = re.compile(r"([A-Z0-9]+)_([a-z0-9]+)_([a-z]+)_t([0-9]+)")


class ErroEntrada(Exception):
    """Código de saída 2."""


# ------------------------------------------------------------------ leitura


def _sha256(caminho):
    h = hashlib.sha256()
    with open(caminho, "rb") as f:
        for bloco in iter(lambda: f.read(1 << 20), b""):
            h.update(bloco)
    return h.hexdigest()


def _json(caminho):
    try:
        with open(caminho, encoding="utf-8") as f:
            return json.load(f)
    except json.JSONDecodeError as erro:
        raise ErroEntrada(f"{Path(caminho).name}: JSON inválido ({erro})") from None


def obrigatorios(rodada):
    _voz, _amb, com_l30 = RODADAS[rodada]
    lista = [("T01", "")] + [("T02", v) for v in ("1", "2", "3")]
    lista += [(f"T{n:02d}", "") for n in range(3, 11)]
    return lista + ([("L30", "")] if com_l30 else [])


def ler_oficiais(conteudo, rodada):
    """oficiais.tsv (nome<TAB>variante) -> lista de (nome, ID, variante). Valida voz, ambiente e repetição."""
    voz, amb, _ = RODADAS[rodada]
    linhas = [linha.split("\t") for linha in conteudo.splitlines() if linha.strip()]
    if not linhas or linhas[0] != ["nome", "variante"]:
        raise ErroEntrada("oficiais: cabeçalho diferente de nome<TAB>variante")
    saida, vistos = [], set()
    for n, campos in enumerate(linhas[1:], start=1):
        if len(campos) != 2:
            raise ErroEntrada(f"oficiais: linha {n} sem 2 campos")
        nome, variante = campos
        m = _NOME.fullmatch(nome)
        if m is None or m.group(1) not in roteiro.IDS:
            raise ErroEntrada(f"oficiais: linha {n}: nome fora do padrão <ID>_<voz>_<ambiente>_t<n>")
        texto_id = m.group(1)
        if (m.group(2), m.group(3)) != (voz, amb):
            raise ErroEntrada(f"oficiais: linha {n}: {nome} não é da rodada {rodada} ({voz}, {amb})")
        if (texto_id == "T02") != (variante in ("1", "2", "3")) or (texto_id != "T02" and variante != ""):
            raise ErroEntrada(f"oficiais: linha {n}: variante só no T02, e lá é 1, 2 ou 3")
        if (texto_id, variante) in vistos:
            raise ErroEntrada(f"oficiais: linha {n}: {texto_id} {variante} repetido")
        vistos.add((texto_id, variante))
        saida.append((nome, texto_id, variante))
    return saida


def ler_meta(caminho):
    meta = {}
    for linha in Path(caminho).read_text(encoding="utf-8").splitlines():
        if "=" in linha:
            chave, valor = linha.split("=", 1)
            meta[chave] = valor
    return meta


def contar_eventos(conteudos):
    """Intervalos por tipo nos arquivos de eventos que o relatório usou, com a validação do metricas.py."""
    try:
        faixas = metricas.intervalos(metricas.ler_eventos(conteudos, 0, 10**9))
    except metricas.ErroEntrada as erro:
        raise ErroEntrada(str(erro)) from None
    return {k: len(faixas[k]) for k in ("improviso", "silencio", "pausa", "pulo")}


# ------------------------------------------------------------------ estado por gravação


def estado_base(criterio, m):
    """(estado, motivo) de um critério num relatório do metricas.py, sem sensibilidade nem revisão."""
    c = m["criterios"]
    if criterio == "CS1":
        a = c["atraso"]
        if a["amostras"] == 0:
            return NAO_MEDIDO, "sem amostras de leitura normal"
        return (PASSOU, None) if a["veredito"] == "PASSA" else (REPROVOU, None)
    if criterio == "CS2":
        if m["amostras"]["leitura_normal"] == 0:
            return NAO_MEDIDO, "sem amostras de leitura normal"
        if m["cursor_final"]["cursor"] == 0:
            return NAO_MEDIDO, "o cursor não saiu do token 0"
        ruim = c["salto_errado"]["veredito"] == "REPROVA" or c["recuo"]["veredito"] == "REPROVA"
        return (REPROVOU, None) if ruim else (PASSOU, None)
    if criterio == "CS3":
        r = c["recuperacao"]["veredito"]
        if r == "NAO_SE_APLICA":
            return NAO_MEDIDO, "sem pulo na folha de eventos"
        return (PASSOU, None) if r == "PASSA" else (REPROVOU, None)
    movimentos = [mv for mv in c["salto_errado"]["por_movimento"] if mv["intervalo"] in ("silencio", "pausa")]
    return (REPROVOU, None) if movimentos else (PASSOU, None)


def numeros(criterio, m, eventos):
    c = m["criterios"]
    if criterio == "CS1":
        a = c["atraso"]
        return {"amostras": a["amostras"], "ate_3": a["ate_3"], "fracao": a["fracao"], "maior_atraso": a["maior_atraso"]}
    if criterio == "CS2":
        s = c["salto_errado"]
        classes = {}
        for mv in s["por_movimento"]:
            classes[mv["classe"]] = classes.get(mv["classe"], 0) + 1
        cf = m["cursor_final"]
        return {"amostras": m["amostras"]["total"], "leitura_normal": m["amostras"]["leitura_normal"],
                "episodios_posicao": len(s["por_posicao"]), "movimentos": len(s["por_movimento"]),
                "movimentos_por_classe": classes, "recuos": len(c["recuo"]["violacoes"]),
                "amostras_janela_toque": sum(j["amostras_excluidas"] for j in m["janelas_toque"]),
                "fracao_lida": round(cf["cursor"] / cf["ultimo_token"], 4) if cf["ultimo_token"] else None,
                "eventos": eventos}
    if criterio == "CS3":
        return {"pulos": [{"recuperacao_ms": p["recuperacao_ms"], "alvo": p["alvo"],
                           "primeiro_pulado": p["primeiro_pulado"]} for p in c["recuperacao"]["pulos"]],
                "eventos": eventos}
    movimentos = [mv for mv in c["salto_errado"]["por_movimento"] if mv["intervalo"] in ("silencio", "pausa")]
    return {"intervalos": {"silencio": eventos["silencio"], "pausa": eventos["pausa"]}, "movimentos": len(movimentos),
            "movimentos_por_classe": {k: sum(1 for mv in movimentos if mv["classe"] == k)
                                      for k in ("alcance", "além do esperado")}}


def ligado(criterio, texto_id, ambiente):
    return texto_id in LIGADOS[criterio] and (criterio not in SO_SALA or ambiente == "sala")


def conferir_entradas(entradas, pastas, nome, oficial=None):
    """sha256 de cada arquivo citado pelo relatório igual ao do disco (Q8). Devolve {papel: [caminhos]}.
    `oficial`: nome da gravação oficial, dono do esperado e do WAV quando `nome` é uma reprodução."""
    caminhos = {}
    sufixos = {"trajetoria": ".trajectory.csv", "esperado": ".expected.csv", "hipoteses": ".hypotheses.csv",
               "wav": ".wav", "level": ".level.csv"}
    dono = {"esperado": oficial or nome, "wav": oficial or nome}
    for e in entradas:
        papel, arquivo = e["papel"], e["arquivo"]
        if papel in sufixos and arquivo != dono.get(papel, nome) + sufixos[papel]:
            raise ErroEntrada(f"{nome}: o relatório usou {arquivo} como {papel}")
        if papel == "eventos" and not arquivo.startswith(nome + "."):
            raise ErroEntrada(f"{nome}: o relatório usou eventos de outra gravação ({arquivo})")
        caminho = pastas[papel] / arquivo
        if not caminho.is_file() or _sha256(caminho) != e["sha256"]:
            raise ErroEntrada(f"{nome}: {arquivo} mudou ou sumiu depois do relatório (Q8); rode de novo")
        caminhos.setdefault(papel, []).append(caminho)
    return caminhos


def avaliar_gravacao(oficial, texto_id, pastas, sufixo="", config=None):
    """Estado de cada critério ligado de uma gravação da lista (ou da reprodução dela, com `sufixo`)."""
    config = config or CONFIG_OFICIAL
    nome = oficial + sufixo
    ambiente = _NOME.fullmatch(oficial).group(3)
    criterios = [c for c in CRITERIOS if ligado(c, texto_id, ambiente)]
    rel = pastas["relatorios"]
    arquivos = {"d": rel / f"{nome}.json", "m200": rel / f"{nome}.m200.json", "p200": rel / f"{nome}.p200.json",
                "sinc": rel / f"{nome}.sinc.json", "meta": pastas["trajetoria"] / f"{nome}.meta.txt"}
    faltam = sorted(p.name for p in arquivos.values() if not p.is_file())
    saida = {"nome": nome, "oficial": oficial, "texto": texto_id, "criterios": {}, "faltam": faltam, "motivos": []}
    if faltam:
        for c in criterios:
            saida["criterios"][c] = {"estado": NAO_MEDIDO, "motivo": "faltam " + ", ".join(faltam)}
        return saida
    m = {k: _json(arquivos[k]) for k in ("d", "m200", "p200")}
    sinc = _json(arquivos["sinc"])
    d = sinc["deslocamento_ms"]
    for chave, esperado in (("d", d), ("m200", d - TOLERANCIA_MS), ("p200", d + TOLERANCIA_MS)):
        if m[chave]["deslocamento_ms"] != esperado:
            raise ErroEntrada(f"{nome}: {arquivos[chave].name} com deslocamento {m[chave]['deslocamento_ms']}, "
                              f"esperado {esperado} (sincronizar deu {d})")
    proveniencia = [sorted((e["papel"], e["arquivo"], e["sha256"]) for e in m[k]["entradas"]) for k in m]
    if proveniencia[0] != proveniencia[1] or proveniencia[0] != proveniencia[2]:
        raise ErroEntrada(f"{nome}: os três relatórios do metricas.py não usaram os mesmos arquivos")
    caminhos = conferir_entradas(m["d"]["entradas"], pastas, nome, oficial)
    conferir_entradas(sinc["entradas"], pastas, nome, oficial)
    eventos = contar_eventos([c.read_text(encoding="utf-8") for c in caminhos.get("eventos", [])])
    meta = ler_meta(arquivos["meta"])
    if meta.get("text_id") != texto_id:
        raise ErroEntrada(f"{nome}: meta.txt com text_id={meta.get('text_id')}")
    diferentes = [f"{k}={meta.get(k)}" for k, v in config.items() if meta.get(k) != v]
    revisao = pastas["esperado"] / f"{oficial}.revisao.md"
    revisada = revisao.is_file() and REVISAO_CONCLUIDA in revisao.read_text(encoding="utf-8").splitlines()
    saida.update({"deslocamento_ms": d, "sinc_motivos": sinc["motivos"], "revisada": revisada, "eventos": eventos,
                  "configuracao_diferente": diferentes})
    for c in criterios:
        faltando = [k for k in EXIGIDOS.get((c, texto_id), ()) if eventos[k] == 0]
        if diferentes:
            estado, motivo = NAO_MEDIDO, "configuração diferente da pedida (D10 ou --config): " + ", ".join(diferentes)
        elif faltando:
            estado, motivo = NAO_MEDIDO, "sem " + " e ".join(faltando) + " nos eventos"
        else:
            estado, motivo = estado_base(c, m["d"])
        vizinhos = [estado_base(c, m[k])[0] for k in ("m200", "p200")]
        sensivel = estado != NAO_MEDIDO and (any(v != estado for v in vizinhos) or bool(sinc["motivos"]))
        if estado == PASSOU and sensivel:
            estado = SENSIVEL
            motivo = "; ".join(sinc["motivos"] + [f"com d−200 e d+200: {vizinhos[0]}, {vizinhos[1]}"])
        if estado in (PASSOU, REPROVOU, SENSIVEL) and not revisada:
            motivo = f"sem revisão concluída (D7); sem ela seria {estado}"
            estado = PENDENTE
        saida["criterios"][c] = {"estado": estado, "motivo": motivo, "sensivel": sensivel,
                                 "regra": REGRAS[c], "numeros": numeros(c, m["d"], eventos)}
    return saida


# ------------------------------------------------------------------ agregação


def agregar(estados):
    """estados: lista de (nome, estado). Devolve (estado do critério, motivos)."""
    if not estados:
        return NAO_SE_APLICA, []
    if any(e == REPROVOU for _, e in estados):
        return REPROVOU, [f"{n}: {e}" for n, e in estados if e == REPROVOU]
    if all(e == PASSOU for _, e in estados):
        return PASSOU, []
    return NAO_MEDIDO, [f"{n}: {e}" for n, e in estados if e != PASSOU]


def rodada(nome_rodada, oficiais, pastas, sufixo="", config=None):
    voz, amb, _ = RODADAS[nome_rodada]
    presentes = {(texto_id, variante): nome for nome, texto_id, variante in oficiais}
    gravacoes = [avaliar_gravacao(nome, texto_id, pastas, sufixo, config) for nome, texto_id, _ in oficiais]
    ausentes = [(t, v) for t, v in obrigatorios(nome_rodada) if (t, v) not in presentes]
    criterios = {}
    for c in CRITERIOS:
        estados = [(g["nome"], g["criterios"][c]["estado"]) for g in gravacoes if c in g["criterios"]]
        estados += [(f"{t}{' variante ' + v if v else ''} (ausente)", NAO_MEDIDO) for t, v in ausentes
                    if ligado(c, t, amb)]
        estado, motivos = agregar(estados)
        criterios[c] = {"estado": estado, "motivos": motivos, "gravacoes": len(estados), "regra": REGRAS[c]}
    pendencias = [f"{g['nome']} {c}" for g in gravacoes for c, v in g["criterios"].items() if v["estado"] == PENDENTE]
    return {"rodada": nome_rodada, "voz": voz, "ambiente": amb, "sufixo": sufixo, "config": config or CONFIG_OFICIAL,
            "criterios": criterios, "gravacoes": gravacoes,
            "ausentes": [f"{t}{' variante ' + v if v else ''}" for t, v in ausentes], "pendencias": pendencias}


def final(rodadas):
    por_nome = {}
    for r in rodadas:
        if r.get("rodada") not in RODADAS:
            raise ErroEntrada("final: JSON que não é de veredito.py rodada")
        if r.get("sufixo") or r.get("config") != CONFIG_OFICIAL:
            raise ErroEntrada(f"final: a rodada {r['rodada']} é de reprodução ou de outra configuração; "
                              "os critérios de saída usam só as gravações oficiais (D1, D10)")
        if r["rodada"] in por_nome:
            raise ErroEntrada(f"final: rodada {r['rodada']} repetida")
        por_nome[r["rodada"]] = r
    criterios = {}
    for c in CRITERIOS:
        exigidas = EXIGIDAS_FINAL[c]
        estados = []
        motivos = [f"rodada {s} ausente" for s in exigidas if s not in por_nome]
        for s, r in sorted(por_nome.items()):
            e = r["criterios"][c]["estado"]
            if e != NAO_SE_APLICA:
                estados.append((s, e))
        if any(e == REPROVOU for _, e in estados):
            estado = REPROVOU
        elif not motivos and estados and all(e == PASSOU for _, e in estados):
            estado = PASSOU
        else:
            estado = NAO_MEDIDO
        motivos += [f"{s}: {e}" for s, e in estados if e != PASSOU]
        gravacoes = sum(por_nome[s]["criterios"][c]["gravacoes"] for s, _ in estados)
        criterios[c] = {"estado": estado, "motivos": motivos, "rodadas": [s for s, _ in estados],
                        "gravacoes": gravacoes, "regra": REGRAS[c]}
    return {"criterios": criterios, "pendencias": [p for r in por_nome.values() for p in r["pendencias"]]}


# ------------------------------------------------------------------ relatório e linha de comando


def relatorio_rodada(res):
    titulo = f"# Veredito da rodada {res['rodada']} ({res['voz']}, {res['ambiente']})"
    if res["sufixo"]:
        titulo += f", reprodução {res['sufixo']}"
    linhas = [titulo, "", "Configuração exigida: " + ", ".join(f"{k}={v}" for k, v in res["config"].items()), "",
              "## Critérios", ""]
    linhas += ["| Critério | Estado | Gravações | Motivos |", "|---|---|---|---|"]
    for c, v in res["criterios"].items():
        linhas.append(f"| {c} | {v['estado']} | {v['gravacoes']} | {'; '.join(v['motivos']) or '-'} |")
    linhas += ["", "## Gravações", "", "| Gravação | Critério | Estado | Números | Motivo |", "|---|---|---|---|---|"]
    for g in res["gravacoes"]:
        for c, v in g["criterios"].items():
            nums = json.dumps(v.get("numeros", {}), ensure_ascii=False, sort_keys=True)
            linhas.append(f"| {g['nome']} | {c} | {v['estado']} | {nums} | {v['motivo'] or '-'} |")
    linhas += ["", "## Regras de contagem", ""] + [f"- {c}: {r}" for c, r in REGRAS.items()]
    linhas += ["", f"Ausentes: {', '.join(res['ausentes']) or 'nenhuma'}",
               f"Pendências de revisão: {', '.join(res['pendencias']) or 'nenhuma'}"]
    return "\n".join(linhas) + "\n"


def relatorio_final(res):
    linhas = ["# Critérios de saída CS1 a CS4 (voz)", "", "| Critério | Estado | Rodadas | Gravações | Motivos |",
              "|---|---|---|---|---|"]
    for c, v in res["criterios"].items():
        linhas.append(f"| {c} | {v['estado']} | {', '.join(v['rodadas']) or '-'} | {v['gravacoes']} | "
                      f"{'; '.join(v['motivos']) or '-'} |")
    linhas += ["", "## Regras de contagem", ""] + [f"- {c}: {r}" for c, r in REGRAS.items()]
    linhas += ["", f"Pendências de revisão: {', '.join(res['pendencias']) or 'nenhuma'}"]
    return "\n".join(linhas) + "\n"


def _codigo(res):
    """Pendência já deixa o critério em NÃO MEDIDO (agregar), então basta olhar os estados."""
    return 0 if all(v["estado"] in (PASSOU, NAO_SE_APLICA) for v in res["criterios"].values()) else 1


def main(argv=None):
    parser = argparse.ArgumentParser(prog="veredito.py", description="Critérios de saída CS1 a CS4 por gravação, "
                                     "por rodada e no fim (spec 0005, D9).")
    sub = parser.add_subparsers(dest="comando", required=True)
    p_rodada = sub.add_parser("rodada", help="veredito de uma rodada")
    p_rodada.add_argument("--rodada", required=True, choices=sorted(RODADAS))
    p_rodada.add_argument("--oficiais", required=True, help="TSV nome<TAB>variante das gravações oficiais")
    p_rodada.add_argument("--relatorios", help="pasta dos JSON (padrão: etapa0/relatorios/<rodada>)")
    p_rodada.add_argument("--trajetorias", default=str(roteiro.ETAPA0 / "trajetorias"))
    p_rodada.add_argument("--eventos", default=str(roteiro.ETAPA0 / "eventos"))
    p_rodada.add_argument("--esperado", default=str(roteiro.ETAPA0 / "esperado"))
    p_rodada.add_argument("--gravacoes", default=str(roteiro.ETAPA0 / "gravacoes"))
    p_rodada.add_argument("--sufixo", default="", help="reprodução: <nome><sufixo> (ex.: _r_agil_off_novo_l0000)")
    p_rodada.add_argument("--config", action="append", default=[], metavar="CHAVE=VALOR",
                          help="troca uma chave da configuração oficial (D10) exigida no meta.txt")
    p_rodada.add_argument("--json", help="grava o resultado em JSON")
    p_final = sub.add_parser("final", help="junta as rodadas nos critérios de saída")
    p_final.add_argument("rodadas", nargs="+", help="JSON de veredito.py rodada")
    p_final.add_argument("--json", help="grava o resultado em JSON")
    args = parser.parse_args(argv)
    try:
        if args.comando == "rodada":
            pastas = {"relatorios": Path(args.relatorios or roteiro.ETAPA0 / "relatorios" / args.rodada),
                      "trajetoria": Path(args.trajetorias), "hipoteses": Path(args.trajetorias),
                      "level": Path(args.trajetorias), "eventos": Path(args.eventos),
                      "esperado": Path(args.esperado), "wav": Path(args.gravacoes)}
            oficiais = ler_oficiais(Path(args.oficiais).read_text(encoding="utf-8"), args.rodada)
            config = dict(CONFIG_OFICIAL)
            for par in args.config:
                chave, sep, valor = par.partition("=")
                if not sep or chave not in CONFIG_OFICIAL:
                    raise ErroEntrada(f"--config {par}: use chave=valor com chave em {', '.join(CONFIG_OFICIAL)}")
                config[chave] = valor
            if args.sufixo and not re.fullmatch(r"(_[a-z0-9]+)+", args.sufixo):
                raise ErroEntrada("--sufixo: só _[a-z0-9]+ (o nome da exportação do app não aceita outro caractere)")
            res = rodada(args.rodada, oficiais, pastas, args.sufixo, config)
            texto = relatorio_rodada(res)
        else:
            res = final([_json(c) for c in args.rodadas])
            texto = relatorio_final(res)
    except (OSError, UnicodeDecodeError, ErroEntrada) as erro:
        print(f"veredito.py: {erro}", file=sys.stderr)
        return 2
    except (KeyError, TypeError) as erro:
        print(f"veredito.py: JSON sem o campo esperado ({erro!r}); é saída do metricas.py/sincronizar.py?",
              file=sys.stderr)
        return 2
    sys.stdout.write(texto)
    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(res, f, ensure_ascii=False, indent=2, sort_keys=True)
            f.write("\n")
    return _codigo(res)


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Passo 4: ver passar**

Rodar: `python3 etapa0/ferramentas/testar.py test_veredito.py`
Esperado: `testar.py: 51 testes, 0 falhas, 0 erros, 0 pulados`.

- [ ] **Passo 5: commit**

```bash
git add etapa0/ferramentas/veredito.py etapa0/ferramentas/testes/test_veredito.py
git commit -m "feat(etapa0): veredito.py, critérios de saída por gravação e rodada (spec 0005 D9)"
```

### Tarefa 4 (motor-voz): `nivel.py` (spec F4, D12; A6)

**Arquivos:**
- Criar: `etapa0/ferramentas/testes/test_nivel.py`
- Criar: `etapa0/ferramentas/nivel.py`

- [ ] **Passo 1: escrever o teste**

`etapa0/ferramentas/testes/test_nivel.py`:

```python
"""nivel.py com gravações sintéticas (spec 0005, D12, F4; impl_plan 0005, A6).

Base: level.csv a cada 200 ms (leitura 0,05; silêncio do app em [10.000, 40.000) com 0,002 e um pico de 0,004
em 20.000); relatório do metricas.py com deslocamento D (os t_ms do relatório estão no relógio do WAV).
"""

import hashlib
import json
import unittest
from pathlib import Path

import nivel
from testes import sintetico as s
from testes.apoio import Pasta, rodar

D = 1370


def _sha(caminho):
    return hashlib.sha256(Path(caminho).read_bytes()).hexdigest()


def _level(extras=None, fim=60000):
    linhas = ["t_ms,rms"]
    for t in range(200, fim, 200):
        rms = 0.002 if 10000 <= t < 40000 else 0.05
        rms = (extras or {}).get(t, rms)
        linhas.append(f"{t},{rms:.6f}")
    return "\n".join(linhas) + "\n"


def _hipoteses(linhas):
    """linhas: (t, kind, palavras, aceita)."""
    return "t_ms,session,kind,words,accepted\n" + "".join(
        f'{t},1,{k},"{p}",{1 if a else 0}\n' for t, k, p, a in linhas)


class Gravacoes:
    def __init__(self, raiz):
        self.pastas = {k: Path(raiz) / k for k in ("relatorios", "trajetorias", "eventos", "esperado")}
        for p in self.pastas.values():
            p.mkdir(parents=True, exist_ok=True)

    def gravacao(self, nome, level, hipoteses, eventos, movimentos=()):
        tp = self.pastas["trajetorias"]
        (tp / f"{nome}.level.csv").write_text(level, encoding="utf-8")
        hip = tp / f"{nome}.hypotheses.csv"
        hip.write_text(_hipoteses(hipoteses), encoding="utf-8")
        traj = tp / f"{nome}.trajectory.csv"
        traj.write_text(f"trajetoria {nome}\n", encoding="utf-8")
        esp = self.pastas["esperado"] / f"{nome}.expected.csv"
        esp.write_text(f"esperado {nome}\n", encoding="utf-8")
        ev = self.pastas["eventos"] / f"{nome}.events.csv"
        ev.write_text(s.eventos_csv(eventos), encoding="utf-8")
        entradas = [{"papel": p, "arquivo": c.name, "sha256": _sha(c)}
                    for p, c in (("trajetoria", traj), ("esperado", esp), ("eventos", ev), ("hipoteses", hip))]
        relatorio = {"deslocamento_ms": D, "entradas": entradas,
                     "criterios": {"salto_errado": {"por_movimento": [
                         {"t_ms": t + D, "cursor": 5, "move": "Short", "intervalo": k, "esperado_no_inicio": 4,
                          "classe": "alcance"} for t, k in movimentos]}}}
        (self.pastas["relatorios"] / f"{nome}.json").write_text(json.dumps(relatorio), encoding="utf-8")
        return self

    def rodar(self, *nomes):
        saida = self.pastas["relatorios"] / "nivel.json"
        argv = [*nomes, "--relatorios", str(self.pastas["relatorios"]), "--trajetorias", str(self.pastas["trajetorias"]),
                "--eventos", str(self.pastas["eventos"]), "--esperado", str(self.pastas["esperado"]), "--json", str(saida)]
        codigo, out, err = rodar(nivel.main, argv)
        dados = json.loads(saida.read_text(encoding="utf-8")) if saida.exists() else None
        return codigo, dados, out, err


SILENCIO = [(10000, "silencio_inicio", ""), (40000, "silencio_fim", "")]
PICO = {20000: 0.004}


class NivelTest(unittest.TestCase):
    def test_F4_sem_movimento_recomenda_zero_e_da_os_niveis(self):
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level(PICO), [(5000, "partial", "ouvir", True)],
                                          SILENCIO)
            codigo, dados, out, err = g.rodar("T06_giorgio_sala_t1")
        self.assertEqual(0, codigo, err)
        r = dados["recomendacao"]
        self.assertEqual((0.0, None), (r["recomendacao"], r["candidato"]))
        n = dados["gravacoes"][0]["niveis"]
        self.assertEqual({"n": 150, "mediana": 0.002, "p95": 0.002, "maximo": 0.004}, n["silencio"])
        self.assertEqual(299 - 150, n["leitura"]["n"])
        self.assertEqual(0, n["pausa"]["n"])
        self.assertIn("| T06_giorgio_sala_t1 | silencio | 150 |", out)

    def test_F4_intervalo_fechado_no_inicio_e_aberto_no_fim(self):
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level({10000: 0.9, 40000: 0.8}), [], SILENCIO)
            _, dados, _, _ = g.rodar("T06_giorgio_sala_t1")
        self.assertEqual(0.9, dados["gravacoes"][0]["niveis"]["silencio"]["maximo"])
        self.assertEqual(0.8, dados["gravacoes"][0]["niveis"]["leitura"]["maximo"])

    def test_D12_movimento_por_parcial_no_silencio_da_candidato(self):
        hip = [(5000, "partial", "ouvir", True), (20100, "partial", "isso", True)]
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level({**PICO, 30000: 0.006, 50000: 0.006}), hip,
                                          SILENCIO, movimentos=[(20300, "silencio")])
            codigo, dados, out, _ = g.rodar("T06_giorgio_sala_t1")
        self.assertEqual(1, codigo)
        mv = dados["gravacoes"][0]["movimentos"][0]
        self.assertEqual((20300, 20100, "partial", 0.004), (mv["t_app_ms"], mv["causa_t_ms"], mv["causa_kind"], mv["causa_nivel"]))
        r = dados["recomendacao"]
        self.assertEqual((None, 0.006, 0.004), (r["recomendacao"], r["candidato"], r["limite"]))
        self.assertEqual(0, dados["gravacoes"][0]["leitura_abaixo_do_candidato"])
        self.assertIn("verificar por reprodução", out)

    def test_D12_causa_e_a_ultima_aceita_e_o_nivel_e_o_da_linha_anterior(self):
        hip = [(19000, "partial", "a", True), (20000, "partial", "b", True), (20050, "partial", "c", False),
               (20060, "error", "Internal", True), (20300, "partial", "d", True)]
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level({**PICO, 19800: 0.003}), hip, SILENCIO,
                                          movimentos=[(20300, "silencio")])
            _, dados, _, _ = g.rodar("T06_giorgio_sala_t1")
        mv = dados["gravacoes"][0]["movimentos"][0]
        self.assertEqual((20300, 0.002), (mv["causa_t_ms"], mv["causa_nivel"]))
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level({**PICO, 19800: 0.003}), hip[:4], SILENCIO,
                                          movimentos=[(20100, "silencio")])
            _, dados, _, _ = g.rodar("T06_giorgio_sala_t1")
        mv = dados["gravacoes"][0]["movimentos"][0]
        self.assertEqual((20000, 0.004), (mv["causa_t_ms"], mv["causa_nivel"]))

    def test_D12_movimento_por_final_nao_da_candidato(self):
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level(PICO), [(20100, "final", "isso", True)],
                                          SILENCIO, movimentos=[(20300, "silencio")])
            codigo, dados, _, _ = g.rodar("T06_giorgio_sala_t1")
        self.assertEqual(1, codigo)
        self.assertIsNone(dados["recomendacao"]["candidato"])
        self.assertIn("Final", dados["recomendacao"]["motivo"])

    def test_D12_movimento_no_improviso_nao_pede_limiar(self):
        eventos = [(10000, "improviso_inicio", ""), (40000, "improviso_fim", "")]
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T04_giorgio_sala_t1", _level(PICO), [(20100, "partial", "x", True)],
                                          eventos, movimentos=[(20300, "improviso")])
            codigo, dados, _, _ = g.rodar("T04_giorgio_sala_t1")
        self.assertEqual(0, codigo)
        self.assertEqual(0.0, dados["recomendacao"]["recomendacao"])
        self.assertEqual(1, len(dados["gravacoes"][0]["movimentos"]))

    def test_D12_candidato_olha_todas_as_gravacoes_e_o_maior_nivel_causador(self):
        pausa = [(10000, "pausa_toque", ""), (40000, "retoma_toque", "")]
        with Pasta() as p:
            g = Gravacoes(p.dir)
            g.gravacao("T06_giorgio_sala_t1", _level(PICO), [(20100, "partial", "x", True)], SILENCIO,
                       movimentos=[(20300, "silencio")])
            g.gravacao("T07_giorgio_sala_t1", _level({20000: 0.01, 30000: 0.012}), [(20100, "partial", "y", True)],
                       pausa, movimentos=[(20300, "pausa")])
            g.gravacao("T01_giorgio_sala_t1", _level({30000: 0.011}), [], [])
            codigo, dados, _, _ = g.rodar("T06_giorgio_sala_t1", "T07_giorgio_sala_t1", "T01_giorgio_sala_t1")
        self.assertEqual(1, codigo)
        self.assertEqual((0.01, 0.011), (dados["recomendacao"]["limite"], dados["recomendacao"]["candidato"]))

    def test_D12_nenhum_nivel_acima_do_causador(self):
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level({20000: 0.9}), [(20100, "partial", "x", True)],
                                          SILENCIO, movimentos=[(20300, "silencio")])
            _, dados, _, _ = g.rodar("T06_giorgio_sala_t1")
        self.assertIsNone(dados["recomendacao"]["candidato"])
        self.assertIn("nenhum nível", dados["recomendacao"]["motivo"])

    def test_entradas_invalidas_saem_com_2(self):
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", _level(), [], SILENCIO)
            codigo, dados, out, err = g.rodar("T06_giorgio_sala_t1", "T07_giorgio_sala_t1")
            self.assertEqual((2, None, ""), (codigo, dados, out))
            self.assertIn("falta T07_giorgio_sala_t1.json", err)
            (g.pastas["eventos"] / "T06_giorgio_sala_t1.events.csv").write_text("t_ms,evento,detalhe\n", encoding="utf-8")
            codigo, _, _, err = g.rodar("T06_giorgio_sala_t1")
            self.assertEqual(2, codigo)
            self.assertIn("mudou ou sumiu", err)
        with Pasta() as p:
            g = Gravacoes(p.dir).gravacao("T06_giorgio_sala_t1", "t_ms,rms\n200,2\n", [], SILENCIO)
            codigo, _, _, err = g.rodar("T06_giorgio_sala_t1")
            self.assertEqual(2, codigo)
            self.assertIn("level.csv", err)

    def test_estatisticas_p95_por_posto_mais_proximo(self):
        self.assertEqual({"n": 20, "mediana": 10.5, "p95": 19, "maximo": 20}, nivel.estatisticas(list(range(20, 0, -1))))
        self.assertEqual(20, nivel.estatisticas(list(range(1, 22)))["p95"])
        self.assertEqual({"n": 0, "mediana": None, "p95": None, "maximo": None}, nivel.estatisticas([]))

    def test_ajuda(self):
        codigo, out, _ = rodar(nivel.main, ["--help"])
        self.assertEqual(0, codigo)
        for termo in ("nomes", "--relatorios", "--trajetorias", "--eventos", "--json"):
            self.assertIn(termo, out)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Passo 2: ver falhar**

Rodar: `python3 etapa0/ferramentas/testar.py test_nivel.py`
Esperado: `ModuleNotFoundError: No module named 'nivel'`; `testar.py: 1 testes, 0 falhas, 1 erros, 0 pulados`.

- [ ] **Passo 3: escrever a ferramenta**

`etapa0/ferramentas/nivel.py`:

```python
"""nivel.py <nome>... [--relatorios DIR] [--trajetorias DIR] [--eventos DIR] [--json SAIDA]: limiar de nível.

spec 0005, D12 (limiar) e critério F4; impl_plan 0005, A6.

Para cada gravação (relatório <nome>.json do metricas.py com limiar 0, <nome>.level.csv, <nome>.hypotheses.csv e
os eventos que o relatório usou): o nível (rms do level.csv) dentro do improviso, do silêncio e da pausa e na
leitura (o resto), com n, mediana, p95 e máximo; e cada movimento automático dentro desses intervalos, com a
hipótese aceita que o causou (a última com t <= t do movimento, no relógio do app) e o nível em vigor nela
(o da última linha do level.csv com t <= t da hipótese: é o `lastLevel` que o ReaderController compara).

Regra do D12: sem movimento em silêncio ou pausa, a recomendação é 0. Com movimento, o candidato é o menor nível
do level.csv (de todas as gravações) acima do maior nível das hipóteses parciais que causaram esses movimentos.
O limiar não atua em Final (ReaderController: `isFinal || lastLevel >= threshold`); movimento causado por Final
fica listado e não entra no candidato. O candidato ainda tem de ser verificado por reprodução (F4).
Códigos de saída: 0 sem movimento em silêncio ou pausa; 1 há movimento (candidato a verificar ou nenhum);
2 entrada inválida.
"""

import argparse
import json
import math
import statistics
import sys
from pathlib import Path

import metricas
import roteiro
import sincronizar
import veredito

INTERVALOS = ("improviso", "silencio", "pausa")
CORRIGIVEIS = ("silencio", "pausa")  # D12: o limiar é para silêncio e pausa


class ErroEntrada(Exception):
    """Código de saída 2."""


def estatisticas(valores):
    """n, mediana, p95 (posto mais próximo, como o metricas.py) e máximo."""
    if not valores:
        return {"n": 0, "mediana": None, "p95": None, "maximo": None}
    ordenados = sorted(valores)
    return {"n": len(ordenados), "mediana": statistics.median(ordenados),
            "p95": ordenados[math.ceil(0.95 * len(ordenados)) - 1], "maximo": ordenados[-1]}


def _ultimo_ate(itens, t, chave):
    melhor = None
    for item in itens:
        if chave(item) <= t:
            melhor = item
        else:
            break
    return melhor


def analisar(nome, relatorio, level, hipoteses, eventos):
    """relatorio: JSON do metricas.py; level: [(t, rms)]; hipoteses: [metricas.Hipotese] (relógio do app)."""
    d = relatorio["deslocamento_ms"]
    faixas = metricas.intervalos(eventos)
    stats = {}
    for k in INTERVALOS:
        stats[k] = estatisticas([rms for t, rms in level if any(a <= t < b for a, b, _ in faixas[k])])
    fora = [(t, rms) for t, rms in level if not any(a <= t < b for k in INTERVALOS for a, b, _ in faixas[k])]
    stats["leitura"] = estatisticas([rms for _, rms in fora])
    aceitas = sorted((h for h in hipoteses if h.aceita and h.kind in ("partial", "final")), key=lambda h: h.t)
    movimentos = []
    for mv in relatorio["criterios"]["salto_errado"]["por_movimento"]:
        t_app = mv["t_ms"] - d
        causa = _ultimo_ate(aceitas, t_app, lambda h: h.t)
        linha = _ultimo_ate(level, causa.t, lambda linha: linha[0]) if causa else None
        movimentos.append({"t_app_ms": t_app, "intervalo": mv["intervalo"], "move": mv["move"], "classe": mv["classe"],
                           "causa_t_ms": causa.t if causa else None, "causa_kind": causa.kind if causa else None,
                           "causa_nivel": linha[1] if linha else None})
    return {"nome": nome, "deslocamento_ms": d, "niveis": stats, "movimentos": movimentos,
            "leitura_rms": [rms for _, rms in fora]}


def recomendar(gravacoes, todos_os_niveis):
    corrigiveis = [mv for g in gravacoes for mv in g["movimentos"] if mv["intervalo"] in CORRIGIVEIS]
    if not corrigiveis:
        return {"recomendacao": 0.0, "candidato": None, "limite": None,
                "motivo": "nenhum movimento automático em silêncio ou pausa com limiar 0 (D12)"}
    por_parcial = [mv["causa_nivel"] for mv in corrigiveis if mv["causa_kind"] == "partial" and mv["causa_nivel"] is not None]
    if not por_parcial:
        return {"recomendacao": None, "candidato": None, "limite": None,
                "motivo": "movimentos causados por Final ou sem nível conhecido: o limiar não atua neles"}
    limite = max(por_parcial)
    acima = [rms for rms in todos_os_niveis if rms > limite]
    if not acima:
        return {"recomendacao": None, "candidato": None, "limite": limite,
                "motivo": "nenhum nível do level.csv acima do nível das parciais que causaram os movimentos"}
    candidato = min(acima)
    sem_efeito = len(corrigiveis) - len(por_parcial)
    motivo = "menor nível do level.csv acima do maior nível das parciais que causaram movimento (D12); verificar por reprodução"
    if sem_efeito:
        motivo += f"; {sem_efeito} movimento(s) por Final ou sem nível não são resolvidos pelo limiar"
    return {"recomendacao": None, "candidato": candidato, "limite": limite, "motivo": motivo}


def carregar(nome, pastas):
    rel = pastas["relatorios"] / f"{nome}.json"
    if not rel.is_file():
        raise ErroEntrada(f"{nome}: falta {rel.name}")
    try:
        relatorio = json.loads(rel.read_text(encoding="utf-8"))
    except json.JSONDecodeError as erro:
        raise ErroEntrada(f"{rel.name}: JSON inválido ({erro})") from None
    try:
        caminhos = veredito.conferir_entradas(relatorio["entradas"], pastas, nome)
    except veredito.ErroEntrada as erro:
        raise ErroEntrada(str(erro)) from None
    try:
        conteudos = [c.read_text(encoding="utf-8") for c in caminhos.get("eventos", [])]
        eventos = metricas.ler_eventos(conteudos, 0, 10**9)
        level = sincronizar.ler_level((pastas["trajetoria"] / f"{nome}.level.csv").read_text(encoding="utf-8"))
        hipoteses = metricas.ler_hipoteses((pastas["trajetoria"] / f"{nome}.hypotheses.csv").read_text(encoding="utf-8"), 0)
    except (metricas.ErroEntrada, sincronizar.ErroEntrada) as erro:
        raise ErroEntrada(f"{nome}: {erro}") from None
    return relatorio, level, hipoteses, eventos


def relatorio_md(res):
    linhas = ["# Limiar de nível (F4)", "", "| Gravação | Intervalo | n | mediana | p95 | máximo |", "|---|---|---|---|---|---|"]
    for g in res["gravacoes"]:
        for k in INTERVALOS + ("leitura",):
            e = g["niveis"][k]
            linhas.append(f"| {g['nome']} | {k} | {e['n']} | {e['mediana']} | {e['p95']} | {e['maximo']} |")
    linhas += ["", "## Movimentos automáticos dentro dos intervalos (limiar 0)", ""]
    movs = [(g["nome"], mv) for g in res["gravacoes"] for mv in g["movimentos"]]
    if not movs:
        linhas.append("- nenhum")
    for nome, mv in movs:
        linhas.append(f"- {nome}: {mv['move']} em {mv['t_app_ms']} ms do app, {mv['intervalo']} ({mv['classe']}); "
                      f"causa {mv['causa_kind']} em {mv['causa_t_ms']} ms, nível {mv['causa_nivel']}")
    r = res["recomendacao"]
    linhas += ["", "## Recomendação", "", f"- recomendação: {r['recomendacao']}", f"- candidato: {r['candidato']}",
               f"- maior nível das parciais causadoras: {r['limite']}", f"- {r['motivo']}"]
    for g in res["gravacoes"]:
        if g.get("leitura_abaixo_do_candidato") is not None:
            linhas.append(f"- {g['nome']}: {g['leitura_abaixo_do_candidato']} de {g['niveis']['leitura']['n']} "
                          "linhas de leitura abaixo do candidato")
    return "\n".join(linhas) + "\n"


def main(argv=None):
    parser = argparse.ArgumentParser(prog="nivel.py", description="Nível de áudio nos intervalos e candidato a limiar (D12).")
    parser.add_argument("nomes", nargs="+", help="gravações (<ID>_<voz>_<ambiente>_t<n>), com relatório de limiar 0")
    parser.add_argument("--relatorios", required=True, help="pasta dos <nome>.json do metricas.py")
    parser.add_argument("--trajetorias", default=str(roteiro.ETAPA0 / "trajetorias"))
    parser.add_argument("--eventos", default=str(roteiro.ETAPA0 / "eventos"))
    parser.add_argument("--esperado", default=str(roteiro.ETAPA0 / "esperado"))
    parser.add_argument("--json", help="grava o resultado em JSON")
    args = parser.parse_args(argv)
    pastas = {"relatorios": Path(args.relatorios), "trajetoria": Path(args.trajetorias),
              "hipoteses": Path(args.trajetorias), "eventos": Path(args.eventos), "esperado": Path(args.esperado)}
    try:
        gravacoes, todos = [], []
        for nome in args.nomes:
            relatorio, level, hipoteses, eventos = carregar(nome, pastas)
            gravacoes.append(analisar(nome, relatorio, level, hipoteses, eventos))
            todos += [rms for _, rms in level]
        recomendacao = recomendar(gravacoes, todos)
    except (OSError, UnicodeDecodeError, KeyError, TypeError, ErroEntrada) as erro:
        print(f"nivel.py: {erro!r}" if isinstance(erro, (KeyError, TypeError)) else f"nivel.py: {erro}", file=sys.stderr)
        return 2
    for g in gravacoes:
        leitura = g.pop("leitura_rms")
        c = recomendacao["candidato"]
        g["leitura_abaixo_do_candidato"] = None if c is None else sum(1 for rms in leitura if rms < c)
    res = {"gravacoes": gravacoes, "recomendacao": recomendacao}
    sys.stdout.write(relatorio_md(res))
    if args.json:
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(res, f, ensure_ascii=False, indent=2, sort_keys=True)
            f.write("\n")
    tem_movimento = any(mv["intervalo"] in CORRIGIVEIS for g in gravacoes for mv in g["movimentos"])
    return 1 if tem_movimento else 0


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Passo 4: ver passar**

Rodar: `python3 etapa0/ferramentas/testar.py test_nivel.py`
Esperado: `testar.py: 11 testes, 0 falhas, 0 erros, 0 pulados`.

- [ ] **Passo 5: commit**

```bash
git add etapa0/ferramentas/nivel.py etapa0/ferramentas/testes/test_nivel.py
git commit -m "feat(etapa0): nivel.py, candidato a limiar de nível (spec 0005 F4, D12)"
```

### Tarefa 5 (motor-voz): linha de comando e cadeia (spec E1; A2)

**Arquivos:**
- Criar: `etapa0/ferramentas/testes/test_cli_0005.py`

- [ ] **Passo 1: escrever o teste**

`etapa0/ferramentas/testes/test_cli_0005.py`:

```python
"""Linha de comando de verdade das ferramentas da 0005 e a cadeia sincronizar -> folha -> metricas -> veredito
(spec 0005, D3 a D6, E1; impl_plan 0005, Tarefa 6)."""

import csv
import io
import json
import subprocess
import sys
import unittest

import roteiro
import veredito
from testes import sinal
from testes import sintetico as s
from testes.test_veredito import Rodada

FERRAMENTAS = roteiro.FERRAMENTAS


def _cli(ferramenta, *argv):
    return subprocess.run([sys.executable, str(FERRAMENTAS / ferramenta), *argv],
                          capture_output=True, text=True, encoding="utf-8", timeout=120, check=False)


class AjudaTest(unittest.TestCase):
    def test_ajuda_e_codigo_2_pelo_processo(self):
        esperado = {
            "sincronizar.py": ["wav", "level", "--faixa-ms", "--palma-ate-ms", "--json"],
            "folha.py": ["texto", "esperado", "transcricao", "--deslocamento-ms", "-o"],
            "veredito.py": ["rodada", "final"],
            "nivel.py": ["nomes", "--relatorios", "--json"],
        }
        for ferramenta, termos in esperado.items():
            with self.subTest(ferramenta=ferramenta):
                r = _cli(ferramenta, "--help")
                self.assertEqual(0, r.returncode)
                for termo in termos:
                    self.assertIn(termo, r.stdout)
        with s.Pasta() as p:
            x = p.arquivo("x.csv", "x\n")
            for ferramenta, argv in (("sincronizar.py", [x, x]), ("folha.py", [x, x, x, "--deslocamento-ms", "0"]),
                                     ("veredito.py", ["final", x]), ("nivel.py", ["T01_giorgio_sala_t1", "--relatorios", p.arquivo("d", "")])):
                with self.subTest(ferramenta=ferramenta):
                    r = _cli(ferramenta, *argv)
                    self.assertEqual((2, ""), (r.returncode, r.stdout))


class CadeiaTest(unittest.TestCase):
    def test_E1_sincronizar_folha_metricas_com_o_mesmo_deslocamento(self):
        d_real = 1370
        amostras = sinal.fala_sintetica(40000, 3, palma_ms=d_real + 2000, silencio_ate_ms=d_real + 4000)
        n = len(s.tokens("T06"))
        # esperado no relógio do WAV: leitura começa em d + 4000; silêncio de 10 s depois do token 134
        tempos = []
        for i in range(n):
            extra = 10000 if i > 134 else 0
            ini = d_real + 4000 + 100 * i + extra
            tempos.append((ini, ini + 80))
        with s.Pasta() as p:
            wav = sinal.escrever_wav(p.dir / "T06_giorgio_sala_t1.wav", amostras)
            level = p.arquivo("T06_giorgio_sala_t1.level.csv", sinal.level_csv(amostras, d_real, 38000, ganho=0.3))
            r = _cli("sincronizar.py", wav, level, "--json", str(p.dir / "s.json"))
            self.assertEqual(0, r.returncode, r.stderr)
            d = json.loads((p.dir / "s.json").read_text(encoding="utf-8"))["deslocamento_ms"]
            self.assertLessEqual(abs(d - d_real), 10)

            esperado = p.arquivo("T06_giorgio_sala_t1.expected.csv", s.esperado_csv("T06", tempos))
            palavras = io.StringIO()
            w = csv.writer(palavras, lineterminator="\n")
            w.writerow(["palavra", "t_inicio_ms", "t_fim_ms", "conf"])
            for t, (ini, fim) in zip(s.tokens("T06"), tempos):
                w.writerow([t.formas[0][0], ini, fim, "1.0"])
            words = p.arquivo("T06_giorgio_sala_t1.words.csv", palavras.getvalue())
            texto = str(roteiro.TEXTOS / "T06.txt")
            folha = p.dir / "T06_giorgio_sala_t1.folha.events.csv"
            r = _cli("folha.py", texto, esperado, words, "--deslocamento-ms", str(d), "-o", str(folha))
            self.assertEqual(0, r.returncode, r.stderr)
            linhas = list(csv.reader(io.StringIO(folha.read_text(encoding="utf-8"))))
            self.assertEqual(tempos[134][1] - d, int(linhas[1][0]))

            # trajetória no relógio do app: o cursor chega a cada token 10 ms depois do início dele
            traj = [(0, 0, "Reposition")]
            for i in range(1, n):
                traj.append((tempos[i][0] - d + 10, i, "Short"))
            trajetoria = p.arquivo("T06_giorgio_sala_t1.trajectory.csv", s.trajetoria_csv(traj))
            app = p.arquivo("T06_giorgio_sala_t1.events.csv", s.eventos_csv([]))
            for sufixo, desl in (("", d), (".m200", d - 200), (".p200", d + 200)):
                r = _cli("metricas.py", trajetoria, esperado, app, str(folha), "--deslocamento-ms", str(desl),
                         "--json", str(p.dir / f"T06_giorgio_sala_t1{sufixo}.json"))
                self.assertIn(r.returncode, (0, 1), r.stderr)
            dados = json.loads((p.dir / "T06_giorgio_sala_t1.json").read_text(encoding="utf-8"))
            self.assertEqual("PASSA", dados["criterios"]["salto_errado"]["veredito"])
            self.assertGreater(dados["amostras"]["excluidas_intervalo"], 90, "o silêncio da folha foi aplicado")


class VereditoPeloProcessoTest(unittest.TestCase):
    def test_H1_rodada_e_final_pelo_processo(self):
        with s.Pasta() as p:
            r = Rodada(p.dir / "S1").completa()
            argv = ["rodada", "--rodada", "S1", "--oficiais", str(r.tsv()), "--relatorios", str(r.pastas["relatorios"]),
                    "--trajetorias", str(r.pastas["trajetorias"]), "--eventos", str(r.pastas["eventos"]),
                    "--esperado", str(r.pastas["esperado"]), "--gravacoes", str(r.pastas["gravacoes"]),
                    "--json", str(p.dir / "S1.json")]
            res = _cli("veredito.py", *argv)
            self.assertEqual(0, res.returncode, res.stderr)
            res = _cli("veredito.py", "final", str(p.dir / "S1.json"))
            self.assertEqual(1, res.returncode)
            self.assertIn(f"| CS1 | {veredito.NAO_MEDIDO} |", res.stdout)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Passo 2: rodar**

Rodar: `python3 etapa0/ferramentas/testar.py test_cli_0005.py`
Esperado: `testar.py: 3 testes, 0 falhas, 0 erros, 0 pulados`. Este teste junta as quatro ferramentas já escritas, então não há "ver falhar" separado. A falha dele contra uma quebra fica provada na Tarefa 6 (quebra S14 ou F08 derruba também a cadeia).

- [ ] **Passo 3: commit**

```bash
git add etapa0/ferramentas/testes/test_cli_0005.py
git commit -m "test(etapa0): cadeia sincronizar, folha, metricas e veredito pela linha de comando (spec 0005 E1)"
```

### Tarefa 6 (motor-voz): verificação completa e evidências

- [ ] **Passo 1: suíte inteira**

Rodar: `python3 etapa0/ferramentas/testar.py 2>&1 | tail -1 | tee work/0005-etapa0-medicao/evidencias/b-testar.txt`
Esperado: `testar.py: 270 testes, 0 falhas, 0 erros, 0 pulados`.

- [ ] **Passo 2: só stdlib, sem rede e sem escrita de áudio**

```bash
grep -n -E "^(import|from) " etapa0/ferramentas/{sincronizar,folha,veredito,nivel}.py | tee work/0005-etapa0-medicao/evidencias/b-imports.txt
grep -n -E "urllib|socket|http|requests|subprocess" etapa0/ferramentas/{sincronizar,folha,veredito,nivel}.py ; echo "rc=$?"
grep -n -E "open\(.*[\"']w|wave\.open\(.*[\"']wb" etapa0/ferramentas/{sincronizar,folha,veredito,nivel}.py
```
Esperado: imports só de `argparse csv hashlib io json math re statistics sys wave array dataclasses pathlib` e dos módulos `roteiro metricas alinhar sincronizar veredito`; o segundo `grep` não acha nada (`rc=1`); o terceiro só acha as escritas de `--json`/`-o` (texto), nunca um WAV.

- [ ] **Passo 3: quebras Q2 (pelo menos as ★, uma a uma)**

Para cada quebra ★ da tabela: trocar o trecho "de" pelo "para" no arquivo, rodar `find etapa0/ferramentas -name __pycache__ -prune -exec rm -r {} + ; PYTHONDONTWRITEBYTECODE=1 python3 -B etapa0/ferramentas/testar.py 2>&1 | tail -1`, conferir que há falha ou erro, desfazer com `git checkout -- <arquivo>` e conferir `git diff --stat` vazio. Registrar id, saída e restauração em `work/0005-etapa0-medicao/evidencias/b-quebras-q2.txt`.

- [ ] **Passo 4: registro e commit**

Preencher o "Registro da construção" no fim deste arquivo (no worktree), e então:

```bash
git add work/0005-etapa0-medicao/evidencias/b-*.txt work/0005-etapa0-medicao/impl_plan.md
git commit -m "docs(0005): evidências e registro da construção das ferramentas"
```

Avisar o orquestrador: pronto para o REVIEW. O merge em `work/0005-etapa0-medicao` só acontece com o `code_review.md` APROVADO.

---

## Tarefa Q (qa-publicacao): scripts de condução do aparelho

Criar em `work/0005-etapa0-medicao/scripts/` os arquivos abaixo, exatamente, e depois:

```bash
chmod +x work/0005-etapa0-medicao/scripts/*.sh work/0005-etapa0-medicao/scripts/*.py
python3 work/0005-etapa0-medicao/scripts/ui.py autoteste                 # ui.py autoteste: ok
python3 work/0005-etapa0-medicao/scripts/reproduzir.py X --preset normal --vocab off --estrategia novo --autoteste   # reproduzir.py autoteste: ok
for f in work/0005-etapa0-medicao/scripts/*.sh; do bash -n "$f" && echo "ok $f"; done
git add work/0005-etapa0-medicao/scripts
git commit -m "test(0005): scripts de condução do aparelho e das medições (spec 0005 D25)"
```

`comum.sh`:

```bash
# comum.sh: incluído pelos scripts da 0005 (impl_plan 0005, A8). Bash, não zsh: no zsh "$VAR com espaço" não
# se divide em palavras e "a()" com adb quebra (visto no PLAN).
set -euo pipefail
RAIZ="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"
SCRIPTS="$RAIZ/work/0005-etapa0-medicao/scripts"
EV="$RAIZ/work/0005-etapa0-medicao/evidencias"
E0="$RAIZ/etapa0"
FERR="$E0/ferramentas"
PKG="io.github.gigioderico.tribuna.etapa0"
EXPORTS="/sdcard/Android/data/$PKG/files/exports"
WAVDIR="/sdcard/Android/data/$PKG/files/wav"
export ANDROID_SERIAL="${ANDROID_SERIAL:-RQCW80BPXKN}"
TMPD="${TMPDIR:-/tmp}/tribuna-0005"
VENV="${TRIBUNA_VENV:-$HOME/.venvs/tribuna-etapa0}"
MODELO="${TRIBUNA_MODELO:-$HOME/vosk-modelos/vosk-model-pt-fb-v0.1.1-20220516_2113}"
mkdir -p "$EV" "$TMPD"

a() { adb shell "$@" | tr -d '\r'; }
ui() { python3 "$SCRIPTS/ui.py" "$@"; }
exige_aparelho() {
  if [ "$(adb get-state 2>/dev/null)" != "device" ]; then
    echo "PARAR: aparelho $ANDROID_SERIAL não está no adb (cabo, depuração USB ou autorização). Pedir ao humano." >&2
    exit 3
  fi
}
log() { echo "$(date '+%Y-%m-%dT%H:%M:%S') $*" | tee -a "$EV/diario.txt"; }
```

`aparelho.sh`:

```bash
#!/usr/bin/env bash
# aparelho.sh salvar|aplicar [bateria]|restaurar|ler: ajustes do aparelho com valor de antes e de depois
# (spec A4, D18; impl_plan 0005, A9). Nunca sobrescreve o "antes": se ele já existe, salvar sai com erro.
source "$(dirname "$0")/comum.sh"
ESTADO="$EV/aparelho-estado-antes.env"
AJ="$EV/aparelho-ajustes.txt"

ler() {
  echo "airplane_mode_on=$(a settings get global airplane_mode_on)"
  echo "zen_mode=$(a settings get global zen_mode)"
  echo "screen_brightness=$(a settings get system screen_brightness)"
  echo "screen_brightness_mode=$(a settings get system screen_brightness_mode)"
  echo "screen_off_timeout=$(a settings get system screen_off_timeout)"
  echo "stay_on_while_plugged_in=$(a settings get global stay_on_while_plugged_in)"
  echo "accelerometer_rotation=$(a settings get system accelerometer_rotation)"
  echo "user_rotation=$(a settings get system user_rotation)"
}

exige_aparelho
case "${1:-}" in
  ler) ler ;;
  salvar)
    if [ -f "$ESTADO" ]; then echo "já existe $ESTADO: restaure antes de salvar de novo" >&2; exit 1; fi
    ler > "$ESTADO"
    { echo "== antes $(date '+%Y-%m-%dT%H:%M:%S')"; cat "$ESTADO"; } >> "$AJ" ;;
  aplicar)
    [ -f "$ESTADO" ] || { echo "rode 'aparelho.sh salvar' antes" >&2; exit 1; }
    a cmd connectivity airplane-mode enable
    a cmd notification set_dnd on
    a settings put system screen_brightness_mode 0
    a settings put system screen_brightness 255
    a svc power stayon usb
    if [ "${2:-}" = "bateria" ]; then a settings put system screen_off_timeout 3600000; fi
    { echo "== aplicado ${2:-sessao} $(date '+%Y-%m-%dT%H:%M:%S')"; ler; } >> "$AJ"
    falta=""
    [ "$(a settings get global airplane_mode_on)" = "1" ] || falta="$falta modo-aviao"
    [ "$(a settings get global zen_mode)" != "0" ] || falta="$falta nao-perturbe"
    [ "$(a settings get system screen_brightness_mode)" = "0" ] || falta="$falta brilho-fixo"
    if [ "${2:-}" = "bateria" ]; then
      [ "$(a settings get system screen_off_timeout)" = "3600000" ] || falta="$falta tempo-de-tela"
    fi
    if [ -n "$falta" ]; then echo "PARAR: o adb não conseguiu:$falta. Pedir ao humano que ajuste na tela." >&2; exit 1; fi
    echo "ajustes aplicados" ;;
  restaurar)
    [ -f "$ESTADO" ] || { echo "nada a restaurar" >&2; exit 1; }
    # shellcheck disable=SC1090
    source "$ESTADO"
    if [ "$airplane_mode_on" = "0" ]; then a cmd connectivity airplane-mode disable; fi
    case "$zen_mode" in 0) a cmd notification set_dnd off ;; 1) a cmd notification set_dnd priority ;;
      2) a cmd notification set_dnd none ;; 3) a cmd notification set_dnd alarms ;; esac
    a settings put system screen_brightness "$screen_brightness"
    a settings put system screen_brightness_mode "$screen_brightness_mode"
    a settings put system screen_off_timeout "$screen_off_timeout"
    a settings put global stay_on_while_plugged_in "$stay_on_while_plugged_in"
    a settings put system accelerometer_rotation "$accelerometer_rotation"
    a settings put system user_rotation "$user_rotation"
    ler > "$TMPD/depois.env"
    { echo "== depois $(date '+%Y-%m-%dT%H:%M:%S')"; cat "$TMPD/depois.env"; } >> "$AJ"
    if diff -u "$ESTADO" "$TMPD/depois.env" >> "$AJ"; then
      mv "$ESTADO" "$EV/aparelho-estado-antes-$(date '+%Y%m%dT%H%M%S').env"
      echo "restaurado: igual ao antes"
    else
      echo "PARAR: o estado depois difere do antes (diff em $AJ). Pedir ao humano." >&2; exit 1
    fi ;;
  *) echo "uso: aparelho.sh salvar|aplicar [bateria]|restaurar|ler" >&2; exit 2 ;;
esac
```

`ui.py`:

```python
#!/usr/bin/env python3
"""ui.py: dirige a tela da etapa0 por adb + uiautomator (impl_plan 0005, A8). Só stdlib; não muda número nenhum.

Uso (ANDROID_SERIAL no ambiente):
  ui.py tocar <texto>            toca no nó com esse texto exato (procura rolando o painel até 6 vezes)
  ui.py chip <ID>                toca no chip do texto de teste (rola a fileira de chips se preciso)
  ui.py campo <atual> <novo>     troca o texto do campo editável que hoje mostra <atual>
  ui.py tem <texto>              sai com 0 se o texto exato está na tela, 1 se não
  ui.py linhas <regex>           imprime os textos da tela que casam com a regex (números do painel)
  ui.py esperar <texto> <seg>    espera o texto aparecer (um dump a cada volta)
  ui.py marcado <texto>          imprime true/false: checked ou selected do nó (ou do pai clicável) com esse texto
  ui.py interruptor <rótulo> on|off   liga ou desliga o Switch da mesma linha do rótulo e confere
  ui.py esperar-habilitado <texto> <seg>   espera o nó com esse texto ficar habilitado (botão)
  ui.py limiar <valor>           põe o slider do limiar em [valor, valor + 0,002] e imprime o valor do rótulo
  ui.py tem-sistema <regex>      sai com 0 se algum nó de outro pacote (diálogo do sistema) casa com a regex
  ui.py tocar-sistema <texto>    toca no nó de outro pacote com esse texto exato (botão do diálogo de permissão)
  ui.py autoteste                testa o leitor de XML com um dump de exemplo (sem aparelho)

Os dumps ficam em $TMPDIR/tribuna-0005/ (fora do repositório) e são apagados no fim de cada chamada: podem ter
texto de notificação do sistema (Q6). Na evidência entram só as linhas pedidas.
Códigos: 0 ok; 1 não achou; 2 uiautomator falhou (por exemplo, "could not get idle state").
"""

import os
import re
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACOTE = "io.github.gigioderico.tribuna.etapa0"
TMP = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "tribuna-0005"
_BOUNDS = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")


class FalhaDump(Exception):
    pass


def adb(*args, capturar=True):
    r = subprocess.run(["adb", *args], capture_output=capturar, text=True, timeout=60)
    return r.stdout if capturar else ""


def nos_sistema(xml):
    """(texto, pacote, bounds) dos nós de outros pacotes: só para o diálogo de permissão (B1)."""
    saida = []
    for n in ET.fromstring(xml).iter("node"):
        m = _BOUNDS.fullmatch(n.get("bounds", ""))
        if n.get("package") != PACOTE and m and (n.get("text") or n.get("content-desc")):
            saida.append((n.get("text") or n.get("content-desc"), n.get("package"), tuple(int(v) for v in m.groups())))
    return saida


def nos(xml):
    """Lista de (texto, classe, (x1, y1, x2, y2), habilitado, marcado, checável) dos nós do pacote da etapa0.
    `marcado` é checked ou selected do próprio nó ou de um ancestral (o FilterChip marca o contêiner do rótulo)."""
    raiz = ET.fromstring(xml)
    saida = []

    def andar(n, marcado_pai):
        marcado = marcado_pai or n.get("checked") == "true" or n.get("selected") == "true"
        m = _BOUNDS.fullmatch(n.get("bounds", ""))
        if n.get("package") == PACOTE and m:
            texto = n.get("text") or n.get("content-desc") or ""
            saida.append((texto, n.get("class", ""), tuple(int(v) for v in m.groups()), n.get("enabled") == "true",
                          marcado, n.get("checkable") == "true"))
        for filho in n.findall("node"):
            andar(filho, marcado)

    for n in raiz.findall("node"):
        andar(n, False)
    return saida


def dump(sistema=False):
    TMP.mkdir(parents=True, exist_ok=True)
    destino = "/sdcard/tribuna-0005-ui.xml"
    saida = adb("shell", "uiautomator", "dump", destino)
    if "dumped to" not in saida:
        raise FalhaDump(saida.strip())
    local = TMP / "ui.xml"
    adb("pull", destino, str(local))
    adb("shell", "rm", destino)
    try:
        conteudo = local.read_text(encoding="utf-8")
        return nos_sistema(conteudo) if sistema else nos(conteudo)
    finally:
        local.unlink(missing_ok=True)


def tem_sistema(regex):
    achados = [(t, pac) for t, pac, _ in dump(sistema=True) if re.search(regex, t) and "systemui" not in pac]
    for t, pac in achados:
        print(f"{pac}\t{t}")
    return 0 if achados else 1


def tocar_sistema(texto):
    achados = [b for t, pac, b in dump(sistema=True) if t == texto and "systemui" not in pac]
    if len(achados) != 1:
        print(f"ui.py: '{texto}' achado {len(achados)} vez(es) fora do app", file=sys.stderr)
        return 1
    x, y = centro(achados[0])
    adb("shell", "input", "tap", str(x), str(y))
    return 0


def centro(b):
    return (b[0] + b[2]) // 2, (b[1] + b[3]) // 2


def achar(lista, texto):
    achados = [n for n in lista if n[0] == texto]
    return achados[0] if len(achados) == 1 else None


def tocar(texto, rolar=6):
    for volta in range(rolar + 1):
        n = achar(dump(), texto)
        if n is not None:
            if not n[3]:
                print(f"ui.py: '{texto}' está desabilitado", file=sys.stderr)
                return 1
            x, y = centro(n[2])
            adb("shell", "input", "tap", str(x), str(y))
            return 0
        if volta < rolar:
            adb("shell", "input", "swipe", "540", "2000", "540", "1400", "300")
            time.sleep(0.5)
    print(f"ui.py: '{texto}' não está na tela (ou aparece mais de uma vez)", file=sys.stderr)
    return 1


def chip(texto_id):
    for _ in range(4):
        lista = dump()
        n = achar(lista, texto_id)
        if n is not None:
            x, y = centro(n[2])
            adb("shell", "input", "tap", str(x), str(y))
            return 0
        chips = [c for c in lista if re.fullmatch(r"T\d\d|L30", c[0])]
        if not chips:
            break
        y = centro(chips[0][2])[1]
        adb("shell", "input", "swipe", "900", str(y), "200", str(y), "300")
        time.sleep(0.5)
    print(f"ui.py: chip {texto_id} não achado", file=sys.stderr)
    return 1


def campo(atual, novo):
    n = next((c for c in dump() if c[0] == atual and c[1].endswith("EditText")), None)
    if n is None:
        print(f"ui.py: campo com '{atual}' não achado", file=sys.stderr)
        return 1
    x, y = centro(n[2])
    adb("shell", "input", "tap", str(x), str(y))
    adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
    adb("shell", "input", "keyevent", *(["KEYCODE_DEL"] * (len(atual) + 2)))
    adb("shell", "input", "text", novo)
    adb("shell", "input", "keyevent", "KEYCODE_BACK")  # fecha o teclado
    time.sleep(0.5)
    if not any(c[0] == novo and c[1].endswith("EditText") for c in dump()):
        print(f"ui.py: o campo não ficou com '{novo}'", file=sys.stderr)
        return 1
    return 0


def linhas(regex):
    padrao = re.compile(regex)
    achadas = [n[0] for n in dump() if padrao.search(n[0])]
    for t in achadas:
        print(t)
    return 0 if achadas else 1


def marcado(texto):
    n = achar(dump(), texto)
    if n is None:
        print(f"ui.py: '{texto}' não achado", file=sys.stderr)
        return 1
    print("true" if n[4] else "false")
    return 0


_ROTULO_LIMIAR = re.compile(r"Limiar de nível: (\d+\.\d{3})")


def limiar(valor, voltas=6):
    """Toca no slider do limiar até o rótulo ficar em [valor, valor + 0,002] (o rótulo tem 3 casas)."""
    alvo = float(valor)
    for _ in range(voltas):
        lista = dump()
        rotulo = next((n for n in lista if _ROTULO_LIMIAR.fullmatch(n[0])), None)
        if rotulo is None:
            print("ui.py: rótulo do limiar não está na tela (abra o painel)", file=sys.stderr)
            return 1
        atual = float(_ROTULO_LIMIAR.fullmatch(rotulo[0]).group(1))
        if alvo <= atual <= alvo + 0.002:
            print(f"{atual:.3f}")
            return 0
        abaixo = [n for n in lista if n[2][1] >= rotulo[2][3]
                  and (n[1].endswith("SeekBar") or (n[0] == "" and n[2][2] - n[2][0] > 500))]
        if not abaixo:
            print("ui.py: slider do limiar não achado", file=sys.stderr)
            return 1
        x1, y1, x2, y2 = min(abaixo, key=lambda n: n[2][1])[2]
        x = round(x1 + (x2 - x1) * (alvo + 0.001))
        adb("shell", "input", "tap", str(min(max(x, x1), x2)), str((y1 + y2) // 2))
        time.sleep(0.5)
    print("ui.py: o limiar não chegou ao alvo; ajuste à mão e confira o meta.txt", file=sys.stderr)
    return 1


def interruptor(rotulo, valor):
    """O Switch do Compose não tem texto e é irmão do rótulo: acha o nó checável à direita, na mesma linha."""
    for _ in range(2):
        lista = dump()
        r = achar(lista, rotulo)
        if r is None:
            print(f"ui.py: '{rotulo}' não achado", file=sys.stderr)
            return 1
        y = centro(r[2])[1]
        switches = [n for n in lista if n[5] and n[2][1] <= y <= n[2][3] and n[2][0] >= r[2][2]]
        if len(switches) != 1:
            print(f"ui.py: {len(switches)} interruptor(es) na linha de '{rotulo}'", file=sys.stderr)
            return 1
        if switches[0][4] == (valor == "on"):
            print(valor)
            return 0
        x, yy = centro(switches[0][2])
        adb("shell", "input", "tap", str(x), str(yy))
        time.sleep(0.5)
    print(f"ui.py: o interruptor de '{rotulo}' não ficou {valor}", file=sys.stderr)
    return 1


def esperar_habilitado(texto, segundos):
    fim = time.time() + float(segundos)
    while time.time() < fim:
        n = achar(dump(), texto)
        if n is not None and n[3]:
            return 0
        time.sleep(1)
    return 1


def esperar(texto, segundos):
    fim = time.time() + float(segundos)
    while time.time() < fim:
        if achar(dump(), texto) is not None:
            return 0
        time.sleep(1)
    return 1


EXEMPLO = """<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><hierarchy rotation="0">
<node text="" package="com.android.systemui" class="android.widget.FrameLayout" bounds="[0,0][1080,80]" enabled="true">
<node text="Mensagem de alguém" package="com.android.systemui" class="android.widget.TextView" bounds="[0,0][500,80]" enabled="true" /></node>
<node text="" package="io.github.gigioderico.tribuna.etapa0" class="android.view.View" bounds="[0,0][1080,2340]" enabled="true">
<node text="" package="io.github.gigioderico.tribuna.etapa0" class="android.view.View" bounds="[30,190][140,270]" enabled="true" selected="true">
<node text="T01" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.TextView" bounds="[36,200][136,260]" enabled="true" /></node>
<node text="Ouvir" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.TextView" bounds="[36,2100][236,2200]" enabled="true" />
<node text="Pausar" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.TextView" bounds="[260,2100][460,2200]" enabled="false" />
<node text="T01" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.EditText" bounds="[36,1800][1044,1900]" enabled="true" />
<node text="Vocabulário restrito" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.TextView" bounds="[36,1600][400,1660]" enabled="true" />
<node text="" package="io.github.gigioderico.tribuna.etapa0" class="android.view.View" bounds="[430,1590][560,1670]" enabled="true" checkable="true" checked="false" />
<node text="instalação 812 ms · prepare 1234 ms · beginSession 45 ms · 1º parcial - ms" package="io.github.gigioderico.tribuna.etapa0" class="android.widget.TextView" bounds="[36,1500][1044,1540]" enabled="true" />
</node></hierarchy>"""


def autoteste():
    lista = nos(EXEMPLO)
    assert all("Mensagem" not in n[0] for n in lista), "nó de outro pacote entrou"
    assert ("Mensagem de alguém", "com.android.systemui", (0, 0, 500, 80)) in nos_sistema(EXEMPLO)
    assert achar(lista, "Ouvir")[2] == (36, 2100, 236, 2200)
    assert centro(achar(lista, "Ouvir")[2]) == (136, 2150)
    assert achar(lista, "T01") is None, "T01 aparece duas vezes (chip e campo): ambíguo"
    assert achar(lista, "Pausar")[3] is False
    assert [n[4] for n in lista if n[0] == "T01"] == [True, False], "marcado vem do contêiner do chip"
    r = achar(lista, "Vocabulário restrito")
    y = centro(r[2])[1]
    sw = [n for n in lista if n[5] and n[2][1] <= y <= n[2][3] and n[2][0] >= r[2][2]]
    assert len(sw) == 1 and sw[0][4] is False, "interruptor da linha do rótulo"
    numeros = [n[0] for n in lista if re.search(r"beginSession \d+ ms", n[0])]
    assert numeros == ["instalação 812 ms · prepare 1234 ms · beginSession 45 ms · 1º parcial - ms"]
    print("ui.py autoteste: ok")
    return 0


def main(argv):
    if not argv:
        print(__doc__)
        return 2
    comando, args = argv[0], argv[1:]
    try:
        if comando == "autoteste":
            return autoteste()
        funcoes = {"tocar": tocar, "chip": chip, "campo": campo, "tem": lambda t: 0 if achar(dump(), t) else 1,
                   "linhas": linhas, "esperar": esperar, "marcado": marcado, "limiar": limiar,
                   "tem-sistema": tem_sistema, "tocar-sistema": tocar_sistema, "interruptor": interruptor,
                   "esperar-habilitado": esperar_habilitado}
        return funcoes[comando](*args)
    except FalhaDump as erro:
        print(f"ui.py: uiautomator falhou: {erro}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
```

`take.sh`:

```bash
#!/usr/bin/env bash
# take.sh preparar <ID> | fechar <nome> [reproducoes|ensaio] | puxar <nome> [reproducoes|ensaio]
# Antes e depois de cada take (spec C1, C2; impl_plan 0005, A8). O humano toca Ouvir/Pausar; o agente fecha.
source "$(dirname "$0")/comum.sh"
exige_aparelho
cmd="${1:-}"; arg="${2:-}"; sub="${3:-}"
case "$cmd" in
  preparar)
    [[ "$arg" =~ ^(T0[1-9]|T10|L30)$ ]] || { echo "ID inválido: $arg" >&2; exit 2; }
    a am start -n "$PKG/.MainActivity" > /dev/null
    sleep 1
    ui chip "$arg"
    log "preparado $arg" ;;
  fechar)
    nome="$arg"; id="${nome%%_*}"
    [[ "$nome" =~ ^(T0[1-9]|T10|L30)(_[a-z0-9]+)+$ ]] || { echo "nome fora do padrão: $nome" >&2; exit 2; }
    ui tocar "Depuração"
    ui tocar "Parar"
    ui campo "$id" "$nome"
    ui tocar "Exportar"
    sleep 1
    ui linhas "^Exportado: " | tee -a "$EV/diario.txt"
    ui tocar "Depuração"
    "$0" puxar "$nome" "$sub" ;;
  puxar)
    nome="$arg"; id="${nome%%_*}"
    destino_t="$E0/trajetorias${sub:+/$sub}"; destino_e="$E0/eventos${sub:+/$sub}"
    if [ "$sub" = ensaio ]; then destino_t="$TMPD/ensaio"; destino_e="$TMPD/ensaio"; fi  # ensaio: fora do repositório
    mkdir -p "$destino_t" "$destino_e"
    for suf in hypotheses.csv level.csv trajectory.csv meta.txt events.csv; do
      alvo="$destino_t/$nome.$suf"; [ "$suf" = events.csv ] && alvo="$destino_e/$nome.$suf"
      if [ -e "$alvo" ]; then echo "PARAR: $alvo já existe (take refeito ganha t<n+1>)" >&2; exit 1; fi
      adb pull "$EXPORTS/$nome.$suf" "$TMPD/$nome.$suf" > /dev/null
      mv "$TMPD/$nome.$suf" "$alvo"
    done
    meta="$destino_t/$nome.meta.txt"
    grep -qx "text_id=$id" "$meta" || { echo "PARAR: text_id do meta.txt não é $id" >&2; exit 1; }
    log "puxado $nome: $(tr '\n' ' ' < "$meta")"
    wc -l "$destino_t/$nome."*.csv "$destino_e/$nome.events.csv" | tee -a "$EV/diario.txt" ;;
  *) echo "uso: take.sh preparar <ID> | fechar <nome> [reproducoes] | puxar <nome> [reproducoes]" >&2; exit 2 ;;
esac
```

`medir.sh`:

```bash
#!/usr/bin/env bash
# medir.sh converter <m4a> <nome> | sinc <rodada> <nome> [reproducao <rep>] | verdade <nome>
#          | folha <rodada> <nome> [reproducao <rep>] | metricas <rodada> <nome> [reproducao <rep>]
# Do áudio ao relatório de uma gravação (spec C2, D1–D5, E1; impl_plan 0005, A3–A5). Saídas e códigos vão para
# relatorios/<rodada>/ e para o diário. Nenhum passo sobrescreve WAV, words.csv ou expected.csv (correções da
# revisão ficam no expected.csv e no revisao.md, D7).
source "$(dirname "$0")/comum.sh"
cmd="${1:-}"
rc_aceito() { [ "$1" -le "$2" ] || { echo "PARAR: código $1 (entrada inválida); ver stderr acima" >&2; exit 1; }; }

case "$cmd" in
  converter)
    m4a="$2"; nome="$3"; wav="$E0/gravacoes/$nome.wav"
    [ -e "$wav" ] && { echo "PARAR: $wav já existe" >&2; exit 1; }
    mkdir -p "$EV/afinfo"
    afinfo "$m4a" > "$EV/afinfo/$nome.original.txt"
    afconvert -f WAVE -d LEI16@16000 -c 1 --mix "$m4a" "$wav"
    afinfo "$wav" > "$EV/afinfo/$nome.wav.txt"
    python3 -c "import wave,sys; w=wave.open(sys.argv[1]); assert (w.getnchannels(),w.getsampwidth(),w.getframerate(),w.getcomptype())==(1,2,16000,'NONE'), 'formato'; print('wav ok', w.getnframes()/16000, 's')" "$wav" | tee -a "$EV/diario.txt" ;;
  sinc)
    rodada="$2"; nome="$3"; rel="$E0/relatorios/$rodada"; mkdir -p "$rel"
    if [ "${4:-}" = reproducao ]; then
      rep="$5"; rel="$rel/reproducoes"; mkdir -p "$rel"
      set +e; python3 "$FERR/sincronizar.py" "$E0/gravacoes/$nome.wav" "$E0/trajetorias/reproducoes/$rep.level.csv" \
        --faixa-ms -1000 1000 --json "$rel/$rep.sinc.json" > "$rel/$rep.sinc.md"; rc=$?; set -e
      log "sinc $rep: rc=$rc d=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['deslocamento_ms'])" "$rel/$rep.sinc.json" 2>/dev/null)"
    else
      set +e; python3 "$FERR/sincronizar.py" "$E0/gravacoes/$nome.wav" "$E0/trajetorias/$nome.level.csv" \
        --json "$rel/$nome.sinc.json" > "$rel/$nome.sinc.md"; rc=$?; set -e
      log "sinc $nome: rc=$rc d=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['deslocamento_ms'])" "$rel/$nome.sinc.json" 2>/dev/null)"
    fi
    rc_aceito "$rc" 1 ;;
  verdade)
    nome="$2"; id="${nome%%_*}"; esp="$E0/esperado"
    words="$esp/$nome.words.csv"; expected="$esp/$nome.expected.csv"
    [ -x "$VENV/bin/python" ] || { echo "PARAR: venv do D1 ausente ($VENV); o passo D1 vem antes" >&2; exit 1; }
    if [ ! -e "$words" ]; then
      "$VENV/bin/python" "$FERR/timestamps.py" "$E0/gravacoes/$nome.wav" --modelo "$MODELO" -o "$words"
      log "timestamps $nome: rc=0 $(wc -l < "$words") linhas"
    fi
    if [ ! -e "$expected" ]; then
      mkdir -p "$EV/alinhar"
      python3 "$FERR/alinhar.py" "$E0/textos/$id.txt" "$words" -o "$expected" 2> "$EV/alinhar/$nome.stderr.txt"
      log "alinhar $nome: rc=0; $(head -3 "$EV/alinhar/$nome.stderr.txt" | tr '\n' ' ')"
    fi ;;
  folha)
    rodada="$2"; nome="$3"; id="${nome%%_*}"
    case "$id" in T04|T05|T06|L30) ;; *) echo "$id não tem folha (só T04, T05, T06, L30)"; exit 0 ;; esac
    sincjson="$E0/relatorios/$rodada/$nome.sinc.json"; folha="$E0/eventos/$nome.folha.events.csv"; rotulo="$nome"
    if [ "${4:-}" = reproducao ]; then  # a folha da reprodução sai no relógio da reprodução (A10)
      sincjson="$E0/relatorios/$rodada/reproducoes/$5.sinc.json"; folha="$E0/eventos/reproducoes/$5.folha.events.csv"
      rotulo="$5"
    fi
    d=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['deslocamento_ms'])" "$sincjson")
    mkdir -p "$EV/folha"
    set +e; python3 "$FERR/folha.py" "$E0/textos/$id.txt" "$E0/esperado/$nome.expected.csv" "$E0/esperado/$nome.words.csv" \
      --deslocamento-ms "$d" -o "$folha" 2> "$EV/folha/$rotulo.stderr.txt"; rc=$?; set -e
    log "folha $rotulo: rc=$rc (pontos de escuta em evidencias/folha/$rotulo.stderr.txt)"
    rc_aceito "$rc" 1 ;;
  metricas)
    rodada="$2"; nome="$3"; rel="$E0/relatorios/$rodada"
    traj="$E0/trajetorias/$nome"; ev="$E0/eventos/$nome.events.csv"; saida="$nome"; sincjson="$rel/$nome.sinc.json"
    if [ "${4:-}" = reproducao ]; then
      rep="$5"; traj="$E0/trajetorias/reproducoes/$rep"; ev="$E0/eventos/reproducoes/$rep.events.csv"
      rel="$rel/reproducoes"; saida="$rep"; sincjson="$rel/$rep.sinc.json"
    fi
    d=$(python3 -c "import json,sys;print(json.load(open(sys.argv[1]))['deslocamento_ms'])" "$sincjson")
    folha="$E0/eventos/$nome.folha.events.csv"
    [ "${4:-}" = reproducao ] && folha="$E0/eventos/reproducoes/$5.folha.events.csv"
    eventos=("$ev"); [ -e "$folha" ] && eventos+=("$folha")
    for par in ":$d" ".m200:$((d - 200))" ".p200:$((d + 200))"; do
      suf="${par%%:*}"; desl="${par##*:}"
      set +e; python3 "$FERR/metricas.py" "$traj.trajectory.csv" "$E0/esperado/$nome.expected.csv" "${eventos[@]}" \
        --deslocamento-ms "$desl" --hipoteses "$traj.hypotheses.csv" --json "$rel/$saida$suf.json" > "$rel/$saida$suf.md"
      rc=$?; set -e
      log "metricas $saida$suf (d=$desl): rc=$rc"
      rc_aceito "$rc" 1
    done ;;
  *) echo "uso: ver o cabeçalho" >&2; exit 2 ;;
esac
```

`reproduzir.py`:

```python
#!/usr/bin/env python3
"""reproduzir.py <oficial> --preset P --vocab on|off --estrategia novo|reset [--limiar V] [--painel] [--ciclos N]
[--captura] [--rodada S1]

Reproduz no app o WAV de uma gravação oficial pela fonte WAV da 0003, com a configuração pedida, e exporta como
<oficial>_r_<preset>_<vocab>_<estrategia>_l<limiar×1000 em 4 dígitos> (spec D1, D11, F1–F4, G1; impl_plan 0005, A10).
- As pausas da gravação oficial (pausa_toque/retoma_toque do events.csv dela) são repetidas por adb no mesmo
  instante do WAV: t_wav = t_app_oficial + deslocamento oficial (sinc.json da rodada).
- --painel: lê as linhas de beginSession e de latência do painel durante a reprodução (uiautomator, um dump por
  volta) e grava em evidencias/painel/<rep>.tsv; --captura troca o dump por screencap (PNG fora do repositório)
  se o uiautomator não conseguir ler a tela em movimento.
- --ciclos N: em vez de seguir as pausas oficiais, faz N ciclos Pausar -> Ouvir (F3) e lê o beginSession de cada um.
Só dirige o aparelho; os números saem do metricas.py, do veredito.py e do painel.
"""

import argparse
import json
import re
import subprocess
import sys
import time
import wave
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import ui  # noqa: E402

RAIZ = Path(subprocess.run(["git", "-C", str(Path(__file__).parent), "rev-parse", "--show-toplevel"],
                           capture_output=True, text=True, check=True).stdout.strip())
E0 = RAIZ / "etapa0"
EV = RAIZ / "work" / "0005-etapa0-medicao" / "evidencias"
WAVDIR = f"/sdcard/Android/data/{ui.PACOTE}/files/wav"
PRESETS = {"conservador": "Conservador", "normal": "Normal", "agil": "Ágil"}
ESTRATEGIAS = {"novo": "Novo por sessão", "reset": "reset()"}
_PAINEL = re.compile(r"beginSession|latência 2:")


def nome_reproducao(oficial, preset, vocab, estrategia, limiar):
    return f"{oficial}_r_{preset}_{vocab}_{estrategia}_l{round(limiar * 1000):04d}"


def agenda_pausas(eventos_csv, deslocamento_oficial):
    """[(t_wav_ms, botão)] das pausas da gravação oficial, no relógio do WAV."""
    saida = []
    for linha in eventos_csv.splitlines()[1:]:
        t, evento, _ = linha.split(",", 2)
        if evento == "pausa_toque":
            saida.append((int(t) + deslocamento_oficial, "Pausar"))
        elif evento == "retoma_toque":
            saida.append((int(t) + deslocamento_oficial, "Ouvir"))
    return saida


def agenda_ciclos(n, inicio_ms=8000, periodo_ms=8000, pausa_ms=2000):
    saida = []
    for k in range(n):
        t = inicio_ms + k * periodo_ms
        saida += [(t, "Pausar"), (t + pausa_ms, "Ouvir")]
    return saida


def duracao_ms(caminho):
    with wave.open(str(caminho)) as w:
        return w.getnframes() * 1000 // w.getframerate()


def coordenadas(*textos):
    lista = ui.dump()
    saida = {}
    for t in textos:
        n = ui.achar(lista, t)
        if n is None:
            raise SystemExit(f"reproduzir.py: '{t}' não está na tela")
        saida[t] = ui.centro(n[2])
    return saida


def tocar_xy(xy):
    ui.adb("shell", "input", "tap", str(xy[0]), str(xy[1]))


def amostrar(destino, captura, rotulo):
    agora = time.time()
    if captura:
        png = ui.TMP / f"painel-{rotulo}-{agora:.0f}.png"
        ui.TMP.mkdir(parents=True, exist_ok=True)
        with open(png, "wb") as f:
            subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=f, check=False)
        destino.write(f"{agora:.3f}\t{rotulo}\tcaptura\t{png}\n")
        return
    try:
        linhas = [n[0] for n in ui.dump() if _PAINEL.search(n[0])]
    except ui.FalhaDump as erro:
        destino.write(f"{agora:.3f}\t{rotulo}\tfalha\t{erro}\n")
        return
    for linha in linhas:
        destino.write(f"{agora:.3f}\t{rotulo}\tpainel\t{linha}\n")


def configurar(preset, vocab, estrategia, limiar, wav_nome):
    ui.tocar("Atualizar WAV")
    passos = [(wav_nome, None), (PRESETS[preset], None), (ESTRATEGIAS[estrategia], None)]
    for texto, _ in passos:
        if ui.tocar(texto) != 0:
            raise SystemExit(f"reproduzir.py: não consegui tocar em '{texto}'")
    if ui.interruptor("Vocabulário restrito", vocab) != 0:
        raise SystemExit("reproduzir.py: vocabulário não ajustado")
    if limiar > 0 and ui.limiar(str(limiar)) != 0:
        raise SystemExit("reproduzir.py: limiar não ajustado")


def main(argv=None):
    p = argparse.ArgumentParser(prog="reproduzir.py")
    p.add_argument("oficial")
    p.add_argument("--preset", required=True, choices=sorted(PRESETS))
    p.add_argument("--vocab", required=True, choices=("on", "off"))
    p.add_argument("--estrategia", required=True, choices=sorted(ESTRATEGIAS))
    p.add_argument("--limiar", type=float, default=0.0)
    p.add_argument("--rodada", default="S1")
    p.add_argument("--painel", action="store_true")
    p.add_argument("--captura", action="store_true")
    p.add_argument("--ciclos", type=int, default=0)
    p.add_argument("--ensaio", help="WAV do ensaio (D21), fora do repositório: sem pausas, exporta fora do repositório")
    p.add_argument("--autoteste", action="store_true")
    args = p.parse_args(argv)
    if args.autoteste:
        assert nome_reproducao("T01_giorgio_sala_t1", "agil", "off", "novo", 0.0) == "T01_giorgio_sala_t1_r_agil_off_novo_l0000"
        assert nome_reproducao("T01_giorgio_sala_t1", "normal", "on", "reset", 0.012) == "T01_giorgio_sala_t1_r_normal_on_reset_l0012"
        ev = "t_ms,evento,detalhe\n5000,pausa_toque,\n15500,retoma_toque,\n20000,toque_palavra,12\n"
        assert agenda_pausas(ev, 1370) == [(6370, "Pausar"), (16870, "Ouvir")]
        assert agenda_ciclos(2) == [(8000, "Pausar"), (10000, "Ouvir"), (16000, "Pausar"), (18000, "Ouvir")]
        print("reproduzir.py autoteste: ok")
        return 0
    oficial = args.oficial
    texto_id = oficial.split("_")[0]
    wav = Path(args.ensaio) if args.ensaio else E0 / "gravacoes" / f"{oficial}.wav"
    sub = "ensaio" if args.ensaio else "reproducoes"
    destino = (ui.TMP / "ensaio") if args.ensaio else (E0 / "trajetorias" / "reproducoes")
    rep = nome_reproducao(oficial, args.preset, args.vocab, args.estrategia, args.limiar)
    if (destino / f"{rep}.trajectory.csv").exists():
        raise SystemExit(f"reproduzir.py: {rep} já existe")
    if args.ensaio:
        agenda = []
    elif args.ciclos:
        agenda = agenda_ciclos(args.ciclos)
    else:
        sinc = json.loads((E0 / "relatorios" / args.rodada / f"{oficial}.sinc.json").read_text(encoding="utf-8"))
        agenda = agenda_pausas((E0 / "eventos" / f"{oficial}.events.csv").read_text(encoding="utf-8"),
                               sinc["deslocamento_ms"])
    if "wav" not in ui.adb("shell", "ls", f"{WAVDIR}/{oficial}.wav"):
        ui.adb("push", str(wav), f"{WAVDIR}/{oficial}.wav")
    ui.adb("shell", "am", "start", "-n", f"{ui.PACOTE}/.MainActivity")
    time.sleep(1)
    if ui.chip(texto_id) != 0:
        raise SystemExit(1)
    ui.tocar("Depuração")
    configurar(args.preset, args.vocab, args.estrategia, args.limiar, f"{oficial}.wav")
    if not args.painel:
        ui.tocar("Depuração")
    xy = coordenadas("Ouvir", "Pausar")
    EV.joinpath("painel").mkdir(parents=True, exist_ok=True)
    fim_ms = (args.ciclos * 8000 + 10000) if args.ciclos else duracao_ms(wav) + 3000
    with open(EV / "painel" / f"{rep}.tsv", "a", encoding="utf-8") as painel:
        tocar_xy(xy["Ouvir"])
        t0 = time.monotonic()
        pendentes = list(agenda)
        while (time.monotonic() - t0) * 1000 < fim_ms:
            agora_ms = (time.monotonic() - t0) * 1000
            if pendentes and agora_ms >= pendentes[0][0]:
                t, botao = pendentes.pop(0)
                tocar_xy(xy[botao])
                painel.write(f"{time.time():.3f}\ttoque\t{botao}\t{t}\n")
                if args.ciclos and botao == "Ouvir":
                    time.sleep(1.5)
                    amostrar(painel, args.captura, f"ciclo-{len(agenda) - len(pendentes)}")
                continue
            if args.painel and not args.ciclos:
                amostrar(painel, args.captura, "leitura")
            else:
                time.sleep(0.05)
    subprocess.run([str(Path(__file__).parent / "take.sh"), "fechar", rep, sub], check=True)
    meta = (destino / f"{rep}.meta.txt").read_text(encoding="utf-8")
    esperado = {"audio_source": "wav", "wav_name": f"{oficial}.wav", "vocabulary": args.vocab,
                "recognizer_strategy": "NewPerSession" if args.estrategia == "novo" else "Reset",
                "preset": {"conservador": "Conservador", "normal": "Normal", "agil": "Agil"}[args.preset]}
    erros = [f"{k}={v}" for k, v in esperado.items() if f"{k}={v}" not in meta.splitlines()]
    if erros:
        raise SystemExit(f"reproduzir.py: PARAR: meta.txt de {rep} não tem {', '.join(erros)}")
    print(f"reproduzido: {rep}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

`privacidade.sh`:

```bash
#!/usr/bin/env bash
# privacidade.sh arquivos <rotulo> | comparar <antes> <depois> | logcat-limpar | logcat <rotulo>
# Regras 2 e 3 no aparelho (spec B1, B5; impl_plan 0005, A11). O logcat inteiro do aparelho do humano nunca vai
# para o repositório (tem dados de outros apps): fica em $TMPD, é lido por grep e apagado; na evidência só as
# contagens.
source "$(dirname "$0")/comum.sh"
exige_aparelho
PRIV="$EV/privacidade"; mkdir -p "$PRIV"
# 10 palavras distintivas dos textos lidos (conferidas em etapa0/textos/ no PLAN), com e sem acento
PALAVRAS=(rodeios conselhos estiagem agricultor perfeccionismo escondida "paciência" paciencia agradecer
          "coríntios" corintios subestimado)
AUDIO='\.(wav|pcm|raw|m4a|3gp|amr|ogg)$'
case "${1:-}" in
  arquivos)
    r="$2"
    { a run-as "$PKG" find . -type f 2>&1 | sed 's|^|interno: |'
      a find "/sdcard/Android/data/$PKG" -type f 2>&1 | sed 's|^|externo: |'; } | sort > "$PRIV/arquivos-$r.txt"
    wc -l < "$PRIV/arquivos-$r.txt" ;;
  comparar)
    novos="$PRIV/arquivos-novos-$2-$3.txt"
    comm -13 "$PRIV/arquivos-$2.txt" "$PRIV/arquivos-$3.txt" > "$novos"
    echo "novos: $(wc -l < "$novos")"; cat "$novos"
    ruins=$(grep -E -i "$AUDIO" "$novos" || true)
    fora=$(grep '^externo: ' "$novos" | grep -v "/files/exports/" || true)
    internos=$(grep '^interno: ' "$novos" || true)
    if [ -n "$ruins$fora$internos" ]; then
      echo "REPROVA: arquivo novo de áudio, fora de exports/ ou no armazenamento interno:"; echo "$ruins$fora$internos"; exit 1
    fi
    echo "PASSA: nenhum arquivo novo de áudio; novos só em exports/" ;;
  logcat-limpar) adb logcat -c; log "logcat limpo" ;;
  logcat)
    r="$2"; bruto="$TMPD/logcat-$r.txt"
    adb logcat -d > "$bruto"
    {
      echo "linhas do logcat: $(wc -l < "$bruto")"
      echo "linhas do pacote $PKG: $(grep -c "$PKG" "$bruto" || true)"
      echo "FATAL EXCEPTION: $(grep -c 'FATAL EXCEPTION' "$bruto" || true)"
      for p in "${PALAVRAS[@]}"; do echo "palavra $p: $(grep -c -i -w "$p" "$bruto" || true)"; done
    } > "$PRIV/logcat-$r.txt"
    rm -f "$bruto"
    cat "$PRIV/logcat-$r.txt"
    if grep -E -q '^(FATAL EXCEPTION|palavra [^:]+): [1-9]' "$PRIV/logcat-$r.txt"; then echo "REPROVA"; exit 1; fi
    echo "PASSA" ;;
  *) echo "uso: privacidade.sh arquivos <r> | comparar <antes> <depois> | logcat-limpar | logcat <r>" >&2; exit 2 ;;
esac
```

`memoria.sh`:

```bash
#!/usr/bin/env bash
# memoria.sh <rotulo> <minutos>: dumpsys meminfo do pacote a cada 60 s (spec G3, D15; impl_plan 0005, A12).
# Rodar em segundo plano durante a S1; para sozinho no fim ou quando o processo morre (registrado).
source "$(dirname "$0")/comum.sh"
r="$1"; minutos="$2"; saida="$EV/memoria-$r.tsv"
echo -e "hora\tpid\tlinha_total" > "$saida"
for _ in $(seq 1 "$minutos"); do
  pid=$(a pidof "$PKG" || true)
  if [ -z "$pid" ]; then echo -e "$(date '+%H:%M:%S')\t-\tprocesso morto" >> "$saida"
  else echo -e "$(date '+%H:%M:%S')\t$pid\t$(a dumpsys meminfo "$PKG" | grep -E 'TOTAL PSS:|TOTAL RSS:' | tr -s ' ' | head -1)" >> "$saida"
  fi
  sleep 60
done
python3 - "$saida" <<'EOF'
import re, statistics, sys
pss, rss = [], []
for linha in open(sys.argv[1], encoding="utf-8").read().splitlines()[1:]:
    m = re.search(r"TOTAL PSS: (\d+).*TOTAL RSS: (\d+)", linha)
    if m:
        pss.append(int(m.group(1))); rss.append(int(m.group(2)))
for nome, v in (("PSS", pss), ("RSS", rss)):
    print(f"{nome} kB: n={len(v)}, mediana={statistics.median(v) if v else None}, pico={max(v) if v else None}")
EOF
```

`frio.sh`:

```bash
#!/usr/bin/env bash
# frio.sh quente <n> | primeira <n>: inicialização a frio e primeira cópia do modelo (spec G2, D17; impl_plan 0005, A12).
# Cada corrida: force-stop (ou pm clear + pm grant), am start -W, chip T01, Ouvir até o modelo carregar, Pausar,
# Parar, e lê o painel ("instalação X ms · prepare Y ms · beginSession Z ms · ..."). O microfone abre por ~3 s.
source "$(dirname "$0")/comum.sh"
exige_aparelho
modo="$1"; n="$2"; saida="$EV/frio-$modo.tsv"
echo -e "corrida\tTotalTime_ms\tpainel" > "$saida"
for i in $(seq 1 "$n"); do
  a am force-stop "$PKG"
  if [ "$modo" = primeira ]; then
    a pm clear "$PKG" > /dev/null
    a pm grant "$PKG" android.permission.RECORD_AUDIO
  fi
  total=$(a am start -W -n "$PKG/.MainActivity" | sed -n 's/^TotalTime: //p')
  sleep 1
  ui chip T01
  ui tocar "Ouvir"
  ui esperar-habilitado "Pausar" 180 || { echo "PARAR: o modelo não carregou em 180 s" >&2; exit 1; }
  sleep 3
  ui tocar "Pausar"
  ui tocar "Depuração"
  ui tocar "Parar"
  painel=$(ui linhas "beginSession" | head -1)
  echo -e "$i\t$total\t$painel" | tee -a "$saida"
done
python3 - "$saida" <<'EOF'
import re, statistics, sys
linhas = open(sys.argv[1], encoding="utf-8").read().splitlines()[1:]
def col(regex):
    v = [int(m.group(1)) for l in linhas for m in [re.search(regex, l)] if m]
    return f"n={len(v)}, mediana={statistics.median(v) if v else None}, máximo={max(v) if v else None}"
print("TotalTime ms:", col(r"^\d+\t(\d+)\t"))
print("prepare ms:", col(r"prepare (\d+) ms"))
print("instalação ms:", col(r"instalação (\d+) ms"))
EOF
```

`ensaio.py`:

```python
#!/usr/bin/env python3
"""ensaio.py montar <ID> | concatenar <saida> <wav>...: áudio sintético do ensaio (D21) e o WAV longo da bateria (D14).

montar: `say` com a voz pt-BR Luciana lê etapa0/textos/<ID>.txt; o afconvert passa a 16 kHz, mono, PCM 16 bits; na
frente entram 2 s de silêncio, uma palma sintética de 20 ms e mais 2 s (o roteiro do humano). Sai em
$TMPDIR/tribuna-0005/ensaio/<ID>_ensaio_sala_t1.wav, FORA do repositório (o ensaio não conta para critério nenhum).
concatenar: junta WAVs de gravacoes/ num só (bateria, D14), também fora do repositório.
Nada aqui grava voz de ninguém: o `say` é voz sintética e a concatenação só copia WAVs de texto de teste.
"""

import os
import subprocess
import sys
import tempfile
import wave
from array import array
from pathlib import Path

TMP = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "tribuna-0005" / "ensaio"
RAIZ = Path(subprocess.run(["git", "-C", str(Path(__file__).parent), "rev-parse", "--show-toplevel"],
                           capture_output=True, text=True, check=True).stdout.strip())


def montar(texto_id):
    TMP.mkdir(parents=True, exist_ok=True)
    aiff, fala = TMP / f"{texto_id}.aiff", TMP / f"{texto_id}.fala.wav"
    subprocess.run(["say", "-v", "Luciana", "-f", str(RAIZ / "etapa0" / "textos" / f"{texto_id}.txt"), "-o", str(aiff)],
                   check=True)
    subprocess.run(["afconvert", "-f", "WAVE", "-d", "LEI16@16000", "-c", "1", "--mix", str(aiff), str(fala)], check=True)
    with wave.open(str(fala)) as w:
        amostras = array("h")
        amostras.frombytes(w.readframes(w.getnframes()))
    palma = array("h", [30000 if k % 2 == 0 else -30000 for k in range(320)])
    tudo = array("h", [0] * 32000) + palma + array("h", [0] * (32000 - 320)) + amostras
    saida = TMP / f"{texto_id}_ensaio_sala_t1.wav"
    with wave.open(str(saida), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(16000)
        w.writeframes(tudo.tobytes())
    aiff.unlink()
    fala.unlink()
    print(f"{saida}\t{len(tudo) / 16000:.1f} s; palma em 2000 ms")
    return 0


def concatenar(saida, wavs):
    with wave.open(str(saida), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(16000)
        total = 0
        for caminho in wavs:
            with wave.open(str(caminho)) as w:
                assert (w.getnchannels(), w.getsampwidth(), w.getframerate()) == (1, 2, 16000), caminho
                out.writeframes(w.readframes(w.getnframes()))
                total += w.getnframes()
    print(f"{saida}\t{total / 16000 / 60:.1f} min")
    return 0


if __name__ == "__main__":
    if sys.argv[1:2] == ["montar"]:
        sys.exit(montar(sys.argv[2]))
    if sys.argv[1:2] == ["concatenar"]:
        sys.exit(concatenar(sys.argv[2], sys.argv[3:]))
    print(__doc__)
    sys.exit(2)
```

---

## Runbook de execução (qa-publicacao)

Convenções: comandos da raiz do repositório, em **bash** (`bash -c '...'` se o shell for zsh); `S=work/0005-etapa0-medicao/scripts`; toda saída citada vai para `work/0005-etapa0-medicao/evidencias/`; `diario.txt` recebe uma linha por passo (os scripts já escrevem). Antes de qualquer fase com o aparelho: `adb devices -l` mostra `RQCW80BPXKN device`; senão, a parada H-cabo.

**Mensagens padrão para o humano** (o agente copia e encerra a vez):
- **H-cabo:** "Preciso do S23 no cabo, desbloqueado e com a tela ligada (a depuração USB já está autorizada). Me avise quando estiver assim."
- **H-ajuste:** "O adb não conseguiu ligar <item>. Pode ligar à mão no celular (puxando a barra do topo) e me avisar?"

### Fase A (orquestrador)

1. Conferir este plano no portão automático e commitar `impl_plan.md` no branch `work/0005-etapa0-medicao`.
2. `git worktree add ../Tribuna-0005-ferramentas -b work/0005-ferramentas work/0005-etapa0-medicao` e chamar o `motor-voz` (Tarefas 1–6). Em paralelo, chamar o `qa-publicacao` (Fase C).
3. Com o registro da construção feito, chamar o `revisor` (contexto limpo) sobre `git diff work/0005-etapa0-medicao...work/0005-ferramentas` e os scripts da Tarefa Q. APROVADO → `git merge --no-ff work/0005-ferramentas` em `work/0005-etapa0-medicao` e `git worktree remove ../Tribuna-0005-ferramentas`.

### Fase C (qa-publicacao, sem humano salvo H-cabo)

- **C0.** Tarefa Q.
- **C1 (A1).** `bash -c 'cd etapa0 && ./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest assembleDebug assembleRelease' 2>&1 | tee work/0005-etapa0-medicao/evidencias/a1-gradle.txt | tail -5`; `python3 etapa0/ferramentas/testar.py 2>&1 | tail -1 | tee work/0005-etapa0-medicao/evidencias/a1-testar-base.txt`; `git rev-parse HEAD > work/0005-etapa0-medicao/evidencias/a1-commit.txt`. Esperado: BUILD SUCCESSFUL e `149 testes … 0 pulados`. Depois do merge da Fase B, repetir o `testar.py` em `a1-testar-final.txt` (270).
- **C2 (A2).** `bash -c 'cd etapa0 && ./gradlew verify16kAlignment' > work/0005-etapa0-medicao/evidencias/a2-16k.txt 2>&1; echo "rc=$?" >> work/0005-etapa0-medicao/evidencias/a2-16k.txt`. Esperado: falha listando só `armeabi-v7a`/`x86` de `libvosk.so` (OPR-024). Outra falha → PARAR e escalar ao arquiteto.
- **Parada H-cabo** se `adb devices -l` não mostrar o aparelho ou `adb shell dumpsys window | grep isKeyguardShowing` der `true`.
- **C3 (A3).** `adb install -r etapa0/app/build/outputs/apk/debug/app-debug.apk`; `adb pull "$(adb shell pm path io.github.gigioderico.tribuna.etapa0 | sed 's/package://' | tr -d '\r')" "$TMPDIR/instalado.apk"`; `shasum -a 256 etapa0/app/build/outputs/apk/debug/app-debug.apk "$TMPDIR/instalado.apk" > work/0005-etapa0-medicao/evidencias/a3-sha256.txt`; `for p in ro.product.model ro.build.version.release ro.build.version.sdk ro.product.cpu.abilist; do echo "$p=$(adb shell getprop $p)"; done > work/0005-etapa0-medicao/evidencias/a3-aparelho.txt`; `adb shell getconf PAGE_SIZE >> …/a3-aparelho.txt`; `adb shell grep MemTotal /proc/meminfo >> …/a3-aparelho.txt`. Esperado: os dois sha256 iguais.
- **C4 (A4).** `bash $S/aparelho.sh salvar && bash $S/aparelho.sh aplicar`. Saída `PARAR: …` → H-ajuste e repetir `aplicar`.
- **C5 (B1).** `bash $S/privacidade.sh logcat-limpar`; `adb shell pm clear io.github.gigioderico.tribuna.etapa0`; `adb shell am start -n io.github.gigioderico.tribuna.etapa0/.MainActivity`; `python3 $S/ui.py chip T01`; `python3 $S/ui.py tocar Ouvir`; `sleep 2`; `python3 $S/ui.py tem-sistema "Permitir|Allow|uso do app|esta vez" | tee work/0005-etapa0-medicao/evidencias/b1-dialogo.txt` (esperado: uma ou mais linhas do `permissioncontroller`); `python3 $S/ui.py tocar-sistema "Durante o uso do app"` (outro texto: usar o listado; nada → H "toque em Permitir"). Depois `python3 $S/ui.py esperar-habilitado Pausar 180` (o modelo copia e carrega; o Pausar só habilita em leitura), `python3 $S/ui.py tocar Pausar`, `python3 $S/ui.py tocar Depuração`, `python3 $S/ui.py tocar Parar`, `python3 $S/ui.py linhas "beginSession" | tee work/0005-etapa0-medicao/evidencias/b1-painel.txt` e `bash $S/privacidade.sh logcat b1` (esperado `FATAL EXCEPTION: 0` e `PASSA`). O `pm clear` apaga também `files/wav` e as exportações do aparelho; por isso B1 e o G2 "primeira" vêm antes de qualquer WAV copiado.
- **C6 (B2).** `adb shell dumpsys package io.github.gigioderico.tribuna.etapa0 | sed -n '/requested permissions:/,/install permissions:/p' | tee work/0005-etapa0-medicao/evidencias/b2-permissoes.txt`. Esperado: uma linha só, `android.permission.RECORD_AUDIO`.
- **C7 e C8 (B3, B5, ensaio ao vivo).** `python3 $S/ensaio.py montar T01`; `bash $S/privacidade.sh arquivos b5-antes`; `bash $S/privacidade.sh logcat-limpar`; `bash $S/take.sh preparar T01`; `python3 $S/ui.py tocar Ouvir`; `sleep 1`; `afplay "$TMPDIR/tribuna-0005/ensaio/T01_ensaio_sala_t1.wav" &` (o Mac ao lado do celular, volume em 70%); depois de 20 s: `adb shell dumpsys media.audio_flinger | grep -i -E "input|record|16000|AUDIO_FORMAT_PCM_16_BIT|mono" | grep -v -i "output" > "$TMPDIR/tribuna-0005/af.txt"; grep -E "16000" "$TMPDIR/tribuna-0005/af.txt" | head -20 | tee work/0005-etapa0-medicao/evidencias/b3-audioflinger.txt` (esperado: uma trilha de entrada com `16000` Hz, canal mono e `PCM_16_BIT`; só linhas de entrada vão à evidência). Ao fim do `afplay`, `bash $S/take.sh fechar T01_ensaio_sala_t1 ensaio`; mediana do bloco: `python3 -c "import statistics,sys;t=[int(l.split(',')[0]) for l in open(sys.argv[1]).read().split()[1:]];d=[b-a for a,b in zip(t,t[1:])];print('mediana',statistics.median(d),'n',len(d))" "$TMPDIR/tribuna-0005/ensaio/T01_ensaio_sala_t1.level.csv" | tee -a work/0005-etapa0-medicao/evidencias/b3-audioflinger.txt` (esperado ~200). Privacidade: `bash $S/privacidade.sh arquivos b5-depois; bash $S/privacidade.sh comparar b5-antes b5-depois; bash $S/privacidade.sh logcat b5`. Esperado: `PASSA` nos dois (o único arquivo novo é a exportação em `files/exports/`).
- **C9 (B6).** `bash $S/take.sh preparar T01`; `python3 $S/ui.py tocar Ouvir`; `sleep 5`; `python3 $S/ui.py tocar Depuração`; `python3 $S/ui.py linhas "^sessão" > "$TMPDIR/tribuna-0005/sessao-antes.txt"`; rotação: `adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1; sleep 3`; `python3 $S/ui.py linhas "^sessão" > "$TMPDIR/tribuna-0005/sessao-depois.txt"`; `python3 $S/ui.py esperar-habilitado Pausar 5` (código 0: a leitura continua); `adb shell settings put system user_rotation 0`. Esperado: o número depois de `sessão` igual nos dois arquivos. Segundo plano: `adb shell input keyevent KEYCODE_HOME; sleep 2; adb shell dumpsys audio | grep -i -A3 "rec" | grep -i tribuna | tee work/0005-etapa0-medicao/evidencias/b6-audio.txt` (esperado: vazio, sem captura do pacote); `adb shell am start -n io.github.gigioderico.tribuna.etapa0/.MainActivity; sleep 2; python3 $S/ui.py esperar-habilitado Ouvir 5` (código 0: pausado, Ouvir habilitado). Juntar as linhas em `b6-*.txt`. Se o `ui.py` sair com 2 (painel mudando, sem estado ocioso), ler a linha `sessão` por `adb exec-out screencap -p > "$TMPDIR/tribuna-0005/b6-<n>.png"` (imagem fora do repositório) e anotar "lido de captura". Fechar com Depuração, Parar.
- **C10 (B7).** `adb install -r etapa0/app/build/outputs/apk/release/app-release.apk` (mesma chave de debug, `signingConfig = debug`); `adb shell wm size` só como registro; `python3 $S/reproduzir.py T01_ensaio_sala_t1 --ensaio "$TMPDIR/tribuna-0005/ensaio/T01_ensaio_sala_t1.wav" --preset normal --vocab off --estrategia novo`; `tail -1 "$TMPDIR/tribuna-0005/ensaio/T01_ensaio_sala_t1_r_normal_off_novo_l0000.trajectory.csv"`. Esperado: cursor 214 (último token do T01). Com voz sintética o cursor pode não chegar ao fim; nesse caso B7 é refeito depois da S1 com `T01_giorgio_sala_t1.wav` e o resultado sintético fica registrado como informativo. Depois: `adb install -r etapa0/app/build/outputs/apk/debug/app-debug.apk`. O `pm path` + `shasum` confere qual APK está instalado em cada momento (`b7-*.txt`).
- **C11 (B8; só depois do merge APROVADO).** `say -v '?' > work/0005-etapa0-medicao/evidencias/b8-say-vozes.txt`; reproduzir o ensaio no debug (`reproduzir.py … --ensaio …`, como em C10); `python3 etapa0/ferramentas/sincronizar.py "$TMPDIR/tribuna-0005/ensaio/T01_ensaio_sala_t1.wav" "$TMPDIR/tribuna-0005/ensaio/T01_ensaio_sala_t1_r_normal_off_novo_l0000.level.csv" --faixa-ms -1000 1000 | tee work/0005-etapa0-medicao/evidencias/b8-sinc-reproducao.txt`. Esperado `|deslocamento| <= 200` e palma confere. Ensaio ao vivo de C7: `python3 etapa0/ferramentas/sincronizar.py <ensaio.wav> <T01_ensaio_sala_t1.level.csv> --faixa-ms -5000 0 | tee …/b8-sinc-ao-vivo.txt` (informativo: `d` negativo de algumas centenas de ms, porque o `afplay` começou depois do Ouvir). **Toque fora da tela (D23):** `take.sh preparar L30`, `ui.py tocar Ouvir`, `sleep 3`, `adb shell input swipe 540 1500 540 500 400` (duas vezes), `python3 $S/ui.py linhas "^cursor"` antes e 2 s depois de `adb shell input tap 540 900`; depois Depuração, Parar e `take.sh fechar L30_ensaio_sala_t1 ensaio`; registrar se o `trajectory.csv` tem `Reposition` para um índice ≥ 200 e se a rolagem puxou de volta (`b8-toque.txt`). Se o toque não for confiável, o roteiro do L30 (minuto 22) diz ao humano: parar de falar, rolar com o dedo até "Imagine", tocar nela e seguir.
- **C12 (G2).** `bash $S/frio.sh primeira 3` e depois `bash $S/frio.sh quente 10`. Saídas em `frio-*.tsv`, com a mediana e o máximo impressos no fim.
- **C13 (F3; só depois do merge).** `python3 $S/reproduzir.py T01_ensaio_sala_t1 --ensaio "$TMPDIR/tribuna-0005/ensaio/T01_ensaio_sala_t1.wav" --preset normal --vocab off --estrategia novo --ciclos 10 --painel` e o mesmo com `--estrategia reset`. As linhas `beginSession` de cada ciclo estão em `evidencias/painel/*.tsv`. Se aparecer `falha … idle`, repetir com `--captura` e ler os PNG (A12). Calcular mediana, p95 (posto mais próximo), máximo e n por estratégia e anotar em `evidencias/f3-estrategia.md`.
- **C14.** `bash $S/aparelho.sh restaurar` (esperado `restaurado: igual ao antes`).

### Fase D (S1, com o humano)

- **PARAR E PEDIR AO HUMANO (H-S1):** "Pronto para a S1 (sala silenciosa, você lê; a segunda pessoa ajuda no T07 e no L30; ~2 h, com uma pausa antes do L30). Preciso de: o S23 no cabo e desbloqueado, num suporte a ~50 cm de você, de frente; o MacBook ao lado, à mesma distância; o QuickTime aberto em Arquivo → Nova Gravação de Áudio, microfone interno, qualidade Máxima; uma pasta para salvar os .m4a (sugestão: `~/Tribuna-gravacoes/`); um cronômetro. O improviso e a fala da outra pessoa têm de ser inventados e neutros. Me avise quando estiver tudo pronto."
- **Abertura:** `bash $S/aparelho.sh salvar && bash $S/aparelho.sh aplicar`; para o L30, `adb shell settings put system screen_off_timeout 3600000` (o `restaurar` repõe); `bash $S/memoria.sh S1 120` em segundo plano (G3). `mkdir -p etapa0/relatorios/S1` e criar `etapa0/relatorios/S1/notas.md` com a tabela `| take | nome | roteiro seguido? | o que houve | vale? |`.
- **Cada take, na ordem T01, T02 (t1, t2, t3), T03, …, T10, pausa, L30:**
  1. `bash $S/take.sh preparar <ID>`; `open -a TextEdit etapa0/textos/<ID>.txt`.
  2. Mensagem: "Take `<nome>` (ex.: `T05_giorgio_sala_t1`). Instrução: <linha da tabela do roteiro do spec>. Quando quiser: grave no QuickTime, toque em Ouvir, espere 2 s, bata uma palma, espere 2 s e leia. No fim, 3 s de silêncio e me diga **fim**." Encerrar a vez.
  3. No "fim": `bash $S/take.sh fechar <nome>`; mensagem: "Exportado. Pode parar o QuickTime e salvar como `<nome>.m4a` em `~/Tribuna-gravacoes/`. Seguiu o roteiro ou teve algo?" Encerrar a vez.
  4. Com a resposta: anotar em `notas.md`; `bash $S/medir.sh converter ~/Tribuna-gravacoes/<nome>.m4a <nome>`; depois do merge APROVADO, também `bash $S/medir.sh sinc S1 <nome>`. Se o roteiro saiu muito, o take é refeito como `t<n+1>`, sem apagar o anterior, e `notas.md` diz qual vale.
- **Conferência na tela (~10 min):** pedir ao humano que toque numa palavra visível à frente durante uma leitura do T01 e diga se o destaque saltou e a rolagem acompanhou; e que gire o celular durante a leitura e diga se pausou. Respostas em `notas.md`. **Opcional (OPR-022):** 1º parágrafo do T01 com animação de 150, 375 e 600 ms (slider do painel), e o humano diz qual pareceu melhor.
- **Fechamento:** `bash $S/aparelho.sh restaurar`; esperar o `memoria.sh` terminar ou parar com `kill %1` depois de ≥ 45 amostras, e anexar o resumo; `etapa0/relatorios/S1/oficiais.tsv` (cabeçalho `nome<TAB>variante`, uma linha por take que vale; o T02 com variante 1, 2 ou 3).
- **B4:** salvar o corpo abaixo em `$TMPDIR/tribuna-0005/b4.py` e rodar `python3 "$TMPDIR/tribuna-0005/b4.py" etapa0/trajetorias/*_giorgio_sala_*.hypotheses.csv | tee work/0005-etapa0-medicao/evidencias/b4-json.txt`. Esperado: `ruins=0` e `finais>=1` em todas as linhas.

```python
import csv, sys
for caminho in sys.argv[1:]:
    ruins = finais = 0
    with open(caminho, encoding="utf-8", newline="") as f:
        for linha in csv.DictReader(f):
            palavras = linha["words"].split()
            ruins += sum(1 for p in palavras if any(c in p for c in '{}:"') or p in ("partial", "text", "result"))
            finais += linha["kind"] == "final" and bool(palavras)
    print(f"{caminho.rsplit('/', 1)[-1]}\truins={ruins}\tfinais={finais}")
```

### Fase E (D1 e verdade-terreno; humano já autorizou a opção A)

Comandos exatos, da raiz (o Mac usa a rede só aqui):

```bash
python3 -m venv ~/.venvs/tribuna-etapa0
mkdir -p ~/.venvs/tribuna-etapa0-dl ~/vosk-modelos
~/.venvs/tribuna-etapa0/bin/python -m pip download vosk==0.3.44 --only-binary=:all: --no-deps -d ~/.venvs/tribuna-etapa0-dl
shasum -a 256 ~/.venvs/tribuna-etapa0-dl/vosk-0.3.44-py3-none-macosx_10_6_universal2.whl | tee work/0005-etapa0-medicao/evidencias/d1-vosk.txt
#   esperado: 029d0b3d6a5cff874c575b8a5814a4cccfb37608cff52e846c02a8c67a882801 (outro valor: PARAR, não instalar)
~/.venvs/tribuna-etapa0/bin/python -m pip install ~/.venvs/tribuna-etapa0-dl/vosk-0.3.44-py3-none-macosx_10_6_universal2.whl
~/.venvs/tribuna-etapa0/bin/python -m pip freeze | tee -a work/0005-etapa0-medicao/evidencias/d1-vosk.txt
~/.venvs/tribuna-etapa0/bin/python -c "import vosk, sys; print('vosk ok', sys.version)" | tee -a work/0005-etapa0-medicao/evidencias/d1-vosk.txt
curl -L --fail -o ~/vosk-modelos/vosk-model-pt-fb-v0.1.1-20220516_2113.zip https://alphacephei.com/vosk/models/vosk-model-pt-fb-v0.1.1-20220516_2113.zip
stat -f "%z bytes" ~/vosk-modelos/vosk-model-pt-fb-v0.1.1-20220516_2113.zip | tee work/0005-etapa0-medicao/evidencias/d1-modelo.txt
#   esperado: 1693530883 bytes
shasum -a 256 ~/vosk-modelos/vosk-model-pt-fb-v0.1.1-20220516_2113.zip | tee -a work/0005-etapa0-medicao/evidencias/d1-modelo.txt
unzip -q ~/vosk-modelos/vosk-model-pt-fb-v0.1.1-20220516_2113.zip -d ~/vosk-modelos/
ls ~/vosk-modelos/vosk-model-pt-fb-v0.1.1-20220516_2113 | tee -a work/0005-etapa0-medicao/evidencias/d1-modelo.txt
git status --short | tee work/0005-etapa0-medicao/evidencias/d1-git.txt
git ls-files | grep -i -E "vosk-model-pt-fb|venvs" ; echo "rc=$? (esperado 1: nada versionado)" | tee -a work/0005-etapa0-medicao/evidencias/d1-git.txt
# fumaça com o ensaio (fora do repositório):
~/.venvs/tribuna-etapa0/bin/python etapa0/ferramentas/timestamps.py "$TMPDIR/tribuna-0005/ensaio/T01_ensaio_sala_t1.wav" --modelo ~/vosk-modelos/vosk-model-pt-fb-v0.1.1-20220516_2113 | head -5
```

Se a pasta descompactada tiver outro nome, usar o que o `ls ~/vosk-modelos` mostrar e exportar `TRIBUNA_MODELO=<pasta>` para os scripts. Depois, para cada gravação da S1: `bash $S/medir.sh verdade <nome>` (`words.csv` e `expected.csv`; o stderr do `alinhar.py` vai a `evidencias/alinhar/`).

### Fase F (folha, métricas e revisão da S1)

1. Para cada gravação: `bash $S/medir.sh sinc S1 <nome>` (se ainda não), `bash $S/medir.sh folha S1 <nome>`, `bash $S/medir.sh metricas S1 <nome>`.
2. Para cada gravação, criar `etapa0/esperado/<nome>.revisao.md`:

```markdown
# Revisão de escuta — <nome>

WAV: `etapa0/gravacoes/<nome>.wav` · deslocamento: <d> ms (t_wav = t_app + d)

| # | Tempo no WAV (mm:ss,s) | O que conferir | Origem | Resposta do humano | Correção |
|---|---|---|---|---|---|
| 1 | 01:12,3 | início do improviso (primeira palavra fora do texto) | folha | | |

## Correções

| Arquivo | Antes | Depois | Motivo |
|---|---|---|---|

Revisão concluída: não
```

   Pontos: (a) cada linha `revisar:` do `evidencias/folha/<nome>.stderr.txt` e cada `aviso:`; (b) cada episódio de `salto_errado.por_posicao` e de `por_movimento`, e cada pulo com recuperação acima de 3.000 ms ou nula, do `relatorios/S1/<nome>.json` (tempos já no relógio do WAV); (c) cada faixa "sem tempo: tokens a-z" do `evidencias/alinhar/<nome>.stderr.txt` cujo intervalo de tempo encosta num episódio de (b) ou numa amostra de atraso > 3.
3. **PARAR E PEDIR AO HUMANO (H-revisão):** "Revisão da S1: são <n> pontos em <k> gravações (lista em `etapa0/esperado/*.revisao.md`). Para cada um, abra o WAV no QuickTime, vá ao tempo indicado e me diga **certo**, ou **errado: o tempo certo é mm:ss,s**. Pode ser em mais de uma vez."
4. Com as respostas: corrigir o `expected.csv` ou a folha (cada mudança vira uma linha na tabela "Correções"), rodar de novo `medir.sh metricas` e marcar `Revisão concluída: sim`. Os arquivos ficam no git, e o histórico mostra antes e depois.
5. `python3 etapa0/ferramentas/veredito.py rodada --rodada S1 --oficiais etapa0/relatorios/S1/oficiais.tsv --json etapa0/relatorios/S1/veredito.json | tee etapa0/relatorios/S1/veredito.md`.
6. **Decisão (spec, Ordem sugerida):** se CS1 a CS4 REPROVAREM de forma clara na S1 e a calibração da Fase G não mudar isso (nenhum preset ou limiar faz a gravação passar), o QA avisa o orquestrador antes de marcar a S2 e a S3, com o `veredito.md` e a tabela da calibração.

### Fase G (calibração por reprodução, sem humano)

Com o aparelho no cabo (H-cabo), `aparelho.sh salvar/aplicar` antes e `restaurar` depois. Para cada gravação ligada a CS1–CS4 da S1 (`G=` cada linha do `oficiais.tsv`):

1. **F1:** `python3 $S/reproduzir.py $G --rodada S1 --preset {conservador|normal|agil} --vocab off --estrategia novo` (para `normal` em T01 e L30, acrescentar `--painel`, que dá o G1). Depois, por reprodução: `bash $S/medir.sh sinc S1 $G reproducao <rep>`, `bash $S/medir.sh folha S1 $G reproducao <rep>` (T04, T05, T06, L30) e `bash $S/medir.sh metricas S1 $G reproducao <rep>`. Tabela: `python3 etapa0/ferramentas/veredito.py rodada --rodada S1 --oficiais etapa0/relatorios/S1/oficiais.tsv --sufixo _r_<preset>_off_novo_l0000 --config preset=<Conservador|Normal|Agil> --config audio_source=wav --relatorios etapa0/relatorios/S1/reproducoes --trajetorias etapa0/trajetorias/reproducoes --eventos etapa0/eventos/reproducoes --json etapa0/relatorios/S1/calibracao-<preset>.json | tee etapa0/relatorios/S1/calibracao-<preset>.md`. Código 1 é esperado (há NÃO MEDIDO para quem não foi reproduzido); só o 2 para.
2. **F2:** o mesmo com `--preset normal --vocab on` em T01, T02 (os três), T03, T04 e T09, e `--config vocabulary=on`; o "desligado" é a reprodução `normal` do F1. O tamanho da lista vem da linha `vocabulário: N palavras` do painel (`ui.py linhas "^vocabulário"` em Idle, logo depois de cada reprodução).
3. **F4:** `python3 etapa0/ferramentas/nivel.py $(cut -f1 etapa0/relatorios/S1/oficiais.tsv | tail -n +2) --relatorios etapa0/relatorios/S1 --json etapa0/relatorios/S1/nivel.json | tee etapa0/relatorios/S1/nivel.md`. Código 0 → recomendação 0. Código 1 com candidato `c` → reproduzir as gravações com movimento e todas as de CS1 da sala com `--limiar <c>` (o `ui.py limiar` põe o slider em `[c, c + 0,002]`; o `meta.txt` traz o valor exato) e montar a tabela com o `veredito.py … --config level_threshold=<valor do meta>`. O candidato só é recomendado se eliminar os movimentos e não reprovar o CS1 de nenhuma gravação da sala.
4. **G1:** dos `relatorios/S1/*.json` oficiais, `latencias` (latência 1, latência 2, fala → cursor, com n); das `evidencias/painel/*_r_normal_off_novo_l0000.tsv`, a latência 3 e o `beginSession` (mediana, p95, máximo, n; taxa de amostragem obtida). A latência 3 é comparada com `animation_ms=375`. Início da escuta (D16): `beginSession` do F3 + latência 1 da primeira palavra de cada gravação da S1.
5. **G5:** a latência 2 do L30 por quarto do texto (tokens 0–916, 917–1833, 1834–2750, 2751–3667), a partir das hipóteses aceitas e da trajetória (o mesmo pareamento do `metricas.py`, recortado por token), contra a do T01. Sem meta; vai ao OPR-013. Se a conta não puder ser feita só com os campos do JSON do `metricas.py`, o QA declara "não medido por quarto" e registra a latência 2 do L30 inteiro contra a do T01.
6. **F5:** para cada provisório (peso comum 0,5; margem OPR-007 b; desempate OPR-011; hífen OPR-025; fim da janela OPR-015), o relatório diz "confirmado" (todos os critérios ligados passaram) ou aponta gravação e episódio. Para o OPR-025: a latência fala → cursor dos tokens com hífen do T03 e do L30 (`bom-dia`, os do L30 achados com `grep -o -E "[[:alpha:]]+-[[:alpha:]]+" etapa0/textos/L30.txt`) contra a mediana da mesma gravação.

### Fases H e I (S2 e S3, com o humano)

Iguais às D, E (só `verdade`), F e ao F2 da G, com `voz2`/`sala` (S2) e `giorgio`/`salao` (S3), e `--rodada S2`/`S3`. Mensagens H-S2 e H-S3 trocam "sala silenciosa, você lê" por "sala silenciosa, a segunda voz lê; você é a outra pessoa no T07" e "salão com eco, você lê; a segunda pessoa ajuda". O L30 é opcional na S2 (+35 min; perguntar). **Antes da S2:** "A segunda voz precisa autorizar por escrito que a gravação dela vá ao repositório como `voz2` (Pergunta 2). Guarde a autorização fora do repositório e me confirme que a recebeu."

### Fase J (bateria, com o humano)

1. `python3 $S/ensaio.py concatenar "$TMPDIR/tribuna-0005/bateria.wav" etapa0/gravacoes/L30_giorgio_sala_t1.wav etapa0/gravacoes/T0{1..9}_giorgio_sala_t1.wav etapa0/gravacoes/T10_giorgio_sala_t1.wav` (os nomes do `oficiais.tsv` da S1; ≥ 46 min).
2. `adb push "$TMPDIR/tribuna-0005/bateria.wav" /sdcard/Android/data/io.github.gigioderico.tribuna.etapa0/files/wav/L30_bateria_sala_t1.wav`; `aparelho.sh salvar && aparelho.sh aplicar bateria`; `take.sh preparar L30`; no painel, `ui.py tocar "Atualizar WAV"` e `ui.py tocar L30_bateria_sala_t1.wav`; fechar o painel.
3. **PARAR E PEDIR AO HUMANO (H-bateria):** "Vou começar a corrida de bateria (~50 min). Quando eu disser **agora**, desconecte o cabo e deixe o celular parado, sem tocar. Depois de 47 minutos no seu relógio, reconecte o cabo e me diga **reconectei**." Na mesma mensagem, logo depois: `adb shell dumpsys battery | grep -E "level|Charge counter|temperature|status|USB powered"` em `evidencias/g4-bateria.txt` com `date`, `adb shell settings get system screen_brightness`, e `python3 $S/ui.py tocar Ouvir`. Então escrever "**agora**" e encerrar a vez.
4. No "reconectei": `date` e `dumpsys battery` de novo (logo, antes de a carga subir), `ui.py tocar Depuração`, `ui.py tocar Parar`, `take.sh fechar L30_bateria_sala_t1 ensaio` (a exportação não vai ao repositório; só a duração do `level.csv` entra na evidência), `aparelho.sh restaurar`. Esperado: duração ≥ 45 min (relógio do Mac e `level.csv`), estado "não carregando" no começo da corrida (registrado logo depois de desconectar, se o adb ainda responder; senão, declarar). Se a leitura parou antes (tela apagou), refazer.

### Fase K (confirmação e lacunas)

Se a calibração recomendar configuração diferente da D10 (D28): PARAR E PEDIR AO HUMANO uma sessão curta (T01, T05, T06, T07, ~25 min) com ela, tratada como as rodadas, com `--config` no veredito; o L30 só por reprodução. Sem a sessão, o relatório diz que a recomendada só foi medida por reprodução. Tablet, aparelho de entrada e intermediário: lacuna declarada.

### Fase L (relatório e entrega)

1. `python3 etapa0/ferramentas/veredito.py final etapa0/relatorios/S1/veredito.json etapa0/relatorios/S2/veredito.json etapa0/relatorios/S3/veredito.json --json etapa0/relatorios/final.json | tee etapa0/relatorios/final.md`.
2. `etapa0/relatorios/etapa0-relatorio-final.md` no formato D26/H1/H2. Primeira tabela: os oito critérios de saída. CS1–CS4 vêm do `final.md`; CS5 do `a2-16k.txt` com os caminhos a–d do OPR-024; CS6 de G1–G4 com a regra (mediana, p95, máximo, n) e o rótulo "aparelho topo de linha"; CS7 de F1 e F4; CS8 de F2 e F3, com as regras do D12. Seções: Decisões do TRD, Calibração, Metas propostas, Lacunas, Interpretações pendentes (OPR-032, 033, 034, com quantas amostras cada uma muda, a partir de `janelas_toque`, `movimentos_por_classe` e `pulos`), 16 KB e Recomendação. Se algum de CS1–CS4 reprovar depois da calibração, o QA pede ao arquiteto a seção "Alternativas de motor" antes de fechar.
3. `test_result.md` no template, apontando para o relatório e as evidências, com os desvios de A15, o que não foi medido e por quê, e a linha do consentimento da `voz2`.
4. `registers/opr_register.md`: atualizar OPR-007 b, 010, 011, 013, 015, 020, 024, 025, 033 com o resultado; levar 022, 024, 032, 033 e 034 ao humano pelo relatório; achados novos com dono.
5. Limpeza: `adb shell rm -r /sdcard/Android/data/io.github.gigioderico.tribuna.etapa0/files/wav /sdcard/Android/data/io.github.gigioderico.tribuna.etapa0/files/exports`; apagar `$TMPDIR/tribuna-0005/` com `rm -r` (fora do repositório; pedir ao humano se o sandbox negar). O venv e o modelo grande ficam na máquina do humano (ele decide se apaga).
6. H4: `git status --short` (só `etapa0/{gravacoes,trajetorias,eventos,esperado,relatorios}`, `work/0005-etapa0-medicao`, `registers`, memórias de agente); `git diff main --stat -- etapa0/core/src/main etapa0/app docs/` vazio; `git ls-files | grep -i -E "vosk-model-pt-fb|venvs"` vazio.
7. **PARAR E PEDIR AO HUMANO (portão condicional, Pergunta 2):** "Os WAV somam <du -ch> no LFS. Cabe na cota da conta? E a autorização da segunda voz foi recebida? Só faço o push com as duas respostas." Com o sim: `git lfs ls-files | tee …/c3-lfs.txt` e o push do branch.
8. Ao orquestrador: relatório pronto para o **portão humano 3**.

---

## Portão automático do plano (checklist do PROCESS.md, conferida pelo arquiteto antes de entregar)

- [x] Cada critério do spec (A1–H5) tem passo e comando de verificação (tabela "Critérios do spec → passo → comando").
- [x] Testes antes do código: Tarefas 1–4 com "ver falhar" (`ModuleNotFoundError`) antes da ferramenta; a Tarefa 5 é de integração e está declarada.
- [x] Seção de privacidade preenchida (texto, áudio, logs do aparelho, dependências, rede, Q6).
- [x] Nenhuma assinatura de contrato muda (`git diff` em `etapa0/core/src/main`, `etapa0/app` e `docs/` fica vazio por construção; H4 confere).
- [x] Nenhuma permissão nova no manifesto (o app não muda; B2 confere o instalado).
- [x] Portões humanos explícitos: portão 6 já respondido (A, só no passo D1); portão 6 condicional (cota do LFS e consentimento) antes do push; portão 3 no fim. As paradas físicas (sessões, escuta, bateria, desbloqueio) são execução do roteiro do spec, não portões.
- [x] Parâmetros: nenhum número novo de produto. As constantes das ferramentas vêm do spec (200 ms, ±200 ms), do código da 0003 (3.200 amostras, 16 kHz), do formato (1/32768) ou do roteiro (palma até 4 s). Busca local de 1 s, grade de 100/10 ms e empate de `1e-9` são escolhas de método, sem efeito no veredito além da resolução declarada, e estão provadas nos testes.
- [x] Divergências registradas: A12 (OPR-041 para o analista), A7 (versão do `vosk`), A10 (nome das reproduções).

---

## Registro da construção (preenchido pelo agente de construção)

**Agente:** motor-voz · **Commit(s):** <hash>

- Feito: <...>
- Testes criados: <...>
- Desvios do plano e motivo: <nenhum | ...>
- Comandos rodados e resultado: <...>
