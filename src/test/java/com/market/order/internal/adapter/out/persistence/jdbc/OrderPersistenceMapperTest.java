package com.market.order.internal.adapter.out.persistence.jdbc;

import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.OrderItem;
import com.market.order.internal.domain.model.OrderItemId;
import com.market.order.internal.domain.model.PaymentMethod;
import com.market.order.internal.domain.model.ProductId;
import com.market.order.internal.domain.model.Quantity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderPersistenceMapperTest {

    private final OrderPersistenceMapper mapper = new OrderPersistenceMapper();

    @Test
    void mapsDomainOrderToJdbcEntityAndBackWithoutLosingData() {
        var order = orderFixture(7);

        var entity = mapper.toEntity(order);
        var mappedOrder = mapper.toDomain(entity);

        assertAll(
                () -> assertEquals(order.id().value(), entity.id()),
                () -> assertEquals(order.customerId().value(), entity.customerId()),
                () -> assertEquals(order.paymentMethod().value(), entity.paymentMethod()),
                () -> assertEquals(order.createdAt(), entity.createdAt()),
                () -> assertEquals(order.version(), entity.version()),
                () -> assertEquals(2, entity.items().size()),
                () -> assertEquals(order.items().getFirst().id().value(), entity.items().getFirst().id()),
                () -> assertEquals(order.items().getFirst().productId().value(), entity.items().getFirst().productId()),
                () -> assertEquals(order.items().getFirst().quantity().value(), entity.items().getFirst().quantity()),
                () -> assertEquals(order.items().getLast().id().value(), entity.items().getLast().id()),
                () -> assertEquals(order.items().getLast().productId().value(), entity.items().getLast().productId()),
                () -> assertEquals(order.items().getLast().quantity().value(), entity.items().getLast().quantity()),
                () -> assertOrderStateEquals(order, mappedOrder),
                () -> assertTrue(mappedOrder.domainEvents().isEmpty())
        );
    }

    @Test
    void preservesNullVersionForANewOrder() {
        var order = Order.place(
                new OrderId(UUID.fromString("20c85288-508a-4c2e-a4ae-d61b5ae3d36c")),
                new CustomerId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
                new PaymentMethod("PIX"),
                Instant.parse("2026-09-01T12:34:56Z"),
                orderItemsFixture()
        );

        var mappedOrder = mapper.toDomain(mapper.toEntity(order));

        assertAll(
                () -> assertEquals(1, order.domainEvents().size()),
                () -> assertNull(mappedOrder.version()),
                () -> assertOrderStateEquals(order, mappedOrder),
                () -> assertTrue(mappedOrder.domainEvents().isEmpty())
        );
    }

    private static Order orderFixture(Integer version) {
        return Order.reconstitute(
                new OrderId(UUID.fromString("20c85288-508a-4c2e-a4ae-d61b5ae3d36c")),
                new CustomerId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
                new PaymentMethod("PIX"),
                Instant.parse("2026-09-01T12:34:56Z"),
                version,
                orderItemsFixture()
        );
    }

    private static List<OrderItem> orderItemsFixture() {
        return List.of(
                OrderItem.create(
                        new OrderItemId(UUID.fromString("384414fd-8b64-44df-8678-304f108f87f7")),
                        new ProductId(UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")),
                        new Quantity(2)
                ),
                OrderItem.create(
                        new OrderItemId(UUID.fromString("145df3f2-5904-4af0-adbb-4d07dbe40f0f")),
                        new ProductId(UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")),
                        new Quantity(1)
                )
        );
    }

    private static void assertOrderStateEquals(Order expected, Order actual) {
        assertAll(
                () -> assertEquals(expected.id(), actual.id()),
                () -> assertEquals(expected.customerId(), actual.customerId()),
                () -> assertEquals(expected.paymentMethod(), actual.paymentMethod()),
                () -> assertEquals(expected.createdAt(), actual.createdAt()),
                () -> assertEquals(expected.version(), actual.version()),
                () -> assertEquals(
                        expected.items().stream().map(ItemState::from).toList(),
                        actual.items().stream().map(ItemState::from).toList()
                )
        );
    }

    private record ItemState(OrderItemId id, ProductId productId, Quantity quantity) {

        private static ItemState from(OrderItem item) {
            return new ItemState(item.id(), item.productId(), item.quantity());
        }
    }
}
