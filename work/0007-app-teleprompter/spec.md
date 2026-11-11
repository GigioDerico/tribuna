# Spec — 0007-app-teleprompter

**Autor:** analista · **Data:** 2026-11-11 · **Aprovação:** conferida pelo orquestrador em 2026-11-11

## O que e por quê

Implementação do componente central de teleprompter em Jetpack Compose: `PromptText` e `PrompterScreen`.
Permite ao orador visualizar o texto do discurso com acompanhamento em tempo real, rolagem suave centrada no cursor ativo, reposicionamento manual por toque na palavra e interface de controle completa com overlay de pausa e indicador de microfone, integrados ao `PrompterViewModel`.

## Requisitos atendidos

- PRD:
  - R1 (Modo Voz: avanço pelo reconhecimento de voz, cursor visível na palavra atual).
  - R2 (Modo Constante: rolagem contínua em pixels/s).
  - R3 (Alternância entre modos).
  - R5 (Suavidade e conforto: rolagem interpolada com duração padrão de 375 ms conforme OPR-022).
  - R6 (Toque na tela: pausar e reposicionar o cursor na palavra tocada).
  - R7 (Indicador visual de captura/nível de áudio).
  - D2 (Cronômetro decorrido no topo).
  - C1 (Tela limpa e sem distrações durante a leitura).
  - C2 (Espelhamento horizontal para uso em espelho/vidro refletor - PRD A11).
  - A1–A14 (Ajustes visuais: tamanho de fonte, espaçamento entre linhas, margem lateral, alinhamento, 4 temas prontos: Alto contraste, Noturno suave, Papel, Minimalista).
- TRD:
  - `PromptText` (BasicText + AnnotatedString com estilos de lido/cursor/futuro).
  - Cache de `TextLayoutResult` e hit-test seguro com `getOffsetForPosition` + `getBoundingBox` expandido em +8 dp.
  - Animação de rolagem: interpolação suave para manter a linha do cursor na linha guia central.
  - Overlay de pausa com Retomar, Reiniciar e Concluir.

## Critérios de aceite

- [ ] **A1 (`PromptText` - Destaque & Estilos):** Renderiza o texto com `BasicText` e `AnnotatedString`: palavras lidas com opacidade atenuada (0.50), palavra atual sob o cursor destacada com cor de destaque do tema, e palavras seguintes com estilo normal.
- [ ] **A2 (`PromptText` - Espelhamento):** Suporta parâmetro `mirrored: Boolean`; quando `true`, aplica `graphicsLayer { scaleX = -1f }`.
- [ ] **A3 (`PromptText` - Toque & Hit-Test Seguro):** Toque em uma palavra calcula o offset seguro via `getOffsetForPosition` e confirma se as coordenadas estão dentro do `getBoundingBox(offset)` expandido em 8 dp; aciona o callback de reposicionamento com o índice correto do token.
- [ ] **A4 (Rolagem Suave - 375 ms):** O container de rolagem calcula a posição vertical do cursor ativo em relação à linha guia da tela e executa scroll animado suave (FastOutSlowInEasing ou linear) com duração de 375 ms.
- [ ] **A5 (`PrompterScreen` - Controles & Overlay):** Interface exibe: cronômetro no topo, indicador de microfone/RMS, área de leitura e, durante o estado `Paused`, o `PauseOverlay` com opções para Retomar leitura, Reiniciar do início e Concluir.
- [ ] **A6 (Testes Unitários & Compose):** Testes unitários do cálculo de layout/token mapping e testes de UI Compose em `app/src/test/` cobrindo formatação de texto e interação.

## Fora do escopo

- Telas de edição de texto (`EditorScreen`), tela inicial (`OnboardingScreen`) e tela de configurações completas (`SettingsScreen`) — escopo da tarefa 0008.
- Recursos da Onda 2 (layout 2 colunas para tablet, botões de volume físico, notas `//`, avisos sonoros/visuais de 5m e 1m).

## Mudança proposta no PRD (se houver)

Nenhuma.

## Perguntas em aberto

Nenhuma.

