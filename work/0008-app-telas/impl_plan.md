# Plano — 0008-app-telas

**Autor:** arquiteto · **Spec:** `spec.md` (aprovado em 2026-11-11) · **Aprovação:** conferida pelo orquestrador em 2026-11-11

## Resumo

Construção das telas de suporte e navegação Compose para o aplicativo Tribuna:
1. `EditorScreen`: interface de edição com contador de palavras e cálculo de tempo estimado (130 palavras por minuto), botões rápidos de ação (Colar, Limpar, Ajustes, Iniciar leitura) e seletor de modo inicial (Voz / Constante).
2. `OnboardingScreen`: introdução ao aplicativo, verificação da permissão de áudio e acompanhamento da extração do modelo de voz offline (`ModelInstaller`).
3. `SettingsScreen` / `QuickSettingsSheet`: painel de ajustes visuais (temas pré-definidos, tamanho de fonte, espelhamento, sensibilidade).
4. `MainActivity`: ponto de entrada do Android com `ComponentActivity`, injeção do `PrompterViewModel` e `SettingsRepository`, e rotas do Compose Navigation.

## Arquivos que mudam

| Arquivo | Mudança | Requisito / seção do TRD | Agente |
|---|---|---|---|
| `app/src/main/kotlin/io/github/gigioderico/tribuna/ui/editor/EditorScreen.kt` | Campo de texto, estatísticas (palavras, tempo estimado), colar e limpar | PRD T1, T2, T3; TRD ui.editor | interface |
| `app/src/main/kotlin/io/github/gigioderico/tribuna/ui/onboarding/OnboardingScreen.kt` | Boas-vindas, solicitação de microfone e barra de progresso do modelo | PRD Tela 1a, 1b; TRD ui.onboarding | interface |
| `app/src/main/kotlin/io/github/gigioderico/tribuna/ui/settings/SettingsScreen.kt` | Seletor de temas (4 temas), sliders de fonte, margem, espelhamento | PRD A1-A14; TRD ui.settings | interface |
| `app/src/main/kotlin/io/github/gigioderico/tribuna/ui/navigation/TribunaNavGraph.kt` | Grafo de navegação (`Onboarding`, `Editor`, `Prompter`, `Settings`) | TRD Arquitetura | interface |
| `app/src/main/kotlin/io/github/gigioderico/tribuna/MainActivity.kt` | Activity principal com inicialização de dependências locais e roteamento | TRD Uma Activity | interface |
| `app/src/test/kotlin/io/github/gigioderico/tribuna/ui/editor/EditorEstimatesTest.kt` | Testes da contagem de palavras e cálculo do tempo estimado | TRD Testes | interface |

## Ordem e paralelismo

1. `EditorEstimatesTest` e lógica de cálculo de tempo estimado.
2. `EditorScreen`.
3. `OnboardingScreen`.
4. `SettingsScreen`.
5. `TribunaNavGraph` e `MainActivity`.
6. Validação completa (testes, lint, ktlint, manifest, 16k).

## Testes primeiro

- [ ] `EditorEstimatesTest`: valida cálculo correto de contagem de palavras para textos vazios, com espaços múltiplos, pontuações e estimativa em minutos/segundos a 130 ppm.

## Privacidade (primeira camada)

- O texto editado no `EditorScreen` é enviado unicamente ao `PrompterViewModel.setText(...)` via eventos Compose em memória.
- Nenhuma persistência em `SavedStateHandle`, DataStore, SharedPreferences, arquivos ou banco de dados (Regra Inegociável 2).

## Verificação

```bash
./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest verify16kAlignment
```

## Riscos

- Permissão de áudio negada no Onboarding: o app deve continuar permitindo o uso no modo Constante (conforme PRD Tela 2c e TRD §Permissões).

