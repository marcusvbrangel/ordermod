package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    @Test
    void createsOrderWithImmutableItemsAndNormalizedPaymentMethod() {
        var items = new ArrayList<>(List.of(new OrderItem(UUID.randomUUID(), UUID.randomUUID(), 2)));

        var order = new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                " PIX ",
                Instant.parse("2026-09-01T12:00:00Z"),
                null,
                items
        );

        items.clear();

        assertAll(
                () -> assertEquals("PIX", order.paymentMethod()),
                () -> assertEquals(1, order.items().size()),
                () -> assertThrows(UnsupportedOperationException.class, () -> order.items().clear())
        );
    }

    @Test
    void rejectsOrderWithoutItems() {
        var exception = assertThrows(OrderDomainException.class, () -> new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PIX",
                Instant.parse("2026-09-01T12:00:00Z"),
                null,
                List.of()
        ));

        assertTrue(exception.getMessage().contains("items"));
    }

    @Test
    void rejectsBlankPaymentMethod() {
        var exception = assertThrows(OrderDomainException.class, () -> new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "   ",
                Instant.parse("2026-09-01T12:00:00Z"),
                null,
                List.of(new OrderItem(UUID.randomUUID(), UUID.randomUUID(), 1))
        ));

        assertTrue(exception.getMessage().contains("paymentMethod"));
    }

    @Test
    void rejectsNegativeVersion() {
        var exception = assertThrows(OrderDomainException.class, () -> new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PIX",
                Instant.parse("2026-09-01T12:00:00Z"),
                -1,
                List.of(new OrderItem(UUID.randomUUID(), UUID.randomUUID(), 1))
        ));

        assertTrue(exception.getMessage().contains("version"));
    }

    @Test
    void rejectsNullRequiredFields() {
        var item = new OrderItem(UUID.randomUUID(), UUID.randomUUID(), 1);
        var id = UUID.randomUUID();
        var customerId = UUID.randomUUID();
        var createdAt = Instant.parse("2026-09-01T12:00:00Z");

        assertAll(
                () -> assertThrows(OrderDomainException.class,
                        () -> new Order(null, customerId, "PIX", createdAt, null, List.of(item))),
                () -> assertThrows(OrderDomainException.class,
                        () -> new Order(id, null, "PIX", createdAt, null, List.of(item))),
                () -> assertThrows(OrderDomainException.class,
                        () -> new Order(id, customerId, null, createdAt, null, List.of(item))),
                () -> assertThrows(OrderDomainException.class,
                        () -> new Order(id, customerId, "PIX", null, null, List.of(item))),
                () -> assertThrows(OrderDomainException.class,
                        () -> new Order(id, customerId, "PIX", createdAt, null, null))
        );
    }
}
