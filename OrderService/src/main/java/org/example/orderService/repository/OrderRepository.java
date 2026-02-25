package org.example.orderService.repository;

import org.example.common.model.OrderStatus;
import org.example.orderService.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByCustomerName(String customerName);

    List<OrderEntity> findByStatus(OrderStatus status);

    List<OrderEntity> findByProductId(String productId);

    List<OrderEntity> findByUsername(String username);
}