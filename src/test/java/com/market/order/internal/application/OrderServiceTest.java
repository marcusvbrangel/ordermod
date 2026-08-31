package com.market.order.internal.application;

import com.market.order.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderServiceTest {

    @Test
    void publishesOrderCreatedEvent() {
        var publishedEvent = new AtomicReference<>();
        ApplicationEventPublisher eventPublisher = publishedEvent::set;
        var service = new OrderService(eventPublisher);
        var customerId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var command = new CreateOrderCommand(
                customerId,
                "PIX",
                List.of(new CreateOrderCommand.Item(productId, 2))
        );

        service.createOrder(command);

        var event = assertInstanceOf(OrderCreatedEvent.class, publishedEvent.get());
        assertNotNull(event.orderId());
        assertNotNull(event.createdAt());
        assertEquals(customerId, event.customerId());
        assertEquals("PIX", event.paymentMethod());
        assertEquals(List.of(new OrderCreatedEvent.Item(productId, 2)), event.items());
    }
}
