package com.market.order.internal.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                Instant.now(),
                null,
                items
        );

        items.clear();

        assertEquals("PIX", order.paymentMethod());
        assertEquals(1, order.items().size());
        assertThrows(UnsupportedOperationException.class, () -> order.items().clear());
    }

    @Test
    void rejectsOrderWithoutItems() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PIX",
                Instant.now(),
                null,
                List.of()
        ));

        assertTrue(exception.getMessage().contains("items"));
    }

    @Test
    void rejectsBlankPaymentMethod() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "   ",
                Instant.now(),
                null,
                List.of(new OrderItem(UUID.randomUUID(), UUID.randomUUID(), 1))
        ));

        assertTrue(exception.getMessage().contains("paymentMethod"));
    }
}
