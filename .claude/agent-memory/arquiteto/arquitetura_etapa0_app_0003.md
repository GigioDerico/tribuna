---
name: arquitetura-etapa0-app-0003
description: Desenho do etapa0/:app decidido no PLAN da 0003 — VoskEngine puro sobre costuras, ReaderController puro dentro do ViewModel, exportação só com texto de teste, paralelismo só com worktree
metadata:
  type: project
---

Decidido em 2026-10-11 (`work/0003-etapa0-vosk/impl_plan.md`, A2–A13):

- `VoskEngine` é Kotlin puro sobre costuras (`AudioSource`, `Decoder`/`DecoderFactory`, `ModelLoader`, `CaptureDriver`); só `MicAudioSource.kt` e `VoskDecoders.kt` tocam Android/`org.vosk`. O teste chama o passo do laço à mão (`ManualCaptureDriver`), e o hook `onAccept` do `FakeDecoder` reproduz a corrida endSession/beginSession sem threads.
- Cada sessão é um objeto `Session(id, decoder)`; o laço lê a sessão atual só para rotear áudio; liberação do reconhecedor adiada se um bloco estiver em curso.
- Tela: `ReaderController` puro (fila única via `limitedParallelism(1)`, relógio injetado) dentro de um `ViewModel` fino; testes com `FakeSpeechEngine` + `StandardTestDispatcher`.
- `MainActivity` no pacote raiz (manifesto do portão 1 declara `.MainActivity`).
- Parâmetros visuais da etapa0 vêm dos padrões do PRD (A9 40%, A10 30%), nunca inventados.
- Dois agentes na mesma árvore quebram o build um do outro no passo "ver falhar": paralelo só com `git worktree` em branch próprio.

**Why:** spec 0003 D1 (lógica testável na JVM) e regra 7; a prova do plano (118 testes, 54 quebras Q2 vermelhas) validou o desenho.

**How to apply:** o app (`app/`) deve reaproveitar o mesmo padrão (motor puro + costuras; controlador puro sob o `PrompterViewModel`). Ver [[vosk-android-fatos-0003]], [[prova-do-plano-em-copia]].
