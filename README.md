# Tribuna

App Android de teleprompter que acompanha a voz do orador. 100% offline, sem salvar o texto do discurso e sem gravar áudio. Kotlin + Jetpack Compose, reconhecimento de voz com Vosk.

## Documentos

- `docs/PRD.md`: o que o app faz (requisitos, telas, gestos, critérios de aceite).
- `docs/TRD.md`: como construir (contratos, máquina de estados, motor de voz, metas, testes). Em divergência técnica, o TRD prevalece.
- `docs/TESTES-ETAPA0.md` e `docs/TESTES-ETAPA0-L30.md`: textos de teste e protocolo de medição da Etapa 0.
- `docs/CONFORMIDADE.md`: Play Store, privacidade, licenças.
- `docs/Tribuna - telas e fluxos v2.html` e `docs/Tribuna Design System/`: telas, fluxos e design system.
- `CLAUDE.md`: regras inegociáveis e de processo para quem trabalha no repositório (pessoas e agentes).

## Processo

O trabalho segue um ciclo com agentes especializados (`.claude/agents/`): intake, plano, construção, revisão, QA e publicação, com aprovação humana nos portões. Cada tarefa tem sua pasta em `work/`, criada a partir de `templates/`. Detalhes em `PROCESS.md`; perguntas de revisão em `REVIEW-QUESTIONS.md`; problemas abertos em `registers/opr_register.md`.

## Pré-requisitos

- JDK 21 (Temurin) com `JAVA_HOME` configurado.
- Android SDK com platform 36, build-tools 36 e platform-tools; `ANDROID_HOME` configurado.
- `git` e `git-lfs` (gravações `.wav` e o modelo do Vosk vão pelo LFS: `git lfs install`).
- Python 3 para as ferramentas da Etapa 0 (`etapa0/ferramentas/`).
- Aparelho Android (celular e tablet) com depuração USB, para os testes instrumentados e as gravações.
- Claude Code, para o fluxo agêntico.

## Comandos

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lint ktlintCheck
./gradlew verifyMergedManifest
```

Lista completa no `CLAUDE.md`.
