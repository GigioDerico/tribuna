# Processo de desenvolvimento do Tribuna

Como o trabalho anda, quem faz o quê e onde cada coisa fica. As regras curtas estão no `CLAUDE.md`; aqui está o detalhe.

## Princípios

- **O estado vive em arquivo.** Cada tarefa tem uma pasta com seus artefatos. Nenhum agente depende do que outro "lembra" de uma conversa.
- **Quem constrói não revisa.** O código é escrito por `motor-voz` ou `interface` e revisado por `revisor` em contexto próprio. O `qa-publicacao` testa no aparelho sem ter escrito nem revisado.
- **Determinístico fora do prompt.** Compilação, testes, manifesto, alinhamento de 16 KB e métricas da Etapa 0 são tarefas do Gradle ou scripts. O agente roda e lê o resultado; não opina sobre o que um comando pode responder.
- **Contratos primeiro.** Os contratos do TRD são a fronteira entre motor de voz e interface. Fechado o contrato, os dois constroem em paralelo.

## Papéis

| Agente (`.claude/agents/`) | Papel | Escreve código do app? | Artefato que entrega |
|---|---|---|---|
| `analista` | Transforma pedido em requisito; guarda o PRD | Não | `spec.md` |
| `arquiteto` | Dono do TRD e dos contratos; planeja; primeira camada de privacidade | Não | `impl_plan.md`, `change_impact_analysis.md` |
| `motor-voz` | Implementa `speech/`, `align/`, `data/` e ferramentas da Etapa 0 | Sim | código + testes |
| `interface` | Implementa `ui/`, `viewmodel/`, `input/`, navegação e tema | Sim | código + testes |
| `revisor` | Revisa o diff em contexto limpo; segunda camada de privacidade | Não | `code_review.md` |
| `qa-publicacao` | Testa no aparelho, roda métricas, prepara a publicação | Não (só scripts de teste e de build) | `test_result.md`, `release_check.md` |

A sessão principal do Claude Code é o **orquestrador**: abre a pasta da tarefa, chama os agentes na ordem, confere os portões automáticos e leva ao humano só o que cai num portão humano. Ela não implementa nem revisa.

## Onde ficam as coisas

```
work/
  0001-etapa0-motor-voz/
    brainstorm.md               (superpowers:brainstorming; opcional, ver Intake)
    spec.md
    change_impact_analysis.md   (só quando a tarefa mexe em contrato)
    impl_plan.md
    code_review.md
    test_result.md
    release_check.md            (só em tarefa de publicação)
registers/
  opr_register.md               problemas abertos que não cabem na tarefa atual
templates/                      modelos de cada artefato
.claude/agent-memory/<agente>/  memória de cada agente (MEMORY.md), criada pelo Claude Code
```

Numeração das tarefas: quatro dígitos sequenciais e um nome curto. O orquestrador cria a pasta copiando os modelos de `templates/`.

## Autonomia e portões humanos

Modelos: `analista`, `arquiteto` e `revisor` rodam em Opus (decidem e julgam); `motor-voz`, `interface` e `qa-publicacao` rodam em Sonnet. Em tarefa simples de execução (renomear, ajuste mecânico, script curto, tarefa só de texto), o orquestrador pode chamar o agente de construção com `model: haiku`; na dúvida, Sonnet. Código que toca `align/`, a máquina de estados ou o ciclo de vida de sessão não usa Haiku.

Spec e plano não têm aprovação humana. Os agentes decidem, registram as decisões no próprio artefato (o que estava em aberto, a escolha e o porquê) e o orquestrador confere um **portão automático**, só com checklist, sem opinar:

- **Spec:** critérios de aceite verificáveis por comando ou número; "Fora do escopo" preenchido; nenhum critério depende de decisão pendente; nada cai num portão humano.
- **Plano:** cada critério do spec tem passo e comando de verificação; testes vêm antes do código; seção de privacidade preenchida; nenhuma assinatura de contrato muda; nenhuma permissão nova no manifesto.

Passou: o orquestrador registra "conferida pelo orquestrador em <data>" no cabeçalho e segue. Falhou: devolve ao autor com o item que falhou (conta como falha na escada).

**Portões humanos que continuam** (o que é difícil de desfazer ou muda o produto):

1. Mudança de contrato do TRD, de chave do DataStore, do manifesto ou de regra inegociável (CIA e aprovação humana, CLAUDE.md regra 5 e regra de processo 3).
2. Mudança de requisito ou escopo do PRD.
3. Troca do motor de voz e aprovação da arquitetura ao fim da Etapa 0.
4. Autorização de publicação e envio à Play.
5. Impasse no nível 3 da escada, ou ação irreversível (apagar dados ou repositório, push forçado).
6. Instalação ou gasto fora do ambiente do projeto (instalar software, criar conta, serviço pago).

Em dúvida se algo cai num portão humano, cai.

## O ciclo

```
[0. CIA]    arquiteto      só se a tarefa toca contrato do TRD, schema do DataStore ou regra inegociável
   │
[1. INTAKE] analista ───► brainstorm.md (superpowers) ───► spec.md ───► PORTÃO AUTOMÁTICO
   │
[2. PLAN]   arquiteto ──► impl_plan.md (writing-plans + revisão de privacidade) ───► PORTÃO AUTOMÁTICO
   │
[3. BUILD]  motor-voz e/ou interface ──► código + testes; tudo verde localmente
   │
[4. REVIEW] revisor (contexto limpo) ──► code_review.md: APROVADO ou REPROVADO
   │            REPROVADO volta ao 3 (conta como falha na escada)
[5. QA]     qa-publicacao ──► test_result.md com evidência (logs, CSV, capturas)
   │            falha volta ao 3
[6. AUTORIZAÇÃO]  humano autoriza por escrito subir à Play (só em publicação)
   │
[7. PUBLICAÇÃO]   qa-publicacao ──► release_check.md; o humano envia o AAB
```

**Atalho para mudança trivial** (texto de interface, cor, correção de uma linha com teste): pula 1 e 2; o orquestrador descreve a mudança em `spec.md` com três linhas, e 4 continua obrigatório.

### 0. Análise de impacto (CIA)

Obrigatória antes de mudar qualquer um destes: `SpeechEngine`/`SpeechEvent`/`SessionId`, `Token`, `Aligner`/`AlignState`, `PrompterUiState`/`ReadingPhase`, as chaves do DataStore, o manifesto ou qualquer regra inegociável do `CLAUDE.md`. O `arquiteto` lista todos os usos no repositório (busca real, não memória), o que quebra e quais testes cobrem, e propõe a mudança no TRD. Contrato só muda com aprovação humana e com o TRD atualizado no mesmo commit.

### 1. Intake

O `analista` primeiro roda a skill `superpowers:brainstorming` e grava `brainstorm.md` na pasta da tarefa (não em `docs/superpowers/`). O analista responde as perguntas da skill sozinho e registra pergunta, alternativas, escolha e porquê na seção "Decisões" do `brainstorm.md`; só uma pergunta que caia num portão humano vai ao orquestrador. O brainstorm é dispensado quando o pedido já está coberto pelo PRD e pelo TRD, ou é mudança trivial. Ele não substitui o spec.

Depois, o `analista` escreve o `spec.md`: o que, por quê, requisitos do PRD atendidos, critérios de aceite verificáveis e **fora do escopo** obrigatório. Se o pedido contradiz o PRD, ele propõe a mudança no PRD em vez de inventar. Para itens que já estão no PRD e no TRD (a maior parte da v1), o `spec.md` é curto e aponta para eles.

### 2. Plan

O `arquiteto` escreve o `impl_plan.md` com a skill `superpowers:writing-plans`, no formato de `templates/impl_plan.md` e na pasta da tarefa. O passo final da skill (executar o plano) não se aplica: a execução é dos agentes de construção, depois do portão automático. O plano traz: arquivos que mudam, ordem (contratos e `align/` → `speech/` → ViewModel → telas), testes a escrever primeiro, como verificar, riscos. Inclui a seção de privacidade: onde o texto do discurso passa, se algum caminho novo pode persistir texto ou áudio, se alguma dependência nova traz permissão. Consulta `REVIEW-QUESTIONS.md` (Q1, Q4, Q5, Q6) e o `opr_register.md` da área.

### 3. Build

O agente de construção segue o plano. Teste antes do código em `align/` e `ReadingStateMachine`. Ao terminar: `testDebugUnitTest`, `lint`, `ktlintCheck` e `verifyMergedManifest` verdes, e um resumo no fim do `impl_plan.md` (o que foi feito, desvios do plano e por quê). Nunca declara o próprio código revisado.

Quando motor de voz e interface trabalham na mesma etapa, cada um tem sua tarefa e sua pasta; os dois dependem só do contrato, nunca do código um do outro. A interface usa `FakeSpeechEngine` até o motor estar pronto.

### 4. Review

O `revisor` lê o diff, o `spec.md`, o `impl_plan.md` e as seções do TRD citadas, roda os testes e escreve `code_review.md` com veredito único: **APROVADO** ou **REPROVADO**. Achado de privacidade ou de regra inegociável reprova sempre. Achado menor vira item com dono no próprio relatório ou no `opr_register.md`.

### 5. QA

O `qa-publicacao` roda os testes instrumentados e de Compose UI no aparelho, e, na Etapa 0, as gravações e os scripts de métricas (`docs/TESTES-ETAPA0.md`). Toda afirmação tem evidência anexada à pasta: saída do Gradle, CSV de trajetória, relatório de métricas, capturas. "Passou" sem evidência não conta. Aplica Q2: confirma que pelo menos um teste novo fica vermelho contra uma quebra proposital.

### 6 e 7. Autorização e publicação

Só em tarefa de publicação. O humano autoriza por escrito, na pasta da tarefa. O `qa-publicacao` gera o AAB de release, roda `verify16kAlignment` e `verifyMergedManifest`, confere o checklist do `docs/CONFORMIDADE.md` e escreve `release_check.md`. O envio à Play é feito pelo humano.

## Etapa 0

A Etapa 0 usa o mesmo ciclo, com estas tarefas iniciais:

1. `0001-etapa0-contratos` (arquiteto + motor-voz): contratos do TRD em Kotlin, `FakeSpeechEngine`, projeto `etapa0/` com build e tarefas de verificação.
2. `0002-etapa0-align` (motor-voz): `Normalizer`, `Tokenizer`, `PartialReconciler`, `Aligner` com as suítes do TRD.
3. `0003-etapa0-vosk` (motor-voz): `VoskEngine`, `ModelInstaller`, tela mínima e painel de depuração.
4. `0004-etapa0-ferramentas` (motor-voz): `timestamps.py`, `alinhar.py`, `metricas.py`.
5. `0005-etapa0-medicao` (qa-publicacao): gravações, trajetórias e relatório contra os critérios de saída.

O resultado da 0005 volta ao humano: aprova a arquitetura (segue para o app) ou decide trocar o motor atrás do `SpeechEngine`.

## Escada de escalonamento

| Nível | Quem | Quando |
|---|---|---|
| 0 | Construção ↔ revisor/QA | Ajustes normais. Teto: **3 falhas seguidas no mesmo problema** (build, teste ou reprovação pelo mesmo motivo). |
| 1 | `arquiteto` | Teto do nível 0 atingido, ou o problema é de desenho (contrato, máquina de estados, comportamento do Vosk diferente do TRD). |
| 2 | `analista` | A solução exige mudar requisito ou escopo do PRD. |
| 3 | Humano | Impasse, mudança de contrato, troca de motor, publicação, qualquer coisa difícil de desfazer. |

Ao escalar, o agente escreve na pasta da tarefa o que tentou, o que falhou (com saída real) e a pergunta exata.

## Camadas de verificação

Sem servidor nem contas, o risco do Tribuna é privacidade e correção do acompanhamento, não ataque remoto. As três camadas são:

| Camada | Quem | Quando | O quê |
|---|---|---|---|
| Desenho | `arquiteto` | PLAN | Caminho do texto e do áudio, permissões de dependências novas, contratos, invariantes |
| Código | `revisor` | REVIEW | Regras inegociáveis no diff, invariantes do TRD, testes que de fato testam |
| Executável | `qa-publicacao` + Gradle | QA e publicação | Manifesto mesclado, 16 KB, backup desligado, nada do texto em logs no release, métricas no aparelho |

## Fim de sessão

Toda sessão de agente termina dizendo: o que foi feito, onde está (arquivo, commit), o que ficou pendente e se há decisão humana esperando. Achado que não cabe na tarefa vai para `registers/opr_register.md` com dono; quem encontrou é dono até a transferência confirmada, e o dono padrão é o `arquiteto`.
