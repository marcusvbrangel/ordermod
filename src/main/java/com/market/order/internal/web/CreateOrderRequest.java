package com.market.order.internal.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "customerId é obrigatório")
        UUID customerId,

        @NotBlank(message = "paymentMethod é obrigatório")
        String paymentMethod,

        @NotEmpty(message = "items deve conter pelo menos um item")
        @Valid
        List<Item> items
) {

    public record Item(
            @NotNull(message = "productId é obrigatório")
            UUID productId,

            @Positive(message = "quantity deve ser maior que zero")
            int quantity
    ) {
    }
}
