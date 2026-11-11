# Spec — 0008-app-telas

**Autor:** analista · **Data:** 2026-11-11 · **Aprovação:** conferida pelo orquestrador em 2026-11-11

## O que e por quê

Conclusão da Onda 1 (MVP funcional) do aplicativo Tribuna:
1. `EditorScreen`: tela de entrada do discurso onde o orador insere ou cola seu texto, visualiza a contagem de palavras e tempo estimado de fala (~130 palavras/minuto), escolhe o modo inicial de rolagem (Voz ou Constante) e inicia a leitura.
2. `OnboardingScreen`: fluxo de boas-vindas na primeira execução, esclarecendo a privacidade (100% offline, sem envio ou persistência de texto ou áudio), solicitando a permissão do microfone (`RECORD_AUDIO`) e acionando o preparo do modelo de voz offline.
3. `QuickSettingsSheet` / `SettingsScreen`: painel de ajustes visuais com troca dos 4 temas prontos (DarkTealMint, BlackWhite, WhiteBlack, YellowBlack), tamanho de fonte, espaçamento entre linhas, espelhamento e sensibilidade do alinhamento, persistidos via `SettingsRepository` (DataStore).
4. `MainActivity` e `AppNavigation`: orquestração da navegação com Jetpack Compose Navigation, conectando Onboarding, Editor e Teleprompter com preservação estrita do texto em memória no `PrompterViewModel`.

## Requisitos atendidos

- PRD:
  - T1 (Campo de texto, colar, limpar, contagem de palavras, estimativa de duração).
  - T2 (Botão colar rápido da área de transferência e limpar).
  - T3 (Texto mantido estritamente em memória enquanto o app vive, nunca persistido).
  - C1 (Tela limpa e sem distrações).
  - A1–A14 (Ajustes de tema, fonte, espaçamento, espelhamento, sensibilidade).
  - Telas 1a (Boas-vindas e permissão), 1b (Cópia do modelo), 2a/2b (Editor de texto e ajustes), 3a (Leitura no teleprompter).
- TRD:
  - Módulos `ui.editor`, `ui.onboarding`, `ui.settings`.
  - Navegação entre telas (`AppNavHost`).
  - Ciclo de vida e privacidade: nenhuma persistência de texto.

## Critérios de aceite

- [ ] **A1 (`EditorScreen`):** Permite digitar, colar da área de transferência e limpar o texto; exibe contagem de palavras e tempo estimado (minutos:segundos); botão "Iniciar" habilitado somente quando houver texto válido.
- [ ] **A2 (`OnboardingScreen`):** Apresenta o manifesto de privacidade; solicita permissão `RECORD_AUDIO`; inicializa o modelo de reconhecimento offline com `ModelInstaller`; transiciona para o Editor quando pronto.
- [ ] **A3 (`QuickSettingsSheet` / `SettingsScreen`):** Permite selecionar entre os temas prontos, ajustar tamanho da fonte (slider), espaçamento e ativar espelhamento horizontal; alterações refletem reativamente via `SettingsRepository`.
- [ ] **A4 (`MainActivity` & Navegação):** Configura `AppNavHost` conectando as telas com transições Compose; o texto digitado no Editor é passado diretamente para o `PrompterViewModel` e nunca gravado em disco.
- [ ] **A5 (Validação e Qualidade):** Todos os testes unitários (`:core:test` e `:app:testDebugUnitTest`), `verifyMergedManifest`, `verify16kAlignment`, `ktlintCheck` e `lint` passam 100% verdes.

## Fora do escopo

- Recursos da Onda 2 (layout 2 colunas para tablet, botões de volume físico, notas `//`, avisos visuais de 5m e 1m).

## Mudança proposta no PRD (se houver)

Nenhuma.

## Perguntas em aberto

Nenhuma.

