# TRD — Tribuna: Requisitos Técnicos (Android Offline)

10 de outubro de 2026 · Giorgio Derico

O Tribuna é um app Android nativo em Kotlin + Jetpack Compose, de uma Activity, com reconhecimento de voz offline (Vosk) atrás da interface SpeechEngine. Nenhum dado sai do aparelho e o texto do discurso vive só na memória do processo. Este TRD detalha como construir o PRD aprovado em 10/10/2026.

## Stack e versões

Tudo nativo, sem ponte entre linguagens: áudio, reconhecimento, alinhamento e rolagem rodam no mesmo processo. As versões exatas das bibliotecas são as estáveis mais recentes no dia da criação do projeto, travadas no version catalog.

| Camada            | Escolha                                                                 | Observação                                                 |
|-------------------|-------------------------------------------------------------------------|------------------------------------------------------------|
| Linguagem         | Kotlin 2.x                                                              | Coroutines e StateFlow em todo o fluxo de estado           |
| Interface         | Jetpack Compose + Material 3                                            | Sem XML de layout; versões pelo Compose BOM                |
| Navegação         | Navigation Compose                                                      | Uma Activity; rotas Onboarding, Editor, Prompter, Settings |
| Layout adaptativo | material3-window-size-class                                             | Celular e tablet a partir das mesmas telas                 |
| Voz offline       | com.alphacephei:vosk-android + JNA (AAR)                                | Modelo vosk-model-small-pt-0.3 em assets                   |
| Preferências      | Jetpack DataStore (Preferences)                                         | Só ajustes visuais e de comportamento, nunca o texto       |
| Similaridade      | Levenshtein próprio                                                     | Evita dependência extra; commons-text como alternativa     |
| SDK               | minSdk 26 (Android 8.0), targetSdk 36, compileSdk 36                    | targetSdk 36 é exigido pelo Google Play desde 31/08/2026   |
| Build             | Android Studio, Gradle Kotlin DSL, version catalog (libs.versions.toml) | AGP estável mais recente                                   |
| Testes            | JUnit, kotlinx-coroutines-test, Compose UI Test                         | SpeechEngine falso para testar sem Vosk                    |

No Claude Code, peça sempre "Kotlin, Jetpack Compose, sem XML" para evitar Views antigas.

## Arquitetura

```
Microfone -> VoskEngine (SpeechEngine) -> eventos + sessão -> PrompterViewModel
PrompterViewModel <-> align (reconciliação de hipóteses, Normalizer, Aligner): palavras -> posição
PrompterViewModel -> estado -> Telas em Compose; toques e teclas -> PrompterViewModel
SettingsRepository (DataStore, só ajustes) -> Telas e sensibilidade do align
```

O motor de voz emite eventos marcados com a sessão; o ViewModel passa as palavras ao alinhador, recebe a nova posição e publica o estado que as telas desenham. Toques e teclas voltam ao ViewModel como eventos, e os ajustes chegam do DataStore.

## Módulos e responsabilidades

O PrompterViewModel é a única fonte de verdade do texto, do cursor e do estado da leitura. Motor de voz, toques e botões só enviam eventos; a tela só observa e desenha.

| Pacote        | Classes                                                              | Responsabilidade                                                    | Pode alterar                                    |
|---------------|----------------------------------------------------------------------|---------------------------------------------------------------------|-------------------------------------------------|
| ui.onboarding | OnboardingScreen, ModelSetupScreen                                   | Boas-vindas, permissão de microfone, cópia do modelo, erro da cópia | Nada além da navegação                          |
| ui.editor     | EditorScreen                                                         | Texto, Colar/Limpar, contagem, modo, Iniciar                        | Texto (via ViewModel)                           |
| ui.prompter   | PrompterScreen, PromptText, PauseOverlay, TimerBar, CountdownOverlay | Leitura, toques, pausa, avisos de tempo                             | Envia eventos ao ViewModel                      |
| ui.settings   | QuickSettingsSheet, SettingsScreen, DebugScreen                      | Ajustes A1–A14, R2–R6, D1–D5, C4–C6; depuração oculta               | Ajustes (via repository)                        |
| viewmodel     | PrompterViewModel, ReadingStateMachine                               | Texto, tokens, cursor, estado da leitura, cronômetro                | Cursor e estado (exclusivo)                     |
| speech        | SpeechEngine, VoskEngine, ModelInstaller, FakeSpeechEngine (teste)   | Captura e reconhecimento; emite eventos com id de sessão            | Nada; só emite                                  |
| align         | Normalizer, Tokenizer, PartialReconciler, Aligner                    | Normalização, tokens, reconciliação de hipóteses, alinhamento       | Nada; funções puras que retornam a nova posição |
| data          | SettingsRepository                                                   | DataStore dos ajustes                                               | Ajustes                                         |
| input         | HardwareKeyHandler                                                   | Volume e teclado Bluetooth (C4, C5)                                 | Envia eventos ao ViewModel                      |

Regra: align não conhece Android. Isso deixa o alinhador testável em JVM puro e independente do motor de voz.

## Contratos

Estes contratos são a fronteira entre os agentes de motor de voz e de interface. Mudar qualquer um deles exige nova aprovação da arquitetura.

**SpeechEngine** — separa a captura de áudio do reconhecimento. A captura fica aberta durante toda a leitura (inclusive na pausa, como pede o PRD); o reconhecimento acontece em sessões, e cada sessão tem um reconhecedor limpo.

```kotlin
interface SpeechEngine {
    val events: Flow<SpeechEvent>
    val state: StateFlow<EngineState>
    suspend fun prepare(): Result<Unit>                     // carrega o modelo
    fun startCapture()                                      // abre o microfone
    fun stopCapture()                                       // fecha o microfone
    fun beginSession(vocabulary: List<String>? = null): SessionId
    fun endSession()                                        // descarta hipóteses e áudio pendente
    fun release()
}

@JvmInline value class SessionId(val value: Long)

sealed interface SpeechEvent {
    data class Partial(val session: SessionId, val words: List<String>) : SpeechEvent
    data class Final(val session: SessionId, val words: List<String>) : SpeechEvent
    data class Level(val rms: Float) : SpeechEvent           // da captura, sem sessão
    data class Error(val session: SessionId?, val cause: EngineError) : SpeechEvent
}

enum class EngineState { Idle, Preparing, Ready, Capturing, Recognizing, Failed }
enum class EngineError { ModelMissing, ModelCorrupt, MicUnavailable, PermissionRevoked, Internal }
```

Regras do contrato:

- beginSession cria o SessionId e o reconhecedor juntos. O id é capturado no listener daquele reconhecedor na criação; nenhum callback lê uma variável de "sessão atual". Assim, um callback atrasado de uma sessão antiga chega com o id antigo.
- endSession para de alimentar o reconhecedor, descarta o áudio ainda não processado e libera o reconhecedor. Eventos que ainda chegarem dele carregam o id antigo.
- O ViewModel só aceita Partial, Final e Error cujo id seja a sessão ativa. Sem sessão ativa (pausa, contagem), todo evento de reconhecimento é ignorado.
- Entre sessões, o áudio capturado é lido e descartado, sem ir ao reconhecedor. Level continua sendo emitido para o indicador de microfone.

**Token** — separa o que aparece na tela das formas faladas aceitas. Destaque, toque e progresso usam o token; o alinhamento usa as formas faladas.

```kotlin
data class Token(
    val index: Int,
    val display: IntRange,              // caracteres no texto original
    val spoken: List<List<String>>,     // formas faladas aceitas, cada uma com 1+ palavras
    val weight: Float,                  // menor para palavras comuns
    val sentenceStart: Boolean,         // para Volume − e frase anterior
)
```

Exemplos: "24" vira um token com `spoken = [[vinte, e, quatro]]`. "3:16" vira um token com `spoken = [[tres, dezesseis], [tres, e, dezesseis], [capitulo, tres, versiculo, dezesseis]]`. Intervalos e listas de versículos também são um único token: "13:4-7" tem `spoken = [[treze, quatro, a, sete], [treze, de, quatro, a, sete], [capitulo, treze, versiculos, quatro, a, sete]]`, e "12:1, 2" tem `spoken = [[doze, um, e, dois], [doze, versiculos, um, e, dois], [capitulo, doze, versiculos, um, e, dois]]`. Qualquer variante falada avança o cursor para depois desse token inteiro; o progresso conta tokens, não palavras faladas.

**Aligner** — função pura: recebe o estado e as palavras confirmadas e devolve o novo estado. Não conhece Android nem o motor.

```kotlin
class Aligner(private val tokens: List<Token>, private val params: AlignParams) {
    fun advance(state: AlignState, words: List<String>): AlignState
    fun reposition(tokenIndex: Int): AlignState           // toque, Volume −, arrasto, troca de modo
}

data class AlignState(
    val cursor: Int,
    val highWater: Int,          // maior posição confirmada desde o último reposicionamento
    val missStreak: Int,         // resultados seguidos sem avanço, para a recuperação
    val lastMove: Move,
    val score: Double,
)
enum class Move { Stay, Short, LongJump, Recovery, Reposition }

data class AlignParams(
    val k: Int, val window: Int,
    val shortThreshold: Double, val longThreshold: Double,
)
```

Invariante: um movimento automático nunca leva o cursor abaixo de highWater − 3. Só reposition move o cursor livremente, e ele redefine cursor e highWater para a nova posição.

**Estado da leitura** — exposto pelo ViewModel como um único StateFlow.

```kotlin
data class PrompterUiState(
    val text: String,
    val tokens: List<Token>,
    val cursor: Int,
    val phase: ReadingPhase,
    val mode: ScrollMode,                         // Voice | Constant
    val elapsedMs: Long,
    val targetMs: Long?,
    val warning: TimeWarning,                     // None | FiveMin | OneMin
    val micLevel: Float,
    val micAvailable: Boolean,
)

sealed interface ReadingPhase {
    data object Idle : ReadingPhase
    data class Countdown(val secondsLeft: Int) : ReadingPhase
    data object Reading : ReadingPhase
    data object Paused : ReadingPhase
    data class Finished(val totalMs: Long) : ReadingPhase
}
```

**Ajustes** — um objeto imutável lido do DataStore; os padrões são os da tabela A1–A14 do PRD (destaque verde-menta, fundo azul-petróleo escuro). Fonte (A1) e margem (A5) têm padrão próprio no tablet; ver Layout adaptativo.

```kotlin
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
```

## Máquina de estados da leitura

A leitura passa por Idle → Countdown → Reading ⇄ Paused → Finished, e qualquer estado volta a Idle por Encerrar. As transições ficam em ReadingStateMachine, uma função pura (estado, evento) → estado + efeitos, testada sem interface. Os efeitos de voz são sempre um destes: beginSession, endSession, startCapture, stopCapture, reposition.

| Evento                                   | Countdown                                    | Reading (modo Voz)                                               | Paused                                          |
|------------------------------------------|----------------------------------------------|------------------------------------------------------------------|-------------------------------------------------|
| Entrada na fase                          | startCapture (indicador de microfone ligado) | —                                                                | —                                               |
| Contagem chega a zero                    | Reading; beginSession                        | —                                                                | —                                               |
| Toque em área vazia                      | Paused (cancela a contagem)                  | Paused; endSession; captura continua e a fala é descartada       | Reading; beginSession                           |
| Toque em palavra                         | Paused (cancela a contagem)                  | endSession, reposition na palavra, beginSession                  | Reading; reposition na palavra; beginSession    |
| Toque longo em palavra                   | —                                            | —                                                                | Idle; abre o Editor na palavra                  |
| Arrastar                                 | —                                            | endSession; rolagem manual                                       | Rolagem manual                                  |
| Soltar após arrastar                     | —                                            | reposition na linha de leitura; beginSession                     | reposition na linha de leitura; continua Paused |
| Volume +                                 | —                                            | Paused; endSession                                               | Reading; beginSession                           |
| Volume −                                 | —                                            | endSession, reposition no início da frase anterior, beginSession | reposition no início da frase anterior          |
| Voltar (sistema)                         | Idle; stopCapture                            | Paused; endSession                                               | Diálogo "Encerrar leitura?"                     |
| Cursor no último token por 2 s           | —                                            | Finished; endSession; stopCapture                                | —                                               |
| Rotação ou outra mudança de configuração | Sem efeito                                   | Sem efeito                                                       | Sem efeito                                      |
| Ida real ao segundo plano                | Paused; stopCapture                          | Paused; endSession; stopCapture                                  | stopCapture                                     |
| Volta do segundo plano                   | —                                            | —                                                                | Continua Paused; startCapture                   |
| Erro do motor                            | —                                            | Paused + aviso; oferece modo Constante                           | —                                               |

Regras transversais:

- **Cronômetro.** Conta desde o fim da contagem até Finished ou Encerrar, inclusive em Paused.
- **Sessão ativa.** Só existe em Reading no modo Voz. Retomar sempre abre uma sessão nova, com hipótese limpa; nada ouvido durante a pausa influencia o cursor.
- **Microfone na pausa.** A captura continua aberta, como pede o PRD, para o indicador de microfone e para a retomada ser imediata. O áudio é descartado sem ir ao reconhecedor.
- **Modo Constante.** Mesmas transições, sem sessões de voz e sem captura. A rolagem conduz o cursor (ver Renderização e toque).
- **Troca de modo na pausa.** Voz → Constante: stopCapture; a rolagem parte da posição atual. Constante → Voz: startCapture e reposition no cursor atual; a sessão abre ao retomar.
- **Encerrar.** Volta a Idle mantendo o texto e o cursor; stopCapture. Só fechar o app apaga o texto.

## Motor de voz

O motor transforma áudio em palavras confirmadas e o alinhador transforma palavras em posição; na dúvida, o cursor fica parado.

**Instalação do modelo**

1. Na primeira execução, ModelInstaller copia assets/model-pt para o armazenamento interno com o StorageService do Vosk, mostrando o progresso na tela 1b.
2. Um arquivo de versão dentro do modelo evita cópias repetidas; versão diferente = nova cópia.
3. Falha (sem espaço, arquivo corrompido) leva à tela 1d: Tentar de novo ou Usar só o modo Constante.
4. O modelo é carregado uma vez por processo e mantido enquanto o app estiver vivo.

**Captura e sessões**

- VoskEngine tem um laço próprio de AudioRecord (16 kHz, mono, PCM 16 bits) e alimenta o Recognizer do Vosk diretamente, em vez de usar o SpeechService. Isso separa a captura (aberta na leitura e na pausa) do reconhecimento (só dentro de uma sessão).
- beginSession cria um Recognizer novo e o SessionId ao mesmo tempo; o id fica preso ao reconhecedor e vai em todos os eventos dele. Se a Etapa 0 mostrar que criar um reconhecedor por sessão é lento, usa-se reset() no mesmo reconhecedor, com um id novo.
- endSession para de alimentar o reconhecedor, esvazia o buffer de áudio ainda não processado e libera o reconhecedor.
- Fora de sessão, os quadros de áudio são lidos e descartados; só o nível (Level) segue para o indicador de microfone.
- Os resultados do Recognizer chegam como JSON e viram Partial e Final. O processamento (reconciliação, normalização, alinhamento) roda em Dispatchers.Default; só o cursor resultante vai para o StateFlow lido pela tela.
- Abaixo de um limiar mínimo de nível de áudio, parciais são ignorados (ruído e eco de salão). O limiar é calibrado na Etapa 0.

**Reconciliação de hipóteses** (PartialReconciler)

O Vosk reescreve a hipótese parcial enquanto ouve mais áudio, inclusive no começo da frase. Por isso o reconciliador não conta palavras consumidas; ele compara sequências.

1. Para cada frase em curso (do primeiro parcial até o Final), guarda confirmadas: as palavras já entregues ao alinhador.
2. A cada Partial, calcula o prefixo comum entre ele e o parcial anterior. Só esse prefixo estável é candidato a confirmação.
3. Alinha confirmadas ao prefixo estável por distância de edição em nível de palavra. Isso localiza no prefixo o ponto que corresponde ao fim de confirmadas, mesmo que o Vosk tenha inserido, removido ou corrigido palavras antes dele.
4. Entrega ao alinhador só as palavras depois desse ponto e as acrescenta a confirmadas.
5. No Final, faz o mesmo alinhamento contra o resultado final, entrega o que faltar e zera o estado da frase.
6. Palavras já entregues nunca são retiradas. Um erro que o Vosk corrigir depois é absorvido pelo alinhador, que só avança com correspondência confiável e não recua sozinho.
7. Evento de sessão que não é a ativa é descartado antes do reconciliador.

**Normalização e tokens** (Normalizer, Tokenizer)

- Minúsculas, sem acentos e sem pontuação, aplicado ao roteiro e à fala.
- Cada token guarda o intervalo exibido no texto original e uma ou mais formas faladas (ver Contratos). Números geram a forma por extenso; referências como "João 3:16" geram as variantes "três dezesseis", "três e dezesseis" e "capítulo três versículo dezesseis".
- O nome do livro ("João") é um token próprio; a referência numérica é um único token, qualquer que seja a variante falada.
- Intervalos ("13:4-7", também com travessão ou "a") e listas de versículos ("12:1, 2", "12:1, 2, 5") formam um único token com o capítulo. As formas faladas combinam o capítulo com "a" para intervalos e "e" antes do último item de listas, com e sem "de", "versículo(s)" e "capítulo". Livros com número ("1 Coríntios") geram "primeira coríntios", "primeiro coríntios" e "um coríntios" no token do livro.
- Números sem referência seguem a forma por extenso usual: inteiros, anos ("1914" → "mil novecentos e catorze" ou "quatorze"; grafias alternativas viram formas faladas alternativas), decimais com vírgula ("3,5" → "três vírgula cinco") e porcentagem ("%" → "por cento").
- Linhas iniciadas por // (T5) viram notas: aparecem na tela, mas não geram tokens.

**Alinhamento** (Aligner, parâmetros por sensibilidade conforme o PRD)

1. O roteiro é visto como uma sequência de palavras faladas, cada uma apontando para o token de origem. Um token com variantes ocupa a variante que melhor casar.
2. Com as últimas *k* palavras confirmadas, busca na janela `[max(cursor − 3, highWater − 3), cursor + W]` a posição de maior similaridade média ponderada (1 − Levenshtein / maior comprimento; palavras comuns com peso menor).
3. Avança se a distância for até 5 tokens com similaridade ≥ limite curto, ou se a similaridade for ≥ limite longo. Ao avançar, highWater = max(highWater, cursor).
4. **Recuperação:** depois de 2 resultados seguidos sem avanço, busca as últimas 4 ou mais palavras só à frente de highWater, até o fim do roteiro. Salta apenas se existir uma única posição acima do limite longo; com dois ou mais candidatos próximos do limite, o cursor fica parado.
5. Nenhum movimento automático leva o cursor abaixo de highWater − 3. Assim, recuos pequenos não se acumulam: o limite é medido sempre contra a maior posição confirmada, não contra a última.
6. Voltar mais do que isso só por reposition (toque, Volume −, arrasto), que redefine cursor e highWater.

**Vocabulário restrito.** O Vosk aceita uma lista de palavras na criação do Recognizer (formas faladas do roteiro + `[unk]`). Pode melhorar a precisão, mas piorar o improviso; a Etapa 0 decide se fica ligado, desligado ou configurável.

## Renderização e toque

O texto inteiro é um único BasicText com AnnotatedString, medido uma vez e reaproveitado até algo que afeta o layout mudar.

**PromptText**

- Estilos por intervalo: texto já lido com a opacidade A9, palavra atual com a cor de destaque A8, notas // com a cor secundária.
- O TextLayoutResult fica em cache e só é recalculado quando mudam fonte, peso, tamanho, espaçamento, margem, largura disponível ou orientação. Mudar o cursor só troca estilos, sem nova medição de linhas.
- Meta: discurso de 8.000 palavras sem queda de fluidez. Se a Etapa 0 ou os testes mostrarem custo alto, o texto passa a ser dividido em parágrafos dentro de uma LazyColumn, mantendo o mesmo contrato de tokens.
- Espelho horizontal e vertical (A12) via graphicsLayer com escala −1, aplicado ao conteúdo e ao mapeamento de toque.

**Hit-test: palavra ou vazio**

1. getOffsetForPosition dá o caractere mais próximo do toque, mesmo longe do texto.
2. Por isso o toque só conta como palavra se cair dentro do retângulo da palavra (união dos getBoundingBox dos seus caracteres), ampliado em 8 dp.
3. Entre linhas, margens e depois do fim do texto = vazio.
4. O mapa caractere → token é uma busca binária nos intervalos de Token.
5. Movimento acima do touchSlop vira arrasto; abaixo, toque. Um toque longo usa o timeout padrão do sistema.

**Rolagem**

- A direção do controle depende do modo, e nunca há os dois ao mesmo tempo:
  - **Modo Voz:** o cursor conduz a rolagem. Cada novo cursor anima o scroll até a linha da palavra ficar na linha de leitura (A10), com duração R5 (150–600 ms) e interpolação suave. Um novo cursor no meio da animação redireciona a animação em curso, sem fila.
  - **Modo Constante:** a rolagem conduz o cursor. O scroll avança por tempo, na velocidade de 1 a 10 mapeada para dp/s (tabela definida na calibração). O cursor passa a ser o token que cruza a linha de leitura; essa atualização só troca estilos e não dispara animação.
- O ViewModel expõe a origem de cada mudança de cursor (alinhamento, reposicionamento ou rolagem); a tela só anima quando a origem não é a própria rolagem.
- Salto por toque: a palavra pisca no destaque por 300 ms.

**Tela durante a leitura**

- Modo imersivo (WindowInsetsControllerCompat, barras do sistema ocultas).
- FLAG_KEEP_SCREEN_ON só nas fases Countdown, Reading e Paused (D4).
- Brilho máximo opcional via screenBrightness da janela, restaurado ao sair da leitura.

## Ciclo de vida, privacidade e permissões

O app pede só o microfone, não tem acesso à rede e não grava nada sobre o discurso em disco.

**Manifesto**

- Única permissão: RECORD_AUDIO. Sem INTERNET, sem ACCESS_NETWORK_STATE. A tela ligada usa a flag da janela, então WAKE_LOCK não é necessário.
- android:allowBackup="false" e regras de extração de dados vazias, para nada ir ao backup na nuvem.
- O build verifica o manifesto final mesclado: falha se alguma biblioteca trouxer INTERNET.

**Onde o texto vive**

- Somente no PrompterViewModel, em memória. Sobrevive a rotação e à ida ao segundo plano enquanto o processo estiver vivo.
- Proibido guardar o texto em SavedStateHandle, rememberSaveable, DataStore, arquivo ou banco. Isso é o que garante que fechar o app apaga o texto.
- Se o Android encerrar o processo, o texto some e o app volta ao Editor vazio. É o comportamento esperado.
- O texto colado nunca vai para log. Em release, o R8 remove as chamadas de Log; a tela de depuração mostra a transcrição só em memória.
- O áudio só passa pelo reconhecedor e nunca é gravado.

**Segundo plano**

- Rotação e outras mudanças de configuração recriam a Activity, mas não são ida ao segundo plano: no onStop, se isChangingConfigurations for verdadeiro, nada muda. O ViewModel, a captura e a sessão seguem vivos.
- Ida real ao segundo plano (onStop com isChangingConfigurations falso): leitura vai para Paused, endSession e stopCapture. Na volta, a leitura continua Paused e a captura reabre; o orador toca para continuar.
- Sem serviço em primeiro plano na v1: durante a leitura a tela fica ligada e a Activity visível, então não há captura em segundo plano.

**Microfone**

- Permissão pedida na tela 1a, com a explicação do motivo.
- Negada: modo Voz desabilitado no Editor, com link para permitir (tela 2c).
- Revogada com o app aberto: o motor emite PermissionRevoked, a leitura pausa e o app oferece o modo Constante.

**Publicação na Play Store**

- O formulário de segurança de dados declara que nenhum dado é coletado nem compartilhado.
- Política de privacidade publicada, por causa da permissão de microfone, com o mesmo texto da seção Sobre: nada sai do aparelho. Detalhes no documento de Conformidade (docs/CONFORMIDADE.md).

## Layout adaptativo

Celular e tablet usam as mesmas telas e o mesmo ViewModel; só a disposição muda, decidida pela classe de largura da janela do Material 3 (Compacta abaixo de 600 dp, Média de 600 a 840 dp, Expandida acima de 840 dp).

| Tela                 | Compacta (celular)                                                              | Média e Expandida (tablet)                                                                  |
|----------------------|---------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| Boas-vindas e modelo | Coluna única                                                                    | Conteúdo centralizado, largura máxima                                                       |
| Editor               | Texto em tela cheia; Colar, Limpar, contagem, modo, Aparência e Iniciar embaixo | Duas colunas: texto à esquerda; contagem, Colar/Limpar, modo, Aparência e Iniciar à direita |
| Ajustes rápidos      | Bottom sheet                                                                    | Bottom sheet com largura máxima ou painel lateral                                           |
| Leitura              | Tela cheia                                                                      | Tela cheia em retrato e paisagem; margem A5 vale sobre a largura total                      |
| Pausa                | Painel inferior sobre o texto                                                   | Painel inferior centralizado, largura máxima                                                |
| Configurações        | Lista de seções em rolagem única                                                | Menu lateral de seções e detalhe à direita                                                  |

- **Orientação no celular:** o ajuste A14 (Retrato, Paisagem, Automática) usa requestedOrientation, aplicado só na tela de leitura.
- **Orientação no tablet:** com targetSdk 36, o Android 16 ignora restrições de orientação e de redimensionamento em telas grandes (menor largura de 600 dp ou mais). Todas as telas funcionam em retrato e paisagem, e o A14 fica oculto quando a menor largura da tela é de 600 dp ou mais. O PRD já reflete essa regra.
- **Padrões no tablet:** fonte (A1) 56 sp e margem (A5) 12%, contra 44 sp e 8% no celular. O tipo de aparelho é decidido uma vez pela menor largura da tela física (600 dp ou mais = tablet), não pela janela, para os padrões não mudarem em tela dividida. AppSettings guarda A1 e A5 como opcionais: enquanto a pessoa não mexe no ajuste, vale o padrão do aparelho; depois que ela ajusta, vale o valor salvo.
- O app também funciona em janela redimensionável e em tela dividida: o layout segue a classe de largura da janela, não o tipo de aparelho.
- Rotacionar durante a leitura mantém a palavra atual: o layout é remedido e a rolagem reposiciona o cursor na linha de leitura sem animação, sem pausar nem reabrir a sessão.
- Prévias do Compose para as duas classes e as duas orientações em cada tela, para o agente de interface e o Revisor conferirem sem aparelho.

## Metas de desempenho

As metas marcadas como provisórias só viram compromisso depois da medição na Etapa 0; as demais já valem como critério do Revisor. A latência é medida em três trechos separados, para saber onde está o atraso.

| Métrica                          | Meta                                                                                                 | Como medir                                                                                                   | Situação   |
|----------------------------------|------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|------------|
| Rolagem                          | 60 fps em aparelho de entrada (3 GB de RAM, Android 10)                                              | Macrobenchmark de rolagem com texto de 8.000 palavras; jank abaixo de 5% dos quadros                         | Fixa       |
| Latência 1: reconhecimento       | A fixar                                                                                              | Do fim da palavra no áudio de teste (marca de tempo conhecida) até o primeiro Partial que a contém           | Provisória |
| Latência 2: confirmação e cursor | A fixar                                                                                              | Do Partial até o cursor atualizado, incluindo a espera pela palavra estável                                  | Provisória |
| Latência 3: animação             | Igual à duração R5 configurada                                                                       | Do cursor atualizado até o fim da animação                                                                   | Fixa       |
| Latência total fala → rolagem    | Abaixo de 500 ms (soma dos três trechos, sem a animação)                                             | Soma dos tempos acima no modo de depuração                                                                   | Provisória |
| Atraso do cursor                 | Até 3 palavras atrás da fala em 95% do tempo, ambiente silencioso                                    | Trajetória do cursor contra a trajetória esperada do áudio de teste                                          | Provisória |
| Saltos errados                   | Zero em discurso de 30 min                                                                           | Trajetória completa: qualquer posição fora do trecho falado conta, mesmo que o cursor termine no lugar certo | Fixa       |
| Início da escuta (quente)        | Menos de 2 s do fim da contagem regressiva à primeira palavra reconhecida, com o modelo já carregado | Timestamp no modo de depuração                                                                               | Provisória |
| Inicialização a frio             | Medida, sem meta até a Etapa 0                                                                       | Da abertura do app com o processo encerrado até o modelo pronto; a primeira cópia do modelo é medida à parte | Provisória |
| Memória                          | A fixar; o Vosk cita cerca de 300 MB para modelos pequenos                                           | Android Studio Profiler durante 45 min de leitura                                                            | Provisória |
| Bateria                          | Até 15% em 45 min com brilho máximo, aparelho intermediário                                          | Bateria antes e depois, brilho fixo, mesmo discurso                                                          | Provisória |
| Tamanho                          | AAB abaixo de 80 MB com o modelo                                                                     | Tamanho do AAB e do download por ABI no Play Console                                                         | Fixa       |
| Toque para pausar                | Menos de 100 ms                                                                                      | Teste de interface medindo o tempo até o estado Paused                                                       | Fixa       |

**Protocolo de medição** (definido na Etapa 0 e repetido a cada versão)

- Dois aparelhos fixos: um de entrada e um intermediário, mais o tablet.
- Microfone a cerca de 50 cm do orador, em sala silenciosa e com eco de salão.
- Brilho fixo, modo avião ligado.
- O mesmo conjunto de textos de teste em todas as rodadas, com a trajetória esperada (palavra × tempo) anotada para cada gravação.

## Testes, qualidade e distribuição

O Revisor só aprova uma entrega com todas as suítes abaixo verdes; o reconciliador, o alinhador e a máquina de estados têm cobertura completa porque concentram o risco do produto. Testes de voz avaliam a trajetória do cursor, não só a posição final.

| Suíte                      | Ferramenta                       | O que cobre                                                                                                                                                                                                                                                      |
|----------------------------|----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Reconciliação de hipóteses | JUnit (JVM)                      | Sequências de parciais com inserção, remoção e correção no começo da frase; Final diferente do último parcial; frase repetida; nenhuma palavra entregue duas vezes nem perdida                                                                                   |
| Alinhamento                | JUnit (JVM)                      | Normalizer, Tokenizer e Aligner: leitura normal, palavras erradas, pulo de parágrafo, improviso, repetição, trechos repetidos no roteiro, números e variantes de referências; invariante highWater − 3 em recuos sucessivos; recuperação ambígua mantém o cursor |
| Sessões e leitura          | JUnit + kotlinx-coroutines-test  | ReadingStateMachine e PrompterViewModel com FakeSpeechEngine: callback atrasado de sessão antiga descartado, fala durante a pausa ignorada, cronômetro na pausa, avisos de 5 e 1 min, troca de modo                                                              |
| Interface                  | Compose UI Test                  | Toque palavra vs. vazio, pausa e retomada, cancelamento da contagem, rotação sem pausar e mantendo a palavra, Editar texto voltando à palavra, Editor sem microfone, modo Constante sem animação em laço                                                         |
| Integração de voz          | Teste instrumentado no aparelho  | VoskEngine com áudios de textos de teste; compara a trajetória do cursor com a esperada (atraso em 95% do tempo, nenhum salto errado)                                                                                                                            |
| Desempenho                 | Macrobenchmark                   | Rolagem com texto de 8.000 palavras, início quente e a frio                                                                                                                                                                                                      |
| Golden de transcrições     | Arquivos de teste no repositório | Transcrições brutas de textos de teste gravados na Etapa 0, reusadas como regressão do reconciliador e do alinhador                                                                                                                                              |

**Privacidade nos testes**

- Áudios e transcrições do repositório vêm só de textos de teste escritos para isso. Discursos pessoais não entram no repositório.
- A exportação de transcrições existe só no projeto experimental da Etapa 0 e no build de depuração; o código dela não entra no app distribuído.

**Qualidade de código**

- Android Lint sem erros; ktlint ou detekt para estilo.
- Verificação do manifesto mesclado (sem INTERNET) como tarefa do build.
- Verificação de alinhamento de 16 KB das bibliotecas nativas (libvosk.so, libjnidispatch.so) em todas as ABIs, no build de release: alinhamento dos segmentos ELF e do pacote (`zipalign -c -P 16`). Falha bloqueia a entrega.

**Distribuição**

- Android App Bundle (AAB): o Play entrega a cada aparelho só a biblioteca nativa do seu ABI.
- Suporte a páginas de memória de 16 KB, exigido pelo Google Play para apps com target do Android 15 ou mais. Atualizar o Gradle e o AGP não basta: os binários do Vosk e do JNA precisam vir compilados com esse alinhamento. Se a versão publicada não vier, usar uma versão mais nova ou recompilar a partir do código-fonte.
- R8 ligado no release, com regras para manter as classes do Vosk e do JNA (`org.vosk.**`, `com.sun.jna.**`), que são acessadas por código nativo.
- O modelo fica em assets sem compressão extra; se o tamanho passar da meta, avaliar Play Asset Delivery na v1.1.
- Assinatura pelo Play App Signing; versão e código de versão no version catalog.

## Etapa 0: spike de voz

A Etapa 0 prova, num app mínimo e no aparelho real, que o Vosk mais o reconciliador e o alinhador acompanham um discurso, e que as bibliotecas nativas podem ser publicadas. Ela vem antes da aprovação da arquitetura; se falhar, troca-se o motor atrás do SpeechEngine antes de construir as telas.

**O que construir** (projeto experimental, separado do app)

1. Projeto Kotlin + Compose com uma tela: campo de texto, botões Ouvir e Pausar, e o texto com a palavra atual destacada.
2. SpeechEngine e VoskEngine com captura própria e sessões, mais ModelInstaller, conforme os contratos deste TRD.
3. Normalizer, Tokenizer (com formas faladas), PartialReconciler e Aligner, com os unitários.
4. Painel de depuração: hipóteses brutas, palavras confirmadas, similaridade de cada avanço, tipo de movimento, highWater e os três trechos de latência.
5. Exportação de transcrições e trajetórias para arquivo, só neste projeto e só com textos de teste, para criar os golden. Essa função não passa para o app.

**O que testar** (com o protocolo de medição)

- Leitura normal de um texto de teste de 30 min.
- Improviso de alguns minutos no meio.
- Repetição de frases e trechos repetidos no roteiro.
- Silêncio de 30 s.
- Pausa com alguém falando e retomada.
- Pulo de um parágrafo inteiro.
- Referências bíblicas e números, em todas as variantes faladas.
- Vocabulário restrito ligado e desligado.
- Um reconhecedor novo por sessão contra reset() do mesmo reconhecedor.
- Alinhamento de 16 KB de libvosk.so e libjnidispatch.so num build de release.

**Critérios de saída**

- [ ] Cursor até 3 palavras atrás da fala em 95% do tempo, em ambiente silencioso, medido pela trajetória.
- [ ] Nenhum salto errado em toda a trajetória do texto de 30 min.
- [ ] Pulo de parágrafo recuperado em até 3 s.
- [ ] Silêncio de 30 s e fala durante a pausa sem movimento do texto.
- [ ] Bibliotecas nativas alinhadas a 16 KB em todas as ABIs, ou caminho definido (versão mais nova ou recompilação).
- [ ] Memória, bateria, latência por trecho e inicialização a frio medidas e registradas; metas provisórias fixadas.
- [ ] Parâmetros de sensibilidade e limiar de áudio calibrados.
- [ ] Decisões sobre vocabulário restrito e sobre reconhecedor por sessão registradas.

Se os critérios não forem atingidos após a calibração, a Etapa 0 volta para você com o que falhou e as alternativas de motor, antes de qualquer tela.

## Riscos técnicos e decisões em aberto

Os dois maiores riscos são a precisão do reconhecimento em português em salões com eco e o alinhamento de 16 KB das bibliotecas nativas do Vosk; os dois se resolvem na Etapa 0.

| Risco                                                             | Impacto                                   | Mitigação                                                                                       | Quando se resolve |
|-------------------------------------------------------------------|-------------------------------------------|-------------------------------------------------------------------------------------------------|-------------------|
| Precisão do Vosk pequeno em pt-BR insuficiente                    | Alto: o modo Voz não acompanha            | Calibração, vocabulário restrito; trocar o motor atrás do SpeechEngine                          | Etapa 0           |
| Bibliotecas nativas do Vosk ou do JNA sem alinhamento de 16 KB    | Alto: o Play recusa a publicação          | Verificar no build de release; versão mais nova ou recompilação do Vosk                         | Etapa 0           |
| Revisões da hipótese parcial geram palavras repetidas ou perdidas | Médio: avanços indevidos ou atrasos       | Reconciliação por distância de edição, palavras nunca retiradas, testes com revisões no prefixo | Etapa 0           |
| Callbacks atrasados de sessões antigas                            | Médio: saltos após pausa ou toque         | SessionId preso ao reconhecedor na criação; descarte do áudio pendente                          | Etapa 0           |
| Eco e ruído de salão geram parciais falsos                        | Médio: avanços indevidos                  | Limiar de nível de áudio, palavras confirmadas, "na dúvida, espera"                             | Etapa 0           |
| Memória do modelo acima do previsto                               | Médio: travamentos em aparelho de entrada | Medir; carregar o modelo uma vez; liberar ao sair                                               | Etapa 0           |
| Layout de texto longo pesado                                      | Médio: rolagem com engasgos               | Cache do layout; plano B com parágrafos em LazyColumn                                           | Semana 1          |
| Android 16 ignora o bloqueio de orientação em telas grandes       | Médio: A14 não funciona no tablet         | Layout nas duas orientações; A14 oculto no tablet; PRD ajustado                                 | Resolvido         |
| Android encerra o processo durante o discurso                     | Baixo: texto perdido                      | Tela ligada e Activity visível reduzem a chance; comportamento aceito pelo PRD                  | Aceito            |
| Outro motor exige recalibrar o alinhador                          | Médio: retrabalho                         | Golden de transcrições para recalibrar rápido                                                   | Se houver troca   |
| Biblioteca traz permissão de internet                             | Alto: quebra a promessa de offline        | Verificação do manifesto mesclado no build                                                      | Contínuo          |

**Decisões em aberto**

- [ ] Vocabulário restrito: ligado, desligado ou configurável (Etapa 0).
- [ ] Reconhecedor novo por sessão ou reset() do mesmo reconhecedor (Etapa 0).
- [ ] Tabela nível 1–10 → velocidade em dp/s do modo Constante (calibração).
- [ ] Valores finais de memória, bateria, latência por trecho e inicialização a frio (Etapa 0).
- [x] PRD: o ajuste A14 (Orientação) passa a valer só no celular. Aplicado no PRD em 10/10/2026.
- [x] Margem e fonte padrão do tablet: 56 sp e 12% (celular: 44 sp e 8%).
