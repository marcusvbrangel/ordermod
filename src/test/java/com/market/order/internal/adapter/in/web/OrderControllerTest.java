package com.market.order.internal.adapter.in.web;

import com.market.order.internal.application.port.in.CreateOrderCommand;
import com.market.order.internal.application.port.in.CreateOrderResult;
import com.market.order.internal.application.port.in.CreateOrderUseCase;
import com.market.order.internal.application.port.in.GetOrderQuery;
import com.market.order.internal.application.port.in.GetOrderResult;
import com.market.order.internal.application.port.in.GetOrderUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderControllerTest {

    @Test
    void mapsMonetaryRequestAndCalculatedResultAcrossHttpBoundary() {
        var receivedCommand = new AtomicReference<CreateOrderCommand>();
        var orderId = UUID.randomUUID();
        var customerId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        CreateOrderUseCase useCase = command -> {
            receivedCommand.set(command);
            return new CreateOrderResult(
                    orderId,
                    "AGUARDANDO_ESTOQUE",
                    new BigDecimal("21.00"),
                    "BRL",
                    List.of(new CreateOrderResult.Item(
                            productId,
                            2,
                            new BigDecimal("10.50"),
                            new BigDecimal("21.00")
                    ))
            );
        };
        var controller = new OrderController(useCase, unusedGetOrderUseCase());
        var request = new CreateOrderRequest(
                customerId,
                "CREDIT_CARD",
                "BRL",
                List.of(new CreateOrderRequest.Item(productId, 2, new BigDecimal("10.50")))
        );

        var response = controller.createOrder(request);

        assertAll(
                () -> assertEquals(
                        new CreateOrderCommand(
                                customerId,
                                "CREDIT_CARD",
                                "BRL",
                                List.of(new CreateOrderCommand.Item(productId, 2, new BigDecimal("10.50")))
                        ),
                        receivedCommand.get()
                ),
                () -> assertEquals(HttpStatus.CREATED, response.getStatusCode()),
                () -> assertEquals(orderId, response.getBody().orderId()),
                () -> assertEquals("AGUARDANDO_ESTOQUE", response.getBody().status()),
                () -> assertEquals(new BigDecimal("21.00"), response.getBody().totalAmount()),
                () -> assertEquals(new BigDecimal("21.00"), response.getBody().items().getFirst().subtotal())
        );
    }

    @Test
    void mapsOrderIdentifierAndCompleteQueryResultAcrossHttpBoundary() {
        var receivedQuery = new AtomicReference<GetOrderQuery>();
        var orderId = UUID.randomUUID();
        var customerId = UUID.randomUUID();
        var itemId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var createdAt = Instant.parse("2026-09-02T12:00:00Z");
        GetOrderUseCase getOrderUseCase = query -> {
            receivedQuery.set(query);
            return new GetOrderResult(
                    orderId,
                    customerId,
                    "PIX",
                    "AGUARDANDO_ESTOQUE",
                    new BigDecimal("21.00"),
                    "BRL",
                    createdAt,
                    List.of(new GetOrderResult.Item(
                            itemId,
                            productId,
                            2,
                            new BigDecimal("10.50"),
                            new BigDecimal("21.00")
                    ))
            );
        };
        var controller = new OrderController(command -> {
            throw new AssertionError("criação não deveria ser chamada");
        }, getOrderUseCase);

        var response = controller.getOrder(orderId);

        assertAll(
                () -> assertEquals(new GetOrderQuery(orderId), receivedQuery.get()),
                () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                () -> assertEquals(orderId, response.getBody().orderId()),
                () -> assertEquals(customerId, response.getBody().customerId()),
                () -> assertEquals("PIX", response.getBody().paymentMethod()),
                () -> assertEquals("AGUARDANDO_ESTOQUE", response.getBody().status()),
                () -> assertEquals(new BigDecimal("21.00"), response.getBody().totalAmount()),
                () -> assertEquals("BRL", response.getBody().currency()),
                () -> assertEquals(createdAt, response.getBody().createdAt()),
                () -> assertEquals(itemId, response.getBody().items().getFirst().itemId()),
                () -> assertEquals(new BigDecimal("21.00"), response.getBody().items().getFirst().subtotal())
        );
    }

    private static GetOrderUseCase unusedGetOrderUseCase() {
        return query -> {
            throw new AssertionError("consulta não deveria ser chamada");
        };
    }
}
