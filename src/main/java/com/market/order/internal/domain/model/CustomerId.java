package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;

import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new OrderDomainException("customerId é obrigatório");
        }
    }
}
