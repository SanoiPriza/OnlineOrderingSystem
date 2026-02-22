package org.example.orderService.dto;

import org.example.common.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderResponse {
    private Long id;
    private String customerName;
    private String productId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private String statusMessage;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
    private String paymentTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderResponse() {
    }

    public OrderResponse(Long id, String customerName, String productId, Integer quantity, BigDecimal totalPrice,
            OrderStatus status, String statusMessage, String paymentMethod, BigDecimal amount,
            String currency, String paymentTransactionId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.customerName = customerName;
        this.productId = productId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
        this.statusMessage = statusMessage;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
        this.paymentTransactionId = paymentTransactionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public void setPaymentTransactionId(String paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static OrderResponseBuilder builder() {
        return new OrderResponseBuilder();
    }

    public static class OrderResponseBuilder {
        private Long id;
        private String customerName;
        private String productId;
        private Integer quantity;
        private BigDecimal totalPrice;
        private OrderStatus status;
        private String statusMessage;
        private String paymentMethod;
        private BigDecimal amount;
        private String currency;
        private String paymentTransactionId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public OrderResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrderResponseBuilder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public OrderResponseBuilder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public OrderResponseBuilder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public OrderResponseBuilder totalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }

        public OrderResponseBuilder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public OrderResponseBuilder statusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }

        public OrderResponseBuilder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public OrderResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public OrderResponseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public OrderResponseBuilder paymentTransactionId(String paymentTransactionId) {
            this.paymentTransactionId = paymentTransactionId;
            return this;
        }

        public OrderResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public OrderResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public OrderResponse build() {
            return new OrderResponse(id, customerName, productId, quantity, totalPrice, status, statusMessage,
                    paymentMethod, amount, currency, paymentTransactionId, createdAt, updatedAt);
        }
    }
}
