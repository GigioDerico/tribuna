# Brainstorm — 0005-etapa0-medicao

**Autor:** analista · **Data:** 2026-10-12 · Skill `superpowers:brainstorming`, com as perguntas respondidas pelo próprio analista (PROCESS.md, Intake). Termina aqui: o plano é do `arquiteto`.

## Contexto lido

- `PROCESS.md` (Etapa 0, item 5; portões humanos 2, 3 e 6), `CLAUDE.md` (regras 1 a 3, "Comando antes de opinião", "Não invente parâmetros").
- `docs/TESTES-ETAPA0.md` e `docs/TESTES-ETAPA0-L30.md`: textos, protocolo, folha de eventos, trajetória esperada (passos 1 a 4), métricas, layout de `etapa0/`.
- `docs/TRD.md`: Motor de voz (Captura e sessões, Alinhamento, Vocabulário restrito), Metas de desempenho, Protocolo de medição, Testes, Etapa 0 (O que testar, Critérios de saída), Riscos e decisões em aberto.
- `docs/PRD.md`: R4, R5, tabela de sensibilidade, Robustez, Requisitos não funcionais, Critérios de aceite do MVP.
- `work/0003-etapa0-vosk/spec.md` e `brainstorm.md` (D3–D13, D18, D19, seção E), `work/0004-etapa0-ferramentas/spec.md` e `impl_plan.md` (A5–A8).
- Código da etapa0, só para conferir fatos: exportação em `getExternalFilesDir("exports")` com `hypotheses.csv`, `level.csv`, `trajectory.csv`, `events.csv` e `meta.txt` (chaves de `ExportFiles.META_KEYS`); WAV de fonte em `getExternalFilesDir("wav")`; bloco de áudio de 3.200 amostras (200 ms); `AudioSource.VOICE_RECOGNITION`; latência 2, latência 3, `beginSession` e "1º parcial" só no painel, não exportados; texto em `verticalScroll` com `animateScrollTo`; nenhuma tela de "Fim"; sem `FLAG_KEEP_SCREEN_ON`.
- `registers/opr_register.md`: OPR-005, 007(b), 008, 010, 011, 013, 015, 019, 020, 022, 024, 025, 032, 033, 034, 038.
- Ambiente: Samsung Galaxy S23 (SM-S911B, Android 16, arm64-v8a) por adb; sem tablet; sem aparelho de entrada nem intermediário; MacBook Air com Python 3.13 e sem o pacote `vosk` (memória do QA).

## Escopo

Uma tarefa só, porque tudo converge num relatório para o portão humano 3. Partes:

1. Preparação e verificações por comando no aparelho (agente sozinho).
2. Sessões de gravação (humano lê; agente prepara o aparelho e recolhe os arquivos).
3. Verdade-terreno (`expected.csv`), que depende do portão 6 ou de uma alternativa.
4. Métricas, calibração e relatório final.

Uma ferramenta nova afeta números (sincronização, D3). Ela passa pelo ciclo normal (construção com teste, revisão) antes de entrar no relatório. Os scripts de automação por adb do QA não mudam números e ficam com o QA.

## Decisões

**D1. Qual trajetória é a oficial: microfone ao vivo ou WAV reproduzido no app?**
- (a) Só ao vivo: é o caminho real do produto (microfone do aparelho, `AudioRecord`, eco captado pelo próprio aparelho), mas o WAV de referência sai de outro gravador e de outro relógio.
- (b) Só reprodução do WAV pela fonte WAV da 0003 (D8 de lá): o relógio é o mesmo por construção (`--deslocamento-ms 0`), mas o áudio vem do microfone do gravador externo, não do aparelho.
- (c) As duas: ao vivo é a oficial, e as reproduções do mesmo WAV servem para calibrar (mesmo áudio com presets, vocabulário, estratégia e limiar diferentes).
- **Escolha: (c).** O TRD pede medição no aparelho real com microfone a 50 cm, e o eco do salão só é real no microfone do aparelho. A reprodução torna a calibração repetível sem pedir ao humano que leia de novo. O 0004 já previu esse caso (`--deslocamento-ms` positivo, impl_plan 0004 A6).

**D2. Como gravar o WAV de referência sem instalar nada (o app não grava áudio, regra 3)?**
- (a) Gravador do próprio S23: não roda junto com o app, que está usando o microfone.
- (b) Outro aparelho: o humano teria de providenciar um.
- (c) O MacBook ao lado do aparelho, à mesma distância, com o QuickTime Player (nativo, "Nova Gravação de Áudio", qualidade Máxima, sem perda). A conversão para WAV 16 kHz, mono, PCM 16 bits sai do `afconvert`, nativo do macOS.
- **Escolha: (c).** Nada é instalado, e o `timestamps.py` recusa (código 2) WAV fora do formato, então um erro de conversão não passa em silêncio. O plano confere por comando os parâmetros do `afconvert`. Os originais do QuickTime ficam numa pasta do humano, fora do repositório; o agente não apaga nada fora do repositório.

**D3. Como pôr a trajetória ao vivo no relógio do WAV (`--deslocamento-ms`)?**
- (a) A olho, pela palma no áudio.
- (b) Correlação do `level.csv` do app (RMS por bloco de 200 ms) com o envelope RMS do WAV nos mesmos blocos, conferida pela palma.
- **Escolha: (b).** É determinística e roda por comando. A palma, dada 2 s depois de tocar Ouvir, é um pico nos dois sinais e serve de conferência independente. A resolução é de um bloco (200 ms, constante `BLOCK_SAMPLES` da 0003), e por isso o relatório roda o `metricas.py` também com o deslocamento ±200 ms. Se o veredito de algum critério mudar nessa faixa, a gravação sai como "sensível ao deslocamento" e não conta como PASSOU. A deriva entre os relógios é medida correlacionando o começo e o fim de cada gravação separadamente, e a diferença fica registrada. Se passar de 200 ms, a gravação recebe a mesma marca. A ferramenta é Python só com a stdlib (sem `audioop`, que saiu no 3.13), testada com sinais sintéticos e revisada antes de produzir números. Quem constrói é decisão do arquiteto (padrão do PROCESS: `motor-voz` faz as ferramentas da Etapa 0).

**D4. Verdade-terreno sem o modelo grande: dá?**
O TESTES-ETAPA0 (Trajetória esperada, passo 1) manda usar o Vosk em Python com o modelo grande, independente do modelo do app e mais preciso. Instalar o pacote `vosk` do PyPI e baixar o modelo (GPLv3) é portão 6. Alternativas avaliadas:
- (A) **Portão 6:** venv fora do repositório, `pip install vosk` em versão fixada e o modelo grande de português baixado de alphacephei.com para uma pasta fora do repositório, com o SHA-256 registrado. É o protocolo como está escrito.
- (B) **Modelo pequeno, no próprio aparelho:** um modo do app de etapa0 que decodifica o WAV com `setWords(true)` e exporta `palavra,t_inicio_ms,t_fim_ms,conf`. Não instala nada no computador, porque o `vosk-android` e o modelo já estão no app. Custos: (1) é código novo no app de etapa0, com plano e revisão; (2) muda o passo 1 do TESTES-ETAPA0, o que define como os critérios de saída são medidos, e por isso precisa de aprovação humana; (3) a verdade usa o mesmo modelo acústico que está sendo testado. Onde o modelo pequeno erra a palavra, o token fica sem tempo, o esperado anda em degrau e o atraso sai **otimista** (a métrica de saltos fica pessimista). Os tempos de palavra continuam no relógio do áudio, então a latência não contamina a verdade.
- (B') Modelo pequeno no Mac pelo Python: continua precisando do `pip install vosk`, ou seja, do mesmo portão 6. Não evita nada.
- (C) **Usar a transcrição do próprio app** (`hypotheses.csv`): os tempos são de chegada do parcial, não do áudio. O esperado ficaria atrasado pela latência de reconhecimento, o atraso sairia otimista por construção e a latência 1 seria impossível de medir. Inválido.
- (D) **Marcação manual palavra a palavra:** são cerca de 3.600 palavras no L30 e umas 1.900 nos curtos, por rodada. Inviável. Fica só para a revisão nos pontos de evento (D7).
- (E) **Reconhecimento nativo do macOS (framework Speech):** precisa de autorização de privacidade e, em geral, de baixar o recurso de idioma. Também cai no portão 6, por um caminho menos documentado.
- **Escolha: recomendar (A)** e devolver a pergunta ao humano: (A) com o portão 6, ou (B) com a mudança do protocolo. Não há caminho sem decisão humana que mantenha a validade: (B) evita a instalação, mas troca uma verdade independente por uma correlacionada e exige mudar o TESTES. A pergunta não bloqueia o plano nem as gravações, só o passo do `expected.csv`. Se vier (B), o relatório traz por gravação a cobertura (fração de tokens com tempo) e o número de amostras dentro de lacunas da verdade, para o humano ver o tamanho do viés.

**D5. Como fica a instalação, se vier (A)?**
- **Escolha:** venv fora do repositório (por exemplo `~/.venvs/tribuna-etapa0`); `vosk` em versão fixada, com versão e hash do wheel na evidência; modelo grande numa pasta fora do repositório, nunca em `etapa0/`, no LFS ou no app (CONFORMIDADE: GPLv3 fora do app). O `timestamps.py` roda com o Python do venv e `--modelo <pasta>`. O nome exato do modelo e o tamanho são conferidos na página de modelos no dia do download e registrados. A rede é usada só no computador, só nesse passo; o aparelho segue em modo avião.

**D6. Quem preenche a folha de eventos (improviso, silêncio, pulo), se ninguém sabe o `t_ms` do app na hora?**
- (a) Uma pessoa anota com cronômetro: impreciso e num relógio que não é o do app.
- (b) O agente deriva dos dados. Improviso: faixa de palavras inseridas que o `alinhar.py` lista no stderr. Silêncio: o fim da última palavra antes e o início da primeira depois, no `expected.csv`, conferido pelo `level.csv`. Pulo: `pulo_inicio` com o índice do primeiro token pulado (sabido pelo roteiro), e `pulo_fim` no início da primeira palavra falada depois do pulo. Os tempos passam para o relógio do app subtraindo o deslocamento, porque o `metricas.py` aplica o deslocamento a todos os arquivos de eventos.
- **Escolha: (b)**, gravada em `<nome>.folha.events.csv`, separada do `events.csv` do app (o `metricas.py` aceita os dois, 0004 D11). O humano confirma cada fronteira ouvindo (D7). Pausa e toque em palavra vêm do `events.csv` do app.

**D7. Revisão manual do esperado (passo 3 do TESTES) sem virar ajuste de resultado.**
- **Escolha:** o agente gera uma lista de pontos de escuta por gravação, com o tempo no WAV e o que conferir:
  - cada fronteira da folha de eventos;
  - cada episódio de salto errado e cada recuperação reprovada, **antes** de declarar REPROVA;
  - cada trecho de tokens sem tempo vizinho de amostras que mudam um veredito.
  O humano ouve e responde certo ou errado, e diz o tempo certo quando o ponto estiver errado. Toda correção no `expected.csv` ou na folha vai para um registro (antes, depois, motivo), e o veredito só é calculado depois de todas as correções. As versões ficam no git. O relatório mostra quantas correções houve e em que direção mudaram o veredito.

**D8. Rodadas, com o que existe hoje.**
O protocolo pede aparelho de entrada, intermediário e tablet, duas vozes e dois ambientes. Hoje só há o S23, que é topo de linha.
- **Escolha:**
  - S1: Giorgio, sala silenciosa, S23. T01–T10, com T02 em três takes, e L30.
  - S2: segunda voz, sala silenciosa, S23. T01–T10, com T02 em três takes. O L30 com a segunda voz é opcional.
  - S3: Giorgio, salão com eco, S23. T01–T10 e L30.
  - Tablet: rodada complementar quando houver tablet (T01, T05, T06, T07 e L30), sem bloquear o relatório.
  - Aparelho de entrada e intermediário: lacuna declarada no relatório. Conseguir aparelhos é gasto (portão 6) e fica como recomendação.
  Sem a segunda voz ou sem o salão, os critérios ligados saem NÃO MEDIDO, e nunca PASSOU.

**D9. Como cada critério de saída é julgado (agregação).**
- **Escolha:** por gravação. Toda gravação ligada a um critério tem de passar nele. A ligação segue a tabela "Critério de saída ligado" do TESTES-ETAPA0 e o texto de cada critério do TRD:
  - **CS1, atraso ≤ 3 em 95%:** só sala silenciosa ("em ambiente silencioso"), em T01, T10 e L30, por voz.
  - **CS2, nenhum salto errado:** L30 nos dois ambientes, mais T02, T03, T04, T08 e T09 (a tabela liga cada um a "nenhum salto errado", "cursor parado" ou "não avança nem recua mais de 3"). Os recuos (`highWater − 3`) entram aqui também, porque o T08 os cita e a regra 6 do CLAUDE.md os proíbe.
  - **CS3, recuperação de pulo ≤ 3 s:** T05 e o pulo do L30 (minuto 15).
  - **CS4, sem movimento no silêncio e na pausa:** T06 e T07, e os eventos dos minutos 12 e 18 do L30.

  CS2 a CS4 valem nos dois ambientes, porque o texto deles não restringe ambiente. O atraso no salão e todos os números das outras gravações saem informativos. Cada número leva a contagem de amostras e a regra de contagem (Q3, Q7). Um critério calculado sobre zero amostras sai NÃO MEDIDO.

**D10. Configuração da medição oficial.**
- **Escolha:** os padrões da 0003, sem número novo: preset Normal, vocabulário desligado, reconhecedor novo por sessão, limiar 0 e animação 375 ms (PROVISÓRIO, OPR-022). O `meta.txt` de cada gravação prova a configuração (Q8).

**D11. O que a 0005 calibra e o que só confirma.**
- **Calibra por reprodução do WAV, com os controles do painel:** preset (Conservador, Normal, Ágil), vocabulário (ligado, desligado), estratégia (novo, `reset()`) e limiar de nível.
- **Só confirma ou diagnostica**, porque não estão no painel e mexer neles é código de `align/`: peso das palavras comuns (0,5 PROVISÓRIO), margem de ambiguidade da recuperação (OPR-007 b), desempate (OPR-011), hífen (OPR-025) e fim da janela (OPR-015). O valor provisório fica confirmado se todos os critérios ligados passarem com ele. Se um critério reprovar e o diagnóstico apontar para um deles, o relatório abre uma tarefa nova de recalibração offline com o golden (0006, OPR-008) e não muda nada na 0005.
- **Números da tabela do PRD** (k, W, limites) não mudam aqui: mudar é portão 2. A 0005 só mede os três presets como estão.

**D12. Regras de recomendação, sem número inventado.**
- **Vocabulário:** recomenda a opção que passa em todos os critérios ligados, inclusive o T04 (o TRD avisa que o vocabulário pode piorar o improviso). Se as duas passarem, a sugestão é desligado, que não depende da lista (OPR-020) e é o padrão da 0003; a decisão final é humana, no portão 3.
- **Estratégia:** o TRD só diz "se for lento". "Lento" = o início da escuta (D16) não cumpre a meta provisória de 2 s do TRD em todas as repetições com "novo". Nesse caso, recomenda `reset()` se ele cumprir; senão, continua "novo".
- **Limiar de nível:** fica 0 se nenhuma gravação tiver movimento automático em silêncio ou pausa com limiar 0. Se tiver, o valor candidato é o menor nível que aparece no `level.csv` acima do nível dos parciais que causaram o movimento, e ele é verificado por reprodução: tem de eliminar o movimento e não pode reprovar o CS1 nas gravações da sala. O número sai dos dados, com o método declarado ao lado.
- **Preset:** recomenda entre os que passam em tudo. Se mais de um passar, o relatório mostra os números de cada um e sugere o Normal, padrão da 0003, se ele estiver entre os que passam. OPR-010 (Ágil) recebe o resultado.

**D13. "Metas provisórias fixadas" (CS6) com um aparelho só.**
- **Escolha:** cada meta tem um valor proposto igual ao medido no S23, com a regra declarada (mediana, p95, máximo, n) e o rótulo "aparelho topo de linha". Para as metas que o TRD liga a aparelho de entrada ou intermediário (bateria; memória para entrada), o relatório diz que não dá para fixar sem medir nesses aparelhos. Atualizar o TRD é do arquiteto, com aprovação humana no portão 3.

**D14. Bateria (45 min, brilho máximo, aparelho intermediário).**
- **Escolha:** uma corrida própria, com o aparelho **desconectado**, brilho máximo, modo avião e o app em leitura pela fonte WAV: L30 e, em seguida, outros WAV até completar 45 min. O humano desconecta, toca Ouvir quando um WAV termina e reconecta no fim. O agente lê `dumpsys battery` antes e depois e registra os horários. Desvios declarados: fonte WAV no lugar do microfone (a captura pesa pouco; o custo está no reconhecimento e na tela), resolução de 1% e S23 no lugar de aparelho intermediário.

**D15. Memória.**
- **Escolha:** `dumpsys meminfo <pacote>` a cada 60 s durante a S1, com o processo vivo por 45 min ou mais (a sessão dura mais que isso). O relatório traz PSS e RSS: pico, mediana e n. O desvio está declarado: o TRD fala em Profiler, e o comando dá o mesmo número sem interface gráfica.

**D16. Latências e início da escuta.**
- **Escolha:**
  - Latência 1, latência 2, fala → cursor e latência total: `metricas.py --hipoteses`, com as regras do 0004 D18.
  - Latência 3, tempo de `beginSession` e tempo até o 1º parcial: só existem no painel. O agente lê o painel por `uiautomator dump`, uma amostra por segundo durante reproduções.
  - Início da escuta (quente): tempo de `beginSession` (painel) mais a latência 1 da primeira palavra da gravação, comparado com a meta provisória de 2 s. A etapa0 não tem contagem regressiva, e a soma é a parte do trajeto que existe nela.

**D17. Inicialização a frio e primeira cópia.**
- **Escolha:** `am force-stop` e `am start -W`, 10 vezes, com o `prepare_ms` do painel ou do `meta.txt`; primeira cópia com `pm clear`, 3 vezes (`install_ms`). O n é tamanho de amostra, não limiar.

**D18. Ajustes do aparelho durante as sessões.**
- **Escolha:** por adb, com o valor anterior registrado e restaurado no fim: modo avião ligado, Não perturbe ligado, brilho fixo e tempo de tela longo, ou `svc power stayon usb` com cabo. A etapa0 não tem `FLAG_KEEP_SCREEN_ON` (fora do escopo da 0003), e sem isso a tela apaga no L30, o `onStop` pausa a leitura e a gravação se perde. O que o adb não conseguir mudar vai para o humano.

**D19. De onde o orador lê.**
- **Escolha:** de uma cópia no Mac (fonte grande) ou impressa, olhando o aparelho só para tocar. Assim a fala não se adapta ao cursor que está sendo medido. Quando o protocolo manda tocar (T07 e L30), o toque é no aparelho.

**D20. Nomes e pastas.**
- **Escolha:** `<ID>_<voz>_<ambiente>_t<n>`, com voz `giorgio` ou `voz2` (pseudônimo, para o nome real não ir ao repositório) e ambiente `sala` ou `salao`. Arquivos:
  - `gravacoes/<nome>.wav` (LFS);
  - `trajetorias/<nome>.{trajectory,hypotheses,level}.csv` e `.meta.txt`;
  - `eventos/<nome>.events.csv` (app) e `<nome>.folha.events.csv` (D6);
  - `esperado/<nome>.words.csv`, `<nome>.expected.csv` e `<nome>.revisao.md` (D7);
  - `relatorios/<rodada>/`: um relatório Markdown e um JSON por gravação, e um resumo por rodada.
  As reproduções usam o nome `<nome>-r-<preset>-<vocab>-<estrategia>-<limiar>` e ficam em `trajetorias/reproducoes/`.

**D21. Ensaio do agente antes de chamar o humano.**
- **Escolha:** se o macOS já tiver uma voz pt-BR do `say` (conferir com `say -v '?'`, sem baixar nada), o agente gera o T01 sintético, tocado pelo `afplay` perto do aparelho com o app ao vivo e também enviado como WAV. Isso testa exportação, `adb pull`, conversão e sincronização (o deslocamento da reprodução tem de dar 0 ± 1 bloco) sem gastar tempo do humano. O ensaio não conta para nenhum critério e não vai para o repositório. Sem voz pt-BR, o ensaio usa só a fonte WAV com a primeira gravação da S1.

**D22. LFS e consentimento.**
- **Escolha:** a 32.000 B/s, as três rodadas dão perto de 300 MB de WAV, além dos ~31 MB do modelo já no LFS. O agente mede o total por comando antes do push, e o push dos WAV espera duas coisas do humano: (1) confirmar que cabe na cota do LFS da conta (portão 6 condicional, igual à 0003 D26); (2) o consentimento escrito da segunda voz para a gravação dela ir ao repositório. O improviso e a fala de terceiros só podem ter conteúdo inventado e neutro, porque também vão para os WAV.

**D23. Toque numa palavra 3 parágrafos à frente (L30, minuto 22) na tela da etapa0.**
- **Risco:** a palavra pode estar fora da tela. O texto está num `verticalScroll`, então dá para rolar com o dedo, mas a rolagem automática pode puxar de volta.
- **Escolha:** o agente testa no ensaio (D21) com `input swipe` e `input tap`. Se o gesto não for confiável, o roteiro diz como fazer (parar de falar, rolar, tocar) e o relatório registra a limitação. Toque em palavra com a leitura pausada não entra no roteiro (OPR-038).

**D24. Verificações no aparelho por comando (seção E da 0003).** Todas pelo agente, com saída anexada:
- SHA-256 do APK instalado igual ao do APK construído;
- permissões pedidas pelo pacote instalado (`dumpsys package`): só `RECORD_AUDIO`;
- trilha de captura em 16.000 Hz, mono, PCM 16 bits durante a leitura (`dumpsys media.audio_flinger`);
- `hypotheses.csv` sem resto de JSON (`{`, `}`, `"partial"`, `"text"`) e com finais não vazios, para confirmar o parser de C6 contra o JSON real (Q4);
- `logcat` sem `FATAL EXCEPTION` e sem palavras distintivas dos textos lidos;
- listagem dos arquivos do app antes e depois de uma sessão ao vivo: nenhum arquivo de áudio novo;
- rotação e ida ao segundo plano por adb, com o estado conferido pelo painel e pelo `dumpsys audio`;
- release com R8 instalado, com o modelo carregando e reconhecendo o T01 pela fonte WAV.

**D25. Ferramentas e scripts.**
- **Escolha:** a sincronização (D3) e a derivação da folha (D6) mudam números do relatório. Por isso entram em `etapa0/ferramentas/`, com teste e revisão, pelo ciclo normal. Os scripts que só dirigem o aparelho por adb (tocar, ler o painel, puxar arquivos) ficam com o QA em `work/0005-etapa0-medicao/scripts/` e não mudam número. Nada muda em `align/`, no app de etapa0 ou em contrato. Se vier a opção (B) de D4, a mudança no app vira tarefa à parte.

**D26. Relatório final para o portão 3.**
- **Escolha:** `etapa0/relatorios/etapa0-relatorio-final.md`, com um resumo no `test_result.md`. Conteúdo:
  - uma tabela com os 8 critérios de saída do TRD: número, regra de contagem, n, gravações ligadas, PASSOU, REPROVOU ou NÃO MEDIDO, e evidência;
  - as decisões do TRD (vocabulário, estratégia) com os dados;
  - calibração (preset, limiar) e os provisórios confirmados ou não;
  - metas propostas (D13);
  - lacunas do protocolo (aparelhos, tablet, voz, salão);
  - interpretações pendentes que mudam veredito (OPR-032, 033, 034), com o número de amostras afetadas por cada uma;
  - 16 KB (OPR-024), com os caminhos a–d;
  - recomendação: aprovar a arquitetura, aprovar com condições listadas, ou não aprovar. Neste último caso, a seção de alternativas de motor é do arquiteto.

**D27. Interpretações ainda não aprovadas no texto do TESTES (OPR-032, 033, 034).**
- **Escolha:** o veredito usa o `metricas.py` como está. O relatório mostra quantas amostras ou ocorrências cada interpretação muda. Se alguma delas decidir um critério, isso aparece na primeira página, para o humano decidir no portão 3. O `metricas.py` não muda.

**D28. A configuração recomendada é diferente da oficial.**
- **Escolha:** se a calibração recomendar algo diferente de D10, uma sessão curta ao vivo confirma (T01, T05, T06 e T07 com a configuração recomendada, cerca de 25 min), e o L30 é conferido por reprodução. Sem essa sessão, o relatório diz que a configuração recomendada só foi medida por reprodução.

## Abordagens consideradas para a tarefa como um todo

1. **Ao vivo oficial + reproduções para calibrar (escolhida):** fiel ao produto e repetível, ao custo de uma ferramenta de sincronização.
2. **Só reproduções:** mais simples, mas o microfone medido não é o do aparelho.
3. **Só ao vivo, com cada variante lida de novo:** multiplica as horas do humano por umas 8.

## O que vai ao orquestrador (portões)

- Portão 6: (A) instalar `vosk` e baixar o modelo grande, ou (B) mudar o passo 1 do TESTES (D4). Recomendação: (A). Bloqueia só o passo do `expected.csv`.
- Portão 6 condicional: cota do LFS para os WAV, junto com o consentimento da segunda voz (D22). Bloqueia só o push.
- Portão 3: o relatório final.
- Portão 2, só se a recomendação vier: padrão do R5 (OPR-022) e qualquer número da tabela de sensibilidade.
