package org.example.common.event;

public class StockReservedEvent {
    private String eventId;
    private String orderId;

    public StockReservedEvent() {
    }

    public StockReservedEvent(String eventId, String orderId) {
        this.eventId = eventId;
        this.orderId = orderId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
