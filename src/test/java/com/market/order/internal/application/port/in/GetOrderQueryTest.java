package com.market.order.internal.application.port.in;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetOrderQueryTest {

    @Test
    void acceptsAnOrderIdentifier() {
        var orderId = UUID.randomUUID();

        assertEquals(orderId, new GetOrderQuery(orderId).orderId());
    }

    @Test
    void rejectsNullOrderIdentifier() {
        assertThrows(NullPointerException.class, () -> new GetOrderQuery(null));
    }
}
