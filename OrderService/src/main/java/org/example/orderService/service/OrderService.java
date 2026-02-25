package org.example.orderService.service;

import org.example.common.exception.InvalidOperationException;
import org.example.common.exception.ResourceNotFoundException;
import org.example.common.model.OrderStatus;
import org.example.orderService.dto.PaymentResponse;
import org.example.orderService.dto.OrderRequest;
import org.example.orderService.dto.OrderResponse;
import org.example.orderService.mapper.OrderMapper;
import org.example.orderService.model.OrderEntity;
import org.example.orderService.repository.OrderRepository;
import org.example.orderService.repository.OutboxEventRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderMapper orderMapper;
    private final OrderPaymentProcessor orderPaymentProcessor;

    public OrderService(OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            OrderMapper orderMapper,
            OrderPaymentProcessor orderPaymentProcessor) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.orderMapper = orderMapper;
        this.orderPaymentProcessor = orderPaymentProcessor;
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public List<OrderResponse> getMyOrders() {
        String username = resolveCurrentUsername();
        return orderRepository.findByUsername(username).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public Optional<OrderResponse> getOrderById(Long id) {
        return orderRepository.findById(id).map(orderMapper::toResponse);
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        OrderEntity order = orderMapper.toEntity(orderRequest);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUsername(resolveCurrentUsername());

        order.setAmount(orderRequest.getTotalPrice());
        order.setCurrency("USD");

        OrderEntity savedOrder = orderRepository.save(order);

        org.example.orderService.model.OutboxEvent event = org.example.orderService.model.OutboxEvent.orderCreated(
                savedOrder.getId(), orderRequest.getProductId(), orderRequest.getQuantity());
        outboxEventRepository.save(event);

        return orderMapper.toResponse(savedOrder);
    }

    public CompletableFuture<OrderEntity> processOrderPaymentAsync(Long orderId) {
        return orderPaymentProcessor.processPaymentAsync(orderId);
    }

    @Transactional
    public OrderEntity processOrderPayment(Long orderId) {
        try {
            return orderPaymentProcessor.processPaymentAsync(orderId).get();
        } catch (Exception e) {
            OrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
            order.setStatus(OrderStatus.PAYMENT_ERROR);
            order.setStatusMessage("Error processing payment: " + e.getMessage());
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        if (!order.getStatus().canTransitionTo(status)) {
            throw new InvalidOperationException(
                    "Invalid order status transition: " + order.getStatus() + " -> " + status);
        }
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    public CompletableFuture<OrderEntity> refundOrderPaymentAsync(Long id) {
        return orderPaymentProcessor.refundPaymentAsync(id);
    }

    @Transactional
    public OrderResponse refundOrderPayment(Long id) {
        try {
            return orderMapper.toResponse(orderPaymentProcessor.refundPaymentAsync(id).get());
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error refunding payment: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order", "id", id);
        }
        orderRepository.deleteById(id);
    }

    public List<OrderResponse> getOrdersByCustomerName(String customerName) {
        return orderRepository.findByCustomerName(customerName).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public List<OrderResponse> getOrdersByProductId(String productId) {
        return orderRepository.findByProductId(productId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public CompletableFuture<PaymentResponse> getOrderPaymentStatusAsync(Long id) {
        return orderPaymentProcessor.getPaymentStatusAsync(id);
    }

    public PaymentResponse getOrderPaymentStatus(Long id) {
        try {
            return orderPaymentProcessor.getPaymentStatusAsync(id).get();
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error getting payment status: " + e.getMessage());
        }
    }

    private String resolveCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }
}
