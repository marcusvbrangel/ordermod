package com.market.order.internal.adapter.out.persistence.jdbc;

import com.market.order.internal.domain.model.CustomerId;
import com.market.order.internal.domain.model.Money;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderId;
import com.market.order.internal.domain.model.OrderItem;
import com.market.order.internal.domain.model.OrderItemId;
import com.market.order.internal.domain.model.OrderStatus;
import com.market.order.internal.domain.model.PaymentMethod;
import com.market.order.internal.domain.model.ProductId;
import com.market.order.internal.domain.model.Quantity;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceMapper {

    public OrderJdbcEntity toEntity(Order order) {
        return new OrderJdbcEntity(
                order.id().value(),
                order.customerId().value(),
                order.paymentMethod().value(),
                order.status().name(),
                order.total().amount(),
                order.total().currency(),
                order.createdAt(),
                order.cancelledAt(),
                order.version(),
                order.items().stream()
                        .map(item -> new OrderItemJdbcEntity(
                                item.id().value(),
                                item.productId().value(),
                                item.quantity().value(),
                                item.unitPrice().amount(),
                                item.subtotal().amount()
                        ))
                        .toList()
        );
    }

    public Order toDomain(OrderJdbcEntity entity) {
        return Order.reconstitute(
                new OrderId(entity.id()),
                new CustomerId(entity.customerId()),
                new PaymentMethod(entity.paymentMethod()),
                OrderStatus.valueOf(entity.status()),
                new Money(entity.totalAmount(), entity.currency()),
                entity.createdAt(),
                entity.cancelledAt(),
                entity.version(),
                entity.items().stream()
                        .map(item -> OrderItem.reconstitute(
                                new OrderItemId(item.id()),
                                new ProductId(item.productId()),
                                new Quantity(item.quantity()),
                                new Money(item.unitPrice(), entity.currency()),
                                new Money(item.subtotal(), entity.currency())
                        ))
                        .toList()
        );
    }
}
