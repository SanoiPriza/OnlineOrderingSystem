package org.example.orderService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class OrderRequest {
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @NotNull(message = "Total price is required")
    @Positive(message = "Total price must be positive")
    private BigDecimal totalPrice;

    private String paymentMethod;
    private BigDecimal amount;
    private String currency;

    public OrderRequest() {
    }

    public OrderRequest(String customerName, String productId, Integer quantity, BigDecimal totalPrice,
            String paymentMethod, BigDecimal amount, String currency) {
        this.customerName = customerName;
        this.productId = productId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
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

    public static OrderRequestBuilder builder() {
        return new OrderRequestBuilder();
    }

    public static class OrderRequestBuilder {
        private String customerName;
        private String productId;
        private Integer quantity;
        private BigDecimal totalPrice;
        private String paymentMethod;
        private BigDecimal amount;
        private String currency;

        public OrderRequestBuilder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public OrderRequestBuilder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public OrderRequestBuilder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public OrderRequestBuilder totalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }

        public OrderRequestBuilder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public OrderRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public OrderRequestBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public OrderRequest build() {
            return new OrderRequest(customerName, productId, quantity, totalPrice, paymentMethod, amount, currency);
        }
    }
}
