# ORDER-02 — Consultar um pedido por identificador

## Situação

Aprovada para implementação.

## Contexto

A `ORDER-01` passou a criar pedidos com fotografia comercial e estado inicial, mas a API ainda não permite consultar um pedido já persistido. Atualmente, os valores calculados aparecem somente na resposta imediata do `POST`.

Esta tarefa adiciona uma operação de leitura para recuperar um único pedido pelo seu `orderId`, sem alterar o pedido e sem produzir eventos.

## História

Como consumidor da API de pedidos, quero consultar um pedido pelo identificador, para visualizar os dados registrados, sua fotografia comercial e seu estado atual.

## Objetivo

Disponibilizar uma operação HTTP que receba um `orderId`, procure o agregado persistido e devolva suas informações completas em JSON.

## Decisões de escopo

### Incluído

- consulta de um pedido por UUID;
- retorno dos dados gerais do pedido;
- retorno da fotografia comercial dos itens;
- retorno do estado atual;
- resposta `200 OK` quando o pedido existir;
- resposta `404 Not Found` quando o pedido não existir;
- validação de identificador malformado;
- evolução da porta de persistência;
- testes de domínio afetados, aplicação, persistência e HTTP;
- atualização da documentação da API e da arquitetura.

### Não incluído

- listagem ou paginação de pedidos;
- busca por cliente, status ou período;
- alteração ou cancelamento do pedido;
- histórico de transições;
- dados das publicações de eventos;
- informações internas do estoque, pagamento ou notificação;
- autenticação e autorização;
- implementação de um modelo de leitura separado ou CQRS;
- inclusão de dados que ainda não pertencem ao agregado `Order`.

## Contrato HTTP proposto

### Requisição

```http
GET /api/v1/order/{orderId}
Accept: application/json
```

Exemplo:

```http
GET /api/v1/order/3f9159ff-44c8-4195-85d2-1586c121e631
```

O caminho singular `/api/v1/order` será mantido para ficar consistente com o endpoint de criação existente. Uma eventual padronização para `/api/v1/orders` deve ser tratada separadamente para não misturar evolução funcional com mudança de contrato.

### Resposta de sucesso

Status: `200 OK`.

```json
{
  "orderId": "3f9159ff-44c8-4195-85d2-1586c121e631",
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "paymentMethod": "CREDIT_CARD",
  "status": "AGUARDANDO_ESTOQUE",
  "totalAmount": 25.00,
  "currency": "BRL",
  "createdAt": "2026-09-02T20:03:13.356764465Z",
  "items": [
    {
      "itemId": "384414fd-8b64-44df-8678-304f108f87f7",
      "productId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "quantity": 2,
      "unitPrice": 10.50,
      "subtotal": 21.00
    },
    {
      "itemId": "145df3f2-5904-4af0-adbb-4d07dbe40f0f",
      "productId": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
      "quantity": 1,
      "unitPrice": 4.00,
      "subtotal": 4.00
    }
  ]
}
```

O controle de versão da persistência não será exposto, pois atualmente é um detalhe técnico e não representa informação de negócio para o cliente da API.

### Pedido inexistente

Status: `404 Not Found`.

Resposta no formato `application/problem+json`:

```json
{
  "status": 404,
  "detail": "Pedido não encontrado: 6f1ef635-8076-49d4-93e3-2f1f8a376f61"
}
```

O corpo poderá conter também os campos padrão gerados por `ProblemDetail`, como `type`, `title` e `instance`.

### Identificador malformado

Quando o valor do caminho não puder ser convertido para UUID, a API responderá `400 Bad Request`. A aplicação não deverá ser chamada.

## Fluxo proposto

```text
GET /api/v1/order/{orderId}
    → Spring converte o parâmetro para UUID
    → OrderController cria GetOrderQuery
    → GetOrderUseCase recebe a consulta
    → GetOrderService procura o pedido por OrderId
    → OrderRepository consulta a persistência
    → OrderPersistenceAdapter reconstitui o agregado
    → serviço converte o agregado para GetOrderResult
    → controller converte o resultado para GetOrderResponse
    → API devolve 200 com o pedido
```

Se o repositório não encontrar o pedido:

```text
OrderRepository retorna Optional.empty()
    → GetOrderService sinaliza OrderNotFoundException
    → adaptador HTTP converte para 404 Not Found
```

## Modelo de aplicação proposto

### Porta de entrada

Criar `GetOrderUseCase` com uma operação conceitual:

```java
GetOrderResult getOrder(GetOrderQuery query);
```

### Consulta

`GetOrderQuery` transporta somente o `UUID orderId` e rejeita valor nulo.

### Resultado

`GetOrderResult` contém:

- `orderId`;
- `customerId`;
- `paymentMethod`;
- `status`;
- `totalAmount`;
- `currency`;
- `createdAt`;
- itens com `itemId`, `productId`, `quantity`, `unitPrice` e `subtotal`.

O resultado da aplicação usa tipos simples e não expõe diretamente o agregado para a camada HTTP.

### Serviço

`GetOrderService` deve:

1. validar a presença da consulta;
2. criar `OrderId`;
3. consultar a porta `OrderRepository`;
4. sinalizar pedido inexistente de forma distinguível;
5. mapear o agregado encontrado para `GetOrderResult`.

O serviço não recalcula, corrige ou modifica valores durante a consulta.

## Persistência

A porta `OrderRepository` será ampliada com uma consulta conceitual:

```java
Optional<Order> findById(OrderId orderId);
```

`OrderPersistenceAdapter` delegará ao `SpringDataOrderRepository` e utilizará `OrderPersistenceMapper.toDomain(...)` quando encontrar o registro.

Esta tarefa não exige nova migration: todas as colunas necessárias já existem após a `ORDER-01`.

## Regras de negócio e consistência

1. A consulta não altera o estado do pedido.
2. A consulta não registra nem publica Domain Events.
3. Os valores devolvidos devem ser exatamente os valores persistidos.
4. A ordem dos itens deve ser preservada por `item_index`.
5. O pedido deve ser reconstituído pelas factories existentes, preservando as invariantes do agregado.
6. `subtotal` e `totalAmount` não devem ser recalculados a partir de preços atuais.
7. Dados internos de persistência, como `version` e `item_index`, não serão expostos.
8. A ausência do pedido é um resultado de consulta e deve produzir `404`, não `500` ou `400`.
9. Um UUID malformado é erro da requisição e deve produzir `400`.

## Tratamento de pedidos legados

Os três pedidos sem fotografia comercial foram removidos da base local após a `ORDER-01`. Portanto, a implementação pode assumir que os pedidos consultáveis possuem estado e fotografia completos.

A migration `V2` ainda admite registros legados inteiramente nulos para preservar bases que não passaram pela limpeza manual. Caso essa situação seja relevante em outro ambiente, a consulta não deverá inventar valores. A política para esses registros deverá ser definida antes de considerá-los consultáveis.

## Impacto esperado por camada

### Domínio

- nenhuma nova regra de negócio;
- uso de `Order.reconstitute(...)` e `OrderItem.reconstitute(...)` na leitura;
- nenhum evento novo.

### Aplicação

- criar `GetOrderQuery`;
- criar `GetOrderUseCase`;
- criar `GetOrderResult`;
- criar `GetOrderService`;
- criar uma exceção específica para pedido inexistente;
- ampliar `OrderRepository` com `findById`.

### Adaptador HTTP

- adicionar `GET /api/v1/order/{orderId}` a `OrderHttpApi`;
- criar `GetOrderResponse`;
- mapear resultado para JSON;
- mapear pedido inexistente para `404`;
- manter violações de entrada como `400`.

### Adaptador JDBC

- implementar a nova operação da porta;
- reutilizar `SpringDataOrderRepository.findById(...)`;
- reutilizar `OrderPersistenceMapper`.

### Outros módulos

- nenhum impacto esperado em `Inventory`, `Payment` ou `Notification`;
- nenhum evento será publicado pela consulta.

## Critérios de aceitação

### Cenário 1 — Pedido existente

Dado um pedido persistido, quando a API é consultada com seu `orderId`, então responde `200 OK` e devolve todos os dados definidos no contrato.

### Cenário 2 — Fotografia comercial preservada

Dado um pedido com dois itens, quando ele é consultado, então preços unitários, subtotais, total e moeda coincidem exatamente com os valores persistidos.

### Cenário 3 — Estado preservado

Dado um pedido em `AGUARDANDO_ESTOQUE`, quando ele é consultado, então a resposta contém esse mesmo estado.

### Cenário 4 — Pedido inexistente

Dado um UUID válido que não pertence a nenhum pedido, quando a API é consultada, então responde `404 Not Found` em `application/problem+json`.

### Cenário 5 — Identificador inválido

Dado um valor que não representa UUID, quando a API é consultada, então responde `400 Bad Request` sem acessar o repositório.

### Cenário 6 — Consulta sem efeitos colaterais

Dado um pedido existente, quando ele é consultado, então seu estado e sua versão não mudam, nenhum registro é criado e nenhum evento é publicado.

### Cenário 7 — Ordem dos itens

Dado um pedido com mais de um item, quando ele é consultado, então os itens aparecem na mesma ordem persistida.

## Estratégia de testes

- teste unitário de `GetOrderQuery` para `orderId` nulo;
- teste unitário de `GetOrderService` para mapeamento completo;
- teste unitário de `GetOrderService` para pedido inexistente;
- teste garantindo ausência de publicação de eventos;
- teste do adaptador JDBC para pedido existente e inexistente;
- teste do mapper garantindo reconstituição sem Domain Events;
- teste do controller para conversão entre resultado e resposta;
- teste HTTP para `200` e conteúdo completo do JSON;
- teste HTTP para `404` com `ProblemDetail`;
- teste HTTP para UUID malformado;
- execução dos testes de arquitetura hexagonal e modularidade;
- execução da suíte completa.

## Definição de pronto

- todos os critérios de aceitação automatizáveis possuem testes;
- a suíte completa passa;
- a consulta retorna todos os campos especificados;
- pedido inexistente retorna `404`;
- identificador malformado retorna `400`;
- nenhum evento é produzido pela leitura;
- nenhum detalhe JDBC ou objeto de domínio é exposto na API;
- não existe alteração de esquema nesta tarefa;
- documentação arquitetural e OpenAPI refletem a nova operação.

## Dependências e decisões pendentes

Não existe dependência funcional de outro módulo.

Antes da implementação, deve ser confirmada apenas a política para eventuais registros legados em outros ambientes. Na base local atual, não há pedidos legados.
