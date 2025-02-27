package org.example.orderService.service;

import org.example.orderService.model.OrderEntity;
import org.example.orderService.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<OrderEntity> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public OrderEntity createOrder(OrderEntity order) {
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public OrderEntity updateOrderStatus(Long id, String status) {
        Optional<OrderEntity> optionalOrder = orderRepository.findById(id);
        if (optionalOrder.isPresent()) {
            OrderEntity order = optionalOrder.get();
            order.setStatus(status);
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }
        throw new RuntimeException("Order not found with id: " + id);
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
    
    public List<OrderEntity> getOrdersByCustomerName(String customerName) {
        return orderRepository.findByCustomerName(customerName);
    }
    
    public List<OrderEntity> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }
    
    public List<OrderEntity> getOrdersByProductId(String productId) {
        return orderRepository.findByProductId(productId);
    }
}