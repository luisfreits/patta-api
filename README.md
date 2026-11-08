# Patta API
para rodar a api se usa o comando `.\mvnw.cmd spring-boot:run`
## Perfis de acesso

Toda conta criada por `POST /cadastro/solicitar` é um usuário comum. A conta
administradora deve ser autorizada por configuração de servidor: defina `ADMIN_UIDS`
com o UID da conta no Firebase Authentication. Profissionais são autorizados
separadamente por `PROFISSIONAL_UIDS`. Para várias contas, separe os UIDs por vírgula.
Sem essas configurações, os usuários terão papel `USER`.

`GET /auth/me` exige ID token válido e e-mail verificado e retorna o UID autenticado e
o papel atribuído pelo servidor (`ADMIN`, `PROFISSIONAL` ou `USER`). As permissões seguem
a hierarquia `ADMIN > PROFISSIONAL > USER`: administradores herdam os acessos profissional
e de usuário; profissionais herdam os acessos de usuário; usuários comuns não recebem
permissões profissionais ou administrativas. Operações administrativas verificam a
autorização no backend, independentemente do que a interface web mostrar. Nunca aceite
o papel enviado pelo cliente como fonte de autorização.

### Consultas do painel administrativo

Com um token de uma conta cujo UID está em `ADMIN_UIDS`, o painel pode consultar os
perfis cadastrados e os animais de todos os usuários:

| Método | Caminho | Descrição | Sucesso |
| --- | --- | --- | --- |
| `GET` | `/admin/usuarios` | Lista os perfis e seus papéis, sem dados de autenticação | `200 OK` |
| `PUT` | `/admin/usuarios/{usuarioUid}` | Atualiza nome, telefone e e-mail de qualquer perfil | `200 OK` |
| `GET` | `/admin/animais` | Lista animais e inclui `userId` para identificar o dono | `200 OK` |
| `GET` | `/admin/agendamentos` | Lista todos os agendamentos para o painel administrativo | `200 OK` |
| `POST` | `/admin/agendamentos` | Cria agendamento para um cliente e pet selecionados | `201 Created` |

As rotas `/admin/**` retornam `403` para usuários comuns. As listagens mostram o estado
atual dos dados; não constituem um histórico cronológico de alterações. Para criar um
agendamento, envie `usuarioId` e os campos de um agendamento dentro da propriedade
`agendamento`. O backend valida que o pet selecionado pertence ao cliente informado.

## Cadastro e verificação de e-mail

O cadastro é criado no Firebase Authentication com `emailVerified=false`. O perfil é
gravado em `users/{uid}` no Firestore com o mesmo UID, incluindo a referência da imagem
padrão de perfil `/user/img/novo_usuario_padrao.jpeg`. A API solicita o envio da
verificação ao Firebase Authentication; não é necessário SMTP.

1. Envie os dados cadastrais para `POST /cadastro/solicitar`. A resposta `202` confirma
   que o cadastro foi criado e orienta a pessoa a verificar a caixa de entrada e o spam.
2. A pessoa abre o link do template de verificação do Firebase para confirmar o endereço.
   O Firebase processa o código de ação diretamente.
3. O login continua permitido antes da verificação. A resposta de
   `POST /auth/login` inclui `emailVerified` e `role`, além dos tokens, evitando
   chamadas extras para consultar o estado do e-mail e o perfil. O endpoint
   `GET /auth/verificacao/status` continua disponível para consultas posteriores.
4. As demais rotas autenticadas retornam `403` enquanto a conta permanecer não verificada.
   O backend consulta o estado atual no Firebase Admin Authentication, sem confiar em
   um campo enviado pelo cliente ou em uma claim possivelmente antiga do token.
5. Para solicitar outro e-mail, use
   `POST /auth/verificacao/reenviar` com o ID token. Há um intervalo mínimo de 60 segundos
   por UID e a rota retorna `429` se a solicitação for repetida antes do prazo.

Se o envio inicial falhar depois da criação da conta, o cadastro e o perfil são mantidos
para evitar duplicar ou perder dados. A resposta informa que a pessoa deve entrar e usar
a rota autenticada de reenvio. Se a gravação do perfil falhar, a API tenta remover a conta
recém-criada do Firebase Authenticati
O frontend deste repositório não está presente. O cliente que consome esta API deve
apresentar a instrução de confirmação após o `202`, permitir login, usar o estado
`emailVerified` retornado no login e oferecer o reenvio. Se também usar o Firebase
Client SDK, deve recarregar o usuário após a volta do link (`reload(user)`) antes de
atualizar a interface.

## Configuração Firebase necessária

No Firebase Console, confirme manualmente:

- O provedor **E-mail/senha** está habilitado em Authentication > Sign-in method.
- O template de verificação de endereço está ativo e com texto/idioma adequados. A API
  envia `X-Firebase-Locale: pt-BR`; o projeto ainda deve ter o template de e-mail revisado.
- Os domínios do app e do link de ação estão em Authentication > Settings > Authorized
  domains. Configure o domínio de retorno em produção usando HTTPS.
- O handler de ação do Firebase está configurado para a experiência desejada. Se um
  endereço de retorno for usado, configure `FIREBASE_EMAIL_ACTION_CONTINUE_URL` com uma
  URL HTTPS autorizada. Sem ela, o Firebase usa seu handler padrão de ação.

Variáveis da aplicação:

- `FIREBASE_SERVICE_ACCOUNT_PATH`: caminho da conta de serviço usada somente pelo backend
  para Firebase Admin Authentication e Firestore.
- `FIREBASE_WEB_API_KEY`: Web API Key do projeto Firebase usada pelo backend para chamar
  os endpoints REST oficiais de login e envio de e-mail de ação. Ela não é uma credencial
  de conta de serviço; mantenha-a restrita no Google Cloud.
- `FIREBASE_EMAIL_ACTION_CONTINUE_URL` (opcional): URL autorizada para retorno após a ação.
- `CORS_ALLOWED_ORIGINS`: origens web autorizadas a chamar a API.

O cadastro e a verificação não requerem servidor SMTP nem credenciais de e-mail.

### Atualização do perfil e troca de e-mail

`PUT /users/me` atualiza nome, telefone e e-mail da conta autenticada. O corpo requer
`nome`, `telefone` e `email`; para trocar o endereço, também é obrigatório enviar
`senhaAtual`. A API reautentica a senha atual, atualiza o e-mail no Firebase
Authentication e no Firestore, marca o novo endereço como não verificado e envia um
link de verificação. A sessão precisa ser refeita depois de confirmar o endereço.
Se a atualização ou o envio do link falhar, a API tenta reverter as alterações.

O envio do link de troca de e-mail usa SMTP. Configure `EMAIL_HOST`, `EMAIL_PORT`,
`EMAIL_USER` e `EMAIL_PASSWORD` no backend; sem essas credenciais, a troca é revertida
e a API informa que não foi possível enviar a confirmação. Nome e telefone não exigem
SMTP nem a senha atual.

## API de animais

As rotas abaixo usam a coleção existente `animais` do Firestore. Cada documento tem
um `userId` definido pelo backend a partir do UID do ID token; o UID não é aceito no
payload. Todas as leituras e escritas individuais verificam o proprietário. Um ID que
não exista ou pertença a outra conta retorna `404` para não revelar sua existência.

### Autenticação

Envie o ID token Firebase em todas as rotas de animais:

```http
Authorization: Bearer <firebase-id-token>
Content-Type: application/json
```

O token é validado no Firebase Admin Authentication. Além disso, o backend confere o
estado atual `emailVerified` antes de permitir operações protegidas. Usuários não
verificados recebem `403`; cadastro, login, status de verificação e reenvio seguem as
regras documentadas acima.

### Endpoints

| Método | Caminho | Descrição | Sucesso |
| --- | --- | --- | --- |
| `POST` | `/animais` | Cadastra animal para a conta autenticada | `201 Created` |
| `GET` | `/animais` | Lista animais próprios; aceita os filtros opcionais abaixo | `200 OK` |
| `GET` | `/animais/{id}` | Consulta um animal próprio | `200 OK` |
| `PUT` | `/animais/{id}` | Substitui os campos editáveis do animal próprio | `200 OK` |
| `DELETE` | `/animais/{id}` | Exclui um animal próprio | `204 No Content` |

`GET /animais?nome=rex&especie=Cachorro` combina filtros com **AND**. `nome` procura
um trecho, sem diferenciar maiúsculas/minúsculas; `especie` compara o valor completo,
também sem diferenciar caixa. Valores vazios são ignorados; limites são 100 caracteres
para `nome` e 60 para `especie`. A API primeiro consulta o Firestore pelo `userId`
autenticado e só então aplica os filtros, portanto não varre a coleção global. Não é
necessário índice composto: a consulta de persistência usa somente igualdade em `userId`.

O corpo de `POST` e `PUT` contém os campos abaixo. Todos são obrigatórios; `idade` deve
ser maior ou igual a zero.

```json
{
  "nome": "Rex",
  "sexo": "Macho",
  "idade": 3,
  "animal": "Cachorro",
  "castrado": true
}
```

`PUT` substitui todos esses campos. `id` e `userId` não fazem parte do contrato de
entrada; qualquer `userId` não pode mudar o proprietário, que é obtido da autenticação.
As respostas contêm `id`, `nome`, `sexo`, `idade`, `animal` e `castrado`; não expõem
`userId`.

Exemplo de cadastro:

```http
POST /animais
Authorization: Bearer <firebase-id-token>
Content-Type: application/json
```

Resposta `201`:

```json
{
  "id": "id-gerado-pelo-firestore",
  "nome": "Rex",
  "sexo": "Macho",
  "idade": 3,
  "animal": "Cachorro",
  "castrado": true
}
```

### Respostas de erro

- `400 Bad Request`: JSON inválido, campos obrigatórios ausentes/inválidos ou filtro longo demais.
- `401 Unauthorized`: token ausente, inválido, expirado ou conta inexistente/desativada.
- `403 Forbidden`: token válido, mas e-mail ainda não verificado.
- `404 Not Found`: animal inexistente ou que pertence a outro usuário.
- `500 Internal Server Error`: falha inesperada, inclusive falhas de persistência não mapeadas.

Erros de aplicação usam o JSON `{ "status": 404, "mensagem": "...", "timestamp": "..." }`;
nenhuma resposta de erro inclui stack trace, token ou credenciais.

## API de agendamentos

Os endpoints de agendamento exigem um Firebase ID token válido e e-mail verificado,
seguindo as regras gerais de [autenticação](#autenticação) usadas pelas outras rotas
protegidas. A coleção Firestore é `agendamentos`. A API não aceita do cliente `usuarioId`,
`status`, `id` ou datas de auditoria: o proprietário vem do UID autenticado, o estado
inicial é `PENDENTE` e os timestamps são preenchidos pelo servidor.

### Endpoints

| Método | Caminho | Descrição | Sucesso |
| --- | --- | --- | --- |
| `POST` | `/agendamentos` | Cria uma solicitação para animal próprio | `201 Created` |
| `GET` | `/agendamentos` | Lista os agendamentos do usuário; UID administrativo lista todos | `200 OK` |
| `GET` | `/admin/agendamentos` | Lista todos os agendamentos; exclusivo para administradores | `200 OK` |
| `GET` | `/agendamentos/{id}` | Consulta um agendamento próprio; UID administrativo pode consultar qualquer um | `200 OK` |
| `POST` | `/agendamentos/{id}/cancelar` | Tutor cancela o próprio agendamento antes do check-in | `200 OK` |
| `PATCH` | `/agendamentos/{id}/status` | Profissional ou administrador aprova/rejeita agendamento de usuário comum; admin também gerencia outros papéis | `200 OK` |
| `PATCH` | `/agendamentos/{id}/descricao` | Tutor corrige a descrição do problema do próprio agendamento | `200 OK` |
| `POST` | `/admin/agendamentos/{id}/check-in` | Registra a entrada/check-in de um agendamento aprovado | `200 OK` |
| `POST` | `/admin/agendamentos/{id}/check-out` | Registra a saída/check-out de uma hospedagem | `200 OK` |
| `GET` | `/profissional/agendamentos` | Profissionais veem dados mínimos dos agendamentos de usuários comuns; administradores veem os de todos os papéis | `200 OK` |
| `POST` | `/profissional/agendamentos/{id}/novo` | Cria agendamento para usuário comum com base em um agendamento de referência | `201 Created` |
| `POST` | `/profissional/agendamentos/{id}/check-in` | Profissional ou administrador registra check-in | `200 OK` |
| `POST` | `/profissional/agendamentos/{id}/check-out` | Profissional ou administrador registra check-out de hospedagem | `200 OK` |

O POST recebe datas ISO (`yyyy-MM-dd`) e horários (`HH:mm`). Os serviços aceitos são
`consulta`, `vacina` e `hospedagem`, em minúsculas. `animalId` precisa identificar um
animal pertencente ao usuário autenticado. `observacoes` é opcional, com até 1000
caracteres; data, horário e animal são obrigatórios.
O horário precisa estar configurado e disponível. Para `hospedagem`, informe
`tempoHospedagemDias` (mínimo de um dia); a saída prevista é calculada a partir da data
de entrada. Registros antigos que contenham `dataSaidaPrevista` continuam sendo lidos.
Não é permitido criar mais de um agendamento ativo para o mesmo pet, serviço, data e
horário. Agendamentos rejeitados ou cancelados não impedem uma nova solicitação.
Agendamentos em outra data ou horário continuam permitidos; o sistema não mantém
informações de cobrança/pagamento.

```http
POST /agendamentos
Authorization: Bearer SEU_FIREBASE_ID_TOKEN
Content-Type: application/json
```

```json
{
  "animalId": "id-do-animal",
  "servico": "consulta",
  "data": "2027-05-18",
  "horario": "10:30",
  "observacoes": "Consulta de rotina",
  "dataSaidaPrevista": null
}
```

Resposta `201` (datas de auditoria são exemplos):

```json
{
  "id": "id-gerado-pelo-firestore",
  "usuarioId": "uid-do-firebase",
  "animalId": "id-do-animal",
  "servico": "consulta",
  "data": "2027-05-18",
  "horario": "10:30:00",
  "dataSaidaPrevista": null,
  "duracaoMinutos": 30,
  "status": "PENDENTE",
  "observacoes": "Consulta de rotina",
  "checkInEm": null,
  "checkInPor": null,
  "checkOutEm": null,
  "checkOutPor": null,
  "criadoEm": "2026-10-25T18:44:39Z",
  "atualizadoEm": "2026-10-25T18:44:39Z"
}
```

`GET /agendamentos` retorna uma lista com essa mesma estrutura, ordenada por data,
horário e criação. Para clientes comuns, inclui somente seus agendamentos; para UIDs
configurados como administradores, inclui todos. `GET /agendamentos/{id}` retorna um
objeto; para clientes comuns, um ID de outro usuário e um ID inexistente retornam ambos `404`.

Para atualizar o status, o administrador autenticado envia somente:

```http
PATCH /agendamentos/id-do-agendamento/status
Authorization: Bearer SEU_FIREBASE_ID_TOKEN
Content-Type: application/json
```

```json
{ "status": "APROVADO" }
```

Somente `PENDENTE -> APROVADO` e `PENDENTE -> REJEITADO` são permitidos. Usuários
sem privilégio recebem `403`; transições inválidas recebem `409`. O tutor pode cancelar
somente o próprio agendamento, antes do check-in. O cancelamento não depende de a clínica
estar aberta; agendamentos já cancelados/rejeitados ou com check-in não podem ser cancelados.

O tutor pode corrigir `observacoes` com `PATCH /agendamentos/{id}/descricao` até o
check-in, desde que o agendamento não tenha sido rejeitado/cancelado. O check-in é
registrado por administrador ou profissional autorizado e exige status `APROVADO`; a
resposta inclui o instante real em `checkInEm` e o UID do responsável em `checkInPor`.
O check-out só existe para hospedagem, exige check-in e grava `checkOutEm` e
`checkOutPor`.

Erros usam o envelope geral `{ "status": 400, "mensagem": "...", "timestamp": "..." }`.
Além dos erros usuais de token (`401`) e e-mail não verificado (`403`), os endpoints
retornam `400` para payload/campos inválidos, `403` para falta de privilégio
administrativo, `404` para agendamento ausente ou alheio ao cliente e `409` para
transições de status inválidas. Falhas inesperadas de persistência retornam `500`.

### Autorização administrativa

Configure no servidor `ADMIN_UIDS` e `PROFISSIONAL_UIDS` como listas separadas por
vírgulas de UIDs Firebase confiáveis. As listas são independentes; se um UID estiver
nas duas, prevalece `ADMIN`. Administradores podem acessar as rotas administrativas e profissionais;
profissionais veem na agenda apenas nome do tutor, nome do pet, data/horário e se é
hospedagem, além das ações permitidas para o atendimento. Usuários comuns não acessam
rotas administrativas nem profissionais. O backend compara as listas
com o UID extraído do ID token validado. O frontend não concede privilégios.

## Disponibilidade, check-in e hospedagem

O administrador configura horários semanais e exceções por data para `consulta` e
`vacina`. O dia usa o nome em inglês e caixa alta (`MONDAY` a `SUNDAY`). Durações são
em minutos; cada intervalo gera slots consecutivos a partir do horário de início.
Intervalos no mesmo dia não podem se sobrepor. A capacidade vale para cada slot e a
API confere a reserva novamente em transação no Firestore para não aceitar duas
solicitações concorrentes acima da capacidade. A hospedagem não usa grade por horário:
usa os dias de hospedagem, duração em dias e a capacidade global da clínica.

| Método | Caminho | Descrição |
| --- | --- | --- |
| `PUT` | `/admin/disponibilidade/{servico}/{diaSemana}` | Cria/substitui os horários, duração e capacidade |
| `DELETE` | `/admin/disponibilidade/{servico}/{diaSemana}` | Fecha o serviço nesse dia |
| `PUT` | `/admin/disponibilidade/{servico}/periodo?inicio=2027-05-18&fim=2027-05-24` | Cria exceções de disponibilidade para cada data do período (máximo de 31 dias) |
| `DELETE` | `/admin/disponibilidade/{servico}/periodo?inicio=2027-05-18&fim=2027-05-24` | Remove as exceções de disponibilidade das datas do período |
| `GET` | `/profissional/disponibilidade/padrao` | Consulta o padrão compartilhado de consulta e vacina |
| `PUT` | `/profissional/disponibilidade/padrao` | Define os intervalos, duração e capacidade padrão compartilhados |
| `DELETE` | `/profissional/disponibilidade/padrao` | Remove o padrão compartilhado |
| `GET` | `/disponibilidade?servico=consulta&data=2027-05-18` | Lista slots e vagas disponíveis |

As configurações semanais e por data prevalecem sobre o padrão compartilhado, nessa
ordem. Sem regra específica, o padrão fornece intervalos, `duracaoMinutos` e `capacidade`
para consulta e vacina. Sem qualquer configuração aplicável, a API não inventa horários
e retorna `404`. Ao remover uma exceção por data, a regra semanal volta a ser usada.
A aplicação em período não altera a configuração semanal; o intervalo inclui as datas
inicial e final.

Exemplo de configuração:

```json
{
  "intervalos": [
    { "inicio": "09:00", "fim": "12:00" },
    { "inicio": "13:00", "fim": "17:00" }
  ],
  "duracaoMinutos": 30,
  "capacidade": 1,
  "continuarHorarioPadrao": false
}
```

O padrão usa o mesmo corpo, com exatamente dois intervalos que, em ordem cronológica,
têm uma pausa estritamente maior que zero entre eles. O documento é salvo em
`horariosFuncionamento/padrao_TODOS`; duração e capacidade servem como valores padrão
compartilhados por consulta e vacina. Apenas profissionais e administradores podem
consultar, alterar ou remover esse padrão.

Uma regra semanal ou exceção por data pode definir `continuarHorarioPadrao: true`.
Nesse caso, seus intervalos são unidos aos intervalos atuais do padrão em tempo de
consulta, ordenando-os e juntando intervalos sobrepostos ou adjacentes. Duração e
capacidade permanecem as da regra específica. Com o valor omitido ou `false`, a regra
específica substitui o padrão. Em uma configuração em período com continuação ativada,
somente a primeira data recebe essa opção; os dias seguintes não a herdam.

`GET /disponibilidade` exige autenticação e retorna `duracaoMinutos` e uma lista com
horário, capacidade e vagas disponíveis. Para consulta e vacina, solicitações
pendentes/aprovadas ocupam a capacidade quando seus períodos se sobrepõem. Para
hospedagem, informe `tempoHospedagemDias`; a capacidade é ocupada para cada dia da
estadia, e a saída real é registrada no check-out.

### Estado da clínica e capacidade de hospedagem

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/profissional/clinica/status` | Consulta se a clínica está aberta |
| `POST` | `/profissional/clinica/fechar` | Fecha a clínica |
| `POST` | `/profissional/clinica/abrir` | Reabre a clínica |
| `GET` | `/profissional/clinica/capacidade-hospedagem` | Consulta a capacidade máxima global |
| `PUT` | `/profissional/clinica/capacidade-hospedagem` | Atualiza a capacidade máxima global |

A consulta de status exige autenticação; abrir/fechar a clínica e consultar/alterar a
capacidade exigem profissional ou administrador. Se não existir configuração de estado,
a clínica é considerada aberta. Quando fechada, novas solicitações
de agendamento de clientes recebem `503`; a equipe ainda pode atualizar o estado e
agendamentos existentes continuam podendo ser cancelados. Para hospedagem, a capacidade
é compartilhada por todos os dias da estadia e as solicitações concorrentes são
serializadas para não exceder o limite configurado.

## Carteira vacinal e histórico clínico

Os registros são salvos em coleções Firestore separadas. O tutor autenticado pode
consultar somente os registros dos próprios animais; administradores e profissionais
autorizados podem consultar e registrar dados clínicos ou de vacinação. Os campos de
autoria e timestamps são preenchidos pelo servidor.

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/animais/{animalId}/vacinas` | Consulta a carteira vacinal do próprio animal |
| `GET` | `/animais/{animalId}/historico-clinico` | Consulta o histórico clínico do próprio animal |
| `POST` | `/admin/animais/{animalId}/vacinas` | Registra vacina aplicada |
| `GET` | `/admin/animais/{animalId}/vacinas` | Consulta a carteira para atendimento administrativo |
| `POST` | `/admin/animais/{animalId}/historico-clinico` | Registra atendimento, queixa, diagnóstico e tratamento |
| `GET` | `/admin/animais/{animalId}/historico-clinico` | Consulta o histórico para atendimento administrativo |
| `POST` | `/profissional/agendamentos/{id}/historico-clinico` | Registra atendimento no agendamento em andamento |
| `POST` | `/profissional/agendamentos/{id}/vacinas` | Registra vacina no agendamento em andamento |

A vacina registra nome, data de aplicação, próxima dose opcional, lote, profissional
e observações. O histórico clínico registra data/hora, profissional, queixa,
diagnóstico, tratamento e observações. Não há edição/exclusão de registros clínicos ou
vacinais nesta versão; correções devem ser feitas administrativamente em uma etapa
futura com trilha de auditoria. Ao excluir um animal ou conta, os registros clínicos e
vacinais associados também são removidos.

A listagem profissional retorna somente o id técnico do agendamento, nome do tutor,
nome do pet, data, horário, indicador de hospedagem e indicadores de ações disponíveis.
Profissionais recebem apenas agendamentos de usuários comuns; administradores podem
consultar agendamentos de todos os papéis.
Ela não retorna IDs de usuário/pet, observações, estado clínico ou dados completos do
agendamento. A equipe só pode registrar vacina ou histórico clínico durante um
atendimento com check-in realizado.

O inventário real das novas coleções Firestore não foi lido nesta implementação. A API
cria novos documentos com IDs gerados e não migra nem sobrescreve documentos
preexistentes. Ao excluir uma conta em `DELETE /users/me`, seus agendamentos, animais,
registros clínicos e vacinas também são excluídos, junto com o perfil.

### Execução local e CORS

Configure `FIREBASE_SERVICE_ACCOUNT_PATH`, `FIREBASE_WEB_API_KEY` e as configurações
Firebase descritas acima. Inicie a API com `.\mvnw.cmd spring-boot:run` na raiz do
backend. A configuração CORS aceita as origens locais `http://localhost` e
`http://127.0.0.1` em qualquer porta (incluindo Live Server); origens adicionais devem
ser listadas em `CORS_ALLOWED_ORIGINS` separadas por vírgulas. Em produção, configure
somente as origens HTTPS reais do frontend, sem liberar `*`. Para o frontend publicado
em `https://frontend-six-virid-35.vercel.app`, inclua essa origem em
`CORS_ALLOWED_ORIGINS` no Render, preservando as outras origens necessárias. Quando
essa variável está definida no Render, ela substitui a lista padrão desta aplicação.
