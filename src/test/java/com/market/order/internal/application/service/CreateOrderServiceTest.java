package com.market.order.internal.application.service;

import com.market.order.OrderCreatedEvent;
import com.market.order.internal.application.port.in.CreateOrderCommand;
import com.market.order.internal.application.port.out.OrderEventPublisher;
import com.market.order.internal.application.port.out.OrderRepository;
import com.market.order.internal.domain.model.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateOrderServiceTest {

    @Test
    void savesOrderBeforePublishingEventAndReturnsCreatedOrderId() {
        var calls = new ArrayList<String>();
        var orderReceivedByRepository = new AtomicReference<Order>();
        var eventReceivedByPublisher = new AtomicReference<OrderCreatedEvent>();
        var customerId = UUID.randomUUID();
        var firstProductId = UUID.randomUUID();
        var secondProductId = UUID.randomUUID();

        OrderRepository repository = order -> {
            calls.add("save");
            orderReceivedByRepository.set(order);

            return new Order(
                    order.id(),
                    order.customerId(),
                    order.paymentMethod(),
                    order.createdAt(),
                    0,
                    order.items()
            );
        };
        OrderEventPublisher publisher = event -> {
            calls.add("publish");
            eventReceivedByPublisher.set(event);
        };
        var service = new CreateOrderService(repository, publisher);
        var command = new CreateOrderCommand(
                customerId,
                "PIX",
                List.of(
                        new CreateOrderCommand.Item(firstProductId, 2),
                        new CreateOrderCommand.Item(secondProductId, 1)
                )
        );

        var result = service.createOrder(command);

        var persistedOrder = orderReceivedByRepository.get();
        var publishedEvent = eventReceivedByPublisher.get();

        assertAll(
                () -> assertEquals(List.of("save", "publish"), calls),
                () -> assertNotNull(persistedOrder),
                () -> assertNotNull(persistedOrder.id()),
                () -> assertNotNull(persistedOrder.createdAt()),
                () -> assertEquals(customerId, persistedOrder.customerId()),
                () -> assertEquals("PIX", persistedOrder.paymentMethod()),
                () -> assertNull(persistedOrder.version()),
                () -> assertEquals(2, persistedOrder.items().size()),
                () -> assertNotNull(persistedOrder.items().getFirst().id()),
                () -> assertEquals(firstProductId, persistedOrder.items().getFirst().productId()),
                () -> assertEquals(2, persistedOrder.items().getFirst().quantity()),
                () -> assertNotNull(persistedOrder.items().getLast().id()),
                () -> assertEquals(secondProductId, persistedOrder.items().getLast().productId()),
                () -> assertEquals(1, persistedOrder.items().getLast().quantity()),
                () -> assertNotNull(publishedEvent),
                () -> assertEquals(persistedOrder.id(), publishedEvent.orderId()),
                () -> assertEquals(persistedOrder.createdAt(), publishedEvent.createdAt()),
                () -> assertEquals(customerId, publishedEvent.customerId()),
                () -> assertEquals("PIX", publishedEvent.paymentMethod()),
                () -> assertEquals(
                        List.of(
                                new OrderCreatedEvent.Item(firstProductId, 2),
                                new OrderCreatedEvent.Item(secondProductId, 1)
                        ),
                        publishedEvent.items()
                ),
                () -> assertEquals(persistedOrder.id(), result.orderId())
        );
    }

    @Test
    void doesNotPublishEventWhenRepositoryFails() {
        var repositoryFailure = new IllegalStateException("database unavailable");
        var publicationCount = new AtomicInteger();
        OrderRepository repository = order -> {
            throw repositoryFailure;
        };
        OrderEventPublisher publisher = event -> publicationCount.incrementAndGet();
        var service = new CreateOrderService(repository, publisher);

        var thrown = assertThrows(
                IllegalStateException.class,
                () -> service.createOrder(validCommand())
        );

        assertAll(
                () -> assertSame(repositoryFailure, thrown),
                () -> assertEquals(0, publicationCount.get())
        );
    }

    @Test
    void propagatesPublisherFailureAfterSavingOrder() {
        var publisherFailure = new IllegalStateException("event infrastructure unavailable");
        var orderWasSaved = new AtomicBoolean();
        OrderRepository repository = order -> {
            orderWasSaved.set(true);
            return order;
        };
        OrderEventPublisher publisher = event -> {
            throw publisherFailure;
        };
        var service = new CreateOrderService(repository, publisher);

        var thrown = assertThrows(
                IllegalStateException.class,
                () -> service.createOrder(validCommand())
        );

        assertAll(
                () -> assertTrue(orderWasSaved.get()),
                () -> assertSame(publisherFailure, thrown)
        );
    }

    private static CreateOrderCommand validCommand() {
        return new CreateOrderCommand(
                UUID.randomUUID(),
                "PIX",
                List.of(new CreateOrderCommand.Item(UUID.randomUUID(), 1))
        );
    }
}
