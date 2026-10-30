# Patta API
para rodar a api se usa o comando `.\mvnw.cmd spring-boot:run`
## Perfis de acesso

Toda conta criada por `POST /cadastro/solicitar` é um usuário comum. A conta
administradora deve ser autorizada por configuração de servidor: defina `ADMIN_UIDS`
com o UID da conta no Firebase Authentication. Para várias contas, separe os UIDs por
vírgula. Se a variável estiver vazia, todos os usuários terão papel `USER`.

`GET /auth/me` exige ID token válido e e-mail verificado e retorna o UID autenticado e
o papel atribuído pelo servidor (`ADMIN` ou `USER`). O papel também é aplicado às
autoridades do Spring Security; operações administrativas verificam a autorização no
backend, independentemente do que a interface web mostrar. Nunca aceite o papel enviado
pelo cliente como fonte de autorização.

## Cadastro e verificação de e-mail

O cadastro é criado no Firebase Authentication com `emailVerified=false`. O perfil é
gravado em `users/{uid}` no Firestore com o mesmo UID. A API solicita o envio da
verificação ao Firebase Authentication; não é necessário SMTP.

1. Envie os dados cadastrais para `POST /cadastro/solicitar`. A resposta `202` confirma
   que o cadastro foi criado e orienta a pessoa a verificar a caixa de entrada e o spam.
2. A pessoa abre o link do template de verificação do Firebase para confirmar o endereço.
   O Firebase processa o código de ação diretamente.
3. O login continua permitido antes da verificação. Depois de entrar, consulte
   `GET /auth/verificacao/status` com `Authorization: Bearer <idToken>` para obter
   `{ "emailVerified": true|false }`.
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
apresentar a instrução de confirmação após o `202`, permitir login, consultar o endpoint
de status e oferecer o reenvio. Se também usar o Firebase Client SDK, deve recarregar o
usuário após a volta do link (`reload(user)`) antes de atualizar a interface.

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
| `GET` | `/agendamentos/{id}` | Consulta um agendamento próprio; UID administrativo pode consultar qualquer um | `200 OK` |
| `PATCH` | `/agendamentos/{id}/status` | Admin aprova ou rejeita um agendamento pendente | `200 OK` |

O POST recebe datas ISO (`yyyy-MM-dd`) e horários (`HH:mm`). Os serviços aceitos são
`consulta`, `vacina` e `hospedagem`, em minúsculas. `animalId` precisa identificar um
animal pertencente ao usuário autenticado. `observacoes` é opcional, com até 1000
caracteres; data, horário e animal são obrigatórios.

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
  "observacoes": "Consulta de rotina"
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
  "status": "PENDENTE",
  "observacoes": "Consulta de rotina",
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
comuns recebem `403`; transições inválidas recebem `409`. `CANCELADO` está reservado
no modelo, mas não há endpoint de cancelamento no MVP.

Erros usam o envelope geral `{ "status": 400, "mensagem": "...", "timestamp": "..." }`.
Além dos erros usuais de token (`401`) e e-mail não verificado (`403`), os endpoints
retornam `400` para payload/campos inválidos, `403` para falta de privilégio
administrativo, `404` para agendamento ausente ou alheio ao cliente e `409` para
transições de status inválidas. Falhas inesperadas de persistência retornam `500`.

### Autorização administrativa

Configure no servidor a variável `ADMIN_UIDS` como uma lista separada por vírgulas de
UIDs Firebase confiáveis, por exemplo `ADMIN_UIDS=uid-admin-1,uid-admin-2`. Não existe
valor administrativo padrão; sem configuração, nenhum usuário pode alterar status.
O backend compara essa lista com o UID extraído do ID token validado. O frontend não
pode conceder privilégios por parâmetro ou campo JSON.

O inventário real da coleção Firestore não foi lido nesta implementação. A API cria
novos documentos com IDs gerados e não migra nem sobrescreve documentos preexistentes.
Ao excluir uma conta em `DELETE /users/me`, seus agendamentos também são excluídos,
junto com os animais e o perfil.

### Execução local e CORS

Configure `FIREBASE_SERVICE_ACCOUNT_PATH`, `FIREBASE_WEB_API_KEY` e as configurações
Firebase descritas acima. Inicie a API com `.\mvnw.cmd spring-boot:run` na raiz do
backend. A configuração CORS aceita as origens locais `http://localhost` e
`http://127.0.0.1` em qualquer porta (incluindo Live Server); origens adicionais devem
ser listadas em `CORS_ALLOWED_ORIGINS` separadas por vírgulas. Em produção, configure
somente as origens HTTPS reais do frontend, sem liberar `*`.
