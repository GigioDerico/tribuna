# Plano — 0007-app-teleprompter

**Autor:** arquiteto · **Spec:** `spec.md` (aprovado em 2026-11-11) · **Aprovação:** conferida pelo orquestrador em 2026-11-11

## Resumo

Construção dos componentes centrais de apresentação do teleprompter em Jetpack Compose:
1. `PromptAnnotatedString`: função pura que converte a lista de `Token`, o texto bruto e o índice do cursor em um `AnnotatedString` estilizado conforme o tema (4 temas pré-configurados).
2. `PromptText`: componente Compose usando `BasicText`, espelhamento horizontal via `graphicsLayer`, hit-testing seguro com `TextLayoutResult.getBoundingBox` e suporte a gestos de clique para reposicionamento do cursor.
3. `PrompterScreen` e `PauseOverlay`: tela de leitura com container scrollável com animação suave de 375 ms (OPR-022), barra superior com cronômetro decorrido, indicador de nível de áudio e painel sobreposto de pausa com controles de Retomar, Reiniciar e Concluir.

## Arquivos que mudam

| Arquivo | Mudança | Requisito / seção do TRD | Agente |
|---|---|---|---|
| `app/src/main/kotlin/io/github/gigioderico/tribuna/ui/prompter/PromptStyling.kt` | Função pura para montagem do `AnnotatedString` com estilos de lido/cursor/futuro para cada tema | PRD A1-A14; TRD PromptText | interface |
| `app/src/main/kotlin/io/github/gigioderico/tribuna/ui/prompter/PromptText.kt` | Componente de texto com `BasicText`, cache de layout, espelhamento e hit-test seguro de toque (+8 dp) | PRD R6, C2; TRD PromptText | interface |
| `app/src/main/kotlin/io/github/gigioderico/tribuna/ui/prompter/PauseOverlay.kt` | Diálogo/overlay exibido em `Paused` com botões Retomar, Reiniciar e Concluir | PRD R6; TRD PrompterScreen | interface |
| `app/src/main/kotlin/io/github/gigioderico/tribuna/ui/prompter/PrompterScreen.kt` | Tela principal de leitura com scroll automático animado (375 ms), cronômetro e indicador de microfone | PRD R1, R2, R5, R7, D2, C1; TRD PrompterScreen | interface |
| `app/src/test/kotlin/io/github/gigioderico/tribuna/ui/prompter/PromptStylingTest.kt` | Testes unitários puros da montagem do `AnnotatedString` e destaque correto de tokens | TRD Testes | interface |
| `app/src/test/kotlin/io/github/gigioderico/tribuna/ui/prompter/PromptHitTestTest.kt` | Testes do cálculo geométrico de hit-test com margem de segurança de +8 dp | TRD PromptText | interface |

## Ordem e paralelismo

1. `PromptStyling` (função pura JVM, testável diretamente com JUnit).
2. `PromptText` (renderização e detecção geométrica de toques).
3. `PauseOverlay` e cabeçalho com cronômetro e indicador de microfone.
4. `PrompterScreen` integrando a rolagem suave baseada no `cursor` do `PrompterViewModel`.

## Testes primeiro

- [ ] `PromptStylingTest`: verifica se o token sob o cursor recebe o estilo de destaque, tokens anteriores recebem opacidade atenuada (0.5f) e tokens posteriores mantêm o estilo base.
- [ ] `PromptHitTestTest`: valida que toques dentro do bounding box expandido em 8 dp identificam o token correspondente, enquanto toques distantes são ignorados.

## Privacidade (primeira camada)

- Caminho do texto: O texto é lido da memória em `PrompterUiState` e transmitido para os composables via parâmetros `@Composable (text: String, tokens: List<Token>, cursor: Int)`. Em nenhum momento o texto é salvo em disco, logado ou enviado para fora.
- Caminho do áudio: Continua restrito ao buffer interno em memória do VoskEngine; nada é gravado em arquivo.
- Dependências novas e permissões que trazem: Nenhuma dependência nova.
- Q6 aplicada: Nenhuma persistência em `rememberSaveable`, DataStore ou logs.

## Verificação

```bash
./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest
```

Sucesso significa: Todos os testes unitários passando (132 existentes + novos testes de UI/estilos), lint e ktlint verdes, e manifesto preservado sem nenhuma permissão adicional.

## Riscos

- `TextLayoutResult` nulo no primeiro frame de composição: o cálculo de scroll e de hit-test deve tratar com segurança o estado em que o layout ainda não foi medido (guardas não-nulas).

