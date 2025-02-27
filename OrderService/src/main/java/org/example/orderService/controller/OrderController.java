package org.example.orderService.controller;

import org.example.orderService.model.OrderEntity;
import org.example.orderService.repository.OrderRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping("/orders")
    public OrderEntity createOrder(@RequestBody OrderEntity order) {
        return orderRepository.save(order);
    }
}