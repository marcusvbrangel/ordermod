package com.market.order.internal.web;

import com.market.order.internal.application.CreateOrderCommand;
import com.market.order.internal.application.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController implements OrderHttpApi {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
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

        orderService.createOrder(command);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Pedido recebido com sucesso");
    }
}
