package com.market.order.internal.application.service;

import com.market.order.internal.application.exception.OrderNotCancellableException;
import com.market.order.internal.application.exception.OrderNotFoundException;
import com.market.order.internal.application.port.in.CancelOrderCommand;
import com.market.order.internal.application.port.in.CancelOrderResult;
import com.market.order.internal.application.port.in.CancelOrderUseCase;
import com.market.order.internal.application.port.out.OrderRepository;
import com.market.order.internal.application.port.out.OrderEventPublisher;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public CancelOrderService(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public CancelOrderResult cancelOrder(CancelOrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command é obrigatória");
        }

        var orderId = new OrderId(command.orderId());

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        // Allowed cancel only when order is still in AGUARDANDO_ESTOQUE (no reservation/payment)
        var status = order.status();
        if (status == OrderStatus.CANCELADO) {
            return new CancelOrderResult(order.id().value(), status.name(), Instant.now());
        }

        if (status != OrderStatus.AGUARDANDO_ESTOQUE) {
            throw new OrderNotCancellableException(order.id().value(), status.name());
        }

        var cancelled = order.cancel();

        var saved = orderRepository.save(cancelled);

        // publish domain events recorded during cancellation
        publishDomainEvents(cancelled);

        return new CancelOrderResult(saved.id().value(), saved.status().name(), Instant.now());
    }

    private void publishDomainEvents(Order order) {
        order.domainEvents().forEach(eventPublisher::publish);
        order.clearDomainEvents();
    }
}
