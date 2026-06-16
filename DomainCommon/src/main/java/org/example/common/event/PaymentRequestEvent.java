package org.example.common.event;

import java.math.BigDecimal;

public class PaymentRequestEvent {
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;

    public PaymentRequestEvent() {
    }

    public PaymentRequestEvent(String orderId, BigDecimal amount, String currency, String paymentMethod) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
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
}
