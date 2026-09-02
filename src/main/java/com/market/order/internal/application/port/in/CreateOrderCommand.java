package com.market.order.internal.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateOrderCommand(
        UUID customerId,
        String paymentMethod,
        String currency,
        List<Item> items
) {

    public CreateOrderCommand {
        Objects.requireNonNull(customerId, "customerId é obrigatório");
        Objects.requireNonNull(paymentMethod, "paymentMethod é obrigatório");
        Objects.requireNonNull(currency, "currency é obrigatória");
        Objects.requireNonNull(items, "items é obrigatório");
        items = List.copyOf(items);
    }

    public record Item(
            UUID productId,
            int quantity,
            BigDecimal unitPrice
    ) {

        public Item {
            Objects.requireNonNull(productId, "productId é obrigatório");
            Objects.requireNonNull(unitPrice, "unitPrice é obrigatório");
        }
    }
}
