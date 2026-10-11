# Registro de problemas abertos

Achados que não cabem na tarefa em que apareceram. Quem encontrou é dono até a transferência ser confirmada aqui; o dono padrão é o `arquiteto`. Nenhuma linha some: ao resolver, marque como fechada e cite o commit ou a tarefa.

| ID | Data | Achado | Severidade | Origem (tarefa) | Dono | Situação | Resolução |
|---|---|---|---|---|---|---|---|
| OPR-001 | 2026-10-10 | Com `androidx.core` 1.9+ (vem com Compose), o manifesto mesclado ganha `uses-permission` `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. O `verifyMergedManifest` (lista: só `RECORD_AUDIO`, CLAUDE.md regra 1) vai reprovar. Decidir com o humano: remover com `tools:node="remove"` (e não usar `ContextCompat.registerReceiver` com `RECEIVER_NOT_EXPORTED`) ou ajustar a regra; não afrouxar a verificação. | Média | 0001-etapa0-contratos (PLAN) | arquiteto | Aberto; resolver no PLAN da 0003 | |

