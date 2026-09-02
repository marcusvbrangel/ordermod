# ORDER-03 — Cancelar um pedido antes de reserva e pagamento

## Situação

Aprovada para implementação.

## Contexto

O módulo `order` já registra pedidos com fotografia comercial, total, moeda e estado inicial (`AGUARDANDO_ESTOQUE`). Ainda não existe um fluxo completo de reservas de estoque nem de pagamentos no projeto. Em cenários em que nenhum pedido teve reserva de estoque realizada e nenhum pagamento foi autorizado ou capturado, é necessária uma operação simples de cancelamento que permita ao cliente ou a operação administrativa encerrar a intenção de compra.

Esta tarefa especifica uma operação de cancelamento limitada ao caso em que não existiram efeitos externos (nenhuma reserva, nenhuma autorização/captura). Ela evita tocar nos fluxos de compensação que envolvem reserva, captura ou estorno, que deverão ser tratados por tarefas posteriores.

## História

Como consumidor da API, quero cancelar um pedido que ainda não passou pela reserva de estoque nem por pagamento, para que a intenção de compra seja encerrada sem efeitos financeiros ou operacionais adicionais.

## Objetivo

Adicionar uma operação HTTP que receba um `orderId`, valide que o pedido não teve reserva nem pagamento, marque o pedido como cancelado (`CANCELADO`) e persista a alteração, devolvendo o estado resultante em JSON.

## Decisões de escopo

### Incluído

- cancelamento de pedido identificado por UUID;
- requisito: nenhum efeito externo (sem reserva de estoque e sem pagamento autorizado/capturado);
- atualização do estado do pedido para `CANCELADO` e persistência dessa alteração;
- resposta `200 OK` com corpo JSON contendo identificador e novo estado;
- resposta `404 Not Found` quando o pedido não existir;
- resposta `400 Bad Request` para identificador malformado;
- resposta `409 Conflict` quando o pedido não estiver em estado cancelável (por exemplo, já passou por reserva ou pagamento ou já estiver confirmado);
- testes unitários e de integração que comprovem os critérios de aceitação para o escopo limitado.

### Não incluído

- cancelamento de pedidos que já tiveram reserva de estoque verdadeira;
- cancelamento de pedidos com pagamento autorizado ou capturado (essas situações exigem cancelamento com compensação financeira — estorno ou cancelamento de autorização — tratadas por tarefas futuras);
- automatização de liberações de reservas ou solicitações de estorno;
- orquestração de Sagas ou gerenciador de processos longos.

## Contrato HTTP proposto

### Requisição

POST /api/v1/order/{orderId}/cancel
Accept: application/json

Exemplo:

```
POST /api/v1/order/3f9159ff-44c8-4195-85d2-1586c121e631/cancel
```

Escolheu-se POST em vez de DELETE para deixar explícita a operação sem ambiguidade sobre idempotência e para permitir futura extensão do payload (motivo, solicitante) sem mudar o verbo.

### Resposta de sucesso

Status: `200 OK`

Corpo exemplo:

```json
{
  "orderId": "3f9159ff-44c8-4195-85d2-1586c121e631",
  "status": "CANCELADO",
  "cancelledAt": "2026-09-02T20:03:13.356764465Z"
}
```

Notas:
- `cancelledAt` é opcional no domínio atual; pode ser preenchido pela aplicação para auditabilidade. Se preferir não persistir timestamp no agregado, a aplicação pode devolver apenas `orderId` e `status`.
- A operação deve ser idempotente: se o pedido já estiver `CANCELADO`, responder `200 OK` com o mesmo corpo (ou um corpo que indique que já estava cancelado).

### Pedido inexistente

Status: `404 Not Found`.

Resposta no formato `application/problem+json` com detalhe legível, por exemplo:

```json
{
  "status": 404,
  "detail": "Pedido não encontrado: 6f1ef635-8076-49d4-93e3-2f1f8a376f61"
}
```

### Identificador malformado

Quando o valor do caminho não puder ser convertido para UUID, a API responderá `400 Bad Request`. A aplicação não deverá ser chamada.

### Pedido em estado não cancelável

Status: `409 Conflict`.

Resposta no formato `application/problem+json` com detalhe, por exemplo:

```json
{
  "status": 409,
  "detail": "Pedido não pode ser cancelado no estado: AGUARDANDO_CAPTURA"
}
```

Use `409` para indicar que a requisição é sintaticamente válida, mas inconsistente com o estado atual do recurso.

## Fluxo proposto

```
POST /api/v1/order/{orderId}/cancel
    → Spring converte o parâmetro para UUID
    → OrderController cria CancelOrderCommand
    → CancelOrderUseCase recebe o comando
    → CancelOrderService procura o pedido por OrderId
    → Serviço verifica que não existe reserva e não existe pagamento
    → Serviço altera o estado para CANCELADO e persiste via OrderRepository.save
    → API devolve 200 com orderId e novo estado
```

Se o pedido não existir:

```
OrderRepository retorna Optional.empty()
    → CancelOrderService sinaliza OrderNotFoundException
    → adaptador HTTP converte para 404 Not Found
```

Se o pedido não estiver em estado que permita cancelamento simples:

```
CancelOrderService lança OrderNotCancellableException
    → adaptador HTTP converte para 409 Conflict
```

## Modelo de aplicação proposto

### Porta de entrada

Criar `CancelOrderUseCase` com operação conceitual:

```java
CancelOrderResult cancelOrder(CancelOrderCommand command);
```

### Comando

`CancelOrderCommand` transporta somente o `UUID orderId` e rejeita valor nulo. Pode-se estender futuramente com campos como `reason` ou `requestedBy`.

### Resultado

`CancelOrderResult` contém ao menos:

- `orderId`;
- `status` (deve ser `CANCELADO`);
- `cancelledAt` (opcional).

### Serviço

`CancelOrderService` deve:

1. validar a presença do comando;
2. criar `OrderId`;
3. consultar `OrderRepository` para reconstituir o pedido;
4. verificar que o pedido NÃO possui reserva de estoque nem pagamento autorizado/capturado (em bases locais atuais isso pode ser inferido do estado e/ou ausência de campos de reserva/pagamento);
5. validar que o estado atual permite alteração para `CANCELADO` (por exemplo, `AGUARDANDO_ESTOQUE` ou `AGUARDANDO_AUTORIZACAO` quando sem autorização);
6. sinalizar de forma distinguível quando o pedido não existir (`OrderNotFoundException`) ou não puder ser cancelado (`OrderNotCancellableException`);
7. mapear o agregado para `CancelOrderResult` após persistência.

O serviço não deve publicar eventos nesta tarefa (a decisão de publicar `OrderCanceledEvent` pode ser tomada em tarefa separada que cubra reservas/pagamentos e listeners).

## Persistência

Não é necessário criar nova migration: o campo `status` e demais colunas já existem após `ORDER-01`. A operação de cancelamento reutiliza `OrderPersistenceAdapter`:

- `OrderRepository` já expõe `save(Order order)` e `findById(OrderId orderId)` (esta última foi introduzida por ORDER-02);
- `OrderPersistenceAdapter` reconstituirá o agregado, a aplicação atualizará o estado via um novo `Order.reconstitute(...)`/factory ou através de um método novo no domínio (ex.: `Order.cancel()`); se optar por `Order.cancel()`, ele deve preservar invariantes e registrar nenhum Domain Event nesta tarefa.

### Nota sobre o domínio

Existem duas alternativas coerentes:

1. usar `Order.reconstitute(...)` e produzir uma nova instância `Order.reconstitute(...)` com `status = CANCELADO` (imutabilidade): não registra Domain Events;
2. ou adicionar um método de domínio `Order.cancel()` que valide e retorne uma nova instância ou modifique estado interno, dependendo da estratégia adotada pelo projeto.

A implementação deve preservar a invariant do agregado e não produzir Domain Events nesta tarefa.

## Regras de negócio e consistência

1. O cancelamento não altera nada além do estado do pedido e, opcionalmente, um timestamp de cancelamento.
2. O cancelamento não publica Domain Events neste escopo.
3. O cancelamento só é permitido se o pedido não tiver reserva de estoque verdadeira nem pagamento autorizado/capturado.
4. A operação é idempotente: cancelar um pedido já `CANCELADO` devolve `200 OK`.
5. A ausência do pedido é tratada como `404`, não `500`.
6. Um UUID malformado é erro da requisição (`400`) e não aciona chamadas ao repositório.

## Impacto esperado por camada

### Domínio

- pode ser necessário um pequeno ajuste para permitir reconstituir o pedido com `status = CANCELADO` ou adicionar `Order.cancel()`; nenhuma nova regra de negócio complexa é introduzida.

### Aplicação

- criar `CancelOrderCommand`;
- criar `CancelOrderUseCase`;
- criar `CancelOrderResult`;
- criar `CancelOrderService`;
- criar exceções específicas: `OrderNotFoundException` (existente) e `OrderNotCancellableException` (nova);
- validar ausência de efeitos externos antes de efetivar o cancelamento.

### Adaptador HTTP

- adicionar `POST /api/v1/order/{orderId}/cancel` a `OrderHttpApi` e `OrderController`;
- mapear exceções para `404`, `400` e `409`;
- criar `CancelOrderResponse` para a conversão do resultado para JSON.

### Adaptador JDBC

- reutilizar `OrderPersistenceAdapter.findById(...)` e `save(...)`;
- reutilizar `OrderPersistenceMapper` para garantir reconstituição correta sem gerar eventos de domínio.

### Outros módulos

- nenhum impacto esperado para `Inventory`, `Payment` ou `Notification` nesta tarefa limitada.

## Critérios de aceitação

### Cenário 1 — Pedido existente, sem reserva e sem pagamento

Dado um pedido persistido que não teve reserva de estoque nem pagamento, quando a API é chamada para cancelamento com seu `orderId`, então responde `200 OK` e o pedido passa a ter `status = CANCELADO` persistido.

### Cenário 2 — Idempotência

Dado um pedido já cancelado, quando a API de cancelamento é chamada novamente, então responde `200 OK` e o estado permanece `CANCELADO` (nenhuma falha é retornada).

### Cenário 3 — Pedido inexistente

Dado um UUID válido que não pertence a nenhum pedido, quando a API é chamada, então responde `404 Not Found` em `application/problem+json`.

### Cenário 4 — Identificador inválido

Dado um valor que não representa UUID, quando a API é chamada, então responde `400 Bad Request` sem acessar o repositório.

### Cenário 5 — Pedido em estado não cancelável

Dado um pedido que já possua reserva efetiva ou pagamento autorizado/capturado, quando a API de cancelamento é chamada, então responde `409 Conflict` e nenhum estado é alterado.

## Estratégia de testes

- teste unitário de `CancelOrderCommand` para `orderId` nulo;
- teste unitário de `CancelOrderService` para cancelamento bem-sucedido (mocando `OrderRepository`);
- teste unitário de `CancelOrderService` que lança `OrderNotCancellableException` quando o agregado não estiver em estado cancelável;
- teste do adaptador JDBC para garantir que `findById` + `save` efetivam a mudança de estado quando apropriado (pode usar Testcontainers quando quiser cobrir persistência real);
- teste do controller para conversão entre resultado e resposta JSON;
- teste HTTP para `200` e conteúdo do JSON;
- teste HTTP para `404` com `ProblemDetail` e `400` para UUID inválido;
- execução da suíte completa garantindo que nenhuma publicação de evento ocorra por esta operação.

## Definição de pronto

- todos os critérios de aceitação automatizáveis possuem testes;
- a suíte completa passa;
- o cancelamento só altera estado e persiste a mudança;
- pedido inexistente retorna `404` e identificador inválido retorna `400`;
- pedido em estado não cancelável retorna `409`;
- operação é idempotente;
- documentação arquitetural e OpenAPI refletem a nova operação.

## Dependências e decisões pendentes

- política de cancelamento quando já existirem reservas e/ou pagamentos (compensações financeiras e de estoque) deve ser especificada em tarefas subsequentes;
- definir se a aplicação deve persistir `cancelledAt` no agregado ou apenas retornar o timestamp na resposta sem adicioná-lo ao estado persistido; decisão tomada aqui influencia o mapper e a migration (se necessário).


---

Documentos relacionados:

- ORDER-01 — Fotografia comercial e estado inicial do pedido
- ORDER-02 — Consultar um pedido por identificador
- Processo lógico de uma operação de compra (docs/processo-logico-de-compra.md)