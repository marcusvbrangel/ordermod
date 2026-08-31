# Arquitetura do Ordermod

Este documento descreve a arquitetura atualmente implementada no projeto. O sistema é um monólito modular construído com Spring Boot e Spring Modulith, organizado por capacidades de negócio.

## Visão geral

O pacote principal da aplicação é `com.market`. Cada subpacote direto representa um módulo lógico detectado pelo Spring Modulith:

- `com.market.order`: recebimento e criação de pedidos.
- `com.market.inventory`: reserva de itens em estoque.
- `com.market.payment`: processamento de pagamentos, ainda sem implementação.

Os módulos são executados na mesma aplicação e no mesmo processo Java. Eles não são módulos Maven independentes nem microsserviços.

```mermaid
flowchart LR
    Client[Cliente HTTP] --> Order[order]
    Order -- OrderCreatedEvent --> Inventory[inventory]
    Order -. evento futuro .-> Payment[payment]

    subgraph Application[Aplicação Spring Boot]
        Order
        Inventory
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
| Spring Data JDBC | Acesso relacional futuro |
| PostgreSQL | Banco de dados |
| Spring Modulith JDBC | Registro persistente das publicações de eventos |
| Springdoc OpenAPI | 3.1.0, documentação e Swagger UI |
| Maven | Build e gerenciamento de dependências |
| Docker Compose | PostgreSQL para o ambiente local |

## Estrutura de pacotes

```text
src/main/java/com/market
├── OrdermodApplication.java
├── order
│   ├── OrderCreatedEvent.java
│   └── internal
│       ├── application
│       │   ├── CreateOrderCommand.java
│       │   └── OrderService.java
│       ├── domain
│       ├── infrastructure
│       └── web
│           ├── CreateOrderRequest.java
│           ├── OrderController.java
│           └── OrderHttpApi.java
├── inventory
│   └── internal
│       ├── application
│       │   ├── InventoryOrderCreatedListener.java
│       │   └── InventoryService.java
│       ├── domain
│       ├── infrastructure
│       └── web
└── payment
    └── internal
        ├── application
        ├── domain
        ├── infrastructure
        └── web
```

Os pacotes ainda vazios possuem `package-info.java` para documentar sua finalidade e permitir que a estrutura seja versionada.

## Fronteiras dos módulos

O pacote raiz de cada módulo representa sua API Java pública. Os subpacotes representam detalhes internos.

| Localização | Finalidade | Pode ser acessado por outro módulo? |
| --- | --- | --- |
| `com.market.order` | Contratos públicos do módulo `order` | Sim |
| `com.market.order.internal.*` | Implementação do módulo `order` | Não |
| `com.market.inventory` | Contratos públicos do módulo `inventory` | Sim |
| `com.market.inventory.internal.*` | Implementação do módulo `inventory` | Não |
| `com.market.payment` | Contratos públicos do módulo `payment` | Sim |
| `com.market.payment.internal.*` | Implementação do módulo `payment` | Não |

Atualmente, `OrderCreatedEvent` é o único contrato público entre módulos. Ele fica em `com.market.order` porque `inventory` precisa importá-lo para registrar seu listener.

O nome `internal` expressa uma regra arquitetural do Spring Modulith, mas não é um modificador de acesso do Java. Como vários tipos são `public`, o compilador ainda pode aceitar um import indevido. Para fiscalizar a regra automaticamente, o projeto deverá adicionar um teste com `ApplicationModules.of(OrdermodApplication.class).verify()`.

## Responsabilidades internas

Cada módulo está preparado para usar uma separação interna por responsabilidade:

- `web`: contrato HTTP, controllers e DTOs de entrada ou saída.
- `application`: casos de uso, comandos, listeners e coordenação da aplicação.
- `domain`: agregados, entidades, objetos de valor e regras de negócio.
- `infrastructure`: persistência e integrações técnicas.

Essa separação é interna ao módulo. Uma classe de `inventory` não deve acessar nenhuma dessas camadas internas de `order`.

## Fluxo de criação de pedido

```mermaid
sequenceDiagram
    autonumber
    actor Client as Cliente
    participant API as OrderHttpApi
    participant Controller as OrderController
    participant Service as OrderService
    participant Events as ApplicationEventPublisher
    participant Listener as InventoryOrderCreatedListener
    participant Inventory as InventoryService

    Client->>API: POST /api/v1/order
    API->>Controller: CreateOrderRequest validado
    Controller->>Controller: converte request em CreateOrderCommand
    Controller->>Service: createOrder(command)
    Service->>Service: gera orderId e createdAt
    Service->>Events: publica OrderCreatedEvent
    Service-->>Controller: processamento iniciado
    Controller-->>Client: 201 Pedido recebido com sucesso
    Events-->>Listener: evento após o commit
    Listener->>Listener: converte itens em ItemReservation
    Listener->>Inventory: reserveItems(orderId, items)
```

### Separação dos modelos

O fluxo não passa o DTO HTTP diretamente para o serviço:

```text
CreateOrderRequest
        ↓ mapeamento no controller
CreateOrderCommand
        ↓ processamento no serviço
OrderCreatedEvent
        ↓ mapeamento no listener
InventoryService.ItemReservation
```

Essa separação evita que mudanças no JSON ou no evento alterem diretamente os modelos internos das camadas de aplicação.

## API HTTP

O contrato HTTP está definido em `OrderHttpApi`. O `OrderController` implementa essa interface e contém apenas a adaptação para o caso de uso.

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

O `@Valid` no método HTTP ativa a validação do request. Outro `@Valid` na lista ativa a validação em cascata de cada item.

## OpenAPI e Swagger UI

As anotações REST e OpenAPI ficam concentradas em `OrderHttpApi`. O controller implementa o contrato sem duplicar essas anotações.

Com a aplicação em execução nas configurações padrão:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

## Eventos entre módulos

### Evento publicado

`OrderService` publica `OrderCreatedEvent` através do `ApplicationEventPublisher`. O evento contém:

| Campo | Tipo | Finalidade |
| --- | --- | --- |
| `orderId` | `UUID` | Identificador gerado pelo servidor |
| `createdAt` | `Instant` | Instante da criação gerado pelo servidor |
| `customerId` | `UUID` | Cliente do pedido |
| `paymentMethod` | `String` | Forma de pagamento recebida |
| `items` | `List<Item>` | Produtos e respectivas quantidades |

A lista do evento é copiada com `List.copyOf`, evitando alteração posterior da coleção publicada.

### Consumo no inventory

`InventoryOrderCreatedListener` utiliza `@ApplicationModuleListener` para receber `OrderCreatedEvent` após a confirmação da transação de origem. O listener não implementa a regra de estoque; ele adapta o evento para `InventoryService.ItemReservation` e delega para `InventoryService.reserveItems`.

`InventoryService` atualmente:

- valida o identificador do pedido;
- valida a existência dos itens;
- valida produto e quantidade de cada reserva;
- registra no log a intenção de reserva.

Ainda não existe alteração ou persistência real de estoque.

### Registro persistente de eventos

O projeto utiliza `spring-modulith-starter-jdbc`. A propriedade abaixo está habilitada no ambiente atual:

```yaml
spring:
  modulith:
    events:
      jdbc:
        schema-initialization:
          enabled: true
```

Ela permite ao Spring Modulith criar a estrutura JDBC utilizada para registrar publicações destinadas a listeners transacionais. A criação automática é conveniente em desenvolvimento. Em produção, essa estrutura deve ser criada por uma migration versionada.

## Persistência e ambiente local

O `compose.yaml` declara um PostgreSQL para desenvolvimento. O projeto possui Spring Data JDBC e o driver PostgreSQL, mas ainda não possui:

- agregado ou tabela de pedidos;
- repositório de pedidos;
- modelo ou tabela de estoque;
- repositório de estoque;
- migrations de banco de dados.

## Testes atuais

| Teste | Cobertura atual |
| --- | --- |
| `OrdermodApplicationTests` | Carregamento do contexto Spring |
| `OrderServiceTest` | Publicação e conteúdo do `OrderCreatedEvent` |

O teste de `OrderService` usa um capturador simples de eventos e não inicializa o contexto Spring nem o banco de dados.

## Decisões arquiteturais atuais

1. **Monólito modular:** os módulos permanecem no mesmo deploy, mas possuem fronteiras lógicas.
2. **Organização por negócio:** `order`, `inventory` e `payment` representam capacidades, não camadas globais.
3. **Encapsulamento:** somente tipos no pacote raiz do módulo formam a API Java pública.
4. **Integração por eventos:** `order` não chama diretamente um serviço interno de `inventory`.
5. **DTO separado do comando:** o contrato HTTP não chega à camada de aplicação.
6. **Evento separado do comando:** outros módulos recebem um fato público, não um tipo interno de `order`.
7. **Listener separado do service:** o listener adapta a mensagem; o service representa o caso de uso de estoque.
8. **Contrato HTTP separado:** `OrderHttpApi` concentra REST e OpenAPI; `OrderController` implementa o comportamento.
9. **Dados gerados pelo servidor:** `orderId` e `createdAt` não são aceitos do cliente.

## Limitações e próximos passos

O estado atual é um esqueleto arquitetural funcional, não um fluxo de compra completo. Os próximos passos recomendados são:

1. Criar o agregado `Order` e seus itens no pacote de domínio.
2. Persistir o pedido antes de publicar `OrderCreatedEvent`, usando a mesma transação.
3. Criar migrations para pedidos, estoque e registro de eventos.
4. Implementar reserva e persistência real no módulo `inventory`.
5. Implementar o módulo `payment` e seu listener para o evento do pedido.
6. Definir eventos de sucesso e falha, como estoque reservado ou estoque indisponível.
7. Retornar o `orderId` ou o cabeçalho `Location` na resposta `201 Created`.
8. Padronizar respostas de erro com `@RestControllerAdvice`.
9. Adicionar teste de fronteiras com `ApplicationModules.verify()`.
10. Adicionar testes HTTP e testes de integração dos listeners.
11. Fixar uma versão específica da imagem PostgreSQL em vez de usar `latest`.

Uma inconsistência conhecida deve ser tratada antes de produção: o sistema atualmente publica um evento chamado `OrderCreatedEvent`, mas ainda não persiste um pedido. Quando a persistência for implementada, o evento deverá ser publicado somente após a criação bem-sucedida do agregado, dentro da mesma transação.
