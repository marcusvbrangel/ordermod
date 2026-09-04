package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Quantity(int value) {

    public Quantity {
        if (value <= 0) {
            throw new OrderDomainException("quantity deve ser maior que zero");
        }
    }
}
