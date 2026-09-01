package com.market.order.internal.adapter.in.web;

import com.market.order.internal.application.port.in.CreateOrderCommand;
import com.market.order.internal.application.port.in.CreateOrderResult;
import com.market.order.internal.application.port.in.CreateOrderUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderControllerTest {

    @Test
    void mapsRequestToCommandAndReturnsCreatedResponse() {
        var receivedCommand = new AtomicReference<CreateOrderCommand>();
        var invocationCount = new AtomicInteger();
        var createdOrderId = UUID.randomUUID();
        CreateOrderUseCase useCase = command -> {
            invocationCount.incrementAndGet();
            receivedCommand.set(command);
            return new CreateOrderResult(createdOrderId);
        };
        var controller = new OrderController(useCase);
        var customerId = UUID.randomUUID();
        var firstProductId = UUID.randomUUID();
        var secondProductId = UUID.randomUUID();
        var request = new CreateOrderRequest(
                customerId,
                "PIX",
                List.of(
                        new CreateOrderRequest.Item(firstProductId, 2),
                        new CreateOrderRequest.Item(secondProductId, 1)
                )
        );

        var response = controller.createOrder(request);

        assertAll(
                () -> assertEquals(1, invocationCount.get()),
                () -> assertEquals(
                        new CreateOrderCommand(
                                customerId,
                                "PIX",
                                List.of(
                                        new CreateOrderCommand.Item(firstProductId, 2),
                                        new CreateOrderCommand.Item(secondProductId, 1)
                                )
                        ),
                        receivedCommand.get()
                ),
                () -> assertEquals(HttpStatus.CREATED, response.getStatusCode()),
                () -> assertEquals("Pedido recebido com sucesso", response.getBody())
        );
    }
}
