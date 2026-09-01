package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderItemTest {

    @Test
    void createsValidOrderItem() {
        var id = UUID.randomUUID();
        var productId = UUID.randomUUID();

        var item = new OrderItem(id, productId, 2);

        assertAll(
                () -> assertEquals(id, item.id()),
                () -> assertEquals(productId, item.productId()),
                () -> assertEquals(2, item.quantity())
        );
    }

    @Test
    void rejectsNonPositiveQuantity() {
        var id = UUID.randomUUID();
        var productId = UUID.randomUUID();

        var zeroQuantity = assertThrows(
                OrderDomainException.class,
                () -> new OrderItem(id, productId, 0)
        );
        var negativeQuantity = assertThrows(
                OrderDomainException.class,
                () -> new OrderItem(id, productId, -1)
        );

        assertAll(
                () -> assertTrue(zeroQuantity.getMessage().contains("quantity")),
                () -> assertTrue(negativeQuantity.getMessage().contains("quantity"))
        );
    }

    @Test
    void rejectsNullIdentifiers() {
        var id = UUID.randomUUID();
        var productId = UUID.randomUUID();

        assertAll(
                () -> assertThrows(OrderDomainException.class, () -> new OrderItem(null, productId, 1)),
                () -> assertThrows(OrderDomainException.class, () -> new OrderItem(id, null, 1))
        );
    }
}
