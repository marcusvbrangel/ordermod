package com.market.inventory.internal.application;

import com.market.order.OrderCreatedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryOrderCreatedListener {

    private final InventoryService inventoryService;

    public InventoryOrderCreatedListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @ApplicationModuleListener
    void on(OrderCreatedEvent event) {
        var items = event.items().stream()
                .map(item -> new InventoryService.ItemReservation(
                        item.productId(),
                        item.quantity()
                ))
                .toList();

        inventoryService.reserveItems(event.orderId(), items);
    }
}
