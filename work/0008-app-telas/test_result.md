**Resultado: PASSOU**

# Testes — 0008-app-telas

**Responsável:** qa-publicacao · **Data:** 2026-11-11 · **Commit testado:** work/0008-app-telas (HEAD)

## Ambiente

| Item | Valor |
|---|---|
| Ambiente de execução | Windows x64, OpenJDK 21.0.6, Android SDK 36.0.0, Gradle 9.7.1 |

## Execução

| Suíte / comando | Resultado | Evidência |
|---|---|---|
| `./gradlew :core:test` | PASSOU (122 testes, 0 falhas) | `core/build/test-results/test/` |
| `./gradlew :app:testDebugUnitTest` | PASSOU (19 testes, 0 falhas) | `app/build/test-results/testDebugUnitTest/` |
| `./gradlew verifyMergedManifest` | PASSOU (debug e release válidos com apenas `RECORD_AUDIO`) | Logs da task `verifyMergedManifest` |
| `./gradlew verify16kAlignment` | PASSOU (APK e AAB com LOAD 16384 em 64 bits e zipalign -P 16) | Logs da task `verify16kAlignment` |
| `./gradlew ktlintCheck` | PASSOU (estilo 100% aderente) | Logs da task `ktlintCheck` |
| `./gradlew :app:lintDebug` | PASSOU (0 erros) | `app/build/reports/lint-results-debug.html` |
| `./gradlew :app:assembleRelease :app:bundleRelease` | PASSOU (geração completa dos binários de release) | `app/build/outputs/apk/release/`, `app/build/outputs/bundle/release/` |

## Critérios de aceite

| Critério | Como foi executado | Resultado | Evidência |
|---|---|---|---|
| A1 (`EditorScreen`) | Execução de `EditorEstimatesTest` e compilação do composable | PASSOU | 4 testes unitários verdes |
| A2 (`OnboardingScreen`) | Compilação do fluxo de onboarding e integração com `ModelInstaller` | PASSOU | Código validado no build |
| A3 (`SettingsScreen`) | Compilação da tela de ajustes e conexão com `SettingsRepository` | PASSOU | Código validado no build |
| A4 (`MainActivity` & Navegação) | `TribunaApp` com navegação Compose e manifesto com MainActivity launcher | PASSOU | Código e manifesto validados |
| A5 (Validação e Qualidade) | Execução conjunta de todas as suítes de teste, estilo e release | PASSOU | 141 testes unitários verdes |

## Teste falso (Q2)

- Quebra proposital: Contagem de palavras vazias ou com espaços múltiplos.
- Testes que ficaram vermelhos: `countWords_emptyOrWhitespace_returnsZero` e `countWords_countsSeparatedWordsCorrectly`. Desfeita: sim.

## Não testado

Testes manuais em aparelho físico.

