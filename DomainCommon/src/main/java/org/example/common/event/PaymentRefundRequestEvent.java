package org.example.common.event;

public class PaymentRefundRequestEvent {
    private String orderId;
    private String transactionId;

    public PaymentRefundRequestEvent() {
    }

    public PaymentRefundRequestEvent(String orderId, String transactionId) {
        this.orderId = orderId;
        this.transactionId = transactionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
