package org.example.orderService.service;

import org.example.common.exception.ResourceNotFoundException;
import org.example.common.model.OrderStatus;
import org.example.orderService.client.PaymentServiceClient;

import org.example.common.event.PaymentResultEvent;
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

import org.example.common.event.PaymentRequestEvent;
import org.example.orderService.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import org.springframework.context.ApplicationEventPublisher;
import org.example.orderService.event.OutboxSavedEvent;

@Component
public class OrderPaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrderPaymentProcessor.class);

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RabbitTemplate rabbitTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private OrderPaymentProcessor self;

    public OrderPaymentProcessor(OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            PaymentServiceClient paymentServiceClient,
            ApplicationEventPublisher applicationEventPublisher,
            RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentServiceClient = paymentServiceClient;
        this.applicationEventPublisher = applicationEventPublisher;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentRequestEvent(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        PaymentRequestEvent event = new PaymentRequestEvent(
                order.getId().toString(),
                order.getAmount(),
                order.getCurrency(),
                order.getPaymentMethod()
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.PAYMENT_REQUEST_ROUTING_KEY, event);
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
        applicationEventPublisher.publishEvent(new OutboxSavedEvent(this));
        log.info("Enqueued STOCK_COMPENSATION outbox event for order {} (product={}, qty={})",
                order.getId(), order.getProductId(), order.getQuantity());
    }
}
