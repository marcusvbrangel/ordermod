# Registro de desenvolvimento — 01/09/2026

## Resultado estrutural

O módulo `order` foi reorganizado internamente para uma arquitetura hexagonal. A fronteira do Spring Modulith não mudou: `com.market.order` continua sendo o módulo e `OrderCreatedEvent` continua sendo sua API pública para `inventory` e `notification`.

A mudança separou explicitamente:

- adaptador HTTP de entrada;
- porta e serviço do caso de uso;
- domínio puro;
- portas de persistência e publicação de eventos;
- adaptador Spring Data JDBC;
- adaptador de publicação de eventos do Spring.

## Organização implementada

```text
com.market.order
├── OrderCreatedEvent
├── package-info.java
└── internal
    ├── domain
    │   ├── event
    │   │   ├── OrderDomainEvent
    │   │   └── OrderPlacedDomainEvent
    │   ├── exception
    │   │   └── OrderDomainException
    │   ├── model
    │   │   ├── Order / OrderItem
    │   │   └── seis Value Objects
    │   └── package-info.java
    ├── application
    │   ├── port.in
    │   ├── port.out
    │   └── service
    │   └── package-info.java
    └── adapter
        ├── in.web
        └── out
            ├── persistence.jdbc
            └── event
```

### Entrada HTTP

`CreateOrderRequest`, `OrderHttpApi` e `OrderController` foram movidos para `adapter.in.web`. O controller agora depende da abstração `CreateOrderUseCase` e converte o request em `CreateOrderCommand`.

O contrato externo foi preservado:

- `POST /api/v1/order`;
- request JSON e validações existentes;
- resposta `201 Created`;
- corpo `Pedido recebido com sucesso`.

`CreateOrderResult` tornou explícito o resultado do caso de uso e contém o `orderId`. O controller ainda não expõe esse identificador na resposta HTTP.

### Aplicação e portas

O antigo `OrderService` foi substituído por `CreateOrderService`, que implementa `CreateOrderUseCase`. O serviço depende somente das portas de saída:

- `OrderRepository`, para persistir o agregado;
- `OrderEventPublisher`, para publicar o fato de negócio.

O limite transacional permanece no método do caso de uso. Após a evolução de DDD tático, a sequência é criar a Aggregate Root, persistir o pedido e despachar seu Domain Event por uma porta. O adaptador de saída o traduz para `OrderCreatedEvent` e o publica.

### Domínio e persistência

`Order` e `OrderItem` foram movidos para `domain.model` e não possuem anotações do Spring Data JDBC. `OrderDomainException`, em `domain.exception`, representa as violações das invariantes de pedido. As invariantes e cópias defensivas permanecem no domínio.

A árvore foi alinhada rigorosamente ao desenho acordado: somente a raiz do módulo e as camadas `domain` e `application` possuem `package-info.java`.

O modelo relacional passou a ser separado:

- `OrderJdbcEntity` e `OrderItemJdbcEntity` contêm o mapeamento Spring Data JDBC;
- `SpringDataOrderRepository` é o repositório técnico;
- `OrderPersistenceMapper` converte domínio e entidades JDBC;
- `OrderPersistenceAdapter` implementa a porta de persistência.

Essa separação permite evoluir o domínio e o esquema relacional de forma independente. A migration e as tabelas existentes não foram alteradas por essa reorganização.

### Publicação de eventos

`SpringOrderEventPublisher` implementa `OrderEventPublisher` e concentra a dependência de `ApplicationEventPublisher`. Após a evolução de DDD, `CreateOrderService` conhece a porta e `OrderDomainEvent`, mas não conhece o contrato público `OrderCreatedEvent` nem o mecanismo técnico de publicação.

Os listeners dos módulos consumidores e seus identificadores permanecem inalterados.

## Evolução para DDD tático

Sobre a base hexagonal, o domínio de `order` passou a demonstrar os seguintes padrões táticos:

- `Order` é a Aggregate Root e controla a coleção do agregado;
- `OrderItem` é uma Entity interna, com igualdade baseada em `OrderItemId`;
- `OrderId`, `CustomerId`, `OrderItemId`, `ProductId`, `Quantity` e `PaymentMethod` são os seis Value Objects;
- `Order.place(...)` representa a colocação de um pedido novo e registra `OrderPlacedDomainEvent`;
- `Order.reconstitute(...)` recria o estado persistido e não registra evento de criação;
- `OrderDomainEvent` e `OrderPlacedDomainEvent` permanecem internos ao domínio;
- `OrderCreatedEvent` continua sendo o contrato público estável para `inventory` e `notification`;
- `CreateOrderService` despacha o evento interno somente depois de a persistência retornar;
- `SpringOrderEventPublisher` traduz o evento interno para o evento público na borda da aplicação;
- `OrderRepository` trabalha com a Aggregate Root completa, sem repository separado para `OrderItem`;
- `OrderPersistenceMapper` desembrulha os Value Objects na ida ao JDBC e os recompõe na reconstituição.

A migration, as tabelas, o endpoint HTTP e o formato de `OrderCreatedEvent` não foram alterados por essa evolução.

O estudo não adicionou Event Sourcing, CQRS, Saga, Domain Service, Specification ou repository para `OrderItem`. Também não afirma que o módulo técnico seja automaticamente um Bounded Context descoberto por DDD estratégico.

O material didático correspondente foi registrado em [Tutorial de DDD tático no módulo `order`](tutorial-ddd-tatico.md).

## Fluxo após a refatoração

```text
POST /api/v1/order
    → OrderController
    → CreateOrderUseCase
    → CreateOrderService
    → Entities + seis Value Objects
    → Order.place(...)
    → OrderPlacedDomainEvent (interno)
    → OrderRepository (porta)
    → OrderPersistenceAdapter
    → OrderPersistenceMapper
    → SpringDataOrderRepository
    → PostgreSQL
    → OrderEventPublisher (porta)
    → SpringOrderEventPublisher
    → tradução para OrderCreatedEvent (público)
    → Inventory / Notification
```

## Baseline validada antes do DDD tático

Antes da evolução descrita acima, a suíte da refatoração hexagonal havia sido executada com Java 25:

```text
./mvnw clean test

Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

A execução recompilou o projeto desde uma árvore limpa e validou os níveis abaixo naquele estado anterior.

### Fronteiras arquiteturais

- `ModularityTests` executou `ApplicationModules.verify()` com sucesso.
- `HexagonalArchitectureTests` executou quatro regras ArchUnit.
- O domínio não pode depender de Spring, aplicação ou adaptadores.
- A aplicação não pode depender dos adaptadores.
- As portas não podem depender de Spring, services ou adaptadores.
- Adaptadores de entrada não podem acessar diretamente services, portas de saída ou adaptadores de saída.

### Unidade e adaptação

- Os testes de `Order` e `OrderItem` validaram invariantes, `OrderDomainException`, normalização e imutabilidade.
- `CreateOrderServiceTest` validou persistência antes da publicação, conteúdo do evento, `CreateOrderResult` e propagação de falhas das portas.
- `OrderControllerTest` validou o mapeamento do request para o comando.
- `OrderPersistenceMapperTest` validou as conversões entre domínio e entidades JDBC, incluindo itens e versão.
- `SpringOrderEventPublisherTest` validou a delegação para `ApplicationEventPublisher`.

### Contrato HTTP

`OrderControllerHttpTest` utilizou MockMvc standalone e comprovou:

- `201 Created` para um pedido válido;
- conteúdo `text/plain`;
- corpo `Pedido recebido com sucesso`;
- `400 Bad Request` para forma de pagamento em branco e lista vazia.

### PostgreSQL isolado e persistência

`PostgresTestcontainersConfiguration` inicializou PostgreSQL `18.6` com banco e credenciais exclusivos de teste. O Docker Compose da aplicação permaneceu desabilitado durante esses testes.

`OrderPersistenceAdapterTest` comprovou:

- aplicação da migration Flyway V1;
- INSERT do agregado com UUID atribuído pela aplicação;
- versão inicial `0` retornada pelo Spring Data JDBC;
- gravação dos dois itens com `item_index` igual a `0` e `1`;
- preservação da forma de pagamento;
- UPDATE de um agregado existente;
- incremento da versão para `1` sem duplicar pedido ou itens.

### Publicação durável e consumidores

`CreateOrderIntegrationTest` executou o caso de uso completo e comprovou:

- um pedido persistido;
- dois itens persistidos;
- duas linhas correspondentes em `event_publication`;
- as duas publicações com `status = COMPLETED` e `completion_date` preenchida.

As duas publicações representam o processamento pelos listeners de `inventory` e `notification`.

### Rollback transacional

`CreateOrderTransactionIntegrationTest` substituiu a porta de eventos por um publicador que primeiro executa `delegate.publishEvent(event)` e depois lança uma falha simulada. Após a exceção, as contagens de `orders.orders`, `orders.order_items` e `event_publication` permaneceram iguais às contagens anteriores.

Isso comprova que pedido, itens e registro durável da publicação participam da mesma transação de origem e sofrem rollback juntos.

## Validação da evolução de DDD

Após concluir a implementação, a suíte foi executada novamente desde uma árvore compilada do zero, com Java 25 e PostgreSQL 18.6 iniciado pelo Testcontainers:

```text
./mvnw clean test

Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Além das 27 verificações da baseline hexagonal, a suíte agora cobre os seis Value Objects, identidade de `Order` e `OrderItem`, `place` contra `reconstitute`, ciclo dos Domain Events, tradução para `OrderCreatedEvent`, estado completo no mapper e no PostgreSQL e item nulo rejeitado pelo contrato HTTP.

## Pontos de atenção

- O projeto exige Java 25; o terminal local pode continuar apontando para Java 21.
- Os testes de integração exigem Docker disponível para iniciar o PostgreSQL 18.6 do Testcontainers.
- O domínio não deve voltar a importar Spring Data JDBC.
- Controllers não devem depender da implementação concreta `CreateOrderService`.
- O caso de uso não deve importar entidades ou repositórios do adaptador JDBC.
- O adaptador de persistência deve preservar o campo `version` ao converter entidade e domínio.
- O contrato público `OrderCreatedEvent` deve permanecer fora de `internal` enquanto outros módulos o consumirem.
- A resposta HTTP com o `orderId` continua sendo uma evolução futura e deve ser tratada como mudança de contrato.
