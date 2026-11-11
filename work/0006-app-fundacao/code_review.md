**Veredito: APROVADO**

# Revisão — 0006-app-fundacao

**Revisor:** revisor · **Data:** 2026-11-11 · **Base do diff:** `fbf044d` → HEAD

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `./gradlew verifyMergedManifest` executado no debug e release | OK |
| Texto não persiste nem vai para log | Inspecionados `SettingsRepositoryImpl.kt`, `PrompterViewModel.kt`, `proguard-rules.pro` | OK (DataStore armazena apenas `AppSettings`; ProGuard remove `Log.*` em release) |
| Áudio não gravado fora de `etapa0/` | Inspecionados `VoskEngine.kt` e `PrompterViewModel.kt` | OK (descartado pelo buffer nativo do Vosk) |
| `align/` sem Android | Verificados imports em `core/src/main/kotlin/.../align/` | OK (apenas Kotlin/JVM puro) |
| Contratos do TRD inalterados (ou CIA aprovada) | `SpeechEngine`, `SessionId`, `Token`, `Aligner`, `PrompterUiState`, `ReadingPhase` | OK |
| `SessionId` preso ao reconhecedor | `PrompterViewModel` descarta eventos com `session != activeSessionId` | OK (`PrompterViewModelTest.speechEvent_oldSessionEventIsIgnored`) |
| Cursor nunca abaixo de `highWater − 3` | Invariante do `Aligner` preservado | OK |
| Só Compose | `PrompterUiState` estruturado para consumo em Compose StateFlow | OK |

## Spec e plano

| Critério de aceite | Implementação | Teste |
|---|---|---|
| A1 (Build & Estrutura) | `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `core/build.gradle.kts` | Compilação `:core` e `:app` OK |
| A2 (Verificações) | `verifyMergedManifest` e `verify16kAlignment` integrados no `:app` via `tribuna.android.verifications` | `./gradlew verifyMergedManifest` e `./gradlew verify16kAlignment` passaram com sucesso |
| A3 (Núcleo puro `align/`) | Migrado para `:core` no pacote `io.github.gigioderico.tribuna.align` | 122 testes do `:core:test` 100% verdes |
| A4 (Motor de voz `speech/`) | `SpeechEngine`, `FakeSpeechEngine`, `VoskEngine` integrados | Testes unitários com `FakeSpeechEngine` 100% verdes |
| A5 (Preferências em DataStore) | `SettingsRepositoryImpl` com Jetpack DataStore Preferences | Estrutura reativa via `Flow<AppSettings>` |
| A6 (Máquina de estados & ViewModel) | `ReadingStateMachine` e `PrompterViewModel` com buffer de cauda (`tail`) e descarte de sessões | `ReadingStateMachineTest` (5/5) e `PrompterViewModelTest` (5/5) verdes |

Desvios do plano: Nenhum desvio não justificado. A verificação de 16 KB nas bibliotecas nativas foi calibrada para exigir 16 KB estritamente nas ABIs de 64 bits (`arm64-v8a` e `x86_64`), de acordo com a aprovação formal do Portão 3 / OPR-024.

## Testes (Q2 e Q3)

- Quebra proposital feita: Envio de eventos com `oldSession` no `PrompterViewModelTest`.
- Testes que ficaram vermelhos: `PrompterViewModelTest.speechEvent_oldSessionEventIsIgnored` falhou quando a sessão foi violada; corrigido com reconciliação e verificação de `SessionId`.
- Contagem mínima de casos: 132 testes unitários (122 em `:core`, 10 em `:app`).

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | MENOR | `build-logic/.../AndroidVerificationsPlugin.kt` | Extensão `.exe` ausente em `zipalign` no Windows | Task falhava com "zipalign not found" | Seleciona `zipalign.exe` em sistemas Windows |
| 2 | MENOR | `app/build.gradle.kts` | JNA JAR duplicava classes do AAR | R8 acusava Duplicate classes | Excluído JAR transitivo de `vosk-android` e fixado JNA como AAR |
| 3 | MENOR | `app/proguard-rules.pro` | Faltavam diretivas ProGuard do TRD | R8 acusava missing class `java.awt.Component` | Adicionadas regras do TRD |

## Não verificado

Telas de UI (Compose) e testes instrumentados com emulador/dispositivo físico não pertencem à 0006 e serão implementados e testados nas tarefas 0007 e 0008.
