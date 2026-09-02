package com.market.order.internal.adapter.out.persistence.jdbc;

import com.market.PostgresTestcontainersConfiguration;
import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.OrderItem;
import com.market.order.internal.domain.model.OrderItemId;
import com.market.order.internal.domain.model.PaymentMethod;
import com.market.order.internal.domain.model.ProductId;
import com.market.order.internal.domain.model.Quantity;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJdbcTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import({
        PostgresTestcontainersConfiguration.class,
        OrderPersistenceAdapter.class,
        OrderPersistenceMapper.class
})
class OrderPersistenceAdapterTest {

    @Autowired
    private OrderPersistenceAdapter adapter;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private Flyway flyway;

    @Test
    void insertsAggregateWithAssignedUuidVersionAndOrderedItems() {
        var order = newOrder();

        var savedOrder = adapter.save(order);

        assertAll(
                () -> assertNull(order.version()),
                () -> assertEquals(1, order.domainEvents().size()),
                () -> assertAggregateState(order, 0, savedOrder),
                () -> assertTrue(savedOrder.domainEvents().isEmpty())
        );

        assertEquals(1, count("orders.orders", order.id()));
        assertEquals(2, count("orders.order_items", order.id()));
        assertEquals(
                new StoredOrder(
                        order.id().value(),
                        order.customerId().value(),
                        order.paymentMethod().value(),
                        order.createdAt(),
                        0
                ),
                storedOrder(order.id())
        );
        assertEquals(
                expectedStoredItems(order),
                storedItems(order.id())
        );

        assertNotNull(flyway.info().current());
        assertEquals("1", flyway.info().current().getVersion().getVersion());
    }

    @Test
    void updatesAnExistingAggregateWhenVersionIsPresent() {
        var newOrder = newOrder();
        var savedOrder = adapter.save(newOrder);
        var updatedOrder = Order.reconstitute(
                savedOrder.id(),
                savedOrder.customerId(),
                new PaymentMethod("CREDIT_CARD"),
                savedOrder.createdAt(),
                savedOrder.version(),
                savedOrder.items()
        );

        var result = adapter.save(updatedOrder);

        assertAll(
                () -> assertNull(newOrder.version()),
                () -> assertEquals(0, savedOrder.version()),
                () -> assertAggregateState(updatedOrder, 1, result),
                () -> assertTrue(result.domainEvents().isEmpty())
        );

        assertEquals(1, count("orders.orders", result.id()));
        assertEquals(2, count("orders.order_items", result.id()));
        assertEquals(
                new StoredOrder(
                        result.id().value(),
                        result.customerId().value(),
                        "CREDIT_CARD",
                        result.createdAt(),
                        1
                ),
                storedOrder(result.id())
        );
        assertEquals(
                expectedStoredItems(result),
                storedItems(result.id())
        );
    }

    private int count(String table, OrderId orderId) {
        var idColumn = table.endsWith("order_items") ? "order_id" : "id";

        return jdbcClient.sql("SELECT COUNT(*) FROM " + table + " WHERE " + idColumn + " = :orderId")
                .param("orderId", orderId.value())
                .query(Integer.class)
                .single();
    }

    private StoredOrder storedOrder(OrderId orderId) {
        return jdbcClient.sql("""
                        SELECT id, customer_id, payment_method, created_at, version
                        FROM orders.orders
                        WHERE id = :orderId
                        """)
                .param("orderId", orderId.value())
                .query((resultSet, rowNumber) -> new StoredOrder(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("customer_id", UUID.class),
                        resultSet.getString("payment_method"),
                        resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
                        resultSet.getInt("version")
                ))
                .single();
    }

    private List<StoredItem> storedItems(OrderId orderId) {
        return jdbcClient.sql("""
                        SELECT id, order_id, product_id, quantity, item_index
                        FROM orders.order_items
                        WHERE order_id = :orderId
                        ORDER BY item_index
                        """)
                .param("orderId", orderId.value())
                .query((resultSet, rowNumber) -> new StoredItem(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("order_id", UUID.class),
                        resultSet.getObject("product_id", UUID.class),
                        resultSet.getInt("quantity"),
                        resultSet.getInt("item_index")
                ))
                .list();
    }

    private static List<StoredItem> expectedStoredItems(Order order) {
        return List.of(
                StoredItem.from(order, order.items().getFirst(), 0),
                StoredItem.from(order, order.items().getLast(), 1)
        );
    }

    private static void assertAggregateState(Order expected, int expectedVersion, Order actual) {
        assertAll(
                () -> assertEquals(expected.id(), actual.id()),
                () -> assertEquals(expected.customerId(), actual.customerId()),
                () -> assertEquals(expected.paymentMethod(), actual.paymentMethod()),
                () -> assertEquals(expected.createdAt(), actual.createdAt()),
                () -> assertEquals(expectedVersion, actual.version()),
                () -> assertEquals(
                        expected.items().stream().map(ItemState::from).toList(),
                        actual.items().stream().map(ItemState::from).toList()
                )
        );
    }

    private static Order newOrder() {
        return Order.place(
                new OrderId(UUID.fromString("20c85288-508a-4c2e-a4ae-d61b5ae3d36c")),
                new CustomerId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
                new PaymentMethod("PIX"),
                Instant.parse("2026-09-01T12:34:56.123456Z"),
                List.of(
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
                )
        );
    }

    private record ItemState(OrderItemId id, ProductId productId, Quantity quantity) {

        private static ItemState from(OrderItem item) {
            return new ItemState(item.id(), item.productId(), item.quantity());
        }
    }

    private record StoredOrder(
            UUID id,
            UUID customerId,
            String paymentMethod,
            Instant createdAt,
            int version
    ) {
    }

    private record StoredItem(
            UUID id,
            UUID orderId,
            UUID productId,
            int quantity,
            int itemIndex
    ) {

        private static StoredItem from(Order order, OrderItem item, int itemIndex) {
            return new StoredItem(
                    item.id().value(),
                    order.id().value(),
                    item.productId().value(),
                    item.quantity().value(),
                    itemIndex
            );
        }
    }
}
