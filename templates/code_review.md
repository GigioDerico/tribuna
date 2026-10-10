**Veredito: APROVADO | REPROVADO**

# Revisão — <NNNN>-<nome>

**Revisor:** revisor · **Data:** <AAAA-MM-DD> · **Base do diff:** <hash> → <hash>

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | `./gradlew verifyMergedManifest` | OK / FALHA |
| Texto não persiste nem vai para log | Busca por <termos> nos arquivos do diff + leitura | OK / FALHA |
| Áudio não gravado fora de `etapa0/` | <...> | OK / FALHA |
| `align/` sem Android | <grep de imports> | OK / FALHA |
| Contratos do TRD inalterados (ou CIA aprovada) | <comparação> | OK / FALHA |
| `SessionId` preso ao reconhecedor | <...> | OK / FALHA / N.A. |
| Cursor nunca abaixo de `highWater − 3` | <...> | OK / FALHA / N.A. |
| Só Compose | <...> | OK / FALHA |

## Spec e plano

| Critério de aceite | Implementação | Teste |
|---|---|---|
| | | |

Desvios do plano: <justificados? sim/não>

## Testes (Q2 e Q3)

- Quebra proposital feita: <qual invariante, como>
- Testes que ficaram vermelhos: <quais> · desfeita: sim
- Contagem mínima de casos: <...>

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | BLOQUEANTE / IMPORTANTE / MENOR | | | | |

## Não verificado

<O que não foi possível checar e por quê.>
