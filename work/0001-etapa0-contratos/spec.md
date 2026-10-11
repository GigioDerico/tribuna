# Spec — 0001-etapa0-contratos

**Autor:** analista · **Data:** 2026-10-10 · **Aprovação humana:** aprovado em 2026-10-10 por Giorgio Derico

Brainstorm dispensado: os contratos já estão decididos no TRD (Contratos) e a tarefa é a 1 da Etapa 0 no PROCESS.md, com cobertura total por PRD/TRD (PROCESS.md, Intake).

## O que e por quê

Criar o esqueleto compilável da Etapa 0: os contratos do TRD em Kotlin, o `FakeSpeechEngine` e o projeto Gradle `etapa0/`, com as tarefas `verifyMergedManifest` e `verify16kAlignment` (CLAUDE.md: criá-las é parte da primeira entrega). Fechado o contrato, as tarefas 0002 (align) e 0003 (Vosk) constroem em paralelo sem depender do código uma da outra (PROCESS.md, Contratos primeiro). Não há mudança visível ao orador.

## Requisitos atendidos

- PRD: nenhum requisito de usuário (T, A, R, D, C) é entregue nesta tarefa; é infraestrutura da Etapa 0.
- TRD, contratos a codificar exatamente como escritos, sem mudar assinatura nem semântica (CLAUDE.md, regra 5):
  - Contratos §SpeechEngine: `SpeechEngine`, `SessionId`, `SpeechEvent` (`Partial`, `Final`, `Level`, `Error`), `EngineState`, `EngineError`.
  - Contratos §Token: `Token`.
  - Contratos §Aligner: `Aligner` (só a assinatura), `AlignState`, `Move`, `AlignParams`.
  - Módulos e responsabilidades: `FakeSpeechEngine` no pacote `speech`; `align` sem import de `android.*` nem `androidx.*`.
- TRD §Etapa 0 (O que construir, itens 1 e 2, só a parte de projeto e contratos), §Testes, qualidade e distribuição (Qualidade de código: manifesto mesclado sem INTERNET; 16 KB com `zipalign -c -P 16` e segmentos ELF de `libvosk.so` e `libjnidispatch.so`), §Stack e versões (Kotlin 2.x, version catalog, minSdk 26, targetSdk 36, compileSdk 36).
- Verificado no TRD: os contratos acima existem. `PrompterUiState`, `ReadingPhase`, `AppSettings`, `SettingsRepository`, `ScrollMode` e `TimeWarning` são contratos do app, não da Etapa 0 (que não tem ViewModel nem DataStore), e ficam de fora.

## Critérios de aceite

Todos verificáveis por comando, a partir de `etapa0/`, com a saída anexada ao `impl_plan.md` ou ao `test_result.md`.

- [ ] `./gradlew assembleDebug` termina com BUILD SUCCESSFUL, usando JDK 21 e o SDK 36 de `~/Library/Android/sdk`.
- [ ] `./gradlew testDebugUnitTest` termina verde, com pelo menos um teste por item: ciclo de vida do `FakeSpeechEngine` e cada `SpeechEvent`; `SessionId` crescente a cada `beginSession`.
- [ ] Teste do `FakeSpeechEngine` com `kotlinx-coroutines-test`: evento emitido com o `SessionId` de uma sessão já encerrada continua chegando com o id antigo (TRD §SpeechEngine, regras do contrato). Esse teste fica vermelho se o fake passar a ler uma variável de "sessão atual" no callback (Q2 do `REVIEW-QUESTIONS.md`).
- [ ] Teste de `endSession`: depois dele, o fake não emite `Partial` nem `Final` com a sessão encerrada, e `Level` continua sendo emitido enquanto a captura estiver aberta.
- [ ] `grep -rE "import (android|androidx)\." <pacote align>` não retorna nada, e o pacote compila e testa na JVM sem o plugin Android (regra inegociável 4).
- [ ] `./gradlew verifyMergedManifest` passa no projeto e falha (saída anexada) quando se acrescenta `INTERNET`, `ACCESS_NETWORK_STATE` ou `WAKE_LOCK` ao manifesto de teste; com só `RECORD_AUDIO` passa. A tarefa também falha se o manifesto mesclado tiver qualquer outra permissão.
- [ ] `./gradlew verify16kAlignment` existe e roda: no release com `libvosk.so` e `libjnidispatch.so`, lê os alinhamentos e falha se algum for menor que 16 KB. Nesta tarefa ainda não há dependência do Vosk; a tarefa precisa ao menos falhar de forma explícita (não passar em silêncio) quando não achar as duas bibliotecas num release. Execução real contra o Vosk é da 0003.
- [ ] `./gradlew lint ktlintCheck` sem erros.
- [ ] `./gradlew :<módulo> dependencies` mostra versões só vindas de `gradle/libs.versions.toml`; nenhum literal de versão nos `build.gradle.kts`.
- [ ] Nenhum arquivo de layout XML e nenhum `INTERNET` no manifesto do projeto (`grep -r "uses-permission"` só mostra `RECORD_AUDIO`).

## Fora do escopo

- `VoskEngine`, `ModelInstaller`, modelo `vosk-model-small-pt-0.3`, dependência `vosk-android`/JNA (tarefa 0003).
- Implementação de `Normalizer`, `Tokenizer`, `PartialReconciler` e de `Aligner.advance`/`reposition` (tarefa 0002); aqui o `Aligner` só existe como assinatura.
- Tela mínima, painel de depuração, exportação de transcrições (0003).
- Ferramentas Python `timestamps.py`, `alinhar.py`, `metricas.py` (0004).
- Gravações, trajetórias e medição (0005).
- `PrompterUiState`, `ReadingPhase`, `ReadingStateMachine`, `PrompterViewModel`, `SettingsRepository`, DataStore, telas, navegação: são do app, que só começa depois da Etapa 0 aprovada.
- Valores de `AlignParams` (k, janela, limiares): vêm da calibração da Etapa 0; o contrato não ganha padrões inventados.
- Execução em aparelho (ainda não há um conectado).
- Qualquer mudança em assinatura ou semântica dos contratos; se o código não couber no contrato, parar e pedir aprovação (CLAUDE.md, regra 5).

## Mudança proposta no PRD (se houver)

Nenhuma.

## Decisões humanas (2026-10-10)

1. **Namespace:** `io.github.gigioderico.tribuna` (app) e `io.github.gigioderico.tribuna.etapa0` (projeto `etapa0/`). Nada de zafety: o projeto não tem relação com essa marca. O `applicationId` do app publicado na Play não muda depois.
2. **Módulos:** módulo Kotlin/JVM puro `:core` (contratos `speech`, `FakeSpeechEngine` e `align`) mais módulo Android `:app` de teste em `etapa0/app` que o consome. Quem consome depois (`app/` ou cópia) fica para depois da aprovação da Etapa 0.
3. **Aligner:** esqueleto com `advance`/`reposition` lançando `NotImplementedError` até a 0002, sem testes que o chamem.
4. **Versões de Gradle, AGP e Kotlin:** o `arquiteto` pesquisa, cita a fonte no plano e fixa no `libs.versions.toml`.

## Perguntas em aberto

Nenhuma. As perguntas originais do analista (abaixo, para registro) foram respondidas acima.

1. **Namespace e applicationId do projeto `etapa0/`.** Nenhum documento define. Proposta: `br.com.zafety.tribuna.etapa0` (pacotes `...speech`, `...align`), com o app futuro usando `br.com.zafety.tribuna`. Confirmar ou indicar outro.
2. **Estrutura de módulos e reaproveitamento do código.** O TRD diz "projeto experimental, separado do app" e a exportação de transcrições não pode passar ao app, mas os contratos, o `FakeSpeechEngine` e o `align/` serão idênticos no app. Opções: (a) copiar para `app/` quando a Etapa 0 for aprovada; (b) módulo Gradle Kotlin/JVM puro (`:core` com `speech` contratos e `align`) em `etapa0/`, depois movido ou compartilhado; (c) um repositório de módulos compartilhado desde já. Recomendação: (b), com `align` e contratos num módulo JVM puro, o que impõe a regra 4 pelo próprio build, e módulo Android `:app` de teste (nome sugerido `etapa0/app`) que o consome. Decidir também se `app/` depois consome esse módulo ou recebe cópia.
3. **`Aligner` na tarefa 0001.** O contrato é uma classe com corpo, mas a implementação é da 0002. Aceitar um esqueleto com `advance`/`reposition` lançando `NotImplementedError` (sem testes que o chamem) até a 0002? Recomendação: sim; a alternativa de adiar o `Aligner` quebra o critério "contratos do TRD em Kotlin" desta tarefa.
4. **Versões do Gradle, AGP e Kotlin.** O TRD pede "as estáveis mais recentes no dia da criação" (hoje, 10/10/2026). Aprovar que o `arquiteto` pesquise e fixe no catalog, ou o humano informa as versões.

Itens sem pergunta ao humano, resolvidos por documento: JDK 21 (README), SDK 36 (TRD §Stack), nome das tarefas de verificação (CLAUDE.md).
