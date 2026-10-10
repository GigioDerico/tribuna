# PRD — Tribuna: Teleprompter por Voz (Android Offline)

9 de outubro de 2026 · Giorgio Derico (revisado em 10 de outubro de 2026)

## Visão geral

App Android nativo de teleprompter que acompanha a voz do orador, 100% offline, sem salvar o texto do discurso. Nome: **Tribuna**.

**Problema.** Quem faz discursos públicos (por exemplo, na congregação) precisa ler o esboço sem perder o contato visual. Teleprompters com rolagem fixa exigem falar no ritmo da máquina. Apps que seguem a voz, como o VoicePrompter, costumam depender de internet ou de assinatura.

**Solução.** O orador cola o texto, ajusta o visual e fala. O app reconhece a fala localmente, alinha as palavras com o roteiro e rola a tela até a frase atual. Se o orador pausa, improvisa ou pula um trecho, o texto espera ou salta junto.

**Métricas de sucesso**

- Cursor de leitura no máximo 3 palavras atrás da fala em 95% do tempo, em ambiente silencioso.
- Zero saltos errados por discurso de 30 minutos em teste com roteiro real.
- Discurso de 45 minutos sem travar, sem aquecimento excessivo e com consumo de bateria abaixo de 15%.
- Do toque em "Iniciar" à primeira palavra reconhecida em menos de 2 segundos.

## Decisão de stack

**Kotlin + Jetpack Compose, nativo, com Vosk para reconhecimento offline.** É o caminho que roda mais liso, porque o áudio, o reconhecimento e a rolagem ficam no mesmo processo nativo, sem ponte entre linguagens.

| Opção            | Rolagem e renderização                                                       | Integração com Vosk                                                       | Veredito            |
|------------------|------------------------------------------------------------------------------|---------------------------------------------------------------------------|---------------------|
| Kotlin + Compose | Nativa, 60–120 fps, controle fino de TextLayoutResult para tocar em palavras | AAR oficial com.alphacephei:vosk-android, chamada direta                  | **Escolhida**       |
| Flutter          | Boa, mas o toque por palavra exige RichText + cálculo manual de posições     | Plugin de terceiros ou canal nativo; resultados parciais passam por ponte | Viável, mais atrito |
| React Native     | Rolagem suave exige otimizações; texto longo pesa                            | Módulo nativo próprio, ponte JS a cada parcial                            | Não recomendado     |

**Componentes principais**

- **Linguagem:** Kotlin 2.x, coroutines e StateFlow.
- **UI:** Jetpack Compose + Material 3.
- **Voz:** Vosk Android com modelo vosk-model-small-pt-0.3 (~30–50 MB) empacotado em assets.
- **Preferências:** Jetpack DataStore (só configurações visuais, nunca o texto).
- **Arquitetura:** MVVM, uma Activity, Navigation Compose. O reconhecimento fica atrás de uma interface SpeechEngine, que entrega transcrições parciais e finais; o Aligner decide a posição no roteiro. Assim o Vosk pode ser trocado sem refazer o app.
- **SDK:** mínimo Android 8.0 (API 26), alvo API 36 (exigido pelo Google Play para novos apps e atualizações desde 31/08/2026).
- **Build:** Android Studio e Gradle Kotlin DSL.
- **Testes:** JUnit para o Aligner e testes de interface com Compose. O layout se adapta a celular e tablet com as classes de tamanho de janela do Material 3.

Na sessão do Claude Code, peça explicitamente "Kotlin, Jetpack Compose, sem XML" para evitar que ele gere Views antigas.

O TRD (docs/TRD.md) detalha a implementação e prevalece em caso de divergência técnica com este PRD. As exigências de publicação estão em docs/CONFORMIDADE.md.

## Público, princípios e escopo

**Público.** Oradores que leem um roteiro em pé, num púlpito ou tribuna, com o celular ou tablet apoiado à frente. Uso em salões onde a internet é fraca ou ausente.

**Princípios**

1. **Offline sempre.** Nenhuma funcionalidade depende de rede. O app não pede permissão de internet.
2. **Privacidade.** O texto vive só na memória do processo. Ao fechar o app, ou se o Android encerrar o processo, o texto some. Encerrar a leitura não apaga o texto. Nenhum áudio é gravado.
3. **Nunca atrapalhar.** Na dúvida, o texto espera em vez de saltar.
4. **Um toque resolve.** Durante o discurso, nada exige menus: tocar no vazio pausa, tocar numa palavra posiciona.
5. **Legível à distância.** Fontes grandes, alto contraste e linha de leitura fixa.

**Dentro do escopo (v1)**

- Editor de texto com colar, limpar e edição livre.
- Teleprompter com acompanhamento por voz e modo de velocidade constante.
- Ajustes de fonte, cores, espaçamento, margens, espelho, linha de leitura e velocidade.
- Cronômetro com tempo-alvo e avisos visuais.
- Toques para pausar e saltar para palavra; controle por botões de volume e teclado Bluetooth.
- Layout próprio para tablet (retrato e paisagem), com as mesmas telas e estados.

**Fora do escopo (v1)**

- Salvar, listar ou sincronizar discursos.
- Importar arquivos (.docx, .pdf) e conta de usuário.
- Gravação de áudio ou vídeo.
- iOS e Android TV.
- Tradução ou análise de conteúdo.

## Requisitos funcionais

P0 = obrigatório no MVP; P1 = entra antes da versão 1.0; P2 = desejável.

**Texto**

| ID  | Requisito                                                                                                                                                                                                            | Prioridade |
|-----|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| T1  | Campo de texto multilinha para colar ou digitar o discurso                                                                                                                                                           | P0         |
| T2  | Botões "Colar" (da área de transferência) e "Limpar" com confirmação                                                                                                                                                 | P0         |
| T3  | Texto mantido apenas em memória (ViewModel); sobrevive a rotação e ao segundo plano enquanto o processo estiver vivo; some ao fechar o app ou se o Android encerrar o processo. Encerrar a leitura não apaga o texto | P0         |
| T4  | Contagem de palavras e tempo estimado de leitura (base 130 palavras/min, ajustável)                                                                                                                                  | P1         |
| T5  | Marcadores no texto: linha com // vira nota visual (cor secundária, não é seguida pela voz)                                                                                                                          | P2         |
| T6  | Editar o texto a partir do teleprompter, voltando ao mesmo ponto                                                                                                                                                     | P1         |

**Aparência** (salva em DataStore)

| ID  | Ajuste                          | Faixa                                                                                                                                       | Padrão                |
|-----|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| A1  | Tamanho da fonte                | 24–96 sp                                                                                                                                    | 44 sp (tablet: 56 sp) |
| A2  | Família da fonte                | Sans, Serif, Mono, fonte de alta legibilidade (ex.: Atkinson Hyperlegible)                                                                  | Sans                  |
| A3  | Peso da fonte                   | Normal, Médio, Negrito                                                                                                                      | Médio                 |
| A4  | Espaçamento entre linhas        | 1,0–2,5×                                                                                                                                    | 1,4×                  |
| A5  | Margem lateral                  | 0–30% da largura                                                                                                                            | 8% (tablet: 12%)      |
| A6  | Cor do texto                    | Paleta + seletor livre                                                                                                                      | Branco                |
| A7  | Cor do fundo                    | Paleta + seletor livre                                                                                                                      | Azul-petróleo escuro  |
| A8  | Cor da palavra atual (destaque) | Paleta + seletor livre                                                                                                                      | Verde-menta           |
| A9  | Opacidade do texto já lido      | 20–100%                                                                                                                                     | 40%                   |
| A10 | Posição da linha de leitura     | 10–60% da altura                                                                                                                            | 30%                   |
| A11 | Indicador da linha de leitura   | Nenhum, seta lateral, faixa                                                                                                                 | Seta lateral          |
| A12 | Espelho horizontal / vertical   | Liga/desliga                                                                                                                                | Desligado             |
| A13 | Temas prontos                   | Preto/branco, Branco/preto, Amarelo/preto, Verde/preto                                                                                      | —                     |
| A14 | Orientação                      | Retrato, paisagem, automática. Só no celular: em tablets o Android 16 ignora o bloqueio de orientação e o app funciona nas duas orientações | Automática            |

**Rolagem e voz**

| ID  | Requisito                                                                                                                                                                                       | Prioridade |
|-----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| R1  | Modo **Voz**: rolagem segue a fala reconhecida offline                                                                                                                                          | P0         |
| R2  | Modo **Constante**: rolagem automática com velocidade de 1 a 10 (ajuste ao vivo); a tabela nível → velocidade de rolagem (dp/s) é definida na calibração                                        | P0         |
| R3  | Troca de modo durante a leitura sem perder a posição                                                                                                                                            | P1         |
| R4  | Sensibilidade do acompanhamento: Conservador, Normal, Ágil                                                                                                                                      | P1         |
| R5  | Suavidade da rolagem (duração da animação 150–600 ms)                                                                                                                                           | P1         |
| R6  | Plano B automático: se não houver fala reconhecida por N segundos, opcionalmente mudar para modo Constante; desligado por padrão, para que uma pausa intencional não faça o texto andar sozinho | P2         |
| R7  | Indicador discreto de microfone ativo e nível de áudio                                                                                                                                          | P0         |

**Discurso**

| ID  | Requisito                                                                                       | Prioridade |
|-----|-------------------------------------------------------------------------------------------------|------------|
| D1  | Contagem regressiva configurável antes de iniciar (0, 3, 5, 10 s)                               | P1         |
| D2  | Cronômetro do tempo decorrido, visível ou oculto                                                | P0         |
| D3  | Tempo-alvo (ex.: 30 min) com aviso visual aos 5 min e 1 min restantes (borda colorida, sem som) | P1         |
| D4  | Tela sempre ligada e brilho máximo opcional durante a leitura                                   | P0         |
| D5  | Progresso do discurso (barra fina com % lido)                                                   | P1         |

**Controles**

| ID  | Requisito                                                                                     | Prioridade |
|-----|-----------------------------------------------------------------------------------------------|------------|
| C1  | Toque em área vazia: pausa/retoma                                                             | P0         |
| C2  | Toque numa palavra: cursor vai para ela e a leitura continua                                  | P0         |
| C3  | Arrastar com o dedo: rolagem manual; ao soltar, o cursor assume a palavra na linha de leitura | P1         |
| C4  | Botões de volume: pausar/retomar e voltar uma frase (configurável)                            | P1         |
| C5  | Teclado/controle Bluetooth: espaço pausa, setas avançam/voltam frase                          | P2         |
| C6  | Pinça (pinch) para ajustar o tamanho da fonte ao vivo                                         | P2         |

## Telas e fluxos

São 6 telas principais, com estados adicionais e versão de tablet. O fluxo principal tem 3 passos: colar o texto, iniciar, falar.

**1. Primeiro uso**

- Logo e frase curta: "Seu teleprompter que acompanha sua voz, sem internet".
- Pedido de permissão de microfone com explicação do motivo (texto em docs/CONFORMIDADE.md).
- Preparação do modelo de voz (cópia dos assets para o armazenamento interno, só na primeira vez), com barra de progresso.
- Se a permissão for negada: aviso e opção de usar só o modo Constante.

**2. Editor (tela inicial)**

- Barra superior: nome do app, ícone de Configurações.
- Área de texto ocupando a tela, com placeholder "Cole ou digite seu discurso aqui".
- Barra inferior: Colar, Limpar, contagem de palavras e tempo estimado.
- Seletor de modo: Voz | Constante.
- Botão principal grande: **Iniciar**. Desabilitado com texto vazio.
- Atalho "Aparência" que abre os Ajustes rápidos.

**3. Ajustes rápidos (bottom sheet)**

- Abre a partir do Editor e da tela de pausa.
- Prévia ao vivo de 2 linhas de texto com as cores e a fonte atuais.
- Sliders: tamanho da fonte, espaçamento, margem, velocidade (modo Constante).
- Temas prontos em chips e seletores de cor do texto, fundo e destaque.
- Interruptor de espelho.

**4. Teleprompter (leitura)**

- Texto em tela cheia, sem barra do sistema (modo imersivo).
- Linha de leitura fixa na posição configurada; palavra atual destacada; texto lido esmaecido.
- Canto superior: cronômetro e, se houver, tempo-alvo restante.
- Canto inferior: ponto de microfone ativo (pulsa com o nível de áudio) e barra fina de progresso.
- Borda muda de cor nos avisos de tempo (5 min: amarelo; 1 min: vermelho).
- Contagem regressiva grande no centro antes de começar; qualquer toque cancela a contagem e pausa.

**5. Pausa (sobreposta ao teleprompter)**

- Texto continua visível, levemente escurecido; legenda "Pausado — toque para continuar".
- Botões: Continuar, Voltar ao início, Editar texto, Aparência, Encerrar.
- Troca de modo Voz/Constante.

**6. Configurações**

- Seções: Aparência (todos os itens A1–A14; A14 oculto no tablet), Rolagem e voz (R2–R6), Discurso (D1–D5), Controles (C4–C6).
- Velocidade média de fala para o tempo estimado.
- Sobre: versão, licenças de terceiros (Vosk e o modelo são Apache 2.0) e política de privacidade ("nada sai do aparelho").

**Fluxo de encerramento.** Ao chegar ao fim do texto, o app mostra "Fim" com o tempo total e o tempo-alvo, e um botão Voltar ao Editor; o texto continua intacto, pronto para outra passada.

**Estados adicionais.** Aviso de tempo aos 5 min (borda amarela) e ao 1 min (borda vermelha). Leitura no modo Constante, com indicador de velocidade e sem microfone. Editor sem microfone, com Voz desabilitado e aviso com link para permitir. Erro ao preparar o modelo de voz, com as opções Tentar de novo e Usar só o modo Constante.

**Tablet.** Layout próprio com as mesmas telas e estados: editor em duas colunas (texto à esquerda; contagem, Colar e Limpar, modo, Aparência e Iniciar à direita), Configurações com menu lateral e leitura em retrato e em paisagem. Fonte e margem padrão maiores (A1, A5).

## Interações e gestos

No teleprompter, todo gesto tem um único efeito, previsível, em cada estado.

| Gesto                    | Lendo                                                                  | Pausado                                   | Contagem regressiva        |
|--------------------------|------------------------------------------------------------------------|-------------------------------------------|----------------------------|
| Toque em área vazia      | Pausa (mic continua ligado, cursor congela)                            | Retoma do cursor atual                    | Cancela a contagem e pausa |
| Toque numa palavra       | Cursor vai para a palavra; rola até a linha de leitura; continua lendo | Cursor vai para a palavra e **retoma**    | Cancela a contagem e pausa |
| Toque longo numa palavra | —                                                                      | Abre o Editor posicionado naquela palavra | —                          |
| Arrastar                 | Rolagem manual; leitura suspensa enquanto o dedo está na tela          | Rolagem manual                            | —                          |
| Soltar após arrastar     | Cursor = primeira palavra na linha de leitura; continua                | Mantém pausado                            | —                          |
| Pinça                    | Ajusta fonte (P2)                                                      | Ajusta fonte (P2)                         | —                          |
| Volume +                 | Pausa/retoma                                                           | Retoma                                    | —                          |
| Volume −                 | Volta ao início da frase anterior                                      | Volta ao início da frase anterior         | —                          |
| Voltar (sistema)         | Pausa                                                                  | Confirma "Encerrar leitura?"              | Cancela                    |

**Regras do toque**

1. **Palavra vs. vazio.** O toque conta como palavra só se cair dentro do retângulo da palavra ampliado em 8 dp (área de toque confortável). Espaço entre linhas, margens e o fim do texto contam como vazio.
2. **Por quê.** TextLayoutResult.getOffsetForPosition sempre devolve o caractere mais próximo, mesmo longe do texto. Por isso é obrigatório conferir com getBoundingBox dos caracteres da palavra.
3. **Toque vs. arrasto.** Movimento acima do touchSlop do sistema vira arrasto; abaixo, toque. Toque e arrasto nunca disparam juntos.
4. **Feedback.** Ao saltar para uma palavra, ela pisca no destaque por 300 ms. Ao pausar, vibração curta (opcional, desligável).
5. **Depois do salto.** O motor de voz reinicia a janela de busca a partir da nova posição e ignora os resultados parciais anteriores ao toque.
6. **Pausa e cronômetro.** Pausar a rolagem não pára o cronômetro do discurso. Resultados de voz recebidos durante a pausa são descartados, sem causar saltos. Em segundo plano, a captura de áudio é suspensa.

## Motor de acompanhamento por voz

O motor compara as últimas palavras ouvidas com uma janela do roteiro à frente do cursor e só avança quando a correspondência é confiável. Não há interpretação de sentido; é alinhamento de texto. Esta seção descreve o comportamento; a implementação (captura própria com sessões, reconciliação de parciais, janela medida a partir da maior posição confirmada) está no TRD, que prevalece em caso de divergência.

**Pipeline**

1. **Captura.** AudioRecord a 16 kHz, mono, PCM 16 bits, em laço próprio que alimenta o Recognizer do Vosk (ver TRD).
2. **Reconhecimento.** Vosk emite resultados parciais (a cada ~100–300 ms) e finais (fim de frase). Os dois alimentam o alinhador.
3. **Normalização.** Minúsculas, sem acentos, sem pontuação; números do roteiro convertidos para extenso ("24" → "vinte e quatro"; "3:16" → "três dezesseis") porque o Vosk devolve por extenso. Referências que podem ser faladas de mais de um jeito (como João 3:16: três dezesseis, três e dezesseis, capítulo três versículo dezesseis) aceitam todas as variantes; o texto original é preservado para destaque e toque.
4. **Alinhamento.** Atualiza o índice do cursor (palavra atual).
5. **Rolagem.** A UI observa o cursor (StateFlow) e anima a rolagem até a linha da palavra ficar na linha de leitura.

**Algoritmo de alinhamento**

- O roteiro vira uma lista de tokens normalizados, cada um com seu intervalo de caracteres no texto original (para destacar e tocar).
- Os resultados parciais passam por uma reconciliação: só palavras estáveis entram no alinhamento, e o resultado final confirma o restante da frase, sem entregar a mesma palavra duas vezes (detalhes no TRD, PartialReconciler).
- Busca-se, numa janela à frente do cursor, a posição cuja sequência de *k* tokens tem a maior similaridade média (similaridade = 1 − Levenshtein / maior comprimento).
- Avança se: distância ≤ 5 palavras e similaridade ≥ limite curto; ou similaridade ≥ limite longo (salto).
- Palavras muito comuns ("de", "que", "e", "o", "a") têm peso reduzido, para não causarem saltos.
- Se nada passa no limite, o cursor fica parado (princípio "na dúvida, espera").
- O cursor nunca volta sozinho mais de 3 palavras em relação à maior posição já confirmada; voltar mais só por toque ou botão.
- Recuperação de salto longo: se por 2 resultados seguidos nada passa na janela, mas as últimas 4 ou mais palavras ouvidas batem (similaridade ≥ limite de salto longo) com uma posição única à frente da maior posição já confirmada, o cursor salta para lá. Com trechos repetidos ou candidatos ambíguos, o cursor fica parado.

| Parâmetro                 | Conservador | Normal | Ágil |
|---------------------------|-------------|--------|------|
| Palavras comparadas (*k*) | 4           | 3      | 2    |
| Janela à frente (*W*)     | 25          | 40     | 60   |
| Limite para avanço curto  | 0,70        | 0,60   | 0,55 |
| Limite para salto longo   | 0,90        | 0,85   | 0,80 |

**Vocabulário restrito.** Os modelos pequenos do Vosk aceitam uma lista de palavras na criação do Recognizer. Passar os tokens do roteiro + [unk] pode aumentar a precisão. Validar com o modelo em português na Etapa 0 (prova de conceito); a Etapa 0 decide se fica ligado, desligado ou configurável.

**Robustez**

- Reinício transparente do reconhecedor após erros ou ao saltar por toque (descarta parciais antigos).
- Ruído e eco: limiar mínimo de nível de áudio antes de considerar um parcial.
- Fim do texto: ao atingir o último token, aguarda 2 s e mostra "Fim".

**Teste obrigatório.** Um modo de depuração (oculto em Configurações) mostra a transcrição bruta e a similaridade de cada avanço. É o que permite calibrar com discursos reais.

## Requisitos não funcionais

| Área           | Requisito                                                                                                                                                                                      |
|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Desempenho     | Rolagem a 60 fps em aparelhos de entrada (ex.: 3 GB de RAM, Android 10); latência fala → rolagem abaixo de 500 ms (a validar na prova de conceito)                                             |
| Texto longo    | Discurso de até 8.000 palavras sem queda de fluidez (layout em cache, recalculado só quando fonte, margem, espaçamento ou orientação mudam)                                                    |
| Bateria        | Até 15% em 45 minutos com tela no brilho máximo, em aparelho intermediário; medir com protocolo definido na prova de conceito (aparelhos, distância do microfone, brilho fixo, ruído de salão) |
| Memória        | Meta a fixar após medição na prova de conceito: a documentação do Vosk cita cerca de 300 MB para modelos pequenos, acima dos 250 MB antes previstos                                            |
| Tamanho        | APK/AAB abaixo de 80 MB com o modelo incluso                                                                                                                                                   |
| Offline        | Nenhuma permissão INTERNET no manifesto                                                                                                                                                        |
| Privacidade    | Sem gravação de áudio em disco, sem analytics, sem logs com o texto                                                                                                                            |
| Permissões     | Somente RECORD_AUDIO; a tela ligada usa a flag da janela, sem WAKE_LOCK                                                                                                                        |
| Acessibilidade | Contraste mínimo 4,5:1 nos temas prontos; botões de no mínimo 48 dp; TalkBack nas telas de Editor e Configurações                                                                              |
| Estabilidade   | Rotação de tela e ida ao segundo plano não perdem o texto nem a posição enquanto o processo estiver vivo                                                                                       |
| Idioma         | Interface em português do Brasil; modelo de voz pt-BR                                                                                                                                          |

## Arquitetura e estrutura do projeto

MVVM com uma única fonte de verdade: o PrompterViewModel guarda o texto, o cursor e o estado da leitura; o motor de voz só emite eventos.

Fluxo de dados: Microfone → VoskEngine (SpeechEngine) → PartialReconciler e Aligner → PrompterViewModel → PromptText (tela) e SettingsRepository (DataStore, só ajustes).

O motor de voz e os toques alteram só o cursor no ViewModel; a tela apenas observa o cursor e rola até ele.

**Contrato do SpeechEngine.** A interface separa a captura de áudio das sessões de reconhecimento e emite resultado parcial, resultado final, nível de áudio e erro. Cada evento de reconhecimento carrega o id da sessão de escuta. Ao pausar, saltar por toque ou reiniciar o reconhecedor, a sessão muda e qualquer evento de sessão antiga é descartado. O contrato completo em Kotlin está no TRD. Trocar o motor reduz o trabalho de integração, mas outro reconhecedor pode exigir recalibrar os limites do alinhador.

**Estrutura de pastas**

```
app/src/main/
  assets/model-pt/              # vosk-model-small-pt-0.3
  java/.../tribuna/
    MainActivity.kt
    navigation/AppNavHost.kt
    ui/
      theme/                    # Material 3, cores, tipografia
      onboarding/OnboardingScreen.kt
      editor/EditorScreen.kt
      prompter/
        PrompterScreen.kt
        PromptText.kt           # texto com destaque, toque por palavra
        PauseOverlay.kt
        TimerBar.kt
      settings/
        QuickSettingsSheet.kt
        SettingsScreen.kt
    viewmodel/PrompterViewModel.kt
    speech/
      SpeechEngine.kt           # interface: captura, sessões, parciais, finais, erros
      VoskEngine.kt             # carrega modelo, laço de AudioRecord, Recognizer por sessão
      ModelInstaller.kt         # copia assets na 1ª execução
    align/
      Tokenizer.kt              # tokens + intervalos de caracteres + formas faladas
      Normalizer.kt             # acentos, pontuação, números por extenso
      PartialReconciler.kt      # reconciliação de hipóteses parciais
      Aligner.kt                # algoritmo de alinhamento
    data/SettingsRepository.kt  # DataStore
  test/.../align/AlignerTest.kt
```

A estrutura completa por pacote (incluindo ReadingStateMachine, DebugScreen e HardwareKeyHandler) está no TRD.

**Estados da leitura**

Idle → Countdown → Reading ⇄ Paused → Finished. Qualquer estado pode voltar a Idle por "Encerrar".

**Dependências**

- androidx.compose.material3, androidx.navigation:navigation-compose
- androidx.datastore:datastore-preferences
- com.alphacephei:vosk-android (+ net.java.dev.jna:jna como AAR)
- org.apache.commons:commons-text ou implementação própria de Levenshtein

**Testes**

- Unitários do Aligner e do PartialReconciler com transcrições simuladas: leitura normal, palavras erradas, pulo de parágrafo, improviso, repetição.
- Teste instrumentado do toque palavra vs. vazio no PromptText.
- Testes de interface com Compose para pausa e retomada, rotação durante a leitura, cancelamento da contagem por toque e Editar texto voltando à mesma palavra.
- O Aligner e o fluxo de leitura são testados com um SpeechEngine simulado (fake), sem depender do Vosk.

## Critérios de aceite, roadmap e prompts para o Claude Code

**Critérios de aceite do MVP**

- [ ] Com o modo avião ligado, colar um texto de 3.000 palavras e ler do início ao fim com o modo Voz.
- [ ] Tocar em área vazia pausa em menos de 100 ms; tocar de novo retoma da mesma palavra.
- [ ] Tocar numa palavra 3 parágrafos à frente move o cursor para ela e o acompanhamento continua dali.
- [ ] Pular um parágrafo inteiro falando: o texto salta em até 3 segundos, sem saltos errados.
- [ ] Parar de falar por 30 s: o texto não se move.
- [ ] Trocar fonte, cores, espelho e linha de leitura reflete na hora e persiste após reiniciar o app.
- [ ] Fechar o app e abrir de novo: o texto não está mais lá.
- [ ] Rotacionar a tela durante a leitura mantém a palavra atual.
- [ ] Tocar na tela durante a contagem regressiva cancela a contagem e pausa.
- [ ] Pausar por 10 s com alguém falando e retomar: o texto não salta.
- [ ] Encerrar a leitura mantém o texto no Editor; fechar o app apaga o texto.
- [ ] No tablet, o editor, a leitura em paisagem e em retrato e as Configurações seguem o layout próprio.

**Roadmap**

**Etapa 0 — Prova de conceito de voz (antes das telas).** App mínimo com Vosk, PartialReconciler e Aligner, testado no aparelho com um discurso real: leitura, improviso, repetição, silêncio e salto de parágrafo. Define as metas de memória, bateria e latência e verifica o alinhamento de 16 KB das bibliotecas nativas. A Voz e o Alinhador da semana 2 reaproveitam o código da Etapa 0 e são integrados ao app; as demais semanas seguem abaixo.

1. **Semana 1 — Esqueleto:** projeto Compose, navegação, Editor, Teleprompter com modo Constante, ajustes de aparência e DataStore.
2. **Semana 1 — Toques:** PromptText com destaque, toque palavra vs. vazio, pausa e arrasto.
3. **Semana 2 — Voz:** Vosk, instalação do modelo, permissão, tela de depuração com transcrição bruta.
4. **Semana 2 — Alinhador:** Normalizer, Tokenizer, PartialReconciler, Aligner com testes unitários; ligar ao cursor.
5. **Semanas 3–4 — Calibração:** ensaiar com discursos reais, ajustar parâmetros, cronômetro e avisos, botões de volume.

**Prompts para o Claude Code** (um por etapa; faça a prova de conceito de voz primeiro)

Ordem recomendada: prompts 4 e 5 (voz e alinhador) num projeto mínimo; depois os prompts 1, 2 e 3.

1. "Crie um projeto Android em Kotlin com Jetpack Compose e Material 3, sem XML de layout, minSdk 26, targetSdk 36. Telas: Onboarding, Editor, Teleprompter, Configurações, com Navigation Compose e layout adaptável a celular e tablet (classes de tamanho de janela do Material 3). Siga docs/PRD.md (Telas e Requisitos funcionais) e docs/TRD.md (Módulos, Layout adaptativo)."
2. "Implemente PromptText: renderiza o texto com BasicText + AnnotatedString, destaca a palavra do índice atual, esmaece as anteriores e detecta toque em palavra vs. área vazia usando getOffsetForPosition + getBoundingBox, conforme a seção Interações e gestos do PRD e Renderização e toque do TRD."
3. "Implemente SettingsRepository com DataStore para todos os ajustes A1–A14 (com padrões de tablet para A1 e A5) e a bottom sheet de Ajustes rápidos com prévia ao vivo."
4. "Integre com.alphacephei:vosk-android, copie o modelo de assets/model-pt na primeira execução e implemente VoskEngine atrás da interface SpeechEngine exatamente como o contrato do TRD (captura própria, sessões com SessionId, Partial, Final, Level, Error). Adicione a tela de depuração."
5. "Implemente Normalizer, Tokenizer, PartialReconciler e Aligner exatamente como as seções Contratos e Motor de voz do TRD, com os parâmetros por sensibilidade do PRD, e escreva testes unitários para leitura normal, erros de reconhecimento, revisões de parciais, pulo de parágrafo e improviso."

Dica: mantenha este PRD como docs/PRD.md e o TRD como docs/TRD.md na raiz do repositório e cite-os em cada prompt; o Claude Code mantém o contexto do projeto entre as etapas.
