package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;

import java.util.UUID;

public record OrderItemId(UUID value) {

    public OrderItemId {
        if (value == null) {
            throw new OrderDomainException("orderItemId é obrigatório");
        }
    }
}
