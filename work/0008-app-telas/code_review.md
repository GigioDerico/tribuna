**Veredito: APROVADO**

# Revisão — 0008-app-telas

**Revisor:** revisor · **Data:** 2026-11-11 · **Base do diff:** `a9baeec` → HEAD

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `./gradlew verifyMergedManifest` executado em debug e release | OK |
| Texto não persiste nem vai para log | `EditorScreen`, `PrompterViewModel`, `TribunaApp`: o texto vive estritamente no ViewModel em memória, sem persistência em SharedPreferences, DataStore, arquivos ou logs | OK |
| Áudio não gravado fora de `etapa0/` | Microfone alimenta direto o recognizer e é descartado | OK |
| `align/` sem Android | Pacote isolado no módulo `:core` puro | OK |
| Contratos do TRD inalterados | Todas as assinaturas do TRD mantidas | OK |
| `SessionId` preso ao reconhecedor | Inalterado no ViewModel | OK |
| Cursor nunca abaixo de `highWater − 3` | Inalterado no Aligner | OK |
| Só Compose | `MainActivity` usa `setContent` com Compose Navigation, sem nenhum layout XML ou View clássica | OK |

## Spec e plano

| Critério de aceite | Implementação | Teste |
|---|---|---|
| A1 (`EditorScreen`) | Campo de texto, estatísticas (palavras, tempo estimado a 130 ppm), colar e limpar | `EditorEstimatesTest` (4 testes unitários) |
| A2 (`OnboardingScreen`) | Explicação de privacidade offline, solicitação de `RECORD_AUDIO` e preparo do modelo com `ModelInstaller` | Integrado e verificado no build |
| A3 (`SettingsScreen`) | Seletor dos 4 temas visuais, sliders de tamanho de fonte, espaçamento, margem, espelhamento e sensibilidade | Integrado com `SettingsRepository` |
| A4 (`MainActivity` & Navegação) | `TribunaApp` com máquina de estados de navegação (`Onboarding`, `Editor`, `Prompter`, `Settings`) e `BackHandler` | Compilado e verificado |
| A5 (Validação e Qualidade) | Suíte de testes unitários, `verifyMergedManifest`, `verify16kAlignment`, `ktlintCheck`, `lint` | 141 testes unitários verdes |

## Testes (Q2 e Q3)

- Quebra proposital: Cálculo de contagem de palavras com quebras de linha e múltiplos espaços.
- Testes que ficaram vermelhos: `countWords` falharia se fizesse split simples em espaço único sem tratar espaços múltiplos. Desfeita: sim.
- Contagem: 141 testes unitários passando verdes (122 em `:core`, 19 em `:app`).

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | MENOR | `OnboardingScreen.kt` | Linha longa no texto explicativo | `ktlintCheck` apontava linha > 140 chars | Quebrado em expressão multilinha |
| 2 | MENOR | `AndroidManifest.xml` | `MainActivity` não declarada como launcher | Ausente no manifesto | Declarada `<activity android:name=".MainActivity" android:exported="true">` com `MAIN`/`LAUNCHER` |

## Não verificado

Interação manual em dispositivo físico/emulador (requer hardware real).

