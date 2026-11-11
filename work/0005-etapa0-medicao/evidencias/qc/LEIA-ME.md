# Evidências Q-C1 a Q-C7 (0005)

Ensaio do `medir.sh metricas` novo num repositório descartável (git init em pasta temporária) com as ferramentas
de `etapa0/ferramentas/` e dados sintéticos do caso T04 (improviso; `testes/test_cli_0005._caso_t04`, texto
`etapa0/textos/T04.txt`). Nenhum discurso pessoal, nenhuma gravação real, sem aparelho.

- `medir-metricas-t04-sintetico.txt`: `vizinhas.py` roda (rc=0), três corridas com a folha própria
  (`.m200.folha.events.csv`, `.p200.folha.events.csv`) e d = 2500 / 2300 / 2700.
- `t04-tres-corridas.txt`: Salto errado PASSA, PASSA, REPROVA (o caso do revisor; o esperado do plano).
- `medir-metricas-reproducao-sintetico.txt`: ramo `reproducao`.
- `medir-reproducao-salto.txt`: ramo `reproducao` com as linhas de Salto errado PASSA, PASSA, REPROVA (OPR-054, achado 2b).
- `folha-invalida-rc2.txt`: folha com cabeçalho diferente; `vizinhas.py` sai com 2 e o `medir.sh` para (ele mesmo sai com 1) com "PARAR: código 2"; nenhuma corrida roda (OPR-054, achado 2a).
- Q-C5: `ls etapa0/relatorios/*/*.m200.json` não acha arquivo (lista vazia).
- `r3-medir-troca-junta.txt`: R3-2 (OPR-055, achado 4). Com código 2 em qualquer corrida, nenhum dos seis relatórios muda. Com sucesso, os seis trocam juntos e batem com o `metricas.py` direto. Versão antiga e duas quebras em vermelho.
- `r4-medir-saida-completa.txt`: R4-1 (OPR-056). Uma corrida que sai com 0 ou 1 sem deixar o `.md` com conteúdo e o `.json` legível e coerente com o código faz o `medir.sh` parar sem trocar nenhum dos seis relatórios. Seis sondas verdes, versão anterior vermelha em S1 a S5, seis quebras vermelhas no ponto previsto.
- `opr057-linha-diario.txt`: OPR-057 (achado 2 da revisão do OPR-055). Ensaio novo `opr057-ensaio.sh` confere a linha "nenhum relatório trocado" em T1, T2 e S0 a S5. Verde no `medir.sh` atual; vermelho nas quebras que tiram a linha 103 (T1, T2, S0) ou a 107 (S1 a S5).

Origem do `level.csv` do C11 parcial (`../b8-sinc-reproducao.txt`; OPR-054, achado 1): arquivo da reprodução de 2026-10-12 12:28, feita com o APK release instalado (C10), não com o debug que o C11 manda. O C11 não foi refeito aqui; fica para o item 0 com o aparelho.
