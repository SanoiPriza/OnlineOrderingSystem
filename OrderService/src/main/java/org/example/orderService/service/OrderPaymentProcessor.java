package org.example.orderService.service;

import org.example.common.event.PaymentResultEvent;
import org.example.common.exception.ResourceNotFoundException;
import org.example.common.model.OrderStatus;
import org.example.orderService.client.PaymentServiceClient;
import org.example.orderService.dto.PaymentResponse;
import org.example.orderService.model.OrderEntity;
import org.example.orderService.model.OutboxEvent;
import org.example.orderService.repository.OrderRepository;
import org.example.orderService.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final RabbitTemplate rabbitTemplate;

    public OrderPaymentProcessor(OrderRepository orderRepository,
                                 OutboxEventRepository outboxEventRepository,
                                 PaymentServiceClient paymentServiceClient,
                                 RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentServiceClient = paymentServiceClient;
        this.rabbitTemplate = rabbitTemplate;
    }


    @Transactional
    public OrderEntity handlePaymentResult(PaymentResultEvent paymentResponse) {
        Long orderId = Long.parseLong(paymentResponse.getOrderId());
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

    public OrderEntity handlePaymentError(Long orderId, Throwable ex) {
        log.error("Network or technical error during payment for order ID: {}. Propagating for retry.", orderId, ex);
        throw new RuntimeException("Payment processing failed due to technical error", ex);
    }

    public CompletableFuture<OrderEntity> refundPaymentAsync(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));

        if (order.getPaymentTransactionId() == null) {
            CompletableFuture<OrderEntity> f = new CompletableFuture<>();
            f.completeExceptionally(new ResourceNotFoundException("Order has no payment transaction to refund"));
            return f;
        }

        order.setStatus(OrderStatus.REFUND_PENDING);
        order.setUpdatedAt(LocalDateTime.now());
        OrderEntity savedOrder = orderRepository.save(order);

        OutboxEvent event = OutboxEvent.refundRequested(order.getId(), order.getPaymentTransactionId());
        outboxEventRepository.save(event);

        return CompletableFuture.completedFuture(savedOrder);
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
