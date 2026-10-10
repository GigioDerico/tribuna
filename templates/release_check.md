**Pronto para envio: SIM | NÃO**

# Publicação — <NNNN>-<versão>

**Responsável:** qa-publicacao · **Data:** <AAAA-MM-DD> · **Commit:** <hash> · **versionCode / versionName:** <...>

**Autorização humana:** <citar o arquivo ou a linha na pasta da tarefa onde o humano autorizou, com data>

## Build

| Verificação | Comando | Resultado | Evidência |
|---|---|---|---|
| AAB de release gerado | `./gradlew bundleRelease` | | |
| Manifesto mesclado só com `RECORD_AUDIO` | `./gradlew verifyMergedManifest` | | |
| 16 KB em todas as ABIs (`libvosk.so`, `libjnidispatch.so`) | `./gradlew verify16kAlignment` | | |
| Tamanho do AAB < 80 MB | leitura do arquivo | | |
| Sem painel de depuração nem exportação de transcrições no release | inspeção do AAB / build config | | |
| `allowBackup="false"` e regras de extração vazias | manifesto mesclado | | |

## Conformidade (docs/CONFORMIDADE.md)

- [ ] Política de privacidade publicada na URL e acessível no app
- [ ] Formulário de segurança de dados: nenhum dado coletado nem compartilhado
- [ ] Classificação etária respondida; público-alvo 13+
- [ ] Avisos de licença na tela Sobre
- [ ] Ficha da loja sem marcas de terceiros
- [ ] Conta verificada; teste fechado cumprido (se conta pessoal nova)

## Pendências

<Nada | lista>
