# Event Processor – Teste Técnico Fujitsu Fullstack Andrés Castellanos

Solução do teste técnico fullstack: uma API que recebe eventos dos equipamentos da fábrica e uma interface web para cadastrar e consultar esses eventos.

Usei Java 21 com Spring Boot 4.1 no backend, já que é a stack que vocês usam no dia a dia, e Angular 22 no frontend, com Angular Material e Tailwind.

## Como rodar

### Com Docker

Se tiver Docker instalado, basta um comando na raiz do projeto:

```bash
docker compose up --build
```

O frontend fica em http://localhost:4200 e a API em http://localhost:8080.

> Observação: não consegui validar o compose na minha máquina porque não tenho Docker instalado. Os Dockerfiles seguem o padrão multi-stage, mas se algo falhar, a execução local abaixo foi testada.

### Localmente

Precisa de JDK 21 e Node 24. Não é necessário instalar o Maven, o projeto usa o Maven Wrapper.

Backend:

```bash
cd backend
./mvnw spring-boot:run
```

Frontend, em outro terminal:

```bash
cd frontend
npm install
npm start
```

Depois é só abrir http://localhost:4200. Em desenvolvimento, o Angular redireciona as chamadas `/api` para o backend na porta 8080 (veja `proxy.conf.json`), então não precisei configurar nada de CORS para o frontend funcionar.

Alguns endereços úteis do backend:

- Swagger: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health
- Console do H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:eventsdb`, usuário `sa`, sem senha)

Para testar a API direto pelo terminal, com o payload do enunciado:

```bash
curl -i -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"eventId":"EVT-1001","eventType":"PRODUCTION","description":"Production line started","timestamp":"2026-05-20T10:00:00"}'
```

Rodando o mesmo comando uma segunda vez, a API responde 409, porque o evento já foi processado.

## Testes

```bash
cd backend && ./mvnw test
cd frontend && npm test -- --watch=false
```

No backend são 26 testes. Os de service e dos handlers são unitários, com Mockito. O controller é testado com MockMvc, para conferir os códigos HTTP e o formato dos erros. Tem também um teste de repositório que garante que a restrição de unicidade funciona e um teste de integração que percorre o fluxo inteiro, sem mocks, do POST até a auditoria no banco.

No frontend são 23 testes, com Vitest. Cobrem o serviço de API, o tratamento de erros, o tema claro/escuro e os componentes de formulário e listagem, sempre com a API mockada.

## Endpoints

| Método | Rota | O que faz |
|--------|------|-----------|
| POST | `/api/events` | Registra um evento |
| GET | `/api/events` | Lista os eventos (aceita `?type=PRODUCTION` ou `?type=QUALITY`) |
| GET | `/api/events/{eventId}` | Busca um evento pelo ID |
| GET | `/api/audits` | Lista os registros de auditoria |

O POST pode responder:

- 201 quando o evento é registrado;
- 400 quando falta algum campo ou o JSON está errado;
- 409 quando o `eventId` já foi processado;
- 415 se o `Content-Type` não for JSON.

Os erros seguem o formato Problem Details (RFC 9457), com uma mensagem em português e um `correlationId`, que é o mesmo que aparece no log daquela requisição. Nos erros de validação, a resposta também traz a mensagem de cada campo, e o frontend usa isso para mostrar o erro embaixo do input correspondente.

Sobre os campos:

- `eventId` aceita letras, números, `-` e `_`, com até 50 caracteres, e diferencia maiúsculas de minúsculas.
- `description` não pode ficar vazia nem ter só espaços, e tem limite de 500 caracteres.
- `timestamp` segue o formato do exemplo, sem fuso horário.

## Como está organizado

O fluxo de um evento no backend é este:

```
requisição → CorrelationIdFilter → EventController → EventService → handler do tipo → banco
                                                                      └─ QUALITY → AuditService
```

O backend está dividido em pacotes por responsabilidade: `web` (controllers), `service` (regras), `handler` (o tratamento de cada tipo de evento), `repository`, `domain` (entidades), `dto` e `exception`.

O frontend segue a divisão comum em projetos Angular:

- `core` concentra serviços, modelos e o interceptor de erros;
- `shared` tem componentes pequenos e reaproveitáveis, como o selo do tipo de evento e o botão de tema;
- `features` tem uma pasta por tela: cadastro, lista de eventos e auditoria.

Cada tela é carregada sob demanda pela rota.

## Decisões que tomei

**Duplicidade.** Essa foi a parte em que mais pensei. Verificar se o `eventId` já existe antes de salvar resolve o caso comum, mas não protege contra duas requisições iguais chegando ao mesmo tempo: as duas consultam, nenhuma encontra nada e as duas gravam. Por isso coloquei também uma restrição `UNIQUE` na coluna. Se a consulta deixar passar, o banco barra, e eu converto esse erro em 409. Testei mandando 10 requisições simultâneas com o mesmo ID: uma foi aceita e nove receberam 409.

**409 em vez de 400.** O pedido não está mal formado. Ele entra em conflito com algo que já existe no servidor, e é para isso que serve o 409.

**Tipos de evento.** Em vez de um `if (tipo == QUALITY)` dentro do service, cada tipo tem sua própria classe (`ProductionEventHandler` e `QualityEventHandler`). Com só dois tipos pode parecer exagero, mas foi pouco código a mais, e se amanhã surgir um tipo `MAINTENANCE`, basta criar uma classe nova sem mexer no que já funciona.

**Evento e auditoria na mesma transação.** Se a gravação da auditoria falhar, o evento também não é salvo. Preferi assim a correr o risco de ter um evento de qualidade sem o registro de auditoria correspondente.

**Datas.** O `timestamp` do enunciado vem sem fuso, então usei `LocalDateTime`. O horário em que a API recebeu o evento (`receivedAt`) é gerado pelo servidor em UTC. Injetei um `Clock` para que os testes não dependam do horário em que rodam.

**Banco H2 em memória.** O enunciado permite persistência simples, e o H2 dispensa instalar qualquer coisa. Como uso JPA, trocar por PostgreSQL seria basicamente mudar a dependência e a URL de conexão.

**Logs.** Cada requisição recebe um `correlationId`, que aparece em todas as linhas de log dela e volta para o cliente no header `X-Correlation-Id`. Se alguém reportar um erro, dá para achar exatamente o que aconteceu. O filtro também registra método, rota, status e tempo de cada chamada.

**Frontend.** Usei o que o Angular 22 traz como padrão: componentes standalone, signals e `httpResource` para as listagens, que já entrega carregando, erro e recarregar sem precisar gerenciar `subscribe`. Todos os erros HTTP passam por um interceptor que os transforma em uma mensagem amigável, então os componentes não precisam lidar com `HttpErrorResponse`.

**Material e Tailwind juntos.** O Material cuida dos componentes, que já vêm acessíveis. O Tailwind cuida do layout e da responsividade. Liguei as cores do Tailwind às do tema do Material, assim o modo escuro funciona nos dois sem duplicar estilos.

**Animações.** Fiz com CSS puro e com o `animate.enter` nativo do Angular, porque o pacote `@angular/animations` foi descontinuado no Angular 22. Elas desligam sozinhas se o sistema operacional estiver configurado para reduzir movimento.

## O que ficou de fora

Algumas coisas deixei de fora de propósito, para manter a solução simples como o enunciado pede:

- A API não tem autenticação. Em produção, os equipamentos precisariam se identificar, com mTLS ou um token por equipamento.
- Os dados somem quando o backend reinicia, por causa do H2 em memória.
- A listagem não tem paginação. Com poucos eventos não faz diferença, mas com volume real faria.

## Próximos passos

Se fosse continuar o projeto:

- trocaria o H2 por PostgreSQL, com Flyway para versionar o schema;
- receberia os eventos por fila (Kafka ou RabbitMQ) em vez de HTTP direto, porque equipamentos de fábrica reenviam mensagens, e uma fila com consumidor idempotente lida melhor com isso;
- adicionaria paginação e filtro por período;
- exportaria métricas para Prometheus (eventos por tipo, quantidade de duplicados) e passaria os logs para JSON;
- escreveria testes end-to-end com Playwright e usaria Testcontainers para testar com um banco real;
- montaria um pipeline de CI que rode build e testes a cada push.
