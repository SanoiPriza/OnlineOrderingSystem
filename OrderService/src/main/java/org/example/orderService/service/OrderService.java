package org.example.orderService.service;

import org.example.common.exception.ResourceNotFoundException;
import org.example.common.model.OrderStatus;
import org.example.orderService.client.PaymentServiceClient;
import org.example.orderService.dto.PaymentRequest;
import org.example.orderService.dto.PaymentResponse;
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

    public OrderService(OrderRepository orderRepository, PaymentServiceClient paymentServiceClient) {
        this.orderRepository = orderRepository;
        this.paymentServiceClient = paymentServiceClient;
    }

    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<OrderEntity> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public OrderEntity createOrder(OrderEntity order) {
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        OrderEntity savedOrder = orderRepository.save(order);

        if (order.getPaymentMethod() != null && order.getAmount() != null) {
            processOrderPaymentAsync(savedOrder);
        }

        return savedOrder;
    }

    @Async
    @Transactional
    public CompletableFuture<OrderEntity> processOrderPaymentAsync(OrderEntity order) {
        try {
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderId(order.getId().toString())
                    .amount(order.getAmount())
                    .currency(order.getCurrency())
                    .paymentMethod(order.getPaymentMethod())
                    .build();

            return paymentServiceClient.processPaymentCompletable(paymentRequest)
                    .thenApply(paymentResponse -> {
                        order.setPaymentTransactionId(paymentResponse.getTransactionId());

                        if ("SUCCESS".equals(paymentResponse.getStatus())) {
                            order.setStatus(OrderStatus.PAID);
                        } else if ("FAILED".equals(paymentResponse.getStatus())) {
                            order.setStatus(OrderStatus.PAYMENT_FAILED);
                            order.setStatusMessage(paymentResponse.getErrorMessage());
                        }

                        order.setUpdatedAt(LocalDateTime.now());
                        return orderRepository.save(order);
                    })
                    .exceptionally(ex -> {
                        order.setStatus(OrderStatus.PAYMENT_ERROR);
                        order.setStatusMessage("Error processing payment: " + ex.getMessage());
                        order.setUpdatedAt(LocalDateTime.now());
                        return orderRepository.save(order);
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
    public OrderEntity processOrderPayment(OrderEntity order) {
        try {
            return processOrderPaymentAsync(order).get();
        } catch (Exception e) {
            order.setStatus(OrderStatus.PAYMENT_ERROR);
            order.setStatusMessage("Error processing payment: " + e.getMessage());
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }
    }

    @Transactional
    public OrderEntity updateOrderStatus(Long id, OrderStatus status) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Async
    @Transactional
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
                    if ("REFUNDED".equals(refundResponse.getStatus())) {
                        order.setStatus(OrderStatus.REFUNDED);
                    } else {
                        order.setStatus(OrderStatus.REFUND_FAILED);
                        order.setStatusMessage(refundResponse.getErrorMessage());
                    }

                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order);
                })
                .exceptionally(ex -> {
                    order.setStatus(OrderStatus.REFUND_ERROR);
                    order.setStatusMessage("Error processing refund: " + ex.getMessage());
                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order);
                });
    }

    @Transactional
    public OrderEntity refundOrderPayment(Long id) {
        try {
            return refundOrderPaymentAsync(id).get();
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

    public List<OrderEntity> getOrdersByCustomerName(String customerName) {
        return orderRepository.findByCustomerName(customerName);
    }

    public List<OrderEntity> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public List<OrderEntity> getOrdersByProductId(String productId) {
        return orderRepository.findByProductId(productId);
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