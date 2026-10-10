**Resultado: PASSOU | FALHOU**

# Testes — <NNNN>-<nome>

**Responsável:** qa-publicacao · **Data:** <AAAA-MM-DD> · **Commit testado:** <hash>

## Ambiente

| Item | Valor |
|---|---|
| Aparelho(s) | <modelo, Android, RAM> |
| Ambiente acústico | <sala silenciosa / salão com eco> |
| Voz(es) | <...> |
| Distância do microfone | <~50 cm> |
| Brilho / modo avião | <fixo / ligado> |

## Execução

| Suíte / comando | Resultado | Evidência (arquivo na pasta) |
|---|---|---|
| `./gradlew testDebugUnitTest` | | `evidencias/unit.txt` |
| `./gradlew connectedDebugAndroidTest` | | `evidencias/instrumented.txt` |

## Critérios de aceite

| Critério | Como foi executado | Resultado | Evidência |
|---|---|---|---|
| | | | |

## Métricas da Etapa 0 (quando aplicável)

Regra de contagem (Q7): amostras a cada 100 ms; atraso em tokens; excluídos improviso, silêncio, pausa e 3 s após pulo.

| Gravação | Amostras de leitura normal | Atraso ≤ 3 | Saltos errados | Recuperação de pulo | Latência total |
|---|---|---|---|---|---|
| | | | | | |

## Teste falso (Q2)

- Quebra proposital: <...> · Testes que ficaram vermelhos: <...> · Desfeita: sim

## Não testado

<O que não foi possível testar e por quê.>
