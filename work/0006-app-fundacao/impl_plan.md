# Plano de Implementação — 0006-app-fundacao

**Autor:** arquiteto · **Data:** 2026-11-11 · **Status:** Aprovado para execução

## 1. Contexto e Objetivos

Estabelecer a fundação do aplicativo principal Tribuna em `app/` e `core/` na raiz do repositório, migrando a lógica validada de `etapa0/` e implementando a camada de ViewModel, máquina de estados e DataStore de preferências.

## 2. Ordem de Implementação

1. **Estrutura Gradle do Projeto:**
   - Criar `settings.gradle.kts`, `build.gradle.kts` na raiz, referenciando `libs.versions.toml`.
   - Módulo `:core`: Kotlin puro (`align/`, `speech/SpeechEngine`, contratos) + Android básico onde necessário para o Vosk.
   - Módulo `:app`: Android application com Jetpack Compose, DataStore, lifecycle, viewmodel.
   - `build-logic`: Reusar ou ligar as tarefas `verifyMergedManifest` e `verify16kAlignment` (atualizada para 64-bit conforme OPR-024).

2. **Migração do Núcleo (`align/` e `speech/`):**
   - Mover/copiar o código testado de `etapa0/core/src/main/kotlin/br/com/tribuna/core/align/` para `:core`.
   - Manter suíte completa de testes unitários de `align/` e `speech/FakeSpeechEngine`.

3. **DataStore (`data/SettingsRepository.kt`):**
   - Definir `AppSettings` imutável com as chaves A1–A14 e os 4 temas prontos (Preto/Branco, Branco/Preto, Amarelo/Preto, Verde/Preto).
   - Implementar `SettingsRepositoryImpl` via `dataStore.data.map`.
   - Garantir: NENHUM campo de texto do discurso no DataStore.

4. **Máquina de Estados e ViewModel (`viewmodel/`):**
   - Implementar `ReadingStateMachine`: função pura `(ReadingState, ReadingEvent) -> TransitionResult(newState, effects)`.
   - Implementar `PrompterViewModel`: expõe `PrompterUiState`, gerencia `SessionId` ativo, integra com `SpeechEngine`, controla cronômetro e expõe ações (`setText`, `startReading`, `pauseReading`, `resumeReading`, `finishReading`, `reposition`).
   - Testes unitários do `PrompterViewModel` com `FakeSpeechEngine` e `StandardTestDispatcher`.

## 3. Privacidade e Regras Inegociáveis

- **Sem rede:** `app/src/main/AndroidManifest.xml` conterá única e exclusivamente `<uses-permission android:name="android.permission.RECORD_AUDIO" />`.
- **Texto só em memória:** O texto existirá como `String` apenas no `PrompterViewModel`. Não usar `SavedStateHandle`, `rememberSaveable`, DataStore, arquivos ou logs.
- **Sessões seguras:** Eventos com `SessionId` defasado serão ignorados silenciosamente no `PrompterViewModel`.

## 4. Verificação

- `./gradlew test` (testes de `:core` e unitários de `:app`).
- `./gradlew verifyMergedManifest`
- `./gradlew verify16kAlignment` (com release)

