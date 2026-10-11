---
name: align-decisoes-0002
description: Decisões de arquitetura do align na 0002 (words = cauda é interpretação; guarda da palavra mais nova; ambiguidade; golden 0006) e surpresas do algoritmo do TRD
metadata:
  type: project
---

Decidido no PLAN da 0002 (2026-10-11), registrado em `work/0002-etapa0-align/impl_plan.md` (A1–A6) e OPR-007..010:

- **`words` em `Aligner.advance` = cauda das confirmadas** (desde reposition/início de sessão) é interpretação, não mudança de contrato: o TRD só diz "palavras confirmadas", §Alinhamento 2/4 exige "últimas k"/"4 ou mais", e não havia chamador. Quem chama mantém a cauda (basta max(k,4)) e zera em reposition e nova sessão.
- **Surpresa do algoritmo literal do TRD:** a primeira palavra fora do roteiro avança o cursor 1 token, porque as k−1 anteriores casam com a posição seguinte: (k−1)/k = 0,667 ≥ 0,60 (Normal) e 0,75 ≥ 0,70 (Conservador). Correção adotada (A2): o vencedor só conta se a palavra mais nova casar com o token com sim ≥ limite curto; senão é miss. Texto do TRD proposto, aguardando humano (OPR-009).
- **Ágil (k=2)** salta em pares de palavras comuns ("e a" dá média 1,0 qualquer que seja o peso) e em "quatro versículos" do T02. Fora do spec; vai para a calibração da 0005 (OPR-010).
- **Recuperação ambígua com o refrão do T03 não é testável com os presets:** as cópias distam 33 tokens e W=40 sempre alcança uma (vira LongJump na janela). O teste usa o L30 ("no fim do ano a" aparece 2x, ~1.900 tokens de distância).
- **Golden de transcrições** vira a tarefa 0006-etapa0-golden depois da 0005; a 0003 precisa exportar Partial/Final brutos com tempo.

**Why:** spec 0002 (D1–D23) deixou esses pontos para o arquiteto; o protótipo mostrou que o T04 reprovaria sem A2.

**How to apply:** no PLAN da 0003 e do app, o ViewModel/tela mantém a cauda em memória (é conteúdo do discurso, regra 2); não remover A2 sem teste de improviso verde; ao calibrar na 0005, olhar OPR-009/010. Ver [[prova-do-plano-em-copia]].
