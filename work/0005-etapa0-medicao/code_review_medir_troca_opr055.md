**Veredito: APROVADO**

# Revisão — 0005-etapa0-medicao, `medir.sh metricas` troca os seis relatórios juntos (OPR-055, achado 4; plano R3-2)

**Revisor:** revisor · **Data:** 2026-10-15 · **Base do diff:** `100a021^` (`6a3d92c`) → `100a021`

Escopo: o commit `100a021`, com `work/0005-etapa0-medicao/scripts/medir.sh` (+9 −2), `evidencias/qc/r3-medir-troca-junta.txt` (novo) e uma linha em `evidencias/qc/LEIA-ME.md`. A referência é `impl_plan.md`, seção "Correções da revisão rodada 3 (OPR-055 e contagem)" (linhas 6150-6389, commit `32adeb2`), e o problema original é o achado 4 de `code_review_scripts_qa.md`.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | O commit não toca `app/`, `etapa0/`, manifesto nem dependência (`git show --stat 100a021 -- etapa0 docs` não lista nada) | N.A. (nada mudou) |
| Texto não persiste nem vai para log | O diário ganha só o nome da gravação, a rodada e a contagem. Os temporários ficam em `$TMPDIR/tribuna-0005/metricas.*` e são apagados na saída. Os relatórios são os mesmos de antes (T3, iguais byte a byte) | OK |
| Áudio não gravado fora de `etapa0/` | O script não lê nem escreve áudio neste ramo | OK |
| `align/` sem Android | Sem mudança | N.A. |
| Contratos do TRD inalterados | Sem mudança | N.A. |
| `SessionId`, `highWater − 3`, só Compose | Sem mudança no app | N.A. |

## Saída real dos comandos

O diff do commit é o mesmo do patch do plano, sem nada a mais nem a menos. Para conferir, extraí o bloco `diff` do plano, apliquei com `git apply` sobre a versão antiga (`100a021^`) num diretório descartável e comparei o resultado com o arquivo do commit:

```
$ git show 100a021^:work/0005-etapa0-medicao/scripts/medir.sh | shasum -a 256
bd3804a0877c212fb70bbe2e228fd1432be1fa5e53e42c6b2dcc26d0a7d37a0d  -
$ (git apply --check plano.patch && git apply plano.patch) ; shasum -a 256 .../ap/.../medir.sh
9af077c606c642ae9bae0236f6db36190855611d1ccb2ce4e7427257906bbcfb
$ cmp .../ap/.../medir.sh work/0005-etapa0-medicao/scripts/medir.sh && echo IDENTICO
IDENTICO
$ shasum -a 256 work/0005-etapa0-medicao/scripts/medir.sh
9af077c606c642ae9bae0236f6db36190855611d1ccb2ce4e7427257906bbcfb  work/0005-etapa0-medicao/scripts/medir.sh
$ bash -n work/0005-etapa0-medicao/scripts/medir.sh; echo "bash -n rc=$?"
bash -n rc=0
```

O ensaio é o bloco `bash` do plano, extraído sem mudança (`sha256 2abacc87…43c9`, o mesmo da evidência). Rodei num scratchpad fora da árvore principal, com `medir.antigo.sh` = `bd3804a0…`. Depois tirei os carimbos de hora e comparei com as seções da evidência:

```
ANTIGO igual à evidência (sem carimbo de hora)
NOVO igual à evidência (sem carimbo de hora)
QUEBRAS iguais à evidência
```

Resumo do que saiu (idêntico a `evidencias/qc/r3-medir-troca-junta.txt`):

- **Versão antiga.** T1: `medir.sh rc=1`, `RELATÓRIOS MUDARAM`, `.md com 0 bytes`. T2: `rc=1` e 5 arquivos `trocado` (o `.p200.json` fica antigo, ou seja, conjunto misto). T3: `rc=0`, 6 trocados, 3 iguais ao `metricas.py` direto.
- **Versão nova.** T1: `rc=1`, a linha `nenhum relatório trocado` aparece antes do `PARAR: código 2`, `os seis relatórios iguais aos de antes`, `.md com 1391 bytes`, temporárias 0. T2: `rc=1`, seis iguais, temporárias 0. T3: `rc=0`, `seis relatórios trocados (d, .m200, .p200) em etapa0/relatorios/S1`, 6 trocados, 3 iguais ao direto, temporárias 0, `git status` fora do escopo 0.
- **Quebras do plano.** `quebra1` (mv dentro do laço) fica vermelha em `T2: RELATÓRIOS MUDARAM:` com 4 `trocado`. `quebra2` (sem `trap`) fica vermelha em `pastas temporárias restantes` com 2, 3 e 4.

Rodei o mesmo ensaio, com a versão nova, numa pasta cujo caminho tem espaço e `$` (`r3 novo com espaço e $x`, usada também como `TMPDIR`). T1 a T3 deram o mesmo resultado, com temporárias 0.

Teste de sanidade:

```
$ PYTHONDONTWRITEBYTECODE=1 python3 -B etapa0/ferramentas/testar.py | tail -1
testar.py: 329 testes, 0 falhas, 0 erros, 0 pulados
$ git status --short     # árvore principal depois de todos os ensaios
 M .claude/agent-memory/revisor/MEMORY.md          (pré-existente, não é deste commit)
?? .claude/agent-memory/revisor/...                (idem)
```

## Leitura do bloco `metricas)` (`medir.sh:60-93`)

- **Pasta temporária apagada em todos os caminhos de saída.** O `trap 'rm -r -- "$tmp"' EXIT` (linha 81) é armado logo depois do `mktemp -d`. Ele cobre os três caminhos que levam ao `exit`: o `exit 1` do `rc_aceito`, a saída por `set -e` (por exemplo, quando um `mv` falha) e o fim normal. O `vizinhas.py` e o `rc_aceito` dele (linhas 73-77) rodam antes do `mktemp`, então não deixam pasta. Se o `mktemp` falhar, o `set -e` encerra o script antes do `trap`, e não há o que apagar. Tudo isso foi conferido com saída real (T1, T2, T3 e as sondas, sempre com temporárias 0), menos a saída por sinal (ver "Não verificado").
- **Troca só depois da terceira corrida aceita.** O laço de `mv` (linha 92) fica fora do `for` das corridas e só roda se as três passarem pelo `rc_aceito "$rc" 1`. Não há `mv` dentro do laço.
- **Código 2 em qualquer corrida não muda nenhum relatório.** O `rc_aceito` sai antes da linha 92, e as saídas das corridas vão para `$tmp`, não para `$rel` (linha 86). T1 (falha na corrida d) e T2 (falha na corrida d+200) confirmam.
- **Aspas no caminho.** `TMPD="${TMPDIR:-/tmp}/tribuna-0005"` (`comum.sh:13`), `mktemp -d "$TMPD/metricas.XXXXXX"`, `"$tmp/$saida$suf.{md,json}"` e o `"$tmp"` dentro do `trap` estão todos entre aspas. O `trap` está em aspas simples, então `$tmp` só é expandido na hora da saída, já dentro de aspas duplas. O ensaio com espaço e `$` no caminho passou.
- **Conteúdo igual.** Os relatórios saem iguais byte a byte aos do `metricas.py` rodado direto (T3), porque o caminho do `--json` não entra no relatório.

## Spec e plano

| Item do plano (R3-2) | Implementação | Prova |
|---|---|---|
| Três corridas na pasta temporária, apagada no `EXIT` | `medir.sh:81,86` | T1, T2, T3 com temporárias 0; `quebra2` vermelha |
| Os seis relatórios trocam só depois da terceira corrida aceita | `medir.sh:91-92` | T2 com seis iguais; `quebra1` vermelha |
| Com código 2, o diário ganha "nenhum relatório trocado", sem mudar o `rc_aceito` | `medir.sh:89-90` | Linha presente na saída de T1 e T2. Nenhum teste a exige (achado 2) |
| Com sucesso, o diário ganha "seis relatórios trocados" | `medir.sh:93` | T3 |
| Conteúdo igual ao do `metricas.py` direto | — | T3, três `iguais` |
| Evidência e LEIA-ME | `evidencias/qc/r3-medir-troca-junta.txt`, `LEIA-ME.md:16` | Reproduzidas acima |

Desvios do plano: nenhum. O diff do `medir.sh` é igual ao patch do plano, conferido com `cmp`.

## Testes (Q2 e Q3)

- Quebras do plano, refeitas: `quebra1` (mv dentro do laço) e `quebra2` (sem `trap`) ficaram vermelhas no ponto previsto, com a mesma saída da evidência.
- Quebras minhas, cada uma sozinha em cópia fora do repositório:
  - `quebra3`: tirar a linha 89 (a do diário "nenhum relatório trocado"). Ficou **verde** (achado 2).
  - `quebra4`: o laço de `mv` só com `"" .m200`. Ficou **vermelha** em T3 (`relatórios trocados: 4`, `.p200 DIFERENTE do metricas.py direto`).
- Sonda além do plano: uma exceção não tratada no `metricas.py`, na corrida d+200, faz o Python sair com código 1, e o script trata esse código como aceito. O resultado é um conjunto misto (achado 1).
- Nenhuma quebra foi feita na árvore principal. Todas rodaram em cópias no scratchpad, e o `git status` da árvore principal ficou igual ao do início.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | MENOR | `work/0005-etapa0-medicao/scripts/medir.sh:85-92` | A garantia "juntos ou nenhum" depende de o `metricas.py` só sair com 0 ou 1 depois de escrever `.md` e `.json`. Uma exceção não tratada (bug na ferramenta; `metricas.py:538-547` só captura `OSError`, `UnicodeDecodeError` e `ErroEntrada`, e o `open(args.json)` fica fora do `try`) faz o Python sair com **1**, que o `rc_aceito "$rc" 1` aceita. As outras corridas seguem. Na linha 92, o `mv -f -- a.md a.json "$rel/"` move o `.md` vazio e falha no `.json` que não existe, e o `set -e` encerra o script. O resultado é o mesmo sintoma do achado 4, com outro gatilho. A pasta temporária é apagada. O script sai com 1, mas só com o erro do `mv`, sem `PARAR`. | Sonda no repositório descartável (`raise ValueError` quando `deslocamento_ms == 2700`): `metricas ….p200 (d=2700 …): rc=1`, depois `mv: rename …p200.json …: No such file or directory`, `medir.sh rc=1` e 5 `trocado` (`.md`, `.json`, `.m200.md`, `.m200.json`, `.p200.md`), com `p200.md bytes: 0`. Não é provável com entrada malformada: 5 sondas de entrada inválida (trajetória vazia, `move` inválido, esperado vazio, evento fora da lista) saíram todas com `rc=2`. Por isso a severidade é MENOR. | Antes do laço de `mv`, conferir que os seis arquivos existem em `$tmp` e que o `.json` não está vazio (`[ -s ]`). Se faltar algum, `log` com "nenhum relatório trocado" e `exit 1` com `PARAR`, sem tocar em `$rel`. Outra opção, na própria ferramenta: tratar exceção inesperada com um código próprio (≥ 3). Precisa de linha no `opr_register` (dono sugerido: arquiteto, que desenhou o R3-2; execução pelo qa-publicacao). |
| 2 | MENOR | `work/0005-etapa0-medicao/scripts/medir.sh:89` | A linha do diário "nenhum relatório trocado", que o plano exige no R3-2 ("O que muda no comportamento visível"), não tem asserção no ensaio. Sem a linha, T1 a T3 continuam verdes. | `quebra3` verde (saída acima). | No ensaio, fazer T1 e T2 conferirem a linha `nenhum relatório trocado` antes do `PARAR` (por exemplo, `grep -c` na saída do `medir.sh`). Pode ir no mesmo OPR do achado 1. |
| 3 | MENOR | `work/0005-etapa0-medicao/evidencias/qc/LEIA-ME.md:16` | O item novo da lista entrou depois do parágrafo sobre a origem do `level.csv` do C11, separado dos outros itens da lista (linhas 7-13). Em Markdown, ele aparece como uma lista solta, depois de um parágrafo que não tem relação com ele. | `cat LEIA-ME.md`. | Subir a linha para logo depois da linha `Q-C5` (linha 13). É cosmético, pode ir junto com o próximo commit de evidência. |

Nenhum achado BLOQUEANTE ou IMPORTANTE. O commit faz o que o R3-2 pede, é igual byte a byte ao patch aprovado e corrige o achado 4 nos caminhos que ele descreve (código 2 em qualquer corrida). O achado 1 é uma lacuna do próprio desenho do R3-2: o código 1 é ambíguo e o plano o trata como aceito. O código não diverge do plano.

Registro: a instrução desta revisão proíbe tocar em `registers/`, então os achados 1 e 2 **não** foram lançados no `registers/opr_register.md`. O orquestrador precisa lançá-los com dono (sugestão: um OPR só, dono arquiteto). Antes de escolher o número, confira os IDs já usados na branch principal e nos worktrees, porque já houve colisão de OPR entre branches. O achado 3 é cosmético e pode ir no mesmo registro ou ser corrigido no próximo commit de evidência.

## Não verificado

- **Saída por sinal (Ctrl-C, `SIGTERM`, `SIGHUP`) durante as corridas ou entre os três `mv`.** O teste que eu montei (bash em subshell com `trap … EXIT`, `kill -TERM/-INT/-HUP`) foi bloqueado pela checagem de segurança do ambiente, porque o script passado a `bash -c` contém `rm`. Não contornei. Não afirmo se a pasta temporária é apagada nesses casos, nem qual o estado de `$rel` se o sinal chegar entre os `mv`. A troca na linha 92 tem três `mv`, então não é atômica entre arquivos. A janela é de milissegundos, mas existe.
- Aparelho, adb, áudio, Vosk e modelo não foram usados, como pedido. Isso não faz diferença aqui, porque o ramo `metricas` não depende de nenhum deles.
- `./gradlew verifyMergedManifest` não foi rodado, porque o commit não toca `app/`, `etapa0/` nem dependências.
