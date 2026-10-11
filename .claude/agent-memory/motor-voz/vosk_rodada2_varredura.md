---
name: vosk-rodada2-varredura
description: Varredura de quebras por linha na 0003 rodada 2; 402 quebras em 2 s cada, só 20 equivalentes; lições sobre testes tautológicos e guardas redundantes
metadata:
  type: project
---

Rodada 2 do motor (2026-10-11): script Python em scratchpad aplica troca `(arquivo, linha, old, new)`, roda `:app:testDebugUnitTest -q` (~2 s com daemon quente), lê XMLs e restaura com `git checkout`. 402 quebras em ~15 min; 101 verdes antes, 20 equivalentes depois.

- Pegadinhas: número de linha errado dá IndexError e aborta o lote (tree fica limpa, nada escrito); `.sorted()` removido não compila (usar `.toList()`); mutante `i += 1` etc. pode travar, o runner tem timeout 300 s.
- Testes tautológicos achados: meta comparava `META_KEYS` com ele mesmo; falso de assets aceitava caminho sem `textos/`. Teste que importa constante da produção para comparar não prende nada: usar literal.
- Guardas redundantes (`ended` em end() + `decoderReleased` em release()) são equivalentes isoladas: provar com quebra dos dois juntos (par vermelho) e escrever `closeCalls` no FakeDecoder.
- `captureLost` stale (`capture !== lost`) é reproduzível sem threads: driver que guarda todos os passos + fonte cuja `read` chama stopCapture/startCapture antes de lançar.
- Normalizer une hífen/apóstrofo ("d'água" → "dagua"); Vocabulary separa para achar grafias do léxico; hífen é redundante com `spoken` do Tokenizer, apóstrofo não.

**Why:** a 0001 e a 0003 reprovaram pelo mesmo ponto cego (guarda sem teste). **How to apply:** em qualquer retrabalho, rodar a varredura por linha antes de entregar, não amostra.
