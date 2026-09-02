package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderItemTest {

    @Test
    void createCalculatesSubtotalFromUnitPriceAndQuantity() {
        var id = new OrderItemId(UUID.randomUUID());
        var productId = new ProductId(UUID.randomUUID());
        var quantity = new Quantity(2);
        var unitPrice = money("10.50");

        var item = OrderItem.create(id, productId, quantity, unitPrice);

        assertAll(
                () -> assertEquals(id, item.id()),
                () -> assertEquals(productId, item.productId()),
                () -> assertEquals(quantity, item.quantity()),
                () -> assertEquals(unitPrice, item.unitPrice()),
                () -> assertEquals(money("21.00"), item.subtotal())
        );
    }

    @Test
    void reconstituteRestoresAConsistentPersistedEntity() {
        var item = OrderItem.reconstitute(
                new OrderItemId(UUID.randomUUID()),
                new ProductId(UUID.randomUUID()),
                new Quantity(5),
                money("4.00"),
                money("20.00")
        );

        assertEquals(money("20.00"), item.subtotal());
    }

    @Test
    void rejectsAnInconsistentPersistedSubtotal() {
        assertThrows(OrderDomainException.class, () -> OrderItem.reconstitute(
                new OrderItemId(UUID.randomUUID()),
                new ProductId(UUID.randomUUID()),
                new Quantity(2),
                money("10.00"),
                money("30.00")
        ));
    }

    @Test
    void orderItemsAreEntitiesWhoseEqualityIsDefinedOnlyByIdentity() {
        var sharedId = new OrderItemId(UUID.randomUUID());
        var first = OrderItem.create(sharedId, new ProductId(UUID.randomUUID()), new Quantity(1), money("1.00"));
        var sameIdentityWithDifferentState = OrderItem.reconstitute(
                sharedId,
                new ProductId(UUID.randomUUID()),
                new Quantity(2),
                money("3.00"),
                money("6.00")
        );
        var anotherIdentity = OrderItem.create(
                new OrderItemId(UUID.randomUUID()),
                first.productId(),
                first.quantity(),
                first.unitPrice()
        );

        assertAll(
                () -> assertEquals(first, sameIdentityWithDifferentState),
                () -> assertEquals(first.hashCode(), sameIdentityWithDifferentState.hashCode()),
                () -> assertNotEquals(first, anotherIdentity)
        );
    }

    @Test
    void rejectsNullRequiredConceptsAndNonPositiveUnitPrice() {
        var id = new OrderItemId(UUID.randomUUID());
        var productId = new ProductId(UUID.randomUUID());
        var quantity = new Quantity(1);
        var unitPrice = money("1.00");

        assertAll(
                () -> assertThrows(OrderDomainException.class,
                        () -> OrderItem.create(null, productId, quantity, unitPrice)),
                () -> assertThrows(OrderDomainException.class,
                        () -> OrderItem.create(id, null, quantity, unitPrice)),
                () -> assertThrows(OrderDomainException.class,
                        () -> OrderItem.create(id, productId, null, unitPrice)),
                () -> assertThrows(OrderDomainException.class,
                        () -> OrderItem.create(id, productId, quantity, null)),
                () -> assertThrows(OrderDomainException.class,
                        () -> OrderItem.create(id, productId, quantity, money("0.00")))
        );
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), "BRL");
    }
}
