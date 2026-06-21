package org.example.common.event;

public class PaymentResultEvent {
    private String orderId;
    private String status;
    private String transactionId;
    private String errorMessage;

    public PaymentResultEvent() {
    }

    public PaymentResultEvent(String orderId, String status, String transactionId, String errorMessage) {
        this.orderId = orderId;
        this.status = status;
        this.transactionId = transactionId;
        this.errorMessage = errorMessage;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
