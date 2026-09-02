package com.market.order.internal.application.service;

import com.market.PostgresTestcontainersConfiguration;
import com.market.order.internal.application.port.in.CreateOrderCommand;
import com.market.order.internal.application.port.in.CreateOrderUseCase;
import com.market.order.internal.application.port.in.GetOrderQuery;
import com.market.order.internal.application.port.in.GetOrderUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Import(PostgresTestcontainersConfiguration.class)
class CreateOrderIntegrationTest {

    @Autowired
    private CreateOrderUseCase useCase;

    @Autowired
    private GetOrderUseCase getOrderUseCase;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void persistsOrderAndCompletesDurableModuleEventPublications() {
        var command = new CreateOrderCommand(
                UUID.randomUUID(),
                "CREDIT_CARD",
                "BRL",
                List.of(
                        new CreateOrderCommand.Item(UUID.randomUUID(), 2, new BigDecimal("10.50")),
                        new CreateOrderCommand.Item(UUID.randomUUID(), 1, new BigDecimal("4.00"))
                )
        );

        var result = useCase.createOrder(command);

        assertEquals(1, countOrders(result.orderId()));
        assertEquals(2, countItems(result.orderId()));
        assertEquals(new BigDecimal("25.00"), result.totalAmount());
        assertEquals("AGUARDANDO_ESTOQUE", result.status());

        var foundOrder = getOrderUseCase.getOrder(new GetOrderQuery(result.orderId()));
        assertEquals(result.orderId(), foundOrder.orderId());
        assertEquals(command.customerId(), foundOrder.customerId());
        assertEquals(command.paymentMethod(), foundOrder.paymentMethod());
        assertEquals("AGUARDANDO_ESTOQUE", foundOrder.status());
        assertEquals(new BigDecimal("25.00"), foundOrder.totalAmount());
        assertEquals("BRL", foundOrder.currency());
        assertEquals(2, foundOrder.items().size());
        assertEquals(new BigDecimal("21.00"), foundOrder.items().getFirst().subtotal());

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertEquals(2, countPublications(result.orderId()));
                    assertEquals(2, countCompletedPublications(result.orderId()));
                });
    }

    private int countOrders(UUID orderId) {
        return queryCount("SELECT COUNT(*) FROM orders.orders WHERE id = :orderId", orderId);
    }

    private int countItems(UUID orderId) {
        return queryCount("SELECT COUNT(*) FROM orders.order_items WHERE order_id = :orderId", orderId);
    }

    private int countPublications(UUID orderId) {
        return queryCount("""
                SELECT COUNT(*)
                FROM event_publication
                WHERE serialized_event LIKE '%' || CAST(:orderId AS TEXT) || '%'
                """, orderId);
    }

    private int countCompletedPublications(UUID orderId) {
        return queryCount("""
                SELECT COUNT(*)
                FROM event_publication
                WHERE serialized_event LIKE '%' || CAST(:orderId AS TEXT) || '%'
                  AND status = 'COMPLETED'
                  AND completion_date IS NOT NULL
                """, orderId);
    }

    private int queryCount(String sql, UUID orderId) {
        return jdbcClient.sql(sql)
                .param("orderId", orderId)
                .query(Integer.class)
                .single();
    }
}
