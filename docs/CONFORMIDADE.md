# Conformidade — Tribuna: Play Store, privacidade e licenças

O que precisa estar pronto, além do código, para publicar o Tribuna na Google Play. O app não coleta nem compartilha dados: a voz e o texto são processados só no aparelho e nada sai dele. Isso simplifica quase tudo, mas a política de privacidade continua obrigatória e a conta de desenvolvedor tem exigências próprias. Regras conferidas em 10/10/2026; revisar antes de publicar.

## Checklist de publicação

- [ ] Conta de desenvolvedor verificada e app registrado no Play Console (verificação obrigatória no Brasil desde 30/09/2026).
- [ ] Decidir conta pessoal ou de organização (define se há teste fechado obrigatório; ver Conta e publicação).
- [ ] Política de privacidade publicada numa URL pública e link no Play Console.
- [ ] Mesmo texto acessível dentro do app (Configurações → Sobre).
- [ ] Formulário de segurança de dados: nenhum dado coletado nem compartilhado.
- [ ] Questionário de classificação etária (IARC) respondido.
- [ ] Público-alvo: 13 anos ou mais.
- [ ] Declarações do conteúdo do app: sem anúncios, sem login, não é app de notícias, saúde ou governo.
- [ ] Avisos de licença de terceiros na tela Sobre.
- [ ] Manifesto final só com `RECORD_AUDIO`, sem `INTERNET` (verificado no build, conforme TRD).
- [ ] Bibliotecas nativas alinhadas a 16 KB (critério de saída da Etapa 0).

## Segurança de dados (Play Console)

O Google não exige declarar dados que o app acessa e processa só no aparelho, sem enviar para fora. Coletar significa transmitir para fora do aparelho; compartilhar significa transferir para terceiros. O Tribuna não faz nenhum dos dois, e não tem permissão de internet para isso.

| Pergunta do formulário | Resposta | Por quê |
| --- | --- | --- |
| O app coleta ou compartilha algum dos tipos de dados exigidos? | Não | Áudio e texto ficam só na memória do aparelho; o app não tem acesso à rede |
| Os dados são criptografados em trânsito? | Não se aplica | Nada é transmitido |
| Os usuários podem pedir a exclusão dos dados? | Não se aplica | Nada é guardado fora do aparelho; o texto some ao fechar o app |

O formulário e a política de privacidade precisam dizer a mesma coisa. Se no futuro entrar qualquer SDK (analytics, crash report, anúncios), o formulário muda antes da versão sair.

## Política de privacidade

Toda app na Play precisa de política de privacidade, mesmo sem coletar dados: um link no campo próprio do Play Console e o link ou o texto dentro do app. O microfone é tratado pela Play como dado pessoal e sensível, então a política precisa explicar o uso dele.

**Onde hospedar:** uma página pública e estável, como GitHub Pages ou o site da software house. A URL não pode exigir login.

**Texto proposto** (revisar nome, e-mail e data antes de publicar)

> **Política de privacidade do Tribuna**
>
> Última atualização: [data]
>
> O Tribuna é um teleprompter que acompanha a sua voz. Ele foi feito para funcionar sem internet e sem guardar o que você fala ou lê.
>
> **Microfone.** O app pede acesso ao microfone para reconhecer a sua fala e mover o texto enquanto você lê. O áudio é processado na hora, dentro do aparelho, pelo reconhecedor de voz embutido no app. O áudio não é gravado, não é guardado e não sai do aparelho.
>
> **Texto do discurso.** O texto que você cola ou digita fica só na memória do app enquanto ele está aberto. Ao fechar o app, o texto é apagado. Ele não é salvo em arquivo, não vai para backup e não é enviado a ninguém.
>
> **Preferências.** As suas configurações de aparência e de leitura (fonte, cores, velocidade e similares) ficam salvas no próprio aparelho para a próxima vez. Elas não contêm o texto do discurso e não saem do aparelho.
>
> **O que não fazemos.** O app não tem acesso à internet, não usa contas, não tem anúncios, não usa ferramentas de análise ou de relatório de erros e não compartilha dados com terceiros.
>
> **Seus direitos.** Como nenhum dado pessoal sai do seu aparelho, nós não temos dados seus para consultar, corrigir ou excluir. Para apagar as preferências, limpe os dados do app nas configurações do Android ou desinstale o app.
>
> **Crianças.** O app não é direcionado a crianças com menos de 13 anos.
>
> **Alterações.** Se esta política mudar, a nova versão será publicada nesta página com a data de atualização.
>
> **Contato.** Giorgio Derico — [e-mail de contato]

## Permissão do microfone

A tela 1a (primeiro uso) explica o motivo antes do pedido do sistema. Como os dados não saem do aparelho, o aviso não precisa de consentimento formal; ele serve para a pessoa entender o pedido e para a revisão da Play.

**Texto da tela 1a**

> Para o texto acompanhar a sua voz, o Tribuna precisa ouvir o microfone enquanto você lê. Tudo acontece no seu aparelho: o áudio não é gravado nem enviado a lugar nenhum.
>
> [Permitir microfone]   [Usar só a rolagem constante]

Negada a permissão, o app segue no modo Constante (tela 2c), conforme o PRD.

## LGPD

A LGPD regula o tratamento de dados pessoais. O Tribuna processa a voz e o texto só no aparelho da pessoa, sem que o desenvolvedor receba ou acesse esses dados, e não os armazena. Na prática, o desenvolvedor não trata dados pessoais dos usuários.

- A voz não é usada para identificar a pessoa; não há tratamento de dado biométrico.
- A política de privacidade acima cumpre o dever de transparência: diz o que o app acessa, para quê e que nada sai do aparelho.
- Ponto de atenção: qualquer mudança futura que envie dados para fora (sincronização, crash report, analytics) muda essa conclusão e exige revisão da política, do formulário de segurança de dados e da base legal.

Esta seção é uma análise prática, não parecer jurídico. Para publicação comercial em escala, vale a revisão de um advogado.

## Classificação etária e público-alvo

**Questionário IARC.** O app não tem violência, linguagem imprópria, conteúdo sexual, drogas, apostas, compras, interação entre usuários, compartilhamento de localização nem conteúdo gerado e publicado por usuários (o texto colado nunca sai do aparelho). A expectativa é a classificação mais baixa (Livre no Brasil), mas quem define é o questionário.

**Público-alvo.** Marcar 13 anos ou mais. Incluir faixas abaixo de 13 enquadra o app nas regras de Famílias da Play, com exigências extras sem benefício para o público do Tribuna.

**Outras declarações do conteúdo do app**

| Declaração | Resposta |
| --- | --- |
| Anúncios | Não contém |
| Acesso ao app | Todo o conteúdo disponível sem login |
| App de notícias | Não |
| App de saúde | Não |
| App governamental | Não |
| Recursos financeiros | Não |

## Licenças de terceiros

A tela Sobre lista os componentes de código aberto e as licenças, pelo plugin `oss-licenses` do Google ou por uma lista mantida à mão.

| Componente | Licença | Obrigação |
| --- | --- | --- |
| Vosk (biblioteca Android) | Apache 2.0 | Manter aviso de licença |
| Modelo `vosk-model-small-pt-0.3` | Apache 2.0 | Manter aviso de licença |
| JNA | Apache 2.0 ou LGPL 2.1 (à escolha) | Usar sob Apache 2.0 e manter o aviso |
| AndroidX, Jetpack Compose, Kotlin | Apache 2.0 | Manter aviso de licença |
| Atkinson Hyperlegible (se incluída) | SIL Open Font License 1.1 | Manter aviso; a fonte não pode ser vendida sozinha |

Não usar o modelo grande de português do Vosk (FalaBrazil): ele é GPLv3, incompatível com distribuir o app fechado.

Na ficha da loja, não citar nomes de apps concorrentes (como VoicePrompter): a Play proíbe usar marcas de terceiros para atrair buscas.

## Conta e publicação

**Verificação de desenvolvedor.** Desde 30/09/2026, no Brasil, apps em aparelhos Android certificados só instalam se vierem de um desenvolvedor verificado pelo Google. Na Play, isso é feito no Play Console: verificar a identidade da conta e registrar o app.

**Conta pessoal ou de organização.** Contas pessoais criadas depois de 13/11/2023 precisam fazer um teste fechado com pelo menos 12 testadores inscritos por 14 dias seguidos antes de pedir acesso à produção. Contas de organização não têm essa exigência, mas pedem verificação da empresa (com D-U-N-S), que costuma demorar mais.

| Opção | Vantagem | Custo |
| --- | --- | --- |
| Conta da software house (organização) | Sem teste fechado obrigatório; app sob a marca da empresa | Verificação da empresa e D-U-N-S |
| Conta pessoal existente, criada antes de 13/11/2023 | Sem teste fechado obrigatório | Nenhum |
| Conta pessoal nova | Abertura rápida | Teste fechado: 12+ testadores por 14 dias seguidos; recrutar com folga (por exemplo, 15 a 20 pessoas da congregação) |

Se for conta pessoal nova, o teste fechado pode coincidir com a calibração do app com discursos reais: os testadores da congregação são exatamente o público.

## Decisões em aberto

- [ ] Conta de publicação: organização (software house), pessoal existente ou pessoal nova.
- [ ] URL da política de privacidade e e-mail de contato.
- [ ] Incluir ou não a fonte Atkinson Hyperlegible.
