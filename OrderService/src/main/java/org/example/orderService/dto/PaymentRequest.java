package org.example.orderService.dto;

import java.math.BigDecimal;

public class PaymentRequest {
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;

    public PaymentRequest() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public static class Builder {
        private final PaymentRequest request = new PaymentRequest();

        public Builder orderId(String orderId) {
            request.setOrderId(orderId);
            return this;
        }

        public Builder amount(BigDecimal amount) {
            request.setAmount(amount);
            return this;
        }

        public Builder currency(String currency) {
            request.setCurrency(currency);
            return this;
        }

        public Builder paymentMethod(String paymentMethod) {
            request.setPaymentMethod(paymentMethod);
            return this;
        }

        public PaymentRequest build() {
            return request;
        }
    }
}