# Etapa 0 — Textos de teste e protocolo de medição

Material de entrada da Etapa 0 (TRD, seção Etapa 0). Sem ele, os critérios de saída não podem ser medidos: cada gravação precisa de um texto conhecido, de eventos marcados no tempo e de uma trajetória esperada (palavra × tempo) para comparar com o cursor do app.

## Regras do conjunto

- **Só textos de teste.** Todos os textos aqui são originais e escritos para teste. Discursos pessoais não entram no repositório nem nas gravações (TRD, Privacidade nos testes).
- **Referências sem o texto bíblico.** As referências (João 3:16 etc.) aparecem só como referência; o texto dos versículos não é reproduzido.
- **Duas vozes, no mínimo.** Giorgio e mais uma pessoa, de preferência com timbre bem diferente (por exemplo, voz feminina). Um alinhador calibrado numa voz só engana.
- **Mesmo protocolo sempre** (TRD, Protocolo de medição): aparelho de entrada, aparelho intermediário e tablet; microfone a cerca de 50 cm; brilho fixo; modo avião; uma rodada em sala silenciosa e outra num salão com eco.
- **Áudio:** WAV, 16 kHz, mono, PCM 16 bits — o mesmo formato que o app captura. Nome: `<texto>_<voz>_<ambiente>_<take>.wav`, por exemplo `T02_giorgio_salao_t2.wav`.

## Os textos

| ID  | Nome                           | Duração | O que testa                                                                  | Critério de saída ligado                    |
|-----|--------------------------------|---------|------------------------------------------------------------------------------|---------------------------------------------|
| T01 | Leitura limpa                  | ~2 min  | Referência de base: leitura normal, sem eventos                              | Atraso ≤ 3 palavras em 95% do tempo         |
| T02 | Números e referências          | ~1,5 min| Números por extenso, datas, porcentagens, referências em todas as variantes  | Nenhum salto errado; cursor não trava       |
| T03 | Trechos repetidos no roteiro   | ~1,5 min| Refrão que aparece três vezes; o cursor não pode pular para a cópia errada  | Nenhum salto errado                         |
| T04 | Improviso                      | ~4 min  | T01 com 2 min de fala fora do roteiro no meio                                | Cursor parado no improviso; retoma ao voltar|
| T05 | Pulo de parágrafo              | ~2 min  | T01 lido pulando o segundo parágrafo inteiro                                 | Recuperação em até 3 s                      |
| T06 | Silêncio                       | ~2,5 min| T01 com 30 s de silêncio no meio                                             | Nenhum movimento no silêncio                |
| T07 | Pausa com fala de terceiros    | ~2,5 min| Pausa por toque; outra pessoa fala 10 s perto do aparelho; retoma            | Nenhum movimento; retoma no ponto certo     |
| T08 | Orador repete frase            | ~2 min  | T01 com duas frases repetidas para ênfase                                    | Cursor não avança nem recua mais de 3       |
| T09 | Frases curtas e palavras comuns| ~1 min  | Muitas palavras de peso baixo ("de", "que", "e"); frases de 2 a 4 palavras   | Nenhum salto errado                         |
| T10 | Ritmo                          | ~3 min  | T01 lido devagar, depois rápido                                              | Atraso ≤ 3 palavras em 95% do tempo         |
| L30 | Discurso longo                 | 30 min  | Texto original de ~3.600 palavras com eventos programados (ver abaixo)       | Todos os critérios de voz, em uma trajetória|

### T01 — Leitura limpa

> Ouvir bem é uma habilidade que quase ninguém pratica de propósito. A maioria de nós escuta apenas o suficiente para preparar a próxima frase. Enquanto a outra pessoa fala, já estamos organizando a resposta, procurando um exemplo melhor ou esperando a pausa certa para entrar na conversa.
>
> Isso tem um custo. Quando não ouvimos de verdade, respondemos a uma pergunta que ninguém fez. Damos conselhos para um problema que não existe. E a pessoa sai da conversa com a sensação de que falou sozinha, mesmo tendo ouvido muitas palavras de volta.
>
> Ouvir com atenção começa com uma decisão simples: deixar a resposta para depois. Olhar para quem fala, perceber o tom da voz, notar o que ficou sem ser dito. Às vezes a frase mais importante vem no fim, quase escondida, depois de muitos rodeios.
>
> Outra parte é fazer boas perguntas. Uma pergunta curta e sincera mostra que a outra pessoa foi levada a sério. Perguntar como ela se sentiu, o que aconteceu depois ou o que ela gostaria que fosse diferente abre espaço para que a conversa vá mais fundo.
>
> Por fim, ouvir bem exige paciência com o silêncio. Um momento de pausa não precisa ser preenchido. Muitas vezes, é nesse intervalo que a pessoa encontra coragem para dizer o que realmente queria dizer.

### T02 — Números e referências

Ler cada take de um jeito: no take 1, as referências como "três dezesseis"; no take 2, "três e dezesseis"; no take 3, "capítulo três, versículo dezesseis". Números sempre por extenso, como se fala naturalmente.

> Em 1914, muita coisa mudou no mundo. Mais de 24 países se envolveram no conflito, e cerca de 3,5% da população de algumas regiões foi afetada diretamente.
>
> Hoje, vamos considerar quatro textos. O primeiro está em João 3:16. O segundo, em Salmo 23:1. O terceiro, em Mateus 24:14. E o último, em Romanos 12:2.
>
> Também vale ler 1 Coríntios 13:4-7, que fala de amor de um jeito muito prático. Repare que são 4 versículos, do 4 ao 7.
>
> Se você ler 2 capítulos por dia, termina o livro em 15 dias. Com 1 capítulo por dia, em 30. O importante é começar hoje, dia 10, e não esperar o dia 1 do mês que vem.

### T03 — Trechos repetidos no roteiro

> Cada manhã traz uma escolha. Podemos reclamar do que falta ou agradecer pelo que temos. **E a escolha é sempre nossa.**
>
> Quando o dia começa difícil, a tentação é deixar o humor decidir por nós. Mas um pequeno gesto, como um bom-dia sincero, muda o rumo da conversa. **E a escolha é sempre nossa.**
>
> No fim do dia, olhamos para trás e vemos o que construímos. Talvez pouco, talvez muito. O que importa é ter decidido com calma. **E a escolha é sempre nossa,** mesmo quando parece que não.

O refrão aparece três vezes, a última com um final diferente. O cursor tem de seguir a ocorrência certa em cada parágrafo.

### T04 a T08 — eventos sobre o T01

| ID  | Evento                                                                 | Onde                                        |
|-----|------------------------------------------------------------------------|---------------------------------------------|
| T04 | Improviso de ~2 min sobre o tema (exemplo pessoal inventado), depois volta ao texto | Depois de "...entrar na conversa." (fim do 1º parágrafo) |
| T05 | Pula o 2º parágrafo inteiro ("Isso tem um custo...")                   | Do fim do 1º direto para o 3º               |
| T06 | 30 s de silêncio total                                                 | Depois de "...muitos rodeios." (fim do 3º)  |
| T07 | Toca no vazio (pausa); outra pessoa fala 10 s perto do aparelho; toca de novo (retoma) | Depois de "...levada a sério."         |
| T08 | Repete duas frases para ênfase: "Isso tem um custo." e "Um momento de pausa não precisa ser preenchido." | Logo depois de dizê-las |

### T09 — Frases curtas e palavras comuns

> Sim. E então? É o que é. De novo, não. Que pena. Foi assim que eu vi. E o que você fez? Nada. Só esperei. E deu certo? Deu, mas não do jeito que eu queria. Que bom que você veio. Eu também acho. Não é fácil, mas é o certo. E é por isso que estamos aqui.

### T10 — Ritmo

T01 inteiro: os dois primeiros parágrafos bem devagar, com pausas longas entre frases; os três últimos rápido, sem pausas.

### L30 — Discurso longo

Texto original de ~3.600 palavras (28 a 30 min a ~130 palavras/min, com os eventos), escrito à parte para não repetir trechos dos textos curtos: docs/TESTES-ETAPA0-L30.md. Os eventos estão marcados no próprio texto em linhas de nota (`//`), que o app mostra mas não segue pela voz. Eventos programados:

| Minuto (aprox.) | Evento                                                        |
|-----------------|---------------------------------------------------------------|
| 3               | Duas referências bíblicas (uma com lista de versículos) e um número no mesmo parágrafo |
| 8               | Improviso de 1 min                                            |
| 12              | Pausa por toque, 20 s, com ruído de salão; retoma              |
| 15              | Pulo de um parágrafo pela voz                                 |
| 18              | Silêncio de 30 s                                              |
| 22              | Toque numa palavra 3 parágrafos à frente; continua dali       |
| 25              | Repetição de uma frase para ênfase                            |
| 28–30           | Última frase; cursor deve chegar ao fim e mostrar "Fim"       |

O texto tem cerca de 3.600 palavras lidas; com os eventos e as pausas naturais entre parágrafos, a leitura fica entre 28 e 30 minutos. Os minutos de cada evento valem como referência: o que manda é a folha de eventos gravada na hora.

## Folha de eventos

Cada gravação tem um arquivo `<nome>.events.csv`, preenchido na hora por quem acompanha o teste (ou a partir das marcas do modo de depuração):

```
t_ms,evento,detalhe
95300,improviso_inicio,
214800,improviso_fim,
350100,pausa_toque,
360400,retoma_toque,
```

Eventos válidos: `improviso_inicio`, `improviso_fim`, `silencio_inicio`, `silencio_fim`, `pausa_toque`, `retoma_toque`, `toque_palavra` (detalhe = índice do token), `pulo_inicio` (detalhe = índice do primeiro token pulado), `pulo_fim`.

## Trajetória esperada

A trajetória esperada diz, para cada token do roteiro, quando ele foi falado. É gerada fora do app, uma vez por gravação, e revisada à mão nos pontos de evento.

1. **Tempos das palavras.** Rodar a gravação num script no computador com o Vosk em Python e `setWords(true)`, que devolve início e fim de cada palavra reconhecida. Para a referência, usar o modelo grande de português: ele é mais preciso e, por ficar só na ferramenta de teste, a licença GPLv3 não afeta o app.
2. **Alinhamento global.** Alinhar a transcrição inteira ao roteiro com programação dinâmica (Needleman–Wunsch em nível de palavra, com as formas faladas do Tokenizer). Aqui o alinhador vê a gravação toda, então é bem mais confiável que o do app, que só vê o passado.
3. **Revisão manual.** Ouvir os trechos ao redor de cada evento da folha e corrigir o que o alinhamento errou. Tokens pulados ou não falados ficam sem tempo.
4. **Saída:** `<nome>.expected.csv`

```
token,palavra,t_inicio_ms,t_fim_ms
0,ouvir,1210,1530
1,bem,1530,1790
...
```

## Métricas: como calcular

O app de teste grava `<nome>.trajectory.csv` com cada mudança do cursor (`t_ms,cursor,move,score`). Um script compara com a trajetória esperada, amostrando a cada 100 ms.

- **Posição esperada em t:** o maior token cujo `t_fim_ms ≤ t`. Durante improviso, silêncio e pausa, a posição esperada fica congelada.
- **Atraso:** `esperado(t) − cursor(t)`, em tokens. Critério: ≤ 3 em 95% das amostras, contando só os intervalos de leitura normal (fora de improviso, silêncio, pausa e dos 3 s após um pulo).
- **Salto errado:** qualquer amostra com o cursor mais de 2 tokens **à frente** do esperado, ou qualquer movimento automático durante improviso, silêncio ou pausa. Critério: zero em toda a trajetória, mesmo que o cursor termine no lugar certo.
- **Recuperação de pulo:** do `pulo_fim` (primeira palavra falada depois do pulo) até a primeira amostra com atraso ≤ 3. Critério: ≤ 3 s.
- **Recuo:** nenhum movimento automático abaixo de `highWater − 3`, conferido pelo campo `move` e pelo histórico do cursor.
- **Latências:** lidas dos timestamps que o modo de depuração grava para cada evento (fim da palavra no áudio → `Partial` → cursor → fim da animação), conforme o TRD.

## Onde fica cada coisa no repositório da Etapa 0

```
etapa0/
  textos/            T01.txt ... T10.txt, L30.txt
  gravacoes/         *.wav (Git LFS)
  eventos/           *.events.csv
  esperado/          *.expected.csv
  trajetorias/       *.trajectory.csv (saída do app)
  ferramentas/
    timestamps.py    # Vosk + setWords(true) -> palavras com tempo
    alinhar.py       # Needleman–Wunsch roteiro × transcrição -> expected.csv
    metricas.py      # trajetória × esperado -> relatório dos critérios
  relatorios/        um relatório por rodada (aparelho, ambiente, voz)
```

As transcrições brutas de `gravacoes/` viram os golden de regressão do reconciliador e do alinhador (TRD, Testes).

## Intervalos e listas de versículos

O T02 traz intervalos e listas de versículos ("13:4-7", "12:1, 2"). Decidido em 10/10/2026 e registrado no TRD (Contratos e Normalização e tokens): cada intervalo ou lista forma um único token com o capítulo, com as formas "treze quatro a sete", "treze de quatro a sete" e "capítulo treze, versículos quatro a sete" (e "e" antes do último item de listas). Livros com número ("1 Coríntios") aceitam "primeira", "primeiro" e "um".

## Checklist antes de gravar

- [ ] T01 a T10 salvos em `etapa0/textos/`.
- [x] L30 escrito (docs/TESTES-ETAPA0-L30.md).
- [ ] Segunda voz confirmada.
- [ ] Salão com eco disponível para uma rodada.
- [ ] Aparelho de entrada, intermediário e tablet separados e carregados.
- [x] Regra dos intervalos de versículos decidida (TRD).
