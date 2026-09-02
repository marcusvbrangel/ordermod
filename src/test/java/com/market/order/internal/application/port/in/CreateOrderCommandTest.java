package com.market.order.internal.application.port.in;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateOrderCommandTest {

    @Test
    void protectsItsItemsFromExternalMutation() {
        var items = new ArrayList<>(List.of(
                new CreateOrderCommand.Item(UUID.randomUUID(), 2)
        ));
        var command = new CreateOrderCommand(UUID.randomUUID(), "PIX", items);

        items.clear();

        assertEquals(1, command.items().size());
        assertThrows(UnsupportedOperationException.class, () -> command.items().clear());
    }

    @Test
    void rejectsMissingStructuralData() {
        var customerId = UUID.randomUUID();
        var items = List.of(new CreateOrderCommand.Item(UUID.randomUUID(), 1));

        assertThrows(NullPointerException.class, () -> new CreateOrderCommand(null, "PIX", items));
        assertThrows(NullPointerException.class, () -> new CreateOrderCommand(customerId, null, items));
        assertThrows(NullPointerException.class, () -> new CreateOrderCommand(customerId, "PIX", null));
        assertThrows(NullPointerException.class, () -> new CreateOrderCommand.Item(null, 1));
    }
}
