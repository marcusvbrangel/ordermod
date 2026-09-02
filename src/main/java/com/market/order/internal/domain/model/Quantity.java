package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;

public record Quantity(int value) {

    public Quantity {
        if (value <= 0) {
            throw new OrderDomainException("quantity deve ser maior que zero");
        }
    }
}
