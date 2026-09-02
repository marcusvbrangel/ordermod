package com.market.order.internal.domain.model;

import com.market.order.internal.domain.event.OrderPlacedDomainEvent;
import com.market.order.internal.domain.exception.OrderDomainException;
import org.junit.jupiter.api.Test;

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
    void placeCreatesANewAggregateAndRecordsExactlyOneOrderPlacedEvent() {
        var orderId = new OrderId(UUID.randomUUID());
        var customerId = new CustomerId(UUID.randomUUID());
        var paymentMethod = new PaymentMethod("PIX");
        var item = item(2);

        var order = Order.place(
                orderId,
                customerId,
                paymentMethod,
                CREATED_AT,
                List.of(item)
        );

        var events = order.domainEvents();
        assertEquals(1, events.size());
        var placedEvent = assertInstanceOf(OrderPlacedDomainEvent.class, events.getFirst());

        assertAll(
                () -> assertEquals(orderId, order.id()),
                () -> assertEquals(customerId, order.customerId()),
                () -> assertEquals(paymentMethod, order.paymentMethod()),
                () -> assertEquals(CREATED_AT, order.createdAt()),
                () -> assertNull(order.version()),
                () -> assertEquals(List.of(item), order.items()),
                () -> assertEquals(orderId, placedEvent.orderId()),
                () -> assertEquals(CREATED_AT, placedEvent.occurredAt()),
                () -> assertEquals(customerId, placedEvent.customerId()),
                () -> assertEquals(paymentMethod, placedEvent.paymentMethod()),
                () -> assertEquals(
                        List.of(new OrderPlacedDomainEvent.Item(item.productId(), item.quantity())),
                        placedEvent.items()
                )
        );
    }

    @Test
    void reconstituteRestoresPersistedAggregateWithoutRecordingANewEvent() {
        var orderId = new OrderId(UUID.randomUUID());
        var customerId = new CustomerId(UUID.randomUUID());
        var paymentMethod = new PaymentMethod("CREDIT_CARD");
        var items = List.of(item(3));

        var order = Order.reconstitute(
                orderId,
                customerId,
                paymentMethod,
                CREATED_AT,
                7,
                items
        );

        assertAll(
                () -> assertEquals(orderId, order.id()),
                () -> assertEquals(customerId, order.customerId()),
                () -> assertEquals(paymentMethod, order.paymentMethod()),
                () -> assertEquals(CREATED_AT, order.createdAt()),
                () -> assertEquals(7, order.version()),
                () -> assertEquals(items, order.items()),
                () -> assertTrue(order.domainEvents().isEmpty())
        );
    }

    @Test
    void clearDomainEventsMarksRecordedEventsAsHandled() {
        var order = placeOrder(List.of(item(1)));

        assertEquals(1, order.domainEvents().size());

        order.clearDomainEvents();

        assertTrue(order.domainEvents().isEmpty());
    }

    @Test
    void protectsItemsAndPendingDomainEventsFromExternalMutation() {
        var sourceItems = new ArrayList<>(List.of(item(2)));
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
        var first = Order.reconstitute(
                sharedId,
                new CustomerId(UUID.randomUUID()),
                new PaymentMethod("PIX"),
                CREATED_AT,
                1,
                List.of(item(1))
        );
        var sameIdentityWithDifferentState = Order.reconstitute(
                sharedId,
                new CustomerId(UUID.randomUUID()),
                new PaymentMethod("CREDIT_CARD"),
                CREATED_AT.plusSeconds(60),
                9,
                List.of(item(4))
        );
        var anotherIdentity = Order.reconstitute(
                new OrderId(UUID.randomUUID()),
                first.customerId(),
                first.paymentMethod(),
                first.createdAt(),
                first.version(),
                first.items()
        );

        assertAll(
                () -> assertEquals(first, sameIdentityWithDifferentState),
                () -> assertEquals(first.hashCode(), sameIdentityWithDifferentState.hashCode()),
                () -> assertNotEquals(first, anotherIdentity)
        );
    }

    @Test
    void rejectsOrderWithoutItemsOrWithNullItem() {
        var orderId = new OrderId(UUID.randomUUID());
        var customerId = new CustomerId(UUID.randomUUID());
        var paymentMethod = new PaymentMethod("PIX");

        var emptyItems = assertThrows(OrderDomainException.class, () -> Order.place(
                orderId,
                customerId,
                paymentMethod,
                CREATED_AT,
                List.of()
        ));
        var itemsWithNull = new ArrayList<OrderItem>();
        itemsWithNull.add(null);
        var nullItem = assertThrows(OrderDomainException.class, () -> Order.place(
                orderId,
                customerId,
                paymentMethod,
                CREATED_AT,
                itemsWithNull
        ));

        assertAll(
                () -> assertTrue(emptyItems.getMessage().contains("items")),
                () -> assertTrue(nullItem.getMessage().contains("item nulo"))
        );
    }

    @Test
    void rejectsNegativePersistenceVersionWhenReconstituting() {
        var exception = assertThrows(OrderDomainException.class, () -> Order.reconstitute(
                new OrderId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                new PaymentMethod("PIX"),
                CREATED_AT,
                -1,
                List.of(item(1))
        ));

        assertTrue(exception.getMessage().contains("version"));
    }

    @Test
    void rejectsNullRequiredConcepts() {
        var orderId = new OrderId(UUID.randomUUID());
        var customerId = new CustomerId(UUID.randomUUID());
        var paymentMethod = new PaymentMethod("PIX");
        var items = List.of(item(1));

        assertAll(
                () -> assertThrows(OrderDomainException.class,
                        () -> Order.place(null, customerId, paymentMethod, CREATED_AT, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> Order.place(orderId, null, paymentMethod, CREATED_AT, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> Order.place(orderId, customerId, null, CREATED_AT, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> Order.place(orderId, customerId, paymentMethod, null, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> Order.place(orderId, customerId, paymentMethod, CREATED_AT, null))
        );
    }

    private static Order placeOrder(List<OrderItem> items) {
        return Order.place(
                new OrderId(UUID.randomUUID()),
                new CustomerId(UUID.randomUUID()),
                new PaymentMethod("PIX"),
                CREATED_AT,
                items
        );
    }

    private static OrderItem item(int quantity) {
        return OrderItem.create(
                new OrderItemId(UUID.randomUUID()),
                new ProductId(UUID.randomUUID()),
                new Quantity(quantity)
        );
    }
}
