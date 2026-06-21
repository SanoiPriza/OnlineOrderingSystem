package org.example.orderService.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    public enum EventStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    public enum EventType {
        STOCK_RESTORE,
        ORDER_CREATED,
        STOCK_COMPENSATION,
        INITIATE_PAYMENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private String exchange;

    @Column(nullable = false)
    private String routingKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private int retryCount;

    private static final int MAX_RETRIES = 5;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
    private String errorMessage;

    public OutboxEvent() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String buildPayload(String productId, int quantity) {
        try {
            return MAPPER.writeValueAsString(Map.of("productId", productId, "quantity", quantity));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize outbox event payload for productId='" + productId + "'", e);
        }
    }

    public static OutboxEvent stockRestore(Long orderId, String productId, int quantity) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(EventType.STOCK_RESTORE);
        event.setExchange(org.example.orderService.config.RabbitMQConfig.EXCHANGE_NAME);
        event.setRoutingKey(org.example.orderService.config.RabbitMQConfig.STOCK_COMPENSATION_ROUTING_KEY);
        event.setStatus(EventStatus.PENDING);
        event.setOrderId(orderId);
        org.example.common.event.StockCompensationEvent payloadEvent = new org.example.common.event.StockCompensationEvent(
                null, orderId.toString(), productId, quantity);
        try {
            event.setPayload(MAPPER.writeValueAsString(payloadEvent));
        } catch (Exception e) {
            event.setPayload("{}");
        }
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    public static OutboxEvent orderCreated(Long orderId, String productId, int quantity) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(EventType.ORDER_CREATED);
        event.setExchange(org.example.orderService.config.RabbitMQConfig.EXCHANGE_NAME);
        event.setRoutingKey(org.example.orderService.config.RabbitMQConfig.ORDER_CREATED_ROUTING_KEY);
        event.setStatus(EventStatus.PENDING);
        event.setOrderId(orderId);
        org.example.common.event.OrderCreatedEvent payloadEvent = new org.example.common.event.OrderCreatedEvent(
                null, orderId.toString(), productId, quantity);
        try {
            event.setPayload(MAPPER.writeValueAsString(payloadEvent));
        } catch (Exception e) {
            event.setPayload("{}");
        }
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    public static OutboxEvent stockCompensation(Long orderId, String productId, int quantity) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(EventType.STOCK_COMPENSATION);
        event.setExchange(org.example.orderService.config.RabbitMQConfig.EXCHANGE_NAME);
        event.setRoutingKey(org.example.orderService.config.RabbitMQConfig.STOCK_COMPENSATION_ROUTING_KEY);
        event.setStatus(EventStatus.PENDING);
        event.setOrderId(orderId);
        org.example.common.event.StockCompensationEvent payloadEvent = new org.example.common.event.StockCompensationEvent(
                null, orderId.toString(), productId, quantity);
        try {
            event.setPayload(MAPPER.writeValueAsString(payloadEvent));
        } catch (Exception e) {
            event.setPayload("{}");
        }
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    public static OutboxEvent initiatePayment(Long orderId, java.math.BigDecimal amount, String currency, String paymentMethod) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(EventType.INITIATE_PAYMENT);
        event.setExchange(org.example.orderService.config.RabbitMQConfig.EXCHANGE_NAME);
        event.setRoutingKey(org.example.orderService.config.RabbitMQConfig.PAYMENT_REQUEST_ROUTING_KEY);
        event.setStatus(EventStatus.PENDING);
        event.setOrderId(orderId);
        org.example.common.event.PaymentRequestEvent payloadEvent = new org.example.common.event.PaymentRequestEvent(
                java.util.UUID.randomUUID().toString(), orderId.toString(), amount, currency, paymentMethod);
        try {
            event.setPayload(MAPPER.writeValueAsString(payloadEvent));
        } catch (Exception e) {
            event.setPayload("{}");
        }
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    public static OutboxEvent refundRequested(Long orderId, String transactionId) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(EventType.INITIATE_PAYMENT); // Reusing event type enum just as a placeholder since it's not strictly used by processor
        event.setExchange(org.example.orderService.config.RabbitMQConfig.EXCHANGE_NAME);
        event.setRoutingKey("payment.refund.request");
        event.setStatus(EventStatus.PENDING);
        event.setOrderId(orderId);
        org.example.common.event.PaymentRefundRequestEvent payloadEvent = new org.example.common.event.PaymentRefundRequestEvent(
                orderId.toString(), transactionId);
        try {
            event.setPayload(MAPPER.writeValueAsString(payloadEvent));
        } catch (Exception e) {
            event.setPayload("{}");
        }
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    public boolean canRetry() {
        return retryCount < MAX_RETRIES;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}