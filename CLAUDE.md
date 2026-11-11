# Tribuna

As regras gerais do projeto, válidas para todos os agentes e todas as sessões, estão em [`AGENTS.md`](AGENTS.md). Leia-o primeiro; ele é importado abaixo.

@AGENTS.md

O que segue é complemento para Claude Code: estado atual do projeto e orientações específicas de papel ou de fase. Em conflito com o `AGENTS.md`, vale o `AGENTS.md`.

## Estado atual

- Etapa 0 concluída e arquitetura aprovada (Portão 3).
- Tarefa `0006-app-fundacao` concluída: `:core` e `:app` configurados, `align/` e `speech/` integrados, `SettingsRepository` (DataStore), `ReadingStateMachine` e `PrompterViewModel` com 132 testes unitários passando verdes; `verifyMergedManifest`, `verify16kAlignment`, `ktlintCheck` e `lint` 100% aprovados.
- Próxima tarefa: `0007-app-teleprompter` (Onda 1 - MVP): implementação de `PromptText` (BasicText + AnnotatedString com destaque do cursor, opacidade de lido, espelhamento, hit-test seguro `getBoundingBox`), `PrompterScreen`, `PauseOverlay` e animação suave de rolagem (375 ms).

## Orquestrador

A sessão principal é o orquestrador: abre a tarefa em `work/<NNNN>-<nome>/` a partir de `templates/`, chama os agentes na ordem do `PROCESS.md` e leva ao humano só o que cai num portão humano. Ela não implementa nem revisa.

## Etapa 0

- `docs/TESTES-ETAPA0.md` e `docs/TESTES-ETAPA0-L30.md` — textos de teste, protocolo de gravação e cálculo das métricas da Etapa 0.
- Subpastas de `etapa0/`: `textos/ gravacoes/ eventos/ esperado/ trajetorias/ ferramentas/ relatorios/`.

Ferramentas da Etapa 0 (Python, rodam no computador):

```bash
python etapa0/ferramentas/timestamps.py <wav>
python etapa0/ferramentas/alinhar.py <texto> <transcricao>
python etapa0/ferramentas/metricas.py <trajetoria> <esperado> <eventos>
```

## Erros comuns a evitar

- Usar `SpeechService` do Vosk: o TRD usa laço próprio de `AudioRecord` alimentando o `Recognizer`, para separar captura de sessão.
- Contar "palavras já consumidas" de parciais: use o `PartialReconciler` (prefixo estável + distância de edição).
- Pausar a leitura no `onStop` durante rotação: confira `isChangingConfigurations`.
- Animar a rolagem no modo Constante a partir do cursor: ali a rolagem conduz o cursor, não o contrário.
- Bloquear orientação no tablet: com targetSdk 36 o Android 16 ignora; o A14 só existe no celular.
- `getOffsetForPosition` sozinho para decidir toque em palavra: sempre confira com `getBoundingBox` (+8 dp).
