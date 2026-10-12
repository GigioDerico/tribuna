# Spec — 0005-etapa0-medicao

**Autor:** analista · **Data:** 2026-10-12 · **Aprovação:** conferida pelo orquestrador em 2026-10-12, com ressalva: o portão humano 6 (verdade-terreno, passo D1) fica separado e só bloqueia esse passo; plano, preparação e gravações andam sem ele. A tarefa tem um portão humano 6 (Pergunta 1). Ele precisa de resposta antes do passo D (verdade-terreno), mas não bloqueia o PLAN, a preparação nem as gravações. O resultado final vai ao portão humano 3.

Brainstorm: `work/0005-etapa0-medicao/brainstorm.md`. As decisões D1–D28 citadas abaixo estão lá, com as alternativas e o motivo.

## O que e por quê

Medir no aparelho real se o Vosk, o reconciliador e o alinhador da etapa0 acompanham a leitura, contra os 8 critérios de saída da Etapa 0 (TRD, Etapa 0). Também calibrar o que o painel deixa calibrar e decidir vocabulário restrito e estratégia do reconhecedor. Tudo termina num relatório que leva o humano à decisão do portão 3: aprovar a arquitetura e seguir para o app, ou trocar o motor atrás do `SpeechEngine` (PROCESS.md, Etapa 0, item 5). Para o orador, nada muda ainda. Esta tarefa decide se o modo Voz que ele vai usar está de pé.

## Requisitos atendidos

- PRD: R1, R4 (presets medidos, tabela intacta), R5 (latência 3 medida; o padrão continua em aberto, OPR-022), R7 (`Level`); Motor de acompanhamento (tabela de sensibilidade, palavras comuns, "na dúvida, espera", recuperação, Vocabulário restrito, Robustez: limiar de nível); Requisitos não funcionais (Desempenho, Bateria, Memória, Offline, Privacidade); Critérios de aceite do MVP (pulo em até 3 s, 30 s de silêncio, pausa com alguém falando, toque 3 parágrafos à frente), medidos aqui só no nível do motor.
- TRD: Motor de voz (Captura e sessões, Alinhamento 2–6, Vocabulário restrito); Metas de desempenho e Protocolo de medição; Testes (Privacidade nos testes, Qualidade de código: 16 KB); Distribuição (R8); Ciclo de vida (Manifesto, Segundo plano); Etapa 0 (O que testar e Critérios de saída); Riscos e decisões em aberto (vocabulário, reconhecedor por sessão, valores finais das metas).
- `docs/TESTES-ETAPA0.md` e `docs/TESTES-ETAPA0-L30.md`: todo o protocolo.
- `work/0003-etapa0-vosk/spec.md`, seção E: tudo o que era "só no aparelho". `work/0004-etapa0-ferramentas/spec.md`: as ferramentas e os formatos.
- Registro: mede ou decide OPR-007 (b), 010, 011, 013, 015, 020, 024, 025 e 033; leva ao humano OPR-022, 024, 032, 033 e 034; evita OPR-038 no roteiro; entrega à 0006 (OPR-008, 019) os WAV e as transcrições.
- Contratos, `align/`, o app de etapa0 e o manifesto não mudam. Os números da tabela do PRD também não.

### Interpretações e valores que este spec fixa (do brainstorm)

- **Trajetória oficial = microfone ao vivo no S23** (D1). O WAV de referência é gravado ao mesmo tempo pelo MacBook ao lado do aparelho (QuickTime, qualidade Máxima) e convertido com `afconvert` para 16 kHz, mono, PCM 16 bits (D2). As reproduções do mesmo WAV pela fonte WAV da 0003 servem para calibrar.
- **Sincronização** (D3): correlação do `level.csv` com o envelope RMS do WAV em blocos de 200 ms, conferida pela palma. A resolução é de ±200 ms (um bloco). Uma gravação cujo veredito muda dentro de ±200 ms, ou cuja deriva entre o começo e o fim passa de 200 ms, sai "sensível ao deslocamento" e não conta como PASSOU.
- **Folha de eventos** (D6): `<nome>.folha.events.csv`, derivada dos dados no relógio do app. O `events.csv` do app traz pausa e toque.
- **Revisão manual** (D7): lista de pontos de escuta. Correções com registro (antes, depois, motivo), e o veredito só depois delas.
- **Agregação** (D9): por gravação, e toda gravação ligada a um critério tem de passar.
  - CS1 (atraso): sala silenciosa; T01, T10 e L30; cada voz.
  - CS2 (saltos errados e recuos): L30 nos dois ambientes; T02, T03, T04, T08 e T09.
  - CS3 (recuperação): T05 e o pulo do L30.
  - CS4 (sem movimento): T06, T07 e os eventos dos minutos 12 e 18 do L30.
  - CS2 a CS4 valem nos dois ambientes.
  - Sem gravação, ou com zero amostras, o critério sai NÃO MEDIDO.
- **Configuração oficial** (D10): Normal, vocabulário desligado, reconhecedor novo, limiar 0, animação 375 ms (PROVISÓRIO).
- **Regras de recomendação** (D11, D12): nenhum número novo. Os parâmetros fora do painel (peso comum 0,5, margem de ambiguidade, desempate, hífen) são só confirmados ou diagnosticados.
- **Metas** (D13): o valor proposto é o medido no S23, com a regra de contagem e o rótulo "aparelho topo de linha".
- **Nomes e pastas** (D20): `<ID>_<voz>_<ambiente>_t<n>`, com voz `giorgio` ou `voz2` e ambiente `sala` ou `salao`.

## Quem faz o quê

### (a) O agente sozinho, por comando

Build e testes de base; instalação por `adb install`; ajustes do aparelho por adb (modo avião, Não perturbe, brilho, tela ligada), restaurados no fim (D18); smoke test (abertura, diálogo de permissão por `uiautomator` e `input tap`, cópia e carga do modelo); verificações no aparelho de D24; ensaio com áudio sintético (D21); antes e depois de cada take, escolher o texto, digitar o nome da exportação e conferir a configuração por adb; `adb pull` das exportações; conversão dos WAV; sincronização; folha de eventos; `timestamps.py`, `alinhar.py` e `metricas.py`; reproduções de calibração por fonte WAV, dirigidas por adb; frio, memória, `beginSession`, latência 3; relatórios.

### (b) O humano, fisicamente (roteiro abaixo, com tempos)

Ler os textos em voz alta; conseguir a segunda voz e o consentimento dela; conseguir o salão; ser a "outra pessoa" no T07 e no L30; tocar no aparelho nos pontos do roteiro; gravar no QuickTime; ouvir os pontos da revisão; desconectar e reconectar o cabo na corrida de bateria; conferir na tela o destaque e a rolagem; tablet, quando houver.

### (c) Portões humanos

1. **Portão 6 (Pergunta 1), antes do passo D:** instalar o `vosk` (PyPI) num venv fora do repositório e baixar o modelo grande de português (GPLv3, fora do repositório). A alternativa sem instalação está em D4 (B), também com decisão humana.
2. **Portão 6 condicional (Pergunta 2), antes do push dos WAV:** cota do LFS e consentimento da segunda voz.
3. **Portão 3:** o relatório final.
4. **Portão 2:** só aparece como recomendação no relatório (padrão do R5; qualquer número da tabela de sensibilidade). Esta tarefa não o abre.

## Critérios de aceite

Os comandos rodam da raiz do repositório, salvo indicação. A saída de cada um vai anexada em `work/0005-etapa0-medicao/evidencias/`. Os globs vão entre aspas (OPR-035). "Aparelho" = S23 SM-S911B (RQCW80BPXKN).

### A. Base e instalação (agente, por comando)

- [ ] A1. `cd etapa0 && ./gradlew testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest assembleDebug assembleRelease` termina verde, e `python3 etapa0/ferramentas/testar.py` termina com 0, sem `skip`. As contagens ficam registradas. O commit medido aparece em todo relatório (Q8).
- [ ] A2. `./gradlew verify16kAlignment` roda contra o release, e a saída real fica anexada. O resultado esperado é o de OPR-024: reprova só as `.so` de 32 bits. Isso não reprova a 0005 e vai ao relatório final (CS5) com os caminhos a–d do OPR-024.
- [ ] A3. `adb install` do APK de debug. O SHA-256 do APK puxado do aparelho (`adb shell pm path` e `adb pull`) é igual ao do APK construído. `getprop` (modelo, Android, ABI), `getconf PAGE_SIZE` e `MemTotal` do aparelho ficam registrados.
- [ ] A4. Ajustes do aparelho (D18): os valores de antes e de depois de modo avião, Não perturbe, brilho e tela ligada ficam em `evidencias/aparelho-ajustes.txt`, e todos são restaurados ao fim da tarefa.

### B. Verificações no aparelho (agente, por comando; 0003 seção E)

- [ ] B1. **Permissão e modelo:** depois de `pm clear`, abrir o app mostra o diálogo de microfone (`uiautomator dump` contém o diálogo). Depois de permitir, o painel chega a modelo pronto. `install_ms` e `prepare_ms` ficam registrados. O `logcat -d` da sessão não tem `FATAL EXCEPTION`.
- [ ] B2. **Manifesto instalado:** `adb shell dumpsys package <pacote>` lista só `android.permission.RECORD_AUDIO` entre as permissões pedidas.
- [ ] B3. **Captura:** durante uma leitura ao vivo, `adb shell dumpsys media.audio_flinger` mostra a trilha de entrada do pacote em 16.000 Hz, 1 canal, PCM 16 bits. A mediana do intervalo entre linhas do `level.csv` fica registrada ao lado (o bloco é de 200 ms).
- [ ] B4. **JSON real (Q4, 0003 C6):** em todo `hypotheses.csv` da S1, nenhuma palavra contém `{`, `}`, `:` ou aspas internas, e nenhuma é igual a `partial`, `text` ou `result`. Há pelo menos um `final` com palavras em cada gravação. A contagem fica na evidência.
- [ ] B5. **Privacidade (regras 2 e 3):**
  - A listagem recursiva de `filesDir` (por `run-as`) e do diretório externo do app, antes e depois de uma leitura ao vivo, não mostra arquivo novo com extensão de áudio (`.wav`, `.pcm`, `.raw`, `.m4a`, `.3gp`, `.amr`, `.ogg`) nem fora de `exports/`.
  - O `logcat -d` da sessão não contém nenhuma de 10 palavras distintivas dos textos lidos (lista na evidência; por exemplo "rodeios", "conselhos", "estiagem").
- [ ] B6. **Segundo plano e rotação (TRD Segundo plano):**
  - Com a leitura ao vivo, `input keyevent KEYCODE_HOME` deixa a leitura pausada, e o `dumpsys audio` não mostra captura ativa do pacote.
  - Girar por adb (rotação de usuário com a automática desligada) mantém a leitura sem pausa (painel por `uiautomator`) e o mesmo `SessionId`.
- [ ] B7. **Release com R8 (0003 D19):** com o APK de release instalado (chave de debug), a reprodução do T01 pela fonte WAV carrega o modelo e termina com o cursor no último token do T01 (`trajectory.csv` exportado). O debug é reinstalado depois.
- [ ] B8. **Ensaio (D21):** se houver voz pt-BR no `say` (saída de `say -v '?'` anexada), o T01 sintético rodado como WAV no app dá deslocamento 0 ± 200 ms na ferramenta de sincronização. Sem voz pt-BR, isso fica registrado, e o mesmo teste roda com a primeira gravação da S1 reproduzida (deslocamento esperado 0 ± 200 ms). O teste de toque em palavra fora da tela (D23) fica registrado com o resultado.

### C. Gravações (dependem do humano; conferência por comando)

- [ ] C1. Para cada take das rodadas S1, S2 e S3 (D8) existe o conjunto completo:
  - `etapa0/gravacoes/<nome>.wav`;
  - `etapa0/trajetorias/<nome>.{trajectory,hypotheses,level}.csv` e `<nome>.meta.txt`;
  - `etapa0/eventos/<nome>.events.csv`.

  Uma checagem por script lista os conjuntos faltantes. Um take refeito ganha `t<n+1>` sem apagar o anterior, e a folha de notas diz qual vale.
- [ ] C2. Todo WAV é 16 kHz, mono, PCM 16 bits (cabeçalho lido por script) e tem nome no padrão do `timestamps.py`. O `text_id` de cada `meta.txt` é igual ao `<ID>` do nome, e `audio_source=mic` nas oficiais. A configuração das oficiais é a de D10 (`preset=Normal`, `vocabulary=off`, `recognizer_strategy` = novo, `level_threshold` = 0, `animation_ms=375`).
- [ ] C3. O `git lfs ls-files` lista todos os WAV. O tamanho total dos WAV fica registrado antes do push, e o push só acontece depois da resposta à Pergunta 2.

### D. Verdade-terreno e sincronização (o passo D depende da Pergunta 1)

- [ ] D1. **Opção A (recomendada):** o `vosk` foi instalado num venv fora do repositório, e a versão fica registrada. O modelo grande fica numa pasta fora do repositório, com nome, tamanho e SHA-256 do zip registrados. `git status` e `git ls-files` não mostram nada do modelo nem do venv. Cada `esperado/<nome>.words.csv` sai de `<venv>/bin/python etapa0/ferramentas/timestamps.py <wav> --modelo <pasta>` com código 0.
  - *Se o humano escolher a opção B, D1 passa a ser:* `words.csv` gerado no aparelho com o modelo pequeno (tarefa própria, com plano e revisão), texto do passo 1 do TESTES-ETAPA0 atualizado com aprovação humana, e a cobertura (fração de tokens com tempo) por gravação no relatório.
- [ ] D2. Cada `esperado/<nome>.expected.csv` sai do `alinhar.py` com código 0. O stderr (faixas inseridas) fica anexado.
- [ ] D3. A ferramenta de sincronização (D3) e a de folha de eventos (D6) existem em `etapa0/ferramentas/`, só com a stdlib, com teste no `testar.py` (sinais e roteiros sintéticos) e revisão APROVADA antes de qualquer número delas entrar num relatório. Pelo menos uma quebra proposital de cada uma deixa um teste vermelho (Q2).
- [ ] D4. Para cada gravação oficial ficam registrados: o deslocamento (ms), o pico da palma no WAV e no `level.csv`, e a deriva (deslocamento do começo − deslocamento do fim).
- [ ] D5. Cada `eventos/<nome>.folha.events.csv` usa só os eventos válidos do TESTES-ETAPA0, no relógio do app. Existe para T04, T05 e T06 e para o L30, e o `metricas.py` a aceita (não sai com 2).
- [ ] D6. Cada gravação tem `esperado/<nome>.revisao.md` com os pontos de escuta (D7), a resposta do humano a cada um e o registro das correções (antes, depois, motivo). Nenhum REPROVA de salto errado ou de recuperação sai sem o ponto ouvido.

### E. Métricas e veredito (agente, por comando)

- [ ] E1. Para cada gravação oficial, `python3 etapa0/ferramentas/metricas.py <trajetória> <esperado> <events.csv> [<folha>] --deslocamento-ms <D4> --hipoteses <hipóteses> --json <arquivo>` roda. O relatório e o JSON ficam em `etapa0/relatorios/<rodada>/`, e o código de saída fica registrado. As corridas com deslocamento −200 e +200 ms também ficam registradas (D3).
- [ ] E2. O resumo de cada rodada tem, por gravação e por critério ligado (D9):
  - o número;
  - a regra de contagem;
  - o número de amostras de leitura normal, de episódios e de eventos (Q3, Q7);
  - PASSOU, REPROVOU, NÃO MEDIDO ou SENSÍVEL AO DESLOCAMENTO.

  Um critério com zero amostras ou zero eventos sai NÃO MEDIDO.
- [ ] E3. Cada salto errado traz a classe ("alcance" ou "além do esperado", OPR-033) e a resposta da revisão. As amostras da janela de toque (OPR-032) e da regra de recuperação (OPR-034) aparecem contadas à parte.

### F. Calibração e decisões (agente, por reprodução do WAV; D11, D12)

- [ ] F1. **Presets:** as reproduções com Conservador, Normal e Ágil (vocabulário desligado, reconhecedor novo, limiar 0) cobrem todas as gravações ligadas a CS1–CS4 da S1, e os números ficam numa tabela por preset. O `meta.txt` de cada reprodução confirma a configuração e `audio_source=wav`. OPR-010 recebe o resultado.
- [ ] F2. **Vocabulário:** reprodução com vocabulário ligado e com desligado (preset Normal) em T01, T02 (os três takes), T03, T04 e T09 da S1 e da S2. A tabela mostra os critérios ligados lado a lado e o tamanho da lista (painel). A recomendação segue D12.
- [ ] F3. **Estratégia:** com reconhecedor novo e com `reset()`, 10 ciclos Pausar → Ouvir cada, por adb. O `beginSession` de cada ciclo (painel por `uiautomator`) e o início da escuta (D16) ficam registrados: mediana, p95, máximo, n. A recomendação segue D12, com a meta de 2 s do TRD.
- [ ] F4. **Limiar:**
  - Uma tabela traz, por gravação, os movimentos automáticos dentro de silêncio, pausa e improviso com limiar 0, e as estatísticas do `level.csv` nesses intervalos e na leitura (mediana, p95).
  - Se houver movimento, o valor candidato de D12 é reproduzido. A tabela mostra que ele elimina os movimentos e o CS1 de cada gravação da sala com ele. Sem movimento, a recomendação é 0.
- [ ] F5. **Provisórios fora do painel:** o relatório diz, para cada um (peso comum 0,5; margem de ambiguidade, OPR-007 b; desempate, OPR-011; hífen, OPR-025; fim da janela, OPR-015), se fica confirmado (todos os critérios ligados passaram) ou qual gravação e qual episódio apontam para ele. No segundo caso, o achado vira OPR com dono, ou tarefa nova de recalibração com o golden. Para o OPR-025: atraso medido nos tokens com hífen do T03 e do L30 contra a mediana do atraso da mesma gravação.
- [ ] F6. **Configuração recomendada diferente da oficial (D28):** há uma sessão curta ao vivo (T01, T05, T06, T07) com ela e o L30 em reprodução, ou o relatório diz explicitamente que ela só foi medida por reprodução.

### G. Medições para as metas (agente, por comando; CS6)

- [ ] G1. **Latências:** latência 1, latência 2, fala → cursor e total (do `metricas.py`), mais latência 3 e `beginSession` (painel, amostragem por `uiautomator` de pelo menos 1 por segundo numa reprodução do T01 e do L30), com mediana, p95, máximo e n por gravação da S1. A latência 3 vem comparada com o `animation_ms` (meta fixa do TRD: igual à duração R5).
- [ ] G2. **Inicialização a frio:** 10 corridas de `am force-stop` e `am start -W`, com `TotalTime` e `prepare_ms`. Primeira cópia: 3 corridas com `pm clear`, com `install_ms`. Mediana, máximo e n.
- [ ] G3. **Memória:** `dumpsys meminfo <pacote>` a cada 60 s durante 45 min ou mais de processo vivo na S1, com o pico, a mediana de PSS e RSS e o n de amostras.
- [ ] G4. **Bateria:** corrida de D14, com `dumpsys battery` (nível, temperatura, estado de carga = desconectado) no começo e no fim, horários de início e fim, duração ≥ 45 min, brilho no máximo (valor de `settings get system screen_brightness`) e a lista dos WAV tocados.
- [ ] G5. **Custo do alinhamento (OPR-013):** a latência 2 do G1 no L30, separada da do T01, para mostrar se ela cresce ao longo do texto. Sem meta; vai para o OPR-013.

### H. Relatório final e entrega

- [ ] H1. `etapa0/relatorios/etapa0-relatorio-final.md` existe, no formato de D26. A primeira tabela traz os 8 critérios de saída do TRD, cada um com número, regra, n, gravações e PASSOU, REPROVOU ou NÃO MEDIDO, e a evidência ao lado. Nenhuma linha fica sem resultado.
- [ ] H2. O relatório tem as seções: Decisões do TRD (vocabulário, estratégia), Calibração (preset, limiar, provisórios), Metas propostas (D13), Lacunas do protocolo (aparelho de entrada, intermediário, tablet; segunda voz e salão, se faltarem), Interpretações pendentes (OPR-032, 033 e 034, com o número de amostras que cada uma muda), 16 KB (OPR-024) e Recomendação.
  - A Recomendação é uma de três: aprovar, aprovar com condições listadas, não aprovar.
  - Se algum dos CS1–CS4 reprovar depois da calibração, a seção "Alternativas de motor" é pedida ao arquiteto e o relatório só fica completo com ela (TRD, Etapa 0).
- [ ] H3. `work/0005-etapa0-medicao/test_result.md`, no template, aponta para o relatório e as evidências, e diz o que não pôde ser medido e por quê.
- [ ] H4. Ao fim, `git status` não tem arquivo fora de `etapa0/{gravacoes,trajetorias,eventos,esperado,relatorios,ferramentas}/`, `work/0005-etapa0-medicao/`, `registers/` e memórias de agente. Nada do modelo grande nem do venv fica no repositório. `git diff main --stat -- etapa0/core/src/main etapa0/app docs/` não mostra nada.
- [ ] H5. Os achados novos foram para `registers/opr_register.md` com dono, e os OPR medidos aqui (lista em "Requisitos atendidos") têm a linha atualizada com o resultado.

## Roteiro do humano

**Antes de tudo (uma vez, ~15 min, sem pressa):**
- Responder às Perguntas 1 e 2.
- Combinar a segunda voz (de preferência com timbre bem diferente) e o salão.
- Deixar à mão um suporte que segure o celular a ~50 cm da boca e o MacBook ao lado dele.
- Ter um cronômetro para o silêncio de 30 s.
- Se quiser ler do papel, imprimir os textos de `etapa0/textos/` (não precisa: o agente abre cada texto no Mac em fonte grande).
- O improviso (T04 e L30) e a fala da outra pessoa (T07) só podem ter conteúdo inventado e neutro, porque vão para os WAV.

**Toda sessão começa igual (~10 min, a maior parte do agente):**
1. Ligar o celular ao Mac pelo cabo. O agente instala, põe em modo avião e Não perturbe, fixa o brilho e mantém a tela ligada. Se alguma coisa não der por adb, ele avisa o que tocar.
2. Pôr o celular no suporte a ~50 cm, de frente. O MacBook fica ao lado, à mesma distância.
3. No QuickTime Player, Arquivo → Nova Gravação de Áudio, microfone interno, qualidade **Máxima**.

**Cada take (~2 min de preparo e fechamento, mais a leitura):**
1. O agente escolhe o texto e o nome no app e mostra o texto no Mac. Espere ele dizer "pronto".
2. Clique em gravar no QuickTime.
3. Toque em **Ouvir** no celular.
4. Espere ~2 s, **bata uma palma** e espere mais ~2 s.
5. Leia o texto em ritmo natural, olhando para o Mac ou o papel, com a instrução do take (tabela abaixo).
6. No fim, fique ~3 s em silêncio e toque em **Parar**. O agente exporta e confere.
7. Pare o QuickTime e salve como `<nome>.m4a` (o agente diz o nome) na pasta combinada.
8. Diga ao agente: "seguiu o roteiro" ou "teve X" (tosse, tropeço, erro, ruído). Se saiu muito do roteiro, refaça o take; o anterior não é apagado.

| Take | Instrução | Leitura |
|---|---|---|
| T01 | Leitura normal | ~2 min |
| T02 t1, t2, t3 | Referências como "três dezesseis" (t1), "três e dezesseis" (t2), "capítulo três, versículo dezesseis" (t3); números por extenso | 3 × ~1,5 min |
| T03 | Normal; o refrão aparece três vezes | ~1,5 min |
| T04 | Depois de "...entrar na conversa.", improvisar ~2 min sobre o tema (exemplo inventado) e voltar ao 2º parágrafo | ~4 min |
| T05 | Do fim do 1º parágrafo direto para o 3º (pular "Isso tem um custo...") | ~2 min |
| T06 | Depois de "...muitos rodeios.", 30 s em silêncio total (cronômetro), sem tocar | ~2,5 min |
| T07 | Depois de "...levada a sério.", tocar em **Pausar**; a outra pessoa fala ~10 s perto do celular; tocar em **Ouvir** e seguir | ~2,5 min |
| T08 | Repetir logo depois de dizer: "Isso tem um custo." e "Um momento de pausa não precisa ser preenchido." | ~2 min |
| T09 | Normal | ~1 min |
| T10 | 1º e 2º parágrafos bem devagar, com pausas longas; os três últimos rápido, sem pausas | ~3 min |
| L30 | Seguir as linhas `//`: referências (min 3); improviso de 1 min (min 8); **Pausar**, 20 s de ruído de salão feito pela outra pessoa, **Ouvir** (min 12); pular o parágrafo "Um detalhe prático" (min 15); 30 s de silêncio (min 18); depois de "como uma ponte velha", parar de falar, rolar o texto no celular até "Imagine", tocar nela e seguir dali (min 22); repetir "Ninguém chega longe de uma vez só." (min 25); ler até o fim (min 28–30). Não tocar em palavra com a leitura pausada. | ~30 min |

**Sessões:**

| Sessão | Quem | Onde | Conteúdo | Tempo estimado |
|---|---|---|---|---|
| S1 | Giorgio lê; a segunda pessoa ajuda no T07 e no L30 | Sala silenciosa | Todos os takes acima; depois, conferência na tela (~10 min): tocar numa palavra visível à frente e ver o destaque saltar e a rolagem acompanhar; girar o celular durante a leitura e ver que não pausa. Opcional (~6 min): ler o 1º parágrafo do T01 com a animação em 150, 375 e 600 ms e dizer qual pareceu melhor (OPR-022). | ~2 h, com uma pausa entre os curtos e o L30 |
| S2 | Segunda voz lê; Giorgio é a outra pessoa no T07 | Sala silenciosa | T01–T10 (L30 opcional, +35 min) | ~1 h 10 |
| S3 | Giorgio lê; a segunda pessoa ajuda | Salão com eco | T01–T10 e L30 | ~2 h, mais o deslocamento |
| Bateria | Giorgio, quase sem fazer nada | Qualquer lugar | O agente prepara; você desconecta o cabo, toca **Ouvir** quando cada WAV terminar (o agente diz a ordem antes) e reconecta depois de 45 min | ~50 min |
| Revisão | Giorgio | No Mac | Ouvir os pontos da lista de cada gravação (o agente dá o arquivo e o minuto) e responder certo ou errado | ~30–45 min por rodada, em uma ou mais vezes |
| Confirmação | Giorgio | Sala | Só se a configuração recomendada mudar: T01, T05, T06, T07 | ~25 min |
| Tablet | Giorgio | Sala | Quando houver tablet: T01, T05, T06, T07 e L30 | ~1 h |

Total sem tablet nem confirmação: perto de 7 h a 8 h do humano, em 4 ou 5 encontros. O agente trabalha entre eles sem o humano (conversão, métricas, reproduções).

**Ordem sugerida:** Perguntas 1 e 2 → ensaio do agente (sem humano) → S1 → revisão da S1 → S2 → S3 → bateria → revisões → relatório. A S1 sozinha já mostra se faz sentido seguir com as outras: se a S1 reprovar de forma clara depois da calibração, o agente avisa o orquestrador antes de gastar a S2 e a S3.

## Fora do escopo

- Qualquer mudança em `align/`, no app de etapa0, em contrato do TRD, no manifesto, no `metricas.py`, no `alinhar.py` ou no `timestamps.py`. Se a opção B da Pergunta 1 vier, o código dela é tarefa própria.
- Mudar números da tabela de sensibilidade do PRD, o padrão do R5 ou o texto do TESTES-ETAPA0 (OPR-032, 034). Ficam como recomendação, para decisão humana.
- Recalibrar peso comum, margem, desempate ou hífen em código (tarefa nova com o golden, se o relatório apontar).
- Golden de transcrições como suíte de regressão e teste instrumentado "Integração de voz" (0006; OPR-008, OPR-019).
- Medições que o TRD liga ao app: 60 fps com 8.000 palavras (macrobenchmark), toque para pausar < 100 ms, telas, contagem regressiva, "Fim".
- Comprar ou pedir aparelho de entrada, intermediário ou tablet (portão 6). A falta deles entra como lacuna no relatório.
- Recompilar o Vosk para 16 KB (portão 6; OPR-024).
- Usar discurso pessoal, gravar áudio pelo app, ligar a rede no aparelho, ou pôr o modelo grande ou o venv no repositório.

## Mudança proposta no PRD (se houver)

Nenhuma nesta tarefa. O relatório pode recomendar ao portão 2 o padrão do R5 (OPR-022) e mudanças na tabela de sensibilidade, com os dados.

## Perguntas em aberto

1. **Portão 6, verdade-terreno (D4, D5). Responder antes do passo D; não bloqueia o plano nem as gravações.**
   - **(A) Recomendada:** autorizar a instalação do pacote `vosk` (PyPI), em versão fixada, num venv fora do repositório, e o download do modelo grande de português do Vosk (GPLv3; ordem de 1,5 GB pelo que o analista conhece, com nome e tamanho exatos conferidos em alphacephei.com/vosk/models no dia), numa pasta fora do repositório. O modelo nunca entra no app nem no git. Mantém o protocolo do TESTES-ETAPA0 como está, com uma verdade independente do modelo do app.
   - **(B) Sem instalar nada:** gerar os tempos de palavra no próprio aparelho com o modelo pequeno (tarefa nova de código na etapa0, com plano e revisão) e mudar o passo 1 do TESTES-ETAPA0. Custo: a verdade passa a usar o mesmo modelo que é medido. Onde ele erra, o atraso sai otimista, e o relatório mostra o tamanho desse viés.
   - Usar a transcrição do próprio app ou marcar as palavras à mão não vale (D4, C e D).
2. **Portão 6 condicional e consentimento (D22). Responder antes do push dos WAV.**
   - Os WAV das três rodadas somam perto de 300 MB no Git LFS. Confirmar que isso cabe na cota da conta, ou dizer onde guardar.
   - A segunda voz precisa autorizar por escrito que a gravação dela (textos de teste, improviso inventado) vá ao repositório, sob o pseudônimo `voz2`.
