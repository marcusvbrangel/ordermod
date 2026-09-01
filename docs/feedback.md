# Feedback de arquitetura e organização

Data da avaliação: 31/08/2026

Atualização arquitetural: 01/09/2026

## Avaliação geral

A arquitetura está sólida e coerente para um monólito modular. As decisões principais combinam bem entre si e o projeto não está excessivamente complicado.

O fluxo atual está bem definido:

```text
HTTP
    → Controller
    → CreateOrderUseCase
    → CreateOrderService
    → portas de saída
    → adaptadores JDBC e de eventos
    → PostgreSQL / OrderCreatedEvent
    → Inventory/Notification
```

## Pontos fortes

- Organização por negócio: `order`, `inventory`, `payment` e `notification`.
- Contratos públicos fora de `internal`, como `OrderCreatedEvent`.
- Separação entre DTO HTTP, comando, domínio e evento.
- Persistência do agregado `Order` com seus itens.
- Flyway controlando a estrutura do banco.
- Pedido e publicação persistente participando da mesma transação.
- Comunicação entre módulos por eventos.
- Identificadores explícitos para os listeners.
- Documentação arquitetural acompanhando a implementação.
- Regras básicas protegidas pelo próprio domínio.
- Violações de invariantes representadas por `OrderDomainException` em `domain.exception`.
- Domínio sem dependências de Spring Data JDBC.
- Portas de entrada e saída explícitas.
- Modelos de domínio e persistência separados por um mapper.
- Controllers dependentes do caso de uso, não da implementação concreta.
- Regras hexagonais fiscalizadas por quatro testes ArchUnit.
- PostgreSQL 18.6 isolado para testes através de Testcontainers.
- Persistência, publicação durável e rollback protegidos por testes de integração.
- Contrato HTTP protegido com MockMvc standalone.

## Evolução para arquitetura hexagonal

A separação que antes era uma possibilidade foi implementada dentro do módulo `order`:

```text
adapter.in.web
    → application.port.in
    → application.service
    → domain.model
    → application.port.out
    ← adapter.out.persistence.jdbc
    ← adapter.out.event
```

Agora:

- `CreateOrderUseCase` representa a porta de entrada;
- `OrderRepository` e `OrderEventPublisher` representam portas de saída;
- `CreateOrderService` orquestra o caso de uso;
- `Order` e `OrderItem` formam um domínio puro;
- entidades JDBC e o `CrudRepository` ficam no adaptador de persistência;
- `OrderPersistenceMapper` traduz domínio e representação relacional;
- `SpringOrderEventPublisher` isola a integração com os eventos do Spring.

Essa organização adiciona mais tipos, mas torna explícita a direção das dependências e permite testar o núcleo através das portas. A regra a preservar é que o domínio e a aplicação não passem a depender de controllers, entidades JDBC ou implementações técnicas.

O módulo Spring Modulith continua sendo `com.market.order`; a arquitetura hexagonal organiza o interior do módulo e não cria um novo módulo nem um microsserviço.

## Plano priorizado

### Prioridade alta

- [x] Criar teste arquitetural com `ApplicationModules.verify()`.
- [x] Adicionar regras ArchUnit para a arquitetura hexagonal interna.
- [x] Separar domínio, portas e adaptadores no módulo `order`.
- [x] Manter `OrderCreatedEvent` como API pública do módulo.
- [x] Configurar um PostgreSQL isolado para os testes.
- [x] Criar teste de integração da persistência de `Order` e `OrderItem`.
- [x] Testar rollback do pedido e da publicação persistente.
- [x] Testar a integração do evento com os módulos consumidores.
- [x] Testar o contrato HTTP de sucesso e validação.

### Prioridade média

- [ ] Retornar `orderId` no POST de criação.
- [ ] Retornar o cabeçalho HTTP `Location` junto com `201 Created`.
- [ ] Criar um status para o pedido, como `CREATED`, `CONFIRMED` e `CANCELLED`.
- [ ] Tornar os listeners idempotentes para suportar reprocessamento.
- [ ] Persistir efetivamente a reserva no módulo `inventory`.

### Prioridade baixa

- [ ] Padronizar o formato dos IDs de todos os listeners.
- [ ] Fixar uma versão específica da imagem PostgreSQL.
- [ ] Externalizar as credenciais do PostgreSQL.
- [x] Atualizar a documentação arquitetural com o módulo `notification`.
- [ ] Corrigir a mensagem de log de `NotificationOrderCreatedListener`, que ainda menciona itens de estoque.

## Riscos atuais

Os principais riscos técnicos da refatoração foram reduzidos pela suíte automatizada:

- `ApplicationModules.verify()` e quatro regras ArchUnit protegem as fronteiras;
- Testcontainers valida a persistência contra PostgreSQL real e a migration V1;
- os testes de integração comprovam publicação durável e rollback conjunto;
- MockMvc protege o contrato HTTP atual.

Permanecem riscos de evolução do negócio:

- os listeners ainda não possuem garantia de idempotência;
- `InventoryService.reserveItems` ainda não persiste uma reserva real.

Também permanece necessária uma estratégia de migration para a tabela de publicações em produção.

## Conclusão

A refatoração tornou as fronteiras internas de `order` mais explícitas sem alterar sua fronteira pública no Spring Modulith. O projeto agora possui uma base hexagonal coerente e verificada: domínio puro, aplicação orientada a portas e detalhes técnicos nas bordas. A execução final de `./mvnw clean test` com Java 25 concluiu 27 testes sem falhas, erros ou testes ignorados. A próxima evolução pode concentrar-se em idempotência, persistência de estoque e regras de pagamento.
