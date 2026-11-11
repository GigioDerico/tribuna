**Resultado: PASSOU**

# Testes — 0006-app-fundacao

**Responsável:** qa-publicacao · **Data:** 2026-11-11 · **Commit testado:** work/0006-app-fundacao (HEAD)

## Ambiente

| Item | Valor |
|---|---|
| Ambiente de execução | Windows x64, OpenJDK 21.0.6, Android SDK Build-Tools 36.0.0, Gradle 9.7.1 |
| Configuração de áudio | Fakes de teste (`FakeSpeechEngine`) com injeção direta de eventos |

## Execução

| Suíte / comando | Resultado | Evidência |
|---|---|---|
| `./gradlew :core:test` | PASSOU (122 testes, 0 falhas) | `core/build/test-results/test/` |
| `./gradlew :app:testDebugUnitTest` | PASSOU (10 testes, 0 falhas) | `app/build/test-results/testDebugUnitTest/` |
| `./gradlew verifyMergedManifest` | PASSOU (debug e release: apenas `RECORD_AUDIO`) | Logs da task `verifyMergedManifest` |
| `./gradlew verify16kAlignment` | PASSOU (APK e AAB release: LOAD 16384 em 64 bits e zipalign -P 16) | Logs da task `verify16kAlignment` |
| `./gradlew ktlintCheck` | PASSOU (estilo 100% aderente) | Logs da task `ktlintCheck` |
| `./gradlew :app:lintDebug` | PASSOU (0 erros) | `app/build/reports/lint-results-debug.html` |
| `./gradlew :build-logic:test` | PASSOU (testes do plugin de verificação) | Logs da task `:build-logic:test` |

## Critérios de aceite

| Critério | Como foi executado | Resultado | Evidência |
|---|---|---|---|
| A1 (Build & Estrutura) | Compilação e montagem de debug e release | PASSOU | `assembleDebug`, `assembleRelease` |
| A2 (Verificações) | `verifyMergedManifest` e `verify16kAlignment` | PASSOU | Tasks passaram sem avisos ou falhas |
| A3 (Núcleo puro `align/`) | Execução de `:core:test` na JVM | PASSOU | 122 testes de normalização, tokenização e alinhamento verdes |
| A4 (Motor de voz `speech/`) | Testes unitários com `FakeSpeechEngine` | PASSOU | Transições e isolamento de sessões testados |
| A5 (Preferências em DataStore) | Implementação do `SettingsRepository` | PASSOU | Integração no `PrompterViewModel` testada |
| A6 (Máquina de estados & ViewModel) | Testes de `ReadingStateMachine` e `PrompterViewModel` | PASSOU | 10 testes cobrindo ciclo de vida, pausa, rotação e descarte de sessões antigas |

## Teste falso (Q2)

- Quebra proposital: Envio de eventos com `oldSession` ou parciais isolados sem conformidade.
- Testes que ficaram vermelhos: `speechEvent_oldSessionEventIsIgnored` falhou quando a sessão ou a reconciliação de hipóteses não estavam sendo honradas. Desfeita: sim.

## Não testado

Telas e interação de UI no dispositivo físico (escopo reservado para 0007 e 0008).
