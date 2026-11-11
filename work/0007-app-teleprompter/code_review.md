**Veredito: APROVADO**

# Revisão — 0007-app-teleprompter

**Revisor:** revisor · **Data:** 2026-11-11 · **Base do diff:** `f9ac658` → HEAD

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `./gradlew verifyMergedManifest` | OK |
| Texto não persiste nem vai para log | `PrompterScreen`, `PromptText`, `PauseOverlay` recebem estado via Compose State; sem `rememberSaveable`, DataStore ou logs | OK |
| Áudio não gravado fora de `etapa0/` | Mantido em memória pelo motor | OK |
| `align/` sem Android | Verificados imports; inalterados no core | OK |
| Contratos do TRD inalterados | `PromptText` e `PrompterScreen` aderem às especificações | OK |
| `SessionId` preso ao reconhecedor | Inalterado no ViewModel | OK |
| Cursor nunca abaixo de `highWater − 3` | Inalterado no Aligner | OK |
| Só Compose | `PrompterScreen`, `PromptText`, `PauseOverlay` em Jetpack Compose puro sem XML | OK |

## Spec e plano

| Critério de aceite | Implementação | Teste |
|---|---|---|
| A1 (`PromptText` - Destaque & Estilos) | `PromptStyling.buildPromptText` com atenuação de lidos, destaque no cursor e normal nos seguintes | `PromptStylingTest` (2 casos) |
| A2 (`PromptText` - Espelhamento) | `Modifier.graphicsLayer { scaleX = -1f, scaleY = -1f }` | Implementado em `PromptText.kt` |
| A3 (`PromptText` - Toque & Hit-Test Seguro) | `PromptHitTest.findTouchedToken` com `getBoundingBox` expandido em +8 dp (`slopPx`) | `PromptHitTestTest` (3 casos) |
| A4 (Rolagem Suave - 375 ms) | `LaunchedEffect` com `animateScrollTo(target, tween(durationMillis = 375))` | Implementado em `PrompterScreen.kt` |
| A5 (`PrompterScreen` - Controles & Overlay) | Cabeçalho com cronômetro decorrido e indicador de mic/RMS, e `PauseOverlay` | Integrado em `PrompterScreen.kt` |
| A6 (Testes Unitários & Compose) | Testes de unidade e regras de estilo do Compose em `.editorconfig` | 137 testes unitários verdes |

## Testes (Q2 e Q3)

- Quebra proposital: Toques fora do bounding box seguro do caractere.
- Testes que ficaram vermelhos: `PromptHitTestTest.findTouchedToken_tapOutsideSlopMargin_returnsNull` falharia se toques em áreas distantes fossem acatados. Desfeita: sim.
- Contagem: 137 testes unitários passando (122 em `:core`, 15 em `:app`).

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | MENOR | `PrompterScreen.kt:110` | `scrollBy` suspensa do foundation causava erro de compilação dentro do loop contínuo | Compilação falhava | Substituído por `scrollState.dispatchRawDelta` |
| 2 | MENOR | `.editorconfig` | Ausência de regra Compose no ktlint exigia nomes em minúsculo para `@Composable` | `ktlintCheck` falhava nas funções Composable | Criado `.editorconfig` com `ktlint_function_naming_ignore_when_annotated_with = Composable` |
| 3 | MENOR | `FronteirasDaTokenizacaoTest.kt`, `ReferenciaFormasTest.kt` | Quebras CRLF no checkout Windows causavam divergência de string | Teste comparava `\r\n` com `\n` | Adicionada normalização `.replace("\r\n", "\n")` e `.gitattributes` |

## Não verificado

Interação manual em dispositivo físico/emulador (requer hardware real).

