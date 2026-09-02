package com.market.order.internal.domain.model;

import com.market.order.internal.domain.event.OrderPlacedDomainEvent;
import com.market.order.internal.domain.exception.OrderDomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-01T12:00:00Z");

    @Test
    void placeCalculatesTotalSetsInitialStatusAndRecordsCommercialSnapshot() {
        var orderId = new OrderId(UUID.randomUUID());
        var customerId = new CustomerId(UUID.randomUUID());
        var paymentMethod = new PaymentMethod("CREDIT_CARD");
        var firstItem = item(2, "10.50");
        var secondItem = item(1, "4.00");

        var order = Order.place(
                orderId,
                customerId,
                paymentMethod,
                CREATED_AT,
                List.of(firstItem, secondItem)
        );

        var events = order.domainEvents();
        assertEquals(1, events.size());
        var placedEvent = assertInstanceOf(OrderPlacedDomainEvent.class, events.getFirst());

        assertAll(
                () -> assertEquals(orderId, order.id()),
                () -> assertEquals(customerId, order.customerId()),
                () -> assertEquals(paymentMethod, order.paymentMethod()),
                () -> assertEquals(OrderStatus.AGUARDANDO_ESTOQUE, order.status()),
                () -> assertEquals(money("25.00"), order.total()),
                () -> assertEquals(CREATED_AT, order.createdAt()),
                () -> assertNull(order.version()),
                () -> assertEquals(List.of(firstItem, secondItem), order.items()),
                () -> assertEquals(orderId, placedEvent.orderId()),
                () -> assertEquals(OrderStatus.AGUARDANDO_ESTOQUE, placedEvent.status()),
                () -> assertEquals(money("25.00"), placedEvent.total()),
                () -> assertEquals(
                        List.of(
                                eventItem(firstItem),
                                eventItem(secondItem)
                        ),
                        placedEvent.items()
                )
        );
    }

    @Test
    void rejectsItemsWithDifferentCurrencies() {
        var items = List.of(
                item(1, "10.00"),
                OrderItem.create(
                        new OrderItemId(UUID.randomUUID()),
                        new ProductId(UUID.randomUUID()),
                        new Quantity(1),
                        new Money(new BigDecimal("2.00"), "USD")
                )
        );

        assertThrows(OrderDomainException.class, () -> placeOrder(items));
    }

    @Test
    void reconstituteRestoresSnapshotWithoutRecordingANewEvent() {
        var items = List.of(item(3, "2.00"));

        var order = Order.reconstitute(
                new OrderId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                new PaymentMethod("CREDIT_CARD"),
                OrderStatus.AGUARDANDO_CAPTURA,
                money("6.00"),
                CREATED_AT,
                7,
                items
        );

        assertAll(
                () -> assertEquals(OrderStatus.AGUARDANDO_CAPTURA, order.status()),
                () -> assertEquals(money("6.00"), order.total()),
                () -> assertEquals(7, order.version()),
                () -> assertEquals(items, order.items()),
                () -> assertTrue(order.domainEvents().isEmpty())
        );
    }

    @Test
    void reconstituteRejectsTotalThatDoesNotMatchItems() {
        assertThrows(OrderDomainException.class, () -> Order.reconstitute(
                new OrderId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                new PaymentMethod("CREDIT_CARD"),
                OrderStatus.AGUARDANDO_ESTOQUE,
                money("99.00"),
                CREATED_AT,
                0,
                List.of(item(1, "1.00"))
        ));
    }

    @Test
    void clearDomainEventsMarksRecordedEventsAsHandled() {
        var order = placeOrder(List.of(item(1, "1.00")));

        order.clearDomainEvents();

        assertTrue(order.domainEvents().isEmpty());
    }

    @Test
    void protectsItemsAndPendingDomainEventsFromExternalMutation() {
        var sourceItems = new ArrayList<>(List.of(item(2, "1.00")));
        var order = placeOrder(sourceItems);
        sourceItems.clear();

        assertAll(
                () -> assertEquals(1, order.items().size()),
                () -> assertThrows(UnsupportedOperationException.class, () -> order.items().clear()),
                () -> assertThrows(UnsupportedOperationException.class, () -> order.domainEvents().clear())
        );
    }

    @Test
    void ordersAreEntitiesWhoseEqualityIsDefinedOnlyByIdentity() {
        var sharedId = new OrderId(UUID.randomUUID());
        var firstItems = List.of(item(1, "1.00"));
        var first = reconstitute(sharedId, OrderStatus.AGUARDANDO_ESTOQUE, firstItems, 1);
        var otherItems = List.of(item(2, "2.00"));
        var sameIdentityWithDifferentState = reconstitute(sharedId, OrderStatus.AGUARDANDO_CAPTURA, otherItems, 9);
        var anotherIdentity = reconstitute(new OrderId(UUID.randomUUID()), first.status(), first.items(), first.version());

        assertAll(
                () -> assertEquals(first, sameIdentityWithDifferentState),
                () -> assertEquals(first.hashCode(), sameIdentityWithDifferentState.hashCode()),
                () -> assertNotEquals(first, anotherIdentity)
        );
    }

    @Test
    void rejectsOrderWithoutItemsOrWithNullItem() {
        var emptyItems = assertThrows(OrderDomainException.class, () -> placeOrder(List.of()));
        var itemsWithNull = new ArrayList<OrderItem>();
        itemsWithNull.add(null);
        var nullItem = assertThrows(OrderDomainException.class, () -> placeOrder(itemsWithNull));

        assertAll(
                () -> assertTrue(emptyItems.getMessage().contains("items")),
                () -> assertTrue(nullItem.getMessage().contains("item nulo"))
        );
    }

    @Test
    void rejectsNegativePersistenceVersionWhenReconstituting() {
        var items = List.of(item(1, "1.00"));

        var exception = assertThrows(OrderDomainException.class, () -> Order.reconstitute(
                new OrderId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                new PaymentMethod("CREDIT_CARD"),
                OrderStatus.AGUARDANDO_ESTOQUE,
                money("1.00"),
                CREATED_AT,
                -1,
                items
        ));

        assertTrue(exception.getMessage().contains("version"));
    }

    private static Order placeOrder(List<OrderItem> items) {
        return Order.place(
                new OrderId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                new PaymentMethod("CREDIT_CARD"),
                CREATED_AT,
                items
        );
    }

    private static Order reconstitute(OrderId id, OrderStatus status, List<OrderItem> items, Integer version) {
        var total = items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money::add)
                .orElseThrow();
        return Order.reconstitute(
                id,
                new CustomerId(UUID.randomUUID()),
                new PaymentMethod("CREDIT_CARD"),
                status,
                total,
                CREATED_AT,
                version,
                items
        );
    }

    private static OrderItem item(int quantity, String unitPrice) {
        return OrderItem.create(
                new OrderItemId(UUID.randomUUID()),
                new ProductId(UUID.randomUUID()),
                new Quantity(quantity),
                money(unitPrice)
        );
    }

    private static OrderPlacedDomainEvent.Item eventItem(OrderItem item) {
        return new OrderPlacedDomainEvent.Item(
                item.productId(),
                item.quantity(),
                item.unitPrice(),
                item.subtotal()
        );
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), "BRL");
    }
}
