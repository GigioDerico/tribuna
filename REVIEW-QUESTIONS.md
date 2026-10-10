# Perguntas de revisão

Consultadas pelo `arquiteto` no PLAN, pelo `revisor` no REVIEW e pelo `qa-publicacao` no QA. Cada pergunta tem exemplos do Tribuna; responda com evidência, não com impressão.

**Q1. O que consome isso e o que quebra quando estiver errado?**
Todo campo, evento ou arquivo novo tem um consumidor declarado. Ex.: um campo novo em `AlignState` — quem lê? Uma coluna nova no `trajectory.csv` — o `metricas.py` usa? Se ninguém consome, não deveria existir.

**Q2. Você forçou isso a falhar?**
Um teste só vale se ficar vermelho contra uma quebra proposital. Ex.: inverta o descarte de `SessionId` antigo no ViewModel e confirme que o teste de callback atrasado falha; remova o limite `highWater − 3` e confirme que o teste de recuos sucessivos falha. Registre qual quebra foi feita.

**Q3. A aprovação passa porque não havia nada para checar?**
Ex.: "nenhum salto errado" num teste em que o cursor nunca se moveu; "manifesto sem INTERNET" checado num manifesto que não foi gerado; "95% dentro do atraso" calculado sobre zero amostras de leitura normal. Exija contagem mínima de casos.

**Q4. Você leu o artefato real ou o seu modelo mental dele?**
Ex.: o manifesto mesclado em `build/intermediates`, não o `AndroidManifest.xml` do código; o JSON que o Vosk realmente devolve, não o que o TRD supõe; o `.so` dentro do AAB, não o da dependência. Abra o arquivo antes de afirmar.

**Q5. O escopo da verificação é igual ao escopo da alegação?**
Ex.: "alinhado a 16 KB" precisa cobrir `libvosk.so` e `libjnidispatch.so` em todas as ABIs; "texto nunca persistido" precisa cobrir todo lugar que recebe o texto, não só o DataStore; "funciona no tablet" precisa das duas orientações.

**Q6. Algum dado do discurso escapa da memória?**
Siga o texto e o áudio por todo caminho novo: logs, `SavedStateHandle`, `rememberSaveable`, crash report, arquivo temporário, intent, clipboard depois do Colar, nome de arquivo, painel de depuração no release. Qualquer saída é reprovação.

**Q7. Este número é medição real ou artefato de contagem?**
Ex.: atraso medido em tokens ou em palavras faladas? Amostras de improviso e pausa entram na conta? Latência medida do fim da palavra no áudio ou do início? Declare a regra de contagem ao lado do número.

**Q8. O artefato ainda descreve o estado que diz descrever?**
Ex.: o `expected.csv` foi gerado da gravação atual ou de um take anterior? O golden de transcrição corresponde à versão do modelo em `assets`? O TRD ainda bate com o contrato no código? Confira a data e a origem.
