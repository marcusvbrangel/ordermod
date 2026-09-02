package com.market.order.internal.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "customerId é obrigatório")
        UUID customerId,

        @NotBlank(message = "paymentMethod é obrigatório")
        String paymentMethod,

        @NotBlank(message = "currency é obrigatória")
        String currency,

        @NotEmpty(message = "items deve conter pelo menos um item")
        List<@NotNull(message = "item é obrigatório") @Valid Item> items
) {

    public record Item(
            @NotNull(message = "productId é obrigatório")
            UUID productId,

            @Positive(message = "quantity deve ser maior que zero")
            int quantity,

            @NotNull(message = "unitPrice é obrigatório")
            @DecimalMin(value = "0.00", inclusive = false, message = "unitPrice deve ser maior que zero")
            @Digits(integer = 17, fraction = 2, message = "unitPrice deve possuir no máximo 17 dígitos inteiros e 2 decimais")
            BigDecimal unitPrice
    ) {
    }
}
