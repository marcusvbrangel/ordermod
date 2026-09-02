package com.market.order.internal.domain.event;

import com.market.order.internal.domain.exception.OrderDomainException;
import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.Money;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.OrderStatus;
import com.market.order.internal.domain.model.PaymentMethod;
import com.market.order.internal.domain.model.ProductId;
import com.market.order.internal.domain.model.Quantity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderPlacedDomainEventTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-09-01T12:00:00Z");

    @Test
    void capturesAnImmutableCommercialSnapshot() {
        var orderId = new OrderId(UUID.randomUUID());
        var customerId = new CustomerId(UUID.randomUUID());
        var item = validItem();
        var sourceItems = new ArrayList<>(List.of(item));

        OrderDomainEvent domainEvent = event(orderId, customerId, money("2.00"), sourceItems);
        sourceItems.clear();
        var placedEvent = assertInstanceOf(OrderPlacedDomainEvent.class, domainEvent);

        assertAll(
                () -> assertEquals(orderId, placedEvent.orderId()),
                () -> assertEquals(OCCURRED_AT, placedEvent.occurredAt()),
                () -> assertEquals(customerId, placedEvent.customerId()),
                () -> assertEquals(new PaymentMethod("CREDIT_CARD"), placedEvent.paymentMethod()),
                () -> assertEquals(OrderStatus.AGUARDANDO_ESTOQUE, placedEvent.status()),
                () -> assertEquals(money("2.00"), placedEvent.total()),
                () -> assertEquals(List.of(item), placedEvent.items()),
                () -> assertThrows(UnsupportedOperationException.class, () -> placedEvent.items().clear())
        );
    }

    @Test
    void rejectsAnEventWithoutRequiredBusinessData() {
        var orderId = new OrderId(UUID.randomUUID());
        var customerId = new CustomerId(UUID.randomUUID());
        var paymentMethod = new PaymentMethod("CREDIT_CARD");
        var total = money("2.00");
        var items = List.of(validItem());

        assertAll(
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(null, OCCURRED_AT, customerId, paymentMethod,
                                OrderStatus.AGUARDANDO_ESTOQUE, total, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, null, customerId, paymentMethod,
                                OrderStatus.AGUARDANDO_ESTOQUE, total, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, OCCURRED_AT, null, paymentMethod,
                                OrderStatus.AGUARDANDO_ESTOQUE, total, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, OCCURRED_AT, customerId, null,
                                OrderStatus.AGUARDANDO_ESTOQUE, total, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, OCCURRED_AT, customerId, paymentMethod,
                                null, total, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, OCCURRED_AT, customerId, paymentMethod,
                                OrderStatus.AGUARDANDO_ESTOQUE, null, items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, OCCURRED_AT, customerId, paymentMethod,
                                OrderStatus.AGUARDANDO_ESTOQUE, money("3.00"), items)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent(orderId, OCCURRED_AT, customerId, paymentMethod,
                                OrderStatus.AGUARDANDO_ESTOQUE, total, List.of()))
        );
    }

    @Test
    void eventItemRejectsMissingOrInconsistentData() {
        var productId = new ProductId(UUID.randomUUID());
        var quantity = new Quantity(2);
        var unitPrice = money("1.00");
        var subtotal = money("2.00");

        assertAll(
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent.Item(null, quantity, unitPrice, subtotal)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent.Item(productId, null, unitPrice, subtotal)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent.Item(productId, quantity, null, subtotal)),
                () -> assertThrows(OrderDomainException.class,
                        () -> new OrderPlacedDomainEvent.Item(productId, quantity, unitPrice, money("3.00")))
        );
    }

    private static OrderPlacedDomainEvent event(
            OrderId orderId,
            CustomerId customerId,
            Money total,
            List<OrderPlacedDomainEvent.Item> items
    ) {
        return new OrderPlacedDomainEvent(
                orderId,
                OCCURRED_AT,
                customerId,
                new PaymentMethod("CREDIT_CARD"),
                OrderStatus.AGUARDANDO_ESTOQUE,
                total,
                items
        );
    }

    private static OrderPlacedDomainEvent.Item validItem() {
        return new OrderPlacedDomainEvent.Item(
                new ProductId(UUID.randomUUID()),
                new Quantity(2),
                money("1.00"),
                money("2.00")
        );
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), "BRL");
    }
}
