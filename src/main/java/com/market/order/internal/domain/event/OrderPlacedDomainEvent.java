package com.market.order.internal.domain.event;

import com.market.order.internal.domain.exception.OrderDomainException;
import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.PaymentMethod;
import com.market.order.internal.domain.model.ProductId;
import com.market.order.internal.domain.model.Quantity;

import java.time.Instant;
import java.util.List;

public record OrderPlacedDomainEvent(
        OrderId orderId,
        Instant occurredAt,
        CustomerId customerId,
        PaymentMethod paymentMethod,
        List<Item> items
) implements OrderDomainEvent {

    public OrderPlacedDomainEvent {
        if (orderId == null) {
            throw new OrderDomainException("orderId é obrigatório no evento de domínio");
        }

        if (occurredAt == null) {
            throw new OrderDomainException("occurredAt é obrigatório no evento de domínio");
        }

        if (customerId == null) {
            throw new OrderDomainException("customerId é obrigatório no evento de domínio");
        }

        if (paymentMethod == null) {
            throw new OrderDomainException("paymentMethod é obrigatório no evento de domínio");
        }

        if (items == null || items.isEmpty()) {
            throw new OrderDomainException("items é obrigatório no evento de domínio");
        }

        items = List.copyOf(items);
    }

    public record Item(ProductId productId, Quantity quantity) {

        public Item {
            if (productId == null) {
                throw new OrderDomainException("productId é obrigatório no evento de domínio");
            }

            if (quantity == null) {
                throw new OrderDomainException("quantity é obrigatória no evento de domínio");
            }
        }
    }
}
