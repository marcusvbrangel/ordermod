package com.market.order.internal.domain.model;

import com.market.order.internal.domain.exception.OrderDomainException;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.UUID;

@ValueObject
public record ProductId(UUID value) {

    public ProductId {
        if (value == null) {
            throw new OrderDomainException("productId é obrigatório");
        }
    }
}
