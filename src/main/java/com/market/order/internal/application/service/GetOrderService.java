package com.market.order.internal.application.service;

import com.market.order.internal.application.exception.OrderNotFoundException;
import com.market.order.internal.application.port.in.GetOrderQuery;
import com.market.order.internal.application.port.in.GetOrderResult;
import com.market.order.internal.application.port.in.GetOrderUseCase;
import com.market.order.internal.application.port.out.OrderRepository;
import com.market.order.internal.domain.model.OrderId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class GetOrderService implements GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GetOrderResult getOrder(GetOrderQuery query) {
        Objects.requireNonNull(query, "query é obrigatória");

        var order = orderRepository.findById(new OrderId(query.orderId()))
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));

        return new GetOrderResult(
                order.id().value(),
                order.customerId().value(),
                order.paymentMethod().value(),
                order.status().name(),
                order.total().amount(),
                order.total().currency(),
                order.createdAt(),
                order.items().stream()
                        .map(item -> new GetOrderResult.Item(
                                item.id().value(),
                                item.productId().value(),
                                item.quantity().value(),
                                item.unitPrice().amount(),
                                item.subtotal().amount()
                        ))
                        .toList()
        );
    }
}
