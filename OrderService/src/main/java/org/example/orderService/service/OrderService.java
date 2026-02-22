package org.example.orderService.service;

import org.example.common.exception.ResourceNotFoundException;
import org.example.common.model.OrderStatus;
import org.example.orderService.client.PaymentServiceClient;
import org.example.orderService.dto.PaymentRequest;
import org.example.orderService.dto.PaymentResponse;
import org.example.orderService.dto.OrderRequest;
import org.example.orderService.dto.OrderResponse;
import org.example.orderService.mapper.OrderMapper;
import org.example.orderService.model.OrderEntity;
import org.example.orderService.repository.OrderRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository, PaymentServiceClient paymentServiceClient,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.paymentServiceClient = paymentServiceClient;
        this.orderMapper = orderMapper;
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
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
        OrderEntity savedOrder = orderRepository.save(order);

        if (order.getPaymentMethod() != null && order.getAmount() != null) {
            processOrderPaymentAsync(savedOrder.getId());
        }

        return orderMapper.toResponse(savedOrder);
    }

    @Async
    public CompletableFuture<OrderEntity> processOrderPaymentAsync(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        try {
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderId(order.getId().toString())
                    .amount(order.getAmount())
                    .currency(order.getCurrency())
                    .paymentMethod(order.getPaymentMethod())
                    .build();

            return paymentServiceClient.processPaymentCompletable(paymentRequest)
                    .thenApply(paymentResponse -> {
                        OrderEntity fresh = orderRepository.findById(orderId)
                                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

                        fresh.setPaymentTransactionId(paymentResponse.getTransactionId());

                        if ("SUCCESS".equals(paymentResponse.getStatus())) {
                            fresh.setStatus(OrderStatus.PAID);
                        } else if ("FAILED".equals(paymentResponse.getStatus())) {
                            fresh.setStatus(OrderStatus.PAYMENT_FAILED);
                            fresh.setStatusMessage(paymentResponse.getErrorMessage());
                        }

                        fresh.setUpdatedAt(LocalDateTime.now());
                        return orderRepository.save(fresh);
                    })
                    .exceptionally(ex -> {
                        OrderEntity fresh = orderRepository.findById(orderId).orElse(null);
                        if (fresh != null) {
                            fresh.setStatus(OrderStatus.PAYMENT_ERROR);
                            fresh.setStatusMessage("Error processing payment: " + ex.getMessage());
                            fresh.setUpdatedAt(LocalDateTime.now());
                            return orderRepository.save(fresh);
                        }
                        return null;
                    });
        } catch (Exception e) {
            order.setStatus(OrderStatus.PAYMENT_ERROR);
            order.setStatusMessage("Error processing payment: " + e.getMessage());
            order.setUpdatedAt(LocalDateTime.now());
            OrderEntity savedOrder = orderRepository.save(order);
            return CompletableFuture.completedFuture(savedOrder);
        }
    }

    @Transactional
    public OrderEntity processOrderPayment(Long orderId) {
        try {
            return processOrderPaymentAsync(orderId).get();
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
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Async
    public CompletableFuture<OrderEntity> refundOrderPaymentAsync(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        if (order.getPaymentTransactionId() == null) {
            CompletableFuture<OrderEntity> future = new CompletableFuture<>();
            future.completeExceptionally(new ResourceNotFoundException("Order has no payment transaction to refund"));
            return future;
        }

        return paymentServiceClient.refundPaymentCompletable(order.getPaymentTransactionId())
                .thenApply(refundResponse -> {
                    OrderEntity fresh = orderRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

                    if ("REFUNDED".equals(refundResponse.getStatus())) {
                        fresh.setStatus(OrderStatus.REFUNDED);
                    } else {
                        fresh.setStatus(OrderStatus.REFUND_FAILED);
                        fresh.setStatusMessage(refundResponse.getErrorMessage());
                    }

                    fresh.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(fresh);
                })
                .exceptionally(ex -> {
                    OrderEntity fresh = orderRepository.findById(id).orElse(null);
                    if (fresh != null) {
                        fresh.setStatus(OrderStatus.REFUND_ERROR);
                        fresh.setStatusMessage("Error processing refund: " + ex.getMessage());
                        fresh.setUpdatedAt(LocalDateTime.now());
                        return orderRepository.save(fresh);
                    }
                    return null;
                });
    }

    @Transactional
    public OrderResponse refundOrderPayment(Long id) {
        try {
            return orderMapper.toResponse(refundOrderPaymentAsync(id).get());
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

    @Async
    public CompletableFuture<PaymentResponse> getOrderPaymentStatusAsync(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        if (order.getPaymentTransactionId() == null) {
            CompletableFuture<PaymentResponse> future = new CompletableFuture<>();
            future.completeExceptionally(new ResourceNotFoundException("Order has no payment transaction"));
            return future;
        }

        return paymentServiceClient.getPaymentStatusCompletable(order.getPaymentTransactionId());
    }

    public PaymentResponse getOrderPaymentStatus(Long id) {
        try {
            return getOrderPaymentStatusAsync(id).get();
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error getting payment status: " + e.getMessage());
        }
    }
}
