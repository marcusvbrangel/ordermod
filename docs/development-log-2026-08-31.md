# Registro de desenvolvimento — 31/08/2026

> Este arquivo preserva o estado do projeto em 31/08/2026. A organização de pacotes e persistência descrita aqui foi posteriormente substituída pela arquitetura hexagonal registrada em [01/09/2026](development-log-2026-09-01.md).

## Resultado do dia

O fluxo de criação de pedidos deixou de apenas publicar um evento e passou a persistir o agregado completo no PostgreSQL antes da publicação. A gravação do pedido, dos itens e da publicação do evento ocorre sob a transação iniciada por `OrderService.createOrder`.

```text
POST /api/v1/order
    → CreateOrderRequest
    → CreateOrderCommand
    → Order + OrderItem
    → OrderRepository.save
    → orders.orders + orders.order_items
    → OrderCreatedEvent
    → event_publication
    → InventoryOrderCreatedListener
```

## PostgreSQL e Docker Compose

- Foi identificado que `ports: - "5432"` não garantia o uso de `localhost:5432`.
- O container deste projeto também encontrou a porta `5432` ocupada pelo container `market-postgres`, pertencente a outro projeto.
- O PostgreSQL do `ordermod` passou a ser publicado em `5433:5432`.
- A conexão do DBeaver usa `localhost:5433`, banco `ordermod`, usuário `myuser` e senha `1234`.
- O suporte `spring-boot-docker-compose` detecta automaticamente a porta publicada para montar a conexão JDBC da aplicação.

## Flyway e estrutura relacional

Foram adicionados `spring-boot-starter-flyway` e `flyway-database-postgresql`. No Spring Boot 4, o starter fornece a autoconfiguração necessária para executar migrations na inicialização.

Como a tabela `public.event_publication` já existia, o Flyway foi configurado com:

```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0
```

A migration `V1__create_order_tables.sql` foi aplicada com sucesso e criou o esquema `orders`, as tabelas `orders.orders` e `orders.order_items`, suas chaves, restrições e índice. O histórico contém o baseline `0` e a migration `1`, ambos concluídos com sucesso.

## Modelo de domínio e Spring Data JDBC

Foram criados:

- `Order`, raiz do agregado, mapeado para `orders.orders`;
- `OrderItem`, componente do agregado, mapeado para `orders.order_items`;
- `OrderRepository`, um `CrudRepository<Order, UUID>` interno ao módulo.

O modelo é imutável e valida identificadores obrigatórios, forma de pagamento não vazia, existência de itens e quantidade positiva. A lista de itens é copiada defensivamente. `@MappedCollection` configura a relação por `order_id` e a ordem da lista por `item_index`.

## Persistência e publicação do evento

`OrderService.createOrder` agora:

1. converte o comando em `Order` e `OrderItem`;
2. gera UUIDs e o instante de criação;
3. chama `OrderRepository.save`;
4. chama o método privado `publishOrderCreatedEvent`;
5. publica o evento usando os dados do agregado retornado pelo repositório.

A extração de `publishOrderCreatedEvent` reduziu a responsabilidade visual do método principal sem alterar o limite da transação.

O listener de estoque passou a usar o identificador legível:

```java
@ApplicationModuleListener(id = "inventory.reserve-items-on-order-created")
```

`@ApplicationModuleListener` já inclui o comportamento equivalente a listener transacional, execução assíncrona e nova transação; não é necessário repetir essas anotações.

## Validações realizadas

- Compilação executada com Java 25.
- Spring Data JDBC encontrou um repositório.
- Cinco testes unitários específicos passaram.
- Uma chamada real ao endpoint retornou `Pedido recebido com sucesso`.
- O pedido de validação foi gravado com dois itens e versão `0`.
- A publicação correspondente terminou com `status = COMPLETED`.
- O listener `inventory.reserve-items-on-order-created` processou o evento.

Pedido usado na validação:

```text
orderId: f92db842-eac6-4c03-be15-e14e07fbcccc
customerId: 9e1e5a0a-6d1a-4a9f-a73c-4bcc63cd55f0
```

## Pendências conhecidas

- O terminal estava configurado com Java 21, mas o projeto compila para Java 25. Foi usado o JDK `25.0.3-tem` durante as validações.
- `OrdermodApplicationTests` ainda não possui uma fonte de dados isolada para testes.
- `InventoryService.reserveItems` apenas registra a intenção no log; ainda não persiste reservas.
- O módulo `payment` ainda não possui comportamento.
- A resposta HTTP ainda não retorna o identificador do pedido.
