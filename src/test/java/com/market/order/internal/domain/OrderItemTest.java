package com.market.order.internal.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderItemTest {

    @Test
    void rejectsNonPositiveQuantity() {
        var exception = assertThrows(IllegalArgumentException.class, () -> new OrderItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                0
        ));

        assertTrue(exception.getMessage().contains("quantity"));
    }
}
