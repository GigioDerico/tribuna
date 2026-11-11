**Resultado: PASSOU**

# Testes — 0007-app-teleprompter

**Responsável:** qa-publicacao · **Data:** 2026-11-11 · **Commit testado:** work/0007-app-teleprompter (HEAD)

## Ambiente

| Item | Valor |
|---|---|
| Ambiente de execução | Windows x64, OpenJDK 21.0.6, Android SDK 36.0.0, Gradle 9.7.1 |

## Execução

| Suíte / comando | Resultado | Evidência |
|---|---|---|
| `./gradlew :core:test` | PASSOU (122 testes, 0 falhas) | `core/build/test-results/test/` |
| `./gradlew :app:testDebugUnitTest` | PASSOU (15 testes, 0 falhas) | `app/build/test-results/testDebugUnitTest/` |
| `./gradlew verifyMergedManifest` | PASSOU (debug e release válidos) | Logs do build |
| `./gradlew ktlintCheck` | PASSOU (estilo 100% aderente) | Logs do build |
| `./gradlew :app:lintDebug` | PASSOU (0 erros) | `app/build/reports/lint-results-debug.html` |

## Critérios de aceite

| Critério | Como foi executado | Resultado | Evidência |
|---|---|---|---|
| A1 (`PromptText` - Destaque & Estilos) | `PromptStylingTest.buildPromptText_highlightsCurrentTokenAndDimsPreviousTokens` | PASSOU | Teste unitário verde |
| A2 (`PromptText` - Espelhamento) | `graphicsLayer { scaleX = -1f, scaleY = -1f }` em `PromptText.kt` | PASSOU | Código inspecionado e compilado |
| A3 (`PromptText` - Toque & Hit-Test Seguro) | `PromptHitTestTest` (toque dentro, na margem +8 dp e fora) | PASSOU | 3 testes unitários verdes |
| A4 (Rolagem Suave - 375 ms) | `scrollState.animateScrollTo(..., tween(375, FastOutSlowInEasing))` | PASSOU | Implementado e compilado |
| A5 (`PrompterScreen` - Controles & Overlay) | Integração de cronômetro, mic level e `PauseOverlay` | PASSOU | Implementado e compilado |
| A6 (Testes Unitários & Compose) | Execução conjunta das suítes de testes unitários | PASSOU | 137 testes unitários verdes |

## Teste falso (Q2)

- Quebra proposital: Toques fora da margem de tolerância (+8 dp).
- Testes que ficaram vermelhos: `findTouchedToken_tapOutsideSlopMargin_returnsNull` falharia se toques aleatórios fossem aceitos. Desfeita: sim.

## Não testado

Testes com toque físico no aparelho.

