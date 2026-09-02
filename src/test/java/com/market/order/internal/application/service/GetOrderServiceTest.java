package com.market.order.internal.application.service;

import com.market.order.internal.application.exception.OrderNotFoundException;
import com.market.order.internal.application.port.in.GetOrderQuery;
import com.market.order.internal.application.port.out.OrderRepository;
import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.Money;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.OrderItem;
import com.market.order.internal.domain.model.OrderItemId;
import com.market.order.internal.domain.model.OrderStatus;
import com.market.order.internal.domain.model.PaymentMethod;
import com.market.order.internal.domain.model.ProductId;
import com.market.order.internal.domain.model.Quantity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetOrderServiceTest {

    @Test
    void findsAndMapsTheCompleteOrderSnapshot() {
        var order = persistedOrder();
        var receivedId = new AtomicReference<OrderId>();
        var service = new GetOrderService(repository(order, receivedId));

        var result = service.getOrder(new GetOrderQuery(order.id().value()));

        assertAll(
                () -> assertEquals(order.id(), receivedId.get()),
                () -> assertEquals(order.id().value(), result.orderId()),
                () -> assertEquals(order.customerId().value(), result.customerId()),
                () -> assertEquals("PIX", result.paymentMethod()),
                () -> assertEquals("AGUARDANDO_ESTOQUE", result.status()),
                () -> assertEquals(new BigDecimal("25.00"), result.totalAmount()),
                () -> assertEquals("BRL", result.currency()),
                () -> assertEquals(order.createdAt(), result.createdAt()),
                () -> assertEquals(2, result.items().size()),
                () -> assertEquals(order.items().getFirst().id().value(), result.items().getFirst().itemId()),
                () -> assertEquals(new BigDecimal("10.50"), result.items().getFirst().unitPrice()),
                () -> assertEquals(new BigDecimal("21.00"), result.items().getFirst().subtotal()),
                () -> assertTrue(order.domainEvents().isEmpty())
        );
    }

    @Test
    void reportsWhenOrderDoesNotExist() {
        var orderId = UUID.randomUUID();
        var service = new GetOrderService(repository(null, new AtomicReference<>()));

        var exception = assertThrows(
                OrderNotFoundException.class,
                () -> service.getOrder(new GetOrderQuery(orderId))
        );

        assertEquals("Pedido não encontrado: " + orderId, exception.getMessage());
    }

    @Test
    void rejectsNullQueryBeforeCallingRepository() {
        var receivedId = new AtomicReference<OrderId>();
        var service = new GetOrderService(repository(null, receivedId));

        assertThrows(NullPointerException.class, () -> service.getOrder(null));
        assertEquals(null, receivedId.get());
    }

    private static OrderRepository repository(Order order, AtomicReference<OrderId> receivedId) {
        return new OrderRepository() {
            @Override
            public Order save(Order aggregate) {
                throw new AssertionError("save não deveria ser chamado");
            }

            @Override
            public Optional<Order> findById(OrderId orderId) {
                receivedId.set(orderId);
                return Optional.ofNullable(order);
            }
        };
    }

    private static Order persistedOrder() {
        return Order.reconstitute(
                new OrderId(UUID.fromString("20c85288-508a-4c2e-a4ae-d61b5ae3d36c")),
                new CustomerId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
                new PaymentMethod("PIX"),
                OrderStatus.AGUARDANDO_ESTOQUE,
                money("25.00"),
                Instant.parse("2026-09-02T12:00:00Z"),
                /* cancelledAt */ null,
                0,
                List.of(
                        OrderItem.reconstitute(
                                new OrderItemId(UUID.fromString("384414fd-8b64-44df-8678-304f108f87f7")),
                                new ProductId(UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")),
                                new Quantity(2),
                                money("10.50"),
                                money("21.00")
                        ),
                        OrderItem.reconstitute(
                                new OrderItemId(UUID.fromString("145df3f2-5904-4af0-adbb-4d07dbe40f0f")),
                                new ProductId(UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")),
                                new Quantity(1),
                                money("4.00"),
                                money("4.00")
                        )
                )
        );
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), "BRL");
    }
}
