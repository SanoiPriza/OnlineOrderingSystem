package org.example.common.event;

public class StockReservedEvent {
    private String orderId;

    public StockReservedEvent() {
    }

    public StockReservedEvent(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
