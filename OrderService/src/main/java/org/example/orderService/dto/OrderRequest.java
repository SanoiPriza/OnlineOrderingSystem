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

    public OrderRequest() {}

    public OrderRequest(String customerName, String productId, Integer quantity, BigDecimal totalPrice,
            String paymentMethod) {
        this.customerName = customerName;
        this.productId = productId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.paymentMethod = paymentMethod;
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public static OrderRequestBuilder builder() { return new OrderRequestBuilder(); }

    public static class OrderRequestBuilder {
        private String customerName;
        private String productId;
        private Integer quantity;
        private BigDecimal totalPrice;
        private String paymentMethod;

        public OrderRequestBuilder customerName(String v) { this.customerName = v; return this; }
        public OrderRequestBuilder productId(String v) { this.productId = v; return this; }
        public OrderRequestBuilder quantity(Integer v) { this.quantity = v; return this; }
        public OrderRequestBuilder totalPrice(BigDecimal v) { this.totalPrice = v; return this; }
        public OrderRequestBuilder paymentMethod(String v) { this.paymentMethod = v; return this; }

        public OrderRequest build() {
            return new OrderRequest(customerName, productId, quantity, totalPrice, paymentMethod);
        }
    }
}
