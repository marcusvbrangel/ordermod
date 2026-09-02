package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValueObjectsTest {

    @Test
    void identityValueObjectsWrapTheirUuidAndUseValueEquality() {
        var value = UUID.randomUUID();

        assertAll(
                () -> assertEquals(value, new OrderId(value).value()),
                () -> assertEquals(new OrderId(value), new OrderId(value)),
                () -> assertEquals(new CustomerId(value), new CustomerId(value)),
                () -> assertEquals(new OrderItemId(value), new OrderItemId(value)),
                () -> assertEquals(new ProductId(value), new ProductId(value)),
                () -> assertNotEquals(new OrderId(value), new OrderId(UUID.randomUUID()))
        );
    }

    @Test
    void identityValueObjectsRejectMissingUuid() {
        assertAll(
                () -> assertThrows(OrderDomainException.class, () -> new OrderId(null)),
                () -> assertThrows(OrderDomainException.class, () -> new CustomerId(null)),
                () -> assertThrows(OrderDomainException.class, () -> new OrderItemId(null)),
                () -> assertThrows(OrderDomainException.class, () -> new ProductId(null))
        );
    }

    @Test
    void paymentMethodNormalizesSurroundingWhitespaceAndUsesValueEquality() {
        var paymentMethod = new PaymentMethod("  PIX  ");

        assertAll(
                () -> assertEquals("PIX", paymentMethod.value()),
                () -> assertEquals(new PaymentMethod("PIX"), paymentMethod),
                () -> assertNotEquals(new PaymentMethod("CREDIT_CARD"), paymentMethod)
        );
    }

    @Test
    void paymentMethodRejectsNullOrBlankValue() {
        var nullValue = assertThrows(OrderDomainException.class, () -> new PaymentMethod(null));
        var blankValue = assertThrows(OrderDomainException.class, () -> new PaymentMethod("   "));

        assertAll(
                () -> assertTrue(nullValue.getMessage().contains("paymentMethod")),
                () -> assertTrue(blankValue.getMessage().contains("paymentMethod"))
        );
    }

    @Test
    void quantityRepresentsAPositiveNumberWithValueEquality() {
        var quantity = new Quantity(3);

        assertAll(
                () -> assertEquals(3, quantity.value()),
                () -> assertEquals(new Quantity(3), quantity),
                () -> assertNotEquals(new Quantity(4), quantity)
        );
    }

    @Test
    void quantityRejectsZeroOrNegativeValue() {
        var zero = assertThrows(OrderDomainException.class, () -> new Quantity(0));
        var negative = assertThrows(OrderDomainException.class, () -> new Quantity(-1));

        assertAll(
                () -> assertTrue(zero.getMessage().contains("quantity")),
                () -> assertTrue(negative.getMessage().contains("quantity"))
        );
    }

    @Test
    void moneyNormalizesScaleAndCurrencyAndSupportsExactArithmetic() {
        var price = new Money(new BigDecimal("10.5"), " brl ");

        assertAll(
                () -> assertEquals(new BigDecimal("10.50"), price.amount()),
                () -> assertEquals("BRL", price.currency()),
                () -> assertEquals(
                        new Money(new BigDecimal("21.00"), "BRL"),
                        price.multiply(new Quantity(2))
                ),
                () -> assertEquals(
                        new Money(new BigDecimal("14.50"), "BRL"),
                        price.add(new Money(new BigDecimal("4.00"), "BRL"))
                )
        );
    }

    @Test
    void moneyRejectsInvalidAmountCurrencyScaleAndMixedCurrencyArithmetic() {
        var brl = new Money(new BigDecimal("1.00"), "BRL");

        assertAll(
                () -> assertThrows(OrderDomainException.class, () -> new Money(null, "BRL")),
                () -> assertThrows(OrderDomainException.class, () -> new Money(BigDecimal.ONE, null)),
                () -> assertThrows(OrderDomainException.class, () -> new Money(BigDecimal.ONE, "XYZ")),
                () -> assertThrows(OrderDomainException.class, () -> new Money(new BigDecimal("-0.01"), "BRL")),
                () -> assertThrows(OrderDomainException.class, () -> new Money(new BigDecimal("1.001"), "BRL")),
                () -> assertThrows(OrderDomainException.class,
                        () -> new Money(new BigDecimal("100000000000000000.00"), "BRL")),
                () -> assertThrows(OrderDomainException.class,
                        () -> brl.add(new Money(new BigDecimal("1.00"), "USD")))
        );
    }
}
