**Veredito: APROVADO**

# Revisão — 0005-etapa0-medicao, `medir.sh metricas` recusa saída incompleta do `metricas.py` (OPR-056; plano R4-1)

**Revisor:** revisor · **Data:** 2026-10-15 · **Base do diff:** `86cb7c6^` (`4e2b391`) → `86cb7c6`

O escopo é o commit `86cb7c6`, que muda `work/0005-etapa0-medicao/scripts/medir.sh` (+20), cria `evidencias/qc/r4-medir-saida-completa.txt` e altera duas linhas em `evidencias/qc/LEIA-ME.md`. A referência é a seção "Correções da revisão rodada 4 (OPR-056)" do `impl_plan.md` (linhas 6365-6640, commit `4e2b391`). O problema original é o achado 1 de `code_review_medir_troca_opr055.md`.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Só `RECORD_AUDIO` no manifesto mesclado | O commit não toca `app/`, `etapa0/`, manifesto nem dependência. `git diff --stat HEAD -- etapa0 docs` sai vazio | N.A. (nada mudou) |
| Texto não persiste nem vai para log | O diário recebe só o nome do arquivo, o nome do tipo da exceção e a palavra do veredito (S1-S5). A conferência só lê da pasta temporária do R3-2. Há uma observação teórica abaixo (O1) | OK |
| Áudio não gravado fora de `etapa0/` | O script não lê nem escreve áudio neste ramo | OK |
| `align/` sem Android, contratos do TRD, `SessionId`, `highWater − 3`, só Compose | Nada mudou no app | N.A. |

## Saída real dos comandos

### Patch igual ao plano

Extraí os blocos `bash`, `python` e `diff` da seção do plano e apliquei o patch sobre `100a021` num diretório descartável.

```
$ shasum -a 256 r4-ensaio.sh r4-quebras.py work/0005-etapa0-medicao/scripts/medir.sh
d46aa6c8da9061eb355c62e0348c3a6e9c856037072d1c7d4567a00ab33a4d8d  r4-ensaio.sh
b25be3f4bd9a5e57471ca6b406159c9d4bfd5d1b37096f6ca9dde91fbdd848bc  r4-quebras.py
e35a59dbf6fd2a4e7efae8878bd031aab71289ebdf3fabfbc76eca31be582c0a  work/0005-etapa0-medicao/scripts/medir.sh
$ git show 100a021:…/medir.sh | shasum -a 256      # e também 86cb7c6^
9af077c606c642ae9bae0236f6db36190855611d1ccb2ce4e7427257906bbcfb
$ git apply --check medir-r4.patch && git apply medir-r4.patch && cmp ap/…/medir.sh work/…/medir.sh && echo CMP_IGUAL
CMP_IGUAL
$ bash -n work/0005-etapa0-medicao/scripts/medir.sh; echo "bash -n rc=$?"
bash -n rc=0
$ shasum -a 256 r3-ensaio.sh
2abacc87f2156fea34ae2709163eac9bd7949793a4dcc907e49536dab76543c9
```

O diff do `medir.sh` é exatamente o patch do plano, sem nada a mais nem a menos. A mudança no `LEIA-ME.md` também segue o passo 7: a linha `r3-…` foi movida para logo depois de "Q-C5", e a linha `r4-…` foi acrescentada abaixo dela.

### Leitura do código (`medir.sh` 13-23 e 95-113)

- `.md` vazio ou ausente: `[ -s "$1.md" ]` (linha 14).
- `.json` lido com `json.load` e com a chave `veredito`: qualquer `OSError`, `ValueError` (inclui `JSONDecodeError` e `UnicodeDecodeError`), `KeyError` ou `TypeError` leva a sair com o motivo (linhas 15-20).
- Código e veredito: `(v == "PASSA") != (rc == 0)` (linha 21). Isso dá 0 se e só se `PASSA`.
- Nenhuma troca acontece antes das três corridas: a conferência roda dentro do laço (linha 105), e o `mv` só vem depois dele (linha 112).
- Em qualquer recusa, o script grava duas linhas no diário, o `PARAR` e `exit 1` (linhas 106-109). Nada em `$rel` é tocado.
- `trap 'rm -r -- "$tmp"' EXIT` (linha 95) cobre o `exit 1` novo, e as sondas confirmam 0 pasta temporária.
- Todos os caminhos estão entre aspas.
- O código 2 continua indo para o `rc_aceito` (linhas 103-104), que sai com 1, como antes (S0 e T1/T2).

### Ensaio da rodada 4 (repositório descartável no scratchpad)

```
=== ATUAL: 9af077c606c6 ===   S0: VERDE  S1-S5: VERMELHO
=== NOVO: e35a59dbf6fd ===    S0: VERDE  S1: VERDE  S2: VERDE  S3: VERDE  S4: VERDE  S5: VERDE
=== NOVO-ESPACO: e35a59dbf6fd === (pasta "pasta com espaço $HOME")  S0-S5: VERDE
```

Os motivos no NOVO são estes, e todos batem com a tabela do plano:

```
S1|  … saída recusada (T04_giorgio_sala_t1.p200.md vazio ou ausente)
S2|  … saída recusada (T04_giorgio_sala_t1.p200.md vazio ou ausente)
S3|  … saída recusada (T04_giorgio_sala_t1.p200.json ausente, vazio ou ilegível (FileNotFoundError))
S4|  … saída recusada (T04_giorgio_sala_t1.p200.json ausente, vazio ou ilegível (JSONDecodeError))
S5|  … saída recusada (T04_giorgio_sala_t1.json com veredito PASSA, que não bate com o código 1)
cada S0-S5: medir.sh rc=1; 'nenhum relatório trocado': 1; PARAR: 1; seis iguais; temporárias: 0
```

Também comparei com as seções da evidência `r4-medir-saida-completa.txt`, depois de tirar os carimbos de hora, o prefixo da pasta e os hashes. `r4-novo` e `r4-r3novo` ficaram idênticos. Em `r4-atual`, a única diferença é o sufixo aleatório do `mktemp` nas duas linhas `mv: rename …metricas.XXXXXX/…p200.json …: No such file or directory`, de S1 e S3. Essas linhas são o achado 1 reproduzido.

### Sem regressão da rodada 3 (`r3-ensaio.sh` com o `medir.sh` novo)

```
T1: medir.sh rc=1; os seis relatórios iguais aos de antes; temporárias 0
T2: medir.sh rc=1; os seis relatórios iguais aos de antes; temporárias 0
T3: … p200 (d=2700 …): rc=1                    # REPROVA legítimo, aceito
T3: seis relatórios trocados (d, .m200, .p200) em etapa0/relatorios/S1; medir.sh rc=0
T3: relatórios trocados: 6; três "iguais ao metricas.py direto"; temporárias 0; git status fora: 0
```

### Falso positivo (corrida legítima recusada?)

- Na base do ensaio, `d.p200` sai `REPROVA` com código 1 e é aceita (a base precisa passar, senão o ensaio para), e o T3 acima também a aceita.
- Fiz ainda uma sonda própria: a cópia do `metricas.py` força `REPROVA` nas três corridas. O resultado foi `rc=1` três vezes, depois `seis relatórios trocados …`, `medir.sh rc=0` e os três `.json` com `REPROVA`. Os seis arquivos foram trocados e não sobrou pasta temporária. Nenhuma corrida legítima foi recusada.
- Também testei a função isolada numa pasta com espaço e `$`:

```
ACEITA  | REPROVA rc1 completo
ACEITA  | PASSA rc0 completo
RECUSA  | REPROVA rc0 -> r.json com veredito REPROVA, que não bate com o código 0
RECUSA  | PASSA rc1 -> r.json com veredito PASSA, que não bate com o código 1
RECUSA  | md vazio / json ausente (FileNotFoundError) / vazio, cortado (JSONDecodeError) / sem veredito (KeyError)
RECUSA  | json lista ou string (TypeError) / UTF-8 inválido (UnicodeDecodeError)
ACEITA  | veredito null rc1 / NAO_SE_APLICA rc1      # inalcançável pelo metricas.py (veredito de topo só PASSA/REPROVA, l. 288)
RECUSA  | aninhado profundo rc1 -> Traceback … RecursionError   # ver O1
```

### Quebras qA-qF (geradas pelo `r4-quebras.py` do plano)

```
=== qA: 2054b2c4a159 === S0: VERDE S1: VERDE S2: VERMELHO S3: VERDE S4: VERDE S5: VERDE
=== qB: 196e78748928 === S0: VERDE S1: VERDE S2: VERDE S3: VERMELHO S4: VERDE S5: VERDE
=== qC: 6a06037d607e === S0: VERDE S1: VERDE S2: VERDE S3: VERDE S4: VERMELHO S5: VERMELHO
=== qD: 4cff9d6d524c === S0: VERDE S1: VERDE S2: VERDE S3: VERDE S4: VERDE S5: VERMELHO
=== qE: 7e3a684f268b === S0: VERDE S1: VERMELHO S2: VERMELHO S3: VERMELHO S4: VERMELHO S5: VERMELHO
=== qF: dd47ffae7ac3 === S0: VERMELHO S1: VERDE S2: VERDE S3: VERDE S4: VERDE S5: VERDE
```

Cada quebra fica vermelha exatamente nas sondas que o plano lista. Os hashes curtos e as linhas são iguais aos da evidência; a única diferença é um espaço no fim da linha.

Também fiz uma quebra própria, `qR`, que tira um dos sentidos do "se e só se": `(v == "PASSA") != (rc == 0)` vira `v == "PASSA" and rc != 0`. O resultado foi `=== qR: e04a99ade25d === S0-S5: VERDE`, ou seja, a quebra passou despercebida. Ver achado 1.

### Sanidade

```
$ PYTHONDONTWRITEBYTECODE=1 python3 -B etapa0/ferramentas/testar.py | tail -1
testar.py: 329 testes, 0 falhas, 0 erros, 0 pulados
$ git diff --stat HEAD -- etapa0 docs
(vazio)
```

## Achados

1. **MENOR**: `work/0005-etapa0-medicao/scripts/medir.sh:21`. O lado "código 0 com veredito diferente de `PASSA` é recusado" não tem sonda: com a quebra `qR`, as seis sondas continuam verdes (Q3). Esse caso não acontece com o `metricas.py` atual, porque ele só devolve 0 na linha 547, depois de `PASSA`, e uma exceção sai com 1. Por isso a lacuna não deixa passar nenhum conjunto misto hoje. Para resolver, basta acrescentar uma sonda S6 (por exemplo, `return 0` incondicional numa corrida que reprova, como a d+200) ao ensaio, ou registrar que o sentido é só defensivo. Não bloqueia. Vai para o orquestrador registrar no `opr_register.md`, com dono sugerido `arquiteto`, porque esta revisão não toca `registers/`.

## Observações (sem achado)

- **O1.** Se o `.json` provocar uma exceção que a lista do `except` não cobre (`RecursionError` num JSON aninhado a 100 mil níveis, ou `MemoryError`), o traceback inteiro do `python3` entra no `motivo` e, com ele, no diário. A corrida é recusada mesmo assim, que é o certo. O traceback traz caminhos da stdlib e a linha do próprio `-c`, nunca conteúdo do relatório, e o `metricas.py` não grava JSON assim. Isso contradiz a frase do plano "nunca recebe mensagem de exceção" só nesse caso teórico, sem risco de privacidade.
- **Não verificado:** um sinal entre os três `mv` (o plano deixa esse caso fora do escopo, como na revisão anterior). Também não verifiquei o comportamento com `/bin/sh` em vez de `bash`: o script exige bash pelo `comum.sh`.

Nenhum aparelho, adb, áudio, Vosk ou modelo foi usado. Todos os ensaios rodaram em repositórios descartáveis no scratchpad, e a árvore principal só ganhou este relatório.
