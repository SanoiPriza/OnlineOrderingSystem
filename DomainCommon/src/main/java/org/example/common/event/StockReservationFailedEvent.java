package org.example.common.event;

public class StockReservationFailedEvent {
    private String eventId;
    private String orderId;
    private String reason;

    public StockReservationFailedEvent() {
    }

    public StockReservationFailedEvent(String eventId, String orderId, String reason) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
