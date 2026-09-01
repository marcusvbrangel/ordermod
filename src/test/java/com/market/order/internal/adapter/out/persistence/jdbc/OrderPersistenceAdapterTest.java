package com.market.order.internal.adapter.out.persistence.jdbc;

import com.market.PostgresTestcontainersConfiguration;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderItem;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        assertEquals(order.id(), savedOrder.id());
        assertEquals(0, savedOrder.version());
        assertEquals(1, count("orders.orders", order.id()));
        assertEquals(2, count("orders.order_items", order.id()));
        assertEquals(
                List.of(0, 1),
                jdbcClient.sql("""
                                SELECT item_index
                                FROM orders.order_items
                                WHERE order_id = :orderId
                                ORDER BY item_index
                                """)
                        .param("orderId", order.id())
                        .query(Integer.class)
                        .list()
        );
        assertEquals(
                "PIX",
                jdbcClient.sql("""
                                SELECT payment_method
                                FROM orders.orders
                                WHERE id = :orderId
                                """)
                        .param("orderId", order.id())
                        .query(String.class)
                        .single()
        );

        assertNotNull(flyway.info().current());
        assertEquals("1", flyway.info().current().getVersion().getVersion());
    }

    @Test
    void updatesAnExistingAggregateWhenVersionIsPresent() {
        var savedOrder = adapter.save(newOrder());
        var updatedOrder = new Order(
                savedOrder.id(),
                savedOrder.customerId(),
                "CREDIT_CARD",
                savedOrder.createdAt(),
                savedOrder.version(),
                savedOrder.items()
        );

        var result = adapter.save(updatedOrder);

        assertEquals(1, result.version());
        assertEquals(1, count("orders.orders", result.id()));
        assertEquals(2, count("orders.order_items", result.id()));
        assertEquals(
                "CREDIT_CARD",
                jdbcClient.sql("SELECT payment_method FROM orders.orders WHERE id = :orderId")
                        .param("orderId", result.id())
                        .query(String.class)
                        .single()
        );
    }

    private int count(String table, UUID orderId) {
        var idColumn = table.endsWith("order_items") ? "order_id" : "id";

        return jdbcClient.sql("SELECT COUNT(*) FROM " + table + " WHERE " + idColumn + " = :orderId")
                .param("orderId", orderId)
                .query(Integer.class)
                .single();
    }

    private static Order newOrder() {
        return new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PIX",
                Instant.now().truncatedTo(ChronoUnit.MICROS),
                null,
                List.of(
                        new OrderItem(UUID.randomUUID(), UUID.randomUUID(), 2),
                        new OrderItem(UUID.randomUUID(), UUID.randomUUID(), 1)
                )
        );
    }
}
