package com.market.order.internal.infrastructure.persistence;

import com.market.order.internal.domain.Order;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface OrderRepository extends CrudRepository<Order, UUID> {
}
