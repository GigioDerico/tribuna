# Roteiro da S1 (0005, Etapa 0)

Montado pelo orquestrador a partir de `spec.md` (roteiro do humano) e da Fase D do runbook em `impl_plan.md`. Estado em 2026-10-14. Não é critério: o protocolo é o `spec.md`.

## 0. Antes da S1 (agente, com o S23 no cabo)

Só exige o cabo. Confirmar: aparelho no cabo, desbloqueado, tela ligada.

1. Confirmar que o APK **debug** está instalado (sha256 contra `evidencias/a3-sha256.txt`). O último passo do release deixou o release instalado.
2. **C11 refeito com debug** (OPR-054). O level.csv usado até agora veio do release. Esperado `|d| <= 200`.
3. **C13 (F3):** `reproduzir.py ... --ciclos 10 --painel`, com `novo` e `reset`. Tabela em `evidencias/f3-estrategia.md`.
4. **D23:** toque fora da tela, em `evidencias/b8-toque.txt`.
5. **B7 com release:** se o ensaio sintético não chegar ao cursor 214, refazer depois da S1 com a gravação real. Registrar como informativo até lá.
6. Abertura: `aparelho.sh salvar && aplicar`, `screen_off_timeout` em 1 h para o L30, `memoria.sh S1 120` em segundo plano, criar `etapa0/relatorios/S1/notas.md`.

## 1. Preparação do humano (uma vez, ~15 min)

- Segunda pessoa combinada (timbre bem diferente), sala silenciosa.
- Suporte para o celular a ~50 cm da boca, de frente. MacBook ao lado, à mesma distância.
- Cronômetro para os 30 s de silêncio.
- QuickTime: Arquivo → Nova Gravação de Áudio, microfone interno, qualidade **Máxima**. Pasta `~/Tribuna-gravacoes/`.
- Improviso (T04, L30) e fala da outra pessoa (T07) **inventados e neutros**, porque vão para os WAV.

## 2. Ordem dos takes

T01 → T02 (t1, t2, t3) → T03 → T04 → T05 → T06 → T07 → T08 → T09 → T10 → pausa → L30.

- **T01:** leitura normal (~2 min).
- **T02:** referências "três dezesseis" (t1), "três e dezesseis" (t2), "capítulo três, versículo dezesseis" (t3). Números por extenso.
- **T03:** normal; o refrão aparece três vezes.
- **T04:** depois de "...entrar na conversa.", improvisar ~2 min e voltar ao 2º parágrafo.
- **T05:** do fim do 1º parágrafo direto ao 3º (pular "Isso tem um custo...").
- **T06:** depois de "...muitos rodeios.", 30 s de silêncio total, sem tocar.
- **T07:** depois de "...levada a sério.", tocar **Pausar**. A outra pessoa fala ~10 s perto do celular. Tocar **Ouvir** e seguir.
- **T08:** repetir logo depois de dizer "Isso tem um custo." e "Um momento de pausa não precisa ser preenchido."
- **T09:** normal.
- **T10:** 1º e 2º parágrafos bem devagar, com pausas longas; os três últimos rápido, sem pausas.
- **L30:** seguir as linhas `//`; improviso de 1 min; **Pausar**, 20 s de ruído de salão feito pela outra pessoa, **Ouvir**; pular "Um detalhe prático"; 30 s de silêncio; depois de "como uma ponte velha", parar, rolar até "Imagine", tocar nela e seguir; repetir "Ninguém chega longe de uma vez só."; ler até o fim. Não tocar em palavra com a leitura pausada.

## 3. Cada take (humano)

1. Esperar o agente dizer "pronto" e abrir o texto no Mac.
2. Clicar em gravar no QuickTime.
3. Tocar **Ouvir** no celular. Esperar ~2 s, **bater uma palma**, esperar ~2 s.
4. Ler no ritmo natural, com a instrução do take.
5. No fim, ~3 s de silêncio, tocar **Parar**, e dizer **"fim"**.
6. O agente exporta e pede o `.m4a`. Salvar como `<nome>.m4a` em `~/Tribuna-gravacoes/`.
7. Dizer "seguiu o roteiro" ou "teve X" (tosse, tropeço, ruído). Se saiu muito do roteiro, refazer como `t<n+1>`. O anterior não é apagado.

## 4. Conferência na tela (~10 min, no fim)

- Durante uma leitura do T01, tocar numa palavra visível à frente. O destaque saltou e a rolagem acompanhou?
- Girar o celular durante a leitura. Ela **não** pausou?
- Opcional (OPR-022): 1º parágrafo do T01 com animação de 150, 375 e 600 ms. Qual pareceu melhor?

## 5. Fechamento

- Agente restaura os ajustes do aparelho (`aparelho.sh restaurar`) e encerra o `memoria.sh`.
- Agente grava `oficiais.tsv` (uma linha por take que vale) e roda o B4 (JSON limpo).

## Depois da S1 (sem o humano, exceto a escuta)

Conversão e `medir.sh sinc S1`, folha, métricas, veredito S1. Se CS1–CS4 reprovarem de forma clara, o agente avisa antes de gastar S2 e S3. Depois: escuta de revisão (30–45 min) e D1 (vosk 0.3.44 e modelo de ~1,7 GB, com ~10 GB livres).

## Pendências que não travam a S1

- **Pergunta 1** (vosk + modelo): autorizada (opção A). Precisa estar pronta antes do passo D.
- **Pergunta 2** (cota do LFS ~300 MB e consentimento por escrito da `voz2`): antes do push dos WAV.
