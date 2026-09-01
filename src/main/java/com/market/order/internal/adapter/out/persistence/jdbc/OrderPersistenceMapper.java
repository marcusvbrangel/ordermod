package com.market.order.internal.adapter.out.persistence.jdbc;

import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceMapper {

    public OrderJdbcEntity toEntity(Order order) {
        return new OrderJdbcEntity(
                order.id(),
                order.customerId(),
                order.paymentMethod(),
                order.createdAt(),
                order.version(),
                order.items().stream()
                        .map(item -> new OrderItemJdbcEntity(
                                item.id(),
                                item.productId(),
                                item.quantity()
                        ))
                        .toList()
        );
    }

    public Order toDomain(OrderJdbcEntity entity) {
        return new Order(
                entity.id(),
                entity.customerId(),
                entity.paymentMethod(),
                entity.createdAt(),
                entity.version(),
                entity.items().stream()
                        .map(item -> new OrderItem(
                                item.id(),
                                item.productId(),
                                item.quantity()
                        ))
                        .toList()
        );
    }
}
