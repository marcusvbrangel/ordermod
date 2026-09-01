# Feedback de arquitetura e organização

Data da avaliação: 31/08/2026

## Avaliação geral

A arquitetura está sólida e coerente para um monólito modular. As decisões principais combinam bem entre si e o projeto não está excessivamente complicado.

O fluxo atual está bem definido:

```text
HTTP
    → Controller
    → Command
    → OrderService
    → OrderRepository
    → PostgreSQL
    → OrderCreatedEvent
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

## Decisão arquitetural a manter consciente

Atualmente:

```text
OrderService
    → importa OrderRepository de infrastructure

Order
    → possui anotações do Spring Data JDBC
```

Isso é aceitável para uma arquitetura pragmática com Spring Data JDBC, mas não representa uma arquitetura hexagonal estrita.

Em uma arquitetura estritamente hexagonal:

- a interface `OrderRepository` ficaria em `domain` ou `application`;
- a infraestrutura implementaria essa interface;
- o domínio não teria anotações de persistência;
- poderia existir um modelo relacional separado.

Para o tamanho atual do projeto, a recomendação é manter a solução pragmática. Separar modelos e criar adaptadores adicionais agora provavelmente adicionaria mais código do que benefício.

## Plano priorizado

### Prioridade alta

- [x] Criar teste arquitetural com `ApplicationModules.verify()`.
- [ ] Configurar um PostgreSQL isolado para os testes.
- [ ] Criar teste de integração da persistência de `Order` e `OrderItem`.
- [ ] Testar rollback do pedido e da publicação persistente.
- [ ] Testar a integração do evento com os módulos consumidores.

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
- [ ] Atualizar a documentação arquitetural com o módulo `notification`.
- [ ] Corrigir a mensagem de log de `NotificationOrderCreatedListener`, que ainda menciona itens de estoque.

## Riscos atuais

O maior risco não está na organização das pastas, mas na cobertura de integração:

- o teste geral de contexto ainda não possui banco próprio;
- os listeners ainda não possuem garantia de idempotência;
- `InventoryService.reserveItems` ainda não persiste uma reserva real.

Essas proteções devem ser implementadas antes que mais regras de negócio e módulos sejam adicionados.

## Conclusão

A fundação arquitetural está boa, o fluxo transacional está correto e as fronteiras estão claras. A próxima evolução deve priorizar testes, idempotência e regras de negócio, sem introduzir novas abstrações ou reorganizações de pastas sem uma necessidade concreta.
