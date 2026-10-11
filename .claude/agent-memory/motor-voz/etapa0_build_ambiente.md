---
name: etapa0-build-ambiente
description: Comportamentos observados do build etapa0/ (Gradle 9.7.1, AGP 9.4.1, KGP 2.4.21, JDK 21, ktlint 1.8) e do shell nesta máquina, verificados em 2026-10-10 na tarefa 0001
metadata:
  type: project
---

Observado ao construir a 0001 (2026-10-10), com saída real:

- AGP 9.4.1 + KGP 2.4.21 + Gradle 9.7.1 funcionam juntos sem erro nem aviso, apesar de o AGP estar acima do intervalo testado pela JetBrains. O plano B (AGP 9.3.3) não foi necessário.
- O `~/.gradle/gradle.properties` do usuário força o JDK 17 via `org.gradle.java.home`; `etapa0/gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`) tem precedência e resolve. Nunca editar nem abrir o arquivo do usuário (tem segredos de outros projetos).
- O `ktlint_official` reprova `libs.versions.x.get().toInt()` em uma linha nos `build.gradle.kts` (`chain-method-continuation`) e comentário depois de `=` em corpo de expressão (`function-signature`). Escrever já no formato quebrado evita a rodada de `ktlintFormat`.
- O `./gradlew clean` da raiz não limpa o build incluído `build-logic`; para evidência sem cache, usar `--rerun-tasks`.
- A saída de console do Gradle só mostra `AssertionError at Arquivo.kt:N`; a comparação esperado/real está em `build/test-results/**/TEST-*.xml` (`<failure message=...>`). Anexar daí nas evidências da Q2.
- KDoc com `lib/<abi>/*.so` quebra a compilação (o `/*` abre comentário aninhado em Kotlin). Revisar blocos de código do plano que tenham `/*` dentro de comentário.
- O shell do Bash é zsh: `${PIPESTATUS[0]}` não existe (usar `${pipestatus[1]}`) e `${v^}` dá erro.

**Why:** a 0002 e a 0003 constroem neste mesmo projeto; isso evita repetir o diagnóstico.
**How to apply:** ao começar a 0002/0003, conferir se as versões do catalog ainda são essas antes de confiar nestas notas. Vosk ainda não foi exercitado (nada sobre JSON, custo de reconhecedor ou vocabulário restrito até aqui); ver [[vosk-comportamento]] quando existir.
