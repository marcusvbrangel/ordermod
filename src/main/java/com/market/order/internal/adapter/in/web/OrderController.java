package com.market.order.internal.adapter.in.web;

import com.market.order.internal.application.port.in.CreateOrderCommand;
import com.market.order.internal.application.port.in.CreateOrderUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController implements OrderHttpApi {

    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @Override
    public ResponseEntity<String> createOrder(CreateOrderRequest request) {
        var command = new CreateOrderCommand(
                request.customerId(),
                request.paymentMethod(),
                request.items().stream()
                        .map(item -> new CreateOrderCommand.Item(
                                item.productId(),
                                item.quantity()
                        ))
                        .toList()
        );

        createOrderUseCase.createOrder(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Pedido recebido com sucesso");
    }
}
