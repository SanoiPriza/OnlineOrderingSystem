package org.example.orderService.service;

import org.example.orderService.client.PaymentServiceClient;
import org.example.orderService.dto.PaymentRequest;
import org.example.orderService.dto.PaymentResponse;
import org.example.orderService.model.OrderEntity;
import org.example.orderService.repository.OrderRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    public OrderEntity createOrder(OrderEntity order) {
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        OrderEntity savedOrder = orderRepository.save(order);

        if (order.getPaymentMethod() != null && order.getAmount() != null) {
            processOrderPaymentAsync(savedOrder);
        }

        return savedOrder;
    }

    @Async
    public CompletableFuture<OrderEntity> processOrderPaymentAsync(OrderEntity order) {
        try {
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderId(order.getId().toString())
                    .amount(order.getAmount())
                    .currency(order.getCurrency())
                    .paymentMethod(order.getPaymentMethod())
                    .cardNumber(order.getCardNumber())
                    .cardExpiryMonth(order.getCardExpiryMonth())
                    .cardExpiryYear(order.getCardExpiryYear())
                    .cardCvv(order.getCardCvv())
                    .cardHolderName(order.getCardHolderName())
                    .build();

            return paymentServiceClient.processPaymentCompletable(paymentRequest)
                    .thenApply(paymentResponse -> {
                        order.setPaymentTransactionId(paymentResponse.getTransactionId());

                        if ("SUCCESS".equals(paymentResponse.getStatus())) {
                            order.setStatus("PAID");
                        } else if ("FAILED".equals(paymentResponse.getStatus())) {
                            order.setStatus("PAYMENT_FAILED");
                            order.setStatusMessage(paymentResponse.getErrorMessage());
                        }

                        order.setUpdatedAt(LocalDateTime.now());
                        return orderRepository.save(order);
                    })
                    .exceptionally(ex -> {
                        order.setStatus("PAYMENT_ERROR");
                        order.setStatusMessage("Error processing payment: " + ex.getMessage());
                        order.setUpdatedAt(LocalDateTime.now());
                        return orderRepository.save(order);
                    });
        } catch (Exception e) {
            order.setStatus("PAYMENT_ERROR");
            order.setStatusMessage("Error processing payment: " + e.getMessage());
            order.setUpdatedAt(LocalDateTime.now());
            OrderEntity savedOrder = orderRepository.save(order);
            return CompletableFuture.completedFuture(savedOrder);
        }
    }

    public OrderEntity processOrderPayment(OrderEntity order) {
        try {
            return processOrderPaymentAsync(order).get();
        } catch (Exception e) {
            order.setStatus("PAYMENT_ERROR");
            order.setStatusMessage("Error processing payment: " + e.getMessage());
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }
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

    @Async
    public CompletableFuture<OrderEntity> refundOrderPaymentAsync(Long id) {
        Optional<OrderEntity> optionalOrder = orderRepository.findById(id);
        if (optionalOrder.isPresent()) {
            OrderEntity order = optionalOrder.get();

            if (order.getPaymentTransactionId() == null) {
                CompletableFuture<OrderEntity> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException("Order has no payment transaction to refund"));
                return future;
            }

            return paymentServiceClient.refundPaymentCompletable(order.getPaymentTransactionId())
                    .thenApply(refundResponse -> {
                        if ("REFUNDED".equals(refundResponse.getStatus())) {
                            order.setStatus("REFUNDED");
                        } else {
                            order.setStatus("REFUND_FAILED");
                            order.setStatusMessage(refundResponse.getErrorMessage());
                        }

                        order.setUpdatedAt(LocalDateTime.now());
                        return orderRepository.save(order);
                    })
                    .exceptionally(ex -> {
                        order.setStatus("REFUND_ERROR");
                        order.setStatusMessage("Error processing refund: " + ex.getMessage());
                        order.setUpdatedAt(LocalDateTime.now());
                        return orderRepository.save(order);
                    });
        }

        CompletableFuture<OrderEntity> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Order not found with id: " + id));
        return future;
    }

    public OrderEntity refundOrderPayment(Long id) {
        try {
            return refundOrderPaymentAsync(id).get();
        } catch (Exception e) {
            throw new RuntimeException("Error refunding payment: " + e.getMessage(), e);
        }
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

    @Async
    public CompletableFuture<PaymentResponse> getOrderPaymentStatusAsync(Long id) {
        Optional<OrderEntity> optionalOrder = orderRepository.findById(id);
        if (optionalOrder.isPresent()) {
            OrderEntity order = optionalOrder.get();

            if (order.getPaymentTransactionId() == null) {
                CompletableFuture<PaymentResponse> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException("Order has no payment transaction"));
                return future;
            }

            return paymentServiceClient.getPaymentStatusCompletable(order.getPaymentTransactionId());
        }

        CompletableFuture<PaymentResponse> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Order not found with id: " + id));
        return future;
    }

    public PaymentResponse getOrderPaymentStatus(Long id) {
        try {
            return getOrderPaymentStatusAsync(id).get();
        } catch (Exception e) {
            throw new RuntimeException("Error getting payment status: " + e.getMessage(), e);
        }
    }
}