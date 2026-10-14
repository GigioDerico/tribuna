# Evidências Q-C1 a Q-C7 (0005)

Ensaio do `medir.sh metricas` novo num repositório descartável (git init em pasta temporária) com as ferramentas
de `etapa0/ferramentas/` e dados sintéticos do caso T04 (improviso; `testes/test_cli_0005._caso_t04`, texto
`etapa0/textos/T04.txt`). Nenhum discurso pessoal, nenhuma gravação real, sem aparelho.

- `medir-metricas-t04-sintetico.txt`: `vizinhas.py` roda (rc=0), três corridas com a folha própria
  (`.m200.folha.events.csv`, `.p200.folha.events.csv`) e d = 2500 / 2300 / 2700.
- `t04-tres-corridas.txt`: Salto errado PASSA, PASSA, REPROVA (o caso do revisor; o esperado do plano).
- `medir-metricas-reproducao-sintetico.txt`: ramo `reproducao`.
- Folha inválida: `vizinhas.py` sai com 2 e o script para com "PARAR: código 2".
- Q-C5: `ls etapa0/relatorios/*/*.m200.json` não acha arquivo (lista vazia).
