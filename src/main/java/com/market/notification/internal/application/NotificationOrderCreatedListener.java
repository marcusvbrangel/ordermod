package com.market.notification.internal.application;

import com.market.order.OrderCreatedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class NotificationOrderCreatedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationOrderCreatedListener.class);

    @ApplicationModuleListener(id = "notification-on-order-created")
    void on(OrderCreatedEvent event) {

        LOGGER.info("Notification Application - Email {} item(ns) do estoque para o pedido {}",
                event.items().size(),
                event.orderId()
        );

    }

}
