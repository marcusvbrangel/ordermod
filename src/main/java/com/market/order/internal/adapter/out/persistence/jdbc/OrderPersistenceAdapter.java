package com.market.order.internal.adapter.out.persistence.jdbc;

import com.market.order.internal.application.port.out.OrderRepository;
import com.market.order.internal.domain.model.Order;
import com.market.order.internal.domain.model.OrderId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OrderPersistenceAdapter implements OrderRepository {

    private final SpringDataOrderRepository repository;
    private final OrderPersistenceMapper mapper;

    public OrderPersistenceAdapter(
            SpringDataOrderRepository repository,
            OrderPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        var savedEntity = repository.save(mapper.toEntity(order));

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return repository.findById(orderId.value())
                .map(mapper::toDomain);
    }
}
