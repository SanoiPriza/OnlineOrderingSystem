package org.example.orderService.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    public enum EventStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    public enum EventType {
        STOCK_RESTORE,
        ORDER_CREATED,
        STOCK_COMPENSATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

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

    public static OutboxEvent stockRestore(Long orderId, String productId, int quantity) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(EventType.STOCK_RESTORE);
        event.setStatus(EventStatus.PENDING);
        event.setOrderId(orderId);
        event.setPayload("{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}");
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    public static OutboxEvent orderCreated(Long orderId, String productId, int quantity) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(EventType.ORDER_CREATED);
        event.setStatus(EventStatus.PENDING);
        event.setOrderId(orderId);
        event.setPayload("{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}");
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    public static OutboxEvent stockCompensation(Long orderId, String productId, int quantity) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(EventType.STOCK_COMPENSATION);
        event.setStatus(EventStatus.PENDING);
        event.setOrderId(orderId);
        event.setPayload("{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}");
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
