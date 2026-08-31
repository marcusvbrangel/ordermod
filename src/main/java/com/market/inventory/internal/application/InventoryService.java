package com.market.inventory.internal.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryService.class);

    public void reserveItems(UUID orderId, List<ItemReservation> items) {
        Objects.requireNonNull(orderId, "orderId é obrigatório");
        Objects.requireNonNull(items, "items é obrigatório");

        if (items.isEmpty()) {
            throw new IllegalArgumentException("items deve conter pelo menos um item");
        }

        LOGGER.info(
                "Reservando {} item(ns) do estoque para o pedido {}",
                items.size(),
                orderId
        );
    }

    public record ItemReservation(
            UUID productId,
            int quantity
    ) {

        public ItemReservation {
            Objects.requireNonNull(productId, "productId é obrigatório");

            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity deve ser maior que zero");
            }
        }
    }
}
