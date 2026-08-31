package com.market.order.internal.application;

import com.market.order.OrderCreatedEvent;
import com.market.order.internal.domain.Order;
import com.market.order.internal.infrastructure.persistence.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderServiceTest {

    @Test
    void persistsOrderBeforePublishingOrderCreatedEvent() {
        var calls = new ArrayList<String>();
        var savedOrderReference = new AtomicReference<Order>();
        var publishedEventReference = new AtomicReference<>();
        var orderRepository = recordingRepository(calls, savedOrderReference);
        ApplicationEventPublisher eventPublisher = event -> {
            calls.add("publish");
            publishedEventReference.set(event);
        };
        var service = new OrderService(orderRepository, eventPublisher);
        var customerId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var command = new CreateOrderCommand(
                customerId,
                "PIX",
                List.of(new CreateOrderCommand.Item(productId, 2))
        );

        service.createOrder(command);

        var savedOrder = savedOrderReference.get();
        var event = assertInstanceOf(OrderCreatedEvent.class, publishedEventReference.get());

        assertEquals(List.of("save", "publish"), calls);
        assertNotNull(savedOrder.id());
        assertNotNull(savedOrder.createdAt());
        assertEquals(customerId, savedOrder.customerId());
        assertEquals("PIX", savedOrder.paymentMethod());
        assertEquals(1, savedOrder.items().size());
        assertNotNull(savedOrder.items().getFirst().id());
        assertEquals(productId, savedOrder.items().getFirst().productId());
        assertEquals(2, savedOrder.items().getFirst().quantity());

        assertNotNull(event.orderId());
        assertNotNull(event.createdAt());
        assertEquals(savedOrder.id(), event.orderId());
        assertEquals(savedOrder.createdAt(), event.createdAt());
        assertEquals(customerId, event.customerId());
        assertEquals("PIX", event.paymentMethod());
        assertEquals(List.of(new OrderCreatedEvent.Item(productId, 2)), event.items());
    }

    private static OrderRepository recordingRepository(
            List<String> calls,
            AtomicReference<Order> savedOrderReference
    ) {
        return (OrderRepository) Proxy.newProxyInstance(
                OrderRepository.class.getClassLoader(),
                new Class<?>[]{OrderRepository.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("save")) {
                        var order = (Order) arguments[0];
                        calls.add("save");
                        savedOrderReference.set(order);
                        return order;
                    }

                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
