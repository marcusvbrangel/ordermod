package com.market.order.internal.adapter.out.event;

import com.market.order.OrderCreatedEvent;
import com.market.order.internal.domain.event.OrderPlacedDomainEvent;
import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.Money;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.OrderStatus;
import com.market.order.internal.domain.model.PaymentMethod;
import com.market.order.internal.domain.model.ProductId;
import com.market.order.internal.domain.model.Quantity;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringOrderEventPublisherTest {

    @Test
    void translatesDomainEventToThePublicIntegrationEvent() {
        var publishedObject = new AtomicReference<Object>();
        ApplicationEventPublisher springPublisher = publishedObject::set;
        var adapter = new SpringOrderEventPublisher(springPublisher);
        var orderId = UUID.randomUUID();
        var customerId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var occurredAt = Instant.parse("2026-09-01T12:00:00Z");
        var event = new OrderPlacedDomainEvent(
                new OrderId(orderId),
                occurredAt,
                new CustomerId(customerId),
                new PaymentMethod("CREDIT_CARD"),
                OrderStatus.AGUARDANDO_ESTOQUE,
                money("21.00"),
                List.of(new OrderPlacedDomainEvent.Item(
                        new ProductId(productId),
                        new Quantity(2),
                        money("10.50"),
                        money("21.00")
                ))
        );

        adapter.publish(event);

        assertEquals(
                new OrderCreatedEvent(
                        orderId,
                        occurredAt,
                        customerId,
                        "CREDIT_CARD",
                        "AGUARDANDO_ESTOQUE",
                        new BigDecimal("21.00"),
                        "BRL",
                        List.of(new OrderCreatedEvent.Item(
                                productId,
                                2,
                                new BigDecimal("10.50"),
                                new BigDecimal("21.00")
                        ))
                ),
                publishedObject.get()
        );
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), "BRL");
    }
}
