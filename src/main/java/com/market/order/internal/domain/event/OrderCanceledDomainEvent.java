package com.market.order.internal.domain.event;

import com.market.order.internal.domain.exception.OrderDomainException;
import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.Money;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.OrderStatus;
import com.market.order.internal.domain.model.PaymentMethod;

import java.time.Instant;

public record OrderCanceledDomainEvent(
        OrderId orderId,
        Instant occurredAt,
        CustomerId customerId,
        Instant cancelledAt
) implements OrderDomainEvent {

    public OrderCanceledDomainEvent {
        if (orderId == null) {
            throw new OrderDomainException("orderId é obrigatório no evento de domínio");
        }

        if (occurredAt == null) {
            throw new OrderDomainException("occurredAt é obrigatório no evento de domínio");
        }

        if (customerId == null) {
            throw new OrderDomainException("customerId é obrigatório no evento de domínio");
        }

        if (cancelledAt == null) {
            throw new OrderDomainException("cancelledAt é obrigatório no evento de domínio");
        }
    }
}
