# Arquitetura do Ordermod

Este documento descreve a arquitetura atualmente implementada no projeto. O sistema é um monólito modular construído com Spring Boot e Spring Modulith, organizado por capacidades de negócio. Dentro do módulo `order`, a implementação segue uma arquitetura hexagonal com domínio, portas e adaptadores.

Para uma explicação passo a passo, consulte o [tutorial de arquitetura hexagonal](tutorial-arquitetura-hexagonal.md).

O histórico da refatoração está em [Registro de desenvolvimento — 01/09/2026](development-log-2026-09-01.md). O estado anterior está preservado no [registro de 31/08/2026](development-log-2026-08-31.md).

## Visão geral

O pacote principal da aplicação é `com.market`. Cada subpacote direto representa um módulo lógico detectado pelo Spring Modulith:

- `com.market.order`: recebimento, persistência e publicação da criação de pedidos;
- `com.market.inventory`: reação à criação do pedido e reserva de itens, ainda sem persistência real;
- `com.market.notification`: reação à criação do pedido para notificação;
- `com.market.payment`: processamento de pagamentos, ainda sem implementação.

Os módulos são executados na mesma aplicação e no mesmo processo Java. Eles não são módulos Maven independentes nem microsserviços.

```mermaid
flowchart LR
    Client[Cliente HTTP] --> Order[order]
    Order -- OrderCreatedEvent --> Inventory[inventory]
    Order -- OrderCreatedEvent --> Notification[notification]
    Order -. integração futura .-> Payment[payment]

    subgraph Application[Aplicação Spring Boot]
        Order
        Inventory
        Notification
        Payment
    end
```

## Tecnologias

| Tecnologia | Versão ou finalidade |
| --- | --- |
| Java | 25, configurado no `pom.xml` |
| Spring Boot | 4.0.8 |
| Spring Modulith | 2.0.8 |
| Spring Web MVC | API HTTP |
| Jakarta Bean Validation | Validação das requisições |
| Spring Data JDBC | Persistência relacional do agregado de pedidos |
| PostgreSQL | Banco de dados |
| Flyway | Versionamento e aplicação das migrations |
| Spring Modulith JDBC | Registro persistente das publicações de eventos |
| Springdoc OpenAPI | Documentação e Swagger UI |
| Maven | Build e gerenciamento de dependências |
| Docker Compose | PostgreSQL para o ambiente local |

## Módulos e fronteiras

O pacote raiz de cada módulo representa sua API Java pública. Os subpacotes `internal` contêm detalhes que não devem ser acessados pelos demais módulos.

| Localização | Finalidade | Pode ser acessado por outro módulo? |
| --- | --- | --- |
| `com.market.order` | Contratos públicos do módulo `order` | Sim |
| `com.market.order.internal.*` | Implementação do módulo `order` | Não |
| `com.market.inventory` | Contratos públicos do módulo `inventory` | Sim |
| `com.market.inventory.internal.*` | Implementação do módulo `inventory` | Não |
| `com.market.notification` | Contratos públicos do módulo `notification` | Sim |
| `com.market.notification.internal.*` | Implementação do módulo `notification` | Não |
| `com.market.payment` | Contratos públicos do módulo `payment` | Sim |
| `com.market.payment.internal.*` | Implementação do módulo `payment` | Não |

`OrderCreatedEvent` permanece em `com.market.order` porque é o contrato público que os módulos consumidores importam. Os DTOs HTTP, comandos, modelos de domínio, portas e adaptadores permanecem em `com.market.order.internal`.

O nome `internal` expressa uma fronteira arquitetural do Spring Modulith, não um modificador de acesso do Java. O teste `ModularityTests` usa `ApplicationModules.of(OrdermodApplication.class).verify()` para fiscalizar acessos entre módulos e ciclos de dependência.

## Arquitetura hexagonal do módulo order

```text
src/main/java/com/market/order
├── OrderCreatedEvent.java                         # API pública entre módulos
├── package-info.java
└── internal
    ├── domain
    │   ├── model
    │   │   ├── Order.java
    │   │   └── OrderItem.java
    │   ├── exception
    │   │   └── OrderDomainException.java
    │   └── package-info.java
    ├── application
    │   ├── port
    │   │   ├── in
    │   │   │   ├── CreateOrderUseCase.java
    │   │   │   ├── CreateOrderCommand.java
    │   │   │   └── CreateOrderResult.java
    │   │   └── out
    │   │       ├── OrderRepository.java
    │   │       └── OrderEventPublisher.java
    │   ├── service
    │   │   └── CreateOrderService.java
    │   └── package-info.java
    └── adapter
        ├── in
        │   └── web
        │       ├── CreateOrderRequest.java
        │       ├── OrderController.java
        │       └── OrderHttpApi.java
        └── out
            ├── persistence
            │   └── jdbc
            │       ├── OrderPersistenceAdapter.java
            │       ├── SpringDataOrderRepository.java
            │       ├── OrderJdbcEntity.java
            │       ├── OrderItemJdbcEntity.java
            │       └── OrderPersistenceMapper.java
            └── event
                └── SpringOrderEventPublisher.java
```

Os três arquivos `package-info.java` fazem parte do desenho acordado: documentam a API pública do módulo e as camadas `domain` e `application`. Não há arquivos desse tipo adicionais nos subpacotes.

### Responsabilidade de cada área

| Área | Responsabilidade | Dependências permitidas |
| --- | --- | --- |
| `domain.model` | Estado, invariantes e comportamento do negócio | Java; não depende de Spring, JDBC ou adaptadores |
| `domain.exception` | Exceção específica para violações das invariantes de pedido | Java; não depende de Spring, JDBC ou adaptadores |
| `application.port.in` | Contratos para iniciar casos de uso | Tipos de entrada e resultado da aplicação |
| `application.service` | Orquestra o caso de uso e delimita a transação | Domínio, portas de entrada/saída e contrato público do evento |
| `application.port.out` | Necessidades externas declaradas pela aplicação | Domínio e contrato público do evento |
| `adapter.in.web` | Converte HTTP em chamada de caso de uso | Porta de entrada; não chama persistência diretamente |
| `adapter.out.persistence.jdbc` | Traduz o domínio e persiste com Spring Data JDBC | Porta de saída, domínio, JDBC e entidades relacionais |
| `adapter.out.event` | Publica o evento pelo mecanismo do Spring | Porta de saída e `ApplicationEventPublisher` |

### Direção das dependências

As dependências de código apontam dos adaptadores para as portas e para o núcleo da aplicação. O núcleo não conhece controllers, Spring Data repositories nem entidades JDBC.

```mermaid
flowchart LR
    HTTP[Adaptador HTTP] --> InPort[CreateOrderUseCase]
    InPort --> Service[CreateOrderService]
    Service --> Domain[Order / OrderItem]
    Service --> RepositoryPort[OrderRepository]
    Service --> EventPort[OrderEventPublisher]
    Persistence[Adaptador JDBC] --> RepositoryPort
    Persistence --> Domain
    Persistence --> JDBC[(Spring Data JDBC)]
    EventAdapter[Adaptador de eventos Spring] --> EventPort
    EventAdapter --> SpringEvents[ApplicationEventPublisher]
```

No diagrama, as setas representam dependências de código ou chamadas através de contratos. As implementações das portas de saída são injetadas pelo Spring em tempo de execução.

## Fluxo de criação de pedido

```mermaid
sequenceDiagram
    autonumber
    actor Client as Cliente
    participant Controller as OrderController
    participant InPort as CreateOrderUseCase
    participant Service as CreateOrderService
    participant Repository as OrderRepository (porta)
    participant Jdbc as OrderPersistenceAdapter
    participant Database as PostgreSQL
    participant EventPort as OrderEventPublisher (porta)
    participant Events as SpringOrderEventPublisher
    participant Listener as Consumidores Modulith

    Client->>Controller: POST /api/v1/order + CreateOrderRequest
    Controller->>Controller: request → CreateOrderCommand
    Controller->>InPort: createOrder(command)
    InPort->>Service: executa caso de uso
    Service->>Service: cria Order e OrderItem
    Service->>Repository: save(order)
    Repository->>Jdbc: implementação injetada
    Jdbc->>Jdbc: domínio → entidades JDBC
    Jdbc->>Database: INSERT pedido e itens
    Database-->>Jdbc: agregado persistido
    Jdbc-->>Service: entidades JDBC → domínio
    Service->>EventPort: publish(OrderCreatedEvent)
    EventPort->>Events: implementação injetada
    Events->>Listener: publicação pelo Spring Modulith
    Service-->>Controller: CreateOrderResult(orderId)
    Controller-->>Client: 201 Pedido recebido com sucesso
```

O controller recebe um `CreateOrderResult`, mas o contrato HTTP atual preserva a resposta textual e ainda não expõe o `orderId`.

### Modelos em cada fronteira

```text
CreateOrderRequest                  contrato HTTP
        ↓ OrderController
CreateOrderCommand                  entrada do caso de uso
        ↓ CreateOrderService
Order + OrderItem                   domínio puro
        ↓ OrderPersistenceMapper
OrderJdbcEntity + OrderItemJdbcEntity
        ↓ Spring Data JDBC
orders.orders + orders.order_items

Order + OrderItem
        ↓ CreateOrderService
OrderCreatedEvent
        ↓ SpringOrderEventPublisher
InventoryOrderCreatedListener / NotificationOrderCreatedListener
```

Essa separação impede que mudanças no JSON ou no esquema relacional contaminem diretamente o domínio. Também permite testar o caso de uso com implementações simples das portas, sem iniciar HTTP ou PostgreSQL.

## API HTTP

O contrato HTTP fica em `adapter.in.web.OrderHttpApi`. O `OrderController` implementa essa interface e contém somente a adaptação para a porta de entrada.

| Método | Caminho | Consome | Produz | Resposta de sucesso |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/order` | `application/json` | `text/plain` | `201 Created` |

Exemplo de requisição:

```json
{
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "paymentMethod": "PIX",
  "items": [
    {
      "productId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "quantity": 2
    },
    {
      "productId": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
      "quantity": 1
    }
  ]
}
```

Resposta atual:

```text
Pedido recebido com sucesso
```

### Validações de entrada

| Campo | Regra |
| --- | --- |
| `customerId` | Obrigatório e representado por UUID |
| `paymentMethod` | Obrigatório e não pode ser vazio ou composto apenas por espaços |
| `items` | Obrigatório e deve conter pelo menos um item |
| `items[].productId` | Obrigatório e representado por UUID |
| `items[].quantity` | Deve ser maior que zero |

O `@Valid` no método HTTP ativa a validação do request e a validação em cascata dos itens.

## OpenAPI e Swagger UI

As anotações REST e OpenAPI ficam concentradas em `OrderHttpApi`. O controller implementa o contrato sem duplicar essas anotações.

Com a aplicação em execução nas configurações padrão:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

## Persistência

O domínio não possui anotações do Spring Data JDBC. O mapeamento relacional fica exclusivamente no adaptador de saída:

- `OrderJdbcEntity` representa `orders.orders` e é a raiz do agregado JDBC;
- `OrderItemJdbcEntity` representa `orders.order_items`;
- `SpringDataOrderRepository` é o `CrudRepository` técnico;
- `OrderPersistenceMapper` converte domínio e entidades nos dois sentidos;
- `OrderPersistenceAdapter` implementa a porta `OrderRepository` usada pelo caso de uso.

O esquema relacional versionado pelo Flyway é:

```text
orders.orders
├── id UUID PK
├── customer_id UUID
├── payment_method VARCHAR(50)
├── created_at TIMESTAMPTZ
└── version INTEGER

orders.order_items
├── id UUID PK
├── order_id UUID FK → orders.orders.id
├── product_id UUID
├── quantity INTEGER CHECK > 0
└── item_index INTEGER
```

`@MappedCollection` configura `order_id` como chave estrangeira e `item_index` como a posição da lista. `@Version` permite ao Spring Data JDBC distinguir um agregado novo, mesmo com UUID gerado pela aplicação, e oferece controle otimista de concorrência.

No ambiente local, o PostgreSQL do `compose.yaml` é publicado em `localhost:5433`. As credenciais de desenvolvimento atuais são banco `ordermod`, usuário `myuser` e senha `1234`.

Nos testes de integração, `PostgresTestcontainersConfiguration` fornece um PostgreSQL `18.6` isolado através de `@ServiceConnection`. O Docker Compose da aplicação é desabilitado nesse contexto, e o container efêmero recebe a migration Flyway antes dos testes de persistência.

## Eventos e transação

`CreateOrderService.createOrder` é transacional. Dentro do mesmo limite, o serviço:

1. cria o agregado de domínio;
2. persiste através da porta `OrderRepository`;
3. cria `OrderCreatedEvent` a partir do agregado persistido;
4. publica através da porta `OrderEventPublisher`.

`SpringOrderEventPublisher` é o único adaptador que conhece `ApplicationEventPublisher`. Assim, o caso de uso não depende diretamente da API técnica de publicação do Spring.

O Spring Modulith registra em `event_publication` as publicações destinadas aos listeners transacionais. A propriedade de inicialização automática do esquema JDBC continua adequada ao desenvolvimento; em produção, essa estrutura deve ser criada por migration versionada.

`InventoryOrderCreatedListener` usa o identificador estável `inventory.reserve-items-on-order-created`. O listener adapta o evento e delega para `InventoryService.reserveItems`. `InventoryService` ainda apenas valida os dados e registra a intenção no log. O módulo `notification` também reage ao evento; `payment` permanece sem comportamento.

Como persistência e publicação são chamadas dentro da transação do caso de uso, uma falha antes do commit causa rollback do pedido, dos itens e do registro da publicação. `CreateOrderTransactionIntegrationTest` protege esse comportamento simulando uma falha depois de delegar a publicação ao Spring e verificando que as três contagens permanecem inalteradas.

## Testes automatizados

A suíte cobre níveis diferentes da arquitetura e do fluxo:

| Nível | Testes e cobertura |
| --- | --- |
| Módulos | `ModularityTests` executa `ApplicationModules.verify()` para detectar ciclos e acessos indevidos entre módulos |
| Hexagonal | `HexagonalArchitectureTests` aplica quatro regras ArchUnit sobre domínio, aplicação, portas e adaptadores de entrada |
| Domínio | `OrderTest` e `OrderItemTest` verificam invariantes, normalização e imutabilidade sem Spring |
| Caso de uso | `CreateOrderServiceTest` verifica `save` antes de `publish`, conteúdo do evento, resultado e falhas das portas |
| Adaptador HTTP | `OrderControllerTest` verifica o mapeamento; `OrderControllerHttpTest` usa MockMvc standalone para validar `201`, texto de sucesso e `400` |
| Mapper e eventos | `OrderPersistenceMapperTest` verifica as conversões; `SpringOrderEventPublisherTest` verifica a delegação técnica |
| Persistência | `OrderPersistenceAdapterTest` usa PostgreSQL 18.6 isolado, migration V1, INSERT com versão `0`, índices `0/1` e UPDATE com versão `1` |
| Integração de eventos | `CreateOrderIntegrationTest` comprova pedido, itens e duas publicações `COMPLETED`, para `inventory` e `notification` |
| Transação | `CreateOrderTransactionIntegrationTest` comprova rollback de pedido, itens e `event_publication` após falha simulada |
| Contexto | `OrdermodApplicationTests` comprova o carregamento da aplicação com o PostgreSQL de teste |

Em 01/09/2026, `./mvnw clean test` foi executado com Java 25: 27 testes, sem falhas, erros ou testes ignorados, com `BUILD SUCCESS`.

## Decisões arquiteturais

1. **Monólito modular:** os módulos permanecem no mesmo deploy, com fronteiras verificáveis.
2. **Hexagonal dentro de `order`:** o módulo continua orientado ao negócio e organiza internamente entradas, núcleo e saídas.
3. **API pública mínima:** somente `OrderCreatedEvent` é compartilhado por `order`.
4. **Domínio puro:** `Order` e `OrderItem` não dependem de Spring ou JDBC.
5. **Porta de entrada explícita:** o controller depende de `CreateOrderUseCase`, não da classe concreta do serviço.
6. **Portas de saída explícitas:** persistência e eventos são capacidades solicitadas pela aplicação.
7. **Adaptadores técnicos isolados:** Spring MVC, Spring Data JDBC e `ApplicationEventPublisher` ficam nas bordas.
8. **Modelos separados:** request, comando, domínio, entidades JDBC e evento não são reutilizados entre fronteiras.
9. **Persistência antes do evento:** o fato público usa os dados retornados pelo repositório.
10. **Transação única de origem:** pedido, itens e registro da publicação confirmam ou sofrem rollback juntos.
11. **Contrato HTTP preservado:** a reorganização interna não altera caminho, validação, status ou mensagem.
12. **Banco versionado:** alterações no esquema de negócio são feitas por migrations Flyway.

## Limitações e próximos passos

1. Retornar o `orderId` ou o cabeçalho `Location` na resposta `201 Created`.
2. Implementar reserva e persistência real no módulo `inventory`.
3. Implementar o módulo `payment` e sua reação aos eventos do pedido.
4. Tornar os consumidores idempotentes para suportar reprocessamento.
5. Criar migration da estrutura de publicação do Spring Modulith para produção.
6. Padronizar respostas de erro com `@RestControllerAdvice`.
7. Fixar uma versão específica da imagem PostgreSQL de desenvolvimento e externalizar credenciais.
