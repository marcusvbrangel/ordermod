package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderItemTest {

    @Test
    void createBuildsAnEntityFromDomainValueObjects() {
        var id = new OrderItemId(UUID.randomUUID());
        var productId = new ProductId(UUID.randomUUID());
        var quantity = new Quantity(2);

        var item = OrderItem.create(id, productId, quantity);

        assertAll(
                () -> assertEquals(id, item.id()),
                () -> assertEquals(productId, item.productId()),
                () -> assertEquals(quantity, item.quantity())
        );
    }

    @Test
    void reconstituteRestoresThePersistedEntity() {
        var id = new OrderItemId(UUID.randomUUID());
        var productId = new ProductId(UUID.randomUUID());
        var quantity = new Quantity(5);

        var item = OrderItem.reconstitute(id, productId, quantity);

        assertAll(
                () -> assertEquals(id, item.id()),
                () -> assertEquals(productId, item.productId()),
                () -> assertEquals(quantity, item.quantity())
        );
    }

    @Test
    void orderItemsAreEntitiesWhoseEqualityIsDefinedOnlyByIdentity() {
        var sharedId = new OrderItemId(UUID.randomUUID());
        var first = OrderItem.create(
                sharedId,
                new ProductId(UUID.randomUUID()),
                new Quantity(1)
        );
        var sameIdentityWithDifferentState = OrderItem.reconstitute(
                sharedId,
                new ProductId(UUID.randomUUID()),
                new Quantity(99)
        );
        var anotherIdentity = OrderItem.create(
                new OrderItemId(UUID.randomUUID()),
                first.productId(),
                first.quantity()
        );

        assertAll(
                () -> assertEquals(first, sameIdentityWithDifferentState),
                () -> assertEquals(first.hashCode(), sameIdentityWithDifferentState.hashCode()),
                () -> assertNotEquals(first, anotherIdentity)
        );
    }

    @Test
    void rejectsNullRequiredConcepts() {
        var id = new OrderItemId(UUID.randomUUID());
        var productId = new ProductId(UUID.randomUUID());
        var quantity = new Quantity(1);

        assertAll(
                () -> assertThrows(OrderDomainException.class,
                        () -> OrderItem.create(null, productId, quantity)),
                () -> assertThrows(OrderDomainException.class,
                        () -> OrderItem.create(id, null, quantity)),
                () -> assertThrows(OrderDomainException.class,
                        () -> OrderItem.create(id, productId, null))
        );
    }
}
