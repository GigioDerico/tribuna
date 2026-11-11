# Spec — 0006-app-fundacao

**Autor:** analista · **Data:** 2026-11-11 · **Aprovação:** conferida pelo orquestrador em 2026-11-11

## O que e por quê

Início da construção do app Tribuna em `app/` (Onda 1 - MVP). Esta tarefa estabelece a fundação completa do aplicativo:
1. Módulo `:core` e módulo `:app` na raiz do repositório (com Gradle Kotlin DSL, version catalog e plugins de verificação).
2. Integração e reaproveitamento do núcleo de voz e alinhamento (`align/` e `speech/`) validados na Etapa 0.
3. `SettingsRepository` com Jetpack DataStore (Preferences) para persistir apenas os ajustes visuais (sem persistir texto).
4. `ReadingStateMachine` e `PrompterViewModel` como fonte única da verdade para texto em memória, cursor, cronômetro e fases de leitura.

## Requisitos atendidos

- PRD: T1 (campo e texto), T2 (ações de colar/limpar), T3 (texto só em memória), R1 (modo Voz), R2 (modo Constante), R3 (troca de modo), R5 (suavidade de rolagem - 375 ms padrão), R7 (indicador de mic), D2 (cronômetro), A1-A14 (ajustes visuais com 4 temas prontos).
- TRD: Arquitetura, Módulos e responsabilidades, Contratos (`SpeechEngine`, `SessionId`, `Token`, `Aligner`, `PrompterUiState`, `ReadingPhase`), Máquina de estados da leitura, Ciclo de vida e privacidade.

## Critérios de aceite

- [ ] **A1 (Build & Estrutura):** Projeto raiz configura `:core` e `:app` com `libs.versions.toml`, `minSdk 26`, `targetSdk 36`, `compileSdk 36`.
- [ ] **A2 (Verificações):** Tarefas `./gradlew verifyMergedManifest` e `./gradlew verify16kAlignment` funcionam no `:app` (16 KB verificado para ABIs de 64 bits: `arm64-v8a` e `x86_64`).
- [ ] **A3 (Núcleo puro `align/`):** `align/` importado em `:core` sem dependências do Android; todos os testes unitários da JVM passam (`./gradlew :core:test`).
- [ ] **A4 (Motor de voz `speech/`):** `SpeechEngine`, `VoskEngine`, `ModelInstaller` e `FakeSpeechEngine` presentes em `:core`/`:app`.
- [ ] **A5 (Preferências em DataStore):** `SettingsRepository` persiste e restaura `AppSettings` (A1–A14) de forma reativa (`Flow`), nunca persistindo o texto do discurso.
- [ ] **A6 (Máquina de estados & ViewModel):** `ReadingStateMachine` e `PrompterViewModel` cobrem as transições (Idle -> Countdown -> Reading <-> Paused -> Finished), cronômetro, descarte de sessões antigas com `SessionId`, e preservação do texto em memória durante rotação e segundo plano, com testes unitários passando (`./gradlew :app:testDebugUnitTest`).

## Fora do escopo

- Telas e componentes visuais de Compose (`PrompterScreen`, `PromptText`, `EditorScreen`, `OnboardingScreen`, `SettingsScreen`) — entram nas tarefas 0007 e 0008.
- Gestos complexos de toque e arrasto na tela de leitura — entram na tarefa 0007.
- Recursos da Onda 2 (layout de duas colunas para tablet, botões físicos de volume, notas `//`, avisos visuais de 5m/1m).

## Mudança proposta no PRD (se houver)

Nenhuma. Escopo segue estritamente os requisitos P0 do PRD e as decisões aprovadas no Portão 3.

## Perguntas em aberto

Nenhuma.

