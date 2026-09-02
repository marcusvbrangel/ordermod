package com.market.order.internal.domain.event;

import com.market.order.internal.domain.exception.OrderDomainException;
import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.PaymentMethod;
import com.market.order.internal.domain.model.ProductId;
import com.market.order.internal.domain.model.Quantity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderPlacedDomainEventTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-09-01T12:00:00Z");

    @Test
    void capturesAnImmutableSnapshotOfTheFactThatAnOrderWasPlaced() {
        var orderId = new OrderId(UUID.randomUUID());
        var customerId = new CustomerId(UUID.randomUUID());
        var paymentMethod = new PaymentMethod("PIX");
        var item = new OrderPlacedDomainEvent.Item(
                new ProductId(UUID.randomUUID()),
                new Quantity(2)
        );
        var sourceItems = new ArrayList<>(List.of(item));

        OrderDomainEvent domainEvent = new OrderPlacedDomainEvent(
                orderId,
                OCCURRED_AT,
                customerId,
                paymentMethod,
                sourceItems
        );
        sourceItems.clear();
        var placedEvent = assertInstanceOf(OrderPlacedDomainEvent.class, domainEvent);

        assertAll(
                () -> assertEquals(orderId, placedEvent.orderId()),
                () -> assertEquals(OCCURRED_AT, placedEvent.occurredAt()),
                () -> assertEquals(customerId, placedEvent.customerId()),
                () -> assertEquals(paymentMethod, placedEvent.paymentMethod()),
                () -> assertEquals(List.of(item), placedEvent.items()),
                () -> assertThrows(UnsupportedOperationException.class, () -> placedEvent.items().clear())
        );
    }

    @Test
    void rejectsAnEventWithoutRequiredBusinessData() {
        var orderId = new OrderId(UUID.randomUUID());
        var customerId = new CustomerId(UUID.randomUUID());
        var paymentMethod = new PaymentMethod("PIX");
        var items = List.of(validItem());

        assertAll(
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(null, OCCURRED_AT, customerId, paymentMethod, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, null, customerId, paymentMethod, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, OCCURRED_AT, null, paymentMethod, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, OCCURRED_AT, customerId, null, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, OCCURRED_AT, customerId, paymentMethod, null)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, OCCURRED_AT, customerId, paymentMethod, List.of()))
        );
    }

    @Test
    void eventItemRejectsMissingProductOrQuantity() {
        var productId = new ProductId(UUID.randomUUID());
        var quantity = new Quantity(1);

        assertAll(
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent.Item(null, quantity)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent.Item(productId, null))
        );
    }

    private static OrderPlacedDomainEvent.Item validItem() {
        return new OrderPlacedDomainEvent.Item(
                new ProductId(UUID.randomUUID()),
                new Quantity(1)
        );
    }
}
