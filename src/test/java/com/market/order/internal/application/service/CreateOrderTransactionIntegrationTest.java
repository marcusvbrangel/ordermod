package com.market.order.internal.application.service;

import com.market.PostgresTestcontainersConfiguration;
import com.market.order.OrderCreatedEvent;
import com.market.order.internal.application.port.in.CreateOrderCommand;
import com.market.order.internal.application.port.in.CreateOrderUseCase;
import com.market.order.internal.application.port.out.OrderEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Import({
        PostgresTestcontainersConfiguration.class,
        CreateOrderTransactionIntegrationTest.FailingPublisherConfiguration.class
})
class CreateOrderTransactionIntegrationTest {

    @Autowired
    private CreateOrderUseCase useCase;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void rollsBackOrderItemsAndDurablePublicationsWhenPublishingFails() {
        var ordersBefore = count("orders.orders");
        var itemsBefore = count("orders.order_items");
        var publicationsBefore = count("event_publication");
        var command = new CreateOrderCommand(
                UUID.randomUUID(),
                "PIX",
                List.of(new CreateOrderCommand.Item(UUID.randomUUID(), 2))
        );

        assertThrows(PublicationFailure.class, () -> useCase.createOrder(command));

        assertEquals(ordersBefore, count("orders.orders"));
        assertEquals(itemsBefore, count("orders.order_items"));
        assertEquals(publicationsBefore, count("event_publication"));
    }

    private int count(String table) {
        return jdbcClient.sql("SELECT COUNT(*) FROM " + table)
                .query(Integer.class)
                .single();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingPublisherConfiguration {

        @Bean
        @Primary
        OrderEventPublisher failingOrderEventPublisher(ApplicationEventPublisher delegate) {
            return event -> {
                delegate.publishEvent(event);
                throw new PublicationFailure(event);
            };
        }
    }

    static class PublicationFailure extends RuntimeException {

        PublicationFailure(OrderCreatedEvent event) {
            super("Falha simulada após publicar o pedido " + event.orderId());
        }
    }
}
