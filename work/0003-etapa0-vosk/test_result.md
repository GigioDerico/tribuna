**Resultado: PASSOU** (critérios A a D; critério E fora: não verificável sem aparelho, vai para a 0005)

# Testes — 0003-etapa0-vosk

**Responsável:** qa-publicacao · **Data:** 2026-10-11 · **Commit testado:** 3a5491f (branch `work/0003-etapa0-vosk`, árvore limpa em `etapa0/` no início e no fim)

## Ambiente

| Item | Valor |
|---|---|
| Aparelho(s) | Nenhum. `adb devices` devolveu lista vazia (`List of devices attached`). Critério E não executado. |
| Máquina | MacBook (arm64), macOS 27.0.1, JDK 21.0.10, Gradle 9.7.1, build-tools 36.0.0 (`aapt2`, `zipalign`) |
| Ambiente acústico / voz / distância | Não se aplica (sem aparelho, sem microfone) |
| Brilho / modo avião | Não se aplica |

## Execução

| Suíte / comando | Resultado | Evidência |
|---|---|---|
| `testDebugUnitTest :core:test lint ktlintCheck verifyMergedManifest --rerun-tasks` | BUILD SUCCESSFUL, 75 tarefas executadas | `evidencias/qa-1-testes.txt` |
| Contagem (XMLs de resultado) | `:app` 228, `:core` 118, `build-logic` 27; 0 falhas, 0 erros, 0 ignorados; `@Ignore`/`@Disabled` em `etapa0/`: 0 | `evidencias/qa-1-contagem.txt`, `qa-5-greps.txt` |
| `clean assembleDebug assembleRelease bundleRelease` | BUILD SUCCESSFUL | `evidencias/qa-2-build.txt` |
| `verify16kAlignment` | FALHA esperada (OPR-024), ver B5 | `evidencias/qa-4-verify16k.txt` |
| `connectedDebugAndroidTest` | Não executado (sem aparelho; fora do escopo da 0003, spec "Fora do escopo") | n/a |

## Critérios de aceite

Contagem de amostras (Q3): estes critérios são de comando ou de lógica na JVM; não há métrica estatística. Regra: um teste conta quando está nos XMLs de `testDebugUnitTest`/`:core:test`/build-logic com `failures=0` e `skipped=0`; total 373 testes (228+118+27).

| Critério | Como foi executado | Resultado | Evidência |
|---|---|---|---|
| A1 | Build limpo dos três artefatos. AAB 49.590.809 bytes (meta 80 MB, passa); APK de release 74.214.036 bytes. `uuid` do modelo no APK (`assets/model-pt/uuid`) e no AAB (`base/assets/model-pt/uuid`), `libvosk.so` e `libjnidispatch.so` nas 4 ABIs (arm64-v8a, armeabi-v7a, x86, x86_64) em ambos | OK | `qa-2-build.txt`, `qa-2-tamanho.txt`, `qa-2-apk-itens.txt`, `qa-2-aab-itens.txt` |
| A2 | Suítes verdes, contagens acima, nenhum teste desativado, ktlint com 2 fontes executadas (`runKtlintCheckOverMainSourceSet`, `...TestSourceSet`) | OK. Ressalva: confirmei que cada critério B–D tem teste por amostragem de 22 quebras Q2 (A8/B/C/D), não item a item; ver "Não testado" | `qa-1-*` |
| A3 | Não re-executado `:app:dependencies`; versões vêm do catalog e a evidência da construção está em `A3-dependencias.txt`. Eu só conferi que o build limpo resolve (A1) | OK (por evidência existente + build limpo) | `evidencias/A3-dependencias.txt` |
| A4 | `git lfs ls-files` = 15 ponteiros em `model-pt/` (todos com `*`, objeto presente); `git lfs fsck` = OK; `uuid` = `vosk-model-small-pt-0.3 sha256=6e1ce909...e93b7`, igual no APK e no repositório | OK | `qa-6-lfs.txt`, `qa-2-*` |
| A5 | `git diff main --stat -- etapa0/core/src/main` vazio | OK | `qa-5-greps.txt` |
| A6 | `grep SpeechService`, `grep res/layout` e `find ... res/layout` vazios | OK | `qa-5-greps.txt` |
| A7 | `rememberSaveable\|SavedStateHandle\|DataStore\|MediaRecorder`: só 2 comentários dizendo "nunca rememberSaveable" (`ReaderScreen.kt:80`, `FieldSync.kt:7`); escritas em arquivo: só `ModelInstaller.kt:82` (cópia do modelo de assets) e `ExportWriter.kt:40` (texto da exportação); `Log.` vazio; `registerReceiver`/`Socket`/`URL(` vazios | OK | `qa-5-greps.txt` |
| A8 | `:app:ktlintCheck --rerun-tasks` verde, tarefas `main` e `test` executadas. Violação proposital em `ui/QaViolacaoMain.kt` e `ui/QaViolacaoTest.kt` (novos, removidos depois): `ktlintMainSourceSetCheck FAILED` e `ktlintTestSourceSetCheck FAILED` (BUILD FAILED nas duas) | OK | `qa-7-ktlint.txt`, `qa-7-ktlint-negativo.txt`, `qa-7-ktlint-negativo-test.txt` |
| B1 | `aapt2 dump permissions` do APK de release: só `RECORD_AUDIO`; nenhum elemento `<permission>` (0); `verifyMergedManifest` verde. Observação: o `receiver` do profileinstaller usa `android:permission="android.permission.DUMP"` como proteção do componente; não é `uses-permission` | OK | `qa-3-aapt2.txt`, `qa-1-testes.txt` |
| B2 | `scripts/verificar-manifesto-negativo.sh`: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, CAMERA reprovam; "só RECORD_AUDIO" passa; exit=0; `app/src/debug` removido (`ls app/src` = main, test) | OK | `qa-3-negativo.txt` |
| B3 | Manifesto do APK: `allowBackup=false`, `dataExtractionRules=@xml`, `fullBackupContent=@xml`. Lint verde. O conteúdo das regras de exclusão não foi relido por mim, só a presença no manifesto (a evidência da construção está em `B3-manifesto-mesclado.txt`) | OK (parcial) | `qa-3-aapt2.txt`, `evidencias/B3-manifesto-mesclado.txt` |
| B4 | `--dry-run`: `check` lista `verifyDebugMergedManifest`, `verifyReleaseMergedManifest`, `verifyMergedManifest`; `assembleRelease` e `bundleRelease` listam `verifyReleaseMergedManifest`; `assembleDebug` lista `verifyDebugMergedManifest`. Q2-C1 (apagar o `configureEach`) deixa de listar nas duas (contagem 0; restaurado, 1) | OK | `qa-3-dryrun.txt`, `qa-8-C1.txt` |
| B5 | `verify16kAlignment` roda contra APK e AAB: 12 bibliotecas por contêiner (3 libs x 4 ABIs), 24 linhas LOAD. Reprova só por `libvosk.so` armeabi-v7a (LOAD 4096) e x86 (LOAD 4096) no APK e no AAB (4 problemas, 2 por contêiner). As outras 20 leituras: LOAD 16384, inclusive `libjnidispatch.so` e `libandroidx.graphics.path.so` em todas as ABIs, `libvosk.so` arm64-v8a e x86_64. `zipalign -c -P 16 -v 4` do APK: Verification successful. OPR-024 aberto no registro com caminhos (a)–(d). Resultado previsto no B5 do spec; a verificação não foi afrouxada | OK (como previsto) | `qa-4-verify16k.txt`, `qa-4-zipalign.txt`, `registers/opr_register.md` OPR-024 |
| B6 | `mapping.txt`: `org.vosk.Recognizer -> org.vosk.Recognizer:` (linha 223985) e `com.sun.jna.Native -> com.sun.jna.Native:` (189262) | OK | `qa-2-mapping.txt` |
| C1–C11 | Cobertos pelos 373 testes verdes na JVM (nenhum carrega `org.vosk.*`); Q2 de C1, C4, C7, C10, C11, mais Error de sessão trocada: ver tabela abaixo | OK | `qa-1-*`, `qa-8-*` |
| D1–D9 | Idem, no `:app` com `FakeSpeechEngine`; Q2 de D1, D2, D4, D6, D7, D9, OPR-027 | OK | `qa-1-*`, `qa-8-*` |
| E | Permissão em execução, `AudioRecord`, JSON real do Vosk, carga do modelo, R8 no aparelho, tela, rotação | **Não verificável sem aparelho -> 0005** | `adb devices` vazio |

## Métricas da Etapa 0 (quando aplicável)

Não se aplica. Esta tarefa não grava nem mede; as gravações e o relatório são da 0005.

## Teste falso (Q2)

Quebras feitas por mim, uma por vez, restauradas com `git checkout --` (árvore limpa ao final). Cada quebra roda `testDebugUnitTest --continue` (app + build-logic). Linha de base verde antes (0 falhas) e verde depois (0 falhas).

| Quebra | Resultado | Teste(s) que ficaram vermelhos |
|---|---|---|
| Q2-C1 (apagar `configureEach` do plugin) | Vermelho por comando: dry-run deixa de listar `verifyReleaseMergedManifest` em assembleRelease e bundleRelease | `qa-8-C1.txt` |
| Q2-C2 (apagar `tools:node="remove"`) | Vermelho por comando: `verifyReleaseMergedManifest FAILED`, "declaração de permissão não permitida: ...DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" | `qa-8-C2.txt` |
| Q2-R1 (Error do reconhecedor com id da sessão atual do motor; bloqueante 1 da rodada 1) | Vermelho | `VoskEngineSessionTest.erroDoReconhecedorComSessaoTrocadaNoMeioDoBloco_saiComOIdAntigo` |
| Q2-R1b (Error com `SessionId(nextSession - 1)`) | Vermelho | o mesmo |
| Q2-I1 (Partial atrasado entra no reconciliador por `active ?: event.session`; bloqueante 2) | Vermelho | `ReaderSessionTest.parcialAtrasadoDaSessaoAnterior_naoChegaAoReconciliador`, `...eventosAtrasadosEntramNaExportacaoComOIdOriginalEAccepted0` |
| Q2-I2 (`fromActive` aceita parcial com sessão ativa qualquer) | Vermelho | os mesmos dois |
| Q2-V3 (onStop em Paused cai no ramo Preparing) | Vermelho | `ReaderLifecycleTest.onStopDeVerdadeComALeituraPausada_fechaACaptura_eNaVoltaReabreMantendoPausada` |
| Q2-V3b (guarda do onStop só `Reading`) | Vermelho | o mesmo |
| Q2-I4a (OPR-027: `inBackground = false`) | Vermelho | `ReaderLifecycleTest.onStopDeVerdadeEnquantoOModeloCarrega_naoAbreOMicrofoneQuandoACargaTermina`, `ReaderGuardsTest.cargaTerminadaEmSegundoPlano_...` |
| Q2-I4b (OPR-027: sem ramo Preparing no onStop) | Vermelho | os mesmos dois |
| Q2-E1 (Partial com a sessão atual do motor) | Vermelho | `VoskEngineSessionTest.resultadoProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo` |
| Q2-E2 (Final com a sessão atual) | Vermelho | `VoskEngineSessionTest.finalProduzidoDepoisDeEndSessionENovoBeginSession_saiComOIdAntigo` |
| Q2-E7 (Reset ignora mudança de vocabulário) | Vermelho | `VoskEngineSessionTest.estrategiaReset_vocabularioMudou_criaReconhecedorNovo` |
| Q2-V1 (sem filtro de sessão no controlador) | Vermelho (6) | `ReaderGuardsTest.parcialDeSessaoAntigaSemSessaoAtiva_eIgnorado`, `ReaderSessionTest` x4 (inclui `parcialEFinalAtrasados...` e `falaNaPausa...`), `ReaderExportTest.hipotesesLidasDeVolta_...` |
| Q2-V17 (reposition antes do endSession) | Vermelho | `ReaderTapTest.toqueNaPalavraDuranteALeitura_endSessionRepositionBeginSession_nessaOrdem` |
| Q2-V9 (reconciliador não zera na sessão) | Vermelho | `ReaderSessionTest.falaNaPausa_naoMoveOCursor_eAoRetomarASessaoENovaComCaudaVazia` |
| Q2-V18 (rotação fecha a captura) | Vermelho (3) | `ReaderLifecycleTest.rotacaoComALeituraPausada_naoReabreACaptura`, `...onStopComMudancaDeConfiguracao_naoFechaSessaoNemCaptura`, `ReaderPhaseGuardsTest.rotacaoEnquantoOModeloCarrega_...` |
| Q2-S1 (`needsCopy` invertido) | Vermelho (6) | `ModelInstallerTest` (uuidIgualNoDestino_naoCopia, uuidDiferenteNoDestino_copiaDeNovo, mesmaVersao_..., e mais 3) |
| Q2-S5 (escala 32767) | Vermelho (2) | `AudioLevelTest.meioDaEscala_daMeio`, `...soContaAsAmostrasLidas` |
| Q2-S6 (WAV aceita não PCM) | Vermelho | `WavAudioSourceTest.wavNaoPcm_eRecusado` |
| Q2-S8 (WAV aceita 44,1 kHz) | Vermelho | `WavAudioSourceTest.wav44kHz_eRecusado` |
| Q2-X3 (replay sem reset por sessão) | Vermelho | `ExportFilesTest.reprocessar_soAsAceitas_zerandoAFraseACadaSessao` |
| Q2-R29 (catálogo com `startsWith`) | Vermelho | `TestTextCatalogTest.textoDeTesteComAlgoAcrescentado_naoIdentifica` |
| Q2-B1 (build-logic: manifesto sem declarações) | Vermelho (3) | `ManifestPermissionCheckTest.declaracaoDePermissaoPropria_falha`, `...soADeclaracao_semUsesPermission_tambemFalha`, `...grupoEArvoreDePermissao_falham` |

Total: 22 quebras de código mais C1 e C2 por comando = 24; todas vermelhas, todas desfeitas (`git status --short etapa0` vazio; suíte final com 0 falhas). Saídas: `evidencias/qa-8-quebras-q2.txt` (a primeira execução marcou R1/R1b como "troca não aplicada" porque o texto aparecia 2 vezes no arquivo; refeitas com contexto da linha 269 em `qa-8-quebras-q2-R1.txt`). A violação proposital do ktlint (A8) conta como quebra extra, vermelha.

## Não testado

- Critério E inteiro (aparelho): permissão de microfone, `AudioRecord` a 16 kHz, JSON real do Vosk (C6 só segue a documentação), carga do modelo e durações, R8 rodando no aparelho, tela, rotação e segundo plano reais, cota de LFS num `git push` real. Sem aparelho conectado. Vai para a 0005.
- `connectedDebugAndroidTest` (fora do escopo da 0003).
- Q2 não feita por mim para C2, C5, C8, C9, D5, D8 (o plano e a varredura de 402 + 333 quebras do construtor cobrem; eu não refiz). A contagem "≥ 1 teste por critério" do A2 não foi conferida item a item por mim.
- Conteúdo dos arquivos de regras de backup (B3): só a presença no manifesto final; `:app:dependencies` (A3) não foi re-rodado.
- Corrida real de `ThreadCaptureDriver` (join de 1 s, `release` do `onCleared`): só `ManualCaptureDriver` na JVM, como disse o revisor.
