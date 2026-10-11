# Análise de impacto — 0003-etapa0-vosk (portão humano 1: manifesto)

**Autor:** arquiteto · **Data:** 2026-10-11 · **Aprovação humana:** pendente

Escopo: só o `etapa0/app/src/main/AndroidManifest.xml` e dois recursos XML novos que ele referencia. Nenhum contrato Kotlin do TRD (`SpeechEngine`, `SpeechEvent`, `SessionId`, `Token`, `Aligner`/`AlignState`, `PrompterUiState`, `ReadingPhase`), nenhuma chave de DataStore e nenhuma regra do `CLAUDE.md` muda. Origem: spec 0003, "Perguntas em aberto" 1; brainstorm D23, D24, D25; OPR-001 e OPR-002.

## O que muda

Manifesto atual (`etapa0/app/src/main/AndroidManifest.xml`, 10 linhas):

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <application
        android:allowBackup="false"
        android:label="@string/app_name" />
</manifest>
```

Manifesto proposto. Foi validado no merge real numa cópia descartável (ver "Por quê", E3):

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <!-- OPR-001: o androidx.core injeta esta permissão própria. O app não chama
         ContextCompat.registerReceiver com RECEIVER_NOT_EXPORTED (ver TRD, Manifesto). -->
    <permission
        android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        tools:node="remove" />
    <uses-permission
        android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
        tools:node="remove" />

    <application
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:label="@string/app_name">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

São três mudanças:

- **(a) OPR-001, D24.** Remover os **dois** elementos que o `androidx.core` injeta, `<permission … protectionLevel="signature">` e `<uses-permission>` de `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, com `tools:node="remove"`. O pedido original só falava do `<uses-permission>`. Proponho tirar também a declaração `<permission>`: sem isso, o manifesto final continuaria declarando uma permissão do app, e o `verifyMergedManifest` hoje não olha para `<permission>` (ver "Busca", linha 12).
- **(b) OPR-002, D23.** Adicionar `android:dataExtractionRules="@xml/data_extraction_rules"` e `android:fullBackupContent="@xml/backup_rules"`. Correção de redação em relação ao pedido: "regras vazias" não basta. Seção `<cloud-backup>` ou `<device-transfer>` vazia ou ausente não exclui nada (ver E5). As regras precisam **excluir explicitamente** todos os domínios:
  - `res/xml/data_extraction_rules.xml`: `<data-extraction-rules>` com `<cloud-backup>` e `<device-transfer>`, cada uma com `<exclude domain="X" path="." />` para X ∈ {`root`, `file`, `database`, `sharedpref`, `external`, `device_root`, `device_file`, `device_database`, `device_sharedpref`}.
  - `res/xml/backup_rules.xml`: `<full-backup-content>` com os mesmos nove `<exclude … path="." />`. Esse arquivo é para o Android 8 a 11 (minSdk 26). Sem ele, o lint continua com `DataExtractionRules` (E5), e o critério B3 do spec pede esse aviso zerado.
- **(c) D25.** Declarar `.MainActivity` (uma `ComponentActivity` em Compose) como lançadora, com `android:exported="true"`, obrigatório para quem tem intent-filter no targetSdk 31 ou mais. Não proponho `configChanges` nem `screenOrientation`: a rotação recria a Activity e o código confere `isChangingConfigurations` (TRD Segundo plano; brainstorm D16).

## Por quê

Evidência por comando. Cópia descartável: `rsync` de `etapa0/` para o scratchpad. O repositório não foi alterado. As dependências do spec foram postas direto no `app/build.gradle.kts` da cópia.

**E1. A permissão aparece de fato.** Com Compose BOM 2026.09.00, activity-compose 1.13.0, lifecycle-*-compose 2.11.0, vosk-android 0.3.75 e jna 5.19.1@aar, `:app:processReleaseMainManifest` gera no manifesto mesclado:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<permission android:name="io.github.gigioderico.tribuna.etapa0.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
    android:protectionLevel="signature" />
<uses-permission android:name="io.github.gigioderico.tribuna.etapa0.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />
```

A origem é `androidx.core:core:1.18.0/AndroidManifest.xml` (`${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`). Entre as 76 bibliotecas do grafo, é a única que declara `<permission>` ou `<uses-permission>`. A verificação atual reprova:

```
> Task :app:verifyDebugMergedManifest FAILED
> Task :app:verifyReleaseMergedManifest FAILED
    - permissão não permitida: io.github.gigioderico.tribuna.etapa0.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
  Permitidas: android.permission.RECORD_AUDIO
```

**E2. Para que a permissão serve no `androidx.core` 1.18.0.** Lido no bytecode com `javap -c` de `ContextCompat`, `ContextCompat$Api26Impl` e `ContextCompat$Api33Impl`:

- `ContextCompat.registerReceiver(ctx, receiver, filter, permission, handler, flags)`:
  - API 33 ou mais: repassa as flags ao `Context.registerReceiver` do sistema e não usa a permissão.
  - API 26 a 32, só quando `flags` tem `RECEIVER_NOT_EXPORTED` (4) e `permission == null`: chama `obtainAndCheckReceiverPermission`, que monta `<pacote>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, confere com `PermissionChecker.checkSelfPermission` e, **se o app não tiver a permissão, lança `RuntimeException`** ("Permission … is required by your application to receive broadcasts…"). Com `RECEIVER_EXPORTED` (2), o caminho não toca na permissão.
- Consequência: remover a permissão é seguro **enquanto ninguém no processo chamar `ContextCompat.registerReceiver` (ou o `Context.receiveBroadcasts*` do `core-ktx`) com `RECEIVER_NOT_EXPORTED` num aparelho com Android 8 a 12L**. Se alguém chamar, a falha não é silenciosa: é exceção na hora do registro.

**E3. Quem chama `registerReceiver` no grafo real.** Varredura de todos os `.class` das 76 bibliotecas de `releaseRuntimeClasspath` (12.158 classes; `grep -a registerReceiver` e depois `javap -c`), feita com o grafo compatível com compileSdk 36 (ver E4):

| Classe | Chamada | Flags | Depende da permissão? |
|---|---|---|---|
| `androidx.compose.ui.adaptive.MediaQuery_androidKt$obtainUiMediaScope$4$1` (ui-android 1.11.4) | `ContextCompat.registerReceiver(ctx, r, IntentFilter("android.intent.action.DOCK_EVENT"), 2)` | `RECEIVER_EXPORTED` | Não |
| `androidx.core.content.ContextKt` (core-ktx 1.18.0) | `receiveBroadcasts`/`receiveBroadcastsAsync` repassam as flags de quem chama | do chamador | Só se alguém chamar com 4; **nenhuma classe do grafo chama** (`grep -a receiveBroadcasts` só acha o próprio `ContextKt`) |
| `androidx.localbroadcastmanager.content.LocalBroadcastManager` 1.0.0 | método próprio `registerReceiver`, em processo; não chama `Context.registerReceiver` (`javap`: só `getApplicationContext`, `getMainLooper`, `getContentResolver`) | não se aplica | Não |
| `vosk-android` 0.3.75, `jna` 5.19.1 | nenhuma chamada | não se aplica | Não |

No grafo com Compose 1.12.1 (BOM 2026.09.00) o resultado é o mesmo: só a `MediaQuery`, com flag 2. Nenhuma classe contém a string `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` fora do próprio `ContextCompat`. No repositório, `grep -rnE "registerReceiver|RECEIVER_NOT_EXPORTED|receiveBroadcasts|DYNAMIC_RECEIVER" etapa0 app` não devolve nada.

Com o manifesto proposto, na cópia:

```
verifyDebugMergedManifest: OK …/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml -> android.permission.RECORD_AUDIO
verifyReleaseMergedManifest: OK …/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml -> android.permission.RECORD_AUDIO
:app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease  ->  BUILD SUCCESSFUL in 1m 9s
aapt2 dump permissions app-release-unsigned.apk  ->  uses-permission: name='android.permission.RECORD_AUDIO'
aapt2 dump permissions app-debug.apk             ->  uses-permission: name='android.permission.RECORD_AUDIO'
bundle_manifest/release/processApplicationManifestReleaseForBundle/AndroidManifest.xml -> só uses-permission RECORD_AUDIO
scripts/verificar-manifesto-negativo.sh -> OK INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, CAMERA reprovadas; OK só RECORD_AUDIO passou
```

O `tools:node="remove"` resolve o placeholder `${applicationId}` e remove os dois elementos do manifesto mesclado de debug, de release e do bundle.

**E4. Achado colateral, não é de manifesto, mas afeta o PLAN e o D26.** A versão estável mais recente do Compose não compila com o compileSdk 36 fixado no `CLAUDE.md` (Stack). `:app:checkDebugAarMetadata` falha:

- Compose BOM 2026.09.00 (Compose 1.12.1) e 2026.08.00 (1.12.0): 11 bibliotecas exigem "compile against version 37 or later".
- Lifecycle `lifecycle-runtime-compose` e `lifecycle-viewmodel-compose` 2.11.0: mesma exigência.
- Combinação mais nova que compila com compileSdk 36: **Compose BOM 2026.06.01 (Compose 1.11.4, material3 1.4.0), lifecycle 2.10.0, activity-compose 1.13.0** (`androidx.core` resolvido: 1.18.0).

Decisão de arquiteto para o PLAN: fixar essas versões e manter compileSdk 36. Subir para 37 muda a Stack do `CLAUDE.md` e é outra decisão, fora deste portão. O lint vai avisar `GradleDependency` nessas três; o PLAN registra o motivo. Peço ao orquestrador que abra um OPR (dono: arquiteto) para "compileSdk 37 quando a Stack for revista".

**E5. Backup.** Conferido em developer.android.com/identity/data/autobackup:

- "If there are no rules for a particular backup mode, such as if the `<device-transfer>` section is missing, that mode is fully enabled for all content except for `no-backup` and `cache`."
- "For apps targeting Android 12 (API level 31) or higher … specifying `android:allowBackup="false"` disables cloud-based backup … but doesn't disable device-to-device transfers" em aparelhos de alguns fabricantes.

Por isso as regras **excluem** os nove domínios nas duas seções. Na cópia, o lint só para de dar `DataExtractionRules` depois que entra o `android:fullBackupContent`:

- Antes: `Warning: The attribute android:dataExtractionRules only applies for Android 12 and higher; since minSdkVersion is API 26 you should also set android:fullBackupContent`.
- Depois: o aviso some (`lintDebug`/`lintRelease` verdes; só restam `GradleDependency`, `UseTomlInstead` da cópia e `MissingApplicationIcon`, que já existia).

## Alternativas para (a)

- **(A) Remover com `tools:node="remove"` e proibir `RECEIVER_NOT_EXPORTED` via `ContextCompat` no app. Recomendada.**
  - Cumpre a letra da regra 1: o manifesto final tem só `RECORD_AUDIO`, provado por `aapt2`.
  - Não muda o `CLAUDE.md`. Não afrouxa a verificação; a endurece (ver abaixo).
  - Risco residual: exceção em Android 8 a 12L se uma dependência futura passar a chamar `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)`. Mitigação abaixo.
- **(B) Exceção explícita no `verifyMergedManifest` para `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.**
  - **Contradiz a letra da regra 1** ("O manifesto final só pode ter `RECORD_AUDIO`"): o manifesto passaria a ter duas `uses-permission`. Exigiria reescrever a regra inegociável e a descrição da tarefa ("permissão além de RECORD_AUDIO").
  - É uma permissão `signature` do próprio app, sem efeito de rede nem de privacidade. Mesmo assim, apareceria na lista de permissões do APK e do Play Console, e abriria precedente de lista de exceções na verificação, que a regra de processo 6 proíbe afrouxar.
  - Ganho: nenhum. Nada no grafo precisa da permissão (E3). Rejeitada.
- **(C1) Não depender do `androidx.core`.** Impossível: `activity-compose` e o Compose dependem dele, e a regra 8 exige Compose. Rejeitada.
- **(C2) Fixar `androidx.core` < 1.9.0**, anterior à injeção. O Compose atual exige `core` muito mais novo, e o Gradle resolve para cima. Rejeitada.
- **(C3) Remover só o `<uses-permission>` e manter a declaração `<permission>`.** Passa no `verifyMergedManifest` atual, que só lê `uses-permission`, mas o manifesto final continua declarando uma permissão do app. Não traz ganho sobre (A). Rejeitada.

**Mitigação do risco de (A).** Vai para o PLAN da 0003. É endurecimento, não exceção:

1. `ManifestPermissionCheck` (build-logic) passa a reprovar também qualquer `<permission>`, `<permission-group>` ou `<permission-tree>` no manifesto mesclado: "declaração de permissão não permitida: X". Teste em `ManifestPermissionCheckTest`, com caso negativo usando o XML de E1. Assim, se o `tools:node="remove"` sumir ou o nome mudar numa versão nova do `core`, o build falha.
2. Critério por comando no spec/plano:
   ```
   grep -rnE "registerReceiver|receiveBroadcasts|RECEIVER_NOT_EXPORTED" etapa0/app/src
   ```
   Deve sair vazio. A etapa0 não precisa de receptor dinâmico: ciclo de vida por `Lifecycle` (D16), permissão por `ActivityResult`.
3. Script de evidência `etapa0/scripts/varrer-registerreceiver.sh`, rodado a cada mudança de versão no catalog. Extrai os `.class` de `releaseRuntimeClasspath`, lista quem referencia `ContextCompat.registerReceiver` ou `receiveBroadcasts` e mostra a flag constante passada (`javap -c`). Passa se nenhuma chamada usar 4 ou flag não constante. A saída fica em `work/0003-etapa0-vosk/evidencias/`.
4. Se um dia o app precisar de receptor não exportado, usar `Context.registerReceiver(r, filter, Context.RECEIVER_NOT_EXPORTED)` só a partir do API 33, ou um receptor com `permission` de sistema. Nunca `ContextCompat` com flag 4 abaixo do API 33.

## Busca no repositório

Comandos usados (Q5):

```bash
find . -name AndroidManifest.xml -not -path "*/build/*"
grep -rn --exclude-dir=build --exclude-dir=.gradle -E "registerReceiver|RECEIVER_NOT_EXPORTED|receiveBroadcasts|DYNAMIC_RECEIVER" etapa0 app      # vazio
grep -rn --exclude-dir=build --exclude-dir=.gradle -E "dataExtractionRules|fullBackupContent|allowBackup|tools:node" etapa0 app docs CLAUDE.md
grep -rn --exclude-dir=build -E "RECORD_AUDIO|allowedPermissions|requiredPermissions" etapa0/build-logic etapa0/scripts
```

O `app/` (Tribuna) ainda não existe. Só há um manifesto no repositório.

| Arquivo:linha | Uso | Quebra? | Teste que cobre |
|---|---|---|---|
| `etapa0/app/src/main/AndroidManifest.xml:1-10` | manifesto que muda | Muda (a, b, c) | `verify{Debug,Release}MergedManifest`; `scripts/verificar-manifesto-negativo.sh`; `lint` |
| `etapa0/build-logic/.../ManifestPermissionCheck.kt:12,21-24` | lê só `uses-permission*` | Não quebra. Endurecimento proposto: também `<permission*>` | `ManifestPermissionCheckTest` (ganha casos novos) |
| `etapa0/build-logic/.../AndroidVerificationsPlugin.kt:18,26-27` | `allowed = required = {RECORD_AUDIO}` | Não muda (com B mudaria) | `ManifestPermissionCheckTest`; script negativo |
| `etapa0/build-logic/.../VerifyMergedManifestTask.kt:20-39` | lê o manifesto mesclado e reporta | Não muda | idem |
| `etapa0/build-logic/src/test/.../ManifestPermissionCheckTest.kt:19` | XML de teste com `allowBackup="false"` | Não | é o próprio teste |
| `etapa0/scripts/verificar-manifesto-negativo.sh:19-41` | sobrepõe manifesto de debug com permissões proibidas | Não (rodado na cópia com o manifesto novo: 4 reprovações e 1 aprovação, como esperado) | o próprio script |
| `docs/TRD.md:289` | "regras de extração de dados vazias" | Redação imprecisa (E5) | texto novo abaixo |
| `docs/CONFORMIDADE.md:16` | checklist "Manifesto final só com `RECORD_AUDIO`" | Não; (A) mantém, (B) contradiria | `aapt2 dump permissions` no release (critério novo do PLAN) |
| `etapa0/app/src/main/res/xml/` (novo) | `data_extraction_rules.xml`, `backup_rules.xml` | Novo, recurso e não layout (regra 8 intacta) | `lint` (`DataExtractionRules` zerado) |
| `etapa0/app/src/main/java/.../MainActivity.kt` (novo) | Activity declarada em (c) | Novo | `lint` (`MissingClass`), `assembleDebug` |

## Efeito em outros agentes

- **motor-voz:** implementa (a), (b), (c) e o endurecimento do `ManifestPermissionCheck` no BUILD da 0003. Não usa `registerReceiver`. Fixa as versões de E4.
- **interface:** escreve a `MainActivity` (Compose, `ComponentActivity`). Sem `configChanges`, sem receptor dinâmico.
- **qa-publicacao:** passa a anexar `aapt2 dump permissions` do APK de release (só `RECORD_AUDIO`), a saída do script de varredura e o lint sem `DataExtractionRules`.
- **Play Store / CONFORMIDADE.md:**
  - Com (A), o Play Console mostra só `RECORD_AUDIO`, o que bate com o checklist da linha 16 e com a declaração de segurança de dados (nada coletado, nada compartilhado).
  - O backup excluído nas duas seções sustenta a frase "não vai para backup" da política (linha 47) também no Android 12+ com transferência entre aparelhos.
  - CONFORMIDADE não muda de texto.
- **Ferramentas da Etapa 0 / goldens:** nada a regenerar.
- **App Tribuna (`app/`, futuro):** a mesma situação vai acontecer lá (Compose traz o mesmo `core`). O texto do TRD abaixo vale para os dois projetos.

## Texto proposto para o TRD

Substituir, em TRD §Ciclo de vida, privacidade e permissões → **Manifesto** (linhas 288–290):

```markdown
- Única permissão: RECORD_AUDIO. Sem INTERNET, sem ACCESS_NETWORK_STATE. A tela ligada usa a flag da janela, então WAKE_LOCK não é necessário.
- A permissão própria `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, que o androidx.core (1.9+) injeta, é removida no manifesto do app com `tools:node="remove"`, tanto o `<permission>` quanto o `<uses-permission>`. Por isso nenhum código do app chama `ContextCompat.registerReceiver` (nem `Context.receiveBroadcasts` do core-ktx) com `RECEIVER_NOT_EXPORTED`: do Android 8 ao 12L essa chamada lança exceção sem a permissão. Se um receptor não exportado for necessário, usar `Context.registerReceiver(…, RECEIVER_NOT_EXPORTED)` só a partir do API 33. A cada troca de versão de dependência, a varredura de `registerReceiver` no classpath de release confirma que nenhuma biblioteca usa esse caminho.
- android:allowBackup="false", e regras de extração e de backup que excluem todos os domínios (`root`, `file`, `database`, `sharedpref`, `external` e os `device_*`), em `cloud-backup` e em `device-transfer` (`android:dataExtractionRules`, Android 12+) e em `android:fullBackupContent` (Android 8–11). Seção ausente ou vazia não exclui nada, e no Android 12+ `allowBackup="false"` sozinho não desliga a transferência entre aparelhos.
- O build verifica o manifesto final mesclado: falha se houver `uses-permission` além de RECORD_AUDIO ou qualquer declaração `<permission>`, `<permission-group>` ou `<permission-tree>`.
```

A regra 1 do `CLAUDE.md` fica como está, porque a letra dela continua verdadeira com (A).

## Plano de migração

1. Aprovação humana desta CIA (portão 1).
2. Commit de documentação: o texto acima no `docs/TRD.md`, OPR-001 e OPR-002 marcados "decidido (portão 1, data)", OPR novo do E4.
3. BUILD da 0003, conforme o `impl_plan.md`:
   - teste novo em `ManifestPermissionCheckTest` (vermelho), depois o endurecimento do `ManifestPermissionCheck`;
   - recursos XML e manifesto;
   - `MainActivity`;
   - `verifyMergedManifest`, script negativo, `aapt2 dump permissions`, script de varredura e `lint`, com a saída em `evidencias/`.

   O manifesto e o endurecimento entram no mesmo commit, para o build nunca ficar verde com uma regra que ainda não é verificada.
4. Critério B1 do spec fica como está: (A) é a decisão que ele já supõe.

## O que o humano precisa aprovar (redação exata)

> Aprovo, para `etapa0/app/src/main/AndroidManifest.xml` (e, quando existir, para o manifesto do `app/`):
> 1. Remover com `tools:node="remove"` os elementos `<permission>` e `<uses-permission>` de `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` injetados pelo androidx.core, com a contrapartida de que o código do app não chama `ContextCompat.registerReceiver`/`receiveBroadcasts` com `RECEIVER_NOT_EXPORTED`, e de que o `verifyMergedManifest` passa a reprovar também qualquer declaração `<permission>`, `<permission-group>` e `<permission-tree>`.
> 2. Adicionar `android:dataExtractionRules` e `android:fullBackupContent` apontando para recursos XML que excluem os nove domínios em `cloud-backup`, `device-transfer` e no backup completo, mantendo `android:allowBackup="false"`.
> 3. Declarar `.MainActivity` como Activity lançadora (`exported="true"`, intent-filter MAIN/LAUNCHER), sem `configChanges` nem `screenOrientation`.
> 4. Substituir o texto do TRD §Manifesto pelo trecho proposto nesta CIA.
>
> Nenhuma permissão nova entra; o manifesto final continua só com `RECORD_AUDIO`. Rejeito a alternativa (B), exceção na verificação.

Atributos de Activity que não mexem em permissão, componente exportado, intent-filter, provider, service, receiver ou backup (por exemplo, `android:theme` com tema da plataforma) ficam dentro desta aprovação. Qualquer outra mudança de manifesto volta ao portão 1.
