# Plano — <NNNN>-<nome>

**Autor:** arquiteto · **Spec:** `spec.md` (aprovado em <data>) · **Aprovação humana:** pendente | aprovado em <data> por <nome>

## Resumo

<Três linhas: a abordagem.>

## Arquivos que mudam

| Arquivo | Mudança | Requisito / seção do TRD | Agente |
|---|---|---|---|
| `align/Aligner.kt` | <...> | <TRD Motor de voz §Alinhamento 4> | motor-voz |

## Ordem e paralelismo

1. <Contratos / align>
2. <speech>
3. <viewmodel>
4. <ui>

<O que motor-voz e interface podem fazer em paralelo e de qual contrato dependem.>

## Testes primeiro

Casos que devem existir e falhar antes do código:

- [ ] <Ex.: `AlignerTest.recuosSucessivosNaoPassamDeHighWaterMenos3`>
- [ ] <...>

## Privacidade (primeira camada)

- Caminho do texto: <por onde passa; confirmar que não persiste nem loga>
- Caminho do áudio: <...>
- Dependências novas e permissões que trazem: <nenhuma | lista>
- Q6 aplicada: <resultado>

## Verificação

```bash
./gradlew testDebugUnitTest lint ktlintCheck verifyMergedManifest
<outros comandos>
```

Sucesso significa: <...>

## Riscos

- <Ex.: o Recognizer pode demorar para ser criado; se passar de X ms, usar reset() conforme TRD.>

---

## Registro da construção (preenchido pelo agente de construção)

**Agente:** <motor-voz | interface> · **Commit(s):** <hash>

- Feito: <...>
- Testes criados: <...>
- Desvios do plano e motivo: <nenhum | ...>
- Comandos rodados e resultado: <...>
