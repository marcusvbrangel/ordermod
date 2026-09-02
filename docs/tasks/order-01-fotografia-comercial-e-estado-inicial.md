# ORDER-01 — Fotografia comercial e estado inicial do pedido

## Situação

Implementada.

## Contexto

Antes da `ORDER-01`, o módulo `Order` registrava apenas o cliente, a forma de pagamento, os produtos, as quantidades e a data de criação. O pedido não preservava os preços acordados, o valor total, a moeda nem a etapa do seu ciclo de vida.

Sem essas informações, o sistema não consegue:

- informar a `Payment` quanto deve ser autorizado;
- verificar posteriormente se autorização e captura correspondem ao pedido;
- distinguir um pedido recém-criado de uma compra confirmada;
- reconstruir a fotografia comercial aceita no momento da compra.

Esta é a primeira evolução recomendada em [Processo lógico de uma operação de compra](../processo-logico-de-compra.md#12-ordem-lógica-sugerida-para-a-evolução-do-projeto).

## História

Como sistema de pedidos, quero receber provisoriamente o preço unitário informado na solicitação, preservar a fotografia comercial, calcular subtotais e total e registrar o estado inicial, para exercitar as regras do domínio e permitir a evolução do ciclo de vida do pedido.

## Objetivo

Ao criar um pedido, `Order` deve receber o preço unitário e a moeda pela interface pública, validar os valores, calcular a fotografia comercial dos itens, calcular o total e iniciar o pedido em `AGUARDANDO_ESTOQUE`.

Ao final desta tarefa, criar um pedido ainda não reserva estoque, não autoriza pagamento e não confirma a compra.

## Decisões de escopo

### Incluído

- preço unitário de cada item;
- subtotal de cada item;
- valor total do pedido;
- moeda única para todo o pedido;
- estado inicial `AGUARDANDO_ESTOQUE`;
- recebimento provisório de `unitPrice` e `currency` pela API;
- cálculo de `subtotal` e `totalAmount` exclusivamente pelo domínio;
- persistência dos novos dados;
- inclusão dos dados comerciais e do estado no evento de criação;
- resposta HTTP com a fotografia calculada e o estado inicial;
- testes de domínio, aplicação, persistência, evento e API afetados.

### Não incluído

- reserva ou baixa de estoque;
- integração com a Stripe;
- transições posteriores do estado do pedido;
- descontos, cupons, impostos e frete;
- alteração de preços de um pedido já criado;
- múltiplas moedas no mesmo pedido;
- consulta pública de pedidos;
- dados de entrega;
- criação de `Catalog`, `Pricing` ou de uma porta de consulta de preços.

Enquanto descontos e frete estiverem fora do escopo, o total será exatamente a soma dos subtotais dos itens.

## Regras de negócio

1. O cliente informa `productId`, `quantity` e `unitPrice` para cada item, além de uma única `currency` para o pedido.
2. O cliente não informa `subtotal`, `totalAmount` nem `status`.
3. Nesta etapa de estudo, o `unitPrice` recebido é considerado o preço acordado, embora ainda não seja verificado contra uma fonte comercial confiável.
4. O preço unitário deve ser maior que zero.
5. O subtotal de um item é `unitPrice × quantity`.
6. O total do pedido é a soma de todos os subtotais.
7. O total deve ser maior que zero.
8. Um pedido novo inicia em `AGUARDANDO_ESTOQUE`.
9. Os valores salvos são uma fotografia imutável. Uma mudança posterior no catálogo não altera pedidos existentes.
10. A criação deve ser atômica: falha ao validar qualquer preço ou cálculo impede a persistência do pedido e a publicação do evento.
11. O evento deve carregar exatamente os valores que foram persistidos.
12. Cálculos monetários não devem usar `double` ou `float`.

## Decisão provisória sobre a origem do preço

O projeto ainda não possui `Catalog` ou `Pricing`. Para manter o fluxo executável durante o estudo, `unitPrice` será informado manualmente no JSON da requisição e atravessará `CreateOrderRequest` e `CreateOrderCommand` até o domínio.

Esta é uma exceção consciente e temporária na fronteira de confiança. Ela permite estudar `Money`, invariantes, cálculo do agregado, persistência e eventos, mas não representa um contrato seguro para produção: um consumidor poderia declarar qualquer preço positivo.

Quando existir uma fonte de preços, a API deixará de aceitar `unitPrice`. A aplicação consultará uma porta de saída usando `productId`, enquanto `OrderItem` e `Order` continuarão calculando subtotal e total da mesma forma. Assim, a origem do preço poderá mudar sem transferir as regras monetárias para o controller ou para a infraestrutura.

## Modelo de domínio proposto

### `Money`

Value Object que representa um valor e sua moeda.

Invariantes:

- `amount` obrigatório;
- `currency` obrigatória;
- precisão decimal, sem ponto flutuante binário;
- operações aritméticas somente entre valores da mesma moeda;
- normalização compatível com a unidade monetária persistida.

Para esta tarefa, preços e totais devem possuir duas casas decimais. Arredondamentos silenciosos não são permitidos: um valor recebido com escala incompatível deve ser rejeitado na fronteira HTTP ou pelo domínio.

### `OrderItem`

Novos atributos:

| Atributo | Tipo conceitual | Regra |
| --- | --- | --- |
| `unitPrice` | `Money` | preço declarado e congelado no pedido |
| `subtotal` | `Money` | calculado por `unitPrice × quantity` |

O subtotal não será recebido pronto. Ele deve ser calculado pelo domínio.

### `OrderStatus`

Estados previstos pelo processo completo:

- `AGUARDANDO_ESTOQUE`;
- `AGUARDANDO_AUTORIZACAO`;
- `AGUARDANDO_BAIXA_ESTOQUE`;
- `AGUARDANDO_CAPTURA`;
- `CONFIRMADO`;
- `CANCELAMENTO_PENDENTE`;
- `CANCELADO`;
- `EXPIRADO`.

Esta tarefa implementa apenas a entrada em `AGUARDANDO_ESTOQUE`. As transições serão adicionadas junto aos resultados de estoque e pagamento. O estado `CRIADO` não será persistido como uma etapa separada, pois a criação atômica já termina com o pedido aguardando estoque.

### `Order`

Novos atributos:

| Atributo | Tipo conceitual | Regra |
| --- | --- | --- |
| `status` | `OrderStatus` | obrigatório; novo pedido começa em `AGUARDANDO_ESTOQUE` |
| `total` | `Money` | soma calculada dos subtotais |

`Order.place(...)` calcula o total, define o estado inicial e registra o evento. `Order.reconstitute(...)` recebe os valores e o estado persistidos, validando a coerência estrutural sem registrar um novo evento.

## Fluxo da criação

```text
POST /api/v1/order
    → validação sintática da requisição
    → CreateOrderCommand
    → criação dos itens com o preço unitário declarado
    → OrderItem calcula cada subtotal
    → Order calcula o total e assume AGUARDANDO_ESTOQUE
    → persistência do agregado
    → publicação do evento de domínio
    → conversão para OrderCreatedEvent
    → resposta com identificador, fotografia comercial e estado
```

Persistência e publicação continuam dentro da mesma transação, preservando o comportamento durável já existente com Spring Modulith.

## Contrato HTTP

O corpo da requisição será enriquecido com `currency` no pedido e `unitPrice` em cada item:

```json
{
  "customerId": "2c623f8e-76dd-4a0c-a625-ff23b6af58f8",
  "paymentMethod": "CREDIT_CARD",
  "currency": "BRL",
  "items": [
    {
      "productId": "99058370-7bb4-4d94-8524-e9f4020eb1e8",
      "quantity": 2,
      "unitPrice": 10.50
    },
    {
      "productId": "28cbfd63-a511-4462-a8ca-f7178247c245",
      "quantity": 1,
      "unitPrice": 4.00
    }
  ]
}
```

`subtotal`, `totalAmount` e `status` não fazem parte da requisição, pois são resultados das regras do domínio.

A resposta `201 Created` passará a usar JSON para permitir a conferência no Postman:

```json
{
  "orderId": "fb116546-49d5-4946-86e2-a18327817eb9",
  "status": "AGUARDANDO_ESTOQUE",
  "currency": "BRL",
  "totalAmount": 25.00,
  "items": [
    {
      "productId": "99058370-7bb4-4d94-8524-e9f4020eb1e8",
      "quantity": 2,
      "unitPrice": 10.50,
      "subtotal": 21.00
    },
    {
      "productId": "28cbfd63-a511-4462-a8ca-f7178247c245",
      "quantity": 1,
      "unitPrice": 4.00,
      "subtotal": 4.00
    }
  ]
}
```

A mensagem “Pedido recebido com sucesso” foi retirada do corpo. O status HTTP `201` e o estado `AGUARDANDO_ESTOQUE` comunicam, respectivamente, que o recurso foi criado e que a compra ainda não foi confirmada.

## Evento de domínio e evento público

`OrderPlacedDomainEvent` e `OrderCreatedEvent` devem passar a representar:

```text
orderId
occurredAt / createdAt
customerId
paymentMethod
status = AGUARDANDO_ESTOQUE
currency
totalAmount
items[]:
    productId
    quantity
    unitPrice
    subtotal
```

Valores monetários do evento público devem usar representação decimal exata e um código de moeda separado. Consumidores não devem recalcular o total a partir de preços atuais.

Como o contrato público será enriquecido, os listeners de `Inventory` e `Notification` deverão continuar funcionando e seus testes deverão ser atualizados. Eles podem ignorar os campos novos nesta etapa.

## Persistência

Uma nova migração Flyway deve evoluir as tabelas sem alterar `V1__create_order_tables.sql`.

### Tabela `orders.orders`

Adicionar:

| Coluna | Tipo sugerido | Restrições |
| --- | --- | --- |
| `status` | `VARCHAR(40)` | valor conhecido e obrigatório para novos pedidos |
| `total_amount` | `NUMERIC(19,2)` | maior que zero e obrigatório para novos pedidos |
| `currency` | `CHAR(3)` | código em maiúsculas e obrigatório para novos pedidos |

### Tabela `orders.order_items`

Adicionar:

| Coluna | Tipo sugerido | Restrições |
| --- | --- | --- |
| `unit_price` | `NUMERIC(19,2)` | maior que zero e obrigatório para novos pedidos |
| `subtotal` | `NUMERIC(19,2)` | maior que zero e obrigatório para novos pedidos |

A migração preserva pedidos preexistentes sem inventar preços históricos. Para esses registros legados, todos os campos da fotografia comercial permanecem nulos. Os checks exigem que a fotografia de pedido e item seja inteiramente preenchida ou inteiramente ausente; a aplicação nova sempre a preenche.

Essa nulabilidade é uma compatibilidade de migração, não uma flexibilização do domínio. Objetos `Order` novos ou reconstituídos pela versão atual continuam exigindo status e valores completos. Antes de adicionar consultas de pedidos, será necessário definir como apresentar, arquivar ou enriquecer os registros legados.

A coerência `subtotal = unit_price × quantity` e `total = soma dos subtotais` pertence primariamente ao agregado. Os checks do banco protegem limites básicos; a reconstrução e os testes protegem a coerência completa.

## Tratamento de falhas

A criação deve falhar sem salvar ou publicar evento quando ocorrer qualquer uma destas situações:

- `unitPrice` ausente;
- `currency` ausente ou inválida;
- preço nulo, zero, negativo ou com escala inválida;
- overflow ou valor fora da precisão suportada;
- inconsistência entre total, itens e moeda durante a reconstituição.

Erros de sintaxe da requisição e violações das invariantes do domínio devem ser distinguíveis na aplicação. O mapeamento HTTP exato será definido durante a implementação de acordo com o tratamento de erros existente no projeto.

## Impacto esperado por camada

### Domínio

- criar `Money` e `OrderStatus`;
- adicionar preço e subtotal a `OrderItem`;
- adicionar estado e total a `Order`;
- enriquecer `OrderPlacedDomainEvent`;
- proteger as novas invariantes.

### Aplicação

- adicionar `currency` e `unitPrice` ao comando de criação;
- converter os dados monetários de entrada para `Money`;
- retornar a fotografia calculada em `CreateOrderResult`;
- preservar a atomicidade do caso de uso.

### Adaptadores

- adicionar `currency` e `unitPrice` à requisição HTTP;
- não aceitar `subtotal`, `totalAmount` nem `status` na requisição;
- alterar a resposta de texto para JSON com os valores calculados;
- evoluir entidades JDBC, mapper e repositório;
- enriquecer a conversão para o evento público.

### Consumidores

- adaptar listeners existentes ao contrato enriquecido;
- não interpretar `AGUARDANDO_ESTOQUE` como compra confirmada.

## Critérios de aceitação

### Cenário 1 — Criação com valores válidos

Dado que o cliente informa `currency` e preços unitários válidos, quando cria o pedido, então:

- cada item preserva o preço declarado;
- cada subtotal é calculado corretamente;
- o total corresponde à soma dos subtotais;
- a moeda é preservada;
- o estado é `AGUARDANDO_ESTOQUE`;
- pedido e itens são persistidos;
- o evento contém os mesmos valores persistidos.

Exemplo: duas unidades a R$ 10,50 e uma unidade a R$ 4,00 resultam em subtotais de R$ 21,00 e R$ 4,00, com total de R$ 25,00.

### Cenário 2 — Valores calculados não pertencem à entrada

Dado um pedido HTTP, então o contrato recebe `unitPrice` e `currency`, mas `subtotal`, `totalAmount` e `status` não fazem parte do modelo de entrada nem são utilizados como fonte para os cálculos.

### Cenário 3 — Preço ausente ou inválido

Dado que ao menos um item não possui `unitPrice`, ou que o valor é zero, negativo ou possui escala inválida, quando a criação é solicitada, então nenhum pedido é salvo e nenhum evento é publicado.

### Cenário 4 — Moeda ausente ou inválida

Dado que `currency` está ausente ou não representa um código monetário suportado, quando a criação é solicitada, então o pedido é rejeitado, nada é salvo e nenhum evento é publicado.

### Cenário 5 — Resposta calculada

Dado um pedido válido, quando ele é criado, então a resposta `201` apresenta `orderId`, `status`, `currency`, `totalAmount` e os itens com seus preços e subtotais calculados.

### Cenário 6 — Reconstituição

Dado um pedido persistido válido, quando ele é reconstituído, então seus valores e estado são preservados e nenhum novo evento de criação é registrado.

### Cenário 7 — Imutabilidade da fotografia

Dado um pedido já criado, quando o preço corrente de um produto muda, então preço, subtotal e total armazenados no pedido permanecem inalterados.

## Estratégia de testes

- testes unitários de `Money`, incluindo escala, moeda, soma e multiplicação;
- testes unitários de `OrderItem` para cálculo de subtotal;
- testes unitários de `Order` para total, moeda única e estado inicial;
- testes do comando e do serviço com os valores recebidos pela entrada;
- teste de rollback quando a validação monetária falhar;
- testes do mapper nos dois sentidos;
- testes de persistência com PostgreSQL para precisão decimal e novos campos;
- testes do evento de domínio e do evento público;
- testes dos listeners atuais com o contrato enriquecido;
- testes HTTP da entrada de `unitPrice` e `currency`, da rejeição de valores inválidos e da resposta calculada;
- execução dos testes de arquitetura e modularidade.

## Definição de pronto

- todos os critérios de aceitação automatizáveis possuem testes;
- a suíte completa passa;
- a migração funciona em banco vazio e preserva dados existentes sem fabricar preços históricos;
- apenas `unitPrice` e `currency` entram pela requisição; subtotal, total e status são calculados pelo domínio;
- a resposta permite conferir a fotografia comercial e o estado inicial;
- evento persistido e evento entregue carregam a mesma fotografia comercial do pedido;
- documentação arquitetural é atualizada para refletir o novo modelo e fluxo;
- não há implementação antecipada de estoque ou pagamento.

## Dívida técnica deliberada e decisões pendentes

O recebimento de `unitPrice` pela API é uma dívida técnica deliberada deste projeto de estudos. Quando `Catalog/Pricing` existir, a substituição esperada será:

```text
Hoje:     JSON → unitPrice → domínio
Futuro:   productId → porta de preços → unitPrice → domínio
```

Essa mudança futura não deve alterar o cálculo de `OrderItem`, o cálculo de `Order` nem a persistência da fotografia comercial.

Pedidos anteriores à `ORDER-01` são preservados com a fotografia comercial ausente. Essa compatibilidade deverá ser encerrada futuramente por arquivamento, enriquecimento a partir de uma fonte confiável ou recriação consciente da base de estudo; não será feito backfill com preços inventados.
