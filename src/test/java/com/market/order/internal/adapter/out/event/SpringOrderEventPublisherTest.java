package com.market.order.internal.adapter.out.event;

import com.market.order.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;

class SpringOrderEventPublisherTest {

    @Test
    void delegatesTheSameEventToSpringPublisher() {
        var publishedObject = new AtomicReference<Object>();
        ApplicationEventPublisher springPublisher = publishedObject::set;
        var adapter = new SpringOrderEventPublisher(springPublisher);
        var event = new OrderCreatedEvent(
                UUID.randomUUID(),
                Instant.parse("2026-09-01T12:00:00Z"),
                UUID.randomUUID(),
                "PIX",
                List.of(new OrderCreatedEvent.Item(UUID.randomUUID(), 2))
        );

        adapter.publish(event);

        assertSame(event, publishedObject.get());
    }
}
