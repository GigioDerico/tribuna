---
name: build-toolchain-etapa0
description: Versões fixadas na 0001 (AGP 9.4.1, Gradle 9.7.1, KGP 2.4.21), o conflito da matriz KGP×AGP e a armadilha do JDK 17 no gradle.properties do usuário
metadata:
  type: project
---

Escolha da 0001 (2026-10-10): Gradle 9.7.1, AGP 9.4.1 (Kotlin embutido; runtime KGP 2.2.10, subido a 2.4.21 via `kotlin.jvm apply false` na raiz), KGP 2.4.21, coroutines 1.11.0, ktlint-gradle 14.2.0 + ktlint 1.8.0, build-tools 36.0.0. Plano B pré-aprovado: AGP 9.3.3.

**Why:** a matriz oficial do KGP 2.4.21 vai só até AGP 9.3.1 e Gradle 9.7.0; o TRD pede o AGP estável mais recente. Gradle 9.8.1 ficou de fora pelo teto do KGP.

**How to apply:** ao atualizar versões, conferir as duas tabelas (kotlinlang gradle-configure-project e developer.android.com gradle-plugin) antes; o AGP 9 proíbe aplicar `org.jetbrains.kotlin.android`.

Ambiente: `~/.gradle/gradle.properties` do usuário fixa `org.gradle.java.home` no JDK 17 (e guarda segredos de outros projetos; não abrir). Solução: `etapa0/gradle/gradle-daemon-jvm.properties` com `toolchainVersion=21`, que tem precedência. Sem `readelf`/NDK na máquina: checagem ELF de 16 KB feita em Kotlin no build-logic. Ver [[verificacoes-gradle-desenho]].
