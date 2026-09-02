package com.market.order.internal.adapter.in.web;

import com.market.order.internal.application.port.in.CreateOrderCommand;
import com.market.order.internal.application.port.in.CreateOrderUseCase;
import com.market.order.internal.application.port.in.GetOrderQuery;
import com.market.order.internal.application.port.in.GetOrderUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController implements OrderHttpApi {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final com.market.order.internal.application.port.in.CancelOrderUseCase cancelOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase, GetOrderUseCase getOrderUseCase) {
        this(createOrderUseCase, getOrderUseCase, null);
    }

    public OrderController(CreateOrderUseCase createOrderUseCase, GetOrderUseCase getOrderUseCase, com.market.order.internal.application.port.in.CancelOrderUseCase cancelOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
    }

    @Override
    public ResponseEntity<CreateOrderResponse> createOrder(CreateOrderRequest request) {
        var command = new CreateOrderCommand(
                request.customerId(),
                request.paymentMethod(),
                request.currency(),
                request.items().stream()
                        .map(item -> new CreateOrderCommand.Item(
                                item.productId(),
                                item.quantity(),
                                item.unitPrice()
                        ))
                        .toList()
        );

        var result = createOrderUseCase.createOrder(command);

        var response = new CreateOrderResponse(
                result.orderId(),
                result.status(),
                result.totalAmount(),
                result.currency(),
                result.items().stream()
                        .map(item -> new CreateOrderResponse.Item(
                                item.productId(),
                                item.quantity(),
                                item.unitPrice(),
                                item.subtotal()
                        ))
                        .toList()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Override
    public ResponseEntity<GetOrderResponse> getOrder(java.util.UUID orderId) {
        var result = getOrderUseCase.getOrder(new GetOrderQuery(orderId));

        var response = new GetOrderResponse(
                result.orderId(),
                result.customerId(),
                result.paymentMethod(),
                result.status(),
                result.totalAmount(),
                result.currency(),
                result.createdAt(),
                result.items().stream()
                        .map(item -> new GetOrderResponse.Item(
                                item.itemId(),
                                item.productId(),
                                item.quantity(),
                                item.unitPrice(),
                                item.subtotal()
                        ))
                        .toList()
        );

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CancelOrderResponse> cancelOrder(java.util.UUID orderId) {
        if (cancelOrderUseCase == null) {
            throw new IllegalStateException("CancelOrderUseCase não está disponível");
        }

        var result = cancelOrderUseCase.cancelOrder(new com.market.order.internal.application.port.in.CancelOrderCommand(orderId));

        var response = new CancelOrderResponse(
                result.orderId(),
                result.status(),
                result.cancelledAt()
        );

        return ResponseEntity.ok(response);
    }
}
