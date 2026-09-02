package com.market.order.internal.application.service;

import com.market.order.internal.application.port.in.CreateOrderCommand;
import com.market.order.internal.application.port.out.OrderEventPublisher;
import com.market.order.internal.application.port.out.OrderRepository;
import com.market.order.internal.domain.event.OrderDomainEvent;
import com.market.order.internal.domain.event.OrderPlacedDomainEvent;
import com.market.order.internal.domain.exception.OrderDomainException;
import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.PaymentMethod;
import com.market.order.internal.domain.model.ProductId;
import com.market.order.internal.domain.model.Quantity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateOrderServiceTest {

    @Test
    void savesAggregateBeforePublishingItsDomainEventAndReturnsPersistedIdentity() {
        var calls = new ArrayList<String>();
        var orderReceivedByRepository = new AtomicReference<Order>();
        var pendingEventsAtSave = new AtomicInteger();
        var eventReceivedByPublisher = new AtomicReference<OrderDomainEvent>();
        var customerId = UUID.randomUUID();
        var firstProductId = UUID.randomUUID();
        var secondProductId = UUID.randomUUID();

        OrderRepository repository = order -> {
            calls.add("save");
            orderReceivedByRepository.set(order);
            pendingEventsAtSave.set(order.domainEvents().size());

            return Order.reconstitute(
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
                "  PIX  ",
                List.of(
                        new CreateOrderCommand.Item(firstProductId, 2),
                        new CreateOrderCommand.Item(secondProductId, 1)
                )
        );

        var result = service.createOrder(command);

        var aggregate = orderReceivedByRepository.get();
        var placedEvent = assertInstanceOf(
                OrderPlacedDomainEvent.class,
                eventReceivedByPublisher.get()
        );

        assertAll(
                () -> assertEquals(List.of("save", "publish"), calls),
                () -> assertNotNull(aggregate),
                () -> assertNotNull(aggregate.id().value()),
                () -> assertNotNull(aggregate.createdAt()),
                () -> assertEquals(new CustomerId(customerId), aggregate.customerId()),
                () -> assertEquals(new PaymentMethod("PIX"), aggregate.paymentMethod()),
                () -> assertNull(aggregate.version()),
                () -> assertEquals(2, aggregate.items().size()),
                () -> assertNotNull(aggregate.items().getFirst().id().value()),
                () -> assertEquals(new ProductId(firstProductId), aggregate.items().getFirst().productId()),
                () -> assertEquals(new Quantity(2), aggregate.items().getFirst().quantity()),
                () -> assertNotNull(aggregate.items().getLast().id().value()),
                () -> assertEquals(new ProductId(secondProductId), aggregate.items().getLast().productId()),
                () -> assertEquals(new Quantity(1), aggregate.items().getLast().quantity()),
                () -> assertEquals(1, pendingEventsAtSave.get()),
                () -> assertTrue(aggregate.domainEvents().isEmpty()),
                () -> assertEquals(aggregate.id(), placedEvent.orderId()),
                () -> assertEquals(aggregate.createdAt(), placedEvent.occurredAt()),
                () -> assertEquals(aggregate.customerId(), placedEvent.customerId()),
                () -> assertEquals(aggregate.paymentMethod(), placedEvent.paymentMethod()),
                () -> assertEquals(
                        List.of(
                                new OrderPlacedDomainEvent.Item(
                                        new ProductId(firstProductId),
                                        new Quantity(2)
                                ),
                                new OrderPlacedDomainEvent.Item(
                                        new ProductId(secondProductId),
                                        new Quantity(1)
                                )
                        ),
                        placedEvent.items()
                ),
                () -> assertEquals(aggregate.id().value(), result.orderId())
        );
    }

    @Test
    void doesNotPublishDomainEventWhenRepositoryFails() {
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
    void propagatesPublisherFailureAfterSavingAndKeepsEventPending() {
        var publisherFailure = new IllegalStateException("event infrastructure unavailable");
        var calls = new ArrayList<String>();
        var savedOrder = new AtomicReference<Order>();
        OrderRepository repository = order -> {
            calls.add("save");
            savedOrder.set(order);
            return order;
        };
        OrderEventPublisher publisher = event -> {
            calls.add("publish");
            throw publisherFailure;
        };
        var service = new CreateOrderService(repository, publisher);

        var thrown = assertThrows(
                IllegalStateException.class,
                () -> service.createOrder(validCommand())
        );

        assertAll(
                () -> assertEquals(List.of("save", "publish"), calls),
                () -> assertSame(publisherFailure, thrown),
                () -> assertEquals(1, savedOrder.get().domainEvents().size())
        );
    }

    @Test
    void rejectsInvalidDomainValueBeforeCallingOutputPorts() {
        var repositoryWasCalled = new AtomicBoolean();
        var publisherWasCalled = new AtomicBoolean();
        OrderRepository repository = order -> {
            repositoryWasCalled.set(true);
            return order;
        };
        OrderEventPublisher publisher = event -> publisherWasCalled.set(true);
        var service = new CreateOrderService(repository, publisher);
        var command = new CreateOrderCommand(
                UUID.randomUUID(),
                "   ",
                List.of(new CreateOrderCommand.Item(UUID.randomUUID(), 1))
        );

        assertThrows(OrderDomainException.class, () -> service.createOrder(command));

        assertAll(
                () -> assertFalse(repositoryWasCalled.get()),
                () -> assertFalse(publisherWasCalled.get())
        );
    }

    @Test
    void rejectsNullCommandBeforeCallingOutputPorts() {
        var repositoryWasCalled = new AtomicBoolean();
        var publisherWasCalled = new AtomicBoolean();
        OrderRepository repository = order -> {
            repositoryWasCalled.set(true);
            return order;
        };
        OrderEventPublisher publisher = event -> publisherWasCalled.set(true);
        var service = new CreateOrderService(repository, publisher);

        assertThrows(NullPointerException.class, () -> service.createOrder(null));

        assertAll(
                () -> assertFalse(repositoryWasCalled.get()),
                () -> assertFalse(publisherWasCalled.get())
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
