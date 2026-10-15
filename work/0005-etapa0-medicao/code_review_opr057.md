**APROVADO**

# Revisão — 0005-etapa0-medicao, ensaio da linha do diário "nenhum relatório trocado" (OPR-057, achados 2 e 3 da revisão do OPR-055)

**Revisor:** revisor · **Data:** 2026-10-15 · **Commit revisado:** `d72cbba` (base `c2cbe71`)

Escopo: `evidencias/qc/opr057-linha-diario.txt` (novo, 75 linhas), um bullet em `evidencias/qc/LEIA-ME.md` e o ensaio `$TMPDIR/tribuna-0005/opr057-ensaio.sh`, que fica fora do repositório, como o `r3-ensaio.sh` e o `r4-ensaio.sh`. Referências: achados 2 e 3 de `code_review_medir_troca_opr055.md` (linhas 98-99) e `impl_plan.md`, rodadas 3 e 4.

## Regras inegociáveis

| Regra | Verificado como | Resultado |
|---|---|---|
| Manifesto só com `RECORD_AUDIO` | O commit não toca `app/`, dependência nem manifesto (`git show --stat`: só 2 arquivos em `evidencias/qc/`) | N.A. |
| Texto não persiste nem vai para log | Só usa o texto de teste T04 (`etapa0/textos/`), num repositório descartável | OK |
| Áudio fora de `etapa0/` | Não há áudio | N.A. |
| `align/`, contratos, sessão, cursor, Compose | Não foram tocados | N.A. |

## Saída real

```
$ git show d72cbba --stat
 work/0005-etapa0-medicao/evidencias/qc/LEIA-ME.md  |  1 +
 .../evidencias/qc/opr057-linha-diario.txt          | 75 ++++++++++++++++++++++
 2 files changed, 76 insertions(+)

$ git show d72cbba -- .../LEIA-ME.md      (só uma linha +, nenhuma linha -)
+- `opr057-linha-diario.txt`: OPR-057 (achado 2 da revisão do OPR-055). Ensaio novo `opr057-ensaio.sh` confere ...

$ shasum -a 256 medir.sh opr057-ensaio.sh r3-ensaio.sh r4-ensaio.sh
e35a59dbf6fd2a4e7efae8878bd031aab71289ebdf3fabfbc76eca31be582c0a  work/0005-etapa0-medicao/scripts/medir.sh
169bd3a5ad1bb65504cbef7b5278621ca7f02a7861f294489285237641f82858  $TMPDIR/tribuna-0005/opr057-ensaio.sh
2abacc87f2156fea34ae2709163eac9bd7949793a4dcc907e49536dab76543c9  $TMPDIR/tribuna-0005/r3-ensaio.sh   (= r3-medir-troca-junta.txt:3)
d46aa6c8da9061eb355c62e0348c3a6e9c856037072d1c7d4567a00ab33a4d8d  $TMPDIR/tribuna-0005/r4-ensaio.sh   (= impl_plan.md:6429, r4-medir-saida-completa.txt:3)

$ sed -n '53,75p' opr057-linha-diario.txt | diff - $TMPDIR/tribuna-0005/opr057-ensaio.sh && echo TEXTO_IGUAL
TEXTO_IGUAL
```

Reexecução numa cópia no scratchpad (ensaios copiados de `$TMPDIR/tribuna-0005/`, pastas novas). As quebras são `sed '103d'` e `sed '107d'` do `medir.sh` do repositório, com o mesmo sha da evidência (`dd47ffae…`, `e2d51f4c…`):

```
=== OPR-057 original: e35a59dbf6fd ===
T1..S5: presente (1): OK  (8 linhas)
casos verificados: 8 (T1, T2, S0 a S5); falhas: 0
RESULTADO: VERDE
rc=0
=== OPR-057 quebra-a: dd47ffae7ac3 ===          (sem a linha 103, código 2)
T1: x0 (esperado 1): FALHOU; T2: x0: FALHOU; S0: ausente ou repetida: FALHOU; S1..S5: OK
casos verificados: 8 (T1, T2, S0 a S5); falhas: 3
RESULTADO: VERMELHO
rc=1
=== OPR-057 quebra-b: e2d51f4cd68d ===          (sem a linha 107, saída recusada)
T1, T2, S0: OK; S1..S5: ausente ou repetida: FALHOU
casos verificados: 8 (T1, T2, S0 a S5); falhas: 5
RESULTADO: VERMELHO
rc=1
```

O resultado é igual ao das seções 1 a 3 da evidência.

Sondas extras do revisor (Q2: "exatamente uma vez" e caminho de sucesso):

```
dupa (linha 103 duplicada):      T1 x2 FALHOU, T2 x2 FALHOU, S0 FALHOU; falhas: 3; VERMELHO; rc=1
dupb (linha 107 duplicada):      S1..S5 FALHOU; falhas: 5; VERMELHO; rc=1
suc  (linha falsa "nenhum relatório trocado" logo antes do "seis relatórios trocados", linha 113):
                                 falhas: 0; RESULTADO: VERDE; rc=0   <- achado 1
```

Árvore depois das execuções:

```
$ git status --short     (só .claude/agent-memory/*, que já estavam assim antes da revisão)
$ shasum medir.sh        e35a59dbf6fd2a4e7efa…  (sem mudança)
$ PYTHONDONTWRITEBYTECODE=1 python3 -B etapa0/ferramentas/testar.py
testar.py: 329 testes, 0 falhas, 0 erros, 0 pulados
```

## Leitura do ensaio (`opr057-ensaio.sh`, texto em `opr057-linha-diario.txt:53-75`)

- **Não edita os ensaios r3 e r4:** só os chama com `bash` (linhas 9-10), e os sha continuam os registrados.
- **Exige a linha exatamente uma vez nos 8 casos:**
  - T1 e T2 (linhas 13-16): o `awk` recorta o trecho que vai de `--- Tn:` até o próximo `--- `, o `grep -c` conta e o ensaio exige `= 1`.
  - S0 a S5 (linhas 17-20): exige no `r4.txt` a linha de resumo do `r4-ensaio.sh` (`r4-ensaio.sh:55`) com `rc=1` e `linhas 'nenhum relatório trocado': 1;`. O `;` impede que um valor como `10` case.
  - As sondas `dup*` confirmam que uma repetição também fica vermelha.
- **Falha fechada:**
  - Se um dos ensaios internos parar cedo (base falhou, `PARAR`), o trecho ou a linha de resumo não aparece, `n` fica `0` ou vazio e o caso FALHA.
  - Com o arquivo ausente, o `grep -c` não imprime nada e o caso também FALHA.
  - O código de saída final segue `falhas` (linha 23).
- **Padrão do grep:** é literal. O padrão das sondas S casa com o formato exato do `r4-ensaio.sh:55` (prefixo `^Sn: medir.sh rc=1; `), e a quebra b, vermelha em S1 a S5, mostra que ele não passa por engano. A nota da evidência (linha 49) registra que a primeira versão errava esse padrão e foi corrigida antes das provas.

## Achados

| # | Severidade | Arquivo:linha | Achado | Evidência | O que resolve |
|---|---|---|---|---|---|
| 1 | MENOR | `$TMPDIR/tribuna-0005/opr057-ensaio.sh:13-20` (texto em `evidencias/qc/opr057-linha-diario.txt:65-72`) | O ensaio só confere que a linha **aparece** onde a troca é recusada. Ele não confere que ela **não aparece** quando os seis trocam (T3 do `r3-ensaio.sh`). Um `medir.sh` que escreve "nenhum relatório trocado" no diário depois de trocar os seis passa verde. O diário mentiria justamente no caso normal. Isso fica fora do que o achado 2 pediu (T1 e T2), por isso não bloqueia. | Sonda `suc` acima: `falhas: 0; RESULTADO: VERDE`, com `r3.txt:35` contendo `nenhum relatório trocado (sonda revisor)` no trecho T3. | No laço da linha 13, incluir T3 com contagem esperada `0`, ou seja, a contagem esperada passa a ser por caso. Registrar no `opr_register` (dono sugerido: qa-publicacao, dono do OPR-057). |
| 2 | MENOR | commit `d72cbba`, mensagem | A mensagem cita "LEIA-ME, achado 3", mas o achado 3 (item solto depois do parágrafo do C11) já tinha sido corrigido no `86cb7c6`, que moveu o item do r3 para a lista. Este commit só acrescenta um bullet, no lugar certo (linha 16, dentro da lista, antes do parágrafo do C11), sem alterar nenhum item existente. O conteúdo está correto; só a atribuição na mensagem é imprecisa. | `git show 86cb7c6 -- …/LEIA-ME.md` (o `-` na linha do r3 depois do parágrafo e o `+` dentro da lista). | Nada a corrigir no código. Ao fechar o OPR-057, citar `86cb7c6` para o achado 3. |

Nenhum achado BLOQUEANTE ou IMPORTANTE. O achado 2 da revisão do OPR-055 está resolvido: a linha do diário tem asserção nos 8 casos de recusa, e as duas quebras e as duas duplicações ficam vermelhas. O achado 3 está resolvido no estado atual do `LEIA-ME.md`.

Registro: a instrução desta revisão proíbe tocar em `registers/`. O achado 1 precisa ser lançado pelo orquestrador (ou junto com o fechamento do OPR-057), conferindo antes os IDs usados nas outras branches. O OPR-057 continua "Aberto" no registro (`registers/opr_register.md:62`), e quem fecha é o orquestrador.

## Não verificado

- Aparelho, adb, áudio, Vosk e modelo: não usados, por instrução, e não necessários para este escopo.
- `verifyMergedManifest` e Gradle: não rodados, porque o commit não toca `app/` nem o build.
- O ensaio continua fora do repositório (`$TMPDIR`), como os ensaios r3 e r4. Para reproduzi-lo, é preciso usar o texto da evidência, e conferi que ele é igual byte a byte ao arquivo.
