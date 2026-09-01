package com.market.order.internal.adapter.out.persistence.jdbc;

import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderPersistenceMapperTest {

    private final OrderPersistenceMapper mapper = new OrderPersistenceMapper();

    @Test
    void mapsDomainOrderToJdbcEntityAndBackWithoutLosingData() {
        var order = orderFixture(7);

        var entity = mapper.toEntity(order);
        var mappedOrder = mapper.toDomain(entity);

        assertAll(
                () -> assertEquals(order.id(), entity.id()),
                () -> assertEquals(order.customerId(), entity.customerId()),
                () -> assertEquals(order.paymentMethod(), entity.paymentMethod()),
                () -> assertEquals(order.createdAt(), entity.createdAt()),
                () -> assertEquals(order.version(), entity.version()),
                () -> assertEquals(2, entity.items().size()),
                () -> assertEquals(order.items().getFirst().id(), entity.items().getFirst().id()),
                () -> assertEquals(order.items().getFirst().productId(), entity.items().getFirst().productId()),
                () -> assertEquals(order.items().getFirst().quantity(), entity.items().getFirst().quantity()),
                () -> assertEquals(order.items().getLast().id(), entity.items().getLast().id()),
                () -> assertEquals(order.items().getLast().productId(), entity.items().getLast().productId()),
                () -> assertEquals(order.items().getLast().quantity(), entity.items().getLast().quantity()),
                () -> assertEquals(order, mappedOrder)
        );
    }

    @Test
    void preservesNullVersionForANewOrder() {
        var order = orderFixture(null);

        var mappedOrder = mapper.toDomain(mapper.toEntity(order));

        assertAll(
                () -> assertNull(mappedOrder.version()),
                () -> assertEquals(order, mappedOrder)
        );
    }

    private static Order orderFixture(Integer version) {
        return new Order(
                UUID.fromString("20c85288-508a-4c2e-a4ae-d61b5ae3d36c"),
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "PIX",
                Instant.parse("2026-09-01T12:34:56Z"),
                version,
                List.of(
                        new OrderItem(
                                UUID.fromString("384414fd-8b64-44df-8678-304f108f87f7"),
                                UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"),
                                2
                        ),
                        new OrderItem(
                                UUID.fromString("145df3f2-5904-4af0-adbb-4d07dbe40f0f"),
                                UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8"),
                                1
                        )
                )
        );
    }
}
