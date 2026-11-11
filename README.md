# Tribuna

[![Download APK](https://img.shields.io/badge/Download-APK%20(v0.1.0)-success?style=for-the-badge&logo=android)](https://github.com/GigioDerico/tribuna/releases/latest)

> **Teleprompter inteligente que acompanha a voz do orador em tempo real, 100% offline e com privacidade absoluta.**

O **Tribuna** é um aplicativo Android moderno desenvolvido em Kotlin e Jetpack Compose. Utilizando processamento de linguagem natural e reconhecimento de voz *on-device* com Vosk, o app escuta a sua fala e sincroniza automaticamente a rolagem do roteiro com o seu ritmo natural — sem depender de conexões de rede, servidores externos ou velocidades fixas pré-definidas.

---

## 🌟 Novidades da Versão 0.1 (`v0.1.0`)

A versão `v0.1.0` consolida a primeira entrega completa do motor de leitura e experiência do usuário no palco:

- 📊 **Barra de Progressão em Tempo Real (PRD D5):** Barra de leitura sutil de 3 dp no topo da tela indicando a porcentagem exata do discurso já apresentada.
- ⏱️ **Tempo-Alvo com Alerta Silencioso (PRD D3):** Defina a duração planejada do seu discurso (de 5 a 60 minutos, ou Livre). Conforme o prazo se aproxima, a tela exibe alertas visuais perimétricos elegantes e 100% silenciosos (borda âmbar a 5 min do fim e borda vermelha/coral a 1 min).
- 🎭 **Dicas de Palco no Roteiro (`//`) (PRD T5):** Adicione instruções cênicas ou anotações de respiração iniciando a linha com `//` (ex: `// [Pausa dramática]`, `// [Sorria e olhe para o público]`). Elas são estilizadas em tom âmbar e itálico, sendo ignoradas automaticamente pelo motor de voz.
- 💡 **Tela Sempre Ligada (PRD D4):** A tela permanece ativa e com brilho constante durante toda a apresentação via `FLAG_KEEP_SCREEN_ON`, sem exigir permissão de sistema `WAKE_LOCK`.
- 🎯 **Altura da Linha de Leitura Customizável (PRD A4):** Defina onde os seus olhos encontram o texto (de 20% a 70% da altura da tela), auxiliado por uma linha guia discreta.
- ⚡ **Acompanhamento Natural e Fluido:** Algoritmo calibrado com resposta imediata e rolagem adaptativa que acompanha pausas, acelerações e ênfases.
- 👆 **Reposicionamento Tátil Direto:** Pular ou voltar trechos do texto ao tocar em qualquer palavra, retomando o acompanhamento de voz instantaneamente.
- 🎨 **Prévia Interativa em Tempo Real:** Ajuste tamanho de fonte (24 a 72 sp), espaçamento entre linhas, margens laterais e temas com prévia dinâmica na tela de Ajustes.
- 🏁 **Barra de Finalização Fixa:** Ao término do discurso, uma barra inferior ergonômica permite reiniciar a apresentação ou retornar ao editor com um único toque.

---

## 🛡️ Pilares Inegociáveis de Privacidade

O Tribuna foi arquitetado desde o primeiro dia com princípios de privacidade estrita:

1. **Sem Rede (100% Offline):** O manifesto Android declara estritamente a permissão `RECORD_AUDIO`. Não há permissão de `INTERNET`, `ACCESS_NETWORK_STATE` ou qualquer canal de comunicação externa. O aplicativo é matematicamente incapaz de enviar dados.
2. **O Texto Nunca é Gravado:** O conteúdo do discurso reside exclusivamente na memória RAM ativa da tela (`PrompterViewModel`). Ele nunca é salvo em disco, SQLite, DataStore, arquivos temporários ou relatórios de diagnóstico. Ao fechar o app, o texto deixa de existir.
3. **Áudio Nunca é Gravado:** O áudio capturado pelo microfone é convertido em fluxo PCM para o reconhecedor Vosk local e imediatamente descartado. Nenhuma gravação permanece na memória ou no armazenamento.
4. **Arquitetura Desacoplada:** O núcleo de alinhamento (`core/`) é Kotlin puro (sem dependências de `android.*`), auditável e testável de forma determinística na JVM.

---

## 📱 Telas Principais

- **Editor (`ui.editor`):** Área limpa para digitar ou colar o discurso. Suporta contagem instantânea de palavras, estimativa de tempo e detecção de dicas de palco (`//`).
- **Teleprompter (`ui.prompter`):** Ambiente imersivo de apresentação em tela cheia com temas de alto contraste, indicação de voz ativa, cronômetro, barra de progresso e rolagem suave adaptativa.
- **Ajustes (`ui.settings`):** Configuração completa da apresentação — seleção de temas visuais (Dark Teal Mint, Dark Amber, Light Clean, etc.), ajuste de sensibilidade do motor (Ágil, Normal, Conservador), tempo-alvo, altura da linha de leitura e espelhamento horizontal para uso em vidro refletor.

---

## 🛠️ Stack Tecnológica

- **Linguagem:** Kotlin 2.x com coroutines e `StateFlow`.
- **UI Toolkit:** Jetpack Compose com Material 3 (Compose BOM 2026.06.01).
- **Arquitetura:** MVI / MVVM unificado com separação estrita de camadas.
- **Motor de Voz:** `com.alphacephei:vosk-android` + JNA com modelo `vosk-model-small-pt-0.3` empacotado localmente em assets.
- **Persistência de Preferências:** Jetpack DataStore (Preferences) unicamente para configurações visuais e ajustes do usuário.
- **Alvo:** `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36`.
- **Qualidade e Estilo:** ktlint, Android Lint e verificação automatizada de manifesto mesclado (`verifyMergedManifest`).

---

## 📂 Estrutura do Repositório

```text
├── app/                  # Módulo Android: UI Compose, ViewModel, DataStore e Services
│   ├── src/main/assets/  # Modelo Vosk offline em pt-BR (vosk-model-small-pt-0.3)
│   └── src/test/         # Testes de ViewModel, formatação e ciclo de vida
├── core/                 # Núcleo de alinhamento em Kotlin puro (JVM, sem Android)
│   ├── align/            # Normalizer, Tokenizer, PartialReconciler e Aligner
│   └── test/             # Suíte completa de testes unitários do motor de alinhamento
├── docs/                 # Documentação de produto, técnica e conformidade
│   ├── PRD.md            # Product Requirements Document
│   ├── TRD.md            # Technical Requirements Document
│   └── CONFORMIDADE.md   # Políticas de privacidade, Play Store e licenças
├── etapa0/               # Laboratório experimental de calibração do motor de voz
└── gradle/               # Version Catalog (libs.versions.toml) e configurações de build
```

---

## 🚀 Como Compilar e Executar

### Pré-requisitos
- **JDK 21** (Eclipse Temurin ou OpenJDK) com `JAVA_HOME` configurado.
- **Android SDK** com Platform 36 e Build-Tools 36 configurados em `ANDROID_HOME`.
- Dispositivo Android (ou emulador) com Android 8.0+ (API 26+) conectado via USB com Depuração USB ativada.

### Comandos de Build e Testes

```bash
# Compilar APK de depuração
./gradlew assembleDebug

# Executar bateria de testes unitários (core e app)
./gradlew testDebugUnitTest

# Verificação de formatação e análise estática
./gradlew lint ktlintCheck

# Validar que nenhuma permissão indevida entrou no manifesto final
./gradlew verifyMergedManifest
```

### Instalação no Aparelho

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 Licença

Desenvolvido com foco em liberdade, oratória e privacidade.
Distribuído sob licença aberta compatível com o ecossistema Vosk e Apache 2.0.
