package org.example.orderService.service;

import org.example.common.exception.ResourceNotFoundException;
import org.example.common.model.OrderStatus;
import org.example.orderService.client.PaymentServiceClient;

import org.example.orderService.dto.PaymentRequest;
import org.example.orderService.dto.PaymentResponse;
import org.example.orderService.model.OrderEntity;
import org.example.orderService.model.OutboxEvent;
import org.example.orderService.repository.OrderRepository;
import org.example.orderService.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Component
public class OrderPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentProcessor.class);

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentServiceClient paymentServiceClient;

    public OrderPaymentProcessor(OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            PaymentServiceClient paymentServiceClient) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentServiceClient = paymentServiceClient;
    }

    @Async
    public CompletableFuture<OrderEntity> processPaymentAsync(Long orderId) {
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
                    .thenApply(paymentResponse -> handlePaymentResult(orderId, paymentResponse))
                    .exceptionally(ex -> handlePaymentError(orderId, ex));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(handlePaymentError(orderId, e));
        }
    }

    @Transactional
    protected OrderEntity handlePaymentResult(Long orderId, PaymentResponse paymentResponse) {
        OrderEntity fresh = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        fresh.setPaymentTransactionId(paymentResponse.getTransactionId());

        if ("SUCCESS".equals(paymentResponse.getStatus())) {
            fresh.setStatus(OrderStatus.PAID);
        } else {
            fresh.setStatus(OrderStatus.PAYMENT_FAILED);
            fresh.setStatusMessage(paymentResponse.getErrorMessage());
            enqueueStockRestore(fresh);
        }

        fresh.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(fresh);
    }

    @Transactional
    protected OrderEntity handlePaymentError(Long orderId, Throwable ex) {
        OrderEntity fresh = orderRepository.findById(orderId).orElse(null);
        if (fresh != null) {
            fresh.setStatus(OrderStatus.PAYMENT_ERROR);
            fresh.setStatusMessage("Error processing payment: " + ex.getMessage());
            fresh.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(fresh);
            enqueueStockRestore(fresh);
            return fresh;
        }
        return null;
    }

    @Async
    public CompletableFuture<OrderEntity> refundPaymentAsync(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        if (order.getPaymentTransactionId() == null) {
            CompletableFuture<OrderEntity> f = new CompletableFuture<>();
            f.completeExceptionally(new ResourceNotFoundException("Order has no payment transaction to refund"));
            return f;
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

    @Async
    public CompletableFuture<PaymentResponse> getPaymentStatusAsync(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        if (order.getPaymentTransactionId() == null) {
            CompletableFuture<PaymentResponse> f = new CompletableFuture<>();
            f.completeExceptionally(new ResourceNotFoundException("Order has no payment transaction"));
            return f;
        }

        return paymentServiceClient.getPaymentStatusCompletable(order.getPaymentTransactionId());
    }

    private void enqueueStockRestore(OrderEntity order) {
        if (order.getProductId() == null || order.getQuantity() == null || order.getQuantity() <= 0) {
            return;
        }
        OutboxEvent event = OutboxEvent.stockCompensation(order.getId(), order.getProductId(), order.getQuantity());
        outboxEventRepository.save(event);
        log.info("Enqueued STOCK_COMPENSATION outbox event for order {} (product={}, qty={})",
                order.getId(), order.getProductId(), order.getQuantity());
    }
}
