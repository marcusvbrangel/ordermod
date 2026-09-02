package com.market.order.internal.application.service;

import com.market.order.internal.application.port.in.CreateOrderCommand;
import com.market.order.internal.application.port.out.OrderEventPublisher;
import com.market.order.internal.application.port.out.OrderRepository;
import com.market.order.internal.domain.event.OrderDomainEvent;
import com.market.order.internal.domain.event.OrderPlacedDomainEvent;
import com.market.order.internal.domain.exception.OrderDomainException;
import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.Money;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.OrderStatus;
import com.market.order.internal.domain.model.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
    void calculatesSnapshotSavesBeforePublishingAndReturnsCalculatedResult() {
        var calls = new ArrayList<String>();
        var receivedOrder = new AtomicReference<Order>();
        var pendingEventsAtSave = new AtomicInteger();
        var receivedEvent = new AtomicReference<OrderDomainEvent>();
        var customerId = UUID.randomUUID();
        var firstProductId = UUID.randomUUID();
        var secondProductId = UUID.randomUUID();

        OrderRepository repository = repository(order -> {
            calls.add("save");
            receivedOrder.set(order);
            pendingEventsAtSave.set(order.domainEvents().size());
            return Order.reconstitute(
                    order.id(),
                    order.customerId(),
                    order.paymentMethod(),
                    order.status(),
                    order.total(),
                    order.createdAt(),
                    /* cancelledAt */ null,
                    0,
                    order.items()
            );
        });
        OrderEventPublisher publisher = event -> {
            calls.add("publish");
            receivedEvent.set(event);
        };
        var service = new CreateOrderService(repository, publisher);
        var command = new CreateOrderCommand(
                customerId,
                "  CREDIT_CARD  ",
                " brl ",
                List.of(
                        new CreateOrderCommand.Item(firstProductId, 2, new BigDecimal("10.50")),
                        new CreateOrderCommand.Item(secondProductId, 1, new BigDecimal("4.00"))
                )
        );

        var result = service.createOrder(command);

        var aggregate = receivedOrder.get();
        var placedEvent = assertInstanceOf(OrderPlacedDomainEvent.class, receivedEvent.get());
        assertAll(
                () -> assertEquals(List.of("save", "publish"), calls),
                () -> assertNotNull(aggregate.id().value()),
                () -> assertNotNull(aggregate.createdAt()),
                () -> assertEquals(new CustomerId(customerId), aggregate.customerId()),
                () -> assertEquals(new PaymentMethod("CREDIT_CARD"), aggregate.paymentMethod()),
                () -> assertEquals(OrderStatus.AGUARDANDO_ESTOQUE, aggregate.status()),
                () -> assertEquals(money("25.00"), aggregate.total()),
                () -> assertNull(aggregate.version()),
                () -> assertEquals(money("21.00"), aggregate.items().getFirst().subtotal()),
                () -> assertEquals(money("4.00"), aggregate.items().getLast().subtotal()),
                () -> assertEquals(1, pendingEventsAtSave.get()),
                () -> assertTrue(aggregate.domainEvents().isEmpty()),
                () -> assertEquals(aggregate.id(), placedEvent.orderId()),
                () -> assertEquals(aggregate.total(), placedEvent.total()),
                () -> assertEquals(aggregate.id().value(), result.orderId()),
                () -> assertEquals("AGUARDANDO_ESTOQUE", result.status()),
                () -> assertEquals(new BigDecimal("25.00"), result.totalAmount()),
                () -> assertEquals("BRL", result.currency()),
                () -> assertEquals(new BigDecimal("21.00"), result.items().getFirst().subtotal())
        );
    }

    @Test
    void doesNotCallOutputPortsWhenMonetaryValueIsInvalid() {
        var repositoryWasCalled = new AtomicBoolean();
        var publisherWasCalled = new AtomicBoolean();
        var service = new CreateOrderService(
                repository(order -> {
                    repositoryWasCalled.set(true);
                    return order;
                }),
                event -> publisherWasCalled.set(true)
        );
        var command = new CreateOrderCommand(
                UUID.randomUUID(),
                "CREDIT_CARD",
                "BRL",
                List.of(new CreateOrderCommand.Item(UUID.randomUUID(), 1, new BigDecimal("1.001")))
        );

        assertThrows(OrderDomainException.class, () -> service.createOrder(command));

        assertAll(
                () -> assertFalse(repositoryWasCalled.get()),
                () -> assertFalse(publisherWasCalled.get())
        );
    }

    @Test
    void doesNotPublishDomainEventWhenRepositoryFails() {
        var repositoryFailure = new IllegalStateException("database unavailable");
        var publicationCount = new AtomicInteger();
        var service = new CreateOrderService(
                repository(order -> {
                    throw repositoryFailure;
                }),
                event -> publicationCount.incrementAndGet()
        );

        var thrown = assertThrows(IllegalStateException.class, () -> service.createOrder(validCommand()));

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
        var service = new CreateOrderService(
                repository(order -> {
                    calls.add("save");
                    savedOrder.set(order);
                    return order;
                }),
                event -> {
                    calls.add("publish");
                    throw publisherFailure;
                }
        );

        var thrown = assertThrows(IllegalStateException.class, () -> service.createOrder(validCommand()));

        assertAll(
                () -> assertEquals(List.of("save", "publish"), calls),
                () -> assertSame(publisherFailure, thrown),
                () -> assertEquals(1, savedOrder.get().domainEvents().size())
        );
    }

    @Test
    void rejectsNullCommandBeforeCallingOutputPorts() {
        var repositoryWasCalled = new AtomicBoolean();
        var publisherWasCalled = new AtomicBoolean();
        var service = new CreateOrderService(
                repository(order -> {
                    repositoryWasCalled.set(true);
                    return order;
                }),
                event -> publisherWasCalled.set(true)
        );

        assertThrows(NullPointerException.class, () -> service.createOrder(null));

        assertAll(
                () -> assertFalse(repositoryWasCalled.get()),
                () -> assertFalse(publisherWasCalled.get())
        );
    }

    private static CreateOrderCommand validCommand() {
        return new CreateOrderCommand(
                UUID.randomUUID(),
                "CREDIT_CARD",
                "BRL",
                List.of(new CreateOrderCommand.Item(UUID.randomUUID(), 1, new BigDecimal("1.00")))
        );
    }

    private static OrderRepository repository(Function<Order, Order> save) {
        return new OrderRepository() {
            @Override
            public Order save(Order order) {
                return save.apply(order);
            }

            @Override
            public Optional<Order> findById(OrderId orderId) {
                return Optional.empty();
            }
        };
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), "BRL");
    }
}
